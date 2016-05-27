package com.reconinstruments.jump;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class JumpHistoryActivity extends Activity {

	private JumpHistoryTabView mTabView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = this.getIntent();
		Bundle b = i.getExtras();
		
		ArrayList<Bundle> jumps = b.getParcelableArrayList("Jumps");
		
		if(jumps == null || jumps.size() < 1) {
			finish();
			return;
		}
		
		mTabView = new JumpHistoryTabView(this, jumps);
		setContentView(mTabView);
	}
}
