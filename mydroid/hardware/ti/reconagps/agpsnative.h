/***************************************************************************
 *
 *  Native AGPS header. Defines static context
 *
 ***************************************************************************/

#ifndef __agpsnative_h__
#define __agpsnative_h__

#include <jni.h>
#include "recongps.h"

#include "agpsresponsehandler.h"
#include "agpsalmanac.h"
#include "agpsephemeris.h"

// forward declaration
class GPSDriver;

// agps callbacks: Context
typedef struct _ReconAGpsCallbacks
{
   JavaVM*  jvm;             // virtual machine

   jclass   handlerClass;    // Java Handler Class 
   char*    handlerName;     // Java Handler Class Name

   char*    statusCbk;       // Status Function Callback name
   char*    commandCbk;      // Command Function Callback name

   _ReconAGpsCallbacks():jvm(0), handlerClass(0), handlerName(0), statusCbk(0), commandCbk(0){}
   ~_ReconAGpsCallbacks(){this->cleanup ();}

   // callback signatures
   static const char* CmdCompleteCallbackSignature;
   static const char* StatusCallbackSignature;

   // helper to store passed strings
   GPSStatus assignData (const char* handler, const char* status, const char* command);
   GPSStatus assignProperty (char** member, const char* data);

   // cleanup helper
   void cleanup ();

}ReconAGpsCallbacks;

class AGPSContext
{

   private:
       /* Singleton Pattern */
       AGPSContext();
       static AGPSContext* __instance;

       /* Data Members */
   private:
      GPSDriver*          m_pDriver;             // driver facing I/O 
      GPSResponseHandler* m_pResponseHandler;    // JNI facing reporter

      ReconAGpsCallbacks  m_callbacks;           // client callbacks: ResponseHandler manages we use it

   public:
       /* Construction - Destruction */
       static AGPSContext* getInstance();
       static JavaVM*      __jVM;

       ~AGPSContext ();

       ReconAGpsCallbacks* getCallbacks() {return &m_callbacks;}

       GPSStatus  initialize ();
       void       cleanup ();

       GPSStatus beginAssist (int flags);
       GPSStatus endAssist   ();

       GPSStatus injectLocation     (double lat, double lon, double alt, int flags);
       GPSStatus injectTime         (GpsUtcTime utctime, int uncertainty);
       GPSStatus injectAlmanac      (const char* pszAlmanacFile, int format);
       GPSStatus injectEphemeris    (const char* pszEphemerisFile, int format);

};

// various constants generated on java side
#ifdef __cplusplus
extern "C" {
#endif
#undef com_reconinstruments_agps_GpsNative_LOCATION_INVALID
#define com_reconinstruments_agps_GpsNative_LOCATION_INVALID 0L
#undef com_reconinstruments_agps_GpsNative_LOCATION_HAS_LAT_LONG
#define com_reconinstruments_agps_GpsNative_LOCATION_HAS_LAT_LONG 1L
#undef com_reconinstruments_agps_GpsNative_LOCATION_HAS_ALTITUDE
#define com_reconinstruments_agps_GpsNative_LOCATION_HAS_ALTITUDE 2L
#undef com_reconinstruments_agps_GpsNative_LOCATION_HAS_SPEED
#define com_reconinstruments_agps_GpsNative_LOCATION_HAS_SPEED 4L
#undef com_reconinstruments_agps_GpsNative_LOCATION_HAS_BEARING
#define com_reconinstruments_agps_GpsNative_LOCATION_HAS_BEARING 8L
#undef com_reconinstruments_agps_GpsNative_LOCATION_HAS_ACCURACY
#define com_reconinstruments_agps_GpsNative_LOCATION_HAS_ACCURACY 16L
#undef com_reconinstruments_agps_GpsNative_ALMANAC_FORMAT_YUMA
#define com_reconinstruments_agps_GpsNative_ALMANAC_FORMAT_YUMA 1L
#undef com_reconinstruments_agps_GpsNative_ALMANAC_FORMAT_SEM
#define com_reconinstruments_agps_GpsNative_ALMANAC_FORMAT_SEM 2L
#undef com_reconinstruments_agps_GpsNative_EPHEMERIS_FORMAT_RINEX
#define com_reconinstruments_agps_GpsNative_EPHEMERIS_FORMAT_RINEX 1L
#undef com_reconinstruments_agps_GpsNative_EPHEMERIS_FORMAT_RAW
#define com_reconinstruments_agps_GpsNative_EPHEMERIS_FORMAT_RAW 2L


#ifdef __cplusplus
}
#endif


#endif    // __agpsnative_h__
