/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.reconinstruments.navigation.R;

/**
 * This activity lists out all countries that has ski resorts
 * that has mountain dynamic map data.
 * When the country is selected, either all the resorts will be list out
 * or the sub-region list will be shown if the country has sub-region defined
 * in the database 
 */
public class CountryListActivity extends ListActivity 
{
	//the list for keeping all countries that has some resort map availabe on device
	private ArrayList<CountryInfo> mAvailableCountries = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {    	
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        mAvailableCountries = ResortInfoProvider.getAvailableCountries();
        
        //setListAdapter(new ArrayAdapter<CountryInfo>(this,
        //        android.R.layout.simple_list_item_1, ResortInfoProvider.sCountries));       

        setListAdapter(new ArrayAdapter<CountryInfo>(this,
                R.layout.limo_default_list_item, mAvailableCountries ));       

        final ListView listView = getListView();

        //single selection mode
        listView.setItemsCanFocus(true);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener( mItemClickListener );

    }

    /**
     * This method is called when the CountryListActivity activity has finished, with the
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
        if (requestCode == GET_RESORT && data != null ) {

        	String resortName = data.getStringExtra("resortName");
        	String regionName = data.getStringExtra("regionName");
        	String countryName = data.getStringExtra("countryName");
        	
        	//return the result back to the upper level call
       		Intent intent = new Intent( );
    		intent.putExtra("resortName", resortName );
    		intent.putExtra("regionName", regionName );
    		intent.putExtra("countryName", countryName);
    		
    		setResult(RESULT_OK, intent );
    		
        	//force the categoryList activity to finish and 
    		//return back to the calling activity, since
        	//we already got the selected item back from the PoiItemList activity
        	finish( );
        }
    }


    // Definition of the one requestCode we use for receiving poi-item selection data.
    static final private int GET_RESORT = 0;
    
    private OnItemClickListener mItemClickListener = new OnItemClickListener( )
    {
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		CountryInfo countryInfo = mAvailableCountries.get(position);
    		
    		//the country has no sub regions
    		if( countryInfo.mRegions == null )
    		{
        		Intent intent = new Intent( CountryListActivity.this, ResortListActivity.class );
        		intent.putExtra("countryName", countryInfo.mName);
        		startActivityForResult(intent, GET_RESORT);    			
    		}
    		else
    		{
    			//list out the sub regions
        		Intent intent = new Intent( CountryListActivity.this, RegionListActivity.class );
        		intent.putExtra("countryName", countryInfo.mName);
        		startActivityForResult(intent, GET_RESORT);    			

    		}
    	}    	
    };

}