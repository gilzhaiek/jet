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
import com.reconinstruments.ifisoakley.OakleyDecider;
public class ActivateDeviceActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	if (OakleyDecider.isOakley()) {
	    setContentView(R.layout.activate_device_layout_airwave);
	} else {
	    setContentView(R.layout.activate_device_layout);
	}
    }
    public void onBackPressed() {
	Intent i = new Intent("RECON_CONFIRM_ACTIVATE");
	startActivity(i);
	finish();
	return;
    }
}
