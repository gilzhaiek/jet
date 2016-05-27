package com.reconinstruments.jumpdetection;

import android.os.SystemClock;
import android.util.Log;

// this is helper class that does heavy lifting
// JumpAnalyzer can be regarded as state machine that handles sensors, and
// reporting back to the parent. This object is pure algorithm
class JumpEngine
{
	// standard ident tag
    private static final String TAG = "RECON.JUMP.JumpEngine";
  
    private JumpDebugLog mLogger = null;    // logger utility, moved out to make code cleaner
	
	/* Constants and Enumerations */
    
	// Jump Treshholds (miliseconds)
    static final int       MINIMUM_JUMP_AIR_MS = 10;//500;   
    static final int       MAXIMUM_JUMP_AIR_MS = 9000;  
    
    private static final short[]  Pressure_Delimiter = 
    {
		1000, 1130, 1300, 1500, 1730, 2000, 2300,  2650,      // above the Mount Everest
	  	3000, 3350, 3700, 4100, 4500, 5000, 5500,  6000,
	  	6500, 7100, 7800, 8500, 9200, 9700, 10300, 11000
    }; // 24 members

    private static final short[]  AltCoefficient_i = 
    {
   		12256, 10758, 9329, 8085, 7001, 6069, 5360, 4816,
	  	4371,  4020,  3702, 3420, 3158, 2908, 2699, 2523,
	  	2359,  2188,  2033, 1905, 1802, 1720, 1638
    }; // 23 members
	 
    // misc constants; only Li knows what some of them are
    static final float  JUMP_LAND_THRESHOLD_SQ         = 311.6f;   // (1.8g)^2
    static final float  JUMP_LAND_SOFT1_THRESHOLD_SQ    = 96.17f;   // (1g)^2 g=9.8066
    static final float  JUMP_LAND_SOFT2_THRESHOLD_SQ   = 34.62f;   // (0.6g)^2
    
    static final int    NUM_SAMPLES                    = 4;
    static final int    SOFT_LAND_TIME_THRESHOLD       = 500;
    static final float  ACC_THRESHOLD                  = 18f;
    static final long   SYSTEM_OFFSET_TIME_DELAY       = 150;      // Air time offset for system delay
    static final float  ALTITUDE_COEFFICIENT_ADJUSTMENT= 204.8f;   // another Li magic number; pulled out from algorithm when
                                                                   // Jump is being constructed after landing has been detected
    static final float  PRESSURE_DELTA                 = 0.05f;   
    static final float  K_FACTOR                       = 0.5f;	
    
   
    
	// air state enumeration. Once we land, we fire 
	// parent interface indicating whether we had actual jump or not
	static enum AirState
	{
		FREEFALL,
		FLYING,
		LANDED
	}
	  
    static enum DetectState
    {
   		AIRTIME_CONTINUE,
    	AIRTIME_END,
    	AIRTIME_END_CHECK
    }
   
    static enum AccState
    {
    	INDEX_ZERO,
    	INDEX_ONE,
    	START_JUMP_BEGIN_CHECK,
    	GET_JUMP_BEGIN_CHECK,
    	END_JUMP_BEGIN_CHECK,
    }
    
    /* Instance Variables */
    private long  mJumpEndTime   = 0;      // Timestamp when jump ended
    private long  mJumpStartDate = 0;      // FreeFall detection timestamp as reported from sensors
    private float mLastPressure  = 0;      // last unmodified reported pressure
    
    private float mP_Prev        = 0;
    private float mP_sum         = 0;      // sum of n pressure data
    
    private float[] mP_index     = new float[JumpEngine.NUM_SAMPLES];   // first pressure of the sum
    
    private int   mIndex         = 0;
    private int   mIndextoFill   = 0;
    
      /*****
	  Ptemp1 store the highest point (attitude) of the track starting when 
	  freefall happens and ending at the point when the landing condition is detected. Usually, such track is equal or longer(soft landing) than the jump track 
   
    * NOTE: There is LOTS of mess with these variables; need to clean it up when I understand
    *       guts of algorithm
    *****/
    private float mP_avg   = 0;                // calculated during pressure measurement; used for
                                               // landing detection criteria
    private float mPtemp1           = 0;
    
    
    private int   mSoft_land_time   = 0;
    
    private long  mJump_RecheckTime = 0;
    
    private float mP0               = 0;       // jump start pressure
    private float mP1               = 0;       // jump end pressure
    private float mPtemp            = 0;       // store highest point(attitude) of jump track
    
    private long  mLastUpdateTime   = 0;  
    
    
    private JumpEngine.AirState     mAirIndicator   = JumpEngine.AirState.LANDED;  
    public  JumpEngine.AirState AirState() {return mAirIndicator;}
    
    private JumpEngine.DetectState  mJumpDetected   = JumpEngine.DetectState.AIRTIME_CONTINUE;
    public  JumpEngine.DetectState  JumpState () {return mJumpDetected;}

    private JumpEngine.AccState mAcc_check = JumpEngine.AccState.INDEX_ZERO; //work around for sync sensor framework, the second measured data always come late, make it hard to do smooth analysis
    
    /* Methods */
    
    // c-tor
    JumpEngine()
    {
        //  setup logging if we are in debug mode
	    if (JumpAnalyzer.DEBUG_MODE) mLogger = new JumpDebugLog();
    }
    
    // init, called at start
    public void Init (float pressure, long lastUpdateTime, long startDate)
    {
    	mP0 = pressure;
    	mLastUpdateTime = lastUpdateTime;
    	mJumpStartDate = startDate;
    	
    	mAirIndicator  = JumpEngine.AirState.FREEFALL;
        mJumpDetected  = JumpEngine.DetectState.AIRTIME_CONTINUE;
        
        // init logging as well
       if (JumpAnalyzer.DEBUG_MODE) 
    	   mLogger.startLog(startDate, pressure, lastUpdateTime);
    }
    
    // public export to reset internal state when landing is detected
    public void Reset()
    {
    	// close logging
    	if (JumpAnalyzer.DEBUG_MODE)
    		mLogger.endLog();
    	
    	// reset all internal state vars; this is quite a task, considering the mess
    	
    }
    
    // public export to construct Jump Object from internal state variables
    // called by parent when Landed state has been detected
    public ReconJump getJump()
    {
    	ReconJump jump = null;
    	
    	// we need to be in landed state
    	if (mAirIndicator != AirState.LANDED)
    	{
    		Log.d(TAG, "Not in Landed state: Can not construct Jump Object");
    		return jump;
    	}
  
        long diff1 = 600; 
        long diff = 0; 
	  
        // Check air time------------
        if (mAcc_check == AccState.GET_JUMP_BEGIN_CHECK)
        {
        	diff1 = mJumpEndTime - mJump_RecheckTime;
        }
      
        if (JumpAnalyzer.DEBUG_MODE) mLogger.logJumpEndTime(mJump_RecheckTime, mJumpEndTime);
      
        diff = mJumpEndTime - mJumpStartDate;   // jump start already adjusted
      
  /*      String strLog = String.format("Testing Jump Conditions. mJumpStartDate [%d], mJumpEndTime [%d], mJump_RecheckTime [%d]",
        		mJumpStartDate, mJumpEndTime, mJump_RecheckTime);
 
        Log.d(TAG, strLog);*/
        
        // Test to see if the airtime (diff) meets threshhold
        if ( (diff1 > JumpEngine.MINIMUM_JUMP_AIR_MS) && 
        	 (diff  > JumpEngine.MINIMUM_JUMP_AIR_MS) &&
        	 (diff  < JumpEngine.MAXIMUM_JUMP_AIR_MS) )      
        { 
	    
        	// there is jump
            // allocate and populate new jump object
            // NOTE: Distance must come from elsewhere
        	jump = new ReconJump();  
            jump.mDate = mJumpEndTime;
            jump.mAir  = (int)diff + JumpEngine.SYSTEM_OFFSET_TIME_DELAY;  // add offset ait time from the system delay----------------------------
             
            int p = (int)(mPtemp * 10);
            int h, i;
        
            for (i = 22; i > 0; i--)  //22 ,Pressure_Delimiter[i]=10300
            {
            	h = (int)(Pressure_Delimiter[i] );
            	if (p >= h)
                   break;
            }
        
            if (mP1 >= mPtemp)
            {
            	jump.mDrop = (mP1 - mPtemp) * AltCoefficient_i[i] / JumpEngine.ALTITUDE_COEFFICIENT_ADJUSTMENT;
                if (jump.mDrop > 0)
                {
                    //jump.mDrop = jump.mDrop;  // round it (??????)
                    jump.mDrop = Math.round(jump.mDrop);
                }
            }
            else
               jump.mDrop = ReconJump.INVALID_DROP;
          
        
            if (mP0 >= mPtemp)
            {  
            	jump.mHeight = (mP0 - mPtemp) * AltCoefficient_i[i] / JumpEngine.ALTITUDE_COEFFICIENT_ADJUSTMENT;     
            	if (jump.mHeight > 0)
            	{
            		//  jump.mHeight = jump.mHeight;  // round it   (????)
            		jump.mHeight = Math.round(jump.mHeight);  
            	}
            }
            else
            	jump.mHeight = ReconJump.INVALID_HEIGHT;
          
            // finally, do some logging
            if (JumpAnalyzer.DEBUG_MODE) mLogger.logJump(jump, mP0, mP1, mPtemp, mPtemp1);
  
            Log.d(TAG, "Jump Detected!");
        } 	
       
        return jump;
    }
    
    // processes reported pressure measurement
	void processPressure (float pressure)
	{
		long  time_ms = 0; 
		                   
		// save reported pressure
        mLastPressure = pressure;
        
        // reset instance variable; this is used for jump end detection criteria
        // when acc sensor data is processed
        mP_avg   = 0;   
      
        // check pressure----------------------------------
        if (mAirIndicator.equals(AirState.FREEFALL) )
	    {
    	    // get timestamp
            time_ms = SystemClock.elapsedRealtime();     
      
            mP_Prev = pressure;
      
            long ms = time_ms - mLastUpdateTime;
            if (ms > 162)
               mPtemp = mP_Prev + (162 * (mP0 - mP_Prev) / (float)ms);
            else
               mPtemp = (mP0 + mP_Prev) / 2;
      
            mP0 = mPtemp;
            mPtemp1 = mP0;
      
            mP_Prev       = mP0; 
            mP_index[0]   = mP0; 
            mP_sum        = mP0;
            mIndex        = 1;
            mAirIndicator = AirState.FLYING;
            mAcc_check    = AccState.INDEX_ZERO;
      
            Log.d(TAG, "P0:" + mP0 + "\n");
       }
       else 
	   {
            // means we are flying
            // find the highest point in the jump track
            // Pass low pass filter first-------------------- 
            float temp = pressure - mP_Prev;
            temp = Math.abs(temp);
            
            if (temp >= JumpEngine.PRESSURE_DELTA)  
               mP_Prev = mP_Prev * (1 - JumpEngine.K_FACTOR) + JumpEngine.K_FACTOR * pressure;
            else
               mP_Prev = pressure;
            
            // Pass average----------------------
            if (mIndex < JumpEngine.NUM_SAMPLES)
            {
               mP_index[mIndex] = mP_Prev;
               mP_sum = mP_sum + mP_Prev;
      
               mIndex = mIndex+1;
            }
    
            if (mIndex == JumpEngine.NUM_SAMPLES)   //  first time fill the "ring buffer"
            {
               mIndex = mIndex+1;
    
               mP_avg = mP_sum / JumpEngine.NUM_SAMPLES;
               mIndextoFill = 0;
      
               //P0 = P_avg;
               // Ptemp  = P_avg;
               // Ptemp1 = P_avg;
            }
    
            else if (mIndex > JumpEngine.NUM_SAMPLES)
            {
               mP_sum                 = mP_sum - mP_index[mIndextoFill];
               mP_index[mIndextoFill] = mP_Prev;
               mP_sum                 = mP_sum + mP_index[mIndextoFill];
               
               mP_avg                  = mP_sum / JumpEngine.NUM_SAMPLES;
      
               mIndextoFill           = mIndextoFill+1;
      
               if (mIndextoFill >= JumpEngine.NUM_SAMPLES)
                  mIndextoFill = 0;
            }
    
       
            // update Ptemp1
            if ( (mP_avg > 0) && (mP_avg < mPtemp1) )
            {
               mPtemp1 = mP_avg;                     
       
               // update Ptemp
               // if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
               // Ptemp = Ptemp1;    
            }
    
            if (mAcc_check == JumpEngine.AccState.INDEX_ZERO)
                mAcc_check = JumpEngine.AccState.INDEX_ONE;
            
        } // if (mAirIndicator.equals(AirState.FREEFALL) 
        
        String strLog = String.format("Processed Pressure: [%f]", pressure);
        Log.d(TAG, strLog);
	}
	
	// process reported accelerometer measurement
	JumpEngine.AirState processAccelerometer (float [] values)
	{
		float acc_sum = 0;
       
		if ( (values[0] > JumpEngine.ACC_THRESHOLD) ||
             (values[1] > JumpEngine.ACC_THRESHOLD) ||
             (values[2] > JumpEngine.ACC_THRESHOLD) )
        {
    	    // reduce large data calculation-----------
    	    acc_sum = JumpEngine.JUMP_LAND_THRESHOLD_SQ;   
        }
        else  
        {
    	    // just squares them, god knows why
            acc_sum = values[0] * values[0] + values[1] * values[1] + values[2] * values[2];
        }
      
		// log measurements we are processing:
		if (JumpAnalyzer.DEBUG_MODE)
		{
			mLogger.logCheckpoint(
					SystemClock.elapsedRealtime(),
					mLastPressure,
					values, mP_avg);
		}
    
		// 1800mg, hard landing
		if (acc_sum >= JumpEngine.JUMP_LAND_THRESHOLD_SQ)  
        {
			if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
				mJumpDetected = DetectState.AIRTIME_END;          // possible landing point in the jump track    
        
            mAirIndicator = AirState.LANDED;
        }     
    
		// in Java space right now the average time interval is 50ms
        else if (acc_sum >= JumpEngine.JUMP_LAND_SOFT1_THRESHOLD_SQ)
        {
        	// 1000mg, soft landing     
        	mSoft_land_time += 25; //125;        //25;    //> 1g for 200ms (500/25=20points=200ms @100hz)
        	if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
        		mJumpDetected = DetectState.AIRTIME_END;          // possible landing point in the jump track
    
        	if (mAcc_check == AccState.INDEX_ONE)
            {
        		mAcc_check=AccState.START_JUMP_BEGIN_CHECK;
    	    }      
        }  
   
        else if (acc_sum >= JumpEngine.JUMP_LAND_SOFT2_THRESHOLD_SQ) 
        {
        	// 600mg
    	    mSoft_land_time += 10;   //50;//10;   //   > 600mg for 500ms (500/10=50points=500ms @ 100hz)
    	    if (mJumpDetected == DetectState.AIRTIME_CONTINUE)
    		   mJumpDetected = DetectState.AIRTIME_END;          // possible landing point in the jump track       
        }
    
        else
        {
    	    mJumpDetected = DetectState.AIRTIME_CONTINUE;
    	    mSoft_land_time = 0; 
    	
    	    if (mAcc_check == AccState.START_JUMP_BEGIN_CHECK)
    	    {
    		    mAcc_check = AccState.GET_JUMP_BEGIN_CHECK;
    		
    		    // add second jump begin timestamp here-----
    		    mJump_RecheckTime = SystemClock.elapsedRealtime();
    	    }
        }
    
		
		JumpEngine.AirState state = landed_criteria();
		       
		String strLog = String.format("Processed Accelerometer: X [%f], Y [%f], Z [%f]", 
				values[0], values[1], values[2] );
        Log.d(TAG, strLog);
        
		return state;
	}
      
   
	// internal helper to determine end of jump
	private AirState landed_criteria ()
	{
		if (mAcc_check == AccState.INDEX_ONE)
		{
			mAcc_check = AccState.END_JUMP_BEGIN_CHECK;
	    }        
    
		// check if landing found-----------------
		if (mJumpDetected == DetectState.AIRTIME_END)
		{
			mJumpEndTime  = SystemClock.elapsedRealtime();//new Date();
			mP1 = mP_avg; 
      
			// update Ptemp
			mPtemp = mPtemp1;
            mJumpDetected = DetectState.AIRTIME_END_CHECK;
        }
       
		if (mSoft_land_time >= JumpEngine.SOFT_LAND_TIME_THRESHOLD)
	    {
			//  detect landing
            mAirIndicator = AirState.LANDED; 
        }  
		
		return mAirIndicator;
	}
	    

	/** This appears not used in current algorithm, so I commented it out
	 * 
	 * 
	private static final short[]  AltCoefficient_j = 
    {
   		16212, 15434, 14541, 13630, 12722, 11799, 10910, 9994,
	 	9171,  8424,  7737,  7014,  6346,  5575,  4865,  4206,
	 	3590,  2899,  2151,  1456,  805,   365,   -139
    }; // 23 members 0.1mbar
   
   
    // helper to calculate altitude from sensor barometric pressure [hPa]
    private  int calculate_altitude(float pressure) 
    {
       int h;
       int altitude_decimal_bits;
       char i;
       int p = (int)(pressure*100);
         
       for (i = 22; i > 0; i--)  //22 ,Pressure_Delimiter[i]=10300
       {
          h = (int)(JumpEngine.Pressure_Delimiter[i] * 10);
          if (p >= h)
            break;
       }
         
       h = (int)(JumpEngine.AltCoefficient_j[i] * 10);
       altitude_decimal_bits = (p - (int)JumpEngine.Pressure_Delimiter[i] * 10) * (int)JumpEngine.AltCoefficient_i[i];
       h = h - (altitude_decimal_bits >> 11);
        
       return h;   // 0.1m
     }
     */

}
