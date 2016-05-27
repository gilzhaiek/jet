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

public class SelectButton extends QuickStartActivity {
	
	private static final String TAG = SelectButton.class.getSimpleName();
	
	VideoView selectButtonAnimation; 
	ImageView topImage; 
	LinearLayout topLayout; 
	TextView topText; 
	String correctFrame = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.framecorrection;
	String selectGlow = "android.resource://com.reconinstruments.QuickstartGuide/"+R.raw.select;
	Uri cfUri = Uri.parse(correctFrame); 
	Uri selectGlowUri = Uri.parse(selectGlow); 
	Intent next_intent;
	Bundle bundle;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); 
		next_intent = new Intent(this, Confirmation.class); 
		bundle = new Bundle();
		bundle.putString(QuickStartActivity.CONFIRMATION_KEY, mRes.getString(R.string.select_success));
		bundle.putString(QuickStartActivity.NEXT_CLASS_KEY, mRes.getString(R.string.finished_page)); 
		next_intent.putExtras(bundle); 
		setContentView(R.layout.selectbutton_layout); 
		selectButtonAnimation = (VideoView)findViewById(R.id.selectButtonVV); 
		initFrameLayout(R.id.selectButton);
		initTopText("");
		initBottomText(R.string.select_button_bottom_text); 
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
		topImage.setImageResource(R.drawable.select_btn);
		topImage.setPadding(0, 30, 0, 0);
		topLayout.addView(topImage);

		//add text view
		topText = new TextView(this);
		topText.setLayoutParams(topTextParams);
		topText.setText(R.string.select_button_top_text);
		topText.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		topText.setTextSize(26);
		topText.setId(8);
		topText.setPadding(5, 30, 0, 0);
		topLayout.addView(topText);
	}
	
	@Override 
	public boolean onKeyUp(int keyCode, KeyEvent event){
		Animation fadeOut = AnimationUtils.loadAnimation(this,  R.anim.fadeout);
		Animation bounce_up = AnimationUtils.loadAnimation(this,  R.anim.bounce_up);

		Log.d(TAG, "keycode: " + keyCode);
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
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
        	if(bottomText.isShown()){
				startActivity(next_intent);
				handler.postDelayed(hideBottomText, 0);
				topLayout.startAnimation(fadeOut);
				topLayout.setVisibility(View.GONE);
	        	selectButtonAnimation.pause();
        	}
        default:
            return super.onKeyUp(keyCode, event);
        }
	}
	
	@Override
	public void onBackPressed(){
		//do nothing 
	}
	
	public Runnable selectButtonTrans1 = new Runnable(){
		public void run(){
			selectButtonAnimation.setVideoURI(cfUri);
			selectButtonAnimation.requestFocus();
			selectButtonAnimation.start();
		}
	};

	
	public Runnable selectButtonTrans2 = new Runnable(){
		public void run(){
			selectButtonAnimation.setVideoURI(selectGlowUri);
			selectButtonAnimation.requestFocus(); 
			selectButtonAnimation.start();
			
			selectButtonAnimation.setOnPreparedListener(new OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp){
					mp.setLooping(true); 
				}
			});
		}
	};
	
	public Runnable showTopTextWithImage = new Runnable(){
		public void run(){
			qsFrameLayout.addView(topLayout);
		}
	};
	
	public Runnable hideTopTextWithImage = new Runnable(){
		public void run(){
			topLayout.setVisibility(View.GONE);
		}
	};
	
	public void animationSequence(){
		handler.postDelayed(selectButtonTrans2, 0);
		handler.postDelayed(showTopTextWithImage, 500); 
		handler.postDelayed(showBottomText, 1500);

	}
	
}
