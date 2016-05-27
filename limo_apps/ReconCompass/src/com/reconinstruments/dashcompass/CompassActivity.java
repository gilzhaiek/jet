package com.reconinstruments.dashcompass;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.reconinstruments.connect.messages.BuddyInfoMessage;
import com.reconinstruments.connect.messages.BuddyInfoMessage.BuddyInfo;
import com.reconinstruments.connect.messages.XMLMessage;
import com.reconinstruments.dashelement1.ColumnElementActivity;
import com.reconinstruments.heading.HeadingEvent;
import com.reconinstruments.heading.HeadingListener;
import com.reconinstruments.heading.HeadingManager;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.DeviceUtils;


public class CompassActivity extends ColumnElementActivity implements HeadingListener {	// for HUD build
    private static String TAG = "CompassActivity";
    private static double PIXELS_PER_45_DEGREES = 190.0;
    //	private static double BUDDY_DECTECTION_DISTANCE = 5000.0;
    private static double BUDDY_DETECTION_DISTANCE = 500000.0;
    private static boolean USE_FAKE_BUDDIES = false;
    private static long REMOVE_BUDDY_TIME_MS = 7200000; // 2hr	
    private static long FADE_OFFLINE_BUDDY_TIME_MS = 300000; //change offline threshold from 1min to 5mins. old value = 60000;
    private static int FAST_LOCATION_POST_UPDATE_INTERVAL = 10;
	private static final double 	ANGLE_TO_EDGE_OF_SCREEN_IN_RADS = Math.atan2(214,PIXELS_PER_45_DEGREES);
	private static final double 	ANGLE_TO_EDGE_OF_SCREEN_IN_DEGREES = Math.toDegrees(ANGLE_TO_EDGE_OF_SCREEN_IN_RADS);
	
    private View overlayView = null;
    private FrameLayout mainLayout = null;
    private boolean isCalibrationNeeded = true;
	private LinearLayout 		calibrateScreen;

    ArrayList<CompassBuddyInfo>		mCurrentBuddies = new ArrayList<CompassBuddyInfo>();
    private static HeadingManager	mHeadingManager	= null;
    float 							mUserHeading = 0.0f;
    RelativeLayout 					mScreenLayout = null;
    LinearLayout					mBuddyNameContainer = null;
    LinearLayout					mTapTextContainer = null;
    boolean							mProcessHeading = false;
    ImageView						mCompassBar = null;
    ImageView						mCompassUnderline = null;
    TextView						mBuddyNameTV = null;
    TextView						mDistanceFromMeTV = null;
    TextView						mLocationTimeTextView = null;
    TextView						mNumBuddies = null;
    Context							mContext = null;
    int								mBuddyCount = 0;
    Location 						mUserLocation = null;
	private TextView 				mTapButtonText = null;
	private TextView 				mEngageAppText = null;
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG,"onCreate "+System.currentTimeMillis());
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.activity_compass);
    	
    	mContext = getBaseContext();
    	mScreenLayout = (RelativeLayout) findViewById(R.id.compass_activity_container);
    	
    	mTapTextContainer = (LinearLayout) findViewById(R.id.tap_text_container);
    	mBuddyNameContainer = (LinearLayout) findViewById(R.id.buddy_name_container);

    	mCompassBar = (ImageView) findViewById(R.id.compass_bar); 
    	Matrix matrix = new Matrix();
    	matrix.reset();
    	matrix.postTranslate(-428, 0);
    	mCompassBar.setScaleType(ScaleType.MATRIX);
    	mCompassBar.setImageMatrix(matrix);

    	mCompassUnderline = (ImageView) findViewById(R.id.compass_underline); 
    	mBuddyNameTV = (TextView) findViewById(R.id.buddy_name);
    	mDistanceFromMeTV = (TextView) findViewById(R.id.distance_from_me); 
    	mLocationTimeTextView = (TextView) findViewById(R.id.offline_time); 
    	mNumBuddies = (TextView) findViewById(R.id.number_buddies); 
    	mTapButtonText = (TextView) findViewById(R.id.tap_text);
    	mEngageAppText = (TextView) findViewById(R.id.on_engage_app_text);

    	Log.i(TAG,"text: "+mBuddyNameTV+", "+mNumBuddies);

    	registerReceiver(buddyInfoReceiver, new IntentFilter(XMLMessage.BUDDY_INFO_MESSAGE));

    	if(USE_FAKE_BUDDIES) {
    		CompassBuddyInfo curBuddy = new CompassBuddyInfo(mContext, 0, "faker1", 49.270855, -123.123644, System.currentTimeMillis(), System.currentTimeMillis());
    		//if(nextBuddyName.equalsIgnoreCase("scott")) 
    		curBuddy.AddBuddyToView(mScreenLayout);
    		mCurrentBuddies.add(curBuddy);
    		curBuddy = new CompassBuddyInfo(mContext, 1, "longfaker2", 49.270855, -123.123644, System.currentTimeMillis(), System.currentTimeMillis());
    		//if(nextBuddyName.equalsIgnoreCase("scott")) 
    		curBuddy.AddBuddyToView(mScreenLayout);
    		mCurrentBuddies.add(curBuddy);
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Inflate the menu; this adds items to the action bar if it is present.
    	getMenuInflater().inflate(R.menu.compass, menu);
    	return true;
    }

    @Override
    public void onResume() {
    	Log.i(TAG,"onResume "+System.currentTimeMillis());
    	super.onResume();

        // Request for active GPS.
        Intent intent = new Intent("RECON_ACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);

    	mProcessHeading = true;

    	mainLayout = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);

    	if(mHeadingManager == null) 
    		mHeadingManager = new HeadingManager(getApplication(), this);
    	mHeadingManager.initService();
    	Log.i(TAG,"onResume2 "+System.currentTimeMillis());

    	runCalibrationTest();

    	// Notify that need faster updates
    	HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,FAST_LOCATION_POST_UPDATE_INTERVAL);

    	//get phone connection state
    	int state = BTHelper.getInstance(this.getApplicationContext()).getBTConnectionState();
    	
    	// if phone is not connected to HUD, display appropriate message
    	if(state == BTHelper.BT_STATE_DISCONNECTED && mUserLocation == null){
    		mNumBuddies.setText(R.string.connect_smartphone_msg);
    	}
    } 

    @Override
    public void onPause()  {
    	Log.d(TAG,"onPause");

    	if(mHeadingManager != null) {
    		mHeadingManager.releaseService();
    		mHeadingManager = null;
    	}

    	// Back to normal update rate
    	HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,0);

        Intent intent = new Intent("RECON_DEACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);

    	super.onPause();
    }

    @Override
    public void onDestroy(){
    	unregisterReceiver(buddyInfoReceiver);
    	Log.d(TAG,"onDestroy");
    	super.onDestroy();
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	Log.d(TAG,"onKeyUp");
    	switch(keyCode) {
	case KeyEvent.KEYCODE_ENTER:
    	case KeyEvent.KEYCODE_DPAD_CENTER:
    		if (isCalibrationNeeded) {
    			startActivity(new Intent("com.reconinstruments.compass.CALIBRATE"));
    			return true;
    		}
    	}
    	return super.onKeyUp(keyCode,event);
    }




    public void ResetBuddyState()  {
    	for(int i=mCurrentBuddies.size()-1; i>=0; i--)	{		// reverse through array so removals don't effect processing
    		CompassBuddyInfo curBuddy = mCurrentBuddies.get(i);
    		if((System.currentTimeMillis() - curBuddy.mLastUpdateTime) > REMOVE_BUDDY_TIME_MS) {	// remove offline buddy after time limit of no updates
    			curBuddy.RemoveBuddyFromView(mScreenLayout);
    			curBuddy = null;
    			mCurrentBuddies.remove(i);
    		}
    		else {
    			if((System.currentTimeMillis() - curBuddy.mLastUpdateTime) > FADE_OFFLINE_BUDDY_TIME_MS) curBuddy.mOnline = false;	// mark buddy as offline after time limit of no updates
    			else curBuddy.mOnline = true;	// mark buddy as online
    		}
    		//			curBuddy.mOnline = false;  // for simple testing
    	}
    }


    // HeadingListener callback
    public void onHeadingChanged(HeadingEvent headingEvent) {
    	//		Log.i(TAG,"onHeadingChanged: "+headingEvent.mYaw);
        if(mProcessHeading && !(Float.isNaN(headingEvent.mYaw))) {
    		float prevUserHeading = mUserHeading;
    		float newHeading = headingEvent.mYaw;
    		if(mUserHeading > 270.0f && newHeading < 90.0f) {
    			mUserHeading = mUserHeading - 360.0f;	// avoid aliasing in average when crossing North (angle = 0.0)
    			//				Log.i(TAG,"user headings 1: "+ prevUserHeading + ", " +mUserHeading + ", "+newHeading);
    		}
    		else if (mUserHeading < 90.0f && newHeading > 270.0f) {
    			newHeading = newHeading - 360.0f;	// avoid aliasing in average when crossing North (angle = 0.0)
    			//				Log.i(TAG,"user headings 2: "+ prevUserHeading + ", "+ mUserHeading + ", "+newHeading);
    		}

    		mUserHeading = (float) ((4.0*mUserHeading + newHeading)/5.0);		// smooth heading

    		if(mUserHeading < 0.0f) mUserHeading += 360.0f;
    		if(mUserHeading > 360.0f) mUserHeading -= 360.0f;

    		mUserLocation = headingEvent.mLocation;
    		//			Log.i(TAG,"user location: "+userLocation);

    		runOnUiThread(new Runnable() {
    			@Override
    			public void run() {
    				// update buddy states based on time since last update
    				ResetBuddyState();		
    				
					int connectionState = BTHelper.getInstance(getApplicationContext()).getBTConnectionState();

    				// move compass strip
    				int x;
    				int offset = (mUserHeading >= 315f && mUserHeading <= 360) ? -(int)PIXELS_PER_45_DEGREES*7 : (int)PIXELS_PER_45_DEGREES;
    				x = (int)(mUserHeading / 360.0 * (8.0*PIXELS_PER_45_DEGREES)) + offset;
    				mCompassBar.getImageMatrix().reset();
    				mCompassBar.getImageMatrix().postTranslate(-x, 0);
    				findViewById(R.id.compass_bar).invalidate();
    				//    				Log.i(TAG,"user headings 3: "+ x );


    				String dir = "";
    				if((mUserHeading >= 0 && mUserHeading < 22.5) || (mUserHeading >= 337.5 && mUserHeading < 360.0) ) dir = "N";
    				else if(mUserHeading >= 22.5 && mUserHeading < 67.5) dir = "NE";
    				else if(mUserHeading >= 67.5 && mUserHeading < 112.5) dir = "E";
    				else if(mUserHeading >= 112.5 && mUserHeading < 157.5) dir = "SE";
    				else if(mUserHeading >= 157.5 && mUserHeading < 202.5) dir = "S";
    				else if(mUserHeading >= 202.5 && mUserHeading < 247.5) dir = "SW";
    				else if(mUserHeading >= 247.5 && mUserHeading < 292.5) dir = "W";
    				else dir = "NW";

//    				mDistanceFromMeTV.setText("" + (int)mUserHeading + (char) 0x00B0 );
//    				mBuddyNameTV.setText("" + dir);

    				if(mUserLocation != null) {
    					double userLatitude = mUserLocation.getLatitude() ;
    					double userLongitude = mUserLocation.getLongitude();
    					// for buddies, display bitmap and info if on screen
    					mBuddyCount = 0;

    					if(mCurrentBuddies.size() > 0){
    						showBuddyTextViews();
    						
	    					// first calc buddy angle and distance relative to user and related icon location
	    					for(CompassBuddyInfo curBuddy : mCurrentBuddies)	{			
	    						double vertDist = VerticalDistanceInMeters(curBuddy.mLatitude, userLatitude);					//+ve up/north
	    						double horzDist = HorizontalDistanceInMeters(curBuddy.mLongitude, userLongitude, userLatitude); // +ve right/east
	
	    						curBuddy.mDistFromUser = Math.sqrt(horzDist*horzDist + vertDist*vertDist);
	    						double angleFromEastCCInRads = Math.atan2(vertDist, horzDist);		// angle from east counterclockwise
	    						double angleFromNorthInRads = Math.PI/2.0 - angleFromEastCCInRads;		// angle from north clockwise
	    						curBuddy.mAngleFromNorth = (Math.toDegrees(angleFromNorthInRads)) ;
	    						if(curBuddy.mAngleFromNorth < 0.0) curBuddy.mAngleFromNorth = 360.0 + curBuddy.mAngleFromNorth;		// make angleFromNorth 0-360 like user heading
	
	    						curBuddy.mRelAngleBuddyToUserInDegrees = curBuddy.mAngleFromNorth - mUserHeading;
	    						if(curBuddy.mRelAngleBuddyToUserInDegrees >= 180){
	    							curBuddy.mRelAngleBuddyToUserInDegrees -= 360;
	    						}
	    						else if(curBuddy.mRelAngleBuddyToUserInDegrees < -179){
	    							curBuddy.mRelAngleBuddyToUserInDegrees += 360;
	    						}
	    						double testAngle = Math.abs(curBuddy.mRelAngleBuddyToUserInDegrees);
	    						double iconWidthInDegrees = Math.atan2(curBuddy.mIconWidth,PIXELS_PER_45_DEGREES);
	    						
//	    						Log.e(TAG, "angle to edge of screen: " + String.format("%.2f", (float)ANGLE_TO_EDGE_OF_SCREEN_IN_DEGREES) + " testAngle: " + String.format("%.2f",(float)testAngle) + " " + curBuddy.mRelAngleBuddyToUserInDegrees +" "+curBuddy.mAngleFromNorth +" "+mUserHeading);
//	    						Log.d(TAG, "RelAngleBuddyToUser: " + curBuddy.mRelAngleBuddyToUserInDegrees);
	
	    						if(curBuddy.mDistFromUser < BUDDY_DETECTION_DISTANCE) {
	    							mBuddyCount++;
	    							if(testAngle > 360.0-ANGLE_TO_EDGE_OF_SCREEN_IN_DEGREES*1.1  ||  testAngle < (ANGLE_TO_EDGE_OF_SCREEN_IN_DEGREES*1.1 - iconWidthInDegrees)) {
	    								curBuddy.mDrawingOffset = 214 - curBuddy.mIconWidth/2 + (int) (PIXELS_PER_45_DEGREES * Math.tan(Math.toRadians(curBuddy.mRelAngleBuddyToUserInDegrees))) ;
	    								curBuddy.mNormalBuddyImageView.setImageResource(R.drawable.buddy_active);
	    							}
	    							else {
	    								curBuddy.mNormalBuddyImageView.setImageResource(R.drawable.buddy_inactive);
	    								if(curBuddy.mRelAngleBuddyToUserInDegrees > 0){
	    									curBuddy.mDrawingOffset = 428 - curBuddy.mIconWidth; //put view on right edge of screen
	    								}
	    								else {
	    									curBuddy.mDrawingOffset = 0;  // put view on left edge of screen	    									
	    								}
	    							}
	    						}
//	    						Log.d(TAG, "buddy : " + curBuddy.mName + ", offsetPixels = " + curBuddy.mDrawingOffset + ", angleRelUserInDegrees: " + String.format("%.2f", curBuddy.mRelAngleBuddyToUserInDegrees));
	    					}
	    					// next calculate closest icon position to center, displacing groups horizonally to avoid confusing overlap
	    					CompassBuddyInfo closestBuddyToScreenCenter = null;
	    					int closestIcon = 10000;	
	    					for(CompassBuddyInfo curBuddy : mCurrentBuddies)	{	
	    						curBuddy.mNormalBuddyImageView.getImageMatrix().reset();
	    						curBuddy.mNormalBuddyImageView.getImageMatrix().postTranslate(-100, 0);
	    						curBuddy.mHasFocusBuddyImageView.getImageMatrix().reset();
	    						curBuddy.mHasFocusBuddyImageView.getImageMatrix().postTranslate(-100, 0);
	    						curBuddy.mNormalFadingBuddyImageView.getImageMatrix().reset();
	    						curBuddy.mNormalFadingBuddyImageView.getImageMatrix().postTranslate(-100, 0);
	    						curBuddy.mHasFocusFadingBuddyImageView.getImageMatrix().reset();
	    						curBuddy.mHasFocusFadingBuddyImageView.getImageMatrix().postTranslate(-100, 0);
	    						//    						Log.i(TAG, "-buddy  " + curBuddy.mName + " at dist :" +curBuddy.mDistFromUser);
	    						if(curBuddy.mDistFromUser < BUDDY_DETECTION_DISTANCE) {
	    							if(curBuddy.mDrawingOffset >= -curBuddy.mIconWidth/2) {	//ie, buddy on screen
	    								for(CompassBuddyInfo testBuddy : mCurrentBuddies)	{
	    									if(testBuddy != curBuddy && curBuddy.mDrawingOffset >= testBuddy.mDrawingOffset-1 && curBuddy.mDrawingOffset <= testBuddy.mDrawingOffset+1) {
	    										if(curBuddy.mRelAngleBuddyToUserInDegrees > 0) curBuddy.mDrawingOffset -= 7;
	    										else curBuddy.mDrawingOffset += 7;
	    									}
	
	    								}
	    							}
	    							if(curBuddy.mOnline) {
	    								curBuddy.mNormalBuddyImageView.getImageMatrix().reset();
	    								curBuddy.mNormalBuddyImageView.getImageMatrix().postTranslate(curBuddy.mDrawingOffset, 0);
	    							}
	    							else {
	    								curBuddy.mNormalFadingBuddyImageView.getImageMatrix().reset();
	    								curBuddy.mNormalFadingBuddyImageView.getImageMatrix().postTranslate(curBuddy.mDrawingOffset, 0);
	    							}
	
	    							if(Math.abs(curBuddy.mDrawingOffset - (214-curBuddy.mIconWidth/2)) < closestIcon) {
	    								closestIcon = Math.abs(curBuddy.mDrawingOffset - (214-curBuddy.mIconWidth/2));
	    								closestBuddyToScreenCenter = curBuddy;
	    							}
	    						}
	    						//    						Log.i(TAG, "-buddy  " + curBuddy.mName + " at dist :" +curBuddy.mDistFromUser + ", " + curBuddy.mDrawingOffset);
	    						curBuddy.mNormalBuddyImageView.invalidate();
	    						curBuddy.mHasFocusBuddyImageView.invalidate();
	    						curBuddy.mNormalFadingBuddyImageView.invalidate();
	    						curBuddy.mHasFocusFadingBuddyImageView.invalidate();
	    					}
	
	    					// finally, for closest buddy to center within specific angle to user, emphasize icon and display name and distance
	    					if(closestBuddyToScreenCenter != null && Math.abs(closestBuddyToScreenCenter.mRelAngleBuddyToUserInDegrees) < 20.0) {
	    						showBuddyTextViews();
	    						String postTimeStr = " mins ago";
	    						String preTimeStr = "  ";
	    						int timeSinceBuddyReport = (int)((System.currentTimeMillis() - closestBuddyToScreenCenter.mLastUpdateTime)/60000);
	    						if(timeSinceBuddyReport < 1) {
	    							timeSinceBuddyReport = 1;
	    							preTimeStr = "< ";
	    						}
	    						if(timeSinceBuddyReport == 1) postTimeStr = " min ago";
	    						if(closestBuddyToScreenCenter.mOnline) {
	    							closestBuddyToScreenCenter.mHasFocusBuddyImageView.bringToFront();
	    							closestBuddyToScreenCenter.mHasFocusBuddyImageView.getImageMatrix().reset();
	    							closestBuddyToScreenCenter.mHasFocusBuddyImageView.getImageMatrix().postTranslate(closestBuddyToScreenCenter.mDrawingOffset, 0);
	    							closestBuddyToScreenCenter.mHasFocusBuddyImageView.invalidate();
	    							mNumBuddies.setVisibility(View.VISIBLE);
	    							mNumBuddies.setText("" + preTimeStr + timeSinceBuddyReport + postTimeStr);
	    						}
	    						else {
	    							closestBuddyToScreenCenter.mHasFocusFadingBuddyImageView.bringToFront();
	    							closestBuddyToScreenCenter.mHasFocusFadingBuddyImageView.getImageMatrix().reset();
	    							closestBuddyToScreenCenter.mHasFocusFadingBuddyImageView.getImageMatrix().postTranslate(closestBuddyToScreenCenter.mDrawingOffset, 0);
	    							closestBuddyToScreenCenter.mHasFocusFadingBuddyImageView.invalidate();
	    							mNumBuddies.setVisibility(View.VISIBLE);
	    							mNumBuddies.setText("" + preTimeStr + timeSinceBuddyReport + postTimeStr);
	    						}
	
	    						mBuddyNameTV.setText("" + closestBuddyToScreenCenter.mName  );
	    						int units = SettingsUtil.getUnits(getApplicationContext());
	    						if ( units == SettingsUtil.RECON_UNITS_METRIC) {
	    							if(closestBuddyToScreenCenter.mDistFromUser < 1000) {
	    								mDistanceFromMeTV.setText("  " + (int)closestBuddyToScreenCenter.mDistFromUser + " m");
	    							}
	    							else {
	    								mDistanceFromMeTV.setText("  " + (int)(closestBuddyToScreenCenter.mDistFromUser/1000.0 + 0.5) + " km");
	    							}
	    						}
	    						else { // Imperial
	    							double imperDistInFeet = closestBuddyToScreenCenter.mDistFromUser * 3.28084; // in ft
	    							if(imperDistInFeet < 5280) {
	    								mDistanceFromMeTV.setText("  " + (int)(imperDistInFeet + 0.5) + " ft");
	    							}
	    							else {
	    								mDistanceFromMeTV.setText("  " + (int)(imperDistInFeet/5280.0 + 0.5) + " mi");
	    							}
	    						}
	
	    					}
	    					else {
	    						hideBuddyTextViews();
                                mNumBuddies.setVisibility(View.VISIBLE);
                                if(mBuddyCount == 1) {
                                    mNumBuddies.setText(mBuddyCount + " friend online");
                                }
                                else {
                                    mNumBuddies.setText(mBuddyCount + " friends online");
                                }
	    					}
	
	    					// have location but buddy is too far away
	    					if(mBuddyCount == 0) {
	    						hideTapTextViews();
	    						setRelativeViewMargins(mNumBuddies, 15, 0, 0, 10);
	    					}
	    					else {
	    						hideTapTextViews();
	    						setRelativeViewMargins(mNumBuddies, 15, 0, 0, 10);
	    					}
    					}
    					
    					//if no friends detected
    					else {
    						// if connected but no friends 
    						if(connectionState == 2){
    							showTapTextViews();
    							mNumBuddies.setText(R.string.see_your_friends);
    							setRelativeViewMargins(mNumBuddies, 5, 0, 0, 15);
    						}
    						// if not connected
    						else if(connectionState == 0){
    							hideTapTextViews();
    							mNumBuddies.setText(R.string.connect_smartphone_msg);
    							setRelativeViewMargins(mNumBuddies, 25, 0, 0, 15);
    						}
    						hideBuddyTextViews();
    					}
    				}
    				else { // no location info
    					hideBuddyTextViews();
    					// if connected, but no location info
    					if(connectionState == 2){
    						int buddyCount = mCurrentBuddies.size();

    						// buddies been detected at least once before losing location info
    						if(buddyCount > 0){
    							hideTapTextViews();
    							setRelativeViewMargins(mNumBuddies, 15, 0, 0, 10);
                                mNumBuddies.setText(buddyCount + ((buddyCount == 1) ? " friend " : " friends ") + "online\nWait for GPS fix to see where they are.");
    						}
    						else {
	    						showTapTextViews();
	    						mNumBuddies.setText(R.string.see_your_friends);
	    						setRelativeViewMargins(mNumBuddies, 5, 0, 0, 15);
    						}
    					}
    					
    					// if not connected and no location info
    					else if(connectionState == 0){
    						hideTapTextViews();
    						mNumBuddies.setText(R.string.connect_smartphone_msg);
    						setRelativeViewMargins(mNumBuddies, 25, 0, 0, 15);
    					}
    					mNumBuddies.setVisibility(View.VISIBLE);
    					for(CompassBuddyInfo curBuddy : mCurrentBuddies)	{	
    						curBuddy.mNormalBuddyImageView.getImageMatrix().reset();
    						curBuddy.mNormalBuddyImageView.getImageMatrix().postTranslate(-100, 0);
    						curBuddy.mHasFocusBuddyImageView.getImageMatrix().reset();
    						curBuddy.mHasFocusBuddyImageView.getImageMatrix().postTranslate(-100, 0);
    						curBuddy.mNormalFadingBuddyImageView.getImageMatrix().reset();
    						curBuddy.mNormalFadingBuddyImageView.getImageMatrix().postTranslate(-100, 0);
    						curBuddy.mHasFocusFadingBuddyImageView.getImageMatrix().reset();
    						curBuddy.mHasFocusFadingBuddyImageView.getImageMatrix().postTranslate(-100, 0);
    					}
    				}
    				mNumBuddies.invalidate();           			
    				//                	findViewById(android.R.id.content).invalidate();
    				findViewById(R.id.compass_activity_container).invalidate();
    			}
    		});
    	}
    }	

    public double VerticalDistanceInMeters(double latitude1, double latitude2) 
    {
    	double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

    	return (latitude1-latitude2)/360.0 * meridionalCircumference;
    }

    public double HorizontalDistanceInMeters(double longitude1, double longitude2, double latitude) 
    { 
    	double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth

    	return ((longitude1-longitude2)/360.0) * equitorialCircumference*Math.cos(Math.toRadians(latitude)); 
    }

    BroadcastReceiver buddyInfoReceiver = new BroadcastReceiver()
    {
    	@Override
    	public void onReceive(Context context, Intent intent)
    	{
    		Bundle bundle = intent.getExtras();
    		if(bundle!=null){
    			Log.i(TAG,"onReceive() buddyInfoReceiver ");
    			byte[] bytes = bundle.getByteArray("message");
    			HUDConnectivityMessage cMsg = new HUDConnectivityMessage(bytes);
    			String buddyInfo = new String(cMsg.getData());

    			BuddyInfoMessage message = new BuddyInfoMessage();


    			@SuppressWarnings("unchecked")
    			ArrayList<BuddyInfo> buddyList = (ArrayList<BuddyInfo>) message.parse(buddyInfo);

    			//			Log.i(TAG, "onReceive()");
    			Log.i(TAG, "buddy count: " + buddyList.size());
    			for (BuddyInfo buddy : buddyList)
    			{
    				//				Log.v(TAG, "buddy[" + (index++) + "]: id=" + buddy.localId + ", name=" + buddy.name + ", email=" + buddy.email + 
    				//						   ", location=" + buddy.location.getLatitude() + " " + buddy.location.getLongitude());


    				String nextBuddyName = buddy.name;
    				int	nextBuddyID = buddy.localId ;

    				//			Log.v(TAG,  "buddy "+nextBuddy.name);
    				//			//Log.i(TAG, "buddyInfoID=" + buddyInfo.localId + ", name=" + buddyInfo.name +  ", location=" + buddyInfo.location.getLatitude() + " " + buddyInfo.location.getLongitude());

    				boolean notInCurrentBuddies = true;
    				if(mCurrentBuddies.size() > 0) {
    					for(CompassBuddyInfo curBuddy : mCurrentBuddies)	{	
    						if(curBuddy != null && curBuddy.mName != null) {  
    							if (curBuddy.mName.equals(nextBuddyName) )	{	// if yes, update info... assumes later data is in array
    								curBuddy.mLatitude = buddy.location.getLatitude();
    								curBuddy.mLongitude = buddy.location.getLongitude();
    								curBuddy.mLocationTimestamp = buddy.location.getTime();
    								curBuddy.mLastUpdateTime = System.currentTimeMillis();

    								notInCurrentBuddies = false;
    								break;	
    							}
    						}
    					}
    				}
    				if(notInCurrentBuddies) {					// otherwise add it
    					CompassBuddyInfo curBuddy = new CompassBuddyInfo(mContext, nextBuddyID, nextBuddyName, buddy.location.getLatitude(), buddy.location.getLongitude(), buddy.location.getTime(), System.currentTimeMillis());
    					//if(nextBuddyName.equalsIgnoreCase("scott")) 
    					curBuddy.AddBuddyToView(mScreenLayout);
    					mCurrentBuddies.add(curBuddy);
    					//				Log.v(TAG, "update adding buddy=" + nextBuddyName + "," + curBuddy.mLongitude +  ", " + curBuddy.mLatitude + " - " + curBuddy.mDrawingX +  ", " + curBuddy.mDrawingY);
    				}
    			}
    		}
    	}

    };



    private void addCalibrationOverlay() {
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
    	overlayView = inflater.inflate(R.layout.calib_please_layout, null);
		calibrateScreen = (LinearLayout)overlayView.findViewById(R.id.calibrate_main);
		if(DeviceUtils.isSun()){
			calibrateScreen.setBackgroundResource(R.drawable.compass_calib_default);
		}else{
			calibrateScreen.setBackgroundResource(R.drawable.snow_compass_calib_default);
		}
    	mainLayout.addView(overlayView, new LinearLayout.LayoutParams(mainLayout.getLayoutParams().width, mainLayout.getLayoutParams().height));
    }

    private void removeCalibrationOverlay() {
    	if(overlayView != null){
    		mainLayout.removeView(overlayView);
    		overlayView = null;
    	}
    }
    
    private void resetCalibration() {
    	CompassUtil.copy(CompassUtil.SYSTEM_SENSORS_CONF, CompassUtil.DATA_SENSORS_CONF);
		Settings.Secure.putInt(getContentResolver(), "hasWrittenMagOffsetsV2", 0);
    }

    private void runCalibrationTest() {
    	// Launch calibration if it hasn't been done before
    	removeCalibrationOverlay();
    	
    	if (CompassUtil.DATA_SENSORS_CONF.exists()) {
    		if (!CompassUtil.checkSensorsIsCalibrated()) {
    			resetCalibration();
    		}
    	} else {
    		resetCalibration();
    	}
    	isCalibrationNeeded = Settings.Secure.getInt(getContentResolver(), "hasWrittenMagOffsetsV2", 0)!=1;

    	//isCalibrationNeeded = true;// testing
    	if(isCalibrationNeeded) { 
    		Log.v(TAG, "addCalibration");
    		addCalibrationOverlay();
    	}

    }

    private void hideBuddyTextViews(){
		mBuddyNameTV.setVisibility(View.GONE);
		mDistanceFromMeTV.setVisibility(View.GONE);
		mBuddyNameContainer.setVisibility(View.GONE);
    }
    
    private void hideTapTextViews(){
    	mTapTextContainer.setVisibility(View.GONE);
		mTapButtonText.setVisibility(View.GONE);
		mEngageAppText.setVisibility(View.GONE);
    }
    
    private void showTapTextViews(){
    	mTapTextContainer.setVisibility(View.VISIBLE);
		mTapButtonText.setVisibility(View.VISIBLE);
		mEngageAppText.setVisibility(View.VISIBLE);
    }
    
    private void showBuddyTextViews(){
		mBuddyNameTV.setVisibility(View.VISIBLE);
		mDistanceFromMeTV.setVisibility(View.VISIBLE);
		mBuddyNameContainer.setVisibility(View.VISIBLE);
    }
    
    private void setRelativeViewMargins(View view, int left, int top, int right, int bottom){
    	RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
		params.leftMargin = left;
		params.topMargin = top;
		params.rightMargin = right;
		params.bottomMargin = bottom;
		view.setLayoutParams(params);
    }

}
