package com.reconinstruments.geodataservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import com.reconinstruments.connect.messages.XMLMessage;
import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager;
import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager.DSMState;
import com.reconinstruments.geodataservice.datasourcemanager.DynamicData.UserTranscoder;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.geodataservice.devinterface.IDevTesting;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.hud_phone_status_exchange.PhoneStateMessage;

//import com.reconinstruments.hudservice.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.InformationRetrievalCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.GeoBuddyInfo;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.ObjectTypeList;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.SerializableLocation;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.SourcedObjectType;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.connect.messages.BuddyInfoMessage;
import com.reconinstruments.connect.messages.BuddyInfoMessage.BuddyInfo;


public class GeodataService extends Service implements DataSourceManager.IDataSourceManagerCallbacks 
{
// constants 
	private final static String TAG = "GeodataService";
	private static final boolean WAIT_FOR_DEBUGGER = false;
	public final static String GEODATASERVICE_START = "com.reconinstruments.geodataservice.start";
	public final static String GEODATASERVICE_BIND_CLIENT_INTERFACE = "com.reconinstruments.geodataservice.clientinterface";
	public final static String GEODATASERVICE_BIND_IDEV = "com.reconinstruments.geodataservice.developerinterface";
	public final static String GEODATASERVICE_BROADCAST_SERVICE_LOADING_DATA = "com.reconinstruments.geodataservice.serviceloadingdata";
	public final static String GEODATASERVICE_BROADCAST_SHUTTDOWN_REQUEST = "com.reconinstruments.geodataservice.shutdown";
	public final static String GEODATASERVICE_BROADCAST_STATE_CHANGED = "com.reconinstruments.geodataservice.state_changed";
	public final static String GEODATASERVICE_BROADCAST_REQUESTED_DATA_READY = "com.reconinstruments.geodataservice.requested_data_ready";
	public final static String GEODATASERVICE_BROADCAST_REQUESTED_DATA_LOAD_ERROR = "com.reconinstruments.geodataservice.requested_data_load_error";
	public final static String GEODATASERVICE_BROADCAST_NEW_DYNAMIC_USER_DATA = "com.reconinstruments.geodataservice.new_dynamic_user_data";
	public final static String GEODATASERVICE_BROADCAST_NEW_DYNAMIC_BUDDY_DATA = "com.reconinstruments.geodataservice.new_dynamic_buddy_data";
	public final static String FAKE_LOCATION_PROVIDER_REGISTERED = "com.reconinstruments.mocklocation.fake_location_registered";
	public final static String FAKE_LOCATION_PROVIDER_UNREGISTERED = "com.reconinstruments.mocklocation.fake_location_unregistered";
    public static final String PHONE_INTERNET_STATE = "com.reconinstruments.hudservice.PHONE_INTERNET_STATE";
    public static final String AGPS_REGISTERED = "com.reconinstruments.agps.AGPS_REGISTERED";

	private final static String RECON_AGPS_PROVIDER = "RECON_AGPS";
	private final static String RECON_FAKE_GPS_PROVIDER = "FakeGPS";
    public static final String LAST_DEV_TESTING_STATE = "LAST_DEV_TESTING_STATE";
    public static final Boolean USE_DEV_TESTING = true;
	private final static boolean FAKE_UI = false;
	private static final boolean FAKE_BUDDY = false;			// turn on to override buddy listening and force a single buddy at a set location
	private final static boolean RESET_DEV_DATA = false;
//	private final static String SELECTED_PROFILE = "snow";
	
    private final Semaphore mAccessToGetData = new Semaphore(1, true);

	  
	public enum DataInCacheState {
		NOT_IN_CACHE,
		IN_CACHE,
		LOADING_IN_BACKGROUND,
		JUST_INSIDE_CACHED_DATA_BOUNDARY_COULD_PRELOAD,
		JUST_INSIDE_LOADING_DATA_BOUNDARY_COULD_PRELOAD
	}


// members 
	private GeodataService			mThis = this;
	private GeoDataServiceState		mServiceState = new GeoDataServiceState();
	private ArrayList<Object> 		mNonInitializedSubcompontents = new ArrayList<Object>();// stores all subcomponents that haven't been fully initialized, used as checklist during init
	private boolean					mServiceStarted = false;
	private boolean					mPhoneNetworkAvailable = false;
	private SerializableLocation	mUserLocation = null;
	private double					mUserVelocity = 0;
	private TileDataRequest			mCachedDataRequest = null;
	
//	private ArrayList<WorldObject> 	mCachedWorldObjects = new ArrayList<WorldObject>();		// stores all the retrieved world objects - acts as master list across all loaded data requests so that only one copy of each WO is saved even though it may be pointed to by multiple DataRequests
	private String 					mLastErrorMessage = "";
	private DataSourceManager 		mDSM = null;
	private LocationManager 		mLocationManager = null;
	private UserTranscoder			mUserTranscoder = null;
	private boolean 				gpsActive = false;
	protected DevTestingState		mDevTestingState = null;
	private TextView				mMessageView = null;
	private Capability				mUserPositionCapability = new Capability(Capability.CapabilityTypes.DYNAMIC_USER_POSITION);
	private Capability				mBuddyTrackingCapability = new Capability(Capability.CapabilityTypes.DYNAMIC_BUDDY_POSITION);
	TreeMap<String, MapComposition> mMapCompositions = new TreeMap<String, MapComposition>();
	TreeMap<String, WorldObject> 	mCachedWorldObjects = new TreeMap<String, WorldObject>();
	private boolean					mGetDataIsBusySemaphore = false;
			TileDataRequest			mLoadingDataRequest = null;
			boolean					mUserLocationBusy = false;
	private boolean					mGPSProviderAvailable=false;
	private boolean					mAGPSProviderAvailable=false;
	private boolean					mFakeGPSProviderAvailable=false;
	private boolean					mStartServiceComplete = false;
	// TODO needed??
	float counter = 0;
	private long lastBuddyUpdate = 0, lastBuddyPull = 0;
	private Bundle mBuddyListBundle = null;
	private HashMap<Integer, GeoBuddyInfo> mBuddyMap;
	ArrayList<String> 				mBuddyClients = new ArrayList<String>();
// methods
	
	
//=============================================== 
// Service lifecycle  handlers
    @Override
    public void onCreate() {
    	super.onCreate();
		Log.i(TAG,"service created");
		mServiceStarted = false;
		
//		android.os.Debug.waitForDebugger();
    }
  
    @Override
    public int onStartCommand(Intent i, int flag, int startId)    {
    	// Register mMessageReceiver to receive messages.
		Log.i(TAG,"service starting");
		
		try {
			if(!mServiceStarted) startService();
		} 
		catch (Exception e) {
			serviceStartFailed(e);
		}
		
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(FAKE_LOCATION_PROVIDER_REGISTERED);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
    	registerReceiver(mFakeGpsChangeReceiver, filter);

    	filter = new IntentFilter();
    	filter.addAction(FAKE_LOCATION_PROVIDER_UNREGISTERED);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
    	registerReceiver(mFakeGpsChangeReceiver, filter);

    	filter = new IntentFilter();
    	filter.addAction(XMLMessage.BUDDY_INFO_MESSAGE);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
		registerReceiver(mBuddyInfoReceiver, filter);

    	filter = new IntentFilter();
    	filter.addAction(PHONE_INTERNET_STATE);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
		registerReceiver(mPhoneStatusChangedReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(AGPS_REGISTERED);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mAGPSProviderRegistered, filter);

    	return START_STICKY;
   }

	@Override
	public IBinder onBind(Intent intent) { 	// called by client to start service and bind to an interface

		try {
			if(!mServiceStarted) startService();

			Log.d(TAG,"binding request: " + intent.getAction() + "  | service started? " + mServiceStarted  );
			if(intent.getAction().compareTo(GEODATASERVICE_BIND_IDEV) == 0) {
				Log.i(TAG,"binding to IDevTesting with " + intent.getAction());
				return mBinderDev;
			}
			else {
				return mBinder;		// else bind to the clientinterface
			}
		} 
		catch (Exception e) {
			serviceStartFailed(e);
		}

		return null;
	}

	private void startService() throws Exception {
		mServiceStarted = true;
		
		if(WAIT_FOR_DEBUGGER) {
			android.os.Debug.waitForDebugger();
		} 

		mDevTestingState = new DevTestingState();
		if(RESET_DEV_DATA) {
			saveDevTestingSettings();
		}
		else {
			if(USE_DEV_TESTING) {
				SharedPreferences devTestingSettings = getSharedPreferences(LAST_DEV_TESTING_STATE, 0);		// read in old values if they exist, only saved during IDevTesting handling 
				if(devTestingSettings != null) {
					for(DevTestingState.TestingConditions condition : DevTestingState.TestingConditions.values()) {
						mDevTestingState.mTestingConditionState.set(condition.ordinal(), devTestingSettings.getInt(condition.toString(), 0));
					}
				}
			}
			else {
				mDevTestingState.mTestingConditionState.set(DevTestingState.TestingConditions.ENABLED.ordinal(), 0);	// turn off all dev_testing
			}
		}

		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
				mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.SERVICE_CREATE_ERROR.ordinal()) == 1) ) {
			throw new Exception ("Forced SERVICE creation error.");
		}

		if(mLocationManager == null) {
			//			Log.i(TAG, "starting LocationManager GPS service");
			mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0l, 0.f, locationListener);
			try {
				mLocationManager.requestLocationUpdates(RECON_FAKE_GPS_PROVIDER, 0l, 0.f, locationListener);
                mLocationManager.requestLocationUpdates(RECON_AGPS_PROVIDER, 0l, 0.f, locationListener);
			}
			catch (Exception e) {
				// do nothing as fake GPS / AGPS provider is just not registered (yet)... will try again if a intent is received
			}
		}


		mUserTranscoder = new UserTranscoder(mDSM, this, mDevTestingState);

		mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_STARTING);

        Log.i(TAG,"service state: " + mServiceState.GetState());

		initComponents();

		mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_INITIALIZING);
		Log.i(TAG,"service state: " + mServiceState.GetState());
		//		mMessageView = (TextView)findViewById(0x7f040000);
		//		mMessageView.setText("Geodata Service initializing...");

		NotifyClientsOfStateChange();
		
		mStartServiceComplete = true;
	}

	private void serviceStartFailed(Exception e) {
		// TODO Auto-generated catch block
//		e.printStackTrace();
		
		mDevTestingState = new DevTestingState();	// reset test values on create error to avoid endless loops and having to manually reset things
		saveDevTestingSettings();
		
		mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_SHUTTING_DOWN);
		NotifyClientsOfStateChange();

		Log.i(TAG,"service state: " + mServiceState.GetState());
		Log.e(TAG,"Terminating service.  Execption caught during Geodata initialization: " + e.toString());

		stopSelf();
//		Intent intent = new Intent();
//		intent.setAction(GEODATASERVICE_BROADCAST_SHUTTDOWN_REQUEST);
//		intent.addCategory(Intent.CATEGORY_DEFAULT);
//		sendBroadcast(intent); 		// request parent activity to stop this service - causes things to unbind properly...
    	
    }
	
//    @Override
//    public int onStopCommand(Intent i, int flag, int startId)    {
//    }

    @Override
    public void onDestroy() {
		Log.i(TAG,"service destroyed");
    	unregisterReceiver(mPhoneStatusChangedReceiver);
    	unregisterReceiver(mFakeGpsChangeReceiver);
    	unregisterReceiver(mBuddyInfoReceiver);
        unregisterReceiver(mAGPSProviderRegistered);

//		mMessageView.setText("Geodata Service stopped...");
    	super.onDestroy();

    }


//============================================
// support routines
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mFakeGpsChangeReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		Log.d(TAG,"in mFakeGpsChangeReceiver");
      		if(intent.getAction().toString().equalsIgnoreCase(FAKE_LOCATION_PROVIDER_REGISTERED)) {
    			try {
    				mLocationManager.requestLocationUpdates(RECON_FAKE_GPS_PROVIDER, 0l, 0.f, locationListener);
    			}
    			catch (Exception e) {
    				// it's possible, although unlikely that the mock location service will crash after sending out this intent and thus the above would fail.
    			}
     		}
      		
     		if(intent.getAction().toString().equalsIgnoreCase(FAKE_LOCATION_PROVIDER_UNREGISTERED)) {
     			// TODO... not sure how to undo the request for Location updates...
     		}
    	}
    };
    
    
   
    private BroadcastReceiver mPhoneStatusChangedReceiver = new BroadcastReceiver() {

    	@Override
    	public void onReceive(Context context, Intent intent) {

//    	   private void broadcastState(Context c, String type) { 
//        	Log.v(TAG,"Broadcast phone internet state " + type); 
//        	c.removeStickyBroadcast(sPhoneISIntent); 
//        	sPhoneISIntent.putExtra("Provider",type); 
//        	sPhoneISIntent.putExtra("IsConnected",!(type.equals("none"))); 
//        	c.sendStickyBroadcast(sPhoneISIntent); 
//        }
    		String provider = intent.getStringExtra("Provider");
			mPhoneNetworkAvailable = intent.getBooleanExtra("IsConnected", false);

    		Log.d(TAG, "mPhoneStatusChangedReceiver PHONE_STATE_CHANGED: " + mPhoneNetworkAvailable + " from provider: " + provider);
    		if(mDSM != null) {
    			mDSM.phoneNetworkIsAvailable(mPhoneNetworkAvailable);
    		}
    	}
    };
    
    private void initComponents() throws Exception {
    	
		mNonInitializedSubcompontents.add(mDSM = new DataSourceManager(this, mDevTestingState)); // spawns task, then calls back IDataSourceManager.InitializeComplete() when done
		
    	mUserTranscoder.init();
    	mUserTranscoder.AddCapabilities(mServiceState.mCapabilities);
    	
		mDSM.init(mServiceState.mCapabilities); // spawns task, then calls back IDataSourceManager.InitializeComplete() when done
										// adds all dataRetrievalCapabilities to capabilities list
					 					// require separate creator and init() so to avoid init finished race condition during errors

		// initGC();
    	// initUserModel();
   }
    
    private void reportErrorDuringInitialization(Object component, String errorMsg) {		
		mServiceState.SetState(GeoDataServiceState.ServiceStates.ERROR_DURING_SERVICE_INITIALIZATION);
		NotifyClientsOfStateChange();

		Log.i(TAG,"service state: " + mServiceState.GetState());

    	if(mLastErrorMessage == null) {
    		mLastErrorMessage = errorMsg;
    	}
    	else {
    		mLastErrorMessage = mLastErrorMessage + "; " + errorMsg;  // compile all error messages together
    	}
    }
    
    private void checkForEndOfInitialization() {		
    	if(mNonInitializedSubcompontents.size() == 0) {

			if(mDSM.mState == DataSourceManager.DSMState.READY_NO_USER_LOCATION) {
				mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION);
			}
			if(mDSM.mState == DataSourceManager.DSMState.READY_WITH_USER_LOCATION_LOADING_DATA) {
				mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA);
				EnableUserPositionCapacity();
			}
			if(mDSM.mState == DataSourceManager.DSMState.READY_WITH_USER_LOCATION) {
				mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION);
				EnableUserPositionCapacity();
			}
			Log.i(TAG,"service state: " + mServiceState.GetState());
			NotifyClientsOfStateChange();

    	}
    }
    
     
  //============================================
 // capability change routines
    
	void NotifyClientsOfStateChange() {
		// broadcast change notification to all listening clients
		Intent intent = new Intent();
		intent.setAction(GEODATASERVICE_BROADCAST_STATE_CHANGED);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
//		intent.putExtra		// specify what has changed?
		sendBroadcast(intent); 				// allows clients to getState again and update capabilities
	}
	
	public void BroadcastDataLoadPercentage(float loadedPercentage) {
		// broadcast change notification to all listening clients
		Intent intent = new Intent();
		intent.setAction(GEODATASERVICE_BROADCAST_SERVICE_LOADING_DATA);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra("percentLoaded", (double)loadedPercentage);		
		sendBroadcast(intent); 				// allows clients to getState again and update capabilities
	}
	
	
	void EnableUserPositionCapacity() {
		if(!mUserPositionCapability.mEnabled) {
			mUserPositionCapability.Enable();
		}
	}
	void DisableUserPositionCapacity() {
		if(mUserPositionCapability.mEnabled) {
			mUserPositionCapability.Disable();
		}
	}
	
	void NotifyClientsOfUserPositionChange(double longitude, double latitude, double velocity) {
		// broadcast change notification to all listening clients
		Intent intent = new Intent();
		intent.setAction(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_USER_DATA);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra("newLongitude", longitude);		
		intent.putExtra("newLatitude", latitude);		
		intent.putExtra("newVelocity", velocity);		
		sendBroadcast(intent); 				// allows clients to getState again and update capabilities
	}
	
	void NotifyClientsOfBuddyChange() {
		// broadcast change notification to all listening clients
		Intent intent = new Intent();
		intent.setAction(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_BUDDY_DATA);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(intent); 				// allows registered clients to poll for new buddies if they wish
		Log.d(TAG,"NotifyClientsOfBuddyChange");
	}
	
//===============================================
// Interfaces call-ins and call-backs

//   DataSourceManager.IDataSourceManagerCallbacks
    
   public void DSMInitializeComplete(Object dsm) {		
		Log.i(TAG,"DSMInitializeComplete");
		Iterator itr = mNonInitializedSubcompontents.iterator();
		while(itr.hasNext()){
			if(itr.next().equals(dsm))  {
				itr.remove();
			}
		}
		
		checkForEndOfInitialization();
	}
    public void DSMInitializationError(Object component, String errorMsg) {
		// TODO handle error case
		Log.e(TAG,"DSMInitializationError");
		reportErrorDuringInitialization(component, errorMsg);
	}
	
	public void DSMLocationStateChanged(DSMState state) {
		if(state == DataSourceManager.DSMState.READY_WITH_USER_LOCATION_LOADING_DATA && 
		  mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION ) {
			mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA);
			EnableUserPositionCapacity();
			Log.i(TAG,"service state: " + mServiceState.GetState());
			NotifyClientsOfStateChange();
		}
		
		if(state == DataSourceManager.DSMState.READY_WITH_USER_LOCATION && 
		  (mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION ||
		  mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA)) {
			mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION);
			EnableUserPositionCapacity();
			Log.i(TAG,"service state: " + mServiceState.GetState());
			NotifyClientsOfStateChange();
		}
	}
	
	
//  IDevTesting interface for developers -  defined through IDL (aidl)
   private final IDevTesting.Stub mBinderDev = new IDevTesting.Stub()
	{
		public boolean setDevTestingState(DevTestingState _devTestingState) {
			if(_devTestingState == null) return false;
			
			mDevTestingState = _devTestingState;		// save new testing state (overwrite old)
			mDSM.setDevTestingState(_devTestingState);	// propagate new DevTestState to DSM and its subcomponents
			saveDevTestingSettings();					// save test state preferences to allow testing of startup conditions... require manual kill process
			return true;
		}
		public DevTestingState getDevTestingState() {
			assert mDevTestingState != null;
			return mDevTestingState;
		}

	};

	private void saveDevTestingSettings() {
		SharedPreferences devTestingSettings = getSharedPreferences(LAST_DEV_TESTING_STATE, 0);
		SharedPreferences.Editor editor = devTestingSettings.edit();
		for(DevTestingState.TestingConditions condition : DevTestingState.TestingConditions.values()) {
			editor.putInt(condition.toString(), mDevTestingState.mTestingConditionState.get(condition.ordinal()) );
		}
		editor.commit();
	}

	 
	
//   IGeodataService interface for clients-  defined through IDL (aidl)
    private final IGeodataService.Stub mBinder = new IGeodataService.Stub()
	{
		public IGeodataServiceResponse getServiceState() {
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			
//			Log.e(TAG,"In getServiceState response on server -----------------------");
			response.mResponseCode = IGeodataServiceResponse.ResponseCodes.REQUEST_COMPLETED;
			response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.SERVICE_STATE;
			if(mServiceState.mState == GeoDataServiceState.ServiceStates.ERROR_WITH_SERVICE ||
			   mServiceState.mState == GeoDataServiceState.ServiceStates.ERROR_DURING_SERVICE_INITIALIZATION) {
				response.mErrorMessage = mLastErrorMessage;
			}
			response.mServiceState = mServiceState;
			return response;
		}
		
		public IGeodataServiceResponse getClosestResorts(ResortRequest _resortRequest) {
			
			Log.d(TAG,"In getClosestResorts: " + _resortRequest.mLocation.x + "," + _resortRequest.mLocation.y + " - " + _resortRequest.mDataSource);
			
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			
			// first validate that requested capability exists
			boolean capabilityExists = false;
			for(Capability capability : mServiceState.mCapabilities) {
				if(capability instanceof InformationRetrievalCapability && 
				   ((InformationRetrievalCapability)capability).mInfoType == InformationRetrievalCapability.InfoRetrievalTypes.CLOSEST_RESORTS && 
				   ((InformationRetrievalCapability)capability).mDataSource == _resortRequest.mDataSource) {
					capabilityExists = true;
					break;
				}
			}
			if(!capabilityExists) {
				response.mResponseCode = IGeodataServiceResponse.ResponseCodes.CAPABILITY_NOT_AVAILABLE;
				return response;
			}
			
			// then 
			switch(mServiceState.GetState()) {
				case SERVICE_READY_WITH_NO_USER_LOCATION: 
				case SERVICE_READY_WITH_STALE_USER_LOCATION: 
				case SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA:
				case SERVICE_READY_WITH_USER_LOCATION: {
					try {
						response.mResponseCode = IGeodataServiceResponse.ResponseCodes.REQUEST_COMPLETED;
						response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.ARRAY_RESORTINFO;
						ArrayList<ResortInfoResponse> resorts = (ArrayList<ResortInfoResponse>) mDSM.GetClosestResorts(_resortRequest);
						Log.d(TAG, "Returning " + resorts.size() + " resorts");
						response.mResortsArray = resorts;
					}
					catch (Exception e){
						response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_DURING_REQUEST;
						response.mResortsArray = null;
					}
					
					break;
				}
				case ERROR_DURING_SERVICE_INITIALIZATION: 
				case ERROR_WITH_SERVICE: {
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_WITH_SERVICE;
					response.mErrorMessage = mLastErrorMessage;
					break;
				}
				default: {
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.SERVICE_NOT_READY;
				}
			}
			return response;
			
		}

		
		public IGeodataServiceResponse defineMapComposition(String _clientDefinedID, ObjectTypeList _objectTypeList) {
//			Log.d(TAG,"In defineMapComposition response on server -----------------------");
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			
			switch(mServiceState.GetState()) {
				case ERROR_DURING_SERVICE_INITIALIZATION: 
				case ERROR_WITH_SERVICE: {
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_WITH_SERVICE;
					response.mErrorMessage = mLastErrorMessage;
					break;
				}
				default: {
					MapComposition newMapComposition = new MapComposition(_clientDefinedID,  _objectTypeList);
					
					if(mMapCompositions.get(_clientDefinedID) != null) {		// if previously defined, remove it, start fresh
						mMapCompositions.remove(_clientDefinedID);
					}
					
					mMapCompositions.put(_clientDefinedID, newMapComposition);	
					
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.REQUEST_COMPLETED;
					response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.STRING;
					response.mStringData = _clientDefinedID;
				
					break;
				}
			}
			return response;
		}

		public IGeodataServiceResponse releaseMapComposition(String _mapCompositionID) {
//			Log.d(TAG,"In releaseMapComposition response on server -----------------------");
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			switch(mServiceState.GetState()) {
				case SERVICE_READY_WITH_NO_USER_LOCATION: 
				case SERVICE_READY_WITH_STALE_USER_LOCATION: 
				case SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA:
				case SERVICE_READY_WITH_USER_LOCATION: {
//					for(DataRequest tileDataRequest : mMapCompositions.get(_mapCompositionID).mLoadingDataRequests) {	
//						tileDataRequest.Clean();  // release array objs so GC is free to collect parents
//						tileDataRequest = null;
//					}			
					mMapCompositions.remove(_mapCompositionID);	
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.REQUEST_COMPLETED;
					response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;
					break;
				}
				case ERROR_DURING_SERVICE_INITIALIZATION: 
				case ERROR_WITH_SERVICE: {
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_WITH_SERVICE;
					response.mErrorMessage = mLastErrorMessage;
					break;
				}
				default: {
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.SERVICE_NOT_READY;
				}
			}
			return response;
		}

		
		public IGeodataServiceResponse getMapTileData(int _geoTileIndex, String _mapCompositionID) {	// expecting to be running in a background task within the client
			
//			Log.d(TAG,"In getMapData response on server -----------------------");
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			
//			Log.d(TAG,"   using map composition ID:" + _mapCompositionID); 
//			GeoRegion gr = _geoRegion;
//			Log.d(TAG,"getMapData(): " + gr.mBoundingBox.left + ", " + gr.mBoundingBox.right + ", " + gr.mBoundingBox.top + ", " + gr.mBoundingBox.bottom);
//			Log.d(TAG,"getMapData(): tile index=" + _geoTileIndex);
			
			// trap bad request
			if(_mapCompositionID == null || mMapCompositions.get(_mapCompositionID)==null || 
				(mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
  				 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MAPDATA_REQUEST_BADLY_FORMED.ordinal()) == 1) ) {
				Log.e(TAG,"in error case: " + mMapCompositions.get(_mapCompositionID));
				response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_WITH_REQUEST;
				response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;
				return response;
			}
				
			switch(mServiceState.GetState()) {
				case SERVICE_STARTING: 
				case SERVICE_SHUTTING_DOWN: 
				case SERVICE_INITIALIZING: {
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.SERVICE_NOT_READY;
					break;
				}
				case ERROR_DURING_SERVICE_INITIALIZATION: 
				case ERROR_WITH_SERVICE: {
					response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_WITH_SERVICE;
					response.mErrorMessage = mLastErrorMessage;
					break;
				}
				case SERVICE_READY_WITH_NO_USER_LOCATION: 
				case SERVICE_READY_WITH_STALE_USER_LOCATION: 
				case SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA:
				case SERVICE_READY_WITH_USER_LOCATION: {
					TileDataRequest newTileRequest = new TileDataRequest(_geoTileIndex, mMapCompositions.get(_mapCompositionID));   // change _geoRegion to _geoTileIndex

					// !!! from this point on...code needs to be thread safe !! 
					
					try {
						response.mGeoTile = mDSM.SourceTileData(newTileRequest);   // needs to be thread safe					

						response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.GEOTILE;
						response.mResponseCode = IGeodataServiceResponse.ResponseCodes.LOADDATAREQUEST_DATA_ATTACHED;  
//						Log.i(TAG,"DSMSourcingDataComplete for tile index ...");
					}
					catch (TileLoadingFromNetworkException e) {
						response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;
						response.mResponseCode = IGeodataServiceResponse.ResponseCodes.TILE_LOADING_FROM_NETWORK_TRY_LATER;  
					}		 
					catch (Exception e) {
//							Log.e(TAG,"DSMSourcingDataError");
						response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;				
						response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_DURING_REQUEST;
					}
					
					return response;
				}
			}
			return response;
		}
		
//		public IGeodataServiceResponse getMapPOIObjects(GeoRegion _geoRegion) {
//			IGeodataServiceResponse response = new IGeodataServiceResponse();
//			return response;
//		}
		public IGeodataServiceResponse getUserLocation(){
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			if(mUserLocation == null) {
				response.mResponseCode = IGeodataServiceResponse.ResponseCodes.ERROR_WITH_REQUEST;
				response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;				
			}
			else {
				response.mResponseCode = IGeodataServiceResponse.ResponseCodes.LOADDATAREQUEST_DATA_LOADING;  
				response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;
				NotifyClientsOfUserPositionChange(mUserLocation.mLongitude, mUserLocation.mLatitude, mUserVelocity);		// broadcast of data
				Log.d(TAG, "GeodataService - getUserLocation: " + mUserLocation.mLongitude + ", " + mUserLocation.mLatitude);
			}
			return response;
		}
		public IGeodataServiceResponse getBuddies(){
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			if(FAKE_BUDDY) {
				fakeBuddy();	// overrides mBuddyMap
			}
			lastBuddyPull = lastBuddyUpdate;
			if(lastBuddyUpdate != 0) {
				response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.ARRAY_BUDDYINFO;
				response.mResponseCode = IGeodataServiceResponse.ResponseCodes.BUDDYREQUEST_BUDDIES_ATTACHED;  
				response.mBuddyArray = new ArrayList<GeoBuddyInfo>();
				for(GeoBuddyInfo buddy : mBuddyMap.values()) {
					response.mBuddyArray.add(buddy);
				}
			}
			else {
				response.mDataClassId = IGeodataServiceResponse.ResponseDataClassIDs.NO_DATA;
				response.mResponseCode = IGeodataServiceResponse.ResponseCodes.BUDDYREQUEST_NO_CONNECTION;  // no connection yet, ie, no data because of no connection
			}
			Log.d(TAG,"getBuddies, lastBuddyUpdate="+lastBuddyUpdate+", respose="+response.mResponseCode);
			return response;
		}
		public IGeodataServiceResponse registerForBuddies(String processID) {
			mBuddyClients.add(processID);
			Log.d(TAG, "New buddy client registrant. Total registered processes = " + mBuddyClients.size());
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			NotifyClientsOfBuddyChange();
			return response;
		}
		public IGeodataServiceResponse unregisterForBuddies(String processID) {
			mBuddyClients.remove(processID);
			Log.d(TAG, "Buddy client unregistrant. Total registered processes = " + mBuddyClients.size());
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			return response;
		}
		public IGeodataServiceResponse getClosestItem() {
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			return response;
		}
		public IGeodataServiceResponse getRoute() {
			IGeodataServiceResponse response = new IGeodataServiceResponse();
			return response;
		}


	};
	
	//=============== Buddy receiver
	// Define a listener that responds to buddy broadcasts
	BroadcastReceiver mBuddyInfoReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Bundle bundle = intent.getExtras();
			if(bundle!=null){
				Log.d(TAG, "mBuddyInfoReceiver.onReceive.");
				byte[] bytes = bundle.getByteArray("message");
				HUDConnectivityMessage cMsg = new HUDConnectivityMessage(bytes);
				String buddyInfo = new String(cMsg.getData());

				BuddyInfoMessage message = new BuddyInfoMessage();

				@SuppressWarnings("unchecked")
				ArrayList<BuddyInfo> buddyList = (ArrayList<BuddyInfo>) message.parse(buddyInfo);

				//			Log.i(TAG, "onReceive()");
				//			Log.i(TAG, "buddy count: " + buddyList.size());
				// int index = 0;
				//			for (BuddyInfo buddy : buddyList)
				//			{
				//				Log.v(TAG, "buddy[" + (/++) + "]: id=" + buddy.localId + ", name=" + buddy.name + ", email=" + buddy.email + 
				//						   ", location=" + buddy.location.getLatitude() + " " + buddy.location.getLongitude());
				//			}

				if(mBuddyMap == null) {
					mBuddyMap = new HashMap<Integer, GeoBuddyInfo>();
				}

				mBuddyMap.clear();

				for (BuddyInfo buddy : buddyList) { 
					GeoBuddyInfo buddyInMap = new GeoBuddyInfo(buddy.localId, buddy.name, buddy.location.getLatitude(), buddy.location.getLongitude());
					mBuddyMap.put(buddyInMap.mID, buddyInMap);
				}			

//				mBuddyListBundle = constructBundleFromBuddyInfo(mBuddyMap);
				lastBuddyUpdate = System.currentTimeMillis();	

				if(mBuddyClients.size() > 0) {
					NotifyClientsOfBuddyChange();
				}
			}
		}
	};

//	private static final Bundle constructBundleFromBuddyInfo(HashMap<Integer, BuddyInfo> buddyMap) {
//
//		ArrayList<Bundle> blist = new ArrayList<Bundle>();
//
//
//		for (BuddyInfo bi : buddyMap.values()) {
//			if (bi != null){
//				Bundle bib = new Bundle();
//				bib.putInt("id", bi.mID);
//				bib.putString("email", bi.mEmail);
//				bib.putString("name", bi.mName);
//				bib.putDouble("location",bi.mLocation);
//				blist.add(bib);	
//			}
//		}
//		Bundle b = new Bundle();
//		b.putParcelableArrayList("BuddyInfoBundle",blist);
//		return b;
//	}
//
	public void fakeBuddy()
	{
		if(mBuddyMap == null)
		{
			mBuddyMap = new HashMap<Integer, GeoBuddyInfo>();
			mBuddyMap.clear();

			double latitude = 50.108333;   // Whistler
			double longitude = -122.9425;
//			double latitude = 49.276793;   // Recon HQ
//			double longitude = -123.121186;

			GeoBuddyInfo fakeBuddy  = new GeoBuddyInfo(99,"faker1",latitude-0.006, longitude-0.006);	// fake buddy for now..
			mBuddyMap.put(fakeBuddy.mID, fakeBuddy);

			fakeBuddy  = new GeoBuddyInfo(98,"faker2",latitude+0.006, longitude+0.006);	// fake buddy for now..
			mBuddyMap.put(fakeBuddy.mID, fakeBuddy);

//			mBuddyListBundle = constructBundleFromBuddyInfo(mBuddyMap);
		}
		lastBuddyUpdate = System.currentTimeMillis();		
	}


	//=============== Location Services protocol handlers
	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			String provider = location.getProvider();
//			Log.i(TAG,"gps data from provider " + provider);
//			mGPSProviderAvailable = true;
			if(mStartServiceComplete ) {
				if(provider.equalsIgnoreCase(RECON_FAKE_GPS_PROVIDER) ) {
					mFakeGPSProviderAvailable=true;
					handleLocationChange(location);
				}
				else if(provider.equalsIgnoreCase(mLocationManager.GPS_PROVIDER) && !mFakeGPSProviderAvailable) {
					mGPSProviderAvailable=true;
					handleLocationChange(location);
				}
				else if(provider.equalsIgnoreCase(RECON_AGPS_PROVIDER) && !mFakeGPSProviderAvailable && !mGPSProviderAvailable) {
					mAGPSProviderAvailable=true;
					handleLocationChange(location);
				}
			}
		}

		public void onProviderEnabled(String provider) {
			Log.i(TAG,"gpsLocationProvider "+ provider +" enabled");
			if(provider.equalsIgnoreCase(mLocationManager.GPS_PROVIDER)) {
				mGPSProviderAvailable=true;
			}
			if(provider.equalsIgnoreCase(RECON_AGPS_PROVIDER)) {
				mAGPSProviderAvailable=true;
			}
			if(provider.equalsIgnoreCase(RECON_FAKE_GPS_PROVIDER)) {
				mFakeGPSProviderAvailable=true;
			}
		}
		public void onProviderDisabled(String provider) {
			Log.i(TAG,"gpsLocationProvider "+ provider +" disabled");		 
			if(provider.equalsIgnoreCase(mLocationManager.GPS_PROVIDER)) {
				mGPSProviderAvailable=false;
			}
			if(provider.equalsIgnoreCase(RECON_AGPS_PROVIDER)) {
				mAGPSProviderAvailable=false;
			}
			if(provider.equalsIgnoreCase(RECON_FAKE_GPS_PROVIDER)) {
				mFakeGPSProviderAvailable=false;
			}
			if(!mGPSProviderAvailable && !mAGPSProviderAvailable && !mAGPSProviderAvailable) {
				handleAllProvidersDisabled();
			}
		}
		public void onStatusChanged(String provider, int status, Bundle extras) {
//			if(status == LocationProvider.AVAILABLE ) {  // request for location stays registered with the LM when disabled and will resume automatically so no need to re-request...
//				onProviderEnabled(provider);
//			}
//			else {
//				onProviderDisabled(provider);
//			}
		}
	};

    private BroadcastReceiver mAGPSProviderRegistered = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Registered AGPS provider, requesting location updates from AGPS... ");
            mLocationManager.requestLocationUpdates(RECON_AGPS_PROVIDER, 0l, 0.f, locationListener);
        }
    };


	void handleLocationChange(Location location) {
		if(!(mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 &&
				mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.SERVICE_NO_GPS.ordinal()) == 1) ) {	 // if not blocked by testing configuration

			if(mUserLocationBusy == false) {
				mUserLocationBusy = true;

				mUserLocation = new SerializableLocation(location.getLongitude(), location.getLatitude());
				mUserTranscoder.SetLocation((float)location.getLongitude(), (float)location.getLatitude());	  // will save location and broadcast change to registered clients if change is significant 
				mUserVelocity = location.getSpeed();
				
				if(mDSM != null) {	// send location to datasources in case they preload data or some other action 
					try {
						mDSM.SetUserLocation((float)location.getLongitude(), (float)location.getLatitude());  // will throw exception if can't access data files/cache
					} 
					catch (Exception e) {
						// error during data prefetch in data source

						// TODO - clarify what is appropriate response here... change state and send notification?? ie crash out??? or try to recover...  depends... file errors = crash...
						if(mServiceState.mState != GeoDataServiceState.ServiceStates.ERROR_WITH_SERVICE) {
							mLastErrorMessage = e.getMessage();
						}

						mServiceState.SetState(GeoDataServiceState.ServiceStates.ERROR_WITH_SERVICE);
						NotifyClientsOfStateChange();
						e.printStackTrace();
					}
				}
				//					if(!gpsActive) {
				//						Log.d(TAG, "have user location - onLocationChanged");
				//					}
				gpsActive = true;
				if(mDSM.mState == DataSourceManager.DSMState.READY_WITH_USER_LOCATION_LOADING_DATA && 
				mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION ) {
					mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA);
					EnableUserPositionCapacity();

					Log.i(TAG,"service state: " + mServiceState.GetState());
					NotifyClientsOfStateChange();

				}
				
				if(mDSM.mState == DataSourceManager.DSMState.READY_WITH_USER_LOCATION && 
				 (mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION ||
				  mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA)) {
					mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION);
					EnableUserPositionCapacity();
					Log.i(TAG,"service state: " + mServiceState.GetState());
					NotifyClientsOfStateChange();

				}

//				Log.e(TAG, "Geodata new location: "+ location.getLongitude() +", " + location.getLatitude() +  "  speed: " + location.getSpeed());
				NotifyClientsOfUserPositionChange(location.getLongitude(), location.getLatitude(), location.getSpeed());
			}
			mUserLocationBusy = false;
		}		
	}

	void handleAllProvidersDisabled() {
		gpsActive = false;	// TODO figure out how to respond with stale data
		Log.d(TAG, "lost user location - onProviderDisabled");
//		if(mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION ||
//		   mServiceState.GetState() == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA) {
//			mServiceState.SetState(GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION);
//			mUserLocation = null;
//			DisableUserPositionCapacity();
//
//			Log.d(TAG,"service state: " + mServiceState.GetState());
//		}
	}

	protected class WaitTask extends AsyncTask<Void, Void, String> {
		int mTime;
		String mMapCompositionID; 
		
		public WaitTask(int mSec, String mapCompositionID) {
			mTime = mSec;
			mMapCompositionID = mapCompositionID;
	    }

		protected String doInBackground(Void...voids)  {
			try {
				Thread.sleep(mTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "";
		}

		protected void onPostExecute(String endString) {
			IGeodataServiceResponse rc;
//			try {
////				rc = mBinder.getMapData(new GeoRegion().MakeUsingBoundingBox(-123.6f, 49.42f, -123.0f, 49.35f), mMapCompositionID) ;
//			} catch (RemoteException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}	
		}
	}
}
