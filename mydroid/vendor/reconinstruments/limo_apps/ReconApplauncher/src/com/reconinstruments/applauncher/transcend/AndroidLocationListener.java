package com.reconinstruments.applauncher.transcend;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.connect.messages.XMLMessage;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.connect.messages.LocationMessage;
import com.reconinstruments.connect.messages.LocationRequestMessage;
import com.reconinstruments.connect.messages.LocationRequestMessage.LocationCommand;
import com.reconinstruments.connect.messages.LocationRequestMessage.LocationRequest;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TimeZone;
/**
 * Class to handle the complexitity of listening for location updates
 * from the system and invoking propoer functions such as logger. This
 * class will also broadcast its location to be picked up by the
 * mobile app. In addition it responds to location update requests
 * from the mobile app.
 *
 */
public class AndroidLocationListener implements LocationListener, GpsStatus.Listener {
    public  boolean mHasHadGPSfix = false;
    private LocationManager mlocManager;//Android location manager
    private GpsStatus mGpsStatus=null;
    private static final String TAG = "AndroidLocationListener";
    ReconTranscendService mRTS;
    private int phoneGPSdelay = -1;//Same as before
    private long lastGPSmessage = 0;

    public static final String ACTION_ACTIVATE_GPS = "RECON_ACTIVATE_GPS";
    public static final String ACTION_DEACTIVATE_GPS = "RECON_DEACTIVATE_GPS";
    public static final String EXTRA_GPS_CLIENT = "RECON_GPS_CLIENT";
    private ArrayList<String> mGpsClientList = new ArrayList<String>();
    private enum SmartGpsState {
        PASSIVE_GPS,
        ACTIVE_GPS,
        PENDING_PASSIVE_GPS,
    }
    private SmartGpsState meGpsState = SmartGpsState.PASSIVE_GPS;
    private Timer mGpsTimer = null;
    private boolean mSmartGpsEnabled = true;
    private static final int GPS_DELAY_OFF = 5000; // GPS turn off delay in ms. Only used if we never had a fix.
    private Handler mHandler = new Handler();

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mSmartGpsEnabled = (SettingsUtil.getSystemIntOrSet(mRTS, "RECON_GPS_ALWAYS_ON", 0) == 0);
            boolean active = !mSmartGpsEnabled;
            boolean change = false;

            // If we're suppose to go active, then we can go ahead and become active. If we're suppose to go
            // passive, make sure the client list is empty (ie. no one is still requesting for us to be active)
            // and that we're not currently in an activity.
            if (active ||
                (!active && mGpsClientList.isEmpty() && !mRTS.getSportsActivityManager().isDuringSportsActivity())) {
                change = true;
            }

            if (change) {
                requestGps(active);
            }
        }
    };

    public AndroidLocationListener(ReconTranscendService rts) {
        mRTS = rts;
    }

    /**
     * Boilerplate for initialization. Get the location manager stuff
     * running and register relevant receivers
     *
     */
    public void init() {
        mSmartGpsEnabled = (SettingsUtil.getSystemIntOrSet(mRTS, "RECON_GPS_ALWAYS_ON", 0) == 0);
        mlocManager = (LocationManager)mRTS.getSystemService(Context.LOCATION_SERVICE);
        mlocManager.requestLocationUpdates((mSmartGpsEnabled) ? LocationManager.PASSIVE_PROVIDER :
                                                                LocationManager.GPS_PROVIDER, 0, 0, this);
        mlocManager.addGpsStatusListener(this);
        mRTS.registerReceiver(bluetoothReceiver,
                    new IntentFilter(XMLMessage.LOCATION_REQUEST_MESSAGE));

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ACTIVATE_GPS);
        filter.addAction(ACTION_DEACTIVATE_GPS);

        mRTS.registerReceiver(smartGpsReceiver, filter);

        // Register a ContentObserver to know when the Smart GPS setting is changed
        ContentResolver resolver = mRTS.getContentResolver();
        Uri uri = Settings.System.getUriFor("RECON_GPS_ALWAYS_ON");
        resolver.registerContentObserver(uri, false, mObserver);
    }
    /**
     * Cleanup stuff: Shutdown location manager update and unregister
     * receivers.
     *
     */
    public void cleanUp() {
        mlocManager.removeUpdates(this);
        mRTS.unregisterReceiver(bluetoothReceiver);
        ContentResolver resolver = mRTS.getContentResolver();
        resolver.unregisterContentObserver(mObserver);
    }
    @Override
    public void onLocationChanged(Location location) {
        // Log.v(TAG,"onLocationChanged");
        // Log.v(TAG,"The speed is "+location.getSpeed()+"");
        if (mGpsStatus == null) {
            Log.v(TAG,"gps status is null ignore location change event until not null");
            return;
        }
        if (!mHasHadGPSfix){
            mHasHadGPSfix = true;
        }
        int numSats = 0;
        // TODO multithreading safe?
        mRTS.getReconTimeManager().updateMembers(location);
        Iterator<GpsSatellite> ss = mGpsStatus.getSatellites().iterator();
        GpsSatellite s;
        while (ss.hasNext()){
            s =ss.next();
            if (s.usedInFix()){
            numSats++;
            }
        }
        if (numSats == 0) {
            return;
        }
        // Log.d(TAG, "Num sats " + numSats);
        mRTS.getReconLocationManager().updateMembers(location,numSats);
        if (location.hasAltitude()) {
            mRTS.getReconAltManager().setTempGpsAlt((float)location.getAltitude());
        } // TODO perhaps a case for handing it otherwise
        mRTS.getReconDataLogHandler().writeLogEntry();
        if(phoneGPSdelay!=-1){
            long timePassed = (long)(new Date().getTime()-lastGPSmessage);
            //Log.d(TAG, "last gps message sent: "+lastGPSmessage);
            //Log.d(TAG, "time passed since last gps message sent: "+timePassed);
            if(timePassed>phoneGPSdelay*1000){

            //broadcasting the GPS location so that HQMobile can intercept it
            Intent myi = new Intent();
            myi.setAction(ConnectHelper.GEN_MSG);
            myi.putExtra("message", LocationMessage.compose(location));
            Context c = mRTS.getApplicationContext();
            c.sendBroadcast(myi);

            lastGPSmessage = new Date().getTime();
            }
        }
        //update system time if using gps based time
        if(SettingsUtil.getTimeAuto(mRTS)) {
            Calendar c = Calendar.getInstance(TimeZone.getDefault());
            int tzCorrection = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET); // Timezone + Daylight savings offset from UTC

            long gpsTime = location.getTime() + (DeviceUtils.isLimo() ? tzCorrection : 0);
            long sysTime = System.currentTimeMillis();
            if(Math.abs(gpsTime - sysTime) > 1000) { // If off by more than 1 sec, correct time
            Log.d(TAG, "Time Mismatch by " + Math.abs(gpsTime - sysTime) + ", use location time, LocationTime : " + gpsTime + " , SystemTime : " + sysTime);
            SystemClock.setCurrentTimeMillis(gpsTime); // Set's wall clock, so need to add TZ & DST offsets
            mRTS.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
            }
        }
    }
    public void onProviderDisabled(String provider) { }
    public void onProviderEnabled(String provider) { }
    private int mPreviousGpsStatusInt = -1;
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (status!=LocationProvider.AVAILABLE){
            //If there is no location then create one
            if (mRTS.getReconLocationManager().getLocation() == null){
            Location location = new Location("GPS");
            mRTS.getReconLocationManager().setLocation(location);
            }
            mRTS.getReconLocationManager().setMockSpeed(0);
        }
        // Notify Phone
        if (mPreviousGpsStatusInt != status){
            HudPhoneStatusExchanger.sendGpsStatusToPhone(mRTS, status);
            mPreviousGpsStatusInt = status;
        }
    }
    public void onGpsStatusChanged(int event) {
        mGpsStatus = mlocManager.getGpsStatus(mGpsStatus);
    }
    BroadcastReceiver bluetoothReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle extras = intent.getExtras();
            String xml;
            Object msgData = extras.get("message");
            if (msgData instanceof byte[]) {
                byte[] msgBytes = (byte[]) msgData;
                HUDConnectivityMessage msg = new HUDConnectivityMessage(msgBytes);
                xml = new String(msg.getData());
            } else {
                // For backwards compatibilty
                Log.w(TAG, "GPS location update request received in a deprecated format");
                xml = (String) msgData;
            }
            LocationRequest request = LocationRequestMessage.parse(xml);
            if(request.action==LocationCommand.ENABLE)
                phoneGPSdelay = request.delay;
            else if(request.action==LocationCommand.DISABLE)
                phoneGPSdelay = -1;
            lastGPSmessage = 0;
            Log.d(TAG, "phone gps delay: "+phoneGPSdelay);
        }
    };

    public void requestGps(boolean active, String client) {
        if (active) {
            // Make sure the client is not already in the list.
            if (!mGpsClientList.contains(client) && mGpsClientList.add(client) && mSmartGpsEnabled) {
                // Check to see if our GPS timer is running. If so, cancel the timer.
                if (meGpsState == SmartGpsState.PENDING_PASSIVE_GPS && mGpsTimer != null) {
                    mGpsTimer.cancel();
                    mGpsTimer = null;

                    // Reset the state to active.
                    meGpsState = SmartGpsState.ACTIVE_GPS;
                }

                // If we're not active, we should become active now.
                if (meGpsState != SmartGpsState.ACTIVE_GPS) {
                    requestGps(true);
                }
            }
        } else {
            if (mGpsClientList.remove(client) && mGpsClientList.isEmpty() && mSmartGpsEnabled) {
                // The client was removed from the list and the list is now empty.
                // Check to see if we're currently in an activity. If we are, then
                // then we still need to be active. Otherwise start a timer to count
                // down when we should rever to passive.
                if (!mRTS.getSportsActivityManager().isDuringSportsActivity()) {
                    // Not in an activity start a timer and wait until it expires before going
                    // passive.
                    meGpsState = SmartGpsState.PENDING_PASSIVE_GPS;
                    mGpsTimer = new Timer();
                    Log.d(TAG, "GPS will turn off in " + GPS_DELAY_OFF + " ms.");
                    mGpsTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // Timer has expired. If the state is still pending to be
                            // passive, then let's go passive.
                            Log.d(TAG, "GPS Timer has expired. Going passive.");
                            mHandler.post(mRequestGpsRunnable);
                        }
                    }, GPS_DELAY_OFF);
                }
            }
        }
    }

    private final Runnable mRequestGpsRunnable = new Runnable() {
        public void run() {
            requestGps(false);
        }
    };

    private void requestGps(boolean active) {
        // First unregister for location updates.
        mlocManager.removeUpdates(this);

        // Now request for location updates based on active flag.
        mlocManager.requestLocationUpdates((active) ? LocationManager.GPS_PROVIDER:
                                                      LocationManager.PASSIVE_PROVIDER, 0, 0, this);
        mlocManager.addGpsStatusListener(this);
        meGpsState = (active) ? SmartGpsState.ACTIVE_GPS : SmartGpsState.PASSIVE_GPS;
    }

    BroadcastReceiver smartGpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String client = intent.getStringExtra(EXTRA_GPS_CLIENT);
            Log.d(TAG, "Received intent action: " + action + " from: " + client);
            requestGps(action.equals(ACTION_ACTIVATE_GPS), client);
        }
    };
}
