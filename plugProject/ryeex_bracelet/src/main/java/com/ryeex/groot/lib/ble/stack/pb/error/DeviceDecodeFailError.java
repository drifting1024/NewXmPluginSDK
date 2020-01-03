package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceDecodeFailError extends Error {
    public DeviceDecodeFailError() {
        super(2, "device decode fail");
    }
}
