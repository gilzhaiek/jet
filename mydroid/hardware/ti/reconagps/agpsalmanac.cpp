/*
 *******************************************************************************
 *         
 *
 * FileName         :   AGPSAlmanac.cpp
 *
 * Description      :   GPS Almanac Base Functionality
 *
 * Author           :   RECON Instruments
 * Date             :   December 2013
 *
 * Comments         :   Provides access management to contained gpsalmanac data structure arra
 *                      factory allocation as well as conversions as per ICD-200
 *
 *                      This is pure virtual class; concrete parsers (Yuma/Sem) are 
 *                      Implemented in derived classes
 *                          
 ******************************************************************************
 */

#define LOG_TAG "RECON.AGPS"    // our identification for logcat (adb logcat RECON.AGPS:V *:S)

#include <stdlib.h>
#include "agpsnative.h"

// field ids
const unsigned int AGPSAlmanac::OFFSET_SVID      = 0;
const unsigned int AGPSAlmanac::OFFSET_HEALTH    = 1;
const unsigned int AGPSAlmanac::OFFSET_WEEK      = 2;
const unsigned int AGPSAlmanac::OFFSET_TOA       = 3;
const unsigned int AGPSAlmanac::OFFSET_ECC       = 4;
const unsigned int AGPSAlmanac::OFFSET_I0        = 5;
const unsigned int AGPSAlmanac::OFFSET_OMEGADOT  = 6;
const unsigned int AGPSAlmanac::OFFSET_SQRTA     = 7;
const unsigned int AGPSAlmanac::OFFSET_OMEGA0    = 8;
const unsigned int AGPSAlmanac::OFFSET_W         = 9;
const unsigned int AGPSAlmanac::OFFSET_M0        = 10;
const unsigned int AGPSAlmanac::OFFSET_AF0       = 11;
const unsigned int AGPSAlmanac::OFFSET_AF1       = 12;

// data ranges, as per ICD-GPS-200. ICD does not specify valid data range for several properties
// so bellow data ranges are pulled from http://www.navcen.uscg.gov/?pageName=gpsSem
const unsigned char  AGPSAlmanac::MIN_SVID       = 1;
const unsigned char  AGPSAlmanac::MAX_SVID       = 32;

const unsigned char  AGPSAlmanac::MIN_HEALTH     = 0;
const unsigned char  AGPSAlmanac::MAX_HEALTH     = 63;

const unsigned short AGPSAlmanac::MIN_WEEK       = 0;
const unsigned short AGPSAlmanac::MAX_WEEK       = 1023;

const unsigned int   AGPSAlmanac::MIN_TOA        = 0;
const unsigned int   AGPSAlmanac::MAX_TOA        = 602112;

const double         AGPSAlmanac::MIN_ECC        = 0.0;
const double         AGPSAlmanac::MAX_ECC        = 0.03;

const double         AGPSAlmanac::MIN_I0         = -9.999999E-2;
const double         AGPSAlmanac::MAX_I0         = +9.999999E-2;

const double         AGPSAlmanac::MIN_OMEGADOT   = -9.999999E-7;
const double         AGPSAlmanac::MAX_OMEGADOT   =  +9.999999E-7;

const double         AGPSAlmanac::MIN_SQRTA      =  79.0; 
const double         AGPSAlmanac::MAX_SQRTA      =  9999.99999;

const double         AGPSAlmanac::MIN_OMEGA0     = -1.0;
const double         AGPSAlmanac::MAX_OMEGA0     = +1.0;

const double         AGPSAlmanac::MIN_W          = -1.0;
const double         AGPSAlmanac::MAX_W          = +1.0;

const double         AGPSAlmanac::MIN_M0         = -1.0;
const double         AGPSAlmanac::MAX_M0         = +1.0;

const double         AGPSAlmanac::MIN_AF0        = -9.9999E-4;
const double         AGPSAlmanac::MAX_AF0        = +9.9999E-4;

const double         AGPSAlmanac::MIN_AF1        = -9.9999E-9;
const double         AGPSAlmanac::MAX_AF1        = +9.9999E-9;

const double         AGPSAlmanac::PI_VALUE       = 3.14159;

// default c-tor
AGPSAlmanac::AGPSAlmanac ()
{
   this->Reset();
}

AGPSAlmanac::~AGPSAlmanac ()
{
}

// almanac structure accessor; index is 0 based
gpsalmanac*  AGPSAlmanac::getAlmanac (unsigned int iOffset)
{
   return (iOffset >= mSVs) ? 0 : &(mAlmanac[iOffset]);  
}

// Factory method
AGPSAlmanac* AGPSAlmanac::Factory (int format)
{
   AGPSAlmanac* ret = 0;

   switch (format)
   {
      case com_reconinstruments_agps_GpsNative_ALMANAC_FORMAT_YUMA:
      {
         ret = new YUMAlmanac();
      }
      break;
  
      default:
          GPS_LOG_ERR("Requested Almanac format [%d] is currently not supported\n", format);
          return ret;
   }

   if (ret == 0)
      GPS_LOG_ERR("Memory allocation error\n");

   return ret;
}

// Internal helper to reset memory contents
void AGPSAlmanac::Reset ()
{
   mSVs = 0;
   for (unsigned int i = 0; i < GPS_MAX_SVS; i++)
   {
      memset(&(mAlmanac[i]), 0, sizeof(struct _gpsalmanac) );
   }
}


/* assignment of data value
   -- svid, health, week, toa : unsigned short
   -- ecc, i0, omegadot, sqrta, w, m0, af0, af1: double

   Note: This routine should be made robust to check data validity as per
         ICD-GPS-200  http://www.navcen.uscg.gov/?pageName=gpsICD200c

*/
GPSStatus AGPSAlmanac::AssignValue 
(
   unsigned int id,            // field id
   const char*  data,          // string data as parsed from particular serialized format
   gpsalmanac*  palm           // our gps almanac data structure
)
{
   if ( (!data) || (*data == '\0') ) return GPSStatusCode::GPS_E_ALMANAC_DATA;

   // ID
   if (id == AGPSAlmanac::OFFSET_SVID)
   {
       int val = atoi(data); 
       if ( (val < AGPSAlmanac::MIN_SVID) || (val > AGPSAlmanac::MAX_SVID) ) 
       {
          GPS_LOG_ERR("Space Vehicle ID out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       palm->svid = static_cast<unsigned char>(val);
       return GPSStatusCode::GPS_SUCCESS;
   }

   // HEALTH: 000 is ok!
   if (id == AGPSAlmanac::OFFSET_HEALTH)
   {
       int val = atoi(data); 
       if ( (val < AGPSAlmanac::MIN_HEALTH) || (val > AGPSAlmanac::MAX_HEALTH) ) 
       {
          GPS_LOG_ERR("Space Vehicle Health out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       palm->health = static_cast<unsigned char>(val);
       return GPSStatusCode::GPS_SUCCESS;
   }

   // GPS week
   if (id == AGPSAlmanac::OFFSET_WEEK)
   {
       int val = atoi(data); 
       if ( (val < AGPSAlmanac::MIN_WEEK) || (val > AGPSAlmanac::MAX_WEEK) ) 
       {
          GPS_LOG_ERR("Space Vehicle Week out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       palm->week = static_cast<unsigned short>(val);
       return GPSStatusCode::GPS_SUCCESS;
   }


   // Time of Applicability
   if (id == AGPSAlmanac::OFFSET_TOA)
   {
       int val = atoi(data); 
       if ( (val < static_cast<int>(AGPSAlmanac::MIN_TOA) ) || (val > static_cast<int>(AGPSAlmanac::MAX_TOA) ) ) 
       {
          GPS_LOG_ERR("Space Vehicle Time of Applicability out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       palm->toa = static_cast<unsigned int>(val);
       return GPSStatusCode::GPS_SUCCESS;
   }

   // Eccentricity
   if (id == AGPSAlmanac::OFFSET_ECC)
   {
       palm->ecc = strtod(data, NULL); 
       if ( (palm->ecc < AGPSAlmanac::MIN_ECC) || (palm->ecc > AGPSAlmanac::MAX_ECC) ) 
       {
          GPS_LOG_ERR("Space Vehicle Eccentricity out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }
 

   // Orbital Inclination
   if (id == AGPSAlmanac::OFFSET_I0)
   {
       palm->i0 = atof(data); 
       if ( (palm->i0 < AGPSAlmanac::MIN_I0) || (palm->i0 > AGPSAlmanac::MAX_I0) ) 
       {
          GPS_LOG_ERR("Space Vehicle Orbital Inclination out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }


   // Rate of Right Ascen
   if (id == AGPSAlmanac::OFFSET_OMEGADOT)
   {
       palm->omegadot = atof(data); 
       if ( (palm->omegadot < AGPSAlmanac::MIN_OMEGADOT) || (palm->omegadot > AGPSAlmanac::MAX_OMEGADOT) ) 
       {
          GPS_LOG_ERR("Space Vehicle Orbital Inclination out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }


   // SQRT(A)
   if (id == AGPSAlmanac::OFFSET_SQRTA)
   {
       palm->sqrta = atof(data); 
       if ( (palm->sqrta < AGPSAlmanac::MIN_SQRTA) || (palm->sqrta > AGPSAlmanac::MAX_SQRTA) ) 
       {
          GPS_LOG_ERR("Space Vehicle SQRT(A) out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }

   
   // Right Ascen at Week
   if (id == AGPSAlmanac::OFFSET_OMEGA0)
   {
       palm->omega0 = atof(data); 
       if ( (palm->omega0 < AGPSAlmanac::MIN_OMEGA0) || (palm->omega0 > AGPSAlmanac::MAX_OMEGA0) ) 
       {
          GPS_LOG_ERR("Space Vehicle Right Ascen at Week Epoch out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }


   // Argument of Perigee
   if (id == AGPSAlmanac::OFFSET_W)
   {
       palm->w = atof(data); 
       if ( (palm->w < AGPSAlmanac::MIN_W) || (palm->w > AGPSAlmanac::MAX_W) ) 
       {
          GPS_LOG_ERR("Space Vehicle Argument of Perigee out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }


   // Mean Anom
   if (id == AGPSAlmanac::OFFSET_M0)
   {
       palm->m0 = atof(data); 
       if ( (palm->m0 < AGPSAlmanac::MIN_M0) || (palm->m0 > AGPSAlmanac::MAX_M0) ) 
       {
          GPS_LOG_ERR("Space Vehicle Mean Anomaly out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }

   // Af0
   if (id == AGPSAlmanac::OFFSET_AF0)
   {
       palm->af0 = atof(data); 
       if ( (palm->af0 < AGPSAlmanac::MIN_AF0) || (palm->af0 > AGPSAlmanac::MAX_AF0) ) 
       {
          GPS_LOG_ERR("Space Vehicle Zeroth Order Clock Correction out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }


   // Af1
   if (id == AGPSAlmanac::OFFSET_AF1)
   {
       palm->af1 = atof(data); 
       if ( (palm->af1 < AGPSAlmanac::MIN_AF1) || (palm->af1 > AGPSAlmanac::MAX_AF1) ) 
       {
          GPS_LOG_ERR("Space Vehicle First Order Clock Correction out of Range (%s)\n", data);
          return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }

       return GPSStatusCode::GPS_SUCCESS;
   }
   

   // If here, this is internal error!
   return GPSStatusCode::GPS_E_INTERNAL;
}

// logging
void  AGPSAlmanac::log ()
{
   GPS_LOG_TRACE("GPS Almanac. Number of Space Vehicles: [%d]\n", mSVs);
   GPS_LOG_TRACE("+++++++++++++++++++++++++++++++++++++++++++++\n");

   for (unsigned int i = 0; i < mSVs; i++)
   {
       GPS_LOG_TRACE("\n");
       GPS_LOG_TRACE("Space Vehicle: [%d]\n", i);
       GPS_LOG_TRACE("-------------------------------\n");
       this->logEntry (&(mAlmanac[i]) );
       GPS_LOG_TRACE("\n");
   }
}

void  AGPSAlmanac::logEntry (gpsalmanac* palm)
{
   GPS_LOG_TRACE("PRN: %d\n", palm->svid);
   GPS_LOG_TRACE("Health: %03d\n", palm->health);
   GPS_LOG_TRACE("GPS Week: %d\n", palm->week);
   GPS_LOG_TRACE("TOA: %d\n", palm->toa);
   GPS_LOG_TRACE("Eccentricity: %0.10E\n", palm->ecc);
   GPS_LOG_TRACE("Orbital Inclination: %0.10E\n", palm->i0);
   GPS_LOG_TRACE("Omega Dot: %0.10E\n", palm->omegadot);
   GPS_LOG_TRACE("SQRT(A): %0.10E\n", palm->sqrta);
   GPS_LOG_TRACE("Right Ascen at Weekly Epoch: %0.10E\n", palm->omega0);
   GPS_LOG_TRACE("Perigee: %0.10E\n", palm->w);
   GPS_LOG_TRACE("Mean Anomaly: %0.10E\n", palm->m0);
   GPS_LOG_TRACE("Clock Bias: %0.10E\n", palm->af0);
   GPS_LOG_TRACE("Clock Drift: %0.10E\n", palm->af1);

}



