package com.reconinstruments.dashlauncher.radar.maps.bundlers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.reconinstruments.dashlauncher.radar.maps.bo.MapDrawingsBO;
import com.reconinstruments.dashlauncher.radar.maps.drawings.MapDrawings;
import com.reconinstruments.dashlauncher.radar.maps.objects.MapImagesInfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

public class MapBundler extends BundlerBase {
	protected static final String TAG = "MapBundler";
	
	protected static final double MAP_VERSION = .64; 
	public static final int MAX_BITMAP_SEGMENT_SIZE 	= 1024; // pixels
		
	public static MapImagesInfo GetMapImagesInfo(Bundle bundle) {
		Bundle segmentsBundle = bundle.getBundle("SegmentsBundle");
		
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
		}
		catch (Exception e){
			throw new Exception("MapBundler Failed");
		}
		Log.v(TAG, "Done GenerateMapBundle");
	}
}
