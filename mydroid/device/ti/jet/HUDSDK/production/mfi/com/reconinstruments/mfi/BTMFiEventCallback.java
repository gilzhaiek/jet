package com.reconinstruments.mfi;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.SPPM.ConnectionType;
import com.stonestreetone.bluetopiapm.SPPM.DataConfirmationStatus;
import com.stonestreetone.bluetopiapm.SPPM.IDPSState;
import com.stonestreetone.bluetopiapm.SPPM.IDPSStatus;
import com.stonestreetone.bluetopiapm.SPPM.LineStatus;
import com.stonestreetone.bluetopiapm.SPPM.PortStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.ServerEventCallback;

import java.util.EnumSet;

public class BTMFiEventCallback implements ServerEventCallback, ClientEventCallback {

    public static final int SESSION_DATA_RECEIVED_EVENT = 0;
    public static final int SESSION_DATA_CONFIRMATION_EVENT = 1;
    public static final int FROM_HUD_CONNECTED_EVENT = 2;
    public static final int FROM_IPHONE_CONNECTED_EVENT = 3;
    public static final int DISCONNECTED_EVENT = 4;
    public static final int SESSION_OPEN_REQUEST_EVENT = 5;
    public static final int SESSION_CLOSE_EVENT = 6;


    private final String TAG = this.getClass().getName();
    private final String mName;
    private Handler mHandler;

    public BTMFiEventCallback(String name, Handler handler) {
        mName = name;
        mHandler = handler;
    }

    @Override
    public void dataReceivedEvent(int dataLength) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Data Received Event: Data Length: ").append(dataLength);
        Log.i(TAG, sb.toString());
    }

    @Override
    public void disconnectedEvent() {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Disconnected");
        Log.i(TAG, sb.toString());
        mHandler.sendEmptyMessage(DISCONNECTED_EVENT);
    }

    @Override
    public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" IDPS Status: \n State: ").append(idpsState.name());
        sb.append("\n Status: ").append(idpsStatus.name());
        Log.i(TAG, sb.toString());
    }

    @Override
    public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Line Status Changed:");
        if (lineStatus.isEmpty()) {
            sb.append(" No errors");
        } else {
            if (lineStatus.contains(LineStatus.OVERRUN_ERROR)) {
                sb.append(" OVERRUN_ERROR");
            }
            if (lineStatus.contains(LineStatus.PARITY_ERROR)) {
                sb.append(" PARITY_ERROR");
            }
            if (lineStatus.contains(LineStatus.FRAMING_ERROR)) {
                sb.append(" FRAMING_ERROR");
            }
        }
        Log.d(TAG, sb.toString());
    }

    @Override
    public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status) {
        //We do not deal with non session data
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Non-Session Data Confirmation: Packet ID: ").append(packetID);
        sb.append(" Transaction ID: ").append(transactionID);
        sb.append(" Status: ").append((status != null ? status.name() : "null"));
        Log.d(TAG, sb.toString());
    }

    @Override
    public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data) {
        //We do not deal with non session data
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Non-Session Data Received: Lingo ID: ").append(lingoID);
        sb.append(" Command ID: ").append(commandID);
        sb.append(" Data Length: ").append(data.length);
        Log.d(TAG, sb.toString());
    }

    @Override
    public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append("Port Status Changed:");
        sb.append(" Enabled flags:");
        if (portStatus.isEmpty()) {
            sb.append(" NONE");
        } else {
            if (portStatus.contains(PortStatus.RTS_CTS)) {
                sb.append(" RTS_CTS");
            }
            if (portStatus.contains(PortStatus.DTR_DSR)) {
                sb.append(" DTR_DSR");
            }
            if (portStatus.contains(PortStatus.RING_INDICATOR)) {
                sb.append(" RING_INDICATOR");
            }
            if (portStatus.contains(PortStatus.CARRIER_DETECT)) {
                sb.append(" CARRIER_DETECT");
            }
        }
        sb.append(" Break Signal: ").append((breakSignal == true) ? "Enabled" : "Disabled");
        sb.append(" Break Timeout: ").append(breakTimeout);
        Log.d(TAG, sb.toString());
    }

    @Override
    public void sessionCloseEvent(int sessionID) {
        //TODO Is this a bug that the argument sessionID is always 0 ?
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Session Closed: Session ID: ").append(sessionID);
        Message msg = new Message();
        msg.what = SESSION_CLOSE_EVENT;
        msg.arg1 = sessionID;
        mHandler.sendMessage(msg);
        Log.i(TAG, sb.toString());
    }

    @Override
    public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Session Data Confirmation: Session ID: ").append(sessionID);
        sb.append(" Packet ID: ").append(packetID);
        sb.append(" Status: ").append((status != null ? status.name() : "null"));

        Message msg = new Message();
        msg.what = SESSION_DATA_CONFIRMATION_EVENT;
        msg.arg1 = sessionID;
        msg.arg2 = packetID;
        msg.obj = status;
        mHandler.sendMessage(msg);

        Log.i(TAG, sb.toString());
    }

    @Override
    public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Session Data Received: Session ID: ").append(sessionID);
        sb.append(" Data Length: ").append(sessionData.length);
        Message msg = new Message();
        msg.what = SESSION_DATA_RECEIVED_EVENT;
        msg.arg1 = sessionID;
        msg.obj = sessionData;
        mHandler.sendMessage(msg);
        Log.i(TAG, sb.toString());
    }

    @Override
    public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Session Open Request:");
        sb.append(" Maximum Transmit Packet: ").append(maximumTransmitPacket);
        sb.append(" Maximum Receive Packet: ").append(maximumReceivePacket);
        sb.append(" Session ID: ").append(sessionID);
        sb.append(" Protocol Index: ").append(protocolIndex);
        Log.i(TAG, sb.toString());
        Message msg = new Message();
        msg.what = SESSION_OPEN_REQUEST_EVENT;
        msg.arg1 = sessionID;
        msg.arg2 = protocolIndex;
        mHandler.sendMessage(msg);
    }

    @Override
    public void transmitBufferEmptyEvent() {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Transmit Buffer Empty");
        Log.i(TAG, sb.toString());
    }

    @Override
    public void connectedEvent(BluetoothAddress remoteDeviceAddress, ConnectionType type) {
        //A connected event showing a connection established from the Iphone to HUD
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Connected:");
        sb.append(" Address: ").append(remoteDeviceAddress.toString());
        sb.append(" Type: ");
        switch (type) {
            case SPP:
                sb.append("SPP");
                break;
            case MFI:
                sb.append("MFi");
                break;
            default:
                sb.append("Unknown type");
                Log.w(TAG, "There is an unknown connection type from iphone");
                break;
        }
        Log.i(TAG, sb.toString());
        Message msg = new Message();
        msg.what = FROM_IPHONE_CONNECTED_EVENT;
        msg.obj = remoteDeviceAddress.toString();
        mHandler.sendMessage(msg);
    }

    @Override
    public void connectionRequestEvent(BluetoothAddress remoteDeviceAddress) {
        //A connection request event coming from the iphone
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Connection Request:");
        sb.append(" Address: ").append(remoteDeviceAddress.toString());
        Log.d(TAG, sb.toString());
    }

    @Override
    public void connectionStatusEvent(ConnectionStatus status, ConnectionType type) {
        //A status event update after HUD tries to connect to iphone
        boolean isConnected = false;
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" Connection Status:");
        switch (status) {
            case SUCCESS:
                sb.append("SUCCESS");
                isConnected = true;
                break;
            case FAILURE_TIMEOUT:
                sb.append("FAILURE_TIMEOUT");
                break;
            case FAILURE_REFUSED:
                sb.append("FAILURE_REFUSED");
                break;
            case FAILURE_SECURITY:
                sb.append("FAILURE_SECURITY");
                break;
            case FAILURE_DEVICE_POWER_OFF:
                sb.append("FAILURE_DEVICE_POWER_OFF");
                break;
            case FAILURE_UNKNOWN:
            default:
                sb.append("FAILURE_UNKNOWN");
                break;
        }
        sb.append(" Connection Type: ");
        switch (type) {
            case SPP:
                sb.append("SPP");
                break;
            case MFI:
                sb.append("MFi");
                break;
            default:
                sb.append("Unknown Connection Type");
                Log.w(TAG, "There is unknown connection type from HUD connection");
                break;
        }
        if (isConnected) {
            Log.i(TAG, sb.toString());
        } else {
            Log.e(TAG, sb.toString());
        }
        Message msg = new Message();
        msg.what = FROM_HUD_CONNECTED_EVENT;
        msg.arg1 = isConnected ? 1 : 0;
        mHandler.sendMessage(msg);
    }

    @Override
    public void unhandledControlMessageReceivedEvent(short controlMessageID, byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append(mName).append(" unhandledControlMessageReceivedEvent:");
        sb.append(" controlMessageID: ").append(controlMessageID);
        sb.append(" data: ").append(data.length);
        Log.d(TAG, sb.toString());
    }
}
