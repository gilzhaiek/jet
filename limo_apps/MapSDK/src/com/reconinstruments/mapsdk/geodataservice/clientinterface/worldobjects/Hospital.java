package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Hospital extends WO_POI  implements Parcelable
{
	private final static String TAG = "Hospital";

	public Hospital(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.HOSPITAL, name, _gpsLocation, dataSource);
	}


	public Hospital(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Hospital> CREATOR  = new Parcelable.Creator<Hospital>() {
        public Hospital createFromParcel(Parcel _parcel) {
            return new Hospital(_parcel);
        }

        public Hospital[] newArray(int size) {
            return new Hospital[size];
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
