package com.ryeex.groot.lib.ble.stack.json;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;

import com.ryeex.groot.lib.ble.BleManager;
import com.ryeex.groot.lib.ble.BleManager.ManagerListener;
import com.ryeex.groot.lib.ble.requestresult.DescriptorWriteResult;
import com.ryeex.groot.lib.ble.stack.crypto.Crypto;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

import java.util.UUID;

import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_ON_RECEIVE_BYTES_TIMEOUT;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_JSON;
import static com.ryeex.groot.lib.ble.BleSetting.SERVICE_RYEEX;

/**
 * Created by chenhao on 2018/1/14.
 */

public class JsonApi {
    public static Object sLock = new Object();
    public static boolean sIsSending = false;

    public static boolean isSending() {
        boolean sending = false;
        synchronized (sLock) {
            sending = sIsSending;
        }
        return sending;
    }

    public static void sendJson(final BleManager bleManager, final String json, final AsyncCallback<String, Error> callback) {
        if (bleManager == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(-1, "BleManager is null"));
            }
            return;
        }

        synchronized (sLock) {
            sIsSending = true;
        }

        bleManager.notify(SERVICE_RYEEX, CHARACTER_RYEEX_JSON, new AsyncCallback<DescriptorWriteResult, Error>() {
            @Override
            public void onSuccess(DescriptorWriteResult result) {
                final int MSG_ON_RECEIVE_BYTES_TIMEOUT = 1;

                final Handler timeoutHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void dispatchMessage(Message msg) {
                        switch (msg.what) {
                            case MSG_ON_RECEIVE_BYTES_TIMEOUT:
                                removeMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, msg.obj);
                                bleManager.removeManagerListener((ManagerListener) msg.obj);

                                if (callback != null) {
                                    callback.sendFailureMessage(new Error(BLE_ON_RECEIVE_BYTES_TIMEOUT, "onReceiveBytes timeout"));
                                }
                                synchronized (sLock) {
                                    sIsSending = false;
                                }
                                break;
                        }
                    }
                };

                final ManagerListener listener = new ManagerListener() {
                    @Override
                    public void onDisconnected(BleManager.ManagerDisconnectedCause cause) {
                        if (timeoutHandler.hasMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, this)) {
                            timeoutHandler.removeMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, this);
                            bleManager.removeManagerListener(this);

                            if (callback != null) {
                                callback.sendFailureMessage(new Error(-1, "disconnected"));
                            }

                            synchronized (sLock) {
                                sIsSending = false;
                            }
                        }
                    }

                    @Override
                    public void onReceiveBytes(UUID serviceId, UUID characterId, final byte[] plainBytes) {
                        if (timeoutHandler.hasMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, this)) {
                            if (serviceId.equals(SERVICE_RYEEX) && characterId.equals(CHARACTER_RYEEX_JSON)) {
                                timeoutHandler.removeMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, this);
                                bleManager.removeManagerListener(this);

                                String response = new String(plainBytes);
                                Log.d("json-api", response);
                                if (callback != null) {
                                    callback.sendSuccessMessage(response);
                                }

                                synchronized (sLock) {
                                    sIsSending = false;
                                }
                            }
                        }
                    }
                };
                bleManager.addManagerListener(listener);

                timeoutHandler.sendMessageDelayed(Message.obtain(timeoutHandler, MSG_ON_RECEIVE_BYTES_TIMEOUT, listener), 5 * DateUtils.SECOND_IN_MILLIS);

                bleManager.sendBytes(json.getBytes(), Crypto.CRYPTO.JSON, new AsyncCallback<Float, Error>() {
                    @Override
                    public void onSuccess(Float speed) {

                    }

                    @Override
                    public void onFailure(Error error) {
                        timeoutHandler.removeMessages(MSG_ON_RECEIVE_BYTES_TIMEOUT, listener);
                        bleManager.removeManagerListener(listener);

                        if (callback != null) {
                            callback.sendFailureMessage(new Error(-1, ""));
                        }

                        synchronized (sLock) {
                            sIsSending = false;
                        }
                    }
                });
            }

            @Override
            public void onFailure(Error error) {
                if (callback != null) {
                    callback.sendFailureMessage(error);
                }

                synchronized (sLock) {
                    sIsSending = false;
                }
            }
        });

    }
}
