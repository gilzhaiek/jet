package com.reconinstruments.mobilesdk.btmfi;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.mfitester.MfiTesterActivity.Channel;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
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

public class BTMfiSessionManager {

	private static final String TAG = "BTMfiSessionManager";

	private static BTMfiSessionManager instance;

	private SerialPortClientManager serialPortClientManager;
	private SerialPortServerManager serialPortServerManager;
	private BTMfiClientEventCallback clientEventCallback;
	private BTMfiServerEventCallback serverEventCallback;
	private boolean inUse = false;

	private Context mContext;

	private int totalReceived = 0;
	private int dataReceived = 0;
	private ByteBuffer receivingBuff = ByteBuffer.allocate(0);;

	private BTMfiSessionManager(Context context) {
		mContext = context;
	}

	public static BTMfiSessionManager getInstance(Context context) {
		if (instance == null) {
			instance = new BTMfiSessionManager(context);
		}
		return instance;
	}

	public synchronized final void init() {
		Log.d(TAG, "init");

		clientEventCallback = new BTMfiClientEventCallback(mContext, this);
		serverEventCallback = new BTMfiServerEventCallback(mContext, this);

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
		openServer(1, null, null);
		connectRemoteDevice("9C:20:7B:1C:DA:69", 1, false);
	}

	public synchronized final void cleanup() {
		Log.d(TAG, "cleanup");
		if (serialPortClientManager != null) {
			serialPortClientManager.dispose();
			serialPortClientManager = null;
		}
		setInUse(false);
		closeServer();
	}

	public boolean isInUse() {
		return inUse;
	}

	public void setInUse(boolean inUse) {
		this.inUse = inUse;
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
					if (result == 0) {
						setInUse(false);
					} else {
						setInUse(true);
					}
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
	public boolean sendSessionData(Channel channel, final byte[] message) {
		if (Channel.COMMAND_CHANNEL.compareTo(channel) == 0) {
			sendSessionData(true, clientEventCallback.getCommandSessionId(),
					message);
		} else if (Channel.OBJECT_CHANNEL.compareTo(channel) == 0) {
			sendSessionData(true, clientEventCallback.getObjectSessionId(),
					message);
		} else if (Channel.FILE_CHANNEL.compareTo(channel) == 0) {
			sendSessionData(true, clientEventCallback.getFileSessionId(),
					message);
		}
		return true;
	}

	// (MFi) Send Session Data
	public void sendSessionData(final boolean client, final int sessionID,
			final byte[] message) {
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
					result = manager.sendSessionData(sessionID, message);
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
					if (result == 0) {
						setInUse(true);
					} else {
						setInUse(false);
					}
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
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			public void run() {
				Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	private int byteArrayToInt(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b.clone(), 0, 4);
		int size = bb.getInt();
		if (size < 50331648) // maximum heap size
			return size;
		return 0;
	}

	public synchronized void receiveData(byte[] readBuf, int len) {

		int remaining = receivingBuff.remaining();
		if (remaining == 0) { // new data blocks are coming
			totalReceived = 0;
			dataReceived = 0;
			receivingBuff = ByteBuffer.allocate(0);
			if (len < 16) {
				return;
			}
			totalReceived = byteArrayToInt(readBuf);
			if (totalReceived > 0) {
				receivingBuff = ByteBuffer.allocate(totalReceived);
				remaining = receivingBuff.remaining();
				Log.i(TAG,
						"Start receiving new HUDConnectivityMessage data block, total size = "
								+ totalReceived);
			} else {
				return;
			}
		}

		Log.d(TAG, "Remaining size in the receivingBuff = " + remaining);
		Log.d(TAG, "The data block size = " + len);
		if (remaining < len) {
			receivingBuff.put(readBuf, 0, remaining);
			dataReceived += remaining;
		} else {
			receivingBuff.put(readBuf, 0, len);
			dataReceived += len;
		}

		Log.d(TAG, "dataReceived= " + dataReceived);
		remaining = receivingBuff.remaining();
		if (remaining == 0) {
			totalReceived = 0;
			dataReceived = 0;
			byte[] tmp = receivingBuff.array().clone();
			receivingBuff = ByteBuffer.allocate(0);
			sendHUDConnectivityMessage(tmp);
		}
	}

	/**
	 * generate an md5 checksum from a byte array offset allows md5 to be
	 * calculated ignoring the beginning of the data
	 */
	public static String md5(byte[] array, int offset) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			// digest.update(array);
			digest.update(array, offset, array.length - offset);
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	private synchronized void sendHUDConnectivityMessage(byte[] readBuf) {
		Log.i(TAG,
				"Stop receiving data, constructing the HUDConnectivityMessage");
		HUDConnectivityMessage cMsg = new HUDConnectivityMessage(readBuf);

		if (cMsg != null && cMsg.getIntentFilter() != null) {
			Log.d(TAG, "cMsg size = " + cMsg.toByteArray().length);
			Log.d(TAG, "cMsg.getData() size = " + cMsg.getData().length);
			Log.i(TAG, "md5(cMsg.getData(),0) = " + md5(cMsg.getData(), 0));

			Log.d(TAG, "Received the message " + cMsg.toString());
			Log.d(TAG, "Received the message, data field: " + new String(cMsg.getData()));
			if (true) {
				Toast.makeText(mContext.getApplicationContext(),
						"Received the message " + cMsg.toString(),
						Toast.LENGTH_LONG).show();
			}
			Intent i = new Intent(cMsg.getIntentFilter());
			i.putExtra("message", cMsg.toByteArray());
			mContext.sendBroadcast(i);
			Log.d(TAG, "Sent out the broadcast to " + cMsg.getIntentFilter());
			if (true) {
				Toast.makeText(mContext.getApplicationContext(),
						"Sent out the broadcast to " + cMsg.getIntentFilter(),
						Toast.LENGTH_LONG).show();
			}
		} else {
			Log.d(TAG, "Received the message " + cMsg.toString());
		}
	}

}
