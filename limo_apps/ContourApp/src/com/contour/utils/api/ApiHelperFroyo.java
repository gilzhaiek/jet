package com.contour.utils.api;

import android.app.Activity;

public class ApiHelperFroyo extends ApiHelperBase
{
    protected ApiHelperFroyo(Activity activity)
    {
        super(activity);
    }

    @Override
    protected void enableStrictMode()
    {
       //Not available in Froyo
    }
}
