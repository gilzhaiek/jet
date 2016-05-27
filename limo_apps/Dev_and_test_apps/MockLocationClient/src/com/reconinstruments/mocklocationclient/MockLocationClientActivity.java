package com.reconinstruments.mocklocationclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MockLocationClientActivity extends Activity {
	private final static String TAG = "MLClientActivity";
	private final static boolean MAPS_APP_TESTING = true;
	private final static String GPS_STRING = "GPS - enable/disable";

	ArrayList<String>	mResortNames = new ArrayList<String>();
	ArrayList<RectF>    mResortBBs = new ArrayList<RectF>();
	ArrayList<PointF>	mAtResortLocation = new ArrayList<PointF>();
	ArrayList<PointF>	mNearResortLocation = new ArrayList<PointF>();
	ArrayList<PointF>	mClosestResortLocation = new ArrayList<PointF>();
	ListView 			mListview;
	String				prevResort = "";
	ResortQualifier		prevQualifier = ResortQualifier.NONE; 
	ResortQualifier		mSpecificLocation = ResortQualifier.IN_RESORT; 
	Activity			mThisActivity = this;
	boolean				mGPSEnabled = true;
	TextView			mGPSStateView = null;
	PointF 				mResortCenter = new PointF(0.f,0.f);
	String 				mSelectedResort = "";
	int					mCurrentListViewIndex = 0;
	AlertDialog			mLocationAlertDialog;
	AlertDialog			mChangeGPSStateDialog;
	
	protected enum ResortQualifier {
		NONE,
		IN_RESORT,
		NEAR_RESORT,
		RESORT_IS_CLOSEST
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mock_location_client);

		mGPSStateView = (TextView)findViewById(R.id.gps_state);
		mGPSStateView.setText("GPS enabled");
		Intent intent = new Intent();
		intent.setAction("com.reconinstruments.mocklocationclient.change_location");
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra("GPSstate", true);
		intent.putExtra("Latitude", 0.);
		intent.putExtra("Longitude", 0.);
		sendBroadcast(intent); 		// force GPS on in case it was disabled...

		if(MAPS_APP_TESTING) {		// add ability to turn enable and disable GPS
			mResortNames.add(GPS_STRING);
			mResortBBs.add(new RectF(0.f,0.f,0.f,0.f));
			mAtResortLocation.add(new PointF(0.f,0.f));
			mNearResortLocation.add(new PointF(0.f,0.f));
			mClosestResortLocation.add(new PointF(0.f,0.f));
		}
		// read resort input file
		InputStream is;
		BufferedReader br;
		try {
			File theFile = new File("/sdcard/ReconApps/mock_locations.txt");
			if (theFile.exists()) {
				br = new BufferedReader(new FileReader(theFile.getAbsolutePath()));
			}
			else {
				is = getResources().openRawResource(R.raw.resortdump);
				br = new BufferedReader(new InputStreamReader(is));
			}
			String str;
			while ((str = br.readLine()) != null) {
				//				Log.i(TAG, ""+ str);
				String[] ar=str.split(",");
				if(ar[0].charAt(0) == '#'){
					continue;
				}
				double eastWest = (float)Double.parseDouble(ar[1]);
				double northSouth =  (float)Double.parseDouble(ar[3]);
				mResortNames.add((northSouth >= 0. ? "N" : "S") + (eastWest >= 0. ? "E" : "W") + " - " + ar[0] );
				//				Log.i(TAG, ""+ ar[0] + ", "+ ar[1] + ", "+ ar[2] + ", "+ ar[3] + ", "+ ar[4]);
				mResortBBs.add(new RectF((float)Double.parseDouble(ar[1]), (float)Double.parseDouble(ar[3]), (float)Double.parseDouble(ar[2]), (float)Double.parseDouble(ar[4])));
				mAtResortLocation.add(new PointF((float)(Double.parseDouble(ar[2])/2.0 + Double.parseDouble(ar[1])/2.0), (float)(Double.parseDouble(ar[3])/2.0 + Double.parseDouble(ar[4])/2.0)));
				mNearResortLocation.add(new PointF((float)(Double.parseDouble(ar[2])/2.0 + Double.parseDouble(ar[1])/2.0), (float)(Double.parseDouble(ar[3]) + 0.02)));
				mClosestResortLocation.add(new PointF((float)(Double.parseDouble(ar[2])/2.0 + Double.parseDouble(ar[1])/2.0), (float)(Double.parseDouble(ar[3]) + 0.2)));
			}
			br.close();
		} catch (IOException e) {
			System.out.println("File Read Error");
		}

		mListview = (ListView) findViewById(R.id.resortlist);
		final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, mResortNames);
		//	    final StableArrayAdapter adapter = new StableArrayAdapter(this,android.R.layout.simple_list_item_1, mResortNames);
		mListview.setAdapter(adapter);
		mListview.setSelection(0);
		mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

				mCurrentListViewIndex = position;
				mResortCenter = new PointF(0.f,0.f);
				mSelectedResort = mResortNames.get(position);
				if(mSelectedResort.equalsIgnoreCase(GPS_STRING)) {
					ChangeGPSState(mThisActivity);  // allows user to modify GPS state in service
				}
				else {
					mSpecificLocation = ResortQualifier.IN_RESORT;

					if(! prevResort.equalsIgnoreCase(mSelectedResort) || MAPS_APP_TESTING) {
						if(MAPS_APP_TESTING) {
							ChooseRelativeLocation(mThisActivity, mSelectedResort);  // allows user to modify mSpecificLocation
						}
						else {
							SendMsgToService();	// no user adjustments, so just send IN_RESORT change request
						}
						prevResort = mSelectedResort;
					}
					else {
						//					Log.i(TAG,"ignoring extra key press for "+ selectedResort);
					}

				}
			}

		});
	}

	private void SendMsgToService() {
		if(mGPSEnabled) mGPSStateView.setText("GPS enabled");
		else mGPSStateView.setText("GPS disabled");

		switch(mSpecificLocation) {
		case IN_RESORT:
			mResortCenter = mAtResortLocation.get(mCurrentListViewIndex);
			break;
		case NEAR_RESORT:
			mResortCenter = mNearResortLocation.get(mCurrentListViewIndex);
			break;
		case RESORT_IS_CLOSEST:
			mResortCenter = mClosestResortLocation.get(mCurrentListViewIndex);
			break;
		}

		Intent intent = new Intent();
		intent.setAction("com.reconinstruments.mocklocationclient.change_location");
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra("GPSstate", (boolean)mGPSEnabled);
		intent.putExtra("Latitude", (double)mResortCenter.y);
		intent.putExtra("Longitude", (double)mResortCenter.x);
		sendBroadcast(intent); 
		Log.i(TAG,"broadcasting change_location: "+ mSelectedResort +": " + mResortCenter.y+ ", "+ mResortCenter.x);
	}

	private void ChangeGPSState(Activity contextActivity) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(contextActivity);

		// set title
		alertDialogBuilder.setTitle("Set GPS state");

		// set dialog message
		alertDialogBuilder 
		.setMessage("Enable or disable GPS in Mock Location server.")
		.setCancelable(false)
		.setNegativeButton("Enable",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				mGPSEnabled = true;
				dialog.cancel();
				SendMsgToService();
			}
		})

		.setPositiveButton("Disable",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				// if this button is clicked, just close
				// the dialog box and do nothing
				mGPSEnabled = false;
				dialog.cancel();
				SendMsgToService();
			}
		})
		
		.setOnKeyListener(new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				switch(keyCode){
				case KeyEvent.KEYCODE_BACK:
					mChangeGPSStateDialog.dismiss();
					break;
				}
				return false;
			}
		});

		// create alert dialog
		mChangeGPSStateDialog = alertDialogBuilder.create();

		// show it
		if(mChangeGPSStateDialog != null) {
			mChangeGPSStateDialog.show();
		}
	}
	private void ChooseRelativeLocation(Activity contextActivity, String selectedResort) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(contextActivity);

		// set title
		alertDialogBuilder.setTitle(selectedResort);

		// set dialog message
		alertDialogBuilder 
		.setMessage("Set GPS to be In this resort, Near this resort or such that this resort is the Closest resort.")
		.setCancelable(false)
		.setNegativeButton("In",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				mSpecificLocation = ResortQualifier.IN_RESORT;
				dialog.cancel();
				SendMsgToService();
			}
		})

		.setNeutralButton("Near",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				// if this button is clicked, just close
				// the dialog box and do nothing
				mSpecificLocation = ResortQualifier.NEAR_RESORT;
				dialog.cancel();
				SendMsgToService();
			}
		})

		.setPositiveButton("Closest",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				// if this button is clicked, just close
				// the dialog box and do nothing
				mSpecificLocation = ResortQualifier.RESORT_IS_CLOSEST;
				dialog.cancel();
				SendMsgToService();
			}
		})
		
		.setOnKeyListener(new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				switch(keyCode){
				case KeyEvent.KEYCODE_BACK:
					mLocationAlertDialog.dismiss();
					break;
				}
				return false;
			}
		});

		// create alert dialog
		mLocationAlertDialog = alertDialogBuilder.create();

		// show it
		if(mLocationAlertDialog != null) {
			mLocationAlertDialog.show();
		}
	}
	
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
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mock_location_client, menu);
		return true;
   }
	
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(TAG, "onResume");
    }

	@Override
	public void onPause() {
		super.onPause();
	}

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
