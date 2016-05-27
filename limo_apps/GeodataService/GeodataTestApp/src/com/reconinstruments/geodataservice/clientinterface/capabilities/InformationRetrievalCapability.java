package com.reconinstruments.geodataservice.clientinterface.capabilities;

import java.io.Serializable;


public class InformationRetrievalCapability extends Capability implements Serializable
{
// constants	
	public enum InfoRetrievalTypes {
		CLOSEST_RESORTS,
		OTHER
	}

	// members
		public InfoRetrievalTypes  	mInfoType;
		public Capability.DataSources mDataSource;
		
	// constructors
		public InformationRetrievalCapability(InfoRetrievalTypes infoType, Capability.DataSources dataSource) {
			super(CapabilityTypes.INFO_RETRIEVAL);
			mInfoType = infoType;
			mDataSource = dataSource;
		}


}
