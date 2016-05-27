/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * ANT Protocol API Sample for Stonestreet One Bluetooth Protocol Stack
 * Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.antm;

import java.util.EnumSet;

import android.util.Log;

import com.stonestreetone.bluetopiapm.ANTM;
import com.stonestreetone.bluetopiapm.ANTM.ANTCapabilities;
import com.stonestreetone.bluetopiapm.ANTM.ChannelResponseMessageCode;
import com.stonestreetone.bluetopiapm.ANTM.ChannelType;
import com.stonestreetone.bluetopiapm.ANTM.ExtendedAssignmentFeatures;
import com.stonestreetone.bluetopiapm.ANTM.ExtendedMessagesFlags;
import com.stonestreetone.bluetopiapm.ANTM.StartupMessageFlags;
import com.stonestreetone.bluetopiapm.ANTM.TransmitPowerLevel;
import com.stonestreetone.bluetopiapm.ANTM.USBDescriptorStringNumber;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;
import com.stonestreetone.bluetopiapm.sample.util.Utils;

public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG = "ANTM_Sample";

    /*package*/ DEVM                           deviceManager;
    /*package*/ ANTM antm;

    @Override
    protected boolean profileEnable() {
        synchronized(this) {

            try {
                deviceManager = new DEVM(deviceEventCallback);
                antm = new ANTM(antmEventCallback);
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
            if(deviceManager != null) {
                deviceManager.dispose();
                deviceManager = null;
            }

            if(antm != null) {
                antm.dispose();
                antm = null;
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
        return 5;
    }



    /*==========================================================
     * DEVm and GATT Callback handlers
     */
    private final EventCallback deviceEventCallback = new EventCallback() {

        @Override
        public void devicePoweredOnEvent() { }

        @Override
        public void devicePoweringOffEvent(int poweringOffTimeout) { }

        @Override
        public void devicePoweredOffEvent() { }

        @Override
        public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.localDevicePropertiesChangedEventLabel));

            displayMessage("");
            displayMessage(sb);

            for(LocalPropertyField localPropertyField: changedFields) {
                switch(localPropertyField) {
                    case DISCOVERABLE_MODE:
                        if(localProperties.discoverableMode)
                            displayMessage("Device is Discoverable");
                        else
                            displayMessage("Device is Not Discoverable");

                        displayMessage("Discoverable Mode Timeout: " + localProperties.discoverableModeTimeout);
                        break;
                    case CLASS_OF_DEVICE:
                    case DEVICE_NAME:
                    case CONNECTABLE_MODE:
                    case PAIRABLE_MODE:
                    case DEVICE_FLAGS:
                        break;
				default:
					break;
                }
            }
        }

        @Override
        public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) { }

        @Override
        public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
            StringBuilder sb = new StringBuilder();

            sb.append("remoteLowEnergyDeviceServicesStatusEvent: ");
            sb.append(remoteDevice.toString());
            if(success)
                sb.append(" Success");
            else
                sb.append(" Failure");

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void discoveryStartedEvent() { }

        @Override
        public void discoveryStoppedEvent() { }

        @Override
        public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {
            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
                StringBuilder sb = new StringBuilder();

                sb.append(resourceManager.getString(R.string.remoteDeviceFoundEventLabel));
                displayMessage("");
                displayMessage(sb);

                displayMessage("Device Address  : " + deviceProperties.deviceAddress.toString());
                displayMessage("Class of Device : " + Integer.toHexString(deviceProperties.classOfDevice));
                displayMessage("Device Name     : " + deviceProperties.deviceName);
                displayMessage("RSSI            : " + deviceProperties.rssi);
                displayMessage("Transmit power  : " + deviceProperties.transmitPower);
                displayMessage("Sniff Interval  : " + deviceProperties.sniffInterval);


                if(deviceProperties.applicationData != null) {
                    displayMessage("Friendly Name   : " + deviceProperties.applicationData.friendlyName);
                    displayMessage("Application Info: " + deviceProperties.applicationData.applicationInfo);
                }

                for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
                    displayMessage("Remote Device Flag: " + remoteDeviceFlag.toString());
            }
        }

        @Override
        public void remoteDevicePropertiesStatusEvent(boolean success, RemoteDeviceProperties deviceProperties) {
            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
                StringBuilder sb = new StringBuilder();

                sb.append(resourceManager.getString(R.string.remoteDevicePropertiesStatusEventLabel)).append(": ");
                if(success)
                    sb.append("success");
                else
                    sb.append("failure");

                displayMessage("");
                displayMessage(sb);

                displayMessage("Device Address    : " + deviceProperties.deviceAddress.toString());
                displayMessage("Class of Device   : " + Integer.toHexString(deviceProperties.classOfDevice));
                displayMessage("Device Name       : " + deviceProperties.deviceName);
                displayMessage("RSSI              : " + deviceProperties.rssi);
                displayMessage("Transmit power    : " + deviceProperties.transmitPower);
                displayMessage("Sniff Interval    : " + deviceProperties.sniffInterval);

                if(deviceProperties.applicationData != null) {
                    displayMessage("Friendly Name     : " + deviceProperties.applicationData.friendlyName);
                    displayMessage("Application Info  : " + deviceProperties.applicationData.applicationInfo);
                }

                for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
                    displayMessage("Remote Device Flag: " + remoteDeviceFlag.toString());
            }
        }

        @Override
        public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties deviceProperties, EnumSet<RemotePropertyField> changedFields) {
            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
                boolean foundChange = false;
                boolean foundFlag = false;

                StringBuilder sb = new StringBuilder();

                sb.append(resourceManager.getString(R.string.remoteDevicePropertiesChangedEventLabel)).append(": ");
                sb.append(deviceProperties.deviceAddress.toString());

                displayMessage("");
                displayMessage(sb);

                for(RemotePropertyField remotePropertyField: changedFields) {
                    switch(remotePropertyField) {
                        case DEVICE_FLAGS:
                            for(RemoteDeviceFlags remoteDeviceFlags: deviceProperties.remoteDeviceFlags)
                                displayMessage("Remote Device Flag: " + remoteDeviceFlags);
                            break;
                        case CLASS_OF_DEVICE:
                            displayMessage("Class of Device: " + Integer.toHexString(deviceProperties.classOfDevice));
                            foundChange = true;
                            break;
                        case DEVICE_NAME:
                            displayMessage("Device Name: " + deviceProperties.deviceName);
                            foundChange = true;
                            break;
                        case APPLICATION_DATA:
                            if(deviceProperties.applicationData != null) {
                                displayMessage("Friendly Name   : " + deviceProperties.applicationData.friendlyName);
                                displayMessage("Application Info: " + deviceProperties.applicationData.applicationInfo);
                            } else {
                                displayMessage("Application Data is null");
                            }
                            foundChange = true;
                            break;
                        case RSSI:
                            displayMessage("RSSI: " + deviceProperties.rssi);
                            foundChange = true;
                            break;
                        case PAIRING_STATE:
                            for(RemoteDeviceFlags searchPaired: deviceProperties.remoteDeviceFlags) {
                                if (searchPaired == RemoteDeviceFlags.CURRENTLY_PAIRED) {
                                    displayMessage("Device Paired");
                                    foundFlag = true;
                                    break;
                                }
                            }

                            if(foundFlag == false)
                               displayMessage("Device Unpaired");
                            else
                                foundFlag = false;

                            foundChange = true;
                            break;
                        case CONNECTION_STATE:
                            for(RemoteDeviceFlags searchConnected: deviceProperties.remoteDeviceFlags) {
                                if (searchConnected == RemoteDeviceFlags.CURRENTLY_CONNECTED) {
                                    displayMessage("Device Connected");
                                    foundFlag = true;
                                    break;
                                }
                            }

                            if(foundFlag == false)
                               displayMessage("Device Unconnected");
                            else
                                foundFlag = false;

                            foundChange = true;
                            break;
                        case ENCRYPTION_STATE:
                            for(RemoteDeviceFlags searchEncrypted: deviceProperties.remoteDeviceFlags) {
                                if (searchEncrypted == RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED) {
                                    displayMessage("Device Link Encrypted");
                                    foundFlag = true;
                                    break;
                                }
                            }

                            if(foundFlag == false)
                               displayMessage("Device Link Unencrypted");
                            else
                                foundFlag = false;

                            foundChange = true;
                            break;
                        case SNIFF_STATE:
                            for(RemoteDeviceFlags searchSniff: deviceProperties.remoteDeviceFlags) {
                                if(searchSniff == RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE) {
                                    displayMessage("Device Link in Sniff Mode");
                                    foundFlag = true;
                                    break;
                                }
                            }

                            if(foundFlag == false)
                               displayMessage("Device Link not in Sniff Mode");
                            else
                                foundFlag = false;

                            foundChange = true;
                            break;
                        case SERVICES_STATE:
                            for(RemoteDeviceFlags searchServices: deviceProperties.remoteDeviceFlags) {
                                if(searchServices == RemoteDeviceFlags.SERVICES_KNOWN) {
                                    displayMessage("Services are Known");
                                    foundFlag = true;
                                    break;
                                }
                            }

                            if(foundFlag == false)
                               displayMessage("Services are not Known");
                            else
                                foundFlag = false;

                            foundChange = true;
                            break;
					default:
						break;
                    }

                    if(foundChange)
                        break;
                }
            }
        }

        @Override
        public void remoteDevicePairingStatusEvent(BluetoothAddress remoteDevice, boolean success, int authenticationStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDevicePairingStatusEventLabel)).append(": ");
            if(success)
                sb.append("Success ").append(remoteDevice.toString());
            else
                sb.append("Failure ");

            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Authentication Status: " + (authenticationStatus & 0x7FFFFFFF) + (((authenticationStatus & 0x80000000) != 0) ? " (LE)" : "(BR/EDR)"));
        }

        @Override
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceEncryptionStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Encryption Status: " + status);
        }

        @Override
        public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice) { }

        @Override
        public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceConnectionStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Connection Status: " + status);
        }

        @Override
        public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceAuthenticationStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Authentication Status: " + status);
        }

        @Override
        public void deviceScanStartedEvent() {
            displayMessage("");
            displayMessage("deviceScanStartedEvent");
        }

        @Override
        public void deviceScanStoppedEvent() {
            displayMessage("");
            displayMessage("deviceScanStoppedEvent");
        }

        @Override
        public void deviceAdvertisingStarted() {
            displayMessage("");
            displayMessage("deviceAdvertisingStartedEvent");
            
        }
        
        @Override
        public void deviceAdvertisingStopped() {
            displayMessage("");
            displayMessage("deviceAdvertisingStoppedEvent");
            
        }
        @Override
        public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress PriorResolvableDeviceAddress, BluetoothAddress CurrentResolvableDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append("remoteLowEnergyDeviceAddressChangedEvent: \n");
            sb.append("    PriorResolvableDeviceAddress = ");
            sb.append(PriorResolvableDeviceAddress.toString());
            sb.append("\n    CurrentResolvableDeviceAddress = ");
            sb.append(CurrentResolvableDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);
        }
    };

    /*==========================================================
     * LE Profile Callback handlers
     */

    private final ANTM.EventCallback antmEventCallback = new ANTM.EventCallback() {
		
		@Override
		public void extendedBurstDataPacketEvent(int arg0, int arg1, int arg2,
				int arg3, byte[] arg4) {
			displayMessage("");
			displayMessage("Extended Burst Data Packed Event");
			
			StringBuilder sb = new StringBuilder();
            sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
            sb.append("Device Number     : ").append(String.valueOf(arg1)).append('\n');
            sb.append("Device Type       : ").append(String.valueOf(arg2)).append('\n');
            sb.append("Transmission Type : ").append(String.valueOf(arg3)).append('\n');
            displayMessage(sb);
		}
		
		@Override
		public void extendedBroadcastDataPacketEvent(int arg0, int arg1, int arg2,
				int arg3, byte[] arg4) {
			displayMessage("");
			displayMessage("Extended Broadcast Data Packet Event");
			
			StringBuilder sb = new StringBuilder();
            sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
            sb.append("Device Number     : ").append(String.valueOf(arg1)).append('\n');
            sb.append("Device Type       : ").append(String.valueOf(arg2)).append('\n');
            sb.append("Transmission Type : ").append(String.valueOf(arg3)).append('\n');
            displayMessage(sb);
			
		}
		
		@Override
		public void extendedAcknowledgedDataPacketEvent(int arg0, int arg1,
				int arg2, int arg3, byte[] arg4) {
			displayMessage("");
			displayMessage("Extended Acknowledged Data Packed Event");
			
			StringBuilder sb = new StringBuilder();
            sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
            sb.append("Device Number     : ").append(String.valueOf(arg1)).append('\n');
            sb.append("Device Type       : ").append(String.valueOf(arg2)).append('\n');
            sb.append("Transmission Type : ").append(String.valueOf(arg3)).append('\n');
            displayMessage(sb);
            
		}
		
		@Override
		public void channelStatusEvent(int arg0, int arg1) {
			displayMessage("");
			displayMessage("Channel Status Event");
			StringBuilder sb = new StringBuilder();
            sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
            sb.append("Channel Status    : ").append(String.valueOf(arg1)).append('\n');
            displayMessage(sb);
		}

		@Override
		public void channelIDEvent(int arg0, int arg1, int arg2, int arg3) {
			displayMessage("");
			displayMessage("Channel ID Event");
			
			StringBuilder sb = new StringBuilder();
            sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
            sb.append("Device Number     : ").append(String.valueOf(arg1)).append('\n');
            sb.append("Device Type       : ").append(String.valueOf(arg2)).append('\n');
            sb.append("Transmission Type : ").append(String.valueOf(arg3)).append('\n');
            displayMessage(sb);
		}

		@Override
		public void burstDataPacketEvent(int arg0, byte[] arg1) {
			displayMessage("");
			displayMessage("Burst Data Packet Event");
			
			StringBuilder sb = new StringBuilder();
			sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
			sb.append("Data              : ");
			for (int i = 0; i < arg1.length; i++) {
				sb.append(Byte.toString(arg1[i]));
			}
			sb.append('\n');
			displayMessage(sb);
		}
		
		@Override
		public void broadcastDataPacketEvent(int arg0, byte[] arg1) {
			displayMessage("");
			displayMessage("Broadcast Data Packet Event");
			
			StringBuilder sb = new StringBuilder();
			sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
			sb.append("Data              : ");
			for (int i = 0; i < arg1.length; i++) {
				sb.append(Byte.toString(arg1[i]));
			}
			sb.append('\n');
			displayMessage(sb);
		}
		
		@Override
		public void acknowledgedDataPacketEvent(int arg0, byte[] arg1) {
			displayMessage("");
			displayMessage("Acknowledged Data Packet Event");
			
			StringBuilder sb = new StringBuilder();
			sb.append("Channel Number    : ").append(String.valueOf(arg0)).append('\n');
			sb.append("Data              : ");
			for (int i = 0; i < arg1.length; i++) {
				sb.append(Byte.toString(arg1[i]));
			}
			sb.append('\n');
			displayMessage(sb);
		}
		
		@Override
		public void ANTVersionEvent(byte[] arg0) {
			displayMessage("");
			displayMessage("ANT Version Event");
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < arg0.length; i++) {
				sb.append(Byte.toString(arg0[i]));
			}
			sb.append('\n');
			displayMessage(sb);
		}

		@Override
		public void capabilitiesEvent(int arg0, int arg1,
				EnumSet<ANTCapabilities> arg2) 
		{
			displayMessage("");
			displayMessage("Capabilities Event");
			
			ANTCapabilities[] capabilities = new ANTCapabilities[arg2.size()];
			arg2.toArray(capabilities);
			for (int i = 0; i < capabilities.length; i++) {
				switch (capabilities[i]) {
				case NO_RECEIVE_CHANNELS:
					displayMessage("");
					displayMessage("NO_RECEIVE_CHANNELS");
					break;
				case NO_TRANSMIT_CHANNELS:
					displayMessage("");
					displayMessage("NO_TRANSMIT_CHANNELS");
					break;
				case NO_RECEIVE_MESSAGES:
					displayMessage("");
					displayMessage("NO_RECEIVE_MESSAGES");
					break;
				case NO_TRANSMIT_MESSAGES:
					displayMessage("");
					displayMessage("NO_TRANSMIT_MESSAGES");
					break;
				case NO_ACKNOWLEDGED_MESSAGES:
					displayMessage("");
					displayMessage("NO_ACKNOWLEDGED_MESSAGES");
					break;
				case NO_BURST_MESSAGES:
					displayMessage("");
					displayMessage("NO_BURST_MESSAGES");
					break;
				case NETWORK_ENABLED:
					displayMessage("");
					displayMessage("NETWORK_ENABLED");
					break;
				case SERIAL_NUMBER_ENABLED:
					displayMessage("");
					displayMessage("SERIAL_NUMBER_ENABLED");
					break;
				case PER_CHANNEL_TX_POWER_ENABLED:
					displayMessage("");
					displayMessage("PER_CHANNEL_TX_POWER_ENABLED");
					break;
				case LOW_PRIORITY_SEARCH_ENABLED:
					displayMessage("");
					displayMessage("LOW_PRIORITY_SEARCH_ENABLED");
					break;
				case SCRIPT_ENABLED:
					displayMessage("");
					displayMessage("SCRIPT_ENABLED");
					break;
				case SEARCH_LIST_ENABLED:
					displayMessage("");
					displayMessage("SEARCH_LIST_ENABLED");
					break;
				case LED_ENABLED:
					displayMessage("");
					displayMessage("LED_ENABLED");
					break;
				case EXTENDED_MESSAGE_ENABLED:
					displayMessage("");
					displayMessage("EXTENDED_MESSAGE_ENABLED");
					break;
				case SCAN_MODE_ENABLED:
					displayMessage("");
					displayMessage("SCAN_MODE_ENABLED");
					break;
				case PROXIMITY_SEARCH_ENABLED:
					displayMessage("");
					displayMessage("PROXIMITY_SEARCH_ENABLED");
					break;
				case EXTENDED_ASSIGN_ENABLED:
					displayMessage("");
					displayMessage("EXTENDED_ASSIGN_ENABLED");
					break;
				case FS_ANTFS_ENABLED:
					displayMessage("");
					displayMessage("FS_ANTFS_ENABLED");
					break;
				}
			}
		}

		@Override
		public void channelResponseEvent(int arg0, int arg1,
				ChannelResponseMessageCode arg2) {
			displayMessage("");
			displayMessage("Channel Response Event");
			
			switch (arg2) {
			case RESPONSE_NO_ERROR:
				displayMessage("");
				displayMessage("RESPONSE_NO_ERROR");
				break;
			case EVENT_RX_SEARCH_TIMEOUT:
				displayMessage("");
				displayMessage("EVENT_RX_SEARCH_TIMEOUT");
				break;
			case EVENT_RX_FAIL:
				displayMessage("");
				displayMessage("EVENT_RX_FAIL");
				break;
			case EVENT_TX:
				displayMessage("");
				displayMessage("EVENT_TX");
				break;
			case EVENT_TRANSFER_RX_FAILED:
				displayMessage("");
				displayMessage("EVENT_TRANSFER_RX_FAILED");
				break;
			case EVENT_TRANSFER_TX_COMPLETED:
				displayMessage("");
				displayMessage("EVENT_TRANSFER_TX_COMPLETED");
				break;
			case EVENT_TRANSFER_TX_FAILED:
				displayMessage("");
				displayMessage("EVENT_TRANSFER_TX_FAILED");
				break;
			case EVENT_CHANNEL_CLOSED:
				displayMessage("");
				displayMessage("EVENT_CHANNEL_CLOSED");
				break;
			case EVENT_RX_FAIL_GO_TO_SEARCH:
				displayMessage("");
				displayMessage("EVENT_RX_FAIL_GO_TO_SEARCH");
				break;
			case EVENT_CHANNEL_COLLISION:
				displayMessage("");
				displayMessage("EVENT_CHANNEL_COLLISION");
				break;
			case EVENT_TRANSFER_TX_START:
				displayMessage("");
				displayMessage("EVENT_TRANSFER_TX_START");
				break;
			case CHANNEL_IN_WRONG_STATE:
				displayMessage("");
				displayMessage("CHANNEL_IN_WRONG_STATE");
				break;
			case CHANNEL_NOT_OPENED:
				displayMessage("");
				displayMessage("CHANNEL_NOT_OPENED");
				break;
			case CHANNEL_ID_NOT_SET:
				displayMessage("");
				displayMessage("CHANNEL_ID_NOT_SET");
				break;
			case CLOSE_ALL_CHANNELS:
				displayMessage("");
				displayMessage("CLOSE_ALL_CHANNELS");
				break;
			case TRANSFER_IN_PROGRESS:
				displayMessage("");
				displayMessage("TRANSFER_IN_PROGRESS");
				break;
			case TRANSFER_SEQUENCE_NUMBER_ERROR:
				displayMessage("");
				displayMessage("TRANSFER_SEQUENCE_NUMBER_ERROR");
				break;
			case TRANSFER_IN_ERROR:
				displayMessage("");
				displayMessage("TRANSFER_IN_ERROR");
				break;
			case MESSAGE_SIZE_EXCEEDS_LIMIT:
				displayMessage("");
				displayMessage("MESSAGE_SIZE_EXCEEDS_LIMIT");
				break;
			case INVALID_MESSAGE:
				displayMessage("");
				displayMessage("INVALID_MESSAGE");
				break;
			case INVALID_NETWORK_NUMBER:
				displayMessage("");
				displayMessage("INVALID_NETWORK_NUMBER");
				break;
			case INVALID_LIST_ID:
				displayMessage("");
				displayMessage("INVALID_LIST_ID");
				break;
			case INVALID_SCAN_TX_CHANNEL:
				displayMessage("");
				displayMessage("INVALID_SCAN_TX_CHANNEL");
				break;
			case INVALID_PARAMETER_PROVIDED:
				displayMessage("");
				displayMessage("INVALID_PARAMETER_PROVIDED");
				break;
			case EVENT_QUEUE_OVERFLOW:
				displayMessage("");
				displayMessage("EVENT_QUEUE_OVERFLOW");
				break;
			case NVM_FULL_ERROR:
				displayMessage("");
				displayMessage("NVM_FULL_ERROR");
				break;
			case NVM_WRITE_ERROR:
				displayMessage("");
				displayMessage("NVM_WRITE_ERROR");
				break;
			case USB_STRING_WRITE_FAIL:
				displayMessage("");
				displayMessage("USB_STRING_WRITE_FAIL");
				break;
			case MSG_SERIAL_ERROR_ID:
				displayMessage("");
				displayMessage("MSG_SERIAL_ERROR_ID");
				break;
			case UNKNOWN_RESPONSE_CODE:
				displayMessage("");
				displayMessage("UNKNOWN_RESPONSE_CODE");
				break;
			}
		}

		@Override
		public void startupMessageEvent(EnumSet<StartupMessageFlags> arg0) {
			displayMessage("");
			displayMessage("Startup Message Event");
			
			StartupMessageFlags[] flags = new StartupMessageFlags[arg0.size()];
			arg0.toArray(flags);
			for (int i = 0; i < flags.length; i++) {
				switch (flags[i]) {
				case POWER_ON_RESET:
					displayMessage("");
					displayMessage("POWER_ON_RESET");
					break;
				case HARDWARE_RESET:
					displayMessage("");
					displayMessage("HARDWARE_RESET");
					break;
				case WATCH_DOG_RESET:
					displayMessage("");
					displayMessage("WATCH_DOG_RESET");
					break;
				case COMMAND_RESET:
					displayMessage("");
					displayMessage("COMMAND_RESET");
					break;
				case SYNCHRONOUS_RESET:
					displayMessage("");
					displayMessage("SYNCHRONOUS_RESET");
					break;
				case SUSPEND_RESET:
					displayMessage("");
					displayMessage("SUSPEND_RESET");
					break;
				}
			}
		}

        @Override
        public void rawDataPacketEvent(byte[] arg0) {
            displayMessage("");
            displayMessage("Raw Data Packet Event");
            
            StringBuilder sb = new StringBuilder();
            sb.append("Data: \n");
            for (int i = 0; i < arg0.length; i++) {
                sb.append(String.format("0x%02X ", arg0[i]));
            }
            sb.append('\n');
            displayMessage(sb);
            
        }
	};

    /*
     * ============================
     *  Profile-specific Commands.
     * ============================
     */
    
    private final CommandHandler assignChannel_Handler = new CommandHandler() {
    	
    	String[] ChannelTypes =
    		{
    			"Receive Channel",
    			"Transmit Channel",
    			"Recieve Only Channel",
    			"Transmit Only Channel",
    			"Shared Bi-Directional Receive Channel",
    			"Shared Bi-Directional Transmit Channel"
    		};
    
    	
    	@Override
    	public void run() {
    		int result;   		
    		int channelNumber;
    		SpinnerValue spinnerValue;
    		ChannelType channelType = ChannelType.RECEIVE_CHANNEL;
        	int networkNumber;
        	EnumSet<ExtendedAssignmentFeatures> features = EnumSet.noneOf(ExtendedAssignmentFeatures.class);
        	
    		if (antm != null) {
    			
    			try {
    			    displayMessage("Param: " + getCommandParameterView(0).getValueText().text.toString());
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			spinnerValue = getCommandParameterView(1).getValueSpinner();
    			switch(spinnerValue.selectedItem) {
    			case 0:
    				channelType = ChannelType.RECEIVE_CHANNEL;
    				break;
    			case 1:
    				channelType = ChannelType.TRANSMIT_CHANNEL;
    				break;
    			case 2:
    				channelType = ChannelType.RECEIVE_ONLY_CHANNEL;
    				break;
    			case 3:
    				channelType = ChannelType.TRANSMIT_ONLY_CHANNEL;
    				break;
    			case 4:
    				channelType = ChannelType.SHARED_BIDIRECTIONAL_RECEIVE_CHANNEL;
    				break;
    			case 5:
    				channelType = ChannelType.SHARED_BIDIRECTIONAL_TRANSMIT_CHANNEL;
    				break;
    			}
    			
    			try {
    				networkNumber = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    			
    			if (getCommandParameterView(3).getValueCheckbox().value) { features.add(ExtendedAssignmentFeatures.BACKGROUND_SCANNING_CHANNEL_ENABLE); }
    			if (getCommandParameterView(4).getValueCheckbox().value) { features.add(ExtendedAssignmentFeatures.FREQUENCY_AGILITY_ENABLE); }
    			result = antm.assignChannel(channelNumber, channelType, networkNumber, features);
    			
    			displayMessage("");
    			displayMessage("assignChannel() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeSpinner("Channel Type", ChannelTypes);           
            getCommandParameterView(2).setModeText("", "Network Number"); 
            getCommandParameterView(3).setModeCheckbox("Background Scanning Channel Enable", false);
            getCommandParameterView(4).setModeCheckbox("Frequency Agility Enable", false);
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler unAssignChannel_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		int channelNumber;
    		
    		if (antm != null) {
    			
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			result = antm.unAssignChannel(channelNumber);
    			
    			displayMessage("");
    			displayMessage("unAssignChannel() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };

    private final CommandHandler setChannelID_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int deviceNumber;
    		int deviceType;
    		int transmissionType;
    		
    		if (antm != null) {
    			
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				deviceNumber = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			try {
    				deviceType = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    			
    			try {
    				transmissionType = Integer.valueOf(getCommandParameterView(3).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 4");
                    return;
                }
    			
    			result = antm.setChannelID(channelNumber, deviceNumber, deviceType, transmissionType);
    			
    			displayMessage("");
    			displayMessage("addChannelID() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeText("", "Device Number");
            getCommandParameterView(2).setModeText("", "Device Type"); 
            getCommandParameterView(3).setModeText("", "Transmission Type");
            getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setChannelPeriod_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int messagingPeriod;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				messagingPeriod = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			
    			result = antm.setChannelPeriod(channelNumber, messagingPeriod);
    			
    			displayMessage("");
    			displayMessage("setChannelPeriod() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeText("", "Messaging Period");
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setChannelSearchTimeout_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int searchTimeout;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				searchTimeout = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			
    			result = antm.setChannelSearchTimeout(channelNumber, searchTimeout);
    			
    			displayMessage("");
    			displayMessage("setChannelSearchTimeout() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeText("", "Search Timeout");
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setChannelRfFrequency_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int RFFrequency;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				RFFrequency = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			
    			result = antm.setChannelRfFrequency(channelNumber, RFFrequency);
    			
    			displayMessage("");
    			displayMessage("setChannelRfFrequency() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeText("", "RF Frequency");
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setNetworkKey_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		int channelNumber;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			String temp = getCommandParameterView(1).getValueText().text.toString();
    			byte[] networkKey = temp.getBytes();
    			
    			ANTM.NetworkKey key = new ANTM.NetworkKey(networkKey);
    			result = antm.setNetworkKey(channelNumber, key);
    			
    			displayMessage("");
    			displayMessage("setNetworkKey() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeText("", "Network Key");
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setTransmitPower_Handler = new CommandHandler() {
    	
    	String[] TransmitPower =
    		{
    			"Negative Twenty dBm",
    			"Negative Ten dBm",
    			"Negative Five dBm",
    			"Zero dBm",
    			"Four dBm"
    		};
    	
    	@Override
    	public void run() {
    		int result;
    		SpinnerValue spinnerValue;
    		TransmitPowerLevel tp = TransmitPowerLevel.NEGATIVE_TWENTY_DBM;
    	
    		if (antm != null) {
    			
    			spinnerValue = getCommandParameterView(0).getValueSpinner();
    			switch(spinnerValue.selectedItem) {
    			case 0:
    				tp = TransmitPowerLevel.NEGATIVE_TWENTY_DBM;
    				break;
    			case 1:
    				tp = TransmitPowerLevel.NEGATIVE_TEN_DBM;
    				break;
    			case 2:
    				tp = TransmitPowerLevel.NEGATIVE_FIVE_DBM;
    				break;
    			case 3:
    				tp = TransmitPowerLevel.ZERO_DBM;
    				break;
    			case 4:
    				tp = TransmitPowerLevel.FOUR_DBM;
    				break;
    			}
    			
    			result = antm.setTransmitPower(tp);
    			
    			displayMessage("");
    			displayMessage("setTransmitPower() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeSpinner("Power Level", TransmitPower);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler addChannelID_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int deviceNumber;
    		int deviceType;
    		int transmissionType;
    		int listIndex;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				deviceNumber = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			try {
    				deviceType = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    			
    			try {
    				transmissionType = Integer.valueOf(getCommandParameterView(3).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 4");
                    return;
                }
    			
    			try {
    				listIndex = Integer.valueOf(getCommandParameterView(4).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 5");
                    return;
                }
    			
    			
    			result = antm.addChannelID(channelNumber, deviceNumber, deviceType, transmissionType, listIndex);
    			
    			displayMessage("");
    			displayMessage("addChannelID() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
            getCommandParameterView(1).setModeText("", "Device Number");
            getCommandParameterView(2).setModeText("", "Device Type"); 
            getCommandParameterView(3).setModeText("", "Transmission Type");
            getCommandParameterView(4).setModeText("", "List Index");
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler configureInclusionExclusionList_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int listSize;
    		int exclude;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				listSize = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			try {
    				exclude = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    				
    			result = antm.configureInclusionExclusionList(channelNumber, listSize, exclude);
    			
    			displayMessage("");
    			displayMessage("configureInclusionExclusionList() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "List Size");
    		getCommandParameterView(2).setModeText("", "Exclude");
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setChannelTransmitPower_Handler = new CommandHandler() {
    	
    	String[] TransmitPower =
    		{
    			"Negative Twenty dBm",
    			"Negative Ten dBm",
    			"Negative Five dBm",
    			"Zero dBm",
    			"Four dBm"
    		};
    	
    	@Override
    	public void run() {
    		int result;
        	SpinnerValue spinnerValue;
        	TransmitPowerLevel tp = TransmitPowerLevel.NEGATIVE_TWENTY_DBM;
    		int channelNumber;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			spinnerValue = getCommandParameterView(0).getValueSpinner();
    			switch(spinnerValue.selectedItem) {
    			case 0:
    				tp = TransmitPowerLevel.NEGATIVE_TWENTY_DBM;
    				break;
    			case 1:
    				tp = TransmitPowerLevel.NEGATIVE_TEN_DBM;
    				break;
    			case 2:
    				tp = TransmitPowerLevel.NEGATIVE_FIVE_DBM;
    				break;
    			case 3:
    				tp = TransmitPowerLevel.ZERO_DBM;
    				break;
    			case 4:
    				tp = TransmitPowerLevel.FOUR_DBM;
    				break;
    			}
    				
    			result = antm.setChannelTransmitPower(channelNumber, tp);
    			
    			displayMessage("");
    			displayMessage("setChannelTransmitPower() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeSpinner("Power Level", TransmitPower);
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setLowPriorityChannelSearchTimeout_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int searchTimeout;
    	
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				searchTimeout = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    				
    			result = antm.setLowPriorityChannelSearchTimeout(channelNumber, searchTimeout);
    			
    			displayMessage("");
    			displayMessage("setLowPriorityChannelSearchTimeout() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Search Timeout");
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setSerialNumberChannelID_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int deviceType;
    		int transmissionType;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				deviceType = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			try {
    				transmissionType = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    				
    			result = antm.setSerialNumberChannelID(channelNumber, deviceType, transmissionType);
    			
    			displayMessage("");
    			displayMessage("setSerialNumberChannelID() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Device Type");
    		getCommandParameterView(2).setModeText("", "Transmission Type");
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler enableExtendedMessages_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		if (antm != null) {
    			
    				
    			result = antm.enableExtendedMessages(getCommandParameterView(0).getValueCheckbox().value);
    			
    			displayMessage("");
    			displayMessage("enableExtendedMessages() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeCheckbox("Extended Messages", true);
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler enableLED_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		if (antm != null) {
    			result = antm.enableLED(getCommandParameterView(0).getValueCheckbox().value);
    			
    			displayMessage("");
    			displayMessage("enableLED() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeCheckbox("LED", true);
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler enableCrystal_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		if (antm != null) {
    			result = antm.enableCrystal();
    			displayMessage("");
    			displayMessage("enableCrystal() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeHidden();
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler configureExtendedMessages_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		EnumSet<ExtendedMessagesFlags> flags = EnumSet.noneOf(ExtendedMessagesFlags.class);
    		
    		if (antm != null) {
    			if (getCommandParameterView(0).getValueCheckbox().value) { flags.add(ExtendedMessagesFlags.RX_TIMESTAMP_OUTPUT); }
    			if (getCommandParameterView(1).getValueCheckbox().value) { flags.add(ExtendedMessagesFlags.RSSI_OUTPUT); }
    			if (getCommandParameterView(2).getValueCheckbox().value) { flags.add(ExtendedMessagesFlags.CHANNEL_ID_OUTPUT); } 
    				
    			result = antm.configureExtendedMessages(flags);
    			
    			displayMessage("");
    			displayMessage("configureInclusionExclusionList() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeCheckbox("Rx Timestamp Output", false);
    		getCommandParameterView(1).setModeCheckbox("RSSI Output", false);
    		getCommandParameterView(2).setModeCheckbox("Channel ID Output", false);
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler configureFrequencyAgility_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int frequencyAgility1;
    		int frequencyAgility2;
    		int frequencyAgility3;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				frequencyAgility1 = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			try {
    				frequencyAgility2 = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    			
    			try {
    				frequencyAgility3 = Integer.valueOf(getCommandParameterView(3).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 4");
                    return;
                }
    			
    			result = antm.configureFrequencyAgility(channelNumber, frequencyAgility1, frequencyAgility2, frequencyAgility3);
    			
    			displayMessage("");
    			displayMessage("configureFrequencyAgility() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Frequency Agility 1");
    		getCommandParameterView(2).setModeText("", "Frequency Agility 2");
    		getCommandParameterView(3).setModeText("", "Frequency Agility 3");
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setProximitySearch_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int searchThreshold;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				searchThreshold = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			result = antm.setProximitySearch(channelNumber, searchThreshold);
    			
    			displayMessage("");
    			displayMessage("setProximitySearch() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Search Threshold");
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setChannelSearchPriority_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int searchPriority;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				searchPriority = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			result = antm.setChannelSearchPriority(channelNumber, searchPriority);
    			
    			displayMessage("");
    			displayMessage("setChannelSearchPriority() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Search Priority");
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setUSBDescriptorString_Handler = new CommandHandler() {
    	String[] DescriptorString =
    		{
    			"PID VID",
    			"Manufacturer",
    			"Device",
    			"Serial Number"
    		};
    	@Override
    	public void run() {
    		int result;
    		SpinnerValue spinnerValue;
    		USBDescriptorStringNumber dsn = USBDescriptorStringNumber.PID_VID;
    		String descriptor;
    		if (antm != null) {
    			
    			spinnerValue = getCommandParameterView(0).getValueSpinner();
    			switch (spinnerValue.selectedItem) {
    			case 0:
    				dsn = USBDescriptorStringNumber.PID_VID;
    				break;
    			case 1:
    				dsn = USBDescriptorStringNumber.MANUFACTURER;
    				break;
    			case 2:
    				dsn = USBDescriptorStringNumber.DEVICE;
    				break;
    			case 3:
    				dsn = USBDescriptorStringNumber.SERIAL_NUMBER;
    				break;
    			}

    			descriptor = getCommandParameterView(1).getValueText().text.toString();

    			result = antm.setUSBDescriptorString(dsn, descriptor);
    			
    			displayMessage("");
    			displayMessage("setUSBDescriptorString() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeSpinner("USB Descriptor String", DescriptorString);
    		getCommandParameterView(1).setModeText("", "Descriptor String");
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler resetSystem_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		if (antm != null) {
    			
    			result = antm.resetSystem();
    			
    			displayMessage("");
    			displayMessage("resetSystem() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeHidden();
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler openChannel_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			result = antm.openChannel(channelNumber);
    			
    			displayMessage("");
    			displayMessage("openChannel() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler closeChannel_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			result = antm.closeChannel(channelNumber);
    			
    			displayMessage("");
    			displayMessage("closeChannel() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler requestMessage_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		int messageID;
    		
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				messageID = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			result = antm.requestMessage(channelNumber, messageID);
    			
    			displayMessage("");
    			displayMessage("requestMessage() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Message ID");
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler openRxScanMode_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    		
    			result = antm.openRxScanMode(channelNumber);
    			
    			displayMessage("");
    			displayMessage("openRxScanMode() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler sleepMessage_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		if (antm != null) {
    			result = antm.sleepMessage();
    			displayMessage("");
    			displayMessage("sleepMessage() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeHidden();
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden();       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler sendBroadcastData_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		String temp;
    		byte[] data;
    		int offset;
    		int length;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			temp = getCommandParameterView(1).getValueText().text.toString();
    			data = new byte[temp.length()];
    			for (int i=0; i < data.length; i++) {
    		        data[i] = Byte.valueOf((byte)temp.charAt(i));     
    			}

    			try {
    				offset = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    			
    			try {
    				length = Integer.valueOf(getCommandParameterView(3).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 4");
                    return;
                }
    			result = antm.sendBroadcastData(channelNumber, data, offset, length);
    			
    			displayMessage("");
    			displayMessage("sendBroadcastData() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Data");
    		getCommandParameterView(2).setModeText("", "Offset");
    		getCommandParameterView(3).setModeText("", "Length");       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler sendAcknowledgedData_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		String temp;
    		byte[] data;
    		int offset;
    		int length;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			temp = getCommandParameterView(1).getValueText().text.toString();
    			data = new byte[temp.length()];
    			for (int i=0; i < data.length; i++) {
    		        data[i] = Byte.valueOf((byte)temp.charAt(i));     
    			}

    			try {
    				offset = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    			
    			try {
    				length = Integer.valueOf(getCommandParameterView(3).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 4");
                    return;
                }
    			result = antm.sendAcknowledgedData(channelNumber, data, offset, length);
    			
    			displayMessage("");
    			displayMessage("sendAcknowledgedData() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Data");
    		getCommandParameterView(2).setModeText("", "Offset");
    		getCommandParameterView(3).setModeText("", "Length");       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler sendBurstTransferData_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		int channelNumber;
    		String temp;
    		byte[] data;
    		int offset;
    		int length;
    		if (antm != null) {
    			try {
    				channelNumber = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			temp = getCommandParameterView(1).getValueText().text.toString();
    			data = new byte[temp.length()];
    			for (int i=0; i < data.length; i++) {
    		        data[i] = Byte.valueOf((byte)temp.charAt(i));     
    			}

    			try {
    				offset = Integer.valueOf(getCommandParameterView(2).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 3");
                    return;
                }
    			
    			try {
    				length = Integer.valueOf(getCommandParameterView(3).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 4");
                    return;
                }
    			result = antm.sendBurstTransferData(channelNumber, data, offset, length);
    			
    			displayMessage("");
    			displayMessage("sendBurstTransferData() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Channel Number");
    		getCommandParameterView(1).setModeText("", "Data");
    		getCommandParameterView(2).setModeText("", "Offset");
    		getCommandParameterView(3).setModeText("", "Length");       
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler initalizeCWTestMode_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
    		
    		if (antm != null) {
    			result = antm.initializeCWTestMode();
    			displayMessage("");
    			displayMessage("initalizeCWTestMode() result: " + result);
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeHidden();
    		getCommandParameterView(1).setModeHidden();
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden(); 
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler setCWTestMode_Handler = new CommandHandler() {
    	@Override
    	public void run() {
    		int result;
   
    		int txPower;
    		int RFFrequency;
    		
    		if (antm != null) {
    			
    			try {
    				txPower = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 1");
                    return;
                }
    			
    			try {
    				RFFrequency = Integer.valueOf(getCommandParameterView(1).getValueText().text.toString());
                } catch(NumberFormatException e) {
                	displayMessage("");
                    displayMessage("Invalid Parameter 2");
                    return;
                }
    			
    			result = antm.setCWTestMode(txPower, RFFrequency);
    			
    			displayMessage("");
    			displayMessage("setCWTestMode() result: " + result);
    			
    		}
    	}
    	
    	@Override
    	public void selected() {
    		getCommandParameterView(0).setModeText("", "Tx Power");
    		getCommandParameterView(1).setModeText("", "RF Frequency");
    		getCommandParameterView(2).setModeHidden();
    		getCommandParameterView(3).setModeHidden(); 
    		getCommandParameterView(4).setModeHidden();
    	}
    	
    	@Override
        public void unselected() {

        }
    };
    
    private final CommandHandler sendRawPacket_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;

            String packetText;
            byte[] packet;

            if (antm != null) {
                packetText = getCommandParameterView(0).getValueText().toString();

                try {
                    packet = Utils.hexToByteArray(packetText);
                }
                catch(IllegalArgumentException e) {
                    packet = null;
                }

                displayMessage("");

                if(packet != null) {
                    result = antm.sendRawPacket(packet);

                    displayMessage("sendRawPacket() result: " + result);
                } else {
                    displayMessage("Packet Data invalid. Must be a continuous string comprised only of characters from the set \"[a-zA-Z0-9]\" and contain an even number of characters.");
                }
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Raw Packet Bytes (Hex: \"0103F23B...\")");
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final Command[] commandList = new Command[] {
            new Command("Assign Channel", assignChannel_Handler),
            new Command("Un Assign Channel", unAssignChannel_Handler),
            new Command("Set Channel ID", setChannelID_Handler),
            new Command("Set Channel Period", setChannelPeriod_Handler),
            new Command("Set Channel Search Timeout", setChannelSearchTimeout_Handler),
            new Command("Set Channel Rf Frequency", setChannelRfFrequency_Handler),
            new Command("Set Network Key", setNetworkKey_Handler),
            new Command("Set Transmit Power", setTransmitPower_Handler),
            new Command("Add Channel ID", addChannelID_Handler),
            new Command("Configure Inclusion Exclusion List", configureInclusionExclusionList_Handler),
            new Command("Set Channel Transmit Power", setChannelTransmitPower_Handler), 
            new Command("Set Low Priority Channel Search Timeout", setLowPriorityChannelSearchTimeout_Handler),
            new Command("Set Serial Number Channel ID", setSerialNumberChannelID_Handler),
            new Command("Enable Extended Messages", enableExtendedMessages_Handler), 
            new Command("Enable LED", enableLED_Handler),
            new Command("Enable Crystal", enableCrystal_Handler),
            new Command("Configure Extended Messages", configureExtendedMessages_Handler),
            new Command("Configure Frequency Agility", configureFrequencyAgility_Handler),
            new Command("Set Proximity Search", setProximitySearch_Handler),
            new Command("Set Channel Search Priority", setChannelSearchPriority_Handler),
            new Command("Set USB Descriptor String", setUSBDescriptorString_Handler),
            new Command("Reset System", resetSystem_Handler),
            new Command("Open Channel", openChannel_Handler),
            new Command("Close Channel", closeChannel_Handler),
            new Command("Request Message", requestMessage_Handler),
            new Command("Open Rx Scan Mode", openRxScanMode_Handler),
            new Command("Sleep Message", sleepMessage_Handler),
            new Command("Send Broadcast Data", sendBroadcastData_Handler),
            new Command("Send Acknowledged Data", sendAcknowledgedData_Handler),
            new Command("Send Burst Transfer Data", sendBurstTransferData_Handler),
            new Command("Initalize CW Test Mode", initalizeCWTestMode_Handler),
            new Command("Set CW Test Mode", setCWTestMode_Handler),
            new Command("Send Raw Packet", sendRawPacket_Handler),
    };
}
