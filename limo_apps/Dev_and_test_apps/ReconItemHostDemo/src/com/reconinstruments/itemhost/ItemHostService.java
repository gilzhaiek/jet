
package com.reconinstruments.itemhost;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * <code>ItemHostService</code> is designed to demo how to produce and broadcast
 * a remoteView in a service. It produces a remoteView which has different
 * layout for every 5 seconds.
 */
public class ItemHostService extends Service {

    private static final String TAG = ItemHostService.class.getSimpleName();

    public final static String INTENT_ACTION = "RECON_WIDGET";
    public final static String EXTRA_VIEW = "REMOTE_VIEW";
    public final static String EXTRA_ITEM = "ITEM";

    private final static int POST_DELAYED = 5 * 1000;

    private final Handler itemHostHandler = new Handler();
    private int mCounter = 0;

    // generate a remoteView and broadcast it for every 5 seconds.
    private Runnable broadcastRemoteView = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "mCounter=" + mCounter);
            mCounter++;
            Intent intent = null;
            if (mCounter % 2 == 0) {
                intent = generateRemoteViewIntentForItem1();
            } else if (mCounter % 3 == 0) {
                intent = generateRemoteViewIntentForItem2();
            } else {
                intent = generateRemoteViewIntentForItem3();
            }
            getApplicationContext().sendBroadcast(intent);
            itemHostHandler.postDelayed(this, POST_DELAYED);
        }

    };

    private Intent generateRemoteViewIntentForItem1() {
        RemoteViews remoteView = null;
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION);
        remoteView = new RemoteViews(getPackageName(), R.layout.remote_view1);
        remoteView.setTextViewText(R.id.textView, (mCounter) + " for Item 1");
        intent.putExtra(EXTRA_ITEM, ItemHostActivity.ITEM_1);
        remoteView.setTextViewText(R.id.textView, (mCounter) + " Messenger");
        intent.putExtra(EXTRA_VIEW, remoteView);
        return intent;
    }

    private Intent generateRemoteViewIntentForItem2() {
        RemoteViews remoteView = null;
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION);
        remoteView = new RemoteViews(getPackageName(), R.layout.remote_view2);
        remoteView.setTextViewText(R.id.textView, (mCounter) + " for Item 2");
        intent.putExtra(EXTRA_ITEM, ItemHostActivity.ITEM_2);
        remoteView.setTextViewText(R.id.textView, (mCounter) + " Messenger");
        intent.putExtra(EXTRA_VIEW, remoteView);
        return intent;
    }

    private Intent generateRemoteViewIntentForItem3() {
        RemoteViews remoteView = null;
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION);
        remoteView = new RemoteViews(getPackageName(), R.layout.remote_view3);
        remoteView.setTextViewText(R.id.textView, (mCounter) + " for Item 3");
        intent.putExtra(EXTRA_ITEM, ItemHostActivity.ITEM_3);
        remoteView.setTextViewText(R.id.textView, (mCounter) + " Messenger");
        intent.putExtra(EXTRA_VIEW, remoteView);
        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        itemHostHandler.postDelayed(broadcastRemoteView, POST_DELAYED);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        itemHostHandler.removeCallbacks(broadcastRemoteView);
        super.onDestroy();
    }

}
