package com.reconinstruments.mapImages.objects;

import android.graphics.PointF;
import android.graphics.RectF;

public class ResortInfo {
	public int		ResortID;
	public String	Name;
	public int		CountryRegionID;
	public PointF	ResortLocation;
	public int 		MapVersion;
	public RectF	BoundingBox;
	public String	AssetBaseName;
	public String	RegionName = null;
	public String	CountryName;
		
	public ResortInfo(int resortID , String name, int countryRegionID, PointF resortLocation, int mapVersion, RectF boundingBox, String countryName, String regionName){
		ResortID		= resortID;
		Name			= name;
		CountryRegionID	= countryRegionID;
		ResortLocation	= resortLocation;
		MapVersion		= mapVersion;
		BoundingBox		= boundingBox;
		AssetBaseName	= "" + resortID;
		CountryName		= countryName;
		RegionName		= regionName;
	}
	
	// Override the default toString function to output the resort name as the string to feed to an ArrayAdapter to render on a listView
	@Override
	public String toString()
	{
		return Name;
	}
	
	public String GetParentName(){
		if(RegionName != null) return CountryName + "/" + RegionName;
		else return CountryName;
	}
}
