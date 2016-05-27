
package com.reconinstruments.antplus;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.AutoZeroStatus;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CalculatedWheelDistanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CalculatedWheelSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CalibrationMessage;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CrankLengthSetting;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.CrankParameters;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.DataSource;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IAutoZeroStatusReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedPowerReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedTorqueReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalibrationMessageReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICrankParametersReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IInstantaneousCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IMeasurementOutputDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IPedalPowerBalanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IPedalSmoothnessReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRawCrankTorqueDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRawCtfDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRawPowerOnlyDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRawWheelTorqueDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IRequestFinishedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ITorqueEffectivenessReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.BatteryStatus;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestStatus;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IBatteryStatusReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IManufacturerIdentificationReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IProductInformationReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;


import android.util.Log;
import android.util.SparseArray;
import android.os.Handler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import android.content.Intent;

/**
 * Describe class <code>BWManager</code> here. Class to hide the complexities of
 * dealing with Power events.
 */
public class BWManager extends AntPlusProfileManager {

    private static final String TAG = "BWManager";

    public static final String ACTION_BIKE_POWER = "com.reconinstruments.externalsensors.bikepower";
    public static final String ACTION_BIKE_POWER_CALIBRATION = "com.reconinstruments.externalsensors.power.calibration";
    public static final String EXTRA_BIKE_POWER_VALUE = "Power";
    public static final String EXTRA_BIKE_POWER_CALIBRATION_RESULT = "Result";
    public static final String EXTRA_BIKE_POWER_CALIBRATION_OFFSET = "ZeroOffset";
    public static final String EXTRA_BIKE_POWER_CALIBRATION_TORQUE = "Torque";
    
    public static final int INVALID_POWER = -1;
    
    private static boolean mIgnoreSpeed = false;
    private static boolean mIgnoreCadence = false;

    public static final int INVALID_POWER_SOURCE = -1000;
    public static final Vector<String> mSupportedSources = new Vector<String>();
    // NOTE: We're using 2.07m as the wheel circumference to pass to the calculated events
    //BigDecimal mWheelCircumferenceInMeters = new BigDecimal("2.095"); // 2.095m circumference = an average 700cx23mm road tire

    private float mCircumference = 2.095f; // 2.095m circumference = an average 700cx23mm road tire
    public void setCircumference(float circumference){
        mCircumference = circumference;
    }

    AntPlusBikePowerPcc bwPcc = null;

    private int mSource = INVALID_POWER_SOURCE; // one of value in mSupportedSources
    public BWManager(IAntContext owner) {
        super(owner);
        if(mSupportedSources.size() == 0){
            mSupportedSources.add(DataSource.POWER_ONLY_DATA.name()); //doesn't support calibration power, cadence, avg power
            mSupportedSources.add(DataSource.CTF_DATA.name()); //slope, cadence
            mSupportedSources.add(DataSource.CRANK_TORQUE_DATA.name()); //power, avg power, avg torque, avg cadence, cadence
            mSupportedSources.add(DataSource.WHEEL_TORQUE_DATA.name()); // avg cadence, avg whell speed, avg wheel distance, avg power, cadence, power
            mSupportedSources.add(DataSource.INVALID_CTF_CAL_REQ.name()); // this is the initial state for ctf device, after valid calibration, it would change to CTF_DATA
        }
    }

    @Override
    final protected String getBroadcastAction() {
        return ACTION_BIKE_POWER;
    }

    @Override
    final protected String getProfileNameKey() {
        return EXTRA_BIKE_POWER_VALUE;
    }

    @Override
    final protected int getProfileInt() {
        return EXTRA_BIKE_POWER_PROFILE;
    }

    @Override
    protected AntPluginPcc getPcc() {
        return bwPcc;
    }

    @Override
    protected void setPcc(AntPluginPcc pcc) {
        bwPcc = (AntPlusBikePowerPcc) pcc;
    }
    
    public static void ignoreSpeed(boolean ignoreSpeed) {
        mIgnoreSpeed = ignoreSpeed;
    }

    public static void ignoreCadence(boolean ignoreCadence) {
        mIgnoreCadence = ignoreCadence;
    }

    //broadcast power metrics
    private void broadcastBikePower(int value) {
        Intent intent = new Intent(BWManager.ACTION_BIKE_POWER);
        intent.putExtra(BWManager.EXTRA_BIKE_POWER_VALUE, value);
        mOwner.getContext().sendBroadcast(intent);
    }
    
    private void broadcastBikeSpeed(int bs) {
        if(!mIgnoreSpeed){
            Intent bsChangeIntent = new Intent(BSManager.ACTION_BIKE_SPEED);
            bsChangeIntent.putExtra(BSManager.EXTRA_BIKE_SPEED_VALUE, bs);
            mOwner.getContext().sendBroadcast(bsChangeIntent);
        }else{
//            Log.i(TAG, "using the single device speed value instead");
        }
    }

    private void broadcastBikeCadence(int bc) {
        if(!mIgnoreCadence){
            Intent bcChangeIntent = new Intent(BCManager.ACTION_BIKE_CADENCE);
            bcChangeIntent.putExtra(BCManager.EXTRA_BIKE_CADENCE_VALUE, bc);
            mOwner.getContext().sendBroadcast(bcChangeIntent);
        }else{
//            Log.i(TAG, "using the single device cadence value instead");
        }
    }
    
    private void broadcastBikePowerRequestResponse(boolean result, String zeroOffset, String torque) {
        Intent intent = new Intent(BWManager.ACTION_BIKE_POWER_CALIBRATION);
        intent.putExtra(EXTRA_BIKE_POWER_CALIBRATION_RESULT, result);
        if(zeroOffset != null) intent.putExtra(EXTRA_BIKE_POWER_CALIBRATION_OFFSET, zeroOffset);
        if(torque != null) intent.putExtra(EXTRA_BIKE_POWER_CALIBRATION_TORQUE, torque);
        mOwner.getContext().sendBroadcast(intent);
    }
    
    /**
     * request to connect
     */
    @Override
    public void requestAccessToPcc() {
        AntPlusBikePowerPcc.requestAccess(mOwner.getContext(),
                mAntDeviceId,
                0, // searchProximityThreshold
                base_IPluginAccessResultReceiver,
                base_IDeviceStateChangeReceiver);
    }

    /**
     * Release the PCC connection
     */
    @Override
    public void releaseAccess() {
        // Release the old access if it exists
        if (bwPcc != null) {
            bwPcc.releaseAccess();
            bwPcc = null;
            mSource = INVALID_POWER_SOURCE; //reset this value
        }
    }

    private int mFailCounter = 0;
    public static final int MAX_FAIL_COUNTER = 3;
    Handler mFailHandler = new Handler(); 
    final IRequestFinishedReceiver requestFinishedReceiver = new IRequestFinishedReceiver() {
        @Override
        public void onNewRequestFinished(final long estTimestamp,
                final EnumSet<EventFlag> eventFlags, final RequestStatus requestStatus) {
            switch (requestStatus) {
                case SUCCESS:
		    mFailCounter = 0;
		    mFailHandler.removeCallbacksAndMessages(null);
                    broadcastBikePowerRequestResponse(true, null, null);
                    Log.d(TAG, "Power: onNewRequestFinished: Request Successfully Sent");
                    break;
                default:
		    if (mFailCounter == MAX_FAIL_COUNTER) {
			mFailCounter = 0;
			broadcastBikePowerRequestResponse(false, null, null);
			Log.w(TAG, "Power: onNewRequestFinished: Request Failed to be Sent " + requestStatus);
		    } else {
			Log.w(TAG, "Power: onNewRequestFinished: Request Failed to be Sent " + requestStatus);
			mFailHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
				    mFailCounter++;
				    requestManualCalibration();
				}}, 1000);
		    }
                    break;
            }
        }
    };

    public boolean requestManualCalibration(){
        if(bwPcc == null) return false;
        boolean result = bwPcc.requestManualCalibration(requestFinishedReceiver);
        if(!result) broadcastBikePowerRequestResponse(result, null, null);
        return result;
        
    }

    public boolean requestCustomCalibrationParameters(){
        if(bwPcc == null) return false;
        //TODO Manufacturer specific data required here
        byte[] customParameters = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        boolean result = bwPcc.requestCustomCalibrationParameters(customParameters, requestFinishedReceiver);
        if(!result) broadcastBikePowerRequestResponse(result, null, null);
        return result;
    }
    
    public boolean setCustomCalibrationParameters(){
        if(bwPcc == null) return false;
        //TODO Manufacturer specific data required here
        byte[] customParameters = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        boolean result = bwPcc.requestSetCustomCalibrationParameters(customParameters, requestFinishedReceiver);
        if(!result) broadcastBikePowerRequestResponse(result, null, null);
        return result;
    }
    
    public boolean setCtfSlope(BigDecimal newSlope){
        if(bwPcc == null) return false;
        //TODO UI to allow user to set slope
        //BigDecimal newSlope = new BigDecimal("10.0");
        boolean result = bwPcc.requestSetCtfSlope(newSlope,  requestFinishedReceiver);
        if(!result) broadcastBikePowerRequestResponse(result, null, null);
        return result;
    }
    
    public boolean setAutoZero(boolean autoZeroEnable){
        if(bwPcc == null) return false;
        boolean result = bwPcc.requestSetAutoZero(autoZeroEnable, requestFinishedReceiver);
        if(!result) broadcastBikePowerRequestResponse(result, null, null);
        return result;
    }
    
    public boolean requestCrankParameters(BigDecimal newCrankLength){
        if(bwPcc == null) return false;
        //TODO UI to allow user to input crank length
        //BigDecimal newCrankLength = new BigDecimal("172.5");
        boolean result = false;
        if(newCrankLength == null) { //auto
            result = bwPcc.requestSetCrankParameters(CrankLengthSetting.AUTO_CRANK_LENGTH, null, requestFinishedReceiver);
        }else{
            CrankLengthSetting newSetting = CrankLengthSetting.MANUAL_CRANK_LENGTH;
            result = bwPcc.requestSetCrankParameters(newSetting, newCrankLength, requestFinishedReceiver);
        }
        if(!result) broadcastBikePowerRequestResponse(result, null, null);
        return result;
    }
    
    
    /**
     * Subscribes all the data events
     */
    @Override
    public void subscribeToEvents() {
        bwPcc.subscribeCalculatedPowerEvent(
                new ICalculatedPowerReceiver() {
                    @Override
                    public void onNewCalculatedPower(
                            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                            final DataSource dataSource,
                            final BigDecimal calculatedPower) {
                        //save the current data source, one of the values in mSupportedSources
                        if(mSource == INVALID_POWER_SOURCE || (dataSource.name().equals(DataSource.INVALID_CTF_CAL_REQ.name()))) {
                            if (mSupportedSources.contains(dataSource.name())) {
                                mSource = dataSource.ordinal();
                            }
                            List<AntPlusSensor> powerDevices = AntPlusManager.getInstance(mOwner.getContext()).getConnectedDevices(EXTRA_BIKE_POWER_PROFILE);
                            if (powerDevices.size() > 0 && mSource != INVALID_POWER_SOURCE) {
                                // for ctf device, the mSource would change from INVALID_CTF_CAL_REQ to CTF_DATA after calibration
                                ((PowerSensor)powerDevices.get(0)).setPowerSource(mSource); 
                                AntPlusManager.getInstance(mOwner.getContext()).persistRememberedDevice();
                            }
                            Log.d(TAG, "dataSource: " + dataSource.name());
                        }
                        //NOTE: The calculated power event will send an initial value code if it needed to calculate a NEW average.
                        //This is important if using the calculated power event to record user data, as an initial value indicates an average could not be guaranteed.
                        //The event prioritizes calculating with torque data over power only data.
                        // the ctf power comes from here
                        if(mSource == DataSource.INVALID_CTF_CAL_REQ.ordinal() || mSource == DataSource.CTF_DATA.ordinal())
                            broadcastBikePower(calculatedPower.intValue()); 
                    }
                });

        bwPcc.subscribeCalculatedTorqueEvent(
                new ICalculatedTorqueReceiver() {
                    @Override
                    public void onNewCalculatedTorque(
                            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                            final DataSource dataSource,
                            final BigDecimal calculatedTorque) {
                        //NOTE: The calculated torque event will send an initial value code if it needed to calculate a NEW average.
                        //This is important if using the calculated torque event to record user data, as an initial value indicates an average could not be guaranteed.
                        //Log.d(TAG, "onNewCalculatedTorque: " + calculatedTorque.doubleValue()); //Average Torque, unit is "Nm"
                        broadcastBikePowerRequestResponse(true, null, String.valueOf(calculatedTorque.setScale(2, RoundingMode.CEILING)));
                        //broadcastBikePowerMetrics(EXTRA_BIKE_POWER_AVG_TORQUE, calculatedTorque.doubleValue());
                    }
                });
        
        bwPcc.subscribeCalculatedCrankCadenceEvent(
                new ICalculatedCrankCadenceReceiver() {
                    @Override
                    public void onNewCalculatedCrankCadence(
                            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                            final DataSource dataSource,
                            final BigDecimal calculatedCrankCadence) {
                                //NOTE: The calculated crank cadence event will send an initial value code if it needed to calculate a NEW average.
                                //This is important if using the calculated crank cadence event to record user data, as an initial value indicates an average could not be guaranteed.
                                //Log.d(TAG, "onNewCalculatedCrankCadence: " + calculatedCrankCadence.doubleValue()); //Average Cadence, unit is "RPM"
                                //broadcastBikePowerMetrics(EXTRA_BIKE_POWER_AVG_CADENCE, calculatedCrankCadence.doubleValue());
                                // the ctf cadence comes from here    
                                if(mSource == DataSource.INVALID_CTF_CAL_REQ.ordinal() || mSource == DataSource.CTF_DATA.ordinal())
                                    broadcastBikeCadence(calculatedCrankCadence.intValue()); 
                    }
                });
        
        bwPcc.subscribeCalculatedWheelSpeedEvent(
                new CalculatedWheelSpeedReceiver(new BigDecimal(mCircumference)) {
                    @Override
                    public void onNewCalculatedWheelSpeed(
                            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                            final DataSource dataSource,
                            final BigDecimal calculatedWheelSpeed) {
                        //NOTE: The calculated speed event will send an initial value code if it needed to calculate a NEW average.
                        //This is important if using the calculated speed event to record user data, as an initial value indicates an average could not be guaranteed.
                        //Log.d(TAG, "onNewCalculatedWheelSpeed: " + calculatedWheelSpeed.doubleValue()); //Average Speed, unit is "km/h"
                        //broadcastBikePowerMetrics(EXTRA_BIKE_POWER_AVG_SPEED, calculatedWheelSpeed.doubleValue());
                        broadcastBikeSpeed(calculatedWheelSpeed.intValue());
                    }
                });

        bwPcc.subscribeInstantaneousCadenceEvent(
                new IInstantaneousCadenceReceiver() {
                    @Override
                    public void onNewInstantaneousCadence(
                            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                            final DataSource dataSource,
                            final int instantaneousCadence) {
                        //instantaneousCadence < 0 is invalid value
                        //Log.d(TAG, "onNewInstantaneousCadence, instantaneousCadence: " + instantaneousCadence); //Cadence, unit is "RPM"
                        //broadcastBikePowerMetrics(EXTRA_BIKE_POWER_CADENCE, instantaneousCadence);
                        broadcastBikeCadence(instantaneousCadence);
                    }
                });

        bwPcc.subscribeRawPowerOnlyDataEvent(
                new IRawPowerOnlyDataReceiver() {
                    @Override
                    public void onNewRawPowerOnlyData(
                            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                            final long powerOnlyUpdateEventCount,
                            final int instantaneousPower,
                            final long accumulatedPower) {
                        //Log.d(TAG, "onNewRawPowerOnlyData, powerOnlyUpdateEventCount: " + powerOnlyUpdateEventCount);
                        //Log.d(TAG, "onNewRawPowerOnlyData, instantaneousPower: " + instantaneousPower); //Power, unit is "w"
                        //Log.d(TAG, "onNewRawPowerOnlyData, accumulatedPower: " + accumulatedPower); //unit is "w"
                        broadcastBikePower(instantaneousPower); 
                    }
                });

        // get Calibration Message response
        bwPcc.subscribeCalibrationMessageEvent(
                new ICalibrationMessageReceiver() {
                    @Override
                    public void onNewCalibrationMessage(
                            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                            final CalibrationMessage calibrationMessage) {
                        String message = "";
                        boolean result = true;
                        String calibrationData = null;
                        switch(calibrationMessage.calibrationId) {
                            case GENERAL_CALIBRATION_FAIL:
                                result = false;
                            case GENERAL_CALIBRATION_SUCCESS:
                                calibrationData = String.valueOf(calibrationMessage.calibrationData.intValue());
                                message = "calibrationData: " + calibrationMessage.calibrationData.toString();
                                broadcastBikePowerRequestResponse(result, calibrationData, null);
                                break;
                            case CUSTOM_CALIBRATION_RESPONSE:
                            case CUSTOM_CALIBRATION_UPDATE_SUCCESS:
                                String bytes = "";
                                for(byte manufacturerByte : calibrationMessage.manufacturerSpecificData)
                                    bytes += "[" + manufacturerByte + "]";
                                message = "manufacturerSpecificBytes: " + bytes;
                                break;
                            case CTF_ZERO_OFFSET:
                                 message = "ctfOffset: " + calibrationMessage.ctfOffset.toString();
                                 broadcastBikePowerRequestResponse(result, calibrationMessage.ctfOffset.toString(), null);
                                break;
                            case UNRECOGNIZED:
                                //TODO This flag indicates that an unrecognized value was sent by the service, an upgrade of your PCC may be required to handle this new value.
                                message = "onNewCalibrationMessage: Failed: UNRECOGNIZED. Upgrade Required?";
                                result = false;
                            default:
                                result = false;
                                break;
                        }
                        Log.d(TAG, "onNewCalibrationMessage: " + result + ", " + message);
                    }
                });
    }

    /**
     * to handle the connect request result
     */
    protected IPluginAccessResultReceiver<AntPlusBikePowerPcc> base_IPluginAccessResultReceiver =
            new IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {
                // Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusBikePowerPcc result,
                        RequestAccessResult resultCode,
                        DeviceState initialDeviceState) {
                    onResultReceivedGeneric(result, resultCode, initialDeviceState);
                }
            };

    // broadcast connected message
    @Override
    public void broadcastConnectedMessage(int devNum) {
        super.broadcastConnectedMessage(devNum);
    }

    @Override
    protected Runnable generateCustomizeDisconnectRunnable(final int devNum) {
        return new Runnable() {
            public void run() {
                broadcastStateChangeCommon(devNum, false);
                //TODO broadcast disconnect message to transcend service
                broadcastBikePower(INVALID_POWER);
            }
        };
    }

    @Override
    protected String getDeviceName() {
        return "Power Device";
    }

}
