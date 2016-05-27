package com.reconinstruments.os.connectivity.http;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * An HTTP response is generated when you send a web request using {@link com.reconinstruments.os.connectivity.HUDConnectivityManager#sendWebRequest}.
 */
public final class HUDHttpResponse extends HUDHttpMessage {
    @SuppressWarnings("unused")
    private final String TAG = this.getClass().getSimpleName();

    private static final String JSON_ATTR_RESPONSE_CODE = "responseCode";
    private static final String JSON_ATTR_RESPONSE_MESSAGE = "responseMessage";
    private int mResponseCode;
    private String mResponseMessage;

    /**{@hide}*/
    public HUDHttpResponse(byte[] bytes) throws Exception {
        readFromJSON(new JSONObject(new String(bytes)));
    }

    /**
     * @param in
     * {@hide}
     */
    public HUDHttpResponse(Parcel in) {
        readFromParcel(in);
    }

    /**
     * The HUDHttpResponse constructor is created for each response and can't be recycled or used.
     *
     * @param responseCode    the response code, otherwise -1 if no valid response code.
     * @param responseMessage the response message, otherwise null if no such response exists.
     * {@hide}
     */
    public HUDHttpResponse(int responseCode, String responseMessage) {
        this(responseCode, responseMessage, null, null);
    }

    /**
     * The HUDHttpResponse constructor is created for each response and can't be recycled or used.
     *
     * @param responseCode    the response code, otherwise -1 if no valid response code.
     * @param responseMessage the response message, otherwise null if no such response exists.
     * @param headers         the response name/value headers
     * {@hide}
     */
    public HUDHttpResponse(int responseCode, String responseMessage, Map<String, List<String>> headers) {
        this(responseCode, responseMessage, headers, null);
    }

    /**
     * The HUDHttpResponse constructor is created for each response and can't be recycled or used.
     *
     * @param responseCode    the response code, otherwise -1 if no valid response code.
     * @param responseMessage the response message, otherwise null if no such response exists.
     * @param headers         the response name/value headers
     * @param body            the response body
     * {@hide}
     */
    public HUDHttpResponse(int responseCode, String responseMessage, Map<String, List<String>> headers, byte[] body) {
        super(headers, body);
        mResponseCode = responseCode;
        mResponseMessage = responseMessage;
    }

    /**
     * Returns the remote HTTP server's response code.
     *
     * @return the response code, otherwise -1 if no valid response code.
     */
    public int getResponseCode() {
        return mResponseCode;
    }

    /**
     * Returns the remote HTTP server's response message.
     *
     * @return the response message, otherwise null if no such response exists.
     */
    public String getResponseMessage() {
        return mResponseMessage;
    }

    /**{@hide}*/
    @Override
    protected void writeToJSON(JSONObject json) throws Exception {
        // Response Code
        json.put(JSON_ATTR_RESPONSE_CODE, this.mResponseCode);

        // Response Message
        json.put(JSON_ATTR_RESPONSE_MESSAGE, this.mResponseMessage);

        super.writeToJSON(json);
    }

    /**
     * @param json
     * @throws Exception
     * {@hide}
     */
    @Override
    protected void readFromJSON(JSONObject json) throws Exception {
        // Response Code
        this.mResponseCode = json.getInt(JSON_ATTR_RESPONSE_CODE);

        this.mResponseMessage = json.getString(JSON_ATTR_RESPONSE_MESSAGE);

        super.readFromJSON(json);
    }

    /**
     * @param dest
     * @param flags
     * {@hide}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mResponseCode);
        dest.writeString(this.mResponseMessage);
        super.writeToParcel(dest, flags);
    }

    /**
     * @param in
     * {@hide}
     */
    @Override
    public void readFromParcel(Parcel in) {
        this.mResponseCode = in.readInt();
        this.mResponseMessage = in.readString();
        super.readFromParcel(in);
    }

    /**
     * {@hide}
     */
    public static final Parcelable.Creator<HUDHttpResponse> CREATOR = new Parcelable.Creator<HUDHttpResponse>() {
        @Override
        public HUDHttpResponse createFromParcel(Parcel in) {
            return new HUDHttpResponse(in);
        }

        @Override
        public HUDHttpResponse[] newArray(int size) {
            return new HUDHttpResponse[size];
        }
    };
}
