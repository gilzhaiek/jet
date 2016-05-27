package com.reconinstruments.applauncher.transcend;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Gallery;
import android.widget.Toast;

public class OnAlarmReceiver extends BroadcastReceiver {
	
	private static final String TAG = "ReconTranscendOnAlarmReceiver";
	public ReconTranscendService mOwner = null;
	@Override
	public void onReceive(Context context, Intent intent) {
		if (mOwner != null){
		    //Log.v(TAG,"received");
		    mOwner.ppsUpdate();
		}

	}
	
}
