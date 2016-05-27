package com.rxnetworks.rxnservicesxybrid;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.rxnetworks.rxnserviceslib.request.MslXybridRequest;
import com.rxnetworks.xybridcommonlibrary.datacollector.RadioInfo;
import com.rxnetworks.xybridcommonlibrary.location.RXN_RefLocation_t;

public class MPRHandler implements GpsLocationAgent.GpsConsumer, WifiReceiverWrapper.WifiConsumer {
	
    private final GpsLocationAgent mGpsReceiverWrapper;
    private final MslXybridRequest mRequest;
    private final XybridHandler mXybridHandler;
	private RXN_RefLocation_t mGPSLocation;

	private final static String TAG = "MPRHandler";
	
	public void onLocationAvailable(final RXN_RefLocation_t location)
	{
        Log.i(TAG, "GPS fix received");
        
        mGPSLocation = location;

        mXybridHandler.startScanning(mRequest, this);
	}
	
    public void onWifiScanComplete(final List<RadioInfo> wifiInfoList)
    {
    	Log.d(TAG, "Received radio data for MPR");
    	
    	// Stop listening to events right away so that we don't accidentally send
    	// extra requests to the server.
    	mXybridHandler.stopListeningToWifi(this);
    	
    	mXybridHandler.handleObservation(mGPSLocation, wifiInfoList);
    }
	
	public MPRHandler(Context context, MslXybridRequest request, XybridHandler xybridHandler)
	{
        mRequest = request;
        mXybridHandler = xybridHandler;
        mGpsReceiverWrapper = new GpsLocationAgent(context);
	}

	public void start() 
	{
		Log.d(TAG, "Starting");
        mGPSLocation = null;
        mGpsReceiverWrapper.addObserver(this);
       	mGpsReceiverWrapper.requestGpsFix();
	}

}
