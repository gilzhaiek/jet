package com.reconinstruments.intro.onhill;

import com.reconinstruments.intro.R;
import com.reconinstruments.intro.R.id;
import com.reconinstruments.intro.R.layout;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.VideoView;

public class VideoActivity extends Activity {
	VideoView video_view;
	int[] unlock_sequence = new int[] { KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BACK };
	int[] key_sequence = new int[unlock_sequence.length];
	int key_sequence_pos = 0;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.video);
		
		String uriPath = this.getIntent().getStringExtra("video_uri");
		
		video_view = (VideoView) findViewById(R.id.video_view);
		video_view.setVideoURI(Uri.parse(uriPath));
		
		video_view.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				finish();
			}
		});
	}
	
	public void onPostResume() {
		super.onPostResume();
		
		video_view.start();
	}
}
