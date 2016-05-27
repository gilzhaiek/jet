package com.reconinstruments.dashlauncher.applauncher;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;

import com.reconinstruments.dashlauncher.BoardActivity;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.settings.SettingsActivity;

public class AppsOrSettingsActivity extends BoardActivity {
	public static final String TAG = "AppLauncherActivity";
	

	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.apps_settings);

	    View apps = (View) findViewById(R.id.apps);
	    View settings = (View) findViewById(R.id.settings);
	    
	    apps.setOnClickListener(new OnClickListener(){
	    	public void onClick(View v)
			{
				startActivity(new Intent(AppsOrSettingsActivity.this,AppLauncherActivity.class));
			}
	    });
	    settings.setOnClickListener(new OnClickListener(){
			public void onClick(View v)
			{
				startActivity(new Intent(AppsOrSettingsActivity.this,SettingsActivity.class));
				//finish();
			}
	    });
	    apps.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
	    settings.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
	    apps.setSelected(true);
	    apps.requestFocus();
	    
	    apps.setOnLongClickListener(musicShortcut);
	    settings.setOnLongClickListener(musicShortcut);
	}
}
