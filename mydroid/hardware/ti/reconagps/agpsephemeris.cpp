/*
 *******************************************************************************
 *         
 *
 * FileName         :   AGPSEphemeris.cpp
 *
 * Description      :   GPS Ephemeris Functionality
 *
 * Author           :   RECON Instruments
 * Date             :   March 2014
 *
 * Comments         :   Provides access management to contained emphemeris data structure
 *                      as well as conversions as per ICD-200
 *
 *                          
 ******************************************************************************
 */

#define LOG_TAG "RECON.AGPS"    // our identification for logcat (adb logcat RECON.AGPS:V *:S)

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <math.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include "agpsnative.h"

// constant initialization
const unsigned int AGPSEphemeris::RINEX_MAX_LINE      = 100; 
const char*        AGPSEphemeris::RINEX_VERSION_TYPE  = "RINEX VERSION / TYPE";
const char*        AGPSEphemeris::RINEX_FILE_TYPE     = "N: GPS NAV DATA";
const char*        AGPSEphemeris::RINEX_END_OF_HEADER = "END OF HEADER";
const double       AGPSEphemeris::RINEX_VERSION       = 2.10;

// record ids
const unsigned int AGPSEphemeris::RECORD_PRN        = 0;
const unsigned int AGPSEphemeris::RECORD_ORBIT_1    = 1;
const unsigned int AGPSEphemeris::RECORD_ORBIT_2    = 2;
const unsigned int AGPSEphemeris::RECORD_ORBIT_3    = 3;
const unsigned int AGPSEphemeris::RECORD_ORBIT_4    = 4;
const unsigned int AGPSEphemeris::RECORD_ORBIT_5    = 5;
const unsigned int AGPSEphemeris::RECORD_ORBIT_6    = 6;
const unsigned int AGPSEphemeris::RECORD_ORBIT_7    = 7;

// data ranges, as per ICD-GPS-200
// NOTE: For Almanac I referenced additional range rules found in http://www.navcen.uscg.gov/?pageName=gpsSem
//       For Ephemeris I could not something similar, so I am reverting to ICD spec 
//       http://geoweb.mit.edu/~tah/icd200c123.pdf  Table 20-I and 20-II, which is not very much
const double         AGPSEphemeris::MIN_TOE         = 0;
const double         AGPSEphemeris::MAX_TOE         = 604784;

// default c-tor
AGPSEphemeris::AGPSEphemeris ()
{
   this->Reset();
}

AGPSEphemeris::~AGPSEphemeris ()
{
}

// ephemeris structure accessor; index is 0 based
gpsephemeris*  AGPSEphemeris::getEphemeris (unsigned int iOffset)
{
   return (iOffset >= mSVs) ? 0 : &(mEphemeris[iOffset]);  
}



// Internal helper to reset memory contents
void AGPSEphemeris::Reset ()
{
   mVersion = 0.0;
   mSVs = 0;
   for (unsigned int i = 0; i < GPS_MAX_SVS; i++)
   {
      memset(&(mEphemeris[i]), 0, sizeof(struct _gpsephemeris) );
   }

   memset(&mRawEphemeris, 0, sizeof(struct _gpsrawephemeris) );
}

// Internal helper to correctly interpret D19.12 field
void AGPSEphemeris::handleD
(
   const char* data,    // [in]   "-3.183231456205D-12", guaranteed to be null terminated
   double*     val      // [out]  result
)
{
   *val = atof(data);   // in each case start with this

   char* d = strchr(data, 'D');
   if (d) 
      *val *= pow(10, atoi(d + 1) );
}

/* Internal helper to Read Rinex Header and retrieve version */
GPSStatus AGPSEphemeris::ParseHeader(FILE* pfh)
{
   // RINEX is positional. First line is RINEX VERSION and TYPE
   //      2.10           N: GPS NAV DATA                         RINEX VERSION / TYPE
   char line[RINEX_MAX_LINE];

   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   // Must have Version Type string
   if (!strstr(line, AGPSEphemeris::RINEX_VERSION_TYPE) )
   {
      GPS_LOG_ERR("Invalid RINEX File - 1st Line does not contain [%s] String\n", AGPSEphemeris::RINEX_VERSION_TYPE);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   // Must have File Type string
   if (!strstr(line, AGPSEphemeris::RINEX_FILE_TYPE) )
   {
      GPS_LOG_ERR("Invalid RINEX File - 1st Line does not contain [%s] String\n", AGPSEphemeris::RINEX_FILE_TYPE);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   // get version. 
   char ver[10];
   strncpy(ver, line + 5, 4); ver[4] = '\0';
   mVersion = atof(ver);

   // now read till we get End of Header
   
   while (!strstr(line, AGPSEphemeris::RINEX_END_OF_HEADER) )
   {
       if (!fgets(line, sizeof(line), pfh ) )
       {
          GPS_LOG_ERR("RINEX File I/O Error - [%d]. End of Header not found\n", -errno);
          return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
       }
   }
   
   // all ok!
   return GPSStatusCode::GPS_SUCCESS;
}

/* Internal Helper to parse single SV Entry */
GPSStatus AGPSEphemeris::ParseSVEntry
(
    FILE*         pfh,    // open rinex file
    gpsephemeris* peph    // pointer to ephemeris data structure
)
{
   char line[RINEX_MAX_LINE];
   char data[20];   // 19 + null
   GPSStatus stat = GPSStatusCode::GPS_SUCCESS;
   unsigned int cnt = 1;

   // blank data
   memset(peph, 0, sizeof(struct _gpsephemeris) );

   // this is first line, might be eof
   if (!fgets(line, sizeof(line), pfh ) )
   {
      if (feof(pfh) ) return GPSStatusCode::GPS_SUCCESS;

      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   /* PRN EPOCH               SV CLK
        9 14  3 12  1 59 44.0 3.117066808045D-04 2.842170943040D-12 0.000000000000D+00

      PRN:                   I2
      Epoch:
         year:               1X, I2.2  (2 digits, padded with 0 if necessary)
         month:              1X, I2
         day:                1X, I2
         hour:               1X, I2
         minute:             1X, I2
         second:             F5.1
      SV CLK:
         clock bias:         D19.12
         clock drift:        D19.12
         clock drift rate:   D19.12
   */


   memcpy(data, line, 2); data[2] = '\0';      // PRN
   stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, 1, data, peph);
   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   memcpy(data, line + 3, 2); data[2] = '\0';  // Epoch - Year
   stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, 2, data, peph);
   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   memcpy(data, line + 6, 2); data[2] = '\0';  // Epoch - Month
   stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, 3, data, peph);
   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   memcpy(data, line + 9, 2); data[2] = '\0';  // Epoch - Day
   stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, 4, data, peph);
   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   memcpy(data, line + 12, 2); data[2] = '\0'; // Epoch - Hour
   stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, 5, data, peph);
   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   memcpy(data, line + 15, 2); data[2] = '\0'; // Epoch - Minute
   stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, 6, data, peph);
   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   memcpy(data, line + 18, 4); data[4] = '\0';  // Epoch - Second
   stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, 7, data, peph);
   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   for (cnt = 1; cnt <= 3; cnt++)
   {
      memcpy(data, line+22 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_PRN, cnt + 7, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // Broadcast Orbit 1    3X, 4D 19.12
   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   for (cnt = 1; cnt <= 4; cnt++)
   {
      memcpy(data, line+3 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_ORBIT_1, cnt, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // Broadcast Orbit 2    3X, 4D 19.12
   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   for (cnt = 1; cnt <= 4; cnt++)
   {
      memcpy(data, line+3 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_ORBIT_2, cnt, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // Broadcast Orbit 3    3X, 4D 19.12
   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   for (cnt = 1; cnt <= 4; cnt++)
   {
      memcpy(data, line+3 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_ORBIT_3, cnt, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // Broadcast Orbit 4    3X, 4D 19.12
   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   for (cnt = 1; cnt <= 4; cnt++)
   {
      memcpy(data, line+3 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_ORBIT_4, cnt, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // Broadcast Orbit 5    3X, 4D 19.12
   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   for (cnt = 1; cnt <= 4; cnt++)
   {
      memcpy(data, line+3 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_ORBIT_5, cnt, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // Broadcast Orbit 6    3X, 4D 19.12
   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   for (cnt = 1; cnt <= 4; cnt++)
   {
      memcpy(data, line+3 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_ORBIT_6, cnt, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // Broadcast Orbit 7    3X, 2D 19.12   (2 spares)
   if (!fgets(line, sizeof(line), pfh ) )
   {
      GPS_LOG_ERR("RINEX File I/O Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   for (cnt = 1; cnt <= 2; cnt++)
   {
      memcpy(data, line+3 + (cnt - 1) * 19, 19); data[19] = '\0';
      stat = this->AssignValue(AGPSEphemeris::RECORD_ORBIT_7, cnt, data, peph);
      if (stat != GPSStatusCode::GPS_SUCCESS) return stat;
   }

   // all ok!
   return GPSStatusCode::GPS_SUCCESS;
}


/* RAW Parser */
GPSStatus AGPSEphemeris::ParseRAWFile   (const char* pszPath)
{
   GPS_LOG_INF("%s: Reading RAW Ephemeris File [%s]\n", "AGPSEphemeris::ParseRAWFile", pszPath);

   // reset internal state
   this->Reset();

   // open file and read straight into gpsrawpephemeris; if size doesn't match, this is an error
   int fd = ::open(pszPath, O_RDONLY);
   if (fd == -1) 
   {
      GPS_LOG_ERR("Could not open RAW Ephemeris File. Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   ssize_t size = ::read(fd, &mRawEphemeris, sizeof(struct _gpsrawephemeris) );
   if (size != sizeof(struct _gpsrawephemeris) )
   {
      GPS_LOG_ERR("Read Failure from RAW Ephemeris File [%s]. Read [%ld] bytes, expected [%d]. Errno: %d",
         pszPath, size, sizeof(struct _gpsrawephemeris), -errno);

      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   // all ok!
   GPS_LOG_INF("Finished. Successfully read RAW Ephemeris File [%s]\n", pszPath);
   ::close(fd);

   return GPSStatusCode::GPS_SUCCESS;
}

/*
  RINEX Parser. Initial implementation is quick and dirty, based on 2.10 format
                Going forward this might need to be readjusted */
GPSStatus AGPSEphemeris::ParseRINEXFile (const char* pszPath)
{
   GPS_LOG_INF("%s: Attempting to parse RINEX Ephemeris File [%s]\n", "AGPSEphemeris::ParseRINEXFile", pszPath);

   // reset internal state
   this->Reset();

   // open RINEX file
   FILE* pfh = fopen (pszPath, "r");
   if (pfh == 0) 
   {
      GPS_LOG_ERR("Could not open RINEX File. Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }


   // Parse header and Validate Version (current is 2.10)
   GPSStatus stat = this->ParseHeader(pfh);
   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
      fclose(pfh); return stat;
   }
   
   if (mVersion != AGPSEphemeris::RINEX_VERSION)
   {
      GPS_LOG_ERR("Invalid RINEX Version. Supported: [%f], detected: [%f]\n", AGPSEphemeris::RINEX_VERSION, mVersion);
      fclose(pfh); return GPSStatusCode::GPS_E_EPHEMER_FORMAT;
   }

   // We have valid RINEX Ephemeris File and supported version. Now simply loop and extract Data for each SV
   // SV entries can duplicate (more recent data coming in during capture time window), so we keep always latest one
   gpsephemeris entry;
   while (1)
   {
      stat = this->ParseSVEntry(pfh, &entry);
      if (stat != GPSStatusCode::GPS_SUCCESS)
      {
         fclose(pfh); return stat;
      }

      // test for eof, in which case we break of loop
      if (feof(pfh) ) break;

      GPS_LOG_TRACE("Successfully parsed SV entry. SVID=%d\n", entry.svid);

      // insert or replace
      gpsephemeris* pentry = 0;
      for (unsigned int i = 0; i < mSVs; i++)
      {
         if (mEphemeris[i].svid == entry.svid)  // duplicate
         {
            pentry = &(mEphemeris[i]);
            break;
         }
      }

      if (pentry) memcpy(pentry, &entry, sizeof(struct _gpsephemeris) );
      else
      {
         memcpy(&(mEphemeris[mSVs]), &entry, sizeof(struct _gpsephemeris) );
         mSVs++;
      }
   }

   // close RINEX file and we are done!
   fclose (pfh); 

   // log what we parsed!
   GPS_LOG_INF("Finished. Successfully parsed  RINEX Ephemeris File [%s]\n", pszPath);
   //this->log();

   return GPSStatusCode::GPS_SUCCESS;


}

/* assignment of data value

   Note: This routine should be made robust to check data validity as per
         ICD-GPS-200  http://www.navcen.uscg.gov/?pageName=gpsICD200c

*/
GPSStatus AGPSEphemeris::AssignValue 
(
   unsigned int  record_id,        // record id
   unsigned int  field_offset,     // field offset
   const char*   data,             // null-terminated string data
   gpsephemeris* peph              // ephemeris data structure
)
{
   if ( (!data) || (*data == '\0') ) return GPSStatusCode::GPS_E_EPHEMER_DATA;
   GPSStatus stat = GPSStatusCode::GPS_SUCCESS;

   // RECORD_PRN
   if (record_id == AGPSEphemeris::RECORD_PRN)
   {
       switch (field_offset)
       {
          case 1:  // PRN
          {
             int val = atoi(data); 
             if ( (val < AGPSAlmanac::MIN_SVID) || (val > AGPSAlmanac::MAX_SVID) ) 
             {
                GPS_LOG_ERR("Space Vehicle PRN out of Range (%s)\n", data);
                stat = GPSStatusCode::GPS_E_EPHEMER_DATA;
             }
             else
                peph->svid = static_cast<unsigned char>(val);
          }
          break;


          /* Currently not validating Epoch Timestamp; for completion should probably be compared
             to systemtime and make sure it fits within certain range */
          case 2:  // Epoch - Year
          {
             peph->epoch_year = atoi(data);
          }
          break;
      
          case 3: // Epoch - Month
          {
             peph->epoch_month = atoi(data);
          }
          break;

          case 4: // Epoch - Day
          {
             peph->epoch_day = atoi(data);
          }
          break;

          case 5: // Epoch - Hour
          {
             peph->epoch_hour = atoi(data);
          }
          break;

          case 6: // Epoch - Minute
          {
             peph->epoch_minute = atoi(data);
          }
          break;

          case 7: // Epoch - Second
          {
             this->handleD(data, &(peph->epoch_second) );
          }
          break;

          case 8: // SV clock bias
          {
              this->handleD(data, &(peph->cbias) );
          }
          break;

          case 9: // SV clock drift
          {
              this->handleD(data, &(peph->cdrift) );
          }
          break;

          case 10: // SV clock drift rate
          {
              this->handleD(data, &(peph->cdrift_rate) );
          }
          break;   

          default:
            GPS_LOG_ERR("Internal Error - invalid field ID %d for PRN Record\n", field_offset);
            stat = GPSStatusCode::GPS_E_INTERNAL;

            break;
       }
       return stat;

   }   // RECORD_PRN

   // RECORD_ORBIT_1
   if (record_id == AGPSEphemeris::RECORD_ORBIT_1)
   { 
       switch (field_offset)
       {
         case 1:  // IODE Issue of Data
          {
             this->handleD(data, &(peph->iode) );
          }
          break;
      
          case 2: // Amplitude of the Sine Harmonic Correction Term to the Orbit Radius
          {
             this->handleD(data, &(peph->crs) );
          }
          break;

          case 3: // Mean Motion Difference from Computed Value 
          {
             // convert to semi-circles/sec
             this->handleD(data, &(peph->deltan) );
             peph->deltan /= AGPSAlmanac::PI_VALUE;
          }
          break;

          case 4: // Mean Anomaly at Reference Time  
          {
             // convert to semi-circles
             this->handleD(data, &(peph->m0) );
             peph->m0 /= AGPSAlmanac::PI_VALUE;
          }
          break;

          default:
            GPS_LOG_ERR("Internal Error - invalid field ID %d for Record Orbit 1\n", field_offset);
            stat = GPSStatusCode::GPS_E_INTERNAL;

            break;
       }
       return stat;

   }  // RECORD_ORBIT_1

   // RECORD_ORBIT_2
   if (record_id == AGPSEphemeris::RECORD_ORBIT_2)
   {   
      switch (field_offset)
      {
          case 1:  // Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude
          {
             this->handleD(data, &(peph->cuc) );
          }
          break;
      
          case 2: // Eccentricity
          {
             this->handleD(data, &(peph->ecc) );
             if (peph->ecc > AGPSAlmanac::MAX_ECC)  // ICD-200
             {
               GPS_LOG_ERR("Record Orbit 2: Space Vehicle [%d] Eccentricity [%f] out of Range\n", peph->svid, peph->ecc);
               stat = GPSStatusCode::GPS_E_EPHEMER_DATA;
             }
          }
          break;

          case 3: // Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude 
          {
             this->handleD(data, &(peph->cus) );
          }
          break;

          case 4: // Square Root of the Semi-Major Axis
          {
             this->handleD(data, &(peph->sqrta) );
          }
          break;

          default:
            stat = GPSStatusCode::GPS_E_INTERNAL;
            GPS_LOG_ERR("Internal Error - invalid field ID %d for Record Orbit 2\n", field_offset);

            break;
      }
      return stat;

   }  // RECORD_ORBIT_2

   // RECORD_ORBIT_3
   if (record_id == AGPSEphemeris::RECORD_ORBIT_3)
   {
      switch (field_offset)
      {
          case 1:  // Reference Time Ephemeris 
          {
             this->handleD(data, &(peph->toe) );
             if ((peph->toe < AGPSEphemeris::MIN_TOE) || (peph->toe > AGPSEphemeris::MAX_TOE) )
             {
               GPS_LOG_ERR("Record Orbit 3 - Reference Time Ephemeris out of Range (%s)\n", data);
               stat = GPSStatusCode::GPS_E_EPHEMER_DATA;
             }
          }
          break;
      
          case 2: // Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination
          {
             this->handleD(data, &(peph->cic) );
          }
          break;

          case 3: //  Longitude of Ascending Node of Orbit Plane at Weekly Epoch
          {
             // convert to semi-circles
             this->handleD(data, &(peph->OMEGA) );
             peph->OMEGA /= AGPSAlmanac::PI_VALUE;
          }
          break;

          case 4: // Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination  
          {
             this->handleD(data, &(peph->cis) );
          }
          break;

          default:
            stat = GPSStatusCode::GPS_E_INTERNAL;
            GPS_LOG_ERR("Internal Error - invalid field ID %d for Record Orbit 3\n", field_offset);

            break;
      }
      return stat;

   }  // RECORD_ORBIT_3

   // RECORD_ORBIT_4
   if (record_id == AGPSEphemeris::RECORD_ORBIT_4)
   {
      switch (field_offset)
      {
          case 1:  // Inclination Angle at Reference Time  
          {
             // convert to semi-circles
             this->handleD(data, &(peph->i0) );
             peph->i0 /= AGPSAlmanac::PI_VALUE;
          }
          break;
      
          case 2: //  Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius
          {
             this->handleD(data, &(peph->crc) );
          }
          break;

          case 3: //  Argument of Perigee       
          {
             // convert to semi-circles
             this->handleD(data, &(peph->w) );
             peph->w /= AGPSAlmanac::PI_VALUE;
          }
          break;

          case 4: //  Rate of Right Ascension 
          {
             // convert to semi-circles/sec
             this->handleD(data, &(peph->omega_dot) );
             peph->omega_dot /= AGPSAlmanac::PI_VALUE;
          }
          break;

          default:
            stat = GPSStatusCode::GPS_E_INTERNAL;
            GPS_LOG_ERR("Internal Error - invalid field ID %d for Record Orbit 4\n", field_offset);

            break;
      }
      return stat;
   }  // RECORD_ORBIT_4

   // RECORD_ORBIT_5
   if (record_id == AGPSEphemeris::RECORD_ORBIT_5)
   {
      switch (field_offset)
      {
          case 1: // Rate of Inclination Angle
          {
             // convert to semi-circles/sec
             this->handleD(data, &(peph->idot) );
             peph->idot /= AGPSAlmanac::PI_VALUE;
          }
          break;
      
          case 2: // Codes on L2 channel
          {
             this->handleD(data, &(peph->l2code) );
          }
          break;

          case 3: // GPS Week number (to go with toe)        
          {
             this->handleD(data, &(peph->gpsweek) );
          }
          break;

          case 4: // L2 p Data Flag 
          {
             this->handleD(data, &(peph->l2p) );
          }
          break;

          default:
            stat = GPSStatusCode::GPS_E_INTERNAL;
            GPS_LOG_ERR("Internal Error - invalid field ID %d for Record Orbit 5\n", field_offset);

            break;
        }
        return stat;

   }  // RECORD_ORBIT_5

   // RECORD_ORBIT_6
   if (record_id == AGPSEphemeris::RECORD_ORBIT_6)
   {
      switch (field_offset)
      {
          case 1: // SV accuracy
          {
              // won't worry about negative, as eventual index conversion will treat it as inacurate if < 0
              this->handleD(data, &(peph->accuracy) );
          }
          break;
      
          case 2: // SV health        (bits 17-22 w 3 sf 1)
          {
              this->handleD(data, &(peph->health) );
          }
          break;

          case 3: // TGD: Estimate Group Delay Differential        
          {
              this->handleD(data, &(peph->tgd) );
          }
          break;

          case 4: // IODC Issue of Data, Clock
          {
              this->handleD(data, &(peph->iodc) );
          }
          break;

          default:
            stat = GPSStatusCode::GPS_E_INTERNAL;
            GPS_LOG_ERR("Internal Error - invalid field ID %d for Record Orbit 6\n", field_offset);

            break;
        }
        return stat;

   }  // RECORD_ORBIT_6

   // RECORD_ORBIT_7
   if (record_id == AGPSEphemeris::RECORD_ORBIT_7)
   {
     switch (field_offset)
      {
          case 1: // Transmission time of message sec of GPS week, derived from Z-count in Hand Over Word (HOW)
          {
             this->handleD(data, &(peph->toc) );
             if ((peph->toc < AGPSEphemeris::MIN_TOE) || (peph->toc > AGPSEphemeris::MAX_TOE) )
             {
               GPS_LOG_ERR("Transmission Time of Message out of Range (%s)\n", data);
               stat = GPSStatusCode::GPS_E_EPHEMER_DATA;
             }
          }
          break;
      
          case 2: // Fit Interval: indicates whether the ephemerides are based on a four-hour fit interval
          {
             this->handleD(data, &(peph->fiti) );
          }
          break;

          default:
            stat = GPSStatusCode::GPS_E_INTERNAL;
            GPS_LOG_ERR("Internal Error - invalid field ID %d for Record Orbit 7\n", field_offset);

            break;
        }
        return stat;
   }  // RECORD_ORBIT_7

    
   // If here, this is internal error!
   GPS_LOG_ERR("Internal Error - invalid Record ID %d\n", record_id);
   return GPSStatusCode::GPS_E_INTERNAL;
}

// logging
void  AGPSEphemeris::log ()
{
   GPS_LOG_INF("GPS Ephemeris. Number of Space Vehicles: [%d]\n", mSVs);
   GPS_LOG_INF("+++++++++++++++++++++++++++++++++++++++++++++\n");

   for (unsigned int i = 0; i < mSVs; i++)
   {
       GPS_LOG_INF("\n");
       GPS_LOG_INF("Space Vehicle: [%d]\n", i+1);
       GPS_LOG_INF("-------------------------------\n");
       this->logEntry (&(mEphemeris[i]) );

       GPS_LOG_INF("\n");
   }
}

// Logs single Ephemeris Entry, as per parsed RINEX format
void  AGPSEphemeris::logEntry (gpsephemeris* peph)
{
   // PRN / EPOCH / CLOCK
   GPS_LOG_INF("PRN: %d\n", peph->svid);
   GPS_LOG_INF("Epoch: YY [%02d] MM [%02d] DD [%02d] HH [%02d] mm [%02d] sec [%0.10E] \n",
       peph->epoch_year, peph->epoch_month, peph->epoch_day, peph->epoch_hour, peph->epoch_minute, peph->epoch_second);

   GPS_LOG_INF("SV Clock Bias    (sec): %0.10E\n", peph->cbias);
   GPS_LOG_INF("SV Clock Drift   (sec/sec): %0.10E\n", peph->cdrift);
   GPS_LOG_INF("Clock Drift Rate (sec/sec2): %0.10E\n", peph->cdrift_rate);

   // Broadcast Orbit - 1
   GPS_LOG_INF("\nBroadcast Orbit - 1\n");
   GPS_LOG_INF("IODE Issue of Data, Ephemeris: %0.10E\n", peph->iode);
   GPS_LOG_INF("Amplitude of the Sine Harmonic Correction Term to the Orbit Radius: %0.10E [m]\n", peph->crs);
   GPS_LOG_INF("Mean Motion Difference from Computed Value: %0.10E [rad/sec]\n", peph->deltan);
   GPS_LOG_INF("Mean Anomaly at Reference Time: %0.10E [rad]\n", peph->m0);
 
   // Broadcast Orbit - 2
   GPS_LOG_INF("\nBroadcast Orbit - 2\n");
   GPS_LOG_INF("Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude: %0.10E [rad]\n", peph->cuc);
   GPS_LOG_INF("Eccentricity: %0.10E\n", peph->ecc);
   GPS_LOG_INF("Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude: %0.10E [rad]\n", peph->cus);
   GPS_LOG_INF("Square Root of Semi-Major Axis: %0.10E [m^(1/2)]\n", peph->sqrta);

   // Broadcast Orbit - 3
   GPS_LOG_INF("\nBroadcast Orbit - 3\n");
   GPS_LOG_INF("Reference Time Ephemeris: %0.10E [sec of GPS week]\n", peph->toe);
   GPS_LOG_INF("Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination: %0.10E [rad]\n", peph->cic);
   GPS_LOG_INF("Longitude of Ascending Node of Orbit Plane at Weekly Epoch: %0.10E [rad]\n", peph->OMEGA);
   GPS_LOG_INF("Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination: %0.10E [rad]\n", peph->cis);

   // Broadcast Orbit - 4
   GPS_LOG_INF("\nBroadcast Orbit - 4\n");
   GPS_LOG_INF("Inclination Angle at Reference Time: %0.10E [rad]\n", peph->i0);
   GPS_LOG_INF("Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius: %0.10E [rad]\n", peph->crc);
   GPS_LOG_INF("Argument of Perigee: %0.10E [rad]\n", peph->w);
   GPS_LOG_INF("Rate of Right Ascension: %0.10E [rad/sec]\n", peph->omega_dot);

   // Broadcast Orbit - 5
   GPS_LOG_INF("\nBroadcast Orbit - 5\n");
   GPS_LOG_INF("Rate of Inclination Angle: %0.10E [rad/sec]\n", peph->idot);
   GPS_LOG_INF("Codes on L2 channel: %0.10E\n", peph->l2code);
   GPS_LOG_INF("GPS Week number (to go with toe): %0.10E\n", peph->gpsweek);
   GPS_LOG_INF("L2 p Data Flag: %0.10E\n", peph->l2p);                                      

   // Broadcast Orbit - 6
   GPS_LOG_INF("\nBroadcast Orbit - 6\n");
   GPS_LOG_INF("SV accuracy: %0.10E [m]\n", peph->accuracy);
   GPS_LOG_INF("SV health (bits 17-22 w 3 sf 1): %0.10E\n", peph->health);
   GPS_LOG_INF("TGD: Estimate Group Delay Differential: %0.10E\n", peph->tgd);
   GPS_LOG_INF("IODC Issue of Data, Clock: %0.10E\n", peph->iodc);                       

   // Broadcast Orbit - 7
   GPS_LOG_INF("\nBroadcast Orbit - 7\n");
   GPS_LOG_INF("Transmission time of message sec of GPS week: %0.10E [sec]\n", peph->toc);
   GPS_LOG_INF("Fit Interval: %0.10E [hours]\n", peph->fiti);

}



