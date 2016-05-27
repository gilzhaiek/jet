package com.reconinstruments.example.webapi;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.reconinstruments.webapi.SDKWebService;
import com.reconinstruments.webapi.SDKWebService.WebResponseListener;
import com.reconinstruments.webapi.WebRequestMessage.WebMethod;
import com.reconinstruments.webapi.WebRequestMessage.WebRequestBundle;

/**
 * 
 *  Simple Example that shows the guideline for using WebAPIs.
 * 
 * @author Patrick Cho
 *
 */

public class WebTestActivity extends Activity {

	private static final String TAG = "WebTestActivity";
	private WebView wv;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		wv = (WebView) findViewById(R.id.main_webview);

		// make a instance of WebRequestBundle, which contains all the information about
		// HTTP request
		List<NameValuePair> values = new ArrayList<NameValuePair>();
		values.add(new BasicNameValuePair("","{\"format\": \"json\", \"topic\": \"order/created\", \"url\": \"http://myshop.example.com/notify_me\"}"));

		WebRequestBundle wrb = new WebRequestBundle("IntentFilterActionName",
				"http://postcatcher.in/catchers/50b512a01911cb0200000127" , WebMethod.POST, "1" /*Any Unique Number So you can distinguish each request*/, values);

		SDKWebService.httpRequest(getBaseContext(), wrb, new WebResponseListener(){

			@Override
			public void onComplete(byte[] response, String statusCode,
					String statusId, String requestId) {
				Log.d(TAG, "onComplete Byte[]" + new String(response));

			}

			@Override
			public void onComplete(String response, String statusCode,
					String statusId, String requestId) {
				Log.d(TAG, "onComplete String" + response);
			}});


		/*
		List<NameValuePair> values = new ArrayList<NameValuePair>();
		values.add(new BasicNameValuePair("name","value"));
		values.add(new BasicNameValuePair("name2","value2"));

		WebRequestBundle wrb = new WebRequestBundle("any-unique-callback",
				"http://www.postdata.com" , WebMethod.POST, "request_id", values);

		SDKWebService.httpRequest(getBaseContext(), wrb, new ResponseListener(){

			@Override
			public void onComplete(byte[] response, String statusCode,
					String statusId, String requestId) {
				Log.e(TAG, new String(response));

			}

			@Override
			public void onComplete(String response, String statusCode,
					String statusId, String requestId) {
				wv.loadData(response, "text/html", null);
				Log.e(TAG, response);
			}});

		 */
	}
}