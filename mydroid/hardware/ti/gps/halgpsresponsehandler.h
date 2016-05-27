#ifndef __halgpsresponsehandler_h__
#define __halgpsresponsehandler_h__

#include "gpsresponsehandler.h"

class HALGPSResponseHandler : public GPSResponseHandler 
{
   protected:
     HALGPSResponseHandler();            // force construction with valid Android Callback structure pointer

   virtual GPSStatus  thread_create (
         pthread_t*        pt,
         const char*       ident,    
         gps_proc_fct      fct,      
         void*             prm);     

   public:
     HALGPSResponseHandler(void* context);
     ~HALGPSResponseHandler();

     void commandCompleted (GPSStatus command, GPSStatus result);
     void handleData       (gpsmessage*);

};

#endif   // __halgpsresponsehandler_h__

