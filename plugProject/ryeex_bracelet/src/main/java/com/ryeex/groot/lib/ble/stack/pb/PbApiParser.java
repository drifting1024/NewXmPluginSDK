package com.ryeex.groot.lib.ble.stack.pb;


import com.google.protobuf.ByteString;

/**
 * Created by chenhao on 2017/11/6.
 */
public interface PbApiParser<T> {
    T parse(ByteString val) throws Exception;
}