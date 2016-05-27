package com.reconinstruments.hudserver.metrics;

public class CircularFloatArray {

    private float[] mData; 
    private int mSize;
    private int mCounter = 0;

    /**
     * Construct a circular float array
     * @param size of the array
     */
    public CircularFloatArray(int size) {
        this(size,Float.NaN);
    }

    /**
     * Construct a circular float array
     * @param size of the array
     * @param value to fill in the array
     */
    public CircularFloatArray(int size,float value) {
        mSize = size;
        mData = new float[size];
        fillValues(value);
    }

    private float readPrevious(int index) {
        // If it goes deepest in history 
        if (index >= mSize) {
            index = mSize - 1;
        }
        index = (mCounter - index + mSize) % mSize;
        return mData[index];
    }

    /**
     * @return the oldest pushed value
     */
    public float readOldestValue(){
        return readPrevious(-1);
    }

    /**
     * @return the most recent pushed value
     */
    public float readMostRecentValue(){
        return readPrevious(0);
    }

    /**
     * Push value into the circular array
     * @param value
     */
    public void push (float value) {
        mCounter = (mCounter + 1) % mSize;
        mData[mCounter] = value;
    }


    /**
     * Reset the array by filling up the array with Float.NaN
     */
    public void reset(){
        fillValues(Float.NaN);
    }

    /**
     * Fill up the array with the provided value
     * @param value 
     */
    public void fillValues(float value) {
        for (int i = 0; i < mSize;i++) {
            mData[i] = value;
        }
    }
}