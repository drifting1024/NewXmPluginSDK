package com.ryeex.groot.lib.ble.blerequest;

import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;

import java.util.UUID;

/**
 * Created by chenhao on 2017/7/3.
 */

public class WriteCharacterWithoutRspRequest extends BaseRequest {
    public UUID service;
    public UUID character;
    public byte[] bytes;
    public AsyncCallback<Void, Error> callback;

    public WriteCharacterWithoutRspRequest(UUID service, UUID character, byte[] bytes, AsyncCallback<Void, Error> callback) {
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
