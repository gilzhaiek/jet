
package com.reconinstruments.hudservice;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import com.reconinstruments.mobilesdk.hudconnectivity.Constants;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.modlive.NotificationHelper;
import com.reconinstruments.ifisoakley.OakleyDecider;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.messagecenter.ReconMessageAPI;

public class BtConnectionStateChangeReceiver extends BroadcastReceiver {
    public static final int CONNECTION_NOTIFICATION_ID = 1; // Same as Mobile
                                                            // connect library
    private final static String TAG = "BtConnectionStateChangeReceiver";
    HUDService mTheService;
    private static int mPrevConnectionState = 0;

    public BtConnectionStateChangeReceiver(HUDService theService) {
        super();
        Log.v(TAG, "constructor");
        mTheService = theService;
    }

    

    // Notes on the behaviour logic:
    // Hide the connection icon if you go from connected to disconencted or to connecting
    // For iOS only disconenct Hfp and Map when fully disconnecting
    // Notify of disconnection whenever you go away from the connected state
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "HUD_STATE_CHANGED");
        Bundle bundle = intent.getExtras();
        int b = bundle.getInt("state"); // connectionstate
        Log.v(TAG, "b is " + b);
        if (b == 0) { // disconnected
             // Just blindly disconnect
	    Log.v(TAG, "attempt to disconnect map and hfp");
	    disconnectCallAndTextSS1(context);
	    Log.v(TAG, "disconnected");
	    // Send notification:
	    if (mPrevConnectionState == 2) {
		ReconMessageAPI.showPassiveNotification(context,"Engage app disconnected",0);
		NotificationHelper.clearNotifications(mTheService);
	    }
        }
        else if (b == 1) { //connecting
	     if (mPrevConnectionState == 2) { // Was previously fully connected
		 ReconMessageAPI.showPassiveNotification(context,"Engage app disconnected",0);
		 NotificationHelper.clearNotifications(mTheService);
	     }
             else {
        	 if (getBTConnectedDeviceType(context) == 1) { // 1 means ios
                     Log.v(TAG, "attempt to enable hfp on ios");
                     String btadderss = getBTConnectedDeviceAddress(context);
                     connectHfpSS1(context, btadderss);
                     Log.v(TAG, "attempt to enable map on ios");
                     connectMapSS1_delayed();
            	 }
            }
        }
        else if (b == 2) { // connected
            if(getBTConnectedDeviceType(context) == 0) { // android
                 Log.v(TAG, "attempt to enable hfp on android");
                 String btaddress = getBTConnectedDeviceAddress(context);
                 connectHfpSS1(context, btaddress);
            }
        
            Log.v(TAG, "connected");
            postConnected(context, R.drawable.sp_connectivity);
            sendAfterConnectInfo();
            mTheService.resetReconnectingTask();
        }
	mPrevConnectionState = b; // update previous state
    }

    static void postConnected(Context context, int icon_id) {
        Log.v(TAG, "posting conneted");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n;
        n = new Notification(icon_id, null, System.currentTimeMillis());
        n.setLatestEventInfo(context, "", "", contentIntent);
        n.flags |= Notification.FLAG_ONGOING_EVENT;
        n.flags |= Notification.FLAG_NO_CLEAR;
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Log.v(TAG, "notify");
        notificationManager.notify(CONNECTION_NOTIFICATION_ID, n);
    }

    private void sendAfterConnectInfo() {

        String hudType = Build.MODEL;
        String brand = (OakleyDecider.isOakley()) ? "oakley" : "recon";
        String xmlMessage = "<recon intent=\"after_connect\"><firmware_version>" + getVersionName()
                + "</firmware_version><serial_number>" + Build.SERIAL
                + "</serial_number> <hud_type>" + hudType + "</hud_type> <brand>" + brand
                + "</brand> </recon>";

        HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
        cMsg.setIntentFilter("AFTER_CONNECT");
        cMsg.setSender(TAG);
        cMsg.setRequestKey(0);
        cMsg.setData(xmlMessage.getBytes());
        mTheService.push(cMsg, HUDConnectivityService.Channel.OBJECT_CHANNEL);

        // TODO: uncomment with new cmsg from Henry.
        // Also send the ID File:
        HUDConnectivityMessage cMsgid = new HUDConnectivityMessage();
        cMsgid.setIntentFilter("ID_FILE");
        cMsgid.setSender(TAG);
        cMsgid.setRequestKey(0);
        cMsgid.setInfo("ID.RIB");
        byte[] idcontent = readIdFile();
        if (idcontent != null) {
            cMsgid.setData(writeFile(idcontent).getBytes());
            mTheService.push(cMsgid, HUDConnectivityService.Channel.FILE_CHANNEL);
        }
        else {
            Log.w(TAG, "id file content is null");
        }

	// Now send sports activity info
	IncomingPhoneStatusReceiver.sendActivityInfoXmlViaBt(mTheService);

    }

    private String getVersionName() {
        String versionName = "unknown";
        try {
            PackageInfo pInfo =
                    mTheService.getPackageManager().getPackageInfo(mTheService.getPackageName(),
                            PackageManager.GET_META_DATA);
            versionName = pInfo.versionName;
        } catch (NameNotFoundException e1) {
            Log.e(mTheService.getClass().getSimpleName(), "Name not found", e1);
        }
        return versionName;
    }

    private byte[] readIdFile() {
        try {
            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/ReconApps/ID.RIB";
            RandomAccessFile f = new RandomAccessFile(fileName, "r");
            byte[] b = new byte[(int) f.length()];
            f.read(b);
            return b;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    // Warning code duplication from ConnectDevice:
    // TODO: get them into a jar or something.

    // These functions are specific to JET platform that uses SS1 BT Stack

    void connectHfpSS1(Context context, String btaddress) {
        Intent i = new Intent("RECON_SS1_HFP_COMMAND");
        i.putExtra("command", 500);
        i.putExtra("address", btaddress);
        context.sendBroadcast(i);
    }

    static void connectMapSS1(Context context, String btaddress) {
        Intent i = new Intent("RECON_SS1_MAP_COMMAND");
        i.putExtra("command", 500);
        i.putExtra("address", btaddress);
        context.sendBroadcast(i);
    }

    void disconnectHfpSS1(Context context) {
        Intent i = new Intent("RECON_SS1_HFP_COMMAND");
        i.putExtra("command", 600);
        context.sendBroadcast(i);
    }

    static void disconnectMapSS1(Context context) {
        Intent i = new Intent("RECON_SS1_MAP_COMMAND");
        i.putExtra("command", 600);
        context.sendBroadcast(i);
    }

    private void connectMapSS1_delayed() {
        Log.v(TAG, "connecting with Map with a delay of 3 seconds");
        mTheService.defaultHandler.removeCallbacks(mTheService.connectMapSS1_runnable);
        mTheService.defaultHandler.postDelayed(mTheService.connectMapSS1_runnable,
                5000);
    }

    private void disconnectCallAndTextSS1(Context context) {
        // We disable it because it introduces instablity as of July 22nd 2013.

        disconnectHfpSS1(context);
        mTheService.defaultHandler.postDelayed(mTheService.disconnectMapSS1_runnable,
                300);
    }

    public static int getBTConnectedDeviceType(Context context) {
        Log.d(TAG, "BTConnectedDeviceType from Settings.System");
        int res = 0;
        try {
            res = Settings.System.getInt(context.getContentResolver(), "BTConnectedDeviceType");
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static String getBTConnectedDeviceAddress(Context context) {
        Log.d(TAG, "BTConnectedDeviceAddress from Settings.System");
        return Settings.System.getString(context.getContentResolver(), "BTConnectedDeviceAddress");
    }

    public String writeFile(byte[] fileArray) {

        String filename = performChecksum(fileArray) + ".tmp";
        String path = "";
        //
        String parentFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/tmp/";
        // make desired directories

        File file = new File(parentFolderPath);
        if (!file.mkdirs())
        {
            Log.d(TAG, "Parent directories were not created. Possibly since they already exist.");
        }

        // delete file if it exists
        String file_path = file.getAbsolutePath() + "/" + filename;
        Log.d(TAG, "temporary path: " + file_path);
        File fileToDel = new File(file_path);
        boolean deleted = false;
        if (fileToDel.exists())
            deleted = fileToDel.delete();

        if (deleted)
            Log.d(TAG, "file was succesfully deleted");
        else {
            Log.d(TAG, "no file found to delete");
        }

        // write to file
        file = new File(file_path);

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            Log.w(TAG, "caught exception opening buffer: " + e);
            e.printStackTrace();
            return path;
        }
        try {
            bos.write(fileArray);
            bos.flush();
            bos.close();
            path = file.getAbsolutePath();
        } catch (IOException e) {
            Log.w(TAG, "caught exception closing file : " + e);
            e.printStackTrace();
            return path;
        }

        //
        return path;
    }

    private String performChecksum(byte[] c) {
        String localSum = md5(c, 0);
        return localSum;
    }

    /**
     * generate an md5 checksum from a byte array offset allows md5 to be
     * calculated ignoring the beginning of the data
     */
    public static String md5(byte[] array, int offset) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            // digest.update(array);
            digest.update(array, offset, array.length - offset);
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(String.format("%02x", messageDigest[i] & 0xff));
            String md5 = hexString.toString();
            return md5;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}
