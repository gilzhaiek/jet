package com.reconinstruments.intro;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

public class LockOutActivity extends Activity {

	int[] unlock_sequence = new int[] { KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BACK };
	int[] key_sequence = new int[unlock_sequence.length];
	int key_sequence_pos = 0;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.lockout_layout);
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == unlock_sequence[key_sequence_pos]) {
			key_sequence[key_sequence_pos] = keyCode;
			key_sequence_pos++;
			key_sequence_pos %= unlock_sequence.length;
		} else {
			key_sequence = new int[unlock_sequence.length];
			key_sequence_pos = 0;
		}
		
		if(doesKeySequenceMatch()) {
			
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			
			finish();
			
			return true;
		}
		
		return true;
	}
	
	private boolean doesKeySequenceMatch() {
		int i = 0;
		while(i < unlock_sequence.length) {
			if(unlock_sequence[i] != key_sequence[i])
				return false;
			i++;
		}
		return true;
	}
	
}
