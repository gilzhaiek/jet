/*
 *******************************************************************************
 *         
 *
 * FileName         :   YUMAlmanac.cpp
 *
 * Description      :   Parser of GPS Almanac Data in Yuma Format
 *
 * Author           :   RECON Instruments
 * Date             :   December 2013
 *
 * Comments         :   Concrete Implementation of Yuma format
 *                      Yuma format is plain ASCII text file, where each Space Vehicle
 *                      entry is defined with following signature:
 *
   ******** Week 748 almanac for PRN-01 ********
   ID:                         01
   Health:                     000
   Eccentricity:               0.2617359161E-002
   Time of Applicability(s):  405504.0000
   Orbital Inclination(rad):   0.9603342912
   Rate of Right Ascen(r/s):  -0.7908900866E-008
   SQRT(A)  (m 1/2):           5153.628418
   Right Ascen at Week(rad):   0.1059226467E+001
   Argument of Perigee(rad):   0.424856502
   Mean Anom(rad):            -0.2023449864E+001
   Af0(s):                     0.9632110596E-004
   Af1(s/s):                   0.3637978807E-011
   week:                        748
    *                          
    ******************************************************************************
 */

#define LOG_TAG "RECON.AGPS"    // our identification for logcat (adb logcat RECON.AGPS:V *:S)

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#include "agpsalmanac.h"

// constant initialization
const char* YUMAlmanac::TAG_ID                                 = "ID";
const char* YUMAlmanac::TAG_HEALTH                             = "Health";
const char* YUMAlmanac::TAG_ECCENTRICITY                       = "Eccentricity";
const char* YUMAlmanac::TAG_TOA                                = "Time of Applicability(s)";
const char* YUMAlmanac::TAG_ORBITAL_INCL                       = "Orbital Inclination(rad)";
const char* YUMAlmanac::TAG_RATE_RIGHT_ASC                     = "Rate of Right Ascen(r/s)";
const char* YUMAlmanac::TAG_SQRT                               = "SQRT(A)  (m 1/2)";
const char* YUMAlmanac::TAG_RIGHT_ASC_WEEK                     = "Right Ascen at Week(rad)";
const char* YUMAlmanac::TAG_ARG_OF_PERIGEE                     = "Argument of Perigee(rad)";
const char* YUMAlmanac::TAG_MEAN_ANOM                          = "Mean Anom(rad)";
const char* YUMAlmanac::TAG_AF0                                = "Af0(s)";
const char* YUMAlmanac::TAG_AF1                                = "Af1(s/s)";
const char* YUMAlmanac::TAG_WEEK                               = "week";

const unsigned int YUMAlmanac::YUMA_MAX_LINE                          = 100;  
const unsigned int YUMAlmanac::YUMA_NAME_SIZE                         = 50;
const unsigned int YUMAlmanac::YUMA_VALUE_SIZE                        = 50;

const char* YUMAlmanac::SV_LINE_START                          = "******** Week";
const char  YUMAlmanac::SEP_CHAR                               = ':';

const double YUMAlmanac::ORBITAL_55_RADIANS                    = 0.95993028;   // 55/180 * 3.14159

GPSStatus YUMAlmanac::ParseAlmanacFile (const char* pszPath)
{
   GPS_LOG_INF("Enter: Attempting to parse GPS Almanac [%s]\n", pszPath);

   // reset internal state
   this->Reset();

   // open almanac file
   FILE* pfh = fopen (pszPath, "r");
   if (pfh == 0) 
   {
      GPS_LOG_ERR("Could not open Almanac File. Error [%d]\n", -errno);
      return GPSStatusCode::GPS_E_ALMANAC_FORMAT;
   }

   // keep reading till we identify start of SV entry (or EOF). 
   // we must also handle unlikely case of corrupted file, with duplicate entries, more entries than GPS_MAX_SVS etc
   char line[YUMA_MAX_LINE];

   while ( fgets(line, sizeof(line), pfh ) )
   {
       // remove trailing newline character fgets gives us
       line[strlen(line) - 1] = '\0';

       // trim line of spaces, etc
       char* start = GPSUtilities::TrimLine (line);

       if (*start != '\0')   // skip empty lines
       {
          // determine if start of SV entry
          if (this->isSVHeader (start) )
          {
             GPS_LOG_TRACE("Found SV Start: [%s]. Parsing almanac entry... \n", start);
             GPSStatus stat = this->ParseSVEntry (pfh, start);

             if (stat != GPSStatusCode::GPS_SUCCESS)
             {
                 fclose(pfh);
                 this->Reset ();

                 // routine will log what was wrong; we just quit
                 return stat;
             }
          }
          else
          {
              // still won't treat as corrupted, but log - this is unexpected
              GPS_LOG_WARN("Ignoring unexpected line [%s] in Yuma Almanac File\n", start);
          }
       }
   }

   // close almanac file and we are done!
   fclose (pfh); 

   // log what we parsed!
   GPS_LOG_INF("Finished. Successfully parsed  GPS Almanac [%s]\n", pszPath);
   this->log();

   return GPSStatusCode::GPS_SUCCESS;
}

// Determines if passed line is SV header line. 1 - yes, 0 no
unsigned int YUMAlmanac::isSVHeader (const char* line)
{
   // we simply look for SV_LINE_START prefix; could get more
   // sophisticated if required
   if (strncmp(line, YUMAlmanac::SV_LINE_START, strlen(YUMAlmanac::SV_LINE_START) ) == 0)
       return 1;

   return 0;
}

/* Internal helper that extracts single SV entry. File pointer is past header line
   returns GPSStatusCode::GPS_SUCCESS, or error code

   Here we handle various error conditions, such as:
       -- entries not in expected sequential order
       -- not all entries found (next SV header before all properties were extracted)
       -- feof before all properties were extracted
       -- duplicate SV
       -- more SV entries than GPS_MAX_SVS
       -- corrupted/invalid data (Handled by range values, see AGPSAlmanac.cpp)
*/

GPSStatus YUMAlmanac::ParseSVEntry (FILE* pfh, const char* header)
{
    // first check if we have reached GPS_MAX_SVS
    if (mSVs >= GPS_MAX_SVS)
    {
       GPS_LOG_ERR("Invalid YUMA Almanac - more entries than [%d]\n", GPS_MAX_SVS);
       return GPSStatusCode::GPS_E_ALMANAC_FORMAT;
    }

    // ID : additional check unique SV ID
    GPSStatus stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_SVID, YUMAlmanac::TAG_ID, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    for (unsigned int i = 0; i < mSVs; i++)
    {
       if (mAlmanac[i].svid == mAlmanac[mSVs].svid)
       {
           GPS_LOG_ERR("Invalid YUMA Almanac - duplicate SVID [%d]\n", mAlmanac[mSVs].svid);
           return GPSStatusCode::GPS_E_ALMANAC_DATA;
       }
    }

    // Health 
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_HEALTH, YUMAlmanac::TAG_HEALTH, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Eccentricity
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_ECC, YUMAlmanac::TAG_ECCENTRICITY, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Time of Applicability
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_TOA, YUMAlmanac::TAG_TOA, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Orbital Inclination
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_I0, YUMAlmanac::TAG_ORBITAL_INCL, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Rate of Right Ascen
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_OMEGADOT, YUMAlmanac::TAG_RATE_RIGHT_ASC, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // SQRT(A)
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_SQRTA, YUMAlmanac::TAG_SQRT, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Right Ascen at Week
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_OMEGA0, YUMAlmanac::TAG_RIGHT_ASC_WEEK, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Argument of Perigee
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_W, YUMAlmanac::TAG_ARG_OF_PERIGEE, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Mean Anom
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_M0, YUMAlmanac::TAG_MEAN_ANOM, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Af0
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_AF0, YUMAlmanac::TAG_AF0, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // Af1
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_AF1, YUMAlmanac::TAG_AF1, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

    // week
    stat = this->HandleProperty (header, AGPSAlmanac::OFFSET_WEEK, YUMAlmanac::TAG_WEEK, pfh, &(mAlmanac[mSVs]) );
    if (stat != GPSStatusCode::GPS_SUCCESS) return stat;


    // all ok here!
    mSVs++;
    return GPSStatusCode::GPS_SUCCESS;
}

// Internal helper that handles extraction and assignment of single almanac data value
GPSStatus YUMAlmanac::HandleProperty 
(
   const char*  header,    // header line this value belongs to
   unsigned int id,        // property identifier, regardless of format
   const char*  tag,       // tag we are reading
   FILE*        pfh,       // open file pointer
   gpsalmanac*  palm       // [out]: almanac entry
)
{
    char line[YUMA_MAX_LINE];
    GPSStatus stat = GPSStatusCode::GPS_SUCCESS;
    int parsed = 0;

    while (!parsed)
    {
       if (!fgets(line, sizeof(line), pfh ) )
       {
          GPS_LOG_ERR("Invalid YUMA Almanac - [%s] Tag not found for header [%s]\n", tag, header);
          return GPSStatusCode::GPS_E_ALMANAC_FORMAT;
       }

       line[strlen(line) - 1] = '\0';
       char* start = GPSUtilities::TrimLine (line);

       if (*start != '\0')
       {
           parsed = 1;   // loop exit

           char namebuf  [YUMA_NAME_SIZE];  
           char valuebuf [YUMA_VALUE_SIZE];  

           this->Tokenize(line, namebuf, valuebuf);

           char* name  = GPSUtilities::TrimLine(namebuf); 
           char* value = GPSUtilities::TrimLine(valuebuf);

           if (strcmp(name, tag) != 0)   // tag must match
           {
               GPS_LOG_ERR("Invalid YUMA Almanac. Was expecting TAG [%s], found [%s]. Header: [%s], Line: [%s]\n", tag, name, header, line);
               stat = GPSStatusCode::GPS_E_ALMANAC_FORMAT;
           }
           else
           {
              /* For angular properties we need to convert into semi-circles */
              if ( (id == AGPSAlmanac::OFFSET_OMEGADOT) ||  // Rate of Right Ascen(r/s)
                   (id == AGPSAlmanac::OFFSET_OMEGA0) ||    // Right Ascen at Week(rad)
                   (id == AGPSAlmanac::OFFSET_W) ||         // Argument of Perigee(rad)
                   (id == AGPSAlmanac::OFFSET_M0) )         // Mean Anom(rad)
              {
                  sprintf(value, "%0.9E", atof(value)/AGPSAlmanac::PI_VALUE);
              }

              if (id == AGPSAlmanac::OFFSET_I0)         // Orbital Inclination(rad). Must first calc angle with 55 degree orbit
              {
                 sprintf(value, "%0.9E", (atof(value) - YUMAlmanac::ORBITAL_55_RADIANS) / AGPSAlmanac::PI_VALUE);  
              }

              stat = this->AssignValue (id, value, palm);
              if (stat != GPSStatusCode::GPS_SUCCESS)
              {
                 GPS_LOG_ERR("Invalid YUMA Almanac - Data appears to be corrupted. Header: [%s], Line: [%s]\n", header, line);
              }
           }
       }
    }

    return stat;
}

// Internal helper that Tokenizes passed line into name, value pair
void YUMAlmanac::Tokenize(const char* line, char* name, char* value)
{
   // blank results first. Note -- line has been comment stripped and trimmed
   // and is guaranteed to be non-zero length
   name[0] = value[0] = '\0';
   const char* delimiter = line;

   // look for SEP_CHAR
   while (*delimiter)
   {
      if ( (*delimiter) == YUMAlmanac::SEP_CHAR)
          break;

      delimiter++;
   }
  
   if (*delimiter)
   {
      strncpy(name, line, delimiter - line);
      name[delimiter-line] = '\0';

      strcpy(value, delimiter+1);
   }
}







