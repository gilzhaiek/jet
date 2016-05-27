package com.reconinstruments.intro.onhill;

import java.util.ArrayList;

import com.reconinstruments.intro.R;
import com.reconinstruments.intro.SettingAdapter;
import com.reconinstruments.intro.SettingItem;
import com.reconinstruments.intro.R.id;
import com.reconinstruments.intro.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class VideoSelectorActivity extends Activity {

	boolean isOakley = false;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) findViewById(R.id.setting_desc_text);
		desc.setPadding(0, 0, 0, 15);
		desc.setText("Select Video");
		
		ArrayList<SettingItem> listItems = new ArrayList<SettingItem>();
		
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			isOakley = ai.metaData.getBoolean("isOakley");
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(isOakley) {
			listItems.add(new SettingItem(new Intent(this, com.reconinstruments.intro.onhill.VideoActivity.class), "Demo Video" ));
			listItems.add(new SettingItem(new Intent(this, com.reconinstruments.intro.onhill.VideoActivity.class), "Tutorial Video" ));
		} else {
			listItems.add(new SettingItem(new Intent(this, com.reconinstruments.intro.onhill.VideoActivity.class), "Tutorial Video"));
		}
		
		
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(new SettingAdapter(this, 0, listItems));
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Intent i = new Intent(getApplicationContext(), com.reconinstruments.intro.onhill.VideoActivity.class);
				if(isOakley) {
					if(position == 0) {
						i.putExtra("video_uri", "android.resource://com.reconinstruments.intro/raw/oakley_demo");
					} else {
						i.putExtra("video_uri", "android.resource://com.reconinstruments.intro/raw/in_goggle_no_intro");
					}
				} else {
					i.putExtra("video_uri", "android.resource://com.reconinstruments.intro/raw/in_goggle_after_intro");
				}
				startActivity(i);
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});
	}
}
