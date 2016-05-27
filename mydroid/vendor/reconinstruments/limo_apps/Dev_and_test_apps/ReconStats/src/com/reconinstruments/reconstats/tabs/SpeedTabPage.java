package com.reconinstruments.reconstats.tabs;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.reconsettings.ReconSettingsUtil;
import com.reconinstruments.reconstats.R;
import com.reconinstruments.reconstats.util.*;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class SpeedTabPage extends TabPage {

	private Context mContext;
	private View mTheBigView;
	
	private TextView titleTV, maxSpdTV, maxSpdFieldTV, maxSpdUnitTV, avgSpdTV, avgSpdFieldTV, avgSpdUnitTV;
	private TextView allTimeMaxSpdTV, allTimeMaxSpdFieldTV, allTimeMaxSpdUnitTV;
	
	public SpeedTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		
		mContext = context;
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTheBigView = inflater.inflate(R.layout.tab_speed_layout, null);

		this.addView(mTheBigView);
		
		// Populate view
		titleTV = (TextView) mTheBigView.findViewById(R.id.tab_title);
		
		maxSpdTV = (TextView) mTheBigView.findViewById(R.id.max_speed);
		maxSpdFieldTV = (TextView) mTheBigView.findViewById(R.id.max_speed_field);
		maxSpdUnitTV = (TextView) mTheBigView.findViewById(R.id.max_speed_field_unit);
		
		avgSpdTV = (TextView) mTheBigView.findViewById(R.id.avg_speed);
		avgSpdFieldTV = (TextView) mTheBigView.findViewById(R.id.avg_speed_field);
		avgSpdUnitTV = (TextView) mTheBigView.findViewById(R.id.avg_speed_field_unit);
		
		allTimeMaxSpdTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_speed);
		allTimeMaxSpdFieldTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_speed_field);
		allTimeMaxSpdUnitTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_speed_field_unit);
		
		// Set Font
		Typeface tf = FontSingleton.getInstance(mContext).getTypeface();
		titleTV.setTypeface(tf);
		
		maxSpdTV.setTypeface(tf);
		maxSpdFieldTV.setTypeface(tf);
		maxSpdUnitTV.setTypeface(tf);
		
		avgSpdTV.setTypeface(tf);
		avgSpdFieldTV.setTypeface(tf);
		avgSpdUnitTV.setTypeface(tf);
		
		allTimeMaxSpdTV.setTypeface(tf);
		allTimeMaxSpdFieldTV.setTypeface(tf);
		allTimeMaxSpdUnitTV.setTypeface(tf);
		
		titleTV.setText("SPEED");	
	}

	public void setData(Bundle data) {
		Bundle speedBundle = data.getBundle("SPEED_BUNDLE");
		
		if(speedBundle == null)
			return;
		
		boolean metric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UINTS_METRIC;
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		df.setMinimumFractionDigits(0);
		
		if(metric) {
			maxSpdFieldTV.setText(df.format(speedBundle.getFloat("MaxSpeed")));
			maxSpdUnitTV.setText("km/h");
			
			avgSpdFieldTV.setText(df.format(speedBundle.getFloat("AverageSpeed")));
			avgSpdUnitTV.setText("km/h");
			
			allTimeMaxSpdFieldTV.setText(df.format(speedBundle.getFloat("AllTimeMaxSpeed")));
			allTimeMaxSpdUnitTV.setText("km/h");
		} else {
			float maxSpd = (float) ConversionUtil.kmsToMiles(speedBundle.getFloat("MaxSpeed"));
			maxSpdFieldTV.setText(df.format(maxSpd));
			maxSpdUnitTV.setText("mph");
			
			float avgSpd = (float) speedBundle.getFloat("AverageSpeed");
			avgSpdFieldTV.setText(df.format(avgSpd));
			avgSpdUnitTV.setText("mph");
			
			float allTimeMaxSpd = (float) speedBundle.getFloat("AllTimeMaxSpeed");
			allTimeMaxSpdFieldTV.setText(df.format(allTimeMaxSpd));
			allTimeMaxSpdUnitTV.setText("mph");
		}
	}
}
