//package android.networking;
package android.supl;

//package com.android.server.supl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import android.util.Log;
import java.security.GeneralSecurityException;

/**
 * Provides SSL socket functionality.
 *
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public class CNetSSLSocketProvider extends CNetTLSProviderBase {

    //private static native boolean PostSLPMessageToQueue(byte[] mesg);

	private static final int SSL_CTIMEOUT = 10000; // in milli sec
    private static final int SSL_HTIMEOUT = 40000; // in milli sec
	private static final String TAG = "CNetSSLSocketProvider";
    private static byte PACKET_LENGTH_SIZE = 2;
    private SSLSocket sslSocket;
    private InputStream in;
    private OutputStream out;

    private boolean autoReceive = true;
    private ReceiveThread ReadListener = null;
	private ConnectThread ConnectListener = null;
    private boolean IsReset = false;

	private static final int CON_SUCCESS = 0x00;
	private static final int CON_FAILURE = 0x01;
	private static final int CON_SSL_ERR_RETRY = 0x02;
    private static String AutoFqdnStorePath = null;
    private static String FqdnPhoneStorePath = null;

    /**
     * Thread for receive action
     */
    private class ReceiveThread extends Thread {
        public void run() {
            ReadProc();
        }
    }

	    /**
     * Thread for Connect action
     */
    private class ConnectThread extends Thread {

		private String Host_port;
		ConnectThread(String Host_port) {
				 this.Host_port = Host_port;
			 }
        public void run() {
            ConnectProc(Host_port);
        }
    }

    /**
     * Default constructor
     * @param Context - SSLContext object. required
     *                  for working with SSL-related functions
     */
    public CNetSSLSocketProvider(SSLContext Context) {
        super(Context);
    }

    /**
     * Closes active SSL socket connection
     */
    private void Reset() {
        IsReset = true;
        //Log.e(TAG, " ===>>> Reset: Entering \n");

        InitialHandshakeComplete = false;
        try {
            if (ConnectListener != null) {
                if (ConnectListener.isAlive()) {
                    ConnectListener.interrupt();
                }
                ConnectListener = null;
            }

			if (ReadListener != null) {
                if (ReadListener.isAlive()) {
                    ReadListener.interrupt();
                }
                ReadListener = null;
            }
        }
        catch (SecurityException e) {
            Log.e(TAG, "Can't interrupt the listener task:" + e.getMessage());
        }

		 try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
			if (sslSocket != null) {
                sslSocket.close();
            }
        }
        catch (IOException e) {
            Log.e(TAG, "Reset() I/O Exception:" + e.getMessage());
        }
 }



    /**
     * Creates SSL socket connection.
     * @param Host_port - host IP address and port number
     * @return -1 if failed to connect to SLP server
     */
     public int ConnectProc(String Host_port) {

	 byte conResult;
        final int totalRetryTimes = 3;
        String dummy_host = "0.0.0.0:0";
        String hslp_addr = null;
        String spirent_addr = null;

        IsReset = false;
        TrustSecureManager tm;
        KeySecureManager km;
        SSLContext sslCtx = null;
        String cer_path = null;
        /* Connection with h-slp.mnc%03d.mcc%03d.pub.3gppnetwork.org */
        hslp_addr = Host_port.substring(0, 9);
        if (hslp_addr.equals("h-slp.mnc"))
        {
            cer_path = AutoFqdnStorePath;
        }

        Log.d(TAG, " ===>>> [GPS] spirent_addr = " + spirent_addr + " hslp_addr = " + hslp_addr);

        /* */
        if(cer_path == null)
        {
            Log.d(TAG, "[GPS] Using default certificate file");
            cer_path = FqdnPhoneStorePath;
        }

        cer_path.trim();
        try {
            sslCtx = null;

            tm = new TrustSecureManager();
            km = new KeySecureManager();
            tm.Init(cer_path, "123456");
            km.Init(cer_path, "123456");
            sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(km.getKetManagers(), tm.getKetManagers(), null);
        }
        catch (GeneralSecurityException e) {
            Log.d(TAG, "[GPS] ERR: GeneralSecurityException");
            e.printStackTrace();
            return -1;
        }
        catch (UnknownHostException e) {
            log("[GPS] ERR: CreateConnection(): Unknown host exception, " + e.getMessage());
            e.printStackTrace();
            Reset();
            return -1;
        }
        catch (IOException e) {
            log("[GPS] ERR: CreateConnection(): I/O Exception, " + e.getMessage());
            e.printStackTrace();
            Reset();
            return -1;
        }

        try{
            super.CreateConnection(Host_port);
            SSLSocketFactory ssf = sslCtx.getSocketFactory();
            sslSocket = (SSLSocket) ssf.createSocket();

            // Log.d("CNetSSLSocket", "[GPS] connect to IP=" + /*IP*/InetAddr.getHostName() + " Port=" + Port);
			sslSocket.connect(new InetSocketAddress(Host, Port), SSL_CTIMEOUT);


            sslSocket.setSoTimeout(SSL_CTIMEOUT); // in milli sec for handshake

            sslSocket.addHandshakeCompletedListener(
                new HandshakeCompletedListener() {
                        public void handshakeCompleted(HandshakeCompletedEvent event) {
                            Log.d(TAG, "**HANDSHAKE FINISHED**");
                            log("\t SessionId " + event.getSession());
                            log("\t PeerHost " + event.getSession().getPeerHost());
                        }
                }
            );

            msleep(300);
            Log.d(TAG, "**INITIAL HANDSHAKE BEGIN**");
            sslSocket.setSoTimeout(SSL_HTIMEOUT); // in milli sec
            Log.d("CNetSSLSocket", "[GPS] before startHandshake");
            sslSocket.startHandshake();
            Log.d("CNetSSLSocket", "[GPS] after startHandshake");

            msleep(500);
            in = sslSocket.getInputStream();
            out = sslSocket.getOutputStream();

           InitialHandshakeComplete = true;

		   conResult = CON_SUCCESS;


		   Log.d(TAG, "**CONNECTION SUCCESSFUL**");
		   ConnectionRespose(conResult);
		   ConnectListener = null;
		   Receive();

        }
        catch (UnknownHostException e) {

            Log.d(TAG,"CreateConnection(): Unknown host exception, " + e.getMessage());
			String fqdn = "www.spirent-lcs.com";

			if(Host.equals("www.spirent-lcs.com"))
			{
				Log.d(TAG,"CreateConnection(): Changing Host to 192.168.0.35 ");
				Host = "192.168.0.35";
				ConnectProc("192.168.0.35:7275");
			}
			else
			{
				Log.d(TAG,"CreateConnection(): Undefined hostHost " + Host);
				conResult=CON_FAILURE;
				ConnectListener = null;
				ConnectionRespose(conResult);
			}

            return -1;
        }
        catch (javax.net.ssl.SSLException e) {

            Log.d(TAG,"CreateConnection(): SSLException, " + e.getMessage());

			conResult=CON_SSL_ERR_RETRY;
			ConnectListener = null;
			ConnectionRespose(conResult);

            return -1;
        }
        catch (IOException e) {

            Log.d(TAG,"CreateConnection(): I/O Exception, " + e.getMessage());

			String fqdn = "www.spirent-lcs.com";

			if(Host.equals("www.spirent-lcs.com"))
			{
				Log.d(TAG,"CreateConnection(): Changing Host to 192.168.0.35 ");
				Host = "192.168.0.35";
				ConnectProc("192.168.0.35:7275");
			}
			else
			{
				Log.d(TAG,"CreateConnection(): I/O Exception,Undefined hostHost " + Host);
				conResult=CON_FAILURE;
				ConnectListener = null;
				ConnectionRespose(conResult);
			}

            return -1;
        }
        return 0;
    }

    /**
     * Sends data through active connection.
     * @param b - data to transmit
     * @param off - offset
     * @param len - number of bytes to transmit
     * @return result of sending: 0 if operation was sucessful
     *                            else - -1.
     */
    public int Write(byte[] b, int off, int len) {
        try {
            out.write(b, off, len);
            out.flush();

             //Log.e(TAG, " ===>>> Write: Exiting Successfully \n");
            return 0;
        }
        catch (IOException e) {
            Reset();
            Log.e(TAG, "Write() I/O Exception" + e.getMessage());
            return -1;
        }
		catch (NullPointerException e)
		{
            Reset();
            Log.e(TAG, "Write() NullPointerException" + e.getMessage());
            return -1;
		}
    }

    /**
     * Reads data from active connection.
     * @param b - byte array where to put data
     * @param off - offset
     * @param len - number of bytes that should be received
     * @return result of reading: number of recieved bytes if operation
     *         was sucessful;
     *         -2 if IOExeption occured;
     *         -1 in other cases.
     */
    public int Read(byte[] b, int off, int len) {
        if (autoReceive || off < 0 || len < 0) {
            return -1;
        }
        try {
            int read = 0;
            while (len - read > 0) {
                read += in.read(b, off + read, len - read);
            }
            return len;
        } catch (IOException e) {
            Reset();
            Log.e(TAG, "Read () I/O Exception" + e.getMessage());
            return -2;
        }
    }

    /**
     * Reads SUPL message.
     * @return message read from socket
     * @throws IOException - if read from socket fails
     */
    private byte [] ReadMessage () throws IOException  {
        byte [] firstTwoBytes = new byte[PACKET_LENGTH_SIZE];
        byte [] message = null;
        int messageSize = 0;
        int read = 0;

        //Log.e(TAG, "===>>> ReadMessage: Entering \n");

        while (PACKET_LENGTH_SIZE - read > 0) {
            //Log.e(TAG, "===>>> ReadMessage: Blocked on read \n");

			try
            {
            read += in.read(firstTwoBytes, 0 + read,
                            PACKET_LENGTH_SIZE - read);
            }
			catch (IndexOutOfBoundsException e)
			{
				Log.e(TAG, "===>>> ReadMessage: IndexOutOfBoundsException 1 \n");
				return null;

			}
			catch (IOException e)
			{
				Log.e(TAG, "===>>> ReadMessage: IOException 1 \n");
				return null;
			}
        }

        //Log.e(TAG, "===>>> ReadMessage: Outside while 1 \n");

        messageSize = (int) (((int)firstTwoBytes[0]) << 8) & 0x0000FF00;
        messageSize |= ((int) firstTwoBytes[1] & 0x000000FF);
        log("ReadMessage (): SLP server message length = " + messageSize);

        message = new byte[messageSize];
        message[0] = firstTwoBytes[0];
        message[1] = firstTwoBytes[1];
        read = 0;


        while (messageSize - PACKET_LENGTH_SIZE - read > 0) {
            //Log.e(TAG, "===>>> ReadMessage: Blocked on read 2222 \n");
        try
        {
            read += in.read(message, PACKET_LENGTH_SIZE + read,
                            messageSize - PACKET_LENGTH_SIZE - read);
        }
		catch (IndexOutOfBoundsException e)
    	{
			Log.e(TAG, "===>>> ReadMessage: IndexOutOfBoundsException 2 \n");
			return null;
		}
		catch (IOException e)
		{
			Log.e(TAG, "===>>> ReadMessage: IOException 2 \n");
			return null;
		}

        }
        //Log.e(TAG, "===>>> ReadMessage: Outside while 2 \n");
        return message;
    }

    /**
     * Closes active connection.
     */
    @Override
    public void FreeConnection() {
        log("<FreeConnection>");
	/*
        if (InitialHandshakeComplete == false){
            log("InitialHandshakeComplete == false");
            return;
        }
        */
        Reset(); //no more I/O operations
    }

    /**
     * Initializes CNetSSLSocketProvider instance.
     */
    @Override
    public int Init() {
        return 0;
    }

    /**
     * Retrieves connection status.
     * @return 0 - if CNetSSLSocketProvider doesn't have active connections
     *         1 - otherwise
     */
    @Override
    public int IsActive() {
        if (InitialHandshakeComplete) {
            return 1;
        }
        return 0;
    }

    /**
     * Main read procedure
     */
    private void ReadProc() {
        try {
            int num = 0;

            log(" ===>>> ReadProc: Entering with InitialHandshakeComplete = %d \n" +InitialHandshakeComplete);

            while(InitialHandshakeComplete) {
                byte [] mess = ReadMessage ();
                if (mess == null) {
                    log("error ReadMessage message, ReadProc() exit! ");
                    Reset();
                    return;
                }
                else {
                    PostSLPMessageToQueue(mess);
                    //DumpMessage(mess);



                }
            }
        }
        catch (IOException e) {
            Log.e(TAG, "ReadProc(): I/O Exception, " + e.getMessage());
            //Reset();
        }
    }

    /**
     * Creates ReceiveListener thread.
     * @return always 0
     */
    @Override
    public int Receive() {
        if (ReadListener == null) {
            ReadListener = new ReceiveThread ();
            ReadListener.start();
        }
        return 0;
    }


    /**
     * Creates Connection thread.
     * @return always 0
     */
    @Override
    public int CreateConnection(String Host_port) {
        if (ConnectListener == null) {
            ConnectListener = new ConnectThread (Host_port);
            ConnectListener.start();
        }
        return 0;
    }



    /**
     * Resets active connection.
     */
    @Override
    public void ResetConnection() {
    }

    /**
     * Sends byte array.
     * @param data - data to transmit
     * @return result of sending: 0 if operation was sucessful
     *         else - -1.
     */
    @Override
    public int Send(byte[] data) {

        int cnt = 0;
        //DumpMessage(data);
		/*
        Log.e(TAG,"====>>>CNETSSLSOCKET:: Send:  <<<===");


        for( cnt = 0; cnt < data.length; cnt++)
        {
            Log.e(TAG," Send Byte[%d]" + cnt);
            Log.e(TAG,":%x\n" + data[cnt]);
        }
       */
               return Write(data, 0, data.length);
    }

    /**
     * Prints messages to Android log.
     */
    private void log(String str) {
        if (logging) {
               //Log.d(TAG, str);
           }
    }
    public void msleep(int millisecond)
    {
        try {
            Thread.sleep(millisecond);
        }catch(InterruptedException e){}
    }
    /**
     * Set type of crptographic protocol.
     * @param I - Type of protocol (TLS/Non_TLS...)
     */
    public static void SetAutoFqdnStorePath(String autoFqdnStorePath) {
        AutoFqdnStorePath = autoFqdnStorePath;
    }

    /**
     * Set type of crptographic protocol.
     * @param I - Type of protocol (TLS/Non_TLS...)
     */
    public static void SetFqdnPhoneStorePath(String fqdnPhoneStorePath) {
        FqdnPhoneStorePath = fqdnPhoneStorePath;
    }
}
