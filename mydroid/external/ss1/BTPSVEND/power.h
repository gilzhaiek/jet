/*****< power.h >**************************************************************/
/*      Copyright 2000 - 2010 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  POWER - Definitions, Constants, and Prototypes, for the power control     */
/*          routines used by the Stonestreet One Android Bluetooth Stack.     */
/******************************************************************************/

#ifndef __POWERH__
#define __POWERH__

#ifdef __cplusplus
extern "C"
{
#endif

#include "SS1BTPM.h"

   /* This function initializes data structures used by the radio power */
   /* control functions. It returns 0 on success or -1 on error.        */
int InitRadioPowerControl();

   /* The following function provides access to the current power state */
   /* of the Bluetooth radio. You must call InitRadioPowerControl()     */
   /* before using this function. On success, the function returns 1    */
   /* for "power on" or 0 for "power off". A negative return value      */
   /* indicates an error.                                               */
int GetRadioPower();

   /* The following function is used to set a new power state for the   */
   /* Bluetooth radio. The desired state is specified as 'true' for     */
   /* "power on" and 'false' for "power off". Before calling this       */
   /* function, you must call InitRadioPowerControl() successfully.     */
   /* This function returns zero on success, or a negative value to     */
   /* indicate an error.                                                */
int SetRadioPower(Boolean_t NewPowerState);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* __POWERH__ */

