package com.reconinstruments.wahoo_demo;

import java.util.Date;

import android.util.Log;

public class HeartRateDecoder {

	private static final String TAG = "HeartRateDecoder";
	
	
	public HeartRateDecoder(){
	}

	public int getHeartRate(){
		return bpm;
	}
	
	boolean hrValFormat;
	boolean sensorContactStatus;
	boolean sensorContactFeature;
	boolean energyExpended;
	boolean rrInterval;
	
	int bpm;
	
	//sample data:  14457f037f0390036b03640364033003 	16 bytes
	//				144a53035303 						 6 bytes
	// 0482 2 bytes
	//14 = 0001 0100
	//04 = 0000 0100
	public void decodeData(byte[] data) {
		Log.d(TAG,"data: "+bytArrayToHex(data));
		
		int offset = 0;
		int flags = data[offset++];
	    
		if((flags&0x10)!=0){
		    if((flags&0x01)==0){ //8bit bpm
		        bpm = data[offset++];
		    } 
		    else{ //16bit bpm
		        bpm = ((data[offset++]&0xFF) | ((data[offset++]&0xFF)<<8));
		    }
		}
		//else
			//bpm = -1;
		
		Log.d(TAG,"heart rate: "+bpm);
		
	}
	
	public static String bytArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for(byte b: a)
			sb.append(String.format("%02x", b&0xff));
		return sb.toString();
	}
}
