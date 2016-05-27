package com.reconinstruments.dashlauncher.livestats.widgets;

import java.text.DecimalFormat;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.ConversionUtil;
import com.reconinstruments.dashlauncher.ReconSettingsUtil;
import com.reconinstruments.polarhr.service.PolarHRStatus;

public class ReconHRWidget4x1 extends ReconDashboardWidget {

	private TextView fieldTextView;

	public ReconHRWidget4x1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_hr_4x1, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconHRWidget4x1(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_hr_4x1, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconHRWidget4x1(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_hr_4x1, null);
		this.addView(myView);

		prepareInsideViews();
	}

	@Override
	public void updateInfo(Bundle fullinfo) {
	    	    if (fullinfo == null ) {
		return;
	    }
		Bundle hrBundle = (Bundle) fullinfo.get("POLAR_BUNDLE");
		
		if(hrBundle != null) {
			int connectionState = hrBundle.getInt("ConnectionState");
			if(connectionState == PolarHRStatus.STATE_CONNECTED	&& hrBundle.getInt("AvgHR") > 0)
				fieldTextView.setText(Integer.toString(hrBundle.getInt("AvgHR")));
			else if(connectionState == PolarHRStatus.STATE_CONNECTING) {
				fieldTextView.setText("...");
			}
			
			else if(connectionState == PolarHRStatus.STATE_NONE) {
				fieldTextView.setText("--");
			}
		} else {
			fieldTextView.setText("--");
		}
	}

	@Override
	public void prepareInsideViews() {
		fieldTextView = (TextView) findViewById(R.id.hr_field);
//		fieldTextView.setTypeface(livestats_widget_typeface);
	}

}
