package com.reconinstruments.dashlauncherredux;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import java.util.List;

/**
 * A class corresponding to an element that sits in the columns of the
 * dashlauncher. Through this element the corresponding activity can be
 * launched.
 * 
 * @author <a href="mailto:ali@reconinstruments.com">Ali R. Mohazab</a>
 * @version 1.0
 */
public class ColumnElementInfo {
    Intent mIntent = null;
    String mProfileName = "all";
    int mPreferredOrder = 0;
    private static final String TAG = "ColumnElementInfo";

    /**
     * Creates a new <code>ColumnElementInfo</code> instance based on a
     * component info object..
     * 
     * Note: The resulting Object may not correspong to an actual activity, e.g.
     * if the supplied component name is garbage. Need to call
     * <code>isValid()</code> to make sure it corresponds to an actual activity.
     * 
     * @param compname
     *            a <code>ComponentName</code> value
     * @param extras
     *            a <code>String</code> value
     */
    public ColumnElementInfo(ComponentName compname,String extras) {
	mIntent = new Intent(Intent.ACTION_MAIN);
	mIntent.setComponent(compname);
	mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
			 | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
	mIntent.putExtra("com.reconinstruments.columnelement.BeElement",true);
	// By setting this to true we tell the ColumnElement Activity
	// to behave as column element.
	if (extras != null) {
	    mIntent.putExtra("com.reconinstruments.columnelement.extras", extras);
	}
    }

    /**
     * Tests if the ColumnElementInfo actually corresponds to an activity that
     * can be launched.
     * 
     * @param c
     *            a <code>Context</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isValid(Context c) {
	Log.d(TAG, "isValid " + mIntent.toString());
	List<ResolveInfo> activities = c.getPackageManager()
	    .queryIntentActivities(mIntent,
				   PackageManager.MATCH_DEFAULT_ONLY);
	return (!activities.isEmpty());
    }
}