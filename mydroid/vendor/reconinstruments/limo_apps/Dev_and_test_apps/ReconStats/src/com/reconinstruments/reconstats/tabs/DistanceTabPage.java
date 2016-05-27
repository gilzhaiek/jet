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

public class DistanceTabPage extends TabPage {

	private Context mContext;
	private View mTheBigView;
	
	private TextView titleTV, totalDistTV, totalDistFieldTV, totalDistUnitTV;
	private TextView allTimeDistTV, allTimeDistFieldTV, allTimeDistUnitTV;
	
	public DistanceTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		
		mContext = context;
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTheBigView = inflater.inflate(R.layout.tab_distance_layout, null);

		this.addView(mTheBigView);
		
		// Populate view
		titleTV = (TextView) mTheBigView.findViewById(R.id.tab_title);
		
		totalDistTV = (TextView) mTheBigView.findViewById(R.id.total_distance);
		totalDistFieldTV = (TextView) mTheBigView.findViewById(R.id.total_distance_field);
		totalDistUnitTV = (TextView) mTheBigView.findViewById(R.id.total_distance_field_unit);
		
		allTimeDistTV = (TextView) mTheBigView.findViewById(R.id.all_time_distance);
		allTimeDistFieldTV = (TextView) mTheBigView.findViewById(R.id.all_time_distance_field);
		allTimeDistUnitTV = (TextView) mTheBigView.findViewById(R.id.all_time_distance_field_unit);
		
		// Set Font
		Typeface tf = FontSingleton.getInstance(mContext).getTypeface();
		titleTV.setTypeface(tf);
		
		totalDistTV.setTypeface(tf);
		totalDistFieldTV.setTypeface(tf);
		totalDistUnitTV.setTypeface(tf);
		
		allTimeDistTV.setTypeface(tf);
		allTimeDistFieldTV.setTypeface(tf);
		allTimeDistUnitTV.setTypeface(tf);
		
		titleTV.setText("DISTANCE");		
	}

	public void setData(Bundle data) {
		Bundle distanceBundle = data.getBundle("DISTANCE_BUNDLE");
		
		if(distanceBundle == null)
			return;
		
		boolean metric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UINTS_METRIC;
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		df.setMinimumFractionDigits(0);
		
		if(metric) {
			float dist = distanceBundle.getFloat("Distance");
			if(dist < 10000) {
				df.setMaximumFractionDigits(0);
				df.setMinimumFractionDigits(0);
				totalDistFieldTV.setText(df.format(dist));
				totalDistUnitTV.setText("m");
			} else {
				float distKm = dist / 1000;
				df.setMaximumFractionDigits(2);
				df.setMinimumFractionDigits(2);
				totalDistFieldTV.setText(df.format(distKm));
				totalDistUnitTV.setText("km");
			}
			
			float allTimeDist = distanceBundle.getFloat("AllTimeDistance");
			if(allTimeDist < 10000) {
				df.setMaximumFractionDigits(0);
				df.setMinimumFractionDigits(0);
				allTimeDistFieldTV.setText(df.format(allTimeDist));
				allTimeDistUnitTV.setText("m");
			} else {
				float allTimeDistKm = allTimeDist / 1000;
				df.setMaximumFractionDigits(2);
				df.setMinimumFractionDigits(2);
				allTimeDistFieldTV.setText(df.format(allTimeDistKm));
				allTimeDistUnitTV.setText("km");
			}
			
		} else {
			float dist = distanceBundle.getFloat("Distance");
			if(dist < 10000) {
				float distFt = (float) ConversionUtil.metersToFeet(dist);
				df.setMaximumFractionDigits(0);
				df.setMinimumFractionDigits(0);
				totalDistFieldTV.setText(df.format(distFt));
				totalDistUnitTV.setText("ft");
			} else {
				float distMi = (float) ConversionUtil.metersToMiles(dist);
				df.setMaximumFractionDigits(2);
				df.setMinimumFractionDigits(2);
				totalDistFieldTV.setText(df.format(distMi));
				totalDistUnitTV.setText("mi");
			}
			
			float allTimeDist = distanceBundle.getFloat("AllTimeDistance");
			if(allTimeDist < 10000) {
				float allTimeDistFt = (float) ConversionUtil.metersToFeet(allTimeDist);
				df.setMaximumFractionDigits(0);
				df.setMinimumFractionDigits(0);
				allTimeDistFieldTV.setText(df.format(allTimeDistFt));
				allTimeDistUnitTV.setText("ft");
			} else {
				float allTimeDistMi = (float) ConversionUtil.metersToMiles(allTimeDist);
				df.setMaximumFractionDigits(2);
				df.setMinimumFractionDigits(2);
				allTimeDistFieldTV.setText(df.format(allTimeDistMi));
				allTimeDistUnitTV.setText("mi");
			}
		}
	}
}
