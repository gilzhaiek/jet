/*
 *******************************************************************************
 *         
 *
 * FileName         :   HALGPSResponseHander.cpp
 *
 * Description      :   Implementation of HALGPSResponseHandler object
 *
 * Author           :   RECON Instruments
 * Date             :   November 2013
 *
 * Comments         :   Concrete Implementation of GPS Response Handler
 ******************************************************************************
 */

#define LOG_TAG "RECON.GPS"        // our identification for logcat (adb logcat RECON.GPS:V *:S)

#include "halgpsresponsehandler.h"    // definition of this object


/* Construction - Destruction */
HALGPSResponseHandler::HALGPSResponseHandler ()
{
}

HALGPSResponseHandler::HALGPSResponseHandler (void* context)
:GPSResponseHandler(context)
{
}

HALGPSResponseHandler::~HALGPSResponseHandler()
{
}

/* Concrete Implementation of handling of GPS Command Completion. Mappings:

   Android API             Command                     
   ---------------------   --------------------------                

   GpsInterface::init      GPS_START             
   GpsInterface::start     GPS_FIX_BEGIN     
   GpsInterface::stop      GPS_FIX_END       
   GpsInterface::cleanup   GPS_STOP         

 */
void HALGPSResponseHandler::commandCompleted 
(
    GPSStatus command,    // issued command
    GPSStatus result      // completion result
)
{
   // check result; in Android world they only want to be notified about success
   // of 4 specific commands
   if (result != GPSStatusCode::GPS_SUCCESS)
   {
      GPS_LOG_ERR("Driver Command [0x%x] Failure (Error [0x%x]\n", command, result);
      return;
   }

   GpsCallbacks* pCallbacks = reinterpret_cast<GpsCallbacks*>(m_pCallbackContext);
   GpsStatus as;
   as.size = sizeof(GpsStatus);

   switch (command)
   {
      case GPSCommand::GPS_START:
      {
          as.status = GPS_STATUS_ENGINE_ON;
      }
      break;

      case GPSCommand::GPS_STOP:
      {
          as.status = GPS_STATUS_ENGINE_OFF;
      }
      break;

      case GPSCommand::GPS_FIX_BEGIN:
      {
          as.status = GPS_STATUS_SESSION_BEGIN;
      }
      break;

      case GPSCommand::GPS_FIX_END:
      {
          as.status = GPS_STATUS_SESSION_END;
      }
      break;

      case GPSCommand::GPS_FATAL_ERROR:
      {
          as.status = GPS_STATUS_FATAL_ERROR;
      }
      break;

      default:
         GPS_LOG_TRACE("Not reporting command [0x%x] completion to Android - they are not interested\n", command);
         return;
   }
  

   // this is now sync, in context of driver working thread. Calls back into Android;
   // receive halts untill this returns 
   // **** NOTE: See if this is sufficient, or we need another thread in order not to block, as in original TI Implementation ***
   GPS_LOG_TRACE("Reporting to Android: [0x%x]\n", as.status);
   pCallbacks->status_cb(&as);    // sync
}


/* Concrete Implementation of Received Data Handler. Mappings:

  Data Type                Callback
  ---------------------    ------------------------
  DATA_NMEA                nmea_cb
  DATA_SV                  sv_status_cb

*/
void  HALGPSResponseHandler::handleData 
(
   gpsmessage* pmsg     // received gps message: Code + Payload
)
{
    GpsCallbacks* pCallbacks = reinterpret_cast<GpsCallbacks*>(m_pCallbackContext);

    switch (pmsg->opcode)
    {
       case GPSStatusCode::DATA_NMEA:
       {
          struct timespec t;
          t.tv_sec = t.tv_nsec = 0;
          clock_gettime(CLOCK_REALTIME, &t);

    //      GPS_LOG_TRACE("Reporting to Android NMEA Sequence (%s)\n", reinterpret_cast<char*>(pmsg->pPayload) ); 
          pCallbacks->nmea_cb ((t.tv_sec)*1000000000LL + t.tv_nsec, 
             reinterpret_cast<char*>(pmsg->pPayload), pmsg->i_msglen);
       }
       break;

       case GPSStatusCode::DATA_SV:
       {
   /*        GpsSvStatus* pdata = reinterpret_cast<GpsSvStatus*>(pmsg->pPayload);
           if (pdata->num_svs > 0 && pdata->used_in_fix_mask > 0)
           {
           GPS_LOG_INF("Reporting to Android SV Data\n"); 
           GPS_LOG_INF("Space Vehicles Currently Visible: [%d], used in Fix Mask: [%d]\n", pdata->num_svs, pdata->used_in_fix_mask);
           GPS_LOG_INF("==========================================\n");
          
           for (int i = 0; i < pdata->num_svs; i++)
           {
              GpsSvInfo* psvinfo = &(pdata->sv_list[i]);
              
              GPS_LOG_INF("PRN: [%d], snr: [%f], elevation: [%f], azimuth: [%f]\n",
                   psvinfo->prn, psvinfo->snr, psvinfo->elevation, psvinfo->azimuth);
           }
           }*/

           pCallbacks->sv_status_cb (
             reinterpret_cast<GpsSvStatus*>(pmsg->pPayload) );
       }
       break;

       case GPSStatusCode::DATA_LOCATION:
       {
           GpsLocation* ploc = reinterpret_cast<GpsLocation*>(pmsg->pPayload);

 /*         GPS_LOG_TRACE("Reporting to Android Location: [Lat %3.06f Deg] [Long %3.06f Deg] [Alt %3.06f m] [acc=%f] [speed=%f] [bearing=%f] [timestamp=%llu] [flags=%x]\n",
                  ploc->latitude, ploc->longitude, ploc->altitude, ploc->accuracy, ploc->speed, ploc->bearing, ploc->timestamp, ploc->flags); */

           pCallbacks->location_cb (ploc);
       }
       break;
  

       default:
        GPS_LOG_ERR("Not handling GPS Data Code [0x%x]\n", pmsg->opcode);
   }
}

GPSStatus HALGPSResponseHandler::thread_create (
     pthread_t*   pt,
     const char*  ident,
     gps_proc_fct fct,
     void*        prm)
{
   // allocate response processing thread. We MUST do it via context callback pointer
   // for HAL purposes (see gps.h). Although in HAL layer check is already made
   // for valid pointer, we do it here too as this might be utilized outside of HAL context
   GpsCallbacks* pCallbacks = (GpsCallbacks*)m_pCallbackContext;
   if (!pCallbacks)
   {
      GPS_LOG_ERR("GpsCallbacks has not been configured! (NULL Pointer)\n");
      return GPSStatusCode::GPS_E_INTERNAL;
   }

   *pt = pCallbacks->create_thread_cb (
            ident, reinterpret_cast<void (*)(void*)>(fct), prm);

   if (*pt== NULL_HANDLE)
      return GPSStatusCode::GPS_E_THREAD_ALLOC;


   return GPSStatusCode::GPS_SUCCESS;
}



