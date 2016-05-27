package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class DownhillSkiTrail_DBlack extends DownhillSkiTrail implements Parcelable
{
	private final static String TAG = "Terrain_Park";

// constructors
	public DownhillSkiTrail_DBlack(String name, ArrayList<PointXY> _nodes, DownhillSkiTrail.DownhillDirection _downhillDir, Capability.DataSources dataSource) {
		super(WorldObjectTypes.DOWNHILLSKITRAIL_DBLACK, name, _nodes, _downhillDir, dataSource);
		// calc mMainAngleFromNorth
	}
	
	
	public DownhillSkiTrail_DBlack(Parcel _parcel) {				
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<DownhillSkiTrail_DBlack> CREATOR  = new Parcelable.Creator<DownhillSkiTrail_DBlack>() {
        public DownhillSkiTrail_DBlack createFromParcel(Parcel _parcel) {
            return new DownhillSkiTrail_DBlack(_parcel);
        }

        public DownhillSkiTrail_DBlack[] newArray(int size) {
            return new DownhillSkiTrail_DBlack[size];
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
