/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.menuHandler;

import java.util.ArrayList;

import android.content.Context;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.datamanagement.CountryInfo;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfo;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfoProvider;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;
import com.reconinstruments.navigation.navigation.views.MenuView;
import com.reconinstruments.navigation.navigation.views.MenuViewItem;

/**
 * 
 * define the base class for handling menu item
 *
 */
public class LocationsMenuHandler extends MenuHandler
{
	protected class ResortIDs{
		public int mCountryID = -1;
		public int mResortID = -1;
		
		public ResortIDs(int countryID, int resortID)
		{
			mCountryID = countryID;
			mResortID = resortID;
		}
	}
	
	private MenuViewItem mMyLocationItem = null;
	private MenuViewItem mLastResortItem = null;
	private MenuView mLocationsMenuView = null; 
	
	private boolean visited = false;
	private MapManager mMapManager = null;
	public LocationsMenuHandler( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler defaultEventHandler, MapManager mapManager )
	{
		super( context, overlayManager, defaultEventHandler );
		mMapManager = mapManager;
		ArrayList<MenuViewItem> items = new ArrayList<MenuViewItem>( );
		
		mLocationsMenuView = new MenuView( context, null, null, context.getResources().getString(R.string.tab_title_locations), this );
		
		if(mMapManager.mOwnerLocation != null)
		{
			mMyLocationItem = new MenuViewItem( mContext.getResources().getString(R.string.menu_my_location), RootMenuHandler.ROOT_MENU_ITEM_MY_LOCATION, mLocationsMenuView );
			items.add(mMyLocationItem);
		}
		
		if(mMapManager.getLastResortID() >= 0)
		{
			ResortIDs resortIDs = new ResortIDs(mMapManager.getLastResortCountryID(), mMapManager.getLastResortID()); 
			mLastResortItem = new MenuViewItem( mMapManager.getLastResortName(), RootMenuHandler.ROOT_MENU_ITEM_LAST_RESORT, mLocationsMenuView , (Object)resortIDs);
			items.add(mLastResortItem);
		}
		
		mLocationsMenuView.setMenuItems( items );		
	}
	
	public MenuView GetMenuView()
	{
		return mLocationsMenuView;
	}
	
	/*
	 * Override by the derived class for handling an menuItem object is selected
	 */
	public void OnMenuItemSelected( Object item )
	{
		MenuViewItem menuViewitem = (MenuViewItem)item;
		
		if(menuViewitem.equals(mMyLocationItem))
		{
			mMapManager.loadOwnerClosestLocation();
			mMapManager.FocusOnMe();
			if( mOverlayManager != null )
			{
				mOverlayManager.rollBack();
			}
		}
		else if(menuViewitem.equals(mLastResortItem))
		{
			try
			{
				ResortIDs resortIDs = (ResortIDs)menuViewitem.getDataObject();
				ResortInfo resortInfo = ResortInfoProvider.getResort(resortIDs.mCountryID, resortIDs.mResortID);
						
				if(resortInfo != null)
				{
					mMapManager.loadResort(resortInfo);
					if( mOverlayManager != null )
					{
						mOverlayManager.rollBack();
					}					
				}
			}
			catch (Exception e) {
			}
		}
	}

}
