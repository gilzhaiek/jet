package com.reconinstruments.jump;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ReconJumpActivity extends Activity {
	
	private JumpTabView mJumpTabView = null;
	private TranscendServiceConnection mTranscendConnection = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mJumpTabView = new JumpTabView(this);
        
        mTranscendConnection = new TranscendServiceConnection( this, mJumpTabView );            
        boolean connect = bindService( new Intent( "RECON_MOD_SERVICE" ), mTranscendConnection, Context.BIND_AUTO_CREATE );
        
        setContentView(mJumpTabView);
    }
    
    @Override 
    protected void onDestroy()
    {
    	super.onDestroy();
    	
        //unbind Transcend Service connection
        unbindService( mTranscendConnection );
        
    }
}