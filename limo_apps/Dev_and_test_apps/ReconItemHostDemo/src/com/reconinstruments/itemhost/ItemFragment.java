
package com.reconinstruments.itemhost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

/**
 * <code>ItemFragment</code> is designed to represent the remote view item
 * layout. It receives the remote view from <code>ItemHostService</code> and
 * show the content as an item.
 */
public class ItemFragment extends Fragment {
    private int mItem;

    public ItemFragment(int item) {
        mItem = item;
    }

    public int getItem() {
        return mItem;
    }

    public void setItem(int item) {
        this.mItem = item;
    }

    // the receiver receives the remote view from ItemHostService and then apply
    // to one of item content.
    private final BroadcastReceiver remoteViewReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RemoteViews views = (RemoteViews) intent.getExtras().getParcelable(
                    ItemHostService.EXTRA_VIEW);
            int item = intent.getExtras().getInt(ItemHostService.EXTRA_ITEM, ItemHostActivity.ITEM_3);
            if (getItem() == item) {
                ViewGroup vg = (ViewGroup) getView().findViewById(R.id.remoteview);
                View inflatedView = views.apply(context, vg);
                if (vg.getChildCount() > 0) {
                    vg.removeAllViews();
                }
                vg.addView(inflatedView);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_initial, container, false);
        TextView messageTextView = (TextView) v.findViewById(R.id.textView1);
        messageTextView.setText("Coming soon...");
        v.setBackgroundColor(Color.argb(255, (getItem() * 1) * 40, (getItem() * 3) * 10,
                (getItem() * 2) * 40));

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getActivity().registerReceiver(remoteViewReceiver,
                new IntentFilter(ItemHostService.INTENT_ACTION));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        try {
            getActivity().unregisterReceiver(remoteViewReceiver);
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

}
