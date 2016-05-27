package com.reconinstruments.compass;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * Attention !!! This service is not being used anymore <-------- 
 */
public class CompassCalibrationService extends Service implements SensorEventListener {
    
	public static final String TAG = "CompassCalibrationService";
	public static final String INTENT_ACTION = "COMPASS_CALIBRATION_REQUEST";
	
	CalibrationReceiver mCalibrationReceiver;
	SensorManager mSensorManager;
	ErrFunction mErrFunction;
	Handler mHandler = new Handler();
	
	// Shared Preferences info
	public static final String APP_SHARED_PREFS = "com.reconinstruments.compass.calibration";
	public static final String KEY_X_OFFSET = "magOffsetX";
	public static final String KEY_Y_OFFSET = "magOffsetY";
	public static final String KEY_Z_OFFSET = "magOffsetZ";
	SharedPreferences mPrefs;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mPrefs = this.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_WORLD_READABLE);
		mCalibrationReceiver = new CalibrationReceiver();
		
		// Listen for broadcasts requesting a calibration check
		registerReceiver(mCalibrationReceiver, new IntentFilter(INTENT_ACTION));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mCalibrationReceiver);
		mSensorManager.unregisterListener(this);
	}
	
	class CalibrationReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG, "Received: " + intent.getAction());
			mErrFunction = new ErrFunction();
			
			// Check magnetometer readings
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			mSensorManager.registerListener(CompassCalibrationService.this,
					mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					SensorManager.SENSOR_DELAY_FASTEST);
			
			Intent startedIntent = new Intent();
			startedIntent.setAction("COMPASSING_CALIBRATION_STARTED");
			CompassCalibrationService.this.sendBroadcast(startedIntent);
			
			// Collect data for 5 seconds
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					float xOffset = mPrefs.getFloat(KEY_X_OFFSET, 0);
					float yOffset = mPrefs.getFloat(KEY_Y_OFFSET, 0);
					float zOffset = mPrefs.getFloat(KEY_Z_OFFSET, 0);
					
					double[] offsets = {(double) xOffset, (double) yOffset, (double) zOffset};
					
					double variance = mErrFunction.value(offsets);
					
					// Start calibration activity if magnetic norm shows significant variance or
					// the norm is greater than outside the range of earth's magnetic field strength
					if(variance > 15 || mErrFunction.range(offsets, 0, 80)) {
						// Launch calibration activity
						Intent dialogIntent = new Intent(getBaseContext(), CompassSensorActivity.class);
						dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						getApplication().startActivity(dialogIntent);
					}
					
					// stop reading from magnetometer
					mSensorManager.unregisterListener(CompassCalibrationService.this);
				}
				
			}, 5000);
		}
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		mErrFunction.addPoints(new Double(event.values[0]), 
				new Double(event.values[1]), 
				new Double(event.values[2]));
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

}
