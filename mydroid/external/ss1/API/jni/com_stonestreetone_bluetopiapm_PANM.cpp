/*****< com_stonestreetone_bluetopiapm_PANM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_PANM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager PANM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   12/16/11  G. Hensley     Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>

extern "C" {
#include "SS1BTPANM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_PANM.h"

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
static WEAK_REF Class_PANM;

static jfieldID Field_PANM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_PANM_incomingConnectionRequestEvent;
static jmethodID Method_PANM_connectedEvent;
static jmethodID Method_PANM_disconnectedEvent;
static jmethodID Method_PANM_connectionStatusEvent;

typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_PANM_localData, Exclusive)) == NULL)
      PRINT_ERROR("PANM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_PANM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_PANM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void PANM_EventCallback(PANM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         IntVal1;
   jint         IntVal2;
   jint         IntVal3;
   JNIEnv      *Env;
   jobject      PANMObject;
   jstring      String;
   BD_ADDR_t   *RemoteDevice;
   jbyteArray   ByteArray;
   jbyte       *ByteArrayData;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our PANM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((PANMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, PANMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case petPANMIncomingConnectionRequest:
                           Env->CallVoidMethod(PANMObject, Method_PANM_incomingConnectionRequestEvent,
                                 EventData->EventData.IncomingConnectionReqeustEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.IncomingConnectionReqeustEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.IncomingConnectionReqeustEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.IncomingConnectionReqeustEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.IncomingConnectionReqeustEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.IncomingConnectionReqeustEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case petPANMConnected:
                           switch(EventData->EventData.ConnectedEventData.ServiceType)
                           {
                              case pstPersonalAreaNetworkUser:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER;
                                 break;
                              case pstNetworkAccessPoint:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_NETWORK_ACCESS_POINT;
                                 break;
                              case pstGroupAdhocNetwork:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_GROUP_ADHOC_NETWORK;
                                 break;
                           }

                           Env->CallVoidMethod(PANMObject, Method_PANM_connectedEvent,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1);
                           break;
                        case petPANMDisconnected:
                           switch(EventData->EventData.DisconnectedEventData.ServiceType)
                           {
                              case pstPersonalAreaNetworkUser:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER;
                                 break;
                              case pstNetworkAccessPoint:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_NETWORK_ACCESS_POINT;
                                 break;
                              case pstGroupAdhocNetwork:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_GROUP_ADHOC_NETWORK;
                                 break;
                           }

                           Env->CallVoidMethod(PANMObject, Method_PANM_disconnectedEvent,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1);
                           break;
                        case petPANMConnectionStatus:
                           switch(EventData->EventData.ConnectionStatusEventData.ServiceType)
                           {
                              case pstPersonalAreaNetworkUser:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER;
                                 break;
                              case pstNetworkAccessPoint:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_NETWORK_ACCESS_POINT;
                                 break;
                              case pstGroupAdhocNetwork:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_GROUP_ADHOC_NETWORK;
                                 break;
                           }

                           Env->CallVoidMethod(PANMObject, Method_PANM_connectionStatusEvent,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1,
                                 EventData->EventData.ConnectionStatusEventData.Status);
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during PANM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, PANMObject, LocalData);
                  }

                  Env->DeleteLocalRef(PANMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the PANM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The PANM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("PANM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in PANM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The PANM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: PANM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: PANM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_PANM = (jclass)(NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_PANM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_PANM_incomingConnectionRequestEvent = Env->GetMethodID(Clazz, "incomingConnectionRequestEvent", "(BBBBBB)V");
      Method_PANM_connectedEvent                 = Env->GetMethodID(Clazz, "connectedEvent",                 "(BBBBBBI)V");
      Method_PANM_disconnectedEvent              = Env->GetMethodID(Clazz, "disconnectedEvent",              "(BBBBBBI)V");
      Method_PANM_connectionStatusEvent          = Env->GetMethodID(Clazz, "connectionStatusEvent",          "(BBBBBBII)V");
   }
   else
      PRINT_ERROR("PANM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.PANM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    initObjectNative
 * Signature: ()V
 */
static void InitObjectNative(JNIEnv *Env, jobject Object)
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
            if((RegistrationResult = PANM_Register_Event_Callback(PANM_EventCallback, LocalData->WeakObjectReference)) > 0)
            {
               LocalData->CallbackID = RegistrationResult;
               SetLocalData(Env, Object, LocalData);
            }
            else
            {
               PRINT_ERROR("PANM: Unable to register callback for events (%d)", RegistrationResult);

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
            PRINT_ERROR("PANM: Out of Memory: Unable to obtain weak reference to object");

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
 * Class:     com_stonestreetone_bluetopiapm_PANM
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
         PANM_Un_Register_Event_Callback(LocalData->CallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    connectionRequestResponseNative
 * Signature: (BBBBBBZ)I
 */
static jint ConnectionRequestResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean AcceptConnection)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = PANM_Connection_Request_Response(RemoteDeviceAddress, ((AcceptConnection == JNI_FALSE) ? FALSE : TRUE));

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    connectRemoteDeviceNative
 * Signature: (BBBBBBIIIZ)I
 */
static jint ConnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint LocalServiceType, jint RemoteServiceType, jint ConnectionFlags, jboolean WaitForConnection)
{
   int                Type;
   int                Flags;
   jint               Result;
   BD_ADDR_t          RemoteDeviceAddress;
   LocalData_t       *LocalData;
   unsigned int       ConnectionStatus;
   PAN_Service_Type_t LocalType;
   PAN_Service_Type_t RemoteType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(LocalServiceType)
   {
      case com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER:
         LocalType = pstPersonalAreaNetworkUser;
         break;
      case com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_NETWORK_ACCESS_POINT:
         LocalType = pstNetworkAccessPoint;
         break;
      case com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_GROUP_ADHOC_NETWORK:
      default:
         LocalType = pstGroupAdhocNetwork;
         break;
   }

   switch(RemoteServiceType)
   {
      case com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER:
         RemoteType = pstPersonalAreaNetworkUser;
         break;
      case com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_NETWORK_ACCESS_POINT:
         RemoteType = pstNetworkAccessPoint;
         break;
      case com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_GROUP_ADHOC_NETWORK:
      default:
         RemoteType = pstGroupAdhocNetwork;
         break;
   }

   Flags  = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_PANM_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? PANM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_PANM_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? PANM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_ENCRYPTION     : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = PANM_Connect_Remote_Device(RemoteDeviceAddress, LocalType, RemoteType, Flags, PANM_EventCallback, LocalData->WeakObjectReference, NULL);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }
   else
   {
      Result = PANM_Connect_Remote_Device(RemoteDeviceAddress, LocalType, RemoteType, Flags, NULL, NULL, &ConnectionStatus);

      if(Result == 0)
         Result = ConnectionStatus;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;

}

/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    disconnectRemoteDeviceNative
 * Signature: (BBBBBB)I
 */
static jint DisconnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = PANM_Close_Connection(RemoteDeviceAddress);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}
/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    queryConnectedDevicesNative
 * Signature: ([[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;)I
 */
static jint QueryConnectedDevicesNative(JNIEnv *Env, jobject Object, jobjectArray RemoteDeviceAddressList)
{
   jint         Result;
   jobject      Address;
   BD_ADDR_t   *BD_ADDRList;
   unsigned int Index;
   unsigned int TotalConnected;
   jobjectArray AddressList;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Result = PANM_Query_Connected_Devices(0, NULL, &TotalConnected)) == 0)
   {
      if(TotalConnected > 0)
      {
         if((BD_ADDRList = (BD_ADDR_t *)BTPS_AllocateMemory(TotalConnected * sizeof(BD_ADDR_t))) != NULL)
         {
            if((Result = PANM_Query_Connected_Devices(TotalConnected, BD_ADDRList, NULL)) >= 0)
            {
               /* 'Result' now contains the actual number of connected  */
               /* devices. Create a Java Array of exactly this size in  */
               /* which to return the results so the array length       */
               /* implies the number of valid results.                  */
               if((AddressList = Env->NewObjectArray(Result, Env->FindClass("com/stonestreetone/bluetopiapm/BluetoothAddress"), NULL)) != NULL)
               {
                  for(Index = 0; Index < Result; Index++)
                  {
                     if((Address = NewBluetoothAddress(Env, BD_ADDRList[Index])) != NULL)
                     {
                        Env->SetObjectArrayElement(AddressList, Index, Address);
                        Env->DeleteLocalRef(Address);
                     }
                     else
                     {
                        Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                        break;
                     }
                  }

                  Env->SetObjectArrayElement(RemoteDeviceAddressList, 0, AddressList);
               }
            }

            BTPS_FreeMemory(BD_ADDRList);
         }
         else
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
      else
      {
         if(TotalConnected == 0)
         {
            /* The query was successful, but no devices are connected,  */
            /* so we will return an empty array.                        */
            if((AddressList = Env->NewObjectArray(0, Env->FindClass("com/stonestreetone/bluetopiapm/BluetoothAddress"), NULL)) != NULL)
               Env->SetObjectArrayElement(RemoteDeviceAddressList, 0, AddressList);
            else
               Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);

         }
      }
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    queryCurrentConfigurationNative
 * Signature: ([I[I)I
 */
static jint QueryCurrentConfigurationNative(JNIEnv *Env, jobject Object, jintArray ServiceTypeFlags, jintArray IncomingConnectionFlags)
{
   jint                         Result;
   jint                         TypeFlags;
   jint                         ConnectionFlags;
   PANM_Current_Configuration_t Config;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Result = PANM_Query_Current_Configuration(&Config);

   if(Result == 0) {
      TypeFlags  = 0;
      TypeFlags |= ((Config.ServiceTypeFlags & PAN_PERSONAL_AREA_NETWORK_USER_SERVICE) ? com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER : 0);
      TypeFlags |= ((Config.ServiceTypeFlags & PAN_NETWORK_ACCESS_POINT_SERVICE)       ? com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_NETWORK_ACCESS_POINT       : 0);
      TypeFlags |= ((Config.ServiceTypeFlags & PAN_GROUP_ADHOC_NETWORK_SERVICE)        ? com_stonestreetone_bluetopiapm_PANM_SERVICE_TYPE_GROUP_ADHOC_NETWORK        : 0);

      ConnectionFlags  = 0;
      ConnectionFlags |= ((Config.IncomingConnectionFlags & PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? com_stonestreetone_bluetopiapm_PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
      ConnectionFlags |= ((Config.IncomingConnectionFlags & PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? com_stonestreetone_bluetopiapm_PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
      ConnectionFlags |= ((Config.IncomingConnectionFlags & PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? com_stonestreetone_bluetopiapm_PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

      Env->SetIntArrayRegion(ServiceTypeFlags, 0, 1, &TypeFlags);
      Env->SetIntArrayRegion(IncomingConnectionFlags, 0, 1, &ConnectionFlags);
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PANM
 * Method:    changeIncomingConnectionFlagsNative
 * Signature: (I)I
 */
static jint ChangeIncomingConnectionFlagsNative(JNIEnv *Env, jobject Object, jint IncomingConnectionFlags)
{
   jint          Result;
   unsigned long Flags;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Flags  = 0;
   Flags |= ((IncomingConnectionFlags & com_stonestreetone_bluetopiapm_PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
   Flags |= ((IncomingConnectionFlags & com_stonestreetone_bluetopiapm_PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((IncomingConnectionFlags & com_stonestreetone_bluetopiapm_PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? PANM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

   Result = PANM_Change_Incoming_Connection_Flags(Flags);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                     "()V",                                                    (void *) InitClassNative},
   {"initObjectNative",                    "()V",                                                    (void *) InitObjectNative},
   {"cleanupObjectNative",                 "()V",                                                    (void *) CleanupObjectNative},
   {"connectionRequestResponseNative",     "(BBBBBBZ)I",                                             (void *) ConnectionRequestResponseNative},
   {"connectRemoteDeviceNative",           "(BBBBBBIIIZ)I",                                          (void *) ConnectRemoteDeviceNative},
   {"disconnectRemoteDeviceNative",        "(BBBBBB)I",                                              (void *) DisconnectRemoteDeviceNative},
   {"queryConnectedDevicesNative",         "([[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;)I", (void *) QueryConnectedDevicesNative},
   {"queryCurrentConfigurationNative",     "([I[I)I",                                                (void *) QueryCurrentConfigurationNative},
   {"changeIncomingConnectionFlagsNative", "(I)I",                                                   (void *) ChangeIncomingConnectionFlagsNative},
};

int register_com_stonestreetone_bluetopiapm_PANM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/PANM";

   Result = -1;

   PRINT_DEBUG("Registering PANM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}
