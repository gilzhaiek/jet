
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

public class VideoCapOverlay extends ReconJetDialog {
    private static final String TAG = VideoCapOverlay.class.getSimpleName();

    private BatterySavingActivity mActivity;

    public VideoCapOverlay(String title, List<Fragment> list, int layout, BatterySavingActivity activity) {
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
                    if (mPager.getCurrentItem() == 0) { //15 seconds
                        SettingsUtil.setSystemInt(mActivity,SettingsUtil.VIDEO_RECORD_DURATION, 15);
                    } else if(mPager.getCurrentItem() == 1) { //30 seconds
                        SettingsUtil.setSystemInt(mActivity,SettingsUtil.VIDEO_RECORD_DURATION, 30);
                    } else if(mPager.getCurrentItem() == 2) { //1 minute
                        SettingsUtil.setSystemInt(mActivity,SettingsUtil.VIDEO_RECORD_DURATION, 60);
                    }

                    getDialog().dismiss();
                    mActivity.updateVideoCap();
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
