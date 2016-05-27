package com.reconinstruments.wahoo_demo;

import java.util.Date;

import android.util.Log;

public class SpeedCadenceDecoder {

	private static final String TAG = "SpeedCadenceDecoder";
	
	static float MAX_CRANK_TICKS_PER_SECOND = 10.0f;  // 10 ticks per second is 600 RPM for crank.
	static float MAX_WHEEL_TICKS_PER_SECOND = 50.0f;  // 50 ticks per second is roughly 231 mph.

	static boolean CPM_FLAG_SPEED_DATA(int x) 				{return (x & 0x01)!=0;}
	static boolean CPM_FLAG_CADENCE_DATA(int x) 			{return (x & 0x02)!=0;}

	boolean dataInitialized; 
	boolean hasSpeedData;
	boolean hasCadenceData;
	
	long ulTotalWheelRevs;

    Date lastWheelTime;
    Date lastCrankTime;
    
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
		int instantWheelRPM;
		int accumCrankRevolutions;
		int accumCadenceTime;
		int instantCrankRPM;
	}
	PageData pageData;
	PageData prevPageData;
	CalculatedData calculatedData;
	
	public SpeedCadenceDecoder(){
		calculatedData = new CalculatedData();
		calculatedData.instantCrankRPM = -1;
		calculatedData.instantWheelRPM = -1;
		pageData = new PageData();
		prevPageData = new PageData();
		
		dataInitialized = false;
	}

	public int getCadence(){

		return calculatedData.instantCrankRPM;
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
	//data: 03410d0000e5b75e02e022 11 bytes
	//data: 03c21a0000cb396604de52

	public void decodeData(byte[] data) {
		// 12 bytes
		Log.d(TAG,"data: "+bytArrayToHex(data));
		int offset = 0;
		int flags = data[offset++];
        hasSpeedData = CPM_FLAG_SPEED_DATA(flags);
        hasCadenceData = CPM_FLAG_CADENCE_DATA(flags);
        Log.d(TAG,
        		(hasSpeedData?"hasSpeedData ":"")+
        		(hasCadenceData?"hasCadenceData ":""));
        
        prevPageData = new PageData(pageData);//clone
        
        // process the speed data.
        if (hasSpeedData)
        {
            // decode the wheel revs.
        	pageData.usCumSpeedRevCount = ((data[offset++]&0xFF)|((data[offset++]&0xFF)<<8)|((data[offset++]&0xFF)<<16)|((data[offset++]&0xFF)<<24));
            ulTotalWheelRevs = pageData.usCumSpeedRevCount;
	    	Log.d(TAG, "usCumSpeedRevCount: "+pageData.usCumSpeedRevCount);
			
            // decode the wheel event time.
	    	pageData.usLastTime1024  = ((data[offset++]&0xFF) | ((data[offset++]&0xFF)<<8));
            Log.d(TAG, "lastWheelTime: "+pageData.usLastTime1024);
        }
        // process the cadence data.
        if (hasCadenceData)
        {
            // decode the crank revs.
        	pageData.usCumCadenceRevCount = ((data[offset++]&0xFF) | ((data[offset++]&0xFF)<<8));
	    	Log.d(TAG, "usCumCadenceRevCount: "+pageData.usCumCadenceRevCount);
            // decode the crank event time.
	    	pageData.usLastCadence1024  = ((data[offset++]&0xFF) | ((data[offset++]&0xFF)<<8));
	    	Log.d(TAG, "lastCrankTime: "+pageData.usLastCadence1024);
        }
        
        if(dataInitialized){
        	if (pageData.usLastTime1024 != prevPageData.usLastTime1024)
			{
        		long wheelRevs = ((pageData.usCumSpeedRevCount - prevPageData.usCumSpeedRevCount) & 0xFFFF);
                Log.d(TAG, "wheelRevs: "+wheelRevs);
				int timeOffset = ((pageData.usLastTime1024 - prevPageData.usLastTime1024) & 0xFFFF);
	            Log.d(TAG, "timeOffset: "+timeOffset);
		
				float ticksPerSecond = (float)wheelRevs / ((float)timeOffset/1024.0f);
				Log.d(TAG, "ticksPerSecond: "+ticksPerSecond);
				if ( ticksPerSecond < MAX_WHEEL_TICKS_PER_SECOND )
				{
					calculatedData.accumWheelRevolutions += wheelRevs;
					calculatedData.accumSpeedTime += timeOffset;
					// calculate Wheel cadence in RPM.
					calculatedData.instantWheelRPM = (int)( ((long)wheelRevs * 0xF000) / (long)timeOffset);
					Log.d(TAG, "accumWheelRevolutions: "+calculatedData.accumWheelRevolutions);
					Log.d(TAG, "accumSpeedTime: "+calculatedData.accumSpeedTime);
					Log.d(TAG, "instantWheelRPM: "+calculatedData.instantWheelRPM);
					lastWheelTime = new Date();
				}
			} else {
				if(lastWheelTime==null)
					calculatedData.instantWheelRPM = -1;
				else{
					long timeElapsed = new Date().getTime()-lastWheelTime.getTime();
					if(timeElapsed>5000)
						calculatedData.instantWheelRPM = -1;
				}
			}
        	if (pageData.usLastCadence1024 != prevPageData.usLastCadence1024)
			{
        		long pedalRevs = ((pageData.usCumCadenceRevCount - prevPageData.usCumCadenceRevCount) & 0xFFFF);
                Log.d(TAG, "wheelRevs: "+pedalRevs);
				int timeOffset = ((pageData.usLastCadence1024 - prevPageData.usLastCadence1024) & 0xFFFF);
	            Log.d(TAG, "timeOffset: "+timeOffset);
		
				float ticksPerSecond = (float)pedalRevs / ((float)timeOffset/1024.0f);
				Log.d(TAG, "ticksPerSecond: "+ticksPerSecond);
				if ( ticksPerSecond < MAX_CRANK_TICKS_PER_SECOND )
				{
					calculatedData.accumCrankRevolutions += pedalRevs;
					calculatedData.accumCadenceTime += timeOffset;
					// calculate Wheel cadence in RPM.
					calculatedData.instantCrankRPM = (int)( ((long)pedalRevs * 0xF000) / (long)timeOffset);
					Log.d(TAG, "accumCrankRevolutions: "+calculatedData.accumCrankRevolutions);
					Log.d(TAG, "accumCadenceTime: "+calculatedData.accumCadenceTime);
					Log.d(TAG, "instantCrankRPM: "+calculatedData.instantCrankRPM);
					lastCrankTime = new Date();
				}
			} else {
				if(lastCrankTime==null)
					calculatedData.instantCrankRPM = -1;
				else{
					long timeElapsed = new Date().getTime()-lastCrankTime.getTime();
					if(timeElapsed>5000)
						calculatedData.instantCrankRPM = -1;
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
