package com.reconinstruments.mapImages.bundlers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.mapImages.MapsManager;
import com.reconinstruments.mapImages.bo.MapDrawingsBO;
import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.objects.MapImagesInfo;
import com.reconinstruments.mapImages.objects.ResortInfo;

import android.os.Environment;

public class MapBundler extends BundlerBase {
	protected static final String TAG = "MapBundler";
	
	protected static final double MAP_VERSION = .56; 
	protected static final int MAX_BITMAP_SEGMENT_SIZE 	= 1024; // pixels
	protected static final int MAX_BITMAP_RESORT_SIZE 	= 2048; // pixels
		
	public static MapImagesInfo GetMapImagesInfo(Bundle bundle) {
		Bundle segmentsBundle = bundle.getBundle("SegmentsBundle");
		if (segmentsBundle == null) {
			Log.v(TAG,"MapImagesInfo, ###SegmentsBundle=null");
			return null;
		}
		
		MapImagesInfo mapImagesInfo = new MapImagesInfo(segmentsBundle.size());
		
		for(int i = 0; i < segmentsBundle.size(); i++){
			Bundle segmentBundle = segmentsBundle.getBundle(Integer.toString(i));
			mapImagesInfo.AddImageInfo(segmentBundle.getString("SegmentTrailsFileName"), segmentBundle.getString("SegmentNoTrailFileName"), GetRect("SegmentLocation", segmentBundle));
		}  
		
		return mapImagesInfo;
	}
	
	public static MapDrawings GetMapDrawings(Bundle bundle) { 
		MapDrawings mapDrawings = new MapDrawings();
		
		mapDrawings.SetResortInfo(ResortInfoBundler.GenerateResortInfo(bundle.getBundle("ResortInfo")));
		mapDrawings.SetTransformedBoundingBox(GetRectF("TransformedBoundingBox", bundle));
		
		int arraySize = bundle.getInt("POIs.size");
		Bundle tmpBundle = bundle.getBundle("POIs");
		for(int i = 0; i < arraySize; i++) {
			mapDrawings.AddPOIDrawing(DrawingBundler.GeneratePOIDrawing(tmpBundle.getBundle(Integer.toString(i))));
		}
		Log.v(TAG,"POIs.size="+arraySize);
		
		arraySize = bundle.getInt("Trails.size");
		tmpBundle = bundle.getBundle("Trails");
		for(int i = 0; i < arraySize; i++) {
			mapDrawings.AddTrailDrawing(DrawingBundler.GenerateTrailDrawing(tmpBundle.getBundle(Integer.toString(i))));
		}
					
		arraySize = bundle.getInt("Areas.size");
		tmpBundle = bundle.getBundle("Areas");
		for(int i = 0; i < arraySize; i++) {
			mapDrawings.AddAreaDrawing(DrawingBundler.GenerateAreaDrawing(tmpBundle.getBundle(Integer.toString(i))));
		}
					
		return mapDrawings;
	}
	
	public static void CreateResortFile(Context context, int resortID, double mapVersion, String fileName) {
		FileOutputStream fileOutputStream = null;		
		DataOutputStream dataOutputStream = null;
		
		try {
			fileOutputStream = context.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
			
			dataOutputStream = new DataOutputStream(fileOutputStream);
			
			dataOutputStream.writeUTF(Integer.toString(resortID));
			dataOutputStream.writeUTF(Double.toString(mapVersion));
		} catch (IOException e) {
		} finally {
			try {if (dataOutputStream != null)	dataOutputStream.close();}	catch (IOException ex) {}
			try {if (fileOutputStream != null)	fileOutputStream.close();}	catch (IOException ex) {}
		}
	}
	
	protected static boolean DeviceHasResort(Context context, int resortID, double mapVersion, String fileName) {
		FileInputStream fileInputStream = null;		
		DataInputStream dataInputStream = null;
		
		try {			
			fileInputStream = context.openFileInput(fileName);
			
			if(fileInputStream.available() == 0){
				Log.v(TAG, "fileInputStream.available() == 0");
				return false;
			}
			
			dataInputStream = new DataInputStream(fileInputStream);
			
			String readStr = dataInputStream.readUTF();
			if(!Integer.toString(resortID).equals(readStr)) {
				Log.v(TAG, "resortID = " + Integer.toString(resortID) + " != readStr="+readStr);
				return false;
			}
				
			readStr = dataInputStream.readUTF();
			if(!Double.toString(mapVersion).equals(readStr)) {
				Log.v(TAG, "mapVersion = " + Double.toString(mapVersion) + " != readStr="+readStr);
				return false;
			}
			
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {if (dataInputStream != null)	dataInputStream.close();}	catch (IOException ex) {}
			try {if (fileInputStream != null)	fileInputStream.close();}	catch (IOException ex) {}
		}
	}
	
	public static void DeleteResortsFiles(String path){
		try {
			File dir = new File(path);
			if (dir.isDirectory()) {
				String[] children = dir.list();
				for (int i = 0; i < children.length; i++) {
					new File(dir, children[i]).delete();
				}
			}
		} catch (Exception e) {}
	}
	
	public static void GenerateMapImages(Context context, Bundle bundle, String fileName, MapDrawings mapDrawings){
		try {
	    	
	    	//Log.v(TAG,"GenerateMapImages-");
			
			Bitmap bitmap = null;
	    	
	    	String pathName = context.getFilesDir() + "/";

	    	
	    	int totalWidth = (int)mapDrawings.mTransformedBoundingBox.width();
	    	int totalHeight = (int)mapDrawings.mTransformedBoundingBox.height();
	    	
	    	boolean deviceHasResort = DeviceHasResort(context, mapDrawings.mResortInfo.ResortID, MAP_VERSION, fileName);
	    	if(!deviceHasResort) {
	    		DeleteResortsFiles(pathName);
	    	}
	    	
	    	Bundle segmentsBundle = new Bundle(); 
	    	int cnt = 0;
	    	for(int sHeight = 0; sHeight < totalHeight; sHeight += MAX_BITMAP_SEGMENT_SIZE ) {
	    		for(int sWidth = 0; sWidth < totalWidth; sWidth += MAX_BITMAP_SEGMENT_SIZE ) {
	    			int eHeight = ((sHeight  + MAX_BITMAP_SEGMENT_SIZE) > totalHeight) ? totalHeight : sHeight  + MAX_BITMAP_SEGMENT_SIZE;
	    			int eWidth = ((sWidth  + MAX_BITMAP_SEGMENT_SIZE) > totalWidth) ? totalWidth : sWidth  + MAX_BITMAP_SEGMENT_SIZE;
	    			
	    			bitmap = null;
	    			Bundle segmentBundle = new Bundle();
	    			
	    			AddRect("SegmentLocation", segmentBundle, new Rect(sWidth, sHeight, eWidth, eHeight));
	    			
	    			String imageFileName = fileName + "_nt_"+cnt+".png";
	    			segmentBundle.putString("SegmentNoTrailFileName", pathName + imageFileName);
	    			
	    			
	    			FileOutputStream fileOutputStream = null;
	    			if(!deviceHasResort || !(new File(pathName + imageFileName)).exists()) {
		    	    	try {
			    	    	bitmap = MapDrawingsBO.GetMapSegmentBitmap(mapDrawings, new Rect(sWidth, sHeight, eWidth, eHeight), false);
			    	    	fileOutputStream = context.openFileOutput(imageFileName, Context.MODE_WORLD_WRITEABLE);
			    	    	bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
		    	    	} catch (Exception e) {Log.v(TAG,"Failed to save bitmap nt");return;}
		    	    	finally {
		    	    		try { if(bitmap != null) 			bitmap.recycle();}			catch (Exception e) {}
		    	    		try { if(fileOutputStream != null)	fileOutputStream.close();}	catch (Exception e) {}
		    	    	}
	    			}
	    	    	
	    	    	imageFileName = fileName + "_wt_"+cnt+".png";
	    			segmentBundle.putString("SegmentTrailsFileName", pathName + imageFileName);
	    			fileOutputStream = null;
	    			if(!deviceHasResort || !(new File(pathName + imageFileName)).exists()) {
		    	    	try {
			    	    	bitmap = MapDrawingsBO.GetMapSegmentBitmap(mapDrawings, new Rect(sWidth, sHeight, eWidth, eHeight), true);
			    	    	fileOutputStream = context.openFileOutput(imageFileName, Context.MODE_WORLD_WRITEABLE);
			    	    	bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
		    	    	} catch (Exception e) {Log.v(TAG,"Failed to save bitmap nt");return;}
		    	    	finally {
		    	    		try { if(bitmap != null) 			bitmap.recycle();}			catch (Exception e) {}
		    	    		try { if(fileOutputStream != null)	fileOutputStream.close();}	catch (Exception e) {}
		    	    	}
	    			}
	    	    	
	    	    	segmentsBundle.putBundle(Integer.toString(cnt), segmentBundle);
	    	    	cnt++;
	    		}
	    	}
	    	
	    	CreateResortFile(context, mapDrawings.mResortInfo.ResortID, MAP_VERSION, fileName);
	    	bundle.putBundle("SegmentsBundle", segmentsBundle);
		}
		catch (Exception e) {} 
	}


	public static void GenerateResortMapImgs(Context context,  MapDrawings mapDrawings){
		
    	Log.v(TAG,"GenerateResortMapImgs-");
		
		Bitmap bitmap = null;
	 	boolean success = true;
		File file = null;
		FileOutputStream fileOutputStream = null;
		String pathName = null;
		String fileName = null;
		String imageFileName = null; 
		boolean needMoreBitmap = false;
		Rect rect = null;
		int max_width_size = MAX_BITMAP_RESORT_SIZE;
		int max_height_size = MAX_BITMAP_RESORT_SIZE;

		pathName = Environment.getExternalStorageDirectory().toString() +"/ReconApps/mapImg/";
    	File folder = new File(pathName);
         if (!folder.exists()) 
         {
             success = folder.mkdirs();
         }
         Log.v(TAG,"GenerateResortMapImgs, " + success+" create folder.");
        
    	int totalWidth = (int)mapDrawings.mTransformedBoundingBox.width();
    	int totalHeight = (int)mapDrawings.mTransformedBoundingBox.height();
    
    	//need one or multiple Bitmap?
    	try {
    		bitmap = Bitmap.createBitmap(totalWidth,  totalHeight, Config.ARGB_4444);
    		max_width_size = totalWidth;
    		max_height_size = totalHeight;
    		needMoreBitmap = false;
    		
    		try { 
    			if(bitmap != null) {		
    				bitmap.recycle();
    				bitmap = null;
    			}
    		}
    		catch (Exception e) {
    			Log.v(TAG,"GenerateResortMapImgs error-3");
    		}
    		
    	}
    	catch(Exception e) {
    		needMoreBitmap = true;
    		bitmap = null;
    	}
    	
    	
    	fileName = getResortIdNameByDrawings(mapDrawings);
    	
    	int i=0, j=0;
    	for(int sHeight = 0; sHeight < totalHeight; sHeight += max_height_size, ++i) {
    		j = 0;
    		for(int sWidth = 0; sWidth < totalWidth; sWidth += max_width_size, ++j ) {
    			
    			int eWidth = ((sWidth  + max_width_size) >= totalWidth) ? totalWidth : sWidth  + max_width_size;
    			int eHeight = ((sHeight  + max_height_size) >= totalHeight) ? totalHeight : sHeight  + max_height_size;

    			
    			rect = new Rect(sWidth, sHeight, eWidth, eHeight);
    			Log.v(TAG,"GenerateResortMapImgs 1. needMoreBitmap=" + needMoreBitmap + "swidth=" + sWidth +", eWidth=" + eWidth +", sHeight=" + sHeight +", eHeight=" + eHeight);
    			imageFileName =  fileName;
    			if (needMoreBitmap) {
    				imageFileName += "_" + i + "_" + j;	
    			}
    			imageFileName += ".png";
    			Log.v(TAG,"GenerateResortMapImgs 2. path=" + pathName +", imageFileName=" + imageFileName);

    			try {
	    			file = new File(pathName, imageFileName);
	        		if (file.exists())
	        			file.delete();
	        		file.createNewFile();
	        		fileOutputStream = new FileOutputStream(file);
	        		bitmap = MapDrawingsBO.GetMapSegmentBitmap(mapDrawings, new Rect(sWidth, sHeight, eWidth, eHeight), true);
	    	    	Log.v(TAG,",GenerateResortMapImgs 5 bitmap=" +((bitmap==null)? "null":"OK"));
	    	    	if (bitmap == null) {
	    	    		Log.v(TAG,"GenerateResortMapImgs.bitmap=null");
	    	    		byte outstr[] = "OUt of memory".getBytes();
	    	    		fileOutputStream.write(outstr);
	    	    		return;
	    	    	}
	    	    	
	    	    	Log.v(TAG,"GenerateResortMapImgs-6");
	    	    	boolean result = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
	    	    	Log.v(TAG,"GenerateResortMapImgs-7, bitmapOK=" + result);
	    	    	    		    	    	
    			} catch (Exception e) {
    	    		e.printStackTrace(); 
    	    		Log.v(TAG,"GenerateResortMapImgs error-0");
    		    	return;
    		    	
    	    	} finally {
    	    		try { if(bitmap != null) {		
    	    			bitmap.recycle();
    	    			bitmap = null;
    	    			}
    	    		}
    	    		catch (Exception e) {
    	    			Log.v(TAG,"GenerateResortMapImgs error-1");
    	    		}
    	    		
    	    		try { 
    	    			if(fileOutputStream != null) {
    	    				fileOutputStream.flush();
    	    				fileOutputStream.close();
    	    				fileOutputStream = null;
    	    			}
    	    		}
    				catch (Exception e) {
    					Log.v(TAG,"GenerateResortMapImgs error-2");
    				}
    	    		
    	    	}//eof finally
    			
    	    	
    		}//eof for
    	}//eof for
    	
    	//fileName  = pathName + "resortMapList.txt";
    	//Log.v(TAG,"--------------SMN--8, CreateResortFile");
    	//CreateResortFile(context, mapDrawings.mResortInfo.ResortID, MAP_VERSION, fileName);
    	//Log.v(TAG,"--------------SMN--9, CreateResortFile=" + fileName);
	    	
		
	} //eof GenerateResortMapImgs
	
    private static String getResortIdNameByDrawings(MapDrawings mapDrawings) {
    	
    	String result = "";
    	int resortId = 0;
		String resortName = ""; 
    	ResortInfo resortInfo = null;
    	
		if (mapDrawings != null){
			resortInfo = mapDrawings.mResortInfo;
			if (resortInfo != null)
				resortId = resortInfo.ResortID;
				resortName = resortInfo.Name;
		}
		
		
    	if (resortName == null) {
    		result = "None";
    	}
    	else if (resortName.length() > 200) {
    		result = resortName.substring(0, 200);
    	}
    	else 
    		result = resortName;
    	
       result += "_" + resortId;
       return result;
    }//eof getResortIdNameByDrawings
	
		
	public static void GenerateMapBundle(Bundle bundle, MapDrawings mapDrawings) throws Exception {
		try {
			Bundle resortInfoBundle = ResortInfoBundler.GenerateBundle(mapDrawings.mResortInfo);
			bundle.putBundle("ResortInfo", resortInfoBundle);
			AddRectF("TransformedBoundingBox", bundle, mapDrawings.mTransformedBoundingBox);
			
			// POIs
			bundle.putInt("POIs.size", mapDrawings.POIDrawings.size());
			Bundle poiBundles = new Bundle();
			for(int i = 0; i < mapDrawings.POIDrawings.size(); i++) 
			{
				poiBundles.putBundle(Integer.toString(i), DrawingBundler.GeneratePOIBundle(mapDrawings.POIDrawings.get(i)));
			}
			bundle.putBundle("POIs", poiBundles);
			
			// Trail
			bundle.putInt("Trails.size", mapDrawings.TrailDrawings.size());
			Bundle trailBundles = new Bundle();
			for(int i = 0; i < mapDrawings.TrailDrawings.size(); i++) 
			{
				trailBundles.putBundle(Integer.toString(i), DrawingBundler.GenerateTrailBundle(mapDrawings.TrailDrawings.get(i)));
			}
			bundle.putBundle("Trails", trailBundles);
			
			// Areas
			bundle.putInt("Areas.size", mapDrawings.AreaDrawings.size());
			Bundle areaBundles = new Bundle();
			for(int i = 0; i < mapDrawings.AreaDrawings.size(); i++) 
			{
				areaBundles.putBundle(Integer.toString(i), DrawingBundler.GenerateAreaBundle(mapDrawings.AreaDrawings.get(i)));
			}
			bundle.putBundle("Areas", areaBundles);
		}
		catch (Exception e){
			throw new Exception("MapBundler Failed");
		}
		Log.v(TAG, "Done GenerateMapBundle");
	}
}
