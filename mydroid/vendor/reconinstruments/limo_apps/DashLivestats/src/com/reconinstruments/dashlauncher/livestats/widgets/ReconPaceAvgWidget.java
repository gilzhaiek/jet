//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.utils.ConversionUtil;

public class ReconPaceAvgWidget extends ReconDashboardWidget {

    private static final String TAG = ReconPaceAvgWidget.class.getSimpleName();
    private String displayValue;
    private String unitString;
    private Boolean isValidPace;
    private float value;
    public static final float PACE_TOO_SLOW = 1800; // 1800 sec/km 2km/h
	
    public ReconPaceAvgWidget(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
    }


    public ReconPaceAvgWidget(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public ReconPaceAvgWidget(Context context) {
	super(context);
    }

	
    @Override
    public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
	boolean isMetric = (ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC);
	value = fullinfo.getBundle("SPEED_BUNDLE").getFloat("AverageSpeed"); 
	String convertedValue = ConversionUtil.speedToPace(value, isMetric);// convert to avg pace
	unitString = isMetric ? "/KM":"/MI" ;
	if(!inActivity){ // show '---'
	    fieldTextView.setText(Html.fromHtml(mInvalidGreyString));
	}else{
	    fieldTextView.setText(Html.fromHtml(((inActivity) ? convertedValue : "<font color=\"#808181\">" + convertedValue + "</font>")));
	}
	unitTextView.setText(unitString);
	fieldTextView.invalidate();
    }

    @Override
    public void prepareInsideViews() {
        super.prepareInsideViews();
    }

}
