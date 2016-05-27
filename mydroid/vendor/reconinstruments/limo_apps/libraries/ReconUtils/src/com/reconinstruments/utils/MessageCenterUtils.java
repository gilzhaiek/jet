package com.reconinstruments.utils;


import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

//from ReconMessageCenter
public class MessageCenterUtils {

	public static final String BUNDLE_INTENT_STRING = "TheIntent";
	
	// serialize intent into byte array that can be persisted to BLOB db field
	// This is serious hack; Intent does not implement serializable, and using
	// Parcel wont' work directly. So we write bundle, then flatten bundle to Parcel
	// In real world you want to implement Serializable interface
	public static byte [] IntentToBytes (Intent i)
	{
		if(i==null) return new byte[0];
		
		Log.d("MessageCenterUtils", i.toUri(0));
		
		Parcel bundleparcel = Parcel.obtain();
		Bundle b = new Bundle();

		b.putParcelable(BUNDLE_INTENT_STRING, i);
		b.writeToParcel(bundleparcel, 0);
		bundleparcel.setDataPosition(0);

		byte [] bytes = bundleparcel.marshall();   
		bundleparcel.recycle();

		return bytes;
	}

	// Deserialization of Intent from byte array retrieved from BLOB db field
	// see comments for reverse "IntentToBytes"
	public static Intent BytesToIntent (byte [] bytes)
	{
		try{
			if ( (null == bytes) || (bytes.length == 0) ) return null;
	
			final Parcel targetparcel = Parcel.obtain();
			targetparcel.unmarshall(bytes, 0, bytes.length);        
			targetparcel.setDataPosition(0);
	
			Bundle b2 = targetparcel.readBundle();
			Intent i = b2.getParcelable(BUNDLE_INTENT_STRING);
			targetparcel.recycle();
	
			return i;
		}
		catch(Exception e){
			return null;
		}
	}
}
