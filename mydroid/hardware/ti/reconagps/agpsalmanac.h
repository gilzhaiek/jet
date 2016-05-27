/***************************************************************************
 *
 *  Native Parser of serialized Almanac Data. We plan to support both
 *  Yuma and Sem formats. Also knows how to perform various conversions
 *  based on ICD-200 specifications
 * 
 *  For more details on Almanac Data Format please see: http://www.navcen.uscg.gov/?pageName=gpsAlmanacs
 *  For ICD-200 spec please see: http://geoweb.mit.edu/~tah/icd200c123.pdf
 *  
 *
 ***************************************************************************/

#ifndef __agpsalmanac_h__
#define __agpsalmanac_h__

#include "recongps.h"

// Abstract base
class AGPSAlmanac
{
   protected:
      gpsalmanac   mAlmanac[GPS_MAX_SVS];
      unsigned int mSVs;    // how many were extracted

     // internal helpers
     void Reset ();
     GPSStatus AssignValue  (unsigned int id, const char* data, gpsalmanac* palm);

     // field offsets
     static const unsigned int OFFSET_SVID;
     static const unsigned int OFFSET_HEALTH;
     static const unsigned int OFFSET_WEEK;
     static const unsigned int OFFSET_TOA;
     static const unsigned int OFFSET_ECC;
     static const unsigned int OFFSET_I0;
     static const unsigned int OFFSET_OMEGADOT;
     static const unsigned int OFFSET_SQRTA;
     static const unsigned int OFFSET_OMEGA0;
     static const unsigned int OFFSET_W;
     static const unsigned int OFFSET_M0;
     static const unsigned int OFFSET_AF0;
     static const unsigned int OFFSET_AF1;

   public:
     // data validity range, per ICD-GPS-200
     static const unsigned char  MIN_SVID;
     static const unsigned char  MAX_SVID;

     static const unsigned char  MIN_HEALTH;
     static const unsigned char  MAX_HEALTH;

     static const unsigned short MIN_WEEK;
     static const unsigned short MAX_WEEK;

     static const unsigned int   MIN_TOA;
     static const unsigned int   MAX_TOA;

     static const double         MIN_ECC;
     static const double         MAX_ECC;

     static const double         MIN_I0;
     static const double         MAX_I0;

     static const double         MIN_OMEGADOT;
     static const double         MAX_OMEGADOT;

     static const double         MIN_SQRTA;
     static const double         MAX_SQRTA;

     static const double         MIN_OMEGA0;
     static const double         MAX_OMEGA0;

     static const double         MIN_W;
     static const double         MAX_W;

     static const double         MIN_M0;
     static const double         MAX_M0;

     static const double         MIN_AF0;
     static const double         MAX_AF0;

     static const double         MIN_AF1;
     static const double         MAX_AF1;

     static const double         PI_VALUE;
            
   
   public:

     // construction-destruction
     static AGPSAlmanac* Factory (int format);

     AGPSAlmanac ();
     virtual ~AGPSAlmanac ();
 
     // pure virtual parse: Derived classes implement their format
     virtual GPSStatus ParseAlmanacFile (const char* pszPath) = 0; 

     // accessors
     unsigned int getNumSVs () {return mSVs;}
     gpsalmanac*  getAlmanac (unsigned int iOffset);

     // logging
     void  log ();
     void  logEntry (gpsalmanac*);
                                        

};

// Concrete Implementation of YUMA format
class YUMAlmanac : public AGPSAlmanac
{
   protected:
     static const char* TAG_ID;
     static const char* TAG_HEALTH;
     static const char* TAG_ECCENTRICITY;
     static const char* TAG_TOA;
     static const char* TAG_ORBITAL_INCL;
     static const char* TAG_RATE_RIGHT_ASC;
     static const char* TAG_SQRT;
     static const char* TAG_RIGHT_ASC_WEEK;
     static const char* TAG_ARG_OF_PERIGEE;
     static const char* TAG_MEAN_ANOM;
     static const char* TAG_AF0;
     static const char* TAG_AF1; 
     static const char* TAG_WEEK;

     static const unsigned int YUMA_MAX_LINE;     // max single line length in Yuma Almanac 
     static const unsigned int YUMA_NAME_SIZE;    // max length (chars) of Yuma Name 
     static const unsigned int YUMA_VALUE_SIZE;   // max length (chars) of Yuma Value

     static const char* SV_LINE_START;            // string indicator of Yuma header ("******** Week xxx almanac...")
     static const char  SEP_CHAR;                 // name/value separator

     static const double ORBITAL_55_RADIANS;      // 55 degrees angle in radians

   public:
      GPSStatus ParseAlmanacFile (const char* pszPath);

   protected:  // internal helpers
     unsigned int isSVHeader(const char*);
     GPSStatus ParseSVEntry (FILE*, const char*);

     GPSStatus HandleProperty  (const char* header, unsigned int id, const char* tag, FILE* pfh, gpsalmanac* palm);
     void      Tokenize(const char* line, char* name, char* value);
     
};

/*
class SEMAlmanac : public AGPSAlmanac
{
   public:
      GPSStatusCode ParseAlmanacFile (const char* pszPath);
}; 

*/


/* ICD200 conversion macros

NOTES: 

      1) Angular properties are expected in semi-circles; it is the duty of specific
         Almanac Parser implementation to perform this conversions from radians, if required

      2) Currently it is not clear whether two's complement properties smaller in size
         than allocated 16 or 32 bit signed int (i.e. omegadot, af1) need to have sign bit adjusted. If so,
         macros bellow will need to be adjusted (or moved into separate functions if more practical)



Parameter      # of bits      Scale Factor (LSB)      Units             Remark
---------------------------------------------------------------------------------------------------
toa            8              2 exp (+12)             seconds

ecc            16             2 exp (-21)             dimensionless     Max = 602112

i0             16             2 exp (-19)             semi-circles      Relative to i0 = 0.30 semi-circles
                                                                        Two's complement, sign in MSB

omegadot       16             2 exp (-38)             semi-circles/sec  Two's complement, sign in MSB

sqrta          24             2 exp (-11)             meters exp (1/2)

omega0         24             2 exp (-23)             semi-circles      Two's complement, sign in MSB

w              24             2 exp (-23)             semi-circles      Two's complement, sign in MSB

m0             24             2 exp (-23)             semi-circles      Two's complement, sign in MSB

af0            11             2 exp (-20)             seconds           Two's complement, sign in MSB

af             11             2 exp (-38)             sec/sec           Two's complement, sign in MSB

*/

#define ALM_TOA_2_ICD(value)           static_cast<unsigned char>((value / exp2(12) ) )
#define ALM_ECC_2_ICD(value)           static_cast<unsigned short>((value / exp2(-21) ) )
#define ALM_I0_2_ICD(value)            static_cast<signed short>((value / exp2(-19) ) )
#define ALM_OMEGADOT_2_ICD(value)      static_cast<signed short>((value / exp2(-38) ) )
#define ALM_SQRTA_2_ICD(value)         static_cast<unsigned int>((value / exp2(-11) ) )
#define ALM_OMEGA0_2_ICD(value)        static_cast<signed int>((value / exp2(-23) ) )
#define ALM_W_2_ICD(value)             static_cast<signed int>((value / exp2(-23) ) )
#define ALM_M0_2_ICD(value)            static_cast<signed int>((value / exp2(-23) ) )
#define ALM_AF0_2_ICD(value)           static_cast<signed short>((value / exp2(-20) ) )
#define ALM_AF1_2_ICD(value)           static_cast<signed short>((value / exp2(-38) ) )



#endif    // __agpsalmanac_h__
