
package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.messagecenter.MessageDBSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.phone.PhoneLogProvider;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;

import java.util.List;

public class SetUnitsOverlay extends ReconJetDialog {
    private static final String TAG = SetUnitsOverlay.class.getSimpleName();

    private ActivityActivity mActivity;

    public SetUnitsOverlay(String title, List<Fragment> list, int layout, ActivityActivity activity) {
        //custom item width to 220dp
        super(title, list, layout, 240);
        mActivity = activity;
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
                    int unitSetting = SettingsUtil.getUnits(mActivity.getBaseContext());
                    if (mPager.getCurrentItem() == 0) {
                        if( unitSetting != SettingsUtil.RECON_UNITS_METRIC ){
                            SettingsUtil.setUnits( mActivity.getBaseContext(), SettingsUtil.RECON_UNITS_METRIC );
                        }
                    } else if(mPager.getCurrentItem() == 1) {
                        if( unitSetting != SettingsUtil.RECON_UNITS_IMPERIAL ){
                            SettingsUtil.setUnits( mActivity.getBaseContext(),SettingsUtil.RECON_UNITS_IMPERIAL );
                        }
                    }

                    getDialog().dismiss();
                    mActivity.updateUnits();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) { // if back button is presssed
                    getDialog().dismiss();
                    return true;
                }
                return false;
            }
        });
        mPager.setPadding(100,0,100,0);
    }
}
