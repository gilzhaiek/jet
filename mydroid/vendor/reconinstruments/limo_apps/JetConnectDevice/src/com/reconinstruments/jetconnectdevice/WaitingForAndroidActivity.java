
package com.reconinstruments.jetconnectdevice;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.utils.BTHelper;

/**
 * <code>WaitingForAndroidActivity</code> enables the bluetooth discoverability and listen for HUDService state changed event and takes proper actions.
 */
public class WaitingForAndroidActivity extends WaitingForPhoneActivity {

    private static final String TAG = WaitingForAndroidActivity.class.getSimpleName();
    private static final String RECON_YELLOW_TEXT = ConnectSmartphoneActivity.RECON_YELLOW_TEXT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitleTV.setText(R.string.title_waiting_device_android);
        mTitleTV.setTextSize(29);
        mBody1TV.setText
        (Html
                .fromHtml(
                        "Login to <font color=\"" + RECON_YELLOW_TEXT + "\">Recon Engage</font> on your Android, then tap&nbsp&nbsp&nbsp<img src=\"phone_icon.png\" align=\"middle\">&nbsp  and follow the prompts. Select <font color=\"" + RECON_YELLOW_TEXT + "\">" + BTHelper.getInstance(getApplicationContext()).getJetName() + "</font>.",
                        new ImageGetter() {
                            @Override
                            public Drawable getDrawable(String source) {
                                int id;
                                if (source
                                        .equals("phone_icon.png")) {
                                    id = R.drawable.phone_icon;
                                } else {
                                    return null;
                                }
                                LevelListDrawable d = new LevelListDrawable();
                                Drawable empty = getResources()
                                        .getDrawable(id);
                                d.addLevel(0, 0, empty);
                                d.setBounds(0, 0,
                                        empty.getIntrinsicWidth(),
                                        empty.getIntrinsicHeight());
                                return d;
                            }
                        }, null));
        mBody1TV.setTextSize(23);
        mBody2TV.setText(getString(R.string.desc_waiting_device_android2));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHUDServiceDisconnected() {
    }

    @Override
    protected void onHUDServiceConnecting() {
    }

    @Override
    protected void onHUDServiceConnected() {
    }
}
