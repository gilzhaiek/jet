package com.reconinstruments.motionlogger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.widget.LinearLayout;

public class FirstScreen extends Activity {

    //The current selection between walk and cycle
    private boolean mSelectWalk;

    //The Layout (to change the background)
    private LinearLayout mMainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_screen);

        mMainLayout = (LinearLayout) findViewById(R.id.first_screen);

        mSelectWalk = true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
            mMainLayout.playSoundEffect(SoundEffectConstants.CLICK);
			
			Intent i = new Intent(FirstScreen.this, MotionLogger.class);

            if (mSelectWalk) 
				i.putExtra("Walking?", true);
            else 
				i.putExtra("Walking?", false);

			FirstScreen.this.startActivity(i);
            this.finish();
            break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            mSelectWalk = false;
            mMainLayout.setBackgroundResource(R.drawable.first_screen_bike);
            mMainLayout.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            mSelectWalk = true;
            mMainLayout.setBackgroundResource(R.drawable.first_screen_walk);
            mMainLayout.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        }

        super.onKeyDown(keyCode, event);

        return true;
    }
}