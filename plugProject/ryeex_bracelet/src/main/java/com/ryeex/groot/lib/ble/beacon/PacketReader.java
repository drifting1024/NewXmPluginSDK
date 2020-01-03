package com.ryeex.groot.lib.ble.beacon;

import com.ryeex.groot.lib.common.util.StringUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by chenhao on 2017/12/20.
 */

public class PacketReader {
    private byte[] bytes;
    private ByteBuffer mByteBuffer;

    public PacketReader(BleAdvertise item) {
        this.bytes = item.bytes;
        mByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void position(int position) {
        mByteBuffer.position(position);
    }

    public int getShort() {
        return mByteBuffer.getShort() & 0xffff;
    }

    public int getByte() {
        return mByteBuffer.get() & 0xff;
    }

    public boolean getBit(int n, int index) {
        return (n & (1 << index)) != 0;
    }

    public int getInt(int n, int start, int end) {
        return (n >> start) & ((1 << (end - start + 1)) - 1);
    }

    public String getMac() {
        String[] texts = new String[6];
        for (int i = texts.length - 1; i >= 0; i--) {
            texts[i] = String.format("%02x", getByte()).toUpperCase();
        }
        return StringUtil.join(texts, ":");
    }

    public int getLastShort() {
        position(bytes.length - 2);
        return getShort();
    }

    public String getShortString() {
        String[] texts = new String[2];
        for (int i = texts.length - 1; i >= 0; i--) {
            texts[i] = String.format("%02x", getByte()).toUpperCase();
        }

        return StringUtil.join(texts, "");
    }
}
