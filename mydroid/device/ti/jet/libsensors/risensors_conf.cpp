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

#include <hardware/sensors.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <errno.h>

#include <pthread.h>
#include <sys/inotify.h>
#include "risensors.h"

#define ARRAY_SIZE(a) (sizeof(a) / sizeof(a[0]))

// various sensor property constants
const char* DEFAULT_NAME   =   "Unknown";
const char* DEFAULT_VENDOR =   "Unknown";

#define     DEFAULT_VERSION     1
#define     DEFAULT_RANGE       0
#define     DEFAULT_RESOLUTION  0
#define     DEFAULT_POWER       0
#define     DEFAULT_DELAY       100   // be safe: In default mode, with no sensors.conf don't let it run faster than 100ms!            

#define     DEFAULT_CONV_A      1.0f  // can't make assumptions about data conversion - it is too specific
#define     DEFAULT_CONV_B      0

#define     DEFAULT_DATA_SIZE   DS_LONG    /* This is now handled more inteligently in code */

// tag names
#define TAG_SENSOR     "[SENSOR]"
#define TAG_HANDLE     "Handle"
#define TAG_NAME       "Name"
#define TAG_VENDOR     "Vendor"
#define TAG_VERSION    "Version"
#define TAG_RANGE      "MaxRange"
#define TAG_RESOLUTION "Resolution"
#define TAG_POWER      "Power"
#define TAG_DELAY      "MinDelay"
#define TAG_DT         "DataType"
#define TAG_CONV_A     "conv_A"
#define TAG_CONV_B     "conv_B"

#define TAG_MUL_A1     "mul_A1"
#define TAG_MUL_A2     "mul_A2"
#define TAG_MUL_A3     "mul_A3"

#define TAG_ROT_A     "rot_A"
#define TAG_ROT_B     "rot_B"
#define TAG_ROT_C     "rot_C"


#define TAG_MODE       "Mode"
#define TAG_QUEUE      "Queue"

#define VAL_DS_SHORT      "DS_SHORT"
#define VAL_DS_LONG       "DS_LONG"
#define VAL_DS_SHORT3     "DS_SHORT3"
#define VAL_DS_TIMESTAMP  "DS_TIMESTAMP"

// other constants
#define COMMENT_CHAR      '#'
#define EQUAL_CHAR        '='
#define CALIBRATION_TOKEN  " "    // separator between configuration (x, y, z) values
#define MATRIX_TOKEN  " ,"

#define LINE_SIZE      250
#define NAME_SIZE      100
#define VALUE_SIZE     100


/***************************************************************************
 *
 *  Sensor configuration. Gets built by synchronyzing "sensors.conf" file and
 *  true board configuration bitmask passed by ioctl to 'MUX' device. 
 *
 *  Only matched entries are reported to Android which guarantees user apps will never
 *  be able toregister for Events of sensor chips that do not exist/failed to probe, etc.
 *
 *
 ***************************************************************************/

/* forward declaration */
struct ri_sensors_module_t;


/* RISensorConf class implementation */
void RISensorsConf::Reset()
{
    if (this->iConfSize)
    {
       if (this->psd)   free(this->psd);
       if (this->pconf) free(this->pconf);

       this->pconf = 0;
       this->iConfSize = 0;
       this->psd = 0;
    }
}

// assigns defaults based on passed mask
int RISensorsConf::ParseDefaults(int mask)
{

   // empty old if any first
   this->Reset();

   // allocate configuration entry array based on number of bits set in passed mask
   for (int i = 0; i < 32; i++)
   {
      if (CHECK_BIT(mask, i) ) this->iConfSize++;
   }

   this->psd   = (sensor_t*)(malloc (this->iConfSize * sizeof (struct sensor_t) ) );
   if (this->psd)
      this->pconf = (ri_sensors_conf*)(malloc (this->iConfSize * sizeof (struct _ri_sensors_conf) ) );

   if (!this->pconf)   // memory allocation error
   {
      this->Reset();
      return -ENOMEM;
   }

   // parse bitmask and assign default properties, because this IS true board configuration
   // we will expose to user space regardless of sensors.conf
   ri_sensors_conf* pc = this->pconf;
   sensor_t*        ps = this->psd;
   int i = 0;

   for (i = 0; i < 32; i++)
   {
      if (CHECK_BIT(mask, i ) )
      {
          this->AssignDefaults(pc, ps, i);
          pc++; ps++;
      }
   }

   // all ok
   return 0;
}

// Main configuration parse function; assigns properties
// Passed file handle is expected to be valid
int RISensorsConf::Parse(FILE* pfh, int printResults)
{

   ri_sensors_conf* pc = this->pconf;
   sensor_t*        ps = this->psd;
   int i = 0;

   for (i = 0; i < this->iConfSize; i++)
   {
       pc = this->pconf + i;
       ps = this->psd + i;

       int iRet = this->ParseSensor(pc, ps, pfh);
       if (iRet != 0)
       {
           ALOGE("*** risensors_conf::Error parsing configuration properties for sensor [0x%x]. Default properties will be assigned ***\n", ps->handle);
           return iRet;
       }
   }

   if(printResults) {
      // diagnostics: Dump board configuration
      SENSD("*** RISensorsConf::Parse Returning %d sensors. Board Configuration: ***\n", this->iConfSize);
      SENSD("*** =============================================================== ***\n\n");

      for (i = 0; i < this->iConfSize; i++)
         this->DumpConf(this->pconf + i, this->psd + i);
   }

   return 0;
}

// parse function that will parse calibrated sensors.conf
int RISensorsConf::ParseCalibrated(int printResult) {
   // open configuration file
   FILE* pfh = fopen (SENSORS_CONF_FILE, "r");
   if (pfh == NULL) {
      ALOGE("*** Error opening [%s]. fopen returned %i. Calibration changes will not take effect till next reboot ***", SENSORS_CONF_FILE, errno);
      return -EEXIST;
   }
   else
   {
      int iRet = this->Parse(pfh, printResult);
      fclose (pfh);

      if(iRet != 0) {
         ALOGE("*** [%s] Reload Error. fclose() returned %i. Calibration changes will not take effect till next reboot ***", SENSORS_CONF_FILE, errno);
         // open SNESOR_CONFFILE and delete because it is corrupted
         if(remove(SENSORS_CONF_FILE) != 0)
            ALOGE("*** [%s] Deleting of file failed. ***", SENSORS_CONF_FILE);
         else
            ALOGI("*** Sensor Library: calibrated sensors.conf is deleted successfully ***");

         return -EINVAL;
      }
      else {
         ALOGI("*** Sensor Library: Calibration Monitor successfully reloaded changes ***");
         return 0;
      }
   }
}

// checks valid open configuration file for specified entry.
// returns 1 if found, 0 otherwise. Passed file handle is expected
// to be valid and open
int  RISensorsConf::HasEntry
(
   FILE*            pfh, 
   RI_SENSOR_HANDLE sensor
)
{
   ri_sensors_conf sc;
   sensor_t        sn; 

   this->AssignDefaults(&sc, &sn, sensor);

   return (this->ParseSensor(&sc, &sn, pfh) == 0) ? 1 : 0;

}

/* Internal helper to parse value that can be either hex or decimal */
int RISensorsConf::ParseHexField (char* p)
{
   int base = 10; char* start = p;

   // both hex or decimal are supported
   while (*p )
   {
      if (isxdigit(*p) )
      {
          base = 16;
          break;
      }
      p++;
   }

   return strtol(start, &p, base);
}

/* Internal helper to parse conf properties for current entry */
int RISensorsConf::ParseSensor(ri_sensors_conf* pc, sensor_t* ps, FILE* fp)
{
   int inSection = 0;
   long  sect_offset = 0;
   int found = 0;

   char line  [LINE_SIZE];
   char namebuf  [NAME_SIZE];   char* name  = &(namebuf[0]);
   char valuebuf [VALUE_SIZE];  char* value = &(valuebuf[0]);

   // first rewind to start of file
   fseek(fp, 0L, SEEK_SET);
 
   while ( fgets(line, sizeof(line), fp ) )
   {
       // remove trailing newline character fgets gives us
       line[strlen(line) - 1] = '\0';

       // pre-process the line: Strip blanks and comments
       char* start = this->Trim(line);

       if (*start != '\0')   // don't even consider pure comment lines
       {
          // if not in section look for section start, indicated by TAG_SENSOR
	  if (inSection == 0)
	  {
             if (strcmp(start, TAG_SENSOR) == 0)
             {
                inSection = 1;
                sect_offset = ftell(fp);  
	     }
	  }

	  // otherwise look for TAG_HANDLE
	  else
	  {
              this->Tokenize(line, namebuf, valuebuf);

              name = this->Trim(namebuf); 
              value = this->Trim(valuebuf);

              if ( (strcasecmp(name, TAG_HANDLE) == 0) )  
              {
                 int h = this->ParseHexField(value);
                 if (h == ps->handle)
                 {
		   //		   ALOGE(""+ps->handle);
                    found = 1;
                    break;
                 }
                 else
                 {
                     // not our handle, so consider we are not in section
                     inSection = 0;
                     sect_offset = 0;
                 }
              }

	  }  

        }  // if (*start != '\0')

   }  // while

   if (found == 0)
      return -ENXIO;

   this->AssignDefaultsConv(pc);

   // rewind to start of section. This is just past the line with TAG_SENSOR
   fseek (fp, sect_offset, SEEK_SET);

   // read and attempt to parse all lines till start of next section
   while (fgets(line, sizeof(line), fp) )
   { 
      // remove trailing newline character fgets gives us
      line[strlen(line) - 1] = '\0';

      char* start = this->Trim(line);
      if (*start != '\0')
      {
          // if we got another SENSOR_TAG, quit
          if (strcmp(start, TAG_SENSOR) == 0)
             break;

          // tokenize the line
          this->Tokenize(line, namebuf, valuebuf);

          name  = this->Trim(namebuf); 
          value = this->Trim(valuebuf);

          // extract different properties
          if (name[0] && value[0])
             this->AssignProperty(pc, ps, name, value);
      }
   }
   
   // all ok here
   return 0;
}

// property parser. Both name and value are guaranteed to be non-empty, left and right trimmed
void  RISensorsConf::AssignProperty(ri_sensors_conf* pc, sensor_t* ps, char* name, char* value)
{
    // cycle the tags. Ignore TAG_HANDLE because it's been parsed by this point
    if (strcasecmp(name, TAG_HANDLE) == 0) return;

    else if (strcasecmp(name, TAG_NAME) == 0)
        strcpy(pc->namebuf, value);

    else if (strcasecmp(name, TAG_VENDOR) == 0)
        strcpy(pc->vendorbuf, value);

    else if (strcasecmp(name, TAG_VERSION) == 0)
        ps->version = atoi(value);

    else if (strcasecmp(name, TAG_RANGE) == 0)
        ps->maxRange = atof(value);

    else if (strcasecmp(name, TAG_RESOLUTION) == 0)
        ps->resolution = atof(value);

    else if (strcasecmp(name, TAG_POWER) == 0)
        ps->power = atof(value);

    else if (strcasecmp(name, TAG_MODE) == 0)
        pc->mode = atoi(value);

    else if (strcasecmp(name, TAG_QUEUE) == 0)
        pc->rate = atoi(value);

    else if (strcasecmp(name, TAG_DELAY) == 0)
    {
        /* Data validation: Delay can not be negative */
        ps->minDelay = atoi(value);
        if (ps->minDelay < 0)
        {
           ps->minDelay = DEFAULT_DELAY;
           ALOGE("*** RISensorsConf::AssignProperty: Min Delay can not be negative value. Sensor [0x%x], Delay [%s] ***\n",
                 ps->handle, value);
        }
    }

    else if (strcasecmp(name, TAG_DT) == 0)
    {
       if (strcmp(value, VAL_DS_SHORT) == 0)
         pc->dataSize = DS_SHORT;

       else if (strcmp(value, VAL_DS_LONG) == 0)
         pc->dataSize = DS_LONG;

       else if (strcmp(value, VAL_DS_SHORT3) == 0)
         pc->dataSize = DS_SHORT3;

       else if (strcmp(value, VAL_DS_TIMESTAMP) == 0)
         pc->dataSize = DS_TIMESTAMP;

       else   // log we got invalid data size
       {
         SENSD("*** RISensorsConf::AssignProperty --- Ignoring Unrecognized Data Size [%s] ***\n", value);
         return;
       }
    }

    else if (strcasecmp(name, TAG_CONV_A) == 0)
        this->ParseCalibration(pc->conv_A, value);

    else if (strcasecmp(name, TAG_CONV_B) == 0)
        this->ParseCalibration(pc->conv_B, value);

    else if(strcasecmp(name, TAG_MUL_A1) == 0)
    {
        pc->multiplier_matrix_enabled |= (this->ParseMatrixRow(pc->mul_A1, value) == 3);
    }
    else if (strcasecmp(name, TAG_MUL_A2) == 0)
    {
        pc->multiplier_matrix_enabled |= ((this->ParseMatrixRow(pc->mul_A2, value) == 3) << 1);
    }
    else if (strcasecmp(name, TAG_MUL_A3) == 0)
    {
        pc->multiplier_matrix_enabled |= ((this->ParseMatrixRow(pc->mul_A3, value) == 3) << 2);
    }

    else if (strcasecmp(name, TAG_ROT_A) == 0)
    {
        pc->rot_enabled |= (this->ParseMatrixRow(pc->rot_A, value) == 3);
    }
    else if (strcasecmp(name, TAG_ROT_B) == 0)
    {
        pc->rot_enabled |= ((this->ParseMatrixRow(pc->rot_B, value) == 3) << 1);
    }
    else if (strcasecmp(name, TAG_ROT_C) == 0)
    {
        pc->rot_enabled |= ((this->ParseMatrixRow(pc->rot_C, value) == 3) << 2);
    }

        // ParseMatrixRow returns the number of values parsed (0,1,2,3)
        // for a 3x3 matrix, each needs to collect all three values
        // if all three values are bit shifted correctly, pc->rot_enabled
        // will be equal to MATRIX_PARSE_COMPLETE (0b111)

    else
    {
        // log that we got unrecognized tag
        SENSD("*** RISensorsConf::AssignProperty --- Ignoring Unrecognized Tag [%s] ***\n", name);
        return;
    }

}

/* internal helper: Parses and assigns calibration adjustments. Line is passed with 3 values token separated
   buffer to be assigned has already defaults. Absence means inheritance from previous value). So following applies
   
   conv_A      = .303             #   x = 0.303  y = .303  z = .303
   conv_A      = .303 .503        #   x = 0.303  y = .503  z = .503
   conv_A      = .303 .503 .303   #   x = 0.303  y = .503  z = .303
*/
void RISensorsConf::ParseCalibration(float* cal, char* value)
{
   // X axis
   char* entry = strtok(value, CALIBRATION_TOKEN);
   if (!entry) return;

   // assign inherit values to all 3
   cal[0] = cal[1] =  cal[2] = atof(entry);

   // Y axis
   entry = strtok(NULL, CALIBRATION_TOKEN);
   if (!entry) return;
   cal[1] = cal[2] = atof(entry);

   // Z axis
   entry = strtok(NULL, CALIBRATION_TOKEN);
   if (!entry) return;
   cal[2] = atof(entry);
}

int RISensorsConf::ParseMatrixRow(float* cal, char* value)
{

   // X axis
   char* entry = strtok(value, MATRIX_TOKEN);
   if (!entry) return 0;

   // assign inherit values to all 3
   cal[0] = cal[1] =  cal[2] = atof(entry);

   // Y axis
   entry = strtok(NULL, MATRIX_TOKEN);
   if (!entry) return 1;
   cal[1] = cal[2] = atof(entry);

   // Z axis
   entry = strtok(NULL, MATRIX_TOKEN);
   if (!entry) return 2;
   cal[2] = atof(entry);
   return 3;
}

// Pre-process: Throwaway comments; trim left/right blanks
char* RISensorsConf::Trim 
(
   char* line /* parsed line, null-terminated */
)
{
   char* start = line;
   char* end = start + strlen(line);

   // first run through entire line & set end of line wherever we find comment
   while (start < end)
   {
       if (*start == COMMENT_CHAR)
       {
          *start = '\0';
          end = start;

          break;
       }

       start++;
   }

   // reposition start
   start = line;

   // left trim
   while (start < end)
   {
      // exit left hunt with first non-blank char. Comments are already processed
      if ( (*start != ' ') ) break;
      start++;
   }

   // right trim
   end--;
   while (end > start)
   {
      // exit with first non-blank, non-comment char
      if ( (*end != ' ') ) break;
      
      end--;
   }

   *(end + 1) = '\0';
   return start;
}

/* Tokenize read line into name=value pair */
void RISensorsConf::Tokenize(char* line, char* name, char* value)
{
   // blank results first. Note -- line has been comment stripped and trimmed
   // and is guaranteed to be non-zero length
   name[0] = value[0] = '\0';
   char* delimiter = line;

   // look for EQUAL_CHAR
   while (*delimiter)
   {
      if ( (*delimiter) == EQUAL_CHAR)
          break;

      delimiter++;
   }
  
   if (*delimiter)
   {
      strncpy(name, line, delimiter - line - 1);
      name[delimiter-line-1] = '\0';

      strcpy(value, delimiter+1);
   }
}

/* Internal helper to assign default sensor properties */
void RISensorsConf::AssignDefaultsConv(ri_sensors_conf* pc)
{
   pc->conv_A[0] = pc->conv_A[1] = pc->conv_A[2]   = DEFAULT_CONV_A;
   pc->conv_B[0] = pc->conv_B[1] = pc->conv_B[2]   = DEFAULT_CONV_B;
   pc->rot_enabled = 0;
   pc->multiplier_matrix_enabled = 0;
}

/* Internal helper to assign default sensor properties */
void RISensorsConf::AssignDefaults(ri_sensors_conf* pc, sensor_t* ps, int i)
{
   strcpy(pc->namebuf,   DEFAULT_NAME);
   strcpy(pc->vendorbuf, DEFAULT_VENDOR);

   pc->conv_A[0] = pc->conv_A[1] = pc->conv_A[2]   = DEFAULT_CONV_A;
   pc->conv_B[0] = pc->conv_B[1] = pc->conv_B[2]   = DEFAULT_CONV_B;
   
   // identity matrix
   pc->rot_A[0] = 1; pc->rot_A[1] = 0; pc->rot_A[2] = 0;
   pc->rot_B[0] = 0; pc->rot_B[1] = 1; pc->rot_B[2] = 0;
   pc->rot_C[0] = 0; pc->rot_C[1] = 0; pc->rot_C[2] = 1;

   pc->rot_enabled = 0;
   pc->multiplier_matrix_enabled = 0;
   ps->version          = DEFAULT_VERSION;
   ps->handle           = 1 << (i);
   ps->type             = i + 1;
   ps->maxRange         = DEFAULT_RANGE;       
   ps->resolution       = DEFAULT_RESOLUTION;        
   ps->power            = DEFAULT_POWER;    
   ps->minDelay         = DEFAULT_DELAY;    

   pc->mode             = DEFAULT_MODE; 
   pc->rate             = DEFAULT_QUEUE;

   ps->name   = pc->namebuf;
   ps->vendor = pc->vendorbuf;

  // modify data size to be a bit more intelligent based on sensor handle
  switch (ps->handle)
  {
     case RI_SENSOR_HANDLE_ACCELEROMETER:
     case RI_SENSOR_HANDLE_MAGNETOMETER:
     case RI_SENSOR_HANDLE_GYROSCOPE:
       pc->dataSize = DS_SHORT3;
       break;

     case RI_SENSOR_HANDLE_PRESSURE:
     case RI_SENSOR_HANDLE_TEMPERATURE:
       pc->dataSize = DS_LONG;
       break;

     case RI_SENSOR_HANDLE_FREEFALL:
       pc->dataSize = DS_TIMESTAMP;
       break;

     default:
       ALOGI("+++ Unknown Sensor [0x%x] -- assigning default data size [%d bytes] +++\n", ps->handle, DEFAULT_DATA_SIZE);
       pc->dataSize = DEFAULT_DATA_SIZE;
  }
}

/* Debug: LOG conf entry we parsed */
void  RISensorsConf::DumpConf (ri_sensors_conf* pc, sensor_t* ps)
{
    SENSD("\n");

    SENSD("*** RISensorsConf -- Configuration Entry ***\n");
    SENSD("*** ==================================== ***\n");

    SENSD("[%s] = [0x%x]\n", TAG_HANDLE, ps->handle);
    SENSD("[%s] = [%s]\n", TAG_NAME, ps->name);
    SENSD("[%s] = [%s]\n", TAG_VENDOR, ps->vendor);
    SENSD("[%s] = [%d]\n", TAG_VERSION, ps->version);
    SENSD("[%s] = [%f]\n", TAG_RANGE, ps->maxRange);
    SENSD("[%s] = [%f]\n", TAG_RESOLUTION, ps->resolution);
    SENSD("[%s] = [%f]\n", TAG_POWER, ps->power);
    SENSD("[%s] = [%d]\n", TAG_DELAY, ps->minDelay);

    SENSD("[%s] = [%u]\n", TAG_MODE,  pc->mode);
    SENSD("[%s] = [%lu]\n", TAG_QUEUE, pc->rate);

    SENSD("[%s] = [%d]\n", TAG_DT, pc->dataSize);
    SENSD("[%s] = [%f]\n", TAG_CONV_A, pc->conv_A[0]); 
    SENSD("[%s] = [%f]\n", TAG_CONV_B, pc->conv_B[0]);

    if(pc->multiplier_matrix_enabled == MATRIX_PARSE_COMPLETE)
    {
        SENSD("multilplier matrix:\n");
        SENSD("%f  %f  %f\n", pc->mul_A1[0],pc->mul_A1[1],pc->mul_A1[2]); 
        SENSD("%f  %f  %f\n", pc->mul_A2[0],pc->mul_A2[1],pc->mul_A2[2]); 
        SENSD("%f  %f  %f\n", pc->mul_A3[0],pc->mul_A3[1],pc->mul_A3[2]); 
    }

    if (pc->rot_enabled == MATRIX_PARSE_COMPLETE)
    {
        SENSD("rotation matrix:\n");
        SENSD("%f  %f  %f\n", pc->rot_A[0],pc->rot_A[1],pc->rot_A[2]); 
        SENSD("%f  %f  %f\n", pc->rot_B[0],pc->rot_B[1],pc->rot_B[2]); 
        SENSD("%f  %f  %f\n", pc->rot_C[0],pc->rot_C[1],pc->rot_C[2]); 
    }

    // for multi-axis, dump other calibration values as well
    if (pc->dataSize == DS_SHORT3)
    {
       SENSD("[%s](1)= [%f]\n", TAG_CONV_A, pc->conv_A[1]); 
       SENSD("[%s](1) = [%f]\n", TAG_CONV_B, pc->conv_B[1]);

       SENSD("[%s](2) = [%f]\n", TAG_CONV_A, pc->conv_A[2]);
       SENSD("[%s](2) = [%f]\n", TAG_CONV_B, pc->conv_B[2]);
    }


    SENSD("*** ==================================== ***\n");
    SENSD("\n");
}


// Management of Calibration Monitor Thread
void* calhandler_thread(void* arg)
{
   // get ourselves
   RISensorsConf* pThis = (RISensorsConf*)arg;

   SENSD("Sensor Library: Calibration Monitor starting...");
   int bmodified = 0;
   char buf[100 * (sizeof (struct inotify_event) + 16)]; // 100 events at a time is likely overkill
   int i = 0;

   int len = read (pThis->inot, buf, sizeof(buf) );

   while (i < len)
   {
       struct inotify_event* event = (struct inotify_event*) &buf[i];

       if (event->len)
            SENSD("+++ Calibration Monitor Notify Event name=%s +++", event->name);

       SENSD("+++ Calibration Monitor Notify Event: wd=%d mask=0x%x cookie=%u len=%u +++",
                event->wd, event->mask,
                event->cookie, event->len);

       // is this what we are interested in?
       if ( ((event->mask & IN_CLOSE_WRITE) == IN_CLOSE_WRITE) ||     // this is normal case -- runtime calibration app modifications
            ((event->mask & IN_IGNORED) == IN_IGNORED) )    // this is "super" debug case -- when I simply do "mv"
       {
           bmodified = 1;
           break;
       }

       i += (sizeof (struct inotify_event)) + event->len;
    }

    // now if modified flag is set, reload the configuration file
    if (bmodified == 1)
    {
         ALOGI("*** Calibration Monitor Signaled. Reloading [%s] configuration file ***", SENSORS_CONF_FILE);

         pThis->ParseCalibrated(1);
    }

   // close and restart watch
   inotify_rm_watch(pThis->inot, IN_CLOSE_WRITE);
   close (pThis->inot); pThis->inot = -1;

   pThis->calthread = -1;
   i = pThis->MonitorCalibration();
   if (i != 0)
   {
       ALOGE("*** calhander_thread::Calibration Monitor failed to Initialize (%d). Further changes will not take effect till next reboot ***\n", i);
   }

   pthread_exit(0);
   return NULL;
}

int RISensorsConf::MonitorCalibration()
{
    if (this->calthread != -1)
    {
       SENSD("+++ calhandler_thread is already set, ignoring request +++");
       return 0;
    }

    // initialize notifier
    this->inot = inotify_init();
    if (this->inot == -1)
    {
        ALOGE("+++ inotify_init() error: [%d] +++", -errno);
        return -errno;
    }

    // add watch when file gets modified
    if (inotify_add_watch (this->inot, SENSORS_CONF_FILE, IN_CLOSE_WRITE) < 0)
    {
        ALOGE("+++ inotify_add_watch() error: [%d] +++", -errno);
        return -errno;
    }

    // spawn monitor thread; parameter are us (this pointer)
    if (pthread_create(&(this->calthread), NULL, &calhandler_thread, (void*)this) != 0)
    {
        ALOGE("+++ Calibration Monitor Thread creation error: [%d] +++", -errno);
        return -errno;
    }

    // all ok here
    ALOGI("*** Calibration Monitor Initialized OK ***");
    return 0;
}



// list is  parsed / synhronized
int sensors__get_sensors_list
(
   struct sensors_module_t* module,
   struct sensor_t const**  list
)
{
   return ((ri_sensors_module*)module )->pconf->GetDS((sensor_t**)list);
}





/*****************************************************************************/

