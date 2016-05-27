package com.reconinstruments.imucompfil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DecimalFormat;

import android.app.Activity;
import android.hardware.RISensor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class IMUCompFilterActivity extends Activity implements SensorEventListener {

	private static final String TAG = "DarrellActivity";
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mGyroscope;
	private Sensor mMagnetic;
	private Sensor mSynchro;
	private float accX, accY, accZ, gyroX, gyroY, gyroZ, magX, magY, magZ;
	private float accXold=0,accYold=0,accZold = 0;
	private String accX_str, accY_str, accZ_str, gyroX_str, gyroY_str, gyroZ_str, magX_str, magY_str, magZ_str;
	private String comp_roll_str, comp_pitch_str, comp_yaw_str;	//formatted complementary data in string
	private String kal_roll_str, kal_pitch_str, kal_yaw_str;	//formatted kalman data in string

	// Kalman filter variables ////////////////////////
	private Boolean firstIteration;
//	private double deltaT; // sampling rate
	private double sigma_g_square = 1.0e-4; // variance of gyro output
	private double sigma_q_square = 1.0e-5; // variance of quaternion (by trial and error)
	private double rho = 1.0; // magnetometer weight

	private int i, j;
	//private double pi;
	//private int data, data_number = 1;
	// char *pstring, filename[20],cat;
	//private double time; // current time
	private double ya[], yw[], ym[]; // sensor measurements (Don't need, use actual measurement)
	private double ya_norm, ym_norm; // norm of ya and ym
	private double ywprev[]; // previous yw
	private double bw[]; // gyro bias
	private double q[]; // target value quaternion to be solved
	private double qprev[]; // previous quaternion
	private double q_measured[]; // measured quaternion
	private double qpriori[]; // a priori state estimate (integrated quaternion)
	private double rotationMatrix[][]; // rotation matrix from q
	private double Rpriori[][]; // rotation matrix from qpriori
	private double Rreset[][]; // initial R according to the reset option
	private double EulerAng[];
	private double nfg[], nfm[]; // fixed reference expressed by navigation frame
	private double bfg[], bfm[]; // fixed reference expressed by body frame
	private double bsg[], bsm[]; // sensor measurement vectors expressed by body frame
	private double g_norm, m_norm; // norm of initial g(ya) and m(ym)
	private double deltapi[]; // virtual rotation
	private double deltaq[]; // quaternion variation for Gauss-Newton
	private double C[][], invC[][]; // constant matrix in system matrix of G-N
	private double Phi[][]; // transition matrix
	private double Q[][]; // process noise covariance matrix
	private double K[][]; // Kalman gain
	private double covR[][]; // measurement noise covariance matrix
	private double Pprev[][]; // a posteriori error covariance matrix of the previous time-step
	private double Ppriori[][]; // a priori error covariance matrix of the previous time-step
	private double omega[][]; // function of angular velocity
	private double Xi[][]; // function of quaternion
	private double residual[];
	private double covResidual[][];
	private double norm; // vector norm for common use
	private double I33[][]; // 3 by 3 identity matrix
	private double I44[][]; // 4 by 4 identity matrix
	private double temp1Vec3[], temp2Vec3[], temp3Vec3[], temp4Vec3[];
	private double temp1Vec4[], temp1Mat44[][], temp2Mat44[][], temp3Mat44[][];
	private double temp1Mat33[][], temp2Mat33[][];
	//////////////////////////////////////////////////////////////////////
	
	// Velocity ////////////////////////////////
	double rotationMatrixTrans[][]=new double [3][3];
	double AccelerationEarthFrame []= new double [3]; 
	double VelocityEarthFrame []= new double [3]; 
	double PositionEarthFrame []=new double [3];
	double AccelerationBodyFrame [] = new double [3];
	
	// gyroscope roll,pitch,yaw //////////////////////
	double gyroRoll, gyroPitch, gyroYaw;
	double initialCompYaw;	//initial complementary yaw to align complementary yaw with kalman yaw
	int nGyro;
	float[] gyroBiasXarray;
	float[] gyroBiasYarray;
	float[] gyroBiasZarray;
	float gyroBiasX,accBiasX,accBiasY,accBiasZ;
	float gyroBiasY;
	float gyroBiasZ;
	double currentTimestamp,prevTimestamp, dT, startTimestamp; // timestamp of gyro sensor data,
											// delta time of
	// gyro
	private static final double NS2S = 1.0f / 1000000000.0f;
	private static final double MS2S = 1.0f / 1000.0f;
	double comp_roll, comp_pitch, comp_yaw;
	Boolean logData;
	File file;
	FileOutputStream f;
	Writer pw;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		//mSynchro = mSensorManager.getDefaultSensor(RISensor.RI_SENSOR_TYPE_SYNCHRONIZED);
		registerSensorListener();

		initVariables();
		Log.i(TAG,"Started");
		}

	public void myClickHandler(View view) {

		Button button1;

		button1 = (Button) findViewById(R.id.button1);


		if (logData == false) {
			logData = true;
			startTimestamp = prevTimestamp;
			try {
				// Find the root of the external storage.
				File root = android.os.Environment.getExternalStorageDirectory();				
				File dir = new File(root.getAbsolutePath() + "/Recon Sensors");
				dir.mkdirs();
				file = new File(dir, "imuData"+String.valueOf(System.currentTimeMillis())+".txt");
				pw = new BufferedWriter(new FileWriter(file));
				Log.i(TAG,"I'm logging");
				String dataline = "Start Of Logging: Acc and Giro \n\r";
				pw.write(dataline);
				//Log.i(TAG,"I'm logging the stuff");
				pw.flush();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			Toast.makeText(getApplicationContext(), "Data logging started.", Toast.LENGTH_SHORT).show();
			button1.setText("Stop data logging");

		} else {
			logData = false;

			try {
				//pw.flush();
				pw.close();
				Log.i(TAG,"I stopped");
				//f.close();
			} catch (Exception e) {
				e.printStackTrace();

			}
			Toast.makeText(getApplicationContext(), "Data logging stopped.", Toast.LENGTH_SHORT).show();
			button1.setText("Start data logging");
		}
	}

	private void registerSensorListener() {

		
		mSensorManager.registerListener(this, mAccelerometer, (int)10000);
		mSensorManager.registerListener(this, mGyroscope, (int)10000);
		mSensorManager.registerListener(this, mMagnetic, (int)10000);
		
		//mSensorManager.registerListener(this, mSynchro, SensorManager.SENSOR_DELAY_FASTEST); //(int)10000 =100Hz in microsecond

	}

	private void unregisterSensorListener() {
		mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerSensorListener();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterSensorListener();
		
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		//currentTimestamp = SystemClock.elapsedRealtime()*MS2S;
		Sensor sensor = event.sensor;
		
		//////////////////////////////////////////////////////////////////////////////
		///////////// Recon sensor framework calling method///////////////////////////
	
		if (sensor.getType() == RISensor.RI_SENSOR_TYPE_SYNCHRONIZED) {
			//currentTimestamp = (event.timestamp*NS2S);
			
			Log.i(TAG,"ReconSensor");
			if (firstIteration == true) {
				startTimestamp = (event.timestamp *NS2S);
				prevTimestamp = startTimestamp;
			}
			currentTimestamp = ( event.timestamp*NS2S) - startTimestamp;
			
			dT = (currentTimestamp - prevTimestamp);
			prevTimestamp = currentTimestamp;
			
		/////////////////////////////// accelerometer ///////////////////////////
			TextView tvX, tvY, tvZ;

			tvX = (TextView) findViewById(R.id.acc_x_axis);
			tvY = (TextView) findViewById(R.id.acc_y_axis);
			tvZ = (TextView) findViewById(R.id.acc_z_axis);

			//mapping for goggles
			accX = -event.values[1];
			accY = event.values[2];
			accZ = -event.values[0];
						
			accX_str = double2str(accX);
			accY_str = double2str(accY);
			accZ_str = double2str(accZ);
			tvX.setText(accX_str);
			tvY.setText(accY_str);
			tvZ.setText(accZ_str);
			
			ya[0] = accX;
			ya[1] = accY;
			ya[2] = accZ;

		
			/////////////////////////////// magnetometer ///////////////////////////
			TextView tvX1, tvY1, tvZ1;

			magX = event.values[3];
			magY = event.values[4];
			magZ = event.values[5];
			magX_str = double2str(magX);
			magY_str = double2str(magY);
			magZ_str = double2str(magZ);
		
			ym[0] = magX;
			ym[1] = magY;
			ym[2] = magZ;

			/////////////////////////////// gyroscope ///////////////////////////
		
//			dT = (currentTimestamp - prevTimestamp) * NS2S;
			
			
			TextView tvX2, tvY2, tvZ2;
			
			tvX2 = (TextView) findViewById(R.id.Giro_X);
			tvY2 = (TextView) findViewById(R.id.Giro_Y);
			tvZ2 = (TextView) findViewById(R.id.Giro_Z);
			
			gyroX = event.values[10];
			gyroY = event.values[11];
			gyroZ = -event.values[9];
			
			if (firstIteration == true) {
			//	prevTimestamp = currentTimestamp;
				gyroBiasX = gyroX;
				gyroBiasY = gyroY;
				gyroBiasZ = gyroZ;
			}
			
			//gyroX-=gyroBiasX; gyroY-=gyroBiasY; gyroZ-=gyroBiasZ;	
			
			gyroX_str = double2str(gyroX);
			gyroY_str = double2str(gyroY);
			gyroZ_str = double2str(gyroZ);
			tvX2.setText(gyroX_str);
			tvY2.setText(gyroY_str);
			tvZ2.setText(gyroZ_str);


			yw[0] = gyroX;	//kalman data measurement
			yw[1] = gyroY;
			yw[2] = gyroZ;

//			if (ya[0] != 0 && ym[0] != 0) {		//calling the filters
//				complementaryFilter();
//				kalmanFilter();
//				
//			}
			
			
			
			if (logData == true) 
				writeDataToFile();
			firstIteration=false;	//enable when disabling kalman filter
		
			
		
		}
		
		///////////////////////////////////////////////////////////////////////////////
		///////////// Standard Android sensor calling method///////////////////////////
		

		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			TextView tvX, tvY, tvZ;

			Log.i(TAG,"Android sensor");
			tvX = (TextView) findViewById(R.id.acc_x_axis);
			tvY = (TextView) findViewById(R.id.acc_y_axis);
			tvZ = (TextView) findViewById(R.id.acc_z_axis);

			accX = event.values[0];
			accY = event.values[1];
			accZ = event.values[2];
			

//			if (firstIteration == true) {
//				//	prevTimestamp = currentTimestamp;
//					accBiasX = -0.6f;
//					accBiasY = -0.3f;
//					accBiasZ = accZ;
//				}
//			else{
//				accX-=accBiasX;
//				accY-=accBiasY;
////				accZ-=accBiasZ;
//			}
			
			accX_str = double2str(accX);
			accY_str = double2str(accY);
			accZ_str = double2str(accZ);
			tvX.setText(accX_str);
			tvY.setText(accY_str);
			tvZ.setText(accZ_str);
			
			ya[0] = accX;
			ya[1] = accY;
			ya[2] = accZ;

		}  else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			TextView tvX, tvY, tvZ;

			magX = event.values[0];
			magY = event.values[1];
			magZ = event.values[2];
			magX_str = double2str(magX);
			magY_str = double2str(magY);
			magZ_str = double2str(magZ);

			ym[0] = magX;
			ym[1] = magY;
			ym[2] = magZ;

		} else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			
			if (firstIteration == true) {
				startTimestamp = (event.timestamp *NS2S);
				prevTimestamp = startTimestamp;
			}
			currentTimestamp = ( event.timestamp*NS2S) - startTimestamp;
			
			dT = (currentTimestamp - prevTimestamp);
			prevTimestamp = currentTimestamp;
			TextView tvX2, tvY2, tvZ2;
			tvX2 = (TextView) findViewById(R.id.Giro_X);
			tvY2 = (TextView) findViewById(R.id.Giro_Y);
			tvZ2 = (TextView) findViewById(R.id.Giro_Z);
			
			gyroX = event.values[0];
			gyroY = event.values[1];
			gyroZ = event.values[2];
			
			gyroX_str = double2str(gyroX);
			gyroY_str = double2str(gyroY);
			gyroZ_str = double2str(gyroZ);
			
			tvX2.setText(gyroX_str);
			tvY2.setText(gyroY_str);
			tvZ2.setText(gyroZ_str);
			
			
			if (firstIteration == true) {
				prevTimestamp = currentTimestamp;
//				gyroBiasX = gyroX;
//				gyroBiasY = gyroY;
//				gyroBiasZ = gyroZ;
			}
			
			gyroX-=gyroBiasX; gyroY-=gyroBiasY; gyroZ-=gyroBiasZ;	
			
			gyroX_str = double2str(gyroX);
			gyroY_str = double2str(gyroY);
			gyroZ_str = double2str(gyroZ);	

			yw[0] = gyroX;	//kalman data measurement
			yw[1] = gyroY;
			yw[2] = gyroZ;

			if (ya[0] != 0 && ym[0] != 0) {		//calling the filters
				//complementaryFilter();
				//kalmanFilter();
				
			}
			
			

			if (logData == true){
				writeDataToFile();
//				velocityUpdate();
			}
		//	firstIteration=false;
			
		}
		
		
	}
	private void velocityUpdate(){
//		AccelerationBodyFrame[0]  = accX; AccelerationBodyFrame[1] =accY;AccelerationBodyFrame[2] = accZ;
		

		////// Low pass filer for accelerometer to smooth out data
//		float accFilterConst = 0.9f;
//		accX = ((accXold *accFilterConst) + (1-accFilterConst)*accX);
//		accY = ((accYold *accFilterConst) + (1-accFilterConst)*accY);
//		accZ = ((accZold *accFilterConst) + (1-accFilterConst)*accZ);
//		accXold = accX;
//		accYold = accY;
//		accZold = accZ;
		
//		double gravityEarthFrame [] = {-0.5678,-0.2691,9.81f};	//accelerations due to gravity at initial position
//		
//		double gravityBodyFrame[] = new double [3];
		
//		gravityBodyFrame = KalmanHelper.ats(rotationMatrix,gravityEarthFrame);	//transform the initial gravity to the body frame
		
		AccelerationBodyFrame[0]  = accX ;//-gravityBodyFrame[0];	//remove the gravity component from body acceleration
		AccelerationBodyFrame[1]  = accY ;//-gravityBodyFrame[1];
		AccelerationBodyFrame[2]  = accZ ;//-gravityBodyFrame[2];
		
		//// Discrimination window
//		if(AccelerationBodyFrame[0]<-0.1 || AccelerationBodyFrame[0] >0.1)
//			AccelerationBodyFrame[0]  = accX;
//		else
//			AccelerationBodyFrame[0]  = 0;
//			AccelerationBodyFrame[1]  = accY;
//		else
//			AccelerationBodyFrame[1]  = 0;
//		if(accZ<-0.1 || accZ >0.1)
//			AccelerationBodyFrame[2]  = accZ;
//		else
//			AccelerationBodyFrame[2]  = 0;
		
		
		
		rotationMatrixTrans = KalmanHelper.transposeMatrix(rotationMatrix);		//get the inverse of the rotation matrix
		AccelerationEarthFrame = KalmanHelper.ats(rotationMatrixTrans,AccelerationBodyFrame); //transform the body acceleration to earth frame		
		
		double velocityChange,positionChange;
		
		velocityChange = (AccelerationEarthFrame[0]*dT);
		VelocityEarthFrame[0]+=velocityChange;
		
		velocityChange = (AccelerationEarthFrame[1]*dT);
		VelocityEarthFrame[1]+=velocityChange;
		
		velocityChange = AccelerationEarthFrame[2]*dT;
		VelocityEarthFrame[2]+=velocityChange;

		PositionEarthFrame[0] += (VelocityEarthFrame[0]*dT);
		PositionEarthFrame[1] += (VelocityEarthFrame[1]*dT);
		PositionEarthFrame[2] += (VelocityEarthFrame[2]*dT);
		
		
		String dataline;
		dataline = double2str(currentTimestamp) + " , " + double2str(accX) + " , " + double2str(accY) + " , " + double2str(accZ)
		+ " , " + double2str(AccelerationEarthFrame[0])+ " , " + double2str(AccelerationEarthFrame[1]) + " , " + double2str(AccelerationEarthFrame[2]);
		Log.i(TAG,dataline);
		
	}
	
	
	
	
	
	private void writeDataToFile(){
		String dataline;
		dataline = "Acc," + accX_str + "," + accY_str+ "," + accZ_str + ",Giro," + gyroX_str + "," + gyroY_str + "," + gyroZ_str;
		try{
		//pw = new BufferedWriter(new FileWriter(file));
		pw.write(dataline);
		pw.write("\n\r");
		//Log.i(TAG,"I'm logging the stuff");
		pw.flush();
		//pw.close();
		}
		catch(Exception e)
		{}
	}

	private void initVariables() {
		firstIteration = true;
		prevTimestamp=0;
		gyroRoll = 0;
		gyroPitch = 0;
		gyroYaw = 0;
		comp_roll = 0;
		comp_pitch = 0;
		comp_yaw = 0;
		logData = false;
		nGyro = 0;
		gyroBiasXarray = new float[1000];
		gyroBiasYarray = new float[1000];
		gyroBiasZarray = new float[1000];
		
		// //// Kalman filter variables /////////////////////
//		deltaT = 0.01; // sampling rate
		sigma_g_square = 1.0e-4; // variance of gyro output
		sigma_q_square = 1.0e-5; // variance of quaternion (by trial and error)
		rho = 1.0; // magnetometer weight

		//pi = Math.acos(-1.0);
		//data_number = 1;
		ya = new double[3];// sensor measurements
		yw = new double[3];
		ym = new double[3];

		ywprev = new double[3];// previous yw
		bw = new double[3];// gyro bias
		q = new double[4]; // target value quaternion to be solved
		qprev = new double[4];// previous quaternion
		q_measured = new double[4]; // measured quaternion
		qpriori = new double[4];// a priori state estimate (integrated quaternion)
		rotationMatrix = new double[3][3]; // rotation matrix from q
		Rpriori = new double[3][3]; // rotation matrix from qpriori
		Rreset = new double[3][3]; // initial R according to the reset option
		EulerAng = new double[3];
		nfg = new double[3]; // fixed reference expressed by navigation frame
		nfm = new double[3];
		bfg = new double[3]; // fixed reference expressed by body frame
		bfm = new double[3];
		bsg = new double[3]; // sensor measurement vectors expressed by body
								// frame
		bsm = new double[3];
		deltapi = new double[3]; // virtual rotation
		deltaq = new double[4]; // quaternion variation for Gauss-Newton
		C = new double[3][3]; // constant matrix in system matrix of G-N
		invC = new double[3][3];
		Phi = new double[4][4];// transition matrix
		Q = new double[4][4]; // process noise covariance matrix
		K = new double[4][4]; // Kalman gain
		covR = new double[4][4]; // measurement noise covariance matrix
		Pprev = new double[4][4]; // a posteriori error covariance matrix of the previous time-step
		Ppriori = new double[4][4]; // a priori error covariance matrix of the previous time-step
		omega = new double[4][4]; // function of angular velocity
		Xi = new double[4][3]; // function of quaternion
		residual = new double[4];
		covResidual = new double[4][4];

		// create identity matrix
		I33 = new double[3][3];// 3 by 3 identity matrix
		I44 = new double[4][4];// 4 by 4 identity matrix
		int i, j;
		for (i = 0; i < 3; i++)
			for (j = 0; j < 3; j++)
				I33[i][j] = 0.0;
		for (i = 0; i < 3; i++)
			I33[i][i] = 1.0;
		for (i = 0; i < 4; i++)
			for (j = 0; j < 4; j++)
				I44[i][j] = 0.0;
		for (i = 0; i < 4; i++)
			I44[i][i] = 1.0;

		temp1Vec3 = new double[3];
		temp2Vec3 = new double[3];
		temp3Vec3 = new double[3];
		temp4Vec3 = new double[3];
		temp1Vec4 = new double[4];
		temp1Mat44 = new double[4][4];
		temp2Mat44 = new double[4][4];
		temp3Mat44 = new double[4][4];
		temp1Mat33 = new double[3][3];
		temp2Mat33 = new double[3][3];
		
		// Velocity ////////////////////////////////
		
		
		 VelocityEarthFrame [0]=0;  VelocityEarthFrame [1]=0;  VelocityEarthFrame [2]=0; 
		 PositionEarthFrame [0]=0;  PositionEarthFrame [1]=0;  PositionEarthFrame [2]=0; 

	}

	private double wrapYaw(double angle) {
		if (angle < 0) {
			angle = 180 + (180 + angle);

		}

		/*
		 * if (angle > 180) angle -= 360; else if (angle<-180) angle+= 360; else
		 * if (angle==0) angle+=360;
		 */
		return angle;
	}

	// method to format doubles to 4 decimal places
	public String double2str(double inValue) {
		DecimalFormat threeDec = new DecimalFormat("0.0000");
		threeDec.setGroupingUsed(false);
		return threeDec.format(inValue);
	}

}