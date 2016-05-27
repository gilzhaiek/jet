package com.reconinstruments.ss1.test;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.SPPM.ConnectionType;
import com.stonestreetone.bluetopiapm.SPPM.DataConfirmationStatus;
import com.stonestreetone.bluetopiapm.SPPM.IDPSState;
import com.stonestreetone.bluetopiapm.SPPM.IDPSStatus;
import com.stonestreetone.bluetopiapm.SPPM.LineStatus;
import com.stonestreetone.bluetopiapm.SPPM.MFiAccessoryInfo;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocolMatchAction;
import com.stonestreetone.bluetopiapm.SPPM.PortStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.ServerEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocol;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;

import java.util.EnumSet;
import java.util.UUID;

public class SPPMManager {
    
    private static final String TAG = SPPMManager.class.getSimpleName();
    
    private MainActivity mActivity;
    private static SPPMManager instance;
    
    private SerialPortClientManager serialPortClientManager;
    private SerialPortServerManager serialPortServerManager;
    private int connectingRoute= 0; // 0:N/A, 1:client, 2:server 

    protected SPPMManager(MainActivity activity) {
        mActivity = activity;
        openServer_Handler.start();
        profileEnable();
        configureMFiSettings_Handler.start();
    }
    
    public static SPPMManager getInstance(MainActivity activity) {
        if(instance == null) {
            instance = new SPPMManager(activity);
        }
        return instance;
    }
    
    public void stop(){
        closeServer_Handler.start();
        profileDisable();
    }

    private void showToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private final ClientEventCallback clientEventCallback = new ClientEventCallback() {

        @Override
        public void dataReceivedEvent(int dataLength) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Data Received").append(":");
            sb.append(" Data Length: ").append(dataLength);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void disconnectedEvent() {
            
            //set connectingRoute to unknown
            connectingRoute = 0;
            
            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Disconnected");

            Log.d(TAG, sb.toString());
            showToast(sb.toString());
        }

        @Override
        public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("IDPS Status").append(":");
            sb.append("\n  State: " + idpsState.name());
            sb.append("\n  Status: " + idpsStatus.name());

            Log.d(TAG, sb.toString());
        }

        @Override
        public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Line Status Changed").append(":");

            if(lineStatus.isEmpty()) {
                sb.append("  No errors");
            } else {
                if(lineStatus.contains(LineStatus.OVERRUN_ERROR)) {
                    sb.append("  OVERRUN_ERROR");
                }
                if(lineStatus.contains(LineStatus.PARITY_ERROR)) {
                    sb.append("  PARITY_ERROR");
                }
                if(lineStatus.contains(LineStatus.FRAMING_ERROR)) {
                    sb.append("  FRAMING_ERROR");
                }
            }

            Log.d(TAG, sb.toString());
        }

        @Override
        public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Non-Session Data Confirmation").append(":");
            sb.append("  Packet ID:      ").append(packetID);
            sb.append("  Transaction ID: ").append(transactionID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            Log.d(TAG, sb.toString());
        }

        @Override
        public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Non-Session Data Received").append(":");
            sb.append("  Lingo ID:    ").append(lingoID);
            sb.append("  Command ID:  ").append(commandID);
            sb.append("  Data Length: ").append(data.length);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Port Status Changed").append(":");
            sb.append("  Enabled flags:");

            if(portStatus.isEmpty()) {
                sb.append("    NONE");
            } else {
                if(portStatus.contains(PortStatus.RTS_CTS)) {
                    sb.append("    RTS_CTS");
                }
                if(portStatus.contains(PortStatus.DTR_DSR)) {
                    sb.append("    DTR_DSR");
                }
                if(portStatus.contains(PortStatus.RING_INDICATOR)) {
                    sb.append("    RING_INDICATOR");
                }
                if(portStatus.contains(PortStatus.CARRIER_DETECT)) {
                    sb.append("    CARRIER_DETECT");
                }
            }

            sb.append("  Break Signal:  ").append((breakSignal == true) ? "Enabled" : "Disabled");
            sb.append("  Break Timeout: ").append(breakTimeout);

            Log.d(TAG, sb.toString());
            showToast(sb.toString());
        }

        @Override
        public void sessionCloseEvent(int sessionID) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Session Closed").append(":");
            sb.append("  Session ID: ").append(sessionID);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Session Data Confirmation").append(":");
            sb.append("  Session ID: ").append(sessionID);
            sb.append("  Packet ID:  ").append(packetID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            Log.d(TAG, sb.toString());
        }

        @Override
        public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Session Data Received").append(":");
            sb.append("  Session ID:  ").append(sessionID);
            sb.append("  Data Length: ").append(sessionData.length);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Session Open Request").append(":");
            sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
            sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
            sb.append("  Session ID:              ").append(sessionID);
            sb.append("  Protocol Index:          ").append(protocolIndex);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void transmitBufferEmptyEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Transmit Buffer Empty");

            Log.d(TAG, sb.toString());
        }

        @Override
        public void connectionStatusEvent(ConnectionStatus status, ConnectionType type) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append("Connection Status").append(":");

            sb.append("  Connection Status: ");
            switch(status) {
            case SUCCESS:
                sb.append("SUCCESS");
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
                sb.append("FAILURE_UNKNOWN");
                break;
            }

            sb.append("  Connection Type: ");
            switch(type) {
            case SPP:
                sb.append("SPP");
                break;
            case MFI:
                sb.append("MFi");
                break;
            }
            
            //set connectingRoute to client
            if(status == ConnectionStatus.SUCCESS && type == ConnectionType.MFI){
                connectingRoute = 1;
            }
            
            Log.d(TAG, sb.toString());
        }

    };

    private final ServerEventCallback serverEventCallback = new ServerEventCallback() {

        @Override
        public void dataReceivedEvent(int dataLength) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Data Received").append(":");
            sb.append(" Data Length: ").append(dataLength);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void disconnectedEvent() {

            //set connectingRoute to unknown
            connectingRoute = 0;
            
            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Disconnected");

            Log.d(TAG, sb.toString());
            showToast(sb.toString());
        }

        @Override
        public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("IDPS Status").append(":");

            switch(idpsState) {
            case START_IDENTIFICATION_REQUEST:
                sb.append("  State: START_IDENTIFICATION_REQUEST");
                break;
            case START_IDENTIFICATION_PROCESS:
                sb.append("  State: START_IDENTIFICATION_PROCESS");
                break;
            case IDENTIFICATION_PROCESS:
                sb.append("  State: IDENTIFICATION_PROCESS");
                break;
            case IDENTIFICATION_PROCESS_COMPLETE:
                sb.append("  State: IDENTIFICATION_PROCESS_COMPLETE");
                break;
            case START_AUTHENTICATION_PROCESS:
                sb.append("  State: START_AUTHENTICATION_PROCESS");
                break;
            case AUTHENTICATION_PROCESS:
                sb.append("  State: AUTHENTICATION_PROCESS");
                break;
            case AUTHENTICATION_PROCESS_COMPLETE:
                sb.append("  State: AUTHENTICATION_PROCESS_COMPLETE");
                break;
            }

            switch(idpsStatus) {
            case SUCCESS:
                sb.append("  Status: SUCCESS");
                break;
            case ERROR_RETRYING:
                sb.append("  Status: ERROR_RETRYING");
                break;
            case TIMEOUT_HALTING:
                sb.append("  Status: TIMEOUT_HALTING");
                break;
            case GENERAL_FAILURE:
                sb.append("  Status: GENERAL_FAILURE");
                break;
            case PROCESS_FAILURE:
                sb.append("  Status: PROCESS_FAILURE");
                break;
            case PROCESS_TIMEOUT_RETRYING:
                sb.append("  Status: PROCESS_TIMEOUT_RETRYING");
                break;
            }

            Log.d(TAG, sb.toString());
            showToast(sb.toString());
        }

        @Override
        public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Line Status Changed").append(":");

            if(lineStatus.isEmpty()) {
                sb.append("  No errors");
            } else {
                if(lineStatus.contains(LineStatus.OVERRUN_ERROR)) {
                    sb.append("  OVERRUN_ERROR");
                }
                if(lineStatus.contains(LineStatus.PARITY_ERROR)) {
                    sb.append("  PARITY_ERROR");
                }
                if(lineStatus.contains(LineStatus.FRAMING_ERROR)) {
                    sb.append("  FRAMING_ERROR");
                }
            }

            Log.d(TAG, sb.toString());
        }

        @Override
        public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Non-Session Data Confirmation").append(":");
            sb.append("  Packet ID:      ").append(packetID);
            sb.append("  Transaction ID: ").append(transactionID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            Log.d(TAG, sb.toString());
        }

        @Override
        public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Non-Session Data Received").append(":");
            sb.append("  Lingo ID:    ").append(lingoID);
            sb.append("  Command ID:  ").append(commandID);
            sb.append("  Data Length: ").append(data.length);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Port Status Changed").append(":");
            sb.append("  Enabled flags:");

            if(portStatus.isEmpty()) {
                sb.append("    NONE");
            } else {
                if(portStatus.contains(PortStatus.RTS_CTS)) {
                    sb.append("    RTS_CTS");
                }
                if(portStatus.contains(PortStatus.DTR_DSR)) {
                    sb.append("    DTR_DSR");
                }
                if(portStatus.contains(PortStatus.RING_INDICATOR)) {
                    sb.append("    RING_INDICATOR");
                }
                if(portStatus.contains(PortStatus.CARRIER_DETECT)) {
                    sb.append("    CARRIER_DETECT");
                }
            }

            sb.append("  Break Signal:  ").append((breakSignal == true) ? "Enabled" : "Disabled");
            sb.append("  Break Timeout: ").append(breakTimeout);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void sessionCloseEvent(int sessionID) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Session Closed").append(":");
            sb.append("  Session ID: ").append(sessionID);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Session Data Confirmation").append(":");
            sb.append("  Session ID: ").append(sessionID);
            sb.append("  Packet ID:  ").append(packetID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            Log.d(TAG, sb.toString());
        }

        @Override
        public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Session Data Received").append(":");
            sb.append("  Session ID:  ").append(sessionID);
            sb.append("  Data Length: ").append(sessionData.length);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Session Open Request").append(":");
            sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
            sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
            sb.append("  Session ID:              ").append(sessionID);
            sb.append("  Protocol Index:          ").append(protocolIndex);

            Log.d(TAG, sb.toString());
        }

        @Override
        public void transmitBufferEmptyEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Transmit Buffer Empty");

            Log.d(TAG, sb.toString());
        }

        @Override
        public void connectedEvent(BluetoothAddress remoteDeviceAddress, ConnectionType type) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Connected");
            sb.append("  Address: ").append(remoteDeviceAddress.toString());

            sb.append("  Type:    ");
            switch(type) {
            case SPP:
                sb.append("SPP");
                break;
            case MFI:
                sb.append("MFi");
                break;
            }

            //set connectingRoute to server, set bluetooth to remote device address
            if(type == ConnectionType.MFI){
                connectingRoute = 2;
                mActivity.setBTAddress(remoteDeviceAddress.toString());
            }

            Log.d(TAG, sb.toString());
        }

        @Override
        public void connectionRequestEvent(BluetoothAddress remoteDeviceAddress) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append("Connection Request");
            sb.append("  Address: ").append(remoteDeviceAddress.toString());

            Log.d(TAG, sb.toString());
        }

    };
    
    private final void profileEnable() {
        synchronized(this) {

            try {
                serialPortClientManager = new SerialPortClientManager(clientEventCallback);
            } catch(Exception e) {
                /*
                 * BluetopiaPM server couldn't be contacted.
                 * This should never happen if Bluetooth was
                 * successfully enabled.
                 */
                showToast("ERROR: Could not connect to the BluetopiaPM service.");
                BluetoothAdapter.getDefaultAdapter().disable();
            }

        }
    }

    private final void profileDisable() {
        synchronized(mActivity) {
            if(serialPortClientManager != null) {
                serialPortClientManager.dispose();
                serialPortClientManager = null;
            }

            if(serialPortServerManager != null) {
                serialPortServerManager.dispose();
                serialPortServerManager = null;
            }
        }
    }

    public final Thread configureMFiSettings_Handler = new Thread() {

        @Override
        public void run() {

            int  result;

            if(serialPortClientManager != null) {
                result = serialPortClientManager
                        .configureMFiSettings(
                                2060,
                                800,
                                null,
                                new MFiAccessoryInfo(
                                        SPPM.MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP,
                                        // "Recon Jet",
                                        BluetoothAdapter.getDefaultAdapter().getName(),
                                        0x00010000,
                                        0x00010000,
                                        "Recon Instruments",
                                        "Jet",
                                        "40984E5CCE15",
                                        (SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1
                                                | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3)),
                                                new MFiProtocol[] {
                                                        new MFiProtocol("com.reconinstruments.command", MFiProtocolMatchAction.NONE),
                                                        new MFiProtocol("com.reconinstruments.object", MFiProtocolMatchAction.NONE),
                                                        new MFiProtocol("com.reconinstruments.file", MFiProtocolMatchAction.NONE)}, 
                                "12345ABCDE", null, "en", null);

                Log.d(TAG, "configureMFiSettings() client result: " + result);
            }
        }
    };

    public final Thread openServer_Handler = new Thread() {

        @Override
        public void run() {
            int                              portNumber = 5;
            UUID[]                           serviceClasses = null;
            String                           serviceName    = null;
            EnumSet<IncomingConnectionFlags> flags;

            if(serialPortServerManager == null) {
                flags          = EnumSet.noneOf(IncomingConnectionFlags.class);

                //flags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                flags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                flags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
                //flags.add(IncomingConnectionFlags.MFI_ALLOWED);
                flags.add(IncomingConnectionFlags.MFI_REQUIRED);

                try {
                    if((serviceClasses != null) || (serviceName != null)) {
                        serialPortServerManager = new SerialPortServerManager(serverEventCallback, portNumber, flags, serviceClasses, null, serviceName);
                    } else {
                        serialPortServerManager = new SerialPortServerManager(serverEventCallback, portNumber, flags);
                    }
                    Log.d(TAG, "Open Server result: Success");
                } catch(ServerNotReachableException e) {
                    Log.w(TAG, "Open Server result: Unable to communicate with Platform Manager service");
                } catch(BluetopiaPMException e) {
                    Log.w(TAG, "Open Server result: Unable to register server port (already in use?)");
                }
            } else {
                Log.d(TAG, "Open Server result: The sample already has an active SPP server manager");
            }
        }
    };

    public final Thread closeServer_Handler = new Thread() {

        @Override
        public void run() {
            if(serialPortServerManager != null) {
                serialPortServerManager.dispose();
                serialPortServerManager = null;

                Log.d(TAG, "Close Server result: Success");
            } else {
                Log.d(TAG, "Close Server result: No server is currently active.");
            }
        }
    };

    public void connectRemoteDevice() {
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "connect remote device: " + mActivity.getBTAddress());
                int result;
                BluetoothAddress bluetoothAddress = new BluetoothAddress(mActivity.getBTAddress());
                int portNumber = 1;
                EnumSet<ConnectionFlags> connectionFlags;
                boolean waitForConnection = false;
    
                if(serialPortClientManager != null) {
                    connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
    
                    connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                    connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
                    connectionFlags.add(ConnectionFlags.MFI_REQUIRED);
    
                    result = serialPortClientManager.connectRemoteDevice(bluetoothAddress, portNumber, connectionFlags, waitForConnection);
    
                    Log.d(TAG, "connectRemoteDevice() result: " + result);
                    showToast("connectRemoteDevice() result: " + result);
                } else {
                    Log.d(TAG, "connectRemoteDevice() result: The Client Manager is not initialized");
                }
            }
        }.start();
    }
    
    public void disconnectRemoteDevice() {
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "disconnect remote device: " + mActivity.getBTAddress());
                int                result;
                int                flushTimeoutInteger = 5000;
                SPPM               manager;
    
                if(connectingRoute == 1) {
                    manager = serialPortClientManager;
                } else if (connectingRoute == 2){
                    manager = serialPortServerManager;
                } else {
                    manager = null;
                    Log.w(TAG, "connectingRoute is unknown");
                }
    
                if(manager != null) {
    
                    result = -1;
    
                    result = manager.disconnectRemoteDevice(flushTimeoutInteger);
    
                    Log.d(TAG, "disconnectRemoteDevice() result: " + result);
                    showToast("disconnectRemoteDevice() result: " + result);
                } else {
                    Log.d(TAG, "disconnectRemoteDevice() result: The selected Manager is not initialized");
                }
            }
        }.start();
    }
}
