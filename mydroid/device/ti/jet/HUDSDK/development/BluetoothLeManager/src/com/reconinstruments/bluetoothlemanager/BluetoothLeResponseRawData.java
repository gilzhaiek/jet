package com.reconinstruments.bluetoothlemanager;

import java.util.Map;

import android.util.SparseArray;

import com.stonestreetone.bluetopiapm.DEVM.AdvertisingDataType;

public class BluetoothLeResponseRawData {
	public static final int RESPONSE_RAW_DATA_TYPE_FLAGS                                      = 0x01;
	public static final int RESPONSE_RAW_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL                = 0x02;
	public static final int RESPONSE_RAW_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE               = 0x03;
	public static final int RESPONSE_RAW_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL                = 0x04;
	public static final int RESPONSE_RAW_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE               = 0x05;
	public static final int RESPONSE_RAW_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL               = 0x06;
	public static final int RESPONSE_RAW_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE              = 0x07;
	public static final int RESPONSE_RAW_DATA_TYPE_LOCAL_NAME_SHORTENED                       = 0x08;
	public static final int RESPONSE_RAW_DATA_TYPE_LOCAL_NAME_COMPLETE                        = 0x09;
	public static final int RESPONSE_RAW_DATA_TYPE_TX_POWER_LEVEL                             = 0x0A;
	public static final int RESPONSE_RAW_DATA_TYPE_CLASS_OF_DEVICE                            = 0x0D;
	public static final int RESPONSE_RAW_DATA_TYPE_SIMPLE_PAIRING_HASH_C                      = 0x0E;
	public static final int RESPONSE_RAW_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R                = 0x0F;
	public static final int RESPONSE_RAW_DATA_TYPE_SECURITY_MANAGER_TK                        = 0x10;
	public static final int RESPONSE_RAW_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS         = 0x11;
	public static final int RESPONSE_RAW_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE            = 0x12;
	public static final int RESPONSE_RAW_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS  = 0x14;
	public static final int RESPONSE_RAW_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS = 0x15;
	public static final int RESPONSE_RAW_DATA_TYPE_SERVICE_DATA                               = 0x16;
	public static final int RESPONSE_RAW_DATA_TYPE_PUBLIC_TARGET_ADDRESS                      = 0x17;
	public static final int RESPONSE_RAW_DATA_TYPE_RANDOM_TARGET_ADDRESS                      = 0x18;
	public static final int RESPONSE_RAW_DATA_TYPE_APPEARANCE                                 = 0x19;
	public static final int RESPONSE_RAW_DATA_TYPE_MANUFACTURER_SPECIFIC                      = 0xFF;

	private SparseArray<byte[]> mRawData = new SparseArray<byte[]>(); 

	/** @hide */
	public BluetoothLeResponseRawData(Map<AdvertisingDataType, byte[]> rawData) {
		for (Map.Entry<AdvertisingDataType, byte[]> entry : rawData.entrySet())
		{
			switch (entry.getKey()){
			case APPEARANCE:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_APPEARANCE, entry.getValue());
				break;
			case CLASS_OF_DEVICE:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_CLASS_OF_DEVICE, entry.getValue());
				break;
			case FLAGS:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_FLAGS, entry.getValue());
				break;
			case LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS, entry.getValue());
				break;
			case LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS, entry.getValue());
				break;
			case LOCAL_NAME_COMPLETE:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_LOCAL_NAME_COMPLETE, entry.getValue());
				break;
			case LOCAL_NAME_SHORTENED:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_LOCAL_NAME_SHORTENED, entry.getValue());
				break;
			case MANUFACTURER_SPECIFIC:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_MANUFACTURER_SPECIFIC, entry.getValue());
				break;
			case PUBLIC_TARGET_ADDRESS:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_PUBLIC_TARGET_ADDRESS, entry.getValue());
				break;
			case RANDOM_TARGET_ADDRESS:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_RANDOM_TARGET_ADDRESS, entry.getValue());
				break;
			case SECURITY_MANAGER_OUT_OF_BAND_FLAGS:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS, entry.getValue());
				break;
			case SECURITY_MANAGER_TK:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_SECURITY_MANAGER_TK, entry.getValue());
				break;
			case SERVICE_CLASS_UUID_128_BIT_COMPLETE:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE, entry.getValue());
				break;
			case SERVICE_CLASS_UUID_128_BIT_PARTIAL:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL, entry.getValue());
				break;
			case SERVICE_CLASS_UUID_16_BIT_COMPLETE:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE, entry.getValue());
				break;
			case SERVICE_CLASS_UUID_16_BIT_PARTIAL:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL, entry.getValue());
				break;
			case SERVICE_CLASS_UUID_32_BIT_COMPLETE:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE, entry.getValue());
				break;
			case SERVICE_CLASS_UUID_32_BIT_PARTIAL:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL, entry.getValue());
				break;
			case SERVICE_DATA:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_SERVICE_DATA, entry.getValue());
				break;
			case SIMPLE_PAIRING_HASH_C:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_SIMPLE_PAIRING_HASH_C, entry.getValue());
				break;
			case SIMPLE_PAIRING_RANDOMIZER_R:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R, entry.getValue());
				break;
			case SLAVE_CONNECTION_INTERVAL_RANGE:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE, entry.getValue());
				break;
			case TX_POWER_LEVEL:
				mRawData.put(RESPONSE_RAW_DATA_TYPE_TX_POWER_LEVEL, entry.getValue());
				break;
			default:
				break;

			}
		}
	}
	
	/**
	 * @return a sparse array of the Byte Array of the raw data where the key is based on BluetoothLeResponseRawData.RESPONSE_RAW_DATA_TYPE_XXX 
	 */
	public SparseArray<byte[]> getBluetoothLeResponseRawData() {
		return mRawData;
	}
}
