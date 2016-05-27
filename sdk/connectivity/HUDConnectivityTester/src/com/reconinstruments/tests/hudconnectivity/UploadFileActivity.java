package com.reconinstruments.tests.hudconnectivity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest.RequestMethod;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadFileActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();
    private static final boolean DEBUG = true;

    public static final int UPLOAD_BYTE_SIZE = 70000;
    public static final String DONWLOAD_FILE_URL = null;
    public static final String DOWNLOAD_MD5 = null;

    public static TextView mNumberOfRequest;
    public static TextView mNumberOfGoodResponse;
    public static TextView mNumberOfBadResponse;

    public static int mRequestCounter = 0;
    public static int mGoodCounter = 0;
    public static int mBadCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.file_view);

        mNumberOfRequest = (TextView) findViewById(R.id.numberOfRequest);
        mNumberOfGoodResponse = (TextView) findViewById(R.id.numberOfGoodResponse);
        mNumberOfBadResponse = (TextView) findViewById(R.id.numberOfBadResponse);
    }

    @Override
    protected void onResume() {
        super.onResume();

        byte[] data1 = new byte[UPLOAD_BYTE_SIZE];
        Arrays.fill(data1, "b".getBytes()[0]);
        String value = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(data1);
            byte[] resultByte = md.digest();

            for (int i=0; i < resultByte.length; i++)
            {
                value += Integer.toString( ( resultByte[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        String url1 = "http://httpbin.org/post";
        String md5String1 = value.trim();

        mRequestCounter = 0;
        mGoodCounter = 0;
        mBadCounter = 0;

        for(int i = 0; i < 1 ; i++){
            new DownloadFileTask(data1, url1, md5String1, i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private class DownloadFileTask extends AsyncTask<String, Void, Boolean> {

        byte[] mData;
        String mUrl;
        String mMD5String;
        int mTaskNumber;
        long startTime;
        long stopTime;

        public DownloadFileTask(byte[] data, String url, String md5, int taskNumber) {
            mUrl = url;
            mMD5String = md5;
            mTaskNumber = taskNumber;
            mData = data;
            Log.d(TAG, "New Task Created: TaskNumber: " + taskNumber);
            mRequestCounter ++;
            mNumberOfRequest.setText(Integer.toString(mRequestCounter));
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            boolean result = false;
            try {

                if (DEBUG) Log.d(TAG, "TaskNumber: " + mTaskNumber + "prepare for Request");

                startTime = System.currentTimeMillis();
                Map<String, List<String>> headers = new HashMap<String, List<String>>();
                List<String> aList = new ArrayList<String>();
                aList.add("text");
                headers.put("Content-Type", aList);

                HUDHttpRequest request = new HUDHttpRequest(RequestMethod.POST, new URL(mUrl), headers, mData);
                Log.d("XXXX",request.toString());
                if (DEBUG) Log.d(TAG, "TaskNumber: " + mTaskNumber + " sendWebRequest:" + request.getURL());
                HUDHttpResponse response = TestActivity.mHUDConnectivityManager.sendWebRequest(request);
                stopTime = System.currentTimeMillis();
                if (response.hasBody()) {
                    if (DEBUG) Log.d(TAG, "TaskNumber: " + mTaskNumber + " sendWebRequest(response) bodySize:" + response.getBody().length);
                    byte[] data = response.getBody();
                    String dataString = new String(data);
                    int start = dataString.indexOf("\"data\": ") + 9;
                    dataString = dataString.substring(start);
                    int end = dataString.indexOf("\"");
                    dataString = dataString.substring(0, end);
                    Log.d(TAG,dataString);
                    data = dataString.getBytes();
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.reset();
                    md.update(data);
                    byte[] resultByte = md.digest();
                    String value = "";
                    for (int i=0; i < resultByte.length; i++)
                    {
                        value += Integer.toString( ( resultByte[i] & 0xff ) + 0x100, 16).substring( 1 );
                    }                        
                    if(value.trim().equalsIgnoreCase(mMD5String)){
                        result = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result){
                mGoodCounter ++;
            }else {
                mBadCounter ++;
            }

            Log.d(TAG, "Result: Total    GoodCounter: " + mGoodCounter + " BadCounter: " + mBadCounter);
            Log.d(TAG, "TaskNumber: " + mTaskNumber + " Time: " + (stopTime -startTime) + " Result: " + (result ? "Successful" : "Failed"));

            mNumberOfGoodResponse.setText(Integer.toString(mGoodCounter));
            mNumberOfBadResponse.setText(Integer.toString(mBadCounter));
        }
    }
}
