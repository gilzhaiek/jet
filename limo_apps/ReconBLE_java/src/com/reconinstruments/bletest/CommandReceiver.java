package com.reconinstruments.bletest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.os.Environment;

public class CommandReceiver extends BroadcastReceiver {
    BLETestService mTheService = null;
    private static final String TAG = "BLECommandReceiver";
    public CommandReceiver(BLETestService theService) {
	mTheService = theService;
    }
    @Override
    public void onReceive (Context c, Intent i) {
	BLELog.d(TAG,"Command Receiver");
	Bundle b = i.getExtras();
	int command = b.getInt("command",0);
	if (command == 1){	// slave mode
	    mTheService.setSlave();
	    BLELog.d(TAG, "Not calling pairDevice");
	    //		pairDevice();
	}
	else if (command == 2) { // master mode
	    mTheService.setMaster();
	    BLELog.d(TAG, "Not calling pairDevice");
	    //		pairDevice();
	}
	else if (command == 3) { // send shit
	    
	    mTheService.addToPriorList(Environment.getExternalStorageDirectory().getAbsolutePath()+"/init.rc", 0);
	    mTheService.pushElement(0);	     // dummy value of prior of 0;
	}
	else if (command == 7) { // Clear buffers
	    mTheService.mBLE.mUnifiedTask.ClearBuffers();
	}
	else if (command == 8) { // Set Send priority
	    int prio = b.getInt("value");
	    BLELog.d(TAG,"Set priority");
	    mTheService.mBLE.SetSendPriority(prio);
	}
	else if (command == 9) { // Get priority
	    BLELog.d(TAG,"Set priority");
	    int prior =  mTheService.mBLE.GetCurrentSendPriority();
	    mTheService.toast("Priority is " +  prior);
	}
	else if (command == 10) {//cancel send
	    for (int j = 0; j<= 3;j++) {
		mTheService.mPriorList[j] = null;
	    }
	    mTheService.mBLE.mUnifiedTask.NotifyStopSend();
	    mTheService.toast("Cancel send");
	}
	else if (command == 11) {//cancel send
	    int prior = mTheService.mBLE.GetCurrentReceivePriority();
	    mTheService.toast("Rcv prior is "+prior);
	}
	else if (command == 12) { //xml to Que
	    String str = b.getString("data");
	    mTheService.addToPriorList(str, 1);
	    //		pushElement(1);
	}
	else if (command == 14) { //pair
	    BLELog.d(TAG,"Pair request Command");
	    Bundle resultBundle = b.getBundle("ResultBundle");
	    int mode = resultBundle.getInt("Mode",0);
	    byte[] mac = resultBundle.getByteArray("MacAddress");
	    BLELog.d(TAG,"mode is"+mode);
	    BLELog.d(TAG,"mac is"+mac);
	    mTheService.mBLE.PairInMasterMode(mode, mac);
	}
	else if (command == 15) { //in music
	    mTheService.mInMusicApp = true;
	    BLELog.d(TAG,"in Music");
	}
	else if (command == 16) { //out of music
	    mTheService.mInMusicApp = false;
	    BLELog.d(TAG,"out of Music");
	}



    }
}
