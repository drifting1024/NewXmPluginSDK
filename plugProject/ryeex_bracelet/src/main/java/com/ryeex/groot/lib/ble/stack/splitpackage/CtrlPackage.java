package com.ryeex.groot.lib.ble.stack.splitpackage;

/**
 * Miot分包协议的控制包 <br/>
 * <br/>
 * Created by chenhao on 2017/6/19.
 */
public class CtrlPackage {

    public final static byte[] FLAG_CTRL = new byte[]{
            0x00, 0x00
    }; // 控制包标志

    public final static byte TYPE_CMD = 0x00; // 控制包CMD类型

    public final static byte TYPE_ACK = 0x01; // 控制包ACK类型
}
