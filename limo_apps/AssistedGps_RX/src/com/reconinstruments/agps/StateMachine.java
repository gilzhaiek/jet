package com.reconinstruments.agps;
import java.util.concurrent.Semaphore;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import android.location.Location;
import android.util.Log;
import android.os.Handler;
import java.lang.Runnable;
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
    private static final String TAG = "StateMachine";
    private Handler delayHandler = new Handler();
    private Runnable askForPhoneLoc = new Runnable() {
	    @Override
	    public void run() {
		ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), 1);
	    }
	};
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
	WAITING_FOR_PHONE_LOCATION,
	    WAITING_FOR_ENOUGH_OWN_LOCATION,
	    DONE,
    }
    /**
     * Sets the initial state
     *
     */
    public void initialize() {
	try {
	    mSemaphore.acquire();
	    mCurrentState = State.WAITING_FOR_PHONE_LOCATION;
	    goToState(State.WAITING_FOR_PHONE_LOCATION);
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
	return;			// We don't need it for RX
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
		// Just insert the location don't care about the fact
		// the chip requires assistance or not. But Being in
		// WAITING_FOR_PHONE_LOCATION means that we don't
		// already have location. So it is not wasted effort.
		Log.v(TAG,"Waiting for Phone location inject pos");
		mOwner.mToChipTalker.prepareAndPushDataToChip(loc);
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
	default: break;
	}
	mCurrentState = state;
	// Only do things that are independent of the previous state
	switch (mCurrentState) {
	case WAITING_FOR_PHONE_LOCATION:
	    delayHandler.postDelayed(askForPhoneLoc,2000);
	    break;
	case DONE:
	    // Tell the phone to shutup:
	    delayHandler.removeCallbacks(askForPhoneLoc);
	    ReconAGps.sendUpdatePeriodToPhoneViaBroadcast(mOwner.getContext(), -1);
	    // Stop assistance
	    mOwner.mToChipTalker.cleanUp();
	default: break;
	}
    }
}
