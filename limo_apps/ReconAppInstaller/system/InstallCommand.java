package com.reconinstruments.installer.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.reconinstruments.installer.InstallerService;
import com.reconinstruments.installer.InstallerUtil;
import com.reconinstruments.installer.PackageUtils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class InstallCommand {
	public static final String TAG = "InstallCommand";

	public static final boolean DEBUG = true;

    public static final String CORE_UPDATE_XML_PATH = "/ReconApps/cache/update_package.xml";
	
	public String bundle = null;
	
	private static boolean isInstalling = false;
	
	private Context mContext = null;
	private Queue<RifInfo> installQueue;

	/**
	 * TODO later put Platform specific settings here such as Limo / Jet or etc
	 * settings
	 */
	public InstallCommand(Context context) {
		mContext = context;
		installQueue = new LinkedList<RifInfo>();
	}
	
	/**
	 * Puts Rif file info into InstallQueue
	 * 
	 * @param info
	 * @return succeed?
	 */
	public synchronized boolean enqueRif(RifInfo info) {
		return installQueue.add(info);
	}

	/**
	 * initiate Install Process after enqueing
	 * 
	 * @return there is > 0 rif left to install?
	 */
	public synchronized boolean startInstall() {
		if (DEBUG) Log.d(TAG, "startInstall(), isInstalling? " + isInstalling );
		if (!isInstalling) {
			isInstalling = true;
			installNextRif();
			return true;
		}
		// Return false if it is already installing
		return false;

	}

	/**
	 * If packageName is installed, remove it from queue
	 * If 
	 * 
	 * @param packageName
	 * @return
	 */
	public boolean notifyInstalled(String packageName) {
		
		if (installQueue.peek().getPackageName().equalsIgnoreCase(packageName)) {
			
			if (installQueue.size() > 0) {
				
				installQueue.poll();
				
				if (DEBUG) Log.d(TAG, "polled " + packageName + " from the InstallQueue");
				
				return installNextRif();
				
			}		
			
		}
		else {
			if (DEBUG) Log.d(TAG, "Package That is just installed is Not the Current top of the Installer Queue. It could be other installation method" );
		}
		
		return true;
	}

	/**
	 * Checks the integrity of the rif file by checking MD5 and then Execute decodeTask
	 * 
	 * @return
	 */
	private boolean installNextRif() {
		
		if (installQueue.size() > 0) {
			RifInfo rifInfo = installQueue.peek();
			if (DEBUG) Log.d(TAG, "installNextRif()" + rifInfo.getRifPath());
			
			if ( PackageUtils.isInstalled(TAG, mContext, rifInfo.getPackageName()) && 
					rifInfo.getVersion() <= PackageUtils.getInstalledPackageVersionCode(TAG, mContext, rifInfo.getPackageName())	) {
				if (DEBUG) Log.d(TAG, "isInstalled and installed version >= rifInfo, poll rifInfo, erase installer files, and run install next rif");
					eraseInstallerFile(rifInfo);
					installQueue.poll();
					installNextRif();
			}
			else {
				new SystemDecodeTask(mContext).execute(installQueue.peek());
			}
			
			return true;
		}
		else {
			if (DEBUG) Log.d(TAG, "0 RifInfo on the Installer Queue");
			isInstalling = false;
			if(new File(Environment.getExternalStorageDirectory() + InstallerUtil.INSTALLER_SYSTEM_PATH + "/update_package.xml").delete())
				if (DEBUG) Log.d(TAG, "update_package.xml deleted");
			return false;
		}
		
	}
	
	/**
	 * Erase both files from the system
	 * 
	 */
	private boolean eraseInstallerFile(RifInfo rifInfo) {
		if (DEBUG) Log.d(TAG, "eraseInstallerFile()");
		return (rifInfo.getApkFile().delete() && rifInfo.getRifFile().delete());
	}
	
    public static final String TAG_UPDATE_INFO              = "app_update_info";
    public static final String UPDATE_INFO_ATTR_BUNDLE      = "bundle";
    public static final String TAG_PACKAGE                  = "package";
    public static final String PACKAGE_ATTR_FILE_NAME       = "file_name";
    public static final String PACKAGE_ATTR_PACKAGE_NAME    = "name";
    public static final String PACKAGE_ATTR_VERSION         = "version";
    public static final String PACKAGE_ATTR_MD5_RIF         = "md5_rif";
    public static final String PACKAGE_ATTR_MD5_APK         = "md5_apk";
	
	public static InstallCommand parseUpdatePackageXML(Context mContext, String explicitPath) {

        InstallCommand cmd = new InstallCommand(mContext);

        if (TextUtils.isEmpty(explicitPath))
            explicitPath = CORE_UPDATE_XML_PATH;
        try {
            FileInputStream fis = new FileInputStream(new File (Environment.getExternalStorageDirectory() + explicitPath ));

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new InputStreamReader(fis));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if(eventType == XmlPullParser.START_DOCUMENT) {
                    if (InstallerService.DEBUG) Log.d(TAG, "START DOCUMENT");
                }

                if(eventType == XmlPullParser.START_TAG && xpp.getName().equalsIgnoreCase(TAG_UPDATE_INFO)) {

                    for (int i = 0 ; i < xpp.getAttributeCount() ; i++){

                        if (DEBUG) Log.d(TAG, xpp.getAttributeName(i) + " : " + xpp.getAttributeValue(i));

                        if (xpp.getAttributeName(i).equalsIgnoreCase(UPDATE_INFO_ATTR_BUNDLE)) {
                            cmd.bundle = xpp.getAttributeValue(i);
                        }

                    }
                }

                if(eventType == XmlPullParser.START_TAG && xpp.getName().equalsIgnoreCase(TAG_PACKAGE)) {

                    if (DEBUG) Log.d(TAG, "enque RifInfo object");

                    File rifFile = null;
                    String packageName = null;
                    int version = -1;
                    String rifMD5 = null;
                    String apkMD5 = null;

                    for (int i = 0 ; i < xpp.getAttributeCount() ; i++){    
                        if (DEBUG) Log.d(TAG, xpp.getAttributeName(i) + " : " + xpp.getAttributeValue(i));

                        if (xpp.getAttributeName(i).equalsIgnoreCase(PACKAGE_ATTR_FILE_NAME)) {
                            rifFile = new File(Environment.getExternalStorageDirectory() + InstallerUtil.INSTALLER_SYSTEM_PATH + "/" + xpp.getAttributeValue(i));
                        } else if (xpp.getAttributeName(i).equalsIgnoreCase(PACKAGE_ATTR_VERSION)) {
                            version = Integer.parseInt(xpp.getAttributeValue(i));
                        } else if (xpp.getAttributeName(i).equalsIgnoreCase(PACKAGE_ATTR_MD5_RIF)) {
                            rifMD5 = xpp.getAttributeValue(i); 
                        } else if (xpp.getAttributeName(i).equalsIgnoreCase(PACKAGE_ATTR_MD5_APK)) {
                            apkMD5 = xpp.getAttributeValue(i); 
                        } else if (xpp.getAttributeName(i).equalsIgnoreCase(PACKAGE_ATTR_PACKAGE_NAME)) {
                            packageName = xpp.getAttributeValue(i); 
                        }
                    }   
                    cmd.enqueRif(new RifInfo(mContext,rifFile, packageName, rifMD5, apkMD5, version, true));
                }
                eventType = xpp.next();
            }
            if (DEBUG) Log.d(TAG, "End document");

        } catch (FileNotFoundException e) {
            if (DEBUG) Log.e(TAG, e.getMessage());
            return null;
        } catch (XmlPullParserException e) {
            if (DEBUG) Log.e(TAG, e.getMessage());
            return null;
        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, e.getMessage());
            return null;
        }

        return cmd;
    }

}
