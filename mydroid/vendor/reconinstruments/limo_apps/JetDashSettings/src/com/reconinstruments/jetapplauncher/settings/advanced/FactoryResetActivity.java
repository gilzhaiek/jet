package com.reconinstruments.jetapplauncher.settings.advanced;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.platform.UBootEnvNative;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.settings.SettingAdapter;
import com.reconinstruments.jetapplauncher.settings.SettingButtonAdapter;
import com.reconinstruments.jetapplauncher.settings.SettingItem;
import com.reconinstruments.jetapplauncher.settings.TimeZoneActivity;
import com.reconinstruments.jetapplauncher.settings.Util;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.commonwidgets.TwoOptionsJumpFixer;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.installer.FirmwareUtils;

public class FactoryResetActivity extends ListActivity {
    public static final String TAG = "FactoryResetActivity";
    private TwoOptionsJumpFixer twoOptionsJumpFixer;

    private AlertDialog lowBatterDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.setting_layout);

        ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
        headerIcon.setVisibility(View.GONE);
        TextView title = (TextView) findViewById(R.id.setting_title);
        title.setText("FACTORY RESET");

        LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
        desc_layout.setVisibility(View.VISIBLE);
        TextView desc = (TextView) findViewById(R.id.setting_desc_text);
        desc.setText("Firmware will be reset to the factory default.");


        /*
         * Low Battery Alert Dialog Initialisation  
         */
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("WARNING");
        alertDialogBuilder
        .setMessage("Battery is too low - please connect to charger and the system will notify you when battery is sufficient to perform a recovery")
        .setCancelable(false)
        .setPositiveButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                finish();
            }
        });
        lowBatterDialog = alertDialogBuilder.create();


        ArrayList<SettingItem> factoryResetList = new ArrayList<SettingItem>();

        factoryResetList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "RESET" ));
        factoryResetList.add(new SettingItem(new Intent(this, TimeZoneActivity.class), "CANCEL" ));

        setListAdapter(new SettingButtonAdapter(this, factoryResetList));
        twoOptionsJumpFixer = new TwoOptionsJumpFixer(getListView());
        twoOptionsJumpFixer.start();

        this.getListView().setOnItemClickListener(new OnItemClickListener(){
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(twoOptionsJumpFixer != null){
                    twoOptionsJumpFixer.stop();
                }
                if (position == 0){

                    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryStatus = registerReceiver(null, ifilter);

                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                    if (level < 0 || scale < 0) { // Something is wrong, battery level not detected. Failsafe
                        return;
                    }
                    float batteryPct = level / (float)scale;

                    if (batteryPct < 0.25 && lowBatterDialog != null) {
                        lowBatterDialog.show();
                        return;
                    }

                    PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);


                    OutputStream out;
                    try {
                        out = new FileOutputStream(FirmwareUtils.JET_COMMAND_RECOVERY);
                        out.write("--wipe_data\n--wipe_cache".getBytes());
                        out.close();
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                        return;
                    } catch (IOException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                        return;
                    }

                    Log.d (TAG,"rebooting the device");

                    pm.reboot("recovery");
                }
                else{
                    finish();	
                }				
            }});

    }

}
