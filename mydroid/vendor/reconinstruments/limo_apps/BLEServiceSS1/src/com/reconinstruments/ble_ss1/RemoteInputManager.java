//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.ble_ss1;
import android.os.Build;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.content.BroadcastReceiver;
import com.reconinstruments.utils.DeviceUtils;

public class RemoteInputManager {

    public static final String TAG = "RemoteInputManager";
        
    static final int  RECON_KEY_ENTER =  0x01;
    static final int  RECON_KEY_DOWN =  0x02;
    static final int  RECON_KEY_UP = 0x10;
    static final int  RECON_KEY_RIGHT = 0x20;
    static final int  RECON_KEY_LEFT = 0x08;
    static final int  RECON_KEY_CANCEL = 0x04;
    static final String BUTTON_HOLD_BEHAVIOUR =
        "com.reconinstruments.inputs.BUTTON_HOLD_BEAVIOUR_CHANGED";
    static final String SNOW_SELECT_HOLD_INTENT = "com.reconinstruments.quickactions.snow2";
    static final String SUN_SELECT_HOLD_INTENT = "com.reconinstruments.quickactions";
    static final String SNOW_BACK_HOLD_INTENT = ""; // Not used
    static final String SUN_BACK_HOLD_INTENT = "RECON_POWER_MENU";


    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
        
    Handler inputHandler;
    HandlerThread inputThread;
        
    private Instrumentation instru = new Instrumentation();
        
    // last attribute value for button presses sent by the remote
    int lastKeyVal;

    private boolean mShouldLaunchAppOnButtonHold = true;
    BroadcastReceiver mButtonHoldBehaviourReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                mShouldLaunchAppOnButtonHold = intent.getBooleanExtra("shouldLaunchApp",true);
            }
        };

    private Context mContext;
    public RemoteInputManager(Context owner) {
        this();
        mContext = owner;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BUTTON_HOLD_BEHAVIOUR);
        mContext.registerReceiver(mButtonHoldBehaviourReceiver, filter);
        //Log.d(TAG, "JET");
    }
    public RemoteInputManager(){
        //thread can't run on UI thread, hence the handler thread
        inputThread = new HandlerThread("inputHandlerThread");
        inputThread.start();
        inputHandler = new InputHandler(inputThread.getLooper());
    }

    public void onDestroy() {
        if (mContext != null) {
            mContext.unregisterReceiver(mButtonHoldBehaviourReceiver);
        }
    }

    private class InputHandler extends Handler {
        private boolean mIgnoreBackUp;
        private boolean mIgnoreCenterUp;
        public InputHandler(Looper looper) {
            super(looper);
        }

        private void launchApp(Intent i) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(i);
        }
        private void launchSelectLongPress() {
            Intent i = DeviceUtils.isSun()?
                new Intent(SUN_SELECT_HOLD_INTENT):
                new Intent(SNOW_SELECT_HOLD_INTENT);
            launchApp(i);
        }
        private void launchBackLongPress() {
            // We only invode back long hold for sun and not snow
            if (DeviceUtils.isSnow2()) return;
            Intent i = new Intent(SUN_BACK_HOLD_INTENT);
            launchApp(i);
        }
        /**
         * Special function to handle the weird key behaviour design
         * for JET
         * @param ke a <code>KeyEvent</code> value
         */
        private void handleSunKeyComplexity(KeyEvent ke) {
            int kcode = ke.getKeyCode();
            if (ke.getAction() == KeyEvent.ACTION_DOWN) { 
                if (ke.getRepeatCount() < 1) {
                    Log.v(TAG, "keyup Ignored for "+kcode);
                    return; // do nothing
                }
                removeMessages(dpadKeyMap(kcode));
                if (kcode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    mIgnoreCenterUp = true;
                    Log.v(TAG,"fire_quick_nav");
                    launchSelectLongPress();
                }
                else if (kcode == KeyEvent.KEYCODE_BACK) {
                    mIgnoreBackUp = true;
                    Log.v(TAG,"fire_power_menu");
                    launchBackLongPress();
                }
            } else if (ke.getAction() == KeyEvent.ACTION_UP) {
                Log.v(TAG,"keyup");
                // TODO: The logic can be simplified or tweaked. For
                // now good enough
                if (mIgnoreBackUp || mIgnoreCenterUp ) {
                    mIgnoreCenterUp = mIgnoreBackUp = false;
                    Log.v(TAG, "keyup Ignored for "+kcode);
                    return; // do nothing
                }
                instru.sendKeyDownUpSync(kcode);
            }
        }
        @Override
        public void handleMessage(Message msg) {
            synchronized(inputHandler){
                KeyEvent event = (KeyEvent)msg.obj;
                event = KeyEvent.changeTimeRepeat(event, SystemClock.uptimeMillis(),
                                                  event.getRepeatCount()+1);
                // in case on key up message never comes (disconnect?)
                // don't continue sending messages
                if((lastKeyVal&msg.what)!=0){
                    msg.obj = event;
                    sendMessageDelayed(Message.obtain(msg), 100);
                }
                Log.d(TAG, "key down: "+event.getKeyCode()+" repeat: "+
                      event.getRepeatCount());
                int keycode = event.getKeyCode();
                if (mShouldLaunchAppOnButtonHold &&
                    (keycode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     keycode == KeyEvent.KEYCODE_BACK)) {
                    handleSunKeyComplexity(event);
                } else {
                    instru.sendKeySync(event);
                }
            }
        }
    }
    static int remoteKeyMap(int key){
        switch(key){
        case RECON_KEY_ENTER:   return KeyEvent.KEYCODE_DPAD_CENTER;
        case RECON_KEY_DOWN:    return KeyEvent.KEYCODE_DPAD_DOWN;
        case RECON_KEY_UP:      return KeyEvent.KEYCODE_DPAD_UP;
        case RECON_KEY_RIGHT:   return KeyEvent.KEYCODE_DPAD_RIGHT;
        case RECON_KEY_LEFT:    return KeyEvent.KEYCODE_DPAD_LEFT;
        case RECON_KEY_CANCEL:  return KeyEvent.KEYCODE_BACK;
        }
        return -1;
    }
    static int dpadKeyMap(int key){
        switch(key){
        case KeyEvent.KEYCODE_DPAD_CENTER:      return RECON_KEY_ENTER;
        case KeyEvent.KEYCODE_DPAD_DOWN:        return RECON_KEY_DOWN;
        case KeyEvent.KEYCODE_DPAD_UP:          return RECON_KEY_UP;
        case KeyEvent.KEYCODE_DPAD_RIGHT:       return RECON_KEY_RIGHT;
        case KeyEvent.KEYCODE_DPAD_LEFT:        return RECON_KEY_LEFT;
        case KeyEvent.KEYCODE_BACK:             return RECON_KEY_CANCEL;
        }
        return -1;
    }
    void sendKeyEvent(int keyVal) {
        //int keyValDiff = keyVal | (lastKeyVal^keyVal); // check those bits that are true or have changed
        int keyValDiff = lastKeyVal^keyVal; // check only bits that have changed
        lastKeyVal = keyVal;

        // iterate over all recon key codes from RECON_KEY_ENTER thru RECON_KEY_RIGHT
        for(int RECON_KEY_CODE=RECON_KEY_ENTER;RECON_KEY_CODE<=RECON_KEY_RIGHT;
            RECON_KEY_CODE=RECON_KEY_CODE<<1) {
            if((RECON_KEY_CODE&keyValDiff)!=0){
                int keyCode = remoteKeyMap(RECON_KEY_CODE);
                if((RECON_KEY_CODE&keyVal)!=0){// key down event
                    Log.v(TAG, "key down: "+keyCode);
                    KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                                                  SystemClock.uptimeMillis(),
                                                  KeyEvent.ACTION_DOWN,keyCode,0);
                    if ((RECON_KEY_CODE != RECON_KEY_ENTER &&
                         RECON_KEY_CODE != RECON_KEY_CANCEL )||
                        !mShouldLaunchAppOnButtonHold) {
                        instru.sendKeySync(event);
                        // The events for these are triggered as <down,up> on <up>
                    }
                    synchronized(inputHandler){
                        Message msg = inputHandler.obtainMessage(RECON_KEY_CODE, event);
                        inputHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT);
                    }
                }
                else{// key up event
                    synchronized(inputHandler){
                        inputHandler.removeMessages(RECON_KEY_CODE);    
                        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                                                      SystemClock.uptimeMillis(),
                                                      KeyEvent.ACTION_UP,keyCode,0);
                        if (mShouldLaunchAppOnButtonHold &&
                            (RECON_KEY_CODE == RECON_KEY_ENTER ||
                             RECON_KEY_CODE == RECON_KEY_CANCEL)) {
                            Message msg = inputHandler.obtainMessage(RECON_KEY_CODE, event);
                            inputHandler.sendMessage(msg);
                        }
                        else {  // Deafault behaviour
                            instru.sendKeySync(event);
                        }
                    }

                }
            }
        }
    }
    public void onConnect() {
        lastKeyVal = 0;
    }
    public void onDisconnect() {
        if(lastKeyVal!=0){
            //send key up event for any keys that are currently down
            sendKeyEvent(0);
        }
    }
}
