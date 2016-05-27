package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.CircularFloatArray;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

/**
 * The grade (also called slope) represents as a percentage calculated from rise/run,
 * where 0% indicates 0 degree angle and 100% = 45 degree angle ... 
 * Larger the number higher or steeper degree of tilt.
 * A positive number indicates going the uphill and negative number indicates going down the hill.
 *   
 */
public class GradeBO extends BaseBO implements MetricChangedListener {

    private static final boolean DEBUG = BASE_DEBUG | false;

    /**
     * Minimum horizontal distance(meter) required for a valid grade calculation 
     */
    private static final float MIN_HORZ_DISTANCE_FOR_GRADE = 4;

    /**
     * Based on the altitude pressure and gps location updates every second. 
     * The circular array size 7 equals 7 seconds of information
     */
    private static final int CIRCULAR_ARRAY_SIZE = 7;  

    private static final CircularFloatArray mAltitudeCFA = new CircularFloatArray(CIRCULAR_ARRAY_SIZE);
    private static final CircularFloatArray mDistanceCFA = new CircularFloatArray(CIRCULAR_ARRAY_SIZE);

    private static long mLastDistanceTimeStamp = 0;
    private static float mDeltaAltitude;
    private static float mDeltaDistance;

    public GradeBO() {
        super(HUDMetricIDs.GRADE);
    }

    @Override
    protected void reset() {
        super.reset();
        mAltitudeCFA.reset();
        mDistanceCFA.reset();
        mLastDistanceTimeStamp = 0;
    }

    @Override
    protected void enableMetrics() {
        MetricsBOs.get(HUDMetricIDs.ALTITUDE_PRESSURE).registerListener(this);
        MetricsBOs.get(HUDMetricIDs.DISTANCE_HORIZONTAL).registerListener(this);
    }

    @Override
    protected void disableMetrics() {
        MetricsBOs.get(HUDMetricIDs.ALTITUDE_PRESSURE).unregisterListener(this);
        MetricsBOs.get(HUDMetricIDs.DISTANCE_HORIZONTAL).unregisterListener(this);
    }

   /**
     * There are 2 Circular arrays: one is for the Altitude, and another for the Distance.
     * When a value has been changed, the corraspanding circular array will get pushed with the new value
     * The Horizontal distance just updates the circular array.
     * The Altitude based pressure will invoke the update algorithem for the Grade.
     *
     * The Grade Algorithem
     * 1st Step: Calculate the Delta Altitude from this point to CIRCULAR_ARRAY_SIZE seconds before it
     * 2nd step: Calculate the Delta Distance from this point to CIRCULAR_ARRAY_SIZE seconds before it.
     *           The distance has to be greater than MIN_HORZ_DISTANCE_FOR_GRADE to make sure we are moving
     * Final Step:  Grade =  Convert To Precentage Number( Delta of Altitude / Delta of Distance )
     */
    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        if(metricID == HUDMetricIDs.ALTITUDE_PRESSURE){
            mAltitudeCFA.push(value);
            if(!calculateDeltaAltitude()){
                getMetric().addInvalidValue();
                return;
            }
            if(!calculateDeltaDistance()){
                getMetric().addInvalidValue();
                return;
            }

            float grade = 100 * (mDeltaAltitude / mDeltaDistance);
            if (Float.isInfinite(grade)){
                getMetric().addValue(0.0f);
            } else {
                getMetric().addValue(grade, changeTime);
            }
            if(DEBUG) Log.d(TAG,"Grade: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
        else if(metricID == HUDMetricIDs.DISTANCE_HORIZONTAL){
            mLastDistanceTimeStamp = changeTime;
            mDistanceCFA.push(value);
        }
    }

    /**
     * Calculate the delta altitude 
     * @return true if there is valid delta altitude, otherwise false
     */
    private boolean calculateDeltaAltitude(){
        float firstFromQueue = mAltitudeCFA.readMostRecentValue();
        float lastFromQueue = mAltitudeCFA.readOldestValue();

        //Check for valid values
        if(!isValidFloat(firstFromQueue, lastFromQueue)){
            return false;
        }
        //Calculate the delta
        mDeltaAltitude =  firstFromQueue - lastFromQueue;
        return true;
    }

    /**
     * Calculate the delta distance
     * @return true if there is valid delta distance, otherwise false
     */
    private boolean calculateDeltaDistance(){
        //Check for valid values
        float firstFromQueue = mDistanceCFA.readMostRecentValue();
        float lastFromQueue = mDistanceCFA.readOldestValue();

        if(!isValidFloat(lastFromQueue, firstFromQueue)){
            return false;
        }
        //Check for data OutOfSync
        if(MetricUtils.isOutOfSync(mLastDistanceTimeStamp)){
            return false;
        }
        //Calculate and check for valid distance
        mDeltaDistance =  firstFromQueue - lastFromQueue;
        if(mDeltaDistance < MIN_HORZ_DISTANCE_FOR_GRADE){
            return false;
        }
        return true;
    }

    private boolean isValidFloat(float value1, float value2){
        return (MetricUtils.isValidFloat(value1) && MetricUtils.isValidFloat(value2));
    }
}
