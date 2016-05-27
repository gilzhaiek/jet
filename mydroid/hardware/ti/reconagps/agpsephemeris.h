/***************************************************************************
 *
 *  Native Parser of Ephemeris Data serialized in RINEX format
 * 
 *  For more details on Rinex Data Format please see: http://igscb.jpl.nasa.gov/igscb/data/format/rinex210.txt  (Table A-4)
 *  For ICD-200 spec please see: http://geoweb.mit.edu/~tah/icd200c123.pdf
 *  
 *
 ***************************************************************************/

#ifndef __agpsephemeris_h__
#define __agpsephemeris_h__

#include "recongps.h"

class AGPSEphemeris
{
   // Internal constants
   protected:
      static const unsigned int RINEX_MAX_LINE;      // max single line length in RINEX File 
      static const double       RINEX_VERSION;       // currently supported RINEX version

      static const char*        RINEX_VERSION_TYPE;  // Rinex Header
      static const char*        RINEX_FILE_TYPE;
      static const char*        RINEX_END_OF_HEADER;

   // Record offsets
   protected:
      static const unsigned int RECORD_PRN;
      static const unsigned int RECORD_ORBIT_1;
      static const unsigned int RECORD_ORBIT_2;
      static const unsigned int RECORD_ORBIT_3;
      static const unsigned int RECORD_ORBIT_4;
      static const unsigned int RECORD_ORBIT_5;
      static const unsigned int RECORD_ORBIT_6;
      static const unsigned int RECORD_ORBIT_7;
  
   // Data Ranges
   protected:
      static const double MIN_TOE;
      static const double MAX_TOE;

   // Data members
   protected:
      gpsrawephemeris mRawEphemeris;   // raw ephemeris; this is bit of hack & inconsistent (i.e. doesn't belong here
                                       // so might adress down the road for OO purity

      gpsephemeris    mEphemeris[GPS_MAX_SVS];
      unsigned int    mSVs;      // how many SVs were extracted
      double          mVersion;  // Rinex Version

   // Internal helpers
   protected:
     void Reset ();
     GPSStatus AssignValue  (unsigned int record_id, unsigned int field_offset, const char* data, gpsephemeris* peph);
  
     GPSStatus ParseHeader(FILE* pfh);
     GPSStatus ParseSVEntry(FILE* pfh, gpsephemeris* peph);

     void      handleD(const char* data, double* val);

   // Construction - Destruction
   public:
     AGPSEphemeris ();
     ~AGPSEphemeris ();
 

   // Public Interface
   public:

     // accessors
     unsigned int   getNumSVs () {return mSVs;}
     gpsephemeris*  getEphemeris (unsigned int iOffset);

     const gpsrawephemeris* getrawephemeris(){return &mRawEphemeris;}

     // logging
     void  log ();
     void  logEntry (gpsephemeris*);

     // Parse of passed RINEX Ephemeris File
     GPSStatus ParseRINEXFile (const char* pszPath); 

     // Parse passed RAW Ephemeris File
     GPSStatus ParseRAWFile   (const char* pszPath);
                                                                                
};



/* ICD200 conversion macros

NOTES: 

      1) Angular properties are expected in semi-circles; it is the duty of Ephemeris
         Parser implementation to perform this conversions from radians

      2) Currently it is not clear whether two's complement properties smaller in size
         than allocated 16 or 32 bit signed int need to have sign bit adjusted. If so,
         macros bellow will need to be adjusted (or moved into separate functions if more practical)



Parameter      # of bits      Scale Factor (LSB)      Units              Remark
---------------------------------------------------------------------------------------------------
cbias          22             2 exp (-31)             seconds            SV clock bias
cdrift         16             2 exp (-43)             sec/sec            SV clock drift
cdrift_rate     8             2 exp (-55)             sec/sec2           SV clock drift rate

iode            8                 -                      -               Data Change Indicator (see ICD-200, pg 113)
crs            16             2 exp (-5)              meters             Amplitude of the Sine Harmonic Correction Term to the Orbit Radius
deltan         16             2 exp (-43)             semi-circles/sec   Mean Motion Difference from Computed Value
m0             32             2 exp (-31)             semi-circles       Mean Anomaly at Reference Time

cuc            16             2 exp (-29)             radians            Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude
ecc            32             2 exp (-33)                -               Eccentricity
cus            16             2 exp (-29)             radians            Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude
sqrta          32             2 exp (-19)             m^(1/2)            Square Root of the Semi-Major Axis

toe            16             2 exp (+4)              sec                Reference Time Ephemeris 
cic            16             2 exp (-29)             rad                Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination
OMEGA          32             2 exp (-32)             semi-circles       Longitude of Ascending Node of Orbit Plane at Weekly Epoch
cis            16             2 exp (-29)             rad                Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination

i0             32             2 exp (-31)             semi-circles       Inclination Angle at Reference Time     
crc            16             2 exp (-5)              m                  Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius 
w              32             2 exp (-31)             semi-circles       Argument of Perigee         
omega_dot      24             2 exp (-43)             [semi-circles/sec] Rate of Right Ascension        

idot;          14             2 exp (-43)             [semi-circles/sec] Rate of Inclination Angle                                        
l2code          2                 -                         -            Codes on L2 channel          
gpsweek        10                 -                         -            GPS Week number (to go with toe)  
l2p             1                 -                         -            L2 p Data Flag     

accuracy        4                 -                   [index]            20.3.3.3.1.3     (SV accuracy)                          
health;         6                 -                         -            20.3.3.3.1.4     (SV health)     
tgd             8             2 exp (-31)             [sec]              20.3.3.3.1.8     (TGD: Estimate Group Delay Differential)) 
iodc           10                 -                         -            20.3.3.3.1.5     (IODC Issue of Data, Clock)    

toc            16             2 exp (+4)              [sec]              Transmission time of message sec of GPS week
fiti            1                 -                         -            Fit Interval: 0: <= 4 hrs, 1 > 4hrs
*/

// PRN / EPOCH / SV CLK
#define EPH_CBIAS_2_ICD(value)           static_cast<signed int>((value / exp2(-31) ) )
#define EPH_CDRIFT_2_ICD(value)          static_cast<signed short>((value / exp2(-43) ) )
#define EPH_CDRIFTRATE_2_ICD(value)      static_cast<signed char>((value / exp2(-55) ) )

// Broadcast Orbit 1
#define EPH_IODE_2_ICD(value)            static_cast<unsigned char>(value)
#define EPH_CRS_2_ICD(value)             static_cast<signed short>((value / exp2(-5) ) )
#define EPH_DELTAN_2_ICD(value)          static_cast<signed short>((value / exp2(-43) ) )
#define EPH_M0_2_ICD(value)              static_cast<signed int>((value / exp2(-31) ) )

// Broadcast Orbit 2
#define EPH_CUC_2_ICD(value)             static_cast<signed short>((value / exp2(-29) ) )
#define EPH_ECC_2_ICD(value)             static_cast<unsigned int>((value / exp2(-33) ) )
#define EPH_CUS_2_ICD(value)             static_cast<signed short>((value / exp2(-29) ) )
#define EPH_SQRTA_2_ICD(value)           static_cast<unsigned int>((value / exp2(-19) ) )

// Broadcast Orbit 3
#define EPH_TOE_2_ICD(value)             static_cast<unsigned short>((value / exp2(+4) ) )
#define EPH_CIC_2_ICD(value)             static_cast<signed short>((value / exp2(-29) ) )
#define EPH_OMEGA_2_ICD(value)           static_cast<signed int>((value / exp2(-32) ) )
#define EPH_CIS_2_ICD(value)             static_cast<signed short>((value / exp2(-29) ) )

// Broadcast Orbit 4
#define EPH_I0_2_ICD(value)              static_cast<signed int>((value / exp2(-31) ) )
#define EPH_CRC_2_ICD(value)             static_cast<signed short>((value / exp2(-5) ) )
#define EPH_W_2_ICD(value)               static_cast<signed int>((value / exp2(-31) ) )
#define EPH_OMEGADOT_2_ICD(value)        static_cast<signed int>((value / exp2(-43) ) )

// Broadcast Orbit 5: Codes on L2 channel, GPS week and L2p Data flag for completeness
#define EPH_IDOT_2_ICD(value)            static_cast<signed short>((value / exp2(-43) ) )
#define EPH_L2CODE_2_ICD(value)          static_cast<unsigned char>(value)    // scale factor: 1
#define EPH_GPSWEEK_2_ICD(value)         static_cast<unsigned short>(value)   // scale factor: 1
#define EPH_L2P_2_ICD(value)             static_cast<unsigned char>(value)    // scale factor: 1  

// Broadcast Orbit 6
inline unsigned char EPH_ACCURACY_2_ICD(double val) 
{
   // if outside range no accuracy prediction is available - unauthorized users
   // are advised to use the SV at their own risk
   if ( (val <= 0) || (val > 6144.00) ) return 15;

   if ((val <= 2.40) )    return 0;
   if ((val <= 3.40) )    return 1;
   if ((val <= 4.85) )    return 2;
   if ((val <= 6.85) )    return 3;
   if ((val <= 9.65) )    return 4;
   if ((val <= 13.65) )   return 5;
   if ((val <= 24.00) )   return 6;
   if ((val <= 48.00) )   return 7;
   if ((val <= 96.00) )   return 8;  
   if ((val <= 192.00) )  return 9; 
   if ((val <= 384.00) )  return 10; 
   if ((val <= 768.00) )  return 11; 
   if ((val <= 1536.00) ) return 12; 
   if ((val <= 3072.00) ) return 13; 
   if ((val <= 6144.00) ) return 14; 
   
   // never reached, but to silence the compiler
   return 15;
}

#define EPH_HEALTH_2_ICD(value)        static_cast<unsigned char>(value)
#define EPH_TGD_2_ICD(value)           static_cast<signed char>((value / exp2(-31) ) )
#define EPH_IODC_2_ICD(value)          static_cast<unsigned short>(value)

// Broadcast Orbit 7
#define EPH_TOC_2_ICD(value)           static_cast<unsigned short>((value / exp2(+4) ) )
inline unsigned char EPH_FITI_2_ICD (double val)
{
   return (val > 4.0 ? 1 : 0);
}


#endif    // __agpsephemeris_h__
