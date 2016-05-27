//-*-  indent-tabs-mode:nil;  -*-
package com.rxnetworks.rxnserviceslib;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.rxnetworks.device.DeviceId;
import com.rxnetworks.device.NetworkStatus;
import com.rxnetworks.rxnserviceslib.request.MslXybridRequest;

// Our hack:
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import android.os.FileObserver;
import android.os.Environment;
// End our hack

public class RXNetworksService extends Service{
    private SocketServer domainSocket;
    private Class<?> XybridHandlerClass;
    private Object xybridHandler = null;
    private Class<?> SynchroHandlerClass;
    private Object synchroHandler = null;
        
    public final static String SYNCHRO_CLIENT_ID = "evaluation";
    public final static int SYNCHRO_PORT = 9380;
    public final static String SYNCHRO_SERVER = "xybrid-dev.gpstream.net";
        
    public final static String VERSION = "7.0.5";
        
    public static final String TAG = "RXNetworksServices";

    public IBinder onBind(Intent intent) 
    {
        return null;
    }
    @Override
    public void onCreate() 
    {
        super.onCreate(); 
        try
            {
                domainSocket = new SocketServer(this);
                domainSocket.start();           
                
                try {
                    XybridHandlerClass = Class.forName("com.rxnetworks.rxnservicesxybrid.XybridHandler");
                    xybridHandler = XybridHandlerClass.getConstructors()[0].newInstance(this, domainSocket);
                } catch (Exception e) {
                    Log.d(TAG, "Xybrid not available in this release");
                    Log.d(TAG, e.toString());
                }
                
                try {
                    SynchroHandlerClass = Class.forName("com.rxnetworks.rxnservicesxybrid.SynchroHandler");
                    synchroHandler = SynchroHandlerClass.getConstructors()[0].newInstance(this, domainSocket);
                } catch (Exception e) {
                    Log.d(TAG, "Synchro not available in this release");
                }
            }
        catch(UnknownHostException uhe)
            {
                uhe.printStackTrace();
            }
        catch(IOException e) 
            {
                e.printStackTrace();
            }
            
        registerReceiver(onConnectivityChanged, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
                
        /* Retrieve the IMEI and IMSI from the device */
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        DeviceId.setDeviceId(tm.getDeviceId(), tm.getSubscriberId());


        ////////////////////////////////////////
        Log.v(TAG,"inject if there is existing seed file");
        getAndInjectAndDeleteDefaultFile(domainSocket);
        Log.v(TAG,"start Seed Watching");
        mSeedFileObserver.startWatching();
        ////////////////////////////////////////
    }
        
    public void onDestroy()
    {
        ////////////////////////////////
        Log.v(TAG,"stop Seed Watching");
        mSeedFileObserver.stopWatching();
        ///////////////////////////////
        Log.d(TAG, "onDestroy");
        if(onConnectivityChanged != null)
            {           
                unregisterReceiver(onConnectivityChanged);
            }
        if(domainSocket != null)
            {
                domainSocket.stop();
            }
        super.onDestroy();
        System.exit(1);
    }
        
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }
        
    BroadcastReceiver onConnectivityChanged = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                boolean dataAccessEnabled = NetworkStatus.IsDownloadAllowed(context, (NetworkInfo) intent.getParcelableExtra("networkInfo"));
                domainSocket.setDataAccessEnabled(dataAccessEnabled);
                        
                if (dataAccessEnabled && synchroHandler != null)
                    {
                        // Schedule a Synchro download check in 30 seconds
                        new Timer().schedule(new TimerTask() {
                                public void run() {
                                    try {
                                        SynchroHandlerClass.getMethod("download").invoke(synchroHandler);
                                    } catch (Exception e) {
                                        Log.e(TAG, e.toString());
                                    }
                                }
                            }
                            , 30 * 1000);
                    }
                        
                // If there is no data access and not XYBRID, then just exit
                if (!dataAccessEnabled && xybridHandler == null)
                    {
                        Log.i(TAG, "No data access, so Rx Networks Services will exit.");

                        ComponentName comp = new ComponentName(getPackageName(), RXNetworksService.class.getName());
                        stopService(new Intent().setComponent(comp));
                                
                        stopSelf();
                    }
            }
        };
        
    public void handleXybridRequest(final MslXybridRequest request)
    {
        try {
            XybridHandlerClass.getMethod("start", MslXybridRequest.class).invoke(xybridHandler, request);
        } catch (Exception e) {
            Log.e(TAG, "Xybrid request received, but Xybrid not available in this release");
        }
    }
        
    public void handleSynchroRequest(final MslXybridRequest xybridRequest)
    {
        try {
            SynchroHandlerClass.getMethod("start").invoke(synchroHandler);
        } catch (Exception e) {
            Log.e(TAG, "Synchro request received, but Synchro not available in this release");
        }
    }

    /////////////////////////////////////////////////////////////////////////
    private final static String PATH_TO_WATCH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/ReconApps/RXN/Seed";
    private final static String DEFAULT_FILE_NAME = "seedGPS_0701_00.current";
    private static final int READ_CHUNK_SIZE = 512;             
    // Observe the folder into which the seed file is going to be
    // copied:
    FileObserver mSeedFileObserver =
        new FileObserver(PATH_TO_WATCH, FileObserver.MODIFY) {
            @Override
            public void onEvent(int event, String file) {
                Log.v(TAG,"Seed File created or modified "+file);
                if (file != null) {
                    File f = new File(PATH_TO_WATCH+"/"+file);
                    injectAndDelete(f,domainSocket);
                }
            }
        };
    /**
     * injects a seed file through the provided socket
     *
     * @param seedFile a <code>File</code> 
     * @param socket an RXN <code>SocketServer</code>
     */
    public void inject(File seedFile, SocketServer socket) {
        SendPGPSSeed pgpsSeedSenderToNative = new SendPGPSSeed(socket);
        try {
            // Dump the file into the output stream of seed sender:
            OutputStream oos = pgpsSeedSenderToNative.getOutputStream();
            InputStream in = new FileInputStream(seedFile);
        
            byte[] data = new byte[READ_CHUNK_SIZE];
            int size = 0;
            while ((size = in.read(data, 0, READ_CHUNK_SIZE)) > 0) {
                oos.write(data, 0, size);
            }
            in.close();
            // Now send it:
            pgpsSeedSenderToNative.xmlSocketSend();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void injectAndDelete(File seedFile, SocketServer socket) {
        inject(seedFile,socket);
        try {
            Log.v(TAG,"deleting seed file");
            if (seedFile.delete()) {
                Log.v(TAG,"deleted");
            }
            else {
                Log.v(TAG,"wasn't deleted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Couldn't delete seedFile");
            e.printStackTrace();
        }
    }

    private void getAndInjectAndDeleteDefaultFile(SocketServer socket) {
        File defFile = new File(PATH_TO_WATCH,DEFAULT_FILE_NAME);
        if (defFile.exists()) {
            injectAndDelete(defFile,socket);
        }
    }
}
