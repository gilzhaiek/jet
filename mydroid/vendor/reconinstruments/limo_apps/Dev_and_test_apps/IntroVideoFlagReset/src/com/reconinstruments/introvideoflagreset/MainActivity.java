package com.reconinstruments.introvideoflagreset;

import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;

import com.reconinstruments.introvideoflagreset.R;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Settings.System.putInt(this.getContentResolver(), "INTRO_VIDEO_PLAYED", 0);
    }

}
