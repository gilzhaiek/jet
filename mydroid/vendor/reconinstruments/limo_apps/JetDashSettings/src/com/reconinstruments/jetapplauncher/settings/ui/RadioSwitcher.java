//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.jetapplauncher.settings.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;

/**
 * 
 * Custom Switcher View to handle Radio Type change
 * 
 * @author patrickcho
 * @since 2014.03.04
 * @version 1<br>
 *          1 -> Initial Commit
 */
public class RadioSwitcher extends Switch implements OnRadioSwitchListener {
    private static final String TAG = RadioSwitcher.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final File RADIO_CONFIG_DIR = new File("/data/misc/recon/");
    public static final File RADIO_CONFIG_FILE = new File(
                                                          "/data/misc/recon/BT.conf");
    public static final String RADIO_MODE_BLE = "mode=BLE";
    public static final String RADIO_MODE_ANT = "mode=ANT";

    private static final String ON_TEXT = "BLE";
    private static final String OFF_TEXT = "ANT";
    private Context mContext = null;
    private boolean mIsBLE;

    private OnRadioSwitchListener mOnRadioSwitchListener = this;

    /**
     * Listener for check changed event
     */
    private final OnCheckedChangeListener mOnCheckedChangedListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                mIsBLE = isChecked;
                mOnRadioSwitchListener.onChange(mIsBLE);
            }
        };

    public RadioSwitcher(Context context) {
        super(context);
        init(context);
    }

    public RadioSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RadioSwitcher(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        checkAndCreateConf();
        mContext = context;
        mIsBLE = (SettingsUtil.getBleOrAnt() == SettingsUtil.LOW_POWER_WIRELESS_MODE_BLE);
        // this is placed here to avoid the mOnCheckedChangedListener to be called
        // when custom behaviour is set
        this.setChecked(mIsBLE);
                
        this.setOnCheckedChangeListener(mOnCheckedChangedListener);
                
        this.setTextOn(ON_TEXT);
        this.setTextOff(OFF_TEXT);
                
    }

    @Override
    public void onChange(boolean isBLE) {
        //Log.v(TAG,"isBLE is "+isBLE);
        if (mIsBLE) { // BLE
            // WRITE BLE TO THE CONFIG FILE
            writeBleToConfig();
        } else { // ANT
            // WRITE ANT TO THE CONFIG FILE
            writeAntToConfig();
        }
	//Log.v(TAG,"BLE or Ant immediate check"+SettingsUtil.getBleOrAnt(mContext));
                
        this.setClickable(false);
                
        /**
         * TODO WAITING FOR SPEC FOR BETTER USABILITY
         */
        getHandler().postDelayed(new Runnable() {               
                @Override
                public void run() {
                    if (mIsBLE) {
                        // Send intent to disconnect all ANT+ sensors
                        Intent intent = new Intent("RECON_ANT_SERVICE");
                        intent.putExtra("disconnect_all", true);
                        mContext.startService(intent);

                        // Start the BLEService
                        mContext.startService(new Intent("RECON_THE_BLE_SERVICE"));
                    } else {
                        // Stop the BLEService
                        mContext.stopService(new Intent("RECON_THE_BLE_SERVICE"));

                        // Send intent to reconnect all ANT+ sensors
                        mContext.startService(new Intent("RECON_ANT_SERVICE"));
                    }
                }
            }, 300);
    }

    /**
     * Set the new listener for new behaviour for custom radio change
     * 
     * @param listener
     */
    public final void setOnRadioSwitchListener(OnRadioSwitchListener listener) {
        mOnRadioSwitchListener = listener;
    }

    private static final void writeAntToConfig() {
        log("writing ant");
        writeToConfig(false);
    }

    private static final void writeBleToConfig() {
        log("writing ble");
        writeToConfig(true);
    }

    private static final synchronized void writeToConfig(boolean isBLE) {
        try {
            FileWriter writer = new FileWriter(RADIO_CONFIG_FILE, false);
            writer.write(isBLE ? RADIO_MODE_BLE : RADIO_MODE_ANT);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final void checkAndCreateConf() {
        log(RADIO_CONFIG_FILE.getPath() + " exists? "
            + RADIO_CONFIG_FILE.exists());

        if (!RADIO_CONFIG_DIR.exists()) {
            RADIO_CONFIG_DIR.mkdirs();
        }

        if (!RADIO_CONFIG_FILE.exists()) {
            try {
                RADIO_CONFIG_FILE.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * checks the radio status and return the mode
     * 
     * if file does not exist -> set to ble mode
     * 
     * @return isBLE
     */
    private static final boolean getRadioStatus() {
        String firstLine = RADIO_MODE_BLE; // fallback is BLE mode
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(RADIO_CONFIG_FILE));
            firstLine = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (firstLine == null){
            return true;
        }
                
        return (firstLine.contains(RADIO_MODE_BLE));
    }

    private static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
        
    private static void reboot(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot("BegForShutdown");
        //              pm.reboot(null);
    }

}
