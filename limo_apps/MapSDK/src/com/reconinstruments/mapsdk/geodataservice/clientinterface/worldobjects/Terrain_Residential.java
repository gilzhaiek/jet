package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Terrain_Residential extends Terrain  implements Parcelable 
{
	private final static String TAG = "Terrain_Residential";

	public Terrain_Residential(ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_RESIDENTIAL, _nodes, dataSource);
	}

	public Terrain_Residential(Parcel _parcel) {			
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Terrain_Residential> CREATOR  = new Parcelable.Creator<Terrain_Residential>() {
        public Terrain_Residential createFromParcel(Parcel _parcel) {
            return new Terrain_Residential(_parcel);
        }

        public Terrain_Residential[] newArray(int size) {
            return new Terrain_Residential[size];
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
