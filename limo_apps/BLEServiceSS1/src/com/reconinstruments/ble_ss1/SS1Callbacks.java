package com.reconinstruments.ble_ss1;

import java.util.EnumSet;

import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.GATM.AttributeProtocolErrorType;
import com.stonestreetone.bluetopiapm.GATM.ConnectionType;
import com.stonestreetone.bluetopiapm.GATM.RequestErrorType;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager.ClientEventCallback;

public class SS1Callbacks {


	TheBLEService bleService;
	public SS1Callbacks(TheBLEService bleService){
		this.bleService = bleService;
	}
	
	private static final String TAG = "TheBLEService";

	final ClientEventCallback genericAttributeClientEventCallback = new ClientEventCallback() {

		// We need this too
		@Override
		public void connectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {
			Log.v(TAG,"Device connected");

			Log.v(TAG,"address: "+remoteDeviceAddress);
			Log.v(TAG,"type: "+connectionType);
			Log.v(TAG,"MTU: "+MTU);

			bleService.gattConnected(remoteDeviceAddress);
		}

		@Override
		public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress) {
			Log.v(TAG,"disconnectedEvent");

			Log.v(TAG,"address: "+remoteDeviceAddress);
			Log.v(TAG,"type: "+connectionType);

			bleService.gattDisconnected(remoteDeviceAddress);
		}

		@Override
		public void connectionMTUUpdateEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {

			Log.v(TAG,"connectionMTUUpdateEvent");
			Log.v(TAG,"MTU: "+MTU);
			Log.v(TAG,"address: "+remoteDeviceAddress);
			Log.v(TAG,"connection type: "+connectionType);
		}

		// We need this too.
		@Override
		public void handleValueEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean handleValueIndication, int attributeHandle, byte[] attributeValue) {
			//Log.d(TAG,"handleValueIndication");

			//Log.v(TAG,"address: "+remoteDeviceAddress);
			//Log.v(TAG,"connection type: "+connectionType);
			//Log.v(TAG,"handleValueIndication: "+handleValueIndication);
			//Log.d(TAG,"attributeHandle: "+Integer.toHexString(attributeHandle));

			//if(attributeValue != null) {
				//Log.d(TAG,"attributeValue: "+BLEUtils.byteArrayToHex(attributeValue));
			//}

			bleService.receivedGattData(remoteDeviceAddress,attributeHandle,attributeValue);
		}

		@Override
		public void readResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, boolean isFinal, byte[] value) {
			Log.v(TAG,"readResponseEvent");

			Log.v(TAG,"address: "+remoteDeviceAddress);
			Log.v(TAG,"connection type: "+connectionType);
			Log.v(TAG,"transactionID: "+transactionID);
			Log.v(TAG,"handle: "+handle);
			Log.v(TAG,"isFinal: "+isFinal);

			if(value != null) {
				Log.v(TAG,new String(value));
			}
		}

		@Override
		public void writeResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle) {
			Log.v(TAG,"writeResponseEvent");

			Log.v(TAG,"address: "+remoteDeviceAddress);
			Log.v(TAG,"connection type: "+connectionType);
			Log.v(TAG,"transactionID: "+transactionID);
			Log.v(TAG,"handle: "+handle);
		}

		@Override
		public void errorResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, RequestErrorType requestErrorType, AttributeProtocolErrorType attributeProtoccolErrorType) {
			Log.v(TAG,"errorResponseEvent");
			Log.v(TAG,"requestErrorType: "+requestErrorType);
			Log.v(TAG,"address: "+remoteDeviceAddress);
			Log.v(TAG,"connection type: "+connectionType);
			Log.v(TAG,"transactionID: "+transactionID);
			Log.v(TAG,"handle: "+handle);

			if(requestErrorType == RequestErrorType.ERROR_RESPONSE) {
				Log.v(TAG,"attributeProtoccolErrorType: "+attributeProtoccolErrorType);
			}
		}
	};
	

	// Callbacks
	final EventCallback deviceEventCallback = new EventCallback() {

		@Override
		public void devicePoweredOnEvent() {Log.v(TAG,"devicePoweredOnEvent");}
		@Override
		public void devicePoweringOffEvent(int poweringOffTimeout) {Log.v(TAG,"devicePoweringOffEvent");}
		@Override
		public void devicePoweredOffEvent() {Log.v(TAG,"devicePoweredOffEvent");}

		@Override
		public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields) {
			Log.v(TAG,"localDevicePropertiesChangedEvent");

			for(LocalPropertyField localPropertyField: changedFields) {
				switch(localPropertyField) {
				case DISCOVERABLE_MODE:
					if(localProperties.discoverableMode)
						Log.v(TAG,"Device is Discoverable");
					else
						Log.v(TAG,"Device is Not Discoverable");

					Log.v(TAG,"Discoverable Mode Timeout: " + localProperties.discoverableModeTimeout);
					break;
				case CLASS_OF_DEVICE:
				case DEVICE_NAME:
				case CONNECTABLE_MODE:
				case PAIRABLE_MODE:
				case DEVICE_FLAGS:
					break;
				}
			}
		}

		@Override
		public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
			Log.v(TAG,"remoteDeviceServicesStatusEvent");}
		@Override
		public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
			Log.v(TAG,"remoteLowEnergyDeviceServicesStatusEvent: "+(success?"Success":"Failure"));}
		@Override
		public void discoveryStartedEvent() {Log.v(TAG,"discoveryStartedEvent");}
		@Override
		public void discoveryStoppedEvent() {
			Log.v(TAG,"discoveryStoppedEvent");

			bleService.discoveryStopped();
		}

		@Override
		public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {
			// if we are looking new devices mBondStatus == 0,
			// then we dont care about new found devices. We will
			// perform pairing at remote Device property change.
			Log.d(TAG,"remoteDeviceFoundEvent");

			if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
				Log.d(TAG,"Device Address  : " + deviceProperties.deviceAddress.toString());
				Log.v(TAG,"Class of Device : " + Integer.toHexString(deviceProperties.classOfDevice));
				Log.d(TAG,"Device Name     : " + deviceProperties.deviceName);
				Log.v(TAG,"RSSI            : " + deviceProperties.rssi);
				Log.v(TAG,"Transmit power  : " + deviceProperties.transmitPower);
				Log.v(TAG,"Sniff Interval  : " + deviceProperties.sniffInterval);

				if(deviceProperties.applicationData != null) {
					Log.v(TAG,"Friendly Name   : " + deviceProperties.applicationData.friendlyName);
					Log.v(TAG,"Application Info: " + deviceProperties.applicationData.applicationInfo);
				}

				for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
					Log.v(TAG,"Remote Device Flag: " + remoteDeviceFlag.toString());

				bleService.newBleDeviceFound(deviceProperties);
			}
		}

		@Override
		public void remoteDevicePropertiesStatusEvent(boolean success, RemoteDeviceProperties deviceProperties) {	
			Log.d(TAG,"remoteDevicePropertiesStatusEvent: "+(success?"Success":"Failure"));

			if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {

				Log.d(TAG,"Device Address    : " + deviceProperties.deviceAddress.toString());
				Log.v(TAG,"Class of Device   : " + Integer.toHexString(deviceProperties.classOfDevice));
				Log.d(TAG,"Device Name       : " + deviceProperties.deviceName);
				Log.v(TAG,"RSSI              : " + deviceProperties.rssi);
				Log.v(TAG,"Transmit power    : " + deviceProperties.transmitPower);
				Log.v(TAG,"Sniff Interval    : " + deviceProperties.sniffInterval);

				if(deviceProperties.applicationData != null) {
					Log.v(TAG,"Friendly Name     : " + deviceProperties.applicationData.friendlyName);
					Log.v(TAG,"Application Info  : " + deviceProperties.applicationData.applicationInfo);
				}

				for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
					Log.v(TAG,"Remote Device Flag: " + remoteDeviceFlag.toString());
			}
		}
		
		// We need this one.
		@Override
		public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties deviceProperties, EnumSet<RemotePropertyField> changedFields) {
			Log.d(TAG,"remoteDevicePropertiesChangedEvent: "+deviceProperties.deviceAddress.toString());
			if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {

				bleService.devicePropertyChanged(deviceProperties);

				for(RemotePropertyField remotePropertyField: changedFields) {
					switch(remotePropertyField) {
					case DEVICE_FLAGS:
						for(RemoteDeviceFlags remoteDeviceFlags: deviceProperties.remoteDeviceFlags)
							Log.d(TAG,"Remote Device Flag: " + remoteDeviceFlags.name());
						break;
					case CLASS_OF_DEVICE:
						Log.d(TAG,"Class of Device: " + Integer.toHexString(deviceProperties.classOfDevice));
						break;
					case DEVICE_NAME:
						Log.d(TAG,"Device Name: " + deviceProperties.deviceName);
						break;
					case APPLICATION_DATA:
						if(deviceProperties.applicationData != null) {
							Log.d(TAG,"Friendly Name   : " + deviceProperties.applicationData.friendlyName);
							Log.v(TAG,"Application Info: " + deviceProperties.applicationData.applicationInfo);
						} else {
							Log.v(TAG,"Application Data is null");
						}
						break;
					case RSSI:
						Log.v(TAG,"RSSI: " + deviceProperties.rssi);
						break;
					case PAIRING_STATE:
						if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_PAIRED))
							Log.d(TAG,"Device Paired");
						else
							Log.d(TAG,"Device Unpaired");

						break;
						//TODO: sometimes only this is called and not the GATM connection callback, find out why, if it is a bug, notify connection from here
					case CONNECTION_STATE:
						if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_CONNECTED))
							Log.d(TAG,"Device Connected");
						else
							Log.d(TAG,"Device Disconnected");

						break;
					case ENCRYPTION_STATE:
						if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED))
							Log.d(TAG,"Device Link Encrypted");
						else
							Log.d(TAG,"Device Link Unencrypted");

						break;
					case SNIFF_STATE:
						if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE))
							Log.d(TAG,"Device Link in Sniff Mode");
						else
							Log.d(TAG,"Device Link not in Sniff Mode");

						break;
					case SERVICES_STATE:
						if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SERVICES_KNOWN))
							Log.d(TAG,"Services are Known");
						else
							Log.d(TAG,"Services are not Known");

						break;
					default:
						break;
					}
				}
			}
		}

		@Override
		public void remoteDevicePairingStatusEvent(BluetoothAddress remoteDevice, boolean success, int authenticationStatus) {
			Log.v(TAG,"remoteDevicePairingStatusEvent");

			Log.v(TAG,(success?"Success":"Failure"));
			Log.v(TAG,remoteDevice.toString());
			Log.v(TAG,"Authentication Status: " + (authenticationStatus & 0x7FFFFFFF) + (((authenticationStatus & 0x80000000) != 0) ? " (LE)" : "(BR/EDR)"));
		}

		@Override
		public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {
			Log.v(TAG,"remoteDeviceEncryptionStatusEvent");
			Log.v(TAG,remoteDevice.toString());
			Log.v(TAG,"Encryption Status: " + status);
		}

		@Override
		public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice) {
			Log.v(TAG,"remoteDeviceDeletedEvent");}

		@Override
		public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status) {
			Log.v(TAG,"remoteDeviceConnectionStatusEvent");
			Log.v(TAG,remoteDevice.toString());
			Log.v(TAG,"Connection Status: " + status);
			
			bleService.bleConnectionStatusChanged(remoteDevice,status);
		}

		@Override
		public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status) {
			Log.v(TAG,"remoteDeviceAuthenticationStatusEvent");
			Log.v(TAG,remoteDevice.toString());
			Log.v(TAG,"Authentication Status: " + status);
		}

		@Override
		public void deviceScanStartedEvent() {
			Log.v(TAG,"deviceScanStartedEvent");
		}

		@Override
		public void deviceScanStoppedEvent() {
			Log.v(TAG,"deviceScanStoppedEvent");
		}

		@Override
		public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress PriorResolvableDeviceAddress, BluetoothAddress CurrentResolvableDeviceAddress) {
			Log.v(TAG,"remoteLowEnergyDeviceAddressChangedEvent");
			Log.v(TAG,"remoteLowEnergyDeviceAddressChangedEvent: \n"+
					"    PriorResolvableDeviceAddress = "+
					PriorResolvableDeviceAddress.toString()+
					"\n    CurrentResolvableDeviceAddress = "+
					CurrentResolvableDeviceAddress.toString());
		}

		@Override
		public void deviceAdvertisingStarted() {
		// TODO Auto-generated method stub

		}

		@Override
		public void deviceAdvertisingStopped() {
		// TODO Auto-generated method stub

		}
	};
};
