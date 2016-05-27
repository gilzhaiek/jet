package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class WorldObject implements Serializable 
{
	private final static String TAG = "WorldObject";
	private static final long serialVersionUID = 7506468544349L;
	public final static int OBJECTID_NAME_LENGTH = 10;
	public final static String IDNAME_PADDING = "_______________________________";	// needs to be longer than OBJECTID_NAME_LENGTH
	public final static String UNKNOWN_ITEM_NAME = "__unknown__";

	public enum WorldObjectTypes {	// correspond to WorldObject subtypes
		NOT_DEFINED,
		CHAIRLIFT,
		CHAIRLIFT_ACCESS,
		CARACCESS_PARKING,
		DOWNHILLSKITRAIL_GREEN,
		DOWNHILLSKITRAIL_BLUE,
		DOWNHILLSKITRAIL_BLACK,
		DOWNHILLSKITRAIL_DBLACK,
		DOWNHILLSKITRAIL_RED,
		TERRAIN_SHRUB,
		TERRAIN_WOODS,
		TERRAIN_TUNDRA,
		TERRAIN_PARK,
		TERRAIN_SKIRESORT,
		SKIRESORTSERVICE_INFO,
		SKIRESORTSERVICE_RESTAURANT,
		SKIRESORTSERVICE_WALKWAY,
		TERRAIN_WATER,
		TERRAIN_RESIDENTIAL,
		CARACCESS_HIGHWAY,
		STORE,
		WASHROOM,
		HOSPITAL,
		TERRAIN_LAND,
		TERRAIN_OCEAN,
		TERRAIN_CITYTOWN,
		ROAD_RESIDENTIAL,
		ROAD_ARTERY_PRIMARY,
		ROAD_ARTERY_SECONDARY,
		ROAD_ARTERY_TERTIARY,
		ROAD_FREEWAY,
		NO_DATA_ZONE,
		BUDDY,
		DRINKINGWATER,
		BORDER_NATIONAL,
		WATERWAY
	}

	public enum WorldObjectStates {
		ACTIVE,
		INACTIVE
	}
	
	public String				mObjectID="";		// unique, reproducible ID based on object data
	public WorldObjectTypes		mType;
	public WorldObjectStates 	mState;
	public String				mName;
	public Capability.DataSources mDataSource;
	public Date					mCreatedOn;
	
//======================================
// constructors

	public WorldObject(WorldObjectTypes _type, String name, Capability.DataSources dataSource) {  
		mCreatedOn = new Date();
		mType = _type;
		mName = name;
		mState = WorldObjectStates.ACTIVE;		// always init active (makes obj creation simpler)
		mDataSource = dataSource;
		
	}
	
	public WorldObject(Parcel _parcel) {	
    }
	
//======================================
// methods
	public boolean InGeoRegion(GeoRegion geoRegion) {	
		return false;
	}
	
//============ parcelable protocol handlers
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<WorldObject> CREATOR  = new Parcelable.Creator<WorldObject>() {
        public WorldObject createFromParcel(Parcel _parcel) {
            return new WorldObject(_parcel);
        }

        public WorldObject[] newArray(int size) {
            return new WorldObject[size];
        }
    };
    
    // Override these next 2 methods in subclasses - and call super.xxx() from there
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
//    	_parcel.writeInt(mType.ordinal());
    	_parcel.writeString(mObjectID);
    	_parcel.writeInt(mState.ordinal());
    	_parcel.writeString(mName);
    	_parcel.writeInt(mDataSource.ordinal());
    	_parcel.writeValue(mCreatedOn);
   }
    
    protected void readFromParcel(Parcel _parcel) {				
//   	mType = WorldObjectTypes.values()[_parcel.readInt()];
    	mObjectID = _parcel.readString();
    	mState = WorldObjectStates.values()[_parcel.readInt()];
    	mName = _parcel.readString();
    	mDataSource = Capability.DataSources.values()[_parcel.readInt()];
    	mCreatedOn = (Date) _parcel.readValue(getClass().getClassLoader());
    }
}
