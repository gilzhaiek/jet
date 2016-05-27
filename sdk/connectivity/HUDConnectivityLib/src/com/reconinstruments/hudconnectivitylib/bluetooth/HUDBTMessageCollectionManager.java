package com.reconinstruments.hudconnectivitylib.bluetooth;

import android.util.Log;
import android.util.SparseArray;

public class HUDBTMessageCollectionManager {
    private final static String TAG = "HUDBTMessageCollectionManager";

    protected static final boolean DEBUG = true;

    private static final byte MAX_REQUEST_IDS = (byte) 0x7E;
    private static final byte ERROR_REQUEST_ID = (byte) 0x7F;
    private static boolean mRequestIDsPool[] = new boolean[MAX_REQUEST_IDS];

    static {
        for (int i = 0; i < MAX_REQUEST_IDS; i++) {
            mRequestIDsPool[i] = true;
        }
    }

    private static SparseArray<HUDBTMessage> mPendingHUDBTResponse = new SparseArray<HUDBTMessage>();
    private static SparseArray<HUDBTMessage> mWorkingHUDBTMessage = new SparseArray<HUDBTMessage>();

    public static HUDBTMessage createPendingBTResponse(byte[] header) throws Exception {
        byte requestID = obtainRequestID();
        if (requestID == ERROR_REQUEST_ID) {
            throw new Exception("Can't assign a new request ID - buffer is full WTF");
        }
        if (mPendingHUDBTResponse.get(requestID) != null) {
            Log.e(TAG, "Dumping leftover HUDBTRequest from requestID " + requestID);
        }

        HUDBTMessage btMessage = new HUDBTMessage(requestID);
        mPendingHUDBTResponse.put(requestID, btMessage);

        HUDBTHeaderFactory.setRequestID(header, requestID);

        return btMessage;
    }

    public static void recycleIncompleteRequestID(HUDBTMessage btResponse){
        if(btResponse == null){
            return;
        }

        byte requestID = btResponse.getRequestID();
        freeRequestID(requestID);
    }

    //Only used for DEBUG purpose
    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static HUDBTMessage addData(int incomingSessionID, byte[] data, int bytes, ExcessDataAgent excessDataAgent) {
        if (DEBUG)
            Log.d(TAG, "addData ( incomingSessionID: " + incomingSessionID + " bytes: " + bytes + ")");
        boolean isHeader = HUDBTHeaderFactory.isHeader(data);
        HUDBTMessage btMessage = null;
        if (isHeader) {
            if(DEBUG) {
                String headerString = byteArrayToHex(data);
                Log.d(TAG, "HeaderByteInHex: " + headerString);
            }
            byte requestID = HUDBTHeaderFactory.getRequestID(data);
            if (HUDBTHeaderFactory.getMessageType(data) == HUDBTHeaderFactory.MESSAGE_TYPE__RESPONSE) {
                btMessage = mWorkingHUDBTMessage.get(incomingSessionID);
                if (btMessage != null && isHeader) {
                    Log.w(TAG, "Dumping leftover Working HUDBTMessage from incomingSessionID " + incomingSessionID);
                    mWorkingHUDBTMessage.remove(incomingSessionID);
                    freeRequestID(btMessage.getRequestID());
                }
                btMessage = mPendingHUDBTResponse.get(requestID);
                if (btMessage == null) {
                    Log.e(TAG, "No pending response found for id=" + requestID);
                    return null;
                }
                mPendingHUDBTResponse.remove(requestID);
            } else {
                btMessage = new HUDBTMessage(requestID);
            }
            mWorkingHUDBTMessage.put(incomingSessionID, btMessage);
        } else {
            btMessage = mWorkingHUDBTMessage.get(incomingSessionID);
            if (btMessage == null) {
                if(!excessDataAgent.smartAppendData(data, bytes)){
                    Log.e(TAG, "No working message found for sessionID = " + incomingSessionID);
                }
                return null;
            }
        }

        btMessage.addData(data, bytes, isHeader, excessDataAgent);

        if (btMessage.isComplete()) {
            if(HUDBTHeaderFactory.isResponse(btMessage.getHeader())){
                synchronized (btMessage) {
                    btMessage.notify();
                }
                freeRequestID(HUDBTHeaderFactory.getRequestID(btMessage.getHeader()));
                mWorkingHUDBTMessage.remove(incomingSessionID);
                return null;
            } else {
                mWorkingHUDBTMessage.remove(incomingSessionID);   
                return btMessage;    
            }
        } else {
            return null;
        }
    }

    private static byte obtainRequestID() {
        synchronized (mRequestIDsPool) {
            for (byte i = 0; i < MAX_REQUEST_IDS; i++) {
                if (mRequestIDsPool[i]) {
                    mRequestIDsPool[i] = false;
                    if(DEBUG)Log.d(TAG, "ObtainRequestID: " + i);
                    return i;
                }
            }
        }

        return ERROR_REQUEST_ID;
    }

    private static void freeRequestID(byte id) {
        synchronized (mRequestIDsPool) {
            if (id < MAX_REQUEST_IDS) {
                mRequestIDsPool[id] = true;
                if(DEBUG)Log.d(TAG, "freeRequestID: " + (int) id);
            }
        }
    }
}
