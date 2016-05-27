package com.contour.connect.data;

import android.location.Location;

public interface DataListener
{
    public enum ProviderStatus
    {
        NO_PROVIDER,
        NETWORK_DISABLED,
        NETWORK_ENABLED,
        GPS_ENABLED,
        GPS_DIABLED,
        
    }
    
    void onLocationProviderChanged(ProviderStatus providerStatus);
    void onCurrentLocationUpdate(Location location);
    void onAzimuthUpdate(float azimuth);
    void onSpeedUpdate(float speedMph);
}
