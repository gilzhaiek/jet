package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;

import com.reconinstruments.dashsettings.R;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.reconinstruments.commonwidgets.TwoOptionsJumpFixer;


public class ResetStatsActivity extends ListActivity {
	
	private TwoOptionsJumpFixer twoOptionsJumpFixer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.setting_layout);
		
		ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
		headerIcon.setBackgroundResource(R.drawable.reset_stats_white);
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("RESET STATS");
		
		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) findViewById(R.id.setting_desc_text);
		desc.setText("Max, min, average, and cumulative stats will be reset");
		
		
		ArrayList<SettingItem> resetStatsList = new ArrayList<SettingItem>();

		resetStatsList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "RESET" ));
		resetStatsList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "CANCEL" ));
		
		setListAdapter(new SettingButtonAdapter(this, resetStatsList));
		twoOptionsJumpFixer = new TwoOptionsJumpFixer(getListView());
		twoOptionsJumpFixer.start();
		
		this.getListView().setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				
				if(twoOptionsJumpFixer != null){
					twoOptionsJumpFixer.stop();
				}
				if (position == 0){
					Util.resetStats();
					Util.resetAllTimeStats(ResetStatsActivity.this);
					finish();
				}
				else{
					finish();	
				}				
			}});
		
	}



}
