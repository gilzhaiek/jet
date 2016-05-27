package com.contour.utils;

import android.location.Location;
import android.location.LocationManager;

public class LocationUtils
{
    /***
     * Usually location fixes older than a minute are worthless
     */
    public static long DEFAULT_MAX_LOCATION_AGE_MS = 60 * 1000L;
    
    /***
     * Location fixes not accurate within 200 meters should be tossed
     */
    public static float MIN_ACCEPTABLE_ACCURACY = 200f;
    
    /**
     * Checks if the location fix is non-null and valid lat/long 
     * @param location
     * @return true if location is valid
     */
    public static boolean isLocationValid(Location location)
    {
        return location != null && Math.abs(location.getLatitude()) <= 90 && Math.abs(location.getLongitude()) <= 180;
    }

    /***
     * Checks if the location is valid and the age of the location if is less than DEFAULT_MAX_LOCATION_AGE_MS
     * 
     * @param location
     * @return true if the location is invalid or older than DEFAULT_MAX_LOCATION_AGE_MS
     */
    public static boolean isLocationStale(Location location)
    {
        return isLocationStale(location, DEFAULT_MAX_LOCATION_AGE_MS);
    }

    /***
     * Checks if the location is valid and the age of the location if is within a specified time interval
     * 
     * @param location
     * @param maxLocationAge <p/> max time interval in milliseconds for fresh location fix
     * @return true if the location is invalid or older than maxLocationAge
     */
    public static boolean isLocationStale(Location location, long maxLocationAge)
    {
        boolean invalid = isLocationValid(location) == false;
        boolean expired = System.currentTimeMillis() - location.getTime() > maxLocationAge;

        return invalid || expired;
    }
    
    /***
     * Check if this location fix is valid and came from the device GPS
     * @param location
     * @return true if GPS location
     */
    public static boolean isLocationFromGPS(Location location)
    {
        if (isLocationValid(location))
        {
            return location.getProvider().equals(LocationManager.GPS_PROVIDER);
        }
        return false;
    }
    
    /***
     * Checks if the location is valid and the age of the location if is less than DEFAULT_MAX_LOCATION_AGE_MS
     * 
     * @param location
     * @return true if the location is invalid or older than DEFAULT_MAX_LOCATION_AGE_MS
     */
    public static boolean locationAgeImproved(Location newLocation, Location prevLocation)
    {
        if (isLocationStale(newLocation) == false)
        {
            if(isLocationStale(prevLocation))
            {
                return true;
            }
            return newLocation.getTime() > prevLocation.getTime();
        }
        
        return false;
    }
    
    /***
     * compares two locations and returns true if newLocation is more accurate than prev
     * @param location
     * @return true if newLoc is more accurate the prevLoc
     */
    public static boolean locationAccuracyImproved(Location newLocation, Location prevLocation)
    {
        boolean improved = false;
        if (isLocationValid(newLocation) && newLocation.hasAccuracy())
        {
            if(isLocationValid(prevLocation) && prevLocation.hasAccuracy())
            {
                float newAccuracy = prevLocation.getAccuracy();
                float prevAccuracy = prevLocation.getAccuracy();
                if(prevAccuracy < MIN_ACCEPTABLE_ACCURACY && newAccuracy < prevAccuracy)
                {
                    improved = true;
                }
            } else
            {
                improved = true;
            }
        }
        
        return improved;
    }
}
