package com.reconinstruments.QuickstartGuide;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.VideoView;
import com.reconinstruments.utils.UIUtils;

public class Outro extends QuickStartActivity {
	
	private String outroUriPath = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.fadeout;
	private Uri outroUri;
	private MediaPlayer mp = null; 
	private VideoView outroVideo; 	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler(); 
		outroUri = Uri.parse(outroUriPath);
		setContentView(R.layout.outro_layout);
		initFrameLayout(R.id.outroLayout); 
		outroVideo = (VideoView)findViewById(R.id.outroVideo); 
		animationSequence(); 
	}

	@Override 
	protected void onDestroy(){
		super.onDestroy();
	}
	
	@Override
	protected void onPause(){
		super.onPause();
	}
	
	public void animationSequence(){
		outroVideo.setVideoURI(outroUri);
		outroVideo.requestFocus();
		outroVideo.start();
		handler.postDelayed(videoIsDone, 4000);
	}
	
	public Runnable videoIsDone = new Runnable(){
		public void run(){
			boolean firstVideoPlay = Settings.System.getInt(getContentResolver(), "INTRO_VIDEO_PLAYED", 0) == 1;
		    UIUtils.setButtonHoldShouldLaunchApp(Outro.this,true);
			// if tutorial was played from initial startup
			if(!firstVideoPlay){
				Settings.System.putInt(getContentResolver(), "INTRO_VIDEO_PLAYED", 1); 
				Log.v("Outro Video", "================Intro Video Played: "+Settings.System.getInt(getContentResolver(), "INTRO_VIDEO_PLAYED", 0));
				Intent goToCompass = new Intent("com.reconinstruments.compass.CALIBRATE"); 
				goToCompass.putExtra(JetFadeIn.GO_TO_COMPASS_INTENT_NAME, JetFadeIn.CALLED_FROM_INITIAL_SETUP);
				goToCompass.putExtra("startFromBeginning", true);
				startActivity(goToCompass);
			
			}
			// otherwise, the tutorial is being started from Settings; finish
			finish();
		}
	};
	
}
