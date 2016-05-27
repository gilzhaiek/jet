
package com.reconinstruments.installer.system;

import java.io.File;

import com.reconinstruments.installer.InstallerUtil;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class has the information about RIF file. <br>
 * This class implements Parcelable interface
 * <ul>
 * <li>Full Rif File path</li>
 * <li>Full Apk file path</li>
 * <li>version of the Application</li>
 * <li>MD5 hash of RIF</li>
 * <li>MD5 hash of APK</li>
 * <li>isSystemApp?</li>
 * </ul>
 *
 * @author patrickcho
 */
public class RifInfo implements Parcelable
{
    public static final String TAG = "RifInfo";

    private boolean isSystemRif = false;
    private String mPackageName = null;
    private File mRifFile = null;
    private File mApkFile = null;
    private int mVersion = -1;
    private String mRifMD5 = null;
    private String mApkMD5 = null;

    private static final String RIF_EXT = ".rif";
    private static final String APK_EXT = ".apk";

    /**
     * Initialize with file, packageName and isSystem if it is System Rif, apk path will be
     * different upon initialization
     * 
     * @param rifFile
     * @param packageName
     * @param system
     */
    public RifInfo(Context context, File rifFile, String packageName, String rifMD5, String apkMD5, int version,
            boolean system)
    {
        mRifFile = rifFile;
        isSystemRif = system;
        // mApkFile = new File ((isSystemRif? CACHE_PATH_SYSTEM : CACHE_PATH_USER) + "/" +
        // mRifFile.getName().substring(0, mRifFile.getName().lastIndexOf(RIF_EXT)) + APK_EXT);
        
        // write apk to applications local cache directory
        mApkFile = new File(context.getCacheDir()+ "/"
                + mRifFile.getName().substring(0, mRifFile.getName().lastIndexOf(RIF_EXT))
                + APK_EXT);
        mPackageName = packageName;
        mRifMD5 = rifMD5;
        mApkMD5 = apkMD5;
        mVersion = version;

    }

    /*
     * ////////////////////////////////////////////////////////////////////////////////// Parcelable
     * Interface implemetation
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mRifFile);
        dest.writeSerializable(mApkFile);
        dest.writeString(mPackageName);
        dest.writeString(mRifMD5);
        dest.writeString(mApkMD5);
        dest.writeInt(mVersion);
        dest.writeByte((byte) (isSystemRif ? 1 : 0));
    }

    /**
     * Rebuild RifInfo from Parcelable Object
     * 
     * @param in Parcel object
     */
    public RifInfo(Parcel in) {
        mRifFile = (File) in.readSerializable();
        mApkFile = (File) in.readSerializable();
        mPackageName = in.readString();
        mRifMD5 = in.readString();
        mApkMD5 = in.readString();
        mVersion = in.readInt();
        isSystemRif = in.readByte() == 1;
    }

    /**
     * This is used by system for rebuilding RifInfo from Parcelable object
     */
    public static final Parcelable.Creator<RifInfo> CREATOR = new Parcelable.Creator<RifInfo>() {

        public RifInfo createFromParcel(Parcel in) {
            return new RifInfo(in);
        }

        public RifInfo[] newArray(int size) {
            return new RifInfo[size];
        }

    };

    /*
     */// ///////////////////////////////////////////////////////////////////////////////

    /**
     * Return if SystemRif
     * 
     * @return apkPath
     */
    public boolean isSystemRif()
    {
        return isSystemRif;
    }

    /**
     * Return the package Name of the Rif
     * 
     * @return mPackageName
     */
    public String getPackageName()
    {
        return mPackageName;
    }

    /**
     * Return the full path name of the .apk file
     * 
     * @return apkPath
     */
    public String getApkPath()
    {
        return mApkFile.getPath();
    }

    /**
     * Return the .apk file Object with full path
     * 
     * @return apkFile
     */
    public File getApkFile()
    {
        return mApkFile;
    }

    /**
     * Return the full path name of the .rif file
     * 
     * @return rifPath
     */
    public String getRifPath()
    {
        return mRifFile.getPath();
    }

    /**
     * Return the .rif file Object with full path
     * 
     * @return rifFile
     */
    public File getRifFile()
    {
        return mRifFile;
    }

    /**
     * This MD5 must be from update_package.xml
     * 
     * @param rifMD5
     */
    public void setRifMD5(String rifMD5) {
        mRifMD5 = rifMD5;
    }

    /**
     * This MD5 must be from update_package.xml
     * 
     * @param apkMD5
     */
    public void setApkMD5(String apkMD5) {
        mApkMD5 = apkMD5;
    }

    /**
     * This MD5 must be from update_package.xml
     * 
     * @return rifMD5
     */
    public String getRifMD5() {
        return mRifMD5;
    }

    /**
     * This MD5 must be from update_package.xml
     * 
     * @param apkMD5
     */
    public String getApkMD5() {
        return mApkMD5;
    }

    /**
     * Returns the version of the rif file
     * 
     * @param version
     */
    public int getVersion() {
        return mVersion;
    }

}
