
package com.reconinstruments.jetapplauncher.settings;

import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.PopUpDialog;
import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.phone.PhoneLogProvider;

import java.util.List;

public class ClearNotificationsOverlay extends ReconJetDialog {
    private static final String TAG = ClearNotificationsOverlay.class.getSimpleName();

    private FragmentActivity mActivity;
    private PopUpDialog mFeedbackDialog;

    public ClearNotificationsOverlay(String title, String desc, List<Fragment> list, int layout, FragmentActivity activity) {
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
                    if (mPager.getCurrentItem() == 1) { // clear
                        // Delete all phone records from DB
                        int count = mActivity.getContentResolver().delete(
                                PhoneLogProvider.CONTENT_URI, null, null);
                        Log.d(TAG, "phone records deleted: " + count);

                        String select = null; // This deletes everything
                        count = mActivity.getContentResolver().delete(ReconMessageAPI.GROUPS_URI,
                                select, null);
                        Log.d(TAG, "phone messages deleted: " + count);
                        mFeedbackDialog = new PopUpDialog("Cleared", null, mActivity,
                                PopUpDialog.ICONTYPE.CHECKMARK).showDialog(PopUpDialog.DEFAULT_TIMEOUT, new Runnable() {
                            @Override
                            public void run() {
                                getDialog().dismiss();
                            }
                        });
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
