package com.ryeex.sake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.ryeex.groot.lib.ble.BleContext;
import com.ryeex.groot.lib.ble.BleManager;
import com.ryeex.groot.lib.ble.requestresult.DescriptorWriteResult;
import com.ryeex.groot.lib.ble.scan.BleScanner;
import com.ryeex.groot.lib.ble.stack.app.BleApi;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBDevice;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBMiScene;
import com.ryeex.groot.lib.ble.stack.pb.entity.PBProperty;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.crypto.Base64Coder;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.log.Logger;
import com.ryeex.sake.ota.OtaManager;
import com.ryeex.sake.ui.dialog.ProgressDialogFragment;
import com.xiaomi.smarthome.bluetooth.BleUpgrader;
import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.device.api.BaseDevice;
import com.xiaomi.smarthome.device.api.BaseDevice.StateChangedListener;
import com.xiaomi.smarthome.device.api.BtFirmwareUpdateInfo;
import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.IXmPluginHostActivity;
import com.xiaomi.smarthome.device.api.SceneInfo;
import com.xiaomi.smarthome.device.api.XmPluginBaseActivity;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_OPEN;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_RC4;
import static com.ryeex.groot.lib.ble.BleSetting.SERVICE_RYEEX;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.DOWNLOADING;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.DOWNLOAD_FAILURE;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.PENDING;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.TRANSFERRING;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.TRANSFER_FAILURE;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.UPDATE_SUCCESS;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.WAIT_REBOOTING;
import static com.ryeex.sake.ota.OtaManager.OtaStatus.WAIT_REBOOTING_FAILURE;

public class MainActivity extends XmPluginBaseActivity implements StateChangedListener {
    public static final String TAG = "groot-ble";
    Device mDevice;

    View mNewFirmView;

    TextView mTitleView;
    TextView mCurrentStepTextView;
    TextView mTargetStepTextView;
    TextView mLastHeartRateTextView;
    TextView mLastHeartRateTimeTextView;

    boolean mIsChannelReady = false;

    int mCurrentStep = -1;
    int mTargetStep = -1;
    int mLastHeartRate = -1;
    long mLastHeartRateTime = -1;
    BtFirmwareUpdateInfo mBtUpdateInfo;
    String mCurrentFirmwareVersion;

    BleManager mBleManager = new BleManager();

    OtaManager.OtaStatus mOtaStatus = PENDING;
    float mOtaDownloadProgress;
    float mOtaTransferProgress;
    float mOtaWaitRebootProgress;
    ProgressDialogFragment mOtaDialog;

    MyUpgrader mMyUpgrader = new MyUpgrader();

    BroadcastReceiver mSceneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.i(TAG, "SceneReceiver.onReceive");

            startSyncScene();
        }
    };

    BroadcastReceiver mOtaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isCurrentDevice(intent)) {
                return;
            }

            String action = intent.getAction();

            if (action.equals(OtaManager.ACTION_DOWNLOAD_START)) {
                mOtaStatus = DOWNLOADING;
                refreshUI();
            } else if (action.equals(OtaManager.ACTION_DOWNLOAD_PROGRESS)) {
                float progress = intent.getFloatExtra(OtaManager.KEY_PROGRESS, 0);
                mOtaDownloadProgress = progress;
                mOtaStatus = DOWNLOADING;
                refreshUI();

                mMyUpgrader.showProgress((int) (progress * 0.1));
            } else if (action.equals(OtaManager.ACTION_DOWNLOAD_FAILURE)) {
                Error error = intent.getParcelableExtra(OtaManager.KEY_ERROR);
                mOtaStatus = DOWNLOAD_FAILURE;
                refreshUI();

                mMyUpgrader.showFailure();

            } else if (action.equals(OtaManager.ACTION_TRANSFER_START)) {
                mOtaStatus = TRANSFERRING;
                refreshUI();
            } else if (action.equals(OtaManager.ACTION_TRANSFER_PROGRESS)) {
                float progress = intent.getFloatExtra(OtaManager.KEY_PROGRESS, 0);
                mOtaTransferProgress = progress * 100;
                mOtaStatus = TRANSFERRING;
                refreshUI();

                mMyUpgrader.showProgress((int) (progress * 70) + 10);

            } else if (action.equals(OtaManager.ACTION_TRANSFER_FAILURE)) {
                Error error = intent.getParcelableExtra(OtaManager.KEY_ERROR);
                mOtaStatus = TRANSFER_FAILURE;
                refreshUI();

                mMyUpgrader.showFailure();

            } else if (action.equals(OtaManager.ACTION_WAIT_REBOOT_START)) {
                mOtaStatus = WAIT_REBOOTING;
                refreshUI();
            } else if (action.equals(OtaManager.ACTION_WAIT_REBOOT_PROGRESS)) {
                float progress = intent.getFloatExtra(OtaManager.KEY_PROGRESS, 0);
                mOtaWaitRebootProgress = progress * 100;
                mOtaStatus = WAIT_REBOOTING;
                refreshUI();

                mMyUpgrader.showProgress((int) (progress * 20) + 80);

            } else if (action.equals(OtaManager.ACTION_WAIT_REBOOT_FAILURE)) {
                Error error = intent.getParcelableExtra(OtaManager.KEY_ERROR);
                mOtaStatus = WAIT_REBOOTING_FAILURE;
                refreshUI();

                mMyUpgrader.showFailure();

            } else if (action.equals(OtaManager.ACTION_FINAL_SUCCESS)) {
                mOtaStatus = UPDATE_SUCCESS;
                Toast.makeText(activity(), getString(R.string.ota_success), Toast.LENGTH_SHORT).show();
                refreshUI();

                mMyUpgrader.showSuccess();
            }
        }

        private boolean isCurrentDevice(Intent intent) {
            if (mDevice == null) {
                return false;
            }

            String did = intent.getStringExtra(OtaManager.KEY_DID);

            if (!TextUtils.isEmpty(did) && did.equals(mDevice.getDid())) {
                return true;
            } else {
                return false;
            }
        }
    };

    private void initBle() {
        BleContext.sAppContext = activity().getApplicationContext();
        BleScanner.getInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initBle();

        setContentView(R.layout.activity_main);

        mCurrentStepTextView = (TextView) findViewById(R.id.current_step);
        mTargetStepTextView = (TextView) findViewById(R.id.target_step);
        mLastHeartRateTextView = (TextView) findViewById(R.id.last_heart_rate);
        mLastHeartRateTimeTextView = (TextView) findViewById(R.id.last_heart_rate_time);

        mNewFirmView = findViewById(R.id.title_bar_redpoint);
        mTitleView = ((TextView) findViewById(R.id.title_bar_title));

        // 初始化device
        mDevice = Device.getDevice(mDeviceStat);

        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));

        findViewById(R.id.title_bar_return).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.title_bar_more).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ArrayList<IXmPluginHostActivity.MenuItemBase> menus = new ArrayList<>();

                // 配置自定义蓝牙固件升级
                menus.add(IXmPluginHostActivity.BleMenuItem.newUpgraderItem(mMyUpgrader));

                // 配置已有一级菜单item是否显示，更多配置参数可参考接口描述
                Intent params = new Intent();
                params.putExtra("help_feedback_enable", true);

                // 配置已有二级通用菜单item是否显示，更多配置参数可参考接口描述
                Intent commonSettingParams = new Intent();
                commonSettingParams.putExtra("unbind_enable", true);
                commonSettingParams.putExtra("timezone_enable", false);

                mHostActivity.openMoreMenu2(menus, true, 12345, params, commonSettingParams);

//                mHostActivity.openMoreMenu(null, true, -1);
//                List<IXmPluginHostActivity.MenuItemBase> menus = new ArrayList<>();
//                menus.add(IXmPluginHostActivity.BleMenuItem.newUpgraderItem(mMyUpgrader));
//                hostActivity().openMoreMenu((ArrayList<IXmPluginHostActivity.MenuItemBase>) menus, true, 0);
            }
        });

        findViewById(R.id.add_scene_layer).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hostActivity().startCreateSceneByDid(mDevice.getDid());

//                hostActivity().openSceneActivity(mDevice.getDid());
//                hostActivity().startEditRecommendScenes();
//                hostActivity().getDeviceRecommendScenes(mDevice.getDid(), new IXmPluginHostActivity.AsyncCallback<List<RecommendSceneItem>>() {
//                    @Override
//                    public void onSuccess(List<RecommendSceneItem> recommendSceneItems) {
//                        Log.d(TAG, "getDeviceRecommendScenes onSuccess " + recommendSceneItems.size());
//                    }
//
//                    @Override
//                    public void onFailure(int i, Object o) {
//                        Log.d(TAG, "getDeviceRecommendScenes onFailure " + i + " " + o);
//                    }
//                });
            }
        });

        findViewById(R.id.edit_scene_layer).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hostActivity().openSceneActivity(mDevice.getDid());
            }
        });

        // 打开分享
        View shareView = findViewById(R.id.title_bar_share);
        if (mDevice.isOwner()) {
            shareView.setVisibility(View.VISIBLE);
            shareView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHostActivity.openShareActivity();
                }
            });
        } else {
            shareView.setVisibility(View.GONE);
        }


        Logger.i(TAG, "did:" + mDevice.getDid() + " mac:" + mDevice.getMac());

        int connectStatus = XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac());

        Logger.i(TAG, "connectStatus:" + connectStatus);

        Logger.i(TAG, "XmBluetoothManager.secureConnect");
        XmBluetoothManager.getInstance().secureConnect(mDevice.getMac(), new Response.BleConnectResponse() {
            @Override
            public void onResponse(int code, Bundle data) {
                Logger.i(TAG, "XmBluetoothManager.secureConnect onResponse code:" + code);
                if (code == XmBluetoothManager.Code.REQUEST_SUCCESS) {

                    startGetCurrentFirmwareVersion();

                    final byte[] token = data.getByteArray(XmBluetoothManager.EXTRA_TOKEN);
                    String tokenStr = ByteUtil.byteToString(token);

                    Logger.i(TAG, "token:" + tokenStr);

                    mBleManager.setToken(token);
                    mBleManager.setMac(mDevice.getMac());
                    Logger.i(TAG, "mBleManager.connect");

                    mBleManager.connect(new AsyncCallback<Void, Error>() {
                        @Override
                        public void onSuccess(Void result) {
                            Logger.i(TAG, "BleApi.connect onSuccess");
                            mBleManager.notify(SERVICE_RYEEX, CHARACTER_RYEEX_OPEN, new AsyncCallback<DescriptorWriteResult, Error>() {
                                @Override
                                public void onSuccess(DescriptorWriteResult result) {
                                    Logger.i(TAG, "BleApi.notify SERVICE_RYEEX CHARACTER_RYEEX_OPEN onSuccess");
                                    mBleManager.notify(SERVICE_RYEEX, CHARACTER_RYEEX_RC4, new AsyncCallback<DescriptorWriteResult, Error>() {
                                        @Override
                                        public void onSuccess(DescriptorWriteResult result) {
                                            Logger.i(TAG, "BleApi.notify SERVICE_RYEEX CHARACTER_RYEEX_RC4 onSuccess");

                                            mIsChannelReady = true;
                                            refreshUI();

                                            startBind(token, mDevice);

                                            startGetCurrentStep();

                                            startGetTargetStep();

                                            startGetHealthLastHeartRate();

                                            startCheckFirmwareUpdate();

                                            startSyncScene();
                                        }

                                        @Override
                                        public void onFailure(Error error) {
                                            Logger.e(TAG, "BleApi.notify SERVICE_RYEEX CHARACTER_RYEEX_RC4 onFailure " + error);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Error error) {
                                    Logger.e(TAG, "BleApi.notify SERVICE_RYEEX CHARACTER_RYEEX_OPEN onFailure " + error);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Error error) {
                            Logger.e(TAG, "BleApi.connect onFailure " + error);
                        }
                    });
                } else {

                }
            }
        });

        IntentFilter otaFilter = new IntentFilter();
        otaFilter.addAction(OtaManager.ACTION_DOWNLOAD_START);
        otaFilter.addAction(OtaManager.ACTION_DOWNLOAD_PROGRESS);
        otaFilter.addAction(OtaManager.ACTION_DOWNLOAD_FAILURE);
        otaFilter.addAction(OtaManager.ACTION_TRANSFER_START);
        otaFilter.addAction(OtaManager.ACTION_TRANSFER_PROGRESS);
        otaFilter.addAction(OtaManager.ACTION_TRANSFER_FAILURE);
        otaFilter.addAction(OtaManager.ACTION_WAIT_REBOOT_START);
        otaFilter.addAction(OtaManager.ACTION_WAIT_REBOOT_PROGRESS);
        otaFilter.addAction(OtaManager.ACTION_WAIT_REBOOT_FAILURE);
        otaFilter.addAction(OtaManager.ACTION_FINAL_SUCCESS);
        LocalBroadcastManager.getInstance(activity()).registerReceiver(mOtaReceiver, otaFilter);

        activity().registerReceiver(mSceneReceiver, new IntentFilter("scene_status_update"));

        refreshUI();
    }

    private void startBind(final byte[] token, final Device device) {
        BleApi.getDeviceInfo(mBleManager, new AsyncCallback<PBDevice.DeviceInfo, Error>() {
            @Override
            public void onSuccess(final PBDevice.DeviceInfo deviceInfo) {
                BleApi.getDevCredential(mBleManager, new AsyncCallback<PBDevice.DeviceCredential, Error>() {
                    @Override
                    public void onSuccess(PBDevice.DeviceCredential credential) {
                        Logger.i(TAG, "BleApi.getDevCredential onSuccess \n" + credential);

                        String[] dids = new String[1];
                        dids[0] = mDevice.getDid();
                        JSONObject payloadJsonObj = new JSONObject();
                        try {
                            payloadJsonObj.put("url", "/user/bind_device");
                            JSONObject paramJsonObj = new JSONObject();
                            paramJsonObj.put("did", deviceInfo.getDid());
                            paramJsonObj.put("mac", device.getMac());
                            paramJsonObj.put("model", deviceInfo.getModel());
                            paramJsonObj.put("sn", credential.getSn());
                            paramJsonObj.put("nonce", credential.getNonce());
                            paramJsonObj.put("sign", Base64Coder.encode(credential.getSign().toByteArray()));
                            paramJsonObj.put("sign_version", credential.getSignVer());
                            paramJsonObj.put("device_token", Base64Coder.encode(token));
                            payloadJsonObj.put("param", paramJsonObj);
                        } catch (JSONException e) {
                        }

                        Logger.i(TAG, "callRemoteAsync " + payloadJsonObj);

                        XmPluginHostApi.instance().callRemoteAsync(dids, 10279, payloadJsonObj, new Callback<JSONObject>() {
                            @Override
                            public void onSuccess(JSONObject jsonObject) {
                                Logger.i(TAG, "callRemoteAsync onSuccess " + jsonObject.toString());
                            }

                            @Override
                            public void onFailure(int i, String s) {
                                Logger.e(TAG, "callRemoteAsync onFailure " + i + " " + s);
                            }
                        });

                    }

                    @Override
                    public void onFailure(Error error) {
                        Logger.e(TAG, "BleApi.getDevCredential onFailure " + error);
                    }
                });
            }

            @Override
            public void onFailure(Error error) {
                Logger.e(TAG, "BleApi.getDeviceInfo onFailure " + error);
            }
        });
    }

    private void startGetCurrentStep() {
        BleApi.getSportPedoCurrentStep(mBleManager, new AsyncCallback<PBProperty.SportPedoCurrentStepsPropVal, Error>() {
            @Override
            public void onSuccess(PBProperty.SportPedoCurrentStepsPropVal propVal) {
                mCurrentStep = propVal.getSteps();

                refreshUI();
            }

            @Override
            public void onFailure(Error error) {
                Logger.e(TAG, "BleApi.getSportPedoCurrentStep onFailure " + error);
            }
        });
    }

    private void startGetTargetStep() {
        BleApi.getHealthTargetStep(mBleManager, new AsyncCallback<PBProperty.HealthTargetStepPropVal, Error>() {
            @Override
            public void onSuccess(PBProperty.HealthTargetStepPropVal propVal) {
                mTargetStep = propVal.getStep();

                refreshUI();
            }

            @Override
            public void onFailure(Error error) {
                Logger.e(TAG, "BleApi.getHealthTargetStep onFailure " + error);
            }
        });
    }

    private void startGetHealthLastHeartRate() {
        BleApi.getHealthLastHeartRate(mBleManager, new AsyncCallback<PBProperty.HealthLastHeartRatePropVal, Error>() {
            @Override
            public void onSuccess(PBProperty.HealthLastHeartRatePropVal propVal) {
                mLastHeartRate = propVal.getRate();
                mLastHeartRateTime = propVal.getTime();
                refreshUI();
            }

            @Override
            public void onFailure(Error error) {
                Logger.e(TAG, "BleApi.startGetHealthLastHeartRate onFailure " + error);
            }
        });
    }

    private void startGetCurrentFirmwareVersion() {
        XmBluetoothManager.getInstance().getBluetoothFirmwareVersion(mDevice.getMac(), new Response.BleReadFirmwareVersionResponse() {
            @Override
            public void onResponse(int code, String version) {
                // version类似1.0.3_2001
                Logger.i(TAG, "getBluetoothFirmwareVersion code:" + code + " version:" + version);
                if (code == 0) {
                    mCurrentFirmwareVersion = version;
                }

                refreshUI();
            }
        });
    }

    private void startCheckFirmwareUpdate() {
        XmPluginHostApi.instance().getBluetoothFirmwareUpdateInfo(mDevice.getModel(), new Callback<BtFirmwareUpdateInfo>() {
            @Override
            public void onSuccess(BtFirmwareUpdateInfo btFirmwareUpdateInfo) {
                mBtUpdateInfo = btFirmwareUpdateInfo;
                refreshUI();

//                ConfirmDialogFragment confirmDialog = new ConfirmDialogFragment();
//                confirmDialog.setTitle(getString(R.string.ota_dialog_title));
//                String content = getString(R.string.ota_changelog) + "\n" + mBtUpdateInfo.changeLog;
//                confirmDialog.setContent(content);
//                confirmDialog.setConfirmListener(getString(R.string.ota_dialog_confirm), new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        startOta(mBleManager);
//                    }
//                });
//                try {
//                    confirmDialog.show(activity().getFragmentManager(), null);
//                } catch (Exception e) {
//
//                }
            }

            @Override
            public void onFailure(int i, String s) {
                Logger.e(TAG, "getBluetoothFirmwareUpdateInfo onFailure " + i + " " + s);
            }
        });
    }

    private void startSyncScene() {
        final List<SceneInfo> sceneInfoList = new ArrayList<>();
        try {
            sceneInfoList.addAll(hostActivity().getSceneByDid(mDevice.getDid()));
        } catch (Exception e) {
        }
        Logger.d(TAG, "SceneInfo count:" + sceneInfoList.size());
        for (SceneInfo sceneInfo : sceneInfoList) {

            if (XmPluginHostApi.instance().getApiLevel() >= 67) {
                Logger.d(TAG, "ApiLevel:" + XmPluginHostApi.instance().getApiLevel() + " SceneId:" + sceneInfo.mSceneIdV2 + " enable:" + sceneInfo.mEnable);
            } else {
                Logger.d(TAG, "ApiLevel:" + XmPluginHostApi.instance().getApiLevel() + " SceneId:" + sceneInfo.mSceneId + " enable:" + sceneInfo.mEnable);
            }
            try {
                for (SceneInfo.SceneLaunch launch : sceneInfo.mLaunchList) {
                    Logger.d(TAG, "LaunchType:" + launch.mLaunchType + " LaunchName:" + launch.mLaunchName + " DeviceModel:" + launch.mDeviceModel + " Did:" + launch.mDid + " EventString:" + launch.mEventString + " EventValue:" + launch.mEventValue);
                }
                for (SceneInfo.SceneAction action : sceneInfo.mActions) {
                    Logger.d(TAG, "ActionType:" + action.mActionType + " ActionName:" + action.mActionName + " ActionValue:" + action.mActionValue + " ActionString:" + action.mActionString + " DeviceModel:" + action.mDeviceModel + " DeviceName:" + action.mDeviceName + " Did:" + action.mDid);
                }
            } catch (Exception e) {
            }
        }

        final String userId = XmPluginHostApi.instance().getAccountId();

        BleApi.startMiSceneDeleteAll(mBleManager, new AsyncCallback<Void, Error>() {
            @Override
            public void onSuccess(Void result) {
                PBMiScene.MiSceneList.Builder sceneListBuilder = PBMiScene.MiSceneList.newBuilder();
                for (SceneInfo sceneInfo : sceneInfoList) {
                    try {
                        PBMiScene.MiSceneInfo.Builder sceneInfoBuilder = PBMiScene.MiSceneInfo.newBuilder();

                        int sceneId = 0;
                        if (XmPluginHostApi.instance().getApiLevel() >= 67) {
                            try {
                                sceneId = Integer.parseInt(sceneInfo.mSceneIdV2);
                            } catch (Exception e) {
                            }
                        } else {
                            sceneId = sceneInfo.mSceneId;
                        }

                        sceneInfoBuilder.setSceneId(sceneId);
                        sceneInfoBuilder.setEnable(sceneInfo.mEnable);
                        sceneInfoBuilder.setMid(userId);
                        sceneInfoBuilder.setName(getRyeexString(sceneInfo.mName));
                        for (SceneInfo.SceneLaunch launch : sceneInfo.mLaunchList) {
                            try {
                                PBMiScene.MiSceneLaunchInfo.Builder launchBuilder = PBMiScene.MiSceneLaunchInfo.newBuilder();
                                launchBuilder.setLaunchType(launch.mLaunchType);
                                launchBuilder.setLaunchName(launch.mLaunchName);
                                launchBuilder.setModel(launch.mDeviceModel);
                                launchBuilder.setDid(launch.mDid);
                                launchBuilder.setEventStr(launch.mEventString);
                                launchBuilder.setEventValue(launch.mEventValue.toString());
                                sceneInfoBuilder.addLaunchs(launchBuilder.build());
                            } catch (Exception e) {

                            }
                        }
                        for (SceneInfo.SceneAction action : sceneInfo.mActions) {
                            try {
                                PBMiScene.MiSceneActionInfo.Builder actionBuilder = PBMiScene.MiSceneActionInfo.newBuilder();
                                actionBuilder.setActionType(action.mActionType);
                                actionBuilder.setActionName(action.mActionName);
                                actionBuilder.setActionStr(getRyeexString(action.mActionString));
                                actionBuilder.setActionValue(getRyeexString(action.mActionValue.toString()));
                                actionBuilder.setModel(action.mDeviceModel);
                                actionBuilder.setDid(action.mDid);
                                actionBuilder.setDeviceName(action.mDeviceName);
                                sceneInfoBuilder.addActions(actionBuilder.build());
                            } catch (Exception e) {

                            }
                        }


                        sceneListBuilder.addMiSceneInfos(sceneInfoBuilder.build());

                    } catch (Exception e) {
                    }
                }

                BleApi.startMiSceneAddBatch(mBleManager, sceneListBuilder.build(), new AsyncCallback<PBMiScene.MiSceneList, Error>() {
                    @Override
                    public void onSuccess(PBMiScene.MiSceneList result) {
                    }

                    @Override
                    public void onFailure(Error error) {
                        Logger.e(TAG, "BleApi.startMiSceneAddBatch fail " + error);
                    }
                });
            }

            @Override
            public void onFailure(Error error) {
                Logger.e(TAG, "BleApi.startMiSceneDeleteAll fail " + error);
            }
        });
    }

    private String getRyeexString(String rawStr) {
        String result = "";
        if (!TextUtils.isEmpty(rawStr)) {
            if (rawStr.length() > 15) {
                result = rawStr.substring(0, 15);
            } else {
                result = rawStr;
            }
        }
        return result;
    }

    private void startOta(BleManager bleManager) {
        if (mOtaStatus == DOWNLOADING || mOtaStatus == TRANSFERRING) {
            return;
        }

        Logger.i(TAG, "startOta");

//        mOtaDialog = new ProgressDialogFragment();
//        mOtaDialog.setTitle(getString(R.string.ota_progress_dialog_title));
//        mOtaDialog.setCancelable(false);
//        mOtaDialog.show(activity().getFragmentManager(), null);

        mOtaDownloadProgress = 0;
        mOtaTransferProgress = 0;
        mOtaWaitRebootProgress = 0;
        refreshUI();

        OtaManager.getInstance().setAppContext(activity().getApplicationContext());
        OtaManager.getInstance().startFirmwareUpdate(mDevice, bleManager, mBtUpdateInfo);
    }

    public void refreshUI() {
        mTitleView.setText(mDevice.getName());

        if (mBtUpdateInfo != null && !TextUtils.isEmpty(mCurrentFirmwareVersion)) {
            if (hasNewVersion(mCurrentFirmwareVersion, mBtUpdateInfo.version)) {
                mNewFirmView.setVisibility(View.VISIBLE);
            } else {
                mNewFirmView.setVisibility(View.GONE);
            }
        } else {
            mNewFirmView.setVisibility(View.GONE);
        }

        if (mCurrentStep < 0) {
            mCurrentStepTextView.setText("-");
        } else {
            mCurrentStepTextView.setText("" + mCurrentStep);
        }

        if (mTargetStep < 0) {
            mTargetStepTextView.setText("-");
        } else {
            mTargetStepTextView.setText("" + mTargetStep);
        }

        if (mLastHeartRate <= 0 || mLastHeartRateTime <= 0) {
            mLastHeartRateTextView.setText("-");
            mLastHeartRateTimeTextView.setText("-");
        } else {
            mLastHeartRateTextView.setText("" + mLastHeartRate);
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
            Date lastDate = new Date();
            lastDate.setTime(mLastHeartRateTime * 1000);
            mLastHeartRateTimeTextView.setText(dateFormat.format(lastDate));
        }

        switch (mOtaStatus) {
            case PENDING:
                if (mOtaDialog != null) {
                    mOtaDialog.setTitle(getString(R.string.ota_progress_dialog_title));
                }
                break;
            case DOWNLOADING:
                if (mOtaDialog != null) {
                    mOtaDialog.setTitle(getString(R.string.ota_progress_dialog_title_downloading));
                    mOtaDialog.setProgress(mOtaDownloadProgress, String.format("%.1f%%", mOtaDownloadProgress));
                }
                break;
            case DOWNLOAD_FAILURE:
                if (mOtaDialog != null) {
                    mOtaDialog.dismissAllowingStateLoss();
                }
                break;
            case TRANSFERRING:
                if (mOtaDialog != null) {
                    mOtaDialog.setTitle(getString(R.string.ota_progress_dialog_title_transferring));
                    mOtaDialog.setProgress(mOtaTransferProgress, String.format("%.1f%%", mOtaTransferProgress));
                }
                break;
            case TRANSFER_FAILURE:
                if (mOtaDialog != null) {
                    mOtaDialog.dismissAllowingStateLoss();
                }
                break;
            case WAIT_REBOOTING:
                if (mOtaDialog != null) {
                    mOtaDialog.setTitle(getString(R.string.ota_progress_dialog_title_wait_rebooting));
                    mOtaDialog.setProgress(mOtaWaitRebootProgress, String.format("%.1f%%", mOtaWaitRebootProgress));
                }
                break;
            case WAIT_REBOOTING_FAILURE:
                if (mOtaDialog != null) {
                    mOtaDialog.dismissAllowingStateLoss();
                }
                break;
            case UPDATE_SUCCESS:
                if (mOtaDialog != null) {
                    mOtaDialog.dismissAllowingStateLoss();
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mDevice.updateDeviceStatus();
        ((TextView) findViewById(R.id.title_bar_title)).setText(mDevice.getName());

        // 监听设备数据变化
        mDevice.addStateChangedListener(this);
        refreshUI();
    }


    @Override
    public void onPause() {
        super.onPause();

        // 取消监听
        mDevice.removeStateChangedListener(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(activity()).unregisterReceiver(mOtaReceiver);

        activity().unregisterReceiver(mSceneReceiver);

        if (mBleManager != null) {
            mBleManager.disconnect(null);
        }

        XmBluetoothManager.getInstance().disconnect(mDevice.getMac());
    }

    @Override
    public void onStateChanged(BaseDevice device) {
        refreshUI();
    }

    private boolean hasNewVersion(String currentFirmwareVersion, String newFirmwareVersion) {
        if (TextUtils.isEmpty(currentFirmwareVersion) || TextUtils.isEmpty(newFirmwareVersion)) {
            return false;
        } else {
            try {
                String[] currentVersion = currentFirmwareVersion.split("\\.");
                String[] lastestVersion = newFirmwareVersion.split("\\.");

                int currentVersion_1 = Integer.parseInt(currentVersion[0]);
                int currentVersion_2 = Integer.parseInt(currentVersion[1]);
                int currentVersion_3 = Integer.parseInt(currentVersion[2]);

                int lastestVersion_1 = Integer.parseInt(lastestVersion[0]);
                int lastestVersion_2 = Integer.parseInt(lastestVersion[1]);
                int lastestVersion_3 = Integer.parseInt(lastestVersion[2]);

                boolean hasNew = false;
                if (lastestVersion_1 > currentVersion_1) {
                    hasNew = true;
                } else if (lastestVersion_1 == currentVersion_1) {
                    if (lastestVersion_2 > currentVersion_2) {
                        hasNew = true;
                    } else if (lastestVersion_2 == currentVersion_2) {
                        if (lastestVersion_3 > currentVersion_3) {
                            hasNew = true;
                        }
                    }
                }

                return hasNew;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public class MyUpgrader extends BleUpgrader {

        @Override
        public String getCurrentVersion() {
            // 返回当前固件版本
            Log.d(TAG, "MyUpgrader.getCurrentVersion");
            if (!TextUtils.isEmpty(mCurrentFirmwareVersion)) {
                return mCurrentFirmwareVersion;
            }
            return "";
        }

        @Override
        public String getLatestVersion() {
            // 返回最新固件版本
            Log.d(TAG, "MyUpgrader.getLatestVersion");

            if (mBtUpdateInfo != null) {
                return mBtUpdateInfo.version;
            }
            return "";
        }

        public void showProgress(int progress) {
            Bundle bundle = new Bundle();
            bundle.putInt(XmBluetoothManager.EXTRA_UPGRADE_PROCESS, progress);
            showPage(XmBluetoothManager.PAGE_UPGRADING, bundle);
        }

        public void showSuccess() {
            showPage(XmBluetoothManager.PAGE_UPGRADE_SUCCESS, null);
        }

        public void showFailure() {
            showPage(XmBluetoothManager.PAGE_UPGRADE_FAILED, null);
        }

        @Override
        public String getUpgradeDescription() {
            // 返回最新固件升级描述
            Log.d(TAG, "MyUpgrader.getUpgradeDescription");
            if (mBtUpdateInfo != null) {
                return mBtUpdateInfo.changeLog;
            }
            return "";
        }

        @Override
        public void startUpgrade() {
            // 开始固件升级时回调
            Log.d(TAG, "MyUpgrader.startUpgrade");
            startOta(mBleManager);

            showPage(XmBluetoothManager.PAGE_UPGRADING, null);
        }

        @Override
        public void onActivityCreated(Bundle bundle) throws RemoteException {
            // 必须得在onActivityCreated调用showPage告诉米家app当前升级状态，不然的话会一直显示Loading页
            // 通知米家app升级页面，当前固件不是最新版本，需要提示用户升级
            Log.d(TAG, "MyUpgrader.onActivityCreated local-version:" + mCurrentFirmwareVersion + " server-version:" + mBtUpdateInfo.version);
            if (TextUtils.isEmpty(mCurrentFirmwareVersion) || mBtUpdateInfo == null || TextUtils.isEmpty(mBtUpdateInfo.version)) {
                showPage(XmBluetoothManager.PAGE_CURRENT_LATEST, null);
            } else {
                if (hasNewVersion(mCurrentFirmwareVersion, mBtUpdateInfo.version)) {
                    showPage(XmBluetoothManager.PAGE_CURRENT_DEPRECATED, null);
                } else {
                    showPage(XmBluetoothManager.PAGE_CURRENT_LATEST, null);
                }
            }
        }
    }
}

