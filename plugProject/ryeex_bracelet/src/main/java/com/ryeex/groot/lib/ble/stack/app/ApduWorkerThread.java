package com.ryeex.groot.lib.ble.stack.app;

import android.os.Handler;

import com.ryeex.groot.lib.common.thread.MessageHandlerThread;

/**
 * Created by chenhao on 2017/12/24.
 */

public class ApduWorkerThread {
    private static Object sLock = new Object();
    private static MessageHandlerThread sWorkerThread;
    private static Handler sWorkerHandler;

    public static Handler get() {
        if (sWorkerThread == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sWorkerThread == null) {
                    sWorkerThread = new MessageHandlerThread("ApduWorkerThread");
                    sWorkerThread.start();
                    sWorkerHandler = new Handler(sWorkerThread.getLooper());
                }
            }
        }
        return sWorkerHandler;
    }
}
