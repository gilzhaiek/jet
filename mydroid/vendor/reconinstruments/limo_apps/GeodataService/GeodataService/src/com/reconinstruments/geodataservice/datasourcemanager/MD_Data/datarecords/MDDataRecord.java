package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords;

import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.MDDataSource;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

public class MDDataRecord {
	private final static String TAG = "MDDataRecord";

	public MDDataSource.MDBaseDataTypes	mMDType;
	public String						mName;
	public boolean						mIsUserLocationDependent; 	// used in datasource GC... if true, this record was loaded based on user location - ie, should not be GC'd
	
	public MDDataRecord(MDDataSource.MDBaseDataTypes mdType, String name, boolean isUserLocationDependent) {
		mMDType = mdType;
		mName = name;
		mIsUserLocationDependent = isUserLocationDependent;
	}	
	
	public boolean ContainedInGR(RectXY GRbb) {
		return false;
	}
	
	
}
