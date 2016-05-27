package com.reconinstruments.jetapplauncher.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.util.Log;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;

import java.lang.NullPointerException;
import java.lang.Override;

public class PasscodeActivity extends JetSettingsListActivity {

    private static final String TAG = "PasscodeActivity";

    private Context mContext;
    //new
    private LinearLayout mLinearLayout;
    private TextView lockJetInstruction;
    private SharedPreferences mPrefs;
    private SettingItem item = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        updateDisplay();
        mListAdapter.notifyDataSetChanged();
    }

    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(this, "Enable Passcode",
                SettingsUtil.PASSCODE_ENABLED, false));
        mSettingList.add(new SettingItem("Change Passcode"));


        setJetInstructionText();

    }

    private void setJetInstructionText() {
        mLinearLayout = (LinearLayout)findViewById(R.id.base_lin_layout);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mListView.getLayoutParams();
        lp.height = 180;//modify list layout height to accomodate text below
        mListView.setLayoutParams(lp);
        lockJetInstruction = new TextView(getApplicationContext());
        lockJetInstruction.setText("Lock JET from OPTIONS menu");
        lockJetInstruction.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        lockJetInstruction.setTextSize(22);
        lockJetInstruction.setTextColor(Color.WHITE);
        mLinearLayout.addView(lockJetInstruction);
        updateDisplay();

    }

    private void updateDisplay(){
        try {
            item = mListAdapter.getItem(0);
        }catch (NullPointerException np){
            Log.i(TAG, "List adapter not initialized");
        }
        if(isPasscodeEnabled()){
            lockJetInstruction.setVisibility(View.VISIBLE);
            if(mSettingList.size() == 1){
                mSettingList.add(new SettingItem("Change Passcode"));
            }
            if(item!= null){
                item.setIsChecked(true);
            }
        }else{
            lockJetInstruction.setVisibility(View.INVISIBLE);
            if(mSettingList.size() == 2){
                mSettingList.remove(1);
            }
            if(item!= null){
                item.setIsChecked(false);
            }
        }
    }

    @Override
    protected void settingsItemClicked(int position) {
        Intent intent = new Intent("com.reconinstruments.passcodelock.PASSCODE_MAIN");
        switch (position) {
            case 0:
                intent.putExtra("CALLING_ACTION", "ENABLE_PASSCODE");
                startActivity(intent);
                break;
            case 1:
                intent.putExtra("CALLING_ACTION", "CHANGE_PASSCODE");
                startActivity(intent);
                break;
        }
    }

    private boolean isPasscodeEnabled() {
        return (SettingsUtil.getCachableSystemIntOrSet(this,
                SettingsUtil.PASSCODE_ENABLED,
                0) == 1);
    }

}
