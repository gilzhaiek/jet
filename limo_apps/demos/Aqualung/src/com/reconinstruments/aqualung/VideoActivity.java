package com.reconinstruments.aqualung;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

public class VideoActivity extends Activity {

	VideoView video_view;

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
