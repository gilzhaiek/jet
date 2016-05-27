package com.reconinstruments.bletest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.reconble.*;
class XmlMessageReceiver extends BroadcastReceiver  {
    public static final String TAG = "BLEXmlMessageReceiver";
    private BLETestService mTheService;
    public XmlMessageReceiver(BLETestService theService){
	super();
	mTheService = theService;
    }
    @Override
    public void onReceive(Context context, Intent intent)  {
	if (intent.getAction().equals(BTCommon.GEN_MSG)) {
	    // BLELog.d(TAG,"Received Message");
	    // If we are connected, forward the message so it can be sent
	    Bundle bundle = intent.getExtras();
	    String str = bundle.getString("message");
	    BLELog.d(TAG,"Trying to send "+str);

	    //HACK: FIXME
	    if (!mTheService.xmlCanCome || (mTheService.mInMusicApp && !str.matches(".*MUSIC.*"))) {
		Log.v(TAG,"Not doing xml shit because we are either busy or in music");
		return;
	    }
		
	    if (mTheService.mStatus == ReconBLE.BLEStatus.STATUS_CONNECTED &&
		!mTheService.isMaster ) {
		BLELog.v(TAG,"We are connected");
		if (mTheService.mPriorList[1] != null) {
		    BLELog.v(TAG,"list exists");
		    if (!mTheService.mPriorList[1].isEmpty()) {
			Log.v(TAG,"Que is too long"+mTheService.mPriorList[1].size()+" dropping the event");
			mTheService.mFailedpush++;
		    }
		    else {
			BLELog.v(TAG,"list is empty");
			mTheService.addToPriorList(str, 1);
			// TODO: Right now we terminate all the
			// shit if we keep failing to send
			mTheService.mFailedpush =
			    mTheService.pushElement(1)?0:mTheService.mFailedpush+1;
		    }
		}
		else {//(mTheService.mPriorList[1] == null)
		    mTheService.addToPriorList(str, 1);
		    mTheService.mFailedpush =
			mTheService.pushElement(1)?0:mTheService.mFailedpush+1;
		}
	    }
	}
	Log.v(TAG,"mFailedpush "+mTheService.mFailedpush);
	mTheService.clearXmlQueIfNecessary();
    }

    // Hack functions: We are going to add some filtration on what can
    // go and not go through
    String get_finalIntent(String message) {
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
	    return type;
		
	} catch(Exception e){
	    Log.e(TAG, "Failed to parse xml", e );
	}
	return null;
    }

}