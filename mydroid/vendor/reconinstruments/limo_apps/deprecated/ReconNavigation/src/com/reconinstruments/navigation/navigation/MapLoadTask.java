/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.datamanagement.DataDecoder;
import com.reconinstruments.navigation.navigation.datamanagement.DataDecoder.InvalidMapDataException;
import com.reconinstruments.navigation.routing.ReManager;
import com.reconinstruments.navigation.routing.ReUtil;

/**
 * This an asynchronized task for loading trails, areas and point-of-interest shape
 * map of a given Resort; The loading is performed in a separate thread independent of
 * the UI thread. While the loading is on-going, a progress dialog will be showing on the
 * UI thread to give users a hint. After loading, the map will be attached to the map-view
 * and be rendered.
 */
public class MapLoadTask extends AsyncTask<String, Void, Void> 
{
	public interface IMapLoadCallback
	{
		void onPreExecute();			//extra call back that will be called in onPreExecute() of the MapLoadTask
		void onPostExecute();			//extra call back that will be called in onPostExecut() of the MapLoadTask;
	}
	
	//loading status 
	static final int LOADING_STATUS_NOTSTARTED = 0;
	static final int LOADING_STATUS_LOADING = 1;
	static final int LOADING_STATUS_INVALID_ASSET = 2;
	static final int LOADING_STATUS_NO_COMPRESS_MAP = 3;
	static final int LOADING_STATUS_SUCCEED = 4;
	
	static final String MAP_DATA_ZIPFILE = "mdmaps.zip";
	static final boolean LOAD_COMPRESSED_MAP = true;
	
	private ProgressDialog mProgressDialog = null; // the progress dialog to
													// show before starting load
													// map files
	private Context mContext = null; 		// the context under which the MapLoadTask is launched
	private ShpMap mMap = null;     		// the Shape map to be filled by this task
	private ShpMapLoader mMapLoader = null; // shape map loader
	private MapView mMapView = null;		//the mapView to bind to the map after finish loading
	private ReManager mReManager = null;
	private IMapLoadCallback mMapLoadCallback = null;
	private DataDecoder mDecoder = null;
	private int mLoadStatus = LOADING_STATUS_NOTSTARTED;
	private ZipFile mMapZip = null;

	public MapLoadTask(Context context, MapView mapView, String resortName, ShpMap map, ReManager reManager, IMapLoadCallback mapLoadCallback ) 
	{
		mContext = context;
		mMap = map;
		mMapLoader = new ShpMapLoader();
		mMapView = mapView;
		map.mResortName = resortName;
		mReManager = reManager;
		mMapLoadCallback = mapLoadCallback;
		mDecoder = new DataDecoder();
	}

	/**
	 * This method will be called in the UI thread
	 * by the main Activity before the map-loading task
	 * starts executing
	 */
	protected void onPreExecute() 
	{
		//clear whatever content that the map has now
		mMap.reset();
		
		//reset the SHPCONTENT_INIT to false to tell ShpMapLoader
		//to init the world to local transform matrix after loading
		//the first shp map file, which should be trails shape file
		Util.SHPCONTENT_INIT = false;
		

		//unbind the map from the mapView since the map will be updated
		//in the loading thread
		mMapView.setMap( null );
		
		if( mMapLoadCallback != null )
		{
			mMapLoadCallback.onPreExecute();
		}
		
		String content = mContext.getResources().getString(R.string.map_loading_prompt) + " " +  mMap.mResortName;
		
		mProgressDialog = ProgressDialog.show(
				mContext, mContext.getResources().getString(R.string.wait_for_loading),
				 Html.fromHtml("<big>" + content +  "</big>")
				 );
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

		if( mReManager != null )
		{
			mReManager.dumpNetworks();
		}

		//bind the map to the mapview
		mMapView.setMap( mMap );
		
		if( mMapLoadCallback != null )
		{
			mMapLoadCallback.onPostExecute();
		}
		
		//ask the mapview to fresh its rendering
		mMapView.invalidate();
		
		if( mLoadStatus == LOADING_STATUS_INVALID_ASSET )
		{
			AlertDialog.Builder dlg = new AlertDialog.Builder( mContext );
			dlg.setCancelable(true);
			dlg.setIcon(R.drawable.alert_dialog_icon);
			dlg.setTitle( mContext.getResources().getString(R.string.invalid_map_dialog_title));
			dlg.setMessage( mContext.getResources().getString(R.string.invalid_map_warning));
			dlg.show();		
		}
		else if( mLoadStatus == LOADING_STATUS_NO_COMPRESS_MAP )
		{
			AlertDialog.Builder dlg = new AlertDialog.Builder( mContext );
			dlg.setCancelable(true);
			dlg.setIcon(R.drawable.alert_dialog_icon);
			dlg.setTitle( mContext.getResources().getString(R.string.miss_map_dialog_title));
			dlg.setMessage( mContext.getResources().getString(R.string.miss_map_warning));
			dlg.show();		
			
		}
	}

	protected Void doInBackground(String... files) 
	{
		//make sure that we load the trails shape file first, 
		//because we have to initialize the Util's world to
		//local transform by call Util.initShpContentMap( ) inside
		//the ShpMapLoader
		
		try
		{
			mLoadStatus = LOADING_STATUS_LOADING;
			
			if( LOAD_COMPRESSED_MAP )
			{
				//check if the compressed map data exists or not
				//if not, exit prematurely, and set the loading status to 
				//a invalid loading status
				String assetFolder = com.reconinstruments.navigation.navigation.datamanagement.ResortInfoProvider.getAssetFolder();
				
				File mapZipFile = new File( assetFolder + MAP_DATA_ZIPFILE );
				
				if( mapZipFile.exists() == false || mapZipFile.canRead() == false )
				{
					mLoadStatus = LOADING_STATUS_NO_COMPRESS_MAP;
					return null;
				}
				else
				{
					mMapZip = new ZipFile( mapZipFile );
				}
			}

			// load in all trails
			Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Start Loading trails..." + files[0] + "..." + files[1]);
			String shpFile = "lines/" + files[0];
			String dbfFile = "lines/" + files[1];
			loadShpFile(shpFile, dbfFile);
			Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Finish Loading trails...");
	
			
			// load in all areas
			Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Start Loading areas..."+ files[0] + "..." + files[1]);
			shpFile = "areas/" + files[0];
			dbfFile = "areas/" + files[1];
			loadShpFile(shpFile, dbfFile);
			Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Finish Loading areas...");
	
			// load in all point-of-interests
			Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Start Loading point-of-interests..."+ files[0] + "..." + files[1]);
			shpFile = "points/" + files[0];
			dbfFile = "points/" + files[1];
			loadShpFile(shpFile, dbfFile);
			Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Finish Loading point-of-interests...");
	
			mMapZip.close();
			
			mLoadStatus = LOADING_STATUS_SUCCEED;
			
			//debug dump information
			if( DebugUtil.DEBUG_DUMP )
			{
				int idx;
				for( idx = 0; idx < Trail.NUM_TRAIL_TYPES; ++idx )
				{
					Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Trail type #" + idx +" Number of trails: " + mMap.mTrails.get(idx).size() );
				}
				
				for( idx = 0; idx < Area.NUM_AREA_TYPES; ++idx )
				{
					Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Area type #" + idx +" Number of areas: " + mMap.mAreas.get(idx).size() );
				}
		
				for( idx = 0; idx < PoInterest.NUM_POI_TYPE; ++idx )
				{
					Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "POI type #" + idx +" Number of POIs: " + mMap.mPoInterests.get(idx).size() );
				}
				
			}

			if( ReUtil.RE_VERIFY_TOPOLOGY )
			{
				mReManager.verifyTopology();
			}
			
		}
		catch( DataDecoder.InvalidMapDataException invalidMapData )
		{
			//one or more .shp/.dbf file is invalid, let's clear out the
			//map since it might be just parly-loaded
			mMap.reset();
			if( mMapZip != null )
			{
				try
				{
					mMapZip.close();
				}
				catch( Exception e)
				{
					
				}
			}

			mLoadStatus = LOADING_STATUS_INVALID_ASSET;
		}
		catch (IOException e) 
		{
			e.printStackTrace(System.out);
			Log.e(DebugUtil.LOG_TAG_LOADING, "ShpMap - Loading shapefile" 
					+ "failed", e);
			throw new RuntimeException(e);
		}
		// we are not interested in return anything, so just null here
		return null;

	}

	/**
	 * since the task does not report back progress(coz
	 * we dont know about it), this class do nothing
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
	 * 
	 * @param shpFileName
	 * @param dbfFileName
	 * @throws DataDecoder.InvalidMapDataException if one of the .shp/.dbf is invalid
	 */
	protected void loadShpFile(String shpFileName, String dbfFileName) throws DataDecoder.InvalidMapDataException
	{

		FileInputStream shpStream = null;
		FileInputStream dbfStream = null;
		
		try 
		{
			
			if( LOAD_COMPRESSED_MAP )
			{
					
				if( mMapZip != null )
				{
					ByteBuffer shpByteBuffer = null;
					ByteBuffer dbfByteBuffer = null;
					
					//take shpFileName from the zip file
					ZipEntry entry = mMapZip.getEntry(shpFileName);
					if( entry != null )
					{
						InputStream inputStream = mMapZip.getInputStream(entry);
						int len = (int)entry.getSize();
						byte[] content = new byte[len];
						int offset = 0;
						while( len > 0 )
						{
							int actualRead = inputStream.read( content, offset, len );
							offset += actualRead;
							len -= actualRead;
						}
						
						byte[] decodedContent = mDecoder.Decode(content);
						shpByteBuffer = ByteBuffer.wrap( decodedContent );
						inputStream.close();					
						
					}

					entry = mMapZip.getEntry(dbfFileName);
					if( entry != null )
					{
						InputStream inputStream = mMapZip.getInputStream(entry);
						int len = (int)entry.getSize();
						byte[] content = new byte[len];
						int offset = 0;
						while( len > 0 )
						{
							int actualRead = inputStream.read( content, offset, len );
							offset += actualRead;
							len -= actualRead;
						}
						
						byte[] decodedContent = mDecoder.Decode(content);
						dbfByteBuffer = ByteBuffer.wrap( decodedContent );
						inputStream.close();					
						
					}
															
					//take the dbfFileName from the zipFile
					mMapLoader.loadMap(shpByteBuffer, dbfByteBuffer, mMap, mReManager);
					
					
				}
			}
			else
			{
				String assetFolder = com.reconinstruments.navigation.navigation.datamanagement.ResortInfoProvider.getAssetFolder();
				
				// create a ByteBuffer for shape file
				File file = new File( assetFolder + shpFileName  );
				shpStream = new FileInputStream( file );
				byte[] content = new byte[(int)file.length()];
				shpStream.read( content, 0, (int)file.length() );	
				byte[] decodedContent = mDecoder.Decode(content);
				ByteBuffer shpByteBuffer = ByteBuffer.wrap( decodedContent );
				shpStream.close();
				
				// create a ByteBuffer for dbf file
				file = new File( assetFolder + dbfFileName  );
				dbfStream = new FileInputStream( file );
				content = new byte[(int)file.length()];			
				dbfStream.read( content, 0, (int)file.length() );
				decodedContent = mDecoder.Decode(content);
				ByteBuffer dbfByteBuffer = ByteBuffer.wrap( decodedContent );
				dbfStream.close();

				mMapLoader.loadMap(shpByteBuffer, dbfByteBuffer, mMap, mReManager);
				
			}

		} 
		catch( InvalidMapDataException invalidMap )
		{
			Log.e(DebugUtil.LOG_TAG_LOADING, shpFileName + " or " + dbfFileName + " is not valid asset" );
			
			//invalid map file hit, let's close any pending InputStream for a clean up
			try
			{
				if( shpStream != null )
				{
					shpStream.close();
				}
				
				if( dbfStream != null )
				{
					dbfStream.close();
				}
			}
			catch (IOException ioEx )
			{
			
			}
			//rethrow the exception so that the caller can catch it and do some post processing
			throw invalidMap;
		}
		catch (IOException e) 
		{
			e.printStackTrace(System.out);
			Log.e(DebugUtil.LOG_TAG_LOADING, "ShpMap - Loading shapefile" + shpFileName
					+ "failed", e);
			
			//un-recoverable error.
			throw new RuntimeException(e);
		}
	}

}