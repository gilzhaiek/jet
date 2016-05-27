/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.menuHandler;

import android.content.Context;

import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.PoiInfoProvider;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;
import com.reconinstruments.navigation.navigation.views.PoiItemView;

/** 
 * define the base class for handling menu item
 */
public class PoiCategoryMenuHandler extends MenuHandler
{
	private MapManager mMapManager = null;
	public PoiCategoryMenuHandler( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler defaultEventHandler, MapManager mapManager )
	{
		super( context, overlayManager, defaultEventHandler );
		mMapManager = mapManager;
	}
	
	/*
	 * Override by the derived class for handling an menuItem object is selected
	 */
	public void OnMenuItemSelected( Object item )
	{
		PoiInfoProvider.PoiCategory category = (PoiInfoProvider.PoiCategory)item;
		
		PoiItemView view = new PoiItemView( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager, category.mPoiType );
		mOverlayManager.addOverlayView(view);
		
	}

}
