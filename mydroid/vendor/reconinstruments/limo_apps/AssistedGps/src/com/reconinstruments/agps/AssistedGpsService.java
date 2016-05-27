package com.reconinstruments.agps;
import android.app.Service;
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
import android.os.IBinder;

public class AssistedGpsService extends Service
{
    private static String TAG = "AssistedGpsActivity";
    private ReconAGpsContext mReconAGpsContext;

    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
	mReconAGpsContext = new ReconAGpsContext(this);
	mReconAGpsContext.initialize();
    }
    public int onStartCommand(Intent intent, int flags, int startid){
	return START_STICKY;
    }
    public void onDestroy() {
	mReconAGpsContext.cleanUp();
	super.onDestroy();
    }

}
