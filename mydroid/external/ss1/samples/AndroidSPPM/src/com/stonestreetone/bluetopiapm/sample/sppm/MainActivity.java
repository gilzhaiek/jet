/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Serial Port Profile Sample for Stonestreet One Bluetooth Protocol Stack
 * Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.sppm;

import java.util.EnumSet;
import java.util.UUID;

import android.text.InputType;
import android.util.Log;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.SPPM.ConnectionType;
import com.stonestreetone.bluetopiapm.SPPM.DataConfirmationStatus;
import com.stonestreetone.bluetopiapm.SPPM.IDPSState;
import com.stonestreetone.bluetopiapm.SPPM.IDPSStatus;
import com.stonestreetone.bluetopiapm.SPPM.LineStatus;
import com.stonestreetone.bluetopiapm.SPPM.MFiAccessoryInfo;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocol;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocolMatchAction;
import com.stonestreetone.bluetopiapm.SPPM.PortStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.ServerEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.ServiceRecordInformation;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG = "SPPM_Sample";

    /*package*/ SerialPortClientManager serialPortClientManager;
    /*package*/ SerialPortServerManager serialPortServerManager;

    @Override
    protected boolean profileEnable() {
        synchronized(this) {

            try {
                serialPortClientManager = new SerialPortClientManager(clientEventCallback);
                return true;
            } catch(Exception e) {
                /*
                 * BluetopiaPM server couldn't be contacted.
                 * This should never happen if Bluetooth was
                 * successfully enabled.
                 */
                showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                return false;
            }

        }
    }

    @Override
    protected void profileDisable() {
        synchronized(MainActivity.this) {
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

    @Override
    protected Command[] getCommandList() {
        return commandList;
    }

    @Override
    protected int getNumberProfileParameters() {
        return 0;
    }

    @Override
    protected int getNumberCommandParameters() {
        return 4;
    }

    private final ClientEventCallback clientEventCallback = new ClientEventCallback() {

        @Override
        public void dataReceivedEvent(int dataLength) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.dataReceivedEventLabel)).append(":");
            sb.append(" Data Length: ").append(dataLength);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void disconnectedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.disconnectedEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.idpsStatusEventLabel)).append(":");
            sb.append("\n  State: " + idpsState.name());
            sb.append("\n  Status: " + idpsStatus.name());

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.lineStatusChangedEventLabel)).append(":");

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

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.nonSessionDataConfirmationEventLabel)).append(":");
            sb.append("  Packet ID:      ").append(packetID);
            sb.append("  Transaction ID: ").append(transactionID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.nonSessionDataReceivedEventLabel)).append(":");
            sb.append("  Lingo ID:    ").append(lingoID);
            sb.append("  Command ID:  ").append(commandID);
            sb.append("  Data Length: ").append(data.length);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void unhandledControlMessageReceivedEvent(short controlMessageID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.unhandledControlMessageEventLabel)).append(":");
            sb.append("  Control Messasge ID: ").append(controlMessageID);
            sb.append("  Data Length:         ").append(data.length);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.portStatusChangedEventLabel)).append(":");
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

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionCloseEvent(int sessionID) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionCloseEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionDataConfirmationEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);
            sb.append("  Packet ID:  ").append(packetID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionDataReceivedEventLabel)).append(":");
            sb.append("  Session ID:  ").append(sessionID);
            sb.append("  Data Length: ").append(sessionData.length);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.sessionOpenRequestEventLabel)).append(":");
            sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
            sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
            sb.append("  Session ID:              ").append(sessionID);
            sb.append("  Protocol Index:          ").append(protocolIndex);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void transmitBufferEmptyEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.transmitBufferEmptyEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void connectionStatusEvent(ConnectionStatus status, ConnectionType type) {

            StringBuilder sb = new StringBuilder();

            sb.append("CLIENT: ").append(resourceManager.getString(R.string.connectionStatusEventLabel)).append(":");

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

            displayMessage("");
            displayMessage(sb);

        }

    };

    private final ServerEventCallback serverEventCallback = new ServerEventCallback() {

        @Override
        public void dataReceivedEvent(int dataLength) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.dataReceivedEventLabel)).append(":");
            sb.append(" Data Length: ").append(dataLength);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void disconnectedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.disconnectedEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.idpsStatusEventLabel)).append(":");

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

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.lineStatusChangedEventLabel)).append(":");

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

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.nonSessionDataConfirmationEventLabel)).append(":");
            sb.append("  Packet ID:      ").append(packetID);
            sb.append("  Transaction ID: ").append(transactionID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.nonSessionDataReceivedEventLabel)).append(":");
            sb.append("  Lingo ID:    ").append(lingoID);
            sb.append("  Command ID:  ").append(commandID);
            sb.append("  Data Length: ").append(data.length);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void unhandledControlMessageReceivedEvent(short controlMessageID, byte[] data) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.unhandledControlMessageEventLabel)).append(":");
            sb.append("  Control Messasge ID: ").append(controlMessageID);
            sb.append("  Data Length:         ").append(data.length);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.portStatusChangedEventLabel)).append(":");
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

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionCloseEvent(int sessionID) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionCloseEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionDataConfirmationEventLabel)).append(":");
            sb.append("  Session ID: ").append(sessionID);
            sb.append("  Packet ID:  ").append(packetID);
            sb.append("  Status:     ").append((status != null ? status.name() : "null"));

            displayMessage("");
            displayMessage(sb);


        }

        @Override
        public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionDataReceivedEventLabel)).append(":");
            sb.append("  Session ID:  ").append(sessionID);
            sb.append("  Data Length: ").append(sessionData.length);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.sessionOpenRequestEventLabel)).append(":");
            sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
            sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
            sb.append("  Session ID:              ").append(sessionID);
            sb.append("  Protocol Index:          ").append(protocolIndex);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void transmitBufferEmptyEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.transmitBufferEmptyEventLabel));

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void connectedEvent(BluetoothAddress remoteDeviceAddress, ConnectionType type) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.connectedEventLabel));
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

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void connectionRequestEvent(BluetoothAddress remoteDeviceAddress) {

            StringBuilder sb = new StringBuilder();

            sb.append("SERVER: ").append(resourceManager.getString(R.string.connectionRequestEventLabel));
            sb.append("  Address: ").append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

        }

    };

    private final CommandHandler disconnectRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                result;
            TextValue          flushTimeoutParameter;
            int                flushTimeoutInteger;
            SPPM               manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {

                flushTimeoutParameter = getCommandParameterView(1).getValueText();

                try {
                    flushTimeoutInteger = Integer.parseInt(flushTimeoutParameter.text.toString(),16);
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = -1;

                result = manager.disconnectRemoteDevice(flushTimeoutInteger);

                displayMessage("");
                displayMessage("disconnectRemoteDevice() result: " + result);

            } else {
                displayMessage("");
                displayMessage("disconnectRemoteDevice() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeText("", "Flush Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler readData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int    result;
            byte[] dataBuffer;
            String dataString = null;
            SPPM   manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null)
            {
                // TODO need to add support for testing readData overload for partial buffers

                dataBuffer = new byte[1024];
                result = manager.readData(dataBuffer, SPPM.TIMEOUT_IMMEDIATE);

                displayMessage("");
                displayMessage("readData() result: " + result);

                for (int index=0;index < result; index++)
                {

                    dataString = dataString + (char)dataBuffer[index];

                }

                if (dataString != null)
                {

                    displayMessage(dataString);

                }

            }
            else
            {
                displayMessage("");
                displayMessage("readData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler writeData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int    result;
            SPPM   manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                result = manager.writeData("This is an SPP data test message".getBytes(), 5000);

                displayMessage("");
                displayMessage("writeData() result: " + result);
            } else {
                displayMessage("");
                displayMessage("writeData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler writeDataArbitrary_Handler = new CommandHandler() {

        String[] specialCharacters =
        {
            "Carriage Return(^M)",
            "Substitute(^Z)",
            "None"
        };

        @Override
        public void run()
        {
            int result;
            SPPM manager;

            SpinnerValue specialCharacterParameter;
            String arbitraryDataString = null;

            TextValue writeTimeoutParameter;
            int writeTimeout;

            if(getCommandParameterView(3).getValueCheckbox().value)
            {

                manager = serialPortClientManager;

            }
            else
            {

                manager = serialPortServerManager;

            }

            if(manager != null)
            {

                specialCharacterParameter = getCommandParameterView(1).getValueSpinner();

                switch (specialCharacterParameter.selectedItem)
                {

                    case 0:

                        arbitraryDataString = getCommandParameterView(0).getValueText().text.toString() + (char)13;
                        break;

                    case 1:

                        arbitraryDataString = getCommandParameterView(0).getValueText().text.toString() + (char)26;
                        break;

                    case 2:

                        arbitraryDataString = getCommandParameterView(0).getValueText().text.toString();
                        break;


                }

                writeTimeoutParameter = getCommandParameterView(2).getValueText();

                try
                {

                    writeTimeout = Integer.valueOf(writeTimeoutParameter.text.toString());

                }
                catch(NumberFormatException e)
                {

                    // TODO complain
                    return;

                }

                result = manager.writeData(arbitraryDataString.getBytes(), writeTimeout);

                displayMessage("");
                displayMessage("writeData() result: " + result);

            }
            else
            {

                displayMessage("");
                displayMessage("writeData() result: The selected Manager is not initialized");

            }

        }

        @Override
        public void selected()
        {
            getCommandParameterView(0).setModeText("", "Arbitrary Data");
            getCommandParameterView(1).setModeSpinner("Terminate String with Special Character", specialCharacters);
            getCommandParameterView(2).setModeText("5000", "Write Timeout");
            getCommandParameterView(3).setModeCheckbox("Client (uncheck for Server)", true);
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendLineStatus_Handler = new CommandHandler() {

        String[] lineStatuses = {
                "Overrun Error",
                "Parity Error",
                "Framing Error",
        };

        boolean[] lineStatusValues = new boolean[] {false, false, false};

        @Override
        public void run() {
            ChecklistValue           lineStatusParameter;
            EnumSet<SPPM.LineStatus> lineStatus;
            int                      result;
            SPPM                     manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                lineStatusParameter = getCommandParameterView(1).getValueChecklist();

                lineStatus = EnumSet.noneOf(SPPM.LineStatus.class);

                if(lineStatusParameter.checkedItems[0]) {
                    lineStatus.add(LineStatus.OVERRUN_ERROR);
                }
                if(lineStatusParameter.checkedItems[1]) {
                    lineStatus.add(LineStatus.PARITY_ERROR);
                }
                if(lineStatusParameter.checkedItems[2]) {
                    lineStatus.add(LineStatus.FRAMING_ERROR);
                }

                result = manager.sendLineStatus(lineStatus);

                displayMessage("");
                displayMessage("sendLineStatus() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendLineStatus() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeChecklist("Line Statuses", lineStatuses, lineStatusValues);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendPortStatus_Handler = new CommandHandler() {

        String[] portStatuses = {
                "RTS/CTS",
                "DTR/DSR",
                "Ring Indicator",
                "Carrier Detect",
        };

        boolean[] portStatusValues = new boolean[] {false, false, false, false};

        @Override
        public void run() {
            ChecklistValue           portStatusParameter;
            EnumSet<SPPM.PortStatus> portStatus;
            boolean                  breakSignal;
            int                      breakTimeout;
            int                      result;
            SPPM                     manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                portStatusParameter = getCommandParameterView(1).getValueChecklist();

                portStatus = EnumSet.noneOf(SPPM.PortStatus.class);

                if(portStatusParameter.checkedItems[0]) {
                    portStatus.add(PortStatus.RTS_CTS);
                }
                if(portStatusParameter.checkedItems[1]) {
                    portStatus.add(PortStatus.DTR_DSR);
                }
                if(portStatusParameter.checkedItems[2]) {
                    portStatus.add(PortStatus.RING_INDICATOR);
                }
                if(portStatusParameter.checkedItems[3]) {
                    portStatus.add(PortStatus.CARRIER_DETECT);
                }

                breakSignal = getCommandParameterView(1).getValueCheckbox().value;

                try {
                    breakTimeout = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.sendPortStatus(portStatus, breakSignal, breakTimeout);

                displayMessage("");
                displayMessage("sendPortStatus() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendPortStatus() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeChecklist("Port Statuses", portStatuses, portStatusValues);
            getCommandParameterView(2).setModeCheckbox("Break Signal:", false);
            getCommandParameterView(3).setModeText("", "Break Timeout");
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler queryRemoteDeviceServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            ServiceRecordInformation[] serviceRecordInformation;

            BluetoothAddress bluetoothAddress;

            if(serialPortClientManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                serviceRecordInformation = serialPortClientManager.queryRemoteDeviceServices(bluetoothAddress);

                displayMessage("");

                if (serviceRecordInformation != null) {
                    displayMessage("queryRemoteDeviceServices():");

                    for(ServiceRecordInformation serviceRecord: serviceRecordInformation) {
                        displayMessage("");
                        displayMessage("Service Record Handle     : " + serviceRecord.serviceRecordHandle);
                        displayMessage("Service Class             : " + serviceRecord.serviceClassID.toString());
                        displayMessage("Service Name              : " + serviceRecord.serviceName);
                        displayMessage("Service RFCOMM Port Number: " + serviceRecord.rfcommPortNumber);
                    }
                } else {
                    displayMessage("queryRemoteDeviceServices(): returned null");
                }

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler configureMFiSettings_Handler = new CommandHandler() {

        @Override
        public void run() {

            int  result;

            if(serialPortClientManager != null) {
                result = serialPortClientManager.configureMFiSettings(
                        /* maximumReceivePacketSize */  65535,
                        /* dataPacketTimeout */         800,
                        /* supportedLingoes */          null,
                        /* accessoryInformation */      new MFiAccessoryInfo(
                                /* capabilitiesBitmask */       SPPM.MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP,
                                /* name */                      "Apple Accessory",
                                /* firmwareVersion */           0x00010000,
                                /* hardwareVersion */           0x00010000,
                                /* manufacturer */              "Stonestreet One",
                                /* modelNumber */               "BluetopiaPM",
                                /* serialNumber */              "SN:012346",
                                /* rfCertification */           (SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3)),
                        /* supportedProtocols */        new MFiProtocol[] {
                                                                new MFiProtocol("com.stonestreetone.demo", MFiProtocolMatchAction.NONE),
                                                                new MFiProtocol("com.stonestreetone.test", MFiProtocolMatchAction.NONE),
                                                                new MFiProtocol("com.stonestreetone.ses0", MFiProtocolMatchAction.NONE),
                                                                new MFiProtocol("com.stonestreetone.ses1", MFiProtocolMatchAction.NONE)},
                        /* bundleSeedID */              "12345ABCDE",
                        /* fidTokens */                 null,
                        /* currentLanguage */           "en",
                        /* supportedLanguages */        null,
                        /* controlMessageIDsSent */     new short[] {
                                                            (short)0xEA02,  /* RequestAppLaunch */

                                                            /* Messages useful for testing SendControlMessage API. */
                                                            (short)0x5700,  /* RequestWiFiInformation */
                                                        },
                        /* controlMessageIDsReceived */ new short[] {
                                                            (short)0xEA00,  /* StartExternalAccessoryProtocolSession */
                                                            (short)0xEA01,  /* StopExternalAccessoryProtocolSession */

                                                            /* Messages useful for testing SendControlMessage API. */
                                                            (short)0x5701,  /* WiFiInformation */
                                                        });
                displayMessage("");
                displayMessage("configureMFiSettings() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler queryConnectionType_Handler = new CommandHandler() {

        @Override
        public void run() {
            ConnectionType type;
            SPPM           manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                type = manager.queryConnectionType();

                displayMessage("");

                if(type != null) {
                    switch(type) {
                    case SPP:
                        displayMessage("queryConnectionType() result: SPP");
                        break;
                    case MFI:
                        displayMessage("queryConnectionType() result: MFi");
                        break;
                    }
                } else {
                    displayMessage("queryConnectionType() result: Not currently connected");
                }
            } else {
                displayMessage("");
                displayMessage("queryConnectionType() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler openSessionRequestResponse_Handler = new CommandHandler() {

        @Override
        public void run() {
            int       result;
            int       sessionID;
            boolean   accept;
            SPPM               manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                try {
                    sessionID = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                accept = getCommandParameterView(2).getValueCheckbox().value;

                result = manager.openSessionRequestResponse(sessionID, accept);

                displayMessage("");
                displayMessage("openSessionRequestResponse() result: " + result);
            } else {
                displayMessage("");
                displayMessage("openSessionRequestResponse() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeText("", "Session ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeCheckbox("Accept?", true);
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendSessionData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int  result;
            int  sessionID;
            SPPM manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                try {
                    sessionID = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.sendSessionData(sessionID, "This is a session data test message".getBytes());

                displayMessage("");
                displayMessage("sendSessionData() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendSessionData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeText("", "Session ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendNonSessionData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int  lingoID;
            int  commandID;
            int  transactionID;
            int  result;
            SPPM manager;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                try {
                    lingoID = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    commandID = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    transactionID = Integer.valueOf(getCommandParameterView(3).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.sendNonSessionData(lingoID, commandID, transactionID, "This is a non-session data test message".getBytes());

                displayMessage("");
                displayMessage("sendNonSessionData() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendNonSessionData() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeText("", "Lingo ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeText("", "Command ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(3).setModeText("", "Transaction ID", InputType.TYPE_CLASS_NUMBER);
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler sendControlMessage_Handler = new CommandHandler() {

        @Override
        public void run() {
            int   result;
            SPPM  manager;
            short messageID;

            if(getCommandParameterView(0).getValueCheckbox().value) {
                manager = serialPortClientManager;
            } else {
                manager = serialPortServerManager;
            }

            if(manager != null) {
                try {
                    messageID = Short.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = manager.sendControlMessage(messageID, null);

                displayMessage("");
                displayMessage("sendControlMessage() result: " + result);
            } else {
                displayMessage("");
                displayMessage("sendControlMessage() result: The selected Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Client: (uncheck for Server)", true);
            getCommandParameterView(1).setModeText("", "Control Message ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
        }

    };

    private final CommandHandler connectRemoteDevice_Handler = new CommandHandler() {

        String[] connectionFlagLabels = {
                "Require Authentication",
                "Require Encryption",
                "MFi Required",
        };

        boolean[] connectionFlagValues = new boolean[] {false, false, false};

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;
            int portNumber;
            EnumSet<ConnectionFlags> connectionFlags;
            ChecklistValue           connectionFlagsParameter;
            boolean waitForConnection;

            if(serialPortClientManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    portNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                connectionFlagsParameter = getCommandParameterView(1).getValueChecklist();
                connectionFlags = EnumSet.noneOf(ConnectionFlags.class);

                if(connectionFlagsParameter.checkedItems[0]) {
                    connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(connectionFlagsParameter.checkedItems[1]) {
                    connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
                }
                if(connectionFlagsParameter.checkedItems[2]) {
                    connectionFlags.add(ConnectionFlags.MFI_REQUIRED);
                }

                waitForConnection = getCommandParameterView(2).getValueCheckbox().value;

                result = serialPortClientManager.connectRemoteDevice(bluetoothAddress, portNumber, connectionFlags, waitForConnection);

                displayMessage("");
                displayMessage("connectRemoteDevice() result: " + result);
            } else {
                displayMessage("");
                displayMessage("connectRemoteDevice() result: The Client Manager is not initialized");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Port number", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            getCommandParameterView(2).setModeCheckbox("Wait for connection:", false);
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {


        }

    };

    private final CommandHandler connectionRequestResponse_Handler = new CommandHandler() {

        @Override
        public void run() {
            int     result;
            boolean accept;

            if(serialPortServerManager != null) {
                accept = getCommandParameterView(0).getValueCheckbox().value;

                result = serialPortServerManager.connectionRequestResponse(accept);

                displayMessage("");
                displayMessage("connectionRequestResponse() result: " + result);
            } else {
                displayMessage("");
                displayMessage("connectionRequestResponse() result: There is not active server port");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Accept?", true);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler openServer_Handler = new CommandHandler() {

        String[] incomingConnectionFlags = {
                "Require Authorization",
                "Require Athentication",
                "Require Encryption",
                "MFI Allowed",
                "MFi Required",
        };

        boolean[] incomingConnectionFlagsValues = new boolean[] {false, false, false, false, false};

        @Override
        public void run() {
            int                              portNumber;
            UUID[]                           serviceClasses = null;
            String                           serviceName    = null;
            ChecklistValue                   flagsParameter;
            EnumSet<IncomingConnectionFlags> flags;

            if(serialPortServerManager == null) {
                try {
                    portNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                flagsParameter = getCommandParameterView(1).getValueChecklist();
                flags          = EnumSet.noneOf(IncomingConnectionFlags.class);

                if(flagsParameter.checkedItems[0]) {
                    flags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                }
                if(flagsParameter.checkedItems[1]) {
                    flags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(flagsParameter.checkedItems[2]) {
                    flags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
                }
                if(flagsParameter.checkedItems[3]) {
                    flags.add(IncomingConnectionFlags.MFI_ALLOWED);
                }
                if(flagsParameter.checkedItems[4]) {
                    flags.add(IncomingConnectionFlags.MFI_REQUIRED);
                }

                // FIXME support custom service class and name
                if(getCommandParameterView(2).getValueText().text.length() > 0) {
                    try {
                        serviceClasses = new UUID[1];

                        String uuidText = getCommandParameterView(2).getValueText().text.toString();

                        if(uuidText.contains("-")) {
                            serviceClasses[0] = UUID.fromString(getCommandParameterView(2).getValueText().text.toString());
                        } else if(uuidText.length() == 32) {
                            serviceClasses[0] = new UUID(Long.parseLong(uuidText.substring(0, 16), 16), Long.parseLong(uuidText.substring(16), 16));
                        } else {
                            serviceClasses = null;
                        }
                    } catch(IllegalArgumentException e) {
                        serviceClasses = null;
                    }

                    if(serviceClasses == null) {
                        displayMessage("UUID is malformed. It should follow the format: 00112233-4455-6677-8899-AABBCCDDEEFF");
                        return;
                    }
                }

                if(getCommandParameterView(3).getValueText().text.length() > 0) {
                    serviceName = getCommandParameterView(3).getValueText().text.toString();
                }

                displayMessage("");

                try {
                    if((serviceClasses != null) || (serviceName != null)) {
                        serialPortServerManager = new SerialPortServerManager(serverEventCallback, portNumber, flags, serviceClasses, null, serviceName);
                    } else {
                        serialPortServerManager = new SerialPortServerManager(serverEventCallback, portNumber, flags);
                    }
                    displayMessage("Open Server result: Success");
                } catch(ServerNotReachableException e) {
                    displayMessage("Open Server result: Unable to communicate with Platform Manager service");
                } catch(BluetopiaPMException e) {
                    displayMessage("Open Server result: Unable to register server port (already in use?)");
                }
            } else {
                displayMessage("");
                displayMessage("Open Server result: The sample already has an active SPP server manager");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Port Number", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeChecklist("Incoming Connection Flags", incomingConnectionFlags, incomingConnectionFlagsValues);
            getCommandParameterView(2).setModeText("", "(Optional) Service Class UUID");
            getCommandParameterView(3).setModeText("", "(Optional) Service Name");
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler closeServer_Handler = new CommandHandler() {

        @Override
        public void run() {
            if(serialPortServerManager != null) {
                serialPortServerManager.dispose();
                serialPortServerManager = null;

                displayMessage("");
                displayMessage("Close Server result: Success");
            } else {
                displayMessage("");
                displayMessage("Close Server result: No server is currently active.");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final Command[] commandList = new Command[] {

            new Command("Connect Remote Device", connectRemoteDevice_Handler),
            new Command("Open Server Port", openServer_Handler),
            new Command("Connection Request Response", connectionRequestResponse_Handler),
            new Command("Disconnect Remote Device", disconnectRemoteDevice_Handler),
            new Command("Close Server Port", closeServer_Handler),
            new Command("Query Connection Type", queryConnectionType_Handler),
            new Command("Read Data", readData_Handler),
            new Command("Write Data", writeData_Handler),
            new Command("Write Arbitrary Data", writeDataArbitrary_Handler),
            new Command("Send Line Status", sendLineStatus_Handler),
            new Command("Send Port Status", sendPortStatus_Handler),
            //xxx
            new Command("Query Remote Device Services", queryRemoteDeviceServices_Handler),
            new Command("(MFi) Configure MFi", configureMFiSettings_Handler),
            new Command("(MFi) Open Session Request Response", openSessionRequestResponse_Handler),
            new Command("(MFi) Send Session Data", sendSessionData_Handler),
            new Command("(MFi) Send Non-Session Data", sendNonSessionData_Handler),
            new Command("(MFi) Send Control Message", sendControlMessage_Handler),
    };
}
