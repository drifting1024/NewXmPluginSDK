package com.ryeex.groot.lib.common.util;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by chenhao on 2018/2/5.
 */

public class TimeUtil {

    private static SimpleDateFormat sdf = null;

    public static String formatUTC(long l, String strPattern) {
        if (TextUtils.isEmpty(strPattern)) {
            strPattern = "yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat sdf = null;

        try {
            sdf = new SimpleDateFormat(strPattern, Locale.CHINA);
        } catch (Throwable e) {
        }

        return sdf.format(l);
    }
}
