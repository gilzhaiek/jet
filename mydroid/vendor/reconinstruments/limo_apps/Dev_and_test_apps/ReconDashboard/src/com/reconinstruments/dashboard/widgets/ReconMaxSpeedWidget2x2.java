package com.reconinstruments.dashboard.widgets;

import java.text.DecimalFormat;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.dashboard.ConversionUtil;
import com.reconinstruments.dashboard.R;
import com.reconinstruments.dashboard.ReconSettingsUtil;

public class ReconMaxSpeedWidget2x2 extends ReconDashboardWidget {
	
	private TextView fieldTextView;
	private TextView unitTextView;
	
	public ReconMaxSpeedWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_maxspeed_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	public ReconMaxSpeedWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_maxspeed_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconMaxSpeedWidget2x2(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_maxspeed_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	
	@Override
	public void updateInfo(Bundle fullInfo) {
		Bundle spdBundle = (Bundle) fullInfo.get("SPEED_BUNDLE");
		float spd = spdBundle.getFloat("MaxSpeed");
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
			fieldTextView.setText(df.format(spd));
			unitTextView.setText("km/h");
		} else {
			fieldTextView.setText(df.format(ConversionUtil.kmsToMiles(spd)));
			unitTextView.setText("mph");
		}
		
	}
	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.maxspd_field);
		fieldTextView.setTypeface(recon_typeface);
		unitTextView = (TextView) findViewById(R.id.unit_field);
		unitTextView.setTypeface(recon_typeface);
	}
}