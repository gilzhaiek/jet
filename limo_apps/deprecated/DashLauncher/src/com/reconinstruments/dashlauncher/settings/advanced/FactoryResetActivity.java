package com.reconinstruments.dashlauncher.settings.advanced;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.platform.UBootEnvNative;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.settings.SettingAdapter;
import com.reconinstruments.dashlauncher.settings.SettingItem;
import com.reconinstruments.dashlauncher.settings.TimeZoneActivity;

public class FactoryResetActivity extends ListActivity {
	
	static private final String UBOOT_OPT_NAME = "BOOT_OPT";
	static private final String UBOOT_UPDATE_FIRMWARE = "UPDATE_REQ";
	static private final String UBOOT_RESET_FIRMWARE = "FACTORY_REQ";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("Factory Reset");
		
		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) findViewById(R.id.setting_desc_text);
		desc.setText("Firmware will be reset to the"+"\n"+"factory default");
		
		
		ArrayList<SettingItem> factoryResetList = new ArrayList<SettingItem>();

		factoryResetList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "Reset" ));
		factoryResetList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "Cancel" ));
		
		setListAdapter(new SettingAdapter(this, 0, factoryResetList));
		
		this.getListView().setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				if (position == 0){
					PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
					UBootEnvNative.Set_UBootVar(UBOOT_OPT_NAME, UBOOT_RESET_FIRMWARE);
					pm.reboot(null);
				}
				else{
					finish();	
				}				
			}});
		
	}

}
