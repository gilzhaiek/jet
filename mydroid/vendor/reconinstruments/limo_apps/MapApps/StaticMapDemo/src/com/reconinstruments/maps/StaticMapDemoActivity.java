package com.reconinstruments.maps;

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
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.reconinstruments.maps.R;
import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
//import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapfragment.MapFragment;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.MapView.IMapViewCallbacks;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomAnnotationCache;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */

public class StaticMapDemoActivity extends ColumnElementFragmentActivity  implements IMapViewCallbacks
{
// constants    
    private static final String TAG = "MapActivity";
    private static int FAST_LOCATION_POST_UPDATE_INTERVAL = 10;
    private static final boolean WAIT_FOR_DEBUGGER = false;

    // members
    private MapFragment mMapFragment = null;
    private MapView mMap = null;
    // For compass calibration
    private boolean             mIsCalibrationNeeded = true;
    private View                        mOverlayView = null;
    private FrameLayout         mMainLayout = null;

    
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
                mMapFragment = new MapFragment(mapIdentifier);        
        
                // create a MapView object - defines type of map presented to the user (default MapView creates a basic street view)
                mMap = new MapView(this, mapIdentifier,null);        // this passed as context

                // bind MapView to MapFragment - connect content to controls and sets default MapView config for fragment
                mMapFragment.BindMap(mMap);
                
                // configure camera after MapviewCameraIsReady() callback - part of IMapViewCallbacks interface
                // add annotations after MapViewReadyForAnnotation() callback 

                // as with any fragment, forward the Activity's Intent's extras to the fragment as arguments
                mMapFragment.setArguments(getIntent().getExtras());             
                    
                // Add the fragment to the UI frame 
                getSupportFragmentManager().beginTransaction().add(R.id.map_activity_container, mMapFragment).commit();

        }
        catch (Exception e) {
                Log.e(TAG,"Error instantiating MapFragment or MapView:" + e.getMessage());
                finish();
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();

        mMainLayout = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
        runCalibrationTest();


        HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,FAST_LOCATION_POST_UPDATE_INTERVAL);  //TODO undo comment for ColumnElementFragmentActivity
    } 

// interface IMapViewCallbacks 
    public void MapViewReadyToConfigure() {   // indicates mapview is ready to receive camera configuration         
//        GeoRegion newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(-123.2088f, 49.3250f, -123.0321f, 49.2435f );
        GeoRegion newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(-123.1588f, 49.3000f, -123.1021f, 49.2685f );
        mMap.SetCameraToShowGeoRegion(newGeoRegion, 0.0f, true);
    } 

    public void MapViewReadyForAnnotation() {   // indicates mapview is ready to receive annotations
       // do further configuration and annotations after this point            
        
		// example annotations
	
        CustomAnnotationCache.AnnotationErrorCode errorCode;
        int id = R.drawable.info_icon_3; 
        Bitmap image = BitmapFactory.decodeResource(getResources(), id);
        PointXY poiLocation = new PointXY(-123.122886f, 49.279793f);
        errorCode = mMap.AddPointAnnotation("TestPoint", poiLocation, image, 255) ;
        id = R.drawable.lift_icon_3; 
        image = BitmapFactory.decodeResource(getResources(), id);
        poiLocation = new PointXY(-123.121686f, 49.272793f);
        errorCode = mMap.AddPointAnnotation("BankPoint", poiLocation, image, 255) ;
        
        ArrayList<PointXY> nodes = new ArrayList<PointXY>();
        nodes.add(new PointXY(-123.131186f, 49.281103f));
        nodes.add(new PointXY(-123.111186f, 49.277793f));
        nodes.add(new PointXY(-123.121186f, 49.274793f));
        errorCode = mMap.AddOverlayAnnotation("TestOverlay", nodes, 0xdddddd,  128);
        errorCode = mMap.AddLineAnnotation("TestLine", nodes, 20.f, 0x40c4c3,  128);

        nodes.clear();
        nodes.add(new PointXY(-123.121186f, 49.274793f));
        nodes.add(new PointXY(-123.131186f, 49.281103f));
        errorCode = mMap.AddLineAnnotation("RedLine", nodes, 20.f, 0xc43030,  128);

    }

    @Override
    public void onPause()  
    {
        Log.d(TAG,"onPause");
//              HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,0);   //TODO undo comment for ColumnElementFragmentActivity
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
        if (mMapFragment.onKeyDown(keyCode, event)) {
            return true;
        }
            return super.onKeyDown(keyCode,event);
    }


    @Override
    public void onBackPressed() {
        if(!mMapFragment.onBackPressed()) {
        	goBack();             
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
    if(mIsCalibrationNeeded && keyCode == KeyEvent.KEYCODE_DPAD_CENTER)  {
    	startActivity(new Intent("com.reconinstruments.compass.CALIBRATE"));
    	return true;
    }
        if (mMapFragment.onKeyUp(keyCode, event)) {
            return true;
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
