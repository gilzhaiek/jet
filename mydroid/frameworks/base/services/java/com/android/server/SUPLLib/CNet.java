package android.supl;
//package com.android.server.supl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import android.util.Log;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

/**
 * CNet class.
 * This class enables secure communications using protocols 
 * such as the Secure Sockets Layer (SSL) or 
 * "Transport Layer Security" (TLS) protocols.
 *
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public class CNet {
    //private static native boolean nativeEnable();
    //private static native boolean nativePostSlpMessage();

    public static final String TLS_PROTOCOL = "TLS";
    public static final String NON_TLS_CONNECTION = "Non_TLS";
    public static final String ENGINE_IMPLEMENTATION = "Engine";
    public static final String SOCKET_IMPLEMENTATION = "Socket";
    public static final String TAG = "CNET";
    
    private static TrustSecureManager tm = null;
    private static KeySecureManager km = null;
    private static CNetTLSProviderBase Net = null;
    private static String Path = null;
    private static String Impl = SOCKET_IMPLEMENTATION;
    private static String ProtocolType = NON_TLS_CONNECTION;
    private static String Pass = "123456";
    private static String localhost_port = "127.0.0.1:7275";
    private static CNet network = null;
    private static Thread Wait = null;

    /**
     * Localhost flag: is true if connection with localhost is used
     */
    public  static boolean localhost = false;

    /**
     * IP address of remote SLP server.
     */
//    public  static String slphost_port = "208.8.164.3:7275";
    public  static String slphost_port = "208.8.164.7:7275";
    /**
     * Unit test thread.
     */
    private static class WaitForCloseThread extends Thread {
        public void run() {
            Log("Waiting for close JVM");
            StartNativeUnitTest();
        }
    }
    
    /**
     * Set path to certificate storage.
     * @param p - path to certificate storage
     */
    public static void SetPath(String P) {
        Path = P;
    }
    
    /**
     * Set type of CNet connection.
     * @param I - type of CNet connection (Socket/Engine)
     */
    public static void SetImpl(String I) {
        Impl = I;
    }

    /**
     * Set type of crptographic protocol.
     * @param I - Type of protocol (TLS/Non_TLS...)
     */
    public static void SetProtocolType(String protocol) {
        ProtocolType = protocol;
    }

    /**
     * Private CNet constructor.
     */
 //   private CNet()
    public CNet()
    {
	Log("===>> Inside CNet Constructor <<===");
        km = new KeySecureManager();
        tm = new TrustSecureManager();
        Log("Net = " + Net);

    } 

    /**
     * Private CNet constructor.
     */
    public CNet(String StorePath) {
        km = new KeySecureManager();
        tm = new TrustSecureManager();
    }
    
    /**
     * Print providers information to log.
     * FIXME: remove this function in final release
     */
    private void PrintAllProviders() {
        int i= 0;
        SSLContext [] sslCtx = null;
        Provider [] sysProviders = Security.getProviders();
        
        SSLEngine engine = null;
        SSLSocketFactory ssf = null;
        String [] cipherSuites = null;
        
        sslCtx = new SSLContext[sysProviders.length];
        
        Log("Providers List:");
        for (i = 0; i < sysProviders.length; i++) {
            Log("Provider: " + i);
            Log("    name: " + sysProviders[i].getName());
            Log("    info: " + sysProviders[i].getInfo());
            Log("    vers: " + sysProviders[i].getVersion());
            Log("    class: " + sysProviders[i].getClass().getName());
            
            try {
                sslCtx[i] = SSLContext.getInstance(TLS_PROTOCOL, sysProviders[i]);
                sslCtx[i].init(km.getKetManagers(), tm.getKetManagers(), null);
                
                ssf = sslCtx[i].getSocketFactory();
                engine = sslCtx[i].createSSLEngine("208.8.164.3", 7275);
                engine.setUseClientMode(true);
                
                Log("SocketFactory supported suites:");
                cipherSuites = ssf.getSupportedCipherSuites();
                for (i = 0; i < cipherSuites.length; i ++) {
                    Log ("Suite " + i + ":" + cipherSuites[i]);
                }

                Log("SSLEngine supported suites:");
                cipherSuites = engine.getSupportedCipherSuites();
                for (i = 0; i < cipherSuites.length; i ++) {
                    Log ("Suite " + i + ":" + cipherSuites[i]);
                }
            }
            catch (NoSuchAlgorithmException e) {
                Log("Algorithm is not found - " + e.getMessage() + "  :" + i );
            }
            catch (GeneralSecurityException e) {
                Log("GeneralSecurityException  " + e.getMessage() + "  :" + i );
            }
        }
    }
    
    /**
     * Initialize CNet instance.
     */
    public int Init(String KeyStorePath,
                    String Password,
                    String Implementation,
                    String protocol)
    {
	Log("===>> Inside Init <<===");
	 if (Net != null) {
            Log("Network is already created.");
            return -1;
        }
        
        Log("Creating new Net");

        if (Implementation == null) {
            Implementation = SOCKET_IMPLEMENTATION;
            Log("Defaulting to Implementation SOCKET_IMPLEMENTATION");
        }

		if (protocol == null)
		{
				protocol = NON_TLS_CONNECTION;
				Log("Defaulting to protocol NON_TLS_CONNECTION");
        }
        

        try {
            if ( protocol.equals(TLS_PROTOCOL) )
            {
				Log("ProtocolType TLS_PROTOCOL");
                SSLContext sslCtx;
                // Only for TLS connection.
                tm.Init(KeyStorePath, Password);
                km.Init(KeyStorePath, Password);
                sslCtx = SSLContext.getInstance(TLS_PROTOCOL);
                sslCtx.init(km.getKetManagers(), tm.getKetManagers(), null);
                PrintAllProviders();

                if ( Implementation.equals(SOCKET_IMPLEMENTATION) )
                {
                    Net = new CNetSSLSocketProvider(sslCtx);
                    Log("CNetSSLSocketProvider created!");
                }
                else
                {
                    // For every other case...
                Net = new CNetSSLEngineProvider(sslCtx);
                Log("CNetSSLEngineProvider created!");
                }
            }
            else if ( Implementation.equals(SOCKET_IMPLEMENTATION) && protocol.equals(NON_TLS_CONNECTION) )
            {
				Log("ProtocolType NON_TLS_CONNECTION and Implementation SOCKET_IMPLEMENTATION");
                Net = new CNetSocketProvider();
                Log("CNetSocketProvider created!");
            }
			else
			{
            	Log("Defaulting to Implementation SOCKET_IMPLEMENTATION and ProtocolType NON_TLS_CONNECTION");
				Implementation = SOCKET_IMPLEMENTATION;
				protocol = NON_TLS_CONNECTION;
                Net = new CNetSocketProvider();
			}

            Log("network.Init(), Net = " + Net);
            Net.Init();
            return 0;
        } catch (IOException e) {
            Log.e(TAG, "CNet->Init(): I/O Exception " + e.getMessage());
            e.printStackTrace();
            tm = null;
            km = null;
            return -1;
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "CNet->Init(): General security exception " + 
                  e.getMessage());
            tm = null;
            km = null;
            return -1;
        } catch (NullPointerException e) {
            Log.e(TAG, "CNet->Init(): NullPointerException!!" + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Start Native unit tests.
     * FIXME: remove that function in final release
     */
    public static void nativeUnitTest() {
        StartNativeUnitTest();
    }

    /**
     * Unit test function.
     * FIXME: remove that function in final release
     */
    public static void UnitTestWithLocalHost()
    {
        byte [] message = {0x00, 0x1b, 0x1, 0x0, 0x0, 
                           (byte)0x84, (byte)0xe2, 0xe, 0x2, 0x44, 
                           (byte)0x8d, 0x15, (byte)0x9e, 0x24, 0x7, 
                           (byte)0xfc, 0x41, 0x4, 0x40, 0xf, 
                           (byte)0xa0, 0xe, 0x50, 0x1c, 0x22, 
                           0x1c, (byte)0x80};
        
        String host_port = "127.0.0.1:7275";
        
        if (Init() < 0) {
            Log("UnitTest ():Unable to init network!");
            return;
        }
        Log("CreateConnection" + host_port);
        if (CreateConnection(host_port)  < 0) {
            Log("UnitTest ():Unable to create network connection!");
            return;
        }
        if (IsActive() == 0) {
            Log("UnitTest ():Connection is not active!");
            return ;
        }
        else {
            Log("UnitTest ():Connection active!");
        }
        if (Receive() < 0) {
            Log("UnitTest ():Error Receive() call!");
            FreeConnection();
            return;
        }
        
        Log("UnitTest ():FreeConnection().");
        FreeConnection();
    }

    /**
     * Unit test function.
     * FIXME: remove that function in final release
     */
    public static void UnitTest() {
        byte [] message = {0x00, 0x1b, 0x1, 0x0, 0x0, 
                           (byte)0x84, (byte)0xe2, 0xe, 0x2, 0x44, 
                           (byte)0x8d, 0x15, (byte)0x9e, 0x24, 0x7, 
                           (byte)0xfc, 0x41, 0x4, 0x40, 0xf, 
                           (byte)0xa0, 0xe, 0x50, 0x1c, 0x22, 
                           0x1c, (byte)0x80};
        
        String host_port = "208.8.164.7:7275";
        
        if (Init() < 0) {
            Log("UnitTest ():Unable to init network!");
            return;
        }
        Log("CreateConnection" + host_port);
        if (CreateConnection(host_port)  < 0) {
            Log("UnitTest ():Unable to create network connection!");
            return;
        }
        if (IsActive() == 0) {
            Log("UnitTest ():Connection is not active!");
            return ;
        }
        else{ 
            Log("UnitTest ():Connection active!");
        }
        if (Receive() < 0) {
            Log("UnitTest ():Error Receive() call!");
            FreeConnection();
            return;
        }
        if (Send(message) < 0) {
            Log("UnitTest ():Error send request to SLP server!");
            FreeConnection() ;
            return;
        }
        else {
            Log("UnitTest ():Waiting 10 secs for SLP server response!");
        }
        
        synchronized(network) {
            try {
                network.wait(10000);
            } 
            catch (InterruptedException e) {
                Log("UnitTest (): Interrupted exception" + e.getMessage());
            }
        }
        Log("UnitTest ():FreeConnection().");
        FreeConnection() ;
        return;
    }
    
    /**
     * Create and initialize CNet instance.
     * @return 0 - if init was sucessful, else negative error code
     */
    public static int Init() {
        Log(" ===>>> Init: Entering \n");  
        if (network == null) {
            network = new CNet ();
        }
        else {
            Log("network is already initialized");
            return 0;
        }
        int res = network.Init(Path, Pass, Impl, ProtocolType);
        if (res < 0) {
            Log("CNet->Init(): Error init network.");
        }
        return res;
    }
    
    public static int ReInit() {
        if (network == null) {
	    return Init();
	}
    
        if (IsActive() != 0) {
	    FreeConnection();
	    Net = null;
	    int res = network.Init(Path, Pass, Impl, ProtocolType);
            if (res < 0) {
                Log("ReInit() error: " + res);
	    }
	    
	    return res;
        }
	else {
	    Net = null;
	    int res = network.Init(Path, Pass, Impl, ProtocolType);
            if (res < 0) {
                Log("ReInit() error: " + res);
	    }
	    
	    return res;
	}
    }
    
    /**
     * Create connection.
     * @return result: -1 if CNet failed to connect to SLP server
     */
    public static int CreateConnection(String Host_port) {
        //String Host_port;
        Log(" ===>>> CreateConnection: Entering Host_port =["+ Host_port + "]");

        //Log(" ===>>> CreateConnection: Calling nativePostSlpMessage() \n");
        //nativePostSlpMessage();


        if (Net != null) {
			Log(" CreateConnection: not null \n");
            try {
                if (localhost == true) {
					Log(" CreateConnection: Host_port = localhost_port \n");
                    Host_port = localhost_port;
                }
                else {
					if (Host_port.length() == 0)
					{										
					Log(" CreateConnection: Host_port = NULL \n");
                    Host_port = slphost_port;
					}
                }
				Log(" CreateConnection: Host_port=[" + Host_port + "]");
				Log(" CreateConnection: Net.CreateConnection \n");
                return Net.CreateConnection(Host_port);
            } 
            catch (UnknownHostException e) {
                Log.e(TAG, e.getMessage());
                return -1;
            }
        }
        else {
            Log(" CreateConnection:Net == null");
            return -1;
        }
    }
    
    /**
     * Closes active connection.
     */
    public static void FreeConnection() {
        if (Net != null){
            Net.FreeConnection();
        }
        else {
            Log("Net == null");
        }
    }
    
    /**
     * Returns connection status.
     * @return 0 if CNet doesn't have active connections
     *         else 1
     */
     public static int IsActive() {
        if (Net != null) {
            Log("Net IsActive");
            return Net.IsActive();
        }
        else {
            return 0;
        }
    }

    /**
     * Starts data receiving thread of underlying SSL provider implementation.
     * @return 0 if Receive operation was sucessful else -1
     */
    public static int Receive() {
        if (Net != null){
            Log("Net Receive");
            return Net.Receive();
        }
        else {
            return -1;
        }
    }

    /**
     * Resets active connection.
     */
    public static void ResetConnection() {
        if (Net != null){
            Net.ResetConnection();
        }
    }

    /**
     * Sends byte array.
     * @param data - data to transmit
     * @return result of send operation: 0 if operation was sucessful
     *                                   else -1
     */
    public static int Send(byte[] data) {
/*        byte [] message = {0x00, 0x1b, 0x1, 0x0, 0x0,
                           (byte)0x80, (byte)0xc2, (byte)0x4e, (byte)0xae, (byte)0xae,
                           (byte)0xae, (byte)0xae, (byte)0xae, (byte)0xaf, (byte)0xff,
                           (byte)0xfc, 0x41, 0x2, 0x40, 0xf,
                           (byte)0xa0, 0xe, 0x50, 0x1c, 0x22,
                           0x1c, (byte)0x80};


        byte [] message = {0x00, 0x1b, 0x1, 0x0, 0x0,
                           (byte)0x84, (byte)0xe2, 0xe, 0x2, 0x44,
                           (byte)0x8d, 0x15, (byte)0x9e, 0x24, 0x7,
                           (byte)0xfc, 0x41, 0x4, 0x40, 0xf,
                           (byte)0xa0, 0xe, 0x50, 0x1c, 0x22,
                           0x1c, (byte)0x80};
*/






        if (Net != null) {
            Log("Net Send");
            return Net.Send(data);
            //return Net.Send(message);
        }
        else {
            return -1;
        }
    }
    
        
    /**
     * Prints debug message to Android log.
     */
    private static void Log(String Message) {
        Log.d("CNet", Message);
    }
    
    /**
     * Calls unit test function from native library.
     * FIXME: remove that function in final release
     */
    private static native void StartNativeUnitTest();
    
    /** 
     * Loads native library.
     */
//    static {
//        System.loadLibrary("supllocationprovider");
//   }
}
