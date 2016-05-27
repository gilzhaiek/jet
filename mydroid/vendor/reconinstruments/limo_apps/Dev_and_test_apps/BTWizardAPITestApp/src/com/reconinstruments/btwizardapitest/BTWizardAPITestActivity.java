package com.reconinstruments.btwizardapitest;

import 	android.widget.ArrayAdapter;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.reconinstruments.applauncher.phone.IPhoneRelayService;
import com.reconinstruments.bletest.IBLEService;
import java.util.Set;


public class BTWizardAPITestActivity extends Activity
{

    public static final String TAG = "BTWizardAPITestActivity";
    /** Called when the activity is first created. */

    private ArrayAdapter<String> mAdapter;
    private Spinner deviceSpinner;

    @Override
    public void onResume() {
	super.onResume();
	initService_ble();
	initService_phone();
    }

    @Override
    public void onPause() {
	releaseService_ble();
	releaseService_phone();
	super.onPause();
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	final Button bleDeviceName = (Button) findViewById(R.id.ble_device_name);
	bleDeviceName.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    String result = getBLEDeviceName();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show();
		}
	    });

	final Button btDeviceName = (Button) findViewById(R.id.bt_device_name);
	btDeviceName.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    String result = getBTDeviceName();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show();
		}
	    });

	final Button mapStatus = (Button) findViewById(R.id.map_status);
	mapStatus.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    int result = getMapStatus();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show();
		}
	    });

	final Button hfpStatus = (Button) findViewById(R.id.hfp_status);
	hfpStatus.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    int result = getHfpStatus();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show();
		}
	    });

	final Button sendBtStat = (Button) findViewById(R.id.send_bt_stat);
	sendBtStat.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    int result_hfp = getHfpStatus();
		    int result_map = getMapStatus();
		    // Use helper function to generate the xml 
		    String theXml = generateBtStatXml(result_hfp, result_map);
		    // push the Xml to BLE
		    boolean resultbool = pushXmlString(theXml);
		    String result = (resultbool)? "success": "failed";
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show();
		}
	    });
	
	final Button isMasterBefore = (Button) findViewById(R.id.is_master_before);
	isMasterBefore.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    boolean result = getIsMasterBeforeonCreate();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show();		    
		}
	    });

	final Button disconnectBT = (Button) findViewById(R.id.disconnect_bt);
	disconnectBT.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    boolean result = disconnectHfp() && disconnectMap();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show();		    
		}
	    });




	final Button requestIOSRCStat = (Button) findViewById(R.id.request_ios_rc_status);
	requestIOSRCStat.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    boolean result = requestIOSRemoteStatus();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_SHORT).show();		    
		}
	    });

	final Button readIOSRCStat = (Button) findViewById(R.id.read_ios_rc_status);
	readIOSRCStat.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    int result = readIOSRemoteStatus();
		    Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_SHORT).show();		    
		}
	    });


	final Button connectWithBt = (Button) findViewById(R.id.connect_with_bt);
	connectWithBt.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Log.v(TAG,"connect_with_bt clicked");
		    // Read the mac address from the spinner
		    int theIndex = deviceSpinner.getSelectedItemPosition();
		    String theAddress = "";
		    if (theIndex >= 0) {
			 theAddress = (String) mAdapter.getItem(theIndex);
		    }
		    else {
			return;
		    }

		    // Establish connection
		    connectToHfp(theAddress);
		    connectToMap(theAddress);
		}
	    });


	// Note to the future developer: Here I get the list of paired
	// devices that we try to connect to. Technically the list can
	// be any list with a bunc of mac addresses in them. And if
	// the device is not already paired with that mac address once
	// you try to call connectoToHfp (address), the system will
	// automatically popup the pairing request. If it is paired
	// then the connection will established automatically. There
	// is However one little thing that has to be verified:
	//
	// TODO: (To be verified)
	// If the device is unpaired and you try to connect and
	// pairing request appears and you pair do you have to call
	// the function AGAIN to connect or it will connect
	// automatically after pairing.
	 mAdapter =
	    new ArrayAdapter (this, android.R.layout.simple_spinner_item);
	 populateAdapterWithBondedBtDevices();
	
	deviceSpinner = (Spinner) findViewById(R.id.devices_spinner);
	deviceSpinner.setAdapter(mAdapter);

    }

    // Some helper functions:
    private void populateAdapterWithBondedBtDevices () {
	BluetoothAdapter mBluetoothAdapter = null;
	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
	for (BluetoothDevice d : devices) {
	    mAdapter.add(d.getAddress());
	}
    }
    String generateBtStatXml(int hfpstat, int mapstat) {
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

	String theXmlString = "<recon intent=\"bt_update_status\"><hfp state=\""+
	    hfpString+"\"/><map state=\""+mapString+"\"/></recon> ";
	return theXmlString;

    }


    ///////////////////////////// BLE Service Connection /////////////////////
    //////////////////////////////////////////////////////
    // aidl service connection.
    /////////////////////////////////////////////////////
    private IBLEService bleService;
    private BLEServiceConnection bleServiceConnection;

    public void initService_ble() {
	if (bleServiceConnection == null) {
	    bleServiceConnection = new BLEServiceConnection();
	    Intent i = new Intent("RECON_BLE_TEST_SERVICE");
	    bindService(i, bleServiceConnection, Context.BIND_AUTO_CREATE);
	}
    }

    public void releaseService_ble() {
	//unregister:
	try {
	    if (bleService != null) {
		String test = bleService.getiOSDeviceName();
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	//unbind:
	if (bleServiceConnection != null){
	    unbindService(bleServiceConnection);
	    bleServiceConnection = null;
	    Log.d(TAG, "unbindService()");
	}
    }

    class BLEServiceConnection implements ServiceConnection {
	public void onServiceConnected(ComponentName className, IBinder boundService) {
	    Log.d(TAG, "onServiceConnected_ble");
	    bleService = IBLEService.Stub.asInterface((IBinder) boundService);
	    try	{
		if (bleService != null) {
		    String test = bleService.getiOSDeviceName();
		}
	    }
	    catch (RemoteException e)	{
		e.printStackTrace();
	    }
	}
	public void onServiceDisconnected(ComponentName className){ 
	    bleService = null;
	    Log.d(TAG, "onServiceDisconnected_ble");
	}
    };
    //////////////////// End of aidl shit///////////////////////

    public String getBLEDeviceName() {
	try	{
	    if (bleService != null) {
		String test = bleService.getiOSDeviceName();
		Log.v(TAG,"BLE Device name"+test);
		return test;
	    }
	}
	catch (RemoteException e)	{
	    e.printStackTrace();
	}
	return null;
    }
    
    public boolean pushXmlString(String xmlString) {
	try {	
	    if (bleService != null) {
		int result = bleService.pushXml(xmlString);
		return (result==0);
	    }
	}
	catch (RemoteException e)	{
	    e.printStackTrace();
	}
	return false;
    }

    public boolean getIsMasterBeforeonCreate() {
	//Log.v(TAG,"getIsMasterBeforeonCreate");
	try {
	    if (bleService != null) {
		boolean result = bleService.getIsMasterBeforeonCreate();
		//Log.v(TAG,"getIsMasterBeforeonCreate result" + result);
		return result;
	    }
	}
	catch (RemoteException e)	{
	    e.printStackTrace();
	}
	return true;
    }

    public boolean requestIOSRemoteStatus () {
	try {
	    if (bleService != null) {
		int result = bleService.sendControlByte((byte)0x11); // That is for remote
		return true;
	    }
	}
	catch (RemoteException e)	{
	    e.printStackTrace();
	}
	return false;
    }

    public int readIOSRemoteStatus () {
	try {
	    if (bleService != null) {
		int result = bleService.getiOSRemoteStatus(); 
		return result;
	    }
	}
	catch (RemoteException e)	{
	    e.printStackTrace();
	}
	return -1;		// Equivalent to unknown
    }


    ///// End of BLE Shit

    ///////////////////////////// Phone Relay Service Connection /////////////////////
    //////////////////////////////////////////////////////
    // aidl service connection.
    /////////////////////////////////////////////////////
    private IPhoneRelayService phoneRelayService;
    private PhoneRelayServiceConnection phoneRelayServiceConnection;

    public void initService_phone() {
	if (phoneRelayServiceConnection == null) {
	    phoneRelayServiceConnection = new PhoneRelayServiceConnection();
	    Intent i = new Intent("RECON_PHONE_RELAY_SERVICE");
	    bindService(i, phoneRelayServiceConnection, Context.BIND_AUTO_CREATE);
	}
    }

    public void releaseService_phone() {
	//unbind:
	if (phoneRelayServiceConnection != null){
	    unbindService(phoneRelayServiceConnection);
	    phoneRelayServiceConnection = null;
	    Log.d(TAG, "unbindService()");
	}
    }

    class PhoneRelayServiceConnection implements ServiceConnection {
	public void onServiceConnected(ComponentName className, IBinder boundService) {
	    Log.d(TAG, "onServiceConnected_phone");
	    phoneRelayService = IPhoneRelayService.Stub.asInterface((IBinder) boundService);
	}
	public void onServiceDisconnected(ComponentName className){ 
	    phoneRelayService = null;
	    Log.d(TAG, "onServiceDisconnected_phone");
	}
    };
    //////////////////// End of aidl shit///////////////////////

    public String getBTDeviceName () {
	try {
	    if (phoneRelayService != null) {
		String test = phoneRelayService.getBluetoothDeviceName();
		return test;
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return null;
	
    }

    // For SMS
    // 0 means not connected, 1 means connecting, 2 means connected
    public int getMapStatus () {
	try {
	    if (phoneRelayService != null) {
		int test = phoneRelayService.getMapStatus();
		return test;
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return -1;
    }

    // For call relay
    public int getHfpStatus () {
	try {
	    if (phoneRelayService != null) {
		int test = phoneRelayService.getHfpStatus();
		return test;
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return -1;
    }

    public boolean connectToHfp (String address) {
	//Log.v(TAG,"connectToHfp");
	//	address = "F0:CB:A1:74:CB:57";
	//Log.v(TAG,"Mac Address is "+address);
	try {
	    if (phoneRelayService != null) {
		boolean test = phoneRelayService.remoteConnectToHfpDevice(address);
		return test;
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return false;

    }

    public boolean connectToMap (String address) {
	//Log.v(TAG,"connectToMap");
	//	address = "F0:CB:A1:74:CB:57";
	try {
	    if (phoneRelayService != null) {
		boolean test = phoneRelayService.remoteConnectToMapDevice(address);
		return test;
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return false;
    }

    public boolean disconnectMap() {
	try {
	    if (phoneRelayService != null) {
		boolean test = phoneRelayService.remoteDisconnectMapDevice();
		return test;
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return false;
    }

    public boolean disconnectHfp() {
	try {
	    if (phoneRelayService != null) {
		boolean test = phoneRelayService.remoteDisconnectHfpDevice();
		return test;
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return false;
    }

}
