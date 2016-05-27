/*
 *******************************************************************************
 *         
 *
 * FileName         :   TIGPSDriver.cpp
 *
 * Description      :   Implementation of TIGPSDriver object
 *
 * Author           :   RECON Instruments
 * Date             :   November 2013
 *
 * Comments         :   Concrete Implementation of TI GPS Driver --
 *                      communication with NAVD server via MCP Sockets
 ******************************************************************************
 */
#define LOG_TAG "RECON.GPS"   // our identification for logcat (adb logcat RECON.GPS:V *:S)

#include "recongps.h" 
#include "agpsalmanac.h"
#include "agpsephemeris.h"

// Platform Includes
#include "tigpsdriver.h"    // definition of this object

// various TI headers; this is not structured cleanly, as there are inter-dependencies
// This is another reason I abstracted TI interface from rest of the Library
#include <cutils/properties.h>
#include <math.h>
#include <signal.h>    

#include <sys/ioctl.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/types.h>

#include <sys/time.h>
#include <cutils/properties.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include <sys/socket.h>
#include <sys/un.h>
#include <netdb.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "mcp_hal_misc.h"
#include "mcp_hal_fs.h"
#include "mcpf_services.h"
#include "gpsc_types.h"
#include "navc_api.h"
#include "suplc_api.h"
#include "hostSocket.h"
#include "navl_api.h"
#include "navc_priority_handler.h"
#include "gpsc_data.h"


// constant initialization (this can be done as simple define, etc. but this is "java" like way 
// which can be done in C++ as well)
const char* TIGPSDriver::TI_GPS_PROCESSING_THREAD  = "hgpsCBthread";

const char* TIGPSDriver::NAVL_FIFO                 = "/data/gps/navlfifo";
const char* TIGPSDriver::NAVL_EXIT_CMD             = "exit";

const char* TIGPSDriver::NAVL_SERVER_NAME          = "navl_server";
const char* TIGPSDriver::RXN_INTAPP_NAME           = "rxn";
const char* TIGPSDriver::CTL_START                 = "ctl.start";
const char* TIGPSDriver::CTL_STOP                  = "ctl.stop";

const char* TIGPSDriver::GPS_CONFIG_FILE_PATH      = "/system/etc/gps/config/GpsConfigFile.txt";
/* const char* TIGPSDriver::GPS_PERIODIC_CONFIG_FILE_PATH = "/system/etc/gps/config/PeriodicConfFile.cfg"; */

const unsigned short TIGPSDriver::MCPF_SOCKET_PORT =  4121;
const unsigned short TIGPSDriver::INVALID_SOCKET   = -1;

const char* TIGPSDriver::MCPF_HOST_NAME            =  "127.0.6.1";
const char* TIGPSDriver::SOC_NAME_4121             = "/data/gps/gps4121";
const char* TIGPSDriver::SOC_NAME_6121             = "/data/gps/gps6121";


// #ifdef GPSM_NMEA_FILE
const unsigned short TIGPSDriver::LOCATION_FIX_TYPE_BITMAP = (GPSC_RESULT_PROT | NMEA_MASK | GPSC_RESULT_RAW); 
//const unsigned short TIGPSDriver::LOCATION_FIX_TYPE_BITMAP = (GPSC_RESULT_PROT | GPSC_RESULT_RAW); 

const unsigned int   TIGPSDriver::LOCATION_FIX_FREQUENCY = 1;
const unsigned short TIGPSDriver::LOCATION_FIX_NUM_REPORTS = 0;

const float          TIGPSDriver::HEADING_SCALE   = (32768.0F / 180.0F);
const float          TIGPSDriver::C_LSB_EAST_VEL  = (1000.0 / 65535.0);   /* -500 to 500 range, 16
                                                                          bits: LSB = 0.0152590
                                                                          meters/sec */

const float          TIGPSDriver::C_LSB_NORTH_VEL = (1000.0 / 65535.0);   /* -500 to 500 range, 16
                                                                           bits: LSB = 0.0152590
                                                                           meters/sec */
const float          TIGPSDriver::C_LSB_VERTICAL_VEL = (1000.0/65535.0);  /* -500 to 500 range, 16
                                                                              bits: LSB = 0.0152590
                                                                               meters/sec */

const float          TIGPSDriver::C_LSB_EAST_ER   = (0.1);
const float          TIGPSDriver::C_LSB_NORTH_ER  = (0.1);

const double         TIGPSDriver::LAT_SCALE       = (8388608.0F / 90.0F);
const double         TIGPSDriver::LONG_SCALE      = (16777216.0F / 360.0F);

const int64_t        TIGPSDriver::GPS2UTC         = 315964800;   // 1.1.1980 UTC - 1.1.1970 UTC (sec)
const int            TIGPSDriver::SEC_MSEC        = 1000;

const unsigned char  TIGPSDriver::BEARING         = 0x02;
const unsigned char  TIGPSDriver::HORSPEED        = 0x04;
const unsigned char  TIGPSDriver::HORUNCERTSPEED  = 0x10;

const unsigned char  TIGPSDriver::ASSIST_PRIORITY_ENABLE  = 1;
const unsigned char  TIGPSDriver::ASSIST_PRIORITY_DISABLE = 0;

// custom commands, to bypass silly architecture where single command must be sent as multiple messages
const unsigned int   TIGPSDriver::CMD_SUPL_SET_MODE            = GPSCommand::GPS_CMD_CUSTOM + 1;
const unsigned int   TIGPSDriver::CMD_NAV_SET_MODE             = GPSCommand::GPS_CMD_CUSTOM + 2;
const unsigned int   TIGPSDriver::CMD_NAV_REG_ASSIST           = GPSCommand::GPS_CMD_CUSTOM + 3;
const unsigned int   TIGPSDriver::CMD_NAV_SET_ASSIST_PRIORITY  = GPSCommand::GPS_CMD_CUSTOM + 4;


/* Constructor */
TIGPSDriver::TIGPSDriver()
:m_socket(TIGPSDriver::INVALID_SOCKET), m_TTFFflag(0)
{
   memset(&m_gpsSvStatus, 0, sizeof(GpsSvStatus) );
   m_gpsSvStatus.size = sizeof(GpsSvStatus);

   memset(&m_gpsLocation, 0, sizeof(GpsLocation) );
   m_gpsLocation.size = sizeof(GpsLocation);

   // initialize location fix settings
   m_settings.mode               = GPS_POSITION_MODE_MS_BASED;
   m_settings.fix_frequency      = TIGPSDriver::LOCATION_FIX_FREQUENCY;
   m_settings.preferred_accuracy = GPSDriver::MAX_HOR_ACC_VALUE;
   m_settings.preferred_time     = GPSDriver::MAX_RSP_TIME_VALUE;

   m_locage = GPSDriver::MAX_LOC_AGE_VALUE;
   m_assistState = GPSCommand::GPS_END_ASSIST;

   // initialize GPS post processing location values
   resetPostProcessLocation();
}

/* Destructor */
TIGPSDriver::~TIGPSDriver()
{
   this->close();
   delete [] m_gpsrsp.pPayload;   // free to delete null pointer
   m_gpsrsp.pPayload = 0;
}

GPSStatus TIGPSDriver::open (GPSResponseHandler* pResponseHandler)
{
   GPS_LOG_TRACE ("Enter\n");
   GPSStatus stat = GPSStatusCode::GPS_SUCCESS;
   
   stat = this->set_navl_server(NAVL_START);   // starts NAVL server (will block internally a bit)
   if (stat == GPSStatusCode::GPS_SUCCESS)
     stat = this->connect_server();            // connects server socket

   if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

   // Remember Response Handler; it is allocated and configured by Client
   m_pResponseHandler = pResponseHandler;

   // all ok!
   GPS_LOG_TRACE("OK\n");
   return stat;
}
       
GPSStatus TIGPSDriver::close(bool stop_server)
{
    if (stop_server) {
        // Send command to stop NAV server. The NAV server will be restarted by Android.
        GPS_LOG_INF("Sending NAVL_STOP to nav server");
        set_navl_server(NAVL_STOP);
    }

    // close the socket. Parent is responsible for receiver thread management
    ::shutdown(m_socket, SHUT_RDWR);
    ::close(m_socket);

    m_socket = TIGPSDriver::INVALID_SOCKET;
    m_TTFFflag = 0;

    return GPSStatusCode::GPS_SUCCESS;
}      

/* Concrete IOCTL Implementation for TI navd server */
GPSStatus TIGPSDriver::sendCommand (struct _gpsmessage* pmsg)
{
   hostSocket_header_t s_MsgHdr;
   unsigned char* pPayload = 0;

   memset(&s_MsgHdr, 0, sizeof(hostSocket_header_t) );

   GPS_LOG_TRACE("Enter. Requested command: [0x%x]\n", pmsg->opcode);

   // check for client commands that must be sent in multiple pieces

   // set position mode - optional send to SUPL first, depending on current GPS mode
   if (pmsg->opcode == GPSCommand::GPS_SET_POS_MODE)
   {
      gpspositionmode* pmode = reinterpret_cast<struct _gpspositionmode*>(pmsg->pPayload);
      if (pmode->mode != GPS_POSITION_MODE_STANDALONE)
      {
          pmsg->opcode = TIGPSDriver::CMD_SUPL_SET_MODE;
          GPSStatus stat = this->sendCommand(pmsg);
          if (stat != GPSStatusCode::GPS_SUCCESS) return stat;

          sleep(1);
      }

      pmsg->opcode = TIGPSDriver::CMD_NAV_SET_MODE;
      return this->sendCommand(pmsg);
   }

   // assistant registration: register + set priority
   if (pmsg->opcode == GPSCommand::GPS_REG_ASSIST)
   {
       pmsg->opcode = TIGPSDriver::CMD_NAV_REG_ASSIST;
       GPSStatus stat = this->sendCommand(pmsg);
     
       return stat;
   }

   // now single messages
   // build the I/O buffer: Header + Payload. This is now translation layer
   // between Library Generic format, and TI Specific format
   switch (pmsg->opcode)
   {
      case GPSCommand::GPS_START:  /* NAVC_CMD_START */
      case GPSCommand::GPS_STOP:   /* NAVC_CMD_STOP */
      {
         s_MsgHdr.msgClass = NAVL_CLASS_INTERNAL;
         s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
         s_MsgHdr.payloadLen =  0;
      }
      break;

      /* NAVD_CMD_REQUEST_FIX */
      case GPSCommand::GPS_FIX_BEGIN:
      {
           // On each location fix request, reset post processing location values
           resetPostProcessLocation();

           // allocate memory for payload
           pPayload = new unsigned char[sizeof(TNAVC_reqLocFix)];
           if (!pPayload)
           {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
           }

           s_MsgHdr.msgClass = NAVL_CLASS_GPS;
           s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
           s_MsgHdr.payloadLen = sizeof(TNAVC_reqLocFix);
      }
      break;

      /* NAVC_CMD_STOP_FIX */
      case GPSCommand::GPS_FIX_END:
      {
           // allocate memory for payload
           pPayload = new unsigned char[sizeof(TNAVC_stopLocFix)];
           if (!pPayload)
           {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
           }

           s_MsgHdr.msgClass = NAVL_CLASS_GPS;
           s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
           s_MsgHdr.payloadLen = sizeof(TNAVC_stopLocFix);
           
      }
      break;

      /* NAVC_CMD_BEGIN_ASSISTANCE */
      /* NAVC_CMD_SAVE_ASSISTANCE */
      /* NAVC_CMD_COMPLETE_ASSISTANCE */
      case GPSCommand::GPS_BEGIN_ASSIST:
      case GPSCommand::GPS_SAVE_ASSIST:
      case GPSCommand::GPS_END_ASSIST:
      {
            s_MsgHdr.msgClass = NAVL_CLASS_GPS;
            s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
            s_MsgHdr.payloadLen =  0;
      }
      break;

      /* NAVC_CMD_DELETE_ASISTANCE */
      case GPSCommand::GPS_DEL_ASSIST:
      {
          // NOTE: Original TI implementation sends separately ephemeris and almanac, although it appears
          //       their 'delete bitmap' accomodates generic case. I am not going to split it initially until
          //       it proves necessary (check with TI contact)
          pPayload = new unsigned char[sizeof(TNAVC_delAssist)];
          if (!pPayload)
          {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
          }

         s_MsgHdr.msgClass = NAVL_CLASS_GPS;
         s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
         s_MsgHdr.payloadLen =  sizeof(TNAVC_delAssist);

      }
      break;

      /* NAVC_CMD_REGISTER_ASSIST_SRC */
      case TIGPSDriver::CMD_NAV_REG_ASSIST:
      {
          pPayload = new unsigned char[sizeof(TNAVC_assist_src_type)];
          if (!pPayload)
          {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
          }

         s_MsgHdr.msgClass = NAVL_CLASS_GPS;
         s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
         s_MsgHdr.payloadLen =  sizeof(TNAVC_assist_src_type);
      }
      break;

      /* NAVC_CMD_SET_ASSIST_PRIORITY */
      case TIGPSDriver::CMD_NAV_SET_ASSIST_PRIORITY:
      {
          // MAX_ASSIST_PROVIDER: ENAVC_assist_src_type
          pPayload = new unsigned char[MAX_ASSIST_PROVIDER * sizeof(TNAVC_assist_src_priority_set) ];
          if (!pPayload)
          {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
          }

         s_MsgHdr.msgClass = NAVL_CLASS_GPS;
         s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
         s_MsgHdr.payloadLen =  MAX_ASSIST_PROVIDER * sizeof(TNAVC_assist_src_priority_set);
      }
      break;

      /* NAVC_CMD_QOP_UPDATE */
      case TIGPSDriver::CMD_NAV_SET_MODE:
      {
           // allocate memory for payload
           pPayload = new unsigned char[sizeof(TNAVC_updateFixRate)];
           if (!pPayload)
           {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
           }

           s_MsgHdr.msgClass = NAVL_CLASS_GPS;
           s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
           s_MsgHdr.payloadLen =  sizeof(TNAVC_updateFixRate);
 
      }
      break;

      /* SUPLC_CMD_SET_MODE */
      case TIGPSDriver::CMD_SUPL_SET_MODE:
      {
           // allocate memory for payload
           pPayload = new unsigned char[sizeof(TSUPLC_FixMode)];
           if (!pPayload)
           {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
           }

           s_MsgHdr.msgClass = NAVL_CLASS_SUPL;
           s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
           s_MsgHdr.payloadLen =  sizeof(TSUPLC_FixMode);
 
      }
      break;

      /* NAVC_CMD_INJECT_ASSISTANCE */
      case GPSCommand::GPS_INJ_TIME:       // GPSC_ASSIST_TIME
      case GPSCommand::GPS_INJ_POSITION:   // GPSC_ASSIST_POSITION
      case GPSCommand::GPS_INJ_ALMANAC:    // GPSC_ASSIST_ALMANAC
      case GPSCommand::GPS_INJ_EPHEMERIS:  // GPSC_ASSIST_EPH
      case GPSCommand::GPS_INJ_RAW_EPH:    // GPSC_ASSIST_EPH
      {
           // sanity check - for raw ephemeris structs must match (68 bytes is size as of original implementation) 
           if (pmsg->opcode == GPSCommand::GPS_INJ_RAW_EPH)
           {
              if (sizeof(struct _gpsrawephemeris) != sizeof(T_GPSC_ephemeris_assist) )
              {
                 GPS_LOG_ERR("Internal Error - size of Raw Ephemeris structure does not match (%d != %d)\n",
                     sizeof(struct _gpsrawephemeris), sizeof(T_GPSC_ephemeris_assist) );

                 return GPSStatusCode::GPS_E_UNEXPECTED;
              }
           }

           // allocate memory for payload
           pPayload = new unsigned char[sizeof(TNAVC_cmdParams)];
           if (!pPayload)
           {
              GPS_LOG_ERR("Memory Allocation Error!\n");
              return GPSStatusCode::GPS_E_MEM_ALLOC;
           }

           s_MsgHdr.msgClass = NAVL_CLASS_GPS;
           s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
           s_MsgHdr.payloadLen =  sizeof(TNAVC_cmdParams);
      }
      break;


      default:
        GPS_LOG_ERR("Unsupported GPS Command [%d] request\n", pmsg->opcode);
        return GPSStatusCode::GPS_E_INVALID_CMD;
   }


   // call internal helper to build rest of message
   this->buildNavMsg (pmsg, &(s_MsgHdr.opCode), pPayload);

   // send header. We are in blocking mode, so partial send means connection problem
   // in which case we abort with failure
   if (this->write_buffer(m_socket, reinterpret_cast<unsigned char*>(&s_MsgHdr), sizeof(s_MsgHdr) ) != sizeof(s_MsgHdr) )
   {
        GPS_LOG_ERR("Error sending [0x%x] command to NAV Server\n", pmsg->opcode);
        return GPSStatusCode::GPS_E_DRIVER_WRITE;
   }

   // Send payload if any
   if (s_MsgHdr.payloadLen > 0)
   {
        unsigned int iSent = this->write_buffer(m_socket, pPayload, s_MsgHdr.payloadLen);

        // deallocate payload memory in each case
        delete [] pPayload;
        if (iSent != s_MsgHdr.payloadLen)
        {
            GPS_LOG_ERR("Error sending payload for [0x%x] command to NAV Server\n", pmsg->opcode);
            return GPSStatusCode::GPS_E_DRIVER_WRITE;
        }
   }

   // If these were Location Fix Settings, save them
   if (pmsg->opcode == GPSCommand::GPS_SET_POS_MODE)
   {
      gpspositionmode* pmode = reinterpret_cast<struct _gpspositionmode*>(pmsg->pPayload);

      m_settings.mode = pmode->mode;
      m_settings.recurrence = pmode->recurrence;
      m_settings.fix_frequency = (pmode->fix_frequency == 0) ? 1 : pmode->fix_frequency / 1000;
      if (pmode->preferred_accuracy > 0) // if specified
      {
          m_settings.preferred_accuracy = (pmode->preferred_accuracy > GPSDriver::MAX_HOR_ACC_VALUE) ? GPSDriver::MAX_HOR_ACC_VALUE : pmode->preferred_accuracy;
      } 

      if (pmode->preferred_time > 0) // if specified
      {
          m_settings.preferred_time = (pmode->preferred_time > GPSDriver::MAX_RSP_TIME_VALUE) ? GPSDriver::MAX_RSP_TIME_VALUE : pmode->preferred_time;
      } 

   }

   // all ok here!
   GPS_LOG_INF("+++ %s: Successfully sent command [0x%x] +++\n", __FUNCTION__, s_MsgHdr.opCode);
   return GPSStatusCode::GPS_SUCCESS;
} 




/* Internal routines, taken from original TI implementation */

/* NAV Message Builder:
   -- translates GPS code into NAV code
   -- allocates and populates payload; sets payload size
 */
void TIGPSDriver::buildNavMsg 
(
   struct _gpsmessage*  pmsg,        // platform GPS message
   unsigned char*       pcode,       // resulting NAV code
   unsigned char*       pPayload     // NAV payload (managed by caller)
)
{
   switch (pmsg->opcode)
   {
      /* NAVC_CMD_START */
      case GPSCommand::GPS_START:  
      {
          (*pcode) = NAVC_CMD_START;
      }
      break;

      /* NAVC_CMD_STOP */
      case GPSCommand::GPS_STOP:   
      {
          (*pcode) = NAVC_CMD_STOP;
      }
      break;

      /* NAVC_CMD_REQUEST_FIX */
      case GPSCommand::GPS_FIX_BEGIN:  
      {
          (*pcode) = NAVC_CMD_REQUEST_FIX;

          TNAVC_reqLocFix* pReqLocFix = reinterpret_cast<TNAVC_reqLocFix*>(pPayload);
          memset(pReqLocFix, 0, sizeof(TNAVC_reqLocFix) );

          // he identifies himself as autonomous session Question: Session token must come from NAVD server!
          // Fix Mode and Call Type are bassed on GPS Mode
          pReqLocFix->loc_fix_session_id  = m_sessionID;

          switch (m_settings.mode)
          {
             case GPS_POSITION_MODE_MS_BASED:
             {
                 pReqLocFix->loc_fix_mode = GPSC_FIXMODE_MSBASED;
                 pReqLocFix->call_type    = NAVC_SUPL_MO;
             }
             break;

             case GPS_POSITION_MODE_MS_ASSISTED:
             {
                 pReqLocFix->loc_fix_mode = GPSC_FIXMODE_MSASSISTED;
                 pReqLocFix->call_type    = NAVC_SUPL_MO;
             }
             break;

             case GPS_POSITION_MODE_STANDALONE:
             default:
                 pReqLocFix->loc_fix_mode = GPSC_FIXMODE_AUTONOMOUS;
                 pReqLocFix->call_type = NAVC_STANDLONE;
          }
      
          pReqLocFix->loc_fix_result_type_bitmap = TIGPSDriver::LOCATION_FIX_TYPE_BITMAP;

          // TBD: support for fix period and num reports from PERIODIC_CONF_FILE 
          //      (was disabled in original TI code)
          pReqLocFix->loc_fix_period      = m_settings.fix_frequency;
          pReqLocFix->loc_fix_num_reports = TIGPSDriver::LOCATION_FIX_NUM_REPORTS;

          pReqLocFix->loc_fix_max_ttff                = m_settings.tt_ff;
          pReqLocFix->loc_fix_qop.horizontal_accuracy = m_settings.preferred_accuracy;
          pReqLocFix->loc_fix_qop.max_response_time   = m_settings.preferred_time;
          pReqLocFix->loc_fix_qop.max_loc_age         = m_locage;

      }
      break;

      /* NAVC_CMD_STOP_FIX */
      case GPSCommand::GPS_FIX_END:
      {
         (*pcode) = NAVC_CMD_STOP_FIX;

         TNAVC_stopLocFix* pStopLocFix = reinterpret_cast<TNAVC_stopLocFix*>(pPayload);
         memset(pStopLocFix, 0, sizeof(TNAVC_stopLocFix) );

         pStopLocFix->uSessionId = m_sessionID;
      }
      break;

      /* NAVC_CMD_BEGIN_ASSISTANCE */
      case GPSCommand::GPS_BEGIN_ASSIST:
      {
         (*pcode) = NAVC_CMD_BEGIN_ASSISTANCE;
         m_assistState = GPSCommand::GPS_BEGIN_ASSIST;
      }
      break;

      /* NAVC_CMD_SAVE_ASSISTANCE */
      case GPSCommand::GPS_SAVE_ASSIST:
      {
         (*pcode) = NAVC_CMD_SAVE_ASSISTANCE;
      }
      break;

      /* NAVC_CMD_COMPLETE_ASSISTANCE */
      case GPSCommand::GPS_END_ASSIST:
      {
         (*pcode) = NAVC_CMD_COMPLETE_ASSISTANCE;
         m_assistState = GPSCommand::GPS_END_ASSIST;
      }
      break;


      /* NAVC_CMD_DELETE_ASISTANCE (Note the typo!! Should be 2s) */
      case GPSCommand::GPS_DEL_ASSIST:
      {
         (*pcode) = NAVC_CMD_DELETE_ASISTANCE;

         TNAVC_delAssist* pDelAssist = reinterpret_cast<TNAVC_delAssist*>(pPayload);
         memset(pDelAssist, 0, sizeof(TNAVC_delAssist) );

         // now populate delete bitmap, there is some obfuscated logic
         GpsAidingData* pflags = reinterpret_cast<GpsAidingData*>(pmsg->pPayload);

         if ((*pflags & GPS_DELETE_ALL) == GPS_DELETE_ALL)
         {
            pDelAssist->uDelAssistBitmap |= 
               GPSC_DEL_AIDING_TIME | GPSC_DEL_AIDING_POSITION | GPSC_DEL_AIDING_IONO_UTC |
               GPSC_DEL_AIDING_SVHEALTH | GPSC_DEL_AIDING_ALMANAC | GPSC_DEL_AIDING_EPHEMERIS;
         }
         else
         {
               if (*pflags & GPS_DELETE_POSITION)
                 pDelAssist->uDelAssistBitmap |= GPSC_DEL_AIDING_POSITION;

               if (*pflags & GPS_DELETE_TIME)
                 pDelAssist->uDelAssistBitmap |= GPSC_DEL_AIDING_TIME;

               if ( (*pflags & GPS_DELETE_IONO) || (*pflags & GPS_DELETE_UTC) )
                 pDelAssist->uDelAssistBitmap |= GPSC_DEL_AIDING_IONO_UTC;

               if (*pflags & GPS_DELETE_HEALTH)
                 pDelAssist->uDelAssistBitmap |= GPSC_DEL_AIDING_SVHEALTH;

               if (*pflags & GPS_DELETE_SVDIR)
                 pDelAssist->uDelAssistBitmap |= GPSC_DEL_AIDING_SVDIR;

              if (*pflags & GPS_DELETE_ALMANAC)
                 pDelAssist->uDelAssistBitmap |= GPSC_DEL_AIDING_ALMANAC;

              if (*pflags & GPS_DELETE_EPHEMERIS)
                 pDelAssist->uDelAssistBitmap |= GPSC_DEL_AIDING_EPHEMERIS;
         }

      }
      break;

      /* NAVC_CMD_REGISTER_ASSIST_SRC */
      case TIGPSDriver::CMD_NAV_REG_ASSIST:
      {
         (*pcode) = NAVC_CMD_REGISTER_ASSIST_SRC;
         TNAVC_assist_src_type* pRegisterClient = reinterpret_cast<TNAVC_assist_src_type*>(pPayload);
         pRegisterClient->eAssistSrcType = CUSTOM_AGPS_PROVIDER1;  
      }
      break;

      /* NAVC_CMD_SET_ASSIST_PRIORITY */
      case TIGPSDriver::CMD_NAV_SET_ASSIST_PRIORITY:
      {
         (*pcode) = NAVC_CMD_SET_ASSIST_PRIORITY;
         TNAVC_assist_src_priority_set* pClientPriority = reinterpret_cast<TNAVC_assist_src_priority_set*>(pPayload);

         // enable only CUSTOM_AGPS_PROVIDER1
         for (int i = 0; i < MAX_ASSIST_PROVIDER; i++)
         {
            pClientPriority[i].eAssistSrcType = static_cast<ENAVC_assist_src_type>(i);  // ENAVC_assist_src_type; enum
            pClientPriority[i].assist_src_priority = TIGPSDriver::ASSIST_PRIORITY_DISABLE;
         }
         
         pClientPriority[CUSTOM_AGPS_PROVIDER1].assist_src_priority = TIGPSDriver::ASSIST_PRIORITY_ENABLE;
      }
      break;

      /* NAVC_CMD_QOP_UPDATE */
      case TIGPSDriver::CMD_NAV_SET_MODE:
      {
         (*pcode) = NAVC_CMD_QOP_UPDATE;
         TNAVC_updateFixRate* pUpdateFixRate = reinterpret_cast<TNAVC_updateFixRate*>(pPayload);
         memset(pUpdateFixRate, 0, sizeof(TNAVC_updateFixRate) );

         gpspositionmode* pmode = reinterpret_cast<struct _gpspositionmode*>(pmsg->pPayload);

         // Converts fix_frequency from milli-second to second. I guess NAV expects it in secs, but Android provides in msec
         pUpdateFixRate->update_rate = (pmode->fix_frequency == 0) ? TIGPSDriver::LOCATION_FIX_FREQUENCY : pmode->fix_frequency / 1000; 
         pUpdateFixRate->loc_sessID  = m_sessionID;
      }
      break;

      /* SUPLC_CMD_SET_MODE */
      case TIGPSDriver::CMD_SUPL_SET_MODE:
      {
         (*pcode) = SUPLC_CMD_SET_MODE;
         TSUPLC_FixMode* p_locationFixMode = reinterpret_cast<TSUPLC_FixMode*>(pPayload);
         memset(p_locationFixMode, 0, sizeof(TSUPLC_FixMode) );

         gpspositionmode* pmode = reinterpret_cast<struct _gpspositionmode*>(pmsg->pPayload);

         if (pmode->mode == GPS_POSITION_MODE_MS_BASED)
            p_locationFixMode->e_locFixMode = SUPLC_FIXMODE_MSBASED;

         else if (pmode->mode == GPS_POSITION_MODE_MS_ASSISTED)
            p_locationFixMode->e_locFixMode = SUPLC_FIXMODE_MSASSISTED;

         else
            p_locationFixMode->e_locFixMode = SUPLC_FIXMODE_AUTONOMOUS;

         p_locationFixMode->u16_fixFrequency = (pmode->fix_frequency == 0) ? static_cast<unsigned int>(TIGPSDriver::LOCATION_FIX_FREQUENCY) : static_cast<unsigned int>(pmode->fix_frequency / 1000);
      }
      break;

      /* NAVC_CMD_INJECT_ASSISTANCE --  GPSC_ASSIST_TIME */
      case GPSCommand::GPS_INJ_TIME:
      {
         (*pcode) = NAVC_CMD_INJECT_ASSISTANCE;
         TNAVC_cmdParams* pNavcCmd = reinterpret_cast<TNAVC_cmdParams*>(pPayload);
         T_GPSC_time_assist* pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.time_assist;
         gpstimeassist* pta = reinterpret_cast<struct _gpstimeassist*>(pmsg->pPayload);

         memset(pNavcCmd, 0, sizeof(TNAVC_cmdParams ) );
         pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_TIME;

         pAssist->gps_msec      = pta->gps_msec;
         pAssist->gps_week      = pta->gps_week ; // Actual week with roll over added
         pAssist->time_unc      = pta->unc_msec;  
         pAssist->sub_ms        = 0;
         pAssist->time_accuracy = GPSC_TIME_ACCURACY_COARSE;

         m_assistState = GPSCommand::GPS_INJ_TIME;
      }
      break;

      /* NAVC_CMD_INJECT_ASSISTANCE --  GPSC_ASSIST_EPH */
      case GPSCommand::GPS_INJ_RAW_EPH:
      {
         (*pcode) = NAVC_CMD_INJECT_ASSISTANCE;
         TNAVC_cmdParams* pNavcCmd = reinterpret_cast<TNAVC_cmdParams*>(pPayload);
         AGPSEphemeris* pe = reinterpret_cast<AGPSEphemeris*>(pmsg->pPayload);

         memset(pNavcCmd, 0, sizeof(TNAVC_cmdParams ) );
         pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_EPH;

         // we can dump directly because it is raw!
         T_GPSC_ephemeris_assist* pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.ephemeris_assist[0];
         memcpy(pAssist, pe->getrawephemeris(), sizeof(struct _gpsrawephemeris) );

         // end the 1st entry
         pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.ephemeris_assist[1];
         pAssist->svid = 0xFF;

         m_assistState = GPSCommand::GPS_INJ_EPHEMERIS;
      }
      break;

      /* NAVC_CMD_INJECT_ASSISTANCE --  GPSC_ASSIST_EPH */
      case GPSCommand::GPS_INJ_EPHEMERIS:
      {
         (*pcode) = NAVC_CMD_INJECT_ASSISTANCE;
         TNAVC_cmdParams* pNavcCmd = reinterpret_cast<TNAVC_cmdParams*>(pPayload);
         AGPSEphemeris* pe = reinterpret_cast<AGPSEphemeris*>(pmsg->pPayload);

         // now populate all in a loop, setting last SVID (if there is room) to 0xFF
         unsigned int iSVs = pe->getNumSVs();
         T_GPSC_ephemeris_assist* pAssist = 0;

         memset(pNavcCmd, 0, sizeof(TNAVC_cmdParams ) );
         pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_EPH;

         for (unsigned int cnt = 0; cnt < iSVs; cnt++)
         {
            gpsephemeris* peph = pe->getEphemeris(cnt);
            pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.ephemeris_assist[cnt];

            pAssist->svid               = peph->svid;

            pAssist->iode               = EPH_IODE_2_ICD(peph->iode);
            pAssist->ephem_crs          = EPH_CRS_2_ICD(peph->crs);
            pAssist->ephem_delta_n      = EPH_DELTAN_2_ICD(peph->deltan);
            pAssist->ephem_cuc          = EPH_CUC_2_ICD(peph->cuc);
            pAssist->ephem_m0           = EPH_M0_2_ICD(peph->m0);
            pAssist->ephem_e            = EPH_ECC_2_ICD(peph->ecc);
            pAssist->ephem_a_power_half = EPH_SQRTA_2_ICD(peph->sqrta);
            pAssist->ephem_omega_a0     = EPH_OMEGA_2_ICD(peph->OMEGA);
            pAssist->ephem_cus          = EPH_CUS_2_ICD(peph->cus);
            pAssist->ephem_toe          = EPH_TOE_2_ICD(peph->toe);
            pAssist->ephem_cic          = EPH_CIC_2_ICD(peph->cic);
            pAssist->ephem_cis          = EPH_CIS_2_ICD(peph->cis);
            pAssist->ephem_i0           = EPH_I0_2_ICD(peph->i0);
            pAssist->ephem_w            = EPH_W_2_ICD(peph->w);
            pAssist->ephem_omega_adot   = EPH_OMEGADOT_2_ICD(peph->omega_dot);
            pAssist->ephem_crc          = EPH_CRC_2_ICD(peph->crc);
            pAssist->ephem_idot         = EPH_IDOT_2_ICD(peph->idot);
            pAssist->ephem_code_on_l2   = EPH_L2CODE_2_ICD(peph->l2code);
            pAssist->ephem_ura          = EPH_ACCURACY_2_ICD(peph->accuracy);
            pAssist->ephem_svhealth     = EPH_HEALTH_2_ICD(peph->health);
            pAssist->ephem_tgd          = EPH_TGD_2_ICD(peph->tgd);
            pAssist->ephem_iodc         = EPH_IODC_2_ICD(peph->iodc);
            pAssist->ephem_toc          = EPH_TOC_2_ICD(peph->toc);
            pAssist->ephem_af2          = EPH_CDRIFTRATE_2_ICD(peph->cdrift_rate);
            pAssist->ephem_af1          = EPH_CDRIFT_2_ICD(peph->cdrift);
            pAssist->ephem_af0          = EPH_CBIAS_2_ICD(peph->cbias);
            pAssist->ephem_predicted    = FALSE;    // decoded from sky, not predicted
            pAssist->ephem_predSeedAge  = 0;        // since ephemeris is not predicted (?)
            pAssist->ephem_week         = EPH_GPSWEEK_2_ICD(peph->gpsweek);

         }

         // set svid of following entry to 0xff, if less than GPS_MAX_SVS; this tells server to stop
         if (iSVs < GPS_MAX_SVS)
         {
            pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.ephemeris_assist[iSVs];
            pAssist->svid = 0xFF;
         }

         m_assistState = GPSCommand::GPS_INJ_EPHEMERIS;
      }
      break;

      /* NAVC_CMD_INJECT_ASSISTANCE --  GPSC_ASSIST_ALMANAC */
      case GPSCommand::GPS_INJ_ALMANAC:
      {
         (*pcode) = NAVC_CMD_INJECT_ASSISTANCE;
         TNAVC_cmdParams* pNavcCmd = reinterpret_cast<TNAVC_cmdParams*>(pPayload);
         AGPSAlmanac* pa = reinterpret_cast<AGPSAlmanac*>(pmsg->pPayload);

         // now populate all in a loop, setting last SVID (if there is room) to 0xFF
         unsigned int iSVs = pa->getNumSVs();
         T_GPSC_almanac_assist* pAssist = 0;

         memset(pNavcCmd, 0, sizeof(TNAVC_cmdParams ) );
         pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_ALMANAC;

         for (unsigned int cnt = 0; cnt < iSVs; cnt++)
         {
            gpsalmanac* palm = pa->getAlmanac(cnt);
            pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.almanac_assist[cnt];

            pAssist->svid               = palm->svid;
            pAssist->almanac_svhealth   = palm->health;
            pAssist->almanac_week       = palm->week;

            pAssist->almanac_toa          = ALM_TOA_2_ICD(palm->toa);
            pAssist->almanac_e            = ALM_ECC_2_ICD(palm->ecc);                       
            pAssist->almanac_ksii         = ALM_I0_2_ICD(palm->i0);           
            pAssist->almanac_omega_dot    = ALM_OMEGADOT_2_ICD(palm->omegadot);    
            pAssist->almanac_a_power_half = ALM_SQRTA_2_ICD(palm->sqrta);                              
            pAssist->almanac_omega0       = ALM_OMEGA0_2_ICD(palm->omega0);                            
            pAssist->almanac_w            = ALM_W_2_ICD(palm->w);                
            pAssist->almanac_m0           = ALM_M0_2_ICD(palm->m0);                        
            pAssist->almanac_af0          = ALM_AF0_2_ICD(palm->af0);                 
            pAssist->almanac_af1          = ALM_AF1_2_ICD(palm->af1);
         }

         // set svid of following entry to 0xff, if less than GPS_MAX_SVS; this tells server to stop
         if (iSVs < GPS_MAX_SVS)
         {
            pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.almanac_assist[iSVs];
            pAssist->svid = 0xFF;
         }

         m_assistState = GPSCommand::GPS_INJ_ALMANAC;
      }
      break;

     
      /* NAVC_CMD_INJECT_ASSISTANCE --  GPSC_ASSIST_POSITION */
      case GPSCommand::GPS_INJ_POSITION:
      {
         (*pcode) = NAVC_CMD_INJECT_ASSISTANCE;
         TNAVC_cmdParams* pNavcCmd = reinterpret_cast<TNAVC_cmdParams*>(pPayload);
         T_GPSC_position_assist* pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.position_assist;
         GpsLocation* pLoc = reinterpret_cast<GpsLocation*>(pmsg->pPayload);

         memset(pNavcCmd, 0, sizeof(TNAVC_cmdParams ) );
         pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_POSITION;


         pAssist->d_assist_ToaSec = 0;   // according to TI can leave at 0, if they really know what they are talking about
         pAssist->position_uncertainty  = 15 /*KM*/ * 1000 * 10;   // TI says so, big ?

         if ( (pLoc->flags & GPS_LOCATION_HAS_LAT_LONG) == GPS_LOCATION_HAS_LAT_LONG)
         {
             // have to convert from platform types into MCP types. Heaven help us
             pAssist->longitude = static_cast<McpS32>(pLoc->longitude * TIGPSDriver::LONG_SCALE);
             if (pLoc->latitude < 0)
             {
                 pAssist->latitude_sign  = 1;
                 McpS32 lat = static_cast<McpS32>(pLoc->latitude * TIGPSDriver::LAT_SCALE);
                 pAssist->latitude = static_cast<McpS32>(lat & 0x007FFFFF); //signed extension
             }
             else
             {
                 pAssist->latitude_sign = 0;
                 pAssist->latitude = static_cast<McpU32>(pLoc->latitude * TIGPSDriver::LAT_SCALE);
             }

         }

         if ( (pLoc->flags & GPS_LOCATION_HAS_ALTITUDE) == GPS_LOCATION_HAS_ALTITUDE)
         {
             // this conversion code is directly from TI email; ? around it and test
             if (pLoc->altitude < 0)
             {       
                pAssist->altitude_sign = 1;   // depth  
                signed short x_Ht = static_cast<signed short>(0 - (static_cast<unsigned short>(pLoc->altitude) & 0x7FFF) );
                pAssist->altitude = static_cast<unsigned short>(-1 * x_Ht);
             }
             else
             {
                pAssist->altitude_sign = 0; // height
                pAssist->altitude = static_cast<unsigned short>(pLoc->altitude);
             }
         }

         m_assistState = GPSCommand::GPS_INJ_POSITION;
      }
      break;

   }
}


/* NAVL server start/stop */
GPSStatus TIGPSDriver::set_navl_server(NavlState state)
{
    GPS_LOG_TRACE("Enter\n");
    int fd = -1;

    if (state == NAVL_START)
    {
        if (property_set(TIGPSDriver::CTL_START, TIGPSDriver::NAVL_SERVER_NAME) < 0)
        {
            GPS_LOG_INF("Navl Server has already started!\n");
        }
        else
        {
            /* Allow Navl server to init */
           sleep(3);

           // Start RX Network
           set_rxn_intapp(RXN_START);
        }

    }
    else if (state == NAVL_STOP)
    {
        /* Open the pipe for reading */
        fd = ::open(TIGPSDriver::NAVL_FIFO, O_CREAT |O_RDWR| O_TRUNC );
        if (fd < 0)
        {
            GPS_LOG_ERR("FIFO Opening FAILED !!!");
            return GPSStatusCode::GPS_E_DRIVER_STOP;
        }

        // Write exit
        if ( write(fd, TIGPSDriver::NAVL_EXIT_CMD, strlen(TIGPSDriver::NAVL_EXIT_CMD)) < 0 )
        {
            GPS_LOG_ERR("Write operation FAILED !!!");
            ::close(fd);

            return GPSStatusCode::GPS_E_DRIVER_STOP;
        }

        ::close(fd);

    }

    GPS_LOG_TRACE("OK\n");
    return GPSStatusCode::GPS_SUCCESS;
}

/* RXN_IntApp start/stop */
void TIGPSDriver::set_rxn_intapp(RxnState state) {
    GPS_LOG_TRACE("set_rxn_intapp");
    if (state == RXN_START) {
        if (property_set(TIGPSDriver::CTL_START, TIGPSDriver::RXN_INTAPP_NAME) < 0) {
            GPS_LOG_ERR("Failed to start rxn_intapp!");
            return;
        }
        GPS_LOG_INF("Start success");
    } else if (state == RXN_STOP) {
        if (property_set(TIGPSDriver::CTL_STOP, TIGPSDriver::RXN_INTAPP_NAME) < 0) {
            GPS_LOG_ERR("Failed to stop rxn_intapp!");
            return;
        }
        GPS_LOG_INF("Stop success");
    } else {
        GPS_LOG_ERR("Invalid option: %d", state);
    }
}

/**
 * Function:        connectWithMcpfSocket
 *   Connects MCP Server Socket for Driver RPC */
GPSStatus TIGPSDriver::connect_server ()
{
    struct sockaddr_un serverAddress;       /* Internet Address of Server. */
    struct hostent* p_host = 0;             /* The host (server) info. */

    GPS_LOG_TRACE("Enter\n");

    /* Obtain host information. */
    p_host = gethostbyname((char*)TIGPSDriver::MCPF_HOST_NAME);
    if (!p_host)
    {
        GPS_LOG_ERR("Error obtaining host information\n");
        return GPSStatusCode::GPS_E_DRIVER_CONNECT;
    }

    /* Clear the structure. */
    ::memset( &serverAddress, 0, sizeof(serverAddress) );

    serverAddress.sun_family = AF_UNIX;
    strcpy(serverAddress.sun_path, TIGPSDriver::SOC_NAME_4121);

    /* Create a new socket. */
    m_socket = ::socket(AF_UNIX, SOCK_STREAM, 0);

    if (m_socket < 0)
    {
        GPS_LOG_ERR("Socket Creation Failure\n");
        return GPSStatusCode::GPS_E_DRIVER_CONNECT;
    }

    /* Connect with MCPF. */
    GPS_LOG_TRACE("Connecting with: %s\n", TIGPSDriver::SOC_NAME_4121);
    int retVal = ::connect(m_socket,
                     (struct sockaddr *)&serverAddress,
                     sizeof(serverAddress) );

    if (retVal < 0)
    {
        GPS_LOG_ERR("Connection with [%s] failed. (Error [%d])\n", TIGPSDriver::SOC_NAME_4121, -errno);
        ::close(m_socket); m_socket = TIGPSDriver::INVALID_SOCKET;

        return GPSStatusCode::GPS_E_DRIVER_CONNECT;
    }

    GPS_LOG_TRACE("OK\n");
    return GPSStatusCode::GPS_SUCCESS;
}


/* Processing Function: We don't know or care if we are running in
   thread or not. When we process each NAV response, we 
   hand it off to GpsResponseHandler and block there */
void* TIGPSDriver::TiGpsResponseHandler(void* contextPtr)
{
    TIGPSDriver* pThis = (TIGPSDriver*)contextPtr;
    GPSStatus retVal = GPSStatusCode::GPS_SUCCESS;

    hostSocket_header_t s_msgHdr;
    unsigned char* p_readBuffer = NULL;

    GPS_LOG_INF("+++ TIGPSDriver: NAVL Thread Receiver is starting ... +++\n");

    pThis->pingWatchdog(CTRL_TD_PING);

    memset(&s_msgHdr, 0, sizeof(hostSocket_header_t) );

    // central processing loop. Tests exit signal on top, then blocking read on connected socket
    // should blocking read fail, this probably means socket has been disconnected. It does not terminate
    // on its own, instead keeps spinning until exit has been signalled by whoever is using services of this object
    while(!pThis->m_pResponseHandler->getExitSignal(CONTROL_THREAD))
    {
        // Read Header 
        int s32_bytesRead = pThis->read_buffer(pThis->m_socket, reinterpret_cast<unsigned char*>(&s_msgHdr), sizeof(hostSocket_header_t) );

        if (s32_bytesRead < 0)
        {
            GPS_LOG_ERR("+++ Failed to read buffer from NAV Server. Attempting to recover... +++\n");
            break;
        }
        else if (s32_bytesRead == sizeof(hostSocket_header_t) )
        {
           pThis->m_gpsrsp.opcode = s_msgHdr.opCode;
           if (s_msgHdr.payloadLen > 0)
           {
              // Read Message Data if any. We simply check if we have enough room, in order to avoid
              // constant memory fragmentation. Memory allocation failure here we treat as unrecoverable
              // (When this fails, likely everything else will)
              if (pThis->m_gpsrsp.i_msglen < s_msgHdr.payloadLen)
              {
                 delete [] pThis->m_gpsrsp.pPayload; pThis->m_gpsrsp.pPayload = 0;
                 pThis->m_gpsrsp.pPayload = new unsigned char[s_msgHdr.payloadLen];
                 if (!pThis->m_gpsrsp.pPayload)
                 {
                    GPS_LOG_ERR("Memory Allocation Error! Processing Function is terminating...\n");
                    return 0;
                 }
              }

              pThis->m_gpsrsp.i_msglen = s_msgHdr.payloadLen;   // sets new payload size
              s32_bytesRead = pThis->read_buffer(pThis->m_socket, pThis->m_gpsrsp.pPayload, pThis->m_gpsrsp.i_msglen);

              if (s32_bytesRead == (int)pThis->m_gpsrsp.i_msglen)
                 pThis->handleGps (&pThis->m_gpsrsp);
              else
              {
                 GPS_LOG_ERR("Not processing NAVD response [Code = %d]. Received # of bytes [%d] is less than required [%d]\n",
                    pThis->m_gpsrsp.opcode, s32_bytesRead, pThis->m_gpsrsp.i_msglen);
              }
            }  // if (s_msgHdr.payloadLen > 0)

        } // else if (s32_bytesRead == sizeof(hostSocket_header_t) )

    }

    // Signal that the CONTROL_THREAD is exiting
    pThis->m_pResponseHandler->exit(CONTROL_THREAD);

    GPS_LOG_INF("TIGPSDriver: NAVL Control Thread is exiting\n");
    return 0;
}



// Received Response Dispatcher override
void TIGPSDriver::handleGps (gpsmessage* pmsg)
{
   TNAVC_evt*  pEvt = reinterpret_cast<TNAVC_evt*>(pmsg->pPayload);

   // check what we got. If payload is NULL, we can't really do anything; (I don't know if this is still ok
   // under some scenarios). At any rate log and go back to receiver thread
   if (pEvt == NULL)
   {
      GPS_LOG_INF ("%s: NAVC Event: [0x%x], Payload is NULL. No further processing \n", __FUNCTION__, pmsg->opcode);
      return;
   }

   // check the result
   if (pEvt->eResult != RES_OK)
   {
      GPS_LOG_ERR("NAVC Event [0x%x], Completed Command: [0x%x], Error Result [0x%x]\n",
          pmsg->opcode, pEvt->eComplCmd, pEvt->eResult);
      return;
   }

   // process command completion
   if (pmsg->opcode == NAVC_EVT_CMD_COMPLETE)
   {
      GPS_LOG_INF ("+++ NAVC_EVT_CMD_COMPLETE Received. Command: [0x%x]. Result: %d +++\n", pEvt->eComplCmd, pEvt->eResult); 
      GPSStatus commandResult = (pEvt->eResult == RES_OK) ? GPSStatusCode::GPS_SUCCESS : GPSStatusCode::GPS_E_INVALID_RESP;  // TODO: Meaninguful mapping!

      switch (pEvt->eComplCmd)
      {
         case NAVC_CMD_START:
         {
            m_pResponseHandler->commandCompleted (GPSCommand::GPS_START, commandResult);
         }
         break;

         case NAVC_CMD_STOP:
         {
            m_pResponseHandler->commandCompleted (GPSCommand::GPS_STOP, commandResult);
         }
         break;

         case NAVC_CMD_REQUEST_FIX:
         {
            m_pResponseHandler->commandCompleted (GPSCommand::GPS_FIX_BEGIN, commandResult);
         }
         break;

         case NAVC_CMD_STOP_FIX:
         {
            // issue command to save assistance, before calling back into Android
            // This was original TI implementation, not sure if this is required (appears non-standard)
            gpsmessage cmd;
            cmd.opcode = GPSCommand::GPS_SAVE_ASSIST;
            GPSStatus stat = this->sendCommand(&cmd);
            if (stat != GPSStatusCode::GPS_SUCCESS)
               GPS_LOG_ERR("Could not save assistance during GPS Session tear-down!\n"); 

            m_pResponseHandler->commandCompleted (GPSCommand::GPS_FIX_END, commandResult);
         }
         break;

         case NAVC_CMD_SAVE_ASSISTANCE:
         {
             m_pResponseHandler->commandCompleted (GPSCommand::GPS_SAVE_ASSIST, commandResult);
         }
         break;

         case NAVC_CMD_BEGIN_ASSISTANCE:
         {
             m_pResponseHandler->commandCompleted(GPSCommand::GPS_BEGIN_ASSIST, commandResult);
         }
         break;

         case NAVC_CMD_DELETE_ASISTANCE:
         {
             m_pResponseHandler->commandCompleted (GPSCommand::GPS_DEL_ASSIST, commandResult);
         }
         break;

         case NAVC_CMD_COMPLETE_ASSISTANCE:
         {
             m_pResponseHandler->commandCompleted (GPSCommand::GPS_END_ASSIST, commandResult);
         }
         break;

         case NAVC_CMD_INJECT_ASSISTANCE:
         {
             m_pResponseHandler->commandCompleted(m_assistState, commandResult);
         }
         break;

         default:
             GPS_LOG_INF("%s: Not reporting completion of command: [0x%x]\n",
                __FUNCTION__,  pEvt->eComplCmd);
      }
      
   }
   

   else if (pmsg->opcode == NAVC_EVT_POSITION_FIX_REPORT)
   {
   /*    GPS_LOG_TRACE ("+++ NAVC_EVT_POSITION_FIX_REPORT Received. Type: [0x%x] +++\n", 
                  (&pEvt->tParams.tLocFixReport)->ctrl_loc_fix_response_union); */

       switch ( (&pEvt->tParams.tLocFixReport)->ctrl_loc_fix_response_union)
       {
          case GPSC_RESULT_NMEA:
          {
             this->processNmeaData(pmsg);
          }
          break;

          case GPSC_RESULT_PROT_MEASUREMENT:
          {
             this->processProtMeasurement ();
          }
          break;

          case GPSC_RESULT_RAW_POSITION:
          {
             this->processRawPosition ();
          }
          break;

          case GPSC_RESULT_PROT_POSITION:
          {
             this->processProtPosition ();
          }
          break;

          case NAVC_EVT_ASSISTANCE_REQUEST:
          {
           //  GPS_LOG_INF("+++ %s: Assistance Request Event Received inside Fix Report! +++\n", __FUNCTION__);
             this->processAssistanceRequest (&m_gpsrsp);
          }
          break;

          default:
             GPS_LOG_INF("%s: Not processing Position Fix Report Event; Response Type: [0x%x]\n",
                __FUNCTION__,  (&pEvt->tParams.tLocFixReport)->ctrl_loc_fix_response_union);
       }

        // Notify GPS watchdog that we're still alive. NOOP if watchdog is not enabled.
        this->pingWatchdog(LOCATION_WD_PING);
   }
   else if ( (pmsg->opcode == NAVC_EVT_ASSIST_DEL_RES) )
   {
       m_pResponseHandler->handleStatus(GPSStatusCode::STATUS_GPS_DEL_ASSIST);
   }
   else if ( (pmsg->opcode == NAVC_EVT_ASSISTANCE_REQUEST) )
   {
      //GPS_LOG_INF("+++ %s: Assistance Request Event Received! +++\n", __FUNCTION__);
      this->processAssistanceRequest (&m_gpsrsp);
   }
   else
       GPS_LOG_ERR("NAV Event [0x%x] not handled\n", pmsg->opcode);


} 


/* Assistance Request Handler */
void TIGPSDriver::processAssistanceRequest (gpsmessage* pmsg)
{

     // extract mandatory bitmask
     GPSStatus mandatory_assist_bitmask = 0;
     TNAVC_evt*  pEvt = reinterpret_cast<TNAVC_evt*>(pmsg->pPayload);

     if ( (pEvt->tParams.tAssistReq.uAssistBitmapMandatory & GPSC_REQ_ALMANAC) == GPSC_REQ_ALMANAC) 
     {
      //  GPS_LOG_INF("+++ %s: Almanac Requested +++\n", __FUNCTION__);  
        mandatory_assist_bitmask |= ReconAGPSType::REQ_ALMANAC;
     }

     if ( (pEvt->tParams.tAssistReq.uAssistBitmapMandatory & GPSC_REQ_LOC) == GPSC_REQ_LOC) 
     {
      //  GPS_LOG_INF("+++ %s: Location Requested +++\n", __FUNCTION__);  
        mandatory_assist_bitmask |= ReconAGPSType::REQ_POSITION;
     }

     if ( (pEvt->tParams.tAssistReq.uAssistBitmapMandatory & GPSC_REQ_TIME) == GPSC_REQ_TIME) 
     {
       // GPS_LOG_INF("+++ %s: Time Requested +++\n", __FUNCTION__);  
        mandatory_assist_bitmask |= ReconAGPSType::REQ_TIME;
     }

     if ( (pEvt->tParams.tAssistReq.uAssistBitmapMandatory & GPSC_REQ_NAV) == GPSC_REQ_NAV) 
     {
       // GPS_LOG_INF("+++ %s: Ephemeris Requested +++\n", __FUNCTION__);  
        mandatory_assist_bitmask |= ReconAGPSType::REQ_EPHEMERIS;
     }

     // *** TESTING, REMOVE ***
     //mandatory_assist_bitmask = ReconAGPSType::REQ_EPHEMERIS;

     // and send it to handler
     m_pResponseHandler->handleStatus(GPSStatusCode::STATUS_GPS_REQUEST_ASSIST, mandatory_assist_bitmask);
}

/* NMEA Data Handler Override */
void TIGPSDriver::processNmeaData(gpsmessage* pmsg)
{
   TNAVC_evt*  pEvt = reinterpret_cast<TNAVC_evt*>(pmsg->pPayload);
   T_GPSC_loc_fix_response*       loc_fix_response = &pEvt->tParams.tLocFixReport;
   T_GPSC_loc_fix_response_union* p_LocFixRespUnion = &loc_fix_response->loc_fix_response_union;
   T_GPSC_nmea_response* p_nmeaPos = &p_LocFixRespUnion->nmea_response;

   char* temp_ptr = reinterpret_cast<char*>(&(p_nmeaPos->nmea_string[0]) );
   char* p_token = strtok(temp_ptr, "$");

   if (!p_token)
   {
      GPS_LOG_ERR("Invalid NMEA buffer received (No leading '$' found). Data: [%s]\n", p_nmeaPos->nmea_string);
      return;
   }

   // send message to response handler
   gpsmessage msg;
   msg.opcode = GPSStatusCode::DATA_NMEA;
   msg.pPayload = reinterpret_cast<unsigned char*>(p_token - 1);
   msg.i_msglen = strlen(p_token - 1);

   m_pResponseHandler->handleData(&msg);   // must complete sync

}

/* PROT Measurement Data Handler */
void TIGPSDriver::processProtMeasurement ()
{
   TNAVC_evt*  pEvt = reinterpret_cast<TNAVC_evt*>(m_gpsrsp.pPayload);
   T_GPSC_loc_fix_response*       loc_fix_response = &pEvt->tParams.tLocFixReport;
   T_GPSC_loc_fix_response_union* p_LocFixRespUnion = &loc_fix_response->loc_fix_response_union;

   T_GPSC_prot_measurement* p_ProtMeas = &p_LocFixRespUnion->prot_measurement;
   T_GPSC_sv_measurement*   p_measurement = (T_GPSC_sv_measurement *)&p_ProtMeas->sv_measurement;

   // resets cached Space Vehicle Info
   unsigned int used_in_fix_mask = m_gpsSvStatus.used_in_fix_mask;
   memset(&m_gpsSvStatus, 0, sizeof(GpsSvStatus));
   m_gpsSvStatus.size = sizeof(GpsSvStatus);
   m_gpsSvStatus.used_in_fix_mask = used_in_fix_mask;

   /*** ============= PRN and SNR Values BEGIN ================== ***/
   McpS32 index = 0;
   McpS32 index_SNR = 0;

   // Pseudo Random Noise and Signal to Noise Ratio 
   for (index = 0; ((index < p_ProtMeas->c_sv_measurement) && (index < 32)); index++)
   {
        // Space Vehicle ID 0 - 63 represents SV PRN 1 - 64 
        if ( (static_cast<FLT>(p_measurement[index].snr) / 10) != 0) 
        {
            m_gpsSvStatus.sv_list[index_SNR].prn = p_measurement[index].svid + 1;

            // SV signal to noise ratio, unsigned, 0.1 dB per bit 
            m_gpsSvStatus.sv_list[index_SNR].snr = static_cast<FLT> (p_measurement[index].cno) / 10;
            m_gpsSvStatus.sv_list[index_SNR].elevation = static_cast<FLT> (p_measurement[index].elevation) * C_LSB_ELEV_MASK;
            m_gpsSvStatus.sv_list[index_SNR].azimuth = static_cast<FLT> (p_measurement[index].azimuth) * C_LSB_AZIM_MASK;

            // SV signal to noise ratio, unsigned, 0.1 dB per bit
            index_SNR++;
        }
    }
    m_gpsSvStatus.num_svs = index_SNR;
  
    /*** ============= PRN and SNR Values END ================== ***/
    // Note: Number of SV's used in fix mask is filled in processRawPosition
    //   p_outGpsSVStatus->used_in_fix_mask = gp_gpsalObject->Svstatus.used_in_fix_mask;

    m_gpsSvStatus.ephemeris_mask = ~(p_ProtMeas->eph_availability_flags);
    m_gpsSvStatus.ephemeris_mask |= ~(p_ProtMeas->pred_eph_availability_flags);
    m_gpsSvStatus.almanac_mask = ~(p_ProtMeas->alm_availability_flags);
  
    // Send data to response handler
    gpsmessage msg;
    msg.opcode = GPSStatusCode::DATA_SV;
    msg.pPayload = reinterpret_cast<unsigned char*>(&m_gpsSvStatus);
    msg.i_msglen = sizeof(GpsSvStatus);

    m_pResponseHandler->handleData(&msg);   // must complete sync!

}

/* RAW Position Data Handler */
void TIGPSDriver::processRawPosition ()
{
    TNAVC_evt*  pEvt = reinterpret_cast<TNAVC_evt*>(m_gpsrsp.pPayload);
    T_GPSC_loc_fix_response*       loc_fix_response = &pEvt->tParams.tLocFixReport;
    T_GPSC_loc_fix_response_union* p_LocFixRespUnion = &loc_fix_response->loc_fix_response_union;

    T_GPSC_raw_position* p_RawPosition = &p_LocFixRespUnion->raw_position;
    T_GPSC_position*     gpsSvStatus = &p_RawPosition->position;
    T_GPSC_toa*          p_toa = &p_RawPosition->toa;

    // reset location data; as we don't report now, but in prot_measurement
    memset(&m_gpsLocation, 0, sizeof(GpsLocation) );
    m_gpsLocation.size = sizeof(GpsLocation);

    // calculate # of space vehicles used in Fix 
    for (int index = 0; (index < gpsSvStatus->c_position_sv_info) && (index < GPSC_RAW_MAX_SV_REPORT); index++)
    {
        const T_GPSC_position_sv_info* p_positionSvInfo = &gpsSvStatus->position_sv_info[index];
        m_gpsSvStatus.used_in_fix_mask |= (1<< (p_positionSvInfo->svid ));

        //GPS_LOG_INF("SV Used in Fix: [%d]. Mask: %d\n", p_positionSvInfo->svid, m_gpsSvStatus.used_in_fix_mask);
    }

    // calculate speed
    float x_VelEast_mps  = gpsSvStatus->velocity_east * TIGPSDriver::C_LSB_EAST_VEL;           // East Velocity
    float x_VelNorth_mps = gpsSvStatus->velocity_north * TIGPSDriver::C_LSB_NORTH_VEL;         // North Velocity
    float x_VelVert_mps  = gpsSvStatus->velocity_vertical * TIGPSDriver::C_LSB_VERTICAL_VEL;   // Vertical Velocity

    m_gpsLocation.speed  = sqrt((x_VelEast_mps * x_VelEast_mps) + (x_VelNorth_mps * x_VelNorth_mps));  // horizontal speed
    m_gpsLocation.flags |= GPS_LOCATION_HAS_SPEED;

    // calculate accuracy  (Original TI comment bellow)
    /* Vertical, horizontal, speed and heading accuracy can be calculated from the
     * information available in NAVC. Not sure which data has to be given to android application.
     * For now, consider horizontal accuracy */
    float f_EastUnc = gpsSvStatus->uncertainty_east * TIGPSDriver::C_LSB_EAST_ER;
    float f_NorthUnc = gpsSvStatus->uncertainty_north * TIGPSDriver::C_LSB_NORTH_ER;
    m_gpsLocation.accuracy = sqrt( (f_EastUnc * f_EastUnc) + (f_NorthUnc * f_NorthUnc) );
    m_gpsLocation.flags |= GPS_LOCATION_HAS_ACCURACY;
    
    // Bearing and Timestamp calculation is pending for when we report
    m_gpsLocation.bearing = static_cast<float> (gpsSvStatus->heading_true / TIGPSDriver::HEADING_SCALE);
    m_gpsLocation.flags |= GPS_LOCATION_HAS_BEARING;
    m_gpsLocation.msl_altitude = gpsSvStatus->altitude_msl;
}

/* Low speed processing */
void TIGPSDriver::postProcessLocation() {
    int64_t avgTime = 0;
    double avgLat = 0, avgLon = 0;
    float distance = 0, posSpeed = 0, compositeSpeed = 0, deltaTime = 0, dopSpeedCoeff = 0, dopSpeed = m_gpsLocation.speed;

    // Convert max and min speeds from km/h to m/s
    float maxSpeed = (float)MAX_SPEED * 1000.0 * (1.0/60.0) * (1.0/60.0);
    float minSpeed = (float)MIN_SPEED * 1000.0 * (1.0/60.0) * (1.0/60.0);

    // Add current reading to our location array
    GpsLocation *pLoc = &m_gpsLocationArray[m_gpsLocationPtr];
    memcpy(pLoc, &m_gpsLocation, sizeof(GpsLocation));
    m_gpsLocationPtr++;

    if (m_gpsLocationPtr == LOCATION_ARRAY_SIZE) {
        m_gpsUsePosSpeed = true;
        m_gpsLocationPtr %= LOCATION_ARRAY_SIZE;
    }

    if (m_gpsUsePosSpeed) {
        GPS_LOG_TRACE("dopSpeed: %f, timestamp: %f", dopSpeed, (double)m_gpsLocation.timestamp);

        // Calculate dopSpeed coefficient
        dopSpeedCoeff = (dopSpeed - minSpeed) / (maxSpeed - minSpeed);

        // Ensure the coefficient is within range [0,1]
        if (dopSpeedCoeff < 0) {
            dopSpeedCoeff = 0;
        } else if (dopSpeedCoeff > 1) {
            dopSpeedCoeff = 1;
        }

        // Take the average of LOCATION_ARRAY_SIZE previous readings
        for (int i = 0; i < LOCATION_ARRAY_SIZE; i++) {
            avgLat += m_gpsLocationArray[i].latitude;
            avgLon += m_gpsLocationArray[i].longitude;
            avgTime += m_gpsLocationArray[i].timestamp;
            GPS_LOG_TRACE("       m_gpsLocationArray[%d]: %f", i, m_gpsLocationArray[i].latitude);
        }
        avgLat /= (double)LOCATION_ARRAY_SIZE;
        avgLon /= (double)LOCATION_ARRAY_SIZE;
        avgTime /= (int64_t)LOCATION_ARRAY_SIZE;

        memset(&m_gpsAvgLocation, 0, sizeof(GpsLocation));
        m_gpsAvgLocation.latitude = avgLat;
        m_gpsAvgLocation.longitude = avgLon;
        m_gpsAvgLocation.timestamp = avgTime;

        GPS_LOG_TRACE("   lat: %f, lon: %f, time: %f", avgLat, avgLon, avgTime);

        if (dopSpeedCoeff == 1) {
            // Just use dopSpeed. No need to calculate posSpeed.
            GPS_LOG_TRACE("Just using dopSpeed: %f", dopSpeed);
            compositeSpeed = dopSpeed;
        } else if (m_gpsPrevLocation.latitude != 0 || m_gpsPrevLocation.longitude != 0) {
            // Compute the distance and speed
            distance = calcDistance(m_gpsPrevLocation.latitude, m_gpsPrevLocation.longitude,
                                    m_gpsAvgLocation.latitude, m_gpsAvgLocation.longitude);
            deltaTime = (m_gpsAvgLocation.timestamp - m_gpsPrevLocation.timestamp) / 1000.0;
            posSpeed = distance/deltaTime;
            GPS_LOG_TRACE("   deltaTime: %f", deltaTime);
            GPS_LOG_TRACE("   distance: %f", distance);
            GPS_LOG_TRACE("   posSpeed: %f", posSpeed);

            // Check to see if the calculated position speed is greater than max speed. If it
            // is, it is most likely a speed spike. As a result, just use doppler speed.
            if (posSpeed > maxSpeed) {
                GPS_LOG_TRACE("posSpeed > maxSpeed [%f>%f]", posSpeed, maxSpeed);
                compositeSpeed = dopSpeed;
            } else {
                // Calculate overall speed using both position speed and doppler speed
                compositeSpeed = (1 - dopSpeedCoeff) * posSpeed + dopSpeedCoeff * dopSpeed;
            }
        }

        // Save the new average point for next time
        memcpy(&m_gpsPrevLocation, &m_gpsAvgLocation, sizeof(GpsLocation));

        GPS_LOG_TRACE("Calculated new speed: %f using coeff: %f", compositeSpeed, dopSpeedCoeff);
        m_gpsLocation.speed = compositeSpeed;
    }
}

/* Helper function to calculate distance (in meters) between two points using Haversine formula */
float TIGPSDriver::calcDistance(double lat1, double lon1, double lat2, double lon2) {
    double Radius = 6371.0;  // Radius of the earth in km (roughly)
    lat1 *= (PI / 180.0);
    lat2 *= (PI / 180.0);
    lon1 *= (PI / 180.0);
    lon2 *= (PI / 180.0);

    double dLat = lat2 - lat1;
    double dLon = lon2 - lon1;

    double a = pow(sin(dLat/2.0), 2) + cos(lat1) * cos(lat2) * pow(sin(dLon/2.0), 2);
    double b = 2.0 * atan2(sqrt(a), sqrt(1.0-a));

    return Radius * b * 1000.0;
}

/* PROT Position Data Handler */
void TIGPSDriver::processProtPosition ()
{
    TNAVC_evt*  pEvt = reinterpret_cast<TNAVC_evt*>(m_gpsrsp.pPayload);
    T_GPSC_loc_fix_response*       loc_fix_response = &pEvt->tParams.tLocFixReport;
    T_GPSC_loc_fix_response_union* p_LocFixRespUnion = &loc_fix_response->loc_fix_response_union;

    T_GPSC_prot_position*          p_RrlpPosition = &p_LocFixRespUnion->prot_position;
    T_GPSC_ellip_alt_unc_ellip*    p_RrlpEllipInfo = &p_RrlpPosition->ellip_alt_unc_ellip;

    switch (p_RrlpPosition->prot_fix_result)
    {
        case GPSC_PROT_FIXRESULT_2D:   // navc_api.h
        {
            m_gpsLocation.altitude = 0;
            m_gpsLocation.flags |= GPS_LOCATION_HAS_LAT_LONG;   // gps.h
        }
        break;

        case GPSC_PROT_FIXRESULT_3D:   // navc_api.h
        {
            /**** TODO: This is WRONG; taking altitude straight from unsigned short without conversion ****/
            if (p_RrlpEllipInfo->altitude_sign)
                m_gpsLocation.altitude = static_cast<double>(-p_RrlpEllipInfo->altitude);
            else
                m_gpsLocation.altitude = static_cast<double>(p_RrlpEllipInfo->altitude);

            m_gpsLocation.flags |= (GPS_LOCATION_HAS_LAT_LONG | GPS_LOCATION_HAS_ALTITUDE);   // gps.h
        }
        break;

        case GPSC_PROT_FIXRESULT_NOFIX:
            GPS_LOG_TRACE("%s: No fix detected\n", __FUNCTION__);
            return;
        break;

        default:
            GPS_LOG_ERR("Unrecognized Fix Result Type: [%d]\n", p_RrlpPosition->prot_fix_result);
            return;
    }

    if (p_RrlpEllipInfo->latitude_sign)
        m_gpsLocation.latitude = -static_cast<double>(p_RrlpEllipInfo->latitude) / TIGPSDriver::LAT_SCALE;
    else
        m_gpsLocation.latitude = static_cast<double>(p_RrlpEllipInfo->latitude) / TIGPSDriver::LAT_SCALE;

    m_gpsLocation.longitude = static_cast<double>(p_RrlpEllipInfo->longitude) / TIGPSDriver::LONG_SCALE;


    if ((p_RrlpEllipInfo->velocity_flag) & TIGPSDriver::HORSPEED)
    {
        m_gpsLocation.speed = p_RrlpEllipInfo->horspeed;   // ???? conversion ???
        m_gpsLocation.flags |= GPS_LOCATION_HAS_SPEED;
    }

    if ((p_RrlpEllipInfo->velocity_flag) & TIGPSDriver::BEARING)
    {
        m_gpsLocation.bearing  = static_cast<float>(p_RrlpEllipInfo->bearing) / TIGPSDriver::HEADING_SCALE;
        m_gpsLocation.flags |= GPS_LOCATION_HAS_BEARING;
    }

    if ((p_RrlpEllipInfo->velocity_flag) & TIGPSDriver::HORUNCERTSPEED)
    {
        m_gpsLocation.accuracy = static_cast<float>(p_RrlpEllipInfo->horuncertspeed);
        m_gpsLocation.flags |= GPS_LOCATION_HAS_ACCURACY;
    }

    // GpsLocation::timestamp = int64_t. p_RrlpPosition->ellip_alt_unc_ellip.utctime = double
    m_gpsLocation.timestamp = static_cast<int64_t>(p_RrlpPosition->ellip_alt_unc_ellip.utctime * TIGPSDriver::SEC_MSEC) +
      TIGPSDriver::GPS2UTC * TIGPSDriver::SEC_MSEC;

    // For low speeds, do some more processing to be more accurate
    postProcessLocation();

    //TODO: Original has some convoluted logic in respect to GpsSvStatus; looks like # of space vehicles
    //      used in fix mask can come out of order & thus he is carrying cached GpsSvStatus on class level
    //      + flag indicator whether SvStatus has been reported or not. This needs cleanup once I determine
    //      exact order of received messages
    if (m_TTFFflag == 0)
    {
        m_TTFFflag = 1;
        gpsmessage msg;
        msg.opcode = GPSStatusCode::DATA_SV;
        msg.pPayload = reinterpret_cast<unsigned char*>(&m_gpsSvStatus);
        msg.i_msglen = sizeof(GpsSvStatus);

        m_pResponseHandler->handleData(&msg);   // must complete sync!
     } 

    // Now send Full GpsPosition
    // -- Other fields are filled in GPSC_RESULT_RAW_POSITION.
    // (Order of calls: 1. GPSC_RESULT_RAW_POSITION... 2. GPSC_RESULT_PROT_POSITION)
    gpsmessage msg;
    msg.opcode = GPSStatusCode::DATA_LOCATION;
    msg.pPayload = reinterpret_cast<unsigned char*>(&m_gpsLocation);
    msg.i_msglen = sizeof(GpsLocation);

    m_pResponseHandler->handleData(&msg);   // must complete sync!

}

// Helper function to reset GPS post processing location values
void TIGPSDriver::resetPostProcessLocation() {
    GPS_LOG_TRACE("Resetting GPS post processing location values");

    memset(&m_gpsLocationArray, 0, sizeof(GpsLocation)*LOCATION_ARRAY_SIZE);
    m_gpsLocationPtr = 0;
    memset(&m_gpsPrevLocation, 0, sizeof(GpsLocation));
    memset(&m_gpsAvgLocation, 0, sizeof(GpsLocation));
    m_gpsUsePosSpeed = false;
}

