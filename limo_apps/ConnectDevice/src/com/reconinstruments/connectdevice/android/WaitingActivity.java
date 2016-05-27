package com.reconinstruments.connectdevice.android;

import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.connectdevice.ChooseDeviceActivity;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.connectdevice.ios.MfiReconnectActivity;

public class WaitingActivity extends ConnectionActivity {
	protected static final String TAG = "WaitingActivity";

	final static int DISCOVERABLE_REQUEST = 4;

	TextView timerTV;
	TextView mainTV;
	TextView nameTV;
	int timeOut;

	boolean cancelTimer = false;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_android_waiting_jet);

		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
		startActivityForResult(discoverableIntent, DISCOVERABLE_REQUEST);

		registerReceiver(btReceiver, new IntentFilter(
				BluetoothDevice.ACTION_BOND_STATE_CHANGED));

		timerTV = (TextView) findViewById(R.id.timeout);
		mainTV = (TextView) findViewById(R.id.main_text);

		nameTV = (TextView) findViewById(R.id.btname_text);
		nameTV.setText("(Device Name: "
				+ BluetoothAdapter.getDefaultAdapter().getName() + ")");

		PreferencesUtils.setLastPairedDeviceAddress(this, "");
		PreferencesUtils.setLastPairedDeviceName(this, "");
		
		if (OakleyDecider.isOakley()) {
			TextView conn = (TextView) findViewById(R.id.main_text);
			conn.setText(R.string.android_waiting_oakley);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(btReceiver);
		timerThread.interrupt();
		cancelTimer = true;
	}

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			timerTV.setText((String) msg.obj);
		}
	};

	public String timeString(int seconds) {
		return String.format(
				"%d:%02d",
				TimeUnit.SECONDS.toMinutes(seconds),
				TimeUnit.SECONDS.toSeconds(seconds)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS
								.toMinutes(seconds)));
	}

	Thread timerThread = new Thread() {
		@Override
		public void run() {
			super.run();
			int time = timeOut;
			while (time > 0) {
				Message msg = new Message();
				msg.obj = timeString(time);
				handler.sendMessage(msg);
				time--;
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
			if (!cancelTimer) {
				startActivity(new Intent(WaitingActivity.this,
						FailedActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				finish();
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == DISCOVERABLE_REQUEST) {
			if (resultCode >= Activity.RESULT_FIRST_USER) {
				nameTV.setText("(Device Name: "
						+ BluetoothAdapter.getDefaultAdapter().getName() + ")");
				timeOut = resultCode;
				timerThread.start();
			} else if (resultCode == Activity.RESULT_CANCELED) {
				finish();
			}
		}
	}

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE) == BluetoothDevice.BOND_BONDED) {

					// Toast.makeText(WaitingActivity.this,
					// "Paired with "+device.getName(),
					// Toast.LENGTH_LONG).show();
					
//					timerTV.setVisibility(View.GONE);
//					mainTV.setText("You are now paired with\n"
//							+ device.getName());

					timerThread.interrupt();
					cancelTimer = true;
					finish();
				}
				if (intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE) == BluetoothDevice.BOND_BONDING) {
					// hopefully pairing request pops up
				}

				// JIRA: MODLIVE-687 MOD Live stays at "Waiting for Phone"
				// screen and its timer stops
				if (intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE) == BluetoothDevice.BOND_NONE) {
					Log.d(TAG,
							"user canceled pairing request, the activity should be finished.");
					timerThread.interrupt();
					cancelTimer = true;
					finish();
				}
				// End of JIRA

			}
			timerThread.interrupt();
			cancelTimer = true;
		}
	};
	
	@Override
	public void onBackPressed() {
	    startActivity(new Intent(WaitingActivity.this,
				     FirstConnectActivity.class));
	    WaitingActivity.this.finish();

	}
}
