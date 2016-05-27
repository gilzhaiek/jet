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
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.intro.R.id;
import com.reconinstruments.intro.R.layout;
import com.reconinstruments.intro.R;
import com.reconinstruments.intro.SettingAdapter;
import com.reconinstruments.intro.SettingItem;
import java.util.ArrayList;

public class ReplayTutorialPromptActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.setting_layout_replay);
	// Change url to oakley in case it is Oakley:
	if (OakleyDecider.isOakley()) {
	    TextView tv = (TextView) (findViewById(R.id.setting_desc_text_3));
	    tv.setText(R.string.airwave_url);
	    
	}
	String [] items = {"REPLAY TUTORIAL VIDEO","CONTINUE TO HUD EXPERIENCE"};
	ArrayAdapter <String> ad = new ArrayAdapter<String>
	    (this, R.layout.recon_simple_list_item, items);
	ListView lv = (ListView) findViewById(android.R.id.list);
	lv.setAdapter(ad);
	lv.setSelection(1);
	lv.setOnItemClickListener(new OnItemClickListener() {
		public void onItemClick( AdapterView<?> adapterView,
					 View view, int position, long id) {
		    if(position == 1) {
			// Remember demo movie watched
			rememberAndFinish();
		    } else {
			Intent i = new Intent(getApplicationContext(),
					      com.reconinstruments.intro.startup.TutorialActivity.class);
			i.putExtra("start_one_sec_in", true);
			startActivity(i);
		    }
		    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
	    });
    }
    public void onBackPressed() {
	return;
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
	switch(keyCode) {
	case KeyEvent.KEYCODE_POWER:
	    rememberAndFinish();
	    return true;
	}
	return super.onKeyUp(keyCode, event);
    }
    private void rememberAndFinish() {
	Settings.System.putInt(ReplayTutorialPromptActivity.this.getContentResolver(),
			       "INTRO_VIDEO_PLAYED", 1);
	finish();
    }
}
