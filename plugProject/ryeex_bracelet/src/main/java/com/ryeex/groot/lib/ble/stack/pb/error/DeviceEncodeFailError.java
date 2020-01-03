package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceEncodeFailError extends Error {
    public DeviceEncodeFailError() {
        super(9, "device encode fail");
    }
}
