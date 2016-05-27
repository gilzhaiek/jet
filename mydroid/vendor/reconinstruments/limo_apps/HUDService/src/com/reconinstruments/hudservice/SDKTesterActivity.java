package com.reconinstruments.hudservice;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import com.reconinstruments.commonwidgets.ReconToast;
public class SDKTesterActivity extends Activity {

    private static final String TAG = "SDKTesterActivity";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_test);
	startService(new Intent("RECON_HUD_SERVICE"));
    }

    public void onResume() {
	super.onResume();
	ReconToast rtoast = new ReconToast(this, "Phone battery low");
	rtoast.show();
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
	stopService(new Intent("RECON_HUD_SERVICE"));
    }

}
