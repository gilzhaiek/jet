package com.reconinstruments.geodataservice.clientinterface.capabilities;

import java.io.Serializable;

import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;


public class DataRetrievalCapability extends Capability implements Serializable
{
// constants	
	public enum StaticMapDataSources {
		RECON_SKI_DATA,
		OPEN_STREET_MAPS
	}

	// members
		public WorldObjectTypes  	mWorldObjectType;
		public StaticMapDataSources mDataSource;
		
	// constructors
		public DataRetrievalCapability(WorldObjectTypes _worldObjectType, StaticMapDataSources _dataSource) {
			super(CapabilityTypes.STATIC_MAP_DATA);
			mWorldObjectType = _worldObjectType;
			mDataSource = _dataSource;
		}


}
