package com.ryeex.groot.lib.common.util;

import android.util.Log;

/**
 * Created by chenhao on 2017/11/9.
 */

public class LogUtil {
    public static String getCallStack() {
        return Log.getStackTraceString(new Throwable());
    }
}
