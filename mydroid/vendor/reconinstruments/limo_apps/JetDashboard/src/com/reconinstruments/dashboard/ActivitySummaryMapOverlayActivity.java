
package com.reconinstruments.dashboard;

import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import android.support.v4.app.FragmentManager;

import com.reconinstruments.commonwidgets.MapOverlayActivity;
import com.reconinstruments.dashboard.R;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ActivitySummaryMapOverlayActivity extends MapOverlayActivity {

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Intent intent = new Intent(this, ActivitySummaryActivity.class);
                intent.putExtra(MetricManager.EXTRA_SUMMARY,
                        MetricManager.getInstance(this.getApplicationContext()).getLatestData());
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
    protected void setupLayout() {
        // the layout file should include R.id.map_activity_container which is
        // used in postOnCreate()
        setContentView(R.layout.post_activity_map_overlay); // empty FrameLayout
    }

    @Override
    protected void preCreate(){
    	
        // set the trip data file name via intent
        mFileName = getIntent().getStringExtra(MetricManager.EXTRA_TRIP_DATA_FILE_NAME);
    }
    
    @Override
    public void onResume() {


        mFileName = getIntent().getStringExtra(MetricManager.EXTRA_TRIP_DATA_FILE_NAME);
        
        super.onResume();


    }
    
    @Override
    protected void postCreate() {
        
    	// Add the fragment to the UI frame
    	//getSupportFragmentManager().beginTransaction().add( R.id.map_image, mMapFragment ).commit();
    }
    
    @Override
    public void onBackPressed() {
    	Intent intent = new Intent(this, DashboardActivity.class);
    	startActivity(intent);
    }
 

    @Override
	protected void DrawBitmap(){
		
		/*TextView textView = ((TextView) findViewById(R.id.text_label));
		textView.setText("Tile created.");
		textView.setVisibility(View.VISIBLE);*/
		if (mBitmap == null){
			 Log.e(TAG, "DrawBitmap bitmap=null.");
			 return;
		}
		Log.d(TAG, "DrawBitmap().");
		ImageView bitmapView = (ImageView) findViewById(R.id.map_image);
		bitmapView.setImageBitmap(mBitmap); 
		bitmapView.setVisibility(View.VISIBLE);
		
	}
}
