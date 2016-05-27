/*
 *******************************************************************************
 *         
 *
 * FileName         :   GpsInterfaceImpl.cpp
 *
 * Description      :   Implementation of GpsInterfaceImpl object
 *
 * Author           :   RECON Instruments
 * Date             :   November 2013
 *
 * Comments         :   
 ******************************************************************************
 */

#define LOG_TAG "RECON.GPS"   // our identification for logcat (adb logcat RECON.GPS:V *:S)

#include "gpsinterfaceimpl.h"      // this object definition; includes main platform include
#include "tigpsdriver.h"           // gps driver object definition

GPSInterfaceImpl* GPSInterfaceImpl::__instance = 0;    // initializer

/* Default c-tor, hidden */
GPSInterfaceImpl::GPSInterfaceImpl()
:m_pDriver(0), m_pResponseHandler(0)
{
}

/* Constructor */
GPSInterfaceImpl* GPSInterfaceImpl::getInstance()
{
   if (GPSInterfaceImpl::__instance == 0)
   {
      GPS_LOG_TRACE("First Instance!\n");
      GPSInterfaceImpl::__instance = new GPSInterfaceImpl();
   }

   return GPSInterfaceImpl::__instance;
}

GPSInterfaceImpl::~GPSInterfaceImpl()
{
   this->cleanup();
}

/* GpsInterface::init
   Initializes GPS Subsystem. This is essentially constructor; destructor is implemented in 'cleanup'
   The purpose is to bootstrap all the pieces, without starting GPS reporting itself

   -- allocates GPS Driver (currently TI; change to something else in different architecture)
 */
GPSStatus GPSInterfaceImpl::init  
(
   GpsCallbacks* pCallbacks   // structure of function pointers, used to report back to Android upon async arrival of GPS events
)
{
   GPS_LOG_TRACE("Enter\n");

   // If driver has been allocated, init has already been called
   if (m_pDriver || m_pResponseHandler)
   {
      GPS_LOG_WARN("GPS Subsystem has already been Initialized!\n");
      return GPSStatusCode::GPS_SUCCESS;
   }

   // validate input parameter; must be valid pointer!
   if (pCallbacks == 0) 
   {
       GPS_LOG_ERR("Invalid GpsCallbacks Pointer!\n");
       return GPSStatusCode::GPS_E_IN_PARAM;
   }

   // allocate driver. Here we only allocate object, without opening connection
   m_pDriver = GPSDriver::Factory (TI_GPS);
   if (!m_pDriver) 
   {
      GPS_LOG_ERR("Internal Error: OEM Driver Failed to Initialize\n");
      return GPSStatusCode::GPS_E_INTERNAL;
   }

   // Parse configuration file
   GPSStatus stat = m_pDriver->readConfigFile (TIGPSDriver::GPS_CONFIG_FILE_PATH);  
   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
      delete m_pDriver; m_pDriver = 0;
      return stat;
   }

   // Allocate Response Handler, specific for HAL layer & bound to GpsCallbacks 
   // structure provided by Android.
   m_pResponseHandler = new HALGPSResponseHandler (pCallbacks);
   if (!m_pResponseHandler)
   {
      GPS_LOG_ERR("Memory Allocation Error\n");
      delete m_pDriver; m_pDriver = 0;

      return GPSStatusCode::GPS_E_MEM_ALLOC;
   }
    
   // Initialize Driver subsystem. This is OEM specific implementation,
   // (i.e. for TI, NaviLink server is started and Socket Connection established)
   stat = m_pDriver->open (m_pResponseHandler);
   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
      delete m_pDriver; m_pDriver = 0;
      delete m_pResponseHandler; m_pResponseHandler = 0;

      return stat;
   }

   // start processing thread(s)
   stat = m_pResponseHandler->startReceiver (
       TIGPSDriver::TI_GPS_PROCESSING_THREAD,   // control thread name
       TIGPSDriver::TiGpsResponseHandler, 
       m_pDriver,
       GPSDriver::DATA_PROCESSING_THREAD,
       GPSDriver::DataResponseHandler,
       m_pDriver,
       0, 0, 0);  // no status thread - this goes to assistant

   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
      GPS_LOG_ERR("Processing Thread Start Failure!\n");

      m_pDriver->closeDataPort();
      m_pDriver->close();

      delete m_pDriver; m_pDriver = 0;
      delete m_pResponseHandler; m_pResponseHandler = 0;

      return stat;
   }

   // and finally send request on connected driver to start baseband. 
   gpsmessage cmd;
   cmd.opcode = GPSCommand::GPS_START;
   stat = m_pDriver->sendCommand(&cmd);
   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
      GPS_LOG_ERR("GPS Baseband Start Failure!\n");

      m_pDriver->closeDataPort();
      m_pDriver->close();

      m_pResponseHandler->endReceiver ();

      delete m_pDriver; m_pDriver = 0;
      delete m_pResponseHandler; m_pResponseHandler = 0;

      return stat;
   }

   pCallbacks->set_capabilities_cb( GPS_CAPABILITY_MSB | GPS_CAPABILITY_MSA);
   m_pDriver->setSessionID(ReconGPSSession::HAL_SESSION_ID);

   // all ok!
   GPS_LOG_INF ("GPS Interface successfully initialized!\n");
   GPS_LOG_TRACE("OK\n");

   return GPSStatusCode::GPS_SUCCESS;
}   

/* GpsInterface::start 
    -- Begins GPS Session by issuing GPSCommand::GPS_FIX_REQUEST command
    -- We check against successful init (existence of Driver & ResponseHandler)
    -- Subsequenty We won't check for prior successful start; it is the client duty to call API
       in proper sequence, and handle result. If start fails, all other API (except cleanup) will fail too
 */
GPSStatus GPSInterfaceImpl::start ()
{
   GPS_LOG_TRACE("Enter\n");

   // We must have Driver and Response Handler
   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

    // Enable GPS Watchdog
    m_pDriver->enableWatchdog(LOCATION_WD_PING);

   // now issue Fix Request command  (c-tor nulls gpsmessage)
   gpsmessage cmd;
   cmd.opcode = GPSCommand::GPS_FIX_BEGIN;

   GPSStatus stat = m_pDriver->sendCommand(&cmd);

   if (stat != GPSStatusCode::GPS_SUCCESS)
      GPS_LOG_ERR("Failed to send Location Fix Start Request!\n");
   
   // success in sending command still does not mean success; result is received
   // asynchronously via Response Handler and delivered via GpsCallbacks
   GPS_LOG_TRACE("Exit: Status = %d\n", stat);
   return stat;
}   

/* GpsInterface::stop */           
GPSStatus GPSInterfaceImpl::stop ()
{
   // this is implemented via sending GPS_FIX_END request to the driver
   // internal infrastructure is still retained; released as part of cleanup
   
   // *** Original TI implementation was waiting here for completion, then invoking
   //     NAVC_CMD_SAVE_ASSISTANCE, waiting for that, and finally triggering callback
   //   
   //     This implementation simply follows path of other async completions.

   GPS_LOG_TRACE("Enter\n");

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

    // Stop the GPS watchdog
    m_pDriver->disableWatchdog();

   // now issue Fix Stop command
   gpsmessage cmd;
   cmd.opcode = GPSCommand::GPS_FIX_END;
   GPSStatus stat = m_pDriver->sendCommand(&cmd);

   if (stat != GPSStatusCode::GPS_SUCCESS)
      GPS_LOG_ERR("Failed to send Location Fix End Request!\n");
   
   // success in sending command still does not mean success; result is received
   // asynchronously via Response Handler and delivered via GpsCallbacks
   GPS_LOG_TRACE("Exit: Status = %d\n", stat);
   return stat;
}                    

/* GpsInterface::cleanup */
void GPSInterfaceImpl::cleanup(bool stop_server)
{
   GPS_LOG_TRACE("%s: Enter\n", "GPSInterfaceImpl::cleanup");

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return;
   }

   // TI Original would send GPS_STOP (NAVC_CMD_STOP). This is not necessary and if done causes problems
   // should the stack need to be brought up again during runtime (as part of LocationProvider enable/disable).
   // Also, I've witnessed intermittent bug with stack overflow on NAV side (gpsc_state.c) - recursion. 
   // It does not hurt to leave it this way, as long as STOP_FIX has been issued (GpsInterfaceImpl::stop) 
   // main reason being AI2 power consumption
   //
   /*gpsmessage cmd;
   cmd.opcode = GPSCommand::GPS_STOP;
   GPSStatus stat = m_pDriver->sendCommand(&cmd);

   if (stat != GPSStatusCode::GPS_SUCCESS)
      GPS_LOG_ERR("GPS Baseband Stop Failure!\n");*/

   // stop response receivers; this signals end to data and control threads
   m_pResponseHandler->endReceiver ();

   // close data port. Thread is signaled, but still blocking - closing of port should unfreeze him
   // as pipe handle will become invalid and he will exit cleanly
   m_pDriver->closeDataPort();

   // close the driver. This should close control port as well
   m_pDriver->close(stop_server);

   // now deallocate objects
   delete m_pDriver; m_pDriver = 0;
   delete m_pResponseHandler; m_pResponseHandler = 0;

   GPS_LOG_INF("+++ %s: HAL GPS Stack has been successfully destroyed +++\n", "GPSInterfaceImpl::cleanup");

}            
   

/* GpsInterface::delete_aiding_data */
void  GPSInterfaceImpl::delete_aiding_data 
(
   GpsAidingData flags
)
{
   GPS_LOG_TRACE("Enter. flags=[%d]\n", flags);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return;
   }

   // fill message; payload is bitmask of aiding data
   gpsmessage cmd;

   cmd.opcode = GPSCommand::GPS_DEL_ASSIST;
   cmd.i_msglen = sizeof(GpsAidingData);
   cmd.pPayload = reinterpret_cast<unsigned char*>(&flags); 

   // send message to driver
   GPSStatus stat = m_pDriver->sendCommand(&cmd);
   if (stat != GPSStatusCode::GPS_SUCCESS)
      GPS_LOG_ERR("Failed to delete Gps Aiding Data!\n");

   GPS_LOG_TRACE("Exit\n");
}

/* GpsInterface::set_position_mode */
GPSStatus GPSInterfaceImpl::set_position_mode
(
   GpsPositionMode         mode, 
   GpsPositionRecurrence   recurrence,
   uint32_t                min_interval, 
   uint32_t                preferred_accuracy, 
   uint32_t                preferred_time
)
{
   GPS_LOG_TRACE("Enter. GpsPositionMode [%d], GpsPositionRecurrence [%d], min_interval [%d], preferred_accuracy [%d], preferred_time [%d]\n", mode, recurrence, min_interval, preferred_accuracy, preferred_time);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   // fill message; payload is Android info above. 
   // *** FOR RECON - We start Server as standalone, as direct MS-BASED assistance is not available
   //                 Instead we base this in custom AGPS provider
   gpsmessage cmd;

#ifdef RECON_JET
   mode = GPS_POSITION_MODE_STANDALONE;
#endif 
   gpspositionmode posmode(mode, recurrence, min_interval, preferred_accuracy, preferred_time, GPSDriver::MAX_TT_FF_VALUE);

   cmd.opcode = GPSCommand::GPS_SET_POS_MODE;
   cmd.i_msglen = sizeof(struct _gpspositionmode);
   cmd.pPayload = reinterpret_cast<unsigned char*>(&posmode); 

   // send message to driver
   GPSStatus stat = m_pDriver->sendCommand(&cmd);
   if (stat != GPSStatusCode::GPS_SUCCESS)
      GPS_LOG_ERR("Failed to set Firmware Position Mode!\n");

   GPS_LOG_TRACE("Exit: Status = %d\n", stat);
   return stat;
}   

/* GpsInterface::inject_time 

TI initially did not have this implemented, with lame comment "Google did not get this right"
Truth is, they did not know how to convert UtcTime into GPS format (week # and msec offset)

TODO: Not sure what timeReference is for?
*/
GPSStatus GPSInterfaceImpl::inject_time
(
   GpsUtcTime     utctime,          // miliseconds since 1.1.1970
   int64_t        timeReference,    // ???? not sure what is this for ????
   int            uncertainty       // time uncertainty (msec)
)
{
   GPS_LOG_TRACE("Enter\n");
  
   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   // get GPS Week number and millisecond offset from passed utctime
   gpstimeassist assist;
   GPSUtilities::get_gps_time (utctime, &assist.gps_week, &assist.gps_msec);
   assist.unc_msec = uncertainty;

   GPS_LOG_TRACE("### GPS week: [%d], GPS Millisecond: [%d]. Uncertainty: [%d] ###\n", 
      assist.gps_week, assist.gps_msec, assist.unc_msec);

   // build payload for driver
   gpsmessage msg;

   msg.opcode = GPSCommand::GPS_INJ_TIME;
   msg.i_msglen = sizeof(gpstimeassist);
   msg.pPayload = reinterpret_cast<unsigned char*>(&assist);

   // call into driver. Results will be fired back asyncrhonously
   GPSStatus stat = m_pDriver->sendCommand(&msg);
   GPS_LOG_TRACE("Exit: Status = %d\n", stat);

   return stat;
}     

/* GpsInterface::inject_location */
GPSStatus GPSInterfaceImpl::inject_location
(
   double latitude, 
   double longitude, 
   float  accuracy
)
{
   GPS_LOG_TRACE("Enter\n");

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   // build payload for driver
   GpsLocation loc;
   memset(&loc, 0, sizeof(GpsLocation) );

   loc.size = sizeof(GpsLocation);

   loc.latitude  = latitude;
   loc.longitude = longitude;
   loc.accuracy  = accuracy;

   loc.flags = GPS_LOCATION_HAS_LAT_LONG;

   // assemble message
   gpsmessage msg;

   msg.opcode = GPSCommand::GPS_INJ_POSITION;
   msg.i_msglen = sizeof(GpsLocation);
   msg.pPayload = reinterpret_cast<unsigned char*>(&loc);

   // call into driver. Results will be fired back asyncrhonously
   GPSStatus stat = m_pDriver->sendCommand(&msg);
   GPS_LOG_TRACE("Exit: Status = %d\n", stat);
   return stat;
}   

/* GpsInterface::get_extension

   Following sequence is queried:
  [gps-xtra] [agps] [gps-ni] [gps-debug] [agps-ril] 

   For RECON implementation at least to start with, none of these are feasible
   due to specific nature of the device. Going further, some might be inserted based
   on Engage mobile support (TBD)
 */
const void* GPSInterfaceImpl::get_extension 
(
   const char* name   // string identifier of requested extension interface
)
{
   GPS_LOG_TRACE("Enter. Extension = [%s]\n", name);
   int iSupported = 0;

   GPS_LOG_TRACE("Exit. Supported [%d]\n", iSupported);
   return 0;
}



