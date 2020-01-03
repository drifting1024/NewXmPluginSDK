package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceInvalidStateRunningError extends Error {
    public DeviceInvalidStateRunningError() {
        super(13, "device invalid state running");
    }
}
