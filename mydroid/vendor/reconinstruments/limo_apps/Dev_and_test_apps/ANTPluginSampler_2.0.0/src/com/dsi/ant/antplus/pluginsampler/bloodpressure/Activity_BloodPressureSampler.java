/*
This software is subject to the license described in the License.txt file 
included with this software distribution. You may not use this file except in compliance 
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
*/

package com.dsi.ant.antplus.pluginsampler.bloodpressure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.antplus.pluginsampler.LayoutController_FileId;
import com.dsi.ant.antplus.pluginsampler.R;
import com.dsi.ant.plugins.antplus.common.AntFsCommon.IAntFsProgressUpdateReceiver;
import com.dsi.ant.plugins.antplus.common.FitFileCommon.FitFile;
import com.dsi.ant.plugins.antplus.common.FitFileCommon.IFitFileDownloadedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBloodPressurePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBloodPressurePcc.IDownloadAllHistoryFinishedReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusEnvironmentPcc;
import com.dsi.ant.plugins.antplus.pcc.defines.AntFsRequestStatus;
import com.dsi.ant.plugins.antplus.pcc.defines.AntFsState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.garmin.fit.BloodPressureMesg;
import com.garmin.fit.BloodPressureMesgListener;
import com.garmin.fit.Decode;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.UserProfileMesg;
import com.garmin.fit.UserProfileMesgListener;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Manages the blood pressure display sample.
 */
public class Activity_BloodPressureSampler extends Activity
{
    AntPlusBloodPressurePcc bpPcc;
    ArrayList<Closeable> layoutControllerList;
    
    Button button_getAntFsMfgID;
    Button button_requestDownloadAllHistory;
    LinearLayout linearLayout_FitDataView;
    
    ProgressDialog antFsProgressDialog;
    
    TextView tv_status;
    TextView tv_mfgID;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bloodpressure);
        
        layoutControllerList = new ArrayList<Closeable>();

        button_getAntFsMfgID = (Button) findViewById(R.id.button_getMfgId);
        button_requestDownloadAllHistory = (Button) findViewById(R.id.button_requestDownloadAllHistory);
        linearLayout_FitDataView = (LinearLayout) findViewById(R.id.linearLayout_BloodPressureCards);
        
        tv_status = (TextView)findViewById(R.id.textView_Status);   
        tv_mfgID = (TextView) findViewById(R.id.textView_AntFsMfgId);
        
        button_requestDownloadAllHistory.setOnClickListener(
            new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    antFsProgressDialog = new ProgressDialog(Activity_BloodPressureSampler.this);
                    antFsProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    antFsProgressDialog.setMessage("Sending Request...");
                    antFsProgressDialog.setCancelable(false);
                    antFsProgressDialog.setIndeterminate(false);
                    
                    boolean submitted = bpPcc.requestDownloadAllHistory(
                            new IDownloadAllHistoryFinishedReceiver()
                            {
                                //Process the final result of the download
                                @Override
                                public void onDownloadAllHistoryFinished(
                                    final AntFsRequestStatus statusCode)
                                {
                                    runOnUiThread(new Runnable()
                                    {                                            
                                        @Override
                                        public void run()
                                        { 
                                            button_requestDownloadAllHistory.setEnabled(true);
                                            antFsProgressDialog.dismiss();
                                            
                                            switch(statusCode)
                                            {
                                                case SUCCESS:
                                                    Toast.makeText(Activity_BloodPressureSampler.this, "DownloadAllHistory finished successfully.", Toast.LENGTH_SHORT).show();
                                                    break;
                                                case FAIL_ALREADY_BUSY_EXTERNAL:
                                                    Toast.makeText(Activity_BloodPressureSampler.this, "DownloadAllHistory failed, device busy.", Toast.LENGTH_SHORT).show();
                                                    break;
                                                case FAIL_DEVICE_COMMUNICATION_FAILURE:
                                                    Toast.makeText(Activity_BloodPressureSampler.this, "DownloadAllHistory failed, communication error.", Toast.LENGTH_SHORT).show();
                                                    break;
                                                case FAIL_AUTHENTICATION_REJECTED:
                                                    //NOTE: This is thrown when authentication has failed, most likely when user action is required to enable pairing
                                                    Toast.makeText(Activity_BloodPressureSampler.this, "DownloadAllHistory failed, authentication rejected.", Toast.LENGTH_LONG).show();
                                                    break;
                                                case FAIL_DEVICE_TRANSMISSION_LOST:
                                                    Toast.makeText(Activity_BloodPressureSampler.this, "DownloadAllHistory failed, transmission lost.", Toast.LENGTH_SHORT).show();
                                                    break;
                                                case UNRECOGNIZED:
                                                    //TODO This flag indicates that an unrecognized value was sent by the service, an upgrade of your PCC may be required to handle this new value.
                                                    Toast.makeText(Activity_BloodPressureSampler.this, "Failed: UNRECOGNIZED. Upgrade Required?", Toast.LENGTH_SHORT).show();
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                                }
                                
                            }, 
                            //Written using FIT SDK 7.10 Library (fit.jar)
                            new IFitFileDownloadedReceiver()
                            {
                                //Process incoming FIT file(s)
                                //NOTE: This is better done as a background task to prevent UI thread blocking
                                @Override
                                public void onNewFitFileDownloaded(
                                        FitFile downloadedFitFile)
                                {
                                    InputStream fitFile = downloadedFitFile.getInputStream();
                                    
                                    if(!Decode.checkIntegrity(fitFile))
                                    {
                                        Toast.makeText(Activity_BloodPressureSampler.this, "FIT file integrity check failed.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    //Must reset InputStream after reading it for integrity check
                                    try
                                    {
                                        fitFile.reset();
                                    } catch (IOException e)
                                    {
                                        //No IOExceptions thrown from ByteArrayInputStream
                                    }
                                    
                                    FileIdMesgListener fileIdMesgListener = new FileIdMesgListener()
                                    {
                                        @Override
                                        public void onMesg(final FileIdMesg mesg)
                                        {
                                            //Add File ID Layout to the list of layouts displayed to the user
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                { 
                                                    layoutControllerList.add(new LayoutController_FileId(getLayoutInflater(), linearLayout_FitDataView, mesg));
                                                }
                                            });
                                        }
                                    };
                                    
                                    UserProfileMesgListener userProfileMesgListener = new UserProfileMesgListener()
                                    {
                                        @Override
                                        public void onMesg(final UserProfileMesg mesg)
                                        {
                                            //Add User Profile Layout to the list of layouts displayed to the user
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                { 
                                                    layoutControllerList.add(new LayoutController_BloodPressureUserProfile(getLayoutInflater(), linearLayout_FitDataView, mesg));
                                                }
                                            });
                                        }
                                    };
                                    
                                    BloodPressureMesgListener bloodPressureMesgListener = new BloodPressureMesgListener()
                                    {
                                        @Override
                                        public void onMesg(final BloodPressureMesg mesg)
                                        {
                                            //Add Blood Pressure Layout to the list of layouts displayed to the user
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                { 
                                                    layoutControllerList.add(new LayoutController_BloodPressure(getLayoutInflater(), linearLayout_FitDataView, mesg));
                                                }
                                            });
                                        }
                                    };
                                    
                                    DeviceInfoMesgListener deviceInfoMesgListener = new DeviceInfoMesgListener()
                                    {
                                        @Override
                                        public void onMesg(final DeviceInfoMesg mesg)
                                        {
                                            //Add Device Information Layout to the list of layouts displayed to the user
                                            runOnUiThread(new Runnable()
                                            {                                            
                                                @Override
                                                public void run()
                                                { 
                                                    layoutControllerList.add(new LayoutController_BloodPressureDeviceInfo(getLayoutInflater(), linearLayout_FitDataView, mesg));
                                                }
                                            });
                                        }
                                    };
                                    
                                    
                                    MesgBroadcaster mesgBroadcaster = new MesgBroadcaster();
                                    mesgBroadcaster.addListener(fileIdMesgListener);
                                    mesgBroadcaster.addListener(userProfileMesgListener);
                                    mesgBroadcaster.addListener(bloodPressureMesgListener);
                                    mesgBroadcaster.addListener(deviceInfoMesgListener);
                                    
                                    try
                                    {
                                        mesgBroadcaster.run(fitFile);
                                    }
                                    catch (FitRuntimeException e)
                                    {
                                        Log.e("BloodPressureSampler", "Error decoding FIT file: " + e.toString());
                                        runOnUiThread(new Runnable()
                                        {                                            
                                            @Override
                                            public void run()
                                            {
                                                Toast.makeText(Activity_BloodPressureSampler.this, 
                                                        "Error decoding FIT file", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                                
                            }, 
                            new IAntFsProgressUpdateReceiver()
                            {
                                @Override
                                public void onNewAntFsProgressUpdate(final AntFsState stateCode,
                                    final long transferredBytes, final long totalBytes)
                                {
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            switch(stateCode)
                                            {
                                                //In Link state and requesting to link with the device in order to pass to Auth state
                                                case LINK_REQUESTING_LINK:
                                                    antFsProgressDialog.setMax(4);
                                                    antFsProgressDialog.setProgress(1);
                                                    antFsProgressDialog.setMessage("In Link State: Requesting Link.");
                                                    break;
                                        
                                                //In Authentication state, processing authentication commands
                                                case AUTHENTICATION:
                                                    antFsProgressDialog.setMax(4);
                                                    antFsProgressDialog.setProgress(2);
                                                    antFsProgressDialog.setMessage("In Authentication State.");
                                                    break;
                                        
                                                //In Authentication state, currently attempting to pair with the device
                                                //NOTE: Feedback SHOULD be given to the user here as pairing typically requires user interaction with the device
                                                case AUTHENTICATION_REQUESTING_PAIRING:
                                                    antFsProgressDialog.setMax(4);
                                                    antFsProgressDialog.setProgress(2);
                                                    antFsProgressDialog.setMessage("In Authentication State: User Pairing Requested.");
                                                    break;
                                        
                                                //In Transport state, no requests are currently being processed
                                                case TRANSPORT_IDLE:
                                                    antFsProgressDialog.setMax(4);
                                                    antFsProgressDialog.setProgress(3);
                                                    antFsProgressDialog.setMessage("Requesting download (In Transport State: Idle)...");
                                                    break;
                                        
                                                //In Transport state, files are currently being downloaded
                                                case TRANSPORT_DOWNLOADING:
                                                    antFsProgressDialog.setMessage("In Transport State: Downloading.");
                                                    antFsProgressDialog.setMax(100);
                                        
                                                    if(transferredBytes >= 0 && totalBytes > 0)
                                                    {
                                                        int progress = (int)(transferredBytes*100/totalBytes);
                                                        antFsProgressDialog.setProgress(progress);
                                                    }
                                                    break;
                                                    
                                                case UNRECOGNIZED:
                                                    //TODO This flag indicates that an unrecognized value was sent by the service, an upgrade of your PCC may be required to handle this new value.
                                                    Toast.makeText(Activity_BloodPressureSampler.this, "Failed: UNRECOGNIZED. Upgrade Required?", Toast.LENGTH_SHORT).show();
                                                    break;
                                                default:
                                                    Log.w("BloodPressureSampler", "Unknown ANT-FS State Code Received: " + stateCode);
                                                    break;
                                               }
                                        }
                                    });
                                }
                            });
                    
                    if(submitted)
                    {
                        clearLayoutList();
                        
                        button_requestDownloadAllHistory.setEnabled(false);
                        antFsProgressDialog.show();
                    }
                }
            });
        
        button_getAntFsMfgID.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                if(bpPcc != null)
                    tv_mfgID.setText(Integer.toString(bpPcc.getAntFsManufacturerID()));                
            }            
        });
        
        resetPcc();
    }

    private void resetPcc()
    {
        //Release the old access if it exists
        if(bpPcc != null)
        {
            bpPcc.releaseAccess();
            bpPcc = null;
        }
        
        button_requestDownloadAllHistory.setEnabled(false);
        button_getAntFsMfgID.setEnabled(false);
        clearLayoutList();

        //Make the access request
        AntPlusBloodPressurePcc.requestAccess(this, this,
                new IPluginAccessResultReceiver<AntPlusBloodPressurePcc>()
                {
                    //Handle the result, connecting to events on success or reporting failure to user.
                    @Override
                    public void onResultReceived(AntPlusBloodPressurePcc result,
                        RequestAccessResult resultCode, DeviceState initialDeviceState)
                    {
                        switch(resultCode)
                        {
                            case SUCCESS:
                                bpPcc = result;
                                button_requestDownloadAllHistory.setEnabled(true);
                                button_getAntFsMfgID.setEnabled(true);
                                tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);                                
                                break;
                            case CHANNEL_NOT_AVAILABLE:
                                Toast.makeText(Activity_BloodPressureSampler.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                                tv_status.setText("Error. Do Menu->Reset.");
                                break;
                            case OTHER_FAILURE:
                                Toast.makeText(Activity_BloodPressureSampler.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                                tv_status.setText("Error. Do Menu->Reset.");
                                break;
                            case DEPENDENCY_NOT_INSTALLED:
                                tv_status.setText("Error. Do Menu->Reset.");
                                AlertDialog.Builder adlgBldr = new AlertDialog.Builder(Activity_BloodPressureSampler.this);
                                adlgBldr.setTitle("Missing Dependency");
                                adlgBldr.setMessage("The required service\n\"" + AntPlusEnvironmentPcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                                adlgBldr.setCancelable(true);
                                adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                Intent startStore = null;
                                                startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusEnvironmentPcc.getMissingDependencyPackageName()));
                                                startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                
                                                Activity_BloodPressureSampler.this.startActivity(startStore);                                                
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
                                Toast.makeText(Activity_BloodPressureSampler.this, "Failed: UNRECOGNIZED. Upgrade Required?", Toast.LENGTH_SHORT).show();
                                tv_status.setText("Error. Do Menu->Reset.");
                                break;
                            default:
                                Toast.makeText(Activity_BloodPressureSampler.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                                tv_status.setText("Error. Do Menu->Reset.");
                                break;
                        } 
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
                                tv_status.setText(bpPcc.getDeviceName() + ": " + newDeviceState);
                                if(newDeviceState == DeviceState.DEAD)
                                {
                                    if(antFsProgressDialog != null)
                                        antFsProgressDialog.dismiss();
                                    
                                    button_requestDownloadAllHistory.setEnabled(false);
                                    bpPcc = null;
                                }
                            }
                        });
                    }
                });
    }
    
    private void clearLayoutList()
    {
        if(!layoutControllerList.isEmpty())
        {
            for(Closeable controller : layoutControllerList)
                try
                {
                    controller.close();
                } catch (IOException e)
                {
                    //Never happens
                }
            
            layoutControllerList.clear();
        }
    }

    @Override
    protected void onDestroy()
    {
        if(bpPcc != null)
        {
            bpPcc.releaseAccess();
            bpPcc = null;
        }
        
        clearLayoutList();
        
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
