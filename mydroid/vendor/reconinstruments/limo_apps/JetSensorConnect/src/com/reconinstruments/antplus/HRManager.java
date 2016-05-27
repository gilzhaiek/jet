package com.reconinstruments.antplus;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataTimestampReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;
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
 * Describe class <code>HRManager</code> here. Class to hide the complexities of
 * dealing with Heartrate events.
 */
public class HRManager extends AntPlusProfileManager {

    private static final String TAG = "HRManager";

    public static final String ACTION_HEART_RATE = "com.reconinstruments.externalsensors.heartrate";
    public static final String EXTRA_HEART_RATE_VALUE = "computedHeartRate";
    public static final int INVALID_HEARTRATE = 255;
    AntPlusHeartRatePcc hrPcc = null;

    public HRManager(IAntContext owner) {
	super(owner);
    }

    @Override
    final protected String getBroadcastAction() {
	return ACTION_HEART_RATE;
    }
    @Override
    final protected String getProfileNameKey() {
	return EXTRA_HEART_RATE_VALUE;
    }
    @Override
    final protected int getProfileInt() {
	return EXTRA_HEART_RATE_PROFILE;
    }

    @Override
    protected AntPluginPcc getPcc() {
	return hrPcc;
    }
    @Override
    protected void setPcc(AntPluginPcc pcc) {
	hrPcc = (AntPlusHeartRatePcc)pcc;
    }


    private Intent mHRChangeIntent =
            new Intent(ACTION_HEART_RATE);
    
    private void broadcastHeartRate(int hr) {
        mHRChangeIntent.putExtra(EXTRA_HEART_RATE_VALUE, hr);
        mOwner.getContext().sendBroadcast(mHRChangeIntent);
    }

    @Override
    public void requestAccessToPcc() {
        AntPlusHeartRatePcc.requestAccess(mOwner.getContext(),
                mAntDeviceId,
                1, // Strongest device
                base_IPluginAccessResultReceiver,
                base_IDeviceStateChangeReceiver);
    }

    /**
     * Resets the PCC connection to request access again and clears any existing
     * display data.
     */
    @Override
    public void releaseAccess() {
        if (hrPcc != null) {
            hrPcc.releaseAccess();
            hrPcc = null;
        }
    }

    /**
     * Switches the active view to the data display and subscribes to all the
     * data events
     */
    @Override
    public void subscribeToEvents() {
        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(final long estTimestamp,
                    final EnumSet<EventFlag> eventFlags,
                    final int computedHeartRate,
                    final long heartBeatCounter) {
                // Log.v(TAG,"new Heartrate Data. HR = " + computedHeartRate);
                broadcastHeartRate(computedHeartRate);

            }
        });
    }

    protected IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
            new IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
                // Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusHeartRatePcc result,
                        RequestAccessResult resultCode,
                        DeviceState initialDeviceState) {
                    onResultReceivedGeneric(result,resultCode,initialDeviceState);
                }
            };

    @Override
    protected Runnable generateCustomizeDisconnectRunnable(final int devNum) {
	return new Runnable() {
            public void run() {
		broadcastStateChangeCommon(devNum,false);
                // set heart rate value to 255
                broadcastHeartRate(INVALID_HEARTRATE);
            }
	};
    }

    @Override
    protected String getDeviceName() {
	return "HR Device";
    }

}
