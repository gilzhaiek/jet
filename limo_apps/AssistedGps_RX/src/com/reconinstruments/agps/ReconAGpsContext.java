package com.reconinstruments.agps;
import android.content.Context;
import android.content.IntentFilter;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
/**
 * <code>ReconAGpsContext</code> completely shields the application
 * developer from the details of having assisted Gps capabilites,
 * almanac fetching, phone connection and disconnection and all the
 * dirty things that come with it. To make this work an instance of
 * this class should be aggregated inside an Android
 * <code>Context</code> object, e.g. Service. After calling the
 * <code>initilize()</code> method on an instance of this class, AGps
 * will start working! On destroying the owner context,
 * <code>cleanUp()</code> method needs to be called.
 *
 */
public class ReconAGpsContext {
    Context mOwner;
    // Components:
    AndroidLocationListener mOnBoardLocListener;
    IncomingAGpsLocationListener mAgpsLocListener; // listens to incoming phone locs
    MockLocationProvider mMockLocationProvider; // pushes mock location
    AlmanacFetcher mAlmanacFetcher = null;	// WE don't use almanac fetcher for RX
    ToChipTalker mToChipTalker;	    // Talks to Jack's stuff
    BluetoothConnectionStatusListener mBtXListener; // monitors Bt connection status
    StateMachine mStateMachine; // This is the brain
    /**
     * Creates a new <code>ReconAGpsContext</code> instance.
     *
     * @param owner an Android <code>Context</code> 
     */
    public ReconAGpsContext(Context owner) {
	mOwner = owner;
	// Onboard
	mOnBoardLocListener = new AndroidLocationListener(this);
	// Phone Loc Listener:
	mAgpsLocListener = new IncomingAGpsLocationListener(this);
	// Mock Location pusher:
	mMockLocationProvider = new MockLocationProvider(this);
	// To Chip talker:
	mToChipTalker = new ToChipTalker(this);
	// Bluetooth state monitor
	mBtXListener = new BluetoothConnectionStatusListener(this);
	// The state machine:
	mStateMachine = new StateMachine(this);
    }
    /**
     * Gets the context (activity or service) that owns this module
     *
     * @return a <code>Context</code> value
     */
    public Context getContext() {
	return mOwner;
    }
    /**
     * Gets the machinary rolling. 
     *
     */
    public void initialize() {
	IntentFilter locFilter = new IntentFilter(ReconAGps.LOCATION_UPDATE_INTENT);
	mOwner.registerReceiver(mAgpsLocListener, locFilter);
	if (!mToChipTalker.initialize()) return; // Shit has hit the fan
	IntentFilter btxstatusFilter = new IntentFilter("HUD_STATE_CHANGED");
	mOwner.registerReceiver(mBtXListener, btxstatusFilter);
	mOnBoardLocListener.initialize();
	mStateMachine.initialize();
    }
    /**
     * Cleanups. Typically needs to be called in <code>onDestroy()</code>
     *
     */
    public void cleanUp() {
	// The state machine
	mStateMachine.cleanUp();
	// BtXListerner
	mOwner.unregisterReceiver(mBtXListener);
	// To Chip talker:
	if (mToChipTalker != null) mToChipTalker.cleanUp();
	// Mock Location pusher:
	mMockLocationProvider.shutdown();
	// Phone loc Listener: 
	mOwner.unregisterReceiver(mAgpsLocListener);
    }
    
	
}
