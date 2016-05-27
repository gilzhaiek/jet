package com.reconinstruments.bluetoothlemanager;


public class BluetoothLeDevice {
	/** @hide */
	private String mAddress;
	
	/** @hide */
	private String mDeviceName;
	
	/** @hide */
	private int mRSSI;
	
	/** @hide */
	private int mAddressType;
	
	
	/**
	 * BLE Address is unknown
	 */
	public static final int ADDRESS_TYPE_UNKNOWN = 0;
	
	/**
	 * BLE Address which is randomly generated and used only on a single connection or for 
	 * specific types of actions as prescribed by the Bluetooth 4.0 spec.
	 */
	public static final int ADDRESS_TYPE_PRIVATE_NONRESOLVABLE = 1;
	
	/**
	 * BLE Address which is randomly generated and used only on a single connection but which may
	 * be used to identify a device when combined with that device's Identity Resolving Key (IRK).
	 */
	public static final int ADDRESS_TYPE_PRIVATE_RESOLVABLE = 2;
	
	/**
	 * BLE Address which is permanently assigned to the device in accordance with Section 9.2 of
	 * the IEEE 802-2001 standard.
	 */
	public static final int ADDRESS_TYPE_PUBLIC = 3;
	
	/**
	 * BLE Address which is randomly generated and may only be (optionally) changed when the device
	 * is power-cycled.
	 */
	public static final int ADDRESS_TYPE_STATIC = 4;
	
	/** @hide */
	public BluetoothLeDevice(String address, String deviceName, int rssi, int addressType) {
		mAddress = address;
		mDeviceName = deviceName;
		mRSSI = rssi;
		mAddressType = addressType;
	}
	
	/**
	 * Returns the hardware address of this Bluetooth Le device.
	 * <p> For example, "00:11:22:AA:BB:CC".
	 * @return Bluetooth Le hardware address as string
	 */
	public String getAddress() {
		return mAddress;
	}
	
	/**
	 * @return The name reported by the Bluetooth Le device
	 */
	public String getDeviceName() {
		return mDeviceName;
	}
	
	/**
	 * Returns the RSSI (Received Signal Strength Indicator) - the power present in a the Bluetooth Le Device
	 * @return The RSSI value for the remote device as reported by the Bluetooth hardware.
	 */
	public int getRSSI() {
		return mRSSI;
	}
	
	/**
	 * @return the integer of this type as defined in BluetoothLeDevice.ADDRESS_TYPE_XXX
	 */
	public int getAddressType() {
		return mAddressType;
	}
}
