package com.reconinstruments.geodataservice.clientinterface;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.PointXY;

public class ResortRequest implements Parcelable {

	public	PointXY				mLocation;
	public	float				mMaxDistanceInKm = 1000000.f;
	public  int					mResultLimit = 30;
	public 	Capability.DataSources mDataSource;

	public ResortRequest(PointXY location, float maxDistanceInKm, int limit, Capability.DataSources dataSource) {
		mLocation = location;
		mMaxDistanceInKm = maxDistanceInKm;
		mResultLimit = limit;
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
    	_parcel.writeFloat(mLocation.x);
    	_parcel.writeFloat(mLocation.y);
    	_parcel.writeFloat(mMaxDistanceInKm);
    	_parcel.writeInt(mDataSource.ordinal());
   }

    private ResortRequest(Parcel _parcel) {				// constructor from parcel - data in (decoding)
    	mLocation = new PointXY(_parcel.readFloat(), _parcel.readFloat());
    	mMaxDistanceInKm = _parcel.readFloat();
    	mDataSource = Capability.DataSources.values()[_parcel.readInt()];
    }

	@Override
	public int describeContents() {
		return 0;
	}

}
