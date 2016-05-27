package com.reconinstruments.agps;

import android.app.Activity;
import android.os.Bundle;
import java.net.URL;
import java.net.MalformedURLException;
import com.reconinstruments.webapi.IReconHttpCallback;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;
import com.reconinstruments.webapi.ReconWebApiClient;
import com.reconinstruments.webapi.ReconOSHttpClient;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import android.content.IntentFilter;
import android.content.Intent;

public class AssistedGpsActivity extends Activity
{
    private static String TAG = "AssistedGpsActivity";
    private ReconAGpsContext mReconAGpsContext;

    // boilerplate:x
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	mReconAGpsContext = new ReconAGpsContext(this);
	mReconAGpsContext.initialize();
	startService(new Intent("com.reconinstruments.agps.AssistedGpsService"));
    }
    public void onDestroy() {
	mReconAGpsContext.cleanUp();
	super.onDestroy();
    }

}
