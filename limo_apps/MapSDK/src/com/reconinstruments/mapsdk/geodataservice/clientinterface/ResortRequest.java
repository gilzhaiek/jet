package com.reconinstruments.mapsdk.geodataservice.clientinterface;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;

public class ResortRequest implements Parcelable {

	public	PointXY				mLocation;
	public	float				mMaxDistanceInKm = 1000000.f;
	public  int					mResultLimit = 30;
	public 	Capability.DataSources mDataSource;

	public ResortRequest(PointXY location, float maxDistanceInKm, int numResultsLimit, Capability.DataSources dataSource) {
		mLocation = location;
		mMaxDistanceInKm = maxDistanceInKm;
		mResultLimit = numResultsLimit;
		mDataSource = dataSource;
	}
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<ResortRequest> CREATOR  = new Parcelable.Creator<ResortRequest>() {
        public ResortRequest createFromParcel(Parcel _parcel) {
            return new ResortRequest(_parcel);
        }

        public ResortRequest[] newArray(int size) {
            return new ResortRequest[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeValue(mLocation);
    	_parcel.writeFloat(mMaxDistanceInKm);
    	_parcel.writeInt(mDataSource.ordinal());
   }

    private ResortRequest(Parcel _parcel) {						// constructor from parcel - data in (decoding)
    	mLocation = (PointXY)_parcel.readValue(getClass().getClassLoader());
    	mMaxDistanceInKm = _parcel.readFloat();
    	mDataSource = Capability.DataSources.values()[_parcel.readInt()];
    }

	@Override
	public int describeContents() {
		return 0;
	}

}
