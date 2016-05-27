package com.reconinstruments.applauncher.transcend;
public class CircularFloatArray {
    private float[] mData; 
    private int mSize;
    private int mCounter = 0;
    public CircularFloatArray(int size) {
	 this(size,0);
    }
    public CircularFloatArray(int size,float v) {
        mSize = size;
        mData = new float[size];
         for (int i = 0; i < size;i++) {
	    mData[i] = v;
	}
    }
    float readPrevious(int i) {
	// If it goes deepest in history 
	if (i >= mSize) {
		i = mSize - 1;
	}
	i = (mCounter - i + mSize) % mSize;
	return mData[i];
    }
    void push ( float v) {
        mCounter = (mCounter + 1) % mSize;
        mData[mCounter] = v;
    }
}