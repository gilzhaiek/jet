/*****< com_stonestreetone_bluetopiapm_HIDM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_HIDM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager HIDM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Glenn Steenrod                                                   */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   12/11/12  G. Steenrod    Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>

extern "C" {
#include "SS1BTHIDM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_HIDM.h"

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
static WEAK_REF Class_HIDM;

static jfieldID Field_HIDM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_HIDM_deviceConnectionRequest;
static jmethodID Method_HIDM_deviceConnected;
static jmethodID Method_HIDM_deviceConnectionStatus;
static jmethodID Method_HIDM_deviceDisconnected;
static jmethodID Method_HIDM_bootKeyboardKeyPress;
static jmethodID Method_HIDM_bootKeyboardKeyRepeat;
static jmethodID Method_HIDM_bootMouse;
static jmethodID Method_HIDM_reportDataReceived;

typedef struct _tagLocalData_t
{
   WEAK_REF WeakObjectReference;
   unsigned int CallbackID;
   unsigned int DataCallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_HIDM_localData, Exclusive)) == NULL)
      PRINT_ERROR("HIDM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_HIDM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_HIDM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void HIDM_EventCallback(HIDM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   JNIEnv      *Env;
   jobject      HIDMObject;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our HIDM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((HIDMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, HIDMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case hetHIDDeviceConnectionRequest:

                           Env->CallVoidMethod(HIDMObject, Method_HIDM_deviceConnectionRequest,
                              EventData->EventData.DeviceConnectionRequestData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.DeviceConnectionRequestData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.DeviceConnectionRequestData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.DeviceConnectionRequestData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.DeviceConnectionRequestData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.DeviceConnectionRequestData.RemoteDeviceAddress.BD_ADDR0);

                           break;

                        case hetHIDDeviceConnected:

                           Env->CallVoidMethod(HIDMObject, Method_HIDM_deviceConnected,
                              EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress.BD_ADDR0);

                           break;

                        case hetHIDDeviceConnectionStatus:

                           Env->CallVoidMethod(HIDMObject, Method_HIDM_deviceConnectionStatus,
                              EventData->EventData.DeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.DeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.DeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.DeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.DeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.DeviceConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.DeviceConnectionStatusEventData.ConnectionStatus);

                           break;

                        case hetHIDDeviceDisconnected:

                           Env->CallVoidMethod(HIDMObject, Method_HIDM_deviceDisconnected,
                              EventData->EventData.DeviceDisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.DeviceDisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.DeviceDisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.DeviceDisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.DeviceDisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.DeviceDisconnectedEventData.RemoteDeviceAddress.BD_ADDR0);

                           break;

                        case hetHIDBootKeyboardKeyPress:

                        case hetHIDBootKeyboardKeyRepeat:

                        case hetHIDBootMouseEvent:

                        case hetHIDReportDataReceived:

                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during HIDM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, HIDMObject, LocalData);
                  }

                  Env->DeleteLocalRef(HIDMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the HIDM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The HIDM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("HIDM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in HIDM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The HIDM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: HIDM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: HIDM Manager was already cleaned up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Event data callback handler.
 */
static void HIDM_DataEventCallback(HIDM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   JNIEnv      *Env;
   jobject      HIDMObject;
   LocalData_t *LocalData;
   jbyteArray   ByteArray;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our HIDM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((HIDMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, HIDMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case hetHIDDeviceConnectionRequest:

                        case hetHIDDeviceConnected:

                        case hetHIDDeviceConnectionStatus:

                        case hetHIDDeviceDisconnected:

                           break;

                        case hetHIDBootKeyboardKeyPress:

                           Env->CallVoidMethod(HIDMObject, Method_HIDM_bootKeyboardKeyPress,
                              EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress.BD_ADDR0,
                              ((EventData->EventData.BootKeyboardKeyPressEventData.KeyDown == TRUE) ? JNI_TRUE : JNI_FALSE),
                              EventData->EventData.BootKeyboardKeyPressEventData.KeyModifiers,
                              EventData->EventData.BootKeyboardKeyPressEventData.Key);

                           break;

                        case hetHIDBootKeyboardKeyRepeat:

                           Env->CallVoidMethod(HIDMObject, Method_HIDM_bootKeyboardKeyRepeat,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.KeyModifiers,
                              EventData->EventData.BootKeyboardKeyRepeatEventData.Key);

                           break;

                        case hetHIDBootMouseEvent:

                           Env->CallVoidMethod(HIDMObject, Method_HIDM_bootMouse,
                              EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.BootMouseEventEventData.CX,
                              EventData->EventData.BootMouseEventEventData.CY,
                              EventData->EventData.BootMouseEventEventData.ButtonState,
                              EventData->EventData.BootMouseEventEventData.CZ);

                           break;

                        case hetHIDReportDataReceived:

                           if(EventData->EventData.ReportDataReceivedEventData.ReportLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.ReportDataReceivedEventData.ReportLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.ReportDataReceivedEventData.ReportLength, (jbyte *)(EventData->EventData.ReportDataReceivedEventData.ReportData));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(HIDMObject, Method_HIDM_reportDataReceived,
                                    EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR0,
                                    ByteArray);
                              }

                              if(ByteArray)
                                 Env->DeleteLocalRef(ByteArray);
                           }
                           else
                           {
                              Env->CallVoidMethod(HIDMObject, Method_HIDM_reportDataReceived,
                                 EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ReportDataReceivedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 NULL);
                           }  

                           break;
                     }

                     ReleaseLocalData(Env, HIDMObject, LocalData);
                  }

                  Env->DeleteLocalRef(HIDMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the HIDM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The HIDM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("HIDM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in HIDM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The HIDM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: HIDM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: HIDM Manager was already cleaned up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_HIDM = (NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_HIDM_localData = Env->GetFieldID(Clazz, "localData", "J")) == NULL)
         PRINT_ERROR("Unable to locate $localData field");

      Method_HIDM_deviceConnectionRequest = Env->GetMethodID(Clazz, "deviceConnectionRequest", "(BBBBBB)V");
      Method_HIDM_deviceConnected         = Env->GetMethodID(Clazz, "deviceConnected",         "(BBBBBB)V");
      Method_HIDM_deviceConnectionStatus  = Env->GetMethodID(Clazz, "deviceConnectionStatus",  "(BBBBBBI)V");
      Method_HIDM_deviceDisconnected      = Env->GetMethodID(Clazz, "deviceDisconnected",      "(BBBBBB)V");  
      Method_HIDM_bootKeyboardKeyPress    = Env->GetMethodID(Clazz, "bootKeyboardKeyPress",    "(BBBBBBZSS)V");
      Method_HIDM_bootKeyboardKeyRepeat   = Env->GetMethodID(Clazz, "bootKeyboardKeyRepeat",   "(BBBBBBSS)V");
      Method_HIDM_bootMouse               = Env->GetMethodID(Clazz, "bootMouse",               "(BBBBBBBBSB)V");
      Method_HIDM_reportDataReceived      = Env->GetMethodID(Clazz, "reportDataReceived",      "(BBBBBB[B)V");
   }
   else
      PRINT_ERROR("HIDM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.HIDM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
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
            if((RegistrationResult = HIDM_Register_Event_Callback(HIDM_EventCallback, LocalData->WeakObjectReference)) > 0)
            {
               LocalData->DataCallbackID = 0;
               LocalData->CallbackID     = RegistrationResult;
               SetLocalData(Env, Object, LocalData);
            }
            else
            {
               PRINT_ERROR("HIDM: Unable to register callback for events (%d)", RegistrationResult);

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
            PRINT_ERROR("HIDM: Out of Memory: Unable to obtain weak reference to object");

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
 * Class:     com_stonestreetone_bluetopiapm_HIDM
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
         HIDM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

      if(LocalData->CallbackID)
         HIDM_Un_Register_Event_Callback(LocalData->CallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    registerDataEventCallbackNative
 * Signature: ()I
 */
static jint RegisterDataEventCallbackNative(JNIEnv *Env, jobject Object)
{
   jint         Result;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, TRUE)) != NULL)
   {
      if((Result = HIDM_Register_Data_Event_Callback(HIDM_DataEventCallback, LocalData->WeakObjectReference)) > 0)
         LocalData->DataCallbackID = Result;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
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
         HIDM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

         LocalData->DataCallbackID = 0;
      }

      ReleaseLocalData(Env, Object, LocalData);
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    connectionRequestResponseNative
 * Signature: (BBBBBBZJ)I
 */
static jint ConnectionRequestResponseNative(JNIEnv *, jobject, jbyte Address1, jbyte Address2, jbyte Address3 , jbyte Address4, jbyte Address5, jbyte Address6, jboolean Accept, jlong ConnectionFlags)
{
   int Result;
   unsigned long Flags;
   BD_ADDR_t     RemoteDeviceAddress;
   LocalData_t  *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_RESPONSE_CONNECTION_FLAG_REPORT_MODE) ? HIDM_CONNECTION_REQUEST_CONNECTION_FLAGS_REPORT_MODE : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_RESPONSE_CONNECTION_FLAG_PARSE_BOOT)  ? HIDM_CONNECTION_REQUEST_CONNECTION_FLAGS_PARSE_BOOT : 0);

   Result = HIDM_Connection_Request_Response(RemoteDeviceAddress, ((Accept == JNI_FALSE) ? FALSE : TRUE), Flags);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    connectRemoteDeviceNative
 * Signature: (BBBBBBJZ)I
 */
static jint ConnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ConnectionFlags, jboolean WaitForConnection)
{
   int                     Result;
   unsigned int            Status;
   unsigned long           Flags;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_CONNECTION_FLAG_REQUIRE_AUTHENTICATION) ? HIDM_CONNECT_HID_DEVICE_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_CONNECTION_FLAG_REQUIRE_ENCRYPTION)     ? HIDM_CONNECT_HID_DEVICE_FLAGS_REQUIRE_ENCRYPTION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_CONNECTION_FLAG_REPORT_MODE)            ? HIDM_CONNECT_HID_DEVICE_FLAGS_REPORT_MODE : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_CONNECTION_FLAG_PARSE_BOOT)             ? HIDM_CONNECT_HID_DEVICE_FLAGS_PARSE_BOOT : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HIDM_Connect_Remote_Device(RemoteDeviceAddress, Flags, HIDM_EventCallback, LocalData->WeakObjectReference, 0);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }
   else
   {
      Result = HIDM_Connect_Remote_Device(RemoteDeviceAddress, Flags, 0, 0, &Status);

      /* If the request was submitted correctly, use the connection  */
      /* status as the return value.                                 */
      if(Result == 0)
         Result = Status;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}
/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    disconnectDeviceNative
 * Signature: (BBBBBBZ)I
 */
static jint DisconnectDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean SendVirtualCableDisconnect)
{
   int       Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = HIDM_Disconnect_Device(RemoteDeviceAddress, ((SendVirtualCableDisconnect == JNI_TRUE) ? TRUE : FALSE));

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;

}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    queryConnectedDevicesNative
 * Signature: (J[[B)I
 */
static jint QueryConnectedDevicesNative(JNIEnv *Env, jobject Object, jlong MaximumConnectionListEntries, jobjectArray RemoteDeviceAddressArray)
{
   int          Result;
   int          ReturnedConnectedDevices;
   unsigned int TotalNumberConnectedDevices;
   int          BufferSize;
   jbyte       *RemoteDeviceAddressPointer;
   jbyteArray   RemoteDeviceAddresses;
   BD_ADDR_t   *RemoteDeviceAddressList;
   int          Index;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Result = 0;
   ReturnedConnectedDevices    = 0;
   TotalNumberConnectedDevices = 0;

   ReturnedConnectedDevices = HIDM_Query_Connected_Devices(0, NULL, &TotalNumberConnectedDevices);

   if(!(ReturnedConnectedDevices < 0))
   {
      if((TotalNumberConnectedDevices > 0) && (RemoteDeviceAddressArray))
      {
         if((MaximumConnectionListEntries > 0) && (MaximumConnectionListEntries < TotalNumberConnectedDevices))
         {
            BufferSize = sizeof(BD_ADDR_t) * MaximumConnectionListEntries;
         }
         else
         {
            BufferSize = sizeof(BD_ADDR_t) * TotalNumberConnectedDevices;
         }

         if((RemoteDeviceAddressList = (BD_ADDR_t *)malloc(BufferSize)) != NULL)
         {
            if((MaximumConnectionListEntries > 0) && (MaximumConnectionListEntries < TotalNumberConnectedDevices))
            {
               ReturnedConnectedDevices = HIDM_Query_Connected_Devices(MaximumConnectionListEntries, &(RemoteDeviceAddressList[0]), NULL);
            }
            else
            {
               ReturnedConnectedDevices = HIDM_Query_Connected_Devices(TotalNumberConnectedDevices, &(RemoteDeviceAddressList[0]), NULL);
            }

            if(ReturnedConnectedDevices > 0)
            {
               if((RemoteDeviceAddresses = Env->NewByteArray(ReturnedConnectedDevices * 6)) != NULL)
               {
                  if((RemoteDeviceAddressPointer = Env->GetByteArrayElements(RemoteDeviceAddresses, NULL)) != NULL)
                  {
                     for(int Index = 0; Index < ReturnedConnectedDevices; Index++)
                     {                             
                        RemoteDeviceAddressPointer[((Index * 6) + 0)] = RemoteDeviceAddressList[Index].BD_ADDR5;
                        RemoteDeviceAddressPointer[((Index * 6) + 1)] = RemoteDeviceAddressList[Index].BD_ADDR4;
                        RemoteDeviceAddressPointer[((Index * 6) + 2)] = RemoteDeviceAddressList[Index].BD_ADDR3;
                        RemoteDeviceAddressPointer[((Index * 6) + 3)] = RemoteDeviceAddressList[Index].BD_ADDR2;
                        RemoteDeviceAddressPointer[((Index * 6) + 4)] = RemoteDeviceAddressList[Index].BD_ADDR1;
                        RemoteDeviceAddressPointer[((Index * 6) + 5)] = RemoteDeviceAddressList[Index].BD_ADDR0;
                     }

                     Env->SetObjectArrayElement(RemoteDeviceAddressArray, 0, RemoteDeviceAddresses);
                     if(Env->ExceptionCheck())
                     {
                        Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                     }

                     Env->ReleaseByteArrayElements(RemoteDeviceAddresses, RemoteDeviceAddressPointer, 0);
                  }
                  else
                  {
                     Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                  }

                  Env->DeleteLocalRef(RemoteDeviceAddresses);
               }
               else
               {
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               }
                  
               Env->DeleteLocalRef(RemoteDeviceAddresses);
            }
         }
         else
         {
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         }
      }
      else
      {
         Result = TotalNumberConnectedDevices;
      }
   }
   
   if(!(Result))
   {
      Result = ReturnedConnectedDevices;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    changeIncomingConnectionFlagsNative
 * Signature: (J)I
 */
static jint ChangeIncomingConnectionFlagsNative(JNIEnv *Env, jobject Object, jlong ConnectionFlags)
{
   int Result;
   unsigned long Flags;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_INCOMING_CONNECTION_FLAG_REQUIRE_AUTHORIZATION)  ? HIDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_INCOMING_CONNECTION_FLAG_REQUIRE_AUTHENTICATION) ? HIDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_INCOMING_CONNECTION_FLAG_REQUIRE_ENCRYPTION)     ? HIDM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_INCOMING_CONNECTION_FLAG_REPORT_MODE)            ? HIDM_INCOMING_CONNECTION_FLAGS_REPORT_MODE : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HIDM_INCOMING_CONNECTION_FLAG_PARSE_BOOT)             ? HIDM_INCOMING_CONNECTION_FLAGS_PARSE_BOOT : 0);

   Result = HIDM_Change_Incoming_Connection_Flags(Flags);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    setKeyboardRepeatRateNative
 * Signature: (JJ)I
 */
static jint SetKeyboardRepeatRateNative(JNIEnv *Env, jobject Object, jlong RepeatDelay, jlong RepeatRate)
{
   int Result;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Result = HIDM_Set_Keyboard_Repeat_Rate((unsigned int)RepeatDelay, (unsigned int)RepeatRate);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HIDM
 * Method:    sendReportDataNative
 * Signature: (BBBBBB[B)I
 */
static jint SendReportDataNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyteArray ReportDataArray)
{
   jint      Result;
   LocalData_t *LocalData;
   jbyte    *ReportData; 
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(ReportDataArray)
   {
      if((ReportData = Env->GetByteArrayElements(ReportDataArray, NULL)) != NULL)
      {
         ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

         if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
         {
            Result = HIDM_Send_Report_Data(LocalData->DataCallbackID, RemoteDeviceAddress, sizeof(ReportData), (unsigned char *)ReportData);

            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = -1;

         Env->ReleaseByteArrayElements(ReportDataArray, ReportData, 0);
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
   }
   else
   {
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                                      "()V",                                                  (void*) InitClassNative},
   {"initObjectNative",                                     "()V",                                                  (void*) InitObjectNative},
   {"cleanupObjectNative",                                  "()V",                                                  (void*) CleanupObjectNative},
   {"registerDataEventCallbackNative",                      "()I",                                                  (void*) RegisterDataEventCallbackNative},
   {"unregisterDataEventCallbackNative",                    "()V",                                                  (void*) UnregisterDataEventCallbackNative},
   {"connectionRequestResponseNative",                      "(BBBBBBZJ)I",                                          (void*) ConnectionRequestResponseNative},
   {"connectRemoteDeviceNative",                            "(BBBBBBJZ)I",                                          (void*) ConnectRemoteDeviceNative},
   {"disconnectDeviceNative",                               "(BBBBBBZ)I",                                           (void*) DisconnectDeviceNative},
   {"queryConnectedDevicesNative",                          "(J[[B)I",                                              (void*) QueryConnectedDevicesNative},
   {"changeIncomingConnectionFlagsNative",                  "(J)I",                                                 (void*) ChangeIncomingConnectionFlagsNative},
   {"setKeyboardRepeatRateNative",                          "(JJ)I",                                                (void*) SetKeyboardRepeatRateNative},
   {"sendReportDataNative",                                 "(BBBBBB[B)I",                                          (void*) SendReportDataNative}
};

int register_com_stonestreetone_bluetopiapm_HIDM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/HIDM";

   Result = -1;

   PRINT_DEBUG("Registering HIDM native functions");

   if((Class = Env->FindClass(ClassName)) != NULL)
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));

   return Result;
}
