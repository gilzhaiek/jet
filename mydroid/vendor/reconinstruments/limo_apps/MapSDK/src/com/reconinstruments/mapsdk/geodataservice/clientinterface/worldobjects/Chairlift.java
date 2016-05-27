package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Chairlift extends WO_Polyline  implements Parcelable 
{
	private final static String TAG = "Chairlift";
	
	public enum BottomOfLift {
		IS_FIRST_POINT,
		IS_LAST_POINT
	}

	BottomOfLift	mLiftStationPoint = BottomOfLift.IS_FIRST_POINT;

	public Chairlift(String name, ArrayList<PointXY> _nodes, BottomOfLift _isFirstLastPoint, Capability.DataSources dataSource) {
		super(WorldObjectTypes.CHAIRLIFT, name, _nodes, dataSource);
		mLiftStationPoint = _isFirstLastPoint;
	}
	
	public Chairlift(Parcel _parcel) {					
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Chairlift> CREATOR  = new Parcelable.Creator<Chairlift>() {
        public Chairlift createFromParcel(Parcel _parcel) {
            return new Chairlift(_parcel);
        }

        public Chairlift[] newArray(int size) {
            return new Chairlift[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
//    	int originalSize = _parcel.dataSize();
		super.writeToParcel(_parcel, flags);					// write all super member data to parcel first
    	_parcel.writeInt(mLiftStationPoint.ordinal());
//    	Log.d(TAG, "Chairlift subtype parcel size=" + (_parcel.dataSize()-originalSize));
   }
    
    @Override
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	super.readFromParcel(_parcel);
    	mLiftStationPoint = BottomOfLift.values()[_parcel.readInt()];
    }	
	
}
