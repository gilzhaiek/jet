/*
 *******************************************************************************
 *         
 *
 * FileName         :   gpshal.cpp
 *
 * Description      :   Main GPS HAL layer Implementation File
 *
 * Author           :   RECON Instruments
 * Date             :   November 2013
 *
 * Comments         : This file is just a glue between Android required HAL Layer
 *                    hookup and actual library implementation, encapsulated in set
 *                    of C++ objects (see recongps.h), wherein lies meat of the processing
 *
 *                    Key objects is GpsInterfaceImpl, implemented as Singleton. It lives in 
 *                    memory as long as SO is mapped in address space of calling (init) process
 *                    and provides requires services, as specified in hardware/libhardware/include/hardware/gps.h
 *
 ******************************************************************************
 */

#define LOG_TAG "RECON.GPS"   // our identification for logcat (adb logcat RECON.GPS:V *:S)


#include "gpsinterfaceimpl.h"     // HAL gps interface; includes recongps.h
#include <hardware/hardware.h>

#define GET_INTERFACE_IMPL GPSInterfaceImpl::getInstance

/*************************************** GPS HAL layer API glue **************************************/

// forward declaration of library constructor
static int recon_gps_open     (hw_module_t const*, const char* id, hw_device_t**);

// module constructor hookup
static struct hw_module_methods_t recon_gps_module_methods = 
{
    open : recon_gps_open    // GPS Library Constructor
};

struct hw_module_t HAL_MODULE_INFO_SYM = 
{
    tag             : HARDWARE_MODULE_TAG,
    version_major   : 1,
    version_minor   : 0,
    id              : GPS_HARDWARE_MODULE_ID,
    name            : "Recon GPS HAL Layer",
    author          : "Recon Instruments",
    methods         : &recon_gps_module_methods,
    dso             : 0,
    reserved        : {0},
};

// GpsInterface routing
static int  rgps_init    (GpsCallbacks* pCallbacks)  { return GET_INTERFACE_IMPL()->init(pCallbacks);}
static int  rgps_start   (void)                      { return GET_INTERFACE_IMPL()->start();} 
static int  rgps_stop    (void)                      { return GET_INTERFACE_IMPL()->stop();}
static void rgps_cleanup (bool stop_server)          { GET_INTERFACE_IMPL()->cleanup(stop_server);}

static int  rgps_inject_time (GpsUtcTime time, int64_t timeReference, int uncertainty) 
       {return GET_INTERFACE_IMPL()->inject_time(time, timeReference, uncertainty);}

static int rgps_inject_location (double latitude, double longitude, float accuracy)
       {return GET_INTERFACE_IMPL()->inject_location(latitude, longitude, accuracy);}

static void rgps_delete_aiding_data (GpsAidingData flags)
       {GET_INTERFACE_IMPL()->delete_aiding_data(flags);}

static int rgps_set_position_mode(GpsPositionMode mode, GpsPositionRecurrence recurrence,
            uint32_t min_interval, uint32_t preferred_accuracy, uint32_t preferred_time)
       {return GET_INTERFACE_IMPL()->set_position_mode(mode, recurrence, min_interval, preferred_accuracy, preferred_time);}

static const void* rgps_get_extension(const char* name)
       {return GET_INTERFACE_IMPL()->get_extension(name);}


// Android GpsInterface instance; returned during library initialization
// Routs implementation to GpsInterfaceImpl object
GpsInterface recongps = 
{
    sizeof(GpsInterface),       /* Size of the GPS interface */

    rgps_init,       
    rgps_start,                                          
    rgps_stop,                                          
    rgps_cleanup,     
    rgps_inject_time,           
    rgps_inject_location,       
    rgps_delete_aiding_data,    
    rgps_set_position_mode,                                
    rgps_get_extension,                                   
};

/**
 * Glue Module Export: Provide HAL GpsInterface Implementation to higher layers
 */
const GpsInterface* recon_get_gps_interface(struct gps_device_t* dev){return &recongps;}


/**
 * Function: recon_gps_open
 *
 * Standard Linux Shared Library entry point; only direct export, rest is 
 * via GpsInterface allocated object
 */
static int recon_gps_open(const struct hw_module_t* module, char const* name,
                    struct hw_device_t** device)
{
    
    struct gps_device_t* dev = (struct gps_device_t *) malloc(sizeof(struct gps_device_t));
    memset(dev, 0, sizeof(*dev));

    dev->common.tag = HARDWARE_DEVICE_TAG;
    dev->common.version = 0;
    dev->common.module = (struct hw_module_t*)module;
    dev->get_gps_interface = recon_get_gps_interface;

    *device = (struct hw_device_t*)dev;
   
    GPS_LOG_TRACE("OK\n");
    return 0;
}

