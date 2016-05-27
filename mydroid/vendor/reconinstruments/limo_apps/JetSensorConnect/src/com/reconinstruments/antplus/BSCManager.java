
package com.reconinstruments.antplus;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc.ICalculatedCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedAccumulatedDistanceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.IRawSpeedAndDistanceDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc;

import android.os.Handler;
import android.util.Log;

import java.math.BigDecimal;
import java.util.EnumSet;
import android.content.Intent;

import com.reconinstruments.messagecenter.ReconMessageAPI;

/**
 * Describe class <code>BSCManager</code> here. Class to hide the complexities of
 * dealing with Bike Speed Cadence events.
 */
public class BSCManager extends AntPlusProfileManager {

    private static final String TAG = "BSCManager";
    // This one is not really used. We specifically use speed or cadence
    public static final String EXTRA_BIKE_SPEED_CADENCE = "computedBikeSpeedCadence";
    
    public static final String ACTION_SPEED_CADENCE = "com.reconinstruments.externalsensors.speedcadence";
    // TODO Check this shit why to Pcc
    AntPlusBikeSpeedDistancePcc bsdPcc = null;
    AntPlusBikeCadencePcc bcPcc = null;

    public BSCManager(IAntContext owner) {
	super(owner);
    }
    @Override
    final protected String getBroadcastAction() {
	return ACTION_SPEED_CADENCE;
    }
    @Override
    final protected String getProfileNameKey() {
	return EXTRA_BIKE_SPEED_CADENCE;
    }
    @Override
    final protected int getProfileInt() {
	return EXTRA_BIKE_SPEED_CADENCE_PROFILE;
    }


    @Override
    protected AntPluginPcc getPcc() {
	return bsdPcc;
    }
    @Override
    protected void setPcc(AntPluginPcc pcc) {
	bsdPcc = (AntPlusBikeSpeedDistancePcc)pcc;
    }

    private static boolean mIgnoreSpeed = false;
    private static boolean mIgnoreCadence = false;
    private float mCircumference = 2.095f; // 2.095m circumference = an average 700cx23mm road tire
    
    public static void ignoreSpeed(boolean ignoreSpeed) {
        mIgnoreSpeed = ignoreSpeed;
    }

    public static void ignoreCadence(boolean ignoreCadence) {
        mIgnoreCadence = ignoreCadence;
    }

    public void setCircumference(float circumference){
        mCircumference = circumference;
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

    /**
     * request to connect
     */
    @Override
    public void requestAccessToPcc() {
        AntPlusBikeSpeedDistancePcc.requestAccess(mOwner.getContext(),
                mAntDeviceId,
                0, // searchProximityThreshold
                true, // isSpdCadCombinedSensor
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

        Log.i(TAG, "set circumference to " + mCircumference);
        bsdPcc.subscribeCalculatedSpeedEvent(new CalculatedSpeedReceiver(new BigDecimal(mCircumference)) { //2.095m circumference = an average 700cx23mm road tire
            @Override
            public void onNewCalculatedSpeed(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final BigDecimal calculatedSpeed) {
                //Log.v(TAG, "onNewCalculatedSpeed: " + calculatedSpeed.intValue());
                broadcastBikeSpeed((int)(calculatedSpeed.floatValue() * 3.6+0.5f));
            }
        });

        AntPlusBikeCadencePcc.requestAccess(
                mOwner.getContext(), 
                bsdPcc.getAntDeviceNumber(), 
                0, // searchProximityThreshold
                true, // isSpdCadCombinedSensor
            new IPluginAccessResultReceiver<AntPlusBikeCadencePcc>() {
                //Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusBikeCadencePcc result, RequestAccessResult resultCode,
                        DeviceState initialDeviceStateCode) {
                    switch(resultCode) {
                        case SUCCESS:
                            bcPcc = result;
                            bcPcc.subscribeCalculatedCadenceEvent(new ICalculatedCadenceReceiver() {
                                @Override
                                public void onNewCalculatedCadence(
                                        long estTimestamp,
                                        EnumSet<EventFlag> eventFlags,
                                        final BigDecimal calculatedCadence) {
                                    broadcastBikeCadence(calculatedCadence.intValue());
                                    //Log.v(TAG, "onNewCalculatedCadence: " + String.valueOf(calculatedCadence));
                                }
                            });
                            break;
                        case CHANNEL_NOT_AVAILABLE:
                            //Log.v(TAG, "isSpeedAndCadenceCombinedSensor CHANNEL NOT AVAILABLE");
                            break;
                        case OTHER_FAILURE:
                            //Log.v(TAG, "isSpeedAndCadenceCombinedSensor OTHER FAILURE");
                            break;
                        case DEPENDENCY_NOT_INSTALLED:
                            //Log.v(TAG, "isSpeedAndCadenceCombinedSensor DEPENDENCY_NOT_INSTALLED");
                            break;
                        default:
                            //Log.v(TAG, "isSpeedAndCadenceCombinedSensor UNRECOGNIZED ERROR: " + resultCode);
                            break;
                    } 
                }
            },
            //Receives state changes
            new IDeviceStateChangeReceiver() {                    
                @Override
                public void onDeviceStateChange(final DeviceState newDeviceState) {
                    if(newDeviceState != DeviceState.TRACKING)
                        Log.v(TAG, "isSpeedAndCadenceCombinedSensor onDeviceStateChange: " + newDeviceState.toString());
                    if(newDeviceState == DeviceState.DEAD){
                        bcPcc = null;
                        broadcastBikeCadence(255);
                    }
                }
            } );
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



    @Override
    protected Runnable generateCustomizeDisconnectRunnable(final int devNum) {
	return new Runnable() {
            public void run() {
		broadcastStateChangeCommon(devNum,false);
                broadcastBikeSpeed(-1);
                // set cadence value to 0XFFFF
                broadcastBikeCadence(0XFFFF);
            }
        };
    }
    @Override
    protected String getDeviceName() {
	return "Spd/Cad Device";
    }

}
