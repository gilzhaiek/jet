package com.reconinstruments.hudserver.metrics;

import android.util.Log;

// Efficient Average Float Class
public class SmartAveragerF {
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = false; // change to true to enable debugging

    private long mPushCount = 0;
    private float[] mData;
    private float mAverage;
    private int mPos = 0;
    private boolean mArrayFilled = false;
    private float mRemovedValue; // Temporary Value for Average

    public SmartAveragerF(int size) {
        mData = new float[size];
        reset();
    }

    public void reset() {
        mData[mPos] = 0;
        mAverage = 0;
        mPushCount = 0;
        mArrayFilled = false;
    }

    public float getAverage() {
        return mAverage;
    }

    public long getPushCount() {
        return mPushCount;
    }

    public long getSize() {
        return mData.length;
    }

    public boolean isArrayFilled() {
        return mArrayFilled;
    }

    public void push(float value, int repeat) {
        synchronized (mData) {
            debugMsg("Push "+ value + "*"+repeat);
            if(repeat > mData.length) repeat = mData.length;

            for(int i = 0; i < repeat; i++) {
                push(value);
            }
        }
    }

    public void push(float value) {
        synchronized (mData) {
            debugMsg("Push1 "+ value);
            mRemovedValue = mData[mPos]; 
            mData[mPos++] = value;
            if(mPos >= mData.length) {
                mPos = 0;
            }
            mPushCount++;

            // Updating Average
            if(!mArrayFilled) {
                mAverage = (mAverage*(mPushCount-1)/mPushCount) + (value/mPushCount);
            } else {
                mAverage -= mRemovedValue/mData.length;
                mAverage += value/mData.length;
            }

            if(mPos == 0) {
                mArrayFilled = true; // Switch to true only once (at least until reset() is called)
            }
            debugMsg("Push2 "+ value);
        }
    }

    private void debugMsg(String prefix) {
        if(!DEBUG) return;

        Log.d(TAG,prefix + ": PushCount=" + mPushCount + " Average=" + mAverage + " Pos=" + mPos + " ArrayFilled=" + mArrayFilled);
    }
}
