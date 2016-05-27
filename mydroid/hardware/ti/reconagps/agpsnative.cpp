/*
 *******************************************************************************
 *         
 *
 * FileName         :   agpsnative.cpp
 *
 * Description      :   JNI Layer for RECON AGPS
 *
 * Author           :   RECON Instruments
 * Date             :   December 2013
 *
 * Comments         :   Currently it is too much of pain in the a$$ to setup compilation
 *                      via standard JNI NDK-BUILD; thus I've set this up as other HAL SO. 
 *                      To test, simply build as HAL so (build_module.sh libreconagps)
 *                      and adb push libreconagps.so /system/lib/. 
 *                      Then build Java Driver APK normally and install on device
 *                          
 ******************************************************************************
 */

#define LOG_TAG "RECON.AGPS"    // our identification for logcat (adb logcat RECON.AGPS:V *:S)

#include <string.h>
#include <jni.h>
#include <asm/ioctl.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#include "agpsnative.h"
#include "tigpsdriver.h"           // gps driver object definition

// init literal constants

const char* ReconAGpsCallbacks::CmdCompleteCallbackSignature = "(II)V";
const char* ReconAGpsCallbacks::StatusCallbackSignature      = "(II)V";

// static initializers
AGPSContext* AGPSContext::__instance = 0;    
JavaVM*      AGPSContext::__jVM      = 0;

#define GET_CONTEXT AGPSContext::getInstance

// cleanup helper for internally managed callbacks structure
void ReconAGpsCallbacks::cleanup()
{
      delete [] handlerName; handlerName = 0;
      delete [] statusCbk;   statusCbk   = 0;
      delete [] commandCbk;  commandCbk  = 0;
}


// helper to store passed strings in managed callbacks structure
GPSStatus ReconAGpsCallbacks::assignProperty (char** member, const char* data)
{
   int iLen = strlen(data);
   if (iLen > 0)
   {
      delete [] *member; *member = 0;
      *member = new char [iLen + 1];

      if (!(*member) )
      {
         GPS_LOG_ERR("Memory Allocation Error\n");
         return GPSStatusCode::GPS_E_MEM_ALLOC;
      }
      strcpy((*member), data);
   }

   return GPSStatusCode::GPS_SUCCESS;
}

GPSStatus ReconAGpsCallbacks::assignData 
(
    const char* handler, 
    const char* status, 
    const char* command
)
{
   GPSStatus stat = this->assignProperty (&handlerName, handler);
   if (stat == GPSStatusCode::GPS_SUCCESS)
      stat = this->assignProperty(&statusCbk, status);

   if (stat == GPSStatusCode::GPS_SUCCESS)
      stat = this->assignProperty(&commandCbk, command);

   return stat;
}

/* Context Constructor */
AGPSContext::AGPSContext()
:m_pDriver(0), m_pResponseHandler(0)
{
}

AGPSContext* AGPSContext::getInstance()
{
   if (AGPSContext::__instance == 0)
   {
      AGPSContext::__instance = new AGPSContext();
   }

   return AGPSContext::__instance;
}

AGPSContext::~AGPSContext()
{
   this->cleanup();
}

GPSStatus AGPSContext::initialize ()
{
    GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);

    // If driver has been allocated, init has already been called
    if (m_pDriver || m_pResponseHandler)
    {
       GPS_LOG_WARN("### GPS Subsystem has already been Initialized! ###\n");
       return GPSStatusCode::GPS_SUCCESS;	
    }


    // allocate driver. Here we only allocate object, without opening connection
    m_pDriver = GPSDriver::Factory (TI_GPS);
    if (!m_pDriver) 
    {
       GPS_LOG_ERR("Internal Error: OEM Driver Failed to Initialize\n");
       m_callbacks.cleanup();

       return GPSStatusCode::GPS_E_INTERNAL;
    }

    // Parse configuration file	
    GPSStatus stat = m_pDriver->readConfigFile (TIGPSDriver::GPS_CONFIG_FILE_PATH);  
    if (stat != GPSStatusCode::GPS_SUCCESS)
    {
       m_callbacks.cleanup();

       delete m_pDriver; m_pDriver = 0;
       return stat;
    }

    // Allocate Response Handler, specific for our little AGPS setup.
    // Context is AgpsCallbacks pointer, which we manage internally
    m_pResponseHandler = new AGPSResponseHandler (&m_callbacks);
    if (!m_pResponseHandler)
    {
       GPS_LOG_ERR("Memory Allocation Error\n");
       delete m_pDriver; m_pDriver = 0;

       m_callbacks.cleanup();

       return GPSStatusCode::GPS_E_MEM_ALLOC;
    }

    // Initialize Driver subsystem.
    stat = m_pDriver->open (m_pResponseHandler);
    if (stat == GPSStatusCode::GPS_SUCCESS)
    {
        // start processing threads - TI Control, no interest in Data, Status
        stat = m_pResponseHandler->startReceiver (
          TIGPSDriver::TI_GPS_PROCESSING_THREAD, 
          TIGPSDriver::TiGpsResponseHandler, 
          m_pDriver,
          0, 0, 0,
          GPSDriver::STATUS_PROCESSING_THREAD,
          GPSDriver::StatusResponseHandler,
          m_pDriver);
    }

    if (stat != GPSStatusCode::GPS_SUCCESS)
    {
       m_callbacks.cleanup();

       delete m_pDriver; m_pDriver = 0;
       delete m_pResponseHandler; m_pResponseHandler = 0;

       return stat;
    }

    // and finally register as assistance source. 
    gpsmessage cmd;

    cmd.opcode = GPSCommand::GPS_REG_ASSIST;
    stat = m_pDriver->sendCommand(&cmd);

    if (stat != GPSStatusCode::GPS_SUCCESS)
    {
       GPS_LOG_ERR("Register assistance source failure!\n");

       m_pDriver->close();
       m_pResponseHandler->endReceiver ();

       delete m_pDriver; m_pDriver = 0;
       delete m_pResponseHandler; m_pResponseHandler = 0;

       m_callbacks.cleanup();

       return stat;
   }

   // ident ourselves as AGPS session
   m_pDriver->setSessionID(ReconGPSSession::AGPS_SESSION_ID);
   return GPSStatusCode::GPS_SUCCESS;
}

void AGPSContext::cleanup ()
{
   GPS_LOG_TRACE("%s: Enter\n", __FUNCTION__);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return;
   }

   m_pDriver->closeStatusPort();

   // stop response receiver
   m_pResponseHandler->endReceiver();

   m_pDriver->close();

   // now deallocate objects
   delete m_pDriver; m_pDriver = 0;
   delete m_pResponseHandler; m_pResponseHandler = 0;

   // free client callbacks as well
   m_callbacks.cleanup ();

   GPS_LOG_TRACE("Exit\n");
}


/* Begin Assistance */
GPSStatus AGPSContext::beginAssist (int flags)
{
   GPS_LOG_TRACE("%s: Enter\n", __FUNCTION__);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   // assemble message
   gpsmessage msg;

   msg.opcode = GPSCommand::GPS_BEGIN_ASSIST;
   msg.i_msglen = sizeof(GpsAidingData);

   // for payload, we use GpsAidingData that will indicate
   // what type of assistance will be provided. 
   msg.pPayload = reinterpret_cast<unsigned char*>(&flags);

   // call into driver. Results will be fired back asyncrhonously
   GPSStatus stat = m_pDriver->sendCommand(&msg);
   GPS_LOG_TRACE("Exit: Status = %d\n", stat);

   return stat;
}

/* End Assistance */
GPSStatus AGPSContext::endAssist ()
{
   GPS_LOG_TRACE("%s: Enter\n", __FUNCTION__);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   // assemble message
   gpsmessage cmd;
   cmd.opcode = GPSCommand::GPS_END_ASSIST;

   // call into driver. Do NOT issue FIX START, because
   // this has already been issued by HAL - otherwise we'd not receive
   // assistance request!
   GPSStatus stat = m_pDriver->sendCommand(&cmd);
 /*  if (stat == GPSStatusCode::GPS_SUCCESS)
   {
       cmd.opcode = GPSCommand::GPS_FIX_BEGIN;
       stat = m_pDriver->sendCommand(&cmd);
   }*/

   GPS_LOG_TRACE("Exit: Status = %d\n", stat);
   return stat;
}

       
/* Location Injection */
GPSStatus AGPSContext::injectLocation 
(
   double lat, 
   double lon, 
   double alt, 
   int    flags
)
{
   GPS_LOG_TRACE("%s: Enter\n", __FUNCTION__);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   // build payload for driver
   GpsLocation loc;
   memset(&loc, 0, sizeof(GpsLocation) );

   loc.size = sizeof(GpsLocation);

   loc.latitude  = lat;
   loc.longitude = lon;
   loc.altitude  = alt;

   loc.flags = flags;

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

/* Time Injection */
GPSStatus AGPSContext::injectTime 
(
   GpsUtcTime utctime,   // UTC Time (miliseconds)
   int uncertainty       // uncertainty (miliseconds)
)
{
   GPS_LOG_TRACE("%s: Enter\n", __FUNCTION__);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   // get GPS Week number and millisecond offset from passed utctime
   gpstimeassist assist;
   GPSUtilities::get_gps_time (utctime, &assist.gps_week, &assist.gps_msec);
   assist.unc_msec = uncertainty;

   GPS_LOG_INF("+++ %s: GPS week: [%d], GPS Millisecond: [%d]. Uncertainty: [%d] +++\n", 
      __FUNCTION__, assist.gps_week, assist.gps_msec, assist.unc_msec);

   // build payload for driver
   gpsmessage msg;

   msg.opcode = GPSCommand::GPS_INJ_TIME;
   msg.i_msglen = sizeof(gpstimeassist);
   msg.pPayload = reinterpret_cast<unsigned char*>(&assist);

   // call into driver. Results will be fired back asyncrhonously
   GPSStatus stat = m_pDriver->sendCommand(&msg);

   // all ok
   GPS_LOG_TRACE("Exit. Status: %d\n", stat);
   return stat;
}

/* Almanac Injection */
GPSStatus AGPSContext::injectAlmanac 
(
   const char* pszAlmanacFile,    // path to almanac file
   int         format             // yuma=1, sem=2
)
{
   GPS_LOG_TRACE("%s: Enter. Almanac File [%s], Format: [%d]\n",
      __FUNCTION__, pszAlmanacFile, format);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   
   // parse almanac from passed YUMA file
   AGPSAlmanac* palm = AGPSAlmanac::Factory (format);
   if (palm == 0) return GPSStatusCode::GPS_E_UNEXPECTED;

   GPS_LOG_INF("Allocated Almanac: Format=[%d]\n", format);

   GPSStatus stat = palm->ParseAlmanacFile (pszAlmanacFile);

   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
       GPS_LOG_ERR("Failed to extract data from Serialized Almanac File [%s]!\n", pszAlmanacFile);

       delete palm; palm = 0;
       return stat;
   }

   // send message. Payload is almanac object
   gpsmessage msg;

   msg.opcode = GPSCommand::GPS_INJ_ALMANAC;
   msg.i_msglen = sizeof(AGPSAlmanac*);
   msg.pPayload = reinterpret_cast<unsigned char*>(palm);

   // call into driver. Results will be fired back asynchronously
   stat = m_pDriver->sendCommand(&msg);
  
   // deallocate almanac
   delete palm; palm = 0;

   GPS_LOG_TRACE("Exit. Status: %d\n", stat);
   return stat;

}

/* Ephemeris Injection */
GPSStatus AGPSContext::injectEphemeris 
(
   const char* pszEphemerisFile,    // path to Ephemeris file
   int         format               // format of Ephemeris file: RINEX or RAW
)
{
   GPS_LOG_TRACE("%s: Enter\n", __FUNCTION__);

   if ((m_pDriver == 0) || (m_pResponseHandler == 0))
   {
      GPS_LOG_ERR("GPS Subsystem has not been Initialized!\n");
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

 
   // depending on format, parse from RINEX or RAW file
   AGPSEphemeris eph; GPSStatus stat = GPSStatusCode::GPS_SUCCESS;
   if (format == com_reconinstruments_agps_GpsNative_EPHEMERIS_FORMAT_RINEX)
      stat = eph.ParseRINEXFile (pszEphemerisFile);

   else if (format == com_reconinstruments_agps_GpsNative_EPHEMERIS_FORMAT_RAW)
      stat = eph.ParseRAWFile (pszEphemerisFile);

   else
   {
      GPS_LOG_ERR("Unsupported Ephemeris File format [%d]", format);
      return GPSStatusCode::GPS_E_UNEXPECTED;
   }

   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
       GPS_LOG_ERR("Failed to extract data from Ephemeris File [%s]!\n", pszEphemerisFile);
       return stat;
   }

   // send message. In each case payload is ephemeris object, but command is different
   gpsmessage msg;

   if (format == com_reconinstruments_agps_GpsNative_EPHEMERIS_FORMAT_RINEX) 
      msg.opcode = GPSCommand::GPS_INJ_EPHEMERIS;
   else
      msg.opcode = GPSCommand::GPS_INJ_RAW_EPH;

   msg.i_msglen = sizeof(AGPSEphemeris*);
   msg.pPayload = reinterpret_cast<unsigned char*>(&eph);

   // call into driver. Results will be fired back asynchronously
   stat = m_pDriver->sendCommand(&msg);

   GPS_LOG_TRACE("Exit. Status: %d\n", stat);
   return stat;

}

/* Following is JNI Plumbing which must be linked without C++ name mangling */
#ifdef __cplusplus
extern "C" {
#endif


/* Called when Native is loaded into Java:
   -- allocate Context Singleton
   -- cache JVM context */

JNIEXPORT jint JNI_OnLoad
(
   JavaVM* vm, 
   void* reserved
)
{
    GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);

    JNIEnv* env = 0;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK)
    {
       GPS_LOG_ERR("JNI Internal Error - could not get Environment Pointer\n");
       return -1;
    }

    // cache JavaVM pointer
    AGPSContext::__jVM = vm;

    GPS_LOG_TRACE("JNI Initialized OK\n");
    return JNI_VERSION_1_6;
}

/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_init
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1init
(
   JNIEnv* env,            // environment ponter
   jclass  jthis,          // parent stub java class
   jstring handlerName,    // name of handler class
   jstring statusFct,      // name of status callback function
   jstring commandFct      // name of command completion callback function
)
{
    GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
    AGPSContext* pContext = GET_CONTEXT();
    ReconAGpsCallbacks* pCallbacks = pContext->getCallbacks();

    const char* handlerString    = env->GetStringUTFChars(handlerName, 0);

    // check handler class validity
    pCallbacks->jvm = AGPSContext::__jVM;
   
    pCallbacks->handlerClass = reinterpret_cast<jclass>(
        env->NewGlobalRef(env->FindClass(handlerString) ) );

    if (pCallbacks->handlerClass == 0)
    {
       GPS_LOG_ERR("JNI Error: Could not find Java Callback class [%s]\n", handlerString);
       env->ReleaseStringUTFChars(handlerName, handlerString);

       return GPSStatusCode::GPS_E_IN_PARAM;
    }

    // assign passed strings
    const char* statusString     = env->GetStringUTFChars(statusFct, 0);
    const char* commandString    = env->GetStringUTFChars(commandFct, 0);

    GPSStatus stat = pCallbacks->assignData (
      handlerString, statusString, commandString);

    env->ReleaseStringUTFChars(handlerName, handlerString);
    env->ReleaseStringUTFChars(statusFct, statusString);
    env->ReleaseStringUTFChars(commandFct, commandString);

    if (stat != GPSStatusCode::GPS_SUCCESS)
    {
       pCallbacks->cleanup();
       return stat;
    }

    return pContext->initialize ();

}

/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_cleanup
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1cleanup
(
   JNIEnv* env,    // environment pointer
   jclass  jthis   // parent stub java class
)
{
   GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
   GET_CONTEXT()->cleanup();
}

/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_begin_assist
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1begin_1assist
 (
   JNIEnv* env,     // environment pointer
   jclass  jthis,   // parent stub java class
   jint    flags    // bitmask of assistance types client will provide
)
{
   GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
   return GET_CONTEXT()->beginAssist (flags);
}

/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_end_assist
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1end_1assist
(
   JNIEnv* env,    // environment pointer
   jclass  jthis   // parent stub java class
)
{
   GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
   return GET_CONTEXT()->endAssist ();
}


/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_inject_location
 * Signature: (DDDI)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1inject_1location
(
   JNIEnv* env,   // environment pointer
   jclass  jthis, // parent stub java class
   jdouble lat,   // latitude
   jdouble lon,   // longitude
   jdouble alt,   // altitude
   jint    flags  // flags indicating which data has been provided
)
{
   GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
   return GET_CONTEXT()->injectLocation(lat, lon, alt, flags);
}

/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_inject_time
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1inject_1time
(
   JNIEnv* env,      // environment pointer
   jclass  jthis,    // parent stub java class
   jlong   utctime,  // UTC time (miliseconds)
   jint    uncert    // uncertaintly (miliseconds)
)
{
   GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
   return GET_CONTEXT()->injectTime(utctime, uncert);
}

/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_inject_almanac
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1inject_1almanac
(
   JNIEnv* env,      // environment pointer
   jclass  jthis,    // parent stub java class
   jstring path,     // path to almanac file
   jint    format    // almanac format (yuma = 1, sem = 2)
)
{
   GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
   const char* almanacFile = env->GetStringUTFChars(path, 0);

   GPSStatus stat = GET_CONTEXT()->injectAlmanac(almanacFile, format);

   env->ReleaseStringUTFChars(path, almanacFile);
   return stat;
}

/*
 * Class:     com_reconinstruments_agps_GpsNative
 * Method:    agps_inject_ephemeris
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_agps_GpsNative_agps_1inject_1ephemeris
 (
   JNIEnv* env,      // environment pointer
   jclass  jthis,    // parent stub java class
   jstring path,     // path to ephemeris file
   jint    format    // ephemeris format (rinex = 1, raw = 2)
)
{
   GPS_LOG_TRACE ("### %s ###\n", __FUNCTION__);
   const char* ephemerisFile = env->GetStringUTFChars(path, 0);

   GPSStatus stat = GET_CONTEXT()->injectEphemeris(ephemerisFile, format);

   env->ReleaseStringUTFChars(path, ephemerisFile);
   return stat;
}

#ifdef __cplusplus
}
#endif


