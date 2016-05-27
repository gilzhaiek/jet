package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

import java.text.DecimalFormat;

public class ReconVrtWidget2x2 extends ReconDashboardWidget {

	public ReconVrtWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_vertical_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconVrtWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_vertical_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconVrtWidget2x2(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_vertical_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
	    	    if (fullinfo == null ) {
		return;
	    }

		Bundle vrtBundle = (Bundle) fullinfo.get("VERTICAL_BUNDLE");
		float vrt = vrtBundle.getFloat("Vert");
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);

		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
		    fieldTextView.setText(df.format(vrt)+"m");
		} else {
			// IMPERIAL
			double ft = ConversionUtil.metersToFeet(vrt);
			fieldTextView.setText(df.format(ft)+"ft");
		}
	}

	@Override
	public void prepareInsideViews() {
	    super.prepareInsideViews();
	}

}
