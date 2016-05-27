package com.reconinstruments.mobilesdk.hudconnectivity.bluetooth.mfi;

import java.util.EnumSet;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.mfitester.MfiTesterActivity;
import com.stonestreetone.bluetopiapm.SPPM.ConnectionType;
import com.stonestreetone.bluetopiapm.SPPM.DataConfirmationStatus;
import com.stonestreetone.bluetopiapm.SPPM.IDPSState;
import com.stonestreetone.bluetopiapm.SPPM.IDPSStatus;
import com.stonestreetone.bluetopiapm.SPPM.LineStatus;
import com.stonestreetone.bluetopiapm.SPPM.PortStatus;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionStatus;

public class MfiClientEventCallback implements ClientEventCallback {
	private static final String TAG = "ClientEventCallback";
	private Activity mActivity;

	public MfiClientEventCallback(Activity activity) {
		mActivity = activity;
	}

	@Override
	public void dataReceivedEvent(int dataLength) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Data Received").append(":");
		sb.append(" Data Length: ").append(dataLength);
		Log.d(TAG, sb.toString());
	}

	@Override
	public void disconnectedEvent() {
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
		Log.d(TAG, sb.toString());
	}

	@Override
	public void nonSessionDataConfirmationEvent(int packetID,
			int transactionID, DataConfirmationStatus status) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Non-Session Data Confirmation")
				.append(":");
		sb.append("  Packet ID:      ").append(packetID);
		sb.append("  Transaction ID: ").append(transactionID);
		sb.append("  Status:     ").append(
				(status != null ? status.name() : "null"));
		Log.d(TAG, sb.toString());
	}

	@Override
	public void nonSessionDataReceivedEvent(int lingoID, int commandID,
			byte[] data) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Non-Session Data Received").append(":");
		sb.append("  Lingo ID:    ").append(lingoID);
		sb.append("  Command ID:  ").append(commandID);
		sb.append("  Data Length: ").append(data.length);
		Log.d(TAG, sb.toString());
	}

	@Override
	public void portStatusChangedEvent(EnumSet<PortStatus> portStatus,
			boolean breakSignal, int breakTimeout) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Port Status Changed").append(":");
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
		Log.d(TAG, sb.toString());
	}

	@Override
	public void sessionCloseEvent(int sessionID) {
		final StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Session Closed").append(":");
		sb.append("  Session ID: ").append(sessionID);
		Log.d(TAG, sb.toString());
		showToast(sb.toString());

		MfiTesterActivity.sessionID1 = sessionID;
		MfiTesterActivity.sessionID2 = sessionID;
		MfiTesterActivity.sessionID3 = sessionID;
		Log.d(TAG, "MfiTesterActivity.sessionID1 =" + sessionID);
		Log.d(TAG, "MfiTesterActivity.sessionID2 =" + sessionID);
		Log.d(TAG, "MfiTesterActivity.sessionID3 =" + sessionID);
	}

	@Override
	public void sessionDataConfirmationEvent(int sessionID, int packetID,
			DataConfirmationStatus status) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Session Data Confirmation").append(":");
		sb.append("  Session ID: ").append(sessionID);
		sb.append("  Packet ID:  ").append(packetID);
		sb.append("  Status:     ").append(
				(status != null ? status.name() : "null"));
		Log.d(TAG, sb.toString());
		showToast(sb.toString());
	}

	@Override
	public void sessionDataReceivedEvent(int sessionID, byte[] sessionData) {
		final StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Session Data Received").append(":");
		sb.append("  Session ID:  ").append(sessionID);
		sb.append("  Data Length: ").append(sessionData.length);
		Log.d(TAG, sb.toString());
		showToast(sb.toString());
	}

	@Override
	public void sessionOpenRequestEvent(int maximumTransmitPacket,
			int maximumReceivePacket, int sessionID, int protocolIndex) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Session Open Request").append(":");
		sb.append("  Maximum Transmit Packet: ").append(maximumTransmitPacket);
		sb.append("  Maximum Receive Packet:  ").append(maximumReceivePacket);
		sb.append("  Session ID:              ").append(sessionID);
		sb.append("  Protocol Index:          ").append(protocolIndex);
		Log.d(TAG, sb.toString());

		Log.d(TAG, "openSessionRequestResponse(true, sessionID, true)");
		MfiCommandManager.getInstance(mActivity).openSessionRequestResponse(
				true, sessionID, true);

		Log.d(TAG, "MfiTesterActivity.sessionID =" + sessionID);
		if (MfiTesterActivity.sessionID1 == 0) {
			MfiTesterActivity.sessionID1 = sessionID;
		} else if (MfiTesterActivity.sessionID2 == 0) {
			MfiTesterActivity.sessionID2 = sessionID;
		} else {
			MfiTesterActivity.sessionID3 = sessionID;
		}
	}

	@Override
	public void transmitBufferEmptyEvent() {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Transmit Buffer Empty");
		Log.d(TAG, sb.toString());
	}

	@Override
	public void connectionStatusEvent(ConnectionStatus status,
			ConnectionType type) {
		StringBuilder sb = new StringBuilder();
		sb.append("CLIENT: ").append("Connection Status").append(":");
		sb.append("  Connection Status: ");
		switch (status) {
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
		switch (type) {
		case SPP:
			sb.append("SPP");
			break;
		case MFI:
			sb.append("MFi");
			break;
		}
		Log.d(TAG, sb.toString());
		showToast(sb.toString());
	}

	@Override
	public void unhandledControlMessageReceivedEvent(short arg0,byte[] arg1) {
		// TODO Auto-generated method stub
	}

	private void showToast(final String message) {
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
			}
		});
	}
}
