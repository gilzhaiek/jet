package com.reconinstruments.jetapplauncher.settings;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.jetapplauncher.R;

import android.os.Bundle;
import com.reconinstruments.utils.SettingsUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AntBleSwitchActivity extends CarouselItemHostActivity {

    private static final String TAG = AntBleSwitchActivity.class.getSimpleName();
    private static final File RADIO_CONFIG_FILE = new File("/data/misc/recon/BT.conf");
    private static final String RADIO_MODE_BLE = "mode=BLE";
    private static final String RADIO_MODE_ANT = "mode=ANT";
    private Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_antbleswitch_layout);
        initPager();

        int chosenSetting = (SettingsUtil.getBleOrAnt() == SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) ? 0 : 1;
        mPager.setCurrentItem(chosenSetting);
        mPager.setPadding(0,0,0,0);
        mPager.setPageMargin(-201);
	}

    @Override
    protected List<Fragment> getFragments(){
        ArrayList<Fragment> fList = new ArrayList<Fragment>();
        fList.add(new AntBleSwitchFragment(R.layout.setting_antbleswitch_item, "ANT+", 0));
        fList.add(new AntBleSwitchFragment(R.layout.setting_antbleswitch_item, "Bluetooth LE", 1));
        return fList;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent){
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if(mPager.getCurrentItem() == 0){
                    // switch to ANT
                    changeRadio(false);
                    showCheckmark(0);
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result",0);
                    AntBleSwitchActivity.this.setResult(1,returnIntent);
                    AntBleSwitchActivity.this.finish();
                }
                else {
                    // switch to BLE
                    changeRadio(true);
                    showCheckmark(1);
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result",1);
                    AntBleSwitchActivity.this.setResult(1,returnIntent);
                    AntBleSwitchActivity.this.finish();
                }
            return true;
        }
        return super.onKeyUp(keyCode, keyEvent);
    }

	@Override
	public void onBackPressed() {
		super.onBackPressed();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result",(SettingsUtil.getBleOrAnt() == SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) ? 0 : 1);
        setResult(1,returnIntent);
        overridePendingTransition(R.anim.fadeout_faster, 0);
	}

    private void changeRadio(final boolean isBLE) {
        if (isBLE) { // BLE
            // WRITE BLE TO THE CONFIG FILE
            Log.d(TAG, "writing BLE to config file");
            writeToConfig(true);
        } else { // ANT
            // WRITE ANT TO THE CONFIG FILE
            Log.d(TAG, "writing ANT to config file");
            writeToConfig(false);
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isBLE) {
                    // Send intent to disconnect all ANT+ sensors
                    Intent intent = new Intent("RECON_ANT_SERVICE");
                    intent.putExtra("disconnect_all", true);
                    startService(intent);
                    //stop ANT service
                    stopService(new Intent("RECON_ANT_SERVICE"));

                    // Start the BLEService
                    startService(new Intent("RECON_THE_BLE_SERVICE"));
                } else {
                    // Stop the BLEService
                    stopService(new Intent("RECON_THE_BLE_SERVICE"));

                    // Send intent to reconnect all ANT+ sensors
                    startService(new Intent("RECON_ANT_SERVICE"));
                }
            }
        }, 300);
    }

    private static final synchronized void writeToConfig(boolean isBLE) {
        try {
            FileWriter writer = new FileWriter(RADIO_CONFIG_FILE, false);
            writer.write(isBLE ? RADIO_MODE_BLE : RADIO_MODE_ANT);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showCheckmark(int item){
        CarouselItemPageAdapter adapter = (CarouselItemPageAdapter) mPager.getAdapter();
        ((AntBleSwitchFragment) adapter.getItem(item)).showCheckmark();
        ((AntBleSwitchFragment) adapter.getItem((item == 0) ? 1 : 0)).hideCheckmark();
    }
}
