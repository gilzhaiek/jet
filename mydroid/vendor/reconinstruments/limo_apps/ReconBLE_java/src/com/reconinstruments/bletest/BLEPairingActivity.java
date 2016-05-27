package com.reconinstruments.bletest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.SystemClock;

import android.view.KeyEvent;

import android.widget.TextView;


import com.reconinstruments.nativetest.R;
import com.reconinstruments.reconble.*;

public class BLEPairingActivity extends ListActivity {
    private static final String TAG = "BLE_PAIRING_ACTIVITY";
    private int SIZE_OF_MAC_ADDRESS = 6;
    public TextView line1Text, line2Text, line3Text;
    private int numRemotes;
    static public int TIME_OUTSIDE_WITHOUT_BLERIB = 60;
    static public int TIME_OUTSIDE_WITH_BLERIB = 30;
    static public int TIME_INSIDE = 10;
    public int timerCounter = 60;
    public int counter = -1;
    private byte[] mListOfRemotes;
    private Intent myI;
    private PendingIntent pi;
    private AlarmManager mgr;
    private BLEPairingTimerReceiver mBLETimerReceiver;
    private static final String BLE_PAIRING_ALARM = "ReconBLEPairingAlarm";
    private ArrayList<RemoteItem> remoteLists = null;
    private RemoteAdapter remoteListAdapter; 
    
    public void onCreate(Bundle savedInstanceState) {

	BLELog.d(TAG, "onCreate");
	super.onCreate(savedInstanceState);

	boolean exists = (new File(BLETestService.BLE_FILE_LOC))
	    .exists();
	if (exists) {
	    timerCounter = TIME_OUTSIDE_WITH_BLERIB;
	} else {
	    timerCounter = TIME_OUTSIDE_WITHOUT_BLERIB;
	}

	setContentView(R.layout.ble_pairing_dialog);
	Intent theI = getIntent();
	Bundle b = theI.getExtras();
	
	line1Text = (TextView) this.findViewById(R.id.line1Text);
	line2Text = (TextView) this.findViewById(R.id.line2Text);
	line3Text = (TextView) this.findViewById(R.id.line3Text);
	
	line1Text.setText("New Remote Detected");
	line2Text.setText("Press Power button to pair");
	line3Text.setText("or wait "+ timerCounter+" seconds to cancel");	
	
	if(b != null) mListOfRemotes = (byte[]) b.get("remotes");

	
	 //Fake Remotes for debugging
//	if(mListOfRemotes == null) {
//	Random rand = new Random();
//	mListOfRemotes = new byte[1 * SIZE_OF_MAC_ADDRESS];
//	rand.nextBytes(mListOfRemotes);
//	timerCounter = 180;
//	TIME_INSIDE = 180;
//	}

		
	numRemotes = mListOfRemotes.length
	    / SIZE_OF_MAC_ADDRESS;
	
	
	remoteLists = new ArrayList<RemoteItem>(128);
	
	for (int i = 0; i < numRemotes; i++) {
		String address = String.format("%02X%02X%02X",
					//mListOfRemotes[i * SIZE_OF_MAC_ADDRESS + 5],
					//mListOfRemotes[i * SIZE_OF_MAC_ADDRESS + 4],
					//mListOfRemotes[i * SIZE_OF_MAC_ADDRESS + 3],
					mListOfRemotes[i * SIZE_OF_MAC_ADDRESS + 2],
					mListOfRemotes[i * SIZE_OF_MAC_ADDRESS + 1],
					mListOfRemotes[i * SIZE_OF_MAC_ADDRESS + 0]);
					remoteLists.add( new RemoteItem(address));
	    }
	
	
    remoteListAdapter = new RemoteAdapter (this, 0, remoteLists);
    
    setListAdapter(remoteListAdapter);
		
	// TimerShit()
	mgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
	mBLETimerReceiver = new BLEPairingTimerReceiver();
	mBLETimerReceiver.mOwner = this;
	myI = new Intent(BLE_PAIRING_ALARM);
	pi = PendingIntent.getBroadcast(this, 0, myI, 0);
	// myI =new Intent(this, BLETimerReceiver.class);
	registerReceiver(mBLETimerReceiver, new IntentFilter(BLE_PAIRING_ALARM));
	mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
			 SystemClock.elapsedRealtime(), 1000, pi);

    }

    public void selectRemote() {
	Bundle resultBundle = new Bundle();
	
	BLELog.d(TAG, "Chose\n" + counter);
	// unRegister
	BLELog.d(TAG, "unregister");
	unregisterReceiver(mBLETimerReceiver);
	// Disown:
	BLELog.d(TAG, "disown");
	mBLETimerReceiver.mOwner = null;
	// Dereference
	BLELog.d(TAG, "dereference");
	mBLETimerReceiver = null;
	if (counter < 0) {// User chose no remote:
	    mgr.cancel(pi);
	    boolean exists = (new File(BLETestService.BLE_FILE_LOC)).exists();
	    if (exists) {// BLE file exists
		//Ask it to connect to BLE file address
		byte[] tempBytes;
		tempBytes = BLETestService.readBLEFile();
		if (tempBytes != null){
		    resultBundle.putInt("Mode",2);
		    resultBundle.putByteArray("MacAddress", tempBytes);
		    // writeToTape(tempBytes);
		}
		else{
		    byte[] tempBytes2 = new byte[1];
		    tempBytes2[0] = (byte) '0';
		    resultBundle.putInt("Mode",0);
		    resultBundle.putByteArray("MacAddress", null);
		    // writeToTape(tempBytes2);
		}
	    } else {// No BLE file exists
		//Keep looking for new remote
		byte[] tempBytes = new byte[1];
		tempBytes[0] = (byte) '0';
		resultBundle.putInt("Mode",0);
		resultBundle.putByteArray("MacAddress", null);
		//writeToTape(tempBytes);
	    }
	} else {
	    //ll.getChildAt(counter);
	    byte[] RemoteAddress = new byte[SIZE_OF_MAC_ADDRESS];
	    RemoteAddress[0] = mListOfRemotes[counter * SIZE_OF_MAC_ADDRESS + 0];
	    RemoteAddress[1] = mListOfRemotes[counter * SIZE_OF_MAC_ADDRESS + 1];
	    RemoteAddress[2] = mListOfRemotes[counter * SIZE_OF_MAC_ADDRESS + 2];
	    RemoteAddress[3] = mListOfRemotes[counter * SIZE_OF_MAC_ADDRESS + 3];
	    RemoteAddress[4] = mListOfRemotes[counter * SIZE_OF_MAC_ADDRESS + 4];
	    RemoteAddress[5] = mListOfRemotes[counter * SIZE_OF_MAC_ADDRESS + 5];
	    //writeToTape(RemoteAddress);
	    resultBundle.putInt("Mode",2);
	    resultBundle.putByteArray("MacAddress", RemoteAddress);
	    BLETestService.writeToBLEFile(RemoteAddress);
	    mgr.cancel(pi);
	}
	Intent resultIntent = new Intent("private_ble_command");
	resultIntent.putExtra("ResultBundle",resultBundle);
	resultIntent.putExtra("command",14);
	sendBroadcast(resultIntent);
	finish();

    }
 
    public boolean onKeyDown(int keyCode, KeyEvent event) {

	if (keyCode == KeyEvent.KEYCODE_POWER) {
	    // Toast.makeText(this, "Power button", 500).show();
	}
	timerCounter = TIME_INSIDE;
	counter = (counter + 1) % numRemotes;

	setSelection(counter);
	
	line1Text.setText("Pairing with Remote-"+remoteLists.get(counter).title+" in");
	line2Text.setText(timerCounter+" seconds");
	if (numRemotes>1){
		line3Text.setText("Press power button to chose another remote");
	}
	else if (numRemotes==1){
		line3Text.setText("");

}
	return true;
    }


    @Override
    public void onDestroy() {
	super.onDestroy();
    }
}

class BLEPairingTimerReceiver extends BroadcastReceiver {

    private static final String TAG = "BLEPairingActivityReceiver";
    public BLEPairingActivity mOwner = null;

    @Override
    public void onReceive(Context context, Intent intent) {
	if (mOwner != null) {
	    mOwner.timerCounter--;
	    if (mOwner.counter==-1){
		    mOwner.line3Text.setText("or wait "+ mOwner.timerCounter+" seconds to cancel");
	    }
	    else {
	    	 mOwner.line2Text.setText(mOwner.timerCounter+" seconds");
	    }
	    if (mOwner.timerCounter == 0) {
		mOwner.selectRemote();
	    }
	}
    }
}
