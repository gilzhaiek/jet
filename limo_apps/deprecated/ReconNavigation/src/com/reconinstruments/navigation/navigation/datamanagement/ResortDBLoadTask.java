/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;
import android.text.Html;


/**
 * This an asynchronized task for loading a database at the right beginning
 * of the application.  The database has the informations of all the
 * ski resorts, such as the name, the hub location, the bounding box(in 
 * latitude and longitude), the id of the country it locates on, whether we have
 * a mountain dynamic map for this site or not etc etc.
 */
public class ResortDBLoadTask extends AsyncTask<String, Void, Void> 
{
	public interface IPostLoadedCallback
	{
		void onPostDBLoaded();
	}
	
	private ProgressDialog mProgressDialog = null; // the progress dialog to
													// show before starting load
													// map files
	private Context mContext = null; 				// the context under which the MapLoadTask is launched
	private AssetDBHelper mAssetDB = null;			// the DB helper for create an database from asset
	private String mLoadDialogTitle = null;
	private String mLoadDialogContent = null;
	private String  mExpectedDBVersion = null;		//the expected DB version to open. This is defined as a string asset by the
													//application. Bump it to higher version, if the DB shipped with .apk has
													//been updated. When AssetDBHelper detected that the dbVersion defined in the 
													//DB is different from the expected version, the new version will be copied
													//from .apk assets to the device mass storage
	private IPostLoadedCallback mPostLoadCallback = null;

	public ResortDBLoadTask(Context context, String loadDialogTitle, String loadDialogContent, String expectedDBVersion, IPostLoadedCallback callBack ) 
	{
		mContext = context;
		mLoadDialogTitle = loadDialogTitle;
		mLoadDialogContent = loadDialogContent;
		mExpectedDBVersion = expectedDBVersion;
		mPostLoadCallback = callBack;
	}

	/**
	 * This method will be called in the UI thread
	 * by the main Activity before the map-loading task
	 * starts executing
	 */
	protected void onPreExecute() 
	{
		ResortInfoProvider.reset();
			 
		//mProgressDialog = ProgressDialog.show(mContext, mLoadDialogTitle, mLoadDialogContent );
		mProgressDialog = ProgressDialog.show(mContext, mLoadDialogTitle, Html.fromHtml("<big>" + mLoadDialogContent +  "</big>") );
				
	}

	/**
	 * This method will be called in the UI thread
	 * by the main Activity after the map-loading task
	 * has been executed. Since no result value returned by
	 * doInBackground, so we pass the a Void result to this
	 * function
	 */
	protected void onPostExecute(Void result) 
	{
		mProgressDialog.dismiss();
		if( mPostLoadCallback != null )
		{
			mPostLoadCallback.onPostDBLoaded();
		}
	}

	protected Void doInBackground(String... dbName) 
	{
		try
		{
			//enumerate all available assets on this device
			ResortInfoProvider.sAssetEnumerator.Enumerate();
		}
		catch( FileNotFoundException e)
		{
			//no mapdata created yet, it is normal, just mean
			//no data has been pushed to device yet
			
			//AlertDialog.Builder dlg = new AlertDialog.Builder( mContext );
			//dlg.setMessage( ResortInfoProvider.getAssetFolder() + " is not existed on the file system of this device" );
			//dlg.show();			
		}
		
		mAssetDB = new AssetDBHelper( mContext, dbName[0], mExpectedDBVersion );
		
		
		//create a DB from asset
		try 
		{
			mAssetDB.createDataBase();
		} 
		catch (IOException e) 
		{
			throw new Error("Error copying database");
		}

		//open the DB
		 try 
		 {
			 mAssetDB.openDataBase(); 
		 } 
		 catch( SQLException sqlE ) 
		 { 
			 throw new Error("Error on opening database"); 
		 }

		 SQLiteDatabase db = mAssetDB.getDatabase();
		 
		 //now let's fetch all countryRegion information and sorted by id
		 String countryQuery = "SELECT * FROM countryRegion order by id";
		 
		 db.beginTransaction();
		 Cursor c = null;
		 try
		 {			
			c = db.rawQuery(countryQuery, new String[] {});
			//Cursor c = db.query("TestTable", null, null, null, null, null, null);
			db.setTransactionSuccessful();
		 }
		 catch (Exception ce)
		 {
			Log.e("Error in transaction", ce.toString());
		 } 
		 finally 
		 {
			db.endTransaction();
		
			int idxName = c.getColumnIndex( "name" );
			int idxId = c.getColumnIndex( "id" );
			
			for (int i = 0; i < c.getCount(); ++i)
			{
				c.moveToPosition(i);
				String countryRegionName =  c.getString(idxName);
				int countryId = c.getInt(idxId);
				countryRegionName.trim();
				ResortInfoProvider.addMapping(countryId,countryRegionName);		
				
				//create the countryInfo or regionInfo for this entry if necessary
				if( hasRegion( countryRegionName) )
				{
					String regionName = getRegionName( countryRegionName );
					String countryName = getCountryName( countryRegionName );
					CountryInfo countryInfo = ResortInfoProvider.getCountryInfo(countryName);
					if( countryInfo == null )
					{
						//created a new CountryInfo, passing the UNDFINED as the countryID, 
						//since this country has sub-region, which has unique id instead
						ResortInfoProvider.addCountry(CountryInfo.COUNTRY_ID_UNDEFINED, countryName);
						countryInfo = ResortInfoProvider.getCountryInfo(countryName);
					}
					//create a new region					
					countryInfo.addRegion( countryId, regionName);
				}
				else
				{
					ResortInfoProvider.addCountry(countryId, countryRegionName);
				}
			}
			Log.d( "Resort DB Loading", "Find totally: " + c.getCount() + " records");
		
			c.close();
		 }

		 //now let's fetch all resortLocation information from the database
		 String resortQuery = "SELECT * FROM resortLocation where mapVersion=1 order by countryID";
		 db.beginTransaction();
		 try
		 {
			 c = db.rawQuery( resortQuery, new String[]{} );
			 db.setTransactionSuccessful();
		 }
		 catch(Exception ce)
		 {
			 Log.e("Error in transaction", ce.toString());
		 }
		 finally
		 {
			 //finish the transaction
			 db.endTransaction();
			 
			 int idxId = c.getColumnIndex("id");
			 int idxName = c.getColumnIndex("name");
			 int idxCountryId = c.getColumnIndex("countryID");
			 int idxlat = c.getColumnIndex("hubLatitude");
			 int idxlng = c.getColumnIndex("hubLongitude");
			 int idxMinLat = c.getColumnIndex("minLatitude");
			 int idxMaxLat = c.getColumnIndex("maxLatitude");
			 int idxMinLng = c.getColumnIndex("minLongitude");
			 int idxMaxLng = c.getColumnIndex("maxLongitude");
			 int idxMapVersion = c.getColumnIndex("mapVersion");
			 
			 int rowCount = c.getCount();
			 CountryInfo countryInfo = null;
			 for( int i = 0; i < rowCount; ++i )
			 {
				 c.moveToPosition(i);
				 RectF bbox = new RectF( (float)c.getDouble(idxMinLng), (float)c.getDouble(idxMinLat), (float)c.getDouble(idxMaxLng), (float)c.getDouble(idxMaxLat));
				 PointF location = new PointF( (float)c.getDouble(idxlat), (float)c.getDouble(idxlng));
				 int countryId = c.getInt(idxCountryId);
				 int mapVersion = c.getInt(idxMapVersion);
				 ResortInfo resort = new ResortInfo( c.getInt(idxId), c.getString(idxName), countryId, location, mapVersion, bbox );
				 
				 //check for availability of this resort
				 resort.mIsAvailable = ResortInfoProvider.sAssetEnumerator.isAvailable(resort.mAssetBaseName);
				 
				 //look for the countryInfo that should contains this resort
				 if( countryInfo == null || countryInfo.isMyID( countryId ) == false )
				 {
					 countryInfo = ResortInfoProvider.getCountryInfo(countryId);
				 }
				 
				 if( countryInfo == null )
				 {
					 Log.e("ResortDB", "The country ID# " + countryId + "from resort " + c.getString(idxName )  + " is not defined"  );
					 throw new Error( "The country ID# " + countryId + " is not defined");
				 }
				 else
				 {
					 countryInfo.addResort(resort);
				 }
			 }		
			 
			 c.close();
		 }
		 
		 //compact the generated list afterwards to get rid of empty country and region
		 ResortInfoProvider.compactList();
		 
		 //we are done with the db, close it
		 mAssetDB.close();
		 // we are not interested in return anything, so just null here
		 return null;
	}

	/**
	 * since the task does not report back progress(coz
	 * we dont know about it), this function does nothing
	 * and the parameter type is defined as Void(the second
	 * the parameter type of MapLoadTask
	 */
	protected void onProgressUpdate(Void... progress) 
	{
		// do nothing here, since we dont know the exact progress
		// instead, we let the ProgressDialog gives user a hint that
		// we are loading
	}


	/**
	 * Check if a name is a country name or a combination of countryRegion
	 */
	private boolean hasRegion( String name )
	{
		if( name.contains("/") )
		{
			return true;
		}
		else
		{
			return false;			
		}
	}
	
	/**
	 * Get the country name from a string that might mix with both country and region name
	 */
	private String getCountryName( String name )
	{
		if( hasRegion( name ) )
		{
			int idx = name.lastIndexOf("/");
			return name.substring(0, idx);
		}
		else
		{
			return name;
		}
	}

	private String getRegionName( String name )
	{
		if( hasRegion( name ) )
		{
			int idx = name.lastIndexOf("/");
			return name.substring(idx + 1, name.length());
		}
		else
		{
			return null;
		}
	}

}