package com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.StaticMapDataCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;


public class SourcedObjectType implements Parcelable
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

	public SourcedObjectType(StaticMapDataCapability drc) {
		mWorldObjectType = drc.mWorldObjectType;
		mDataSource = drc.mDataSource;
	}

	public SourcedObjectType(Parcel _parcel) {				// data in (decoding)
		readFromParcel(_parcel);
	}
	
//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<SourcedObjectType> CREATOR  = new Parcelable.Creator<SourcedObjectType>() {
        public SourcedObjectType createFromParcel(Parcel _parcel) {
            return new SourcedObjectType(_parcel);
        }

        public SourcedObjectType[] newArray(int size) {
            return new SourcedObjectType[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeInt(mWorldObjectType.ordinal());
    	_parcel.writeInt(mDataSource.ordinal());

   }
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	mWorldObjectType = WorldObjectTypes.values()[_parcel.readInt()];
    	mDataSource = Capability.DataSources.values()[_parcel.readInt()];
    }

}

