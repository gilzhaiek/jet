/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.util.Log;

import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage;
import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage.BuddyInfo;
import com.reconinstruments.modlivemobile.dto.message.XMLMessage;


/**
 *This class receive a list of buddy information described by an XML chunk
 *and parse the buddy information to send to navigation system for udpate
 */

public class BuddyInfoReceiver extends BroadcastReceiver
{
	static final String LOG_TAG = "BuddyInfoReceiver";	
	private CustomPoiManager mManager;
	
	public BuddyInfoReceiver( CustomPoiManager manager )
	{
		mManager = manager;
	}
	
	@Override
	public void onReceive( Context context, Intent intent )
	{
		//we only act on the action defined by ACTION_TAG 
		if( intent.getAction().compareTo(XMLMessage.BUDDY_INFO_MESSAGE ) == 0 )
		{
			String buddyInfo = intent.getStringExtra("message");
			
			BuddyInfoMessage message = new BuddyInfoMessage( );
			
			@SuppressWarnings("unchecked")
			ArrayList<BuddyInfo> buddyList = (ArrayList<BuddyInfo>)message.parse(buddyInfo);
			
			for( BuddyInfo buddy : buddyList )
			{

    			CustomPoiManager.CustomPoi poi = mManager.findPoi(buddy.localId, PoInterest.POI_TYPE_BUDDY);
    			if( poi == null )
    			{
    				PointF location = Util.mapLatLngToLocal(buddy.location.getLatitude(), buddy.location.getLongitude());
    				poi = mManager.new CustomPoi(location.x, location.y,  buddy.name, buddy.localId, PoInterest.POI_TYPE_BUDDY);
    				mManager.addPoi(poi);
    				Log.d(LOG_TAG, "Created a new buddy: " + buddy.name );
    			}
    			
    			mManager.updatePoiLocation(buddy.localId, PoInterest.POI_TYPE_BUDDY, buddy.location.getLatitude(), buddy.location.getLongitude());
    			poi.updateLocation(buddy.location.getLatitude(), buddy.location.getLongitude());
    			Log.d(LOG_TAG, "Update buddy: " + buddy.name + " located at(" + buddy.location.getLatitude() + "," + buddy.location.getLongitude()  );

			}
			
			mManager.postCustomPoiUpdated();
		}
	}
}