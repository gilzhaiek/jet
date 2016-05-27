package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.jetapplauncher.R;

import java.util.List;

public class ScreenDelayOverlay extends ReconJetDialog {
    private static final String TAG = ScreenDelayOverlay.class.getSimpleName();

    private GlanceActivity mActivity;

    public ScreenDelayOverlay(String title, List<Fragment> list, int layout, GlanceActivity activity) {
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
                    int selectedDelay = mPager.getCurrentItem();
                    getDialog().dismiss();
                    mActivity.updateScreenDelay(selectedDelay);
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
