package com.reconinstruments.motiondetector;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

public class MotionDetectorSettings extends Activity {

    private SharedPreferences mPreferences;

    private SharedPreferences.Editor mPreferenceEditor;

    private RelativeLayout mMainLayout;

    private Button mProfOneButton;
    private Button mProfTwoButton;

    private Button mUseLog;

    private Button mAlgoOneButton;
    private Button mAlgoTwoButton;
    private Button mAlgoThrButton;
    private Button mAlgoForButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.motion_settings);

        mMainLayout = (RelativeLayout) findViewById(R.id.motion_settings);
        mMainLayout.setBackgroundColor(Color.parseColor("#aaaaaa"));

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferenceEditor = mPreferences.edit();

        mProfOneButton = (Button) findViewById(R.id.profOneButton);
        mProfTwoButton = (Button) findViewById(R.id.profTwoButton);

        mProfOneButton.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mPreferenceEditor.putString("Profile", "Walk1");
                    mPreferenceEditor.apply();
                }
            });
        
        mProfTwoButton.setOnClickListener( new View.OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    mPreferenceEditor.putString("Profile", "Bike1");
                    mPreferenceEditor.apply();
                }
            });

        mUseLog = (Button) findViewById(R.id.useLog);

        mUseLog.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if(mPreferences.getString("Logging", "").equals("True")) {
                        mPreferenceEditor.putString("Logging", "False");
                        mPreferenceEditor.apply();
                        mUseLog.setText("Use Logging");
                    }
                    else {
                        mPreferenceEditor.putString("Logging", "True");
                        mPreferenceEditor.apply();
                        mUseLog.setText("Don't use Logging");
                    }
                    mPreferenceEditor.apply();
                }
            });

        mAlgoOneButton = (Button) findViewById(R.id.algoOneButton);
        mAlgoTwoButton = (Button) findViewById(R.id.algoTwoButton);
        mAlgoThrButton = (Button) findViewById(R.id.algoThrButton);
	mAlgoForButton = (Button) findViewById(R.id.algoForButton);

        mAlgoOneButton.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mPreferenceEditor.putString("Algorithm", "Etienne1");
                    mPreferenceEditor.apply();
                }
            });

        mAlgoTwoButton.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mPreferenceEditor.putString("Algorithm", "Ahmed1");
                    mPreferenceEditor.apply();
                }
            });

        mAlgoThrButton.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mPreferenceEditor.putString("Algorithm", "Ahmed2");
                    mPreferenceEditor.apply();
                }
            });
	mAlgoForButton.setOnClickListener( new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
		    mPreferenceEditor.putString("Algorithm", "EasyBike");
		    mPreferenceEditor.apply();
		}
	    });
    }
}