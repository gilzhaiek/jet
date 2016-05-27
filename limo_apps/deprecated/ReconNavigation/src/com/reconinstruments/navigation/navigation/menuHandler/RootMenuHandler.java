/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.menuHandler;

import java.util.ArrayList;

import android.content.Context;
import android.widget.ImageView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.PoInterest;
import com.reconinstruments.navigation.navigation.PoiInfoProvider;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfoProvider;
import com.reconinstruments.navigation.navigation.views.DropPinView;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;
import com.reconinstruments.navigation.navigation.views.MenuView;
import com.reconinstruments.navigation.navigation.views.MenuViewItem;
import com.reconinstruments.navigation.navigation.views.PoiItemView;

/**
 * 
 * define the base class for handling menu item
 *
 */
public  class RootMenuHandler extends MenuHandler
{
	public static final int ROOT_MENU_ITEM_DROPPIN = 1;
	public static final int ROOT_MENU_ITEM_SELECTRESORT = 2;
	public static final int ROOT_MENU_ITEM_MYPIN = 3;
	public static final int ROOT_MENU_ITEM_POI = 3;
	public static final int ROOT_MENU_ITEM_MYPINS = 4;
	public static final int ROOT_MENU_ITEM_MYBUDDIES = 5;
	public static final int ROOT_MENU_ITEM_MY_LOCATION = 6;	
	public static final int ROOT_MENU_ITEM_LAST_RESORT = 7;
	
	protected MapManager mMapManager = null;
	public DropPinView mDropPinView = null;	
	protected LocationsMenuHandler mLocationsMenuHandler = null;
	protected CountryMenuHandler mCountryMenuHandler = null;
	protected PoiCategoryMenuHandler mPoiCategoryMenuHandler = null;
	protected ArrayList<PoiInfoProvider.PoiCategory> mPoiCategories = null;
	
	public RootMenuHandler( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler defaultEventHandler, MapManager mapManager )
	{
		super( context, overlayManager, defaultEventHandler );
		mMapManager = mapManager;
	}
	
	/*
	 * Override by the derived class for handling an menuItem object is selected
	 */
	public void OnMenuItemSelected( Object item )
	{
		MenuViewItem menuItem = (MenuViewItem)item;
		
		switch( menuItem.getID() )
		{
			case ROOT_MENU_ITEM_SELECTRESORT:
			{
				//country list
				if( mCountryMenuHandler == null )
				{
					mCountryMenuHandler = new CountryMenuHandler( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager);
				}
				MenuView view = new MenuView( mContext, mOverlayManager, mDefaultRemoteEventHandler, null, mCountryMenuHandler );				
				view.setMenuItems( ResortInfoProvider.getAvailableCountries());				
				mOverlayManager.addOverlayView(view);
			}	
			break;
			
			
			case ROOT_MENU_ITEM_DROPPIN:
			{				
				//the drop in view
				if( mDropPinView == null )
				{
					mDropPinView = new DropPinView( mContext, mMapManager.mMapView, mOverlayManager, mDefaultRemoteEventHandler, mMapManager);
				}
				
				mOverlayManager.addOverlayView(mDropPinView);
			}
			break;
			
			case ROOT_MENU_ITEM_MY_LOCATION:
			{
			}
			break;
			
			case ROOT_MENU_ITEM_POI:				
			{
				if( mPoiCategoryMenuHandler  == null )
				{
					mPoiCategoryMenuHandler = new PoiCategoryMenuHandler( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager );
				}
				MenuView view = new MenuView( mContext, mOverlayManager, mDefaultRemoteEventHandler, "POINT OF INTERESTS", mPoiCategoryMenuHandler );
				
				PoiInfoProvider.fill(mMapManager.mMap);
				
				if( mPoiCategories == null )
				{
					mPoiCategories = new ArrayList<PoiInfoProvider.PoiCategory>( PoInterest.NUM_POI_TYPE );
				}
				else
				{
					mPoiCategories.clear();
				}
				for( PoiInfoProvider.PoiCategory category : PoiInfoProvider.sPoiCategories )
				{
					if( category.mPoiType != PoInterest.POI_TYPE_CDP && category.mPoiType != PoInterest.POI_TYPE_BUDDY )
					{
						mPoiCategories.add(category);
					}
				}
				view.setMenuItems( mPoiCategories );	
				mOverlayManager.addOverlayView(view);

			}
			break;
			
			case ROOT_MENU_ITEM_MYPINS:
			{
				PoiInfoProvider.fill(mMapManager.mMap);
				
				PoiItemView view = new PoiItemView( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager, PoInterest.POI_TYPE_CDP );
				mOverlayManager.addOverlayView(view);

			}
			break;
			
			case ROOT_MENU_ITEM_MYBUDDIES:
			{
				PoiInfoProvider.fill(mMapManager.mMap);
				
				PoiItemView view = new PoiItemView( mContext, mOverlayManager, mDefaultRemoteEventHandler, mMapManager, PoInterest.POI_TYPE_BUDDY );
				mOverlayManager.addOverlayView(view);
				
			}
			break;
		}
	}

}
