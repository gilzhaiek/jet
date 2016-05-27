package com.rxnetworks.rxnserviceslib;

import android.util.Log;

public class SendDeviceID {
	private SocketServer socket;
	public SendDeviceID(SocketServer socket)
	{
		this.socket = socket;
	}
	public void xmlSocketSend(String sIMEI, String sIMSI)
	{
		String xmlMessage;
		try
		{
			xmlMessage = "<RXN_Response>\n" +
			"<name>Device_ID</name>\n" +
			"<data>\n" +
			"<IMEI>" + sIMEI + "</IMEI>\n" +
			"<IMSI>" + sIMSI + "</IMSI>\n" +
			"</data>\n" +
			"</RXN_Response>\0";
			
			socket.send(xmlMessage.getBytes());
		}
		catch(NullPointerException npe)
		{
			Log.i("RXNetworksService.SendDeviceID", "Nothing sent to client.");
		}		
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
