package com.reconinstruments.applauncher.transcend;

import android.os.Bundle;
import com.reconinstruments.utils.stats.StatsUtil;

public class ReconJump {
    public final static int INVALID_VALUE = StatsUtil.INVALID_VALUE;
    public final static int INVALID_NUMBER = StatsUtil.INVALID_NUMBER;
    public final static long INVALID_DATE = StatsUtil.INVALID_DATE;
    public final static int INVALID_AIR = StatsUtil.INVALID_AIR;
    public final static float INVALID_DISTANCE = StatsUtil.INVALID_DISTANCE;
    public final static float INVALID_DROP = StatsUtil.INVALID_DROP;
    public final static float INVALID_HEIGHT = StatsUtil.INVALID_HEIGHT;


    
    public int mNumber = INVALID_NUMBER;
    public long mDate = INVALID_DATE;
    public int mAir = INVALID_AIR;
    public float mDistance = INVALID_DISTANCE ;
    public float mDrop = INVALID_DROP;
    public float mHeight = INVALID_HEIGHT;


    private Bundle mBundle = null;;
	
    protected Bundle generateBundle() {
	if (mBundle == null) mBundle = new Bundle();

	mBundle.putInt("Number", mNumber) ;
	mBundle.putLong("Date", mDate) ;
	mBundle.putInt("Air", mAir) ;
	mBundle.putFloat("Distance", mDistance) ;
	mBundle.putFloat("Drop", mDrop) ;
	mBundle.putFloat("Height", mHeight) ;

	return mBundle;

    }
	
    public ReconJump(Bundle b) {
	//Constructor from Bundle
	if (b != null) {
	mNumber = b.getInt("Number");
	mDate = b.getLong("Date");
	mAir = b.getInt("Air");
	mDistance = b.getFloat("Distance");
	mDrop = b.getFloat("Drop");
	mHeight = b.getFloat("Height");
	}

	generateBundle();
    }
	
    public ReconJump(){
	
    }
	
    public Bundle getBundle(){
	return mBundle;
    }
	
    public void updateBundle(){
	generateBundle();
    }

}
