package com.ryeex.groot.lib.ble.scan;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chenhao on 2017/11/10.
 */

public class ScannedDevice implements Parcelable {


    public static final Creator<ScannedDevice> CREATOR = new Creator<ScannedDevice>() {
        @Override
        public ScannedDevice createFromParcel(Parcel source) {
            return new ScannedDevice(source);
        }

        @Override
        public ScannedDevice[] newArray(int size) {
            return new ScannedDevice[size];
        }
    };
    private String mName;
    private String mMac;
    private int mRssi;
    private int mProductId;

    public ScannedDevice() {
    }

    protected ScannedDevice(Parcel in) {
        this.mName = in.readString();
        this.mMac = in.readString();
        this.mRssi = in.readInt();
        this.mProductId = in.readInt();
    }

    public synchronized String getName() {
        return mName;
    }

    public synchronized void setName(String name) {
        mName = name;
    }

    public synchronized String getMac() {
        return mMac;
    }

    public synchronized void setMac(String mac) {
        mMac = mac;
    }

    public synchronized int getRssi() {
        return mRssi;
    }

    public synchronized void setRssi(int rssi) {
        mRssi = rssi;
    }

    public synchronized int getProductId() {
        return mProductId;
    }

    public synchronized void setProductId(int productId) {
        mProductId = productId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mName);
        dest.writeString(this.mMac);
        dest.writeInt(this.mRssi);
        dest.writeInt(this.mProductId);
    }
}
