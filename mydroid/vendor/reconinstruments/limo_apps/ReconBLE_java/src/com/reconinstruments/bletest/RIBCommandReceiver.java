// This broadcast receiver receives xml commands pertaining to sending
// rib file to iPhone
package com.reconinstruments.bletest;
import android.content.Context;
import android.content.Context;
import android.content.BroadcastReceiver;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import java.io.StringReader;

public class RIBCommandReceiver extends BroadcastReceiver {

    BLETestService mTheService = null;
    private static final String TAG = "RIBCommandReceiver";
    
    public RIBCommandReceiver (BLETestService theService) {
	super();
	mTheService = theService;
    }
	@Override
	public void onReceive (Context c, Intent i) {
	    BLELog.d(TAG,"RIBCommandReceiver");
	    Bundle b = i.getExtras();
	    String message = b.getString("message");
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    	
	    try {
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(message));
	        
		Document doc = db.parse(is);
		NodeList nodes = doc.getElementsByTagName("recon");
		Node rootNode = nodes.item(0);
		NamedNodeMap nnm = rootNode.getAttributes();
		Node n = nnm.getNamedItem("intent");
		String type = n.getNodeValue();
		
	    } catch(Exception e){
		Log.e(TAG, "Failed to parse xml", e );
	    }
	    String latestRIB = BLETestService.findTheLatestRIB(BLETestService.RIB_PARENT_DIR,true);
	    BLELog.d(TAG,"Latest RIB is "+latestRIB);
	    mTheService.addToPriorList(latestRIB, 0);
	    mTheService.pushElement(0);	     // dummy value of prior of 0;
	    mTheService.xmlCanCome = false;
	    mTheService.canSendIncrementalRib = true;
	}
    }
