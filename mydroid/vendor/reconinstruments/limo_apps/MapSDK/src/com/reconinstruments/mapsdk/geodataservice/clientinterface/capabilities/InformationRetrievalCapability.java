package com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities;

import android.os.Parcel;
import android.os.Parcelable;


public class InformationRetrievalCapability extends Capability implements Parcelable
{
// constants	
	private static final long serialVersionUID = 235434212311L;
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


		public InformationRetrievalCapability(Parcel _parcel) {	
			super(_parcel);				// calls (overridden) readFromParcel(_parcel);
		}

	//============ parcelable protocol handlers
		
	    public static final Parcelable.Creator<InformationRetrievalCapability> CREATOR  = new Parcelable.Creator<InformationRetrievalCapability>() {
	        public InformationRetrievalCapability createFromParcel(Parcel _parcel) {
	            return new InformationRetrievalCapability(_parcel);
	        }

	        public InformationRetrievalCapability[] newArray(int size) {
	            return new InformationRetrievalCapability[size];
	        }
	    };
	    
	    @Override
	    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
	    	super.writeToParcel(_parcel, flags);
	    	_parcel.writeInt(mInfoType.ordinal());
	    	_parcel.writeInt(mDataSource.ordinal());
	   }
	    
	    @Override
	    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
	    	super.readFromParcel(_parcel);
	    	mInfoType = InfoRetrievalTypes.values()[_parcel.readInt()];
	    	mDataSource = Capability.DataSources.values()[_parcel.readInt()];
	    }
}
