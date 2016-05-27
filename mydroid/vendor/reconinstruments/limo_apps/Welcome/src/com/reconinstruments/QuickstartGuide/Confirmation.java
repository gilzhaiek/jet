package com.reconinstruments.QuickstartGuide;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Confirmation extends Activity {

	Class<?> c = null; 
	Resources mRes;
	TextView confirmation_textview, outro_string;
	LinearLayout confirmationLayout, confirmationString; 
	ImageView checkMark; 
	Handler handler;
	MediaPlayer mp = null;
	Bundle bundleAnimation;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bundleAnimation = ActivityOptions.makeCustomAnimation(
				getApplicationContext(), R.anim.fadein, R.anim.fadeout).toBundle();

		mRes = getResources();
		//Possible custom points of entry depending on class that envokes this activity 
		Animation slideInBottom = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
		//Remove content from bundle passed and determine appropriate action from there.
		Bundle b = getIntent().getExtras();
		String confirmation = b.getString(QuickStartActivity.CONFIRMATION_KEY);
		String next_class = b.getString(QuickStartActivity.NEXT_CLASS_KEY);
		Log.v("\n\nConfirmation: ", confirmation + "Next Activity: " + next_class); 
		setContentView(R.layout.confirmation_layout);
		confirmationLayout = (LinearLayout)findViewById(R.id.confirmationLayout);
		confirmationString = (LinearLayout)findViewById(R.id.confirmationLL); 
		checkMark = (ImageView)findViewById(R.id.checkMark); 
		handler = new Handler();
		mp = MediaPlayer.create(this, R.raw.confirmation);
		mp.start();
		confirmation_textview = (TextView) findViewById(R.id.confirmation_string);
		confirmation_textview.setText(confirmation);
		chooseNextIntent(slideInBottom, confirmation);

	}
	
	private void chooseNextIntent(Animation animation, String confirmMessage){
		confirmationString.startAnimation(animation);
		handler.postDelayed(fadeAway, 1000);
	}
	
	@Override 
	protected void onDestroy(){
		super.onDestroy();
	}

	
	@Override
	public void onBackPressed(){
		//do nothing 
	}
	
	public Runnable fadeAway = new Runnable(){
		public void run(){
			Bundle b = getIntent().getExtras();
			String next_class = b.getString(QuickStartActivity.NEXT_CLASS_KEY);
			Log.v("fade runner", next_class); 
			Intent next_intent;
			if(next_class.equals(mRes.getString(R.string.swipe_backward_page))){
				next_intent = new Intent(getBaseContext(), SwipeBackward.class);
			} 
			else if(next_class.equals(mRes.getString(R.string.swipe_down_page))){
				next_intent = new Intent(getBaseContext(), SwipeDown.class);
			} 
			else if(next_class.equals(mRes.getString(R.string.select_button_page))){
				next_intent = new Intent(getBaseContext(), SelectButton.class); 
			} 
			else if(next_class.equals(mRes.getString(R.string.finished_page))){
				next_intent = new Intent(getBaseContext(), Outro.class);
			} 
			else {//Means: next_class.equals("BackButton"
				next_intent = new Intent(getBaseContext(), BackButton.class);
			}
			startActivity(next_intent, bundleAnimation);
		}
	};

}
