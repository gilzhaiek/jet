/*
This software is subject to the license described in the License.txt file 
included with this software distribution. You may not use this file except in compliance 
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
 */

package com.dsi.ant.antplus.pluginsampler.heartrate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.antplus.pluginsampler.R;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataTimestampReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Base class to connects to Heart Rate Plugin and display all the event data.
 */
public abstract class Activity_HeartRateDisplayBase extends Activity
{
    protected abstract void requestAccessToPcc();

    AntPlusHeartRatePcc hrPcc = null;

    TextView tv_status;

    TextView tv_estTimestamp;

    TextView tv_computedHeartRate;
    TextView tv_heartBeatCounter;
    TextView tv_timestampOfLastEvent;

    TextView tv_manufacturerSpecificByte;
    TextView tv_previousToLastHeartBeatEventTimeStamp;

    TextView tv_cumulativeOperatingTime;

    TextView tv_manufacturerID;
    TextView tv_serialNumber;

    TextView tv_hardwareVersion;
    TextView tv_softwareVersion;
    TextView tv_modelNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handleReset();
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */    
    protected void handleReset()
    {
        //Release the old access if it exists
        if(hrPcc != null)
        {
            hrPcc.releaseAccess();
            hrPcc = null;
        }

        requestAccessToPcc();
    }

    protected void showDataDisplay(String status)
    {
        setContentView(R.layout.activity_heart_rate);

        tv_status = (TextView)findViewById(R.id.textView_Status);

        tv_estTimestamp = (TextView)findViewById(R.id.textView_EstTimestamp);

        tv_computedHeartRate = (TextView)findViewById(R.id.textView_ComputedHeartRate);
        tv_heartBeatCounter = (TextView)findViewById(R.id.textView_HeartBeatCounter);
        tv_timestampOfLastEvent = (TextView)findViewById(R.id.textView_TimestampOfLastEvent);

        tv_manufacturerSpecificByte = (TextView)findViewById(R.id.textView_ManufacturerSpecificByte);
        tv_previousToLastHeartBeatEventTimeStamp = (TextView)findViewById(R.id.textView_PreviousToLastEventTime);

        tv_cumulativeOperatingTime = (TextView)findViewById(R.id.textView_CumulativeOperatingTime);

        tv_manufacturerID = (TextView)findViewById(R.id.textView_ManufacturerID);
        tv_serialNumber = (TextView)findViewById(R.id.textView_SerialNumber);

        tv_hardwareVersion = (TextView)findViewById(R.id.textView_HardwareVersion);
        tv_softwareVersion = (TextView)findViewById(R.id.textView_SoftwareVersion);
        tv_modelNumber = (TextView)findViewById(R.id.textView_ModelNumber);

        //Reset the text display
        tv_status.setText(status);

        tv_estTimestamp.setText("---");

        tv_computedHeartRate.setText("---");
        tv_heartBeatCounter.setText("---");
        tv_timestampOfLastEvent.setText("---");

        tv_manufacturerSpecificByte.setText("---");
        tv_previousToLastHeartBeatEventTimeStamp.setText("---");

        tv_cumulativeOperatingTime.setText("---");

        tv_manufacturerID.setText("---");
        tv_serialNumber.setText("---");

        tv_hardwareVersion.setText("---");
        tv_softwareVersion.setText("---");
        tv_modelNumber.setText("---");
    }

    /**
     * Switches the active view to the data display and subscribes to all the data events
     */
    public void subscribeToHrEvents()
    {
        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver()
        {
            @Override
            public void onNewHeartRateData(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                final int computedHeartRate, final long heartBeatCounter)
            {
                runOnUiThread(new Runnable()
                {                                            
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        tv_computedHeartRate.setText(String.valueOf(computedHeartRate));
                        tv_heartBeatCounter.setText(String.valueOf(heartBeatCounter));
                    }
                });
            }
        });

        hrPcc.subscribeHeartRateDataTimestampEvent(new IHeartRateDataTimestampReceiver()
        {
            @Override
            public void onNewHeartRateDataTimestamp(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final BigDecimal timestampOfLastEvent)
            {
                runOnUiThread(new Runnable()
                {                                            
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        tv_timestampOfLastEvent.setText(String.valueOf(timestampOfLastEvent));
                    }
                });
            }
        });

        hrPcc.subscribePage4AddtDataEvent(new IPage4AddtDataReceiver()
        {
            @Override
            public void onNewPage4AddtData(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                final int manufacturerSpecificByte,
                final BigDecimal timestampOfPreviousToLastHeartBeatEvent)
            {
                runOnUiThread(new Runnable()
                {                                            
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        tv_manufacturerSpecificByte.setText(String.format("0x%02X", manufacturerSpecificByte));
                        tv_previousToLastHeartBeatEventTimeStamp.setText(String.valueOf(timestampOfPreviousToLastHeartBeatEvent));
                    }
                });
            }
        });

        hrPcc.subscribeCumulativeOperatingTimeEvent(new ICumulativeOperatingTimeReceiver()
        {
            @Override
            public void onNewCumulativeOperatingTime(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long cumulativeOperatingTime)
            {
                runOnUiThread(new Runnable()
                {                                            
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        tv_cumulativeOperatingTime.setText(String.valueOf(cumulativeOperatingTime));
                    }
                });
            }
        });

        hrPcc.subscribeManufacturerAndSerialEvent(new IManufacturerAndSerialReceiver()
        {
            @Override
            public void onNewManufacturerAndSerial(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int manufacturerID,
                final int serialNumber)
            {
                runOnUiThread(new Runnable()
                {                                            
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        tv_manufacturerID.setText(String.valueOf(manufacturerID));
                        tv_serialNumber.setText(String.valueOf(serialNumber));
                    }
                });
            }
        });

        hrPcc.subscribeVersionAndModelEvent(new IVersionAndModelReceiver()
        {
            @Override
            public void onNewVersionAndModel(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int hardwareVersion,
                final int softwareVersion, final int modelNumber)
            {
                runOnUiThread(new Runnable()
                {                                            
                    @Override
                    public void run()
                    {
                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        tv_hardwareVersion.setText(String.valueOf(hardwareVersion));
                        tv_softwareVersion.setText(String.valueOf(softwareVersion));
                        tv_modelNumber.setText(String.valueOf(modelNumber));
                    }
                });
            }
        });
    }

    protected IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
        new IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
        {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
            DeviceState initialDeviceState)
        {
            showDataDisplay("Connecting..."); 
            switch(resultCode)
            {
                case SUCCESS:
                    hrPcc = result;
                    tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);
                    subscribeToHrEvents();
                    break;
                case CHANNEL_NOT_AVAILABLE:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case OTHER_FAILURE:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                    tv_status.setText("Error. Do Menu->Reset.");
                    AlertDialog.Builder adlgBldr = new AlertDialog.Builder(Activity_HeartRateDisplayBase.this);
                    adlgBldr.setTitle("Missing Dependency");
                    adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                    adlgBldr.setCancelable(true);
                    adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent startStore = null;
                            startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                            startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            Activity_HeartRateDisplayBase.this.startActivity(startStore);
                        }
                    });
                    adlgBldr.setNegativeButton("Cancel", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    final AlertDialog waitDialog = adlgBldr.create();
                    waitDialog.show();
                    break;
                case USER_CANCELLED:
                    tv_status.setText("Cancelled. Do Menu->Reset.");
                    break;
                case UNRECOGNIZED:
                    //TODO This flag indicates that an unrecognized value was sent by the service, an upgrade of your PCC may be required to handle this new value.
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Failed: UNRECOGNIZED. Upgrade Required?", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                default:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
            } 
        }
        };

        //Receives state changes and shows it on the status display line
        protected  IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver = 
            new IDeviceStateChangeReceiver()
        {                    
            @Override
            public void onDeviceStateChange(final DeviceState newDeviceState)
            {
                runOnUiThread(new Runnable()
                {                                            
                    @Override
                    public void run()
                    {
                        tv_status.setText(hrPcc.getDeviceName() + ": " + newDeviceState);
                        if(newDeviceState == DeviceState.DEAD)
                            hrPcc = null;
                    }
                });


            }
        };

        @Override
        protected void onDestroy()
        {
            if(hrPcc != null)
            {
                hrPcc.releaseAccess();
                hrPcc = null;
            }
            super.onDestroy();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu)
        {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.activity_heart_rate, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            switch(item.getItemId())
            {
                case R.id.menu_reset:
                    handleReset();
                    tv_status.setText("Resetting...");
                    return true;
                default:
                    return super.onOptionsItemSelected(item);                
            }
        }
}
