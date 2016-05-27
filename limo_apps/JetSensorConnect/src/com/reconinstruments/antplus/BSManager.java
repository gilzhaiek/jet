
package com.reconinstruments.antplus;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedAccumulatedDistanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.IRawSpeedAndDistanceDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc;

import android.os.Handler;
import android.util.Log;

import java.math.BigDecimal;
import java.util.EnumSet;
import android.content.Intent;

import com.reconinstruments.messagecenter.ReconMessageAPI;

/**
 * Describe class <code>BSManager</code> here. Class to hide the complexities of
 * dealing with Bike Speed events.
 */
public class BSManager extends AntPlusProfileManager {

    private static final String TAG = "BSManager";

    public static final String ACTION_BIKE_SPEED = "com.reconinstruments.externalsensors.bikespeed";
    public static final String EXTRA_BIKE_SPEED_VALUE = "computedBikeSpeed";
    public static final int INVALID_SPEED = -1;
    AntPlusBikeSpeedDistancePcc bsdPcc = null;

    public BSManager(IAntContext owner) {
	super(owner);
    }
    @Override
    final protected String getBroadcastAction() {
	return ACTION_BIKE_SPEED;
    }
    @Override
    final protected String getProfileNameKey() {
	return EXTRA_BIKE_SPEED_VALUE;
    }
    @Override
    final protected int getProfileInt() {
	return EXTRA_BIKE_SPEED_PROFILE;
    }

    @Override
    protected AntPluginPcc getPcc() {
	return bsdPcc;
    }
    @Override
    protected void setPcc(AntPluginPcc pcc) {
	bsdPcc = (AntPlusBikeSpeedDistancePcc)pcc;
    }

    private float mCircumference = 2.095f; // 2.095m circumference = an average 700cx23mm road tire
    
    private void broadcastBikeSpeed(int bs) {
        Intent bsChangeIntent = new Intent(BSManager.ACTION_BIKE_SPEED);
        bsChangeIntent.putExtra(BSManager.EXTRA_BIKE_SPEED_VALUE, bs);
        mOwner.getContext().sendBroadcast(bsChangeIntent);
    }

    public void setCircumference(float circumference){
        mCircumference = circumference;
    }

    /**
     * request to connect
     */
    @Override
    public void requestAccessToPcc() {
        AntPlusBikeSpeedDistancePcc.requestAccess(mOwner.getContext(),
                mAntDeviceId,
                0, // searchProximityThreshold
                false, // isSpdCadCombinedSensor
                base_IPluginAccessResultReceiver,
                base_IDeviceStateChangeReceiver);
    }

    /**
     * Release the PCC connection
     */
    public void releaseAccess() {
        //Release the old access if it exists
        if(bsdPcc != null) {
            bsdPcc.releaseAccess();
            bsdPcc = null;
        }
    }

    /**
     * Subscribes all the data events
     */
    @Override
    public void subscribeToEvents() {

        Log.i(TAG, "set circumference to " + mCircumference);
        bsdPcc.subscribeCalculatedSpeedEvent(new CalculatedSpeedReceiver(new BigDecimal(mCircumference)) { //2.095m circumference = an average 700cx23mm road tire
            @Override
            public void onNewCalculatedSpeed(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final BigDecimal calculatedSpeed) {
                //Log.v(TAG, "onNewCalculatedSpeed: " + calculatedSpeed.intValue());
                broadcastBikeSpeed((int)(calculatedSpeed.floatValue() * 3.6 + 0.5f));
            }
        });
    }

    /**
     * to handle the connect request result
     */
    protected IPluginAccessResultReceiver<AntPlusBikeSpeedDistancePcc> base_IPluginAccessResultReceiver =
        new IPluginAccessResultReceiver<AntPlusBikeSpeedDistancePcc>() {
            // Handle the result, connecting to events on success or reporting failure to user.
            @Override
            public void onResultReceived(AntPlusBikeSpeedDistancePcc result,
                    RequestAccessResult resultCode, DeviceState initialDeviceState) {
		onResultReceivedGeneric(result,resultCode,initialDeviceState);
            }
    };


    // broadcast connected message
    @Override
    public void broadcastConnectedMessage(int devNum) {
	super.broadcastConnectedMessage(devNum);
	BSCManager.ignoreSpeed(true);
    }

    @Override
    protected Runnable generateCustomizeDisconnectRunnable(final int devNum) {
	return new Runnable() {
	    @Override
            public void run() {
		broadcastStateChangeCommon(devNum,false);
		// set speed value to -1
		broadcastBikeSpeed(INVALID_SPEED);
		BSCManager.ignoreSpeed(false);
            }
        };
    }
    @Override
    protected String getDeviceName() {
	return "Speed Device";
    }

}
