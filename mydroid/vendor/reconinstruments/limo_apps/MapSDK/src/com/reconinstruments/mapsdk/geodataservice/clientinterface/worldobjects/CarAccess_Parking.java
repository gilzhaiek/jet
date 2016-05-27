package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class CarAccess_Parking extends WO_POI  implements Parcelable
{
	private final static String TAG = "CarAccess_Parking";

	public CarAccess_Parking(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.CARACCESS_PARKING, name, _gpsLocation, dataSource);
	}

	
	public CarAccess_Parking(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<CarAccess_Parking> CREATOR  = new Parcelable.Creator<CarAccess_Parking>() {
        public CarAccess_Parking createFromParcel(Parcel _parcel) {
            return new CarAccess_Parking(_parcel);
        }

        public CarAccess_Parking[] newArray(int size) {
            return new CarAccess_Parking[size];
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
