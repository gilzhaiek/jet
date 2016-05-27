package com.reconinstruments.motiondetector;

public class GPSCycleManager {

    private static final int HELD_SPEEDS = 10;
    private static final int STOP_SPEEDS = 2;

    private static final float STOP_THRESH_KPH = (float) 7.5;
    private static final float MOVE_THRESH_KPH = (float) 10;

    private static final float MPS_TO_KPH = (float) 3.6;

    private boolean mMoveStatus;

    private float[] mPastSpeeds;

    public GPSCycleManager() {
	this.mPastSpeeds = new float[HELD_SPEEDS];
    }

    public void addNewSpeed(float speed) {

	for (int i = HELD_SPEEDS - 1; i > 0; i--) {
	    mPastSpeeds[i] = mPastSpeeds[i-1];
	}
	mPastSpeeds[0] = speed * MPS_TO_KPH;
    }

    public boolean movementAlgorithm() {

	if (mPastSpeeds[0] < STOP_THRESH_KPH &&
	    mPastSpeeds[1] < STOP_THRESH_KPH &&
	    mPastSpeeds[2] < STOP_THRESH_KPH &&
	    mPastSpeeds[3] < STOP_THRESH_KPH &&
	    mPastSpeeds[4] < STOP_THRESH_KPH &&
	    mPastSpeeds[5] < STOP_THRESH_KPH &&
	    mPastSpeeds[6] < STOP_THRESH_KPH &&
	    mPastSpeeds[7] < STOP_THRESH_KPH &&
	    mPastSpeeds[8] < STOP_THRESH_KPH &&
	    mPastSpeeds[9] < STOP_THRESH_KPH) {
	    mMoveStatus = false;
	    return false;
	}
	else if (mPastSpeeds[0] > MOVE_THRESH_KPH &&
		 mPastSpeeds[1] > MOVE_THRESH_KPH) {
	    mMoveStatus = true;
	    return true;
	}
	else
	    return mMoveStatus;
	    
    }

}
