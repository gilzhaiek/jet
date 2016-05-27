package com.reconinstruments.hudserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;

import com.reconinstruments.os.hardware.screen.HUDScreenManager;
import com.reconinstruments.os.hardware.screen.IHUDScreenService;
import com.reconinstruments.lib.hardware.HUDScreen;

class IHUDScreenServiceImpl extends IHUDScreenService.Stub {
    private static final String TAG = "IHUDScreenServiceImpl";
    private static final boolean DEBUG = false;
    private static final int SCREEN_ON_DELAY = 10;

    private final Context mContext;
    private final HUDScreen mHUDScreen;

    IHUDScreenServiceImpl(Context context) {
        this.mContext = context;
        this.mHUDScreen = new HUDScreen();

        IntentFilter filter = new IntentFilter();
        filter.addAction("glance_turn_on_screen");
        filter.addAction("turn_on_screen");
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        mContext.registerReceiver(mTurnOnScreenReceiver, filter);
    }

    public void screenOn(boolean onOff) {
        if (DEBUG) Log.d(TAG, "Turning screen: " + onOff);
        this.mHUDScreen.screenOn(onOff);
    }

    public void setScreenOffDelay(int delay) {
        if (DEBUG) Log.d(TAG, "Setting screen off delay: " + delay);
        this.mHUDScreen.setScreenOffDelay(delay);
    }

    public int getScreenState() {
        if (DEBUG) Log.d(TAG, "Retrieving screen state");
        return this.mHUDScreen.getScreenState();
    }

    public void setScreenState(int state) {
        this.mHUDScreen.setScreenState(state);
    }

    public void forceScreenOn(int delay, boolean stayOn) {
        if (DEBUG) Log.d(TAG, "Forcing screen on: " + delay + " stayOn: " + stayOn);
        // Get the current screen state. If it is already on, there's no point to force it on
        // again.
        int screenState = this.mHUDScreen.getScreenState();
        if (screenState == HUDScreenManager.SCREEN_STATE_ON) {
            Log.w(TAG, "Screen is already on. No need to force screen on");
        } else {
            this.mHUDScreen.forceScreenOn(delay, stayOn);
            // Send broadcast to inform Android's PowerManager that we have forced the screen on.
            Intent intent = new Intent("com.reconinstruments.hudscreen.FORCED_SCREEN_ON");
            mContext.sendBroadcast(intent);
        }
    }

    public void cancelForceScreen() {
        if (DEBUG) Log.d(TAG, "Cancel force screen on");
        this.mHUDScreen.cancelForceScreen();
    }

    BroadcastReceiver mTurnOnScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) Log.d(TAG, "Received intent: " + action);

            // Depending on the Intent action, we have to perform a number of things to
            // either perform operations on the screen or simply update the HUD screen state.
            if (action.equals("glance_turn_on_screen")) {
                // Received intent to turn on the screen for SCREEN_ON_DELAY seconds
                mHUDScreen.forceScreenOn(SCREEN_ON_DELAY, true);
            } else if (action.equals("turn_on_screen")) {
                // Received intent indicating that the user has pressed a button to wake
                // up the screen via Android's PowerManager. What we need to do is to cancel
                // the force turn on screen timer thread if it is running.
                mHUDScreen.cancelForceScreen();
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                // Android's PowerManager has finished turning on the screen. Update the state
                // within our HUD screen state.
                mHUDScreen.setScreenState(HUDScreenManager.SCREEN_STATE_ON);
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                // Android's PowerManager has finished turning off the screen. Update the state
                // within our HUD screen state.
                mHUDScreen.setScreenState(HUDScreenManager.SCREEN_STATE_POWER_OFF);
            }
        }
    };
}
