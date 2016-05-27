//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.jetapplauncher.settings;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.installer.FirmwareUtils;

public class UpdateActivity extends ListActivity {
    static private final String TAG = "UpdateActivity";

    private ArrayList<SettingItem> updateList; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setting_layout);

        ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
        headerIcon.setBackgroundResource(R.drawable.software_update_white);
        TextView title = (TextView) findViewById(R.id.setting_title);
        title.setText("SOFTWARE UPDATE");

        LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
        desc_layout.setVisibility(View.VISIBLE);
        TextView desc = (TextView) findViewById(R.id.setting_desc_text);

        updateList = new ArrayList<SettingItem>();

        SettingItem item = new SettingItem("UPDATE");

        if (FirmwareUtils.JET_UPDATE_BIN_CACHE.isFile() && FirmwareUtils.JET_UPDATE_BIN_CACHE.exists()
            && FirmwareUtils.JET_COMMAND_RECOVERY_BAK.isFile() && FirmwareUtils.JET_COMMAND_RECOVERY_BAK.exists()) {
            desc.setText("Software Update Available");
            item.titleAlpha = 255;
            updateList.add(item);
                                
        }
        else{
            desc.setText("No Update Files");
        }

        item = new SettingItem("CANCEL");
        updateList.add(item);

        setListAdapter(new SettingButtonAdapter(this, updateList));

        this.getListView().setOnItemClickListener(updateListener);

        /*
         * Low Battery Alert Dialog Initialisation  
         */
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("WARNING");
        alertDialogBuilder
            .setMessage("Battery Too Low For Update")
            .setCancelable(false)
            .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        finish();
                    }
                });
        lowBatterDialog = alertDialogBuilder.create();

    }

    private AlertDialog lowBatterDialog = null;

    private OnItemClickListener updateListener = new OnItemClickListener(){

            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                if (updateList.get(position).title.equalsIgnoreCase("Update")){

                    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryStatus = registerReceiver(null, ifilter);

                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                    if (level < 0 || scale < 0) { // Something is wrong, battery level not detected. Failsafe
                        return;
                    }
                    float batteryPct = level / (float)scale;

                    // TODO: use BatteryUtil
                    if (batteryPct < 0.25 && lowBatterDialog != null) {
                        lowBatterDialog.show();
                        return;
                    }

                    if (FirmwareUtils.JET_UPDATE_BIN_CACHE.isFile() && FirmwareUtils.JET_UPDATE_BIN_CACHE.exists()
                        && FirmwareUtils.JET_COMMAND_RECOVERY_BAK.isFile() && FirmwareUtils.JET_COMMAND_RECOVERY_BAK.exists()) {
                        Log.d(TAG, "JET UPGRADING FIRMWARE : /cache/update.bin file exists start upgrade");
                        FirmwareUtils.doJetFirmwareUpgrade(UpdateActivity.this);
                    }
                }
                else{
                    finish();
                }

            }

        };

}
