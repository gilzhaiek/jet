//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashboard;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;
import android.widget.RemoteViews;

import com.reconinstruments.modservice.ReconMODServiceMessage;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.TranscendUtils;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.UIUtils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * 
 * <code>MetricManager</code> deals with all of the sensor bundle data and broadcast out the data as big widget and/or small widget.
 *
 */
public class MetricManager {
    
    private static final String TAG = MetricManager.class.getSimpleName();

    public final static String INTENT_ACTION_BIG_VIEWS = "com.reconinstruments.dashboard.BIG_VIEWS";
    public final static String INTENT_ACTION_SMALL_VIEWS = "com.reconinstruments.dashboard.SMALL_VIEWS";
    public final static String INTENT_ACTION_SPORT_STATE = "com.reconinstruments.dashboard.SPORT_STATE";
    public final static String INTENT_ACTION_VIEWS_COUNT = "com.reconinstruments.dashboard.VIEWS_COUNT";
    public final static String EXTRA_WIDGET = "WIDGET";
    public final static String EXTRA_COUNT = "COUNT";
    public final static String EXTRA_STATE = "STATE";
    public final static String EXTRA_TYPE = "TYPE"; // TYPE_DATA_BUNDLE or TYPE_REMOTE_VIEW
    public final static String EXTRA_SLOT = "SLOT"; //
    public final static String EXTRA_SUMMARY = "SUMMARY"; // activity summary
    public final static String EXTRA_TRIP_DATA_FILE_NAME= "TRIPDATA"; // trip data file name
    
    public final static int TYPE_DATA_BUNDLE = 0;
    public final static int TYPE_REMOTE_VIEW = 1;
    
    
    public enum MetricType {WATTAGE, HEARTRATE, CADENCE, SPEED, PACE, DISTANCE, MOVINGDURATION, TERRAINGRADE, ELEVATIONGAIN, CALORIE};

    // save the sports activity state coming from SPORTS_ACTIVITY_BUNDLE, initial value is 0 - no activity
    private int mActivityState = 0;

    private static MetricManager instance = null;
    
    //count how many small widgets should be shown up
    private int previousViewCount = 0;
    private int viewCount = 0;
    private int mSportType = ActivityUtil.SPORTS_TYPE_DEFAULT;
    
    //invalid values from Transcend Service
    // FIXME This section should go under separate jar file
    ////////////////////////////////////////////////////////////
    private static final int INVALID_POWER = 0X7FF;
    private static final short INVALID_HR = 255;
    private static final int INVALID_CADENCE = 0XFFFF;
    private static final float INVALID_SPEED = -1;
    private static final float INVALID_PACE = -1;
    private static final float INVALID_DISTANCE = -1;
    /////////////////////////////////////////////////////////////

    private String wattageValue = "---";
    private String wattageUnit;
    private String heartrateValue = "---";
    private String heartrateUnit;
    private String cadenceValue = "---";
    private String cadenceUnit;
    private String speedValue = "---";
    private String speedUnit;
    private String paceValue = "---";
    private String paceUnit;
    private String distanceValue = "---";
    private String distanceUnit;
    private String movingTimeValue = "---";
    private String movingTimeUnit;
    private String terrainGradeValue = "---";
    private String terrainGradeUnit;
    private String elevationGainValue = "---";
    private String elevationGainUnit;
    private String calorieValue = "---";
    private String calorieUnit;
    
    private Context mContext;
    
    private final static String TRANSCEND_SERVICE = "RECON_MOD_SERVICE";
    private final static int POST_DELAYED = (int) (0.5 * 1000);
    

    private final Handler mDashboardServiceHandler = new Handler();
    
    // the messenger for handling message coming from MODService / Transcend Serivce
    private Messenger mIncomingMessenger = null;
    // the messenger for sending query message to MODService / Transcend Serivce
    private Messenger mOutgoingMessenger = null;
    
    private DashLayout mLayout;
    
    private boolean mHasGpsFix = false; // TODO put description
    private boolean mShowNoGps = false; // TODO put description

    protected MetricManager(Context context) {
        mContext = context;
        mSportType = ActivityUtil.getCurrentSportsType(context);
        loadSportsLayout(mSportType);
    }
    
    public static MetricManager getInstance(Context context) {
        if(instance == null) {
            instance = new MetricManager(context);
        }
        return instance;
    }
    
    /**
     * Getter for mActivityState
     *
     * @return an <code>int</code> value
     */
    public int getActivityState(){
        return mActivityState;
    }

    public void setActivityState(int state){
        mActivityState = state;
    }
    
    public int getSportType(){
        return mSportType;
    }
    
    public String getLastTripDataFileName(){
        if(mSportType > ActivityUtil.SPORTS_TYPE_OTHER){
          //return "tripdata.csv";
            //fixed value to hold the current activity data
            String name = "simpleLatLng.tmp.txt";
            Log.i(TAG, "last trip file is: " + name);
            return name;  
        }
        return "simpleLatLng.tmp.txt"; //by default
    }
    
    public boolean hasGpsFix(){
        return mHasGpsFix;
    }
    
    public String getDuration(){
        return movingTimeValue;
    }

    /**
     * bind to transcend service and load custom dashboard layout.
     */
    public void init(){
        Intent intent = new Intent(TRANSCEND_SERVICE);
        mContext.bindService(intent, mTranscendServiceConnection, Context.BIND_AUTO_CREATE);
        mContext.registerReceiver(transcendReceiver,
                                  new IntentFilter(TranscendUtils.FULL_INFO_UPDATED)); 
    }
    
    /**
     * unbind transcend service, clean up the instance.
     */
    public void stop(){
        mContext.unbindService(mTranscendServiceConnection);
        mContext.unregisterReceiver(transcendReceiver); 
        instance = null;
    }

    /**
     * reload sports layout when the user switches to the different sports type, 
     * it will be called in JetDashboard onResume method
     */
    public void loadSportsLayout(int sportsType){
        mLayout = DashLayout.loadLayout(sportsType);
    }
    
    public void startSportsActivity() {
        sendCommandToTranscendService(ActivityUtil.MSG_START_SPORTS_ACTIVITY);
    }
    
    public void pauseSportsActivity() {
        sendCommandToTranscendService(ActivityUtil.MSG_PAUSE_SPORTS_ACTIVITY);
    }
    
    public void resumeSportsActivity() {
        sendCommandToTranscendService(ActivityUtil.MSG_RESTART_SPORTS_ACTIVITY);
    }
    
    public void stopSportsActivity() {
        sendCommandToTranscendService(ActivityUtil.MSG_STOP_SPORTS_ACTIVITY);
    }
    
    public void saveSportsActivity() {
        sendCommandToTranscendService(ActivityUtil.MSG_SAVE_SPORTS_ACTIVITY);
    }
    
    public void discardSportsActivity() {
        sendCommandToTranscendService(ActivityUtil.MSG_DISCARD_SPORTS_ACTIVITY);
    }
    
    private void sendCommandToTranscendService(int i) {
        try {
            Message msg = Message.obtain(null,i, 0, 0);
            msg.replyTo = null; // there is no any message returned back
            if(mOutgoingMessenger != null)
                mOutgoingMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    ///////////////////////////////////////////////////////////////
    // New paradigm for accessing data from Transcend service
    ///////////////////////////////////////////////////////////////
    BroadcastReceiver transcendReceiver = new BroadcastReceiver () {
            @Override
            public void onReceive(Context c, Intent i) {
                Bundle data = i.getBundleExtra("FullInfo");
                handleIfActivityTypeChanged(data);
                handleEverHadGps(data);
                handleMetricBundle(data, mLayout);
            }
        };
    ///////////////////////////////////////////////////////////////

    /**
     * 
     * handle the message bundle data returning back from transcend service
     *
     */
    private class TranscendServiceMessageHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            default:
                super.handleMessage(msg);
            }
        }
    }


    /**
     * Check to see if we have ever had Gps. If the answer is false
     * then broadcast a message to the dashboard app to show search
     * for dialogue message
     */
    
    private void handleEverHadGps (Bundle data) {
        mHasGpsFix = data.getBoolean("LOCATION_BUNDLE_VALID");
    }

    private void handleIfActivityTypeChanged(Bundle data) {
        //Log.v(TAG,"handleIfActivityTypeChanged");
        int sportsType = data.getBundle("SPORTS_ACTIVITY_BUNDLE").getInt("Type");
        if (mSportType != sportsType) {
            //Log.v(TAG,"Change layout");
            mSportType = sportsType;
            loadSportsLayout(mSportType);
        }
    }
     

    private ServiceConnection mTranscendServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mOutgoingMessenger = new Messenger(service);
                mIncomingMessenger = new Messenger(new TranscendServiceMessageHandler());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mOutgoingMessenger = null;
            }

        };

    /**
     * If there is no layout, it goes throught the pre defined metric order. 
     * If there is a layout, it uses the custom layout order.
     * 
     * @param data a <code>Bundle</code> value
     * @param layout a <code>DashLayout</code> value
     */
    public void handleMetricBundle(Bundle data, DashLayout layout){
        viewCount = 0;
        if(layout != null){
            handleMetric(data, layout.mainMetric);
            for(String secondaryMetric: layout.secondaryMetrics){
                handleMetric(data, secondaryMetric);
            }
        }else{
            //Log.i(TAG, "using defalut layout");
            handleMetric(data, null);
        }
        if(previousViewCount == viewCount){
            return;
        }
        previousViewCount = viewCount;
        broadcastViewsCount((int) Math.ceil((viewCount -1 )/2.0));
    }
    
    /**
     * broadcast sports activity state which coming with SPORTS_ACTIVITY_BUNDLE.
     */
    private void broadcastSportsState() {
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION_SPORT_STATE);
        intent.putExtra(EXTRA_STATE, mActivityState);
        mContext.sendBroadcast(intent);
    }
    
    /**
     * Generate the big widget bundle or remote view depends on asBundle param.
     * @param asBundle ture to generate bundle, false to generate remote view
     * @param metricType the sensor type
     * @param value metric value
     * @param unit metric unit
     * @param icon metric icon
     * @param disconnectedIcon the icon when the sensor is disconnected
     */
    private void broadcastBigWidget(boolean asBundle, MetricType metricType, String value, String unit, int icon, int disconnectedIcon) {
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION_BIG_VIEWS);
        BigWidget widget = new BigWidget(metricType, value, unit, icon, null, disconnectedIcon);
        if(asBundle){
            intent.putExtra(EXTRA_WIDGET, widget.toBundle());
            intent.putExtra(EXTRA_TYPE, TYPE_DATA_BUNDLE);
        }else{
            RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.big_widget);
            remoteView.setTextViewText(R.id.value, value);
            remoteView.setTextViewText(R.id.unit, unit);
            if(disconnectedIcon != 0){
                remoteView.setImageViewResource(R.id.image, icon);
                Drawable drawable = UIUtils.getDrawableFromAPK(mContext.getPackageManager(), widget.getPackageName(), widget.getMetricTypeIcon());
                remoteView.setImageViewBitmap(R.id.image, ((BitmapDrawable)drawable).getBitmap());
            }
            intent.putExtra(EXTRA_WIDGET, remoteView);
            intent.putExtra(EXTRA_TYPE, TYPE_REMOTE_VIEW);
        }
        mContext.sendBroadcast(intent);
    }

    /**
     * Describe <code>broadcastSmallWidget</code> method here.
     *
     * @param asBundle a <code>boolean</code> value indicates if the
     * bundle should be sent or a whole remote view object
     * @param item an <code>int</code> value What is the total number
     * of of views sof far. This will be soon deprecated. The logic
     * should be handled by the receiver end.
     * @param metricType a <code>MetricType</code> value. The sensor type
     * @param value a <code>String</code> value The value of the sensor
     * @param unit a <code>String</code> value the unit that is shown
     * @param disconnectedIcon an <code>int</code> value
     */
    private void broadcastSmallWidget(boolean asBundle, int item, MetricType metricType, String value, String unit, int disconnectedIcon) {
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION_SMALL_VIEWS);
        if(asBundle){
            SmallWidget widget = new SmallWidget(metricType, value, unit, item, null, disconnectedIcon);
            intent.putExtra(EXTRA_WIDGET, widget.toBundle());
            intent.putExtra(EXTRA_TYPE, TYPE_DATA_BUNDLE);
        }else{
            RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.small_widget);
            remoteView.setTextViewText(R.id.value, value);
            remoteView.setTextViewText(R.id.unit, unit);
            intent.putExtra(EXTRA_WIDGET, remoteView);
            intent.putExtra(EXTRA_SLOT, item);
            intent.putExtra(EXTRA_TYPE, TYPE_REMOTE_VIEW);
        }
        mContext.sendBroadcast(intent);
    }
    
    private void broadcastViewsCount(int count){
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION_VIEWS_COUNT);
        intent.putExtra(EXTRA_COUNT, count);
        mContext.sendBroadcast(intent);
    }
    
    /**
     * get the latest sensor metric data and generate them as bundle
     */
    public Bundle getLatestData() {
        Bundle b = new Bundle();
        String invalidStr = mContext.getString(R.string.invalid_value);
        if (!invalidStr.equals(wattageValue)) {
            b.putString(MetricType.WATTAGE.name(), wattageValue);
            b.putString(MetricType.WATTAGE.name()+"_UNIT", wattageUnit);
        }
        if (!invalidStr.equals(heartrateValue)) {
            b.putString(MetricType.HEARTRATE.name(), heartrateValue);
            b.putString(MetricType.HEARTRATE.name()+"_UNIT", heartrateUnit);
        }
        if (!invalidStr.equals(cadenceValue)) {
            b.putString(MetricType.CADENCE.name(), cadenceValue);
            b.putString(MetricType.CADENCE.name()+"_UNIT", cadenceUnit);
        }
        if (!invalidStr.equals(speedValue)) {
            b.putString(MetricType.SPEED.name(), speedValue);
            b.putString(MetricType.SPEED.name()+"_UNIT", speedUnit);
        }
        if (!invalidStr.equals(paceValue)) {
            Log.i(TAG, "paceValue=" + paceValue);
            b.putString(MetricType.PACE.name(), paceValue);
            b.putString(MetricType.PACE.name()+"_UNIT", paceUnit);
        }
        if (!invalidStr.equals(distanceValue)) {
            b.putString(MetricType.DISTANCE.name(), distanceValue);
            b.putString(MetricType.DISTANCE.name()+"_UNIT", distanceUnit);
        }
        if (!invalidStr.equals(movingTimeValue)) {
            b.putString(MetricType.MOVINGDURATION.name(), movingTimeValue);
            b.putString(MetricType.MOVINGDURATION.name()+"_UNIT", movingTimeUnit);
        }
        if (!invalidStr.equals(terrainGradeValue)) {
            b.putString(MetricType.TERRAINGRADE.name(), terrainGradeValue);
            b.putString(MetricType.TERRAINGRADE.name()+"_UNIT", terrainGradeUnit);
        }
        if (!invalidStr.equals(elevationGainValue)) {
            b.putString(MetricType.ELEVATIONGAIN.name(), elevationGainValue);
            b.putString(MetricType.ELEVATIONGAIN.name()+"_UNIT", elevationGainUnit);
        }
        if (!invalidStr.equals(calorieValue)) {
            b.putString(MetricType.CALORIE.name(), calorieValue);
            b.putString(MetricType.CALORIE.name()+"_UNIT", calorieUnit);
        }
        return b;
    }

    /**
     * broadcast the metric data to big view or small view depends on <code>main</code> param
     */
    private void broadcastMetricData(MetricType type, String value, String unit, boolean main){
        // TODO Document hte logic
        if(main){
            if(!mHasGpsFix && (type == MetricType.SPEED || type == MetricType.TERRAINGRADE || type == MetricType.PACE)){
                broadcastBigWidget(true, type, value, unit, 1, R.drawable.no_gps);
                mShowNoGps = true;
            }else{
                broadcastBigWidget(true, type, value, unit, 1, 0);
            }
            viewCount ++;
        }else{
            if(!mHasGpsFix && !mShowNoGps && (type == MetricType.SPEED || type == MetricType.TERRAINGRADE || type == MetricType.PACE)){
                broadcastSmallWidget(true, viewCount, type, value, unit, R.drawable.gps_lost);
                mShowNoGps = true;
            }else{
                broadcastSmallWidget(true, viewCount, type, value, unit, 0);
            }
            viewCount ++;
        }
    }
    
    private Pair<String, String> converValueAndUnit(MetricType type, int value, int invalidValue){
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(0);
        df.setGroupingUsed(false);
        String valueStr = "";
        String unitStr = "";
        Pair<String, String> pair;
        if(value != invalidValue){
            switch(type){
            case WATTAGE:
                valueStr = combinePrefixZero(df.format(value));
                unitStr = "W";
                break;
            case HEARTRATE:
                valueStr = combinePrefixZero(df.format(value)); 
                unitStr = "BPM";
                break;
            case CADENCE:
                valueStr = combinePrefixZero(df.format(value)); 
                unitStr = "RPM";
                break;
            default:
                break;
            }
            pair = new Pair<String, String>(valueStr, unitStr);
            broadcastMetricData(type, pair.first, pair.second, viewCount > 0 ? false : true);
        }else{
            valueStr = mContext.getString(R.string.invalid_value);
            unitStr = "";
            pair = new Pair<String, String>(valueStr, unitStr);
        }
        return pair;
    }

    private Pair<String, String> converValueAndUnit(MetricType type, float value, float invalidValue){
        boolean isMetric = SettingsUtil.getUnits(mContext) == SettingsUtil.RECON_UINTS_METRIC;
        String valueStr = "";
        String unitStr = "";
        Pair<String, String> pair;
        if(Float.compare(value, invalidValue) != 0 && value != Float.NaN && value != Float.POSITIVE_INFINITY && value != Float.NEGATIVE_INFINITY){
            DecimalFormat df = obtain();
            
            try {
                df.setMaximumFractionDigits(0);
                df.setGroupingUsed(false);
                ///////////////////////////////////
                switch(type){
                case SPEED:
                    valueStr = combinePrefixZero(isMetric ? df.format(value) : df.format(ConversionUtil.kmsToMiles(value)));
                    unitStr = isMetric ? mContext.getString(R.string.kmh): mContext.getString(R.string.mph);
                    break;
                case PACE:
                    valueStr = isMetric ? ConversionUtil.secondsToMinutes((long)value):
                    ConversionUtil.secondsToMinutes((long)(value/ConversionUtil
                                                           .KM_MILE_RATIO));
                    unitStr = isMetric ? mContext.getString(R.string.minkm):
                        mContext.getString(R.string.minmi);
                    break;
                case DISTANCE:
                    if(value < 1000){ // something like 500m
                        valueStr = combinePrefixZero(df.format(value)); 
                        unitStr = "M";
                    }else if(value < 1000 * 10){ // something like 1.5km
                        df.setMaximumFractionDigits(2);
                        valueStr = combinePrefixZero(df.format(value/1000)); 
                        unitStr = "KM";
                    }else{ // something like 015km
                        df.setMaximumFractionDigits(1);
                        valueStr = combinePrefixZero(df.format(value/1000)); 
                        unitStr = "KM";
                    }
                    break;
                case TERRAINGRADE:
                    if(Float.compare(value, invalidValue) > 0){
                        valueStr = combinePrefixZero(df.format(value*100.0f)); 
                    }else{
                        valueStr = mContext.getString(R.string.invalid_value);
                    }
                    unitStr = "%";
                    break;
                case ELEVATIONGAIN:
                    valueStr = combinePrefixZero(df.format(value));
                    unitStr = "M";
                    break;
                case CALORIE:
                    if(Float.compare(value, invalidValue) > 0){
                        valueStr = combinePrefixZero(df.format(value));
                    }else{
                        valueStr = mContext.getString(R.string.invalid_value);
                    }
                    unitStr = "CAL";
                    break;
                default:
                    break;
                }
            }
            finally {
                release(df);

            }
        }else{
            valueStr = mContext.getString(R.string.invalid_value);
            unitStr = "";
            //keep the unit even if the value is invalid for temporally
            switch(type){
            case SPEED:
                unitStr = isMetric ? mContext.getString(R.string.kmh): mContext.getString(R.string.mph);
                break;
            case PACE:
                unitStr = isMetric ? mContext.getString(R.string.minkm): mContext.getString(R.string.minmi);
                break;
            case DISTANCE:
                if(value < 1000){ // something like 500m
                    unitStr = "M";
                }else if(value < 1000 * 10){ // something like 1.5km
                    unitStr = "KM";
                }else{ // something like 015km
                    unitStr = "KM";
                }
                break;
            case TERRAINGRADE:
                unitStr = "%";
                break;
            case ELEVATIONGAIN:
                unitStr = "M";
                break;
            case CALORIE:
                unitStr = "CAL";
                break;
            default:
                break;
            }
        }
        pair = new Pair<String, String>(valueStr, unitStr);
        broadcastMetricData(type, pair.first, pair.second, viewCount > 0 ? false : true);
        return pair;
    }

    private Pair<String, String> converValueAndUnit(MetricType type, long value, long invalidValue){
        SimpleDateFormat df = null;
        if(value < 60*60*1000){
            df = new SimpleDateFormat("mm:ss");
        }else{
            df = new SimpleDateFormat("HH:mm:ss");
        }
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String valueStr = "";
        String unitStr = "";
        Pair<String, String> pair;
        if(value > invalidValue){
            switch(type){
            case MOVINGDURATION:
                valueStr = df.format(value); 
                unitStr = "";
                break;
            default:
                break;
            }
            pair = new Pair<String, String>(valueStr, unitStr);
        }else{
            valueStr = "00:00";
            unitStr = "";
            pair = new Pair<String, String>(valueStr, unitStr);
        }
        return pair;
    }
   
    /**
     * handle the integer metric data, now are wattage, heart rate, cadence
     */
    private void handleMetric(Bundle data, String subBundleName, MetricType type, String bundleParamName, int invalidValue){
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(0);
        df.setGroupingUsed(false);
        Bundle bundle = (Bundle) data.get(subBundleName);
        Pair<String, String> pair = null;
        if (bundle != null) {
            int value = bundle.getInt(bundleParamName);
            pair = converValueAndUnit(type, value, invalidValue);
            switch(type){
            case WATTAGE:
                //TODO set average wattage
                wattageValue = pair.first; //combinePrefixZero(df.format(bundle.getInt("AverageWattage"))); 
                wattageUnit = "W";
                break;
            case HEARTRATE:
                //TODO set average heartrate
                heartrateValue = pair.first; //combinePrefixZero(df.format(bundle.getInt("AverageHeartrate"))); 
                heartrateUnit = "BPM";
                break;
            case CADENCE:
                //TODO set average cadence
                cadenceValue = pair.first; //combinePrefixZero(df.format(bundle.getInt("AverageCadence"))); 
                cadenceUnit = "RPM";
                break;
            default:
                break;
            }
        }
    }
    
    
    static final ArrayList<DecimalFormat> dfListAvail = new ArrayList<DecimalFormat>();
    
    private DecimalFormat obtain() {
        if(dfListAvail.size() == 0) {
            for(int i = 0; i < 5; i++) {
                dfListAvail.add(new DecimalFormat());
            }
        } 
        DecimalFormat df = dfListAvail.get(dfListAvail.size()-1);
        dfListAvail.remove(dfListAvail.size()-1);
        
        return df;
    }
    
    private void release(DecimalFormat df) {
        dfListAvail.add(df);
    }    
    
    /**
     * handle the float metric data, now are speed, distance
     */
    private void handleMetric(Bundle data, String subBundleName, MetricType type, String bundleParamName, float invalidValue){
        boolean isMetric = SettingsUtil.getUnits(mContext) == SettingsUtil.RECON_UINTS_METRIC;

        DecimalFormat df = obtain();
        try {
            df.setMaximumFractionDigits(0);
            df.setGroupingUsed(false);
            Bundle bundle = (Bundle) data.get(subBundleName);
            Pair<String, String> pair = null;
            if (bundle != null) {
                float value = bundle.getFloat(bundleParamName);
                pair = converValueAndUnit(type, value, invalidValue);
                switch(type){
                case SPEED:
                    //set average speed
                    speedValue = combinePrefixZero(isMetric ? df.format(bundle.getFloat("AverageSpeed")) : df.format(ConversionUtil.kmsToMiles(bundle.getFloat("AverageSpeed"))));
                    speedUnit = isMetric ? mContext.getString(R.string.kmh): mContext.getString(R.string.mph);
                    break;
                case PACE:
                    //set average pace
                    paceValue = ConversionUtil.speedToPace(bundle.getFloat("AverageSpeed"), isMetric);
                    paceUnit = isMetric ? mContext.getString(R.string.minkm): mContext.getString(R.string.minmi);
                    break;
                case TERRAINGRADE:
                    terrainGradeValue = pair.first; 
                    terrainGradeUnit = pair.second;
                    break;
                case DISTANCE:
                    distanceValue = pair.first; 
                    distanceUnit = pair.second;
                    break;
                case ELEVATIONGAIN:
                    elevationGainValue = pair.first; 
                    elevationGainUnit = pair.second;
                    break;
                case CALORIE:
                    calorieValue = pair.first; 
                    calorieUnit = pair.second;
                    break;
               default:
                    break;
                }
            }
        }
        finally {
            release(df);
        }
    }

    /**
     * handle the long metric data, now is duration
     */
    private void handleMetric(Bundle data, String subBundleName, MetricType type, String bundleParamName, long invalidValue){
        Bundle bundle = (Bundle) data.get(subBundleName);
        Pair<String, String> pair = null;
        if (bundle != null) {
            if(type == MetricType.MOVINGDURATION){
                int status = bundle.getInt("Status"); // 0 no activiy, 1, ongoing, 2 paused
                if(mActivityState != status){
                    mActivityState = status;
                    broadcastSportsState();
                }
                mSportType = bundle.getInt("Type");//set sports type
            }
            long value = bundle.getLong(bundleParamName);
            pair = converValueAndUnit(type, value, invalidValue);
            broadcastMetricData(type, pair.first, pair.second, viewCount > 0 ? false : true);
            switch(type){
            case MOVINGDURATION:
                movingTimeValue = pair.first; 
                movingTimeUnit = pair.second;
                break;
            default:
                break;
            }
        }
    }
    
    /**
     * the main method to handle metric, it deals with two cases: with or without custom layout
     */
    private void handleMetric(Bundle data, String metric){
        if(metric != null){
            if("power".equalsIgnoreCase(metric)){
                handleMetric(data, "POWER_BUNDLE", MetricType.WATTAGE, "Wattage", INVALID_POWER);
            }else if("heartrate".equalsIgnoreCase(metric)){
                handleMetric(data, "HR_BUNDLE", MetricType.HEARTRATE, "HeartRate", INVALID_HR);
            }else if("cadence".equalsIgnoreCase(metric)){
                handleMetric(data, "CADENCE_BUNDLE", MetricType.CADENCE, "Cadence", INVALID_CADENCE);
            }else if("speed".equalsIgnoreCase(metric)){
                handleMetric(data, "SPEED_BUNDLE", MetricType.SPEED, "Speed", INVALID_SPEED);
            }else if("pace".equalsIgnoreCase(metric)){
                //convert to pace based on speed data
                handleMetric(data, "SPEED_BUNDLE", MetricType.PACE, "Pace", INVALID_PACE);
            }else if("distance".equalsIgnoreCase(metric)){
                handleMetric(data, "DISTANCE_BUNDLE", MetricType.DISTANCE, "Distance", INVALID_DISTANCE);
            }else if("duration".equalsIgnoreCase(metric)){
                handleMetric(data, "SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "Durations", 0l);
            }else if("grade".equalsIgnoreCase(metric)){ 
                handleMetric(data, "GRADE_BUNDLE", MetricType.TERRAINGRADE, "TerrainGrade", -10000f);
                // ^ReconGRADEManager.INVAlAID_GRADE
            }else if("elevgain".equalsIgnoreCase(metric)){ 
                handleMetric(data, "VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "ElevGain", 0.0f);
                // ^elev gain is always positive
            }else if("calorie".equalsIgnoreCase(metric)){ 
                handleMetric(data, "CALORIE_BUNDLE", MetricType.CALORIE, "TotalCalories", -1.0f);
            }
        }else{
            handleMetric(data, "POWER_BUNDLE", MetricType.WATTAGE, "Wattage", INVALID_POWER);
            handleMetric(data, "HR_BUNDLE", MetricType.HEARTRATE, "HeartRate", INVALID_HR);
            handleMetric(data, "CADENCE_BUNDLE", MetricType.CADENCE, "Cadence", INVALID_CADENCE);
            if(mSportType != ActivityUtil.SPORTS_TYPE_RUNNING){
                handleMetric(data, "SPEED_BUNDLE", MetricType.SPEED, "Speed", INVALID_SPEED);
            }else{
                //convert to pace based on speed data
                handleMetric(data, "SPEED_BUNDLE", MetricType.PACE, "Pace", INVALID_PACE);
            }
            handleMetric(data, "DISTANCE_BUNDLE", MetricType.DISTANCE, "Distance", INVALID_DISTANCE);
            handleMetric(data, "SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "Durations", 0l);
            handleMetric(data, "GRADE_BUNDLE", MetricType.TERRAINGRADE, "TerrainGrade", 0.0f);
            handleMetric(data, "VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "ElevGain", 0.0f);
            handleMetric(data, "CALORIE_BUNDLE", MetricType.CALORIE, "TotalCalories", -1.0f);
       }
    }
    
    private String combinePrefixZero(String value){
        String res = value;
        if(value.length() == 1){
            res = "00" + value;
        }else if(value.length() == 2){
            res = "0" + value;
        }
        return res;
    }
}
