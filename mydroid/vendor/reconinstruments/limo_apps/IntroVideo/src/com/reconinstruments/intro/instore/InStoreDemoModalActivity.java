package com.reconinstruments.intro.instore;

import java.util.ArrayList;

import com.reconinstruments.intro.R;
import com.reconinstruments.intro.SettingAdapter;
import com.reconinstruments.intro.SettingItem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class InStoreDemoModalActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.GONE);
		
		ArrayList<SettingItem> listItems = new ArrayList<SettingItem>();

		listItems.add(new SettingItem(null, "Continue Demo" ));
		listItems.add(new SettingItem(null, "Explore HUD Features" ));
		
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(new SettingAdapter(this, 0, listItems));
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				if(position == 0) {
					Intent returnIntent = new Intent();
					returnIntent.putExtra("resume", true);
					setResult(RESULT_OK,returnIntent);     
					finish();
				} else {
					Intent returnIntent = new Intent();
					returnIntent.putExtra("resume", false);
					setResult(RESULT_OK,returnIntent);     
					finish();
				}
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
			
		});
	}
	
	public void onBackPressed() {
		return;
	}
}
