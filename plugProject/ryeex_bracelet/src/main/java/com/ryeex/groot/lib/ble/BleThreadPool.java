package com.ryeex.groot.lib.ble;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by chenhao on 2017/11/6.
 */

public class BleThreadPool {
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    public static ExecutorService get() {
        return threadPool;
    }
}
