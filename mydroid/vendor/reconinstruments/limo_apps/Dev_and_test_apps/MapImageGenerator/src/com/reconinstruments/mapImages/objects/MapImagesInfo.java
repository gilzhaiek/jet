package com.reconinstruments.mapImages.objects;

import java.util.ArrayList;

import android.graphics.Rect;
import android.util.Log;

public class MapImagesInfo {
	protected static final String TAG = "MapImagesInfo";
	
	public ArrayList<String> mMapImageFileNameWithTrails = null; 
	public ArrayList<String> mMapImageFileNameNoTrails = null;
	public ArrayList<Rect> mMapImageFileBoundingBox = null;
	
	public MapImagesInfo(int segmentBundleSize){
		mMapImageFileNameWithTrails = new ArrayList<String>(segmentBundleSize); 
		mMapImageFileNameNoTrails = new ArrayList<String>(segmentBundleSize);
		mMapImageFileBoundingBox = new ArrayList<Rect>(segmentBundleSize);
	}
	
	public void AddImageInfo(String imageFileNameWTrails, String imageFileNameNTrails, Rect boundingBox) {
		mMapImageFileNameWithTrails.add(imageFileNameWTrails);
		mMapImageFileNameNoTrails.add(imageFileNameNTrails);
		mMapImageFileBoundingBox.add(boundingBox);
	}
}
