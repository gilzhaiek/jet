package com.rxnetworks.rxnserviceslib;

import java.io.*;

import android.util.Log;

public class SendPGPSSeed 
{
    private final SocketServer socket;
    private final ByteArrayOutputStream xmlMessage;
    
    private static final String xmlMessageStart = "<RXN_Response>\n" +
            "<name>PGPS_Seed</name>\n" +
            "<data>\n" +
            "<seed><![CDATA[";

    private static final String TAG = "RXNetworksService.SendPGPSSeed";

	public SendPGPSSeed(SocketServer socket)
	{
		this.socket = socket;
        xmlMessage = new ByteArrayOutputStream(4609);
        
        try {
            xmlMessage.write(xmlMessageStart.getBytes("US-ASCII"));
        } catch (Exception e) {
            Log.d(TAG, e.toString());
	}
    }
    
    public OutputStream getOutputStream()
    {
        return xmlMessage;
    }
    
    public void xmlSocketSend()
    {
        if (xmlMessage.size() <= xmlMessageStart.length())
	{
            Log.d(TAG, "No message to send.");
            return;
        }
        
		try
		{
            String xmlMessageEnd = "]]></seed>\n" + 
			"</data>\n" +
			"</RXN_Response>"+(char)26;
			
			xmlMessage.write(xmlMessageEnd.getBytes("US-ASCII"));
			socket.send(xmlMessage.toByteArray());
		}
		catch(NullPointerException npe)
		{
            Log.i(TAG, "Nothing sent to client.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
}
