package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

public class PoiInfoProvider
{	
	
	/**
	 * 
	 * A utility class for keeping the POI  category description(Category name+number of POI-item in this category)
	 * and the poi-type
	 */
	static public class PoiCategory
	{
		public PoiCategory( String desc, int poiType )
		{
			mDesc = desc;
			mPoiType = poiType; 
		}
		
		/*
		 * Override the toString( ) to provide the string 
		 * Requested by ArrayAdapter to feed the correct content for rendering
		 * in a listView 
		 */
		@Override
		public String toString()
		{
			return mDesc;
		}
		
		public String mDesc;					//the description of the poi-category
		public int   mPoiType;					//the type of point-of-interest
	}

	/**
	 * 
	 * A utility class for keeping the POI item description:
	 * the item name and the item index in the ShpMap mPoInterests array
	 */
	static public class PoiItem
	{
		public PoiItem( String name, int index )
		{
			mName = name;
			mIndex = index;
		}

		/*
		 * Override the toString( ) to provide the string 
		 * Requested by ArrayAdapter to feed the correct content for rendering
		 * in a listView 
		 */
		@Override
		public String toString()
		{
			return mName;
		}
		public String mName;
		public int mIndex;
	}
	
	static public  ArrayList<PoiCategory> sPoiCategories = new ArrayList<PoiCategory>( PoInterest.NUM_POI_TYPE );
	static public  ArrayList<ArrayList<PoiItem>> sPoiItemLists = null;
	static final boolean SORT_ITEM_LIST = true;
	static final boolean SORT_CATEGORY_LIST = true;

	
	static public void reset( )
	{
		sPoiCategories.clear();

		if( sPoiItemLists != null )
		{
			for( ArrayList<PoiItem> names : sPoiItemLists )
			{
				names.clear();
			}
		}
	}
	
	static private void addCategory( String desc,  int poiType )
	{
		PoiCategory category = new PoiCategory( desc, poiType );			
		if( PoiInfoProvider.SORT_CATEGORY_LIST )
		{
			//sorted by the POI name
			int j;
			for( j = 0; j < sPoiCategories.size(); ++j )
			{
				if( category.mDesc.compareTo( sPoiCategories.get(j).mDesc ) <= 0 )
					break;
			}
			sPoiCategories.add( j, category );
		}
		else
		{
			//un-sorted list
			sPoiCategories.add( category );
		}

	}
	
	static public void fill( ShpMap map )
	{
		if( sPoiItemLists == null )
		{
			sPoiItemLists = new ArrayList<ArrayList<PoiItem>>( PoInterest.NUM_POI_TYPE );	
			for( int i = 0; i < PoInterest.NUM_POI_TYPE; ++i )
			{
				sPoiItemLists.add( new ArrayList<PoiItem>( 32 ) );
			}
		}
		else
		{
			reset( );
		}
		
		for( int i = 0; i < PoInterest.NUM_POI_TYPE; ++i )
		{
			//skip all the owner
			if( i == PoInterest.POI_TYPE_OWNER )
			{
				continue;
			}
			
			String name = null;
			if( map.mPoInterests.get(i).size() > 0 )
			{
				switch( i )
				{
					case PoInterest.POI_TYPE_BANK:
						name = "Banks";
					break;
					
					case PoInterest.POI_TYPE_BAR:
						name = "Bars";
					break;
					
					case PoInterest.POI_TYPE_CARPARKING:
						name = "Parking";
					break;
					
					case PoInterest.POI_TYPE_CHAIRLIFTING:
						name = "Chair Lifts";
					break;

					case PoInterest.POI_TYPE_HOTEL:
						name = "Hotels";
					break;

					case PoInterest.POI_TYPE_INFORMATION:
						name = "Information Centers";
					break;

					case PoInterest.POI_TYPE_PARK:
						name = "Parks";
					break;

					case PoInterest.POI_TYPE_RESTAURANT:
						name = "Restaurants";
					break;

					case PoInterest.POI_TYPE_RESTROOM:
						name = "Restrooms";
					break;

					case PoInterest.POI_TYPE_SKICENTER:
						name = "Ticket Centres";
					break;

					case PoInterest.POI_TYPE_SKISCHOOL:
						name = "Ski Schools";
					break;
					
					case PoInterest.POI_TYPE_BUDDY:
						name = "Buddies";
					break;
					
					case PoInterest.POI_TYPE_CDP:
						name = "Pins";
					break;
					
					default:
						name = "Not-defined";
				}
					
				name += "(" + map.mPoInterests.get(i).size() + ")";
				
				//using the upper case for the POI categories
				//name = name.toUpperCase();
				
				//create a new category for the map 
				//and insert to the category list by sorting against the desc.
				addCategory( name, i );
				
				//add poi name to the list
				int index = 0;
				ArrayList<PoiItem> poiItems = sPoiItemLists.get(i);
				for( PoInterest poi : map.mPoInterests.get(i) )
				{
					PoiInfoProvider.PoiItem  item = new PoiInfoProvider.PoiItem( poi.mName, index );					
					
					if( PoiInfoProvider.SORT_ITEM_LIST )
					{
						//sorted by the POI name
						int j;
						for( j = 0; j < index; ++j )
						{
							if( item.mName.compareTo( poiItems.get(j).mName ) <= 0 )
								break;
						}
						poiItems.add( j, item );
					}
					else
					{
						//un-sorted list
						poiItems.add( item );
					}
					++index;
				}
			}	
		}
	}
}