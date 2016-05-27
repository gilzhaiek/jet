package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Waterway extends WO_Polyline  implements Parcelable
{
	private final static String TAG = "CarAccess_Roadway";

	public Waterway(WorldObjectTypes roadType, String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(roadType, name, _nodes, dataSource);
	}
	
	public Waterway(Parcel _parcel) {					
		super(_parcel);				// first instantiate it 
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Waterway> CREATOR  = new Parcelable.Creator<Waterway>() {
        public Waterway createFromParcel(Parcel _parcel) {
            return new Waterway(_parcel);
        }

        public Waterway[] newArray(int size) {
            return new Waterway[size];
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
