package com.reconinstruments.geodataservice.clientinterface;

import com.reconinstruments.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.RectXY;

public class ResortInfoResponse {
	public String	mName;
	public float	mDistanceFromTargetPoint;
	public boolean  mTargetPointWithinResortBoundingBox;
	public PointXY	mLocation;
		
	public ResortInfoResponse(String name, float distance, PointXY location, boolean inBB){
		mName					 = name;
		mDistanceFromTargetPoint = distance;
		mLocation = location;
		mTargetPointWithinResortBoundingBox = inBB;
	}
}
