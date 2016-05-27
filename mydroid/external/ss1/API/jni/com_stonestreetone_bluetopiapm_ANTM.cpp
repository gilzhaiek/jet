/*****< com_stonestreetone_bluetopiapm_ANTM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_ANTM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager ANTM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Matt Seabold                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/09/13  M. Seabold     Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>
#include <limits.h>

extern "C" {
#include "SS1BTANTM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_ANTM.h"

static WEAK_REF Class_ANTM;

static jfieldID Field_ANTM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_startupMessageEvent;
static jmethodID Method_channelResponseEvent;
static jmethodID Method_channelStatusEvent;
static jmethodID Method_channelIDEvent;
static jmethodID Method_ANTVersionEvent;
static jmethodID Method_capabilitiesEvent;
static jmethodID Method_broadcastDataPacketEvent;
static jmethodID Method_acknowledgedDataPacketEvent;
static jmethodID Method_burstDataPacketEvent;
static jmethodID Method_extendedBroadcastDataPacketEvent;
static jmethodID Method_extendedAcknowledgedDataPacketEvent;
static jmethodID Method_extendedBurstDataPacketEvent;
static jmethodID Method_rawDataPacketEvent;

typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_ANTM_localData, Exclusive)) == NULL)
      PRINT_ERROR("ANTM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_ANTM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_ANTM_localData, LocalData);
}

/*
 * Event Callback Handler.
 */
static void ANTM_EventCallback(ANTM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   JNIEnv      *Env;
   jobject      ANTMObject;
   jbyteArray   ByteArray;
   LocalData_t *LocalData;
   unsigned int Options; 

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our ANTM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((ANTMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, ANTMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case aetANTMStartupMessage:
                           Env->CallVoidMethod(ANTMObject, Method_startupMessageEvent,
                                 EventData->EventData.StartupMessageEventData.StartupMessage);
                           break;
                        case aetANTMChannelResponse:
                           Env->CallVoidMethod(ANTMObject, Method_channelResponseEvent,
                                 EventData->EventData.ChannelResponseEventData.ChannelNumber,
                                 EventData->EventData.ChannelResponseEventData.MessageID,
                                 EventData->EventData.ChannelResponseEventData.MessageCode);
                           break;
                        case aetANTMChannelStatus:
                           Env->CallVoidMethod(ANTMObject, Method_channelStatusEvent,
                                 EventData->EventData.ChannelStatusEventData.ChannelNumber,
                                 EventData->EventData.ChannelStatusEventData.ChannelStatus);
                           break;
                        case aetANTMChannelID:
                           Env->CallVoidMethod(ANTMObject, Method_channelIDEvent,
                                 EventData->EventData.ChannelIDEventData.ChannelNumber,
                                 EventData->EventData.ChannelIDEventData.DeviceNumber,
                                 EventData->EventData.ChannelIDEventData.DeviceTypeID,
                                 EventData->EventData.ChannelIDEventData.TransmissionType);
                           break;
                        case aetANTMANTVersion:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.ANTVersionEventData.VersionDataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.ANTVersionEventData.VersionDataLength), (jbyte *)(EventData->EventData.ANTVersionEventData.VersionData));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(ANTMObject, Method_ANTVersionEvent,
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
                        case aetANTMCapabilities:
                           Options = 0;

                           Options |= (EventData->EventData.CapabilitiesEventData.StandardOptions << 24);
                           Options |= (EventData->EventData.CapabilitiesEventData.AdvancedOptions << 16);
                           Options |= (EventData->EventData.CapabilitiesEventData.AdvancedOptions2 << 8);
                           Options |= (EventData->EventData.CapabilitiesEventData.Reserved);

                           Env->CallVoidMethod(ANTMObject, Method_capabilitiesEvent,
                                 EventData->EventData.CapabilitiesEventData.MaxChannels,
                                 EventData->EventData.CapabilitiesEventData.MaxNetworks,
                                 Options);
                           break;
                        case aetANTMBroadcastDataPacket:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.BroadcastDataPacketEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.BroadcastDataPacketEventData.DataLength), (jbyte *)(EventData->EventData.BroadcastDataPacketEventData.Data));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(ANTMObject, Method_broadcastDataPacketEvent,
                                    (jint)EventData->EventData.BroadcastDataPacketEventData.ChannelNumber,
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
                        case aetANTMAcknowledgedDataPacket:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.AcknowledgedDataPacketEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.AcknowledgedDataPacketEventData.DataLength), (jbyte *)(EventData->EventData.AcknowledgedDataPacketEventData.Data));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(ANTMObject, Method_acknowledgedDataPacketEvent,
                                    (jint)EventData->EventData.AcknowledgedDataPacketEventData.ChannelNumber,
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
                        case aetANTMBurstDataPacket:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.BurstDataPacketEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.BurstDataPacketEventData.DataLength), (jbyte *)(EventData->EventData.BurstDataPacketEventData.Data));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(ANTMObject, Method_burstDataPacketEvent,
                                    (jint)EventData->EventData.BurstDataPacketEventData.SequenceChannelNumber,
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
                        case aetANTMExtendedBroadcastDataPacket:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.ExtendedBroadcastDataPacketEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.ExtendedBroadcastDataPacketEventData.DataLength), (jbyte *)(EventData->EventData.ExtendedBroadcastDataPacketEventData.Data));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(ANTMObject, Method_extendedBroadcastDataPacketEvent,
                                    (jint)EventData->EventData.ExtendedBroadcastDataPacketEventData.ChannelNumber,
                                    (jint)EventData->EventData.ExtendedBroadcastDataPacketEventData.DeviceNumber,
                                    (jint)EventData->EventData.ExtendedBroadcastDataPacketEventData.DeviceType,
                                    (jint)EventData->EventData.ExtendedBroadcastDataPacketEventData.TransmissionType,
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
                        case aetANTMExtendedAcknowledgedDataPacket:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.ExtendedAcknowledgedDataPacketEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.ExtendedAcknowledgedDataPacketEventData.DataLength), (jbyte *)(EventData->EventData.ExtendedAcknowledgedDataPacketEventData.Data));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(ANTMObject, Method_extendedAcknowledgedDataPacketEvent,
                                    (jint)EventData->EventData.ExtendedAcknowledgedDataPacketEventData.ChannelNumber,
                                    (jint)EventData->EventData.ExtendedAcknowledgedDataPacketEventData.DeviceNumber,
                                    (jint)EventData->EventData.ExtendedAcknowledgedDataPacketEventData.DeviceType,
                                    (jint)EventData->EventData.ExtendedAcknowledgedDataPacketEventData.TransmissionType,
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
                        case aetANTMExtendedBurstDataPacket:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.ExtendedBurstDataPacketEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.ExtendedBurstDataPacketEventData.DataLength), (jbyte *)(EventData->EventData.ExtendedBurstDataPacketEventData.Data));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(ANTMObject, Method_extendedBurstDataPacketEvent,
                                    (jint)EventData->EventData.ExtendedBurstDataPacketEventData.SequenceChannelNumber,
                                    (jint)EventData->EventData.ExtendedBurstDataPacketEventData.DeviceNumber,
                                    (jint)EventData->EventData.ExtendedBurstDataPacketEventData.DeviceType,
                                    (jint)EventData->EventData.ExtendedBurstDataPacketEventData.TransmissionType,
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
                        case aetANTMRawDataPacket:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.RawDataPacketEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.RawDataPacketEventData.DataLength), (jbyte *)(EventData->EventData.RawDataPacketEventData.Data));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                              Env->CallVoidMethod(ANTMObject, Method_rawDataPacketEvent, ByteArray);
                           else
                           {
                              Env->ExceptionDescribe();
                              Env->ExceptionClear();
                           }

                           if(ByteArray)
                              Env->DeleteLocalRef(ByteArray);

                           break;
                        default:
                           PRINT_ERROR("Unknown ANTM Event Type.");

                     }

                     /* Check for Java exceptions thrown during the        */
                     /* callback.                                          */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during ANTM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, ANTMObject, LocalData);
                  }

                  Env->DeleteLocalRef(ANTMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the SPPM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The SPPM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("SPPM object was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in SPPM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The SPPM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: SPPM Manager was not cleaned up properly.");
            }
         }
         
         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_ANTM = (jclass)(NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_ANTM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_startupMessageEvent                 = Env->GetMethodID(Clazz, "startupMessageEvent",                 "(I)V");
      Method_channelResponseEvent                = Env->GetMethodID(Clazz, "channelResponseEvent",                "(III)V");
      Method_channelStatusEvent                  = Env->GetMethodID(Clazz, "channelStatusEvent",                  "(II)V");
      Method_channelIDEvent                      = Env->GetMethodID(Clazz, "channelIDEvent",                      "(IIII)V");
      Method_ANTVersionEvent                     = Env->GetMethodID(Clazz, "ANTVersionEvent",                     "([B)V");
      Method_capabilitiesEvent                   = Env->GetMethodID(Clazz, "capabilitiesEvent",                   "(III)V");
      Method_broadcastDataPacketEvent            = Env->GetMethodID(Clazz, "broadcastDataPacketEvent",            "(I[B)V");
      Method_acknowledgedDataPacketEvent         = Env->GetMethodID(Clazz, "acknowledgedDataPacketEvent",         "(I[B)V");
      Method_burstDataPacketEvent                = Env->GetMethodID(Clazz, "burstDataPacketEvent",                "(I[B)V");
      Method_extendedBroadcastDataPacketEvent    = Env->GetMethodID(Clazz, "extendedBroadcastDataPacketEvent",    "(IIII[B)V");
      Method_extendedAcknowledgedDataPacketEvent = Env->GetMethodID(Clazz, "extendedAcknowledgedDataPacketEvent", "(IIII[B)V");
      Method_extendedBurstDataPacketEvent        = Env->GetMethodID(Clazz, "extendedBurstDataPacketEvent",        "(IIII[B)V");
      Method_rawDataPacketEvent                  = Env->GetMethodID(Clazz, "rawDataPacketEvent",                  "([B)V");
   }
   else
      PRINT_ERROR("ANTM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.ANTM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
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
            if((RegistrationResult = ANTM_Register_Event_Callback(ANTM_EventCallback, LocalData->WeakObjectReference)) > 0)
            {
               LocalData->CallbackID = RegistrationResult;
               SetLocalData(Env, Object, LocalData);
            }
            else
            {
               PRINT_ERROR("ANTM: Unable to register callback for events (%d)", RegistrationResult);

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
            PRINT_ERROR("ANTM: Out of Memory: Unable to obtain weak reference to object");

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
 * Class:     com_stonestreetone_bluetopiapm_ANTM
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

      //TODO Add any other cleanup actions here
      ANTM_Un_Register_Event_Callback(LocalData->CallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    assignChannelNative
 * Signature: (IIII)I
 */
static jint AssignChannelNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint ChannelType, jint NetworkNumber, jint ExtendedAssignment)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Assign_Channel(LocalData->CallbackID, ChannelNumber, ChannelType, NetworkNumber, ExtendedAssignment);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    unAssignChannelNative
 * Signature: (I)I
 */
static jint UnAssignChannelNative(JNIEnv *Env, jobject Object, jint ChannelNumber)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Un_Assign_Channel(LocalData->CallbackID, ChannelNumber);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setChannelIDNative
 * Signature: (IIII)I
 */
static jint SetChannelIDNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint DeviceNumber, jint DeviceType, jint TransmissionType)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Channel_ID(LocalData->CallbackID, ChannelNumber, DeviceNumber, DeviceType, TransmissionType);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setChannelPeriodNative
 * Signature: (II)I
 */
static jint SetChannelPeriodNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint MessagingPeriod)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Channel_Period(LocalData->CallbackID, ChannelNumber, MessagingPeriod);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setChannelSearchTimeoutNative
 * Signature: (II)I
 */
static jint SetChannelSearchTimeoutNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint SearchTimeout)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Channel_Search_Timeout(LocalData->CallbackID, ChannelNumber, SearchTimeout);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setChannelRfFrequencyNative
 * Signature: (II)I
 */
static jint SetChannelRFFrequencyNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint RFFrequency)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Channel_RF_Frequency(LocalData->CallbackID, ChannelNumber, RFFrequency);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setNetworkKeyNative
 * Signature: (IBBBBBBBB)I
 */
static jint SetNetworkKeyNative(JNIEnv *Env, jobject Object, jint NetworkNumber, jbyte byte0, jbyte byte1, jbyte byte2, jbyte byte3, jbyte byte4, jbyte byte5, jbyte byte6, jbyte byte7)
{
   int ret_val;
   LocalData_t *LocalData;
   ANT_Network_Key_t NetworkKey;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ASSIGN_NETWORK_KEY(NetworkKey, byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7);

      ret_val = ANTM_Set_Network_Key(LocalData->CallbackID, NetworkNumber, NetworkKey);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setTransmitPowerNative
 * Signature: (I)I
 */
static jint SetTransmitPowerNative(JNIEnv *Env, jobject Object, jint TransmitPower)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Transmit_Power(LocalData->CallbackID, TransmitPower);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    addChannelIDNative
 * Signature: (IIIII)I
 */
static jint AddChannelIDNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint DeviceNumber, jint DeviceType, jint TransmissionType, jint ListIndex)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Add_Channel_ID(LocalData->CallbackID, ChannelNumber, DeviceNumber, DeviceType, TransmissionType, ListIndex);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    configureInclusionExclusionListNative
 * Signature: (III)I
 */
static jint ConfigureInclusionExclusionListNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint ListSize, jint Exclude)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Configure_Inclusion_Exclusion_List(LocalData->CallbackID, ChannelNumber, ListSize, Exclude);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setChannelTransmitPowerNative
 * Signature: (II)I
 */
static jint SetChannelTransmitPowerNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint TransmitPower)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Channel_Transmit_Power(LocalData->CallbackID, ChannelNumber, TransmitPower);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setLowPriorityChannelSearchTimeoutNative
 * Signature: (II)I
 */
static jint SetLowPriorityChannelSearchTimeoutNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint SearchTimeout)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Low_Priority_Channel_Search_Timeout(LocalData->CallbackID, ChannelNumber, SearchTimeout);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setSerialNumberChannelIDNative
 * Signature: (III)I
 */
static jint SetSerialNumberChannelIDNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint DeviceType, jint TransmissionType)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Serial_Number_Channel_ID(LocalData->CallbackID, ChannelNumber, DeviceType, TransmissionType);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    enableExtendedMessagesNative
 * Signature: (Z)I
 */
static jint EnableExtendedMessagesNative(JNIEnv *Env, jobject Object, jboolean Enable)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Enable_Extended_Messages(LocalData->CallbackID, Enable);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    enableLEDNative
 * Signature: (Z)I
 */
static jint EnableLEDNative(JNIEnv *Env, jobject Object, jboolean Enable)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Enable_LED(LocalData->CallbackID, Enable);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    enableCrystalNative
 * Signature: ()I
 */
static jint EnableCrystalNative(JNIEnv *Env, jobject Object)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Enable_Crystal(LocalData->CallbackID);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    configureExtendedMessagesNative
 * Signature: (I)I
 */
static jint ConfigureExtendedMessagesNative(JNIEnv *Env, jobject Object, jint Mask)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Configure_Extended_Messages(LocalData->CallbackID, Mask);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    configureFrequencyAgilityNative
 * Signature: (IIII)I
 */
static jint ConfigureFrequencyAgilityNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint Agility1, jint Agility2, jint Agility3)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Configure_Frequency_Agility(LocalData->CallbackID, ChannelNumber, Agility1, Agility2, Agility3);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setProximitySearchNative
 * Signature: (II)I
 */
static jint SetProximitySearchNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint SearchThreshold)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Proximity_Search(LocalData->CallbackID, ChannelNumber, SearchThreshold);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setChannelSearchPriorityNative
 * Signature: (II)I
 */
static jint SetChannelSearchPriorityNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint SearchPriority)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_Channel_Search_Priority(LocalData->CallbackID, ChannelNumber, SearchPriority);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setUSBDescriptorStringNative
 * Signature: (ILjava/lang/String;)I
 */
static jint SetUSBDescriptorStringNative(JNIEnv *Env, jobject Object, jint StringNumber, jstring DescriptorString)
{
   int           ret_val;
   char         *StringBuffer;
   const char   *String;
   unsigned int  StringLength;
   LocalData_t  *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((String = Env->GetStringUTFChars(DescriptorString, NULL)) != NULL)
      {
         StringLength = Env->GetStringUTFLength(DescriptorString);

         if((StringBuffer = (char *)malloc(StringLength+1)) != NULL)
         {
            strncpy(StringBuffer, String, StringLength);
            StringBuffer[StringLength] = '\0';

            ret_val = ANTM_Set_USB_Descriptor_String(LocalData->CallbackID, StringNumber, StringBuffer);

            free(StringBuffer);
         }
         else
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

         Env->ReleaseStringUTFChars(DescriptorString, String);
      }
      else
         ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   if((ret_val == BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY) && (Env->ExceptionCheck() == JNI_FALSE))
      Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    resetSystemNative
 * Signature: ()I
 */
static jint ResetSystemNative(JNIEnv *Env, jobject Object)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Reset_System(LocalData->CallbackID);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    openChannelNative
 * Signature: (I)I
 */
static jint OpenChannelNative(JNIEnv *Env, jobject Object, jint ChannelNumber)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Open_Channel(LocalData->CallbackID, ChannelNumber);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    closeChannelNative
 * Signature: (I)I
 */
static jint CloseChannelNative(JNIEnv *Env, jobject Object, jint ChannelNumber)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Close_Channel(LocalData->CallbackID, ChannelNumber);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    requestMessageNative
 * Signature: (II)I
 */
static jint RequestMessageNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jint MessageID)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Request_Message(LocalData->CallbackID, ChannelNumber, MessageID);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    openRxScanModeNative
 * Signature: (I)I
 */
static jint OpenRxScanModeNative(JNIEnv *Env, jobject Object, jint ChannelNumber)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Open_Rx_Scan_Mode(LocalData->CallbackID, ChannelNumber);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    sleepMessageNative
 * Signature: ()I
 */
static jint SleepMessageNative(JNIEnv *Env, jobject Object)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Sleep_Message(LocalData->CallbackID);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    sendBroadcastDataNative
 * Signature: (I[BII)I
 */
static jint SendBroadcastDataNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jbyteArray Data, jint Offset, jint DataLength)
{
   int    ret_val;
   jsize  BufferLength;
   jbyte *RawBuffer;
   
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      BufferLength = Env->GetArrayLength(Data);

      if((Data) && (DataLength <= ANT_MESSAGE_PAYLOAD_MAXIMUM_SIZE) && (DataLength >= 0) && (Offset >= 0) && ((Offset + DataLength) <= BufferLength))
      {
         if((RawBuffer = Env->GetByteArrayElements(Data, NULL)) != NULL)
         {
            ret_val = ANTM_Send_Broadcast_Data(LocalData->CallbackID, ChannelNumber, DataLength, (((Byte_t *)RawBuffer) + Offset));
            Env->ReleaseByteArrayElements(Data, RawBuffer, 0);
         }
         else
         {
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
         ret_val = BTPM_ERROR_CODE_INVALID_PARAMETER;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    sendAcknowledgedDataNative
 * Signature: (I[BII)I
 */
static jint SendAcknowledgedDataNative(JNIEnv *Env, jobject Object, jint ChannelNumber, jbyteArray Data, jint Offset, jint DataLength)
{
   int    ret_val;
   jsize  BufferLength;
   jbyte *RawBuffer;
   
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      BufferLength = Env->GetArrayLength(Data);

      if((Data) && (DataLength <= ANT_MESSAGE_PAYLOAD_MAXIMUM_SIZE) && (DataLength >= 0) && (Offset >= 0) && ((Offset + DataLength) <= BufferLength))
      {
         if((RawBuffer = Env->GetByteArrayElements(Data, NULL)) != NULL)
         {
            ret_val = ANTM_Send_Acknowledged_Data(LocalData->CallbackID, ChannelNumber, DataLength, (((Byte_t *)RawBuffer) + Offset));
            Env->ReleaseByteArrayElements(Data, RawBuffer, 0);
         }
         else
         {
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
         ret_val = BTPM_ERROR_CODE_INVALID_PARAMETER;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    sendBurstTransferDataNative
 * Signature: (I[BII)I
 */
static jint SendBurstTransferDataNative(JNIEnv *Env, jobject Object, jint SequenceChannelNumber, jbyteArray Data, jint Offset, jint DataLength)
{
   int    ret_val;
   jsize  BufferLength;
   jbyte *RawBuffer;
   
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      BufferLength = Env->GetArrayLength(Data);

      if((Data) && (DataLength <= ANT_MESSAGE_PAYLOAD_MAXIMUM_SIZE) && (DataLength >= 0) && (Offset >= 0) && ((Offset + DataLength) <= BufferLength))
      {
         if((RawBuffer = Env->GetByteArrayElements(Data, NULL)) != NULL)
         {
            ret_val = ANTM_Send_Burst_Transfer_Data(LocalData->CallbackID, SequenceChannelNumber, DataLength, (((Byte_t *)RawBuffer) + Offset));
            Env->ReleaseByteArrayElements(Data, RawBuffer, 0);
         }
         else
         {
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
         ret_val = BTPM_ERROR_CODE_INVALID_PARAMETER;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    initializeCWTestModeNative
 * Signature: ()I
 */
static jint InitializeCWTestModeNative(JNIEnv *Env, jobject Object)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Initialize_CW_Test_Mode(LocalData->CallbackID);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    setCWTestModeNative
 * Signature: (II)I
 */
static jint SetCWTestModeNative(JNIEnv *Env, jobject Object, jint TxPower, jint RFFrequency)
{
   int ret_val;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      ret_val = ANTM_Set_CW_Test_Mode(LocalData->CallbackID, TxPower, RFFrequency);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    sendRawPacket
 * Signature: ([B)I
 */
static jint SendRawPacketNative(JNIEnv *Env, jobject Object, jbyteArray Packet)
{
   int          ret_val;
   jbyte       *PacketData;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((PacketData = Env->GetByteArrayElements(Packet, NULL)) != NULL)
      {
         ret_val = ANTM_Send_Raw_Packet(LocalData->CallbackID, Env->GetArrayLength(Packet), (Byte_t *)PacketData);

         Env->ReleaseByteArrayElements(Packet, PacketData, JNI_ABORT);
      }
      else
      {
         ret_val = -1;
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      ret_val = -1;
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_ANTM
 * Method:    sendRawPacketAsync
 * Signature: ([B)V
 */
static void SendRawPacketAsyncNative(JNIEnv *Env, jobject Object, jbyteArray Packet)
{
   jbyte       *PacketData;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((PacketData = Env->GetByteArrayElements(Packet, NULL)) != NULL)
      {
         ANTM_Send_Raw_Packet_Async(LocalData->CallbackID, Env->GetArrayLength(Packet), (Byte_t *)PacketData);

         Env->ReleaseByteArrayElements(Packet, PacketData, JNI_ABORT);
      }
      else
      {
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
   {
      Env->ThrowNew(Env->FindClass("java/lang/IllegalStateException"), "Invalid Class Reference");
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                          "()V",                    (void*) InitClassNative},
   {"initObjectNative",                         "()V",                    (void*) InitObjectNative},
   {"cleanupObjectNative",                      "()V",                    (void*) CleanupObjectNative},
   {"assignChannelNative",                      "(IIII)I",                (void*) AssignChannelNative},
   {"unAssignChannelNative",                    "(I)I",                   (void*) UnAssignChannelNative},
   {"setChannelIDNative",                       "(IIII)I",                (void*) SetChannelIDNative},
   {"setChannelPeriodNative",                   "(II)I",                  (void*) SetChannelPeriodNative},
   {"setChannelSearchTimeoutNative",            "(II)I",                  (void*) SetChannelSearchTimeoutNative},
   {"setChannelRfFrequencyNative",              "(II)I",                  (void*) SetChannelRFFrequencyNative},
   {"setNetworkKeyNative",                      "(IBBBBBBBB)I",           (void*) SetNetworkKeyNative},
   {"setTransmitPowerNative",                   "(I)I",                   (void*) SetTransmitPowerNative},
   {"addChannelIDNative",                       "(IIIII)I",               (void*) AddChannelIDNative},
   {"configureInclusionExclusionListNative",    "(III)I",                 (void*) ConfigureInclusionExclusionListNative},
   {"setChannelTransmitPowerNative",            "(II)I",                  (void*) SetChannelTransmitPowerNative},
   {"setLowPriorityChannelSearchTimeoutNative", "(II)I",                  (void*) SetLowPriorityChannelSearchTimeoutNative},
   {"setSerialNumberChannelIDNative",           "(III)I",                 (void*) SetSerialNumberChannelIDNative},
   {"enableExtendedMessagesNative",             "(Z)I",                   (void*) EnableExtendedMessagesNative},
   {"enableLEDNative",                          "(Z)I",                   (void*) EnableLEDNative},
   {"enableCrystalNative",                      "()I",                    (void*) EnableCrystalNative},
   {"configureExtendedMessagesNative",          "(I)I",                   (void*) ConfigureExtendedMessagesNative},
   {"configureFrequencyAgilityNative",          "(IIII)I",                (void*) ConfigureFrequencyAgilityNative},
   {"setProximitySearchNative",                 "(II)I",                  (void*) SetProximitySearchNative},
   {"setChannelSearchPriorityNative",           "(II)I",                  (void*) SetChannelSearchPriorityNative},
   {"setUSBDescriptorStringNative",             "(ILjava/lang/String;)I", (void*) SetUSBDescriptorStringNative},
   {"resetSystemNative",                        "()I",                    (void*) ResetSystemNative},
   {"openChannelNative",                        "(I)I",                   (void*) OpenChannelNative},
   {"closeChannelNative",                       "(I)I",                   (void*) CloseChannelNative},
   {"requestMessageNative",                     "(II)I",                  (void*) RequestMessageNative},
   {"openRxScanModeNative",                     "(I)I",                   (void*) OpenRxScanModeNative},
   {"sleepMessageNative",                       "()I",                    (void*) SleepMessageNative},
   {"sendBroadcastDataNative",                  "(I[BII)I",               (void*) SendBroadcastDataNative},
   {"sendAcknowledgedDataNative",               "(I[BII)I",               (void*) SendAcknowledgedDataNative},
   {"sendBurstTransferDataNative",              "(I[BII)I",               (void*) SendBurstTransferDataNative},
   {"initializeCWTestModeNative",               "()I",                    (void*) InitializeCWTestModeNative},
   {"setCWTestModeNative",                      "(II)I",                  (void*) SetCWTestModeNative},
   {"sendRawPacketNative",                      "([B)I",                  (void*) SendRawPacketNative},
   {"sendRawPacketAsyncNative",                 "([B)V",                  (void*) SendRawPacketAsyncNative},
};

int register_com_stonestreetone_bluetopiapm_ANTM(JNIEnv *Env)
{
   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/ANTM";

   Result = -1;

   PRINT_DEBUG("Registering ANTM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}

