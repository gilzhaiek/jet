/*****< power.c >**************************************************************/
/*      Copyright 2000 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  power - API layer for controlling power of the Bluetooth device. This will*/
/*          be customized for specific devices.                               */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   11/15/11  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "BTPSVEND_Power"

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include "cutils/properties.h"

#include "power.h"
#include "SS1UTILI.h"

static char *RadioPowerControlFile = NULL;

   /* This function initializes data structures used by the radio power */
   /* control functions. It returns 0 on success or -1 on error.        */
int InitRadioPowerControl()
{
   SS1_LOGD("Enter");

   int  ret_val = 2;
   int  Fd;
   int  ReadCount;
   int  DeviceNumber;
   char Buffer[48];

   /* Only search for the rfkill interface if we don't already have     */
   /* a path stored. If we already have an interface selected, just     */
   /* return success.                                                   */
   if(!RadioPowerControlFile)
   {
      /* Search for the rfkill device control file. Rfkill interfaces   */
      /* are exposed under /sys/class/rfkill with each device           */
      /* represented by a subdirectory named rfkillN, where N is an     */
      /* integer counting up from 0. This directory contains a number   */
      /* of informational files, including 'type', which contains a     */
      /* string representation of the type of the radio device. Since   */
      /* Android only supports one Bluetooth device, the first rfkill   */
      /* interface we find with a type of "bluetooth" is the one we     */
      /* want.                                                          */
      for(DeviceNumber = 0; ret_val == 2; DeviceNumber++)
      {
         /* Contruct the path to the rfkill device type file.           */
         snprintf(Buffer, sizeof(Buffer), "/sys/class/rfkill/rfkill%d/%s", DeviceNumber, "type");

         /* Try to open a file at this path for reading.                */
         if((Fd = open(Buffer, O_RDONLY)) >= 0)
         {
            SS1_LOGD("Trying interface /sys/class/rfkill/rfkill%d", DeviceNumber);

            /* The file opened successfully, so try reading from it.    */
            if((ReadCount = read(Fd, Buffer, sizeof(Buffer))) >= 9)
            {
               /* We were able to read something, so make sure it's     */
               /* NULL-terminated and test for the device type we want. */
               Buffer[ReadCount] = '\0';
               SS1_LOGD("Type of rfkill%d: %s", DeviceNumber, Buffer);
               if(strncmp(Buffer, "bluetooth", 9) == 0)
               {
                  /* We found the device we are looking for, so store   */
                  /* the path to its rfkill state file for future use.  */
                  if(asprintf(&RadioPowerControlFile, "/sys/class/rfkill/rfkill%d/%s", DeviceNumber, "state") >= 0)
                  {
                     /* Done.                                           */
                     ret_val = 0;
                     SS1_LOGD("Selected interface rfkill%d", DeviceNumber);
                  }
                  else
                  {
                     /* We found the bluetooth device, but couldn't     */
                     /* store the path to the control file. Put the     */
                     /* pointer to the file path in a known state       */
                     /* (asprintf doesn't guarantee anything on         */
                     /* failure) and return an error status.            */
                     SS1_LOGE("Out of memory");
                     RadioPowerControlFile = NULL;
                     ret_val = -1;
                  }
               }
            }
            close(Fd);
         }
         else
         {
            /* We couldn't open the next device type file, meaning      */
            /* we've run out of rfkill devices to search for the        */
            /* bluetooth radio. Return an error status.                 */
            SS1_LOGE("Unable to find bluetooth rfkill interface");
            ret_val = -1;
         }
      }
   }
   else
      ret_val = 0;

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
}

   /* The following function provides access to the current power state */
   /* of the Bluetooth radio. You must call InitRadioPowerControl()     */
   /* before using this function. On success, the function returns 1    */
   /* for "power on" or 0 for "power off". A negative return value      */
   /* indicates an error.                                               */
int GetRadioPower()
{
   SS1_LOGD("Enter");

   int  ret_val   = -1;
   int  Fd;
   int  ReadCount;
   char Buffer;

   /* Check to make sure InitRadioPowerControl() was able to find the   */
   /* rfkill state file path.                                           */
   if(RadioPowerControlFile)
   {
      /* Attempt to open the rfkill state file.                         */
      if((Fd = open(RadioPowerControlFile, O_RDONLY)) >= 0)
      {
         /* If it opened successfully, read the current state (one      */
         /* ASCII-coded byte).                                          */
         if((ReadCount = read(Fd, &Buffer, 1)) == 1)
         {
            /* The current state comes as an ASCII character. Either    */
            /* '1' or ON or '0' for OFF.                                */
            switch(Buffer)
            {
               case '1':
                  /* Radio is ON                                        */
                  ret_val = 1;
                  break;
               case '0':
                  /* Radio is OFF                                       */
                  ret_val = 0;
                  break;
               default:
                  /* Any other value is an error: we should only read   */
                  /* either ASCII 1 or 0 from the rfkill state file.    */
                  break;
            }
         }
         else
            SS1_LOGE("Could not read status (%d)", errno);

         close(Fd);
      }
      else
         SS1_LOGE("Could not open power control interface");
   }
   else
      SS1_LOGE("Radio power control system not inititalized");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
}

   /* The following function is used to set a new power state for the   */
   /* Bluetooth radio. The desired state is specified as 'true' for     */
   /* "power on" and 'false' for "power off". Before calling this       */
   /* function, you must call InitRadioPowerControl() successfully.     */
   /* This function returns zero on success, or a negative value to     */
   /* indicate an error.                                                */
int SetRadioPower(Boolean_t NewPowerState)
{
   SS1_LOGD("Enter (%d)", NewPowerState);

   int  ret_val    = -1;
   int  Fd;
   int  WriteCount;
   char Buffer;

   /* Translate the desired power state into an ASCII value: '1' for ON */
   /* or '0' for OFF.                                                   */
   Buffer = (NewPowerState ? '1' : '0');

   /* Check to make sure InitRadioPowerControl() was able to find the   */
   /* rfkill state file path.                                           */
   if(RadioPowerControlFile)
   {
      /* Attempt to open the rfkill state file.                         */
      if((Fd = open(RadioPowerControlFile, O_WRONLY)) >= 0)
      {
         /* If it opened successfully, write the new state.             */
         if((WriteCount = write(Fd, &Buffer, 1)) == 1)
         {
            /* Everything worked correctly. Return success.             */
            ret_val = 0;
         }
         else
            SS1_LOGE("Could not send enable code (%d)", WriteCount);

         close(Fd);
      }
      else
         SS1_LOGE("Could not open power control interface");
   }
   else
      SS1_LOGE("Radio power control system not inititalized");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
}

