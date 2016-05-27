package com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer;

import android.graphics.Bitmap;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;

public class ReticleItem {
	public Bitmap mImage = null;
	public PointXY mDrawingLocation = null;
	public MeshGL mMesh = null;
	private int mPOIIndex = -1;
	private boolean mIsUserIcon = false;
	
	/**
	 * Defines a reticle item that is a Point of Interest 
	 * @param poiIndex POI index as defined in rendering_schemes.xml
	 * @param location Location in drawing coordinates
	 */
	public ReticleItem(int poiIndex, PointXY location) {
		mPOIIndex = poiIndex;
		mDrawingLocation = location;
	}
	
	/**
	 * Defines a reticle item that may be anything (i.e. user icon arrow, custom image).
	 * If mPOIIndex is still -1 after this constructor, ReticuleLayer will load the image
	 * defined here
	 * @param image Bitmap to use as texture
	 * @param location Location in drawing coordinates
	 */
	public ReticleItem(Bitmap image, PointXY location, boolean isUserIcon){
		mImage = image;
		mDrawingLocation = location;
		this.mIsUserIcon = isUserIcon;
	}
	
	/**
	 * 
	 * @return true if this ReticleItem is a POI
	 */
	public boolean isPOIItem(){
		return (mPOIIndex != -1);
	}
	
	/**
	 * 
	 * @return true if this is the user icon blip
	 */
	public boolean isUserIcon(){
		return mIsUserIcon;
	}
	
	public int getPOIID(){
		return mPOIIndex;
	}
}
