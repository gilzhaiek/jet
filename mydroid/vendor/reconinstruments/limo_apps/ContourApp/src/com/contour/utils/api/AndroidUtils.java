package com.contour.utils.api;

import org.apache.http.client.HttpClient;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

public class AndroidUtils
{
    
    public static void enableStrictMode()
    {
        ApiHelper.createInstance(null).enableStrictMode();
    }    
    
    public static HttpClient getHttpClient(String userAgentString)
    {
        return ApiHelper.createInstance(null).getHttpClient(userAgentString);
    }
    
    public static boolean hasGpsEnabled(Context context)
    {
        return ((LocationManager)context.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    public static boolean hasNetworkConnection(Context context)
    {
        NetworkInfo info = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }
    
    public static boolean isEclairOrLower()
    {
       return Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1;
    }
    
    public static boolean isFroyoOrHigher()
    {
       return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }
    
    public static boolean isGingerbreadOrHigher()
    {
       return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }
    
    public static boolean isHoneycombOrHigher()
    {
       return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
    
    public static boolean isIceCreamSandwichOrHigher()
    {
       return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
    public static boolean isJellybeanOrHigher()
    {
       return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
    
}
