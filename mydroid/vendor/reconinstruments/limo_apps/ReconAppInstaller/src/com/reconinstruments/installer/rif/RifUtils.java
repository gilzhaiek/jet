package com.reconinstruments.installer.rif;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.reconinstruments.utils.FileUtils;

public class RifUtils {

    static final String TAG = "RifUtils";

    public static final String RIF_EXT = ".rif";
    public static final FilenameFilter RIF_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            String upperName = filename.toUpperCase(Locale.US);
            if( upperName.endsWith(RIF_EXT.toUpperCase(Locale.US)) ) return true;
            else return false;
        }
    };

    public static ArrayList<String> getRifList()
    {
        ArrayList<String> rifs = new ArrayList<String>();

        File appFile = InstallerFileObserver.INSTALLER_USER_DIR;
        String path = Environment.getExternalStorageDirectory()+InstallerFileObserver.INSTALLER_USER_PATH+"/";
        
        if( appFile.exists() && appFile.isDirectory() ) {
            String[] allRifs = appFile.list(RIF_FILTER);
            for(String file : allRifs) {
                rifs.add( path + file);
                Log.d(TAG,"found rif: "+rifs.get(rifs.size()-1));
            }
        }
        return rifs;
    }
    
    public static String getPackageNameForRif(String file) {
        file = new File(file).getName();
        return file.substring(0, file.lastIndexOf(RifUtils.RIF_EXT));
    }

    public static boolean isRifInstalled(Context context, String rifPath) {
        //now search for all installed package for a match of this package archive
        //make sure the apk file exists
        String apkPath = getApkPathForRif(context,rifPath);
        File apkFile = new File(apkPath);

        if(apkFile.exists()) {
            PackageManager pm = context.getPackageManager();            
            PackageInfo archiveInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);

            // archiveInfo will be null if the package could not be successfully parsed
            if(archiveInfo==null) {
                Log.e(TAG, apkPath + " could not be successfully parsed");
            }
            else {
                List<PackageInfo> installedPacks = pm.getInstalledPackages(0);
                for( PackageInfo info: installedPacks ) {
                    if(info.packageName.equalsIgnoreCase(archiveInfo.packageName) &&
                            info.versionCode == archiveInfo.versionCode &&
                            info.versionName.equalsIgnoreCase(archiveInfo.versionName))
                    {
                        return true;                
                    }
                }
            }
        }
        return false;
    }

    static final String DECRYPT_KEY = "recon-1050-apkpk";

    public static void decodeRif(String rifPath, String apkPath, String apkMd5)
            throws IOException, NoSuchAlgorithmException, 
                NoSuchPaddingException, InvalidKeyException, 
                IllegalBlockSizeException, BadPaddingException,
                InterruptedException
    {    
        byte[] rawKeys = DECRYPT_KEY.getBytes();
        SecretKeySpec keySpec = new SecretKeySpec( rawKeys, "AES" );

        Cipher cipher = null;
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        // read in the .rif file and decrypt it into an .apk file
        File rifFile = new File( rifPath );
        if (rifFile.exists() && rifFile.isFile()){
            
            FileInputStream inputStream = new FileInputStream( rifFile );
            
            //compose the decrypted file name                               
            File apkFile = new File(apkPath);
            FileOutputStream outputStream = new FileOutputStream( apkFile );

            CipherOutputStream cipherOutput = new CipherOutputStream(outputStream, cipher);
            copyStream(inputStream, cipherOutput);

            if (apkMd5!=null) {
                checkApkMD5(apkFile,apkMd5);
            }

            int error = FileUtils.chmod(apkFile,"644");
            if(error!=0) {
                throw new IOException("Error running chmod on "+apkFile+" error: "+error);
            }
        }
        else {
            if(!rifFile.exists())
                throw new FileNotFoundException(rifFile + " : does not exist");
            else
                throw new IOException(rifFile + " : exists but is not a file");
        }
    }
    public static void checkApkMD5(File apkFile,String expectedMd5) throws MD5MismatchException {
        if (!FileUtils.md5(apkFile).equalsIgnoreCase(expectedMd5)) {
            apkFile.delete();
            throw new MD5MismatchException();
        }
    }
    public static class MD5MismatchException extends IOException {
        private static final long serialVersionUID = 1L;
    }

    // uggh, use Guava or Apache IOUtils
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[64];
        int numBytes;
        while ((numBytes = is.read(bytes)) != -1) {
            os.write(bytes, 0, numBytes);
        }
        os.flush();
        os.close();
        is.close();
    }
    
    public static String getApkPathForRif(Context context, String rifFile)
    {
        int idx = rifFile.lastIndexOf(RifUtils.RIF_EXT);
        String name = rifFile.substring(0, idx);
        name += ".apk";
        //return Environment.getExternalStorageDirectory()+"/"+new File(name).getName();
        
        return (context.getCacheDir()+"/"+new File(name).getName());
    }
}
