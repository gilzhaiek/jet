
package com.reconinstruments.jetsensorconnect;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.AsyncScanResultDeviceInfo;
import com.reconinstruments.antplus.AntPlusManager;
import com.reconinstruments.antplus.AntPlusSensor;
import com.reconinstruments.antplus.AntService;
import com.reconinstruments.antplus.BSCManager;
import com.reconinstruments.antplus.BSManager;
import com.reconinstruments.antplus.BWManager;
import com.reconinstruments.antplus.HRManager;
import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.commonwidgets.FeedbackDialog.ICONTYPE;

import java.util.List;

/**
 * <code>SensorOptionsOverlay</code> is for disabling/enabling/forgetting device.
 */
public class SensorOptionsOverlay extends ReconJetDialog {

    private static final String TAG = SensorOptionsOverlay.class.getSimpleName();

    private SensorConnectActivity mActivity;
    private AntPlusSensor mDevice;
    private int mPosition;
    private Dialog mDialog;
    private SparseArray<String> mActionArray;

    public SensorOptionsOverlay(AntPlusSensor device, List<Fragment> list, int layout, SensorConnectActivity activity, int position, SparseArray<String> actionArray) {
        super(device.convertedDisplayName().toUpperCase(), list, layout); 
        mActivity = activity;
        mDevice = device;
        mPosition = position;
        mActionArray = actionArray;
    }

    @Override
    protected void setupKeyListener() {
        mDialog = getDialog();
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                    }
                    AntPlusManager antPlusManager = AntPlusManager.getInstance(mActivity.getApplicationContext());
                    if ("Connect".equals(mActionArray.get(mPager.getCurrentItem())) || "Disconnect".equals(mActionArray.get(mPager.getCurrentItem()))) { // disable or enable
                        if (!antPlusManager.isConnected(mDevice)) { // enable
                            FeedbackDialog.showDialog(mActivity, "Connecting...", null, null, FeedbackDialog.SHOW_SPINNER);
                            Log.i(TAG, "connecting to " + mDevice.getDisplayName() + ", device number: " + mDevice.getDeviceNumber());
                            connectOrDisconnect(true);
                        } else { // disable
                            FeedbackDialog.showDialog(mActivity, "Disconnecting...", null, null, FeedbackDialog.SHOW_SPINNER);
                            Log.i(TAG, "disconnecting to " + mDevice.getDisplayName() + ", device number: " + mDevice.getDeviceNumber());
                            connectOrDisconnect(false);
                        }
                        new CountDownTimer(5 * 1000, 1000) { //give 5 seconds delay to dismiss feedback dialog
                            public void onTick(long millisUntilFinished) {}
                            public void onFinish() {
                                mActivity.reloadSensors();
                                FeedbackDialog.dismissDialog(mActivity);
                                mDialog.dismiss();
                            }
                        }.start();
                    } else if ("Forget".equals(mActionArray.get(mPager.getCurrentItem()))) { // forget, disconnect first
                        Log.i(TAG, "disconnecting to " + mDevice.getDisplayName() + ", device number: " + mDevice.getDeviceNumber());
                        connectOrDisconnect(false);
                        antPlusManager.removeFromRememberList(mDevice);
                        FeedbackDialog.showDialog(mActivity, "Sensor Removed", null, FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                        new CountDownTimer(3 * 1000, 1000) {
                            public void onTick(long millisUntilFinished) {}
                            public void onFinish() {
                                mActivity.reloadSensors();
                                FeedbackDialog.dismissDialog(mActivity);
                                mDialog.dismiss();
                            }
                        }.start();
                    } else if ("Calibrate".equals(mActionArray.get(mPager.getCurrentItem()))) { // calibration
                        if (!antPlusManager.isConnected(mDevice)) { //if disconected then connect first
                            FeedbackDialog.showDialog(mActivity, "Connecting...", null, null, FeedbackDialog.SHOW_SPINNER);
                            connectOrDisconnect(true);
                            new CountDownTimer(5 * 1000, 1000) { //give 5 seconds delay to dismiss feedback dialog
                                public void onTick(long millisUntilFinished) {}
                                public void onFinish() {
                                    FeedbackDialog.dismissDialog(mActivity);
                                    mActivity.startActivity(new Intent("com.reconinstruments.jetsensorconnect.calibrate"));
                                    mDialog.dismiss();
                                }
                            }.start();
                       }else{
                           mActivity.startActivity(new Intent("com.reconinstruments.jetsensorconnect.calibrate"));
                           mDialog.dismiss();
                       }
                    } else if ("Wheel Size".equals(mActionArray.get(mPager.getCurrentItem()))) { // wheel size
                        mActivity.showCircumferenceOverlay(mDevice, true);
                        mDialog.dismiss();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) { // if back button is presssed
                    mDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
    }
    
    private void connectOrDisconnect(boolean connect){
        Intent i = new Intent(AntService.ACTION_ANT_SERVICE);
        i.putExtra(AntService.EXTRA_SENSOR_PROFILE, mDevice.getProfile());
        i.putExtra(AntService.EXTRA_ANT_SERVICE_CONNECT, connect);
        i.putExtra(AntService.EXTRA_SHOW_PASSIVE_NOTIFICATION, false);
        i.putExtra(AntService.EXTRA_SENSOR_ID, mDevice.getDeviceNumber());
        mActivity.getApplicationContext().startService(i);
    }
}
