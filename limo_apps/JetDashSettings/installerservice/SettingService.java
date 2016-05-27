package com.reconinstruments.jetapplauncher.settings.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.platform.UBootEnvNative;
import android.util.Base64;
import android.util.Log;

import com.reconinstruments.utils.installer.ReconPackageRecorder;
import com.reconinstruments.jetapplauncher.settings.service.UninstallMessage.UninstallBundle;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath.PathType;

public class SettingService extends Service {

    static public final String UNINSTALL_MESSAGE = "RECON_UNINSTALL";
    static public final String PKGLIST_REQUEST_MESSAGE = "RECON_PKGLIST_REQUEST";
    static public final String FIRMWARE_MESSAGE = "RECON_FIRMWARE_REQUEST";
    private static final String INTENT_REQUEST_TIME = "request_time";

    private static final String UBOOT_OPT_NAME = "BOOT_OPT";
    private static final String UBOOT_UPDATE_FIRMWARE = "UPDATE_REQ";

    private Context mContext = this;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ReconPackageRecorder.writePackageData(this, new File(Environment.getExternalStorageDirectory(),ReconPackageRecorder.PACKAGES_XML_PATH));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        registerReceiver(uninst,new IntentFilter(UNINSTALL_MESSAGE));
        registerReceiver(makeList, new IntentFilter(PKGLIST_REQUEST_MESSAGE));
        registerReceiver(fwUpdate, new IntentFilter(FIRMWARE_MESSAGE));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try{
            unregisterReceiver(fwUpdate);
            unregisterReceiver(uninst);
            unregisterReceiver(makeList);
        }catch(IllegalArgumentException e){
            //ignore
        }

    }

    private BroadcastReceiver fwUpdate = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent fwUpdateIntent) {

            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            UBootEnvNative.Set_UBootVar(UBOOT_OPT_NAME, UBOOT_UPDATE_FIRMWARE);
            pm.reboot(null);	
        }
    };


    private BroadcastReceiver uninst = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            // 

            Bundle bundle = intent.getExtras();
            String message = bundle.getString("message");

            UninstallMessage um = new UninstallMessage();

            UninstallBundle uninst = um.parse(message);

            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
            uninstallIntent.setData(Uri.parse("package:"+uninst.pkgName));
            startActivity(uninstallIntent);
        }
    };

    private BroadcastReceiver makeList = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            // 

            ReconPackageRecorder.writePackageData(context, new File(Environment.getExternalStorageDirectory(),"ReconApps/Installer/installed_packages_img.xml"));

            ConnectHelper.pushFile(context,
                    new FilePath("ReconApps/Installer/installed_packages_img.xml", PathType.STORAGE),
                    new FilePath("ReconApps/Installer/installed_packages_img.xml", PathType.STORAGE));
        }
    };
}
