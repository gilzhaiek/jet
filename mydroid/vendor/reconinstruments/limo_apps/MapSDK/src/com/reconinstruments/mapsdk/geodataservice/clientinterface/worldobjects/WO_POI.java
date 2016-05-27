package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class WO_POI extends WorldObject  implements Parcelable
{
	private final static String TAG = "POI";

// members
	public PointXY		mGPSLocation = new PointXY(0,0);
	
// constructors
	public WO_POI(WorldObjectTypes _type, String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(_type, name, dataSource);
		mGPSLocation = _gpsLocation;
		
		SetObjectID();
	}

	public WO_POI(Parcel _parcel) {			
		super(_parcel);			// calls (overridden) readFromParcel(_parcel);
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<WO_POI> CREATOR  = new Parcelable.Creator<WO_POI>() {
        public WO_POI createFromParcel(Parcel _parcel) {
            return new WO_POI(_parcel);
        }

        public WO_POI[] newArray(int size) {
            return new WO_POI[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
		super.writeToParcel(_parcel, flags);					// write all super member data to parcel first
//    	_parcel.writeValue(mGPSLocation);

		_parcel.writeFloat(mGPSLocation.x);
    	_parcel.writeFloat(mGPSLocation.y);
   }
    
    @Override
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	super.readFromParcel(_parcel);
//    	mGPSLocation = (PointXY) _parcel.readValue(getClass().getClassLoader());

    	mGPSLocation = new PointXY(_parcel.readFloat(), _parcel.readFloat());
    }
    
//======================================
// methods
	@Override
	public boolean InGeoRegion(GeoRegion geoRegion) {
		return geoRegion.mBoundingBox.Contains(mGPSLocation.x, mGPSLocation.y);
	}
	
	public void SetObjectID() {	// virtual to be overwritten
		mObjectID = String.format("P%d_%s%011d%011d", mType.ordinal(), (mName+WorldObject.IDNAME_PADDING).substring(0, WorldObject.OBJECTID_NAME_LENGTH), (int)(mGPSLocation.x *1000000), (int)(mGPSLocation.y *1000000));
	}
	
}
