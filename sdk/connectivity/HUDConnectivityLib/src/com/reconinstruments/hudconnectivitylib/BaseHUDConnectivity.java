package com.reconinstruments.hudconnectivitylib;

import org.json.JSONObject;

public abstract class BaseHUDConnectivity {

    public BaseHUDConnectivity() {
    }

    protected abstract void writeToJSON(JSONObject json) throws Exception;

    protected abstract void readFromJSON(JSONObject json) throws Exception;

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
     */
    public byte[] getByteArray() {
        return toString().getBytes();
    }
}
