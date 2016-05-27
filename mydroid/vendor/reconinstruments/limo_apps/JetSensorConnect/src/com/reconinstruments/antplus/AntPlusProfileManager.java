package com.reconinstruments.antplus;
import android.os.Handler;
import android.util.Log;

import java.math.BigDecimal;
import java.util.EnumSet;
import android.content.Intent;

import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc;

abstract public class AntPlusProfileManager {
    public static final String TAG = "AntPlusProfileManager";
    
    public static final int EXTRA_HEART_RATE_PROFILE = 0;
    public static final int EXTRA_BIKE_CADENCE_PROFILE = 1;
    public static final int EXTRA_BIKE_SPEED_PROFILE = 2;
    public static final int EXTRA_BIKE_SPEED_CADENCE_PROFILE = 3;
    public static final int EXTRA_BIKE_POWER_PROFILE = 4;
    
    public AntPlusProfileManager(IAntContext owner) {
	mOwner = owner;
    }
    public static final int DEFAULT_DISCONNECT_MSG_DELAY = 10 * 1000;
    protected IAntContext mOwner;
    protected int mAntDeviceId; // support only one bike speed device
    protected Handler mHandler = new Handler();
    abstract protected String getBroadcastAction();
    abstract protected String getProfileNameKey();
    abstract protected int getProfileInt();
    protected String getEventString() {return "event";}
    protected int getConnectedInt() {return 0;}
    protected int getDisconnectedInt() {return 1;}

    
    public void broadcastStateChangeCommon(int devNum,boolean connOrDisconn) {
	AntPlusManager antPlusManager = AntPlusManager.getInstance(mOwner.getContext());
        AntPlusSensor device = antPlusManager.getDevice(getProfileInt(), devNum);
        antPlusManager.updateConnectionState(device, connOrDisconn);
        Intent i = new Intent(getBroadcastAction());
        i.putExtra(getEventString(), getConnectedInt());
        i.putExtra(AntService.EXTRA_SENSOR_PROFILE, getProfileInt());
        i.putExtra(AntService.EXTRA_SENSOR_ID, devNum);
        mOwner.getContext().sendBroadcast(i);
    }
    public void broadcastConnectedMessage(int devNum) {
	if (!mHandler.hasMessages(0)) {
	    showNotificationIfNeeded(devNum,true);
	}
	mHandler.removeCallbacksAndMessages(null);
	broadcastStateChangeCommon(devNum,true);
    }
    public void broadcastDisconnectedMessage(final int devNum, boolean shouldDelay) {
	broadcastDisconnectedMessage(devNum, DEFAULT_DISCONNECT_MSG_DELAY);
    }
    public void broadcastDisconnectedMessage(final int devNum, int timeout) {
	mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessage(0); // insert an empty message into the queue
	mHandler.postDelayed(generateCustomizeDisconnectRunnable(devNum),timeout);
    }

    protected void showNotificationIfNeeded(int devNum, boolean isConnected) {
        String conn = isConnected ? " connected" : " disconnected";
        if (AntService.mShowPassiveNotification) {
            ReconMessageAPI.showPassiveNotification(mOwner.getContext(),
                    getDeviceName() + " " + devNum +
                            conn,
                    ReconMessageAPI
                    .NOTIFICATION_SHORT_DURATION);
        }
    }

    abstract protected String getDeviceName();
    abstract protected Runnable generateCustomizeDisconnectRunnable(final int devNum);
    abstract protected AntPluginPcc getPcc();
    abstract protected void setPcc(AntPluginPcc pcc);
    protected IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
        new IDeviceStateChangeReceiver() {
            DeviceState previousDeviceState = DeviceState.UNRECOGNIZED;
           @Override
            public void onDeviceStateChange(final DeviceState newDeviceState) {
                Log.v(TAG, getPcc().getDeviceName() + ": " + newDeviceState);
                if (newDeviceState == DeviceState.DEAD) {
                    Log.w(TAG, "broadcastDisconnectedMessage, newDeviceState: " + DeviceState.DEAD);
                    broadcastDisconnectedMessage(getPcc().getAntDeviceNumber(), true);
                    setPcc(null);
                } else if (newDeviceState == DeviceState.SEARCHING) { // disconnected in this case
                    Log.w(TAG, "broadcastDisconnectedMessage, newDeviceState: " + DeviceState.SEARCHING);
                    broadcastDisconnectedMessage(getPcc().getAntDeviceNumber(), true);
                } else if (newDeviceState == DeviceState.TRACKING) { // connected in this case
                    if(previousDeviceState != DeviceState.PROCESSING_REQUEST)
                        broadcastConnectedMessage(getPcc().getAntDeviceNumber());
                } else if(newDeviceState == DeviceState.PROCESSING_REQUEST){ //skip it
                    Log.d(TAG, "newDeviceState: " + newDeviceState);
                } else {
                    Log.w(TAG, "broadcastDisconnectedMessage, newDeviceState: " + newDeviceState);
                    broadcastDisconnectedMessage(getPcc().getAntDeviceNumber(), true);
                }
                previousDeviceState = newDeviceState;
            }
	};

    abstract public void subscribeToEvents();
    abstract public void requestAccessToPcc();
    abstract public void releaseAccess();

    public void onResultReceivedGeneric(AntPluginPcc result,
				       RequestAccessResult resultCode,
				       DeviceState initialDeviceState) {
	Log.v(TAG, "Connecting...");
	Log.v(TAG, "Result Code: " + resultCode);
	if(result == null) return;
	// broadcast connection state message
	if (resultCode.equals(RequestAccessResult.SUCCESS)) { // connected
	    broadcastConnectedMessage(result.getAntDeviceNumber());
	} else { // disconnected or connect fails for some reason
	    broadcastDisconnectedMessage(result.getAntDeviceNumber(), true);
	}
	switch (resultCode) {
	case SUCCESS:
	    setPcc(result);
	    Log.v(TAG, result.getDeviceName() + ": " + initialDeviceState);
	    subscribeToEvents();
	    Log.v(TAG, "SUCCESS");
	    break;
	case CHANNEL_NOT_AVAILABLE:
	    Log.v(TAG, "CHANNEL_NOT_AVAILABLE Error. Do Menu->Reset.");
	    break;
	case OTHER_FAILURE:
	    Log.v(TAG, "Error. Do Menu->Reset.");
	    break;
	case DEPENDENCY_NOT_INSTALLED:
	    Log.e(TAG, "DEPENDENCY_NOT_INSTALLED Error. Do Menu->Reset.");
	    break;
	case USER_CANCELLED:
	    Log.v(TAG, "USER_CANCELLED Cancelled. Do Menu->Reset.");
	    break;
	case UNRECOGNIZED:
	    Log.v(TAG, "UNRECOGNIZED Error. Do Menu->Reset.");
	    break;
	default:
	    Log.v(TAG, "DEFAULT Error. Do Menu->Reset.");
	    break;
	}
    }

    /**
     * Resets the PCC connection to request access again
     */
    public void handleReset(int deviceId) {
        mAntDeviceId = deviceId;
        // Release the old access if it exists
        releaseAccess();
        requestAccessToPcc();
    }
    public void cleanup() {
        releaseAccess();
    }

}