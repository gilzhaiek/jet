//-*-  indent-tabs-mode:nil;  -*-w
package com.reconinstruments.installer.apk;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.reconinstruments.installer.PackageCodes;

import android.content.Context;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

/**
 * class <code>ApkInstaller</code>
 *
 * Helper class for installing APKs without user interaction.  It uses
 * reflection to call methods that called by the system when an APK is
 * being installed.
 * 
 * credit to https://paulononaka.wordpress.com/2011/07/02/how-to-install-a-application-in-background-on-android/
 * for description of this method
 */
public class ApkInstaller {

    private static final String TAG = "ApkInstaller";
        
    public static interface OnPackageInstalled {
        public void packageInstalled(String packageName, int returnCode);
    }

    public static boolean installPackage(String apkPath,Context context,final OnPackageInstalled onPackageInstalled) {
        IPackageInstallObserver.Stub observer = new IPackageInstallObserver.Stub(){
                @Override
                public void packageInstalled(String packageName, int returnCode) throws RemoteException {
                    onPackageInstalled.packageInstalled(packageName, returnCode);
                }
            };
                
        PackageManager pm = context.getPackageManager();
        
        Class<?>[] types = new Class[] {Uri.class, IPackageInstallObserver.class, int.class, String.class};
        Method installPackageMethod;
        try {
            installPackageMethod = pm.getClass().getMethod("installPackage", types);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
                
        File apkFile = new File(apkPath);
        if (apkFile.exists()) {
            Uri packageURI = Uri.fromFile(apkFile);
            try {
                installPackageMethod.invoke(pm, new Object[] {packageURI, observer, PackageCodes.INSTALL_REPLACE_EXISTING, null});
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Apk file does not exist: "+apkFile);
        }
        return false;
    }
}