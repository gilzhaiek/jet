package com.reconinstruments.compass;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

/**
 * Sensors.conf parsing and writing class to replace SensorsConfWriter
 * @author darrell
 *
 */
public class SensorsConf {
	private static final String TAG = "SensorsConf";
	public static final int ACCELEROMETER = 0;
	public static final int GYROSCOPE = 1;
	public static final int MAGNETOMETER = 2;

	
	private Sensor[] sensGroup = new Sensor[3];
	
	private String allLines;
	private String[] lines;
	private boolean fileLoaded, fileParsed;
	private static final String CONFIG_FILE_LOC = "/data/system/sensors.conf";
	private static final String CONFIG_FILE_LOC_WRITTEN = "/data/system/sensors_written.conf";
	
	public static final String KEY_MAG_OFFSET_X = "MAG_OFFSET_X";
	public static final String KEY_MAG_OFFSET_Y = "MAG_OFFSET_Y";
	public static final String KEY_MAG_OFFSET_Z = "MAG_OFFSET_Z";
	
	public SensorsConf(){

		sensGroup[ACCELEROMETER] = new Sensor();
		sensGroup[GYROSCOPE] = new Sensor();
		sensGroup[MAGNETOMETER] = new Sensor();
		fileLoaded = false;
		fileParsed = false;
	}
	
	
	public void loadFile(){
		StringBuilder sb = null;
	
		try {
			BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE_LOC));
			sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			
			br.close();
			allLines = sb.toString();
			lines = allLines.split("\n");
			fileLoaded = true;
			fileParsed = false;
			
			Log.d(TAG,"Loaded sensors.conf file in "+ CONFIG_FILE_LOC);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public boolean updateFile(Context context){
		int lineNum;
		float[] values = new float[3]; 
		int sensNum = 0;
		for (Sensor sensor:sensGroup){
			if (sensor.checkModified(Key.conv_A)){
				lineNum = sensor.getFieldLine(Key.conv_A);
				values = sensor.getFieldValue(Key.conv_A);
				Log.d(TAG,"Modifying conv_A in ["+getSensorStr(sensNum) +"]");
				lines[lineNum] = "conv_A      = ";
				for(float value : values)
					lines[lineNum] += value + " ";
			}
			if (sensor.checkModified(Key.conv_B)){
				Log.d(TAG,"Modifying conv_B in ["+getSensorStr(sensNum) +"]");
				lineNum = sensor.getFieldLine(Key.conv_B);
				values = sensor.getFieldValue(Key.conv_B);
				lines[lineNum] = "conv_B      = ";
				for(float value : values)
					lines[lineNum] += value + " ";
				Log.d(TAG,lines[lineNum]);
			}
			sensNum++;
		}

		// Only Magnetometer has MulA1-3
		if (sensGroup[MAGNETOMETER].checkModified(Key.mul_A1)){
			Log.d(TAG,"Modifying mul_A1 in ["+getSensorStr(MAGNETOMETER) +"]");
			lineNum = sensGroup[MAGNETOMETER].getFieldLine(Key.mul_A1);
			values = sensGroup[MAGNETOMETER].getFieldValue(Key.mul_A1);
			
			lines[lineNum] = String.format("mul_A1\t= %.8f %.8f %.8f",values[0],values[1],values[2]);
//			lines[lineNum] = "mul_A1      = ";
//			for(float value : values)
//				lines[lineNum] += value + " ";
		}
		if (sensGroup[MAGNETOMETER].checkModified(Key.mul_A2)){
			Log.d(TAG,"Modifying mul_A2 in ["+getSensorStr(MAGNETOMETER) +"]");
			lineNum = sensGroup[MAGNETOMETER].getFieldLine(Key.mul_A2);
			values = sensGroup[MAGNETOMETER].getFieldValue(Key.mul_A2);
			
			lines[lineNum] = String.format("mul_A2\t= %.8f %.8f %.8f",values[0],values[1],values[2]);
//			lines[lineNum] = "mul_A2      = ";
//			for(float value : values)
//				lines[lineNum] += value + " ";
		}
		if (sensGroup[MAGNETOMETER].checkModified(Key.mul_A3)){
			Log.d(TAG,"Modifying mul_A3 in ["+getSensorStr(MAGNETOMETER) +"]");
			lineNum = sensGroup[MAGNETOMETER].getFieldLine(Key.mul_A3);
			values = sensGroup[MAGNETOMETER].getFieldValue(Key.mul_A3);
			lines[lineNum] = String.format("mul_A3\t= %.8f %.8f %.8f",values[0],values[1],values[2]);
//			lines[lineNum] = "mul_A3      = ";
//			for(float value : values)
//				lines[lineNum] += value + " ";
		}
		
		// Write lines to file
		try {
			FileWriter fw;

			fw = new FileWriter(CONFIG_FILE_LOC);

			for(String line : lines) {
				fw.write(line);
				fw.write("\n");
			}
			fw.close();
			Log.d(TAG,"Written to sensors.conf file in "+ CONFIG_FILE_LOC);
		} catch (IOException e) {
			Log.d(TAG, "Could not write sensors.conf");
			e.printStackTrace();
			return false;
		}
		
		// Write offsets to Settings Provider
//		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_X, (float) offsets[0]);
//		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_Y, (float) offsets[1]);
//		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_Z, (float) offsets[2]);

		// Note that we have written preferences
		Settings.Secure.putInt(context.getContentResolver(), "hasWrittenMagOffsetsV2", 1);

		SharedPreferences mPrefs = context.getSharedPreferences(CompassCalibrationService.APP_SHARED_PREFS, Activity.MODE_WORLD_READABLE);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean("hasWrittenMagOffsetsV2", true);
		editor.commit();


		return true;
	}
	

	/**
	 * Get conv_A vector. 
	 * 
	 * If FieldType is ONE, the array will be filled with the single conv_A value
	 * Returns null if FieldType is COMMENT.
	 * 
	 * @param sensor
	 * @return conv_A array
	 */
	public float[] getConv_A (int sensor){
		if (!fileParsed)
			return null;
		return sensGroup[sensor].getFieldValue(Key.conv_A);
		
	}


	public float[] getConvB(int sensor){
		if (!fileParsed)
			return null;		
		return sensGroup[sensor].getFieldValue(Key.conv_B);

	}

	/*
	private float[] getRotA(int sensor){
		if (!fileParsed)
			return null;
		return sensGroup[sensor].getFieldValue(Key.rot_A);

	}
	private float[] getRotB(int sensor){
		if (!fileParsed)
			return null;
		return sensGroup[sensor].getFieldValue(Key.rot_A);			

	}
	private float[] getRotC(int sensor){
		if (!fileParsed)
			return null;
		return sensGroup[sensor].getFieldValue(Key.rot_A);			

	}
	*/

	private float[] getMulA1(){
		if (!fileParsed)
			return null;
		return sensGroup[MAGNETOMETER].getFieldValue(Key.mul_A1);	

	}
	private float[] getMulA2(){
		if (!fileParsed)
			return null;

		return sensGroup[MAGNETOMETER].getFieldValue(Key.mul_A2);	

	}
	private float[] getMulA3(){
		if (fileParsed)
			return sensGroup[MAGNETOMETER].getFieldValue(Key.mul_A3);			
		else
			return null;
	}
	
	private double[][] getMulA(){
		if (!fileParsed)
			return null;	
		double[][] mulA = new double[3][3];
		float[] row1 = getMulA1();
		float[] row2 = getMulA2();
		float[] row3 = getMulA3();
		
		for (int i=0; i<3; i++){
			mulA[0][i] = (double)row1[i];
			mulA[1][i] = (double)row2[i];
			mulA[2][i] = (double)row3[i];
		}
		
		return mulA;
	}

	/**
	 * Update the conv_A ONLY. (IE: new_conv_B = 0);
	 * <p>
	 * IMPORTANT: Use updateConvAB() if both values need to be updated!
	 * @param sensor
	 * @param new_conv_A
	 * @return
	 */
	public boolean update_convA(int sensor,float[] new_conv_A){
		if (!fileParsed)
			return false;
		float[] conv_A = new float[3];
		float[] old_conv_A = sensGroup[sensor].getFieldValue(Key.conv_A);
		
		/*
		 * old_calibrated_meas = old_conv_A * raw_meas + old_conv_B
		 * new_calibrated_meas = new_conv_A * old_calibrated_meas + new_conv_B
		 *
		 * Therefore:
		 * conv_A = new_conv_A * old_conv_A
		 */
		
		for (int i=0; i<3; i++)
			conv_A[i]= new_conv_A[i]*old_conv_A[i];
		
		sensGroup[sensor].updateField(Key.conv_A, conv_A);
		
		return true;
		
	}
	/**
	 * Update the conv_B ONLY. (IE: new_conv_A = 1 or mul_A = identity)
	 * <p>
	 * IMPORTANT: Use updateConvAB() if both values need to be updated!
	 * 
	 * @param sensor
	 * @param new_conv_B
	 * @return
	 */
	public boolean update_convB(int sensor,float[] new_conv_B) {
		if (!fileParsed)
			return false;
		
		float[] conv_B = new float[3];
		float[] old_conv_B = sensGroup[sensor].getFieldValue(Key.conv_B);
		
		/*
		 * old_calibrated_meas = old_conv_A * raw_meas + old_conv_B
		 * new_calibrated_meas = old_calibrated_meas + new_conv_B
		 *
		 * Therefore:
		 * conv_B = old_conv_B + new_conv_B
		 */
		
		for (int i=0; i<3; i++)
			conv_B[i]= old_conv_B[i]+new_conv_B[i];
		
		sensGroup[sensor].updateField(Key.conv_B, conv_B);
		
		return true;
		
	}
	/**
	 * Update the conv_A along with conv_B
	 * 
	 * @param sensor
	 * @param new_conv_A
	 * @param new_conv_B
	 * @return
	 */
	public boolean update_convA_convB(int sensor,float[] new_conv_A,float[] new_conv_B) {
		if (!fileParsed)
			return false;
		
		float[] conv_B = new float[3];
//		float[] new_conv_A = sensGroup[sensor].getFieldValue(Key.conv_A);
		float[] old_conv_B = sensGroup[sensor].getFieldValue(Key.conv_B);
		
		/*
		 * old_calibrated_meas = old_conv_A * raw_meas + old_conv_B
		 * new_calibrated_meas = new_conv_A * old_calibrated_meas + new_conv_B
		 *
		 * Therefore:
		 * conv_B = new_conv_A * old_conv_B + new_conv_B
		 */
		
		for (int i=0; i<3; i++)
			conv_B[i]= new_conv_A[i]*old_conv_B[i]+new_conv_B[i];
		
		sensGroup[sensor].updateField(Key.conv_B, conv_B);
		
		return true;
		
	}
	
	public static double[] convertFloatsToDoubles(float[] input)
	{
	    if (input == null)
	    {
	        return null; // Or throw an exception - your choice
	    }
	    double[] output = new double[input.length];
	    for (int i = 0; i < input.length; i++)
	    {
	        output[i] = input[i];
	    }
	    return output;
	}
	public static float[] convertDoublesToFloats(double[] input)
	{
	    if (input == null)
	    {
	        return null; // Or throw an exception - your choice
	    }
	    float[] output = new float[input.length];
	    for (int i = 0; i < input.length; i++)
	    {
	        output[i] = (float) input[i];
	    }
	    return output;
	}
	
	public boolean update_mulA_convB(int sensor,float[] new_mul_A1,float[] new_mul_A2,float[] new_mul_A3,float[] new_conv_B){
		if (!fileParsed)
			return false;
		Log.d(TAG,"update_mulA_convB()");
		/* Update mul_A1-2 */
		RealMatrix new_mul_A = MatrixUtils.createRealMatrix(3, 3);
		new_mul_A.setRow(0, convertFloatsToDoubles(new_mul_A1));
		new_mul_A.setRow(1, convertFloatsToDoubles(new_mul_A2));
		new_mul_A.setRow(2, convertFloatsToDoubles(new_mul_A3));
		
		updateMulA(new_mul_A);
		
		/*
		 * old_calibrated_meas = old_conv_A * raw_meas + old_conv_B
		 * new_calibrated_meas = new_conv_A * old_calibrated_meas + new_conv_B
		 *
		 * Therefore:
		 * conv_B = new_mul_A * old_conv_B + new_conv_B
		 */
		float[] conv_B = new float[3];
		float[] old_conv_B = sensGroup[sensor].getFieldValue(Key.conv_B);
		
		conv_B[0]=new_mul_A1[0]*old_conv_B[0]+new_mul_A1[1]*old_conv_B[1]+new_mul_A1[2]*old_conv_B[2] + new_conv_B[0];
		conv_B[1]=new_mul_A2[0]*old_conv_B[0]+new_mul_A2[1]*old_conv_B[1]+new_mul_A2[2]*old_conv_B[2] + new_conv_B[1];
		conv_B[2]=new_mul_A3[0]*old_conv_B[0]+new_mul_A3[1]*old_conv_B[1]+new_mul_A3[2]*old_conv_B[2] + new_conv_B[2];
		
		
		sensGroup[sensor].updateField(Key.conv_B, conv_B);
		
		return true;
		
	}

	
	/**
	 * Update Magnetometer soft-iron scaling matrix
	 * @param value
	 */
	public boolean updateMulA(RealMatrix new_mulA){
		if (!fileParsed)
			return false;
		
		Log.d(TAG,"updateMulA()");
		RealMatrix mulA = MatrixUtils.createRealMatrix(3,3);
		RealMatrix old_mulA = MatrixUtils.createRealMatrix(getMulA());
		/*
		 * old_calibrated_meas = old_conv_A * raw_meas + old_conv_B
		 * new_calibrated_meas = new_conv_A * old_calibrated_meas + new_conv_B
		 *
		 * Therefore:
		 * conv_A = new_conv_A * old_conv_A
		 */
		mulA = new_mulA.multiply(old_mulA);
		double[] mulA1 = mulA.getRow(0);
		double[] mulA2 = mulA.getRow(1);
		double[] mulA3 = mulA.getRow(2);
		sensGroup[MAGNETOMETER].updateField(Key.mul_A1, mulA1);
		sensGroup[MAGNETOMETER].updateField(Key.mul_A2, mulA2);
		sensGroup[MAGNETOMETER].updateField(Key.mul_A3, mulA3);
		
		return true;
		
	}
	
	
	public boolean parseFile(){
		long millis = System.currentTimeMillis();
		if (!fileLoaded)
			return false;
		Log.d(TAG,"===== START parsing sensors.conf ===== ");
		
		String accSensorRegEx = "^Handle[ \t]*=[ \t]*0x01.*";
		String gyroSensorRegEx = "^Handle[ \t]*=[ \t]*0x08.*";
		String magSensorRegEx = "^Handle[ \t]*=[ \t]*0x02.*";
		// pressure sensor is only used to determine if line search has past magnetometer section
		String pressSensorRegEx = "^Handle[ \t]*=[ \t]*0x20.*";
		
		Pattern convAPattern1 = Pattern.compile("^conv_A[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[ \t]*(\\#|\\n)");
		Pattern convAPattern3 = Pattern.compile("^conv_A[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)");
		Pattern convBPattern1 = Pattern.compile("^conv_B[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[ \t]*(\\#|\\n)");
		Pattern convBPattern3 = Pattern.compile("^conv_B[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)");
		Pattern rotAPattern =  	 Pattern.compile("^rot_A[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)");
		Pattern rotBPattern = 	 Pattern.compile("^rot_B[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)");
		Pattern rotCPattern = 	 Pattern.compile("^rot_C[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)[ \\t]+(-?\\d*\\.?\\d+)");
		Pattern mulA1Pattern = 	Pattern.compile("^mul_A1[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[, \\t]+(-?\\d*\\.?\\d+)[, \\t]+(-?\\d*\\.?\\d+)");
		Pattern mulA2Pattern = 	Pattern.compile("^mul_A2[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[, \\t]+(-?\\d*\\.?\\d+)[, \\t]+(-?\\d*\\.?\\d+)");
		Pattern mulA3Pattern = 	Pattern.compile("^mul_A3[ \\t]*=[ \\t]*(-?\\d*?\\.?\\d+)[, \\t]+(-?\\d*\\.?\\d+)[, \\t]+(-?\\d*\\.?\\d+)");
		Matcher matcher;
		
//		String convARegEx = "conv_A";
		
		int currentSection = -1;

	
		int lineNum = 0;

		for ( lineNum=0; lineNum<lines.length; lineNum++){
			
			
			/********* Find which sensor section it's in ***/
			if (lines[lineNum].matches(pressSensorRegEx))
				break;
			
			if(currentSection<ACCELEROMETER){
				if( lines[lineNum].matches(accSensorRegEx)){

					currentSection = ACCELEROMETER;
					Log.d(TAG,"=== ACCELEROMETER section ===");
				}	
			}else if (currentSection<GYROSCOPE){ 
				if(lines[lineNum].matches(gyroSensorRegEx)){

					currentSection = GYROSCOPE;
					Log.d(TAG,"=== GYROSCOPE section ===");
				}	
			}else if (currentSection<MAGNETOMETER) { 
				if( lines[lineNum].matches(magSensorRegEx)){
					currentSection = MAGNETOMETER;
					Log.d(TAG,"=== MAGNETOMETER section ===");
				}
			}
			
			/********* Start parsing field values only after the first sensor segment is found ***/
			if (currentSection>=ACCELEROMETER){
				if (lines[lineNum].matches("#conv_A.*")){
					sensGroup[currentSection].addField(Key.conv_A, lineNum);
						Log.d(TAG,getSensorStr(currentSection) + " conv_A is unused");

				}else if (lines[lineNum].matches("#conv_B.*")){
					sensGroup[currentSection].addField(Key.conv_B, lineNum);
					
						Log.d(TAG,getSensorStr(currentSection) + " conv_B is unused");

				}else if (lines[lineNum].matches("^conv_A.*")){
					//				Log.d(TAG,"Found conv_A");
					matcher = convAPattern1.matcher(lines[lineNum]);
					if (matcher.find()){
						// Matches (1)  conv_A
						sensGroup[currentSection].addField(Key.conv_A, Float.parseFloat(matcher.group(1)), lineNum);

						Log.d(TAG,getSensorStr(currentSection)+" mConvA is "+ sensGroup[currentSection].getFieldValue(Key.conv_A)[0]);
						continue;
					}
					matcher = convAPattern3.matcher(lines[lineNum]);
					if (matcher.find()){
						// Matches (3) conv_A
						sensGroup[currentSection].addField(Key.conv_A, new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))}, lineNum);

						Log.d(TAG,getSensorStr(currentSection)+" mConvA is "+ sensGroup[currentSection].getFieldValue(Key.conv_A)[0]+", "
								+ sensGroup[currentSection].getFieldValue(Key.conv_A)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.conv_A)[2]);
						continue;
					}

				}else if (lines[lineNum].matches("^conv_B.*")){
					//				Log.d(TAG,"Found conv_B");
					matcher = convBPattern1.matcher(lines[lineNum]);
					if (matcher.find()){
						// Matches (1) conv_A
						sensGroup[currentSection].addField(Key.conv_B, Float.parseFloat(matcher.group(1)), lineNum);
						Log.d(TAG,getSensorStr(currentSection)+" mConvB is "+ sensGroup[currentSection].getFieldValue(Key.conv_B)[0]);
						continue;
					}
					matcher = convBPattern3.matcher(lines[lineNum]);
					if (matcher.find()){
						// Matches (3) conv_A
						sensGroup[currentSection].addField(Key.conv_B, new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))}, lineNum);

						Log.d(TAG,getSensorStr(currentSection)+" mConvB is "+ sensGroup[currentSection].getFieldValue(Key.conv_B)[0]+", "
								+ sensGroup[currentSection].getFieldValue(Key.conv_B)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.conv_B)[2]);
						continue;
					}
				}

				/********* Parse rotA, rotB & rot_C ******/		
				matcher = rotAPattern.matcher(lines[lineNum]);
				if (matcher.find()){
					float[] arr = new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))};
					sensGroup[currentSection].addField(Key.rot_A, arr, lineNum);
					Log.d(TAG,getSensorStr(currentSection)+" rotA is "+ sensGroup[currentSection].getFieldValue(Key.rot_A)[0]+", "
							+ sensGroup[currentSection].getFieldValue(Key.rot_A)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.rot_A)[2]);
					continue;
				}
				matcher = rotBPattern.matcher(lines[lineNum]);
				if (matcher.find()){
					float[] arr = new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))};
					sensGroup[currentSection].addField(Key.rot_B, arr, lineNum);
					Log.d(TAG,getSensorStr(currentSection)+" rot_B is "+ sensGroup[currentSection].getFieldValue(Key.rot_B)[0]+", "
							+ sensGroup[currentSection].getFieldValue(Key.rot_B)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.rot_B)[2]);
					continue;
				}
				matcher = rotCPattern.matcher(lines[lineNum]);
				if (matcher.find()){
					float[] arr = new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))};
					sensGroup[currentSection].addField(Key.rot_C, arr, lineNum);
					Log.d(TAG,getSensorStr(currentSection)+" rot_C is "+ sensGroup[currentSection].getFieldValue(Key.rot_C)[0]+", "
							+ sensGroup[currentSection].getFieldValue(Key.rot_C)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.rot_C)[2]);
					continue;
				}
			}
			/********* Parse mul_A1, mul_A2, mul_A3 (Magnetometer only) ******/
			if (currentSection == MAGNETOMETER){
				matcher = mulA1Pattern.matcher(lines[lineNum]);
				if (matcher.find()){
					float[] arr = new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))};
					sensGroup[currentSection].addField(Key.mul_A1, arr, lineNum);
					Log.d(TAG,getSensorStr(currentSection)+" mulA1 is "+ sensGroup[currentSection].getFieldValue(Key.mul_A1)[0]+", "
							+ sensGroup[currentSection].getFieldValue(Key.mul_A1)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.mul_A1)[2]);
					continue;
				}
				matcher = mulA2Pattern.matcher(lines[lineNum]);
				if (matcher.find()){
					float[] arr = new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))};
					sensGroup[currentSection].addField(Key.mul_A2, arr, lineNum);
					Log.d(TAG,getSensorStr(currentSection)+" mulA2 is "+ sensGroup[currentSection].getFieldValue(Key.mul_A2)[0]+", "
							+ sensGroup[currentSection].getFieldValue(Key.mul_A2)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.mul_A2)[2]);
					continue;
				}
				matcher = mulA3Pattern.matcher(lines[lineNum]);
				if (matcher.find()){
					float[] arr = new float[]{Float.parseFloat(matcher.group(1)),Float.parseFloat(matcher.group(2)),Float.parseFloat(matcher.group(3))};
					sensGroup[currentSection].addField(Key.mul_A3, arr, lineNum);
					Log.d(TAG,getSensorStr(currentSection)+" mulA3 is "+ sensGroup[currentSection].getFieldValue(Key.mul_A3)[0]+", "
							+ sensGroup[currentSection].getFieldValue(Key.mul_A3)[1]+", "+ sensGroup[currentSection].getFieldValue(Key.mul_A3)[2]);
					continue;
				}
			}
		}
		long dt = System.currentTimeMillis() - millis;
		Log.d(TAG,"Parsing time: "+dt +" ms"); 
		fileParsed = true;
		Log.d(TAG,"===== FINISHED parsing in " + +dt +" ms ===== ");
		return true;	
	}
	
	public static String getSensorStr(int sensor){
		String str = "NULL";
		switch(sensor){
		case 0:
			return "ACCELEROMETER";
		case 1:
			return "GYROSCOPE";
	
		case 2:
			return "MAGNETOMETER";

		case 3:
			return "PRESSURE";

		}
		return str;
	}
}
