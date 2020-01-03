package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceInvalidStateCardError extends Error {
    public DeviceInvalidStateCardError() {
        super(15, "device invalid state card");
    }
}
