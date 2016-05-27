/*
This software is subject to the license described in the License.txt file 
included with this software distribution. You may not use this file except in compliance 
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
 */

package com.dsi.ant.antplus.pluginsampler.fitnessequipment;

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
import com.dsi.ant.plugins.antplus.common.FitFileCommon.FitFile;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.EquipmentState;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.EquipmentType;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.HeartRateDataSource;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IBikeDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IClimberDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IEllipticalDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IFitnessEquipmentStateReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IGeneralFitnessEquipmentDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IGeneralMetabolicDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IGeneralSettingsReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.ILapOccuredReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.INordicSkierDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.IRowerDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.ITreadmillDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.Settings;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IManufacturerIdentificationReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IProductInformationReceiver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Connects to Environment Plugin and display all the event data.
 */
public class Activity_FitnessEquipmentSampler extends Activity
{
    AntPlusFitnessEquipmentPcc fePcc = null;
    Settings settings;
    FitFile[] files;

    TextView tv_status;    
    TextView tv_estTimestamp;

    TextView tv_feType;
    TextView tv_state;
    TextView tv_laps;
    TextView tv_cycleLength;
    TextView tv_inclinePercentage;
    TextView tv_resistanceLevel;
    TextView tv_mets;
    TextView tv_caloricBurn;
    TextView tv_calories;
    TextView tv_time;
    TextView tv_distance;
    TextView tv_speed;
    TextView tv_heartRate;
    TextView tv_heartRateSource;
    TextView tv_treadmillCadence;
    TextView tv_treadmillNegVertDistance;
    TextView tv_treadmillPosVertDistance;
    TextView tv_ellipticalPosVertDistance;
    TextView tv_ellipticalStrides;
    TextView tv_ellipticalCadence;
    TextView tv_ellipticalPower;
    TextView tv_bikeCadence;
    TextView tv_bikePower;
    TextView tv_rowerStrokes;
    TextView tv_rowerCadence;
    TextView tv_rowerPower;
    TextView tv_climberStrideCycles;
    TextView tv_climberCadence;
    TextView tv_climberPower;
    TextView tv_skierStrides;
    TextView tv_skierCadence;
    TextView tv_skierPower; 

    TextView tv_hardwareRevision;
    TextView tv_manufacturerID;
    TextView tv_modelNumber;

    TextView tv_softwareRevision;
    TextView tv_serialNumber;

    TextView tv_deviceNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fitnessequipment);

        tv_status = (TextView)findViewById(R.id.textView_Status);
        tv_deviceNumber = (TextView)findViewById(R.id.textView_DeviceNumber);

        tv_estTimestamp = (TextView)findViewById(R.id.textView_EstTimestamp);

        tv_feType = (TextView) findViewById(R.id.textView_FitnessEquipmentType);
        tv_state = (TextView) findViewById(R.id.textView_State);
        tv_laps = (TextView) findViewById(R.id.textView_Laps);
        tv_cycleLength = (TextView) findViewById(R.id.textView_CycleLength);
        tv_inclinePercentage = (TextView) findViewById(R.id.textView_InclinePercentage);
        tv_resistanceLevel = (TextView) findViewById(R.id.textView_ResistanceLevel);
        tv_mets = (TextView) findViewById(R.id.textView_METS);
        tv_caloricBurn = (TextView) findViewById(R.id.textView_CaloricBurn);
        tv_calories = (TextView) findViewById(R.id.textView_Calories);
        tv_time = (TextView) findViewById(R.id.textView_Time);
        tv_distance = (TextView) findViewById(R.id.textView_Distance);
        tv_speed = (TextView) findViewById(R.id.textView_Speed);
        tv_heartRate = (TextView) findViewById(R.id.textView_HeartRate);
        tv_heartRateSource = (TextView) findViewById(R.id.textView_HeartRateSource);
        tv_treadmillCadence = (TextView) findViewById(R.id.textView_TreadmillCadence);
        tv_treadmillNegVertDistance = (TextView) findViewById(R.id.textView_TreadmillNegVertDistance);
        tv_treadmillPosVertDistance = (TextView) findViewById(R.id.textView_TreadmillPosVertDistance);
        tv_ellipticalPosVertDistance = (TextView) findViewById(R.id.textView_EllipticalPosVertDistance);
        tv_ellipticalStrides = (TextView) findViewById(R.id.textView_EllipticalStrides);
        tv_ellipticalCadence = (TextView) findViewById(R.id.textView_EllipticalCadence);
        tv_ellipticalPower = (TextView) findViewById(R.id.textView_EllipticalPower);
        tv_bikeCadence = (TextView) findViewById(R.id.textView_BikeCadence);
        tv_bikePower = (TextView) findViewById(R.id.textView_BikePower);
        tv_rowerStrokes = (TextView) findViewById(R.id.textView_RowerStrokes);
        tv_rowerCadence = (TextView) findViewById(R.id.textView_RowerCadence);
        tv_rowerPower = (TextView) findViewById(R.id.textView_RowerPower);
        tv_climberStrideCycles = (TextView) findViewById(R.id.textView_ClimberStrideCycles);
        tv_climberCadence = (TextView) findViewById(R.id.textView_ClimberCadence);
        tv_climberPower = (TextView) findViewById(R.id.textView_ClimberPower);
        tv_skierStrides = (TextView) findViewById(R.id.textView_SkierStrides);
        tv_skierCadence = (TextView) findViewById(R.id.textView_SkierCadence);
        tv_skierPower = (TextView) findViewById(R.id.textView_SkierPower);

        tv_hardwareRevision = (TextView)findViewById(R.id.textView_HardwareRevision);
        tv_manufacturerID = (TextView)findViewById(R.id.textView_ManufacturerID);
        tv_modelNumber = (TextView)findViewById(R.id.textView_ModelNumber);

        tv_softwareRevision = (TextView)findViewById(R.id.textView_SoftwareRevision);
        tv_serialNumber = (TextView)findViewById(R.id.textView_SerialNumber);

        Bundle b = getIntent().getExtras();
        if(b != null)
        {
            String name = b.getString(Dialog_ConfigSettings.SETTINGS_NAME);
            Settings.Gender gender = Settings.Gender.FEMALE;
            if(b.getBoolean(Dialog_ConfigSettings.SETTINGS_GENDER))
                gender = Settings.Gender.MALE;
            short age = b.getShort(Dialog_ConfigSettings.SETTINGS_AGE);
            float height = b.getFloat(Dialog_ConfigSettings.SETTINGS_HEIGHT);
            float weight = b.getFloat(Dialog_ConfigSettings.SETTINGS_WEIGHT);

            settings = new Settings(name, gender, age, height, weight);

            if(b.getBoolean(Dialog_ConfigSettings.INCLUDE_WORKOUT))
            {	    		
                try 
                {
                    // Make available a FIT workout file to the fitness equipment
                    // The sample file included with this project was obtained from the FIT SDK, v7.10
                    InputStream is = getAssets().open("WorkoutRepeatSteps.fit");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int next;
                    while((next = is.read()) != -1)
                        bos.write(next);
                    bos.flush();
                    is.close();
                    FitFile workoutFile = new FitFile(bos.toByteArray());
                    workoutFile.setFileType((short) 5);  // Make sure to set the File Type, so this information is also available to the fitness equipment
                    // Refer to the FIT SDK for more details on FIT file types
                    files = new FitFile[] { workoutFile};
                } 
                catch (IOException e) 
                {
                    files = null;
                }
            }
        }

        resetPcc();
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */ 
    private void resetPcc()
    {
        //Release the old access if it exists
        if(!(fePcc == null))
        {
            fePcc.releaseAccess();
            fePcc = null;
        }


        //Reset the text display
        tv_status.setText("Connecting...");

        tv_estTimestamp.setText("---");

        tv_feType.setText("---");
        tv_state.setText("---");
        tv_laps.setText("---");
        tv_cycleLength.setText("---");
        tv_inclinePercentage.setText("---");
        tv_resistanceLevel.setText("---");
        tv_mets.setText("---");
        tv_caloricBurn.setText("---");
        tv_calories.setText("---");
        tv_time.setText("---");
        tv_distance.setText("---");
        tv_speed.setText("---");
        tv_heartRate.setText("---");
        tv_heartRateSource.setText("---");
        tv_treadmillCadence.setText("---");
        tv_treadmillNegVertDistance.setText("---");
        tv_treadmillPosVertDistance.setText("---");
        tv_ellipticalStrides.setText("---");
        tv_ellipticalCadence.setText("---");
        tv_ellipticalPower.setText("---");
        tv_bikeCadence.setText("---");
        tv_bikePower.setText("---");
        tv_rowerStrokes.setText("---");
        tv_rowerCadence.setText("---");
        tv_rowerPower.setText("---");
        tv_climberStrideCycles.setText("---");
        tv_climberPower.setText("---");
        tv_skierStrides.setText("---");
        tv_skierCadence.setText("---");
        tv_skierPower.setText("---");      

        tv_hardwareRevision.setText("---");
        tv_manufacturerID.setText("---");
        tv_modelNumber.setText("---");

        tv_softwareRevision.setText("---");
        tv_serialNumber.setText("---");


        //Make the access request
        AntPlusFitnessEquipmentPcc.requestNewFeSessionAccess(this,
            new IPluginAccessResultReceiver<AntPlusFitnessEquipmentPcc>()
            {         
            //Handle the result, connecting to events on success or reporting failure to user.
            @Override
            public void onResultReceived(AntPlusFitnessEquipmentPcc result,
                RequestAccessResult resultCode, DeviceState initialDeviceState)
            {
                switch(resultCode)
                {
                    case SUCCESS:
                        fePcc = result;
                        tv_deviceNumber.setText(String.valueOf(fePcc.getAntDeviceNumber()));    //Get device ID
                        if(initialDeviceState == DeviceState.CLOSED)
                            tv_status.setText(fePcc.getDeviceName() + ": " + "Waiting for FE Session Request");
                        else
                            tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);
                        subscribeToEvents();
                        break;
                    case CHANNEL_NOT_AVAILABLE:
                        Toast.makeText(Activity_FitnessEquipmentSampler.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                        tv_status.setText("Error. Do Menu->Reset.");
                        break;
                    case OTHER_FAILURE:
                        Toast.makeText(Activity_FitnessEquipmentSampler.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                        tv_status.setText("Error. Do Menu->Reset.");
                        break;
                    case DEPENDENCY_NOT_INSTALLED:
                        tv_status.setText("Error. Do Menu->Reset.");
                        AlertDialog.Builder adlgBldr = new AlertDialog.Builder(Activity_FitnessEquipmentSampler.this);
                        adlgBldr.setTitle("Missing Dependency");
                        adlgBldr.setMessage("The required service\n\"" + AntPlusFitnessEquipmentPcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                        adlgBldr.setCancelable(true);
                        adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Intent startStore = null;
                                startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusFitnessEquipmentPcc.getMissingDependencyPackageName()));
                                startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                Activity_FitnessEquipmentSampler.this.startActivity(startStore);                                                
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
                        Toast.makeText(Activity_FitnessEquipmentSampler.this, "Failed: UNRECOGNIZED. Upgrade Required?", Toast.LENGTH_SHORT).show();
                        tv_status.setText("Error. Do Menu->Reset.");
                        break;
                    default:
                        Toast.makeText(Activity_FitnessEquipmentSampler.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                        tv_status.setText("Error. Do Menu->Reset.");
                        break;
                } 
            }

            /**
             * Subscribe to all the data events, connecting them to display their data.
             */
            private void subscribeToEvents()
            {
                fePcc.subscribeGeneralFitnessEquipmentDataEvent(new IGeneralFitnessEquipmentDataReceiver()
                {
                    @Override
                    public void onNewGeneralFitnessEquipmentData(final long estTimestamp,
                        EnumSet<EventFlag> eventFlags, final BigDecimal elapsedTime,
                        final long cumulativeDistance, final BigDecimal instantaneousSpeed,
                        final int instantaneousHeartRate, final HeartRateDataSource heartRateDataSource)
                    {
                        runOnUiThread(new Runnable()
                        {                                            
                            @Override
                            public void run()
                            {
                                tv_estTimestamp.setText(String.valueOf(estTimestamp)); 
                                if(elapsedTime.intValue() == -1)
                                    tv_time.setText("Invalid");
                                else
                                    tv_time.setText(String.valueOf(elapsedTime) + "s");

                                if(cumulativeDistance == -1)
                                    tv_distance.setText("Invalid");
                                else
                                    tv_distance.setText(String.valueOf(cumulativeDistance) + "m");

                                if(instantaneousSpeed.intValue() == -1)
                                    tv_speed.setText("Invalid");
                                else
                                    tv_speed.setText(String.valueOf(instantaneousSpeed) + "m/s");

                                if(instantaneousHeartRate == -1)
                                    tv_heartRate.setText("Invalid");
                                else
                                    tv_heartRate.setText(String.valueOf(instantaneousHeartRate) + "bpm");

                                switch(heartRateDataSource)
                                {
                                    case ANTPLUS_HRM:
                                    case EM_5KHz:
                                    case HAND_CONTACT_SENSOR:
                                    case UNKNOWN:
                                        tv_heartRateSource.setText(heartRateDataSource.toString());
                                        break;
                                    case UNRECOGNIZED:
                                        //TODO This flag indicates that an unrecognized value was sent by the service, an upgrade of your PCC may be required to handle this new value.
                                        Toast.makeText(Activity_FitnessEquipmentSampler.this, "Failed: UNRECOGNIZED. Upgrade Required?", Toast.LENGTH_SHORT).show();
                                        break;
                                }

                            }
                        });         
                    }
                });

                fePcc.subscribeLapOccuredEvent(new ILapOccuredReceiver()
                {

                    @Override
                    public void onNewLapOccured(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                        final int lapCount)
                    {
                        runOnUiThread(new Runnable()
                        {                                            
                            @Override
                            public void run()
                            {
                                tv_estTimestamp.setText(String.valueOf(estTimestamp));   

                                tv_laps.setText(String.valueOf(lapCount));
                            }
                        });     
                    }

                });

                fePcc.subscribeGeneralSettingsEvent(new IGeneralSettingsReceiver()
                {

                    @Override
                    public void onNewGeneralSettings(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final BigDecimal cycleLength,
                        final BigDecimal inclinePercentage, final int resistanceLevel) 
                    {
                        runOnUiThread(new Runnable()
                        {                                            
                            @Override
                            public void run()
                            {
                                tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                if(cycleLength.intValue() == -1)
                                    tv_cycleLength.setText("Invalid");
                                else
                                    tv_cycleLength.setText(String.valueOf(cycleLength) + "m");

                                if(inclinePercentage.intValue() == 0x7FFF)
                                    tv_inclinePercentage.setText("Invalid");
                                else
                                    tv_inclinePercentage.setText(String.valueOf(inclinePercentage) + "%");

                                if(resistanceLevel == -1)
                                    tv_resistanceLevel.setText("Invalid");
                                else
                                    tv_resistanceLevel.setText(String.valueOf(resistanceLevel));
                            }
                        });

                    }

                });

                fePcc.subscribeGeneralMetabolicDataEvent(new IGeneralMetabolicDataReceiver()
                {
                    @Override
                    public void onNewGeneralMetabolicData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final BigDecimal instantaneousMetabolicEquivalents,
                        final BigDecimal instantaneousCaloricBurn, final long cumulativeCalories) 
                    {
                        runOnUiThread(new Runnable()
                        {                                            
                            @Override
                            public void run()
                            {
                                tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                if(instantaneousMetabolicEquivalents.intValue() == -1)
                                    tv_mets.setText("Invalid");
                                else
                                    tv_mets.setText(String.valueOf(instantaneousMetabolicEquivalents) + "METs");

                                if(instantaneousCaloricBurn.intValue() == -1)
                                    tv_caloricBurn.setText("Invalid");
                                else
                                    tv_caloricBurn.setText(String.valueOf(instantaneousCaloricBurn) + "kcal/h");

                                if(cumulativeCalories == -1)
                                    tv_calories.setText("Invalid");
                                else
                                    tv_calories.setText(String.valueOf(cumulativeCalories) + "kcal");
                            }
                        });                
                    }                
                });

                fePcc.subscribeManufacturerIdentificationEvent(new IManufacturerIdentificationReceiver()
                {
                    @Override
                    public void onNewManufacturerIdentification(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int hardwareRevision,
                        final int manufacturerID, final int modelNumber)
                    {
                        runOnUiThread(new Runnable()
                        {                                            
                            @Override
                            public void run()
                            {
                                tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                tv_hardwareRevision.setText(String.valueOf(hardwareRevision));
                                tv_manufacturerID.setText(String.valueOf(manufacturerID));
                                tv_modelNumber.setText(String.valueOf(modelNumber));
                            }
                        });
                    }
                });

                fePcc.subscribeProductInformationEvent(new IProductInformationReceiver()
                {

                    @Override
                    public void onNewProductInformation(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int softwareRevision,
                        final long serialNumber)
                    {
                        runOnUiThread(new Runnable()
                        {                                            
                            @Override
                            public void run()
                            {
                                tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                tv_softwareRevision.setText(String.valueOf(softwareRevision));
                                tv_serialNumber.setText(String.valueOf(serialNumber));
                            }
                        });
                    }
                });
            }
            }, 
            //Receives state changes and shows it on the status display line
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
                            //Note: The state here is the state of our data receiver channel which is closed until the ANTFS session is established
                            if(newDeviceState == DeviceState.CLOSED)
                            {
                                tv_status.setText(fePcc.getDeviceName() + ": " + "Waiting for FE Session Request");
                            }
                            else
                            {
                                tv_status.setText(fePcc.getDeviceName() + ": " + newDeviceState);
                                if(newDeviceState == DeviceState.DEAD)
                                    fePcc = null;
                            }
                        }
                    });
                }
            },
            new IFitnessEquipmentStateReceiver()
            {
                @Override
                public void onNewFitnessEquipmentState(final long estTimestamp,
                    EnumSet<EventFlag> eventFlags, final EquipmentType equipmentType,
                    final EquipmentState equipmentState)
                {
                    runOnUiThread(new Runnable()
                    {                                            
                        @Override
                        public void run()
                        {
                            tv_estTimestamp.setText(String.valueOf(estTimestamp));

                            switch(equipmentType)
                            {
                                case GENERAL:
                                    tv_feType.setText("GENERAL");
                                    break;
                                case TREADMILL:
                                    tv_feType.setText("TREADMILL");
                                    fePcc.subscribeTreadmillDataEvent(new ITreadmillDataReceiver()
                                    {

                                        @Override
                                        public void onNewTreadmillData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int instantaneousCadence,
                                            final BigDecimal cumulativeNegVertDistance, final BigDecimal cumulativePosVertDistance) 
                                        {
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                {
                                                    tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                                    if(instantaneousCadence == -1)
                                                        tv_treadmillCadence.setText("Invalid");
                                                    else
                                                        tv_treadmillCadence.setText(String.valueOf(instantaneousCadence) + "strides/min");
                                                    if(cumulativeNegVertDistance.intValue() == 1)
                                                        tv_treadmillNegVertDistance.setText("Invalid");
                                                    else
                                                        tv_treadmillNegVertDistance.setText(String.valueOf(cumulativeNegVertDistance) + "m");
                                                    if(cumulativePosVertDistance.intValue() == -1)
                                                        tv_treadmillPosVertDistance.setText("Invalid");
                                                    else
                                                        tv_treadmillPosVertDistance.setText(String.valueOf(cumulativePosVertDistance) + "m");
                                                }
                                            });

                                        }});
                                    break;
                                case ELLIPTICAL:
                                    tv_feType.setText("ELLIPTICAL");
                                    fePcc.subscribeEllipticalDataEvent(new IEllipticalDataReceiver()
                                    {

                                        @Override
                                        public void onNewEllipticalData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final BigDecimal cumulativePosVertDistance,
                                            final long cumulativeStrides, final int instantaneousCadence, final int instantaneousPower) 
                                        {
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                {
                                                    tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                                    if(instantaneousCadence == -1)
                                                        tv_ellipticalCadence.setText("Invalid");
                                                    else
                                                        tv_ellipticalCadence.setText(String.valueOf(instantaneousCadence) + "strides/min");
                                                    if(cumulativePosVertDistance.intValue() == -1)
                                                        tv_ellipticalPosVertDistance.setText("Invalid");
                                                    else
                                                        tv_ellipticalPosVertDistance.setText(String.valueOf(cumulativePosVertDistance) + "m");
                                                    if(cumulativeStrides == -1)
                                                        tv_ellipticalStrides.setText("Invalid");
                                                    else
                                                        tv_ellipticalStrides.setText(String.valueOf(cumulativeStrides));
                                                    if(instantaneousPower == -1)
                                                        tv_ellipticalPower.setText("Invalid");
                                                    else
                                                        tv_ellipticalPower.setText(String.valueOf(instantaneousPower) + "W");
                                                }
                                            });                                 
                                        }

                                    });
                                    break;
                                case BIKE:
                                    tv_feType.setText("BIKE");
                                    fePcc.subscribeBikeDataEvent(new IBikeDataReceiver()
                                    {

                                        @Override
                                        public void onNewBikeData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int instantaneousCadence,
                                            final int instantaneousPower) 
                                        {
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                {
                                                    tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                                    if(instantaneousCadence == -1)
                                                        tv_bikeCadence.setText("Invalid");
                                                    else
                                                        tv_bikeCadence.setText(String.valueOf(instantaneousCadence) + "rpm");
                                                    if(instantaneousPower == -1)
                                                        tv_bikePower.setText("Invalid");
                                                    else
                                                        tv_bikePower.setText(String.valueOf(instantaneousPower) + "W");
                                                }
                                            });

                                        }
                                    });
                                    break;
                                case ROWER:
                                    tv_feType.setText("ROWER");
                                    fePcc.subscribeRowerDataEvent(new IRowerDataReceiver()
                                    {

                                        @Override
                                        public void onNewRowerData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long cumulativeStrokes,
                                            final int instantaneousCadence, final int instantaneousPower) 
                                        {
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                {
                                                    tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                                    if(cumulativeStrokes == -1)
                                                        tv_rowerStrokes.setText("Invalid");
                                                    else
                                                        tv_rowerStrokes.setText(String.valueOf(cumulativeStrokes));
                                                    if(instantaneousCadence == -1)
                                                        tv_rowerCadence.setText("Invalid");
                                                    else
                                                        tv_rowerCadence.setText(String.valueOf(instantaneousCadence) + "strokes/min");
                                                    if(instantaneousPower == -1)
                                                        tv_rowerPower.setText("Invalid");
                                                    else
                                                        tv_rowerPower.setText(String.valueOf(instantaneousPower) + "W");
                                                }
                                            });


                                        }
                                    });
                                    break;
                                case CLIMBER:
                                    tv_feType.setText("CLIMBER");
                                    fePcc.subscribeClimberDataEvent(new IClimberDataReceiver()
                                    {

                                        @Override
                                        public void onNewClimberData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long cumulativeStrideCycles,
                                            final int instantaneousCadence, final int instantaneousPower) 
                                        {
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                {
                                                    tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                                    if(cumulativeStrideCycles == -1)
                                                        tv_climberStrideCycles.setText("Invalid");
                                                    else
                                                        tv_climberStrideCycles.setText(String.valueOf(cumulativeStrideCycles));
                                                    if(instantaneousCadence == -1)
                                                        tv_climberCadence.setText("Invalid");
                                                    else
                                                        tv_climberCadence.setText(String.valueOf(instantaneousCadence) + "strides/min");
                                                    if(instantaneousPower == -1)
                                                        tv_climberPower.setText("Invalid");
                                                    else
                                                        tv_climberPower.setText(String.valueOf(instantaneousPower) + "W");
                                                }
                                            });

                                        }
                                    });
                                    break;
                                case NORDICSKIER:
                                    tv_feType.setText("NORDIC SKIER");
                                    fePcc.subscribeNordicSkierDataEvent(new INordicSkierDataReceiver()
                                    {
                                        @Override
                                        public void onNewNordicSkierData(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long cumulativeStrides,
                                            final int instantaneousCadence, final int instantaneousPower) 
                                        {
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                {
                                                    tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                                    if(cumulativeStrides == -1)
                                                        tv_skierStrides.setText("Invalid");
                                                    else
                                                        tv_skierStrides.setText(String.valueOf(cumulativeStrides));
                                                    if(instantaneousCadence == -1)
                                                        tv_skierCadence.setText("Invalid");
                                                    else
                                                        tv_skierCadence.setText(String.valueOf(instantaneousCadence) + "strides/min");
                                                    if(instantaneousPower == -1)
                                                        tv_skierPower.setText("Invalid");
                                                    else
                                                        tv_skierPower.setText(String.valueOf(instantaneousPower) + "W");
                                                }
                                            });

                                        }

                                    });
                                    break;
                                case UNKNOWN:
                                    tv_feType.setText("UNKNOWN");
                                    break;
                                case UNRECOGNIZED:
                                    tv_feType.setText("UNRECOGNIZED type, upgrade required?");
                                    break;
                                default:
                                    tv_feType.setText("INVALID: " + String.valueOf(equipmentType));
                                    break;
                            }

                            switch(equipmentState)
                            {
                                case ASLEEP_OFF:
                                    tv_state.setText("OFF");
                                    break;
                                case READY:
                                    tv_state.setText("READY");
                                    break;
                                case IN_USE:
                                    tv_state.setText("IN USE");
                                    break;
                                case FINISHED_PAUSED:
                                    tv_state.setText("FINISHED/PAUSE");
                                    break;
                                case UNRECOGNIZED:
                                    //TODO This flag indicates that an unrecognized value was sent by the service, an upgrade of your PCC may be required to handle this new value.
                                    Toast.makeText(Activity_FitnessEquipmentSampler.this, "Failed: UNRECOGNIZED. Upgrade Required?", Toast.LENGTH_SHORT).show();
                                default:
                                    tv_state.setText("INVALID: " + equipmentState);
                            }
                        }
                    });
                }

            },
            0, settings, files);
    }

    @Override
    protected void onDestroy()
    {
        if(fePcc != null)
        {
            fePcc.releaseAccess();
            fePcc = null;
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
                resetPcc();
                tv_status.setText("Resetting...");
                return true;
            default:
                return super.onOptionsItemSelected(item);                
        }
    }
}
