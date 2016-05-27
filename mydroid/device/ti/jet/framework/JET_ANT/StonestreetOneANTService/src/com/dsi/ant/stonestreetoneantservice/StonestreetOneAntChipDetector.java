package com.dsi.ant.stonestreetoneantservice;

import android.util.Log;

import com.dsi.ant.chip.AntChipBase;
import com.dsi.ant.chip.AntChipDetectorBase;
import com.dsi.ant.chip.IAntChipDetectorEventReceiver;

/**
 * Provides our chip reference to the ANT Radio Service when asked to start detecting chips.
 */
public class StonestreetOneAntChipDetector extends AntChipDetectorBase {
    public static final String TAG = StonestreetOneAntChipDetector.class.getSimpleName();
    public static final boolean DEBUG = false;

    /** There is always one (and only one) built-in chip we are dealing with. */
    private final StonestreetOneAntChip mAntChip;

    /** Sends chip added/removed notifications to the ANT Radio Service. */ 
    private IAntChipDetectorEventReceiver mEventReceiver = null;

    /** Are we currently "detecting chips" */
    private boolean mDoneChipAdded = false;

    /** There will never be any "new" chips, so this is returned by any scan request */
    private static final StonestreetOneAntChip NO_NEW_CHIPS[] = new StonestreetOneAntChip[0];

    /**
     * Creates a new SS1 ANT chip detector.
     * 
     * @param isLowPowerWirelessModeAnt The initial WiLink mode.
     */
    public StonestreetOneAntChipDetector(boolean isLowPowerWirelessModeAnt) {
        mAntChip = new StonestreetOneAntChip(isLowPowerWirelessModeAnt);
    }

    @Override
    public synchronized void start(IAntChipDetectorEventReceiver eventReceiver) {
        if(DEBUG) Log.v(TAG, "start");

        mEventReceiver = eventReceiver;

        if(!mDoneChipAdded) {
            // There is always a single chip
            if(null != eventReceiver) {
                eventReceiver.chipAdded(mAntChip);
                mDoneChipAdded = true;
            }
        }
    }

    @Override
    public synchronized void stop() {
        if(DEBUG) Log.v(TAG, "stop");

        // Nothing to do as we are not actually "detecting chips".
    }

    @Override
    public synchronized AntChipBase[] scanForNewChips() {
        if(DEBUG) Log.v(TAG, "scanForNewChips");

        if(!mDoneChipAdded) {
            mDoneChipAdded = true;

            return new AntChipBase[]{mAntChip};
        } else {
            return NO_NEW_CHIPS;
        }
    }

    @Override
    public synchronized void destroy() {
        if(DEBUG) Log.v(TAG, "destroy");

        // Service is probably being destroyed, so chip can't be used.
        mAntChip.setEventReceiver(null);
        if(mDoneChipAdded) {
            if(null != mEventReceiver) {
                mEventReceiver.chipRemoved(mAntChip);
            }

            mDoneChipAdded = false;
        }

        // To be safe, make sure the chip is disabled.
        mAntChip.disable();

        mAntChip.destroy();

        super.destroy();
    }

    /**
     * Notify the chip that the WiLink has changed mode.
     * 
     * @param isAnt true if the new setting is 'ANT mode'.
     */
    public void onIsLowPowerWirelessModeAntChange(boolean isAnt) {
        if(DEBUG) Log.v(TAG, "onIsLowPowerWirelessModeAntChange: "+ isAnt);

        mAntChip.onIsLowPowerWirelessModeAntChange(isAnt);
    }

}
