package com.reconinstruments.compass;

public class CalibrationParameters {

	//TODO: Add all the offset, scaling results here
	public float std; 		//Standard deviation of the final calibrated readings
	public int iterations;	
	public long id; 	//Time of calibration. (it'll be used to ID each calibration case)
	
	public CalibrationParameters(){
		id = System.currentTimeMillis();
	}

}
