package com.reconinstruments.dashlauncher.livestats.widgets;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.ConversionUtil;
import com.reconinstruments.dashlauncher.ReconSettingsUtil;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ReconSpeedWidget4x2 extends ReconDashboardWidget {

	private TextView fieldTextView;
	private TextView unitTextView;
	
	public ReconSpeedWidget4x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}


	public ReconSpeedWidget4x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconSpeedWidget4x2(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	
	@Override
	public void updateInfo(Bundle fullinfo) {
	    	    if (fullinfo == null ) {
		return;
	    }

		Bundle tempBundle = (Bundle) fullinfo.get("SPEED_BUNDLE");
		float spd = tempBundle.getFloat("Speed");
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
			if(spd > -1) {
				fieldTextView.setText(Integer.toString((int)spd));
			} else {
				fieldTextView.setText("--");
			}
			unitTextView.setText(getContext().getResources().getString(R.string.kmh));
		} else {
			if(spd > -1) {
				fieldTextView.setText(Integer.toString((int)ConversionUtil.kmsToMiles(spd)));
			} else {
				fieldTextView.setText("--");
			}
			unitTextView.setText(getContext().getResources().getString(R.string.mph));
		}
	}


	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.speed);
		unitTextView = (TextView) findViewById(R.id.unit);
//		fieldTextView.setTypeface(livestats_widget_typeface);
//		unitTextView.setTypeface(livestats_widget_typeface);
	}

}
