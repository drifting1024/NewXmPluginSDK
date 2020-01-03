package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceLowPowerError extends Error {
    public DeviceLowPowerError() {
        super(1, "device low power");
    }
}
