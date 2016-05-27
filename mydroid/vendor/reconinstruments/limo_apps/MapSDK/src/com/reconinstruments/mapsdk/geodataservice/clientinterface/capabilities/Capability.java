package com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities;

import android.os.Parcel;
import android.os.Parcelable;

public class Capability implements Parcelable
//public class Capability  implements Serializable 
{
// constants	
	public enum CapabilityTypes {
		UNDEFINED,
		STATIC_MAP_DATA,
		DYNAMIC_USER_POSITION,
		DYNAMIC_BUDDY_POSITION,
		INFO_RETRIEVAL,
		ROUTE_FINDING
	}
	
	public enum DataSources {
		MD_SKI,				// mountain dynamics 
		RECON_BASE,			// recon's terrain and street data set (mostly OSM)
		RECON_SNOW,			// recon's ski feature set (from various sources)
		
		DEVICE_USER_POSITION_SENSORS,	// dynamic
		RECON_BUDDY_TRACKING_SERVER,
		RECON_CHAIRLIFT_STATUS_SERVER
	}

// members	
	public CapabilityTypes	mType = CapabilityTypes.UNDEFINED;
	public Boolean			mEnabled;
	
// constructors
	public Capability(CapabilityTypes type) {
		mType = type;
		mEnabled = false;
	}
	
	public Capability(Parcel _parcel) {				
		readFromParcel(_parcel);			// designed to call (Overridden) subclass method of this name
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
    
    // Override these next 2 methods in subclasses - and call super.xxx() from there
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeInt(mType.ordinal());
        _parcel.writeInt(mEnabled ? 1 : 0);
   }
    
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	mType = CapabilityTypes.values()[_parcel.readInt()];
    	mEnabled = (_parcel.readInt() > 0 ? true : false);
    }

 // =========== methods	
 	public void Enable() {
 		mEnabled = true;
 	}
 	
 	public void Disable() {
 		mEnabled = false;
 	}
 	
}
