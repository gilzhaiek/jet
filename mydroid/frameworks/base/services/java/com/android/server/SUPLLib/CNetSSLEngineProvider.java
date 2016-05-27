//package android.networking;
package android.supl;

//package com.android.server.supl;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.Provider;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

public class CNetSSLEngineProvider extends CNetTLSProviderBase {
    private SSLEngine engine;
    private SocketChannel channel = null;
    private ByteBuffer readMessage = null; // for read thread

    private Object ReHandshakeSynch = new Object();
    private int ReHandshakeResult = 0;

    private ByteBuffer ToServAppData;
    private ByteBuffer ToServNetData;
    private ByteBuffer FromServAppData;
    private ByteBuffer FromServNetData;

    private ReceiveThread ReadListener = null;

    private static boolean resultOnce = true;

    private class ReHanshakeThread extends Thread {
        public void run() {
            ReHandshake();
        }
    }

    private class ReceiveThread extends Thread {
        public void run() {
            ReadProc();
        }
    }

    public CNetSSLEngineProvider(SSLContext Context) {
        super(Context);
    }

    @Override
    public int CreateConnection(String Host_port) {
        try {
            super.CreateConnection(Host_port);
            engine = sslContext.createSSLEngine(InetAddr.getHostName(), Port);
            engine.setUseClientMode(true);
            engine.setEnableSessionCreation(true);
            Provider prov = sslContext.getProvider();
            log("SSL context provider - " + prov.getName() + ": "
                + prov.getInfo());
            String[] cipherSuites = engine.getSupportedCipherSuites();
            int rezult = 0;
            log("names of the cipher suites which could be enabled: ");
            for (int i = 0; i < cipherSuites.length; i++) {
                log("Suite " + i + ":" + cipherSuites[i]);
            }
            channel = SocketChannel.open();
            Socket s = channel.socket();

            log("Try connect to server: " + InetAddr.getHostName() + ":" 
                    + Port);

            s.connect(new InetSocketAddress(InetAddr.getHostName(), Port));
	    channel = s.getChannel();
            log("===>> Channel value is: <<===" + channel );
            channel.configureBlocking(false);
            log("===>> Creating Buffer <<===");
            createBuffers();

            if ((rezult = doHandshake(true)) < 0) {
                log("Initial Handshake error: " + rezult);
                return -1;
            }

            InitialHandshakeComplete = true;
            return 0;
        } 
        catch (UnknownHostException e) {
            Reset();
            log("CreateConnection(): Unknown host exception, " 
                    + e.getMessage());
            return -1;
        } 
        catch (SSLException e) {
            Reset();
            log("CreateConnection(): SSL engine exception, " + e.getMessage()
                + " - " + e.getCause());
            return -1;
        } catch (IOException e) {
            Reset();
            log("CreateConnection(): I/O Exception, " + e.getMessage());
            return -1;
        }
    }

    @Override
    public synchronized void FreeConnection() {
        if (InitialHandshakeComplete == false) {
            log("InitialHandshakeComplete == false");
            return;
        }
        log("<FreeConnection>");
        try {
            InitialHandshakeComplete = false; // no more I/O operations
            engine.closeOutbound();
            ToServAppData.clear();
            ToServNetData.clear();
            SSLEngineResult res;
            while (!engine.isOutboundDone()) {
                // Get close message
                res = engine.wrap(ToServAppData, ToServNetData);
                // Check res statuses
                switch (res.getStatus()) {
                    case OK:
                        // Send close message to peer
                        while (ToServNetData.hasRemaining()) {
                            int num = channel.write(ToServNetData);
                            if (num == -1) {
                                // handle closed channel
                            } else if (num == 0) {
                                // no bytes written; try again later
                            }
                            ToServNetData.compact();
                        }
                        break;
                    case BUFFER_UNDERFLOW:
                    case BUFFER_OVERFLOW:
                        log("FreeConnection(): Incorrect close outbound.");
                    case CLOSED:
                        Reset();
                    return;
                }

            } // while
        } 
        catch (Exception e) {
            log("FreeConnection(): Incorrect close." + e.getMessage());
        }
        Reset(); // free all resources
        return;
    }

    @Override
    public int Init() {
        return 0;
    }

    @Override
    public int IsActive() {
        if (InitialHandshakeComplete) {
            return 1;
        }
        else {
            return 0;
        }
    }

    private void resize_ToServAppData_Buffer(boolean need_flip) {
        int appBuffNeed = engine.getSession().getApplicationBufferSize();

        if (appBuffNeed > ToServAppData.capacity()) {
            ByteBuffer new_buffer = ByteBuffer.allocate(appBuffNeed);
            if (need_flip) {
                ToServAppData.flip();
            }
            new_buffer.put(ToServAppData);
            ToServAppData = new_buffer;
        }
    }

    private void resize_FromServAppData_Buffer(boolean need_flip) {
        int appBuffNeed = engine.getSession().getApplicationBufferSize();

        if (appBuffNeed > FromServAppData.capacity()) {
            ByteBuffer new_buffer = ByteBuffer.allocate(appBuffNeed);
            if (need_flip) {
                FromServAppData.flip();
            }
            new_buffer.put(FromServAppData);
            FromServAppData = new_buffer;
        }
    }

    private void resize_ToServNetData_Buffer(boolean need_flip) {
        int netBuffNeed = engine.getSession().getApplicationBufferSize();

        if (netBuffNeed > ToServNetData.capacity()) {
            ByteBuffer new_buffer = ByteBuffer.allocate(netBuffNeed);
            if (need_flip) {
                ToServNetData.flip();
            }
            new_buffer.put(ToServNetData);
            ToServNetData = new_buffer;
        }
    }

    private void resize_FromServNetData_Buffer(boolean need_flip) {
        int netBuffNeed = engine.getSession().getApplicationBufferSize();

        if (netBuffNeed > FromServNetData.capacity()) {
            ByteBuffer new_buffer = ByteBuffer.allocate(netBuffNeed);
            if (need_flip) {
                FromServNetData.flip();
            }
            new_buffer.put(FromServNetData);
            FromServNetData = new_buffer;
        }
    }

    private synchronized int Write(byte[] data) throws SSLException,
        IOException {

        if (!InitialHandshakeComplete) {
            return -1;
        }

        int numWrite = 0;
        boolean exit = false;
        int remain = data.length;
        int off = 0;
        SSLEngineResult res = null;

        while (remain > 0) {
            if (remain > ToServAppData.remaining()) {
                ToServAppData.put(data, off, ToServAppData.remaining());
                off += ToServAppData.remaining();
                remain = data.length - off;
            } 
            else {
                ToServAppData.put(data, off, remain);
                off += remain;
                remain = 0;
            }

            ToServAppData.flip();
            res = engine.wrap(ToServAppData, ToServNetData);

            if (res.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
                switch (res.getStatus()) {
                    case OK:
                    case BUFFER_OVERFLOW:
                        ToServNetData.flip();
                        while (ToServNetData.hasRemaining()) {
                            log("write(): " + ToServNetData.remaining());
                            if (channel.write(ToServNetData) < 0) {
                             throw new IOException(
                                "channel has reached end-of-stream.");
                            }

                        }

                        ToServNetData.clear();
                        if (res.getStatus() == 
                                SSLEngineResult.Status.BUFFER_OVERFLOW) {
                            resize_ToServNetData_Buffer(true); // need flip
                        }
                        ToServAppData.compact();
                        break;
                    case BUFFER_UNDERFLOW: // need more App data -- ??
                        if (remain == 0 && ToServAppData.remaining() != 0) {
                            throw new SSLException(
                                "Write error: underflow at the wrap operation.");
                        }
                        ToServAppData.compact();
                        break;

                    case CLOSED:
                        throw new SSLException("Write engine was closed.");
                } // switch
            } 
            else {
                // Need re-handshake
                synchronized (ReHandshakeSynch) {
                    ReHanshakeThread RH = new ReHanshakeThread();
                    RH.start();
                    try {
                        ReHandshakeSynch.wait();
                    } 
                    catch (InterruptedException e) {
                        throw new IOException("ReHandshake interrupted.");
                    }

                    if (ReHandshakeResult < 0) {
                        IOException e = new IOException("ReHandshake filed:"
                            + ReHandshakeResult);
                        ReHandshakeResult = 0;
                        throw e;
                    }
                } // synchronized
            }
        } // while

        ToServAppData.clear();
        ToServNetData.clear();
        return data.length;
    }

    private synchronized ByteBuffer Read() throws SSLException, IOException {

        if (!InitialHandshakeComplete) {
            return null;
        }

        SSLEngineResult res = null;

        int numRead = 0;
        boolean exit = false;

        while (!exit) {
            numRead = channel.read(FromServNetData);
            if (numRead == -1) {
                throw new IOException("channel has reached end-of-stream.");
            }

            FromServNetData.flip();
            res = engine.unwrap(FromServNetData, FromServAppData);

            if (res.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
                switch (res.getStatus()) {
                    case OK:
                    case BUFFER_UNDERFLOW:
                        if (FromServNetData.remaining() == 0) {
                            FromServNetData.compact();
                            numRead = channel.read(FromServNetData);
                            if (numRead == 0) {
                                exit = true;
                            }
                        } 
                        else {
                            FromServNetData.compact();
                        }
                        break;
                    case BUFFER_OVERFLOW:
                        FromServAppData.flip();
                        readMessage.put(FromServAppData);
                        FromServAppData.clear();
                        FromServNetData.compact();
                        resize_FromServAppData_Buffer(true);
                        break;
                    case CLOSED:
                        throw new SSLException("SSL engine was closed.");
                }
            } 
            else {
                // throw new SSLException("Need re-handshake");
                // or start re-handshake thread!
                synchronized (ReHandshakeSynch) {
                    ReHanshakeThread RH = new ReHanshakeThread();
                    RH.start();
                    try {
                        ReHandshakeSynch.wait();
                    } 
                    catch (InterruptedException e) {
                        throw new IOException("ReHandshake interrupted.");
                    }

                    if (ReHandshakeResult < 0) {
                        IOException e = new IOException("ReHandshake filed:"
                            + ReHandshakeResult);
                        ReHandshakeResult = 0;
                        throw e;
                    }
                } // synchronized
            } // else
        } // while

        FromServAppData.flip();
        readMessage.put(FromServAppData);
        FromServAppData.clear();
        FromServNetData.clear();
        // native method
        return readMessage;
    }

    private void ReadProc() {
        try {
            Selector selector = Selector.open();
            SelectionKey keylisten = channel.register(selector,
                SelectionKey.OP_READ);
            int num = 0;

            while (InitialHandshakeComplete) {
                log("Select!");
                while ((num = selector.select()) == 0);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                log("Ready for reading!");
                if (it != null && it.hasNext()) {
                    readMessage = ByteBuffer.allocate(10240);
                    SelectionKey key = (SelectionKey) it.next();
                    if (key.equals(keylisten) && ((key.readyOps() 
                            & SelectionKey.OP_READ) == SelectionKey.OP_READ)) {
                        if (Read() == null) {
                            continue;
                        }
                        else {
                            byte[] mess = new byte[readMessage.position()];
                            readMessage.flip();
                            readMessage.get(mess, 0, mess.length);
                            int messageSize = (int) (mess[0] << 8 | mess[1]);
                            log("Try PostSLPMessageToQueue length:"
                                + mess.length + "/length:" + messageSize);
                            PostSLPMessageToQueue(mess);
                            DumpMessage(mess);
                            readMessage = null;
                             mess = null;
                            //
                        }
                    } else {
                        // error selector key
                        log("Select error");
                    }
                    keys.clear();
                } 
                else {
                    // error selector key
                    log("Selector error: keys.iterator()");
                }
            // selector.close();
            }
        } 
        catch (SSLException e) {
            log("ReadProc(): SSL engine exception, " + e.getMessage());
            Reset();
            return;
        } 
        catch (IOException e) {
            log("ReadProc(): I/O Exception, " + e.getMessage());
            Reset();
            return;
        } 
        catch (ClosedSelectorException e) {
            log("ReadProc(): selector was closed," + e.getMessage());
            Reset();
            return;
        }
    }

    @Override
    public int Receive() {
        if (ReadListener == null) {
           ReadListener = new ReceiveThread();
            ReadListener.start();
        }
        return 0;
    }

    @Override
    public void ResetConnection() {
        //
    }

    @Override
    public int Send(byte[] data/* , int len */) {
        try {
            DumpMessage(data);
            return Write(data);
        } 
        catch (SSLException e) {
            log("Send() error:" + e.getMessage());
            return -1;
        } 
        catch (IOException e) {
            log("Send() error:" + e.getMessage());
            return -1;
        }
    }

    private void Reset() {
        InitialHandshakeComplete = false;
        try {
            if (channel != null) {
            channel.socket().close();
            channel.close();
            log("channel != null: socket.close()!");
            log("Client socket is " + channel.isConnected());
            }
        } 
        catch (IOException e) {
            log("Reset() I/O Exception:" + e.getMessage());
        }

        try {
            if (ReadListener != null) {
                if (ReadListener.isAlive()) {
                    ReadListener.interrupt();
                }
            ReadListener = null;
            }
        } 
        catch (SecurityException e) {
            log("Can't interrupt the listener task:" + e.getMessage());
        }

        channel = null;
        engine = null;
        readMessage = null;
        ToServAppData = null;
        ToServNetData = null;
        FromServAppData = null;
        FromServNetData = null;
        ReHandshakeResult = 0;
    }

    private void ReHandshake() {
        synchronized (ReHandshakeSynch) {
            try {
                int reHnd = 0;
                FromServAppData.flip();
                readMessage.put(FromServAppData);
                FromServAppData.clear();

                ToServNetData.flip();
                while (ToServNetData.hasRemaining()) {
                    if (channel.write(ToServNetData) < 0) {
                        throw new IOException(
                            "channel has reached end-of-stream.");
                    }
                }
                ToServNetData.clear();
                ReHandshakeResult = doHandshake(false);
                ReHandshakeSynch.notifyAll();
            } 
            catch (SSLException e) {
                log("SSL engine error ReHadshake:" + e.getMessage());
                ReHandshakeSynch.notifyAll();
                ReHandshakeResult = -10;
                return;
            } 
            catch (IOException e) {
                log("I/0 Error ReHadshake:" + e.getMessage());
                ReHandshakeSynch.notifyAll();
                ReHandshakeResult = -11;
                return;
            }
        }
    }

    private int doHandshake(boolean initial) throws SSLException, IOException {
        int appBufferSize = engine.getSession().getApplicationBufferSize() + 100;
        ByteBuffer HToServAppData;
        ByteBuffer HFromServAppData;
        boolean bug_exitWithNOT_HANDSHAKING = false;
        if (initial == true) {
            HToServAppData = ByteBuffer.allocate(appBufferSize);
            HFromServAppData = ByteBuffer.allocate(appBufferSize);
        } else {
            HToServAppData = ToServAppData;
            HFromServAppData = FromServAppData;
        }

        engine.beginHandshake();

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        SSLEngineResult res = null;
        if (initial == true) {
            log("**INITIAL HANDSHAKE BEGIN**  ");
        } else {
            log("**ReHANDSHAKE BEGIN**  ");
        }

        log("Start handshake status =" + hs);
        while (hs != SSLEngineResult.HandshakeStatus.FINISHED
            && bug_exitWithNOT_HANDSHAKING == false) {
            switch (hs) {
                case NEED_UNWRAP:
                    int r = 0;
                    if ((r = channel.read(FromServNetData)) < 0) {
                        throw new IOException(
                                "channel has reached end-of-stream.");
                    }
                    log("NEED_UNWRAP: was read " + r + "bytes");
                    FromServNetData.flip();
                    res = engine.unwrap(FromServNetData, HFromServAppData);
                    log("after unwrap:", res);
                    switch (res.getStatus()) {
                        case OK:
                            FromServNetData.compact();
                            break;

                        case BUFFER_UNDERFLOW:
                            resize_FromServNetData_Buffer(false);
                            FromServNetData.compact();
                            if (FromServNetData.remaining() == 0) { // No free 
                                                                //space for read data
                                throw new SSLException(
                                    "SSL error: underflow at the unwrap" 
                                    + "operation at the handshake state!");
                            }
                            break;
                        case BUFFER_OVERFLOW:
                            if (initial == false) {
                                HFromServAppData.flip();
                                readMessage.put(HFromServAppData);
                                int appSize = engine.getSession()
                                    .getApplicationBufferSize();
                                if (appSize > HFromServAppData.capacity()) {
                                    FromServAppData = 
                                        ByteBuffer.allocate(appSize);
                                    HFromServAppData = FromServAppData;
                                } 
                                else {
                                    HFromServAppData.clear();
                                }
                                FromServNetData.compact();
                            } 
                            else {
                                // Reset(); //some undefined SSLEngine error
                                throw new SSLException(
                                "SSL initial handshake undefined error.");
                            }
                            break;
                        case CLOSED:
                            // Reset();
                            throw new SSLException(
                                "SSL engine was closed at the handshake state"
                                + "(unwrap).");
                    }
                hs = res.getHandshakeStatus();
                break;
            case NEED_WRAP:
                // ToServNetData.clear();
                res = engine.wrap(HToServAppData, ToServNetData);
                log("after wrap:", res);
                switch (res.getStatus()) {
                    case OK:
                    case BUFFER_OVERFLOW:
                        ToServNetData.flip();
                        while (ToServNetData.hasRemaining()) {
                            if (channel.write(ToServNetData) < 0)
                                throw new IOException(
                                    "channel has reached end-of-stream.");
                        }
                        ToServNetData.clear();
                        if (res.getStatus() 
                                == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                            resize_ToServNetData_Buffer(true); // need flip
                        }
                        break;
                    case BUFFER_UNDERFLOW: // need more App data -- ??
                        // Reset(); //some undefined SSLEngine error
                        throw new SSLException(
                            "SSL error: underflow at the wrap operation" 
                            + "at the handshake state!");
                     case CLOSED:
                        // Reset();
                         throw new SSLException(
                            "SSL engine was closed at the handshake" + 
                            "state (wrap).");
                }
                hs = res.getHandshakeStatus();
                break;
            case NEED_TASK:
                Runnable task;
                while ((task = engine.getDelegatedTask()) != null) {
                    Thread t = new Thread(task);
                    t.start();
                    try {
                        t.join();
                    } 
                    catch (InterruptedException e) {
                        log("Error join handshake thread:" + e.getMessage());
                    }
                }
                log("Handshake in-between task is complete.");
                hs = SSLEngineResult.HandshakeStatus.NEED_WRAP;
                break;
        case NOT_HANDSHAKING:
            if (initial == true) {
                log("Handshake NOT_HANDSHAKING engine status");
                log("\t   Host name: " + engine.getSession().getPeerHost());
                log("\t   Host port: " + engine.getSession().getPeerPort());
                log("\t   Protocol : " + engine.getSession().getProtocol());
                log("\t   Cipher suite : "
                    + engine.getSession().getCipherSuite());
                log("\t   Session Id   : " + engine.getSession().getId());
                bug_exitWithNOT_HANDSHAKING = true;
                break;
            } 
            else {
                return 0; // Engine is not in handshaking state
            }
        } // switch
        }

        HToServAppData.compact();
        log("****", res);
        // resizeBuffers ();
        if (initial) {
            InitialHandshakeComplete = true;
        }
        return 0; // All OK
    }

    private void createBuffers() {
        int pad = 100;
        SSLSession session = engine.getSession();
        ToServAppData = ByteBuffer.allocate(session.getApplicationBufferSize()
            + pad);
        ToServNetData = ByteBuffer
            .allocate(session.getPacketBufferSize() + pad);
        FromServAppData = ByteBuffer.allocate(session.getApplicationBufferSize()
            + pad);
        FromServNetData = ByteBuffer.allocate(session.getPacketBufferSize()
            + pad);
    }

    private void log(String str, SSLEngineResult result) {
        if (!logging) {
            return;
        }
        if (resultOnce) {
            resultOnce = false;
            System.out.println("The format of the SSLEngineResult is: \n"
                + "\t\"getStatus() / getHandshakeStatus()\" +\n"
                + "\t\"bytesConsumed() / bytesProduced()\"\n");
        }

        HandshakeStatus hsStatus = result.getHandshakeStatus();
        SSLEngineResult.Status status = result.getStatus();

        log(str + result.getStatus() + "/" + hsStatus + ", "
            + result.bytesConsumed() + "/" + result.bytesProduced()
            + " bytes");

        if (status == SSLEngineResult.Status.BUFFER_OVERFLOW) {
            log("\t...buffer overflow");
        }
        else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            log("\t...buffer underflow");
        }
        else if (status == SSLEngineResult.Status.CLOSED) {
            log("\t...The operation just closed this side of the SSLEngine.");
        }
        else if (status == SSLEngineResult.Status.OK) {
            log("\t...The SSLEngine completed the operation.");
        }

        if (hsStatus == HandshakeStatus.FINISHED) {
            log("\t...ready for application data, remaining data in net buffer:"
                + FromServNetData.remaining());
            log("\t   Host name: " + engine.getSession().getPeerHost());
            log("\t   Host port: " + engine.getSession().getPeerPort());
            log("\t   Protocol : " + engine.getSession().getProtocol());
            log("\t   Cipher suite : " + engine.getSession().getCipherSuite());
            log("\t   Session Id   : " + engine.getSession().getId());
        }
    }

    private void log(String str) {
        if (logging) {
            System.out.println(str);
        }
    }

}
