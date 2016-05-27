package com.reconinstruments.compass;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.String;
import java.lang.Integer;
import java.lang.Double;
import java.lang.Math;
import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.os.Build;
import android.util.Log;

public class AccGyroCalibrator {

	// This is to turn on and off logs for process information and results.
	private final boolean VERBOSE = true;

	// For prefix in logcat
	private static final String TAG = "AccGyroCalibrator";
	
	private static final int ACC_INDEX = 0;		// Array index for acc sensor
	private static final int GYRO_INDEX = 1;	// Array index for gyro sensor
	
	private static final int MEM_INDEX = 0;		// Array index for mem-side data
	private static final int CPU_INDEX = 1;		// Array index for cpu-side data
	
	// Pathname for sensors.conf file from where the rotation matrix for converting
	//  the accelerometer data from the head reference frame to the sensor reference
	//  frame will be taken from. Index [MEM_INDEX] is for mem-side sensors.conf and
	//  index [CPU_INDEX] is for cpu-side sensors.conf.
	private static String[] PATHNAME_FACTORY_SENSORSCONF;
	
	// Pathname for raw sensor calibration data from board in memory-side-up
	//  calibration orienation (PATHNAME_CSV[MEM_INDEX]) and CPU-side-up calibration
	//  orientation (PATHNAME_CSV[CPU_INDEX]).
	private static String[] PATHNAME_CSV;
	
	// Pathname for sensors.conf file from where the rotation matrix for converting
	//  the accelerometer data from the head reference frame to the sensor reference
	//  frame will be taken from. Index [MEM_INDEX] is for mem-side sensors.conf and
	//  index [CPU_INDEX] is for cpu-side sensors.conf. (Note: This is for testing
	//  test units that doesn't have factory calibration data).
	private static String[] PATHNAME_TEST_FACTORY_SENSORSCONF;
	
	// Pathname for raw sensor test calibration data from board in memory-side-up
	//  calibration orienation (PATHNAME_CSV[MEM_INDEX]) and CPU-side-up calibration
	//  orientation (PATHNAME_CSV[CPU_INDEX]). (Note: This is for testing test units
	//  that does not have factory calibration data).
	private static String[] PATHNAME_TEST_CSV;
	
	// Sampling frequency in Hz that was used in gathering acc and gyro calibration
	//  data.
	private static final int SAMP_FREQ = 50;
	
	// Window length of the accelerometer and gyro variance (in seconds) that will be
	// used to determine the stability of the acc and gyro calibration data.
	private static final int VARIANCE_WINDOW_LENGTH = 5;
	
	// The length of time (in seconds*10) in between calculation of the variance of
	//  the variance window.
	private static final int WINDOW_INCREMENT_LENGTH = 5;
	
	// Equivalent number of data points of VARIANCE_WINDOW_LENGTH. This is equal to
	//  VARIANCE_WINDOW_LENGTH*SAMP_FREQ;
	private int varianceWindowDataLength;
	
	// Minimum required stable accelerometer data for accelerometer calibration. This
	//  is equal to varianceWindowDataLength.
	private int requiredValidData;
	
	// Equivalent number of data points of WINDOW_INCREMENT_LENGTH. This is equal to
	//  WINDOW_INCREMENT_LENGTH*SAMP_FREQ/10;
	private int varianceIncrementDataLength;
	
	// Constant for earth's gravitational acceleration in m/s^2 in double data type.
	private static final double EARTH_GRAVITY_double = 9.80665;
	
	// Stability threshold for the variance of factory accelerometer data's norm
	private static final double ACC_NORM_VAR_STAB_THRESHOLD= 0.015;
	
	// Stability threshold for the variance of factory gyroscope data's norm
	private static final double GYRO_NORM_VAR_STAB_THRESHOLD = 0.00015;
	
	// Threshold for colinearity of board orientations during 2-sided calibration.
	// This is equal to [2*(1-assumed max increase in acc. scaling)*(9.80665)]^2.
	// Currently, assumed maximum increase in scaling is 10%.
	private static final double ORIENTATION_COLINEARITY_THRESHOLD = 311.592;
	
	// This arrays contains the csv column index of acc. and gyro data in the factory 
	//  csv files. Column index for gyro is in CSV_COL_INDEX[GYRO_INDEX][] and the
	//  column index for acc. is in CSV_COL_INDEX[MEM_INDEX][].
	private static int[][] CSV_COL_INDEX;
	
	// This contains the name of the sensors. "Acc." is in SENSOR_NAME[MEM_INDEX] and
	//  "Gyro" is in SENSOR_NAME[CPU_INDEX].
	private static String[] SENSOR_NAME;
	
	// This contains the name of the side used during calibration data gathering.
	//  CSV_SIDE_NAME[MEM_INDEX] contains "Mem" and CSV_SIDE_NAME[CPU_INDEX] contains
	//  "CPU".
	private static String[] CSV_SIDE_NAME;
	
	// These are arrays which stores the arguments that were passed to the constructor
	//  regarding which board-orientation was selected to be used for calibrating both
	//  accelerometer and gyroscope. A value of true means that the corresponding
	//  calibration board-orientation was selected to be used for the specific sensor. 
	//  [MEM_INDEX] and [CPU_INDEX] corresponds to the mem-side-orientation and cpu-
	//  side-orientation, respectively, during data gathering. useThisSideForAcc and
	//  useThisSideForGyro are for accelerometer and gyroscope, respectively.
	private final boolean[] useThisSideForAcc, useThisSideForGyro;
	
	// These variables tells if the specific sensor is desired to be calibrated. The
	//  value of these variables are true if at least one of the data-side-orientation
	//  was selected in useThisSideForAcc and useThisSideForGyro arrays.
	public boolean accWillBeCalibrated, gyroWillBeCalibrated;
	
	// This indicates the existence of sensors.conf file that was used during factory
	//  calibration after checking for it.
	private boolean[] factorySensorsConfExists;
	
	// This indicates the existence of mem.csv and cpu.csv factory calibration files
	//  afer checking for it. Element [MEM_INDEX] corresponds to mem.csv and element
	//  [CPU_INDEX] corresponds to cpu.csv.
	private boolean[] factoryCalibCSVExists;
	
	// This indicates that all the needed files for calibration for accelerometer and
	//  gyroscope exist. The values of these are based on the values of 
	//  useThisSideForAcc[] and useThisSideForGyro[];
	public boolean allFilesForAccCalibExist, allFilesForGyroCalibExist;
	
	// Temporarily stores the rotation matrix that was used to convert accelerometer
	//  data from sensor-ref-frame to head-ref-frame during calibration data gathering
	//  in factory. The value of this would be taken from sensors.conf
	//  in factory partition.
	private double[][][] rotationMatrix;
	
	// Temporarily stores the inverse of rotationMatrix to be able to convert
	//  factory accelerometer data back from head-ref-frame to sensor-ref-frame.
	private double[][][] invRotationMatrix;
	
	// This array indicates that there is enough data for the selected calibration
	//  board orientation/s for each of the sensors that is desired to be calibrated.
	//  Data is enough if at least the length of the corresponding calibration data
	//  is equal to requiredValidData. The first dimension of the array corresponds
	//  to the sensor, wherein [ACC_INDEX] and [GYRO_INDEX] corresponds to
	//  accelerometer and gyroscope, respectively, and the second dimension of the
	//  array corresponds to the calibration board-orientations, wherein [MEM_INDEX]
	//  and [CPU_INDEX] corresponds to mem-side and cpu-side orientations,
	//  respectively.
	private boolean[][] thereIsEnoughSensorData;
	
	// This stores the accelerometer and gyroscope norm's minimum window variance for
	//  both calibration board-orientations. The convention for indexing this array
	//  is the same as that in thereIsEnoughSensorData[][].
	private double[][] minSensorNormVar;
	
	// This stores the line number of the accelerometer and gyroscope norm's minimum
	//  window variance. The convention for indexing this aray is tha same as that in
	//  minSensorNormVar[][].
	private int[][] minSensorNormVarIndex;
	
	// This stores the average of accelerometer and gyroscopes data from each axis
	//  for both calibration board-orientations. The first and second and third
	//  dimensions corresponds to sensor, calibration board-orientation and sensor-
	//  axis, respectively.
	public double[][][] sensorAverageVector;
	
	// This stores the calculated accelerometer bias estimate.
	public double[] accBias;
	
	// This stores the final form of accelerometer bias that will be written to
	//  sensors.conf.
	public double[] accConvB;
	
	// This stores the index for the board orientation will be used for gyroscope
	//  calibration.
	private int dataSideForGyroBias;
	
	// This stores the calculated gyroscope bias estimate.
	public double[] gyroBias;
	
	// This stores the final form of gyroscope bias that will be written to 
	//  sensors.conf.
	public double[] gyroConvB;
	
	// Constructor
	public AccGyroCalibrator(boolean memDataForAcc, boolean cpuDataForAcc, boolean memDataForGyro, boolean cpuDataForGyro) {
		
		PATHNAME_FACTORY_SENSORSCONF = new String[2];
		PATHNAME_FACTORY_SENSORSCONF[MEM_INDEX] = "/factory/sensors/mem_sensors.conf";
		PATHNAME_FACTORY_SENSORSCONF[CPU_INDEX] = "/factory/sensors/cpu_sensors.conf";
		
		PATHNAME_CSV = new String[2];
		PATHNAME_CSV[MEM_INDEX] = "/factory/sensors/mem.csv";
		PATHNAME_CSV[CPU_INDEX] = "/factory/sensors/cpu.csv";
		
		PATHNAME_TEST_FACTORY_SENSORSCONF = new String[2];
		PATHNAME_TEST_FACTORY_SENSORSCONF[MEM_INDEX] = "/data/test/factory/mem_sensors.conf";
		PATHNAME_TEST_FACTORY_SENSORSCONF[CPU_INDEX] = "/data/test/factory/cpu_sensors.conf";
		
		PATHNAME_TEST_CSV = new String[2];
		PATHNAME_TEST_CSV[MEM_INDEX] = "/data/test/factory/mem.csv";
		PATHNAME_TEST_CSV[CPU_INDEX] = "/data/test/factory/cpu.csv";
		
		CSV_COL_INDEX = new int[2][3];
		CSV_COL_INDEX[ACC_INDEX][0] = 1;
		CSV_COL_INDEX[ACC_INDEX][1] = 2;
		CSV_COL_INDEX[ACC_INDEX][2] = 3;
		CSV_COL_INDEX[GYRO_INDEX][0] = 4;
		CSV_COL_INDEX[GYRO_INDEX][1] = 5;
		CSV_COL_INDEX[GYRO_INDEX][2] = 6;
		
		SENSOR_NAME = new String[2];
		SENSOR_NAME[ACC_INDEX] = "Acc.";
		SENSOR_NAME[GYRO_INDEX] = "Gyro";
		
		CSV_SIDE_NAME = new String[2];
		CSV_SIDE_NAME[MEM_INDEX] = "Mem";
		CSV_SIDE_NAME[CPU_INDEX] = "CPU";
		
		varianceWindowDataLength = VARIANCE_WINDOW_LENGTH*SAMP_FREQ;
		requiredValidData = varianceWindowDataLength;
		varianceIncrementDataLength = WINDOW_INCREMENT_LENGTH*SAMP_FREQ/10;
		
		factorySensorsConfExists = new boolean[2];
		factoryCalibCSVExists = new boolean[2];
		allFilesForAccCalibExist = false;
		allFilesForGyroCalibExist = false;
		
		rotationMatrix = new double[2][3][3];
		invRotationMatrix = new double[2][3][3];
		
		thereIsEnoughSensorData = new boolean[2][2];
		minSensorNormVar = new double[2][2];
		minSensorNormVarIndex = new int[2][2];
		sensorAverageVector = new double[2][2][3];
		
		useThisSideForAcc = new boolean[2];
		useThisSideForAcc[MEM_INDEX] = memDataForAcc;
		useThisSideForAcc[CPU_INDEX] = cpuDataForAcc;
		accWillBeCalibrated = (memDataForAcc||cpuDataForAcc) ? true: false;
		accBias = new double[3];
		accConvB = new double[3];
		
		useThisSideForGyro = new boolean[2];
		useThisSideForGyro[MEM_INDEX] = memDataForGyro;
		useThisSideForGyro[CPU_INDEX] = cpuDataForGyro;
		gyroWillBeCalibrated = (memDataForGyro||cpuDataForGyro) ? true: false;
		dataSideForGyroBias = 2; 	// 2 means it is not yet used
		gyroBias = new double[3];
		gyroConvB = new double[3];
		
	}
	
	// Check if the files /factory/sensors.conf, /factory/mem.csv and/or /factory/cpu.csv
	//  exist.
	//
	// **Note: Currently, for test purposes, if test data are in data/test/factory/,
	//  use test data. To remove such functionality, delete lines of code in between
	//  "//<T" and "//T>", and move the rest of the code out of the highest level 
	//  "else" condition statement with the "// Use factory data" comment.
	//
	public void checkAvailabilityOfCalibrationFiles() {
	//<T
		File file_TestSensorsConfMem = new File(PATHNAME_TEST_FACTORY_SENSORSCONF[MEM_INDEX]);
		File file_TestSensorsConfCPU = new File(PATHNAME_TEST_FACTORY_SENSORSCONF[CPU_INDEX]);
		File file_TestMem = new File(PATHNAME_TEST_CSV[MEM_INDEX]);
		File file_TestCPU = new File(PATHNAME_TEST_CSV[CPU_INDEX]);
		
		// If all of the test calibration data exist, use them instead of the
		//  factory calibration data. If they exist, overwrite the factory
		//  calibration file pathname variables with the pathnames of test 
		//  calibration files.
		if(file_TestSensorsConfMem.isFile() && file_TestSensorsConfCPU.isFile() 
			&& file_TestMem.isFile() && file_TestCPU.isFile()) {
		
			PATHNAME_FACTORY_SENSORSCONF[MEM_INDEX] = PATHNAME_TEST_FACTORY_SENSORSCONF[MEM_INDEX];
			PATHNAME_FACTORY_SENSORSCONF[CPU_INDEX] = PATHNAME_TEST_FACTORY_SENSORSCONF[CPU_INDEX];
			PATHNAME_CSV[MEM_INDEX] = PATHNAME_TEST_CSV[MEM_INDEX];
			PATHNAME_CSV[CPU_INDEX] = PATHNAME_TEST_CSV[CPU_INDEX];
			
			factorySensorsConfExists[MEM_INDEX] = true;
			factorySensorsConfExists[CPU_INDEX] = true;
			factoryCalibCSVExists[MEM_INDEX] = true;
			factoryCalibCSVExists[CPU_INDEX] = true;
			allFilesForGyroCalibExist = true;
			allFilesForAccCalibExist = true;
			
			if (VERBOSE) Log.d(TAG, "Will be using test calibration sensors.conf mem.csv and cpu.csv files");
		
		}
	//T>
		else {	// Use factory data
		
			boolean[] allFactoryCalibFilesExist = new boolean[2];
			
			File file_FactorySensorsConfMem = new File(PATHNAME_FACTORY_SENSORSCONF[MEM_INDEX]);
			File file_FactorySensorsConfCPU = new File(PATHNAME_FACTORY_SENSORSCONF[CPU_INDEX]);
			File file_MemCSV = new File(PATHNAME_CSV[MEM_INDEX]);
			File file_CPUCSV = new File(PATHNAME_CSV[CPU_INDEX]);
		
			factorySensorsConfExists[MEM_INDEX] = file_FactorySensorsConfMem.isFile();
			factorySensorsConfExists[CPU_INDEX] = file_FactorySensorsConfCPU.isFile();
			factoryCalibCSVExists[MEM_INDEX] = file_MemCSV.isFile();
			factoryCalibCSVExists[CPU_INDEX] = file_CPUCSV.isFile();
			
			allFactoryCalibFilesExist[MEM_INDEX] = factorySensorsConfExists[MEM_INDEX] 
									& factoryCalibCSVExists[MEM_INDEX];
			allFactoryCalibFilesExist[CPU_INDEX] = factorySensorsConfExists[CPU_INDEX] 
									& factoryCalibCSVExists[CPU_INDEX];
			
			// If all calib files for both sides exists.
			if (allFactoryCalibFilesExist[MEM_INDEX] 
					&& allFactoryCalibFilesExist[CPU_INDEX]) {
				
				if(VERBOSE) Log.d(TAG, "All calibration files for both mem-side and cpu-side exist");
				
				// This is true whichever the needed csv file is.
				allFilesForGyroCalibExist = true;
				allFilesForAccCalibExist = true;
			}
			
			// If only the calibration files from mem-side are all available.
			else if (allFactoryCalibFilesExist[MEM_INDEX] 
					&& !allFactoryCalibFilesExist[CPU_INDEX]) {
				
				if(VERBOSE) Log.d(TAG, "Only the calibration files from mem-side are all available");
				
				// For Gyro: only true if only mem-side is needed.
				if (useThisSideForGyro[MEM_INDEX] && !useThisSideForGyro[CPU_INDEX]) {
					allFilesForGyroCalibExist = true;
				}
				else {
					allFilesForGyroCalibExist = false;
				}
				// For Acc: only true if only mem-side is needed.
				if (useThisSideForAcc[MEM_INDEX] && !useThisSideForAcc[CPU_INDEX]) {
					allFilesForAccCalibExist = true;
				}
				else {
					allFilesForAccCalibExist = false;
				}
			}
			
			// If only the calibration files from cpu-side are all available
			else if (!allFactoryCalibFilesExist[MEM_INDEX] 
					&& allFactoryCalibFilesExist[CPU_INDEX]) {
				
				if(VERBOSE) Log.d(TAG, "Only the calibration files from cpu-side are all available");
				
				// For Gyro: only true if only cpu-side is needed.
				if (!useThisSideForGyro[MEM_INDEX] && useThisSideForGyro[CPU_INDEX]) {
					allFilesForGyroCalibExist = true;
				}
				else {
					allFilesForGyroCalibExist = false;
				}
				// For Acc: only true if only cpu-side is needed.
				if (!useThisSideForAcc[MEM_INDEX] && useThisSideForAcc[CPU_INDEX]) {
					allFilesForAccCalibExist = true;
				}
				else {
					allFilesForAccCalibExist = false;
				}
			}
			
			// If none of the calibration orientations have all the calibration files available.
			else {
				
				if(VERBOSE) Log.d(TAG, "None of the calibration orientations have all the calibration files available");
				
				allFilesForGyroCalibExist = false;
				allFilesForAccCalibExist = false;
			}
		}
	}
	
	// Get acceleration rotation matrix from accelerometer section of
	//  factory/sensors.conf, store it to rotationMatrix[][], calculate its inverse,
	//  then store it to invRotationMatrix[][].
	public boolean getAndInverseAndStoreRotationMatrices() {
	
		int dataSide = 0;
		int possibleCalibrationSides = 2;
		boolean[] matrixAvailableAndProcessed = new boolean[2];
	
		// Get store and inverse matrix for each of the sides if needed
		for(dataSide=0; dataSide<possibleCalibrationSides; dataSide++) {
		
			if ((useThisSideForAcc[dataSide] || useThisSideForGyro[dataSide]) && factorySensorsConfExists[dataSide]) {
			
				boolean inAccSection = false;
				boolean doneA,doneB,doneC;
				BufferedReader reader = null;
		
				doneA=doneB=doneC = false;
		
				try {
					reader = new BufferedReader(new FileReader(PATHNAME_FACTORY_SENSORSCONF[dataSide]));
					String line = null;
			
					while ((line = reader.readLine()) != null) {
						if (line.matches("(.*RI_SENSOR_HANDLE_ACCELEROMETER.*)")) {
							inAccSection = true;
						}
						if (inAccSection) {
							if (line.matches("^rot_[ABC].*"))
							{
								if (line.matches("^rot_A.*")) {
									String vals[] = line.split(" ");
									rotationMatrix[dataSide][0][0] = Double.parseDouble(vals[2]);
									rotationMatrix[dataSide][0][1] = Double.parseDouble(vals[3]);
									rotationMatrix[dataSide][0][2] = Double.parseDouble(vals[4]);
									doneA = true;
								}
								if (line.matches("^rot_B.*")) {
									String vals[] = line.split(" ");
									rotationMatrix[dataSide][1][0] = Double.parseDouble(vals[2]);
									rotationMatrix[dataSide][1][1] = Double.parseDouble(vals[3]);
									rotationMatrix[dataSide][1][2] = Double.parseDouble(vals[4]);
									doneB = true;
								}
								if (line.matches("^rot_C.*")) {
									String vals[] = line.split(" ");
									rotationMatrix[dataSide][2][0] = Double.parseDouble(vals[2]);
									rotationMatrix[dataSide][2][1] = Double.parseDouble(vals[3]);
									rotationMatrix[dataSide][2][2] = Double.parseDouble(vals[4]);
									doneC = true;
								}
								if (doneA && doneB && doneC) {
									if(VERBOSE) Log.d(TAG, "Rotation matrix for " 
										+ CSV_SIDE_NAME[dataSide] + "-side data:");
									if(VERBOSE) Log.d(TAG, "  rotA: " 
										+ Double.toString(rotationMatrix[dataSide][0][0]) + " " 
										+ Double.toString(rotationMatrix[dataSide][0][1]) + " " 
										+ Double.toString(rotationMatrix[dataSide][0][2]));
									if(VERBOSE) Log.d(TAG, "  rotB: " 
										+ Double.toString(rotationMatrix[dataSide][1][0]) + " " 
										+ Double.toString(rotationMatrix[dataSide][1][1]) + " " 
										+ Double.toString(rotationMatrix[dataSide][1][2]));
									if(VERBOSE) Log.d(TAG, "  rotC: " 
										+ Double.toString(rotationMatrix[dataSide][2][0]) + " " 
										+ Double.toString(rotationMatrix[dataSide][2][1]) + " " 
										+ Double.toString(rotationMatrix[dataSide][2][2]));
							
									// Inverse the matrix (note: for arotation matrix,
									//  its transpose is equal to its inverse)
									invRotationMatrix[dataSide][0][0] = rotationMatrix[dataSide][0][0];
									invRotationMatrix[dataSide][0][1] = rotationMatrix[dataSide][1][0];
									invRotationMatrix[dataSide][0][2] = rotationMatrix[dataSide][2][0];
									invRotationMatrix[dataSide][1][0] = rotationMatrix[dataSide][0][1];
									invRotationMatrix[dataSide][1][1] = rotationMatrix[dataSide][1][1];
									invRotationMatrix[dataSide][1][2] = rotationMatrix[dataSide][2][1];
									invRotationMatrix[dataSide][2][0] = rotationMatrix[dataSide][0][2];
									invRotationMatrix[dataSide][2][1] = rotationMatrix[dataSide][1][2];
									invRotationMatrix[dataSide][2][2] = rotationMatrix[dataSide][2][2];
							
									if(VERBOSE) Log.d(TAG, "Inverse of rotation matrix for " 
										+ CSV_SIDE_NAME[dataSide] + "-side data:");
									if(VERBOSE) Log.d(TAG, "  rotA: " 
										+ Double.toString(invRotationMatrix[dataSide][0][0]) + " " 
										+ Double.toString(invRotationMatrix[dataSide][0][1]) + " " 
										+ Double.toString(invRotationMatrix[dataSide][0][2]));
									if(VERBOSE) Log.d(TAG, "  rotB: " 
										+ Double.toString(invRotationMatrix[dataSide][1][0]) + " " 
										+ Double.toString(invRotationMatrix[dataSide][1][1]) + " " 
										+ Double.toString(invRotationMatrix[dataSide][1][2]));
									if(VERBOSE) Log.d(TAG, "  rotC: " 
										+ Double.toString(invRotationMatrix[dataSide][2][0]) + " " 
										+ Double.toString(invRotationMatrix[dataSide][2][1]) + " " 
										+ Double.toString(invRotationMatrix[dataSide][2][2]));
							
									matrixAvailableAndProcessed[dataSide] = true;
									break;
								} 
							}
						}
					}
					if (matrixAvailableAndProcessed[dataSide] == true) {
						continue;
					}

				} catch (FileNotFoundException e) {
					Log.e(TAG, "Could not find " + PATHNAME_FACTORY_SENSORSCONF[dataSide]);
				} catch (IOException e) {
					Log.e(TAG, "Could not read " + PATHNAME_FACTORY_SENSORSCONF[dataSide]);
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e2) {
							Log.e(TAG, "IOException closing BufferedReader");
						}
					}
				}
				Log.e(TAG, "Failed to get rotation matrix from " + PATHNAME_FACTORY_SENSORSCONF[dataSide]);
		
				matrixAvailableAndProcessed[dataSide] = false;
				continue;
			}
			else {
				continue;
			}
		}
		
		if ((useThisSideForAcc[MEM_INDEX] || useThisSideForGyro[MEM_INDEX]) 
			&& (useThisSideForAcc[CPU_INDEX] || useThisSideForGyro[CPU_INDEX])) {
			
			if (matrixAvailableAndProcessed[MEM_INDEX] 
				&& matrixAvailableAndProcessed[CPU_INDEX]) {
				return true;
			}
			else {
				return false;
			}
		}
		else if ((useThisSideForAcc[MEM_INDEX] || useThisSideForGyro[MEM_INDEX]) 
			&& !(useThisSideForAcc[CPU_INDEX] || useThisSideForGyro[CPU_INDEX])) {
			
			if (matrixAvailableAndProcessed[MEM_INDEX]) {
				return true;
			}
			else {
				return false;
			}
		}
		else if (!(useThisSideForAcc[MEM_INDEX] || useThisSideForGyro[MEM_INDEX]) 
			&& (useThisSideForAcc[CPU_INDEX] || useThisSideForGyro[CPU_INDEX])) {
			
			if (matrixAvailableAndProcessed[CPU_INDEX]) {
				return true;
			}
			else {
				return false;
			}
		}
		else {	// The program will not get into this part
			return false;
		}
	}

	// Find the window with minimum variance for both gyroscope and accelerometer.
	public void findAccAndGyroWindowWithMinVariance() {
	
		// If acc's mem-side will be used
		if (allFilesForAccCalibExist && useThisSideForAcc[MEM_INDEX]) {
			findWindowWithMinVariance(ACC_INDEX,MEM_INDEX);
		}
		// If acc's cpu-side will be used
		if (allFilesForAccCalibExist && useThisSideForAcc[CPU_INDEX]) {
			findWindowWithMinVariance(ACC_INDEX,CPU_INDEX);
		}
		// If gyro's mem-side will be used
		if (allFilesForGyroCalibExist && useThisSideForGyro[MEM_INDEX]) {
			findWindowWithMinVariance(GYRO_INDEX,MEM_INDEX);
		}
		// If gyro's cpu-side will be used
		if (allFilesForGyroCalibExist && useThisSideForGyro[CPU_INDEX]) {
			findWindowWithMinVariance(GYRO_INDEX,CPU_INDEX);
		}
	}
	
	// Find the minimum window variance from the selected sensor and board
	//  orientation side. This also checks if there is enough data to at least 
	//  calculate a window of data and store its results to
	//  thereIsEnoughSensorData[sensorIndex][dataSide].
	private void findWindowWithMinVariance(int sensorIndex, int dataSide) {
		double[] sensorTemp = new double[3];
		double sensorNorm;
		double tempSensorNormVar;
		int minSensorNormVarWindowIndex = 0;
		int totalCSVLines = 0;
		int totalWindows = 0;
		DescriptiveStatistics sensorStats = new DescriptiveStatistics();
		String[] nextLine;
		
		sensorStats.setWindowSize(varianceWindowDataLength);
	
		try {
			CSVReader csvReader = new CSVReader(new FileReader(PATHNAME_CSV[dataSide]));
			
			if(VERBOSE) Log.d(TAG,"Finding the window with minimum norm variance for " 
						+ CSV_SIDE_NAME[dataSide] + " side-data of " 
						+ SENSOR_NAME[sensorIndex] + " sensor:");
			
			while((nextLine = csvReader.readNext()) != null) {
				
				totalCSVLines++;
				
				// Parse the sensor data from the calibration csv file.
				sensorTemp[0] = Double.parseDouble(nextLine[CSV_COL_INDEX[sensorIndex][0]]);
				sensorTemp[1] = Double.parseDouble(nextLine[CSV_COL_INDEX[sensorIndex][1]]);
				sensorTemp[2] = Double.parseDouble(nextLine[CSV_COL_INDEX[sensorIndex][2]]);
			
				sensorNorm = Math.sqrt((sensorTemp[0]*sensorTemp[0])
						+ (sensorTemp[1]*sensorTemp[1])
						+ (sensorTemp[2]*sensorTemp[2]));
					
				// Update the window with the newly calculated norm.
				sensorStats.addValue(sensorNorm);
				
				// Calculate the moving variance of the sensor data. Only
				//  start calculating the variance when variance window 
				//  already gets filled.
				if ((totalCSVLines >= varianceWindowDataLength)
					 && ((totalCSVLines-varianceWindowDataLength) 
					 	% varianceIncrementDataLength == 0)) {
					 
					totalWindows++;
					if (totalWindows == 1) {
					
						minSensorNormVar[sensorIndex][dataSide] 
								= sensorStats.getVariance();
						minSensorNormVarWindowIndex = totalWindows;

						if(VERBOSE) Log.d(TAG,"   Sensor: " 
								+ SENSOR_NAME[sensorIndex] 
								+ " dataSide: " 
								+ CSV_SIDE_NAME[dataSide] 
								+ "totalCSVLines = " 
								+ Integer.toString(totalCSVLines) 
								+ " totalWindows = " 
								+ Integer.toString(totalWindows) 
								+ " variance = " 
								+ Double.toString(minSensorNormVar[sensorIndex][dataSide]));
					}
					else {
						tempSensorNormVar = sensorStats.getVariance();
						if (tempSensorNormVar < minSensorNormVar[sensorIndex][dataSide]) {
						
							minSensorNormVar[sensorIndex][dataSide] 
									= tempSensorNormVar;
							minSensorNormVarWindowIndex = totalWindows;
						}
						if(VERBOSE) Log.d(TAG,"   Sensor: " 
								+ SENSOR_NAME[sensorIndex] 
								+ " dataSide: " 
								+ CSV_SIDE_NAME[dataSide] 
								+ "totalCSVLines = " 
								+ Integer.toString(totalCSVLines) 
								+ " totalWindows = " 
								+ Integer.toString(totalWindows) 
								+ " variance = " + Double.toString(tempSensorNormVar));
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			Log.e(TAG, "CSVReader cannot find " + PATHNAME_CSV[dataSide]);
		} catch (IOException e) {
			Log.e(TAG, "CSVReader failed to read next line of " + PATHNAME_CSV[dataSide]);
		}
		
		// This condition checks if there was enough data to at least have one
		//  window for calculating variance.
		if (totalCSVLines >= requiredValidData) {
			
			thereIsEnoughSensorData[sensorIndex][dataSide] = true;
			
			// Calculate the equivalent data index of the determined minimum
			//  variance window index.
			minSensorNormVarIndex[sensorIndex][dataSide] = 
				((minSensorNormVarWindowIndex-1)*varianceIncrementDataLength)+1;
			
			if(VERBOSE) Log.d(TAG, "  Minimum window variance: " 
						+ Double.toString(minSensorNormVar[sensorIndex][dataSide]) 
						+ ". Start line of min. variance: " 
						+ Integer.toString(minSensorNormVarIndex[sensorIndex][dataSide]));
		}
		else {
			thereIsEnoughSensorData[sensorIndex][dataSide] = false;
		}
	}
	
	// Calculate bias of accelerometer.
	public boolean calculateAccelerometerBias() {
	
		if(VERBOSE) Log.d(TAG, "Calculating Accelerometer bias:");
	
		if (allFilesForAccCalibExist && isAccelerationCalibrationDataEnoughAndStable()) {
			
			// Calculate the average acceleration vector from mem-side if it
			//  will be used
			if(useThisSideForAcc[MEM_INDEX]) {
				calculateSensorAverageVector(ACC_INDEX,MEM_INDEX);
			}
			// Calculate the average acceleration vector from cpu-side if it
			//  will be used
			if(useThisSideForAcc[CPU_INDEX]) {
				calculateSensorAverageVector(ACC_INDEX,CPU_INDEX);
			}
			
			// Calculate the accelerometer bias if the board orientatins during
			//  calibration data gathering is acceptable.
			if (areBoardCalibOrientationsAcceptable()) {
			
				// If data from both sensor orientations will be used
				if(useThisSideForAcc[MEM_INDEX] && useThisSideForAcc[CPU_INDEX]) {	

					// Calculate the average of the combined accumulated values from 
					//  both mem side and cpu side orientations.
					accBias[0] = (sensorAverageVector[ACC_INDEX][MEM_INDEX][0]
							+ sensorAverageVector[ACC_INDEX][CPU_INDEX][0])/2;
					accBias[1] = (sensorAverageVector[ACC_INDEX][MEM_INDEX][1]
							+ sensorAverageVector[ACC_INDEX][CPU_INDEX][1])/2;
					// The gravity component in z-axis is automatically cancelled out 
					//  by averaging.
					accBias[2] = (sensorAverageVector[ACC_INDEX][MEM_INDEX][2]
							+ sensorAverageVector[ACC_INDEX][CPU_INDEX][2])/2;
			
				}
				// If data only from mem side orientation will be used.
				else if(useThisSideForAcc[MEM_INDEX] && !useThisSideForAcc[CPU_INDEX]){	

					// Calculate the average of the accumulated bias values from mem 
					//  side orientation.
					accBias[0] = sensorAverageVector[ACC_INDEX][MEM_INDEX][0];
					accBias[1] = sensorAverageVector[ACC_INDEX][MEM_INDEX][1];
					
					// Remove gravity component.
					accBias[2] = sensorAverageVector[ACC_INDEX][MEM_INDEX][2]
							- EARTH_GRAVITY_double;	
				}
				// If data only from cpu side orientation will be used.
				else if(!useThisSideForAcc[MEM_INDEX] && useThisSideForAcc[CPU_INDEX]) {	

					// Calculate the average of the accumulated bias values from cpu
					//  side orientation.
					accBias[0] = sensorAverageVector[ACC_INDEX][CPU_INDEX][0];
					accBias[1] = sensorAverageVector[ACC_INDEX][CPU_INDEX][1];
			
					// Remove gravity component.
					accBias[2] = sensorAverageVector[ACC_INDEX][CPU_INDEX][2]
							+ EARTH_GRAVITY_double;	
				}
				else {
		
				}

				if(VERBOSE) Log.d(TAG, "  Accelerometer bias estimate (m/s^2): x=" 
							+ Double.toString(accBias[0]) + " y=" 
							+ Double.toString(accBias[1]) + " z=" 
							+ Double.toString(accBias[2]));
			
				// Convert accBias into a form required by convB in sensors.conf
				accConvB[0] = -accBias[0];
				accConvB[1] = -accBias[1];
				accConvB[2] = -accBias[2];
		
				if(VERBOSE) Log.d(TAG, "  ConvB for accelerometer bias: " 
							+ Double.toString(accConvB[0]) + ", " 
							+ Double.toString(accConvB[1]) + ", " 
							+ Double.toString(accConvB[2]));
				
				return true;
			}
			else {
				// Logs for this will come from areBoardCalibOrientationsAcceptable()
				return false;
			}
		}
		else {
			// Logs for this will come from isAccelerationCalibrationDataEnoughAndStable()
			return false;
		}
	}
	
	
	// Determine if accelerometer factory calibration data for the desired board orientation/s
	//  is stable enough.
	private boolean isAccelerationCalibrationDataEnoughAndStable() {

		// If data from both sides will be used.
		if(useThisSideForAcc[MEM_INDEX] && useThisSideForAcc[CPU_INDEX]) {
		
			// If there is enough data for both sides
			if (thereIsEnoughSensorData[ACC_INDEX][MEM_INDEX] 
				&& thereIsEnoughSensorData[ACC_INDEX][CPU_INDEX]) {
			
				// If data from both sides are stable, return true.
				if ((minSensorNormVar[ACC_INDEX][MEM_INDEX] 
						<= ACC_NORM_VAR_STAB_THRESHOLD)  
					&& (minSensorNormVar[ACC_INDEX][CPU_INDEX] 
						<= ACC_NORM_VAR_STAB_THRESHOLD)) {
			
					if(VERBOSE) Log.d(TAG, "  Acc. data from both sides are within the threshold for stability");
					return true;
				}
				// If at least data from one of the sides is not stable,
				//  log which side is it and return false.
				else {
					if (!(minSensorNormVar[ACC_INDEX][MEM_INDEX] 
							<= ACC_NORM_VAR_STAB_THRESHOLD)) {
							
						Log.e(TAG, "Acc. data from mem-side-up orientation is not stable enough for calibration");
					}
					if (!(minSensorNormVar[ACC_INDEX][CPU_INDEX] 
							<= ACC_NORM_VAR_STAB_THRESHOLD)) {
							
						Log.e(TAG, "Acc. data from cpu-side-up orientation is not stable enough for calibration");
					}
					return false;
				}
			}
			// If there is no enough data for at least one of the sides,
			//  log which board-orientation does not have enough data and
			//  return false;
			else {
				if (!thereIsEnoughSensorData[ACC_INDEX][MEM_INDEX]) {
					Log.e(TAG, "Acc. data from mem-side-up orientation is not enough to perform calibration");
				}
				if (!thereIsEnoughSensorData[ACC_INDEX][CPU_INDEX]) {
					Log.e(TAG, "Acc. data from cpu-side-up orientation is not enough to perform calibration");
				}
				return false;
			}
			
		}
		// If data only from mem-side will be used.
		else if(useThisSideForAcc[MEM_INDEX] && !useThisSideForAcc[CPU_INDEX]) {
		
			// If data is enough from mem-side, check if it is stable and
			//  return true. If not stable, log and return false.
			if (thereIsEnoughSensorData[ACC_INDEX][MEM_INDEX]) {
			
				if (minSensorNormVar[ACC_INDEX][MEM_INDEX] 
						<= ACC_NORM_VAR_STAB_THRESHOLD) {
				
					if(VERBOSE) Log.d(TAG, "  Acc. data from mem-side-up orientation is within the threshold for stability");
					return true;
				}
				else {
					Log.e(TAG, "Acc data from mem-side-up orientation is not stable enough for calibration");
					return false;
				}
			}
			// Log and return false if data from mem-side is not enough.
			else {
				Log.e(TAG, "Acc. data from mem-side-up orientation is not enough to perform calibration");
				return false;
			}
		}
		// If data only from cpu-side will be used.
		else if(!useThisSideForAcc[MEM_INDEX] && useThisSideForAcc[CPU_INDEX]) {
		
			// If data is enough from cpu-side, check if it is stable and
			//  return true. If not stable, log and return false.
			if (thereIsEnoughSensorData[ACC_INDEX][CPU_INDEX]) {
			
				if (minSensorNormVar[ACC_INDEX][CPU_INDEX] 
						<= ACC_NORM_VAR_STAB_THRESHOLD) {
				
					if(VERBOSE) Log.d(TAG, "  Acc. data from cpu-side-up orientation is within the threshold for stability");
					return true;
				}
				else {
					Log.e(TAG, "Acc. data from cpu-side-up orientation is not stable enough for calibration");
					return false;
				}
			}
			else {
			// Log and return false if data from cpu-side is not enough.
				Log.e(TAG, "Acc. data from cpu-side-up orientation is not enough to perform calibration");
				return false;
			}
		}
		else {
			return false;
		}
	}

	// If data from both mem side and cpu side orientations will be used, check if 
	//  the orientations of the boards during the 2-sided test are colinear enough.
	// Note: 
	//	-The colinearity is measured by measuring the distance between the 
	//	 calculated acceleration vectors from both sides. This value should be
	//	 close to EARTH_GRAVITY*2
	//	-To avoid calculating square root and 
	private boolean areBoardCalibOrientationsAcceptable() {

		// Check if data from both orientations will be used.
		if(useThisSideForAcc[MEM_INDEX] && useThisSideForAcc[CPU_INDEX]) {	
			
			double[] vectorDist = new double[3];
			double twoSideVectorDistSquared;
			
			vectorDist[0] = sensorAverageVector[ACC_INDEX][MEM_INDEX][0]
					- sensorAverageVector[ACC_INDEX][CPU_INDEX][0];
			vectorDist[1] = sensorAverageVector[ACC_INDEX][MEM_INDEX][1]
					- sensorAverageVector[ACC_INDEX][CPU_INDEX][1];
			vectorDist[2] = sensorAverageVector[ACC_INDEX][MEM_INDEX][2]
					- sensorAverageVector[ACC_INDEX][CPU_INDEX][2];
			
			twoSideVectorDistSquared = (vectorDist[0]*vectorDist[0])
				+(vectorDist[1]*vectorDist[1])+(vectorDist[2]*vectorDist[2]);
			
			if(twoSideVectorDistSquared >= ORIENTATION_COLINEARITY_THRESHOLD) {
				Log.d(TAG, "  The alignments of the board during the 2-sided factory accelerometer calibration are acceptable");
				return true;
			}
			else {
				Log.e(TAG, "The calculated square of distance between the two vectors during the 2-sided factory acc. calibration of " 
					+ Double.toString(twoSideVectorDistSquared) 
					+ " is less than the min required value of " 
					+ Double.toString(ORIENTATION_COLINEARITY_THRESHOLD)
					+ ". Accelerometer calibration should not be continued because there is a risk of increasing offset error.");
				return false;
			}
		}
		// Return true and do not check for bias difference if accelerometer data 
		//  only from one side will be used.
		else {	
			return true;
		}
	}
	
	// Calculate bias of gyroscope.
	public boolean calculateGyroscopeBias() {
	
		if(VERBOSE) Log.d(TAG, "Calculating Gyroscope bias:");
	
		// Calculate bias only if there is available valid gyro data.
		if (determineDataSideForGyroBias()) {
		
			// Calculate the average gyroscope vector from selected data-side
			//  for calbration
			calculateSensorAverageVector(GYRO_INDEX,dataSideForGyroBias);
		
			gyroBias[0] = sensorAverageVector[GYRO_INDEX][dataSideForGyroBias][0];
			gyroBias[1] = sensorAverageVector[GYRO_INDEX][dataSideForGyroBias][1];
			gyroBias[2] = sensorAverageVector[GYRO_INDEX][dataSideForGyroBias][2];

			if(VERBOSE) Log.d(TAG, "  Gyroscope bias estimate (rad/s): x=" 
						+ Double.toString(gyroBias[0]) + " y=" 
						+ Double.toString(gyroBias[1]) + " z=" 
						+ Double.toString(gyroBias[2]));
			
			// Convert gyroBias into a form required by convB in sensors.conf
			gyroConvB[0] = -gyroBias[0];
			gyroConvB[1] = -gyroBias[1];
			gyroConvB[2] = -gyroBias[2];
			
			if(VERBOSE) Log.d(TAG, "  ConvB for gyroscope bias (rad/s): x=" 
						+ Double.toString(gyroConvB[0]) + " y=" 
						+ Double.toString(gyroConvB[1]) + " z=" 
						+ Double.toString(gyroConvB[2]));
			
			return true;
		}
		else {
			Log.e(TAG, "Cannot perform gyroscope calibration since gyroscope calibration data is not enough and/or stable");
			return false;
		}
	}
	
	// This method determines which side of data will be used for calculating gyroscope bias.
	// This returns true if there is a valid gyro data that could be used.
	private boolean determineDataSideForGyroBias() {
		boolean[] sideWasChosenAndIsEnoughAndStable = new boolean[2];
		
		sideWasChosenAndIsEnoughAndStable[MEM_INDEX] = (useThisSideForGyro[MEM_INDEX] 
				&& thereIsEnoughSensorData[GYRO_INDEX][MEM_INDEX] 
				&& (minSensorNormVar[GYRO_INDEX][MEM_INDEX] 
					<= GYRO_NORM_VAR_STAB_THRESHOLD)) ? true : false;
				
		sideWasChosenAndIsEnoughAndStable[CPU_INDEX] = (useThisSideForGyro[CPU_INDEX] 
				&& thereIsEnoughSensorData[GYRO_INDEX][CPU_INDEX] 
				&& (minSensorNormVar[GYRO_INDEX][CPU_INDEX] 
					<= GYRO_NORM_VAR_STAB_THRESHOLD)) ? true : false;

		// If data from both sides will be used, are both enough and stable, use
		//  side with the smaller gyro norm variance.
		if(sideWasChosenAndIsEnoughAndStable[MEM_INDEX] 
				&& sideWasChosenAndIsEnoughAndStable[CPU_INDEX]) {
		
			dataSideForGyroBias = 
				(minSensorNormVar[GYRO_INDEX][MEM_INDEX] 
					< minSensorNormVar[GYRO_INDEX][CPU_INDEX]) ? MEM_INDEX : CPU_INDEX;
			return true;
		}
		// If data only from mem-side satisfies all the requirements.
		else if(sideWasChosenAndIsEnoughAndStable[MEM_INDEX] 
				&& !sideWasChosenAndIsEnoughAndStable[CPU_INDEX]) {
		
			dataSideForGyroBias = MEM_INDEX;
			return true;
		}
		// If data only from cpu-side satisfies all the requirements.
		else if(!sideWasChosenAndIsEnoughAndStable[MEM_INDEX] 
				&& sideWasChosenAndIsEnoughAndStable[CPU_INDEX]) {
		
			dataSideForGyroBias = CPU_INDEX;
			return true;
		}
		else {
			return false;
		}
	}
	
	// ++
	private void calculateSensorAverageVector(int sensorIndex, int dataSide) {
		double[] sensorTemp = new double[3];
		double[] sensorVectorSum = new double[3];
		int totalAccumSensorData = 0;
		int lineCtr = 0;
		String[] nextLine;
	
		if(VERBOSE) Log.d(TAG, "  Calculating average vector for " + CSV_SIDE_NAME[dataSide] 
						+ " data-side of " + SENSOR_NAME[sensorIndex] + " sensor");
	
		try {
			CSVReader csvReader = new CSVReader(new FileReader(PATHNAME_CSV[dataSide]));
		
			while((nextLine = csvReader.readNext()) != null) {
			
				lineCtr++;
				if (lineCtr >= minSensorNormVarIndex[sensorIndex][dataSide]) {
				
					// Parse the sensor data from the calibration csv file.
					sensorTemp[0] = Double.parseDouble(nextLine[CSV_COL_INDEX[sensorIndex][0]]);
					sensorTemp[1] = Double.parseDouble(nextLine[CSV_COL_INDEX[sensorIndex][1]]);
					sensorTemp[2] = Double.parseDouble(nextLine[CSV_COL_INDEX[sensorIndex][2]]);
					
					convertFromHeadToSensorRefFrame(sensorTemp, dataSide);
					
					// Accumulate sensor readings
					sensorVectorSum[0] += sensorTemp[0];
					sensorVectorSum[1] += sensorTemp[1];
					sensorVectorSum[2] += sensorTemp[2];
					totalAccumSensorData++;
					
					if (totalAccumSensorData == requiredValidData) break;
				}
			}	

		} catch (FileNotFoundException e) {
			Log.e(TAG, "CSVReader cannot find " 
				+ PATHNAME_CSV[dataSide]);
				
		} catch (IOException e) {
			Log.e(TAG, "CSVReader failed to read next line of " 
				+ PATHNAME_CSV[dataSide]);
		}

		// Calculate the average of the accumulated sensor vector
		sensorAverageVector[sensorIndex][dataSide][0] = sensorVectorSum[0]/totalAccumSensorData;
		sensorAverageVector[sensorIndex][dataSide][1] = sensorVectorSum[1]/totalAccumSensorData;
		sensorAverageVector[sensorIndex][dataSide][2] = sensorVectorSum[2]/totalAccumSensorData;

		if (VERBOSE) Log.d(TAG, "    Total data used for calculating the average vector " 
				+ Integer.toString(totalAccumSensorData));	
	}
	
	// A method to convert either the accelerometer or gyroscope data from
	//  head-reference-frame to sensor-reference-frame.
	private void convertFromHeadToSensorRefFrame(double[] sensorData, int dataSide) {
		double[] sensorTemp = new double[3];
		
		sensorTemp[0] = sensorData[0];
		sensorTemp[1] = sensorData[1];
		sensorTemp[2] = sensorData[2];
		
		sensorData[0] = invRotationMatrix[dataSide][0][0]*sensorTemp[0] 
					+ invRotationMatrix[dataSide][0][1]*sensorTemp[1] 
					+ invRotationMatrix[dataSide][0][2]*sensorTemp[2];
		sensorData[1] = invRotationMatrix[dataSide][1][0]*sensorTemp[0] 
					+ invRotationMatrix[dataSide][1][1]*sensorTemp[1] 
					+ invRotationMatrix[dataSide][1][2]*sensorTemp[2];
		sensorData[2] = invRotationMatrix[dataSide][2][0]*sensorTemp[0] 
					+ invRotationMatrix[dataSide][2][1]*sensorTemp[1] 
					+ invRotationMatrix[dataSide][2][2]*sensorTemp[2];
	}
	
	
}

