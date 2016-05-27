package com.reconinstruments.hudconnectivity;

import org.json.JSONObject;

public abstract class BaseHUDConnectivity {
    public BaseHUDConnectivity(byte[] bytes) throws Exception {
        this(new JSONObject(new String(bytes)));
    }

    public BaseHUDConnectivity(JSONObject json) throws Exception {
        readFromJSON(json);
    }

    public BaseHUDConnectivity() {
    }

    protected abstract void writeToJSON(JSONObject json) throws Exception;

    protected abstract void readFromJSON(JSONObject json) throws Exception;

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

    public byte[] getByteArray() throws Exception {
        return toString().getBytes();
    }
}
