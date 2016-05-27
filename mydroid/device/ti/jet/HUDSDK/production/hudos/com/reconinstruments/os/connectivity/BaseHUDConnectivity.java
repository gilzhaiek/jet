package com.reconinstruments.os.connectivity;

import org.json.JSONObject;

import android.os.Parcelable;

/** {@hide} */
public abstract class BaseHUDConnectivity implements Parcelable {

    /** {@hide} */
    public BaseHUDConnectivity() {
    }

    /** {@hide} */
    protected abstract void writeToJSON(JSONObject json) throws Exception;

    /** {@hide} */
    protected abstract void readFromJSON(JSONObject json) throws Exception;

    /** {@hide} */
    @Override
    public String toString() {
        try {
            JSONObject json = new JSONObject();
            writeToJSON(json);
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encodes this message into a sequence of bytes
     * @return the resultant byte array
     * {@hide}
     */
    public byte[] getByteArray() {
        return toString().getBytes();
    }
}
