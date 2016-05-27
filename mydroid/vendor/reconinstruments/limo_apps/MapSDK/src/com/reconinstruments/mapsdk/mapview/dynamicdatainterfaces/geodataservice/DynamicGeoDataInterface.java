package com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.geodataservice;

import android.os.RemoteException;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;

public class DynamicGeoDataInterface {
	public interface IDynamicGeoData {
		public void SetGeodataServiceInterface(IGeodataService	geodataServiceInterface)  throws RemoteException;		
		public boolean SetGeodataServiceState(GeoDataServiceState geoDataServiceState);
	}
}
