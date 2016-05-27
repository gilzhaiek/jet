package com.reconinstruments.test.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.reconinstruments.bletest.*;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

public class MusicControlTest extends Activity {
    private static final String TAG = "BLEMusicControlTest";
    private static final byte MUSIC_NEXT_TRACK = 0x01;
    private static final byte MUSIC_PREVIOUS_TRACK = 0x02;
    private static final byte MUSIC_VOLUME_UP = 0x03;
    private static final byte MUSIC_VOLUME_DOWN = 0x04;
    private static final byte MUSIC_TOGGLE_PLAY_PAUSE = 0x05;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music);

	final Button btnnt = (Button) findViewById(R.id.btnnt);
        btnnt.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    bleCommandWithCheck(MUSIC_NEXT_TRACK);
		}
	    });

	final Button btnpt = (Button) findViewById(R.id.btnpt);
        btnpt.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    bleCommandWithCheck(MUSIC_PREVIOUS_TRACK);
		}
	    });


	final Button btnvu = (Button) findViewById(R.id.btnvu);
        btnvu.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    bleCommandWithCheck(MUSIC_VOLUME_UP);
		}
	    });


	final Button btnvd = (Button) findViewById(R.id.btnvd);
        btnvd.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    bleCommandWithCheck(MUSIC_VOLUME_DOWN);
		}
	    });

	final Button btntpp = (Button) findViewById(R.id.btntpp);
        btntpp.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    bleCommandWithCheck(MUSIC_TOGGLE_PLAY_PAUSE);
		}
	    });
    }

    @Override
    public void onStart() {
	super.onStart();
	initService();
    }

    @Override
    public void onStop() {
	releaseService();
	super.onStop();
    }

    //////////////////////////////////////////////////////
    // aidl service connection.
    /////////////////////////////////////////////////////
    private IBLEService bleService;
    private BLEServiceConnection bleServiceConnection;

    private void initService() {
        if( bleServiceConnection == null ) {
            bleServiceConnection = new BLEServiceConnection();
            Intent i = new Intent("RECON_BLE_TEST_SERVICE");
            bindService( i, bleServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d( TAG, "bindService()" );
        } 
    }

    private void releaseService() {
	if( bleServiceConnection != null ) {
	    unbindService( bleServiceConnection );	  
	    bleServiceConnection = null;
	    Log.d( TAG, "unbindService()" );
        }
    }

    class BLEServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, 
				       IBinder boundService ) {
            bleService = IBLEService.Stub.asInterface((IBinder)boundService);
            Log.d(TAG,"onServiceConnected" );
        }

        public void onServiceDisconnected(ComponentName className) {
            bleService = null;
            Log.d( TAG,"onServiceDisconnected" );
        }
    };
    /////////////////// End of aidl shit///////////////////////

    private void bleCommandWithCheck(byte b) {
	try {
	    if (bleService != null) {
		bleService.sendControlByte(b);
	    }
	} catch (RemoteException e) {
	    e.printStackTrace();
	}
    }
}
    
