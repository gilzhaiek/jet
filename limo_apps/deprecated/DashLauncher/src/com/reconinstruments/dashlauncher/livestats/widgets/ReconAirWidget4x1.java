package com.reconinstruments.dashlauncher.livestats.widgets;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;

public class ReconAirWidget4x1 extends ReconDashboardWidget {
	
	private TextView fieldTextView;
	
	public ReconAirWidget4x1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_air_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	public ReconAirWidget4x1(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_air_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconAirWidget4x1(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_air_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	
	@Override
	public void updateInfo(Bundle fullInfo) {
	    	    if (fullInfo == null ) {
		return;
	    }

		Bundle jumpBundle = (Bundle) fullInfo.get("JUMP_BUNDLE");
		ArrayList<Bundle> jumpBundles = jumpBundle.getParcelableArrayList("Jumps");
		
		if(jumpBundles.isEmpty()) {
			fieldTextView.setText("--");
		} else {
			Bundle jump = jumpBundles.get(jumpBundles.size() - 1);
			Double time = ((double) jump.getInt("Air")) / 1000;
			
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(1);
			df.setMinimumFractionDigits(1);
			
			fieldTextView.setText(df.format(time) + "s");
		}
	}
	
	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.air_field);
//		fieldTextView.setTypeface(livestats_widget_typeface);
	}
}