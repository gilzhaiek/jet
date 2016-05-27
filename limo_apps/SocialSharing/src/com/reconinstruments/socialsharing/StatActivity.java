package com.reconinstruments.socialsharing;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.hudservice.helper.BTPropertyReader;
import com.reconinstruments.socialsharing.ShowMessageActivity.MessageViewData;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.messagecenter.MessageDBSchema.CatSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;
import com.reconinstruments.utils.MessageCenterUtils;

public class StatActivity extends Activity {
	
	private static final String TAG = "StatActivity";
	public static final String CATEGORY_SPEED = "all_time_speed";
	public static final String CATEGORY_VERTICAL = "all_time_vert";
	public static final String CATEGORY_DISTANCE = "all_time_dist";
	public static final String CATEGORY_ALTITUDE = "all_time_alt";
	public static final String CATEGORY_AIR = "all_time_air";
	
	private String category;
	
	private String title;
	private String valueAndUnit;
	private int stat;
	private float airStat;
    private long when = (long)System.currentTimeMillis()/1000;

	private int mIndex = 0;           // current nav index
	
	private int category_id;
	private int msg_id;

	MessageViewData[] messages;

    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stat);
		
		Intent intent = getIntent();
		category = intent.getStringExtra("category");
		Log.d(TAG, "category = " + category);
		
		TextView titleTV = (TextView) findViewById(R.id.title);
		TextView timeTV = (TextView) findViewById(R.id.sub_title);
		TextView titleInContentTV = (TextView) findViewById(R.id.content_1_text_2);
		TextView valueTV = (TextView) findViewById(R.id.content_2_text_1);
		TextView unitTV = (TextView) findViewById(R.id.content_2_text_2);
		TextView lastOrTotalTV = (TextView) findViewById(R.id.content_2_text_4);
		TextView previousBestTV = (TextView) findViewById(R.id.content_3_text_2);
		LinearLayout Content3TV = (LinearLayout) findViewById(R.id.content_3);

		int category_id = intent.getIntExtra("category_id",0);
		int msg_id = intent.getIntExtra("message_id",0);
		
		ContentResolver contentResolver = getContentResolver();
		String msgSelect = MsgSchema.COL_CATEGORY_ID+" = "+category_id;
		Cursor cursor = contentResolver.query(ReconMessageAPI.MESSAGES_VIEW_URI, ALL_FIELDS, msgSelect, null, null);
				
		messages = new MessageViewData[cursor.getCount()];
		
		for(int i=0;i<messages.length;i++){	
			cursor.moveToNext();
			messages[i] = new MessageViewData(cursor);
			Log.d(TAG, "msg: "+messages[i].id+" processed: "+messages[i].processed);
			
			if(msg_id==messages[i].id)//if there is a msg_id and it matches this one, view this one
				mIndex = i;
		}
		// close the cursor.
		cursor.close();
		
		if(mIndex==0)
			mIndex = messages.length-1; //default view the last message

		if(messages[mIndex].date.getTime() > 0){
			when = (long)messages[mIndex].date.getTime()/1000;
		}
		
		if(CATEGORY_VERTICAL.equals(category)){
			titleTV.setText("VERTICAL MILESTONE");
			titleInContentTV.setText("Vertical Milestone");
			
			Bundle b = (Bundle) intent.getExtras().getParcelable("StatsBundle");
			Float allTimeVert = b.getFloat("AllTimeVert");
			int allTimeTotalRunNumberOfAllTimeVert = b.getInt("mAllTimeTotalRunNumberOfAllTimeVert");

			stat = Math.round(allTimeVert);
			String unit = "m";
			if(SettingsUtil.getUnits(this) == 1){
				allTimeVert =(float)ConversionUtil.metersToFeet((double)allTimeVert);
				unit = "ft";
			}

			timeTV.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messages[mIndex].date));
			valueTV.setText(String.valueOf(Math.round(allTimeVert)));
			unitTV.setText(unit);
			lastOrTotalTV.setText("in " + allTimeTotalRunNumberOfAllTimeVert + " Runs");
			Content3TV.setVisibility(View.GONE);
			
			title = "VERTICAL MILESTONE";
			valueAndUnit = Math.round(allTimeVert) + unit;
		}else if(CATEGORY_DISTANCE.equals(category)){
			titleTV.setText("DISTANCE MILESTONE");
			titleInContentTV.setText("Distance Milestone");
			
			Bundle b = (Bundle) intent.getExtras().getParcelable("StatsBundle");
			Float allTimeDistance = b.getFloat("AllTimeDistance");
			int allTimeTotalRunNumberOfAllTimeDistance = b.getInt("AllTimeTotalRunNumberOfAllTimeDistance");
			
			stat = Math.round(allTimeDistance);
			String unit = "m";
			if(SettingsUtil.getUnits(this) == 1){
				allTimeDistance =(float)ConversionUtil.metersToMiles((double)allTimeDistance);
				unit = "mi";
			}
			
			timeTV.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messages[mIndex].date));
			valueTV.setText(String.valueOf(Math.round(allTimeDistance)));
			unitTV.setText(unit);
			lastOrTotalTV.setText("in " + allTimeTotalRunNumberOfAllTimeDistance + " Runs");
			Content3TV.setVisibility(View.GONE);
			
			title = "DISTANCE MILESTONE";
			valueAndUnit = Math.round(allTimeDistance) + unit;
		}else if(CATEGORY_ALTITUDE.equals(category)){
			titleTV.setText("MAX ALTITUDE MILESTONE");
			titleInContentTV.setText("Max Altitude Milestone");
			
			Bundle b = (Bundle) intent.getExtras().getParcelable("StatsBundle");
			Float allTimeMaxAlt = b.getFloat("AllTimeMaxAlt");
			Float previousMaxAlt = b.getFloat("PreviousMaxAlt");
			int runNumberOfAllTimeMaxAlt = b.getInt("RunNumberOfAllTimeMaxAlt");
			
			stat = Math.round(allTimeMaxAlt);
			String unit = "m";
			if(SettingsUtil.getUnits(this) == 1){
				allTimeMaxAlt =(float)ConversionUtil.metersToFeet((double)allTimeMaxAlt);
				unit = "ft";
			}
			timeTV.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messages[mIndex].date));
			valueTV.setText(String.valueOf(Math.round(allTimeMaxAlt)));
			unitTV.setText(unit);
			if(runNumberOfAllTimeMaxAlt > 0){
				lastOrTotalTV.setText("on Run " + runNumberOfAllTimeMaxAlt);
			}else{
				lastOrTotalTV.setVisibility(View.GONE);
			}
			
			if(Math.round(allTimeMaxAlt) > 0){
				previousBestTV.setText(String.valueOf(Math.round(allTimeMaxAlt)) + " " + unit);
			}else{
				Content3TV.setVisibility(View.GONE);
			}
			
			title = "MAX ALTITUDE MILESTONE";
			valueAndUnit = Math.round(allTimeMaxAlt) + unit;
		}else if(CATEGORY_AIR.equals(category)){
			titleTV.setText("ALL TIME BEST AIR");
			titleInContentTV.setText("All Time Best Air");
			
			Bundle b = (Bundle) intent.getExtras().getParcelable("StatsBundle");
			
			timeTV.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messages[mIndex].date));

			airStat = ((float)((int)((b.getInt("Air") + 50)/100)*1))/10;
			String value = airStat+"";
			String unit = "sec";
			valueTV.setText(value);
			unitTV.setText(unit);
			lastOrTotalTV.setVisibility(View.GONE);
			Content3TV.setVisibility(View.GONE);
			//lastOrTotalTV.setText("on Run 3");
			//previousBestTV.setText("1.8 sec");
			
			title = "ALL TIME BEST AIR";
			valueAndUnit = value + unit;
		}else if(CATEGORY_SPEED.equals(category)){
			titleTV.setText("ALL TIME MAX SPEED");
			titleInContentTV.setText("All Time Max Speed");
			
			Bundle b = (Bundle) intent.getExtras().getParcelable("StatsBundle");
			Float allTimeMaxSpeed = b.getFloat("AllTimeMaxSpeed");
			Float previousAllTimeMaxSpeed = b.getFloat("PreviousAllTimeMaxSpeed");
			int runNumberOfAllTimeMaxSpeed = b.getInt("RunNumberOfAllTimeMaxSpeed");

			stat = Math.round(allTimeMaxSpeed);

			String unit = "km/h";
			if(SettingsUtil.getUnits(this) == 1){
				allTimeMaxSpeed =(float)ConversionUtil.kmsToMiles((double)allTimeMaxSpeed);
				previousAllTimeMaxSpeed =(float)ConversionUtil.kmsToMiles((double)previousAllTimeMaxSpeed);
				unit = "mph";
			}
			
			timeTV.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messages[mIndex].date));
			valueTV.setText(String.valueOf(Math.round(allTimeMaxSpeed)));
			unitTV.setText(unit);
			if(runNumberOfAllTimeMaxSpeed > 0){
				lastOrTotalTV.setText("on Run " + runNumberOfAllTimeMaxSpeed);
			}else{
				lastOrTotalTV.setVisibility(View.GONE);
			}
			
			if(Math.round(previousAllTimeMaxSpeed) > 0){
				previousBestTV.setText(String.valueOf(Math.round(previousAllTimeMaxSpeed)) + " " + unit);
			}else{
				Content3TV.setVisibility(View.GONE);
			}
			
			title = "ALL TIME MAX SPEED";
			valueAndUnit = Math.round(allTimeMaxSpeed) + unit;
		}

	}

    @Override
    public void onResume() {
	super.onResume();
	int categoryId = getIntent().getIntExtra("category_id",-1);
	if (categoryId != -1) {
	    ReconMessageAPI.markAllMessagesInCategoryAsRead(this, categoryId);
	}
    }
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				startActivity((new Intent("RECON_STATS_ALLTIME")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if(BTPropertyReader.getBTConnectionState(this) != 2){
					startActivity(new Intent(this,ConnectPhoneActivity.class));
				}else{
					int socialConnected = 0;
					try {
						socialConnected = Settings.System.getInt(getContentResolver(), "isFacebookConnected");
					} catch (SettingNotFoundException e) {
						e.printStackTrace();
					}
					if(socialConnected != 0){
						Intent intent = new Intent(this,SharingActivity.class);
						intent.putExtra("category", category);
						intent.putExtra("title", title);
						intent.putExtra("valueAndUnit", valueAndUnit);
						if(CATEGORY_AIR.equals(category)){
							intent.putExtra("airStat", airStat);
						}else{
							intent.putExtra("stat", stat);
						}
						intent.putExtra("when", when);
						startActivity(intent);
						finish();
					}else{
						startActivity(new Intent(this,ConnectFacebookActivity.class));
					}

				}
				return true;
			default:
				return super.onKeyUp(keyCode, event);
		}
	}
	
	public static final String[] ALL_FIELDS = new String[] {
		MsgSchema._ID,
		MsgSchema.COL_TIMESTAMP,
		MsgSchema.COL_TEXT,
		MsgSchema.COL_PROCESSED,
		CatSchema.COL_DESCRIPTION,
		CatSchema.COL_PRESS_INTENT,
		CatSchema.COL_PRESS_CAPTION,
		MsgSchema.COL_CATEGORY_ID
	};
	public static class MessageViewData{
		MessageViewData(Cursor cursor){
			id = cursor.getInt(0);
			date = new Date(cursor.getLong(1));
			text = cursor.getString(2);
			processed = cursor.getInt(3)==1;
			catDesc = cursor.getString(4);
			pressIntent = MessageCenterUtils.BytesToIntent(cursor.getBlob(5));
			pressCaption = cursor.getString(6);
			catId = cursor.getInt(7);
		}
		int id;
		Date date;
		String text;
		boolean processed;
		String catDesc;
		Intent pressIntent;
		String pressCaption;
		int catId;
	}
	
}
