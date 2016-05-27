package com.reconinstruments.chrono;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;

public class LapListView extends ListView {

	public LapListView(Context context) {
		super(context);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		super.onKeyDown(keyCode, event);
		
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			Log.v("LapListView", "KEYPAD_LEFT");
		}
		
		return false;
	}
}
