package com.reconinstruments.battery.logger;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.provider.Settings;
import android.util.Log;
import android.os.Build;

public class BatteryLogBootReceiver extends BroadcastReceiver {

    public static final String TAG = BatteryLogBootReceiver.class.getSimpleName();
    public static final String LOG_ENABLED =
            "com.reconinstruments.battery.logger.LOG_ENABLED";
    public static final String LOG_MODE =
            "com.reconinstruments.battery.logger.LOG_MODE";
    public static final String LOG_INTERVAL =
            "com.reconinstruments.battery.logger.LOG_INTERVAL";
    public static final int NORMAL_MODE = 0;
    public static final int POLLING_MODE = 1;
    public static final int DEFAULT_LOG_BEHAVIOUR = 0; // Set to 1 to enable by default
    //static final boolean isModelJet = Build.MODEL.equalsIgnoreCase("JET") ? true : false;

    @Override
    public void onReceive(Context context, Intent intent) {
        // just make sure we are getting the right intent (better safe than sorry)
        if(!intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.e(TAG,"Boot not complete");
            return;
        }

        int logEnabled = 0;
        int logMode = NORMAL_MODE;
        int logInterval = 100;//By defaul using 100 second
        // If the flag is enabled
        try {
            logEnabled = Settings.System.getInt( context.getContentResolver(),
                    LOG_ENABLED);
            logMode = Settings.System.getInt( context.getContentResolver(),
                    LOG_MODE);
            logInterval = Settings.System.getInt( context.getContentResolver(),
                    LOG_INTERVAL);

        } catch( Settings.SettingNotFoundException e ){
            Log.d(TAG, "intent not found");
            //By default we will disable log for both snow and jet
            /*
            if(isModelJet==true)
                logEnabled= 1;
            else
            */
            logEnabled= DEFAULT_LOG_BEHAVIOUR;
        }

        if (logEnabled == 0) {
            Log.d(TAG,"Log disabled");
            return;
        }

        // Now we can continue
        ComponentName comp = new ComponentName(context.getPackageName(), BatteryLogService.class.getName());

        Intent i = new Intent().setComponent(comp);
        i.putExtra("logMode",logMode);
        i.putExtra("logInterval",logInterval);

        ComponentName service = context.startService(i);
        if (service == null){
            // something really wrong here
            Log.e(TAG, "Could not start service " + comp.toString());
        }
    }
}

