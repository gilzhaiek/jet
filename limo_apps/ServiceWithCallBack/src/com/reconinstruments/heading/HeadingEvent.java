package com.reconinstruments.heading;

import android.os.Bundle;
import android.location.Location;

public class HeadingEvent {
	public Location mLocation = null;
	public float mYaw;
	public float mPitch;
	public float mRoll;
	public boolean mIsGPSHeading;
	  
	public HeadingEvent (Bundle b) {
		mLocation = (Location) b.getParcelable("Location");
		mYaw = b.getFloat("Yaw");
		mPitch = b.getFloat("Pitch");
		mRoll = b.getFloat("Roll");
		mIsGPSHeading = b.getBoolean("IsGPSHeading");
	}
}
