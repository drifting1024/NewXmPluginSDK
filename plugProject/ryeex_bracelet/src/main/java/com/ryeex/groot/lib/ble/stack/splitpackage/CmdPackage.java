package com.ryeex.groot.lib.ble.stack.splitpackage;

import com.ryeex.groot.lib.common.util.ByteUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by chenhao on 2017/7/5.
 */

public class CmdPackage {
    public final static byte DATA_TYPE_RAW_DATA = 0x00; // 透传数据
    public final static byte DATA_TYPE_DEVICE_CERT = 0x01; // 设备证书
    public final static byte DATA_TYPE_SERVER_CERT = 0x02; // 生产服务器证书
    public final static byte DATA_TYPE_PUBLIC_KEY = 0x03;// 公钥
    public final static byte DATA_TYPE_DEVICE_SIG = 0x04;// 设备签名
    public final static byte DATA_TYPE_DEVICE_LOGIN_INFO = 0x05;// 设备登录信息
    public final static byte DATA_TYPE_SHARE_LOGIN_INFO = 0x06; // 分享登录信息

    /**
     * 标志: 2 bytes
     */
    public byte[] flag;

    /**
     * 类型: 1 byte
     */
    public byte type;

    /**
     * 具体类型: 1 byte
     */
    public byte dataType;

    /**
     * 数据包个数: 2 bytes
     */
    public byte[] dataPkgNum;

    private CmdPackage(byte dataType) {
        this.flag = CtrlPackage.FLAG_CTRL;
        this.type = CtrlPackage.TYPE_CMD;
        this.dataType = dataType;
    }

    /**
     * 创建一个CmdRawData包
     *
     * @param dataPkgLength
     * @return
     */
    public static CmdPackage buildCmdRawData(int dataPkgLength) {
        CmdPackage result = new CmdPackage(DATA_TYPE_RAW_DATA);
        result.dataPkgNum = ByteUtil.getBytes(ByteUtil.intToBytes(dataPkgLength, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return result;
    }

    /**
     * 创建一个CmdDeviceCert包
     *
     * @param dataPkgLength
     * @return
     */
    public static CmdPackage buildCmdDeviceCert(int dataPkgLength) {
        CmdPackage result = new CmdPackage(DATA_TYPE_DEVICE_CERT);
        result.dataPkgNum = ByteUtil.getBytes(ByteUtil.intToBytes(dataPkgLength, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return result;
    }

    /**
     * 创建一个CmdServerCert包
     *
     * @param dataPkgLength
     * @return
     */
    public static CmdPackage buildCmdServerCert(int dataPkgLength) {
        CmdPackage result = new CmdPackage(DATA_TYPE_SERVER_CERT);
        result.dataPkgNum = ByteUtil.getBytes(ByteUtil.intToBytes(dataPkgLength, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return result;
    }

    /**
     * 创建一个CmdPublicKey包
     *
     * @param dataPkgLength
     * @return
     */
    public static CmdPackage buildCmdPublicKey(int dataPkgLength) {
        CmdPackage result = new CmdPackage(DATA_TYPE_PUBLIC_KEY);
        result.dataPkgNum = ByteUtil.getBytes(ByteUtil.intToBytes(dataPkgLength, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return result;
    }

    /**
     * 创建一个CmdDeviceSig包
     *
     * @param dataPkgLength
     * @return
     */
    public static CmdPackage buildCmdDeviceSig(int dataPkgLength) {
        CmdPackage result = new CmdPackage(DATA_TYPE_DEVICE_SIG);
        result.dataPkgNum = ByteUtil.getBytes(ByteUtil.intToBytes(dataPkgLength, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return result;
    }

    /**
     * 创建一个CmdDeviceLoginInfo包
     *
     * @param dataPkgLength
     * @return
     */
    public static CmdPackage buildCmdDeviceLoginInfo(int dataPkgLength) {
        CmdPackage result = new CmdPackage(DATA_TYPE_DEVICE_LOGIN_INFO);
        result.dataPkgNum = ByteUtil.getBytes(ByteUtil.intToBytes(dataPkgLength, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return result;
    }

    /**
     * 创建一个CmdShareLoginInfo包
     *
     * @param dataPkgLength
     * @return
     */
    public static CmdPackage buildCmdShareLoginInfo(int dataPkgLength) {
        CmdPackage result = new CmdPackage(DATA_TYPE_SHARE_LOGIN_INFO);
        result.dataPkgNum = ByteUtil.getBytes(ByteUtil.intToBytes(dataPkgLength, ByteOrder.LITTLE_ENDIAN), 0, 1);
        return result;
    }

    public static CmdPackage parse(byte[] bytes) {
        if (bytes == null || (bytes.length != 6 && bytes.length != 8)) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byte[] flag = new byte[2];
        buffer.get(flag);
        if (!ByteUtil.byteEquals(CtrlPackage.FLAG_CTRL, flag)) {
            return null;
        }

        byte type = buffer.get();
        if (type != CtrlPackage.TYPE_CMD) {
            return null;
        }

        byte dataType = buffer.get();
        if (dataType != DATA_TYPE_RAW_DATA
                && dataType != DATA_TYPE_DEVICE_CERT
                && dataType != DATA_TYPE_SERVER_CERT
                && dataType != DATA_TYPE_PUBLIC_KEY
                && dataType != DATA_TYPE_DEVICE_SIG
                && dataType != DATA_TYPE_DEVICE_LOGIN_INFO
                && dataType != DATA_TYPE_SHARE_LOGIN_INFO) {
            return null;
        }

        CmdPackage result = new CmdPackage(dataType);

        byte[] dataPkgNum = new byte[2];
        buffer.get(dataPkgNum);
        result.dataPkgNum = dataPkgNum;

        return result;
    }

    public boolean isCmdRawData() {
        return dataType == DATA_TYPE_RAW_DATA;
    }

    public boolean isCmdDeviceCert() {
        return dataType == DATA_TYPE_DEVICE_CERT;
    }

    public boolean isCmdServerCert() {
        return dataType == DATA_TYPE_SERVER_CERT;
    }

    public boolean isCmdPublicKey() {
        return dataType == DATA_TYPE_PUBLIC_KEY;
    }

    public boolean isCmdDeviceSig() {
        return dataType == DATA_TYPE_DEVICE_SIG;
    }

    public boolean isCmdDeviceLoginInfo() {
        return dataType == DATA_TYPE_DEVICE_LOGIN_INFO;
    }

    public boolean isCmdShareLoginInfo() {
        return dataType == DATA_TYPE_SHARE_LOGIN_INFO;
    }

    public byte[] toBytes() {
        int byteNum = 6;
        ByteBuffer buffer = ByteBuffer.allocate(byteNum).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(flag);
        buffer.put(type);
        buffer.put(dataType);
        buffer.put(dataPkgNum);
        return buffer.array();
    }
}
