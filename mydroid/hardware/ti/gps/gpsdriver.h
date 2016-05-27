#ifndef __gpsdriver_h__
#define __gpsdriver_h__


#include "gpsresponsehandler.h"
#include "GpsWatchdog.h"

#define LOCATION_WD_PING 0x01       // Perform ping to watchdog on location fix reports
#define DATA_WD_PING     0x02       // Perform ping to watchdog on data reports
#define STATUS_WD_PING   0x04       // Perform ping to watchdog on status reports
#define CTRL_TD_PING     0x08       // Perform ping to watchdog on control thread creation
#define DATA_TD_PING     0x10       // Perform ping to watchdog on data thread creation
#define STATUS_TD_PING   0x20       // Perform ping to watchdog on status thread creation

#define READ_BUF_TIMEOUT 500000     // Read buffer select timeout (in us)

/* Definition of GPS Driver, responsible for sending OP commands
   and receiving responses/data buffers

   Send completes syncrhonously, in context of calling thread which eventually comes down from
   GpsLocationProvider in app (java/jni) space (via GpsInterface HAL implementation) 

   Receive blocks in separate thread; upon arrival of data, invokes services
   of ResponseHandler, then sends it upstairs before going back to Rx

   This is actually abstracted from TI specific transport (i.e. sockets)
   so it could be replaced by different I/O mechanism (i.e. true kernel driver)
   in the future, without impacting rest of the system

 */

/* enumeration of GPS Driver Interfaces this layer supports; used for Factory */
typedef enum 
{
   TI_GPS = 1,       // Texas Intruments NAVILINK
}OemDriverEnum;

class GPSDriver
{
    // variety of literal constants
    public:
       static const unsigned short MAX_TT_FF_VALUE;
       static const char*          DATA_PORT;     
       static const char*          STATUS_PORT;
       static const int            SLEEP_RECONNECT_SECS;   
       static const char*          DATA_PROCESSING_THREAD;
       static const char*          STATUS_PROCESSING_THREAD;

    protected:
       static const unsigned int MAX_BUF_LENGTH;
       static const char*        GPS_CONFIG_FILE_TOKEN_SEPARATOR;

       static const char* TOKEN_GPS_MODE;
       static const char* TOKEN_RECURRENCE;
       static const char* TOKEN_FIX_FREQUENCY;
       static const char* TOKEN_HOR_ACC;
       static const char* TOKEN_RSP_TIME;
       static const char* TOKEN_LOC_AGE;
       static const char* TOKEN_TT_FF;
       
       static const char* GPS_AUTONOMOUS_MODE;
       static const char* GPS_MSBASED_MODE;
       static const char* GPS_MSASSISTED_MODE;

       static const unsigned short MAX_HOR_ACC_VALUE;
       static const unsigned short MAX_LOC_AGE_VALUE;
       static const unsigned short MAX_RSP_TIME_VALUE;

   public:
      GPSDriver(); 
      virtual ~GPSDriver() = 0;                  // pure virtual d-tor; base is not intended as direct instantiation

      virtual GPSStatus open (GPSResponseHandler*) = 0;   // OEM Specific Initialization
      virtual GPSStatus close(bool stop_server = false) = 0;                    // OEM Specific Tear-down

      virtual GPSStatus      sendCommand(struct _gpsmessage* ) = 0;   // sends client command
      virtual GPSStatus      readConfigFile (const char*);            // reuse common configuration params; override if needed
    
      static GPSDriver* Factory(OemDriverEnum);  // virtual c-tor, in order to facilitate OEM specific instantiation
      
      void setSessionID(unsigned int id){m_sessionID = id;}

      void                enableWatchdog(int enable);
      void                disableWatchdog();
      int                 getEnableWatchdog();

      GPSStatus           openDataPort  ();
      void                closeDataPort ();

      GPSStatus           openStatusPort ();
      void                closeStatusPort ();

      // data function entry point. Typically will execute in context of separate thread, thus
      // made static; however, this is not a requirement. Originally implemented to support tearing up
      // TI NAV bottleneck horror, but can be utilized for other OEM configurations as long as low-level driver
      // knows how to dump data to GPSDriver::DATA_PORT in format we expect
      static void* DataResponseHandler(void* contextPtr);

      // status function entry point; same comments as for Data
      static void* StatusResponseHandler(void* contextPtr);

   // data members
   protected:
      int                 m_dataPort;           // Recon Data Port
      int                 m_statusPort;         // Recon Status Port

      GPSResponseHandler* m_pResponseHandler;   // GPS Messages Response Handler

      sp<GpsWatchdog>     m_pGpsWatchdog;       // GPS watchdog
      int                 m_enableWatchdog;     // GPS watchdog enable bitmask

      gpspositionmode     m_settings;           // GPS position fix configuration
      unsigned short      m_locage;             // Location Age

      struct _gpsmessage  m_gpsrsp;             // received gps message
      unsigned int        m_sessionID;          // Server Session ID

      int                 read_buffer  (int descriptor, unsigned char* buffer, unsigned int size);
      unsigned int        write_buffer (int descriptor, unsigned char* buffer, unsigned int size);
      void                pingWatchdog(int ping = 0);

      virtual void handleGps         (gpsmessage* pmsg);
      virtual void handleStatus      (gpsmessage* pmsg);

      virtual void processNmeaData   (gpsmessage* pmsg);         // process NMEA responses
      virtual void processAssistanceRequest (gpsmessage* pmsg);  // process Assistance Requests
};

#endif   // __gpsdriver_h__
