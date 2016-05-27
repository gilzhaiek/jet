/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/***************************************************************************
 *
 *  Main user space sensor library implementation file
 *
 ***************************************************************************/

/* Include Files */
#include <strings.h>
#include <stdlib.h>
#include <errno.h>


#include "risensors.h"

/* local definitions */
#define DEFAULT_SYNC_DELAY 1000       // default initial poll rate for synchronized event, if not enforced via conf

#undef COPY_SENSORS_CONF

/* Misc local macros */
#define NSEC_TO_MS(val) val / 1000000
#define MSEC_TO_MS(val) val / 1000

/*************************************** Sensors library API glue **************************************/

// forward declarations
int sensors__get_sensors_list (struct sensors_module_t*, const sensor_t**);
static int risensors_open     (hw_module_t const*, const char* id, hw_device_t**);

// module constructor hookup
static struct hw_module_methods_t sensors_module_methods = 
{
    open : risensors_open    // Sensor Library Constructor
};

struct ri_sensors_module HAL_MODULE_INFO_SYM =
{
    common :
    {
        tag           : HARDWARE_MODULE_TAG,
        version_major : 1,
        version_minor : 0,
        id            : SENSORS_HARDWARE_MODULE_ID,
        name          : "JET Sensors Module",
        author        : "Recon Instruments",
        methods       : &sensors_module_methods,      // entry point -- invokes constructor
        dso           : 0,
        reserved      : {0},
    },

    get_sensors_list  : sensors__get_sensors_list,    // board configuration
    pconf             : 0                             // config class; allocated at open
};


/**** Sensors Interface Adapter ****/
static int ISENSORSAPI__close
(
   struct hw_device_t* dev
)
{
    ri_sensors_context* ctx = (ri_sensors_context*)dev;

    // close the mux and free memory
    if (ctx)
    {
        close (ctx->m_mux);
        delete ctx;
    }

    SENSD("*** ISENSORAPI_close::finished ***\n");
    return 0;
}

static int ISENSORSAPI__activate
(
   struct sensors_poll_device_t* dev,
   int                           handle, 
   int                           enabled
)
{
    ri_sensors_context *ctx = (ri_sensors_context*)dev;
    return ctx->activate(handle, enabled);
}

static int ISENSORSAPI__setDelay
(
   struct sensors_poll_device_t* dev,
   int                           handle,
   int64_t                       ns
)
{
    ri_sensors_context* ctx = (ri_sensors_context*)dev;
    return ctx->setDelay(handle, ns);
}

static int ISENSORSAPI__poll
(
   struct sensors_poll_device_t* dev,
   sensors_event_t*              data,
   int                           count
)
{
    ri_sensors_context* ctx = (ri_sensors_context*)dev;
    return ctx->pollEvents(data, count);
}

/* Internal helper to handle conf file mess till I devise more robust solution */
static int handle_conf(RI_SENSOR_HANDLE boardmask, struct ri_sensors_module* pm)
{
   // first assign default properties. If this fails, we treat it as unrecoverable (memory) error
   int iRet = pm->pconf->ParseDefaults(boardmask);
   if (iRet != 0) return iRet;

   // first try calibrated, which is what happen most of the time
   SENSD("+++ Parsing Calibrated Configuration File... +++\n");
   iRet = pm->pconf->ParseCalibrated(0);
   if (iRet == 0)
   {
      SENSD("+++ Board Configuration sucessfully extracted from Calibrated Configuration File! +++\n");
      return iRet;
   }
   else
   {
      ALOGE("+++ Error [%d] Parsing Calibrated Configration File. Attempting from default...+++\n", iRet);
   }

   // we are here because calibrated either doesn't exist, or parse failed. In either case we will try
   // from default; if we succeed, we will also make calibrated copy
   FILE* pfh_default = fopen (SENSORS_CONF_FILE_DEFAULT, "r");
   if (pfh_default == NULL)
   {
       ALOGE("+++ CRITICAL: Neither calibrated or default configuration files found!! +++\n");
       return 0;
   }

   // if parse of default factory file fails, system is in unpredictable state = no sensors
   iRet = pm->pconf->Parse(pfh_default, 0);

#ifdef COPY_SENSORS_CONF
   // The only way we can end up here is if we successfully loaded default CONF file, so try to make a calibrated copy
   SENSD("+++ Trying to create calibrated CONF file [%s] for the first time...+++\n", SENSORS_CONF_FILE);
   pfh_calibrated = fopen (SENSORS_CONF_FILE, "w");
   if (pfh_calibrated == NULL)
   {
     ALOGI("+++ Could not open [%s] calibrated CONF file for writing +++\n", SENSORS_CONF_FILE);
     return 0;  // still success
   }

   // get size of original file
   fseek(pfh_default, 0, SEEK_END);
   int pfh_default_length = ftell(pfh_default);
   rewind(pfh_default);

   SENSD("*** Default configuration file size: (%d). ***\n", pfh_default_length);

   // copy
   for (int i = 0 ; i < pfh_default_length ; i++)
     fputc(fgetc(pfh_default), pfh_calibrated);

   // close both
   fclose(pfh_calibrated);

   // change permissions and return success
   char buf[100];
   sprintf(buf, "chmod 666 %s", SENSORS_CONF_FILE);
   system(buf);

   sprintf(buf, "chown system.system %s", SENSORS_CONF_FILE);
   system(buf);

   SENSD("+++ Successfully created Calibrated CONF file for the first time! +++\n"); */

#endif

   fclose(pfh_default);
   pfh_default = 0;

   return iRet;
}

/* Sensors Library Constructor. Glue required API, then
 * allocate all concrete objects that will hookup to their
 * drivers. In Phase 2 this will simply revert to link with
 * RI Virtual Sensor node */
static int risensors_open
(
   const struct hw_module_t* module,
   const char*               id,         // gingerbread will pass "poll"; froyo calls this twice ("control" + "data")
   hw_device_t**             device
)
{
    struct ri_sensors_module* pm = (struct ri_sensors_module*)module;

    SENSD("+++ risensors_open::ENTER. Passed id [%s] +++\n", id);

    // validate id
    if (strcmp(id, SENSORS_HARDWARE_POLL) )
    {
       ALOGE("*** risensors_open::Invalid Android Context. Got [%s], expecting [%s] ***\n", id, SENSORS_HARDWARE_POLL);
       return -ENODEV;
    }


    // allocate memory for context
    ri_sensors_context* dev = new ri_sensors_context();
    if (!dev)
    {
       ALOGE("*** risensors_open::Memory Allocation Error ***\n");
       return -ENOMEM;
    }

    memset(&dev->device, 0, sizeof(sensors_poll_device_t));

    // allocate configuration object
    pm->pconf = new RISensorsConf();
    if (!pm->pconf)
    {
       ALOGE("*** risensors_open::Memory Allocation Error ***\n");
       delete dev;

       return -ENOMEM;

    }

    // open the mux device. All HAL communication with Kernel routs this way
    char node[20];
    sprintf (node, "/dev/%s", PROXMUX_DRIVER_NAME);
    dev->m_mux = open (node, O_RDONLY);

    if (dev->m_mux == -1)
    {
       ALOGE("*** risensors_open::Error opening [%s] (%d). ***\n", node, errno);
       delete dev; delete pm->pconf;

       return -errno;
    }

    SENSD("*** risensors_open:: [%s] successfully opened ***\n", node);

    // Get board configuration. This includes bitmask of registered sensors and max IO data chunk size
    risensorcmd cmd;
    memset(&cmd, 0, sizeof(risensorcmd) );

    int iRet = ioctl (dev->m_mux, PROXMUX_IOCTL_GET_CONFIG, &cmd);

    if (iRet < 0)
    {
       ALOGE("*** risensors_open::PROXMUX_IOCTL_GET_CONFIG error (%d). ***\n", errno);
       close (dev->m_mux);
       delete dev; delete pm->pconf;

       return iRet;
    }

    RI_SENSOR_HANDLE boardmask = cmd.handle;
    unsigned int datasize = cmd.long1;

    SENSD("*** risensors_open:: Retrieved Board Configuration. Registered Sensor Bitmask [0x%x], Data Buffer Size [%d] bytes ***\n", boardmask, datasize);

    // handle configuration file
    iRet = handle_conf (boardmask, pm);
    if (iRet != 0)
    {
       ALOGE("*** risensors_open::Error Extracting board Configuration (%d). ***\n", iRet);
       close (dev->m_mux);
       delete dev; delete pm->pconf;

       return iRet;
    }

    // allocate event reader and data buffer: size (max # of bytes we can retrieve via single read) is returned in long1
    dev->m_pReader = new RIEventReader ();
    if (dev->m_pReader)
    {
       if (dev->m_pReader->allocateBuffer (datasize) != 0)
       {
           delete dev->m_pReader; dev->m_pReader = 0;
       }
    }

    if (dev->m_pReader == 0)
    {
       ALOGE("*** risensors_open::Memory Allocation Error ***\n");

       close (dev->m_mux);
       delete dev; delete pm->pconf;

       return -ENOMEM;
    }

    // spawn calibration monitor thread that will re-load changes from conf file
    iRet = pm->pconf->MonitorCalibration();
    if (iRet != 0)
    {
        ALOGE("*** risensors_open::Calibration Monitor failed to Initialize (%d). Changes will not take effect till next reboot ***\n", iRet);
    }

    // configure initial mode and minimum delay (leaving queue out; not tested enough)
    for (int i = 0; i < 32; i++)
    {
       if (CHECK_BIT(boardmask, i ) )
       {
          RI_SENSOR_HANDLE h = 1 << (i);
          ri_sensors_conf* pconf = pm->pconf->GetConfEntry(h);
          sensor_t*        ps    = pm->pconf->GetSensorEntry(h);

          // sensor mode
          if (pconf->mode != DEFAULT_MODE)
          {
              cmd.handle = h; cmd.short1 = pconf->mode;
              iRet = ioctl (dev->m_mux, PROXMUX_IOCTL_SET_MODE, &cmd);

              if (iRet < 0)
              {
                 ALOGE("*** risensors_open::PROXMUX_IOCTL_SET_MODE error (%d). Could not change reporting mode for sensor [0x%x] to %u ***\n", errno, h, pconf->mode);
              }
          }

          // minimum poll rate
          cmd.handle = h; cmd.long1 = ps->minDelay;
          iRet = ioctl (dev->m_mux, PROXMUX_IOCTL_SET_MIN_DELAY, &cmd);
          if (iRet < 0)
          {
                 ALOGE("*** risensors_open::PROXMUX_IOCTL_SET_MIN_DELAY error (%d). Could not change minimum reporting rate for sensor [0x%x] to %d ***\n", errno, h, ps->minDelay);
          }

  /*        // queue affinity
          if (pconf->rate != DEFAULT_QUEUE)
          {
              cmd.handle = h; cmd.long1 = pconf->rate;
              iRet = ioctl (dev->m_mux, PROXMUX_IOCTL_START_QUEUE, &cmd);

              if (iRet < 0)
                 ALOGE("*** risensors_open::PROXMUX_IOCTL_START_QUEUE error (%d). Could not change reporting mode for sensor [0x%x] to %lu ***\n", errno, h, pconf->rate);
          } */

 

       }
    }

    // rest is now just fill the slots
    dev->device.common.tag      = HARDWARE_DEVICE_TAG;
    dev->device.common.version  = 0;
    dev->device.common.module   = const_cast<hw_module_t*>(module);

    dev->device.common.close    = ISENSORSAPI__close;

    dev->device.activate        = ISENSORSAPI__activate;
    dev->device.setDelay        = ISENSORSAPI__setDelay;
    dev->device.poll            = ISENSORSAPI__poll;

    dev->m_pconf = pm->pconf;              // configuration object reference

    *device = &dev->device.common;

    ALOGI("*** risensors_open::OK ***\n");
    return 0;
}


/************* Sensors API Implementation ***************************

NOTE: Android wants NEGATIVE return values on failure, which MUX already does


*/

/*
 *   libsensors::activate
 *
 *   Android calls this API in HAL layer (us) to signal sensor, identified by passed handle,
 *   should be activated or deactivated. Granularity is 1:1, i.e 1 call per sensor
 * 
 *   Initially, after Android boots up, SensorManager will disable all sensors (1 libsensors::activate call 
 *   per each sensor reported in sensor_module_t::get_sensors_list). As APKs register for sensor events, Android
 *   will enable the sensor & keep reference count; when it drops to 0, disable is called again.
 *
 *   This is directly related to power management; HAL layer simply forwards the call to the driver in Kernel space,
 *   which then performs specific task of turning on/shutting down of the sensor
 *
 *   Implementation forwards the request to Kernel MUX device. For synchronized event 
 *
 */
int ri_sensors_context::activate
(
   int handle,     // sensor ID, as specified in sensors_module_t::get_sensors_list
   int enable      // enable/disable switch
)
{
   int iRet = 0;
   risensorcmd cmd;

   // registration check
   if (m_pconf->GetSensorEntry(handle) == 0)
   {
      ALOGE("*** ri_sensors_context::activate: Unexpected error -- sensor handle [0x%x] not registered! ***\n", handle);
      return -ENXIO;
   }

   // set the parameters. Via this interface we can't manipulate Driver mode (FIFO/CR)
   // PROTOCOL:  handle = sensor, flag = long1, mode = short1 (unused, leave set to 0)
   memset(&cmd, 0, sizeof(risensorcmd) );
   cmd.handle = handle;
   cmd.long1  = enable > 0 ? 1 : 0;

   iRet = ioctl (m_mux, PROXMUX_IOCTL_SET_ENABLE, &cmd);
   if (iRet <0)
   {
      ALOGE("*** PROXMUX_IOCTL_SET_ENABLE error (%d) for Sensor [0x%4x] ***\n", errno, handle);
      return iRet; 
   }

   if (enable == 0) ALOGI("*** Sensor [0x%x] Disabled ***\n", handle);
   else
      ALOGI("*** Sensor [0x%x] Enabled ***\n", handle);

   // spawn calibration monitor thread that will re-load changes from conf file
   iRet = m_pconf->MonitorCalibration();
   if (iRet != 0)
   {
      ALOGE("*** risensors_open::Calibration Monitor failed to Initialize (%d). Changes will not take effect till next reboot ***\n", iRet);
   }

   return 0;
}


   
/*
 *   libsensors::setDelay
 *
 *   Java apps (APKs) that register for Sensor Events can request reporting rate at either one of 4 Android
 *   defined constants (SensorManager.java -- see http://developer.android.com/reference/android/hardware/SensorManager.html
 *   or specify arbitrary integer rate (Google doesn't check!)
 *   The request is then directly forwarded to this API; Granularity is 1:1, i.e. 1 call per sensor
 *
 *   Earlier implementations of Sensor Framework used to do check at HAL layer; this has
 *   now been moved to Kernel (MUX) device
 */
int ri_sensors_context::setDelay
(
   int handle,    // sensor ID, as specified in sensors_module_t::get_sensors_list
   int64_t ns     // poll rate in nano-seconds; conf has it in (Oh infinite Google geeks wisdom) in microseconds, while
                  // everything should really be in miliseconds
)
{
   int iRet = 0;
   risensorcmd cmd;

   unsigned int delay = NSEC_TO_MS(ns);

   // PROTOCOL:  handle = sensor, delay = long1 [ms]
   memset(&cmd, 0, sizeof(risensorcmd) );
   cmd.handle = handle;
   cmd.long1  = delay;

    iRet = ioctl (m_mux, PROXMUX_IOCTL_SET_DELAY, &cmd);

    if (iRet <0)
    {
        ALOGE("*** PROXMUX_IOCTL_SET_DELAY Error: %d for Sensor [0x%x] ***\n", errno, handle);
        return iRet;
    }
    else if(iRet == 0)
        ALOGI("*** Poll Rate for Sensor [0x%x] set to %d ms ***\n", handle, delay);
#if defined RISENS_DEBUG
    else
        ALOGI("*** Poll Rate for Sensor [0x%x] cannot change to %d ms with return code %d ***\n", handle, delay, iRet);
#endif
    return 0;
}

/*  libsensors::pollEvents
 *
 *  Main sensor event multiplexer. Android Sensor Manager calls this API in tight loop (as long as
 *  there is at least 1 registered Java listener).
 *
 *  Return value indicates number of BYTES filled in passed buffer.
 *  This is different when compared to original LIMO version where we propagated sensor structures
 *
 *  We pass retrieved data buffer to Internal object who takes care of caching
 */
int ri_sensors_context::pollEvents
(
   sensors_event_t* data,    // Android allocated buffer -- array of sensors_event_t structures
   int count                 // Event buffer size (number of sensors_event_t structures)
)
{
    unsigned int numEvents = 0;     // running # of events that will be returned to Android. Can't be 0, as we'd block otherwise
    struct pollfd fds;              // mux poll structure
    int iRead = 0;                  // # of unprocessed bytes in reader buffer

    int iEvents = 0;

  //  SENSD("+++ ri_sensors_context::pollEvents  ENTER +++\n");

    // check if there is anything left from previous run first
    if (m_pReader->hasEvents() )
    {
       iEvents = m_pReader->Parse(data, count, m_pconf);
    //   SENSD("+++ ri_sensors_context::pollEvents -- RETURNING %d events to Android +++\n", iEvents);

       return iEvents;
    }
  
    // now BLOCK on mux device
    fds.events = POLLIN;
    fds.fd     = m_mux;

    if (poll (&fds, 1, -1) == -1)       // -1 means infinite
    {
          ALOGE("*** Error polling [%s] (%d). ***\n", PROXMUX_DRIVER_NAME, errno);
          return -errno;
    }

    // here the data is ready. test for POLLIN; this is the only thing MUX knows how to return
    if (!(fds.revents & POLLIN) ) 
    {
          ALOGE("*** Error polling [%s]. Expected 0x%x, got 0x%x ***\n", PROXMUX_DRIVER_NAME, POLLIN, fds.revents);
          return -ENXIO;
    }

    // Ask Event Reader to pull data. Initial driver registration guarantees we pull everything available in one shot 
    iRead = m_pReader->Read(m_mux);
    if (iRead < 0)
    {
          ALOGE("*** Error retrieving Sensor Data from MUX device. ***\n");
          return 0;
    }

    iEvents = m_pReader->Parse(data, count, m_pconf);

/* Debug Plug: Dump Cache */
    //m_pReader->logCache ();

  //  SENSD("+++ ri_sensors_context::pollEvents -- RETURNING %d events to Android +++\n", iEvents);
    return iEvents;
}








