/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.menuHandler;

import android.content.Context;

import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.datamanagement.RegionInfo;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;
import com.reconinstruments.navigation.navigation.views.MenuView;

/**
 * 
 * define the base class for handling menu item
 *
 */
public class RegionMenuHandler extends MenuHandler
{
	private MapManager mMapManager = null;
	private ResortMenuHandler mResortMenuHandler = null;
	
	public RegionMenuHandler( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler defaultEventHandler, MapManager mapManager )
	{
		super( context, overlayManager, defaultEventHandler );
		mMapManager = mapManager;
	}
	
	/*
	 * Override by the derived class for handling an menuItem object is selected
	 */
	public void OnMenuItemSelected( Object item )
	{
		RegionInfo RegionInfo = (RegionInfo)item;	
		
		if( mResortMenuHandler == null )
		{
			mResortMenuHandler = new ResortMenuHandler( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager );
		}
		MenuView view = new MenuView( mContext, mOverlayManager, mDefaultRemoteEventHandler, "SELECT RESORT", mResortMenuHandler );
					
		view.setMenuItems(  RegionInfo.getAvailableResorts() );					
		mOverlayManager.addOverlayView(view);

	}

}
