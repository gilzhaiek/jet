/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;


import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.MapView;
import com.reconinstruments.navigation.navigation.PoInterest;
import com.reconinstruments.navigation.navigation.PoiInfoProvider;

/**
 *The view for showing the point-of-interest items 
 */

public class PoiItemView extends OverlayView
{	
	MapManager mMapManager = null;
	TipedListView mPoiItemList = null;
	PoiItemAdapter mPoiItemAdapter = null;
	int mPoiType;
	int mPrevSelected = -1;						//the previously selected poiitem index; -1 means nothing selected before;
	Context mContext=null;
	
	public PoiItemView( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler throwBack, MapManager mapManager, int poiType )
	{
		super( context, overlayManager, throwBack );
		mContext = context;
		mThrowBack = throwBack;
		mMapManager = mapManager;
		mPoiType = poiType;

		//inflate a view from predefined xml file
        LayoutInflater factory = LayoutInflater.from(context);
        View navView = factory.inflate(R.layout.poi_item_view, null);
        this.addView(navView);
    
        mPoiItemList = (TipedListView)navView.findViewById(R.id.poiItemListView);
        
        //create the adapter for the list view
        mPoiItemAdapter = new PoiItemAdapter( context, R.layout.poiitemview_item_layout, mMapManager.mMap.mPoInterests.get( poiType ), poiType);
        
        mPoiItemList.setAdapter(mPoiItemAdapter); 
        mPoiItemList.setOnKeyListener(mListViewKeyListener);
          
        //Disable the tips provider
        mPoiItemList.setTipsProvider(mTipsProvider);
        
        mPoiItemList.setOnItemSelectedListener(mItemSelectedListener);
        
        //the code to remove the divider of the listview
        mPoiItemList.setDivider(null);
        mPoiItemList.setDividerHeight(0);

	}	

	/**
	 * Handle the key-pressed event of the listView to hide
	 * the PoiItemView when back key is pressed
	 */
	private View.OnKeyListener mListViewKeyListener = new View.OnKeyListener() 
    {	
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) 
		{
			//intercept the back key from the listView 
			// and translate it to cancel the menu view
			if( keyCode == KeyEvent.KEYCODE_BACK )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN )
				{
					mMapManager.mMapView.UnlockView();
					
					//dehilite selected POI
					if(mPrevSelected >= 0)
					{
						ArrayList<PoInterest> pois = mMapManager.mMap.mPoInterests.get(mPoiType);
						PoInterest poi = pois.get(mPrevSelected);
						poi.setHilited(false);
					}
					
					PoiItemView.this.onBackDown(mPoiItemList);
					return true;
				}
				else
				{
					return false;
				}
			}
			else if( keyCode == KeyEvent.KEYCODE_DPAD_CENTER )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN )
				{
					PoiItemView.this.onSelectDown(mPoiItemList);
					return true;
				}
				else
				{
					return false;
				}
				
			}
			else
				return false;
		}
	 };
		
	protected AlertDialog GetSelectDialog(View textEntryView)
	{
		return new AlertDialog.Builder( new ContextThemeWrapper( this.getContext(), android.R.style.Theme_Translucent_NoTitleBar ) )
        //AlertDialog dlg = new AlertDialog.Builder( this.getContext() )
            .setView(textEntryView)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) 
                {
    				//now let's fetch the selected poi item from mPoiItemList;
                	mPoiItemAdapter.getItem(mPoiItemList.getSelectedItemPosition()).toggleStatus();
    				mPoiItemAdapter.notifyDataSetChanged();
    				mMapManager.Save();
    				mMapManager.mMapView.invalidate();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            })
            .create();	
	}	 
	 
	private AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener() 
	{		
		
		@Override
		public void onItemSelected (AdapterView parent, View view, int position, long id)
		{
			//make sure that the event comes from the mPoiItemList
			//for robust coding
			if( parent == mPoiItemList )
			{				
				//make sure to call the itemSelectListener of the TipedListView
				//which handle the tips for the selected item;
				mPoiItemList.mItemSelectedListener.onItemSelected(parent, view, position, id);
				
				//now let's fetch the selected poi item from mPoiItemList;
				PoInterest poi = mPoiItemAdapter.getItem(position);				
				poi.setHilited(true);
				
				//let's center the map to the location of the selected point-of-interest
				mMapManager.mMapView.setCenter(poi.mPosition.x, poi.mPosition.y, false, true);
							
				//de-hilite the previous selected item
				if( mPrevSelected >= 0 )
				{
					if(mPoiItemAdapter.getCount() > mPrevSelected)
					{
						poi = mPoiItemAdapter.getItem(mPrevSelected);
						poi.setHilited(false);
					}
				}
				mPrevSelected = position;
			}
		}
		
		@Override
		public void onNothingSelected (AdapterView parent)
		{
			//make sure that the event comes from the mPoiItemList
			//for robust coding
			if( parent == mPoiItemList )
			{
				//make suer to call the itemSelectListener of the TipedListView
				//which handle the tips for the selected item;
				mPoiItemList.mItemSelectedListener.onNothingSelected(parent);
			}
		}
	};
		
   @Override
	public boolean onDownArrowDown(  View srcView )
	{		  
	   mPoiItemList.requestFocus();
	   return true;
	}
	
	@Override
	public boolean onUpArrowDown(  View srcView )
	{			
		mPoiItemList.requestFocus();
		return true;
	} 
	
	@Override
	public void onPreShow()
	{
		//record the MapView feature so it can be restored late on
		mMapManager.recordMapFeature();
		
		//now set the feature specifically for dropping pin
		mMapManager.mMapView.clearFeatures();
		mMapManager.mMapView.setFeature(MapView.MAP_FEATURE_LOCATION, true);
		//mMapManager.mMapView.setFeature(MapView.MAP_FEATURE_COMPASS, true);
		
		PoInterest owner = mMapManager.getOwner();
		if( owner != null )
		{
			owner.setStatus( PoInterest.OWNER_STATUS_MEASURE_CENTER );
		}

	}
	
	@Override
	public void onPreHide()
	{
		mMapManager.restoreMapFeature(true);
		
		PoInterest owner = mMapManager.getOwner();
		if( owner != null )
		{
			owner.setStatus( PoInterest.OWNER_STATUS_MEASURE_NONE );
		}

	}
	
	@Override
	public boolean onSelectDown( View srcView )
	{
		if( mPoiType == PoInterest.POI_TYPE_CDP )
		{
			//trigger the pin delete popup dialog
			
	        LayoutInflater factory = LayoutInflater.from(this.getContext().getApplicationContext());
	        final View textEntryView = factory.inflate(R.layout.pin_delete_view, null);
	                
	        AlertDialog dlg = new AlertDialog.Builder( new ContextThemeWrapper( this.getContext(), android.R.style.Theme_Translucent_NoTitleBar ) )
	        //AlertDialog dlg = new AlertDialog.Builder( this.getContext() )
	            .setView(textEntryView)
	            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) 
	                {
	    				//now let's fetch the selected poi item from mPoiItemList;
	                	PoInterest poi = mPoiItemAdapter.getItem(mPoiItemList.getSelectedItemPosition());	    			
	    				mMapManager.removePin(poi);
	    			
	    				if(mPoiItemList.getCount() > 1)  // Make sure we have at least 2 elements
	    				{
	    					if(mPoiItemList.getSelectedItemPosition() == 0)  // If it is the first one - we go to the next
	    					{
	    						mPoiItemList.setSelection(1);
	    					}
	    					else
	    					{
	    						mPoiItemList.setSelection(mPoiItemList.getSelectedItemPosition()-1);  // Try to go to the previous
	    					}
	    				}
	    				
	    				mPoiItemAdapter.remove(poi);
	    				mPoiItemAdapter.notifyDataSetChanged();
	                }
	            })
	            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {

	                }
	            })
	            .create();

	        dlg.show();
		}
		else if( mPoiType == PoInterest.POI_TYPE_BUDDY )
		{
	        LayoutInflater factory = LayoutInflater.from(this.getContext().getApplicationContext());
	        final View textEntryView = factory.inflate(R.layout.pin_delete_view, null);
			//now let's fetch the selected poi item from mPoiItemList;
	        TextView msgView  =  (TextView)textEntryView.findViewById(R.id.msg_view);       
	                
	        PoInterest poi = mPoiItemAdapter.getItem(mPoiItemList.getSelectedItemPosition());
		
			if( poi.getStatus() != PoInterest.POI_STATUS_TRACKED )
			{
				msgView.setText("Start tracking this buddy");
			}
			else
			{
				msgView.setText("Stop tracking this buddy");
			}
	        

	        AlertDialog dlg = GetSelectDialog(textEntryView);

	        dlg.show();
			
		}
		else if(mPoiType == PoInterest.POI_TYPE_OWNER)
		{
			// Do Nothing
		}
		else {
	        LayoutInflater factory = LayoutInflater.from(this.getContext().getApplicationContext());
	        final View textEntryView = factory.inflate(R.layout.pin_delete_view, null);
			//now let's fetch the selected poi item from mPoiItemList;
	        TextView msgView  =  (TextView)textEntryView.findViewById(R.id.msg_view);       
	                
	        PoInterest poi = mPoiItemAdapter.getItem(mPoiItemList.getSelectedItemPosition());
		
			if( poi.getStatus() != PoInterest.POI_STATUS_TRACKED )
			{
				msgView.setText("Start tracking " + poi.mName);
			}
			else
			{
				msgView.setText("Stop tracking " + poi.mName);
			}
	       
	        AlertDialog dlg = GetSelectDialog(textEntryView);
	        dlg.show();
		}
		return true;
	}
	private TipedListView.ITipsProvider mTipsProvider = new TipedListView.ITipsProvider() 
	{
		
		@Override
		public String getTips(int position) 
		{
			return PoiInfoProvider.sPoiItemLists.get(mPoiType).get(position).toString();				
		}
	};
	
	
}