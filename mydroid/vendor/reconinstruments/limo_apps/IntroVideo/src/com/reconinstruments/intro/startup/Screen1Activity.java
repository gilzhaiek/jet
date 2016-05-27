package com.reconinstruments.intro.startup;

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
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.VideoView;

public class Screen1Activity extends Activity {

	boolean isOakley = false;
	View mainView;
	VideoView videoView = null;
	boolean videoPlayed = false;

	Uri path = Uri.parse("android.resource://com.reconinstruments.intro/raw/in_goggle_intro_no_status");
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			isOakley = ai.metaData.getBoolean("isOakley");
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (isOakley) {
			setContentView(R.layout.screen_one_layout);
		} else {
			setContentView(R.layout.video);

			videoView = (VideoView) findViewById(R.id.video_view);
			videoView.setVideoURI(path);
			videoView.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) {
					videoPlayed = true;
				}
			});
		}

		mainView = findViewById(android.R.id.content);
	}

	public void onPostResume() {
		super.onPostResume();

		if (videoView != null)
			videoView.start();
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (isOakley) {
			startActivity(new Intent(this, com.reconinstruments.intro.startup.OakleyDemoActivity.class));
			overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		} else if (videoView != null && videoPlayed) {
			startActivity(new Intent(this, com.reconinstruments.intro.startup.TutorialActivity.class));
			overridePendingTransition(0, 0);
		}
		return true;
	}
}
