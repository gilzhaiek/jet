/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

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
public class RegionListActivity extends ListActivity 
{
	String mCountryName = null; 
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mCountryName = intent.getStringExtra("countryName");

        CountryInfo countryInfo = ResortInfoProvider.getCountryInfo(mCountryName);
        
        //setListAdapter(new ArrayAdapter<RegionInfo>(this,
         //       android.R.layout.simple_list_item_1, countryInfo.mRegions));
        
        setListAdapter(new ArrayAdapter<RegionInfo>(this,
                R.layout.limo_default_list_item, countryInfo.mRegions));     
        
        final ListView listView = getListView();

        //single selection mode
        listView.setItemsCanFocus(true);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener( mItemClickListener );
        
        this.setTitle("Select a region in " + mCountryName);
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
    		CountryInfo countryInfo = ResortInfoProvider.getCountryInfo(mCountryName);
    		RegionInfo regionInfo = countryInfo.mRegions.get(position);
    		
    		Intent intent = new Intent( RegionListActivity.this, ResortListActivity.class );
    		intent.putExtra("countryName", mCountryName);
    		intent.putExtra("regionName", regionInfo.mName);
    		startActivityForResult(intent, GET_RESORT);    			
    	}
    	
    };

}