package com.ryeex.groot.lib.common.util;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * Created by chenhao on 2017/9/22.
 */

public class SDCardUtil {
    /**
     * 没有检测到SD卡
     *
     * @return
     */
    public static boolean isSDCardUnavailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED);
    }

    /**
     * @return true 如果SD卡处于不可读写的状态
     */
    public static boolean isSDCardBusy() {
        return !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 检查SD卡是否已满。如果SD卡的剩余空间小于100K，则认为SD卡已满。
     *
     * @param
     * @return
     */
    public static boolean isSDCardFull() {
        return getSDCardAvailableBytes() <= (100 * 1024);
    }

    public static boolean isSDCardUseful() {
        return (!SDCardUtil.isSDCardBusy()) && (!SDCardUtil.isSDCardFull()) && (!SDCardUtil.isSDCardUnavailable());
    }

    /**
     * 获取ＳＤ卡的剩余字节数。
     *
     * @return
     */
    public static long getSDCardAvailableBytes() {
        if (isSDCardBusy()) {
            return 0;
        }

        final File path = Environment.getExternalStorageDirectory();
        final StatFs stat = new StatFs(path.getPath());
        final long blockSize = stat.getBlockSize();
        final long availableBlocks = stat.getAvailableBlocks();
        return blockSize * (availableBlocks - 4);
    }
}
