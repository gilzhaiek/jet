package com.reconinstruments.jumpdetection;
 

import java.util.Date;

import com.reconinstruments.jump.R;
import com.reconinstruments.reconsensor.ReconSensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class JumpActivity extends Activity implements JumpEndEvent
{
	private static final String TAG = "RECON.JUMP";  
	    
	// sensor manager and Recon FreeFall sensor
	private SensorManager mSensorManager  = null;    // android sensor manager
	private Sensor        mFreeFallSensor = null;    // Recon FreeFall sensor
	private Sensor        mPressureSensor = null;    // Pressure Sensor (Transcend does this all the time)
	
	private float         mLastPressure       = 0f;
	private long          mLastUpdateTime     = 0;
	
	
	// Instance of Jump Analyzer and Callback
	private JumpAnalyzer  mJumpAnalyzer = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// get standard Android sensor manager
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		 
		// allocate JumpAnalyzer. This will hook-up Recon SDK native code
		mJumpAnalyzer = JumpAnalyzer.Instance(this);
		
		if (null == mJumpAnalyzer)
		{
			Log.e(TAG, "Could not allocate JumpAnalyzer; FreeFall processing disabled");
		}
		else
		{
			// Get FreeFall and Pressure Sensors
			mPressureSensor    = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			if (null == mPressureSensor)
				Log.e(TAG, "Pressure Sensor not available -- FreeFall Events will not be processed");
			else
		       mFreeFallSensor    = mSensorManager.getDefaultSensor(ReconSensorManager.TYPE_FREEFALL);
	        
	        if (null == mFreeFallSensor) Log.i(TAG, "FreeFall Sensor Not Available  -- FreeFall Events will not be processed");
	        else
	        {
	        	Log.i(TAG, "FreeFall and Pressure Sensors detected. Recon JUMP Feature supported!!");
        
	        }
		}
		
        setContentView(R.layout.activity_main);
	}
	


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	} 
	
	 private final SensorEventListener mPressureListener = new SensorEventListener ()
	 {
		 public void onAccuracyChanged(Sensor sensor, int accuracy)
		 {	
	     }
		 
		 public void onSensorChanged(SensorEvent event) 
		 {
		     // remember last pressure measurement and timestamp for analyzer
			 mLastPressure     = event.values[0];
			 mLastUpdateTime   = SystemClock.elapsedRealtime();
		 }
	 };
	 
     private final SensorEventListener mFreeFallListener = new SensorEventListener ()
	 {
		 public void onAccuracyChanged(Sensor sensor, int accuracy)
		 {			
	     }
		 
		 public void onSensorChanged(SensorEvent event) 
		 {
			 Date jstart = new Date ( event.timestamp / 1000000);
			 String strLog = String.format("FreeFall Event Received!  Kernel Date: %s. Starting JumpAnalyzer... ",
					 jstart.toString() );
			 
			 Log.i(TAG, strLog);
			 
	         // unregister FreeFall Listener first
			 mSensorManager.unregisterListener(this);
			 
			 // now start JumpAnalyzer which will call us back once Landing is detected
			 // with, or without valid Jump Object
			 mJumpAnalyzer.Begin(
					 JumpActivity.this,     // callback to invoke when Landing is detected, with or without Jump object
					 mLastPressure,         // last pressure measurement we did
					 mLastUpdateTime,       // timestamp of last pressure measurement
					 SystemClock.elapsedRealtime() );       // freefall timestamp
		 }
	 };
	 
    @Override
    protected void onPause ()
    {
    	 mSensorManager.unregisterListener(mFreeFallListener);
    	 mSensorManager.unregisterListener(mPressureListener);
    }
    
	@Override
   	protected void onResume()
    {
       super.onResume();  

       // re-register freefall and pressure listeners
       mSensorManager.registerListener(
    		   (SensorEventListener) mFreeFallListener,	
    		   mFreeFallSensor, 
    		   SensorManager.SENSOR_DELAY_FASTEST);  // ignored for IRQ sensors
       
       mSensorManager.registerListener(
    		   (SensorEventListener) mPressureListener,	
    		   mPressureSensor, 
    		   SensorManager.SENSOR_DELAY_NORMAL);  
    }



	@Override
	public void landed(ReconJump jump)
	{
        // Transcend will do its stuff. Here we just log 
		// (and demonstrate what needs to be done)
		Log.i(TAG, "Landing Detected");
		if (null == jump)
		{
			Log.i(TAG, "No Jump Conditions");
		}
		else
		{
			// update last update time
			mLastUpdateTime = jump.mDate;
			
			// log what we got
			String strLog = String.format(
					"New Jump Detected! Date: [%d], Air: [%d], Distance: [%f], Drop: [%f], Height: [%f]",
					jump.mDate, jump.mAir, jump.mDistance, jump.mDrop, jump.mHeight);

            Log.i(TAG, strLog);
		}
		
		// now re-register FreeFall listener for new one
        mSensorManager.registerListener(
    		   (SensorEventListener) mFreeFallListener,	
    		   mFreeFallSensor, 
    		   SensorManager.SENSOR_DELAY_FASTEST);  // ignored for IRQ sensors
		
  		
	}
       
}
