package com.reconinstruments.hudconnectivitylib.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class HUDHttpRequest extends HUDHttpMessage {
    private final String TAG = this.getClass().getSimpleName();

    private static final int DEFAULT_TIMEOUT_MS = 15000;
    private static final boolean DEFAULT_DO_INPUT = true;

    private static final String JSON_ATTR_REQUEST_METHOD = "requestMethod";
    private static final String JSON_ATTR_URL = "url";
    private static final String JSON_ATTR_DO_INPUT = "doInput";
    private static final String JSON_ATTR_TIMEOUT = "timeout";

    public enum RequestMethod {
        OPTIONS,
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        TRACE
    }

    private RequestMethod mRequestMethod;
    private URL mURL;
    private boolean mDoInput = DEFAULT_DO_INPUT;
    private int mTimeoutMillis = DEFAULT_TIMEOUT_MS;

    public HUDHttpRequest(byte[] bytes) throws Exception {
        readFromJSON(new JSONObject(new String(bytes)));
    }

    public HUDHttpRequest(Parcel in) {
        super(in);
    }

    /**
     * Create an HTTP message from POJOs
     *
     * @param requestMethod the RequestMethod (enum) representing the method to be used to send to the remote HTTP server
     * @param url           the URL which represents the remote target in the connection
     * @throws MalformedURLException
     */
    public HUDHttpRequest(RequestMethod requestMethod, String url) throws MalformedURLException {
        this(requestMethod, new URL(url));
    }

    /**
     * Create an HTTP message from POJOs
     *
     * @param requestMethod the RequestMethod (enum) representing the method to be used to send to the remote HTTP server
     * @param url           the URL which represents the remote target in the connection
     */
    public HUDHttpRequest(RequestMethod requestMethod, URL url) {
        this(requestMethod, url, null, null);
    }

    /**
     * Create an HTTP message from POJOs
     *
     * @param requestMethod the RequestMethod (enum) representing the method to be used to send to the remote HTTP server
     * @param url           the URL which represents the remote target in the connection
     * @param headers       the request name/value headers
     */
    public HUDHttpRequest(RequestMethod requestMethod, URL url, Map<String, List<String>> headers) {
        this(requestMethod, url, headers, null);
    }

    /**
     * Create an HTTP message from POJOs
     *
     * @param requestMethod the RequestMethod (enum) representing the method to be used to send to the remote HTTP server
     * @param url           the URL which represents the remote target in the connection
     * @param headers       the request name/value headers
     * @param body          the request body.  Giving a value to this, will set doOutput to be true
     */
    public HUDHttpRequest(RequestMethod requestMethod, URL url, Map<String, List<String>> headers, byte[] body) {
        super(headers, body);

        if (requestMethod == null || url == null) {
            throw new IllegalArgumentException("HTTP Request method and URL cannot be null");
        }

        this.mRequestMethod = requestMethod;
        this.mURL = url;
    }

    /**
     * @return the RequestMethod (enum) representing the method to be used to send to the remote HTTP server
     */
    public RequestMethod getRequestMethod() {
        return mRequestMethod;
    }

    /**
     * @return the RequestMethod as String
     */
    public String getRequestMethodString() {
        switch (getRequestMethod()) {
            case DELETE:
                return "DELETE";
            case GET:
                return "GET";
            case HEAD:
                return "HEAD";
            case OPTIONS:
                return "OPTIONS";
            case POST:
                return "POST";
            case PUT:
                return "PUT";
            case TRACE:
                return "TRACE";
            default:
                return null;
        }
    }

    /**
     * @return the URL which represents the remote target in the connection
     */
    public URL getURL() {
        return mURL;
    }

    /**
     * Gets the doInput flag which will be given to the URLConnection.
     * @return the value of the doInput flag</br>
     * Note: Always return true, if it is a GET request
     */
    public boolean getDoInput() {
        if (mRequestMethod == RequestMethod.GET) {
            return true;
        }
        return mDoInput;
    }

    /**
     * Sets the value of the doInput field for which will be provided to the URLConnection
     * </br>A URL connection can be used for input and/or output.
     * </br>Set the DoInput flag to true if you intend to use the URL connection for input, false if not.
     * </p>The default is true.</p>
     *
     * @param doInput - the new value.
     */
    public void setDoInput(boolean doInput) {
        mDoInput = doInput;
    }

    /**
     * Gets the doOutput flag which will be given to the URLConnection
     * @return the value of the doOutput flag
     */
    public boolean getDoOutput() {
        return hasBody();
    }

    /**
     * Set timeout for the connection, where default is set to 15 seconds
     * @param timeoutMillis timeout time in milliseconds
     */
    public void setTimeout(int timeoutMillis) {
        this.mTimeoutMillis = timeoutMillis;
    }

    /**
     * Get connection timeout 
     * @return the connection/read timeout, in milliseconds.
     */
    public int getTimeout() {
        return this.mTimeoutMillis;
    }

    @Override
    protected void writeToJSON(JSONObject json) throws Exception {
        // Request Method
        json.put(JSON_ATTR_REQUEST_METHOD, getRequestMethod().ordinal());

        // URL
        json.put(JSON_ATTR_URL, getURL());

        // Do Input
        json.put(JSON_ATTR_DO_INPUT, getDoInput());

        // Timeout
        json.put(JSON_ATTR_TIMEOUT, getTimeout());

        super.writeToJSON(json);
    }

    @Override
    protected void readFromJSON(JSONObject json) throws Exception {
        // Request Method
        this.mRequestMethod = RequestMethod.values()[json.getInt(JSON_ATTR_REQUEST_METHOD)];

        // URL
        this.mURL = new URL(json.getString(JSON_ATTR_URL));

        // Set Do Input
        if (json.has(JSON_ATTR_DO_INPUT)) {
            this.mDoInput = json.getBoolean(JSON_ATTR_DO_INPUT);
        } else {
            this.mDoInput = DEFAULT_DO_INPUT;
        }

        // Timeout
        if (json.has(JSON_ATTR_TIMEOUT)) {
            this.mTimeoutMillis = json.getInt(JSON_ATTR_TIMEOUT);
        } else {
            this.mTimeoutMillis = DEFAULT_TIMEOUT_MS;
        }

        super.readFromJSON(json);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRequestMethod.ordinal());
        dest.writeString(this.mURL.toString());
        dest.writeInt(this.mDoInput ? 1 : 0);
        dest.writeInt(this.mTimeoutMillis);
        super.writeToParcel(dest, flags);
    }

    @Override
    public void readFromParcel(Parcel in) {
        this.mRequestMethod = RequestMethod.values()[in.readInt()];
        try {
            this.mURL = new URL(in.readString());
        } catch (MalformedURLException e) {
            Log.e(TAG, "readFromParcel failed, the URL is malformed", e);
        }
        this.mDoInput = (in.readInt() == 1);
        this.mTimeoutMillis = in.readInt();
        super.readFromParcel(in);
    }

    public static final Parcelable.Creator<HUDHttpRequest> CREATOR = new Parcelable.Creator<HUDHttpRequest>() {
        @Override
        public HUDHttpRequest createFromParcel(Parcel in) {
            return new HUDHttpRequest(in);
        }

        @Override
        public HUDHttpRequest[] newArray(int size) {
            return new HUDHttpRequest[size];
        }
    };
}