package com.reconinstruments.utils.stats;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.R;
import com.reconinstruments.utils.stats.StatsUtil.FormattedStat;
import com.reconinstruments.utils.stats.StatsUtil.MetricType;
import com.reconinstruments.utils.stats.StatsUtil.StatDescription;
import com.reconinstruments.utils.stats.StatsUtil.StatType;

/*
 * Utilities for formatting and displaying stats
 */
public class StatFormatUtils {

    private static final String TAG = "StatFormatUtils";

    public static String getValueString(MetricType type,Object value,boolean isMetric) {
        switch(type){
        case MOVINGDURATION:
            String valueStr;
            long longVal = (Long) value;
            if(longVal < 60*60*1000){
                valueStr = minFormat.format(value);
            }else{
                valueStr = hourFormat.format(value);
            }
            if(valueStr.startsWith("0")) // to remove the prefix 0
                valueStr = valueStr.substring(1, valueStr.length());
            return valueStr;
        case SPEED:
            value = isMetric ? value : ConversionUtil.kmsToMiles((Float)value);
            return decFormat.format(value);
        case PACE:
            return ConversionUtil.speedToPace((Float)value, isMetric);
        case DISTANCE:
            value = ((Float)value)/1000.00f;
            value = isMetric ? value : ConversionUtil.kmsToMiles((Float)value);
            return decFormat.format(value);
        case TERRAINGRADE:
            return decFormat.format((Float)value * 100.0f); 
        case ELEVATIONGAIN:
            value = isMetric ? value : ConversionUtil.metersToFeet((Float)value);
            return decFormat.format(value);
        case JUMP:
            value = ((Integer)value) / 1000f;
            return decFormat.format(value);
        case DATE:
            return dateFormat.format(value);
        case CALORIE:
        case HEARTRATE:
        case SKIRUN:
        case CADENCE:
        case POWER:
        case ACTIVITIES:
        default:
            return decFormat.format(value);
        }
    }

    public static String getUnitString(Context context, MetricType type,boolean isMetric,boolean lowerCase) {
        if(lowerCase)
            return getUnitString(context,type,isMetric).toLowerCase();
        else
            return getUnitString(context,type,isMetric);
    }
    public static String getUnitString(Context context, MetricType type,boolean isMetric) {
        switch(type){
        case POWER:
            return "W";
        case HEARTRATE:
            return "BPM";
        case CADENCE:
            return "RPM";
        case SPEED:
            return isMetric ? context.getString(R.string.kmh): context.getString(R.string.mph);
        case PACE:
            return "/" + (isMetric ? context.getString(R.string.minkm): context.getString(R.string.minmi));
        case DISTANCE:
            return isMetric ? context.getString(R.string.minkm): context.getString(R.string.minmi);
        case TERRAINGRADE:
            return "%";
        case ELEVATIONGAIN:
            return isMetric ? context.getString(R.string.meters_abr): context.getString(R.string.feet_abr);
        case JUMP:
            return "S";
        case MOVINGDURATION:
        case SKIRUN:
        case ACTIVITIES:
        case DATE:
        default:
            return "";
        }
    }

    static DecimalFormat decFormat = new DecimalFormat();
    static DecimalFormat distanceFormat = new DecimalFormat("0.00");
    static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
    static SimpleDateFormat minFormat = new SimpleDateFormat("mm:ss");
    static SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");
    static {
        decFormat.setMaximumFractionDigits(0);
        decFormat.setGroupingUsed(false);
        minFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        hourFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static boolean validateFloat(float value) {
        return  value != Float.NaN &&
                value != Float.POSITIVE_INFINITY &&
                value != Float.NEGATIVE_INFINITY;
    }
    public static boolean validateFloat(float value, float invalidValue) {
        return Float.compare(value, invalidValue) != 0 && validateFloat(value);
    }
    public static boolean validateInt(int value, int invalidValue) {
        return (value > 0) && (value != invalidValue);
    }

    public static boolean validateValue(Object value,MetricType metricType, StatType type) {

        if(value == null)
            return false;
        
        switch(metricType) {
            case CALORIE:
                return validateFloat((Float) value, StatsUtil.INVALID_CALORIES);
            case DISTANCE:
                return validateFloat((Float) value, StatsUtil.INVALID_DISTANCE);
            case SPEED:
            case PACE:
                return validateFloat((Float) value, StatsUtil.INVALID_SPEED);
            case TERRAINGRADE:
                return validateFloat((Float) value, StatsUtil.INVALID_GRADE);
            case JUMP:
            case ELEVATIONGAIN:
                return validateFloat((Float) value, 0.0f);
            case DATE:
            case MOVINGDURATION:
                return (Long) value > 0l;
            case ACTIVITIES:
                return (Integer) value > 0;
            case CADENCE:
                return validateInt((Integer) value,StatsUtil.INVALID_CADENCE);
            case HEARTRATE:
                return validateInt((Integer) value,StatsUtil.INVALID_HR);
            case SKIRUN:
                return (Integer) value != 0;
            case POWER:
		return validateInt((Integer) value,StatsUtil.INVALID_POWER);
            default:
                break;
        }

        return true;
    }

    public static FormattedStat formatData(Context context, Bundle data, StatDescription statDesc,boolean isMetric,boolean lowerCaseUnit){
        Bundle bundle = (Bundle) data.get(statDesc.bundleName);
        if (bundle != null) {
            Object value = getValueFromBundle(bundle,statDesc.bundleParam,statDesc.metricType,statDesc.type);
            if(validateValue(value,statDesc.metricType,statDesc.type)) {
                return new FormattedStat(
                        getValueString(statDesc.metricType,value,isMetric),
                        getUnitString(context,statDesc.metricType,isMetric,lowerCaseUnit));
            } else {
                Log.d(TAG, statDesc.type.name()+" value "+value+" invalid");
                return null;
            }
        } else {
            return null;
        }
    }

    private static Object getValueFromBundle(Bundle bundle,String paramName,MetricType type,StatType statType) {
        if (type==MetricType.JUMP){
            Bundle jumpBundle = bundle.getBundle(paramName);
            if (jumpBundle == null)
                return null;
            return jumpBundle.getInt("Air");
        }
        if(statType==StatType.LAST_ACTIVITY_RUNS) {
            return bundle.getParcelableArrayList(paramName).size();
        }
        return bundle.get(paramName);
    }
}
