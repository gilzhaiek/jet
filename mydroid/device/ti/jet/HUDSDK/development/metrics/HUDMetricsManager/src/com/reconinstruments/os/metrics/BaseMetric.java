package com.reconinstruments.os.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class BaseMetric {
    private final String TAG = this.getClass().getSimpleName();

    static final boolean DEBUG = true;
    static final boolean localLOGV = DEBUG;
    @SuppressWarnings("unused") static final boolean DEBUG_VALUES = localLOGV || false;	
    @SuppressWarnings("unused") static final boolean DEBUG_PARCEL = localLOGV || false;
    @SuppressWarnings("unused") static final boolean DEBUG_PERSSITANT = localLOGV || false;

    private final Map<IBinder, ListenerTracker> listeners = new HashMap<IBinder, ListenerTracker>();

    protected List<MetricChangedListener> mMetricChangedHandlersOnValue = new ArrayList<MetricChangedListener>();

    protected ListenerChangedHandler mListenerChangedHandler = null;

    Bundle mMetricBundle = new Bundle();

    // Final Values
    protected int mMetricID = -1;
    protected String mMetricName = "INVALID";

    // Dynamic Values
    protected boolean mHasValue = false;
    BaseValue mValue = new BaseValue();

    public int getMetricID() { return mMetricID; }

    public float getValue() { return mValue.Value; }
    public BaseValue getBaseValue() { return mValue; }

    /** @hide */	
    public BaseMetric(int hudMetricID) {
        mMetricID = hudMetricID;
    }

    /** @hide */	
    public void setMetricChangedListener(ListenerChangedHandler listenerChangedHandler) {
        mListenerChangedHandler = listenerChangedHandler;
    }

    /**
     * @param metricChangedListener - the base handler class that needs to be implemented by the receiving class
     */
    public void registerListener(MetricChangedListener metricChangedListener) {
        synchronized (mMetricChangedHandlersOnValue) {
            if(!hasListeners()){
                mListenerChangedHandler.onServiceStatusChange(ListenerChangedHandler.STATUS_ENABLE_SERVICE);
            }
            if(!mMetricChangedHandlersOnValue.contains(metricChangedListener)) {
                mMetricChangedHandlersOnValue.add(metricChangedListener);
            }	
        }
    }

    /**
     * @param metricChangedListener - the base handler class that needs to be implemented by the receiving class
     */
    public void unregisterListener(MetricChangedListener metricChangedListener) {
        synchronized (mMetricChangedHandlersOnValue) {
            if(mMetricChangedHandlersOnValue.contains(metricChangedListener)) {
                mMetricChangedHandlersOnValue.remove(metricChangedListener);
            }
            if(!hasListeners()){
                mListenerChangedHandler.onServiceStatusChange(ListenerChangedHandler.STATUS_DISABLE_SERVICE);
            }
        }
    }

    /**
     * @return the time in millisecond when the latest value was inserted
     */
    public long getTimeMillisLatestChange() {
        return mValue.ChangeTime;
    }

    /**
     * @return the time in millisecond since a value was inserted
     */
    public long getTimeMillisSinceLastChange() {
        return System.currentTimeMillis() - getTimeMillisLatestChange();
    }

    /**
     * @return true if there was at least one valid value inserted
     */
    public boolean hasValue() {
        return mHasValue;
    }

    public boolean isValueValid() {
        return mValue.isValidFloat();
    }

    /** @hide */
    public void addInvalidValue() {
        addValue(Float.NaN);
    }

    /** @hide */
    public void addValue(float value) {
        addValue(value, System.currentTimeMillis());
    }

    /** @hide */
    public void addValue(float value, long changeTime) {
        mHasValue = true;
        mValue.Value = value;
        mValue.ChangeTime = changeTime;
        if(DEBUG_VALUES) Log.v(TAG,mMetricName + ":addValue - "+mValue.Value);

        for(int i = 0; i < mMetricChangedHandlersOnValue.size(); i++) {
            mMetricChangedHandlersOnValue.get(i).onValueChanged(mMetricID, value, changeTime, isValueValid());
        }

        Bundle bundle = null;
        if(this.listeners.size() > 0) {
            bundle = new Bundle();
            Bundler.addToBundleF(bundle, this);

            synchronized(this.listeners) {
                for (Map.Entry<IBinder, ListenerTracker> entry : this.listeners.entrySet()) {
                    ListenerTracker listenerTracker = entry.getValue();
                    try {
                        if (DEBUG) Log.d(TAG, "addValue: Notifying listener: " +  listenerTracker.getListenerID());
                        listenerTracker.getListener().onMetricDataChanged(listenerTracker.getListenerID(), Bundler.BUNDLE_TYPE_FLOAT, bundle);
                    } catch (RemoteException e) {
                        Log.e(TAG, "addValue: Failed to update listener: " + entry.getKey(), e);
                        this.unregister(listenerTracker.getListener());
                    }
                }
            }
        }
    }

    /** @hide */
    public void load(SharedPreferences persistantStats) {
        mHasValue = persistantStats.getBoolean(mMetricName + ".HasValue", mHasValue);
        mValue.Value = persistantStats.getFloat(mMetricName + ".Value", mValue.Value);
        mValue.ChangeTime = persistantStats.getLong(mMetricName + ".ChangeTime", mValue.ChangeTime);

        if(DEBUG_PERSSITANT) Log.v(TAG, mMetricName + ":load - persistant stats loaded");
    }

    /** @hide */
    public void save(Editor persistantStatsEditor) {
        persistantStatsEditor.putBoolean(mMetricName + ".HasValue", mHasValue);
        persistantStatsEditor.putFloat(mMetricName + ".Value", mValue.Value);
        persistantStatsEditor.putLong(mMetricName + ".ChangeTime", mValue.ChangeTime);

        if(DEBUG_PERSSITANT) Log.v(TAG, mMetricName + ":save - persistant stats saved");
    }

    /** @hide */
    public void reset(){
        mHasValue = false;
        mValue.reset(); 
    }

    /** @hide */
    public boolean hasListeners(){
        synchronized(this.listeners){
            if (!this.listeners.isEmpty()) {
                return true;
            }
        }
        synchronized(this.mMetricChangedHandlersOnValue){
            if(!this.mMetricChangedHandlersOnValue.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void register(IHUDMetricListener listener, int listenerID) throws RemoteException {
        if (listener != null) {
            IBinder binder = listener.asBinder();
            synchronized(this.listeners) {
                if (this.listeners.containsKey(binder)) {
                    Log.w(TAG, "Ignoring duplicate listener: " + binder);
                } else {
                    if(!hasListeners()){
                        mListenerChangedHandler.onServiceStatusChange(ListenerChangedHandler.STATUS_ENABLE_SERVICE);
                    }
                    ListenerTracker listenerTracker = new ListenerTracker(listener, listenerID);
                    binder.linkToDeath(listenerTracker, 0);
                    this.listeners.put(binder, listenerTracker);
                    if (DEBUG) Log.d(TAG, "Registered listener: " + binder);
                }
            }
        }

    }

    public void unregister(IHUDMetricListener listener) {
        if (listener != null) {
            IBinder binder = listener.asBinder();
            synchronized(this.listeners) {
                ListenerTracker listenerTracker = this.listeners.remove(binder);
                if (listenerTracker != null) {
                    if (DEBUG) Log.d(TAG, "Unregistered listener: " + binder);
                    binder.unlinkToDeath(listenerTracker, 0);
                }
                if(!hasListeners()){
                    mListenerChangedHandler.onServiceStatusChange(ListenerChangedHandler.STATUS_DISABLE_SERVICE);
                }
            }
        }
    }

    private final class ListenerTracker implements IBinder.DeathRecipient {
        private final IHUDMetricListener mListener;
        private final int mListenerID;

        public ListenerTracker(IHUDMetricListener listener, int listenerID) {
            this.mListener = listener;
            this.mListenerID = listenerID;
        }

        public IHUDMetricListener getListener() {
            return this.mListener;
        }

        public int getListenerID() {
            return this.mListenerID;
        }

        public void binderDied() {
            BaseMetric.this.unregister(this.mListener);
        }
    }


}
