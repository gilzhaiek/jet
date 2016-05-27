package com.reconinstruments.dashlauncher.livestats.widgets;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.applauncher.transcend.ReconChronoManager;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ReconChronoWidget4x1 extends ReconDashboardWidget {

	private static final String TAG = "ReconChronoWidget4x1";
	private TextView fieldTextView;
	
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
	public void updateInfo(Bundle fullinfo) {
	    if (fullinfo == null ) {
		return;
	    }

		//Log.v(TAG, "updateInfo()");
		
		Bundle chronoBundle = (Bundle) fullinfo.getBundle("CHRONO_BUNDLE");
		ArrayList<Bundle> trials = chronoBundle.getParcelableArrayList("Trials");
		Bundle lastTrial = trials.get(trials.size() -1 );
		
		boolean isRunning = lastTrial.getBoolean("IsRunning");
		//boolean hasRun = lastTrial.getBoolean("HasRun");
		Long elapsedTime = lastTrial.getLong("ElapsedTime");
		//Long startTime = lastTrial.getLong("StartTime");
		//Long endTime = lastTrial.getLong("EndTime");
		
		String time = ReconChronoManager.parseElapsedTime(elapsedTime, true);
		
		if (!isRunning) {
			time += ReconChronoManager.parseElapsedTime(elapsedTime, false);
		}
		
		fieldTextView.setText(time);
	}
	
	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.time_field);
//		fieldTextView.setTypeface(livestats_widget_typeface);
	}

}
