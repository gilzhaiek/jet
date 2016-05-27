
package com.reconinstruments.dashboard;

import java.util.List;

import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.ReconJetDialog;

/**
 * 
 * <code>FinishDialog</code> pops up a fragment dialog to ask if the user resume sports or not.
 *
 */
public class ResumeDialog extends ReconJetDialog {

    private DashboardActivity mActivity;

    public ResumeDialog(String title, List<Fragment> list, DashboardActivity activity) {
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
                    if (mPager.getCurrentItem() == 0) { // resume
                        MetricManager.getInstance(mActivity.getApplicationContext()).resumeSportsActivity();
                        getDialog().dismiss();
                   } else if (mPager.getCurrentItem() == 1) { // launch finishing confirmation dialog
                       getDialog().dismiss();
               mActivity.showFinishDialog();
                    }
                }
                return false;
            }
        });
        
    }
}
