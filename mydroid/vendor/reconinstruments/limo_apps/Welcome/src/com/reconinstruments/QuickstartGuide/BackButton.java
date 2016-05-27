package com.reconinstruments.QuickstartGuide;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class BackButton extends QuickStartActivity {

	VideoView backButtonAnimation; 
	LinearLayout topLayout; 
	TextView topText; 
	ImageView topImage; 
	String correctFrame = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.framecorrection;
	String backBtnGlow = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.back;
	Uri backBtnGlowUri = Uri.parse(backBtnGlow);
	Uri cfUri = Uri.parse(correctFrame); 
	MediaPlayer mp = null;
	Animation fadeIn;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.backbutton_layout); 
		backButtonAnimation = (VideoView)findViewById(R.id.backButtonVV);
		fadeIn = AnimationUtils.loadAnimation(this,  R.anim.fadein);
		initFrameLayout(R.id.backButton);
		initTopText(""); 
		initBottomText(R.string.back_button_bottom_text);
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
	
	@Override
	public void initTopText(String string){
		//center top layout
		LayoutParams topTextParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
		topTextParams.gravity = (Gravity.CENTER | Gravity.TOP);
		
		//create layout
		topLayout = new LinearLayout(this); 
		topLayout.setLayoutParams(topTextParams);
		topLayout.setOrientation(LinearLayout.HORIZONTAL);

		//add image view
		topImage = new ImageView(this); 
		topImage.setLayoutParams(topTextParams);
		topImage.setImageResource(R.drawable.back_btn);
		topImage.setPadding(0, 30, 0, 0);
		topLayout.addView(topImage);
		
		//add text view
		topText = new TextView(this); 
		topText.setLayoutParams(topTextParams); 
		topText.setText(R.string.back_button_top_text);
		topText.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		topText.setTextSize(26);
		topText.setId(8);
		topText.setPadding(10, 30, 0, 0);
		topLayout.addView(topText);
	}
	
	@Override 
	public boolean onKeyUp(int keyCode, KeyEvent event){
		Animation bounce_up = AnimationUtils.loadAnimation(this,  R.anim.bounce_up);
	    switch (keyCode) {
	    
	    case KeyEvent.KEYCODE_DPAD_LEFT:
			bottomText.startAnimation(bounce_up);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			bottomText.startAnimation(bounce_up);
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			bottomText.startAnimation(bounce_up);
			return true;
        case KeyEvent.KEYCODE_DPAD_DOWN:
        	bottomText.startAnimation(bounce_up);
			return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
	}
	
	@Override
	public void onBackPressed(){
		if(bottomText.isShown()){
			Animation fadeOut = AnimationUtils.loadAnimation(this,  R.anim.fadeout);
			Intent next_intent = new Intent(this, Confirmation.class); 
			Bundle bundle = new Bundle(); 
			bundle.putString(QuickStartActivity.CONFIRMATION_KEY, mRes.getString(R.string.back_success));
			Log.v("BackButton", "New Next class destination"); 
			bundle.putString(QuickStartActivity.NEXT_CLASS_KEY, mRes.getString(R.string.select_button_page)); 
			next_intent.putExtras(bundle);
			startActivity(next_intent);
			handler.postDelayed(hideBottomText, 0);
			topLayout.startAnimation(fadeOut);
			qsFrameLayout.removeView(topLayout);
			qsFrameLayout.removeView(bottomText);
			backButtonAnimation.pause();
		}
	}
	
	public Runnable correctFrameTrans = new Runnable(){
		public void run(){
			backButtonAnimation.setVideoURI(cfUri);
			backButtonAnimation.requestFocus();
			backButtonAnimation.start();
		}
	};
	
	public Runnable backButtonTrans = new Runnable(){
		public void run(){
			backButtonAnimation.setVideoURI(backBtnGlowUri);
			backButtonAnimation.requestFocus();
			backButtonAnimation.start();
			
			backButtonAnimation.setOnPreparedListener(new OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp){
					mp.setLooping(true); 
				}
			});
		}
	};
	
	public Runnable showTopLayout = new Runnable(){
		public void run(){
			topLayout.startAnimation(fadeIn);
			qsFrameLayout.addView(topLayout);
		}
	};
	
	public Runnable hideTopTextWithImage = new Runnable(){
		public void run(){
			topLayout.setVisibility(View.GONE);
		}
	};
	
	public void animationSequence(){
		handler.postDelayed(correctFrameTrans, 0);
		handler.postDelayed(backButtonTrans, 1500);
		handler.postDelayed(showTopLayout, 2000); 
		handler.postDelayed(showBottomText,2500);
	}
}
