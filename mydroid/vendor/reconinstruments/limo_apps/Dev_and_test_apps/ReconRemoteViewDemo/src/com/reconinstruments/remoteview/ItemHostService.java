
package com.reconinstruments.remoteview;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * 
 * <code>ItemHostService</code> is designed to demo how to produce and broadcast a remoteView in a service. 
 * It produces a remoteView for every 5 seconds.
 *
 */
public class ItemHostService extends Service {

    private static final String TAG = ItemHostService.class.getSimpleName();

    public final static String INTENT_ACTION = "RECON_WIDGET";
    public final static String INTENT_EXTRA = "REMOTE_VIEW";
    
    private final static int POST_DELAYED = 5 * 1000;

    private final Handler itemHostHandler = new Handler();
    private int mCounter = 0;

    private Runnable generateRemoteView = new Runnable() {

        @Override
        public void run() {
            Log.e(TAG, "mCounter=" + mCounter);
            mCounter++;
            RemoteViews remoteView =
                    new RemoteViews(getPackageName(), R.layout.remote_view);
            remoteView.setTextViewText(R.id.textView, (mCounter) + " Messenger");
            Intent intent = new Intent();
            intent.setAction(INTENT_ACTION);
            intent.putExtra(INTENT_EXTRA, remoteView);
            getApplicationContext().sendBroadcast(intent);
            itemHostHandler.postDelayed(this, POST_DELAYED);
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        itemHostHandler.postDelayed(generateRemoteView, POST_DELAYED);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        itemHostHandler.removeCallbacks(generateRemoteView);
        super.onDestroy();
    }

}
