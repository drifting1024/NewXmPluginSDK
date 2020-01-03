package com.ryeex.groot.lib.ble.stack.splitpackage;

import com.ryeex.groot.lib.common.util.ByteUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Miot分包协议的数据包<br/>
 * <br/>
 * Created by chenhao on 2017/6/16.
 */
public class DataPackage {
    public final static int MAX_PAYLOAD = 18;

    public byte[] sn; // 序列号: 2 bytes

    public byte[] payload; // 负载: 0-18 bytes

    public static DataPackage parse(byte[] bytes) {
        if (bytes == null || bytes.length <= 1) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byte[] sn = new byte[2];
        buffer.get(sn);

        if (ByteUtil.byteEquals(sn, CtrlPackage.FLAG_CTRL)) {
            return null;
        }

        DataPackage result = new DataPackage();
        result.sn = sn;
        byte[] payload = new byte[bytes.length - 2];
        buffer.get(payload);
        result.payload = payload;

        return result;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(2 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(sn);
        buffer.put(payload);
        return buffer.array();
    }
}
