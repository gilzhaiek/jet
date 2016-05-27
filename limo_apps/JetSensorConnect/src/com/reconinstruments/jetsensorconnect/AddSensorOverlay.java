
package com.reconinstruments.jetsensorconnect;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import com.reconinstruments.antplus.AntService;
import com.reconinstruments.antplus.BCManager;
import com.reconinstruments.antplus.BSCManager;
import com.reconinstruments.antplus.BSManager;
import com.reconinstruments.antplus.BWManager;
import com.reconinstruments.antplus.HRManager;
import com.reconinstruments.commonwidgets.ReconJetDialog;

import java.util.List;

/**
 * <code>AddSensorOverlay</code> shows a list of options to add different sensor.
 */
public class AddSensorOverlay extends ReconJetDialog {
    private static final String TAG = AddSensorOverlay.class.getSimpleName();

    private SensorConnectActivity mActivity;

    public AddSensorOverlay(String title, List<Fragment> list, int layout,
            SensorConnectActivity activity) {
        super(title, list, layout, -1);
        mActivity = activity;
    }

    @Override
    protected void setupKeyListener() {
        mPager.setPadding(80,0,80,0); // eyeball the width to fir the current items
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                    }
                    if (mPager.getCurrentItem() == 0) { // hr monitor
                        mActivity.showSensorListOverlay(HRManager.EXTRA_HEART_RATE_PROFILE);
                    }else if(mPager.getCurrentItem() == 1){ // cadence
                        mActivity.showSensorListOverlay(BCManager.EXTRA_BIKE_CADENCE_PROFILE);
                    }else if(mPager.getCurrentItem() == 2){ // speed
                        mActivity.showSensorListOverlay(BSManager.EXTRA_BIKE_SPEED_PROFILE);
                    }else if(mPager.getCurrentItem() == 3){ // speed + cadence
                        mActivity.showSensorListOverlay(BSCManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE);
                    }else if(mPager.getCurrentItem() == 4){ // power
                        mActivity.showSensorListOverlay(BWManager.EXTRA_BIKE_POWER_PROFILE);
                    }
                    getDialog().dismiss();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) { // if back button is presssed
                    getDialog().dismiss();
                    return true;
                }
                return false;
            }
        });
    }
}
