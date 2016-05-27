package com.reconinstruments.reconsensor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.annotation.SuppressLint;
import android.content.Context;


/* Recon Extension of standard Android Sensors Library */
public class ReconSensorManager 
{
	// standard Ident tag for logcat filtering
	private static final String TAG = "RECON.SensorManager"; 
	
    // native so file name
	private static final String SHARED_LIB = "reconsensor";    // "libreconsensor.so"
	
    // A constant describing RECON FreeFall Sensor Event Type
    public static final int TYPE_FREEFALL = 17;
    
    // session context pointer. Can be anything -- i.e. native side class. Initially
	// this is only open handle to MUX device
	private  int m_context = 0;
	 
    /** 
     *  Sensor Reporting Mode Enumeration 
     */
    public enum ReportingMode
    {
    	SENSOR_MODE_CR(1),
    	SENSOR_MODE_FIFO(2);
    	
    	private final int mode;
 
        private ReportingMode(int m) {mode = m;}
 
        // Mapping mode to mode id
	    @SuppressLint("UseSparseArrays")
		private static final Map<Integer, ReportingMode> _map = new HashMap<Integer, ReportingMode>();
        static
        {
            for (ReportingMode sensormode : ReportingMode.values())
              _map.put(sensormode.mode, sensormode);
        }
 
        public static ReportingMode from(int value)
        {
           return _map.get(value);
        }
      };
    
    private SensorManager mSensorManager = null;
    private static ReconSensorManager mInstance = null;
    
    // cache container
    Map<Integer, SensorCache> mCache = null;
      
    // Singleton Pattern
    private ReconSensorManager () {}
    
    // virtual c-tor: get instance of ReconSensorManager
    public static ReconSensorManager Initialize (Context c)  throws UnsatisfiedLinkError, IOException
    {
    	if (null == ReconSensorManager.mInstance)
    	{
    		Log.d(TAG, "Loading Native Library...");
    		
    		// load native shared library. This will automatically throw if libRECON-native.so can not be found
			System.loadLibrary(ReconSensorManager.SHARED_LIB);
			 
			Log.d(TAG, "Native shared Library loaded!");
			
    		// allocate ourselves
    		ReconSensorManager.mInstance = new ReconSensorManager();
    		ReconSensorManager.mInstance.mCache = new TreeMap<Integer, SensorCache>();
    		
    		// initialize native layer -- this connects with MUX device
			ReconSensorManager.mInstance.m_context = SensorNative.openDevice();
			if (ReconSensorManager.mInstance.m_context == 0)
			{
				 ReconSensorManager.mInstance = null;
				 throw new IOException ("Failed to Establish Session with MUX Device");
			}
			
			Log.d(TAG, "Native Device opened successfuly. Initialization complete!");
			
    		// get android sensor manager reference
    		ReconSensorManager.mInstance.mSensorManager = (SensorManager)c.getSystemService(Context.SENSOR_SERVICE);
    	}
    	
    	return mInstance; 
    }
 
    // Recon API: changes reporting mode for passed sensor
    public int setReportingMode (Sensor s, ReconSensorManager.ReportingMode mode)
    {
    	return SensorNative.setReportingMode (m_context, 1 << (s.getType() - 1), mode.mode);
    }
    
    // Recon API gets reporting mode for passed sensor
    public int getSensorStatus (Sensor s, SensorStatus status )
    {
    	int result = SensorNative.getSensorStatus (
    			m_context,
    			1 << (s.getType() - 1),
    			status);
    	
        return result;
    }
    
    // start custom kernel queue with passed sensor mask
    public int startSensorQueue (Sensor [] sensors, int delay_ms)
    {
       int mask = 0x00;
       for (int i = 0; i < sensors.length; i++)
       {
          mask |= 1 << (sensors[i].getType() - 1); 
       }
    
       return SensorNative.startQueue (m_context, mask, delay_ms);
    }
    
    // stop custom kernel queue identified by passed milisecond delay
    public int stopSensorQueue (int delay_ms)
    {
    	return SensorNative.stopQueue (m_context, delay_ms);
    }

    // start caching of sensor data
    public void startSensorCache (Sensor s, int size)
    {
    	SensorCache cache = (SensorCache) mCache.get(s.getType() );
    	if (null != cache) this.stopSensorCache(s);
    	
    	cache = new SensorCache(size);
    	mCache.put(s.getType(), cache);
    	
    	// what is the rate of this sensor?
    	SensorStatus sensorStatus = new SensorStatus();
    	this.getSensorStatus(s, sensorStatus);
    	
    	mSensorManager.registerListener(
    		   (SensorEventListener) cache,	
    		   s, sensorStatus.CurrentRate() * 1000000); 
    }
    
    // stop caching of sensor data
    public void stopSensorCache (Sensor s)
    {
    	SensorCache cache = (SensorCache) mCache.get(s.getType() );
    	if (null == cache)
    	{
    		String strLog = String.format("Cache for Sensor [0x%d] has not been configured!", 1 << (s.getType() - 1));
    		Log.i(TAG, strLog);
    		
    		return;
    	}
    	
    	// unregister event listener
    	mSensorManager.unregisterListener(cache);
    	
    	// remove cache itself
    	mCache.remove(s.getType() );
    }
    
    // retrieve cache for particular sensor. Returns # of events filled,
    // which can be less that client allocated array
    public int getSensorCache (Sensor s, ReconSensorEvent [] events)
    {
        // get the object
    	SensorCache cache = (SensorCache) mCache.get(s.getType() );
    	if (null == cache)
    	{
    		String strLog = String.format("Cache for Sensor [0x%d] has not been configured!", 1 << (s.getType() - 1));
    		Log.i(TAG, strLog);
    		
    		return 0;
    	}
    	
    	return cache.getCache(events);
    }
}
