package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Terrain_Park extends Terrain  implements Parcelable 
{
	private final static String TAG = "Terrain_Park";

	public Terrain_Park(ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_PARK, _nodes, dataSource);
	}

	public Terrain_Park(Parcel _parcel) {			
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Terrain_Park> CREATOR  = new Parcelable.Creator<Terrain_Park>() {
        public Terrain_Park createFromParcel(Parcel _parcel) {
            return new Terrain_Park(_parcel);
        }

        public Terrain_Park[] newArray(int size) {
            return new Terrain_Park[size];
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
