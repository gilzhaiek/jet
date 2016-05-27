package com.reconinstruments.dashboard.widgets;

import com.reconinstruments.dashboard.ConversionUtil;
import com.reconinstruments.dashboard.R;
import com.reconinstruments.dashboard.ReconSettingsUtil;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ReconSpeedWidget6x4 extends ReconDashboardWidget {

	private TextView fieldTextView;
	private TextView unitTextView;
	private ImageView gaugeImageView;
	
	private static float GAUGE_TOP_END = 75;
	
	public ReconSpeedWidget6x4(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_speed_6x4, null);
        this.addView(myView);
        
        prepareInsideViews();
	}


	public ReconSpeedWidget6x4(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_speed_6x4, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconSpeedWidget6x4(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_speed_6x4, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	
	@Override
	public void updateInfo(Bundle fullinfo) {
		Bundle tempBundle = (Bundle) fullinfo.get("SPEED_BUNDLE");
		float spd = tempBundle.getFloat("Speed");
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
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
		
		float pctOfTopSpeed = spd / GAUGE_TOP_END;
		
		if (Float.compare(pctOfTopSpeed, (float) 0.0625) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_0_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.125) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_1_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.1875) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_2_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.25) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_3_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.3125) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_4_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.375) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_5_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.4375) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_6_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.5) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_7_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.5625) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_8_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.625) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_9_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.6875) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_10_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.75) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_11_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.8125) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_12_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.875) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_13_lrg);
		} else if (Float.compare(pctOfTopSpeed, (float) 0.9375) < 0) {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_14_lrg);
		} else {
			gaugeImageView.setImageResource(R.drawable.speed_gauge_15_lrg);
		}
	}


	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.speed);
		unitTextView = (TextView) findViewById(R.id.unit);
		gaugeImageView = (ImageView) findViewById(R.id.gauge);
		
		fieldTextView.setTypeface(recon_typeface);
		unitTextView.setTypeface(recon_typeface);
	}

}
