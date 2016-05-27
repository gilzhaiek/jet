
package com.reconinstruments.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.dashboard.MetricManager.MetricType;
import com.reconinstruments.utils.stats.ActivityUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>ActivitySummaryActivity</code> represents the activity summary screen.
 */
public class ActivitySummaryActivity extends CarouselItemHostActivity {
    private static final String TAG = ActivitySummaryActivity.class.getSimpleName();

    private Bundle bundle;
    private MetricManager mMetricManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.summary);
        
        bundle = getIntent().getBundleExtra(MetricManager.EXTRA_SUMMARY);
        
        mMetricManager = MetricManager.getInstance(getApplicationContext());
        
        initPager();
        mPager.setCurrentItem(0);
    }

    /**
     * init the fragments which represent every metric data on each fragment
     */
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        int i = 0;
        if(bundle != null){
            if(bundle.getString(MetricManager.MetricType.WATTAGE.name()) != null){
                fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.WATTAGE.name()), 0, i, "AVG. WATTAGE", bundle.getString(MetricManager.MetricType.WATTAGE.name()+"_UNIT")));
                i++;
            }
            if(mMetricManager.getSportType() != ActivityUtil.SPORTS_TYPE_RUNNING){
                if(bundle.getString(MetricManager.MetricType.SPEED.name()) != null){
                    fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.SPEED.name()), 0, i, "AVG. SPEED", bundle.getString(MetricManager.MetricType.SPEED.name()+"_UNIT")));
                    i++;
                }
            }else{
                if(bundle.getString(MetricManager.MetricType.PACE.name()) != null){
                    fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.PACE.name()), 0, i, "AVG. PACE", bundle.getString(MetricManager.MetricType.PACE.name()+"_UNIT")));
                    i++;
                }
            }
            if(bundle.getString(MetricManager.MetricType.DISTANCE.name()) != null){
                fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.DISTANCE.name()), 0, i, "DISTANCE", bundle.getString(MetricManager.MetricType.DISTANCE.name()+"_UNIT")));
                i++;
            }
            if(bundle.getString(MetricManager.MetricType.MOVINGDURATION.name()) != null){
                fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.MOVINGDURATION.name()), 0, i, "DURATION", bundle.getString(MetricManager.MetricType.MOVINGDURATION.name()+"_UNIT")));
                i++;
            }
//            if(bundle.getString(MetricManager.MetricType.TERRAINGRADE.name()) != null){
//                fList.add(new ActivitySummaryFragment(R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.TERRAINGRADE.name()), 0, i, "TERRAIN GRADE", bundle.getString(MetricManager.MetricType.TERRAINGRADE.name()+"_UNIT")));
//                i++;
//            }
            if(bundle.getString(MetricManager.MetricType.ELEVATIONGAIN.name()) != null){
                fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.ELEVATIONGAIN.name()), 0, i, "ELEV. GAIN", bundle.getString(MetricManager.MetricType.ELEVATIONGAIN.name()+"_UNIT")));
                i++;
            }
            if(bundle.getString(MetricManager.MetricType.HEARTRATE.name()) != null){
                fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.HEARTRATE.name()), 0, i, "AVG. HEARTRATE", bundle.getString(MetricManager.MetricType.HEARTRATE.name()+"_UNIT")));
                i++;
            }
            if(bundle.getString(MetricManager.MetricType.CADENCE.name()) != null){
                fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.CADENCE.name()), 0, i, "AVG. CADENCE", bundle.getString(MetricManager.MetricType.CADENCE.name()+"_UNIT")));
                i++;
            }
            if(bundle.getString(MetricManager.MetricType.CALORIE.name()) != null){
                fList.add(new ActivitySummaryFragment(this, R.layout.summary_fragment, bundle.getString(MetricManager.MetricType.CALORIE.name()), 0, i, "CALORIES", bundle.getString(MetricManager.MetricType.CALORIE.name()+"_UNIT")));
                i++;
            }
        }
        return fList;
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Intent intent = new Intent(this, ActivitySummaryMapOverlayActivity.class);
                intent.putExtra(MetricManager.EXTRA_TRIP_DATA_FILE_NAME, mMetricManager.getLastTripDataFileName());
                startActivity(intent);
                return true;
            case KeyEvent.KEYCODE_ENTER: // launch the save dialog
            case KeyEvent.KEYCODE_DPAD_CENTER:
                showSaveDialog();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void showSaveDialog() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        list.add(new ReconJetDialogFragment(R.layout.carousel_text_only, "SAVE", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.carousel_text_only, "DISCARD", 0, 1));
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        SaveDialog dialog = new SaveDialog("Save Activity?", list, this);
        dialog.show(fm, null);
    }
    
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
