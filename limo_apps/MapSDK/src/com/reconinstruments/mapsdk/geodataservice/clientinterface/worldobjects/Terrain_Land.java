package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Terrain_Land extends Terrain  implements Parcelable 
{
	private final static String TAG = "Terrain_Land";

	public Terrain_Land(ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_LAND, _nodes, dataSource);
	}

	public Terrain_Land(Parcel _parcel) {				
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Terrain_Land> CREATOR  = new Parcelable.Creator<Terrain_Land>() {
        public Terrain_Land createFromParcel(Parcel _parcel) {
            return new Terrain_Land(_parcel);
        }

        public Terrain_Land[] newArray(int size) {
            return new Terrain_Land[size];
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
