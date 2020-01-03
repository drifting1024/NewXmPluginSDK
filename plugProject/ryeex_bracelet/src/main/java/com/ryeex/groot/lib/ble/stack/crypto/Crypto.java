package com.ryeex.groot.lib.ble.stack.crypto;

import com.ryeex.groot.lib.ble.BleThreadPool;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.log.Logger;

import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CRYPTO_NOT_SUPPORT;
import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;

/**
 * Created by chenhao on 2017/11/15.
 */

public class Crypto {
    public static void encrypt(final CRYPTO crypto, final byte[] bytes, final byte[] token, final AsyncCallback<byte[], Error> callback) {
        BleThreadPool.get().submit(new Runnable() {
            @Override
            public void run() {
                byte[] finalBytes;

                if (crypto == CRYPTO.RC4) {
                    byte[] encryptedBytes = BleCipher.encrypt(token, bytes);

                    finalBytes = encryptedBytes;

                    Logger.d(TAG_BLE, "encrypt rc4 token:" + ByteUtil.byteToString(token) + " plainBytes:" + ByteUtil.byteToString(bytes) + " encryptedBytes:" + ByteUtil.byteToString(encryptedBytes));
                } else if (crypto == CRYPTO.OPEN) {
                    finalBytes = bytes;

                    Logger.d(TAG_BLE, "encrypt open plainBytes:" + ByteUtil.byteToString(bytes));
                } else if (crypto == CRYPTO.JSON) {
                    finalBytes = bytes;

                    Logger.d(TAG_BLE, "encrypt json plainBytes:" + ByteUtil.byteToString(bytes));
                } else {
                    if (callback != null) {
                        callback.sendFailureMessage(new Error(BLE_CRYPTO_NOT_SUPPORT, "crypto not support"));
                    }
                    return;
                }

                if (callback != null) {
                    callback.sendSuccessMessage(finalBytes);
                }
            }
        });
    }

    public static byte[] decrypt(byte[] token, byte[] encryptedBytes) throws RuntimeException {
        byte[] plainBytes = BleCipher.encrypt(token, encryptedBytes);

        Logger.d(TAG_BLE, "decrypt token:" + ByteUtil.byteToString(token) + " encryptedBytes:" + ByteUtil.byteToString(encryptedBytes) + " plainBytes:" + ByteUtil.byteToString(plainBytes));
        return plainBytes;
    }

    public enum CRYPTO {
        OPEN, RC4, JSON
    }
}
