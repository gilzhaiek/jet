
package com.reconinstruments.webapi;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

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

import android.util.Log;
/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
/**
 * This class defined the base interface for parsing/composing
 * a XML chunk that are passing through Bluetooth pipe between a Smartphone
 * and Recon Limo Goggle
 * 
 * @author hongzhi wang
 */

public class XMLMessage
{
	static String TAG;
	
	static public final String BUDDY_INFO_MESSAGE  = "RECON_FRIENDS_LOCATION_UPDATE";
	
	static public final String CALL_RELAY_MESSAGE = "RECON_CALL_RECEIVE_RELAY";
	static public final String SMS_RELAY_MESSAGE = "RECON_SMS_RECEIVE_RELAY";
	static public final String PHONE_CONTROL_MESSAGE = "RECON_PHONE_CONTROL";
	static public final String PHONE_CALL_ENDED = "RECON_INCOMING_CALL_ENDED";
	
	
	static public final String MUSIC_RESPONSE_MESSAGE = "RECON_MUSIC_RESPONSE";
	static public final String MUSIC_CONTROL_MESSAGE = "RECON_MUSIC_CONTROL";

	static public final String TRANSFER_REQUEST_MESSAGE = "RECON_TRANSFER_REQUEST";
	static public final String TRANSFER_RESPONSE_MESSAGE = "RECON_TRANSFER_RESPONSE";

	static public final String BT_CONNECT_MESSAGE = "RECON_BT_CONNECTED";

	static public final String LOCATION_REQUEST_MESSAGE = "RECON_LOCATION_REQUEST";
	static public final String LOCATION_RELAY_MESSAGE = "RECON_LOCATION_RELAY";

	static public final String WEB_REQUEST_MESSAGE = "RECON_WEB_REQUEST";
	static public final String WEB_RESPONSE_MESSAGE = "RECON_WEB_RESPONSE";

	static public final String JUMP_DATA_MESSAGE = "RECON_JUMP_DATA";
	static public final String JUMP_SNS_MESSAGE = "RECON_JUMP_SNS";
	
	static public final String FACEBOOK_INBOX_MESSAGE = "RECON_FACEBOOK_INBOX";
	
	public static final boolean DUMP_MESSAGE_FOR_DEBUG = false;


	//Utility function for composing the head element of the message
	protected static String composeHead(String intent)
	{
		return "<recon intent=\"" + intent + "\">";
	}
	//Utility function for appending the closing element to the message 
	protected static String appendEnding( String message )
	{
		return message + "</recon>";
	}

	//parse a message string which is a XML chunk return an object
	public static Document validate( String intent, String message ){
		if( XMLMessage.DUMP_MESSAGE_FOR_DEBUG )
		{
			Log.d(TAG, "Received Message: " + message);
		}
		//parse the buddyInfo which is a xml trunk for a list of buddy information
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
            
	        //verify the message has the right intent
	        if( type.compareTo(intent) != 0 )
	        {
	        	Log.e(TAG, "The XML protocol's intent should be " +intent );
	        	return null;
	        }
	        else{
	        	Log.d(TAG, "Has right intent" );
	        	return doc;
	        }
    	} catch(Exception e){
    		Log.e(TAG, "Failed to parse xml", e );
    	}
		return null;
	}


	public static String composeSimpleMessage(String intent)
	{
		return "<recon intent=\"" + intent + "\"></recon>";
	}
	
	public static String composeSimpleMessage(String intent,String action,String type)
	{
		String message = composeHead(intent);
		message += String.format("<%s type=\"%s\"/>",action,type);
		message = appendEnding(message);
		return message;
	}
	public static String composeSimpleMessage(String intent, String action,BasicNameValuePair[] values)
	{
		String message = composeHead(intent);
		message += String.format("<%s ",action);
		for(BasicNameValuePair value: values){
			message += String.format("%s=\"%s\" ",value.getName(),value.getValue());
		}
		message += String.format("/>");
		
		message = appendEnding(message);
		return message;
	}
	public static String composeSimpleMessageElements(String intent, BasicNameValuePair[] values)
	{
		String message = composeHead(intent);
		for(BasicNameValuePair value: values){
			message += String.format("<%s>%s</%s>",value.getName(),value.getValue(),value.getName());
		}
		message = appendEnding(message);
		return message;
	}
	
	// returns type attribute as in <...><action type="this"/><...>
	public static String parseSimpleMessage(String message)
	{
		try {
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(message));

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList nodes = doc.getElementsByTagName("recon");
			Node rootNode = nodes.item(0);

			Node firstChild = rootNode.getFirstChild();	
			NamedNodeMap actionAttr = firstChild.getAttributes(); 
			Node nActionType = actionAttr.getNamedItem("type");
			String actionType = nActionType.getNodeValue();
			return actionType;
		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
		return "";
	}
	// assumes one node with a number of attributes, returns a map of all the attributes
	public static NamedNodeMap parseSimpleMessageToMap(String message)
	{
		try {
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(message));
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

			return doc.getElementsByTagName("recon").item(0).getFirstChild().getAttributes(); 	
		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	// assumes a structure like this <element>value</element><element2>value2</element> etc., returns a map of all the elements:values
	public static HashMap<String, String> parseSimpleMessageElementsToHashMap(String message)
	{
		try {
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(message));
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

			NodeList n = doc.getElementsByTagName("recon");
			n.item(0).getChildNodes();
			
			HashMap<String, String> map = new HashMap<String, String>();
			
			for(int i=0;i<n.item(0).getChildNodes().getLength();i++){
				map.put(n.item(0).getChildNodes().item(i).getNodeName(), n.item(0).getChildNodes().item(i).getFirstChild().getNodeValue());
			}
			return map; 	
		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	// returns an XML messages intent
	public static String getMessageIntent(String message)
	{
		String intent = "";
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			message = message.replace("&", "+");
			is.setCharacterStream(new StringReader(message));

			Document doc = db.parse(is);
			NodeList nodes = doc.getElementsByTagName("recon");
			Node r = nodes.item(0);
			NamedNodeMap nnm = r.getAttributes();
			Node n = nnm.getNamedItem("intent");
			intent = n.getNodeValue();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DOMException e) {
			e.printStackTrace();
		}
		return intent;
	}
}