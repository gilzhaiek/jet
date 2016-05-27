/*****< ARBTJBCOM.h >**********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBCOM - Definitions, Constants, and Prototypes, common across the     */
/*              modules of the Stonestreet One Android Runtime Bluetooth JNI  */
/*              Bridge.                                                       */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/18/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __ARBTJBCOMH__
#define __ARBTJBCOMH__

#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"

#ifdef HAVE_BLUETOOTH

#include <stdio.h>

extern "C"
{
#include "SS1BTPM.h"
}

#include "SS1UTIL.h"

namespace android
{

typedef struct _tagDeviceList_Device_t
{
   BD_ADDR_t    BD_ADDR;
   Boolean_t    LowEnergy;
   void        *ExtraData;
} DeviceList_Device_t;

typedef struct _tagDeviceList_t
{
   pthread_mutex_t           Mutex;
   unsigned int              Length;
   unsigned int              Capacity;
   DeviceList_Device_t *Devices;
} DeviceList_t;


   /* This function is used to set a particular element of a Java       */
   /* String[] to the value of the buffer UTF8Value.                    */
void SetStringArrayElement(JNIEnv *Env, jobjectArray StringArray, unsigned int Index, char *UTF8String);
void SetStringArrayElement(JNIEnv *Env, jobjectArray StringArray, unsigned int Index, const char *UTF8String);

jobjectArray BuildLocalPropertyStringArray(JNIEnv *Env, unsigned long IncludeFields, DEVM_Local_Device_Properties_t *LocalDevProps);

jobjectArray BuildRemotePropertyStringArray(JNIEnv *Env, unsigned long IncludeFields, DEVM_Remote_Device_Properties_t *RemoteDevProps);

#define LOCAL_PROPERTY_ADDRESS               0x0001
#define LOCAL_PROPERTY_NAME                  0x0002
#define LOCAL_PROPERTY_CLASS                 0x0004
#define LOCAL_PROPERTY_POWERED               0x0008
#define LOCAL_PROPERTY_DISCOVERABLE          0x0010
#define LOCAL_PROPERTY_DISCOVERABLETIMEOUT   0x0020
#define LOCAL_PROPERTY_PAIRABLE              0x0040
#define LOCAL_PROPERTY_PAIRABLETIMEOUT       0x0080
#define LOCAL_PROPERTY_DISCOVERING           0x0100
#define LOCAL_PROPERTY_DEVICES               0x0200
#define LOCAL_PROPERTY_ALL                   (LOCAL_PROPERTY_ADDRESS | LOCAL_PROPERTY_NAME | LOCAL_PROPERTY_CLASS | LOCAL_PROPERTY_POWERED | LOCAL_PROPERTY_DISCOVERABLE | LOCAL_PROPERTY_DISCOVERABLETIMEOUT | LOCAL_PROPERTY_PAIRABLE | LOCAL_PROPERTY_PAIRABLETIMEOUT | LOCAL_PROPERTY_DISCOVERING | LOCAL_PROPERTY_DEVICES)

#define REMOTE_PROPERTY_ADDRESS        0x0001
#define REMOTE_PROPERTY_NAME           0x0002
#define REMOTE_PROPERTY_UUIDS          0x0004
#define REMOTE_PROPERTY_CLASS          0x0008
#define REMOTE_PROPERTY_PAIRED         0x0010
#define REMOTE_PROPERTY_CONNECTED      0x0020
#define REMOTE_PROPERTY_TRUSTED        0x0040
#define REMOTE_PROPERTY_ALIAS          0x0080
#define REMOTE_PROPERTY_NODES          0x0100
#define REMOTE_PROPERTY_ADAPTER        0x0200
#define REMOTE_PROPERTY_LEGACYPAIRING  0x0400
#define REMOTE_PROPERTY_RSSI           0x0800
#define REMOTE_PROPERTY_TXPOWER        0x1000
#define REMOTE_PROPERTY_ALL            (REMOTE_PROPERTY_ADDRESS | REMOTE_PROPERTY_NAME | REMOTE_PROPERTY_UUIDS | REMOTE_PROPERTY_CLASS | REMOTE_PROPERTY_PAIRED | REMOTE_PROPERTY_CONNECTED | REMOTE_PROPERTY_TRUSTED | REMOTE_PROPERTY_ALIAS | REMOTE_PROPERTY_NODES | REMOTE_PROPERTY_ADAPTER | REMOTE_PROPERTY_LEGACYPAIRING | REMOTE_PROPERTY_RSSI | REMOTE_PROPERTY_TXPOWER)

#define REMOTE_DEVICE_APPLICATION_INFO_FLAG_TRUSTED 0x00000001

jobjectArray SplitNextProp(JNIEnv *Env, jobjectArray PropertyList, jsize *NextPropStart);

unsigned long BTPMChangedLocalPropsToBTJB(unsigned long BTPMProps);

unsigned long BTPMChangedRemotePropsToBTJB(unsigned long BTPMProps);

int GetRemoteDeviceRFCOMMChannel(BD_ADDR_t BD_ADDR, UUID_128_t UUID, Word_t AttributeID);

int AttachThreadToJVM(JavaVM *VM, jint JNIEnvVersion, JNIEnv **Env);

void SanitizeUTF8(char *UTF8String, unsigned int Length, char Replacement = '?');

   /* The following functions are provided to manipulate the contents   */
   /* of DeviceList_t structures.                                       */
bool InitDeviceList(DeviceList_t *DeviceList, unsigned int InitialCapacity = 0);
bool DestroyDeviceList(DeviceList_t *DeviceList);
bool CleanDeviceList(DeviceList_t *DeviceList, void (*CleanCallback)(BD_ADDR_t BD_ADDR, Boolean_t LowEnergy, void *ExtraData, void *CallbackParameter), void *CallbackParameter);
int AddDeviceToList(DeviceList_t *DeviceList, BD_ADDR_t BD_ADDR, Boolean_t LowEnergy, void *ExtraData, bool KeyOnExtraData = false);
bool RemoveDeviceFromList(DeviceList_t *DeviceList, BD_ADDR_t BD_ADDR, Boolean_t LowEnergy, void **ExtraData);
bool RemoveDeviceFromListAny(DeviceList_t *DeviceList, BD_ADDR_t *BD_ADDR, Boolean_t *LowEnergy, void **ExtraData);
bool RemoveDeviceFromListByExtraData(DeviceList_t *DeviceList, void *ExtraData, BD_ADDR_t *BD_ADDR, Boolean_t *LowEnergy);
bool DeviceInListHasExtraData(DeviceList_t *DeviceList, BD_ADDR_t BD_ADDR);

   /* Return values for AddDeviceToList.                                */
#define ADD_DEVICE_SUCCESS           (0)
#define ADD_DEVICE_ALREADY_EXISTS    (1)
#define ADD_DEVICE_ERROR            (-1)

void DebugDumpStringArray(JNIEnv *Env, jobjectArray StringArray, const char *fncName);

jboolean IdenticalUuid(JNIEnv *Env, jobjectArray SinglePropertyArray, jstring JavaStringDelimitedUuid);

} /* namespace android */


#endif /* HAVE_BLUETOOTH */

#endif /* __ARBTJBCOMH__ */
