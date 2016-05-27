package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

public class ReconSpeedWidget4x4 extends ReconDashboardWidget {

	public ReconSpeedWidget4x4(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x4, null);
        this.addView(myView);
        
        prepareInsideViews();
	}


	public ReconSpeedWidget4x4(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x4, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconSpeedWidget4x4(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x4, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
	    if (fullinfo == null ) {
		return;
	    }

		Bundle tempBundle = (Bundle) fullinfo.get("SPEED_BUNDLE");
		float spd = tempBundle.getFloat("Speed");
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
			if(spd > -1) {
				fieldTextView.setText(Integer.toString((int)spd));
			} else {
				fieldTextView.setText("--");
			}
			unitTextView.setText("km/h");
		} else {
			if(spd > -1) {
				fieldTextView.setText(Integer.toString((int)ConversionUtil.kmsToMiles(spd)));
			} else {
				fieldTextView.setText("--");
			}
			unitTextView.setText("mph");
		}
	}


	@Override
	public void prepareInsideViews() {
	    super.prepareInsideViews();
	}

}
