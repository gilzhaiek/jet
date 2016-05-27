package com.reconinstruments.phone.dto;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class OutgoingSMSMessage extends XMLMessage {

    public static final String LOG_TAG = "InitiateCallMessage";
    public static final String MAIN_ATTR_TYPE = "send_sms"; 
    public static final String MAIN_NODE = "action";
    public static final String MAIN_ATTR = "type";
    public static final String NUM_NODE = "num";
    public static final String BODY_NODE = "body";
    
    @Override
    public String compose(Object messageInfo) {
	if (messageInfo instanceof OutgoingSMSBundle == false) {
	    Log.e(LOG_TAG, "messageInfo must be instance of String!");
	    return "";
	}
	
	OutgoingSMSBundle thisMsg = (OutgoingSMSBundle) messageInfo;
	
	String message = composeHead(PHONE_CONTROL_MESSAGE);
	
	message += String.format("<%s %s=\"%s\">", MAIN_NODE, MAIN_ATTR, MAIN_ATTR_TYPE);
	
	message += String.format("<%s>%s</%s>", NUM_NODE, thisMsg.getNum(), NUM_NODE);
	
	message += String.format("<%s>%s</%s>", BODY_NODE, thisMsg.getBody(), BODY_NODE);	
	
	message += String.format("</%s>", MAIN_NODE);
	
	message = appendEnding(message);
	
	return message;
    }

    @SuppressWarnings("finally")
    @Override
    public Object parse(String message) {

	if (XMLMessage.DUMP_MESSAGE_FOR_DEBUG) {
	    Log.d(LOG_TAG, "Received Message: " + message);
	}

	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	
	OutgoingSMSBundle bundle = null;
	
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

	    // Verify that the message has the right intent.
	    if (type.compareTo(XMLMessage.PHONE_CONTROL_MESSAGE) != 0) {
		Log.e(LOG_TAG, "The XML protocol's intent should be " + XMLMessage.PHONE_CONTROL_MESSAGE);
	    } else {
		Node firstChild = rootNode.getFirstChild();
		if (firstChild.getNodeType() == Node.ELEMENT_NODE && firstChild.getNodeName().compareToIgnoreCase(MAIN_NODE) == 0) {

		    NamedNodeMap actionAttr = firstChild.getAttributes();
		    Node nActionType = actionAttr.getNamedItem("type");
		    String actionType = nActionType.getNodeValue();

		    if (actionType.compareToIgnoreCase(MAIN_ATTR_TYPE) != 0) {
			Log.e(LOG_TAG, "The XML protocol's action type should be " + MAIN_ATTR_TYPE);
		    }
		    
		    NodeList childNodes = firstChild.getChildNodes();
		    Node numNode = childNodes.item(0).getFirstChild();
		    String num = numNode.getNodeValue();
		    Node bodyNode = childNodes.item(1).getFirstChild();
		    String body = bodyNode.getNodeValue();
		    
		    bundle = new OutgoingSMSBundle(num, body);
		    
		}
	    }
	} catch (ParserConfigurationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SAXException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (NumberFormatException e) {
	    e.printStackTrace();
	} catch (NullPointerException e) {
	    e.printStackTrace();
	} finally {
	    return bundle;
	}

    }

}
