package com.reconinstruments.hudconnectivitylib.http;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.hudconnectivitylib.BaseHUDConnectivity;
import com.reconinstruments.os.hardware.ashmem.HUDAshmem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HUDHttpMessage extends BaseHUDConnectivity implements Parcelable  {
    @SuppressWarnings("unused")
    private final String TAG = this.getClass().getSimpleName();

    /** @hide */
    private static final String JSON_ATTR_HEADERS = "headers";

    /** @hide */
    private Map<String, List<String>> mHeaders = null;
    private byte[] mBody = null;

    public HUDHttpMessage(){
    }

    public HUDHttpMessage(Parcel in) {
        readFromParcel(in);
    }

    protected HUDHttpMessage(Map<String, List<String>> headers, byte[] body) {
        super();
        this.mHeaders = headers;
        if(body != null) {
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
     * @param body the message body
     */
    public void setBody(byte[] body) {
        if(body != null) {
            this.mBody = body;
        } else {
            this.mBody = null;
        }
    }

    /**
     * @return the message name/value headers
     */
    public Map<String, List<String>> getHeaders() {
        return this.mHeaders;
    }

    /**
     * Check if there is a body
     * @return true if the body has size > 0, otherwise false
     */
    public boolean hasBody() {
        if(this.mBody == null) {
            return false;
        }

        return (this.mBody.length > 0);
    }

    /**
     * @return the body message, as a byte array
     */
    public byte[] getBody() {
        return this.mBody;
    }

    /**
     * @return the message body in string format<br>basically calling <code>return new String(getBody());</code>
     */
    public String getBodyString() {
        if(hasBody()) {
            return new String(getBody());
        }
        return null;
    }

    @Override
    protected void writeToJSON(JSONObject json) throws Exception {
        // Headers
        if(this.mHeaders != null) {
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
        if(json.has(JSON_ATTR_HEADERS)) {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if(this.mHeaders != null) {
            dest.writeInt(this.mHeaders.size());
            for (Entry<String, List<String>> header : this.mHeaders.entrySet()) {
                if (header.getKey() == null) {
                    // This is the first line of the response, skip it.
                    // See: http://developer.android.com/reference/java/net/URLConnection.html#getHeaderFields()
                    continue;
                }
                dest.writeString(header.getKey());
                dest.writeStringList(header.getValue());
            }
        } else {
            dest.writeInt(0);
        }

        if(this.mBody != null) {
            dest.writeInt(this.mBody.length);
            if(this.mBody.length > 0) {
                HUDAshmem tempBody = new HUDAshmem(this.mBody);
                tempBody.writeToParcel(dest, flags);
            }
        } else {
            dest.writeInt(0);
        }
    }

    public void readFromParcel(Parcel in) {
        int size = in.readInt();
        if(size > 0) {
            this.mHeaders = new HashMap<String, List<String>>();
            for(int i = 0; i < size; i++) {
                String key = in.readString();
                List<String> header = new ArrayList<String>();
                in.readStringList(header);
                this.mHeaders.put(key, header);
            }
        } else {
            this.mHeaders = null;
        }

        size = in.readInt();
        if(size > 0) {
            this.mBody = HUDAshmem.CREATOR.createFromParcel(in).getData();
        } else {
            this.mBody = null;
        }
    }

    public static final Parcelable.Creator<HUDHttpMessage> CREATOR = new Parcelable.Creator<HUDHttpMessage>() {
        @Override
        public HUDHttpMessage createFromParcel(Parcel in) {
            return new HUDHttpMessage(in);
        }

        @Override
        public HUDHttpMessage[] newArray(int size) {
            return new HUDHttpMessage[size];
        }
    };
}
