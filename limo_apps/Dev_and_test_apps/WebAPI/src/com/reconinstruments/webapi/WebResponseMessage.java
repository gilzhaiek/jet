package com.reconinstruments.webapi;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.util.Log;

/**
 *Copyright 2012 Recon Instruments
 *All Rights Reserved.
 *
 * @author Patrick Cho
 * 
 *  This class is used to parse and compose an XML Message containing a web service request message in base64.
 */
public class WebResponseMessage extends XMLMessage {

	static final String TAG = "WebResponseMessage";
	
	static final String NODE_HTTP_RESPONSE = "http-response";
	static final String NODE_HTTP_RESPONSE_STATUS_CODE_ATTR = "status_code";
	static final String NODE_HTTP_RESPONSE_STATUS_LINE_ATTR = "status_line";
	static final String NODE_HTTP_RESPONSE_REQUEST_ID_ATTR = "request-id";
	
	public static class WebResponseBundle {

		// callback intent
		public String callbackIntent;
		// status informations
		public String statusCode;
		public String statusLine;
		public String requestId;
		
		public String contentInBase64;

		public WebResponseBundle(String callbackIntent, String statusCode, String statusLine, String requestId , String contentInBase64 ) {
			this.callbackIntent = callbackIntent;
			this.statusCode = statusCode;
			this.statusLine = statusLine;
			this.requestId = requestId;
			this.contentInBase64 = contentInBase64;
		}
	}
	
	public static String compose( WebResponseBundle webResponseBundle )
	{
		String message = composeHead( webResponseBundle.callbackIntent );
		
		message += String.format("<%s %s=\"%s\" %s=\"%s\" %s=\"%s\" >", 
				NODE_HTTP_RESPONSE,
				NODE_HTTP_RESPONSE_STATUS_CODE_ATTR, webResponseBundle.statusCode,
				NODE_HTTP_RESPONSE_STATUS_LINE_ATTR, webResponseBundle.statusLine,
				NODE_HTTP_RESPONSE_REQUEST_ID_ATTR, webResponseBundle.requestId
				);
		
		message += webResponseBundle.contentInBase64;
		
		message += String.format("</%s>",
				NODE_HTTP_RESPONSE
				);
			
		message = appendEnding( message );

		return message;
	}
	
	public static WebResponseBundle parse(String message, String callbackIntent) {

		Document doc = XMLMessage.validate(callbackIntent , message);
		if(doc==null) return null;

		WebResponseBundle responseBundle = null;

		try {
			
			Element httpResponseElement = (Element)doc.getElementsByTagName("http-response").item(0);
			String statusCode = httpResponseElement.getAttribute(NODE_HTTP_RESPONSE_STATUS_CODE_ATTR);
			String statusLine = httpResponseElement.getAttribute(NODE_HTTP_RESPONSE_STATUS_LINE_ATTR);
			String requestId = httpResponseElement.getAttribute(NODE_HTTP_RESPONSE_REQUEST_ID_ATTR);
			String contentInBase64 = httpResponseElement.getTextContent();
			
			responseBundle = new WebResponseBundle(callbackIntent, statusCode, statusLine, requestId , contentInBase64 );
			
		} 
		catch (Exception e) {
			Log.e( TAG, "Error parsing XML message",e);
		} 
		return responseBundle;
	}

}
