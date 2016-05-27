package com.reconinstruments.mfitester;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.reconinstruments.mobilesdk.btmfi.BTMfiSessionManager;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;

public class MfiTesterActivity extends Activity {

	private static final String TAG = "MfiTesterActivity";

	public static int sessionID1 = 0;
	public static int sessionID2 = 0;
	public static int sessionID3 = 0;

	private ArrayAdapter<String> mAdapter;
	private List<String> mAddress;
	private Spinner deviceSpinner;

	private Activity mActivity;
	
	public enum Channel {
		COMMAND_CHANNEL, OBJECT_CHANNEL, FILE_CHANNEL
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tester);

		mActivity = this;

		Log.d(TAG, "onCreate");
		init();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
//		BTMfiSessionManager.getInstance(mActivity).cleanup();
		super.onDestroy();
	}

	private void populateAdapterWithBondedBtDevices() {
		BluetoothAdapter mBluetoothAdapter = null;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
		mAddress = new ArrayList<String>();
		for (BluetoothDevice d : devices) {
			mAdapter.add(d.getName() + " " + d.getAddress().substring(0, 5));
			mAddress.add(d.getAddress());
		}
	}

	private void init() {

		Log.d(TAG, "init");
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		populateAdapterWithBondedBtDevices();

		deviceSpinner = (Spinner) findViewById(R.id.devices_spinner);
		deviceSpinner.setAdapter(mAdapter);

		final Button connectButton = (Button) findViewById(R.id.connect);
		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int theIndex = deviceSpinner.getSelectedItemPosition();
				String theAddress = "";
				if (theIndex >= 0) {
					Log.d(TAG, "connect");
					theAddress = (String) mAddress.get(theIndex);
					Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.connect");
					i.putExtra("address", theAddress);
					i.putExtra("deviceType", "IOS");
					sendBroadcast(i);
					Log.d(TAG, "connect to " + theAddress);
//					BTMfiSessionManager.getInstance(mActivity)
//							.connectRemoteDevice(theAddress, 1, false);
				}

			}
		});
		final Button disconnectButton = (Button) findViewById(R.id.disconnect);
		disconnectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int theIndex = deviceSpinner.getSelectedItemPosition();
				String theAddress = "";
				if (theIndex >= 0) {
					theAddress = (String) mAddress.get(theIndex);
					Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.disconnect");
					i.putExtra("address", theAddress);
					i.putExtra("deviceType", "IOS");
					sendBroadcast(i);
					Log.d(TAG, "disconnect with " + theAddress);
				}
//				BTMfiSessionManager.getInstance(mActivity)
//						.disconnectRemoteDevice(true, 5000);
			}
		});
//		final Button readDataButton = (Button) findViewById(R.id.read_data);
//		readDataButton.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				Log.d(TAG, "readData");
//				BTMfiSessionManager.getInstance(mActivity).readData(true);
//			}
//		});
//		final Button writeDataButton = (Button) findViewById(R.id.write_data);
//		writeDataButton.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				Log.d(TAG, "sendNonSessionData");
//				BTMfiSessionManager.getInstance(mActivity).sendNonSessionData(
//						true, 1, 1, 1);
//				Log.d(TAG, "writeData");
//				BTMfiSessionManager.getInstance(mActivity).writeData(true);
//			}
//		});
//		final Button sendSessionDataButton = (Button) findViewById(R.id.write_session_data);
//		sendSessionDataButton.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				if (sessionID1 > 0 && sessionID2 > 0 && sessionID3 > 0) {
//					BTMfiSessionManager.getInstance(mActivity).sendSessionData(
//							true, sessionID1, "This is a session data test message".getBytes());
//					Log.d(TAG, "sendSessionData via sessionID1");
//					BTMfiSessionManager.getInstance(mActivity).sendSessionData(
//							true, sessionID2, "This is a session data test message".getBytes());
//					Log.d(TAG, "sendSessionData via sessionID2");
//					BTMfiSessionManager.getInstance(mActivity).sendSessionData(
//							true, sessionID3, "This is a session data test message".getBytes());
//					Log.d(TAG, "sendSessionData via sessionID3");
//					
//					HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
//					cMsg.setIntentFilter("IntentFilter.COMMAND");
//					cMsg.setRequestKey(0);
//					cMsg.setSender("com.reconinstruments.mfitester.MfiTesterActivity");
//					cMsg.setData("This is a session data test message".getBytes());
//					
//					BTMfiSessionManager.getInstance(mActivity).sendSessionData(
//							Channel.COMMAND_CHANNEL, cMsg.toByteArray());
//					Log.d(TAG, "sendSessionData via Channel.COMMAND_CHANNEL");
//					cMsg.setIntentFilter("IntentFilter.OBJECT");
//					BTMfiSessionManager.getInstance(mActivity).sendSessionData(
//							Channel.OBJECT_CHANNEL, cMsg.toByteArray());
//					Log.d(TAG, "sendSessionData via Channel.OBJECT_CHANNEL");
//					cMsg.setIntentFilter("IntentFilter.FILE");
//					BTMfiSessionManager.getInstance(mActivity).sendSessionData(
//							Channel.FILE_CHANNEL, cMsg.toByteArray());
//					Log.d(TAG, "sendSessionData via Channel.FILE_CHANNEL");
//				} else {
//					Log.d(TAG, "No session");
//				}
//			}
//		});
//		BTMfiSessionManager.getInstance(mActivity).init();
	}
}
