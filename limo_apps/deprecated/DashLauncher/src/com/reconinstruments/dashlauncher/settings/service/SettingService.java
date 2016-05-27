package com.reconinstruments.dashlauncher.settings.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
import android.platform.UBootEnvNative;
import android.util.Base64;
import android.util.Log;

import com.reconinstruments.dashlauncher.DashLauncherApp;
import com.reconinstruments.dashlauncher.HUDServiceHelper;
import com.reconinstruments.dashlauncher.settings.service.UninstallMessage.UninstallBundle;
import com.reconinstruments.dashlauncher.packagerecorder.ReconPackageRecorder;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath.PathType;

public class SettingService extends Service {

	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
	private static final String START_TAG = "<installed-packages>\n";
	private static final String END_TAG = "</installed-packages>\n";
	private static final String PKG_TAG ="\t<package name=\"%s\"\n\t versionCode=\"%s\" \n\t versionName=\"%s\" >\n";
	private static final String PKG_TAG_END ="</package>\n";

	static public final String UNINSTALL_MESSAGE = "RECON_UNINSTALL";
	static public final String PKGLIST_REQUEST_MESSAGE = "RECON_PKGLIST_REQUEST";
	static public final String FIRMWARE_MESSAGE = "RECON_FIRMWARE_REQUEST";

	private static final String UBOOT_OPT_NAME = "BOOT_OPT";
	private static final String UBOOT_UPDATE_FIRMWARE = "UPDATE_REQ";

	private Context mContext = this;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

       @Override
       public void onCreate() {
	   super.onCreate();
	   ReconPackageRecorder.writePackageData(this, new File(Environment.getExternalStorageDirectory(),ReconPackageRecorder.PACKAGES_XML_FILE));
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

		unregisterReceiver(fwUpdate);
		unregisterReceiver(uninst);
		unregisterReceiver(makeList);
		
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

		}};

		private BroadcastReceiver makeList = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				// 

				ReconPackageRecorder.writePackageData(context, new File(Environment.getExternalStorageDirectory(),"ReconApps/Installer/installed_packages_img.xml"));
//				HUDServiceHelper.getInstance(DashLauncherApp.getInstance().getApplicationContext()).pushFile(DashLauncherApp.getInstance(), new FilePath("ReconApps/Installer/installed_packages_img.xml", PathType.STORAGE),
//						new FilePath("ReconApps/Installer/installed_packages_img.xml", PathType.STORAGE));
				BTCommon.pushFile(context,
						new FilePath("ReconApps/Installer/installed_packages_img.xml", PathType.STORAGE),
						new FilePath("ReconApps/Installer/installed_packages_img.xml", PathType.STORAGE));
			}

		};

		public static String getPackageDataXML(Context c) {

			/* Get an instance of the package manager. */
			PackageManager pManager = c.getPackageManager();

			if (pManager == null) {
				Log.e("ReconPackageRecorder", "Failed to get package manager object.");
			}

			/* Get installed packages. */
			List<PackageInfo> pkgList = pManager.getInstalledPackages(0);

			/* Iterate through installed packages and create an XML string. */
			String xmlString = XML_HEADER + START_TAG;
			for (PackageInfo pkgInfo : pkgList) {
				xmlString += String.format(PKG_TAG, pkgInfo.packageName, pkgInfo.versionCode, pkgInfo.versionName);
				Drawable icon = null;

				try {
					icon = pManager.getApplicationIcon(pkgInfo.packageName);
				} catch (NameNotFoundException e) {
					e.getLocalizedMessage();
				}

				Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 70, baos);
				byte[] bitmapData = baos.toByteArray();

				xmlString += new String(Base64.encode(bitmapData, Base64.DEFAULT));
				xmlString += "\n";

				xmlString += PKG_TAG_END;

			}
			xmlString += END_TAG;
			return xmlString;

		}

		public static void writePackageData(Context c, File f) {

			String xmlString =getPackageDataXML(c);


			/* Write to location specified. */
			try {
				(f.getParentFile()).mkdirs();
				FileWriter fw = new FileWriter(f);
				fw.append(xmlString);
				fw.flush();
				fw.close();
			} catch (IOException e) {
				Log.e("ReconPackageRecorder", "IOException writing package list.");
				return;
			}
			Log.v("ReconPackageRecorder", "Wrote package list to " + f.getPath() + ".");

		}


}
