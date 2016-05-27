package com.rxnetworks.rxnserviceslib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class PGPSSeedHTTPRequest {
	private URL grnServerURL;
	public int contentLength = 0;
	private static final int READ_CHUNK_SIZE = 512;		
	private static final int CONNECT_TIMEOUT = 10 * 1000;		// connect timeout in milliseconds
	
	//private static final String TAG = "RXNetworksService.PGPSSeedHTTPRequest";
	
	public PGPSSeedHTTPRequest(String host, int port, String request)
	{
		/* Build URL String */
		StringBuilder urlBuilder = new StringBuilder("http://");
		urlBuilder.append(host).append(":").append(port).append("/").append(request);
		
		try
		{
			grnServerURL = new URL(urlBuilder.toString());
		}
		catch (MalformedURLException mue) 
		{
			//Log.e(TAG, "RXNServices.PGPSSeedHTTPRequest: Error in constructing URL: " + mue.getMessage());
		}
		//Log.v(TAG, "RXNServices.PGPSSeedHTTPRequest:Constructed URL " + grnServerURL);		
	}
	
	public synchronized void getSeedData(OutputStream outputStream) 
	{
		HttpURLConnection conn = null;
		contentLength = 0;
				
		try 
		{
			System.setProperty("http.keepAlive", "false");
			conn = (HttpURLConnection) grnServerURL.openConnection();
			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.connect();

			InputStream in = conn.getInputStream();
			//Log.d(TAG, "Response Code is " + conn.getResponseCode());
			//Log.v(TAG, "Response Message is" + conn.getResponseMessage());
				
            byte[] data = new byte[READ_CHUNK_SIZE];
            int size = 0;
            while ((size = in.read(data, 0, READ_CHUNK_SIZE)) > 0)
				{
            	outputStream.write(data, 0, size);
			}
            in.close();
		}
		catch (UnknownHostException uhe)
		{
			//Log.e(TAG, "UnknownHostException in getSeedData(): " + uhe.getMessage());
		}
		catch (IOException ioe)		// This also handles SocketTimeoutException 
		{
			//Log.e(TAG, "IOException in getSeedData(): " + ioe.getMessage());
		}
		finally 
		{
			conn.disconnect();
		}
	}	
}
