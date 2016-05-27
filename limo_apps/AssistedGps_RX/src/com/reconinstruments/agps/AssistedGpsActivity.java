package com.reconinstruments.agps;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.webapi.IReconHttpCallback;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;
import com.reconinstruments.webapi.ReconOSHttpClient;
import com.reconinstruments.webapi.ReconWebApiClient;
import java.net.MalformedURLException;
import java.net.URL;
public class AssistedGpsActivity extends Activity {
    private static String TAG = "AssistedGpsActivity";
    // boilerplate:x
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	startService(new Intent("com.reconinstruments.agps.AssistedGpsService"));

	final Button fakeLocation = (Button) findViewById(R.id.fake_location);
	fakeLocation.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    // Create a location object and broadcast it.
		    Location loc = new Location("RECON_ENGAGE");
		    loc.setLatitude(49.0); // mandetory
		    loc.setLongitude(-127.0); // mandetory
		    //loc.setTime(System.currentTimeMillis()); // mandetory
		    loc.setTime(1407545852301L);
		    String xml = ReconAGps.locationToXml(loc);
		    fakeIncomingLocationFromPhone(xml);
		}
	    });

    }
    public void onDestroy() {
	super.onDestroy();
    }
    private void fakeIncomingLocationFromPhone (String xml) {
	Log.v("Crap","Sending fake Incoming location from phone");
	Intent intent = new Intent(ReconAGps.LOCATION_UPDATE_INTENT);
	HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
	cMsg.setSender(TAG);
	cMsg.setIntentFilter(ReconAGps.LOCATION_UPDATE_INTENT);
	cMsg.setData(xml.getBytes());
	intent.putExtra(HUDConnectivityMessage.TAG, cMsg.toByteArray());
	sendBroadcast(intent);
    }
}
