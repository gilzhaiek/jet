package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.polarhr.service.PolarHRStatus;

public class ReconHRWidget2x2 extends ReconDashboardWidget {

	public ReconHRWidget2x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_hr_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconHRWidget2x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_hr_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	public ReconHRWidget2x2(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
		View myView = infl.inflate(R.layout.livestats_widget_hr_2x2, null);
		this.addView(myView);

		prepareInsideViews();
	}

	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
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
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}

}
