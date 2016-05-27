package com.reconinstruments.webapi;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Base64;
import android.util.Log;

/**
 *Copyright 2012 Recon Instruments
 *All Rights Reserved.
 */
/**
 * @author Patrick Cho
 * 
 *  This class is used to parse and compose an XML Message containing a web service request message in base64.
 */
public class WebRequestMessage extends XMLMessage {

	static final String TAG = "WebRequestMessage";

	static String intent = WEB_REQUEST_MESSAGE;

	static final String NODE_CALLBACK = "callback";
	static final String NODE_CALLBACK_INTENT_ATTR = "intent";

	static final String NODE_HTTP_REQUEST = "http-request";
	static final String NODE_HTTP_REQUEST_URL_ATTR = "url";
	static final String NODE_HTTP_REQUEST_METHOD_ATTR = "method";
	static final String NODE_HTTP_REQUEST_REQUEST_ID_ATTR = "request-id";

	static final String NODE_HTTP_HEADER = "http-header";
	static final String NODE_HTTP_PARAMETER = "http-parameter";
	static final String NODE_HTTP_JSON = "http-json";
	static final String NODE_HTTP_DATA= "http-data";

	public enum WebMethod{
		GET,POST,PUT,DELETE,HEAD
	}

	public static class WebRequestBundle {

		// callback intent
		public String callbackIntent;
		// URL Address
		public String URL;
		// type of HTTP REQUEST
		public WebMethod method;
		// id of request
		public String requestId;
		
		// Request Headers
		public List<NameValuePair> headerNameValuePairs = new ArrayList<NameValuePair>();
		// Request Parameters		
		public List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		// Request jsonObject
		public JSONObject jsonObject = null;
		// Request dataString
		public String dataString = null;
		
		/**
		 * <h1>***This works with all REST***</h1> <hr>
		 * 
		 * @param <b>callbackIntent</b> <em>You can put any unique action name for intent filter.</em>
		 * @param <b>URL This is the</b> <em>url you are requesting</em> 
		 * @param <b>method</b> <em>This is the type of request method </em>
		 * @param <b>requestId</b> <em>This is used for distinguishing when getting response</em> 
		 * @param <b>headerNameValuePairs</b> <em>You can pass list of header</em>
		 * @param <b>nameValuePairs</b> <em>You can pass list of parameters, if you are posting data, or json string, see below</em> <br>
		 * @see WebRequestBundle( String callbackIntent, String URL, WebMethod method, String requestId,<br>
		 * List<NameValuePair> headerNameValuePairs, JSONObject jsonObject )
		 * @see WebRequestBundle( String callbackIntent, String URL, WebMethod method, String requestId,<br>
		 * List<NameValuePair> headerNameValuePairs, String dataString)
		 */
		public WebRequestBundle(
				String callbackIntent,
				String URL,
				WebMethod method,
				String requestId,
				List<NameValuePair> headerNameValuePairs,
				List<NameValuePair> nameValuePairs
				) {

			this.callbackIntent = callbackIntent;
			this.URL = URL;
			this.method = method;
			this.requestId = requestId;
			if (headerNameValuePairs != null)
				this.headerNameValuePairs = headerNameValuePairs;
			if (nameValuePairs != null)
				this.nameValuePairs = nameValuePairs;

		}
		
		/**
		 * <h1>***This won't work with GET + DELETE since it has data entity***</h1> <hr>
		 * 
		 * @param <b>callbackIntent</b> <em>You can put any unique action name for intent filter.</em>
		 * @param <b>URL This is the</b> <em>url you are requesting</em> 
		 * @param <b>method</b> <em>This is the type of request method </em>
		 * @param <b>requestId</b> <em>This is used for distinguishing when getting response</em> 
		 * @param <b>headerNameValuePairs</b> <em>You can pass list of header</em>
		 * @param <b>jsonObject</b> <em>You can pass JSONObject</em>
		 */
		public WebRequestBundle(
				String callbackIntent,
				String URL,
				WebMethod method,
				String requestId,
				List<NameValuePair> headerNameValuePairs,
				JSONObject jsonObject
				) {

			this.callbackIntent = callbackIntent;
			this.URL = URL;
			this.method = method;
			this.requestId = requestId;
			if (headerNameValuePairs != null)
				this.headerNameValuePairs = headerNameValuePairs;
			this.jsonObject = jsonObject;

		}
		
		/**
		 * <h1>***This won't work with GET + DELETE since it has data entity***</h1><hr>
		 * 
		 * @param <b>callbackIntent</b> <em>You can put any unique action name for intent filter.</em>
		 * @param <b>URL This is the</b> <em>url you are requesting</em> 
		 * @param <b>method</b> <em>This is the type of request method </em>
		 * @param <b>requestId</b> <em>This is used for distinguishing when getting response</em> 
		 * @param <b>headerNameValuePairs</b> <em>You can pass list of header</em>
		 * @param <b>jsonObject</b> <em>You can pass JSONObject</em>
		 */
		public WebRequestBundle(
				String callbackIntent,
				String URL,
				WebMethod method,
				String requestId,
				List<NameValuePair> headerNameValuePairs,
				String dataString
				) {

			this.callbackIntent = callbackIntent;
			this.URL = URL;
			this.method = method;
			this.requestId = requestId;
			if (headerNameValuePairs != null)
				this.headerNameValuePairs = headerNameValuePairs;
			this.dataString = dataString;

		}

	}

	public static String compose( WebRequestBundle webRequestBundle )
	{
		String message = composeHead( intent );

		if (webRequestBundle.headerNameValuePairs == null)
			webRequestBundle.headerNameValuePairs = new ArrayList<NameValuePair>();
		if (webRequestBundle.nameValuePairs == null)
			webRequestBundle.nameValuePairs = new ArrayList<NameValuePair>();

		message += String.format("<%s %s=\"%s\" />\n" +
				"<%s %s=\"%s\" %s=\"%s\" %s=\"%s\" />",
				NODE_CALLBACK,
				NODE_CALLBACK_INTENT_ATTR, webRequestBundle.callbackIntent,
				NODE_HTTP_REQUEST,
				NODE_HTTP_REQUEST_URL_ATTR, webRequestBundle.URL,
				NODE_HTTP_REQUEST_METHOD_ATTR, webRequestBundle.method,	
				NODE_HTTP_REQUEST_REQUEST_ID_ATTR, webRequestBundle.requestId
				);

		for (int i = 0 ; i < webRequestBundle.headerNameValuePairs.size() ; i++){
			message += String.format("<%s %s=\"%s\" />",
					NODE_HTTP_HEADER,
					webRequestBundle.headerNameValuePairs.get(i).getName(),
					new String(Base64.encode(webRequestBundle.headerNameValuePairs.get(i).getValue().getBytes() , Base64.DEFAULT))
					);
		}
		
		for (int i = 0 ; i < webRequestBundle.nameValuePairs.size() ; i++){
			message += String.format("<%s %s=\"%s\" />",
					NODE_HTTP_PARAMETER, 
					webRequestBundle.nameValuePairs.get(i).getName(),
					new String(Base64.encode(webRequestBundle.nameValuePairs.get(i).getValue().getBytes() , Base64.DEFAULT))
					);
		}
		
		if (webRequestBundle.jsonObject != null){
			message += String.format("<%s %s=\"%s\" />",
					NODE_HTTP_JSON,
					"data",
					new String(Base64.encode(webRequestBundle.jsonObject.toString().getBytes(), Base64.DEFAULT))
					);
		}
		
		if (webRequestBundle.dataString != null){
			message += String.format("<%s %s=\"%s\" />",
					NODE_HTTP_DATA,
					"data",
					new String(Base64.encode(webRequestBundle.dataString.getBytes(), Base64.DEFAULT))
					);
		}

		message = appendEnding( message );

		return message;
	}

	public static WebRequestBundle parse(String message) {

		Document doc = XMLMessage.validate(intent,message);
		if(doc==null) return null;

		WebRequestBundle requestBundle = null;

		try {

			Element callbackElement = (Element)doc.getElementsByTagName(NODE_CALLBACK).item(0);
			String callbackIntent = callbackElement.getAttribute(NODE_CALLBACK_INTENT_ATTR);

			Element httpResponseElement = (Element)doc.getElementsByTagName(NODE_HTTP_REQUEST).item(0);
			String URL = httpResponseElement.getAttribute(NODE_HTTP_REQUEST_URL_ATTR);
			WebMethod methodType = WebMethod.valueOf(httpResponseElement.getAttribute(NODE_HTTP_REQUEST_METHOD_ATTR));
			String requestId = httpResponseElement.getAttribute(NODE_HTTP_REQUEST_REQUEST_ID_ATTR);

			NodeList httpHeaderElements = doc.getElementsByTagName(NODE_HTTP_HEADER);
			List<NameValuePair> headerNameValuePair = new ArrayList<NameValuePair>();
			
			NodeList httpParameterElements = doc.getElementsByTagName(NODE_HTTP_PARAMETER);
			List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
			
			NodeList httpJSONElements = doc.getElementsByTagName(NODE_HTTP_JSON);
			JSONObject jsonObject = null;
			
			NodeList httpDataElements = doc.getElementsByTagName(NODE_HTTP_DATA);
			String dataString = null;

			for (int i = 0 ; i < httpHeaderElements.getLength() ; i++){
				Element httpHeaderElement = (Element)httpHeaderElements.item(i);
				Attr attributes = (Attr)httpHeaderElement.getAttributes().item(0);
				headerNameValuePair.add(new BasicNameValuePair(
						attributes.getName(),
						WebAPIUtils.decodeBase64String(attributes.getValue())
						)
				);
			}
			
			for (int i = 0 ; i < httpParameterElements.getLength() ; i++){
				Element httpParameterElement = (Element)httpParameterElements.item(i);
				Attr attributes = (Attr)httpParameterElement.getAttributes().item(0);
				nameValuePair.add(new BasicNameValuePair(
						attributes.getName(),
						WebAPIUtils.decodeBase64String(attributes.getValue())
						)
				);
			}
			
			if (httpJSONElements.getLength() > 0){
				Element httpJSONElement = (Element)httpJSONElements.item(0);
				Attr attributes = (Attr)httpJSONElement.getAttributes().item(0);
				
				try {
					jsonObject = new JSONObject(WebAPIUtils.decodeBase64String(attributes.getValue()));
				} catch (JSONException e){
					Log.e(TAG, e.getLocalizedMessage());
					Log.e(TAG, "JSONObject object creation failed");
				}
			}
			
			if (httpDataElements.getLength() > 0){
				Element httpDataElement = (Element)httpDataElements.item(0);
				Attr attributes = (Attr)httpDataElement.getAttributes().item(0);
				dataString = WebAPIUtils.decodeBase64String(attributes.getValue());
			}
			
			if (jsonObject != null){
				requestBundle = new WebRequestBundle(callbackIntent, URL , methodType, requestId, headerNameValuePair, jsonObject);
			}
			else if (dataString != null){
				requestBundle = new WebRequestBundle(callbackIntent, URL , methodType, requestId, headerNameValuePair, jsonObject);
			}
			else{
				requestBundle = new WebRequestBundle(callbackIntent, URL , methodType, requestId, headerNameValuePair, nameValuePair);
			}
		} 
		catch (Exception e) {
			Log.e( TAG, "Error parsing XML message",e);
		} 
		return requestBundle;
	}

}
