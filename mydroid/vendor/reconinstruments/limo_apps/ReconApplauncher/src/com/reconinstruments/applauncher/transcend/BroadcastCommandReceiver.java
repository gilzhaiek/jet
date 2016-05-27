//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.BroadcastReceiver;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
/**
 *  <code>BroadcastCommandReceiver</code> is a broadcast receiver that
 *  receives commands via broadcasts. The commands contains the
 *  message object that are typically sent to the transcend service
 *  via the binder. This is in a way a shortcut to do that.  The
 *  message object is inside the intent and then this class delivers
 *  it to the messenger service. 
 *
 */
public class BroadcastCommandReceiver extends BroadcastReceiver {
    ReconTranscendService mRTS;
    public BroadcastCommandReceiver(ReconTranscendService rts) {
        super();
        mRTS = rts;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("BroadcastCommandReceiver", "Received a command");
        Message m = (Message)intent.getExtras().getParcelable("command");
        if (!mRTS.mIgnoreIncomingMessages) {
            try {
                mRTS.mMessenger.send(m);
            } catch (RemoteException e) {
                //Should never happen
            }
        }
    }
}