
package com.reconinstruments.navigation.navigation.views;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.ImageHelper;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.PoInterest;
import com.reconinstruments.navigation.navigation.PoiInfoProvider;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfoProvider;
import com.reconinstruments.navigation.navigation.menuHandler.LocationsMenuHandler;
import com.reconinstruments.navigation.navigation.menuHandler.CountryMenuHandler;
import com.reconinstruments.navigation.navigation.menuHandler.PoiCategoryMenuHandler;
import com.reconinstruments.navigation.navigation.menuHandler.RootMenuHandler;

public class RootTabView extends TabView
{
	protected MapManager mMapManager = null;
	//menu handler for country list
	protected LocationsMenuHandler mLocationsMenuHandler = null;
	protected CountryMenuHandler mCountryMenuHandler = null;
	protected PoiCategoryMenuHandler mPoiCategoryMenuHandler = null;
	protected RootMenuHandler mRootMenuHandler = null;
	protected ArrayList<PoiInfoProvider.PoiCategory> mPoiCategories = null;

	public RootTabView( Context context, IOverlayManager overlayManager, MapManager mapManager )
	{
		super( context, overlayManager );
		mMapManager = mapManager;
		createTabPages();
		this.focusTabBar();
	}
	
	private void createTabPages( )
	{
		MenuViewItem item = null;
		ArrayList<MenuViewItem> items = null;
		ArrayList<TabPage> tabPages = new ArrayList<TabPage>( 6 );
		
		Context context = this.getContext();
		
		if( mLocationsMenuHandler == null )
		{
			mLocationsMenuHandler = new LocationsMenuHandler( context, null, null, mMapManager);
		}
		Drawable locationIcon1 = context.getResources().getDrawable(R.drawable.tab_locations_grey);
		Drawable locationIcon2 = context.getResources().getDrawable(R.drawable.tab_locations_color);
		Drawable locationIcon3 = context.getResources().getDrawable(R.drawable.tab_locations_white);
		MenuTabPage locationsTabPage = new MenuTabPage( context, locationIcon1, locationIcon2, locationIcon3,  this, mLocationsMenuHandler.GetMenuView() );
		mLocationsMenuHandler.setOverlayManager( locationsTabPage.mOverlayManager );
		tabPages.add(locationsTabPage);
		
		//create a select resort tab page
		if( mCountryMenuHandler == null )
		{
			mCountryMenuHandler = new CountryMenuHandler( context, null, null, mMapManager);
		}
		Drawable resortIcon1 = context.getResources().getDrawable(R.drawable.tab_resort_grey);
		Drawable resortIcon2 = context.getResources().getDrawable(R.drawable.tab_resort_color);
		Drawable resortIcon3 = context.getResources().getDrawable(R.drawable.tab_resort_white);
		MenuView countryMenuView = new MenuView( context, null, null, context.getResources().getString(R.string.tab_title_resort), mCountryMenuHandler );				
		countryMenuView.setMenuItems( ResortInfoProvider.getAvailableCountries());			
		MenuTabPage resortTabPage = new MenuTabPage( context, resortIcon1, resortIcon2, resortIcon3, this, countryMenuView );
		mCountryMenuHandler.setOverlayManager( resortTabPage.mOverlayManager );
		tabPages.add(resortTabPage);
		
		//create the rest of tabs if a resort is active now
		if( mMapManager.getActiveResort() != null )
		{
			//create a select point-of-interest tab page
			if( mPoiCategoryMenuHandler  == null )
			{
				mPoiCategoryMenuHandler = new PoiCategoryMenuHandler( context, null, null, mMapManager );
			}
			MenuView poiMenuView = new MenuView( context, null, null, context.getResources().getString(R.string.tab_title_poi), mPoiCategoryMenuHandler );
			
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
			poiMenuView.setMenuItems( mPoiCategories );	
			poiMenuView.setIconProvider(mPoiCategoryIconProvider);
			Drawable poiIcon1 = context.getResources().getDrawable(R.drawable.tab_poi_grey);
			Drawable poiIcon2 = context.getResources().getDrawable(R.drawable.tab_poi_color);
			Drawable poiIcon3 = context.getResources().getDrawable(R.drawable.tab_poi_white);
			MenuTabPage poiTabPage = new MenuTabPage( context, poiIcon1, poiIcon2, poiIcon3, this, poiMenuView );
			//set the poi category menu handler's overlay manager to be the TabView's overlay manager
			mPoiCategoryMenuHandler.setOverlayManager(this.mOverlayManager);
			tabPages.add(poiTabPage);
			
			if( mRootMenuHandler == null )
			{
				mRootMenuHandler = new RootMenuHandler( context, null, null, mMapManager );
			}

			//create a pin related tab page
			MenuView pinMenuView = new MenuView( context, null, null, context.getResources().getString(R.string.tab_title_pin), mRootMenuHandler );
			items = new ArrayList<MenuViewItem>( );
			item = new MenuViewItem( getResources().getString(R.string.menu_drop_pin), RootMenuHandler.ROOT_MENU_ITEM_DROPPIN, pinMenuView );
			items.add(item);
			
			//check if there is any pin previously created on this site
			ArrayList<PoInterest> onSitePins = mMapManager.mMap.mPoInterests.get( PoInterest.POI_TYPE_CDP );
			if( onSitePins.size() > 0 )
			{				
				item = new MenuViewItem( getResources().getString(R.string.menu_show_my_pins), RootMenuHandler.ROOT_MENU_ITEM_MYPINS, pinMenuView );
				items.add(item);					
			}
			pinMenuView.setMenuItems( items );
			Drawable pinIcon1 = context.getResources().getDrawable(R.drawable.tab_pin_grey);
			Drawable pinIcon2 = context.getResources().getDrawable(R.drawable.tab_pin_color);
			Drawable pinIcon3 = context.getResources().getDrawable(R.drawable.tab_pin_white);
			MenuTabPage pinTabPage = new MenuTabPage( context, pinIcon1, pinIcon2, pinIcon3,  this, pinMenuView );
			mRootMenuHandler.setOverlayManager(this.mOverlayManager);
			tabPages.add(pinTabPage);			
								
			//check if there is any buddies previously created on this site
			ArrayList<PoInterest> onSiteBuddies = mMapManager.mMap.mPoInterests.get( PoInterest.POI_TYPE_BUDDY );
			if( onSiteBuddies.size() > 0 )
			{				
				MenuView buddyMenuView = new MenuView( context, null, null, context.getResources().getString(R.string.tab_title_buddy), mRootMenuHandler );
				ArrayList<MenuViewItem> buddyItems = new ArrayList<MenuViewItem>( );
				item = new MenuViewItem( getResources().getString(R.string.menu_show_my_buddies), RootMenuHandler.ROOT_MENU_ITEM_MYBUDDIES, buddyMenuView );
				buddyItems.add(item);
				buddyMenuView.setMenuItems( buddyItems );
				Drawable buddyIcon1 = context.getResources().getDrawable(R.drawable.tab_buddy_grey);
				Drawable buddyIcon2 = context.getResources().getDrawable(R.drawable.tab_buddy_color);
				Drawable buddyIcon3 = context.getResources().getDrawable(R.drawable.tab_buddy_white);
				MenuTabPage buddyTabPage = new MenuTabPage( context, buddyIcon1, buddyIcon2, buddyIcon3, this, buddyMenuView );
				mRootMenuHandler.setOverlayManager(this.mOverlayManager);
				tabPages.add(buddyTabPage);			

			}
			
		}
		
		this.setTabPages(tabPages);

	}
	
	private MenuView.IIconProvider mPoiCategoryIconProvider = new MenuView.IIconProvider() {
		
		@Override
		public void setIcon(ImageView imageView, int position)
		{			
			PoiInfoProvider.PoiCategory category = mPoiCategories.get(position);
			
			
			if( category != null )
			{			
				imageView.setImageBitmap(ImageHelper.getRoundedCornerBitmap(PoInterest.getIcon(category.mPoiType), 4));
			}			
		}
	};

}