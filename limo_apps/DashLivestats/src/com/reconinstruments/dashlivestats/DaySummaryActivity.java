//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlivestats;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.StatFormatUtils;
import com.reconinstruments.utils.stats.StatsUtil;
import com.reconinstruments.utils.stats.StatsUtil.FormattedStat;
import com.reconinstruments.utils.stats.StatsUtil.StatDescription;
import com.reconinstruments.utils.stats.StatsUtil.StatType;
import com.reconinstruments.utils.stats.TranscendUtils;

import java.util.ArrayList;
import java.util.List;

public class DaySummaryActivity extends CarouselItemHostActivity {

    private Context mContext;
    private JetDialog daySavedDialog;
    private FragmentManager fm;
    private Bundle sFullInfo;
    private boolean mShouldQuit = false;

    boolean isMetric;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        if(DeviceUtils.isSun()){
            setContentView(R.layout.jet_day_summary);
            daySavedDialog = new JetDialog(this, R.layout.jet_day_saved);
        }else{
            setContentView(R.layout.day_summary);
            daySavedDialog = new JetDialog(this, R.layout.day_saved);
        }
        fm = getSupportFragmentManager();
        sFullInfo = TranscendUtils.getFullInfoBundle(this);

        isMetric = SettingsUtil.getUnits(this) == SettingsUtil.RECON_UINTS_METRIC;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // stub
                showFinishDayOverlay();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Intent intent = new Intent(this, PostActivityMapOverlayActivity.class);
                CommonUtils.scrollDown(this, intent, false);
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        CommonUtils.launchParent(this, new Intent(this, JetLiveStatsActivity.class), true);
    }

    @Override
    public void onStart(){
        super.onStart();
        initPager();
        mPager.setPadding(90,0,90,0);
        ((CarouselItemPageAdapter)mPager.getAdapter()).setBreadcrumbView(false);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mShouldQuit){
            startActivity(new Intent("com.reconinstruments.itemhost"));
            finish();
        }
    }

    
    protected List<Fragment> getFragments(){
        //try to load full info bundle, to prevent null value return
        for (int i = 0; i < 100; i ++){
            if(sFullInfo == null){
                sFullInfo = TranscendUtils.getFullInfoBundle(this);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }else{
                break;
            }
        }

        enableInfinitePager = true;
        return makeFragments();
    }
    
    public DaySummaryFragment createFragment(StatType statType, int i) {
        StatDescription statDesc = StatsUtil.getStatDesc(statType);
        FormattedStat stat = StatFormatUtils.formatData(this,sFullInfo,statDesc,isMetric,true);
        if(stat!=null) {
            return new DaySummaryFragment(stat, statType, i);
        }
        return null;
    }
    
    StatType[] cyclingStatTypes = {
            StatType.LAST_ACTIVITY_MAX_SPEED,
            StatType.LAST_ACTIVITY_AVERAGE_SPEED,
            StatType.LAST_ACTIVITY_AVERAGE_CADENCE,
            StatType.LAST_ACTIVITY_AVERAGE_POWER,
            StatType.LAST_ACTIVITY_MAX_POWER
    };
    StatType[] runningStatTypes = {
            StatType.LAST_ACTIVITY_RUNS,
            StatType.LAST_ACTIVITY_AVERAGE_PACE
    };
    StatType[] sunStatTypes = {
            StatType.LAST_ACTIVITY_CALORIES_BURNED,
            StatType.LAST_ACTIVITY_GRADE
    };
    StatType[] commonStatTypes = {
            StatType.LAST_ACTIVITY_DISTANCE,
            StatType.LAST_ACTIVITY_AVERAGE_HEART_RATE,
            StatType.LAST_ACTIVITY_DURATION,
            StatType.LAST_ACTIVITY_ELEVATION_GAIN
    };
    
    public int addStats(StatType[] stats,List<Fragment> fragments,int i) {
        for(StatType statType:stats) {
            DaySummaryFragment fragment = createFragment(statType,i);
            if(fragment!=null) {
                fragments.add(fragment);
                i++;
            }
        }
        return i;
    }
    public List<Fragment> makeFragments() {
        int sportType = ActivityUtil.getCurrentSportsType(this.getApplicationContext());
        List<Fragment> fragments = new ArrayList<Fragment>();
        int i = 0;
        if(sportType == ActivityUtil.SPORTS_TYPE_CYCLING) {
            i = addStats(cyclingStatTypes,fragments,i);
        } else if(sportType == ActivityUtil.SPORTS_TYPE_RUNNING) {
            i = addStats(runningStatTypes,fragments,i);
        }

        if(sportType != ActivityUtil.SPORTS_TYPE_SKI) {
            i = addStats(sunStatTypes,fragments,i);
        }
        i = addStats(commonStatTypes,fragments,i);
        return fragments;
    }

    public void showFinishDayOverlay() {
        List<Fragment> list = new ArrayList<Fragment>();

        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Cancel", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Save & Finish", 0, 1));
        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Save & Share", 0, 2));
        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Discard", 0, 3));

        Fragment frg = fm.findFragmentByTag("finish_day");
        if (frg == null) {
            FinishDayOverlay overlay = null;
            if(DeviceUtils.isSun()){
                overlay = new FinishDayOverlay("FINISH ACTIVITY?", list, R.layout.finish_day, this);
            } else {
                overlay = new FinishDayOverlay("FINISH DAY?", list, R.layout.finish_day, this);
            }

            overlay.show(fm.beginTransaction(), "finish_day");
        }
    }

    public void showDaySavedDialog() {
        if (!daySavedDialog.isShowing()) {
            daySavedDialog.show();
            new CountDownTimer(2 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if (daySavedDialog != null) {
                        daySavedDialog.dismiss();
                        mShouldQuit = true;
                    }
                    goHome();
                }
            }.start();
        }
    }
    public void goHome() {
        startActivity(new Intent("com.reconinstruments.itemhost"));
        finish();
    }
}