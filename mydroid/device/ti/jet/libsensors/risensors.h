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
 *  Main user space sensor library header file
 *
 ***************************************************************************/
#ifndef __RISENSORS_H__
#define __RISENSORS_H__

#define LOG_TAG "SENSORS.JET"   // our identification for logcat (adb logcat SENSORS.JET:V *:S)

/*  Include Files */
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>
#include <math.h>
#include <fcntl.h>

#include <sys/cdefs.h>
#include <sys/types.h>
//#include <sys/stat.h>
#include <linux/input.h>
#include <poll.h>
#include <utils/Log.h>


#include <hardware/hardware.h>

#include <hardware/sensors.h>  // Google Android Interface
#include "risensors_def.h"     // RECON Framework Interface

#include <utils/KeyedVector.h>

using namespace android;

/* MUX include is same source for both user and kernel; 
   Linux doesn't like if we include kernel header in user space
   so I'm maintaining separate copy */ 

//#define RISENS_DEBUG 1         // comment to stop all diagnostics

#if defined RISENS_DEBUG
   #define SENSD ALOGI
#else
   #define SENSD    // nothing
#endif

#define CALIBRATION_SEMAPHORE "senscal"                  // named semaphore for singalling from calibration app(s)
#define SENSORS_CONF_FILE_DEFAULT "/system/lib/hw/sensors.conf"  // full path to sensors configuration file
#define SENSORS_CONF_FILE         "/data/system/sensors.conf"    // full path to calibrated sensors configuration file

#define MATRIX_PARSE_COMPLETE 0x7
typedef unsigned long             SENSOR_QUEUE_RATE;

#define     DEFAULT_MODE        0    // starting mode -- whatever driver wants
#define     DEFAULT_QUEUE       0    // no custom queue

/*
 * Configuration structure, as parsed from sensors.conf
 *
 */
typedef enum _risens_DS
{
   DS_SHORT  = 2,    // 2 bytes: first float
   DS_LONG   = 4,    // 4 bytes: first float
   DS_SHORT3 = 6,    // 6 bytes: first 3 floats

   DS_TIMESTAMP = 8,   // Timestamp when event occured, stored as 8 contigous bytes of timespec structure

   // extend if needed
}risens_DS;

typedef struct _ri_sensors_conf
{
    risens_DS         dataSize;      // event data size

    SENSOR_QUEUE_RATE rate;          // initial custom queue rate
    RI_SENSOR_MODE    mode;          // sensor mode 
  
    // data conversion: A * val + B (val is measurement returned from Kernel)
    // if not specified, no conversion is performed

    float  conv_A[3];    // utilized size is based on dataSize. Currently DS_SHORT3 = 3 floats, rest always 1
    float  conv_B[3];
    //Orientation Matrix
    float  rot_A[3];
    float  rot_B[3];
    float  rot_C[3];
    int rot_enabled;
    //Multiplier Matrix. Note: this matrix can replace conv_A for multiplier conversion
    float  mul_A1[3];
    float  mul_A2[3];
    float  mul_A3[3];
    int multiplier_matrix_enabled;
    // name, vendor buffers
    char namebuf[50];
    char vendorbuf[50];

}ri_sensors_conf;

/*
 * Configuration object. Could have been implemented as singleton; I am keeping reference
 *                       in both device and module 
 */
class RISensorsConf
{
   friend void* calhandler_thread(void* arg);

   protected:
      sensor_t*         psd;              // properties -- OEM Datasheet
      ri_sensors_conf*  pconf;            // configuration array, (re)built with Parse
      int               iConfSize;        // size of configuration array
      int               inot;             // async notification "handle" for sensors.conf changes
      pthread_t         calthread;

      void  Reset();                                                   // deallocate current configuration array
      void  AssignDefaultsConv(ri_sensors_conf* pc);
      void  AssignDefaults(ri_sensors_conf* pc, sensor_t* ps, int i);  // assign default values to current entry
      int   ParseSensor(ri_sensors_conf*, sensor_t*, FILE* fh);        // parse and assing CONF values to current entry

      char* Trim(char* line);                                          // right/left trim line of spaces
      void  Tokenize(char* line, char* name, char* value);             // tokenize line into name = value

      void  AssignProperty(ri_sensors_conf* pc, sensor_t* ps, char* name, char* value);  // property parser
      int   ParseHexField(char* p);

      void  ParseCalibration(float* cal, char* value);
      int  ParseMatrixRow(float* cal, char* value);

   public:
     RISensorsConf():psd(0), pconf(0), iConfSize(0), inot(-1), calthread(-1) {}
     ~RISensorsConf(){this->Reset();}

     // get accessors
     inline int  GetConf (ri_sensors_conf** ppconf){*ppconf = pconf; return iConfSize;}
     inline int  GetDS   (sensor_t** ppsd){*ppsd = psd; return iConfSize;}

     // set -- build configuration by synchronyzing conf file and passed bitmask
     int  Parse(FILE* pfh, int printResults);
     int  ParseCalibrated(int printResults);
     int  ParseDefaults(int mask);
     int  HasEntry(FILE* pfh, RI_SENSOR_HANDLE sensor); 

     // diagnostic -- dump conf to log
     void  DumpConf (ri_sensors_conf* pc, sensor_t* ps);

     // accessor: get configuration entry for passed handle
     inline ri_sensors_conf*  GetConfEntry (RI_SENSOR_HANDLE handle)
     {
	  for (int i = 0; i < this->iConfSize; i++)
	  {
	       if ( (this->psd + i)->handle == handle)  return (this->pconf + i);
	  }

	  return 0;   // not found
     }

     inline sensor_t*  GetSensorEntry (RI_SENSOR_HANDLE handle)
     {
	  for (int i = 0; i < this->iConfSize; i++)
	  {
	       if ( (this->psd + i)->handle == handle)  return (this->psd + i);
	  }

	  return 0;   // not found
     }

     // public export to start (and stop later for completeness) calibration monitor thread
     int MonitorCalibration ();
     
};


/*  Event Reader */
class RIEventReader
{
   private:
      unsigned char* m_pdata;                   // Data buffer for I/O across User/Kernel barrier
      unsigned int   m_buffersize;              // Data buffer size. Retrieved via initial PROXMUX_IOCTL_GET_CONFIG
      unsigned int   m_datasize;                // Current data size. <= buffersize
      unsigned int   m_offset;                  // current offset into processed array. In case google poll buffer is smaller

      // context -- if google buffer got filled before entire chunk of FIFO events was processed
      RI_SENSOR_HANDLE m_ctxhandle;           // current handle
      RI_DATA_SIZE     m_ctxsize;             // events left

      // "Private" helpers
      int          data_handler   (sensors_event_t* pa, RISensorsConf* pConf);

      void         fill_common    (sensors_event_t* pa);
      void         filter_data    (sensors_event_t* pa,  ri_sensors_conf* pconf);

   public:
      RIEventReader();
      ~RIEventReader();

      int hasEvents(){return m_datasize;}

      int  allocateBuffer (unsigned int size);  
      int  Read (int handle);

      int  Parse(sensors_event_t* data, int count, RISensorsConf* pConf);
};

/*
 * Context singleton that carries over to Android Sensor Manager and binds everything together 
 *
 */
struct ri_sensors_context
{
    // mandatory first entry
    struct sensors_poll_device_t device;

    // mux device handle
    int m_mux;

    // external sensor library API, as expected by Android
    int activate   (int handle, int enabled);
    int setDelay   (int handle, int64_t ns);
    int pollEvents (sensors_event_t* data, int count);

    // configuration entry reference
    RISensorsConf*     m_pconf;

    // Event Reader. Takes care of parsing, caching, etc
    RIEventReader*     m_pReader;

};

/* RI Sensors Module Definition */
struct ri_sensors_module
{
   struct hw_module_t common;
   int (*get_sensors_list)(struct sensors_module_t*, struct sensor_t const**);

   RISensorsConf*     pconf;
};


// some utilities
class RISensorUtilities
{
   // helper that returns timestamp -- used by event readers
   public:

   static int64_t getTimestamp() 
   {
      struct timespec t;
      t.tv_sec = t.tv_nsec = 0;
      clock_gettime(CLOCK_REALTIME, &t);

      return int64_t(t.tv_sec)*1000000000LL + t.tv_nsec; 

   }

   static int TypeFromHandle(RI_SENSOR_HANDLE h)
   {
       for (int i = 0; i < 32; i++)
       {
          if (CHECK_BIT(h, i) )
             return i+1;
       }

       return 0;
   }

};





#endif  // __RISENSORS_H__
