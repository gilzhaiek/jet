package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

import java.text.DecimalFormat;


public class ReconDistanceWidget2x2 extends ReconDashboardWidget {
	
	private TextView fieldTextView;
	
	public ReconDistanceWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_distance_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	public ReconDistanceWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_distance_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconDistanceWidget2x2(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_distance_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	
	@Override
	public void updateInfo(Bundle fullInfo, boolean inActivity) {
	    	    if (fullInfo == null ) {
		return;
	    }

		Bundle distBundle = (Bundle) fullInfo.get("DISTANCE_BUNDLE");
		float dst = distBundle.getFloat("Distance");
		
		DecimalFormat df = new DecimalFormat();
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
			if (dst > 10000) { // More than 10kms
				dst /= 1000;
							
				df.setMaximumFractionDigits(2);
				
				fieldTextView.setText(df.format(dst)+"km");
			} else {
				fieldTextView.setText(Integer.toString((int)dst)+"m");
			}
		} else {
			// IMPERIAL
			double ft = ConversionUtil.metersToFeet(dst);
			if (ft > 10000) { //More than 10,000 ft
				df.setMaximumFractionDigits(2);
				
				double miles = ConversionUtil.metersToMiles(dst);
				fieldTextView.setText(df.format(miles)+"mi");
			} else {
				df.setMaximumFractionDigits(0);
				fieldTextView.setText(df.format(ft)+"ft");
			}
		}
	}
	
	@Override
	public void prepareInsideViews() {
	    super.prepareInsideViews();
	}
}