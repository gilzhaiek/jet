package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.reconinstruments.utils.installer.FirmwareUtils;

public class FactoryResetOverlay extends ReconJetDialog {
    private static final String TAG = FactoryResetOverlay.class.getSimpleName();

    private Activity mActivity;

    public FactoryResetOverlay(String title, String desc, List<Fragment> list, int layout, Activity activity) {
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
                        PowerManager pm = (PowerManager) mActivity.getBaseContext().getSystemService(Context.POWER_SERVICE);
                        OutputStream out;
                        try {
                            out = new FileOutputStream(FirmwareUtils.JET_COMMAND_RECOVERY);
                            out.write("--wipe_data\n--wipe_cache\n--update_package=/factory/images/factory.bin".getBytes());
                            out.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d (TAG,"rebooting the device");
                        pm.reboot("recovery");
                    }
                    getDialog().dismiss();
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
