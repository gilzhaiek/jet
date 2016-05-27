package com.reconinstruments.jetapplauncher.settings;

import java.util.ArrayList;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;


public class SetUnitsActivity extends ListActivity {
	
	SettingAdapter setUnitsAdapter;
	ArrayList<SettingItem> setUnitsList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
		headerIcon.setBackgroundResource(R.drawable.set_units_white);
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("SET UNITS");
		
		int unitSetting = SettingsUtil.getUnits(getBaseContext());
		
		setUnitsList = new ArrayList<SettingItem>();
		
		
		SettingItem item = new SettingItem("Metric");
		if (unitSetting == SettingsUtil.RECON_UNITS_METRIC)
			item.checkMark = true;
		setUnitsList.add(item);
		
		item = new SettingItem("US/Imperial");
		if (unitSetting == SettingsUtil.RECON_UNITS_IMPERIAL)
			item.checkMark = true;
		setUnitsList.add(item);
		
		setUnitsAdapter = new SettingAdapter(this, 0, setUnitsList); 
		
		setListAdapter(setUnitsAdapter);
		
		this.getListView().setOnItemClickListener(setUnitsListener);
		
	}
	
	private OnItemClickListener setUnitsListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			
			int unitSetting = SettingsUtil.getUnits(getBaseContext());
			
			if (position == 0){
				if( unitSetting != SettingsUtil.RECON_UNITS_METRIC ) {
					SettingsUtil.setUnits( getBaseContext(), SettingsUtil.RECON_UNITS_METRIC );
					setUnitsList.get(0).checkMark = true;
					setUnitsList.get(1).checkMark = false;
					setUnitsAdapter.notifyDataSetChanged();
				}
			}
			else
				if( unitSetting == SettingsUtil.RECON_UNITS_METRIC ) {
					SettingsUtil.setUnits(getBaseContext(), SettingsUtil.RECON_UNITS_IMPERIAL );
					setUnitsList.get(0).checkMark = false;
					setUnitsList.get(1).checkMark = true;
					setUnitsAdapter.notifyDataSetChanged();
				}
			
		}
		
	};

	

}
