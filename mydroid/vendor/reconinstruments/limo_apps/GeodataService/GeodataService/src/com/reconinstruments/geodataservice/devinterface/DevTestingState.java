package com.reconinstruments.geodataservice.devinterface;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class DevTestingState implements Parcelable
{
// constants 
	private final static String TAG = "DevTestingState";
	
	public enum TestingConditions {
		ENABLED,								// use TesingConditions
		
		SERVICE_CREATE_ERROR,					// throw no memory exception
		SERVICE_RANDOM_SYSTEM_ERROR,			// throw generic exception
		SERVICE_LOSE_CLIENT_CONNECTION,
		SERVICE_NO_GPS,
		SERVICE_FAKE_GPS,
		SERVICE_NO_USER_HEADING,

		DSM_CREATE_ERROR,						// throw no memory exception
		DSM_RANDOM_SYSTEM_ERROR,				// throw generic exception
		DSM_NOT_RESPONDING,						// block all responses
		DSM_CONFIG_XML_READ_ERROR,				// emulates a read error in dsm_configuration.xml during init()
		
		MD_TRANSCODER_CREATE_ERROR,				// throw no memory exception
		MD_TRANSCODER_RANDOM_SYSTEM_ERROR,		// throw generic exception
		MD_TRANSCODER_NOT_RESPONDING,			// block all responses
		MD_TRANSCODER_SOURCING_DATA_ERROR,
		
		MD_SOURCE_CREATE_ERROR,					// throw no memory exception
		MD_SOURCE_RANDOM_SYSTEM_ERROR,			// throw generic exception
		MD_SOURCE_NOT_RESPONDING,				// block all responses - emulate lost during data retrieval
		MD_SOURCE_RESORTINFO_DB_NO_FILE,
		MD_SOURCE_RESORTINFO_DB_READ_ERROR,
		MD_SOURCE_MDDATA_ZIP_NO_FILE,
		MD_SOURCE_MDDATA_ZIP_READ_ERROR,
		MD_SOURCE_SOURCING_DATA_ERROR,
		
		OSM_TRANSCODER_CREATE_ERROR,				// throw no memory exception
		OSM_TRANSCODER_RANDOM_SYSTEM_ERROR,		// throw generic exception
		OSM_TRANSCODER_NOT_RESPONDING,			// block all responses
		OSM_TRANSCODER_SOURCING_DATA_ERROR,
		
		OSM_SOURCE_CREATE_ERROR,					// throw no memory exception
		OSM_SOURCE_RANDOM_SYSTEM_ERROR,			// throw generic exception
		OSM_SOURCE_NOT_RESPONDING,				// block all responses - emulate lost during data retrieval
		OSM_SOURCE_SOURCING_DATA_ERROR,
		
		MAPDATA_REQUEST_BADLY_FORMED
	}

// members 
	public ArrayList<Integer> mTestingConditionState = null;		// stores all subcomponents that haven't been fully initialized
	
// methods
	public DevTestingState() {
		mTestingConditionState = new ArrayList<Integer>();
		int cnt = 0;
		for(TestingConditions condition : TestingConditions.values()) {
			if(condition == TestingConditions.ENABLED) {
				mTestingConditionState.add(1);	
			}
			else { 
				mTestingConditionState.add(0); // instantiate with 0 state (disabled) for every test condition
			}
		}
	}
    private DevTestingState(Parcel _parcel) {				
    	mTestingConditionState = new ArrayList<Integer>();
		while(_parcel.dataAvail() > 0) {
			mTestingConditionState.add(_parcel.readInt());  // instantiate with parceled state for every test condition
		}
    }
	
//============ parcelable protocol handlers - creator above
	
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<DevTestingState> CREATOR  = new Parcelable.Creator<DevTestingState>() {
        public DevTestingState createFromParcel(Parcel _parcel) {
            return new DevTestingState(_parcel);
        }

        public DevTestingState[] newArray(int size) {
            return new DevTestingState[size];
        }
    };
    
    public void writeToParcel(Parcel _parcel, int flags) {		// data out (encoding)
    	for(Integer val : mTestingConditionState) {
    		_parcel.writeInt(val);
    	}
   }


}
