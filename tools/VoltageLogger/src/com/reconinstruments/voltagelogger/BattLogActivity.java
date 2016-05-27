package com.reconinstruments.voltagelogger;

import com.reconinstruments.voltagelogger.BattLogService.LocalBinder;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;

import android.util.Log;

import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class BattLogActivity extends Activity implements BattLogListener {
    private static final String TAG = "BattLogActivity";

    private Button LogButton;
    private TextView LogStatus;
    private Button ModeButton;
    private TextView ModeStatus;

    private boolean mLogEnable;
    private RunningMode mRunningMode;

    private BattLogService mService = null;
    private IncomingHandler mHandler = null;

    //private BattLogActivity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        //mActivity = this;

        // Set default value
        mLogEnable = false;

        // Set default as normal mode
        mRunningMode = RunningMode.Normal;
        setLayout();

        mHandler = new IncomingHandler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        // Bind to LocalService
        Intent Intent = new Intent(this, BattLogService.class);
        startService(Intent);
        bindService(Intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        // The activity is no longer visible (it is now "stopped")
        super.onStop();
        Log.d(TAG, "onStop");
        if(mService != null)
            mService.unregisterHandler(BattLogActivity.this);
        //Unbind from the service
        unbindService(mConnection);
        if(mLogEnable == false){
            Intent Intent = new Intent(this, BattLogService.class);
            stopService(Intent);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        // The activity is about to be destroyed.
    }

    private void setLayout() {
        LogButton = (Button) findViewById(R.id.LogButton);
        LogStatus = (TextView) findViewById(R.id.LogStatusText);
        ModeButton = (Button) findViewById(R.id.Mode);
        ModeStatus = (TextView) findViewById(R.id.ModeStatusText);

        LogButton.setOnClickListener(LogButton_onClick);
        ModeButton.setOnClickListener(ModeButton_onClick);

        //EditText timeintervalText = (EditText) findViewById(R.id.TimeIntervaleditText);
        //timeintervalText.setText("10", BufferType.EDITABLE);
        //TimeIntervalText.setOnEditorActionListener(Time_OnEditorActionListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service Connected");
            LocalBinder binder = (LocalBinder)service;
            mService = binder.getService();
            mService.registerHandler(BattLogActivity.this);
            //Enable the button
            LogButton.setEnabled(true);
            ModeButton.setEnabled(true);
            if(mLogEnable != mService.mLogEnable)
                toggleLogButton();
            if(mRunningMode != mService.mRunningMode)
                toggleModeButton();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service DisConnected");
            //Disable the button
            LogButton.setEnabled(false);
            ModeButton.setEnabled(false);
        }
    };

    private void toggleLogButton() {
        if (mLogEnable == false) {
            mLogEnable = true;
            LogStatus.setText(R.string.LogOn);
        } else {
            mLogEnable = false;
            LogStatus.setText(R.string.LogOff);
        }
    }

    private final OnClickListener LogButton_onClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleLogButton();
        }
    };

    private void toggleModeButton() {
        if (mRunningMode.equals(RunningMode.Normal)) {
            mRunningMode = RunningMode.Polling;
            ModeStatus.setText(R.string.polling);
        } else {
            mRunningMode = RunningMode.Normal;
            ModeStatus.setText(R.string.normal);
        }
    }

    private final OnClickListener ModeButton_onClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleModeButton();
        }
    };

    // Called when the user clicks the Send button
    public void sendCommand(final View view){
        int intervalTimeSec;
        EditText timeintervalText;
        String message;

        timeintervalText = (EditText) findViewById(R.id.TimeIntervaleditText);
        message = timeintervalText.getText().toString();

        if(message.matches("")){
            intervalTimeSec = 100;
            Log.d(TAG, "Set Running Mode with default interval time of " + intervalTimeSec + " seconds");
        } else    {
            intervalTimeSec = Integer.valueOf(message);
            Log.d(TAG, "Set Running Mode with interval time of " + intervalTimeSec + " seconds");
        }
        try {
            mService.setRunningMode(mLogEnable, mRunningMode, intervalTimeSec);
        } catch (Exception e){
            Log.e(TAG, "fail to send command");
        }
        timeintervalText.setText("done");
    }

    private void updateDisplay(BatteryStatus battStatus)
    {
        ((TextView)findViewById(R.id.textView1)).setText("Percentage: " + battStatus.percentage + "%");
        ((TextView)findViewById(R.id.textView2)).setText("Voltage: " + battStatus.voltage + " mV");
        ((TextView)findViewById(R.id.textView3)).setText("Current Average: " + battStatus.currentAverage  + " mA");
        ((TextView)findViewById(R.id.textView4)).setText("Current: " + battStatus.currentNow  + " mA");
        ((TextView)findViewById(R.id.textView5)).setText("Temperature: " + battStatus.temperature  + " C");
    }
 
    /**
     * Activity Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            BatteryStatus battStatus = (BatteryStatus)msg.obj;
            updateDisplay(battStatus);
        }
    }

    @Override
    public void onBatteryStatusChanged(BatteryStatus batteryStatus) {
        Message msg = Message.obtain();
        msg.obj = batteryStatus;
        mHandler.sendMessage(msg);
    }
}
