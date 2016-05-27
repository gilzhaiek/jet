package com.reconinstruments.applauncher.transcend;
public class CircularArray {
    private Number[] mData;
    private int mSize;
    private int mCounter = 0;

    public CircularArray(int size, Number value) {
	mSize = size;
	mData = new Number[size];
	for (int i = 0; i< size;i++) {
	    mData[i] = value;
	}
    }
    public Number readPrevious(int i) {
	i = (mCounter - (i % mSize) + mSize) % mSize;
	return mData[i];
    }
    public void push (Number v) {
	mCounter = (mCounter + 1) % mSize;
	mData[mCounter] = v;
    }
    public int getSize() {
	return mSize;
    }
}