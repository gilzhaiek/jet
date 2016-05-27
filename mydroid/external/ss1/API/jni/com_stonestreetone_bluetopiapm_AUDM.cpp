/*****< com_stonestreetone_bluetopiapm_AUDM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_AUDM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager AUDM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   01/28/11  G. Hensley     Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>

extern "C" {
#include "SS1BTAUDM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_AUDM.h"

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
static WEAK_REF Class_AUDM;

static jfieldID Field_AUDM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_AUDM_incomingConnectionRequestEvent;
static jmethodID Method_AUDM_audioStreamConnectedEvent;
static jmethodID Method_AUDM_audioStreamConnectionStatusEvent;
static jmethodID Method_AUDM_audioStreamDisconnectedEvent;
static jmethodID Method_AUDM_audioStreamStateChangedEvent;
static jmethodID Method_AUDM_changeAudioStreamStateStatusEvent;
static jmethodID Method_AUDM_audioStreamFormatChangedEvent;
static jmethodID Method_AUDM_changeAudioStreamFormatStatusEvent;
static jmethodID Method_AUDM_remoteControlConnectedEvent;
static jmethodID Method_AUDM_remoteControlConnectionStatusEvent;
static jmethodID Method_AUDM_remoteControlDisconnectedEvent;
static jmethodID Method_AUDM_encodedAudioStreamDataEvent;
static jmethodID Method_AUDM_remoteControlPassThroughCommandIndicationEvent;
static jmethodID Method_AUDM_remoteControlPassThroughCommandConfirmationEvent;
static jmethodID Method_AUDM_vendorDependentCommandIndicationEvent;
static jmethodID Method_AUDM_getCapabilitiesCommandIndicationEvent;
static jmethodID Method_AUDM_getElementAttributesCommandIndicationEvent;
static jmethodID Method_AUDM_getPlayStatusCommandIndicationEvent;
static jmethodID Method_AUDM_registerNotificationCommandIndicationEvent;
static jmethodID Method_AUDM_setAbsoluteVolumeCommandIndicationEvent;
static jmethodID Method_AUDM_vendorDependentCommandConfirmationEvent;
static jmethodID Method_AUDM_getCompanyIDCapabilitiesCommandConfirmationEvent;
static jmethodID Method_AUDM_getEventsSupportedCapabilitiesCommandConfirmationEvent;
static jmethodID Method_AUDM_getElementAttributesCommandConfirmationEvent;
static jmethodID Method_AUDM_getPlayStatusCommandConfirmationEvent;
static jmethodID Method_AUDM_playbackStatusChangedNotificationEvent;
static jmethodID Method_AUDM_trackChangedNotificationEvent;
static jmethodID Method_AUDM_trackReachedEndNotificationEvent;
static jmethodID Method_AUDM_trackReachedStartNotificationEvent;
static jmethodID Method_AUDM_playbackPositionChangedNotificationEvent;
static jmethodID Method_AUDM_systemStatusChangedNotificationEvent;
static jmethodID Method_AUDM_volumeChangeNotificationEvent;
static jmethodID Method_AUDM_setAbsoluteVolumeCommandConfirmationEvent;
static jmethodID Method_AUDM_getCapabilitiesRejectedEvent;
static jmethodID Method_AUDM_registerNotificationRejectedEvent;

typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
   unsigned int DataCallbackID;
   unsigned int ControllerCallbackID;
   unsigned int TargetCallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_AUDM_localData, Exclusive)) == NULL)
      PRINT_ERROR("AUDM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_AUDM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_AUDM_localData, LocalData);
}

static void HandleGetElementAttributesResponse(JNIEnv *Env, jobject AUDMObject, jint ResponseCode, BD_ADDR_t BD_ADDR, jbyte Transaction, jint Status, AVRCP_Get_Element_Attributes_Response_Data_t *GetElementAttributesResponseData)
{
   jint         Index;
   jint         IntVal;
   jint         CSet;
   Boolean_t    Error = FALSE;
   jintArray    AttributeIDs  = NULL;
   jintArray    CharacterSets = NULL;
   jbyteArray   DataElement;
   jobjectArray DataArray     = NULL;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(((AttributeIDs = Env->NewIntArray(GetElementAttributesResponseData->NumberAttributes)) != NULL) && ((CharacterSets = Env->NewIntArray(GetElementAttributesResponseData->NumberAttributes)) != NULL) && ((DataArray = Env->NewObjectArray(GetElementAttributesResponseData->NumberAttributes, Env->FindClass("[B"), NULL)) != NULL))
   {
      for(Index=0;Index<GetElementAttributesResponseData->NumberAttributes && !Error;Index++)
      {
         if((DataElement = Env->NewByteArray(GetElementAttributesResponseData->AttributeList[Index].AttributeValueLength)) != NULL)
         {
            switch(GetElementAttributesResponseData->AttributeList[Index].AttributeID)
            {
               case AVRCP_MEDIA_ATTRIBUTE_ID_TITLE_OF_MEDIA:
               default:
                  IntVal = com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_TITLE;
                  break;
               case AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ARTIST:
                  IntVal = com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_ARTIST;
                  break;
               case AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ALBUM:
                  IntVal = com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_ALBUM;
                  break;
               case AVRCP_MEDIA_ATTRIBUTE_ID_NUMBER_OF_MEDIA:
                  IntVal = com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA;
                  break;
               case AVRCP_MEDIA_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA:
                  IntVal = com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA;
                  break;
               case AVRCP_MEDIA_ATTRIBUTE_ID_GENRE:
                  IntVal = com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_GENRE;
                  break;
               case AVRCP_MEDIA_ATTRIBUTE_ID_PLAYING_TIME_MS:
                  IntVal = com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_PLAYING_TIME;
                  break;
            }

            Env->SetByteArrayRegion(DataElement, 0, GetElementAttributesResponseData->AttributeList[Index].AttributeValueLength, (jbyte *)GetElementAttributesResponseData->AttributeList[Index].AttributeValueData);
            Env->SetObjectArrayElement(DataArray, Index, DataElement);
            Env->SetIntArrayRegion(AttributeIDs, Index, 1, &IntVal);
            CSet = (jint)GetElementAttributesResponseData->AttributeList[Index].CharacterSet;
            Env->SetIntArrayRegion(CharacterSets, Index, 1, &CSet);

            Env->DeleteLocalRef(DataElement);
         }
         else
         {
            Error      = TRUE;
         }

      }
      if(!Error)
      {
         Env->CallVoidMethod(AUDMObject, Method_AUDM_getElementAttributesCommandConfirmationEvent,
              BD_ADDR.BD_ADDR5,
              BD_ADDR.BD_ADDR4,
              BD_ADDR.BD_ADDR3,
              BD_ADDR.BD_ADDR2,
              BD_ADDR.BD_ADDR1,
              BD_ADDR.BD_ADDR0,
              Transaction,
              Status,
              ResponseCode,
              AttributeIDs,
              CharacterSets,
              DataArray
              );
      }
   }

   if(AttributeIDs)
      Env->DeleteLocalRef(AttributeIDs);

   if(CharacterSets)
      Env->DeleteLocalRef(CharacterSets);

   if(DataArray)
      Env->DeleteLocalRef(DataArray);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

static void HandleCommandRejectResponse(JNIEnv *Env, jobject AUDMObject, BD_ADDR_t BD_ADDR, jbyte TransactionID, jint Status, AVRCP_Message_Type_t MessageType)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);
  
   switch(MessageType)
   {
      case amtPassThrough:
         Env->CallVoidMethod(AUDMObject, Method_AUDM_remoteControlPassThroughCommandConfirmationEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               Status,
               com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED);
         break;
      case amtGetCapabilities:
         Env->CallVoidMethod(AUDMObject, Method_AUDM_getCapabilitiesRejectedEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               Status);
         break;
      case amtVendorDependent_Generic:
         Env->CallVoidMethod(AUDMObject, Method_AUDM_vendorDependentCommandConfirmationEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               Status,
               com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED,
               0,
               0,
               0,
               0,
               0,
               NULL);
         break;
      case amtGetElementAttributes:
         Env->CallVoidMethod(AUDMObject, Method_AUDM_getElementAttributesCommandConfirmationEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               Status,
               com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED,
               NULL,
               NULL,
               NULL);
         break;
      case amtGetPlayStatus:
         Env->CallVoidMethod(AUDMObject, Method_AUDM_getPlayStatusCommandConfirmationEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               Status,
               com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED,
               0,
               0,
               0);
         break;
      case amtRegisterNotification:
         Env->CallVoidMethod(AUDMObject, Method_AUDM_registerNotificationRejectedEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               Status);
         break;
      case amtSetAbsoluteVolume:
         Env->CallVoidMethod(AUDMObject, Method_AUDM_setAbsoluteVolumeCommandConfirmationEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               Status,
               com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED,
               0);
         break;
      case amtUnknown:
      case amtUnitInfo:
      case amtSubunitInfo:
      case amtBrowsingChannel_Generic:
      case amtFragmentedMessage:
      case amtGroupNavigation:
      case amtListPlayerApplicationSettingAttributes:
      case amtListPlayerApplicationSettingValues:
      case amtGetCurrentPlayerApplicationSettingValue:
      case amtSetPlayerApplicationSettingValue:
      case amtGetPlayerApplicationSettingAttributeText:
      case amtGetPlayerApplicationSettingValueText:
      case amtInformDisplayableCharacterSet:
      case amtInformBatteryStatusOfCT:
      case amtRequestContinuingResponse:
      case amtAbortContinuingResponse:
      case amtCommandRejectResponse:
      case amtSetAddressedPlayer:
      case amtPlayItem:
      case amtAddToNowPlaying:
      case amtSetBrowsedPlayer:
      case amtChangePath:
      case amtGetItemAttributes:
      case amtSearch:
      case amtGetFolderItems:
      case amtGeneralReject:
      default:
         break;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Event callback handler.
 */
static void AUDM_EventCallback(AUDM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         StreamType;
   jint         IntVal1;
   jint         IntVal2;
   jint         IntVal3;
   jint         IntVal4;
   jsize        Index;
   JNIEnv      *Env;
   jobject      AUDMObject;
   jstring      String;
   jmethodID    Method;
   Boolean_t    Valid;
   BD_ADDR_t   *RemoteDevice;
   jbyteArray   ByteArray;
   jbyte       *ByteArrayData;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our AUDM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((AUDMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, AUDMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case aetIncomingConnectionRequest:
                           switch(EventData->EventData.IncomingConnectionRequestEventData.RequestType)
                           {
                              case acrStream:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_REQUEST_TYPE_STREAM;
                                 break;
                              case acrRemoteControl:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_REQUEST_TYPE_REMOTE_CONTROL;
                                 break;
                           }

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_incomingConnectionRequestEvent,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1);
                           break;
                        case aetAudioStreamConnected:
                           switch(EventData->EventData.AudioStreamConnectedEventData.StreamType)
                           {
                              case astSNK:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK;
                                 break;
                              case astSRC:
                              default:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC;
                                 break;
                           }

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_audioStreamConnectedEvent,
                                 StreamType,
                                 EventData->EventData.AudioStreamConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AudioStreamConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AudioStreamConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AudioStreamConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AudioStreamConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AudioStreamConnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.AudioStreamConnectedEventData.MediaMTU,
                                 (jlong)(EventData->EventData.AudioStreamConnectedEventData.StreamFormat.SampleFrequency),
                                 (jint)(EventData->EventData.AudioStreamConnectedEventData.StreamFormat.NumberChannels),
                                 (jint)(EventData->EventData.AudioStreamConnectedEventData.StreamFormat.FormatFlags));
                           break;
                        case aetAudioStreamConnectionStatus:
                           switch(EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus)
                           {
                              case AUDM_STREAM_CONNECTION_STATUS_SUCCESS:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_SUCCESS;
                                 break;
                              case AUDM_STREAM_CONNECTION_STATUS_FAILURE_TIMEOUT:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_TIMEOUT;
                                 break;
                              case AUDM_STREAM_CONNECTION_STATUS_FAILURE_REFUSED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_REFUSED;
                                 break;
                              case AUDM_STREAM_CONNECTION_STATUS_FAILURE_SECURITY:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_SECURITY;
                                 break;
                              case AUDM_STREAM_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF;
                                 break;
                              case AUDM_STREAM_CONNECTION_STATUS_FAILURE_UNKNOWN:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_UNKNOWN;
                                 break;
                           }

                           switch(EventData->EventData.AudioStreamConnectionStatusEventData.StreamType)
                           {
                              case astSNK:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK;
                                 break;
                              case astSRC:
                              default:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC;
                                 break;
                           }

                           if(EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus == AUDM_STREAM_CONNECTION_STATUS_SUCCESS)
                           {
                              Env->CallVoidMethod(AUDMObject, Method_AUDM_audioStreamConnectionStatusEvent,
                                    IntVal1,
                                    EventData->EventData.AudioStreamConnectionStatusEventData.MediaMTU,
                                    StreamType,
                                    (jlong)(EventData->EventData.AudioStreamConnectionStatusEventData.StreamFormat.SampleFrequency),
                                    (jint)(EventData->EventData.AudioStreamConnectionStatusEventData.StreamFormat.NumberChannels));
                           }
                           else
                           {
                              Env->CallVoidMethod(AUDMObject, Method_AUDM_audioStreamConnectionStatusEvent,
                                    IntVal1,
                                    EventData->EventData.AudioStreamConnectionStatusEventData.MediaMTU,
                                    StreamType,
                                    (jlong)(0),
                                    (jint)(0));
                           }
                           break;
                        case aetAudioStreamDisconnected:
                           switch(EventData->EventData.AudioStreamDisconnectedEventData.StreamType)
                           {
                              case astSNK:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK;
                                 break;
                              case astSRC:
                              default:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC;
                                 break;
                           }

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_audioStreamDisconnectedEvent, StreamType);
                           break;
                        case aetAudioStreamStateChanged:
                           switch(EventData->EventData.AudioStreamStateChangedEventData.StreamType)
                           {
                              case astSNK:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK;
                                 break;
                              case astSRC:
                              default:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC;
                                 break;
                           }

                           switch(EventData->EventData.AudioStreamStateChangedEventData.StreamState)
                           {
                              case astStreamStarted:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STARTED;
                                 break;
                              case astStreamStopped:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STOPPED;
                                 break;
                           }

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_audioStreamStateChangedEvent, StreamType, IntVal1);
                           break;
                        case aetChangeAudioStreamStateStatus:
                           switch(EventData->EventData.ChangeAudioStreamStateStatusEventData.StreamType)
                           {
                              case astSNK:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK;
                                 break;
                              case astSRC:
                              default:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC;
                                 break;
                           }

                           switch(EventData->EventData.ChangeAudioStreamStateStatusEventData.StreamState)
                           {
                              case astStreamStarted:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STARTED;
                                 break;
                              case astStreamStopped:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STOPPED;
                                 break;
                           }

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_changeAudioStreamStateStatusEvent,
                                 ((EventData->EventData.ChangeAudioStreamStateStatusEventData.Successful == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 StreamType,
                                 IntVal1);
                           break;
                        case aetAudioStreamFormatChanged:
                           switch(EventData->EventData.AudioStreamFormatChangedEventData.StreamType)
                           {
                              case astSNK:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK;
                                 break;
                              case astSRC:
                              default:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC;
                                 break;
                           }

                           // TODO: If FormatFlags ever becomes meaningful, treat like a flag field where we translate from BTPMS/BTPM constants to BTPMJ constants

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_audioStreamFormatChangedEvent,
                                 StreamType,
                                 (jlong)(EventData->EventData.AudioStreamFormatChangedEventData.StreamFormat.SampleFrequency),
                                 (jint)(EventData->EventData.AudioStreamFormatChangedEventData.StreamFormat.NumberChannels),
                                 (jint)(EventData->EventData.AudioStreamFormatChangedEventData.StreamFormat.FormatFlags));
                           break;
                        case aetChangeAudioStreamFormatStatus:
                           switch(EventData->EventData.ChangeAudioStreamFormatStatusEventData.StreamType)
                           {
                              case astSNK:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK;
                                 break;
                              case astSRC:
                              default:
                                 StreamType = com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC;
                                 break;
                           }

                           // TODO: If FormatFlags ever becomes meaningful, treat like a flag field where we translate from BTPMS/BTPM constants to BTPMJ constants

                           if(EventData->EventData.ChangeAudioStreamFormatStatusEventData.Successful == FALSE)
                           {
                              Env->CallVoidMethod(AUDMObject, Method_AUDM_changeAudioStreamFormatStatusEvent,
                                    JNI_FALSE,
                                    StreamType,
                                    (jlong)(0),
                                    (jint)(0),
                                    (jint)(0));
                           }
                           else
                           {
                              Env->CallVoidMethod(AUDMObject, Method_AUDM_changeAudioStreamFormatStatusEvent,
                                    JNI_TRUE,
                                    StreamType,
                                    (jlong)(EventData->EventData.ChangeAudioStreamFormatStatusEventData.StreamFormat.SampleFrequency),
                                    (jint)(EventData->EventData.ChangeAudioStreamFormatStatusEventData.StreamFormat.NumberChannels),
                                    (jint)(EventData->EventData.ChangeAudioStreamFormatStatusEventData.StreamFormat.FormatFlags));
                           }
                           break;
                        case aetEncodedAudioStreamData:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.EncodedAudioStreamDataEventData.RawAudioDataFrameLength)) != NULL)
                           {
                              Env->SetByteArrayRegion(ByteArray, 0, (jsize)(EventData->EventData.EncodedAudioStreamDataEventData.RawAudioDataFrameLength), (jbyte *)(EventData->EventData.EncodedAudioStreamDataEventData.RawAudioDataFrame));

                              Env->CallVoidMethod(AUDMObject, Method_AUDM_encodedAudioStreamDataEvent,
                                    EventData->EventData.EncodedAudioStreamDataEventData.StreamDataEventsHandlerID,
                                    ByteArray);

                              Env->DeleteLocalRef(ByteArray);
                           }
                           else
                              Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                           break;
                        case aetRemoteControlConnected:
                           Env->CallVoidMethod(AUDMObject, Method_AUDM_remoteControlConnectedEvent,
                                 EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case aetRemoteControlConnectionStatus:
                           switch(EventData->EventData.RemoteControlConnectionStatusEventData.ConnectionStatus)
                           {
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_SUCCESS:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_SUCCESS;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_TIMEOUT:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_TIMEOUT;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_REFUSED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_REFUSED;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_SECURITY:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_SECURITY;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_UNKNOWN:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CONNECTION_STATUS_FAILURE_UNKNOWN;
                                 break;
                           }

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_remoteControlConnectionStatusEvent,
                                 EventData->EventData.RemoteControlConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteControlConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteControlConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteControlConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteControlConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteControlConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1);
                           break;
                        case aetRemoteControlDisconnected:
                           switch(EventData->EventData.RemoteControlDisconnectedEventData.DisconnectReason)
                           {
                              case adrRemoteDeviceDisconnect:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_REMOTE_CONTROL_DISCONNECT_REASON_DISCONNECT;
                                 break;
                              case adrRemoteDeviceLinkLoss:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_REMOTE_CONTROL_DISCONNECT_REASON_LINK_LOSS;
                                 break;
                              case adrRemoteDeviceTimeout:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AUDM_REMOTE_CONTROL_DISCONNECT_REASON_TIMEOUT;
                                 break;
                           }

                           Env->CallVoidMethod(AUDMObject, Method_AUDM_remoteControlDisconnectedEvent,
                                 EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1);
                           break;
                        case aetRemoteControlCommandIndication:
                           Valid = TRUE;

                           switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageType)
                           {
                              case amtPassThrough:
                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.CommandType)
                                 {
                                    case AVRCP_CTYPE_CONTROL:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_CONTROL;
                                       break;
                                    case AVRCP_CTYPE_STATUS:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_STATUS;
                                       break;
                                    case AVRCP_CTYPE_SPECIFIC_INQUIRY:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_SPECIFIC_INQUIRY;
                                       break;
                                    case AVRCP_CTYPE_NOTIFY:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_NOTIFY;
                                       break;
                                    case AVRCP_CTYPE_GENERAL_INQUIRY:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_GENERAL_INQUIRY;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.SubunitType)
                                 {
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TUNER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_PANEL:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_EXTENDED:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_UNIT:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT;
                                       break;
                                    default:
                                       IntVal2 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.SubunitID)
                                 {
                                    case AVRCP_SUBUNIT_ID_INSTANCE_0:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_1:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_2:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_3:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_4:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4;
                                       break;
                                    case AVRCP_SUBUNIT_ID_EXTENDED:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_ID_IGNORE:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE;
                                       break;
                                    default:
                                       IntVal3 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 if(!Valid) 
                                    break;

                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID)
                                 {
                                    case AVRCP_PASS_THROUGH_ID_SELECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SELECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_LEFT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RIGHT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RIGHT_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RIGHT_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_LEFT_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_LEFT_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_ROOT_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ROOT_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_SETUP_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SETUP_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CONTENTS_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_FAVORITE_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_EXIT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EXIT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_0:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_0;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_1:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_1;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_2:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_2;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_3:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_3;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_4:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_4;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_5:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_5;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_6:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_6;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_7:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_7;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_8:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_8;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_9:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_9;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_DOT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_ENTER:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ENTER;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CLEAR:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CLEAR;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CHANNEL_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_SOUND_SELECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_INPUT_SELECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_HELP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_HELP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PAGE_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PAGE_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_POWER:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_POWER;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_VOLUME_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_VOLUME_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_MUTE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_MUTE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PLAY:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PLAY;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_STOP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_STOP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PAUSE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAUSE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RECORD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RECORD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_REWIND:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_REWIND;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_FAST_FORWARD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_EJECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EJECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_FORWARD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FORWARD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_BACKWARD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_BACKWARD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_ANGLE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ANGLE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_SUBPICTURE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SUBPICTURE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
                                       break;
                                    default:
                                       /* Workaround the fact that the  */
                                       /* following Passthrough IDs were*/
                                       /* all defined to (0xFF, invalid)*/
                                       /* at the time of this writing,  */
                                       /* so we cannot include them in  */
                                       /* the switch block above.       */
                                       if(AVRCP_PASS_THROUGH_ID_F1 == EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID)
                                          IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F1;
                                       else
                                       {
                                          if(AVRCP_PASS_THROUGH_ID_F2 == EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID)
                                             IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F2;
                                          else
                                          {
                                             if(AVRCP_PASS_THROUGH_ID_F3 == EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID)
                                                IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F3;
                                             else
                                             {
                                                if(AVRCP_PASS_THROUGH_ID_F4 == EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID)
                                                   IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F4;
                                                else
                                                {
                                                   if(AVRCP_PASS_THROUGH_ID_F5 == EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID)
                                                      IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F5;
                                                   else
                                                   {
                                                      IntVal4 = 0;
                                                      Valid   = FALSE;
                                                   }
                                                }
                                             }
                                          }
                                       }
                                       break;
                                 }

                                 if(Valid)
                                 {
                                    if((ByteArray = Env->NewByteArray(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationDataLength)) != NULL)
                                    {
                                       Env->SetByteArrayRegion(ByteArray, 0, (jsize)(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationDataLength), (jbyte *)(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationData));

                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_remoteControlPassThroughCommandIndicationEvent,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID),
                                             IntVal1,
                                             IntVal2,
                                             IntVal3,
                                             IntVal4,
                                             ((EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.StateFlag == FALSE) ? JNI_FALSE : JNI_TRUE),
                                             ByteArray);

                                       Env->DeleteLocalRef(ByteArray);
                                    }
                                    else
                                       Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                                 }

                                 break;
                              case amtVendorDependent_Generic:
                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.CommandType)
                                 {
                                    case AVRCP_CTYPE_CONTROL:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_CONTROL;
                                       break;
                                    case AVRCP_CTYPE_STATUS:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_STATUS;
                                       break;
                                    case AVRCP_CTYPE_SPECIFIC_INQUIRY:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_SPECIFIC_INQUIRY;
                                       break;
                                    case AVRCP_CTYPE_NOTIFY:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_NOTIFY;
                                       break;
                                    case AVRCP_CTYPE_GENERAL_INQUIRY:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_GENERAL_INQUIRY;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.SubunitType)
                                 {
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TUNER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_PANEL:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_EXTENDED:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_UNIT:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT;
                                       break;
                                    default:
                                       IntVal2 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.SubunitID)
                                 {
                                    case AVRCP_SUBUNIT_ID_INSTANCE_0:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_1:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_2:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_3:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_4:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4;
                                       break;
                                    case AVRCP_SUBUNIT_ID_EXTENDED:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_ID_IGNORE:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE;
                                       break;
                                    default:
                                       IntVal3 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }
                                 if(!Valid)
                                    break;
                                 if((ByteArray = Env->NewByteArray(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.DataLength)) != NULL)
                                 {
                                    Env->SetByteArrayRegion(ByteArray, 0, (jsize)(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.DataLength), (jbyte *)(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.DataBuffer));

                                    Env->CallVoidMethod(AUDMObject, Method_AUDM_vendorDependentCommandIndicationEvent,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID),
                                             IntVal1,
                                             IntVal2,
                                             IntVal3,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.CompanyID.CompanyID0,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.CompanyID.CompanyID1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.VendorDependentGenericCommandData.CompanyID.CompanyID1,
                                             ByteArray
                                             );
                                 }
                                 break;
                              case amtGetCapabilities:
                                    switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.GetCapabilitiesCommandData.CapabilityID)
                                    {
                                       case AVRCP_GET_CAPABILITIES_CAPABILITY_ID_COMPANY_ID:
                                          IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CAPABILITY_ID_COMPANY_ID;
                                          break;
                                       case AVRCP_GET_CAPABILITIES_CAPABILITY_ID_EVENTS_SUPPORTED:
                                          IntVal1 = com_stonestreetone_bluetopiapm_AUDM_CAPABILITY_ID_EVENTS_SUPPORTED;
                                          break;
                                       default:
                                          IntVal1 = 0;
                                          Valid   = FALSE;
                                          break;
                                    }
                                    if(Valid)
                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_getCapabilitiesCommandIndicationEvent,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID),
                                             IntVal1
                                             );
                                 break;
                              case amtGetElementAttributes:
                                    IntVal1 = 0;
                                    for(Index=0;Index<EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.GetElementAttributesCommandData.NumberAttributes;Index++)
                                    {
                                       switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.GetElementAttributesCommandData.AttributeIDList[Index])
                                       {
                                          case AVRCP_MEDIA_ATTRIBUTE_ID_TITLE_OF_MEDIA:
                                             IntVal1 |= com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_TITLE;
                                             break;
                                          case AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ARTIST:
                                             IntVal1 |= com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_ARTIST;
                                             break;
                                          case AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ALBUM:
                                             IntVal1 |= com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_ALBUM;
                                             break;
                                          case AVRCP_MEDIA_ATTRIBUTE_ID_NUMBER_OF_MEDIA:
                                             IntVal1 |= com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA;
                                             break;
                                          case AVRCP_MEDIA_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA:
                                             IntVal1 |= com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA;
                                             break;
                                          case AVRCP_MEDIA_ATTRIBUTE_ID_GENRE:
                                             IntVal1 |= com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_GENRE;
                                             break;
                                          case AVRCP_MEDIA_ATTRIBUTE_ID_PLAYING_TIME_MS:
                                             IntVal1 |= com_stonestreetone_bluetopiapm_AUDM_ELEMENT_ATTRIBUTE_ID_PLAYING_TIME;
                                             break;
                                       }
                                    }
                                    Env->CallVoidMethod(AUDMObject, Method_AUDM_getElementAttributesCommandIndicationEvent,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID),
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.GetElementAttributesCommandData.Identifier,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.GetElementAttributesCommandData.NumberAttributes,
                                             IntVal1
                                             );
                                 break;
                              case amtGetPlayStatus:
                                    Env->CallVoidMethod(AUDMObject, Method_AUDM_getPlayStatusCommandIndicationEvent,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID)
                                             );
                                 break;
                              case amtRegisterNotification:
                                 switch(EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.RegisterNotificationCommandData.EventID)
                                 {
                                    case AVRCP_EVENT_PLAYBACK_STATUS_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_PLAYBACK_STATUS_CHANGED;
                                       break;
                                    case AVRCP_EVENT_TRACK_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_TRACK_CHANGED;
                                       break;
                                    case AVRCP_EVENT_TRACK_REACHED_END:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_TRACK_REACHED_END;
                                       break;
                                    case AVRCP_EVENT_TRACK_REACHED_START:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_TRACK_REACHED_START;
                                       break;
                                    case AVRCP_EVENT_PLAYBACK_POS_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_PLAYBACK_POSITION_CHANGED;
                                       break;
                                    case AVRCP_EVENT_SYSTEM_STATUS_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_SYSTEM_STATUS_CHANGED;
                                       break;
                                    case AVRCP_EVENT_VOLUME_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_VOLUME_CHANGED;
                                       break;
                                    case AVRCP_EVENT_BATT_STATUS_CHANGED:
                                    case AVRCP_EVENT_PLAYER_APPLICATION_SETTING_CHANGED:
                                    case AVRCP_EVENT_NOW_PLAYING_CONTENT_CHANGED:
                                    case AVRCP_EVENT_AVAILABLE_PLAYERS_CHANGED:
                                    case AVRCP_EVENT_ADDRESSED_PLAYER_CHANGED:
                                    case AVRCP_EVENT_UIDS_CHANGED:
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }
                                 if(Valid)
                                    Env->CallVoidMethod(AUDMObject, Method_AUDM_registerNotificationCommandIndicationEvent,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID),
                                             IntVal1,
                                             EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.RegisterNotificationCommandData.PlaybackInterval
                                             );
                                 break;
                              case amtSetAbsoluteVolume:
                                 Env->CallVoidMethod(AUDMObject, Method_AUDM_setAbsoluteVolumeCommandIndicationEvent,
                                          EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                          EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                          EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                          EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                          EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                          EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                          (jbyte)(EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID),
                                          EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.SetAbsoluteVolumeCommandData.AbsoluteVolume
                                          );
                                 
                                 break;
                              case amtUnknown:
                              case amtUnitInfo:
                              case amtSubunitInfo:
                              case amtBrowsingChannel_Generic:
                              case amtFragmentedMessage:
                              case amtGroupNavigation:
                              case amtListPlayerApplicationSettingAttributes:
                              case amtListPlayerApplicationSettingValues:
                              case amtGetCurrentPlayerApplicationSettingValue:
                              case amtSetPlayerApplicationSettingValue:
                              case amtGetPlayerApplicationSettingAttributeText:
                              case amtGetPlayerApplicationSettingValueText:
                              case amtInformDisplayableCharacterSet:
                              case amtInformBatteryStatusOfCT:
                              case amtRequestContinuingResponse:
                              case amtAbortContinuingResponse:
                              case amtCommandRejectResponse:
                              case amtSetAddressedPlayer:
                              case amtPlayItem:
                              case amtAddToNowPlaying:
                              case amtSetBrowsedPlayer:
                              case amtChangePath:
                              case amtGetItemAttributes:
                              case amtSearch:
                              case amtGetFolderItems:
                              case amtGeneralReject:
                                 Env->ThrowNew(Env->FindClass("java/lang/IllegalArgumentException"), NULL);
                           }
                           break;
                        case aetRemoteControlCommandConfirmation:
                           PRINT_DEBUG("Response Type: %u", EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageType);

                           Valid = TRUE;

                           switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageType) 
                           {
                              case amtPassThrough:
                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.ResponseCode)
                                 {
                                    case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED;
                                       break;
                                    case AVRCP_RESPONSE_ACCEPTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED;
                                       break;
                                    case AVRCP_RESPONSE_REJECTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED;
                                       break;
                                    case AVRCP_RESPONSE_IN_TRANSITION:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION;
                                       break;
                                    case AVRCP_RESPONSE_STABLE:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE;
                                       break;
                                    case AVRCP_RESPONSE_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED;
                                       break;
                                    case AVRCP_RESPONSE_INTERIM:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.SubunitType)
                                 {
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TUNER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_PANEL:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_EXTENDED:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_UNIT:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT;
                                       break;
                                    default:
                                       IntVal2 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.SubunitID)
                                 {
                                    case AVRCP_SUBUNIT_ID_INSTANCE_0:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_1:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_2:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_3:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_4:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4;
                                       break;
                                    case AVRCP_SUBUNIT_ID_EXTENDED:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_ID_IGNORE:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE;
                                       break;
                                    default:
                                       IntVal3 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 if(!Valid)
                                    break;
                                    
                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationID)
                                 {
                                    case AVRCP_PASS_THROUGH_ID_SELECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SELECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_LEFT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RIGHT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RIGHT_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RIGHT_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_LEFT_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_LEFT_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_ROOT_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ROOT_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_SETUP_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SETUP_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CONTENTS_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_FAVORITE_MENU:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_EXIT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EXIT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_0:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_0;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_1:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_1;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_2:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_2;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_3:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_3;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_4:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_4;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_5:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_5;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_6:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_6;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_7:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_7;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_8:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_8;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_9:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_9;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_DOT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_ENTER:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ENTER;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CLEAR:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CLEAR;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CHANNEL_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_SOUND_SELECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_INPUT_SELECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_HELP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_HELP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PAGE_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PAGE_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_POWER:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_POWER;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_VOLUME_UP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_UP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_VOLUME_DOWN:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_MUTE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_MUTE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PLAY:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PLAY;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_STOP:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_STOP;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_PAUSE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAUSE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_RECORD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RECORD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_REWIND:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_REWIND;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_FAST_FORWARD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_EJECT:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EJECT;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_FORWARD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FORWARD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_BACKWARD:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_BACKWARD;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_ANGLE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ANGLE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_SUBPICTURE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SUBPICTURE;
                                       break;
                                    case AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE:
                                       IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
                                       break;
                                    default:
                                       /* Workaround the fact that the  */
                                       /* following Passthrough IDs were*/
                                       /* all defined to (0xFF, invalid)*/
                                       /* at the time of this writing,  */
                                       /* so we cannot include them in  */
                                       /* the switch block above.       */
                                       if(AVRCP_PASS_THROUGH_ID_F1 == EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationID)
                                          IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F1;
                                       else
                                       {
                                          if(AVRCP_PASS_THROUGH_ID_F2 == EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationID)
                                             IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F2;
                                          else
                                          {
                                             if(AVRCP_PASS_THROUGH_ID_F3 == EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationID)
                                                IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F3;
                                             else
                                             {
                                                if(AVRCP_PASS_THROUGH_ID_F4 == EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationID)
                                                   IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F4;
                                                else
                                                {
                                                   if(AVRCP_PASS_THROUGH_ID_F5 == EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationID)
                                                      IntVal4 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F5;
                                                   else
                                                   {
                                                      IntVal4 = 0;
                                                      Valid   = FALSE;
                                                   }
                                                }
                                             }
                                          }
                                       }
                                       break;
                                 }

                                 if(Valid)
                                 {
                                    if((ByteArray = Env->NewByteArray(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationDataLength)) != NULL)
                                    {
                                       Env->SetByteArrayRegion(ByteArray, 0, (jsize)(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationDataLength), (jbyte *)(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.OperationData));

                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_remoteControlPassThroughCommandConfirmationEvent,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                             IntVal1,
                                             IntVal2,
                                             IntVal3,
                                             IntVal4,
                                             ((EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.StateFlag == FALSE) ? JNI_FALSE : JNI_TRUE),
                                             ByteArray);

                                       Env->DeleteLocalRef(ByteArray);
                                    }
                                    else
                                       Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                                 }

                                 break;
                              case amtVendorDependent_Generic:
                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.ResponseCode)
                                 {
                                    case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED;
                                       break;
                                    case AVRCP_RESPONSE_ACCEPTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED;
                                       break;
                                    case AVRCP_RESPONSE_REJECTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED;
                                       break;
                                    case AVRCP_RESPONSE_IN_TRANSITION:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION;
                                       break;
                                    case AVRCP_RESPONSE_STABLE:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE;
                                       break;
                                    case AVRCP_RESPONSE_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED;
                                       break;
                                    case AVRCP_RESPONSE_INTERIM:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.SubunitType)
                                 {
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_TUNER:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_PANEL:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_EXTENDED:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_TYPE_UNIT:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT;
                                       break;
                                    default:
                                       IntVal2 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.PassThroughResponseData.SubunitID)
                                 {
                                    case AVRCP_SUBUNIT_ID_INSTANCE_0:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_1:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_2:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_3:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3;
                                       break;
                                    case AVRCP_SUBUNIT_ID_INSTANCE_4:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4;
                                       break;
                                    case AVRCP_SUBUNIT_ID_EXTENDED:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED;
                                       break;
                                    case AVRCP_SUBUNIT_ID_IGNORE:
                                       IntVal3 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE;
                                       break;
                                    default:
                                       IntVal3 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 if(!Valid)
                                    break;

                                 if((ByteArray = Env->NewByteArray(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.VendorDependentGenericResponseData.DataLength)) != NULL)
                                 {
                                    Env->SetByteArrayRegion(ByteArray, 0, (jsize)(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.VendorDependentGenericResponseData.DataLength), (jbyte *)(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.VendorDependentGenericResponseData.DataBuffer));

                                    Env->CallVoidMethod(AUDMObject, Method_AUDM_vendorDependentCommandConfirmationEvent,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                             (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                             IntVal1,
                                             IntVal2,
                                             IntVal3,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.VendorDependentGenericResponseData.CompanyID.CompanyID0,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.VendorDependentGenericResponseData.CompanyID.CompanyID1,
                                             EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.VendorDependentGenericResponseData.CompanyID.CompanyID1,
                                             ByteArray
                                             );

                                    Env->DeleteLocalRef(ByteArray);
                                 }

                              case amtGetCapabilities:
                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.ResponseCode)
                                 {
                                    case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED;
                                       break;
                                    case AVRCP_RESPONSE_ACCEPTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED;
                                       break;
                                    case AVRCP_RESPONSE_REJECTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED;
                                       break;
                                    case AVRCP_RESPONSE_IN_TRANSITION:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION;
                                       break;
                                    case AVRCP_RESPONSE_STABLE:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE;
                                       break;
                                    case AVRCP_RESPONSE_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED;
                                       break;
                                    case AVRCP_RESPONSE_INTERIM:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 if(!Valid)
                                    break;

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.CapabilityID)
                                 {
                                    case AVRCP_GET_CAPABILITIES_CAPABILITY_ID_COMPANY_ID:
                                       if((ByteArray = Env->NewByteArray(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.NumberCapabilities)) != NULL)
                                       {
                                          if((ByteArrayData = Env->GetByteArrayElements(ByteArray, JNI_FALSE)) != NULL)
                                          {
                                             for(Index=0;Index<EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.NumberCapabilities;Index++)
                                             {
                                                ByteArrayData[Index*3] = EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.CapabilityInfoList[Index].CapabilityInfo.CompanyID.CompanyID0;
                                                ByteArrayData[Index*3+1] = EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.CapabilityInfoList[Index].CapabilityInfo.CompanyID.CompanyID1;
                                                ByteArrayData[Index*3+2] = EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.CapabilityInfoList[Index].CapabilityInfo.CompanyID.CompanyID2;
                                             }

                                             Env->ReleaseByteArrayElements(ByteArray, ByteArrayData, 0);
                                             
                                             Env->CallVoidMethod(AUDMObject, Method_AUDM_getCompanyIDCapabilitiesCommandConfirmationEvent,
                                                      EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                      EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                      EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                      EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                      EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                      EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                      (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                      EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                      IntVal1,
                                                      ByteArray
                                                      );
                                          }
                                          Env->DeleteLocalRef(ByteArray);
                                       }
                                       break;
                                    case AVRCP_GET_CAPABILITIES_CAPABILITY_ID_EVENTS_SUPPORTED:
                                       IntVal2 = 0;
                                       for(Index=0;Index<EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.NumberCapabilities;Index++)
                                       {
                                          switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetCapabilitiesResponseData.CapabilityInfoList[Index].CapabilityInfo.EventID)
                                          {
                                             case AVRCP_EVENT_PLAYBACK_STATUS_CHANGED:
                                                IntVal2 |= com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_PLAYBACK_STATUS_CHANGED;
                                                break;
                                             case AVRCP_EVENT_TRACK_CHANGED:
                                                IntVal2 |= com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_TRACK_CHANGED;
                                                break;
                                             case AVRCP_EVENT_TRACK_REACHED_END:
                                                IntVal2 |= com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_TRACK_REACHED_END;
                                                break;
                                             case AVRCP_EVENT_TRACK_REACHED_START:
                                                IntVal2 |= com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_TRACK_REACHED_START;
                                                break;
                                             case AVRCP_EVENT_PLAYBACK_POS_CHANGED:
                                                IntVal2 |= com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_PLAYBACK_POSITION_CHANGED;
                                                break;
                                             case AVRCP_EVENT_SYSTEM_STATUS_CHANGED:
                                                IntVal2 |= com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_SYSTEM_STATUS_CHANGED;
                                                break;
                                             case AVRCP_EVENT_VOLUME_CHANGED:
                                                IntVal2 |= com_stonestreetone_bluetopiapm_AUDM_EVENT_ID_VOLUME_CHANGED;
                                                break;
                                             case AVRCP_EVENT_BATT_STATUS_CHANGED:
                                             case AVRCP_EVENT_PLAYER_APPLICATION_SETTING_CHANGED:
                                             case AVRCP_EVENT_NOW_PLAYING_CONTENT_CHANGED:
                                             case AVRCP_EVENT_AVAILABLE_PLAYERS_CHANGED:
                                             case AVRCP_EVENT_ADDRESSED_PLAYER_CHANGED:
                                             case AVRCP_EVENT_UIDS_CHANGED:
                                             default:
                                                break;
                                          }
                                       }
                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_getEventsSupportedCapabilitiesCommandConfirmationEvent,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                IntVal1,
                                                IntVal2
                                                );
                                       break;
                                    default:
                                       break;
                                 }

                                 break;
                              case amtGetElementAttributes:
                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetElementAttributesResponseData.ResponseCode)
                                 {
                                    case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED;
                                       break;
                                    case AVRCP_RESPONSE_ACCEPTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED;
                                       break;
                                    case AVRCP_RESPONSE_REJECTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED;
                                       break;
                                    case AVRCP_RESPONSE_IN_TRANSITION:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION;
                                       break;
                                    case AVRCP_RESPONSE_STABLE:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE;
                                       break;
                                    case AVRCP_RESPONSE_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED;
                                       break;
                                    case AVRCP_RESPONSE_INTERIM:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 PRINT_DEBUG("Calling Elem Handler");

                                 HandleGetElementAttributesResponse(Env, AUDMObject, IntVal1, EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress, (jbyte)EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID, EventData->EventData.RemoteControlCommandConfirmationEventData.Status, &EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetElementAttributesResponseData);

                                 break;
                              case amtGetPlayStatus:
                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetPlayStatusResponseData.ResponseCode)
                                 {
                                    case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED;
                                       break;
                                    case AVRCP_RESPONSE_ACCEPTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED;
                                       break;
                                    case AVRCP_RESPONSE_REJECTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED;
                                       break;
                                    case AVRCP_RESPONSE_IN_TRANSITION:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION;
                                       break;
                                    case AVRCP_RESPONSE_STABLE:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE;
                                       break;
                                    case AVRCP_RESPONSE_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED;
                                       break;
                                    case AVRCP_RESPONSE_INTERIM:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetPlayStatusResponseData.PlayStatus)
                                 {
                                    case AVRCP_PLAY_STATUS_STATUS_STOPPED:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_STOPPED;
                                       break;
                                    case AVRCP_PLAY_STATUS_STATUS_PLAYING:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_PLAYING;
                                       break;
                                    case AVRCP_PLAY_STATUS_STATUS_PAUSED:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_PAUSED;
                                       break;
                                    case AVRCP_PLAY_STATUS_STATUS_FWD_SEEK:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_FWD_SEEK;
                                       break;
                                    case AVRCP_PLAY_STATUS_STATUS_REV_SEEK:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_REV_SEEK;
                                       break;
                                    case AVRCP_PLAY_STATUS_STATUS_ERROR:
                                       IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_ERROR;
                                       break;
                                    default:
                                       Valid = FALSE;
                                 }

                                 if(!Valid)
                                    break;


                                 Env->CallVoidMethod(AUDMObject, Method_AUDM_getPlayStatusCommandConfirmationEvent,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                          (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                          IntVal1,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetPlayStatusResponseData.SongLength,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.GetPlayStatusResponseData.SongPosition,
                                          IntVal2
                                          );

                                 break;
                              case amtRegisterNotification:
                           PRINT_DEBUG("Notification response", EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.EventID);

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.ResponseCode)
                                 {
                                    case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED;
                                       break;
                                    case AVRCP_RESPONSE_ACCEPTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED;
                                       break;
                                    case AVRCP_RESPONSE_REJECTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED;
                                       break;
                                    case AVRCP_RESPONSE_IN_TRANSITION:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION;
                                       break;
                                    case AVRCP_RESPONSE_STABLE:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE;
                                       break;
                                    case AVRCP_RESPONSE_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED;
                                       break;
                                    case AVRCP_RESPONSE_INTERIM:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.EventID)
                                 {
                                    case AVRCP_EVENT_PLAYBACK_STATUS_CHANGED:
                                       
                                       switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackStatusChangedData.PlayStatus)
                                       {
                                          case AVRCP_PLAY_STATUS_STATUS_STOPPED:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_STOPPED;
                                             break;
                                          case AVRCP_PLAY_STATUS_STATUS_PLAYING:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_PLAYING;
                                             break;
                                          case AVRCP_PLAY_STATUS_STATUS_PAUSED:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_PAUSED;
                                             break;
                                          case AVRCP_PLAY_STATUS_STATUS_FWD_SEEK:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_FWD_SEEK;
                                             break;
                                          case AVRCP_PLAY_STATUS_STATUS_REV_SEEK:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_REV_SEEK;
                                             break;
                                          case AVRCP_PLAY_STATUS_STATUS_ERROR:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_PLAY_STATUS_ERROR;
                                             break;
                                          default:
                                             Valid = FALSE;
                                       }

                                       if(Valid)
                                          Env->CallVoidMethod(AUDMObject, Method_AUDM_playbackStatusChangedNotificationEvent,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                   (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                   IntVal1,
                                                   IntVal2
                                                   );
                                       break;
                                    case AVRCP_EVENT_TRACK_CHANGED:
                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_trackChangedNotificationEvent,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                IntVal1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.NotificationData.TrackChangedData.Identifier
                                                );
                                       break;
                                    case AVRCP_EVENT_TRACK_REACHED_END:
                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_trackReachedEndNotificationEvent,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                IntVal1
                                                );
                                       break;
                                    case AVRCP_EVENT_TRACK_REACHED_START:
                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_trackReachedStartNotificationEvent,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                IntVal1
                                                );
                                       break;
                                    case AVRCP_EVENT_PLAYBACK_POS_CHANGED:
                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_playbackPositionChangedNotificationEvent,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                IntVal1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackPosChangedData.PlaybackPosition
                                                );
                                       break;
                                    case AVRCP_EVENT_SYSTEM_STATUS_CHANGED:
                                       switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.NotificationData.SystemStatusChangedData.SystemStatus)
                                       {
                                          case AVRCP_SYSTEM_STATUS_CHANGED_POWER_ON:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_SYSTEM_STATUS_POWER_ON;
                                             break;
                                          case AVRCP_SYSTEM_STATUS_CHANGED_POWER_OFF:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_SYSTEM_STATUS_POWER_OFF;
                                             break;
                                          case AVRCP_SYSTEM_STATUS_CHANGED_POWER_UNPLUGGED:
                                             IntVal2 = com_stonestreetone_bluetopiapm_AUDM_SYSTEM_STATUS_UNPLUGGED;
                                             break;
                                          default:
                                             Valid = FALSE;
                                       }

                                       if(Valid)
                                       {
                                          Env->CallVoidMethod(AUDMObject, Method_AUDM_systemStatusChangedNotificationEvent,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                   (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                   EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                   IntVal1,
                                                   IntVal2
                                                   );
                                       }
                                       break;
                                    case AVRCP_EVENT_VOLUME_CHANGED:
                                       PRINT_DEBUG("Handling Volume Changed");
                                       PRINT_DEBUG("Method: %lu", Method_AUDM_volumeChangeNotificationEvent);
                                       Env->CallVoidMethod(AUDMObject, Method_AUDM_volumeChangeNotificationEvent,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                                EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                                IntVal1,
                                                (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.RegisterNotificationResponseData.NotificationData.VolumeChangedData.AbsoluteVolume)
                                                );

                                       break;
                                    case AVRCP_EVENT_BATT_STATUS_CHANGED:
                                    case AVRCP_EVENT_PLAYER_APPLICATION_SETTING_CHANGED:
                                    case AVRCP_EVENT_NOW_PLAYING_CONTENT_CHANGED:
                                    case AVRCP_EVENT_AVAILABLE_PLAYERS_CHANGED:
                                    case AVRCP_EVENT_ADDRESSED_PLAYER_CHANGED:
                                    case AVRCP_EVENT_UIDS_CHANGED:
                                    default:
                                       break;
                                 }

                                 break;
                              case amtSetAbsoluteVolume:
                                 switch(EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.SetAbsoluteVolumeResponseData.ResponseCode)
                                 {
                                    case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED;
                                       break;
                                    case AVRCP_RESPONSE_ACCEPTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED;
                                       break;
                                    case AVRCP_RESPONSE_REJECTED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED;
                                       break;
                                    case AVRCP_RESPONSE_IN_TRANSITION:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION;
                                       break;
                                    case AVRCP_RESPONSE_STABLE:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE;
                                       break;
                                    case AVRCP_RESPONSE_CHANGED:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED;
                                       break;
                                    case AVRCP_RESPONSE_INTERIM:
                                       IntVal1 = com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM;
                                       break;
                                    default:
                                       IntVal1 = 0;
                                       Valid   = FALSE;
                                       break;
                                 }

                                 Env->CallVoidMethod(AUDMObject, Method_AUDM_setAbsoluteVolumeCommandConfirmationEvent,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                          (jbyte)(EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID),
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.Status,
                                          IntVal1,
                                          EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.SetAbsoluteVolumeResponseData.AbsoluteVolume
                                          );
                                 break;
                              case amtCommandRejectResponse:
                                 HandleCommandRejectResponse(Env, AUDMObject, EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress, (jbyte)EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID, EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.CommandRejectResponseData.ErrorCode, EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteControlResponseData.MessageData.CommandRejectResponseData.MessageType);
                                 break;
                              case amtUnknown:
                              case amtUnitInfo:
                              case amtSubunitInfo:
                              case amtBrowsingChannel_Generic:
                              case amtFragmentedMessage:
                              case amtGroupNavigation:
                              case amtListPlayerApplicationSettingAttributes:
                              case amtListPlayerApplicationSettingValues:
                              case amtGetCurrentPlayerApplicationSettingValue:
                              case amtSetPlayerApplicationSettingValue:
                              case amtGetPlayerApplicationSettingAttributeText:
                              case amtGetPlayerApplicationSettingValueText:
                              case amtInformDisplayableCharacterSet:
                              case amtInformBatteryStatusOfCT:
                              case amtRequestContinuingResponse:
                              case amtAbortContinuingResponse:
                              case amtSetAddressedPlayer:
                              case amtPlayItem:
                              case amtAddToNowPlaying:
                              case amtSetBrowsedPlayer:
                              case amtChangePath:
                              case amtGetItemAttributes:
                              case amtSearch:
                              case amtGetFolderItems:
                              case amtGeneralReject:
                              default:
                                 Env->ThrowNew(Env->FindClass("java/lang/IllegalArgumentException"), NULL);
                           }
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during AUDM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, AUDMObject, LocalData);
                  }

                  Env->DeleteLocalRef(AUDMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the    */
                  /* AUDM java object. This can happen if the object has   */
                  /* been garbage collected or if the VM has run out of    */
                  /* memory. Now, check whether the object was GC'd. Since */
                  /* NewLocalRef doesn't throw exceptions, we can do this  */
                  /* with IsSameObject.                                    */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The AUDM Manager object has been GC'd. Since we are*/
                     /* still receiving events on this registration, the   */
                     /* Manager was not cleaned up properly.               */
                     PRINT_ERROR("AUDM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it  */
                     /* could indicate a leak in this thread context.      */
                     PRINT_ERROR("VM reports 'Out of Memory' in AUDM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The AUDM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: AUDM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: AUDM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_AUDM = (jclass)(NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_AUDM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_AUDM_incomingConnectionRequestEvent                         = Env->GetMethodID(Clazz, "incomingConnectionRequestEvent",                         "(BBBBBBI)V");
      Method_AUDM_audioStreamConnectedEvent                              = Env->GetMethodID(Clazz, "audioStreamConnectedEvent",                              "(IBBBBBBIJII)V");
      Method_AUDM_audioStreamConnectionStatusEvent                       = Env->GetMethodID(Clazz, "audioStreamConnectionStatusEvent",                       "(IIIJI)V");
      Method_AUDM_audioStreamDisconnectedEvent                           = Env->GetMethodID(Clazz, "audioStreamDisconnectedEvent",                           "(I)V");
      Method_AUDM_audioStreamStateChangedEvent                           = Env->GetMethodID(Clazz, "audioStreamStateChangedEvent",                           "(II)V");
      Method_AUDM_changeAudioStreamStateStatusEvent                      = Env->GetMethodID(Clazz, "changeAudioStreamStateStatusEvent",                      "(ZII)V");
      Method_AUDM_audioStreamFormatChangedEvent                          = Env->GetMethodID(Clazz, "audioStreamFormatChangedEvent",                          "(IJII)V");
      Method_AUDM_changeAudioStreamFormatStatusEvent                     = Env->GetMethodID(Clazz, "changeAudioStreamFormatStatusEvent",                     "(ZIJII)V");
      Method_AUDM_remoteControlConnectedEvent                            = Env->GetMethodID(Clazz, "remoteControlConnectedEvent",                            "(BBBBBB)V");
      Method_AUDM_remoteControlConnectionStatusEvent                     = Env->GetMethodID(Clazz, "remoteControlConnectionStatusEvent",                     "(BBBBBBI)V");
      Method_AUDM_remoteControlDisconnectedEvent                         = Env->GetMethodID(Clazz, "remoteControlDisconnectedEvent",                         "(BBBBBBI)V");
      Method_AUDM_encodedAudioStreamDataEvent                            = Env->GetMethodID(Clazz, "encodedAudioStreamDataEvent",                            "(I[B)V");
      Method_AUDM_remoteControlPassThroughCommandIndicationEvent         = Env->GetMethodID(Clazz, "remoteControlPassThroughCommandIndicationEvent",         "(BBBBBBBIIIIZ[B)V");
      Method_AUDM_remoteControlPassThroughCommandConfirmationEvent       = Env->GetMethodID(Clazz, "remoteControlPassThroughCommandConfirmationEvent",       "(BBBBBBBIIIIIZ[B)V");
      Method_AUDM_vendorDependentCommandIndicationEvent                  = Env->GetMethodID(Clazz, "vendorDependentCommandIndicationEvent",                  "(BBBBBBBIIIBBB[B)V");
      Method_AUDM_getCapabilitiesCommandIndicationEvent                  = Env->GetMethodID(Clazz, "getCapabilitiesCommandIndicationEvent",                  "(BBBBBBBI)V");
      Method_AUDM_getElementAttributesCommandIndicationEvent             = Env->GetMethodID(Clazz, "getElementAttributesCommandIndicationEvent",             "(BBBBBBBJII)V");
      Method_AUDM_getPlayStatusCommandIndicationEvent                    = Env->GetMethodID(Clazz, "getPlayStatusCommandIndicationEvent",                    "(BBBBBBB)V");
      Method_AUDM_registerNotificationCommandIndicationEvent             = Env->GetMethodID(Clazz, "registerNotificationCommandIndicationEvent",             "(BBBBBBBII)V");
      Method_AUDM_setAbsoluteVolumeCommandIndicationEvent                = Env->GetMethodID(Clazz, "setAbsoluteVolumeCommandIndicationEvent",                "(BBBBBBBB)V");
      Method_AUDM_vendorDependentCommandConfirmationEvent                = Env->GetMethodID(Clazz, "vendorDependentCommandConfirmationEvent",                "(BBBBBBBIIIIBBB[B)V");
      Method_AUDM_getCompanyIDCapabilitiesCommandConfirmationEvent       = Env->GetMethodID(Clazz, "getCompanyIDCapabilitiesCommandConfirmationEvent",       "(BBBBBBBII[B)V");
      Method_AUDM_getEventsSupportedCapabilitiesCommandConfirmationEvent = Env->GetMethodID(Clazz, "getEventsSupportedCapabilitiesCommandConfirmationEvent", "(BBBBBBBIII)V");
      Method_AUDM_getElementAttributesCommandConfirmationEvent           = Env->GetMethodID(Clazz, "getElementAttributesCommandConfirmationEvent",           "(BBBBBBBII[I[I[[B)V");
      Method_AUDM_getPlayStatusCommandConfirmationEvent                  = Env->GetMethodID(Clazz, "getPlayStatusCommandConfirmationEvent",                  "(BBBBBBBIIIII)V");
      Method_AUDM_playbackStatusChangedNotificationEvent                 = Env->GetMethodID(Clazz, "playbackStatusChangedNotificationEvent",                 "(BBBBBBBIII)V");
      Method_AUDM_trackChangedNotificationEvent                          = Env->GetMethodID(Clazz, "trackChangedNotificationEvent",                          "(BBBBBBBIIJ)V");
      Method_AUDM_trackReachedEndNotificationEvent                       = Env->GetMethodID(Clazz, "trackReachedEndNotificationEvent",                       "(BBBBBBBII)V");
      Method_AUDM_trackReachedStartNotificationEvent                     = Env->GetMethodID(Clazz, "trackReachedStartNotificationEvent",                     "(BBBBBBBII)V");
      Method_AUDM_playbackPositionChangedNotificationEvent               = Env->GetMethodID(Clazz, "playbackPositionChangedNotificationEvent",               "(BBBBBBBIII)V");
      Method_AUDM_systemStatusChangedNotificationEvent                   = Env->GetMethodID(Clazz, "systemStatusChangedNotificationEvent",                   "(BBBBBBBIII)V");
      Method_AUDM_volumeChangeNotificationEvent                          = Env->GetMethodID(Clazz, "volumeChangeNotificationEvent",                          "(BBBBBBBIIB)V");
      Method_AUDM_setAbsoluteVolumeCommandConfirmationEvent              = Env->GetMethodID(Clazz, "setAbsoluteVolumeCommandConfirmationEvent",              "(BBBBBBBIIB)V");
      Method_AUDM_getCapabilitiesRejectedEvent                           = Env->GetMethodID(Clazz, "getCapabilitiesRejectedEvent",                           "(BBBBBBBI)V");
      Method_AUDM_registerNotificationRejectedEvent                      = Env->GetMethodID(Clazz, "registerNotificationRejectedEvent",                      "(BBBBBBBI)V");
   }
   else
      PRINT_ERROR("AUDM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.AUDM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    initObjectNative
 * Signature: ()V
 */
static void InitObjectNative(JNIEnv *Env, jobject Object, jint ServerType, jboolean DataCallbacks, jboolean RemoteControlController, jboolean RemoteControlTarget)
{
   int          RegistrationResult = 0;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(InitBTPMClient(TRUE) == 0)
   {
      if((LocalData = (LocalData_t *)malloc(sizeof(LocalData_t))) != NULL)
      {
         LocalData->CallbackID           = 0;
         LocalData->DataCallbackID       = 0;
         LocalData->ControllerCallbackID = 0;
         LocalData->TargetCallbackID     = 0;

         if((LocalData->WeakObjectReference = NewWeakRef(Env, Object)) != NULL)
         {
            switch(ServerType)
            {
               case com_stonestreetone_bluetopiapm_AUDM_SERVER_TYPE_SINK:

                  if(DataCallbacks == JNI_TRUE)
                  {
                     RegistrationResult        = AUDM_Register_Data_Event_Callback(astSNK, AUDM_EventCallback, LocalData->WeakObjectReference);
                     LocalData->DataCallbackID = RegistrationResult;
                  }
                  else
                  {
                     RegistrationResult        = AUDM_Register_Event_Callback(AUDM_EventCallback, LocalData->WeakObjectReference);
                     LocalData->CallbackID     = RegistrationResult;
                  }
                  break;

               case com_stonestreetone_bluetopiapm_AUDM_SERVER_TYPE_SOURCE:

                  if(DataCallbacks == JNI_TRUE)
                  {
                     RegistrationResult        = AUDM_Register_Data_Event_Callback(astSRC, AUDM_EventCallback, LocalData->WeakObjectReference);
                     LocalData->DataCallbackID = RegistrationResult;
                  }
                  else
                  {
                     RegistrationResult        = AUDM_Register_Event_Callback(AUDM_EventCallback, LocalData->WeakObjectReference);
                     LocalData->CallbackID     = RegistrationResult;
                  }
                  break;
            }

            if(RegistrationResult < 0)
            {
               PRINT_ERROR("AUDM: Unable to register callback for events (%d)", RegistrationResult);

               DeleteWeakRef(Env, LocalData->WeakObjectReference);
               free(LocalData);
               LocalData = 0;
               CloseBTPMClient();

               //XXX Change this to a more appropriate exception
               Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/ServerNotReachableException"), NULL);
            }
            else
            {
               if(RemoteControlController == JNI_TRUE)
               {
                  RegistrationResult              = AUDM_Register_Remote_Control_Event_Callback(AUDM_REGISTER_REMOTE_CONTROL_DATA_SERVICE_TYPE_CONTROLLER, AUDM_EventCallback, LocalData->WeakObjectReference);
                  LocalData->ControllerCallbackID = RegistrationResult;
               }
                  
               if(RegistrationResult < 0)
               {
                  PRINT_ERROR("AUDM: Unable to register callback for remote control controller events (%d)", RegistrationResult);

                  if(LocalData->CallbackID > 0)
                     AUDM_Un_Register_Event_Callback(LocalData->CallbackID);
                  else
                     if(LocalData->DataCallbackID > 0)
                        AUDM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

                  DeleteWeakRef(Env, LocalData->WeakObjectReference);
                  free(LocalData);
                  LocalData = 0;
                  CloseBTPMClient();

                  //XXX Change this to a more appropriate exception
                  Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/ServerNotReachableException"), NULL);
               }
               else
               {
                  if(RemoteControlTarget == JNI_TRUE)
                  {      
                     RegistrationResult          = AUDM_Register_Remote_Control_Event_Callback(AUDM_REGISTER_REMOTE_CONTROL_DATA_SERVICE_TYPE_TARGET, AUDM_EventCallback, LocalData->WeakObjectReference);
                     LocalData->TargetCallbackID = RegistrationResult;
                  }
                  
                  if(RegistrationResult < 0)
                  {
                     PRINT_ERROR("AUDM: Unable to register callback for remote control target events (%d)", RegistrationResult);

                     if(LocalData->CallbackID > 0)
                        AUDM_Un_Register_Event_Callback(LocalData->CallbackID);
                      else
                         if(LocalData->DataCallbackID > 0)
                            AUDM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

                     if(LocalData->ControllerCallbackID > 0)
                        AUDM_Un_Register_Remote_Control_Event_Callback(LocalData->ControllerCallbackID);

                     DeleteWeakRef(Env, LocalData->WeakObjectReference);
                     free(LocalData);
                     LocalData = 0;
                     CloseBTPMClient();

                     //XXX Change this to a more appropriate exception
                     Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/ServerNotReachableException"), NULL);
                  }
                  else
                     SetLocalData(Env, Object, LocalData);
               }
            }
         }
         else
         {
            /* No need to throw an exception here. NewWeakRef will throw*/
            /* an OutOfMemoryError if we are out of resources.          */
            PRINT_ERROR("AUDM: Out of Memory: Unable to obtain weak reference to object");
            
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
 * Class:     com_stonestreetone_bluetopiapm_AUDM
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
         AUDM_Un_Register_Event_Callback(LocalData->CallbackID);
      else
         if(LocalData->DataCallbackID > 0)
            AUDM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

      if(LocalData->ControllerCallbackID > 0)
         AUDM_Un_Register_Remote_Control_Event_Callback(LocalData->ControllerCallbackID);

      if(LocalData->TargetCallbackID > 0)
         AUDM_Un_Register_Remote_Control_Event_Callback(LocalData->TargetCallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    connectionRequestResponseNative
 * Signature: (IBBBBBBZ)I
 */
static jint ConnectionRequestResponseNative(JNIEnv *Env, jobject Object, jint RequestType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean Accept)
{
   jint                          Result;
   BD_ADDR_t                     RemoteDeviceAddress;
   AUD_Connection_Request_Type_t AUDRequestType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(RequestType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_CONNECTION_REQUEST_TYPE_STREAM:
         AUDRequestType = acrStream;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_CONNECTION_REQUEST_TYPE_REMOTE_CONTROL:
      default:
         AUDRequestType = acrRemoteControl;
         break;
   }

   Result = AUDM_Connection_Request_Response(AUDRequestType, RemoteDeviceAddress, ((Accept == JNI_FALSE) ? FALSE : TRUE));

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    connectAudioStreamNative
 * Signature: (BBBBBBIIZ)I
 */
static jint ConnectAudioStreamNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint StreamType, jint StreamFlags, jboolean WaitForConnection)
{
   jint              Result;
   BD_ADDR_t         RemoteDeviceAddress;
   LocalData_t      *LocalData;
   unsigned int      Status;
   unsigned long     Flags;
   AUD_Stream_Type_t AUDStreamType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   Flags = 0;
   Flags |= ((StreamFlags & com_stonestreetone_bluetopiapm_AUDM_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((StreamFlags & com_stonestreetone_bluetopiapm_AUDM_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_ENCRYPTION     : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = AUDM_Connect_Audio_Stream(RemoteDeviceAddress, AUDStreamType, Flags, AUDM_EventCallback, LocalData->WeakObjectReference, 0);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }
   else
   {
      Result = AUDM_Connect_Audio_Stream(RemoteDeviceAddress, AUDStreamType, Flags, 0, 0, &Status);

      /* If the request was submitted correctly, use the connection  */
      /* status as the return value.                                 */
      if(Result == 0)
         Result = Status;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    disconnectAudioStreamNative
 * Signature: (I)I
 */
static jint DisconnectAudioStreamNative(JNIEnv *Env, jobject Object, jint StreamType)
{
   jint              Result;
   AUD_Stream_Type_t AUDStreamType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   Result = AUDM_Disconnect_Audio_Stream(AUDStreamType);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    queryAudioStreamConnectedNative
 * Signature: (I[B)I
 */
static jint QueryAudioStreamConnectedNative(JNIEnv *Env, jobject Object, jint StreamType, jbyteArray Address)
{
   jint              Result;
   jbyte            *AddressArray;
   BD_ADDR_t         RemoteDeviceAddress;
   AUD_Stream_Type_t AUDStreamType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   Result = AUDM_Query_Audio_Stream_Connected(AUDStreamType, &RemoteDeviceAddress);

   if(Address)
   {
      if((Result == AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTING) || (Result == AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTED))
      {
         if((AddressArray = Env->GetByteArrayElements(Address, NULL)) != NULL)
         {
            AddressArray[0] = RemoteDeviceAddress.BD_ADDR5;
            AddressArray[1] = RemoteDeviceAddress.BD_ADDR4;
            AddressArray[2] = RemoteDeviceAddress.BD_ADDR3;
            AddressArray[3] = RemoteDeviceAddress.BD_ADDR2;
            AddressArray[4] = RemoteDeviceAddress.BD_ADDR1;
            AddressArray[5] = RemoteDeviceAddress.BD_ADDR0;

            Env->ReleaseByteArrayElements(Address, AddressArray, 0);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    queryAudioStreamStateNative
 * Signature: (I[I)I
 */
static jint QueryAudioStreamStateNative(JNIEnv *Env, jobject Object, jint StreamType, jintArray StreamState)
{
   jint               Result;
   jint               StateValue;
   AUD_Stream_Type_t  AUDStreamType;
   AUD_Stream_State_t AUDStreamState;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   Result = AUDM_Query_Audio_Stream_State(AUDStreamType, &AUDStreamState);

   if(Result == 0)
   {
      switch(AUDStreamState)
      {
         case astStreamStarted:
            StateValue = com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STARTED;
            break;
         case astStreamStopped:
         default:
            StateValue = com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STOPPED;
            break;
      }

      Env->SetIntArrayRegion(StreamState, 0, 1, &StateValue);
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    queryAudioStreamFormatNative
 * Signature: (I[J[I)I
 */
static jint QueryAudioStreamFormatNative(JNIEnv *Env, jobject Object, jint StreamType, jlongArray SampleFrequency, jintArray NumberChannels)
{
   jint                Result;
   jint                ChannelsValue;
   jlong               FrequencyValue;
   AUD_Stream_Type_t   AUDStreamType;
   AUD_Stream_Format_t AUDStreamFormat;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   Result = AUDM_Query_Audio_Stream_Format(AUDStreamType, &AUDStreamFormat);

   if(Result == 0)
   {
      FrequencyValue = AUDStreamFormat.SampleFrequency;
      ChannelsValue  = AUDStreamFormat.NumberChannels;

      Env->SetLongArrayRegion(SampleFrequency, 0, 1, &FrequencyValue);
      Env->SetIntArrayRegion(NumberChannels, 0, 1, &ChannelsValue);
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    changeAudioStreamStateNative
 * Signature: (II)I
 */
static jint ChangeAudioStreamStateNative(JNIEnv *Env, jobject Object, jint StreamType, jint StreamState)
{
   jint               Result;
   AUD_Stream_Type_t  AUDStreamType;
   AUD_Stream_State_t AUDStreamState;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   switch(StreamState)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STARTED:
         AUDStreamState = astStreamStarted;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_STATE_STOPPED:
      default:
         AUDStreamState = astStreamStopped;
         break;
   }

   Result = AUDM_Change_Audio_Stream_State(AUDStreamType, AUDStreamState);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    changeAudioStreamFormatNative
 * Signature: (IJI)I
 */
static jint ChangeAudioStreamFormatNative(JNIEnv *Env, jobject Object, jint StreamType, jlong SampleFrequency, jint NumberChannels)
{
   jint                Result;
   AUD_Stream_Type_t   AUDStreamType;
   AUD_Stream_Format_t AUDStreamFormat;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   AUDStreamFormat.SampleFrequency = SampleFrequency;
   AUDStreamFormat.NumberChannels  = NumberChannels;
   AUDStreamFormat.FormatFlags     = 0;

   Result = AUDM_Change_Audio_Stream_Format(AUDStreamType, &AUDStreamFormat);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    queryAudioStreamConfigurationNative
 * Signature: (I[I[J[B)I
 */
static jint QueryAudioStreamConfigurationNative(JNIEnv *Env, jobject Object, jint StreamType, jintArray mtuChannelsCodecType, jlongArray sampleFrequency, jbyteArray mediaCodecInformation)
{
   jint                       Result;
   jint                      *mtuChannelsCodecTypeArray;
   jlong                      FrequencyValue;
   AUD_Stream_Type_t          AUDStreamType;
   AUD_Stream_Configuration_t AUDStreamConfiguration;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(StreamType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SNK:
         AUDStreamType = astSNK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_STREAM_TYPE_SRC:
      default:
         AUDStreamType = astSRC;
         break;
   }

   Result = AUDM_Query_Audio_Stream_Configuration(AUDStreamType, &AUDStreamConfiguration);

   if(Result == 0)
   {
      if((mtuChannelsCodecTypeArray = Env->GetIntArrayElements(mtuChannelsCodecType, NULL)) != NULL)
      {
         mtuChannelsCodecTypeArray[0] = AUDStreamConfiguration.MediaMTU;
         mtuChannelsCodecTypeArray[1] = AUDStreamConfiguration.StreamFormat.NumberChannels;
         mtuChannelsCodecTypeArray[2] = AUDStreamConfiguration.MediaCodecType;

         Env->ReleaseIntArrayElements(mtuChannelsCodecType, mtuChannelsCodecTypeArray, 0);
      }

      FrequencyValue = AUDStreamConfiguration.StreamFormat.SampleFrequency;
      Env->SetLongArrayRegion(sampleFrequency, 0, 1, &FrequencyValue);

      Env->SetByteArrayRegion(mediaCodecInformation, 0, 32, (jbyte *)(AUDStreamConfiguration.MediaCodecInformation));
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    changeIncomingConnectionFlagsNative
 * Signature: (I)I
 */
static jint ChangeIncomingConnectionFlagsNative(JNIEnv *Env, jobject Object, jint ConnectionFlags)
{
   jint Result;
   long Flags;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

   Result = AUDM_Change_Incoming_Connection_Flags(Flags);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    sendEncodedAudioDataNative
 * Signature: ([B)I
 */
static jint SendEncodedAudioDataNative(JNIEnv *Env, jobject Object, jbyteArray RawAudioDataFrame)
{
   jint         Result;
   jsize        ArrayLength;
   jbyte       *Array;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(RawAudioDataFrame != NULL)
   {
      if((ArrayLength = Env->GetArrayLength(RawAudioDataFrame)) > 0)
      {
         if((Array = Env->GetByteArrayElements(RawAudioDataFrame, NULL)) != NULL)
         {
            if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
            {
               Result = AUDM_Send_Encoded_Audio_Data(LocalData->DataCallbackID, (unsigned int)ArrayLength, (unsigned char *)Array);

               ReleaseLocalData(Env, Object, LocalData);
            }
            else
               Result = -1;
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
         Result = 0;
   }
   else
      Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    sendRemoteControlPassThroughCommandNative
 * Signature: (BBBBBBJIIIIZ[B)I
 */
static jint SendRemoteControlPassThroughCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jint CommandType, jint SubunitType, jint SubunitID, jint OperationID, jboolean StateFlag, jbyteArray OperationData)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageType = amtPassThrough;

   switch(CommandType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_CONTROL:
         CommandData.MessageData.PassThroughCommandData.CommandType = AVRCP_CTYPE_CONTROL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_STATUS:
         CommandData.MessageData.PassThroughCommandData.CommandType = AVRCP_CTYPE_STATUS;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_SPECIFIC_INQUIRY:
         CommandData.MessageData.PassThroughCommandData.CommandType = AVRCP_CTYPE_SPECIFIC_INQUIRY;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_NOTIFY:
         CommandData.MessageData.PassThroughCommandData.CommandType = AVRCP_CTYPE_NOTIFY;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_GENERAL_INQUIRY:
      default:
         CommandData.MessageData.PassThroughCommandData.CommandType = AVRCP_CTYPE_GENERAL_INQUIRY;
         break;
   }

   switch(SubunitType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_TUNER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_PANEL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT:
      default:
         CommandData.MessageData.PassThroughCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_UNIT;
         break;
   }

   switch(SubunitID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0:
         CommandData.MessageData.PassThroughCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1:
         CommandData.MessageData.PassThroughCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2:
         CommandData.MessageData.PassThroughCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3:
         CommandData.MessageData.PassThroughCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4:
         CommandData.MessageData.PassThroughCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED:
         CommandData.MessageData.PassThroughCommandData.SubunitID = AVRCP_SUBUNIT_ID_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE:
      default:
         CommandData.MessageData.PassThroughCommandData.SubunitID = AVRCP_SUBUNIT_ID_IGNORE;
         break;
   }

   switch(OperationID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SELECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ROOT_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_ROOT_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SETUP_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SETUP_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CONTENTS_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAVORITE_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EXIT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_EXIT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_0:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_0;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_1:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_2:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_3:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_4:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_5:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_5;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_6:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_6;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_7:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_7;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_8:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_8;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_9:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_9;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_DOT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ENTER:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_ENTER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CLEAR:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CLEAR;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SOUND_SELECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_INPUT_SELECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_HELP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_HELP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PAGE_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_POWER:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_POWER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_VOLUME_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_MUTE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_MUTE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PLAY:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PLAY;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_STOP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_STOP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAUSE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PAUSE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RECORD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RECORD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_REWIND:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_REWIND;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAST_FORWARD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EJECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_EJECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FORWARD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_FORWARD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_BACKWARD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_BACKWARD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ANGLE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_ANGLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SUBPICTURE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SUBPICTURE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F1:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F2:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F3:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F4:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F5:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F5;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE:
      default:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
         break;
   }

   CommandData.MessageData.PassThroughCommandData.StateFlag           = StateFlag;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if(OperationData)
      {
         CommandData.MessageData.PassThroughCommandData.OperationDataLength = Env->GetArrayLength(OperationData);
         CommandData.MessageData.PassThroughCommandData.OperationData       = (Byte_t *)(Env->GetByteArrayElements(OperationData, NULL));

         if(CommandData.MessageData.PassThroughCommandData.OperationData)
         {
            Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseByteArrayElements(OperationData, (jbyte *)(CommandData.MessageData.PassThroughCommandData.OperationData), JNI_ABORT);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         CommandData.MessageData.PassThroughCommandData.OperationDataLength = 0;
         CommandData.MessageData.PassThroughCommandData.OperationData       = NULL;

         Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    sendRemoteControlResponseNative
 * Signature: (BBBBBBIIIIIZ[B)I
 */
static jint SendRemoteControlPassThroughResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jint SubunitType, jint SubunitID, jint OperationID, jboolean StateFlag, jbyteArray OperationData)
{
   jint                                Result;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtPassThrough;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseData.MessageData.PassThroughResponseData.ResponseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseData.MessageData.PassThroughResponseData.ResponseCode = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseData.MessageData.PassThroughResponseData.ResponseCode = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseData.MessageData.PassThroughResponseData.ResponseCode = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseData.MessageData.PassThroughResponseData.ResponseCode = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseData.MessageData.PassThroughResponseData.ResponseCode = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseData.MessageData.PassThroughResponseData.ResponseCode = AVRCP_RESPONSE_INTERIM;
         break;
   }

   switch(SubunitType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_TUNER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_PANEL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT:
      default:
         ResponseData.MessageData.PassThroughResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_UNIT;
         break;
   }

   switch(SubunitID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0:
         ResponseData.MessageData.PassThroughResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1:
         ResponseData.MessageData.PassThroughResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2:
         ResponseData.MessageData.PassThroughResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3:
         ResponseData.MessageData.PassThroughResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4:
         ResponseData.MessageData.PassThroughResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED:
         ResponseData.MessageData.PassThroughResponseData.SubunitID = AVRCP_SUBUNIT_ID_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE:
      default:
         ResponseData.MessageData.PassThroughResponseData.SubunitID = AVRCP_SUBUNIT_ID_IGNORE;
         break;
   }

   switch(OperationID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SELECT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_UP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOWN:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_UP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RIGHT_DOWN:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_UP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_LEFT_DOWN:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ROOT_MENU:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_ROOT_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SETUP_MENU:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_SETUP_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CONTENTS_MENU:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAVORITE_MENU:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EXIT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_EXIT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_0:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_0;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_1:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_2:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_3:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_4:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_5:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_5;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_6:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_6;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_7:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_7;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_8:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_8;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_9:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_9;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DOT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_DOT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ENTER:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_ENTER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CLEAR:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_CLEAR;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_UP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SOUND_SELECT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_INPUT_SELECT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_HELP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_HELP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_UP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_PAGE_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAGE_DOWN:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_POWER:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_POWER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_UP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_VOLUME_UP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VOLUME_DOWN:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_MUTE:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_MUTE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PLAY:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_PLAY;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_STOP:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_STOP;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_PAUSE:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_PAUSE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_RECORD:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_RECORD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_REWIND:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_REWIND;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FAST_FORWARD:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_EJECT:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_EJECT;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_FORWARD:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_FORWARD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_BACKWARD:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_BACKWARD;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_ANGLE:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_ANGLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_SUBPICTURE:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_SUBPICTURE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F1:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_F1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F2:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_F2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F3:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_F3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F4:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_F4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_F5:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_F5;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE:
      default:
         ResponseData.MessageData.PassThroughResponseData.OperationID = AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
         break;
   }

   ResponseData.MessageData.PassThroughResponseData.StateFlag           = StateFlag;


   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if(OperationData)
      {
         ResponseData.MessageData.PassThroughResponseData.OperationDataLength = Env->GetArrayLength(OperationData);
         ResponseData.MessageData.PassThroughResponseData.OperationData       = (Byte_t *)(Env->GetByteArrayElements(OperationData, NULL));

         if(ResponseData.MessageData.PassThroughResponseData.OperationData)
         {
            Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

            Env->ReleaseByteArrayElements(OperationData, (jbyte *)(ResponseData.MessageData.PassThroughResponseData.OperationData), JNI_ABORT);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         ResponseData.MessageData.PassThroughResponseData.OperationDataLength = 0;
         ResponseData.MessageData.PassThroughResponseData.OperationData       = NULL;

         Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

static jint ConnectRemoteControlNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ConnectionFlags, jboolean WaitForConnection)
{
   jint          Result;
   BD_ADDR_t     RemoteDeviceAddress;
   LocalData_t  *LocalData;
   unsigned int  Flags;
   unsigned int  Status;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AUDM_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AUDM_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_ENCRYPTION     : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = AUDM_Connect_Remote_Control(RemoteDeviceAddress, Flags, AUDM_EventCallback, LocalData->WeakObjectReference, 0);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }
   else
   {
      Result = AUDM_Connect_Remote_Control(RemoteDeviceAddress, Flags, 0, 0, &Status);

      /* If the request was submitted correctly, use the connection  */
      /* status as the return value.                                 */
      if(Result == 0)
         Result = Status;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

static jint DisconnectRemoteControlNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = AUDM_Disconnect_Remote_Control(RemoteDeviceAddress);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

static jint QueryConnectedRemoteControlDevicesNative(JNIEnv *Env, jobject Object, jobjectArray RemoteDeviceAddressList)
{
   jint         Result;
   jobject      Address;
   BD_ADDR_t   *BD_ADDRList;
   unsigned int Index;
   unsigned int TotalConnected;
   jobjectArray AddressList;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Result = AUDM_Query_Remote_Control_Connected_Devices(0, NULL, &TotalConnected)) == 0)
   {
      if(TotalConnected > 0)
      {
         if((BD_ADDRList = (BD_ADDR_t *)BTPS_AllocateMemory(TotalConnected * sizeof(BD_ADDR_t))) != NULL)
         {
            if((Result = AUDM_Query_Remote_Control_Connected_Devices(TotalConnected, BD_ADDRList, NULL)) >= 0)
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
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    vendorDependentCommandNative
 * Signature: (BBBBBBJIIIBBB[B)I
 */
static jint VendorDependentCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jint CommandType, jint SubunitType, jint SubunitID, jbyte ID0, jbyte ID1, jbyte ID2, jbyteArray OperationData)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(CommandType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_CONTROL:
         CommandData.MessageData.VendorDependentGenericCommandData.CommandType = AVRCP_CTYPE_CONTROL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_STATUS:
         CommandData.MessageData.VendorDependentGenericCommandData.CommandType = AVRCP_CTYPE_STATUS;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_SPECIFIC_INQUIRY:
         CommandData.MessageData.VendorDependentGenericCommandData.CommandType = AVRCP_CTYPE_SPECIFIC_INQUIRY;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_NOTIFY:
         CommandData.MessageData.VendorDependentGenericCommandData.CommandType = AVRCP_CTYPE_NOTIFY;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_CTYPE_GENERAL_INQUIRY:
      default:
         CommandData.MessageData.VendorDependentGenericCommandData.CommandType = AVRCP_CTYPE_GENERAL_INQUIRY;
         break;
   }

   switch(SubunitType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_TUNER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_PANEL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT:
      default:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitType = AVRCP_SUBUNIT_TYPE_UNIT;
         break;
   }

   switch(SubunitID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitID = AVRCP_SUBUNIT_ID_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE:
      default:
         CommandData.MessageData.VendorDependentGenericCommandData.SubunitID = AVRCP_SUBUNIT_ID_IGNORE;
         break;
   }

   CommandData.MessageData.VendorDependentGenericCommandData.CompanyID.CompanyID0 = ID0;
   CommandData.MessageData.VendorDependentGenericCommandData.CompanyID.CompanyID1 = ID1;
   CommandData.MessageData.VendorDependentGenericCommandData.CompanyID.CompanyID2 = ID2;

   CommandData.MessageType = amtVendorDependent_Generic;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if(OperationData)
      {
         CommandData.MessageData.VendorDependentGenericCommandData.DataLength = Env->GetArrayLength(OperationData);
         CommandData.MessageData.VendorDependentGenericCommandData.DataBuffer = (Byte_t *)(Env->GetByteArrayElements(OperationData, NULL));

         if(CommandData.MessageData.VendorDependentGenericCommandData.DataBuffer)
         {
            Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseByteArrayElements(OperationData, (jbyte *)(CommandData.MessageData.VendorDependentGenericCommandData.DataBuffer), JNI_ABORT);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         CommandData.MessageData.VendorDependentGenericCommandData.DataLength = 0;
         CommandData.MessageData.VendorDependentGenericCommandData.DataBuffer = NULL;

         Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    getCapabilitiesCommandNative
 * Signature: (BBBBBBJI)I
 */
static jint GetCapabilitiesCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jint CapabilityID)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageType = amtGetCapabilities;

   switch(CapabilityID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_CAPABILITY_ID_COMPANY_ID:
         CommandData.MessageData.GetCapabilitiesCommandData.CapabilityID = AVRCP_GET_CAPABILITIES_CAPABILITY_ID_COMPANY_ID;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_CAPABILITY_ID_EVENTS_SUPPORTED:
      default:
         CommandData.MessageData.GetCapabilitiesCommandData.CapabilityID = AVRCP_GET_CAPABILITIES_CAPABILITY_ID_EVENTS_SUPPORTED;
         break;
   }

   PRINT_DEBUG("%s: CapabilityID: %u\n", __FUNCTION__, CommandData.MessageData.GetCapabilitiesCommandData.CapabilityID);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    getElementAttributesCommandNative
 * Signature: (BBBBBBJJII)I
 */
static jint GetElementAttributesCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jlong Identifier, jint NumberAttributes, jint AttributeMask)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   DWord_t                            IDs[7];
   LocalData_t                       *LocalData;
   unsigned int                       AttributesSet;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageType = amtGetElementAttributes;

   AttributesSet = 0;

   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_TITLE)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_TITLE_OF_MEDIA;
   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_ARTIST)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ARTIST;
   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_ALBUM)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ALBUM;
   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_ARTIST)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ARTIST;
   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_NUMBER_OF_MEDIA;
   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA;
   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_GENRE)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_GENRE;
   if(AttributeMask & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_PLAYING_TIME)
      IDs[AttributesSet++] = AVRCP_MEDIA_ATTRIBUTE_ID_PLAYING_TIME_MS;

   CommandData.MessageData.GetElementAttributesCommandData.Identifier       = Identifier;
   CommandData.MessageData.GetElementAttributesCommandData.NumberAttributes = AttributesSet;
   CommandData.MessageData.GetElementAttributesCommandData.AttributeIDList  = IDs;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    getPlayStatusCommandNative
 * Signature: (BBBBBBJ)I
 */
static jint GetPlayStatusCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageType = amtGetPlayStatus;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    registerNotificationCommandNative
 * Signature: (BBBBBBJII)I
 */
static jint RegisterNotificationCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jint EventID, jint PlaybackInterval)
{
   jint                               Result = 0;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageType = amtRegisterNotification;

   switch(EventID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_PLAYBACK_STATUS_CHANGED:
         CommandData.MessageData.RegisterNotificationCommandData.EventID = AVRCP_EVENT_PLAYBACK_STATUS_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_TRACK_CHANGED:
         CommandData.MessageData.RegisterNotificationCommandData.EventID = AVRCP_EVENT_TRACK_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_TRACK_REACHED_END:
         CommandData.MessageData.RegisterNotificationCommandData.EventID = AVRCP_EVENT_TRACK_REACHED_END;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_TRACK_REACHED_START:
         CommandData.MessageData.RegisterNotificationCommandData.EventID = AVRCP_EVENT_TRACK_REACHED_START;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_PLAYBACK_POSITION_CHANGED:
         CommandData.MessageData.RegisterNotificationCommandData.EventID = AVRCP_EVENT_PLAYBACK_POS_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_SYSTEM_STATUS_CHANGED:
         CommandData.MessageData.RegisterNotificationCommandData.EventID = AVRCP_EVENT_SYSTEM_STATUS_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_VOLUME_CHANGED:
         CommandData.MessageData.RegisterNotificationCommandData.EventID = AVRCP_EVENT_VOLUME_CHANGED;
         break;
      default:
         Result = BTPM_ERROR_CODE_UNKNOWN_REMOTE_CONTROL_EVENT_TYPE;
   }

   CommandData.MessageData.RegisterNotificationCommandData.PlaybackInterval = PlaybackInterval;

   if(!Result)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    setAbsoluteVolumeCommandNative
 * Signature: (BBBBBBJB)I
 */
static jint SetAbsoluteVolumeCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyte AbsoluteVolume)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageType = amtSetAbsoluteVolume;

   CommandData.MessageData.SetAbsoluteVolumeCommandData.AbsoluteVolume;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Command(LocalData->ControllerCallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    vendorDependentResponseNative
 * Signature: (BBBBBBBIIIBBB[B)I
 */
static jint VendorDependentResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jint SubunitType, jint SubunitID, jbyte ID0, jbyte ID1, jbyte ID2, jbyteArray OperationData)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   switch(SubunitType)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_TUNER:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_TUNER;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_PANEL:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_PANEL;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_EXTENDED:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_TYPE_UNIT:
      default:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitType = AVRCP_SUBUNIT_TYPE_UNIT;
         break;
   }

   switch(SubunitID)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_0:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_1:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_2:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_3:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_INSTANCE_4:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_EXTENDED:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitID = AVRCP_SUBUNIT_ID_EXTENDED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_SUBUNIT_ID_IGNORE:
      default:
         ResponseData.MessageData.VendorDependentGenericResponseData.SubunitID = AVRCP_SUBUNIT_ID_IGNORE;
         break;
   }
   
   ResponseData.MessageData.VendorDependentGenericResponseData.ResponseCode = ResponseCodeNative;

   ResponseData.MessageData.VendorDependentGenericResponseData.CompanyID.CompanyID0 = ID0;
   ResponseData.MessageData.VendorDependentGenericResponseData.CompanyID.CompanyID1 = ID1;
   ResponseData.MessageData.VendorDependentGenericResponseData.CompanyID.CompanyID2 = ID2;

   ResponseData.MessageType = amtVendorDependent_Generic;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if(OperationData)
      {
         ResponseData.MessageData.VendorDependentGenericResponseData.DataLength = Env->GetArrayLength(OperationData);
         ResponseData.MessageData.VendorDependentGenericResponseData.DataBuffer = (Byte_t *)(Env->GetByteArrayElements(OperationData, NULL));

         if(ResponseData.MessageData.VendorDependentGenericResponseData.DataBuffer)
         {
           Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

           Env->ReleaseByteArrayElements(OperationData, (jbyte *)(ResponseData.MessageData.VendorDependentGenericResponseData.DataBuffer), JNI_ABORT);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         ResponseData.MessageData.VendorDependentGenericResponseData.DataLength = 0;
         ResponseData.MessageData.VendorDependentGenericResponseData.DataBuffer = NULL;

         Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    getCompanyIDCapabilitiesResponseNative
 * Signature: (BBBBBBBI[B)I
 */
static jint GetCompanyIDCapabilitiesResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jbyteArray CompanyIDs)
{
   jint                                Result;
   jbyte                               *IDData;
   jsize                               IDLength;
   jsize                               CapabilityLength;
   jsize                               Index;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AVRCP_Capability_Info_t            *Capabilities;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtGetCapabilities;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   if((IDLength = Env->GetArrayLength(CompanyIDs)) > 0)
   {
      CapabilityLength = IDLength / 3;

      if((Capabilities = (AVRCP_Capability_Info_t *)BTPS_AllocateMemory(sizeof(AVRCP_Capability_Info_t) * (CapabilityLength))) != NULL)
      {
         if((IDData = Env->GetByteArrayElements(CompanyIDs, JNI_FALSE)) != NULL)
         {
            for(Index=0;Index<CapabilityLength;Index++)
            {
               Capabilities[Index].CapabilityInfo.CompanyID.CompanyID0 = IDData[Index*3];
               Capabilities[Index].CapabilityInfo.CompanyID.CompanyID1 = IDData[Index*3+1];
               Capabilities[Index].CapabilityInfo.CompanyID.CompanyID2 = IDData[Index*3+2];
            }

            ResponseData.MessageData.GetCapabilitiesResponseData.ResponseCode       = ResponseCodeNative;
            ResponseData.MessageData.GetCapabilitiesResponseData.CapabilityID       = AVRCP_GET_CAPABILITIES_CAPABILITY_ID_COMPANY_ID;
            ResponseData.MessageData.GetCapabilitiesResponseData.NumberCapabilities = CapabilityLength;
            ResponseData.MessageData.GetCapabilitiesResponseData.CapabilityInfoList = Capabilities;

            if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
            {
               Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

               ReleaseLocalData(Env, Object, LocalData);
            }
            else
               Result = -1;

            Env->ReleaseByteArrayElements(CompanyIDs, IDData, 0);
         }
         else
            Result = -1;

         BTPS_FreeMemory(Capabilities);
      }
      else
         Result = -1;
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    getEventsSupportedCapabilitiesResponseNative
 * Signature: (BBBBBBBII)I
 */
static jint GetEventsSupportedCapabilitiesResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jint EventIDs)
{
   jint                                Result;
   jsize                               CapabilitiesSet;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AVRCP_Capability_Info_t             Capabilities[13];
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtGetCapabilities;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   CapabilitiesSet = 0;

   if(EventIDs & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_PLAYBACK_STATUS_CHANGED)
      Capabilities[CapabilitiesSet++].CapabilityInfo.EventID = AVRCP_EVENT_PLAYBACK_STATUS_CHANGED;
   if(EventIDs & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_TRACK_CHANGED)
      Capabilities[CapabilitiesSet++].CapabilityInfo.EventID = AVRCP_EVENT_TRACK_CHANGED;
   if(EventIDs & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_TRACK_REACHED_END)
      Capabilities[CapabilitiesSet++].CapabilityInfo.EventID = AVRCP_EVENT_TRACK_REACHED_END;
   if(EventIDs & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_TRACK_REACHED_START)
      Capabilities[CapabilitiesSet++].CapabilityInfo.EventID = AVRCP_EVENT_TRACK_REACHED_START;
   if(EventIDs & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_PLAYBACK_POSITION_CHANGED)
      Capabilities[CapabilitiesSet++].CapabilityInfo.EventID = AVRCP_EVENT_PLAYBACK_POS_CHANGED;
   if(EventIDs & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_SYSTEM_STATUS_CHANGED)
      Capabilities[CapabilitiesSet++].CapabilityInfo.EventID = AVRCP_EVENT_SYSTEM_STATUS_CHANGED;
   if(EventIDs & com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_EVENT_ID_VOLUME_CHANGED)
      Capabilities[CapabilitiesSet++].CapabilityInfo.EventID = AVRCP_EVENT_VOLUME_CHANGED;

   ResponseData.MessageData.GetCapabilitiesResponseData.ResponseCode       = ResponseCodeNative;
   ResponseData.MessageData.GetCapabilitiesResponseData.CapabilityID       = AVRCP_GET_CAPABILITIES_CAPABILITY_ID_EVENTS_SUPPORTED;
   ResponseData.MessageData.GetCapabilitiesResponseData.NumberCapabilities = CapabilitiesSet;
   ResponseData.MessageData.GetCapabilitiesResponseData.CapabilityInfoList = Capabilities;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    getElementAttributesResponseNative
 * Signature: (BBBBBBBI[I[I[[B)I
 */
static jint GetElementAttributesResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jintArray AttributeIDs, jintArray CharacterSets, jobjectArray ElementData)
{
   jint                                  Result;
   jint                                 *IDData = NULL;
   jint                                 *CharacterData = NULL;
   jbyte                                *DataBytes;
   jsize                                 Index;
   jsize                                 ElementIndex;
   jsize                                 ListLength;
   jsize                                 TotalLength;
   Byte_t                                ResponseCodeNative;
   Boolean_t                             Error = FALSE;
   BD_ADDR_t                             RemoteDeviceAddress;
   jbyteArray                            DataArray;
   LocalData_t                          *LocalData;
   AUD_Remote_Control_Response_Data_t    ResponseData;
   AVRCP_Element_Attribute_List_Entry_t  ElementAttributeList[7];

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtGetElementAttributes;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   if((TotalLength = Env->GetArrayLength(AttributeIDs)) > 0)
   {
      if(((IDData = Env->GetIntArrayElements(AttributeIDs, JNI_FALSE)) != NULL) && ((CharacterData = Env->GetIntArrayElements(CharacterSets, JNI_FALSE)) != NULL))
      {
         ElementIndex = 0;
         ListLength   = 0;

         for(Index=0;Index < TotalLength && !Error;Index++)
         {
            if((DataArray = (jbyteArray)Env->GetObjectArrayElement(ElementData, Index)) != NULL)
            {
               if((ElementAttributeList[ElementIndex].AttributeValueData = (Byte_t *)BTPS_AllocateMemory(Env->GetArrayLength(DataArray))) != NULL)
               {
                  if((DataBytes = Env->GetByteArrayElements(DataArray, JNI_FALSE)) != NULL)
                  {
                     switch(IDData[Index])
                     {
                        case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_TITLE:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_TITLE_OF_MEDIA;
                           break;
                        case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_ARTIST:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ARTIST;
                           break;
                        case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_ALBUM:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ALBUM;
                           break;
                        case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_NUMBER_OF_MEDIA;
                           break;
                        case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA;
                           break;
                        case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_GENRE:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_GENRE;
                           break;
                        case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_ELEMENT_ATTRIBUTE_ID_PLAYING_TIME:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_PLAYING_TIME_MS;
                           break;
                        default:
                           ElementAttributeList[ElementIndex].AttributeID = AVRCP_MEDIA_ATTRIBUTE_ID_ILLEGAL;
                           break;
                           
                     }

                     ElementAttributeList[ElementIndex].CharacterSet         = CharacterData[Index];
                     ElementAttributeList[ElementIndex].AttributeValueLength = Env->GetArrayLength(DataArray);

                     BTPS_MemCopy(ElementAttributeList[ElementIndex].AttributeValueData, DataBytes, Env->GetArrayLength(DataArray));

                     ElementIndex++;
                     ListLength++;

                     Env->ReleaseByteArrayElements(DataArray, DataBytes, 0);
                  }
                  else
                     Error      = TRUE;
               }
               else
                  Error      = TRUE;
            }
         }

         ResponseData.MessageData.GetElementAttributesResponseData.ResponseCode     = ResponseCodeNative;
         ResponseData.MessageData.GetElementAttributesResponseData.NumberAttributes = ListLength;
         ResponseData.MessageData.GetElementAttributesResponseData.AttributeList    = ElementAttributeList;

         if((!Error) && (ListLength > 0))
         {
            if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
            {
               Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

               ReleaseLocalData(Env, Object, LocalData);
            }
            else
               Result = -1;
      }
         else
            Result = -1;

         for(Index=0;Index<ListLength;Index++)
            BTPS_FreeMemory(ElementAttributeList[Index].AttributeValueData);
      }
      else
         Result = -1;

      if(IDData)
         Env->ReleaseIntArrayElements(AttributeIDs, IDData, 0);

      if(CharacterData)
         Env->ReleaseIntArrayElements(CharacterSets, CharacterData, 0);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    getPlayStatusResponseNative
 * Signature: (BBBBBBBIIII)I
 */
static jint GetPlayStatusResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jint Length, jint Position, jint Status)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtGetPlayStatus;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.GetPlayStatusResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.GetPlayStatusResponseData.SongLength   = Length;
   ResponseData.MessageData.GetPlayStatusResponseData.SongPosition = Position;

   switch(Status)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_STOPPED:
         ResponseData.MessageData.GetPlayStatusResponseData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_STOPPED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_PLAYING:
         ResponseData.MessageData.GetPlayStatusResponseData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_PLAYING;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_PAUSED:
         ResponseData.MessageData.GetPlayStatusResponseData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_PAUSED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_FWD_SEEK:
         ResponseData.MessageData.GetPlayStatusResponseData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_FWD_SEEK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_REV_SEEK:
         ResponseData.MessageData.GetPlayStatusResponseData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_REV_SEEK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_ERROR:
      default:
         ResponseData.MessageData.GetPlayStatusResponseData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_ERROR;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    playbackStatusChangedNotificationNative
 * Signature: (BBBBBBBII)I
 */
static jint PlaybackStatusChangedNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jint Status)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtRegisterNotification;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.RegisterNotificationResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.RegisterNotificationResponseData.EventID      = AVRCP_EVENT_PLAYBACK_STATUS_CHANGED;

   switch(Status)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_STOPPED:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackStatusChangedData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_STOPPED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_PLAYING:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackStatusChangedData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_PLAYING;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_PAUSED:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackStatusChangedData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_PAUSED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_FWD_SEEK:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackStatusChangedData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_FWD_SEEK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_REV_SEEK:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackStatusChangedData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_REV_SEEK;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_PLAY_STATUS_ERROR:
      default:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackStatusChangedData.PlayStatus = AVRCP_PLAY_STATUS_STATUS_ERROR;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
} 

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    trackChangedNotificationNative
 * Signature: (BBBBBBBIJ)I
 */
static jint TrackChangedNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jlong Identifier)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtRegisterNotification;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.RegisterNotificationResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.RegisterNotificationResponseData.EventID      = AVRCP_EVENT_TRACK_CHANGED;

   ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.TrackChangedData.Identifier = Identifier;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    trackReachedEndNotificationNative
 * Signature: (BBBBBBBI)I
 */
static jint TrackReachedEndNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtRegisterNotification;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.RegisterNotificationResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.RegisterNotificationResponseData.EventID      = AVRCP_EVENT_TRACK_REACHED_END;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    trackReachedStartNotificationNative
 * Signature: (BBBBBBBI)I
 */
static jint TrackReachedStartNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtRegisterNotification;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.RegisterNotificationResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.RegisterNotificationResponseData.EventID      = AVRCP_EVENT_TRACK_REACHED_START;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    playbackPositionChangedNotificationNative
 * Signature: (BBBBBBBII)I
 */
static jint PlaybackPositionChangedNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jint Position)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtRegisterNotification;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.RegisterNotificationResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.RegisterNotificationResponseData.EventID      = AVRCP_EVENT_PLAYBACK_POS_CHANGED;

   ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.PlaybackPosChangedData.PlaybackPosition = Position;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    systemStatusChangedNotificationNative
 * Signature: (BBBBBBBII)I
 */
static jint SystemStatusChangedNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jint Status)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtRegisterNotification;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.RegisterNotificationResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.RegisterNotificationResponseData.EventID      = AVRCP_EVENT_SYSTEM_STATUS_CHANGED;
   
   switch(Status)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_SYSTEM_STATUS_POWER_ON:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.SystemStatusChangedData.SystemStatus = AVRCP_SYSTEM_STATUS_CHANGED_POWER_ON;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_SYSTEM_STATUS_POWER_OFF:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.SystemStatusChangedData.SystemStatus = AVRCP_SYSTEM_STATUS_CHANGED_POWER_OFF;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AudioSinkManager_SYSTEM_STATUS_UNPLUGGED:
      default:
         ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.SystemStatusChangedData.SystemStatus = AVRCP_SYSTEM_STATUS_CHANGED_POWER_UNPLUGGED;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    volumeChangeNotificationNative
 * Signature: (BBBBBBBIB)I
 */
static jint VolumeChangeNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jbyte Volume)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtRegisterNotification;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.RegisterNotificationResponseData.ResponseCode = ResponseCodeNative;
   ResponseData.MessageData.RegisterNotificationResponseData.EventID      = AVRCP_EVENT_VOLUME_CHANGED;

   ResponseData.MessageData.RegisterNotificationResponseData.NotificationData.VolumeChangedData.AbsoluteVolume = Volume;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AUDM
 * Method:    setAbsoluteVolumeResponseNative
 * Signature: (BBBBBBBIB)I
 */
static jint SetAbsoluteVolumeResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyte TransactionID, jint ResponseCode, jbyte Volume)
{
   jint                                Result;
   Byte_t                              ResponseCodeNative;
   BD_ADDR_t                           RemoteDeviceAddress;
   LocalData_t                        *LocalData;
   AUD_Remote_Control_Response_Data_t  ResponseData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   ResponseData.MessageType = amtSetAbsoluteVolume;

   switch(ResponseCode)
   {
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_NOT_IMPLEMENTED:
         ResponseCodeNative = AVRCP_RESPONSE_NOT_IMPLEMENTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_ACCEPTED:
         ResponseCodeNative = AVRCP_RESPONSE_ACCEPTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_REJECTED:
         ResponseCodeNative = AVRCP_RESPONSE_REJECTED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_IN_TRANSITION:
         ResponseCodeNative = AVRCP_RESPONSE_IN_TRANSITION;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_STABLE:
         ResponseCodeNative = AVRCP_RESPONSE_STABLE;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_CHANGED:
         ResponseCodeNative = AVRCP_RESPONSE_CHANGED;
         break;
      case com_stonestreetone_bluetopiapm_AUDM_AVRCP_RESPONSE_INTERIM:
      default:
         ResponseCodeNative = AVRCP_RESPONSE_INTERIM;
         break;
   }

   ResponseData.MessageData.SetAbsoluteVolumeResponseData.AbsoluteVolume = Volume;

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = AUDM_Send_Remote_Control_Response(LocalData->TargetCallbackID, RemoteDeviceAddress, TransactionID, &ResponseData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                              "()V",                                                    (void *) InitClassNative},
   {"initObjectNative",                             "(IZZZ)V",                                                (void *) InitObjectNative},
   {"cleanupObjectNative",                          "()V",                                                    (void *) CleanupObjectNative},
   {"connectionRequestResponseNative",              "(IBBBBBBZ)I",                                            (void *) ConnectionRequestResponseNative},
   {"connectAudioStreamNative",                     "(BBBBBBIIZ)I",                                           (void *) ConnectAudioStreamNative},
   {"disconnectAudioStreamNative",                  "(I)I",                                                   (void *) DisconnectAudioStreamNative},
   {"queryAudioStreamConnectedNative",              "(I[B)I",                                                 (void *) QueryAudioStreamConnectedNative},
   {"queryAudioStreamStateNative",                  "(I[I)I",                                                 (void *) QueryAudioStreamStateNative},
   {"queryAudioStreamFormatNative",                 "(I[J[I)I",                                               (void *) QueryAudioStreamFormatNative},
   {"changeAudioStreamStateNative",                 "(II)I",                                                  (void *) ChangeAudioStreamStateNative},
   {"changeAudioStreamFormatNative",                "(IJI)I",                                                 (void *) ChangeAudioStreamFormatNative},
   {"queryAudioStreamConfigurationNative",          "(I[I[J[B)I",                                             (void *) QueryAudioStreamConfigurationNative},
   {"changeIncomingConnectionFlagsNative",          "(I)I",                                                   (void *) ChangeIncomingConnectionFlagsNative},
   {"sendEncodedAudioDataNative",                   "([B)I",                                                  (void *) SendEncodedAudioDataNative},
   {"sendRemoteControlPassThroughCommandNative",    "(BBBBBBJIIIIZ[B)I",                                      (void *) SendRemoteControlPassThroughCommandNative},
   {"sendRemoteControlPassThroughResponseNative",   "(BBBBBBBIIIIZ[B)I",                                      (void *) SendRemoteControlPassThroughResponseNative},
   {"connectRemoteControlNative",                   "(BBBBBBIZ)I",                                            (void *) ConnectRemoteControlNative},
   {"disconnectRemoteConrolNative",                 "(BBBBBB)I",                                              (void *) DisconnectRemoteControlNative},
   {"queryConnectedRemoteControlDevicesNative",     "([[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;)I", (void *) QueryConnectedRemoteControlDevicesNative},
   {"vendorDependentCommandNative",                 "(BBBBBBJIIIBBB[B)I",                                     (void *) VendorDependentCommandNative},
   {"getCapabilitiesCommandNative",                 "(BBBBBBJI)I",                                            (void *) GetCapabilitiesCommandNative},
   {"getElementAttributesCommandNative",            "(BBBBBBJJII)I",                                          (void *) GetElementAttributesCommandNative},
   {"getPlayStatusCommandNative",                   "(BBBBBBJ)I",                                             (void *) GetPlayStatusCommandNative},
   {"registerNotificationCommandNative",            "(BBBBBBJII)I",                                           (void *) RegisterNotificationCommandNative},
   {"setAbsoluteVolumeCommandNative",               "(BBBBBBJB)I",                                            (void *) SetAbsoluteVolumeCommandNative},
   {"vendorDependentResponseNative",                "(BBBBBBBIIIBBB[B)I",                                     (void *) VendorDependentResponseNative},
   {"getCompanyIDCapabilitiesResponseNative",       "(BBBBBBBI[B)I",                                          (void *) GetCompanyIDCapabilitiesResponseNative},
   {"getEventsSupportedCapabilitiesResponseNative", "(BBBBBBBII)I",                                           (void *) GetEventsSupportedCapabilitiesResponseNative},
   {"getElementAttributesResponseNative",           "(BBBBBBBI[I[I[[B)I",                                     (void *) GetElementAttributesResponseNative},
   {"getPlayStatusResponseNative",                  "(BBBBBBBIIII)I",                                         (void *) GetPlayStatusResponseNative},
   {"playbackStatusChangedNotificationNative",      "(BBBBBBBII)I",                                           (void *) PlaybackStatusChangedNotificationNative},
   {"trackChangedNotificationNative",               "(BBBBBBBIJ)I",                                           (void *) TrackChangedNotificationNative},
   {"trackReachedEndNotificationNative",            "(BBBBBBBI)I",                                            (void *) TrackReachedEndNotificationNative},
   {"trackReachedStartNotificationNative",          "(BBBBBBBI)I",                                            (void *) TrackReachedStartNotificationNative},
   {"playbackPositionChangedNotificationNative",    "(BBBBBBBII)I",                                           (void *) PlaybackPositionChangedNotificationNative},
   {"systemStatusChangedNotificationNative",        "(BBBBBBBII)I",                                           (void *) SystemStatusChangedNotificationNative},
   {"volumeChangeNotificationNative",               "(BBBBBBBIB)I",                                           (void *) VolumeChangeNotificationNative},
   {"setAbsoluteVolumeResponseNative",              "(BBBBBBBIB)I",                                           (void *) SetAbsoluteVolumeResponseNative},
};

int register_com_stonestreetone_bluetopiapm_AUDM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/AUDM";

   Result = -1;

   PRINT_DEBUG("Registering AUDM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}

