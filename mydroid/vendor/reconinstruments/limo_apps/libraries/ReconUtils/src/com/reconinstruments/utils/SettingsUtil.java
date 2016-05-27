//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.utils;
import android.content.ContentResolver;
//taken from DashNotification
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.reconinstruments.utils.DeviceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Describe class <code>SettingsUtil</code> here.
 * 
 * This class is mostly used to fetch recon-specific and even
 * non-recon-specific fields from System settings provider. The major
 * function of this class is that it caches the setting values that it
 * reads from the system and unless those values are changed it uses
 * the cache. This cuts back on IPC and helps with battery
 * consumption. As long as you know the key for the setting item that
 * you are trying to manipulate, there are only Two APIs that you
 * really need. <code>setSystemInt</code> to set a field. and
 * <code>getCachableSystemIntOrSet</code> to read the field. You can
 * call the latter function as often as you want. As long as the value
 * is not changed it does not invoke any IPC on subsequent calls to
 * get the value associated a field.
 */
public class SettingsUtil {
    static private final String TAG = SettingsUtil.class.getSimpleName();
    static public final int RECON_UNITS_METRIC = 0;
    static public final int RECON_UNITS_IMPERIAL = 1;
    static public final int RECON_UINTS_METRIC = 0;
    static public final int RECON_UINTS_IMPERIAL = 1;
    static private ContentResolver sResolver = null;
  
    /**
     * Utility function for checking the unit settings
     * return 0 for Metric; 1 for Imperial
     */
    static public int getUnits( Context context ) {
        return getCachableSystemIntOrSet(context,
                                         RECON_UNIT_SETTING,
                                         RECON_UNITS_METRIC);
    }

    static public boolean isMetric(Context context) {
        return getUnits(context) == RECON_UINTS_METRIC;
    }

    /**
     * Set the Units setting: 0 for Metric; Non-zero for Imperial
     */
    static public void setUnits( Context context, int setting ){
        setSystemInt(context,RECON_UNIT_SETTING,setting);
    }
    /** The WiLink is in ANT mode. */
    public static final int LOW_POWER_WIRELESS_MODE_ANT = 0;
    /** The WiLink is in BLE mode. */
    public static final int LOW_POWER_WIRELESS_MODE_BLE = 1;

    private static final String RADIO_MODE_BLE = "mode=BLE";
    private static final String RADIO_MODE_ANT = "mode=ANT";
    private static final File RADIO_CONFIG_FILE = new File("/data/misc/recon/BT.conf");

    /** The key for reading the low power wireless mode setting from
     * 'Settings.Secure'. */
    public static final String LOW_POWER_WIRELESS_MODE = "low_power_wireless_mode";
    static public int getBleOrAnt() {
        if (DeviceUtils.isSnow2()) return LOW_POWER_WIRELESS_MODE_BLE;
        String firstLine = null;
        BufferedReader reader = null;
        int mode = LOW_POWER_WIRELESS_MODE_BLE;
        try {
            reader = new BufferedReader(new FileReader(RADIO_CONFIG_FILE));
            firstLine = reader.readLine();
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read from " + RADIO_CONFIG_FILE);
            Log.e(TAG, "Exception: " + e);
        }

        if (firstLine != null){
            if (firstLine.contains(RADIO_MODE_ANT)) {
                mode = LOW_POWER_WIRELESS_MODE_ANT;
            }
        }
        return mode;
    }

    /**
     * <code>getSystemIntOrSet</code> Reads an int from
     * Settings.System if it doesn't exist creates it with the
     * provided default value
     *
     * @param context a <code>Context</code> value
     * @param field a <code>String</code> value for the field name
     * @param dflt an <code>int</code> value for the default value
     * @return an <code>int</code> value
     */
    public static final int getSystemIntOrSet (Context context, String field, int dflt) {
        ContentResolver cr = context.getContentResolver();
        int value = dflt;
        try {
            value = Settings.System.getInt(cr, field);
        } catch( Settings.SettingNotFoundException e ){
            Settings.System.putInt(cr,field,dflt);
            Log.v("Util","There is no field "+field);
        }
        return value;
    }

    /**
     * Describe <code>setSystemInt</code> method here.
     * Sets the value for a system settings
     *
     * @param context a <code>Context</code> value
     * @param field a <code>String</code> value
     * @param value an <code>int</code> value
     */
    public static final void setSystemInt (Context context, String field, int value) {
        Settings.System.putInt(context.getContentResolver(), field, value);
    }


    /**
     * <code>getSecureIntOrSet</code> Reads an int from
     * Settings.Secure if it doesn't exist creates it with the
     * provided default value
     *
     * @param context a <code>Context</code> value
     * @param field a <code>String</code> value for the field name
     * @param dflt an <code>int</code> value for the default value
     * @return an <code>int</code> value
     */
    public static final int getSecureIntOrSet (Context context, String field, int dflt) {
        ContentResolver cr = context.getContentResolver();
        int value = dflt;
        try {
            value = Settings.Secure.getInt(cr, field);
        } catch( Settings.SettingNotFoundException e ){
            Settings.Secure.putInt(cr,field,dflt);
            Log.v("Util","There is no field "+field);
        }
        return value;
    }

    /**
     * Describe <code>setSecurent</code> method here.
     * Sets the value for a secure system settings
     *
     * @param context a <code>Context</code> value
     * @param field a <code>String</code> value
     * @param value an <code>int</code> value
     */
    public static final void setSecureInt (Context context, String field, int value) {
        Settings.Secure.putInt(context.getContentResolver(), field, value);
    }




    /////////////////////////////////////////////////////////////////////////////
    // Generic getter for everything
    private static Map<String, Integer> map = new HashMap<String, Integer>();
    private static Map<Uri,String> uriToString = new HashMap<Uri,String>();

    /**
     * <code>getCachableSystemIntOrSet</code> Reads an int value
     * associated with a key from the system settings provider. If the
     * key doesn't exist it creats it with the default value
     * provided. In subsequent calls it uses a caches value to read
     * this so that it doesn't need to call any IPC. At the same time
     * it has an observer and if the value changes it updates the
     * cache
     *
     * @param context a <code>Context</code> value
     * @param field a <code>String</code> value. The key
     * @param dflt an <code>int</code> value the default value
     * @return an <code>int</code> value the read value
     */
    public static final int getCachableSystemIntOrSet (Context context,
                                                         String field,
                                                         int dflt) {
        Integer result = map.get(field);
        if (result == null) {
            //Log.v(TAG,"using system provider");
            int value = getSystemIntOrSet(context,field,dflt);
            map.put(field,value);
            registerObserver(context,field);
            return value;
        } else {
            //Log.v(TAG,"using the map");
            return result.intValue();
        }

    }

    static private GenericSettingObserver sGenericObserver = null;
    static class GenericSettingObserver extends ContentObserver {
        public GenericSettingObserver(Handler handler) {
            super(handler);
        }
        @Override
        public void onChange(boolean selfChange) {
            Log.v(TAG,"onChange(boolean self) should not be called");
        }
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            try {
                String fieldName = uriToString.get(uri);
                //Log.v(TAG,"fieldname is "+fieldName);
                //Log.v(TAG,"What is happening?");
                int val = Settings.System.getInt(sResolver,fieldName);
                //Log.v(TAG,"What is happening here");
                //Log.v(TAG,"val is "+val);
                map.put(fieldName,val);
            } catch (SettingNotFoundException e) {
                e.printStackTrace();
                Log.v(TAG,"Couldn't load");
            }
        }
    }
    static private void registerObserver(Context context, String fieldName) {
        //Log.v(TAG,"register Observer");
        if (sGenericObserver == null) { // For Next time, we will catch any update
            sGenericObserver = new GenericSettingObserver(null);
            //Log.v(TAG,"observer created");
        }
        if (sResolver == null) sResolver = context.getContentResolver();
        Uri uri= Settings.System.getUriFor(fieldName);
        uriToString.put(uri,fieldName);
        sResolver.registerContentObserver(uri,false, sGenericObserver);
    }

    // Fields
    public static final String SHOULD_AUTO_ROTATE = "com.reconinstruments.settings.SHOULD_AUTO_ROTATE";
    public static final int AUTO_ROTATE_PERIOD = 10;
    public static final int SHOULD_AUTO_ROTATE_DEFAULT = 0; // No auto rotate
    public static final String SHOULD_NOTIFICATION_SOUNDS = "com.reconinstruments.settings.SHOULD_NOTIFICATION_SOUNDS";
    public static final String RECON_UNIT_SETTING = "ReconUnitSetting";
    @Deprecated
    public static final String UNIT_SETTING = "ReconUnitSetting"; // deprecated
    public static final String SHOULD_NOTIFY_CALLS = "RECON_SHOULD_NOTIFY_CALLS";
    public static final String SHOULD_NOTIFY_SMS = "RECON_SHOULD_NOTIFY_SMS";

    // sync time with smartphone variables
    static public final String RECON_SYNC_TIME_SMARTPHONE = "ReconSyncTimeWithSmartPhone";
    static public final int RECON_SYNC_TIME_OFF = 0;
    static public final int RECON_SYNC_TIME_ON = 1;
    static public final int RECON_DEFAULT_SYNC_TIME = RECON_SYNC_TIME_ON;

    // auto time variables
    static public final String RECON_SET_TIME_GPS = "ReconSetTimeGPS";
    static public final int RECON_SET_TIME_GPS_OFF = 0;
    static public final int RECON_SET_TIME_GPS_ON = 1;
    static public final int RECON_SET_TIME_GPS_DEFAULT = 1 - RECON_DEFAULT_SYNC_TIME ;
    // ^Default value is opposite of sync itime

    // video record duration
    static public final int DEFAULT_VIDEO_RECORD_DURATION = 15; // seconds
    static public final String VIDEO_RECORD_DURATION = "com.reconinstruments.settings.VIDEO_RECORD_DURATION";

    static public final String PASSCODE_ENABLED = "com.reconinstruments.settings.PASSCODE_ENABLED";
    static public final String PASSCODE_IN_LOCK = "com.reconinstruments.settings.PASSCODE_IN_LOCK";
    static public final String ANALYTICS_ENABLED = "com.reconinstruments.settings.ANALYTICS_ENABLED";
    static public final int ANALYTICS_ENABLED_DEFAULT = 1; // enabled by default




    static public void setSyncTimeWithSmartPhone( Context context, boolean syncTimeOn){
        SettingsUtil.setSystemInt (context,
                SettingsUtil.RECON_SYNC_TIME_SMARTPHONE,
                syncTimeOn?
                        SettingsUtil.RECON_SYNC_TIME_ON :
                        SettingsUtil.RECON_SYNC_TIME_OFF);
        if(syncTimeOn){
            setTimeAuto(context, false);
        }
    }

    static public boolean getSyncTimeWithSmartPhone( Context context){
        return (SettingsUtil.getCachableSystemIntOrSet(context,
                SettingsUtil.RECON_SYNC_TIME_SMARTPHONE,
                SettingsUtil.RECON_DEFAULT_SYNC_TIME)
                == SettingsUtil.RECON_SYNC_TIME_ON);
    }

    /*
     * Set time automatically
     */
    static public void setTimeAuto( Context context, boolean useGPSTime) {
        SettingsUtil.setSystemInt(context,SettingsUtil.RECON_SET_TIME_GPS,
                useGPSTime ? SettingsUtil.RECON_SET_TIME_GPS_ON :
                        SettingsUtil.RECON_SET_TIME_GPS_OFF);
    }

    /*
     * Is time automatically set by GPS?
     */
    static public boolean getTimeAuto( Context context) {
        return (SettingsUtil.getCachableSystemIntOrSet(context,
                SettingsUtil.RECON_SET_TIME_GPS,
                SettingsUtil.RECON_SET_TIME_GPS_DEFAULT)
                == SettingsUtil.RECON_SET_TIME_GPS_ON);
    }
}
