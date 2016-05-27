/*****< com_stonestreetone_bluetopiapm_MAPM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_MAPM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager MAPM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Glenn Steenrod                                                   */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   09/21/12  G. Steenrod    Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>

extern "C" {
#include "SS1BTMAPM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_MAPM.h"

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
static WEAK_REF Class_MAPM;

static jfieldID Field_MAPM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_MAPM_incomingConnectionRequest;
static jmethodID Method_MAPM_connected;
static jmethodID Method_MAPM_disconnected;
static jmethodID Method_MAPM_connectionStatus;

static jmethodID Method_MAPM_enableNotificationsResponse;
static jmethodID Method_MAPM_notificationIndication;
static jmethodID Method_MAPM_getFolderListingResponse;
static jmethodID Method_MAPM_getFolderListingSizeResponse;
static jmethodID Method_MAPM_getMessageListingResponse;
static jmethodID Method_MAPM_getMessageListingSizeResponse;
static jmethodID Method_MAPM_getMessageResponse;
static jmethodID Method_MAPM_setMessageStatusResponse;
static jmethodID Method_MAPM_pushMessageResponse;
static jmethodID Method_MAPM_updateInboxResponse;
static jmethodID Method_MAPM_setFolderResponse;

static jmethodID Method_MAPM_enableNotificationsRequest;
static jmethodID Method_MAPM_notificationConfirmation;
static jmethodID Method_MAPM_getFolderListingRequest;
static jmethodID Method_MAPM_getFolderListingSizeRequest;
static jmethodID Method_MAPM_getMessageListingRequest;
static jmethodID Method_MAPM_getMessageListingSizeRequest;
static jmethodID Method_MAPM_getMessageRequest;
static jmethodID Method_MAPM_setMessageStatusRequest;
static jmethodID Method_MAPM_pushMessageRequest;
static jmethodID Method_MAPM_updateInboxRequest;
static jmethodID Method_MAPM_setFolderRequest;

typedef struct _tagLocalData_t
{
   WEAK_REF WeakObjectReference;
   unsigned int CallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_MAPM_localData, Exclusive)) == NULL)
      PRINT_ERROR("MAPM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_MAPM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_MAPM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void MAPM_EventCallback(MAPM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         IntegerValue01;
   JNIEnv      *Env;
   jobject      MAPMObject;
   jstring      String;
   BD_ADDR_t   *RemoteDevice;
   jbyteArray   ByteArray;
   jbyte       *ByteArrayData;
   jintArray    IntegerArray;
   jint         IntegerArrayData[6];        
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our MAPM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((MAPMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, MAPMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case metMAPIncomingConnectionRequest:
                        case metMAPConnected:
//XXX: Add support for server-role.
                           PRINT_DEBUG("%s: MAP Server-role event received (%d). Server-role not currently supported.", __FUNCTION__, EventData->EventType);
                           break;

                        case metMAPDisconnected:
                           switch(EventData->EventData.DisconnectedEventData.ConnectionType)
                           {  
                              case mctNotificationServer:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_SERVER;
                                 break;
                              case mctNotificationClient:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_CLIENT;
                                 break;
                              case mctMessageAccessServer:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_SERVER;
                                 break;
                              case mctMessageAccessClient:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(MAPMObject, Method_MAPM_disconnected,
                              IntegerValue01,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.DisconnectedEventData.InstanceID);

                           break;

                        case metMAPConnectionStatus:
                           switch(EventData->EventData.ConnectionStatusEventData.ConnectionType)
                           {  
                              case mctNotificationServer:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_SERVER;
                                 break;
                              case mctNotificationClient:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_CLIENT;
                                 break;
                              case mctMessageAccessServer:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_SERVER;
                                 break;
                              case mctMessageAccessClient:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(MAPMObject, Method_MAPM_connectionStatus,
                                 IntegerValue01,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.ConnectionStatusEventData.InstanceID,
                                 EventData->EventData.ConnectionStatusEventData.ConnectionStatus);

                           break;

                        case metMAPEnableNotificationsResponse:
                           Env->CallVoidMethod(MAPMObject, Method_MAPM_enableNotificationsResponse,
                              EventData->EventData.EnableNotificationsResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.EnableNotificationsResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.EnableNotificationsResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.EnableNotificationsResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.EnableNotificationsResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.EnableNotificationsResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.EnableNotificationsResponseEventData.InstanceID,
                              EventData->EventData.EnableNotificationsResponseEventData.ResponseStatusCode);

                           break;

                        case metMAPGetFolderListingResponse:
                           if(EventData->EventData.GetFolderListingResponseEventData.FolderListingLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.GetFolderListingResponseEventData.FolderListingLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.GetFolderListingResponseEventData.FolderListingLength, (jbyte *)(EventData->EventData.GetFolderListingResponseEventData.FolderListingData));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(MAPMObject, Method_MAPM_getFolderListingResponse,
                                    EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.GetFolderListingResponseEventData.InstanceID,
                                    EventData->EventData.GetFolderListingResponseEventData.ResponseStatusCode,         
                                    ((EventData->EventData.GetFolderListingResponseEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    ByteArray);
                              }

                              if(ByteArray)
                                 Env->DeleteLocalRef(ByteArray);
                           }
                           else
                           {
                              Env->CallVoidMethod(MAPMObject, Method_MAPM_getFolderListingResponse,
                                 EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.GetFolderListingResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.GetFolderListingResponseEventData.InstanceID,
                                 EventData->EventData.GetFolderListingResponseEventData.ResponseStatusCode,         
                                 ((EventData->EventData.GetFolderListingResponseEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 NULL);
                           }

                           break;

                        case metMAPGetFolderListingSizeResponse:
                           Env->CallVoidMethod(MAPMObject, Method_MAPM_getFolderListingSizeResponse,
                              EventData->EventData.GetFolderListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.GetFolderListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.GetFolderListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.GetFolderListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.GetFolderListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.GetFolderListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.GetFolderListingSizeResponseEventData.InstanceID,
                              EventData->EventData.GetFolderListingSizeResponseEventData.ResponseStatusCode,              
                              EventData->EventData.GetFolderListingSizeResponseEventData.NumberOfFolders);

                           break;

                        case metMAPGetMessageListingResponse:
                           IntegerArrayData[0] = (jint)EventData->EventData.GetMessageListingResponseEventData.MSETime.Year;  
                           IntegerArrayData[1] = (jint)EventData->EventData.GetMessageListingResponseEventData.MSETime.Month;
                           IntegerArrayData[2] = (jint)EventData->EventData.GetMessageListingResponseEventData.MSETime.Day;
                           IntegerArrayData[3] = (jint)EventData->EventData.GetMessageListingResponseEventData.MSETime.Hour;  
                           IntegerArrayData[4] = (jint)EventData->EventData.GetMessageListingResponseEventData.MSETime.Minute;
                           IntegerArrayData[5] = (jint)EventData->EventData.GetMessageListingResponseEventData.MSETime.Second;

                           if((IntegerArray = Env->NewIntArray(sizeof(IntegerArrayData)/sizeof(jint))) != NULL)
                           {
                              Env->SetIntArrayRegion(IntegerArray, 0, sizeof(IntegerArrayData)/sizeof(jint), IntegerArrayData);
                           }

                           if(EventData->EventData.GetMessageListingResponseEventData.MessageListingLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.GetMessageListingResponseEventData.MessageListingLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.GetMessageListingResponseEventData.MessageListingLength, (jbyte *)(EventData->EventData.GetMessageListingResponseEventData.MessageListingData));
                              }
                         
                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(MAPMObject, Method_MAPM_getMessageListingResponse,
                                    EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.GetMessageListingResponseEventData.InstanceID,
                                    EventData->EventData.GetMessageListingResponseEventData.ResponseStatusCode,    
                                    ((EventData->EventData.GetMessageListingResponseEventData.NewMessage == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    IntegerArray,
                                    ((EventData->EventData.GetMessageListingResponseEventData.MSETime.UTC_Time == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    EventData->EventData.GetMessageListingResponseEventData.MSETime.UTC_Offset,  
                                    ((EventData->EventData.GetMessageListingResponseEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    EventData->EventData.GetMessageListingResponseEventData.NumberOfMessages,
                                    ByteArray);
                              }

                              if(ByteArray)
                                 Env->DeleteLocalRef(ByteArray);
                           }
                           else
                           {
                              Env->CallVoidMethod(MAPMObject, Method_MAPM_getMessageListingResponse,
                                 EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.GetMessageListingResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.GetMessageListingResponseEventData.InstanceID,
                                 EventData->EventData.GetMessageListingResponseEventData.ResponseStatusCode,    
                                 ((EventData->EventData.GetMessageListingResponseEventData.NewMessage == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 IntegerArray,
                                 ((EventData->EventData.GetMessageListingResponseEventData.MSETime.UTC_Time == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 EventData->EventData.GetMessageListingResponseEventData.MSETime.UTC_Offset,  
                                 ((EventData->EventData.GetMessageListingResponseEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 EventData->EventData.GetMessageListingResponseEventData.NumberOfMessages,
                                 NULL);
                           }

                           if(IntegerArray)
                              Env->DeleteLocalRef(IntegerArray);

                           break;

                        case metMAPGetMessageListingSizeResponse:
                           IntegerArrayData[0] = (jint)EventData->EventData.GetMessageListingSizeResponseEventData.MSETime.Year;  
                           IntegerArrayData[1] = (jint)EventData->EventData.GetMessageListingSizeResponseEventData.MSETime.Month;
                           IntegerArrayData[2] = (jint)EventData->EventData.GetMessageListingSizeResponseEventData.MSETime.Day;
                           IntegerArrayData[3] = (jint)EventData->EventData.GetMessageListingSizeResponseEventData.MSETime.Hour;  
                           IntegerArrayData[4] = (jint)EventData->EventData.GetMessageListingSizeResponseEventData.MSETime.Minute;
                           IntegerArrayData[5] = (jint)EventData->EventData.GetMessageListingSizeResponseEventData.MSETime.Second;

                           if((IntegerArray = Env->NewIntArray(sizeof(IntegerArrayData)/sizeof(jint))) != NULL)
                           {
                              Env->SetIntArrayRegion(IntegerArray, 0, sizeof(IntegerArrayData)/sizeof(jint), IntegerArrayData);
                           }

                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(MAPMObject, Method_MAPM_getMessageListingSizeResponse,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.InstanceID,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.ResponseStatusCode,      
                                 ((EventData->EventData.GetMessageListingResponseEventData.NewMessage == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 IntegerArray,
                                 ((EventData->EventData.GetMessageListingResponseEventData.MSETime.UTC_Time == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 EventData->EventData.GetMessageListingResponseEventData.MSETime.UTC_Offset,
                                 EventData->EventData.GetMessageListingSizeResponseEventData.NumberOfMessages);
                           }

                           if(IntegerArray)
                              Env->DeleteLocalRef(IntegerArray);

                           break;

                        case metMAPGetMessageResponse:
                           switch(EventData->EventData.GetMessageResponseEventData.FractionalType)
                           {  
                              case ftMore:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_FRACTION_DELIVER_MORE;
                                 break;
                              case ftLast:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_MAPM_FRACTION_DELIVER_LAST;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           if(EventData->EventData.GetMessageResponseEventData.MessageDataLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.GetMessageResponseEventData.MessageDataLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.GetMessageResponseEventData.MessageDataLength, (jbyte *)(EventData->EventData.GetMessageResponseEventData.MessageData));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(MAPMObject, Method_MAPM_getMessageResponse,
                                    EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.GetMessageResponseEventData.InstanceID,
                                    EventData->EventData.GetMessageResponseEventData.ResponseStatusCode,       
                                    IntegerValue01,
                                    ((EventData->EventData.GetMessageResponseEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    ByteArray);
                              }

                              if(ByteArray)
                                 Env->DeleteLocalRef(ByteArray);
                           }
                           else
                           {
                              Env->CallVoidMethod(MAPMObject, Method_MAPM_getMessageResponse,
                                 EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.GetMessageResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.GetMessageResponseEventData.InstanceID,
                                 EventData->EventData.GetMessageResponseEventData.ResponseStatusCode,       
                                 IntegerValue01,
                                 ((EventData->EventData.GetMessageResponseEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 NULL);
                           }  

                           break;

                        case metMAPSetMessageStatusResponse:
                           Env->CallVoidMethod(MAPMObject, Method_MAPM_setMessageStatusResponse,
                              EventData->EventData.SetMessageStatusResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.SetMessageStatusResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.SetMessageStatusResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.SetMessageStatusResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.SetMessageStatusResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.SetMessageStatusResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.SetMessageStatusResponseEventData.InstanceID,
                              EventData->EventData.SetMessageStatusResponseEventData.ResponseStatusCode);

                           break;

                        case metMAPPushMessageResponse:
                           if(EventData->EventData.PushMessageResponseEventData.MessageHandle)
                           {
                              if((String = Env->NewStringUTF(EventData->EventData.PushMessageResponseEventData.MessageHandle)) != NULL)
                              {
                                 Env->CallVoidMethod(MAPMObject, Method_MAPM_setMessageStatusResponse,
                                    EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.PushMessageResponseEventData.InstanceID,
                                    EventData->EventData.PushMessageResponseEventData.ResponseStatusCode,    
                                    String);

                                 Env->DeleteLocalRef(String);
                              }
                           }
                           else
                           {
                              Env->CallVoidMethod(MAPMObject, Method_MAPM_setMessageStatusResponse,
                                 EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.PushMessageResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.PushMessageResponseEventData.InstanceID,
                                 EventData->EventData.PushMessageResponseEventData.ResponseStatusCode,    
                                 NULL);
                           }

                           break;

                        case metMAPUpdateInboxResponse:
                           Env->CallVoidMethod(MAPMObject, Method_MAPM_updateInboxResponse,
                              EventData->EventData.UpdateInboxResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.UpdateInboxResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.UpdateInboxResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.UpdateInboxResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.UpdateInboxResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.UpdateInboxResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.UpdateInboxResponseEventData.InstanceID,
                              EventData->EventData.UpdateInboxResponseEventData.ResponseStatusCode); 

                           break;

                        case metMAPSetFolderResponse:
                           if(EventData->EventData.SetFolderResponseEventData.CurrentPath)
                           {
                              if((String = Env->NewStringUTF(EventData->EventData.SetFolderResponseEventData.CurrentPath)) != NULL)
                              {
                                 Env->CallVoidMethod(MAPMObject, Method_MAPM_setFolderResponse,
                                    EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.SetFolderResponseEventData.InstanceID,
                                    EventData->EventData.SetFolderResponseEventData.ResponseStatusCode,       
                                    String);

                                 Env->DeleteLocalRef(String);
                              }
                           }
                           else
                           {
                              Env->CallVoidMethod(MAPMObject, Method_MAPM_setFolderResponse,
                                 EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.SetFolderResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.SetFolderResponseEventData.InstanceID,
                                 EventData->EventData.SetFolderResponseEventData.ResponseStatusCode,       
                                 NULL);
                           }
 
                           break;

                        case metMAPNotificationIndication:
                           if(EventData->EventData.NotificationIndicationEventData.EventReportLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.NotificationIndicationEventData.EventReportLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.NotificationIndicationEventData.EventReportLength, (jbyte *)(EventData->EventData.NotificationIndicationEventData.EventReportData));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(MAPMObject, Method_MAPM_notificationIndication,
                                    EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.NotificationIndicationEventData.InstanceID,
                                    ((EventData->EventData.NotificationIndicationEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    ByteArray);  
                              }

                              if(ByteArray)
                                 Env->DeleteLocalRef(ByteArray);
                           }
                           else
                           {
                              Env->CallVoidMethod(MAPMObject, Method_MAPM_notificationIndication,
                                 EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.NotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.NotificationIndicationEventData.InstanceID,
                                 ((EventData->EventData.NotificationIndicationEventData.Final == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 NULL);
                           }

                           break;

                        case metMAPEnableNotificationsIndication:
                        case metMAPGetFolderListingRequest:
                        case metMAPGetFolderListingSizeRequest:
                        case metMAPGetMessageListingRequest:
                        case metMAPGetMessageListingSizeRequest:
                        case metMAPGetMessageRequest:
                        case metMAPSetMessageStatusRequest:
                        case metMAPPushMessageRequest:
                        case metMAPUpdateInboxRequest:
                        case metMAPSetFolderRequest:
                        case metMAPNotificationConfirmation:
//XXX: Add support for server-role.
                           PRINT_DEBUG("%s: MAP Server-role event received (%d). Server-role not currently supported.", __FUNCTION__, EventData->EventType);
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during MAPM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, MAPMObject, LocalData);
                  }

                  Env->DeleteLocalRef(MAPMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the MAPM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The MAPM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("MAPM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in MAPM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The MAPM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: MAPM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: MAPM Manager was already cleaned up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_MAPM = (NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_MAPM_localData = Env->GetFieldID(Clazz, "localData", "J")) == NULL)
         PRINT_ERROR("Unable to locate $localData field");

      Method_MAPM_disconnected                  = Env->GetMethodID(Clazz, "disconnected",                  "(IBBBBBBI)V");
      Method_MAPM_connectionStatus              = Env->GetMethodID(Clazz, "connectionStatus",              "(IBBBBBBII)V");
      Method_MAPM_enableNotificationsResponse   = Env->GetMethodID(Clazz, "enableNotificationsResponse",   "(BBBBBBII)V");  
      Method_MAPM_getFolderListingResponse      = Env->GetMethodID(Clazz, "getFolderListingResponse",      "(BBBBBBIIZ[B)V");
      Method_MAPM_getFolderListingSizeResponse  = Env->GetMethodID(Clazz, "getFolderListingSizeResponse",  "(BBBBBBIII)V");
      Method_MAPM_getMessageListingResponse     = Env->GetMethodID(Clazz, "getMessageListingResponse",     "(BBBBBBIIZ[IZIZI[B)V");
      Method_MAPM_getMessageListingSizeResponse = Env->GetMethodID(Clazz, "getMessageListingSizeResponse", "(BBBBBBIIZ[IZII)V");
      Method_MAPM_getMessageResponse            = Env->GetMethodID(Clazz, "getMessageResponse",            "(BBBBBBIIIZ[B)V");
      Method_MAPM_setMessageStatusResponse      = Env->GetMethodID(Clazz, "setMessageStatusResponse",      "(BBBBBBII)V");
      Method_MAPM_pushMessageResponse           = Env->GetMethodID(Clazz, "pushMessageResponse",           "(BBBBBBIILjava/lang/String;)V");
      Method_MAPM_updateInboxResponse           = Env->GetMethodID(Clazz, "updateInboxResponse",           "(BBBBBBII)V");
      Method_MAPM_setFolderResponse             = Env->GetMethodID(Clazz, "setFolderResponse",             "(BBBBBBIILjava/lang/String;)V");
      Method_MAPM_notificationIndication        = Env->GetMethodID(Clazz, "notificationIndication",        "(BBBBBBIZ[B)V");
   }
   else
      PRINT_ERROR("MAPM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.MAPM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
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
            PRINT_ERROR("MAPM: Out of Memory: Unable to obtain weak reference to object");

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
 * Class:     com_stonestreetone_bluetopiapm_MAPM
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
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    connectRemoteDeviceNative
 * Signature: (IBBBBBBIIIZ)I
 */
static jint ConnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jint connectionClient, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint RemoteServerPort, jint InstanceID, jint ConnectionMask, jboolean WaitForConnection)
{
   jint                   Result;
   BD_ADDR_t              RemoteDeviceAddress;
   LocalData_t           *LocalData;
   unsigned int           ConnectionStatus;
   unsigned long          ConnectionFlags;
   MAPM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ConnectionType = connectionClient == com_stonestreetone_bluetopiapm_MAPM_CONNECTION_CLIENT_MESSAGE_ACCESS ? mctMessageAccessClient : mctNotificationClient;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ConnectionFlags  = 0;
   ConnectionFlags  = ((ConnectionMask & com_stonestreetone_bluetopiapm_MAPM_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? MAPM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_AUTHENTICATION :0);
   ConnectionFlags |= ((ConnectionMask & com_stonestreetone_bluetopiapm_MAPM_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? MAPM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_ENCRYPTION : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = MAPM_Connect_Remote_Device(ConnectionType, RemoteDeviceAddress, RemoteServerPort, InstanceID, ConnectionFlags, MAPM_EventCallback, LocalData->WeakObjectReference, 0);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }
   else
   {
      Result = MAPM_Connect_Remote_Device(ConnectionType, RemoteDeviceAddress, RemoteServerPort, InstanceID, ConnectionFlags, 0, 0, &ConnectionStatus);

      if(Result == 0)
         Result = ConnectionStatus;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    disconnectNative
 * Signature: (IBBBBBBI)I
 */
static jint DisconnectNative(JNIEnv *Env, jobject Object, jint ConnectionTypeConstant, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID)
{
   jint                   Result;
   BD_ADDR_t              RemoteDeviceAddress;
   MAPM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ConnectionType = mctMessageAccessClient;

   switch(ConnectionTypeConstant)
   {
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_SERVER:
         ConnectionType = mctNotificationServer;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_CLIENT:
         ConnectionType = mctNotificationClient;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_SERVER:
         ConnectionType = mctMessageAccessServer;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT:
         ConnectionType = mctMessageAccessClient;
         break;
   }

   Result = MAPM_Disconnect(ConnectionType, RemoteDeviceAddress, InstanceID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    abortNative
 * Signature: (IBBBBBBI)I
 */
static jint AbortNative(JNIEnv *Env, jobject Object, jint ConnectionTypeConstant, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID)
{
   jint                   Result;
   BD_ADDR_t              RemoteDeviceAddress;
   MAPM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ConnectionType = mctMessageAccessClient;

   switch(ConnectionTypeConstant)
   {
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_SERVER:
         ConnectionType = mctNotificationServer;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_NOTIFICATION_CLIENT:
         ConnectionType = mctNotificationClient;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_SERVER:
         ConnectionType = mctMessageAccessServer;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT:
         ConnectionType = mctMessageAccessClient;
         break;
   }

   Result = MAPM_Abort(ConnectionType, RemoteDeviceAddress, InstanceID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    queryCurrentFolderNative
 * Signature: (BBBBBBII)Ljava/lang/String;
 */
static jstring QueryCurrentFolderNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID)
{
   int                    CurrentFolderResult;
   char                  *CurrentFolderBuffer;
   jstring                CurrentFolderString;
   BD_ADDR_t              RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CurrentFolderResult = MAPM_Query_Current_Folder(RemoteDeviceAddress, InstanceID, 0, NULL);

   if(CurrentFolderResult > 0)
   {
      if((CurrentFolderBuffer = (char *)malloc(CurrentFolderResult + 1)) != NULL)
      {
         CurrentFolderResult = MAPM_Query_Current_Folder(RemoteDeviceAddress, InstanceID, (CurrentFolderResult + 1), CurrentFolderBuffer);

         if(CurrentFolderResult > 0)
         {
            if((CurrentFolderString = Env->NewStringUTF(CurrentFolderBuffer)) == NULL)
               CurrentFolderResult = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         }
         else
         {
            CurrentFolderString = NULL;
         }

         free(CurrentFolderBuffer);
      }
      else
      {
         CurrentFolderResult = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         CurrentFolderString = NULL;
      }
   }
   else
   {
      CurrentFolderString = NULL; 
   }

   if(CurrentFolderResult < 0)
      Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/BluetopiaPMException"), const_cast<char *>(ERR_ConvertErrorCodeToString(CurrentFolderResult)));

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return CurrentFolderString;
}

jint ParseRemoteMessageAccessServicesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jobjectArray ServiceNameArray, jobjectArray InstanceIDArray, jobjectArray ServerPortArray, jobjectArray SupportedMessageTypesArray)
{
   jint                                      Result;
   BD_ADDR_t                                 RemoteDeviceAddress;
   MAPM_Parsed_Message_Access_Service_Info_t ServiceInfo;
   jobjectArray                              ServiceNames;
   jintArray                                 InstanceID;
   jint                                     *InstanceIDPointer;
   jintArray                                 ServerPorts;
   jint                                     *ServerPortsPointer;
   jlongArray                                SupportedMessageTypes;
   jlong                                    *SupportedMessageTypesPointer;
   jstring                                   ServiceName;
   int                                       index;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = MAPM_Parse_Remote_Message_Access_Services(RemoteDeviceAddress, &ServiceInfo);

   if(Result == 0)
   {
      if((ServiceNames = Env->NewObjectArray(ServiceInfo.NumberServices, Env->FindClass("java/lang/String"), NULL)) != NULL)
      {
         if((InstanceID = Env->NewIntArray(ServiceInfo.NumberServices)) != NULL)
         {
            if((InstanceIDPointer = Env->GetIntArrayElements(InstanceID, 0)) != NULL)
            {
               if((ServerPorts = Env->NewIntArray(ServiceInfo.NumberServices)) != NULL)
               {
                  if((ServerPortsPointer = Env->GetIntArrayElements(ServerPorts, 0)) != NULL)
                  {
                     if((SupportedMessageTypes = Env->NewLongArray(ServiceInfo.NumberServices)) != NULL)
                     {
                        if((SupportedMessageTypesPointer = Env->GetLongArrayElements(SupportedMessageTypes, 0)) != NULL)
                        {
                           for(index = 0; index < (int)ServiceInfo.NumberServices; index++)
                           {
                              if(ServiceInfo.ServiceDetails)
                              {
                                 if((ServiceName = Env->NewStringUTF(ServiceInfo.ServiceDetails[index].ServiceName ? ServiceInfo.ServiceDetails[index].ServiceName : "")) != NULL)
                                 {
                                    Env->SetObjectArrayElement(ServiceNames, index, ServiceName);

                                    if(Env->ExceptionCheck())
                                    {
                                       Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                                       break;
                                    }
                                    Env->DeleteLocalRef(ServiceName);
                                 }
                                 else
                                 {
                                    Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                                    break;
                                 }

                                 InstanceIDPointer[index]            = (jint)ServiceInfo.ServiceDetails[index].InstanceID;
                                 ServerPortsPointer[index]           = (jint)ServiceInfo.ServiceDetails[index].ServerPort;
                                 SupportedMessageTypesPointer[index] = (jlong)ServiceInfo.ServiceDetails[index].SupportedMessageTypes;
                              }  
                           }

                           if(!(Result))
                           {
                              Env->SetObjectArrayElement(ServiceNameArray, 0, ServiceNames);
                              if(Env->ExceptionCheck())
                              {
                                 Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                              }
                              else
                              {
                                 Env->SetObjectArrayElement(InstanceIDArray, 0, InstanceID);
                                 if(Env->ExceptionCheck())
                                 {
                                    Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                                 }
                                 else
                                 {
                                    Env->SetObjectArrayElement(ServerPortArray, 0, ServerPorts);
                                    if(Env->ExceptionCheck())
                                    {
                                       Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                                    }
                                    else
                                    {
                                       Env->SetObjectArrayElement(SupportedMessageTypesArray, 0, SupportedMessageTypes);
                                       if(Env->ExceptionCheck())
                                       {
                                          Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                                       }
                                    }
                                 }
                              }
                           }

                           Env->ReleaseLongArrayElements(SupportedMessageTypes,SupportedMessageTypesPointer,0);
                        }

                        Env->DeleteLocalRef(SupportedMessageTypes);
                     }

                     Env->ReleaseIntArrayElements(ServerPorts, ServerPortsPointer, 0);
                  }

                  Env->DeleteLocalRef(ServerPorts);
               }

               Env->ReleaseIntArrayElements(InstanceID, InstanceIDPointer, 0);
            }

            Env->DeleteLocalRef(InstanceID);
         }

         Env->DeleteLocalRef(ServiceNames);
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
   }

   MAPM_Free_Parsed_Message_Access_Service_Info(&ServiceInfo);

   if(Result)
      Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/BluetopiaPMException"), const_cast<char *>(ERR_ConvertErrorCodeToString(Result)));

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    enableNotificationsNative
 * Signature: (BBBBBBIZ)I
 */
jint EnableNotificationsNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID, jboolean Enable)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = MAPM_Enable_Notifications(RemoteDeviceAddress, InstanceID, Enable == JNI_TRUE ? TRUE : FALSE);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    setFolderNative
 * Signature: (BBBBBBIILjava/lang/String;)I
 */
jint SetFolderNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID, jint SetFolderOptionConstant, jstring FolderNameString)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   MAP_Set_Folder_Option_t SetFolderOption;
   const char             *FolderName;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   SetFolderOption = sfRoot;

   switch(SetFolderOptionConstant)
   {
      case com_stonestreetone_bluetopiapm_MAPM_PATH_OPTION_ROOT:
         SetFolderOption = sfRoot;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_PATH_OPTION_DOWN:
         SetFolderOption = sfDown;
         break;
      case com_stonestreetone_bluetopiapm_MAPM_PATH_OPTION_UP:
         SetFolderOption = sfUp;
         break;
   }

   Result = 0;

   if(FolderNameString)
   {
      if((FolderName = Env->GetStringUTFChars(FolderNameString, 0)) == NULL)
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         Result = MAPM_Set_Folder(RemoteDeviceAddress, InstanceID, SetFolderOption, const_cast<char *>(FolderName));

         Env->ReleaseStringUTFChars(FolderNameString, FolderName);
      }
   }
   else
   {
      Result = MAPM_Set_Folder(RemoteDeviceAddress, InstanceID, SetFolderOption, NULL);
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    setFolderAbsoluteNative
 * Signature: (BBBBBBILjava/lang/String;)I
 */
jint SetFolderAbsoluteNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID, jstring AbsolutePathString)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   const char             *AbsolutePath;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if(AbsolutePathString)
   {
      Result = 0;

      if((AbsolutePath = Env->GetStringUTFChars(AbsolutePathString, 0)) == NULL)
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         Result = MAPM_Set_Folder_Absolute(RemoteDeviceAddress, InstanceID, const_cast<char *>(AbsolutePath));

         Env->ReleaseStringUTFChars(AbsolutePathString, AbsolutePath);
      }
   }
   else
   {
      Result = MAPM_Set_Folder_Absolute(RemoteDeviceAddress, InstanceID, NULL);
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    getFolderListingNative
 * Signature: (BBBBBBIII)I
 */
jint GetFolderListingNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID, jint MaxListCount, jint ListStartOffset)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = MAPM_Get_Folder_Listing(RemoteDeviceAddress, InstanceID, MaxListCount, ListStartOffset);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    getFolderListingSizeNative
 * Signature: (BBBBBBI)I
 */
jint GetFolderListingSizeNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = MAPM_Get_Folder_Listing_Size(RemoteDeviceAddress, InstanceID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    getMessageListingNative
 * Signature: ([BILjava/lang/String;IIISJS[I[ISLjava/lang/String;Ljava/lang/String;S)I
 */
jint GetMessageListingNative(JNIEnv *Env, jobject Object, jbyteArray AddressArray, jint InstanceID, jstring FolderNameString, jint MaxListCount, jint ListStartOffset, jint OptionMask, jshort SubjectLength, jlong ParameterMask, jshort FilterMessageType, jintArray FilterPeriodBeginArray, jintArray FilterPeriodEndArray, jshort FilterReadStatus, jstring FilterRecipientString, jstring FilterOriginatorString, jshort FilterPriority)
{
   jint                        Result;
   BD_ADDR_t                   RemoteDeviceAddress;
   jbyte                      *Address;
   char const                 *FolderName;
   MAP_Message_Listing_Info_t *MessageListingInfo;
   jint                       *FilterPeriodBegin;
   jint                       *FilterPeriodEnd;
   char const                 *FilterRecipient;
   char const                 *FilterOriginator;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(AddressArray)
   {
      if((Address = Env->GetByteArrayElements(AddressArray, NULL)) != NULL)
      {
         ASSIGN_BD_ADDR(RemoteDeviceAddress, Address[0], Address[1], Address[2], Address[3], Address[4], Address[5]);
         Env->ReleaseByteArrayElements(AddressArray, Address, 0);

         Result = 0;

         if(FolderNameString)
         {
            if((FolderName = Env->GetStringUTFChars(FolderNameString, NULL)) == NULL)
               Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         }
         else
         {
            FolderName = NULL;
         }

         if((OptionMask) && (!(Result)))
         {
            if(FilterRecipientString)
            {
               if((FilterRecipient = Env->GetStringUTFChars(FilterRecipientString, NULL)) == NULL)
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            }
            else
            {
               FilterRecipient = NULL;
            }

            if((FilterOriginatorString) && (!(Result)))
            {
               if((FilterOriginator = Env->GetStringUTFChars(FilterOriginatorString, NULL)) == NULL)
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            }
            else
            {
               FilterOriginator = NULL;
            }

            if(!(Result))
            {
               if((MessageListingInfo = (MAP_Message_Listing_Info_t *)malloc(MAP_MESSAGE_LISTING_INFO_DATA_SIZE(sizeof(FilterRecipient), sizeof(FilterOriginator)))) == NULL)
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            }

            if(!(Result))
            {
               MessageListingInfo->OptionMask        = OptionMask;
               MessageListingInfo->SubjectLength     = SubjectLength;
               MessageListingInfo->ParameterMask     = ParameterMask;
               MessageListingInfo->FilterMessageType = FilterMessageType;
            }

            if((FilterPeriodBeginArray) && (!(Result)))
            {
               if((FilterPeriodBegin = Env->GetIntArrayElements(FilterPeriodBeginArray, NULL)) == NULL)
               {  
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               }
               else
               {
                  MessageListingInfo->FilterPeriodBegin.Year       = FilterPeriodBegin[0];
                  MessageListingInfo->FilterPeriodBegin.Month      = FilterPeriodBegin[1];
                  MessageListingInfo->FilterPeriodBegin.Day        = FilterPeriodBegin[2];
                  MessageListingInfo->FilterPeriodBegin.Hour       = FilterPeriodBegin[3];
                  MessageListingInfo->FilterPeriodBegin.Minute     = FilterPeriodBegin[4];
                  MessageListingInfo->FilterPeriodBegin.Second     = FilterPeriodBegin[5];
                  MessageListingInfo->FilterPeriodBegin.UTC_Time   = FilterPeriodBegin[6];
                  MessageListingInfo->FilterPeriodBegin.UTC_Offset = FilterPeriodBegin[7];

                  Env->ReleaseIntArrayElements(FilterPeriodBeginArray, FilterPeriodBegin, 0);
               }
            }

            if((FilterPeriodEndArray) && (!(Result)))
            {
               if((FilterPeriodEnd = Env->GetIntArrayElements(FilterPeriodEndArray, NULL)) == NULL)
               {
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               }
               else
               {
                  MessageListingInfo->FilterPeriodEnd.Year       = FilterPeriodEnd[0];
                  MessageListingInfo->FilterPeriodEnd.Month      = FilterPeriodEnd[1];
                  MessageListingInfo->FilterPeriodEnd.Day        = FilterPeriodEnd[2];
                  MessageListingInfo->FilterPeriodEnd.Hour       = FilterPeriodEnd[3];
                  MessageListingInfo->FilterPeriodEnd.Minute     = FilterPeriodEnd[4];
                  MessageListingInfo->FilterPeriodEnd.Second     = FilterPeriodEnd[5];
                  MessageListingInfo->FilterPeriodEnd.UTC_Time   = FilterPeriodEnd[6];
                  MessageListingInfo->FilterPeriodEnd.UTC_Offset = FilterPeriodEnd[7];

                  Env->ReleaseIntArrayElements(FilterPeriodEndArray, FilterPeriodEnd, 0);
               }
            }

            if(!(Result))
               MessageListingInfo->FilterReadStatus = FilterReadStatus;

            if((FilterRecipient) && (!(Result)))
            {
               MessageListingInfo->FilterRecipient = const_cast<char *>(FilterRecipient);
               Env->ReleaseStringUTFChars(FilterRecipientString, FilterRecipient);
            }

            if((FilterOriginator) && (!(Result)))
            {
               MessageListingInfo->FilterOriginator = const_cast<char *>(FilterOriginator);
               Env->ReleaseStringUTFChars(FilterOriginatorString, FilterOriginator);
            }

            if(!(Result))
               MessageListingInfo->FilterPriority = FilterPriority;
         }
         else
         {
            MessageListingInfo = NULL;
         }
      }
      else
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }

      if(!(Result))
         Result = MAPM_Get_Message_Listing(RemoteDeviceAddress, InstanceID, const_cast<char *>(FolderName), MaxListCount, ListStartOffset, MessageListingInfo);

      if(FolderNameString)
         Env->ReleaseStringUTFChars(FolderNameString, FolderName);

      if(MessageListingInfo)
         free(MessageListingInfo);
   }
   else
   {
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    getMessageListingSizeNative
 * Signature: ([BILjava/lang/String;IS[I[ISLjava/lang/String;Ljava/lang/String;S)I
 */
jint GetMessageListingSizeNative(JNIEnv *Env, jobject Object, jbyteArray AddressArray, jint InstanceID, jstring FolderNameString, jint OptionMask, jshort FilterMessageType, jintArray FilterPeriodBeginArray, jintArray FilterPeriodEndArray, jshort FilterReadStatus, jstring FilterRecipientString, jstring FilterOriginatorString, jshort FilterPriority)
{
   jint                        Result;
   BD_ADDR_t                   RemoteDeviceAddress;
   jbyte                      *Address;
   char const                 *FolderName;
   MAP_Message_Listing_Info_t *MessageListingInfo;
   jint                       *FilterPeriodBegin;
   jint                       *FilterPeriodEnd;
   char const                 *FilterRecipient;
   char const                 *FilterOriginator;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(AddressArray)
   {
      FolderName         = NULL;
      MessageListingInfo = NULL;

      if((Address = Env->GetByteArrayElements(AddressArray, NULL)) != NULL)
      {
         ASSIGN_BD_ADDR(RemoteDeviceAddress, Address[0], Address[1], Address[2], Address[3], Address[4], Address[5]);
         Env->ReleaseByteArrayElements(AddressArray, Address, 0);

         Result = 0;

         if(FolderNameString)
         {
            if((FolderName = Env->GetStringUTFChars(FolderNameString, NULL)) == NULL)
               Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         }
         else
         {
            FolderName = NULL;
         }

         if((OptionMask) && (!(Result)))
         {
            if(FilterRecipientString)
            {
               if((FilterRecipient = Env->GetStringUTFChars(FilterRecipientString, 0)) == NULL)
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            }
            else
               FilterRecipient = NULL;

            if((FilterOriginatorString) && (!(Result)))
            {
               if((FilterOriginator = Env->GetStringUTFChars(FilterOriginatorString, 0)) == NULL)
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            }
            else
               FilterOriginator = NULL;

            if(!(Result))
            {
               if((MessageListingInfo = (MAP_Message_Listing_Info_t *)malloc(MAP_MESSAGE_LISTING_INFO_DATA_SIZE(sizeof(FilterRecipient), sizeof(FilterOriginator)))) == NULL)
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            }

            if(!(Result))
            {
               MessageListingInfo->OptionMask        = OptionMask;
               MessageListingInfo->FilterMessageType = FilterMessageType;
            }

            if((FilterPeriodBeginArray) && (!(Result)))
            {
               if((FilterPeriodBegin = Env->GetIntArrayElements(FilterPeriodBeginArray, NULL)) == NULL)
               {  
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               }
               else
               {
                  MessageListingInfo->FilterPeriodBegin.Year       = FilterPeriodBegin[0];
                  MessageListingInfo->FilterPeriodBegin.Month      = FilterPeriodBegin[1];
                  MessageListingInfo->FilterPeriodBegin.Day        = FilterPeriodBegin[2];
                  MessageListingInfo->FilterPeriodBegin.Hour       = FilterPeriodBegin[3];
                  MessageListingInfo->FilterPeriodBegin.Minute     = FilterPeriodBegin[4];
                  MessageListingInfo->FilterPeriodBegin.Second     = FilterPeriodBegin[5];
                  MessageListingInfo->FilterPeriodBegin.UTC_Time   = FilterPeriodBegin[6];
                  MessageListingInfo->FilterPeriodBegin.UTC_Offset = FilterPeriodBegin[7];

                  Env->ReleaseIntArrayElements(FilterPeriodBeginArray, FilterPeriodBegin, 0);
               }
            }

            if((FilterPeriodEndArray) && (!(Result)))
            {
               if((FilterPeriodEnd = Env->GetIntArrayElements(FilterPeriodEndArray, NULL)) == NULL)
               {
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               }
               else
               {
                  MessageListingInfo->FilterPeriodEnd.Year       = FilterPeriodEnd[0];
                  MessageListingInfo->FilterPeriodEnd.Month      = FilterPeriodEnd[1];
                  MessageListingInfo->FilterPeriodEnd.Day        = FilterPeriodEnd[2];
                  MessageListingInfo->FilterPeriodEnd.Hour       = FilterPeriodEnd[3];
                  MessageListingInfo->FilterPeriodEnd.Minute     = FilterPeriodEnd[4];
                  MessageListingInfo->FilterPeriodEnd.Second     = FilterPeriodEnd[5];
                  MessageListingInfo->FilterPeriodEnd.UTC_Time   = FilterPeriodEnd[6];
                  MessageListingInfo->FilterPeriodEnd.UTC_Offset = FilterPeriodEnd[7];

                  Env->ReleaseIntArrayElements(FilterPeriodEndArray, FilterPeriodEnd, 0);
               }
            }

            if(!(Result))
               MessageListingInfo->FilterReadStatus = FilterReadStatus;

            if((FilterRecipient) && (!(Result)))
            {
               MessageListingInfo->FilterRecipient = const_cast<char *>(FilterRecipient);
               Env->ReleaseStringUTFChars(FilterRecipientString, FilterRecipient);
            }

            if((FilterOriginator) && (!(Result)))
            {
               MessageListingInfo->FilterOriginator = const_cast<char *>(FilterOriginator);
               Env->ReleaseStringUTFChars(FilterOriginatorString, FilterOriginator);
            }

            if(!(Result))
               MessageListingInfo->FilterPriority = FilterPriority;
         }
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

      if(!(Result))
         Result = MAPM_Get_Message_Listing_Size(RemoteDeviceAddress, InstanceID, const_cast<char *>(FolderName), MessageListingInfo);

      if((FolderNameString) && (FolderName))
         Env->ReleaseStringUTFChars(FolderNameString, FolderName);

      if(MessageListingInfo)
         free(MessageListingInfo);
   }
   else
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    getMessageNative
 * Signature: (BBBBBBILjava/lang/String;ZII)I
 */
jint GetMessageNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID, jstring MessageHandleString, jboolean Attachment, jint CharSetConstant, jint FractionRequestConstant)
{
   jint                  Result;
   BD_ADDR_t             RemoteDeviceAddress;
   char const           *MessageHandle;
   MAP_CharSet_t         CharSet;
   MAP_Fractional_Type_t FractionalType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if(MessageHandleString)
   {
      Result= 0;

      if((MessageHandle = Env->GetStringUTFChars(MessageHandleString, NULL)) == NULL)
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         CharSet = CharSetConstant == com_stonestreetone_bluetopiapm_MAPM_CHARACTER_SET_NATIVE ? csNative : csUTF8;

         FractionalType = ftFirst;

         if(FractionRequestConstant)
         {
            FractionalType = ((FractionRequestConstant == com_stonestreetone_bluetopiapm_MAPM_FRACTIONAL_TYPE_NEXT) ? ftNext : ftFirst);
         }

         Result = MAPM_Get_Message(RemoteDeviceAddress, InstanceID, const_cast<char *>(MessageHandle), Attachment == JNI_TRUE ? TRUE : FALSE, CharSet, FractionalType);

         Env->ReleaseStringUTFChars(MessageHandleString, MessageHandle);
      }
   }
   else
   {
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    setMessageStatusNative
 * Signature: (BBBBBBILjava/lang/String;IZ)I
 */
jint SetMessageStatusNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID, jstring MessageHandleString, jint StatusIndicatorConstant, jboolean StatusValue)
{
   jint                   Result;
   BD_ADDR_t              RemoteDeviceAddress;
   char const            *MessageHandle;
   MAP_Status_Indicator_t StatusIndicator;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if(MessageHandleString)
   {
      Result = 0;

      if((MessageHandle = Env->GetStringUTFChars(MessageHandleString, NULL)) == NULL)
      {
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         StatusIndicator = StatusIndicatorConstant == com_stonestreetone_bluetopiapm_MAPM_STATUS_INDICATOR_READ_STATUS ? siReadStatus : siDeletedStatus;

         Result = MAPM_Set_Message_Status(RemoteDeviceAddress, InstanceID, const_cast<char *>(MessageHandle), StatusIndicator, StatusValue == JNI_TRUE ? TRUE : FALSE);

         Env->ReleaseStringUTFChars(MessageHandleString, MessageHandle);
      }
   }
   else
   {
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    pushMessageNative
 * Signature: (BBBBBBILjava/lang/String;ZZI[BZ)I
 */
jint PushMessageNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID, jstring FolderNameString, jboolean Transparent, jboolean Retry, jint CharSetConstant, jbyteArray MessageBufferArray, jboolean IsFinal)
{
   jint          Result;
   BD_ADDR_t     RemoteDeviceAddress;
   char const   *FolderName;
   MAP_CharSet_t CharSet;
   jbyte        *MessageBuffer;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if(MessageBufferArray)
   {
      Result = 0;

      if((MessageBuffer = Env->GetByteArrayElements(MessageBufferArray, NULL)) == NULL)
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

      if((FolderNameString) && (!(Result)))
      {
         if((FolderName = Env->GetStringUTFChars(FolderNameString, 0)) == NULL)
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         FolderName = NULL;
      }

      if(!(Result))
      {
         CharSet = CharSetConstant == com_stonestreetone_bluetopiapm_MAPM_CHARACTER_SET_NATIVE ? csNative : csUTF8;

         Result = MAPM_Push_Message(RemoteDeviceAddress, InstanceID, const_cast<char *>(FolderName), Transparent == JNI_TRUE ? TRUE : FALSE, Retry == JNI_TRUE ? TRUE : FALSE, CharSet, sizeof(MessageBuffer), (Byte_t *)MessageBuffer, IsFinal == JNI_TRUE ? TRUE : FALSE);

         if(FolderNameString)
            Env->ReleaseStringUTFChars(FolderNameString, FolderName);
      }

      Env->ReleaseByteArrayElements(MessageBufferArray, MessageBuffer, 0);
   }
   else
   {
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

/*
 * Class:     com_stonestreetone_bluetopiapm_MAPM
 * Method:    updateInboxNative
 * Signature: (BBBBBBI)I
 */
jint UpdateInboxNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint InstanceID)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = MAPM_Update_Inbox(RemoteDeviceAddress, InstanceID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;   
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                                      "()V",                                                                          (void*) InitClassNative},
   {"initObjectNative",                                     "()V",                                                                          (void*) InitObjectNative},
   {"cleanupObjectNative",                                  "()V",                                                                          (void*) CleanupObjectNative},
   {"connectRemoteDeviceNative",                            "(IBBBBBBIIIZ)I",                                                               (void*) ConnectRemoteDeviceNative},
   {"disconnectNative",                                     "(IBBBBBBI)I",                                                                  (void*) DisconnectNative},
   {"queryCurrentFolderNative",                             "(BBBBBBI)Ljava/lang/String;",                                                  (void*) QueryCurrentFolderNative},
   {"parseRemoteMessageAccessServicesNative",               "(BBBBBB[[Ljava/lang/String;[[I[[I[[J)I",                                       (void*) ParseRemoteMessageAccessServicesNative},
   {"abortNative",                                          "(IBBBBBBI)I",                                                                  (void*) AbortNative},
   {"enableNotificationsNative",                            "(BBBBBBIZ)I",                                                                  (void*) EnableNotificationsNative},
   {"setFolderNative",                                      "(BBBBBBIILjava/lang/String;)I",                                                (void*) SetFolderNative},
   {"setFolderAbsoluteNative",                              "(BBBBBBILjava/lang/String;)I",                                                 (void*) SetFolderAbsoluteNative},
   {"getFolderListingNative",                               "(BBBBBBIII)I",                                                                 (void*) GetFolderListingNative},
   {"getFolderListingSizeNative",                           "(BBBBBBI)I",                                                                   (void*) GetFolderListingSizeNative},
   {"getMessageListingNative",                              "([BILjava/lang/String;IIISJS[I[ISLjava/lang/String;Ljava/lang/String;S)I",     (void*) GetMessageListingNative},
   {"getMessageListingSizeNative",                          "([BILjava/lang/String;IS[I[ISLjava/lang/String;Ljava/lang/String;S)I",         (void*) GetMessageListingSizeNative},
   {"getMessageNative",                                     "(BBBBBBILjava/lang/String;ZII)I",                                              (void*) GetMessageNative},
   {"setMessageStatusNative",                               "(BBBBBBILjava/lang/String;IZ)I",                                               (void*) SetMessageStatusNative},
   {"pushMessageNative",                                    "(BBBBBBILjava/lang/String;ZZI[BZ)I",                                           (void*) PushMessageNative},
   {"updateInboxNative",                                    "(BBBBBBI)I",                                                                   (void*) UpdateInboxNative}
};

int register_com_stonestreetone_bluetopiapm_MAPM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/MAPM";

   Result = -1;

   PRINT_DEBUG("Registering MAPM native functions");

   if((Class = Env->FindClass(ClassName)) != NULL)
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));

      return Result;
}
