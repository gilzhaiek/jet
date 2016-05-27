package com.reconinstruments.agps;
import java.util.concurrent.Semaphore;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import android.location.Location;
import android.widget.Toast;
/**
 * <code>TestStateMachine</code> is the test version of the state
 * machine
 *
 */
class TestStateMachine extends StateMachine{
    ReconAGpsContext mOwner;
    protected State mCurrentState;
    protected final Semaphore mSemaphore = new Semaphore(1,true);
    public TestStateMachine(ReconAGpsContext owner) {
	mOwner = owner;
	initialize();
    }
    public State getCurrentState() {
	return mCurrentState;
    }
    public void initialize() {
	try {
	    mSemaphore.acquire();
	    mCurrentState = State.WAITING_FOR_ASSIST_REQUEST;
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    @Override
    public void almanacUpdated() {
	try {
	    mSemaphore.acquire();
	    switch(mCurrentState) {
	    case WAITING_FOR_ALMANAC:
		goToState(State.WAITING_FOR_PHONE_LOCATION);
		break;
	    default:
		// do nothing
	    }
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    public void bluetoothStateChanged(int btStatus) {
	if (btStatus!=2) return;	// only interested in connected bt state
	try {
	    mSemaphore.acquire();
	    // only care when connected
	    switch (mCurrentState) {
	    case WAITING_FOR_PHONE_LOCATION:
		goToState(State.WAITING_FOR_PHONE_LOCATION);
		// so that request is sent again
		break;
	    case WAITING_FOR_ALMANAC:
		goToState(State.WAITING_FOR_ALMANAC);
		break;
	    }
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    public void gpsChipStateChanged(int status) {
	try {
	    mSemaphore.acquire();
	    if (status == 1) {
		goToState(State.WAITING_FOR_ALMANAC);
	    }
	    else {		// doesn't need assistance
		goToState(State.WAITING_FOR_ENOUGH_OWN_LOCATION);
	    }
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    public void haveGoodOwnGps() {
	try {
	    mSemaphore.acquire();
	    switch (mCurrentState) {
	    case DONE:
		break;
	    case WAITING_FOR_ENOUGH_OWN_LOCATION:
		//TODO if enough own location
		// For now one location point is enough
		goToState(State.DONE);
		break;
	    default:
		goToState(State.WAITING_FOR_ENOUGH_OWN_LOCATION);
	    }
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    public void phoneGpsReceived(Location loc) {
	try {
	    mSemaphore.acquire();
	    switch(mCurrentState) {
	    case DONE:
		toast("ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), -1)");
		ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), -1);
		break;
	    case WAITING_FOR_PHONE_LOCATION:
		toast("mOwner.mToChipTalker.prepareAndPushDataToChip(loc);");
		mOwner.mToChipTalker.prepareAndPushDataToChip(loc);
		goToState(State.WAITING_FOR_ENOUGH_OWN_LOCATION);
	    default:
		toast("push Location");
		//mOwner.mMockLocationProvider.pushLocation();
		break;
	    }
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    public void cleanUp() {
	
    }
    private void goToState(State state) {
	switch (mCurrentState) {
	case WAITING_FOR_ALMANAC:
	    mOwner.mAlmanacFetcher.setKeepRetrying(false);
	    break;
	default: break;
	}

	toast("new State: "+state.toString());
	mCurrentState = state;
	switch (mCurrentState) {
	case WAITING_FOR_ALMANAC:
	    toast("mOwner.mAlmanacFetcher.fetchAlmanacFile()");
	    mOwner.mAlmanacFetcher.setKeepRetrying(true); // keeps trying for almanacs
	    mOwner.mAlmanacFetcher.fetchAlmanacFile();
	    break;
	case WAITING_FOR_PHONE_LOCATION:
	    toast("ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner, 1)");
	    ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), 1);
	    break;
	case DONE:
	    toast("ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner, -1)");
	    ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), -1);
	default: break;
	}
    }
    private void toast(String s) {
	Toast.makeText(mOwner.getContext(), s, Toast.LENGTH_SHORT).show();	
    }
	    

}