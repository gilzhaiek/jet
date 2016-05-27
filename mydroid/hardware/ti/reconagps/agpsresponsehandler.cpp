/*
 *******************************************************************************
 *         
 *
 * FileName         :   AGPSResponseHander.cpp
 *
 * Description      :   Implementation of AGPSResponseHandler object
 *
 * Author           :   RECON Instruments
 * Date             :   December 2013
 *
 * Comments         :   Concrete Implementation of GPS Response Handler for AGPS Client
 ******************************************************************************
 */

#define LOG_TAG "RECON.AGPS"        // our identification for logcat (adb logcat RECON.AGPS:V *:S)

#include "agpsresponsehandler.h"    // definition of this object


/* Construction - Destruction */
AGPSResponseHandler::AGPSResponseHandler ()
{
}

AGPSResponseHandler::AGPSResponseHandler (void* context)
:GPSResponseHandler(context)
{
}

AGPSResponseHandler::~AGPSResponseHandler()
{
}


void AGPSResponseHandler::commandCompleted
(
   GPSStatus cmd,     // command
   GPSStatus result   // result
)
{
    GPS_LOG_TRACE ("Command Completion Report. Cmd: [0x%x], Result: [0x%x]\n", cmd, result);

    // context is ReconAGpsCallbacks structure, which contains Java VM pointer as well
    ReconAGpsCallbacks* pCallbacks = reinterpret_cast<ReconAGpsCallbacks*>(m_pCallbackContext);

    // check if command callback has been registered
    if ( pCallbacks->commandCbk == 0 )
    {
       GPS_LOG_TRACE ("Command Callback has not been registered. Exiting...\n");
       return; 
    }

    JNIEnv* env = 0;
    pCallbacks->jvm->AttachCurrentThread(&env, NULL);

    jmethodID mid = env->GetStaticMethodID(pCallbacks->handlerClass, 
       pCallbacks->commandCbk, ReconAGpsCallbacks::CmdCompleteCallbackSignature);

    if (mid == 0)
    {
       GPS_LOG_ERR("JNI Internal Error: Could not retrieve Method (%s:%s(%s) )\n",
          pCallbacks->handlerName, pCallbacks->commandCbk, ReconAGpsCallbacks::CmdCompleteCallbackSignature);
       
       pCallbacks->jvm->DetachCurrentThread ();
       return;
    }

    env->CallStaticVoidMethod (pCallbacks->handlerClass, mid, cmd, result);

    GPS_LOG_TRACE ("Command Completion Report Finished\n");
   
    pCallbacks->jvm->DetachCurrentThread ();
}

/* Concrete Implementation of Received GPS Status code

   Command                    Response
   --------------             --------------------
 */
void  AGPSResponseHandler::handleStatus 
(
   GPSStatus     code,     // received status code
   GPSStatus     extra     // extra data
)
{
    //ALOGI ("+++ Status Report. Code: [0x%x]. Extra [0x%x] +++\n", code, extra);

    // context is ReconAGpsCallbacks structure, which contains Java VM pointer as well
    ReconAGpsCallbacks* pCallbacks = reinterpret_cast<ReconAGpsCallbacks*>(m_pCallbackContext);

    // check if status callback has been registered
    if ( pCallbacks->statusCbk == 0 )
    {
       GPS_LOG_TRACE ("Status Callback has not been registered. Exiting...\n");
       return; 
    }

    JNIEnv* env = 0;
    pCallbacks->jvm->AttachCurrentThread(&env, NULL);

    jmethodID mid = env->GetStaticMethodID(pCallbacks->handlerClass, 
       pCallbacks->statusCbk, ReconAGpsCallbacks::StatusCallbackSignature);

    if (mid == 0)
    {
       GPS_LOG_ERR("JNI Internal Error: Could not retrieve Method (%s:%s(%s) )\n",
          pCallbacks->handlerName, pCallbacks->statusCbk, ReconAGpsCallbacks::StatusCallbackSignature);
       
       pCallbacks->jvm->DetachCurrentThread ();
       return;
    }

    env->CallStaticVoidMethod (pCallbacks->handlerClass, mid, code, extra);
    GPS_LOG_TRACE("Status Report Complete\n");
    pCallbacks->jvm->DetachCurrentThread ();
}

/* Concrete Implementation of Received Data Handler. Mappings:

  Data Type                Callback
  ---------------------    ------------------------


*/
void  AGPSResponseHandler::handleData 
(
   gpsmessage* pmsg     // received gps message: Code + Payload
)
{
    // context is ReconAGpsCallbacks structure, which contains Java VM pointer as well
    ReconAGpsCallbacks* pCallbacks = reinterpret_cast<ReconAGpsCallbacks*>(m_pCallbackContext);

    JNIEnv* env = 0;
   
    switch (pmsg->opcode)
    {
      //TODO: Enable this via JNI to assistant, he might be interested in SV data
      case GPSStatusCode::DATA_SV:
      {
        /* GpsSvStatus* psv = reinterpret_cast<GpsSvStatus*>(pmsg->pPayload);

         GPS_LOG_INF("Space Vehicle Data. Not sending to JNI, Dump Follows: \n");
         GPS_LOG_INF("Number of SV's currently visible: [%d]\n", psv->num_svs);

         GPS_LOG_INF("Ephemeris Mask: [0x%x], Almanac Mask: [0x%x]. Used in Fix Mask: [0x%x]\n",
           psv->ephemeris_mask, psv->almanac_mask, psv->used_in_fix_mask);

         if (psv->num_svs > 0)
         {
            GPS_LOG_INF("Space Vehicle List: \n");
            GPS_LOG_INF("=================== \n");
            for (int i = 0; i < psv->num_svs; i++)
            {
               GPS_LOG_INF("%d. PRN: [%d], Signal-to-noise: [%f], Elevation: [%f], Azimuth: [%f]\n",
                 i + 1, psv->sv_list[i].prn, psv->sv_list[i].snr, psv->sv_list[i].elevation, psv->sv_list[i].azimuth);
            }
         }*/
      }
      break;

       default:
        GPS_LOG_INF("Not handling GPS Data Code [0x%x]\n", pmsg->opcode);
   }


 //  GPS_LOG_TRACE("Data Report Complete\n");
 
}





