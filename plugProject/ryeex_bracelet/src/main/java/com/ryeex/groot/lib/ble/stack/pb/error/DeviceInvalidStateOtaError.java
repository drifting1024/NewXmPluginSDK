package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceInvalidStateOtaError extends Error {
    public DeviceInvalidStateOtaError() {
        super(14, "device invalid state ota");
    }
}
