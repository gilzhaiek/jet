package com.reconinstruments.intro.startup;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.reconinstruments.intro.R.id;
import com.reconinstruments.intro.R.layout;
import com.reconinstruments.intro.R;
import com.reconinstruments.intro.SettingAdapter;
import com.reconinstruments.intro.SettingItem;
import java.util.ArrayList;
import android.widget.ArrayAdapter;
public class ActivateDeviceActivityConfirm extends Activity {
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activate_device_confirm_layout);
	String [] items = {"UPDATE LATER","SHOW ME HOW TO UPDATE"};
	ArrayAdapter <String> ad = new ArrayAdapter<String>
	    (this, R.layout.recon_simple_list_item, items);
	ListView lv = (ListView) findViewById(android.R.id.list);
	lv.setAdapter(ad);
	lv.setSelection(1);
	lv.setOnItemClickListener(new OnItemClickListener() {
		public void onItemClick( AdapterView<?> adapterView,
					 View view, int position, long id) {
		    if(position == 0) {
			finish();
		    } else {
			Intent i = new Intent("RECON_UPDATE_AND_ACTIVATE");
			startActivity(i);
		    }
		    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
	    });
    }
    public void onBackPressed() {
	return;
    }
}
