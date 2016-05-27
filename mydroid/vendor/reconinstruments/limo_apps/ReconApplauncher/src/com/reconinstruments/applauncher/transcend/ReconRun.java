package com.reconinstruments.applauncher.transcend;

import android.os.Bundle;

public class ReconRun {
    public int mNumber;
    public int mStart;
    public float mAverageSpeed;
    public float mMaxSpeed;
    public float mDistance;
    public float mVertical;
    public float mMaxAltitude;
	
    private Bundle mBundle = null;
	
    protected Bundle generateBundle(){
	if (mBundle == null) mBundle = new Bundle();
	mBundle.putInt("Number", mNumber);
	mBundle.putInt("Start", mStart);
	mBundle.putFloat("AverageSpeed", mAverageSpeed);
	mBundle.putFloat("MaxSpeed", mMaxSpeed);
	mBundle.putFloat("Distance", mDistance);
	mBundle.putFloat("Vertical", mVertical);
	mBundle.putFloat("MaxAltitude", mMaxAltitude);
	return mBundle;
    }
	
    public ReconRun(Bundle b)
    //Constructor from Bundle
    {
	mNumber = b.getInt("Number");
	mStart = b.getInt("Start");
	mAverageSpeed = b.getFloat("AverageSpeed");
	mMaxSpeed = b.getFloat("MaxSpeed");
	mDistance = b.getFloat("Distance");
	mVertical = b.getFloat("Vertical");
	mMaxAltitude = b.getFloat("MaxAltitude");
	generateBundle();
    }
	
    public ReconRun(){
	
    }
	
    public Bundle getBundle(){
	return mBundle;
    }
	
    public void updateBundle(){
	generateBundle();
    }
}
	
