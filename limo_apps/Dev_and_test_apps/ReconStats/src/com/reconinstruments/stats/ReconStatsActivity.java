package com.reconinstruments.stats;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.reconinstruments.reconstats.R;

public class ReconStatsActivity extends Activity {
	
	private StatsTabView mStatsTabView = null;
	private TranscendServiceConnection mTranscendConnection = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mStatsTabView = new StatsTabView(this);
        
        setContentView(mStatsTabView);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	mTranscendConnection = TranscendServiceConnection.getInstance(this); 
    	mTranscendConnection.setViewToUpdate(mStatsTabView);
    	bindService( new Intent( "RECON_MOD_SERVICE" ), mTranscendConnection, Context.BIND_AUTO_CREATE );
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	unbindService(mTranscendConnection);
    }
}