//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlivestats;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import com.reconinstruments.commonwidgets.CommonUtils;

/**
 * <code>DayPausedDialog</code> shows the <code>day_paused</code> layout using <code>Dialog</code>
 * 
 * @param activity an <code>Activity</code> value
 */
public class DayPausedDialog extends JetDialog {
    private int mSportType = -1;

    public DayPausedDialog(Context context, int layout) {
        super(context, layout);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                CannotResumeOverlay cannotResumeOverlay = CannotResumeOverlay
                        .showCannotResumeOverlayIfNeeded(mActivity);
                if (cannotResumeOverlay == null) {
                    ((JetLiveStatsActivity) mActivity).resumeActivitySequence();
                    this.dismiss();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                this.dismiss();
                Intent intent = new Intent(mActivity, DaySummaryActivity.class);
                intent.putExtra("ActivityType", mSportType);
                CommonUtils.launchNew(mActivity, intent, false);

                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return mActivity.onKeyDown(keyCode, event);
                // Note that this calls the go left go right of the column element

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        mActivity.onBackPressed();
    }

    public void setSportType(int sportType) {
        mSportType = sportType;
    }
}
