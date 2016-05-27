/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
/**
 * Handling open an static database that was shipped as part of asset
 */
public class AssetDBHelper extends SQLiteOpenHelper
{
 
    //The Android's default system path of your application database.
    private  String DB_PATH = "/data/data/";
 
    //private static String DB_NAME = "dictionary";
    private  String mDBName;
    		
 
    //force to copy from asset to system database folder regardless
    //if it is existed on the system folder or not if the option is true
    private static boolean FORCE_COPY = false;	
    
    //the static database that shipped with the application for
    //storing resort related information only
    private SQLiteDatabase mStaticDataBase = null; 
 
    private final Context mContext;
    
    private final String mCurrentVersion;
 
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     * @param currentVersion: the up-to-date version of the DB contained within the asset pack
     */
    public AssetDBHelper(Context context, String dbName, String currentVersion ) 
    {
    	super(context, dbName, null, 1);
    	
    	DB_PATH = DB_PATH + context.getPackageName() +"/databases/";
    	mDBName = dbName;
        this.mContext = context;
        
        mCurrentVersion = currentVersion;
        
    }	
 
  /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException
    {
 
    	boolean needUpdate = checkDataBase();
 
    	if( needUpdate || FORCE_COPY == true )
    	{
 
    		//By calling this method and empty database will be created into the default system path
               //of your application so we are gonna be able to overwrite that database with our database.    		
    		//call close on it to force clear out in memory DB handle cached by Android
    		//so that the next openDataBase call will not using the cached DB handle
        	this.getReadableDatabase().close();
 
        	try
        	{
 
    			copyDataBase();   
 
    		} 
        	catch (IOException e) 
        	{
 
        		throw new Error("Error copying database");
 
        	}
    	}
 
    }
 
    /**
     * Check if the database needs a update
     * @return true if it not exists, or if exists but its version not much with expected version
     */
    private boolean checkDataBase()
    {
 /*
 		File testFile = mContext.getFileStreamPath( DB_NAME );
  
    	return testFile.exists();
*/
    	boolean needUpdate = false;
    	
     	SQLiteDatabase checkDB = null;
     	 
    	try
    	{
    		String myPath = DB_PATH + mDBName;
    		//try to open the database without checking for the locale
    		//coz our resort info DB might not have the locale defined for
    		//the device currently selected locale(right now, we only define
    		//locale for en_US
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS );
 
    	}
    	catch(SQLiteException e)
    	{
 
    		//database does't exist yet.
 
    	}
 
    	if(checkDB != null)
    	{ 
	    	 //the db exists, let's check if its version matched with the mCurrentVersion
	   		 //now let's fetch all countryRegion information and sorted by id
	   		 String countryQuery = "SELECT * FROM dbVersion";
	   		 
	   		 Cursor c = null;
	   		 try
	   		 {			
	   			c = checkDB.rawQuery(countryQuery, new String[] {});
	   			//Cursor c = db.query("TestTable", null, null, null, null, null, null);
	   		 }
	   		 catch (Exception ce)
	   		 {
	   			Log.e("Error in transaction", ce.toString());
	   		 } 
	   		 finally 
	   		 { 		   			
	   			if( c != null )
	   			{	   				
		   			int idxVersion = c.getColumnIndex( "version" );
		  
					c.moveToPosition(0);
					String existedVersion  =  c.getString(idxVersion);
					
					if( mCurrentVersion.compareTo(existedVersion) > 0 )
					{
						needUpdate = true;
					}
					
					c.close();
	   			}
	   		 }
	   		 
    		 checkDB.close();
    	}
    	else
    	{
    		//the db not existed yet on the /data/data/app.package/databases folder
    		//force to copy the DB from the asset package to that folder
    		needUpdate = true;
    	}
    		
 
    	return  needUpdate;

    }
 
    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException
    {
    	//Open your local db as the input stream
    	InputStream myInput = mContext.getAssets().open(mDBName);
 
    	// Path to the just created empty db
    	String outFileName = DB_PATH + mDBName;
 
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0)
    	{
    		myOutput.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
 
    }
 
    public void openDataBase() throws SQLException
    {
    	//Open the database
        String myPath = DB_PATH+mDBName;
    	mStaticDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS); 
    }
 
    @Override
	public synchronized void close()
    {
    	if(mStaticDataBase != null)
    	{
    		mStaticDataBase.close();
    	}
 
    	super.close();
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		Log.d("Asset DB Helper", "Create the Dabatase now");
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		Log.d("Asset DB Helper", "Update the DB for version " + newVersion);
	}
 
   // Add your public helper methods to access and get content from the database.
   // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
   // to you to create adapters for your views.
 
	public SQLiteDatabase getDatabase( )
	{
		return mStaticDataBase;
	}
	
	private void insertTestItem( )
	{
/*		
		SQLiteDatabase db = this.getWritableDatabase();
		
	     db.execSQL("INSERT INTO TestTable (resortName) VALUES ('test Mountain1');");
	     db.execSQL("INSERT INTO TestTable (resortName) VALUES ('test Mountain2');");
	     db.execSQL("INSERT INTO TestTable (resortName) VALUES ('test Mountain3');");
	     
	     db.close();
*/		
	}
}