package com.ryeex.groot.lib.common.completecallback;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public abstract class CompleteCallback<R> {

    private static final int MSG_COMPLETE = 1;

    protected Handler mDispatcher;

    public CompleteCallback() {
        Looper looper = Looper.myLooper();

        if (looper == null) {
            mDispatcher = new Dispatcher<R>(this, Looper.getMainLooper());
        } else {
            mDispatcher = new Dispatcher<R>(this, looper);
        }
    }

    public abstract void onComplete(R result);


    public void sendCompleteMessage(R result) {
        mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_COMPLETE, result));
    }

    private static class Dispatcher<R> extends Handler {
        private CompleteCallback mCallback;

        Dispatcher(CompleteCallback callback, Looper looper) {
            super(looper);

            mCallback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_COMPLETE:
                    R result = (R) msg.obj;
                    mCallback.onComplete(result);
                    break;
            }
        }
    }
}