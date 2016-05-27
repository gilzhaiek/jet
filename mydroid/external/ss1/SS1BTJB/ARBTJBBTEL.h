/*****< ARBTJBBTEL.h >*********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTEL - Stonestreet One Android Runtime Bluetooth JNI Bridge Type    */
/*               Definitions, Constants, and Prototypes for the               */
/*               BluetoothEventLoop module.                                   */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/29/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __ARBTJBBTELH__
#define __ARBTJBBTELH__

#include "JNIHelp.h"
#include "jni.h"

#ifdef HAVE_BLUETOOTH

#include "ARBTJBCOM.h"

extern "C" {
#include "SS1BTPM.h"

#if (SS1_PLATFORM_SDK_VERSION >= 14)

#if SS1_SUPPORT_HID
   #include "HIDMAPI.h"
#endif

#if SS1_SUPPORT_PAN
   #include "PANMAPI.h"
#endif

#endif
}

   /* If non-zero, when a device is found by discovery, do not announce */
   /* the newly discovered device until after attempting to query the   */
   /* device's name.                                                    */
#define BTEL_DELAY_FOUND_FOR_NAME 1

   /* If non-zero, defer announcement of successful pairing until after */
   /* attempting to query the device's service records.                 */
#define ANNOUNCE_BOND_AFTER_SDP 0

   /* If non-zero, The UUID that Android has cached for a remote device */
   /* are compared to the set of UUID in a remote device properties     */ 
   /* change event. If the sets are identical, then the UUID are        */
   /* are witheld from the set of changed properties reported to        */
   /* Android.                                                          */ 
#define WITHOLD_IDENTICAL_UUID_SETS 0

namespace android
{

typedef struct _tagBTEL_NativeData_t
{
   JavaVM *VM;
   jobject Object;
   jint    JNIEnvVersion;

   int  AuthCallbackID;
   bool EventLoopThreadStatus;
   bool PoweredOnStateAnnounced;

   pthread_mutex_t PendingCreateDeviceMutex;
   pthread_cond_t  PendingCreateDeviceRemovalCondition;
   BD_ADDR_t       PendingCreateDeviceList[16];
   unsigned int    PendingCreateDeviceNumber;

   pthread_mutex_t DevicePoweredOffMutex;
   pthread_cond_t  DevicePoweredOffCondition;

   /* State list for emulation of BlueZ-like DeviceCreated and          */
   /* DeviceRemoved events.                                             */
   DeviceList_t KnownDevices;

   /* State list for tracking remote devices which have a pending       */
   /* authentication request but are first waiting on a device name     */
   /* inquiry.                                                          */
   DeviceList_t NamePendingForAuthDevices;

#if BTEL_DELAY_FOUND_FOR_NAME
   /* State list for tracking remote devices which have a pending       */
   /* DeviceFound notification but are first waiting on a device name   */
   /* inquiry.                                                          */
   DeviceList_t NamePendingForFoundDevices;
#endif

   /* State list for tracking remote devices on which we are currently  */
   /* waiting for a reply to a PIN Code, Passkey, or Confirmation for   */
   /* pairing.                                                          */
   DeviceList_t PairResponsePendingDevices;

   /* State list for tracking remote devices on which we are currently  */
   /* waiting for a SDP Service Discovery response.                     */
   DeviceList_t ServiceDiscoveryPendingDevices;
} BTEL_NativeData_t;

BTEL_NativeData_t *BTEL_GetNativeData(JNIEnv *Env, jobject Object);

void BTEL_CallOnPropertyChanged(JNIEnv *Env, jobject BTELObject, jobjectArray PropValues);

void BTEL_CallOnCreateDeviceResult(JNIEnv *Env, jobject BTELObject, jstring Address, jint Result);

void BTEL_CallOnCreatePairedDeviceResult(JNIEnv *Env, jobject BTELObject, jstring Address, jint Result);

bool BTEL_AddPendingPairDevice(BTEL_NativeData_t *NatData, BD_ADDR_t BD_ADDR, Boolean_t LowEnergy);

void BTEL_DEVMEventCallback(DEVM_Event_Data_t *EventData, void *CallbackParameter);

void BTEL_DEVMAuthenticationCallback(DEVM_Authentication_Information_t *AuthenticationRequestInformation, void *CallbackParameter);

#if (SS1_PLATFORM_SDK_VERSION >= 14)

#if SS1_SUPPORT_HID
void BTEL_HIDMEventCallback(HIDM_Event_Data_t *EventData, void *CallbackParameter);
#endif

#if SS1_SUPPORT_PAN
void BTEL_PANMEventCallback(PANM_Event_Data_t *EventData, void *CallbackParameter);
#endif

#endif

void BTEL_CallOnInputDeviceConnectionResult(JNIEnv *Env, jobject BTELObject, jstring Path, jint Result);

void BTEL_CallOnPanDeviceConnectionResult(JNIEnv *Env, jobject BTELObject, jstring Path, jint Result);

void BTEL_IPCLostCallback(void *CallbackParameter);

   /* Result codes for use in call to                                   */
   /* Java->onCreatePairedDeviceResult().                               */
#define BOND_RESULT_ERROR                -1000
#define BOND_RESULT_SUCCESS               0
#define BOND_RESULT_AUTH_FAILED           1
#define BOND_RESULT_AUTH_REJECTED         2
#define BOND_RESULT_AUTH_CANCELED         3
#define BOND_RESULT_REMOTE_DEVICE_DOWN    4
#define BOND_RESULT_DISCOVERY_IN_PROGRESS 5
#define BOND_RESULT_AUTH_TIMEOUT          6
#define BOND_RESULT_REPEATED_ATTEMPTS     7
#define BOND_RESULT_REMOTE_AUTH_CANCELED  8

#define CREATE_DEVICE_ALREADY_EXISTS 1
#define CREATE_DEVICE_SUCCESS        0
#define CREATE_DEVICE_FAILED        -1

   /* Result codes for the connect and disconnect requests for PAN      */
   /* devices.                                                          */
#define PAN_DISCONNECT_FAILED_NOT_CONNECTED  1000
#define PAN_CONNECT_FAILED_ALREADY_CONNECTED 1001
#define PAN_CONNECT_FAILED_ATTEMPT_FAILED    1002
#define PAN_OPERATION_GENERIC_FAILURE        1003
#define PAN_OPERATION_SUCCESS                1004

   /* Result codes for the connect and disconnect requests for HID      */
   /* devices.                                                          */
#define INPUT_DISCONNECT_FAILED_NOT_CONNECTED   5000
#define INPUT_CONNECT_FAILED_ALREADY_CONNECTED  5001
#define INPUT_CONNECT_FAILED_ATTEMPT_FAILED     5002
#define INPUT_OPERATION_GENERIC_FAILURE         5003
#define INPUT_OPERATION_SUCCESS                 5004

#define AUTHORIZATION_PROFILE_A2DP  1
#define AUTHORIZATION_PROFILE_AVRCP 2
#define AUTHORIZATION_PROFILE_PAN   3
#define AUTHORIZATION_PROFILE_HID   4

}

#endif /* HAVE_BLUETOOTH */

#endif /* __ARBTJBBTELH__ */

