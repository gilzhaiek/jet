package com.reconinstruments.mapsdk.geodataservice.clientinterface;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.GeoBuddyInfo;

public class IGeodataServiceResponse implements Parcelable  
{
	private final static String TAG = "IGeodataServiceResponse";
// constants
	public enum ResponseCodes {
		RESPONSE_UNINITIALIZED,
		SERVICE_NOT_READY,
		ERROR_WITH_SERVICE,
		CAPABILITY_NOT_AVAILABLE,
		REQUEST_COMPLETED,
		ERROR_WITH_REQUEST,
		ERROR_DURING_REQUEST,
		BUDDYREQUEST_BUDDIES_ATTACHED,
		BUDDYREQUEST_NO_CONNECTION,
		LOADDATAREQUEST_DATA_LOADING,
		LOADDATAREQUEST_DATA_ATTACHED,
		LOADDATAREQUEST_DATA_NOW_AVAILABLE,
		LOADDATAREQUEST_DATA_LOAD_ERROR,
		TILE_LOADING_FROM_NETWORK_TRY_LATER
	}

	public enum ResponseDataClassIDs {
		NO_DATA,
		STRING,
		SERVICE_STATE,
		ARRAY_RESORTINFO,
		ARRAY_BUDDYINFO,
		GEOTILE
	}
// members
	public ResponseCodes		mResponseCode = ResponseCodes.RESPONSE_UNINITIALIZED;
	public String 				mErrorMessage;
	public ResponseDataClassIDs mDataClassId;
	// returned data types - only one is used per response
	public String				mStringData; 
	public GeoDataServiceState	mServiceState; 
	public GeoTile				mGeoTile;	
	public ArrayList<ResortInfoResponse> mResortsArray;
	public ArrayList<GeoBuddyInfo> mBuddyArray;
		
// constructors
	public IGeodataServiceResponse() {
		mResponseCode = ResponseCodes.RESPONSE_UNINITIALIZED;
		mErrorMessage = "";
		mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;
		mStringData = "";
		mServiceState = null;
		mGeoTile = null;
		mResortsArray = null;
	}
	
    private IGeodataServiceResponse(Parcel _parcel) {				// data in (decoding)
    	readFromParcel(_parcel);
    }
	
//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<IGeodataServiceResponse> CREATOR  = new Parcelable.Creator<IGeodataServiceResponse>() {
        public IGeodataServiceResponse createFromParcel(Parcel _parcel) {
            return new IGeodataServiceResponse(_parcel);
        }

        public IGeodataServiceResponse[] newArray(int size) {
            return new IGeodataServiceResponse[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeInt(mResponseCode.ordinal());
        _parcel.writeString(mErrorMessage);
    	_parcel.writeInt(mDataClassId.ordinal());
    	switch(mDataClassId) {
	    	case NO_DATA: {
	    		break;
	    	}
	    	case STRING: {
	            _parcel.writeString(mStringData);
	    		break;
	    	}
	    	case SERVICE_STATE: {
	    		_parcel.writeValue(mServiceState);
	    		break;
	    	}
	    	case ARRAY_RESORTINFO: {
	    		_parcel.writeList(mResortsArray);			// TODO make into TypedList for space efficiency as the contents are all the same type
	    		break;
	    	}
	    	case ARRAY_BUDDYINFO: {
	    		_parcel.writeList(mBuddyArray);				// TODO make into TypedList for space efficiency as the contents are all the same type
	    		break;
	    	}
	    	case GEOTILE: {
	    		mGeoTile.writeToParcel(_parcel, 0);			// write out geotile... call writeToParcel directly to avoid writing class info in order to make parcel as small as possible
	    		break;
	    	}
    	}
//    	Log.d(TAG, "parcel size=" + _parcel.dataSize() + ", capacity=" + _parcel.dataCapacity());
    } 
    
    @SuppressWarnings("unchecked")
	protected void readFromParcel(Parcel _parcel)  {				// data in (decoding)
    	mResponseCode = ResponseCodes.values()[_parcel.readInt()];
    	mErrorMessage = _parcel.readString();
    	mDataClassId = ResponseDataClassIDs.values()[_parcel.readInt()];
    	switch(mDataClassId) {
	    	case NO_DATA: {
	    		break;
	    	}
	    	case STRING: {
	        	mStringData = _parcel.readString();
	    		break;
	    	}
	    	case SERVICE_STATE: {
	    		mServiceState = (GeoDataServiceState) _parcel.readValue(getClass().getClassLoader());
	    		break;
	    	}
	    	case ARRAY_RESORTINFO: {
	    		mResortsArray = new ArrayList<ResortInfoResponse>();
	    		_parcel.readList(mResortsArray, getClass().getClassLoader());
	    		break;
	    	}
	    	case ARRAY_BUDDYINFO: {
	    		mBuddyArray = new ArrayList<GeoBuddyInfo>();
	    		_parcel.readList(mBuddyArray, getClass().getClassLoader());
	    		break;
	    	}
	    	case GEOTILE: {
				mGeoTile = (GeoTile.CREATOR.createFromParcel(_parcel));	// don't save class info to make response as small as possible
 	    		break;
	    	}	// end case
    	} // end  switch
    }

	
}
