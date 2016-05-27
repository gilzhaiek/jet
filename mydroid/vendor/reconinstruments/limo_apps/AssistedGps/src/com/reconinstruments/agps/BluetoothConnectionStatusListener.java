package com.reconinstruments.agps;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.location.Location;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import com.reconinstruments.mobilesdk.hudconnectivity.Constants;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService.Channel;

/**
 *Monitors the state of HUD connection to the phone fires
 *<code>bluetothStateChanged()<code> if the bluetooth connectivity
 *state chanages
 *
 */
public class BluetoothConnectionStatusListener extends BroadcastReceiver {
    ReconAGpsContext mOwner;
    private int mLatestStatus;
    public int getLatestStatus() {
	return mLatestStatus;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
	Bundle bundle = intent.getExtras();
	int b = bundle.getInt("state"); // connectionstate
	mLatestStatus = b;
	// notify the state machine
	mOwner.mStateMachine.bluetoothStateChanged(b);
    }
    public BluetoothConnectionStatusListener(ReconAGpsContext agpsa) {
	super();
	mOwner = agpsa;
    }
}