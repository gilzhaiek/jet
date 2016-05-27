package com.reconinstruments.interdevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
public class InterDeviceIntent {
    public static final String TAG = "InterDeviceIntent";
    public static final void  sendInterDeviceIntent(Context c, Intent i) {
	Parcel p = Parcel.obtain();
	Bundle b = new Bundle();
	//b.putParcelable("theIntent",i);
	//
	//Putting the bundle alone will get extra stuff lost. So Here
	// is an interim solution. Final solution will involve using
	// json and shit to transfer the data.
	Bundle iExtras = i.getExtras();
	String iAction = i.getAction();
	b.putBundle("iExtras", iExtras);
	b.putString("iAction", iAction);
	Log.d(TAG,"Writing intent to parcel");
	b.writeToParcel(p, 0);
	Log.d(TAG,"to base 64");
	String s = Base64.encodeToString(p.marshall(), Base64.DEFAULT);
	String sXml = "<recon intent=\"RECON_REMOTE_INTENT\"><remote-intent>"+
	    s+ "</remote-intent></recon>";
	Intent theI = new Intent("RECON_SMARTPHONE_CONNECTION_MESSAGE");
	//Intent theI = new Intent("RECON_REMOTE_INTENT");
	theI.putExtra("message",sXml);
	c.sendBroadcast(theI);
    }
}
