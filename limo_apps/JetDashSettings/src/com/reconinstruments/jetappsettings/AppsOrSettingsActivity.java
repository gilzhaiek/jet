package com.reconinstruments.jetappsettings;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.util.Log;

import com.reconinstruments.dashelement1.ColumnElementActivity;
import com.reconinstruments.jetapplauncher.applauncher.AppLauncherActivity;
import com.reconinstruments.jetapplauncher.settings.SettingsActivity;
import com.reconinstruments.jetapplauncher.R;

public class AppsOrSettingsActivity extends ColumnElementActivity {
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
					transition.startTransition(1);
				} else {
					transition.resetTransition();
				}
			}
		});
	    settings.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(1);
				} else {
					transition.resetTransition();
				}
			}
		});
	}

	@Override
	public void onBackPressed() {
		goBack();
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	switch(keyCode) {
	case KeyEvent.KEYCODE_DPAD_UP:
	    return false;
	case KeyEvent.KEYCODE_DPAD_DOWN:
	    return false;
	}
	return super.onKeyDown(keyCode,event);
    }
}
