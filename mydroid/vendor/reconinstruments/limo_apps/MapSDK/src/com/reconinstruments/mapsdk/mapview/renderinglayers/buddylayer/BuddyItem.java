package com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer;


import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;

public class BuddyItem {
	public String mName = null;
	public String mID = null;
	public PointXY mDLocation = null;
	public float mDistanceToMe = 0.0f;
	public boolean mHighlighted = false;
	
	public BuddyItem(String name, PointXY location, float distanceToMe) {
		mName = name;
		mID = "";
		mDLocation = location;
		mDistanceToMe = distanceToMe;
	}
	
	public BuddyItem(String name, String id, PointXY location, float distanceToMe) {
		mName = name;
		mID = id;
		mDLocation = location;
		mDistanceToMe = distanceToMe;
	}
}
