package com.reconinstruments.ble_ss1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class BLEUtils {

	//    public static String BLE_FILE_LOC = "/sdcard/BLE.RIB";
	public static String BLE_FILE_LOC = "/data/system/BLE.RIB";
	public static final int MAC_ADDRESS_SIZE = 6;
	
	// Auxiliary functions
	static public void writeToBLEFile(byte[] the_bytes) {
		Log.d("BLEUtils","writeToBLEFile: "+byteArrayToHex(the_bytes));
		File srcFile = new File(BLE_FILE_LOC);
		OutputStream os = null;

		try {
			os = new FileOutputStream(srcFile);
			os.write(BLEUtils.inverbytearray(the_bytes));
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	static public byte[] readBLEFile(){
		Log.d("BLEUtils","readBLEFile");
		File srcFile = new File(BLE_FILE_LOC);
		if (!srcFile.exists()) {
			Log.d("BLEUtils","no file");
			return null;
		}
		InputStream inTape = null;
		byte[] result = null;
		try {
		    inTape = new FileInputStream(srcFile);
		    if (inTape == null) {
			return null;
		    }
		    result  = readTape(MAC_ADDRESS_SIZE, inTape);
		    if (result != null) {
			result = inverbytearray(result);
			inTape.close();
			Log.d("BLEUtils","readBLEFile: "+byteArrayToHex(result));
		    }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    try {
			inTape.close();
		    } catch (IOException e) {
		    }
		}
		return result;
	}
	static public void clearBLEFile(){
		Log.d("BLEUtils","clearBLEFile");
		File srcFile = new File(BLE_FILE_LOC);
		if (srcFile.exists()) {
			srcFile.delete();
		}
	}
	static private byte[] readTape(int numBytes, InputStream inTape)
			throws FileNotFoundException, IOException {
		byte[] buf = new byte[numBytes];
		try {
			int size;
			size = inTape.read(buf);
			if (size == numBytes) {
				return buf;
			}
		} finally {

		}
		return null;
	}

	static public String macAddresstoString(byte[] macaddress) {
		return "" + " 0x"+(int)macaddress[0] +
				" 0x"+(int)macaddress[1] + " 0x"+(int)macaddress[2] +
				" 0x"+(int)macaddress[3] + " 0x"+(int)macaddress[4] +
				" 0x"+(int)macaddress[5];
	}
	static public byte[] inverbytearray(byte[] ba) {
		byte[] newarray = new byte[ba.length];
		for (int i = 0; i< ba.length; i++) {
			newarray[ba.length - i -1] = ba[i];
		}
		return newarray;
	}
	
	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for(byte b: a)
			sb.append(String.format("%02X", b&0xff));
		return sb.toString();
	}
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
}
