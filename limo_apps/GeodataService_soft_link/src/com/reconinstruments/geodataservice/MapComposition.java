package com.reconinstruments.geodataservice;

import java.util.ArrayList;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.ObjectTypeList;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.SourcedObjectType;

public class MapComposition {
// constants

// members
	String							mClientID = "";				// supplied by client, used in broadcast notifications after asynch data loads
	ObjectTypeList					mObjectTypeList = null;
//	public ArrayList<DataRequest>  	mLoadingDataRequests = null;	// history of all datarequest for this composition ID
	public ArrayList<SourcedDataContainer>  mSourcedDataContainers = null;
		
// constructors
	public MapComposition(String clientDefinedID, ObjectTypeList objectTypeList) {
		mClientID = clientDefinedID;		
		mObjectTypeList = objectTypeList;
//		mLoadingDataRequests = new ArrayList<DataRequest>();
		mSourcedDataContainers = null;
		
		// create SourcdDataContainer templates from ObjectTypeList
		// parse requested data into SourcedDataContainers - one for each source type 
		// these "templates" will be copied into each dataRequest received for this MapComposition  
		SourcedDataContainer curSourcedDataContainer = null;		
		for(SourcedObjectType objectType : objectTypeList.mObjectTypes) {
			if(mSourcedDataContainers == null) {
				mSourcedDataContainers = new ArrayList<SourcedDataContainer>();
				curSourcedDataContainer = new SourcedDataContainer(objectType.mDataSource);	
				mSourcedDataContainers.add(curSourcedDataContainer);
			}
			else {
				if(curSourcedDataContainer.mSource != objectType.mDataSource) {
					Boolean found = false;
					for(SourcedDataContainer sourcedDataContainer : mSourcedDataContainers) {
						if(sourcedDataContainer.mSource == objectType.mDataSource) {
							curSourcedDataContainer = sourcedDataContainer;
							found = true;
							break;
						}
						if(!found) {
							curSourcedDataContainer = new SourcedDataContainer(objectType.mDataSource);
							mSourcedDataContainers.add(curSourcedDataContainer);
						}
					}
				}
			}
			
			curSourcedDataContainer.AddDataType(objectType.mWorldObjectType);
		}

	}
	
}
