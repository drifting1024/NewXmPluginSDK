package com.ryeex.groot.lib.ble.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;

import com.ryeex.groot.lib.ble.BleContext;
import com.ryeex.groot.lib.ble.beacon.BleAdvertise;
import com.ryeex.groot.lib.ble.beacon.BleAdvertiseParser;
import com.ryeex.groot.lib.ble.beacon.MiotPacket;
import com.ryeex.groot.lib.ble.beacon.MiotPacketParser;
import com.ryeex.groot.lib.common.thread.MessageHandlerThread;
import com.ryeex.groot.lib.log.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;
import static com.ryeex.groot.lib.ble.scan.BleScanner.ScanStatus.ENTERING_SCAN;
import static com.ryeex.groot.lib.ble.scan.BleScanner.ScanStatus.SCANNING;
import static com.ryeex.groot.lib.ble.scan.BleScanner.ScanStatus.SCAN_STOPPED;

/**
 * Created by chenhao on 2017/11/10.
 */

public class BleScanner {
    public static final String ACTION_SCANNED_DEVICE = "com.ryeex.groot.action.blescanner.scanned_device";
    public static final String ACTION_SCAN_STOPPED = "com.ryeex.groot.action.blescanner.scan_stopped";
    public static final String DEVICE = "device";

    private static BleScanner sInstance;

    private static Object sLock = new Object();

    ScanStatus mScanStatus;
    List<ScannedDevice> mScannedDeviceList = new CopyOnWriteArrayList<>();

    List<Long> mRecentFourStartScanTimeList = new CopyOnWriteArrayList<>();

    private Object listLock = new Object();
    private BluetoothAdapter mBluetoothAdapter;
    private MessageHandlerThread mHandlerThread;
    private Handler mWorkerHandler;

    private BluetoothAdapter.LeScanCallback mScanCallbackOld = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice btDevice, final int rssi, final byte[] scanRecord) {
            processDeviceScanned(btDevice, rssi, scanRecord, DeviceScannedSource.ON_LE_SCAN);
        }
    };

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.i(TAG_BLE, "BleScanner scan timeup");

            stopScan();
        }
    };

    private BleScanner() {
        init();
    }

    public static BleScanner getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new BleScanner();
                }
            }
        }
        return sInstance;
    }

    private void processDeviceScanned(final BluetoothDevice btDevice, final int rssi, final byte[] scanRecord, final DeviceScannedSource source) {
        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (btDevice.getName() != null && mBluetoothAdapter.checkBluetoothAddress(btDevice.getAddress())) {
                    ScannedDevice device = new ScannedDevice();
                    device.setName(btDevice.getName());
                    device.setMac(btDevice.getAddress());
                    device.setRssi(rssi);

                    synchronized (listLock) {
                        boolean already = false;

                        for (ScannedDevice deviceItem : mScannedDeviceList) {
                            if (deviceItem.getMac().equals(device.getMac())) {
                                already = true;
                                break;
                            }
                        }

                        if (!already) {
                            List<BleAdvertise> advertiseList = BleAdvertiseParser.parse(scanRecord);
                            MiotPacket packet = MiotPacketParser.parse(advertiseList);

                            if (packet != null && packet.productId == 911) {
                                device.setProductId(packet.productId);

                                mScannedDeviceList.add(device);
                                sendBroadcastDeviceScanned(device);

//                                Logger.i(TAG_BLE, "BleScanner scanned:" + device.getName() + ", " + device.getMac() + ", " + device.getRssi() + ", " + device.getProductId() + ", " + source);
                            }
                        }
                    }
                }
            }
        });
    }

    private void init() {
        mHandlerThread = new MessageHandlerThread("BleScanner-worker");
        mHandlerThread.start();

        mWorkerHandler = new Handler(mHandlerThread.getLooper());

        BluetoothManager bluetoothManager = (BluetoothManager) BleContext.sAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        setScanStatus(SCAN_STOPPED);
    }

    public void startScan() {
        if (getScanStatus() == ENTERING_SCAN || getScanStatus() == SCANNING) {
            // 将已扫到的广播出去
            synchronized (listLock) {
                for (ScannedDevice device : mScannedDeviceList) {
                    sendBroadcastDeviceScanned(device);
                }
            }
            return;
        }

        synchronized (listLock) {
            mScannedDeviceList.clear();
        }

        setScanStatus(ENTERING_SCAN);

        // 30秒之内最多扫4次

        long currentTime = System.currentTimeMillis();

        long startDelayMillis;

        if (mRecentFourStartScanTimeList.size() < 4) {
            mRecentFourStartScanTimeList.add(currentTime);
            startDelayMillis = 0;

            Logger.i(TAG_BLE, "BleScanner.startScan delay:" + startDelayMillis + "ms (for <4 times total)");
        } else {
            long recentFirstTime = mRecentFourStartScanTimeList.get(mRecentFourStartScanTimeList.size() - 4);
            if ((currentTime - recentFirstTime) <= 30 * DateUtils.SECOND_IN_MILLIS) {
                startDelayMillis = 30 * DateUtils.SECOND_IN_MILLIS - (currentTime - recentFirstTime) + 100;

                Logger.i(TAG_BLE, "BleScanner.startScan delay:" + startDelayMillis + "ms (for >4 times 30 seconds)");
            } else {
                mRecentFourStartScanTimeList.add(currentTime);
                mRecentFourStartScanTimeList.remove(0);
                startDelayMillis = 0;

                Logger.i(TAG_BLE, "BleScanner.startScan delay:" + startDelayMillis + "ms (for <=4 times 30 seconds)");
            }
        }

        mWorkerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mScanCallbackOld);

                setScanStatus(SCANNING);

                mWorkerHandler.postDelayed(mStopRunnable, 30 * DateUtils.SECOND_IN_MILLIS);
            }
        }, startDelayMillis);
    }

    public void stopScan() {
        if (getScanStatus() == SCAN_STOPPED) {
            return;
        }

        Logger.i(TAG_BLE, "BleScanner.stopScan");

        mBluetoothAdapter.stopLeScan(mScanCallbackOld);

        mWorkerHandler.removeCallbacks(mStopRunnable);

        setScanStatus(SCAN_STOPPED);

        sendBroadcastScanStopped();
    }

    private synchronized ScanStatus getScanStatus() {
        return mScanStatus;
    }

    private synchronized void setScanStatus(ScanStatus scanStatus) {
        mScanStatus = scanStatus;
    }

    public boolean isScanning() {
        return getScanStatus() == SCANNING;
    }

    private void sendBroadcastDeviceScanned(ScannedDevice device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(BleContext.sAppContext);
        Intent param = new Intent(ACTION_SCANNED_DEVICE);
        param.putExtra(DEVICE, device);
        localBroadManager.sendBroadcast(param);
    }

    private void sendBroadcastScanStopped() {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(BleContext.sAppContext);
        Intent param = new Intent(ACTION_SCAN_STOPPED);
        localBroadManager.sendBroadcast(param);
    }

    enum ScanStatus {
        SCAN_STOPPED, ENTERING_SCAN, SCANNING
    }

    private enum DeviceScannedSource {
        ON_SCAN_RESULT, ON_BATCH_SCAN_RESULTS, ON_LE_SCAN
    }
}
