package com.reconinstruments.dashlauncher.radar.maps.dal;

import com.reconinstruments.dashlauncher.radar.maps.helpers.AssetDB;
import com.reconinstruments.dashlauncher.radar.maps.objects.CountryInfo;
import com.reconinstruments.dashlauncher.radar.maps.objects.RegionInfo;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortHolder;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortInfo;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortsInfo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

public class ResortsInfoDAL {
	public static final String TAG = "ResortsInfoDAL";
	
	AssetDB mResortInfoDB = null;
	private static final String SQL_COUNTRY_REGION_QUERY	= "SELECT * FROM countryRegion order by id";
	
	public ResortsInfoDAL(Context context){
		mResortInfoDB = new AssetDB(context);
	}
	
	protected void LoadCountriesRegions(SQLiteDatabase resortDB, ResortsInfo resortsInfo) throws Exception {
		resortDB.beginTransaction();
		Cursor cursor = null;
		try
		{		
			cursor = resortDB.rawQuery(SQL_COUNTRY_REGION_QUERY, new String[] {});
			resortDB.setTransactionSuccessful();
			resortDB.endTransaction();

			int idxName = cursor.getColumnIndex( "name" );
			int idxId = cursor.getColumnIndex( "id" );
			
			for (int i = 0; i < cursor.getCount(); ++i)
			{
				cursor.moveToPosition(i);
				
				String countryRegionName	= cursor.getString(idxName).trim();
				int countryRegionID 		= cursor.getInt(idxId);
				
				if(HasRegion(countryRegionName)) {
					String regionName = ExtractRegionName( countryRegionName );
					String countryName = ExtractCountryName( countryRegionName );
					
					CountryInfo countryInfo = resortsInfo.GetCountry(countryName);
					if(countryInfo == null) {
						countryInfo = new CountryInfo(CountryInfo.NOID,countryName);
						resortsInfo.AddCountry(countryInfo);
						
						//Log.d(TAG, "Country : " + countryRegionName);
					}
					
					RegionInfo regionInfo = new RegionInfo(countryRegionID, regionName, countryInfo); 
					countryInfo.AddRegion(regionInfo);
					resortsInfo.AddHolder(countryRegionID, regionInfo);
					//Log.d(TAG, "\tRegion [" + countryRegionID + "] - " + countryRegionName);
				} 
				else {
					CountryInfo countryInfo = new CountryInfo(countryRegionID, countryRegionName); 
					resortsInfo.AddCountry(countryInfo);
					resortsInfo.AddHolder(countryRegionID, countryInfo);
					//Log.d(TAG, "Country [" + countryRegionID + "] - " + countryRegionName);
				}
			}
			cursor.close();			 
		}
		catch (Exception exception)
		{
			Log.e("Failed to LoadCountriesRegions", exception.toString());
			throw new Exception("Failed to LoadCountriesRegions");
		} 
		finally 
		{
			if(resortDB.inTransaction()) {
				resortDB.endTransaction();
			}
		}
	}
	
	protected void LoadResorts(SQLiteDatabase resortDB, ResortsInfo resortsInfo) throws Exception {
		resortDB.beginTransaction();
		Cursor cursor = null;
		try
		{		
			cursor = resortDB.rawQuery("SELECT * FROM resortLocation where mapVersion=="+mResortInfoDB.GetDBVersion()+" order by countryID", new String[] {});
			resortDB.setTransactionSuccessful();
			resortDB.endTransaction();

			int idxId			= cursor.getColumnIndex("id");
			int idxName			= cursor.getColumnIndex("name");
			int idxCRId			= cursor.getColumnIndex("countryID");
			int idxlat			= cursor.getColumnIndex("hubLatitude");
			int idxlng			= cursor.getColumnIndex("hubLongitude");
			int idxMinLat		= cursor.getColumnIndex("minLatitude");
			int idxMaxLat		= cursor.getColumnIndex("maxLatitude");
			int idxMinLng		= cursor.getColumnIndex("minLongitude");
			int idxMaxLng		= cursor.getColumnIndex("maxLongitude");
			int idxMapVersion	= cursor.getColumnIndex("mapVersion");
			 
			int rowCount = cursor.getCount();
			ResortHolder resortHolder = null;
			for( int i = 0; i < rowCount; ++i )
			{
				cursor.moveToPosition(i);
				 
				RectF bbox			= new RectF((float)cursor.getDouble(idxMinLng),
					 						(float)cursor.getDouble(idxMinLat),
					 						(float)cursor.getDouble(idxMaxLng),
					 						(float)cursor.getDouble(idxMaxLat));
				 
				PointF location		= new PointF((float)cursor.getDouble(idxlat),
					 					 	 (float)cursor.getDouble(idxlng));
				 
				int crID 			= cursor.getInt(idxCRId);
				int mapVersion		= cursor.getInt(idxMapVersion);
				int resortID		= cursor.getInt(idxId);
				String resortName	= cursor.getString(idxName);
				 
				if(resortHolder == null) {
					resortHolder = resortsInfo.GetHolder(crID);
				}
				 
				if(resortHolder.ID != crID) {
					resortHolder = resortsInfo.GetHolder(crID);
				}
				
				if(resortHolder == null) {
					 Log.e(TAG, "countryID " + crID + " from resort " + resortName  + " is not defined" );
					 throw new Error( "countryID " + crID + " from resort " + resortName  + " is not defined" );
				}
				
				ResortInfo resortInfo = null;
				if(RegionInfo.IsMyInstance(resortHolder)) {
					resortInfo = new ResortInfo(resortID , resortName, crID, location, mapVersion, bbox, resortHolder.ParentHolder.Name, resortHolder.Name);
				}
				else {
					resortInfo = new ResortInfo(resortID , resortName, crID, location, mapVersion, bbox, resortHolder.Name, null);
				}
				
				resortHolder.AddResort(resortInfo);
				resortsInfo.AddResort(resortInfo);
			}		
			 
			cursor.close();			 
		}
		catch (Exception exception)
		{
			Log.e("Failed to LoadResorts", exception.toString());
			throw new Exception("Failed to LoadResorts");
		} 
		finally 
		{
			if(resortDB.inTransaction()) {
				resortDB.endTransaction();
			}
		}		
	}
	
	public void LoadResortsInfo(ResortsInfo resortsInfo) throws Exception {
		SQLiteDatabase resortDB = null;
		try {
			resortDB = mResortInfoDB.GetDB();
			if(resortDB == null) {
				throw new Exception ("Wasn't able to get resort database");
			}
			
			resortsInfo.SetCorrupted(mResortInfoDB.IsMapDataCorrupted());
			
			LoadCountriesRegions(resortDB, resortsInfo);
			LoadResorts(resortDB, resortsInfo);
		}
		catch (Exception exception) {
			Log.e("Wasn't able to LoadResortsInfo ", exception.toString());
			throw new Exception ("Wasn't able to LoadResortsInfo " + exception.toString()); 
		}
		finally {
			if(resortDB != null) {
				if(resortDB.isOpen()) {
					resortDB.close();
				}
			}
		}
	}
	

	// Check if a name is a country name or a combination of countryRegion
	private boolean HasRegion( String name )
	{
		return name.contains("/");
	}
	
	// Get the country name from a string that might mix with both country and region name
	private String ExtractCountryName( String name )
	{
		return (HasRegion( name )) ? name.substring(0, name.lastIndexOf("/")) : name; 
	}

	private String ExtractRegionName( String name )
	{
		return (HasRegion( name )) ? name.substring(name.lastIndexOf("/") + 1, name.length()) : null;
	}
}
