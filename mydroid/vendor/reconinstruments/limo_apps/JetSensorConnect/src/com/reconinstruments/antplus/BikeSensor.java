
package com.reconinstruments.antplus;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Ant Plus Device data object
 */
public class BikeSensor extends AntPlusSensor {
    protected int mCircumference = 2096; // bike speed capability device only which includes spd, spd/cad and pwoer
    protected boolean mSpeedEnabled = false; //to show speed sub item or not, power only
    protected boolean mCadenceEnabled = false; //to show cadence sub item or not, power only

    public BikeSensor(JSONObject jsonObj) {
	super(jsonObj);
	try {
	    setCircumference((Integer) jsonObj.get("circumference"));
	} catch (JSONException e) {
	}
    }
    public BikeSensor(int profile, int deviceNumber, String displayName) {
	super(profile,deviceNumber,displayName);
    }
    public boolean isCircumferenceEnabled() {//enabled by default
	return mCircumference > 0;
    }
    public int getCircumference() {
        return mCircumference;
    }
    public void setCircumference(int circumference) {
        this.mCircumference = circumference;
    }
    public boolean isSpeedEnabled() {
        return mSpeedEnabled;
    }
    public void setSpeedEnabled(boolean speedEnabled) {
        this.mSpeedEnabled = speedEnabled;
    }
    public boolean isCadenceEnabled() {
        return mCadenceEnabled;
    }
    public void setCadenceEnabled(boolean cadenceEnabled) {
        this.mCadenceEnabled = cadenceEnabled;
    }
    @Override
    public JSONObject saveToJSON() {
        JSONObject jsonObj = super.saveToJSON();
        if(jsonObj == null) return jsonObj;
        try {
            jsonObj.put("circumference", mCircumference);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return jsonObj;
    }
    
}
