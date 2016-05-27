/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.reconinstruments.navigation.routing.ReVerifyInfoProvider;

/**
 * This activity lists out all non-empty Point-of-interest
 * categories and let user to select an category and enter into
 * point-of-interest item selection activity
 */
public class NetworkDiagnosticList extends ListActivity 
{
	/*
	 * private class for defining an ArrayAdapter that has it own view of list item
	 * 
	 */

	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        setListAdapter(new ArrayAdapter<ReVerifyInfoProvider.NetworkInfo>(this,
                android.R.layout.simple_list_item_1, ReVerifyInfoProvider.sNetworkInfo));
                
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
        if (requestCode == GET_ISOLATED_NODE && data != null ) {

        	float x = data.getFloatExtra("PositionX", 0.0f);
        	float y = data.getFloatExtra("PositionY", 0.0f);
        	
        	//return the result back to the upper level call
       		Intent intent = new Intent( );
    		intent.putExtra("PositionX", x );
    		intent.putExtra("PositionY", y );
    		
    		setResult(RESULT_OK, intent );
    		
        	//force the categoryList activity to finish and 
    		//return back to the calling activity, since
        	//we already got the selected item back from the PoiItemList activity
        	finish( );
        }
    }


    // Definition of the one requestCode we use for receiving poi-item selection data.
    static final private int GET_ISOLATED_NODE = 0;
    
    private OnItemClickListener mItemClickListener = new OnItemClickListener( )
    {
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		Intent intent = new Intent( NetworkDiagnosticList.this, NetworkIsolatedNodeList.class );
    		intent.putExtra("NetworkId", position);
    		startActivityForResult(intent, GET_ISOLATED_NODE);
    	}
    	
    };
}
