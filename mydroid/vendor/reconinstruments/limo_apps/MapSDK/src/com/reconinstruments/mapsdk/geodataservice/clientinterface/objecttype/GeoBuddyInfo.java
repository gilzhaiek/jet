package com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype;

import android.os.Parcel;
import android.os.Parcelable;


public class GeoBuddyInfo implements Parcelable{

	public String		mObjectID;		// unique, reproducible ID based on object data - currently assumes service provides unique id while tracking
	public int			mID;
	public String 		mName;
	public double	 	mLatitude;		// in world GPS coords
	public double		mLongitude;
	public boolean		mOnline;

	public GeoBuddyInfo(int id, String name, double latitude, double longitude) {
		mID = id;					// buddy server provided ID
		mName = name;
		mLatitude = latitude;
		mLongitude = longitude;
		mOnline = false;
		mObjectID = "BUDDY" + mID;			// unique, reproducible ID based on object data - currently assumes service provides unique id while tracking
	}
	
    private GeoBuddyInfo(Parcel _parcel) {		// data in (decoding)
    	readFromParcel(_parcel);
    }
	

//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<GeoBuddyInfo> CREATOR  = new Parcelable.Creator<GeoBuddyInfo>() {
        public GeoBuddyInfo createFromParcel(Parcel _parcel) {
            return new GeoBuddyInfo(_parcel);
        }

        public GeoBuddyInfo[] newArray(int size) {
            return new GeoBuddyInfo[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
        _parcel.writeString(mObjectID);
    	_parcel.writeInt(mID);
        _parcel.writeString(mName);
    	_parcel.writeDouble(mLatitude);
    	_parcel.writeDouble(mLongitude);
    	_parcel.writeInt(mOnline == true ? 1 : 0);
    } 
    
    @SuppressWarnings("unchecked")
	protected void readFromParcel(Parcel _parcel)  {				// data in (decoding)
    	mObjectID = _parcel.readString();
    	mID = _parcel.readInt();
    	mName = _parcel.readString();
    	mLatitude = _parcel.readDouble();
    	mLongitude = _parcel.readDouble();
    	mOnline = (_parcel.readInt() == 1 ? true : false);
    }
}
