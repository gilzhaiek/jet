package com.contour.utils.api;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;

@TargetApi(14)
public class ApiHelperICS extends ApiHelperHoneycomb
{

    protected ApiHelperICS(Activity activity) {
        super(activity);
    }
    
    @Override
    protected Context getActionBarThemedContext() {
        return mActivity.getActionBar().getThemedContext();
    }
}
