package com.reconinstruments.QuickstartGuide;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.reconinstruments.QuickstartGuide.Confirmation;
import com.reconinstruments.QuickstartGuide.QuickStartActivity;
import com.reconinstruments.utils.UIUtils;

public class Welcome extends QuickStartActivity {

	VideoView welcomeVideo;
	TextView bottomText, bottomText2; 
	LinearLayout bottomLayout;
	Intent next_intent;
	Bundle bndlanimation;
	Bundle confirmation_bundle;
	String introUriPath = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.intro;
	String touchpadUriPath = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.touchpad;
	String swipeforwardUriPath = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.sweepforward;
	Uri introUri = Uri.parse(introUriPath); 
	Uri touchpadUri = Uri.parse(touchpadUriPath); 
	Uri swipeforwardUri = Uri.parse(swipeforwardUriPath);
	MediaPlayer mp = null; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
                UIUtils.setButtonHoldShouldLaunchApp(this,false);

		//Pre load transition data to minimize user interaction response time
		next_intent = new Intent(this, Confirmation.class);
		bndlanimation = ActivityOptions.makeCustomAnimation(
				getApplicationContext(), R.anim.confirmation_fadein,
				R.anim.slide_out_left).toBundle();
		confirmation_bundle = new Bundle(); 
		String swipeForwardSuccess = mRes.getString(R.string.swipe_forward_success),
			   swipeBackwardClass = mRes.getString(R.string.swipe_backward_page);
		
		//Information passed to Confirmation.java with what should be used as the confirmation text
		//and what the next activity is to launch after the confirmation has been shown
		confirmation_bundle.putString(QuickStartActivity.CONFIRMATION_KEY, swipeForwardSuccess);
		confirmation_bundle.putString(QuickStartActivity.NEXT_CLASS_KEY, swipeBackwardClass);
		next_intent.putExtras(confirmation_bundle);
		
		//Initialize UI
		setContentView(R.layout.welcome_layout);
		initFrameLayout(R.id.welcomeLayout);
		initVideoView();
		initBottomText(R.string.intro_welcome_message);
		initBottomTextWithAction();
		
		//Takes care of stitching together videos to look like one continuous stream 
		//with matching text overlayed on the frame layout. 
		animationSequence(); 
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	
	public void initBottomTextWithAction(){
		
		//Center Layout
		LayoutParams bottomTextParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
		bottomTextParams.gravity = (Gravity.CENTER | Gravity.BOTTOM);
		
		//Create Layout to add custom instructional text to 
		bottomLayout = new LinearLayout(this); 
		bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
		bottomLayout.setLayoutParams(bottomTextParams);
		
		bottomText = new TextView(this); 
		bottomText.setText(R.string.swipe_forward_bottom_text_1);
		bottomText.setTextSize(26); 
		bottomText.setId(6); 
		bottomText.setLayoutParams(bottomTextParams);
		bottomText.setPadding(0, 0, 0, 30);
		bottomText.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		bottomLayout.addView(bottomText);
		
		//accented instruction
		bottomText2 = new TextView(this); 
		bottomText2.setText(R.string.swipe_forward_bottom_text_2);
		Typeface semiBold = Typeface.createFromAsset(getAssets(), "fonts/opensans_semibold.ttf");
		bottomText2.setTypeface(semiBold);
		bottomText2.setTextSize(26); 
		bottomText2.setId(7); 
		bottomText2.setLayoutParams(bottomTextParams);
		bottomText2.setTextColor(Color.parseColor("#ffaa00"));
		bottomText2.setPadding(10, 0, 0, 30);
		bottomText2.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		bottomLayout.addView(bottomText2);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Load animations to be used if incorrect input is given by user
		Animation bounce_up = AnimationUtils.loadAnimation(this,  R.anim.bounce_up);
		Animation fadeOut = AnimationUtils.loadAnimation(this,  R.anim.fadeout);
		switch (keyCode) {
		
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			//Make sure that the proper instruction is displayed, only then proceed to with confirmation or otherwise. 
			if(bottomLayout.isShown()){
				startActivity(next_intent);
				bottomLayout.startAnimation(fadeOut);
				bottomLayout.setVisibility(View.GONE);
				welcomeVideo.pause();
			} 
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if(bottomLayout.isShown()){
				bottomLayout.startAnimation(bounce_up);
			}			
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if(bottomLayout.isShown()){
				bottomLayout.startAnimation(bounce_up);
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			if(bottomLayout.isShown()){
				bottomLayout.startAnimation(bounce_up);
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			//do nothing 
			return true;
		default:
			return true;
		}
	}
	
	public void initVideoView(){
		welcomeVideo = (VideoView)findViewById(R.id.welcomeVideo); 
		welcomeVideo.setVideoURI(introUri);
	}
	
	public Runnable showBottomLayout = new Runnable(){
		public void run(){
			qsFrameLayout.addView(bottomLayout);
		}
	};
	
	public Runnable playIntro = new Runnable(){
		public void run(){
			welcomeVideo.setVideoURI(introUri);
			welcomeVideo.requestFocus();
			welcomeVideo.start();
		}
	};
	
	public Runnable playTouchpad = new Runnable(){
		public void run(){
			welcomeVideo.setVideoURI(touchpadUri);
			welcomeVideo.requestFocus();
			welcomeVideo.start();
		}
	};
	
	public Runnable playSwipeforward = new Runnable(){
		public void run(){
			welcomeVideo.setVideoURI(swipeforwardUri);
			welcomeVideo.requestFocus();
			welcomeVideo.start();
			
			welcomeVideo.setOnPreparedListener(new OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp){
					mp.setLooping(true); 
				}
			});
		}
	};
	
	private void animationSequence(){
		handler.postDelayed(playIntro, 0); 
		handler.postDelayed(playTouchpad, 4000);
		handler.postDelayed(showBottomText, 4500); 
		handler.postDelayed(hideBottomText, 6800);
		handler.postDelayed(playSwipeforward, 6800); 
		handler.postDelayed(showBottomLayout, 7200); 
	}
	
}

