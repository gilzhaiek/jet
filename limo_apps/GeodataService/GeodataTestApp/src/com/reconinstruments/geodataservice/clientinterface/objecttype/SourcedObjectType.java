package com.reconinstruments.geodataservice.clientinterface.objecttype;

import java.io.Serializable;

import com.reconinstruments.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;


public class SourcedObjectType implements Serializable
{
// constants	
	private final static String TAG = "SourcedObjectType";

// members
	public WorldObjectTypes  							mWorldObjectType;
	public Capability.DataSources mDataSource;
	
// constructors
	public SourcedObjectType(WorldObjectTypes _worldObjectType, Capability.DataSources _dataSource) {
		mWorldObjectType = _worldObjectType;
		mDataSource = _dataSource;
	}

	public SourcedObjectType(DataRetrievalCapability drc) {
		mWorldObjectType = drc.mWorldObjectType;
		mDataSource = drc.mDataSource;
	}

}
