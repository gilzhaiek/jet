/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.MapView;

/**
 *The view for move a map
 */

public class MapZoomView extends OverlayView
{
	MapView mMapView = null;
	ZoomView mZoomView = null;
	MapManager mMapManager = null;
	
	public MapZoomView( Context context, MapView mapView, IOverlayManager overlayManager, IRemoteControlEventHandler throwBack, MapManager mapManager )
	{
		super( context, overlayManager, throwBack );
		mThrowBack = throwBack;
		mMapView = mapView;
		mMapManager = mapManager;

		//inflate a view from predefined xml file
        LayoutInflater factory = LayoutInflater.from(context);
        View navView = factory.inflate(R.layout.map_zoom_view, null);
        this.addView(navView);
        
        mZoomView = (ZoomView)navView.findViewById(R.id.zoomView);

        mZoomView.setZoomRange(mMapView.getMinZoomLevel(), mMapView.getMaxZoomLevel(), mMapView.getZoomLevel());
	}
	
	@Override
	public boolean onDownArrowDown( View srcView )
	{
		mMapView.zoomOut();	
		mZoomView.setZoomLevel(mMapView.getZoomLevel());
		return true;
	}
	
	@Override
	public boolean onUpArrowDown( View srcView )
	{		
		mMapView.zoomIn();
		mZoomView.setZoomLevel(mMapView.getZoomLevel());
		return true;
	}
	
	@Override
	public boolean onLeftArrowDown( View srcView )
	{
		mMapView.rotate(-mMapView.ROTATION_DELTA);
		return true;
	}

	@Override
	public boolean onRightArrowDown( View srcView )
	{
		mMapView.rotate(mMapView.ROTATION_DELTA);
		return true;
	}
			
	
	@Override
	public boolean onSelectDown( View srcView )
	{
		//let the throwBack handle the select button event
		this.mThrowBack.onSelectDown(srcView);
		return true;
	}
	
	@Override
	public void onPreShow( )
	{		
		mMapManager.recordMapFeature();
		mMapView.clearFeatures();
		mMapView.setFeature(MapView.MAP_FEATURE_SCALEMETRIC, true);
		mMapView.setFeature(MapView.MAP_FEATURE_COMPASS, true);
		mMapView.setFeature(MapView.MAP_FEATURE_ROTATE, true);
	}
	
	@Override
	public void onPreHide()
	{
		mMapManager.restoreMapFeature(false);		
	}
	
	
}