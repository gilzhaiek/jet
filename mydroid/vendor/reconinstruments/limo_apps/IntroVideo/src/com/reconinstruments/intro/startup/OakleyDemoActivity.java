package com.reconinstruments.intro.startup;

import com.reconinstruments.intro.R;
import com.reconinstruments.intro.R.id;
import com.reconinstruments.intro.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.VideoView;

public class OakleyDemoActivity extends Activity {
	
	Uri path = Uri.parse("android.resource://com.reconinstruments.intro/raw/oakley_demo");
	
	VideoView video_view;
	int[] unlock_sequence = new int[] { KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BACK };
	int[] key_sequence = new int[unlock_sequence.length];
	int key_sequence_pos = 0;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.video);
		
		video_view = (VideoView) findViewById(R.id.video_view);
		video_view.setVideoURI(path);
		
		video_view.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				// Goto ReplayDemoActivity
				startActivity(new Intent(OakleyDemoActivity.this, com.reconinstruments.intro.startup.ReplayDemoPromptActivity.class));
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});
	}
	
	public void onPostResume() {
		super.onPostResume();
		
		video_view.start();
	}
	
	public void onBackPressed() {
		return;
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == unlock_sequence[key_sequence_pos]) {
			key_sequence[key_sequence_pos] = keyCode;
			key_sequence_pos++;
			key_sequence_pos %= unlock_sequence.length;
		} else {
			key_sequence = new int[unlock_sequence.length];
			key_sequence_pos = 0;
		}
		
		if(doesKeySequenceMatch()) {
			
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			
			finish();
			
			return true;
		}
		
		return true;
	}
	
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
