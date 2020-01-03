package com.ryeex.groot.lib.ble.stack.crc;

import com.ryeex.groot.lib.ble.BleThreadPool;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.crypto.CRCCoder;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.log.Logger;

import java.nio.ByteOrder;

import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;
import static com.ryeex.groot.lib.common.util.ByteUtil.getBytes;
import static com.ryeex.groot.lib.common.util.ByteUtil.intToBytes;

/**
 * Created by chenhao on 2017/12/23.
 */

public class CRC {
    public static void calculate(final byte[] bytes, final AsyncCallback<byte[], Error> callback) {
        BleThreadPool.get().submit(new Runnable() {
            @Override
            public void run() {
                byte[] crc16Bytes = crc16(bytes);

                byte[] finalBytes = ByteUtil.concat(bytes, crc16Bytes);

                Logger.d(TAG_BLE, "crc " + ByteUtil.byteToString(crc16Bytes));

                if (callback != null) {
                    callback.sendSuccessMessage(finalBytes);
                }
            }
        });
    }

    public static void verify(final byte[] bytes, final AsyncCallback<byte[], Error> callback) {
        BleThreadPool.get().submit(new Runnable() {
            @Override
            public void run() {

                try {
                    int length = bytes.length;

                    if (length <= 2) {
                        throw new RuntimeException();
                    }

                    byte[] dataBytes = ByteUtil.getBytes(bytes, 0, length - 3);
                    byte[] calculateCrc16Bytes = crc16(dataBytes);
                    byte[] targetCrc16Bytes = ByteUtil.getBytes(bytes, length - 2, length - 1);

                    if (ByteUtil.byteEquals(calculateCrc16Bytes, targetCrc16Bytes)) {
                        if (callback != null) {
                            callback.sendSuccessMessage(dataBytes);
                        }
                    } else {
                        throw new RuntimeException();
                    }

                } catch (Exception e) {
                    if (callback != null) {
                        callback.sendFailureMessage(new Error(-1, "crc verify exception"));
                    }
                }

            }
        });
    }


    private static byte[] crc16(byte[] bytes) {
        int crc16Int = (int) CRCCoder.calculateCRC(CRCCoder.Parameters.CCITT, bytes);
        byte[] crc16Bytes = getBytes(intToBytes(crc16Int, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return crc16Bytes;
    }
}
