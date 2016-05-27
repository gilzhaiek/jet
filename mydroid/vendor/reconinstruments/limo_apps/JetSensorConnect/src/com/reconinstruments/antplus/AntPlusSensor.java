
package com.reconinstruments.antplus;

import android.util.SparseArray;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Ant Plus Device data object
 */
public class AntPlusSensor {
    protected int mProfile; // 0: heart rate
    protected int mDeviceNumber;
    protected String mDisplayName;
    protected boolean mConnected;
    protected int mMostRecent = 0; // 1: most recent
    protected static SparseArray<String> mDisplayNames = new SparseArray<String>();
    private boolean mAutoReconnect = true; // to indicate if auto reconnect in runtime

    public AntPlusSensor(JSONObject jsonObj) {
	try {
	    setProfile((Integer) jsonObj.get("profile"));
	    setDeviceNumber((Integer) jsonObj.get("deviceNumber"));
	    setDisplayName((String) jsonObj.get("displayName"));
	    setMostRecent((Integer) jsonObj.get("mostRecent"));
	}
	catch (JSONException e) {
	    e.printStackTrace();
	}
    }
    public AntPlusSensor(int profile, int deviceNumber, String displayName) {
        this.mProfile = profile;
        this.mDeviceNumber = deviceNumber;
        this.mDisplayName = displayName;
    }

    public int getProfile() {
        return mProfile;
    }

    public void setProfile(int profile) {
        this.mProfile = profile;
    }

    public int getDeviceNumber() {
        return mDeviceNumber;
    }

    public void setDeviceNumber(int deviceNumber) {
        this.mDeviceNumber = deviceNumber;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public void setConnected(boolean connected) {
        this.mConnected = connected;
    }
    public int getMostRecent() {
        return mMostRecent;
    }

    public void setMostRecent(int mostRecent) {
        this.mMostRecent = mostRecent;
    }

    public boolean isAutoReconnect() {
        return mAutoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.mAutoReconnect = autoReconnect;
    }

    // FIXME: put in child classe
    public String convertedDisplayName() {
        if (mDisplayNames.size() == 0) { // initialize the array
            mDisplayNames.append(HRManager.EXTRA_HEART_RATE_PROFILE, "HR Monitor");
            mDisplayNames.append(BCManager.EXTRA_BIKE_CADENCE_PROFILE, "Cadence");
            mDisplayNames.append(BSManager.EXTRA_BIKE_SPEED_PROFILE, "Speed");
            mDisplayNames.append(BSCManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE, "Speed/Cad");
            mDisplayNames.append(BWManager.EXTRA_BIKE_POWER_PROFILE, "Power");
        }
        return mDisplayNames.get(mProfile);
    }

    public JSONObject saveToJSON() {
        JSONObject jsonObj = null;
    	try {
    	    jsonObj = new JSONObject();
    	    jsonObj.put("profile", mProfile);
    	    jsonObj.put("deviceNumber", mDeviceNumber);
    	    jsonObj.put("displayName", mDisplayName);
            jsonObj.put("mostRecent", mMostRecent);
    	} catch (JSONException ex) {
    	    ex.printStackTrace();
    	}
    	return jsonObj;
    }
}
