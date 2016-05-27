package com.reconinstruments.bluetoothlemanager;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.Map;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.AdvertisingDataType;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;

public class BluetoothLeManager {
	private static BluetoothLeManager sInstance = null;

	private static final int SCAN_PERIOD = 10000;  // 10 Seconds

	public static final int ERROR_CODE_ERROR = 0;
	public static final int ERROR_CODE_SUCCESS = 1;	
	public static final int ERROR_CODE_SCAN_IN_PROGRESS = 2;
	public static final int ERROR_CODE_INVALID_ARG = 3;

	private static DEVMCB mDEVMCB = null;

	private BluetoothLeManager() throws Exception {
		mDEVMCB = new DEVMCB();
	}

	public static synchronized BluetoothLeManager getInstance() throws Exception {
		if(!isBluetoothLeAdaptorEnabled()) {
			throw new Exception("BluetoothLeManager Bluetooth Le is not enabled");
		}
		
		if (sInstance == null) {
			sInstance = new BluetoothLeManager();
		}
			
		return sInstance;
	}
	
	public static boolean isBluetoothLeAdaptorEnabled() {
        // TODO: if (SettingsUtil.getBleOrAnt() == SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) {

		return true; // TODO ask the settings 
	}

	public int scanLeDevice(BluetoothLeScanCallback bluetoothLeScanCallback){
		return mDEVMCB.startLowEnergyDeviceScan(bluetoothLeScanCallback);
	}
	
	public BluetoothLeResponseRawData getBluetoothLeResponseRawData(String address) {
		Map<AdvertisingDataType, byte[]> result = mDEVMCB.mDEVM.queryRemoteLowEnergyDeviceAdvertisingData(new BluetoothAddress(address));
		
		if(result == null) {
			return null;
		}
		
		if(result.size() == 0) {
			return null;
		}
		
		return new BluetoothLeResponseRawData(result); 
	}

	private class DEVMCB implements DEVM.EventCallback {
		public DEVM mDEVM = null;
		
		private long mScanTime = 0;
		
		private WeakReference<BluetoothLeScanCallback> mLeScanCallback = null;

		public DEVMCB() throws Exception {
			try {
				mDEVM = new DEVM(this);
			} catch (ServerNotReachableException e) {
				e.printStackTrace();
				throw new Exception("DEVMCB: Failed to connect to Bluetooth Le Adaptor");
			}
		}
		
		public int startLowEnergyDeviceScan(BluetoothLeScanCallback bluetoothLeScanCallback) {
			if(bluetoothLeScanCallback == null) {
				return ERROR_CODE_INVALID_ARG;
			}
			
			if((System.currentTimeMillis() - mScanTime) < SCAN_PERIOD) {// We are still scanning 
				return ERROR_CODE_SCAN_IN_PROGRESS;
			}

			mScanTime = System.currentTimeMillis();
			int errCode = mDEVMCB.mDEVM.startLowEnergyDeviceScan(SCAN_PERIOD); // return via remoteDeviceFoundEvent
			if(errCode != ERROR_CODE_SUCCESS) {
				mDEVMCB.mDEVM.stopLowEnergyDeviceScan(); // Force a stop of a scan
				errCode = mDEVMCB.mDEVM.startLowEnergyDeviceScan(SCAN_PERIOD); // return via remoteDeviceFoundEvent
				if(errCode != ERROR_CODE_SUCCESS) {
					return ERROR_CODE_ERROR;
				}
			}
			
			mLeScanCallback = new WeakReference<BluetoothLeScanCallback>(bluetoothLeScanCallback);
			return ERROR_CODE_SUCCESS;			
		}

		@Override
		public void remoteDeviceFoundEvent(RemoteDeviceProperties arg0) {
			BluetoothLeScanCallback bluetoothLeScanCallback = mLeScanCallback.get();
			if(bluetoothLeScanCallback == null) {
				return; // No Need to do anything, the object GC
			}
			
			if(!arg0.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
				return;
			}
			
			// TODO: filter remote
			
			int addressType = BluetoothLeDevice.ADDRESS_TYPE_UNKNOWN;
			switch(arg0.lowEnergyAddressType) {
			case PRIVATE_NONRESOLVABLE:
				addressType = BluetoothLeDevice.ADDRESS_TYPE_PRIVATE_NONRESOLVABLE;
				break;
			case PRIVATE_RESOLVABLE:
				addressType = BluetoothLeDevice.ADDRESS_TYPE_PRIVATE_RESOLVABLE;
				break;
			case PUBLIC:
				addressType = BluetoothLeDevice.ADDRESS_TYPE_PUBLIC;
				break;
			case STATIC:
				addressType = BluetoothLeDevice.ADDRESS_TYPE_STATIC;
				break;
			default:
				break;
			
			}
			
			bluetoothLeScanCallback.onLeScan(new BluetoothLeDevice(arg0.deviceAddress.toString(), arg0.deviceName, arg0.lowEnergyRSSI, addressType));
		}

		@Override
		public void deviceAdvertisingStarted() {
			// TODO Auto-generated method stub

		}

		@Override
		public void deviceAdvertisingStopped() {
			// TODO Auto-generated method stub

		}

		@Override
		public void devicePoweredOffEvent() {
			// TODO Auto-generated method stub

		}

		@Override
		public void devicePoweredOnEvent() {
			// TODO Auto-generated method stub

		}

		@Override
		public void devicePoweringOffEvent(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void deviceScanStartedEvent() {
			// TODO Auto-generated method stub

		}

		@Override
		public void deviceScanStoppedEvent() {
			// TODO Auto-generated method stub

		}

		@Override
		public void discoveryStartedEvent() {
			// TODO Auto-generated method stub

		}

		@Override
		public void discoveryStoppedEvent() {
			// TODO Auto-generated method stub

		}

		@Override
		public void localDevicePropertiesChangedEvent(LocalDeviceProperties arg0,
				EnumSet<LocalPropertyField> arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress arg0,
				int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDeviceConnectionStatusEvent(BluetoothAddress arg0,
				int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDeviceDeletedEvent(BluetoothAddress arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDeviceEncryptionStatusEvent(BluetoothAddress arg0,
				int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDevicePairingStatusEvent(BluetoothAddress arg0,
				boolean arg1, int arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties arg0,
				EnumSet<RemotePropertyField> arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDevicePropertiesStatusEvent(boolean arg0,
				RemoteDeviceProperties arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteDeviceServicesStatusEvent(BluetoothAddress arg0,
				boolean arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress arg0,
				BluetoothAddress arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress arg0,
				boolean arg1) {
			// TODO Auto-generated method stub

		}
	}
}
