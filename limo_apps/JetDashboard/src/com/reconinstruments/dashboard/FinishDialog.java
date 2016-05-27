
package com.reconinstruments.dashboard;

import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.ReconJetDialog;

/**
 * 
 * <code>FinishDialog</code> pops up a fragment dialog to ask if user finish sports or not.
 *
 */
public class FinishDialog extends ReconJetDialog {

    private DashboardActivity mActivity;

    public FinishDialog(String title, List<Fragment> list, DashboardActivity activity) {
        super(title, list);
        mActivity = activity;
    }

    @Override
    protected void setupKeyListener(){
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
		    keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                        }
                    if (mPager.getCurrentItem() == 1) { // finish
                        MetricManager.getInstance(mActivity.getApplicationContext()).stopSportsActivity();
                        Intent intent = new Intent(mActivity, ActivitySummaryActivity.class);
                        intent.putExtra(MetricManager.EXTRA_SUMMARY, MetricManager.getInstance(mActivity.getApplicationContext()).getLatestData());
                        startActivity(intent);
                        mActivity.dismissPauseDialog();
                    }
                    getDialog().dismiss();
                }else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                    }
                    getDialog().dismiss();
              }
                return false;
            }
        });
    }
}
