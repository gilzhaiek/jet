package com.reconinstruments.geodataservice.datasourcemanager.DynamicData;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.DynamicAttributeDataCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.GeoBuddyInfo;

public class BuddyTranscoder extends DynamicDataTranscoder 		// transcodes data from RECON buddy server into geodata server objects
{
// constants 
	private final static String TAG = "BuddyTranscoder";
	public  final  static String 	RECON_BUDDY_BROADCAST = "com.reconinstruments.buddy.new_dynamic_buddy_data";
	public  final  static String 	GEODATASERVICE_BROADCAST_NEW_DYNAMIC_BUDDY_DATA = "com.reconinstruments.geodataservice.new_dynamic_buddy_data";
	
// members 
	int		mNumRegisteredClients = 0;
	ArrayList<GeoBuddyInfo> mCurrentBuddies = new ArrayList<GeoBuddyInfo>();
	protected static Context 				mParentContext = null;
	
// methods
	public BuddyTranscoder(DataSourceManager dsm, Context context,DevTestingState _devTestingState) {
		super(Capability.DataSources.RECON_BUDDY_TRACKING_SERVER, dsm, context, _devTestingState); // set up mSourceID and other variables
		mParentContext = context;
	}
	
	@SuppressWarnings("static-access")
	public void init() throws Exception {
		super.init();
		
		// register for buddy broadcast
    	IntentFilter filter1 = new IntentFilter();
    	filter1.addAction(RECON_BUDDY_BROADCAST);		
    	filter1.addCategory(Intent.CATEGORY_DEFAULT);
    	super.mContext.registerReceiver(mBuddyDataReceiver, filter1);

//		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
//			 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.BUDDY_TRANSCODER_CREATE_ERROR.ordinal()) == 1) ) {
//				throw new Exception ("Forced BUDDYTRANSCODER creation error.");
//		}
		
	}

//============================================
// handler for live buddy data from BroadcastReceiver 
   private BroadcastReceiver mBuddyDataReceiver = new BroadcastReceiver() {
     	@Override
     	public void onReceive(Context context, Intent intent) {

     		Log.d(TAG, "Received new buddy data");
     		
     		// update local buddy state - mCurrentBuddies
     		mCurrentBuddies.clear();
//     		for(BuddyInfo buddy : intent..buddy.) {
//     			mCurrentBuddies.add(new BuddyInfo());
//			}
     		if(mNumRegisteredClients > 0) {    	// broadcast to registered clients
     		 	Intent newIntent = new Intent();
    			intent.setAction(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_BUDDY_DATA);
    			intent.addCategory(Intent.CATEGORY_DEFAULT);
    			intent.putExtra("BuddyInfoArray", mCurrentBuddies);			// use getIntent().getSerializableExtra("BuddyInfoArray");  to retrieve in listener
    			mParentContext.sendBroadcast(newIntent); 

     		}

     	}
    };

	@Override
	public void AddCapabilities(ArrayList<Capability> capabilitiesArray) {
		DynamicAttributeDataCapability newDADCap = new DynamicAttributeDataCapability(Capability.CapabilityTypes.DYNAMIC_BUDDY_POSITION, mSourceID);
		capabilitiesArray.add(newDADCap);
	}

	@Override
	public void RegisterForCapability() {
		mNumRegisteredClients ++;
	}
	
	@Override
	public void UnregisterForCapability() {
		if(mNumRegisteredClients > 0) {
			mNumRegisteredClients --;
		}
	}
	



}
