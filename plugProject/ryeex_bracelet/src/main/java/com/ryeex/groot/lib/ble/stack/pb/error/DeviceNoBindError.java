package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceNoBindError extends Error {
    public DeviceNoBindError() {
        super(10, "device no bind");
    }
}
