package com.rxnetworks.rxnserviceslib;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.net.Credentials;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class SocketServer 
{
	private final Thread socketListenerThread;

	private final RXNetworksService mService;
	private LocalSocket mSocket;

	private InputStream mInputStream;
	private OutputStream mOutputStream;

	private static final String TAG = "RXNServices.SocketServer";
	private static final String DOMAIN_PATH = "RXN_MSL_PGPS";
	private static final int MSL_uid = -1;
	private boolean dataAccessEnabled = false;    
	
	private boolean connected = false;
	private boolean mExitNow = false;

	public SocketServer(RXNetworksService service) throws IOException 
	{	
		mService = service;

        socketListenerThread = 
        	new Thread()
        	{
        		public void run()
        		{
        			while (!mExitNow)
        			{
	        			try
	        			{
	        				if (!connected)
	        					Connect();	        				
	        			}
	        		    catch(IOException e) 
	        		    {
	    					Log.e(TAG, e.toString());
	        		    }
	        			
	        			try {
							sleep(10 * 1000);
						} catch (InterruptedException e) {
							Log.e(TAG, e.toString());
						}
        			}
        			Log.d(TAG, "SocketServer exiting");
        		}
        	};
	}
	
	public void start()
	{
		socketListenerThread.start();
	}
	public void stop()
	{
		mExitNow = true;
		try {
			mSocket.close();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
	}
	public void setDataAccessEnabled(boolean dataAccessEnabled)
	{
		this.dataAccessEnabled = dataAccessEnabled;
		if (mSocket != null && mSocket.isConnected())
			sendDataAccessEnabled();
	}

	private void sendDataAccessEnabled()
	{
		if (mSocket != null)
		{
			Log.d(TAG, "Sending data access status");
			SendDataAccessStatus dataAccess = new SendDataAccessStatus(this);
			dataAccess.xmlSocketSend(dataAccessEnabled);
		}
	}

	private boolean authenticate() throws IOException
	{
		// If authentication not used, return true
		if (MSL_uid == -1)
		{
			return true;
		}

		// Check that the uid of the incoming connection matches what we expect
		Credentials c = mSocket.getPeerCredentials();
		int uid = c.getUid();

		if (uid != MSL_uid)
		{
			Log.e(TAG, "Authentication failed (uid = " + uid + "). Disconnecting");
			return false;
		}
		else
		{
			Log.i(TAG, "Authenticated");
			return true;
		}
	}

	private void Connect() throws IOException
	{
		Log.d(TAG, "Trying to connect to MSL");
		mSocket = new LocalSocket();
		mSocket.connect(new LocalSocketAddress(DOMAIN_PATH));
		
		Log.d(TAG, "Socket connected");
		Log.i(TAG, "RX Networks Services " + RXNetworksService.VERSION);

		if (!authenticate())
		{
			mSocket.close();
		}
		else
		{
			try {
				mOutputStream = mSocket.getOutputStream();
				mInputStream = new BufferedInputStream(mSocket.getInputStream(), 512);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}

			// Send data access enabled status
			sendDataAccessEnabled();

			try {
				inputLoop();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}
	
	private void inputLoop() throws IOException
	{
		while (true)
		{
			Log.d(TAG, "Waiting for input");

			byte[] data = new byte[512];
			int size = 0;
			int pos = 0;

			// The MSL sends 512 byte messages, so loop through until we read all 512 bytes
			while ((size = mInputStream.read(data, pos, 512 - pos)) > 0 && pos < 512)
			{
				pos += size;
			}

			Log.d(TAG, "Read " + pos + " bytes from MSL");

			// No bytes to read means the other side has disconnected
			if (pos <= 0)
				break;

			Log.d(TAG, "About to process request");	        	
			processRequest(new ByteArrayInputStream(data));
		}
		Log.e(TAG, "MSL socket no longer connected");
	}

	private void processRequest(InputStream inputStream)
	{
		try
		{
			/* Get a SAXParser from the SAXPArserFactory. */
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			/* Get the XMLReader of the SAXParser we created. */
			XMLReader xr = sp.getXMLReader();
			RXNMessageHandler msgHandler = new RXNMessageHandler(mService, this);
			xr.setContentHandler(msgHandler);

			InputSource input = new InputSource(inputStream);
			input.setEncoding("US-ASCII");
			xr.parse(input);	    		    
		}
		catch(Throwable t)
		{
			//Log.e(TAG, "processRequest failed: " + t.getMessage());
		}

		Log.d(TAG, "Done processing request");
	}

	public void send(byte[] data)
	{
		Log.d(TAG, "Sending data");
		try {
			mOutputStream.write(data);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		Log.d(TAG, "Done sending data");
	}
}
