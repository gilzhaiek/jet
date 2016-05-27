package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class Buddy extends WO_POI {
	
	private final static String TAG = "Buddy";

	public Buddy(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.BUDDY, name, _gpsLocation, dataSource);
	}


	public Buddy(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
	}

	//============ parcelable protocol handlers

	public static final Parcelable.Creator<Buddy> CREATOR  = new Parcelable.Creator<Buddy>() {
		public Buddy createFromParcel(Parcel _parcel) {
			return new Buddy(_parcel);
		}

		public Buddy[] newArray(int size) {
			return new Buddy[size];
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
