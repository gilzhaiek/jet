
package com.reconinstruments.antplus;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc.ICalculatedCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc.IRawCadenceDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc;

import android.os.Handler;
import android.util.Log;

import java.math.BigDecimal;
import java.util.EnumSet;

import android.content.Context;
import android.content.Intent;

import com.reconinstruments.messagecenter.ReconMessageAPI;

/**
 * Describe class <code>BCManager</code> here. Class to hide the complexities of
 * dealing with Cadence events.
 */
public class BCManager extends AntPlusProfileManager {

    private static final String TAG = "BCManager";

    public static final String ACTION_BIKE_CADENCE = "com.reconinstruments.externalsensors.cadence";
    public static final String EXTRA_BIKE_CADENCE_VALUE = "computedBikeCadence";
    public static final int INVALID_CADENCE = 0XFFFF;
    AntPlusBikeCadencePcc bcPcc = null;

    public BCManager (IAntContext owner) {
	super(owner);
    }
    
    @Override
    final protected String getBroadcastAction() {
	return ACTION_BIKE_CADENCE;
    }
    @Override
    final protected String getProfileNameKey() {
	return EXTRA_BIKE_CADENCE_VALUE;
    }
    @Override
    final protected int getProfileInt() {
	return EXTRA_BIKE_CADENCE_PROFILE;
    }

    @Override
    protected AntPluginPcc getPcc() {
	return bcPcc;
    }
    @Override
    protected void setPcc(AntPluginPcc pcc) {
	bcPcc = (AntPlusBikeCadencePcc)pcc;
    }


    private void broadcastBikeCadence(int bc) {
        Intent bcChangeIntent = new Intent(BCManager.ACTION_BIKE_CADENCE);
        bcChangeIntent.putExtra(BCManager.EXTRA_BIKE_CADENCE_VALUE, bc);
        mOwner.getContext().sendBroadcast(bcChangeIntent);
    }

    /**
     * request to connect
     */
    @Override
    public void requestAccessToPcc() {
        AntPlusBikeCadencePcc.requestAccess(mOwner.getContext(),
                mAntDeviceId,
                0, // searchProximityThreshold
                false, // isSpdCadCombinedSensor
                base_IPluginAccessResultReceiver,
                base_IDeviceStateChangeReceiver);
    }

    /**
     * Release the PCC connection
     */
    @Override
    public void releaseAccess() {
        //Release the old access if it exists
        if(bcPcc != null) {
            bcPcc.releaseAccess();
            bcPcc = null;
        }
    }

    /**
     * Subscribes all the data events
     */
    @Override
    public void subscribeToEvents() {

        bcPcc.subscribeCalculatedCadenceEvent(new ICalculatedCadenceReceiver() {
            @Override
            public void onNewCalculatedCadence(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final BigDecimal calculatedCadence) {
                broadcastBikeCadence(calculatedCadence.intValue());
            }
        });
    }

    /**
     * to handle the connect request result
     */
    protected IPluginAccessResultReceiver<AntPlusBikeCadencePcc> base_IPluginAccessResultReceiver =
        new IPluginAccessResultReceiver<AntPlusBikeCadencePcc>() {
            // Handle the result, connecting to events on success or reporting failure to user.
            @Override
            public void onResultReceived(AntPlusBikeCadencePcc result,
					 RequestAccessResult resultCode,
					 DeviceState initialDeviceState) {
		onResultReceivedGeneric(result,resultCode,initialDeviceState);
	    }
         };

    // broadcast connected message
    @Override
    public void broadcastConnectedMessage(int devNum) {
	super.broadcastConnectedMessage(devNum);
        BSCManager.ignoreCadence(true);
     }

    @Override
    protected Runnable generateCustomizeDisconnectRunnable(final int devNum) {
	return  new Runnable() {
            public void run() {
		broadcastStateChangeCommon(devNum,false);
                // set cadence value to 0XFFFF
                broadcastBikeCadence(0XFFFF);
                BSCManager.ignoreCadence(false);
            }
        };
    }
    @Override
    protected String getDeviceName() {
	return "Cad Device";
    }

}
