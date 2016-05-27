package com.reconinstruments.intro.startup;
import com.reconinstruments.intro.LockOutActivity;
import com.reconinstruments.intro.R;
import com.reconinstruments.intro.R.id;
import com.reconinstruments.intro.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.VideoView;


public class TutorialActivity extends Activity {

	private static final String TAG = "TutorialActivity";
	
	boolean isOakley = false;
	boolean locked = false;
	
	Uri reconVideoPath = Uri.parse("android.resource://com.reconinstruments.intro/raw/in_goggle_after_intro");
	Uri oakleyVideoPath = Uri.parse("android.resource://com.reconinstruments.intro/raw/in_goggle_no_intro");
	
	VideoView video_view;
	
	int[] unlock_sequence = new int[] { KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BACK };
	int[] key_sequence = new int[unlock_sequence.length];
	int key_sequence_pos = 0;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			isOakley = ai.metaData.getBoolean("isOakley");
		}
		catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			locked = ai.metaData.getBoolean("lockout");
		}
		catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		setContentView(R.layout.video);
		
		video_view = (VideoView) findViewById(R.id.video_view);
		video_view.setVideoURI(isOakley ? oakleyVideoPath : reconVideoPath);
		
		video_view.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if(locked) {
					startActivity(new Intent(getApplicationContext(), com.reconinstruments.intro.LockOutActivity.class));
				} else {
					startActivity(new Intent(TutorialActivity.this, com.reconinstruments.intro.startup.ReplayTutorialPromptActivity.class));
				}
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});
		
	}
	
	/*
	public void onResume() {
		super.onResume();
		
		if(!isOakley && getIntent().getBooleanExtra("start_one_sec_in", false)) { 
			Log.v(TAG, "seek to 5s in");
			video_view.seekTo(5000);
		}
	}
	*/
	
	public void onPostResume() {
		super.onPostResume();
		
		video_view.start();
	}

	public void onBackPressed() {
	    startActivity(new Intent(TutorialActivity.this, com.reconinstruments.intro.startup.ReplayTutorialPromptActivity.class));
	    finish();
	}
	
	// public boolean onKeyUp(int keyCode, KeyEvent event) {
	// 	if(keyCode == unlock_sequence[key_sequence_pos]) {
	// 		key_sequence[key_sequence_pos] = keyCode;
	// 		key_sequence_pos++;
	// 		key_sequence_pos %= unlock_sequence.length;
	// 	} else {
	// 		key_sequence = new int[unlock_sequence.length];
	// 		key_sequence_pos = 0;
	// 	}
		
	// 	if(doesKeySequenceMatch()) {
			
	// 		Intent intent = new Intent(Intent.ACTION_MAIN);
	// 		intent.addCategory(Intent.CATEGORY_HOME);
	// 		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// 		startActivity(intent);
			
	// 		finish();
			
	// 		return true;
	// 	}
		
	// 	return true;
	// }
	
	private boolean doesKeySequenceMatch() {
		int i = 0;
		while(i < unlock_sequence.length) {
			if(unlock_sequence[i] != key_sequence[i])
				return false;
			i++;
		}
		return true;
	}
}
