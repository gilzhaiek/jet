//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashboard;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;

import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.utils.SettingsUtil;

/**
 * <code>DashboardActivity</code> represents the metrics data on the
 * dashboard.  It holds a big view and a small view based on
 * fragment. If the small view over 2, the screen would switch them
 * one by one every 5 seconds. the user can switch them by up/down
 * swipping as well.

 To support having third party widgets. The view objects are generated
 outside the activity and then broadcast and picked up in the
 <code>DashboardActivity</code>. The remote view is then inflated and
 shown to the user.

 Even the views that are based on onBoard sensor and are calculated
 in-house are treated this way. Future optimization may treat them
 differently (say local).
*/
public class DashboardActivity extends ColumnElementFragmentActivity {
    
    private static final String TAG = DashboardActivity.class.getSimpleName();
    
    //constants for 'new activity' remote views
    public final static String INTENT_ACTION = "com.reconinstruments.itemhost.RECON_WIDGET";
    public final static String EXTRA_VIEW = "REMOTE_VIEW";
    public final static String EXTRA_TAG = "TAG";
    public final static String EXTRA_UP_LAUNCHER = "UP_LAUNCHER";
    public final static String EXTRA_DOWN_LAUNCHER = "DOWN_LAUNCHER";
    private final static String DEFAULT_LAUNCHER = "com.reconinstruments.connectdevice.CONNECT";
    public static final String TAG_NEW_ACTIVITY = "A815909C-4E5A-4D85-990E-C9461B5BB909";

    /**
     *  <code>mCurrentPosition</code> referss to the position of the
     *  <code>DashboardFragment</code> should be visible.
     */
    private int mCurrentPosition = 0;
    private boolean mShouldAutoRotate = false;
    private int mAutoRotatePeriod = 0;
    private Handler mPanelRotator = new Handler();
    private List<DashboardFragment> mDashFragments = new ArrayList<DashboardFragment>();
    private MetricManager mMetricManager; // Singleton

    // record the previous gps fix status to determine if the 'start ride' toast should show or not
    private boolean mHasGpsFixPreviousValue = false; 
    
    private ImageView mActivityStateIV;
    private Typeface semiboldTypeface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);
        
        mActivityStateIV = (ImageView)findViewById(R.id.activity_state);
        
        semiboldTypeface = UIUtils.getFontFromRes(getApplicationContext(), R.raw.opensans_semibold);
        
        mMetricManager = MetricManager.getInstance(getApplicationContext());
        mMetricManager.init();
    }

    @Override 
    public void onStart() {
        super.onStart();
        // start receiving sports activity metrix and state data
        resumeActivity();

    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        startService(new Intent("RECON_ANT_SERVICE"));
        
        mActivityStateIV.setVisibility(View.VISIBLE);
        setAutoRotateRelatedFields();
        mHasGpsFixPreviousValue = false;

        try{
            
            int showGpsOverlay = Settings.System.getInt(getApplicationContext().getContentResolver(),
                                                        "SHOWGPSOVERLAYIFNEEDED");
            if(showGpsOverlay == 1){
                if(!mMetricManager.hasGpsFix() && MetricManager.getInstance(getApplicationContext()).getActivityState() == ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY){
                    //if there is no gps fix and the activity doesn't start, show searching gps overlay
                    showSearchingGPSDialog();
                }
            }
        }catch(SettingNotFoundException e){
            //load default sports activity - cycling instead
            mMetricManager.loadSportsLayout(ActivityUtil.SPORTS_TYPE_CYCLING);
        }
        int state = mMetricManager.getActivityState();
        updateActivityState(state);
        // state: 0 no activiy, 1, ongoing, 2 paused
        if(state == ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY){
            // if there is gps fix but the activity doesn't start, show start ride overlay
            //TODO comment out until we are going to do 'share ride'
            //showStartActivityOverlay();
        }else if(state == ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED){
            // ask for resuming activity
            mActivityStateIV.setVisibility(View.INVISIBLE);
            showPauseDialog();
        }
    }

    
    @Override
    public void onStop() {
        super.onStop();
        //stop receiving sports activity metrix and state data
        pauseActivity();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mMetricManager.stop();
        try {
            unregisterReceiver(metricReceiver);
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                e.printStackTrace();
            }
        }
    }

    private void buildFragments(int count) {
        int previousCount = mDashFragments.size();
        if(count > previousCount){
            for(int i = 0; i< count - previousCount; i ++){
                mDashFragments.add(new DashboardFragment(this, count, previousCount + i)); 
            }
        }else{
            for(int i = 0; i< previousCount - count; i ++){
                mDashFragments.remove(mDashFragments.size() - 1);
            }
        }
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP:
            nextDashboard(-1);  // previous
            return true;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            nextDashboard(1);   // next
            return true;
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            doActivity();
            return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
    }
    
    /**
     * take proper action depends on different activity state
     */
    public void doActivity(){
        int state = mMetricManager.getActivityState();
        if(state == 0){
            if(mMetricManager.hasGpsFix()){// nothing running and there is gps fix
                mMetricManager.startSportsActivity();
            }else{
                // show GpsOverlay if there is no gps fix and activity doesn't start
                showSearchingGPSDialog(); 
            }
        }else if(state == 1){ // ongoing
            mActivityStateIV.setVisibility(View.INVISIBLE);
            mMetricManager.pauseSportsActivity();
            showPauseDialog();
        }else if(state == 2){ // paused   
            mMetricManager.resumeSportsActivity();
        }
    }
    
    /**
     * launch the item host app.
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent("com.reconinstruments.itemhost");
        startActivity(intent);
    }
    
    /**
     * pause receiving sports activity updated event
     */
    public void pauseActivity(){
        try {
            unregisterReceiver(metricReceiver);
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                e.printStackTrace();
            }
        }
        mPanelRotator.removeCallbacks(mRotateToNext);
    }

    /**
     * resume receiving sports activity updated event
     */
    public void resumeActivity(){
        //receive big view data, sports activity state, small view count.
        IntentFilter filter = new IntentFilter();
        filter.addAction(MetricManager.INTENT_ACTION_VIEWS_COUNT);
        filter.addAction(MetricManager.INTENT_ACTION_SMALL_VIEWS);
        filter.addAction(MetricManager.INTENT_ACTION_BIG_VIEWS);
        filter.addAction(MetricManager.INTENT_ACTION_SPORT_STATE);
        registerReceiver(metricReceiver, filter);
        setDashboard(mCurrentPosition);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
            goLeft();
            return true;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            goRight();
            return true;
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
            return true;        // just consume
            
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private final BroadcastReceiver metricReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(MetricManager.INTENT_ACTION_BIG_VIEWS.equals(intent.getAction())){ // big view
                    int extraType = intent.getIntExtra(MetricManager.EXTRA_TYPE, MetricManager.TYPE_DATA_BUNDLE);
                    if(extraType == MetricManager.TYPE_DATA_BUNDLE){ // only data no view
                        Bundle b = intent.getExtras().getBundle(MetricManager.EXTRA_WIDGET);
                        BigWidget widget = new BigWidget(b);
                        updateBigWidget(widget);
                    }else{ //remote views
                        RemoteViews views = (RemoteViews)intent.getParcelableExtra(MetricManager.EXTRA_WIDGET);
                        replaceAsRemoteView(context, intent, views);
                    }
                    showOrDismissStartActivityOverlayIfNeeded();
                    mHasGpsFixPreviousValue = mMetricManager.hasGpsFix();
                }
                else if(MetricManager.INTENT_ACTION_SMALL_VIEWS.equals(intent.getAction())){
                    if(mDashFragments.size() < 1){
                        return;
                    }
                    DashboardFragment fragment = null;
                    int slot = 0;
                    int extraType = intent.getIntExtra(MetricManager.EXTRA_TYPE, MetricManager.TYPE_DATA_BUNDLE);
                    if(extraType == MetricManager.TYPE_DATA_BUNDLE){
                        Bundle b = intent.getExtras().getBundle(MetricManager.EXTRA_WIDGET);
                        SmallWidget widget = new SmallWidget(b);
                        slot = widget.getSlot();
                    }else{ //remote views
                        slot = intent.getIntExtra(MetricManager.EXTRA_SLOT, 0);
                    }
                    if(slot < 1){
                        return;
                    }
                    try{
                        fragment = mDashFragments.get((int) Math.ceil(slot/2.0) - 1);
                    }catch(java.lang.IndexOutOfBoundsException iobe){
                        //ignore it, not ready yet this time
                    }
                    if(fragment == null || fragment.getView() == null){
                        return;
                    }
                    fragment.updateSmallWidget(context, intent);
                }

                // TODO: remove this condition and rewrite so that it is not needed
                else if(MetricManager.INTENT_ACTION_VIEWS_COUNT.equals(intent.getAction())){
                    int count = intent.getExtras()
                        .getInt(MetricManager.EXTRA_COUNT, 0);
                    if(count > 0){
                        buildFragments(count);
                        mCurrentPosition = 0;
                        setDashboard(mCurrentPosition);
                    }
                }
                else if(MetricManager.INTENT_ACTION_SPORT_STATE.equals(intent.getAction())){
                    int state = intent.getExtras()
                        .getInt(MetricManager.EXTRA_STATE, 0);
                    updateActivityState(state);
                }
            }
        };
        
    private void updateActivityState(int state){   
        if(ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY == state){
            if(mActivityStateIV.getVisibility() != View.VISIBLE){
                mActivityStateIV.setVisibility(View.VISIBLE);
            }
            if(mMetricManager.hasGpsFix()){
                mActivityStateIV.setImageResource(R.drawable.start_activity_icon);
            }else{
                mActivityStateIV.setImageResource(R.drawable.select_btn);
            }
        }else if(ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING == state){
            if(mActivityStateIV.getVisibility() != View.VISIBLE){
                mActivityStateIV.setVisibility(View.VISIBLE);
            }
            mActivityStateIV.setImageResource(R.drawable.pause_activity_icon);
        }else if(ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED == state){
            // in this case, a resume overlay will be shown up
            if(mActivityStateIV.getVisibility() != View.INVISIBLE){
                mActivityStateIV.setVisibility(View.INVISIBLE);
            }
        }
        setAutoRotateRelatedFields();
    }
    
    /**
     * Reads hasHadGps from the metric manager and show or dismiss the
     * overlay if needed
     */
    private void showOrDismissStartActivityOverlayIfNeeded(){
        if(mMetricManager.hasGpsFix()){
            // dismiss searching gps overylay if needed once it has gps fix 
            // since it would show up before in the case of no gps fix
            dismissSearchingGPSDialog();
            
            if(mMetricManager.getActivityState() == 0 && !mHasGpsFixPreviousValue){
                // if gps fix is acquired, show 'start ride' overlay
                // TODO comment out until we are going to do 'share ride'
                //showStartActivityOverlay();
            }
        }else{
            // dismiss start ride overylay if needed once it donesn't have gps fix
            dismissStartActivityOverlay();
        }
    }
    
    private void replaceAsRemoteView(Context context, Intent intent, RemoteViews views){
        ViewGroup vg = (ViewGroup) findViewById(R.id.top_layout);
        View inflatedView = views.apply(context, vg);
        if (vg.getChildCount() > 0) {
            vg.removeAllViews();
        }
        vg.addView(inflatedView);
        vg.invalidate();
    }
    
    private void updateBigWidget(BigWidget widget){
        int disconnectedIcon = widget.getDisconnectedIcon();
        TextView valueTV = (TextView)findViewById(R.id.value);
        ImageView invalidImageView = (ImageView)findViewById(R.id.invalid_image);
        valueTV.setVisibility(View.VISIBLE);
        invalidImageView.setVisibility(View.GONE);
        Spannable wordToSpan = new SpannableString(widget.getValue());
        if(widget.getValue().endsWith("---") || widget.getValue().startsWith("000")){
            wordToSpan.setSpan(new ForegroundColorSpan(Color.DKGRAY), 0, 3, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }else if(widget.getValue().startsWith("+00") || widget.getValue().startsWith("-00")){
            wordToSpan.setSpan(new ForegroundColorSpan(Color.DKGRAY), 1, 3, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }else if(widget.getValue().startsWith("00")){
            wordToSpan.setSpan(new ForegroundColorSpan(Color.DKGRAY), 0, 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }else if(widget.getValue().startsWith("+0") || widget.getValue().startsWith("-0")){
            wordToSpan.setSpan(new ForegroundColorSpan(Color.DKGRAY), 1, 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }else if(widget.getValue().startsWith("0")){
            wordToSpan.setSpan(new ForegroundColorSpan(Color.DKGRAY), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        valueTV.setTypeface(semiboldTypeface);
        valueTV.setText(wordToSpan);
        TextView unitTV = (TextView)findViewById(R.id.unit);
        unitTV.setTypeface(semiboldTypeface);
        unitTV.setText(widget.getUnit());
    }
    
    public void dismissPauseDialog(){
        Fragment dialog = getSupportFragmentManager().findFragmentByTag("pause_dialog");
        if (dialog != null) {
            DialogFragment df = (DialogFragment) dialog;
            df.dismiss();
        }
    }
    
    public void showPauseDialog() {
        pauseActivity();
        dismissPauseDialog();
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        PauseDialog dialog= new PauseDialog(this);
        dialog.show(fm, "pause_dialog");
    }
    
    private void showResumeDialog() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        list.add(new ReconJetDialogFragment(R.layout.carousel_text_only, "RESUME", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.carousel_text_only, "FINISH", 0, 1));
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        ResumeDialog dialog = new ResumeDialog("Resume Ride?", list, this);
        dialog.show(fm, "resume_dialog");
    }
    
    public void showFinishDialog() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        list.add(new ReconJetDialogFragment(R.layout.carousel_text_only, "CANCEL", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.carousel_text_only, "FINISH", 0, 1));
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        FinishDialog dialog = new FinishDialog("Finish Ride?", list, this);
        dialog.show(fm, "finish_dialog");
    }
    public void showSearchingGPSDialog() {
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        // search fragment by tag and then show or dismiss the dialog
        Fragment frg = fm.findFragmentByTag("search_dialog");
        if (frg == null) {
            SearchingGPSDialog dialog = new SearchingGPSDialog(this);
            dialog.show(fm.beginTransaction(), "search_dialog");
        }
    }
    
    public void dismissSearchingGPSDialog() {
        // search fragment by tag and then show or dismiss the dialog
        Fragment dialog = getSupportFragmentManager().findFragmentByTag("search_dialog");
        if (dialog != null) {
            DialogFragment df = (DialogFragment) dialog;
            df.dismissAllowingStateLoss();
            // reset the value to 0, 0 means don't need to show it again.
            // this property has been set back to 1 in ChooseActivityActivity@ReconItemHost
            Settings.System.putInt(getApplicationContext().getContentResolver(), "SHOWGPSOVERLAYIFNEEDED", 0);
        }
    }
    public void showStartActivityOverlay() {
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        // search fragment by tag and then show or dismiss the dialog
        Fragment frg = fm.findFragmentByTag("start_activity");
        if (frg == null) {
            StartActivityOverlay dialog = new StartActivityOverlay(this);
            dialog.show(fm.beginTransaction(), "start_activity");
        }
    }
    
    public void dismissStartActivityOverlay() {
        // search fragment by tag and then show or dismiss the dialog
        Fragment dialog = getSupportFragmentManager().findFragmentByTag("start_activity");
        if (dialog != null) {
            DialogFragment df = (DialogFragment) dialog;
            df.dismissAllowingStateLoss();
        }
    }


    public void nextDashboard(int increment) {
        int size = mDashFragments.size();
        if (size <= 0) return;
        mCurrentPosition = (mCurrentPosition + increment + size) % size;
        setDashboard(mCurrentPosition);
    }

    public void setDashboard(int position) {
        setDashboard(position, mShouldAutoRotate);
    }

    public void setDashboard(int position, boolean autoRotateToNext) {
        int size  = mDashFragments.size();
        if (size > 0) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            ft.setCustomAnimations(R.anim.slide_in_top, R.anim.fade_out_bottom);
            ft.replace(R.id.dashboard_frame, (Fragment)(mDashFragments.get(position)));
            ft.commitAllowingStateLoss();
        }
        if (autoRotateToNext) {
            mPanelRotator.removeCallbacks(mRotateToNext);
            mPanelRotator.postDelayed(mRotateToNext,mAutoRotatePeriod);
        }
    }

    private Runnable mRotateToNext = new Runnable() {
            @Override
            public void run() {
                nextDashboard(1);
            }
        };

    private void setAutoRotateRelatedFields() {
                
        mAutoRotatePeriod = SettingsUtil
            .getCachableSystemIntOrSet(this,
                                       SettingsUtil.SHOULD_AUTO_ROTATE,
                                       SettingsUtil.SHOULD_AUTO_ROTATE_DEFAULT)
            * 1000; // Make it seconds
        mShouldAutoRotate = (mAutoRotatePeriod != 0);
        
        //don't auto rotate if the activity hasn't been started
        if(MetricManager.getInstance(getApplicationContext()).getActivityState() == ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY){
            mShouldAutoRotate = false;
            return;
        }
        
        if (mShouldAutoRotate) {
            mPanelRotator.removeCallbacks(mRotateToNext);
            mPanelRotator.postDelayed(mRotateToNext,mAutoRotatePeriod);
        }
    }
}
