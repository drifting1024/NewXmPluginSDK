package com.ryeex.groot.lib.common.util;

import android.content.SharedPreferences;

/**
 * Created by chenhao on 2017/11/9.
 */

public class PrefsUtil {

    public static void setSettingBoolean(SharedPreferences sharedPreferences, final String key, final boolean value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean(key, value).commit();
        }
    }

    public static boolean getSettingBoolean(SharedPreferences sharedPreferences, final String key, final boolean defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    public static void setSettingString(SharedPreferences sharedPreferences, final String key, final String value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(key, value).commit();
        }
    }

    public static String getSettingString(SharedPreferences sharedPreferences, final String key, final String defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public static int getSettingInt(SharedPreferences sharedPreferences, final String key, final int defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    public static void setSettingInt(SharedPreferences sharedPreferences, final String key, final int value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putInt(key, value).commit();
        }
    }

    public static long getSettingLong(SharedPreferences sharedPreferences, final String key, final long defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getLong(key, defaultValue);
        }
        return defaultValue;
    }

    public static void setSettingLong(SharedPreferences sharedPreferences, final String key, final long value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putLong(key, value).commit();
        }
    }

    public static void setSettingFloat(SharedPreferences sharedPreferences, final String key, final float value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putFloat(key, value).commit();
        }
    }

    public static void removePreference(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().remove(key).commit();
        }
    }

    public static void clearPreference(SharedPreferences p) {
        if (p != null) {
            SharedPreferences.Editor editor = p.edit();
            editor.clear();
            editor.commit();
        }
    }
}
