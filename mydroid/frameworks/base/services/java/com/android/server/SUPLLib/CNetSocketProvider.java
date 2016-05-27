package android.supl;


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
import javax.net.SocketFactory;
import android.util.Log;

/**
 * Provides Socket functionality.
 *
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public class CNetSocketProvider extends CNetTLSProviderBase {

	private static final int SSL_CTIMEOUT = 10000; // in milli sec
	private static final int SSL_HTIMEOUT = 20000; // in milli sec
    private static final String TAG = "CNetSocketProvider";
    private static byte PACKET_LENGTH_SIZE = 2;
    private Socket nonSecureSocket;
    private InputStream in;
    private OutputStream out;

    private boolean autoReceive = true;
    private ReceiveThread ReadListener = null;
	private ConnectThread ConnectListener = null;

    private boolean isConnectionActive = false;


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
     * @param
     */
    public CNetSocketProvider() {
        super(null);
    }

    /**
     * Closes active socket connection
     */
    private void Reset() {
        try {
            if (nonSecureSocket != null) {
                nonSecureSocket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }

            isConnectionActive = false;
			log("Successfully resetting connection");
        }
        catch (IOException e) {
            Log.e(TAG, "Reset() I/O Exception:" + e.getMessage());
        }

        try {
            if (ReadListener != null) {
                if (ReadListener.isAlive()) {
                    ReadListener.interrupt();
                }
                ReadListener = null;
            }

			if (ConnectListener != null) {
                if (ConnectListener.isAlive()) {
                    ConnectListener.interrupt();
                }
                ConnectListener = null;
            }
        }
        catch (SecurityException e) {
            Log.e(TAG, "Can't interrupt the listener task:" + e.getMessage());
        }
    }

    /**
     * Creates Socket connection.
     * @param Host_port - host IP address and port number
     * @return -1 if failed to connect to SLP server
     */
 
    public int ConnectProc(String Host_port) {
     byte conResult;
        try {
            super.CreateConnection(Host_port);
            log("CNetSocketProvider :: CreateConnection :: Entering");

            SocketFactory sf = SocketFactory.getDefault();
            nonSecureSocket = (Socket) sf.createSocket();
            nonSecureSocket.connect(new InetSocketAddress(Host,
                                    Port),SSL_CTIMEOUT);

            in = nonSecureSocket.getInputStream();
            out = nonSecureSocket.getOutputStream();

            isConnectionActive = true;
        }
        catch (UnknownHostException e) {
            Reset();
            log("CreateConnection(): Unknown host exception, " + e.getMessage());
			conResult=0x01;
			ConnectListener = null;
			ConnectionRespose(conResult);
            return -1;
        }
        catch (IOException e) {
            Reset();
            log("CreateConnection(): I/O Exception, " + e.getMessage());
			conResult=0x01;
			ConnectListener = null;
			ConnectionRespose(conResult);
            return -1;
        }
			   
		   conResult =0x00; 		   
		   Log.d(TAG, "**CONNECTION SUCCESSFUL**");
		   ConnectionRespose(conResult);
		    Receive();
        log("CNetSocketProvider :: CreateConnection :: Exiting Successfully. ");
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
        log("CNetSocketProvider :: Write to Socket");

        try {
            out.write(b, off, len);
            out.flush();

             Log.e(TAG, " ===>>> Write: Exiting Successfully \n");
            return 0;
        }
        catch (IOException e) {
            Log.e(TAG, "ERR: Write() I/O Exception" + e.getMessage());
            Reset();
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
        log("CNetSocketProvider :: Read to Socket");

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
    private byte [] ReadMessage () throws IOException, ArrayIndexOutOfBoundsException
	{
        byte [] firstTwoBytes = new byte[PACKET_LENGTH_SIZE];
        byte [] message = null;
        int messageSize = 0;
        int read = 0;
		int bytes_read = 0;

        Log.e(TAG, "===>>> ReadMessage: Entering \n");

        Log.e(TAG, "===>>> ReadMessage: Blocking on read 1\n");

        /*while (PACKET_LENGTH_SIZE - read > 0) {
            if ((bytes_read = in.read(firstTwoBytes, read, PACKET_LENGTH_SIZE - read)) > 0)
					read += bytes_read;
        }*/
        while (PACKET_LENGTH_SIZE - read > 0)
        	{
				try
				{
	           		 read += in.read(firstTwoBytes, read, PACKET_LENGTH_SIZE - read);
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

        Log.e(TAG, "===>>> ReadMessage: Outside while 1 \n");

        messageSize = (int) (((int)firstTwoBytes[0]) << 8) & 0x0000FF00;
        messageSize |= ((int) firstTwoBytes[1] & 0x000000FF);
        log("ReadMessage (): SLP server message length = " + messageSize);

        message = new byte[messageSize];
        message[0] = firstTwoBytes[0];
        message[1] = firstTwoBytes[1];
        read = 0;
		bytes_read = 0;

        Log.e(TAG, "===>>> ReadMessage: Blocking on read 2\n");

        /*while (messageSize - PACKET_LENGTH_SIZE - read > 0) {
            if ((bytes_read = in.read(message, PACKET_LENGTH_SIZE + read, messageSize - PACKET_LENGTH_SIZE - read)) > 0)
					read += bytes_read;
        }*/
       while (messageSize - PACKET_LENGTH_SIZE - read > 0) {
           
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
        Log.e(TAG, "===>>> ReadMessage: Outside while 2 \n");

        return message;
    }

    /**
     * Closes active connection.
     */
    @Override
    public void FreeConnection() {
        log("[GPS_NET] <FreeConnection>");
 
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
        if (isConnectionActive)
            return 1;
        else
            return 0;
    }

    /**
     * Main read procedure
     */
    private void ReadProc() {
        try {
            int num = 0;

            log(" ===>>> ReadProc: Entering with isConnectionActive " + isConnectionActive);

            while(isConnectionActive) {
                byte [] mess = ReadMessage ();
                if (mess == null) {
                    log("ReadMessage end of stream!");
                    Reset();
                    return;
                }
                else {
                    PostSLPMessageToQueue(mess);
                }
            }
        }
        catch (IOException e) {
            Log.e(TAG, "ReadProc(): I/O Exception, " + e.getMessage());
            Reset();
        }
		catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "ReadProc(): ArrayIndexOutOfBoundsException, " + e.getMessage());
            Reset();
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
        return Write(data, 0, data.length);
    }

    /**
     * Prints messages to Android log.
     */
    private void log(String str) {
               Log.d(TAG, str);
    }
}
