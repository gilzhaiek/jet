package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class DownhillSkiTrail_Black extends DownhillSkiTrail implements Parcelable
{
	
// constructors
	public DownhillSkiTrail_Black(String name, ArrayList<PointXY> _nodes, DownhillSkiTrail.DownhillDirection _downhillDir, Capability.DataSources dataSource) {
		super(WorldObjectTypes.DOWNHILLSKITRAIL_BLACK, name, _nodes, _downhillDir, dataSource);
		// calc mMainAngleFromNorth
	}
	
	
	public DownhillSkiTrail_Black(Parcel _parcel) {				
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<DownhillSkiTrail_Black> CREATOR  = new Parcelable.Creator<DownhillSkiTrail_Black>() {
        public DownhillSkiTrail_Black createFromParcel(Parcel _parcel) {
            return new DownhillSkiTrail_Black(_parcel);
        }

        public DownhillSkiTrail_Black[] newArray(int size) {
            return new DownhillSkiTrail_Black[size];
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
