//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlivestats;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import com.reconinstruments.commonwidgets.PopUpDialog;
import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.utils.DeviceUtils;
import android.os.CountDownTimer;

import java.util.List;

public class FinishDayOverlay extends ReconJetDialog {

    private FragmentActivity mActivity;
    private PopUpDialog mFeedbackDialog;

    public FinishDayOverlay(String title, List<Fragment> list, int layout,
                            FragmentActivity activity) {
        super(title, list, layout, -1);
        mActivity = activity;
    }

    @Override
    protected void setupKeyListener() {
        mPager.setPadding(80, 0, 80, 0); // eyeball the width to fit 'Save & Finish'
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        if (event.getAction() != KeyEvent.ACTION_UP) return false;
                        if (mPager.getCurrentItem() == 0) { // cancel
                            getDialog().dismiss();
                            return true; // Finish activity after the timer in showDaySavedDialog
                        } else if (mPager.getCurrentItem() == 1) { // save & finish
                            ActivityUtil.stopActivity(mActivity);
                            ActivityUtil.saveActivity(mActivity);
                            if (mActivity instanceof DaySummaryActivity) {
                                ((DaySummaryActivity) mActivity).showDaySavedDialog();
                            } else if (mActivity instanceof PostActivityMapOverlayActivity) {
                                ((PostActivityMapOverlayActivity) mActivity).showDaySavedDialog();
                            }
                            return true; // Finish activity after the timer in showDaySavedDialog
                        } else if (mPager.getCurrentItem() == 2) { // share save & finish
                            ActivityUtil.stopActivity(mActivity);
                            ActivityUtil.saveActivity(mActivity);

                            mFeedbackDialog = new PopUpDialog((DeviceUtils.isSun()) ? "Activity Saved" : "Day Saved", null,
                                    mActivity, PopUpDialog.ICONTYPE.CHECKMARK).showDialog(PopUpDialog.DEFAULT_TIMEOUT, new Runnable() {
                                @Override
                                public void run() {
                                    Intent i = new Intent("com.reconinstruments.social.facebook.ShareDayActivity");
                                    i.putExtra("backt_to_main_screen", true);
                                    mActivity.startActivity(i);
                                    mActivity.finish();
                                    mActivity.overridePendingTransition(R.anim.fade_slide_in_bottom,R.anim.fadeout_faster);
                                    getDialog().dismiss();
                                }
                            });
                            return true; // Finish activity after the timer in showDaySavedDialog
                        } else if (mPager.getCurrentItem() == 3) { // discard
                            ActivityUtil.stopActivity(mActivity);
                            try {
                                Thread.sleep(300); // to prevent possible
                                // race condition with
                                // discard activity
                            } catch (Exception e) {
                            }
                            ActivityUtil.discardActivity(mActivity);
			    goHome();
			    getDialog().dismiss();
                        }

                        return true;
                    }
                    // if back button is presssed
                    else if (keyCode == KeyEvent.KEYCODE_BACK) {
                        getDialog().dismiss();
                        return true;
                    }
                    return false;
                }
            });
    }

    // FIXME: code duplication from DaySummaryActivty
    private void goHome() {
	mActivity.startActivity(new Intent("com.reconinstruments.itemhost"));
	mActivity.finish();
    }

}
