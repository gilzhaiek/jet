package com.reconinstruments.reconstats;

import java.util.ArrayList;

import com.reconinstruments.widgets.TabView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class RunHistoryActivity extends Activity {
	
	private TabView mTabView;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent i = this.getIntent();
		Bundle b = i.getExtras();
        ArrayList<Bundle> runs = b.getParcelableArrayList("Runs");
        
        if(runs == null || runs.size() < 1) {
			finish();
			return;
		}
        
        mTabView = new RunHistoryTabView(this, runs);
        
        setContentView(mTabView);
    }
}
