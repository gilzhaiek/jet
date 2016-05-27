package com.reconinstruments.reconsensor;

import com.reconinstruments.reconsensor.ReconSensorManager.ReportingMode;

/* Simple data struct for Sensor status */
public class SensorStatus
{
   boolean       mEnabled;
   int           mDelay;
   int           mMinDelay;   // This is redundant as android.sensor also has it, but it is in microsecond 
                              // instead of milisecond as all rates should be
   int           mMode;
   
   public boolean        Enabled()       {return mEnabled;}
   public int            CurrentRate()   {return mDelay;}
   public int            FastestRate()   {return mMinDelay;}
   public ReportingMode  Mode()          {return ReportingMode.from(mMode); }
}
    