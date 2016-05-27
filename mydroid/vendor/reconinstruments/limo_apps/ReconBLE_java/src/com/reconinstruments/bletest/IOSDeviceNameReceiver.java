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


public class IOSDeviceNameReceiver extends BroadcastReceiver {
    public static final String TAG = "IOSDeviceNameReceiver";
    private BLETestService mTheService;
    public IOSDeviceNameReceiver(BLETestService theService) {
	mTheService = theService;
    }

    @Override
    public void onReceive (Context c, Intent i) {
	// Extract the name of the device from the XML Message and set
	// the variable in the service
	Bundle bundle = i.getExtras();
	String message = bundle.getString("message");
	String deviceName = getDeviceName(message);
	//Log.v(TAG,"deviceName is"+deviceName);
	mTheService.miOSDeviceName = deviceName;
    }

    private String getDeviceName(String message) {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	try {
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(message));
	        
	    Document doc = db.parse(is);
	    NodeList nodes = doc.getElementsByTagName("device");
	    Node rootNode = nodes.item(0);
	    NamedNodeMap nnm = rootNode.getAttributes();
	    Node n = nnm.getNamedItem("name");
	    String type = n.getNodeValue();
	    return type;
		
	} catch(Exception e){
	    Log.e(TAG, "Failed to parse xml", e );
	}
	return null;
    }
}