package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class DownhillSkiTrail extends WO_Polyline implements Parcelable
{
	private final static String TAG = "DownhillSkiTrail";

	public enum DownhillDirection {
		NO_SLOPE_FLAT,
		LOWEST_POINT_IS_LAST,
		LOWEST_POINT_IS_FIRST
	}
	
	DownhillDirection	mDownhillDirection = DownhillDirection.LOWEST_POINT_IS_FIRST;
	
	
// constructors
	public DownhillSkiTrail(WorldObjectTypes _type, String name, ArrayList<PointXY> _nodes, DownhillDirection _downhillDir, Capability.DataSources dataSource) {
		super(_type, name, _nodes, dataSource);
		mDownhillDirection = _downhillDir;
	}
	
	public DownhillSkiTrail(Parcel _parcel) {				
		super(_parcel);			// first instantiate it 
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<DownhillSkiTrail> CREATOR  = new Parcelable.Creator<DownhillSkiTrail>() {
        public DownhillSkiTrail createFromParcel(Parcel _parcel) {
            return new DownhillSkiTrail(_parcel);
        }

        public DownhillSkiTrail[] newArray(int size) {
            return new DownhillSkiTrail[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
//    	int originalSize = _parcel.dataSize();
		super.writeToParcel(_parcel, flags);					// write all super member data to parcel first
    	_parcel.writeInt(mDownhillDirection.ordinal());
//    	Log.d(TAG, "DownhillSkiTrial subtype parcel size=" + (_parcel.dataSize()-originalSize));
   }
    
    @Override
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	super.readFromParcel(_parcel);
    	mDownhillDirection = DownhillDirection.values()[_parcel.readInt()];
    }	
	

}
