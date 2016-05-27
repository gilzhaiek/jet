package com.reconinstruments.jetapplauncher.settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.hudservice.helper.BTPropertyReader;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.hud_phone_status_exchange.TimesyncRequestMessage;
import com.reconinstruments.hud_phone_status_exchange.TimesyncResponseMessage;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;

public class DateTimeActivity extends ListActivity{
	private static String TAG = "DateTimeActivity";
	
	private static final String INTENT_REQUEST_TIME = "request_time";

//	private static final int POS_TIMEZONE	= 0;
//	private static final int POS_24HOUR		= 1;
//	private static final int POS_AUTO		= 2;
//	private static final int POS_MANUAL		= 3;
	private static final int POS_TIMEZONE	= 2;
	private static final int POS_24HOUR		= 1;
	private static final int POS_AUTO		= 0;
	private static final int POS_MANUAL		= 3;

	private ArrayList<SettingItem> timeDateList;
	private SettingAdapter dateTimeAdapter;

	private boolean mIs24;
	private boolean mIsAuto;
	
	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.setting_layout);

		ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
		headerIcon.setBackgroundResource(R.drawable.time_white);
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("SET TIME");

		timeDateList = new ArrayList<SettingItem>();
		SettingItem item;

		/*
		 * Automatic Time Initialization
		 */
		//item = new SettingItem("Set Manually");
		item = new SettingItem("Sync Time With Smartphone");
		item.checkBox = true;

		if (SettingsUtil.getSyncTimeWithSmartPhone(getBaseContext())  && BTPropertyReader.getBTConnectionState(DateTimeActivity.this) == 2 ) {
			item.checkBoxValue = true;
		} else {
			item.checkBoxValue = false;
		}

		timeDateList.add(item);

		/*
		 * 24 Hour Format Initialization
		 */
		mIs24 = DateFormat.is24HourFormat(this);

		if (mIs24)
			item = new SettingItem("Use 12 hour format");
		else
			item = new SettingItem("Use 24 hour format");

		timeDateList.add(item);

		/*
		 * TimeZone Initialization
		 */
		item = new SettingItem("Select Time Zone");
		item.intent = new Intent(this, TimeZoneActivity.class);
		if (timeDateList.get(0).checkBoxValue && BTPropertyReader.getBTConnectionState(DateTimeActivity.this) == 2)
			item.titleAlpha = 100;
		timeDateList.add(item);

		/*
		 * Manual Time Initialization
		 */
		//item = new SettingItem("Set Time");
		item = new SettingItem("Set Time Manually");
		item.intent = new Intent(DateTimeActivity.this, ManualTimeActivity.class);
		//if (!timeDateList.get(2).checkBoxValue)
		if (timeDateList.get(0).checkBoxValue && BTPropertyReader.getBTConnectionState(DateTimeActivity.this) == 2)
			item.titleAlpha = 100;

		timeDateList.add(item);

		if (timeDateList.get(0).checkBoxValue && BTPropertyReader.getBTConnectionState(DateTimeActivity.this) == 2){
			
			((SettingItem)timeDateList.get(2)).phoneIcon = true;
			((SettingItem)timeDateList.get(3)).phoneIcon = true;
		}else{
			((SettingItem)timeDateList.get(2)).phoneIcon = false;
			((SettingItem)timeDateList.get(3)).phoneIcon = false;
		}
		
		dateTimeAdapter = new SettingAdapter(this, 0, timeDateList); 

		setListAdapter(dateTimeAdapter);

		this.getListView().setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				Log.v(TAG, "position : " + position);
				switch (position) {
				case POS_TIMEZONE:
					if (!timeDateList.get(0).checkBoxValue || BTPropertyReader.getBTConnectionState(DateTimeActivity.this) != 2)
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
				    if(BTPropertyReader.getBTConnectionState(DateTimeActivity.this) == 2){
	                    if (!SettingsUtil.getSyncTimeWithSmartPhone(getBaseContext())) { // gps local time is on, means syncing time with smartphone
                            SettingsUtil.setSyncTimeWithSmartPhone(getBaseContext(), true);
	                        timeDateList.get(POS_AUTO).checkBoxValue = true;
	                        dateTimeAdapter.notifyDataSetChanged();
	                        
	                        HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
	                        TimesyncRequestMessage msg = new TimesyncRequestMessage();
	                        cMsg.setSender(DateTimeActivity.class.getCanonicalName());
	                        cMsg.setIntentFilter(TimesyncRequestMessage.INTENT);
	                        cMsg.setData(msg.serialize().getBytes());
	                        HUDConnectivityHelper.getInstance(DateTimeActivity.this).push(cMsg, HUDConnectivityService.Channel.OBJECT_CHANNEL);
	                        
	                        progressDialog = new ProgressDialog(DateTimeActivity.this);
	                        progressDialog.setIndeterminate(true);
	                        progressDialog.setCancelable(false);
	                        progressDialog.show();
	                        progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
	                        TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
	                        textTv.setText("Syncing time...");
	                        new CountDownTimer(5 * 1000, 1000) {
	                            public void onTick(long millisUntilFinished) {
	                            }
	                            public void onFinish() {
	                                try{
	                                    if (progressDialog != null && progressDialog.isShowing()) {
	                                        progressDialog.dismiss();
	                                        failToSyncTime();
	                                    }
	                                }catch(IllegalArgumentException e){
	                                    //do nothing since the activity has been finished.
	                                }
	                            }
	                        }.start();
	                    } else {
                            SettingsUtil.setSyncTimeWithSmartPhone(getBaseContext(), false);
	                        timeDateList.get(POS_AUTO).checkBoxValue = false;
	                        dateTimeAdapter.notifyDataSetChanged();
	                        //timeDateList.get(POS_MANUAL).titleAlpha = 100;
	                        timeDateList.get(POS_MANUAL).titleAlpha = 255;
	                        timeDateList.get(POS_TIMEZONE).titleAlpha = 255;
	                        ((SettingItem)timeDateList.get(2)).phoneIcon = false;
	                        ((SettingItem)timeDateList.get(3)).phoneIcon = false;
	                    }
				    }
					break;
				case POS_MANUAL:

					//if (timeDateList.get(2).checkBoxValue)
					if (!timeDateList.get(0).checkBoxValue || BTPropertyReader.getBTConnectionState(DateTimeActivity.this) != 2)
						startActivity(timeDateList.get(position).intent);				

					break;
				}

			}

		});
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(TimesyncResponseMessage.INTENT);
		filter.addAction("HUD_STATE_CHANGED");
		registerReceiver(syncTimeUIUpdateBroadcastReceiver, filter);

	}
	
	@Override
	protected void onDestroy() {
		try{
		    Log.d(TAG, "unregisterReceiver hudServiceBroadcastReceiver");
		    unregisterReceiver(syncTimeUIUpdateBroadcastReceiver);
		}catch(IllegalArgumentException e){
		    //ignore
		}
		super.onDestroy();
	}
	private void failToSyncTime(){
		timeDateList.get(POS_AUTO).checkBoxValue = false;
		dateTimeAdapter.notifyDataSetChanged();
		//timeDateList.get(POS_MANUAL).titleAlpha = 100;
		timeDateList.get(POS_MANUAL).titleAlpha = 255;
		timeDateList.get(POS_TIMEZONE).titleAlpha = 255;
		((SettingItem)timeDateList.get(2)).phoneIcon = false;
		((SettingItem)timeDateList.get(3)).phoneIcon = false;
		(new ReconToast(DateTimeActivity.this, com.reconinstruments.commonwidgets.R.drawable.error_icon, "Failed to sync time")).show();
	}
	
	private BroadcastReceiver syncTimeUIUpdateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		    if(intent.getAction().equals("HUD_STATE_CHANGED")){
	            Bundle bundle = intent.getExtras();
	            int b = bundle.getInt("state"); // connectionstate
	            if(b == 0){ //disconnected
	                timeDateList.get(POS_AUTO).checkBoxValue = false;
	                dateTimeAdapter.notifyDataSetChanged();
	                //timeDateList.get(POS_MANUAL).titleAlpha = 100;
	                timeDateList.get(POS_MANUAL).titleAlpha = 255;
	                timeDateList.get(POS_TIMEZONE).titleAlpha = 255;
	                ((SettingItem)timeDateList.get(2)).phoneIcon = false;
	                ((SettingItem)timeDateList.get(3)).phoneIcon = false;
	            }
		    }else if(intent.getAction().equals(TimesyncResponseMessage.INTENT)){
				if (!intent.hasExtra("message")) {
					if (progressDialog != null && progressDialog.isShowing()) {
						progressDialog.dismiss();
					}
					failToSyncTime();
				}else{
					String xml = new String(new HUDConnectivityMessage(intent.getByteArrayExtra("message")).getData());
					if (progressDialog != null && progressDialog.isShowing()) {
						progressDialog.dismiss();
					}
					try{
						TimesyncResponseMessage msg = new TimesyncResponseMessage(xml);
						int phoneOffset = msg.getUtcOffset();
						long phoneTime = msg.getUtcTime();
						if(SettingsUtil.getSyncTimeWithSmartPhone(context)){
                            SettingsUtil.setTimeAuto(getBaseContext(), false); // turn off auto GPS
						    timeDateList.get(POS_AUTO).checkBoxValue = true;
							timeDateList.get(POS_MANUAL).titleAlpha = 100;
							timeDateList.get(POS_TIMEZONE).titleAlpha = 100;
							((SettingItem)timeDateList.get(2)).phoneIcon = true;
							((SettingItem)timeDateList.get(3)).phoneIcon = true;
							dateTimeAdapter.notifyDataSetChanged();
							(new ReconToast(DateTimeActivity.this, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, "Time Synced")).show();

							AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
							alarm.setTime(phoneTime);
							if(TimeZone.getDefault().getRawOffset() != phoneOffset){
								String[] tzStrs = TimeZone.getAvailableIDs(phoneOffset);
								if(tzStrs.length > 0){
									alarm.setTimeZone(tzStrs[0]);
								}
							}
							Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
							sendBroadcast(timeChanged);
						}else{
							failToSyncTime();
					}
					}catch(NumberFormatException e){
						failToSyncTime();
					}
				}
			}
		}		
	};

}
