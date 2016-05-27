
package com.reconinstruments.dashboard;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

/**
 * <code>PauseDialog</code> is shown after the user pauses the sport activity,
 * there are three options to be provided: back to main screen, resume and
 * finish.
 */
public class PauseDialog extends DialogFragment {

    private DashboardActivity mActivity;

    public PauseDialog(DashboardActivity activity) {
        mActivity = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Window window = getDialog().getWindow();
        LayoutParams params = window.getAttributes();
        params.alpha = 0.8f;
        window.setAttributes((android.view.WindowManager.LayoutParams) params);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pause_dialog, container);
        setupKeyListener();
        return view;
    }

    private void setupKeyListener() {
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                    }
                    MetricManager.getInstance(mActivity.getApplicationContext())
                            .resumeSportsActivity();
                    mActivity.resumeActivity();
                    getDialog().dismiss();
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (event.getAction() != KeyEvent.ACTION_UP) {
                        return true;
                    }
                    Intent intent = new Intent(mActivity, ActivitySummaryActivity.class);
                    intent.putExtra(MetricManager.EXTRA_SUMMARY, MetricManager.getInstance(mActivity.getApplicationContext()).getLatestData());
                    startActivity(intent);
                    getDialog().dismiss();
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    mActivity.goLeft();
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    mActivity.goRight();
                } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    Intent intent = new Intent("com.reconinstruments.itemhost");
                    startActivity(intent);
                    getDialog().dismiss();
                }
                return false;
            }
        });
    }

}
