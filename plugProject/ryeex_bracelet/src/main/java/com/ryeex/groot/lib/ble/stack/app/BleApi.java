package com.ryeex.groot.lib.ble.stack.app;

import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Int32Value;
import com.ryeex.groot.lib.ble.BleManager;
import com.ryeex.groot.lib.ble.stack.app.util.ByteStringWrapper;
import com.ryeex.groot.lib.ble.stack.pb.PbApi;
import com.ryeex.groot.lib.ble.stack.pb.PbApiParser;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBAlarmClock;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDataUpload;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevApp;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevLog;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevice;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevice.DeviceInfo;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevice.DeviceRunState;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevice.DeviceStatus;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBFirmware;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBGate;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBMiScene;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBNotification;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBPhoneApp;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBProperty;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBRbp;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBSurface;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBTransit;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBWeather;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.log.Logger;

import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;
import static com.ryeex.groot.lib.ble.stack.crypto.Crypto.CRYPTO.OPEN;
import static com.ryeex.groot.lib.ble.stack.crypto.Crypto.CRYPTO.RC4;


/**
 * Created by chenhao on 2017/11/7.
 */

public class BleApi {
    public static void getDeviceStatus(BleManager bleManager, AsyncCallback<DeviceStatus, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDeviceStatus");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GET_DEV_STATUS, null, OPEN, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<DeviceStatus>() {
            @Override
            public DeviceStatus parse(ByteString val) throws Exception {
                DeviceStatus deviceStatus = DeviceStatus.parseFrom(val);
                return deviceStatus;
            }
        }, callback);
    }

    public static void getDeviceRunState(BleManager bleManager, AsyncCallback<DeviceRunState, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDeviceRunState");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GET_DEV_RUN_STATE, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<DeviceRunState>() {
            @Override
            public DeviceRunState parse(ByteString val) throws Exception {
                DeviceRunState deviceRunState = DeviceRunState.parseFrom(val);
                return deviceRunState;
            }
        }, callback);
    }

    public static void getDeviceInfo(BleManager bleManager, AsyncCallback<DeviceInfo, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDeviceInfo");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GET_DEV_INFO, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<DeviceInfo>() {
            @Override
            public DeviceInfo parse(ByteString val) throws Exception {
                DeviceInfo deviceInfo = DeviceInfo.parseFrom(val);
                return deviceInfo;
            }
        }, callback);
    }

    public static void getUnbindToken(BleManager bleManager, AsyncCallback<PBDevice.DeviceUnBindToken, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getUnbindToken");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GET_DEV_UNBIND_TOKEN, null, OPEN, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBDevice.DeviceUnBindToken>() {
            @Override
            public PBDevice.DeviceUnBindToken parse(ByteString val) throws Exception {
                PBDevice.DeviceUnBindToken unBindToken = PBDevice.DeviceUnBindToken.parseFrom(val);
                return unBindToken;
            }
        }, callback);
    }

    public static void startBindAck(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startBindAck");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_BIND_ACK_START, null, OPEN, 30 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getDevCredential(BleManager bleManager, final AsyncCallback<PBDevice.DeviceCredential, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDevCredential");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GET_DEV_CREDENTIAL, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBDevice.DeviceCredential>() {
            @Override
            public PBDevice.DeviceCredential parse(ByteString val) throws Exception {
                PBDevice.DeviceCredential credential = PBDevice.DeviceCredential.parseFrom(val);
                return credential;
            }
        }, callback);
    }

    public static void startBindResult(BleManager bleManager, int code, String uid, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startBindResult code:" + code + " uid:" + uid);

        PBDevice.BindResult.Builder bindResultBuilder = PBDevice.BindResult.newBuilder();
        bindResultBuilder.setError(code);
        if (!TextUtils.isEmpty(uid)) {
            bindResultBuilder.setUid(uid);
        }

        PBDevice.BindResult bindResult = bindResultBuilder.build();

        PbApi.sendReqNoRes(bleManager, PBRbp.CMD.DEV_BIND_RESULT, bindResult.toByteArray(), RC4, callback);
    }

    public static void unbindByDid(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.unbindByDid");

        PbApi.sendReqNoRes(bleManager, PBRbp.CMD.DEV_UNBIND, null, RC4, callback);
    }

    public static void unbindByUnbindToken(BleManager bleManager, PBDevice.ServerUnBindToken serverUnBindToken, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.unbindByUnbindToken " + serverUnBindToken);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_UNBIND, serverUnBindToken.toByteArray(), OPEN, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void syncTime(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.syncTime");

        long currentTimeLong = System.currentTimeMillis();
        int currentTimeInt = (int) (currentTimeLong / 1000);
        Int32Value currentTimeInt32 = Int32Value.newBuilder().setValue(currentTimeInt).build();

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SET_TIME, currentTimeInt32.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void setPhoneAppInfo(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setPhoneAppInfo");

        PBPhoneApp.PhoneAppInfo.Builder builder = PBPhoneApp.PhoneAppInfo.newBuilder();
        builder.setType(1);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SET_PHONE_APP_INFO, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startFirmwareUpdateToken(BleManager bleManager, byte[] otaTokenBytes, AsyncCallback<PBFirmware.FwUpdateTokenResult, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startFirmwareUpdateToken");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_FW_UPDATE_TOKEN, otaTokenBytes, OPEN, 20 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBFirmware.FwUpdateTokenResult>() {
            @Override
            public PBFirmware.FwUpdateTokenResult parse(ByteString val) throws Exception {
                PBFirmware.FwUpdateTokenResult tokenResult = PBFirmware.FwUpdateTokenResult.parseFrom(val);
                return tokenResult;
            }
        }, callback);
    }

    public static void startFirmwareUpdateStart(BleManager bleManager, PBFirmware.FwUpdateInfo fwUpdateInfo, AsyncCallback<PBFirmware.FwUpdateStartResult, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startFirmwareUpdateStart " + fwUpdateInfo);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_FW_UPDATE_START, fwUpdateInfo.toByteArray(), RC4, 20 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBFirmware.FwUpdateStartResult>() {
            @Override
            public PBFirmware.FwUpdateStartResult parse(ByteString val) throws Exception {
                PBFirmware.FwUpdateStartResult startResult;
                try {
                    startResult = PBFirmware.FwUpdateStartResult.parseFrom(val);
                } catch (Exception e) {
                    startResult = PBFirmware.FwUpdateStartResult.newBuilder().setTransferedLength(0).build();
                }
                return startResult;
            }
        }, callback);
    }

    public static void startFirmwareUpdateFile(BleManager bleManager, byte[] fileBytes, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startFirmwareUpdateFile");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_FW_UPDATE_FILE, fileBytes, OPEN, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startFirmwareUpdateStop(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startFirmwareUpdateStop");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_FW_UPDATE_STOP, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startUploadDataStart(BleManager bleManager, AsyncCallback<PBDataUpload.UploadDataStartParam, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startUploadDataStart");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_UPLOAD_DATA_START, null, RC4, 5 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBDataUpload.UploadDataStartParam>() {
            @Override
            public PBDataUpload.UploadDataStartParam parse(ByteString val) throws Exception {
                PBDataUpload.UploadDataStartParam startParam = PBDataUpload.UploadDataStartParam.parseFrom(val);
                return startParam;
            }
        }, callback);
    }

    public static void startNotification(BleManager bleManager, PBNotification.Notification notification, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startNotification " + notification);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_NOTIFICATION, notification.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getSportPedoCurrentStep(BleManager bleManager, AsyncCallback<PBProperty.SportPedoCurrentStepsPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getSportPedoCurrentStep");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.SPORT_PEDO_CURRENT_STEPS);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.SportPedoCurrentStepsPropVal>() {
            @Override
            public PBProperty.SportPedoCurrentStepsPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.SportPedoCurrentStepsPropVal propVal = PBProperty.SportPedoCurrentStepsPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void getHealthLastHeartRate(BleManager bleManager, AsyncCallback<PBProperty.HealthLastHeartRatePropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getHealthLastHeartRate");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_LAST_HEART_RATE);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthLastHeartRatePropVal>() {
            @Override
            public PBProperty.HealthLastHeartRatePropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthLastHeartRatePropVal propVal = PBProperty.HealthLastHeartRatePropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void getHealthRestingHeartRate(BleManager bleManager, AsyncCallback<PBProperty.HealthRestingHeartRatePropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getHealthRestingHeartRate");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_RESTING_HEART_RATE);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthRestingHeartRatePropVal>() {
            @Override
            public PBProperty.HealthRestingHeartRatePropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthRestingHeartRatePropVal propVal = PBProperty.HealthRestingHeartRatePropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void getHealthSettingHeartRateAuto(BleManager bleManager, AsyncCallback<PBProperty.HealthSettingHeartRateAutoPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getHealthSettingHeartRateAuto");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_SETTING_HEART_RATE_AUTO);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthSettingHeartRateAutoPropVal>() {
            @Override
            public PBProperty.HealthSettingHeartRateAutoPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthSettingHeartRateAutoPropVal propVal = PBProperty.HealthSettingHeartRateAutoPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void setHealthSettingHeartRateAuto(BleManager bleManager, boolean enable, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setHealthSettingHeartRateAuto " + enable);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.HEALTH_SETTING_HEART_RATE_AUTO);
        builder.setPropVal(PBProperty.HealthSettingHeartRateAutoPropVal.newBuilder().setEnable(enable ? 1 : 0).build().toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getDeviceRemainingPower(BleManager bleManager, AsyncCallback<PBProperty.DeviceRemainingPowerPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDeviceRemainingPower");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.DEVICE_REMAINING_POWER);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.DeviceRemainingPowerPropVal>() {
            @Override
            public PBProperty.DeviceRemainingPowerPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.DeviceRemainingPowerPropVal propVal = PBProperty.DeviceRemainingPowerPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void getHealthCurrentCalories(BleManager bleManager, AsyncCallback<PBProperty.HealthCurrentCaloriesPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDeviceRemainingPower");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_CURRENT_CALORIES);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthCurrentCaloriesPropVal>() {
            @Override
            public PBProperty.HealthCurrentCaloriesPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthCurrentCaloriesPropVal propVal = PBProperty.HealthCurrentCaloriesPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void getHealthCurrentDistance(BleManager bleManager, AsyncCallback<PBProperty.HealthCurrentDistancePropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDeviceRemainingPower");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_CURRENT_DISTANCE);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthCurrentDistancePropVal>() {
            @Override
            public PBProperty.HealthCurrentDistancePropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthCurrentDistancePropVal propVal = PBProperty.HealthCurrentDistancePropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void getDeviceSettingRaiseToWake(BleManager bleManager, AsyncCallback<PBProperty.DeviceSettingRaiseToWakePropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getDeviceSettingRaiseToWake");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.DEVICE_SETTING_RAISE_TO_WAKE);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.DeviceSettingRaiseToWakePropVal>() {
            @Override
            public PBProperty.DeviceSettingRaiseToWakePropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.DeviceSettingRaiseToWakePropVal propVal = PBProperty.DeviceSettingRaiseToWakePropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void setDeviceSettingRaiseToWake(BleManager bleManager, boolean enable, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setDeviceSettingRaiseToWake " + enable);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.DEVICE_SETTING_RAISE_TO_WAKE);
        builder.setPropVal(PBProperty.DeviceSettingRaiseToWakePropVal.newBuilder().setEnable(enable ? 1 : 0).build().toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getNotificationSettingMsgRedPoint(BleManager bleManager, AsyncCallback<PBProperty.NotificationSettingMsgRedPointPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getNotificationSettingMsgRedPoint");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.NOTIFICATION_SETTING_MSG_RED_POINT);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.NotificationSettingMsgRedPointPropVal>() {
            @Override
            public PBProperty.NotificationSettingMsgRedPointPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.NotificationSettingMsgRedPointPropVal propVal = PBProperty.NotificationSettingMsgRedPointPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void setNotificationSettingMsgRedPoint(BleManager bleManager, boolean enable, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setNotificationSettingMsgRedPoint " + enable);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.NOTIFICATION_SETTING_MSG_RED_POINT);
        builder.setPropVal(PBProperty.NotificationSettingMsgRedPointPropVal.newBuilder().setEnable(enable ? 1 : 0).build().toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getNotificationSettingRepeat(BleManager bleManager, AsyncCallback<PBProperty.NotificationSettingRepeatPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getNotificationSettingRepeat");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.NOTIFICATION_SETTING_REPEAT);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.NotificationSettingRepeatPropVal>() {
            @Override
            public PBProperty.NotificationSettingRepeatPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.NotificationSettingRepeatPropVal propVal = PBProperty.NotificationSettingRepeatPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void setNotificationSettingRepeat(BleManager bleManager, int times, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setNotificationSettingRepeat " + times);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.NOTIFICATION_SETTING_REPEAT);
        builder.setPropVal(PBProperty.NotificationSettingRepeatPropVal.newBuilder().setTimes(times).build().toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startTransitCreateStart(BleManager bleManager, PBTransit.TransitCreateParam transitCreateParam, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startTransitCreateStart " + transitCreateParam);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_TRANSIT_START_CREATE, transitCreateParam.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startTransitCreateAck(BleManager bleManager, PBTransit.TransitCreateAck transitCreateAck, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startTransitCreateAck " + transitCreateAck);

        PbApi.sendReqNoRes(bleManager, PBRbp.CMD.DEV_TRANSIT_CREATE_ACK, transitCreateAck.toByteArray(), RC4, callback);
    }

    public static void startGateCheck(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startGateCheck");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GATE_START_CHECK, null, RC4, 10 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startGateCreate(BleManager bleManager, PBGate.GateCreateParam gateCreateParam, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startGateCreate " + gateCreateParam);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GATE_START_CREATE, gateCreateParam.toByteArray(), RC4, 10 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startGateCreateAck(BleManager bleManager, PBGate.GateCreateAck gateCreateAck, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startGateCreateAck " + gateCreateAck);

        PbApi.sendReqNoRes(bleManager, PBRbp.CMD.DEV_GATE_CREATE_ACK, gateCreateAck.toByteArray(), RC4, callback);
    }

    public static void startGateDelete(BleManager bleManager, PBGate.GateDeleteParam gateDeleteParam, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startGateDelete " + gateDeleteParam);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GATE_START_DELETE, gateDeleteParam.toByteArray(), RC4, 10 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startGateDeleteAck(BleManager bleManager, PBGate.GateDeleteAck gateDeleteAck, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startGateDeleteAck " + gateDeleteAck);

        PbApi.sendReqNoRes(bleManager, PBRbp.CMD.DEV_GATE_DELETE_ACK, gateDeleteAck.toByteArray(), RC4, callback);
    }

    public static void startGetGateInfoList(BleManager bleManager, AsyncCallback<PBGate.GetGateInfoListResult, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startGetGateInfoList");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GATE_GET_INFO_LIST, null, OPEN, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBGate.GetGateInfoListResult>() {
            @Override
            public PBGate.GetGateInfoListResult parse(ByteString val) throws Exception {
                PBGate.GetGateInfoListResult getGateInfoListResult = PBGate.GetGateInfoListResult.parseFrom(val);
                return getGateInfoListResult;
            }
        }, callback);
    }

    public static void startSetGateInfo(BleManager bleManager, PBGate.GateCardInfo gateCardInfo, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSetGateInfo " + gateCardInfo);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_GATE_SET_INFO, gateCardInfo.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startUploadFile(BleManager bleManager, byte[] val, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startUploadFile");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_UPLOAD_FILE, val, RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startWeatherGetCityList(BleManager bleManager, AsyncCallback<PBWeather.WeatherCityList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startWeatherGetCityList");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_WEATHER_GET_CITY_LIST, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBWeather.WeatherCityList>() {
            @Override
            public PBWeather.WeatherCityList parse(ByteString val) throws Exception {
                PBWeather.WeatherCityList weatherCityList = PBWeather.WeatherCityList.parseFrom(val);
                return weatherCityList;
            }
        }, callback);
    }

    public static void startWeatherSetCityList(BleManager bleManager, PBWeather.WeatherCityList weatherCityList, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startWeatherSetCityList " + weatherCityList);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_WEATHER_SET_CITY_LIST, weatherCityList.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startAlarmClockGetList(BleManager bleManager, AsyncCallback<PBAlarmClock.AlarmClockList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startAlarmClockGetList");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_ALARM_CLOCK_GET_LIST, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBAlarmClock.AlarmClockList>() {
            @Override
            public PBAlarmClock.AlarmClockList parse(ByteString val) throws Exception {
                PBAlarmClock.AlarmClockList alarmClockList = PBAlarmClock.AlarmClockList.parseFrom(val);
                return alarmClockList;
            }
        }, callback);
    }

    public static void startAlarmClockSet(BleManager bleManager, PBAlarmClock.AlarmClockItem alarmClockItem, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startAlarmClockSet " + alarmClockItem);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_ALARM_CLOCK_SET, alarmClockItem.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startAlarmClockDelete(BleManager bleManager, int alarmClockId, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startAlarmClockDelete " + alarmClockId);

        PBAlarmClock.AlarmClockDeleteItem.Builder builder = PBAlarmClock.AlarmClockDeleteItem.newBuilder();
        builder.setId(alarmClockId);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_ALARM_CLOCK_DELETE, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startSeOpen(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSeOpen");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SE_OPEN, null, RC4, 6 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startSeClose(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSeClose");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SE_CLOSE, null, RC4, 6 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static byte[] startSeExecuteApdu(final BleManager bleManager, final byte[] apdu) {
        Logger.i(TAG_BLE, "BleApi.startSeExecuteApdu " + ByteUtil.byteToString(apdu));

        final ByteStringWrapper byteStringWrapper = new ByteStringWrapper();

        final Object syncLock = new Object();

        ApduWorkerThread.get().post(new Runnable() {
            @Override
            public void run() {
                AsyncCallback<ByteString, Error> callback = new AsyncCallback<ByteString, Error>() {
                    @Override
                    public void onSuccess(ByteString result) {

                        byteStringWrapper.byteString = result;

                        synchronized (syncLock) {
                            syncLock.notify();
                        }
                    }

                    @Override
                    public void onFailure(Error error) {

                        byteStringWrapper.byteString = null;

                        synchronized (syncLock) {
                            syncLock.notify();
                        }
                    }
                };

                PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SE_EXECUTE_APDU, apdu, RC4, 5 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<ByteString>() {
                    @Override
                    public ByteString parse(ByteString val) throws Exception {
                        return val;
                    }
                }, callback);
            }
        });

        synchronized (syncLock) {
            try {
                syncLock.wait();
            } catch (InterruptedException e) {
            }
        }

        byte[] finalResult;

        ByteString apduResultByteStr = byteStringWrapper.byteString;
        if (apduResultByteStr == null) {
            finalResult = null;
        } else {
            byte[] apduResultBytes = apduResultByteStr.toByteArray();
            finalResult = apduResultBytes;
        }


        return finalResult;
    }

    public static byte[] startSeExecuteApduOpen(final BleManager bleManager, final byte[] apdu) {
        Logger.i(TAG_BLE, "BleApi.startSeExecuteApduOpen " + ByteUtil.byteToString(apdu));

        final ByteStringWrapper byteStringWrapper = new ByteStringWrapper();

        final Object syncLock = new Object();

        ApduWorkerThread.get().post(new Runnable() {
            @Override
            public void run() {
                AsyncCallback<ByteString, Error> callback = new AsyncCallback<ByteString, Error>() {
                    @Override
                    public void onSuccess(ByteString result) {

                        byteStringWrapper.byteString = result;

                        synchronized (syncLock) {
                            syncLock.notify();
                        }
                    }

                    @Override
                    public void onFailure(Error error) {

                        byteStringWrapper.byteString = null;

                        synchronized (syncLock) {
                            syncLock.notify();
                        }
                    }
                };

                PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SE_EXECUTE_APDU_OPEN, apdu, OPEN, 5 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<ByteString>() {
                    @Override
                    public ByteString parse(ByteString val) throws Exception {
                        return val;
                    }
                }, callback);
            }
        });

        synchronized (syncLock) {
            try {
                syncLock.wait();
            } catch (InterruptedException e) {
            }
        }

        byte[] finalResult;

        ByteString apduResultByteStr = byteStringWrapper.byteString;
        if (apduResultByteStr == null) {
            finalResult = null;
        } else {
            byte[] apduResultBytes = apduResultByteStr.toByteArray();
            finalResult = apduResultBytes;
        }


        return finalResult;
    }

    public static void startActivateSeStart(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startActivateSeStart");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_ACTIVATE_SE_START, null, OPEN, 5 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startActivateSeResult(BleManager bleManager, PBDevice.SeActivateResult result, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startActivateSeResult " + result);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_ACTIVATE_SE_RESULT, result.toByteArray(), OPEN, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getHealthSettingSitAlert(BleManager bleManager, AsyncCallback<PBProperty.HealthSettingSitAlertPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getHealthSettingSitAlert");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_SETTING_SIT_ALERT);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthSettingSitAlertPropVal>() {
            @Override
            public PBProperty.HealthSettingSitAlertPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthSettingSitAlertPropVal propVal = PBProperty.HealthSettingSitAlertPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void setHealthSettingSitAlert(BleManager bleManager, PBProperty.HealthSettingSitAlertPropVal propVal, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setHealthSettingSitAlert " + propVal);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.HEALTH_SETTING_SIT_ALERT);
        builder.setPropVal(propVal.toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getHealthSettingSleepAlert(BleManager bleManager, AsyncCallback<PBProperty.HealthSettingSleepAlertPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getHealthSettingSleepAlert");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_SETTING_SLEEP_ALERT);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthSettingSleepAlertPropVal>() {
            @Override
            public PBProperty.HealthSettingSleepAlertPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthSettingSleepAlertPropVal propVal = PBProperty.HealthSettingSleepAlertPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void setHealthSettingSleepAlert(BleManager bleManager, PBProperty.HealthSettingSleepAlertPropVal propVal, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setHealthSettingSleepAlert " + propVal);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.HEALTH_SETTING_SLEEP_ALERT);
        builder.setPropVal(propVal.toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void getHealthTargetStep(BleManager bleManager, AsyncCallback<PBProperty.HealthTargetStepPropVal, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.getHealthTargetStep");

        PBProperty.PropGetParam.Builder builder = PBProperty.PropGetParam.newBuilder();
        builder.addPropId(PBProperty.PROP_ID.HEALTH_TARGET_STEP);

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_GET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBProperty.HealthTargetStepPropVal>() {
            @Override
            public PBProperty.HealthTargetStepPropVal parse(ByteString val) throws Exception {
                PBProperty.PropGetResult propGetResult = PBProperty.PropGetResult.parseFrom(val);
                PBProperty.HealthTargetStepPropVal propVal = PBProperty.HealthTargetStepPropVal.parseFrom(propGetResult.getPropVal(0));
                return propVal;
            }
        }, callback);
    }

    public static void setHealthTargetStep(BleManager bleManager, PBProperty.HealthTargetStepPropVal propVal, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setHealthTargetStep " + propVal);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.HEALTH_TARGET_STEP);
        builder.setPropVal(propVal.toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startSurfaceGetList(BleManager bleManager, AsyncCallback<PBSurface.SurfaceList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSurfaceGetList");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SURFACE_GET_LIST, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBSurface.SurfaceList>() {
            @Override
            public PBSurface.SurfaceList parse(ByteString val) throws Exception {
                PBSurface.SurfaceList surfaceList = PBSurface.SurfaceList.parseFrom(val);
                return surfaceList;
            }
        }, callback);
    }

    public static void startSurfaceAddStart(BleManager bleManager, PBSurface.SurfaceAddStartParam addStartParam, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSurfaceAddStart " + addStartParam);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SURFACE_ADD_START, addStartParam.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startSurfaceAddData(BleManager bleManager, byte[] fileBytes, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSurfaceAddData");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SURFACE_ADD_DATA, fileBytes, RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startSurfaceDelete(BleManager bleManager, PBSurface.SurfaceDeleteParam deleteParam, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSurfaceDelete " + deleteParam);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SURFACE_DELETE, deleteParam.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startSurfaceSetCurrent(BleManager bleManager, PBSurface.SurfaceSetCurrentParam setCurrentParam, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startSurfaceSetCurrent " + setCurrentParam);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_SURFACE_SET_CURRENT, setCurrentParam.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startAppGetList(BleManager bleManager, AsyncCallback<PBDevApp.DevAppList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startAppGetList");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_APP_GET_LIST, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBDevApp.DevAppList>() {
            @Override
            public PBDevApp.DevAppList parse(ByteString val) throws Exception {
                PBDevApp.DevAppList devAppList = PBDevApp.DevAppList.parseFrom(val);
                return devAppList;
            }
        }, callback);
    }

    public static void startAppSetList(BleManager bleManager, PBDevApp.DevAppList devAppList, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startAppSetList " + devAppList);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_APP_SET_LIST, devAppList.toByteArray(), RC4, 5 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void setPersonalGender(BleManager bleManager, PBProperty.PersonalGenderPropVal propVal, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setPersonalGender " + propVal);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.PERSONAL_GENDER);
        builder.setPropVal(propVal.toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void setPersonalBirth(BleManager bleManager, PBProperty.PersonalBirthPropVal propVal, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setPersonalBirth " + propVal);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.PERSONAL_BIRTH);
        builder.setPropVal(propVal.toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void setPersonalHeight(BleManager bleManager, PBProperty.PersonalHeightPropVal propVal, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setPersonalHeight " + propVal);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.PERSONAL_HEIGHT);
        builder.setPropVal(propVal.toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void setPersonalWeight(BleManager bleManager, PBProperty.PersonalWeightPropVal propVal, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.setPersonalWeight " + propVal);

        PBProperty.PropSetParam.Builder builder = PBProperty.PropSetParam.newBuilder();
        builder.setPropId(PBProperty.PROP_ID.PERSONAL_WEIGHT);
        builder.setPropVal(propVal.toByteString());

        PbApi.sendReq(bleManager, PBRbp.CMD.PROP_SET, builder.build().toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startLogStart(BleManager bleManager, AsyncCallback<PBDevLog.DevLogStartResult, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startLogStart");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_LOG_START, null, RC4, 5 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBDevLog.DevLogStartResult>() {
            @Override
            public PBDevLog.DevLogStartResult parse(ByteString val) throws Exception {
                PBDevLog.DevLogStartResult startParam = PBDevLog.DevLogStartResult.parseFrom(val);
                return startParam;
            }
        }, callback);
    }

    public static void startMiSceneGetList(BleManager bleManager, AsyncCallback<PBMiScene.MiSceneList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startMiSceneGetList");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_MI_SCENE_GET_LIST, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, new PbApiParser<PBMiScene.MiSceneList>() {
            @Override
            public PBMiScene.MiSceneList parse(ByteString val) throws Exception {
                PBMiScene.MiSceneList miSceneList = PBMiScene.MiSceneList.parseFrom(val);
                return miSceneList;
            }
        }, callback);
    }

    public static void startMiSceneAdd(BleManager bleManager, PBMiScene.MiSceneInfo miSceneInfo, AsyncCallback<PBMiScene.MiSceneList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startMiSceneAdd " + miSceneInfo);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_MI_SCENE_ADD, miSceneInfo.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startMiSceneAddBatch(BleManager bleManager, PBMiScene.MiSceneList miSceneList, AsyncCallback<PBMiScene.MiSceneList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startMiSceneAddBatch " + miSceneList);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_MI_SCENE_ADD_BATCH, miSceneList.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startMiSceneModify(BleManager bleManager, PBMiScene.MiSceneInfo miSceneInfo, AsyncCallback<PBMiScene.MiSceneList, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startMiSceneModify " + miSceneInfo);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_MI_SCENE_MODIFY, miSceneInfo.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startMiSceneDelete(BleManager bleManager, PBMiScene.MiSceneDeleteParam deleteParam, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startMiSceneDelete " + deleteParam);

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_MI_SCENE_DELETE, deleteParam.toByteArray(), RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    public static void startMiSceneDeleteAll(BleManager bleManager, AsyncCallback<Void, Error> callback) {
        Logger.i(TAG_BLE, "BleApi.startMiSceneDeleteAll");

        PbApi.sendReq(bleManager, PBRbp.CMD.DEV_MI_SCENE_DELETE_ALL, null, RC4, 2 * DateUtils.SECOND_IN_MILLIS, null, callback);
    }

    @Deprecated
    public static byte[] sendApduTest(final BleManager bleManager, final byte[] apdu) {
        Logger.d(TAG_BLE, "BleApi.sendApduTest " + ByteUtil.byteToString(apdu));

        final ByteStringWrapper byteStringWrapper = new ByteStringWrapper();

        final Object syncLock = new Object();

        ApduWorkerThread.get().post(new Runnable() {
            @Override
            public void run() {
                AsyncCallback<ByteString, Error> callback = new AsyncCallback<ByteString, Error>() {
                    @Override
                    public void onSuccess(ByteString result) {

                        byteStringWrapper.byteString = result;

                        synchronized (syncLock) {
                            syncLock.notify();
                        }
                    }

                    @Override
                    public void onFailure(Error error) {

                        byteStringWrapper.byteString = null;

                        synchronized (syncLock) {
                            syncLock.notify();
                        }
                    }
                };

                PbApi.sendReq(bleManager, PBRbp.CMD.TEST_DEV_SEND_APDU, apdu, OPEN, null, new PbApiParser<ByteString>() {
                    @Override
                    public ByteString parse(ByteString val) throws Exception {
                        return val;
                    }
                }, callback);
            }
        });

        synchronized (syncLock) {
            try {
                syncLock.wait();
            } catch (InterruptedException e) {
            }
        }

        byte[] finalResult;

        ByteString apduResultByteStr = byteStringWrapper.byteString;
        if (apduResultByteStr == null) {
            finalResult = null;
        } else {
            byte[] apduResultBytes = apduResultByteStr.toByteArray();
            finalResult = apduResultBytes;
        }


        return finalResult;
    }
}
