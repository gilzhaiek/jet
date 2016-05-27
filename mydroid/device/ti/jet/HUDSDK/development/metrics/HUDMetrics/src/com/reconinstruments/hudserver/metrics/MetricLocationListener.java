package com.reconinstruments.hudserver.metrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class MetricLocationListener implements LocationListener, GpsStatus.Listener {
    private static final String TAG = "MetricLocationListener";

    private static MetricLocationListener instance = null; 

    private LocationManager mLocationManager = null;

    private GpsStatus mLastGpsStatus = null; // Comes before location - so we need to save this first
    List <MetricLocationHandler> mMetricLocationClients = new ArrayList<MetricLocationHandler>();

    private MetricLocationListener(Context context) {
        mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static MetricLocationListener init(Context context) {
        if(instance != null) {
            Log.e(TAG, "init: MetricLocationListener is already initilized");
        } else {
            instance = new MetricLocationListener(context);
        }
        return instance;
    }

    public static MetricLocationListener getInstance() {
        if(instance == null) {
            Log.e(TAG, "getInstance: MetricLocationListener is not initilized, returning null");
        } 
        return instance;
    }

    protected void start(MetricLocationListener listener) {
        new AsyncTask<MetricLocationListener, Void, MetricLocationListener>(){

            @Override
            protected MetricLocationListener doInBackground(MetricLocationListener... l) {
                return l[0];
            }

            @Override
            protected void onPostExecute(MetricLocationListener l){
                mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, l);
                mLocationManager.addGpsStatusListener(l);
            }

        }.execute(listener);
    }

    protected void stop() {
        mLocationManager.removeUpdates(this);	
        mLocationManager.removeGpsStatusListener(this);
    }

    public void register(MetricLocationHandler client) {
        synchronized (mMetricLocationClients) {			
            if(mMetricLocationClients.contains(client)) {
                Log.w(TAG, "register MetricLocationHandler ("+client.getClass().getName()+") already exists");
            } else {				
                mMetricLocationClients.add(client);
                // Starting GPS Location update when we have the first client
                if(mMetricLocationClients.size() == 1) { 
                    start(this);
                }
            }
        }
    }

    public void unregister(MetricLocationHandler client) {
        synchronized (mMetricLocationClients) {			
            if(mMetricLocationClients.contains(client)) {
                mMetricLocationClients.remove(client);
                // Stopping GPS Location update when no clients listening 
                if(mMetricLocationClients.isEmpty()) {
                    stop();
                }
            } else {
                Log.w(TAG, "unregister MetricLocationHandler does not contain client ("+client.getClass().getName()+")");
            }
        }
    }

    /*
     * For each of the following functions: onLocationChanged, onStatusChanged
     * We need to be very careful in the cases where a client takes it's time
     * We will print out the error and still continue to execute the function as we don't want to
     * lose any events
     * We will also catch any exception that comes from any client and print the information
     * We still missing a timeout where a client takes it's time
     */

    public int onLocationChangedBlocked = 0;
    public String onLocationChangedLastClientName = "NULL";
    @Override
    public void onLocationChanged(Location location) {
        if(mLastGpsStatus == null) {
            Log.w(TAG,"onLocationChanged - GpsStatus is null, ignoring location");
            return;
        }

        if(location == null) {
            Log.e(TAG,"onLocationChanged - Location is null");
            return;
        }

        if(onLocationChangedBlocked > 0) {
            Log.e(TAG,"onLocationChanged is blocked ("+onLocationChangedBlocked+") on " + onLocationChangedLastClientName);
        }

        int numOfSatsInFix = 0;
        Iterator<GpsSatellite> gpsSatsIter = mLastGpsStatus.getSatellites().iterator();
        while (gpsSatsIter.hasNext()) {
            if(gpsSatsIter.next().usedInFix()) {
                numOfSatsInFix++;
            }
        }

        onLocationChangedBlocked++;
        try {
            for (MetricLocationHandler client : mMetricLocationClients) {
                onLocationChangedLastClientName = client.getClass().getName();
                client.onLocationChanged(location, mLastGpsStatus, numOfSatsInFix);
            } 
        } catch (Exception e) {
            Log.e(TAG, "onLocationChanged - a "+onLocationChangedLastClientName+" has crashed", e);
        }
        onLocationChangedBlocked--;
    }

    public int onStatusChangedBlocked = 0;
    public String onStatusChangedLastClientName = "NULL";
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if(onStatusChangedBlocked > 0) {
            Log.e(TAG,"onStatusChanged is blocked ("+onStatusChangedBlocked+") on " + onStatusChangedLastClientName);
        }

        onStatusChangedBlocked++;
        try {
            for (MetricLocationHandler client : mMetricLocationClients) {
                onStatusChangedLastClientName = client.getClass().getName();
                client.onStatusChanged(provider, status, extras);
            } 
        } catch (Exception e) {
            Log.e(TAG, "onStatusChanged - a "+onStatusChangedLastClientName+" has crashed", e);
        }
        onStatusChangedBlocked--;
    }

    @Override
    public void onGpsStatusChanged(int event) {
        mLastGpsStatus = mLocationManager.getGpsStatus(null);
    }

    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
}
