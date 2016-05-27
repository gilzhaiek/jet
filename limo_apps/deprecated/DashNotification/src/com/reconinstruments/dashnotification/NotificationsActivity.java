package com.reconinstruments.dashnotification;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


import org.apache.commons.lang3.StringUtils;

import com.reconinstruments.dashelement1.ColumnElementActivity;
import com.reconinstruments.dashlauncher.notifications.ConversionUtil;
import com.reconinstruments.dashlauncher.notifications.NotificationsDatabase;
import com.reconinstruments.dashlauncher.notifications.NotificationsProvider;
import com.reconinstruments.dashlauncher.notifications.ReconSettingsUtil;
import com.reconinstruments.dashnotification.R;
import com.reconinstruments.dashnotification.R.id;
import com.reconinstruments.dashnotification.R.layout;


public class NotificationsActivity extends ColumnElementActivity {
	
	public static final String TAG = "NotificationsActivity";
	
	private ListView listView;
	private CursorAdapter notificationsAdapter;
	private boolean isMetric = true;
	private DecimalFormat df = new DecimalFormat();
	private ContentResolver mContentResolver;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.notifications_list_layout);		
		listView = (ListView) findViewById(android.R.id.list);
		
		mContentResolver = getContentResolver();
		Cursor notifications = this.managedQuery(NotificationsProvider.NOTIFICATION_URI, DB_FIELDS, null, null, null);
		notificationsAdapter = new NotificationsAdapter(this, notifications);
		
		listView.setAdapter(notificationsAdapter);
		listView.setOnItemClickListener(notificationClickListener);
		
		View emptyView = findViewById(android.R.id.empty);
		if (emptyView != null)
        	listView.setEmptyView(emptyView);
	}
	
	public void onResume() {
		super.onResume();
		isMetric = ReconSettingsUtil.getUnits(this) == ReconSettingsUtil.RECON_UINTS_METRIC;
		
		// force refresh list
		notificationsAdapter.notifyDataSetChanged();
	}
	
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			goLeft();
	        return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			goRight();
	        return true;
	    }
	    return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		goBack();
	}

	class NotificationsAdapter extends CursorAdapter {
		private Cursor mCursor;
		private Context mContext;
		private final LayoutInflater mInflater;
		
		public NotificationsAdapter(Context context, Cursor c) {
			super(context, c);
			mInflater = LayoutInflater.from(context);
			mCursor = c;
			//Log.v(TAG, "Notifications: " + c.getCount());
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ImageView icon = (ImageView) view.findViewById(R.id.notifications_row_icon);
			TextView timeTV = (TextView) view.findViewById(R.id.notifications_row_time);
			TextView titleTV = (TextView) view.findViewById(R.id.notifications_row_label);
			TextView messageTV = (TextView) view.findViewById(R.id.notifications_row_info);
			
			// Get notifications bundle
			byte[] notificationDataBlob = cursor.getBlob(cursor.getColumnIndex(NotificationsDatabase.KEY_DATA));
	    	final Parcel p = Parcel.obtain();
	    	p.unmarshall(notificationDataBlob, 0, notificationDataBlob.length);
	    	p.setDataPosition(0);
	    	Bundle notificationBundle = p.readBundle();
	    	
	    	// Fill out view
	    	icon.setImageBitmap((Bitmap) notificationBundle.getParcelable("icon"));
	    	
	    	Long datetime = cursor.getLong(cursor.getColumnIndex(NotificationsDatabase.KEY_DATE));
	    	//Log.v(TAG, "datetime: " + datetime);
	    	Calendar c = Calendar.getInstance();
	    	c.setTimeZone(TimeZone.getDefault());
	    	c.setTimeInMillis(datetime);

	    	if(DateFormat.is24HourFormat(NotificationsActivity.this)) {
	    		timeTV.setText(String.format("%1$tk:%1$tM", c));
	    	} else {
	    		timeTV.setText(String.format("%1$tl:%1$tM %1$Tp", c));
	    	}
	    	
	    	titleTV.setText(notificationBundle.getString("title"));
	    	
	    	// Message
	    	messageTV.setText(parseNotificationMessage(notificationBundle.getString("message"), isMetric));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = mInflater.inflate(R.layout.notifications_list_item, parent,false); 
	        return view;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
		    if (!mCursor.moveToPosition(position)) {
		        throw new IllegalStateException("couldn't move cursor to position " + position);
		    }
		    
		    View v;
		    
		    if (convertView == null) {
		        v = newView(mContext, mCursor, parent);
		    } else {
		        v = convertView;
		    }
		 
		    bindView(v, mContext, mCursor);
		    return v;
		}
	}
	
	OnItemClickListener notificationClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> listView, View view, int position,long id)
		{
			Cursor cursor = notificationsAdapter.getCursor();
			
			if(!cursor.moveToPosition(position)) return;
			
			byte[] notificationDataBlob = cursor.getBlob(cursor.getColumnIndex(NotificationsDatabase.KEY_DATA));
	    	final Parcel p = Parcel.obtain();
	    	p.unmarshall(notificationDataBlob, 0, notificationDataBlob.length);
	    	p.setDataPosition(0);
	    	Bundle notificationBundle = p.readBundle();
	    	
	    	Class<?> intentClass = (Class<?>) notificationBundle.getSerializable("class");
	    	String intentString = notificationBundle.getString("intentString");
	    	Bundle intentExtras = (Bundle) notificationBundle.getParcelable("extrasBundle");
	    	
	    	Intent mIntent = new Intent();
	    	if(intentClass != null) mIntent = new Intent(NotificationsActivity.this, intentClass);
	    	if(intentString != null) mIntent= new Intent(intentString);
	    	if(intentExtras != null) mIntent.putExtras(intentExtras);
	    	
	    	startActivity(mIntent);
		}
	};
	
	public static String parseNotificationMessage(String rawMessage, boolean metric) {
		
		Pattern pattern;
		Matcher matcher;
		
		// Parse km/h and mph
		pattern = Pattern.compile("<\\-?[0-9\\.]*\\|km/h>");
		matcher = pattern.matcher(rawMessage);

		DecimalFormat mDecFomat = new DecimalFormat();
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"km/h";
			} else {
				float speed = (float) ConversionUtil.kmsToMiles(Float.parseFloat(values[0]));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(speed)+"mph";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		// Parse m and ft
		pattern = Pattern.compile("<\\-?[0-9\\.]*\\|m>");
		matcher = pattern.matcher(rawMessage);
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"m";
			} else {
				float length = (float) ConversionUtil.metersToFeet((Float.parseFloat(values[0])));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(length)+"ft";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		// parse celcius and fahrenheit (sp?)
		pattern = Pattern.compile("<\\-?[0-9\\.]*\\|c>");
		matcher = pattern.matcher(rawMessage);
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"C";
			} else {
				float temp = (float) ConversionUtil.celciusToFahrenheit((int)(Float.parseFloat(values[0])));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(temp)+"F";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		// parse distance (km and mi)
		pattern = Pattern.compile("<\\-?[0-9\\.]*\\|km>");
		matcher = pattern.matcher(rawMessage);
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"km";
			} else {
				float temp = (float) ConversionUtil.kmsToMiles((Float.parseFloat(values[0])));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(temp)+"mi";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		return rawMessage;
	}
	
	private static final String[] DB_FIELDS = new String[] {
		NotificationsDatabase.KEY_ROWID,
		NotificationsDatabase.KEY_NOTIFICATION_ID,
		NotificationsDatabase.KEY_DATE,
		NotificationsDatabase.KEY_DATA,
		NotificationsDatabase.KEY_SINGLETON_PERSISTANT
	};

}
