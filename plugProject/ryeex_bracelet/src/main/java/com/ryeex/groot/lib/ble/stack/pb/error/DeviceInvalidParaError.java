package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceInvalidParaError extends Error {
    public DeviceInvalidParaError() {
        super(4, "device invalid param");
    }
}
