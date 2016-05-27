package com.reconinstruments.dashlauncher.livestats.widgets;

import java.text.DecimalFormat;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.ConversionUtil;
import com.reconinstruments.dashlauncher.ReconSettingsUtil;

public class ReconVrtWidget2x2 extends ReconDashboardWidget {

	private TextView fieldTextView;

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
	public void updateInfo(Bundle fullinfo) {
	    	    if (fullinfo == null ) {
		return;
	    }

		Bundle vrtBundle = (Bundle) fullinfo.get("VERTICAL_BUNDLE");
		float vrt = vrtBundle.getFloat("Vert");
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);

		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
		    fieldTextView.setText(df.format(vrt)+"m");
		} else {
			// IMPERIAL
			double ft = ConversionUtil.metersToFeet(vrt);
			fieldTextView.setText(df.format(ft)+"ft");
		}
	}

	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.vrt_field);
//		fieldTextView.setTypeface(livestats_widget_typeface);
	}

}
