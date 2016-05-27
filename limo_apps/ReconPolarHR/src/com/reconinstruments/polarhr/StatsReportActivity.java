package com.reconinstruments.polarhr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

public class StatsReportActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats_report_layout);
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	
    	if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        	
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        	
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        } 
        else if (keyCode == KeyEvent.KEYCODE_BACK) {
        	finish();
        } 
        else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER) {
        	
        }
        else {
        	return false;
        }
    
        return true;
    }
}
