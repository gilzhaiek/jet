package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Road extends WO_Polyline  implements Parcelable
{
	private final static String TAG = "Road";

	public Road(WorldObjectTypes roadType, String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(roadType, name, _nodes, dataSource);
	}
	
	public Road(Parcel _parcel) {					
		super(_parcel);				// first instantiate it 
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Road> CREATOR  = new Parcelable.Creator<Road>() {
        public Road createFromParcel(Parcel _parcel) {
            return new Road(_parcel);
        }

        public Road[] newArray(int size) {
            return new Road[size];
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
