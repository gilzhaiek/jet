/*
 * < MainActivity.java > Copyright 2011 Stonestreet One. All Rights Reserved.
 *
 * Hands-free Profile Sample for Stonestreet One Bluetooth Protocol Stack
 * Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.reconinstruments.ble_ss1.device;

import java.util.EnumSet;
import java.util.HashMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.ble_ss1.BLEUtils;
import com.reconinstruments.ble_ss1.R;
import com.reconinstruments.ble_ss1.R.id;
import com.reconinstruments.ble_ss1.R.layout;
import com.reconinstruments.ble_ss1.R.string;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.ConnectFlags;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFilter;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.GATM.AttributeProtocolErrorType;
import com.stonestreetone.bluetopiapm.GATM.ConnectionInformation;
import com.stonestreetone.bluetopiapm.GATM.ConnectionType;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.GATM.RequestErrorType;
import com.stonestreetone.bluetopiapm.GATM.ServiceDefinition;

/**
 *  GATM Generic Attribute Client Sample
 */
public class UtilActivity extends Activity {

	public static final String LOG_TAG                          = "BLE_Util";


	static final int   MESSAGE_DISPLAY_MESSAGE          = 1;
	static final int   MESSAGE_UPDATE_DEVICE      	    = 2;

	//BluetoothAddress address = new BluetoothAddress("E9:79:17:E6:E5:DD");
	
	/*BluetoothAddress[] remoteAddresses = {
			new BluetoothAddress("EF:BB:95:82:E8:DF"),//wahoo kickr
			new BluetoothAddress("FA:5B:AE:1B:1B:C8"),//wahoo csc
			new BluetoothAddress("E9:79:17:E6:E5:DD"),//wahoo hrm
			new BluetoothAddress("90:D7:EB:B1:BB:98")};//recon remote
	*/
	HashMap<BluetoothAddress, BLEDevice> devices;
	BLEDevice activeDevice;

	Resources     resourceManager;
	static Handler uiThreadMessageHandler;

	Button        bluetoothToggleButton;
	Button        scanButton;
	Button        getDevButton;
	Button        connectButton;
	Button        disconnectButton;
	TextView      deviceAddressText;
	TextView      deviceNameText;
	TextView      deviceConnectedText;
	Button        nextDevButton;
	EditText      attHandleText;
	EditText      dataText;
	Button        eNotifyButton;
	Button        dNotifyButton;

	TextView      outputLogView;
	ScrollView    logOutputScrollView;

	static void showToast(Context context, int resourceID) {
		Toast.makeText(context, resourceID, Toast.LENGTH_LONG).show();
	}
	static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	DEVM deviceManager;
	GenericAttributeClientManager genericAttributeClientManager; 

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		devices = new HashMap<BluetoothAddress,BLEDevice>();
		
		resourceManager = getResources();
		uiThreadMessageHandler = new Handler(uiThreadHandlerCallback);

		setContentView(R.layout.main);

		bluetoothToggleButton = (Button)findViewById(R.id.bluetoothToggleButton);
		scanButton = (Button)findViewById(R.id.scanButton);
		getDevButton = (Button)findViewById(R.id.getDevButton);
		connectButton = (Button)findViewById(R.id.connectButton);
		disconnectButton = (Button)findViewById(R.id.disconnectButton);
		deviceAddressText = (TextView)findViewById(R.id.deviceAddressText);
		deviceNameText = (TextView)findViewById(R.id.deviceNameText);
		deviceConnectedText = (TextView)findViewById(R.id.deviceConnectedText);
		nextDevButton = (Button)findViewById(R.id.nextDevButton);
		attHandleText = (EditText)findViewById(R.id.attHandleText);
		eNotifyButton = (Button)findViewById(R.id.eNotifyButton);
		dNotifyButton = (Button)findViewById(R.id.dNotifyButton);

		outputLogView = (TextView)findViewById(R.id.logOutputText);
		logOutputScrollView = (ScrollView)findViewById(R.id.logOutputScroller);

		if(outputLogView != null) {
			outputLogView.setText("");
		}

		bluetoothToggleButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			togglePower();}});
		scanButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			scan();}});
		getDevButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			getDeviceServices();}});
		connectButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			connect();}});
		disconnectButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			disconnect();}});
		nextDevButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			loadNextDevice();}});
		eNotifyButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			setNotify(true);}});
		dNotifyButton.setOnClickListener(new OnClickListener() {@Override public void onClick(View v) {
			setNotify(false);}});
	}

	@Override
	protected void onStart() {
		super.onStart();

		BluetoothAdapter bluetoothAdapter;

		// Register a receiver for a Bluetooth "State Changed" broadcast event.
		registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter != null) {
			if(bluetoothAdapter.isEnabled()) {
				bluetoothToggleButton.setText("BT on");
				displayMessage(getResources().getString(R.string.localAddressLogMessagePrefix) + bluetoothAdapter.getAddress());
				profileEnable();
			} else {
				bluetoothToggleButton.setText("BT off");
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		unregisterReceiver(bluetoothBroadcastReceiver);
		profileDisable();
	}

	void getDeviceServices(){
		if(activeDevice!=null){
			ServiceDefinition[] services = genericAttributeClientManager.queryRemoteDeviceServices(activeDevice.address);
			if(services==null){
				int result = deviceManager.updateRemoteLowEnergyDeviceServices(activeDevice.address);
				if(result<0) displayMessage("Error updating remote device services");
			} else {
				activeDevice.setServices(services);
				updateDeviceText();
			}
		}
	}
	
	int getAttributeHandle() {
		if(attHandleText != null) {
			String attHandleString = attHandleText.getText().toString().trim();
			try {
				return Integer.parseInt(attHandleString);
			} catch(NumberFormatException e) {
				displayMessage("invalid attribute handle: "+attHandleString);
			}
		}
		return 0;
	}
	byte[] getData() {
		if(dataText != null) {
			String dataString = dataText.getText().toString().trim();
			try {
				return BLEUtils.hexStringToByteArray(dataString);
			} catch(NumberFormatException e) {
				displayMessage("invalid hex string: "+dataString);
			}
		}
		return new byte[]{};
	}

	// This part just turns BT on and off through a button
	public void togglePower() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if(bluetoothAdapter != null) {
			if(bluetoothAdapter.isEnabled()) {
				if(bluetoothAdapter.disable() == false) {
					showToast(this, R.string.errorBluetoothNotSupportedToastMessage);
				}
			} else {
				bluetoothAdapter.enable();
			}
		} else {
			showToast(this, R.string.errorBluetoothNotSupportedToastMessage);
		}
	}

	protected void scan() {
        int result = deviceManager.startLowEnergyDeviceScan(100);
        displayMessage("startLowEnergyDeviceScan for 100 seconds result: " + result);
	}
	static int addressIndex = 0;
	private void loadNextDevice(){
		if(devices.size()>0){
			activeDevice = (BLEDevice)devices.values().toArray()[addressIndex%devices.size()];
			getBLEDeviceProperties(activeDevice);
			updateDeviceText();
			addressIndex++;
		}
	}
	public void updateDevice(BLEDevice device){
		if(device==activeDevice){
			Message.obtain(uiThreadMessageHandler, MESSAGE_UPDATE_DEVICE, "").sendToTarget();
		}
	}
	public void updateDeviceText(){
		deviceAddressText.setText(activeDevice.address.toString());
		deviceNameText.setText(activeDevice.name!=null?activeDevice.name.toString():"");
		deviceConnectedText.setText(activeDevice.connected?"Connected":"Disconnected");
		if(activeDevice.notifyHandle!=-1)
			attHandleText.setText(""+activeDevice.notifyHandle);
	}
	public void getBLEDeviceProperties(BLEDevice device){
		RemoteDeviceProperties remoteDeviceProperties = deviceManager.queryRemoteLowEnergyDeviceProperties(device.address, false);
		if(remoteDeviceProperties!=null){
			device.name = remoteDeviceProperties.deviceName;
			displayMessage("BLE device: "+device.address+" "+device.name);
			updateDevice(device);
		} else {
			displayMessage("Couldn't get device properties: "+device.address);
		}
	}
	
	protected void setNotify(boolean enable) {
		
		if(activeDevice!=null&&activeDevice.notifyHandle!=-1){
			byte[] data = enable?new byte[]{0x01,0x00}:new byte[]{0x00,0x00};
			int result = genericAttributeClientManager.writeValueWithoutResponse(activeDevice.address, activeDevice.notifyHandle, data);
			displayMessage("set notify result: "+result);
		}
	}
	protected void write() {
		int result = genericAttributeClientManager.writeValueWithoutResponse(activeDevice.address, getAttributeHandle(), getData());
		Log.d(LOG_TAG,"write result: "+result);
	}
	protected void disconnect() {
		int result = deviceManager.disconnectRemoteLowEnergyDevice(activeDevice.address,true);
		displayMessage("Attempting to disconnect from "+activeDevice.address+(result==0?"":""));		
		Log.d(LOG_TAG,"disconnect result: "+result);
	}
	protected void connect() {	
		int result = deviceManager.connectWithRemoteLowEnergyDevice(activeDevice.address,EnumSet.noneOf(ConnectFlags.class));
		displayMessage("Attempting to connect to "+activeDevice.address+(result==0?"":""));
		Log.d(LOG_TAG,"connect result: "+result);
	}
	protected void queryRemoteDeviceList() {
		BluetoothAddress[] remoteAddresses = deviceManager.queryRemoteDeviceList(RemoteDeviceFilter.ALL_DEVICES, 0);
		if (remoteAddresses == null)
			displayMessage("queryRemoteDeviceList() result: null");
		else {               	
			displayMessage("queryRemoteDeviceList(): ");
			for (BluetoothAddress remoteAddress:remoteAddresses)
			{
				displayMessage(remoteAddress.toString());
				if(!devices.containsKey(remoteAddress))
					devices.put(remoteAddress, new BLEDevice(remoteAddress));
			}
		}
	}
	public void queryConnectedDeviceList() {
		ConnectionInformation[] connectionInformation;

		connectionInformation = genericAttributeClientManager.queryConnectedDevices();

		displayMessage(resourceManager.getString(R.string.queryConnectedDevicesLabel) );

		if(connectionInformation.length==0){
			displayMessage("No Devices Connected");
		}
		else {
			for(ConnectionInformation connectionInfo : connectionInformation) {
				BLEDevice device = devices.get(connectionInfo.remoteDeviceAddress);
				device.connected = true;
				
				if(connectionInfo.connectionType==ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE){
					displayMessage("Reg BT device connected: "+connectionInfo.remoteDeviceAddress);
				}
				else if(connectionInfo.connectionType==ConnectionType.LOW_ENERGY){
					displayMessage("BLE device connected: "+connectionInfo.remoteDeviceAddress);
					getBLEDeviceProperties(devices.get(connectionInfo.remoteDeviceAddress));
				}
				updateDevice(device);
			}
		}
	}
	
	public static void displayMessage(CharSequence string) {
		Log.d(LOG_TAG,string.toString());
		Message.obtain(uiThreadMessageHandler, MESSAGE_DISPLAY_MESSAGE, string).sendToTarget();
	}
	// This part is just for displaying messages and logs on ui
	private final Handler.Callback uiThreadHandlerCallback = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			final String stringValue;

			switch(msg.what) {
			case MESSAGE_DISPLAY_MESSAGE:
				if(msg.obj != null) {
					stringValue = msg.obj.toString();
					outputLogView.append("\n");
					outputLogView.append(stringValue);

					logOutputScrollView.post(new Runnable() {
								@Override
								public void run() {
									logOutputScrollView.smoothScrollBy(0, outputLogView.getBottom());
								}});

					if(Log.isLoggable(LOG_TAG, Log.INFO) && (stringValue.length() > 0)) {
						Log.i(LOG_TAG, stringValue);
					}
				}
				return true;
			case MESSAGE_UPDATE_DEVICE:
				updateDeviceText();
			}
			return false;
		}
	};

	// The important part here is profileEnable and profileDisable calls
	private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				switch(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
				case BluetoothAdapter.STATE_TURNING_ON:
					bluetoothToggleButton.setText("enabling");
					break;

				case BluetoothAdapter.STATE_ON:
					bluetoothToggleButton.setText("BT on");
					profileEnable();
					break;

				case BluetoothAdapter.STATE_TURNING_OFF:
					bluetoothToggleButton.setText("disabling");
					profileDisable();
					break;

				case BluetoothAdapter.STATE_OFF:
					bluetoothToggleButton.setText("BT off");
					break;

				case BluetoothAdapter.ERROR:
				default:
					displayMessage(resourceManager.getString(R.string.bluetoothUnknownStateToastMessage));
				}
			}
		}
	};

	private final ClientEventCallback genericAttributeClientEventCallback = new ClientEventCallback() {

		@Override
		public void connectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {
			BLEDevice device = devices.get(remoteDeviceAddress);
			device.connected = true;
			updateDevice(device);
			
			displayMessage("connectedEvent");
			displayMessage(resourceManager.getString(R.string.connectedEventLabel));
			displayMessage(resourceManager.getString(R.string.bluetoothAddressLabel)+": "+remoteDeviceAddress);
			displayMessage(resourceManager.getString(R.string.connectionTypeLabel)+": "+connectionType);
			displayMessage(resourceManager.getString(R.string.MTULabel)+": "+MTU);
			
			getDeviceServices();
		}

		@Override
		public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress) {
			BLEDevice device = devices.get(remoteDeviceAddress);
			device.connected = false;
			updateDevice(device);
			
			displayMessage("disconnectedEvent");

			displayMessage(resourceManager.getString(R.string.disconnectedEventLabel));
			displayMessage(resourceManager.getString(R.string.bluetoothAddressLabel)+": "+remoteDeviceAddress);
			displayMessage(resourceManager.getString(R.string.connectionTypeLabel)+": "+connectionType);
		}

		@Override
		public void connectionMTUUpdateEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {
			displayMessage("connectionMTUUpdateEvent");

			displayMessage(resourceManager.getString(R.string.connectionMTUUpdateEventLabel)+": "+MTU);
			displayMessage(resourceManager.getString(R.string.bluetoothAddressLabel)+": "+remoteDeviceAddress);
			displayMessage(resourceManager.getString(R.string.connectionTypeLabel)+": "+connectionType);
		}

		@Override
		public void handleValueEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean handleValueIndication, int attributeHandle, byte[] attributeValue) {
			displayMessage("handleValueEvent");

			//displayMessage(resourceManager.getString(R.string.handleValueEventLabel));
			displayMessage(resourceManager.getString(R.string.bluetoothAddressLabel)+": "+remoteDeviceAddress);
			//displayMessage(resourceManager.getString(R.string.connectionTypeLabel)+": "+connectionType);
			displayMessage(resourceManager.getString(R.string.handleValueIndicationLabel)+": "+handleValueIndication);
			displayMessage(resourceManager.getString(R.string.attributeHandleLabel)+": "+Integer.toHexString(attributeHandle));

			displayMessage(resourceManager.getString(R.string.attributeValueLabel)+": "+BLEUtils.byteArrayToHex(attributeValue));
		}

		@Override
		public void readResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, boolean isFinal, byte[] value) {
			displayMessage("readResponseEvent");

			displayMessage(resourceManager.getString(R.string.readResponseEventLabel));
			displayMessage(resourceManager.getString(R.string.bluetoothAddressLabel)+": "+remoteDeviceAddress);
			displayMessage(resourceManager.getString(R.string.connectionTypeLabel)+": "+connectionType);
			displayMessage(resourceManager.getString(R.string.transactionIDLabel)+": "+transactionID);
			displayMessage(resourceManager.getString(R.string.handleLabel)+": "+Integer.toHexString(handle));

			displayMessage(resourceManager.getString(R.string.isFinalLabel)+": "+isFinal);

			displayMessage(resourceManager.getString(R.string.attributeValueLabel)+": "+BLEUtils.byteArrayToHex(value));
		}

		@Override
		public void writeResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle) {
			displayMessage("writeResponseEvent");

			displayMessage(resourceManager.getString(R.string.writeResponseEventLabel));
			displayMessage(resourceManager.getString(R.string.bluetoothAddressLabel)+": "+remoteDeviceAddress);
			displayMessage(resourceManager.getString(R.string.connectionTypeLabel)+": "+connectionType);
			displayMessage(resourceManager.getString(R.string.transactionIDLabel)+": "+transactionID);
			displayMessage(resourceManager.getString(R.string.handleLabel)+": "+Integer.toHexString(handle));
		}

		@Override
		public void errorResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, RequestErrorType requestErrorType, AttributeProtocolErrorType attributeProtoccolErrorType) {
			displayMessage("errorResponseEvent");

			displayMessage(resourceManager.getString(R.string.errorResponseEventLabel)+": "+requestErrorType);
			displayMessage(resourceManager.getString(R.string.bluetoothAddressLabel)+": "+remoteDeviceAddress);
			displayMessage(resourceManager.getString(R.string.connectionTypeLabel)+": "+connectionType);
			displayMessage(resourceManager.getString(R.string.transactionIDLabel)+": "+transactionID);
			displayMessage(resourceManager.getString(R.string.handleLabel)+": "+Integer.toHexString(handle));

			if(requestErrorType == RequestErrorType.ERROR_RESPONSE) {
				displayMessage(resourceManager.getString(R.string.attributeProtoccolErrorTypeLabel)+": "+attributeProtoccolErrorType);
			}
		}
	};

	private final void profileEnable() {
		synchronized(this) {
			try {
				deviceManager = new DEVM(deviceEventCallback);
				genericAttributeClientManager = new GenericAttributeClientManager(genericAttributeClientEventCallback);
				
				queryRemoteDeviceList();
				queryConnectedDeviceList();
				scan();
				loadNextDevice();
			} catch(Exception e) {
				// BluetopiaPM server couldn't be contacted. This should never
				// happen if Bluetooth was successfully enabled.
				Log.e(LOG_TAG, "Couldn't reach bluetooth server",e);
				showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
				BluetoothAdapter.getDefaultAdapter().disable();
			}
		}
	}
	private final void profileDisable() {
		synchronized(UtilActivity.this) {
			if(genericAttributeClientManager != null) {
				genericAttributeClientManager.dispose();
				genericAttributeClientManager = null;
			}
			if(deviceManager != null){
				deviceManager.dispose();
				deviceManager = null;
			}
			displayMessage("profile disabled, no ble services available");
		}
	}
	private final EventCallback deviceEventCallback = new EventCallback() {
		@Override
		public void devicePoweredOnEvent() { displayMessage("devicePoweredOnEvent"); }
		@Override
		public void devicePoweringOffEvent(int poweringOffTimeout) { displayMessage("devicePoweringOffEvent"); }
		@Override
		public void devicePoweredOffEvent() { displayMessage("devicePoweredOffEvent"); }
		@Override
		public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields) {
			displayMessage("localDevicePropertiesChangedEvent");
			//displayMessage(changedFields.toString());
			for(LocalPropertyField localPropertyField: changedFields) {
				switch(localPropertyField) {
				case DISCOVERABLE_MODE:
					if(localProperties.discoverableMode)
						displayMessage("Device is Discoverable");
					else
						displayMessage("Device is Not Discoverable");

					displayMessage("Discoverable Mode Timeout: " + localProperties.discoverableModeTimeout);
					break;
				case CLASS_OF_DEVICE:
				case DEVICE_NAME:
				case CONNECTABLE_MODE:
				case PAIRABLE_MODE:
				case DEVICE_FLAGS:
					break;
				}
			}
		}
		@Override
		public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) { 
			displayMessage("remoteDeviceServicesStatusEvent"); 
		}
		@Override
		public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
			displayMessage("remoteLowEnergyDeviceServicesStatusEvent");

			if(success){
				ServiceDefinition[] services = genericAttributeClientManager.queryRemoteDeviceServices(remoteDevice);
				if(services==null){
					displayMessage("Error getting known services!");
				} else {
					BLEDevice device = devices.get(remoteDevice);
					device.setServices(services);
					updateDevice(device);
				}
			}
		}
		@Override
		public void discoveryStartedEvent() { displayMessage("discoveryStartedEvent"); }
		@Override
		public void discoveryStoppedEvent() { displayMessage("discoveryStoppedEvent"); }
		// We need this
		@Override
		public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {
			displayMessage("remoteDeviceFoundEvent");
			if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
				displayMessage("found BLE device: "+deviceProperties.deviceAddress+" "+deviceProperties.deviceName);
				
				BLEDevice device = devices.get(deviceProperties.deviceAddress);
				if(device==null){
					device = new BLEDevice(deviceProperties);
					devices.put(deviceProperties.deviceAddress, device);
				} else {
					device.name = deviceProperties.deviceName;
				}
				updateDevice(device);
			}
		}

		@Override
		public void remoteDevicePropertiesStatusEvent(boolean success, RemoteDeviceProperties deviceProperties) {
			displayMessage("remoteDevicePropertiesStatusEvent");
			BLEDevice device = devices.get(deviceProperties.deviceAddress);
			if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
				displayMessage(
						resourceManager.getString(R.string.remoteDevicePropertiesStatusEventLabel)+": "+(success?"success":"failure"));

				displayMessage("Device Address    : " + deviceProperties.deviceAddress.toString());
				displayMessage("Class of Device   : " + Integer.toHexString(deviceProperties.classOfDevice));
				displayMessage("Device Name       : " + deviceProperties.deviceName);
				displayMessage("RSSI              : " + deviceProperties.rssi);
				displayMessage("Transmit power    : " + deviceProperties.transmitPower);
				displayMessage("Sniff Interval    : " + deviceProperties.sniffInterval);

				if(deviceProperties.applicationData != null) {
					displayMessage("Friendly Name     : " + deviceProperties.applicationData.friendlyName);
					displayMessage("Application Info  : " + deviceProperties.applicationData.applicationInfo);
				}

				for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
					displayMessage("Remote Device Flag: " + remoteDeviceFlag.toString());
				
				device.name = deviceProperties.deviceName;
				device.connected = deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE);
			}
			updateDevice(device);
		}
		// We need this one.
		@Override
		public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties deviceProperties, EnumSet<RemotePropertyField> changedFields) {
			displayMessage("remoteDevicePropertiesChangedEvent: "+deviceProperties.deviceAddress);
			BLEDevice device = devices.get(deviceProperties.deviceAddress);
			
			displayMessage("flags: "+deviceProperties.remoteDeviceFlags.toString());
			
			if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
				
				for(RemotePropertyField remotePropertyField: changedFields) {
					displayMessage(remotePropertyField.name()+" changed");
					
					switch(remotePropertyField) {
					case DEVICE_FLAGS:
						break;
					case CLASS_OF_DEVICE:
						break;
					case DEVICE_NAME:
						device.name = deviceProperties.deviceName;
						break;
					case APPLICATION_DATA:
						break;
					case RSSI:
						break;
					case PAIRING_STATE:
						if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_CONNECTED)){
							displayMessage("Device Connected");
							device.connected = true;
						}
						else {
							displayMessage("Device Unconnected");
							device.connected = false;
						}
						break;
					case CONNECTION_STATE:
						if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_CONNECTED)){
							displayMessage("Device Connected");
							device.connected = true;
						}
						else {
							displayMessage("Device Unconnected");
							device.connected = false;
						}
						break;
					case ENCRYPTION_STATE:
						break;
					case SNIFF_STATE:
						break;
					case SERVICES_STATE:
						for(RemoteDeviceFlags searchServices: deviceProperties.remoteDeviceFlags) {
							if(searchServices == RemoteDeviceFlags.SERVICES_KNOWN) {
								displayMessage("Services are Known");
							} else {
								displayMessage("Services are not Known");
							}
						}
						break;
					}
				}
			}
			updateDevice(device);
		}

		@Override
		public void remoteDevicePairingStatusEvent(BluetoothAddress remoteDevice, boolean success, int authenticationStatus) {
			displayMessage("remoteDevicePairingStatusEvent: "+remoteDevice);
			displayMessage(resourceManager.getString(R.string.remoteDevicePairingStatusEventLabel)+": "+(success?"success":"failure"));
			displayMessage("Authentication Status: " + (authenticationStatus & 0x7FFFFFFF) + (((authenticationStatus & 0x80000000) != 0) ? " (LE)" : "(BR/EDR)"));
		}

		@Override
		public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {
			displayMessage("remoteDeviceEncryptionStatusEvent");
			StringBuilder sb = new StringBuilder();

			sb.append(resourceManager.getString(R.string.remoteDeviceEncryptionStatusEventLabel)).append(": ");
			sb.append(remoteDevice.toString());
			displayMessage(sb);
			displayMessage("Encryption Status: " + status);
		}

		@Override
		public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice) {
			displayMessage("remoteDeviceDeletedEvent");
		}

		@Override
		public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status) {
			displayMessage("remoteDeviceConnectionStatusEvent");
			displayMessage("Connection Status for "+remoteDevice.toString()+" : " + status);
		}

		@Override
		public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status) {
			displayMessage("remoteDeviceConnectionStatusEvent");
			displayMessage("Authentication Status for "+remoteDevice.toString()+" : " + status);
		}

		@Override
		public void deviceScanStartedEvent() {
			displayMessage("deviceScanStartedEvent");
		}

		@Override
		public void deviceScanStoppedEvent() {
			displayMessage("deviceScanStoppedEvent");
		}

		@Override
		public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress PriorResolvableDeviceAddress, BluetoothAddress CurrentResolvableDeviceAddress) {
			displayMessage("remoteLowEnergyDeviceAddressChangedEvent");

			displayMessage("PriorResolvableDeviceAddress = "+PriorResolvableDeviceAddress.toString());
			displayMessage("CurrentResolvableDeviceAddress = "+CurrentResolvableDeviceAddress.toString());
		}

		@Override
		public void deviceAdvertisingStarted() {
		// TODO Auto-generated method stub

		}

		@Override
		public void deviceAdvertisingStopped() {
		// TODO Auto-generated method stub

		}
	};

}

