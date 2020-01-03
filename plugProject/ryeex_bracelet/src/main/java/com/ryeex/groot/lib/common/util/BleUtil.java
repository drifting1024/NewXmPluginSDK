package com.ryeex.groot.lib.common.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.lang.reflect.Method;

/**
 * Created by chenhao on 17/1/5.
 */

public class BleUtil {
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return characteristic != null && (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
    }

    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return characteristic != null && (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
    }

    public static boolean isCharacteristicNoRspWritable(BluetoothGattCharacteristic characteristic) {
        return characteristic != null && (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
    }

    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return characteristic != null && (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    public static boolean isCharacteristicIndicatable(BluetoothGattCharacteristic characteristic) {
        return characteristic != null && (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
    }

    public static boolean clearGattCache(BluetoothGatt gatt) {
        boolean result = false;
        try {
            if (gatt != null) {
                Method refresh = BluetoothGatt.class.getMethod("refresh");
                if (refresh != null) {
                    refresh.setAccessible(true);
                    result = (boolean) refresh.invoke(gatt, new Object[0]);
                }
            }
        } catch (Exception e) {
        }

        return result;
    }

    public static boolean isBleEnabled() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter.isEnabled();
    }
}
