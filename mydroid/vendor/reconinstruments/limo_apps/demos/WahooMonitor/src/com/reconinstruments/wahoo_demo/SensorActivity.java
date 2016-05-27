package com.reconinstruments.wahoo_demo;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.GATM.AttributeProtocolErrorType;
import com.stonestreetone.bluetopiapm.GATM.ConnectionType;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.GATM.RequestErrorType;

public class SensorActivity extends Activity {

	protected static final String TAG = "SensorActivity";
	
	TextView      cadenceOut;
	//TextView      powerOut;
	TextView      speedOut;
	TextView      heartOut;
	
	TextView      rightText;
	
	TextView      time;
	TextView      ampm;
	
	DialView dial;

	Handler uiThreadMessageHandler; 
	PowerMeterDecoder powerDecoder;
	SpeedCadenceDecoder cadenceDecoder;
	HeartRateDecoder heartDecoder;
	
	GenericAttributeClientManager genericAttributeClientManager;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		
		cadenceOut = (TextView)findViewById(R.id.cadenceOut);
		//powerOut = (TextView)findViewById(R.id.powerOut);
		speedOut = (TextView)findViewById(R.id.speedOut); 
		heartOut = (TextView)findViewById(R.id.heartOut);
		
		rightText = (TextView)findViewById(R.id.rightText);
		
		time = (TextView)findViewById(R.id.time);
		ampm = (TextView)findViewById(R.id.ampm);

		dial = (DialView)findViewById(R.id.dial);
		
		dial.setMaxVal(80);
		dial.setCurrentVal(0);
		dial.invalidate();
		

		cadenceOut.setText("--");
		//powerOut.setText("--");
		speedOut.setText("--");
		heartOut.setText("--");
		
		setClock();
		
		uiThreadMessageHandler = new Handler(uiThreadHandlerCallback);
		powerDecoder = new PowerMeterDecoder();
		cadenceDecoder = new SpeedCadenceDecoder();
		heartDecoder = new HeartRateDecoder();
		
		synchronized(this) {
			try {
				genericAttributeClientManager = new GenericAttributeClientManager(genericAttributeClientEventCallback);
			} catch(Exception e) {
				// BluetopiaPM server couldn't be contacted. This should never
				// happen if Bluetooth was successfully enabled.
				//showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
				BluetoothAdapter.getDefaultAdapter().disable();
			}
		}
	}
	boolean runThread;
	class UpdateThread extends Thread{
		@Override
		public void run() {
			
			int t = 0;
			while(runThread){
				try {
					sleep(40);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if(t%10==0){
					/*Bundle data = new Bundle();
					data.putString("speed", getReading(testSpeed(t)));
					data.putString("cadence", getReading(testSpeed(t)));
					data.putString("heart-rate", getReading((int)testSpeed(t)));
					//Log.d(TAG,testSpeed(t));
					setText(data);*/
				}
				setText(null);
				t++;
			}
			super.run();
		}
	};
	UpdateThread updateThread;
	
	@Override
	protected void onPause() {
		super.onPause();
		
		runThread = false;
		updateThread = null;
	}


	@Override
	protected void onResume() {
		super.onResume();

		runThread = true;
		if(updateThread==null||!updateThread.isAlive()){
			updateThread = new UpdateThread();
			updateThread.start();
		}
	}


	float testSpeed(float t){
		
		return (float) (100.0f*Math.abs(Math.sin(t/50)));
	}
	
	
	private final ClientEventCallback genericAttributeClientEventCallback = new ClientEventCallback() {
		@Override
		public void connectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {
			//displayMessage("connectedEvent");
		}
		@Override
		public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress) {
			//displayMessage("disconnectedEvent");
		}
		@Override
		public void connectionMTUUpdateEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {
			//displayMessage("connectionMTUUpdateEvent");
		}
		@Override
		public void handleValueEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean handleValueIndication, int attributeHandle, byte[] attributeValue) {
			//displayMessage("handleValueEvent");
			//if(activeDevice!=null&&activeDevice.address==remoteDeviceAddress&&activeDevice.notifyHandle==attributeHandle){
			//	activeDevice.handleNotification(attributeValue);
			//}

			Bundle data = new Bundle();
			if(attributeHandle==35){
				cadenceDecoder.decodeData(attributeValue);
				data.putString("cadence", getReading(cadenceDecoder.getCadence()));
				data.putString("speed", getReading(cadenceDecoder.getSpeed()));
			} else if(attributeHandle==41){
				powerDecoder.decodeData(attributeValue);
				data.putString("power", getReading(powerDecoder.getInstantaneousPower()));
				data.putString("speed", getReading(powerDecoder.getSpeed()));
			} else if(attributeHandle==26){
				heartDecoder.decodeData(attributeValue);
				data.putString("heart-rate", getReading(heartDecoder.getHeartRate()));
			}

			setText(data);
		}
		@Override
		public void readResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, boolean isFinal, byte[] value) {
			//displayMessage("readResponseEvent");
		}
		@Override
		public void writeResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle) {
			//displayMessage("writeResponseEvent");
		}
		@Override
		public void errorResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, RequestErrorType requestErrorType, AttributeProtocolErrorType attributeProtoccolErrorType) {
			//displayMessage("errorResponseEvent");
		}
	};
	public String getReading(int value){
		if(value==-1) return "--";
		return ""+value;
	}
	public String getReading(float value){
		if(value<0) return "--";
		
		if(((int)value+0.1f)<10)
			return String.format("%.1f", value);
		
		return ""+(int)value;
	}
	
	public static String bytArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for(byte b: a)
			sb.append(String.format("%02x", b&0xff));
		return sb.toString();
	}

	void setText(Bundle data) {
		Message msg = Message.obtain(uiThreadMessageHandler);
		msg.setData(data);
		msg.sendToTarget();
	}
	// This part is just for displaying messages and logs on ui
	private final Handler.Callback uiThreadHandlerCallback = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Bundle data = msg.getData();
			for(String key:data.keySet()){
				if(key.equals("cadence")){
					cadenceOut.setText(data.getString(key));
				} else if(key.equals("power")){
					rightText.setText("w");
					cadenceOut.setText(data.getString(key));
				} else if(key.equals("speed")){
					speedOut.setText(data.getString(key));
					
					try {
						dial.setCurrentVal(Float.parseFloat(data.getString(key)));
					} catch (NumberFormatException n){
						dial.setCurrentVal(0);
					}
				} else if(key.equals("heart-rate")){
					heartOut.setText(data.getString(key));
				}
			}
			if(data.isEmpty()){
				dial.invalidate();
			}
			setClock();
			return true;
		}
	};
	Calendar now = Calendar.getInstance();
	int lastMin = -1;
	public void setClock(){
		now.setTime(new Date());
		//Log.d("Sensor","this minute:"+now.get(Calendar.MINUTE)+" last minute:"+lastMin);
		if(now.get(Calendar.MINUTE)!=lastMin){
			time.setText(now.get(Calendar.HOUR)+":"+String.format("%02d", now.get(Calendar.MINUTE)));
			if(now.get(Calendar.AM_PM)==Calendar.PM)
				ampm.setText("pm");
			else
				ampm.setText("am");
			lastMin = now.get(Calendar.MINUTE);
		}
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_DPAD_UP){
			Intent intent = new Intent();
			String mPackage = "com.reconinstruments.ble_ss1";
			String mClass = ".device.UtilActivity";
			intent.setComponent(new ComponentName(mPackage,mPackage+mClass));
			this.startActivity(intent);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	
}
