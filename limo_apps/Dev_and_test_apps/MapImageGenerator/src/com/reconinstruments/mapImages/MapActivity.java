package com.reconinstruments.mapImages;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
//import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;

import com.reconinstruments.mapImages.util.SystemUiHider;
import com.reconinstruments.mapImages.R;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */

//public class MapActivity extends ColumnElementFragmentActivity implements HeadingListener
//{
public class MapActivity extends FragmentActivity
//extends ColumnElementFragmentActivity 
{
	private static final String TAG = "MapActivity";
        private static int FAST_LOCATION_POST_UPDATE_INTERVAL = 10;

	private ReconMapFragment mMapFragment = null;

    // For compass calibration
    private boolean isCalibrationNeeded = true;
    private View overlayView = null;
    private FrameLayout mainLayout = null;


    
	// activity/fragment life cycle routines
  	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG,"onCreate "+System.currentTimeMillis());
		super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_map);
		
        // if we're being restored from a previous state, then don't do anything otherwise we could end up with overlapping fragments.
		if (savedInstanceState != null) {
			return;
		}
		
		mMapFragment = new ReconMapFragment();				   	// Create an instance of ReconMapFragment
		mMapFragment.init(2, 2000);  							// ask for 2D map that shows 2000m across
		
		mMapFragment.setArguments(getIntent().getExtras());		// pass the Activity's Intent's extras to the fragment as arguments
																// Add the fragment to the Fragment Manager
		
//		Log.e(TAG, "frag: "+mMapFragment);
		getSupportFragmentManager().beginTransaction().add(R.id.map_activity_container, mMapFragment).commit();


  	}

	@Override
	public void onResume()
	{
	    super.onResume();

//		mMapFragment.SetMapCenter(50.085702, -122.963705);		// could push this if desired... but it would turn off user tracking
																// for ref, this data point is on Whistler Mtn, Raven's Nest
		Log.i(TAG,"onResume "+System.currentTimeMillis());

		// 
		mainLayout = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
		runCalibrationTest();
		// Notify that need faster updates
		HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,FAST_LOCATION_POST_UPDATE_INTERVAL);


	} 
	
	@Override
	public void onPause()  
	{
		Log.d(TAG,"onPause");
		//HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,0);
		super.onPause();
	}

	@Override
	public void onDestroy(){
		Log.d(TAG,"onDestroy");
		super.onDestroy();
	}
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
	    //if (mMapFragment.mMapMode != ReconMapFragment.MapMode.EXPLORE &&
		//keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
		Log.v(TAG,"onKeyDown.");
		super.onKeyDown(keyCode,event);
	    //}
	    //else {
		mMapFragment.onKeyDown(keyCode, event);
	    //}
		switch(keyCode) {
		case KeyEvent.KEYCODE_BACK:
		    onBackPressed();
		}
	    return true;

	}


    @Override
    public void onBackPressed() {
	//goBack();
    finish();
    }

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		Log.v(TAG,"onKeyup.");
	    return mMapFragment.onKeyUp(keyCode, event);
	}



    private void addCalibrationOverlay() {
	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
	overlayView = inflater.inflate(R.layout.calib_please_layout, null);
	mainLayout.addView(overlayView, new LinearLayout.LayoutParams(mainLayout.getLayoutParams().width, mainLayout.getLayoutParams().height));
    }
	
    private void removeCalibrationOverlay() {
	if(overlayView != null){
	    mainLayout.removeView(overlayView);
	    overlayView = null;
	}
    }

    private void runCalibrationTest() {
	// Launch calibration if it hasn't been done before
	removeCalibrationOverlay();
	isCalibrationNeeded = Settings.Secure.getInt(getContentResolver(), "hasWrittenMagOffsets", 0)!=1;

	//isCalibrationNeeded = true;// testing
	if(isCalibrationNeeded) { 
	    Log.v(TAG, "addCalibration");
	    addCalibrationOverlay();
	}

    }

}
