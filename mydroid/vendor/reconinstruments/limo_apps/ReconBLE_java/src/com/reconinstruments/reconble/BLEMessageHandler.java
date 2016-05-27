package com.reconinstruments.reconble;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
/* Package Internal class that facilitates passing of Notification Messages 
 * from Async Monitor Threads to main App Thread */
public class BLEMessageHandler extends Handler 
{
    // key enumeration
    public static final String KEY_STATUS   = "Status";
    public static final String KEY_TYPE     = "Type";
    public static final String KEY_PROGRESS = "Progress";
    public static final String KEY_LAST_SENT_PRIORITY = "LastSentPriority";

    public static final int    TYPE_PAIR        = 1;
   
    public static final int    TYPE_rx_PROGRESS = 3;
    public static final int    TYPE_rx_END      = 4;
   
    public static final int    TYPE_tx_PROGRESS = 6;
    public static final int    TYPE_tx_END      = 7;
    public static final int    TYPE_ALL      = 8; // everything
    public static final int    TYPE_XML_READY = 9;
    public static final int    TYPE_MISC_DATA_READY = 10;

    public static final String TAG = "BLEMessageHandler";
  
		 
    protected IReconBLEEventListener mCbk = null;
		 
    @Override
    public void handleMessage(Message msg) 
    {
	//	Log.v("BLEMessenger","handleMessage");
	Bundle b = msg.getData();
      
	// route to Listner callback based on message type
	int type = b.getInt(BLEMessageHandler.KEY_TYPE);
      
	if (type == BLEMessageHandler.TYPE_PAIR) {
	    //	    Log.v(TAG,"Pair status changed");
	    //	    Log.v(TAG,"The status is "+b.getInt(BLEMessageHandler.KEY_STATUS));
	    mCbk.onPairChanged(b.getInt(BLEMessageHandler.KEY_STATUS) );
	}
	else if (type == BLEMessageHandler.TYPE_tx_END){
	    mCbk.onSendCompleted(b.getInt(BLEMessageHandler.KEY_STATUS),b);
	}
	else if ( type == BLEMessageHandler.TYPE_tx_PROGRESS){
	    mCbk.onSendUpdate(b.getInt(BLEMessageHandler.KEY_PROGRESS));
	}
	else if (type == BLEMessageHandler.TYPE_rx_END) {
	    mCbk.onReceiveCompleted(b.getInt(BLEMessageHandler.KEY_STATUS),b);
	}
	else if ( type == BLEMessageHandler.TYPE_rx_PROGRESS) {
	    mCbk.onReceiveUpdate(b.getInt(BLEMessageHandler.KEY_PROGRESS) );
	}
	else if ( type == BLEMessageHandler.TYPE_XML_READY){
	    String xml = b.getString("xml");
	    mCbk.onXmlReceived(xml);
	}
	else if ( type == BLEMessageHandler.TYPE_MISC_DATA_READY){
	    //  Log.v("BLEMessenger","type == BLEMessageHandler.TYPE_MISC_DATA_READY");
	    byte[] misc_data  = b.getByteArray("misc_data");
	    mCbk.onMiscData(misc_data);
	}

    }
}
