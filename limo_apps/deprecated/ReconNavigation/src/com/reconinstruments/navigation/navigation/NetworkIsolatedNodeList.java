/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.reconinstruments.navigation.routing.ReVerifyInfoProvider;

/**
 * This activity is for selecting a point-of-interest item 
 * from a specific category of POI. 
 * The poi item ID(the position in its correspondent category arraylist at a ShpMap) and the poi type
 * will be sent back to the calling activity
 */
public class NetworkIsolatedNodeList extends ListActivity {
    
	int mNetworkId = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        mNetworkId = intent.getIntExtra("NetworkId", 0);

        setListAdapter(new ArrayAdapter<ReVerifyInfoProvider.LocationInfo>(this,
                android.R.layout.simple_list_item_1, ReVerifyInfoProvider.sNetworkLocationInfo.get(mNetworkId)));
                
        final ListView listView = getListView();

        //single selection mode
        listView.setItemsCanFocus(true);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener( mItemClickListener );

    }
    
    
    private OnItemClickListener mItemClickListener = new OnItemClickListener( )
    {
    	//send the selected item back
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		Intent intent = new Intent( );
    		PointF point = ReVerifyInfoProvider.sNetworkLocationInfo.get(mNetworkId).get(position).mPosition;
    		intent.putExtra("PositionX",  point.x);
    		intent.putExtra("PositionY", point.y );
    		
    		
    		setResult(RESULT_OK, intent );
    		
    		//tell the activity to finish itself, and
    		//return back to the calling activity
    		finish( );
    	}
    	
    };
}