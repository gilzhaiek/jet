
package com.reconinstruments.remoteview;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 
 * <code>MainActivity</code> displays a remoteview getting from broadcast receiver.
 *
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    
    private final BroadcastReceiver remoteViewReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RemoteViews views = (RemoteViews) intent.getExtras().getParcelable(ItemHostService.INTENT_EXTRA);
            ViewGroup vg = (ViewGroup) findViewById(R.id.remoteview);
            View inflatedView = views.apply(context, vg);
            if (vg.getChildCount() > 0){
                vg.removeAllViews();
            }
            vg.addView(inflatedView);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //for demo only, start the service as long as the activity is started.
        startService(new Intent(this, ItemHostService.class));
        
        registerReceiver(remoteViewReceiver, new IntentFilter(ItemHostService.INTENT_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        //for demo only, stop the service as long as the activity is stopped.
        stopService(new Intent(this, ItemHostService.class));
        
        try {
            unregisterReceiver(remoteViewReceiver);
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                e.printStackTrace();
            }
        }
    }

}
