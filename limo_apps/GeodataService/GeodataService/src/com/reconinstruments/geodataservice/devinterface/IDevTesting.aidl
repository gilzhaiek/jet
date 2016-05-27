package com.reconinstruments.geodataservice.devinterface;

/* Import our Parcelable object types - note the code and aidl definitions need to be shared with client apps*/
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
 
interface IDevTesting {
	boolean setDevTestingState(in DevTestingState _devTestingState);
	DevTestingState getDevTestingState();
}