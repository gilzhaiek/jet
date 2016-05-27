package com.reconinstruments.compass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math3.optimization.direct.SimplexOptimizer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

public class CompassSensorListener implements SensorEventListener {

	// This is to turn on and off logs for process information and results.
	private final boolean VERBOSE = true;
	
	private static final String TAG = "CompassSensorListener";

	private SensorManager mSensorManager;
	private float[] mMagneticValues;

	//	private float[] mAccelerometerValues;

	//	private float mAzimuth;
	//	private float mPitch;
	//	private float mRoll;

	// Magnetometer error correction
	private double magOffsetX = 0, magOffsetY = 0, magOffsetZ = 0;
	private ErrFunction errFunction;
	private boolean loggingForErrorCorrection = false;
	private boolean optimizationComplete = false;

	private float[] rotationMatrix = new float[9]; // this will actually store the inverse of the rotation matrix in sensors.conf
	private boolean matrixLoaded = false;

	private static final double minHWFailure = 5;
	private static final double maxHWFailure = 300;
	private static final double percentHWFailure = 0.1;

	Context context;

	public CompassSensorListener(Context context)
	{
		this.context = context;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}

	public void startCalibration(){
		// Clear old data and start correction
		loggingForErrorCorrection = true;
		errFunction = new ErrFunction();

		// Load matrix from sensors.conf and invert it
		float[] rotation = new float[9];
		if (CompassUtil.getSensorMatrix(rotation))
		{
			// transpose is the same as inverse so
			// we transpose because it's easier
			// [0] [1] [2]			[0] [3] [6]
			// [3] [4] [5]	--->	[1] [4] [7]
			// [6] [7] [8]			[2] [5] [8]

			rotationMatrix[0] = rotation[0];
			rotationMatrix[4] = rotation[4];
			rotationMatrix[8] = rotation[8];

			rotationMatrix[1] = rotation[3];
			rotationMatrix[3] = rotation[1];

			rotationMatrix[2] = rotation[6];
			rotationMatrix[6] = rotation[2];

			rotationMatrix[5] = rotation[7];
			rotationMatrix[7] = rotation[5];

			matrixLoaded = true;

			String str = String
				.format("\n|% 3.3f % 3.3f % 3.3f|\n|% 3.3f % 3.3f % 3.3f|\n|% 3.3f % 3.3f % 3.3f|",
						(rotationMatrix[0]), (rotationMatrix[1]), (rotationMatrix[2]), (rotationMatrix[3]), (rotationMatrix[4]), (rotationMatrix[5]), (rotationMatrix[6]),
						(rotationMatrix[7]), (rotationMatrix[8]));
				Log.w(TAG,"Loaded and inverted matrix:\n" + str);
		}

		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),(int)20000);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),(int)10000);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),(int)10000);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),(int)10000);

	}

	public void finishCalibration() {
		loggingForErrorCorrection = false;
		mSensorManager.unregisterListener(this);
	}
	public boolean isCalibrationSuccesful(){
		if (errFunction != null)
		{
			ErrFunction.Stats s = errFunction.GetMinMax();
			Log.d(TAG,"min: " + Math.sqrt(s.min) + ", max: " + Math.sqrt(s.max) + ", avg: " + Math.sqrt(s.avg));
		}
		return calculateOffsets();
	}

	public boolean isHardwareFaulty()
	{
		double inRange = 0;
		if (errFunction != null)
		{
			inRange = errFunction.rangePercent(minHWFailure,maxHWFailure);
			Log.d(TAG, "Compass hw test: " + (inRange * 100) + "% of values within acceptable range");
		}

		return (inRange < percentHWFailure);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
			mMagneticValues = event.values.clone();//limoToAndroidMag(event.values.clone());

			if(loggingForErrorCorrection && errFunction != null) {
				if (matrixLoaded)
				{
					// rewrite matrix multiply
					Float x0 = mMagneticValues[0];
	                Float x1 = mMagneticValues[1];
	                Float x2 = mMagneticValues[2];
	                mMagneticValues[0] = rotationMatrix[0]*x0 + rotationMatrix[1]*x1 + rotationMatrix[2]*x2;
	                mMagneticValues[1] = rotationMatrix[3]*x0 + rotationMatrix[4]*x1 + rotationMatrix[5]*x2;
	                mMagneticValues[2] = rotationMatrix[6]*x0 + rotationMatrix[7]*x1 + rotationMatrix[8]*x2;
				}
				errFunction.addPoints(Double.valueOf(mMagneticValues[0]), Double.valueOf(mMagneticValues[1]), Double.valueOf(mMagneticValues[2]));
			}

			//non tilt azimuth
			double degrees = Math.toDegrees(Math.abs(Math.atan2(mMagneticValues[2], mMagneticValues[1]) - Math.PI));
			if(optimizationComplete) Log.d(TAG, "non tilt az: " + degrees);

			/*
			if(degrees >= 0 && degrees < 22.5)
				heading.setText("N " + (int) degrees);
			else if(degrees >= 22.5 && degrees < 67.5)
				heading.setText("NE " + (int) degrees);
			else if(degrees >= 67.5 && degrees < 112.5)
				heading.setText("E " + (int) degrees);
			else if(degrees >= 112.5 && degrees < 157.5)
				heading.setText("SE " + (int) degrees);
			else if(degrees >= 157.5 && degrees < 202.5)
				heading.setText("S " + (int) degrees);
			else if(degrees >= 202.5 && degrees < 247.5)
				heading.setText("SW " + (int) degrees);
			else if(degrees >= 247.5 && degrees < 292.5)
				heading.setText("W " + (int) degrees);
			else if(degrees >= 292.5 && degrees < 337.5)
				heading.setText("NW " + (int) degrees);
			else if(degrees >= 337.5)
				heading.setText("N " + (int) degrees);*/

			break;
			//		case Sensor.TYPE_ACCELEROMETER:
			//			mAccelerometerValues = limoToAndroidAccel(event.values.clone());
			//			break;
		}
		//
		//		if (mMagneticValues != null && mAccelerometerValues != null) {
		//			float[] R = new float[9];
		//			float[] I = new float[9];
		//			SensorManager.getRotationMatrix(R, I, mAccelerometerValues,
		//					mMagneticValues);
		//			float[] orientation = new float[3];
		//			SensorManager.getOrientation(R, orientation);
		//			mAzimuth = orientation[0];
		//			mPitch = orientation[1];
		//			mRoll = orientation[2];

		//			Log.d(TAG, "azimuth: " + Math.toDegrees(SensorManager.getInclination(I)));
		//			Log.d(TAG, "pitch: " + Math.toDegrees(mPitch));
		//			Log.d(TAG, "roll: " + Math.toDegrees(mRoll));
		//		}
	}

	//	private float[] limoToAndroidAccel(float[] values) {
	//		float[] arr = new float[3];
	//
	//		arr[0] = values[2];
	//		arr[1] = values[0];
	//		arr[2] = -values[1];
	//
	//		return arr;
	//	}

	private float[] limoToAndroidMag(float[] values) {
		float[] arr = new float[3];

		arr[0] = values[0] + (float) magOffsetX;
		arr[1] = values[1] + (float) magOffsetY;
		arr[2] = values[2] + (float) magOffsetZ;

		return arr;
	}

	//	public float calcMagYaw(float[] orientation) {
	//		return (float) Math.toDegrees(Math.atan2(-orientation[0], orientation[1]));
	//	}

	public boolean calculateOffsets() {
		//Toast.makeText(context, "Running Optimization", Toast.LENGTH_SHORT).show();

		if(errFunction != null){
			// Run optimizer
			SimplexOptimizer optimizer = new SimplexOptimizer();
			optimizer.setSimplex(new NelderMeadSimplex(3));
			final PointValuePair optimum = optimizer.optimize(1000, errFunction, GoalType.MINIMIZE, new double[] { 0, 0, 0 });

			// Get values
			magOffsetX = optimum.getPoint()[0];
			magOffsetY = optimum.getPoint()[1];
			magOffsetZ = optimum.getPoint()[2];
			//Toast.makeText(context, "Optimization Complete", Toast.LENGTH_SHORT).show();
			
			if (CompassSensorActivity.LOG_DIAGNOSTIC_DATA){
				//save readings
				Log.d(TAG,"Saving all readings to CSV");
				errFunction.writeReadingssToCSV();
			}
			if (Build.PRODUCT.equals("limo")) {
				// Save values
				SensorConfWriter.writeMagOffsets(context, optimum.getPoint(), true);
			}
			else {
				double avgMagStrengh = errFunction.GetAverageMagStrengh(magOffsetX, magOffsetY, magOffsetZ);
				//Log.i(TAG,"Average Mag Strengh : " + avgMagStrengh);
				if(errFunction.CalculateOffsetAndScale(avgMagStrengh)){ 	//if Calculation of the offset and scale success then save values
					
					if (CompassSensorActivity.LOG_DIAGNOSTIC_DATA){
						Log.d(TAG,"Saving calibration parameter to txt");
						errFunction.writeCalParamToCSV();
					}
					SensorsConf conf = new SensorsConf();
					conf.loadFile();
					conf.parseFile();
					
					double[] offsets = errFunction.GetOffsets().clone();
					Log.d(TAG,"offsets: "+offsets[0] + ", "+offsets[1] + ", "+offsets[2]);
//					double[] scales  = errFunction.GetScales();
					double[][] scale_matrix = errFunction.GetScaleMatrix().clone();
					Log.d(TAG,"Scale_matrix row1: "+scale_matrix[0][0] + ", "+scale_matrix[0][1] + ", "+scale_matrix[0][2]);
					// Save values
//					SensorConfWriter.writeMagOffsetsAndScale(context, offsets, scale_matrix, true);
					
					float[] new_conv_B = SensorsConf.convertDoublesToFloats(offsets);
					float[] new_mul_A1 = new float[3];
					float[] new_mul_A2 = new float[3];
					float[] new_mul_A3 = new float[3];
					for (int i=0;i<3;i++){
						new_mul_A1[i] = (float) scale_matrix[0][i];
						new_mul_A2[i] = (float) scale_matrix[1][i];
						new_mul_A3[i] = (float) scale_matrix[2][i];
					}
					Log.d(TAG,"update_mulA_convB for MAGNETOMETER");
					conf.update_mulA_convB(SensorsConf.MAGNETOMETER, new_mul_A1, new_mul_A2, new_mul_A3, new_conv_B);
					Log.d(TAG,"updateFile");
					
					if (CompassSensorActivity.LOAD_ACC_OFFSET){
						/* Update accelerometer offset (if exist) */
						final String ACCCAL_FILE_LOC = "/sdcard/acc_cal/offsets.txt";
						try {
							BufferedReader br = new BufferedReader(new FileReader(ACCCAL_FILE_LOC));

							String line = br.readLine(); 

							br.close();
							String[] parts = line.split("\\s");

							float[] acc_offsets = new float[3];
							for(int i=0; i<parts.length; i++){
								Log.d(TAG,"Accel offset["+i+"] = "+ parts[i]);
								acc_offsets[i]=Float.parseFloat(parts[i]);
							}			

							Log.d(TAG,"Loaded accel offset file in "+ ACCCAL_FILE_LOC);
							Log.d(TAG,"update_convB for ACCELEROMETER");
							conf.update_convB(SensorsConf.ACCELEROMETER,  acc_offsets);

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
					}
					
					 /**
					  * This section of code calculates the accelerometer and gyroscope
					  *  bias using factory calibration data and writes them to 
					  *  /data/system/sensors.conf.
					  *
					  *	-Arguments to AccGyroCalibrator(boolean,boolean,boolean,boolean)
					  *	 are switches to select which calibration files will be used
					  *	 for accelerometer (arguments 1 and 2) and gyroscope (arguments
					  *	 3 and 4).
					  *
					  *	-Passing a value of true means that the corresponding
					  *	 calibration file will used.
					  *	
					  *	-For calibrating accelerometer, use arguments 1 and 2 to select
					  *	 mem-side data and cpu-side data, respectively.
					  *	
					  *	-For calibrating gyroscope, use arguments 3 and 4 to select
					  *	 mem-side data and cpu-side data, respectively.
					  */
					 
					AccGyroCalibrator accgyroCalib = new AccGyroCalibrator(true,true,true,false);
					
					// Check if the files sensors.conf, mem.csv and/or cpu.csv in factory
					//  partition exist.
					accgyroCalib.checkAvailabilityOfCalibrationFiles();
					
					// Continue further processing only if at least one of the sensors is
					//  desired to be both calibrated and has all the needed calibration
					//  files.
					if((accgyroCalib.accWillBeCalibrated && accgyroCalib.allFilesForAccCalibExist) 
						|| (accgyroCalib.gyroWillBeCalibrated && accgyroCalib.allFilesForGyroCalibExist)) {

						// Get the rotational matrix from sensors.conf, get its 
						//  inverse, then continue if successfull.
						if(accgyroCalib.getAndInverseAndStoreRotationMatrices()) {
						
							accgyroCalib.findAccAndGyroWindowWithMinVariance();
							
							// Calibrate accelerometer only if it is desired to
							//  be calibrated and all the needed calibration
							//  files are available.
							if (accgyroCalib.accWillBeCalibrated && accgyroCalib.allFilesForAccCalibExist) {
							
								// Calculate accelerometer bias.
								if (accgyroCalib.calculateAccelerometerBias()) {
					
									// Update SensorField for accelerometer
									//  (these values will be written to 
									//  sensors.conf upon calling 
									//  conf.updatefile(context)).
									if(conf.update_convB(SensorsConf.ACCELEROMETER,conf.convertDoublesToFloats(accgyroCalib.accConvB))) {
										if(VERBOSE) Log.d(TAG, "Finished updating SensorField for accelerometer and is ready for writing to sensors.conf");
									}
									else {
										Log.e(TAG, "Failed to update SensorField for accelerometer");
									}
								}
								else {
									Log.e(TAG, "--Discontinuing accelerometer calibration");
								}
							}
							else {
								if (!accgyroCalib.accWillBeCalibrated) {
									Log.e(TAG, "None of the csv calibration files were selected for accelerometer calibration");
									Log.e(TAG, "--Discontinuing accelerometer calibration");
								}
								if (!accgyroCalib.allFilesForAccCalibExist) {
									Log.e(TAG, "Not all the needed files for accelerometer calibration are available");
									Log.e(TAG, "--Discontinuing accelerometer calibration");
								}
							}
							
							// Calibrate gyroscope only if it is desired to
							//  be calibrated and all the needed calibration
							//  files are available.
							if (accgyroCalib.gyroWillBeCalibrated && accgyroCalib.allFilesForGyroCalibExist) {
							
								// Calculate gyroscope bias.
								if (accgyroCalib.calculateGyroscopeBias()) {
								
									// Update SensorField for gyroscope
									//  (these values will be written to 
									//  sensors.conf upon calling 
									//  conf.updatefile(context)).
									if(conf.update_convB(SensorsConf.GYROSCOPE,conf.convertDoublesToFloats(accgyroCalib.gyroConvB))) {
										if(VERBOSE) Log.d(TAG, "Finished updating SensorField for gyroscope and is ready for writing to sensors.conf");
									}
									else {
										Log.e(TAG, "Failed to update SensorField for gyroscope");
									}
								}
								else {
									Log.e(TAG, "--Discontinuing gyroscope calibration");
								}
							}
							else {
								if (!accgyroCalib.gyroWillBeCalibrated) {
									Log.e(TAG, "None of the csv calibration files were selected for gyroscope calibration");
									Log.e(TAG, "--Discontinuing gyroscope calibration");
								}
								if (!accgyroCalib.allFilesForGyroCalibExist) {
									Log.e(TAG, "Not all the needed files for gyroscope calibration are available");
									Log.e(TAG, "--Discontinuing gyroscope calibration");
								}
							}
						}
						else {
							Log.e(TAG, "--Discontinuing both accelerometer and gyroscope calibration");
						}
					}
					else {
						if ((accgyroCalib.accWillBeCalibrated && !accgyroCalib.allFilesForAccCalibExist) 
							&& (accgyroCalib.gyroWillBeCalibrated && !accgyroCalib.allFilesForGyroCalibExist)) {
							Log.e(TAG, "Neither of all the needed files for accelerometer nor gyroscope are all available");
						}
						else if (!accgyroCalib.accWillBeCalibrated && !accgyroCalib.gyroWillBeCalibrated) {
							Log.e(TAG, "Neither of the accelerometer and the gyroscope is desired to be calibrated");
						}
						Log.e(TAG, "--Discontinuing both accelerometer and gyroscope calibration");
					}
					
					conf.updateFile(context);
					
				}else{
					Log.e(TAG, "calibration error please try again ...");
					return false;
				}
			}
			
			
			optimizationComplete = true;
		}
		return true;

	}
}

