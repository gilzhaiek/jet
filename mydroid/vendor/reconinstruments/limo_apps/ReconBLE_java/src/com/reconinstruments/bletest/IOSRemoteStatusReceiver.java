package com.reconinstruments.bletest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import android.widget.Toast;


public class IOSRemoteStatusReceiver extends BroadcastReceiver {
    public static final String TAG = "IOSRemoteStatusReceiver";
    private BLETestService mTheService;
    public IOSRemoteStatusReceiver (BLETestService theService) {
	mTheService = theService;
    }

    @Override
    public void onReceive (Context c, Intent i) {
	// Extract the name of the device from the XML Message and set
	// the variable in the service
	Bundle bundle = i.getExtras();
	String message = bundle.getString("message");
	String remoteStatus = getRemoteStatus(message);
	int intRemoteStatus = BLETestService.IOS_REMOTE_STATUS_UNKNOWN;
	if (remoteStatus == null ) {
	    intRemoteStatus = BLETestService.IOS_REMOTE_STATUS_UNKNOWN;
	}
	else if (remoteStatus.equals("connected")) {
	    intRemoteStatus = BLETestService.IOS_REMOTE_STATUS_CONNECTED;
	}
	else if (remoteStatus.equals("disconnected")) {
	    intRemoteStatus = BLETestService.IOS_REMOTE_STATUS_DISCONNECTED;
	}
	else if (remoteStatus.equals("virtual")) {
	    intRemoteStatus = BLETestService.IOS_REMOTE_STATUS_VIRTUAL;
	}
	    
	mTheService.miOSRemoteStatus = intRemoteStatus;

	//DEBUG Toast
	//Toast.makeText(c, ""+remoteStatus, Toast.LENGTH_SHORT).show();		    
    }

    private String getRemoteStatus(String message) {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	try {
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(message));
	        
	    Document doc = db.parse(is);
	    NodeList nodes = doc.getElementsByTagName("remote");
	    Node rootNode = nodes.item(0);
	    NamedNodeMap nnm = rootNode.getAttributes();
	    Node n = nnm.getNamedItem("status");
	    String type = n.getNodeValue();
	    return type;
		
	} catch(Exception e){
	    Log.e(TAG, "Failed to parse xml", e );
	}
	return null;
    }
}