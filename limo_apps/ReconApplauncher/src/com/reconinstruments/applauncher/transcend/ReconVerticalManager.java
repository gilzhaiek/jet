package com.reconinstruments.applauncher.transcend;
import android.util.Log;
import java.util.ArrayList;
public class ReconVerticalManager extends ReconStatsManager{
    private static final String TAG = "ReconVerticalManager";
    private ReconAltitudeManager mAltMan;
    private Stat<Float> mVert =
	new Stat<Float>(0f,"Vert");
    private Stat<Float> mAllTimeVert = 
	new Stat<Float> (0f,"AllTimeVert");
    private Stat<Float> mElevGain =
	new Stat<Float>(0f,"ElevGain");
    private Stat<Float>mAllTimeElevGain =
	new Stat<Float>(0f,"AllTimeElevGain");
    private MaxStat<Float> mBestElevGain =
	new MaxStat<Float>("BestElevGain",mElevGain);
    private float mTmpDeltaAlt = 0;
    public static final String BROADCAST_ACTION_STRING  = "RECON_MOD_BROADCAST_VERTICAL";
    @Override protected void populateStatLists() {
	addToLists(new Stat[]{mVert,mElevGain,mAllTimeElevGain,mBestElevGain},
		   new ArrayList[]{mToBeBundled});
	addToLists(new Stat[]{mElevGain,mVert},
		   new ArrayList[]{mToBeSaved});
	addToLists(new Stat[]{mBestElevGain},
		   new ArrayList[]{mToBeAllTimeSaved,mPostActivityValues});
	addToLists(new Stat[]{mAllTimeElevGain},
		   new ArrayList[]{mToBeAllTimeSaved});
    }
    public ReconVerticalManager(ReconTranscendService rts){
        mRTS = rts;
        mAltMan = mRTS.getReconAltManager();
	prepare(rts);
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override public void updateCumulativeValues() {
	mTmpDeltaAlt = mAltMan.getDeltaAlt_LP();
        if (mTmpDeltaAlt < 0) { // Vert added
            mVert.setValue(mVert.getValue().floatValue() -
			   mTmpDeltaAlt);//mVert should always be positive
        }
	else {                  // gain added
            mElevGain.setValue(mElevGain.getValue().floatValue() +
			       mTmpDeltaAlt);
        }
    }
    @Override
    public void updateComparativeValues() {
	super.updateComparativeValues();
	if (mBestElevGain.isRecord()) {
	    notifyBestRecordIfNecessary(0,0,"Best Elevation Gain");
	}
    }
    @Override
    public void updatePostActivityValues(){
	super.updatePostActivityValues();
	mAllTimeElevGain.setValue(mAllTimeElevGain.getValue().floatValue() +
				  mElevGain.getValue().floatValue());
    }
}
