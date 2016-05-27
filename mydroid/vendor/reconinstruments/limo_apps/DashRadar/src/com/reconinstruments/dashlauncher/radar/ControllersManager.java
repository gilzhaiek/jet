package com.reconinstruments.dashlauncher.radar;

import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;

import android.app.Activity;

public class ControllersManager {
	public static final int 		COMPASS_CONTROLLER	= 0;
	public static final int 		RADAR_CONTROLLER	= 1;
	public static final int 		CONTROLLERS_SIZE	= 2;
	
	private static final int		DEFAULT_RESORT_CONTROLLER		= RADAR_CONTROLLER;
	private static final int		DEFAULT_NO_RESORT_CONTROLLER	= RADAR_CONTROLLER;//COMPASS_CONTROLLER;
	
	private BaseControllerHelper[]	mControllers = null;
	private boolean					mResortExists = false;
	private Activity				mActivity = null;
	private LocationTransformer		mLocationTransformer = null;
	
	public ControllersManager(Activity activity, LocationTransformer locationTransformer){
		mActivity = activity;
		
		mLocationTransformer = locationTransformer;
		mControllers = new BaseControllerHelper[CONTROLLERS_SIZE];
		for(int i = 0; i < CONTROLLERS_SIZE; i++) {
			mControllers[i] = null;
		}
	}	
	
	public BaseControllerHelper GetController(int controllerIdx) {
		if(mControllers[controllerIdx] == null)
		{
			switch (controllerIdx) {
			case COMPASS_CONTROLLER:
				mControllers[controllerIdx] = new CompassViewControllerHelper(mActivity, mLocationTransformer);
				break;
			case RADAR_CONTROLLER:
				mControllers[controllerIdx] = new RadarViewControllerHelper(mActivity, mLocationTransformer);
				break;
			default:
				break;
			}
		}
		
		return mControllers[controllerIdx];
	}
	
	public BaseControllerHelper GetDefaultController(){
		if(mResortExists) {
			return GetController(DEFAULT_RESORT_CONTROLLER);
		}
		else {
			return GetController(DEFAULT_NO_RESORT_CONTROLLER);
		}
	}

	public BaseControllerHelper GetExtendedView(){
		return GetController(COMPASS_CONTROLLER);
	}

	public void SetResortExists (boolean resortExists) {
		mResortExists = resortExists;
	}	
}
