package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class DownhillSkiTrail_Green extends DownhillSkiTrail implements Parcelable
{
	private final static String TAG = "DownhillSkiTrail_Green";

// constructors
	public DownhillSkiTrail_Green(String name, ArrayList<PointXY> _nodes, DownhillSkiTrail.DownhillDirection _downhillDir, Capability.DataSources dataSource) {
		super(WorldObjectTypes.DOWNHILLSKITRAIL_GREEN, name, _nodes, _downhillDir, dataSource);
		// calc mMainAngleFromNorth
	}
	
	
	public DownhillSkiTrail_Green(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<DownhillSkiTrail_Green> CREATOR  = new Parcelable.Creator<DownhillSkiTrail_Green>() {
        public DownhillSkiTrail_Green createFromParcel(Parcel _parcel) {
            return new DownhillSkiTrail_Green(_parcel);
        }

        public DownhillSkiTrail_Green[] newArray(int size) {
            return new DownhillSkiTrail_Green[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
		super.writeToParcel(_parcel, flags);					// write all super member data to parcel first
   }
    
    @Override
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	super.readFromParcel(_parcel);
    }	
	

}
