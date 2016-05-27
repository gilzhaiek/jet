package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParserException;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;


public class TimeZoneActivity extends ListActivity {			
	private static final String TAG = "ZoneList";
	private static final String KEY_ID = "id";
	private static final String KEY_DISPLAYNAME = "name";
	private static final String KEY_GMT = "gmt";
	private static final String KEY_OFFSET = "offset";
	private static final String XMLTAG_TIMEZONE = "timezone";

	private static final int HOURS_1 = 60 * 60000;
	private static final int HOURS_24 = 24 * HOURS_1;
	private static final int HOURS_HALF = HOURS_1 / 2;

	private SettingAdapter timeZoneAdapter; 

	private ArrayList<HashMap> mTimeZones = null;

	private ArrayList<SettingItem> timeZoneLists = null;

	private Integer checkedIndex = null;

	// Initial focus position
	private int mDefault = 0;

	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		setContentView(R.layout.setting_layout);

		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("Select Time Zone");

		timeZoneLists = new ArrayList<SettingItem>( 128 );

		if( mTimeZones == null )
		{
			mTimeZones = enumerateZones( );
		}

		TimeZone tz = TimeZone.getDefault();
		Log.v(TAG, tz.getID());

		for( int i = 0; i < mTimeZones.size(); ++i )
		{
			timeZoneLists.add( new SettingItem( (String)mTimeZones.get(i).get(KEY_DISPLAYNAME)));
		}


		this.getListView().setOnItemClickListener(timeZoneClickListener);
		checkedIndex = null;
		for(SettingItem m : (ArrayList<SettingItem>) timeZoneLists) {
			if(tz.getID().equals((String)mTimeZones.get(timeZoneLists.indexOf(m)).get(KEY_ID))) {
				checkedIndex = timeZoneLists.indexOf(m);
				m.checkMark = true;
			}
		}

		timeZoneAdapter = new SettingAdapter(this, 0, timeZoneLists);

		setListAdapter(timeZoneAdapter);

		if (checkedIndex != null)
			this.getListView().setSelection(checkedIndex);

	}

	private ArrayList<HashMap> enumerateZones() {

		ArrayList<HashMap> myData = new ArrayList<HashMap>();
		long date = Calendar.getInstance().getTimeInMillis();
		try {
			XmlResourceParser xrp = getResources().getXml(R.xml.timezones);
			while (xrp.next() != XmlResourceParser.START_TAG)
				continue;
			xrp.next();
			while (xrp.getEventType() != XmlResourceParser.END_TAG) {
				while (xrp.getEventType() != XmlResourceParser.START_TAG) {
					if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
						return myData;
					}
					xrp.next();
				}
				if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
					String id = xrp.getAttributeValue(0);
					String displayName = xrp.nextText();
					addItem(myData, id, displayName, date);
				}
				while (xrp.getEventType() != XmlResourceParser.END_TAG) {
					xrp.next();
				}
				xrp.next();
			}
			xrp.close();
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Ill-formatted timezones.xml file");
		} catch (java.io.IOException ioe) {
			Log.e(TAG, "Unable to read timezones.xml file");
		}

		return myData;

	}

	protected void addItem(ArrayList<HashMap> myData, String id, String displayName, 
			long date) {
		HashMap map = new HashMap();
		map.put(KEY_ID, id);
		map.put(KEY_DISPLAYNAME, displayName);
		TimeZone tz = TimeZone.getTimeZone(id);
		int offset = tz.getOffset(date);
		int p = Math.abs(offset);
		StringBuilder name = new StringBuilder();
		name.append("GMT");

		if (offset < 0) {
			name.append('-');
		} else {
			name.append('+');
		}

		name.append(p / (HOURS_1));
		name.append(':');

		int min = p / 60000;
		min %= 60;

		if (min < 10) {
			name.append('0');
		}
		name.append(min);

		map.put(KEY_GMT, name.toString());
		map.put(KEY_OFFSET, offset);

		if (id.equals(TimeZone.getDefault().getID())) {
			mDefault = myData.size();
		}

		myData.add(map);
	}

	private static class MyComparator implements Comparator<HashMap> {
		private String mSortingKey; 

		public MyComparator(String sortingKey) {
			mSortingKey = sortingKey;
		}

		public void setSortingKey(String sortingKey) {
			mSortingKey = sortingKey;
		}

		public int compare(HashMap map1, HashMap map2) {
			Object value1 = map1.get(mSortingKey);
			Object value2 = map2.get(mSortingKey);

			/* 
			 * This should never happen, but just in-case, put non-comparable
			 * items at the end.
			 */
			if (!isComparable(value1)) {
				return isComparable(value2) ? 1 : 0;
			} else if (!isComparable(value2)) {
				return -1;
			}

			return ((Comparable) value1).compareTo(value2);
		}

		private boolean isComparable(Object value) {
			return (value != null) && (value instanceof Comparable); 
		}
	}




	/*
   protected void setMenuItemViewValue( int idx, View view  )
   {
		HashMap map = mTimeZones.get(idx);

		TextView title = (TextView)view.findViewById(R.id.menuview_item_text);
		title.setText( (String)map.get(KEY_DISPLAYNAME) );

		TextView gmt = (TextView)view.findViewById(R.id.menuview_item_desc);
		gmt.setText( (String)map.get(KEY_GMT) );

		//gmt.setTypeface( Util.getMenuFont(this.getContext()) );

   }*/

	private OnItemClickListener timeZoneClickListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

			if (checkedIndex != null)
				timeZoneLists.get(checkedIndex).checkMark=false;

			checkedIndex = position;

			HashMap map = mTimeZones.get(position);

			// Update the system timezone value
			AlarmManager alarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
			alarm.setTimeZone((String) map.get(KEY_ID));


			timeZoneLists.get(position).checkMark = true;
			
			timeZoneAdapter.notifyDataSetChanged();
			
			TimeZoneActivity.this.getListView().setSelection(position);

		}

	};

}



