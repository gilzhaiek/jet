package com.reconinstruments.api.facebook;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.util.Base64;
import android.util.Log;

/**
 *Copyright 2012 Recon Instruments
 *All Rights Reserved.
 */
/**
 * @author Patrick Cho
 * 
 *  This class is used to parse and compose an XML Message containing a facebook service request message in base64.
 */
public class FacebookResponseMessage extends XMLMessage {

	static final String TAG = "FacebookResponseMessage";
	
	static final String NODE_FACEBOOK_RESPONSE = "facebook-response";
	
	public static class FacebookResponseBundle {

		// callback intent
		public String callbackIntent;		
		public String contentInBase64;

		public FacebookResponseBundle(String callbackIntent, String contentInBase64 ) {
			this.callbackIntent = callbackIntent;
			
			this.contentInBase64 = contentInBase64;
		}
	}
	
	public static String compose( FacebookResponseBundle facebookResponseBundle )
	{
		String message = composeHead( facebookResponseBundle.callbackIntent );
		
		message += String.format("<%s >", 
				NODE_FACEBOOK_RESPONSE
				);
		
		message += facebookResponseBundle.contentInBase64;
		
		message += String.format("</%s>",
				NODE_FACEBOOK_RESPONSE
				);
			
		message = appendEnding( message );

		return message;
	}
	
	public static FacebookResponseBundle parse(String message, String callbackIntent) {

		Document doc = XMLMessage.validate(callbackIntent , message);
		if(doc==null) return null;

		FacebookResponseBundle responseBundle = null;

		try {
			
			Element httpResponseElement = (Element)doc.getElementsByTagName(NODE_FACEBOOK_RESPONSE).item(0);
			String contentInBase64 = httpResponseElement.getTextContent();
			
			responseBundle = new FacebookResponseBundle(callbackIntent, contentInBase64 );
			
		} 
		catch (Exception e) {
			Log.e( TAG, "Error parsing XML message",e);
		} 
		return responseBundle;
	}
	
	/**
	 * 
	 * This method decode the String encoded in Base64 format
	 * into a String
	 * 
	 * @param base64String 
	 * 	base64 encoded String
	 * @return decodedBase64String
	 * 	decoded String 
	 */
	public static String decodeBase64String(String base64String){		
		
		byte[] decoded = Base64.decode(base64String, Base64.DEFAULT);
		
		String decodedBase64String = new String(decoded);
		
		return decodedBase64String; 
		
	}
	
	/**
	 * 
	 * This method decode the String encoded in Base64 format
	 * into a byte array
	 * 
	 * @param base64String 
	 * 	base64 encoded String
	 * @return decodedBase64ByteArray
	 * 	decoded byte array 
	 */
	public static byte[] decodeBase64ByteArray(String base64String){		
		
		byte[] decodedBase64ByteArray = Base64.decode(base64String, Base64.DEFAULT);
		
		return decodedBase64ByteArray; 
		
	}

}
