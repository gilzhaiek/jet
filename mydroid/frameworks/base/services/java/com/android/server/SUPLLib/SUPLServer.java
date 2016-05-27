
package com.android.server;

import android.supl.*;
import android.supl.config.SuplConfig;
import android.util.Log;
import android.content.Context;

import java.lang.*;
import java.lang.StringBuffer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import  java.io.BufferedInputStream;


/**
 *  Description: Is created at system bootup and starts listening for NAL messages.
 *  Note:
 */
class SUPLServer implements Runnable {

    /* Native function to listen for NAL messages. */
    private native boolean nativeStartSocketServer();

    public static final String CONFIG_PATH = "/system/etc/gps/config/SuplConfig.spl";


    /**
     * The default port used to start SUPL servers.
     */
    public static final int VIEW_SERVER_DEFAULT_PORT = 5000;

    // Debug facility
    private static final String LOG_TAG = "SUPLServer";

    private static final String VALUE_PROTOCOL_VERSION = "2";
    private static final String VALUE_SERVER_VERSION = "2";

    // Protocol commands
    // Returns the protocol version
    private static final String COMMAND_PROTOCOL_VERSION = "PROTOCOL";
    // Returns the server version
    private static final String COMMAND_SERVER_VERSION = "SERVER";
    // Lists all of the available windows in the system
    private static final String COMMAND_WINDOW_MANAGER_LIST = "LIST";

    private ServerSocket mServer;
    private Thread mThread;
    private Thread SLPThread;
    private Thread TestThread;

    private Context mContext;

    private final SUPLService mSuplServer;
    private final int mPort;
    private boolean checkBind;

    private static String Path = "/certificate/client_keystore.bks";
    public static String Impl = "Socket";
    private static String Pass = "123456";

    /**
     * Loads native library.
     */
    static {
        /* Load SUPL_HelperService_JNI library. */
        System.loadLibrary("suplhelperservicejni");

    }


    /**
     * Creates a new SUPLServer associated with the specified SUPL Service.
     *
     */
    SUPLServer(SUPLService suplserv) {
        this(suplserv, VIEW_SERVER_DEFAULT_PORT);
    //Log.d(LOG_TAG," SUPLServer: Constructor 1");
    }

    /**
     * Creates a new SUPLServer associated with the specified SUPL Service on the
     * specified local port. The server is not started by default.
     *
     */
    SUPLServer(SUPLService suplserv, int port) {
    //Log.d(LOG_TAG," SUPLServer: Constructor 2");
        mSuplServer = suplserv;
        mPort = port;
    }

    SUPLServer(SUPLService suplserv, Context context) {
    //Log.d(LOG_TAG," SUPLServer: Constructor 3");
        this(suplserv);
        mContext = context;
    }

    /**
     * Starts the server.
     *
     * @return True if the server was successfully created, or false if it already exists.
     * @throws IOException If the server cannot be created.
     *
     * @see #stop()
     * @see #isRunning()
     * @see SUPLService#startSuplServer(int)
     */
    boolean start() throws IOException {

    //Log.d(LOG_TAG," SUPLServer: Start");
        if (mThread != null) {
            return false;
        }

    if (SLPThread != null) {
        return false;
    }

        // Port Number, Number of Client allowed, address of client
        mServer = new ServerSocket(mPort, 5, InetAddress.getLocalHost());
        mThread = new Thread(this, "SLP Server [port=" + mPort + "]");
        SLPThread = new Thread(this, "SLP Client Thread [port=" + mPort + "]");
    TestThread = new Thread(this,"===>>TEST Thread <<<==");
    checkBind = mServer.isBound();
    /*
    if( checkBind )
        //Log.d(LOG_TAG,"===>>>> Bounded <<<===");
    else
        //Log.d(LOG_TAG,"===>>>> Not Bounded <<<===");

    Log.d(LOG_TAG,"===>>> SUPLServer Started <<<===");
    */
        mThread.start();

        return true;
    }

    /**
     * Stops the server.
     *
     * @return True if the server was stopped, false if an error occured or if the
     *         server wasn't started.
     *
     * @see #start()
     * @see #isRunning()
     * @see SUPLService#stopSuplServer()
     */
    boolean stop() {
    //Log.d(LOG_TAG,"===>>> SUPLServer Stop <<<===");
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
            try {
        //Log.d(LOG_TAG,"===>>> SUPLServer closing connection <<<===");
                mServer.close();
                mServer = null;
                return true;
            } catch (IOException e) {
                Log.w(LOG_TAG, "Could not close the view server");
            }
        }
        return false;
    }

    /**
     * Indicates whether the server is currently running.
     *
     * @return True if the server is running, false otherwise.
     *
     * @see #start()
     * @see #stop()
     * @see SUPLService#isSUPLServerRunning()
     */
    boolean isRunning() {
        return mThread != null && mThread.isAlive();
    }

    /**
     * Main server loop.
     */
    public void run() {

    //Log.d(LOG_TAG,"===>>> SUPLServer RUN: 1111  <<<===");
        while (Thread.currentThread() == mThread) {

            // Any uncaught exception will crash the system process
            try {
                try {
            //Log.w(LOG_TAG,"===>>> SEND GPS Command to MCPF <<<===");
                    //CNet.SetPath(Path);
                    //CNet.SetImpl(Impl);

                    // Read Config File (SuplConfig.spl)
                    SuplConfig spl = new SuplConfig();
                    spl.readConfigPath(CONFIG_PATH);
                    spl.PrintConfig();

                    // CNet configure
                	//Log.w(LOG_TAG, "Setting CNet parameters: Implementation [" + spl.getImplType() + "] Protocol [" + spl.getProtocolType() + "] SLP Host [" + spl.getSLPHost() + "]");
					Log.w(LOG_TAG, "Setting CNet parameters: Implementation [" + spl.getImplType() + "] Protocol [" + spl.getProtocolType() + "]");
                    CNet.SetPath(spl.getStorePath());
                    CNet.SetImpl(spl.getImplType());
                    CNet.SetProtocolType(spl.getProtocolType());
                    CNet.localhost = spl.isLocalhost();
                    //CNet.slphost_port = spl.getSLPHost();

                    /* */
                    CNetSSLSocketProvider.SetAutoFqdnStorePath(spl.getAutoFqdnStorePath());
                    CNetSSLSocketProvider.SetFqdnPhoneStorePath(spl.getStorePath());
					// CSUPL_LP Init and SetContext
					CSUPL_LP.Init(mContext);					

                    // CSUPL_LP Configure
                    CSUPL_LP.SetMcc(spl.getMcc());
                    CSUPL_LP.SetMnc(spl.getMnc());
                    CSUPL_LP.SetLac(spl.getLac());
                    CSUPL_LP.SetCi(spl.getCi());
					CSUPL_LP.SetCellType(spl.getCellType());
					CSUPL_LP.SetMsisdn(spl.getMsisdn());
					CSUPL_LP.SetHMcc(spl.getHmcc());
                    CSUPL_LP.SetHMnc(spl.getHmnc());


                    /* Start Socket Server. */
                    nativeStartSocketServer();
                } finally {
                }

            } catch (Exception e) {
                Log.w(LOG_TAG, "Connection error: ", e);
            } finally {
            //Log.d(LOG_TAG,"===>> INSDIE FINALLY <<===");
        }

        }
   }

   private static boolean writeValue(Socket client, String value) {
        boolean result;
        BufferedWriter out = null;
        try {
        //Log.d(LOG_TAG,"===>>> SUPLServer writeValue: 1111  <<<===");
            OutputStream clientStream = client.getOutputStream();
            out = new BufferedWriter(new OutputStreamWriter(clientStream), 8 * 1024);
            out.write(value);
            out.write("\n");
            out.flush();
            result = true;
        } catch (Exception e) {
            result = false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    result = false;
                }
            }
        }
        return result;
    }
}

