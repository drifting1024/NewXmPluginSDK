
package com.ryeex.groot.lib.ble.requestresult;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by chenhao on 17/1/3.
 */

public class ReadResult {
    public BluetoothGattCharacteristic characteristic;
    public int status;
    public byte[] value;
}
