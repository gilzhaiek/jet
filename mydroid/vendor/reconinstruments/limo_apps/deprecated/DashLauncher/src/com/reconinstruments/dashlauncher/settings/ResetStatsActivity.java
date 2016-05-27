package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;

public class ResetStatsActivity extends ListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("Reset Stats History");
		
		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) findViewById(R.id.setting_desc_text);
		desc.setText("Max, min, average, and"+"\n"+"cumulative stats will be reset");
		
		
		ArrayList<SettingItem> resetStatsList = new ArrayList<SettingItem>();

		resetStatsList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "Reset" ));
		resetStatsList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "Cancel" ));
		
		setListAdapter(new SettingAdapter(this, 0, resetStatsList));
		
		this.getListView().setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				
				if (position == 0){
					Util.resetStats();
					Util.resetAllTimeStats();
					finish();
				}
				else{
					finish();	
				}				
			}});
		
	}



}
