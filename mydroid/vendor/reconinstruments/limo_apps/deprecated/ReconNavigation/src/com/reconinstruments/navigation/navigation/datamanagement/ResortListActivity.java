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
public class ResortListActivity extends ListActivity 
{
	private String mCountryName = null; 
	private String mRegionName = null;
	private ArrayList<ResortInfo> mAvailableResorts = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mCountryName = intent.getStringExtra("countryName");
        mRegionName = intent.getStringExtra("regionName");

        CountryInfo countryInfo = ResortInfoProvider.getCountryInfo(mCountryName);
        if( countryInfo.mResorts == null )
        {
        	RegionInfo region = countryInfo.getRegion(mRegionName);
        	mAvailableResorts = region.getAvailableResorts();
            setListAdapter(new ArrayAdapter<ResortInfo>(this,
            		R.layout.limo_default_list_item, mAvailableResorts));       

        }
        else
        {
        	mAvailableResorts = countryInfo.getAvailableResorts();
	        setListAdapter(new ArrayAdapter<ResortInfo>(this,
	        		R.layout.limo_default_list_item, mAvailableResorts));       
        }
        final ListView listView = getListView();

        //single selection mode
        listView.setItemsCanFocus(true);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener( mItemClickListener );
        if( mRegionName == null )
        {
        	this.setTitle("Select a resort in " + mCountryName );
        }
        else
        {
        	this.setTitle("Select a resort in " + mCountryName  + "/" + mRegionName );
        }
    }
    
    private OnItemClickListener mItemClickListener = new OnItemClickListener( )
    {
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{

    		ResortInfo resort = mAvailableResorts.get(position);

    		
    		Intent intent = new Intent( );
    		intent.putExtra("countryName", mCountryName);
    		if( mRegionName != null )
    		{
    			intent.putExtra("regionName", mRegionName);
    		}
    		intent.putExtra("resortName", resort.mName);
    		
    		setResult(RESULT_OK, intent );
    		
    		//tell the activity to finish itself, and
    		//return back to the calling activity
    		finish( );
    	}
    	
    };

}