package com.reconinstruments.mapsdk.geodataservice.clientinterface;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class GeoDataServiceState implements Parcelable
{
// constants
	private final static String TAG = "GeoDataServiceState";
	public enum ServiceStates {
		RESPONSE_UNINITIALIZED,
		SERVICE_STARTING,
		SERVICE_INITIALIZING,
		ERROR_DURING_SERVICE_INITIALIZATION,
		SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA,    	 // have gps fix but data source is loading cache				
		SERVICE_READY_WITH_USER_LOCATION,    	 // have gps fix and data source in geodataservice preloaded based on gps fix				
		SERVICE_READY_WITH_NO_USER_LOCATION,	 // never had gps fix
		SERVICE_READY_WITH_STALE_USER_LOCATION,  // gps fix lost
		SERVICE_SHUTTING_DOWN,
		ERROR_WITH_SERVICE,
		SERVICE_NOT_READY
	}

	
// members
	public ServiceStates			mState = ServiceStates.RESPONSE_UNINITIALIZED;
	public ArrayList<Capability> 	mCapabilities = null;

// constructors
	public GeoDataServiceState() {
		mState = ServiceStates.RESPONSE_UNINITIALIZED;
		mCapabilities = new ArrayList<Capability>();
	}

	public GeoDataServiceState(Parcel _parcel) {				// data in (decoding)
		mCapabilities = new ArrayList<Capability>();
    	readFromParcel(_parcel);
    }

// methods
	public void SetState(ServiceStates newState) {
		mState = newState;
	}
	
	public ServiceStates GetState() {
		return mState;
	}

	public void AddCapability(Capability cap) {
		mCapabilities.add(cap);
	}
	
//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<GeoDataServiceState> CREATOR  = new Parcelable.Creator<GeoDataServiceState>() {
        public GeoDataServiceState createFromParcel(Parcel _parcel) {
            return new GeoDataServiceState(_parcel);
        }

        public GeoDataServiceState[] newArray(int size) {
            return new GeoDataServiceState[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeInt(mState.ordinal());
    	_parcel.writeList(mCapabilities);
   }

    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	mState = ServiceStates.values()[_parcel.readInt()];
    	_parcel.readList(mCapabilities, getClass().getClassLoader());
    }
}
