/*****< com_stonestreetone_bluetopiapm_HDSM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_HDSM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager HDSM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Matt Seabold                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   08/16/13  M. Seabold     Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>

extern "C" {
#include "SS1BTHDSM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_HDSM.h"

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
static WEAK_REF Class_HDSM;

static jfieldID Field_HDSM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_HDSM_incomingConnectionRequestEvent;
static jmethodID Method_HDSM_connectedEvent;
static jmethodID Method_HDSM_disconnectedEvent;
static jmethodID Method_HDSM_connectionStatusEvent;
static jmethodID Method_HDSM_audioConnectedEvent;
static jmethodID Method_HDSM_audioDisconnectedEvent;
static jmethodID Method_HDSM_audioConnectionStatusEvent;
static jmethodID Method_HDSM_audioDataEvent;
static jmethodID Method_HDSM_speakerGainIndicationEvent;
static jmethodID Method_HDSM_microphoneGainIndicationEvent;
static jmethodID Method_HDSM_ringIndicationEvent;
static jmethodID Method_HDSM_buttonPressedIndicationEvent;


typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
   unsigned int DataCallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_HDSM_localData, Exclusive)) == NULL)
      PRINT_ERROR("HDSM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_HDSM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_HDSM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void HDSM_EventCallback(HDSM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         BitMask;
   JNIEnv      *Env;
   jobject      HDSMObject;
   BD_ADDR_t   *RemoteDevice;
   jbyteArray   ByteArray;
   jbyte       *ByteArrayData;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our HDSM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((HDSMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, HDSMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        /* Common Headset/Audio Gateway events.         */
                        case hetHDSIncomingConnectionRequest:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_incomingConnectionRequestEvent,
                                 EventData->EventData.IncomingConnectionRequestEventData.ConnectionType,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHDSConnected:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_connectedEvent,
                                 EventData->EventData.ConnectedEventData.ConnectionType,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHDSDisconnected:
                           switch(EventData->EventData.DisconnectedEventData.DisconnectReason)
                           {
                              case HDSM_DEVICE_DISCONNECT_REASON_NORMAL_DISCONNECT:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_DISCONNECTION_STATUS_SUCCESS;
                                 break;
                              case HDSM_DEVICE_DISCONNECT_REASON_SERVICE_LEVEL_ERROR:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_DISCONNECTION_STATUS_SERVICE_LEVEL_CONNECTION_ERROR;
                                 break;
                           }

                           Env->CallVoidMethod(HDSMObject, Method_HDSM_disconnectedEvent,
                                 EventData->EventData.DisconnectedEventData.ConnectionType,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask);
                           break;
                        case hetHDSConnectionStatus:
                           switch(EventData->EventData.ConnectionStatusEventData.ConnectionStatus)
                           {
                              case HDSM_DEVICE_CONNECTION_STATUS_SUCCESS:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_CONNECTION_STATUS_SUCCESS;
                                 break;
                              case HDSM_DEVICE_CONNECTION_STATUS_FAILURE_TIMEOUT:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_CONNECTION_STATUS_FAILURE_TIMEOUT;
                                 break;
                              case HDSM_DEVICE_CONNECTION_STATUS_FAILURE_REFUSED:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_CONNECTION_STATUS_FAILURE_REFUSED;
                                 break;
                              case HDSM_DEVICE_CONNECTION_STATUS_FAILURE_SECURITY:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_CONNECTION_STATUS_FAILURE_SECURITY;
                                 break;
                              case HDSM_DEVICE_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF;
                                 break;
                              case HDSM_DEVICE_CONNECTION_STATUS_FAILURE_UNKNOWN:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HDSM_CONNECTION_STATUS_FAILURE_UNKNOWN;
                                 break;
                           }

                           Env->CallVoidMethod(HDSMObject, Method_HDSM_connectionStatusEvent,
                                 EventData->EventData.ConnectionStatusEventData.ConnectionType,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask);
                           break;
                        case hetHDSAudioConnected:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_audioConnectedEvent,
                                 EventData->EventData.AudioConnectedEventData.ConnectionType,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHDSAudioDisconnected:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_audioDisconnectedEvent,
                                 EventData->EventData.AudioDisconnectedEventData.ConnectionType,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHDSAudioConnectionStatus:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_audioConnectionStatusEvent,
                                 EventData->EventData.AudioConnectionStatusEventData.ConnectionType,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.AudioConnectionStatusEventData.Successful == FALSE) ? JNI_FALSE : JNI_TRUE));
                           break;
                        case hetHDSAudioData:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.AudioDataEventData.AudioDataLength)) != 0)
                           {
                              Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.AudioDataEventData.AudioDataLength, (jbyte *)(EventData->EventData.AudioDataEventData.AudioData));
                           }

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(HDSMObject, Method_HDSM_audioDataEvent,
                                    EventData->EventData.AudioDataEventData.ConnectionType,
                                    EventData->EventData.AudioDataEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.AudioDataEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.AudioDataEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.AudioDataEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.AudioDataEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.AudioDataEventData.RemoteDeviceAddress.BD_ADDR0,
                                    ByteArray);
                           }
                           else
                           {
                              Env->ExceptionDescribe();
                              Env->ExceptionClear();
                           }

                           if(ByteArray)
                              Env->DeleteLocalRef(ByteArray);
                           break;
                        case hetHDSSpeakerGainIndication:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_speakerGainIndicationEvent,
                                 EventData->EventData.SpeakerGainIndicationEventData.ConnectionType,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.SpeakerGainIndicationEventData.SpeakerGain);
                           break;
                        case hetHDSMicrophoneGainIndication:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_microphoneGainIndicationEvent,
                                 EventData->EventData.MicrophoneGainIndicationEventData.ConnectionType,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.MicrophoneGainIndicationEventData.MicrophoneGain);
                           break;
                        case hetHDSRingIndication:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_ringIndicationEvent,
                                 com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHDSButtonPressedIndication:
                           Env->CallVoidMethod(HDSMObject, Method_HDSM_buttonPressedIndicationEvent,
                                 com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY,
                                 EventData->EventData.ButtonPressIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ButtonPressIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ButtonPressIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ButtonPressIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ButtonPressIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ButtonPressIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during HDSM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, HDSMObject, LocalData);
                  }

                  Env->DeleteLocalRef(HDSMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the    */
                  /* HDSM java object. This can happen if the object has   */
                  /* been garbage collected or if the VM has run out of    */
                  /* memory. Now, check whether the object was GC'd. Since */
                  /* NewLocalRef doesn't throw exceptions, we can do this  */
                  /* with IsSameObject.                                    */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The HDSM Manager object has been GC'd. Since we are*/
                     /* still receiving events on this registration, the   */
                     /* Manager was not cleaned up properly.               */
                     PRINT_ERROR("HDSM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it  */
                     /* could indicate a leak in this thread context.      */
                     PRINT_ERROR("VM reports 'Out of Memory' in HDSM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The HDSM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: HDSM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: HDSM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_HDSM = NewWeakRef(Env, Clazz)) != 0)
   {
      if((Field_HDSM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_HDSM_incomingConnectionRequestEvent                              = Env->GetMethodID(Clazz, "incomingConnectionRequestEvent",                              "(IBBBBBB)V");
      Method_HDSM_connectedEvent                                              = Env->GetMethodID(Clazz, "connectedEvent",                                              "(IBBBBBB)V");
      Method_HDSM_disconnectedEvent                                           = Env->GetMethodID(Clazz, "disconnectedEvent",                                           "(IBBBBBBI)V");
      Method_HDSM_connectionStatusEvent                                       = Env->GetMethodID(Clazz, "connectionStatusEvent",                                       "(IBBBBBBI)V");
      Method_HDSM_audioConnectedEvent                                         = Env->GetMethodID(Clazz, "audioConnectedEvent",                                         "(IBBBBBB)V");
      Method_HDSM_audioDisconnectedEvent                                      = Env->GetMethodID(Clazz, "audioDisconnectedEvent",                                      "(IBBBBBB)V");
      Method_HDSM_audioConnectionStatusEvent                                  = Env->GetMethodID(Clazz, "audioConnectionStatusEvent",                                  "(IBBBBBBZ)V");
      Method_HDSM_audioDataEvent                                              = Env->GetMethodID(Clazz, "audioDataEvent",                                              "(IBBBBBB[B)V");
      Method_HDSM_speakerGainIndicationEvent                                  = Env->GetMethodID(Clazz, "speakerGainIndicationEvent",                                  "(IBBBBBBI)V");
      Method_HDSM_microphoneGainIndicationEvent                               = Env->GetMethodID(Clazz, "microphoneGainIndicationEvent",                               "(IBBBBBBI)V");
      Method_HDSM_ringIndicationEvent                                         = Env->GetMethodID(Clazz, "ringIndicationEvent",                                         "(IBBBBBB)V");
      Method_HDSM_buttonPressedIndicationEvent                                = Env->GetMethodID(Clazz, "buttonPressedIndicationEvent",                                "(IBBBBBB)V");
   }
   else
      PRINT_ERROR("HDSM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.HDSM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    initObjectNative
 * Signature: (IZ)V
 */
static void InitObjectNative(JNIEnv *Env, jobject Object, jint ServerType, jboolean Controller)
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
            switch(ServerType)
            {
               case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
                  RegistrationResult = HDSM_Register_Event_Callback(sctHeadset, ((Controller == JNI_FALSE) ? FALSE : TRUE), HDSM_EventCallback, LocalData->WeakObjectReference);
                  break;
               case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
               default:
                  RegistrationResult = HDSM_Register_Event_Callback(sctAudioGateway, ((Controller == JNI_FALSE) ? FALSE : TRUE), HDSM_EventCallback, LocalData->WeakObjectReference);
                  break;
            }

            if(RegistrationResult > 0)
            {
               LocalData->DataCallbackID = 0;
               LocalData->CallbackID     = RegistrationResult;
               SetLocalData(Env, Object, LocalData);
            }
            else
            {
                PRINT_ERROR("HDSM: Unable to register callback for events (%d)", RegistrationResult);

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
            PRINT_ERROR("HDSM: Out of Memory: Unable to obtain weak reference to object");
            
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
 * Class:     com_stonestreetone_bluetopiapm_HDSM
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

      if(LocalData->DataCallbackID)
         HDSM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

      if(LocalData->CallbackID)
         HDSM_Un_Register_Event_Callback(LocalData->CallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    connectionRequestResponseNative
 * Signature: (IBBBBBBZ)I
 */
static jint ConnectionRequestResponseNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean AcceptConnection)
{
   jint                   Result;
   BD_ADDR_t              RemoteDeviceAddress;
   HDSM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return HDSM_Connection_Request_Response(ConnectionType, RemoteDeviceAddress, ((AcceptConnection == JNI_FALSE) ? FALSE : TRUE));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    connectRemoteDeviceNative
 * Signature: (IBBBBBBIIZ)I
 */
static jint ConnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint RemoteServerPort, jint ConnectionFlags, jboolean WaitForConnection)
{
   int                     Result;
   unsigned int            Status;
   unsigned long           Flags;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HDSM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HDSM_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? HDSM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HDSM_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? HDSM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_ENCRYPTION : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HDSM_Connect_Remote_Device(ConnectionType, RemoteDeviceAddress, RemoteServerPort, Flags, HDSM_EventCallback, LocalData->WeakObjectReference, 0);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }
   else
   {
      Result = HDSM_Connect_Remote_Device(ConnectionType, RemoteDeviceAddress, RemoteServerPort, Flags, 0, 0, &Status);

      /* If the request was submitted correctly, use the connection  */
      /* status as the return value.                                 */
      if(Result == 0)
         Result = Status;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    disconnectDeviceNative
 * Signature: (IBBBBBB)I
 */
static jint DisconnectDeviceNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;
   HDSM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   return HDSM_Disconnect_Device(ConnectionType, RemoteDeviceAddress);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    queryConnectedDevicesNative
 * Signature: (I)[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;
 */
static jobjectArray QueryConnectedDevicesNative(JNIEnv *Env, jobject Object, jint ServerType)
{
   int                    Index;
   int                    Result;
   jobject                Address;
   BD_ADDR_t             *AddressBuffer;
   unsigned int           TotalConnected;
   jobjectArray           AddressList;
   HDSM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   /* Determine the number of connected devices.                        */
   if((Result = HDSM_Query_Connected_Devices(ConnectionType, 0, NULL, &TotalConnected)) >= 0)
   {
      /* Allocate a Java array to hold the device addresses.            */
      if((AddressList = Env->NewObjectArray(TotalConnected, Env->FindClass("com/stonestreetone/bluetopiapm/BluetoothAddress"), NULL)) != NULL)
      {
         /* Make sure there are connected devices before we query the   */
         /* actual list.                                                */
         if(TotalConnected > 0)
         {
            /* Allocate enough storage to accommodate the device list.  */
            if((AddressBuffer = (BD_ADDR_t *)malloc(sizeof(BD_ADDR_t) * TotalConnected)) != NULL)
            {
               /* Get the list of connected devices.                    */
               Result = HDSM_Query_Connected_Devices(ConnectionType, TotalConnected, AddressBuffer, NULL);

               /* Copy the device addresses into the Java array.        */
               for(Index = 0; Index < Result; Index++)
               {
                  if((Address = NewBluetoothAddress(Env, AddressBuffer[Index])) != NULL)
                  {
                     Env->SetObjectArrayElement(AddressList, Index, Address);
                     Env->DeleteLocalRef(Address);
                  }
                  else
                     break;
               }

               /* Free the storage we allocated previously.             */
               free(AddressBuffer);

               /* Check whether we copied all the addresses correctly.  */
               /* If not, throw an error.                               */
               if(Index != Result)
                  Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
            }
            else
            {
               /* We couldn't allocate storage for the list. Throw an   */
               /* error.                                                */
               Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
            }
         }
         else
         {
            /* There are no connected devices. Return a valid but empty */
            /* java array to indicate this.                             */
         }
      }
      else
      {
         /* We coudn't allocate the Java array. NewObjectArray() will   */
         /* have already thrown an error for this, so we don't need to  */
         /* do anything.                                                */
      }
   }
   else
   {
      /* We received an error when querying the number of connected     */
      /* devices. Return NULL to indicate this.                         */
      AddressList = NULL;
   }

   return AddressList;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    queryCurrentConfigurationNative
 * Signature: (I[I)I
 */
static jint QueryCurrentConfigurationNative(JNIEnv *Env, jobject Object, jint ServerType, jintArray Flags)
{
   int                          Result;
   jint                         IncomingConnectionFlags;
   jint                         SupportedFeaturesMask;
   jint                         ConfigurationFlags[2];
   HDSM_Connection_Type_t       ConnectionType;
   HDSM_Current_Configuration_t CurrentConfiguration;

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   if(!(Result = HDSM_Query_Current_Configuration(ConnectionType, &CurrentConfiguration)))
   {
      IncomingConnectionFlags  = 0;
      IncomingConnectionFlags |= (CurrentConfiguration.IncomingConnectionFlags & HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)?com_stonestreetone_bluetopiapm_HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION:0;
      IncomingConnectionFlags |= (CurrentConfiguration.IncomingConnectionFlags & HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION)?com_stonestreetone_bluetopiapm_HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION:0;
      IncomingConnectionFlags |= (CurrentConfiguration.IncomingConnectionFlags & HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)?com_stonestreetone_bluetopiapm_HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION:0;


      SupportedFeaturesMask  = 0;

      switch(ConnectionType)
      {
         case sctHeadset:
            SupportedFeaturesMask |= (CurrentConfiguration.SupportedFeaturesMask & HDSM_SUPPORTED_FEATURES_MASK_AUDIO_GATEWAY_SUPPORTS_IN_BAND_RING)?com_stonestreetone_bluetopiapm_HDSM_HeadsetServerManager_SUPPORTED_FEATURE_REMOTE_AUDIO_VOLUME_CONTROLS:0;
            break;
         case sctAudioGateway:
            SupportedFeaturesMask |= (CurrentConfiguration.SupportedFeaturesMask & HDSM_SUPPORTED_FEATURES_MASK_HEADSET_SUPPORTS_REMOTE_AUDIO_VOLUME_CONTROLS)?com_stonestreetone_bluetopiapm_HDSM_AudioGatewayServerManager_SUPPORTED_FEATURE_IN_BAND_RING:0;
      }

      ConfigurationFlags[com_stonestreetone_bluetopiapm_HDSM_CONFIGURATION_INCOMING_CONNECTION_FLAGS] = IncomingConnectionFlags;
      ConfigurationFlags[com_stonestreetone_bluetopiapm_HDSM_CONFIGURATION_SUPPORTED_FEATURES_MASK]   = SupportedFeaturesMask;

      Env->SetIntArrayRegion(Flags,0,2,ConfigurationFlags);
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    changeIncomingConnectionFlagsNative
 * Signature: (II)I
 */
static jint ChangeIncomingConnectionFlagsNative(JNIEnv *Env, jobject Object, jint ServerType, jint ConnectionFlags)
{
   unsigned long          Flags;
   HDSM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? HDSM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);

   return HDSM_Change_Incoming_Connection_Flags(ConnectionType, Flags);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    setRemoteSpeakerGainNative
 * Signature: (IBBBBBBI)I
 */
static jint SetRemoteSpeakerGainNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint SpeakerGain)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HDSM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HDSM_Set_Remote_Speaker_Gain(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress, SpeakerGain);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    setRemoteMicrophoneGainNative
 * Signature: (IBBBBBBI)I
 */
static jint SetRemoteMicrophoneGainNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint MicrophoneGain)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HDSM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HDSM_Set_Remote_Microphone_Gain(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress, MicrophoneGain);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    registerDataEventCallbackNative
 * Signature: (I)I
 */
static jint RegisterDataEventCallbackNative(JNIEnv *Env, jobject Object, jint ServerType)
{
   jint                    Result;
   LocalData_t            *LocalData;
   HDSM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, TRUE)) != NULL)
   {
      if((Result = HDSM_Register_Data_Event_Callback(ConnectionType, HDSM_EventCallback, LocalData->WeakObjectReference)) > 0)
         LocalData->DataCallbackID = (unsigned int)Result;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    unregisterDataEventCallbackNative
 * Signature: ()V
 */
static void UnregisterDataEventCallbackNative(JNIEnv *Env, jobject Object)
{
   LocalData_t *LocalData = 0;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, TRUE)) != NULL)
   {
      if(LocalData->DataCallbackID > 0)
      {
         HDSM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

         LocalData->DataCallbackID = 0;
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    setupAudioConnectionNative
 * Signature: (IBBBBBBZ)I
 */
static jint SetupAudioConnectionNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean InBandRinging)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HDSM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HDSM_Setup_Audio_Connection(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress, InBandRinging);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    releaseAudioConnectionNative
 * Signature: (IBBBBBB)I
 */
static jint ReleaseAudioConnectionNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HDSM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HDSM_Release_Audio_Connection(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    sendAudioDataNative
 * Signature: (IBBBBBB[B)I
 */
static jint SendAudioDataNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyteArray AudioData)
{
   jint                    Result;
   jbyte                  *AudioDataBytes;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HDSM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_HEADSET:
         ConnectionType = sctHeadset;
         break;
      case com_stonestreetone_bluetopiapm_HDSM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = sctAudioGateway;
         break;
   }

   if((AudioDataBytes = Env->GetByteArrayElements(AudioData, NULL)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HDSM_Send_Audio_Data(LocalData->DataCallbackID, ConnectionType, RemoteDeviceAddress, Env->GetArrayLength(AudioData), (unsigned char *)AudioDataBytes);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;

      Env->ReleaseByteArrayElements(AudioData, AudioDataBytes, JNI_ABORT);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    sendButtonPressNative
 * Signature: (BBBBBB)I
 */
static jint SendButtonPressNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   LocalData_t *LocalData;
   BD_ADDR_t    RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HDSM_Send_Button_Press(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HDSM
 * Method:    ringIndicationNative
 * Signature: (BBBBBB)I
 */
static jint RingIndicationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   LocalData_t *LocalData;
   BD_ADDR_t    RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HDSM_Ring_Indication(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                                      "()V",                                                   (void*) InitClassNative},
   {"initObjectNative",                                     "(IZ)V",                                                 (void*) InitObjectNative},
   {"cleanupObjectNative",                                  "()V",                                                   (void*) CleanupObjectNative},
   {"connectionRequestResponseNative",                      "(IBBBBBBZ)I",                                           (void*) ConnectionRequestResponseNative},
   {"connectRemoteDeviceNative",                            "(IBBBBBBIIZ)I",                                         (void*) ConnectRemoteDeviceNative},
   {"disconnectDeviceNative",                               "(IBBBBBB)I",                                            (void*) DisconnectDeviceNative},
   {"queryConnectedDevicesNative",                          "(I)[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;", (void*) QueryConnectedDevicesNative},
   {"queryCurrentConfigurationNative",                      "(I[I)I",                                                (void*) QueryCurrentConfigurationNative},
   {"changeIncomingConnectionFlagsNative",                  "(II)I",                                                 (void*) ChangeIncomingConnectionFlagsNative},
   {"setRemoteSpeakerGainNative",                           "(IBBBBBBI)I",                                           (void*) SetRemoteSpeakerGainNative},
   {"setRemoteMicrophoneGainNative",                        "(IBBBBBBI)I",                                           (void*) SetRemoteMicrophoneGainNative},
   {"registerDataEventCallbackNative",                      "(I)I",                                                  (void*) RegisterDataEventCallbackNative},
   {"unregisterDataEventCallbackNative",                    "()V",                                                   (void*) UnregisterDataEventCallbackNative},
   {"setupAudioConnectionNative",                           "(IBBBBBBZ)I",                                           (void*) SetupAudioConnectionNative},
   {"releaseAudioConnectionNative",                         "(IBBBBBB)I",                                            (void*) ReleaseAudioConnectionNative},
   {"sendAudioDataNative",                                  "(IBBBBBB[B)I",                                          (void*) SendAudioDataNative},
   {"sendButtonPressNative",                                "(BBBBBB)I",                                             (void*) SendButtonPressNative},
   {"ringIndicationNative",                                 "(BBBBBB)I",                                             (void*) RingIndicationNative},
};

int register_com_stonestreetone_bluetopiapm_HDSM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/HDSM";

   Result = -1;

   PRINT_DEBUG("Registering HDSM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}
