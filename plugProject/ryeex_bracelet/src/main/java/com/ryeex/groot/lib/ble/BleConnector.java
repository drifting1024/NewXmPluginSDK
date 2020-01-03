package com.ryeex.groot.lib.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateUtils;

import com.ryeex.groot.lib.ble.blerequest.BaseRequest;
import com.ryeex.groot.lib.ble.blerequest.ConnectRequest;
import com.ryeex.groot.lib.ble.blerequest.IndicationRequest;
import com.ryeex.groot.lib.ble.blerequest.NotifyRequest;
import com.ryeex.groot.lib.ble.blerequest.ReadCharacterRequest;
import com.ryeex.groot.lib.ble.blerequest.ReadDescriptorRequest;
import com.ryeex.groot.lib.ble.blerequest.ReadRssiRequest;
import com.ryeex.groot.lib.ble.blerequest.UnIndicationRequest;
import com.ryeex.groot.lib.ble.blerequest.UnNotifyRequest;
import com.ryeex.groot.lib.ble.blerequest.WriteCharacterRequest;
import com.ryeex.groot.lib.ble.blerequest.WriteCharacterWithoutRspRequest;
import com.ryeex.groot.lib.ble.blerequest.WriteDescriptorRequest;
import com.ryeex.groot.lib.ble.requestresult.DescriptorWriteResult;
import com.ryeex.groot.lib.ble.requestresult.ReadDescriptorResult;
import com.ryeex.groot.lib.ble.requestresult.ReadResult;
import com.ryeex.groot.lib.ble.requestresult.ReadRssiResult;
import com.ryeex.groot.lib.ble.requestresult.WriteResult;
import com.ryeex.groot.lib.ble.util.BleUUIDUtil;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.thread.MessageHandlerThread;
import com.ryeex.groot.lib.common.util.BleUtil;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ryeex.groot.lib.ble.BleConnector.DisconnectedCause.PROCESS_TIMEOUT;
import static com.ryeex.groot.lib.ble.BleConnector.DisconnectedCause.STATUS_EQUAL_8;
import static com.ryeex.groot.lib.ble.BleConnector.DisconnectedCause.UNKNOWN;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_IS_NULL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_NOT_INDICATABLE;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_NOT_NOTIFIABLE;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_NOT_READABLE;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_NOT_WRITABLE;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_NOT_WRITABLE_NO_RESPONSE;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_READ_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_SET_INDICATABLE_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_SET_NOTIFIABLE_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CHARACTER_WRITE_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CONNECT_GATT_EXCEPTION;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_DESCRIPTOR_IS_NULL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_DESCRIPTOR_READ_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_DESCRIPTOR_SET_VALUE_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_DESCRIPTOR_WRITE_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_DISCONNECTED;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_GATT_IS_NULL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_RSSI_READ_FAIL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_TIMEOUT;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_WORKER_THREAD_NOT_READY;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_MI_EVENT;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_MI_TOKEN;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_OPEN;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_RC4;
import static com.ryeex.groot.lib.ble.BleSetting.SERVICE_MI;
import static com.ryeex.groot.lib.ble.BleSetting.SERVICE_RYEEX;
import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;
import static com.ryeex.groot.lib.common.util.BleUtil.isCharacteristicIndicatable;
import static com.ryeex.groot.lib.common.util.BleUtil.isCharacteristicNoRspWritable;
import static com.ryeex.groot.lib.common.util.BleUtil.isCharacteristicNotifiable;
import static com.ryeex.groot.lib.common.util.BleUtil.isCharacteristicReadable;
import static com.ryeex.groot.lib.common.util.BleUtil.isCharacteristicWritable;

/**
 * 蓝牙连接器: 直接与设备连接 <br/>
 * <br/>
 * Created by chenhao on 16/12/30.
 */
public class BleConnector {
    private static final int MSG_PROCESS_REQUEST = 1;
    private static final int MSG_PROCESS_TIMEOUT = 2;

    private static final long TIMEOUT_REQUEST_CONNECT = 30 * DateUtils.SECOND_IN_MILLIS;
    private static final long TIMEOUT_REQUEST = 5 * DateUtils.SECOND_IN_MILLIS;

    private static int sThreadNum = 0;
    MessageHandlerThread mWorkerThread;
    WorkerHandler mWorkerHandler;
    private BaseRequest mCurrentRequest;
    private String mMac;
    private Queue<BaseRequest> mRequestQueue = new ConcurrentLinkedQueue<>();
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;

    private Object mConnectFailCountLock = new Object();
    private int mConnectFailCount = 0;

    private Object mProcessConnect133Lock = new Object();
    private int mConnect133Count = 0;
    private boolean mIsProcessingConnect133 = false;

    private Map<UUID, Map<UUID, BluetoothGattCharacteristic>> mProfile = new HashMap<>();
    private List<ConnectorListener> mConnectorListeners = new ArrayList<>();
    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, final int status, final int newState) {
            if (!isWorking()) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Logger.i(TAG_BLE, "onConnectionStateChange status:" + status + " newState:" + newState);

                Logger.i(TAG_BLE, "BluetoothGatt.discoverServices");
                if (mBluetoothGatt.discoverServices()) {

                } else {
                    Logger.i(TAG_BLE, "BluetoothGatt.discoverServices false");

                    Logger.i(TAG_BLE, "BleUtil.clearGattCache");
                    BleUtil.clearGattCache(mBluetoothGatt);

                    // 1秒后第一次重试
                    mWorkerHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Logger.i(TAG_BLE, "BluetoothGatt.discoverServices (2)");
                            if (!mBluetoothGatt.discoverServices()) {

                                Logger.i(TAG_BLE, "BluetoothGatt.discoverServices false (2)");
                                BleUtil.clearGattCache(mBluetoothGatt);

                                // 1秒后第二次重试
                                mWorkerHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Logger.i(TAG_BLE, "BluetoothGatt.discoverServices (3)");
                                        if (!mBluetoothGatt.discoverServices()) {
                                            Logger.i(TAG_BLE, "BluetoothGatt.discoverServices false (3)");
                                        }
                                    }
                                }, 1000);
                            }
                        }
                    }, 1000);
                }
            } else {
                Logger.i(TAG_BLE, "onConnectionStateChange status:" + status + " newState:" + newState);

                if (mWorkerHandler.hasMessages(MSG_PROCESS_TIMEOUT) && (getCurrentRequest() instanceof ConnectRequest)) {
                    if (status == 133) {
                        //连接时的133很有可能是广播信道连上了，但数据信道失败，此时重试即可
                        final int doConnect133Count;
                        synchronized (mProcessConnect133Lock) {
                            mConnect133Count++;
                            doConnect133Count = mConnect133Count;
                        }

                        if (doConnect133Count >= 10) {
                            Logger.i(TAG_BLE, "receive too many 133, count:" + doConnect133Count);

                            boolean isProcessingConnect133;
                            synchronized (mProcessConnect133Lock) {
                                isProcessingConnect133 = mIsProcessingConnect133;
                            }

                            Logger.i(TAG_BLE, "receive too many 133, isProcessing:" + isProcessingConnect133);

                            if (isProcessingConnect133) {
                                return;
                            }

                            synchronized (mProcessConnect133Lock) {
                                mIsProcessingConnect133 = true;
                            }

                            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            boolean disableSuccess = bluetoothAdapter.disable();

                            Logger.i(TAG_BLE, "process too many 133, BluetoothAdapter.disable " + disableSuccess);

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    boolean enableSuccess = bluetoothAdapter.enable();

                                    Logger.i(TAG_BLE, "process too many 133, BluetoothAdapter.enable " + enableSuccess);

                                    synchronized (mProcessConnect133Lock) {
                                        mConnect133Count = 0;
                                        mIsProcessingConnect133 = false;
                                    }
                                }
                            }, 1000);

                        } else {
                            Logger.i(TAG_BLE, "BleUtil.clearGattCache");
                            BleUtil.clearGattCache(mBluetoothGatt);

                            synchronized (BleConnector.this) {
                                mBluetoothGatt.close();
                            }

                            mWorkerHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Logger.i(TAG_BLE, "retry doConnectWorked status:" + status + " newState:" + newState + " retryCount:" + doConnect133Count);

                                    doConnectWorked(null);
                                }
                            }, 500);
                        }
                    } else if (status == 8 || status == 19 || status == 129) {
                        Logger.i(TAG_BLE, "BleUtil.clearGattCache");
                        BleUtil.clearGattCache(mBluetoothGatt);

                        synchronized (BleConnector.this) {
                            mBluetoothGatt.close();
                        }

                        mWorkerHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Logger.i(TAG_BLE, "retry doConnectWorked status:" + status + " newState:" + newState);

                                doConnectWorked(null);
                            }
                        }, 500);
                    } else {
                        Logger.i(TAG_BLE, "BleConnector.doDisconnect ConnectRequest status:" + status + " newState:" + newState);

                        doDisconnectCallback(new Error(BLE_DISCONNECTED, "onConnectionStateChange status:" + status + " newState:" + newState));

                        // 自动断开连接
                        disconnect();

                        for (ConnectorListener connectorListener : mConnectorListeners) {
                            connectorListener.onDisconnected((status == 8) ? STATUS_EQUAL_8 : UNKNOWN);
                        }
                    }
                } else {
                    Logger.i(TAG_BLE, "BleConnector.doDisconnect non-ConnectRequest status:" + status + " newState:" + newState);

                    doDisconnectCallback(new Error(BLE_DISCONNECTED, "onConnectionStateChange status:" + status + " newState:" + newState));

                    // 自动断开连接
                    disconnect();

                    for (ConnectorListener connectorListener : mConnectorListeners) {
                        connectorListener.onDisconnected((status == 8) ? STATUS_EQUAL_8 : UNKNOWN);
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!isWorking()) {
                return;
            }

            if (mWorkerHandler.hasMessages(MSG_PROCESS_TIMEOUT)) {
                Logger.i(TAG_BLE, "BleConnector.onServicesDiscovered success");

                Logger.i(TAG_BLE, "onServicesDiscovered refreshProfile");
                refreshProfile();

                Logger.i(TAG_BLE, "onServicesDiscovered checkProfile");
                if (checkProfile()) {
                    mWorkerHandler.removeMessages(MSG_PROCESS_TIMEOUT);

                    BaseRequest currentRequest = getCurrentRequest();
                    if (currentRequest instanceof ConnectRequest) {
                        synchronized (mProcessConnect133Lock) {
                            mConnect133Count = 0;
                        }

                        ConnectRequest connectRequest = (ConnectRequest) currentRequest;
                        if (connectRequest.callback != null) {
                            connectRequest.callback.sendSuccessMessage(null);
                        }
                    }

                    clearCurrentRequest();
                    scheduleNextRequest();
                } else {
                    Logger.e(TAG_BLE, "BleConnect.checkProfile false");

                    Logger.i(TAG_BLE, "BleUtil.clearGattCache");
                    BleUtil.clearGattCache(mBluetoothGatt);

                    mWorkerHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Logger.i(TAG_BLE, "BluetoothGatt.discoverServices (while onServicesDiscovered checkProfile)");
                            if (!mBluetoothGatt.discoverServices()) {
                                Logger.e(TAG_BLE, "BluetoothGatt.discoverServices false for checkProfile fail");
                            }
                        }
                    }, 1000);
                }
            } else {
                Logger.i(TAG_BLE, "BleConnector.onServicesDiscovered but timeout");
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (!isWorking()) {
                return;
            }

            if (mWorkerHandler.hasMessages(MSG_PROCESS_TIMEOUT)) {
                mWorkerHandler.removeMessages(MSG_PROCESS_TIMEOUT);

                BaseRequest currentRequest = getCurrentRequest();
                if (currentRequest instanceof ReadCharacterRequest) {
                    ReadCharacterRequest readRequest = (ReadCharacterRequest) currentRequest;
                    if (readRequest.callback != null) {
                        ReadResult readResult = new ReadResult();
                        readResult.characteristic = characteristic;
                        readResult.status = status;
                        readResult.value = characteristic.getValue();
                        readRequest.callback.sendSuccessMessage(readResult);
                    }
                }

                clearCurrentRequest();
                scheduleNextRequest();
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (!isWorking()) {
                return;
            }

            if (mWorkerHandler.hasMessages(MSG_PROCESS_TIMEOUT)) {
                mWorkerHandler.removeMessages(MSG_PROCESS_TIMEOUT);

                BaseRequest currentRequest = getCurrentRequest();
                if (currentRequest instanceof WriteCharacterRequest) {
                    WriteCharacterRequest writeRequest = (WriteCharacterRequest) currentRequest;
                    if (writeRequest.callback != null) {
                        WriteResult writeResult = new WriteResult();
                        writeResult.characteristic = characteristic;
                        writeResult.status = status;
                        writeRequest.callback.sendSuccessMessage(writeResult);
                    }
                }

                clearCurrentRequest();
                scheduleNextRequest();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic character) {
            if (!isWorking()) {
                return;
            }

            UUID serviceId = character.getService().getUuid();
            UUID characterId = character.getUuid();

            Logger.d(TAG_BLE, "BleConnector.onCharacterChanged       " + BleUUIDUtil.getValue(serviceId) + " " + BleUUIDUtil.getValue(characterId) + " " + ByteUtil.byteToString(character.getValue()));

            for (ConnectorListener connectorListener : mConnectorListeners) {
                connectorListener.onCharacterChanged(gatt, character);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (!isWorking()) {
                return;
            }

            if (mWorkerHandler.hasMessages(MSG_PROCESS_TIMEOUT)) {
                mWorkerHandler.removeMessages(MSG_PROCESS_TIMEOUT);

                BaseRequest currentRequest = getCurrentRequest();
                if (currentRequest instanceof ReadDescriptorRequest) {
                    ReadDescriptorRequest readDescriptorRequest = (ReadDescriptorRequest) currentRequest;
                    if (readDescriptorRequest.callback != null) {
                        ReadDescriptorResult readDescriptorResult = new ReadDescriptorResult();
                        readDescriptorResult.descriptor = descriptor;
                        readDescriptorResult.status = status;
                        readDescriptorResult.bytes = descriptor.getValue();
                        readDescriptorRequest.callback.sendSuccessMessage(readDescriptorResult);
                    }
                }

                clearCurrentRequest();
                scheduleNextRequest();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (!isWorking()) {
                return;
            }

            if (mWorkerHandler.hasMessages(MSG_PROCESS_TIMEOUT)) {
                mWorkerHandler.removeMessages(MSG_PROCESS_TIMEOUT);

                BaseRequest currentRequest = getCurrentRequest();
                if (currentRequest instanceof WriteDescriptorRequest) {
                    WriteDescriptorRequest writeDescriptorRequest = (WriteDescriptorRequest) currentRequest;
                    if (writeDescriptorRequest.callback != null) {
                        DescriptorWriteResult descriptorWriteResult = new DescriptorWriteResult();
                        descriptorWriteResult.descriptor = descriptor;
                        descriptorWriteResult.status = status;
                        writeDescriptorRequest.callback.sendSuccessMessage(descriptorWriteResult);
                    }

                } else if (currentRequest instanceof NotifyRequest) {
                    NotifyRequest notifyRequest = (NotifyRequest) currentRequest;
                    if (notifyRequest.callback != null) {
                        DescriptorWriteResult descriptorWriteResult = new DescriptorWriteResult();
                        descriptorWriteResult.descriptor = descriptor;
                        descriptorWriteResult.status = status;
                        notifyRequest.callback.sendSuccessMessage(descriptorWriteResult);
                    }
                } else if (currentRequest instanceof UnNotifyRequest) {
                    UnNotifyRequest unNotifyRequest = (UnNotifyRequest) currentRequest;
                    if (unNotifyRequest.callback != null) {
                        DescriptorWriteResult descriptorWriteResult = new DescriptorWriteResult();
                        descriptorWriteResult.descriptor = descriptor;
                        descriptorWriteResult.status = status;
                        unNotifyRequest.callback.sendSuccessMessage(descriptorWriteResult);
                    }
                } else if (currentRequest instanceof IndicationRequest) {
                    IndicationRequest indicationRequest = (IndicationRequest) currentRequest;
                    if (indicationRequest.callback != null) {
                        DescriptorWriteResult descriptorWriteResult = new DescriptorWriteResult();
                        descriptorWriteResult.descriptor = descriptor;
                        descriptorWriteResult.status = status;
                        indicationRequest.callback.sendSuccessMessage(descriptorWriteResult);
                    }
                } else if (currentRequest instanceof UnIndicationRequest) {
                    UnIndicationRequest unIndicationRequest = (UnIndicationRequest) currentRequest;
                    if (unIndicationRequest.callback != null) {
                        DescriptorWriteResult descriptorWriteResult = new DescriptorWriteResult();
                        descriptorWriteResult.descriptor = descriptor;
                        descriptorWriteResult.status = status;
                        unIndicationRequest.callback.sendSuccessMessage(descriptorWriteResult);
                    }
                }

                clearCurrentRequest();
                scheduleNextRequest();
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (!isWorking()) {
                return;
            }

            if (mWorkerHandler.hasMessages(MSG_PROCESS_TIMEOUT)) {
                mWorkerHandler.removeMessages(MSG_PROCESS_TIMEOUT);

                BaseRequest currentRequest = getCurrentRequest();
                if (currentRequest instanceof ReadRssiRequest) {
                    ReadRssiRequest rssiRequest = (ReadRssiRequest) currentRequest;
                    if (rssiRequest.callback != null) {
                        ReadRssiResult readRssiResult = new ReadRssiResult();
                        readRssiResult.rssi = rssi;
                        readRssiResult.status = status;
                        rssiRequest.callback.sendSuccessMessage(readRssiResult);
                    }
                }

                clearCurrentRequest();
                scheduleNextRequest();
            }
        }
    };

    BleConnector() {
    }

    void setMac(String mac) {
        mMac = mac;
    }

    private void refreshProfile() {
        List<BluetoothGattService> serviceList = mBluetoothGatt.getServices();

        Map<UUID, Map<UUID, BluetoothGattCharacteristic>> newProfile = new HashMap<>();

        for (BluetoothGattService service : serviceList) {
            UUID serviceUUID = service.getUuid();

            Map<UUID, BluetoothGattCharacteristic> map = newProfile.get(serviceUUID);

            if (map == null) {
                map = new HashMap<>();
                newProfile.put(service.getUuid(), map);
            }

            List<BluetoothGattCharacteristic> characters = service.getCharacteristics();

            for (BluetoothGattCharacteristic character : characters) {
                map.put(character.getUuid(), character);
            }
        }

        mProfile.clear();
        mProfile.putAll(newProfile);
    }

    private boolean checkProfile() {
        if (mProfile.isEmpty()) {
            return false;
        }

        Map<UUID, BluetoothGattCharacteristic> serviceMiMap = mProfile.get(SERVICE_MI);
        if (serviceMiMap == null) {
            return false;
        }

        BluetoothGattCharacteristic characterMiEvent = serviceMiMap.get(CHARACTER_MI_EVENT);
        if (characterMiEvent == null) {
            return false;
        }
        BluetoothGattCharacteristic characterMiToken = serviceMiMap.get(CHARACTER_MI_TOKEN);
        if (characterMiToken == null) {
            return false;
        }

        Map<UUID, BluetoothGattCharacteristic> serviceRyeexMap = mProfile.get(SERVICE_RYEEX);
        if (serviceRyeexMap == null) {
            return false;
        }
        BluetoothGattCharacteristic characterRyeexOpen = serviceRyeexMap.get(CHARACTER_RYEEX_OPEN);
        if (characterRyeexOpen == null) {
            return false;
        }
        BluetoothGattCharacteristic characterRyeexRc4 = serviceRyeexMap.get(CHARACTER_RYEEX_RC4);
        if (characterRyeexRc4 == null) {
            return false;
        }

        return true;
    }

    void connect(final AsyncCallback<Void, Error> callback) {
        mWorkerThread = new MessageHandlerThread("BleConnector-" + (sThreadNum++));
        mWorkerThread.start();
        mWorkerHandler = new WorkerHandler(mWorkerThread.getLooper());

        AsyncCallback<Void, Error> connectCallback = new AsyncCallback<Void, Error>() {
            @Override
            public void onSuccess(Void result) {
                synchronized (mConnectFailCountLock) {
                    mConnectFailCount = 0;
                }

                if (callback != null) {
                    callback.sendSuccessMessage(null);
                }
            }

            @Override
            public void onFailure(Error error) {
                synchronized (mConnectFailCountLock) {
                    mConnectFailCount++;
                }

                if (callback != null) {
                    callback.sendFailureMessage(error);
                }
            }
        };

        ConnectRequest connectRequest = new ConnectRequest(connectCallback);
        mRequestQueue.add(connectRequest);

        int connectFailCount;
        synchronized (mConnectFailCountLock) {
            connectFailCount = mConnectFailCount;
        }

        if (connectFailCount >= 5) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            boolean disableSuccess = bluetoothAdapter.disable();

            Logger.i(TAG_BLE, "BluetoothAdapter.disable " + disableSuccess);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean enableSuccess = bluetoothAdapter.enable();

                    Logger.i(TAG_BLE, "BluetoothAdapter.enable " + enableSuccess);
                }
            }, 500);

            synchronized (mConnectFailCountLock) {
                mConnectFailCount = 0;
            }
        }

        scheduleNextRequest();
    }

    synchronized void disconnect() {
        if (mWorkerHandler != null) {
            // 须清空所有
            mWorkerHandler.removeCallbacksAndMessages(null);
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        if (mWorkerThread != null) {
            mWorkerThread.quitSafely();
            mWorkerThread = null;
            mWorkerHandler = null;
        }

        mProfile.clear();
    }

    /**
     * 未连接或已断开连接时不工作
     *
     * @return
     */
    private synchronized boolean isWorking() {
        return mWorkerHandler != null;
    }

    void readCharacter(UUID service, UUID character, AsyncCallback<ReadResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        ReadCharacterRequest readCharacterRequest = new ReadCharacterRequest(service, character, callback);
        mRequestQueue.add(readCharacterRequest);

        scheduleNextRequest();
    }

    void writeCharacter(UUID service, UUID character, byte[] bytes, AsyncCallback<WriteResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        Logger.d(TAG_BLE, "BleConnector.writeCharacter           " + BleUUIDUtil.getValue(service) + " " + BleUUIDUtil.getValue(character) + " " + ByteUtil.byteToString(bytes));

        WriteCharacterRequest writeCharacterRequest = new WriteCharacterRequest(service, character, bytes, callback);
        mRequestQueue.add(writeCharacterRequest);

        scheduleNextRequest();
    }

    void writeCharacterWithoutRsp(UUID service, UUID character, byte[] bytes, AsyncCallback<Void, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        Logger.d(TAG_BLE, "BleConnector.writeCharacterWithoutRsp " + BleUUIDUtil.getValue(service) + " " + BleUUIDUtil.getValue(character) + " " + ByteUtil.byteToString(bytes));

        WriteCharacterWithoutRspRequest writeCharacterWithoutRspRequest = new WriteCharacterWithoutRspRequest(service, character, bytes, callback);
        mRequestQueue.add(writeCharacterWithoutRspRequest);

        scheduleNextRequest();
    }

    void readDescriptor(UUID service, UUID character, UUID descriptor, AsyncCallback<ReadDescriptorResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        ReadDescriptorRequest readDescriptorRequest = new ReadDescriptorRequest(service, character, descriptor, callback);
        mRequestQueue.add(readDescriptorRequest);

        scheduleNextRequest();
    }

    void writeDescriptor(UUID service, UUID character, UUID descriptor, byte[] value, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        WriteDescriptorRequest writeDescriptorRequest = new WriteDescriptorRequest(service, character, descriptor, value, callback);
        mRequestQueue.add(writeDescriptorRequest);

        scheduleNextRequest();

    }

    void notify(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        NotifyRequest notifyRequest = new NotifyRequest(service, character, callback);
        mRequestQueue.add(notifyRequest);

        scheduleNextRequest();
    }

    void unnotify(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        UnNotifyRequest unNotifyRequest = new UnNotifyRequest(service, character, callback);
        mRequestQueue.add(unNotifyRequest);

        scheduleNextRequest();
    }

    void indication(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        IndicationRequest indicationRequest = new IndicationRequest(service, character, callback);
        mRequestQueue.add(indicationRequest);

        scheduleNextRequest();
    }

    void unindication(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        UnIndicationRequest unIndicationRequest = new UnIndicationRequest(service, character,
                callback);
        mRequestQueue.add(unIndicationRequest);

        scheduleNextRequest();
    }

    void readRssi(AsyncCallback<ReadRssiResult, Error> callback) {
        if (!isWorking()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_WORKER_THREAD_NOT_READY, "connector worker-thread not ready"));
            }
            return;
        }

        ReadRssiRequest readRssiRequest = new ReadRssiRequest(callback);
        mRequestQueue.add(readRssiRequest);

        scheduleNextRequest();
    }

    private synchronized BluetoothGattCharacteristic getCharacterWorked(UUID service, UUID character) {
        BluetoothGattCharacteristic characteristic = null;

        if (service != null && character != null) {
            Map<UUID, BluetoothGattCharacteristic> characters = mProfile.get(service);
            if (characters != null) {
                characteristic = characters.get(character);
            }
        }

        if (characteristic == null) {
            if (mBluetoothGatt != null) {
                BluetoothGattService gattService = mBluetoothGatt.getService(service);
                if (gattService != null) {
                    characteristic = gattService.getCharacteristic(character);
                }
            }
        }

        return characteristic;
    }

    synchronized void addConnectorListener(ConnectorListener connectorListener) {
        if (connectorListener == null) {
            return;
        }
        mConnectorListeners.add(connectorListener);
    }

    synchronized void removeConnectorListener(ConnectorListener connectorListener) {
        if (mConnectorListeners.contains(connectorListener)) {
            mConnectorListeners.remove(connectorListener);
        }
    }

    private synchronized void scheduleNextRequest() {
        mWorkerHandler.sendEmptyMessage(MSG_PROCESS_REQUEST);
    }

    private synchronized BaseRequest getCurrentRequest() {
        return mCurrentRequest;
    }

    private synchronized void setCurrentRequest(BaseRequest request) {
        mCurrentRequest = request;
    }

    private synchronized void clearCurrentRequest() {
        mCurrentRequest = null;
    }

    private synchronized void processRequestWorked() {
        if (getCurrentRequest() != null) {
            // 当前请求还在处理中
            return;
        }

        BaseRequest request = mRequestQueue.poll();
        if (request == null) {
            return;
        }

        setCurrentRequest(request);

        if (request instanceof ConnectRequest) {
            ConnectRequest connectRequest = (ConnectRequest) request;

            synchronized (mProcessConnect133Lock) {
                mConnect133Count = 0;
            }

            Logger.i(TAG_BLE, "BleUtil.clearGattCache (before doConnectWorked)");
            BleUtil.clearGattCache(mBluetoothGatt);

            if (doConnectWorked(connectRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST_CONNECT);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof ReadCharacterRequest) {
            ReadCharacterRequest readCharacterRequest = (ReadCharacterRequest) request;

            if (doReadWorked(readCharacterRequest.service, readCharacterRequest.character, readCharacterRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof WriteCharacterRequest) {
            WriteCharacterRequest writeCharacterRequest = (WriteCharacterRequest) request;

            if (doWriteWorked(writeCharacterRequest.service, writeCharacterRequest.character, writeCharacterRequest.bytes, writeCharacterRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof WriteCharacterWithoutRspRequest) {
            WriteCharacterWithoutRspRequest writeCharacterNoAckRequest = (WriteCharacterWithoutRspRequest) request;

            if (doWriteWithoutRspWorked(writeCharacterNoAckRequest.service, writeCharacterNoAckRequest.character, writeCharacterNoAckRequest.bytes, writeCharacterNoAckRequest.callback)) {
                // 不用等Response
                if (writeCharacterNoAckRequest.callback != null) {
                    writeCharacterNoAckRequest.callback.sendSuccessMessage(null);
                }

                clearCurrentRequest();
                scheduleNextRequest();
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof ReadDescriptorRequest) {
            ReadDescriptorRequest readDescriptorRequest = (ReadDescriptorRequest) request;

            if (doReadDescriptorWorked(readDescriptorRequest.service, readDescriptorRequest.character, readDescriptorRequest.descriptor, readDescriptorRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof WriteDescriptorRequest) {
            WriteDescriptorRequest writeDescriptorRequest = (WriteDescriptorRequest) request;

            if (doWriteDescriptorWorked(writeDescriptorRequest.service, writeDescriptorRequest.character, writeDescriptorRequest.descriptor, writeDescriptorRequest.value, writeDescriptorRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof NotifyRequest) {
            NotifyRequest notifyRequest = (NotifyRequest) request;

            if (doNotifyWorked(notifyRequest.service, notifyRequest.character, true, notifyRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof UnNotifyRequest) {
            UnNotifyRequest unNotifyRequest = (UnNotifyRequest) request;

            if (doNotifyWorked(unNotifyRequest.service, unNotifyRequest.character, false, unNotifyRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof IndicationRequest) {
            IndicationRequest indicationRequest = (IndicationRequest) request;

            if (doIndicationWorked(indicationRequest.service, indicationRequest.character, true, indicationRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof UnIndicationRequest) {
            UnIndicationRequest unIndicationRequest = (UnIndicationRequest) request;

            if (doIndicationWorked(unIndicationRequest.service, unIndicationRequest.character, false, unIndicationRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else if (request instanceof ReadRssiRequest) {
            ReadRssiRequest readRssiRequest = (ReadRssiRequest) request;

            if (doReadRssi(readRssiRequest.callback)) {
                Message msg = Message.obtain(mWorkerHandler, MSG_PROCESS_TIMEOUT, request);
                mWorkerHandler.sendMessageDelayed(msg, TIMEOUT_REQUEST);
            } else {
                clearCurrentRequest();
                scheduleNextRequest();
            }
        } else {
            clearCurrentRequest();
            scheduleNextRequest();
        }
    }

    private synchronized boolean doConnectWorked(AsyncCallback<Void, Error> callback) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothDevice = bluetoothAdapter.getRemoteDevice(mMac);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothGatt = mBluetoothDevice.connectGatt(BleContext.sAppContext, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = mBluetoothDevice.connectGatt(BleContext.sAppContext, false, mBluetoothGattCallback);
            }
            return true;
        } catch (Exception e) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CONNECT_GATT_EXCEPTION, "connect exception"));
            }
            return false;
        }
    }

    private boolean doReadWorked(UUID service, UUID character, AsyncCallback<ReadResult, Error> callback) {
        if (mBluetoothGatt == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "mBluetoothGatt is null"));
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = getCharacterWorked(service, character);

        if (characteristic == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_IS_NULL, "characteristic is null"));
            }
            return false;
        }

        if (!isCharacteristicReadable(characteristic)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_NOT_READABLE, "characteristic is not readable"));
            }
            return false;
        }

        if (!mBluetoothGatt.readCharacteristic(characteristic)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_READ_FAIL, "readCharacteristic return false"));
            }
            return false;
        }

        return true;
    }

    private boolean doWriteWorked(UUID service, UUID character, byte[] bytes, AsyncCallback<WriteResult, Error> callback) {
        if (mBluetoothGatt == null) {
            Logger.e(TAG_BLE, "BleConnector.doWrite mBluetoothGatt is null");
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "doWriteWorked mBluetoothGatt is null"));
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = getCharacterWorked(service, character);

        if (characteristic == null) {
            Logger.e(TAG_BLE, "BleConnector.doWrite Characteristic is null, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_IS_NULL, "doWriteWorked characteristic is null"));
            }
            return false;
        }

        if (!isCharacteristicWritable(characteristic)) {
            Logger.e(TAG_BLE, "BleConnector.doWrite Characteristic is not writable, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_NOT_WRITABLE, "doWriteWorked characteristic is not writable"));
            }
            return false;
        }

        characteristic.setValue(bytes != null ? bytes : ByteUtil.EMPTY_BYTES);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
            Logger.e(TAG_BLE, "BleConnector.doWrite writeCharacteristic false, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));

//            if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
//                Logger.e(TAG_BLE, "BleConnector.doWrite writeCharacteristic retry false, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));
//                if (callback != null) {
//                    callback.sendFailureMessage(new Error(BLE_CHARACTER_WRITE_FAIL, "doWriteWorked writeCharacteristic false"));
//                }
//                return false;
//            }

            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_WRITE_FAIL, "doWriteWorked writeCharacter false"));
            }
            return false;
        }

        return true;
    }

    private boolean doWriteWithoutRspWorked(UUID service, UUID character, byte[] bytes, AsyncCallback<Void, Error> callback) {
        if (mBluetoothGatt == null) {
            Logger.e(TAG_BLE, "BleConnector.doWriteWithoutRsp mBluetoothGatt is null");
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "doWriteWithoutRspWorked mBluetoothGatt is null"));
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = getCharacterWorked(service, character);

        if (characteristic == null) {
            Logger.e(TAG_BLE, "BleConnector.doWriteWithoutRsp character is null, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_IS_NULL, "doWriteWithoutRspWorked character is null"));
            }
            return false;
        }

        if (!isCharacteristicNoRspWritable(characteristic)) {
            Logger.e(TAG_BLE, "BleConnector.doWriteWithoutRsp character is not writable, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_NOT_WRITABLE_NO_RESPONSE, "doWriteWithoutRspWorked character is not writable"));
            }
            return false;
        }

        characteristic.setValue(bytes != null ? bytes : ByteUtil.EMPTY_BYTES);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
            Logger.e(TAG_BLE, "BleConnector.doWriteWithoutRsp writeCharacter false, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));

//            if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
//                Logger.e(TAG_BLE, "BleConnector.doWriteWithoutRsp writeCharacter retry false, service:" + BleUUIDUtil.getValue(service) + " character:" + BleUUIDUtil.getValue(character));
//                if (callback != null) {
//                    callback.sendFailureMessage(new Error(BLE_CHARACTER_WRITE_FAIL, "doWriteWithoutRspWorked writeCharacter false"));
//                }
//                return false;
//            }

            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_WRITE_FAIL, "doWriteWithoutRspWorked writeCharacter false"));
            }
            return false;
        }

        return true;
    }

    private boolean doReadDescriptorWorked(UUID service, UUID character, UUID descriptor, AsyncCallback<ReadDescriptorResult, Error> callback) {
        if (mBluetoothGatt == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "mBluetoothGatt is null"));
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = getCharacterWorked(service, character);

        if (characteristic == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_IS_NULL, "characteristic is null"));
            }
            return false;
        }

        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(descriptor);
        if (gattDescriptor == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_IS_NULL, "gattDescriptor is null"));
            }
            return false;
        }

        if (!mBluetoothGatt.readDescriptor(gattDescriptor)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_READ_FAIL, "readDescriptor return false"));
            }
            return false;
        }

        return true;
    }

    private boolean doWriteDescriptorWorked(UUID service, UUID character, UUID descriptor, byte[] value, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (mBluetoothGatt == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "mBluetoothGatt is null"));
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = getCharacterWorked(service, character);

        if (characteristic == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_IS_NULL, "characteristic is null"));
            }
            return false;
        }

        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(descriptor);
        if (gattDescriptor == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_IS_NULL, "gattDescriptor is null"));
            }
            return false;
        }

        gattDescriptor.setValue(value != null ? value : ByteUtil.EMPTY_BYTES);

        if (!mBluetoothGatt.writeDescriptor(gattDescriptor)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_WRITE_FAIL, "writeDescriptor return false"));
            }
            return false;
        }

        return true;
    }

    private boolean doNotifyWorked(UUID service, UUID character, boolean enable, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (mBluetoothGatt == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "mBluetoothGatt is null"));
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = getCharacterWorked(service, character);

        if (characteristic == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_IS_NULL, "character is null"));
            }
            return false;
        }

        if (!isCharacteristicNotifiable(characteristic)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_NOT_NOTIFIABLE, "character is not notifiable"));
            }
            return false;
        }

        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_SET_NOTIFIABLE_FAIL, "setCharacteristicNotification return false"));
            }
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

        if (descriptor == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_IS_NULL, "getDescriptor for notify null"));
            }
            return false;
        }

        byte[] value = (enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        if (!descriptor.setValue(value)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_SET_VALUE_FAIL, "setValue for notify descriptor failed"));
            }
            return false;
        }

        if (!mBluetoothGatt.writeDescriptor(descriptor)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_WRITE_FAIL, "writeDescriptor return false"));
            }
            return false;
        }

        return true;
    }

    private boolean doIndicationWorked(UUID service, UUID character, boolean enable, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (mBluetoothGatt == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "mBluetoothGatt is null"));
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = getCharacterWorked(service, character);

        if (characteristic == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_IS_NULL, "characteristic not exist"));
            }
            return false;
        }

        if (!isCharacteristicIndicatable(characteristic)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_NOT_INDICATABLE, "characteristic not indicatable"));
            }
            return false;
        }

        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CHARACTER_SET_INDICATABLE_FAIL, "setCharacteristicIndication failed"));
            }
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

        if (descriptor == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_IS_NULL, "getDescriptor for indicate null"));
            }
            return false;
        }

        byte[] value = (enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        if (!descriptor.setValue(value)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_SET_VALUE_FAIL, "setValue for indicate descriptor failed"));
            }
            return false;
        }

        if (!mBluetoothGatt.writeDescriptor(descriptor)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DESCRIPTOR_WRITE_FAIL, "writeDescriptor for indicate failed"));
            }
            return false;
        }

        return true;
    }

    private boolean doReadRssi(AsyncCallback<ReadRssiResult, Error> callback) {
        if (mBluetoothGatt == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_GATT_IS_NULL, "ble gatt null"));
            }
            return false;
        }

        if (!mBluetoothGatt.readRemoteRssi()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_RSSI_READ_FAIL, "readRemoteRssi failed"));
            }
            return false;
        }

        return true;
    }

    private void doTimeoutWorked() {
        Logger.i(TAG_BLE, "BleConnector.doTimeoutWorked");

        doDisconnectCallback(new Error(BLE_TIMEOUT, "timeout"));

        // 超时断开连接
        disconnect();

        for (ConnectorListener connectorListener : mConnectorListeners) {
            connectorListener.onDisconnected(PROCESS_TIMEOUT);
        }
    }

    void doDisconnectCallback(Error cause) {
        BaseRequest currentRequest = getCurrentRequest();
        if (currentRequest != null) {
            AsyncCallback currentCallback = currentRequest.getCallback();
            if (currentCallback != null) {
                currentCallback.sendFailureMessage(cause);
            }
            clearCurrentRequest();
        }

        while (mRequestQueue.peek() != null) {
            BaseRequest waitingRequest = mRequestQueue.poll();
            if (waitingRequest != null) {
                AsyncCallback waitingCallback = waitingRequest.getCallback();
                if (waitingCallback != null) {
                    waitingCallback.sendFailureMessage(cause);
                }
            }
        }
        mRequestQueue.clear();
    }

    enum DisconnectedCause {
        UNKNOWN, STATUS_EQUAL_8, PROCESS_TIMEOUT
    }

    interface ConnectorListener {
        void onDisconnected(DisconnectedCause cause);

        void onCharacterChanged(BluetoothGatt gatt, BluetoothGattCharacteristic character);
    }

    private class WorkerHandler extends Handler {
        WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROCESS_REQUEST:
                    processRequestWorked();
                    break;
                case MSG_PROCESS_TIMEOUT:
                    doTimeoutWorked();
                    break;
            }
        }
    }

}
