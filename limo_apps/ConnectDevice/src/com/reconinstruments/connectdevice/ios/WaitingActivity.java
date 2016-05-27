package com.reconinstruments.connectdevice.ios;

import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.R;

import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.utils.DeviceUtils;

public class WaitingActivity extends ConnectionActivity {
	private static final String TAG = "WaitingActivity";

	private TextView macAddressTV;
	private TextView timerTV;
	private CountDownTimer waittingMonitor;

	private void putDeviceInSlaveMode() {
		// put device in slave mode to connect with iphone
		Intent theIntent = new Intent().setAction("private_ble_command");
		theIntent.putExtra("command", 1);
		sendBroadcast(theIntent);
		Log.d(TAG,
				"Send out a broadcast to put the mod live into the slave mode.");
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "onCreate");

		this.setContentView(R.layout.activity_ios_waiting);

		displayed = false;
		started = false;

		if(DeviceUtils.isLimo()){
			macAddressTV = (TextView) findViewById(R.id.mac_textview);
		}else{
			macAddressTV = (TextView) findViewById(R.id.mac_textview);
			byte[] macAddress = BluetoothAdapter.getDefaultAdapter().getAddress().getBytes();
			String deviceID = getDeviceID(macAddress);
			macAddressTV.setText("(DeviceID: " + deviceID + ")");
		}

		timerTV = (TextView) findViewById(R.id.timeout);

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				timerTV.setText((String) msg.obj);
			}
		};

		new Thread() {
			@Override
			public void run() {
				super.run();
				int time = 90;
				while (time >= 0) {
					Message msg = new Message();
					msg.obj = timeString(time);
					handler.sendMessage(msg);
					// timerTV.setText(String.format("0:%2d", time));
					time--;
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// clear timer
				Message msg = new Message();
				msg.obj = "";
				handler.sendMessage(msg);

				// Intent theIntent = new
				// Intent().setAction("private_ble_command");
				// theIntent.putExtra("command",2);
				// sendBroadcast(theIntent);
				// startActivity(new
				// Intent(WaitingActivity.this,FailedActivity.class));
				// finish();
			}

		}.start();

		TextView conn = (TextView) findViewById(R.id.activity_ios_waiting_text);
		if (OakleyDecider.isOakley()) {
			if(from == 0 || !WaitingActivity.super.isTheSameDevice()){
				conn.setText(R.string.ios_waiting_oakley);
			}else{
				conn.setText(R.string.ios_waiting_oakley_reconnecting);
			}
		} else {
			if(from == 0 || !WaitingActivity.super.isTheSameDevice()){
				conn.setText(R.string.ios_waiting);
			}else{
				conn.setText(R.string.ios_waiting_reconnecting);
			}
		}
		Log.d(TAG, "waittingMonitor is running");
		waittingMonitor = new CountDownTimer(120 * 1000, 1000) {

			public void onTick(long millisUntilFinished) {

			}

			public void onFinish() {
				putDeviceInMasterMode();
				startActivity(new Intent(WaitingActivity.this,
						FailedActivity.class));
				WaitingActivity.this.finish();
			}
		}.start();

	}

	public String timeString(int seconds) {
		return String.format(
				"%d:%02d",
				TimeUnit.SECONDS.toMinutes(seconds),
				TimeUnit.SECONDS.toSeconds(seconds)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS
								.toMinutes(seconds)));
	}

	public void onDestroy() {
		super.onDestroy();
		// PreferencesUtils.setDeviceName(this, getBLEDeviceName());
		
		if(DeviceUtils.isLimo()){
			releaseService();
		}
		
		if (waittingMonitor != null) {
			waittingMonitor.cancel();
		}
		Log.d(TAG, "onDestroy");
	}

	public void onResume() {
		super.onResume();
		if(DeviceUtils.isLimo()){
			putDeviceInSlaveMode();
			initService();
		}
		Log.d(TAG, "onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		if (!this.isFinishing()) {
			finish();
		}
	}

	// JIRA: MODLIVE-773 Implement cancel slave mode by applying the power
	// button.
	private void putDeviceInMasterMode() {
		Intent theIntent = new Intent().setAction("private_ble_command");
		theIntent.putExtra("command", 2);
		sendBroadcast(theIntent);
		Log.d(TAG,
				"Send out a broadcast to put the mod live into the master mode.");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
			Log.d(TAG, "The power button was be pressed.");
			putDeviceInMasterMode();
			startActivity(new Intent(this, FailedActivity.class));
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// End of JIRA: MODLIVE-773

	// ////////////////////////////////////////////////////
	// aidl service connection.
	// ///////////////////////////////////////////////////
	private IBLEService bleService;
	private BLEServiceConnection bleServiceConnection;

	private void initService() {
		if (bleServiceConnection == null) {
			bleServiceConnection = new BLEServiceConnection();
			Intent i = new Intent("RECON_BLE_TEST_SERVICE");
			bindService(i, bleServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "bindService()");
		}
	}

	private void releaseService() {
		if (bleServiceConnection != null) {
			unbindService(bleServiceConnection);
			bleServiceConnection = null;
			Log.d(TAG, "unbindService()");
		}
	}

	class BLEServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			bleService = IBLEService.Stub.asInterface((IBinder) boundService);
			Log.d(TAG, "onServiceConnected");

			if (bleService != null) {
				try {
					byte[] macAddress = bleService.getOwnMacAddress();
					String deviceID = getDeviceID(macAddress);

					macAddressTV.setText("(DeviceID: " + deviceID + ")");
					// Potential redundancy:
					// Sometimes the broadast to
					// change ble mode to slave
					// does not go throuh. Here we
					// do one more check and set
					// it in case it doesn'g
					if (bleService.getIsMaster()) {
						putDeviceInSlaveMode();
					}

				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (NullPointerException n) {
					n.printStackTrace();
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			bleService = null;
			Log.d(TAG, "onServiceDisconnected");
		}
	};

	/** returns the last 3 bytes in hex */
	public String getDeviceID(byte[] macAddress) {
	    if(DeviceUtils.isLimo()){
			if (macAddress.length != 6)
				return "unknown";
		}

		return String.format("%02X%02X%02X", macAddress[2], macAddress[1],
				macAddress[0]);
	}

}
