package com.reconinstruments.applauncher.transcend;
import java.text.DecimalFormat;
import com.reconinstruments.applauncher.R;
import com.reconinstruments.notification.ReconNotification;
import android.content.Context;
import android.os.Build;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.reconinstruments.utils.stats.StatsUtil;

/**
 *  <code>ReconGradeManager</code> class is used to calculate the
 * grade it uses a few altitude and a few distance points to come up
 * with the best estimate of the grade
 */
public class ReconGradeManager extends ReconStatsManager {
    private static final String TAG = "ReconGradeManager";
    public static final float MIN_HORZ_DISTANCE_FOR_GRADE = 4;
    public static final float INVALID_GRADE = StatsUtil.INVALID_GRADE;
    private static final int CIR_ARRAY_SIZE = 7;
    private float mTerrainGrade = 0;
    private CircularFloatArray mAlts =
	new CircularFloatArray(CIR_ARRAY_SIZE, ReconAltitudeManager.INVALID_ALT);
    private CircularFloatArray mDists =
	new CircularFloatArray(CIR_ARRAY_SIZE, -1);
    public ReconGradeManager(ReconTranscendService rts) {
	prepare(rts);
	generateBundle();
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override
    protected Bundle generateBundle() {
	super.generateBundle();
	mBundle.putFloat("TerrainGrade",mTerrainGrade);
	return mBundle;
    }
    @Override
    public void updateCurrentValues() {
	mAlts.push(mRTS.mAltMan.getPressureAlt());
	mDists.push(mRTS.mDistMan.getHorzDistance());
	// Calculate the grade
	float y0 = mAlts.readPrevious(-1); // oldest element
	float y1 = mAlts.readPrevious(0); // latest alt
	float x0 = mDists.readPrevious(-1);
	float x1 = mDists.readPrevious(0);
	if (y1 == ReconAltitudeManager.INVALID_ALT ||
	    y0 == ReconAltitudeManager.INVALID_ALT) {
	    mTerrainGrade = INVALID_GRADE;
	    //Log.v(TAG,"invalid alt");
	    return;
	}
	if ((x1 - x0) < MIN_HORZ_DISTANCE_FOR_GRADE) {
	    mTerrainGrade = INVALID_GRADE;
	    //Log.v(TAG,"small horizontal");
	    return;
	}
	mTerrainGrade = (y1 - y0)/(x1 - x0);
	//Log.v(TAG,"Grade is "+mTerrainGrade);
    }
     public float getTerrainGrade() {return mTerrainGrade;}
}