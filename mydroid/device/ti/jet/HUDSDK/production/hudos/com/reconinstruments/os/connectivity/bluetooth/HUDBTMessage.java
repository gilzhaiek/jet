package com.reconinstruments.os.connectivity.bluetooth;

import android.util.Log;

/** {@hide}*/
public class HUDBTMessage {

    private final static String TAG = "HUDBTMessage";
    private final static boolean DEBUG = true;

    static final public int HEADER = 1 << 0;
    static final public int BODY = 1 << 1;
    static final public int PAYLOAD = 1 << 2;

    private final byte mRequestID;
    private byte[] mHeader = null;
    private byte[] mPayload = null;
    private byte[] mBody = null;
    private int mBodyCurrentLength = 0;
    private int mPayloadCurrentLength = 0;

    public HUDBTMessage(byte requestID) {
        mRequestID = requestID;
    }

    private int lengthToCopy(int totalIncomingBytes, int consumedIncomingBytes, int targetLength, int targetCurrentLength) {
        int stillMissingBytes = targetLength - targetCurrentLength;
        int incomingBytesLeft = totalIncomingBytes - consumedIncomingBytes;
        return stillMissingBytes < incomingBytesLeft ? stillMissingBytes : incomingBytesLeft;
    }

    public void addData(byte[] data, int bytes, boolean isHeader, ExcessDataAgent excessDataAgent) {
        if (data == null) {
            Log.e(TAG, "Data is null");
            return;
        }
        if (mHeader == null) {
            if (!isHeader) {
                Log.e(TAG, "Expected Header, got unrecognized data byte array");
                return;
            }
        } else if (isHeader) {
            Log.e(TAG, "Received a new Header, while didn't complete the previous transaction");
        }

        int consumedByte = 0;

        if (isHeader) {
            try {
                mHeader = new byte[HUDBTHeaderFactory.HEADER_LENGTH];
                System.arraycopy(data, 0, mHeader, 0, mHeader.length);
                consumedByte += mHeader.length;

                if (HUDBTHeaderFactory.hasPayload(mHeader)) {
                    mPayload = new byte[HUDBTHeaderFactory.getPayloadLength(mHeader)];
                    mPayloadCurrentLength = 0;
                }

                if (HUDBTHeaderFactory.hasBody(mHeader)) {
                    mBody = new byte[HUDBTHeaderFactory.getBodyLength(mHeader)];
                    mBodyCurrentLength = 0;
                }

                if (bytes <= consumedByte) {
                    excessDataAgent.resetData();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace(); /* shouldn't happen */
            }
        }

        if (!isPayloadComplete()) {
            int lengthToCopy = lengthToCopy(bytes, consumedByte, mPayload.length, mPayloadCurrentLength);
            System.arraycopy(data, consumedByte, mPayload, mPayloadCurrentLength, lengthToCopy);
            consumedByte += lengthToCopy;
            mPayloadCurrentLength += lengthToCopy;
            if (bytes <= consumedByte) {
                excessDataAgent.resetData();
                return;
            }
        }

        if (!isBodyComplete()) {
            int lengthToCopy = lengthToCopy(bytes, consumedByte, mBody.length, mBodyCurrentLength);
            System.arraycopy(data, consumedByte, mBody, mBodyCurrentLength, lengthToCopy);
            consumedByte += lengthToCopy;
            mBodyCurrentLength += lengthToCopy;
            if (bytes <= consumedByte) {
                excessDataAgent.resetData();
                return;
            }
        }
        if (bytes > consumedByte) {
            excessDataAgent.setData(data, consumedByte, bytes - consumedByte);
        }
    }

    public byte getRequestID() {
        return mRequestID;
    }

    public byte[] getHeader() {
        return mHeader;
    }

    public byte[] getBody() {
        return mBody;
    }

    public byte[] getPayload() {
        return mPayload;
    }

    private boolean isBodyComplete() {
        if (mBody == null) return true;
        if (DEBUG) Log.d(TAG, "isBodyComplete() mBodyCurrentLength:" + mBodyCurrentLength + " mBody: " + mBody.length);
        return (mBodyCurrentLength >= mBody.length);
    }

    private boolean isPayloadComplete() {
        if (mPayload == null) return true;
        return (mPayloadCurrentLength >= mPayload.length);
    }

    public boolean isComplete() {
        try {
            if (mHeader == null) {
                return false;

            }
            if (!isBodyComplete())
                return false;

            if (!isPayloadComplete())
                return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
