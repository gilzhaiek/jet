package com.contour.utils.api;

import android.app.Activity;
import android.os.StrictMode;

public class ApiHelperGingerbread  extends ApiHelperFroyo
{
    protected ApiHelperGingerbread(Activity activity)
    {
        super(activity);
    }
    
    @Override
    protected void enableStrictMode()
    {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskWrites().detectNetwork().penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
    } 
}
