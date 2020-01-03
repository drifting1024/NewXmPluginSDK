package com.ryeex.groot.lib.common.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;

/**
 * Created by chenhao on 2017/10/21.
 */

public class StorageUtil {
    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false otherwise.
     */
    @SuppressLint("NewApi")
    public static boolean isExternalStorageRemovable() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD || Environment.isExternalStorageRemovable();
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    @SuppressLint("NewApi")
    public static File getExternalCacheDir(final Context context) {
        if (hasExternalCacheDir()) {
            File file = context.getExternalCacheDir();
            if (file != null) {
                return file;
            }
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    /**
     * Check if OS version has built-in external cache dir method.
     *
     * @return
     */
    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }
}
