/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.navigation.R;

/**
 * This activity is for selecting a point-of-interest item 
 * from a specific category of POI. 
 * The poi item ID(the position in its correspondent category arraylist at a ShpMap) and the poi type
 * will be sent back to the calling activity
 */
public class PoiItemList extends ListActivity {
    
	int mPoiType = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.poi_list);
        

        
        final ListView listView = getListView();
        if( mPoiType == PoInterest.POI_TYPE_CDP )
        {
	        setListAdapter(new ArrayAdapter<PoiInfoProvider.PoiItem>(this,
	                R.layout.limo_multi_choice_list_item, PoiInfoProvider.sPoiItemLists.get(mPoiType)));

        	listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        	listView.setItemsCanFocus(false);
        	
        	View btnBar = (View)findViewById( R.id.poiList_button_bar );
        	btnBar.setVisibility(View.VISIBLE);
        	
        	Button deleteBtn = (Button)findViewById( R.id.btn_delete );
        	Button goBtn = (Button)findViewById( R.id.btn_go );
        	goBtn.setOnClickListener(mButtonClickListener);
        	deleteBtn.setOnClickListener(mButtonClickListener);
        }
        else
        {
	        setListAdapter(new ArrayAdapter<PoiInfoProvider.PoiItem>(this,
	                R.layout.limo_default_list_item, PoiInfoProvider.sPoiItemLists.get(mPoiType)));
        	View btnBar = (View)findViewById( R.id.poiList_button_bar );
        	btnBar.setVisibility(View.GONE);

        	listView.setItemsCanFocus(true);
        	listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
             
        
        listView.setOnItemClickListener( mItemClickListener );
       
        ImageView titleIcon = (ImageView)findViewById(R.id.title_icon);
        TextView title = (TextView)findViewById(R.id.title_text);
        switch( mPoiType )
        {
        case PoInterest.POI_TYPE_BANK:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.bank));
        	title.setText("Pick-up a Bank");        	
        break;

        case PoInterest.POI_TYPE_HOTEL:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.hotel));
        	title.setText("Pick-up a Hotel");        	
        break;

        
        case PoInterest.POI_TYPE_RESTAURANT:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.restaurant));
        	title.setText("Pick-up a Restaurant");                	
        break;

        case PoInterest.POI_TYPE_BAR:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.bar));
        	title.setText("Pick-up a Bar");                	
        break;

        
        case PoInterest.POI_TYPE_CHAIRLIFTING:   
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.lift));
        	title.setText("Pick-up a Chair-lifting Center");        
        break;
        
        case PoInterest.POI_TYPE_INFORMATION:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.info));
        	title.setText("Pick-up a Information Center");
        break;

        case PoInterest.POI_TYPE_CARPARKING:
        case PoInterest.POI_TYPE_SKIERDROPOFF_PARKING:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.parking));
        	title.setText("Pick-up a Parking Site");
        	//setTitle("Pick-up a Parking Site");        
        break;

        case PoInterest.POI_TYPE_RESTROOM:        	
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.restroom));
        	title.setText("Pick-up a Restroom");
        	//setTitle("Pick-up a Restroom");
        break;

        case PoInterest.POI_TYPE_PARK:       
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.park));
        	title.setText("Pick-up a Park");        	
        break;
        
        case PoInterest.POI_TYPE_SKISCHOOL:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.skischool));
        	title.setText("Pick-up a Ski-School");
        break;
        
        case PoInterest.POI_TYPE_CDP:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.pin));
        	title.setText("Pick-up a Pin");
        break;
        
        case PoInterest.POI_TYPE_BUDDY:
        	titleIcon.setImageDrawable(getResources().getDrawable(R.drawable.buddy));
        	title.setText("Pick-up a Buddy");
        break;

        }     
    }
    
    private OnItemClickListener mItemClickListener = new OnItemClickListener( )
    {
    	//send the selected item back
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		if( mPoiType != PoInterest.POI_TYPE_CDP )
    		{
	    		Intent intent = new Intent( );
	    		intent.putExtra("PoiItemAction",0);			//view poi
	    		intent.putExtra("PoiItemId", PoiInfoProvider.sPoiItemLists.get(mPoiType).get(position).mIndex );
	    		intent.putExtra("PoiType", mPoiType );
	    		
	    		
	    		setResult(RESULT_OK, intent );
	    		
	    		//tell the activity to finish itself, and
	    		//return back to the calling activity
	    		finish( );
    		}
    	}
    	
    };
    
    private View.OnClickListener mButtonClickListener = new View.OnClickListener()
    {
		
		@Override
		public void onClick(View v) 
		{
			switch( v.getId() )
			{
			case R.id.btn_delete:
			{
				ListView listView = getListView();
				SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
				int numSelected = 0;
				for( int i = 0;  i < listView.getCount(); ++i )
				{
					if( checkedItems.get(i) )
					{
						++numSelected;
					}						
				}
				
				if( numSelected > 0 )
				{
					
					int nextId = 0;
					int[] checkedItemIds = new int[numSelected]; 
					for( int i = 0;  i < listView.getCount(); ++i )
					{
						if( checkedItems.get(i) )
						{
							checkedItemIds[nextId++] = PoiInfoProvider.sPoiItemLists.get(mPoiType).get(i).mIndex;
						}						
					}
					
		    		Intent intent = new Intent( );
		    		intent.putExtra("PoiItemAction",1);			//delete poi					    		
		    		intent.putExtra("PoiType", mPoiType );
		    		intent.putExtra("PoiItemIds", checkedItemIds );

		    		setResult(RESULT_OK, intent );
		    		
		    		//tell the activity to finish itself, and
		    		//return back to the calling activity
		    		finish( );
				}
			}
			break;
			
			case R.id.btn_go:
			{
				ListView listView = getListView();
				SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
				int numSelected = 0;
				for( int i = 0;  i < listView.getCount(); ++i )
				{
					if( checkedItems.get(i) )
					{
						++numSelected;
					}						
				}
				
				if( numSelected > 0 )
				{
					
					int nextId = 0;
					for( int i = 0;  i < listView.getCount(); ++i )
					{
						if( checkedItems.get(i) )
						{
							nextId = i;
						}						
					}
					
					nextId = PoiInfoProvider.sPoiItemLists.get(mPoiType).get(nextId).mIndex;
		    		Intent intent = new Intent( );
		    		intent.putExtra("PoiItemAction",0);			//view poi					    		
		    		intent.putExtra("PoiType", mPoiType );
		    		intent.putExtra("PoiItemId", nextId );

		    		setResult(RESULT_OK, intent );
		    		
		    		//tell the activity to finish itself, and
		    		//return back to the calling activity
		    		finish( );
				}
			}
			break;
			}
			
		}
	};
}