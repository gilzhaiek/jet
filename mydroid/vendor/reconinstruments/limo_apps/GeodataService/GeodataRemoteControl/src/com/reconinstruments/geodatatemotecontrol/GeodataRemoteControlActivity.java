package com.reconinstruments.geodatatemotecontrol;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.geodataremotecontrol.R;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.geodataservice.devinterface.IDevTesting;

public class GeodataRemoteControlActivity extends Activity {
	private final static String TAG = "GeoDataRemote";
	public final static String GEODATASERVICE_START_WITH_IDEV = "com.reconinstruments.geodataservice.start_with_idev";
	public final static String GEODATASERVICE_BROADCAST_RESTART_REQUEST = "com.reconinstruments.geodataservice.restart";
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
	private static IDevTesting 				mGeodataDevInterface  = null;
	private int								mCurrentListViewIndex = 0;
	private DevTestingState					mDevTestingState = new DevTestingState();
	private DevTestingState					mNewDevTestingState = null;
	private boolean							mHaveServerState = false;
	private ArrayList<String> 				mDisplayStrings = new ArrayList<String>();
	private ArrayList<String>				mConditionStrings = new ArrayList<String>();
	
	// UI objects
	private ListView 	mListview;
	private TextView	mServiceStateView = null;
	private TextView	mServiceResponseView = null;
	
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

	public class MyArrayAdapter extends ArrayAdapter<String> {
		  private final Context context;
		  private final ArrayList<String> strings;
		  private final ArrayList<Integer> values;

		  public MyArrayAdapter(Context context, ArrayList<String> strings, ArrayList<Integer> values) {
		    super(context, R.layout.listrow, strings);
		    this.context = context;
		    this.strings = strings;
		    this.values = values;
		  }

		  @Override
		  public View getView(int position, View convertView, ViewGroup parent) {
		    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    View rowView = inflater.inflate(R.layout.listrow, parent, false);
		    TextView textView = (TextView) rowView.findViewById(R.id.rowstring);
		    ImageView imageView = (ImageView) rowView.findViewById(R.id.rowicon);
		    textView.setText(strings.get(position));
		    // Change the icon for Windows and iPhone
		    Integer v = values.get(position);
		    if (v == 0) {
		    	imageView.setImageResource(R.drawable.x_red);
		    } else {
		    	imageView.setImageResource(R.drawable.checkmark_green);
		    }

		    return rowView;
		  }
		} 

//======================================================
// activity lifecycle methods
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.geodataremote);

		mServiceStateView = (TextView)findViewById(R.id.service_state);
  		mServiceStateView.setText("Geodata Service offline...");
		mServiceStateView.setTextColor(NOT_RUNNING_COLOR);

		mServiceResponseView = (TextView)findViewById(R.id.service_response);
		mServiceResponseView.setTextColor(Color.GRAY);
		mServiceResponseView.setText("Waiting for geodata service connection...");
 
		for(DevTestingState.TestingConditions condition : DevTestingState.TestingConditions.values()) {
			mConditionStrings.add(condition.toString());
			mDisplayStrings.add(condition.toString());
		}

		
		mListview = (ListView) findViewById(R.id.testconditions);
		final MyArrayAdapter adapter = new MyArrayAdapter(this, mDisplayStrings, mDevTestingState.mTestingConditionState);
		//	    final StableArrayAdapter adapter = new StableArrayAdapter(this,android.R.layout.simple_list_item_1, mResortNames);
		mListview.setAdapter(adapter);
		mListview.setSelection(0);
		mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		 	@Override 
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
 
		 		if(mHaveServerState) {
		 			mCurrentListViewIndex = position;
		 			String selectedItem = mDisplayStrings.get(mCurrentListViewIndex);

		 			//				Log.i(TAG, "old value: "+ mConditionStrings.get(mCurrentListViewIndex));
		 			if(mDevTestingState.mTestingConditionState.get(mCurrentListViewIndex) == 0) {
		 				//					mDisplayStrings.set(mCurrentListViewIndex, "on - "+mConditionStrings.get(mCurrentListViewIndex));
		 				mDevTestingState.mTestingConditionState.set(mCurrentListViewIndex, 1);
		 			}
		 			else {
		 				//	 				mDisplayStrings.set(mCurrentListViewIndex, "off - "+mConditionStrings.get(mCurrentListViewIndex));
		 				mDevTestingState.mTestingConditionState.set(mCurrentListViewIndex, 0);
		 			}
		 			((BaseAdapter) mListview.getAdapter()).notifyDataSetChanged();


		 			try {		// send revised data to service
		 				mGeodataDevInterface.setDevTestingState(mDevTestingState);
		 			}  
		 			catch (RemoteException e) {
		 				// TODO Auto-generated catch block
		 				e.printStackTrace();
		 			}
		 		}
		 		else {
		 			mServiceResponseView.setText("Cannot use remote until server connected...");
		 		}
		 	}

		});

        Button restartButton =(Button)findViewById(R.id.reconnectbutton);

        restartButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

            	Log.i(TAG,"   unbinding current interface...");
            	UnbindGeodataServiceDevInterface();
            	
            	Log.i(TAG,"Remote request to restart service...");
        		Intent intent = new Intent();
        		intent.setAction(GEODATASERVICE_BROADCAST_RESTART_REQUEST);
        		intent.addCategory(Intent.CATEGORY_DEFAULT);
        		sendBroadcast(intent); 		// request server restart
        		
        		try {
					Thread.sleep(3000);
				} 
        		catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	Log.i(TAG,"   rebinding to interface...");
        		BindGeodataServiceDevInterface();  // call only after activity has been created
            	Log.i(TAG,"   rebinding request done");

            }
        });

	}
	  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.geodataremote, menu);
		return true;
   }
	
	public void onResume() {
		super.onResume();

		BindGeodataServiceDevInterface();  // call only after activity has been created
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}


	public void onDestroy(){
		UnbindGeodataServiceDevInterface();
		
		super.onDestroy();
		Log.d(TAG,"onDestroy");
			
	}
	 
//======================================================
// support methods

	private void SendMsgToService(int serviceIndex) {

		
		switch(mCurrentListViewIndex) {
		}

	}
	
	
	

//===========================================================
// geodata service connection and interface handling methods

    class GeodataServiceConnection implements ServiceConnection
	{ 
		public void onServiceConnected(ComponentName className, IBinder boundService)
		{
//			Log.i(TAG, "   GeodataService Dev Connection...");
				// returns IBinder for service.  Use this to create desired (predefined) interface for service
			mGeodataDevInterface = IDevTesting.Stub.asInterface((IBinder)boundService);
			if(mGeodataDevInterface == null) {
				mServiceStateView.setText("Geodata Service connection error: cannot get IDevInterface object");
				mServiceStateView.setTextColor(ERROR_COLOR);
				Log.d(TAG, "   GeodataService Error: cannot get IDevInterface object");
			}
			else {
				mServiceStateView.setText("Connected to Geodata Service Dev Interface");
				mServiceStateView.setTextColor(RUNNING_COLOR);
				Log.d(TAG, "   GeodataService dev interface connected");
				
				try {  
					mNewDevTestingState = mGeodataDevInterface.getDevTestingState();
					if(mNewDevTestingState != null) {
						Log.d(TAG, "      new data");
						for(DevTestingState.TestingConditions condition : DevTestingState.TestingConditions.values()) {
							mDevTestingState.mTestingConditionState.set(condition.ordinal(), mNewDevTestingState.mTestingConditionState.get(condition.ordinal())) ;
						}
						((BaseAdapter) mListview.getAdapter()).notifyDataSetChanged();
						
						mHaveServerState = true;
						mServiceResponseView.setText("Select an action below...");
					}
					else {
						Log.d(TAG, "      data is null");
					}
				} 
				catch (RemoteException e) {
					Log.e(TAG, "   Remote exception error");
					// TODO Handle this error condition
					e.printStackTrace();
				}
			}
		}
    
		public void onServiceDisconnected(ComponentName className)
		{	
			mServiceStateView.setText("Geodata Service offline...");
			mServiceStateView.setTextColor(NOT_RUNNING_COLOR);

			mServiceResponseView.setText("...");
			Log.d(TAG, "GeoataService dev interface disconnected");
			mServiceStateView.setTextColor(NOT_RUNNING_COLOR);
			mGeodataDevInterface = null;
			mHaveServerState = false;
		}
	}   
 	
	private void BindGeodataServiceDevInterface() 
	{
//		Activity activity = getActivity();
//		assert activity != null : TAG + ".BindMapDataService called before activity created.";

		if (mGeodataServiceConnection == null) {
			mGeodataServiceConnection = new GeodataServiceConnection();
			bindService(new Intent(GEODATASERVICE_START_WITH_IDEV), mGeodataServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "IGeodataServiceDeveloper connection bind request. Waiting for response... ");
		}
		// sets mGeodataServiceInterface on callback in GeodataServiceConnection:onServiceConnected( )
	}
	
	private void UnbindGeodataServiceDevInterface()
	{
//		Activity activity = getActivity();
//		assert activity != null : TAG + ".UnbindGeodataService called before activity created.";

	 	
		if (mGeodataServiceConnection != null) {
			if(mGeodataDevInterface != null) {
				try {
					// do any cleanup before unbinding
				} 
				catch (Exception e) {
					e.printStackTrace();
				}	
			}
			
			unbindService(mGeodataServiceConnection);
			mGeodataServiceConnection = null;
			Log.d(TAG, "GeodataService connection unbound");
			mServiceStateView.setText("Geodata Service offline...");
			mServiceStateView.setTextColor(NOT_RUNNING_COLOR);

		}
	}	
	
	
	public Bundle GetBuddiesBundle() {
		return null;
	}
	

	
//=================================
// UI response handlers
	
    @Override
    public void onBackPressed() {
    	finish();
    }


}
