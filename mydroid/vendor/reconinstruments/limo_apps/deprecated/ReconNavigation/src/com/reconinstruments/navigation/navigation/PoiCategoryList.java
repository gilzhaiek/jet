/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.navigation.R;

/**
 * This activity lists out all non-empty Point-of-interest
 * categories and let user to select an category and enter into
 * point-of-interest item selection activity
 */
public class PoiCategoryList extends ListActivity 
{
	/*
	 * private class for defining an ArrayAdapter that has it own view of list item
	 * 
	 */
	private class PoiCategoryAdapter extends ArrayAdapter<PoiInfoProvider.PoiCategory>
	{
		private ArrayList<PoiInfoProvider.PoiCategory> mCategories;
		
		public PoiCategoryAdapter( Context context, int textViewResourceId, ArrayList<PoiInfoProvider.PoiCategory> categories )
		{
			super( context, textViewResourceId, categories );
			mCategories = categories;
		}
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			View v = convertView;
			if( v == null )
			{
				//create a new view from the poiCategoryitem_layout
				LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.poicategoryitem_layout, null);
			}
			
			PoiInfoProvider.PoiCategory category = mCategories.get(position);
			
			if( category != null )
			{
				ImageView icon = (ImageView)v.findViewById(R.id.poi_category_item_icon);
				
				switch( category.mPoiType )
				{
				case PoInterest.POI_TYPE_BANK:
					icon.setImageResource(R.drawable.hotel);
				break;
				
				case PoInterest.POI_TYPE_BAR:
				case PoInterest.POI_TYPE_RESTAURANT:
					icon.setImageResource(R.drawable.restaurant);
				break;
				
				case PoInterest.POI_TYPE_CARPARKING:
				case PoInterest.POI_TYPE_SKIERDROPOFF_PARKING:
					icon.setImageResource(R.drawable.parking);
				break;
				
				case PoInterest.POI_TYPE_CHAIRLIFTING:
					icon.setImageResource(R.drawable.lift);
				break;
				
				case PoInterest.POI_TYPE_HOTEL:
					icon.setImageResource(R.drawable.hotel);
				break;
				
				case PoInterest.POI_TYPE_INFORMATION:
					icon.setImageResource(R.drawable.info);
				break;
				
				case PoInterest.POI_TYPE_PARK:
					icon.setImageResource(R.drawable.park);
				break;
				
				case PoInterest.POI_TYPE_RESTROOM:
					icon.setImageResource(R.drawable.restroom);
				break;
				
				case PoInterest.POI_TYPE_SKISCHOOL:
					icon.setImageResource(R.drawable.skischool);
				break;
				
				case PoInterest.POI_TYPE_CDP:
					icon.setImageResource(R.drawable.cdp);
				break;
				
				case PoInterest.POI_TYPE_BUDDY:
					icon.setImageResource(R.drawable.buddy);
				break;
				}
				
				TextView title = (TextView)v.findViewById(R.id.poi_category_item_text);
				title.setText(category.mDesc);
			}
		
			return v;
			
		}
		
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        PoiCategoryAdapter categoryAdapter = new PoiCategoryAdapter(this, R.layout.poicategoryitem_layout, PoiInfoProvider.sPoiCategories);
 
        setListAdapter( categoryAdapter );        
        final ListView listView = getListView();

        //single selection mode
        listView.setItemsCanFocus(true);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener( mItemClickListener );            
    }

    /**
     * This method is called when the PoiItemList activity has finished, with the
     * result it supplied.
     * 
     * @param requestCode The original request code as given to
     *                    startActivity().
     * @param resultCode From sending activity as per setResult().
     * @param data From sending activity as per setResult().
     */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        // You can use the requestCode to select between multiple child
        // activities you may have started.  Here there is only one thing
        // we launch.
    	//make sure we test if data is null or not, coz when BACK is press
    	//within the PoiItemList activity, no item will be selected
    	//thus, a null intent will be return. For this special case
    	//we should not exit current activity.
        if (requestCode == GET_POI && data != null ) 
        {
        	
        	int actionCode = data.getIntExtra("PoiItemAction", 0);
        	int poiItemType = data.getIntExtra("PoiType", 0);
        	
        	//return the result back to the upper level call
       		Intent intent = new Intent( );
    		
    		intent.putExtra("PoiType", poiItemType );
    		intent.putExtra("PoiItemAction", actionCode);
    	
    		if( actionCode == 0 )
    		{
    			int poiItemId = data.getIntExtra("PoiItemId", 0);	
    			intent.putExtra("PoiItemId", poiItemId );
    		}
    		else
    		{
    			int[] poiItemIds = data.getIntArrayExtra("PoiItemIds");
    			intent.putExtra("PoiItemIds", poiItemIds );
    		}
    		setResult(RESULT_OK, intent );
    		
        	//force the categoryList activity to finish and 
    		//return back to the calling activity, since
        	//we already got the selected item back from the PoiItemList activity
        	finish( );
        }
    }


    // Definition of the one requestCode we use for receiving poi-item selection data.
    static final private int GET_POI = 0;
    
    private OnItemClickListener mItemClickListener = new OnItemClickListener( )
    {
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		Intent intent = new Intent( PoiCategoryList.this, PoiItemList.class );
    		intent.putExtra("PoiType", PoiInfoProvider.sPoiCategories.get(position).mPoiType);
    		startActivityForResult(intent, GET_POI);
    	}
    	
    };
}
