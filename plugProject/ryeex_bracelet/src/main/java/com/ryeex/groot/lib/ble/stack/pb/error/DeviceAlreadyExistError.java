package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceAlreadyExistError extends Error {
    public DeviceAlreadyExistError() {
        super(11, "device already exist");
    }
}
