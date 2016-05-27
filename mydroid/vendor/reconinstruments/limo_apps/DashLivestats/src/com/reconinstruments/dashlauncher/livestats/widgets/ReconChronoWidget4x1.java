package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.stats.TranscendUtils;

import java.util.ArrayList;

public class ReconChronoWidget4x1 extends ReconDashboardWidget {

	private static final String TAG = "ReconChronoWidget4x1";
	
	public ReconChronoWidget4x1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_chrono_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}


	public ReconChronoWidget4x1(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_chrono_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconChronoWidget4x1(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_chrono_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
    }

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
	    if (fullinfo == null ) {
		return;
	    }

		//Log.v(TAG, "updateInfo()");
		
		Bundle chronoBundle = (Bundle) fullinfo.getBundle("CHRONO_BUNDLE");
		if (chronoBundle == null) {return;}
		ArrayList<Bundle> trials = chronoBundle.getParcelableArrayList("Trials");
		Bundle lastTrial = trials.get(trials.size() -1 );
		
		boolean isRunning = lastTrial.getBoolean("IsRunning");
		//boolean hasRun = lastTrial.getBoolean("HasRun");
		Long elapsedTime = lastTrial.getLong("ElapsedTime");
		//Long startTime = lastTrial.getLong("StartTime");
		//Long endTime = lastTrial.getLong("EndTime");
		
		String time = TranscendUtils.parseElapsedTime(elapsedTime, true);
		
		if (!isRunning) {
			time += TranscendUtils.parseElapsedTime(elapsedTime, false);
		}
		
		fieldTextView.setText(time);
	}
	
	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}

}
