package com.reconinstruments.utils;


import com.reconinstruments.utils.stats.ActivityUtil;

/**
 * Describe class <code>DashboardUtils</code> here. A set of utility
 * classes that are used when writing dashboard apps. Livestats and
 * JetDashboard to name a fiew
 *
 */
import android.os.Bundle;
public class DashboardUtils {
    public static final boolean shouldShowOverlay(Bundle fullInfo) {
        // Simple Logic: Dont have Gps and Not in activity
        return (!fullInfo.getBoolean("LOCATION_BUNDLE_VALID", false)
                && (ActivityUtil.getActivityState(fullInfo) ==
                    ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY));
    }

}