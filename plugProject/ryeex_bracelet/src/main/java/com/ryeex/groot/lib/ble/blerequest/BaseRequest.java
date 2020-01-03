
package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

/**
 * Created by chenhao on 17/1/3.
 */

public abstract class BaseRequest<R, E extends Error> {

    public abstract AsyncCallback<R, E> getCallback();
}
