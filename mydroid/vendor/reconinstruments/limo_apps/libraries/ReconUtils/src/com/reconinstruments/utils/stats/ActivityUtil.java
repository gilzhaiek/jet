package com.reconinstruments.utils.stats;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.reconinstruments.utils.SettingsUtil;

/**
 * <code>ActivityUtil</code> provides two api allowing other
 * components to write the activity bundle into SUMMARY_FOLDER, and
 * read it back into a bundle.
 *
 */
public class ActivityUtil {
    private static final String TAG = "ActivityUtil";
    
    public static final int MSG_START_SPORTS_ACTIVITY = 20;
    public static final int MSG_PAUSE_SPORTS_ACTIVITY = 21;
    public static final int MSG_RESTART_SPORTS_ACTIVITY = 22;
    public static final int MSG_STOP_SPORTS_ACTIVITY = 23;
    public static final int MSG_DISCARD_SPORTS_ACTIVITY = 24;
    public static final int MSG_SAVE_SPORTS_ACTIVITY = 25;
    public static final int MSG_SET_SPORTS_ACTIVITY = 26;

    public static final int SPORTS_ACTIVITY_STATUS_ERROR = -1;
    public static final int SPORTS_ACTIVITY_STATUS_NO_ACTIVITY = 0;
    public static final int SPORTS_ACTIVITY_STATUS_ONGOING = 1;
    public static final int SPORTS_ACTIVITY_STATUS_PAUSED = 2;

    public static final int SPORTS_TYPE_OTHER = -1;
    public static final int SPORTS_TYPE_SKI = 0;
    public static final int SPORTS_TYPE_CYCLING = 1;
    public static final int SPORTS_TYPE_RUNNING = 2;
    public static final int SPORTS_TYPE_DEFAULT = SPORTS_TYPE_SKI;

    public static final	String CYCLING_XML = "cycling.xml";
    public static final	String RUNNING_XML = "running.xml";
    public static final	String SNOW_XML = "snow.xml";

    public static final String WAS_SHUTDOWN_WHILE_ACTIVITY_ONGOING = "com.reconinstruments.was_shutdown_while_activity_ongoing";

    public static final HashMap<Integer, String> SPORTS_TO_PROFILE =
            new HashMap<Integer, String>() {{
                put(SPORTS_TYPE_SKI,SNOW_XML);
                put(SPORTS_TYPE_CYCLING,CYCLING_XML);
                put(SPORTS_TYPE_RUNNING,RUNNING_XML);
            }
    };
    public static final void startActivity(Context c) {
        TranscendUtils.sendCommandToTranscendService(c,MSG_START_SPORTS_ACTIVITY);
    }
    public static final void pauseActivity(Context c) {
        TranscendUtils.sendCommandToTranscendService(c,MSG_PAUSE_SPORTS_ACTIVITY);
    }
    public static final void resumeActivity(Context c) {
        TranscendUtils.sendCommandToTranscendService(c,MSG_RESTART_SPORTS_ACTIVITY);
    }
    public static final void stopActivity(Context c) {
        TranscendUtils.sendCommandToTranscendService(c,MSG_STOP_SPORTS_ACTIVITY);
    }
    public static final void discardActivity(Context c) {
        TranscendUtils.sendCommandToTranscendService(c,MSG_DISCARD_SPORTS_ACTIVITY);
    }
    public static final void saveActivity(Context c) {
        TranscendUtils.sendCommandToTranscendService(c,MSG_SAVE_SPORTS_ACTIVITY);
    }
    public static final boolean wasShutDownWhileActivityOngoing(Context c) {
        return (SettingsUtil.getCachableSystemIntOrSet(c,WAS_SHUTDOWN_WHILE_ACTIVITY_ONGOING,0) == 1);
    }
    public static final void setWasShutDownWhileActivityOngoing(Context c,boolean v) {
        int val = v?1:0;
        SettingsUtil.setSystemInt(c,WAS_SHUTDOWN_WHILE_ACTIVITY_ONGOING,val);
    }

    /**
     * @return the list of sports type, sorted by the most recent sports
     */
    public static List<Integer> getAvailableSportsSummaryTypes(){
        List<Pair<Integer,Long>> summaryInfo = new ArrayList<Pair<Integer,Long>>();
        summaryInfo.add(new Pair<Integer, Long>(ActivityUtil.SPORTS_TYPE_CYCLING,getDateForSport(ActivityUtil.SPORTS_TYPE_CYCLING)));
        summaryInfo.add(new Pair<Integer, Long>(ActivityUtil.SPORTS_TYPE_RUNNING,getDateForSport(ActivityUtil.SPORTS_TYPE_RUNNING)));
        summaryInfo.add(new Pair<Integer, Long>(ActivityUtil.SPORTS_TYPE_SKI,getDateForSport(ActivityUtil.SPORTS_TYPE_SKI)));
        
        // sort by date
        Collections.sort(summaryInfo, new Comparator<Pair<Integer, Long>>() {
            public int compare(Pair<Integer, Long> lhs, Pair<Integer, Long> rhs) {
                return lhs.second.compareTo(rhs.second);
            }
        });
        List<Integer> availableSummaries = new ArrayList<Integer>();
        for(Pair<Integer,Long> summary:summaryInfo) {
            if(summary.second!=0)
                availableSummaries.add(summary.first);
        }
        return availableSummaries;
    }
    
    private static long getDateForSport(int sportType) {
        File file = TranscendUtils.bundleFileFromType(sportType);
        if(file != null && (int)file.length() > 0)
            return file.lastModified();
        return 0;
    }

    public static int getCurrentSportsType(Context context) {
        Bundle fullInfo = TranscendUtils.getFullInfoBundle(context);
        return getCurrentSportsType(fullInfo);
    }
    public final static int getCurrentSportsType(Bundle bundle){
        int sportsType = ActivityUtil.SPORTS_TYPE_DEFAULT;
        if (bundle != null) {
            sportsType = bundle.getBundle("SPORTS_ACTIVITY_BUNDLE").getInt("Type");
        }
        return sportsType;
    }

    public final static int getActivityState(Context c) {
        Bundle fullInfo = TranscendUtils.getFullInfoBundle(c);
        return getActivityState(fullInfo);
    }
    public final static int getActivityState(Bundle b) {
        if (b != null) {
            return b.getBundle("SPORTS_ACTIVITY_BUNDLE").getInt("Status");
        } else {
            return ActivityUtil.SPORTS_ACTIVITY_STATUS_ERROR;
        }
    }
    
    public static int getNumRecordedSports() {
        List<Integer> summaries = getAvailableSportsSummaryTypes();
        return summaries.size();
    }
    public static int getMostRecentSport() {
        List<Integer> summaries = getAvailableSportsSummaryTypes();
        if(summaries.size() > 0) {
            Log.i(TAG, "most recent sports activity is " + summaries.get(0));
            return summaries.get(0); // most recent sports activity
        } else {
            Log.i(TAG, "use default sports activity - cycling");
            return ActivityUtil.SPORTS_TYPE_CYCLING;
        }
    }
}
