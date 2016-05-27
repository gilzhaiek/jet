package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Store extends WO_POI  implements Parcelable
{
	private final static String TAG = "Store";

	public Store(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.STORE, name, _gpsLocation, dataSource);
	}


	public Store(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Store> CREATOR  = new Parcelable.Creator<Store>() {
        public Store createFromParcel(Parcel _parcel) {
            return new Store(_parcel);
        }

        public Store[] newArray(int size) {
            return new Store[size];
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
