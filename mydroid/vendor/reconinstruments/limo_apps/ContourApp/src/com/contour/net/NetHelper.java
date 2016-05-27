package com.contour.net;

import com.contour.connect.R;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public abstract class NetHelper {
    final boolean mDebug;
    protected Activity mActivity;
    
    public interface NetHelperCallback {
        public void onCameraVersionDownloaded(int cameraModel, int major, int minor, int build);
        public void onCameraVersionDownloadedFailed();

    }
    
    public static final String URL_VERSION_DATA = "http://contour.mobile.s3.amazonaws.com/fw-versions-debug.json";
    
    public static NetHelper createInstance(Activity activity) {
        return new NetHelperBase(activity);
    }
    
    protected NetHelper(Activity activity) {
        mActivity = activity;
        mDebug = activity.getResources().getBoolean(R.bool.debug_enabled);
    }
    
    public boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    
    public abstract void getVersioningData();
    public abstract void cancelActiveTasks();


}