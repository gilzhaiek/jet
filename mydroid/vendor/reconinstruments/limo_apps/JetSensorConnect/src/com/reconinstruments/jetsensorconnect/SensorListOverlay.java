
package com.reconinstruments.jetsensorconnect;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.AsyncScanResultDeviceInfo;
import com.reconinstruments.antplus.AntPlusManager;
import com.reconinstruments.antplus.AntPlusSensor;
import com.reconinstruments.antplus.PowerSensor;
import com.reconinstruments.antplus.AntService;
import com.reconinstruments.antplus.BSCManager;
import com.reconinstruments.antplus.BSManager;
import com.reconinstruments.antplus.BWManager;
import com.reconinstruments.antplus.BikeSensor;
import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.commonwidgets.FeedbackDialog.ICONTYPE;

import java.util.List;

/**
 * <code>SensorListOverlay</code> shows a list of scanned device for specified device type.
 */
public class SensorListOverlay extends ReconJetDialog {
    private static final String TAG = SensorListOverlay.class.getSimpleName();

    private SensorConnectActivity mActivity;
    private int mProfile;

    public SensorListOverlay(String title, List<Fragment> list, int layout, SensorConnectActivity activity, int profile) {
        super(title, list, layout, -1);
        mActivity = activity;
        mProfile = profile;
    }

    @Override
    protected void setupKeyListener() {
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                    }
                    final AntPlusManager antPlusManager = AntPlusManager.getInstance(mActivity.getApplicationContext());
                    final AsyncScanResultDeviceInfo device = antPlusManager.getScannedDevices().get(mPager.getCurrentItem());
                    
                    if (!antPlusManager.isRemember(mProfile, device.getAntDeviceNumber())) {
                        antPlusManager.addToRememberList(mProfile, device);

                        if(mProfile == BSCManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE || mProfile == BSManager.EXTRA_BIKE_SPEED_PROFILE){
                            mActivity.showCircumferenceOverlay((BikeSensor)(antPlusManager.getDevice(mProfile, device.getAntDeviceNumber())), true);
                            getDialog().dismiss();
                        }else{
                            connectToSensor(device);
                            showPairingOverlay(antPlusManager, device);
                         }
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) { // if back button is presssed
                    getDialog().dismiss();
                    mActivity.showAddSensorOverlay();
                    return true;
                }
                return false;
            }
        });
    }
    
    private void connectToSensor(AsyncScanResultDeviceInfo device){
        Log.i(TAG, "connecting to " + device.getDeviceDisplayName() + ", device number: " + device.getAntDeviceNumber());
        Intent i = new Intent(AntService.ACTION_ANT_SERVICE);
        i.putExtra(AntService.EXTRA_SENSOR_PROFILE, mProfile);
        i.putExtra(AntService.EXTRA_ANT_SERVICE_CONNECT, true);
        i.putExtra(AntService.EXTRA_SHOW_PASSIVE_NOTIFICATION, false);
        i.putExtra(AntService.EXTRA_SENSOR_ID, device.getAntDeviceNumber());
        mActivity.getApplicationContext().startService(i);
    }
    
    private void showPairingOverlay(final AntPlusManager antPlusManager, final AsyncScanResultDeviceInfo device){
        FeedbackDialog.showDialog(mActivity, "Pairing", (antPlusManager.getDevice(mProfile, device.getAntDeviceNumber())).convertedDisplayName(), null, FeedbackDialog.SHOW_SPINNER, false);
        new CountDownTimer(5 * 1000, 1000) { // give 5 seconds to decide if it needs calibration
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                FeedbackDialog.dismissDialog(mActivity);
                showPairedOverlay(antPlusManager, device);
            }
        }.start();
    }
    
    private void showPairedOverlay(final AntPlusManager antPlusManager, final AsyncScanResultDeviceInfo device){
        FeedbackDialog.showDialog(mActivity, "Paired", "Your sensor will connect the next<br>time you start an activity.", FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
        new CountDownTimer(2 * 1000, 1000) {
           public void onTick(long millisUntilFinished) {}
           public void onFinish() {
               mActivity.reloadSensors();
               AntPlusSensor sensor = antPlusManager.getMostRecent(BWManager.EXTRA_BIKE_POWER_PROFILE);
               if(mProfile == BWManager.EXTRA_BIKE_POWER_PROFILE && sensor != null){
                   if(((PowerSensor)sensor).hasCombinedSpeed()){
                       mActivity.showCircumferenceOverlay(antPlusManager.getDevice(mProfile, device.getAntDeviceNumber()), false);
                   }else if(((PowerSensor)sensor).canCalibrate()){
                       mActivity.startActivity(new Intent("com.reconinstruments.jetsensorconnect.calibrate"));
                   }
               }
               FeedbackDialog.dismissDialog(mActivity);
               getDialog().dismiss();
           }
       }.start();
    }
}
