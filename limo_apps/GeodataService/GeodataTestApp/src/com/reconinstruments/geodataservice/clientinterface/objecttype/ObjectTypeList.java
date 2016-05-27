package com.reconinstruments.geodataservice.clientinterface.objecttype;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class ObjectTypeList implements Parcelable 	// basically a container for an arraylist of DataRetrievalCapability to simplify AIDL definintions
{
	private final static String TAG = "ObjectTypeList";

// members
	public ArrayList<SourcedObjectType> mObjectTypes;	// object types define type and data source, ie a DataRetrievalCapability
	
// constructors
	public ObjectTypeList() {
		mObjectTypes = new ArrayList<SourcedObjectType>();
	}
	public ObjectTypeList(Parcel _parcel) {				// data in (decoding)
		readFromParcel(_parcel);
	}
	
//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ObjectTypeList> CREATOR  = new Parcelable.Creator<ObjectTypeList>() {
        public ObjectTypeList createFromParcel(Parcel _parcel) {
            return new ObjectTypeList(_parcel);
        }

        public ObjectTypeList[] newArray(int size) {
            return new ObjectTypeList[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	_parcel.writeSerializable(mObjectTypes);

   }
    protected void readFromParcel(Parcel _parcel) {				//TODO - if needed, remap older, deprecated values to new values
    	mObjectTypes = (ArrayList<SourcedObjectType>) _parcel.readSerializable();
    }

}
