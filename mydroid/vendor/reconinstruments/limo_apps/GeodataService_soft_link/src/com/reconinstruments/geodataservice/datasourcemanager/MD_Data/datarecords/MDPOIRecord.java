package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords;

import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.MDDataSource;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

public class MDPOIRecord extends MDDataRecord 
{
	private final static String TAG = "MDPOIRecord";

	public PointXY	mLocation;
	
	public MDPOIRecord(MDDataSource.MDBaseDataTypes type, String name, PointXY location, boolean isUserLocationDependent) {
		super(type, name, isUserLocationDependent);
		mLocation = location;
	}
	
	@Override
	public boolean ContainedInGR(RectXY GRbb) {
		if(GRbb.Contains(mLocation.x, mLocation.y) ) {
//			Log.e(TAG, " - POI hit test: " + mName + ": " + mLocation.x + ", " + ": " + mLocation.x + ", | "+ GRbb.left +  " : " + GRbb.right + " : " + GRbb.top + " : " + GRbb.bottom );
			return true;
		}
		else {
//			Log.d(TAG, " - POI hit test: " + mName + ": " + mLocation.x + ", " + ": " + mLocation.x + ", | "+ GRbb.left +  " : " + GRbb.right + " : " + GRbb.top + " : " + GRbb.bottom );
			return false;
		}	}

}
