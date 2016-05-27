
package com.reconinstruments.myactivities;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.utils.stats.StatTextUtils;
import com.reconinstruments.utils.stats.StatsUtil;
import com.reconinstruments.utils.stats.StatsUtil.StatType;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.myactivities.MyActivitiesDetailActivity;
import com.reconinstruments.utils.stats.TranscendUtils;

/**
 * 
 * <code>MyActivitiesActivity</code>
 * 
 * Select between All time and Last activity (left/right), and between Cycling and Running (up/down)
 *
 */
public class MyActivitiesActivity extends CarouselItemHostActivity {
    private static final String TAG = MyActivitiesActivity.class.getSimpleName();

    private enum HorizontalPage {
        ALL_TIME,
        LAST_ACTIVITY
    }

    public static final String EXTRA_PAGE = "SelectedPage";
    public static final String EXTRA_ACTIVITY = "ActivityType";

    protected SummaryManager summaryManager;

    HorizontalPage selectedPage = HorizontalPage.ALL_TIME;
    protected int mActivityType = ActivityUtil.SPORTS_TYPE_CYCLING;

    protected Typeface mSemiboldTypeface;
    private LinearLayout vertArrows;
    private TextView titleTV;
    private TextView valueTV;
    private TextView unitTV;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_activities);
        mSemiboldTypeface = UIUtils.getFontFromRes(getApplicationContext(), R.raw.opensans_semibold);

        vertArrows = (LinearLayout) findViewById(R.id.linearLayout1);
        titleTV = (TextView) findViewById(R.id.text_view);
        valueTV = (TextView) findViewById(R.id.value);
        unitTV = (TextView) findViewById(R.id.unit);
        unitTV.setTypeface(mSemiboldTypeface);
    }
    // onStart instead of onResume - bug    
    @Override
    public void onStart() {
        super.onStart();

        mActivityType = getIntent().getIntExtra(EXTRA_ACTIVITY,-1);
        if(mActivityType == -1){
            mActivityType = ActivityUtil.getMostRecentSport();
        }

        if(getIntent().hasExtra(EXTRA_PAGE))
            selectedPage = (HorizontalPage)getIntent().getSerializableExtra(EXTRA_PAGE);
        else
            selectedPage = HorizontalPage.ALL_TIME;

        summaryManager = new SummaryManager(this,mActivityType);
        if(!summaryManager.hasActivities()){
            setNoActivitiesLayout();
            return;
        }

        updatePage();

        initPager();
        mPager.setCurrentItem(selectedPage.ordinal());
    }

    /**
     * update main info according to which page(all time / recent) stays on
     */
    public void updatePage() {

        String totalActivities = summaryManager.getStatValue(StatType.ALL_TIME_ACTIVITIES);

        if (ActivityUtil.getNumRecordedSports()==2) {
            vertArrows.setVisibility(View.VISIBLE);
        }else{
            vertArrows.setVisibility(View.INVISIBLE);
        }

        titleTV.setText(StatTextUtils.getActivityTitle(mActivityType));

        if (selectedPage==HorizontalPage.ALL_TIME) {
            valueTV.setText(totalActivities);
            unitTV.setVisibility(View.VISIBLE);
            unitTV.setText(StatTextUtils.getActivityUnit(mActivityType));
        } else if(selectedPage==HorizontalPage.LAST_ACTIVITY) {
            valueTV.setText(summaryManager.getStatValue(StatType.LAST_ACTIVITY_DATE));
            unitTV.setVisibility(View.INVISIBLE);
        }
    }

    private void setNoActivitiesLayout(){
        if(DeviceUtils.isSnow2()) 
            setContentView(R.layout.no_activities_snow);
        else 
            setContentView(R.layout.no_activities);
    }

    protected List<Fragment> getFragments() {

        List<Fragment> fragments = new ArrayList<Fragment>();
        if(summaryManager.hasActivities()) {
            fragments.add(new MyActivitiesFragment(this, R.layout.my_activities_fragment, "All Time", 0, 0));
            fragments.add(new MyActivitiesFragment(this, R.layout.my_activities_fragment, StatTextUtils.getLastActivityName(mActivityType), 0, 1));
        }

        return fragments;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if(!summaryManager.hasActivities()){
            return super.onKeyUp(keyCode, event);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (ActivityUtil.getNumRecordedSports()==2) {
                    CommonUtils.scrollDown(this, getOtherSportIntent(), false);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if(selectedPage!=HorizontalPage.ALL_TIME) {
                    selectedPage = HorizontalPage.ALL_TIME;
                    updatePage();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if(selectedPage==HorizontalPage.ALL_TIME) {
                    selectedPage = HorizontalPage.LAST_ACTIVITY;
                    updatePage();
                }
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                Intent intent = new Intent(this, MyActivitiesDetailActivity.class);

                if(selectedPage==HorizontalPage.ALL_TIME)
                    intent.putExtra(MyActivitiesDetailActivity.EXTRA_DETAIL, MyActivitiesDetailActivity.DetailType.ALL_TIME_TOTALS);
                else if(selectedPage==HorizontalPage.LAST_ACTIVITY)
                    intent.putExtra(MyActivitiesDetailActivity.EXTRA_DETAIL, MyActivitiesDetailActivity.DetailType.LAST_ACTIVITY);
                intent.putExtra(MyActivitiesActivity.EXTRA_ACTIVITY, mActivityType);
                CommonUtils.launchNew(this,intent,true);

                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        CommonUtils.launchParent(this, new Intent("com.reconinstruments.itemhost"), true);
    }

    private Intent getOtherSportIntent(){
        Intent intent = new Intent("com.reconinstruments.myactivities");
        if (ActivityUtil.SPORTS_TYPE_CYCLING == mActivityType) {
            intent.putExtra(EXTRA_ACTIVITY, ActivityUtil.SPORTS_TYPE_RUNNING);
        } else if (ActivityUtil.SPORTS_TYPE_RUNNING == mActivityType) {
            intent.putExtra(EXTRA_ACTIVITY, ActivityUtil.SPORTS_TYPE_CYCLING);
        }
        intent.putExtra(EXTRA_PAGE, selectedPage);
        return intent;
    }
}
