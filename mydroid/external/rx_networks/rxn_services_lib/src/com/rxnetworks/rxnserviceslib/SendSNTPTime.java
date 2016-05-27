package com.rxnetworks.rxnserviceslib;

import android.util.Log;

public class SendSNTPTime {
	private SocketServer socket;
	public SendSNTPTime(SocketServer socket)
	{
		this.socket = socket;
	}
	public void xmlSocketSend(long time, long clockOffset)
	{
		String xmlMessage;
		try
		{
			xmlMessage = "<RXN_Response>\n" +
			"<name>SNTP_Time</name>\n" +
			"<data>\n" +
			"<time>" + Long.toString(time) + "</time>\n" +
			"<clock offset>" + Long.toString(clockOffset) + "</clock offset>\n" +
			"</data>\n" +
			"</RXN_Response>\0";
			
			socket.send(xmlMessage.getBytes());
		}
		catch(NullPointerException npe)
		{
			Log.i("RXNetworksService.SendSNTPTime", "Nothing sent to client.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
