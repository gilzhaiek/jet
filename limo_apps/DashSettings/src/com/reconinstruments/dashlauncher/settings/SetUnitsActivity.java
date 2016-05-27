package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;

import com.reconinstruments.dashsettings.R;

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
		
		int unitSetting = ReconSettingsUtil.getUnitsWrite(getBaseContext());
		
		setUnitsList = new ArrayList<SettingItem>();
		
		
		SettingItem item = new SettingItem("Metric");
		if (unitSetting == ReconSettingsUtil.RECON_UINTS_METRIC)
			item.checkMark = true;
		setUnitsList.add(item);
		
		item = new SettingItem("US/Imperial");
		if (unitSetting == ReconSettingsUtil.RECON_UINTS_IMPERIAL)
			item.checkMark = true;
		setUnitsList.add(item);
		
		setUnitsAdapter = new SettingAdapter(this, 0, setUnitsList); 
		
		setListAdapter(setUnitsAdapter);
		
		this.getListView().setOnItemClickListener(setUnitsListener);
		
	}
	
	private OnItemClickListener setUnitsListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			
			int unitSetting = ReconSettingsUtil.getUnitsWrite(getBaseContext());
			
			if (position == 0){
				if( unitSetting != ReconSettingsUtil.RECON_UINTS_METRIC )
				{
					ReconSettingsUtil.setUnits( getBaseContext(), ReconSettingsUtil.RECON_UINTS_METRIC );
					setUnitsList.get(0).checkMark = true;
					setUnitsList.get(1).checkMark = false;
					setUnitsAdapter.notifyDataSetChanged();
					
					//broadcast the unit-setting changed intent for potential listener
					Intent intent = new Intent( ReconSettingsUtil.RECON_UNIT_SETTING_CHANGED );
					intent.putExtra(ReconSettingsUtil.UNIT_SETTING, ReconSettingsUtil.RECON_UINTS_METRIC);
					
					getBaseContext().sendBroadcast(intent);
				}
			}
			else
				if( unitSetting == ReconSettingsUtil.RECON_UINTS_METRIC )
				{
					ReconSettingsUtil.setUnits(getBaseContext(), ReconSettingsUtil.RECON_UINTS_IMPERIAL );
					setUnitsList.get(0).checkMark = false;
					setUnitsList.get(1).checkMark = true;
					setUnitsAdapter.notifyDataSetChanged();
					
					//broadcast the unit-setting changed intent for potential listener
					Intent intent = new Intent( ReconSettingsUtil.RECON_UNIT_SETTING_CHANGED );
					intent.putExtra(ReconSettingsUtil.UNIT_SETTING, ReconSettingsUtil.RECON_UINTS_IMPERIAL);
					
					getBaseContext().sendBroadcast(intent);

				}
			
		}
		
	};

	

}
