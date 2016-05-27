package com.reconinstruments.mapImages.mapview;

import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.helpers.LocationTransformer;

public class MapBuddyInfo {

	int					mID;
	String 				mName;
	double	 			mLatitude;	// in world GPS coords
	double				mLongitude;
	long 				mLocationTimeStamp;
	float				mDrawingX;	// calculated from GPS into drawing coords
	float				mDrawingY;
	boolean				mOnline;
	MapDrawings.State 	mState;
	long 				mLastUpdateTime;

	public MapBuddyInfo(int id, String name, double latitude, double longitude, long locationTime, long receivedTime, float maxDrawingY, LocationTransformer locationTransformer) {
		mID = id;
		mName = name;
		mLatitude = latitude;
		mLongitude = longitude;
		mState = MapDrawings.State.NORMAL;
		mDrawingX = 0.0f;
		mDrawingY = 0.0f;
		mLocationTimeStamp = locationTime;
		mLastUpdateTime = receivedTime;
		mOnline = false;

		if(locationTransformer != null) {
			mDrawingX = (float)locationTransformer.TransformLongitude(mLatitude, mLongitude);;
			mDrawingY = maxDrawingY - (float)locationTransformer.TransformLatitude(mLatitude, mLongitude);
		}

	}

}
