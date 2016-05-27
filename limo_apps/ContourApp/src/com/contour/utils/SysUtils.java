package com.contour.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Debug;
import android.os.Environment;
import android.util.DisplayMetrics;

public class SysUtils
{
    public static long getMemoryConsumption()
    {
        return Debug.getNativeHeapAllocatedSize() / 1048576L;
    }
    
    public static String getAppVersion(Context pContext)
    {
        try
        {
            PackageInfo packageInfo = pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), PackageManager.GET_META_DATA);
            return packageInfo.versionName;
        } catch (NameNotFoundException e)
        {
            e.printStackTrace();
            return null; 
        }
    }
    
    public static double getScreenSizeInches(Context pContext) {
        DisplayMetrics dm = pContext.getResources().getDisplayMetrics();
        double screenInches = Math.sqrt( Math.pow(dm.widthPixels/dm.xdpi,2) + Math.pow(dm.heightPixels/dm.ydpi,2));
        return Math.round(screenInches*10)/10.0;
    }
    
    public static String getScreenDensityString(Context pContext) {
        DisplayMetrics dm = pContext.getResources().getDisplayMetrics();
        String dpiStr;
        if(dm.densityDpi == DisplayMetrics.DENSITY_LOW) {
            dpiStr = "ldpi (120)";
        } else if(dm.densityDpi == DisplayMetrics.DENSITY_MEDIUM) {
            dpiStr = "mdpi (160)";
        } else if(dm.densityDpi == DisplayMetrics.DENSITY_HIGH) {
            dpiStr = "hdpi (240)";
        } else if(dm.densityDpi == DisplayMetrics.DENSITY_XHIGH) {
            dpiStr = "xhdpi (320)";
        } else if(dm.densityDpi == DisplayMetrics.DENSITY_XXHIGH) {
            dpiStr = "xxhdpi (480)";
        } else
            dpiStr = "other (" + String.valueOf(dm.densityDpi) + ")";
        return dpiStr;
    }
    
    public static String getScreenResolutionString(Context pContext) {
        DisplayMetrics dm = pContext.getResources().getDisplayMetrics();
        return "(" + dm.widthPixels + "x" + dm.heightPixels + ")";
    }
    
    public static int getDimension(Context pContext, int dimenId) {
        return (int) (pContext.getResources().getDimension(dimenId) + 0.5f);
    }

    public static boolean hasSdCard()
    {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
}
