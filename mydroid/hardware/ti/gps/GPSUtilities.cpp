/*
 *******************************************************************************
 *         
 *
 * FileName         :   GPSUtilities.cpp
 *
 * Description      :   Misc. Helper functions for purpose of GPS Library
 *
 * Author           :   RECON Instruments
 * Date             :   November 2013
 *
 * Comments         :  
 ******************************************************************************
 */   

#define LOG_TAG "RECON.GPS"   // our identification for logcat (adb logcat RECON.GPS:V *:S)

#include <hardware/gps.h>
#include <stdlib.h>
#include <math.h>


#include "recongps.h"         // main include

const unsigned int GPSUtilities::SEC_2_MSEC = 1000;
const unsigned int GPSUtilities::WEEK_MSEC = 7 * 24 * 60 * 60 * 1000;

// initialize leap seconds array
const unsigned int GPSUtilities::_leapseconds [] =
{
   46828800, 78364801, 109900802, 173059203, 252028804, 315187205, 346723206, 393984007, 
   425520008, 457056009, 504489610, 551750411, 599184012, 820108813, 914803214, 1025136015
};

// initialize utc - gps time offset (seconds)
const unsigned int GPSUtilities::_gpsoffset = 315964800;


/* Conversion from Unix time (1.1.1970) to Gps Time (1.1.1980) */
GpsUtcTime GPSUtilities::unix2gps (GpsUtcTime unixtime_msec)
{
   GpsUtcTime unixtime_sec = unixtime_msec / GPSUtilities::SEC_2_MSEC;

   GpsUtcTime gpssec = unixtime_sec - GPSUtilities::_gpsoffset;   // subtract offset
   gpssec += GPSUtilities::countleaps (gpssec, 0);             // add leap seconds
 
   // convert back to miliseconds
   return ( (gpssec * GPSUtilities::SEC_2_MSEC) + (unixtime_msec % GPSUtilities::SEC_2_MSEC) );
}


/* Conversion from Gps Time (1.1.1980) to Unix Time (1.1.1970) */
GpsUtcTime GPSUtilities::gps2unix (GpsUtcTime gpstime_msec)
{
  GpsUtcTime gpstime_sec = gpstime_msec / GPSUtilities::SEC_2_MSEC;

  // calc unix seconds: gps seconds + offset - leap seconds
  GpsUtcTime unixsec = gpstime_sec + GPSUtilities::_gpsoffset - GPSUtilities::countleaps (gpstime_sec, 1);

  // convert back to miliseconds
  return ( (unixsec * GPSUtilities::SEC_2_MSEC) + (gpstime_msec % GPSUtilities::SEC_2_MSEC) );
}

/* Convert passed UTC Time in milliseconds to GPS week number and milisecond offset into current week */
void GPSUtilities::get_gps_time 
(
   GpsUtcTime    utctime_msec, // [in]:  UTC Time in milliseconds
   unsigned int* pgps_week,    // [out]: Resulting GPS Week number
   unsigned int* pweek_msec    // [out]: Resulting offset into current GPS Week in miliseconds
)
{
   // first convert to GPS Time
   GpsUtcTime gpstime_msec = GPSUtilities::unix2gps(utctime_msec);
   
   // get number of weeks
   *pgps_week = gpstime_msec / GPSUtilities::WEEK_MSEC;

   // get offset into current week
   *pweek_msec = gpstime_msec % GPSUtilities::WEEK_MSEC;
}

/* Internal Test to see if a GPS second is a leap second */
unsigned char GPSUtilities::isleap (unsigned int gpssecond)
{
    int lenLeaps = sizeof(GPSUtilities::_leapseconds) / sizeof(GPSUtilities::_leapseconds[0]);

    for (int i = 0; i < lenLeaps; i++)
    {
        if (gpssecond == GPSUtilities::_leapseconds[i]) return 1;
    }
    
    return 0;
}

/* Internal Test to count number of leap seconds that have passed */
unsigned int GPSUtilities::countleaps
(
   GpsUtcTime    seconds,   // total number of seconds
   unsigned char dirFlag    // unixtogps 0, gpstounix > 0
)
{
    int lenLeaps = sizeof(GPSUtilities::_leapseconds) / sizeof(GPSUtilities::_leapseconds[0]);
    unsigned int nleaps = 0;  // number of leap seconds prior to gpsTime

    for (int i = 0; i < lenLeaps; i++) 
    {
         if (dirFlag == 0) 
         {
            if (seconds >= GPSUtilities::_leapseconds[i] - i)
               nleaps++;       
         } 
         else
         {
            if (seconds >= GPSUtilities::_leapseconds[i])
               nleaps++;

         } 
     }
      
    return nleaps;
}


// ASCII parsing (ephemeris, almanac) pre-process: Throwaway trim left/right blanks
char* GPSUtilities::TrimLine 
(
   char* line /* parsed line, null-terminated */
)
{
   char* start = line;
   char* end = start + strlen(line);

   // left trim
   while (start < end)
   {
      // exit left hunt with first non-blank char. (Account for CR stupid DOS will give us)
      if ( (*start != ' ') && (*start != '\r') ) break;
      start++;
   }

   // right trim
   end--;
   while (end > start)
   {
      // exit with first non-blank
      if ( (*end != ' ') && (*end != '\r') ) break;
      
      end--;
   }

   *(end + 1) = '\0';
   return start;
}





