package com.reconinstruments.phone.service;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class CallerNameResolutionReceiver extends BroadcastReceiver {


    private static final String TAG = "CallerNameResolutionReceiver";
    public PhoneRelayService mTheService;
    public CallerNameResolutionReceiver(PhoneRelayService theService) {
	mTheService = theService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
	Bundle bundle = intent.getExtras();
	String str = bundle.getString("message");
	String name = getCallerName(str);
	Log.v(TAG,"Name is "+name);
	mTheService.displayAndSave(name);
    }
    private String getCallerName(String message) {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    	
	try {
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(message));
	        
	    Document doc = db.parse(is);
	    NodeList nodes = doc.getElementsByTagName("caller_name");
	    Node rootNode = nodes.item(0);
	    //	    NamedNodeMap nnm = rootNode.getAttributes();
	    //	    Node n = nnm.getNamedItem("intent");
	    //String type = n.getNodeValue();
	    //	    return type;
	    String value = rootNode.getTextContent();
	    Log.v(TAG,"value"+value);
	    return value;
		
	} catch(Exception e){
	    Log.e(TAG, "Failed to parse xml", e );
	}
	return null;
    }
    

}