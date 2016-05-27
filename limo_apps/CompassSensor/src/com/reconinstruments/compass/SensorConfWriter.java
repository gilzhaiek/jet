package com.reconinstruments.compass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.reconinstruments.utils.DeviceUtils;

public class SensorConfWriter {

	private static final String TAG = "SensorConfWriter";
	private static final String OFFSET = "offset";
	private static final String SCALE = "scale";
	
	private static final String CONFIG_FILE_LOC = "/data/system/sensors.conf";
	private static final String CONFIG_FILE_LOC_LIMO = "/system/lib/hw/sensors.conf";

	public static final String KEY_MAG_OFFSET_X = "MAG_OFFSET_X";
	public static final String KEY_MAG_OFFSET_Y = "MAG_OFFSET_Y";
	public static final String KEY_MAG_OFFSET_Z = "MAG_OFFSET_Z";

	public static boolean writeMagOffsets(Context context, double[] offsets, boolean offsetAddition) {
		File configFile;
		Log.v(TAG,"product name = " + Build.PRODUCT);
		if (DeviceUtils.isLimo()) {
			configFile = new File(CONFIG_FILE_LOC_LIMO);
		}
		else {
			configFile = new File(CONFIG_FILE_LOC);
		}

		StringBuffer strContent = new StringBuffer("");

		try {
			FileInputStream fin = new FileInputStream(configFile);

			int ch;
			while ((ch = fin.read()) != -1)
				strContent.append((char) ch);

			fin.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Could not find sensors.conf");
			Log.e(TAG, e.toString());
			return false;
		} catch (IOException ioe) {
			Log.e(TAG, "Could not read sensors.conf");
			Log.e(TAG, ioe.toString());
			return false;
		}

		Log.d(TAG, "Read in file");

		// Find line number of the magnetometer offsets
		String[] lines = strContent.toString().split("\n");
		int lineNum = findMagOffsetLineNumber(lines);

		if(lineNum < 0) return false;

		// Read in old offsets
		Log.d(TAG, lines[lineNum]);
		String[] offsetParts = lines[lineNum].split("\\s+");
		//double[] currentOffsets = new double[offsetParts.length - 2];

		// Add old offsets to new
		if(offsetAddition)
			for(int i=0; i<3; i++)
				try {
					Log.d(TAG, "offset["+i+"] = " + offsets[i]);
					offsets[i] += Double.parseDouble(offsetParts[i + 2]);
					Log.d(TAG, "offset["+i+"] = " + offsets[i]);
				} catch(Exception e) {
					Log.e(TAG, "could not parse existing offset: " + e.toString());
				}

		// Write out new offsets
		lines[lineNum] = "conv_B      = ";
		for(double offset : offsets)
			lines[lineNum] += offset + " ";

		Log.d(TAG, "writing file");

		// Write lines to file
		try {
			FileWriter fw;
			Log.v(TAG,"1 product name = " + Build.PRODUCT);

			if (DeviceUtils.isLimo()) {
				fw = new FileWriter(CONFIG_FILE_LOC_LIMO);
			} else {
				fw = new FileWriter(CONFIG_FILE_LOC);
			}


			for(String line : lines) {
				fw.write(line);
				fw.write("\n");
			}
			fw.close();
		} catch (IOException e) {
			Log.e(TAG, "Could not write sensors.conf");
			e.printStackTrace();
			return false;
		}

		// Write offsets to Settings Provider
		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_X, (float) offsets[0]);
		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_Y, (float) offsets[1]);
		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_Z, (float) offsets[2]);

		// Note that we have written preferences
		Settings.Secure.putInt(context.getContentResolver(), "hasWrittenMagOffsetsV2", 1);

		SharedPreferences mPrefs = context.getSharedPreferences(CompassCalibrationService.APP_SHARED_PREFS, Activity.MODE_WORLD_READABLE);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean("hasWrittenMagOffsetsV2", true);
		editor.commit();

		return true;
	}
	
	public static boolean writeMagOffsetsAndScale(Context context, double[] offsets, double[] scales, boolean offsetAddition) {
		File configFile;
		Log.v(TAG,"product name = " + Build.PRODUCT);
		if (DeviceUtils.isLimo()) {
			configFile = new File(CONFIG_FILE_LOC_LIMO);
		}
		else {
			configFile = new File(CONFIG_FILE_LOC);
		}

		StringBuffer strContent = new StringBuffer("");

		try {
			FileInputStream fin = new FileInputStream(configFile);

			int ch;
			while ((ch = fin.read()) != -1)
				strContent.append((char) ch);

			fin.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Could not find sensors.conf");
			Log.e(TAG, e.toString());
			return false;
		} catch (IOException ioe) {
			Log.e(TAG, "Could not read sensors.conf");
			Log.e(TAG, ioe.toString());
			return false;
		}

		Log.d(TAG, "Read in file");

		String[] lines = strContent.toString().split("\n");
	
		// Find line number of the magnetometer offsets
		int offsetLineNum = findMagLineNumber(lines, OFFSET);
		int scaleLineNum = findMagLineNumber(lines, SCALE);

		if(offsetLineNum < 0 || scaleLineNum < 0) return false;

		// Read in old scale
		Log.d(TAG, lines[scaleLineNum]);
		String[] scaleParts = lines[scaleLineNum].split("\\s+");
		double[] currentScales = new double[3];

		// Read in old offsets
		Log.d(TAG, lines[offsetLineNum]);
		String[] offsetParts = lines[offsetLineNum].split("\\s+");
		double[] currentOffsets = new double[3];
		
		for (int i = 0; i < 3; i++)
			try {
				Log.d(TAG, "offset[" + i + "] (before adding old offset) = " + offsets[i]);
				currentOffsets[i] = Double.parseDouble(offsetParts[i + 2]);
			} catch (Exception e) {
				Log.e(TAG, "could not parse existing offset: " + e.toString());
				if (i==0) //if the first offset is not defined, there's 0 offset
					currentOffsets[0]=0;
				else if(i > 0) // if the first offset is defined, the second and 3rd uses the first one
					currentOffsets[i] = currentOffsets[0];
			} finally {
				offsets[i] = (currentOffsets[i] - offsets[i]) / scales[i];
				Log.d(TAG, "offset[" + i + "] (final offset) = " + offsets[i]);
			}

		// divide the scale with new scale
		for (int i = 0; i < 3; i++)
			try {
				Log.d(TAG, "scale[" + i + "] (before multiplying old scale) = " + scales[i]);
				currentScales[i] = Double.parseDouble(scaleParts[i + 2]);
			} catch (Exception e) {
				Log.e(TAG, "could not parse existing scale: " + e.toString());
				if (i==0) //if the first scale is not defined, it's 1
					currentScales[0]=1;
				else if(i > 0) // if the first scale is defined, the second and 3rd uses the first one
					currentScales[i] = currentScales[0];
			} finally {
				scales[i] = currentScales[i] / scales[i];
				Log.d(TAG, "scale[" + i + "] (final scale) = " + scales[i]);
			}

		// Write out new scales
		lines[scaleLineNum] = "conv_A      = ";
		for(double scale : scales)
			lines[scaleLineNum] += scale + " ";

		// Write out new offsets
		lines[offsetLineNum] = "conv_B      = ";
		for(double offset : offsets)
			lines[offsetLineNum] += offset + " ";

		// Write lines to file
		try {
			FileWriter fw;
			Log.v(TAG,"1 product name = " + Build.PRODUCT);

			if (DeviceUtils.isLimo()) {
				fw = new FileWriter(CONFIG_FILE_LOC_LIMO);
			} else {
				fw = new FileWriter(CONFIG_FILE_LOC);
			}

			for(String line : lines) {
				fw.write(line);
				fw.write("\n");
			}
			fw.close();
		} catch (IOException e) {
			Log.e(TAG, "Could not write sensors.conf");
			e.printStackTrace();
			return false;
		}

		// Write offsets to Settings Provider
		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_X, (float) offsets[0]);
		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_Y, (float) offsets[1]);
		Settings.Secure.putFloat(context.getContentResolver(), KEY_MAG_OFFSET_Z, (float) offsets[2]);

		// Note that we have written preferences
		Settings.Secure.putInt(context.getContentResolver(), "hasWrittenMagOffsetsV2", 1);

		SharedPreferences mPrefs = context.getSharedPreferences(CompassCalibrationService.APP_SHARED_PREFS, Activity.MODE_WORLD_READABLE);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean("hasWrittenMagOffsetsV2", true);
		editor.commit();

		return true;
	}

	private static int findMagOffsetLineNumber(String[] lines) {
		int lineNum = 0;
		boolean inMagSection = false;
		boolean foundMagOffsetLine = false;
		for(String line : lines) {
			//Log.d(TAG, line);
			if(line.matches("(.*RI_SENSOR_HANDLE_MAGNETOMETER.*)"))
				inMagSection = true;

			if(inMagSection && line.matches("#?conv_B.*")) {
				foundMagOffsetLine = true;
				break;
			} else {
				lineNum++;
			}
		}

		if(!foundMagOffsetLine) {
			Log.d(TAG, "Could not find line");
			return -1;
		}

		Log.d(TAG, "found on line " + lineNum);

		return lineNum;
	}
	
	private static int findMagLineNumber(String[] lines, String option){
		String regExpression = "";
		if(option.equals(OFFSET)){
			regExpression = "#?conv_B.*";
		}
		else if(option.equals(SCALE)){
			regExpression = "#?conv_A.*";
		}
		int lineNum = 0;
		boolean inMagSection = false;
		boolean foundMagOffsetLine = false;
		for(String line : lines) {
			//Log.d(TAG, line);
			if(line.matches("(.*RI_SENSOR_HANDLE_MAGNETOMETER.*)"))
				inMagSection = true;

			if(inMagSection && line.matches(regExpression)) {
				foundMagOffsetLine = true;
				break;
			} else {
				lineNum++;
			}
		}

		if(!foundMagOffsetLine) {
			Log.d(TAG, "Could not find line");
			return -1;
		}

		Log.d(TAG, "found on line " + lineNum);
		return lineNum;
	}
}
