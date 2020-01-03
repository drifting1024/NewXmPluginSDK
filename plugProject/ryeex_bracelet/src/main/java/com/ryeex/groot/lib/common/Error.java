package com.ryeex.groot.lib.common;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chenhao on 17/5/23.
 */

public class Error implements Parcelable {
    public static final Creator<Error> CREATOR = new Creator<Error>() {
        @Override
        public Error createFromParcel(Parcel source) {
            return new Error(source);
        }

        @Override
        public Error[] newArray(int size) {
            return new Error[size];
        }
    };
    private int mCode;
    private String mDetail;

    public Error(int code, String detail) {
        mCode = code;
        mDetail = detail;
    }

    protected Error(Parcel in) {
        this.mCode = in.readInt();
        this.mDetail = in.readString();
    }

    final public int getCode() {
        return mCode;
    }

    final public String getDetail() {
        return mDetail;
    }

    public String toString() {
        return mCode + "(" + mDetail + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mCode);
        dest.writeString(this.mDetail);
    }
}
