//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlauncher.livestats.widgets;
// NOTE: Requires major refactoring
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.stats.TranscendUtils;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.StatsUtil;

public abstract class ReconDashboardWidget extends FrameLayout {

    private static final String TAG = ReconDashboardWidget.class.getSimpleName();
    protected boolean isJet = DeviceUtils.isSun();
    protected TextView titleTextView;
    protected TextView fieldTextView;
    protected TextView unitTextView;
    protected static String mInvalidString = "---";
    protected static String mInvalidGreyString = "<font color=\"#808181\">---</font>";
    
    private static int mInActivityColor = -1;
    private static int mNotInActivityColor = -1;
    private static int mGregTextColor = -1;
    protected boolean mHasGps;
    
    protected Bundle mFullBundle;
    protected boolean mInActivity;

    public ReconDashboardWidget(Context context, AttributeSet attrs,
				int defStyle) {
	super(context, attrs, defStyle);
	init(context);
    }

    public ReconDashboardWidget(Context context, AttributeSet attrs) {
	super(context, attrs);
	init(context);
    }

    public ReconDashboardWidget(Context context) {
	super(context);
	init(context);
    }

    public void updateInfo(Bundle fullInfo, boolean inActivity){
        if (fullInfo == null ) {
            return;
        }
        mFullBundle = fullInfo;
        mInActivity = inActivity;
        loadDynamicAssets();
    }

    public void prepareInsideViews(){
	//common fields include value and unit
	titleTextView = (TextView) findViewById(R.id.title);
        fieldTextView = (TextView) findViewById(R.id.value);
        unitTextView = (TextView) findViewById(R.id.unit);
        loadDynamicAssets();
    }
	
    private void init(Context context){
        mFullBundle = TranscendUtils.getFullInfoBundle(context);
        mInActivity = !(ActivityUtil.getActivityState(mFullBundle) ==
			ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY);
    }
	
    private void loadDynamicAssets(){
	if(!isJet) return; // use the static assets defined in xml layout file for snow2
        if(mInActivityColor == -1){
            mInActivityColor = getResources().getColor(R.color.jet_in_activity);
        }
        if(mNotInActivityColor == -1){
            mNotInActivityColor = getResources().getColor(R.color.jet_not_in_activity);
        }
        if(mGregTextColor == -1){
            mGregTextColor = getResources().getColor(R.color.jet_greg_text);
        }
        LinearLayout seperator = (LinearLayout) findViewById(R.id.seperator);
        if(seperator != null){
            if(mInActivity){
                seperator.setBackground(new ColorDrawable(mInActivityColor));
            }else{
                seperator.setBackground(new ColorDrawable(mNotInActivityColor));
            }
        }
        if(mInActivity){
            if(unitTextView != null)
                unitTextView.setTextColor(mInActivityColor);
        } else {
            if(unitTextView != null)
                unitTextView.setTextColor(mGregTextColor);
        }
    }

    public final static ReconDashboardWidget spitReconDashboardWidget(int id,
								      Context c) {
	// id argument is usually retrieve by running a widget name through
	// WidgegtHashmap("widgetName");
	ReconPowerWidget pwrWidget; //power widget
	    
	switch (id) {
	case ReconDashboardHashmap.ALT_2x2:
	    return new ReconAltWidget2x2(c);
			
	case ReconDashboardHashmap.ALT_4x1:
	    return new ReconAltWidget4x1(c);
            
        case ReconDashboardHashmap.ALT_6x4:
            return new ReconAltWidget6x4(c);
            
        case ReconDashboardHashmap.ALT_3x4:
            return new ReconAltWidget3x4(c);
            
        case ReconDashboardHashmap.ALT_3x2:
            return new ReconAltWidget3x2(c);

	case ReconDashboardHashmap.VRT_4x1:
	    return new ReconVrtWidget4x1(c);

	case ReconDashboardHashmap.VRT_6x4:
	    return new ReconVrtWidget6x4(c);

        case ReconDashboardHashmap.VRT_3x4:
            return new ReconVrtWidget3x4(c);

        case ReconDashboardHashmap.VRT_3x2:
            return new ReconVrtWidget3x2(c);

        case ReconDashboardHashmap.VRT_2x2:
            return new ReconVrtWidget2x2(c);

	case ReconDashboardHashmap.SPD_4x2:
	    return new ReconSpeedWidget4x2(c);

	case ReconDashboardHashmap.SPD_4x3:
	    return new ReconSpeedWidget4x3(c);

	case ReconDashboardHashmap.SPD_4x4:
	    return new ReconSpeedWidget4x4(c);

	case ReconDashboardHashmap.SPD_6x4:
	    return new ReconSpeedWidget6x4(c);

        case ReconDashboardHashmap.SPD_3x4:
            return new ReconSpeedWidget3x4(c);

        case ReconDashboardHashmap.SPD_3x2:
            return new ReconSpeedWidget3x2(c);

	case ReconDashboardHashmap.PLY_4x1:
	    return new ReconPlaylistWidget4x1(c);
			
	case ReconDashboardHashmap.DST_4x1:
	    return new ReconDistanceWidget4x1(c);

	case ReconDashboardHashmap.DST_2x2:
	    return new ReconDistanceWidget2x2(c);

        case ReconDashboardHashmap.DST_3x2:
            return new ReconDistanceWidget3x2(c);

        case ReconDashboardHashmap.DST_3x4:
            return new ReconDistanceWidget3x4(c);

        case ReconDashboardHashmap.DST_6x4:
            return new ReconDistanceWidget6x4(c);
			
	case ReconDashboardHashmap.AIR_4x1:
	    return new ReconAirWidget4x1(c);

	case ReconDashboardHashmap.AIR_2x2:
	    return new ReconAirWidget2x2(c);

        case ReconDashboardHashmap.AIR_6x4:
            return new ReconAirWidget6x4(c);

        case ReconDashboardHashmap.AIR_3x4:
            return new ReconAirWidget3x4(c);

        case ReconDashboardHashmap.AIR_3x2:
            return new ReconAirWidget3x2(c);
			
	case ReconDashboardHashmap.MAXSPD_4x1:
	    return new ReconMaxSpeedWidget4x1(c);

	case ReconDashboardHashmap.MAXSPD_2x2:
	    return new ReconMaxSpeedWidget2x2(c);
			
	case ReconDashboardHashmap.TMP_4x1:
	    return new ReconTemperatureWidget4x1(c);
			
	case ReconDashboardHashmap.TMP_2x2:
	    return new ReconTemperatureWidget2x2(c);
			
	case ReconDashboardHashmap.CHR_4x1:
	    return new ReconChronoWidget4x1(c);
		
	case ReconDashboardHashmap.HR_2x2:
	    return new ReconHRWidget2x2(c);
			
	case ReconDashboardHashmap.HR_4x1:
	    return new ReconHRWidget4x1(c);

	case ReconDashboardHashmap.CAD_6x4: // Cadence
            return new ReconCadenceWidget6x4(c);
	case ReconDashboardHashmap.CAD_3x4:
            return new ReconCadenceWidget3x4(c);
	case ReconDashboardHashmap.CAD_3x2:
            return new ReconCadenceWidget3x2(c);
		    
	case ReconDashboardHashmap.CAL_6x4: // Calorie
            return new ReconCalorieWidget6x4(c);
	case ReconDashboardHashmap.CAL_3x4:
            return new ReconCalorieWidget3x4(c);
	case ReconDashboardHashmap.CAL_3x2:
            return new ReconCalorieWidget3x2(c);
		    
	case ReconDashboardHashmap.EV_6x4: // Elevation Gain
            return new ReconElevationGainWidget6x4(c);
	case ReconDashboardHashmap.EV_3x4:
            return new ReconElevationGainWidget3x4(c);
	case ReconDashboardHashmap.EV_3x2:
            return new ReconElevationGainWidget3x2(c);
		    
	case ReconDashboardHashmap.HRT_6x4: // HeartRate
            return new ReconHeartRateWidget6x4(c);
	case ReconDashboardHashmap.HRT_3x4:
            return new ReconHeartRateWidget3x4(c);
	case ReconDashboardHashmap.HRT_3x2:
            return new ReconHeartRateWidget3x2(c);
		    
	case ReconDashboardHashmap.DUR_6x4: // Moving Duration
            return new ReconMovingDurationWidget6x4(c);
	case ReconDashboardHashmap.DUR_3x4:
            return new ReconMovingDurationWidget3x4(c);
	case ReconDashboardHashmap.DUR_3x2:
            return new ReconMovingDurationWidget3x2(c);
		    
	case ReconDashboardHashmap.PACE_6x4: // Pace
            return new ReconPaceWidget6x4(c);
	case ReconDashboardHashmap.PACE_3x4:
            return new ReconPaceWidget3x4(c);
	case ReconDashboardHashmap.PACE_3x2:
            return new ReconPaceWidget3x2(c);
		    
	case ReconDashboardHashmap.TRG_6x4: // Terrain Grade
            return new ReconTerrainGradeWidget6x4(c);
	case ReconDashboardHashmap.TRG_3x4:
            return new ReconTerrainGradeWidget3x4(c);
	case ReconDashboardHashmap.TRG_3x2:
            return new ReconTerrainGradeWidget3x2(c);
		    
	case ReconDashboardHashmap.PWR_6x4: // Power
	    return  new ReconPowerWidget6x4(c,ReconPowerWidget.PowerType.POWER);
	case ReconDashboardHashmap.PWR_3x4:
	    return  new ReconPowerWidget3x4(c,ReconPowerWidget.PowerType.POWER);
	case ReconDashboardHashmap.PWR_3x2:
	    return  new ReconPowerWidget3x2(c,ReconPowerWidget.PowerType.POWER);
        case ReconDashboardHashmap.PWR_AVG_6x4: // Power Avg
            return  new ReconPowerWidget6x4(c,ReconPowerWidget.PowerType.AVG_POWER);
        case ReconDashboardHashmap.PWR_AVG_3x4:
            return  new ReconPowerWidget3x4(c,ReconPowerWidget.PowerType.AVG_POWER);
        case ReconDashboardHashmap.PWR_AVG_3x2:
            return  new ReconPowerWidget3x2(c,ReconPowerWidget.PowerType.AVG_POWER);
        case ReconDashboardHashmap.PWR_MAX_6x4: // Power MAX
            return new ReconPowerWidget6x4(c,ReconPowerWidget.PowerType.MAX_POWER);
        case ReconDashboardHashmap.PWR_MAX_3x4:
            return  new ReconPowerWidget3x4(c,ReconPowerWidget.PowerType.MAX_POWER);
        case ReconDashboardHashmap.PWR_MAX_3x2:
            return new ReconPowerWidget3x2(c,ReconPowerWidget.PowerType.MAX_POWER);
        case ReconDashboardHashmap.PWR_3S_6x4: // Power 3S
            return new ReconPowerWidget6x4(c,ReconPowerWidget.PowerType.AVG_3S_POWER);
        case ReconDashboardHashmap.PWR_3S_3x4:
            return new ReconPowerWidget3x4(c,ReconPowerWidget.PowerType.AVG_3S_POWER);
        case ReconDashboardHashmap.PWR_3S_3x2:
            return new ReconPowerWidget3x2(c,ReconPowerWidget.PowerType.AVG_3S_POWER);
        case ReconDashboardHashmap.PWR_10S_6x4: // Power 10S
            return new ReconPowerWidget6x4(c,ReconPowerWidget.PowerType.AVG_10S_POWER);
        case ReconDashboardHashmap.PWR_10S_3x4:
            return new ReconPowerWidget3x4(c,ReconPowerWidget.PowerType.AVG_10S_POWER);
        case ReconDashboardHashmap.PWR_10S_3x2:
            return new ReconPowerWidget3x2(c,ReconPowerWidget.PowerType.AVG_10S_POWER);
        case ReconDashboardHashmap.PWR_30S_6x4: // Power 30S
	    return new ReconPowerWidget6x4(c,ReconPowerWidget.PowerType.AVG_30S_POWER);
        case ReconDashboardHashmap.PWR_30S_3x4:
            return new ReconPowerWidget3x4(c,ReconPowerWidget.PowerType.AVG_30S_POWER);
        case ReconDashboardHashmap.PWR_30S_3x2:
            return new ReconPowerWidget3x2(c,ReconPowerWidget.PowerType.AVG_30S_POWER);
        case ReconDashboardHashmap.PACE_AVG_6x4: // Avg Pace
            return new ReconPaceAvgWidget6x4(c);
        case ReconDashboardHashmap.PACE_AVG_3x4:
            return new ReconPaceAvgWidget3x4(c);
        case ReconDashboardHashmap.PACE_AVG_3x2:
            return new ReconPaceAvgWidget3x2(c);
            
        case ReconDashboardHashmap.CAD_AVG_6x4: // Avg Cadence
            return new ReconCadenceAvgWidget6x4(c);
        case ReconDashboardHashmap.CAD_AVG_3x4:
            return new ReconCadenceAvgWidget3x4(c);
        case ReconDashboardHashmap.CAD_AVG_3x2:
            return new ReconCadenceAvgWidget3x2(c);
            
        case ReconDashboardHashmap.HRT_AVG_6x4: // Avg HeartRate
            return new ReconHeartRateAvgWidget6x4(c);
        case ReconDashboardHashmap.HRT_AVG_3x4:
            return new ReconHeartRateAvgWidget3x4(c);
        case ReconDashboardHashmap.HRT_AVG_3x2:
            return new ReconHeartRateAvgWidget3x2(c);
            
        case ReconDashboardHashmap.SPD_AVG_6x4: // Avg Speed
            return new ReconSpeedAvgWidget6x4(c);
        case ReconDashboardHashmap.SPD_AVG_3x4:
            return new ReconSpeedAvgWidget3x4(c);
        case ReconDashboardHashmap.SPD_AVG_3x2:
            return new ReconSpeedAvgWidget3x2(c);
	}
	return null;
    }
	
    protected CharSequence prefixZeroes(float input, boolean inActivity){
	CharSequence output;
	boolean negative = false;
	if(input < -0.5f) {
	    negative = true;
	}
        input = Math.abs(Math.round(input));
	if(input < 10) {
	    output = Html.fromHtml(((negative) ? "-<font color=\"#808181\">00</font>" : "<font color=\"#808181\">00</font>") 
				   + ((inActivity) ? Integer.toString((int)input) : "<font color=\"#808181\">" + Integer.toString((int)input) + "</font>"));
	}
	else if(input < 100) {
	    output = Html.fromHtml(((negative) ? "-<font color=\"#808181\">0</font>" : "<font color=\"#808181\">0</font>") 
				   + ((inActivity) ? Integer.toString((int)input) : "<font color=\"#808181\">" + Integer.toString((int)input) + "</font>"));
	}
	else {
	    output = Html.fromHtml(((negative) ? "-" : "") 
				   + ((inActivity) ? Integer.toString((int)input) : "<font color=\"#808181\">" + Integer.toString((int)input) + "</font>"));
	}
	return output;
    }
	
    // set 5 seconds delay to show invalid metric data for speed, race
    private float mPreviousValue = -1.0f;
    private boolean delay = false;
    private static Handler handler = new Handler();
    protected float invalidSpeedValueDelay(float current){
        if(current > -1.0f){
            if(delay){
                Log.d(TAG, "removed 5 seconds waiting timer");
                handler.removeCallbacksAndMessages(null);
                delay = false;
            }
            mPreviousValue = current;
        }else{ // keep 5 seconds to use valid value
            if(mPreviousValue > -1.0f){ //previous speed is valid
                if(!delay){ //start the timer
                    Log.d(TAG, "5 seconds waiting timer started");
                    handler.postDelayed(new Runnable() {
			    public void run() {
				Log.d(TAG, "5 seconds waiting timer stopped");
				mPreviousValue = -1.0f;
				delay = false;
			    }
			}, 5* 1000);
                }
                current = mPreviousValue;
                delay = true;
            }
        }
        return current;
    }

}
