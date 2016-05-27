/*
 * Copyright (C) 2009 The Android Open Source Project
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
 *
 */

#include <stdio.h>
#include <stdint.h>
#include <errno.h>
#include <math.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#include <linux/input.h>
#include <linux/fcntl.h>
#include <linux/time.h>

#include <string.h>
#include <poll.h>

typedef unsigned char u8;
#define GRAVITY_EARTH (9.80665f)   // from google sensors.h
#define ACC_FACTOR   .12f
#define GYRO_FACTOR  .07f
#define MAG_FACTOR   .016f

#define MODE_ENABLE       '1'
#define MODE_DISABLE      '2'
#define MODE_DELAY        '3'
#define MODE_POLL         '4'
#define MODE_CONFIG       '5'

#define MODE_START_QUEUE  '6'
#define MODE_STOP_QUEUE   '7'

#define SENSOR_EVENT_SIZE 20


#define IS_VALID_MODE(m) \
    ((m == MODE_DISABLE) || (m == MODE_ENABLE) || (m == MODE_DELAY) || \
    (m == MODE_POLL) || (m == MODE_START_QUEUE) || (m == MODE_STOP_QUEUE) || (m == MODE_CONFIG) )

#define SENSOR_DATA_SIZE 1000    // max data size across all nodes in MUX; have it returned via IOCTL!!!

#include "risensors_def.h"

// forward declarations
int pollEvents (int fd);
int usage();
int enable (int, RI_SENSOR_HANDLE handle, unsigned int enable, unsigned short mode);
int setDelay (int, RI_SENSOR_HANDLE, unsigned int);
int start_queue (int, RI_SENSOR_HANDLE, unsigned int);
int get_config(int);
int stop_queue (int, unsigned int);
void dumpevents (unsigned char* databuffer, ssize_t readsize);


int main (int argc, char** argv)
{ 
   int ret = 0;
   int mux = 0;
   RI_SENSOR_HANDLE handle = 0x00;
   char node [100];
   char mode;
   unsigned int delay = 0;
   RI_SENSOR_MODE reporting = 0;     // leave the same if not specified

   // what mode are we
   if (argc == 1)
      return usage ();

   mode = *(argv[1]); 

   if (!IS_VALID_MODE(mode) )
   {
      printf("Invalid mode: %c\n", mode);
      return usage ();
   }

   // config and poll is ok with 1

   // enable/disable/stop queue needs sensor handle / queue id
   if (  ( mode == MODE_DISABLE  || mode == MODE_ENABLE || mode == MODE_STOP_QUEUE) && (argc == 2) ) 
      return usage ();

   // set delay / start queue needs timeout in [ms]
   if ( ( mode == MODE_DELAY  || (mode == MODE_START_QUEUE) ) && (argc <= 3) )
      return usage (); 
   
   // parse params
   if ( (mode != MODE_CONFIG) && (mode != MODE_POLL) )
   {
      int val = atoi(argv[2]);
      if (mode == MODE_START_QUEUE)
         handle = val;
      else
         handle = 1 << (val - 1);
   }

   if ( (mode == MODE_DELAY) || (mode == MODE_START_QUEUE) )
      delay = atoi(argv[3] );

   if ( (mode == MODE_STOP_QUEUE) )
      delay = atoi (argv[2] );

   if ( (mode == MODE_ENABLE) && (argc > 3) )
      reporting = atoi(argv[3] );

   // now open the mux node
   sprintf (node, "/dev/%s", PROXMUX_DRIVER_NAME);
   mux = open (node, O_RDONLY);

   if (mux == -1)
   {
      printf ("Error opening [%s] (%d). \n", node, errno);
      return -errno;
   }


   switch (mode)
   {
      case MODE_DISABLE:
          ret = enable (mux, handle, 0, reporting);
          break;

      case MODE_ENABLE:
      {
          ret = enable (mux, handle, 1, reporting);
          break;
      }

      case MODE_DELAY:
          ret = setDelay(mux, handle, delay);
          break;

      case MODE_POLL:
          ret = pollEvents (mux);
          break;

     case MODE_CONFIG:
          ret = get_config(mux);
          break;


      case MODE_START_QUEUE:
      {
          ret = start_queue (mux, handle, delay);
          break;
      }

 
     case MODE_STOP_QUEUE:
          ret = stop_queue (mux, delay);
          break; 
      
      default:
          break;
   }

   close (mux);
   return ret;
}



// Test PROXMUX_IOCTL_SET_ENABLE
int enable (int mux, RI_SENSOR_HANDLE handle, unsigned int option, RI_SENSOR_MODE mode)
{
   risensorcmd cmd;
   int iRet = 0;

   cmd.handle = handle;
   cmd.long1  = option;
   cmd.short1 = mode;
 
   iRet = ioctl (mux, PROXMUX_IOCTL_SET_ENABLE, &cmd);
   if (iRet == 0)
   {
      if (option == 0) printf("Sensor [0x%x] Disabled\n", handle);
      else
         printf("Sensor [0x%x] Enabled\n", handle);
   }
   else
      printf ("PROXMUX_IOCTL_SET_ENABLE error: %d\n", iRet);

   return iRet;
}

// Test PROXMUX_IOCTL_SET_DELAY 
int setDelay(int mux, RI_SENSOR_HANDLE handle, unsigned int delay)
{
   int iRet = 0;
   risensorcmd cmd;

   cmd.handle = handle;
   cmd.long1  = delay;

   iRet = ioctl (mux, PROXMUX_IOCTL_SET_DELAY, &cmd);
   if (iRet == 0) 
     printf("Poll Rate for Sensor [0x%x] set to %d ms\n", handle, delay);
   else
     printf("PROXMUX_IOCTL_SET_DELAY Error: %d\n", iRet);

   return iRet;
}


// Test PROXMUX_IOCTL_START_QUEUE
int start_queue (int mux, RI_SENSOR_HANDLE mask, unsigned int delay)
{
   int iRet = 0;
   risensorcmd cmd;

   cmd.handle = mask;
   cmd.long1  = delay;

   iRet = ioctl (mux, PROXMUX_IOCTL_START_QUEUE, &cmd);
   if (iRet == 0) 
     printf("Custom Queue with Sensor Mask [0x%x] started with [%d] Reporting Rate (ms)\n", mask, delay);
   else
     printf("PROXMUX_IOCTL_START_QUEUE Error: %d\n", iRet);

   return iRet;
}

// Test PROXMUX_IOCTL_STOP_QUEUE
int stop_queue (int mux, unsigned int delay)
{
   int iRet = 0;
   risensorcmd cmd;
   cmd.long1  = delay;

   iRet = ioctl (mux, PROXMUX_IOCTL_STOP_QUEUE, &cmd);
   if (iRet == 0) 
     printf("Custom Queue with [%d] Reporting Rate (ms) stopped\n", delay);
   else
     printf("PROXMUX_IOCTL_STOP_QUEUE Error: %d\n", iRet);

   return iRet;
}

// Test PROXMUX_IOCTL_GET_CONFIG
int get_config(int mux)
{
   int iRet = 0;
   risensorcmd cmd; 
   memset(&cmd, 0, sizeof(risensorcmd) );

   iRet = ioctl (mux, PROXMUX_IOCTL_GET_CONFIG, &cmd);
   if (iRet == 0)
   {
      int i = 0;   
      printf("Mask of Registered Sensors: 0x%x, Total Data Size: %d bytes\n", cmd.handle, cmd.long1);
      printf("===================\n");

      for (i = 0; i < 32; i++)
      {
         if (CHECK_BIT(cmd.handle, i) )
         {
            // get current status for this sensor
            risensorcmd stat;
            memset(&stat, 0, sizeof(risensorcmd) );

            stat.handle = 1 << i;

            iRet = ioctl (mux, PROXMUX_IOCTL_GET_STATUS, &stat);
            if (iRet)
            {
                printf("PROXMUX_IOCTL_GET_STATUS Error: %d\n", iRet);
                return iRet;
            }
            printf("ID: %d, Handle: [0x%02x]. Poll Rate: %d [ms]. Min Poll Rate: %d [ms]. Enabled: [%d], Buffer Size: [%d], Supported Modes: [0x%x], Current Mode: [0x%x]\n", 
               i + 1, stat.handle, stat.long1, stat.long2, stat.short1, stat.short2, stat.short3, stat.short4);
         }
      }
   }
   else
     printf("PROXMUX_IOCTL_GET_CONFIG Error: %d\n", iRet);

   return iRet;
}

// Test poll '/dev/proxmux'
int pollEvents (int mux)
{
   struct pollfd fds;
   unsigned char databuffer[SENSOR_DATA_SIZE];   
   ssize_t iread = 0;

   while (1)
   {
       fds.events = POLLIN;
       fds.fd = mux;

       // wait for sensor data
       printf("\nPolling [%s] ...\n", PROXMUX_DRIVER_NAME);
       if (poll (&fds, 1, -1) == -1)       // -1 means infinite
       {
          printf("Error polling [%s] (%d). \n", PROXMUX_DRIVER_NAME, errno);
          return -errno;
       }

       if (fds.revents & POLLIN) 
       {
          
	       printf("\npollEvents returned. Data follows: \n");
	       printf("----------------------------------\n");

	       // read
	       iread = read (mux, databuffer, SENSOR_DATA_SIZE );
	       if (iread == 0)
	       {
		       printf("Error -- MUX signalled, but returned no events. Quitting...\n");
		       return -EFAULT;
	       }
	    
          // dump them all 
          dumpevents (databuffer, iread);

       }  // revents & POLLIN
       
   } // while (1)

   return 0;
}


int usage ()
{
   printf ("Usage: senstest mode <params>\n");
   printf ("=============================\n");
   printf ("Mode: %c - Enable     <Sensor ID> <Reporting Mode (optional)> \n", MODE_ENABLE);
   printf ("      %c - Disable    <Sensor ID>\n", MODE_DISABLE);
   printf ("      %c - Delay      <Sensor ID> <Timeout [ms]>\n", MODE_DELAY);
   printf ("      %c - Poll\n", MODE_POLL);
   printf ("      %c - Configuration\n", MODE_CONFIG);
   printf ("      %c - Start Queue <Sensor Mask> <Timeout [ms]\n", MODE_START_QUEUE);
   printf ("      %c - Stop  Queue <Timeout (Queue ID) [ms]\n", MODE_STOP_QUEUE);

   return 0;
}


void dumpevents (unsigned char* databuffer, ssize_t readsize)
{
    // format is: 4 bytes handle + 2 bytes data size + payload
    ssize_t processed = 0;

    while (processed < readsize)
    {
        RI_SENSOR_HANDLE handle = 0x00;
        RI_DATA_SIZE datasize = 0;
        RI_DATA_SIZE dataoffset = 0;

        memcpy (&handle, databuffer + processed, sizeof(RI_SENSOR_HANDLE) ); processed+= sizeof(RI_SENSOR_HANDLE);
        memcpy (&datasize, databuffer + processed, sizeof(RI_DATA_SIZE) ); processed+= sizeof(RI_DATA_SIZE);

        printf("Sensor [0x%x], Payload [%d] bytes\n", handle, datasize);
        while (dataoffset < datasize)
        {
            if (handle == RI_SENSOR_HANDLE_ACCELEROMETER)
            {
                float xf, yf, zf;
                short* v;

                v = (short*)(databuffer + processed + dataoffset); xf = *v; xf = ((xf * ACC_FACTOR * GRAVITY_EARTH) / 1000.0f);
                v = (short*)(databuffer + processed + dataoffset + sizeof(short) ); yf = *v; yf = ((yf * ACC_FACTOR * GRAVITY_EARTH) / 1000.0f);
                v = (short*)(databuffer + processed + dataoffset + sizeof(short) * 2); zf = *v; zf = ((zf * ACC_FACTOR * GRAVITY_EARTH) / 1000.0f);

                printf("Acceleration: X:[%.2f]  Y:[%.2f] Z:[%.2f]\n", xf, yf, zf);
                dataoffset += 3 * sizeof(short);
            }
            else if (handle == RI_SENSOR_HANDLE_GYROSCOPE)
            {
                float xf, yf, zf;
                short* v;

                v = (short*)(databuffer + processed + dataoffset); xf = *v; xf = (xf * GYRO_FACTOR);
                v = (short*)(databuffer + processed + dataoffset + sizeof(short) ); yf = *v; yf = (yf * GYRO_FACTOR);
                v = (short*)(databuffer + processed + dataoffset + sizeof(short) * 2); zf = *v; zf = (zf * GYRO_FACTOR); 

                printf("Gyroscope: X:[%.2f]  Y:[%.2f] Z:[%.2f]\n", xf, yf, zf);
                dataoffset += 3 * sizeof(short);
            }

            else if (handle == RI_SENSOR_HANDLE_MAGNETOMETER)
            {
                float xf, yf, zf;
                short* v;

                v = (short*)(databuffer + processed + dataoffset); xf = *v; xf = (xf * MAG_FACTOR);
                v = (short*)(databuffer + processed + dataoffset + sizeof(short) ); yf = *v; yf = (yf * MAG_FACTOR);
                v = (short*)(databuffer + processed + dataoffset + sizeof(short) * 2); zf = *v; zf = (zf * MAG_FACTOR); 

                printf("Magnetometer: X:[%.2f]  Y:[%.2f] Z:[%.2f]\n", xf, yf, zf);
                dataoffset += 3 * sizeof(short);
            }

            else if (handle == RI_SENSOR_HANDLE_PRESSURE)
            {
               int intval;
		         float fval = 0;

		         memcpy(&(intval), databuffer + processed + dataoffset, sizeof(int) );
               fval = intval; fval = fval / 4096;

		         printf("Pressure: %.2f [hPa]\n", fval);
		         dataoffset += sizeof(int);
            }

            else if (handle == RI_SENSOR_HANDLE_TEMPERATURE)
            {
                short intval;
                float fval = 0;

                memcpy(&(intval), databuffer + processed + dataoffset, sizeof(short) );
                fval = intval; fval = fval / 256;

                printf("Temperature: %.2f [C]\n", fval);
                dataoffset += sizeof(short);
            }
            else
               dataoffset = datasize;
            
     
        }
        processed += datasize;

    }
}

/*
// Test PROXMUX_IOCTL_UNREGISTER
int unregister(int mux, RI_SENSOR_HANDLE handle)
{
   int iRet = 0;
   risensorcmd cmd; cmd.handle = handle; cmd.param = 0;

   iRet = ioctl (mux, PROXMUX_IOCTL_UNREGISTER, &cmd);
   if (iRet == 0)
   {
      printf("Sensor: [0x%02x] successfully unregistered\n", handle);
     
      // show board configuration
      iRet = get_config(mux);
   }
   else
      printf("PROXMUX_IOCTL_UNREGISTER error: %d\n", iRet);

   return iRet;
} 

// Test Register
int sens_register (int mux, RI_SENSOR_HANDLE handle, unsigned int delay, unsigned int extra)
{
   int iRet = 0;
   risensorcmd cmd;

   cmd.handle = handle;
   cmd.param  = delay;
   cmd.extra  = extra;

   printf("Registering sensor: [0x%x]. Delay: (%d), Extra: (%d)\n", handle, delay, extra);

   iRet = ioctl (mux, PROXMUX_IOCTL_REGISTER, &cmd);

   if (iRet == 0) 
     printf("Sensor [0x%x] successfully registered. Initial poll interval: [%d] ms\n", handle, delay);
   else
     printf("PROXMUX_IOCTL_REGISTER Error: %d\n", iRet);

   return iRet;
}
*/
