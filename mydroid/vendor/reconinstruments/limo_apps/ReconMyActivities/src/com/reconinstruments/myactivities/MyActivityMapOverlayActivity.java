
package com.reconinstruments.myactivities;

import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.mapsdk.mapview.MapOverlayActivity;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.TranscendUtils;

public class MyActivityMapOverlayActivity extends MapOverlayActivity {
	
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                CommonUtils.scrollUp(this, generateIntent(), mStaticMapGenerator.isMissingTiles());
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                CommonUtils.scrollDown(this, generateIntent(), mStaticMapGenerator.isMissingTiles());
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MyActivitiesActivity.class);
        intent.putExtra(MyActivitiesActivity.EXTRA_ACTIVITY, getActivityType());
        CommonUtils.launchParent(this, intent, true);
    }

    @Override
    protected void setupLayout() {
        // the layout file should include R.id.map_activity_container which is
        // used in postOnCreate()
        setContentView(R.layout.my_activities_map_overlay); // empty FrameLayout
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
        mFileName = TranscendUtils.getLastTripDataFileName(getActivityType());
	}
    
    
    @Override
    public void onResume() {
        mFileName = TranscendUtils.getLastTripDataFileName(getActivityType());
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

    private Intent generateIntent(){
        Intent intent = new Intent(this, MyActivitiesDetailActivity.class);
        intent.putExtra(MyActivitiesDetailActivity.EXTRA_DETAIL, MyActivitiesDetailActivity.DetailType.LAST_ACTIVITY);
        intent.putExtra(MyActivitiesActivity.EXTRA_ACTIVITY, getActivityType());
        return intent;
    }
    
    public int getActivityType() {
        return getIntent().getIntExtra(MyActivitiesActivity.EXTRA_ACTIVITY,-1);
    }
}
