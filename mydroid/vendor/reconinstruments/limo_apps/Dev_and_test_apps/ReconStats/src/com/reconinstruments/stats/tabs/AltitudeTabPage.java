package com.reconinstruments.stats.tabs;

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
import com.reconinstruments.stats.util.ConversionUtil;
import com.reconinstruments.stats.util.FontSingleton;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class AltitudeTabPage extends TabPage {

	private Context mContext;
	private View mTheBigView;
	
	private TextView titleTV, maxAltTV, maxAltFieldTV, maxAltUnitTV, minAltTV, minAltFieldTV, minAltUnitTV;
	private TextView allTimeMaxAltTV, allTimeMaxAltFieldTV, allTimeMaxAltUnitTV;
	
	public AltitudeTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		
		mContext = context;
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTheBigView = inflater.inflate(R.layout.tab_altitude_layout, null);

		this.addView(mTheBigView);
		
		// Populate view
		titleTV = (TextView) mTheBigView.findViewById(R.id.tab_title);
		
		maxAltTV = (TextView) mTheBigView.findViewById(R.id.max_alt);
		maxAltFieldTV = (TextView) mTheBigView.findViewById(R.id.max_alt_field);
		maxAltUnitTV = (TextView) mTheBigView.findViewById(R.id.max_alt_field_unit);
		
		minAltTV = (TextView) mTheBigView.findViewById(R.id.min_alt);
		minAltFieldTV = (TextView) mTheBigView.findViewById(R.id.min_alt_field);
		minAltUnitTV = (TextView) mTheBigView.findViewById(R.id.min_alt_field_unit);
		
		allTimeMaxAltTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_alt);
		allTimeMaxAltFieldTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_alt_field);
		allTimeMaxAltUnitTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_alt_field_unit);
		
		// Set Font
		Typeface tf = FontSingleton.getInstance(mContext).getTypeface();
		titleTV.setTypeface(tf);
		
		maxAltTV.setTypeface(tf);
		maxAltFieldTV.setTypeface(tf);
		maxAltUnitTV.setTypeface(tf);
		
		minAltTV.setTypeface(tf);
		minAltFieldTV.setTypeface(tf);
		minAltUnitTV.setTypeface(tf);
		
		allTimeMaxAltTV.setTypeface(tf);
		allTimeMaxAltFieldTV.setTypeface(tf);
		allTimeMaxAltUnitTV.setTypeface(tf);
		
		titleTV.setText("ALTITUDE");	
	}

	public void setData(Bundle data) {
		Bundle altitudeBundle = data.getBundle("ALTITUDE_BUNDLE");
		
		if(altitudeBundle == null)
			return;
		
		boolean metric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UINTS_METRIC;
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		df.setMinimumFractionDigits(0);
		
		if(metric) {
			maxAltFieldTV.setText(df.format(altitudeBundle.getFloat("MaxAlt")));
			maxAltUnitTV.setText("m");
			
			minAltFieldTV.setText(df.format(altitudeBundle.getFloat("MinAlt")));
			minAltUnitTV.setText("m");
			
			allTimeMaxAltFieldTV.setText(df.format(altitudeBundle.getFloat("AllTimeMaxAlt")));
			allTimeMaxAltUnitTV.setText("m");
		} else {
			float maxAlt = altitudeBundle.getFloat("MaxAlt");
			maxAltFieldTV.setText(df.format(ConversionUtil.metersToFeet(maxAlt)));
			maxAltUnitTV.setText("ft");
			
			float minAlt = altitudeBundle.getFloat("MinAlt");
			minAltFieldTV.setText(df.format(ConversionUtil.metersToFeet(minAlt)));
			minAltUnitTV.setText("ft");
			
			float allTimeMaxAlt = altitudeBundle.getFloat("AllTimeMaxAlt");
			allTimeMaxAltFieldTV.setText(df.format(ConversionUtil.metersToFeet(allTimeMaxAlt)));
			allTimeMaxAltUnitTV.setText("ft");
		}
	}
}
