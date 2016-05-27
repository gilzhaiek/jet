package com.reconinstruments.geodatatestapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.objecttype.ObjectTypeList;
import com.reconinstruments.geodataservice.clientinterface.objecttype.SourcedObjectType;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class GeodataTestAppActivity extends Activity {
	private final static String TAG = "GeoDataTestApp";
	public final static String GEODATASERVICE_START = "com.reconinstruments.geodataservice.start";
	public final static String GEODATASERVICE_BROADCAST_STATE_CHANGED = "com.reconinstruments.geodataservice.state_changed";
	private final static int RUNNING_COLOR = 0xFF208020;
	private final static int NOT_RUNNING_COLOR = 0xFF802020;
	private final static int ERROR_COLOR = 0xFFE01010;

	protected enum GeodataServerState { // corresponds with getServerState return code
		SERVICE_INITIALIZING,
		WAITING_FOR_LOCATION,
		SERVICE_READY,    //ie, initialized with GPS)
		SERVICE_SHUTTINGDOWN,
		SERVER_ERROR_WITH_SERVICE
	}

//========================================
// member definitions 

	private static GeodataServiceConnection	mGeodataServiceConnection = null;
	private static IGeodataService			mGeodataServiceInterface  = null;
	private ArrayList<String>				mGeodataApiCallNames = new ArrayList<String>();
	private int								mCurrentListViewIndex = 0;

	private ArrayList<ApiCall>				mGeodataApiCalls = new ArrayList<ApiCall>();
																

	// UI objects
	private ListView 	mListview;
	private TextView	mServiceStateView = null;
	private TextView	mServiceResponseView = null;
	
	private String 		mMapCompositionID = "";
//	
///	ArrayList<RectF>    mResortBBs = new ArrayList<RectF>();
//	ArrayList<PointF>	mAtResortLocation = new ArrayList<PointF>();
//	String				prevResort = "";
//	ResortQualifier		prevQualifier = ResortQualifier.NONE; 
//	boolean				mGPSEnabled = true;
//	PointF 				mResortCenter = new PointF(0.f,0.f);
//	String 				mSelectedResort = "";
//	
//	protected enum ResortQualifier {
//		NONE,
//		IN_RESORT,
//		NEAR_RESORT,
//		RESORT_IS_CLOSEST
//	}
	
//========================================
// private class definitions 
	
	private class StableArrayAdapter extends ArrayAdapter<String> {

	    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

	    public StableArrayAdapter(Context context, int textViewResourceId,
	        List<String> objects) {
	      super(context, textViewResourceId, objects);
	      for (int i = 0; i < objects.size(); ++i) {
	        mIdMap.put(objects.get(i), i);
	      }
	    }

	    @Override
	    public long getItemId(int position) {
	      String item = getItem(position);
	      return mIdMap.get(item);
	    }

	    @Override
	    public boolean hasStableIds() {
	      return true;
	    }
	}

	private class ApiCall {
		String 				name;
		ApiCallInterface	callInterface;
		
		public ApiCall(String _name, ApiCallInterface _callInterface) {
			name = _name;
			callInterface = _callInterface;
		}
		
//		public void CallService {
//			callInterface.sendMsg();
//		}
	}
	
	interface 	ApiCallInterface {
		void sendMsg();
	}


//======================================================
// activity lifecycle methods
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.geodata_testapp);

		mServiceStateView = (TextView)findViewById(R.id.service_state);
		mServiceStateView.setText("Geodata Service offline...");
		mServiceStateView.setTextColor(NOT_RUNNING_COLOR);

		mServiceResponseView = (TextView)findViewById(R.id.service_response);
		mServiceResponseView.setTextColor(Color.GRAY);
		mServiceResponseView.setText("Select an action below...");

		
//		Intent intent = new Intent();
//		intent.setAction("com.reconinstruments.mocklocationclient.change_location");
//		intent.addCategory(Intent.CATEGORY_DEFAULT);
//		intent.putExtra("GPSstate", true);
//		intent.putExtra("Latitude", 0.);
//		intent.putExtra("Longitude", 0.);
//		sendBroadcast(intent); 		// force GPS on in case it was disabled...

//		if(MAPS_APP_TESTING) {		// add ability to turn enable and disable GPS
//			mResortNames.add(GPS_STRING);
//			mResortBBs.add(new RectF(0.f,0.f,0.f,0.f));
//			mAtResortLocation.add(new PointF(0.f,0.f));
//			mNearResortLocation.add(new PointF(0.f,0.f));
//			mClosestResortLocation.add(new PointF(0.f,0.f));
//		}

		// set up desired apicalls and response methods
		mGeodataApiCalls.add(new ApiCall("get ServiceState",	new ApiCallInterface() { public void sendMsg() { getServiceState(); } }) );
		mGeodataApiCalls.add(new ApiCall("set MapComposition",  new ApiCallInterface() { public void sendMsg() { setMapComposition();} }) );
		mGeodataApiCalls.add(new ApiCall("get existing MapData",new ApiCallInterface() { public void sendMsg() { getExistingMapData();} }) );
		mGeodataApiCalls.add(new ApiCall("get new MapData", 	new ApiCallInterface() { public void sendMsg() { getNonExistingMapData();} }) );
		mGeodataApiCalls.add(new ApiCall("get Buddies", 		new ApiCallInterface() { public void sendMsg() { getBuddies(); } }));
		mGeodataApiCalls.add(new ApiCall("get Closest item", 	new ApiCallInterface() { public void sendMsg() { getClosestItem(); } }));
		mGeodataApiCalls.add(new ApiCall("get Route to..", 		new ApiCallInterface() { public void sendMsg() { getRoute(); } }));
		mGeodataApiCalls.add(new ApiCall("fake Buddy update", 	new ApiCallInterface() { public void sendMsg() { fakeBuddy(); } }));
		mGeodataApiCalls.add(new ApiCall("fake UserLocation", 	new ApiCallInterface() { public void sendMsg() { fakeUserLocation(); } }));
		mGeodataApiCalls.add(new ApiCall("fake UserHeading", 	new ApiCallInterface() { public void sendMsg() { fakeUserHeading(); } }));
		mGeodataApiCallNames = GetAPICallListForDisplay(mGeodataApiCalls);

		mListview = (ListView) findViewById(R.id.api_calls);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, mGeodataApiCallNames);
		//	    final StableArrayAdapter adapter = new StableArrayAdapter(this,android.R.layout.simple_list_item_1, mResortNames);
		mListview.setAdapter(adapter);
		mListview.setSelection(0);
		mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

				mCurrentListViewIndex = position;
				mGeodataApiCalls.get(mCurrentListViewIndex).callInterface.sendMsg();
			}

		});
		
	}
	 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.geodata_testapp, menu);
		return true;
   }
	
	public void onResume() {
		super.onResume();

//		Log.i(TAG,"onResume "+System.currentTimeMillis());
//		if(mLocationManager == null) {
//			mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
//		}    	

		BindGeodataService();  // call only after activity has been created
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}


	public void onDestroy(){
		UnbindGeodataService();
		
		super.onDestroy();
		Log.d(TAG,"onDestroy");
			
	}
	
//======================================================
// support methods

	private void SendMsgToService(int serviceIndex) {

		
		switch(mCurrentListViewIndex) {
//		case IN_RESORT:
//			mResortCenter = mAtResortLocation.get(mCurrentListViewIndex);
//			break;
//		case NEAR_RESORT:
//			mResortCenter = mNearResortLocation.get(mCurrentListViewIndex);
//			break;
//		case RESORT_IS_CLOSEST:
//			Intent intent = new Intent();
//			intent.setAction("com.reconinstruments.mocklocationclient.change_location");
//			intent.addCategory(Intent.CATEGORY_DEFAULT);
////			intent.putExtra("GPSstate", (boolean)mGPSEnabled);
////			intent.putExtra("Latitude", (double)mResortCenter.y);
////			intent.putExtra("Longitude", (double)mResortCenter.x);
//			sendBroadcast(intent); 
////			Log.i(TAG,"broadcasting change_location: "+ mSelectedResort +": " + mResortCenter.y+ ", "+ mResortCenter.x);
//			break;
		}

	}
	public ArrayList<String> GetAPICallListForDisplay(ArrayList<ApiCall> apiCallArray) {
		ArrayList<String> newList = new ArrayList<String>();
		for(ApiCall apiCall : apiCallArray)	{
			newList.add(apiCall.name);
		}
		return newList;
				
	}

	public void getServiceState() {
		mServiceResponseView.setTextColor(Color.BLACK);
		IGeodataServiceResponse response;
		try {
			response = mGeodataServiceInterface.getServiceState();
			GeoDataServiceState responseStateObj = (GeoDataServiceState)response.mData;
			String capString = responseStateObj.mState + ": ";
			for(Capability cap : responseStateObj.mCapabilities) {
				capString = capString + ", " + cap.mType;
			}
			mServiceResponseView.setText("response to getServiceState is "+ response.mResponseCode + ", capabilities: (" + responseStateObj.mCapabilities.size() + ") " + capString);
		} 
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			mServiceResponseView.setText("error with getServiceState ");
			e.printStackTrace();
		}
	}
	
	public void setMapComposition() {
		IGeodataServiceResponse response;
		try {
			ObjectTypeList objectTypeList = new ObjectTypeList();
			objectTypeList.mObjectTypes.add(new SourcedObjectType(WorldObjectTypes.CARACCESS_PARKING, Capability.DataSources.RECON_SKI_DATA));
			objectTypeList.mObjectTypes.add(new SourcedObjectType(WorldObjectTypes.CHAIRLIFT, Capability.DataSources.RECON_SKI_DATA));
			
			response = mGeodataServiceInterface.defineMapComposition("exampleClientID", objectTypeList);
			
			mMapCompositionID = (String)(response.mData);
			mServiceResponseView.setText("response to setMapComposition is "+ response.mResponseCode + ", map comp ID: " + mMapCompositionID);
		} 
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			mServiceResponseView.setText("error with setMapComposition ");
			e.printStackTrace();
		}
	
	}
	
	public void getExistingMapData() {
		mServiceResponseView.setTextColor(Color.BLACK);
		IGeodataServiceResponse response;
		try {
			response = mGeodataServiceInterface.getMapData(new GeoRegion().MakeUsingBoundingBox(0.f, 0.f, 0.f, 0.f), mMapCompositionID);
			if(response.mResponseCode == IGeodataServiceResponse.ResponseCodes.LOADDATAREQUEST_DATA_ATTACHED) {
				ArrayList<WorldObject> resultList = (ArrayList<WorldObject>)(response.mData);
				mServiceResponseView.setText("response to getMapData is "+ response.mResponseCode + ", " + resultList.size() + " objects retrieved.");
			} 
			else {
				mServiceResponseView.setText("response to getMapData is "+ response.mResponseCode);
			}
		} 
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			mServiceResponseView.setText("error with getMapData ");
			e.printStackTrace();
		}
	} 
	public void getNonExistingMapData() {
		mServiceResponseView.setTextColor(Color.BLACK);
		IGeodataServiceResponse response;
		try {
			response = mGeodataServiceInterface.getMapData(new GeoRegion().MakeUsingBoundingBox(0.f, 0.f, 0.f, 0.f), mMapCompositionID);
			mServiceResponseView.setText("response to getMapBackground is "+ response.mResponseCode);
			
			// if data, for map app, take list of WorldObjectParcels and call WorldObject.typeOfObjectInWOParcel() on each, 
			// 		based on type, call .setDataPosition(0) to reset parcel read pointer
			//      then reconstitute object from parcel using new Area(parcel) or the like, 
			//		then build drawing object (of similar type), 
			//		last, discard reconstituted obj
		} 
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			mServiceResponseView.setText("error with getMapBackground ");
			e.printStackTrace();
		}
	}
	public void getBuddies(){
		mServiceResponseView.setTextColor(Color.BLACK);
		IGeodataServiceResponse response;
		try {
			response = mGeodataServiceInterface.getServiceState();
			mServiceResponseView.setText("response to getBuddies is "+ response.mResponseCode);
		} 
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			mServiceResponseView.setText("error with getBuddies ");
			e.printStackTrace();
		}
	}
	public void getClosestItem() {
		mServiceResponseView.setTextColor(Color.BLACK);
		mServiceResponseView.setText("response to getClosestItem");
	}
	public void getRoute() {
		mServiceResponseView.setTextColor(Color.BLACK);
		mServiceResponseView.setText("response to getRoute");
	}
	public void fakeBuddy() {
		mServiceResponseView.setTextColor(Color.BLACK);
		mServiceResponseView.setText("response to fakeBuddy");
	}
	public void fakeUserLocation() {
		mServiceResponseView.setTextColor(Color.BLACK);
		mServiceResponseView.setText("response to fakeUserLocation");
	}
	public void fakeUserHeading() {
		mServiceResponseView.setTextColor(Color.BLACK);
		mServiceResponseView.setText("response to fakeUserHeading");
	}
	

//===========================================================
// geodata service connection and interface handling methods

	class GeodataServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName className, IBinder boundService)
		{
				// returns IBinder for service.  Use this to create desired (predefined) interface for service
			mGeodataServiceInterface = IGeodataService.Stub.asInterface((IBinder)boundService);
			if(mGeodataServiceInterface == null) {
				mServiceStateView.setText("Geodata Service connection error: cannot get IGeodataService object");
				mServiceStateView.setTextColor(ERROR_COLOR);
				Log.d(TAG, "   GeodataService Error: cannot get IGeodataService object");
			}
			else {
				mServiceStateView.setText("Connected to Geodata Service");
				Log.d(TAG, "   GeodataService interface connected");
				mServiceStateView.setTextColor(RUNNING_COLOR);
			}
		}
 
		public void onServiceDisconnected(ComponentName className)
		{	
			mServiceStateView.setText("Geodata Service offline...");
			Log.d(TAG, "GeoataService interface disconnected");
			mServiceStateView.setTextColor(NOT_RUNNING_COLOR);
		}
	} 
	
	private void BindGeodataService() 
	{
//		Activity activity = getActivity();
//		assert activity != null : TAG + ".BindMapDataService called before activity created.";

		if (mGeodataServiceConnection == null) {
			mGeodataServiceConnection = new GeodataServiceConnection();
			bindService(new Intent(GEODATASERVICE_START), mGeodataServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "GeodataService connection bind request. Waiting for response... ");
		}
		// sets mGeodataServiceInterface on callback in GeodataServiceConnection:onServiceConnected( )
	}
	
	private void UnbindGeodataService()
	{
//		Activity activity = getActivity();
//		assert activity != null : TAG + ".UnbindGeodataService called before activity created.";

		
		if (mGeodataServiceConnection != null) {
			if(mGeodataServiceInterface != null) {
				try {
					// do any cleanup before unbinding
				} catch (Exception e) {
					e.printStackTrace();
				}	
			}
			
			unbindService(mGeodataServiceConnection);
			mGeodataServiceConnection = null;
			Log.d(TAG, "GeodataService connection unbound");
		}
	}	
	
	
	public Bundle GetBuddiesBundle() {
		return null;
	}
	
//	private void ChangeGPSState(Activity contextActivity) {
//		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(contextActivity);
//
//		// set title
//		alertDialogBuilder.setTitle("Set GPS state");
//
//		// set dialog message
//		alertDialogBuilder 
//		.setMessage("Enable or disable GPS in Mock Location server.")
//		.setCancelable(false)
//		.setNegativeButton("Enable",new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog,int id) {
//				mGPSEnabled = true;
//				dialog.cancel();
//				SendMsgToService();
//			}
//		})
//
//		.setPositiveButton("Disable",new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog,int id) {
//				// if this button is clicked, just close
//				// the dialog box and do nothing
//				mGPSEnabled = false;
//				dialog.cancel();
//				SendMsgToService();
//			}
//		});
//
//		// create alert dialog
//		AlertDialog alertDialog = alertDialogBuilder.create();
//
//		// show it
//		if(alertDialog != null) {
//			alertDialog.show();
//		}
//	}


	
//=================================
// UI response handlers
	
    @Override
    public void onBackPressed() {
    	finish();
    }

//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event)
//	{
//		int pos;
////	    Log.d(TAG,"onKeyDown");
//		switch(keyCode) {
//		case KeyEvent.KEYCODE_DPAD_LEFT:
////			goLeft();
//			return true;
//		case KeyEvent.KEYCODE_DPAD_RIGHT:
//			return true;
//		case KeyEvent.KEYCODE_DPAD_UP:
//			pos = mListview.getSelectedItemPosition();
//			if(pos != 0) {
//				mListview.setSelection(pos-1);
//			}
//			return true;
//		case KeyEvent.KEYCODE_DPAD_DOWN:
//			pos = mListview.getSelectedItemPosition();
//			if(pos != mResortNames.size()-1) {
//				mListview.setSelection(pos+1);
//			}
//			return true;
//		case KeyEvent.KEYCODE_BACK:
//			finish();
//			return true;
//		
//		default:
//			return false;
//		}
//
//	}

}
