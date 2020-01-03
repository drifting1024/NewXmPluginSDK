package com.ryeex.groot.lib.log;

import android.util.Log;

import com.xiaomi.smarthome.device.api.XmPluginHostApi;

/**
 * Created by chenhao on 2017/10/11.
 */

public class Logger {
    public static boolean ENABLE_DEBUG_LOG = true;

    public static void v(String tag, String msg) {
        if (ENABLE_DEBUG_LOG) {
            Log.v(tag, msg);
        }
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (ENABLE_DEBUG_LOG) {
            Log.v(tag, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    /**
     * 只会本地打印
     *
     * @param msg
     */
    public static void d(String msg) {
        if (ENABLE_DEBUG_LOG) {
            Log.d("", msg);
        }
    }

    /**
     * 只会本地打印
     *
     * @param tag
     * @param msg
     */
    public static void d(String tag, String msg) {
        if (ENABLE_DEBUG_LOG) {
            Log.d(tag, msg);
        }
    }

    /**
     * 只会本地打印
     *
     * @param tag
     * @param msg
     * @param tr
     */
    public static void d(String tag, String msg, Throwable tr) {
        if (ENABLE_DEBUG_LOG) {
            Log.d(tag, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    public static void i(String tag, String msg) {
        if (ENABLE_DEBUG_LOG) {
            Log.i(tag, msg);
        }

        XmPluginHostApi.instance().log(tag, msg);
//        com.orhanobut.logger.Logger.t(tag);
//        com.orhanobut.logger.Logger.i(msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (ENABLE_DEBUG_LOG) {
            Log.i(tag, msg + '\n' + Log.getStackTraceString(tr));
        }

        XmPluginHostApi.instance().log(tag, msg + '\n' + Log.getStackTraceString(tr));
//        com.orhanobut.logger.Logger.t(tag);
//        com.orhanobut.logger.Logger.i(msg + '\n' + Log.getStackTraceString(tr));
    }

    public static void w(String tag, String msg) {
        if (ENABLE_DEBUG_LOG) {
            Log.w(tag, msg);
        }

        XmPluginHostApi.instance().log(tag, msg);
//        com.orhanobut.logger.Logger.t(tag);
//        com.orhanobut.logger.Logger.w(msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (ENABLE_DEBUG_LOG) {
            Log.w(tag, msg + '\n' + Log.getStackTraceString(tr));
        }

        XmPluginHostApi.instance().log(tag, msg);
//        com.orhanobut.logger.Logger.t(tag);
//        com.orhanobut.logger.Logger.w(msg + '\n' + Log.getStackTraceString(tr));
    }

    public static void w(String tag, Throwable tr) {
        if (ENABLE_DEBUG_LOG) {
            Log.w(tag, Log.getStackTraceString(tr));
        }

        XmPluginHostApi.instance().log(tag, Log.getStackTraceString(tr));
//        com.orhanobut.logger.Logger.t(tag);
//        com.orhanobut.logger.Logger.w(Log.getStackTraceString(tr));
    }

    public static void e(String tag, String msg) {
        if (ENABLE_DEBUG_LOG) {
            Log.e(tag, msg);
        }

        XmPluginHostApi.instance().log(tag, msg);
//        com.orhanobut.logger.Logger.t(tag);
//        com.orhanobut.logger.Logger.e(msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (ENABLE_DEBUG_LOG) {
            Log.e(tag, msg + '\n' + Log.getStackTraceString(tr));
        }

        XmPluginHostApi.instance().log(tag, msg + '\n' + Log.getStackTraceString(tr));
//        com.orhanobut.logger.Logger.t(tag);
//        com.orhanobut.logger.Logger.e(msg + '\n' + Log.getStackTraceString(tr));
    }
}
