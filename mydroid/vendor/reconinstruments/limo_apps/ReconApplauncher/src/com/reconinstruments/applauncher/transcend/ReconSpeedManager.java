package com.reconinstruments.applauncher.transcend;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import com.reconinstruments.externalsensors.*;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.stats.StatsUtil;
import java.util.ArrayList;
public class ReconSpeedManager extends ReconStatsManager
    implements IExternalBikeSpeedListener{
    private static final String TAG = "ReconSpeedManager";
    public static final float INVALID_SPEED = StatsUtil.INVALID_SPEED;
    public static final float INVALID_PACE = StatsUtil.INVALID_PACE;
    public static final float MAX_ALLOWED_HORIZONTAL_SPEED = 300;
    public static final float HDOP_CUTOFF_FOR_VALID_SPEED_JET = 2;
    public static final String BROADCAST_ACTION_STRING = "RECON_MOD_BROADCAST_SPEED";
    // Show max speed notifications only if speed > 25kmh (or ~15mph)
    public static final float THRESHOLD_NOTIFICATION_MAX_SPEED = 25f;
    private Stat<Float> mHorzSpeed  =
	new Stat<Float>(INVALID_SPEED,"HorzSpeed");
    private Stat<Float> mVertSpeed  =
	new Stat<Float>(INVALID_SPEED,"VertSpeed");
    private Stat<Float> mSpeed =
	new Stat<Float>(INVALID_SPEED,"Speed");
    private Stat<Float> mPace =
	new Stat<Float>(INVALID_PACE,"Pace");	// Secondary value
    private static final int PACE_UPDATE_PERIOD = 5; // Every 5 seconds
    private final static float LOW_PASS_FILTER_FACTOR = 0.033f;
    private LowPassStat<Float> mLowPassFilterSpeed =
	new LowPassStat<Float>("LowPassFilterSpeed",mSpeed,
			       LOW_PASS_FILTER_FACTOR);
    private MaxStat<Float> mMaxSpeed=
	new MaxStat<Float>("MaxSpeed",mSpeed);
    private MaxStat<Float> mAllTimeMaxSpeed =
	new MaxStat<Float>("AllTimeMaxSpeed",mMaxSpeed);
    private AverageStat<Float> mAverageSpeed =
	new AverageStat<Float>("AverageSpeed",mSpeed);
    private MaxStat<Float> mBestAverageSpeed =
	new MaxStat<Float>("BestAverageSpeed",mAverageSpeed);
    private ReconTimeManager mTimeMan;
    private ReconAltitudeManager mAltMan;
    private ReconLocationManager mRecLocMan;
    private Stat<Float> mSensorSpeed =
	new Stat<Float>(INVALID_SPEED,"SensorSpeed");
    private float mTempAverageSpeed = 0;
    // Notification delay
    private Handler mHandler = new Handler();
    private SpeedNotificationRunnable mNotificationRunnable = null;
    private int NOTIFICATION_DELAY = 1000 * 5; // 5 seconds
    public static final long MINIMAL_DURATION_FOR_AVG_SPEED = 30000; // milli
    private ExternalBikeSpeedReceiver mExternalBikeSpeedReceiver;
    @Override protected void populateStatLists() {
	// category assignment
	addToLists(new Stat[] {mHorzSpeed,mVertSpeed,mSpeed,mSensorSpeed,
			       mPace,mLowPassFilterSpeed,mMaxSpeed,
			       mAllTimeMaxSpeed,mAverageSpeed,
			       mBestAverageSpeed},
	    new ArrayList[]{mToBeBundled});
	addToLists(new Stat[] {mMaxSpeed},
		   new ArrayList[] {mToBeSaved,mComparativeValues});
	addToLists(new Stat[] {mAverageSpeed},
		   new ArrayList[] {mToBeSaved,mCumulativeValues});
	addToLists(new Stat[] {mAllTimeMaxSpeed},
		   new ArrayList[] {mToBeAllTimeSaved,mPostActivityValues});
	addToLists(new Stat[] {mBestAverageSpeed},
		   new ArrayList[] {mToBeAllTimeSaved,mPostActivityValues});

    }
    public ReconSpeedManager(ReconTranscendService rts){
        mTimeMan = rts.mTimeMan;
        mAltMan = rts.getReconAltManager();
        mRecLocMan = rts.getReconLocationManager();
	// Override mHasNotifiedBestRecord;
	mHasNotifiedBestRecord = new boolean[2];
	// ^Now this is for best speed (0)  and best average speed (1)
	//start sensor receiver:
	prepare(rts);
	mExternalBikeSpeedReceiver = new ExternalBikeSpeedReceiver(mRTS, this);
	mExternalBikeSpeedReceiver.start();
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    // Getters
    public float getHorzSpeed(){return mHorzSpeed.getValue().floatValue();}
    public float getVertSpeed(){return mVertSpeed.getValue().floatValue();}
    public float getSpeed(){return mSpeed.getValue().floatValue();}
    public float getAverageSpeed(){return mAverageSpeed.getValue().floatValue();}
    public float getSensorSpeed() {return mSensorSpeed.getValue().floatValue();}
    public float getTempAverageSpeed() {
        return mTempAverageSpeed;
    }
    // Setters
    public void setTempAverageSpeed(float tempAverageSpeed) {
        this.mTempAverageSpeed = tempAverageSpeed;
    }
    public void setAverageSpeed(float avgspeed) {
	mAverageSpeed.setValue(avgspeed);
    }
    /**
     * Helper function to calculate horz speed
     */
    public void updateHorzSpeed(){
	if (!mRecLocMan.isGPSFix()) {
	    mHorzSpeed.setValue(INVALID_SPEED);
	    return;
        }
	if (mRecLocMan.getLocation() == null) {
	    mHorzSpeed.setValue(INVALID_SPEED);
	    return;
	}
	float hdop = mRecLocMan.getLocation().getAccuracy() /
	    ReconLocationManager.ACCURACY_IN_HDOP;
	if (mHorzSpeed.getValue().floatValue() >
	     MAX_ALLOWED_HORIZONTAL_SPEED) {
	    mHorzSpeed.setValue(INVALID_SPEED);
	    return;
	}
	float tmpSpeed = mRecLocMan.getLocation().getSpeed();
	//This is meter per second have to change that to km/h 
	tmpSpeed = (float) (tmpSpeed * ConversionUtil.KM_p_H_IN_M_p_S);
	if (tmpSpeed > MAX_ALLOWED_HORIZONTAL_SPEED){
	    mHorzSpeed.setValue(INVALID_SPEED);
	}
	else {
	    mHorzSpeed.setValue(tmpSpeed);
	}
    }
    /**
     * Helper function to calculate vert speed
     */
    public void updateVertSpeed(){
	float tmpVertSpeed = mAltMan.getDeltaAlt()/
	    ReconTranscendService.DATA_UPDATE_RATE_IN_SEC;
        tmpVertSpeed = (float) (tmpVertSpeed * ConversionUtil.KM_p_H_IN_M_p_S);
	mVertSpeed.setValue(tmpVertSpeed);
    }
    public void updateSpeed(){
        if (mSensorSpeed.isValid()) {
            mSpeed.setValue(mSensorSpeed.getValue());
        }
        else if (mHorzSpeed.isValid()){
	    double tmphorz = mHorzSpeed.getValue().doubleValue();
	    double tmpvert = mVertSpeed.getValue().doubleValue();
            mSpeed.setValue((float) Math.sqrt((tmphorz*tmphorz +
						       tmpvert*tmpvert)));
        }
        else {
            // At least one of the components has invalid speed
            // We set the whole speed 
            mSpeed.setValue(INVALID_SPEED);
        }
    }
    @Override
    public void updateCurrentValues() {
        updateHorzSpeed();
        updateVertSpeed();
        updateSpeed();
	mLowPassFilterSpeed.update();
	// Now doing pace:
	if (!mSpeed.isValid()) {
	    mPace.setValue(INVALID_PACE);
	}
	else if (((SystemClock.elapsedRealtime() / 1000)%
		  PACE_UPDATE_PERIOD) == 0){
	    mPace.setValue(1f / mLowPassFilterSpeed.getValue().floatValue()
			   * 3600f);
	}
    }
    private void checkForNotificationOfBestAverageSpeed() {
	// The final best average is calculated during the activty
	// This one is a hack to give a user heads up on what's coming.
	if (mHasNotifiedBestRecord[1]) return;
	
	if (mRTS.getSportsActivityManager().getDuration() <
	    MINIMAL_DURATION_FOR_AVG_SPEED) return;

	if (!mBestAverageSpeed.isRecord()) return;
	// Note that we don't update best average speed
	if (mRTS.getSportsActivityManager().getType() !=
	    ActivityUtil.SPORTS_TYPE_RUNNING) {
	    notifyBestRecordIfNecessary(1,0, "Best Average Speed");
	}
	else {
	    notifyBestRecordIfNecessary(1,0, "Best Average Pace");
	}

    }
    private boolean isTodayMax; 
    private boolean isAllTimeMax; 
    @Override
    public void updateComparativeValues() {
	super.updateComparativeValues();
        isTodayMax = mMaxSpeed.wasRecord();
        isAllTimeMax = mAllTimeMaxSpeed.isRecord();
	if (isTodayMax || isAllTimeMax) {
	    mHandler.removeCallbacks(mNotificationRunnable);
            mNotificationRunnable =
                new SpeedNotificationRunnable(mRTS.getApplicationContext(),
                                              isTodayMax, isAllTimeMax,
                                              mMaxSpeed.getValue().floatValue(),
					      mAllTimeMaxSpeed.getValue().floatValue());
            mHandler.postDelayed(mNotificationRunnable, NOTIFICATION_DELAY);
	    
	}
	checkForNotificationOfBestAverageSpeed();
    }
    @Override
    public void onSensorConnected() {
    }
    @Override
    public void onSensorDisconnected() {
	mSensorSpeed.setValue(INVALID_SPEED);
    }
    @Override
    public void onBikeSpeedChanged(int speed) {
	if (mRTS.mSportsActivityMan.getType() ==
	    ActivityUtil.SPORTS_TYPE_CYCLING) {
	    mSensorSpeed.setValue((float)speed);
	}
	else {
	    mSensorSpeed.setValue(INVALID_SPEED);
	}
    }

    /**
     * The notification runnable.
     */
    class SpeedNotificationRunnable implements Runnable {
        boolean today = false;
        boolean alltime = false;
        float maxSpeed, maxSpeedAllTime;
        Context mContext;
        public SpeedNotificationRunnable(Context context, boolean isMaxToday,
                                         boolean isMaxAllTime, float maxSpeed,
                                         float maxSpeedAllTime) {
            this.maxSpeed = maxSpeed;
            this.maxSpeedAllTime = maxSpeedAllTime;
            this.mContext = context;
            this.today = isMaxToday;
            this.alltime = isMaxAllTime;
        }
        @Override
        public void run() {
            if (today) {
                broadcastBundle(BROADCAST_ACTION_STRING, "MaxSpeed");
            }
            if (alltime) {
                broadcastBundle(BROADCAST_ACTION_STRING, "AllTimeMaxSpeed");
		if (mAllTimeMaxSpeed.getValue().floatValue() <
		    THRESHOLD_NOTIFICATION_MAX_SPEED) {
		    return;
		}
		// Post notification if not running
		if (mRTS.getSportsActivityManager().getType() !=
		    ActivityUtil.SPORTS_TYPE_RUNNING) {
		    notifyStatRecord(0, "Record Speed");
		}
            }
        }
    }
}
