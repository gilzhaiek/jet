package com.reconinstruments.hudconnectivity.http;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public final class HUDHttpResponse extends HUDHttpMessage {
    @SuppressWarnings("unused")
    private final String TAG = this.getClass().getSimpleName();

    /**
     * @hide
     */
    private static final String JSON_ATTR_RESPONSE_CODE = "responseCode";

    /**
     * @hide
     */
    private static final String JSON_ATTR_RESPONSE_MESSAGE = "responseMessage";

    /**
     * @hide
     */
    private int mResponseCode;

    /**
     * @hide
     */
    private String mResponseMessage;

    public HUDHttpResponse(byte[] bytes) throws Exception {
        super(bytes);
    }

    /**
     * The HUDHttpResponse constructor is created for each response and can't be recycled or used
     *
     * @param responseCode    the response code, -1 if no valid response code.
     * @param responseMessage the response message. null if no such response exists.
     */
    public HUDHttpResponse(int responseCode, String responseMessage) {
        this(responseCode, responseMessage, null, null);
    }

    /**
     * The HUDHttpResponse constructor is created for each response and can't be recycled or used
     *
     * @param responseCode    the response code, -1 if no valid response code.
     * @param responseMessage the response message. null if no such response exists.
     * @param headers         the response name/value headers
     */
    public HUDHttpResponse(int responseCode, String responseMessage, Map<String, List<String>> headers) {
        this(responseCode, responseMessage, headers, null);
    }

    /**
     * The HUDHttpResponse constructor is created for each response and can't be recycled or used
     *
     * @param responseCode    the response code, -1 if no valid response code.
     * @param responseMessage the response message. null if no such response exists.
     * @param headers         the response name/value headers
     * @param body            the response body
     */
    public HUDHttpResponse(int responseCode, String responseMessage, Map<String, List<String>> headers, byte[] body) {
        super(headers, body);
        mResponseCode = responseCode;
        mResponseMessage = responseMessage;
    }

    /**
     * Returns the response code returned by the remote HTTP server.
     *
     * @return the response code, -1 if no valid response code.
     */
    public int getResponseCode() {
        return mResponseCode;
    }

    /**
     * Returns the response message returned by the remote HTTP server.
     *
     * @return the response message. null if no such response exists.
     */
    public String getResponseMessage() {
        return mResponseMessage;
    }

    @Override
    protected void writeToJSON(JSONObject json) throws Exception {
        // Response Code
        json.put(JSON_ATTR_RESPONSE_CODE, this.mResponseCode);

        // Response Message
        json.put(JSON_ATTR_RESPONSE_MESSAGE, this.mResponseMessage);

        super.writeToJSON(json);
    }

    @Override
    protected void readFromJSON(JSONObject json) throws Exception {
        // Response Code
        this.mResponseCode = json.getInt(JSON_ATTR_RESPONSE_CODE);

        this.mResponseMessage = json.getString(JSON_ATTR_RESPONSE_MESSAGE);

        super.readFromJSON(json);
    }
}
