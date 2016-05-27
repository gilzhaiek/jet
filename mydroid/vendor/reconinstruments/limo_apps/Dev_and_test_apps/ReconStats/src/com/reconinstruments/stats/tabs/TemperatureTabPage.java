package com.reconinstruments.stats.tabs;

import java.text.DecimalFormat;
import java.util.ArrayList;

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

public class TemperatureTabPage extends TabPage {

	private Context mContext;
	private View mTheBigView;
	
	private TextView titleTV;
	private TextView maxTempTV, maxTempFieldTV, maxTempUnitTV, minTempTV, minTempFieldTV, minTempUnitTV,
		allTimeMinTempTV, allTimeMinTempFieldTV, allTimeMinTempUnitTV, 
		allTimeMaxTempTV, allTimeMaxTempFieldTV, allTimeMaxTempUnitTV;
	
	public TemperatureTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		
		mContext = context;
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTheBigView = inflater.inflate(R.layout.tab_temperature_layout, null);

		this.addView(mTheBigView);
		
		// Populate view
		titleTV = (TextView) mTheBigView.findViewById(R.id.tab_title);
		
		maxTempTV = (TextView) mTheBigView.findViewById(R.id.max_temp);
		maxTempFieldTV = (TextView) mTheBigView.findViewById(R.id.max_temp_field);
		maxTempUnitTV = (TextView) mTheBigView.findViewById(R.id.max_temp_field_unit);
		
		minTempTV = (TextView) mTheBigView.findViewById(R.id.min_temp);
		minTempFieldTV = (TextView) mTheBigView.findViewById(R.id.min_temp_field);
		minTempUnitTV = (TextView) mTheBigView.findViewById(R.id.min_temp_field_unit);
		
		allTimeMaxTempTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_temp);
		allTimeMaxTempFieldTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_temp_field);
		allTimeMaxTempUnitTV = (TextView) mTheBigView.findViewById(R.id.all_time_max_temp_field_unit);
		
		allTimeMinTempTV = (TextView) mTheBigView.findViewById(R.id.all_time_min_temp);
		allTimeMinTempFieldTV = (TextView) mTheBigView.findViewById(R.id.all_time_min_temp_field);
		allTimeMinTempUnitTV = (TextView) mTheBigView.findViewById(R.id.all_time_min_temp_field_unit);
		
		// Set Font
		Typeface tf = FontSingleton.getInstance(mContext).getTypeface();
		titleTV.setTypeface(tf);
		
		maxTempTV.setTypeface(tf);
		maxTempFieldTV.setTypeface(tf);
		maxTempUnitTV.setTypeface(tf);
		
		minTempTV.setTypeface(tf);
		minTempFieldTV.setTypeface(tf);
		minTempUnitTV.setTypeface(tf);
		
		allTimeMinTempTV.setTypeface(tf);
		allTimeMinTempFieldTV.setTypeface(tf);
		allTimeMinTempUnitTV.setTypeface(tf);
		
		allTimeMaxTempTV.setTypeface(tf);
		allTimeMaxTempFieldTV.setTypeface(tf);
		allTimeMaxTempUnitTV.setTypeface(tf);
		
		titleTV.setText("TEMPERATURE");	
	}

	public void setData(Bundle data) {
		Bundle tempBundle = data.getBundle("TEMPERATURE_BUNDLE");
		
		if(tempBundle == null) return;
		
		boolean metric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UINTS_METRIC;
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		df.setMinimumFractionDigits(0);
		
		String celcius = "\u00B0" + "C";
		String fahrenheit = "\u00B0" + "F";
		
		if(metric) {
			maxTempFieldTV.setText(Integer.toString(tempBundle.getInt("MaxTemperature")));
			maxTempUnitTV.setText(celcius);
			
			minTempFieldTV.setText(Integer.toString(tempBundle.getInt("MinTemperature")));
			minTempUnitTV.setText(celcius);
			
			allTimeMaxTempFieldTV.setText(Integer.toString(tempBundle.getInt("AllTimeMaxTemperature")));
			allTimeMaxTempUnitTV.setText(celcius);

			allTimeMinTempFieldTV.setText(Integer.toString(tempBundle.getInt("AllTimeMinTemperature")));
			allTimeMinTempUnitTV.setText(celcius);
		} else {
			int temp = ConversionUtil.celciusToFahrenheit(tempBundle.getInt("MaxTemperature"));
			maxTempFieldTV.setText(Integer.toString(temp));
			maxTempUnitTV.setText(fahrenheit);
			
			temp = ConversionUtil.celciusToFahrenheit(tempBundle.getInt("MinTemperature"));
			minTempFieldTV.setText(Integer.toString(temp));
			minTempUnitTV.setText(fahrenheit);
			
			temp = ConversionUtil.celciusToFahrenheit(tempBundle.getInt("AllTimeMaxTemperature"));
			allTimeMaxTempFieldTV.setText(Integer.toString(temp));
			allTimeMaxTempUnitTV.setText(fahrenheit);

			temp = ConversionUtil.celciusToFahrenheit(tempBundle.getInt("AllTimeMinTemperature"));
			allTimeMinTempFieldTV.setText(Integer.toString(temp));
			allTimeMinTempUnitTV.setText(fahrenheit);
		}
	}
}
