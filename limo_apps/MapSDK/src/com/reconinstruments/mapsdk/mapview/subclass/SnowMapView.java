package com.reconinstruments.mapsdk.mapview.subclass;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer.BuddyLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.gridlayer.GridLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer.BackgroundSize;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleConfig;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.subclass.SnowReticleLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.usericonlayer.UserIconLayer;

public class SnowMapView extends MapView {
// Constants
	private static final String TAG = "SnowMapView";
	private static final boolean FORCE_RESOURCE_MISSING_ERROR = false;

// members
	boolean mNameResortByPointingAtIt = false;
	
	public SnowMapView(Activity parentActivity, String identifier) throws Exception {
		this(parentActivity, identifier, parentActivity.getResources().getString(R.string.rendering_schemes));
	}
		
	public SnowMapView(Activity parentActivity, String identifier, String rsmXmlFile) throws Exception {
		super(parentActivity, identifier, rsmXmlFile);
	}
	
	@Override
	protected boolean haveAllRequiredRuntimeResources() {
		boolean result = true;
		String rootpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ReconApps/MapData/";

		if(!(new File(rootpath + mResources.getString(R.string.rendering_schemes))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.reticle_configuration))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.scale_pitch_curves))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.velocity_curves))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.square_tex))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.md_snow_map))).exists()) result = false;

		if(FORCE_RESOURCE_MISSING_ERROR) {
			return false;
		}
		return result;
	}

	// Override this method when defining a subclass
	@Override
	public void DefineMapLayers(ArrayList<RenderingLayer> layers) throws Exception {
		BackgroundSize bkgdSize = BackgroundSize.NORMAL;
		mGridLayer = new GridLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer);
		mCustomLayer = new CustomLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer);
		mMapLayer = new Map2DLayer(mResources.getString(R.string.md_snow_map), mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, mIdentifier, mMapPreloadMultiplier, bkgdSize, mCustomLayer);
		Log.e(TAG, "setting preload multiplier to " + mMapPreloadMultiplier);
		mBuddyLayer  = new BuddyLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, bkgdSize);
		mUserIconLayer  = new UserIconLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, bkgdSize);
		mReticleConfig = new ReticleConfig(getResources());
		if(mReticleConfig != null && mReticleConfig.mEnabled) {
			Log.d(TAG, "snow reticle layer being created!");
			mReticleLayer  = new SnowReticleLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, true, bkgdSize);
			((SnowReticleLayer)mReticleLayer).setMapDrawingSetCallback(mMapLayer.getMapDrawingSet());
			((SnowReticleLayer)mReticleLayer).setUserIconLayerCallback(mUserIconLayer);
		}
		layers.add(mGridLayer);
		layers.add(mMapLayer);
		layers.add(mCustomLayer);
		layers.add(mBuddyLayer);
		if(mReticleConfig != null && mReticleConfig.mEnabled) {
			layers.add(mReticleLayer);
			((SnowReticleLayer)mReticleLayer).SetResortNamingByPoint(mNameResortByPointingAtIt);
		}
		layers.add(mUserIconLayer);
	}
	
	@Override
	public void SetCameraToRotateWithUser(boolean cameraRotatesWithUser) {
		super.SetCameraToRotateWithUser(cameraRotatesWithUser);
		if(cameraRotatesWithUser) {
			mNameResortByPointingAtIt = true;
		}
		else {
			mNameResortByPointingAtIt = false;
		}
		if(mReticleLayer != null) {
			((SnowReticleLayer)mReticleLayer).SetResortNamingByPoint(mNameResortByPointingAtIt);
		}
	}
	
	@Override
	public void SetCameraToPitchWithUser(boolean cameraPitchesWithUser) {
		mCameraPitchesWithUser = false;				// snow mapview doesn't pitch with user - ever
	}
	

}
