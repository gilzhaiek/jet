package com.contour.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.contour.api.CameraComms;
import com.contour.connect.debug.CLog;

public class NetHelperBase extends NetHelper {

    public static final String TAG = "NetHelperBase";
    NetHelperCallback mCallback;
    DownloadTextTask mDownloadTask;
    
    protected NetHelperBase(Activity activity) {
        super(activity);
        mCallback = (NetHelperCallback) mActivity;
    }
    
    public void getVersioningData() {
        cancelActiveTasks();
        mDownloadTask = new DownloadTextTask();
        mDownloadTask.execute(URL_VERSION_DATA);
    }
    
    public void cancelActiveTasks() { 
        if(mDownloadTask != null) {
            mDownloadTask.cancel(false);
            mDownloadTask = null;
        }
    }
    
    private class DownloadTextTask extends AsyncTask<String,Void,Boolean> {
        private int[] gpsVersionVals = new int[3];
        private int[] cpVersionVals = new int[3];
        private int[] cp2VersionVals = new int[3];
     
        @Override
        protected Boolean doInBackground(String... urls) {
              
            // params comes from the execute() call: params[0] is the url.
            try {
                String downloadData = downloadUrl(urls[0]);
                if(!isCancelled()) {
                    parseVersionData(downloadData);
                    return true;
                }
            } catch (IOException e) {
                Log.wtf(TAG, e);
            } catch (JSONException e) {
                Log.wtf(TAG, e);
            }
            return false;
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Boolean result) {
            if(result.booleanValue()) {
                if (!mActivity.isFinishing()) {
                    mCallback.onCameraVersionDownloaded(CameraComms.MODEL_GPS, gpsVersionVals[0], gpsVersionVals[1], gpsVersionVals[2]);
                    mCallback.onCameraVersionDownloaded(CameraComms.MODEL_PLUS, cpVersionVals[0], cpVersionVals[1], cpVersionVals[2]);
                    mCallback.onCameraVersionDownloaded(CameraComms.MODEL_PLUS_2, cp2VersionVals[0], cp2VersionVals[1], cp2VersionVals[2]);
                }
            } else {
                mCallback.onCameraVersionDownloadedFailed();
            }
       }
        @Override

        protected void onCancelled (Boolean result) {
            if(!result) {
                mCallback.onCameraVersionDownloadedFailed();
            }
        }

        
        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;
                
            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(10000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.setDefaultUseCaches(false);
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                if(mDebug) CLog.out(TAG,"The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                String contentAsString = readIt(is, len);
                return contentAsString;
                
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                } 
            }
        }
        
        public String readIt(InputStream stream, int len) throws IOException,
                UnsupportedEncodingException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            return sb.toString();
        }
        
        protected void parseVersionData(String data) throws JSONException {
            JSONObject jObject = new JSONObject(data).getJSONObject("versions");

            JSONObject gpsObj = jObject.getJSONObject("Contour GPS");
            JSONObject cpObj = jObject.getJSONObject("Contour+");
            JSONObject cp2Obj = jObject.getJSONObject("Contour+2");

            gpsVersionVals[0] = gpsObj.getInt("major");
            gpsVersionVals[1] = gpsObj.getInt("minor");
            gpsVersionVals[2] = gpsObj.getInt("build");

            cpVersionVals[0] = cpObj.getInt("major");
            cpVersionVals[1] = cpObj.getInt("minor");
            cpVersionVals[2] = cpObj.getInt("build");

            cp2VersionVals[0] = cp2Obj.getInt("major");
            cp2VersionVals[1] = cp2Obj.getInt("minor");
            cp2VersionVals[2] = cp2Obj.getInt("build");
        }
    }
}
