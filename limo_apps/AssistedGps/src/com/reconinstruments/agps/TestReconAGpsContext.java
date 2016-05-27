package com.reconinstruments.agps;
import android.content.Context;
import android.content.IntentFilter;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
/**
 *Developed to test ReconAGpsContext
 */
public class TestReconAGpsContext extends ReconAGpsContext {
    public TestReconAGpsContext(Context owner) {
	super(owner);
	mStateMachine = new TestStateMachine(this);
    }
    @Override
    public void initialize() {
	IntentFilter locFilter = new IntentFilter(ReconAGps.LOCATION_UPDATE_INTENT);
	mOwner.registerReceiver(mAgpsLocListener, locFilter);
	IntentFilter btxstatusFilter = new IntentFilter("HUD_STATE_CHANGED");
	mOwner.registerReceiver(mBtXListener, btxstatusFilter);
	mOnBoardLocListener.initialize();
    }
    public void cleanUp() {
	// The state machine
	mStateMachine.cleanUp();
	mOwner.unregisterReceiver(mBtXListener);
	mMockLocationProvider.shutdown();
	mOwner.unregisterReceiver(mAgpsLocListener);
    }
}
