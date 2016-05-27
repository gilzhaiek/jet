package com.reconinstruments.messagecenter.frontend;

import java.util.TimeZone;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.ViewGroup;

import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.R;

public class MessageGroupViewerFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "MessageGroupViewerFragment";

	MessageGroupCursorAdapter mAdapter = null;
	LoaderManager loadermanager;

	TimeZone lastTZ;

	public static MessageGroupViewerFragment newInstance() {
		return new MessageGroupViewerFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.grp_list_activity, container,
				false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		loadermanager = getLoaderManager();

		mAdapter = new MessageGroupCursorAdapter(getActivity(), null);
		setListAdapter(mAdapter);
		getListView().setOnItemClickListener(onGroupSelect);

		loadermanager.initLoader(1, null, this);

		// insertTestData();
		// clearAll();
		// testPopup();

		lastTZ = TimeZone.getDefault();
	}

	OnItemClickListener onGroupSelect = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// Launching new Activity on selecting single List Item
			Intent intent = new Intent(getActivity().getApplicationContext(),
					MessageCategoryViewer.class);
			// sending data to new activity
			intent.putExtra("group_id", (int) id);
			startActivity(intent);
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return MessageHelper.getGroups(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor); // swap the new cursor in.
		getListView().requestFocus();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}

	// public void insertTestData() {
	// /*
	// * ReconMessageAPI.postNotification(this, "com.test1", "Group 1", 0,
	// * "testcat1", "Category 1", 0, null, null, null, null, null, false,
	// * "message1", MessagePriority.MESSAGE_PRIORITY_NORMAL,"");
	// * ReconMessageAPI.postNotification(this, "com.test1", "Group 1", 0,
	// * "testcat2", "Category 2", 0, null, null, null, null, null, false,
	// * "message2", MessagePriority.MESSAGE_PRIORITY_NORMAL,"");
	// * ReconMessageAPI.postNotification(this, "com.test2", "Group 2", 0,
	// * "testcat3", "Category 3", 0, null, null, null, null, null, false,
	// * "message3", MessagePriority.MESSAGE_PRIORITY_NORMAL,"");
	// * ReconMessageAPI.postNotification(this, "com.test3", "Group 3", 0,
	// * "testcat4", "Category 4", 0, null, null, null, null, null, false,
	// * "message4", MessagePriority.MESSAGE_PRIORITY_NORMAL,"");
	// * ReconMessageAPI.postNotification(this, "com.test4", "Group 4", 0,
	// * "testcat5", "Category 5", 0, null, null, null, null, null, false,
	// * "message5", MessagePriority.MESSAGE_PRIORITY_NORMAL,"");
	// * ReconMessageAPI.postNotification(this, "com.test5", "Group 5", 0,
	// * "testcat6", "Category 6", 0, null, null, null, null, null, false,
	// * "message6", MessagePriority.MESSAGE_PRIORITY_NORMAL,"");
	// */
	// ReconMessageAPI.postNotification(getActivity(),
	// "com.reconinstruments.calls", "MISSED CALLS", 0, "Babo",
	// "Babo", 0, null, null, new Intent("RECON_CALL_HISTORY"),
	// "View", null, false, "",
	// MessagePriority.MESSAGE_PRIORITY_NORMAL);
	// ReconMessageAPI.postNotification(getActivity(),
	// "com.reconinstruments.calls", "CALLS", 0, "Bibo", "Bibo", 0,
	// null, null, new Intent("RECON_CALL_HISTORY"), "View", null,
	// false, "", MessagePriority.MESSAGE_PRIORITY_NORMAL);
	//
	// ReconMessageAPI.postNotification(getActivity(),
	// "com.reconinstruments.stats", "Stats", 0, "all_time_speed",
	// "All time speed", 0, null, null, null, "View", null, false, "",
	// MessagePriority.MESSAGE_PRIORITY_NORMAL);
	// ReconMessageAPI.postNotification(getActivity(),
	// "com.reconinstruments.stats", "Stats", 0, "all_time_distance",
	// "All time Distance", 0, null, null, null, "View", null, false,
	// "", MessagePriority.MESSAGE_PRIORITY_NORMAL);
	//
	// ReconMessageAPI.postNotification(getActivity(),
	// "com.reconinstruments.texts", "TEXTS", 0, "Frank",
	// "All time Jimmies", 0, null, null, null, "View", null, false,
	// "THIS IS A MESSAGE", MessagePriority.MESSAGE_PRIORITY_NORMAL);
	// }

	// public void clearAll() {
	// MessageHelper.clearAll(getActivity());
	// }

	// public void testPopup() {
	// // Intent reply = new Intent("RECON_REPLY_SMS");
	//
	// // ReconMessageAPI.postNotification(this, "com.test5", "Group 5", 0,
	// // "testcat6", "Category 6", 0, "RECON_REPLY_SMS", null, null, "View",
	// // null, false, "message6",
	// // MessagePriority.MESSAGE_PRIORITY_IMPORTANT,"");
	// }

	@Override
	public void onResume() {
		super.onResume();
		// check if the time zone has changed and reload the views if it has
		if (!lastTZ.equals(TimeZone.getDefault())) {
			Log.d(TAG, "timezone changed!");
			lastTZ = TimeZone.getDefault();
			mAdapter.notifyDataSetChanged();
		}
	}

}
