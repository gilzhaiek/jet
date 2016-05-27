package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.settings.ReconSettingsUtil;

public class DateTimeActivity extends ListActivity{
	private static String TAG = "DateTimeActivity";

	private static final int POS_TIMEZONE	= 0;
	private static final int POS_24HOUR		= 1;
	private static final int POS_AUTO		= 2;
	private static final int POS_MANUAL		= 3;

	private ArrayList<SettingItem> timeDateList;
	private SettingAdapter dateTimeAdapter;

	private boolean mIs24;
	private boolean mIsAuto;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.setting_layout);

		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("Set Time");

		timeDateList = new ArrayList<SettingItem>();

		/*
		 * TimeZone Initialization
		 */
		timeDateList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "Select Time Zone" ));

		/*
		 * 24 Hour Format Initialization
		 */
		mIs24 = DateFormat.is24HourFormat(this);
		SettingItem item;

		if (mIs24)
			item = new SettingItem("Use 12 hour format");
		else
			item = new SettingItem("Use 24 hour format");

		timeDateList.add(item);

		/*
		 * Automatic Time Initialization
		 */
		item = new SettingItem("Set Manually");
		item.checkBox = true;

		if (!ReconSettingsUtil.getTimeAuto(getBaseContext())) {
			item.checkBoxValue = true;
		} else {
			item.checkBoxValue = false;
		}

		timeDateList.add(item);

		/*
		 * Manual Time Initialization
		 */
		item = new SettingItem("Set Time");
		item.intent = new Intent(DateTimeActivity.this, ManualTimeActivity.class);
		if (!timeDateList.get(2).checkBoxValue)
			item.titleAlpha = 100;

		timeDateList.add(item);



		dateTimeAdapter = new SettingAdapter(this, 0, timeDateList); 

		setListAdapter(dateTimeAdapter);

		this.getListView().setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				Log.v(TAG, "position : " + position);
				switch (position) {
				case POS_TIMEZONE:
					startActivity(timeDateList.get(position).intent);
					break;
				case POS_24HOUR:
					SettingItem m = (SettingItem)adapterView.getItemAtPosition(position);

					mIs24 = !(mIs24);
					if (mIs24)
						m.title = "Use 12 hour format";
					else
						m.title = "Use 24 hour format";

					Settings.System.putString(getBaseContext().getContentResolver(),
							Settings.System.TIME_12_24,
							mIs24? "24" : "12");

					dateTimeAdapter.notifyDataSetChanged();

					//broadcast the time changed intent so that status bar can update the change
					Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
					getBaseContext().sendBroadcast(timeChanged);

					break;
				case POS_AUTO:

					if (ReconSettingsUtil.getTimeAuto(getBaseContext())) {
						ReconSettingsUtil.setTimeAuto(getBaseContext(), false); // turn off auto GPS
						timeDateList.get(POS_AUTO).checkBoxValue = true;
						dateTimeAdapter.notifyDataSetChanged();
						timeDateList.get(POS_MANUAL).titleAlpha = 255;
					} else {
						ReconSettingsUtil.setTimeAuto(getBaseContext(), true); // turn on auto GPS
						timeDateList.get(POS_AUTO).checkBoxValue = false;
						dateTimeAdapter.notifyDataSetChanged();
						timeDateList.get(POS_MANUAL).titleAlpha = 100;
					}

					break;
				case POS_MANUAL:

					if (timeDateList.get(2).checkBoxValue)
						startActivity(timeDateList.get(position).intent);				

					break;
				}

			}

		});

	}

}
