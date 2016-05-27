package com.reconinstruments.dashlauncher.radar;


import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.BoardActivity;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.radar.maps.IMapsService;
import com.reconinstruments.dashlauncher.radar.maps.bundlers.MapBundler;
import com.reconinstruments.dashlauncher.radar.maps.drawings.MapDrawings;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.MapImagesInfo;
import com.reconinstruments.dashlauncher.settings.SettingAdapter;
import com.reconinstruments.dashlauncher.settings.SettingItem;
import com.reconinstruments.heading.HeadingEvent;
import com.reconinstruments.heading.HeadingListener;
import com.reconinstruments.heading.HeadingManager;

public class ReconRadarActivity extends BoardActivity implements HeadingListener
{
	private static final String TAG = "ReconRadarActivity";
	private static ControllersManager		mControllersManager = null;
	private static RadarGLController		mRadarGLController	= null;
	private static HeadingManager			mHeadingManager		= null;
	private static boolean					mIsInExtendedView	= false;
	private static MapDrawings 				mMapDrawings		= null;
	private static MapImagesInfo			mMapImagesInfo 		= null; 
	
	private static IMapsService				mMapsService		= null;
	private static MapsServiceConnection	mMapsServiceConnection;
	private static OnResumeTask				mOnResumeTask		= null;
	private static OnHeadingTask			mOnHeadingTask		= null;
	
	private static boolean					mRadarActivityStarted = false;
	private static boolean					mRadarActivityResumed = false;
	
	private static LocationTransformer 		mLocationTransformer  = null;
	
	// Cypress
	//private float							mFakeLongitude		= -123.213816f;
	//private float							mFakeLatitude		= 49.391675f;
	
	// Grouse
	//private float 							mFakeLongitude		= -123.078203f;
	//private float 							mFakeLatitude		= 49.371476f;

	// No Fake Location
	private float 							mFakeLongitude		= 0.0f;
	private float 							mFakeLatitude		= 0.0f;
	
	private View overlayView = null;
	private FrameLayout mainLayout = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG,"onCreate "+System.currentTimeMillis());
		
		super.onCreate(savedInstanceState);
		
		mRadarGLController = new RadarGLController(this);
		
		mainLayout = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
		
		mLocationTransformer = new LocationTransformer();
		mControllersManager = new ControllersManager(this, mLocationTransformer);
		mRadarGLController.SetControllerHelper(mControllersManager.GetDefaultController());
	}
	 
	@Override
	public void onResume()
	{
		Log.d(TAG,"onResume "+System.currentTimeMillis());
		super.onResume();
		
		mRadarActivityResumed = false;
		
		if(mOnResumeTask == null) {
			mOnResumeTask = new OnResumeTask();
			mOnResumeTask.execute(this);
		}
		
		// Launch calibration if it hasn't been done before
		removeCalibrationOverlay();
		if(Settings.Secure.getInt(getContentResolver(), "hasWrittenMagOffsets", 0)!=1) { // If mag offsets have not been written
			Log.v(TAG, "addCalibration");
			addCalibrationOverlay();
		}
	} 
	
	protected class OnResumeTask extends AsyncTask<ReconRadarActivity, Void, Void> {
		@Override
		protected Void doInBackground(ReconRadarActivity... params) {
			try {
				mIsInExtendedView = false;
				
				if(!mRadarActivityStarted) {
					BindMapsService();
									 
					mHeadingManager = new HeadingManager(getApplicationContext(),params[0]);
					
					mRadarActivityStarted = true;
				}
				
				if(isCancelled()) return null;
				mRadarGLController.SetControllerHelper(mControllersManager.GetDefaultController());
				
				if(isCancelled()) return null;
				mHeadingManager.initService();
				
				if(isCancelled()) return null;
				mRadarGLController.onResume();
			}
			catch (Exception e)  {
				if(mRadarActivityStarted) {
					mRadarActivityStarted	= false;
					mRadarActivityResumed	= false;
					
					mHeadingManager.releaseService();
					mRadarGLController.onPause();
					
					UnbindMapsService();
					
					mHeadingManager			= null;
					mLocationTransformer	= null;
					mControllersManager		= null;
										
				}
			} finally {
				mOnResumeTask = null;
			}
			
			return null;
		}
		
		@Override
		protected void onCancelled(){
			super.onCancelled();
			mOnResumeTask = null;
			mRadarActivityResumed = false;
		}

		@Override
		protected void onPostExecute(Void nothing) {
			mOnResumeTask = null;
			mRadarActivityResumed = true;
			Log.d(TAG,"onResume Done"+System.currentTimeMillis());
		}		
    }		
	
	@Override
	protected void onPause()  
	{
		Log.d(TAG,"onPause");
		super.onPause();
		
		mRadarActivityResumed = false;
		
		if(mOnResumeTask != null) {
			mOnResumeTask.cancel(true);
		}
		
		if(mRadarActivityStarted) { 
			mHeadingManager.releaseService();
			mRadarGLController.onPause();
		}
		
		// The following call pauses the rendering thread. If your OpenGL application is memory intensive,
		// you should consider de-allocating objects that consume significant memory here.
		// mGLView.onPause();
	}

	@Override
	protected void onDestroy(){
		Log.d(TAG,"onDestroy");
		super.onDestroy();
		
		UnbindMapsService();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(!mRadarActivityResumed) {
			return super.onKeyDown(keyCode, event);
		}
	
		if(mRadarGLController.onKeyDown(keyCode, event))
			return true;
	
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_DPAD_UP:
			if(!mIsInExtendedView) {
				mRadarGLController.SetControllerHelper(mControllersManager.GetExtendedView());
				mIsInExtendedView = true;
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if(mIsInExtendedView) {
				mRadarGLController.SetControllerHelper(mControllersManager.GetDefaultController());
				mIsInExtendedView = false;
				return true;
			}
			break;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(!mRadarActivityResumed) {
			return super.onKeyUp(keyCode, event);
		}
		
		if(mRadarGLController.onKeyUp(keyCode, event))  
			return true;		
		
		return super.onKeyUp(keyCode, event);
	}
	
	public void onLocationChanged(Location location)
	{
		if(!mRadarActivityResumed) {
			return;
		}
		
		if(mMapDrawings == null) 
			UpdateMapBundle(location);
		
		try
		{
			if(mMapsService != null) {
				Bundle buddyInfoBundle = mMapsService.getListOfBuddies();
				if(buddyInfoBundle != null)
				{
					mRadarGLController.GetContollerHelper().UpdateBuddies(buddyInfoBundle, location);
				}
			}
		} 
		catch (RemoteException e){ e.printStackTrace(); }
		catch (NullPointerException e){ e.printStackTrace(); }
	}
	
	private void BindMapsService() 
	{
		if (mMapsServiceConnection == null)
		{
			mMapsServiceConnection = new MapsServiceConnection();
			Intent i = new Intent("RECON_MAP_SERVICE");
			bindService(i, mMapsServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "BindMapsService:bindService");
		}
	}
 
	private void UnbindMapsService()
	{
		if (mMapsServiceConnection != null)
		{
			unbindService(mMapsServiceConnection);
			mMapsServiceConnection = null;
			Log.d(TAG, "UnbindMapsService:unbindService");
		}
	}	
	
	protected void UpdateMapBundle(Location location){
		try
		{
			Bundle mapBundle = null;
			if (mMapsService != null)
			{
				if(mMapDrawings != null) 
				{
					Bundle resortIDBundle = mMapsService.getResortID();
					if(resortIDBundle.getInt("ResortID") == mMapDrawings.mResortInfo.ResortID) {
						Log.v(TAG, "Already have this ResortId="+ mMapDrawings.mResortInfo.ResortID);
						return;
					}
				}
				mapBundle = mMapsService.getResortGraphicObjects();

				if(mapBundle != null)
				{
					Log.v(TAG, "MapsServiceConnection:mapBundle has data");
					mControllersManager.SetResortExists(true);
					mMapImagesInfo = MapBundler.GetMapImagesInfo(mapBundle);
					mMapDrawings = MapBundler.GetMapDrawings(mapBundle);
					
					Log.i(TAG, "POIs: " + mMapDrawings.POIDrawings.size()); 
					
					RadarViewControllerHelper radarViewControllerHelper = (RadarViewControllerHelper)mControllersManager.GetController(ControllersManager.RADAR_CONTROLLER);
					
					// Center Resort
					/*if(mMapDrawings.mResortInfo.BoundingBox.left < mMapDrawings.mResortInfo.BoundingBox.right) {
						mFakeLongitude = mMapDrawings.mResortInfo.BoundingBox.left + (mMapDrawings.mResortInfo.BoundingBox.width() / 3.0f);
					} else {
						mFakeLongitude = mMapDrawings.mResortInfo.BoundingBox.right + (mMapDrawings.mResortInfo.BoundingBox.width() / 3.0f);
					}
					if(mMapDrawings.mResortInfo.BoundingBox.top < mMapDrawings.mResortInfo.BoundingBox.bottom) {
						mFakeLatitude = mMapDrawings.mResortInfo.BoundingBox.top + (mMapDrawings.mResortInfo.BoundingBox.height() / 3.0f);
					} else {
						mFakeLatitude = mMapDrawings.mResortInfo.BoundingBox.bottom + (mMapDrawings.mResortInfo.BoundingBox.height() / 3.0f);
					}*/
					
					mLocationTransformer.SetBoundingBox(mMapDrawings.mResortInfo.BoundingBox);
					radarViewControllerHelper.LocationTransformerUpdated(mLocationTransformer, false);
					radarViewControllerHelper.SetMapDrawings(mMapImagesInfo, mMapDrawings);
					
					mapBundle.clear();  
				}
			}
			
			if(mapBundle == null)
			{
				if(location != null) {
					RadarViewControllerHelper radarViewControllerHelper = (RadarViewControllerHelper)mControllersManager.GetController(ControllersManager.RADAR_CONTROLLER);
					
					mLocationTransformer.SetGPSPosition(location.getLongitude(), location.getLatitude());
					radarViewControllerHelper.LocationTransformerUpdated(mLocationTransformer, true);
				}
			}
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}		 
	}
	
	class MapsServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName className, IBinder boundService)
		{
			Log.d(TAG, "onServiceConnected");
			
			mMapsService = IMapsService.Stub.asInterface((IBinder)boundService);			
		}

		public void onServiceDisconnected(ComponentName className)
		{	
			Log.d(TAG, "onServiceDisconnected");
			mMapsService = null;
		}
	} 

	protected class OnHeadingTask extends AsyncTask<HeadingEvent, Void, Void> {
		@Override
		protected Void doInBackground(HeadingEvent... params) {
			if(!mRadarActivityResumed) {
				return null;
			}
			
			try {
				HeadingEvent headingEvent = params[0];
				if(mFakeLongitude != 0.0f && mFakeLatitude != 0.0f) {
					mFakeLongitude += 0.00001f;
					mFakeLatitude += 0.00001f;
					headingEvent.mLocation.setLongitude(mFakeLongitude);
					headingEvent.mLocation.setLatitude(mFakeLatitude);
				}
							
				if(mRadarGLController != null) { 
					BaseControllerHelper baseControllerHelper = mRadarGLController.GetContollerHelper();
					if(baseControllerHelper != null) {
						baseControllerHelper.OnLocationHeadingChanged(headingEvent.mLocation, headingEvent.mYaw, headingEvent.mPitch, headingEvent.mIsGPSHeading);
					}
				}
				onLocationChanged(headingEvent.mLocation);
			} catch (Exception e) {
				
			}
			
			return null;
		}
		
		@Override
		protected void onCancelled(){
			super.onCancelled();
			mOnHeadingTask = null;
		}

		@Override
		protected void onPostExecute(Void nothing) {
			mOnHeadingTask = null;
		}		
    }		
	
	public void onHeadingChanged(HeadingEvent headingEvent) {
		if(!mRadarActivityResumed) {
			return;
		}
		
		if(mOnHeadingTask == null) {
			mOnHeadingTask = new OnHeadingTask();
			mOnHeadingTask.execute(headingEvent);
		}
	};
	
	private void addCalibrationOverlay() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		overlayView = inflater.inflate(R.layout.setting_layout, null);
		
		TextView title = (TextView) overlayView.findViewById(R.id.setting_title);
		title.setText("Radar");
		title.setPadding(0, 15, 0, 0);
		
		LinearLayout desc_layout = (LinearLayout) overlayView.findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) overlayView.findViewById(R.id.setting_desc_text);
		desc.setPadding(0, 0, 0, 15);
		desc.setText("Find buddies, chairlifts, restaurants," + "\n" + "and other points of interest near you.");
		
		ArrayList<SettingItem> listItems = new ArrayList<SettingItem>();

		listItems.add(new SettingItem(new Intent("com.reconinstruments.compass.CALIBRATE"), "Calibrate Compass" ));
		
		ListView lv = (ListView) overlayView.findViewById(android.R.id.list);
		lv.setAdapter(new SettingAdapter(this, 0, listItems));
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				startActivity(new Intent("com.reconinstruments.compass.CALIBRATE"));
			}
			
		});
		
		mainLayout.addView(overlayView, new LinearLayout.LayoutParams(mainLayout.getLayoutParams().width, mainLayout.getLayoutParams().height));
		
		lv.requestFocus();
	}
	
	private void removeCalibrationOverlay() {
		if(overlayView != null){
			mainLayout.removeView(overlayView);
			overlayView = null;
		}
	}
}


