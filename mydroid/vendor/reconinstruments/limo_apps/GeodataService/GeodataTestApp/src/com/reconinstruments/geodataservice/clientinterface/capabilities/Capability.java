package com.reconinstruments.geodataservice.clientinterface.capabilities;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Capability  implements Serializable 
{
// constants	
	public enum CapabilityTypes {
		UNDEFINED,
		DYNAMIC_USER_POSITION,
		DYNAMIC_BUDDY_POSITION,
		STATIC_MAP_DATA,
		INFO_RETRIEVAL,
		ROUTE_FINDING
	}

// members	
	public CapabilityTypes	mType = CapabilityTypes.UNDEFINED;
	Boolean			mEnabled;
	
// constructors
	public Capability(CapabilityTypes type) {
		mType = type;
		mEnabled = false;
	}
	
	public Capability(Parcel _parcel) {				// data in (decoding)
		readFromParcel(_parcel);
	}

// methods	
	public void Enable() {
		mEnabled = true;
	}
	
	public void Disable() {
		mEnabled = false;
	}
	
//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Capability> CREATOR  = new Parcelable.Creator<Capability>() {
        public Capability createFromParcel(Parcel _parcel) {
            return new Capability(_parcel);
        }

        public Capability[] newArray(int size) {
            return new Capability[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeInt(mType.ordinal());
        _parcel.writeInt(mEnabled ? 1 : 0);
   }
    
    protected void readFromParcel(Parcel _parcel) {				//TODO - if needed, remap older, deprecated values to new values
    	mType = CapabilityTypes.values()[_parcel.readInt()];
    	mEnabled = (_parcel.readInt() > 0 ? true : false);
    }

}
