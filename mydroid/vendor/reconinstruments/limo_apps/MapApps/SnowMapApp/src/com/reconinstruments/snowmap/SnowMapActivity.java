package com.reconinstruments.snowmap;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.reconinstruments.snowmap.R;
import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapfragment.subclass.MapFragment_Explore;
import com.reconinstruments.mapsdk.mapview.subclass.SnowMapView;
import com.reconinstruments.mapsdk.mapview.MapView.IReconMapViewCallbacks;
import com.reconinstruments.mapsdk.mapfragment.MapFragment.IReconMapFragmentCallbacks;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomAnnotationCache;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */

public class SnowMapActivity extends ColumnElementFragmentActivity  implements IReconMapViewCallbacks, IReconMapFragmentCallbacks
{
// constants    
    private static final String TAG = "MapActivity";
    private static int FAST_LOCATION_POST_UPDATE_INTERVAL = 10;
    private static final boolean WAIT_FOR_DEBUGGER = false;

    // members
    private MapFragment_Explore mMapFragment = null;
    private SnowMapView mMap = null;
    // For compass calibration
    private boolean             mIsCalibrationNeeded = true;
    private View                mOverlayView = null;
    private	Window 				mCurWindow = null;
    private FrameLayout         mMainLayout = null;
    protected boolean			mStatusBarVisible = true;	// always true, but included as an example or for subclassing this activity

    boolean						mMapSystemFilesValid = true;
    
// methods    

// activity/fragment life cycle routines
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG,"-------- onCreate --------"+System.currentTimeMillis());
    	super.onCreate(savedInstanceState);

    	if(WAIT_FOR_DEBUGGER) {
    		android.os.Debug.waitForDebugger();
    	} 

    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	ShowStatusBar(true);

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
    		mMapFragment = new MapFragment_Explore();        
    		mMapFragment.setIdentifier(mapIdentifier);

    		// create a MapView object - defines type of map presented to the user (default MapView creates a basic street view)
    		mMap = new SnowMapView(this, mapIdentifier, null);        // this passed as context

    		// bind MapView to MapFragment - connect content to controls and sets default MapView config for fragment
    		mMapFragment.BindMap(mMap, false);

    		// configure camera after MapviewCameraIsReady() callback - part of IMapViewCallbacks interface
    		// add annotations after MapViewReadyForAnnotation() callback 

    		// as with any fragment, forward the Activity's Intent's extras to the fragment as arguments
    		mMapFragment.setArguments(getIntent().getExtras());             

    		// Add the fragment to the UI frame 
    		getSupportFragmentManager().beginTransaction().add(R.id.map_activity_container, mMapFragment).commit();

    	}
    	catch (Exception e) {
    		mMapFragment = null;
    		mMap = null;
    		mMapSystemFilesValid = false;
    		Log.e(TAG,"Error instantiating MapFragment or MapView:" + mMapSystemFilesValid + ": " + e.getMessage());
//    		finish();
    	}
    }


    @Override
    public void onResume()
    {
    	Log.i(TAG, "-------- onResume --------");
    	super.onResume();

        // Request for active GPS.
        Intent intent = new Intent("RECON_ACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);

    	if(mMapSystemFilesValid) {
    		mMainLayout = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
    		runCalibrationTest();

    		HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,FAST_LOCATION_POST_UPDATE_INTERVAL);  //TODO undo comment for ColumnElementFragmentActivity
    	}
        ShowStatusBar(mStatusBarVisible);	// reset status bar state after resuming

    } 

//  IReconMapFragmentCallbacks-related methods
    
    public void ReconMapFragmentModeChanged(String newMapFragmentMode) {   	// indicates map fragment has changed its mode - often used by activity to hide controls or status bar
    																		// available modes depend on fragment used
																			// !! this is called on UI thread !!
//    	Log.e(TAG,"New Fragment mode: " + newMapFragmentMode);
    	if(newMapFragmentMode.equalsIgnoreCase("Message")) ShowStatusBar(true);		
    	if(newMapFragmentMode.equalsIgnoreCase("LoadMap")) ShowStatusBar(true);
    	if(newMapFragmentMode.equalsIgnoreCase("BaseMap")) ShowStatusBar(true);
	  	if(newMapFragmentMode.equalsIgnoreCase("Explore")) ShowStatusBar(false);
	}

    // window control methods
    public void ShowStatusBar(boolean showSB ) {
    	mStatusBarVisible = showSB;
    	mCurWindow = getWindow();
    	if(showSB) {
    		mCurWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    		mCurWindow.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    		mCurWindow.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    	}
    	else {
    		mCurWindow.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    		mCurWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    		mCurWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	}
    } 
	    
//interface IReconMapViewCallbacks 
    public void MapViewReadyToConfigure() {   // indicates mapview is ready to receive camera configuration         
    } 

    public void MapViewReadyForAnnotation() {   // indicates mapview is ready to receive annotations
    	// do further configuration and annotations after this point            
    }

    @Override
    public void onPause()  
    {
    	//Log.i(TAG,"-------- onPause --------");
    	//              HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,0);   //TODO undo comment for ColumnElementFragmentActivity
    	super.onPause();
        Intent intent = new Intent("RECON_DEACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);
    }
    
    @Override
    public void onStop(){
    	//Log.i(TAG, "-------- onStop --------");
    	super.onStop();
    }

    @Override
    public void onDestroy(){
    	Log.i(TAG,"-------- onDestroy --------");
    	super.onDestroy();
    	
    	// make sure this app is really killed
    	android.os.Process.killProcess(android.os.Process.myPid());
    }
    

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	if(!mMapSystemFilesValid || mIsCalibrationNeeded) {
	    return super.onKeyDown(keyCode, event);
    	}

    	if (mMapFragment != null && mMapFragment.onKeyDown(keyCode, event)) {
    		return true;
    	}
    	return super.onKeyDown(keyCode,event);
    }

    @Override
    public void onBackPressed() {
    	if(!mMapSystemFilesValid || mMapFragment != null && !mMapFragment.onBackPressed()) {
    		super.onBackPressed();
    	}
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)  {
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
    	// Launch calibration if it hasn't been done before
    	if(mMapSystemFilesValid) {
    		removeCalibrationOverlay();
    		// TODO change to V2 for release
    		mIsCalibrationNeeded = Settings.Secure.getInt(getContentResolver(), "hasWrittenMagOffsetsV2", 0)!=1;
    		boolean mIsCalibrationNeeded2 = Settings.Secure.getInt(getContentResolver(), "hasWrittenMagOffsets", 0)!=1;
    		mIsCalibrationNeeded = (mIsCalibrationNeeded  && mIsCalibrationNeeded2);

    		if(mIsCalibrationNeeded) { // hack
    			Log.v(TAG, "adding Calibration overlay");
    			addCalibrationOverlay();
    		} else {
    			//                  Log.v(TAG,"didn't calibrate");
    		}
    	}
    }

}
