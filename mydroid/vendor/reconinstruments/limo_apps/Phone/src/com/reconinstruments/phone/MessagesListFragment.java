package com.reconinstruments.phone;

import java.util.Date;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
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

import com.reconinstruments.phone.dialogs.IncomingSMSActivity;
//import com.reconinstruments.phone.BLEConnectionManager;
import com.reconinstruments.utils.TimeUtils;

public class MessagesListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private ListView lv;
	SimpleCursorAdapter cursorAdapter;
	private BLEServiceConnectionManager mBLEServiceConnectionManager = null;

	private static final int SMS_LIST_LOADER = 0x01;


	public MessagesListFragment() {
		super();
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.list_layout, null);	

		Log.d("MessagesListFragment", "onCreateView");
		TextView emptyTV = (TextView) v.findViewById(android.R.id.empty);

		emptyTV.setText("No Messages");

		Context context = this.getActivity().getApplicationContext();


		cursorAdapter = new SimpleCursorAdapter(
				context ,
				R.layout.message_list_item,
				null,
				new String[] { PhoneLogProvider.KEY_INCOMING,PhoneLogProvider.KEY_CONTACT, PhoneLogProvider.KEY_BODY,PhoneLogProvider.KEY_DATE,PhoneLogProvider.KEY_SOURCE, },
				new int[] {R.id.image, R.id.from, R.id.body, R.id.time },
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER );

		cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

				int viewId = view.getId();
				switch(viewId) {
				case R.id.image:
					boolean incoming = cursor.getInt(columnIndex)==1;
					int drawable = incoming?R.drawable.sms_icons_in:R.drawable.sms_icons_out;
					ImageView image = (ImageView) view;
					image.setImageResource(drawable);
					return true;
				case R.id.from:
					TextView label = (TextView) view;

					String contact = cursor.getString(columnIndex);

					/* Show contact or their number if unknown */
					if(contact == null || contact.equals("Unknown")) 
						label.setText(cursor.getString(cursor.getColumnIndex(PhoneLogProvider.KEY_SOURCE)));
					else 
						label.setText(contact);
					return true;
				case R.id.body:
					TextView body = (TextView) view;
					String text = cursor.getString(columnIndex);
					body.setText(text);
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

		getLoaderManager().initLoader(SMS_LIST_LOADER, null, this);

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Cursor c = ((SimpleCursorAdapter)l.getAdapter()).getCursor();

		//String contact = c.getString(c.getColumnIndex(PhoneLogProvider.KEY_CONTACT));
		//String source = c.getString(c.getColumnIndex(PhoneLogProvider.KEY_SOURCE));

		Intent intent = new Intent(getActivity(), IncomingSMSActivity.class);

		//intent.putExtra("contact", contact);
		//intent.putExtra("source", source);
		// We launch the activity without the reply button if in iOS Mode for now
		intent.putExtra("isiOS",mBLEServiceConnectionManager.isiOSMode());

		int rowId = c.getInt(c.getColumnIndex(PhoneLogProvider.KEY_ROWID));


		String smsUri = ContentUris.withAppendedId(PhoneLogProvider.CONTENT_URI, rowId).toString();
		intent.putExtra("uri", smsUri);

		startActivity(intent);	
		//finish();
	}


	// These are the Contacts rows that we will retrieve.
	static final String[] MESSAGES_PROJECTION = new String[] {
		PhoneLogProvider.KEY_ROWID,
		PhoneLogProvider.KEY_SOURCE,
		PhoneLogProvider.KEY_CONTACT,
		PhoneLogProvider.KEY_BODY,
		PhoneLogProvider.KEY_INCOMING,
		PhoneLogProvider.KEY_DATE,
	};

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		Log.d("MessageListFragment", "querying messages");
		
		Uri baseUri = PhoneLogProvider.CONTENT_URI;

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		String select = "(" + PhoneLogProvider.KEY_TYPE + "="+PhoneLogProvider.TYPE_SMS+")";

		return new CursorLoader(getActivity(), baseUri,
				MESSAGES_PROJECTION, select, null,
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
