//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.test.sticky_intent;

import android.app.Activity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.util.Log;

public class StickyIntentActivity extends Activity
{
    private static final String TAG = "StickyIntentActivity";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    public void onResume()  {
        super.onResume();
        registerReceiver(mStickyReceiver,new IntentFilter("com.reconinstruments.applauncher.transcend.GPS_FIX_CHANGED"));
        Intent intent = registerReceiver(mFullInfoReceiver,new IntentFilter("com.reconinstruments.applauncher.transcend.FULL_INFO_UPDATED"));
        if (intent != null) {
            Log.v(TAG,"The first time getting it");
            Bundle fullinfo = (Bundle)intent.getParcelableExtra("FullInfo");
            int status = ((Bundle)fullinfo.get("SPORTS_ACTIVITY_BUNDLE"))
                .getInt("Status");
            // status: 0 no activiity
            // 1: ongoing. 2: paused. 
            int type = ((Bundle)fullinfo.get("SPORTS_ACTIVITY_BUNDLE"))
                .getInt("Type");
            // type: 0: skiing. 1: cycling. 2: running.
            Log.v(TAG,"Type is "+ type+" status is "+status);
        }
    }
    public void onPause() {
        unregisterReceiver(mStickyReceiver);
        unregisterReceiver(mFullInfoReceiver);
        super.onPause();
    }
    
    BroadcastReceiver mStickyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isFix = intent.getBooleanExtra("isGpsFix",false);
                Log.v(TAG,"Gps State Changed" + isFix);
            }
        };

    BroadcastReceiver mFullInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent )  {
                Bundle fullinfo = (Bundle)intent.getParcelableExtra("FullInfo");
                float speed = ((Bundle)fullinfo.get("SPEED_BUNDLE"))
                    .getFloat("Speed");
                Log.v(TAG,"Speed is "+ speed);
            }
        };
}
