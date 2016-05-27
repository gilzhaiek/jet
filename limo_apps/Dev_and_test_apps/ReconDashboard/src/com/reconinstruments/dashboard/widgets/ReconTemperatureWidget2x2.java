package com.reconinstruments.dashboard.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.dashboard.ConversionUtil;
import com.reconinstruments.dashboard.R;
import com.reconinstruments.dashboard.ReconSettingsUtil;

public class ReconTemperatureWidget2x2 extends ReconDashboardWidget {

	private TextView fieldTextView;

	public ReconTemperatureWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.recon_temperature_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconTemperatureWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.recon_temperature_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconTemperatureWidget2x2(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.recon_temperature_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	@Override
	public void updateInfo(Bundle fullinfo) {
		Bundle tempBundle = (Bundle) fullinfo.get("TEMPERATURE_BUNDLE");
		int temp = tempBundle.getInt("Temperature");
		
		if(temp < -273) {
			fieldTextView.setText("--");
			return;
		}
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
			fieldTextView.setText(Integer.toString(temp) + "\u00B0" + "C");
		} else {
			fieldTextView.setText(Integer.toString((int) ConversionUtil.celciusToFahrenheit(temp)) + "\u00B0" + "F");
		}
	}

	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.tmp_field);
		fieldTextView.setTypeface(this.recon_typeface);
	}

}
