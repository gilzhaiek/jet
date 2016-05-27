package com.reconinstruments.osmimages;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Environment;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator.MapType;

import java.util.Collections;

/**
 * @author simonwang
 *
 */


public class OsmTileManager {

	private MapView mMap = null;
	private static final String TAG = "MapImages";
	private final static String OSM_TILE_RELATIVE_PATH = "/ReconApps/GeodataService/PreloadedOSMTiles/";
	private StaticMapGenerator mStaticMapGenerator = null;
	private Bitmap mBitmap = null;

	public OsmTileManager(MapView map, StaticMapGenerator staticMapGenerator) {
		this.mMap = map;
		mStaticMapGenerator = staticMapGenerator;
	}
	
	public Bitmap GetBitMapImg(Long tileId) {
		OsmTile tile = new OsmTile(tileId);
		OsmBoundingBox bb = tile.mBound;

		Log.d(TAG, "tileId=" + tileId + ", bound_left=" + bb.mBoundingBox.left
				+ ", bound_right=" + bb.mBoundingBox.right + ", bound_bottom="
				+ bb.mBoundingBox.bottom + ", bound_top=" + bb.mBoundingBox.top);

		
		 GeoRegion geoRegion = new GeoRegion();
		 geoRegion.MakeUsingBoundingBox(bb.mBoundingBox.left,
		 bb.mBoundingBox.top, bb.mBoundingBox.right, bb.mBoundingBox.bottom);
		 float percentWidthMargin = 0.0f, cameraHeading = 0.0f; boolean immediate = true;
		 int width = 512, height = 512;
		 
		 if (mStaticMapGenerator == null) {
			 Log.d(TAG, "mStaticMapGenerator == NULL .");
			 return null;
		 }
		 mBitmap = null;
		 mBitmap = mStaticMapGenerator.GenerateMapImage(width, height, geoRegion, percentWidthMargin, cameraHeading);
		 
		 //mMap.SetCameraToShowGeoRegion(geoRegion, percentWidthMargin, immediate);
		  
		Log.d(TAG, "tileId=" + tileId + ", bound_left=" + bb.mBoundingBox.left
				+ ", bound_right=" + bb.mBoundingBox.right + ", bound_bottom="
				+ bb.mBoundingBox.bottom + ", bound_top=" + bb.mBoundingBox.top);
		
		return mBitmap;
		
	}
	public boolean isNotGetBitmap() {
		return (mBitmap==null);
	}
	public void SaveCurrentBitmap(Long tileId) {

		Log.v(TAG, "SaveCurrentBitmap");

		Bitmap bitmap = null;
		boolean success = true;
		File file = null;
		FileOutputStream fileOutputStream = null;
		String pathName = null;
		String fileName = null;
		String imageFileName = null;
	

		pathName = Environment.getExternalStorageDirectory().toString()
				+ "/ReconApps/mapImg/";
		File folder = new File(pathName);
		if (!folder.exists()) {
			success = folder.mkdirs();
		}
		Log.v(TAG, "SaveCurrentBitmap, " + success + " create folder.");


		fileName = OsmTile.GetTileIdString(tileId);


		imageFileName = fileName;
	
		imageFileName += ".png";
		Log.v(TAG, "SaveCurrentBitmap 2. path=" + pathName
				+ ", imageFileName=" + imageFileName);

		try {
			file = new File(pathName, imageFileName);
			if (file.exists())
				file.delete();
			file.createNewFile();
			fileOutputStream = new FileOutputStream(file);

			bitmap = mBitmap;

			Log.v(TAG, ",SaveCurrentBitmap 5 bitmap="
					+ ((bitmap == null) ? "null" : "OK"));
			if (bitmap == null) {
				Log.v(TAG, "SaveCurrentBitmap.bitmap=null");
				byte outstr[] = "OUt of memory".getBytes();
				fileOutputStream.write(outstr);
				return;
			}

			Log.v(TAG, "SaveCurrentBitmap-6");
			boolean result = bitmap.compress(Bitmap.CompressFormat.PNG, 100,
					fileOutputStream);
			Log.v(TAG, "SaveCurrentBitmap-7, bitmapOK=" + result);

		} catch (Exception e) {
			e.printStackTrace();
			Log.v(TAG, "SaveCurrentBitmap error-0");
			return;

		} finally {


			try {
				mBitmap = null;
				if (fileOutputStream != null) {
					fileOutputStream.flush();
					fileOutputStream.close();
					fileOutputStream = null;
				}
			} catch (Exception e) {
				Log.v(TAG, "SaveCurrentBitmap error-2");
			}

		}// eof finally
		Log.v(TAG, "SaveCurrentBitmap 9");

	} // eof SaveCurrentBitmap



	public void RenderTile(Long tileId) {

		OsmTile tile = new OsmTile(tileId);
		OsmBoundingBox bb = tile.mBound;

		Log.d(TAG, "tileId=" + tileId + ", bound_left=" + bb.mBoundingBox.left
				+ ", bound_right=" + bb.mBoundingBox.right + ", bound_bottom="
				+ bb.mBoundingBox.bottom + ", bound_top=" + bb.mBoundingBox.top);

		
		 GeoRegion geoRegion = new GeoRegion();
		 geoRegion.MakeUsingBoundingBox(bb.mBoundingBox.left,
		 bb.mBoundingBox.top, bb.mBoundingBox.right, bb.mBoundingBox.bottom);
		 float percentWidthMargin = 0.0f; boolean immediate = true;
		 
		 mMap.SetCameraToShowGeoRegion(geoRegion, percentWidthMargin, immediate);
		 
		 /*
		 GeoRegion newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(
					-123.2088f, 49.3250f, -123.0321f, 49.2435f);
		 mMap.SetCameraToShowGeoRegion(newGeoRegion, 0.0f, true);
		*/
		 
		Log.d(TAG, "tileId=" + tileId + ", bound_left=" + bb.mBoundingBox.left
				+ ", bound_right=" + bb.mBoundingBox.right + ", bound_bottom="
				+ bb.mBoundingBox.bottom + ", bound_top=" + bb.mBoundingBox.top);

	}

	public void SaveCurrentTileImage(Long tileId) {

		Log.v(TAG, "SaveCurrentTileImage");

		Bitmap bitmap = null;
		boolean success = true;
		File file = null;
		FileOutputStream fileOutputStream = null;
		String pathName = null;
		String fileName = null;
		String imageFileName = null;
	

		pathName = Environment.getExternalStorageDirectory().toString()
				+ "/ReconApps/mapImg/";
		File folder = new File(pathName);
		if (!folder.exists()) {
			success = folder.mkdirs();
		}
		Log.v(TAG, "SaveCurrentTileImage, " + success + " create folder.");


		fileName = OsmTile.GetTileIdString(tileId);


		imageFileName = fileName;
	
		imageFileName += ".png";
		Log.v(TAG, "SaveCurrentTileImage 2. path=" + pathName
				+ ", imageFileName=" + imageFileName);

		try {
			file = new File(pathName, imageFileName);
			if (file.exists())
				file.delete();
			file.createNewFile();
			fileOutputStream = new FileOutputStream(file);

			bitmap = mMap.getBackgroundImage();

			Log.v(TAG, ",SaveCurrentTileImage 5 bitmap="
					+ ((bitmap == null) ? "null" : "OK"));
			if (bitmap == null) {
				Log.v(TAG, "SaveCurrentTileImage.bitmap=null");
				byte outstr[] = "OUt of memory".getBytes();
				fileOutputStream.write(outstr);
				return;
			}

			Log.v(TAG, "SaveCurrentTileImage-6");
			boolean result = bitmap.compress(Bitmap.CompressFormat.PNG, 100,
					fileOutputStream);
			Log.v(TAG, "SaveCurrentTileImage-7, bitmapOK=" + result);

		} catch (Exception e) {
			e.printStackTrace();
			Log.v(TAG, "SaveCurrentTileImage error-0");
			return;

		} finally {


			try {
				if (fileOutputStream != null) {
					fileOutputStream.flush();
					fileOutputStream.close();
					fileOutputStream = null;
				}
			} catch (Exception e) {
				Log.v(TAG, "SaveCurrentTileImage error-2");
			}

		}// eof finally
		Log.v(TAG, "SaveCurrentTileImage 9");



	} // eof SaveCurrentTileImage

	
	public ArrayList<Long> GetTileIdListFromStorage() {
	
		ArrayList<Long> result = new ArrayList<Long>();

		String path = GetOSMTilePath();

		ArrayList<File> inFiles = new ArrayList<File>();
		
		File parentDir = new File(path);
		Log.v(TAG, "GetTileIdListFromStorage,  path=" + path);
		if (!parentDir.exists()) {
			return result;
		}

		Long id = 0L;
		String idString = null;
		File[] files = parentDir.listFiles();
		for (File file : files) {
			Log.v(TAG, "GetTileIdListFromStorage0, str= " + file.getName());
			idString = file.getName().substring(0, file.getName().indexOf("."));
			Log.v(TAG, "GetTileIdListFromStorage1, str= " + idString);
			try {
				id = Long.parseLong(idString);
				
				//work around solution for outofMemory error
				//if (id.longValue() <= 53548100)
				//	continue;
				//end
				
				result.add(id);
			} catch (Exception e) {
				Log.v(TAG, "GetTileIdListFromStorage, str= " + idString);
				continue;
			}

		}
		Log.v(TAG, "GetTileIdListFromStorage, list_size= " + result.size());
		
		//sort the list
		Collections.sort(result);
 
		//after sorted
		System.out.println("ArrayList is sorted");
		for(long temp: result){
			System.out.println(temp);
		}
		return result;
	}

	public static String GetOSMTilePath() {
		return Environment.getExternalStorageDirectory().getPath()
				+ OSM_TILE_RELATIVE_PATH;
	}

	public static Point GetTileCenter(Integer tileId) {
		Point result = new Point();
		return result;
	}

}
