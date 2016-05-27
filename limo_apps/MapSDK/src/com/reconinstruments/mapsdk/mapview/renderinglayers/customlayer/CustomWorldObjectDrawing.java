package com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer;


public class CustomWorldObjectDrawing  
{
	private final static String TAG = "CustomWorldObjectDrawing";

	public enum CustomObjectTypes {	
		TERRAIN,
		TRAIL,
		POI
	}

	public enum CustomWorldObjectDrawingStates {
		ENABLED,
		DISABLED,
	}
	
	public String				mID;
	public CustomObjectTypes	mType;
	public boolean				mEnabled;
	
//======================================
// constructors

	public CustomWorldObjectDrawing(String _ID, CustomObjectTypes _type) {  
		mID = _ID;
		mType = _type;
		mEnabled = true;
	}

	public void SetEnable(boolean enable) {
		mEnabled = enable;
	}
}
