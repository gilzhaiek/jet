#ifndef __agpsresponsehandler_h__
#define __agpsresponsehandler_h__

#include "gpsresponsehandler.h"
#include "agpsnative.h"

class AGPSResponseHandler : public GPSResponseHandler 
{
   protected:
     AGPSResponseHandler();            // force initializing c-tor

   public:
     AGPSResponseHandler(void* context);    // context is AGpsCallbacks structure
     ~AGPSResponseHandler();

     void commandCompleted (GPSStatus cmd, GPSStatus result); 
     void handleStatus     (GPSStatus status, GPSStatus extra = 0); 
     void handleData       (gpsmessage*);

};

#endif   // __agpsresponsehandler_h__

