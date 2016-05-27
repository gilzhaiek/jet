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


public class BTWizardAPITestActivity2 extends Activity
{

    public static final String TAG = "BTWizardAPITestActivity";
    /** Called when the activity is first created. */

    private ArrayAdapter<String> mAdapter;
    private Spinner deviceSpinner;

    @Override
    public void onResume() {
	super.onResume();
    }

    @Override
    public void onPause() {
	super.onPause();
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_screen);

	final Button connectWithBtHfp = (Button) findViewById(R.id.connect_with_bt_hfp);
	connectWithBtHfp.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Log.v(TAG,"connect_with_bt_hfp clicked");
		    // Read the mac address from the spinner
		    int theIndex = deviceSpinner.getSelectedItemPosition();
		    String theAddress = "";
		    Log.v(TAG,"theIndex is "+theIndex);
		    if (theIndex >= 0) {
			Log.v(TAG,"sent the request");
			theAddress = (String) mAdapter.getItem(theIndex);
			Intent i = new Intent("RECON_SS1_HFP_COMMAND");
			i.putExtra("command",500);
			i.putExtra("address",theAddress);
			sendBroadcast(i);
		    }
		    else {
			return;
		    }
		}
	    });

	final Button connectWithBtMap = (Button) findViewById(R.id.connect_with_bt_map);
	connectWithBtMap.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Log.v(TAG,"connect_with_bt_map clicked");
		    // Read the mac address from the spinner
		    int theIndex = deviceSpinner.getSelectedItemPosition();
		    String theAddress = "";
		    Log.v(TAG,"theIndex is "+theIndex);
		    if (theIndex >= 0) {
			Log.v(TAG,"sent the request");
			theAddress = (String) mAdapter.getItem(theIndex);
			Intent i = new Intent("RECON_SS1_MAP_COMMAND");
			i.putExtra("command",500);
			i.putExtra("address",theAddress);
			sendBroadcast(i);
		    }
		    else {
			return;
		    }
		}
	    });
	final Button disconnectWithBtHfp = (Button) findViewById(R.id.disconnect_with_bt_hfp);
	disconnectWithBtHfp.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Log.v(TAG,"disconnect_with_bt_hfp clicked");
			Intent i = new Intent("RECON_SS1_HFP_COMMAND");
			i.putExtra("command",600);
			sendBroadcast(i);
		}
	    });
	final Button disconnectWithBtMap = (Button) findViewById(R.id.disconnect_with_bt_map);
	disconnectWithBtMap.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    Log.v(TAG,"connect_with_bt_map clicked");
		    Intent i = new Intent("RECON_SS1_MAP_COMMAND");
		    i.putExtra("command",600);
		    sendBroadcast(i);
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

}
