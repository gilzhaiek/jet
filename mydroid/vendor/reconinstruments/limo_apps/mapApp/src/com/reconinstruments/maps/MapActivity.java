package com.reconinstruments.maps;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.mapfragment.MapFragment.IReconMapFragmentCallbacks;
import com.reconinstruments.mapsdk.mapfragment.subclass.MapFragment_Find;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.MapView.IReconMapViewCallbacks;
import com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer.BuddyItem;

import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */

public class MapActivity extends ColumnElementFragmentActivity  implements IReconMapViewCallbacks, IReconMapFragmentCallbacks
{
// constants    
    private static final String TAG = "MapFindBuddyActivity";
    private static int FAST_LOCATION_POST_UPDATE_INTERVAL = 10;
    private static final boolean WAIT_FOR_DEBUGGER = false;
    private static final boolean DISABLE_PITCH_WHILE_MOVING = true;

    // members
    public MapFragment_Find mMapFragment = null;
    private MapView mMap = null;
    // For compass calibration
    private boolean             mIsCalibrationNeeded = true;
    protected boolean			mStatusBarVisible = true;	// always true, but included as an example or for subclassing this activity
    private	Window 				mCurWindow = null;
    private View                mOverlayView = null;
    private FrameLayout         mMainLayout = null;

    boolean						mMapSystemFilesValid = true;
    
// methods    

// activity/fragment life cycle routines
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG,"onCreate "+System.currentTimeMillis());
    	super.onCreate(savedInstanceState);

    	if(WAIT_FOR_DEBUGGER) {
    		android.os.Debug.waitForDebugger();
    	} 

    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	ShowStatusBar(false);

    	setContentView(R.layout.activity_map);          // empty FrameLayout

    	// if we're being restored from a previous state, then don't do anything otherwise we could end up with overlapping fragments.
    	if (savedInstanceState != null) {
    		return;
    	}

    	// each map fragment/view pair requires a unique
    	// identifier to work with the backend GeoDataService. 
    	// Here we have simply use the package name as the id
    	String mapIdentifier = getPackageName(); 

    	try {
    		// create MapFragment - defines map navigation controls available to user 

    		mMapFragment = new MapFragment_Find();
            mMapFragment.setContext(this);
    		mMapFragment.setIdentifier(mapIdentifier);
    		mMapFragment.setPitchDisabled(DISABLE_PITCH_WHILE_MOVING);

    		// create a MapView object - defines type of map presented to the user (default MapView creates a basic street view)
    		mMap = new MapView(this, mapIdentifier);        // this passed as context
    		mMap.SetScreenStatusBarHeight(30);

    		// bind MapView to MapFragment - connect content to controls and sets default MapView config for fragment
    		mMapFragment.BindMap(mMap, false);

    		// configure camera after MapviewCameraIsReady() callback - part of IReconMapViewCallbacks interface
    		// add annotations after MapViewReadyForAnnotation() callback 

    		// as with any fragment, forward the Activity's Intent's extras to the fragment as arguments
    		mMapFragment.setArguments(getIntent().getExtras());             

    		// Add the fragment to the UI frame 
    		getSupportFragmentManager().beginTransaction().add(R.id.map_activity_container, mMapFragment).commit();

    	}
    	catch (Exception e) {
    		Log.e(TAG,"Error instantiating MapFragment or MapView:" + e.getMessage());
    		mMapFragment = null;
    		mMap = null;
    		mMapSystemFilesValid = false;
    	}
    }


    @Override
    public void onResume()
    {
        super.onResume();

        // Request for active GPS.
        Intent intent = new Intent("RECON_ACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);

    	if(mMapSystemFilesValid) {
            mMainLayout = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
            runCalibrationTest();


            HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,FAST_LOCATION_POST_UPDATE_INTERVAL);  //TODO undo comment for ColumnElementFragmentActivity
            
            ShowStatusBar(mStatusBarVisible);	// reset status bar state after resuming
    	} 
    }
    
//  IReconMapFragmentCallbacks-related methods
    
    public void ReconMapFragmentModeChanged(String newMapFragmentMode) {   	// indicates map fragment has changed its mode - often used by activity to hide controls or status bar
    																		// available modes depend on fragment used
    																		// !! this is called on UI thread !!
    	if(newMapFragmentMode.equalsIgnoreCase("Message")) ShowStatusBar(true);		// note currently all modes do the same thing... included as a functional illustration
    	if(newMapFragmentMode.equalsIgnoreCase("LoadMap")) ShowStatusBar(true);
    	if(newMapFragmentMode.equalsIgnoreCase("BaseMap")) ShowStatusBar(true);
 	}
	
    // window control methods
	public void ShowStatusBar(boolean showSB ) {
	    mStatusBarVisible = showSB;
		if (mMapFragment != null)
			mMapFragment.setStatusBarVisible(showSB);
		mCurWindow = getWindow();
		if(showSB) {
			mCurWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			mCurWindow.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			mCurWindow.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			if (mMap != null)
				mMap.SetScreenStatusBarHeight(30);
		}
		else {
			mCurWindow.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			mCurWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			mCurWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			if (mMap != null)
				mMap.SetScreenStatusBarHeight(0);
		}

  } 
	    

// interface IReconMapViewCallbacks 
    public void MapViewReadyToConfigure() {   	// indicates mapview is ready to receive camera configuration         
    } 

    public void MapViewReadyForAnnotation() {   // indicates mapview is ready to receive annotations
        // do further configuration and annotations after this point
    }

    @Override
    public void onPause()   {
        // No longer need GPS. Request for passive.
        Intent intent = new Intent("RECON_DEACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);

        HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,0);   
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
	android.os.Process.killProcess(android.os.Process.myPid());
    }
    

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	if(!mMapSystemFilesValid || mIsCalibrationNeeded) {
	    return super.onKeyDown(keyCode, event);
    	}

    	if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            boolean haveBuddies = mMapFragment.haveAnyBuddies();
            if(haveBuddies) {
                mMapFragment.gotoFindBuddyMode();
                showBuddyListOverlay(null);
                return false;
            }
		}

    	if (mMapFragment.onKeyDown(keyCode, event)) {
    		return true;
    	}
    	return super.onKeyDown(keyCode,event);
    }

    @Override
    public void onBackPressed() {
    	if(!mMapSystemFilesValid || !mMapFragment.onBackPressed()) {
    		super.onBackPressed();
    	}
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if(mMapSystemFilesValid) {
    		if(mIsCalibrationNeeded && keyCode == KeyEvent.KEYCODE_DPAD_CENTER)  {
    			startActivity(new Intent("com.reconinstruments.compass.CALIBRATE"));
    			return true;
    		}
    		if (mMapFragment != null && mMapFragment.onKeyUp(keyCode, event)) {
    			return true;
    		}
    	}
        return super.onKeyUp(keyCode,event);
    }



    private void addCalibrationOverlay() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mOverlayView = inflater.inflate(R.layout.calib_please_layout, null);
        mMainLayout.addView(mOverlayView, new LinearLayout.LayoutParams(mMainLayout.getLayoutParams().width, mMainLayout.getLayoutParams().height));
    }
        
    private void removeCalibrationOverlay() {
        if(mOverlayView != null){
            mMainLayout.removeView(mOverlayView);
            mOverlayView = null;
        }
    }

    private void runCalibrationTest() {
    	if(mMapSystemFilesValid) {
    		// Launch calibration if it hasn't been done before
    		removeCalibrationOverlay();
    		// TODO change to V2 for release
    		mIsCalibrationNeeded = Settings.Secure.getInt(getContentResolver(), "hasWrittenMagOffsetsV2", 0)!=1;
    		boolean mIsCalibrationNeeded2 = Settings.Secure.getInt(getContentResolver(), "hasWrittenMagOffsets", 0)!=1;
    		mIsCalibrationNeeded = (mIsCalibrationNeeded  && mIsCalibrationNeeded2);

    		if(mIsCalibrationNeeded) { // hack
    			Log.v(TAG, "adding Calibration overlay");
    			addCalibrationOverlay();
    		} else {
    		}
    	}
    }
    
    /**
     * Shows an overlay of currently available buddies.
     * @param inputList Optional input list of buddies. Input <code>null</code> to fetch the most up-to-date list
     *                  of buddies.
     */
	public void showBuddyListOverlay(ArrayList<BuddyItem> inputList) {
		Log.v(TAG, "showBuddyListOverlay");

		ArrayList<BuddyItem> buddyList;
        if(inputList != null) buddyList = inputList;
        else buddyList = mMapFragment.getSortedBuddyList();

		List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();

		int i = 0;
		for (BuddyItem buddy : buddyList) {
			list.add(new FindBuddyFragment(R.layout.find_buddy_item, buddy.mName, 0, i));
			Log.v(TAG, "-i=" + i + ", name=" + buddy.mName);
			i++;
		}

		android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
		Fragment frg = fm.findFragmentByTag("findBuddy");
		if (frg == null) {
			ShowStatusBar(false);
			FindBuddyOverlay overlay = new FindBuddyOverlay("", list, R.layout.carousel_with_text_findbuddy, this);
			overlay.show(fm.beginTransaction(), "findBuddy");
		}

	}

	public void showBuddyInMap(int buddyIndex) {
		GeoRegion geoRegion = (mMapFragment).getGeoRegionContainBuddy(buddyIndex);
		if (geoRegion == null)
			return;

		float percentWidthMargin = .47f;
		boolean immediate = true;
		mMap.SetCameraToShowGeoRegion(geoRegion, percentWidthMargin, immediate);

	}

}
