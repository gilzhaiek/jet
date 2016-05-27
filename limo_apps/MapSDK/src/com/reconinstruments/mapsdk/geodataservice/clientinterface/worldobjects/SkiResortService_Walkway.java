package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class SkiResortService_Walkway extends WO_Polyline  implements Parcelable
{
	private final static String TAG = "SkiResortService_Walkway";

	public SkiResortService_Walkway(String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.SKIRESORTSERVICE_WALKWAY, name, _nodes, dataSource);
	}


	public SkiResortService_Walkway(Parcel _parcel) {				
		super(_parcel);			// first instantiate it 
		readFromParcel(_parcel);	// then load from parcel
    }
	
//============ parcelable protocol handlers

    public static final Parcelable.Creator<SkiResortService_Walkway> CREATOR  = new Parcelable.Creator<SkiResortService_Walkway>() {
        public SkiResortService_Walkway createFromParcel(Parcel _parcel) {
            return new SkiResortService_Walkway(_parcel);
        }

        public SkiResortService_Walkway[] newArray(int size) {
            return new SkiResortService_Walkway[size];
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
