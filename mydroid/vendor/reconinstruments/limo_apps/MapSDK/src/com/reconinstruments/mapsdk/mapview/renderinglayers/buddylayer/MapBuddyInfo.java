package com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer;

import android.content.Context;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Buddy;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing.WorldObjectDrawingTypes;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;


public class MapBuddyInfo {

	int					mID;
//	String 				mName;
//	double	 			mLatitude;	// in world GPS coords
//	double				mLongitude;
//	float				mDrawingX;	// calculated from GPS into drawing coords
//	float				mDrawingY;
//	PointXY				mDrawing = null;
	boolean				mExpired;	// set true when info expires after time out.. ready for GC
	boolean				mOnline;
	POIDrawing			mBuddyDrawing;
	public WorldObjectDrawing.WorldObjectDrawingStates 	mState;
	long 				mLastUpdateTime;
	PointXY				mScreenCoordinates = null;
	float               mDistToMeInM = 0.0f;

	public MapBuddyInfo(Context context, int id, String name, double latitude, double longitude, long time, World2DrawingTransformer world2DrawingTransformer) {
		mID = id;
//		mName = name;
//		mLatitude = latitude;
//		mLongitude = longitude;
//		mDrawingX = 0.0f;
//		mDrawingY = 0.0f;
		mLastUpdateTime = time;
		mOnline = false;
		mExpired = false;	
		
		Buddy newBuddy = new Buddy(name, new PointXY((float)longitude, (float)latitude), Capability.DataSources.RECON_BUDDY_TRACKING_SERVER) ;
		mBuddyDrawing = new POIDrawing(context, WorldObjectDrawingTypes.BUDDY, newBuddy, null);
		mState = WorldObjectDrawing.WorldObjectDrawingStates.NORMAL;
	}
	

}
