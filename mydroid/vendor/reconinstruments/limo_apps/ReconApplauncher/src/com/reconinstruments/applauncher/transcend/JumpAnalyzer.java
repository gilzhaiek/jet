package com.reconinstruments.applauncher.transcend;

import java.util.Date;

import com.reconinstruments.reconsensor.ReconSensor;
import android.content.Context;
import android.os.Environment;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
public class JumpAnalyzer implements SensorEventListener {
  // standard ident tag
  private static final String TAG = JumpAnalyzer.class.getSimpleName();
  // Jump Treshholds
  public int mMinimumJumpAir = 500; // miliseconsd
  public final int mMaximumJumpAir = 9000;// miliseconds

  // singleton pattern
  static  JumpAnalyzer  m_instance = null; 
  private JumpAnalyzer () {}
  public static boolean DEBUG_MODE = false;//
  private static final String Jump_LOG_FILE_NAME = "jump.txt";//jump log
	private File f;
	private FileWriter os;
  public static JumpAnalyzer Instance(SensorManager sm) 
  {
    if (m_instance == null) {
      String strLog;
          
      m_instance = new JumpAnalyzer ();
      m_instance.mSensorManager = sm;
      
      m_instance.mSyncSensor  = m_instance.mSensorManager.getDefaultSensor
      (ReconSensor.EventType.RI_SENSOR_TYPE_SYNCHRONIZED);
      
      if (m_instance.mSyncSensor == null) {
        strLog = "RECON Synchronized Sensor not Available\n";
        m_instance = null;
      }
      else {
        strLog = String.format("RECON Synchronized Sensor: Name [%s] Vendor [%s] Version [%d] Range [%f] Resolution [%f] Power [%f] Min Delay [%d]",
               m_instance.mSyncSensor.getName(), m_instance.mSyncSensor.getVendor(), m_instance.mSyncSensor.getVersion(),
               m_instance.mSyncSensor.getMaximumRange(), m_instance.mSyncSensor.getResolution(),
               m_instance.mSyncSensor.getPower(), m_instance.mSyncSensor.getMinDelay() );
      }
            
      Log.i(TAG, strLog);
    }

    return m_instance;
  }
	
  private SensorManager mSensorManager;    // Android SensorManager
  private Sensor        mSyncSensor;       // RECON Synchronized Sensor Event	
  private JumpEndEvent  mIJumpEnd;         // jump end callback
  private long mjumpEnd;
  // this method is required to be implemented by framework but it is bull so leave it empty
  public void onAccuracyChanged(Sensor sensor, int accuracy) 
  {
  }

    // key Jump Detection algorithm.
  public void onSensorChanged(SensorEvent event)
  {
    //Log.d(TAG, "Synchronized Event received\n");
       
    if (mAirIndicator.equals (AirState.LANDED) )
      return;
    float P_avg=0;   
    long time_ms=0;  
    // check pressure----------------------------------
    if (mAirIndicator.equals(AirState.FREEFALL) ) {
      time_ms=android.os.SystemClock.elapsedRealtime();     
      P_Prev = event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_PRESSURE];
      long ms=time_ms-mLastUpdateTime;
      if(ms>162)
        Ptemp=P_Prev+(162*(P0-P_Prev)/(float)ms);
      else
        Ptemp=(P0+P_Prev)/2;
      
      P0=Ptemp;
      Ptemp1 = P0;
      P_Prev = P0; 
      P_index[0] =P0; 
      P_sum=P0;
      mIndex=1;
      mAirIndicator = AirState.FLYING;
      mAcc_check=AccState.INDEX_ZERO;
      Log.d(TAG,"P0:" + P0+"\n");
    }
    else {
      // means we are flying
      // find the highest point in the jump track
      // Pass low pass filter first-------------------- 
      float temp=event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_PRESSURE]-P_Prev;
      temp=Math.abs(temp);
      if(temp>=pressure_delta)  
      {
        P_Prev = P_Prev * (1 - k_factor) + k_factor*
        event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_PRESSURE];
      }    
      else
        P_Prev=event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_PRESSURE];
    // Pass average----------------------
    
    if(mIndex<avg_num)
    {
      P_index[mIndex]=P_Prev;
      P_sum=P_sum+P_Prev;
      mIndex=mIndex+1;
    }
    
    if (mIndex==avg_num)//first time fill the "ring buffer"
    {
      mIndex=mIndex+1;
    
      P_avg=P_sum/avg_num;
      mIndextoFill=0;
      
      //P0 = P_avg;
      // Ptemp  = P_avg;
      // Ptemp1 = P_avg;
    }
    else if(mIndex>avg_num)
    {
      P_sum=P_sum-P_index[mIndextoFill];
      P_index[mIndextoFill]=P_Prev;
      P_sum=P_sum+P_index[mIndextoFill];
      P_avg=P_sum/avg_num;
      
      mIndextoFill=mIndextoFill+1;
      if(mIndextoFill>=avg_num)
        mIndextoFill=0;
    }
    
       // update Ptemp1
    if((P_avg>0) && (P_avg < Ptemp1))
    {
      Ptemp1 = P_avg;                     
      // update Ptemp
      // if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
        // Ptemp = Ptemp1;    
    }
    if(mAcc_check==AccState.INDEX_ZERO)
      mAcc_check=AccState.INDEX_ONE;
  }// if (mAirIndicator.equals(AirState.FREEFALL)
       
         
    //check acc---------------------------------
    float acc_sum;
    if(event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_X]>18 ||
        event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]>18 ||
        event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Z]>18
    )
      acc_sum=jump_land_threshold_sq;//reduce large data calculation-----------
    else  
      acc_sum= event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_X]*event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_X]
      + event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]*event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]
      + event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Z]*event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Z];
      
    if(DEBUG_MODE)
   {       
      //Date measure_data=new Date();//measure_data.getTime() 
      long measure_data;
     if (mAirIndicator.equals(AirState.FREEFALL) )  
        measure_data=time_ms;	
      else
        measure_data=android.os.SystemClock.elapsedRealtime();
      try {
        os.write(measure_data+","
        +event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_PRESSURE]+","
        +event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_X]+","
        +event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Y]+","
        +event.values[ReconSensor.SynchronizedEvent.RI_SYNCHRONIZED_OFFSET_ACCEL_Z]+
        ","+P_avg+
        "\r\n");//","+P_sum+
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
     
    if (acc_sum >= jump_land_threshold_sq) {// 1800mg, hard landing

      if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
        mJumpDetected = DetectState.AIRTIME_END;          // possible landing point in the jump track    
        
      mAirIndicator = AirState.LANDED;
    }     
    //in Java space right now the average time interval is 50ms
    else if (acc_sum >= jump_land_soft1_threshold_sq) {// 1000mg, soft landing     
      soft_land_time += 25; //125;        //25;    //> 1g for 200ms (500/25=20points=200ms @100hz)
      if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
        mJumpDetected = DetectState.AIRTIME_END;          // possible landing point in the jump track
          
      if(mAcc_check==AccState.INDEX_ONE)
      {
        mAcc_check=AccState.START_JUMP_BEGIN_CHECK;
      }      
    }     
    else if (acc_sum >= jump_land_soft2_threshold_sq) {// 600mg
      soft_land_time+=10;//50;//10;   //   > 600mg for 500ms (500/10=50points=500ms @ 100hz)
      if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
        mJumpDetected = DetectState.AIRTIME_END;          // possible landing point in the jump track       
    }
    else {
      mJumpDetected = DetectState.AIRTIME_CONTINUE;
      soft_land_time = 0; 
      
      if(mAcc_check==AccState.START_JUMP_BEGIN_CHECK)
      {
        mAcc_check=AccState.GET_JUMP_BEGIN_CHECK;
        //add second jump begin timestamp here-----
        mJump_RecheckTime=android.os.SystemClock.elapsedRealtime();
      }       
    }
    /**************************************************************/
    if(mAcc_check==AccState.INDEX_ONE)
    {
      mAcc_check=AccState.END_JUMP_BEGIN_CHECK;
    }        
    // check if landing founded-----------------
    if (mJumpDetected == DetectState.AIRTIME_END){
      mjumpEnd=android.os.SystemClock.elapsedRealtime();//new Date();
      P1 = P_avg; 
      // update Ptemp
      Ptemp = Ptemp1;
      mJumpDetected =DetectState.AIRTIME_END_CHECK;
    }
       
    if (soft_land_time >= 500) {
        mAirIndicator = AirState.LANDED;  }  //  detect landing
       
    // jump landing detect end---------------
    if (mAirIndicator.equals(AirState.LANDED) ) {
      // Jump Object
      ReconJump jump=null;
      long diff1=600; 
      long diff; 
      // Check air time------------
      if(mAcc_check==AccState.GET_JUMP_BEGIN_CHECK)
      {
        diff1=mjumpEnd-mJump_RecheckTime;
        if(DEBUG_MODE)
        {
           try {
            os.write(mJump_RecheckTime+",");              
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
        }
      }             
      if(DEBUG_MODE)
      {
         try {
          os.write(mjumpEnd+"\r\n");
             
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
      }
      diff = mjumpEnd- mJumpStartDate;   // jump start already adjusted
      // Test to see if the airtime (diff) meetes treshhold
      if (diff1>mMinimumJumpAir&&diff > mMinimumJumpAir && diff < mMaximumJumpAir )  
      { // there is jump
        // allocate and populate new jump object
        // NOTE: Distance must come from elsewhere
        jump = new ReconJump();  
        jump.mDate = mjumpEnd;//.getTime();
        jump.mAir  = (int)diff+150;//add offset ait time from the system delay----------------------------
             
        // calculate drop, height---------------------------------
        /*****************************************
          int h0, htemp , h1;
          h0    = calculate_altitude (P0);        // 0.1 m
          htemp = calculate_altitude (Ptemp1);     // 0.1 m         
          h1    = calculate_altitude(P1);         // 0.1 m

          //Log.d(TAG,"h0="+h0+" htemp="+htemp+" h1="+h1+" P0="+P0 + " P1="+P1+"ptemp ="+Ptemp);

          // Convert to meters
          jump.mDrop   = (float)(htemp - h1)/10.0f;
          jump.mHeight = (float)(htemp - h0)/10.0f;

          if (jump.mDrop < 0) {
              jump.mDrop = ReconJump.INVALID_DROP;
          }
          if (jump.mHeight < 0) {
              jump.mHeight = ReconJump.INVALID_HEIGHT;
          }   
          *************************************************************************************/
        int p = (int)(Ptemp*10);
        int h,i;
        for (i = 22; i > 0; i--) //22 ,Pressure_Delimiter[i]=10300
        {
          h = (int)(mPressure_Delimiter[i] );
          if (p >= h)
            break;
        }
        if(P1>=Ptemp)
        {
          // p=(int)((P1-Ptemp)*100);
          // h=p*(int)mAltCoefficient_i[i];
          // //jump.mDrop   =  (float)(h>>11)/10.0f; //m
          // jump.mDrop =(float)(h)/2048;//2^11
          // jump.mDrop =jump.mDrop/10;  
          jump.mDrop=(P1-Ptemp)*mAltCoefficient_i[i]/204.8f;
          if(jump.mDrop>0)
            jump.mDrop=jump.mDrop;//round it
        }
        else
          jump.mDrop = ReconJump.INVALID_DROP;
          
        if(P0>=Ptemp)
        {
          // p=(int)((P0-Ptemp)*100);
          // h=p*(int)mAltCoefficient_i[i];         
          // //jump.mHeight   =  (float)(h>>11)/10.0f; //m
          // jump.mHeight =(float)(h)/2048;//2^11
          // jump.mHeight =jump.mHeight/10;             
          jump.mHeight =(P0-Ptemp)*mAltCoefficient_i[i]/204.8f;     
          if(jump.mHeight>0)
            jump.mHeight=jump.mHeight;//round it    
        }
        else
          jump.mHeight = ReconJump.INVALID_HEIGHT;
          
        
        /** We don't concern ourselves with bundle here; manager
         *  will do it prior to inserting in container / broadcasting */
        if(DEBUG_MODE)
        {
           try {
            os.write(jump.mDrop+","
            +jump.mHeight+","
            +jump.mAir+","
            +P0+","+P1+","+Ptemp+","+Ptemp1+",E\r\n");
             
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        Log.d(TAG,"Jump Detect\n");
      }
      if(DEBUG_MODE)
      {
        try {
          os.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }  
      // here we have detected landing, so unregister
      // synchronized listener and fire back landing event (regardless if we had jump or not)
      mSensorManager.unregisterListener(this, mSyncSensor);
      mIJumpEnd.landed(jump);   
    }	 
  }
	
    // public export triggered by Parent -- MOD Jump Manager --
    // when FreeFall IRQ is detected. Here we (re)start synchronized Listener
    // and will call Parent back once Jump Finishes
  public void Start(long date, JumpEndEvent cbk, ReconTranscendService rts) 
  {
    // remember callback we'll fire once we detect landing
    mIJumpEnd = cbk;
    P0 = rts.getReconAltManager().getPressure();
    mLastUpdateTime = rts.mTimeMan.mLastUpdate;     
    // reset state variables
    mAirIndicator  = AirState.FREEFALL;
    mJumpDetected  = DetectState.AIRTIME_CONTINUE;
    mJumpStartDate = date;

    // register synchronized event listener
    mSensorManager.registerListener((SensorEventListener) 
    this,	
    mSyncSensor, 
    0);//SensorManager.SENSOR_DELAY_FASTEST
    if(DEBUG_MODE)
    {	 
	f= new File(Environment.getExternalStorageDirectory(),Jump_LOG_FILE_NAME);
      try {
        os = new FileWriter(f,true);
        Date t=new Date();
        os.write(mJumpStartDate+","
        +P0+","+mLastUpdateTime+","
        +t.getHours()+","+t.getMinutes()+","+t.getSeconds()+",B\r\n");//+k_factor+",minimumJumpAir:"+mMinimumJumpAir
        // +mJumpStartDate.getHours()+","
        // +mJumpStartDate.getMinutes()+","
        // +mJumpStartDate.getSeconds()
      } 
      catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
  }
	
  public void Stop() {
      mSensorManager.unregisterListener(this, mSyncSensor);
    }
	
    // // helper to calculate altitude from sensor barometric pressure [hPa]
    // // TODO: Li needs to insert comment explaining how this actually works
  public static  int calculate_altitude(float pressure)  {
      int h;
      int altitude_decimal_bits;
      char i;
      int p = (int)(pressure*100);
         
      for (i = 22; i > 0; i--) //22 ,Pressure_Delimiter[i]=10300
          {
        h = (int)(mPressure_Delimiter[i] * 10);
        if (p >= h)
            break;
          }
         
      h = (int)(mAltCoefficient_j[i] * 10);
      altitude_decimal_bits = (p - (int)mPressure_Delimiter[i] * 10) * (int)mAltCoefficient_i[i];
      h = h - (altitude_decimal_bits >> 11);
        
      return h;//0.1m
  }
	 
  private static final short[]  mPressure_Delimiter = {
	1000, 1130, 1300, 1500, 1730, 2000, 2300,  2650,      //above the Mount Everest
	3000, 3350, 3700, 4100, 4500, 5000, 5500,  6000,
	6500, 7100, 7800, 8500, 9200, 9700, 10300, 11000
    }; // 24 members

    private static final short[]  mAltCoefficient_i = {
	12256, 10758, 9329, 8085, 7001, 6069, 5360, 4816,
	4371,  4020,  3702, 3420, 3158, 2908, 2699, 2523,
	2359,  2188,  2033, 1905, 1802, 1720, 1638
    }; // 23 members
	 
    private static final short[]  mAltCoefficient_j =  {
	16212, 15434, 14541, 13630, 12722, 11799, 10910, 9994,
	9171,  8424,  7737,  7014,  6346,  5575,  4865,  4206,
	3590,  2899,  2151,  1456,  805,   365,   -139
    }; // 23 members 0.1mbar
	
    // air state enumeration. Once we land, we fire 
    // parent interface indicating whether we had actual jump or not
    private enum AirState {
    FREEFALL,
    FLYING,
    LANDED }
	  private enum DetectState {
    AIRTIME_CONTINUE,
    AIRTIME_END,
    AIRTIME_END_CHECK }
    private enum AccState {
    INDEX_ZERO,
    INDEX_ONE,
    START_JUMP_BEGIN_CHECK,
    GET_JUMP_BEGIN_CHECK,
    END_JUMP_BEGIN_CHECK,}
    static final float jump_land_threshold_sq       = 311.6f;   // (1.8g)^2
    static final float jump_land_soft1_threshold_sq = 96.17f;   // (1g)^2 g=9.8066
    static final float jump_land_soft2_threshold_sq = 34.62f;   // (0.6g)^2
    static final int avg_num=4;
    static final float pressure_delta=0.05f;   
    public static float k_factor                     = 0.5f;
	 
    private AirState mAirIndicator   = AirState.LANDED;  
    private DetectState  mJumpDetected   = DetectState.AIRTIME_CONTINUE;
    private long     mJumpStartDate ;
    private long mLastUpdateTime;
    private long mJump_RecheckTime;
    //private static final String TAG = "JumpAnalyzer";
    private float P_Prev = 0;
    private float Ptemp  = 0;   //store highest point(attitude) of jump track
    private float P_sum  = 0;   // sum of n pressure data
    private float[] P_index= new float[avg_num];   // first pressure of the sum
    private int mIndex=0;
    private int mIndextoFill=0;
    private AccState mAcc_check=AccState.INDEX_ZERO; //work around for sync sensor framework, the second measured data always come late, make it hard to do smooth analysis
    /*****
	  Ptemp1 store the highest point (attitude) of the track starting when 
	  freefall happens and ending at the point when the landing condition is detected. Usually, such track is equal or longer(soft landing) than the jump track 
    *****/
    private float Ptemp1         = 0;
    private float P0             = 0;
    private float P1             = 0;

    private int   soft_land_time = 0;
}

