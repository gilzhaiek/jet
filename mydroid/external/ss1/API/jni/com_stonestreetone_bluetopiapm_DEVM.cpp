/*****< com_stonestreetone_bluetopiapm_DEVM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_DEVM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager DEVM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   03/26/12  G. Hensley     Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>
#include <limits.h>

extern "C" {
#include "SS1BTPM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_DEVM.h"

   /* Class references. Because jclass references are actual Java class */
   /* references, holding a "strong" jclass reference will prevent the  */
   /* associated class from unloading at the discretion of the VM. Since*/
   /* we can't guarantee a way to release our reference when no more    */
   /* objects exist, these class references should be stored as Weak    */
   /* Global References. This will allow the VM to unload the class as  */
   /* needed. To effect this, every time we need to access a class, we  */
   /* must convert the Weak reference to a Strong one, access the class,*/
   /* and release the Strong reference. We must be prepared to bail out */
   /* if the Strong ref creation fails. This only means cleaning up and */
   /* failing the procedure since, if the Weak class ref is invalid, the*/
   /* class has been unloaded, meaning no valid objects of the class    */
   /* exist any longer so we have nothing to operate on, anyway.        */
static WEAK_REF Class_DEVM;

static jfieldID Field_DEVM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_DEVM_devicePoweredOnEvent;
static jmethodID Method_DEVM_devicePoweringOffEvent;
static jmethodID Method_DEVM_devicePoweredOffEvent;
static jmethodID Method_DEVM_localDevicePropertiesChangedEvent;
static jmethodID Method_DEVM_discoveryStartedEvent;
static jmethodID Method_DEVM_discoveryStoppedEvent;
static jmethodID Method_DEVM_remoteDeviceFoundEvent;
static jmethodID Method_DEVM_remoteDeviceDeletedEvent;
static jmethodID Method_DEVM_remoteDevicePropertiesChangedEvent;
static jmethodID Method_DEVM_remoteDevicePropertiesStatusEvent;
static jmethodID Method_DEVM_remoteDeviceServicesStatusEvent;
static jmethodID Method_DEVM_remoteLowEnergyDeviceServicesStatusEvent;
static jmethodID Method_DEVM_remoteDevicePairingStatusEvent;
static jmethodID Method_DEVM_remoteDeviceAuthenticationStatusEvent;
static jmethodID Method_DEVM_remoteDeviceEncryptionStatusEvent;
static jmethodID Method_DEVM_remoteDeviceConnectionStatusEvent;
static jmethodID Method_DEVM_deviceScanStartedEvent;
static jmethodID Method_DEVM_deviceScanStoppedEvent;
static jmethodID Method_DEVM_deviceAdvertisingStartedEvent;
static jmethodID Method_DEVM_deviceAdvertisingStoppedEvent;
static jmethodID Method_DEVM_remoteLowEnergyDeviceAddressChangedEvent;

static jmethodID Method_DEVM_AUTH_pinCodeRequestEvent;
static jmethodID Method_DEVM_AUTH_userConfirmationRequestEvent;
static jmethodID Method_DEVM_AUTH_passkeyRequestEvent;
static jmethodID Method_DEVM_AUTH_passkeyIndicationEvent;
static jmethodID Method_DEVM_AUTH_keypressIndicationEvent;
static jmethodID Method_DEVM_AUTH_outOfBandDataRequestEvent;
static jmethodID Method_DEVM_AUTH_ioCapabilitiesRequestEvent;
static jmethodID Method_DEVM_AUTH_authenticationStatusEvent;
static jmethodID Method_DEVM_AUTH_lowEnergyUserConfirmationRequestEvent;
static jmethodID Method_DEVM_AUTH_lowEnergyPasskeyRequestEvent;
static jmethodID Method_DEVM_AUTH_lowEnergyPasskeyIndicationEvent;
static jmethodID Method_DEVM_AUTH_lowEnergyOutOfBandDataRequestEvent;
static jmethodID Method_DEVM_AUTH_lowEnergyIOCapabilitiesRequestEvent;


typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
   unsigned int AuthCallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_DEVM_localData, Exclusive)) == NULL)
      PRINT_ERROR("DEVM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_DEVM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_DEVM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void DEVM_EventCallback(DEVM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         IntVal1;
   jint         IntVal2;
   jint         IntVal3;
   jint         IntVal4;
   JNIEnv      *Env;
   jobject      DEVMObject;
   jstring      String1;
   jstring      String2;
   BD_ADDR_t   *RemoteDevice;
   jbyteArray   ByteArray;
   jbyte       *ByteArrayData;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our DEVM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((DEVMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, DEVMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case detDevicePoweredOn:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_devicePoweredOnEvent);
                           break;
                        case detDevicePoweringOff:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_devicePoweringOffEvent,
                                 EventData->EventData.DevicePoweringOffEventData.PoweringOffTimeout);
                           break;
                        case detDevicePoweredOff:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_devicePoweredOffEvent);
                           break;
                        case detLocalDevicePropertiesChanged:
                           if((String1 = Env->NewStringUTF(EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.DeviceName)) != NULL)
                           {
                              IntVal1  = 0;
                              IntVal1 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS : 0);
                              IntVal1 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS : 0);
                              IntVal1 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS : 0);
                              IntVal1 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE : 0);
                              IntVal1 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY : 0);
                              IntVal1 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS : 0);

                              IntVal2  = 0;
                              IntVal2 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_CLASS_OF_DEVICE)   ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_PROPERTY_FIELD_CLASS_OF_DEVICE   : 0);
                              IntVal2 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_DEVICE_NAME)       ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_PROPERTY_FIELD_DEVICE_NAME       : 0);
                              IntVal2 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_DISCOVERABLE_MODE) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_PROPERTY_FIELD_DISCOVERABLE_MODE : 0);
                              IntVal2 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_CONNECTABLE_MODE)  ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_PROPERTY_FIELD_CONNECTABLE_MODE  : 0);
                              IntVal2 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_PAIRABLE_MODE)     ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_PROPERTY_FIELD_PAIRABLE_MODE     : 0);
                              IntVal2 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_DEVICE_FLAGS)      ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_PROPERTY_FIELD_DEVICE_FLAGS      : 0);
                              IntVal2 |= ((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_DEVICE_APPEARANCE) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_PROPERTY_FIELD_DEVICE_APPEARANCE : 0);

                              Env->CallVoidMethod(DEVMObject, Method_DEVM_localDevicePropertiesChangedEvent,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.BD_ADDR.BD_ADDR5,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.BD_ADDR.BD_ADDR4,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.BD_ADDR.BD_ADDR3,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.BD_ADDR.BD_ADDR2,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.BD_ADDR.BD_ADDR1,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.BD_ADDR.BD_ADDR0,
                                    (((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.ClassOfDevice.Class_of_Device2 << 16) & 0x00FF0000) | ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.ClassOfDevice.Class_of_Device1 << 8) & 0x0000FF00) | ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.ClassOfDevice.Class_of_Device0) & 0x000000FF)),
                                    String1,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.HCIVersion,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.HCIRevision,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LMPVersion,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.LMPSubVersion,
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.DeviceManufacturer,
                                    IntVal1,
                                    ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.DiscoverableMode == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.DiscoverableModeTimeout,
                                    ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.ConnectableMode == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.ConnectableModeTimeout,
                                    ((EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.PairableMode == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.PairableModeTimeout,
                                    IntVal2
                                    );

                              Env->DeleteLocalRef(String1);
                           }
                           break;
                        case detDeviceDiscoveryStarted:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_discoveryStartedEvent);
                           break;
                        case detDeviceDiscoveryStopped:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_discoveryStoppedEvent);
                           break;
                        case detRemoteDeviceFound:
                           IntVal1  = 0;
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)                  ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_NAME_KNOWN                  : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID      : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED)            ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED            : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED         : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_ENCRYPTED)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED    : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_SNIFF_MODE)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE   : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY      : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_AUTHENTICATED_KEY)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_SERVICES_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_TX_POWER_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_EIR_DATA_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_SERVICES_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_APPEARANCE_KNOWN)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN         : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE    : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_CURRENTLY_ENCRYPTED) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_AUTHENTICATED_KEY)        ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY        : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_INITIATED_LOCALLY)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY   : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_TX_POWER_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY  : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)             ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR      : 0);

                           if((String1 = Env->NewStringUTF(EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.DeviceName)) != NULL)
                           {
                              if(EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)
                              {
                                 String2 = Env->NewStringUTF(EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.ApplicationData.FriendlyName);
                                 IntVal2 = EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.ApplicationData.ApplicationInfo;
                              }
                              else
                              {
                                 String2 = NULL;
                                 IntVal2 = 0;
                              }

                              switch(EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.BLEAddressType)
                              {
                                 case atPublic:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PUBLIC;
                                    break;
                                 case atStatic:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_STATIC;
                                    break;
                                 case atPrivate_Resolvable:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_RESOLVABLE;
                                    break;
                                 case atPrivate_NonResolvable:
                                 default:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_NONRESOLVABLE;
                                    break;
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDeviceFoundEvent,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR5,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR4,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR3,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR2,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR1,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR0,
                                       (((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device2 << 16) & 0x00FF0000) | ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device1 << 8) & 0x0000FF00) | ((EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device0) & 0x000000FF)),
                                       String1,
                                       IntVal1,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.RSSI,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.TransmitPower,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.SniffInterval,
                                       String2,
                                       IntVal2,
                                       IntVal3,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.LE_RSSI,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.LETransmitPower,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.DeviceAppearance,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR5,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR4,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR3,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR2,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR1,
                                       EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR0);
                              }

                              Env->DeleteLocalRef(String1);

                              if(String2)
                                 Env->DeleteLocalRef(String2);
                           }
                           break;
                        case detRemoteDeviceDeleted:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDeviceDeletedEvent,
                                 EventData->EventData.RemoteDeviceDeletedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteDeviceDeletedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteDeviceDeletedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteDeviceDeletedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteDeviceDeletedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteDeviceDeletedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case detRemoteDevicePropertiesChanged:
                           IntVal1  = 0;
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)                  ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_NAME_KNOWN                  : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID      : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED)            ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED            : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED         : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_ENCRYPTED)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED    : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_SNIFF_MODE)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE   : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY      : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_AUTHENTICATED_KEY)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_SERVICES_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_TX_POWER_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_EIR_DATA_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_SERVICES_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_APPEARANCE_KNOWN)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN         : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE    : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_CURRENTLY_ENCRYPTED) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_AUTHENTICATED_KEY)        ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY        : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_INITIATED_LOCALLY)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY   : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_TX_POWER_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY  : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)             ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR      : 0);

                           if((String1 = Env->NewStringUTF(EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.DeviceName)) != NULL)
                           {
                              if(EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)
                              {
                                 String2 = Env->NewStringUTF(EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.ApplicationData.FriendlyName);
                                 IntVal2 = EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.ApplicationData.ApplicationInfo;
                              }
                              else
                              {
                                 String2 = NULL;
                                 IntVal2 = 0;
                              }

                              IntVal3  = 0;
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_CLASS_OF_DEVICE)          ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_CLASS_OF_DEVICE          : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_DEVICE_NAME)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_DEVICE_NAME              : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_APPLICATION_DATA)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_APPLICATION_DATA         : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_DEVICE_FLAGS)             ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_DEVICE_FLAGS             : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_RSSI)                     ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_RSSI                     : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_PAIRING_STATE)            ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_PAIRING_STATE            : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_CONNECTION_STATE)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_CONNECTION_STATE         : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_ENCRYPTION_STATE)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_ENCRYPTION_STATE         : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_SNIFF_STATE)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_SNIFF_STATE              : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_SERVICES_STATE)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_SERVICES_STATE           : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_RSSI)                  ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_LE_RSSI                  : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_PAIRING_STATE)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_LE_PAIRING_STATE         : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_CONNECTION_STATE)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_LE_CONNECTION_STATE      : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_ENCRYPTION_STATE)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_LE_ENCRYPTION_STATE      : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_PRIOR_RESOLVABLE_ADDRESS) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_PRIOR_RESOLVABLE_ADDRESS : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_DEVICE_APPEARANCE)        ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_DEVICE_APPEARANCE        : 0);
                              IntVal3 |= ((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_SERVICES_STATE)        ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_PROPERTY_FIELD_LE_SERVICES_STATE        : 0);
                              
                              switch(EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.BLEAddressType)
                              {
                                 case atPublic:
                                    IntVal4 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PUBLIC;
                                    break;
                                 case atStatic:
                                    IntVal4 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_STATIC;
                                    break;
                                 case atPrivate_Resolvable:
                                    IntVal4 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_RESOLVABLE;
                                    break;
                                 case atPrivate_NonResolvable:
                                 default:
                                    IntVal4 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_NONRESOLVABLE;
                                    break;
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDevicePropertiesChangedEvent,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR5,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR4,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR3,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR2,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR1,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR0,
                                       (((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device2 << 16) & 0x00FF0000) | ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device1 << 8) & 0x0000FF00) | ((EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device0) & 0x000000FF)),
                                       String1,
                                       IntVal1,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.RSSI,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.TransmitPower,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.SniffInterval,
                                       String2,
                                       IntVal2,
                                       IntVal3,
                                       IntVal4,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.LE_RSSI,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.LETransmitPower,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.DeviceAppearance,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR5,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR4,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR3,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR2,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR1,
                                       EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR0);
                              }

                              Env->DeleteLocalRef(String1);

                              if(String2)
                                 Env->DeleteLocalRef(String2);
                           }
                           break;
                        case detRemoteDevicePropertiesStatus:
                           IntVal1  = 0;
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)                  ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_NAME_KNOWN                  : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID      : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED)            ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED            : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED         : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_ENCRYPTED)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED    : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_SNIFF_MODE)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE   : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY      : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_AUTHENTICATED_KEY)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_SERVICES_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_TX_POWER_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_EIR_DATA_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN              : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_SERVICES_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_APPEARANCE_KNOWN)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN         : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE    : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_CURRENTLY_ENCRYPTED) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_AUTHENTICATED_KEY)        ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY        : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_INITIATED_LOCALLY)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY   : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_TX_POWER_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN           : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY  : 0);
                           IntVal1 |= ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)             ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR      : 0);

                           if((String1 = Env->NewStringUTF(EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.DeviceName)) != NULL)
                           {
                              if(EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)
                              {
                                 String2 = Env->NewStringUTF(EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.ApplicationData.FriendlyName);
                                 IntVal2 = EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.ApplicationData.ApplicationInfo;
                              }
                              else
                              {
                                 String2 = NULL;
                                 IntVal2 = 0;
                              }
                              
                              switch(EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.BLEAddressType)
                              {
                                 case atPublic:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PUBLIC;
                                    break;
                                 case atStatic:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_STATIC;
                                    break;
                                 case atPrivate_Resolvable:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_RESOLVABLE;
                                    break;
                                 case atPrivate_NonResolvable:
                                 default:
                                    IntVal3 = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_NONRESOLVABLE;
                                    break;
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDevicePropertiesStatusEvent,
                                    ((EventData->EventData.RemoteDevicePropertiesStatusEventData.Success == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR5,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR4,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR3,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR2,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR1,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.BD_ADDR.BD_ADDR0,
                                    (((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device2 << 16) & 0x00FF0000) | ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device1 << 8) & 0x0000FF00) | ((EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.ClassOfDevice.Class_of_Device0) & 0x000000FF)),
                                    String1,
                                    IntVal1,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.RSSI,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.TransmitPower,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.SniffInterval,
                                    String2,
                                    IntVal2,
                                    IntVal3,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.LE_RSSI,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.LETransmitPower,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.DeviceAppearance,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR5,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR4,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR3,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR2,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR1,
                                    EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties.PriorResolvableBD_ADDR.BD_ADDR0);
                              }

                              Env->DeleteLocalRef(String1);

                              if(String2)
                                 Env->DeleteLocalRef(String2);
                           }
                           break;
                        case detRemoteDeviceServicesStatus:
                           if(!(EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_LOW_ENERGY))
                           {
                              Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDeviceServicesStatusEvent,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_SUCCESS) ? JNI_TRUE : JNI_FALSE));
                           }
                           else
                           {
                              Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteLowEnergyDeviceServicesStatusEvent,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_SUCCESS) ? JNI_TRUE : JNI_FALSE));
                           }
                           break;
                        case detRemoteDevicePairingStatus:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDevicePairingStatusEvent,
                                 EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.RemoteDevicePairingStatusEventData.Success == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 (jint)(EventData->EventData.RemoteDevicePairingStatusEventData.AuthenticationStatus));
                           break;
                        case detRemoteDeviceAuthenticationStatus:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDeviceAuthenticationStatusEvent,
                                 EventData->EventData.RemoteDeviceAuthenticationStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteDeviceAuthenticationStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteDeviceAuthenticationStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteDeviceAuthenticationStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteDeviceAuthenticationStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteDeviceAuthenticationStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.RemoteDeviceAuthenticationStatusEventData.Status);
                           break;
                        case detRemoteDeviceEncryptionStatus:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDeviceEncryptionStatusEvent,
                                 EventData->EventData.RemoteDeviceEncryptionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteDeviceEncryptionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteDeviceEncryptionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteDeviceEncryptionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteDeviceEncryptionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteDeviceEncryptionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.RemoteDeviceEncryptionStatusEventData.Status);
                           break;
                        case detRemoteDeviceConnectionStatus:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteDeviceConnectionStatusEvent,
                                 EventData->EventData.RemoteDeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteDeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteDeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteDeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteDeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteDeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.RemoteDeviceConnectionStatusEventData.Status);
                           break;
                        case detDeviceScanStarted:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_deviceScanStartedEvent);
                           break;
                        case detDeviceScanStopped:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_deviceScanStoppedEvent);
                           break;
                        case detDeviceAdvertisingStarted:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_deviceAdvertisingStartedEvent);
                           break;
                        case detDeviceAdvertisingStopped:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_deviceAdvertisingStoppedEvent);
                           break;
                        case detRemoteDeviceAddressChanged:
                           //XXX
                           //Env->CallVoidMethod(DEVMObject, Method_DEVM_remoteLowEnergyDeviceAddressChangedEvent,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.PriorResolvableDeviceAddress.BD_ADDR5,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.PriorResolvableDeviceAddress.BD_ADDR4,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.PriorResolvableDeviceAddress.BD_ADDR3,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.PriorResolvableDeviceAddress.BD_ADDR2,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.PriorResolvableDeviceAddress.BD_ADDR1,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.PriorResolvableDeviceAddress.BD_ADDR0,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.CurrentResolvableDeviceAddress.BD_ADDR5,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.CurrentResolvableDeviceAddress.BD_ADDR4,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.CurrentResolvableDeviceAddress.BD_ADDR3,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.CurrentResolvableDeviceAddress.BD_ADDR2,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.CurrentResolvableDeviceAddress.BD_ADDR1,
                           //      EventData->EventData.RemoteDeviceAddressChangeEventData.CurrentResolvableDeviceAddress.BD_ADDR0);
                        default:
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during DEVM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, DEVMObject, LocalData);
                  }

                  Env->DeleteLocalRef(DEVMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the    */
                  /* DEVM java object. This can happen if the object has   */
                  /* been garbage collected or if the VM has run out of    */
                  /* memory. Now, check whether the object was GC'd. Since */
                  /* NewLocalRef doesn't throw exceptions, we can do this  */
                  /* with IsSameObject.                                    */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The DEVM Manager object has been GC'd. Since we are*/
                     /* still receiving events on this registration, the   */
                     /* Manager was not cleaned up properly.               */
                     PRINT_ERROR("DEVM object was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it  */
                     /* could indicate a leak in this thread context.      */
                     PRINT_ERROR("VM reports 'Out of Memory' in DEVM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The DEVM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: DEVM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: DEVM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Authentication event callback handler.
 */
static void DEVM_AuthCallback(DEVM_Authentication_Information_t *AuthInfo, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         IntVal1;
   jint         IntVal2;
   JNIEnv      *Env;
   jobject      DEVMObject;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((AuthInfo) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our DEVM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((DEVMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, DEVMObject, FALSE)) != NULL)
                  {
                     switch(AuthInfo->AuthenticationAction)
                     {
                        case DEVM_AUTHENTICATION_ACTION_PIN_CODE_REQUEST:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_pinCodeRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_PIN_CODE_RESPONSE:
                           PRINT_DEBUG("DEVM authentication event handler received a PIN Code Response. This should never happen.");
                           break;
                        case DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_REQUEST:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_userConfirmationRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0,
                                 AuthInfo->AuthenticationData.Passkey);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE:
                           PRINT_DEBUG("DEVM authentication event handler received a User Confirmation Response. This should never happen.");
                           break;
                        case DEVM_AUTHENTICATION_ACTION_PASSKEY_REQUEST:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_passkeyRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_PASSKEY_RESPONSE:
                           PRINT_DEBUG("DEVM authentication event handler received a Passkey Response. This should never happen.");
                           break;
                        case DEVM_AUTHENTICATION_ACTION_PASSKEY_INDICATION:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_passkeyIndicationEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0,
                                 AuthInfo->AuthenticationData.Passkey);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_KEYPRESS_INDICATION:
                           switch(AuthInfo->AuthenticationData.Keypress)
                           {
                              case kpEntryStarted:
                                 IntVal1 = com_stonestreetone_bluetopiapm_DEVM_KEYPRESS_ENTRY_STARTED;
                                 break;
                              case kpDigitEntered:
                                 IntVal1 = com_stonestreetone_bluetopiapm_DEVM_KEYPRESS_DIGIT_ENTERED;
                                 break;
                              case kpDigitErased:
                                 IntVal1 = com_stonestreetone_bluetopiapm_DEVM_KEYPRESS_DIGIT_ERASED;
                                 break;
                              case kpCleared:
                                 IntVal1 = com_stonestreetone_bluetopiapm_DEVM_KEYPRESS_CLEARED;
                                 break;
                              case kpEntryCompleted:
                                 IntVal1 = com_stonestreetone_bluetopiapm_DEVM_KEYPRESS_ENTRY_COMPLETED;
                                 break;
                              default:
                                 IntVal1 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_keypressIndicationEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0,
                                 IntVal1);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_REQUEST:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_outOfBandDataRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_RESPONSE:
                           PRINT_DEBUG("DEVM authentication event handler received an Out-of-Band Data Response. This should never happen.");
                           break;
                        case DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_REQUEST:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_ioCapabilitiesRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_RESPONSE:
                           PRINT_DEBUG("DEVM authentication event handler received an IO Capabilities Response. This should never happen.");
                           break;
                        case DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_STATUS_RESULT:
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_authenticationStatusEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0,
                                 AuthInfo->AuthenticationData.AuthenticationStatus);
                           break;
                        case DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_START:
                           PRINT_DEBUG("DEVM authentication event handler received an Authentication Start event.");
                           break;
                        case DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_END:
                           PRINT_DEBUG("DEVM authentication event handler received an Authentication End event.");
                           break;
                        case (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_REQUEST):
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_lowEnergyUserConfirmationRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0,
                                 AuthInfo->AuthenticationData.Passkey);
                           break;
                        case (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_PASSKEY_REQUEST):
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_lowEnergyPasskeyRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0);
                           break;
                        case (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_PASSKEY_INDICATION):
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_lowEnergyPasskeyIndicationEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0,
                                 AuthInfo->AuthenticationData.Passkey);
                           break;
                        case (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_REQUEST):
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_lowEnergyOutOfBandDataRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0);
                           break;
                        case (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_REQUEST):
                           Env->CallVoidMethod(DEVMObject, Method_DEVM_AUTH_lowEnergyIOCapabilitiesRequestEvent,
                                 AuthInfo->BD_ADDR.BD_ADDR5,
                                 AuthInfo->BD_ADDR.BD_ADDR4,
                                 AuthInfo->BD_ADDR.BD_ADDR3,
                                 AuthInfo->BD_ADDR.BD_ADDR2,
                                 AuthInfo->BD_ADDR.BD_ADDR1,
                                 AuthInfo->BD_ADDR.BD_ADDR0);
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during DEVM authentication event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, DEVMObject, LocalData);
                  }

                  Env->DeleteLocalRef(DEVMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the DEVM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The DEVM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("DEVM object was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in DEVM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The DEVM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: DEVM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: DEVM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_DEVM = (jclass)(NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_DEVM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_DEVM_devicePoweredOnEvent                       = Env->GetMethodID(Clazz, "devicePoweredOnEvent",                     "()V");
      Method_DEVM_devicePoweringOffEvent                     = Env->GetMethodID(Clazz, "devicePoweringOffEvent",                   "(I)V");
      Method_DEVM_devicePoweredOffEvent                      = Env->GetMethodID(Clazz, "devicePoweredOffEvent",                    "()V");
      Method_DEVM_localDevicePropertiesChangedEvent          = Env->GetMethodID(Clazz, "localDevicePropertiesChangedEvent",        "(BBBBBBILjava/lang/String;IIIIIIZIZIZII)V");
      Method_DEVM_discoveryStartedEvent                      = Env->GetMethodID(Clazz, "discoveryStartedEvent",                    "()V");
      Method_DEVM_discoveryStoppedEvent                      = Env->GetMethodID(Clazz, "discoveryStoppedEvent",                    "()V");
      Method_DEVM_remoteDeviceFoundEvent                     = Env->GetMethodID(Clazz, "remoteDeviceFoundEvent",                   "(BBBBBBILjava/lang/String;IIIILjava/lang/String;IIIIIBBBBBB)V");
      Method_DEVM_remoteDeviceDeletedEvent                   = Env->GetMethodID(Clazz, "remoteDeviceDeletedEvent",                 "(BBBBBB)V");
      Method_DEVM_remoteDevicePropertiesChangedEvent         = Env->GetMethodID(Clazz, "remoteDevicePropertiesChangedEvent",       "(BBBBBBILjava/lang/String;IIIILjava/lang/String;IIIIIIBBBBBB)V");
      Method_DEVM_remoteDevicePropertiesStatusEvent          = Env->GetMethodID(Clazz, "remoteDevicePropertiesStatusEvent",        "(ZBBBBBBILjava/lang/String;IIIILjava/lang/String;IIIIIBBBBBB)V");
      Method_DEVM_remoteDeviceServicesStatusEvent            = Env->GetMethodID(Clazz, "remoteDeviceServicesStatusEvent",          "(BBBBBBZ)V");
      Method_DEVM_remoteLowEnergyDeviceServicesStatusEvent   = Env->GetMethodID(Clazz, "remoteLowEnergyDeviceServicesStatusEvent", "(BBBBBBZ)V");
      Method_DEVM_remoteDevicePairingStatusEvent             = Env->GetMethodID(Clazz, "remoteDevicePairingStatusEvent",           "(BBBBBBZI)V");
      Method_DEVM_remoteDeviceAuthenticationStatusEvent      = Env->GetMethodID(Clazz, "remoteDeviceAuthenticationStatusEvent",    "(BBBBBBI)V");
      Method_DEVM_remoteDeviceEncryptionStatusEvent          = Env->GetMethodID(Clazz, "remoteDeviceEncryptionStatusEvent",        "(BBBBBBI)V");
      Method_DEVM_remoteDeviceConnectionStatusEvent          = Env->GetMethodID(Clazz, "remoteDeviceConnectionStatusEvent",        "(BBBBBBI)V");
      Method_DEVM_deviceScanStartedEvent                     = Env->GetMethodID(Clazz, "deviceScanStartedEvent",                   "()V");
      Method_DEVM_deviceScanStoppedEvent                     = Env->GetMethodID(Clazz, "deviceScanStoppedEvent",                   "()V");
      Method_DEVM_deviceAdvertisingStartedEvent              = Env->GetMethodID(Clazz, "deviceAdvertisingStartedEvent",                   "()V");
      Method_DEVM_deviceAdvertisingStoppedEvent              = Env->GetMethodID(Clazz, "deviceAdvertisingStoppedEvent",                   "()V");
      Method_DEVM_remoteLowEnergyDeviceAddressChangedEvent   = Env->GetMethodID(Clazz, "remoteLowEnergyDeviceAddressChangedEvent", "(BBBBBBBBBBBB)V");

      Method_DEVM_AUTH_pinCodeRequestEvent                   = Env->GetMethodID(Clazz, "pinCodeRequestEvent",                      "(BBBBBB)V");
      Method_DEVM_AUTH_userConfirmationRequestEvent          = Env->GetMethodID(Clazz, "userConfirmationRequestEvent",             "(BBBBBBI)V");
      Method_DEVM_AUTH_passkeyRequestEvent                   = Env->GetMethodID(Clazz, "passkeyRequestEvent",                      "(BBBBBB)V");
      Method_DEVM_AUTH_passkeyIndicationEvent                = Env->GetMethodID(Clazz, "passkeyIndicationEvent",                   "(BBBBBBI)V");
      Method_DEVM_AUTH_keypressIndicationEvent               = Env->GetMethodID(Clazz, "keypressIndicationEvent",                  "(BBBBBBI)V");
      Method_DEVM_AUTH_outOfBandDataRequestEvent             = Env->GetMethodID(Clazz, "outOfBandDataRequestEvent",                "(BBBBBB)V");
      Method_DEVM_AUTH_ioCapabilitiesRequestEvent            = Env->GetMethodID(Clazz, "ioCapabilitiesRequestEvent",               "(BBBBBB)V");
      Method_DEVM_AUTH_authenticationStatusEvent             = Env->GetMethodID(Clazz, "authenticationStatusEvent",                "(BBBBBBI)V");
      Method_DEVM_AUTH_lowEnergyUserConfirmationRequestEvent = Env->GetMethodID(Clazz, "lowEnergyUserConfirmationRequestEvent",    "(BBBBBBI)V");
      Method_DEVM_AUTH_lowEnergyPasskeyRequestEvent          = Env->GetMethodID(Clazz, "lowEnergyPasskeyRequestEvent",             "(BBBBBB)V");
      Method_DEVM_AUTH_lowEnergyPasskeyIndicationEvent       = Env->GetMethodID(Clazz, "lowEnergyPasskeyIndicationEvent",          "(BBBBBBI)V");
      Method_DEVM_AUTH_lowEnergyOutOfBandDataRequestEvent    = Env->GetMethodID(Clazz, "lowEnergyOutOfBandDataRequestEvent",       "(BBBBBB)V");
      Method_DEVM_AUTH_lowEnergyIOCapabilitiesRequestEvent   = Env->GetMethodID(Clazz, "lowEnergyIOCapabilitiesRequestEvent",      "(BBBBBB)V");
   }
   else
      PRINT_ERROR("DEVM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.DEVM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    convertManufacturerNameToStringNative
 * Signature: (I)Ljava/lang/String;
 */
static jstring ConvertManufacturerNameToStringNative(JNIEnv *Env, jclass Clazz, jint DeviceManufacturer)
{
   return Env->NewStringUTF(DEVM_ConvertManufacturerNameToString(DeviceManufacturer));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    initObjectNative
 * Signature: (Z)V
 */
static void InitObjectNative(JNIEnv *Env, jobject Object, jboolean Authentication)
{
   int          RegistrationResult;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(InitBTPMClient(TRUE) == 0)
   {
      if((LocalData = (LocalData_t *)malloc(sizeof(LocalData_t))) != NULL)
      {
         if((LocalData->WeakObjectReference = NewWeakRef(Env, Object)) != NULL)
         {
            if((RegistrationResult = DEVM_RegisterEventCallback(DEVM_EventCallback, LocalData->WeakObjectReference)) > 0)
            {
               LocalData->CallbackID = RegistrationResult;
               if(Authentication == JNI_TRUE)
               {
                  if((RegistrationResult = DEVM_RegisterAuthentication(DEVM_AuthCallback, LocalData->WeakObjectReference)) >= 0)
                  {
                     LocalData->AuthCallbackID = RegistrationResult;
                     SetLocalData(Env, Object, LocalData);
                  }
                  else
                  {
                     PRINT_ERROR("DEVM: Unable to register callback for authentication events (%d)", RegistrationResult);

                     DEVM_UnRegisterEventCallback(LocalData->CallbackID);
                     DeleteWeakRef(Env, LocalData->WeakObjectReference);
                     free(LocalData);
                     LocalData = 0;
                     CloseBTPMClient();

                     //XXX Change this to a more appropriate exception
                     Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/ServerNotReachableException"), NULL);
                  }
               }
               else
               {
                  SetLocalData(Env, Object, LocalData);
               }
            }
            else
            {
               PRINT_ERROR("DEVM: Unable to register callback for events (%d)", RegistrationResult);

               DeleteWeakRef(Env, LocalData->WeakObjectReference);
               free(LocalData);
               LocalData = 0;
               CloseBTPMClient();

               //XXX Change this to a more appropriate exception
               Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/ServerNotReachableException"), NULL);
            }
         }
         else
         {
            /* No need to throw an exception here. NewWeakRef will throw*/
            /* an OutOfMemoryError if we are out of resources.          */
            PRINT_ERROR("DEVM: Out of Memory: Unable to obtain weak reference to object");

            free(LocalData);
            LocalData = 0;
            CloseBTPMClient();
         }
      }
      else
      {
         CloseBTPMClient();
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }
   else
      Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/ServerNotReachableException"), NULL);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    cleanupObjectNative
 * Signature: ()V
 */
static void CleanupObjectNative(JNIEnv *Env, jobject Object)
{
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, TRUE)) != NULL)
   {
      /* Detach this LocalData from the Java object.  Any threads       */
      /* waiting on this LocalData may immediately resume as they will  */
      /* no longer be able to access the structure.                     */
      SetLocalData(Env, Object, NULL);
      ReleaseLocalData(Env, Object, LocalData);

      if(LocalData->CallbackID > 0)
         DEVM_UnRegisterEventCallback(LocalData->CallbackID);

      if(LocalData->AuthCallbackID > 0)
         DEVM_UnRegisterAuthentication(LocalData->AuthCallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);

      CloseBTPMClient();
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    acquireLockNative
 * Signature: ()Z
 */
static jboolean AcquireLockNative(JNIEnv *Env, jobject Object)
{
   return ((DEVM_AcquireLock() == FALSE) ? JNI_FALSE : JNI_TRUE);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    releaseLockNative
 * Signature: ()V
 */
static void ReleaseLockNative(JNIEnv *Env, jobject Object)
{
   DEVM_ReleaseLock();
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    powerOnDeviceNative
 * Signature: ()I
 */
static jint PowerOnDeviceNative(JNIEnv *Env, jobject Object)
{
   return DEVM_PowerOnDevice();
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    powerOffDeviceNative
 * Signature: ()I
 */
static jint PowerOffDeviceNative(JNIEnv *Env, jobject Object)
{
   return DEVM_PowerOffDevice();
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryDevicePowerStateNative
 * Signature: ()I
 */
static jint QueryDevicePowerStateNative(JNIEnv *Env, jobject Object)
{
   return DEVM_QueryDevicePowerState();
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    acknowledgeDevicePoweringDownNative
 * Signature: ()V
 */
static void AcknowledgeDevicePoweringDownNative(JNIEnv *Env, jobject Object)
{
   LocalData_t *LocalData;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      DEVM_AcknowledgeDevicePoweringDown(LocalData->CallbackID);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryLocalDevicePropertiesNative
 * Signature: ([B[Ljava/lang/String;[I)I
 */
static jint QueryLocalDevicePropertiesNative(JNIEnv *Env, jobject Object, jbyteArray Address, jobjectArray Name, jintArray Properties)
{
   jint                           Result;
   jint                          *PropertiesArrayElements;
   jbyte                         *AddressArrayElements;
   jstring                        JavaString;
   BD_ADDR_t                      RemoteDeviceAddress;
   DEVM_Local_Device_Properties_t DeviceProperties;

   if((Result = DEVM_QueryLocalDeviceProperties(&DeviceProperties)) == 0)
   {
      if((AddressArrayElements = Env->GetByteArrayElements(Address, NULL)) != NULL)
      {
         if((PropertiesArrayElements = Env->GetIntArrayElements(Properties, NULL)) != NULL)
         {
            AddressArrayElements[0] = DeviceProperties.BD_ADDR.BD_ADDR5;
            AddressArrayElements[1] = DeviceProperties.BD_ADDR.BD_ADDR4;
            AddressArrayElements[2] = DeviceProperties.BD_ADDR.BD_ADDR3;
            AddressArrayElements[3] = DeviceProperties.BD_ADDR.BD_ADDR2;
            AddressArrayElements[4] = DeviceProperties.BD_ADDR.BD_ADDR1;
            AddressArrayElements[5] = DeviceProperties.BD_ADDR.BD_ADDR0;

            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_CLASS_OF_DEVICE]           = (((DeviceProperties.ClassOfDevice.Class_of_Device2 << 16) & 0x00FF0000) | ((DeviceProperties.ClassOfDevice.Class_of_Device1 << 8) & 0x0000FF00) | ((DeviceProperties.ClassOfDevice.Class_of_Device0) & 0x000000FF));
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_HCI_VERSION]               = DeviceProperties.HCIVersion;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_HCI_REVISION]              = DeviceProperties.HCIRevision;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LMP_VERSION]               = DeviceProperties.LMPVersion;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LMP_SUBVERSION]            = DeviceProperties.LMPSubVersion;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_DEVICE_MANUFACTURER]       = DeviceProperties.DeviceManufacturer;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_DISCOVERABLE_MODE]         = ((DeviceProperties.DiscoverableMode == FALSE) ? JNI_FALSE : JNI_TRUE);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_DISCOVERABLE_MODE_TIMEOUT] = DeviceProperties.DiscoverableModeTimeout;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_CONNECTABLE_MODE]          = ((DeviceProperties.ConnectableMode == FALSE) ? JNI_FALSE : JNI_TRUE);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_CONNECTABLE_MODE_TIMEOUT]  = DeviceProperties.ConnectableModeTimeout;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_PAIRABLE_MODE]             = ((DeviceProperties.PairableMode == FALSE) ? JNI_FALSE : JNI_TRUE);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_PAIRABLE_MODE_TIMEOUT]     = DeviceProperties.PairableModeTimeout;

            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS]        = 0;
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS]       |= ((DeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS) ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS : 0);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS]       |= ((DeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS)      ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS      : 0);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS]       |= ((DeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS)   ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS   : 0);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS]       |= ((DeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE)   ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE   : 0);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS]       |= ((DeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)   ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY   : 0);
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS]       |= ((DeviceProperties.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS)     ? com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS     : 0);

            if((JavaString = Env->NewStringUTF(DeviceProperties.DeviceName)) != NULL)
            {
               Env->SetObjectArrayElement(Name, 0, JavaString);
               Env->DeleteLocalRef(JavaString);
            }

            Env->ReleaseIntArrayElements(Properties, PropertiesArrayElements, 0);
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }

         Env->ReleaseByteArrayElements(Address, AddressArrayElements, 0);
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    updateClassOfDeviceNative
 * Signature: (I)I
 */
static jint UpdateClassOfDeviceNative(JNIEnv *Env, jobject Object, jint ClassOfDevice)
{
   DEVM_Local_Device_Properties_t devProps;

   memset(&devProps, 0, sizeof(devProps));
   ASSIGN_CLASS_OF_DEVICE(devProps.ClassOfDevice, (Byte_t)(ClassOfDevice >> 16), (Byte_t)(ClassOfDevice >> 8), (Byte_t)(ClassOfDevice));

   return DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_CLASS_OF_DEVICE, &devProps);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    updateDeviceNameNative
 * Signature: (Ljava/lang/String;)I
 */
static jint UpdateDeviceNameNative(JNIEnv *Env, jobject Object, jstring DeviceName)
{
   jint                           Result;
   const char                    *NameString;
   DEVM_Local_Device_Properties_t devProps;

   memset(&devProps, 0, sizeof(devProps));

   if((DeviceName) && ((NameString = Env->GetStringUTFChars(DeviceName, NULL)) != NULL))
   {
      strncpy(devProps.DeviceName, NameString, (MAX_NAME_LENGTH));
      Env->ReleaseStringUTFChars(DeviceName, NameString);

      devProps.DeviceNameLength = strlen(devProps.DeviceName);

      Result = DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_DEVICE_NAME, &devProps);
   }
   else
   {
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    updateDiscoverableModeNative
 * Signature: (ZI)I
 */
static jint UpdateDiscoverableModeNative(JNIEnv *Env, jobject Object, jboolean Discoverable, jint Timeout)
{
   DEVM_Local_Device_Properties_t devProps;

   memset(&devProps, 0, sizeof(devProps));

   devProps.DiscoverableMode        = ((Discoverable == JNI_FALSE) ? FALSE : TRUE);
   devProps.DiscoverableModeTimeout = Timeout;

   return DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_DISCOVERABLE_MODE, &devProps);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    updateConnectableModeNative
 * Signature: (ZI)I
 */
static jint UpdateConnectableModeNative(JNIEnv *Env, jobject Object, jboolean Connectable, jint Timeout)
{
   DEVM_Local_Device_Properties_t devProps;

   memset(&devProps, 0, sizeof(devProps));

   devProps.ConnectableMode        = ((Connectable == JNI_FALSE) ? FALSE : TRUE);
   devProps.ConnectableModeTimeout = Timeout;

   return DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_CONNECTABLE_MODE, &devProps);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    updatePairableModeNative
 * Signature: (ZI)I
 */
static jint UpdatePairableModeNative(JNIEnv *Env, jobject Object, jboolean Pairable, jint Timeout)
{
   DEVM_Local_Device_Properties_t devProps;

   memset(&devProps, 0, sizeof(devProps));

   devProps.PairableMode        = ((Pairable == JNI_FALSE) ? FALSE : TRUE);
   devProps.PairableModeTimeout = Timeout;

   return DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_PAIRABLE_MODE, &devProps);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryLocalDeviceIDInformationNative
 * Signature: ([I)I
 */
static jint QueryLocalDeviceIDInformationNative(JNIEnv *Env, jobject Object, jintArray Values)
{
   jint                          Result;
   jint                         *ValuesArray;
   DEVM_Device_ID_Information_t  DevID;

   if((Values) && (Env->GetArrayLength(Values) == 4))
   {
      if((Result = DEVM_QueryLocalDeviceIDInformation(&DevID)) == 0)
      {
         if((ValuesArray = Env->GetIntArrayElements(Values, NULL)) != NULL)
         {
            ValuesArray[0] = DevID.VendorID;
            ValuesArray[1] = DevID.ProductID;
            ValuesArray[2] = DevID.DeviceVersion;
            ValuesArray[3] = (DevID.USBVendorID ? 1 : 0);

            Env->ReleaseIntArrayElements(Values, ValuesArray, 0);
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
   }
   else
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    enableLocalDeviceFeatureNative
 * Signature: (I)I
 */
static jint EnableLocalDeviceFeatureNative(JNIEnv *Env, jobject Object, jint LocalDeviceFeature)
{
   unsigned long Feature;

   switch(LocalDeviceFeature)
   {
      case com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY:
         Feature = DEVM_LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY; 
         break;
      case com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FEATURE_ANT_PLUS:
         Feature = DEVM_LOCAL_DEVICE_FEATURE_ANT_PLUS;
         break;
      default:
         Feature = (unsigned long)(-1);
   }

   return DEVM_EnableLocalDeviceFeature(Feature);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    disableLocalDeviceFeatureNative
 * Signature: (I)I
 */
static jint DisableLocalDeviceFeatureNative(JNIEnv *Env, jobject Object, jint LocalDeviceFeature)
{
   unsigned long Feature;

   switch(LocalDeviceFeature)
   {
      case com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY:
         Feature = DEVM_LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY; 
         break;
      case com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FEATURE_ANT_PLUS:
         Feature = DEVM_LOCAL_DEVICE_FEATURE_ANT_PLUS;
         break;
      default:
         Feature = (unsigned long)(-1);
   }

   return DEVM_DisableLocalDeviceFeature(Feature);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryActiveLocalDeviceFeatureNative
 * Signature: ([I)I
 */
static jint QueryActiveLocalDeviceFeatureNative(JNIEnv *Env, jobject Object, jintArray LocalDeviceFeature)
{
   jint          Feature[1];
   jint          Result;
   unsigned long ActiveFeature;

   if(!(Result = DEVM_QueryActiveLocalDeviceFeatures(&ActiveFeature)))
   {
      switch(ActiveFeature)
      {
         case DEVM_LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY:
            Feature[0] = com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY;
            break;
         case DEVM_LOCAL_DEVICE_FEATURE_ANT_PLUS:
            Feature[0] = com_stonestreetone_bluetopiapm_DEVM_LOCAL_DEVICE_FEATURE_ANT_PLUS;
      }

      Env->SetIntArrayRegion(LocalDeviceFeature, 0, 1, Feature);
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    startDeviceDiscoveryNative
 * Signature: (I)I
 */
static jint StartDeviceDiscoveryNative(JNIEnv *Env, jobject Object, jint DiscoveryDuration)
{
   if(DiscoveryDuration < 0)
      DiscoveryDuration = INT_MAX;

   return DEVM_StartDeviceDiscovery((unsigned long int)DiscoveryDuration);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    stopDeviceDiscoveryNative
 * Signature: ()I
 */
static jint StopDeviceDiscoveryNative(JNIEnv *Env, jobject Object)
{
   return DEVM_StopDeviceDiscovery();
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    startLowEnergyDeviceScanNative
 * Signature: (I)I
 */
static jint StartLowEnergyDeviceScanNative(JNIEnv *Env, jobject Object, jint ScanDuration)
{
   if(ScanDuration < 0)
      ScanDuration = INT_MAX;

   return DEVM_StartDeviceScan((unsigned long int)ScanDuration);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    stopLowEnergyDeviceScanNative
 * Signature: ()I
 */
static jint StopLowEnergyDeviceScanNative(JNIEnv *Env, jobject Object)
{
   return DEVM_StopDeviceScan();
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    startLowEnergyAdvertisingNative
 * Signature: (II[I[[B)I
 */
static jint StartLowEnergyAdvertisingNative(JNIEnv *Env, jobject Object, jint Flags, jint Duration, jintArray DataTypes, jobjectArray DataList)
{
   int                             RawDataLength;
   jint                            ret_val;
   jint                           *TypeArray;
   jsize                           TypeLength;
   jsize                           DataLength;
   jsize                           Index;
   jsize                           ErrorIndex;
   jbyte                          *DataElems;
   Byte_t                         *RawData;
   Boolean_t                       Error;
   jbyteArray                      DataArray;
   unsigned int                    CFlags;
   DEVM_Tag_Length_Value_t        *TLVList;
   DEVM_Advertising_Information_t  AdvertisingInformation;

   Error      = FALSE;
   CFlags     = 0;
   ret_val    = -1;
   ErrorIndex = 0;

   if(Flags & com_stonestreetone_bluetopiapm_DEVM_ADVERTISING_FLAGS_USE_PUBLIC_ADDRESS)
      CFlags |= DEVM_ADVERTISING_INFORMATION_FLAGS_USE_PUBLIC_ADDRESS;
   if(Flags & com_stonestreetone_bluetopiapm_DEVM_ADVERTISING_FLAGS_DISCOVERABLE)
      CFlags |= DEVM_ADVERTISING_INFORMATION_FLAGS_DISCOVERABLE;
   if(Flags & com_stonestreetone_bluetopiapm_DEVM_ADVERTISING_FLAGS_CONNECTABLE)
      CFlags |= DEVM_ADVERTISING_INFORMATION_FLAGS_CONNECTABLE;
   if(Flags & com_stonestreetone_bluetopiapm_DEVM_ADVERTISING_FLAGS_ADVERTISE_DEVICE_NAME)
      CFlags |= DEVM_ADVERTISING_INFORMATION_FLAGS_ADVERTISE_DEVICE_NAME;
   if(Flags & com_stonestreetone_bluetopiapm_DEVM_ADVERTISING_FLAGS_ADVERTISE_TX_POWER)
      CFlags |= DEVM_ADVERTISING_INFORMATION_FLAGS_ADVERTISE_TX_POWER;
   if(Flags & com_stonestreetone_bluetopiapm_DEVM_ADVERTISING_FLAGS_ADVERTISE_APPEARANCE)
      CFlags |= DEVM_ADVERTISING_INFORMATION_FLAGS_ADVERTISE_APPEARANCE;

   if((TypeLength = Env->GetArrayLength(DataTypes)) > 0)
   {
      if((TLVList = (DEVM_Tag_Length_Value_t *)BTPS_AllocateMemory(sizeof(DEVM_Tag_Length_Value_t) * TypeLength)) != NULL)
      {
         if((TypeArray = Env->GetIntArrayElements(DataTypes, JNI_FALSE)) != NULL)
         {
            for(Index=0;Index<TypeLength && !Error;Index++)
            {
               TLVList[Index].DataType = TypeArray[Index];

               DataArray = (jbyteArray)Env->GetObjectArrayElement(DataList, Index);
              
               if(!Env->ExceptionCheck())
               {
                  if(DataArray)
                  {
                     DataLength = Env->GetArrayLength(DataArray);

                     if((TLVList[Index].DataBuffer = (Byte_t *)BTPS_AllocateMemory(DataLength)) != NULL)
                     {
                        if((DataElems = Env->GetByteArrayElements(DataArray, JNI_FALSE)) != NULL)
                        {
                           TLVList[Index].DataLength = DataLength;
                           BTPS_MemCopy(TLVList[Index].DataBuffer, (Byte_t *)DataElems, DataLength);

                           Env->ReleaseByteArrayElements(DataArray, DataElems, 0);
                        }
                        else
                        {
                           Error      = TRUE;
                           ErrorIndex = Index+1;
                        }
                     }
                     else
                     {
                        Error      = TRUE;
                        ErrorIndex = Index;
                     }
                  }
                  else
                  {
                     TLVList[Index].DataLength = 0;
                     TLVList[Index].DataBuffer = NULL;
                  }
               }
               else
               {
                  Error      = TRUE;
                  ErrorIndex = Index;
               }
            }

            Env->ReleaseIntArrayElements(DataTypes, TypeArray, 0);

            if((!Error) && (ret_val = DEVM_ConvertParsedTLVDataToRaw(TypeLength, TLVList, 0, NULL)) > 0)
            {
               RawDataLength = ret_val;
               ErrorIndex    = TypeLength;

               if((RawData = (Byte_t *)BTPS_AllocateMemory(RawDataLength)) != NULL)
               {
                  if((ret_val = DEVM_ConvertParsedTLVDataToRaw(TypeLength, TLVList, RawDataLength, RawData)) > 0)
                  {
                     AdvertisingInformation.AdvertisingFlags      = CFlags;
                     AdvertisingInformation.AdvertisingDuration   = Duration;
                     AdvertisingInformation.AdvertisingDataLength = RawDataLength;
                     AdvertisingInformation.AdvertisingData       = RawData;

                     ret_val = DEVM_StartAdvertising(&AdvertisingInformation);
                  }

                  BTPS_FreeMemory(RawData);
               }
            }

            for(Index=0;Index<(Error?ErrorIndex:TypeLength);Index++)
               BTPS_FreeMemory(TLVList[Index].DataBuffer);

         }
         else
            Error = TRUE;

         BTPS_FreeMemory(TLVList);
      }
      else
         Error = TRUE;

      if(Error)
      {
         ret_val = -1;

         if(!(Env->ExceptionCheck()))
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }
   else
   {
      AdvertisingInformation.AdvertisingFlags      = CFlags;
      AdvertisingInformation.AdvertisingDuration   = Duration;
      AdvertisingInformation.AdvertisingDataLength = 0;
      AdvertisingInformation.AdvertisingData       = NULL;

      /* No advertising data is being set.                              */
      ret_val = DEVM_StartAdvertising(&AdvertisingInformation);
   }

   return(ret_val);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    stopLowEnergyAdvertisingNative
 * Signature: ()I
 */
static jint StopLowEnergyAdvertisingNative(JNIEnv *Env, jobject Object)
{
   return DEVM_StopAdvertising(0);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryRemoteDeviceListNative
 * Signature: (II[[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;)I
 */
static jint QueryRemoteDeviceListNative(JNIEnv *Env, jobject Object, jint RemoteDeviceFilter, jint ClassOfDeviceMask, jobjectArray RemoteDeviceList)
{
   jint              Result;
   jobject           Address;
   BD_ADDR_t        *NativeAddressList;
   jobjectArray      JavaAddressList;
   unsigned int      Index;
   unsigned int      TotalNumberDevices;
   Class_of_Device_t ClassOfDevice;

   if(RemoteDeviceList)
   {
      ASSIGN_CLASS_OF_DEVICE(ClassOfDevice, (Byte_t)(ClassOfDeviceMask >> 16), (Byte_t)(ClassOfDeviceMask >> 8), (Byte_t)(ClassOfDeviceMask));

      if((Result = DEVM_QueryRemoteDeviceList(RemoteDeviceFilter, ClassOfDevice, 0, NULL, &TotalNumberDevices)) >= 0)
      {
         if(TotalNumberDevices > 0)
         {
            if((NativeAddressList = (BD_ADDR_t*)malloc(TotalNumberDevices * sizeof(BD_ADDR_t))) != NULL)
            {
               if((Result = DEVM_QueryRemoteDeviceList(RemoteDeviceFilter, ClassOfDevice, TotalNumberDevices, NativeAddressList, NULL)) >= 0)
               {
                  TotalNumberDevices = Result;

                  if((JavaAddressList = Env->NewObjectArray(Result, GetBluetoothAddressClass(Env), NULL)) != NULL)
                  {
                     Env->SetObjectArrayElement(RemoteDeviceList, 0, JavaAddressList);

                     for(Index = 0; Index < TotalNumberDevices; Index++)
                     {
                        if((Address = NewBluetoothAddress(Env, NativeAddressList[Index])) != NULL)
                        {
                           Env->SetObjectArrayElement(JavaAddressList, Index, Address);
                           Env->DeleteLocalRef(Address);
                        }
                        else
                        {
                           Env->DeleteLocalRef(JavaAddressList);
                           JavaAddressList = NULL;
                           break;
                        }
                     }

                     if(JavaAddressList)
                        Env->DeleteLocalRef(JavaAddressList);
                  }
               }

               free(NativeAddressList);
            }
            else
            {
               Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
            }
         }
         else
         {
            if((JavaAddressList = Env->NewObjectArray(0, GetBluetoothAddressClass(Env), NULL)) != NULL)
            {
               Env->SetObjectArrayElement(RemoteDeviceList, 0, JavaAddressList);
               Env->DeleteLocalRef(JavaAddressList);
               Result = 0;
            }
            else
               Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         }
      }
   }
   else
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryRemoteDevicePropertiesNative
 * Signature: (BBBBBBZ[Ljava/lang/String;[I[B)I
 */
static jint QueryRemoteDevicePropertiesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean ForceUpdate, jobjectArray Names, jintArray Properties, jbyteArray PriorAddress)
{
   jint                            Result;
   jint                           *PropertiesArrayElements;
   jbyte                          *PriorAddressArrayElements;
   jstring                         JavaString;
   BD_ADDR_t                       RemoteDeviceAddress;
   DEVM_Remote_Device_Properties_t DeviceProperties;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((Result = DEVM_QueryRemoteDeviceProperties(RemoteDeviceAddress, ((ForceUpdate == JNI_FALSE) ? 0 : DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_FORCE_UPDATE), &DeviceProperties)) == 0)
   {
      if((PropertiesArrayElements = Env->GetIntArrayElements(Properties, NULL)) != NULL)
      {
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_CLASS_OF_DEVICE]  = (((DeviceProperties.ClassOfDevice.Class_of_Device2 << 16) & 0x00FF0000) | ((DeviceProperties.ClassOfDevice.Class_of_Device1 << 8) & 0x0000FF00) | ((DeviceProperties.ClassOfDevice.Class_of_Device0) & 0x000000FF));
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_RSSI]             = DeviceProperties.RSSI;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_TRANSMIT_POWER]   = DeviceProperties.TransmitPower;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_SNIFF_INTERVAL]   = DeviceProperties.SniffInterval;

         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]     = ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)                  ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_NAME_KNOWN                  : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID      : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED)            ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED            : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED         : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_ENCRYPTED)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED    : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_SNIFF_MODE)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE   : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY      : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_AUTHENTICATED_KEY)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY           : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_SERVICES_KNOWN              : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_TX_POWER_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN              : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_EIR_DATA_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN              : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_SERVICES_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN           : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_APPEARANCE_KNOWN)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN         : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE    : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_CURRENTLY_ENCRYPTED) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_AUTHENTICATED_KEY)        ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY        : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_INITIATED_LOCALLY)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY   : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_TX_POWER_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN           : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY  : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)             ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR      : 0);

         if(DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_APPLICATION_INFO] = DeviceProperties.ApplicationData.ApplicationInfo;
         else
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_APPLICATION_INFO] = 0;

         switch(DeviceProperties.BLEAddressType)
         {
            case atPublic:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PUBLIC;
               break;
            case atStatic:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_STATIC;
               break;
            case atPrivate_Resolvable:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_RESOLVABLE;
               break;
            case atPrivate_NonResolvable:
            default:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_NONRESOLVABLE;
               break;
         }

         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_RSSI] = DeviceProperties.LE_RSSI;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_TRANSMIT_POWER] = DeviceProperties.LETransmitPower;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_APPEARANCE] = DeviceProperties.DeviceAppearance;

         if((JavaString = Env->NewStringUTF(DeviceProperties.DeviceName)) != NULL)
         {
            Env->SetObjectArrayElement(Names, com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_STRING_INDEX_NAME, JavaString);
            Env->DeleteLocalRef(JavaString);

            if((!Env->ExceptionCheck()) && (DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID))
            {
               if(DeviceProperties.ApplicationData.FriendlyNameLength == 0)
                  Env->SetObjectArrayElement(Names, com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_STRING_INDEX_FRIENDLY_NAME, NULL);
               else
               {
                  if((JavaString = Env->NewStringUTF(DeviceProperties.ApplicationData.FriendlyName)))
                  {
                     Env->SetObjectArrayElement(Names, com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_STRING_INDEX_FRIENDLY_NAME, JavaString);
                     Env->DeleteLocalRef(JavaString);
                  }
               }
            }
         }

         if((PriorAddressArrayElements = Env->GetByteArrayElements(PriorAddress, NULL)) != NULL)
         {
            PriorAddressArrayElements[0] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR5;
            PriorAddressArrayElements[1] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR4;
            PriorAddressArrayElements[2] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR3;
            PriorAddressArrayElements[3] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR2;
            PriorAddressArrayElements[4] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR1;
            PriorAddressArrayElements[5] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR0;

            Env->ReleaseByteArrayElements(PriorAddress, PriorAddressArrayElements, 0);
         }

         Env->ReleaseIntArrayElements(Properties, PropertiesArrayElements, 0);
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryRemoteLowEnergyDevicePropertiesNative
 * Signature: (BBBBBBZ[Ljava/lang/String;[I[B)I
 */
static jint QueryRemoteLowEnergyDevicePropertiesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean ForceUpdate, jobjectArray Names, jintArray Properties, jbyteArray PriorAddress)
{
   jint                            Result;
   jint                           *PropertiesArrayElements;
   jbyte                          *PriorAddressArrayElements;
   jstring                         JavaString;
   BD_ADDR_t                       RemoteDeviceAddress;
   DEVM_Remote_Device_Properties_t DeviceProperties;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((Result = DEVM_QueryRemoteDeviceProperties(RemoteDeviceAddress, (DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_LOW_ENERGY | ((ForceUpdate == JNI_FALSE) ? 0 : DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_FORCE_UPDATE)), &DeviceProperties)) == 0)
   {
      if((PropertiesArrayElements = Env->GetIntArrayElements(Properties, NULL)) != NULL)
      {
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_CLASS_OF_DEVICE]  = (((DeviceProperties.ClassOfDevice.Class_of_Device2 << 16) & 0x00FF0000) | ((DeviceProperties.ClassOfDevice.Class_of_Device1 << 8) & 0x0000FF00) | ((DeviceProperties.ClassOfDevice.Class_of_Device0) & 0x000000FF));
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_RSSI]             = DeviceProperties.RSSI;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_TRANSMIT_POWER]   = DeviceProperties.TransmitPower;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_SNIFF_INTERVAL]   = DeviceProperties.SniffInterval;

         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]     = ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)                  ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_NAME_KNOWN                  : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID      : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED)            ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED            : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED         : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_ENCRYPTED)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED    : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_CURRENTLY_SNIFF_MODE)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE   : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY)      ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY      : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_AUTHENTICATED_KEY)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY           : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_SERVICES_KNOWN              : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_TX_POWER_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN              : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_EIR_DATA_KNOWN)              ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN              : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_SERVICES_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN           : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_APPEARANCE_KNOWN)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN         : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE)    ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE    : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_CURRENTLY_ENCRYPTED) ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_AUTHENTICATED_KEY)        ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY        : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_INITIATED_LOCALLY)   ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY   : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_TX_POWER_KNOWN)           ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN           : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)         ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY  : 0);
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS]    |= ((DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)             ? com_stonestreetone_bluetopiapm_DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR      : 0);

         if(DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_APPLICATION_INFO] = DeviceProperties.ApplicationData.ApplicationInfo;
         else
            PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_APPLICATION_INFO] = 0;

         switch(DeviceProperties.BLEAddressType)
         {
            case atPublic:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PUBLIC;
               break;
            case atStatic:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_STATIC;
               break;
            case atPrivate_Resolvable:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_RESOLVABLE;
               break;
            case atPrivate_NonResolvable:
            default:
               PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE] = com_stonestreetone_bluetopiapm_DEVM_ADDRESS_TYPE_PRIVATE_NONRESOLVABLE;
               break;
         }

         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_RSSI] = DeviceProperties.LE_RSSI;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_TRANSMIT_POWER] = DeviceProperties.LETransmitPower;
         PropertiesArrayElements[com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_PROP_INDEX_DEVICE_APPEARANCE] = DeviceProperties.DeviceAppearance;

         if((JavaString = Env->NewStringUTF(DeviceProperties.DeviceName)) != NULL)
         {
            Env->SetObjectArrayElement(Names, com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_STRING_INDEX_NAME, JavaString);
            Env->DeleteLocalRef(JavaString);

            if((!Env->ExceptionCheck()) && (DeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID))
            {
               if(DeviceProperties.ApplicationData.FriendlyNameLength == 0)
                  Env->SetObjectArrayElement(Names, com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_STRING_INDEX_FRIENDLY_NAME, NULL);
               else
               {
                  if((JavaString = Env->NewStringUTF(DeviceProperties.ApplicationData.FriendlyName)))
                  {
                     Env->SetObjectArrayElement(Names, com_stonestreetone_bluetopiapm_DEVM_NATIVE_REMOTE_STRING_INDEX_FRIENDLY_NAME, JavaString);
                     Env->DeleteLocalRef(JavaString);
                  }
               }
            }
         }

         if((PriorAddressArrayElements = Env->GetByteArrayElements(PriorAddress, NULL)) != NULL)
         {
            PriorAddressArrayElements[0] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR5;
            PriorAddressArrayElements[1] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR4;
            PriorAddressArrayElements[2] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR3;
            PriorAddressArrayElements[3] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR2;
            PriorAddressArrayElements[4] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR1;
            PriorAddressArrayElements[5] = DeviceProperties.PriorResolvableBD_ADDR.BD_ADDR0;

            Env->ReleaseByteArrayElements(PriorAddress, PriorAddressArrayElements, 0);
         }

         Env->ReleaseIntArrayElements(Properties, PropertiesArrayElements, 0);
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryRemoteDeviceEIRDataNative
 * Signature: (BBBBBB[[I[[[B)I
 */
static jint QueryRemoteDeviceEIRDataNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jobjectArray TypesOut, jobjectArray DataOut)
{
   jint                              Result;
   jint                             *TypeArrayElements;
   jsize                             NumberTLVs;
   BD_ADDR_t                         RemoteDeviceAddress;
   jintArray                         TypeArray;
   jbyteArray                        DataEntry;
   unsigned int                      Index;
   jobjectArray                      DataArray;
   DEVM_Tag_Length_Value_t          *TLVEntryList;
   Extended_Inquiry_Response_Data_t  EIRData;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((Result = DEVM_QueryRemoteDeviceEIRData(RemoteDeviceAddress, &EIRData)) > 0)
   {
      if((Result = DEVM_ConvertRawEIRDataToParsedEIRData(&EIRData, 0, NULL)) > 0)
      {
         NumberTLVs = (jsize)Result;

         if((TLVEntryList = (DEVM_Tag_Length_Value_t *)malloc(NumberTLVs * sizeof(DEVM_Tag_Length_Value_t))) != NULL)
         {
            if(DEVM_ConvertRawEIRDataToParsedEIRData(&EIRData, NumberTLVs, TLVEntryList) >= NumberTLVs)
            {
               if((TypeArray = Env->NewIntArray(NumberTLVs)) != NULL)
               {
                  if((TypeArrayElements = Env->GetIntArrayElements(TypeArray, NULL)) != NULL)
                  {
                     if((DataArray = Env->NewObjectArray(NumberTLVs, Env->FindClass("[B"), NULL)) != NULL)
                     {
                        for(Index = 0; Index < NumberTLVs; Index++)
                        {
                           TypeArrayElements[Index] = TLVEntryList[Index].DataType;

                           if((DataEntry = Env->NewByteArray(TLVEntryList[Index].DataLength)) != NULL)
                           {
                              Env->SetByteArrayRegion(DataEntry, 0, TLVEntryList[Index].DataLength, (jbyte *)(TLVEntryList[Index].DataBuffer));
                              Env->SetObjectArrayElement(DataArray, Index, DataEntry);
                              Env->DeleteLocalRef(DataEntry);
                           }
                        }

                        Env->SetObjectArrayElement(TypesOut, 0, TypeArray);
                        Env->SetObjectArrayElement(DataOut, 0, DataArray);
                     }
                     else
                     {
                        /* No need to throw an exception as             */
                        /* NewObjectArray has already done so.          */
                        Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                     }

                     Env->ReleaseIntArrayElements(TypeArray, TypeArrayElements, 0);
                  }
                  else
                  {
                     /* Can't access array elements. Probably a memory  */
                     /* issue.                                          */
                     Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                     Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                  }
               }
               else
               {
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                  Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
               }
            }
            else
            {
               /* Parse error. */
               Result = -1;
            }

            free(TLVEntryList);
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
      {
         /* Not parsable or no entries exist in the EIR data.           */
         Result = -1;
      }
   }
   else
   {
      /* EIR Data is not available.                                     */
      Result = -1;
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryRemoteLowEnergyDeviceAdvertisingDataNative
 * Signature: (BBBBBB[[I[[[BZ)I
 */
static jint QueryRemoteLowEnergyDeviceAdvertisingDataNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jobjectArray TypesOut, jobjectArray DataOut, jboolean ScanResponse)
{
   jint                     Result;
   jint                    *TypeArrayElements;
   jsize                    NumberTLVs;
   BD_ADDR_t                RemoteDeviceAddress;
   jintArray                TypeArray;
   jbyteArray               DataEntry;
   unsigned int             Index;
   jobjectArray             DataArray;
   DEVM_Tag_Length_Value_t *TLVEntryList;
   Advertising_Data_t       AdvertisingData;
   Advertising_Data_t       ScanResponseData;
   Advertising_Data_t      *DataSource;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = DEVM_QueryRemoteDeviceAdvertisingData(RemoteDeviceAddress, &AdvertisingData, &ScanResponseData);

   if(((ScanResponse == JNI_FALSE) && (Result >= 1)) || (Result == 2))
   {
      DataSource = ((ScanResponse == JNI_FALSE) ? &AdvertisingData : &ScanResponseData);

      if((Result = DEVM_ConvertRawAdvertisingDataToParsedAdvertisingData(DataSource, 0, NULL)) > 0)
      {
         NumberTLVs = (jsize)Result;

         if((TLVEntryList = (DEVM_Tag_Length_Value_t *)malloc(NumberTLVs * sizeof(DEVM_Tag_Length_Value_t))) != NULL)
         {
            if(DEVM_ConvertRawAdvertisingDataToParsedAdvertisingData(DataSource, NumberTLVs, TLVEntryList) >= NumberTLVs)
            {
               if((TypeArray = Env->NewIntArray(NumberTLVs)) != NULL)
               {
                  if((TypeArrayElements = Env->GetIntArrayElements(TypeArray, NULL)) != NULL)
                  {
                     if((DataArray = Env->NewObjectArray(NumberTLVs, Env->FindClass("[B"), NULL)) != NULL)
                     {
                        for(Index = 0; Index < NumberTLVs; Index++)
                        {
                           TypeArrayElements[Index] = TLVEntryList[Index].DataType;

                           if((DataEntry = Env->NewByteArray(TLVEntryList[Index].DataLength)) != NULL)
                           {
                              Env->SetByteArrayRegion(DataEntry, 0, TLVEntryList[Index].DataLength, (jbyte *)(TLVEntryList[Index].DataBuffer));
                              Env->SetObjectArrayElement(DataArray, Index, DataEntry);
                              Env->DeleteLocalRef(DataEntry);
                           }
                        }

                        Env->SetObjectArrayElement(TypesOut, 0, TypeArray);
                        Env->SetObjectArrayElement(DataOut, 0, DataArray);
                     }
                     else
                     {
                        /* No need to throw an exception as             */
                        /* NewObjectArray has already done so.          */
                        Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                     }

                     Env->ReleaseIntArrayElements(TypeArray, TypeArrayElements, 0);
                  }
                  else
                  {
                     /* Can't access array elements. Probably a memory  */
                     /* issue.                                          */
                     Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                     Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                  }
               }
               else
               {
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                  Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
               }
            }
            else
            {
               /* Parse error. */
               Result = -1;
            }

            free(TLVEntryList);
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
      {
         /* Not parsable or no entries exist in the Advertising data.   */
         Result = -1;
      }
   }
   else
   {
      /* Advertising Record Data is not available.                      */
      Result = -1;
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryRemoteDeviceServicesNative
 * Signature: (BBBBBBZ[[B)I
 */
static jint QueryRemoteDeviceServicesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean ForceUpdate, jobjectArray ServiceDataBuffer)
{
   jint         Result;
   jbyte       *sdpDataBuffer;
   BD_ADDR_t    RemoteDeviceAddress;
   jbyteArray   sdpDataJavaArray;
   unsigned int TotalDataLength;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if(ServiceDataBuffer)
   {
      if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, 0, 0, NULL, &TotalDataLength)) >= 0)
      {
         if((sdpDataBuffer = (jbyte *)malloc(TotalDataLength)) != NULL)
         {
            if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, ((ForceUpdate == JNI_FALSE) ? 0 : DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_FORCE_UPDATE), TotalDataLength, (Byte_t*)sdpDataBuffer, NULL)) >= 0)
            {
               TotalDataLength = Result;

               if((sdpDataJavaArray = Env->NewByteArray(TotalDataLength)) != NULL)
               {
                  Env->SetByteArrayRegion(sdpDataJavaArray, 0, TotalDataLength, sdpDataBuffer);
                  Env->SetObjectArrayElement(ServiceDataBuffer, 0, sdpDataJavaArray);
               }
            }

            free(sdpDataBuffer);
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
   }
   else
      Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, ((ForceUpdate == JNI_FALSE) ? FALSE : TRUE), 0, NULL, NULL);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    queryRemoteLowEnergyDeviceServicesNative
 * Signature: (BBBBBBZ[[B)I
 */
static jint QueryRemoteLowEnergyDeviceServicesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean ForceUpdate, jobjectArray ServiceDataBuffer)
{
   jint         Result;
   jbyte       *serviceData;
   BD_ADDR_t    RemoteDeviceAddress;
   jbyteArray   serviceDataJavaArray;
   unsigned int TotalDataLength;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if(ServiceDataBuffer)
   {
      if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY, 0, NULL, &TotalDataLength)) >= 0)
      {
         if((serviceData = (jbyte *)malloc(TotalDataLength)) != NULL)
         {
            if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, (DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY | ((ForceUpdate == JNI_FALSE) ? 0 : DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_FORCE_UPDATE)), TotalDataLength, (Byte_t*)serviceData, NULL)) >= 0)
            {
               TotalDataLength = Result;

               if((serviceDataJavaArray = Env->NewByteArray(TotalDataLength)) != NULL)
               {
                  Env->SetByteArrayRegion(serviceDataJavaArray, 0, TotalDataLength, serviceData);
                  Env->SetObjectArrayElement(ServiceDataBuffer, 0, serviceDataJavaArray);
               }
            }

            free(serviceData);
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
   }
   else
      Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, (DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY | ((ForceUpdate == JNI_FALSE) ? 0 : DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_FORCE_UPDATE)), 0, NULL, NULL);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    addRemoteDeviceNative
 * Signature: (BBBBBBI)I
 */
static jint AddRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ClassOfDevice)
{
   BD_ADDR_t         RemoteDeviceAddress;
   Class_of_Device_t ClassOfDeviceStruct;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);
   ASSIGN_CLASS_OF_DEVICE(ClassOfDeviceStruct, (Byte_t)(ClassOfDevice >> 16), (Byte_t)(ClassOfDevice >> 8), (Byte_t)(ClassOfDevice));

   return DEVM_AddRemoteDevice(RemoteDeviceAddress, ClassOfDeviceStruct, NULL);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    addRemoteDeviceNative
 * Signature: (BBBBBBILjava/lang/String;I)I
 */
static jint AddRemoteDeviceNativeWithApplicationData(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ClassOfDevice, jstring FriendlyName, jint ApplicationInfo)
{
   BD_ADDR_t                             RemoteDeviceAddress;
   const char                           *NameBuffer;
   Class_of_Device_t                     ClassOfDeviceStruct;
   DEVM_Remote_Device_Application_Data_t AppData;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);
   ASSIGN_CLASS_OF_DEVICE(ClassOfDeviceStruct, (Byte_t)(ClassOfDevice >> 16), (Byte_t)(ClassOfDevice >> 8), (Byte_t)(ClassOfDevice));

   memset(&AppData, 0, sizeof(DEVM_Remote_Device_Application_Data_t));

   if((FriendlyName) && ((NameBuffer = Env->GetStringUTFChars(FriendlyName, NULL)) != NULL))
   {
      strncpy(AppData.FriendlyName, NameBuffer, MAX_NAME_LENGTH);
      AppData.FriendlyNameLength = Env->GetStringUTFLength(FriendlyName);

      Env->ReleaseStringUTFChars(FriendlyName, NameBuffer);
   }

   AppData.ApplicationInfo = ApplicationInfo;

   return DEVM_AddRemoteDevice(RemoteDeviceAddress, ClassOfDeviceStruct, &AppData);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    deleteRemoteDeviceNative
 * Signature: (BBBBBB)I
 */
static jint DeleteRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_DeleteRemoteDevice(RemoteDeviceAddress);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    updateRemoteDeviceApplicationDataNative
 * Signature: (BBBBBBLjava/lang/String;I)I
 */
static jint UpdateRemoteDeviceApplicationDataNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring FriendlyName, jint ApplicationInfo)
{
   BD_ADDR_t                             RemoteDeviceAddress;
   const char                           *NameBuffer;
   DEVM_Remote_Device_Application_Data_t AppData;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   memset(&AppData, 0, sizeof(DEVM_Remote_Device_Application_Data_t));

   if((FriendlyName) && ((NameBuffer = Env->GetStringUTFChars(FriendlyName, NULL)) != NULL))
   {
      strncpy(AppData.FriendlyName, NameBuffer, MAX_NAME_LENGTH);
      AppData.FriendlyNameLength = Env->GetStringUTFLength(FriendlyName);

      Env->ReleaseStringUTFChars(FriendlyName, NameBuffer);
   }

   AppData.ApplicationInfo = ApplicationInfo;

   return DEVM_UpdateRemoteDeviceApplicationData(RemoteDeviceAddress, &AppData);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    deleteRemoteDevicesNative
 * Signature: (I)I
 */
static jint DeleteRemoteDevicesNative(JNIEnv *Env, jobject Object, jint DeleteDevicesFilter)
{
   return DEVM_DeleteRemoteDevices(DeleteDevicesFilter);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    pairWithRemoteDeviceNative
 * Signature: (BBBBBBZ)I
 */
static jint PairWithRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean ForcePair)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_PairWithRemoteDevice(RemoteDeviceAddress, ((ForcePair == JNI_FALSE) ? 0 : DEVM_PAIR_WITH_REMOTE_DEVICE_FLAGS_FORCE_PAIR));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    pairWithRemoteLowEnergyDeviceNative
 * Signature: (BBBBBBZZ)I
 */
static jint PairWithRemoteLowEnergyDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean ForcePair, jboolean StayConnected)
{
   BD_ADDR_t     RemoteDeviceAddress;
   unsigned long Flags;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Flags  = DEVM_PAIR_WITH_REMOTE_DEVICE_FLAGS_LOW_ENERGY;;
   Flags |= ((ForcePair == JNI_FALSE)     ? 0 : DEVM_PAIR_WITH_REMOTE_DEVICE_FLAGS_FORCE_PAIR);
   Flags |= ((StayConnected == JNI_FALSE) ? 0 : DEVM_PAIR_WITH_REMOTE_DEVICE_FLAGS_KEEP_CONNECTION);

   return DEVM_PairWithRemoteDevice(RemoteDeviceAddress, Flags);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    cancelPairWithRemoteDeviceNative
 * Signature: (BBBBBB)I
 */
static jint CancelPairWithRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_CancelPairWithRemoteDevice(RemoteDeviceAddress);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    unPairRemoteDeviceNative
 * Signature: (BBBBBB)I
 */
static jint UnPairRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_UnPairRemoteDevice(RemoteDeviceAddress, 0);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    unPairRemoteLowEnergyDeviceNative
 * Signature: (BBBBBB)I
 */
static jint UnPairRemoteLowEnergyDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_UnPairRemoteDevice(RemoteDeviceAddress, DEVM_UNPAIR_REMOTE_DEVICE_FLAGS_LOW_ENERGY);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    authenticateRemoteDeviceNative
 * Signature: (BBBBBB)I
 */
static jint AuthenticateRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_AuthenticateRemoteDevice(RemoteDeviceAddress, 0);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    authenticateRemoteLowEnergyDeviceNative
 * Signature: (BBBBBB)I
 */
static jint AuthenticateRemoteLowEnergyDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_AuthenticateRemoteDevice(RemoteDeviceAddress, DEVM_AUTHENTICATE_REMOTE_DEVICE_FLAGS_LOW_ENERGY);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    encryptRemoteDeviceNative
 * Signature: (BBBBBB)I
 */
static jint EncryptRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_EncryptRemoteDevice(RemoteDeviceAddress, 0);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    encryptRemoteLowEnergyDeviceNative
 * Signature: (BBBBBB)I
 */
static jint EncryptRemoteLowEnergyDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_EncryptRemoteDevice(RemoteDeviceAddress, DEVM_ENCRYPT_REMOTE_DEVICE_FLAGS_LOW_ENERGY);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    connectWithRemoteDeviceNative
 * Signature: (BBBBBBI)I
 */
static jint ConnectWithRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ConnectFlags)
{
   BD_ADDR_t     RemoteDeviceAddress;
   unsigned long Flags;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Flags  = 0;
   Flags |= ((ConnectFlags & com_stonestreetone_bluetopiapm_DEVM_CONNECT_FLAGS_AUTHENTICATE) ? DEVM_CONNECT_WITH_REMOTE_DEVICE_FLAGS_AUTHENTICATE : 0);
   Flags |= ((ConnectFlags & com_stonestreetone_bluetopiapm_DEVM_CONNECT_FLAGS_ENCRYPT)      ? DEVM_CONNECT_WITH_REMOTE_DEVICE_FLAGS_ENCRYPT      : 0);

   return DEVM_ConnectWithRemoteDevice(RemoteDeviceAddress, Flags);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    connectWithRemoteLowEnergyDeviceNative
 * Signature: (BBBBBBI)I
 */
static jint ConnectWithRemoteLowEnergyDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ConnectFlags)
{
   BD_ADDR_t     RemoteDeviceAddress;
   unsigned long Flags;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Flags  = DEVM_CONNECT_WITH_REMOTE_DEVICE_FORCE_LOW_ENERGY;
   Flags |= ((ConnectFlags & com_stonestreetone_bluetopiapm_DEVM_CONNECT_FLAGS_AUTHENTICATE) ? DEVM_CONNECT_WITH_REMOTE_DEVICE_FLAGS_AUTHENTICATE : 0);
   Flags |= ((ConnectFlags & com_stonestreetone_bluetopiapm_DEVM_CONNECT_FLAGS_ENCRYPT)      ? DEVM_CONNECT_WITH_REMOTE_DEVICE_FLAGS_ENCRYPT      : 0);

   return DEVM_ConnectWithRemoteDevice(RemoteDeviceAddress, Flags);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    disconnectRemoteDeviceNative
 * Signature: (BBBBBBZ)I
 */
static jint DisconnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean Force)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_DisconnectRemoteDevice(RemoteDeviceAddress, ((Force == JNI_FALSE) ? 0 : DEVM_DISCONNECT_FROM_REMOTE_DEVICE_FLAGS_FORCE));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    disconnectRemoteLowEnergyDeviceNative
 * Signature: (BBBBBBZ)I
 */
static jint DisconnectRemoteLowEnergyDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean Force)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_DisconnectRemoteDevice(RemoteDeviceAddress, (DEVM_DISCONNECT_FROM_REMOTE_DEVICE_FLAGS_LOW_ENERGY | ((Force == JNI_FALSE) ? 0 : DEVM_DISCONNECT_FROM_REMOTE_DEVICE_FLAGS_FORCE)));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    setRemoteDeviceLinkActiveNative
 * Signature: (BBBBBB)I
 */
static jint SetRemoteDeviceLinkActiveNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   return DEVM_SetRemoteDeviceLinkActive(RemoteDeviceAddress);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendPinCodeNative
 * Signature: (BBBBBB[B)I
 */
static jint SendPinCodeNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyteArray pinCode)
{
   jint                              Result;
   jbyte                            *PinCodeBytes;
   jsize                             PinCodeLength;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   if(pinCode)
   {
      PinCodeLength = Env->GetArrayLength(pinCode);
      if(PinCodeLength > 16)
         PinCodeLength = 16;

      memset(&AuthInfo, 0, sizeof(AuthInfo));
      ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
      AuthInfo.AuthenticationAction     = DEVM_AUTHENTICATION_ACTION_PIN_CODE_RESPONSE;
      AuthInfo.AuthenticationDataLength = PinCodeLength;

      if((PinCodeBytes = Env->GetByteArrayElements(pinCode, NULL)) != NULL)
      {
         memcpy(&AuthInfo.AuthenticationData.PINCode, PinCodeBytes, PinCodeLength);
         Env->ReleaseByteArrayElements(pinCode, PinCodeBytes, JNI_ABORT);

        if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
        {
           Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
           ReleaseLocalData(Env, Object, LocalData);
        }
        else
           Result = -1;
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }
   else
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendUserConfirmationNative
 * Signature: (BBBBBBZ)I
 */
static jint SendUserConfirmationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean Accept)
{
   jint                              Result;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   memset(&AuthInfo, 0, sizeof(AuthInfo));
   ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
   AuthInfo.AuthenticationAction            = DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE;
   AuthInfo.AuthenticationDataLength        = sizeof(AuthInfo.AuthenticationData.Confirmation);
   AuthInfo.AuthenticationData.Confirmation = ((Accept == JNI_FALSE) ? FALSE : TRUE);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendPasskeyNative
 * Signature: (BBBBBBI)I
 */
static jint SendPasskeyNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint Passkey)
{
   jint                              Result;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   memset(&AuthInfo, 0, sizeof(AuthInfo));
   ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
   AuthInfo.AuthenticationAction       = DEVM_AUTHENTICATION_ACTION_PASSKEY_RESPONSE;
   AuthInfo.AuthenticationDataLength   = sizeof(AuthInfo.AuthenticationData.Passkey);
   AuthInfo.AuthenticationData.Passkey = Passkey;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendOutOfBandDataNative
 * Signature: (BBBBBB[B[B)I
 */
static jint SendOutOfBandDataNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyteArray SimplePairingHash, jbyteArray SimplePairingRandomizer)
{
   jint                              Result;
   jbyte                            *Buffer;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   if((SimplePairingHash) && (SimplePairingRandomizer))
   {
      memset(&AuthInfo, 0, sizeof(AuthInfo));
      ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
      AuthInfo.AuthenticationAction     = DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_RESPONSE;
      AuthInfo.AuthenticationDataLength = sizeof(AuthInfo.AuthenticationData.OutOfBandData);

      if((Buffer = Env->GetByteArrayElements(SimplePairingHash, NULL)) != NULL)
      {
         ASSIGN_PAIRING_HASH(AuthInfo.AuthenticationData.OutOfBandData.Simple_Pairing_Hash, Buffer[0], Buffer[1], Buffer[2], Buffer[3], Buffer[4], Buffer[5], Buffer[6], Buffer[7], Buffer[8], Buffer[9], Buffer[10], Buffer[11], Buffer[12], Buffer[13], Buffer[14], Buffer[15]);
         Env->ReleaseByteArrayElements(SimplePairingHash, Buffer, JNI_ABORT);

         if((Buffer = Env->GetByteArrayElements(SimplePairingRandomizer, NULL)) != NULL)
         {
            ASSIGN_PAIRING_RANDOMIZER(AuthInfo.AuthenticationData.OutOfBandData.Simple_Pairing_Randomizer, Buffer[0], Buffer[1], Buffer[2], Buffer[3], Buffer[4], Buffer[5], Buffer[6], Buffer[7], Buffer[8], Buffer[9], Buffer[10], Buffer[11], Buffer[12], Buffer[13], Buffer[14], Buffer[15]);
            Env->ReleaseByteArrayElements(SimplePairingRandomizer, Buffer, JNI_ABORT);

            if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
            {  
               Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
               ReleaseLocalData(Env, Object, LocalData);
            }
            else
               Result = -1;
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }
   else
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendIOCapabilitiesNative
 * Signature: (BBBBBBIZZI)I
 */
static jint SendIOCapabilitiesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint IOCapability, jboolean OutOfBandDataPresent, jboolean MITMProtectionRequired, jint BondingType)
{
   jint                              Result;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   memset(&AuthInfo, 0, sizeof(AuthInfo));
   ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
   AuthInfo.AuthenticationAction                                       = DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_RESPONSE;
   AuthInfo.AuthenticationDataLength                                   = sizeof(AuthInfo.AuthenticationData.IOCapabilities);
   AuthInfo.AuthenticationData.IOCapabilities.OOB_Data_Present         = ((OutOfBandDataPresent == JNI_FALSE) ? FALSE : TRUE);
   AuthInfo.AuthenticationData.IOCapabilities.MITM_Protection_Required = ((MITMProtectionRequired == JNI_FALSE) ? FALSE : TRUE);

   switch(IOCapability)
   {
      case com_stonestreetone_bluetopiapm_DEVM_IO_CAPABILITY_DISPLAY_ONLY:
         AuthInfo.AuthenticationData.IOCapabilities.IO_Capability = icDisplayOnly;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_IO_CAPABILITY_DISPLAY_YES_NO:
         AuthInfo.AuthenticationData.IOCapabilities.IO_Capability = icDisplayYesNo;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_IO_CAPABILITY_KEYBOARD_ONLY:
         AuthInfo.AuthenticationData.IOCapabilities.IO_Capability = icKeyboardOnly;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_IO_CAPABILITY_NO_INPUT_NO_OUTPUT:
      default:
         AuthInfo.AuthenticationData.IOCapabilities.IO_Capability = icNoInputNoOutput;
         break;
   }

   switch(BondingType)
   {
      case com_stonestreetone_bluetopiapm_DEVM_BONDING_TYPE_DEDICATED_BONDING:
         AuthInfo.AuthenticationData.IOCapabilities.Bonding_Type = ibDedicatedBonding;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_BONDING_TYPE_GENERAL_BONDING:
         AuthInfo.AuthenticationData.IOCapabilities.Bonding_Type = ibGeneralBonding;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_BONDING_TYPE_NO_BONDING:
      default:
         AuthInfo.AuthenticationData.IOCapabilities.Bonding_Type = ibNoBonding;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendLowEnergyUserConfirmationNative
 * Signature: (BBBBBBZ)I
 */
static jint SendLowEnergyUserConfirmationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean Accept)
{
   jint                              Result;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   memset(&AuthInfo, 0, sizeof(AuthInfo));
   ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
   AuthInfo.AuthenticationAction            = (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE);
   AuthInfo.AuthenticationDataLength        = sizeof(AuthInfo.AuthenticationData.Confirmation);
   AuthInfo.AuthenticationData.Confirmation = ((Accept == JNI_FALSE) ? FALSE : TRUE);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendLowEnergyPasskeyNative
 * Signature: (BBBBBBI)I
 */
static jint SendLowEnergyPasskeyNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint Passkey)
{
   jint                              Result;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   memset(&AuthInfo, 0, sizeof(AuthInfo));
   ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
   AuthInfo.AuthenticationAction       = (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_PASSKEY_RESPONSE);
   AuthInfo.AuthenticationDataLength   = sizeof(AuthInfo.AuthenticationData.Passkey);
   AuthInfo.AuthenticationData.Passkey = Passkey;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendLowEnergyOutOfBandDataNative
 * Signature: (BBBBBB[B[B)I
 */
static jint SendLowEnergyOutOfBandDataNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyteArray EncryptionKey)
{
   jint                              Result;
   jbyte                            *Buffer;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   if(EncryptionKey)
   {
      memset(&AuthInfo, 0, sizeof(AuthInfo));
      ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
      AuthInfo.AuthenticationAction     = (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_RESPONSE);
      AuthInfo.AuthenticationDataLength = sizeof(AuthInfo.AuthenticationData.OutOfBandData);

      if((Buffer = Env->GetByteArrayElements(EncryptionKey, NULL)) != NULL)
      {
         ASSIGN_ENCRYPTION_KEY(AuthInfo.AuthenticationData.LEOutOfBandData.OOB_Key, Buffer[0], Buffer[1], Buffer[2], Buffer[3], Buffer[4], Buffer[5], Buffer[6], Buffer[7], Buffer[8], Buffer[9], Buffer[10], Buffer[11], Buffer[12], Buffer[13], Buffer[14], Buffer[15]);

         Env->ReleaseByteArrayElements(EncryptionKey, Buffer, JNI_ABORT);

         if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
         {   
            Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = -1;
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }
   else
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    sendLowEnergyIOCapabilitiesNative
 * Signature: (BBBBBBIZZI)I
 */
static jint SendLowEnergyIOCapabilitiesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint IOCapability, jboolean OutOfBandDataPresent, jboolean MITMProtectionRequired, jint BondingType)
{
   jint                              Result;
   LocalData_t                      *LocalData;
   DEVM_Authentication_Information_t AuthInfo;

   memset(&AuthInfo, 0, sizeof(AuthInfo));
   ASSIGN_BD_ADDR(AuthInfo.BD_ADDR, Address1, Address2, Address3, Address4, Address5, Address6);
   AuthInfo.AuthenticationAction                            = (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_RESPONSE);
   AuthInfo.AuthenticationDataLength                        = sizeof(AuthInfo.AuthenticationData.IOCapabilities);
   AuthInfo.AuthenticationData.LEIOCapabilities.OOB_Present = ((OutOfBandDataPresent == JNI_FALSE) ? FALSE : TRUE);
   AuthInfo.AuthenticationData.LEIOCapabilities.MITM        = ((MITMProtectionRequired == JNI_FALSE) ? FALSE : TRUE);

   switch(IOCapability)
   {
      case com_stonestreetone_bluetopiapm_DEVM_LE_IO_CAPABILITY_DISPLAY_ONLY:
         AuthInfo.AuthenticationData.LEIOCapabilities.IO_Capability = licDisplayOnly;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_LE_IO_CAPABILITY_DISPLAY_YES_NO:
         AuthInfo.AuthenticationData.LEIOCapabilities.IO_Capability = licDisplayYesNo;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_LE_IO_CAPABILITY_KEYBOARD_ONLY:
         AuthInfo.AuthenticationData.LEIOCapabilities.IO_Capability = licKeyboardOnly;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_LE_IO_CAPABILITY_KEYBOARD_DISPLAY:
         AuthInfo.AuthenticationData.LEIOCapabilities.IO_Capability = licKeyboardDisplay;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_LE_IO_CAPABILITY_NO_INPUT_NO_OUTPUT:
      default:
         AuthInfo.AuthenticationData.LEIOCapabilities.IO_Capability = licNoInputNoOutput;
         break;
   }

   switch(BondingType)
   {
      case com_stonestreetone_bluetopiapm_DEVM_LE_BONDING_TYPE_BONDING:
         AuthInfo.AuthenticationData.LEIOCapabilities.Bonding_Type = lbtBonding;
         break;
      case com_stonestreetone_bluetopiapm_DEVM_LE_BONDING_TYPE_NO_BONDING:
      default:
         AuthInfo.AuthenticationData.LEIOCapabilities.Bonding_Type = lbtNoBonding;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = DEVM_AuthenticationResponse(LocalData->AuthCallbackID, &AuthInfo);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    querySupportedServicesNative
 * Signature: (BBBBBB)[[J
 */
static jobjectArray QuerySupportedServicesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   int                    Result;
   jlong                 *MSBArrayElements;
   jlong                 *LSBArrayElements;
   Byte_t                *SDPDataBuffer;
   BD_ADDR_t              RemoteDeviceAddress;
   UUID_128_t            *UUIDList;
   jlongArray             MSBArray;
   jlongArray             LSBArray;
   jobjectArray           OuterResultArray;
   unsigned int           UUIDIndex;
   unsigned int           UUIDListLength;
   unsigned int           SDPResponseIndex;
   unsigned int           SDPAttributeIndex;
   unsigned int           TotalServiceDataLength;
   DEVM_Parsed_SDP_Data_t ParsedSDPData;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, FALSE, 0, NULL, &TotalServiceDataLength)) >= 0)
   {
      if((SDPDataBuffer = (Byte_t *)malloc(TotalServiceDataLength)) != NULL)
      {
         if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, FALSE, TotalServiceDataLength, SDPDataBuffer, NULL)) >= 0)
         {
            TotalServiceDataLength = Result;

            if(TotalServiceDataLength > 0)
            {
               if((Result = DEVM_ConvertRawSDPStreamToParsedSDPData(TotalServiceDataLength, SDPDataBuffer, &ParsedSDPData)) == 0)
               {
                  if((UUIDList = (UUID_128_t *)malloc(ParsedSDPData.NumberServiceRecords * UUID_128_SIZE)) != NULL)
                  {
                     memset(UUIDList, 0, (ParsedSDPData.NumberServiceRecords * UUID_128_SIZE));
                     UUIDIndex = 0;

                     for(SDPResponseIndex = 0; SDPResponseIndex < ParsedSDPData.NumberServiceRecords; SDPResponseIndex++)
                     {
                        for(SDPAttributeIndex = 0; SDPAttributeIndex < ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].Number_Attribute_Values; SDPAttributeIndex++)
                        {
                           if(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].SDP_Service_Attribute_Value_Data[SDPAttributeIndex].Attribute_ID == SDP_ATTRIBUTE_ID_SERVICE_CLASS_ID_LIST)
                           {
                              if(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].SDP_Service_Attribute_Value_Data[SDPAttributeIndex].SDP_Data_Element->SDP_Data_Element_Type == deSequence)
                              {
                                 if(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].SDP_Service_Attribute_Value_Data[SDPAttributeIndex].SDP_Data_Element->SDP_Data_Element_Length > 0)
                                 {
                                    switch(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].SDP_Service_Attribute_Value_Data[SDPAttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element_Type)
                                    {
                                       case deUUID_128:
                                          UUIDList[UUIDIndex] = ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].SDP_Service_Attribute_Value_Data[SDPAttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_128;
                                          UUIDIndex++;
                                          break;
                                       case deUUID_32:
                                          SDP_ASSIGN_BASE_UUID(UUIDList[UUIDIndex]);
                                          ASSIGN_SDP_UUID_32_TO_SDP_UUID_128(UUIDList[UUIDIndex], ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].SDP_Service_Attribute_Value_Data[SDPAttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_32);
                                          UUIDIndex++;
                                          break;
                                       case deUUID_16:
                                          SDP_ASSIGN_BASE_UUID(UUIDList[UUIDIndex]);
                                          ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(UUIDList[UUIDIndex], ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex].SDP_Service_Attribute_Value_Data[SDPAttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_16);
                                          UUIDIndex++;
                                          break;
                                       default:
                                          /* Any other data type is a   */
                                          /* violation of spec. Ignore  */
                                          /* this record.               */
                                          break;
                                    }
                                 }
                              }

                              /* We processed the only                  */
                              /* ServiceClassIDList attribute in the    */
                              /* record, so skip the rest of the        */
                              /* attributes and continue with the next  */
                              /* record.                                */
                              break;
                           }
                        }
                     }

                     UUIDListLength = UUIDIndex;

                     if((OuterResultArray = Env->NewObjectArray(2, Env->FindClass("[J"), NULL)) != NULL)
                     {
                        if((MSBArray = Env->NewLongArray(UUIDListLength)) != NULL)
                        {
                           if((LSBArray = Env->NewLongArray(UUIDListLength)) != NULL)
                           {
                              Env->SetObjectArrayElement(OuterResultArray, 0, MSBArray);
                              Env->SetObjectArrayElement(OuterResultArray, 1, LSBArray);

                              if((MSBArrayElements = (jlong *)(Env->GetPrimitiveArrayCritical(MSBArray, NULL))) != NULL)
                              {
                                 if((LSBArrayElements = (jlong *)(Env->GetPrimitiveArrayCritical(LSBArray, NULL))) != NULL)
                                 {
                                    for(UUIDIndex = 0; UUIDIndex < UUIDListLength; UUIDIndex++)
                                    {
                                       MSBArrayElements[UUIDIndex] = ((((jlong)(UUIDList[UUIDIndex].UUID_Byte0)  & 0x0FFL) << 56) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte1)  & 0x0FFL) << 48) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte2)  & 0x0FFL) << 40) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte3)  & 0x0FFL) << 32) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte4)  & 0x0FFL) << 24) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte5)  & 0x0FFL) << 16) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte6)  & 0x0FFL) << 8)  |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte7)  & 0x0FFL)));
                                       LSBArrayElements[UUIDIndex] = ((((jlong)(UUIDList[UUIDIndex].UUID_Byte8)  & 0x0FFL) << 56) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte9)  & 0x0FFL) << 48) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte10) & 0x0FFL) << 40) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte11) & 0x0FFL) << 32) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte12) & 0x0FFL) << 24) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte13) & 0x0FFL) << 16) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte14) & 0x0FFL) << 8)  |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte15) & 0x0FFL)));
                                    }

                                    Env->ReleasePrimitiveArrayCritical(LSBArray, LSBArrayElements, 0);
                                 }

                                 Env->ReleasePrimitiveArrayCritical(MSBArray, MSBArrayElements, 0);
                              }

                              Env->DeleteLocalRef(LSBArray);
                           }

                           Env->DeleteLocalRef(MSBArray);
                        }
                     }

                     free(UUIDList);
                  }
                  else
                     OuterResultArray = NULL;

                  DEVM_FreeParsedSDPData(&ParsedSDPData);
               }
               else
                  OuterResultArray = NULL;
            }
            else
            {
               if(TotalServiceDataLength == 0)
               {
                  /* Return a pair of empty lists to indicate that the  */
                  /* query was successful but returned no results.      */
                  if((OuterResultArray = Env->NewObjectArray(2, Env->FindClass("[J"), NULL)) != NULL)
                  {
                     if((MSBArray = Env->NewLongArray(0)) != NULL)
                     {
                        if((LSBArray = Env->NewLongArray(0)) != NULL)
                        {
                           Env->SetObjectArrayElement(OuterResultArray, 0, MSBArray);
                           Env->SetObjectArrayElement(OuterResultArray, 1, LSBArray);

                           Env->DeleteLocalRef(LSBArray);
                        }

                        Env->DeleteLocalRef(MSBArray);
                     }
                  }
               }
               else
                  OuterResultArray = NULL;
            }
         }
         else
            OuterResultArray = NULL;

         free(SDPDataBuffer);
      }
      else
         OuterResultArray = NULL;
   }
   else
      OuterResultArray = NULL;

   return OuterResultArray;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_DEVM
 * Method:    querySupportedLowEnergyServicesNative
 * Signature: (BBBBBB)[[J
 */
static jobjectArray QuerySupportedLowEnergyServicesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
#if BTPM_CONFIGURATION_DEVICE_MANAGER_SUPPORT_LOW_ENERGY
   int                         Result;
   jlong                      *MSBArrayElements;
   jlong                      *LSBArrayElements;
   Byte_t                     *ServiceDataBuffer;
   BD_ADDR_t                   RemoteDeviceAddress;
   UUID_16_t                   TempUUID;
   UUID_128_t                 *UUIDList;
   jlongArray                  MSBArray;
   jlongArray                  LSBArray;
   jobjectArray                OuterResultArray;
   unsigned int                UUIDIndex;
   unsigned int                UUIDListLength;
   unsigned int                ServiceResponseIndex;
   unsigned int                TotalServiceDataLength;
   DEVM_Parsed_Services_Data_t ParsedServiceData;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY, 0, NULL, &TotalServiceDataLength)) >= 0)
   {
      if((ServiceDataBuffer = (Byte_t *)malloc(TotalServiceDataLength)) != NULL)
      {
         if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY, TotalServiceDataLength, ServiceDataBuffer, NULL)) >= 0)
         {
            TotalServiceDataLength = Result;

            if(TotalServiceDataLength > 0)
            {
               if((Result = DEVM_ConvertRawServicesStreamToParsedServicesData(TotalServiceDataLength, ServiceDataBuffer, &ParsedServiceData)) == 0)
               {
                  if((UUIDList = (UUID_128_t *)malloc(ParsedServiceData.NumberServices * UUID_128_SIZE)) != NULL)
                  {
                     memset(UUIDList, 0, (ParsedServiceData.NumberServices * UUID_128_SIZE));
                     UUIDIndex = 0;

                     for(ServiceResponseIndex = 0; ServiceResponseIndex < ParsedServiceData.NumberServices; ServiceResponseIndex++)
                     {
                        switch(ParsedServiceData.GATTServiceDiscoveryIndicationData[ServiceResponseIndex].ServiceInformation.UUID.UUID_Type)
                        {
                           case guUUID_128:
                              CONVERT_BLUETOOTH_UUID_128_TO_SDP_UUID_128(UUIDList[UUIDIndex], ParsedServiceData.GATTServiceDiscoveryIndicationData[ServiceResponseIndex].ServiceInformation.UUID.UUID.UUID_128);
                              UUIDIndex++;
                              break;
                           case guUUID_16:
                              CONVERT_BLUETOOTH_UUID_16_TO_SDP_UUID_16(TempUUID, ParsedServiceData.GATTServiceDiscoveryIndicationData[ServiceResponseIndex].ServiceInformation.UUID.UUID.UUID_16);
                              SDP_ASSIGN_BASE_UUID(UUIDList[UUIDIndex]);
                              ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(UUIDList[UUIDIndex], TempUUID);
                              UUIDIndex++;
                              break;
                        }
                     }

                     UUIDListLength = UUIDIndex;

                     if((OuterResultArray = Env->NewObjectArray(2, Env->FindClass("[J"), NULL)) != NULL)
                     {
                        if((MSBArray = Env->NewLongArray(UUIDListLength)) != NULL)
                        {
                           if((LSBArray = Env->NewLongArray(UUIDListLength)) != NULL)
                           {
                              Env->SetObjectArrayElement(OuterResultArray, 0, MSBArray);
                              Env->SetObjectArrayElement(OuterResultArray, 1, LSBArray);

                              if((MSBArrayElements = (jlong *)(Env->GetPrimitiveArrayCritical(MSBArray, NULL))) != NULL)
                              {
                                 if((LSBArrayElements = (jlong *)(Env->GetPrimitiveArrayCritical(LSBArray, NULL))) != NULL)
                                 {
                                    for(UUIDIndex = 0; UUIDIndex < UUIDListLength; UUIDIndex++)
                                    {
                                       MSBArrayElements[UUIDIndex] = ((((jlong)(UUIDList[UUIDIndex].UUID_Byte0)  & 0x0FFL) << 56) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte1)  & 0x0FFL) << 48) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte2)  & 0x0FFL) << 40) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte3)  & 0x0FFL) << 32) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte4)  & 0x0FFL) << 24) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte5)  & 0x0FFL) << 16) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte6)  & 0x0FFL) << 8)  |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte7)  & 0x0FFL)));
                                       LSBArrayElements[UUIDIndex] = ((((jlong)(UUIDList[UUIDIndex].UUID_Byte8)  & 0x0FFL) << 56) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte9)  & 0x0FFL) << 48) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte10) & 0x0FFL) << 40) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte11) & 0x0FFL) << 32) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte12) & 0x0FFL) << 24) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte13) & 0x0FFL) << 16) |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte14) & 0x0FFL) << 8)  |
                                                                      (((jlong)(UUIDList[UUIDIndex].UUID_Byte15) & 0x0FFL)));
                                    }

                                    Env->ReleasePrimitiveArrayCritical(LSBArray, LSBArrayElements, 0);
                                 }

                                 Env->ReleasePrimitiveArrayCritical(MSBArray, MSBArrayElements, 0);
                              }

                              Env->DeleteLocalRef(LSBArray);
                           }

                           Env->DeleteLocalRef(MSBArray);
                        }
                     }

                     free(UUIDList);
                  }
                  else
                     OuterResultArray = NULL;

                  DEVM_FreeParsedServicesData(&ParsedServiceData);
               }
               else
                  OuterResultArray = NULL;
            }
            else
            {
               /* Return a pair of empty lists to indicate that the  */
               /* query was successful but returned no results.      */
               if((OuterResultArray = Env->NewObjectArray(2, Env->FindClass("[J"), NULL)) != NULL)
               {
                  if((MSBArray = Env->NewLongArray(0)) != NULL)
                  {
                     if((LSBArray = Env->NewLongArray(0)) != NULL)
                     {
                        Env->SetObjectArrayElement(OuterResultArray, 0, MSBArray);
                        Env->SetObjectArrayElement(OuterResultArray, 1, LSBArray);

                        Env->DeleteLocalRef(LSBArray);
                     }

                     Env->DeleteLocalRef(MSBArray);
                  }
               }
            }
         }
         else
            OuterResultArray = NULL;

         free(ServiceDataBuffer);
      }
      else
         OuterResultArray = NULL;
   }
   else
      OuterResultArray = NULL;

   return OuterResultArray;
#else
   return NULL;
#endif
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                                 "()V",                                                      (void *) InitClassNative},
   {"convertManufacturerNameToStringNative",           "(I)Ljava/lang/String;",                                    (void *) ConvertManufacturerNameToStringNative},
   {"initObjectNative",                                "(Z)V",                                                     (void *) InitObjectNative},
   {"cleanupObjectNative",                             "()V",                                                      (void *) CleanupObjectNative},
   {"acquireLockNative",                               "()Z",                                                      (void *) AcquireLockNative},
   {"releaseLockNative",                               "()V",                                                      (void *) ReleaseLockNative},
   {"powerOnDeviceNative",                             "()I",                                                      (void *) PowerOnDeviceNative},
   {"powerOffDeviceNative",                            "()I",                                                      (void *) PowerOffDeviceNative},
   {"queryDevicePowerStateNative",                     "()I",                                                      (void *) QueryDevicePowerStateNative},
   {"acknowledgeDevicePoweringDownNative",             "()V",                                                      (void *) AcknowledgeDevicePoweringDownNative},
   {"queryLocalDevicePropertiesNative",                "([B[Ljava/lang/String;[I)I",                               (void *) QueryLocalDevicePropertiesNative},
   {"updateClassOfDeviceNative",                       "(I)I",                                                     (void *) UpdateClassOfDeviceNative},
   {"updateDeviceNameNative",                          "(Ljava/lang/String;)I",                                    (void *) UpdateDeviceNameNative},
   {"updateDiscoverableModeNative",                    "(ZI)I",                                                    (void *) UpdateDiscoverableModeNative},
   {"updateConnectableModeNative",                     "(ZI)I",                                                    (void *) UpdateConnectableModeNative},
   {"updatePairableModeNative",                        "(ZI)I",                                                    (void *) UpdatePairableModeNative},
   {"queryLocalDeviceIDInformationNative",             "([I)I",                                                    (void *) QueryLocalDeviceIDInformationNative},
   {"enableLocalDeviceFeatureNative",                  "(I)I",                                                     (void *) EnableLocalDeviceFeatureNative},
   {"disableLocalDeviceFeatureNative",                 "(I)I",                                                     (void *) DisableLocalDeviceFeatureNative},
   {"queryActiveLocalDeviceFeatureNative",             "([I)I",                                                    (void *) QueryActiveLocalDeviceFeatureNative},
   {"startDeviceDiscoveryNative",                      "(I)I",                                                     (void *) StartDeviceDiscoveryNative},
   {"stopDeviceDiscoveryNative",                       "()I",                                                      (void *) StopDeviceDiscoveryNative},
   {"startLowEnergyDeviceScanNative",                  "(I)I",                                                     (void *) StartLowEnergyDeviceScanNative},
   {"stopLowEnergyDeviceScanNative",                   "()I",                                                      (void *) StopLowEnergyDeviceScanNative},
   {"startLowEnergyAdvertisingNative",                 "(II[I[[B)I",                                               (void *) StartLowEnergyAdvertisingNative},
   {"stopLowEnergyAdvertisingNative",                  "()I",                                                      (void *) StopLowEnergyAdvertisingNative},
   {"queryRemoteDeviceListNative",                     "(II[[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;)I", (void *) QueryRemoteDeviceListNative},
   {"queryRemoteDevicePropertiesNative",               "(BBBBBBZ[Ljava/lang/String;[I[B)I",                        (void *) QueryRemoteDevicePropertiesNative},
   {"queryRemoteLowEnergyDevicePropertiesNative",      "(BBBBBBZ[Ljava/lang/String;[I[B)I",                        (void *) QueryRemoteLowEnergyDevicePropertiesNative},
   {"queryRemoteDeviceEIRDataNative",                  "(BBBBBB[[I[[[B)I",                                         (void *) QueryRemoteDeviceEIRDataNative},
   {"queryRemoteLowEnergyDeviceAdvertisingDataNative", "(BBBBBB[[I[[[BZ)I",                                        (void *) QueryRemoteLowEnergyDeviceAdvertisingDataNative},
   {"queryRemoteDeviceServicesNative",                 "(BBBBBBZ[[B)I",                                            (void *) QueryRemoteDeviceServicesNative},
   {"queryRemoteLowEnergyDeviceServicesNative",        "(BBBBBBZ[[B)I",                                            (void *) QueryRemoteLowEnergyDeviceServicesNative},
   {"addRemoteDeviceNative",                           "(BBBBBBI)I",                                               (void *) AddRemoteDeviceNative},
   {"addRemoteDeviceNative",                           "(BBBBBBILjava/lang/String;I)I",                            (void *) AddRemoteDeviceNativeWithApplicationData},
   {"deleteRemoteDeviceNative",                        "(BBBBBB)I",                                                (void *) DeleteRemoteDeviceNative},
   {"updateRemoteDeviceApplicationDataNative",         "(BBBBBBLjava/lang/String;I)I",                             (void *) UpdateRemoteDeviceApplicationDataNative},
   {"deleteRemoteDevicesNative",                       "(I)I",                                                     (void *) DeleteRemoteDevicesNative},
   {"pairWithRemoteDeviceNative",                      "(BBBBBBZ)I",                                               (void *) PairWithRemoteDeviceNative},
   {"pairWithRemoteLowEnergyDeviceNative",             "(BBBBBBZZ)I",                                              (void *) PairWithRemoteLowEnergyDeviceNative},
   {"cancelPairWithRemoteDeviceNative",                "(BBBBBB)I",                                                (void *) CancelPairWithRemoteDeviceNative},
   {"unPairRemoteDeviceNative",                        "(BBBBBB)I",                                                (void *) UnPairRemoteDeviceNative},
   {"unPairRemoteLowEnergyDeviceNative",               "(BBBBBB)I",                                                (void *) UnPairRemoteLowEnergyDeviceNative},
   {"authenticateRemoteDeviceNative",                  "(BBBBBB)I",                                                (void *) AuthenticateRemoteDeviceNative},
   {"authenticateRemoteLowEnergyDeviceNative",         "(BBBBBB)I",                                                (void *) AuthenticateRemoteLowEnergyDeviceNative},
   {"encryptRemoteDeviceNative",                       "(BBBBBB)I",                                                (void *) EncryptRemoteDeviceNative},
   {"encryptRemoteLowEnergyDeviceNative",              "(BBBBBB)I",                                                (void *) EncryptRemoteLowEnergyDeviceNative},
   {"connectWithRemoteDeviceNative",                   "(BBBBBBI)I",                                               (void *) ConnectWithRemoteDeviceNative},
   {"connectWithRemoteLowEnergyDeviceNative",          "(BBBBBBI)I",                                               (void *) ConnectWithRemoteLowEnergyDeviceNative},
   {"disconnectRemoteDeviceNative",                    "(BBBBBBZ)I",                                               (void *) DisconnectRemoteDeviceNative},
   {"disconnectRemoteLowEnergyDeviceNative",           "(BBBBBBZ)I",                                               (void *) DisconnectRemoteLowEnergyDeviceNative},
   {"setRemoteDeviceLinkActiveNative",                 "(BBBBBB)I",                                                (void *) SetRemoteDeviceLinkActiveNative},
   {"sendPinCodeNative",                               "(BBBBBB[B)I",                                              (void *) SendPinCodeNative},
   {"sendUserConfirmationNative",                      "(BBBBBBZ)I",                                               (void *) SendUserConfirmationNative},
   {"sendPasskeyNative",                               "(BBBBBBI)I",                                               (void *) SendPasskeyNative},
   {"sendOutOfBandDataNative",                         "(BBBBBB[B[B)I",                                            (void *) SendOutOfBandDataNative},
   {"sendIOCapabilitiesNative",                        "(BBBBBBIZZI)I",                                            (void *) SendIOCapabilitiesNative},
   {"sendLowEnergyUserConfirmationNative",             "(BBBBBBZ)I",                                               (void *) SendLowEnergyUserConfirmationNative},
   {"sendLowEnergyPasskeyNative",                      "(BBBBBBI)I",                                               (void *) SendLowEnergyPasskeyNative},
   {"sendLowEnergyOutOfBandDataNative",                "(BBBBBB[B)I",                                              (void *) SendLowEnergyOutOfBandDataNative},
   {"sendLowEnergyIOCapabilitiesNative",               "(BBBBBBIZZI)I",                                            (void *) SendLowEnergyIOCapabilitiesNative},
   {"querySupportedServicesNative",                    "(BBBBBB)[[J",                                              (void *) QuerySupportedServicesNative},
   {"querySupportedLowEnergyServicesNative",           "(BBBBBB)[[J",                                              (void *) QuerySupportedLowEnergyServicesNative},
};

int register_com_stonestreetone_bluetopiapm_DEVM(JNIEnv *Env)
{
   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/DEVM";

   Result = -1;

   PRINT_DEBUG("Registering DEVM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}
