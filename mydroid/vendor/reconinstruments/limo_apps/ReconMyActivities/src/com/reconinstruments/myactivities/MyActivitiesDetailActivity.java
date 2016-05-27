//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.myactivities;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.stats.StatsUtil.FormattedStat;
import com.reconinstruments.utils.stats.StatsUtil.StatType;
import com.reconinstruments.utils.stats.StatsUtil;

/**
 * 
 * <code>MyActiviesDetailFirstActivity</code>
 * 
 * Displays either All time totals or Last Ride detail, up/down goes to Second Activity
 *
 */
public class MyActivitiesDetailActivity extends CarouselItemHostActivity {
    private static final String TAG = MyActivitiesDetailActivity.class.getSimpleName();

    public enum DetailType {
        ALL_TIME_TOTALS,
            ALL_TIME_BEST,
            LAST_ACTIVITY
            };

    public static final String EXTRA_DETAIL = "DetailType";

    protected SummaryManager summaryManager;

    protected int mActivityType = ActivityUtil.SPORTS_TYPE_CYCLING; // default
    private DetailType mDetail;
    private boolean canScroll; //whether or not vertical scroll options exist

    private TextView titleTV;
    private LinearLayout vertArrows;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_activities_detail);

        titleTV = (TextView)findViewById(R.id.text_view);
        vertArrows = (LinearLayout)findViewById(R.id.top_image);

        canScroll = !DeviceUtils.isSnow2(); // No arrows for snow

        if(canScroll)
            vertArrows.setVisibility(View.VISIBLE);
        else
            vertArrows.setVisibility(View.INVISIBLE);

        enableInfinitePager = true;
    }

    // onStart instead of onResume - bug    
    @Override
    public void onStart() {
        super.onStart();

        mActivityType = getIntent().getIntExtra(MyActivitiesActivity.EXTRA_ACTIVITY,-1);
        if(mActivityType == -1){
            mActivityType = ActivityUtil.getMostRecentSport();
        }
        
        mDetail = (DetailType)getIntent().getSerializableExtra(EXTRA_DETAIL);
        
        summaryManager = new SummaryManager(this,mActivityType);
        if(!summaryManager.hasActivities()) {
            Intent intent = new Intent("com.reconinstruments.myactivities");
            intent.putExtra(MyActivitiesActivity.EXTRA_ACTIVITY, mActivityType);
            CommonUtils.launchParent(this, intent, true);
            return;
        }

        updateDetailView();

        initPager();

        if(canScroll)
            ((CarouselItemPageAdapter)mPager.getAdapter()).setBreadcrumbView(false); // no breadcrumbs when no vertical scroll
    }

    public void updateDetailView() {
        switch(mDetail){
        case ALL_TIME_TOTALS:
            titleTV.setText("ALL TIME TOTALS");
            break;
        case ALL_TIME_BEST:
            titleTV.setText("ALL TIME BEST");
            break;
        case LAST_ACTIVITY:
            setContentView(R.layout.my_activities_last_activity);
            titleTV = (TextView)findViewById(R.id.last_activity_date);
            titleTV.setText(summaryManager.getStatValue(StatType.LAST_ACTIVITY_DATE).toUpperCase());
            LinearLayout mapText = (LinearLayout)findViewById(R.id.map_text);
            if((DeviceUtils.isSun())){
                mapText.setVisibility(View.VISIBLE);
            }else{
                mapText.setVisibility(View.INVISIBLE);
            }
            break;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP:
            if(canScroll) // do nothing to prevent feedback animation
                CommonUtils.scrollUp(this, generateIntent(), false);

            return true;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            if(canScroll)
                CommonUtils.scrollDown(this, generateIntent(), false);

            return true;
        case KeyEvent.KEYCODE_DPAD_CENTER:
            //no animation on key press
            return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MyActivitiesActivity.class);
        intent.putExtra(MyActivitiesActivity.EXTRA_ACTIVITY, mActivityType);
        CommonUtils.launchParent(this, intent, true);
    }

    private Intent generateIntent(){
        Intent intent = null;
        switch(mDetail){
        case ALL_TIME_TOTALS:
        case ALL_TIME_BEST:
            intent = new Intent(this, MyActivitiesDetailActivity.class);
            intent.putExtra(MyActivitiesActivity.EXTRA_ACTIVITY, mActivityType);
            if(mDetail==DetailType.ALL_TIME_TOTALS)
                intent.putExtra(EXTRA_DETAIL, DetailType.ALL_TIME_BEST);
            else
                intent.putExtra(EXTRA_DETAIL, DetailType.ALL_TIME_TOTALS);
            break;
        case LAST_ACTIVITY:
            intent = new Intent(this, MyActivityMapOverlayActivity.class);
            intent.putExtra(MyActivitiesActivity.EXTRA_ACTIVITY, mActivityType);
            break;
        }
        return intent;
    }

    // Generate detail fragments
    StatType[] snowAllTimeDetails = {
        StatType.ALL_TIME_TOTAL_RUNS,
        StatType.ALL_TIME_MAX_SPEED,
        StatType.ALL_TIME_VERTICAL,
        StatType.ALL_TIME_TOTAL_DISTANCE,
        StatType.ALL_TIME_MAX_ALTITUDE,
        StatType.ALL_TIME_BEST_JUMP
    };
    StatType[] snowLastActivityDetails = {
        StatType.LAST_ACTIVITY_RUNS,
        StatType.LAST_ACTIVITY_MAX_SPEED,
        StatType.LAST_ACTIVITY_VERTICAL,
        StatType.LAST_ACTIVITY_DISTANCE,
        StatType.LAST_ACTIVITY_ALTITUDE,
        StatType.LAST_ACTIVITY_JUMP
    };
    StatType[] sunAllTimeTotalDetails = {
        StatType.ALL_TIME_TOTAL_DURATION,
        StatType.ALL_TIME_TOTAL_DISTANCE,
        StatType.ALL_TIME_TOTAL_ELEVATION_GAIN,
        StatType.ALL_TIME_TOTAL_CALORIES_BURNED
    };
    StatType[] sunAllTimeBestRunningDetails = {
        StatType.ALL_TIME_BEST_DURATION,
        StatType.ALL_TIME_BEST_DISTANCE,
        StatType.ALL_TIME_BEST_ELEVATION_GAIN,
        StatType.ALL_TIME_BEST_AVERAGE_PACE,
        StatType.ALL_TIME_BEST_CALORIES_BURNED
    };
    StatType[] sunAllTimeBestCyclingDetails = {
        StatType.ALL_TIME_BEST_DURATION,
        StatType.ALL_TIME_BEST_DISTANCE,
        StatType.ALL_TIME_BEST_ELEVATION_GAIN,
        StatType.ALL_TIME_BEST_AVERAGE_SPEED,
        StatType.ALL_TIME_BEST_CALORIES_BURNED
    };
    StatType[] sunLastActivityRunningDetails = {
        StatType.LAST_ACTIVITY_DISTANCE,
        StatType.LAST_ACTIVITY_DURATION,
        StatType.LAST_ACTIVITY_AVERAGE_PACE,
        StatType.LAST_ACTIVITY_ELEVATION_GAIN,
        StatType.LAST_ACTIVITY_AVERAGE_HEART_RATE,
        StatType.LAST_ACTIVITY_CALORIES_BURNED
    };
    StatType[] sunLastActivityCyclingDetails = {
        StatType.LAST_ACTIVITY_DISTANCE,
        StatType.LAST_ACTIVITY_DURATION,
        StatType.LAST_ACTIVITY_ELEVATION_GAIN,
        StatType.LAST_ACTIVITY_AVERAGE_POWER,
        StatType.LAST_ACTIVITY_MAX_POWER,
        StatType.LAST_ACTIVITY_AVERAGE_HEART_RATE,
        StatType.LAST_ACTIVITY_AVERAGE_CADENCE,
        StatType.LAST_ACTIVITY_AVERAGE_SPEED,
        StatType.LAST_ACTIVITY_CALORIES_BURNED
    };
    /**
     * prepare the fragments, called by method initPager()
     */
    protected List<Fragment> getFragments() {

        if(DeviceUtils.isSnow2()) {
            switch(mDetail){
            case ALL_TIME_TOTALS:
                return getFragmentList(snowAllTimeDetails);
            case LAST_ACTIVITY:
                return getFragmentList(snowLastActivityDetails);
            }
        } 
        else {
            switch(mDetail){
            case ALL_TIME_TOTALS:
                return getFragmentList(sunAllTimeTotalDetails);
            case ALL_TIME_BEST:
                if(mActivityType != ActivityUtil.SPORTS_TYPE_RUNNING){
                    return getFragmentList(sunAllTimeBestCyclingDetails);
                } else {
                    return getFragmentList(sunAllTimeBestRunningDetails);
                }
            case LAST_ACTIVITY:
                if(mActivityType != ActivityUtil.SPORTS_TYPE_RUNNING){
                    return getFragmentList(sunLastActivityCyclingDetails);  
                } else {
                    return getFragmentList(sunLastActivityRunningDetails);
                }
            }
        }
        Log.e(TAG, "Error, no fragment list for detail view, (mDetail="+mDetail+",isSnow="+DeviceUtils.isSnow2()+")");
        return null;
    }

    private List<Fragment> getFragmentList(StatType[] statTypes) {
        Log.d(TAG, "getFragmentList");
        List<Fragment> fragments = new ArrayList<Fragment>();
        if(!summaryManager.hasActivities()) {
            Log.d(TAG, "No details to display, finishing activity");
            finish();
            return fragments;
        } 
        HashMap<StatType, FormattedStat> stats = summaryManager.getStats();
        for(StatType statType:statTypes) {
            if(stats.get(statType)!=null) {
                fragments.add(new MyActivitiesDetailFragment(this, statType, fragments.size()));
            }
        }
        return fragments;
    }
}
