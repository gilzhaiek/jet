package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;

import com.reconinstruments.dashsettings.R;


import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class HelpActivity extends ListActivity {

	private static final String TAG = "HelpActivity";
	
	private ArrayList<SettingItem> advancedList;
	private SettingAdapter advancedAdapter;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
		headerIcon.setVisibility(View.GONE);
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("HELP");
		
		advancedList = new ArrayList<SettingItem>();
				
		advancedList.add(new SettingItem(new Intent("RECON_INTRO_VIDEO"), "Play Movies" ));
		
		advancedAdapter = new SettingAdapter(this, 0, advancedList);
		
		setListAdapter(advancedAdapter);
		
		this.getListView().setOnItemClickListener(advancedListener);
	}
	
	private OnItemClickListener advancedListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			try {
				startActivity(advancedList.get(position).intent);
			} catch(Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	};
}
