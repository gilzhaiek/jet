package com.reconinstruments.reconsensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

class SensorCache implements SensorEventListener 
{
   private ReconSensorEvent [] mEvents;   // bag of Events
   private int mNext;                     // next insertion point
   private int mCapacity;                 // this cache capacity
   
   @SuppressWarnings("unused")
   private SensorCache(){mEvents = null; mNext = 0; mCapacity = 0;}
   
   // initializing  c-tor
   public SensorCache (int size)
   {
	   mEvents = new ReconSensorEvent[size];
	   mNext = 0; mCapacity = size;
   }
   
   
   // cache passed event. 
   private void cacheEvent (SensorEvent evt)
   {
	  synchronized (this)
	  {
	      if (mNext == mCapacity )
	    	  mNext = 0;
	      
	      if (null == mEvents[mNext])
	         mEvents[mNext] = new ReconSensorEvent();
	      
	      ReconSensorEvent.copyEvent(evt, mEvents[mNext]);
	      
	 /*     String strLog = String.format("Cached Event at Offset: %d. X [%f], Y [%f], Z [%f]",
	    		  mNext, mEvents[mNext].values[0], mEvents[mNext].values[1], mEvents[mNext].values[2]);
	      
	      Log.d("RECON.JUMP", strLog); */
	      
	      mNext++;
	  }
	  
   } 
   
   // retrieve cache. Returns # of elements actually filled
   public int getCache (ReconSensorEvent [] events)
   {
	  int iFilled = 0;
	  
	  Log.d("RECON.JUMP", "SensorCache::getCache");
	  synchronized (this)
	  {
          for (int i = mNext; i < mCapacity; i++)
		  {
        	 if (null == mEvents[i]) break;   // hasn't been alloc yet
		     if (iFilled == events.length) return iFilled;  // client size exhausted
		       
		     events[iFilled] = (ReconSensorEvent) mEvents[i].clone();
	          
		          
		/*             String strLog = String.format("Transfered event at offset: %d. X [%f], Y [%f], Z [%f]",
	    		  i, events[iFilled].values[0], events[iFilled].values[1], events[iFilled].values[2]);
	      
	      Log.d("RECON.JUMP", strLog);*/
	      
		     iFilled++;
		  }
	
	      for (int i = 0; i < mNext; i++)
	      {
	    	  if (null == mEvents[i]) break;   // hasn't been alloc yet
	          if (iFilled == events.length) return iFilled;  // client size exhausted
	          
	          events[iFilled] = (ReconSensorEvent) mEvents[i].clone();
	          
	  /*        String strLog = String.format("Transfered event at offset: %d. X [%f], Y [%f], Z [%f]",
	    		  i, events[iFilled].values[0], events[iFilled].values[1],events[iFilled].values[2]);
	      
	      Log.d("RECON.JUMP", strLog); */
	      
	          iFilled++; 
	      }
	  }

      // all retrieved at this point
      return iFilled;
   }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}
	
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		this.cacheEvent(event);
	}
}
