package com.contour.connect.data;

import java.util.EnumSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class DataController
{
    public static final String TAG = "DataController";
    
    private final SensorManager mSensorManager;
    private final LocationManager mLocationManager;
    private final ContentResolver   mContentResolver;
    private final SharedPreferences mSharedPreferences;
    
    final DataControllerListener mDataControllerListener;
    
    final Handler mHandler;
    
    final CompassListener                              mCompassListener;
    final CurrentLocationListener                      mLocationListener;
    final DataControllerSharedPreferenceChangeListener mPrefChangeListener;
    
    private final Set<DataManager.DataType> mCurrentDataListeners = EnumSet.noneOf(DataManager.DataType.class);
    
    public DataController(Context context, DataControllerListener listener)
    {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mContentResolver = context.getContentResolver();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mDataControllerListener = listener;
        mHandler = new Handler();

        mCompassListener = new CompassListener();
        mLocationListener = new CurrentLocationListener();
        mPrefChangeListener = new DataControllerSharedPreferenceChangeListener();
    }
    
    void updateListeners(EnumSet<DataManager.DataType> listeners)
    {
        
        Set<DataManager.DataType> removableListeners = EnumSet.copyOf(mCurrentDataListeners);
        removableListeners.removeAll(listeners);
        
        Set<DataManager.DataType> newListeners = EnumSet.copyOf(listeners);
        newListeners.removeAll(mCurrentDataListeners);

        for (DataManager.DataType type : removableListeners) 
        {
          unregisterListener(type);
        }

        for (DataManager.DataType type : newListeners) 
        {
          registerListener(type);
        }

        mCurrentDataListeners.clear();
        mCurrentDataListeners.addAll(listeners);
    }
    
    void unregisterAllListeners()
    {
        EnumSet<DataManager.DataType> allListeners = EnumSet.allOf(DataManager.DataType.class);
        for(DataManager.DataType listener : allListeners)
        {
            unregisterListener(listener);
        }
    }

    private void registerListener(DataManager.DataType dataType)
    {
        switch(dataType)
        {
            case COMPASS:
                Sensor compass = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
                if (compass != null)
                {
                    mSensorManager.registerListener(mCompassListener, compass, SensorManager.SENSOR_DELAY_UI);
                }
                break;
            case LOCATION:
                registerLocationUpdates();
                break;
            case PREFERENCES:
                mSharedPreferences.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
                break;
        }
    }
    
    private void unregisterListener(DataManager.DataType dataType)
    {
        switch(dataType)
        {
            case COMPASS:
                mSensorManager.unregisterListener(mCompassListener);
                break;
            case LOCATION:
                mLocationManager.removeUpdates(mLocationListener);
                break;
            case PREFERENCES:
                mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
                break;
        }
        
//        mContentResolver.unregisterContentObserver(mSavedPointChangeListener);
    }
    
    private void registerLocationUpdates()
    {
        LocationProvider gpsProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (gpsProvider == null)
        {
            mLocationListener.onProviderDisabled(LocationManager.GPS_PROVIDER);
        } else
        {
            // Listen to GPS location.
            String gpsProviderName = gpsProvider.getName();
            mLocationManager.requestLocationUpdates(gpsProviderName, 0, 0 , mLocationListener);
            boolean gpsListenerAdded = mLocationManager.addGpsStatusListener(mLocationListener);
            
            // Give an initial update on provider state.
            if (mLocationManager.isProviderEnabled(gpsProviderName))
            {
                mLocationListener.onProviderEnabled(gpsProviderName);
            } else
            {
                mLocationListener.onProviderDisabled(gpsProviderName);
            }
        }
        // Listen to network location
        try
        {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 60 * 1 /* minTime */, 0 /* minDist */, mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000 * 60 * 1 /* minTime */, 0 /* minDist */, mLocationListener);
        } catch (RuntimeException e)
        {
            Log.w(TAG, "Could not register network location listener.", e);
        }
    }
    
    class DataControllerSharedPreferenceChangeListener implements OnSharedPreferenceChangeListener
    {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            mDataControllerListener.sharedPreferenceChanged(key);
        }
    }
    
    class SavedPointChangeListener extends ContentObserver
    {
        public SavedPointChangeListener()
        {
            super(mHandler);
        }
        
        @Override
        public void onChange(boolean selfChange)
        {
            mDataControllerListener.savedPointsChanged();
        }
    }
    
    private class CompassListener implements SensorEventListener
    {
        @Override
        public void onSensorChanged(SensorEvent event)
        {
            mDataControllerListener.azimuthChanged(event.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
            // Do nothing
        }
    }
    
    class CurrentLocationListener implements LocationListener, android.location.GpsStatus.Listener
    {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
                mDataControllerListener.locationProviderAvailable(status == LocationProvider.AVAILABLE);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
             mDataControllerListener.locationProviderEnabled(true);
        }

        @Override
        public void onProviderDisabled(String provider)
        {
             mDataControllerListener.locationProviderEnabled(false);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            mDataControllerListener.locationChanged(location);
        }

        @Override
        public void onGpsStatusChanged(int event)
        {

            switch (event)
            {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
            break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            break;
            case GpsStatus.GPS_EVENT_STARTED:
            break;
            case GpsStatus.GPS_EVENT_STOPPED:
            break;
            }
        }
    }
    
    interface DataControllerListener
    {
        void sharedPreferenceChanged(String key);
        void savedPointsChanged();
        void azimuthChanged(float azimuth);
        void locationProviderAvailable(boolean b);
        void locationProviderEnabled(boolean b);
        void locationChanged(Location location);
    }
}
