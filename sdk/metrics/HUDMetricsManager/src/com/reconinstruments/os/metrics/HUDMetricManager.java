package com.reconinstruments.os.metrics;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

public class HUDMetricManager implements ServiceConnection{
    private final String TAG = this.getClass().getSimpleName();

    @SuppressWarnings("unused")
    private static final boolean DEBUG = true; // change to true to enable debugging

    // Make sure we jump in 100s 

    private static IHUDMetricService mIHUDMetricService = null;
    private ServiceConnection mClientHandler = null;

    private int mNextListenerID = 1;
    protected SparseArray<MetricChangedListener> mMetricChangedListeners = new SparseArray<MetricChangedListener>();
    protected ListenersTransport mListenersTransport = new ListenersTransport(mMetricChangedListeners);

    public HUDMetricManager(Context context, ServiceConnection clientHandler) {
        if (!context.bindService(new Intent(IHUDMetricService.class.getName()), this, Context.BIND_AUTO_CREATE)) {
            Log.w(TAG, "Failed to bind to service");
        }

        mClientHandler = clientHandler;
    }

    private class ListenersTransport extends IHUDMetricListener.Stub {
        private SparseArray<MetricChangedListener> mListeners;

        public ListenersTransport(SparseArray<MetricChangedListener> listeners) {
            mListeners = listeners;
        }

        @Override
        public void onMetricDataChanged(int listenerID, int type, Bundle metricData) throws RemoteException {
            Message message = mHandler.obtainMessage();
            if(metricData == null) {
                Log.e(TAG, "onMetricDataChanged: " + listenerID + " type:" + type + " is null");
                return;
            } else {
                Log.d(TAG, "onMetricDataChanged: " + listenerID + " type:" + type);
            }

            message.arg1 = listenerID;
            message.arg2 = type;
            message.setData(metricData);
            mHandler.sendMessage(message);
        }

        // We are ignoring the leak warning as this class should not be GC 
        @SuppressLint("HandlerLeak")
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (mListeners == null) return;

                Bundle metricData = message.getData();
                MetricChangedListener listener = mListeners.get(message.arg1);
                if(message.arg2 == Bundler.BUNDLE_TYPE_FLOAT) {
                    Bundler.onBundleReceiverF(listener, metricData);
                }
            }
        };

    }	

    public boolean isServiceConnected() {
        return (mIHUDMetricService != null); 
    }

    public BaseValue getMetric(int hudMetricID) throws RemoteException {
        if(mIHUDMetricService == null) {
            Log.e(TAG,"registerMetricListener: service is null, please pause a bit before calling the constructor");
            return null;
        }	

        return mIHUDMetricService.getMetricValue(hudMetricID);
    }

    /**
     * Register a {@link com.reconinstruments.os.metrics.MetricChangedListener MetricChangedListener}
     * for the given metric.
     * 
     * @param listener
     * 		  A {@link com.reconinstruments.os.metrics.MetricChangedListener MetricChangedListener} object.
     * 
     * @param hudMetricID
     * 		  The {@link com.reconinstruments.os.metrics.HUDMetricIDs HUDMetricIDs} to register to.
     * 
     * @return <code>true</code> if the metric service is running properly.
     * 
     * @throws RemoteException
     * 
     * @see {@link #unregisterMetricListener(MetricChangedListener, int)}
     */
    public boolean registerMetricListener(MetricChangedListener listener, int hudMetricID) throws RemoteException {
        if(mIHUDMetricService == null) {
            Log.e(TAG,"registerMetricListener: service is null, please pause a bit before calling the constructor");
            return false;
        }

        int listenerID = 0;
        int idx = mMetricChangedListeners.indexOfValue(listener); 
        if(idx < 0) { // listener Doesn't exists
            mMetricChangedListeners.put(mNextListenerID, listener);
            listenerID = mNextListenerID++;
        } else {
            listenerID = mMetricChangedListeners.keyAt(idx);
        }

        mIHUDMetricService.registerMetricListener(mListenersTransport, listenerID, hudMetricID);

        return true;
    }

    /**
     * Unregisters a listener for the metric with which it is registered.
     * 
     * @param listener
     * 		  A {@link com.reconinstruments.os.metrics.MetricChangedListener MetricChangedListener} object.
     * 
     * @param hudMetricID
     * 		  The {@link com.reconinstruments.os.metrics.HUDMetricIDs HUDMetricIDs} to unregister from.
     * 
     * @return <code> true </code> 
     * @throws RemoteException
     */
    public boolean unregisterMetricListener(MetricChangedListener listener, int hudMetricID) throws RemoteException {
        if(mIHUDMetricService == null) {
            Log.w(TAG,"unregisterMetricListener: service is null, please pause a bit before calling the constructor");
            return false;
        }

        int listenerID = mMetricChangedListeners.indexOfValue(listener);
        if(listenerID < 0) { // listener Doesn't exists
            Log.w(TAG,"unregisterMetricListener: Listener was never registered with the service");
            return false;
        }

        mIHUDMetricService.unregisterMetricListener(mListenersTransport, listenerID, hudMetricID);
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected() to " + name);
        mIHUDMetricService = IHUDMetricService.Stub.asInterface(service); // Called on the main thread
        mClientHandler.onServiceConnected(name, service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "onServiceDisconnected() to " + name);
        mIHUDMetricService = null;
        mClientHandler.onServiceDisconnected(name);
    }
}