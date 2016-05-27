
package com.reconinstruments.antplus;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.DataSource;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Ant Plus Device data object
 */
public class PowerSensor extends BikeSensor {
        protected int mPowerSource = BWManager.INVALID_POWER_SOURCE; // for bike power sensor to indicate the type of power, power only

    public PowerSensor(JSONObject jsonObj) {
	super(jsonObj);
	try {
	setPowerSource((Integer) jsonObj.get("powerSource"));
	} catch (JSONException e){
	}
    }
    public PowerSensor(int profile, int deviceNumber, String displayName) {
	super(profile,deviceNumber,displayName);
    }
    public int getPowerSource() {
        return mPowerSource;
    }

    public void setPowerSource(int powerSource) {
        this.mPowerSource = powerSource;
    }
    // all power sensor can calibrate
    public boolean canCalibrate() {
        return (mPowerSource != BWManager.INVALID_POWER_SOURCE || isCTF());
    }

    // WHEEL_TORQUE and CRANK_TORQUE has offset and torque, others don't have
    // them.
    public boolean hasOffsetAndTorque() {
	return ! (mPowerSource == DataSource.POWER_ONLY_DATA.ordinal()
		  ||mPowerSource == DataSource.CTF_DATA.ordinal()) ;
    }

    // all power sensor has cadence
    public boolean hasCombinedCadence() {
        return mPowerSource != BWManager.INVALID_POWER_SOURCE;
    }

    // only WHEEL_TORQUE has speed
    public boolean hasCombinedSpeed() {
        return (mPowerSource == DataSource.WHEEL_TORQUE_DATA.ordinal());
    }

    public boolean isCTF(){
        return (mPowerSource == DataSource.CTF_DATA.ordinal() || mPowerSource == DataSource.INVALID_CTF_CAL_REQ.ordinal());
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

    public String convertedPowerSpeedName() {
        return "Speed (PWR)";
    }

    public String convertedPowerCadenceName() {
        return "Cadence (PWR)";
    }
    @Override
    public JSONObject saveToJSON() {
        JSONObject jsonObj = super.saveToJSON();
        if(jsonObj == null) return jsonObj;
        try {
            jsonObj.put("powerSource", mPowerSource);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return jsonObj;
    }
    
}
