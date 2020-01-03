package com.ryeex.groot.lib.ble;

/**
 * Created by chenhao on 2017/6/12.
 */

public class BleErrorCode {
    public static final int BLE_UNKNOWN = 1;
    public static final int BLE_DISCONNECTED = 2;
    public static final int BLE_CONNECTING = 3;
    public static final int BLE_CONNECTED = 4;
    public static final int BLE_NOT_CONNECTED = 5;
    public static final int BLE_CONNECT_GATT_EXCEPTION = 6;
    public static final int BLE_WORKER_THREAD_NOT_READY = 7;
    public static final int BLE_GATT_IS_NULL = 8;
    public static final int BLE_CHARACTER_IS_NULL = 9;
    public static final int BLE_CHARACTER_NOT_READABLE = 10;
    public static final int BLE_CHARACTER_NOT_WRITABLE = 11;
    public static final int BLE_CHARACTER_NOT_WRITABLE_NO_RESPONSE = 12;
    public static final int BLE_CHARACTER_NOT_NOTIFIABLE = 13;
    public static final int BLE_CHARACTER_NOT_INDICATABLE = 14;
    public static final int BLE_CHARACTER_READ_FAIL = 15;
    public static final int BLE_CHARACTER_WRITE_FAIL = 16;
    public static final int BLE_CHARACTER_SET_NOTIFIABLE_FAIL = 17;
    public static final int BLE_CHARACTER_SET_INDICATABLE_FAIL = 18;
    public static final int BLE_DESCRIPTOR_IS_NULL = 19;
    public static final int BLE_DESCRIPTOR_READ_FAIL = 20;
    public static final int BLE_DESCRIPTOR_WRITE_FAIL = 21;
    public static final int BLE_DESCRIPTOR_SET_VALUE_FAIL = 22;
    public static final int BLE_RSSI_READ_FAIL = 23;
    public static final int BLE_TIMEOUT = 24;
    public static final int BLE_PARAM_WRONG = 25;
    public static final int BLE_TOKEN_IS_NULL = 26;

    public static final int BLE_ON_CHARACTER_CHANGED_TIMEOUT = 27;
    public static final int BLE_ACK_PACKAGE_IS_NULL = 28;
    public static final int BLE_ACK_PACKAGE_IS_NOT_SUCCESS = 29;
    public static final int BLE_ACK_PACKAGE_IS_NOT_READY = 30;
    public static final int BLE_SEND_DATA_PARAM_ERROR = 31;
    public static final int BLE_SEND_DATA_RETRY_TOO_MUCH = 32;
    public static final int BLE_ON_RECEIVE_BYTES_TIMEOUT = 33;
    public static final int BLE_PROTOBUF_PARSE_FAIL = 34;
    public static final int BLE_PB_RES_WRONG_FORMAT = 35;
    public static final int BLE_DEVICE_IS_NULL = 36;
    public static final int BLE_BLE_MANAGER_IS_NULL = 37;
    public static final int BLE_RBP_PARAM_WRONG = 38;
    public static final int BLE_PB_PARSER_IS_NULL = 39;
    public static final int BLE_PB_SESSION_ID_ERROR = 40;
    public static final int BLE_BIND_SET_TOKEN_FAIL = 41;
    public static final int BLE_CRYPTO_NOT_SUPPORT = 42;
    public static final int BLE_UNKNOWN_ACTIVATE_STATUS = 43;
    public static final int BLE_UNKNOWN_BIND_STATUS = 44;

}
