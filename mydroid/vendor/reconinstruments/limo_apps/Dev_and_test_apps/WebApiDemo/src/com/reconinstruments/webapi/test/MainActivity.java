package com.reconinstruments.webapi.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.webapi.IReconHttpCallback;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;
import com.reconinstruments.webapi.ReconWebApiClient;
import com.reconinstruments.webapi.ReconOSHttpClient;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private TextView textView;
	private ReconOSHttpClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		textView = (TextView) findViewById(R.id.text_view);
		textView.setMovementMethod(new ScrollingMovementMethod());
		// The idea is that you instantiate an http client
		// object with a call back and then keep feeding it ReconHttpRequests
		client = new ReconOSHttpClient(this, clientCallback);
}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		client.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void goodRequest(View v) {
		Log.d(TAG, "Fetching data...");
		try {
			URL url = new URL("https://posttestserver.com/post.php?dump");
			Map<String, List<String>> headers = new HashMap<String, List<String>>();
			headers.put("MY-HEADER", Arrays.asList(new String[] { "something" }));
			headers.put("MY-OTHER-HEADER", Arrays.asList(new String[] { "value 1", "value 2" }));
			byte[] body = "this is my test body".getBytes();
			sendRequest(new ReconHttpRequest("POST", url, 5000, headers, body));
		} catch (MalformedURLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	public void tooBigRequest(View v) {
		Log.d(TAG, "Fetching data...");
		try {
			URL url = new URL("https://google.com/");
			Map<String, List<String>> headers = new HashMap<String, List<String>>();
			sendRequest(new ReconHttpRequest("GET", url, 5000, headers, null));
		} catch (MalformedURLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	public void missingRequest(View v) {
		Log.d(TAG, "Fetching data...");
		try {
			URL url = new URL("https://google.com/fakepage");
			Map<String, List<String>> headers = new HashMap<String, List<String>>();
			sendRequest(new ReconHttpRequest("GET", url, 5000, headers, null));
		} catch (MalformedURLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	public void timeoutRequest(View v) {
		Log.d(TAG, "Fetching data...");
		try {
			URL url = new URL("http://posttestserver.com/post.php?dump&sleep=15");
			Map<String, List<String>> headers = new HashMap<String, List<String>>();
			sendRequest(new ReconHttpRequest("POST", url, 5000, headers, null));
		} catch (MalformedURLException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	public void sendRequest(ReconHttpRequest request) {
		if (-1 == client.sendRequest(request)) {
			Toast.makeText(MainActivity.this, "HUD not connected", Toast.LENGTH_SHORT).show();
			textView.setText("");
		} else {
			Toast.makeText(MainActivity.this, "Request Sent", Toast.LENGTH_SHORT).show();
		}
	}

	private IReconHttpCallback clientCallback = new IReconHttpCallback() {
		@Override
		public void onReceive(int requestId, ReconHttpResponse response) {
			Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
			textView.setText(new String(response.getBody()));
		}

		@Override
		public void onError(int requestId, ERROR_TYPE type, String message) {
			Toast.makeText(MainActivity.this, "Error: " + type.toString() + "(" + message + ")", Toast.LENGTH_SHORT).show();
			textView.setText("");
		}

	};
}
