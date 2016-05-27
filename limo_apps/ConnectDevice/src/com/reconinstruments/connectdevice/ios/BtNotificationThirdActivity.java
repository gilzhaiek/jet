package com.reconinstruments.connectdevice.ios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.connectdevice.BTPropertyReader;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.connectdevice.ConnectionActivity.DeviceType;
import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.utils.DeviceUtils;


//JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class BtNotificationThirdActivity extends ConnectionActivity {
	private static final String TAG = "BtNotificationThirdActivity";
	private static final int PAIRING = 1;
	private static final int SCANING = 0;
	private static final int REQUEST_ENABLE_BT = 3;
	private ArrayAdapter<String> adapter;
	private List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
	private BluetoothAdapter mBtAdapter;
	private TextView header;
	private ListView lv;
	private TextView emptyView;
	private TextView nextView;
	private TextView pairView;
	private View okButton;
	private View cancelButton;
	private ImageView backButton;
	private View selectButton;
	private TextView pairTV;
	private int action = SCANING;
	private String deviceName;
	private String deviceAddress;
	private int step = 0;
	private ProgressDialog progressDialog;
	private BluetoothDevice priorDevice;
	private boolean skipScaning = false;
	private CountDownTimer pairingMonitor;
	private boolean pairFails = false;
	private ProgressBar progressBar;
	private boolean noDevice = false;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
        setProgressBarIndeterminateVisibility(true);
		this.setContentView(R.layout.activity_ios_bt_scanning_list_layout_jet);
		progressBar = (ProgressBar) findViewById(R.id.marker_progress);
		progressBar.setVisibility(View.GONE);
		Log.d(TAG, "onCreate");
		lv = (ListView) findViewById(android.R.id.list);
		emptyView = (TextView) findViewById(android.R.id.empty);
		header = (TextView) findViewById(R.id.header_text);
		okButton = (View) findViewById(R.id.ok);
		cancelButton = (View) findViewById(R.id.cancel);
		backButton = (ImageView) findViewById(R.id.back_button);
		okButton.setVisibility(View.GONE);
		cancelButton.setVisibility(View.GONE);
		nextView = (TextView) findViewById(R.id.next);
		pairView = (TextView) findViewById(R.id.pair);
		
		selectButton = (View) findViewById(R.id.select_button);
		pairTV = (TextView) findViewById(R.id.pair);

			adapter = new ArrayAdapter<String>(this,
					R.layout.activity_ios_bt_scanning_list_item_jet);
		lv.setAdapter(adapter);
		if(DeviceUtils.isLimo()){
			emptyView
			.setText(Html
					.fromHtml("Stay in <b>Settings</b> > <b>Bluetooth</b> on your <b>iPhone</b> while your MOD Live is scanning"));
		}else{
			emptyView
			.setText(Html
					.fromHtml("Stay in <b>Settings</b> > <b>Bluetooth</b> on your <b>iPhone</b> while your Snow2 is scanning"));
		}
		lv.setEmptyView(emptyView);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Log.d(TAG, "onItemClick: " + arg2);
				BluetoothDevice device = devices.get(arg2);
				Log.d(TAG, "device.getName()=" + device.getName());
				if(DeviceUtils.isLimo()){
					BtNotificationThirdActivity.this.pairDevice(device);
				}else{
					BtNotificationThirdActivity.this.pairMfiDevice(device);
				}
			}
		});

		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				okButton.setFocusable(false);
				cancelButton.setFocusable(false);
				if (action == PAIRING) {
					Log.d(TAG, "Pairing again...");
						Iterator<BluetoothDevice> iterator = devices.iterator();
						while (iterator.hasNext()) {
							BluetoothDevice device = iterator.next();
							if(deviceName != null && !deviceName.equals(device.getName())){
								iterator.remove();
							}
						}
						if ((devices.size() == 1)) {
							header.setText("PAIRING...");
							BluetoothDevice device = devices.get(0);
							BtNotificationThirdActivity.this.pairMfiDevice(device);
							lv.setVisibility(View.VISIBLE);
							emptyView.setVisibility(View.GONE);
							okButton.setVisibility(View.GONE);
							cancelButton.setVisibility(View.GONE);
						} else {
							Log.d(TAG, "Scanning again...");
							mBtAdapter.startDiscovery();
							nextView.setText("CANCEL");
							progressBar.setVisibility(View.VISIBLE);
//							progressDialog = ProgressDialog.show(
//									BtNotificationThirdActivity.this, "",
//									"Scanning...");
							devices.clear();
							adapter.clear();
							header.setText("SCANNING...");
							lv.setVisibility(View.VISIBLE);
							emptyView.setVisibility(View.GONE);
							okButton.setVisibility(View.GONE);
							cancelButton.setVisibility(View.GONE);
							selectButton.setVisibility(View.GONE);
							pairTV.setVisibility(View.GONE);
						}
				} else {
						Iterator<BluetoothDevice> iterator = devices.iterator();
						while (iterator.hasNext()) {
							BluetoothDevice device = iterator.next();
							if(deviceName != null && !deviceName.equals(device.getName())){
								iterator.remove();
							}
						}
						if ((devices.size() == 1)) {
							header.setText("PAIRING...");
							BluetoothDevice device = devices.get(0);
							BtNotificationThirdActivity.this.pairMfiDevice(device);
							lv.setVisibility(View.VISIBLE);
							emptyView.setVisibility(View.GONE);
							okButton.setVisibility(View.GONE);
							cancelButton.setVisibility(View.GONE);
						} else {
							Log.d(TAG, "Scanning again...");
							mBtAdapter.startDiscovery();
							nextView.setText("CANCEL");
							progressBar.setVisibility(View.VISIBLE);
//							progressDialog = ProgressDialog.show(
//									BtNotificationThirdActivity.this, "",
//									"Scanning...");
							devices.clear();
							adapter.clear();
							header.setText("SCANNING...");
							lv.setVisibility(View.VISIBLE);
							emptyView.setVisibility(View.GONE);
							okButton.setVisibility(View.GONE);
							cancelButton.setVisibility(View.GONE);
							selectButton.setVisibility(View.GONE);
							pairTV.setVisibility(View.GONE);
						}
				}
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				okButton.setFocusable(false);
				cancelButton.setFocusable(false);
				cancel = true;
				BtNotificationThirdActivity.this.finish();
			}
		});

		okButton.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v
						.getBackground();
				if (hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		cancelButton.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v
						.getBackground();
				if (hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});

		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		
		
			devices.clear();
			adapter.clear();
			progressBar.setVisibility(View.VISIBLE);
			lv.setVisibility(View.VISIBLE);
		if (!mBtAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			// initService_phone();
			if (mBtAdapter.isDiscovering()) {
				mBtAdapter.cancelDiscovery();
			}
			if (PreferencesUtils.getDeviceName(this).equals(
					PreferencesUtils.getBTDeviceName(this))
					&& !"unknown".equals(PreferencesUtils.getDeviceName(this)) && !"".equals(PreferencesUtils.getDeviceName(this))) {
				skipScaning = true;
				if(DeviceUtils.isLimo()){
					pairDevice(mBtAdapter.getRemoteDevice(PreferencesUtils
							.getDeviceAddress(this)));
				}else{
					pairMfiDevice(mBtAdapter.getRemoteDevice(PreferencesUtils
							.getDeviceAddress(this)));
				}
				Log.d(TAG, "skip scanning step.");
			}

			if (!skipScaning) {
				mBtAdapter.startDiscovery();
				nextView.setText("CANCEL");
			}
			if (!skipScaning) {
				header.setText("SCANNING...");
				selectButton.setVisibility(View.GONE);
				pairTV.setVisibility(View.GONE);
			} else {
				header.setText("PAIRING...");
			}
		}

	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				if (mBtAdapter.isDiscovering()) {
					mBtAdapter.cancelDiscovery();
				}
				if (PreferencesUtils.getDeviceName(this).equals(
						PreferencesUtils.getBTDeviceName(this))
						&& !"unknown".equals(PreferencesUtils
								.getDeviceName(this))) {
					skipScaning = true;
					if(DeviceUtils.isLimo()){
						pairDevice(mBtAdapter.getRemoteDevice(PreferencesUtils
								.getDeviceAddress(this)));
					}else{
						pairMfiDevice(mBtAdapter.getRemoteDevice(PreferencesUtils
								.getDeviceAddress(this)));
					}
					Log.d(TAG, "skip scanning step.");
				}

				if (!skipScaning) {
					mBtAdapter.startDiscovery();
					nextView.setText("CANCEL");
				}
				if (!skipScaning) {
					header.setText("SCANNING...");
					selectButton.setVisibility(View.GONE);
					pairTV.setVisibility(View.GONE);
				} else {
					header.setText("PAIRING...");
				}
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
		    	(new ReconToast(this, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, "Bluetooth hasn't enabled")).show();
				BtNotificationThirdActivity.this.finish();
			}
			break;
		}
	}

	private void pairMfiDevice(BluetoothDevice device) {
		if (pairingMonitor != null) {
			pairingMonitor.cancel();
		}
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}
		Log.d(TAG, "pairingMonitor is running");
		deviceName = device.getName();
		pairingMonitor = new CountDownTimer(20 * 1000, 1000) {

			public void onTick(long millisUntilFinished) {

			}

			public void onFinish() {
				failToPair();
			}
		}.start();
		if(hudService != null){
			connect(device.getAddress(), DeviceType.IOS);
		}
//		if (progressBar != null) {
//			progressBar.setVisibility(View.GONE);
//		}
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		progressDialog = new ProgressDialog(this);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
		progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
		TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
		textTv.setText("Pairing...");
//		progressBar.setVisibility(View.VISIBLE);
		header.setText("PAIRING...");
	}

	private void pairDevice(BluetoothDevice device) {
		if (pairingMonitor != null) {
			pairingMonitor.cancel();
		}
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}
		Log.d(TAG, "pairingMonitor is running");
		pairingMonitor = new CountDownTimer(20 * 1000, 1000) {

			public void onTick(long millisUntilFinished) {

			}

			public void onFinish() {
				failToPair();
			}
		}.start();

		try {
			registerReceiver(btTelephonyReceiver, new IntentFilter(
					"com.reconinstruments.BtTelephonyStateChanged"));
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "btTelephonyReceiver is already unregistered");
		}
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		progressDialog = new ProgressDialog(this);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
		progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
		TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
		textTv.setText("Pairing...");
		header.setText("PAIRING...");
		int state = device.getBondState();
		deviceName = device.getName();
		deviceAddress = device.getAddress();
		Log.d(TAG, "Call connectToHfp and connectToMap.");
		connectToHfp(deviceAddress);
		connectToMap(deviceAddress);
		if (!PreferencesUtils.getDeviceName(this).equals(
				PreferencesUtils.getBTDeviceName(this))) {
			connectToHfp(deviceAddress);
			connectToMap(deviceAddress);
		}
		PreferencesUtils.setBTDeviceName(this, deviceName);
		step++;
	}

	private void checkDuplicateItems() {
		Map<String, BluetoothDevice> deviceMap = new HashMap<String, BluetoothDevice>();
		for (BluetoothDevice device : devices) {
			deviceMap.put(device.getAddress(), device);
		}
		adapter.clear();
		devices.clear();
		devices.addAll(deviceMap.values());
		for (BluetoothDevice device : devices) {
			adapter.add(device.getName());
		}
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				Log.d(TAG, "BluetoothDevice.ACTION_FOUND");
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				Log.d(TAG, "device.getName():" + device.getName());
				Log.d(TAG, "device.getBluetoothClass().getDeviceClass():" + device.getBluetoothClass().getDeviceClass());
				if(device != null && device.getName() != null && BluetoothClass.Device.COMPUTER_LAPTOP != device.getBluetoothClass().getDeviceClass() && BluetoothClass.Device.COMPUTER_DESKTOP != device.getBluetoothClass().getDeviceClass() && BluetoothClass.Device.COMPUTER_SERVER != device.getBluetoothClass().getDeviceClass()){
					Log.d(TAG, "device.getName():" + device.getName());
						adapter.add(device.getName());
						devices.add(device);
					selectButton.setVisibility(View.VISIBLE);
					pairTV.setVisibility(View.VISIBLE);
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				Log.d(TAG, "BluetoothAdapter.ACTION_DISCOVERY_FINISHED");
				if (devices.size() > 1) {
					header.setText("FOUND DEVICES");
					lv.setFocusable(true);
					lv.setClickable(true);
					lv.setFocusableInTouchMode(true);
					lv.requestFocus();
					lv.requestFocusFromTouch();
					lv.setSelected(true);
					lv.setSelection(0);
					lv.invalidate();
					selectButton.setVisibility(View.VISIBLE);
					pairTV.setVisibility(View.VISIBLE);
				} else if (devices.size() == 1) {
					BluetoothDevice device = devices.get(0);
					Log.d(TAG, "device.getName()=" + device.getName());
					if(DeviceUtils.isLimo()){
						BtNotificationThirdActivity.this.pairDevice(device);
					}else{
//						BtNotificationThirdActivity.this.pairMfiDevice(device);
					}
					selectButton.setVisibility(View.VISIBLE);
					pairTV.setVisibility(View.VISIBLE);
				} else {
					header.setText("NO DEVICES FOUND");
					devices.clear();
					adapter.clear();
					emptyView
							.setText(Html
									.fromHtml("Go to <b>Settings</b> > <b>Bluetooth</b> on your <b>iPhone</b> and try scanning again."));
					emptyView.setVisibility(View.VISIBLE);
						selectButton.setVisibility(View.GONE);
						pairTV.setVisibility(View.GONE);

						backButton.setVisibility(View.VISIBLE);
						backButton.setImageResource(R.drawable.select_button);
						noDevice = true;
				}
					nextView.setText("SCAN AGAIN");
					if (progressBar != null) {
						progressBar.setVisibility(View.GONE);
					}
					if (progressDialog != null) {
						progressDialog.dismiss();
					}
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);
		registerReceiver(btReceiver, new IntentFilter(
				BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		try {
			registerReceiver(btTelephonyReceiver, new IntentFilter(
					"com.reconinstruments.BtTelephonyStateChanged"));
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "btTelephonyReceiver is already unregistered");
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		if (pairingMonitor != null) {
			pairingMonitor.cancel();
		}
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}

		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}
		this.unregisterReceiver(mReceiver);
		unregisterReceiver(btReceiver);
		// releaseService_phone();
		// Log.d(TAG, "unregisterReceiver for btTelephonyReceiver");
		try {
			this.unregisterReceiver(btTelephonyReceiver);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "btTelephonyReceiver is already unregistered");
		}
	}

	private static int priorState = 0;
	private boolean receiving = false;
	private boolean quit = false;
	private BroadcastReceiver btTelephonyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
//			if (step != 0) {
				// state 2 connected, 1 connecting, 0 disconnected
				int hfpstate = intent.getIntExtra("HfpState", 0);
				// int mapstate = intent.getIntExtra("MapState", 0);
				// BtNotificationThirdActivity.mapStatus = mapstate;
				BtNotificationThirdActivity.hfpStatus = hfpstate;
				Log.d(TAG, "hfp state: " + hfpstate);
				// Log.d(TAG, "receiving: " + receiving);
				BtNotificationThirdActivity.super.sendXmlToiPhone();
				if ((hfpstate == 2)) {
					receiving = false;
//					try {
//						BtNotificationThirdActivity.this
//								.unregisterReceiver(btTelephonyReceiver);
//					} catch (IllegalArgumentException e) {
//						Log.d(TAG,
//								"btTelephonyReceiver is already unregistered");
//					}
					Log.d(TAG,
							"Success to disconnect and connect to the phone, done.");
					PreferencesUtils.setDeviceAddress(
							BtNotificationThirdActivity.this, deviceAddress);
					if(!quit){
						quit = true;
						Intent enableIntent = new Intent(
								BtNotificationThirdActivity.this,
								BtNotificationForthActivity.class)
								.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
										| Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(enableIntent);
						BtNotificationThirdActivity.this.finish();
					}
				} else if ((hfpstate == 0) && receiving) {
					receiving = false;
//					try {
//						BtNotificationThirdActivity.this
//								.unregisterReceiver(btTelephonyReceiver);
//					} catch (IllegalArgumentException e) {
//						Log.d(TAG,
//								"btTelephonyReceiver is already unregistered");
//					}
					Log.d(TAG,
							"Failed to disconnect and connect to the phone, ask the user try again.");
					action = PAIRING;
					Log.d(TAG, " can't connect to " + deviceName);
					
					failToPair();
					
//					header.setText("PAIRING UNSUCCESSFUL");
//					emptyView
//							.setText("Failed to pair with "
//											+ deviceName
//											+ ". Would you like to try pairing this device again?");
//					lv.setVisibility(View.GONE);
//					emptyView.setVisibility(View.VISIBLE);
//					okButton.setVisibility(View.VISIBLE);
//					okButton.setEnabled(true);
//					okButton.setFocusable(true);
//					okButton.requestFocus();
//					cancelButton.setVisibility(View.VISIBLE);
//					if (progressDialog != null) {
//						progressDialog.dismiss();
//					}
				} else {
					if (hfpstate == 1)
						receiving = true;
				}
				priorState = hfpstate;
		}
	};

	private void failToPair() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}
		Log.d(TAG,
				"user canceled pairing request, the activity should be finished.");
		header.setText("PAIRING UNSUCCESSFUL");
			emptyView.setText("Failed to pair with " + deviceName
					+ ". Would you like to try pairing this device again?\n\n");
			lv.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
			nextView.setText("CANCEL");
			pairView.setText("TRY AGAIN");
			pairFails = true;
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

				} else if (intent.getExtras().getInt(
						BluetoothDevice.EXTRA_BOND_STATE) == BluetoothDevice.BOND_BONDING) {
					// hopefully pairing request pops up
				} else {// (intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE)
						// == BluetoothDevice.BOND_NONE) {
					BtNotificationThirdActivity.this.failToPair();
				}
			}
		}
	};
	
	@Override
	public void onBackPressed() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}
//		progressDialog = ProgressDialog.show(
//				BtNotificationThirdActivity.this, "",
//				"Scanning...");
		if(noDevice){
			startActivity(new Intent(BtNotificationThirdActivity.this,
					FirstConnectActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
			finish();
		}else{
			devices.clear();
			adapter.clear();
			header.setText("SCANNING...");
			lv.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
			okButton.setVisibility(View.GONE);
			cancelButton.setVisibility(View.GONE);
			selectButton.setVisibility(View.GONE);
			pairTV.setVisibility(View.GONE);
				if(mBtAdapter.isDiscovering()){
					startActivity(new Intent(BtNotificationThirdActivity.this,
								 FirstConnectActivity.class));

					finish();
				}else{
					mBtAdapter.startDiscovery();
					progressBar.setVisibility(View.VISIBLE);
					if(pairFails)
						pairFails = false;
					nextView.setText("CANCEL");
					pairView.setText("PAIR");
				}
		}

	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
			   keyCode == KeyEvent.KEYCODE_ENTER){
				if(noDevice){
					noDevice = false;
					devices.clear();
					adapter.clear();
					header.setText("SCANNING...");
					lv.setVisibility(View.VISIBLE);
					emptyView.setVisibility(View.GONE);
					okButton.setVisibility(View.GONE);
					cancelButton.setVisibility(View.GONE);
					selectButton.setVisibility(View.GONE);
					backButton.setImageResource(R.drawable.back_button);
					pairTV.setVisibility(View.GONE);
							mBtAdapter.startDiscovery();
							progressBar.setVisibility(View.VISIBLE);
							if(pairFails)
								pairFails = false;
							nextView.setText("CANCEL");
							pairView.setText("PAIR");
				}else{
					if(pairFails){
						pairFails = false;
						nextView.setText("SCAN AGAIN");
						pairView.setText("PAIR");

						if (action == PAIRING) {
							Log.d(TAG, "Pairing again...");
							if(DeviceUtils.isLimo()){
								if ((devices.size() == 1)
										|| (!"unknown".equals(PreferencesUtils
												.getDeviceName(BtNotificationThirdActivity.this)) && PreferencesUtils
												.getDeviceName(
														BtNotificationThirdActivity.this)
												.equals(PreferencesUtils
														.getBTDeviceName(BtNotificationThirdActivity.this)))) {
									header.setText("PAIRING FOR DEVICES...");
									BluetoothDevice device = null;
									if (devices.size() == 1) {
										device = devices.get(0);
									} else {
										device = mBtAdapter.getRemoteDevice(PreferencesUtils
												.getDeviceAddress(BtNotificationThirdActivity.this));
									}
									BtNotificationThirdActivity.this.pairDevice(device);
									lv.setVisibility(View.VISIBLE);
									emptyView.setVisibility(View.GONE);
									okButton.setVisibility(View.GONE);
									cancelButton.setVisibility(View.GONE);
								} else {
									Log.d(TAG, "SCANNING AGAIN...");
									mBtAdapter.startDiscovery();
									progressBar.setVisibility(View.VISIBLE);
//									progressDialog = ProgressDialog.show(
//											BtNotificationThirdActivity.this, "",
//											"Scanning...");
									devices.clear();
									adapter.clear();
									header.setText("SCANNING...");
									lv.setVisibility(View.VISIBLE);
									emptyView.setVisibility(View.GONE);
									okButton.setVisibility(View.GONE);
									cancelButton.setVisibility(View.GONE);
								}
							}else{
								Iterator<BluetoothDevice> iterator = devices.iterator();
								while (iterator.hasNext()) {
									BluetoothDevice device = iterator.next();
									if(deviceName != null && !deviceName.equals(device.getName())){
										iterator.remove();
									}
								}
								if ((devices.size() == 1)) {
									header.setText("PAIRING...");
									BluetoothDevice device = devices.get(0);
									BtNotificationThirdActivity.this.pairMfiDevice(device);
									lv.setVisibility(View.VISIBLE);
									emptyView.setVisibility(View.GONE);
									okButton.setVisibility(View.GONE);
									cancelButton.setVisibility(View.GONE);
								} else {
									Log.d(TAG, "Scanning again...");
									mBtAdapter.startDiscovery();
									
									progressBar.setVisibility(View.VISIBLE);
//									progressDialog = ProgressDialog.show(
//											BtNotificationThirdActivity.this, "",
//											"Scanning...");
									devices.clear();
									adapter.clear();
									header.setText("SCANNING...");
									nextView.setText("CANCEL");
									lv.setVisibility(View.VISIBLE);
									emptyView.setVisibility(View.GONE);
									okButton.setVisibility(View.GONE);
									cancelButton.setVisibility(View.GONE);
									selectButton.setVisibility(View.GONE);
									pairTV.setVisibility(View.GONE);
								}
							}
						} else {
						    if(DeviceUtils.isLimo()){
								Log.d(TAG, "Scanning again...");
								mBtAdapter.startDiscovery();
								progressDialog = new ProgressDialog(BtNotificationThirdActivity.this);
								progressDialog.setIndeterminate(true);
								progressDialog.setCancelable(false);
								progressDialog.show();
								progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
								TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
								textTv.setText("Scanning...");
								devices.clear();
								adapter.clear();
								header.setText("SCANNING...");
								lv.setVisibility(View.VISIBLE);
								emptyView.setVisibility(View.GONE);
								okButton.setVisibility(View.GONE);
								cancelButton.setVisibility(View.GONE);
								selectButton.setVisibility(View.GONE);
								pairTV.setVisibility(View.GONE);
							}else{
								Iterator<BluetoothDevice> iterator = devices.iterator();
								while (iterator.hasNext()) {
									BluetoothDevice device = iterator.next();
									if(deviceName != null && !deviceName.equals(device.getName())){
										iterator.remove();
									}
								}
								if ((devices.size() == 1)) {
									header.setText("PAIRING...");
									BluetoothDevice device = devices.get(0);
									BtNotificationThirdActivity.this.pairMfiDevice(device);
									lv.setVisibility(View.VISIBLE);
									emptyView.setVisibility(View.GONE);
									okButton.setVisibility(View.GONE);
									cancelButton.setVisibility(View.GONE);
								} else {
									Log.d(TAG, "Scanning again...");
									mBtAdapter.startDiscovery();
									nextView.setText("CANCEL");
									progressDialog = new ProgressDialog(BtNotificationThirdActivity.this);
									progressDialog.setIndeterminate(true);
									progressDialog.setCancelable(false);
									progressDialog.show();
									progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
									TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
									textTv.setText("Scanning...");
									devices.clear();
									adapter.clear();
									header.setText("SCANNING...");
									lv.setVisibility(View.VISIBLE);
									emptyView.setVisibility(View.GONE);
									okButton.setVisibility(View.GONE);
									cancelButton.setVisibility(View.GONE);
									selectButton.setVisibility(View.GONE);
									pairTV.setVisibility(View.GONE);
								}
							}
						}
									}
				}
				return true;
			}
		return super.onKeyUp(keyCode, event);
	}

}
// End of JIRA: MODLIVE-772
