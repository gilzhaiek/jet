package com.reconinstruments.hudconnectivity.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import com.reconinstruments.hudconnectivity.BaseHUDConnectivity;

public abstract class HUDHttpMessage extends BaseHUDConnectivity {
    /**
     * @hide
     */
    private static final String JSON_ATTR_HEADERS = "headers";

    /**
     * @hide
     */
    private Map<String, List<String>> mHeaders = null;
    private byte[] mBody = null;

    public HUDHttpMessage(byte[] bytes) throws Exception {
        super(bytes);
    }

    protected HUDHttpMessage(Map<String, List<String>> headers, byte[] body) {
        super();
        this.mHeaders = headers;
        if (body != null) {
            mBody = body;
        }
    }

    /**
     * @param headers the message name/value headers
     */
    public void setHeaders(Map<String, List<String>> headers) {
        this.mHeaders = headers;
    }

    /**
     * Set a new body
     *
     * @param body the message body
     */
    public void setBody(byte[] body) {
        if (body != null) {
            this.mBody = body;
        }
    }

    /**
     * @return the message name/value headers
     */
    public Map<String, List<String>> getHeaders() {
        return this.mHeaders;
    }

    /**
     * @return true if there is a body of size > 0
     */
    public boolean hasBody() {
        if (this.mBody == null) {
            return false;
        }

        return (this.mBody.length > 0);
    }

    /**
     * @return the message body in index
     */
    public byte[] getBody() {
        return this.mBody;
    }

    /**
     * @return the message body in string format<br>basically calling <code>return new String(getBody());</code>
     */
    public String getBodyString() {
        if (hasBody()) {
            return new String(getBody());
        }
        return null;
    }

    @Override
    protected void writeToJSON(JSONObject json) throws Exception {
        // Headers
        if (this.mHeaders != null) {
            JSONObject headers = new JSONObject();
            for (Entry<String, List<String>> header : this.mHeaders.entrySet()) {
                if (header.getKey() == null) {
                    // This is the first line of the response, skip it.
                    // See: http://developer.android.com/reference/java/net/URLConnection.html#getHeaderFields()
                    continue;
                }
                headers.put(header.getKey(), new JSONArray(header.getValue()));
            }
            json.put(JSON_ATTR_HEADERS, headers);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void readFromJSON(JSONObject json) throws Exception {
        // Headers
        if (json.has(JSON_ATTR_HEADERS)) {
            this.mHeaders = new HashMap<String, List<String>>();
            JSONObject headers = json.getJSONObject(JSON_ATTR_HEADERS);
            Iterator headerIter = headers.keys();
            while (headerIter.hasNext()) {
                String key = (String) headerIter.next();
                JSONArray values = headers.getJSONArray(key);
                List<String> strValues = new ArrayList<String>();
                for (int i = 0; i < values.length(); i++) {
                    strValues.add(values.getString(i));
                }
                this.mHeaders.put(key, strValues);
            }
        } else {
            this.mHeaders = null;
        }
    }
}
