
package com.reconinstruments.jetsensorconnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.DataSource;
import com.dsi.ant.plugins.antplus.pccbase.AsyncScanController.AsyncScanResultDeviceInfo;

import com.reconinstruments.antplus.AntPlusManager;
import com.reconinstruments.antplus.AntPlusProfileManager;
import com.reconinstruments.antplus.AntPlusSensor;
import com.reconinstruments.antplus.AntService;
import com.reconinstruments.antplus.BCManager;
import com.reconinstruments.antplus.BSCManager;
import com.reconinstruments.antplus.BSManager;
import com.reconinstruments.antplus.HRManager;
import com.reconinstruments.antplus.BWManager;
import com.reconinstruments.antplus.PowerSensor;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.utils.SettingsUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog.ICONTYPE;

/**
 * <code>SensorConnectActivity</code> show the options list and device list. It
 * receives the connection state changed event and update the state on the
 * screen.
 */
public class SensorConnectActivity extends FragmentActivity {

    private static final String TAG = SensorConnectActivity.class.getSimpleName();

    protected ArrayList<SensorItem> mOptionList = new ArrayList<SensorItem>();
    protected ArrayList<SensorItem> mDeviceList = new ArrayList<SensorItem>();
    protected SettingMainAdapter mOptionListAdapter;
    public SettingMainAdapter mDeviceListAdapter;
    protected ListView mOptionListView;
    protected ListView mDeviceListView;
    private CountDownTimer failedTimer;
    protected AntPlusManager antPlusManager;
    private int mPowervalue = BWManager.INVALID_POWER;
    private int mHRValue = HRManager.INVALID_HEARTRATE;
    private int mCadenceValue = BCManager.INVALID_CADENCE;
    private int mSpeedValue = BSManager.INVALID_SPEED;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.sensor_list_layout);
        
        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT != SettingsUtil.getBleOrAnt()) { // ask user switch to Ant+ mode
            startActivityForResult(new Intent("com.reconinstruments.jetappsettings.antbleswitch"), 1);
        }
        
        mOptionListView = (ListView) findViewById(R.id.option_list);
        mDeviceListView = (ListView) findViewById(R.id.device_list);

        mOptionList.add(new SensorItem("Add ANT+ Sensor", null));

        mOptionListAdapter = new SettingMainAdapter(this, mOptionList, true);

        mOptionListView.setAdapter(mOptionListAdapter);

        mOptionListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    showAddSensorOverlay();
                } else if (arg2 == 1) {
                    startActivityForResult(new Intent("com.reconinstruments.jetappsettings.antbleswitch") ,1 );
                }
            }
        });

        antPlusManager = AntPlusManager.getInstance(this.getApplicationContext());
        mDeviceListAdapter = new SettingMainAdapter(this, mDeviceList, false);

        mDeviceListView.setAdapter(mDeviceListAdapter);

        mDeviceListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                SensorItem item = mDeviceListAdapter.getItem(arg2);
                AntPlusSensor device = antPlusManager.getDevice(item.sensor.getProfile(), item.sensor.getDeviceNumber());

                if (device == null)
                    return;

                showSensorOptionsOverlay(device, arg2);
            }
        });

        TextView emptyTV = (TextView) findViewById(android.R.id.empty);
        mDeviceListView.setEmptyView(emptyTV);

        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT == SettingsUtil.getBleOrAnt()) {
            emptyTV.setText("No ANT+ devices paired");
        } else {
            emptyTV.setText("No BLE devices paired");
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BWManager.ACTION_BIKE_POWER);
        filter.addAction(HRManager.ACTION_HEART_RATE);
        filter.addAction(BCManager.ACTION_BIKE_CADENCE);
        filter.addAction(BSManager.ACTION_BIKE_SPEED);
        filter.addAction(BSCManager.ACTION_SPEED_CADENCE);
        registerReceiver(metricsReceiver, filter);

        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT == SettingsUtil.getBleOrAnt()) {
            Intent intent = new Intent("RECON_ANT_SERVICE");
            intent.putExtra(AntService.EXTRA_ANT_SERVICE_START_ATTEMPT_CONNECT, true);
            startService(intent); 
        }
    }
    
    @Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
        super.onActivityResult(requestCode, resultCode, data);  
        if(requestCode==1)  {
            int result = data.getIntExtra("result", -1);
            if(result != 0) {// not in ant+ mode
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        reloadSensors();
    }
    
    /**
     * receieves metrics receiver
     */
    
    private final BroadcastReceiver metricsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int connState = intent.getIntExtra("event", -1);
            if(connState == 0) reloadSensors();
            if(action == BWManager.ACTION_BIKE_POWER){ // power metrics
                mPowervalue = intent.getIntExtra(BWManager.EXTRA_BIKE_POWER_VALUE, BWManager.INVALID_POWER);
            }else if(action == HRManager.ACTION_HEART_RATE){
                mHRValue = intent.getIntExtra(HRManager.EXTRA_HEART_RATE_VALUE, HRManager.INVALID_HEARTRATE);
            }else if(action == BCManager.ACTION_BIKE_CADENCE){
                mCadenceValue = intent.getIntExtra(BCManager.EXTRA_BIKE_CADENCE_VALUE, BCManager.INVALID_CADENCE);
            }else if(action == BSManager.ACTION_BIKE_SPEED){
                mSpeedValue = intent.getIntExtra(BSManager.EXTRA_BIKE_SPEED_VALUE, BSManager.INVALID_SPEED);
            }
            for (SensorItem item : mDeviceListAdapter.items) {
                if(item.sensor.isConnected()){
                    if(item.sensor.getProfile() == AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE){ 
                        if(!item.combinedCadence && !item.combinedSpeed){ // power item
                            item.subTitle = (mPowervalue != BWManager.INVALID_POWER) ? mPowervalue + " W" : "disconnected";
                            if(((PowerSensor)item.sensor).isCTF()) item.subTitle = (mPowervalue != BWManager.INVALID_POWER) ? mPowervalue + " W" : "---";
                            else item.subTitle = (mPowervalue != BWManager.INVALID_POWER) ? mPowervalue + " W" : "disconnected";
                        }else if(item.combinedSpeed){
                            item.subTitle = (mSpeedValue != BSManager.INVALID_SPEED) ? mSpeedValue + " KM/H" : "---";
                        }else if(item.combinedCadence){
                            if(((PowerSensor)item.sensor).getPowerSource() == DataSource.INVALID_CTF_CAL_REQ.ordinal()) item.subTitle = "---";
                            else item.subTitle = (mCadenceValue != BCManager.INVALID_CADENCE) ? mCadenceValue + " RPM" : "---";
                        }
                    }else if(item.sensor.getProfile() == AntPlusProfileManager.EXTRA_HEART_RATE_PROFILE){
                        item.subTitle = (mHRValue != HRManager.INVALID_HEARTRATE) ? mHRValue + " BPM" : "disconnected";
                    }else if(item.sensor.getProfile() == AntPlusProfileManager.EXTRA_BIKE_CADENCE_PROFILE){
                        item.subTitle = (mCadenceValue != BCManager.INVALID_CADENCE) ? mCadenceValue + " RPM" : "disconnected";
                    }else if(item.sensor.getProfile() == AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE){
                        item.subTitle = (mSpeedValue != BSManager.INVALID_SPEED) ? mSpeedValue + " KM/H" : "disconnected";
                    }else if(item.sensor.getProfile() == AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE){
                        item.subTitle = (mSpeedValue != BSManager.INVALID_SPEED && mCadenceValue != BCManager.INVALID_CADENCE) ? mSpeedValue + " KM/H " + mCadenceValue + " RPM" : "disconnected";
                    }
                }else{
                    item.subTitle = "disconnected";
                }
            }
            mDeviceListAdapter.notifyDataSetChanged();
        }
    };

    public void reloadSensors(){
        mDeviceListAdapter.clear();

        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT == SettingsUtil.getBleOrAnt()) { // load ant+ device
            for(AntPlusSensor sensor : antPlusManager.getDevicesWithPriority()){
                if (sensor.isConnected()) {
                    if(sensor.getProfile() == AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE){
                        mDeviceListAdapter.add(new SensorItem(sensor.convertedDisplayName(), "connected", sensor)); // power item
                        if(((PowerSensor)sensor).hasCombinedSpeed() && ((PowerSensor)sensor).isSpeedEnabled()){
                            mDeviceListAdapter.add(new SensorItem(((PowerSensor)sensor).convertedPowerSpeedName(), "connected", sensor, true, false)); // sub speed item
                        }
                        if(((PowerSensor)sensor).hasCombinedCadence() && ((PowerSensor)sensor).isCadenceEnabled()){
                            mDeviceListAdapter.add(new SensorItem(((PowerSensor)sensor).convertedPowerCadenceName(), "connected", sensor, false, true)); // sub cadence item
                        }
                    }else{
                        mDeviceListAdapter.add(new SensorItem(sensor.convertedDisplayName(), "connected", sensor));
                    }
                } else {
                    mDeviceListAdapter.add(new SensorItem(sensor.convertedDisplayName(), "disconnected", sensor));
                }
            }
            mDeviceListAdapter.notifyDataSetChanged();
        }

        TextView emptyTV = (TextView) findViewById(android.R.id.empty);
        mDeviceListView.setEmptyView(emptyTV);
        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT == SettingsUtil.getBleOrAnt()) {
            emptyTV.setText("No devices paired");
        } else {
            emptyTV.setText("No devices paired");
        }
        mOptionListView.setAdapter(mOptionListAdapter);
    }
    
    @Override
    public void onBackPressed(){
        CommonUtils.launchPrevious(this,null,true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT == SettingsUtil.getBleOrAnt()) {
            //disconnect all when it's finished to save battery
            Intent intent = new Intent("RECON_ANT_SERVICE");
            intent.putExtra(AntService.EXTRA_ANT_SERVICE_DISCONNECT_ALL, true);
            startService(intent); 
        }
        antPlusManager.closeScanController();
        if(failedTimer != null){
            failedTimer.cancel();
            failedTimer = null;
        }
        try {
            this.unregisterReceiver(metricsReceiver);
        } catch (IllegalArgumentException e) {
            // ignore it
        }
    }

    public void showSensorOptionsOverlay(AntPlusSensor device, int pos) {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        int i = 0;
        SparseArray<String> actionArray = new SparseArray<String>();
        if (device.getProfile() == AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE) {
            if(((PowerSensor)device).canCalibrate()){
                list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Calibrate", 0, i));
                actionArray.put(i, "Calibrate");
                i++;
            }
            if(((PowerSensor)device).hasCombinedSpeed()){
                list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Wheel Size", 0, i));
                actionArray.put(i, "Wheel Size");
                i++;
            }
        }else if (device.getProfile() == AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE || device.getProfile() == AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE){
            list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Wheel Size", 0, i));
            actionArray.put(i, "Wheel Size");
            i++;
        }
        if (!antPlusManager.isConnected(device)) {
            list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Connect", 0, i));
            actionArray.put(i, "Connect");
            i++;
        } else {
            list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Disconnect", 0, i));
            actionArray.put(i, "Disconnect");
            i++;
        }
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Forget", 0, i));
        actionArray.put(i, "Forget");
        i++;
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("sensor_options");
        if (frg == null) {
            SensorOptionsOverlay overlay = new SensorOptionsOverlay(device, list, R.layout.sensor_options_layout, this, pos, actionArray);
            overlay.setItemWidth(500); // custom the item width
            overlay.show(fm.beginTransaction(), "sensor_options");
        }
    }

    public void dismissSensorOptionsOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("sensor_options");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }

    public void showAddSensorOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();

        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "HR Monitor", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Cadence", 0, 1));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Speed", 0, 2));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Speed/Cad", 0, 3));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Power", 0, 4));

        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("add_sensor");
        if (frg == null) {
            AddSensorOverlay overlay = new AddSensorOverlay("ADD ANT+ SENSOR", list, R.layout.sensor_options_layout, this);
            overlay.show(fm.beginTransaction(), "add_sensor");
        }
    }

    public void dismissAddSensorOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("add_sensor");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }

    public void dismissConfirmationOverlay(int seconds){
        if(seconds == 0){
            FeedbackDialog.dismissDialog(this);
        }else{
            if(failedTimer != null){
                failedTimer.cancel();
                failedTimer = null;
            }
            failedTimer = new CountDownTimer(2 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    FeedbackDialog.dismissDialog(SensorConnectActivity.this);
                }
            };
            failedTimer.start();
        }
    }
    
    public void showSensorListOverlay(final int profile) {
        final android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        final Fragment frg = fm.findFragmentByTag("sensor_list");
        if (frg == null) {
            if(profile == AntPlusProfileManager.EXTRA_HEART_RATE_PROFILE){
                FeedbackDialog.showDialog(this, "Scanning", "Ensure your Heart Rate Monitor is strapped on.", null, FeedbackDialog.SHOW_SPINNER, true);
            }else{
                FeedbackDialog.showDialog(this, "Scanning", "Pedal to activate your sensor.", null, FeedbackDialog.SHOW_SPINNER, true);
            }
            int scanningTimeout = 5;
            antPlusManager.requestScanning(profile);
            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    dismissConfirmationOverlay(0);
                    if (!antPlusManager.hasSensors()) {
                        FeedbackDialog.showDialog(SensorConnectActivity.this, "No Sensor Found", null, FeedbackDialog.ICONTYPE.WARNING, FeedbackDialog.HIDE_SPINNER);
                        dismissConfirmationOverlay(2);
                    } else {
                        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
                        int i = 0;
                        for (AsyncScanResultDeviceInfo device : antPlusManager.getScannedDevices()) {
                            if(profile == AntPlusProfileManager.EXTRA_HEART_RATE_PROFILE){
                                list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "HR Monitor " + device.getAntDeviceNumber(), 0, i));
                            }else if(profile == AntPlusProfileManager.EXTRA_BIKE_CADENCE_PROFILE){
                                list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Cadence " + device.getAntDeviceNumber(), 0, i));
                            }else if(profile == AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE){
                                list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Speed " + device.getAntDeviceNumber(), 0, i));
                            }else if(profile == AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE){
                                list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Speed/Cad " + device.getAntDeviceNumber(), 0, i));
                            }else if(profile == AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE){
                                list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Power " + device.getAntDeviceNumber(), 0, i));
                            }
                            i++;
                        }
                        SensorListOverlay overlay = null;
                        if (antPlusManager.getScannedDevices().size() == 1) {
                            overlay = new SensorListOverlay(antPlusManager.getScannedDevices().size() + " DEVICE FOUND", list, R.layout.sensor_options_layout, SensorConnectActivity.this, profile);
                        } else {
                            overlay = new SensorListOverlay(antPlusManager.getScannedDevices().size() + " DEVICES FOUND", list, R.layout.sensor_options_layout, SensorConnectActivity.this, profile);
                        }
                        overlay.setItemWidth(600);
                        overlay.show(fm.beginTransaction(), "sensor_list");
                    }
                    antPlusManager.closeScanController();
                }
            }, scanningTimeout * 1000);
        }
    }

    public void dismissSensorListOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("sensor_list");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }

    public void showCircumferenceOverlay(AntPlusSensor device, boolean singleAction) {
        
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("circumference");
        if (frg == null) {
            WheelSizeOverlay overlay = new WheelSizeOverlay(this, device, singleAction);
            overlay.show(fm.beginTransaction(), "circumference");
        }
    }

    public void dismissCircumferenceOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("circumference");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }

    protected class SettingMainAdapter extends ArrayAdapter<SensorItem> {

        Context context = null;
        ArrayList<SensorItem> items = new ArrayList<SensorItem>();
        boolean resetPosition = false;

        public SettingMainAdapter(Context context, ArrayList<SensorItem> items, boolean resetPosition) {
            super(context, R.layout.sensor_list_item, items);
            this.context = context;
            this.items = items;
            this.resetPosition = resetPosition;
        }

        @Override
        public SensorItem getItem(int position) {
            return items.get(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.sensor_list_item, null);

            TextView textTV = (TextView) convertView.findViewById(R.id.setting_text);
            TextView subTitleTV = (TextView) convertView.findViewById(R.id.sub_title);
            ImageView subIconIV = (ImageView) convertView.findViewById(R.id.sub_icon);

            if (position == 0 && resetPosition) {
                convertView.setPadding(20, 18, 30, 0);
            }

            textTV.setText(items.get(position).title);

            if (items.get(position).subIconId != null) {
                subIconIV.setVisibility(View.VISIBLE);
                subIconIV.setImageResource(items.get(position).subIconId);
            } else {
                subIconIV.setVisibility(View.GONE);
            }

            if (items.get(position).subTitle != null) {
                subTitleTV.setVisibility(View.VISIBLE);
                subTitleTV.setText(items.get(position).subTitle);
            } else {
                subTitleTV.setVisibility(View.GONE);
            }
            
            return convertView;
        }
    }
    
}
