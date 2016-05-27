package com.reconinstruments.wahoo_demo;

import java.util.Date;

import android.util.Log;

public class PowerMeterDecoder {

	private static final String TAG = "PowerMeterDecoder";
	
	static float MAX_CRANK_TICKS_PER_SECOND = 10.0f;  // 10 ticks per second is 600 RPM for crank.
	static float MAX_WHEEL_TICKS_PER_SECOND = 50.0f;  // 50 ticks per second is roughly 231 mph.

	static int CPM_FLAG_OFFSET                            = 0;
	static int CPM_FLAG_SIZE                              = 2;
	static boolean CPM_FLAG_LEFT_PEDAL_CONTRIB(int x) 		{return (x & 0x0001)!=0;}
	static boolean CPM_FLAG_ACCUM_TORQUE(int x) 			{return (x & 0x0002)!=0;}
	static boolean CPM_FLAG_SPEED_DATA(int x) 				{return (x & 0x0004)!=0;}
	static boolean CPM_FLAG_CADENCE_DATA(int x) 			{return (x & 0x0008)!=0;}
	static boolean CPM_FLAG_EXTREME_FORCES(int x) 			{return (x & 0x0010)!=0;}
	static boolean CPM_FLAG_ANGLES_OF_EXTREME_FORCES(int x) {return (x & 0x0020)!=0;}
	static boolean CPM_FLAG_TOP_DEAD_SPOT_ANGLE(int x) 		{return (x & 0x0040)!=0;}
	static boolean CPM_FLAG_BOTTOM_DEAD_SPOT_ANGLE(int x) 	{return (x & 0x0080)!=0;}

	static int CPM_INST_POWER_OFFSET                      = (CPM_FLAG_OFFSET+CPM_FLAG_SIZE);
	static int CPM_INST_POWER_SIZE                        = 2;
	static int CPM_WHEEL_REVS_SIZE                        = 4;
	static int CPM_WHEEL_TIME_SIZE                        = 2;
	static int CPM_CRANK_REVS_SIZE                        = 2;
	static int CPM_CRANK_TIME_SIZE                        = 2;
	
	boolean dataInitialized; 
	boolean decoederReinitialized;
    
	boolean hasLeftPedalPower;
	boolean hasAccumTorque;
	boolean hasSpeedData;
	boolean hasCadenceData;

	boolean hasData;
	
	int ssInstPower;
	int ucLeftPedalPower;

    long ulAccumTorque;
    long usPreviousAccumTorque;
    
    Date lastDataTime;
    
	class PageData {
		long usCumSpeedRevCount;
		int usLastTime1024;
		int usCumCadenceRevCount;
		int usLastCadence1024;
		PageData(){}
		PageData(PageData data){
			usCumSpeedRevCount = data.usCumSpeedRevCount;
			usLastTime1024 = data.usLastTime1024;
			usCumCadenceRevCount = data.usCumCadenceRevCount;
			usLastCadence1024 = data.usLastCadence1024;
		}
	}
	class CalculatedData {
		int accumWheelRevolutions;
		int accumSpeedTime;
		int accumPower;
		int instantWheelRPM;
		int accumCrankRevolutions;
		int accumCadenceTime;
		int instantCrankRPM;
	}
	PageData pageData;
	PageData prevPageData;
	CalculatedData calculatedData;
	
	public PowerMeterDecoder(){
		calculatedData = new CalculatedData();
		calculatedData.instantWheelRPM = -1;
		pageData = new PageData();
		prevPageData = new PageData();
		
		dataInitialized = false;
	}

	public int getInstantaneousPower(){
		if(ssInstPower==0) return -1;//prints as --
		return ssInstPower;
	}
	static int wheelDiameterInches = 27;
	public float getSpeed(){
		
		if(calculatedData.instantWheelRPM==-1)
			return -1;
		
		float wheelCirc = ((float)wheelDiameterInches*(float)Math.PI);
		//Log.d(TAG, "wheel circumference in in "+wheelCirc);
		
		float wheelCircKm = wheelCirc*0.0254f/1000;
		//Log.d(TAG, "wheel circumference in km "+wheelCircKm);
		
		float wheelKmh = wheelCircKm*calculatedData.instantWheelRPM*60;
		
		return wheelKmh;
	}
	public void decodeData(byte[] data) {
		// 12 bytes
		Log.d(TAG,"data: "+bytArrayToHex(data));
		if(data.length<2) return;
		int offset = 0;
		int flags = data[offset++] | (data[offset++] << 8);
		hasLeftPedalPower = CPM_FLAG_LEFT_PEDAL_CONTRIB(flags);
        hasAccumTorque = CPM_FLAG_ACCUM_TORQUE(flags);
        hasSpeedData = CPM_FLAG_SPEED_DATA(flags);
        hasCadenceData = CPM_FLAG_CADENCE_DATA(flags);
        
        Log.d(TAG,
        		(hasLeftPedalPower?"hasLeftPedalPower ":"")+
        		(hasAccumTorque?"hasAccumTorque ":"")+
        		(hasSpeedData?"hasSpeedData ":"")+
        		(hasCadenceData?"hasCadenceData ":""));
        
        ssInstPower = (int)((data[offset++]&0xFF) | ((data[offset++]&0xFF)<<8));
    	Log.d(TAG,"ssInstPower: "+ssInstPower);
        
        if (hasLeftPedalPower)//left pedal power
        	offset+=1;
        
        long accumTorque=0;
        if (hasAccumTorque){//accumulated torque
        	accumTorque = ((data[offset++]&0xFF) | ((data[offset++]&0xFF)<<8));
            Log.d(TAG,"accumTorque: "+accumTorque);
        }
        prevPageData = new PageData(pageData);//clone
        if (hasSpeedData){//cumulative wheel revs, last wheel event time
        	pageData.usCumSpeedRevCount = ((data[offset++]&0xFF)|((data[offset++]&0xFF)<<8)|((data[offset++]&0xFF)<<16)|((data[offset++]&0xFF)<<24));
	    	Log.d(TAG, "ulWheelRevs: "+pageData.usCumSpeedRevCount);
            
	    	pageData.usLastTime1024  = ((data[offset++]&0xFF) | ((data[offset++]&0xFF)<<8));
            Log.d(TAG, "usLastTime1024: "+pageData.usLastTime1024);
        }
        if (hasCadenceData)//cumulative crank rev count,last crank event time
            offset+=4;
        
        if(dataInitialized){
        	if (pageData.usLastTime1024 != prevPageData.usLastTime1024)
			{
        		long wheelRevs = ((pageData.usCumSpeedRevCount - prevPageData.usCumSpeedRevCount) & 0xFFFF);
                Log.d(TAG, "wheelRevs: "+wheelRevs);
				int timeOffset = ((pageData.usLastTime1024 - prevPageData.usLastTime1024) & 0xFFFF);
	            Log.d(TAG, "timeOffset: "+timeOffset);
		
				float ticksPerSecond = (float)wheelRevs / ((float)timeOffset/2048.0f);
				Log.d(TAG, "ticksPerSecond: "+ticksPerSecond);
				if ( ticksPerSecond < MAX_WHEEL_TICKS_PER_SECOND )
				{
					calculatedData.accumWheelRevolutions += wheelRevs;
					calculatedData.accumSpeedTime += timeOffset;
                    calculatedData.accumPower += (ssInstPower * wheelRevs);
					calculatedData.instantWheelRPM = (int)( ((long)wheelRevs * 0x1E000) / (long)timeOffset);
		            Log.d(TAG, "accumWheelRevolutions: "+calculatedData.accumWheelRevolutions);
		            Log.d(TAG, "accumSpeedTime: "+calculatedData.accumSpeedTime);
		            Log.d(TAG, "accumPower: "+calculatedData.accumPower);
		            Log.d(TAG, "instantWheelRPM: "+calculatedData.instantWheelRPM);
		            lastDataTime = new Date();
				}
			} else {
				if(lastDataTime==null)
					calculatedData.instantWheelRPM = -1;
				else{
					long timeElapsed = new Date().getTime()-lastDataTime.getTime();
					if(timeElapsed>5000)
						calculatedData.instantWheelRPM = -1;
				}
			}
        }
        
        dataInitialized = true;
	}
	public static String bytArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for(byte b: a)
			sb.append(String.format("%02x", b&0xff));
		return sb.toString();
	}
}
