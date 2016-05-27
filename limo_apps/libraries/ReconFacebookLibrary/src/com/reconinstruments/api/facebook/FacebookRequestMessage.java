package com.reconinstruments.api.facebook;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;


public class FacebookRequestMessage {
	
	static final String TAG = "WebRequestMessage";

	static String intent = "RECON_FACEBOOK_REQUEST";

	static final String NODE_CALLBACK = "callback";
	static final String NODE_CALLBACK_INTENT_ATTR = "intent";

	static final String NODE_FACEBOOK_REQUEST = "facebook-request";
	static final String NODE_FACEBOOK_REQUEST_PATH_ATTR = "path"; // 
	static final String NODE_FACEBOOK_REQUEST_METHOD_ATTR = "method";

	static final String NODE_FACEBOOK_REQUEST_PARAMETER = "parameters";

	public static enum HttpMethod{
		GET,POST,DELETE
	};

	public static class FacebookRequestBundle {

		// callback intent
		public String callbackIntent;	
		// Facebook Address
		public String path;
		// type of HTTP REQUEST
		public HttpMethod method;
		
		// Request Headers That can be used for post.
		public List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

		public FacebookRequestBundle(
				String callbackIntent,
				String path,
				HttpMethod method,
				List<NameValuePair> nameValuePairs
				) {

			this.callbackIntent = callbackIntent;
			this.path = path;
			this.method = method;
			this.nameValuePairs = nameValuePairs;

		}

	}

	public static String compose( FacebookRequestBundle facebookRequestBundle )
	{
		String message = XMLMessage.composeHead( intent );

		if (facebookRequestBundle.nameValuePairs == null)
			facebookRequestBundle.nameValuePairs = new ArrayList<NameValuePair>();

		message += String.format("<%s %s=\"%s\" />\n" +
				"<%s %s=\"%s\" %s=\"%s\" />",
				NODE_CALLBACK,
				NODE_CALLBACK_INTENT_ATTR, facebookRequestBundle.callbackIntent,
				NODE_FACEBOOK_REQUEST,
				NODE_FACEBOOK_REQUEST_PATH_ATTR, facebookRequestBundle.path,
				NODE_FACEBOOK_REQUEST_METHOD_ATTR, facebookRequestBundle.method	
				);

		for (int i = 0 ; i < facebookRequestBundle.nameValuePairs.size() ; i++){
			message += String.format("<%s %s=\"%s\" />",
					NODE_FACEBOOK_REQUEST_PARAMETER, 
					facebookRequestBundle.nameValuePairs.get(i).getName(),
					facebookRequestBundle.nameValuePairs.get(i).getValue()
					);
		}

		message = XMLMessage.appendEnding( message );

		return message;
	}

	public static FacebookRequestBundle parse(String message) {

		Document doc = XMLMessage.validate(intent,message);
		if(doc==null) return null;

		FacebookRequestBundle requestBundle = null;

		try {

			Element callbackElement = (Element)doc.getElementsByTagName(NODE_CALLBACK).item(0);
			String callbackIntent = callbackElement.getAttribute(NODE_CALLBACK_INTENT_ATTR);

			Element facebookResponseElement = (Element)doc.getElementsByTagName(NODE_FACEBOOK_REQUEST).item(0);
			String path = facebookResponseElement.getAttribute(NODE_FACEBOOK_REQUEST_PATH_ATTR);
			HttpMethod methodType = HttpMethod.valueOf(facebookResponseElement.getAttribute(NODE_FACEBOOK_REQUEST_METHOD_ATTR));
			
			NodeList httpHeaderElements = doc.getElementsByTagName(NODE_FACEBOOK_REQUEST_PARAMETER);
			List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();

			for (int i = 0 ; i < httpHeaderElements.getLength() ; i++){
				Element httpHeaderElement = (Element)httpHeaderElements.item(i);
				Attr attributes = (Attr)httpHeaderElement.getAttributes().item(0);
				nameValuePair.add(new BasicNameValuePair(attributes.getName(), attributes.getValue()));
			}

			requestBundle = new FacebookRequestBundle(callbackIntent, path , methodType, nameValuePair);

		} 
		catch (Exception e) {
			Log.e( TAG, "Error parsing XML message",e);
		} 
		return requestBundle;
	}

}
