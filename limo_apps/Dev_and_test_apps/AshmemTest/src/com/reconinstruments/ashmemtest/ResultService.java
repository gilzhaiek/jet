package com.reconinstruments.ashmemtest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.os.hardware.ashmem.HUDAshmem;

public class ResultService extends Service {

    private static final String TAG = "ResultService";
    private Context mContext;

    @Override
    public void onCreate() {
        mContext = this;
        Log.d(TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.SEND_ACTION);
        mContext.registerReceiver(mIntentReceiver, filter);

        System.loadLibrary("reconinstruments_jni");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext.unregisterReceiver(mIntentReceiver);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received Intent");
            Bundle b = intent.getBundleExtra(MainActivity.ASHMEM_BDL);
            if (b != null) {
                ParcelFileDescriptor fd = b.getParcelable(MainActivity.ASHMEM_HDL);
                int handle = fd.getFd();
                int length = b.getInt(MainActivity.ASHMEM_LEN, 0);
                byte[] data = null;

                if (handle > 0 && length > 0) {
                    data = HUDAshmem.read(handle, length);
                    if (data != null) {
                        Log.d(TAG, "data.length = " + data.length);
                        Log.d(TAG, "data[0] = " + data[0]);
                        Log.d(TAG, "data[1] = " + data[1]);
                        Log.d(TAG, "data[...] = " + data[data.length - 1]);
                    } else {
                        Log.e(TAG, "Failed to read from handle: " + handle + " length: " + length);
                    }
                    HUDAshmem.free(handle);
                    fd = null;
                    b = null;
                    intent = null;
                    System.gc();
                }
            }
        }
    };
}
