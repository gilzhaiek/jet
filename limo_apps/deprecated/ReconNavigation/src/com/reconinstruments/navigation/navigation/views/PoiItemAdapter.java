/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.views;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.PoInterest;

public class PoiItemAdapter extends ArrayAdapter<PoInterest>
{
	private ArrayList<PoInterest> mPoiItems;
	private int mPoiType;
	
	public PoiItemAdapter( Context context, int textViewResourceId, ArrayList<PoInterest> items, int poiType )
	{
		super( context, textViewResourceId, items );
		mPoiItems = items;
		mPoiType = poiType;
	}
	
	@Override
	public View getView( int position, View convertView, ViewGroup parent )
	{		
		View retView = convertView;
		if( retView == null )
		{
			//create a new view from the poiCategoryitem_layout
			LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			retView = inflater.inflate(R.layout.poiitemview_item_layout, null);
		}
		
		PoInterest poi = mPoiItems.get(position);
		ImageView icon = (ImageView)retView.findViewById(R.id.poiitemview_item_icon);
		icon.setImageBitmap(poi.getIcon(poi.getType()));
		
		retView.setBackgroundResource(R.drawable.listselector);

		return retView;
		
	}
}
