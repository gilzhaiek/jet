//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlivestats;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.mapsdk.mapview.MapOverlayActivity;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.utils.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

public class PostActivityMapOverlayActivity extends MapOverlayActivity {
        
        private static final String TAG = PostActivityMapOverlayActivity.class.getSimpleName();
        private JetDialog daySavedDialog;
        private FinishDayOverlay mFinishDayOverlay;
    private boolean mShouldQuit = false;

    @Override
    protected void setupLayout() {
        // the layout file should include R.id.map_activity_container which is
        // used in postOnCreate()
        setContentView(R.layout.day_summary_map_overlay); // empty FrameLayout
        daySavedDialog = new JetDialog(this, (DeviceUtils.isSun()) ? R.layout.jet_day_saved : R.layout.day_saved);
    }

    @Override
    protected void postCreate() {
        // Add the fragment to the UI frame
        //getSupportFragmentManager().beginTransaction()
        //        .add(R.id.map_activity_container, mMapFragment).commit();
    }
    
    protected void preCreate(){
            // set the trip data file name via intent
            //mFileName = getIntent().getStringExtra(MetricManager.EXTRA_TRIP_DATA_FILE_NAME);
        mFileName = "simpleLatLng.tmp.txt";
        }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
                showFinishDayOverlay();
                return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                        CommonUtils.scrollDown(this, generateIntent(), false);
                        return true;

                default:
                        break;
                }
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public void onBackPressed() {
        CommonUtils.launchParent(this, generateIntent(), true);
        super.onBackPressed();
    };
    
    @Override
    public void onResume() {
        mFileName = "simpleLatLng.tmp.txt";
        if(mShouldQuit) {
            continueLoadingMap(false);
            startActivity(new Intent("com.reconinstruments.itemhost"));
            finish();
        }
        super.onResume();
    }

    @Override
    protected void DrawBitmap(){
        super.DrawBitmap();
        if (mBitmap == null){
                 Log.e(TAG, "DrawBitmap bitmap=null.");
                 return;
        }
        Log.d(TAG, "DrawBitmap().");
        ImageView bitmapView = (ImageView) findViewById(R.id.map_image);
        bitmapView.setImageBitmap(mBitmap); 
        bitmapView.setVisibility(View.VISIBLE);
        
    }
    
    public void showFinishDayOverlay() {
        List<Fragment> list = new ArrayList<Fragment>();

        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Cancel", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Save & Finish", 0, 1));
        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Save & Share", 0, 2));
        list.add(new ReconJetDialogFragment(R.layout.smaller_font_title_body_carousel, "Discard", 0, 3));

        Fragment frg = getSupportFragmentManager().findFragmentByTag("finish_day");
        if (frg == null) {
            mFinishDayOverlay = null;
            if(DeviceUtils.isSun()){
                mFinishDayOverlay = new FinishDayOverlay("FINISH ACTIVITY?", list, R.layout.finish_day, this);
            } else {
                mFinishDayOverlay = new FinishDayOverlay("FINISH DAY?", list, R.layout.finish_day, this);
            }
            
            mFinishDayOverlay.show(getSupportFragmentManager().beginTransaction(), "finish_day");
        }
    }

    private Intent generateIntent(){
        Intent intent = new Intent(this, DaySummaryActivity.class);
        return intent;
    }

    // FIXME; Duplication from DaySummaryActivity
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
