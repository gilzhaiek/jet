package com.rxnetworks.rxnservicesxybrid;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.rxnetworks.xybridcommonlibrary.datacollector.RadioInfo;
import com.rxnetworks.xybridcommonlibrary.wifi.WifiReceiver;

public class WifiReceiverWrapper {

	public interface WifiConsumer
	{
		void onWifiScanComplete(final List<RadioInfo> wifiInfoList);
	};

	private final CopyOnWriteArrayList<WifiConsumer> mClients;
	private final WifiReceiver mWifiReceiver;
	private final Context mContext;
	
	private boolean registered;
	

    private BroadcastReceiver mInfoListReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
			Log.d("WifiManager", "onReceive");
			new Thread(new Runnable()
			{
				public void run()
				{
		            final List<RadioInfo> wifiInfoList = mWifiReceiver.getWifiInfoList();
		
		            for (WifiConsumer client : mClients)
		            {
		            	client.onWifiScanComplete(wifiInfoList);
		            }
				}
			}).start();
        }
    };
	
	public WifiReceiverWrapper(Context context) 
	{
		mContext = context;
		mWifiReceiver = new WifiReceiver(mContext);
		mClients = new CopyOnWriteArrayList<WifiConsumer>();
	}
	
	public void addObserver(WifiConsumer consumer)
	{
		if (!mClients.contains(consumer))
		{
			mClients.add(consumer);
		}
	}
	
	public void removeObserver(WifiConsumer consumer)
	{
		mClients.remove(consumer);
		
		if (mClients.isEmpty() && registered)
		{
			registered = false;
			mWifiReceiver.disable();
    	    mContext.unregisterReceiver(mInfoListReceiver);
		}
	}
	
	public void startScan()
	{
		Log.d("WifiManager", "startScan()");
		
		// If wireless is disabled, make sure that an empty result is returned
		WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled())
		{
			mInfoListReceiver.onReceive(null, null);
		}
		
		// Make sure that we're registered to receive the results
		if (!registered)
		{
            mContext.registerReceiver(mInfoListReceiver, new IntentFilter(WifiReceiver.XYBRID_WIFI_AVAILABLE));
            registered = true;
		}
		
		mWifiReceiver.enable();
		mWifiReceiver.start();
	}
}
