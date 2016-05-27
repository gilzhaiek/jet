
package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.app.FragmentActivity;
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
import com.reconinstruments.utils.stats.TranscendUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;


import java.util.List;

public class ResetStatsOverlay extends ReconJetDialog {
    private static final String TAG = ResetStatsOverlay.class.getSimpleName();

    private FragmentActivity mActivity;

    public ResetStatsOverlay(String title, String desc, List<Fragment> list, int layout, FragmentActivity activity) {
        super(title, desc, list, layout);
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
                    if (mPager.getCurrentItem() == 1) { // reset
			TranscendUtils.sendCommandToTranscendService(mActivity,
								     18);
			TranscendUtils.sendCommandToTranscendService(mActivity,
								     19);
                        FeedbackDialog.showDialog(mActivity, "Stats Reset", null,
                                FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                        new CountDownTimer(2 * 1000, 1000) {
                            public void onTick(long millisUntilFinished) {
                            }
                            public void onFinish() {
                                FeedbackDialog.dismissDialog(mActivity);
                                getDialog().dismiss();
                            }
                        }.start();
                    }else{
                        getDialog().dismiss();
                    }
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
