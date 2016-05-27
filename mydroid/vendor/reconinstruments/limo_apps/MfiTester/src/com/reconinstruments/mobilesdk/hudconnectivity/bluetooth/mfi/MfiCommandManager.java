package com.reconinstruments.mobilesdk.hudconnectivity.bluetooth.mfi;

import java.util.EnumSet;
import java.util.UUID;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;
import com.stonestreetone.bluetopiapm.SPPM.ConnectionType;
import com.stonestreetone.bluetopiapm.SPPM.MFiAccessoryInfo;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager;
import com.stonestreetone.bluetopiapm.SPPM.ServiceRecordInformation;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.IncomingConnectionFlags;

public class MfiCommandManager {

	private static final String TAG = "MfiCommandManager";

	private static MfiCommandManager instance;

	private SerialPortClientManager serialPortClientManager;
	private SerialPortServerManager serialPortServerManager;
	private MfiClientEventCallback clientEventCallback;
	private MfiServerEventCallback serverEventCallback;

	private Activity mActivity;

	private MfiCommandManager(Activity activity) {
		mActivity = activity;
	}

	public static MfiCommandManager getInstance(Activity activity) {
		if (instance == null) {
			instance = new MfiCommandManager(activity);
		}
		return instance;
	}

	public synchronized final void init() {
		Log.d(TAG, "init");

		clientEventCallback = new MfiClientEventCallback(mActivity);
		serverEventCallback = new MfiServerEventCallback(mActivity);

		try {
			serialPortClientManager = new SerialPortClientManager(
					clientEventCallback);
		} catch (Exception e) {
			e.printStackTrace();
			/*
			 * BluetopiaPM server couldn't be contacted. This should never
			 * happen if Bluetooth was successfully enabled.
			 */
			Log.w(TAG, "ERROR: Could not connect to the BluetopiaPM service.");
		}
		configureMFiSettings();
		// openServer(1, null, null);
	}

	public synchronized final void cleanup() {
		Log.d(TAG, "cleanup");
		if (serialPortClientManager != null) {
			serialPortClientManager.dispose();
			serialPortClientManager = null;
		}
		// closeServer();
	}

	// Disconnect Remote Device
	public void disconnectRemoteDevice(final boolean client,
			final int flushTimeout) {
		Log.d(TAG, "disconnectRemoteDevice");
		new Thread() {
			public void run() {
				int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = -1;
					result = manager.disconnectRemoteDevice(flushTimeout);
					Log.d(TAG, "disconnectRemoteDevice() result: " + result);
					showToast("disconnectRemoteDevice() result: " + result);
				} else {
					Log.w(TAG,
							"disconnectRemoteDevice() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// Read Data
	public void readData(final boolean client) {
		Log.d(TAG, "readData");
		new Thread() {
			public void run() {
				final int result;
				byte[] dataBuffer;
				String dataString = null;
				SPPM manager;

				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}

				if (manager != null) {
					// TODO need to add support for testing readData overload
					// for partial buffers

					dataBuffer = new byte[1024];
					result = manager.readData(dataBuffer,
							SPPM.TIMEOUT_IMMEDIATE);
					Log.d(TAG, "readData() result: " + result);
					showToast("readData() result: " + result);
					for (int index = 0; index < result; index++) {
						dataString = dataString + (char) dataBuffer[index];
					}
					if (dataString != null) {
						Log.d(TAG, dataString);
					}
				} else {
					Log.w(TAG,
							"readData() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// Write Data
	public void writeData(final boolean client) {
		Log.d(TAG, "writeData");
		new Thread() {
			public void run() {
				final int result;
				SPPM manager;

				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager
							.writeData("This is an SPP data test message"
									.getBytes(), 5000);

					Log.d(TAG, "writeData() result: " + result);
					showToast("writeData() result: " + result);
				} else {
					Log.w(TAG,
							"writeData() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// Write Arbitrary Data
	public void writeDataArbitrary(final boolean client,
			final String arbitraryDataString, final int writeTimeout) {
		Log.d(TAG, "writeDataArbitrary");
		new Thread() {
			public void run() {
				int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager.writeData(arbitraryDataString.getBytes(),
							writeTimeout);
					Log.d(TAG, "writeData() result: " + result);
				} else {
					Log.w(TAG,
							"writeData() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// Send Line Status
	public void sendLineStatus(final boolean client,
			final EnumSet<SPPM.LineStatus> lineStatus) {
		Log.d(TAG, "sendLineStatus");
		new Thread() {
			public void run() {
				int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager.sendLineStatus(lineStatus);
					Log.d(TAG, "sendLineStatus() result: " + result);
				} else {
					Log.w(TAG,
							"sendLineStatus() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// Send Port Status
	public void sendPortStatus(final boolean client,
			final EnumSet<SPPM.PortStatus> portStatus,
			final boolean breakSignal, final int breakTimeout) {
		Log.d(TAG, "sendPortStatus");
		new Thread() {
			public void run() {
				int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager.sendPortStatus(portStatus, breakSignal,
							breakTimeout);
					Log.d(TAG, "sendPortStatus() result: " + result);
				} else {
					Log.w(TAG,
							"sendPortStatus() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	private BluetoothAddress getRemoteDeviceAddress(String address) {
		BluetoothAddress remoteAddress = null;

		if (address != null) {
			try {
				remoteAddress = new BluetoothAddress(address);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return remoteAddress;
	}

	// Query Remote Device Services
	public void queryRemoteDeviceServices(final String address,
			final EnumSet<SPPM.PortStatus> portStatus,
			final boolean breakSignal, final int breakTimeout) {
		Log.d(TAG, "queryRemoteDeviceServices");
		new Thread() {
			public void run() {
				ServiceRecordInformation[] serviceRecordInformation;
				BluetoothAddress bluetoothAddress;
				if (serialPortClientManager != null) {

					if ((bluetoothAddress = getRemoteDeviceAddress(address)) == null) {
						Log.d(TAG,
								"ERROR: Bluetooth address is not formatted correctly.");
						return;
					}
					serviceRecordInformation = serialPortClientManager
							.queryRemoteDeviceServices(bluetoothAddress);
					if (serviceRecordInformation != null) {
						Log.d(TAG, "queryRemoteDeviceServices():");

						for (ServiceRecordInformation serviceRecord : serviceRecordInformation) {
							Log.d(TAG, "Service Record Handle     : "
									+ serviceRecord.serviceRecordHandle);
							Log.d(TAG, "Service Class             : "
									+ serviceRecord.serviceClassID.toString());
							Log.d(TAG, "Service Name              : "
									+ serviceRecord.serviceName);
							Log.d(TAG, "Service RFCOMM Port Number: "
									+ serviceRecord.rfcommPortNumber);
						}
					} else {
						Log.w(TAG, "queryRemoteDeviceServices(): returned null");
					}
				}
			}
		}.start();
	}

	// (MFi) Configure MFi
	public void configureMFiSettings() {
		Log.d(TAG, "configureMFiSettings");
		new Thread() {
			public void run() {
				final int result;
				if (serialPortClientManager != null) {
					result = serialPortClientManager
							.configureMFiSettings(
									512,
									800,
									null,
									new MFiAccessoryInfo(
											SPPM.MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP,
											"Recon Jet",
											0x00010000,
											0x00010000,
											"Recon Instruments",
											"Jet",
											"40984E5CCE15",
											(SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1
													| SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3)),
									// new MFiAccessoryInfo(
									// SPPM.MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP,
									// "Apple Accessory",
									// 0x00010000,
									// 0x00010000,
									// "Stonestreet One",
									// "BluetopiaPM",
									// "SN:012346",
									// (SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1
									// |
									// SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2
									// |
									// SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3)),
									new String[] {
											"com.reconinstruments.command",
											"com.reconinstruments.object",
											"com.reconinstruments.file" },
									// new String[] {"com.stonestreetone.demo1",
									// "com.stonestreetone.demo2",
									// "com.stonestreetone.demo3"},
									"12345ABCDE", null);

					Log.d(TAG, "configureMFiSettings() result: " + result);
					showToast("configureMFiSettings() result: " + result);
				}
			}
		}.start();
	}

	// Query Connection Type
	public void queryConnectionType(final boolean client) {
		Log.d(TAG, "queryConnectionType");
		new Thread() {
			public void run() {
				ConnectionType type;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					type = manager.queryConnectionType();
					if (type != null) {
						switch (type) {
						case SPP:
							Log.d(TAG, "queryConnectionType() result: SPP");
							break;
						case MFI:
							Log.d(TAG, "queryConnectionType() result: MFi");
							break;
						}
					} else {
						Log.w(TAG,
								"queryConnectionType() result: Not currently connected");
					}
				} else {
					Log.w(TAG,
							"queryConnectionType() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// (MFi) Open Session Request Response
	public void openSessionRequestResponse(final boolean client,
			final int sessionID, final boolean accept) {
		Log.d(TAG, "openSessionRequestResponse");
		new Thread() {
			public void run() {
				int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager.openSessionRequestResponse(sessionID,
							accept);
					Log.d(TAG, "openSessionRequestResponse() result: " + result);
				} else {
					Log.w(TAG,
							"openSessionRequestResponse() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// (MFi) Send Session Data
	public void sendSessionData(final boolean client, final int sessionID) {
		Log.d(TAG, "sendSessionData");
		new Thread() {
			public void run() {
				final int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager.sendSessionData(sessionID,
							"This is a session data test message".getBytes());
					Log.d(TAG, "sendSessionData() result: " + result);
				} else {
					Log.w(TAG,
							"sendSessionData() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// (MFi) Send Non-Session Data
	public void sendNonSessionData(final boolean client, final int lingoID,
			final int commandID, final int transactionID) {
		Log.d(TAG, "sendNonSessionData");
		new Thread() {
			public void run() {
				final int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager.sendNonSessionData(lingoID, commandID,
							transactionID,
							"This is a non-session data test message"
									.getBytes());
					Log.d(TAG, "sendNonSessionData() result: " + result);
					showToast("sendNonSessionData() result: " + result);
				} else {
					Log.w(TAG,
							"sendNonSessionData() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// (MFi) Cancel Packet
	public void cancelPacket(final boolean client, final int packetID) {
		Log.d(TAG, "cancelPacket");
		new Thread() {
			public void run() {
				int result;
				SPPM manager;
				if (client) {
					manager = serialPortClientManager;
				} else {
					manager = serialPortServerManager;
				}
				if (manager != null) {
					result = manager.cancelPacket(packetID);
					Log.d(TAG, "cancelPacket() result: " + result);
				} else {
					Log.w(TAG,
							"cancelPacket() result: The selected Manager is not initialized");
				}
			}
		}.start();
	}

	// Connect Remote Device
	public void connectRemoteDevice(final String address, final int portNumber,
			final boolean waitForConnection) {
		Log.d(TAG, "connectRemoteDevice");
		new Thread() {
			public void run() {
				final int result;
				BluetoothAddress bluetoothAddress;
				EnumSet<ConnectionFlags> connectionFlags = EnumSet
						.noneOf(ConnectionFlags.class);
				connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
				connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
				connectionFlags.add(ConnectionFlags.MFI_REQUIRED);

				if (serialPortClientManager != null) {
					if ((bluetoothAddress = getRemoteDeviceAddress(address)) == null) {
						Log.d(TAG,
								"ERROR: Bluetooth address is not formatted correctly.");
						return;
					}
					result = serialPortClientManager.connectRemoteDevice(
							bluetoothAddress, portNumber, connectionFlags,
							waitForConnection);
					Log.d(TAG, "connectRemoteDevice() result: " + result);
					showToast("connectRemoteDevice() result: " + result);
				} else {
					Log.w(TAG,
							"connectRemoteDevice() result: The Client Manager is not initialized");
				}
			}
		}.start();
	}

	// Connection Request Response
	public void connectionRequestResponse(final boolean accept) {
		Log.d(TAG, "connectionRequestResponse");
		new Thread() {
			public void run() {
				int result;
				if (serialPortServerManager != null) {
					result = serialPortServerManager
							.connectionRequestResponse(accept);
					Log.d(TAG, "connectionRequestResponse() result: " + result);
				} else {
					Log.w(TAG,
							"connectionRequestResponse() result: There is not active server port");
				}
			}
		}.start();
	}

	// Open Server Port
	public void openServer(final int portNumber, final UUID[] serviceClasses,
			final String serviceName) {
		Log.d(TAG, "openServer");
		new Thread() {
			public void run() {
				EnumSet<IncomingConnectionFlags> flags;
				flags = EnumSet.noneOf(IncomingConnectionFlags.class);

				// if(flag == 0) {
				// flags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
				// }
				// if(flag == 1) {
				flags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
				// }
				// if(flag == 2) {
				flags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
				// }
				// if(flag == 3) {
				flags.add(IncomingConnectionFlags.MFI_ALLOWED);
				// }
				// if(flag == 4) {
				flags.add(IncomingConnectionFlags.MFI_REQUIRED);
				// }

				if (serialPortServerManager == null) {
					try {
						// if ((serviceClasses != null) || (serviceName !=
						// null)) {
						// serialPortServerManager = new
						// SerialPortServerManager(
						// serverEventCallback, portNumber, flags,
						// serviceClasses, null, serviceName);
						// } else {
						serialPortServerManager = new SerialPortServerManager(
								serverEventCallback, portNumber, flags);
						// }
						Log.d(TAG, "Open Server result: Success");
					} catch (ServerNotReachableException e) {
						e.printStackTrace();
						Log.w(TAG,
								"Open Server result: Unable to communicate with Platform Manager service");
					} catch (BluetopiaPMException e) {
						e.printStackTrace();
						Log.w(TAG,
								"Open Server result: Unable to register server port (already in use?)");
					}
				} else {
					Log.w(TAG,
							"Open Server result: The sample already has an active SPP server manager");
				}
			}
		}.start();
	}

	// Close Server Port
	public void closeServer() {
		Log.d(TAG, "closeServer");
		new Thread() {
			public void run() {
				if (serialPortServerManager != null) {
					serialPortServerManager.dispose();
					serialPortServerManager = null;
					Log.d(TAG, "Close Server result: Success");
				} else {
					Log.w(TAG,
							"Close Server result: No server is currently active.");
				}
			}
		}.start();
	}

	private void showToast(final String message) {
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
			}
		});
	}
}
