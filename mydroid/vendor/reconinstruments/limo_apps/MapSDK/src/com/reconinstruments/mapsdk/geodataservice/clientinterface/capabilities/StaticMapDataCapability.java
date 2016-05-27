package com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;


public class StaticMapDataCapability extends Capability implements Parcelable
{
// constants	

	// members
		public WorldObjectTypes  	mWorldObjectType;
		public DataSources mDataSource;
		
	// constructors
		public StaticMapDataCapability(WorldObjectTypes _worldObjectType, DataSources _dataSource) {
			super(CapabilityTypes.STATIC_MAP_DATA);
			mWorldObjectType = _worldObjectType;
			mDataSource = _dataSource;
		}

		public StaticMapDataCapability(Parcel _parcel) {
			super(_parcel);				// calls (overridden) readFromParcel(_parcel);
		}

	//============ parcelable protocol handlers
		
	    public static final Parcelable.Creator<StaticMapDataCapability> CREATOR  = new Parcelable.Creator<StaticMapDataCapability>() {
	        public StaticMapDataCapability createFromParcel(Parcel _parcel) {
	            return new StaticMapDataCapability(_parcel);
	        }

	        public StaticMapDataCapability[] newArray(int size) {
	            return new StaticMapDataCapability[size];
	        }
	    };
	    
	    @Override
	    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
	    	super.writeToParcel(_parcel, flags);
	    	_parcel.writeInt(mWorldObjectType.ordinal());
	    	_parcel.writeInt(mDataSource.ordinal());
	   }
	    
	    @Override
	    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
	    	super.readFromParcel(_parcel);
	    	mWorldObjectType = WorldObjectTypes.values()[_parcel.readInt()];
	    	mDataSource = DataSources.values()[_parcel.readInt()];
	    }

}
