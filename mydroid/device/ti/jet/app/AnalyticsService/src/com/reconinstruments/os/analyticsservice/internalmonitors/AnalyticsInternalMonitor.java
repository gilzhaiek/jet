package com.reconinstruments.os.analyticsservice.internalmonitors;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.reconinstruments.os.analyticsagent.AnalyticsEventFilter;
import com.reconinstruments.os.analyticsagent.AnalyticsEventRecord;
import com.reconinstruments.os.analyticsservice.AnalyticsServiceApp;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AnalyticsInternalMonitor {
    // constants
    private final String TAG = this.getClass().getSimpleName();
    protected final static boolean DEBUG = false;

    // members
    public String mMonitorID;
    protected boolean mSaveAllEvents = false;        // used to bypass filters
    protected boolean mInitCompleted = false;
    protected AnalyticsServiceApp mParentService;

    private HashMap<String, AnalyticsEventFilter> mFilters = new HashMap<String, AnalyticsEventFilter>();

    // constructor/destroy
    public AnalyticsInternalMonitor(String monitorID, AnalyticsServiceApp parentObj, String configJSON) {
        mMonitorID = monitorID;
        if (DEBUG) {
            Log.i(TAG, "creating internal monitor id: " + mMonitorID);
        }
        mParentService = parentObj;

        if (LoadConfigFromJSON(configJSON)) {
            mInitCompleted = true;
        }
    }

    public void onDestroy() {
    }

    protected boolean LoadConfigFromJSON(String configJSON) {
        JSONObject jsonObj = null;
        JSONArray filtersArray = null;

        try {
            jsonObj = new JSONObject(configJSON);        // convert string to json object

            //process filters
            filtersArray = jsonObj.getJSONArray("event_filters");

            // looping through All filterItems
            for (int i = 0; i < filtersArray.length(); i++) {

                JSONObject filterItem = filtersArray.getJSONObject(i);     // get filter item, componentID, events and possibly other parameters like queue_size

                String componentType = null;
                String componentID = null;
                try {
                    componentType = filterItem.getString("component_type");
                    componentID = filterItem.getString("component_id");
                } catch (JSONException e) {
                    Log.e(TAG, "The Analytics JSON configuration has a corrupt of missing component_id definition. Please repair the config JSON.");
                    continue;    // skip to next entry
                }

                if (componentType.equalsIgnoreCase("internal") && componentID.equalsIgnoreCase(mMonitorID)) {            // only add filters defined for this agent's client

                    boolean numberError = false;

                    // process events to capture
                    try {
                        JSONArray eventArray = filterItem.getJSONArray("events");
                        for (int j = 0; j < eventArray.length(); j++) {
                            JSONObject eventItem = eventArray.getJSONObject(j);

                            // get event_type - mandatory, ignore on failure
                            String event_type = eventItem.getString("event_type");

                            int timeBetween = 0;
                            int forT = 0;
                            int forN = 0;

                            // get limit_time_between_event_cycles - optional
                            numberError = false;
                            try {
                                timeBetween = Integer.valueOf(eventItem.getString("limit_time_between_event_cycles"));
                                if (timeBetween < 0) {
                                    numberError = true;
                                    timeBetween = 0;
                                }

                            } catch (NumberFormatException e) {
                                timeBetween = 0;
                                numberError = true;
                            } catch (JSONException e) {
                                timeBetween = 0;
                            }
                            if (numberError) {
                                Log.e(TAG, "The Analytics Agent JSON configuration for " + componentID + " and event_type " + event_type + " has corrupt of missing limit_time_between_event_cycles definition. limit_time_between_event_cycles must be a positive number. Please repair the config JSON.");
                            }

                            // get limit_cycle_time - optional
                            numberError = false;
                            try {
                                forT = Integer.valueOf(eventItem.getString("limit_cycle_time"));
                                if (forT < 0) {
                                    numberError = true;
                                    forT = 0;
                                }
                            } catch (NumberFormatException e) {
                                forT = 0;
                                numberError = true;
                            } catch (JSONException e) {
                                forT = 0;
                            }
                            if (numberError) {
                                Log.e(TAG, "The Analytics Agent JSON configuration for " + componentID + " and event_type " + event_type + " has corrupt of missing limit_cycle_time definition. limit_cycle_time must be a positive number. Please repair the config JSON.");
                            }

                            // get limit_number_event_in_cycle - optional
                            numberError = false;
                            try {
                                forN = Integer.valueOf(eventItem.getString("limit_number_event_in_cycle"));
                                if (forN < 0) {
                                    numberError = true;
                                    forN = 0;
                                }
                            } catch (NumberFormatException e) {
                                forN = 0;
                                numberError = true;
                            } catch (JSONException e) {
                                forN = 0;
                            }
                            if (numberError) {
                                Log.e(TAG, "The Analytics Agent JSON configuration for " + componentID + " and event_type " + event_type + " has corrupt of missing limit_number_event_in_cycle definition. limit_number_event_in_cycle must be a positive number. Please repair the config JSON.");
                            }


                            // build filter and add it to list of Agents filters
                            AnalyticsEventFilter newFilter = new AnalyticsEventFilter(event_type, timeBetween, forT, forN);
                            if (DEBUG) Log.d(TAG, "New filter for event type: " + event_type);
                            mFilters.put(event_type, newFilter);

                            if (event_type.equalsIgnoreCase("ALL")) {
                                mSaveAllEvents = true;        // any occurance of ALL event type turns on filter override
                            }

                        } // end for j
                    } // end try
                    catch (JSONException e) {
                        Log.e(TAG, "The Analytics JSON configuration for " + componentID + " has corrupt of missing 'events' definition. Please repair the config JSON.");
                    }
                } // end if
            } // end for i
        } // end try
        catch (JSONException e) {
            Log.e(TAG, "Unable to read JSON configuration for internal monitor," + mMonitorID + " - bad JSON ");
            mFilters.clear();  // on fail, reset mFilters so no events are processed
            return false;
        }
        return true;

    }


    /**
     * @param type = event type, custom string per event type
     * @param str1 = data str 1 - can be anything the event creator desires to capture relevant data
     * @param str2 = data str 2 - can be anything the event creator desires to capture relevant data
     * @param str3 = data str 3 - can be anything the event creator desires to capture relevant data
     */
    public void recordEvent(String type, String str1, String str2, String str3) {
        if (mInitCompleted) {    // ignore all events if not ready...
            AnalyticsEventFilter filter = mFilters.get(type);    // get filter for event type,

            if (mSaveAllEvents || (filter != null && filter.passesConditions())) {    // if it exists and conditions are passed...
                mParentService.addEventToQueue("1:" + (new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), mMonitorID, type, str1, str2, str3).mJSONString));
            }
        }
    }

}
