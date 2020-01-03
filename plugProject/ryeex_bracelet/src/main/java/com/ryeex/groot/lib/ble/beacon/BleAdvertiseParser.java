package com.ryeex.groot.lib.ble.beacon;

import com.ryeex.groot.lib.common.util.ByteUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenhao on 2017/12/20.
 */

public class BleAdvertiseParser {
    public static List<BleAdvertise> parse(byte[] scanRecord) {
        if (ByteUtil.isEmpty(scanRecord)) {
            return null;
        }

//        Logger.d(TAG_BLE, ByteUtil.byteToString(scanRecord));

        ArrayList<BleAdvertise> advertiseList = new ArrayList<>();

        try {
            for (int i = 0; i < scanRecord.length; ) {
                BleAdvertise advertise = parse(scanRecord, i);
                if (advertise != null) {
//                    Logger.d(TAG_BLE, advertise.len + " " + Integer.toHexString(advertise.type) + " " + ByteUtil.byteToString(advertise.bytes));
                    advertiseList.add(advertise);
                    i += advertise.len + 1;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
        }

        return advertiseList;
    }

    private static BleAdvertise parse(byte[] bytes, int startIndex) {
        BleAdvertise advertise = null;

        if (bytes.length - startIndex >= 2) {
            byte length = bytes[startIndex];
            if (length > 0) {
                byte type = bytes[startIndex + 1];
                int firstIndex = startIndex + 2;

                if (firstIndex < bytes.length) {
                    advertise = new BleAdvertise();

                    int endIndex = firstIndex + length - 2;

                    if (endIndex >= bytes.length) {
                        endIndex = bytes.length - 1;
                    }

                    advertise.type = type & 0xff;
                    advertise.len = length;

                    advertise.bytes = ByteUtil.getBytes(bytes, firstIndex, endIndex);
                }
            }
        }

        return advertise;
    }
}
