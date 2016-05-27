package com.reconinstruments.geodataservice.clientinterface;

import java.io.Serializable;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject;

public class IGeodataServiceResponse implements Parcelable  
{
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
		LOADDATAREQUEST_DATA_LOAD_ERROR
	}

	public enum ResponseDataClassIDs {
		NO_DATA,
		SERIALIZABLE_DATA
	}
// members
	public ResponseCodes		mResponseCode = ResponseCodes.RESPONSE_UNINITIALIZED;
	public String 				mErrorMessage;
	public ResponseDataClassIDs mDataClassId;
	public Serializable			mData; 
	
	
// constructors
	public IGeodataServiceResponse() {
		mResponseCode = ResponseCodes.RESPONSE_UNINITIALIZED;
		mErrorMessage = "";
		mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;
		mData = "";
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
	    	case SERIALIZABLE_DATA: {
	    		_parcel.writeSerializable(mData);
	    		break;
	    	}
    	}
    } 
    
    protected void readFromParcel(Parcel _parcel) {				//TODO - if needed, remap older, deprecated values to new values
    	mResponseCode = ResponseCodes.values()[_parcel.readInt()];
    	mErrorMessage = _parcel.readString();
    	mDataClassId = ResponseDataClassIDs.values()[_parcel.readInt()];
    	switch(mDataClassId) {
	    	case NO_DATA: {
	    		break;
	    	}
	    	case SERIALIZABLE_DATA: {
	    		mData = _parcel.readSerializable();
	    		break;
	    	}
    	}
//    	dataParcel = new ???
//    			_parcel.readParcelable(loader); //_parcel.readList(); //TODO fix this 
    }

	
}
