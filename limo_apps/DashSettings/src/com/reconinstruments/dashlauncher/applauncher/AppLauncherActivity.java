package com.reconinstruments.dashlauncher.applauncher;

import java.io.File;

import com.reconinstruments.dashsettings.R;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.GridView;
import android.widget.Toast;


public class AppLauncherActivity extends Activity {
	public static final String TAG = "AppLauncherActivity";

	private AppAdapter mAppAdapter;
	private boolean blocked = true;
	private int previousColumn;
	private GridView gridview;
	
	private AppAddDetect mAppAddDetect = new AppAddDetect();
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.app_grid_layout);

	    gridview = (GridView) findViewById(R.id.gridview);

	    mAppAdapter = new AppAdapter(this);
	    gridview.setAdapter(mAppAdapter);

	    // Fixes a keyEvent conflict between grid view and moving left
	    gridview.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View view, int keyCode, KeyEvent event) {
				Log.v(TAG, "selected column: " + (gridview.getSelectedItemPosition() % 3));

				int selectedColumn = gridview.getSelectedItemPosition() % 3;

				if(event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
					return selectedColumn != previousColumn;
				}

				previousColumn = selectedColumn;

				return false;
			}

	    });

	    gridview.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	        	ApplicationInfo app = (ApplicationInfo) mAppAdapter.getItem(position);
	        	startActivity(app.intent);
	        }
	    });
	    
	}

	public void onResume() {
		super.onResume();
		IntentFilter intentfilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		intentfilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentfilter.addDataScheme("package");
		registerReceiver(mAppAddDetect,intentfilter);
	}
	
	public void onPause() {
		super.onPause();
		unregisterReceiver(mAppAddDetect);
	}
	
	private class AppAddDetect extends BroadcastReceiver {
		@Override
		public void onReceive (Context c, Intent i) {
		    Log.d("AppAddDetect", "++++++++ Package add/remove +++++++++++");
		    
		    mAppAdapter = new AppAdapter(AppLauncherActivity.this);
		    gridview.setAdapter(mAppAdapter);
		    
		    //ReconPackageRecorder.writePackageData(c, new File(Environment.getExternalStorageDirectory(),PACKAGES_XML_FILE));
		    //goToFavoriteApp();

		}
	    }
}