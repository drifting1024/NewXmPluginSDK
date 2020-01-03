
package com.ryeex.groot.lib.common.asynccallback;

import com.ryeex.groot.lib.common.Error;

/**
 * Created by chenhao on 17/5/23.
 */

public abstract class SyncCallback<R, E extends Error> extends AsyncCallback<R, E> {

    private boolean mIsSuccess = false;
    private R mResult;
    private E mError;

    private Object mSyncLock = null;

    public SyncCallback() {
        super();
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    public R getResult() {
        return mResult;
    }

    public E getError() {
        return mError;
    }

    public Object getSyncLock() {
        return mSyncLock;
    }

    // 使用同步锁模式, 基于异步模拟同步
    public void setUseSyncLockMode() {
        mSyncLock = new Object();
    }

    public boolean isSyncLockMode() {
        return (mSyncLock != null);
    }

    @Override
    public void sendSuccessMessage(R result) {
        if (isSyncLockMode()) {
            mIsSuccess = true;
            mResult = result;

            synchronized (mSyncLock) {
                mSyncLock.notify();
            }
        } else {
            onSuccess(result);
        }
    }

    @Override
    public void sendFailureMessage(E error) {
        if (isSyncLockMode()) {
            mIsSuccess = false;
            mError = error;

            synchronized (mSyncLock) {
                mSyncLock.notify();
            }
        } else {
            onFailure(error);
        }
    }
}
