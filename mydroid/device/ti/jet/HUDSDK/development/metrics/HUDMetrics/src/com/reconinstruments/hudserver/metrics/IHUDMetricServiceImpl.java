package com.reconinstruments.hudserver.metrics;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;

import com.reconinstruments.hudserver.metrics.route.AltitudeCalibrateBO;
import com.reconinstruments.hudserver.metrics.route.AltitudeDeltaBO;
import com.reconinstruments.hudserver.metrics.route.AltitudePressureBO;
import com.reconinstruments.hudserver.metrics.route.Distance3DBO;
import com.reconinstruments.hudserver.metrics.route.DistanceHorizontalBO;
import com.reconinstruments.hudserver.metrics.route.DistanceVerticalBO;
import com.reconinstruments.hudserver.metrics.route.GradeBO;
import com.reconinstruments.hudserver.metrics.route.Speed3DBO;
import com.reconinstruments.hudserver.metrics.route.SpeedHorizontalBO;
import com.reconinstruments.hudserver.metrics.route.SpeedPaceBO;
import com.reconinstruments.hudserver.metrics.route.SpeedVerticalBO;
import com.reconinstruments.os.metrics.BaseValue;
import com.reconinstruments.os.metrics.IHUDMetricListener;
import com.reconinstruments.os.metrics.IHUDMetricService;

public class IHUDMetricServiceImpl extends IHUDMetricService.Stub {
    @SuppressWarnings("unused")
    private final String TAG = this.getClass().getSimpleName();

    private final String HUD_SERVICE_STATS = "HUDServiceStats";

    MetricLocationListener mMetricLocationListener = null;

    AltitudePressureBO mAltitudePressureBO;
    AltitudeDeltaBO mAltitudeDeltaService;
    AltitudeCalibrateBO mAltitudeCalibrateService;

    SpeedHorizontalBO mSpeedHorizontalService;
    SpeedVerticalBO mSpeedVerticalService;
    Speed3DBO mSpeed3DService;
    SpeedPaceBO mSpeedPaceService;

    DistanceHorizontalBO mDistanceHorizontalService;
    DistanceVerticalBO mDistanceVerticalService;
    Distance3DBO mDistance3DService;

    GradeBO mGradeBOService;

    SharedPreferences mPersistantStats = null;

    private Context mContext = null;

    @SuppressLint("WorldReadableFiles")
    public IHUDMetricServiceImpl(Context context){
        mContext = context;

        mMetricLocationListener = MetricLocationListener.init(mContext);
        mPersistantStats = mContext.getSharedPreferences(HUD_SERVICE_STATS, Context.MODE_WORLD_READABLE);

        mAltitudePressureBO = new AltitudePressureBO(mContext);
        mAltitudeDeltaService = new AltitudeDeltaBO();
        mAltitudeCalibrateService = new AltitudeCalibrateBO();

        mSpeedHorizontalService = new SpeedHorizontalBO();
        mSpeedVerticalService = new SpeedVerticalBO();
        mSpeed3DService = new Speed3DBO();
        mSpeedPaceService = new SpeedPaceBO();

        mDistanceHorizontalService = new DistanceHorizontalBO();
        mDistanceVerticalService = new DistanceVerticalBO();
        mDistance3DService = new Distance3DBO();

        mGradeBOService = new GradeBO();

        onCreate();
    }

    public void onCreate(){
    }

    public void onDestory() {
    }

    @Override
    public void registerMetricListener(IHUDMetricListener listener, int listenerID, int metricID) throws RemoteException {
        BaseBO baseBO = MetricsBOs.get(metricID);
        if(baseBO != null){
            baseBO.registerMetricListener(listener, listenerID);
        }
    }

    @Override
    public void unregisterMetricListener(IHUDMetricListener listener, int listenerID, int metricID) throws RemoteException {
        BaseBO baseBO = MetricsBOs.get(metricID);
        if(baseBO != null){
            baseBO.unregisterMetricListener(listener);
        }
    }

    @Override
    public BaseValue getMetricValue(int metricID) throws RemoteException {
        BaseBO baseBO = MetricsBOs.get(metricID);
        if(baseBO != null){
            return baseBO.getMetric().getBaseValue();
        }
        return null;
    }
}
