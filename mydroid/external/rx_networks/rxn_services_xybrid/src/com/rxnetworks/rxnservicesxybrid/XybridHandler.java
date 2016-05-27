package com.rxnetworks.rxnservicesxybrid;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.rxnetworks.rxnserviceslib.SocketServer;
import com.rxnetworks.rxnserviceslib.request.MslXybridRequest;
import com.rxnetworks.xybridclientlibrary.XybridClientLibrary;
import com.rxnetworks.xybridclientlibrary.xybridrequest.XybridRequest;
import com.rxnetworks.xybridcommonlibrary.datacollector.RadioInfo;
import com.rxnetworks.xybridcommonlibrary.location.RXN_RefLocation_t;
import com.rxnetworks.xybridcommonlibrary.network.HttpRequest;
import com.rxnetworks.xybridcommonlibrary.telephony.TelephonyListener;

public class XybridHandler implements WifiReceiverWrapper.WifiConsumer {

	private final SocketServer mSocket;
	private MslXybridRequest mRequest;
	private final TelephonyListener mTelephonyListener;
	private final WifiReceiverWrapper mWifiReceiverWrapper;
	private final Context mContext;

	private MPRHandler mMPRHandler;

	private static final String TAG = "XybridHandler";
	private static final String POSITION_URI = "xybridserver/RXNXybrid";
	private static final String DONATE_URI = "xybridserver/RXNRoam";

	public void onWifiScanComplete(final List<RadioInfo> wifiInfoList)
	{
		Log.d(TAG, "Received radio data");

		// Stop listening to events right away so that we don't accidentally send
		// extra requests to the server.
		stopListeningToWifi(this);

		final List<List<RadioInfo>> radioInfoList = buildRadioList(wifiInfoList);

		if (radioInfoList.isEmpty())
		{
			Log.d(TAG, "No radio data found.");
			xmlSocketSendError();
		}
		else
		{
			// Make a coarse position request.
			handleXybridRequest(radioInfoList);			        
		}
	}

	public XybridHandler(Context context, SocketServer socket)
	{	
		mContext = context;
		mSocket = socket;
		mTelephonyListener = new TelephonyListener(context);
		mWifiReceiverWrapper = new WifiReceiverWrapper(context);
	}

	public synchronized void start(final MslXybridRequest request)
	{
		Log.d(TAG, "Radio scan starting.  Wi-Fi scan: " + request.useWifi + "; Cell id scan: " + request.useCell);

		mRequest = request;

		startScanning(mRequest, this);
	}

	public void startScanning(final MslXybridRequest request, final WifiReceiverWrapper.WifiConsumer wifiObserver)
	{
		if (request.useWifi)
		{
			mWifiReceiverWrapper.addObserver(wifiObserver);
			mWifiReceiverWrapper.startScan();
		}

		if (request.useCell)
		{
			mTelephonyListener.enable();
			mTelephonyListener.start();
		}

		// If configured not to use wifi, complete the scan with a null list to
		// proceed with the request.
		if (!request.useWifi)
		{
			wifiObserver.onWifiScanComplete(null);
		}    	
	}

	public void stopListeningToWifi(final WifiReceiverWrapper.WifiConsumer wifiObserver)
	{
		mWifiReceiverWrapper.removeObserver(wifiObserver);
	}

	private List<List<RadioInfo>> buildRadioList(final List<RadioInfo> wifiInfoList)
	{
		final List<List<RadioInfo>> radioInfoList = new ArrayList<List<RadioInfo>>();

		List<RadioInfo> cellInfoList = null;
		if (mTelephonyListener.isEnabled())
		{
			cellInfoList = mTelephonyListener.getCellInfoList();
			mTelephonyListener.disable();
		}

		if (cellInfoList != null)
		{
			radioInfoList.add(cellInfoList);
		}

		if (wifiInfoList != null)
		{
			radioInfoList.add(wifiInfoList);
		}

		return radioInfoList;
	}

	private void handleXybridRequest(final List<List<RadioInfo>> radioInfoList) 
	{
		Log.d(TAG, "Handling Xybrid request");

		// Send Xybrid request to server and send results to MSL
		try {
			final byte[] result = buildXybridRequest(radioInfoList);
			Log.i(TAG, "Xybrid: received reply");

			if (result == null)
			{
				Log.w(TAG, "Error in Xybrid response.");
				xmlSocketSendError();
				return;
			}

			MslXybridRequest.xmlSocketSendReply(mSocket, result);

			// Check for Measure Position Response (MPR)
			if (new String(result).contains("<MPR"))
			{
				Log.d(TAG, "MPR requested");

				startObservation();
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	private void startObservation()
	{
		if (!mRequest.donate)
		{
			Log.e(TAG, "MPR requested, but response disabled in configuration.");
		}
		else
		{
			if (mMPRHandler == null)
			{
				mMPRHandler = new MPRHandler(mContext, mRequest, this);
				mMPRHandler.start();
			}
			else
			{
				Log.d(TAG, "MPR already in progress");
			}
		}

	}

	private void makeObservation(final List<List<RadioInfo>> radioInfoList, RXN_RefLocation_t location) 
	{
		if (!radioInfoList.isEmpty())
		{
			Log.i(TAG, "Sending observation");
			try {
				sendObservation(radioInfoList, location);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}

	private final String CreateHostName(String hostString, int index)
	{
		return hostString.replace("%", Integer.toString(index));
	}

	private final byte[] buildXybridRequest(final List<List<RadioInfo>> radioInfoList)
	{
		for (int i = 1; i <= mRequest.maxIndex; ++i)
		{
			String hostname = CreateHostName(mRequest.host, i);

			Log.d(TAG, "Sending request to " + hostname);

			XybridRequest xybridRequest = 
					new XybridRequest(hostname, mRequest.port, POSITION_URI, mRequest.vendorId, mRequest.vendorSalt, mRequest.gpsWeek * 604800, false);

			String requestStr = xybridRequest.getParamString(radioInfoList, null, mRequest.getRequestMask());
			Log.d(TAG, requestStr);
			HttpRequest req = new HttpRequest();
			req.makeRequest(xybridRequest.getUrlString(), requestStr, HttpRequest.REQUEST_METHOD_POST, false);

			// If the server returned 404, no location was found (v2 interface).  Trigger an automatic MPR.
			if (req.getResponseCode() == 404)
			{
				Log.w(TAG, "Server returned 404 - no location found");
				Log.d(TAG, "Triggering automatic MPR");
				startObservation();
				break;
			}
			else if (req.getResponseCode() == -1)
			{
				// If there was an exception, let's investigate
				String responseMessage = req.getResponseMessage();

				if (responseMessage == null)							// Fallback if no message
					continue;
				else if (responseMessage.contains("authentication"))	// Don't fallback if authentication failed
					break;
				else
					continue;											// Fallback for all other reasons
			}
			else if (req.getResponseCode() >= 500)
			{
				// If there was a server error, fallback to the next server
				continue;
			}
			else
			{
				return req.getBytes();
			}
		}

		return null;
	}

	private String sendObservation(final List<List<RadioInfo>> radioInfoList, RXN_RefLocation_t location)
	{
		XybridClientLibrary xybridClientLibrary = 
				new XybridClientLibrary(mRequest.host, mRequest.port, DONATE_URI, mRequest.vendorId, mRequest.vendorSalt, mRequest.gpsWeek * 604800, false);

		String requestStr = xybridClientLibrary.getDonationString(radioInfoList, location);
		return new HttpRequest().makeRequest(xybridClientLibrary.getUrlString(), requestStr, HttpRequest.REQUEST_METHOD_POST, false);
	}

	private void xmlSocketSendError()
	{
        String xmlMessage = "<RXN_Response>\n"
            + "<name>xybrid_position_error</name>\n" + "<data>\n"
            + "SocketError\n" + "</data>\n" + "</RXN_Response>\0";

		Log.d(TAG, "Sending message to client:" + xmlMessage);

		try
		{
			mSocket.send(xmlMessage.getBytes());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void handleObservation(final RXN_RefLocation_t location, final List<RadioInfo> wifiInfoList) 
	{
		final List<List<RadioInfo>> radioInfoList = buildRadioList(wifiInfoList);

		if (radioInfoList == null)
		{
			Log.w(TAG, "No radio information available for MPR");
			return;
		}

		if (location == null)
		{
			Log.w(TAG, "No GPS location for MPR");
			return;
		}

		makeObservation(radioInfoList, location);

		mMPRHandler = null;
	}
}
