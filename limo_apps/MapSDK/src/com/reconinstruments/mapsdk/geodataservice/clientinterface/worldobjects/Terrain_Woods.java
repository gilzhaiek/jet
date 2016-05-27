package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Terrain_Woods extends Terrain  implements Parcelable 
{
	private final static String TAG = "Terrain_Woods";

	public Terrain_Woods(ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_WOODS, _nodes, dataSource);
	}

	public Terrain_Woods(Parcel _parcel) {				
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Terrain_Woods> CREATOR  = new Parcelable.Creator<Terrain_Woods>() {
        public Terrain_Woods createFromParcel(Parcel _parcel) {
            return new Terrain_Woods(_parcel);
        }

        public Terrain_Woods[] newArray(int size) {
            return new Terrain_Woods[size];
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
