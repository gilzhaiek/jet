package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Border_National extends Road implements Parcelable
{
	private final static String TAG = "Border_National";

	public Border_National(String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.BORDER_NATIONAL, name, _nodes, dataSource);
	}
	
	public Border_National(Parcel _parcel) {					
		super(_parcel);				// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Border_National> CREATOR  = new Parcelable.Creator<Border_National>() {
        public Border_National createFromParcel(Parcel _parcel) {
            return new Border_National(_parcel);
        }

        public Border_National[] newArray(int size) {
            return new Border_National[size];
        }
    };
    
    @Override
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
		super.writeToParcel(_parcel, flags);					// write all super member data to parcel first
   }
    
    @Override
    protected void readFromParcel(Parcel _parcel) {				// data in (decoding)
    	super.readFromParcel(_parcel);
    }	
	
}
