
package com.reconinstruments.dashboard;

import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

/**
 * 
 * <code>StartActivityOverlay</code> pops up a overlay to ask for starting an activity.
 *
 */
public class StartActivityOverlay extends DialogFragment {

    private DashboardActivity mActivity;

    public StartActivityOverlay(DashboardActivity activity) {
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_activity_overlay, container);
        setupKeyListener();
        return view;
    }
    

    private void setupKeyListener() {
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                boolean actionDown = (event.getAction() == KeyEvent.ACTION_DOWN);
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && actionDown) {
                   mActivity.goLeft();
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && actionDown) {
                    mActivity.goRight();
                }else if (keyCode == KeyEvent.KEYCODE_BACK && actionDown) {
                    mActivity.onBackPressed();
                }else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && actionDown) {
                    mActivity.dismissStartActivityOverlay();
                }else if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) && actionDown) {
                    mActivity.doActivity();
                    mActivity.dismissStartActivityOverlay();
                }
                return true;
            }
        });
    }
}
