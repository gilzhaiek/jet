package com.reconinstruments.mapsdk.geodataservice.clientinterface;

/* Import our Parcelable object types - note the code and aidl definitions need to be shared with client apps*/
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.ObjectTypeList;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;

interface IGeodataService {
	IGeodataServiceResponse getServiceState();
	IGeodataServiceResponse getClosestResorts(in ResortRequest _resortRequest);
	IGeodataServiceResponse defineMapComposition(in String _clientID, in ObjectTypeList _objectTypeList);
	IGeodataServiceResponse releaseMapComposition(in String _mapCompositionID);
	IGeodataServiceResponse getMapTileData(in int _geoTileIndex, in String _mapCompositionID);
	IGeodataServiceResponse getUserLocation();
	IGeodataServiceResponse getBuddies();
	IGeodataServiceResponse registerForBuddies(in String _processID);
	IGeodataServiceResponse unregisterForBuddies(in String _processID);
	IGeodataServiceResponse getClosestItem();
	IGeodataServiceResponse getRoute();
}