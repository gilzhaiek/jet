package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import android.util.Log;

import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.ReconBaseDataSource;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

public class ReconBasePOIRecord extends ReconBaseDataRecord 
{
	private final static String TAG = "ReconBasePOIRecord";

	public PointXY	mLocation;
	
	public ReconBasePOIRecord(ReconBaseDataSource.BaseDataTypes type, String name, PointXY location, boolean isUserLocationDependent) {
		super(type, name, isUserLocationDependent);
		mLocation = location;
	}
	
	@Override
	public int PackingSize() throws UnsupportedEncodingException {
		int size = super.PackingSize();
		size += 2 * BYTES_IN_FLOAT; 			// for gps location, PointXY = 2 floats
		return size;
	}
	
	@Override
	public void PackIntoBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		super.PackIntoBuf(packedTileData);
		
		packedTileData.putFloat(mLocation.x);
		packedTileData.putFloat(mLocation.y);
	}

	public void UnpackFromBuf(ByteBuffer packedTileData) throws UnsupportedEncodingException {
		super.UnpackFromBuf(packedTileData);
		
		mLocation = new PointXY(packedTileData.getFloat(), packedTileData.getFloat() );
//		Log.d(TAG, " - POI from RGZ: " + mOSMType + ": " + mName + ": " + mLocation.x + ", " + ": " + mLocation.y );
	}

	@Override
	public boolean ContainedInGR(RectXY GRbb) {
		if(GRbb.Contains(mLocation.x, mLocation.y) ) {
//			Log.e(TAG, " - POI hit test: " + mName + ": " + mLocation.x + ", " + ": " + mLocation.y + ", | "+ GRbb.left +  " : " + GRbb.right + " : " + GRbb.top + " : " + GRbb.bottom );
			return true;
		}
		else {
//			Log.d(TAG, " - POI hit test: " + mName + ": " + mLocation.x + ", " + ": " + mLocation.y + ", | "+ GRbb.left +  " : " + GRbb.right + " : " + GRbb.top + " : " + GRbb.bottom );
			return false;
		}	
	}

	public void SetLocation(float longitude, float latitude) {
		mLocation = new PointXY(longitude, latitude);
	}
}
