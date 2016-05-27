#ifndef __tigpsdriver_h__
#define __tigpsdriver_h__

#include "gpsdriver.h"

#define LOCATION_ARRAY_SIZE 3
#define PI 3.141592653

#define MAX_SPEED 30 // If speed > 30 km/h, then use doppler speed
#define MIN_SPEED 10 // If speed < 10 km/h, then use position speed

#if MAX_SPEED < MIN_SPEED
    #error MAX_SPEED needs to be greater than MIN_SPEED
#endif

/* TI Specific Implementation of GPS Driver */

typedef enum 
{
   NAVL_START,  
   NAVL_STOP     
}NavlState; 

typedef enum
{
    RXN_START,
    RXN_STOP
} RxnState;

class TIGPSDriver : public GPSDriver
{
  /* Variety of literal constants that were hardcoded all over the place in original TI implementation */
  public:
       static const char* GPS_CONFIG_FILE_PATH;   
       static const char* TI_GPS_PROCESSING_THREAD;

   protected:
      static const char* NAVL_FIFO;
      static const char* NAVL_EXIT_CMD;

      static const char* NAVL_SERVER_NAME;
      static const char* RXN_INTAPP_NAME;
      static const char* CTL_START;
      static const char* CTL_STOP;

      static const unsigned short MCPF_SOCKET_PORT;
      static const unsigned short INVALID_SOCKET;

      static const char*          MCPF_HOST_NAME;
      static const char*          SOC_NAME_4121;
      static const char*          SOC_NAME_6121;

      static const unsigned short LOCATION_FIX_TYPE_BITMAP;

      // uncomment if/when support for periodic session configuration via binary config file is enabled
      /*static const char*          GPS_PERIODIC_CONFIG_FILE_PATH; */  

      static const unsigned int   LOCATION_FIX_FREQUENCY;
      static const unsigned short LOCATION_FIX_NUM_REPORTS;

      static const float          HEADING_SCALE;
      static const double         LAT_SCALE;
      static const double         LONG_SCALE;

      static const float          C_LSB_EAST_VEL;
      static const float          C_LSB_NORTH_VEL;
      static const float          C_LSB_VERTICAL_VEL;

      static const float          C_LSB_EAST_ER;
      static const float          C_LSB_NORTH_ER;

      static const int64_t        GPS2UTC;
      static const int            SEC_MSEC;

      static const unsigned char  BEARING;
      static const unsigned char  HORSPEED;
      static const unsigned char  HORUNCERTSPEED;

      static const unsigned char  ASSIST_PRIORITY_ENABLE;   // TNAVC_assist_src_priority_set
      static const unsigned char  ASSIST_PRIORITY_DISABLE;

      // custom commands
      static const unsigned int   CMD_SUPL_SET_MODE;
      static const unsigned int   CMD_NAV_SET_MODE;
      static const unsigned int   CMD_NAV_REG_ASSIST;
      static const unsigned int   CMD_NAV_SET_ASSIST_PRIORITY;

   public:
       TIGPSDriver();
      ~TIGPSDriver();

      GPSStatus open (GPSResponseHandler*);       
      GPSStatus close (bool stop_server = false);

      GPSStatus sendCommand (struct _gpsmessage*); 

      // processing function entry point. Typically will execute in context of separate thread, thus
      // made static; however, this is not a requirement
      static void* TiGpsResponseHandler(void* contextPtr);   


     /* Variety of internal processing routines, taken from original TI implementation */
   protected:
      GPSStatus set_navl_server(NavlState state);
      GPSStatus connect_server();
      void buildNavMsg (struct _gpsmessage* pmsg, unsigned char* pcode, unsigned char* pPayload);
     
      // override processing dispatcher of full received packet from NAVD server
      void handleGps (gpsmessage* pmsg);

      // override assistance request
      void processAssistanceRequest (gpsmessage* pmsg);

      void processNmeaData (gpsmessage* pmsg);        // process NMEA responses
      void processProtMeasurement (); // process PROT measurement
      void processRawPosition ();     // process RAW  position
      void processProtPosition ();    // process PROT position

   private:
      void postProcessLocation();
      float calcDistance(double lat1, double lon1, double lat2, double lon2);
      void resetPostProcessLocation();
      void set_rxn_intapp(RxnState state);

   // data members
   protected:
      short                m_socket;        // navd server I/O
      short                m_TTFFflag;      // time to first fix indicator
      GpsSvStatus          m_gpsSvStatus;   // transient Space Vehicle Status, as pieces get filled at different times
      GpsLocation          m_gpsLocation;   // transient Location, as pieces seem to get filled at different times

      int                  m_assistState;

   private:
      GpsLocation          m_gpsLocationArray[LOCATION_ARRAY_SIZE];
      int                  m_gpsLocationPtr;
      GpsLocation          m_gpsPrevLocation;
      GpsLocation          m_gpsAvgLocation;
      bool                 m_gpsUsePosSpeed;
};

#endif  // __tigpsdriver_h__
