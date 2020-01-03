package com.ryeex.groot.lib.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ryeex.groot.lib.ble.requestresult.DescriptorWriteResult;
import com.ryeex.groot.lib.ble.requestresult.ReadDescriptorResult;
import com.ryeex.groot.lib.ble.requestresult.ReadResult;
import com.ryeex.groot.lib.ble.requestresult.ReadRssiResult;
import com.ryeex.groot.lib.ble.requestresult.WriteResult;
import com.ryeex.groot.lib.ble.stack.crc.CRC;
import com.ryeex.groot.lib.ble.stack.crypto.Crypto;
import com.ryeex.groot.lib.ble.stack.splitpackage.AckPackage;
import com.ryeex.groot.lib.ble.stack.splitpackage.CmdPackage;
import com.ryeex.groot.lib.ble.stack.splitpackage.DataPackage;
import com.ryeex.groot.lib.ble.stack.splitpackage.SplitPackage;
import com.ryeex.groot.lib.common.Error;
import com.ryeex.groot.lib.common.asynccallback.AsyncCallback;
import com.ryeex.groot.lib.common.thread.MessageHandlerThread;
import com.ryeex.groot.lib.common.util.ByteUtil;
import com.ryeex.groot.lib.log.Logger;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CONNECTED;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CONNECTING;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_CRYPTO_NOT_SUPPORT;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_DISCONNECTED;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_NOT_CONNECTED;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_PARAM_WRONG;
import static com.ryeex.groot.lib.ble.BleErrorCode.BLE_TOKEN_IS_NULL;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_JSON;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_OPEN;
import static com.ryeex.groot.lib.ble.BleSetting.CHARACTER_RYEEX_RC4;
import static com.ryeex.groot.lib.ble.BleSetting.SERVICE_RYEEX;
import static com.ryeex.groot.lib.ble.BleSetting.TAG_BLE;
import static com.ryeex.groot.lib.ble.BleStatus.CONNECTED;
import static com.ryeex.groot.lib.ble.BleStatus.CONNECTING;
import static com.ryeex.groot.lib.ble.BleStatus.DISCONNECTED;
import static com.ryeex.groot.lib.ble.stack.crypto.Crypto.CRYPTO.JSON;
import static com.ryeex.groot.lib.ble.stack.crypto.Crypto.CRYPTO.OPEN;
import static com.ryeex.groot.lib.ble.stack.crypto.Crypto.CRYPTO.RC4;
import static com.ryeex.groot.lib.common.util.ByteUtil.bytesToInt;

/**
 * 单个设备的蓝牙管理器: 管理设备的蓝牙信息 <br>
 * <br>
 * Created by chenhao on 16/12/30.
 */
public class BleManager implements BleConnector.ConnectorListener {
    public static final String KEY_SPEED = "speed";

    public static final int RECEIVE_DATA_PKG_MAX_RETRY = 5;//接受数据包最大重传次数

    private static int sThreadNum = 0;
    MessageHandlerThread mWorkerThread;
    Handler mWorkerHandler;

    private byte[] mToken;
    private BleStatus mStatus = DISCONNECTED;
    private BleConnector mConnector;
    private List<ManagerListener> mManagerListeners = new CopyOnWriteArrayList<>();
    private Map<UUID, Map<UUID, ReceivingRecord>> mReceivingMap = new ConcurrentHashMap<>();

    public BleManager() {
        mConnector = new BleConnector();
    }

    @Override
    public synchronized void onDisconnected(BleConnector.DisconnectedCause cause) {
        mConnector.removeConnectorListener(this);

        Logger.i(TAG_BLE, "BleManager.setStatus DISCONNECTED (from BleManager.onDisconnected)");
        setStatus(DISCONNECTED);
        stopAllReceivingPackage();
        stopWorker();

        ManagerDisconnectedCause managerCause;
        switch (cause) {
            case UNKNOWN:
                managerCause = ManagerDisconnectedCause.UNKNOWN;
                break;
            case STATUS_EQUAL_8:
                managerCause = ManagerDisconnectedCause.STATUS_EQUAL_8;
                break;
            case PROCESS_TIMEOUT:
                managerCause = ManagerDisconnectedCause.PROCESS_TIMEOUT;
                break;
            default:
                managerCause = ManagerDisconnectedCause.UNKNOWN;
                break;
        }
        for (ManagerListener managerListener : mManagerListeners) {
            managerListener.onDisconnected(managerCause);
        }
    }

    @Override
    public void onCharacterChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic character) {
        byte[] value = character.getValue();

//        Logger.d(TAG_BLE, "BleManager.onCharacterChanged " + ByteUtil.byteToString(value));

        final UUID serviceId = character.getService().getUuid();
        final UUID characterId = character.getUuid();

        // -------------- Miot分包协议 start --------------
        final int MSG_RECEIVE_TIMEOUT = 1234;
        if (isReceivingPackage(serviceId, characterId)) {
            ReceivingRecord record = getReceivingRecord(serviceId, characterId);

            if (record == null) {
                return;
            }

            DataPackage dataPkg = DataPackage.parse(character.getValue());
            if (dataPkg != null) {
                byte[] buffer = new byte[4];
                buffer[0] = dataPkg.sn[0];
                buffer[1] = dataPkg.sn[1];
                buffer[2] = 0x00;
                buffer[3] = 0x00;
                int currentSN = ByteUtil.bytesToInt(buffer, ByteOrder.LITTLE_ENDIAN);

                record.dataMap.put(currentSN, dataPkg);

                if (record.dataMap.size() == record.destPkgNum) {
                    // 合包
                    byte[] mergedBytes = mergePackage(record.dataMap);

                    if (mergedBytes != null) {
                        AckPackage ackSuccessPkg = AckPackage.buildAckSuccess();
                        writeCharacterWithoutRsp(character.getService().getUuid(), character.getUuid(), ackSuccessPkg.toBytes(), null);

                        onReceiveBytes(serviceId, characterId, mergedBytes);
                    } else {
                        AckPackage ackCancelPkg = AckPackage.buildAckCancel();
                        writeCharacterWithoutRsp(character.getService().getUuid(), character.getUuid(), ackCancelPkg.toBytes(), null);
                    }

                    stopReceivingPackage(serviceId, characterId);

                    record.timeoutHandler.removeCallbacksAndMessages(null);
                } else if (record.dataMap.containsKey(currentSN) && currentSN == record.destPkgNum) {
                    //最后一个包已到，但之前包有丢失，立即触发重传
                    record.timeoutHandler.removeCallbacksAndMessages(null);

                    record.timeoutHandler.sendEmptyMessage(MSG_RECEIVE_TIMEOUT);
                }
            }

            return;
        }

        final CmdPackage cmdPackage = CmdPackage.parse(value);
        if (cmdPackage != null && cmdPackage.isCmdRawData()) {

            final byte[] buffer = new byte[4];
            buffer[0] = cmdPackage.dataPkgNum[0];
            buffer[1] = cmdPackage.dataPkgNum[1];
            buffer[2] = 0x00;
            buffer[3] = 0x00;
            final int destPkgNum = bytesToInt(buffer, ByteOrder.LITTLE_ENDIAN);

            if (destPkgNum > 0) {
                mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        AckPackage ackSuccessPkg = AckPackage.buildAckReady();
                        writeCharacterWithoutRsp(character.getService().getUuid(), character.getUuid(), ackSuccessPkg.toBytes(), new AsyncCallback<Void, Error>() {

                            @Override
                            public void onSuccess(Void result) {

                                Handler timeoutHandler = new Handler(Looper.myLooper()) {
                                    @Override
                                    public void dispatchMessage(Message msg) {
                                        switch (msg.what) {
                                            case MSG_RECEIVE_TIMEOUT:
                                                ReceivingRecord record = getReceivingRecord(serviceId, characterId);

                                                if (record == null) {
                                                    doTimeoutStop();
                                                    return;
                                                }

                                                record.timeoutCount++;

                                                if (record.timeoutCount <= RECEIVE_DATA_PKG_MAX_RETRY) {
                                                    List<Integer> lossPkgList = new ArrayList<>();
                                                    for (int i = 1, len = record.destPkgNum; i <= len; i++) {
                                                        if (!record.dataMap.containsKey(i)) {
                                                            lossPkgList.add(i);
                                                        }
                                                    }

                                                    if (lossPkgList.size() > 0) {
                                                        doTimeoutRetry(lossPkgList);
                                                    } else {
                                                        doTimeoutStop();
                                                    }
                                                } else {
                                                    doTimeoutStop();
                                                }
                                                break;
                                        }
                                    }

                                    private void doTimeoutRetry(List<Integer> lossPkgList) {
                                        AckPackage ackLossPkg = AckPackage.buildAckLoss(lossPkgList);

                                        writeCharacterWithoutRsp(serviceId, characterId, ackLossPkg.toBytes(), null);
                                        sendEmptyMessageDelayed(MSG_RECEIVE_TIMEOUT, lossPkgList.size() * SplitPackage.getPackageTimeout());
                                    }

                                    private void doTimeoutStop() {
                                        stopReceivingPackage(serviceId, characterId);
                                        AckPackage ackTimeoutPkg = AckPackage.buildAckTimeout();
                                        writeCharacterWithoutRsp(serviceId, characterId, ackTimeoutPkg.toBytes(), null);
                                    }
                                };

                                startReceivingPackage(serviceId, characterId, destPkgNum, timeoutHandler);

                                timeoutHandler.sendEmptyMessageDelayed(MSG_RECEIVE_TIMEOUT, destPkgNum * SplitPackage.getPackageTimeout());
                            }

                            @Override
                            public void onFailure(Error error) {
                                stopReceivingPackage(serviceId, characterId);
                            }
                        });
                    }
                });
                return;
            }

        }
        // -------------- Miot分包协议 end ----------------

        for (ManagerListener managerListener : mManagerListeners) {
            managerListener.onCharacterChanged(character, character.getValue());
        }
    }

    private void onReceiveBytes(final UUID serviceId, final UUID characterId, final byte[] mergedBytes) {
        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                CRC.verify(mergedBytes, new AsyncCallback<byte[], Error>() {
                    @Override
                    public void onSuccess(byte[] verifiedBytes) {

                        byte[] plainBytes;
                        try {
                            if (serviceId.equals(SERVICE_RYEEX) && characterId.equals(CHARACTER_RYEEX_OPEN)) {
                                plainBytes = verifiedBytes;
                            } else if (serviceId.equals(SERVICE_RYEEX) && characterId.equals(CHARACTER_RYEEX_RC4)) {
                                if (BleSetting.ENABLE_RC4_RECEIVE) {
                                    plainBytes = Crypto.decrypt(getToken(), verifiedBytes);
                                } else {
                                    plainBytes = verifiedBytes;
                                }
                            } else if (serviceId.equals(SERVICE_RYEEX) && characterId.equals(CHARACTER_RYEEX_JSON)) {
                                plainBytes = verifiedBytes;
                            } else {
                                plainBytes = verifiedBytes;
                            }
                        } catch (Exception e) {
                            return;
                        }

                        for (ManagerListener managerListener : mManagerListeners) {
                            managerListener.onReceiveBytes(serviceId, characterId, plainBytes);
                        }

                    }

                    @Override
                    public void onFailure(Error error) {
                        Logger.e(TAG_BLE, "CRC.verify fail " + error);
                    }
                });
            }
        });
    }

    private boolean isReceivingPackage(UUID service, UUID character) {
        Map<UUID, ReceivingRecord> serviceMap = mReceivingMap.get(service);
        if (serviceMap == null) {
            return false;
        } else {
            ReceivingRecord characterMap = serviceMap.get(character);
            if (characterMap == null) {
                return false;
            } else {
                return true;
            }
        }
    }

    private ReceivingRecord getReceivingRecord(UUID service, UUID character) {
        Map<UUID, ReceivingRecord> serviceMap = mReceivingMap.get(service);
        if (serviceMap != null) {
            ReceivingRecord record = serviceMap.get(character);
            return record;
        } else {
            return null;
        }
    }

    private void startReceivingPackage(final UUID serviceId, final UUID characterId, int destPkgNum, Handler timeoutHandler) {
        Map<UUID, ReceivingRecord> serviceMap = mReceivingMap.get(serviceId);
        if (serviceMap == null) {
            serviceMap = new ConcurrentHashMap<>();
            ReceivingRecord record = new ReceivingRecord(destPkgNum, timeoutHandler);
            serviceMap.put(characterId, record);
            mReceivingMap.put(serviceId, serviceMap);
        } else {
            ReceivingRecord record = serviceMap.get(characterId);
            if (record == null) {
                record = new ReceivingRecord(destPkgNum, timeoutHandler);
                serviceMap.put(characterId, record);
            }
        }
    }

    private void stopReceivingPackage(UUID serviceId, UUID characterId) {
        Map<UUID, ReceivingRecord> serviceMap = mReceivingMap.get(serviceId);
        if (serviceMap != null) {
            ReceivingRecord record = serviceMap.get(characterId);
            if (record != null) {
                serviceMap.remove(characterId);

                if (serviceMap.size() <= 0) {
                    serviceMap.remove(characterId);
                }
            }

            if (serviceMap.size() <= 0) {
                mReceivingMap.remove(serviceId);
            }
        }
    }

    private void stopAllReceivingPackage() {
        mReceivingMap.clear();
    }

    /**
     * 合包, 每个包的序列号需要连续，并且总数等于dataPkgMap.size
     *
     * @param dataPkgMap
     * @return
     */
    private byte[] mergePackage(Map<Integer, DataPackage> dataPkgMap) {
        if (dataPkgMap == null || dataPkgMap.size() <= 0) {
            return null;
        }

        DataPackage[] pkgArray = new DataPackage[dataPkgMap.size()];

        Iterator<Map.Entry<Integer, DataPackage>> it = dataPkgMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, DataPackage> entry = it.next();
            pkgArray[entry.getKey() - 1] = entry.getValue();
        }

        int totalBytes = 0;

        for (int i = 0, len = pkgArray.length; i < len; i++) {
            if (pkgArray[i] == null || pkgArray[i].payload == null) {
                return null;
            }
            totalBytes += pkgArray[i].payload.length;
        }

        byte[] result = new byte[totalBytes];
        int index = 0;
        for (int i = 0, len = pkgArray.length; i < len; i++) {
            ByteUtil.copy(result, pkgArray[i].payload, index, 0);
            index += pkgArray[i].payload.length;
        }

        return result;
    }

    public synchronized byte[] getToken() {
        return mToken;
    }

    public synchronized void setToken(byte[] token) {
        mToken = token;
    }

    public synchronized void setMac(String mac) {
        mConnector.setMac(mac);
    }

    private synchronized void setStatus(BleStatus status) {
        mStatus = status;
    }

    public synchronized void connect(final AsyncCallback<Void, Error> callback) {
        if (mStatus == CONNECTED) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CONNECTED, "already connected"));
            }
            return;
        }

        if (mStatus == CONNECTING) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CONNECTING, "connecting"));
            }
            return;
        }

        Logger.i(TAG_BLE, "BleManager.setStatus CONNECTING (from BleManager.connect)");
        setStatus(CONNECTING);

        mConnector.connect(new AsyncCallback<Void, Error>() {
            @Override
            public void onSuccess(Void result) {
                startWorker();
                Logger.i(TAG_BLE, "BleManager.setStatus CONNECTED (from BleManager.connect)");
                setStatus(CONNECTED);

                mConnector.addConnectorListener(BleManager.this);

                if (callback != null) {
                    callback.sendSuccessMessage(null);
                }
            }

            @Override
            public void onFailure(Error error) {
                Logger.i(TAG_BLE, "BleManager.setStatus DISCONNECTED (from BleManager.connect)");
                setStatus(DISCONNECTED);
                stopAllReceivingPackage();
                stopWorker();

                if (callback != null) {
                    callback.sendFailureMessage(error);
                }
            }
        });
    }

    private synchronized void startWorker() {
        mWorkerThread = new MessageHandlerThread("BleManager-" + (sThreadNum++));
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
    }

    private synchronized void stopWorker() {
        if (mWorkerHandler != null) {
            mWorkerHandler.removeCallbacksAndMessages(null);
        }

        if (mWorkerThread != null) {
            mWorkerThread.quitSafely();
            mWorkerThread = null;
            mWorkerHandler = null;
        }
    }

    public synchronized void disconnect(AsyncCallback<Void, Error> callback) {
        if (mStatus == DISCONNECTED) {
            Logger.i(TAG_BLE, "BleManager.disconnect, but disconnected already");
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "disconnected already"));
            }
            return;
        }

        Logger.i(TAG_BLE, "BleManager.disconnect status:" + mStatus);

        mConnector.removeConnectorListener(this);

        mConnector.doDisconnectCallback(new Error(BLE_DISCONNECTED, "manual disconnected"));

        // 手动断开连接
        mConnector.disconnect();

        Logger.i(TAG_BLE, "BleManager.setStatus DISCONNECTED (from BleManager.disconnect)");
        setStatus(DISCONNECTED);
        stopAllReceivingPackage();
        stopWorker();

        if (callback != null) {
            callback.sendSuccessMessage(null);
        }
    }

    public synchronized boolean isConnected() {
        return mStatus == CONNECTED;
    }

    public synchronized boolean isConnecting() {
        return mStatus == CONNECTING;
    }

    public synchronized boolean isDisconnected() {
        return mStatus == DISCONNECTED;
    }

    public void readCharacter(UUID service, UUID character, AsyncCallback<ReadResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.readCharacter(service, character, callback);
    }

    public void writeCharacter(UUID service, UUID character, byte[] bytes, AsyncCallback<WriteResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.writeCharacter(service, character, bytes, callback);
    }

    public void writeCharacterWithoutRsp(UUID service, UUID character, byte[] bytes, AsyncCallback<Void, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.writeCharacterWithoutRsp(service, character, bytes, callback);
    }

    public void readDescriptor(UUID service, UUID character, UUID descriptor, AsyncCallback<ReadDescriptorResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.readDescriptor(service, character, descriptor, callback);
    }

    public void writeDescriptor(UUID service, UUID character, UUID descriptor, byte[] value, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.writeDescriptor(service, character, descriptor, value, callback);
    }

    public void notify(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.notify(service, character, callback);
    }

    public void unnotify(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.unnotify(service, character, callback);
    }

    public void indication(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.indication(service, character, callback);
    }

    public void unindication(UUID service, UUID character, AsyncCallback<DescriptorWriteResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.unindication(service, character, callback);
    }

    public void readRssi(AsyncCallback<ReadRssiResult, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_DISCONNECTED, "not connected"));
            }
            return;
        }

        mConnector.readRssi(callback);
    }

    /**
     * 加密, CRC, 底层分包
     *
     * @param bytes
     * @param crypto
     * @param callback
     */
    public void sendBytes(final byte[] bytes, final Crypto.CRYPTO crypto, final AsyncCallback<Float, Error> callback) {
        if (!isConnected()) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_NOT_CONNECTED, "not connected"));
            }
            return;
        }

        if (bytes == null) {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_PARAM_WRONG, "wrong param"));
            }
            return;
        }

        final int bytesLength = bytes.length;
        final long startTime = System.currentTimeMillis();

        final UUID characterId;

        if (crypto == RC4) {
            if (ByteUtil.isEmpty(getToken())) {
                if (callback != null) {
                    callback.sendFailureMessage(new Error(BLE_TOKEN_IS_NULL, "token is null"));
                }
                return;
            }

            characterId = CHARACTER_RYEEX_RC4;
        } else if (crypto == OPEN) {
            characterId = CHARACTER_RYEEX_OPEN;
        } else if (crypto == JSON) {
            characterId = CHARACTER_RYEEX_JSON;
        } else {
            if (callback != null) {
                callback.sendFailureMessage(new Error(BLE_CRYPTO_NOT_SUPPORT, "crypto not support"));
            }
            return;
        }

        Crypto.encrypt(crypto, bytes, getToken(), new AsyncCallback<byte[], Error>() {
            @Override
            public void onSuccess(byte[] encryptedBytes) {
                CRC.calculate(encryptedBytes, new AsyncCallback<byte[], Error>() {
                    @Override
                    public void onSuccess(byte[] finalBytes) {
                        SplitPackage.send(BleManager.this, SERVICE_RYEEX, characterId, finalBytes, new AsyncCallback<Void, Error>() {
                            @Override
                            public void onSuccess(Void result) {
                                final long endTime = System.currentTimeMillis();

                                float speed = bytesLength / ((endTime - startTime) / (float) 1000);

                                if (callback != null) {
                                    callback.sendSuccessMessage(speed);
                                }
                            }

                            @Override
                            public void onFailure(Error error) {
                                if (callback != null) {
                                    callback.sendFailureMessage(error);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(Error error) {
                        if (callback != null) {
                            callback.sendFailureMessage(error);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Error error) {
                if (callback != null) {
                    callback.sendFailureMessage(error);
                }
            }
        });
    }

    public synchronized boolean addManagerListener(ManagerListener managerListener) {
        if (managerListener == null) {
            return false;
        }
        mManagerListeners.add(managerListener);
        return true;
    }

    public synchronized void removeManagerListener(ManagerListener managerListener) {
        if (mManagerListeners.contains(managerListener)) {
            mManagerListeners.remove(managerListener);
        }
    }

    public enum ManagerDisconnectedCause {
        UNKNOWN, STATUS_EQUAL_8, PROCESS_TIMEOUT
    }

    public static abstract class ManagerListener {
        public void onDisconnected(ManagerDisconnectedCause cause) {
        }

        public void onCharacterChanged(BluetoothGattCharacteristic character, byte[] value) {
        }

        public void onReceiveBytes(UUID serviceId, UUID characterId, byte[] plainBytes) {
        }
    }

    private class ReceivingRecord {
        int destPkgNum;
        Map<Integer, DataPackage> dataMap;
        Handler timeoutHandler;
        int timeoutCount;

        ReceivingRecord(int destPkgNum, Handler timeoutHandler) {
            this.destPkgNum = destPkgNum;
            this.dataMap = new ConcurrentHashMap<>();
            this.timeoutHandler = timeoutHandler;
            this.timeoutCount = 0;
        }
    }
}
