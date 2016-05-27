package com.reconinstruments.os.analyticsagent;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
//import android.util.Slog;

public class AnalyticsAgent {
    // constants
    private final String TAG = this.getClass().getSimpleName();

    private final static boolean DEBUG = false;

    // members
    private static AnalyticsAgent mSingletonInstance = null;
    private static AnalyticsManager mAnalyticsManager = null;
    private String mAgentID;
    private static boolean mSaveAllEvents = false;        // used to bypass filters
    private static boolean mInitCompleted = false;
    private Context mContext;
    private static HashMap<String, AnalyticsEventFilter> mFilters = new HashMap<String, AnalyticsEventFilter>();

    // constructor/destroy
    public static synchronized AnalyticsAgent getInstance(String agentID, Context context) {
        try {
            if (mSingletonInstance == null) {
                mSingletonInstance = new AnalyticsAgent(agentID, context);
            }
            if (mInitCompleted) {  // if fully initialized
                return mSingletonInstance;
            } else {
                if (mSingletonInstance != null) {
                    mSingletonInstance = null;      // release memory
                }
                if (mAnalyticsManager != null) {
                    mAnalyticsManager.destroy();    // release memory
                }
                mSaveAllEvents = false;
                mFilters.clear(); // reset to release any partial filter definitions
                return null;
            }
        } catch (Exception e) {
            Log.e("AnalyticsAgent", "Error creating AnalyticsAgent");
            return null;
        }
    }

    private AnalyticsAgent(String agentID, Context context) {
        mContext = context;
        mAgentID = agentID;
        if (DEBUG) Log.d(TAG, "creating agent id: " + mAgentID);

        try {
            mAnalyticsManager = new AnalyticsManager(mAgentID, mHandler, mContext);   // throws exception on failure
        } catch (Exception e) {
            mAnalyticsManager = null;
            Log.e(TAG, "Failed to create Analytics Manager for agent " + mAgentID + ". " + e.getMessage());
        }

        if (mAnalyticsManager != null) {
            loadConfig();
        }
    }

    public void destroy() {
        if (mAnalyticsManager != null) {
            mAnalyticsManager.destroy();
        }
    }

    // define handler for Analytics Service callbacks
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message message) {

            switch (ServiceRequestCodes.values()[message.arg1]) {

                case SERVICE_SHUTDOWN: {
                    try {
                        if (mAnalyticsManager != null) {
                            mAnalyticsManager.handleServiceShutdown();  // synchronous flush of data to service
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error encountered in ActivityManager AnalyticsAgent on handleServiceShutdown. " + e.getMessage() + " May have lost some data.");
                        mAnalyticsManager = null;   //
                    }
                    break;
                }
                default:
                    Log.e(TAG, "Unexpected message type encountered in ActivityManager AnalyticsAgent handler. ");
                    break;
            }
        }
    };

    //methods
    private void loadConfig() {
        String configJSON = null;

        mFilters.clear();    // reset before load

        try {
            if (mAnalyticsManager != null) {
                configJSON = mAnalyticsManager.getConfiguration();        // get config JSON from analytics service
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to retrieve the JSON configuration for Analytics Agent," + mAgentID + ". All analytics for this component will be blocked.");
            mAnalyticsManager = null; // on error, disable manager, which blocks all event recording
            return;
        }

        if(configJSON != null) {
            if(!loadConfigFromJSON(configJSON)) {
                Log.e(TAG, "Invalid JSON configuration for Analytics Agent," + mAgentID + ". All analytics for this component will be blocked.");
                mAnalyticsManager = null; // on error, disable manager, which blocks all event recording
            }
        }
        else {  // something is wrong...
            Log.e(TAG, "Invalid JSON configuration for Analytics Agent," + mAgentID + ". All analytics for this component will be blocked.");
            mAnalyticsManager = null; // on error, disable manager, which blocks all event recording
        }
        mInitCompleted = true;
    }

    private boolean loadConfigFromJSON(String configJSON) {
        JSONObject jsonObj = null;
        JSONArray filtersArray = null;

        String level = "beginning";
        try {
            jsonObj = new JSONObject(configJSON);        // convert string to json object

            level = "creating JSON obj";
            filtersArray = jsonObj.getJSONArray("event_filters");

            level = "loading filter array";   // now looping through All filterItems
            for (int i = 0; i < filtersArray.length(); i++) {

                JSONObject filterItem = filtersArray.getJSONObject(i);     // get filter item, componentID, events and possibly other parameters like queue_size

                level = "loading filter items";
                String componentType = null;
                String componentID = null;
                try {
                    componentType = filterItem.getString("component_type");
                    componentID = filterItem.getString("component_id");
                } catch (JSONException e) {
                    Log.e(TAG, "The Analytics Agent JSON configuration has a corrupt of missing component_id definition. Please repair the config JSON.");
                    continue;    // skip to next entry
                }

                if (componentType.equalsIgnoreCase("external") && componentID.equalsIgnoreCase(mAgentID)) {            // only add filters defined for this agent's client

                    // first look for queue_size
                    boolean numberError = false;
                    try {
                        int qSize = Integer.valueOf(filterItem.getString("queue_size"));
                        if (qSize > 0) {
                            if (mAnalyticsManager != null) {
                                mAnalyticsManager.setQSize(qSize);
                            }
                        } else {
                            numberError = true;
                        }
                    } catch (NumberFormatException e) {    // if non-existant  or error, ignore, leave default queue size
                        numberError = true;
                    } catch (JSONException e) {
                    }
                    if (numberError) {
                        Log.e(TAG, "The Analytics Agent JSON configuration for " + componentID + " has corrupt or missing queue_size definition. queue_size must be a positive number. Please repair the config JSON.");
                    }


                    // then process events to capture
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
                        Log.e(TAG, "The Analytics Agent JSON configuration for " + componentID + " has corrupt of missing 'events' definition. Please repair the config JSON.");
                    }
                } // end if
            } // end for i
        } // end try
        catch (JSONException e) {
            Log.e(TAG, "Unable to read JSON configuration for Analytics Agent, " + mAgentID + " - bad JSON. Have completed " + level + ", ConfigJSON=" + configJSON);
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
    public void RecordEvent(String type, String str1, String str2, String str3) {
        try {
            if (mAnalyticsManager != null && mInitCompleted && mFilters != null) {    // ignore all events if not ready...

                if(str2 == "SnapshotSystemFiles" && !str3.isEmpty()) {
                    mAnalyticsManager.diagnosticSnapshotOfSystemFiles(str3); // get Analytics system to take a snapshot of system files for diagnostics, use str2 as linking key
                }

                AnalyticsEventFilter filter = mFilters.get(type);    // get filter for event type,

                if (mSaveAllEvents || (filter != null && filter.passesConditions())) {    // if it exists and conditions are passed...
                    mAnalyticsManager.sendEventToService(new AnalyticsEventRecord(Long.toString(System.currentTimeMillis()), mAgentID, type, str1, str2, str3).mJSONString);    // add json string for new event record
                }
            }
        } catch (Exception e) {
            // log broken
            Log.e(TAG, "Unable to record analytics event " + type + " for Analytics Agent " + mAgentID);
        }
    }


}
