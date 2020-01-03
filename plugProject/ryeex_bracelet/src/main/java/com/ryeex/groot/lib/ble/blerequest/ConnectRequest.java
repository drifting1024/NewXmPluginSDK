
package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

/**
 * Created by chenhao on 2017/6/13.
 */

public class ConnectRequest extends BaseRequest {

    public AsyncCallback<Void, Error> callback;

    public ConnectRequest(AsyncCallback<Void, Error> callback) {
        this.callback = callback;
    }

    @Override
    public AsyncCallback getCallback() {
        return callback;
    }
}
