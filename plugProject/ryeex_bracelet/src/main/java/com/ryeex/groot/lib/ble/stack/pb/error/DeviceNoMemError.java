package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceNoMemError extends Error {
    public DeviceNoMemError() {
        super(5, "device no mem");
    }
}
