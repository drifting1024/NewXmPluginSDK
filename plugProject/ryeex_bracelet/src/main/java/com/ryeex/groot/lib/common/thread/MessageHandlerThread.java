
package com.ryeex.groot.lib.common.thread;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

/**
 * Created by chenhao on 16/12/30.
 */

public class MessageHandlerThread extends HandlerThread {

    private static final int DUR_TIME = 2 * 60 * 60 * 1000;
    private static final int MSG_EMPTY_MSG = 1;
    private Handler mHandler;

    public MessageHandlerThread(String name) {
        super(name);
    }

    public MessageHandlerThread(String name, int priority) {
        super(name, priority);
    }

    void init() {
        mHandler = new Handler(this.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                mHandler.sendEmptyMessageDelayed(MSG_EMPTY_MSG, DUR_TIME);
            }
        };

        mHandler.sendEmptyMessageDelayed(MSG_EMPTY_MSG, DUR_TIME);
    }

    @Override
    public synchronized void start() {
        super.start();
        init();
    }
}
