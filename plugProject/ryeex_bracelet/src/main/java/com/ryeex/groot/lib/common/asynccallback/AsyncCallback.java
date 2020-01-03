package com.ryeex.groot.lib.common.asynccallback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ryeex.groot.lib.common.Error;

/**
 * Created by chenhao on 17/5/23.
 */

public abstract class AsyncCallback<R, E extends Error> {

    private static final int MSG_SUCCESS = 1;
    private static final int MSG_PROGRESS = 2;
    private static final int MSG_UPDATE = 3;
    private static final int MSG_FAILURE = 4;

    protected Handler mDispatcher;

    public AsyncCallback() {
        Looper looper = Looper.myLooper();

        if (looper == null) {
            if (!(this instanceof SyncCallback)) {
                mDispatcher = new Dispatcher<R, E>(this, Looper.getMainLooper());
            }
        } else {
            mDispatcher = new Dispatcher<R, E>(this, looper);
        }
    }

    public abstract void onSuccess(R result);

    public void onProgress(float progress) {
    }

    public void onUpdate(Bundle update) {
    }

    public abstract void onFailure(E error);

    /**
     * 向调用线程发送成功消息
     *
     * @param result
     */
    public void sendSuccessMessage(R result) {
        mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_SUCCESS, result));
    }

    public void sendProgressMessage(float progress) {
        mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_PROGRESS, progress));
    }

    public void sendUpdateMessage(Bundle update) {
        mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_UPDATE, update));
    }

    /**
     * 向调用线程发送失败消息
     */
    public void sendFailureMessage(E error) {
        mDispatcher.sendMessage(mDispatcher.obtainMessage(MSG_FAILURE, error));
    }

    private static class Dispatcher<R, E extends Error> extends Handler {
        private AsyncCallback<R, E> mCallback;

        Dispatcher(AsyncCallback callback, Looper looper) {
            super(looper);

            mCallback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS:
                    R result = (R) msg.obj;
                    mCallback.onSuccess(result);
                    break;
                case MSG_PROGRESS:
                    float progress = (float) msg.obj;
                    mCallback.onProgress(progress);
                    break;
                case MSG_UPDATE:
                    Bundle update = (Bundle) msg.obj;
                    mCallback.onUpdate(update);
                    break;
                case MSG_FAILURE:
                    E err = (E) msg.obj;
                    mCallback.onFailure(err);
                    break;
            }
        }
    }
}
