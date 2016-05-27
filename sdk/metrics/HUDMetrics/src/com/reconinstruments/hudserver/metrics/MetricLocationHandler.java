package com.reconinstruments.hudserver.metrics;

import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;

public interface MetricLocationHandler {
    public void onLocationChanged(Location location, GpsStatus gpsStatus, int numOfSatsInFix);
    public void onStatusChanged(String provider, int status, Bundle extras);
}
