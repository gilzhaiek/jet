package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class Road_Artery_Secondary extends Road implements Parcelable
{
	private static final long serialVersionUID = 1L;
	private final static String TAG = "Road_Artery_Secondary";

	public Road_Artery_Secondary(String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.ROAD_ARTERY_SECONDARY, name, _nodes, dataSource);
	}
	
	public Road_Artery_Secondary(Parcel _parcel) {			
		super(_parcel);				// first instantiate it 
//		Log.i(TAG, "Create Road_Artery_Secondary");
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<Road_Artery_Secondary> CREATOR  = new Parcelable.Creator<Road_Artery_Secondary>() {
        public Road_Artery_Secondary createFromParcel(Parcel _parcel) {
            return new Road_Artery_Secondary(_parcel);
        }

        public Road_Artery_Secondary[] newArray(int size) {
            return new Road_Artery_Secondary[size];
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
