package com.reconinstruments.hudconnectivitylib.http;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.util.ByteArrayBuffer;

import android.util.Log;

public class URLConnectionHUDAdaptor {
    private static final boolean DEBUG = true;

    private static final String TAG = "URLConnectionHUDAdaptor";

    public static HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws IOException {
        if (DEBUG)
            Log.d(TAG, "sendRequest: Method=" + request.getRequestMethod() + " URL(host)=" + request.getURL().getHost());

        HUDHttpResponse response = null;
        boolean hasResponse = request.getDoInput();

        HttpURLConnection urlConnection = (HttpURLConnection) request.getURL().openConnection();

        try {
            urlConnection.setConnectTimeout(request.getTimeout());
            urlConnection.setRequestMethod(request.getRequestMethodString());

            if (hasResponse) {
                urlConnection.setReadTimeout(request.getTimeout());
            }

            // Request Properties / Headers
            Map<String, List<String>> headers = request.getHeaders();
            if (headers != null) {
                for (Entry<String, List<String>> header : headers.entrySet()) {
                    List<String> vals = header.getValue();
                    for (String val : vals) {
                        urlConnection.addRequestProperty(header.getKey(), val);
                    }
                }
            }
            // We don't want keep the connection open between requests, causes EOFException in Android 4.3
            urlConnection.setRequestProperty("Connection", "close");

            if (request.getDoOutput()) {
                urlConnection.setDoOutput(true);
                byte[] body = request.getBody();
                urlConnection.setFixedLengthStreamingMode(body.length);
                OutputStream outputStream = urlConnection.getOutputStream();
                outputStream.write(body);
                outputStream.close();
            } else {
                urlConnection.setDoOutput(false);
            }

            urlConnection.connect();

            if (hasResponse) {
                try {
                    int responseCode = urlConnection.getResponseCode();
                    String responseMessage = urlConnection.getResponseMessage();
                    response = new HUDHttpResponse(responseCode, responseMessage);

                    BufferedInputStream bufferedInputStream;
                    try {
                        bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
                    } catch (FileNotFoundException e) {
                        // There was no input stream returned, use the error stream
                        bufferedInputStream = new BufferedInputStream(urlConnection.getErrorStream());
                    }
                    ByteArrayBuffer byteArrayBuffer = new ByteArrayBuffer(1024);
                    byte[] buff = new byte[1024];
                    for (; ; ) {
                        int len = bufferedInputStream.read(buff);
                        if (len != -1) {
                            byteArrayBuffer.append(buff, 0, len);
                        } else { // Done Reading
                            break;
                        }
                    }
                    bufferedInputStream.close();
                    byte[] body = byteArrayBuffer.toByteArray();
                    if (DEBUG) Log.d(TAG, "Received body size of " + body.length);
                    response.setBody(body);
                } catch (Exception e) {
                }
            }
        } finally {
            urlConnection.disconnect();
        }

        if (DEBUG)
            Log.d(TAG, "sendRequest(Response): Method=" + request.getRequestMethod() + " URL(host)=" + request.getURL().getHost());
        return response;
    }
}
