package com.reconinstruments.dashboard.widgets;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.dashboard.R;

public class ReconAirWidget2x2 extends ReconDashboardWidget {
	
	private TextView fieldTextView;
	
	public ReconAirWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_air_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	public ReconAirWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_air_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconAirWidget2x2(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.recon_air_2x2, null);
        this.addView(myView);
        
        prepareInsideViews();
	}
	
	@Override
	public void updateInfo(Bundle fullInfo) {
		Bundle jumpBundle = (Bundle) fullInfo.get("JUMP_BUNDLE");
		ArrayList<Bundle> jumpBundles = jumpBundle.getParcelableArrayList("Jumps");
		
		if(jumpBundles.isEmpty()) {
			fieldTextView.setText("--");
		} else {
			Bundle jump = jumpBundles.get(jumpBundles.size() - 1);
			Double time = ((double) jump.getInt("Air")) / 1000;
			
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(1);
			df.setMinimumFractionDigits(1);
			
			fieldTextView.setText(df.format(time) + "s");
		}
	}
	
	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.air_field);
		fieldTextView.setTypeface(recon_typeface);
	}
}