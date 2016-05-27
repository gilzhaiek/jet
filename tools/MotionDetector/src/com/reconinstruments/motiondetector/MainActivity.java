package com.reconinstruments.motiondetector;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private SharedPreferences mPreferences;

    private SharedPreferences.Editor mPreferenceEditor;

    private Button mProfileButton;
    private Button mDisplayButton;
    private Button mSettingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferenceEditor = mPreferences.edit();

        if(!mPreferences.contains("Profile")) {
            mPreferenceEditor.putString("Profile", "Walk1");
            mPreferenceEditor.apply();
        }
        if(!mPreferences.contains("Algorithm")) {
            mPreferenceEditor.putString("Algorithm", "Etienne1");
            mPreferenceEditor.apply();
        }
        if(!mPreferences.contains("Logging")) {
            mPreferenceEditor.putString("Logging", "True");
            mPreferenceEditor.apply();
        }

        mProfileButton = (Button) findViewById(R.id.profile);
        mDisplayButton = (Button) findViewById(R.id.display);
        mSettingButton = (Button) findViewById(R.id.settings);

        mProfileButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MotionDetectorProfile.class);
                startActivity(intent);
            }
        });

        mDisplayButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MotionDetectorDisplay.class);
                startActivity(intent);
            }
        });

        mSettingButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MotionDetectorSettings.class);
                startActivity(intent);
            }
        });
    }
}