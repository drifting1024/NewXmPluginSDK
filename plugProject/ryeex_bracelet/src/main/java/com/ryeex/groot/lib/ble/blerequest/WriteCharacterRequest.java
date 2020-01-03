package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.ble.requestresult.WriteResult;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

import java.util.UUID;

/**
 * Created by chenhao on 17/1/3.
 */

public class WriteCharacterRequest extends BaseRequest {
    public UUID service;
    public UUID character;
    public byte[] bytes;
    public AsyncCallback<WriteResult, Error> callback;

    public WriteCharacterRequest(UUID service, UUID character, byte[] bytes, AsyncCallback<WriteResult, Error> callback) {
        this.service = service;
        this.character = character;
        this.bytes = bytes;
        this.callback = callback;
    }

    @Override
    public AsyncCallback getCallback() {
        return callback;
    }
}
