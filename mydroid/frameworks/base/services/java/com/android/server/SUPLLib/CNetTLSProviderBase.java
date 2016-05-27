//package android.networking;

package android.supl;

//package com.android.server.supl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.net.ssl.SSLContext;

/**
 * Abstract class that contains interface for
 * working with CNet providers.
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public abstract class CNetTLSProviderBase {
    protected SSLContext sslContext;
    protected InetAddress InetAddr;
	protected String Host;
    protected int Port;
    /** 
    * Contains handhake operation status
    * true - secure connection is active
    * false otherwise
    */
    protected boolean InitialHandshakeComplete = false; 
    protected boolean logging = true;

    /**
     * CNetTLSProviderBase constructor.
     * @param Context - SSLContext
     */
    public CNetTLSProviderBase(SSLContext Context) {
        sslContext = Context;
    }
    
    /**
     * Creates SSL connection.
     * @param Host_port host IP address
     * @throw UnknownHostException - if host address resolution fails.
     */
    public int CreateConnection(String Host_port) 
                                throws UnknownHostException {
        int Index = Host_port.indexOf(':', 0);
        Host =  Host_port.substring(0, Index);
        String port =  Host_port.substring(Index + 1);
        Port = Integer.parseInt(port);
        //InetAddr = InetAddress.getByName(Host);
        return 0;
    }
    
    /**
     * Initializes instance of provider implementation.
     */
    public abstract int Init();
    /**
     * Closes active connection.
     */
    public abstract void FreeConnection();
    /**
     * Resets active connection.
     */
    public abstract void ResetConnection();
    /**
     * Starts data receiving thread of underlying SSL provider implementation.
     * @return 0 if Receive operation was sucessful, else -  -1
     */
    public abstract int Receive();
    /**
     * Sends byte array.
     * @param data - data to transmit
     * @return result of send operation: 0 if operation was sucessful,
     *                                   else - -1
     */
    public abstract int Send(byte[] data);
    /**
     * Returns connection status.
     * @return 0 if there are no active connections,
     *         else - 1
     */
    public abstract int IsActive();
    
    /**
     * Function is implemented in native code. Posts SLP messages to 
     * SUPL Client internal queue
     * @param mess - message to be posted
     * @return result of post operation: true if operation was sucessful,
     *                                   else - false
     */
    protected native boolean PostSLPMessageToQueue(byte[] mess);

	    /**
     * Function is implemented in native code. Posts Connection response to SUPL Clinet 
     * internal queue
     * @param mess - message to be posted
     * @return result of post operation: true if operation was sucessful,
     *                                   else - false
     */
    protected native boolean ConnectionRespose(byte bRes);

		
    /**
     * Function is implemented in native code. Stores message to file. 
     * File name is generated automatically.
     * @param mess - message to be stored
     * @return result of operation: true if operation was sucessful,
     *                              else - false
     */
    protected native boolean DumpMessage(byte[] mess);
    /**
    * load library where PostSLPMessageToQueue and DumpMessage functions
    * are implemented
    */
//    static {
//        System.loadLibrary("supllocationprovider");
//    }
}
