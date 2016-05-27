package demo.reconinstruments.com.buddytracking;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;

public class BuddyViewActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    ArrayList<BuddyInfo> mBuddiesInfo = new ArrayList<BuddyInfo>();

    BuddyListAdapter mCustomAdapter = null;

    BuddyInfoReceiver mBuddyInfoReceiver = new BuddyInfoReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buddy_view);

        ListView yourListView = (ListView) findViewById(R.id.buddiesListView);

        mCustomAdapter = new BuddyListAdapter(this, R.layout.single_buddy, mBuddiesInfo);

        yourListView.setAdapter(mCustomAdapter);

        registerReceiver(mBuddyInfoReceiver, new IntentFilter(BuddyInfoReceiver.BUDDY_INFO_MESSAGE));
    }

    class BuddyInfoReceiver extends BroadcastReceiver {
        private final String TAG = this.getClass().getSimpleName();

        static public final String BUDDY_INFO_MESSAGE = "RECON_FRIENDS_LOCATION_UPDATE";

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Log.i(TAG, "onReceive() BuddyInfoReceiver");
                byte[] bytes = bundle.getByteArray("message");
                HUDConnectivityMessage cMsg = new HUDConnectivityMessage(bytes);
                String buddyInfoMsg = new String(cMsg.getData());

                BuddyInfoParser.parse(BUDDY_INFO_MESSAGE, buddyInfoMsg, mBuddiesInfo);
                mCustomAdapter.notifyDataSetChanged();
            }
        }
    }
}
