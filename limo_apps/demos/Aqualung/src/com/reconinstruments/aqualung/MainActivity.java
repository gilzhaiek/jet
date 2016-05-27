package com.reconinstruments.aqualung;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.app.Activity;
import android.content.Intent;

import com.reconinstruments.aqualung.R;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_layout);
        
        Intent i = new Intent("RECON_BLE_TEST_SERVICE");
        startService(i);
        
        ArrayList<SettingItem> listItems = new ArrayList<SettingItem>();
        
        listItems.add(new SettingItem(null, "Dashboard" ));
		listItems.add(new SettingItem(null, "Navigation" ));
        
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(new SettingAdapter(this, 0, listItems));
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Intent i = new Intent(getApplicationContext(), com.reconinstruments.aqualung.VideoActivity.class);
				if(position == 0) {
					i.putExtra("video_uri", "android.resource://com.reconinstruments.aqualung/raw/gui_speed");
				} else {
					i.putExtra("video_uri", "android.resource://com.reconinstruments.aqualung/raw/map_speed");
				}
				startActivity(i);
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
			
		});
    }
}
