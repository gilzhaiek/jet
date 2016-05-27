package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.ConversionUtil;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.ReconSettingsUtil;

public class ReconAltWidget2x2 extends ReconDashboardWidget {
	private static String TAG = "ReconAltWidget2x2";
	private TextView fieldTextView;
	
	public ReconAltWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_altitude_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	public ReconAltWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_altitude_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconAltWidget2x2(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_altitude_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	
	@Override
	public void updateInfo(Bundle fullInfo) {
	    	    if (fullInfo == null ) {
		return;
	    }

		Bundle altBundle = (Bundle) fullInfo.get("ALTITUDE_BUNDLE");
		float alt = altBundle.getFloat("Alt");
		boolean isUncertain = altBundle.getInt("HeightOffsetN") < 500;
		
		if(isUncertain) {
			fieldTextView.setText("...");
		} else {
			if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
				fieldTextView.setText(Integer.toString((int)alt)+"m");
			} else {
				// write imperial units
				fieldTextView.setText(Integer.toString((int) ConversionUtil.metersToFeet((double) alt)) + "ft");
			}
		}
	}
	
	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.alt_field);
//		fieldTextView.setTypeface(livestats_widget_typeface);
	}
}