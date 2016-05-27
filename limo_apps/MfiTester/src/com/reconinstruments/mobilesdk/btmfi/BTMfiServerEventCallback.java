package com.reconinstruments.mobilesdk.btmfi;

import java.util.EnumSet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.SPPM.ConnectionType;
import com.stonestreetone.bluetopiapm.SPPM.DataConfirmationStatus;
import com.stonestreetone.bluetopiapm.SPPM.IDPSState;
import com.stonestreetone.bluetopiapm.SPPM.IDPSStatus;
import com.stonestreetone.bluetopiapm.SPPM.LineStatus;
import com.stonestreetone.bluetopiapm.SPPM.PortStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.ServerEventCallback;

public class BTMfiServerEventCallback implements ServerEventCallback {
	private static final String TAG = "BTMfiServerEventCallback";
	private Context mContext;
	private BTMfiSessionManager mManager;

	private int commandSessionId = 0;
	private int objectSessionId = 0;
	private int fileSessionId = 0;

	public BTMfiServerEventCallback(Context context, BTMfiSessionManager manager) {
		mContext = context;
		mManager = manager;
	}

	@Override
	public void dataReceivedEvent(int dataLength) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Data Received").append(":");
		sb.append(" Data Length: ").append(dataLength);
		Log.i(TAG, sb.toString());
	}

	@Override
	public void disconnectedEvent() {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Disconnected");
		Log.i(TAG, sb.toString());
		showToast(sb.toString());
	}

	@Override
	public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("IDPS Status").append(":");
		switch (idpsState) {
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
		switch (idpsStatus) {
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
		Log.i(TAG, sb.toString());
	}

	@Override
	public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Line Status Changed").append(":");
		if (lineStatus.isEmpty()) {
			sb.append("  No errors");
		} else {
			if (lineStatus.contains(LineStatus.OVERRUN_ERROR)) {
				sb.append("  OVERRUN_ERROR");
			}
			if (lineStatus.contains(LineStatus.PARITY_ERROR)) {
				sb.append("  PARITY_ERROR");
			}
			if (lineStatus.contains(LineStatus.FRAMING_ERROR)) {
				sb.append("  FRAMING_ERROR");
			}
		}
		Log.i(TAG, sb.toString());

	}

	@Override
	public void nonSessionDataConfirmationEvent(int packetID,
			int transactionID, DataConfirmationStatus status) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Non-Session Data Confirmation")
				.append(":");
		sb.append("  Packet ID:      ").append(packetID);
		sb.append("  Transaction ID: ").append(transactionID);
		sb.append("  Status:     ").append(
				(status != null ? status.name() : "null"));
		Log.i(TAG, sb.toString());
	}

	@Override
	public void nonSessionDataReceivedEvent(int lingoID, int commandID,
			byte[] data) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Non-Session Data Received").append(":");
		sb.append("  Lingo ID:    ").append(lingoID);
		sb.append("  Command ID:  ").append(commandID);
		sb.append("  Data Length: ").append(data.length);
		Log.d(TAG, sb.toString());
	}

	@Override
	public void portStatusChangedEvent(EnumSet<PortStatus> portStatus,
			boolean breakSignal, int breakTimeout) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Port Status Changed").append(":");
		sb.append("  Enabled flags:");
		if (portStatus.isEmpty()) {
			sb.append("    NONE");
		} else {
			if (portStatus.contains(PortStatus.RTS_CTS)) {
				sb.append("    RTS_CTS");
			}
			if (portStatus.contains(PortStatus.DTR_DSR)) {
				sb.append("    DTR_DSR");
			}
			if (portStatus.contains(PortStatus.RING_INDICATOR)) {
				sb.append("    RING_INDICATOR");
			}
			if (portStatus.contains(PortStatus.CARRIER_DETECT)) {
				sb.append("    CARRIER_DETECT");
			}
		}
		sb.append("  Break Signal:  ").append(
				(breakSignal == true) ? "Enabled" : "Disabled");
		sb.append("  Break Timeout: ").append(breakTimeout);
		Log.i(TAG, sb.toString());
	}

	@Override
	public void sessionCloseEvent(int sessionID) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Session Closed").append(":");
		sb.append("  Session ID: ").append(sessionID);
		Log.i(TAG, sb.toString());
		showToast(sb.toString());

		commandSessionId = sessionID;
		objectSessionId = sessionID;
		fileSessionId = sessionID;
		Log.i(TAG, "commandSessionId =" + sessionID);
		Log.i(TAG, "objectSessionId =" + sessionID);
		Log.i(TAG, "fileSessionId =" + sessionID);
	}

	@Override
	public void sessionDataConfirmationEvent(int sessionID, int packetID,
			DataConfirmationStatus status) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Session Data Confirmation").append(":");
		sb.append("  Session ID: ").append(sessionID);
		sb.append("  Packet ID:  ").append(packetID);
		sb.append("  Status:     ").append(
				(status != null ? status.name() : "null"));
		Log.i(TAG, sb.toString());
		showToast(sb.toString());
	}

	@Override
	public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Session Data Received").append(":");
		sb.append("  Session ID:  ").append(sessionID);
		sb.append("  Data Length: ").append(sessionData.length);
		Log.i(TAG, sb.toString());
		Log.i(TAG, "sessionData=" + new String(sessionData));

		mManager.receiveData(sessionData, sessionData.length);
	}

	@Override
	public void sessionOpenRequestEvent(int maximumTransmitPacket,
			int maximumReceivePacket, int sessionID, int protocolIndex) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Session Open Request").append(":");
		sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
		sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
		sb.append("  Session ID:              ").append(sessionID);
		sb.append("  Protocol Index:          ").append(protocolIndex);
		Log.i(TAG, sb.toString());
		showToast(sb.toString());

		Log.i(TAG, "openSessionRequestResponse(true, sessionID, true)");
		BTMfiSessionManager.getInstance(mContext).openSessionRequestResponse(
				true, sessionID, true);

		if (commandSessionId == 0) {
			commandSessionId = sessionID;
			Log.i(TAG, "commandSessionId =" + sessionID);
		} else if (objectSessionId == 0) {
			objectSessionId = sessionID;
			Log.i(TAG, "objectSessionId =" + sessionID);
		} else {
			fileSessionId = sessionID;
			Log.i(TAG, "fileSessionId =" + sessionID);
		}
	}

	@Override
	public void transmitBufferEmptyEvent() {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Transmit Buffer Empty");
		Log.i(TAG, sb.toString());
	}

	@Override
	public void connectedEvent(BluetoothAddress remoteDeviceAddress,
			ConnectionType type) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Connected");
		sb.append("  Address: ").append(remoteDeviceAddress.toString());
		sb.append("  Type:    ");
		switch (type) {
		case SPP:
			sb.append("SPP");
			break;
		case MFI:
			sb.append("MFi");
			break;
		}
		Log.i(TAG, sb.toString());
		showToast(sb.toString());
	}

	@Override
	public void connectionRequestEvent(BluetoothAddress remoteDeviceAddress) {
		StringBuilder sb = new StringBuilder();
		sb.append("SERVER: ").append("Connection Request");
		sb.append("  Address: ").append(remoteDeviceAddress.toString());
		Log.i(TAG, sb.toString());
		showToast(sb.toString());
	}

	private void showToast(final String message) {
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			public void run() {
				Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void unhandledControlMessageReceivedEvent(short arg0,byte[] arg1) {
		// TODO Auto-generated method stub
	}
}
