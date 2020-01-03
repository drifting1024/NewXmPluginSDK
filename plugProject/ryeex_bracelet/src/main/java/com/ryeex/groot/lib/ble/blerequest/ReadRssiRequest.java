
package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.ble.requestresult.ReadRssiResult;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

/**
 * Created by chenhao on 17/1/3.
 */

public class ReadRssiRequest extends BaseRequest {

    public AsyncCallback<ReadRssiResult, Error> callback;

    public ReadRssiRequest(AsyncCallback<ReadRssiResult, Error> callback) {
        this.callback = callback;
    }

    @Override
    public AsyncCallback getCallback() {
        return callback;
    }
}
