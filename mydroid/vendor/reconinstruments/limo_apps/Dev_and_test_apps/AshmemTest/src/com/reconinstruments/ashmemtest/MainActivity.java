package com.reconinstruments.ashmemtest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.reconinstruments.os.hardware.ashmem.HUDAshmem;

import java.io.IOException;

public class MainActivity extends Activity
{
    private static final String TAG = "MainActivity";
    private static final int ONE_KB = 1024;
    private static final int ONE_MB = ONE_KB * 1024;
    private static final int TEN_MB = 10 * ONE_MB;
    private static final int TWN_MB = 20 * ONE_MB;
    public static final String SEND_ACTION = "com.reconinstruments.ashmem.SEND";
    public static final String ASHMEM_BDL = "com.reconinstruments.ashmem.ASHMEM_BDL";
    public static final String ASHMEM_HDL = "com.reconinstruments.ashmem.ASHMEM_HDL";
    public static final String ASHMEM_LEN = "com.reconinstruments.ashmem.ASHMEM_LEN";

    private Context mContext = null;
    private Handler mHandler = new Handler();
    private int mForIter = 0;
    private int mHandle = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        startService(new Intent(mContext, ResultService.class));
        setContentView(R.layout.main);
        System.loadLibrary("reconinstruments_jni");
    }

    public void oneMB(View v) {
        Log.d(TAG, "oneMB");
        byte[] data = new byte[ONE_MB];
        data[0] = 0x12;
        data[1] = 0x34;
        data[ONE_MB-1] = 0x7F;
        int handle = HUDAshmem.allocate(ONE_MB);
        if (handle > 0) {
            HUDAshmem.write(handle, data, ONE_MB);
            try {
                ParcelFileDescriptor fd;
                fd = ParcelFileDescriptor.fromFd(handle);

                Bundle b = new Bundle();
                b.putParcelable(ASHMEM_HDL, fd);
                b.putInt(ASHMEM_LEN, ONE_MB);

                Intent i = new Intent(SEND_ACTION);
                i.putExtra(ASHMEM_BDL, b);
                sendBroadcast(i);

                fd = null;
                b = null;
                i = null;
                System.gc();
            } catch (IOException e) {
                Log.d(TAG, "Failed to get fd from handle: " + e);
            }
        }
        HUDAshmem.free(handle);
        Log.d(TAG, "oneMB X");
    }

    public void tenMB(View v) {
        Log.d(TAG, "tenMB");
        byte[] data = new byte[TEN_MB];
        data[0] = 0x12;
        data[1] = 0x34;
        data[TEN_MB-1] = 0x55;
        int handle = HUDAshmem.allocate(TEN_MB);
        if (handle > 0) {
            HUDAshmem.write(handle, data, TEN_MB);
            try {
                ParcelFileDescriptor fd;
                fd = ParcelFileDescriptor.fromFd(handle);

                Bundle b = new Bundle();
                b.putParcelable(ASHMEM_HDL, fd);
                b.putInt(ASHMEM_LEN, TEN_MB);

                Intent i = new Intent(SEND_ACTION);
                i.putExtra(ASHMEM_BDL, b);
                sendBroadcast(i);

                fd = null;
                b = null;
                i = null;
                System.gc();
            } catch (IOException e) {
                Log.d(TAG, "Failed to get fd from handle: " + e);
            }
        }
        HUDAshmem.free(handle);
        Log.d(TAG, "tenMB X");
    }

    public void twnMB(View v) {
        Log.d(TAG, "twnMB");
        byte[] data = new byte[TWN_MB];
        data[0] = 0x12;
        data[1] = 0x34;
        data[TWN_MB-1] = 0x65;
        int handle = HUDAshmem.allocate(TWN_MB);
        if (handle > 0) {
            HUDAshmem.write(handle, data, TWN_MB);
            try {
                ParcelFileDescriptor fd;
                fd = ParcelFileDescriptor.fromFd(handle);

                Bundle b = new Bundle();
                b.putParcelable(ASHMEM_HDL, fd);
                b.putInt(ASHMEM_LEN, TWN_MB);

                Intent i = new Intent(SEND_ACTION);
                i.putExtra(ASHMEM_BDL, b);
                sendBroadcast(i);

                fd = null;
                b = null;
                i = null;
                System.gc();
            } catch (IOException e) {
                Log.d(TAG, "Failed to get fd from handle: " + e);
            }
        }
        HUDAshmem.free(handle);
        Log.d(TAG, "twnMB X");
    }

    private final Runnable runFor = new Runnable() {
        public void run() {
            if (mForIter == 0) {
                mHandle = HUDAshmem.allocate(TEN_MB);
            }
            if (mForIter < 10) {
                if (mHandle > 0) {
                    byte[] data = new byte[TEN_MB];
                    data[0] = 0x12;
                    data[1] = 0x34;
                    data[TEN_MB-1] = (byte)mForIter;

                    HUDAshmem.write(mHandle, data, TEN_MB);

                    try {
                        ParcelFileDescriptor fd;
                        fd = ParcelFileDescriptor.fromFd(mHandle);
                        Bundle b = new Bundle();
                        b.putParcelable(ASHMEM_HDL, fd);
                        b.putInt(ASHMEM_LEN, TEN_MB);

                        Intent i = new Intent(SEND_ACTION);
                        i.putExtra(ASHMEM_BDL, b);
                        sendBroadcast(i);

                        i = null;
                        b = null;
                        fd = null;
                    } catch (IOException e) {
                        Log.d(TAG, "Failed to get fd from handle: " + e);
                    }
                }
                mForIter += 1;
                mHandler.postDelayed(runFor, 1000);
            } else {
                Log.d(TAG, "Finished iterations");
                mForIter = 0;
                HUDAshmem.free(mHandle);
                System.gc();
            }
        }
    };

    public void forTenMB(View v) {
        Log.d(TAG, "forTenMB");
        mHandler.postDelayed(runFor, 1000);
        Log.d(TAG, "forTenMB X");
    }
}
