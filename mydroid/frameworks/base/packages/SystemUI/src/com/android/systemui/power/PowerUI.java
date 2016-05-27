/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.power;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Slog;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.PixelFormat;
import com.reconinstruments.os.hardware.power.HUDPowerManager;

import com.android.systemui.R;
import com.android.systemui.SystemUI;

import com.android.internal.policy.PolicyManager;

public class PowerUI extends SystemUI {
    static final String TAG = "PowerUI";

    static final boolean DEBUG = false;
    static final String ACTION_SHOW_BAD_SHUTDOWN = "com.reconinstruments.showIfBadShutdown";
    static final String ACTION_POWER_KEY = "RECON_POWER_KEY";

    Handler mHandler = new Handler();

    int mBatteryLevel = 100;
    int mBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
    int mPlugType = 0;
    int mInvalidCharger = 0;
    int mShutdownReason = HUDPowerManager.SHUTDOWN_GRACEFUL;
    boolean mPendingShowShutdownReason = false;

    int mLowBatteryAlertCloseLevel;
    int[] mLowBatteryReminderLevels = new int[1];

    AlertDialog mInvalidChargerDialog;
    View mLowBatteryView;
    View mBadShutdownReasonView;

    public void start() {

        mLowBatteryAlertCloseLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryCloseWarningLevel);
        mLowBatteryReminderLevels[0] = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        //mLowBatteryReminderLevels[1] = mContext.getResources().getInteger(
        //        com.android.internal.R.integer.config_criticalBatteryWarningLevel);

        // Register for Intent broadcasts for...
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_SHUTDOWN_REASON);
        filter.addAction(ACTION_SHOW_BAD_SHUTDOWN);

        if (!Build.MODEL.equalsIgnoreCase("JET")) {
            filter.addAction(ACTION_POWER_KEY);
        }
        mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);
    }

    /**
     * Buckets the battery level.
     *
     * The code in this function is a little weird because I couldn't comprehend
     * the bucket going up when the battery level was going down. --joeo
     *
     * 1 means that the battery is "ok"
     * 0 means that the battery is between "ok" and what we should warn about.
     * less than 0 means that the battery is low
     */
    private int findBatteryLevelBucket(int level) {
        if (level >= mLowBatteryAlertCloseLevel) {
            return 1;
        }
        if (level >= mLowBatteryReminderLevels[0]) {
            return 0;
        }
        final int N = mLowBatteryReminderLevels.length;
        for (int i=N-1; i>=0; i--) {
            if (level <= mLowBatteryReminderLevels[i]) {
                return -1-i;
            }
        }
        throw new RuntimeException("not possible!");
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SHUTDOWN_REASON)) {
                final int oldShutdownReason = mShutdownReason;
                mShutdownReason = intent.getIntExtra(HUDPowerManager.EXTRA_REASON, 0);

                if(DEBUG) {
                    Slog.d(TAG,"shutdownReason: " + oldShutdownReason + " --> " + mShutdownReason);
                }

                if(oldShutdownReason == HUDPowerManager.SHUTDOWN_GRACEFUL && mShutdownReason != oldShutdownReason) {
                    // Set flag to indicate we are suppose to show the bad shutdown reason dialog. Wait for
                    // another intent to know when to show it.
                    if (DEBUG) {
                        Slog.d(TAG, "Received shutdown info. Waiting for intent to show bad shutdown");
                    }
                    mPendingShowShutdownReason = true;
                } else if(oldShutdownReason != HUDPowerManager.SHUTDOWN_GRACEFUL && mShutdownReason == HUDPowerManager.SHUTDOWN_GRACEFUL) {
                    dismissBadShutdownReasonDialog();
                    return;
                }
            }
            else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                final int oldBatteryLevel = mBatteryLevel;
                mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                final int oldBatteryStatus = mBatteryStatus;
                mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                final int oldPlugType = mPlugType;
                mPlugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1);
                final int oldInvalidCharger = mInvalidCharger;
                mInvalidCharger = intent.getIntExtra(BatteryManager.EXTRA_INVALID_CHARGER, 0);

                final boolean plugged = mPlugType != 0;
                final boolean oldPlugged = oldPlugType != 0;

                int oldBucket = findBatteryLevelBucket(oldBatteryLevel);
                int bucket = findBatteryLevelBucket(mBatteryLevel);

                if (DEBUG) {
                    Slog.d(TAG, "buckets   ....." + mLowBatteryAlertCloseLevel
                            + " .. " + mLowBatteryReminderLevels[0]);
//                            + " .. " + mLowBatteryReminderLevels[1]);
                    Slog.d(TAG, "level          " + oldBatteryLevel + " --> " + mBatteryLevel);
                    Slog.d(TAG, "status         " + oldBatteryStatus + " --> " + mBatteryStatus);
                    Slog.d(TAG, "plugType       " + oldPlugType + " --> " + mPlugType);
                    Slog.d(TAG, "invalidCharger " + oldInvalidCharger + " --> " + mInvalidCharger);
                    Slog.d(TAG, "bucket         " + oldBucket + " --> " + bucket);
                    Slog.d(TAG, "plugged        " + oldPlugged + " --> " + plugged);
                }

                if(mBadShutdownReasonView != null) {
                    // Bad Shutdown Reason view is showing, don't show invalid charger or low battery
                    return;
                } else if (oldInvalidCharger == 0 && mInvalidCharger != 0) {
                    Slog.d(TAG, "showing invalid charger warning");
                    showInvalidChargerDialog();
                    return;
                } else if (oldInvalidCharger != 0 && mInvalidCharger == 0) {
                    dismissInvalidChargerDialog();
                } else if (mInvalidChargerDialog != null) {
                    // if invalid charger is showing, don't show low battery
                    return;
                }

                if (!plugged
                        && (bucket < oldBucket || oldPlugged)
                        && mBatteryStatus != BatteryManager.BATTERY_STATUS_UNKNOWN
                        && bucket < 0) {
                    showLowBatteryWarning();

                    // only play SFX when the dialog comes up or the bucket changes
                    if (bucket != oldBucket || oldPlugged) {
                        playLowBatterySound();
                    }
                } else if (plugged || (bucket > oldBucket && bucket > 0)) {
                    dismissLowBatteryWarning();
                } else if (mLowBatteryView != null) {
                    showLowBatteryWarning();
                }
            } else if (action.equals(ACTION_SHOW_BAD_SHUTDOWN) && mPendingShowShutdownReason) {
                mPendingShowShutdownReason = false;

                if (mShutdownReason == HUDPowerManager.SHUTDOWN_BATT_REMOVED) {
                    Slog.d(TAG, "showing bad shutdown reason warning : Battery Removed");
                    showBadShutdownReasonDialog(mContext.getString(R.string.battery_removed_usb_conn_title), mContext.getString(R.string.battery_removed_usb_conn_subtitle));
                } 
                return;
            } else if (action.equals(ACTION_POWER_KEY)) {
                dismissBadShutdownReasonDialog();
            }
            else {
                Slog.w(TAG, "unknown intent: " + intent);
            }
        }
    };

    void dismissLowBatteryWarning() {
        if (mLowBatteryView != null) {
            Slog.i(TAG, "closing low battery warning: level=" + mBatteryLevel);
            try {
                // Set the animation to fade out
                Animation a = AnimationUtils.loadAnimation(mContext, R.anim.fadeout_faster);
                RelativeLayout rl = (RelativeLayout)mLowBatteryView.findViewById(R.id.bottom_layout);
                rl.startAnimation(a);

                ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).removeView(mLowBatteryView);
            } finally {
                mLowBatteryView = null;
            }
        }
    }

    void showLowBatteryWarning() {
        Slog.i(TAG,
                ((mLowBatteryView == null) ? "showing" : "updating")
                + " low battery warning: level=" + mBatteryLevel
                + " [" + findBatteryLevelBucket(mBatteryLevel) + "]");

        if (mLowBatteryView == null) {
            WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
            Window win;
            win = PolicyManager.makeNewWindow(mContext);
            win.setWindowManager(wm, null, null);
            win.requestFeature(Window.FEATURE_NO_TITLE);
            win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // Set background to be transparent to make alpha work
            ColorDrawable cd = new ColorDrawable(0);
            win.setBackgroundDrawable(cd);
            win.setContentView(R.layout.recon_warning);
            mLowBatteryView = win.getDecorView();

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            lp.format = PixelFormat.RGBA_8888;

            // Set title
            TextView tv = (TextView)mLowBatteryView.findViewById(R.id.warning_title);
            tv.setText("LOW BATTERY");

            // Set the text
            tv = (TextView)mLowBatteryView.findViewById(R.id.warning_text);
            String build = "Snow2";
            if (Build.MODEL.equalsIgnoreCase("JET")) {
                build = "Jet";
            }
            String text = "Please connect " + build + " to a charger.<br />";
            tv.setText(Html.fromHtml(text));
            RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams)((View)tv).getLayoutParams();
            rl.setMargins(0, 30, 0, 0);

            // Set the click listener to dismiss the view
            Button b = (Button)mLowBatteryView.findViewById(R.id.ok_btn);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissLowBatteryWarning();
                }
            });

            // Check build model. If it's Jet, then change the button image to be Jet-specific
            if (Build.MODEL.equalsIgnoreCase("JET")) {
                b.setCompoundDrawablesWithIntrinsicBounds(R.drawable.jet_select, 0, 0, 0);
            }

            // Add a listener to determine whether a status bar is currently visible
            ViewTreeObserver o = mLowBatteryView.getViewTreeObserver();
            o.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = mLowBatteryView.getHeight();
                    ViewTreeObserver o = mLowBatteryView.getViewTreeObserver();
                    o.removeGlobalOnLayoutListener(this);

                    // Set animation to slide and fade in
                    Animation a = AnimationUtils.loadAnimation(mContext, R.anim.fade_slide_in_bottom);
                    RelativeLayout rl = (RelativeLayout)mLowBatteryView.findViewById(R.id.bottom_layout);
                    rl.startAnimation(a);

                    // If height of overlay is not 240, then status bar is showing
                    if (height != 240) {
                        Button b = (Button)mLowBatteryView.findViewById(R.id.ok_btn);
                        // There is a status bar, set padding of select button accordingly
                        b.setPaddingRelative(16, 0, 0, 6);
                    }
                }
            });

            wm.addView(mLowBatteryView, lp);
        }
    }

    void playLowBatterySound() {
        if (DEBUG) {
            Slog.i(TAG, "playing low battery sound. WOMP-WOMP!");
        }

        final ContentResolver cr = mContext.getContentResolver();
        if (Settings.System.getInt(cr, Settings.System.POWER_SOUNDS_ENABLED, 1) == 1) {
            final String soundPath = Settings.System.getString(cr,
                    Settings.System.LOW_BATTERY_SOUND);
            if (soundPath != null) {
                final Uri soundUri = Uri.parse("file://" + soundPath);
                if (soundUri != null) {
                    final Ringtone sfx = RingtoneManager.getRingtone(mContext, soundUri);
                    if (sfx != null) {
                        sfx.setStreamType(AudioManager.STREAM_SYSTEM);
                        sfx.play();
                    }
                }
            }
        }
    }

    void dismissInvalidChargerDialog() {
        if (mInvalidChargerDialog != null) {
            mInvalidChargerDialog.dismiss();
        }
    }

    void showInvalidChargerDialog() {
        Slog.d(TAG, "showing invalid charger dialog");

        dismissLowBatteryWarning();

        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
            b.setCancelable(true);
            b.setMessage(R.string.invalid_charger);
            b.setIconAttribute(android.R.attr.alertDialogIcon);
            b.setPositiveButton(android.R.string.ok, null);

        AlertDialog d = b.create();
            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        mInvalidChargerDialog = null;
                        mLowBatteryView = null;
                    }
                });

        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        d.show();
        mInvalidChargerDialog = d;
    }

    void dismissBadShutdownReasonDialog() {
        if (mBadShutdownReasonView != null) {
            try {
                ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).removeView(mBadShutdownReasonView);
            } finally {
                mBadShutdownReasonView = null;
            }
        }
    }

    void showBadShutdownReasonDialog(String title, String subtitle) {
        // Higher Priority Dialog
        dismissLowBatteryWarning();
        dismissInvalidChargerDialog();

        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Window win;
        win = PolicyManager.makeNewWindow(mContext);
        win.setWindowManager(wm, null, null);
        win.requestFeature(Window.FEATURE_NO_TITLE);
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set background to be transparent to make alpha work
        ColorDrawable cd = new ColorDrawable(0);
        win.setBackgroundDrawable(cd);
        win.setContentView(R.layout.recon_warning);
        mBadShutdownReasonView = win.getDecorView();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        lp.format = PixelFormat.RGBA_8888;

        // Set title
        TextView tv = (TextView)mBadShutdownReasonView.findViewById(R.id.warning_title);
        tv.setText(title);

        // Set the text
        tv = (TextView)mBadShutdownReasonView.findViewById(R.id.warning_text);
        // Check to see if it is warning text
        if (title.equalsIgnoreCase("WARNING")) {
            // Retrieve the first index of newline
            int index = subtitle.indexOf("\n");
            if (index != -1) {
                // For the WARNING dialog, the design is such that the leading between the first line
                // and the second line is greater than normal leading. To do this, use SpannableString
                // and increase the font size of the "\n" to increase the leading.
                SpannableString ss = new SpannableString(subtitle);
                ss.setSpan(new RelativeSizeSpan(2.25f), index, index+1, 0);
                tv.setText(ss);

                // Using SpannableString messes up the spacing, so reset the TextView's positioning
                RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams)((View)tv).getLayoutParams();
                rl.addRule(RelativeLayout.BELOW, 0);
                rl.setMargins(0, 34, 0, 0);
            }
        } else {
            RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams)((View)tv).getLayoutParams();
            rl.addRule(RelativeLayout.CENTER_IN_PARENT);
            tv.setText(subtitle);
        }

        // Set the click listener to dismiss the view
        Button b = (Button)mBadShutdownReasonView.findViewById(R.id.ok_btn);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set animation to fade out
                Animation a = AnimationUtils.loadAnimation(mContext, R.anim.fadeout_faster);
                RelativeLayout rl = (RelativeLayout)mBadShutdownReasonView.findViewById(R.id.bottom_layout);
                rl.startAnimation(a);

                dismissBadShutdownReasonDialog();
            }

        });

        // Check build model. If it's Jet, then change the button image to be Jet-specific
        if (Build.MODEL.equalsIgnoreCase("JET")) {
            b.setCompoundDrawablesWithIntrinsicBounds(R.drawable.jet_select, 0, 0, 0);
        }
        b.setPressed(true);
        b.setPaddingRelative(20, 0, 0, 2);

        // Add a listener to determine whether a status bar is currently visible
        ViewTreeObserver o = mBadShutdownReasonView.getViewTreeObserver();
        o.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = mBadShutdownReasonView.getHeight();
                ViewTreeObserver o = mBadShutdownReasonView.getViewTreeObserver();
                o.removeGlobalOnLayoutListener(this);

                // Set animation to slide and fade in
                Animation a = AnimationUtils.loadAnimation(mContext, R.anim.fade_slide_in_bottom);
                RelativeLayout rl = (RelativeLayout)mBadShutdownReasonView.findViewById(R.id.bottom_layout);
                rl.startAnimation(a);
            }
        });

        wm.addView(mBadShutdownReasonView, lp);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("mLowBatteryAlertCloseLevel=");
        pw.println(mLowBatteryAlertCloseLevel);
        pw.print("mLowBatteryReminderLevels=");
        pw.println(Arrays.toString(mLowBatteryReminderLevels));
        pw.print("mInvalidChargerDialog=");
        pw.println(mInvalidChargerDialog == null ? "null" : mInvalidChargerDialog.toString());
        pw.print("mLowBatteryView=");
        pw.println(mLowBatteryView == null ? "null" : mLowBatteryView.toString());
        pw.print("mBatteryLevel=");
        pw.println(Integer.toString(mBatteryLevel));
        pw.print("mBatteryStatus=");
        pw.println(Integer.toString(mBatteryStatus));
        pw.print("mPlugType=");
        pw.println(Integer.toString(mPlugType));
        pw.print("mInvalidCharger=");
        pw.println(Integer.toString(mInvalidCharger));
        pw.print("bucket: ");
        pw.println(Integer.toString(findBatteryLevelBucket(mBatteryLevel)));
    }
}

