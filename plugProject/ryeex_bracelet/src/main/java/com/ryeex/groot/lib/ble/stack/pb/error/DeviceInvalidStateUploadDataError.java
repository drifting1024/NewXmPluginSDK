package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceInvalidStateUploadDataError extends Error {
    public DeviceInvalidStateUploadDataError() {
        super(16, "device invalid state upload data");
    }
}
