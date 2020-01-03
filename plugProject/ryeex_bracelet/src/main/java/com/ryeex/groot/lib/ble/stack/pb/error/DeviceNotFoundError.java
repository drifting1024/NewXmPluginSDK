package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceNotFoundError extends Error {
    public DeviceNotFoundError() {
        super(3, "device not found(unknown)");
    }
}
