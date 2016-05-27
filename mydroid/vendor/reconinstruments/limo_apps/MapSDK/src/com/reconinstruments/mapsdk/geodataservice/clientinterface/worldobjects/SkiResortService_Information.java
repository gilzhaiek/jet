package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class SkiResortService_Information extends WO_POI  implements Parcelable
{
	private final static String TAG = "SkiResortService_Information";

	public SkiResortService_Information(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.SKIRESORTSERVICE_INFO, name, _gpsLocation, dataSource);
	}


	public SkiResortService_Information(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<SkiResortService_Information> CREATOR  = new Parcelable.Creator<SkiResortService_Information>() {
        public SkiResortService_Information createFromParcel(Parcel _parcel) {
            return new SkiResortService_Information(_parcel);
        }

        public SkiResortService_Information[] newArray(int size) {
            return new SkiResortService_Information[size];
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
