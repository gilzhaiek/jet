//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.SharedPreferences;
import android.os.Bundle;

public class Stat<T extends Number> {
    protected T mInvalidValue;
    protected T mValue = null;
    protected T mResetValue; 
    protected long timestamp = 0;
    protected String mTag;
    public String getTag() {return mTag;}
    public Stat() {
    }
    public Stat(T value, String tag, T invalidValue, T resetValue) {
        mValue = value;
        mTag = tag;
        mInvalidValue = invalidValue;
        mResetValue = resetValue;
    }
    public Stat(T value, String tag, T invalidValue) {
        this(value,tag,invalidValue,value);
    }
    public Stat(T value, String tag) {
        this(value,tag,value,value);
    }
    void setValue(T value) {
        mValue = value;
        // Set time stamp;
    }
    T getValue() {return mValue;}
    T getInvalidValue() {return mInvalidValue;}
    boolean isValid() {
        return !mValue.equals(mInvalidValue);
    }
    boolean isValid(T value) {
        return !value.equals(mInvalidValue);
    }
    public void reset() {
        setValue(mResetValue);
    }
    public void saveState(SharedPreferences.Editor editor) {
        if (mResetValue instanceof Integer) {
            editor.putInt(mTag,mValue.intValue());
        } else if (mResetValue instanceof Float) {
            editor.putFloat(mTag,mValue.floatValue());
        } else if (mResetValue instanceof Long) {
            editor.putLong(mTag,mValue.longValue());
        }
    }
    public void loadState(SharedPreferences sp) {
        if (mResetValue instanceof Integer) {
            mValue = (T)(new Integer(sp.getInt(mTag,mValue.intValue())));
        } else if (mResetValue instanceof Float) {
            mValue = (T)(new Float(sp.getFloat(mTag,mValue.floatValue())));
        } else if (mResetValue instanceof Long) {
            mValue = (T)(new Long(sp.getLong(mTag,mValue.longValue())));
        }
    }
    public Bundle putToBundle(Bundle b) {
        if (mResetValue instanceof Integer) {
            b.putInt(mTag,mValue.intValue());
        } else if (mResetValue instanceof Float) {
            b.putFloat(mTag,mValue.floatValue());
        } else if (mResetValue instanceof Long) {
            b.putLong(mTag,mValue.longValue());
        }
        return b;
    }
    public void update() {return;}
    public void setResetValue(T value) {
        mResetValue = value;
    }
}