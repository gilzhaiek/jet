//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.stats.StatsUtil;

public class ReconPaceWidget extends ReconDashboardWidget {

    private static final String TAG = ReconPaceWidget.class.getSimpleName();
    private String displayValue;
    private String unitString;
    private Boolean isValidPace;
    private float value;
    public static final float PACE_TOO_SLOW = 1800; // 1800 sec/km 2km/h
	
    public ReconPaceWidget(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
    }


    public ReconPaceWidget(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public ReconPaceWidget(Context context) {
	super(context);
    }

	
    @Override
    public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
	boolean isMetric = (ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC);
	value = fullinfo.getBundle("SPEED_BUNDLE").getFloat("Pace");
	value = invalidSpeedValueDelay(value);  
    isValidPace = (Float.compare(value, StatsUtil.INVALID_PACE) > 0) && value < PACE_TOO_SLOW;
    unitString = isMetric ? "/KM":"/MI" ;
    if(!inActivity){ // show '---'
        displayValue = "---";
        fieldTextView.setText(Html.fromHtml(((inActivity) ? displayValue : "<font color=\"#808181\">" + displayValue + "</font>")));
        unitTextView.setText(unitString);
    }else{
        displayValue = isValidPace ? 
                (isMetric ?                   // valid
                 ConversionUtil.secondsToMinutes((long)value, 5):// metric
                 ConversionUtil.secondsToMinutes((long)(value/ConversionUtil
                                    .KM_MILE_RATIO), 5)): // imperial
                "---";      // invalid
            fieldTextView.setText(Html.fromHtml(((inActivity) ? displayValue : "<font color=\"#808181\">" + displayValue + "</font>")));
            unitTextView.setText(unitString);
    }
	fieldTextView.invalidate();
    }

    @Override
    public void prepareInsideViews() {
        super.prepareInsideViews();
    }
}
