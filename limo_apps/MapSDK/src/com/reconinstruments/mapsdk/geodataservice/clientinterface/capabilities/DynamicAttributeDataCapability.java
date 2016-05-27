package com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities;

import android.os.Parcel;
import android.os.Parcelable;


public class DynamicAttributeDataCapability extends Capability implements Parcelable
{
// constants	
	private static final long serialVersionUID = 234234212311L;

	// members
		public DataSources 	mDataSource;
		
	// constructors
		public DynamicAttributeDataCapability(Capability.CapabilityTypes dynamicCapability, DataSources dataSource) {
			super(dynamicCapability);
			mDataSource = dataSource;
		}

		public DynamicAttributeDataCapability(Parcel _parcel) {	
			super(_parcel);				// calls (overridden) readFromParcel(_parcel);
		}

	//============ parcelable protocol handlers
		
	    public static final Parcelable.Creator<DynamicAttributeDataCapability> CREATOR  = new Parcelable.Creator<DynamicAttributeDataCapability>() {
	        public DynamicAttributeDataCapability createFromParcel(Parcel _parcel) {
	            return new DynamicAttributeDataCapability(_parcel);
	        }

	        public DynamicAttributeDataCapability[] newArray(int size) {
	            return new DynamicAttributeDataCapability[size];
	        }
	    };
	    
	    @Override
	    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
	    	super.writeToParcel(_parcel, flags);
	    	_parcel.writeInt(mDataSource.ordinal());
	   }
	    
	    @Override
	    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
	    	super.readFromParcel(_parcel);
	    	mDataSource = DataSources.values()[_parcel.readInt()];
	    }

}
