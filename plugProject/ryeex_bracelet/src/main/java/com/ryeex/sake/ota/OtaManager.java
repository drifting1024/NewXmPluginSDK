package com.ryeex.sake.ota;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import com.ryeex.groot.lib.ble.BleManager;
import com.ryeex.groot.lib.ble.requestresult.DescriptorWriteResult;
import com.ryeex.groot.lib.ble.stack.app.BleApi;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevice;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBFirmware;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.common.util.FileUtil;
import com.ryeex.groot.lib.common.util.ZipUtil;
import com.ryeex.groot.lib.log.Logger;
import com.ryeex.sake.Device;
import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.device.api.BtFirmwareUpdateInfo;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_OPEN;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_RC4;
import static com.ryeex.groot.lib.ble.BleSetting.SERVICE_RYEEX;
import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;

public class OtaManager {
    public static final String TAG = "groot-ota";

    public static final String KEY_DID = "did";

    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_ERROR = "error";

    public static final String ACTION_DOWNLOAD_START = "com.ryeex.groot.action.otamanager.download_start";
    public static final String ACTION_DOWNLOAD_PROGRESS = "com.ryeex.groot.action.otamanager.download_progress";
    public static final String ACTION_DOWNLOAD_SUCCESS = "com.ryeex.groot.action.otamanager.download_success";
    public static final String ACTION_DOWNLOAD_FAILURE = "com.ryeex.groot.action.otamanager.download_failure";
    public static final String ACTION_TRANSFER_START = "com.ryeex.groot.action.otamanager.transfer_start";
    public static final String ACTION_TRANSFER_PROGRESS = "com.ryeex.groot.action.otamanager.transfer_progress";
    public static final String ACTION_TRANSFER_SUCCESS = "com.ryeex.groot.action.otamanager.transfer_success";
    public static final String ACTION_TRANSFER_FAILURE = "com.ryeex.groot.action.otamanager.transfer_failure";
    public static final String ACTION_WAIT_REBOOT_START = "com.ryeex.groot.action.otamanager.wait_reboot_start";
    public static final String ACTION_WAIT_REBOOT_PROGRESS = "com.ryeex.groot.action.otamanager.wait_reboot_progress";
    public static final String ACTION_WAIT_REBOOT_SUCCESS = "com.ryeex.groot.action.otamanager.wait_reboot_success";
    public static final String ACTION_WAIT_REBOOT_FAILURE = "com.ryeex.groot.action.otamanager.wait_reboot_failure";
    public static final String ACTION_FINAL_SUCCESS = "com.ryeex.groot.action.otamanager.final_success";

    private static final int MSG_WAIT_REBOOT_START_CONNECT = 1;
    private static final int MSG_WAIT_REBOOT_TIMEOUT = 2;
    private static final int MSG_WAIT_REBOOT_REFRESH_UI = 3;
    private static final long WAIT_REBOOT_START_CONNECT_DELAY_TIME = 10 * DateUtils.SECOND_IN_MILLIS;
    private static final long WAIT_REBOOT_START_CONNECT_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;
    private static final long WAIT_REBOOT_REFRESH_UI_TIME = 2 * DateUtils.SECOND_IN_MILLIS;
    private static final long WAIT_REBOOT_TIMEOUT_TIME = 4 * DateUtils.MINUTE_IN_MILLIS;

    private static OtaManager sInstance;
    private static Object sLock = new Object();
    Context mAppContext;

    Map<String, OtaRecord> mOtaRecordMap = new ConcurrentHashMap<>();


    private OtaManager() {
    }

    public static OtaManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new OtaManager();
                }
            }
        }
        return sInstance;
    }

    public void setAppContext(Context appContext) {
        mAppContext = appContext;
    }

    private void startDownloadAllFirmware(BtFirmwareUpdateInfo btUpdateInfo, final AsyncCallback<Pair<List<SingleOtaItem>, String>, Error> callback) {
        Logger.i(TAG_BLE, "OtaManager.startDownloadAllFirmware");

        XmPluginHostApi.instance().downloadBleFirmware(btUpdateInfo.url, new Response.BleUpgradeResponse() {
            @Override
            public void onProgress(int progress) {
                if (callback != null) {
                    callback.sendProgressMessage(progress);
                }
            }

            @Override
            public void onResponse(int code, String filePath) {
                if (code == 0) {
                    File zipFile = new File(filePath);
                    File unzipFile = new File(filePath + ".unzip");
                    try {
                        ZipUtil.unzip(zipFile, unzipFile);
                    } catch (IOException e) {
                        Logger.e(TAG_BLE, "XmPluginHostApi.downloadBleFirmware onResponse unzip exception " + e);

                        if (callback != null) {
                            callback.sendFailureMessage(new Error(-1, "unzip fail " + e));
                        }
                        return;
                    }

                    if (unzipFile.isDirectory()) {
                        List<SingleOtaItem> otaItemList = new ArrayList<>();
                        String version = null;

                        File[] files = unzipFile.listFiles();

                        for (int i = 0, len = files.length; i < len; i++) {
                            File file = files[i];

                            if (file.getName().endsWith(".bin")) {
                                SingleOtaItem otaItem = new SingleOtaItem();
                                otaItem.mLocalPath = file.getPath();
                                otaItem.mFwId = Integer.parseInt(file.getName().replace(".bin", ""));
                                otaItem.mLen = (int) file.length();

                                otaItemList.add(otaItem);
                            } else if (file.getName().equalsIgnoreCase("version.json")) {
                                try {
                                    JSONObject otaInfoJsonObj = new JSONObject(FileUtil.readTxtFromFile(file.getPath()));
                                    version = otaInfoJsonObj.getString("version");
                                } catch (JSONException e) {
                                    version = null;
                                }
                            }
                        }

                        if (otaItemList.size() <= 0 || TextUtils.isEmpty(version)) {
                            Logger.e(TAG_BLE, "XmPluginHostApi.downloadBleFirmware onResponse parse unzipFile fail otaItemList.size:" + otaItemList.size() + " version:" + version);

                            if (callback != null) {
                                callback.sendFailureMessage(new Error(-1, ""));
                            }
                        } else {
                            if (callback != null) {
                                callback.sendSuccessMessage(new Pair<>(otaItemList, version));
                            }
                        }

                    } else {
                        Logger.e(TAG_BLE, "XmPluginHostApi.downloadBleFirmware onResponse unzipFile not directory");

                        if (callback != null) {
                            callback.sendFailureMessage(new Error(-1, ""));
                        }
                    }

                } else {
                    Logger.e(TAG_BLE, "XmPluginHostApi.downloadBleFirmware fail code:" + code);

                    if (callback != null) {
                        callback.sendFailureMessage(new Error(-1, ""));
                    }
                }
            }
        });
    }

    public boolean isOtaing(Device device) {
        return mOtaRecordMap.containsKey(device.getDid());
    }

    public void startFirmwareUpdate(final Device device, final BleManager bleManager, final BtFirmwareUpdateInfo btUpdateInfo) {
        Logger.i(TAG_BLE, "OtaManager.startFirmwareUpdate");

        final String did = device.getDid();

        if (mOtaRecordMap.containsKey(did)) {
            return;
        } else {
            OtaRecord otaRecord = new OtaRecord();
            otaRecord.did = did;
            mOtaRecordMap.put(did, otaRecord);
        }

        sendDownloadStartBroadcast(device);

        startDownloadAllFirmware(btUpdateInfo, new AsyncCallback<Pair<List<SingleOtaItem>, String>, Error>() {
            @Override
            public void onSuccess(Pair<List<SingleOtaItem>, String> pair) {
                sendDownloadSuccessBroadcast(device);

                sendTransferStartBroadcast(device);

                startTransferAll(bleManager, btUpdateInfo, pair.first, pair.second, new AsyncCallback<Void, Error>() {
                    @Override
                    public void onSuccess(Void result) {
                        sendTransferSuccessBroadcast(device);

                        sendWaitRebootStartBroadcast(device);

                        startWaitReboot(device, bleManager, btUpdateInfo, new AsyncCallback<Void, Error>() {
                            @Override
                            public void onSuccess(Void result) {
                                mOtaRecordMap.remove(did);

                                sendWaitRebootSuccessBroadcast(device);

                                sendFinalSuccessBroadcast(device);
                            }

                            @Override
                            public void onProgress(float progress) {
                                sendWaitRebootProgressBroadcast(device, progress);
                            }

                            @Override
                            public void onFailure(Error error) {
                                Logger.e("", "OtaManager.startWaitReboot fail:" + error);

                                mOtaRecordMap.remove(did);

                                sendWaitRebootFailureBroadcast(device, error);
                            }
                        });
                    }

                    @Override
                    public void onProgress(float progress) {
                        Log.d("tuhuolong", "onProgress:" + progress);
                        sendTransferProgressBroadcast(device, progress);
                    }

                    @Override
                    public void onFailure(Error error) {
                        Logger.e("", "OtaManager.startTransferAll fail:" + error);

                        mOtaRecordMap.remove(did);

                        sendTransferFailureBroadcast(device, error);
                    }
                });
            }

            @Override
            public void onProgress(float progress) {
                sendDownloadProgressBroadcast(device, progress);
            }

            @Override
            public void onFailure(Error error) {
                Logger.e("", "OtaManager.startDownloadAllFirmware fail:" + error);

                mOtaRecordMap.remove(did);

                sendDownloadFailureBroadcast(device, error);
            }
        });
    }

    private void startTransferAll(final BleManager bleManager, final BtFirmwareUpdateInfo btUpdateInfo, final List<SingleOtaItem> singleOtaItemList, String version, final AsyncCallback<Void, Error> callback) {
        PBFirmware.FwUpdateInfo.Builder fwUpdateInfoBuilder = PBFirmware.FwUpdateInfo.newBuilder();
        fwUpdateInfoBuilder.setForce(false);
        fwUpdateInfoBuilder.setVersion(version);

        int totalLen = 0;

        for (int i = 0, len = singleOtaItemList.size(); i < len; i++) {
            SingleOtaItem singleOtaItem = singleOtaItemList.get(i);
            PBFirmware.FwUpdateItem.Builder fwUpdateItemBuilder = PBFirmware.FwUpdateItem.newBuilder();
            fwUpdateItemBuilder.setFwId(singleOtaItem.mFwId);
            fwUpdateItemBuilder.setLength(singleOtaItem.mLen);
            PBFirmware.FwUpdateItem fwUpdateItem = fwUpdateItemBuilder.build();
            fwUpdateInfoBuilder.addItems(fwUpdateItem);

            totalLen += singleOtaItem.mLen;
        }

        final int totalLength = totalLen;

        final PBFirmware.FwUpdateInfo fwUpdateInfo = fwUpdateInfoBuilder.build();

        BleApi.startFirmwareUpdateStart(bleManager, fwUpdateInfo, new AsyncCallback<PBFirmware.FwUpdateStartResult, Error>() {
            @Override
            public void onSuccess(PBFirmware.FwUpdateStartResult startResult) {
                final int transferredLength = startResult.getTransferedLength();
                int currentTotalLength = 0;
                int firstFileIndex = 0;
                int firstFileStartBytes = 0;
                for (int i = 0, len = singleOtaItemList.size(); i < len; i++) {
                    currentTotalLength += singleOtaItemList.get(i).mLen;

                    if (currentTotalLength < transferredLength) {
                        continue;
                    } else if (currentTotalLength == transferredLength) {
                        firstFileIndex = i + 1;
                        firstFileStartBytes = 0;
                        break;
                    } else {
                        firstFileIndex = i;
                        firstFileStartBytes = singleOtaItemList.get(i).mLen - (currentTotalLength - transferredLength);
                        break;
                    }
                }

                final int startIndex = firstFileIndex;
                final int startBytes = firstFileStartBytes;

                final int MSG_TRANSFER_SINGLE = 1;

                final Handler transferHandler = new Handler(Looper.myLooper()) {

                    int currentIndex = startIndex;
                    int currentStartBytes = startBytes;
                    int currentTransferredWholeLength = transferredLength;

                    @Override
                    public void dispatchMessage(Message msg) {
                        switch (msg.what) {
                            case MSG_TRANSFER_SINGLE:
                                final SingleOtaItem singleOtaItem = singleOtaItemList.get(currentIndex);

                                transferSingleFile(bleManager, singleOtaItem, currentStartBytes, new AsyncCallback<Void, Error>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        currentTransferredWholeLength += singleOtaItem.mLen - currentStartBytes;

                                        currentIndex++;
                                        currentStartBytes = 0;

                                        if (currentIndex < singleOtaItemList.size()) {
                                            sendEmptyMessageDelayed(MSG_TRANSFER_SINGLE, 500);
                                        } else {
                                            if (callback != null) {
                                                callback.sendProgressMessage(1.0f);
                                            }

                                            if (callback != null) {
                                                callback.sendSuccessMessage(null);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onProgress(float progress) {
                                        float totalProgress = (currentTransferredWholeLength + (singleOtaItem.mLen - currentStartBytes) * progress) / totalLength;

                                        if (callback != null) {
                                            callback.sendProgressMessage(totalProgress);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Error error) {
                                        if (callback != null) {
                                            callback.sendFailureMessage(error);
                                        }
                                    }
                                });
                                break;
                        }
                    }
                };

                transferHandler.sendEmptyMessage(MSG_TRANSFER_SINGLE);
            }

            @Override
            public void onFailure(Error error) {
                Logger.e("", "BleApi.startFirmwareUpdateStart failure " + error);

                if (callback != null) {
                    callback.sendFailureMessage(error);
                }
            }
        });
    }

    private void transferSingleFile(BleManager bleManager, final SingleOtaItem singleOtaItem, int startBytes, final AsyncCallback<Void, Error> callback) {
        Logger.d(TAG_BLE, "OtaManager.transferSingleFile");

        byte[] wholeFileBytes = FileUtil.readFile(singleOtaItem.mLocalPath);

        byte[] transferFileBytes;
        if (startBytes == 0) {
            transferFileBytes = wholeFileBytes;
        } else {
            transferFileBytes = new byte[singleOtaItem.mLen - startBytes];
            System.arraycopy(wholeFileBytes, startBytes, transferFileBytes, 0, transferFileBytes.length);
        }

        if (ByteUtil.isEmpty(transferFileBytes)) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "transfer file bytes is null"));
            }
            return;
        }

        BleApi.startFirmwareUpdateFile(bleManager, transferFileBytes, new AsyncCallback<Void, Error>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) {
                    callback.sendSuccessMessage(null);
                }
            }

            @Override
            public void onProgress(float progress) {
                if (callback != null) {
                    callback.sendProgressMessage(progress);
                }
            }

            @Override
            public void onFailure(Error error) {
                Logger.e("", "BleApi.startFirmwareUpdateFile failure " + error);

                if (callback != null) {
                    callback.sendFailureMessage(error);
                }
            }
        });
    }

    private void startWaitReboot(final Device device, final BleManager bleManager, final BtFirmwareUpdateInfo btUpdateInfo, final AsyncCallback<Void, Error> callback) {
        final Handler waitRebootTimeoutHandler = new Handler(Looper.myLooper()) {
            float mRebootProgress = 0;

            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_WAIT_REBOOT_START_CONNECT:
                        removeMessages(MSG_WAIT_REBOOT_START_CONNECT);

                        final Handler waitRebootTimeoutHandler = this;

                        Logger.i(TAG, "OtaManager.startWaitReboot XmBluetoothManager.secureConnect");

                        XmBluetoothManager.getInstance().secureConnect(device.getMac(), new Response.BleConnectResponse() {
                            @Override
                            public void onResponse(int code, Bundle bundle) {
                                Logger.i(TAG, "OtaManager.startWaitReboot XmBluetoothManager.secureConnect onResponse code:" + code);
                                if (code == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                    bleManager.connect(new AsyncCallback<Void, Error>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            Logger.i(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_OPEN");
                                            bleManager.notify(SERVICE_RYEEX, CHARACTER_RYEEX_OPEN, new AsyncCallback<DescriptorWriteResult, Error>() {
                                                @Override
                                                public void onSuccess(DescriptorWriteResult result) {
                                                    Logger.i(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_RC4");
                                                    bleManager.notify(SERVICE_RYEEX, CHARACTER_RYEEX_RC4, new AsyncCallback<DescriptorWriteResult, Error>() {
                                                        @Override
                                                        public void onSuccess(DescriptorWriteResult result) {
                                                            Logger.d(TAG, "OtaManager.startWaitReboot BleApi.getDeviceInfo");

                                                            BleApi.getDeviceInfo(bleManager, new AsyncCallback<PBDevice.DeviceInfo, Error>() {
                                                                @Override
                                                                public void onSuccess(PBDevice.DeviceInfo deviceInfo) {
                                                                    if (deviceInfo.getFwVer().equals(btUpdateInfo.version)) {

                                                                        if (waitRebootTimeoutHandler.hasMessages(MSG_WAIT_REBOOT_TIMEOUT)) {
                                                                            waitRebootTimeoutHandler.removeCallbacksAndMessages(null);

                                                                            if (callback != null) {
                                                                                callback.sendSuccessMessage(null);
                                                                            }
                                                                        }
                                                                    } else {
                                                                        Logger.i(TAG, "OtaManager.startWaitReboot BleApi.getDeviceInfo onSuccess, wrong version, currentVersion:" + deviceInfo.getFwVer() + " targetVersion:" + btUpdateInfo.version);
                                                                        bleManager.disconnect(null);
                                                                        waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onFailure(Error error) {
                                                                    Logger.e(TAG, "OtaManager.startWaitReboot BleApi.getDeviceInfo onFailure " + error);

                                                                    bleManager.disconnect(null);

                                                                    waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
                                                                }
                                                            });
                                                        }

                                                        @Override
                                                        public void onFailure(Error error) {
                                                            Logger.e(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_RC4 onFailure " + error);

                                                            bleManager.disconnect(null);

                                                            waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onFailure(Error error) {
                                                    Logger.e(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_OPEN onFailure " + error);

                                                    bleManager.disconnect(null);

                                                    waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(Error error) {
                                            Logger.e(TAG, "OtaManager.startWaitReboot BleManager.connect onFailure " + error);
                                            waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
                                        }
                                    });
                                } else {
                                    waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
                                }
                            }
                        });

//                        bleManager.connect(new AsyncCallback<Void, Error>() {
//                            @Override
//                            public void onSuccess(Void result) {
//                                Logger.i(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_OPEN");
//                                bleManager.notify(SERVICE_RYEEX, CHARACTER_RYEEX_OPEN, new AsyncCallback<DescriptorWriteResult, Error>() {
//                                    @Override
//                                    public void onSuccess(DescriptorWriteResult result) {
//                                        Logger.i(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_RC4");
//                                        bleManager.notify(SERVICE_RYEEX, CHARACTER_RYEEX_RC4, new AsyncCallback<DescriptorWriteResult, Error>() {
//                                            @Override
//                                            public void onSuccess(DescriptorWriteResult result) {
//                                                Logger.d(TAG, "OtaManager.startWaitReboot BleApi.getDeviceInfo");
//
//                                                BleApi.getDeviceInfo(bleManager, new AsyncCallback<PBDevice.DeviceInfo, Error>() {
//                                                    @Override
//                                                    public void onSuccess(PBDevice.DeviceInfo deviceInfo) {
//                                                        if (deviceInfo.getFwVer().equals(btUpdateInfo.version)) {
//
//                                                            if (waitRebootTimeoutHandler.hasMessages(MSG_WAIT_REBOOT_TIMEOUT)) {
//                                                                waitRebootTimeoutHandler.removeCallbacksAndMessages(null);
//
//                                                                if (callback != null) {
//                                                                    callback.sendSuccessMessage(null);
//                                                                }
//                                                            }
//                                                        } else {
//                                                            Logger.i(TAG, "OtaManager.startWaitReboot BleApi.getDeviceInfo onSuccess, wrong version, currentVersion:" + deviceInfo.getFwVer() + " targetVersion:" + btUpdateInfo.version);
//                                                            bleManager.disconnect(null);
//                                                            waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
//                                                        }
//                                                    }
//
//                                                    @Override
//                                                    public void onFailure(Error error) {
//                                                        Logger.e(TAG, "OtaManager.startWaitReboot BleApi.getDeviceInfo onFailure " + error);
//
//                                                        bleManager.disconnect(null);
//
//                                                        waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
//                                                    }
//                                                });
//                                            }
//
//                                            @Override
//                                            public void onFailure(Error error) {
//                                                Logger.e(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_RC4 onFailure " + error);
//
//                                                bleManager.disconnect(null);
//
//                                                waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
//                                            }
//                                        });
//                                    }
//
//                                    @Override
//                                    public void onFailure(Error error) {
//                                        Logger.e(TAG, "OtaManager.startWaitReboot BleManager.notify CHARACTER_RYEEX_OPEN onFailure " + error);
//
//                                        bleManager.disconnect(null);
//
//                                        waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
//                                    }
//                                });
//                            }
//
//                            @Override
//                            public void onFailure(Error error) {
//                                Logger.e(TAG, "OtaManager.startWaitReboot BleManager.connect onFailure " + error);
//                                waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_INTERVAL);
//                            }
//                        });
                        break;
                    case MSG_WAIT_REBOOT_REFRESH_UI:
                        mRebootProgress = ((WAIT_REBOOT_START_CONNECT_DELAY_TIME + WAIT_REBOOT_TIMEOUT_TIME) * mRebootProgress + WAIT_REBOOT_REFRESH_UI_TIME) / (float) (WAIT_REBOOT_START_CONNECT_DELAY_TIME + WAIT_REBOOT_TIMEOUT_TIME);
                        sendEmptyMessageDelayed(MSG_WAIT_REBOOT_REFRESH_UI, WAIT_REBOOT_REFRESH_UI_TIME);
                        if (callback != null) {
                            callback.sendProgressMessage(mRebootProgress);
                        }
                        break;
                    case MSG_WAIT_REBOOT_TIMEOUT:
                        removeCallbacksAndMessages(null);
                        if (callback != null) {
                            callback.sendFailureMessage(new Error(-1, "wait reboot timeout"));
                        }
                        break;
                }
            }
        };

        bleManager.disconnect(null);

        waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_REFRESH_UI, WAIT_REBOOT_REFRESH_UI_TIME);
        waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_START_CONNECT, WAIT_REBOOT_START_CONNECT_DELAY_TIME);
        waitRebootTimeoutHandler.sendEmptyMessageDelayed(MSG_WAIT_REBOOT_TIMEOUT, WAIT_REBOOT_TIMEOUT_TIME);
    }

    private void sendDownloadStartBroadcast(Device device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_DOWNLOAD_START);
        param.putExtra(KEY_DID, device.getDid());
        localBroadManager.sendBroadcast(param);
    }

    private void sendDownloadProgressBroadcast(Device device, float progress) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_DOWNLOAD_PROGRESS);
        param.putExtra(KEY_DID, device.getDid());
        param.putExtra(KEY_PROGRESS, progress);
        localBroadManager.sendBroadcast(param);
    }

    private void sendDownloadSuccessBroadcast(Device device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_DOWNLOAD_SUCCESS);
        param.putExtra(KEY_DID, device.getDid());
        localBroadManager.sendBroadcast(param);
    }

    private void sendDownloadFailureBroadcast(Device device, Error error) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_DOWNLOAD_FAILURE);
        param.putExtra(KEY_DID, device.getDid());
        param.putExtra(KEY_ERROR, error);
        localBroadManager.sendBroadcast(param);
    }

    private void sendTransferStartBroadcast(Device device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_TRANSFER_START);
        param.putExtra(KEY_DID, device.getDid());
        localBroadManager.sendBroadcast(param);
    }

    private void sendTransferProgressBroadcast(Device device, float progress) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_TRANSFER_PROGRESS);
        param.putExtra(KEY_DID, device.getDid());
        param.putExtra(KEY_PROGRESS, progress);
        localBroadManager.sendBroadcast(param);
    }

    private void sendTransferSuccessBroadcast(Device device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_TRANSFER_SUCCESS);
        param.putExtra(KEY_DID, device.getDid());
        localBroadManager.sendBroadcast(param);
    }

    private void sendTransferFailureBroadcast(Device device, Error error) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_TRANSFER_FAILURE);
        param.putExtra(KEY_DID, device.getDid());
        param.putExtra(KEY_ERROR, error);
        localBroadManager.sendBroadcast(param);
    }

    private void sendWaitRebootStartBroadcast(Device device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_WAIT_REBOOT_START);
        param.putExtra(KEY_DID, device.getDid());
        localBroadManager.sendBroadcast(param);
    }

    private void sendWaitRebootProgressBroadcast(Device device, float progress) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_WAIT_REBOOT_PROGRESS);
        param.putExtra(KEY_DID, device.getDid());
        param.putExtra(KEY_PROGRESS, progress);
        localBroadManager.sendBroadcast(param);
    }

    private void sendWaitRebootSuccessBroadcast(Device device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_WAIT_REBOOT_SUCCESS);
        param.putExtra(KEY_DID, device.getDid());
        localBroadManager.sendBroadcast(param);
    }

    private void sendWaitRebootFailureBroadcast(Device device, Error error) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_WAIT_REBOOT_FAILURE);
        param.putExtra(KEY_DID, device.getDid());
        param.putExtra(KEY_ERROR, error);
        localBroadManager.sendBroadcast(param);
    }

    private void sendFinalSuccessBroadcast(Device device) {
        LocalBroadcastManager localBroadManager = LocalBroadcastManager.getInstance(mAppContext);
        Intent param = new Intent(ACTION_FINAL_SUCCESS);
        param.putExtra(KEY_DID, device.getDid());
        localBroadManager.sendBroadcast(param);
    }

    public enum OtaStatus {
        PENDING, DOWNLOADING, DOWNLOAD_FAILURE, TRANSFERRING, TRANSFER_FAILURE, WAIT_REBOOTING, WAIT_REBOOTING_FAILURE, UPDATE_SUCCESS
    }

    private static class SingleOtaItem {
        public int mFwId;
        public int mLen;
        public String mLocalPath;
    }

    private static class OtaRecord {
        String did;
    }
}
