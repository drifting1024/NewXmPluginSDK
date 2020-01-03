package com.ryeex.groot.lib.ble.stack.pb;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.protobuf.ByteString;
import com.ryeex.groot.lib.ble.BleManager;
import com.ryeex.groot.lib.ble.BleManager.ManagerListener;
import com.ryeex.groot.lib.ble.stack.crypto.Crypto;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBRbp;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceAlreadyExistError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceDecodeFailError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceEncodeFailError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceExeFailError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceInvalidParaError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceInvalidStateCardError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceInvalidStateOtaError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceInvalidStateRunningError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceInvalidStateUploadDataError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceLowPowerError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceNoBindError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceNoMemError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceNotFoundError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceSignVerifyFailError;
import com.ryeex.groot.lib.ble.stack.pb.error.DeviceTblFullError;
import com.ryeex.groot.lib.ble.stack.splitpackage.SplitPackage;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.thread.MessageHandlerThread;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.common.util.RandomUtil;
import com.ryeex.groot.lib.log.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_NOT_CONNECTED;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_ON_RECEIVE_BYTES_TIMEOUT;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_PB_RES_WRONG_FORMAT;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_PROTOBUF_PARSE_FAIL;
import static com.ryeex.groot.lib.ble.BleManager.KEY_SPEED;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_OPEN;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_RC4;
import static com.ryeex.groot.lib.ble.BleSetting.SERVICE_RYEEX;
import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;
import static com.ryeex.groot.lib.ble.stack.pb.PbSetting.PROTOCOL_VER;
import static com.ryeex.groot.lib.common.util.ByteUtil.getBytes;


/**
 * 蓝牙应用层接口(使用PB协议)
 * <p>
 * Created by chenhao on 2017/11/6.
 */
public class PbApi {
    private static final int MSG_DO_SEND_REQ_OR_RES = 1;

    public static int PB_MAX = 2048;

    private static Object sLock = new Object();
    private static MessageHandlerThread sWorkerThread;
    private static Handler sWorkerHandler;

    private static boolean sIsSending = false;
    private static Object sIsSendingLock = new Object();

    private static Queue<PbBaseRecord> sPbQueue = new ConcurrentLinkedQueue<>();

    private static Handler getHandler() {
        if (sWorkerThread == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sWorkerThread == null) {
                    sWorkerThread = new MessageHandlerThread("PbApi-WorkThread");
                    sWorkerThread.start();
                    sWorkerHandler = new Handler(sWorkerThread.getLooper()) {
                        @Override
                        public void dispatchMessage(Message msg) {
                            switch (msg.what) {
                                case MSG_DO_SEND_REQ_OR_RES:
                                    PbBaseRecord record = sPbQueue.poll();
                                    if (record != null) {
                                        if (record instanceof PbReqRecord) {
                                            PbReqRecord reqRecord = (PbReqRecord) record;
                                            final AsyncCallback<Object, Error> callback = reqRecord.callback;
                                            sendReqReal(reqRecord.bleManager, reqRecord.cmd, reqRecord.bytes, reqRecord.crypto, reqRecord.processTimeout, reqRecord.bleParser, new AsyncCallback<Object, Error>() {
                                                @Override
                                                public void onSuccess(Object result) {
                                                    sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);

                                                    if (callback != null) {
                                                        callback.sendSuccessMessage(result);
                                                    }
                                                }

                                                @Override
                                                public void onProgress(float progress) {
                                                    if (callback != null) {
                                                        callback.sendProgressMessage(progress);
                                                    }
                                                }

                                                @Override
                                                public void onUpdate(Bundle update) {
                                                    if (callback != null) {
                                                        callback.sendUpdateMessage(update);
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Error error) {
                                                    sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);

                                                    if (callback != null) {
                                                        callback.sendFailureMessage(error);
                                                    }
                                                }
                                            });
                                        } else if (record instanceof PbReqNoResRecord) {
                                            PbReqNoResRecord reqNoResRecord = (PbReqNoResRecord) record;

                                            final AsyncCallback<Void, Error> callback = reqNoResRecord.callback;
                                            sendReqNoResReal(reqNoResRecord.bleManager, reqNoResRecord.cmd, reqNoResRecord.bytes, reqNoResRecord.crypto, new AsyncCallback<Void, Error>() {
                                                @Override
                                                public void onSuccess(Void result) {
                                                    sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);

                                                    if (callback != null) {
                                                        callback.sendSuccessMessage(null);
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Error error) {
                                                    sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);

                                                    if (callback != null) {
                                                        callback.sendFailureMessage(error);
                                                    }
                                                }
                                            });
                                        } else if (record instanceof PbResRecord) {
                                            PbResRecord resRecord = (PbResRecord) record;

                                            final AsyncCallback<Void, Error> callback = resRecord.callback;
                                            sendResReal(resRecord.bleManager, resRecord.cmd, resRecord.sessionId, resRecord.code, resRecord.bytes, resRecord.crypto, new AsyncCallback<Void, Error>() {
                                                @Override
                                                public void onSuccess(Void result) {
                                                    sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);

                                                    if (callback != null) {
                                                        callback.sendSuccessMessage(null);
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Error error) {
                                                    sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);

                                                    if (callback != null) {
                                                        callback.sendFailureMessage(error);
                                                    }
                                                }
                                            });
                                        } else {
                                            sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);
                                        }
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

    public static <R> void sendReq(final BleManager bleManager, PBRbp.CMD cmd, byte[] bytes, final Crypto.CRYPTO crypto, Long processTimeout, final PbApiParser<R> bleParser, final AsyncCallback<R, Error> callback) {
        if (!bleManager.isConnected()) {
            Logger.i(TAG_BLE, "PbApi.sendReq fail not connected, cmd:" + cmd);
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_NOT_CONNECTED, "not connected"));
            }
            return;
        }

        PbReqRecord<R> reqRecord = new PbReqRecord();
        reqRecord.bleManager = bleManager;
        reqRecord.cmd = cmd;
        reqRecord.bytes = bytes;
        reqRecord.crypto = crypto;
        reqRecord.processTimeout = processTimeout;
        reqRecord.bleParser = bleParser;
        reqRecord.callback = callback;

        sPbQueue.add(reqRecord);

        boolean isSending;
        synchronized (sIsSendingLock) {
            isSending = sIsSending;
            if (!sIsSending) {
                sIsSending = true;
            }
        }
        if (!isSending) {
            getHandler().sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);
        }
    }

    public static void sendReqNoRes(final BleManager bleManager, PBRbp.CMD cmd, byte[] bytes, final Crypto.CRYPTO crypto, final AsyncCallback<Void, Error> callback) {
        if (!bleManager.isConnected()) {
            Logger.i(TAG_BLE, "PbApi.sendReqNoRes fail not connected, cmd:" + cmd);
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_NOT_CONNECTED, "not connected"));
            }
            return;
        }

        PbReqNoResRecord reqNoResRecord = new PbReqNoResRecord();
        reqNoResRecord.bleManager = bleManager;
        reqNoResRecord.cmd = cmd;
        reqNoResRecord.bytes = bytes;
        reqNoResRecord.crypto = crypto;
        reqNoResRecord.callback = callback;

        sPbQueue.add(reqNoResRecord);

        boolean isSending;
        synchronized (sIsSendingLock) {
            isSending = sIsSending;
            if (!sIsSending) {
                sIsSending = true;
            }
        }
        if (!isSending) {
            getHandler().sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);
        }
    }

    public static void sendRes(final BleManager bleManager, PBRbp.CMD cmd, int sessionId, int code, byte[] bytes, final Crypto.CRYPTO crypto, final AsyncCallback<Void, Error> callback) {
        if (!bleManager.isConnected()) {
            Logger.i(TAG_BLE, "PbApi.sendRes fail not connected, cmd:" + cmd + " sessionId:" + sessionId + " code:" + code);
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_NOT_CONNECTED, "not connected"));
            }
            return;
        }

        PbResRecord resRecord = new PbResRecord();
        resRecord.bleManager = bleManager;
        resRecord.cmd = cmd;
        resRecord.sessionId = sessionId;
        resRecord.code = code;
        resRecord.bytes = bytes;
        resRecord.crypto = crypto;
        resRecord.callback = callback;

        sPbQueue.add(resRecord);

        boolean isSending;
        synchronized (sIsSendingLock) {
            isSending = sIsSending;
            if (!sIsSending) {
                sIsSending = true;
            }
        }
        if (!isSending) {
            getHandler().sendEmptyMessage(MSG_DO_SEND_REQ_OR_RES);
        }
    }

    private static <R> void sendReqReal(final BleManager bleManager, final PBRbp.CMD cmd, byte[] bytes, final Crypto.CRYPTO crypto, final Long processTimeout, final PbApiParser<R> bleParser, final AsyncCallback<R, Error> callback) {

        final Map<Integer, ByteString> resMap = new ConcurrentHashMap<>();

        final int sessionId = generateRandomInt();

        final List<PBRbp.RbpMsg> rbpList = splitReqPackage(cmd, sessionId, bytes);

        final int MSG_ON_RECEIVE_BYTES_TIMEOUT = 1;

        final Handler timeoutHandler = new Handler(Looper.myLooper()) {
            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ON_RECEIVE_BYTES_TIMEOUT:
                        removeMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, msg.obj);
                        bleManager.removeManagerListener((ManagerListener) msg.obj);

                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveRes timeout cmd:" + cmd);

                        if (callback != null) {
                            callback.sendFailureMessage(new Error(BLE_ON_RECEIVE_BYTES_TIMEOUT, "onReceiveRes timeout"));
                        }
                        break;
                }
            }
        };

        final ManagerListener listener = new ManagerListener() {

            @Override
            public void onReceiveBytes(UUID serviceId, UUID characterId, final byte[] plainBytes) {
                if (timeoutHandler.hasMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, this)) {

                    if (serviceId.equals(SERVICE_RYEEX) && (characterId.equals(CHARACTER_RYEEX_OPEN) || characterId.equals(CHARACTER_RYEEX_RC4))) {

                        try {
                            final PBRbp.RbpMsg rbpMsg = PBRbp.RbpMsg.parseFrom(plainBytes);

                            if (rbpMsg.getSessionId() != sessionId) {
                                Logger.d(TAG_BLE, "PbApi onReceiveBytes wrong SessionId " + rbpMsg.getSessionId() + " " + sessionId);
                                return;
                            }
                            if (!rbpMsg.hasRes()) {
                                Logger.d(TAG_BLE, "PbApi onReceiveBytes RbpMsg hasn't Res");
                                return;
                            }

                            PBRbp.RbpMsg_Res rbpMsgRes = rbpMsg.getRes();

                            int total = rbpMsgRes.getTotal();
                            int sn = rbpMsgRes.getSn();
                            ByteString val = rbpMsgRes.getVal();

                            Logger.d(TAG_BLE, "PbApi onReceiveBytes sn:" + sn + " total:" + total);

                            resMap.put(sn, val);

                            if (resMap.size() == total) {
                                timeoutHandler.removeMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, this);
                                bleManager.removeManagerListener(this);

                                ByteString mergedPbBytes = mergePackage(resMap);

                                if (mergedPbBytes == null) {
                                    Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes mergedPbBytes is null cmd:" + cmd);

                                    if (callback != null) {
                                        callback.sendFailureMessage(new Error(BLE_PB_RES_WRONG_FORMAT, "RbpMsg_Res wrong format"));
                                    }
                                } else {
                                    int code = rbpMsgRes.getCode();

                                    if (code == 0) {
                                        Logger.i(TAG_BLE, "RbpMsg_Res.code " + code + " cmd:" + cmd);
                                        if (bleParser != null) {
                                            try {
                                                R result = bleParser.parse(mergedPbBytes);

                                                if (result instanceof com.google.protobuf.Message) {
                                                    Logger.i(TAG_BLE, "RbpMsg_Res.content\n\n" + result + "\n");
                                                } else if (result instanceof com.google.protobuf.ByteString) {
                                                    Logger.i(TAG_BLE, "RbpMsg_Res.content\n\n" + ByteUtil.byteToString(((com.google.protobuf.ByteString) result).toByteArray()) + "\n\n");
                                                } else {
                                                    Logger.i(TAG_BLE, "RbpMsg_Res.content\n\n" + result + "\n");
                                                }

                                                if (callback != null) {
                                                    callback.sendSuccessMessage(result);
                                                }
                                            } catch (Exception e) {
                                                Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes BleParse fail cmd:" + cmd + " ex:" + e);

                                                if (callback != null) {
                                                    callback.sendFailureMessage(new Error(BLE_PROTOBUF_PARSE_FAIL, "protobuf parse fail " + e));
                                                }
                                            }
                                        } else {
                                            if (callback != null) {
                                                callback.sendSuccessMessage(null);
                                            }
                                        }
                                    } else if (code == 1) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceLowPowerError());
                                        }
                                    } else if (code == 2) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceDecodeFailError());
                                        }
                                    } else if (code == 3) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceNotFoundError());
                                        }
                                    } else if (code == 4) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceInvalidParaError());
                                        }
                                    } else if (code == 5) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceNoMemError());
                                        }
                                    } else if (code == 6) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceExeFailError());
                                        }
                                    } else if (code == 7) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceTblFullError());
                                        }
                                    } else if (code == 8) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceEncodeFailError());
                                        }
                                    } else if (code == 9) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceSignVerifyFailError());
                                        }
                                    } else if (code == 10) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceNoBindError());
                                        }
                                    } else if (code == 11) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceAlreadyExistError());
                                        }
                                    } else if (code == 13) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceInvalidStateRunningError());
                                        }
                                    } else if (code == 14) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceInvalidStateOtaError());
                                        }
                                    } else if (code == 15) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceInvalidStateCardError());
                                        }
                                    } else if (code == 16) {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new DeviceInvalidStateUploadDataError());
                                        }
                                    } else {
                                        Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes RbpMsg_Res code(" + code + ") not success cmd:" + cmd);

                                        if (callback != null) {
                                            callback.sendFailureMessage(new Error(code, "RbpMsg_Res code not success"));
                                        }
                                    }
                                }
                            } else {
                                return;
                            }
                        } catch (Exception e) {
                            Logger.e(TAG_BLE, "PbApi.sendReqReal onReceiveBytes exception:" + e.toString());
                        }
                    }
                }
            }
        };
        bleManager.addManagerListener(listener);

        final int totalLength = (bytes == null) ? 0 : bytes.length;

        int SPLIT_DATA_PKG_NUM;
        if (ByteUtil.isEmpty(bytes)) {
            SPLIT_DATA_PKG_NUM = 0;
        } else {
            SPLIT_DATA_PKG_NUM = SplitPackage.calculateSplitPkgNum(bytes.length);
        }

        long finalProcessTimeout;
        if (processTimeout == null || processTimeout < 300) {
            finalProcessTimeout = 300;
        } else if (processTimeout > 20000) {
            finalProcessTimeout = 20000;
        } else {
            finalProcessTimeout = processTimeout;
        }

        // 超时时间=处理时间+命令包+数据包
        long timeout = finalProcessTimeout + SplitPackage.getPackageTimeout() + SPLIT_DATA_PKG_NUM * SplitPackage.getPackageTimeout();

        timeoutHandler.sendMessageDelayed(Message.obtain(timeoutHandler, MSG_ON_RECEIVE_BYTES_TIMEOUT, listener), timeout);

        final int MSG_DO_SEND_BYTES = 1;

        final Handler sendHandler = new Handler(Looper.myLooper()) {

            int currentIndex = 0;

            int finishedWholeLength = 0;

            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DO_SEND_BYTES:
                        final PBRbp.RbpMsg rbp = rbpList.get(currentIndex);

                        try {
                            PBRbp.RbpMsg_Req req = rbp.getReq();
                            req.getVal();

                            Logger.i(TAG_BLE, "PbApi.sendReqReal RbpMsg_Req cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + req.getSn() + " total:" + req.getTotal());
                        } catch (Exception e) {
                        }

                        bleManager.sendBytes(rbp.toByteArray(), crypto, new AsyncCallback<Float, Error>() {
                            @Override
                            public void onSuccess(Float speed) {
                                if (timeoutHandler.hasMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, listener)) {

                                    currentIndex++;

                                    try {
                                        if (totalLength > 0) {
                                            finishedWholeLength += rbp.getReq().getVal().size();
                                            float progress = finishedWholeLength / (float) totalLength;
                                            if (callback != null) {
                                                callback.sendProgressMessage(progress);
                                            }
                                        } else {
                                            if (callback != null) {
                                                callback.sendProgressMessage(1.0f);
                                            }
                                        }

                                        if (callback != null) {
                                            Bundle update = new Bundle();
                                            update.putFloat(KEY_SPEED, speed);
                                            callback.sendUpdateMessage(update);
                                        }
                                    } catch (Exception e) {
                                    }

                                    if (currentIndex < rbpList.size()) {
                                        sendEmptyMessage(MSG_DO_SEND_BYTES);
                                    }
                                } else {
                                    try {
                                        PBRbp.RbpMsg_Req req = rbp.getReq();
                                        req.getVal();

                                        Logger.i(TAG_BLE, "PbApi.sendReqReal RbpMsg_Req bleManager.sendBytes onSuccess, but timeout cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + req.getSn() + " total:" + req.getTotal());
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Error error) {
                                try {
                                    PBRbp.RbpMsg_Req req = rbp.getReq();
                                    req.getVal();

                                    Logger.e(TAG_BLE, "PbApi.sendReqReal RbpMsg_Req bleManager.sendBytes onFailure cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + req.getSn() + " total:" + req.getTotal() + " error:" + error);
                                } catch (Exception e) {
                                }

                                if (timeoutHandler.hasMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, listener)) {
                                    timeoutHandler.removeMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, listener);
                                    bleManager.removeManagerListener(listener);

                                    Logger.i(TAG_BLE, "PbApi.sendReqReal RbpMsg_Req bleManager.sendBytes onFailure, and not timeout");

                                    if (callback != null) {
                                        callback.sendFailureMessage(error);
                                    }
                                } else {
                                    Logger.i(TAG_BLE, "PbApi.sendReqReal RbpMsg_Req bleManager.sendBytes onFailure, but timeout");
                                }
                            }
                        });
                        break;
                }
            }
        };

        Message.obtain(sendHandler, MSG_DO_SEND_BYTES, rbpList).sendToTarget();
    }

    private static void sendReqNoResReal(final BleManager bleManager, PBRbp.CMD cmd, byte[] bytes, final Crypto.CRYPTO crypto, final AsyncCallback<Void, Error> callback) {
        final int sessionId = generateRandomInt();

        final List<PBRbp.RbpMsg> rbpList = splitReqPackage(cmd, sessionId, bytes);

        final int MSG_DO_SEND_BYTES = 1;

        final Handler sendHandler = new Handler(Looper.myLooper()) {

            int currentIndex = 0;

            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DO_SEND_BYTES:
                        final PBRbp.RbpMsg rbp = rbpList.get(currentIndex);

                        try {
                            PBRbp.RbpMsg_Req req = rbp.getReq();
                            Logger.i(TAG_BLE, "PbApi.sendReqNoResReal cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + req.getSn() + " total:" + req.getTotal());
                        } catch (Exception e) {
                        }

                        bleManager.sendBytes(rbp.toByteArray(), crypto, new AsyncCallback<Float, Error>() {
                            @Override
                            public void onSuccess(Float speed) {
                                currentIndex++;

                                if (currentIndex < rbpList.size()) {
                                    sendEmptyMessage(MSG_DO_SEND_BYTES);
                                } else {
                                    try {
                                        PBRbp.RbpMsg_Req req = rbp.getReq();
                                        Logger.i(TAG_BLE, "PbApi.sendReqNoResReal bleManager.sendBytes onSuccess cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + req.getSn() + " total:" + req.getTotal());
                                    } catch (Exception e) {
                                    }

                                    if (callback != null) {
                                        callback.sendSuccessMessage(null);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Error error) {

                                try {
                                    PBRbp.RbpMsg_Req req = rbp.getReq();
                                    Logger.e(TAG_BLE, "PbApi.sendReqNoResReal bleManager.sendBytes onFailure cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + req.getSn() + " total:" + req.getTotal());
                                } catch (Exception e) {
                                }

                                if (callback != null) {
                                    callback.sendFailureMessage(error);
                                }
                            }
                        });
                        break;
                }
            }
        };

        Message.obtain(sendHandler, MSG_DO_SEND_BYTES, rbpList).sendToTarget();
    }

    private static void sendResReal(final BleManager bleManager, PBRbp.CMD cmd, int sessionId, int code, byte[] bytes, final Crypto.CRYPTO crypto, final AsyncCallback<Void, Error> callback) {

        final List<PBRbp.RbpMsg> rbpList = splitResPackage(cmd, sessionId, code, bytes);

        final int MSG_DO_SEND_BYTES = 1;

        final Handler sendHandler = new Handler(Looper.myLooper()) {

            int currentIndex = 0;

            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DO_SEND_BYTES:
                        final PBRbp.RbpMsg rbp = rbpList.get(currentIndex);

                        try {
                            PBRbp.RbpMsg_Res res = rbp.getRes();
                            Logger.i(TAG_BLE, "PbApi.sendResReal cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + res.getSn() + " total:" + res.getTotal() + " code:" + res.getCode());
                        } catch (Exception e) {

                        }

                        bleManager.sendBytes(rbp.toByteArray(), crypto, new AsyncCallback<Float, Error>() {
                            @Override
                            public void onSuccess(Float speed) {
                                currentIndex++;

                                if (currentIndex < rbpList.size()) {
                                    sendEmptyMessage(MSG_DO_SEND_BYTES);
                                } else {
                                    if (callback != null) {
                                        callback.sendSuccessMessage(null);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Error error) {

                                try {
                                    PBRbp.RbpMsg_Res res = rbp.getRes();
                                    Logger.e(TAG_BLE, "PbApi.sendResReal BleManager.sendBytes fail cmd:" + rbp.getCmd() + " sessionId:" + rbp.getSessionId() + " sn:" + res.getSn() + " total:" + res.getTotal() + " code:" + res.getCode());
                                } catch (Exception e) {
                                }

                                if (callback != null) {
                                    callback.sendFailureMessage(error);
                                }
                            }
                        });
                        break;
                }
            }
        };

        Message.obtain(sendHandler, MSG_DO_SEND_BYTES, rbpList).sendToTarget();
    }

    private static List<PBRbp.RbpMsg> splitReqPackage(PBRbp.CMD cmd, int sessionId, byte[] bytes) {
        final List<PBRbp.RbpMsg> rbpMsgList = new ArrayList<>();

        if (ByteUtil.isEmpty(bytes)) {
            PBRbp.RbpMsg.Builder rbpMsgBuilder = PBRbp.RbpMsg.newBuilder();
            rbpMsgBuilder.setProtocolVer(PROTOCOL_VER);
            rbpMsgBuilder.setCmd(cmd);
            rbpMsgBuilder.setSessionId(sessionId);
            PBRbp.RbpMsg_Req.Builder rbpMsgReqBuilder = PBRbp.RbpMsg_Req.newBuilder();
            rbpMsgReqBuilder.setTotal(1);
            rbpMsgBuilder.setReq(rbpMsgReqBuilder.build());
            rbpMsgList.add(rbpMsgBuilder.build());
        } else {
            int totalPkg = (int) Math.ceil(bytes.length / (double) PB_MAX);

            for (int i = 0, len = totalPkg; i < len; i++) {
                PBRbp.RbpMsg.Builder rbpMsgBuilder = PBRbp.RbpMsg.newBuilder();
                rbpMsgBuilder.setProtocolVer(PROTOCOL_VER);
                rbpMsgBuilder.setCmd(cmd);
                rbpMsgBuilder.setSessionId(sessionId);

                PBRbp.RbpMsg_Req.Builder rbpMsgReqBuilder = PBRbp.RbpMsg_Req.newBuilder();
                rbpMsgReqBuilder.setTotal(totalPkg);
                rbpMsgReqBuilder.setSn(i);
                if (i < len - 1) {
                    rbpMsgReqBuilder.setVal(ByteString.copyFrom(getBytes(bytes, PB_MAX * i, PB_MAX * (i + 1) - 1)));
                } else {
                    rbpMsgReqBuilder.setVal(ByteString.copyFrom(getBytes(bytes, PB_MAX * i, bytes.length - 1)));
                }
                rbpMsgBuilder.setReq(rbpMsgReqBuilder.build());

                rbpMsgList.add(rbpMsgBuilder.build());
            }
        }

        return rbpMsgList;
    }

    private static List<PBRbp.RbpMsg> splitResPackage(PBRbp.CMD cmd, int sessionId, int code, byte[] bytes) {
        final List<PBRbp.RbpMsg> rbpMsgList = new ArrayList<>();

        if (ByteUtil.isEmpty(bytes)) {
            PBRbp.RbpMsg.Builder rbpMsgBuilder = PBRbp.RbpMsg.newBuilder();
            rbpMsgBuilder.setProtocolVer(PROTOCOL_VER);
            rbpMsgBuilder.setCmd(cmd);
            rbpMsgBuilder.setSessionId(sessionId);
            PBRbp.RbpMsg_Res.Builder rbpMsgResBuilder = PBRbp.RbpMsg_Res.newBuilder();
            rbpMsgResBuilder.setTotal(1);
            rbpMsgResBuilder.setSn(0);
            rbpMsgResBuilder.setCode(code);
            rbpMsgBuilder.setRes(rbpMsgResBuilder.build());
            rbpMsgList.add(rbpMsgBuilder.build());
        } else {
            int totalPkg = (int) Math.ceil(bytes.length / (double) PB_MAX);

            for (int i = 0, len = totalPkg; i < len; i++) {
                PBRbp.RbpMsg.Builder rbpMsgBuilder = PBRbp.RbpMsg.newBuilder();
                rbpMsgBuilder.setProtocolVer(PROTOCOL_VER);
                rbpMsgBuilder.setCmd(cmd);
                rbpMsgBuilder.setSessionId(sessionId);

                PBRbp.RbpMsg_Res.Builder rbpMsgResBuilder = PBRbp.RbpMsg_Res.newBuilder();
                rbpMsgResBuilder.setTotal(totalPkg);
                rbpMsgResBuilder.setSn(i);
                rbpMsgResBuilder.setCode(code);
                if (i < len - 1) {
                    rbpMsgResBuilder.setVal(ByteString.copyFrom(getBytes(bytes, PB_MAX * i, PB_MAX * (i + 1) - 1)));
                } else {
                    rbpMsgResBuilder.setVal(ByteString.copyFrom(getBytes(bytes, PB_MAX * i, bytes.length - 1)));
                }
                rbpMsgBuilder.setRes(rbpMsgResBuilder.build());

                rbpMsgList.add(rbpMsgBuilder.build());
            }
        }

        return rbpMsgList;
    }

    private static ByteString mergePackage(Map<Integer, ByteString> resMap) {
        if (resMap == null || resMap.size() <= 0) {
            return null;
        }

        ByteString[] pkgArray = new ByteString[resMap.size()];

        Iterator<Map.Entry<Integer, ByteString>> it = resMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ByteString> entry = it.next();
            pkgArray[entry.getKey()] = entry.getValue();
        }

        int totalBytes = 0;

        for (int i = 0, len = pkgArray.length; i < len; i++) {
            if (pkgArray[i] == null) {
                return null;
            }
            totalBytes += pkgArray[i].size();
        }

        byte[] result = new byte[totalBytes];
        int index = 0;
        for (int i = 0, len = pkgArray.length; i < len; i++) {
            ByteUtil.copy(result, pkgArray[i].toByteArray(), index, 0);
            index += pkgArray[i].size();
        }

        return ByteString.copyFrom(result);
    }

    private static int generateRandomInt() {
        return RandomUtil.randomInt(1000000);
    }

    private static class PbBaseRecord {
    }

    private static class PbReqRecord<R> extends PbBaseRecord {
        BleManager bleManager;
        PBRbp.CMD cmd;
        byte[] bytes;
        Crypto.CRYPTO crypto;
        Long processTimeout;
        PbApiParser<R> bleParser;
        AsyncCallback<R, Error> callback;
    }

    private static class PbReqNoResRecord extends PbBaseRecord {
        BleManager bleManager;
        PBRbp.CMD cmd;
        byte[] bytes;
        Crypto.CRYPTO crypto;
        AsyncCallback<Void, Error> callback;
    }

    private static class PbResRecord extends PbBaseRecord {
        BleManager bleManager;
        PBRbp.CMD cmd;
        int sessionId;
        int code;
        byte[] bytes;
        Crypto.CRYPTO crypto;
        AsyncCallback<Void, Error> callback;
    }
}
