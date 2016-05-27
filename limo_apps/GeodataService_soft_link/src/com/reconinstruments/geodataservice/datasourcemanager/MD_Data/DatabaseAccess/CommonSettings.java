package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess;

import android.os.Environment;

public class CommonSettings {
	protected static final String TAG 	= "CommonSettings";
	protected static final String MAP_DATA_ZIPFILE 	= "mdmaps.zip";
	protected static final String RESORT_INFO_DB	= "resortinfo.db";
	protected static final String DB_INFO_XML		= "db_info.xml";
	
	public static String GetAssetFolder()
	{
//		Log.e(TAG, "== !!! Using local map data files from /res/raw !!! ==");
		return Environment.getExternalStorageDirectory().getPath() + "/ReconApps/MapData/";
	}
	 
	public static String GetNavigationFolder( )
	{
		return Environment.getExternalStorageDirectory().getPath() + "/ReconApps/Navigation/";
	}
	
	public static String GetMapDataZipFileName()
	{
		return GetAssetFolder() + MAP_DATA_ZIPFILE;
	}
	
	public static String GetMassStorageResortInfoDB()
	{
		return GetAssetFolder() + RESORT_INFO_DB;
	}
	
	public static String GetMassStorageDBInfoXML()
	{
		return GetAssetFolder() + DB_INFO_XML;
	}
}
