/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.menuHandler;

import android.content.Context;

import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfo;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;

/** 
 * define the base class for handling menu item
 */
public class ResortMenuHandler extends MenuHandler
{
	private MapManager mMapManager = null;
	public ResortMenuHandler( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler defaultEventHandler, MapManager mapManager )
	{
		super( context, overlayManager, defaultEventHandler );
		mMapManager = mapManager;
	}
	
	/*
	 * Override by the derived class for handling an menuItem object is selected
	 */
	public void OnMenuItemSelected( Object item )
	{
		//clear out the overlay first
		mOverlayManager.setOverlayView(null);
		mMapManager.loadResort((ResortInfo)item);
	}
}
