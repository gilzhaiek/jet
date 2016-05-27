package com.reconinstruments.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class PackageUtils {

    /**
     * Given an apk File check if it is currently installed
     * 
     * @param TAG String for Log messages
     * @param context Context instance 
     * @param apkInfo file path to installing APK file
     * @return isInstalled 
     */
    public static boolean isInstalled(String TAG, Context mContext, File apkFile) {

        if (apkFile.exists()) {
            String packageName = getPackageName(TAG, mContext, apkFile);
            return isInstalled(TAG, mContext, packageName);
        }
        return false;
    }

    /**
     * Given an packageName check if it is currently installed
     * 
     * @param TAG String for Log messages
     * @param context Context instance 
     * @param packageName packageName of the Package
     * @return isInstalled 
     */
    public static boolean isInstalled(String TAG, Context mContext, String packageName) {

        List<PackageInfo> installedPacks = mContext.getPackageManager().getInstalledPackages(0);
        for (PackageInfo info : installedPacks) {
            if (info.packageName.equalsIgnoreCase(packageName)){
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if apk file inside cache directory is newer version. <br>
     * If not exists, return false for failsafe <br>
     * If no same package is installed on the system, <br> 
     * <strong>-1</strong> is returned from <b>getInstalledPackageVersionCode()</b>,<br>
     * so <b>getPackageVersionCode()</b> is always larger
     * 
     * @param TAG String for Log messages
     * @param context Context instance 
     * @param apkInfo file path to installing APK file
     * @return isNew 
     */
    public static boolean isNewerPackage(String TAG, Context mContext, File apkInfo) {

        try {
            if (!apkInfo.exists()) {
                throw new FileNotFoundException(apkInfo + " is not found");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
            return false; // Failsafe not to install
        }

        if (getPackageVersionCode(TAG, mContext, apkInfo) > getInstalledPackageVersionCode(TAG, mContext, apkInfo))
            return true;
        else
            return false;
    }

    /**
     * Returns Package's Package name.
     * 
     * @param TAG String for Log messages
     * @param context Context instance 
     * @param apkInfo file path to installing APK file
     * @return
     */
    public static String getPackageName(String TAG, Context mContext, File apkInfo) {
        final PackageManager pm = mContext.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkInfo.getPath(), 0);
        String packageName = packageInfo.applicationInfo.packageName;
        return packageName;
    }

    /**
     * Returns Package's Version code. This is useful for comparing package versions than version name
     * 
     * @param TAG String for Log messages
     * @param context Context instance 
     * @param apkInfo file path to installing APK file
     * @return
     */
    public static int getPackageVersionCode(String TAG, Context mContext, File apkInfo) {
        final PackageManager pm = mContext.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkInfo.getPath(), 0);
        return info.versionCode;
    }

    /**
     * Returns Package's Version name.
     * 
     * @param TAG String for Log messages
     * @param context Context instance 
     * @param apkInfo file path to installing APK file
     * @return
     */
    public static int getInstalledPackageVersionCode(String TAG, Context mContext, File apkInfo) {
        String packageName = getPackageName(TAG, mContext, apkInfo);
        return getInstalledPackageVersionCode(TAG, mContext, packageName);
    }

    /**
     * Returns Package's Version name.
     * 
     * @param TAG String for Log messages
     * @param context Context instance 
     * @param packageName name of the package
     * @return
     */
    public static int getInstalledPackageVersionCode(String TAG, Context mContext, String packageName) {

        List<PackageInfo> installedPacks = mContext.getPackageManager().getInstalledPackages(0);
        for (PackageInfo info : installedPacks) {
            if (info.packageName.equalsIgnoreCase(packageName))
                return info.versionCode;
        }

        Log.d(TAG, "No same Package installed");
        return -1;
    }
}
