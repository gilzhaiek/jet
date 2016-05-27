package com.rxnetworks.rxnserviceslib;

import android.util.Log;

public class SendDataAccessStatus 
{
	private SocketServer socket;
	public SendDataAccessStatus(SocketServer socket)
	{
		this.socket = socket;
	}
	public void xmlSocketSend(Boolean enabled)
	{
		String xmlMessage;
		int status;
		
		if(enabled == true)
		{
			status = 1;
		}
		else
		{
			status = 0;
		}
		try
		{
			xmlMessage = "<RXN_Response>\n" +
			"<name>Data_Access_Enabled</name>\n" +
			"<data>\n" +
				status +
			"</data>\n" +
			"</RXN_Response>\0";
			
			socket.send(xmlMessage.getBytes());
			Log.i("SendDataAccessStatus", "Data_Access_Enabled status: " + status);
		}
		catch(NullPointerException npe)
		{
			Log.i("SendDataAccessStatus", "Nothing sent to client.");
		}
		catch(Exception e)
		{	
			e.printStackTrace();
			Log.i("SendDataAccessStatus", "Data_Access_Enabled message failed.");
		}
	}
}
