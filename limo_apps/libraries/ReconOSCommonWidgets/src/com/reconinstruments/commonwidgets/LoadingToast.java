package com.reconinstruments.commonwidgets;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LoadingToast {

	View loadingToast; 
	Animation slideInBottom, fade_in, rotation;
	Handler handler; 
	ImageView loadingImage;
	TextView loadingText;
	LinearLayout loadingMessage;
	
	ViewGroup container;
	View layout;
	
	String message;
	
	public LoadingToast(Context context){
		//Setup the toast view to be inflated and inflate it 
		container = (ViewGroup) ((Activity) context).findViewById(android.R.id.content);
		layout = ((Activity)context).getLayoutInflater().inflate(R.layout.jet_toast, null);
		container.addView(layout);
		
		//Load animations to be used 
		slideInBottom = AnimationUtils.loadAnimation(context, R.anim.slide_in_toast);
		fade_in = AnimationUtils.loadAnimation(context, R.anim.fadein);
		rotation = AnimationUtils.loadAnimation(context, R.anim.rotation);
		rotation.setRepeatCount(Animation.INFINITE);
		handler = new Handler();
		
		//Initialize the toast's layout components 
		loadingToast = layout.findViewById(R.id.jetToast);
		loadingToast.setVisibility(View.INVISIBLE);
		loadingImage = (ImageView) layout.findViewById(R.id.jetToast_image);
		loadingImage.setVisibility(View.INVISIBLE);
		loadingText = (TextView) layout.findViewById(R.id.jetToast_text);
		loadingText.setVisibility(View.INVISIBLE);
		loadingMessage = (LinearLayout) layout.findViewById(R.id.jetToast_message);
	}
	
	public void show(String text){
	    message = text;
		loadingText.setText(message);
		loadingToast.setVisibility(View.VISIBLE);
		loadingImage.setImageResource(R.drawable.dot_array);
		loadingImage.setVisibility(View.VISIBLE);
		loadingText.setVisibility(View.VISIBLE);
		loadingMessage.startAnimation(slideInBottom);
		loadingImage.startAnimation(rotation);
	}
	
	public Runnable affirmation = new Runnable(){
		public void run(){
			loadingText.setText("Connected!");
			loadingImage.setImageResource(R.drawable.checkbox_icon);
			loadingImage.clearAnimation();
			loadingMessage.startAnimation(slideInBottom);
		}
	};
	
	public Runnable removeToast = new Runnable(){
		public void run(){
            loadingToast.startAnimation(new AlphaAnimation(1.0f, 0.0f));
	        container.removeView(layout);
	        container.invalidate();
		}
	};
    
    public void postMessage(String text){
        message = text; //"Connected!";
        handler.postDelayed(affirmation, 0);
    }
	
	public void dismiss(){
	    handler.postDelayed(removeToast, 0);
	}
}
