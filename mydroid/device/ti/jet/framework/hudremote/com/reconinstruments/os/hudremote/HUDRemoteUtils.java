/*
 * Copyright (C) 2015 Recon Instruments
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reconinstruments.os.hudremote;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @hide
 */
final public class HUDRemoteUtils {
    private static final String TAG = "BluetoothUtils";
    static final int BD_ADDR_LEN = 6; // bytes
    //static final int BD_UUID_LEN = 16; // bytes
    static String BLE_FILE_LOC = "/data/system/BLE.RIB";

    public static String getAddressStringFromByte(byte[] address) {
        if (address == null || address.length !=6) {
            return null;
        }

        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                address[0], address[1], address[2], address[3], address[4],
                address[5]);
    }

    public static byte[] getBytesFromAddress(String address) {
        int i, j = 0;
        byte[] output = new byte[BD_ADDR_LEN];

        for (i = 0; i < address.length();i++) {
            if (address.charAt(i) != ':') {
                output[j] = (byte) Integer.parseInt(address.substring(i, i+2), 16);
                j++;
                i++;
            }
        }

        return output;
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

    static private byte[] inverseByteArray(byte[] ba) {
        byte[] newarray = new byte[ba.length];
        for (int i = 0; i< ba.length; i++) {
            newarray[ba.length - i -1] = ba[i];
        }
        return newarray;
    }

    static public byte[] readBLEFile() {
        File srcFile = new File(BLE_FILE_LOC);
        if (!srcFile.exists()) {
            Log.d(TAG,"no file");
            return null;
        }
        InputStream inTape = null;
        byte[] result = null;
        try {
            inTape = new FileInputStream(srcFile);
            if (inTape == null) {
                return null;
            }
            result  = readTape(BD_ADDR_LEN, inTape);
            if (result != null) {
                result = inverseByteArray(result);
                inTape.close();
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

    static public void writeToBLEFile(byte[] the_bytes) {
        File srcFile = new File(BLE_FILE_LOC);
        OutputStream os = null;

        try {
            os = new FileOutputStream(srcFile);
            os.write(inverseByteArray(the_bytes));
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void clearBLEFile() {
        File srcFile = new File(BLE_FILE_LOC);
        if (srcFile.exists()) {
            srcFile.delete();
        }
    }
}
