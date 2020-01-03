package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceSignVerifyFailError extends Error {
    public DeviceSignVerifyFailError() {
        super(9, "device sign verify fail");
    }
}
