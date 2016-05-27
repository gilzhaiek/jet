package com.reconinstruments.connectdevice;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.reconinstruments.phone.service.IPhoneRelayService;
import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.connectdevice.ios.BtNotificationFivthActivity;
import com.reconinstruments.connectdevice.ios.BtNotificationForthActivity;
import com.reconinstruments.connectdevice.ios.BtNotificationFristActivity;
import com.reconinstruments.connectdevice.ios.BtNotificationThirdActivity;
import com.reconinstruments.connectdevice.ios.MfiReconnectActivity;
import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.hudservice.IHUDService;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;

import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.utils.DeviceUtils;


public class ConnectionActivity extends Activity {
	protected static final String TAG = "ConnectionActivity";

	protected static boolean cancel = false;
	protected static boolean priorState = false;
	protected static int activities = 0;
	protected static boolean started = false;
	protected static boolean displayed = false;

	private IOSRemoteStatusReceiver miOSRemoteStatusReceiver = new IOSRemoteStatusReceiver();
	protected static int mapStatus = 0;
	protected static int hfpStatus = 0;
	protected static int from = 0; // 0 means that it was launched
				       // regularly. 1 means it is
				       // reconnecting after bootup. 2 means it is regular reconnecting.
	private static int previousConnectionState = -1; // to prevent calling reconnect activity again and again.

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
			startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
			overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
		}
		
		activities++;
		Log.d(TAG, "onCreate");
		Log.d(TAG, "The number of activity is:" + activities);

		if(DeviceUtils.isLimo()){
			registerReceiver(phoneConnectionReceiver, new IntentFilter(
					BTCommon.MSG_STATE_UPDATED));
			registerReceiver(miOSRemoteStatusReceiver, new IntentFilter(
					"com.reconinstruments.IOS_REMOTE_STATUS"));
			initService_ble();
			initService_phone();
		}
		Log.d(TAG, "registerReceiver hudServiceBroadcastReceiver");
		registerReceiver(hudServiceBroadcastReceiver, new IntentFilter(
									       "HUD_STATE_CHANGED"));
		initService_hudService();
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (cancel)
			finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(DeviceUtils.isLimo()){
			releaseService_ble();
			releaseService_phone();
			try{
				unregisterReceiver(phoneConnectionReceiver);
				unregisterReceiver(miOSRemoteStatusReceiver);
			}catch(IllegalArgumentException e){
				//ignore
			}
		}
		releaseService_hudService();
		try{
		    Log.d(TAG, "unregisterReceiver hudServiceBroadcastReceiver");
		    unregisterReceiver(hudServiceBroadcastReceiver);
		}catch(IllegalArgumentException e){
		    //ignore
		}
		activities--;
		Log.d(TAG, "onDestroy");
		Log.d(TAG, "The number of activity is:" + activities);
		// when the entire app has no activities, reset cancel flag
		if (activities == 0)
			cancel = false;
	}

	private BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "action: " + intent.getAction());

			if (intent.getAction().equals(BTCommon.MSG_STATE_UPDATED)) {
				boolean isConnected = intent
						.getBooleanExtra("connected", false);
				Log.d(TAG, priorState ? "Phone connected previously"
						: "Phone disconnected previously");
				Log.v(TAG, isConnected ? "Phone Connected"
						: "Phone Disconnected");

				if (isConnected && (isConnected != priorState)) {
					if (BTCommon.isConnected(ConnectionActivity.this)) {
						ConnectionActivity.this.finish();
					} else {
						mapStatus = ConnectionActivity.this.getMapStatus();
						hfpStatus = ConnectionActivity.this.getHfpStatus();
						Log.d(TAG, "mapStatus = " + mapStatus
								+ " and hfpStatus = " + hfpStatus);
						if (!ConnectionActivity.this.isTheSameDevice()) {
							mapStatus = 0;
							hfpStatus = 0;
							ConnectionActivity.this.disconnectHfp();
							ConnectionActivity.this.disconnectMap();
						}
						ConnectionActivity.this.sendXmlToiPhone();
						if (ConnectionActivity.this.readIOSRemoteStatus() > 0) {
							if (((mapStatus == 2) && (hfpStatus == 2))) {
								if (ConnectionActivity.from == 0) {
									Log.d(TAG,
											"PhoneConnectionReceiver: Go to the final step to enable the notification.");
									startActivity(new Intent(
											ConnectionActivity.this,
											BtNotificationForthActivity.class)
											.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
													| Intent.FLAG_ACTIVITY_NEW_TASK));
									overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
								} else {
									Log.d(TAG,
											"PhoneConnectionReceiver: Skip to enable call and sms on Mod Live for iPhone only.");
									setupiOSDevice();

									ConnectionActivity.this.sendNotification();

									// when quit from this point, the BT
									// connection would be broken, have to call
									// these methods again to bring up
									ConnectionActivity.this
											.connectToHfp(PreferencesUtils
													.getDeviceAddress(context));
									ConnectionActivity.this
											.connectToMap(PreferencesUtils
													.getDeviceAddress(context));

								}
								ConnectionActivity.this.finish();
							} else if (((ConnectionActivity.hfpStatus != 2) || (ConnectionActivity.mapStatus != 2))
									&& ConnectionActivity.this.isTheSameDevice() && from != 0) {
//								Log.d(TAG,
//										"IOSRemoteStatusReceiver: Go into the pairing step to enable call and sms on Mod Live for iPhone only.");
//								startActivity(new Intent(
//										ConnectionActivity.this,
//										BtNotificationThirdActivity.class)
//										.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//												| Intent.FLAG_ACTIVITY_NEW_TASK));
//								overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
//								ConnectionActivity.this.finish();
								
								// If device reconnecting, just connect to HFP and MAP and then quit
								ConnectionActivity.this.sendNotification();
								setupiOSDevice();

								// when quit from this point, the BT
								// connection would be broken, have to call
								// these methods again to bring up
								ConnectionActivity.this
										.connectToHfp(PreferencesUtils
												.getDeviceAddress(context));
								ConnectionActivity.this
										.connectToMap(PreferencesUtils
												.getDeviceAddress(context));
								ConnectionActivity.this.finish();
							}

//						} else {
//							if (ConnectionActivity.this.readIOSRemoteStatus() > -1) { // ignore
//																						// unknown
//																						// state
//								if (!displayed) {
//									Toast.makeText(
//											context,
//											"Please confirm the remote has paired the phone already",
//											Toast.LENGTH_LONG).show();
//									displayed = true;
//								}
//							}
						}
					}
				}
				priorState = isConnected;
			}
		}

	};

	private void sendNotification(String message) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon,
				message, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ConnectionActivity.class), 0);
		notification.setLatestEventInfo(this, "Setup",
				message, contentIntent);
		notificationManager.notify(654654, notification);
		notificationManager.cancel(654654);

	}

	private void sendNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon,
				"iPhone Setup Complete", System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ConnectionActivity.class), 0);
		notification.setLatestEventInfo(this, "iPhone",
				"iPhone Setup Complete", contentIntent);
		notificationManager.notify(654654, notification);
		notificationManager.cancel(654654);

	}

	protected boolean isTheSameDevice() {
		if (!"unknown".equals(PreferencesUtils.getDeviceName(this))
				&& !"".equals(PreferencesUtils.getDeviceName(this))
				&& PreferencesUtils.getDeviceName(this).equals(
						PreferencesUtils.getBTDeviceName(this))) {
			return true;
		}
		return false;
		// String bleName = getBLEDeviceName();
		// String btName = getBTDeviceName();
		// if(bleName != null && btName != null && bleName.equals(btName)){
		// return true;
		// }
		// return false;
	}

	protected String getBLEDeviceName() {
		try {
			if (bleService != null) {
				String test = bleService.getiOSDeviceName();
				Log.d(TAG, "BLE Device name: " + test);
				return test;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected int readIOSRemoteStatus() {
		try {
			if (bleService != null) {
				int result = bleService.getiOSRemoteStatus();
				return result;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return -1; // Equivalent to unknown
	}

	// /////////////////////////// BLE Service Connection /////////////////////
	// ////////////////////////////////////////////////////
	// aidl service connection.
	// ///////////////////////////////////////////////////
	protected static IBLEService bleService;
	protected BLEServiceConnection bleServiceConnection;

	protected void initService_ble() {
		if (bleServiceConnection == null) {
			bleServiceConnection = new BLEServiceConnection();
			Intent i = new Intent("RECON_BLE_TEST_SERVICE");
			bindService(i, bleServiceConnection, Context.BIND_AUTO_CREATE);
		}
	}

	protected void releaseService_ble() {
		// unregister:
		try {
			if (bleService != null) {
				String test = bleService.getiOSDeviceName();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		// unbind:
		if (bleServiceConnection != null) {
			unbindService(bleServiceConnection);
			bleServiceConnection = null;
			Log.d(TAG, "unbindService()");
		}
	}

	protected class BLEServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			Log.d(TAG, "onServiceConnected_ble");
			bleService = IBLEService.Stub.asInterface((IBinder) boundService);
			try {
				if (bleService != null) {
					String test = bleService.getiOSDeviceName();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			bleService = null;
			Log.d(TAG, "onServiceDisconnected_ble");
		}
	};

	// ////////////////// End of aidl shit///////////////////////

	private class IOSRemoteStatusReceiver extends BroadcastReceiver {
		private static final int IOS_REMOTE_STATUS_UNKNOWN = -1;
		private static final int IOS_REMOTE_STATUS_CONNECTED = 2;
		private static final int IOS_REMOTE_STATUS_DISCONNECTED = 0;
		private static final int IOS_REMOTE_STATUS_VIRTUAL = 3;
		private int miOSRemoteStatus = IOS_REMOTE_STATUS_UNKNOWN;
		private Bundle bundle;
		private String priorMessage = "";
		private String message = "";

		public IOSRemoteStatusReceiver() {
		}

		@Override
		public void onReceive(Context c, Intent i) {
			bundle = i.getExtras();
			message = bundle.getString("message");

			if (!priorMessage.equals(message)) {
				Log.d(TAG, "message=" + message);
				priorMessage = message;
				String remoteStatus = getRemoteStatus(message);
				int intRemoteStatus = IOS_REMOTE_STATUS_UNKNOWN;
				if (remoteStatus == null) {
					intRemoteStatus = IOS_REMOTE_STATUS_UNKNOWN;
				} else if (remoteStatus.equals("connected")) {
					intRemoteStatus = IOS_REMOTE_STATUS_CONNECTED;
				} else if (remoteStatus.equals("disconnected")) {
					intRemoteStatus = IOS_REMOTE_STATUS_DISCONNECTED;
				} else if (remoteStatus.equals("virtual")) {
					intRemoteStatus = IOS_REMOTE_STATUS_VIRTUAL;
				}

				miOSRemoteStatus = intRemoteStatus;
				if ((miOSRemoteStatus == IOS_REMOTE_STATUS_CONNECTED || miOSRemoteStatus == IOS_REMOTE_STATUS_VIRTUAL)
						&& !started) {
					// JIRA: MODLIVE-772 Implement bluetooth connection wizard
					// on MODLIVE
					started = true;
					if (((ConnectionActivity.hfpStatus == 2) && (ConnectionActivity.mapStatus == 2))
							&& ConnectionActivity.this.isTheSameDevice()) {
						if (ConnectionActivity.from == 0) {
							startActivity(new Intent(ConnectionActivity.this,
									BtNotificationForthActivity.class)
									.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
											| Intent.FLAG_ACTIVITY_NEW_TASK));
							overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
						} else {
							Log.d(TAG,
									"IOSRemoteStatusReceiver: Skip to enable call and sms on Mod Live for iPhone only.");
							setupiOSDevice();

							ConnectionActivity.this.sendNotification();
							ConnectionActivity.this.sendConnectedXmlToiPhone();
							// when quit from this point, the BT connection
							// would be broken, have to call these methods again
							// to bring up
							String address = PreferencesUtils
									.getDeviceAddress(c);
							if (!"unknown".equals(address) && !"".equals(address)){
								ConnectionActivity.this
								.connectToHfp(address);
								ConnectionActivity.this
								.connectToMap(address);
							}

						}
						ConnectionActivity.this.finish();
					} else if (((ConnectionActivity.hfpStatus != 2) || (ConnectionActivity.mapStatus != 2))
							&& ConnectionActivity.this.isTheSameDevice() && from != 0) {
//						Log.d(TAG,
//								"IOSRemoteStatusReceiver: Go into the pairing step to enable call and sms on Mod Live for iPhone only.");
//						startActivity(new Intent(ConnectionActivity.this,
//								BtNotificationThirdActivity.class)
//								.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//										| Intent.FLAG_ACTIVITY_NEW_TASK));
//						overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
//						ConnectionActivity.this.finish();

						setupiOSDevice();

						// If device reconnecting, just connect to HFP and MAP and then quit
						ConnectionActivity.this.sendNotification();
						// when quit from this point, the BT connection
						// would be broken, have to call these methods again
						// to bring up
						String address = PreferencesUtils
								.getDeviceAddress(c);
						if (!"unknown".equals(address) && !"".equals(address)){
							ConnectionActivity.this
									.connectToHfp(address);
							ConnectionActivity.this
									.connectToMap(address);
						}
						ConnectionActivity.this.sendConnectedXmlToiPhone();
						ConnectionActivity.this.finish();
					} else {
						Log.d(TAG,
								"IOSRemoteStatusReceiver: Go into the next step to enable call and sms on Mod Live for iPhone only.");
						startActivity(new Intent(ConnectionActivity.this,
								BtNotificationFristActivity.class)
								.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
										| Intent.FLAG_ACTIVITY_NEW_TASK));
						overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
						ConnectionActivity.this.finish();
					}
//				} else {
//					if (ConnectionActivity.this.readIOSRemoteStatus() > -1) { // ignore
//																				// unknown
//																				// state
//						if (!displayed) {
//							Toast.makeText(
//									c,
//									"Please confirm the remote has paired the phone already",
//									Toast.LENGTH_LONG).show();
//							displayed = true;
//						}
//					}
				}

			}
		}

		private boolean requestIOSRemoteStatus() {
			try {
				if (bleService != null) {
					int result = bleService.sendControlByte((byte) 0x11); // That
																			// is
																			// for
																			// remote
					return true;
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return false;
		}

		private String getRemoteStatus(String message) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder db = dbf.newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(message));

				Document doc = db.parse(is);
				NodeList nodes = doc.getElementsByTagName("remote");
				Node rootNode = nodes.item(0);
				NamedNodeMap nnm = rootNode.getAttributes();
				Node n = nnm.getNamedItem("status");
				String type = n.getNodeValue();
				return type;

			} catch (Exception e) {
				Log.e(TAG, "Failed to parse xml", e);
			}
			return null;
		}
	}

	// this method is used by some points that assume the hfp and map state are connected.
	protected void sendConnectedXmlToiPhone() {
		// Use helper function to generate the xml
		String theXml = generateBtStatXml(2, 2);
		// push the Xml to BLE
		boolean resultbool = pushXmlString(theXml);
		String result = (resultbool) ? "success" : "failed";
		Log.d(TAG, "sendXmlToiPhone() result = " + result);

	}

	protected void sendXmlToiPhone() {
		int result_hfp = hfpStatus;
		int result_map = mapStatus;
		// Use helper function to generate the xml
		String theXml = generateBtStatXml(result_hfp, result_map);
		// push the Xml to BLE
		boolean resultbool = pushXmlString(theXml);
		String result = (resultbool) ? "success" : "failed";
		Log.d(TAG, "sendXmlToiPhone() result = " + result);

	}

	protected boolean pushXmlString(String xmlString) {
		try {
			if (bleService != null) {
				int result = bleService.pushXml(xmlString);
				return (result == 0);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected String generateBtStatXml(int hfpstat, int mapstat) {
		String hfpString = "disconnected";
		String mapString = "disconnected";

		if (hfpstat == 2) {
			hfpString = "connected";
		} else if (hfpstat == 1) {
			hfpString = "connecting";
		}

		if (mapstat == 2) {
			mapString = "connected";
		} else if (mapstat == 1) {
			mapString = "connecting";
		}

		String theXmlString = "<recon intent=\"bt_update_status\"><hfp state=\""
				+ hfpString + "\"/><map state=\"" + mapString + "\"/></recon> ";
		Log.d(TAG, "theXmlString: " + theXmlString);
		return theXmlString;

	}

	protected boolean disconnectMap() {
		try {
			if (phoneRelayService != null) {
				boolean test = phoneRelayService.remoteDisconnectMapDevice();
				return test;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected boolean disconnectHfp() {
		try {
			if (phoneRelayService != null) {
				boolean test = phoneRelayService.remoteDisconnectHfpDevice();
				return test;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	// For SMS
	// 0 means not connected, 1 means connecting, 2 means connected
	protected int getMapStatus() {
		try {
			if (phoneRelayService != null) {
				int test = phoneRelayService.getMapStatus();
				mapStatus = test;
				Log.d(TAG, "phoneRelayService.getMapStatus() = " + test);
				return test;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return -1;
	}

	// For call relay
	protected int getHfpStatus() {
		try {
			if (phoneRelayService != null) {
				int test = phoneRelayService.getHfpStatus();
				hfpStatus = test;
				Log.d(TAG, "phoneRelayService.getHfpStatus() = " + test);
				return test;
			} else {
				Log.d(TAG, "phoneRelayService == null");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return -1;
	}

	protected boolean connectToHfp(String address) {
		try {
			if (phoneRelayService != null) {
				boolean test = phoneRelayService
						.remoteConnectToHfpDevice(address);
				return test;
			} else {
				Log.d(TAG, "phoneRelayService == null");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;

	}

	protected boolean connectToMap(String address) {
		try {
			if (phoneRelayService != null) {
				boolean test = phoneRelayService
						.remoteConnectToMapDevice(address);
				return test;
			} else {
				Log.d(TAG, "phoneRelayService == null");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected String getBTDeviceName() {
		try {
			if (phoneRelayService != null) {
				String test = phoneRelayService.getBluetoothDeviceName();
				Log.d(TAG, "Bluetooth Device name: " + test);
				return test;
			} else {
				Log.d(TAG, "phoneRelayService == null");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;

	}

	// /////////////////////////// Phone Relay Service Connection
	// /////////////////////
	// ////////////////////////////////////////////////////
	// aidl service connection.
	// ///////////////////////////////////////////////////
	protected static IPhoneRelayService phoneRelayService;
	protected PhoneRelayServiceConnection phoneRelayServiceConnection;

	protected void initService_phone() {
		if (phoneRelayServiceConnection == null) {
			phoneRelayServiceConnection = new PhoneRelayServiceConnection();
			Intent i = new Intent("RECON_PHONE_RELAY_SERVICE");
			bindService(i, phoneRelayServiceConnection,
					Context.BIND_AUTO_CREATE);
		}
	}

	protected void releaseService_phone() {
		if (phoneRelayServiceConnection != null) {
			unbindService(phoneRelayServiceConnection);
			phoneRelayServiceConnection = null;
			Log.d(TAG, "unbindService()");
		}
	}

	protected class PhoneRelayServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			Log.d(TAG, "onServiceConnected_phone");
			phoneRelayService = IPhoneRelayService.Stub
					.asInterface((IBinder) boundService);

		}

		public void onServiceDisconnected(ComponentName className) {
			phoneRelayService = null;
			Log.d(TAG, "onServiceDisconnected_phone");
		}
	};
	
	
	//the following method is customed for jet.
	
	public enum DeviceType {
		ANDROID, IOS
	}
	
	// 0 disconnected, 1 connecting, 2 connected
	int connectionState = 0;

	protected static IHUDService hudService;
	HUDServiceConnection hudServiceConnection;

	public void disconnect(DeviceType deviceType){
    	Log.d(TAG, "Send disconnect request to HUDService");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.request.disconnect"); // This is AS IF the phone has requested disconnect.
		i.putExtra("deviceType", deviceType.name());
		sendBroadcast(i);
    }
	
	public void connect(String address, DeviceType deviceType){
    	Log.d(TAG, "Send connect request to HUDService");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.connect");
		i.putExtra("address", address);
		i.putExtra("deviceType", deviceType.name());
		sendBroadcast(i);
    }
	
	public void killHUDService(){
    	Log.d(TAG, "Send kill request to HUDService");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.kill");
		sendBroadcast(i);
    }
    
//    int getConnectionState(){
//    	if(hudService == null){
//			connectionState = BTPropertyReader.getBTConnectionState(this);
//    	}
//    	return connectionState;
//    }
	
	private BroadcastReceiver hudServiceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			//device connection state changed event
			if(intent.getAction().equals("HUD_STATE_CHANGED")){
				connectionState = intent.getIntExtra("state", 0);
				if(connectionState == 0){ // 0 disconnected
//					if(deviceType == 0){// android device
//						Toast.makeText(
//								ConnectionActivity.this,
//								"Connecting to " + BTPropertyReader.getBTConnectedDeviceName(ConnectionActivity.this) + " fails, please check your bluetooth settings and try again later",
//								Toast.LENGTH_LONG).show();
//					}else{ // ios device
//						
//					}
					if(PreferencesUtils.isReconnect(ConnectionActivity.this) && (previousConnectionState == -1)){
						previousConnectionState = connectionState;
						Intent reconnectIntent = new Intent(ConnectionActivity.this, MfiReconnectActivity.class);
						reconnectIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
						reconnectIntent.putExtra("fails", true);
						startActivity(reconnectIntent);
						overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
						ConnectionActivity.this.finish();
					}

				}else if(connectionState == 1) { // 1 connecting
						int deviceType = BTPropertyReader.getBTConnectedDeviceType(ConnectionActivity.this);
						if(deviceType == 0){ // android device
						}else{
							//TODO do more extra thing on iOS
						    //enableCallAndTextSS1(); Disable this line for now. It has to be moved. to its activity
//							ConnectionActivity.this.sendNotification("Setup Complete");
//							ConnectionActivity.this.finish();
							
							
//							new CountDownTimer(5 * 1000, 1000) {
//								public void onTick(long millisUntilFinished) {
//								}
//								public void onFinish() {
							PreferencesUtils.setLastPairedDeviceName(ConnectionActivity.this, BTPropertyReader.getBTConnectedDeviceName(ConnectionActivity.this));
							PreferencesUtils.setLastPairedDeviceAddress(ConnectionActivity.this, BTPropertyReader.getBTConnectedDeviceAddress(ConnectionActivity.this));
							PreferencesUtils.setLastPairedDeviceType(ConnectionActivity.this, 1);
							if(!PreferencesUtils.isReconnect(ConnectionActivity.this)){
								Intent enableIntent = new Intent(
										ConnectionActivity.this,
										BtNotificationForthActivity.class)
										.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
												| Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(enableIntent);
								overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
								ConnectionActivity.this.finish();
							}else{
								Intent enableIntent = new Intent(
										ConnectionActivity.this,
										BtNotificationFivthActivity.class)
										.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
												| Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(enableIntent);
								overridePendingTransition(R.anim.fade_slide_in_bottom, R.anim.fadeout_faster);
								ConnectionActivity.this.finish();
							}
//								}
//							}.start();
							
							
						}

				}else if(connectionState == 2){ // 2 connected
					    int deviceType = BTPropertyReader.getBTConnectedDeviceType(ConnectionActivity.this);
					    if(deviceType == 0){ // android device
					    	(new ReconToast(ConnectionActivity.this, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, BTPropertyReader.getBTConnectedDeviceName(ConnectionActivity.this) + " Setup Complete")).show();
							PreferencesUtils.setLastPairedDeviceName(ConnectionActivity.this, BTPropertyReader.getBTConnectedDeviceName(ConnectionActivity.this));
							PreferencesUtils.setLastPairedDeviceAddress(ConnectionActivity.this, BTPropertyReader.getBTConnectedDeviceAddress(ConnectionActivity.this));
							PreferencesUtils.setLastPairedDeviceType(ConnectionActivity.this, 0);

//							ConnectionActivity.this.sendNotification("Setup Complete");
							ConnectionActivity.this.finish();
						}else{
							
						}

				}
			}
		}
	};
	

	class HUDServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			Log.d(TAG, "onServiceConnected_HUDService");
			hudService = IHUDService.Stub.asInterface((IBinder) boundService);
			try {
				connectionState = hudService.getConnectionState();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			hudService = null;
			Log.d(TAG, "onServiceDisconnected_HUDService");
		}
	};

	void initService_hudService() {
		if (hudServiceConnection == null) {
			hudServiceConnection = new HUDServiceConnection();
			Intent i = new Intent("RECON_HUD_SERVICE");
			bindService(i, hudServiceConnection, Context.BIND_AUTO_CREATE);
		}
	}

	void releaseService_hudService() {
		if (hudServiceConnection != null) {
			unbindService(hudServiceConnection);
			hudServiceConnection = null;
			Log.d(TAG, "unbindService()");
		}
	}

    // These functions are specific to JET platform that uses SS1 BT Stack
    protected void enableCallAndTextSS1() {
	String btadderss = BTPropertyReader.getBTConnectedDeviceAddress(this);
	connectHfpSS1(btadderss);
	connectMapSS1(btadderss);
    }
    private void connectHfpSS1(String btaddress) {
	Intent i = new Intent("RECON_SS1_HFP_COMMAND");
	i.putExtra("command",500);
	i.putExtra("address",btaddress);
	sendBroadcast(i);
    }
    private void connectMapSS1(String btaddress) {
	Intent i = new Intent("RECON_SS1_MAP_COMMAND");
	i.putExtra("command",500);
	i.putExtra("address",btaddress);
	sendBroadcast(i);
    }

	private void setupiOSDevice(){
		String address = PreferencesUtils.getDeviceAddress(ConnectionActivity.this);
		if (!"unknown".equals(address) && !"".equals(address)){
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
					.getRemoteDevice(address);
			PreferencesUtils.setLastPairedDeviceName(ConnectionActivity.this, device.getName());
			PreferencesUtils.setBTDeviceName(ConnectionActivity.this, device.getName());
			PreferencesUtils.setDeviceAddress(ConnectionActivity.this, device.getAddress());
			PreferencesUtils.setDeviceName(ConnectionActivity.this, device.getName());
			PreferencesUtils.setLastPairedDeviceAddress(ConnectionActivity.this, device.getAddress());
			PreferencesUtils.setLastPairedDeviceType(ConnectionActivity.this, 1);
		}
	}
	

    protected void unpairDevice() {
		try {
			String address = PreferencesUtils.getLastPairedDeviceAddress(getApplicationContext());
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
		    Method m = device.getClass()
		        .getMethod("removeBond", (Class[]) null);
		    m.invoke(device, (Object[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		}

}
