package com.reconinstruments.tests.hudconnectivity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest.RequestMethod;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;

import java.security.MessageDigest;

public class DownloadFileActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();
    private static final boolean DEBUG = true;

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

        String url1 = "http://www.reconinstruments.com/wp-content/themes/recon/img/jet/slide-3.jpg";
        String md5String1 = "1fae47b3a0f9b52e7d16e744152ef6ec";

        String url2 = "http://mirror.internode.on.net/pub/test/1meg.test";
        String md5String2 = "e6527b4d5db05226f40f9f2e7750abfb";

        //String url3 = "http://download.thinkbroadband.com/5MB.zip";
        //String md5String3 = "b3215c06647bc550406a9c8ccc378756";

        mRequestCounter = 0;
        mGoodCounter = 0;
        mBadCounter = 0;

        mNumberOfRequest.setText("0");
        mNumberOfGoodResponse.setText("0");
        mNumberOfBadResponse.setText("0");

        for(int i = 0; i < 10 ; i++){
            if(i%2 == 0){
                new DownloadFileTask(url1, md5String1, i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else {
                new DownloadFileTask(url2, md5String2, i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    private class DownloadFileTask extends AsyncTask<String, Void, Boolean> {

        String mUrl;
        String mMD5String;
        int mTaskNumber;
        long startTime;
        long stopTime;

        public DownloadFileTask(String url, String md5, int taskNumber) {
            mUrl = url;
            mMD5String = md5;
            mTaskNumber = taskNumber;
            Log.d(TAG, "New Task Created: TaskNumber: " + taskNumber);
            mRequestCounter ++;
            mNumberOfRequest.setText(Integer.toString(mRequestCounter));
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            boolean result = false;
            try {
                if (DEBUG) Log.d(TAG, "TaskNumber: " + mTaskNumber + " prepare for Request");
                startTime = System.currentTimeMillis();
                HUDHttpRequest request = new HUDHttpRequest(RequestMethod.GET, mUrl);
                if (DEBUG) Log.d(TAG, "TaskNumber: " + mTaskNumber + " sendWebRequest:" + request.getURL());
                HUDHttpResponse response = TestActivity.mHUDConnectivityManager.sendWebRequest(request);
                stopTime = System.currentTimeMillis();
                if (response.hasBody()) {
                    if (DEBUG) Log.d(TAG, "TaskNumber: " + mTaskNumber + " sendWebRequest(response) bodySize:" + response.getBody().length);
                    byte[] data = response.getBody();
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
