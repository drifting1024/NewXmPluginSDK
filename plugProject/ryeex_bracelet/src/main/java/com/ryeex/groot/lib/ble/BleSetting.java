package com.ryeex.groot.lib.ble;

import android.text.format.DateUtils;

import com.ryeex.groot.lib.ble.util.BleUUIDUtil;
import com.ryeex.groot.lib.log.Logger;

import java.util.UUID;

/**
 * Created by chenhao on 2017/10/16.
 */

public class BleSetting {
    public static final String TAG_BLE = "groot-ble";

    private static long BLE_CONN_INTERVAL = 2 * DateUtils.SECOND_IN_MILLIS;

    private static int sSplitBatchPackageNum = 6;
    private static long sSplitBatchDelay = 15;

    private static int sOtaConnInterval = 9;//单位:1.25ms

    public static final UUID SERVICE_MI = BleUUIDUtil.makeUUID(0xFE95);

    public static boolean ENABLE_RC4_RECEIVE = true;

    public static final UUID CHARACTER_MI_EVENT = BleUUIDUtil.makeUUID(0x0010);
    public static final UUID CHARACTER_MI_TOKEN = BleUUIDUtil.makeUUID(0x0001);

    public static final UUID SERVICE_RYEEX = BleUUIDUtil.makeUUID(0xB167);
    public static final UUID CHARACTER_RYEEX_RC4 = BleUUIDUtil.makeUUID(0xAA00);
    public static final UUID CHARACTER_RYEEX_OPEN = BleUUIDUtil.makeUUID(0xAA01);
    public static final UUID CHARACTER_RYEEX_JSON = BleUUIDUtil.makeUUID(0xBA01);

    public static final UUID SERVICE_STD_HEART_RATE = BleUUIDUtil.makeUUID(0x180D);
    public static final UUID CHARACTER_STD_HEART_RATE_MEASUREMENT = BleUUIDUtil.makeUUID(0x2A37);

    public static final int MI_REGISTER_SESSION_START = 0xDE85CA90;
    public static final int MI_REGISTER_SESSION_END = 0xFA54AB92;
    public static final int MI_LOGIN_SESSION_START = 0xCD43BC00;
    public static final int MI_LOGIN_ENCRYPT_DATA = 0x93BFAC09;
    public static final int MI_LOGIN_ACK = 0x369A58C9;

    public static final int RYEEX_REGISTER_SESSION_START = 0xDE85CA91;
    public static final int RYEEX_REGISTER_SESSION_END = 0xFA54AB93;
    public static final int RYEEX_LOGIN_SESSION_START = 0xCD43BC01;
    public static final int RYEEX_LOGIN_ENCRYPT_DATA = 0x93BFAC0A;
    public static final int RYEEX_LOGIN_ACK = 0x369A58CA;

    public static final int TICK_LENGTH = 4;

    public static final long fixBleConnInterval(long rawBleConnInterval) {
        long bleConnInterval;
        if (rawBleConnInterval < 8) {
            bleConnInterval = 8;
        } else if (2 * DateUtils.SECOND_IN_MILLIS < rawBleConnInterval) {
            bleConnInterval = 2 * DateUtils.SECOND_IN_MILLIS;
        } else {
            bleConnInterval = rawBleConnInterval;
        }
        return bleConnInterval;
    }

    public static synchronized long getBleConnInterval() {
        return BLE_CONN_INTERVAL;
    }

    public static synchronized void setBleConnInterval(long bleConnInterval) {
        Logger.i(TAG_BLE, "BleSetting.setBleConnInterval bleConnInterval:" + bleConnInterval);
        BLE_CONN_INTERVAL = bleConnInterval;
    }

    public static synchronized int getSplitBatchPackageNum() {
        return sSplitBatchPackageNum;
    }

    public static synchronized long getSplitBatchDelay() {
        return sSplitBatchDelay;
    }

    public static synchronized void setSplitBatchParams(int batchPackageNum, long batchDelay) {
        Logger.i(TAG_BLE, "BleSetting.setSplitBatchParams batchPackageNum:" + batchPackageNum + " batchDelay:" + batchDelay);

        sSplitBatchPackageNum = batchPackageNum;
        sSplitBatchDelay = batchDelay;
    }

    public static synchronized int getOtaConnInterval() {
        return sOtaConnInterval;
    }

    public static synchronized void setOtaConnInterval(int otaConnInterval) {
        Logger.i(TAG_BLE, "BleSetting.setOtaConnInterval otaConnInterval:" + otaConnInterval);
        sOtaConnInterval = otaConnInterval;
    }
}
