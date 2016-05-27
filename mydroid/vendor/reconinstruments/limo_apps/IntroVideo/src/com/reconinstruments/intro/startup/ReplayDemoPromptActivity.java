package com.reconinstruments.intro.startup;

import java.util.ArrayList;

import com.reconinstruments.intro.R;
import com.reconinstruments.intro.SettingAdapter;
import com.reconinstruments.intro.SettingItem;
import com.reconinstruments.intro.R.id;
import com.reconinstruments.intro.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ReplayDemoPromptActivity extends Activity {

	private Handler mHandler = new Handler();
	private boolean fiveSecDelayStarted = false;
	
	public static final String SETTING_KEY_IN_STORE_DEMO = "IN_STORE_DEMO";
	public static final String SETTING_KEY_PLAY_INTRO_ON_START = "INTRO_VIDEO_ALWAYS_PLAY";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) findViewById(R.id.setting_desc_text);
		desc.setPadding(0, 0, 0, 15);
		desc.setText("Let's get started with" + "\n" + "the HUD experience.");
		
		ArrayList<SettingItem> listItems = new ArrayList<SettingItem>();

		listItems.add(new SettingItem(new Intent(this, com.reconinstruments.intro.startup.TutorialActivity.class), "Begin" ));
		listItems.add(new SettingItem(new Intent(this, com.reconinstruments.intro.startup.OakleyDemoActivity.class), "Replay Demo" ));
		
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(new SettingAdapter(this, 0, listItems));
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				if(position == 0)
					startActivity(new Intent(getApplicationContext(), com.reconinstruments.intro.startup.TutorialActivity.class));
				else
					startActivity(new Intent(getApplicationContext(), com.reconinstruments.intro.startup.OakleyDemoActivity.class));
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
			
		});
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(!fiveSecDelayStarted 
				&& keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			
			mHandler.postDelayed(new Runnable() {
				public void run() {
					// Turn on in store demo mode
					Settings.System.putInt(getContentResolver(), SETTING_KEY_IN_STORE_DEMO, 1);
					Settings.System.putInt(getContentResolver(), SETTING_KEY_PLAY_INTRO_ON_START, 1);
					fiveSecDelayStarted = false;
					startService(new Intent("IN_STORE_DEMO_SERVICE"));
				}}, 5000);
			fiveSecDelayStarted = true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			mHandler.removeCallbacks(null);
			fiveSecDelayStarted = false;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	public void onBackPressed() {
		return;
	}
}
