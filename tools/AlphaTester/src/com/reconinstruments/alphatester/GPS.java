package com.reconinstruments.alphatester;


import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class GPS extends Activity implements LocationListener {

	final String TAG = "AlphaTest_GPS";
	double lat;
	double lon;
	TextView textLat;
	TextView textLon;
	TextView textStatus;
	LocationManager locationManager;
	LocationListener gpslocationListener;
	private Timer myTimer;
	final int timeout = 300;
	boolean Complete = false;
	int remain=0;
	long mLockTime=-1;
	String status1 = "Uninitialized";
	String status2 = "";
	String status3 = "";
	String status4 = "";
	String csv = "";
	private double prevLat = -1000;
	private double prevLong = -1000;
	private float prevSpd = -1;
	private float prevAlgSpd = -1;
	
	String systemSpdTxtView, prevSystemSpdTxtView, algSpdTxtView, 
		prevAlgSpdTxtView, alt, accuracy, delloc, delspd;
	
	boolean isGPSEnabled=false;
	public static boolean Locked = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gps);
		textLat = (TextView) findViewById(R.id.textLat);
		textLon = (TextView) findViewById(R.id.textLon);
		textStatus = (TextView) findViewById(R.id.textView1);
		
		locationManager = (LocationManager)
				getSystemService(Context.LOCATION_SERVICE);
	
		if (locationManager != null)
		{
            locationManager.addGpsStatusListener(mGPSStatusListener); 
            gpslocationListener = new LocationListener() 
            {
                public void onLocationChanged(Location loc) 
                {                                                           
                }
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {}                          
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		
		
		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {			
			@Override
			public void run() {
				TimerMethod();
			}
			
		}, 0, 1000);		
		
	}
	

	void dumpfile(String timestamp)
	{
		String filename = "gps_speed." + timestamp + ".csv";
		FileOutputStream outputStream;
		try {
		  outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
		  outputStream.write(csv.getBytes());
		  outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}		
	}
	void spikefile(String timestamp)
	{
		String filename = "gps_speed_spike_detected." + timestamp;
		FileOutputStream outputStream;
		try {
		  outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
		  outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}		
	}	
	
	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			textStatus.setText(status1 + "\n" + status2 + "\n" + status3 + "\n" + status4);
		//This method runs in the same thread as the UI.
			if (Locked)
			{
				int elapsed = (int) ((SystemClock.elapsedRealtime() - mLockTime)/1000);
				status1 = "" + (elapsed < 60 ? elapsed : "" + elapsed/60 + ":" + String.format("%02d",elapsed % 60));
				// print mm:ss format				
				if ((elapsed >= timeout) && (Complete == false))
				{
					Complete = true;
					status4 = "\nTest complete";
					String Timestamp = (String) (android.text.format.DateFormat.format("yyyy-MM-dd_hhmmss", new java.util.Date()));
					spikefile(Timestamp);
					dumpfile(Timestamp);
				}
				

			}
			else
				mLockTime = SystemClock.elapsedRealtime();
		

		}
	};	
	private void TimerMethod()
	{
		//This method is called directly by the timer
		//and runs in the same thread as the timer.

		//We call the method that will work with the UI
		//through the runOnUiThread method.
		this.runOnUiThread(Timer_Tick);
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.g, menu);
		return true;
	}

	private DecimalFormat df = new DecimalFormat("####.#");

	@Override
	public void onLocationChanged(Location location) {
		textLat.setText("" + location.getLatitude());
		textLon.setText("" + location.getLongitude());
		status3 = "" + location.getSpeed()*3.6f + " km/h (android)";
		
		float newSpd = location.getSpeed()*3.6f;
		float algSpd = newSpd;
		double newLat = location.getLatitude();
		double newLong = location.getLongitude();
		double newAlt = location.getAltitude();
		double newAccuracy = location.getAccuracy();
		double deltaLoc = 0;
		double deltaSpeed = 0;
		if(prevLat != 1000) {
			float [] dist = new float[1];
			Location.distanceBetween(prevLat, prevLong, newLat, newLong, dist);
			deltaLoc = dist[0];
		}
		
//		Math.sqrt(Math.pow((), 2) + Math.pow((), 2)) : 0;
		String newDelloc = df.format((deltaLoc));
		deltaSpeed = deltaLoc*3.6;
		String newDelSpd = df.format((deltaSpeed));
		
		if(deltaSpeed < algSpd) {
			algSpd = (float)deltaSpeed;
		}		
	
		systemSpdTxtView = (df.format(newSpd));
		algSpdTxtView = (df.format(algSpd));
		prevSystemSpdTxtView = (df.format(prevSpd));
		prevAlgSpdTxtView = (df.format(prevAlgSpd));

		status2 = "" + deltaSpeed + " km/h (calc)";
		csv = csv + newSpd + "\n";
		Log.d(TAG,"GPS speed: " + newSpd);

		delspd = (newDelSpd);
			
		alt = (df.format(newAlt));
		accuracy = (df.format(newAccuracy));
		delloc = (newDelloc);
		
		prevSpd = newSpd;
		prevAlgSpd =algSpd;
		prevLong = newLong;
		prevLat = newLat;		
		
	}



	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	public Listener mGPSStatusListener = new GpsStatus.Listener()
	{    
	    public void onGpsStatusChanged(int event) 
	    {       
	        switch(event) 
	        {
	            case GpsStatus.GPS_EVENT_STARTED:
	                Log.d(TAG, "GPS_SEARCHING");
	                status1 = "GPS searching";
	                
	                Locked = false;
	                mLockTime = SystemClock.elapsedRealtime();
	                
	                break;
	            case GpsStatus.GPS_EVENT_STOPPED:   
	            	status1 = "GPS off";
	            	mLockTime = SystemClock.elapsedRealtime();
	                Locked = false;
	                break;
	            case GpsStatus.GPS_EVENT_FIRST_FIX:

	                /*
	                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked            
	                 */
	                    Log.d(TAG,"GPS_LOCKED");
	                    Locked = true;
	                    mLockTime = SystemClock.elapsedRealtime();
	                    Location gpslocation = locationManager
	                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);

	                    if(gpslocation != null)
	                    {
	                    	System.out.println("GPS Info:"+gpslocation.getLatitude()+":"+gpslocation.getLongitude());
	                        locationManager.removeGpsStatusListener(mGPSStatusListener);                
	                    }        

	                break;
	            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	 //                 System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
	                break;                  
	       }
	   }
	};  
}
