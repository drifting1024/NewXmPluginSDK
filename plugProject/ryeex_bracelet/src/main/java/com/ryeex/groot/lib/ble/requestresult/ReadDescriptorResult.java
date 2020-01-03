
package com.ryeex.groot.lib.ble.requestresult;

import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by chenhao on 17/1/3.
 */

public class ReadDescriptorResult {
    public BluetoothGattDescriptor descriptor;
    public int status;
    public byte[] bytes;
}
