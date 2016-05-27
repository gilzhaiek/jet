package com.reconinstruments.applauncher.transcend;

// Class for taking care of continuous calibration.
public class AltitudeCalibrator {
    private float[] mData;
    private int mSize;
    public float mAverage;
    private int mCounter = 0;
    public AltitudeCalibrator(int size, float initialFill) {
	mSize = size;
	mData = new float[size];
	for (int i = 0;i < size;i++) {
	    mData[i] = initialFill;
	}
    }
    float push(float v, int repeat) {
	for (int i=0; i<repeat;i++) {
	    pushSingleWithNoUpdate(v);
	}
	mAverage = calculateAverage();
	return mAverage;
    }
    void pushSingleWithNoUpdate(float v) {
	mCounter = (mCounter + 1) % mSize;
	mData[mCounter] = v;
    }
    void push(float v) {
	pushSingleWithNoUpdate(v);
	mAverage = calculateAverage();
    }
    float calculateAverage() {
	float sum = 0;
	for (int i=0;i<mSize;i++) {
	    sum+=mData[i];
	}
	return (sum / mSize);
    }
}