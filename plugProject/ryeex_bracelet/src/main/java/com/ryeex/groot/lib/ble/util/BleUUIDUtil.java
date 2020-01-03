package com.ryeex.groot.lib.ble.util;

import java.util.UUID;

/**
 * Created by chenhao on 2017/10/16.
 */

public class BleUUIDUtil {
    public static final String UUID_FORMAT = "0000%04x-0000-1000-8000-00805f9b34fb";

    private static final String UUID_BASE = "0000%4s-0000-1000-8000-00805f9b34fb";

    public static UUID makeUUID(int value) {
        return UUID.fromString(String.format(UUID_FORMAT, value));
    }

    public static UUID makeUUID(final String uuid) {
        return UUID.fromString(String.format(UUID_BASE, uuid));
    }

    public static String getValue(UUID uuid) {
        String result = "";
        try {
            String first = uuid.toString().split("-")[0];
            result = first.substring(4);
        } catch (Exception e) {

        }
        return result;
    }
}
