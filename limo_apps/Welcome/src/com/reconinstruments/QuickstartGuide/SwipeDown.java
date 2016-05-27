package com.reconinstruments.QuickstartGuide;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.reconinstruments.QuickstartGuide.QuickStartActivity;

public class SwipeDown extends QuickStartActivity {
	Intent next_intent;
	Bundle bndlanimation;
	Bundle bundle;
	TextView bottomText, bottomText2; 
	LinearLayout bottomLayout;
	VideoView downVideo; 
	String downUriPath = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.sweepdown;
	Uri downUri = Uri.parse(downUriPath);
	MediaPlayer mp = null;
	private boolean swipeEnabled = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        //Pre load transition data to minimize user interaction response time 
        next_intent = new Intent(this, Confirmation.class); 
    	bndlanimation = ActivityOptions.makeCustomAnimation(
				getApplicationContext(), R.anim.confirmation_fadein, R.anim.slide_out_top).toBundle();
    	bundle = new Bundle(); 
    	
    	//Information passed to Confirmation.java with what should be used as the confirmation text
    	//and what the next activity is to launch after the confirmation has been shown
		bundle.putString(QuickStartActivity.CONFIRMATION_KEY, mRes.getString(R.string.swipe_down_success));
		bundle.putString(QuickStartActivity.NEXT_CLASS_KEY, mRes.getString(R.string.back_button_page));
		next_intent.putExtras(bundle);
		
		//Init UI 
        setContentView(R.layout.swipedown_layout);
        initFrameLayout(R.id.swipeDown); 
		initBottomText(""); 
		startVideo();
		qsFrameLayout.addView(bottomLayout);
		new CountDownTimer(1 * 1000, 1000) {
			public void onTick(long millisUntilFinished) {
			}
			public void onFinish() {
				swipeEnabled = true;
			}
		}.start();
		
    }
	
	@Override
    protected void onDestroy() {
        super.onDestroy();
    }
	
	@Override 
	protected void onPause(){
		super.onPause();
	}

	
	@Override
	public void initBottomText(String string){
		
		//Center Layout
		LayoutParams bottomTextParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
		bottomTextParams.gravity = (Gravity.CENTER | Gravity.BOTTOM);
		
		//Create Layout to add custom instructional text to 
		bottomLayout = new LinearLayout(this); 
		bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
		bottomLayout.setLayoutParams(bottomTextParams);
		
		bottomText = new TextView(this); 
		bottomText.setText(R.string.swipe_down_bottom_text_1);
		bottomText.setTextSize(26);
		bottomText.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		bottomText.setId(6); 
		bottomText.setLayoutParams(bottomTextParams);
		bottomText.setPadding(0, 0, 0, 30);
		bottomLayout.addView(bottomText);
		
		bottomText2 = new TextView(this); 
		bottomText2.setText(R.string.swipe_down_bottom_text_2);
		Typeface semiBold = Typeface.createFromAsset(getAssets(), "fonts/opensans_semibold.ttf");
		bottomText2.setTypeface(semiBold);
		bottomText2.setTextSize(26);
		bottomText2.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		bottomText2.setId(7); 
		bottomText2.setLayoutParams(bottomTextParams);
		bottomText2.setTextColor(Color.parseColor("#ffaa00"));
		bottomText2.setPadding(10, 0, 0, 30);
		bottomLayout.addView(bottomText2);
		
	}
	
	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event){
		Animation bounce_up = AnimationUtils.loadAnimation(this,  R.anim.bounce_up);
		Animation fadeOut = AnimationUtils.loadAnimation(this,  R.anim.fadeout);

	    switch (keyCode) {
		//If proper input from user is given proceed to confirmation 
        case KeyEvent.KEYCODE_DPAD_DOWN:
			if(bottomLayout.isShown() && swipeEnabled) {
				startActivity(next_intent);
				bottomLayout.startAnimation(fadeOut);
				bottomLayout.setVisibility(View.GONE);
				downVideo.pause();
				return true;
			}
	    case KeyEvent.KEYCODE_DPAD_LEFT:
			bottomLayout.startAnimation(bounce_up);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			bottomLayout.startAnimation(bounce_up);
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			bottomLayout.startAnimation(bounce_up);
			return true;

        default:
            return super.onKeyUp(keyCode, event);
        }	
	}
	
	@Override
	public void onBackPressed(){
		//do nothing 
	}
	
	private void startVideo(){
		downVideo = (VideoView)findViewById(R.id.swipeDownVideo);
		downVideo.setVideoURI(downUri);
		downVideo.requestFocus();
		downVideo.start();
		downVideo.setOnPreparedListener(new OnPreparedListener(){
			@Override
			public void onPrepared(MediaPlayer mp){
				mp.setLooping(true); 
			}
		});
	}
}
