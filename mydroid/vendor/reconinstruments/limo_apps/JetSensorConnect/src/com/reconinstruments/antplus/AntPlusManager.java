
package com.reconinstruments.antplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusBikeSpdCadCommonPcc.BikeSpdCadAsyncScanResultDeviceInfo;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusBikeSpdCadCommonPcc.IBikeSpdCadAsyncScanResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.AsyncScanResultDeviceInfo;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.IAsyncScanResultReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <code>AntPlusManager</code> is in charge of connecting/disconnecting with
 * sensors, it maintances the remember list as well.
 */
public class AntPlusManager {

    private static final String TAG = AntPlusManager.class.getSimpleName();

    private static AntPlusManager instance = null;

    protected AntPlusManager(Context context) {
        mContext = context;
        loadRememberedDevice();
    }

    public static AntPlusManager getInstance(Context context) {
        if (instance == null) {
            instance = new AntPlusManager(context);
        }
        return instance;
    }

    private AsyncScanController<AntPlusHeartRatePcc> hrScanCtrl = null;
    private AsyncScanController<AntPlusBikeCadencePcc> bcScanCtrl = null;
    private AsyncScanController<AntPlusBikeSpeedDistancePcc> bsdScanCtrl = null;
    private AsyncScanController<AntPlusBikePowerPcc> bpScanCtrl = null;

    private HashMap<Integer, AsyncScanController.AsyncScanResultDeviceInfo> mAlreadyConnectedDevicesMap = new HashMap<Integer, AsyncScanController.AsyncScanResultDeviceInfo>();
    private HashMap<Integer, AsyncScanController.AsyncScanResultDeviceInfo> mScannedDevicesMap = new HashMap<Integer, AsyncScanController.AsyncScanResultDeviceInfo>();

    private Context mContext = null;

    private static ArrayList<AntPlusSensor> mRememeberSensors = new ArrayList<AntPlusSensor>();

    /**
     * Ensures the controller is closed
     */
    public void closeScanController() {
        if (hrScanCtrl != null) {
            hrScanCtrl.closeScanController();
            hrScanCtrl = null;
        }
        if (bcScanCtrl != null) {
            bcScanCtrl.closeScanController();
            bcScanCtrl = null;
        }
        if (bsdScanCtrl != null) {
            bsdScanCtrl.closeScanController();
            bsdScanCtrl = null;
        }
        if (bpScanCtrl != null) {
            bpScanCtrl.closeScanController();
            bpScanCtrl = null;
        }
    }

    /**
     * Requests the asynchronous scan controller
     */
    public void requestScanning(int profile) {
        // reset the list for new scanning
        mAlreadyConnectedDevicesMap.clear();
        mScannedDevicesMap.clear();

        switch(profile){
            case HRManager.EXTRA_HEART_RATE_PROFILE:
                requestHeartRateScanning();
                break;
            case BCManager.EXTRA_BIKE_CADENCE_PROFILE:
                requestBikeCadenceScanning(false);
                break;
            case BSManager.EXTRA_BIKE_SPEED_PROFILE:
                requestBikeSpeedScanning(false);
                break;
            case BSCManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE:
                requestBikeSpeedScanning(true);
                break;
            case BWManager.EXTRA_BIKE_POWER_PROFILE:
                requestBikePowerScanning();
                break;
            default:
                requestHeartRateScanning();
                break;
        }
    }
    
    /**
     * Requests the asynchronous scan controller
     */
    public void requestHeartRateScanning() {

        hrScanCtrl = AntPlusHeartRatePcc.requestAsyncScanController(mContext, 0,
                new IAsyncScanResultReceiver() {
                    @Override
                    public void onSearchStopped(RequestAccessResult reasonStopped) {
                        Log.i(TAG, "[HeartRate] onSearchStopped");
                        // The triggers calling this function use the same codes
                        // and require the same actions as those received by the
                        // standard access result receiver
                        // base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
                    }

                    @Override
                    public void onSearchResult(final AsyncScanResultDeviceInfo deviceFound) {
                        if (mScannedDevicesMap.get(deviceFound.getAntDeviceNumber()) != null) {
                            return;
                        }

                        // We split up devices already connected to the plugin
                        // from un-connected devices to make this information
                        // more visible to the user,
                        // since the user most likely wants to be aware of which
                        // device they are already using in another app
                        if (deviceFound.isAlreadyConnected()) {
                            Log.i(TAG, "[HeartRate] add to connected list: " + deviceFound.getDeviceDisplayName());
                            mAlreadyConnectedDevicesMap.put(deviceFound.getAntDeviceNumber(), deviceFound);
                        } else {
                            Log.i(TAG, "[HeartRate] add to scanned list: " + deviceFound.getDeviceDisplayName());
                            mScannedDevicesMap.put(deviceFound.getAntDeviceNumber(), deviceFound);
                        }
                    }
                });
    }

    /**
     * Requests the asynchronous scan controller
     */
    public void requestBikeCadenceScanning(final boolean isCombined) {

        bcScanCtrl = AntPlusBikeCadencePcc.requestAsyncScanController(mContext, 0,
                new IBikeSpdCadAsyncScanResultReceiver() {
                    @Override
                    public void onSearchStopped(RequestAccessResult reasonStopped) {
                        Log.i(TAG, "[BikeCadence] onSearchStopped");
                        // The triggers calling this function use the same codes
                        // and require the same actions as those received by the
                        // standard access result receiver
                        // base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
                    }

                    @Override
                    public void onSearchResult(final BikeSpdCadAsyncScanResultDeviceInfo deviceFound) {
                        if(deviceFound.isSpdAndCadComboSensor != isCombined) return;
                        if (mScannedDevicesMap.get(deviceFound.resultInfo.getAntDeviceNumber()) != null) {
                            return;
                        }

                        // We split up devices already connected to the plugin
                        // from un-connected devices to make this information
                        // more visible to the user,
                        // since the user most likely wants to be aware of which
                        // device they are already using in another app
                        String indicator = "[BikeCadence]";
                        if(deviceFound.isSpdAndCadComboSensor){
                            indicator = "[SpdAndCadComboSensor]";
                        }
                        if (deviceFound.resultInfo.isAlreadyConnected()) {
                            Log.i(TAG, indicator + " add to connected list: " + deviceFound.resultInfo.getDeviceDisplayName() + ", isSpdAndCadComboSensor: " + deviceFound.isSpdAndCadComboSensor);
                            mAlreadyConnectedDevicesMap.put(deviceFound.resultInfo.getAntDeviceNumber(), deviceFound.resultInfo);
                        } else {
                            Log.i(TAG, indicator + " add to scanned list: " + deviceFound.resultInfo.getDeviceDisplayName() + ", isSpdAndCadComboSensor: " + deviceFound.isSpdAndCadComboSensor);
                            mScannedDevicesMap.put(deviceFound.resultInfo.getAntDeviceNumber(), deviceFound.resultInfo);
                        }
                    }

                });
    }
    
    /**
     * Requests the asynchronous scan controller
     */
    public void requestBikeSpeedScanning(final boolean isCombined) {
        bsdScanCtrl = AntPlusBikeSpeedDistancePcc.requestAsyncScanController(mContext, 0,
                new IBikeSpdCadAsyncScanResultReceiver() {
                    @Override
                    public void onSearchStopped(RequestAccessResult reasonStopped) {
                        Log.i(TAG, "[BikeSpeed] onSearchStopped");
                        // The triggers calling this function use the same codes
                        // and require the same actions as those received by the
                        // standard access result receiver
                        // base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
                    }

                    @Override
                    public void onSearchResult(final BikeSpdCadAsyncScanResultDeviceInfo deviceFound) {
                        if(deviceFound.isSpdAndCadComboSensor != isCombined) return;
                        if (mScannedDevicesMap.get(deviceFound.resultInfo.getAntDeviceNumber()) != null) {
                            return;
                        }

                        // We split up devices already connected to the plugin
                        // from un-connected devices to make this information
                        // more visible to the user,
                        // since the user most likely wants to be aware of which
                        // device they are already using in another app
                        String indicator = "[BikeSpeed]";
                        if(deviceFound.isSpdAndCadComboSensor){
                            indicator = "[SpdAndCadComboSensor]";
                        }
                        if (deviceFound.resultInfo.isAlreadyConnected()) {
                            Log.i(TAG, indicator + " add to connected list: " + deviceFound.resultInfo.getDeviceDisplayName() + ", isSpdAndCadComboSensor: " + deviceFound.isSpdAndCadComboSensor);
                            mAlreadyConnectedDevicesMap.put(deviceFound.resultInfo.getAntDeviceNumber(), deviceFound.resultInfo);
                        } else {
                            Log.i(TAG, indicator + " add to scanned list: " + deviceFound.resultInfo.getDeviceDisplayName() + ", isSpdAndCadComboSensor: " + deviceFound.isSpdAndCadComboSensor);
                            mScannedDevicesMap.put(deviceFound.resultInfo.getAntDeviceNumber(), deviceFound.resultInfo);
                        }
                    }

                });
    }
    
    /**
     * Requests the asynchronous scan controller
     */
    public void requestBikePowerScanning() {

        bpScanCtrl = AntPlusBikePowerPcc.requestAsyncScanController(mContext, 0,
                new IAsyncScanResultReceiver() {
                    @Override
                    public void onSearchStopped(RequestAccessResult reasonStopped) {
                        Log.i(TAG, "[BikePower] onSearchStopped");
                        // The triggers calling this function use the same codes
                        // and require the same actions as those received by the
                        // standard access result receiver
                        // base_IPluginAccessResultReceiver.onResultReceived(null, reasonStopped, DeviceState.DEAD);
                    }

                    @Override
                    public void onSearchResult(final AsyncScanResultDeviceInfo deviceFound) {
                        if (mScannedDevicesMap.get(deviceFound.getAntDeviceNumber()) != null) {
                            return;
                        }

                        // We split up devices already connected to the plugin
                        // from un-connected devices to make this information
                        // more visible to the user,
                        // since the user most likely wants to be aware of which
                        // device they are already using in another app
                        if (deviceFound.isAlreadyConnected()) {
                            Log.i(TAG, "[BikePower] add to connected list: " + deviceFound.getDeviceDisplayName());
                            mAlreadyConnectedDevicesMap.put(deviceFound.getAntDeviceNumber(), deviceFound);
                        } else {
                            Log.i(TAG, "[BikePower] add to scanned list: " + deviceFound.getDeviceDisplayName());
                            mScannedDevicesMap.put(deviceFound.getAntDeviceNumber(), deviceFound);
                        }
                    }
                });
    }
    
    public ArrayList<AsyncScanController.AsyncScanResultDeviceInfo> getScannedDevices() {
        return new ArrayList<AsyncScanController.AsyncScanResultDeviceInfo>(mScannedDevicesMap.values());
    }

    public List<AntPlusSensor> getConnectedDevices(int profile) {
        List<AntPlusSensor> devices = new ArrayList<AntPlusSensor>();
        for (AntPlusSensor i : mRememeberSensors) {
            if (i.getProfile() == profile && isConnected(i)){
                devices.add(i);
            }
        }
        return devices;
    }

    public AntPlusSensor getDevice(int profile, int deviceNumber) {
        for (AntPlusSensor i : mRememeberSensors) {
            if (i.getDeviceNumber() == deviceNumber && i.getProfile() == profile)
                return i;
        }
        return null;
    }

    public boolean isRemember(int profile, int deviceNumber) {
        for (AntPlusSensor sensor : mRememeberSensors) {
            if (sensor.getDeviceNumber() == deviceNumber && sensor.getProfile() == profile)
                return true;
        }
        return false;
    }

    //If there is no most recent device, the last added device will be most recent device
    public AntPlusSensor getMostRecent(int profile){
        AntPlusSensor mostRecent = null;
        AntPlusSensor altMostRecent = null;
        for (AntPlusSensor sensor : mRememeberSensors) {
            if(sensor.getProfile() == profile){
                altMostRecent = sensor;
                if (sensor.getMostRecent() == 1){
                    mostRecent = sensor;
                }
            }
        }
        if(mostRecent == null) mostRecent = altMostRecent;
        return mostRecent;
    }
    
    public void setMostRecent(int profile, int deviceNumber) {
        for (AntPlusSensor sensor : mRememeberSensors) {
            if(sensor.getProfile() == profile){
                if (sensor.getDeviceNumber() != deviceNumber){
                    sensor.setMostRecent(0);
                }else{
                    sensor.setMostRecent(1);
                }
            }
        }
        persistRememberedDevice();
    }
    
    public void addToRememberList(int profile, AsyncScanResultDeviceInfo device) {
        //Ensure there is only one device of EACH type in the list before add a new one
        AntPlusSensor mostRecent = getMostRecent(profile);
        if(mostRecent != null){
            removeFromRememberList(mostRecent);
        }
        
        if (!isRemember(profile, device.getAntDeviceNumber())) {
            AntPlusSensor sensor = null;
            if(profile == AntPlusProfileManager.EXTRA_HEART_RATE_PROFILE){
                sensor = new AntPlusSensor(profile, device.getAntDeviceNumber(), device.getDeviceDisplayName());
            }else if(profile == AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE){
                sensor = new PowerSensor(profile, device.getAntDeviceNumber(), device.getDeviceDisplayName());
            }else{
                sensor = new BikeSensor(profile, device.getAntDeviceNumber(), device.getDeviceDisplayName());
            }
            mRememeberSensors.add(sensor);
            persistRememberedDevice();
        }
    }

    public void removeFromRememberList(AntPlusSensor device) {
        if (isRemember(device.getProfile(), device.getDeviceNumber())) {
            for (AntPlusSensor s : mRememeberSensors) {
                if (s.getDeviceNumber() == device.getDeviceNumber() && s.getProfile() == device.getProfile()) {
                    mRememeberSensors.remove(s);
                    break;
                }
            }
            persistRememberedDevice();
        }
    }

    public void updateConnectionState(AntPlusSensor device, boolean connected) {
        if(device == null) return;
        for (AntPlusSensor sensor : mRememeberSensors) {
            if (sensor.getDeviceNumber() == device.getDeviceNumber() && sensor.getProfile() == device.getProfile()) {
                sensor.setConnected(connected);
            }
        }
    }

    public boolean isConnected(AntPlusSensor device) {
        for (AntPlusSensor sensor : mRememeberSensors) {
            if (sensor.getDeviceNumber() == device.getDeviceNumber() && sensor.getProfile() == device.getProfile()) {
                return sensor.isConnected();
            }
        }
        return false;
    }

    public boolean hasSensors() {
        if (mAlreadyConnectedDevicesMap.size() == 0 && mScannedDevicesMap.size() == 0) {
            return false;
        }
        return true;
    }

    public ArrayList<AntPlusSensor> getRememberedDevice() {
        return mRememeberSensors;
    }

    //get the devices with priority level, the higher level device
    //will overwrite the lower level device for instance, when the
    //speed only device is added and on, the other devices should
    //disable their speed capability
    public List<AntPlusSensor> getDevicesWithPriority() {
        //if there is no power connected, skip.
        List<AntPlusSensor> powerDevices = getConnectedDevices(AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE);
        if(powerDevices.size() == 0) return mRememeberSensors;
        
        List<AntPlusSensor> priorityDevices = new ArrayList<AntPlusSensor>();
        priorityDevices.addAll(mRememeberSensors);
        
        PowerSensor powerDevice = (PowerSensor)powerDevices.get(0);
        
        //if power doesn't have speed and cadence capability, skip
        if(!powerDevice.hasCombinedCadence() && !powerDevice.hasCombinedSpeed()) return mRememeberSensors;
            
        if (powerDevice.hasCombinedSpeed()
                && (getConnectedDevices(AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE).size() > 0 || getConnectedDevices(
                        AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE).size() > 0)) {
            powerDevice.setSpeedEnabled(false);
        }else{
            powerDevice.setSpeedEnabled(true);
            if(getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE) != null){
                priorityDevices.remove(getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE)); // remove speed only device if it's disconnect
            }
        }
        if (powerDevice.hasCombinedCadence()
                && (getConnectedDevices(AntPlusProfileManager.EXTRA_BIKE_CADENCE_PROFILE).size() > 0 || getConnectedDevices(
                        AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE).size() > 0)) {
            powerDevice.setCadenceEnabled(false);
        }else{
            powerDevice.setCadenceEnabled(true);
            if(getMostRecent(AntPlusProfileManager.EXTRA_BIKE_CADENCE_PROFILE) != null){
                priorityDevices.remove(getMostRecent(AntPlusProfileManager.EXTRA_BIKE_CADENCE_PROFILE)); // remove cadence only device if it's disconnect
            }
        }
        
        //remove spd/cad if it's disconnected and the power with two capability is connected
        if(powerDevice.isSpeedEnabled() && powerDevice.isCadenceEnabled() && (getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE) != null)){
            priorityDevices.remove(getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE)); 
        }
        
        return priorityDevices;
    }

    /**
     * load remmebered devices from SharedPreferences
     */
    public void loadRememberedDevice() {
        mRememeberSensors.clear();
        SharedPreferences sharedpreferences = mContext.getSharedPreferences("ant_plus_manager", Context.MODE_PRIVATE);
        String remeberedDevices = sharedpreferences.getString("ant_plus_device", "");
        if (!"".equals(remeberedDevices)) {
            try {
                JSONArray jsonArray = new JSONArray(remeberedDevices);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    int profile = (Integer) jsonObj.get("profile");
		    AntPlusSensor sensor;
                    if(profile == AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE){
                        sensor =  new PowerSensor(jsonObj);
                    }
		    else if(profile == AntPlusProfileManager.EXTRA_HEART_RATE_PROFILE){
			sensor =  new AntPlusSensor(jsonObj);
		    }
		    else{
                        sensor =  new BikeSensor(jsonObj);
                    }
		    mRememeberSensors.add(sensor);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * persist remembered devices
     */
    public void persistRememberedDevice() {
        JSONArray jsonArray = new JSONArray();
        for (AntPlusSensor sensor : mRememeberSensors) {
            JSONObject jsonObj = sensor.saveToJSON();
            if(jsonObj != null)
                jsonArray.put(jsonObj);
        }

        // persist it to SharedPreferences
        SharedPreferences sharedpreferences = mContext.getSharedPreferences("ant_plus_manager", Context.MODE_PRIVATE);
        Editor editor = sharedpreferences.edit();
        if (jsonArray.length() > 0) {
            editor.putString("ant_plus_device", jsonArray.toString());
        }else{
            editor.putString("ant_plus_device", null);
        }
        editor.commit();
    }
}
