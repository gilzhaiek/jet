package com.reconinstruments.dashlauncher.packagerecorder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class ReconPackageRecorder {
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
    private static final String START_TAG = "<installed-packages>\n";
    private static final String END_TAG = "</installed-packages>\n";
    private static final String PKG_TAG ="\t<package name=\"%s\"\n\t versionCode=\"%s\" \n\t versionName=\"%s\" />\n";
    public static final String PACKAGES_XML_FILE  = "ReconApps/Installer/installed_packages.xml";
	
    /* Static class; private constructor. */
    private ReconPackageRecorder() { }
	
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
	}
	xmlString += END_TAG;
	return xmlString;
		
    }

	
	
}
