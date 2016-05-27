package com.reconinstruments.jetapplauncher.settings;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.DeviceUtils;

import java.lang.Override;

public class DiagnosticsActivity extends JetSettingsListActivity {

    private static final String TAG = "DiagnosticsActivity";
    private LinearLayout mLinearLayout;
    private TextView infoText;
    private SettingItem item = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(this, "Enable Diagnostics",
                SettingsUtil.ANALYTICS_ENABLED,
                SettingsUtil.ANALYTICS_ENABLED_DEFAULT == 1));

        setInfoText();

    }

    private void setInfoText() {
        mLinearLayout = (LinearLayout)findViewById(R.id.base_lin_layout);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mListView.getLayoutParams();
        lp.height = 145;//modify list layout height to accomodate text below
        mListView.setLayoutParams(lp);
        infoText = new TextView(getApplicationContext());
        infoText.setPadding(0, 0, 0, 12);
        String device = (DeviceUtils.isSun()) ? "JET" : "Snow2";
        infoText.setText(getResources().getString(R.string.diagnostics_text, device));
        infoText.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        infoText.setTextSize(22);
        infoText.setTextColor(Color.WHITE);
        mLinearLayout.addView(infoText);
    }

    @Override
    protected void settingsItemClicked(int position) {
        mSettingList.get(position).toggle(this);
        mListAdapter.notifyDataSetChanged();
    }

}
