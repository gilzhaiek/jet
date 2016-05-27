package com.reconinstruments.mapImages;


import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.reconinstruments.heading.HeadingEvent;
import com.reconinstruments.heading.HeadingListener;
import com.reconinstruments.heading.HeadingManager;
import com.reconinstruments.mapImages.drawings.RenderSchemeManager;
import com.reconinstruments.mapImages.mapdata.DataSourceManager;
import com.reconinstruments.mapImages.mapdata.DataSourceManagerResortFileDataFormat1;
import com.reconinstruments.mapImages.mapdata.MapObject;
import com.reconinstruments.mapImages.mapview.ReconMapView;
import com.reconinstruments.mapImages.mapview.ReconMapView.IMapView;
import com.reconinstruments.mapImages.mapview.ReconMapView.MapViewState;
import com.reconinstruments.mapImages.IMapDataService;
import com.reconinstruments.mapImages.R;
import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;

import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * @author stevenmason
 *
 */

public class ReconMapFragment extends Fragment  implements IMapView, LocationListener, HeadingListener {
	private static final String TAG = "ReconMapFragment";
	private static final boolean FAKE_USER_POSITION = false;
	private static final boolean FAKE_USER_MOVEMENT = false;
	private static final boolean USE_MAP_ROTATION = false;
	private static final long PAN_HOLD_INTERVAL_MS = 250;
	private static final long LOC_BLOCK_INTERVAL_MS = 1000;
	
	private static final String SP_KEY_VIEWSCALE = "com.reconinstruments.maps.viewscale";

	public enum PanDirection {
		NOT_SET,
		UP,
		DOWN,
		LEFT,
		RIGHT
	}
	
	public static enum MapMode {
		INITIALIZE,
		NO_DATA,
		BASE,
		EXPLORE,
		FIND,
		CONTROL
	}
	
	MapMode mMapMode = MapMode.INITIALIZE;
	
	SensorManager mSensorManager;
	Sensor mCompass;

	private static HeadingManager			mHeadingManager		= null;
	ReconMapView mMapView = null;
	DataSourceManager mDataSourceManager = null;
    RenderSchemeManager mRenderSchemeManager = null;
	ImageView mPanImgView = null;
	ImageView mZoomImgView = null;
	ImageView mExploreImgView = null;
	ImageView mReticleImgView = null;
	
	double mInitRegionWidthInMeters;
	int	mDimensions = 2;

	boolean mTrackUser = true;
	boolean mHaveUserPosition = false;
	double mUserLatitude = 0.0;
	double mUserLongitude = 0.0;
	float  mUserHeading = 0.0f;
	
	double mFakeUserMovementX = 0.0;
	double mFakeUserMovementY = 0.0;
	
	boolean mJetOrLimoHardware = true;

	boolean mUseSoftKeys = true;
	SharedPreferences mSavedPrefs;
	Location mHeadingLocation = null;
	
	private static MapDataServiceConnection	mMapDataServiceConnection = null;
	private LocationManager mLocationManager	= null;
	PanDirection mPanDirection = PanDirection.NOT_SET;
	boolean mPanNudge = true;
	Time mPanButtonPressedTime = new Time();
	private Timer mPanTimer = new Timer();
	private TimerTask mPanTimerCallback = GetNewTimerCallbackTask();
	private TimerTask mBlockLocUpdatesCallback = GetNewTimerCallbackTask();
	private boolean	mBlockLocationDraw = false;
	
	private boolean mIsGeneratingImage = false;
	boolean mIsBound = false;
	IMapDataService mMapDataService = null;

	private TimerTask GetNewTimerCallbackTask() {
		return new TimerTask() {
			@Override
			public void run() {

				getActivity().runOnUiThread(new Runnable() { 

					@Override
					public void run() {
//						Log.d(TAG,"pan nudge cancelled");
						mPanNudge = false;
						switch(mPanDirection) {
						case UP:
							mMapView.PanUp(false);
							break;
						case DOWN:
							mMapView.PanDown(false);
							break;
						case LEFT :
							mMapView.PanLeft(false);
							break;
						case RIGHT:
							mMapView.PanRight(false);
							break;

						}
					}

				});
			}
		};
	}
	
	private TimerTask GetBlockLocationUpdatesTask() {
		return new TimerTask() {
			@Override
			public void run() {

				getActivity().runOnUiThread(new Runnable() { 

					@Override
					public void run() {
						mBlockLocationDraw = false;
					}

				});
			}
		};
	}
	

	
	
	public ReconMapFragment() {	// google recommends that no arguments are passed here
	}
	
	public void init(int dimensions, double initRegionWidthInMeters) {	// define critical type, region width and base orientation for map
		mDimensions = dimensions; 
		mInitRegionWidthInMeters = initRegionWidthInMeters;
	}

	//======================================================================================
	// API
	
	// fragment specific API
	public void showNagigationControls() {
		
	}
	public void hideNagigationControls() {
		
	}
	public void showLayerControls() {
		
	}
	public void hideLayerControls() {
		
	}

	// mapView passthru API methods
	public void SetMapCenter(double newLatitude, double newLongitude) {
		mTrackUser = false;
		if(mMapMode == MapMode.INITIALIZE || mMapMode == MapMode.BASE  || mMapMode == MapMode.NO_DATA) {
			mMapView.SetMapCenter(newLatitude, newLongitude);
		}
		else {
			mMapView.SetMapCenterToUserPosition(newLatitude, newLongitude);
		}
	}
	
	public int AddMapObject(MapObject mapObject) { 	
		return mMapView.AddMapObject(mapObject);
	}

	public void reloadRenderSchemeData() {		// used for tweaking UI settings quickly without rebooting app
		mRenderSchemeManager.LoadSettingsFile(Build.PRODUCT, getActivity().getResources(), getActivity().getPackageName());
	}

	
	//======================================================================================
	// activity/fragment life cycle routines
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Log.i(TAG,"onCreate "+System.currentTimeMillis());
		mSavedPrefs = getActivity().getSharedPreferences("com.reconinstruments.maps", Context.MODE_PRIVATE);
//		Log.e(TAG,"I'm here..");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    String productName = Build.PRODUCT;
	    Log.i(TAG, "Hardware product Name: "+productName);

	    View view;
	    if(!productName.contains("jet") && !productName.equals("limo")) {
	    	mJetOrLimoHardware = false;
	    	view = inflater.inflate(R.layout.fragment_map_phone, container, false);
	    }
	    else {
	    	mJetOrLimoHardware = true;
	    	view = inflater.inflate(R.layout.fragment_map, container, false);
	    }	

//	    LayoutParams p = view.getLayoutParams();
	   // Log.e(TAG,view.GetLayoutParams());
//	    view.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

	    mDataSourceManager = new DataSourceManagerResortFileDataFormat1(null, 1.0, 2.0);
	    mRenderSchemeManager = new RenderSchemeManager(productName, getActivity().getResources(), getActivity().getPackageName()) ;
	    mMapView = (ReconMapView) view.findViewById(R.id.map_view);
	    mMapView.onCreate(mDimensions, mInitRegionWidthInMeters, mDataSourceManager, mTrackUser, this, mRenderSchemeManager);		

	    
	    mPanImgView = (ImageView) view.findViewById(R.id.pan_icon);
	    mZoomImgView = (ImageView) view.findViewById(R.id.zoom_icon);
	    mExploreImgView = (ImageView) view.findViewById(R.id.explore_icon);
	    mReticleImgView = (ImageView) view.findViewById(R.id.reticle);
	    
	    gotoBaseMode();
	    
	    if(!mJetOrLimoHardware) {
	    	view.findViewById(R.id.button_pan_up).setOnClickListener(ButtonClickListener);
	    	view.findViewById(R.id.button_pan_down).setOnClickListener(ButtonClickListener);
	    	view.findViewById(R.id.button_pan_left).setOnClickListener(ButtonClickListener);
	    	view.findViewById(R.id.button_pan_right).setOnClickListener(ButtonClickListener);
	    	view.findViewById(R.id.button_zoom).setOnClickListener(ButtonClickListener);
	    	view.findViewById(R.id.button_toggle_poi).setOnClickListener(ButtonClickListener);
	    }
	    

	    
		return view;
	}
	
	/** UI button response routines */
	OnClickListener ButtonClickListener = new OnClickListener() {
	    @Override
	    public void onClick(final View v) {
	    	
	    	
			switch(mMapMode) {
			case INITIALIZE:
				Log.i(TAG, "dpad ignored...  initializing");
				break;
			case BASE:
		        switch(v.getId()) {
		           case R.id.button_zoom:
		        	   Log.i(TAG, "dpad go to explore mode");
		        	   gotoExploreMode();
		        	   break;
		        }				
				break;
			case EXPLORE:
		        switch(v.getId()) {
		           case R.id.button_pan_up:
		        	   Log.d(TAG, "soft button pan up pressed");
		        	   mMapView.PanUp(false);
		        	   break;
		           case R.id.button_pan_down:
		        	   Log.d(TAG, "soft button pan down pressed");
		        	   mMapView.PanDown(false);
		        	   break;
		           case R.id.button_pan_left:
		        	   Log.d(TAG, "soft button pan left pressed");
//		        	   int newZoomLevel2 = mMapView.CycleZoomUp();	//TODO possibly do something with newZoomLevel??
		        	   mMapView.PanLeft(false);
		        	   break;
		           case R.id.button_pan_right:
		        	   Log.d(TAG, "soft button pan right pressed");
		        	   mMapView.PanRight(false);
		        	   break;
		           case R.id.button_zoom:
		        	   Log.d(TAG, "soft button cycle zoom pressed");
//		        	   int newZoomLevel = mMapView.CycleZoomUp();	//TODO possibly do something with newZoomLevel??
		        	   double mapScale = mMapView.ZoomIn();	//TODO possibly do something with mapScale??
		        	   break;
		        }
				break;
			case FIND:
				break;
			
			}
	    	
	    	
	    }
	};	
	
	private void gotoNoDataMode() {
		mMapMode = MapMode.NO_DATA;
		Log.i(TAG, "entering No Data mode");
		
		if(mJetOrLimoHardware) {
			mPanImgView.setVisibility(View.GONE);
			mZoomImgView.setVisibility(View.GONE);
			mExploreImgView.setVisibility(View.GONE);
			mReticleImgView.setVisibility(View.GONE);
		}
		SetFullScreenMode(false);

	    mTrackUser = false;
	    mMapView.ShowClosestItemDescription(false);
	    mMapView.ShowGrid(false);
	    mMapView.MapRotates(false);
		mMapView.invalidate();
	}
	
	private void gotoBaseMode() {
		mMapMode = MapMode.BASE;
		Log.i(TAG, "entering Base mode");
		
		if(mJetOrLimoHardware) {
			mPanImgView.setVisibility(View.GONE);
			mZoomImgView.setVisibility(View.GONE);

			mExploreImgView.setVisibility(View.VISIBLE);
			mReticleImgView.setVisibility(View.VISIBLE);
		}
		SetFullScreenMode(false);

	    mTrackUser = true;
	    if(mHaveUserPosition) {
	    	mMapView.SetMapCenterToUserPosition(mUserLatitude, mUserLongitude);	// set map center to user's coordinates
	    }
	    mMapView.ShowClosestItemDescription(false);
	    mMapView.ShowGrid(true);
	    mMapView.MapRotates(true);
		mMapView.invalidate();
	}
	
	private void gotoExploreMode() {
		mMapMode = MapMode.EXPLORE;
		Log.i(TAG, "entering Explore mode");
		
		SetFullScreenMode(true);
		
		if(mJetOrLimoHardware) {
			mPanImgView.setVisibility(View.VISIBLE);
			mZoomImgView.setVisibility(View.VISIBLE);

			mExploreImgView.setVisibility(View.GONE);
			mReticleImgView.setVisibility(View.VISIBLE);
		}
	    mTrackUser = false;
	    mMapView.ShowClosestItemDescription(true);
	    mMapView.ShowGrid(true);
	    mMapView.MapRotates(false);
	    mMapView.invalidate();
	}
	
	private void gotoFindMode() {
		mMapMode = MapMode.FIND;
		Log.i(TAG, "entering Find mode");
		
		if(mJetOrLimoHardware) {
			mPanImgView.setVisibility(View.GONE);
			mZoomImgView.setVisibility(View.GONE);

			mExploreImgView.setVisibility(View.GONE);
		}		
	    mMapView.ShowClosestItemDescription(false);
	    mMapView.ShowGrid(true);
	    mMapView.MapRotates(false);
		mMapView.invalidate();
		
	}
	
	private void gotoControlMode() {
		Log.v(TAG, "entering Control mode");
		mMapMode = MapMode.CONTROL;
		
		if(mJetOrLimoHardware) {
			mPanImgView.setVisibility(View.GONE);
			mZoomImgView.setVisibility(View.GONE);

			mExploreImgView.setVisibility(View.GONE);
			mReticleImgView.setVisibility(View.GONE);
		}
		
		SetFullScreenMode(false);
		
		mMapView.ShowClosestItemDescription(false);
	    mMapView.ShowGrid(true);
	    mMapView.MapRotates(false);
	    mMapView.isGenerateImage(false);
		mMapView.invalidate();
	}
	protected void SetFullScreenMode( boolean on )
	{
		Window window = getActivity().getWindow(); 
		
		if(on)
		{
			window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		else
		{
			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}
	
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Log.i(TAG, "###keydown: "+mMapMode);
		switch(mMapMode) {
		case INITIALIZE:
			Log.i(TAG, "dpad ignored...  initializing");
		case NO_DATA:
		case BASE:
		case CONTROL:
			Log.i(TAG, "CONTROL MODE.");
			switch(keyCode) {
			case KeyEvent.KEYCODE_BACK:
				Log.i(TAG, "leaving map app");
				gotoControlMode();
				
			    //((ColumnElementFragmentActivity)getActivity()).goBack();
				return true;
			
			case KeyEvent.KEYCODE_DPAD_CENTER:
				Log.v(TAG, "dpad go to control mode");
				gotoControlMode();
				break;
			case KeyEvent.KEYCODE_DPAD_UP:
				Log.v(TAG, "Key start generating image when press up key in CONTROL mode.");
				startGenerateImage();
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:			
				Log.v(TAG, "Key stop generating image when press down key in CONTROL Mode.");
				stopGenerateImage();
				break;
		    }
		case EXPLORE:
			Log.i(TAG, "explore mode");
			switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
//				Log.d(TAG, "dpad pan up pressed "+mPanDirection);
				if(mPanDirection == PanDirection.NOT_SET) {
					mPanDirection = PanDirection.UP;
					postPanKeyDown();
				}
//				else {
					mMapView.PanUp(false);
//				}
				Log.v(TAG, "Key start generating image when press up key in CONTROL mode.");
				startGenerateImage();
				
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
//				Log.d(TAG, "dpad pan down pressed "+mPanDirection);
				if(mPanDirection == PanDirection.NOT_SET) {
					mPanDirection = PanDirection.DOWN;
					postPanKeyDown();
				}
//				else {
					mMapView.PanDown(false);
//				}
				
				Log.v(TAG, "Key stop generating image when press down key in CONTROL Mode.");
				stopGenerateImage();
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
//				Log.d(TAG, "dpad pan left pressed "+mPanDirection);
				if(mPanDirection == PanDirection.NOT_SET) {
					mPanDirection = PanDirection.LEFT;
					postPanKeyDown();
				}
//				else {
					mMapView.PanLeft(false);
//				}
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
//				Log.d(TAG, "dpad pan right pressed "+mPanDirection);
				if(mPanDirection == PanDirection.NOT_SET) {
					mPanDirection = PanDirection.RIGHT;
					postPanKeyDown();
				}
//				else {
					mMapView.PanRight(false);
//				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
//				Log.d(TAG, "dpad cycle zoom pressed");
				double mapScale = mMapView.ZoomIn();	//TODO possibly do something with scale??
//				double mapScale = mMapView.ZoomOut();	//TODO possibly do something with scale??
				break;
			}
			return true;
		case FIND:
			break;
		}
		return true;

	}
	
	private void postPanKeyDown() 
	{
//		mPanNudge = true;
//		mPanTimerCallback.cancel();
//		mPanTimer.purge();
//		mPanTimerCallback = GetNewTimerCallbackTask();
//		mPanTimer.schedule(mPanTimerCallback, PAN_HOLD_INTERVAL_MS);
	}
	
	private void postPanKeyUp() 
	{
		// cancel existing timer tasks	- TODO remove as location change does not invalidate any more
//		mPanTimerCallback.cancel();
//		mBlockLocUpdatesCallback.cancel();
//		mPanTimer.purge();

		// reset nudge timer variables
//		mPanDirection = PanDirection.NOT_SET;
//		mPanTimerCallback = GetNewTimerCallbackTask();
//		mPanNudge = false;
		
		// sched task to block loc update-based drawing... improves ui experience as loc updates cause draw which delay ui (for now.. will change in future)
//		mBlockLocationDraw = true;
//		mBlockLocUpdatesCallback = GetBlockLocationUpdatesTask();
//		mPanTimer.schedule(mBlockLocUpdatesCallback, LOC_BLOCK_INTERVAL_MS);
		
		mMapView.FreezePan();
		
	}

	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		Log.i(TAG, "keyup: "+mMapMode);
		switch(mMapMode) {
		case INITIALIZE:
		case NO_DATA:
		case BASE:
		    break;
		case EXPLORE:
			switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
//				Log.d(TAG, "dpad pan up lifted "+mPanDirection);
//				if(mPanNudge) mMapView.PanUp(true);
				postPanKeyUp();
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
//				Log.d(TAG, "dpad pan down lifted "+mPanDirection);
//				if(mPanNudge) mMapView.PanDown(true);
				postPanKeyUp();
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
//				Log.d(TAG, "dpad pan left lifted "+mPanDirection);
//				(mPanNudge) mMapView.PanLeft(true);
				postPanKeyUp();
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
//				Log.d(TAG, "dpad pan right lifted "+mPanDirection);
//				if(mPanNudge) mMapView.PanRight(true);
				postPanKeyUp();
				break;
			case KeyEvent.KEYCODE_BACK:
				gotoBaseMode();
				break;
			}
			break;
		case FIND:
			break;
		case CONTROL:
			break;
		}
		return true;

	}

	public void onActivityCreated() {	
	}
	
	
	public void onResume(){
		super.onResume();

		Log.i(TAG,"onResume "+System.currentTimeMillis());
		if(mLocationManager == null) {
			mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}    	

		BindMapDataService();  // call only after activity has been created
		getActivity().startService(new Intent("RECON_NEW_MAP_IMAGE_SERVICE"));	// start up map service

		mMapView.onResume();

		// retrieve last used viewscale
		float prevScale = ((Float) mSavedPrefs.getFloat(SP_KEY_VIEWSCALE, 0.0f)).floatValue();
//	    Log.d(TAG, "loading scale " + prevScale);

		if(prevScale > 0.0f) {
			mMapView.SetCurrentViewscale(prevScale); 
		}
		mPanTimer = new Timer();
		mPanTimerCallback = GetNewTimerCallbackTask();
		
		if(mHeadingManager == null) 
			mHeadingManager = new HeadingManager(getActivity().getApplication(), this);
		mHeadingManager.initService();

		
	}
	
	public void onPause(){
		super.onPause();
		mMapView.onPause();
//	    mLocationManager.removeUpdates(this);
//	    mLocationManager = null;			
	    if(mHeadingManager != null) {
	    	mHeadingManager.releaseService();
	    	mHeadingManager = null;
	    }
		if(mPanTimerCallback != null) {
			mPanTimerCallback.cancel();
		}
		if(mBlockLocUpdatesCallback != null) {
			mBlockLocUpdatesCallback.cancel();
		}
		if(mPanTimer != null) {
			mPanTimer.cancel();
		}

	    // save viewscale
	    Float curScale = new Float(mMapView.GetCurrentViewscale());
//	    Log.d(TAG, "saving scale " + curScale);
		mSavedPrefs.edit().putFloat(SP_KEY_VIEWSCALE, curScale).commit() ;

	}
	
	public void onDestroy(){
		UnbindMapDataService();
		
		super.onDestroy();
		Log.d(TAG,"onDestroy");
			
	}


	//===========================================================
	// set up variables and routines used to access map data service

	private void BindMapDataService() 
	{
		Activity activity = getActivity();
		assert activity != null : TAG + ".BindMapDataService called before activity created.";

		if (mMapDataServiceConnection == null) {
			mMapDataServiceConnection = new MapDataServiceConnection();
			activity.bindService(new Intent("RECON_NEW_MAP_IMAGE_SERVICE"), mMapDataServiceConnection, Context.BIND_AUTO_CREATE);
			
			mIsBound = true;
			Log.d(TAG, "BindMapsService:bindService");
		}
	}
 
	private void UnbindMapDataService()
	{
		Activity activity = getActivity();
		assert activity != null : TAG + ".UnbindMapDataService called before activity created.";

		
		if (mMapDataServiceConnection != null)
		{
			mDataSourceManager.RemoveIMapDataService();	// cleanup before unbinding
			
			activity.unbindService(mMapDataServiceConnection);
			mMapDataServiceConnection = null;
			mIsBound = false;
			Log.d(TAG, "UnbindMapsService:unbindService");
		}
	}	
	
	class MapDataServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName className, IBinder boundService)
		{
			Log.d(TAG, "Map Data Service interface available");
			mDataSourceManager.SetIMapDataService(IMapDataService.Stub.asInterface((IBinder)boundService));
			try {
				
				mMapDataService = IMapDataService.Stub.asInterface((IBinder)boundService);
			//mMapService = (MapsService) boundService;
			//mMapService = ((IMapDataService.Stub)boundService).getService();
			
			}
			catch (Exception e) {
				e.printStackTrace();
				Log.v(TAG,"MapDataServiceConnection, mapService=null"); 
			}

			Log.v(TAG, "Key MapDataServiceConnection, mapService=" + ((mMapDataService==null)? "null":"ok"));
		}

		public void onServiceDisconnected(ComponentName className)
		{	
			Log.d(TAG, "Map Data Service interface disconnected");
			mDataSourceManager.SetIMapDataService(null);
			mMapDataService = null;

		}
	}
	
	  
	// HeadingListener callback
	public void onHeadingChanged(HeadingEvent headingEvent) {

		// smooth heading
		float newHeading = headingEvent.mYaw;
		if(mUserHeading > 270.0f && newHeading < 90.0f) {
			mUserHeading = mUserHeading - 360.0f;	// avoid aliasing in average when crossing North (angle = 0.0)
		}
		else if (mUserHeading < 90.0f && newHeading > 270.0f) {
			newHeading = newHeading - 360.0f;	// avoid aliasing in average when crossing North (angle = 0.0)
		}
		mUserHeading = (float) ((4.0*mUserHeading + newHeading)/5.0);		// smooth heading
		
		mUserHeading = (mUserHeading+360.0f)%360.0f; // ensure heading is 0-360
		

		mMapView.SetUserHeading(mUserHeading);
		if (USE_MAP_ROTATION) {
//			Log.i(TAG, "heading listener - " + mUserHeading );
			if(mMapMode == MapMode.BASE) {
				mMapView.SetMapHeading(mUserHeading);
			}
		}
		

	}	



	//======================================================================================
	// callbacks

	// IMapView interface routine
	public void MapViewStateChanged(ReconMapView.MapViewState mapViewState) {		// interface callback from MapView
		Log.i(TAG, "MapViewStateChanged: " + mapViewState);
	    switch(mapViewState) {
	    case WAITING_FOR_LOCATION: 
	    	if(mMapMode != MapMode.NO_DATA) gotoNoDataMode();
	    	break;
	    case STARTING_DATA_SERVICE: 
	    	if(mMapMode != MapMode.NO_DATA) gotoNoDataMode();
	    	break;
	    case LOADING_DATA: 
	    	if(mMapMode != MapMode.NO_DATA) gotoNoDataMode();
	    	break;
	    case MAP:
	    	if(mMapMode == MapMode.NO_DATA) gotoBaseMode();
	    	break;
	    case NO_DATA: 
	    	if(mMapMode != MapMode.NO_DATA) gotoNoDataMode();
	    	break;
	    case ERROR_DATALOAD: 
	    	if(mMapMode != MapMode.NO_DATA) gotoNoDataMode();
	    	break;
	    }		
	}
	
	public void LoadMapDataComplete(String errorMsg) {		// interface callback from MapView
		// when map Data finished loading in MapView 
		if(errorMsg.length() == 0) {	// no error
//			Log.i(TAG,"LoadMapDataComplete");
			gotoBaseMode();
		}
	}
	
	// locationListener callbacks
    @Override
    public void onLocationChanged(Location location) {
 //      	Log.e(TAG, "location listener-onLocationChanged");

    	if(mMapView != null && !mBlockLocationDraw) {
			if(FAKE_USER_POSITION) { 
				if(FAKE_USER_MOVEMENT) { 
					mFakeUserMovementX -=  0.0005;
					mFakeUserMovementY +=  0.000;
				}
				else {
					Random r = new Random();
					mFakeUserMovementX += (float)(r.nextInt(11) - 5) * 0.00000001; // to emulate jitter in GPS signal
					mFakeUserMovementY += (float)(r.nextInt(11) - 5) * 0.00000001;
				}
				location.setLongitude(-122.963705 + mFakeUserMovementX);	// fake user location for now..
				location.setLatitude(50.085702 + mFakeUserMovementY);
			}
			
//			if(mMapMode == mMapMode.INITIALIZE || mMapMode == mMapMode.BASE || (mUserLatitude != location.getLatitude() && mUserLongitude != location.getLongitude())) {
				mUserLatitude = location.getLatitude();
				mUserLongitude = location.getLongitude();
	   			mHaveUserPosition = true;
//	   	      	Log.i(TAG, "location listener-onLocationChanged.. new position or startup");
	
	   			// TODO, skip update/redraw if location hasn't significantly changed
	   			
	   			if(mTrackUser) {
	    			mMapView.SetMapCenterToUserPosition(mUserLatitude, mUserLongitude);	// set map center to user's coordinates and 
	    		}
	    		else {
	     			mMapView.SetUserPosition(mUserLatitude, mUserLongitude);
	    		}
//			}
    	}
    }

    @Override 
    public void onProviderDisabled(String provider)
    {
    	Log.i(TAG, "location listener-onProviderDisabled");
    	//Toast.makeText( mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_disabled), Toast.LENGTH_SHORT);
    }
    
    @Override
    public void onProviderEnabled(String provider)
    {
    	Log.i(TAG, "location listener-onProviderEnabled");
    	//Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_enabled), Toast.LENGTH_SHORT);
    }
    
    @Override
    public void onStatusChanged (String provider, int status, Bundle extras)
    {
//    	Log.e(TAG, "location listener-onStatusChanged");
/*    	
    	switch( status )
    	{
	    	case LocationProvider.OUT_OF_SERVICE:
	    		Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_out_of_service), Toast.LENGTH_SHORT);
	    	break;
	    	
	    	case LocationProvider.AVAILABLE:
	    		Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_out_of_service), Toast.LENGTH_SHORT);
	    	break;
	    	
	    	case LocationProvider.TEMPORARILY_UNAVAILABLE:
	    		Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_unavailable), Toast.LENGTH_SHORT);
    	}
*/    	    	
    }
    
//======================================================================================
    
    private void startGenerateImage() {
    	Log.v(TAG, "startGenerateImage()");
    	
    	if (mIsGeneratingImage) {
    		Log.v(TAG, "startGenerateImage() has already started");
    		return;
    	}
    	
    	mIsGeneratingImage = true;
    	
    	if (mMapDataService==null) {
    		Log.v(TAG, "startGenerateImage(), mMapDataService==null)");
    		return;
    	}
    	
		try {
			
			mMapDataService.startGenerateImage();
			mMapView.isGenerateImage(true);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
    	
    }//startGenerateImage
    
    private void stopGenerateImage() {
    	Log.v(TAG, "stopGenerateImage()");
    	if (!mIsGeneratingImage) {
    		Log.v(TAG, "stopGenerateImage() has not started.");
    		return;
    	}
    	
    	mIsGeneratingImage = false;

    	if (mMapDataService==null) {
    		Log.v(TAG, "stopGenerateImage(), mMapDataService==null");
    		return;
    	}
    	
		try {
		
			mMapDataService.stopGenerateImage();
			mMapView.isGenerateImage(false);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    }//stopGenerateImage
}
