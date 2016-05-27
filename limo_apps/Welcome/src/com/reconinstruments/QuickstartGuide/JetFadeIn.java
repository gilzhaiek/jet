package com.reconinstruments.QuickstartGuide;

import com.reconinstruments.utils.DeviceUtils;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class JetFadeIn extends QuickStartActivity {
	
	ImageView jetIcon; 
	Animation slowFadeIn, slowFadeOut; 
	Intent next_intent; 
	LinearLayout jetFadeInLayout; 
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		//load animations to apply to image 
		slowFadeIn = AnimationUtils.loadAnimation(this, R.anim.slowfadein);
		slowFadeOut = AnimationUtils.loadAnimation(this, R.anim.slowfadeout); 
		next_intent = new Intent(this, Welcome.class);
		
		//Initialize UI with layouts and assets 
		setContentView(R.layout.jet_fade_in);
		jetFadeInLayout = (LinearLayout) findViewById(R.id.jet_fade_in_layout);
		jetIcon = (ImageView) findViewById(R.id.Jet_Icon);
		
		//Apply sequence of animations to asset
		handler.postDelayed(startAnimationSequence, 400); 
	}
	
	public Runnable fadeout = new Runnable(){
		public void run(){
			jetIcon.startAnimation(slowFadeOut);
		}
	};
	
	public Runnable fadein = new Runnable(){
		public void run(){
			jetIcon.startAnimation(slowFadeIn);
			jetIcon.setImageResource(R.drawable.jet_icon);
		}
	};
	
	public Runnable startAnimationSequence = new Runnable(){
		public void run(){
			startAnimationSequence();  
		}
	};
	
	public Runnable launchNextActivity = new Runnable(){
		public void run(){
			jetFadeInLayout.removeView(jetIcon);
			startActivity(next_intent); 
		}
	};
	
	private void startAnimationSequence(){
		handler.postDelayed(fadein, 1000);
		handler.postDelayed(fadeout, 3000);
		handler.postDelayed(launchNextActivity, 5000);
	}
}
