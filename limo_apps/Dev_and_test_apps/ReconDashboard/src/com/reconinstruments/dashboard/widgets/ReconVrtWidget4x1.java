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

public class ReconVrtWidget4x1 extends ReconDashboardWidget {

	private TextView fieldTextView;

	public ReconVrtWidget4x1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.recon_vertical_4x1, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconVrtWidget4x1(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.recon_vertical_4x1, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconVrtWidget4x1(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.recon_vertical_4x1, null);
		this.addView(myView);

		prepareInsideViews();
	}

	@Override
	public void updateInfo(Bundle fullinfo) {
		Bundle vrtBundle = (Bundle) fullinfo.get("VERTICAL_BUNDLE");
		float vrt = vrtBundle.getFloat("Vert");
		
		DecimalFormat df = new DecimalFormat();
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
			fieldTextView.setText(Integer.toString((int)vrt)+"m");
		} else {
			// IMPERIAL
			double ft = ConversionUtil.metersToFeet(vrt);
			df.setMaximumFractionDigits(0);
			fieldTextView.setText(df.format(ft)+"ft");
		}
	}

	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.vrt_field);
		fieldTextView.setTypeface(recon_typeface);
	}

}
