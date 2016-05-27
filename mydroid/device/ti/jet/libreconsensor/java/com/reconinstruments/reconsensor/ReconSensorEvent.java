package com.reconinstruments.reconsensor;

import android.hardware.SensorEvent;

/* Android SensorEvent is not directly instantiable (java is full of crap, indeed)
 * So in order to cache we have to have our own */
public class ReconSensorEvent extends Object implements Cloneable
{
   public long timestamp;
   public float [] values = new float[3];
   public int  type;
   
   // this copies google android event
   public static void copyEvent (SensorEvent source, ReconSensorEvent target)
   {
	   target.timestamp = source.timestamp;
	   target.type = source.sensor.getType();
	   
	   for (int i = 0; i < 3; i++)
	   {
		   target.values[i] = source.values[i];
	   }
   }
   
   // this clones ourselves
   @Override
   protected Object clone()
   {
	   try
	   {
		   return super.clone();
	   }
	   catch (CloneNotSupportedException e)
	   {
		   return null;
	   }
   }
   
}
