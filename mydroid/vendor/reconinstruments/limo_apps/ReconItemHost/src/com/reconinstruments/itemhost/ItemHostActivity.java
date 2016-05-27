//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.itemhost;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.UIUtils;

/**
 * <code>ItemHostActivity</code> is the main activity to host the items. It's
 * implemented by ViewPager with Fragment. RemoteView generated from the
 * <code>ItemHostService</code> would be represented in fragment.
 */
public class ItemHostActivity extends CarouselItemHostActivity implements ImageGetter {

    private static final String TAG = ItemHostActivity.class.getSimpleName();
    private static final String SHOW_TUTORIAL = "com.reconinstruments.itemhost.SHOW_TUTORIAL";
    
    // a list to hold all of the remote view tags.
    private List<String> remoteViewTags = new ArrayList<String>();
    
    // a list to hold all of the fragment which shows the remote view.
    private List<Fragment> fList = new ArrayList<Fragment>();
    private WarningOverlay mWarningOverlay;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_host);
        
        //init fragments by pre defined remote view
        remoteViewTags.add(ItemHostService.TAG_APPS);
        remoteViewTags.add(ItemHostService.TAG_ACTIVITIES);
        remoteViewTags.add(ItemHostService.TAG_NEW_ACTIVITY);
        remoteViewTags.add(ItemHostService.TAG_NOTIFICATIONS);
        remoteViewTags.add(ItemHostService.TAG_SETTINGS);
        initPager();
        mPager.setCurrentItem(2);
        
        // pager settings can be adjusted from defaults here
        mPager.setOffscreenPageLimit(remoteViewTags.size());
        mPager.setPadding(60,0,60,0);
        

        // enable LAUNCH_RECONNECT then attempt to reconnect at system startup
        // if it doesn't need to reconnect, it will be finished simply
        Log.d(TAG,"enable LAUNCH_RECONNECT");
        Settings.System.putInt(getApplicationContext().getContentResolver(), "LAUNCH_RECONNECT", 1);
        
        showOverlayIfNeeded();
    }
    

    /**
     *  <code>showOverlayIfNeeded</code> Deals with the logic of
     *  showing warnings to user regarding their ongoing activities
     *  before the last shutdown and possible battery removal and time
     *  loss.
     */
    private void showOverlayIfNeeded(){
        if(mWarningOverlay == null){
            mWarningOverlay = WarningOverlay.showWarningOverlayIfNeeded(this);
        }
        if(mWarningOverlay != null){
            mWarningOverlay.setOnKeyListener(new Dialog.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface arg0, int keyCode,
                        KeyEvent event) {
                    if (event.getAction() != KeyEvent.ACTION_UP) return false;
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        arg0.dismiss();
                        postCreate();
                    }
                    return true;
                }
            });
        }else{
            postCreate();
        }
    }
    
    private void postCreate(){
        // Video playback logic
        if(performVideoPlaybackLogic()){
                         UIUtils.setButtonHoldShouldLaunchApp(this,false);
        }
        else if(BTHelper.getInstance(getApplicationContext()).shouldAutoReconnect()){ // auto reconnecting
             Intent i = new Intent("com.reconinstruments.connectdevice.RECONNECT");
             i.putExtra("SHOULD_AUTO_RECONNECT", true); // The other option is "manual"
             // startActivity(i);
             CommonUtils.launchNew(this,i,false);
         }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, ItemHostService.class));
        registerReceiver(remoteViewReceiver, new IntentFilter(ItemHostService.INTENT_ACTION));
        registerReceiver(mTutorialReceiver, new IntentFilter(SHOW_TUTORIAL));
    }

    private void setInHome(boolean inHome){
        System.setProperty("inHome", String.valueOf(inHome));
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        setInHome(false);
        
        ((CarouselItemPageAdapter) mPager.getAdapter()).hideBreadcrumbs();  //Hides breadcrumbs
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        setInHome(true);
        broadcastStartOrStopPullingDurationRequest();
        
        if(Settings.System.getInt(getContentResolver(), "com.reconinstruments.itemhost.SHOULD_SHOW_COACHMARKS", 0) == 1){
                Intent showCoachmarksIntent = new Intent();
                showCoachmarksIntent.setAction(SHOW_TUTORIAL);
                sendBroadcast(showCoachmarksIntent);
        }
        overridePendingTransition(R.anim.fade_slide_in_top,0); //animation for re-entry to main menu

    }
    
    @Override
    protected void onStop() {
        broadcastStopPullingDurationRequest();
        try {
            unregisterReceiver(remoteViewReceiver);
            unregisterReceiver(mTutorialReceiver);
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                e.printStackTrace();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, ItemHostService.class));
        super.onDestroy();
    }
    
    // the receiver receives the remote view from ItemHostService and then apply
    // to one of item content.
    private final BroadcastReceiver remoteViewReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                RemoteViews views = (RemoteViews) intent.getExtras().getParcelable(ItemHostService.EXTRA_VIEW);
                String tag = intent.getExtras()
                    .getString(ItemHostService.EXTRA_TAG);
                Intent upLauncher = intent.getExtras().getParcelable(ItemHostService.EXTRA_UP_LAUNCHER);
                Intent downLauncher = intent.getExtras().getParcelable(ItemHostService.EXTRA_DOWN_LAUNCHER);
            
                if(remoteViewTags.contains(tag)){//update the remote view only
                    ItemHostFragment fragment = (ItemHostFragment)(fList.get(remoteViewTags.indexOf(tag)));
                    fragment.setupRemoteView(context, views, upLauncher, downLauncher);
                }else{//add the new remote view
                    remoteViewTags.add(tag);
                    fList.add(new ItemHostFragment(R.layout.item_host_item, "", R.drawable.newactivity_icon, remoteViewTags.size() - 1));
                    initPager();
                    ItemHostFragment fragment = (ItemHostFragment)(fList.get(fList.size() - 1));
                    fragment.setupRemoteView(context, views, upLauncher, downLauncher);
                }
            }
        };

    protected List<Fragment> getFragments() {
        // init fragments by pre defined remote view count
        int icon = DeviceUtils.isSnow2() ?
            R.drawable.snow: R.drawable.newactivity_icon;
        for(int i = 0; i < remoteViewTags.size(); i++){
            fList.add(new ItemHostFragment(R.layout.item_host_item, "", icon, i));
        }
        return fList;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            broadcastStartOrStopPullingDurationRequest();
            return true;
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            broadcastStopPullingDurationRequest();
            Intent downLauncher = ((ItemHostFragment)((CarouselItemPageAdapter)mPager.getAdapter()).getItem(mPager.getCurrentItem())).getDownLauncher();
            if (downLauncher == null) return super.onKeyUp(keyCode, event);
            if (downLauncher.hasExtra(ItemHostService.COLUMN_HANDLER_EXTRA)) {
                startService(downLauncher);
            }
            else {
                CommonUtils.launchNew(this, downLauncher, false);
            }
            return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
    }
    
    /**
     * when it stays on the center position, launch smartphone connection. otherwise switch to the center position.
     */
    @Override
    public void onBackPressed() {
        if(!ItemHostService.TAG_NEW_ACTIVITY.equals(remoteViewTags.get(mPager.getCurrentItem()))){
            broadcastStartPullingDurationRequest();
            mPager.setCurrentItem((int)(remoteViewTags.size()/2));
        }
    }
    
    /*
     * the following methods send broadcast message to itemhost service to request starting or stopping 
     * pulling duration
     */
    private void broadcastStartPullingDurationRequest(){
        Intent intent = new Intent(ItemHostService.INTENT_COMMAND);
        intent.putExtra(ItemHostService.EXTRA_COMMAND, ItemHostService.COMMAND_START_PULLING);
        sendBroadcast(intent);
    }
    
    private void broadcastStopPullingDurationRequest(){
        Intent intent = new Intent(ItemHostService.INTENT_COMMAND);
        intent.putExtra(ItemHostService.EXTRA_COMMAND, ItemHostService.COMMAND_STOP_PULLING);
        sendBroadcast(intent);
    }
    
    private void broadcastStartOrStopPullingDurationRequest(){
        if(ItemHostService.TAG_NEW_ACTIVITY.equals(remoteViewTags.get(mPager.getCurrentItem()))){
            broadcastStartPullingDurationRequest();
        }else{
            broadcastStopPullingDurationRequest();
        }
    }
    
    BroadcastReceiver mTutorialReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
                showCoachMarks();
        }
    };
    
    /**
     * 
     * @return true if this is the first time playing the video
     */
    private boolean performVideoPlaybackLogic() {
        Log.v(TAG,"performVideoPlaybackLogic");

        // Intro Video
        boolean videoPlayed = false;
        boolean alwaysPlayVideo = false;
        try {
            videoPlayed = Settings.System.getInt(this.getContentResolver(), "INTRO_VIDEO_PLAYED", 0) == 1;
            alwaysPlayVideo = Settings.System.getInt(getContentResolver(), "INTRO_VIDEO_ALWAYS_PLAY", 0) == 1;
        } catch(Exception e) {
            Log.e(TAG, e.toString());
        }

        Log.v(TAG, "videoPlayed: " + videoPlayed);
        Log.v(TAG, "alwaysPlayVideo: " + alwaysPlayVideo);

        try {
            if (!videoPlayed || alwaysPlayVideo) {
                Intent i = DeviceUtils.isSun()?
                    new Intent("com.reconinstruments.QuickstartGuide.QUICKSTART_GUIDE"):
                    new Intent("com.reconinstruments.QuickstartGuide.SNOW2_WELCOME");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void showCoachMarks(){
        Settings.System.putInt(getContentResolver(), "com.reconinstruments.itemhost.SHOULD_SHOW_COACHMARKS", 0);
                UIUtils.setButtonHoldShouldLaunchApp(this,true);
        ItemHostHelpDialog navigateHelp = ItemHostHelpDialog.newInstance();
        navigateHelp.show(getSupportFragmentManager(), "item_host_help");
    }

    @Override
    public Drawable getDrawable(String source){
                 int id;
             if(source.equals("info_icon.png")){
                 id = R.drawable.info_icon;
             } else if(source.equals("select_snow2.png")){
                 id = R.drawable.select_snow2;
             } else if(source.equals("select_jet.png")){
                 id = R.drawable.select_jet;
             } else {
                 return null;
             }
             LevelListDrawable d = new LevelListDrawable();
             Drawable empty = getResources()
                     .getDrawable(id);
             d.addLevel(0, 0, empty);
             d.setBounds(0, 0,
                     empty.getIntrinsicWidth(),
                     empty.getIntrinsicHeight());
             return d;
    }
    
}
