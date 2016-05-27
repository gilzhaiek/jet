package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class SkiResortService_Restaurant extends WO_POI  implements Parcelable
{
	private final static String TAG = "SkiResortService_Restaurant";

	public SkiResortService_Restaurant(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.SKIRESORTSERVICE_RESTAURANT,  name, _gpsLocation, dataSource);
	}


	public SkiResortService_Restaurant(Parcel _parcel) {				
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<SkiResortService_Restaurant> CREATOR  = new Parcelable.Creator<SkiResortService_Restaurant>() {
        public SkiResortService_Restaurant createFromParcel(Parcel _parcel) {
            return new SkiResortService_Restaurant(_parcel);
        }

        public SkiResortService_Restaurant[] newArray(int size) {
            return new SkiResortService_Restaurant[size];
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
