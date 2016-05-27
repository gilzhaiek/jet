package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Terrain extends WO_Polyline  implements Parcelable 
{
	private final static String TAG = "Terrain";

	
	public Terrain(WorldObjectTypes _type, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(_type, "terrain", _nodes, dataSource);		// terrains don't have names by default
	}

	public Terrain(Parcel _parcel) {			
		super(_parcel);			// calls (overridden) readFromParcel(_parcel);
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Terrain> CREATOR  = new Parcelable.Creator<Terrain>() {
        public Terrain createFromParcel(Parcel _parcel) {
            return new Terrain(_parcel);
        }

        public Terrain[] newArray(int size) {
            return new Terrain[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
//    	int originalSize = _parcel.dataSize();
		super.writeToParcel(_parcel, flags);					// write all super member data to parcel first
//    	Log.d(TAG, "Terrain subtype parcel size=" + (_parcel.dataSize()-originalSize));
   }
    
    @Override
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	super.readFromParcel(_parcel);
    }
    
//======================================
// methods

}
