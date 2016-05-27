package com.reconinstruments.phone;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.utils.TimeUtils;

public class CallsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private ListView lv;
	SimpleCursorAdapter cursorAdapter;
	private BLEServiceConnectionManager mBLEServiceConnectionManager = null;
	private static final int CALLS_LIST_LOADER = 0x02;

	public CallsListFragment() {
		super();
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		Log.d("CallsListFragment", "onCreateView");
		View v = inflater.inflate(R.layout.list_layout, null);	

		TextView emptyTV = (TextView) v.findViewById(android.R.id.empty);
		emptyTV.setText("No Calls");

		Context context = this.getActivity().getApplicationContext();

		cursorAdapter = new SimpleCursorAdapter(
				context ,
				R.layout.call_list_item,
				null,
				new String[] { PhoneLogProvider.KEY_CONTACT, PhoneLogProvider.KEY_MISSED, PhoneLogProvider.KEY_DATE },
				new int[] {R.id.from, R.id.image, R.id.time },
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER );

		cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

				int viewId = view.getId();
				switch(viewId) {
				case R.id.from:
					TextView label = (TextView) view;

					String contact = cursor.getString(columnIndex);

					/* Show contact or their number if unknown */
					if(contact == null || contact.equals("Unknown")) 
						label.setText(cursor.getString(COL_SOURCE));
					else 
						label.setText(contact);
					return true;
				case R.id.image:
					ImageView image = (ImageView) view;
					boolean missed = cursor.getInt(columnIndex)==1;
					boolean incoming = cursor.getInt(cursor.getColumnIndex(PhoneLogProvider.KEY_INCOMING))==1;

					if(incoming){
						if(missed)
							image.setImageResource(R.drawable.telephone_calls_in_red);
						else 
							image.setImageResource(R.drawable.telephone_calls_in_grey);
					}
					else
						image.setImageResource(R.drawable.telephone_calls_out);

					return true;
				case R.id.time:
					TextView info = (TextView) view;
					long time = Long.parseLong(cursor.getString(columnIndex));
					info.setText(TimeUtils.getTimeString(new Date(time)));
					return true;
				}
				return false;
			}
		});

		return v;
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		lv = this.getListView();
		lv.setSelector(R.drawable.key_selector);
		lv.setAdapter(cursorAdapter);

		getLoaderManager().initLoader(CALLS_LIST_LOADER, null, this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);	

		if (!mBLEServiceConnectionManager.isiOSMode()) {
			//Only attempt call if not iOS mode
			//TODO: need extra condition to not call if no BT connection
			startCall();
		}
	}
	public void startCall(){
		Cursor c = cursorAdapter.getCursor();
		String source = c.getString(c.getColumnIndex(PhoneLogProvider.KEY_SOURCE));
		String contact = c.getString(c.getColumnIndex(PhoneLogProvider.KEY_CONTACT));

		PhoneUtils.startCall(source, contact, getActivity());
	}
	// These are the Contacts rows that we will retrieve.
	static final String[] CALLS_PROJECTION = new String[] {
		PhoneLogProvider.KEY_ROWID,
		PhoneLogProvider.KEY_CONTACT,
		PhoneLogProvider.KEY_SOURCE,
		PhoneLogProvider.KEY_MISSED,
		PhoneLogProvider.KEY_INCOMING,
		PhoneLogProvider.KEY_DATE,
	};
	static final int COL_CONTACT = 1;
	static final int COL_SOURCE = 2;
	static final int COL_REPLIED = 4;

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		Log.d("CallsListFragment", "onCreateLoader");
		Uri baseUri = PhoneLogProvider.CONTENT_URI;

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		String select = "(" + PhoneLogProvider.KEY_TYPE + "="+PhoneLogProvider.TYPE_CALL+")";

		return new CursorLoader(getActivity(), baseUri,
				CALLS_PROJECTION, select, null,
				PhoneLogProvider.KEY_DATE + " DESC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		cursorAdapter.swapCursor(data);

		lv.requestFocus();
	}

	public void onLoaderReset(Loader<Cursor> arg0) {
		cursorAdapter.swapCursor(null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBLEServiceConnectionManager = new BLEServiceConnectionManager(this.getActivity().getApplicationContext());

	}

	@Override
	public void onStart() {
		super.onStart();
		mBLEServiceConnectionManager.initService();
	}
	@Override
	public void onStop() {
		mBLEServiceConnectionManager.releaseService();
		super.onStop();
	}
}
