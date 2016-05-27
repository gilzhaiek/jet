package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;

import java.text.DecimalFormat;

public class ReconMaxSpeedWidget4x1 extends ReconDashboardWidget {
	
	public ReconMaxSpeedWidget4x1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_maxspeed_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	public ReconMaxSpeedWidget4x1(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_maxspeed_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconMaxSpeedWidget4x1(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_maxspeed_4x1, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	
	@Override
	public void updateInfo(Bundle fullInfo, boolean inActivity) {
	    if (fullInfo == null ) {
		return;
	    }
		Bundle spdBundle = (Bundle) fullInfo.get("SPEED_BUNDLE");
		float spd = spdBundle.getFloat("MaxSpeed");
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
			fieldTextView.setText(df.format(spd)+"kmh");
		} else {
			fieldTextView.setText(df.format(ConversionUtil.kmsToMiles(spd)) + "mph");
		}
	}
	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}
}