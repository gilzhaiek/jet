/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.menuHandler;

import android.content.Context;

import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.datamanagement.CountryInfo;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;
import com.reconinstruments.navigation.navigation.views.MenuView;

/**
 * 
 * define the base class for handling menu item
 *
 */
public class CountryMenuHandler extends MenuHandler
{
	private RegionMenuHandler mRegionMenuHandler = null;
	private ResortMenuHandler mResortMenuHandler = null;
	private MapManager mMapManager = null;
	public CountryMenuHandler( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler defaultEventHandler, MapManager mapManager )
	{
		super( context, overlayManager, defaultEventHandler );
		mMapManager = mapManager;
	}
	
	/*
	 * Override by the derived class for handling an menuItem object is selected
	 */
	public void OnMenuItemSelected( Object item )
	{
		CountryInfo countryInfo = (CountryInfo)item;	

		
		
		MenuView view;
					
		if( countryInfo.getAvailableRegions() == null )
		{
			if( mResortMenuHandler == null )
			{
				mResortMenuHandler = new ResortMenuHandler( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager );
			}

			view = new MenuView( mContext, mOverlayManager, mDefaultRemoteEventHandler, "SELECT RESORT", mResortMenuHandler );
			view.setMenuItems(  countryInfo.getAvailableResorts() );		
		}
		else
		{		
			if( mRegionMenuHandler == null )
			{
				mRegionMenuHandler = new RegionMenuHandler( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager );
			}
			

			view = new MenuView( mContext, mOverlayManager, mDefaultRemoteEventHandler, "SELECT RESORT", mRegionMenuHandler );
			view.setMenuItems(  countryInfo.getAvailableRegions() );			
		}
		
					
		mOverlayManager.addOverlayView(view);

	}

}
