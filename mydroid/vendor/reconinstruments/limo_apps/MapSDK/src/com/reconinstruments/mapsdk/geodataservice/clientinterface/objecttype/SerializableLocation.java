package com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype;

import java.io.Serializable;

public class SerializableLocation implements Serializable{

	private static final long serialVersionUID = 750646859829149L;
	
	public double mLongitude;
	public double mLatitude;
	public SerializableLocation(double longitude, double latitude) {
		mLongitude = longitude;
		mLatitude  = latitude;
	}

} 