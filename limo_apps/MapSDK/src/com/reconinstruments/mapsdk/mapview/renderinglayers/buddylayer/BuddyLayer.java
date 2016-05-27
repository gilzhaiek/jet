package com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.RemoteException;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition.DynamicCameraPositionInterface.IDynamicCameraPosition;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.focusableitems.DynamicFocusableItemsInterface.IDynamicFocusableItems;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.geodataservice.DynamicGeoDataInterface.IDynamicGeoData;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.reticleitems.DynamicReticleItemsInterface.IDynamicReticleItems;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer.BackgroundSize;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

public class BuddyLayer extends RenderingLayer implements IDynamicCameraPosition, IDynamicReticleItems, IDynamicFocusableItems, IDynamicGeoData {
// constants
	private final static String TAG = "BuddyLayer";
	private static final int MAX_NUM_OFFSCREEN_BUDDIES = 10;
	private static int mImageScaleMultiplier;
	private final Semaphore mAccessToDrawingSetLoadHandling = new Semaphore(1, true);

// members
	BuddyDrawingSet					mBuddyDrawingSet = null;
	private Context 				mContext;
	

// methods
	public BuddyLayer(Activity parentActivity, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer, BackgroundSize bkgdSize) throws Exception {
		super(parentActivity, "Buddy",rsm);
		mBuddyDrawingSet = new BuddyDrawingSet(this, rsm, world2DrawingTransformer);
		
		switch(bkgdSize) {
			case NORMAL: {
				mImageScaleMultiplier = 2;
				break;
			}
			case MINIMAL: {
				mImageScaleMultiplier = 1;
				break;
			}
		}
		
		mBuddyDrawingSet.setMapImageSize(512 * mImageScaleMultiplier);
	}

	@Override
	public boolean CheckForUpdates() {
		if(mEnabled) {
			boolean newData = mBuddyDrawingSet.SwitchIfUpdateReady();
			return newData;
		}
		return false;
	}

	@Override
	public boolean IsReady() {
		return true;
	}
	
	public void HandleNewBuddies() {
		mBuddyDrawingSet.UpdateBuddies(mContext);
	}
	
	@Override
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		// @Overide in subclass
		if(mEnabled) {
			mBuddyDrawingSet.Draw(canvas, camera, focusedObjectID, res);
		}
	}

// dynamic reticle support
	@Override
	public ArrayList<ReticleItem> GetReticleItems(CameraViewport camera, float withinDistInM) {  /// assumed to be called after Draw()
		if(mEnabled) {
			return mBuddyDrawingSet.GetReticleItems(camera, withinDistInM);
		}
		return null;
	}
	@Override
	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText) {		// for OpenGL based rendering
		// @Overide in subclass
		if(mEnabled){
			mBuddyDrawingSet.Draw(rsm, camera, focusedObjectID, res, loadNewTexture, glText);
		}
	}
		
	
// dynamic geo data interface routines

	@Override
	public void SetGeodataServiceInterface(IGeodataService iGeodataService) throws RemoteException {
		mBuddyDrawingSet.mGeodataServiceInterface = iGeodataService;
		
		HandleNewBuddies();  // fake if for testing

	}

	@Override
	public boolean SetGeodataServiceState(GeoDataServiceState geoDataServiceState) {
		mBuddyDrawingSet.mGeodataServiceState = geoDataServiceState;
		
		return true;  
	}

	@Override
	public void SetCameraPosition(CameraViewport camera) {
		// TODO Auto-generated method stub
		mBuddyDrawingSet.mCameraViewport = camera;
		
	}

	@Override
	public void SetCameraHeading(float heading) {
		// TODO Auto-generated method stub
	}

	@Override
	public void SetCameraPitch(float pitch){
		
	}
	
	@Override
	public ArrayList<POIDrawing> GetFocusableItems() {
		// TODO Auto-generated method stub
		return mBuddyDrawingSet.GetFocusableItems();
	}
	
	public ArrayList<BuddyItem> GetSortedBuddyList() {
		return mBuddyDrawingSet.GetSortedBuddyList();
	}

	@Override
	public void SetCameraPosition(CameraViewport camera, boolean forceUpdate) {
		// TODO Auto-generated method stub
		
	}

    public int getNumBuddies(){
        return mBuddyDrawingSet.GetNextBuddyList().size();
    }
	
}
