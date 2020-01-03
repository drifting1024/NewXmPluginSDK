package com.ryeex.groot.lib.ble.stack.splitpackage;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateUtils;

import com.ryeex.groot.lib.ble.BleManager;
import com.ryeex.groot.lib.ble.BleSetting;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.thread.MessageHandlerThread;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.log.Logger;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_ACK_PACKAGE_IS_NOT_READY;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_ACK_PACKAGE_IS_NOT_SUCCESS;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_ACK_PACKAGE_IS_NULL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_BLE_MANAGER_IS_NULL;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_DISCONNECTED;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_ON_CHARACTER_CHANGED_TIMEOUT;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_SEND_DATA_PARAM_ERROR;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_SEND_DATA_RETRY_TOO_MUCH;
import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;
import static com.ryeex.groot.lib.ble.stack.splitpackage.DataPackage.MAX_PAYLOAD;
import static com.ryeex.groot.lib.common.util.ByteUtil.getBytes;
import static com.ryeex.groot.lib.common.util.ByteUtil.intToBytes;

/**
 * 分包协议
 * <p>
 * Created by chenhao on 2017/11/15.
 */
public class SplitPackage {
    private static final int MSG_DO_SEND = 1;

    private static Object sLock = new Object();
    private static MessageHandlerThread sWorkerThread;
    private static Handler sWorkerHandler;
    private static boolean sIsSending = false;
    private static Object sIsSendingLock = new Object();
    // 发送队列，保证每次发送的原子性
    private static Queue<SendRecord> sTransferQueue = new ConcurrentLinkedQueue<>();

    private static Handler getHandler() {
        if (sWorkerThread == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sWorkerThread == null) {
                    sWorkerThread = new MessageHandlerThread("SplitPackageWorkThread");
                    sWorkerThread.start();
                    sWorkerHandler = new Handler(sWorkerThread.getLooper()) {
                        @Override
                        public void dispatchMessage(Message msg) {
                            switch (msg.what) {
                                case MSG_DO_SEND:
                                    SendRecord sendRecord = sTransferQueue.poll();
                                    if (sendRecord != null) {
                                        final AsyncCallback<Void, Error> callback = sendRecord.callback;

                                        doSend(sendRecord.bleManager, sendRecord.serviceId, sendRecord.characterId, sendRecord.encryptedBytes, new AsyncCallback<Void, Error>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                sendEmptyMessage(MSG_DO_SEND);

                                                if (callback != null) {
                                                    callback.sendSuccessMessage(null);
                                                }
                                            }

                                            @Override
                                            public void onFailure(Error error) {
                                                sendEmptyMessage(MSG_DO_SEND);

                                                if (callback != null) {
                                                    callback.sendFailureMessage(error);
                                                }
                                            }
                                        });
                                    } else {
                                        synchronized (sIsSendingLock) {
                                            sIsSending = false;
                                        }
                                    }
                                    break;
                            }
                        }
                    };
                }
            }
        }
        return sWorkerHandler;
    }

    public static long getPackageTimeout() {
        return BleSetting.getBleConnInterval() * 2 + 1 * DateUtils.SECOND_IN_MILLIS;
    }

    public static void send(BleManager bleManager, UUID serviceId, UUID characterId, byte[] encryptedBytes, AsyncCallback<Void, Error> callback) {
        if (bleManager == null) {
            Logger.e(TAG_BLE, "SplitPackage.send BleManager is null");
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_BLE_MANAGER_IS_NULL, "BleManager is null"));
            }
            return;
        }

        if (!bleManager.isConnected()) {
            Logger.e(TAG_BLE, "SplitPackage.send BleManager not connected");
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        Logger.d(TAG_BLE, "SplitPackage.send");

        SendRecord sendRecord = new SendRecord();
        sendRecord.bleManager = bleManager;
        sendRecord.serviceId = serviceId;
        sendRecord.characterId = characterId;
        sendRecord.encryptedBytes = encryptedBytes;
        sendRecord.callback = callback;

        sTransferQueue.add(sendRecord);

        boolean isSending;
        synchronized (sIsSendingLock) {
            isSending = sIsSending;
            if (!sIsSending) {
                sIsSending = true;
            }
        }
        if (!isSending) {
            getHandler().sendEmptyMessage(MSG_DO_SEND);
        }
    }

    public static int calculateSplitPkgNum(int length) {
        return (int) Math.ceil(length / (double) MAX_PAYLOAD);
    }

    private static void doSend(final BleManager bleManager, final UUID serviceId, final UUID characterId, final byte[] encryptedBytes, final AsyncCallback<Void, Error> callback) {
        if (bleManager == null) {
            Logger.e(TAG_BLE, "SplitPackage.doSend BleManager is null");
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_BLE_MANAGER_IS_NULL, "BleManager is null"));
            }
            return;
        }

        if (!bleManager.isConnected()) {
            Logger.e(TAG_BLE, "SplitPackage.doSend BleManager not connected");
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        Logger.d(TAG_BLE, "SplitPackage.doSend");

        final int DEST_DATA_PACKAGE_NUM = calculateSplitPkgNum(encryptedBytes.length);
        final CmdPackage cmdPackage = CmdPackage.buildCmdRawData(DEST_DATA_PACKAGE_NUM);

        // 第一步 发送命令包
        sendCmdPackage(bleManager, serviceId, characterId, cmdPackage, new AsyncCallback<Void, Error>() {
            @Override
            public void onSuccess(Void result) {
                // 第二步 分包
                List<DataPackage> dataPackageList = splitPackage(DEST_DATA_PACKAGE_NUM, encryptedBytes);

                // 第三步 发送数据包
                sendDataPackage(bleManager, serviceId, characterId, dataPackageList, new AsyncCallback<Void, Error>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (callback != null) {
                            callback.sendSuccessMessage(null);
                        }
                    }

                    @Override
                    public void onFailure(Error error) {
                        if (callback != null) {
                            callback.sendFailureMessage(error);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Error error) {
                if (callback != null) {
                    callback.sendFailureMessage(error);
                }
            }
        });
    }

    private static void sendCmdPackage(final BleManager bleManager, UUID serviceId, final UUID characterId, CmdPackage cmdPackage, final AsyncCallback<Void, Error> callback) {
        Logger.d(TAG_BLE, "SplitPackage.sendCmdPackage");

        final int MSG_ON_CHARACTER_CHANGED_TIMEOUT = 1;

        final Handler timeoutHandler = new Handler(Looper.myLooper()) {
            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ON_CHARACTER_CHANGED_TIMEOUT:
                        removeMessages(MSG_ON_CHARACTER_CHANGED_TIMEOUT, msg.obj);

                        bleManager.removeManagerListener((BleManager.ManagerListener) msg.obj);

                        Logger.e(TAG_BLE, "SplitPackage.sendCmdPackage ack receive timeout");

                        if (callback != null) {
                            callback.sendFailureMessage(new Error(BLE_ON_CHARACTER_CHANGED_TIMEOUT, "onCharacterChanged timeout"));
                        }
                        break;
                }
            }
        };

        final BleManager.ManagerListener listener = new BleManager.ManagerListener() {
            @Override
            public void onDisconnected(BleManager.ManagerDisconnectedCause cause) {
                if (timeoutHandler.hasMessages(MSG_ON_CHARACTER_CHANGED_TIMEOUT, this)) {
                    timeoutHandler.removeMessages(MSG_ON_CHARACTER_CHANGED_TIMEOUT, this);

                    bleManager.removeManagerListener(this);

                    Logger.e(TAG_BLE, "SplitPackage.sendCmdPackage onDisconnected");

                    if (callback != null) {
                        callback.sendFailureMessage(new SendCmdPackageOnDisconnectedError());
                    }
                }
            }

            @Override
            public void onCharacterChanged(BluetoothGattCharacteristic character, byte[] value) {
                if (character.getUuid().compareTo(characterId) == 0) {

                    if (timeoutHandler.hasMessages(MSG_ON_CHARACTER_CHANGED_TIMEOUT, this)) {
                        timeoutHandler.removeMessages(MSG_ON_CHARACTER_CHANGED_TIMEOUT, this);

                        bleManager.removeManagerListener(this);

                        AckPackage ackPackage = AckPackage.parse(value);
                        if (ackPackage == null) {
                            Logger.e(TAG_BLE, "SplitPackage.sendCmdPackage ack is null, bytes:" + ByteUtil.byteToString(value));
                            if (callback != null) {
                                callback.sendFailureMessage(new Error(BLE_ACK_PACKAGE_IS_NULL, "ctrl package is null"));
                            }
                            return;
                        }

                        if (ackPackage.isAckReady()) {
                            if (callback != null) {
                                callback.sendSuccessMessage(null);
                            }
                        } else {
                            Logger.e(TAG_BLE, "SplitPackage.sendCmdPackage ack not ready, Ack:" + ackPackage.status);

                            if (callback != null) {
                                callback.sendFailureMessage(new Error(BLE_ACK_PACKAGE_IS_NOT_READY, "ctrl package is not ack ready"));
                            }
                        }

                    }
                }
            }
        };

        bleManager.addManagerListener(listener);

        //开始计时，先于writeCharacterNoAck，防止来不及触发onSuccess
        timeoutHandler.sendMessageDelayed(Message.obtain(timeoutHandler, MSG_ON_CHARACTER_CHANGED_TIMEOUT, listener), 4 * DateUtils.SECOND_IN_MILLIS);

        bleManager.writeCharacterWithoutRsp(serviceId, characterId, cmdPackage.toBytes(), new AsyncCallback<Void, Error>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onFailure(Error error) {
                timeoutHandler.removeMessages(MSG_ON_CHARACTER_CHANGED_TIMEOUT, listener);

                bleManager.removeManagerListener(listener);

                if (callback != null) {
                    callback.sendFailureMessage(error);
                }
            }
        });
    }

    private static void sendDataPackage(final BleManager bleManager, final UUID serviceId, final UUID characterId, final List<DataPackage> dataPackageList, final AsyncCallback<Void, Error> callback) {
        Logger.d(TAG_BLE, "SplitPackage.sendDataPackage");
        final int MSG_WRITE_DATA_PKG_LIST = 1;
        final int MSG_ACK_TIMEOUT = 2;

        //重传次数
        final IntegerWrapper retryCount = new IntegerWrapper();
        retryCount.integer = 0;

        final HandlerWrapper handlerWrapper = new HandlerWrapper();

        final Handler timeoutHandler = new Handler(Looper.myLooper()) {
            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ACK_TIMEOUT:
                        removeMessages(MSG_ACK_TIMEOUT, msg.obj);

                        bleManager.removeManagerListener((BleManager.ManagerListener) msg.obj);

                        Logger.d(TAG_BLE, "SplitPackage.sendDataPackage ack timeout");

                        if (callback != null) {
                            callback.sendFailureMessage(new Error(BLE_ON_CHARACTER_CHANGED_TIMEOUT, "onCharacterChanged timeout"));
                        }
                        break;
                }
            }
        };

        final BleManager.ManagerListener listener = new BleManager.ManagerListener() {
            @Override
            public void onDisconnected(BleManager.ManagerDisconnectedCause cause) {
                if (timeoutHandler.hasMessages(MSG_ACK_TIMEOUT, this)) {
                    timeoutHandler.removeMessages(MSG_ACK_TIMEOUT, this);

                    bleManager.removeManagerListener(this);

                    Logger.e(TAG_BLE, "SplitPackage.sendDataPackage onDisconnected");

                    if (callback != null) {
                        callback.sendFailureMessage(new SendDataPackageOnDisconnectedError());
                    }
                }
            }

            @Override
            public void onCharacterChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
                if (characteristic.getUuid().compareTo(characterId) == 0) {
                    if (timeoutHandler.hasMessages(MSG_ACK_TIMEOUT, this)) {
                        timeoutHandler.removeMessages(MSG_ACK_TIMEOUT, this);

                        AckPackage ackPackage = AckPackage.parse(value);
                        if (ackPackage == null) {
                            bleManager.removeManagerListener(this);

                            if (callback != null) {
                                callback.sendFailureMessage(new Error(BLE_ACK_PACKAGE_IS_NULL, "ack package is null"));
                            }
                            return;
                        }


                        if (ackPackage.isAckLoss()) {
                            //最多重传SEND_DATA_PKG_MAX_RETRY次
                            if (retryCount.integer > 5) {
                                bleManager.removeManagerListener(this);

                                Logger.d(TAG_BLE, "SplitPackage.sendDataPackage retry stop");

                                if (callback != null) {
                                    callback.sendFailureMessage(new Error(BLE_SEND_DATA_RETRY_TOO_MUCH, "send data retry too much"));
                                }
                            } else {
                                int lossTotal = ackPackage.lossPkgSN.size();

                                List<DataPackage> lossPkgList = new ArrayList<>();

                                for (int i = 0, len = lossTotal; i < len; i++) {
                                    byte[] buffer = new byte[4];
                                    buffer[0] = ackPackage.lossPkgSN.get(i)[0];
                                    buffer[1] = ackPackage.lossPkgSN.get(i)[1];
                                    buffer[2] = 0x00;
                                    buffer[3] = 0x00;
                                    int lossIndex = ByteUtil.bytesToInt(buffer, ByteOrder.LITTLE_ENDIAN);
                                    DataPackage lossPkg = dataPackageList.get(lossIndex - 1);
                                    if (lossPkg != null) {
                                        lossPkgList.add(lossPkg);
                                    }
                                }

                                Logger.d(TAG_BLE, "SplitPackage.sendDataPackage retry start for loss");

                                retryCount.integer++;
                                Message.obtain(handlerWrapper.handler, MSG_WRITE_DATA_PKG_LIST, lossPkgList).sendToTarget();
                            }
                        } else {
                            bleManager.removeManagerListener(this);

                            if (ackPackage.isAckSuccess()) {
                                if (callback != null) {
                                    callback.sendSuccessMessage(null);
                                }
                            } else {
                                Logger.d(TAG_BLE, "SplitPackage.sendDataPackage not AckSuccess , Ack:" + ackPackage.status);

                                if (callback != null) {
                                    callback.sendFailureMessage(new Error(BLE_ACK_PACKAGE_IS_NOT_SUCCESS, "ctrl package is not ack success"));
                                }
                            }
                        }
                    }
                }
            }
        };

        bleManager.addManagerListener(listener);

        final Handler writeHandler = new Handler(Looper.myLooper()) {
            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_WRITE_DATA_PKG_LIST:
                        List<DataPackage> dataPkgList = (List<DataPackage>) msg.obj;
                        //超时时间 = 数据包数 * 5秒
                        long timeout;

                        if (dataPkgList.size() <= 0) {
                            timeout = getPackageTimeout();
                        } else {
                            timeout = dataPkgList.size() * getPackageTimeout();
                        }

                        //开始等待ACK包(防止来不及触发onSuccess回调, 设备ACK回的太快)
                        timeoutHandler.sendMessageDelayed(Message.obtain(timeoutHandler, MSG_ACK_TIMEOUT, listener), timeout);

                        writeDataPackageList(bleManager, serviceId, characterId, dataPkgList, new AsyncCallback<Void, Error>() {
                            @Override
                            public void onSuccess(Void result) {

                            }

                            @Override
                            public void onFailure(Error error) {
                                timeoutHandler.removeMessages(MSG_ACK_TIMEOUT, listener);

                                bleManager.removeManagerListener(listener);

                                if (callback != null) {
                                    callback.sendFailureMessage(error);
                                }
                            }
                        });
                        break;
                }
            }
        };

        handlerWrapper.handler = writeHandler;

        Message.obtain(writeHandler, MSG_WRITE_DATA_PKG_LIST, dataPackageList).sendToTarget();
    }

    private static List<DataPackage> splitPackage(int destNum, byte[] bytes) {
        final List<DataPackage> dataPackageList = new ArrayList<>();

        for (int i = 0, len = destNum; i < len; i++) {
            DataPackage dataPackage = new DataPackage();
            // 序列号从1开始
            dataPackage.sn = getBytes(intToBytes(i + 1, ByteOrder.LITTLE_ENDIAN), 0, 1);
            if (i < len - 1) {
                dataPackage.payload = getBytes(bytes, MAX_PAYLOAD * i, MAX_PAYLOAD * (i + 1) - 1);
            } else {
                dataPackage.payload = getBytes(bytes, MAX_PAYLOAD * i, bytes.length - 1);
            }

            dataPackageList.add(dataPackage);
        }

        return dataPackageList;
    }

    private static void writeDataPackageList(final BleManager bleManager, final UUID serviceId, final UUID characterId, final List<DataPackage> dataPackageList, final AsyncCallback<Void, Error> callback) {
        if (dataPackageList == null || dataPackageList.size() <= 0) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_SEND_DATA_PARAM_ERROR, "dataPackageList is null"));
            }
            return;
        }

        final int BATCH_PACKAGE_NUM = BleSetting.getSplitBatchPackageNum();
        final long BATCH_DELAY = BleSetting.getSplitBatchDelay();

        final int MSG_WRITE_CHARACTER = 1;

        Handler handler = new Handler(Looper.myLooper()) {

            int successPackageCount = 0;

            int oneDataPackageRetry = 0;

            int currentIndex = 0;

            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_WRITE_CHARACTER:

                        DataPackage dataPackage = dataPackageList.get(currentIndex);

                        byte[] buffer = new byte[4];
                        buffer[0] = dataPackage.sn[0];
                        buffer[1] = dataPackage.sn[1];
                        buffer[2] = 0x00;
                        buffer[3] = 0x00;

                        bleManager.writeCharacterWithoutRsp(serviceId, characterId, dataPackage.toBytes(), new AsyncCallback<Void, Error>() {
                            @Override
                            public void onSuccess(Void result) {
                                successPackageCount++;

                                oneDataPackageRetry = 0;

                                currentIndex++;

                                if (currentIndex < dataPackageList.size()) {
                                    if (successPackageCount > BATCH_PACKAGE_NUM) {
                                        successPackageCount = 0;
                                        sendEmptyMessageDelayed(MSG_WRITE_CHARACTER, BATCH_DELAY);
                                    } else {
                                        sendEmptyMessage(MSG_WRITE_CHARACTER);
                                    }
                                } else {
                                    if (callback != null) {
                                        callback.sendSuccessMessage(null);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Error error) {
                                if (oneDataPackageRetry > 20) {
                                    Logger.e(TAG_BLE, "BleManager.writeCharacterWithoutRsp onFailure oneDataPackageRetry>20, batchPackageNum:" + BleSetting.getSplitBatchPackageNum() + " batchDelay:" + BleSetting.getSplitBatchDelay() + " currentIndex:" + currentIndex);
                                    if (callback != null) {
                                        callback.sendFailureMessage(new DataPackageRetryTooMuch(currentIndex));
                                    }
                                } else {
                                    Logger.d(TAG_BLE, "BleConnector.writeCharacterWithoutRsp fail to retry");

                                    oneDataPackageRetry++;
                                    sendEmptyMessageDelayed(MSG_WRITE_CHARACTER, 10);
                                }
                            }
                        });
                        break;
                }
            }
        };

        handler.sendEmptyMessage(MSG_WRITE_CHARACTER);
    }

    private static class SendRecord {
        BleManager bleManager;
        UUID serviceId;
        UUID characterId;
        byte[] encryptedBytes;
        AsyncCallback<Void, Error> callback;
    }

    private static class IntegerWrapper {
        int integer;
    }

    private static class HandlerWrapper {
        Handler handler;
    }

    public static class SendCmdPackageOnDisconnectedError extends Error {
        SendCmdPackageOnDisconnectedError() {
            super(-1, "split sendCmdPackage onDisconnected");
        }
    }

    public static class SendDataPackageOnDisconnectedError extends Error {
        SendDataPackageOnDisconnectedError() {
            super(-1, "split sendDataPackage onDisconnected");
        }
    }

    public static class DataPackageRetryTooMuch extends Error {
        public int mCurrentIndex;

        DataPackageRetryTooMuch(int currentIndex) {
            super(-1, "split data package retry too much, currentIndex:" + currentIndex);
            mCurrentIndex = currentIndex;
        }
    }
}
