package com.reconinstruments.os.metrics;

import android.os.Parcel;
import android.os.Parcelable;

public class BaseValue implements Parcelable {
    public float Value = Float.NaN;
    public long ChangeTime = 0;

    public BaseValue() { }

    public BaseValue(float value, long changeTime) {
        Value = value;
        ChangeTime = changeTime;
    }

    public void reset() {
        Value = Float.NaN;
        ChangeTime = 0;
    }

    /** @hide */
    public BaseValue(Parcel in) {
        readFromParcel(in);
    }

    /** @hide */
    public void readFromParcel(Parcel in) {
        Value = in.readFloat();
        ChangeTime = in.readLong();		
    }

    /** @hide */
    @Override public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(Value);
        dest.writeLong(ChangeTime);
    }

    /** @hide */
    public void setInvalidValue(){
        reset();
    }

    /**
     * @return true if float is not NaN
     */
    public boolean isValidFloat() {
        return !Float.isNaN(Value);
    }

    public void set(BaseValue baseValue) {
        set(baseValue.Value, baseValue.ChangeTime);
    }

    public void set(float value, long changeTime) {
        this.Value = value;
        this.ChangeTime = changeTime;	
    }

    /** @hide */
    public static final Parcelable.Creator<BaseValue> CREATOR = new Parcelable.Creator<BaseValue>() {
        /** @hide */
        @Override public BaseValue createFromParcel(Parcel source) {
            return new BaseValue(source);
        }

        /** @hide */
        @Override public BaseValue[] newArray(int size) {
            return new BaseValue[size];
        }
    };	
}
