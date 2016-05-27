package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

public class ReconSpeedWidget4x3 extends ReconDashboardWidget {

	private static float GAUGE_TOP_END = 75;
	
	public ReconSpeedWidget4x3(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x3, null);
        this.addView(myView);
        
        prepareInsideViews();
	}


	public ReconSpeedWidget4x3(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x3, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconSpeedWidget4x3(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_speed_4x3, null);
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
			unitTextView.setText(getContext().getResources().getString(R.string.kmh));
		} else {
			if(spd > -1) {
				fieldTextView.setText(Integer.toString((int)ConversionUtil.kmsToMiles(spd)));
			} else {
				fieldTextView.setText("--");
			}
			unitTextView.setText(getContext().getResources().getString(R.string.mph));
		}
		
		float pctOfTopSpeed = spd / GAUGE_TOP_END;
		
//		if (Float.compare(pctOfTopSpeed, (float) 0.0625) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_0_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.125) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_1_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.1875) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_2_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.25) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_3_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.3125) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_4_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.375) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_5_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.4375) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_6_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.5) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_7_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.5625) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_8_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.625) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_9_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.6875) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_10_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.75) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_11_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.8125) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_12_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.875) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_13_med);
//		} else if (Float.compare(pctOfTopSpeed, (float) 0.9375) < 0) {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_14_med);
//		} else {
//			gaugeImageView.setImageResource(R.drawable.speed_gauge_15_med);
//		}
	}


	@Override
	public void prepareInsideViews() {
	    super.prepareInsideViews();
	}

}
