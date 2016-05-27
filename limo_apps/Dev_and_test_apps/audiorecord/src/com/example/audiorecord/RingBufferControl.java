package com.example.audiorecord;

import android.util.Log;

//Mirroring RingBuffer, check http://en.wikipedia.org/wiki/Circular_buffer
public class RingBufferControl {
	public static final int BUFFER_EMPTY=0;
	public static final int BUFFER_AVAILIBLE=1;
	public static final int BUFFER_FULL=2;
	private  int mSize; /* maximum number of elements*/
	private  int mState;
	private  int mStart; /* index of oldest element*/
	private  int mEnd; /*index at which to write new element*/
	private  int mS_MSB;
	private  int mE_MSB;

	public RingBufferControl(int size){
		mSize= size;
		mStart= 0;
		mEnd= 0;
		mS_MSB=0;
		mE_MSB=0;
		mState=BUFFER_EMPTY;
	}
	
	public synchronized int setorgetState(boolean isSet, boolean isStartIndex) {
		if (isSet){
			int result[];
			if(isStartIndex){
				result=UpdateIndex(mStart,mS_MSB);
				mStart=result[0];
				mS_MSB=result[1];
			}
			else{
				result=UpdateIndex(mEnd,mE_MSB);
				mEnd=result[0];
				mE_MSB=result[1];
			}
			//Verify buffer state
			if(mEnd == mStart ){
				if(mE_MSB == mS_MSB)
					mState=BUFFER_EMPTY;
				else
					mState=BUFFER_FULL;
			}
			else
				mState=BUFFER_AVAILIBLE;
		}
		//Log.d(MainActivity.LOG_TAG,"mState="+mState+" "+"mEnd="+mEnd+" "+"mStart="+mStart);
		return mState;
	}
	
	private int[] UpdateIndex(int index, int msb){
		index= index+1;
		if(index==mSize){
			msb = msb^1;
			index=0;
		}
		return new int[] {index, msb};
	}
	
	//Step 1: Get write index
	public int GetPushIndex() {
		return mEnd;
	}
	//Step 2: Write to buffer, this will implemented in the write thread
	//Step 3: Update write index and buffer state
	public void UpdatePushIndex() {
		setorgetState(true,false);
	}
	
	//Step1: Get read index
	public int GetPopIndex() {
		return mStart;
	}
	//Step2: Read from buffer, this will implemented in the read thread
	//Step 3: Update read index and buffer state
	public void UpdatePopIndex(){
		setorgetState(true,true);
	}
}
