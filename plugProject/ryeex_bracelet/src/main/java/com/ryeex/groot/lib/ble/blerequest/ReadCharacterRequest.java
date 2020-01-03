
package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.ble.requestresult.ReadResult;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

import java.util.UUID;

/**
 * Created by chenhao on 17/1/3.
 */

public class ReadCharacterRequest extends BaseRequest {

    public UUID service;
    public UUID character;
    public AsyncCallback<ReadResult, Error> callback;

    public ReadCharacterRequest(UUID service, UUID character, AsyncCallback<ReadResult, Error> callback) {
        this.service = service;
        this.character = character;
        this.callback = callback;
    }

    @Override
    public AsyncCallback getCallback() {
        return callback;
    }
}
