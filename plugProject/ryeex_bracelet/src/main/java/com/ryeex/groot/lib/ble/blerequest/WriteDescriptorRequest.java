
package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.ble.requestresult.DescriptorWriteResult;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

import java.util.UUID;

/**
 * Created by chenhao on 17/1/3.
 */

public class WriteDescriptorRequest extends BaseRequest {
    public UUID service;
    public UUID character;
    public UUID descriptor;
    public byte[] value;
    public AsyncCallback<DescriptorWriteResult, Error> callback;

    public WriteDescriptorRequest(UUID service, UUID character, UUID descriptor, byte[] value,
            AsyncCallback<DescriptorWriteResult, Error> callback) {
        this.service = service;
        this.character = character;
        this.descriptor = descriptor;
        this.value = value;
        this.callback = callback;
    }

    @Override
    public AsyncCallback getCallback() {
        return callback;
    }
}
