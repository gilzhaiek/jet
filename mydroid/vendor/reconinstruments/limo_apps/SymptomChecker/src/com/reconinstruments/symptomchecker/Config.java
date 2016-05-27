package com.reconinstruments.symptomchecker;

import android.os.Environment;

public class Config {
		
	public static String CameraFile = "SymptomCheckerPhoto.jpg";
	public static String LogcatFile = "Logcat.txt";
	public static String ReportFile = "SymptomCheckerReport.txt";
	public static String Directory = Environment.getExternalStorageDirectory().getPath() + "/ReconApps/SymptomChecker";
	
	
	public static float AccMinX = -12.0f;
	public static float AccMaxX = 12.0f;
	public static float AccMinY = -12.0f;
	public static float AccMaxY = 12.0f;
	public static float AccMinZ = -12.0f;
	public static float AccMaxZ = 12.0f;
	public static float AccMinMag = 7.0f;
	public static float AccMaxMag = 15.0f;
	
	public static float MagMinX = -100.0f;
	public static float MagMaxX = 100.0f;
	public static float MagMinY = -100.0f;
	public static float MagMaxY = 100.0f;
	public static float MagMinZ = -100.0f;
	public static float MagMaxZ = 100.0f;
	public static float MagMinMag = 20.0f;
	public static float MagMaxMag = 250.0f;
	
	public static float GyroMinX = -10.0f;
	public static float GyroMaxX = 10.0f;
	public static float GyroMinY = -10.0f;
	public static float GyroMaxY = 10.0f;
	public static float GyroMinZ = -10.0f;
	public static float GyroMaxZ = 10.0f;
			
	public static float PressureMin = 700.0f;
	public static float PressureMax = 1500.0f;
	
	public static float TemperatureMin = 5.0f;
	public static float TemperatureMax = 40.0f;
	
	public static int WifiSize = 1;
	
	public static int GPSSize = 1;
	
	public static int BluetoothSize = 1;
	
}
