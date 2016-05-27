package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class DrinkingWater extends WO_POI  implements Parcelable
{
	private final static String TAG = "DrinkingWater";

	public DrinkingWater(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.DRINKINGWATER, name, _gpsLocation, dataSource);
	}


	public DrinkingWater(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<DrinkingWater> CREATOR  = new Parcelable.Creator<DrinkingWater>() {
        public DrinkingWater createFromParcel(Parcel _parcel) {
            return new DrinkingWater(_parcel);
        }

        public DrinkingWater[] newArray(int size) {
            return new DrinkingWater[size];
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
