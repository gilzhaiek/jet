//package com.reconinstruments.applauncher.transcend;
package com.reconinstruments.jumpdetection;

import com.reconinstruments.reconsensor.*;
import com.reconinstruments.reconsensor.ReconSensorManager.ReportingMode;

import android.content.Context;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;


public class JumpAnalyzer implements SensorEventListener 
{
	// standard ident tag
	private static final String TAG = "RECON.JUMP.JumpAnalyzer";
	public static boolean DEBUG_MODE = false; 
	private int mQueue_rate_ms = 0;              // fast queue rate; always adjusted on slowest sensor
  
    // singleton pattern
    static  JumpAnalyzer  m_instance = null; 
    private JumpAnalyzer () {}
  
    public static JumpAnalyzer Instance(Context c) 
    {
    	if (m_instance == null)
        {
    		m_instance = new JumpAnalyzer ();
        
            // get the instance of ReconSensorManager. This can fail if native so
            // can not be found, or  MUX can not be open in which case we will bail
       	    try 
		    {
       	    	m_instance.mReconSensor = ReconSensorManager.Initialize(c);
       	    	Log.d(TAG, "RECON Sensor Manager initialized successfully!");
			
			    // get standard Android sensor manager
                m_instance.mSensorManager = (SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
        
                // verify accelerometer and pressure sensors are available
                m_instance.mAccelSensor = m_instance.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                m_instance.mPressureSensor = m_instance.mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            
                if ( (null == m_instance.mAccelSensor) || (null == m_instance.mPressureSensor) )
                {
            	    Log.d(TAG, "Accelerometer and/or Pressure Sensors are not detected");
            	    m_instance = null;
                }
                else
                {
                	// allocate status objects for both sensors
            	    m_instance.mAccelStatus    = new SensorStatus();
            	    m_instance.mPressureStatus = new SensorStatus();
            	
            	    // allocate engine (algorithm) object
            	    m_instance.mEngine = new JumpEngine();
                }
		
		    } 
       	    catch (Exception e) 
		    {
			    Log.e(TAG, "RECON Sensor Manager failed to initialize. Reason: " + e.getMessage() );
			    m_instance = null;
		    }

        }
    
    	return m_instance;
    }
   
    private ReconSensorManager mReconSensor;
    private SensorManager      mSensorManager;    // Android SensorManager
   
    private Sensor             mAccelSensor;      // standard Accelerometer Sensor
    SensorStatus               mAccelStatus;      // accelerometer configuration
   
    private Sensor             mPressureSensor;   // standard Pressure Sensor
    SensorStatus               mPressureStatus;   // pressure configuration
   
    private JumpEndEvent       mIJumpEnd;         // jump end callback, registered by parent
   
    private JumpEngine         mEngine;           // raw algorithm: Gory details
  
    /* public export triggered by Client who instantiated us in the first place
      when he detected FreeFall IRQ. */
  
    public void Begin 
    (
        JumpEndEvent cbk,           // callback to invoke when we are done
        float        pressure,      // last parent measured pressure  -- ReconTranscendService.getReconAltManager().getPressure();
	    long         date,          // freefall detection timestamp
	    long         lastUpdateTime // Last Transcend tick -- ReconTranscendService.mTimeMan.mLastUpdate
    ) 
    {
       // remember callback we'll fire once we detect landing
       mIJumpEnd = cbk;
       if (null == mIJumpEnd) 
       {
    	   Log.e(TAG, "Can not start Jump Processing: Passed Callback is null");
    	   return;
       }
       
       // reset engine with passed pressure and last update time passed 
       // from parent who is using us
       mEngine.Init(pressure, lastUpdateTime, date);
       
       // get status of accel and pressure sensors, so we can flip back if either is in FIFO mode
       mReconSensor.getSensorStatus(mAccelSensor, mAccelStatus);
       mReconSensor.getSensorStatus(mPressureSensor, mPressureStatus);
       
       // delay will be slower fastest rate between the two
       mQueue_rate_ms = mAccelStatus.FastestRate();
       if (mPressureStatus.FastestRate() > mQueue_rate_ms)
    	   mQueue_rate_ms = mPressureStatus.FastestRate();
       
       // change both to CR mode. If this fails, we'll still continue
       if (mAccelStatus.Mode() != ReportingMode.SENSOR_MODE_CR)
       {
          if (mReconSensor.setReportingMode(mAccelSensor, ReportingMode.SENSOR_MODE_CR) != 0)
    	      Log.i(TAG, "Accelerometer Sensor could not be changed to CR mode");
       }
       
       if (mPressureStatus.Mode() != ReportingMode.SENSOR_MODE_CR)
       {
          if (mReconSensor.setReportingMode(mPressureSensor, ReportingMode.SENSOR_MODE_CR) != 0)
    	      Log.i(TAG, "Pressure Sensor could not be changed to CR mode");
       }
       
       // put both to fast custom queue (i.e. synchronized sensor)
       Sensor [] s = new Sensor [2];
       s[0] = mAccelSensor;
       s[1] = mPressureSensor;
       
       if (mReconSensor.startSensorQueue(s, mQueue_rate_ms) != 0)
       {
       	   Log.e(TAG, "Not Processing FreeFall Event: Could not start Fast sensor queue for Accelerometer/Pressure sensors");
       	   	   
       	   // change accel & pressure sensors back if we flipped mode
	       if (mAccelStatus.Mode() != ReportingMode.SENSOR_MODE_CR)
		      mReconSensor.setReportingMode(mAccelSensor, mAccelStatus.Mode() );
	   
	       if (mPressureStatus.Mode() != ReportingMode.SENSOR_MODE_CR)
		      mReconSensor.setReportingMode(mPressureSensor, mPressureStatus.Mode() );
	       
       	   mIJumpEnd.landed(null);
       }
       else
       {
    	   // start harvesting events standard Android route
           mSensorManager.registerListener(this, mAccelSensor, mQueue_rate_ms * 1000);
           mSensorManager.registerListener(this, mPressureSensor, mQueue_rate_ms * 1000);
       }
  }
	
   // this can be regarded as state machine end; reset everything back to start position
   // and wait for next Start when parent detects new FreeFall
   private void ResetSensors() 
   {
	   // first unregister accel/pressure sensors
	   mSensorManager.unregisterListener(this);
	   
	   // change accel & pressure sensors back if we flipped mode
	   if (mAccelStatus.Mode() != ReportingMode.SENSOR_MODE_CR)
		   mReconSensor.setReportingMode(mAccelSensor, mAccelStatus.Mode() );
	   
	   if (mPressureStatus.Mode() != ReportingMode.SENSOR_MODE_CR)
		   mReconSensor.setReportingMode(mPressureSensor, mPressureStatus.Mode() );
	   
	   // pause a bit, then clean-up custom queue we created
	   try 
	   {
		   Thread.sleep(100);
	   } 
	   catch (InterruptedException e)
	   {
	   }
	   
	   mReconSensor.stopSensorQueue(mQueue_rate_ms); 
   }
	
   // this method is required to be implemented by framework but it is bull so leave it empty
   public void onAccuracyChanged(Sensor sensor, int accuracy) 
   {
   }

   // key Jump Detection algorithm. This code is HUGE mess
   // and needs to be cleaned up & abstracted properly
   public void onSensorChanged(SensorEvent event)
   {  
	  // check on top is if we get event when we have already finished
      if (mEngine.AirState().equals (JumpEngine.AirState.LANDED) )
         return;
        
      if (event.sensor.getType() == mPressureSensor.getType() )
    	  mEngine.processPressure(event.values[0]);
      
      else if (event.sensor.getType() == mAccelSensor.getType() )
      {
    	  JumpEngine.AirState airState = mEngine.processAccelerometer(event.values);
    	  if (airState == JumpEngine.AirState.LANDED)
    	  {
    		  // stop all sensors; this deregisters listeners and puts sensors back to default state
    		  this.ResetSensors();
    		  
    		  // ask engine for Jump object; if we get null, that means we landed but
    		  // got no jump which is ok; report that back to client
    		  ReconJump jump = mEngine.getJump();
    		  
    		  // now reset engine, which also stop logging
    		  mEngine.Reset();
    		  
    		  // and fire back to client
    		  mIJumpEnd.landed(jump);
    	  }
      }
      
      else  // should never happen!!
      {
    	  Log.e(TAG, String.format("Android Error: Invalid Sensor Event type (%d) received!", 
    			  event.sensor.getType() ));
    	  
    	  return;
      }
      
 
  }
  
}

