package com.reconinstruments.jumpvisualiser;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.reconinstruments.ReconSDK.*;

public class ReconJumpVisualiser extends Activity implements IReconEventListener
{
	//private static final String TAG = ReconJumpVisualiser.class.getSimpleName();
	//ReconSDKManager mDataManager = ReconSDKManager.Initialize(this);
	//private static final String RECON_DATA_BUNDLE = "RECON_DATA_BUNDLE";

	// Jump container object (used to parse data from incoming jump events)
	private Jump mJump = null;
	
	// Main class used for jump generation and visualisation
	private DrawPanel mPanel = null;
	
	// Custom broadcast receiver for Jump Events
	private JumpEventReciever mJumpEventReceiver = null;
	
	//
	private ClosedFileObserver closedFileObserver = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Initialise jump object
		mJump = new Jump();
		
		// Set panel object
		mPanel = (DrawPanel) this.findViewById(R.id.SurfaceView);
		mPanel.setDrawingCacheEnabled(true);
		// Initialise panel with the screen dimensions
		Display d = this.getWindowManager().getDefaultDisplay();
		mPanel.initialize(new Point(d.getWidth(), d.getHeight()));
				
		/*
		try
		{
			mDataManager.registerListener(this, ReconEvent.TYPE_JUMP);
		} 
		catch (InstantiationException e){ e.printStackTrace(); }
		*/

		// Register custom broadcast reciever for Jump Events
		mJumpEventReceiver = new JumpEventReciever();
		registerReceiver(mJumpEventReceiver, new IntentFilter("RECON_MOD_BROADCAST_JUMP"));
		
		closedFileObserver = new ClosedFileObserver(this);
	}
	
	@Override
	public void onAttachedToWindow() 
	{
	    super.onAttachedToWindow();
	    Window window = getWindow();
	    window.setFormat(PixelFormat.RGBA_8888);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// Unregister receivers
		//mDataManager.unregisterListener(ReconEvent.TYPE_JUMP);
		unregisterReceiver(mJumpEventReceiver);
	}

	// Custom broadcast receiver for Jump Events
	public class JumpEventReciever extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Extract bundle from intent.getExtra()
			Bundle bundle = intent.getBundleExtra("Bundle");
			
			// -- Changes between firmware have prompted me towards making this try-catch.
			// -- This is because the bundle received in the intent differs between firmware versions.
			try
			{
				// Set mJump to the newest jump received from the intents extra bundle
				ArrayList<Bundle> jumps = bundle.getParcelableArrayList("Jumps"); 
				mJump.setJump(jumps.get(jumps.size() - 1));
			}
			catch(Exception e)
			{
				// Set mJump to the newest jump received from the intents extra bundle
				mJump.setJump(bundle);
			}
			
			// -- TEMPORARY -- FOR SHOW PURPOSES -- //
			// -- Add a random distance value to the jump (between 2 - 5 meters)
			float dist = 2 + Util.roundTo1D((float)Math.random());
			mJump.mDistance = dist;
			
			// Set the panels jump object
			mPanel.setJump(mJump);
			
			// Kill animation thread as it could cause a shared resource issue with the draw canvas
			mPanel.killAnimationThread(); 
			
			// Generate the flight path
			mPanel.generateFlightPath();
			 
			// Save jump bitmap to file system as jpeg and Upload jump image to facebook via ReconHQ
			//mPanel.saveAndUploadFlightPath();
			closedFileObserver.setJump(mJump);
			closedFileObserver.startWatching();
			Util.saveBitmapToFile(mPanel.getJumpBitmap());
			
						
			// Begin animation
			//mPanel.beginAnimatingFlightPath();
			
			// Generates the graphical components of the jump and draws them to its bitmap object
			mPanel.generateBitmap();
			
			// Start the animation thread 
			mPanel.startAnimationThread();
			
			Log.i("JumpVis", "onRecieve()");  
		}
	}

	// --- CURRENTLY UNUSED EVENT HANDLER --- //
	@Override
	public void onDataChanged(ReconEvent event, Method m)
	{
		showToast("Event Received: " + event.toString());
		if(event.getType() == ReconEvent.TYPE_JUMP) { }
		
		Log.i("JumpVis", "onDataChanged()");
	}

	public void showToast(String msg)
	{
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}