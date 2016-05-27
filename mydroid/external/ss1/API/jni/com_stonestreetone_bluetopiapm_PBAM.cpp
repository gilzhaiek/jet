/*****< com_stonestreetone_bluetopiapm_PBAM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_PBAM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager PBAM     */
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
#include "SS1BTPBAM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_PBAM.h"

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
static WEAK_REF Class_PBAM;

static jfieldID Field_PBAM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_PBAM_connectionStatusEvent;
static jmethodID Method_PBAM_disconnectedEvent;
static jmethodID Method_PBAM_vCardDataEvent;
static jmethodID Method_PBAM_vCardListingEvent;
static jmethodID Method_PBAM_phoneBookSizeEvent;
static jmethodID Method_PBAM_phoneBookSetEvent;

typedef struct _tagLocalData_t
{
   WEAK_REF WeakObjectReference;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_PBAM_localData, Exclusive)) == NULL)
      PRINT_ERROR("PBAM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_PBAM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_PBAM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void PBAM_EventCallback(PBAM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         IntVal1;
   jint         IntVal2;
   jint         IntVal3;
   JNIEnv      *Env;
   jobject      PBAMObject;
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
         /* The CallbackParameter is a weak ref to our PBAM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((PBAMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, PBAMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case petConnectionStatus:
                           switch(EventData->EventData.ConnectionStatusEventData.ConnectionStatus)
                           {
                              case PBAM_DEVICE_CONNECTION_STATUS_SUCCESS:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PBAM_CONNECTION_STATUS_SUCCESS;
                                 break;
                              case PBAM_DEVICE_CONNECTION_STATUS_FAILURE_TIMEOUT:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PBAM_CONNECTION_STATUS_FAILURE_TIMEOUT;
                                 break;
                              case PBAM_DEVICE_CONNECTION_STATUS_FAILURE_REFUSED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PBAM_CONNECTION_STATUS_FAILURE_REFUSED;
                                 break;
                              case PBAM_DEVICE_CONNECTION_STATUS_FAILURE_SECURITY:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PBAM_CONNECTION_STATUS_FAILURE_SECURITY;
                                 break;
                              case PBAM_DEVICE_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PBAM_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF;
                                 break;
                              case PBAM_DEVICE_CONNECTION_STATUS_FAILURE_UNKNOWN:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_PBAM_CONNECTION_STATUS_FAILURE_UNKNOWN;
                                 break;
                           }

                           Env->CallVoidMethod(PBAMObject, Method_PBAM_connectionStatusEvent,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1);
                           break;
                        case petDisconnected:
                           Env->CallVoidMethod(PBAMObject, Method_PBAM_disconnectedEvent,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.DisconnectedEventData.DisconnectReason);
                           break;
                        case petVCardData:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.VCardEventData.BufferSize)) != 0)
                           {
                              Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.VCardEventData.BufferSize, (jbyte *)(EventData->EventData.VCardEventData.Buffer));
                           }

                           Env->CallVoidMethod(PBAMObject, Method_PBAM_vCardDataEvent,
                              EventData->EventData.VCardEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.VCardEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.VCardEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.VCardEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.VCardEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.VCardEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.VCardEventData.Status,
                              ((EventData->EventData.VCardEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                              EventData->EventData.VCardEventData.NewMissedCalls,
                              ByteArray);

                           Env->DeleteLocalRef(ByteArray);
                           break;
                        case petVCardListing:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.VCardListingEventData.BufferSize)) != 0)
                           {
                              Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.VCardListingEventData.BufferSize, (jbyte *)(EventData->EventData.VCardListingEventData.Buffer));
                           }

                           Env->CallVoidMethod(PBAMObject, Method_PBAM_vCardListingEvent,
                              EventData->EventData.VCardListingEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.VCardListingEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.VCardListingEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.VCardListingEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.VCardListingEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.VCardListingEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.VCardListingEventData.Status,
                              ((EventData->EventData.VCardListingEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                              EventData->EventData.VCardListingEventData.NewMissedCalls,
                              ByteArray);

                           Env->DeleteLocalRef(ByteArray);
                           break;
                        case petPhoneBookSize:
                           Env->CallVoidMethod(PBAMObject, Method_PBAM_phoneBookSizeEvent,
                              EventData->EventData.PhoneBookSizeEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.PhoneBookSizeEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.PhoneBookSizeEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.PhoneBookSizeEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.PhoneBookSizeEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.PhoneBookSizeEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.PhoneBookSizeEventData.Status,
                              EventData->EventData.PhoneBookSizeEventData.PhoneBookSize);
                           break;
                        case petPhoneBookSet:
                           if((String = Env->NewStringUTF(EventData->EventData.PhoneBookSetEventData.CurrentPath)) != NULL)
                           {
                              Env->CallVoidMethod(PBAMObject, Method_PBAM_phoneBookSetEvent,
                                 EventData->EventData.PhoneBookSetEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.PhoneBookSetEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.PhoneBookSetEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.PhoneBookSetEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.PhoneBookSetEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.PhoneBookSetEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.PhoneBookSetEventData.Status,
                                 String);
                           }
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during PBAM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, PBAMObject, LocalData);
                  }

                  Env->DeleteLocalRef(PBAMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the PBAM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The PBAM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("PBAM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in PBAM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The PBAM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: PBAM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: PBAM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_PBAM = (jclass)(NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_PBAM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_PBAM_connectionStatusEvent = Env->GetMethodID(Clazz, "connectionStatusEvent", "(BBBBBBI)V");
      Method_PBAM_disconnectedEvent     = Env->GetMethodID(Clazz, "disconnectedEvent",     "(BBBBBBI)V");
      Method_PBAM_vCardDataEvent        = Env->GetMethodID(Clazz, "vCardDataEvent",        "(BBBBBBIZI[B)V");
      Method_PBAM_vCardListingEvent     = Env->GetMethodID(Clazz, "vCardListingEvent",     "(BBBBBBIZI[B)V");
      Method_PBAM_phoneBookSizeEvent    = Env->GetMethodID(Clazz, "phoneBookSizeEvent",    "(BBBBBBII)V");
      Method_PBAM_phoneBookSetEvent     = Env->GetMethodID(Clazz, "phoneBookSetEvent",     "(BBBBBBILjava/lang/String;)V");
   }
   else
      PRINT_ERROR("PBAM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.PBAM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    initObjectNative
 * Signature: ()V
 */
static void InitObjectNative(JNIEnv *Env, jobject Object)
{
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(InitBTPMClient(TRUE) == 0)
   {
      if((LocalData = (LocalData_t *)malloc(sizeof(LocalData_t))) != NULL)
      {
         if((LocalData->WeakObjectReference = NewWeakRef(Env, Object)) != NULL)
         {
            SetLocalData(Env, Object, LocalData);
         }
         else
         {
            /* No need to throw an exception here. NewWeakRef will throw*/
            /* an OutOfMemoryError if we are out of resources.          */
            PRINT_ERROR("PBAM: Out of Memory: Unable to obtain weak reference to object");

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
 * Class:     com_stonestreetone_bluetopiapm_PBAM
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

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    connectRemoteDeviceNative
 * Signature: (BBBBBBIIZ)I
 */
static jint ConnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint RemoteServerPort, jint ConnectionFlags, jboolean WaitForConnection)
{
   jint          Result;
   BD_ADDR_t     RemoteDeviceAddress;
   LocalData_t  *LocalData;
   unsigned int  ConnectionStatus;
   unsigned int  Flags;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Flags  = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_PBAM_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? PBAM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_PBAM_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? PBAM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_ENCRYPTION     : 0);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if(WaitForConnection == JNI_FALSE)
         Result = PBAM_Connect_Remote_Device(RemoteDeviceAddress, RemoteServerPort, Flags, PBAM_EventCallback, LocalData->WeakObjectReference, NULL);
      else
      {
         Result = PBAM_Connect_Remote_Device(RemoteDeviceAddress, RemoteServerPort, Flags, PBAM_EventCallback, LocalData->WeakObjectReference, &ConnectionStatus);

         if(Result == 0)
            Result = ConnectionStatus;
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    disconnectNative
 * Signature: (BBBBBB)I
 */
static jint DisconnectNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = PBAM_Disconnect_Device(RemoteDeviceAddress);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    abortNative
 * Signature: (BBBBBB)I
 */
static jint AbortNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = PBAM_Abort(RemoteDeviceAddress);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    pullPhoneBookNative
 * Signature: (BBBBBBLjava/lang/String;JIII)I
 */
static jint PullPhoneBookNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring ObjectName, jlong Filter, jint VCardFormat, jint MaxListCount, jint ListStartOffset)
{
   jint                Result;
   BD_ADDR_t           RemoteDeviceAddress;
   const char         *String;
   PBAM_VCard_Format_t Format;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((String = Env->GetStringUTFChars(ObjectName, NULL)) != NULL)
   {
      switch(VCardFormat)
      {
         case com_stonestreetone_bluetopiapm_PBAM_VCARD_FORMAT_VCARD21:
            Format = pmvCard21;
            break;
         case com_stonestreetone_bluetopiapm_PBAM_VCARD_FORMAT_VCARD30:
            Format = pmvCard30;
            break;
         case com_stonestreetone_bluetopiapm_PBAM_VCARD_FORMAT_DEFAULT:
         default:
            Format = pmDefault;
            break;
      }

      Result = PBAM_Pull_Phone_Book(RemoteDeviceAddress, const_cast<char *>(String), (DWord_t)(Filter), (DWord_t)(Filter >> 32), Format, MaxListCount, ListStartOffset);

      Env->ReleaseStringUTFChars(ObjectName, String);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    pullPhoneBookSizeNative
 * Signature: (BBBBBB)I
 */
static jint PullPhoneBookSizeNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = PBAM_Pull_Phone_Book_Size(RemoteDeviceAddress);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    setPhoneBookNative
 * Signature: (BBBBBBILjava/lang/String;)I
 */
static jint SetPhoneBookNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint PathOption, jstring FolderName)
{
   jint                   Result;
   const char            *String;
   BD_ADDR_t              RemoteDeviceAddress;
   PBAM_Set_Path_Option_t Option;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(PathOption) {
      case com_stonestreetone_bluetopiapm_PBAM_PATH_OPTION_ROOT:
         Option = pspRoot;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_PATH_OPTION_DOWN:
         Option = pspDown;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_PATH_OPTION_UP:
      default:
         Option = pspUp;
         break;
   }

   String = NULL;
   
   if((!FolderName) || ((String = Env->GetStringUTFChars(FolderName, NULL)) != NULL))
   {
      Result = PBAM_Set_Phone_Book(RemoteDeviceAddress, Option, const_cast<char *>(String));

      if(String)
         Env->ReleaseStringUTFChars(FolderName, String);
   }
   else
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    pullvCardListingNative
 * Signature: (BBBBBBLjava/lang/String;IILjava/lang/String;II)I
 */
static jint PullvCardListingNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring PhonebookPath, jint ListOrder, jint SearchAttribute, jstring SearchValue, jint MaxListCount, jint ListStartOffset)
{
   jint                     Result;
   const char              *Path;
   const char              *SValue;
   BD_ADDR_t                RemoteDeviceAddress;
   PBAM_List_Order_t        Order;
   PBAM_Search_Attribute_t  Attribute;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ListOrder) {
      case com_stonestreetone_bluetopiapm_PBAM_LIST_ORDER_INDEXED:
         Order = ploIndexed;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_LIST_ORDER_ALPHABETICAL:
         Order = ploAlphabetical;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_LIST_ORDER_PHONETICAL:
         Order = ploPhonetical;
         break;
      default:
         Order = ploDefault;
   }

   switch(SearchAttribute) {
      case com_stonestreetone_bluetopiapm_PBAM_SEARCH_ATTRIBUTE_NAME:
         Attribute = psaName;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_SEARCH_ATTRIBUTE_NUMBER:
         Attribute = psaNumber;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_SEARCH_ATTRIBUTE_SOUND:
         Attribute = psaSound;
         break;
      default:
         Attribute = psaDefault;
   }

   SValue = NULL;

   if(((Path = Env->GetStringUTFChars(PhonebookPath, NULL)) != NULL) && ((!SearchValue) || ((SValue = Env->GetStringUTFChars(SearchValue, NULL)) != NULL)))
   {
      PRINT_DEBUG("%s\n", ((SearchValue)?"NOT NULL":"NULL"));

      Result = PBAM_Pull_vCard_Listing(RemoteDeviceAddress, const_cast<char *>(Path), Order, Attribute, const_cast<char *>(SValue), MaxListCount, ListStartOffset);

      Env->ReleaseStringUTFChars(PhonebookPath, Path);

      if(SValue)
         Env->ReleaseStringUTFChars(SearchValue, SValue);
   }
   else
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    pullvCardNative
 * Signature: (BBBBBBLjava/lang/String;JI)I
 */
static jint PullvCardNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring VCardName, jlong Filter, jint VCardFormat)
{
   jint                 Result;
   BD_ADDR_t            RemoteDeviceAddress;
   const char          *String;
   PBAM_VCard_Format_t  Format;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(VCardFormat)
   {
      case com_stonestreetone_bluetopiapm_PBAM_VCARD_FORMAT_VCARD21:
         Format = pmvCard21;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_VCARD_FORMAT_VCARD30:
         Format = pmvCard30;
         break;
      case com_stonestreetone_bluetopiapm_PBAM_VCARD_FORMAT_DEFAULT:
      default:
         Format = pmDefault;
         break;
   }

   if((String = Env->GetStringUTFChars(VCardName, NULL)) != NULL)
   {
      Result = PBAM_Pull_vCard(RemoteDeviceAddress, const_cast<char *>(String), (DWord_t)Filter, (DWord_t)(Filter >> 32), Format);

      Env->ReleaseStringUTFChars(VCardName, String);
   }
   else
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   
   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_PBAM
 * Method:    setPhoneBookAbsoluteNative
 * Signature: (BBBBBBLjava/lang/String;)I
 */
static jint SetPhoneBookAbsoluteNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring AbsolutePath)
{
   jint                 Result;
   BD_ADDR_t            RemoteDeviceAddress;
   const char          *String;
   PBAM_VCard_Format_t  Format;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((String = Env->GetStringUTFChars(AbsolutePath, NULL)) != NULL)
   {
      Result = PBAM_Set_Phone_Book_Absolute(RemoteDeviceAddress, const_cast<char *>(String));

      Env->ReleaseStringUTFChars(AbsolutePath, String);
   }
   else
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   
   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",           "()V",                                               (void*) InitClassNative},
   {"initObjectNative",          "()V",                                               (void*) InitObjectNative},
   {"cleanupObjectNative",       "()V",                                               (void*) CleanupObjectNative},
   {"connectRemoteDeviceNative", "(BBBBBBIIZ)I",                                      (void*) ConnectRemoteDeviceNative},
   {"disconnectNative",          "(BBBBBB)I",                                         (void*) DisconnectNative},
   {"abortNative",               "(BBBBBB)I",                                         (void*) AbortNative},
   {"pullPhoneBookNative",       "(BBBBBBLjava/lang/String;JIII)I",                   (void*) PullPhoneBookNative},
   {"pullPhoneBookSizeNative",   "(BBBBBB)I",                                         (void*) PullPhoneBookSizeNative},
   {"setPhoneBookNative",        "(BBBBBBILjava/lang/String;)I",                      (void*) SetPhoneBookNative},
   {"pullvCardListingNative",    "(BBBBBBLjava/lang/String;IILjava/lang/String;II)I", (void*) PullvCardListingNative},
   {"pullvCardNative",           "(BBBBBBLjava/lang/String;JI)I",                     (void*) PullvCardNative},
   {"setPhoneBookAbsoluteNative","(BBBBBBLjava/lang/String;)I",                       (void*) SetPhoneBookAbsoluteNative}
};

int register_com_stonestreetone_bluetopiapm_PBAM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/PBAM";

   Result = -1;

   PRINT_DEBUG("Registering PBAM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}



