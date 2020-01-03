package com.ryeex.groot.lib.ble.beacon;

/**
 * Created by chenhao on 2017/12/20.
 */

public class BleAdvertise {
    /**
     * 广播中声明的长度
     */
    public int len;

    /**
     * 广播中声明的type
     */
    public int type;

    /**
     * 广播中的数据部分
     */
    public byte[] bytes;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        String format = "";

        StringBuilder sb = new StringBuilder();

//        sb.append(String.format("len: %02d", len));
        sb.append(String.format("Type: 0x%02x, ", type));

        sb.append(String.format("Len: %d, ", len));

        switch (type) {
            case 8:
            case 9:
                format = "%c";
                break;
            default:
                format = "%02x ";
                break;
        }

        try {
            for (byte b : bytes) {
                sb.append(String.format(format, b));
            }
        } catch (Throwable e) {
        }

        return sb.toString();
    }
}
