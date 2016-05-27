package com.reconinstruments.agps;
import java.util.concurrent.Semaphore;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import android.location.Location;
/**
 * <code>StateMachine</code> implements the sequence diagram
 * for the assisted gps described at
 * https://github.com/ReconInstruments/Android_MobileSDK/wiki/agps
 *
 */
class StateMachine {
    public StateMachine() {
    }
    ReconAGpsContext mOwner;
    private State mCurrentState;
    // So that two things cannot compete for change of state:
    private final Semaphore mSemaphore = new Semaphore(1,true);
    /**
     * Creates a new <code>StateMachine</code> instance.
     *
     * @param owner an <code>ReconAGpsContext</code> 
     */
    public StateMachine(ReconAGpsContext owner) {
	mOwner = owner;
    }
    /**
     * getter for the current State of the machine
     *
     * @return a <code>State</code> value
     */
    public State getCurrentState() {
	return mCurrentState;
    }
    public static enum State  {
	WAITING_FOR_ASSIST_REQUEST, WAITING_FOR_ALMANAC,
	    WAITING_FOR_PHONE_LOCATION, WAITING_FOR_ENOUGH_OWN_LOCATION,
	    DONE;
    }
    /**
     * Sets the initial state
     *
     */
    public void initialize() {
	try {
	    mSemaphore.acquire();
	    mCurrentState = State.WAITING_FOR_ASSIST_REQUEST;
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    /**
     * Signifies the event that the almanac file is updated. Typically
     * called by <code>AlmanacFetcher</code>
     *
     */
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
    /**
     * Signifies the event that Bluetotoh connection to phone has
     * changed. Typically called by
     * <code>BluetoothConnectionStatusListener</code>
     *
     * @param btStatus an <code>int</code> value
     */
    public void bluetoothStateChanged(int btStatus) {
	try {
	    mSemaphore.acquire();
	    if (btStatus!=2) {
		mSemaphore.release();
		return;	// only interested in connected bt state
	    }
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
    /**
     * Signifies the event that the gps Chip state has changed. From
     * this we can infer if the chip still requires assistance or
     * not. Typically called by <code>ToChipTalker</code>
     *
     * @param status an <code>int</code> value
     */
    public void gpsChipStateChanged(int status) {
	try {
	    mSemaphore.acquire();
	    if (mOwner.mToChipTalker.chipNeedsAssistance()) {
		goToState(State.WAITING_FOR_ALMANAC);
	    }
	    else {		// doesn't need assistance
		goToState(State.WAITING_FOR_ENOUGH_OWN_LOCATION);
	    }
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    /**
     * Signifies the event that HUD has received a streak of good gps
     * location from the chip. Typically called by
     * <code>AndroidLocationListerner</code>
     * 
     */
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
    /**
     * Signifies the event that location data was received from the
     * phone. Typically called by
     * <code>IncomingAGpsLocationListener</code>
     *
     * @param loc the lastest <code>Location</code> received form the
     * phone
     */
    public void phoneGpsReceived(Location loc) {
	try {
	    mSemaphore.acquire();
	    switch(mCurrentState) {
	    case DONE:
		ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), -1);
		break;
	    case WAITING_FOR_PHONE_LOCATION:
		// TODO make sure that the "if condition" is required
		// and we can't just blindly push the data
		if (mOwner.mToChipTalker.chipNeedsAssistance()) {
		    mOwner.mToChipTalker.prepareAndPushDataToChip(loc);
		}
		goToState(State.WAITING_FOR_ENOUGH_OWN_LOCATION);
	    default:
		mOwner.mMockLocationProvider.pushLocation();
		break;
	    }
	    mSemaphore.release();
	} catch (InterruptedException e) {
	}
    }
    /**
     * cleanup before shutdown.
     *
     */
    public void cleanUp() {
    }
    private void goToState(State state) {
	// Cleanup upon leaving the state
	switch (mCurrentState) {
	case WAITING_FOR_ALMANAC:
	    mOwner.mAlmanacFetcher.setKeepRetrying(false);
	    break;
	default: break;
	}
	mCurrentState = state;
	// Only do things that are independent of the previous state
	switch (mCurrentState) {
	case WAITING_FOR_ALMANAC:
	    mOwner.mAlmanacFetcher.setKeepRetrying(true); // keeps trying for almanacs 
	    mOwner.mAlmanacFetcher.fetchAlmanacFile();
	    break;
	case WAITING_FOR_PHONE_LOCATION:
	    ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), 1);
	    break;
	case DONE:
	    // Tell the phone to shutup:
	    ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), -1);
	default: break;
	}
    }
}