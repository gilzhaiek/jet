package com.reconinstruments.battery.logger;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BatteryLogActivity extends Activity
{
    private static final String TAG = BatteryLogActivity.class.getSimpleName();

    private int mLogEnabled;
    private int mLogMode;
    private int mLogInterval;

    private Button LogButton;
    private TextView LogStatus;
    private Button ModeButton;
    private TextView ModeStatus;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        try {
            mLogEnabled = Settings.System.getInt( getContentResolver(),
                    BatteryLogBootReceiver.LOG_ENABLED);
            mLogMode = Settings.System.getInt( getContentResolver(),
                    BatteryLogBootReceiver.LOG_MODE);
            mLogInterval = Settings.System.getInt( getContentResolver(),
                    BatteryLogBootReceiver.LOG_INTERVAL);

        } catch( Settings.SettingNotFoundException e ){
            Log.d(TAG, "intent not found");
            //By default we will enable log
            mLogEnabled = 1;
            mLogMode = BatteryLogBootReceiver.NORMAL_MODE;
            mLogInterval =100;
        }
        Log.d(TAG, "onCreate" + "mLogEnabled=" + mLogEnabled + ",mLogMode=" + mLogMode);
        setLayout();  
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private void setLayout() {
        LogButton = (Button) findViewById(R.id.LogButton);
        LogStatus = (TextView) findViewById(R.id.LogStatusText);
        ModeButton = (Button) findViewById(R.id.Mode);
        ModeStatus = (TextView) findViewById(R.id.ModeStatusText);

        LogButton.setOnClickListener(LogButton_onClick);
        ModeButton.setOnClickListener(ModeButton_onClick);
        /*Set button text properly*/
        if(mLogEnabled == 0)
            LogStatus.setText(R.string.LogOff);
        else
            LogStatus.setText(R.string.LogOn);

        if (mLogMode ==BatteryLogBootReceiver.NORMAL_MODE)
            ModeStatus.setText(R.string.normal);
        else
            ModeStatus.setText(R.string.polling);
        
        ((EditText)findViewById(R.id.TimeIntervaleditText)).
            setText(Integer.toString(mLogInterval));
    }

    private void toggleLogButton() {
        if (mLogEnabled == 0) {
            mLogEnabled = 1;
            LogStatus.setText(R.string.LogOn);
        } else {
            mLogEnabled = 0;
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
        if (mLogMode ==BatteryLogBootReceiver.NORMAL_MODE) {
            mLogMode = BatteryLogBootReceiver.POLLING_MODE;
            ModeStatus.setText(R.string.polling);
        } else {
            mLogMode = BatteryLogBootReceiver.NORMAL_MODE;
            ModeStatus.setText(R.string.normal);
        }
    }

    private final OnClickListener ModeButton_onClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            toggleModeButton();
        }

    };

    /** Called when the user clicks the Send button */
    public void sendCommand(final View view){
        int time;
        EditText timeintervalText;
        String message;

        timeintervalText = (EditText) findViewById(R.id.TimeIntervaleditText);
        message = timeintervalText.getText().toString();

        if(message.matches("")){
            time=100;
            Log.d(TAG, "Use default time interval " + time);
        } else {
            time = Integer.valueOf(message);
            Log.d(TAG, "Send command with time interval " + time);
        }

        Settings.System.putInt( getContentResolver(),
                BatteryLogBootReceiver.LOG_ENABLED, mLogEnabled);
        Settings.System.putInt( getContentResolver(),
                BatteryLogBootReceiver.LOG_MODE, mLogMode);
        Settings.System.putInt( getContentResolver(),
                BatteryLogBootReceiver.LOG_INTERVAL, time);
        timeintervalText.setText("done");
    }

}
