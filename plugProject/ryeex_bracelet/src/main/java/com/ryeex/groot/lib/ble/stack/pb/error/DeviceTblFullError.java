package com.ryeex.groot.lib.ble.stack.pb.error;

import com.ryeex.groot.lib.common.Error;

public class DeviceTblFullError extends Error {
    public DeviceTblFullError() {
        super(7, "device tbl full");
    }
}
