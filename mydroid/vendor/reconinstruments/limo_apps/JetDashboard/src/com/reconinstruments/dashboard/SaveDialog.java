
package com.reconinstruments.dashboard;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.ReconJetDialog;

import java.util.List;

/**
 * <code>SaveDialog</code> provides two options, discard or save the activity
 * record. Once it's dismissed, the system would return back to the main screen.
 */
public class SaveDialog extends ReconJetDialog {

    private Activity mActivity;

    public SaveDialog(String title, List<Fragment> list, Activity activity) {
        super(title, list, -1, 220);
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
                    //make sure stopping the activity first.
                    MetricManager.getInstance(mActivity.getApplicationContext()).stopSportsActivity();
                    if (mPager.getCurrentItem() == 0) { // save the activity
                        MetricManager.getInstance(mActivity.getApplicationContext()).saveSportsActivity();
                    }else if (mPager.getCurrentItem() == 1) { // discard the activity
                        MetricManager.getInstance(mActivity.getApplicationContext()).discardSportsActivity();
                    }
                    startActivity(new Intent("com.reconinstruments.itemhost"));
                    getDialog().dismiss();
                    mActivity.finish();
                }else if(keyCode == KeyEvent.KEYCODE_BACK){ // if back button is presssed
                    getDialog().dismiss();
                }
                return false;
            }
        });
    }
}
