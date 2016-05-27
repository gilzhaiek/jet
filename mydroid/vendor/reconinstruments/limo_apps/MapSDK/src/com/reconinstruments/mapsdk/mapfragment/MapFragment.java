package com.reconinstruments.mapsdk.mapfragment;


import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.heading.HeadingEvent;
import com.reconinstruments.heading.HeadingListener;
import com.reconinstruments.heading.HeadingManager;
import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.MapView.IMapView;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;

/**
 * @author stevenmason
 *
 */
public class MapFragment extends Fragment  implements IMapView, HeadingListener{
	
// constants
	private static final String 	TAG = "MapFragment";
	private static final boolean 	USE_OPENGL = false;

	public  final  static String 	GEODATASERVICE_BIND_CLIENT_INTERFACE = "com.reconinstruments.geodataservice.clientinterface";
	public  final  static String 	GEODATASERVICE_BROADCAST_STATE_CHANGED = "com.reconinstruments.geodataservice.state_changed";
	public  final  static String 	GEODATASERVICE_BROADCAST_SERVICE_LOADING_DATA = "com.reconinstruments.geodataservice.serviceloadingdata";
	public  final  static String 	GEODATASERVICE_BROADCAST_REQUESTED_DATA_READY = "com.reconinstruments.geodataservice.requested_data_ready";
	public  final  static String 	GEODATASERVICE_BROADCAST_REQUESTED_DATA_LOAD_ERROR = "com.reconinstruments.geodataservice.requested_data_load_error";
	public  final  static String 	GEODATASERVICE_BROADCAST_NEW_DYNAMIC_USER_DATA = "com.reconinstruments.geodataservice.new_dynamic_user_data";
	public  final  static String 	GEODATASERVICE_BROADCAST_NEW_DYNAMIC_BUDDY_DATA = "com.reconinstruments.geodataservice.new_dynamic_buddy_data";
	private static final String 	SP_KEY_VIEWSCALE = "com.reconinstruments.mapsdk.viewscale";
	
	private static final float 		HEADING_CORRECTION_FOR_JET = 0.f;
	

	
	// dev flags, disable or remove after dev
	private static final boolean 	SHOW_GEODATA_SERVICE_DEBUG_INDICATORS = false;		// extra leds during dev to show geodata service connection and data
	private static final boolean 	USE_LAST_SCALE = false;								// TODO set to true or remove after dev confirmed this is working

	
//members
	protected ViewGroup					mFragView = null;
	static GeodataServiceConnection	mGeodataServiceConnection = null;
	static IGeodataService			mGeodataServiceInterface  = null;
	static HeadingManager			mHeadingManager		= null;
	
	protected GeoDataServiceState 	mGeodataServiceState = null;
    RenderSchemeManager 			mRenderSchemeManager = null;		// to do, move to mapview
	protected MapView 				mMapView = null;
//	protected ImageView 			mReticleImgView = null;
	protected ImageView 			mGeodataService = null;
	protected ImageView 			mGPSBlink = null;
	protected ProgressBar 			mProgressBar = null;
	protected ProgressBar 			mActiveProgressBar = null;
	protected ProgressBar 			mTimeoutProgressBar = null;
	
	protected TextView 				mMessageTextView = null;
	
	public    String 				mIdentifier = "notDefined";
	protected boolean				mMapVisible = false;
	protected boolean 				mHaveUserPosition = false;
	protected double 				mUserLatitude = 0.0;
	protected double 				mUserLongitude = 0.0;
	protected double 				mUserVelocity = 0.0;
	protected float  				mUserHeading = 0.0f;
	protected float					mUserPitch = 0.0f;
	protected SharedPreferences 	mSavedPrefs;
	protected boolean				mInForeground = false;
	
	protected Window		 		mCurWindow = null;			// used for changing full screen mode on the UI thread
	protected boolean 				mFullScreenOn = false;

	protected String				mFragmentMode = "";
	
	private volatile boolean 		mCameraChangesWithUserVelocity = true;
	private double 					mGeodataServiceLoadingDataPercentage = 0;
	protected boolean mFragmentActive = false;
	
	protected View mOverlayView = null;
	protected boolean mEnableOverlayText = false;
	
	protected boolean 	mPitchDisabled = false;
	
	// interfaces
	public interface IReconMapFragmentCallbacks {
		public void ReconMapFragmentModeChanged(String idStr);		
	}

//	protected boolean				mMapRotates = false;

// constructor and other MapSDK API calls
	public MapFragment() {	
	}
	
	public void setIdentifier(String identifier){
		mIdentifier = identifier;
	}
	public void setPitchDisabled(boolean pitchDisabled){
		mPitchDisabled = pitchDisabled;
	}
	
	public void setEnableOverlayText(boolean enableOverlayText){
		mEnableOverlayText = enableOverlayText;
	}
	
	public void BindMap(MapView mapView, boolean changeWithUserVelocity) {
		mMapView = mapView;
		mMapView.mParentFragment = this;
		mCameraChangesWithUserVelocity = changeWithUserVelocity;
		mapView.SetCameraToFollowUser(true);
		mapView.SetCameraToRotateWithUser(true);
		mMapView.SetCameraToPitchWithUser(false);
		mapView.SetCameraToRespondToUserVelocity(false);
	}
	
	public void ConfigurePreMapViewInit() {  // called from end of MapView onCreateView()
		
	}
	
	public void ConfigurePostMapViewInit() {  // called from end of MapView onCreateView()
		
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "MapFragment *********** Key Down Pressed! ***********");
		return false;		// let system handle keystrokes
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return false;		// let system handle keystrokes

	}
	
	public boolean onBackPressed() { 
		if(mMapView != null) {
			mMapView.UserLeavingApp();
		}
		return false;  // return true if handled
	}
	


	
//======================================================================================
// activity/fragment life cycle routines
	public void onActivityCreated() {	
	}
	 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Log.d(TAG,"onCreate "+System.currentTimeMillis());
//		android.os.Debug.waitForDebugger();

		mSavedPrefs = getActivity().getSharedPreferences("com.reconinstruments.maps", Context.MODE_PRIVATE);
	}
	
	@Override					
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    View view = onCreateViewBase(inflater, container, savedInstanceState, R.layout.fragment_map);
	    
	    gotoMessageMode();
	    return view;
	    
	}
	
	@SuppressWarnings("deprecation")
	protected View onCreateViewBase(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, int layoutID) {
		
		mFragView = (ViewGroup) inflater.inflate(layoutID, null);

        Context context = getActivity();
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        //SetFullScreen(true);
        
 		if(mMapView != null) {
			// attach the mapView to the fragment
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
			mFragView.addView(mMapView, params);
			sendViewToBack(mMapView);
			
			mMapView.setCameraChangeWithUserVelocity(mCameraChangesWithUserVelocity);
			mMapView.onCreateView();		// 
	
		    // retrieve handles to other subViews in fragView
//			mReticleImgView = (ImageView) mFragView.findViewById(R.id.reticle);
		    mGeodataService = (ImageView) mFragView.findViewById(R.id.debug_service_connected);
		    mGPSBlink = (ImageView) mFragView.findViewById(R.id.debug_gps_blinker);

	    	mProgressBar = (ProgressBar) mFragView.findViewById(R.id.loadmap_progressbar);
	    	mActiveProgressBar = (ProgressBar) mFragView.findViewById(R.id.activemap_progressbar);
	    	mTimeoutProgressBar = (ProgressBar) mFragView.findViewById(R.id.loadmap_timeoutbar);
	    	mMessageTextView = (TextView) mFragView.findViewById(R.id.textview);

	    }	
	    else {
	    	Log.e(TAG, "Error: Creating MapFragment view base with no bound MapView");
	    	return null;
	    }
		
		// overly-awkward, android way to get the fragment dimensions
		final View fView = mFragView;
		fView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
		        if (Build.VERSION.SDK_INT < 16) {
		        	fView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		        } else {
		        	fView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		        }

			    float fWidth = fView.getWidth();
			    float fHeight = fView.getHeight();
			    
//			    Log.e(TAG, "screen dimensions 2: " + " | frag:" + fWidth +", " + fHeight);
			    mMapView.SetScreenDimensions(fWidth, fHeight);
		    }
		});

		return mFragView;
	}

//	 SetFullScreen - if fullscreen=true, fragment will draw on full physical screen - under status or action bars
	 private void SetFullScreen(boolean fullscreen) {
		Log.d(TAG, "setting to " + ((fullscreen) ? "fullscreen" : "normal view"));
	  	View decorView = getActivity().getWindow().getDecorView();
	    
	    decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
		    @Override
		    public void onSystemUiVisibilityChange(int visibility) {
		        // system/status bar visible
		        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
			        // system/status bar visible
	        		Log.e(TAG, "Status bar visible ...");
		        	if(mMapView != null) {
		        		mMapView.SetScreenStatusBarHeight(30);
		        	}
		        } 
		        else {
		            // the system bars are NOT visible. 
	        		Log.e(TAG, "Status bar hidden ...");
		        	if(mMapView != null) {
		        		mMapView.SetScreenStatusBarHeight(0);
		        	}
		        }
		    }
		});

	    int uiOptions = (fullscreen) ? View.SYSTEM_UI_FLAG_FULLSCREEN : View.SYSTEM_UI_FLAG_VISIBLE;
	    decorView.setSystemUiVisibility(uiOptions);
	    decorView.requestLayout();
	  }
	
	public static void sendViewToBack(final View child) {
	    final ViewGroup parent = (ViewGroup)child.getParent();
	    if (null != parent) {
	        parent.removeView(child);
	        parent.addView(child, 0);
	    }
	}
	
	
 //============================================
 // handler for pushed (broadcast) Intents (service API calls) 
   private BroadcastReceiver mGeodataServiceAPIReceiver = new BroadcastReceiver() {
     	@Override
     	public void onReceive(Context context, Intent intent) {
     		IGeodataServiceResponse response;
			if(mGeodataServiceInterface != null) {
				
				if(intent.getAction().toString().equalsIgnoreCase(GEODATASERVICE_BROADCAST_SERVICE_LOADING_DATA)) {
	         		Bundle b = intent.getExtras();
	     		 	mGeodataServiceLoadingDataPercentage = b.getDouble("percentLoaded");

	     			if(mMapView.mViewState == MapView.MapViewState.WAITING_FOR_DATA_SERVICE) {
	     				mProgressBar.setProgress((int)(mGeodataServiceLoadingDataPercentage * 100));
	     			}

	     		}
	    		
	     		if(intent.getAction().toString().equalsIgnoreCase(GEODATASERVICE_BROADCAST_STATE_CHANGED)) {
	         		Log.d(TAG, "Received changed service state");
	         		// go get changed state
					try {
						response = mGeodataServiceInterface.getServiceState();
						mGeodataServiceState = response.mServiceState;
						Log.d(TAG, "New Geodata Service state: " + mGeodataServiceState);
						mMapView.SetGeodataServiceState(mGeodataServiceState);
					} 
					
					catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();			//TODO handle API call failures
					}
	     		}
	    		
	     		if(intent.getAction().toString().equalsIgnoreCase(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_USER_DATA)) {
	         		// go get changed state
	         		Bundle b = intent.getExtras();
	     		 	Double userLongitude = b.getDouble("newLongitude");
	     		 	Double userLatitude = b.getDouble("newLatitude");
	     		 	Double userVelocity = b.getDouble("newVelocity");
	     		 	
	     		 	if(mCameraChangesWithUserVelocity) Log.d(TAG,"newVelocity = " + String.valueOf(userVelocity));

	     		 	if(SHOW_GEODATA_SERVICE_DEBUG_INDICATORS) {
	     		 		if(mGPSBlink.getVisibility() == View.VISIBLE) {
	     		 			mGPSBlink.setVisibility(View.GONE);
	     		 		}
	     		 		else {
	     		 			mGPSBlink.setVisibility(View.VISIBLE);
	     		 		}
	     		 	}	     		 	
	     		 	Date d = new Date();
	     		 	mHaveUserPosition = true;
	     		 	mUserLatitude = userLatitude;
	     		 	mUserLongitude = userLongitude;
	     		 	mUserVelocity = userVelocity;
	     			if(mInForeground) {
		     		 	SetMapViewUserPosition();
	     			}
	       		}
	    		
	     		if(intent.getAction().toString().equalsIgnoreCase(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_BUDDY_DATA)) {
	     			Log.d(TAG, "Broadcast new buddy data.");
	     			if(mMapView != null) {
	     				mMapView.HandleNewBuddies();
	     			}
	     		}
			}
			else { // received geodata service broadcast but not connected - attempt to bind again
				mGeodataServiceInterface = null;
				BindMapDataService();  // try to bind service, possibly lost connection somehow
			}
     	}
    };

    
    	// overridden by subclasses that need to change user data handling
	protected void SetMapViewUserPosition() {
    	mMapView.SetUserPosition(mUserLongitude, mUserLatitude, mUserVelocity);	// set user's coordinates and velocity
	}
    
	
	@Override
	public void onResume(){
		Log.d("MapFragment", "onResume() in MapFragment!");
		super.onResume();

    	IntentFilter filter1 = new IntentFilter();
    	filter1.addAction(GEODATASERVICE_BROADCAST_STATE_CHANGED);		
    	filter1.addCategory(Intent.CATEGORY_DEFAULT);
    	getActivity().registerReceiver(mGeodataServiceAPIReceiver, filter1);

    	IntentFilter filter2 = new IntentFilter();
    	filter2.addAction(GEODATASERVICE_BROADCAST_REQUESTED_DATA_READY);		
    	filter2.addCategory(Intent.CATEGORY_DEFAULT);
    	getActivity().registerReceiver(mGeodataServiceAPIReceiver, filter2);

    	IntentFilter filter3 = new IntentFilter();
    	filter3.addAction(GEODATASERVICE_BROADCAST_REQUESTED_DATA_LOAD_ERROR);		
    	filter3.addCategory(Intent.CATEGORY_DEFAULT);
    	getActivity().registerReceiver(mGeodataServiceAPIReceiver, filter3);

    	IntentFilter filter4 = new IntentFilter();
    	filter4.addAction(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_USER_DATA);		
    	filter4.addCategory(Intent.CATEGORY_DEFAULT);
    	getActivity().registerReceiver(mGeodataServiceAPIReceiver, filter4);

       	IntentFilter filter5 = new IntentFilter();
    	filter5.addAction(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_BUDDY_DATA);		
    	filter5.addCategory(Intent.CATEGORY_DEFAULT);
    	getActivity().registerReceiver(mGeodataServiceAPIReceiver, filter5);

       	IntentFilter filter6 = new IntentFilter();
    	filter6.addAction(GEODATASERVICE_BROADCAST_SERVICE_LOADING_DATA);		
    	filter6.addCategory(Intent.CATEGORY_DEFAULT);
    	getActivity().registerReceiver(mGeodataServiceAPIReceiver, filter6);

		Log.d(TAG,"onResume "+System.currentTimeMillis());

//		if(mGeodataServiceInterface == null) {
			BindMapDataService();  // call only after activity has been created
//		}
		mMapView.onResumeView();

		if(USE_LAST_SCALE) {		// retrieve last used viewscale
			float prevScale = ((Float) mSavedPrefs.getFloat(SP_KEY_VIEWSCALE, 0.0f)).floatValue();
	
			if(prevScale > 0.0f) {
				mMapView.SetCameraAltitudeScale(prevScale); 
			}
		}
		mHeadingManager = new HeadingManager(getActivity().getApplication(), this);
		mHeadingManager.initService();
		mInForeground = true;
	}
	
	@Override
	public void onPause(){
		mInForeground = false;
		if(mMapView != null) {
			mMapView.onPauseView();
		}
	    if(mHeadingManager != null) {
	    	mHeadingManager.releaseService();
	    	mHeadingManager = null;
	    }

	    // save viewscale
	    Float curScale = new Float(mMapView.GetCameraAltitudeScale());
		mSavedPrefs.edit().putFloat(SP_KEY_VIEWSCALE, curScale).commit() ;

	    Log.e(TAG, "onPause - unregistering receivers");
	   	getActivity().unregisterReceiver(mGeodataServiceAPIReceiver);
	   	
		super.onPause();
	}
	
	@Override
	public void onStop(){
		Log.d(TAG, "onStop() called");
		super.onStop();
	}
	
	@Override
	public void onDestroy(){
		UnbindMapDataService();
		
		super.onDestroy();
		Log.d(TAG,"onDestroy");
			
	}
	
//===========================================================
// service interface binding

	private void BindMapDataService() 
	{
		BindMapDataService(false);
	}
	
	private void BindMapDataService(boolean force){
		Activity activity = getActivity();
		assert activity != null : TAG + ".BindMapDataService called before activity created.";

		if (mGeodataServiceConnection == null || force) {
			mGeodataServiceConnection = new GeodataServiceConnection();
			activity.bindService(new Intent(GEODATASERVICE_BIND_CLIENT_INTERFACE), mGeodataServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "BindMapsService:bindService");
		}
	}
 
	private void UnbindMapDataService()
	{
		Activity activity = getActivity();
		assert activity != null : TAG + ".UnbindMapDataService called before activity created.";

		
		if (mGeodataServiceConnection != null)
		{
			
			activity.unbindService(mGeodataServiceConnection);
			mGeodataServiceConnection = null;
			Log.d(TAG, "UnbindMapsService:unbindService");
		}
	}	
	
	class GeodataServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName className, IBinder boundService)
		{
			if(SHOW_GEODATA_SERVICE_DEBUG_INDICATORS) mGeodataService.setVisibility(View.VISIBLE);

				Log.d(TAG, "connected to Geodata Service client interface");
				// returns IBinder for service.  Use this to create desired (predefined) interface for service
				mGeodataServiceInterface = IGeodataService.Stub.asInterface((IBinder)boundService);
				if(mGeodataServiceInterface == null) {
					Log.e(TAG, "GeodataService bind error: invalid IGeodataService object");
				}
				else {
					mMapView.SetIGeodataService(mGeodataServiceInterface);	// service pass interface to mapView
					IGeodataServiceResponse response;
					try {
						response = mGeodataServiceInterface.getServiceState();		// go retrieve service state after binding to service
						mGeodataServiceState = response.mServiceState;
						mMapView.SetGeodataServiceState(mGeodataServiceState);		// pass state down to mapView
					} 
					catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();			//TODO handle API call failures
					}
				}
		}

		public void onServiceDisconnected(ComponentName className)
		{	
			if(SHOW_GEODATA_SERVICE_DEBUG_INDICATORS) {
				mGeodataService.setVisibility(View.GONE);
				mGPSBlink.setVisibility(View.GONE);
			}

		    Log.d(TAG, "Map Data Service interface disconnected");
		    mGeodataServiceInterface = null;
			mMapView.SetIGeodataService(mGeodataServiceInterface);
			mGeodataServiceState= null;
			mMapView.SetGeodataServiceState(mGeodataServiceState);
		}
	} 

	protected final Runnable NotifyActivityOfModeChange = new Runnable() {
	    public void run() {
			Activity parentActivity = getActivity();
			if((parentActivity instanceof IReconMapFragmentCallbacks) ) {
				((IReconMapFragmentCallbacks) parentActivity).ReconMapFragmentModeChanged(mFragmentMode);
			}
		}
	};

	final Runnable SetMessage = new Runnable() {
	    public void run() {
    		mMessageTextView.setText(mMapView.mModeMessages.get(mMapView.mViewState));
		}
	};

	final Runnable SetMessageModeUIElements = new Runnable() {
		public void run() {
		    mMapVisible = false;
			mMessageTextView.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.GONE);
			mTimeoutProgressBar.setVisibility(View.GONE);
			mActiveProgressBar.setVisibility(View.GONE);

		    mMapView.ShowClosestItemDescription(false);
		    mMapView.ShowGrid(false);
			mMapView.invalidate();
	    }
	};

	protected void gotoMessageMode() {
		Log.d(TAG, "entering Message mode");
		
		getActivity().runOnUiThread(SetMessageModeUIElements);
		mFragmentMode = "Message";
		getActivity().runOnUiThread(NotifyActivityOfModeChange);
	}
	
	final Runnable SetLoadMapModeUIElements = new Runnable() {
	    public void run() {
	    	mProgressBar.setVisibility(View.VISIBLE);	// set loading progressBar to visible
//			mTimeoutProgressBar.setVisibility(View.VISIBLE);
	    	mProgressBar.setProgress(3);
	    	mTimeoutProgressBar.setProgress(0);

		    mMapView.ShowClosestItemDescription(false);
		    mMapView.ShowGrid(false);
			mMapView.invalidate();
	    }
	};

	protected void gotoLoadMapMode() {
		Log.d(TAG, "entering Load Map mode");
		
		getActivity().runOnUiThread(SetMessageModeUIElements);
		getActivity().runOnUiThread(SetLoadMapModeUIElements);
		mFragmentMode = "LoadingMap";
		getActivity().runOnUiThread(NotifyActivityOfModeChange);
	}

	final Runnable SetBaseMapModeUIElements = new Runnable() {
	    public void run() {
			mMapVisible = true;
			mMessageTextView.setVisibility(View.GONE);
			mProgressBar.setVisibility(View.GONE);
			mTimeoutProgressBar.setVisibility(View.GONE);
			mActiveProgressBar.setVisibility(View.GONE);

			mMapView.ShowClosestItemDescription(false);
		    mMapView.ShowGrid(true);
			mMapView.invalidate();
 	    }
	};

	protected void gotoBaseMapMode() {			// by default, doesn't track user or rotate... needs user to pan it around via activity
		Log.d(TAG, "entering Base Map mode");
		mMapView.setExploreModeEnabled(false);
		getActivity().runOnUiThread(SetBaseMapModeUIElements);
		mFragmentMode = "BaseMap";
		getActivity().runOnUiThread(NotifyActivityOfModeChange);
	}
	
//======================================================================================
// service/mapview callbacks

	// HeadingListener callback
	public void onHeadingChanged(HeadingEvent headingEvent) {

		// smooth heading
		float newHeading = headingEvent.mYaw ;
//		float newHeading = (headingEvent.mYaw + HEADING_CORRECTION_FOR_JET + 360.0f) % 360.0f ;

		if(mUserHeading > 270.0f && newHeading < 90.0f) {
			mUserHeading = mUserHeading - 360.0f;	// avoid aliasing in average when crossing North (angle = 0.0)
		}
		else if (mUserHeading < 90.0f && newHeading > 270.0f) {
			newHeading = newHeading - 360.0f;	// avoid aliasing in average when crossing North (angle = 0.0)
		}
		mUserHeading = (float) ((4.0*mUserHeading + newHeading)/5.0);		// smooth heading
		
		mUserHeading = (mUserHeading  + 360.0f)%360.0f; // ensure heading is 0-360
		
		mUserPitch = headingEvent.mPitch;
		if(mInForeground) {
			mMapView.SetUserHeading(mUserHeading);
			mMapView.SetUserPitch(mUserPitch);
		}
//		handleHeadingChange();
	}	

//	protected void handleHeadingChange() {	// atomic so extensions can override handling of heading change
//		mMapView.SetCameraHeading(mUserHeading);
//	}
//
//	// IMapView interface routine
	public void MapViewStateChanged(MapView.MapViewState mapViewState) {		// interface callback from MapView
		Log.d(TAG, "MapViewStateChanged: " + mapViewState);
	    switch(mapViewState) {
	    case ERROR_INITIALIZING: 
	    case WAITING_FOR_LOCATION: 
	    case PROBLEM_WITH_DATA_SERVICE: 
	    case REQUIRED_DATA_UNAVAILABLE: 
	    case OUT_OF_MEMORY: 
	    case ERROR_LOADING_THEMES: 
	    case ERROR_LOADING_DATA: 
    		gotoMessageMode();	// hide HUD controls to view messages...
    		getActivity().runOnUiThread(SetMessage);
	    	break;
	    case WAITING_FOR_DATA_SERVICE: 
	    case WAITING_FOR_LAYER_INITIALIZATION: 
    		gotoLoadMapMode();	// hide HUD controls to view messages...
    		getActivity().runOnUiThread(SetMessage);
	    	break;
	    case DRAW_LAYERS:
    		gotoBaseMapMode();
	    	break;
	    }
	}
	
    public void LoadingMapWithProgress(int progressLevel, int timeoutAmount) {
		if(mMapView.mViewState == MapView.MapViewState.WAITING_FOR_LAYER_INITIALIZATION) {
			mProgressBar.setProgress(progressLevel);
			mTimeoutProgressBar.setProgress(timeoutAmount);
		}
    }

    public void ActiveLoadingMapWithProgress(int progressLevel) {
		mActiveProgressBar.setProgress(progressLevel);
    	if(progressLevel > 0) {
    	}
    	if(progressLevel >= 100) {
    		mActiveProgressBar.setVisibility(View.GONE);
    	}
    }

	@Override
	public void LoadMapDataComplete(String link) {
		// TODO Auto-generated method stub
		
	}   	
}
