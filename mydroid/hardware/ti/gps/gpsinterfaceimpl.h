#ifndef __gpsinterfaceimp__h__
#define __gpsinterfaceimpl_h__

#include <hardware/gps.h>    // Android GPS interface

#include "recongps.h"
#include "halgpsresponsehandler.h"

class GPSDriver;

/* Implementation of required GpsInterface, as 
   defined in hardware/libhardware/include/hardware/gps.h 

   This is the context object, persistent in memory for the duration of HAL lifetime
   It is created during module initialization, and destroyed at GpsInterface::cleanup
*/
class GPSInterfaceImpl
{
   private:
       /* Singleton Pattern */
       GPSInterfaceImpl();
       static GPSInterfaceImpl* __instance;

   public:
       /* Construction - Destruction */
       static GPSInterfaceImpl* getInstance();
       ~GPSInterfaceImpl();
      
     /* GPS Interface Implementation */

     // GpsInterface::init - Sets up internal architecture, starts Driver etc
     GPSStatus init  (GpsCallbacks*);    

     // GpsInterface::start - Starts navigating. Connects Server, Creates Rx thread and supporting infrastructure
     GPSStatus start ();   

     // GpsInterface::stop  - Stops navigating. Cleanly shuts down Rx thread and disconnects server            
     GPSStatus stop();                  

     // GpsInterface::cleanup - Virtual destructor; stops Driver and performs other cleanup
     void      cleanup(bool stop_server = false);
   
     // GpsInterface::inject_time  - Injects the current time
     GPSStatus inject_time(GpsUtcTime time, int64_t timeReference, int uncertainty);  

    /** GpsInterface::inject_location
     *  Injects current location from another location provider
     *  (typically cell ID).
     *  latitude and longitude are measured in degrees
     *  expected accuracy is measured in meters
     */
    GPSStatus inject_location(double latitude, double longitude, float accuracy);

    /**
     * GpsInterface::delete_aiding_data
     * Specifies that the next call to start will not use the
     * information defined in the flags. GPS_DELETE_ALL is passed for
     * a cold start. error: 'LOGE' was not declared in this scope

     */
    void  delete_aiding_data (GpsAidingData flags);

    /** GpsInterface::set_position_mode
     * 
     * min_interval represents the time between fixes in milliseconds.
     * preferred_accuracy represents the requested fix accuracy in meters.
     * preferred_time represents the requested time to first fix in milliseconds.
     */
    GPSStatus   set_position_mode(GpsPositionMode mode, GpsPositionRecurrence recurrence,
            uint32_t min_interval, uint32_t preferred_accuracy, uint32_t preferred_time);

    /** GpsInterface::get_extension
      * Get a pointer to extension information. */
    const void* get_extension (const char* name);

private:
   GPSDriver*          m_pDriver;             // driver facing I/O 
   GPSResponseHandler* m_pResponseHandler;    // Android facing reporter
  
};

#endif   // __gpsinterfaceimpl_h__


