package com.contour.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import android.bluetooth.BluetoothSocket;

import com.contour.connect.debug.CLog;

public class CommThread extends Thread implements BaseCommThread {
    static final String TAG = "CommThread";
    static final boolean D = true;
  

    private static final int MAX_PACKET_SIZE = 65535;

    private BluetoothSocket mmSocket;
    private OutputStream mmOutput;
    private InputStream mInputStream;
    private ByteBuffer mmRecvBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
    private final CommThreadCallback mCallback;
    boolean mDeviceUnknown = true;
    boolean mUseOldProtocol = false;
    int packetHeaderLength = CameraProtocol.PacketHeader.LENGTH;
    private volatile boolean mCancelled = false;
    
    public CommThread(CommThreadCallback callback, BluetoothSocket socket) {
        mCallback = callback;
        mmSocket = socket;
        mmRecvBuffer.order(ByteOrder.LITTLE_ENDIAN);
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            if(D) CLog.out(TAG,"temp sockets not created", e);
        }

        mInputStream = tmpIn;
        mmOutput = tmpOut;
    }

    @Override
    public void run() {
        final int tenKb = 10 * 1024;    
        byte[] buffer = new byte[tenKb];
        write(new CameraProtocol.AckPacket().toByteArray());
//         write(new CmdPacket(CameraProtocolOld.CMD_PING, 0, null).toByteArray());
        try {
            int bytesRead = mInputStream.read(buffer);
            while (!mCancelled && bytesRead != -1) {
//                byte[] tempdata = new byte[bytesRead];
                
//                System.arraycopy(buffer, 0, tempdata, 0, bytesRead);
//                receivedData(tempdata, bytesRead);
                receivedData(buffer,bytesRead);
                bytesRead = mInputStream.read(buffer);
            }
        } catch (IOException e) {
            if (D)
                CLog.out(TAG, "Read stream broken", e);
            if(!mCancelled)
            	mCallback.onCameraCommDisconnected();
        }
    }

    public void write(byte[] bytes) {
        synchronized (mmOutput) {
            try {
                mmOutput.write(bytes);
                mmOutput.flush();
            } catch (Exception e) {
                if (D)
                    CLog.out(TAG, "Unable to write to device: ", e);
            }
        }
    }

    public void cancel() {
    	mCancelled = true;
        try {
            if (mmSocket != null) {
                mmSocket.close();
                mmSocket = null;
            }
        } catch (Exception e) {
            if(D) CLog.out(TAG,"Failed to close socket while canceling comms thread.", e);
            return;
        }
        if(D) CLog.out(TAG,"Socket Cancelled");
    }
    

    private void receivedData(byte[] buffer, int len) {
        if (mmRecvBuffer.remaining() < len) {
            if(D) CLog.out(TAG,"Buffer overflow, pos",mmRecvBuffer.position(),"rem",mmRecvBuffer.remaining(),"len",len);
            mmRecvBuffer.clear();

            mCallback.onCameraCommDisconnected();     
            return;
        }

        // Copy over the received data
        mmRecvBuffer.put(buffer, 0, len);
        if(mDeviceUnknown && mmRecvBuffer.position() >= 5) {
            int p = mmRecvBuffer.position();
            mmRecvBuffer.rewind();
            if(mmRecvBuffer.get() == 2) {
                if(mmRecvBuffer.getInt() >= 17) {
                    CLog.out(TAG,"DEVICE KNOWN GPS/C+");
                    mUseOldProtocol = true;
                    packetHeaderLength = CameraProtocolOld.PacketHeader.LENGTH;
                } else {
                    CLog.out(TAG,"DEVICE KNOWN C+2");
                    mUseOldProtocol = false;
                    packetHeaderLength = CameraProtocol.PacketHeader.LENGTH;
                }
                mDeviceUnknown = false;
                mCallback.onCameraCommStarted(mUseOldProtocol);
            }
            mmRecvBuffer.position(p);
        }

        if (mmRecvBuffer.position() >= packetHeaderLength)
            parsePackets();
    }

    private void parsePackets() {
        int bufferEnd = mmRecvBuffer.position();
        int dataRemaining = bufferEnd;

        // Start at beginning of buffer
        mmRecvBuffer.rewind();

        // Check that enough data remains in the buffer to grab a packet
        // header
        while (dataRemaining >= packetHeaderLength) {
            // Parse header
            BasePacketHeader header;
            if(mUseOldProtocol)
                header = new CameraProtocolOld.PacketHeader(mmRecvBuffer);
            else
                header = new CameraProtocol.PacketHeader(mmRecvBuffer);


            // Check for header validity
            if (!header.isValid()) {
                if(D) CLog.out(TAG,"Bogus packet header - " + header.toString());
                mmRecvBuffer.clear();
//                cancel();
                return;
            }

            // Check if there is enough data to deserialize the rest of the
            // packet
            if (dataRemaining < header.getLength())
                break;

            // Deserialize the rest of the packet
            BasePacket packet = deserializePacket(header,mmRecvBuffer);
            if (packet != null)
                mCallback.onReceivedPacket(packet);
            else {
                CLog.out(TAG,"Caught an exception while parsing a packet.  Forcing disconnection");
                mCallback.onCameraCommDisconnected();
                return;
            }
            

            if(mUseOldProtocol)
                dataRemaining -= header.getLength();
            else
                dataRemaining -= (header.getLength() + CameraProtocol.PacketHeader.LENGTH);
        }

        // Remove the packet data from the buffer, and place the
        // remaining data at the start of the buffer.
        if (dataRemaining > 0 && dataRemaining != bufferEnd) {
            mmRecvBuffer.position(bufferEnd - dataRemaining);

            // Temporary swap buffer
            byte[] swap = new byte[dataRemaining];

            // Grab the remaining data in the buffer
            mmRecvBuffer.get(swap);
            // Clear out the buffer
            mmRecvBuffer.clear();
            // Place the remaining data at the beginning of the buffer
            mmRecvBuffer.put(swap);

            // Set the new end of the buffer
            bufferEnd = dataRemaining;
        } else if (dataRemaining == 0)
            bufferEnd = 0;

        // Move the buffer position to the end
        mmRecvBuffer.position(bufferEnd);
    }
    
    BasePacket deserializePacket(BasePacketHeader header, ByteBuffer buffer) {
        if (mUseOldProtocol)
            return CameraProtocolOld.deserializePacket((CameraProtocolOld.PacketHeader) header, mmRecvBuffer);
        return CameraProtocol.deserializePacket((CameraProtocol.PacketHeader) header, mmRecvBuffer);
    }
}