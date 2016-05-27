package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

public class ReconTemperatureWidget2x2 extends ReconDashboardWidget {

	public ReconTemperatureWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_temperature_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconTemperatureWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_temperature_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconTemperatureWidget2x2(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_temperature_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
	    	    if (fullinfo == null ) {
		return;
	    }
		Bundle tempBundle = (Bundle) fullinfo.get("TEMPERATURE_BUNDLE");
		int temp = tempBundle.getInt("Temperature");
		
		if(temp < -273) {
			fieldTextView.setText("--");
			return;
		}
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
			fieldTextView.setText(Integer.toString(temp) + "\u00B0" + "C");
		} else {
			fieldTextView.setText(Integer.toString((int) ConversionUtil.celciusToFahrenheit(temp)) + "\u00B0" + "F");
		}
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}

}
