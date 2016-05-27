package com.contour.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.contour.connect.R;
import com.contour.connect.debug.CLog;

public class SharedPrefHelper {

    public static final boolean D = false;
    private static final String CONTOUR_CAMERA_VALS = "com.contour.cameravals";
    public static final String FW_KEY = "fwversionkey";

    public static void registerCallback(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        context.getSharedPreferences(CONTOUR_CAMERA_VALS, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterCallback(Context context, 
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        context.getSharedPreferences(CONTOUR_CAMERA_VALS, Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(listener);

    }
    
    public static void saveDeviceAddress(Context context, String btAddress, String cameraModel) {
        if(D) CLog.out("Saving Device",btAddress,cameraModel);

        SharedPreferences settings = context.getSharedPreferences(CONTOUR_CAMERA_VALS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(btAddress, cameraModel);
        
        boolean commited = editor.commit();
        if(!commited)  {
            Log.wtf(CLog.TAG, "Shared pref editor couldn't commit firmware version!");
        }
    }
    
    public static String checkDeviceAddress(Context context, String btAddress) {
        SharedPreferences settings = context.getSharedPreferences(CONTOUR_CAMERA_VALS, 0);
        return settings.getString(btAddress, null);
    }
    
    public static void saveFwVersion(Context context, int deviceModel, int major, int minor, int build) {
        int fwVersionVal = (deviceModel << 24) | (major << 16) | (minor << 8) | (build & 0xFF);
        if(D) CLog.out("Saving FW Version",deviceModel,major,minor,build,fwVersionVal);

        SharedPreferences settings = context.getSharedPreferences(CONTOUR_CAMERA_VALS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(FW_KEY).commit();
        
        boolean commited = editor.putInt(FW_KEY, fwVersionVal).commit();
        if(!commited)  {
            Log.wtf(CLog.TAG, "Shared pref editor couldn't commit firmware version!");
        }
    }
    
    public static String getFwVersionString(Context context) {
        SharedPreferences settings = context.getSharedPreferences(CONTOUR_CAMERA_VALS, 0);
        
        int fwVersionVal = settings.getInt(FW_KEY, 0);
        if(fwVersionVal == 0) return context.getString(R.string.statusmsgunknown);
        int deviceModel = (fwVersionVal >> 24) & 0xFF;
        int major = (fwVersionVal >> 16) & 0xFF;
        int minor = (fwVersionVal >> 8) & 0xFF;
        int build = fwVersionVal & 0xFF;
        return new StringBuilder().append(major).append('.').append(minor).append('.').append(build).toString();
    }
    
    public static String getDeviceModelString(Context context) {    
        SharedPreferences settings = context.getSharedPreferences(CONTOUR_CAMERA_VALS, 0);
        int fwVersionVal = settings.getInt(FW_KEY, 0);
        if(fwVersionVal == 0) return context.getString(R.string.contourcamera);
        int deviceModel = (fwVersionVal >> 24) & 0xFF;
        return ContourUtils.getCameraModel(context, deviceModel);
    }
    
    public static int getFwVersionVal(Context context) {
        SharedPreferences settings = context.getSharedPreferences(CONTOUR_CAMERA_VALS, 0);
       return settings.getInt(FW_KEY, 0);
    }
}
