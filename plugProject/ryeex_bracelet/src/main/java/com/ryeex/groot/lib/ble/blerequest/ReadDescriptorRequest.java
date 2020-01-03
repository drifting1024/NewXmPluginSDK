
package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.ble.requestresult.ReadDescriptorResult;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

import java.util.UUID;

/**
 * Created by chenhao on 17/1/3.
 */

public class ReadDescriptorRequest extends BaseRequest {
    public UUID service;
    public UUID character;
    public UUID descriptor;
    public AsyncCallback<ReadDescriptorResult, Error> callback;

    public ReadDescriptorRequest(UUID service, UUID character, UUID descriptor,
            AsyncCallback<ReadDescriptorResult, Error> callback) {
        this.service = service;
        this.character = character;
        this.descriptor = descriptor;
        this.callback = callback;
    }

    @Override
    public AsyncCallback getCallback() {
        return callback;
    }
}
