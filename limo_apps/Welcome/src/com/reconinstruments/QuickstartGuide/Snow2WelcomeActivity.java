package com.reconinstruments.QuickstartGuide;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Snow2WelcomeActivity extends QuickStartActivity {

	ImageView jetIcon;
	Animation slowFadeIn, slowFadeOut;
	Intent next_intent;
	LinearLayout jetFadeInLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Settings.System.putInt(getContentResolver(), "INTRO_VIDEO_PLAYED", 1); 
		setContentView(R.layout.snow2_welcome_layout);
	}
	
	@Override
	public boolean onKeyUp(int keycode, KeyEvent keyEvent) {
		switch (keycode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			// if Snow2, skip welcome tutorial and head straight to
			// compass calibration and smartphone pairing
			Intent goToCompass = new Intent("com.reconinstruments.compass.CALIBRATE");
			goToCompass.putExtra(GO_TO_COMPASS_INTENT_NAME, CALLED_FROM_INITIAL_SETUP);
			goToCompass.putExtra("startFromBeginning", true);
			startActivity(goToCompass);
			finish();
			break;
		default:
			break;
		}
		return true;
	}

}
