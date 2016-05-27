package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Road_Residential extends Road  implements Parcelable
{
	private final static String TAG = "Road_Residential";

	public Road_Residential(String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.ROAD_RESIDENTIAL, name, _nodes, dataSource);
	}
	
	public Road_Residential(Parcel _parcel) {					
		super(_parcel);				// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Road_Residential> CREATOR  = new Parcelable.Creator<Road_Residential>() {
        public Road_Residential createFromParcel(Parcel _parcel) {
            return new Road_Residential(_parcel);
        }

        public Road_Residential[] newArray(int size) {
            return new Road_Residential[size];
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
