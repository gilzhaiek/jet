package com.reconinstruments.intro.instore;

import com.reconinstruments.intro.R;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.VideoView;

public class InStoreDemoActivity extends Activity {

	private static final String TAG = "InStoreDemoActivity";
	VideoView video_view;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.video);
		
		video_view = (VideoView) findViewById(R.id.video_view);
		video_view.setVideoURI(Uri.parse("android.resource://com.reconinstruments.intro/raw/oakley_demo"));
		video_view.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.setLooping(true);
			}});
		
		video_view.start();
	}
	
	public void onBackPressed() {
		return;
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		video_view.suspend();
		startActivityForResult(new Intent(this, com.reconinstruments.intro.instore.InStoreDemoModalActivity.class), 0);
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		
		return true;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(TAG, "onActivityResult");
		if(resultCode == RESULT_OK) {
			Log.v(TAG, "RESULT_OK");
			boolean resume = data.getBooleanExtra("resume", true);
			if(resume) {
				Log.v(TAG, "resume");
				video_view.resume();
			} else {
				Log.v(TAG, "no resume");
				finish();
			}
		}
	}
}
