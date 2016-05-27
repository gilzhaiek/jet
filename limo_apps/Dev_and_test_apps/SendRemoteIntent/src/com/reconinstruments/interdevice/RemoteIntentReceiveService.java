package com.reconinstruments.interdevice;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import android.os.IBinder;
public class RemoteIntentReceiveService extends Service {
    private static final String TAG = "RemoteIntentReceiveService";

    @Override
    public IBinder onBind (Intent intent)    {
	return null;
    }

    @Override
    public void onCreate() {
	//	Log.d(TAG,"Service Created");
        super.onCreate();
	registerReceiver(br, new IntentFilter("RECON_REMOTE_INTENT"));
    }

    @Override
    public int onStartCommand(Intent i, int flag, int startId)    {
	return START_STICKY;
    }

    public BroadcastReceiver br = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context c, Intent i ) {
		Log.w(TAG,"inside teh broadcast receiver");
		String s =  i.getStringExtra("message");
		Intent theI = getIntentFromXml(s);
		if (theI != null) {
		    Log.d(TAG,"Sending the intent");
		    sendBroadcast(theI);
		} else {
		    Log.v(TAG,"The damn thing is null");
		}

	    }
	};

    public final Intent getIntentFromXml(String sXml) {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	try {
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(sXml));
	        
	    Document doc = db.parse(is);
	    NodeList nodes = doc.getElementsByTagName("recon");
	    Node rootNode = nodes.item(0);
	    NamedNodeMap nnm = rootNode.getAttributes();
	    //	    Node n = nnm.getNamedItem("intent");

	    
	    NodeList inodes = doc.getElementsByTagName("remote-intent");
	    Node irootNode = inodes.item(0).getFirstChild();
	    String serializedBundle = irootNode.getNodeValue();
	    //	    Log.d(TAG,"The content is "+serializedBundle);

	    //	    Log.d(TAG,"From base 64 to byte array");
	    byte[] theByteArray = Base64.decode(serializedBundle,Base64.DEFAULT);
	    final Parcel p2 = Parcel.obtain();
	    p2.unmarshall(theByteArray,0,theByteArray.length);
	    p2.setDataPosition(0);
	    Bundle b2 = p2.readBundle();
	    //	    Log.d(TAG,"b2 is "+b2.getParcelable("theIntent"));
	    //	    Intent thei2 = b2.getParcelable("theIntent");
	    //
	    // Due to changes in android systems we don't have
	    // inter-android-version compatibility meaning that the
	    // serialized intent cannot be converted to another intent
	    // on a later andriod version. We construct the intent
	    // again from its action and extras, in hopes to mitigate
	    // that.
	    Intent thei2 = new Intent();
	    Bundle iExtras = b2.getBundle("iExtras");
	    if (iExtras != null) {
		thei2.putExtras(iExtras);
	    }
	    String iAction = b2.getString("iAction");
	    if (iAction != null) {
		thei2.setAction(iAction);
	    }
	    return thei2;
	}
    	catch(Exception e){
	    Log.e(TAG, "Failed to parse xml", e );
	    return null;
	}
    }
}