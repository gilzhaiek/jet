package com.reconinstruments.os.analyticsservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

import android.content.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// ANALYTICS
import com.reconinstruments.os.analyticsagent.ServiceRequestCodes;
import com.reconinstruments.os.analyticsagent.IAnalyticsService;
import com.reconinstruments.os.analyticsagent.AnalyticsEventQueue;
import com.reconinstruments.os.analyticsagent.AnalyticsEventRecord;
import com.reconinstruments.os.analyticsservice.internalmonitors.AnalyticsInternalMonitor;
import com.reconinstruments.os.analyticsservice.internalmonitors.BatteryMonitor;
import com.reconinstruments.os.hardware.power.HUDPowerManager;

import android.app.Application;
import android.os.ServiceManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.os.SystemProperties;

public class AnalyticsServiceApp extends Application {
    private final String TAG = this.getClass().getSimpleName();

    private static final String REMOTE_SERVICE_NAME = IAnalyticsService.class.getName();
    public static final String PHONE_INTERNET_STATE = "com.reconinstruments.hudservice.PHONE_INTERNET_STATE";
    public static final String ZIP_ENCRYPTION_KEY = "r3C0n@naLyt!(s";
    private static final boolean CLEAR_PREFERENCES = false;
    private static final boolean WAIT_FOR_DEBUGGER = false;
    private static final boolean DEBUG = false;
    private static final long DEFAULT_ZIP_FILE_STALE_TIME = 60 * 60 * 24;
    private static final long DEFAULT_MAX_ZIP_SIZE = 512000;
    private static final long CONTINUOUS_NET_CONNECTION_UPLOAD_INTERVAL_IN_MS = 3600000; // once per hour?
    private static final int HARVEST_DATA = 0;
    private static final long MAX_TIME_FOR_DATA_HARVEST_AT_SHUTDOWN = 5000;        // 5 seconds

    private final String ANALYTICS_SERVICE_PREFERENCES = "AnalyticsServicePrefs";
    private final String GetConfigDropBoxPath = Environment.getExternalStorageDirectory().getPath() + "/ReconApps/Analytics/config/";

    private String mConfigName = null;
    private String mAnalyticsEndPoint = null;    // for direct network communication
    private String mDiagnosticEndPoint = null;
    private String mGetConfigEndPoint = null;
    private String mAnalyticsDropBox = null;    // intermediate local dir for Uplink-based communication
    public  String mDiagnosticDropBox = null;
    private String mGetConfigDropBox = null;
    private SharedPreferences mAnalyticsServicePreferences = null;
    private boolean mNetworkAvailable = false;
    private boolean mBlockEventCapture = false;
    private IAnalyticsServiceImpl mIAnalyticsServiceImpl = null;
    protected String mSystemConfigurationJSON = "";
    private final Handler mHandler = new Handler();
    private long mStaleZipTime = DEFAULT_ZIP_FILE_STALE_TIME;    // one day default
    private long mZipFileSizeLimit = DEFAULT_MAX_ZIP_SIZE;
    private int mQueueSize = 1000;
    private boolean mPullAgentDataFinished = true;
    private boolean mResetStoredData = false;
    private boolean mShuttingDown = false;
    private boolean mUserSettingEnabled = true;
    private String mDeviceSVN = "not_set";
    private Handler mShutdownReasonHandler = new Handler();
    private int mGetShutdownReasonAttempt = 0;
    private BatteryMonitor mBatteryMon = null;

    HashMap<String, AnalyticsInternalMonitor> mInternalMonitors = new HashMap<String, AnalyticsInternalMonitor>();

    private AnalyticsServiceCloudInterface mCloudInterface;

    public void onCreate() {
        super.onCreate();
        this.mIAnalyticsServiceImpl = new IAnalyticsServiceImpl(this, mHandler);

        ServiceManager.addService(REMOTE_SERVICE_NAME, this.mIAnalyticsServiceImpl);
        if (DEBUG)
            Log.d(TAG, "Registered [" + mIAnalyticsServiceImpl.getClass().getName() + "] as [" + REMOTE_SERVICE_NAME + "] with ServiceManager");

        // register for broadcast connection events, shutdown,
        IntentFilter filter = new IntentFilter();
        filter.addAction(PHONE_INTERNET_STATE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mPhoneStatusChangedReceiver, filter);

        mAnalyticsServicePreferences = getSharedPreferences(ANALYTICS_SERVICE_PREFERENCES, Context.MODE_PRIVATE);

        File newConfigFile = new File(GetConfigDropBoxPath + "config.json");

        if (WAIT_FOR_DEBUGGER) {
            android.os.Debug.waitForDebugger();
        }

        // first confirm GetConfigDropBoxPath exists so Uplink can drop files there
        File folder = new File(GetConfigDropBoxPath);
        if (!folder.exists()) {
            if (DEBUG) Log.d(TAG, "Making config drop box: " + GetConfigDropBoxPath);
            folder.mkdirs();
        }

        // second load config
        if (DEBUG) Log.d(TAG, "AnalyticsService - loading config ");
        //      check for new config on fixed GetConfigDropBoxPath
        if (DEBUG) Log.d(TAG, "Looking for config file: " + newConfigFile + " exists=" + newConfigFile.exists());

        if (newConfigFile.exists()) {
            try {
                if (DEBUG) Log.d(TAG, "AnalyticsService - using new config file " + newConfigFile.getAbsolutePath());
                mSystemConfigurationJSON = loadConfigJSONFromFile(newConfigFile);
                if (loadConfigFromJSON(mSystemConfigurationJSON)) {
                    if (DEBUG) Log.d(TAG, "AnalyticsService - loaded config from new config file: " + mSystemConfigurationJSON);
                    saveConfigJSONInPreferences();    // if config loaded, it's valid so store mAgentConfigurationJSON in preferences
                }
                else {
                    Log.e(TAG, "Invalid config JSON in new analytics config file. Removing file and reloading config from stored preferences...");
                    mSystemConfigurationJSON = "";
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot load new analytics config file. Removing file and reloading config from stored preferences...");
                mSystemConfigurationJSON = "";
            }
            try {
                if (DEBUG) Log.d(TAG, "AnalyticsService - deleting new config file " + newConfigFile.getAbsolutePath());
                newConfigFile.delete();
            } catch (Exception e) {
                Log.e(TAG, "Cannot remove new analytics config file from sd card");
            }
            mResetStoredData = true;
        }
        if(mSystemConfigurationJSON == "") { // if not loaded from new file, try loading config from preferences
            try {
                // get mAgentConfigurationJSON from saved values
                mSystemConfigurationJSON = mAnalyticsServicePreferences.getString("configJSON", "");
                if (!CLEAR_PREFERENCES) {
                    if (!mSystemConfigurationJSON.isEmpty()) {
                        if (!loadConfigFromJSON(mSystemConfigurationJSON)) {
                            Log.e(TAG, "Missing or invalid config JSON in stored preferences. Reloading default config file...");
                            mSystemConfigurationJSON = "";
                        }
                        else {
                            if (DEBUG) Log.d(TAG, "AnalyticsService - loaded config from system preferences: " + mSystemConfigurationJSON);
                        }
                    } else {
                        Log.e(TAG, "Empty config JSON in stored preferences. Reloading default config file...");
                        mSystemConfigurationJSON = "";
                    }
                }
                else {
                    Log.i(TAG, "Forced clear/reset of analytics service preferences. Reloading default config file...");
                    mSystemConfigurationJSON = "";
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading analytics config from stored preferences. Reloading default config file...");
                mSystemConfigurationJSON = "";
            }
        }
        if(mSystemConfigurationJSON == "") { // if config still not set, try loading config from built-in assets file
            mSystemConfigurationJSON = loadConfigJSONFromAssets();
            if (!loadConfigFromJSON(mSystemConfigurationJSON)) {
                Log.e(TAG, "Critical error with Analytics Service built-in config.json asset.  Terminating");
                // should not happen in production... so throw exception to indicate major issue with built in asset
                throw new RuntimeException("Critical error with Analytics Service built-in config.json asset.  Terminating");
            }
            else {
                if (DEBUG) Log.d(TAG, "AnalyticsService - loaded config from default file: " + mSystemConfigurationJSON);
                saveConfigJSONInPreferences();    // store mAgentConfigurationJSON in preferences
            }
        }

        //       confirm mAnalyticsDropBox exists so we can place files there for Uplink to grab
        folder = new File(mAnalyticsDropBox);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        //       confirm mDiagnosticDropBox exists so we can place files there for Uplink to grab
        folder = new File(mDiagnosticDropBox);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // check analytics enabled setting
        ContentResolver cr = this.getContentResolver();
        String analyticsEnabledSettingsKey = "com.reconinstruments.settings.ANALYTICS_ENABLED";
        try {
            mUserSettingEnabled = (Settings.System.getInt(cr, analyticsEnabledSettingsKey) == 1);
        } catch( Settings.SettingNotFoundException e ) {
            // this is the condition where the user has not touched the Analytics settings so there is not setting defined yet... in this case just used default (ANALYTICS_ENABLED);
        }
        if(DEBUG) {
            Log.d(TAG, "User settings: Analytics is " + (mUserSettingEnabled ? "On" : "Off"));
        }

        mDeviceSVN = SystemProperties.get("ro.build.svn.num", "");
        if(DEBUG) {
            Log.d(TAG, "Device SVN is " + mDeviceSVN );
        }

        // set up internal monitors
        mBatteryMon = new BatteryMonitor(this, mSystemConfigurationJSON);
        if(mBatteryMon != null) {
            mInternalMonitors.put(mBatteryMon.mMonitorID, mBatteryMon);
        }

        mShutdownReasonHandler.post(GetShutdownReasonTask);  // spawn timer to get last shutdown reason

        mCloudInterface = new AnalyticsServiceCloudInterface((Context) this, mAnalyticsServicePreferences, mStaleZipTime,
                mZipFileSizeLimit, mAnalyticsDropBox, mQueueSize, mResetStoredData, mConfigName, mDeviceSVN, ZIP_ENCRYPTION_KEY);

    }


    private Runnable GetShutdownReasonTask = new Runnable() {
        @Override
        public void run() {
            if(++mGetShutdownReasonAttempt > 10) {
                addEventToQueue(new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), "AnalyticsService", "LastShutdownReason", "NotAvailable", "", "").mJSONString);
            }
            else {
                HUDPowerManager hpm = null;
                try {
                    hpm = HUDPowerManager.getInstance();
                }
                catch (Exception e) {
                    hpm = null;
                    Log.i(TAG, "Unexpected failure of HUDPowerManager.getInstance(). Error: " + e.getMessage() + ".  Will try again in 1sec.");
                }

                if (hpm == null) {
                    mShutdownReasonHandler.postDelayed(this, 1000);        // try again in one second
                }
                else {
                    String shutdownReasonEventJSON = null;
                    if (hpm.getLastShutdownReason() == HUDPowerManager.SHUTDOWN_ABRUPT) {
                        shutdownReasonEventJSON = new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), "AnalyticsService", "LastShutdownReason", "AbruptShutdown", "", "").mJSONString;
                    }
                    else if (hpm.getLastShutdownReason() == HUDPowerManager.SHUTDOWN_BATT_REMOVED) {
                        shutdownReasonEventJSON = new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), "AnalyticsService", "LastShutdownReason", "BatteryShutdown", "", "").mJSONString;
                    }
                    else {
                        shutdownReasonEventJSON = new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), "AnalyticsService", "LastShutdownReason", "GracefulShutdown", "", "").mJSONString;
                    }
                    if (DEBUG) Log.d(TAG, "Shutdown JSON: " + shutdownReasonEventJSON);
                    addEventToQueue("1:"+shutdownReasonEventJSON);

                    mBatteryMon.setHUDPowerManager(hpm);
                }
            }
        }
    };


    public void onShutdown() {  // called from IAnalyticsServiceImpl.shutdown()
        mShuttingDown = true;
        if(mUserSettingEnabled) {
            unregisterReceiver(mPhoneStatusChangedReceiver);
            mNetworkAvailable = false;

            for (AnalyticsInternalMonitor monitor : mInternalMonitors.values()) {
                if (DEBUG) Log.d(TAG, "Shutting down internal monitor: " + monitor.mMonitorID);
                monitor.onDestroy();
            }
            if (mIAnalyticsServiceImpl.harvestAllAgentData() > 0) { // when done, calls onReceivedDataFromAllAgents()
                mHandler.postDelayed(harvestTimeoutTimer, MAX_TIME_FOR_DATA_HARVEST_AT_SHUTDOWN);    // starts time-out timer
            }
        }
        else {
            mIAnalyticsServiceImpl.endShutdown();
        }
    }

    Runnable harvestTimeoutTimer = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "Timed out waiting for data harvest from agents. Possible lost of Analytics data !");
            doPostHarvestActions();
        }
    };

    protected void onReceivedDataFromAllAgents() {
        mHandler.removeCallbacks(harvestTimeoutTimer);   // stop timeout timer
        if (DEBUG) Log.i(TAG, "shutdown data harvest complete ");
        doPostHarvestActions();
    }

    private void doPostHarvestActions() {
        mCloudInterface.flushQueueToZipOnShutdown();  // synchronous push stored data to local zip file
        mIAnalyticsServiceImpl.endShutdown();
    }

    private void saveConfigJSONInPreferences() {
        SharedPreferences.Editor editor = mAnalyticsServicePreferences.edit();
        editor.putString("configJSON", mSystemConfigurationJSON);
        editor.commit();
    }

    private BroadcastReceiver mPhoneStatusChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String provider = intent.getStringExtra("Provider");
            boolean newState = intent.getBooleanExtra("IsConnected", false);

            if (newState != mNetworkAvailable) {
                mNetworkAvailable = newState;
                Log.d(TAG, "mPhoneStatusChangedReceiver PHONE_STATE_CHANGED: " + mNetworkAvailable + " from provider: " + provider);
            }
        }
    };

    public void addEventToQueue(String jsonEventRecord) {
        if(mUserSettingEnabled) {
            mCloudInterface.storeData(jsonEventRecord);    // copy data to internal buffers, uploaded to cloud later under various conditions
        }
    }

    // define handler for Analytics Service callbacks

    public void onNewAgentData(String dataStr) {
        if (!mBlockEventCapture && mUserSettingEnabled) {
            mCloudInterface.storeData(dataStr);    // copy data to internal buffers, uploaded to cloud later under various conditions
        }
    }

    private String loadConfigJSONFromAssets() {
        AssetManager assetManager = getAssets();

        // To load text file
        InputStream input;
        try {
            input = assetManager.open("config.json");

            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();

            // byte buffer into a string
            return new String(buffer);

        } catch (IOException e) {
            Log.e(TAG, "Cannot read default config.json file.  Disabling analytics until config.json updated from the cloud");
            return "";
        }
    }

    private String loadConfigJSONFromFile(File filePath) {
        DataInputStream input;
        try {
            input = new DataInputStream(new FileInputStream(filePath));

            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();

            // byte buffer into a string
            return new String(buffer);

        } catch (IOException e) {
            Log.e(TAG, "Cannot read config.json file from " + filePath + ".  Disabling analytics until config.json updated from the cloud");
            return "";
        }
    }

    private boolean loadConfigFromJSON(String configJSON) {
        JSONObject jsonObj = null;
        JSONObject systemParms = null;
        JSONArray filtersArray = null;

        if(configJSON == "") {      // fail on empty JSON string
            Log.e(TAG, "Empty JSON configuration in Analytics Service ");
            return false;
        }
        try {
            jsonObj = new JSONObject(configJSON);        // convert string to json object
        } // end try
        catch (JSONException e) {
            Log.e(TAG, "Unable to read JSON configuration in Analytics Service - bad JSON : ");
            return false;
        }
        //process system parms
        try {
            mConfigName = jsonObj.getString("config_name");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error reading JSON configuration in Analytics Service - missing 'config_name' definition ");
            return false;
        }

        try {
            systemParms = jsonObj.getJSONObject("service_parms");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error reading JSON configuration in Analytics Service - missing 'service_parms' definition ");
            return false;
        }

        try {
            mAnalyticsEndPoint = systemParms.getString("analytics_end_point");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error reading JSON configuration in Analytics Service - missing 'analytics_end_point' definition ");
            return false;
        }

        try {
            mDiagnosticEndPoint = systemParms.getString("diagnostics_end_point");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error reading JSON configuration in Analytics Service - missing 'diagnostics_end_point' definition ");
            return false;
        }

        try {
            mAnalyticsDropBox = Environment.getExternalStorageDirectory().getPath() + systemParms.getString("analytics_dropbox_for_uplink");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error reading JSON configuration in Analytics Service - missing 'analytics_dropbox_for_uplink' definition ");
            return false;
        }

        try {
            mDiagnosticDropBox = Environment.getExternalStorageDirectory().getPath() + systemParms.getString("diagnostics_dropbox_for_uplink");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error reading JSON configuration in Analytics Service - missing 'diagnostics_dropbox_for_uplink' definition ");
            return false;
        }

        try {
            mGetConfigEndPoint = systemParms.getString("get_config_end_point");
        }
        catch (JSONException e) {
            Log.e(TAG, "Error reading JSON configuration in Analytics Service - missing 'get_config_end_point' definition ");
            return false;
        }

        // first look for queue_size
        boolean numberError = false;
        try {
            int qSize = Integer.valueOf(systemParms.getString("queue_size"));
            if (qSize > 0) {
                mQueueSize = qSize;
            } else {
                numberError = true;
            }
        } catch (NumberFormatException e) {    // if non-existant  or error, ignore, leave default queue size
            numberError = true;
        } catch (JSONException e) {
            Log.i(TAG, "The Analytics JSON configuration in the Analytics Service does not define queue_size. Default size will be used.");
        }
        if (numberError) {
            Log.e(TAG, "The Analytics JSON configuration in the Analytics Service has a corrupt or missing queue_size definition. queue_size must be a positive number. Please repair the config JSON.");
        }

        // first look for zip file stale time
        numberError = false;
        try {
            long staleTime = Long.parseLong(systemParms.getString("zip_file_secs_until_stale"));
            if (staleTime >= 0) {
                mStaleZipTime = staleTime;
            } else {
                numberError = true;
            }
        } catch (NumberFormatException e) {    // if non-existant  or error, ignore, leave default staleTime
            numberError = true;
        } catch (JSONException e) {
            Log.i(TAG, "The Analytics JSON configuration in the Analytics Service does not define zip_file_secs_until_stale. Default time period will be used.");
        }
        if (numberError) {
            Log.e(TAG, "The Analytics JSON configuration in the Analytics Service has a corrupt or missing zip_file_secs_until_stale definition. zip_file_secs_until_stale must be a positive number. Please repair the config JSON.");
        }

        // first look for zip file max size
        numberError = false;
        try {
            long maxFileSize = Long.parseLong(systemParms.getString("zip_file_max_size"));
            if (maxFileSize > 0) {
                mZipFileSizeLimit = maxFileSize;
            } else {
                numberError = true;
            }
        } catch (NumberFormatException e) {    // if non-existant  or error, ignore, leave default queue size
            numberError = true;
        } catch (JSONException e) {
            Log.i(TAG, "The Analytics JSON configuration in the Analytics Service does not define zip_file_max_size. Default max size will be used.");
        }
        if (numberError) {
            Log.e(TAG, "The Analytics JSON configuration in the Analytics Service has a corrupt or missing zip_file_max_size definition. zip_file_max_size must be a positive number. Please repair the config JSON.");
        }

        return true;

    }


}

