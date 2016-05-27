package com.reconinstruments.dashlauncher.radar.maps.helpers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.radar.maps.common.CommonSettings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AssetDB extends SQLiteOpenHelper{
	public static final String TAG = "AssetDB";
	public static final String LOCAL_DB_VERSION		= "1.0";
	public static final String LOCAL_DB_FILE_NAME	= "resortinfo.db";
	
	static private final String ANDROID_DB_PATH = "/data/data/";
	
	private String 				mPackageDBPath;	// Path
	private String 				mDBFullPath;	// Path + Name
	private String				mVersion 		= LOCAL_DB_VERSION;
	private String				m2011MapsFileMD5 = "069dd7c0df1a8560571c1f083a3960f0";
	private String				m2012MapsFileMD5 = "737b5876b1839be363037103ef75c6e9";
	private String				mMapsFileMD5	= "";
	private String				mResortinfoMD5	= "18436595df6090dc8b4bd8592eb7c194";
	private boolean				mUseLocalDB		= true;

	private Context				mContext;
	
	protected SQLiteDatabase	mSQLiteDatabase = null;
	
	private boolean				mMapFileCorrupted = false;
	private boolean				mResortInfoCorrupted = false;
	
	public AssetDB(Context context) {
		super(context, LOCAL_DB_FILE_NAME, null, 1);
		
		mPackageDBPath	= ANDROID_DB_PATH + context.getPackageName() +"/databases/";
		mDBFullPath		= mPackageDBPath + LOCAL_DB_FILE_NAME;
		
		mContext = context;
	}

	public void OpenDB(){
		if(mSQLiteDatabase == null)
			mSQLiteDatabase = SQLiteDatabase.openDatabase(mDBFullPath, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS );
		if(!mSQLiteDatabase.isOpen())
			mSQLiteDatabase = SQLiteDatabase.openDatabase(mDBFullPath, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS );
	}
	
	public void CloseDB(){
		if(mSQLiteDatabase != null)
			if(mSQLiteDatabase.isOpen())
				mSQLiteDatabase.close();
		
		mSQLiteDatabase = null;
	}	
	
	public boolean IsMapDataCorrupted() {
		return mResortInfoCorrupted || mMapFileCorrupted;
	}
	
	public String GetDBVersion() {
		String retVersion = "0";
		
		if(mSQLiteDatabase == null)
			return retVersion;
		
		OpenDB();
		
  		String versionQuery = "SELECT * FROM dbVersion";
  		 
  		Cursor cursor = null;
  		try
  		{			
  			cursor = mSQLiteDatabase.rawQuery(versionQuery, new String[] {});
  		}
  		catch (Exception ce)
  		{
  			Log.e("Error in transaction", ce.toString());
  		} 
  		finally 
  		{ 		   			
  			if( cursor != null )
  			{	   				
	   			int idxVersion = cursor.getColumnIndex("version");
	  
	   			cursor.moveToPosition(0);
				retVersion = cursor.getString(idxVersion);
				cursor.close();
  			}
  		}
  		
  		return retVersion;
	}
	
    protected void CreateDBFromAsset() throws IOException
    {
		// By calling this method an empty database will be created into the default system path
        // of your application so we are gonna be able to overwrite that database with our database.    		
		// call close on it to force clear out in memory DB handle cached by Android
		// so that the next openDataBase call will not using the cached DB handle
		this.getReadableDatabase().close();		
		
		
    	// Open your local db as the input stream
		InputStream inputStream = null;
		
		if(mUseLocalDB) {
			inputStream = mContext.getAssets().open(LOCAL_DB_FILE_NAME);
		} else {
			File resortInfoFile = new File(CommonSettings.GetMassStorageResortInfoDB());
			inputStream =  new BufferedInputStream(new FileInputStream(resortInfoFile));
		}
  
    	//Open the empty db as the output stream
    	OutputStream outputStream = new FileOutputStream(mDBFullPath);
 
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = inputStream.read(buffer))>0)
    	{
    		outputStream.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	outputStream.flush();
    	outputStream.close();
    	inputStream.close();
    }
    
    protected void UpdateInfo(){
    	mVersion 	= LOCAL_DB_VERSION;
    	mUseLocalDB	= true;
    	
    	// Open your local db as the input stream
		InputStream inputStream = null;
		try{
			File resortInfoFile = new File(CommonSettings.GetMassStorageDBInfoXML());
			inputStream =  new BufferedInputStream(new FileInputStream(resortInfoFile));
		} catch (Exception e){
			return;
		}
		
	    try {
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document xmlDoc					= documentBuilder.parse(inputStream);
			NodeList dbInfoNodeList			= xmlDoc.getElementsByTagName("db_info");
			Node dbInfoFirstNode			= dbInfoNodeList.item(0);
			NamedNodeMap dbInfoAttributes	= dbInfoFirstNode.getAttributes();
			
			Node nodeItem					= dbInfoAttributes.getNamedItem("db_version");
			String nodeValue				= nodeItem.getNodeValue();
			if(Integer.parseInt(nodeValue) > 0) {
				mVersion		= nodeValue;
				mResortinfoMD5	= dbInfoAttributes.getNamedItem("resortinfo_md5").getNodeValue();
				mMapsFileMD5	= dbInfoAttributes.getNamedItem("mdmaps_md5").getNodeValue();
				mUseLocalDB		= false;
			} 
		} catch(Exception e){
	    	Log.e(TAG, "Failed to parse "+CommonSettings.GetMassStorageDBInfoXML(), e );
	    }    	
	    
	    Log.v(TAG,"mVersion = "+mVersion);
    }

    public SQLiteDatabase GetDB()
	{
    	if(mSQLiteDatabase != null)
			return mSQLiteDatabase;
    	
    	String md5Value = "";
		
		UpdateInfo();
		
		try {
			md5Value = MD5Checksum.GetMD5Checksum(CommonSettings.GetMapDataZipFileName());
			//Log.v(TAG,"md5Value="+md5Value + " - mMapsFileMD5=" + mMapsFileMD5);
		}
		catch (Exception e){ 
			md5Value = "0";
			mMapsFileMD5 =  "INVALID";
		}
	
		mMapFileCorrupted = false;
		if(mMapsFileMD5.isEmpty()) {
			if(!md5Value.equalsIgnoreCase(m2011MapsFileMD5)) {
				if(!md5Value.equalsIgnoreCase(m2012MapsFileMD5)) {
					mMapFileCorrupted = true;
					Log.w(TAG,"Old Map File MD5 mismatch! Corruption of map Database");
				}
			}
		}
		else if(!md5Value.equalsIgnoreCase(mMapsFileMD5)) {
			mMapFileCorrupted = true;
			Log.w(TAG,"New Map File MD5 mismatch! Corruption of map Database");
		}
		
		// Try to open an existing database in local folder /data/data
		try
    	{
			OpenDB();
			if(mVersion.equals(GetDBVersion())) {
			
				return mSQLiteDatabase;
			}
			else {
				Log.v(TAG,"mVersion="+mVersion+" != "+GetDBVersion()+" creating database...");
				CloseDB();
			}
    	}
    	catch(SQLiteException e) {}		
		
		try {
			if(mUseLocalDB) {
				md5Value = MD5Checksum.GetMD5Checksum(mContext.getAssets().open(LOCAL_DB_FILE_NAME));
			} else {
				md5Value = MD5Checksum.GetMD5Checksum(CommonSettings.GetMassStorageResortInfoDB());
			}
			//Log.v(TAG,"md5Value="+md5Value + " - mResortinfoMD5=" + mResortinfoMD5);
		} catch (Exception e) {
			md5Value = "0";
			mResortinfoMD5 = "INVALID"; 
		}
		
		if(!md5Value.equalsIgnoreCase(mResortinfoMD5)) {
			mResortInfoCorrupted = true;
			Log.w(TAG,"Resort Info MD5 mismatch! Corruption of map Database");
		} else {
			mResortInfoCorrupted = false;
		}
		
		try {
			CreateDBFromAsset();
		} catch (IOException e) {
			return null;
		}

		try
    	{
			OpenDB();
    	}
    	catch(SQLiteException e) {
    		e.printStackTrace();
    		mSQLiteDatabase = null;
    	}
		
		mVersion = GetDBVersion();

		Log.v(TAG,"New database version="+mVersion);
		
		return mSQLiteDatabase;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
	}
}
