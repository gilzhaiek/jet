package com.reconinstruments.sensorratetester;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

@SuppressLint("HandlerLeak")
public class MainActivity extends Activity implements SensorEventListener {
	private static final String TAG = MainActivity.class.getSimpleName();

	private static final int STAGE_ACCEL		= 0;
	private static final int STAGE_GYRO			= 1;
	private static final int STAGE_MAG			= 2;
	private static final int STAGE_ALL			= 3;
	
	private static final int DATA_COLLECT_TIME	= 10; // in seconds

	private static SensorManager sSensorManager = null;

	private static int sAccelCounter = 0;
	private static int sGyroCounter = 0;
	private static int sMagCounter = 0;

	private static int sStage = -1;

	public Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			nextStage();
		}
	};

	private Thread mCurrentTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

	}

	@Override
	protected void onResume() {
		super.onResume();
		nextStage();	
	}

	@Override
	protected void onPause() {
		// unregister listener
		super.onPause();
		sSensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			sAccelCounter++;
			break;
		case Sensor.TYPE_GYROSCOPE:
			sGyroCounter++;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			sMagCounter++;
			break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	private void nextStage() {
		sStage++;
		processStage(sStage);
	}
	
	private void processStage(int stage) {
		switch (stage) {
		case STAGE_ACCEL:
			mCurrentTask = new SensorThread(sSensorManager, this, mHandler,
					Sensor.TYPE_ACCELEROMETER);
			break;
		case STAGE_GYRO:
			mCurrentTask = new SensorThread(sSensorManager, this, mHandler,
					Sensor.TYPE_GYROSCOPE);
			break;
		case STAGE_MAG:
			mCurrentTask = new SensorThread(sSensorManager, this, mHandler,
					Sensor.TYPE_MAGNETIC_FIELD);
			break;
		case STAGE_ALL:
			mCurrentTask = new SensorThread(sSensorManager, this, mHandler,
					Sensor.TYPE_ACCELEROMETER,
					Sensor.TYPE_GYROSCOPE,
					Sensor.TYPE_MAGNETIC_FIELD);
			break;
		default:
			finish();
			break; // Other than these cases, finish
		}
		
		mCurrentTask.start();
	}
	
	private static void resetCounterAndLog(String delay) {
		Log.d(TAG, "delay : " + delay
				+ "sAccelCounter : " + sAccelCounter/DATA_COLLECT_TIME 
				+ "\t sGyroCounter : " + sGyroCounter/DATA_COLLECT_TIME
				+ "\t sMagCounter : " + sMagCounter/DATA_COLLECT_TIME);
		sAccelCounter = 0;
		sGyroCounter = 0;
		sMagCounter = 0;
	}

	class SensorThread extends Thread {

		private SensorManager mSensorManager;
		private SensorEventListener mListener;
		private int[] mSensorTypeArray;
		private Handler mFinishedHandler;

		public SensorThread(SensorManager sensorManager, SensorEventListener listener, Handler finishedHandler, int... type) {
			mSensorManager = sensorManager;
			mListener = listener;
			mSensorTypeArray = type;
			mFinishedHandler = finishedHandler;
		}
		
		private void registerListener(int delay) {
			for (int i = 0 ; i < mSensorTypeArray.length ; i++) {
				mSensorManager.registerListener(mListener,
						mSensorManager.getDefaultSensor(mSensorTypeArray[i]),
						delay);
			}
		}
		
		private void unregisterListener() {
			mSensorManager.unregisterListener(mListener);
		}
		
		private void sensorToggleForTime(int delay, int sleepTime) {
			registerListener(delay);
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			unregisterListener();
		}

		@Override
		public void run() {
			
			String stage = "\nCOLLECTING ";
			switch (MainActivity.sStage) {
			case STAGE_ACCEL:
				stage += "ACCEL-----------------------";
				break;
			case STAGE_GYRO:
				stage += "GRYO------------------------";
				break;
			case STAGE_MAG:
				stage += "MAG-------------------------";
				break;
			case STAGE_ALL:
				stage += "ALL-------------------------";
				break;
			}
			Log.d(TAG, stage);
			
			sensorToggleForTime(SensorManager.SENSOR_DELAY_NORMAL, DATA_COLLECT_TIME * 1000);
			resetCounterAndLog("SENSOR_DELAY_NORMAL |");
			sensorToggleForTime(SensorManager.SENSOR_DELAY_FASTEST, DATA_COLLECT_TIME * 1000);
			resetCounterAndLog("SENSOR_DELAY_FASTEST|");
			sensorToggleForTime(SensorManager.SENSOR_DELAY_GAME, DATA_COLLECT_TIME * 1000);
			resetCounterAndLog("SENSOR_DELAY_GAME   |");
			sensorToggleForTime(SensorManager.SENSOR_DELAY_UI, DATA_COLLECT_TIME * 1000);
			resetCounterAndLog("SENSOR_DELAY_UI     |");

			mFinishedHandler.sendEmptyMessage(0);

		}

	};

}
