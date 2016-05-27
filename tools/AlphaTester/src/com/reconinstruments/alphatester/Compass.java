package com.reconinstruments.alphatester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.reconinstruments.os.hardware.led.HUDLedManager;

public class Compass extends Activity implements SensorEventListener, LocationListener{

	private static final String TAG = "AlphaTester_Compass";
	TextView Yaw;
	TextView tTarget;
	TextView tList;
	SensorManager sm;
	GeomagneticField gm;
	int TargetAngle=-1;
	final int SamplesRequired = 10;
	int SampleCount=0;
	boolean Testing=false;
	boolean Stable = false;
	boolean Paused = false;
	Timer myTimer;
	double dAccum = 0;
	double[] dAvg = {-1,-1,-1,-1,-1,-1,-1,-1};
	String[] NESW = {"N","NE","E","SE","S","SW","W","NW"};
	String csv = "";
	
	LocationManager locationManager; // for declination
	float decl = 0;
	Button b1;
	
	// temperature variables
	private int mPmicTemp,mXMTemp;
	String[] temperatureStrArr;

        HUDLedManager mLedMgr = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compass);
		Yaw = (TextView) findViewById(R.id.textViewAngle);
		tTarget = (TextView) findViewById(R.id.textViewDecl);
		tList = (TextView) findViewById(R.id.textView1);
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 1000);
		sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), 1000);
		b1 = (Button) findViewById(R.id.buttonStartStop);
		locationManager = (LocationManager)
				getSystemService(Context.LOCATION_SERVICE);
	
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
		
		temperatureStrArr = new String [8];

		mLedMgr = HUDLedManager.getInstance();
        
	}
    public void onClickBtn2(View v)
    {
    	Testing = false;
    	TargetAngle = 0;
		b1.setText("Start");
		disableXMTemp();
    }

    public void onClickBtn(View v)
    {   
//		myTimer = new Timer();
//		myTimer.schedule(new TimerTask() {			
//			@Override
//			public void run() {
//				TimerMethod();
//			}
//			
//		}, 0, 1000);		
    	if (Testing == false)
    	{
    		Testing = true;
    		TargetAngle = 0; 	
    		enableXMTemp();
    	}
    	else
    	{
    		Paused = false;	
    	}
    	
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.compass, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {	
	}
	
    public void blinkLED(int delay) {
	try {
	    int[] pattern = new int[] {0,delay};
	    if(mLedMgr != null) mLedMgr.blinkPowerLED(62,pattern);
	} catch (RuntimeException e) {
	    Log.e(TAG, "Failed to blink.");
	    throw e;
	}
    }


    
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
		{
	        float[] rotationVector = event.values;
	        float[] rotationMatrix = new float[9];
	        float[] orientation = new float[3];
	
	        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
	        SensorManager.getOrientation(rotationMatrix, orientation);
	        
	        double yaw = (orientation[0]/Math.PI)*180+decl;
	        
	        Yaw.setText("Yaw: " + (Math.round(yaw*10)/10f) + " (declin: " + Math.round(decl*10)/10 + ")");
	        //Log.d(TAG,"Yaw: " + (Math.round(yaw*10)/10f) + " (declin: " + Math.round(decl*10)/10 + ")");
	        
	        if ((Stable == true) && (Testing == true) && (Paused == false))
	        {
	        	csv = csv + yaw + "\n";	        	
	        	if (SampleCount == SamplesRequired)
	        	{
	        		// read temperature from PMIC and Magnetometer
	        		mPmicTemp = readPmicTemp();
					mXMTemp = readXMTemp();
					temperatureStrArr[TargetAngle] = Integer.toString(mPmicTemp) + ","+ Integer.toString(mXMTemp) +"\n";					
	        		
	        		
	        		dAvg[TargetAngle] = (dAccum / SamplesRequired + 360) % 360;
	        		Log.d(TAG,"Target: " + NESW[TargetAngle] + "(" + TargetAngle*45 + ") Actual: " + dAvg[TargetAngle]);
	        		dAccum = 0;
	        		SampleCount = 0;
	        		Paused = true; // unpaused by button press (could be a timer)
	        		
	        		// once per angle
	        		TargetAngle++;
	        		TargetAngle %= 8;
	        		tTarget.setText("" + TargetAngle*45 + NESW[TargetAngle]);
	        		b1.setText("Capture "+ TargetAngle*45);
	        		if (TargetAngle == 0)
	        		{
	        			Testing = false;
	        			b1.setText("Start");
	        			double off[] = {0,0,0,0,0,0,0,0};
	        			double max = 0;
	        			String MaxLoc = "";
	        			// we are done, calculate worst nonlinearity
	        			for (int i=0; i<8; i++)
	        				if (i==0)
	        					off[i] = Math.min(Math.abs(45*i - dAvg[i]), Math.abs(360 - dAvg[i]));
	        				else
	        					off[i] = Math.abs(45*i - dAvg[i]);
	        			String list = "";
	        			for (int i=0; i<8; i++)
	        			{
	        				list = list + NESW[i] + ": " + Math.round(dAvg[i]*10)/10 + "\n"; 
	        				if (off[i] > max)
	        				{
	        					max = off[i];
	        					MaxLoc = NESW[i];
	        				}
	        			}
	        			tList.setText(list);
	        			tTarget.setText("Worst nonlinearity (" + MaxLoc + "): " + Math.round(max*10)/10);
	        			blinkLED(300);
	        			dumpfile();
	        			
	        			disableXMTemp();
	        		}
	        		else
	        			blinkLED(100);
	        	}
	        	else
	        	{
	        		dAccum += yaw;
	        		SampleCount++;
	        	}
	        }
		}
		else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
		{
			double gyro_norm = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
			if (gyro_norm < 0.75f)
			{
				Yaw.setTextColor(Color.BLUE);
				Stable = true;
			}
			else
			{
				Yaw.setTextColor(Color.RED);
				Stable = false;
			}
			Log.d(TAG,"Gyro: " + gyro_norm);
		}
		
	}
	void dumpfile()
	{
		String filename = "linearity." + (android.text.format.DateFormat.format("yyyy-MM-dd_hhmmss", new java.util.Date())) + ".csv";
		FileOutputStream outputStream;
		try {
		  outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
		  outputStream.write(csv.getBytes());
		  outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}		
		
		
		// write temperature log to file
		filename = "temperature." + (android.text.format.DateFormat.format("yyyy-MM-dd_hhmmss", new java.util.Date())) + ".csv";
		
		try {
		  outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
		  for (int i=0;i<8;i++)
			  outputStream.write(temperatureStrArr[i].getBytes());
		  outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}	
		
	}
	@Override
	public void onLocationChanged(Location location) {
		gm = new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(), (float) location.getAltitude(), System.currentTimeMillis() );
		//decl = gm.getDeclination();
		decl = 0;
		locationManager.removeUpdates((LocationListener) this);	
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	private int readPmicTemp(){

		File file = new File("/sys/devices/platform/omap/omap_i2c.4/i2c-4/4-0049/temperature");
		//Read text from file
		StringBuilder text = new StringBuilder();
		int pmicTemp = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text.append(line);
				//		        text.append('\n');
				//		    	Log.d("DARRELL","PMIC TEMP: "+ line);
			}
		}
		catch (IOException e) {
			//You'll need to add proper error handling here
		}


		pmicTemp = (int) Integer.parseInt(text.toString());
		Log.d("DARRELL","PMIC TEMP: "+ pmicTemp);
		return pmicTemp;
	}
	
	private void enableXMTemp(){
		Log.d("DARRELL","enableXMTemp!");
		String strFilePath = "/sys/class/misc/jet_sensors/mag_temp_enable";
		String content = "1";
		//create FileOutputStream object
		try {
			FileOutputStream fos = new FileOutputStream(strFilePath);

			byte[] contentInBytes = content.getBytes();
			fos.write(contentInBytes);

			fos.flush();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}		
	}
	
	private void disableXMTemp(){
		Log.d("DARRELL","disableXMTemp!");
		String strFilePath = "/sys/class/misc/jet_sensors/mag_temp_enable";
		String content = "0";
		//create FileOutputStream object
		try {
			FileOutputStream fos = new FileOutputStream(strFilePath);

			byte[] contentInBytes = content.getBytes();
			fos.write(contentInBytes);

			fos.flush();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}		
	}
	
	private int readXMTemp(){
		
		File file = new File("/sys/class/misc/jet_sensors/mag_temp");
		//Read text from file
		StringBuilder text = new StringBuilder();
		int xmTemp = 0;
		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    
		    while ((line = br.readLine()) != null) {
		        text.append(line);
//		        text.append('\n');
//		    	Log.d("DARRELL","PMIC TEMP: "+ line);
		    	
		
		    }
		}
		catch (IOException e) {
		    //You'll need to add proper error handling here
		}

		xmTemp = (int) Integer.parseInt(text.toString());
		Log.d("DARRELL","XM TEMP: "+ xmTemp);
		return xmTemp;
		
	}

}
