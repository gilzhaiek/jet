package com.reconinstruments.QuickstartGuide;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import com.reconinstruments.utils.DeviceUtils;
import android.view.WindowManager;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class QuickStartActivity extends Activity {
	
	public static final String CONFIRMATION_KEY = "confirmation";
	public static final String NEXT_CLASS_KEY = "next_class";
	public static final String GO_TO_COMPASS_INTENT_NAME =  "com.reconinstruments.QuickstartGuide.CALLED_FROM_INITIAL_SETUP";
	public static final boolean CALLED_FROM_INITIAL_SETUP = true;
	public static final String SHADOW_COLOR = "#000000";
	public TextView topText;
	public TextView bottomText;
	public Handler handler;
	public FrameLayout qsFrameLayout; 
	public Resources mRes;
	public static int currentBrightness;
	public static float floatBrightness;
	public Animation fadeOut;
	public Animation fadeIn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		mRes = getResources();
		fadeOut = AnimationUtils.loadAnimation(this,  R.anim.fadeout);
		fadeIn = AnimationUtils.loadAnimation(this,  R.anim.fadein);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	public void initFrameLayout(int id){
		qsFrameLayout = (FrameLayout)findViewById(id); 
	}
	
	public void initTopText(String text){
		LayoutParams topTextParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		topTextParams.gravity = (Gravity.CENTER | Gravity.TOP);
		topText = new TextView(this);
		topText.setText(text);
		topText.setTextSize(26);
		topText.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		topText.setId(5); 
		topText.setLayoutParams(topTextParams);
		topText.setPadding(0, 30, 0, 0);
	}
	public void initTopText(int stringId){
		initTopText(mRes.getString(stringId));
	}
	
	public void initBottomText(String text){
		LayoutParams bottomTextParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		bottomTextParams.gravity = (Gravity.CENTER | Gravity.BOTTOM);
		bottomText = new TextView(this);
		bottomText.setText(text);
		bottomText.setTextSize(26);
		bottomText.setShadowLayer(0.9f, 0, 2, Color.parseColor(SHADOW_COLOR));
		bottomText.setId(6); 
		bottomText.setLayoutParams(bottomTextParams);
		bottomText.setPadding(0, 0, 0, 30);
	}
	public void initBottomText(int stringId){
		initBottomText(mRes.getString(stringId));
	}
	
	public Runnable showTopText = new Runnable(){
		public void run(){
			qsFrameLayout.addView(topText);
		}
	};
	
	public Runnable hideTopText = new Runnable(){
		public void run(){
			topText.startAnimation(fadeOut);
			topText.setVisibility(View.GONE);
		}
	};
	
	public Runnable showBottomText = new Runnable(){
		public void run(){
			bottomText.startAnimation(fadeIn);
			qsFrameLayout.addView(bottomText);
		}
	};
	
	public Runnable hideBottomText = new Runnable(){
		public void run(){
			bottomText.startAnimation(fadeOut);
			bottomText.setVisibility(View.GONE);
		}
	};
	
}
