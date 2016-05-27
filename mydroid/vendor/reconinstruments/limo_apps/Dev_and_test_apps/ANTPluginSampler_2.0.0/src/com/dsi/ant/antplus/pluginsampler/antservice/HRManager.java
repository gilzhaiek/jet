package com.dsi.ant.antplus.pluginsampler.antservice;
import com.dsi.ant.antplus.pluginsampler.R;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataTimestampReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;

import android.util.Log;
import android.widget.Toast;
import java.math.BigDecimal;
import java.util.EnumSet;
import android.content.Intent;
/**
 * Describe class <code>HRManager</code> here. Class to hide the
 * complexities of dealing with Heartrate events.
 *
 */
public class HRManager implements IAntProfile {
    private static final String TAG = "HRManager";
    private IAntContext mOwner;
    AntPlusHeartRatePcc hrPcc = null;
    private int mAntDeviceId;
    private Intent mHRChangeIntent =
	new Intent("com.reconinstruments.externalsensors.heartrate");
    public HRManager(IAntContext owner) {
	mOwner = owner;
    }
    private void broadcastHeartRate(int hr) {
	mHRChangeIntent.putExtra("computedHeartRate",hr);
	mOwner.getContext().sendBroadcast(mHRChangeIntent);
    }
    
    @Override
    public void requestAccessToPcc()  {
        AntPlusHeartRatePcc.requestAccess(mOwner.getContext(),
					  mAntDeviceId,
					  1, // Strongest device
					  base_IPluginAccessResultReceiver,
					  base_IDeviceStateChangeReceiver);
    }

    /**
     * Resets the PCC connection to request access again and clears
     * any existing display data.
     */    
    public void releaseAccess(){
        if(hrPcc != null) {
            hrPcc.releaseAccess();
            hrPcc = null;
        }
    }
    
    /**
     * Resets the PCC connection to request access again and clears
     * any existing display data.
     */    
    public void handleReset(int deviceId){
        mAntDeviceId = deviceId;
        //Release the old access if it exists
        releaseAccess();
        requestAccessToPcc();
    }
    /**
     * Switches the active view to the data display and subscribes to
     * all the data events
     */
    public void subscribeToHrEvents(){
        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver(){
		@Override
		public void onNewHeartRateData(final long estTimestamp,
					       final EnumSet<EventFlag> eventFlags,
					       final int computedHeartRate,
					       final long heartBeatCounter) {
		    //Log.v(TAG,"new Heartrate Data. HR = " + computedHeartRate);
		    broadcastHeartRate(computedHeartRate);
		    
		}
	    });
	hrPcc.subscribeHeartRateDataTimestampEvent(new IHeartRateDataTimestampReceiver() {
		@Override
		public void onNewHeartRateDataTimestamp(final long estTimestamp,
							final EnumSet<EventFlag> eventFlags,
							final BigDecimal timestampOfLastEvent) {
		    //Log.v(TAG,"new Heartate data timestamp");
		}
	    });
        hrPcc.subscribePage4AddtDataEvent(new IPage4AddtDataReceiver() {
		@Override
		public void onNewPage4AddtData(final long estTimestamp,
					       final EnumSet<EventFlag> eventFlags,
					       final int manufacturerSpecificByte,
					       final BigDecimal timestampOf2HBAgo) {
		    //Log.v(TAG,"newPage4AddtData");
		}
	    });
        hrPcc.subscribeCumulativeOperatingTimeEvent(new ICumulativeOperatingTimeReceiver()
	    {
		@Override
		public void onNewCumulativeOperatingTime(final long estTimestamp,
							 final EnumSet<EventFlag> eventFlags,
							 final long cumulativeOperatingTime) {
		    //Log.v(TAG,"newCumulativeOperatingTime");
		}
	    });
	hrPcc.subscribeManufacturerAndSerialEvent(new IManufacturerAndSerialReceiver() {
		@Override
		public void onNewManufacturerAndSerial(final long estTimestamp,
						       final EnumSet<EventFlag> eventFlags,
						       final int manufacturerID,
						       final int serialNumber) {
		    Log.v(TAG,"onNewManufacturerAndSerial");
		}
	    });
	hrPcc.subscribeManufacturerAndSerialEvent(new IManufacturerAndSerialReceiver() {
		@Override
		public void onNewManufacturerAndSerial(final long estTimestamp,
						       final EnumSet<EventFlag> eventFlags,
						       final int manufacturerID,
						       final int serialNumber) {
		    Log.v(TAG,"onNewManufacturerAndSerial");
		}
	    });
    }

    protected IPluginAccessResultReceiver <AntPlusHeartRatePcc>
	base_IPluginAccessResultReceiver =
	new IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(AntPlusHeartRatePcc result,
				     RequestAccessResult resultCode,
				     DeviceState initialDeviceState) {
            Log.v(TAG,"Connecting...");
	    Log.v(TAG,"Result Code"+resultCode);
            switch(resultCode) {
	    case SUCCESS:
		hrPcc = result;
		Log.v(TAG,result.getDeviceName() + ": " + initialDeviceState);
		subscribeToHrEvents();
		Log.v(TAG,"SUCCESS");
		
      Intent i = new Intent("heart_rate_device_connected");
      i.putExtra("profile", 0);
      i.putExtra("deviceId", result.getAntDeviceNumber());
      mOwner.getContext().sendBroadcast(i);
		
		break;
	    case CHANNEL_NOT_AVAILABLE:
		Toast.makeText(mOwner.getContext(), "Channel Not Available",
			       Toast.LENGTH_SHORT).show();
		Log.v(TAG,"CHANNEL_NOT_AVAILABLE Error. Do Menu->Reset.");
		break;
	    case OTHER_FAILURE:
		Toast.makeText(mOwner.getContext(),
			       "OTHER_FAILURE RequestAccess failed. See logcat for details.",
			       Toast.LENGTH_SHORT).show();
		Log.v(TAG,"Error. Do Menu->Reset.");
		break;
	    case DEPENDENCY_NOT_INSTALLED:
		Log.e(TAG,"DEPENDENCY_NOT_INSTALLED Error. Do Menu->Reset.");
		break;
	    case USER_CANCELLED:
		Log.v(TAG,"USER_CANCELLED Cancelled. Do Menu->Reset.");
		break;
	    case UNRECOGNIZED:
		//TODO This flag indicates that an unrecognized value was sent by the service, an upgrade of your PCC may be required to handle this new value.
		Toast.makeText(mOwner.getContext(),
			       "Failed: UNRECOGNIZED. Upgrade Required?",
			       Toast.LENGTH_SHORT).show();
		Log.v(TAG,"UNRECOGNIZED Error. Do Menu->Reset.");
		break;
	    default:
		Toast.makeText(mOwner.getContext(),
			       "Unrecognized result: " + resultCode,
			       Toast.LENGTH_SHORT).show();
		Log.v(TAG,"DEFAULT Error. Do Menu->Reset.");
		break;
            } 
        }
    };

    //Receives state changes and shows it on the status display line
    protected  IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver = 
	new IDeviceStateChangeReceiver(){                    
            @Override
            public void onDeviceStateChange(final DeviceState newDeviceState){
		Log.v(TAG,hrPcc.getDeviceName() + ": " + newDeviceState);
		if(newDeviceState == DeviceState.DEAD) hrPcc = null;
            }
        };

    public void cleanup(){
	if(hrPcc != null){
	    hrPcc.releaseAccess();
	    hrPcc = null;
	}
    }
}
