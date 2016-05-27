package com.reconinstruments.geodataservice.clientinterface;

/* Import our Parcelable object types - note the code and aidl definitions need to be shared with client apps*/
import com.reconinstruments.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.objecttype.ObjectTypeList;
import com.reconinstruments.geodataservice.clientinterface.IGeodataServiceResponse;

interface IGeodataService {
	IGeodataServiceResponse getServiceState();
	IGeodataServiceResponse getClosestResorts(in ResortRequest _resortRequest);
	IGeodataServiceResponse defineMapComposition(String _clientID, in ObjectTypeList _objectTypeList);
	IGeodataServiceResponse releaseMapComposition(String _mapCompositionID);
	IGeodataServiceResponse getMapData(in GeoRegion _geoRegion, String _mapCompositionID);
	IGeodataServiceResponse getBuddies();
	IGeodataServiceResponse getClosestItem();
	IGeodataServiceResponse getRoute();
}