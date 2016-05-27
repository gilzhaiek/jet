package com.reconinstruments.hudserver;

import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.os.hardware.sensors.HeadLocation;
import com.reconinstruments.os.hardware.sensors.HeadLocationListener;
import com.reconinstruments.os.hardware.sensors.IHUDHeadingService;
import com.reconinstruments.os.hardware.sensors.IHeadingServiceCallback;

public class IHUDHeadingServiceImpl extends IHUDHeadingService.Stub implements HeadLocationListener {
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = false;

    final RemoteCallbackList<IHeadingServiceCallback> mCallbacks = new RemoteCallbackList<IHeadingServiceCallback>();

    private final HeadLocation mHeadLocation;

    IHUDHeadingServiceImpl(Context context) {
        mHeadLocation = new HeadLocation(context, this);
    }

    @Override
    public void register(IHeadingServiceCallback callback) throws RemoteException {
        if(callback != null) {
            if(DEBUG) Log.d(TAG, "registering callback: " + callback);

            synchronized (mCallbacks) {
                mCallbacks.register(callback);
                if(!mHeadLocation.isRunning()) {
                    mHeadLocation.start();
                }
            }
        }
    }

    @Override
    public void unregister(IHeadingServiceCallback callback) throws RemoteException {
        if(callback != null) {
            if(DEBUG) Log.d(TAG, "unregistering callback: " + callback);

            synchronized (mCallbacks) {
                mCallbacks.unregister(callback);
                int cbCount = mCallbacks.beginBroadcast();
                if((cbCount == 0) && mHeadLocation.isRunning()) {
                    mHeadLocation.stop();
                }
                mCallbacks.finishBroadcast();
            }
        }
    }

    @Override
    public void onHeadLocation(float yaw, float pitch, float roll) {
        synchronized (mCallbacks) {
            int cbCount = mCallbacks.beginBroadcast();
            if((cbCount == 0) && mHeadLocation.isRunning()) {
                mHeadLocation.stop();
            }
            for (int i = 0; i < cbCount; i++) {
                try {
                    mCallbacks.getBroadcastItem(i).onHeadLocation(yaw, pitch, roll);
                } catch (RemoteException e) {
                    Log.e(TAG, "onHeadLocation has remote (oneway) execption", e);
                }
            }
            mCallbacks.finishBroadcast();
        }
    }
}
