
/***************************************************************************
 *
 *  Recon GPS header file. By including this header, any client (including HAL layer)
 *  has access to GPS Baseband. Requirements:
 *
 *  -- instantiate desired driver (via GPSDriver::Factory)
 *  -- implement response handler (by subclassing GPSResponseHandler)
 *  -- call public exports of the Driver. Responses are received in context
 *     of Driver Processing thread. After they have been processed, client
 *     response handler is invoked
 *
 *     HAL Layer implementation forwards responses up Android call stack; 
 *     Other clients can do what they want, as this is client-server architectu
 *
 ***************************************************************************/
#ifndef __recongps_h__
#define __recongps_h__

typedef int GPSStatus;                   // status code
typedef void* (*gps_proc_fct)(void*);    // response processing function
#define NULL_HANDLE           0          // for pthreads

#define RECON_JET             1          // flip off for standard Android

/* Include files required across generic recongps library */
#include <hardware/gps.h>       // Android gps interface: Reusing structures and constants
#include <utils/Log.h>          // Logging support


/* GPS Session IDs */
class ReconGPSSession
{
   public:
     static const unsigned int HAL_SESSION_ID  = 254;
     static const unsigned int AGPS_SESSION_ID = 255;
};

/* AGPS Request Types */
class ReconAGPSType
{
   public:
     static const unsigned int REQ_ALMANAC    = GPS_DELETE_ALMANAC;
     static const unsigned int REQ_TIME       = GPS_DELETE_TIME;
     static const unsigned int REQ_POSITION   = GPS_DELETE_POSITION;
     static const unsigned int REQ_EPHEMERIS  = GPS_DELETE_EPHEMERIS;
};

/* Asynchronous Processing Thread Type */
typedef enum _receiverType
{
   CONTROL_THREAD,
   DATA_THREAD,
   STATUS_THREAD,
}receiverType;

/* Status conditions */
class GPSStatusCode
{
   public:
      static const int GPS_SUCCESS = 0;               // generic success code
      
      /* Status Indicators */
      static const int STATUS_GPS_SESSION_BEGIN   = 0x01;   
      static const int STATUS_GPS_SESSION_END     = 0x02;
      static const int STATUS_GPS_ENGINE_ON       = 0x03;
      static const int STATUS_GPS_ENGINE_OFF      = 0x04;
      static const int STATUS_GPS_REQUEST_ASSIST  = 0x05;
      static const int STATUS_GPS_DEL_ASSIST      = 0x06;

      /* Data Indicators */
      static const int DATA_NMEA            =  0x10;  // NMEA Data Indicator
      static const int DATA_SV              =  0x11;  // Space Vehicle Data Indicator
      static const int DATA_LOCATION        =  0x12;  // GpsLocation Data Indicator


      /* Error Codes */
      static const int GPS_E_INTERNAL       = -1;     // Generic Internal Error
      static const int GPS_E_MEM_ALLOC      = -2;     // Memory Allocation Failure
      static const int GPS_E_THREAD_ALLOC   = -3;     // Thread allocation error
      static const int GPS_E_IN_PARAM       = -4;     // Invalid Input Parameter
      static const int GPS_E_DRIVER_START   = -5;     // Internal Error starting GPS Driver
      static const int GPS_E_DRIVER_STOP    = -6;     // Internal Error stopping GPS Driver
      static const int GPS_E_CONFIG         = -7;     // Error extracting configuration data
      static const int GPS_E_UNEXPECTED     = -8;     // Method was called at unexpected time
      static const int GPS_E_DRIVER_CONNECT = -9;     // GPS Driver Connection Failure
      static const int GPS_E_DRIVER_READ    = -10;    // GPS Driver Read Failure
      static const int GPS_E_DRIVER_WRITE   = -11;    // GPS Driver Write Failure
      static const int GPS_E_INVALID_CMD    = -12;    // Invalid GPS Command
      static const int GPS_E_INVALID_RESP   = -13;    // Invalid GPS Response
      static const int GPS_E_ALMANAC_FORMAT = -14;    // Failure opening/parsing GPS Almanac File
      static const int GPS_E_ALMANAC_DATA   = -15;    // Invalid data in GPS Almanac File
      static const int GPS_E_EPHEMER_FORMAT = -16;    // Failure opening/parsing RINEX Ephemeris File
      static const int GPS_E_EPHEMER_DATA   = -17;    // Invalid Ephemeris Data in RINEX file
};

/* Enumeration of GPS commands that can be issued to firmware. This is essentially IOCTL specification! */
class GPSCommand 
{
   public:
      static const int GPS_START         = 0x01;        // Starts GPS Firmware
      static const int GPS_STOP          = 0x02;        // Stops  GPS Firmware
      static const int GPS_FIX_BEGIN     = 0x03;        // Begins GPS Session by Issuing Start Fix Request
      static const int GPS_FIX_END       = 0x04;        // Stops  GPS Session by Issying End Fix Request
      static const int GPS_SAVE_ASSIST   = 0x05;        // Instructs Firmware to save Assistance Data
      static const int GPS_SET_POS_MODE  = 0x06;        // GpsInterface::set_position_mode
      static const int GPS_DEL_ASSIST    = 0x07;        // GpsInterface::delete_aiding_data; flags are in payload
      static const int GPS_INJ_POSITION  = 0x08;        // Android GpsLocation is payload
      static const int GPS_INJ_TIME      = 0x09;        // gpstimeassist structure is payload
      static const int GPS_INJ_ALMANAC   = 0xA;         // AGPSAlmanac object is payload
      static const int GPS_INJ_EPHEMERIS = 0xB;         // APGSEphemeris object is payload
      static const int GPS_INJ_RAW_EPH   = 0xC;         // gpsephemeris structure is payload
      static const int GPS_BEGIN_ASSIST  = 0xD;         // no payload 
      static const int GPS_END_ASSIST    = 0xE;         // no payload
      static const int GPS_REG_ASSIST    = 0xF;         // registers assistant source

      static const int GPS_CMD_CUSTOM    = 0x20;        // Driver Specific Internal commands start here
      static const int GPS_FATAL_ERROR   = 0x21;        // NAV fatal error
      static const int GPS_NONE          = 0x99;
   
};

/* Gps Position Mode -- because Google didn't wrap this in nice structure! */
typedef struct _gpspositionmode
{
   GpsPositionMode         mode;                 // uint32_t: 4 bytes  0 - StandAlone, 1 - MobileStation Based, 2 - MobileStation Assisted
   GpsPositionRecurrence   recurrence;           // unit32_t: 4 bytes
   uint32_t                fix_frequency;        //           4 bytes 
   uint32_t                preferred_accuracy;   //           4 bytes
   uint32_t                preferred_time;       //           4 bytes
   uint32_t                tt_ff;                //           4 bytes: Time to First Fix. 0 means not configured

   _gpspositionmode():mode(GPS_POSITION_MODE_STANDALONE), recurrence(0),
    fix_frequency(0), preferred_accuracy(0), preferred_time(0), tt_ff(0) {}
   _gpspositionmode(GpsPositionMode m, GpsPositionRecurrence r, uint32_t ff, uint32_t pa, uint32_t pt, uint32_t ttff)
    :mode(m), recurrence(r), fix_frequency(ff), preferred_accuracy(pa), preferred_time(pt), tt_ff(ttff) {}
}gpspositionmode;

/* Gps Time Injection */
typedef struct _gpstimeassist
{
   unsigned int        gps_week;      // number of weeks since 1.1.1980
   unsigned int        gps_msec;      // millisecond offset into current week
   int                 unc_msec;      // Uncertainty (msec)
   
}gpstimeassist;


/* 
   Definition of almanac structure 
   Angular values are expected to be as specified by ICD-200. 
   Yuma parsers need to perform conversion
*/
typedef struct _gpsalmanac
{
     unsigned char  svid;     // SV PRN
     unsigned char  health;   // 000 - usable
     unsigned short week;     // 10 bit gps week 0-1023
     unsigned int   toa;      // almanac time of applicability (reference time)               [s]
     double         ecc;      // Eccentricity                                                 []
     double         i0;       // orbital inclination at reference time                        [semi-circles]   yuma:rad
     double         omegadot; // rate of right ascension                                      [semi-circles/s] yuma:[rad/s]
     double         sqrta;    // square root of the semi-major axis                           [m^(1/2)]
     double         omega0;   // longitude of ascending node of orbit plane at weekly epoch   [semi-circles]   yuma:[rad]
     double         w;        // argument of perigee                                          [semi-circles]   yuma:[rad]
     double         m0;       // mean anomaly at reference time                               [semi-circles]   yuma:[rad]
     double         af0;      // polynomial clock correction coefficient (clock bias)         [s]
     double         af1;      // polynomial clock correction coefficient (clock drift)        [s/s]
}gpsalmanac;

/* 
   Definition of Ephemeris structure for single SV, as specified in RINEX format
*/
typedef struct _gpsephemeris
{
   // PRN / EPOCH / SV CLK   (TOC = Time Of Clock)
   unsigned char  svid;          // SV PRN

   unsigned char  epoch_month;   // Epoch - TOC month
   unsigned char  epoch_hour;    // Epoch - TOC hour
   unsigned char  epoch_minute;  // Epoch - TOC minute
   unsigned short epoch_year;    // Epoch - TOC year
   unsigned char  epoch_day;     // Epoch - TOC day

   unsigned char  filler [1];

   double         epoch_second;  // Epoch - TOC second

   double         cbias;         // SV clock bias  in sec
   double         cdrift;        // SV clock drift in sec/sec
   double         cdrift_rate;   // SV clock drift rate in sec/sec2

   // Broadcast Orbit - 1
   double         iode;        // IODE Issue of Data, Ephemeris                                               [discrete]
   double         crs;         // Amplitude of the Sine Harmonic Correction Term to the Orbit Radius   [m]                RINEX: [m]
   double         deltan;      // Mean Motion Difference from Computed Value                           [semi-circles/sec] RINEX: [rad/sec]
   double         m0;          // Mean Anomaly at Reference Time                                       [semi-circles]     RINEX: [rad]

   // Broadcast Orbit - 2
   double         cuc;         // Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude [rad]      RINEX: [rad]
   double         ecc;         // Eccentricity                                                                     [discrete]
   double         cus;         // Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude   [rad]      RINEX: [rad]
   double         sqrta;       // Square Root of the Semi-Major Axis                                           [m^(1/2)]  RINEX: [m^(1/2)]

   // Broadcast Orbit - 3
   double         toe;         // Reference Time Ephemeris                                                     [sec]      RINEX: [sec of GPS week]
   double         cic;         // Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination [rad]      RINEX: [rad]
   double         OMEGA;       // Longitude of Ascending Node of Orbit Plane at Weekly Epoch           [semi-circles]     RINEX: [rad]  
   double         cis;         // Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination   [rad]      RINEX: [rad]

   // Broadcast Orbit - 4
   double         i0;          // Inclination Angle at Reference Time                                  [semi-circles]     RINEX: [rad]
   double         crc;         // Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius [m]                RINEX: [m]
   double         w;           // Argument of Perigee                                                  [semi-circles]     RINEX: [rad]
   double         omega_dot;   // Rate of Right Ascension                                              [semi-circles/sec] RINEX: [rad/sec]

   // Broadcast Orbit - 5
   double         idot;        // Rate of Inclination Angle                                            [semi-circles/sec] RINEX: [rad/sec]
   double         l2code;      // Codes on L2 channel                                                             [discrete]
   double         gpsweek;     // GPS Week number (to go with toe)                                                [discrete]
   double         l2p;         // L2 p Data Flag                                                                  [discrete]    

   // Broadcast Orbit - 6
   double         accuracy;    // 20.3.3.3.1.3     (SV accuracy)                                       [index]            RINEX: [m]
   double         health;      // 20.3.3.3.1.4     (SV health)        (bits 17-22 w 3 sf 1)                       [discrete]
   double         tgd;         // 20.3.3.3.1.8     (TGD: Estimate Group Delay Differential))           [sec]              RINEX: [sec]
   double         iodc;        // 20.3.3.3.1.5     (IODC Issue of Data, Clock)                                    [discrete]

   // Broadcast Orbit - 7
   double         toc;         //    ?  Transmission time of message sec of GPS week, derived e.g.           ?            RINEX: [sec]
                               //       from Z-count in Hand Over Word (HOW)
   double         fiti;        //    Fit Interval: indicates whether the ephemerides are based on a four-hour
                               //                  fit interval or a fit interval greater than four hours    [flag]       RINEX: [hours]
  
}gpsephemeris;


/* 
   RAW Ephemeris Structure for Single SV, as specified by ICD-200
*/
typedef struct _gpsrawephemeris
{
  unsigned char  svid;          //         Satellite PRN Number
  unsigned char  iode;          //         IODE Issue of Data, Ephemeris                  
  signed short   crs;           //         Amplitude of the Sine Harmonic Correction Term to the Orbit Radius    
  signed short   delta_n;       //         Mean Motion Difference from Computed Value                              
  signed short   cuc;           //         Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude
  signed int     m0;            //         Mean Anomaly at Reference Time      
  unsigned int   e;             //         Eccentricity
  unsigned int   sqrta;         //         Square Root of the Semi-Major Axis                       
  signed int     omega0;        //         Longitude of Ascending Node of Orbit Plane at Weekly Epoch
  signed short   cus;           //         Amplitude of the Sine Harmonic Correction Term to the Argument of Latitudude                     
  unsigned short toe;           //         Reference Time Ephemeris
  signed short   cic;           //         Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination      
  signed short   cis;           //         Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination
  signed int     i0;            //         Inclination Angle at Reference Time 
  signed int     w;             //         Argument of Perigee  (omega)
  signed int     omegadot;      //         Rate of Right Ascension                            
  signed short   crc;           //         Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius
  signed short   idot;          //         Rate of Inclination Angle        
  unsigned char  l2code;        //         Codes on L2 channel     
  unsigned char  accuracy;      //         SV Accuracy                         
  unsigned char  health;        //         SV Health                      
  signed char    tgd;           //         TGD  (Estimate Group Delay Differential)                          
  unsigned short iodc;          //         Issue of Data, Clock (IODC)                         
  unsigned short toc;           //         Sattelite Clock Correction               
  signed char    af2;           //         SV clock drift rate in sec/sec2                   
  unsigned char  align;         //           <alignment - so it is easier to read in>
  signed short   af1;           //         SV clock drift in sec/sec
  signed int     af0;           //         SV clock bias in sec            
  unsigned char  predicted;     //         Set to TRUE if the ephemeris is predicted, FALSE if it is decoded from sky                
  unsigned char  seedage;       //         Used for storing the Ephemeris seed age incase of Predicted Aphemeris
  unsigned short week;          //         GPS Week number (to go with toe) - 65535 if unknown 
}gpsrawephemeris;




/* Definition of  generic GPS Message structure; used both ways (requests & responses) */
typedef struct _gpsmessage
{
   int            opcode;      // 4 bytes: message code
   unsigned int   i_msglen;    // 4 bytes: payload size
   unsigned char* pPayload;    // 4 bytes: pointer to block of payload memory, managed within processing function

   _gpsmessage():opcode(GPSCommand::GPS_NONE), i_msglen(0), pPayload(0){}
}gpsmessage;

/* Helper Functions 

   Note: GpsUtcTime is 64-bit, defined in Android gps.h as 

  (Milliseconds since January 1, 1970)
  typedef int64_t GpsUtcTime;
*/


class GPSUtilities
{
   protected:
     static const unsigned int _leapseconds [];

     static const unsigned int _gpsoffset;        // 1.1.1970 - 1.1.1980 offset in seconds
     static const unsigned int SEC_2_MSEC;
     static const unsigned int WEEK_MSEC;         // number of milliseconds in week

     
     static unsigned int  countleaps (GpsUtcTime, unsigned char);
     static unsigned char isleap (unsigned int);

   public:
     static GpsUtcTime gps2unix (GpsUtcTime gpstime);
     static GpsUtcTime unix2gps (GpsUtcTime unixtime);

     static void get_gps_time (GpsUtcTime utctime_msec, unsigned int* pgpsweek, unsigned int* pweekmsec);
     static char* TrimLine (char* line);
};

/* Various macros */

#define GPS_LOG_INF(...)  ALOGI(__VA_ARGS__)
#define GPS_LOG_WARN(...) ALOGW(__VA_ARGS__)

#define GPS_LOG_ERR(...) \
{ \
   ALOGE("+++ Source [%s] Line [%d] Function [%s] +++\n", __FILE__, __LINE__, __FUNCTION__); \
   ALOGE(__VA_ARGS__); \
}

#define GPS_LOG_TRACE(...)  ALOGV(__VA_ARGS__) 




#endif   // __recongps_h__
