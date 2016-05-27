package com.reconinstruments.geodataservice.datasourcemanager.MD_Data;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

public class MDResortInfo {
	public int		mResortID;
	public String	mName;
	public int		mCountryRegionID;
	public PointXY	mResortLocation;
	public int 		mMapVersion;
	public RectXY	mBoundingBox;
	public PointXY	mCentroid;
	public String	mAssetBaseName;
	public String	mRegionName = null;
	public String	mCountryName;
		
	public MDResortInfo(int resortID , String name, int countryRegionID, PointXY resortLocation, int mapVersion, RectXY boundingBox, String countryName, String regionName){
		mResortID			= resortID;
		mName				= name;
		mCountryRegionID	= countryRegionID;
		mResortLocation		= resortLocation;
		mMapVersion			= mapVersion;
		mBoundingBox		= boundingBox;
		mAssetBaseName		= "" + resortID;
		mCountryName		= countryName;
		mRegionName			= regionName;
	}
	
	// Override the default toString function to output the resort name as the string to feed to an ArrayAdapter to render on a listView
	@Override
	public String toString() {
		return mName;
	}
	
	public String GetParentName() {
		if(mRegionName != null) return mCountryName + "/" + mRegionName;
		else return mCountryName;
	}
}
