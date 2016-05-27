package com.reconinstruments.bletest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.reconinstruments.nativetest.R;
import com.reconinstruments.reconble.*;
import android.os.Environment;

public class SwitchToMasterMode extends Activity {
    private static final String TAG = "BLE_PAIRING_ACTIVITY";
    private int SIZE_OF_MAC_ADDRESS = 6;
    private LinearLayout deviceListOneLayout, deviceListTwoLayout;
    public TextView timerCounterField, instructText;
    private int numRemotes;
    static public int TIME_OUTSIDE_WITHOUT_BLERIB = 60;
    static public int TIME_OUTSIDE_WITH_BLERIB = 10;
    static public int TIME_INSIDE = 10;
    public int timerCounter = 60;
    public int counter = -1;
    private byte[] mListOfRemotes;
    private Intent myI;
    private PendingIntent pi;
    private AlarmManager mgr;
    private BLETimerReceiver mBLETimerReceiver;
    private static final String BLE_SWITCH_TO_MASTER_MODE = "BLE_SWITCH_TO_MASTER_MODE";
    public static String BLE_FILE_LOC = Environment.getExternalStorageDirectory().getAbsolutePath()+"/BLE.RIB";

    private static final int MASTER_MODE = 0;
    private static final int SLAVE_MODE = 1;

    public void onCreate(Bundle savedInstanceState) {

	BLELog.d(TAG, "onCreate");
	super.onCreate(savedInstanceState);
	setContentView(R.layout.switch_to_master_dialog);
	Intent theI = getIntent();
	Bundle b = theI.getExtras();
	instructText = (TextView) this.findViewById(R.id.sm_dialog_title);
	timerCounterField = (TextView) this.findViewById(R.id.sm_timer_counter);
	timerCounterField.setText("" + timerCounter);
	if(counter < 0) {
	    instructText.setFocusable(true);
	    instructText.requestFocus();
	}
	mgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
	mBLETimerReceiver = new BLETimerReceiver();
	mBLETimerReceiver.mOwner = this;
	myI = new Intent(BLE_SWITCH_TO_MASTER_MODE);
	pi = PendingIntent.getBroadcast(this, 0, myI, 0);
	registerReceiver(mBLETimerReceiver, new IntentFilter(BLE_SWITCH_TO_MASTER_MODE));
	mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
			 SystemClock.elapsedRealtime(), 1000, pi);
    }

    public void selectOption() {
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
	Intent resultIntent = new Intent("private_ble_command");
	if (counter == MASTER_MODE) {
	    BLELog.d(TAG,"Master Mode");
		resultIntent.putExtra("command",2);
	}
	else if (counter == SLAVE_MODE) {
	    BLELog.d(TAG,"Slave Mode");
		resultIntent.putExtra("command",1);
	}
	sendBroadcast(resultIntent);
	finish();

    }
 
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	instructText.setFocusable(false);
		
	if (keyCode == KeyEvent.KEYCODE_POWER) {
	    // Toast.makeText(this, "Power button", 500).show();
	}
	timerCounter = TIME_INSIDE;
	timerCounterField.setText("" + timerCounter);
	counter = (counter + 1) % 2;
	TextView tvm = (TextView) this.findViewById(R.id.master_mode);
	TextView tvs = (TextView) this.findViewById(R.id.slave_mode);
	if (counter == MASTER_MODE) {
	    tvm.setTextColor(Color.BLUE);
	    tvs.setTextColor(Color.BLACK);
	}
	else if (counter == SLAVE_MODE) {
	    tvs.setTextColor(Color.BLUE);
	    tvm.setTextColor(Color.BLACK);
	}
		
	return true;
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
    }
}

class BLETimerReceiver extends BroadcastReceiver {

    private static final String TAG = "SwitchToMasterModeReceiver";
    public SwitchToMasterMode mOwner = null;

    @Override
    public void onReceive(Context context, Intent intent) {
	if (mOwner != null) {
	    if (mOwner.counter >= 0) {
		mOwner.timerCounter--;
		mOwner.timerCounterField.setText("" + mOwner.timerCounter);
		if (mOwner.timerCounter == 0) {
		    mOwner.selectOption();
		}
	    }
	}
    }
}
