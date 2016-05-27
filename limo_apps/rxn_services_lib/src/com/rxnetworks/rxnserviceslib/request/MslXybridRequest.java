package com.rxnetworks.rxnserviceslib.request;

import java.io.ByteArrayOutputStream;

import android.util.Log;

import com.rxnetworks.rxnserviceslib.SocketServer;

public class MslXybridRequest {

	public String host;
	public int maxIndex;
	public int port;
	public String vendorId;
	public String vendorSalt;
	public int gpsWeek;
	public Boolean useWifi;
	public Boolean useCell;
	public Boolean donate;

	public Boolean rt;
	public Boolean bce;
	public Boolean synchro;	

	private Boolean filtered;

	protected final static String TAG = "RXNetworksService.XybridRequest";

	public static void xmlSocketSendReply(SocketServer socket, final byte[] payload)
	{
		try
		{
			ByteArrayOutputStream xmlMessage = new ByteArrayOutputStream();

			xmlMessage.write("<RXN_Response>\n".getBytes());
			xmlMessage.write("<name>xybrid_response</name>\n".getBytes());
			xmlMessage.write("<data>\n".getBytes());
			xmlMessage.write("<data_size>".getBytes());
			xmlMessage.write(Integer.toString(payload.length).getBytes()); 
			xmlMessage.write("</data_size>\n".getBytes());	 	
			xmlMessage.write("<response><![CDATA[".getBytes());
			xmlMessage.write(payload);
			xmlMessage.write("]]></response>\n".getBytes());
			xmlMessage.write("</data>\n".getBytes());
			xmlMessage.write("</RXN_Response>".getBytes());
			xmlMessage.write(0);

			socket.send(xmlMessage.toByteArray());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public Boolean getFiltered()
	{
		return filtered;
	}

	public void setFiltered(Boolean filtered)
	{
		this.filtered = filtered;
	}

	public int getRequestMask()
	{
		Log.d(TAG, "getRequestMask()");
		if (rt && !bce)
			return 4194304;		// REFERENCE_LOCATION
		else if (filtered && rt)
			return 4718592; 	// REFERENCE_LOCATION_AND_NAVIGATION_FILTERED
		else if (!filtered && rt)
			return 4456448; 	// REFERENCE_LOCATION_AND_NAVIGATION_COMPOSITE
		else if (filtered && !rt)
			return 524288; 		// NAVIGATION_FILTERED
		else if (!filtered && !rt)
			return 262144;		// NAVIGATION_COMPOSITE

		return 0;		
	}
}
