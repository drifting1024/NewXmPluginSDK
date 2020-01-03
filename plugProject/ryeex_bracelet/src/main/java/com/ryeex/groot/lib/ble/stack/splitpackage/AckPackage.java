package com.ryeex.groot.lib.ble.stack.splitpackage;

import android.support.annotation.Nullable;

import com.ryeex.groot.lib.common.util.ByteUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenhao on 2017/7/5.
 */

public class AckPackage {

    public final static byte ACK_STATUS_SUCCESS = 0x00; // 传输成功
    public final static byte ACK_STATUS_READY = 0x01; // 设备就绪
    public final static byte ACK_STATUS_BUSY = 0x02; // 设备繁忙
    public final static byte ACK_STATUS_TIMEOUT = 0x03; // 设备超时
    public final static byte ACK_STATUS_CANCEL = 0x04; // 取消传输
    public final static byte ACK_STATUS_LOSS = 0x05; // 传输丢包


    public final static int ACK_LOSS_MAX_NUM = 8;// AckLoss包所能包含的最大丢包数

    public byte[] flag; // 标志: 2 bytes

    public byte type; // 类型: 1 byte

    public byte status; // 确认状态: 1 byte

    @Nullable
    public List<byte[]> lossPkgSN; // 丢包序列号: List<2 bytes>

    private AckPackage(byte status) {
        this.flag = CtrlPackage.FLAG_CTRL;
        this.type = CtrlPackage.TYPE_ACK;
        this.status = status;
    }

    public static AckPackage parse(byte[] bytes) {
        if (bytes == null || bytes.length < 4 || bytes.length > 20 || (bytes.length % 2 != 0)) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byte[] flag = new byte[2];
        buffer.get(flag);
        if (!ByteUtil.byteEquals(CtrlPackage.FLAG_CTRL, flag)) {
            return null;
        }

        byte type = buffer.get();
        if (type != CtrlPackage.TYPE_ACK) {
            return null;
        }

        byte status = buffer.get();
        if (status != ACK_STATUS_SUCCESS
                && status != ACK_STATUS_READY
                && status != ACK_STATUS_BUSY
                && status != ACK_STATUS_TIMEOUT
                && status != ACK_STATUS_CANCEL
                && status != ACK_STATUS_LOSS) {
            return null;
        }

        AckPackage result = new AckPackage(status);


        List<byte[]> lossPkgSNList = new ArrayList<>();

        while (buffer.hasRemaining()) {
            byte[] lossSN = new byte[2];
            buffer.get(lossSN);
            lossPkgSNList.add(lossSN);
        }

        if (lossPkgSNList.size() > 0) {
            result.lossPkgSN = lossPkgSNList;
        }

        //不合法的AckLoss包
        if (status == ACK_STATUS_LOSS && (lossPkgSNList == null || lossPkgSNList.size() == 0)) {
            return null;
        }

        return result;
    }

    /**
     * 创建一个AckSuccess包
     *
     * @return
     */
    public static AckPackage buildAckSuccess() {
        AckPackage result = new AckPackage(ACK_STATUS_SUCCESS);
        return result;
    }

    /**
     * 创建一个AckReady包
     *
     * @return
     */
    public static AckPackage buildAckReady() {
        AckPackage result = new AckPackage(ACK_STATUS_READY);
        return result;
    }

    /**
     * 创建一个AckBusy包
     *
     * @return
     */
    public static AckPackage buildAckBusy() {
        AckPackage result = new AckPackage(ACK_STATUS_BUSY);
        return result;
    }

    /**
     * 创建一个AckTimeout包
     *
     * @return
     */
    public static AckPackage buildAckTimeout() {
        AckPackage result = new AckPackage(ACK_STATUS_TIMEOUT);
        return result;
    }

    /**
     * 创建一个AckCancel包
     *
     * @return
     */
    public static AckPackage buildAckCancel() {
        AckPackage result = new AckPackage(ACK_STATUS_CANCEL);
        return result;
    }

    /**
     * 创建一个AckLoss包
     *
     * @return
     */
    public static AckPackage buildAckLoss(List<Integer> lossPkgList) {
        AckPackage result = new AckPackage(ACK_STATUS_LOSS);
        List<byte[]> lossPkgSNList = new ArrayList<>();
        for (int i = 0, len = lossPkgList.size(); i < len; i++) {
            lossPkgSNList.add(ByteUtil.getBytes(ByteUtil.intToBytes(lossPkgList.get(i), ByteOrder.LITTLE_ENDIAN), 0, 1));
        }
        result.lossPkgSN = lossPkgSNList;
        return result;
    }

    public boolean isAckSuccess() {
        return status == ACK_STATUS_SUCCESS;
    }

    public boolean isAckReady() {
        return status == ACK_STATUS_READY;
    }

    public boolean isAckBusy() {
        return status == ACK_STATUS_BUSY;
    }

    public boolean isAckTimeout() {
        return status == ACK_STATUS_TIMEOUT;
    }

    public boolean isAckCancel() {
        return status == ACK_STATUS_CANCEL;
    }

    public boolean isAckLoss() {
        return status == ACK_STATUS_LOSS;
    }

    public byte[] toBytes() {
        int byteNum;

        if (lossPkgSN == null) {
            byteNum = 4;
        } else {
            byteNum = 4 + lossPkgSN.size() * 2;
        }

        ByteBuffer buffer = ByteBuffer.allocate(byteNum).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(flag);
        buffer.put(type);
        buffer.put(status);
        if (lossPkgSN != null) {
            for (int i = 0, len = lossPkgSN.size(); i < len; i++) {
                buffer.put(lossPkgSN.get(i));
            }
        }
        return buffer.array();
    }
}
