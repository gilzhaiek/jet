/*****< com_stonestreetone_bluetopiapm_HFRM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_HFRM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager HFRM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   11/18/11  G. Hensley     Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>

extern "C" {
#include "SS1BTHFRM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_HFRM.h"

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
static WEAK_REF Class_HFRM;

static jfieldID Field_HFRM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_HFRM_incomingConnectionRequestEvent;
static jmethodID Method_HFRM_connectedEvent;
static jmethodID Method_HFRM_disconnectedEvent;
static jmethodID Method_HFRM_connectionStatusEvent;
static jmethodID Method_HFRM_serviceLevelConnectionEstablishedEvent;
static jmethodID Method_HFRM_audioConnectedEvent;
static jmethodID Method_HFRM_audioDisconnectedEvent;
static jmethodID Method_HFRM_audioConnectionStatusEvent;
static jmethodID Method_HFRM_audioDataEvent;
static jmethodID Method_HFRM_voiceRecognitionIndicationEvent;
static jmethodID Method_HFRM_speakerGainIndicationEvent;
static jmethodID Method_HFRM_microphoneGainIndicationEvent;
static jmethodID Method_HFRM_incomingCallStateIndicationEvent;
static jmethodID Method_HFRM_incomingCallStateConfirmationEvent;
static jmethodID Method_HFRM_controlIndicatorStatusIndicationEvent_Boolean;
static jmethodID Method_HFRM_controlIndicatorStatusIndicationEvent_Range;
static jmethodID Method_HFRM_controlIndicatorStatusConfirmationEvent_Boolean;
static jmethodID Method_HFRM_controlIndicatorStatusConfirmationEvent_Range;
static jmethodID Method_HFRM_callHoldMultipartySupportConfirmationEvent;
static jmethodID Method_HFRM_callWaitingNotificationIndicationEvent;
static jmethodID Method_HFRM_callLineIdentificationNotificationIndicationEvent;
static jmethodID Method_HFRM_ringIndicationEvent;
static jmethodID Method_HFRM_inBandRingToneSettingIndicationEvent;
static jmethodID Method_HFRM_voiceTagRequestConfirmationEvent;
static jmethodID Method_HFRM_currentCallsListConfirmationEvent;
static jmethodID Method_HFRM_networkOperatorSelectionConfirmationEvent;
static jmethodID Method_HFRM_subscriberNumberInformationConfirmationEvent;
static jmethodID Method_HFRM_responseHoldStatusConfirmationEvent;
static jmethodID Method_HFRM_commandResultEvent;
static jmethodID Method_HFRM_arbitraryResponseIndicationEvent;
static jmethodID Method_HFRM_callHoldMultipartySelectionIndicationEvent;
static jmethodID Method_HFRM_callWaitingNotificationActivationIndicationEvent;
static jmethodID Method_HFRM_callLineIdentificationNotificationActivationIndicationEvent;
static jmethodID Method_HFRM_disableSoundEnhancementIndicationEvent;
static jmethodID Method_HFRM_dialPhoneNumberIndicationEvent;
static jmethodID Method_HFRM_dialPhoneNumberFromMemoryIndicationEvent;
static jmethodID Method_HFRM_redialLastPhoneNumberIndicationEvent;
static jmethodID Method_HFRM_generateDTMFCodeIndicationEvent;
static jmethodID Method_HFRM_answerCallIndicationEvent;
static jmethodID Method_HFRM_voiceTagRequestIndicationEvent;
static jmethodID Method_HFRM_hangUpIndicationEvent;
static jmethodID Method_HFRM_currentCallsListIndicationEvent;
static jmethodID Method_HFRM_networkOperatorSelectionFormatIndicationEvent;
static jmethodID Method_HFRM_networkOperatorSelectionIndicationEvent;
static jmethodID Method_HFRM_extendedErrorResultActivationIndicationEvent;
static jmethodID Method_HFRM_subscriberNumberInformationIndicationEvent;
static jmethodID Method_HFRM_responseHoldStatusIndicationEvent;
static jmethodID Method_HFRM_arbitraryCommandIndicationEvent;

typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
   unsigned int DataCallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_HFRM_localData, Exclusive)) == NULL)
      PRINT_ERROR("HFRM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_HFRM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_HFRM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void HFRM_EventCallback(HFRM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         BitMask;
   jint         BitMask2;
   jint         BitMask3;
   JNIEnv      *Env;
   jobject      HFRMObject;
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
         /* The CallbackParameter is a weak ref to our HFRM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((HFRMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, HFRMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        /* Common Hands Free/Audio Gateway events.         */
                        case hetHFRIncomingConnectionRequest:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_incomingConnectionRequestEvent,
                                 EventData->EventData.IncomingConnectionRequestEventData.ConnectionType,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.IncomingConnectionRequestEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRConnected:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_connectedEvent,
                                 EventData->EventData.ConnectedEventData.ConnectionType,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRDisconnected:
                           switch(EventData->EventData.DisconnectedEventData.DisconnectReason)
                           {
                              case HFRM_DEVICE_DISCONNECT_REASON_NORMAL_DISCONNECT:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_DISCONNECTION_STATUS_SUCCESS;
                                 break;
                              case HFRM_DEVICE_DISCONNECT_REASON_SERVICE_LEVEL_ERROR:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_DISCONNECTION_STATUS_SERVICE_LEVEL_CONNECTION_ERROR;
                                 break;
                           }

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_disconnectedEvent,
                                 EventData->EventData.DisconnectedEventData.ConnectionType,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask);
                           break;
                        case hetHFRConnectionStatus:
                           switch(EventData->EventData.ConnectionStatusEventData.ConnectionStatus)
                           {
                              case HFRM_DEVICE_CONNECTION_STATUS_SUCCESS:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CONNECTION_STATUS_SUCCESS;
                                 break;
                              case HFRM_DEVICE_CONNECTION_STATUS_FAILURE_TIMEOUT:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CONNECTION_STATUS_FAILURE_TIMEOUT;
                                 break;
                              case HFRM_DEVICE_CONNECTION_STATUS_FAILURE_REFUSED:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CONNECTION_STATUS_FAILURE_REFUSED;
                                 break;
                              case HFRM_DEVICE_CONNECTION_STATUS_FAILURE_SECURITY:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CONNECTION_STATUS_FAILURE_SECURITY;
                                 break;
                              case HFRM_DEVICE_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF;
                                 break;
                              case HFRM_DEVICE_CONNECTION_STATUS_FAILURE_UNKNOWN:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CONNECTION_STATUS_FAILURE_UNKNOWN;
                                 break;
                           }

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_connectionStatusEvent,
                                 EventData->EventData.ConnectionStatusEventData.ConnectionType,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask);
                           break;
                        case hetHFRServiceLevelConnectionEstablished:
                           BitMask = 0;

                           if(EventData->EventData.ServiceLevelConnectionEstablishedEventData.ConnectionType == hctHandsFree)
                           {
                              /* Local server is Hands-Free, so these will */
                              /* be Audio Gateway features.                */
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_THREE_WAY_CALLING_SUPPORTED_BIT)              ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_THREE_WAY_CALLING : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_AG_SOUND_ENHANCEMENT_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_SOUND_ENHANCEMENT : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_AG_VOICE_RECOGNITION_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_VOICE_RECOGNITION : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_INBAND_RINGING_SUPPORTED_BIT)                 ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_INBAND_RINGING : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_VOICE_TAGS_SUPPORTED_BIT)                     ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_VOICE_TAGS : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_REJECT_CALL_SUPPORT_BIT)                      ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_REJECT_CALL : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_AG_ENHANCED_CALL_STATUS_SUPPORTED_BIT)        ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_ENHANCED_CALL_STATUS : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_AG_ENHANCED_CALL_CONTROL_SUPPORTED_BIT)       ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_ENHANCED_CALL_CONTROL : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_AG_EXTENDED_ERROR_RESULT_CODES_SUPPORTED_BIT) ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_EXTENDED_ERROR_RESULT_CODES : 0);
                           }
                           else
                           {
                              /* Local server is Audio Gateway, so these   */
                              /* will be Hands-Free features.              */
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_HF_SOUND_ENHANCEMENT_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_SOUND_ENHANCEMENT : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_CALL_WAITING_THREE_WAY_CALLING_SUPPORTED_BIT) ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_CALL_WAITING_THREE_WAY_CALLING : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_CLI_SUPPORTED_BIT)                            ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_CLI : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_HF_VOICE_RECOGNITION_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_VOICE_RECOGNITION : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_REMOTE_VOLUME_CONTROL_SUPPORTED_BIT)          ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_REMOTE_VOLUME_CONTROL : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_HF_ENHANCED_CALL_STATUS_SUPPORTED_BIT)        ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_ENHANCED_CALL_STATUS : 0);
                              BitMask |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeatures & HFRE_HF_ENHANCED_CALL_CONTROL_SUPPORTED_BIT)       ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_ENHANCED_CALL_CONTROL : 0);
                           }

                           BitMask2 = 0;
                           BitMask2 |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteCallHoldMultipartySupport & HFRE_RELEASE_ALL_HELD_CALLS)                          ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS : 0);
                           BitMask2 |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteCallHoldMultipartySupport & HFRE_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL)    ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL : 0);
                           BitMask2 |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteCallHoldMultipartySupport & HFRE_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER) ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER : 0);
                           BitMask2 |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteCallHoldMultipartySupport & HFRE_ADD_A_HELD_CALL_TO_CONVERSATION)                 ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION : 0);
                           BitMask2 |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteCallHoldMultipartySupport & HFRE_CONNECT_TWO_CALLS_DISCONNECT_SUBSCRIBER)         ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER : 0);
                           BitMask2 |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteCallHoldMultipartySupport & HFRE_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY)              ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY : 0);
                           BitMask2 |= ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteCallHoldMultipartySupport & HFRE_REQUEST_PRIVATE_CONSULTATION_MODE)               ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE : 0);

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_serviceLevelConnectionEstablishedEvent,
                                 EventData->EventData.ServiceLevelConnectionEstablishedEventData.ConnectionType,
                                 EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.ServiceLevelConnectionEstablishedEventData.RemoteSupportedFeaturesValid == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 BitMask,
                                 BitMask2);
                           break;
                        case hetHFRAudioConnected:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_audioConnectedEvent,
                                 EventData->EventData.AudioConnectedEventData.ConnectionType,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AudioConnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRAudioDisconnected:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_audioDisconnectedEvent,
                                 EventData->EventData.AudioDisconnectedEventData.ConnectionType,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AudioDisconnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRAudioConnectionStatus:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_audioConnectionStatusEvent,
                                 EventData->EventData.AudioConnectionStatusEventData.ConnectionType,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AudioConnectionStatusEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.AudioConnectionStatusEventData.Successful == FALSE) ? JNI_FALSE : JNI_TRUE));
                           break;
                        case hetHFRAudioData:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.AudioDataEventData.AudioDataLength)) != 0)
                           {
                              Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.AudioDataEventData.AudioDataLength, (jbyte *)(EventData->EventData.AudioDataEventData.AudioData));
                           }

                           /* Check whether SetByteArrayRegion triggered an*/
                           /* exception before continuing.                 */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_audioDataEvent,
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
                        case hetHFRVoiceRecognitionIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_voiceRecognitionIndicationEvent,
                                 EventData->EventData.VoiceRecognitionIndicationEventData.ConnectionType,
                                 EventData->EventData.VoiceRecognitionIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.VoiceRecognitionIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.VoiceRecognitionIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.VoiceRecognitionIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.VoiceRecognitionIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.VoiceRecognitionIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.VoiceRecognitionIndicationEventData.VoiceRecognitionActive == FALSE) ? JNI_FALSE : JNI_TRUE));
                           break;
                        case hetHFRSpeakerGainIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_speakerGainIndicationEvent,
                                 EventData->EventData.SpeakerGainIndicationEventData.ConnectionType,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.SpeakerGainIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.SpeakerGainIndicationEventData.SpeakerGain);
                           break;
                        case hetHFRMicrophoneGainIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_microphoneGainIndicationEvent,
                                 EventData->EventData.MicrophoneGainIndicationEventData.ConnectionType,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.MicrophoneGainIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.MicrophoneGainIndicationEventData.MicrophoneGain);
                           break;
                        case hetHFRIncomingCallStateIndication:
                           switch(EventData->EventData.IncomingCallStateIndicationEventData.CallState)
                           {
                              case csHold:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_HOLD;
                                 break;
                              case csAccept:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_ACCEPT;
                                 break;
                              case csReject:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_REJECT;
                                 break;
                              case csNone:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_NONE;
                                 break;
                           }

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_incomingCallStateIndicationEvent,
                                 EventData->EventData.IncomingCallStateIndicationEventData.ConnectionType,
                                 EventData->EventData.IncomingCallStateIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.IncomingCallStateIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.IncomingCallStateIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.IncomingCallStateIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.IncomingCallStateIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.IncomingCallStateIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask);
                           break;

                        /* Hands Free specific events.                     */
                        case hetHFRIncomingCallStateConfirmation:
                           switch(EventData->EventData.IncomingCallStateConfirmationEventData.CallState)
                           {
                              case csHold:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_HOLD;
                                 break;
                              case csAccept:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_ACCEPT;
                                 break;
                              case csReject:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_REJECT;
                                 break;
                              case csNone:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_NONE;
                                 break;
                           }

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_incomingCallStateConfirmationEvent,
                                 EventData->EventData.IncomingCallStateConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.IncomingCallStateConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.IncomingCallStateConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.IncomingCallStateConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.IncomingCallStateConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.IncomingCallStateConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask);
                           break;
                        case hetHFRControlIndicatorStatusIndication:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.ControlIndicatorStatusIndicationEventData.ControlIndicatorEntry.IndicatorDescription)) != 0)
                           {
                              if(EventData->EventData.ControlIndicatorStatusIndicationEventData.ControlIndicatorEntry.ControlIndicatorType == ciBoolean)
                              {
                                 Env->CallVoidMethod(HFRMObject, Method_HFRM_controlIndicatorStatusIndicationEvent_Boolean,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                       String,
                                       ((EventData->EventData.ControlIndicatorStatusIndicationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorBooleanType.CurrentIndicatorValue == FALSE) ? JNI_FALSE : JNI_TRUE));
                              }
                              else
                              {
                                 Env->CallVoidMethod(HFRMObject, Method_HFRM_controlIndicatorStatusIndicationEvent_Range,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                       String,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorRangeType.CurrentIndicatorValue,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorRangeType.RangeStart,
                                       EventData->EventData.ControlIndicatorStatusIndicationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorRangeType.RangeEnd);
                              }

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRControlIndicatorStatusConfirmation:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.ControlIndicatorStatusConfirmationEventData.ControlIndicatorEntry.IndicatorDescription)) != 0)
                           {
                              if(EventData->EventData.ControlIndicatorStatusConfirmationEventData.ControlIndicatorEntry.ControlIndicatorType == ciBoolean)
                              {
                                 Env->CallVoidMethod(HFRMObject, Method_HFRM_controlIndicatorStatusConfirmationEvent_Boolean,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                       String,
                                       ((EventData->EventData.ControlIndicatorStatusConfirmationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorBooleanType.CurrentIndicatorValue == FALSE) ? JNI_FALSE : JNI_TRUE));
                              }
                              else
                              {
                                 Env->CallVoidMethod(HFRMObject, Method_HFRM_controlIndicatorStatusConfirmationEvent_Range,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                       String,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorRangeType.CurrentIndicatorValue,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorRangeType.RangeStart,
                                       EventData->EventData.ControlIndicatorStatusConfirmationEventData.ControlIndicatorEntry.Control_Indicator_Data.ControlIndicatorRangeType.RangeEnd);
                              }

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRCallHoldMultipartySupportConfirmation:
                           BitMask = 0;
                           BitMask |= ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMask & HFRE_RELEASE_ALL_HELD_CALLS)                          ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS : 0);
                           BitMask |= ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMask & HFRE_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL)    ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL : 0);
                           BitMask |= ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMask & HFRE_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER) ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER : 0);
                           BitMask |= ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMask & HFRE_ADD_A_HELD_CALL_TO_CONVERSATION)                 ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION : 0);
                           BitMask |= ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMask & HFRE_CONNECT_TWO_CALLS_DISCONNECT_SUBSCRIBER)         ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER : 0);
                           BitMask |= ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMask & HFRE_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY)              ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY : 0);
                           BitMask |= ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMask & HFRE_REQUEST_PRIVATE_CONSULTATION_MODE)               ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE : 0);

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_callHoldMultipartySupportConfirmationEvent,
                                 EventData->EventData.CallHoldMultipartySupportConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.CallHoldMultipartySupportConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.CallHoldMultipartySupportConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.CallHoldMultipartySupportConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.CallHoldMultipartySupportConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.CallHoldMultipartySupportConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.CallHoldMultipartySupportConfirmationEventData.CallHoldSupportMaskValid == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 BitMask);
                           break;
                        case hetHFRCallWaitingNotificationIndication:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.CallWaitingNotificationIndicationEventData.PhoneNumber)) != 0)
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_callWaitingNotificationIndicationEvent,
                                    EventData->EventData.CallWaitingNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.CallWaitingNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.CallWaitingNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.CallWaitingNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.CallWaitingNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.CallWaitingNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRCallLineIdentificationNotificationIndication:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.CallLineIdentificationNotificationIndicationEventData.PhoneNumber)) != 0)
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_callLineIdentificationNotificationIndicationEvent,
                                    EventData->EventData.CallLineIdentificationNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.CallLineIdentificationNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.CallLineIdentificationNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.CallLineIdentificationNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.CallLineIdentificationNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.CallLineIdentificationNotificationIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRRingIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_ringIndicationEvent,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.RingIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRInBandRingToneSettingIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_inBandRingToneSettingIndicationEvent,
                                 EventData->EventData.InBandRingToneSettingIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.InBandRingToneSettingIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.InBandRingToneSettingIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.InBandRingToneSettingIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.InBandRingToneSettingIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.InBandRingToneSettingIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.InBandRingToneSettingIndicationEventData.Enabled == FALSE) ? JNI_FALSE : JNI_TRUE));
                           break;
                        case hetHFRVoiceTagRequestConfirmation:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.VoiceTagRequestConfirmationEventData.PhoneNumber)) != 0)
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_voiceTagRequestConfirmationEvent,
                                    EventData->EventData.VoiceTagRequestConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.VoiceTagRequestConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.VoiceTagRequestConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.VoiceTagRequestConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.VoiceTagRequestConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.VoiceTagRequestConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRCurrentCallsListConfirmation:
                           PRINT_DEBUG("%s: hetHFRCurrentCallsListConfirmation", __FUNCTION__);
                           // FIXME Need to determine whether the event comes in with a full list or just one element.
                           if(EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.PhoneNumber)
                              String = Env->NewStringUTF(EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.PhoneNumber);
                           else
                              String = NULL;

                           if(((EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.PhoneNumber) && (String)) || (!EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.PhoneNumber))
                           {
                              switch(EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.CallDirection)
                              {
                                 case cdMobileOriginated:
                                    BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_DIRECTION_MOBILE_ORIGINATED;
                                    break;
                                 case cdMobileTerminated:
                                 default:
                                    BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_DIRECTION_MOBILE_TERMINATED;
                                    break;
                              }

                              switch(EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.CallStatus)
                              {
                                 case csActive:
                                    BitMask2 = com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_ACTIVE;
                                    break;
                                 case csHeld:
                                    BitMask2 = com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_HELD;
                                    break;
                                 case csDialing:
                                    BitMask2 = com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_DIALING;
                                    break;
                                 case csAlerting:
                                    BitMask2 = com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_ALERTING;
                                    break;
                                 case csIncoming:
                                    BitMask2 = com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_INCOMING;
                                    break;
                                 case csWaiting:
                                 default:
                                    BitMask2 = com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_WAITING;
                                    break;
                              }

                              switch(EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.CallMode)
                              {
                                 case cmVoice:
                                    BitMask3 = com_stonestreetone_bluetopiapm_HFRM_CALL_MODE_VOICE;
                                    break;
                                 case cmData:
                                    BitMask3 = com_stonestreetone_bluetopiapm_HFRM_CALL_MODE_DATA;
                                    break;
                                 case cmFAX:
                                 default:
                                    BitMask3 = com_stonestreetone_bluetopiapm_HFRM_CALL_MODE_FAX;
                                    break;
                              }

                              Env->CallVoidMethod(HFRMObject, Method_HFRM_currentCallsListConfirmationEvent,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.Index,
                                    BitMask,
                                    BitMask2,
                                    BitMask3,
                                    ((EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.Multiparty == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    String,
                                    EventData->EventData.CurrentCallsListConfirmationEventData.CurrentCallListEntry.NumberFormat);

                           }

                           if(String)
                              Env->DeleteLocalRef(String);

                           break;
                        case hetHFRNetworkOperatorSelectionConfirmation:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.NetworkOperatorSelectionConfirmationEventData.NetworkOperator)) != 0)
                           {
                              switch(EventData->EventData.NetworkOperatorSelectionConfirmationEventData.NetworkMode)
                              {
                                 case HFRE_NETWORK_MODE_AUTOMATIC:
                                    BitMask = com_stonestreetone_bluetopiapm_HFRM_NETWORK_MODE_AUTOMATIC;
                                    break;
                                 case HFRE_NETWORK_MODE_MANUAL:
                                    BitMask = com_stonestreetone_bluetopiapm_HFRM_NETWORK_MODE_MANUAL;
                                    break;
                                 case HFRE_NETWORK_MODE_DEREGISTER:
                                    BitMask = com_stonestreetone_bluetopiapm_HFRM_NETWORK_MODE_DEREGISTER;
                                    break;
                                 case HFRE_NETWORK_MODE_SETONLY:
                                    BitMask = com_stonestreetone_bluetopiapm_HFRM_NETWORK_MODE_SETONLY;
                                    break;
                                 case HFRE_NETWORK_MODE_MANUAL_AUTO:
                                 default:
                                    BitMask = com_stonestreetone_bluetopiapm_HFRM_NETWORK_MODE_MANUAL_AUTO;
                                    break;
                              }

                              Env->CallVoidMethod(HFRMObject, Method_HFRM_networkOperatorSelectionConfirmationEvent,
                                    EventData->EventData.NetworkOperatorSelectionConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.NetworkOperatorSelectionConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.NetworkOperatorSelectionConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.NetworkOperatorSelectionConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.NetworkOperatorSelectionConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.NetworkOperatorSelectionConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    BitMask,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRSubscriberNumberInformationConfirmation:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.SubscriberNumberInformationConfirmationEventData.SubscriberNumberInformation.PhoneNumber)) != 0)
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_subscriberNumberInformationConfirmationEvent,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.SubscriberNumberInformation.ServiceType,
                                    EventData->EventData.SubscriberNumberInformationConfirmationEventData.SubscriberNumberInformation.NumberFormat,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRResponseHoldStatusConfirmation:
                           switch(EventData->EventData.ResponseHoldStatusConfirmationEventData.CallState)
                           {
                              case csHold:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_HOLD;
                                 break;
                              case csAccept:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_ACCEPT;
                                 break;
                              case csReject:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_REJECT;
                                 break;
                              case csNone:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_NONE;
                                 break;
                           }

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_responseHoldStatusConfirmationEvent,
                                 EventData->EventData.ResponseHoldStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ResponseHoldStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ResponseHoldStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ResponseHoldStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ResponseHoldStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ResponseHoldStatusConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask);
                           break;
                        case hetHFRCommandResult:
                           switch(EventData->EventData.CommandResultEventData.ResultType)
                           {
                              case erOK:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_OK;
                                 break;
                              case erError:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_ERROR;
                                 break;
                              case erNoCarrier:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_NOCARRIER;
                                 break;
                              case erBusy:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_BUSY;
                                 break;
                              case erNoAnswer:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_NOANSWER;
                                 break;
                              case erDelayed:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_DELAYED;
                                 break;
                              case erBlacklisted:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_BLACKLISTED;
                                 break;
                              case erResultCode:
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_RESULTCODE;
                                 break;
                           }

                           Env->CallVoidMethod(HFRMObject, Method_HFRM_commandResultEvent,
                                 EventData->EventData.CommandResultEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.CommandResultEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.CommandResultEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.CommandResultEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.CommandResultEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.CommandResultEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask,
                                 EventData->EventData.CommandResultEventData.ResultValue);
                           break;
                        case hetHFRArbitraryResponseIndication:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.ArbitraryResponseIndicationEventData.ResponseData)) != 0)
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_arbitraryResponseIndicationEvent,
                                    EventData->EventData.ArbitraryResponseIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.ArbitraryResponseIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.ArbitraryResponseIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.ArbitraryResponseIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.ArbitraryResponseIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.ArbitraryResponseIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;

                        /* Audio Gateway specific events.                  */
                        case hetHFRCallHoldMultipartySelectionIndication:
                           switch(EventData->EventData.CallHoldMultipartySelectionIndicationEventData.CallHoldMultipartyHandling)
                           {
                              case chReleaseAllHeldCalls:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_RELEASE_ALL_HELD_CALLS;
                                 break;
                              case chReleaseAllActiveCallsAcceptWaitingCall:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL;
                                 break;
                              case chPlaceAllActiveCallsOnHoldAcceptTheOther:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER;
                                 break;
                              case chAddAHeldCallToConversation:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_ADD_A_HELD_CALL_TO_CONVERSATION;
                                 break;
                              case chConnectTwoCallsAndDisconnect:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_CONNECT_TWO_CALLS_AND_DISCONNECT;
                                 break;
                              case chReleaseSpecifiedCallIndex:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_RELEASE_SPECIFIED_CALL_INDEX;
                                 break;
                              case chPrivateConsultationMode:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_PRIVATE_CONSULTATION_MODE;
                                 break;
                              default:
                                 BitMask = com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER;
                                 break;
                           }
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_callHoldMultipartySelectionIndicationEvent,
                                 EventData->EventData.CallHoldMultipartySelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.CallHoldMultipartySelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.CallHoldMultipartySelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.CallHoldMultipartySelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.CallHoldMultipartySelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.CallHoldMultipartySelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 BitMask,
                                 EventData->EventData.CallHoldMultipartySelectionIndicationEventData.Index);
                           break;
                        case hetHFRCallWaitingNotificationActivationIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_callWaitingNotificationActivationIndicationEvent,
                                 EventData->EventData.CallWaitingNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.CallWaitingNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.CallWaitingNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.CallWaitingNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.CallWaitingNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.CallWaitingNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 (EventData->EventData.CallWaitingNotificationActivationIndicationEventData.Enabled)?JNI_TRUE:JNI_FALSE);
                           break;
                        case hetHFRCallLineIdentificationNotificationActivationIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_callLineIdentificationNotificationActivationIndicationEvent,
                                 EventData->EventData.CallLineIdentificationNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.CallLineIdentificationNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.CallLineIdentificationNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.CallLineIdentificationNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.CallLineIdentificationNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.CallLineIdentificationNotificationActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 (EventData->EventData.CallLineIdentificationNotificationActivationIndicationEventData.Enabled)?JNI_TRUE:JNI_FALSE);
                           break;
                        case hetHFRDisableSoundEnhancementIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_disableSoundEnhancementIndicationEvent,
                                 EventData->EventData.DisableSoundEnhancementIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.DisableSoundEnhancementIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.DisableSoundEnhancementIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.DisableSoundEnhancementIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.DisableSoundEnhancementIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.DisableSoundEnhancementIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRDialPhoneNumberIndication:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.DialPhoneNumberIndicationEventData.PhoneNumber)) != 0)
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_dialPhoneNumberIndicationEvent,
                                    EventData->EventData.DialPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.DialPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.DialPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.DialPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.DialPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.DialPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;
                        case hetHFRDialPhoneNumberFromMemoryIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_dialPhoneNumberFromMemoryIndicationEvent,
                                 EventData->EventData.DialPhoneNumberFromMemoryIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.DialPhoneNumberFromMemoryIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.DialPhoneNumberFromMemoryIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.DialPhoneNumberFromMemoryIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.DialPhoneNumberFromMemoryIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.DialPhoneNumberFromMemoryIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 (jint)EventData->EventData.DialPhoneNumberFromMemoryIndicationEventData.MemoryLocation);
                           break;
                        case hetHFRReDialLastPhoneNumberIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_redialLastPhoneNumberIndicationEvent,
                                 EventData->EventData.ReDialLastPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ReDialLastPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ReDialLastPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ReDialLastPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ReDialLastPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ReDialLastPhoneNumberIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRGenerateDTMFCodeIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_generateDTMFCodeIndicationEvent,
                                 EventData->EventData.GenerateDTMFCodeIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.GenerateDTMFCodeIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.GenerateDTMFCodeIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.GenerateDTMFCodeIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.GenerateDTMFCodeIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.GenerateDTMFCodeIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.GenerateDTMFCodeIndicationEventData.DTMFCode);
                           break;
                        case hetHFRAnswerCallIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_answerCallIndicationEvent,
                                 EventData->EventData.AnswerCallIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.AnswerCallIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.AnswerCallIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.AnswerCallIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.AnswerCallIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.AnswerCallIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRVoiceTagRequestIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_voiceTagRequestIndicationEvent,
                                 EventData->EventData.VoiceTagRequestIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.VoiceTagRequestIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.VoiceTagRequestIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.VoiceTagRequestIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.VoiceTagRequestIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.VoiceTagRequestIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRHangUpIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_hangUpIndicationEvent,
                                 EventData->EventData.HangUpIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.HangUpIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.HangUpIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.HangUpIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.HangUpIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.HangUpIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRCurrentCallsListIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_currentCallsListIndicationEvent,
                                 EventData->EventData.CurrentCallsListIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.CurrentCallsListIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.CurrentCallsListIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.CurrentCallsListIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.CurrentCallsListIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.CurrentCallsListIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRNetworkOperatorSelectionFormatIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_networkOperatorSelectionFormatIndicationEvent,
                                 EventData->EventData.NetworkOperatorSelectionFormatIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.NetworkOperatorSelectionFormatIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.NetworkOperatorSelectionFormatIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.NetworkOperatorSelectionFormatIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.NetworkOperatorSelectionFormatIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.NetworkOperatorSelectionFormatIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 (jint)EventData->EventData.NetworkOperatorSelectionFormatIndicationEventData.Format);
                           break;
                        case hetHFRNetworkOperatorSelectionIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_networkOperatorSelectionIndicationEvent,
                                 EventData->EventData.NetworkOperatorSelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.NetworkOperatorSelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.NetworkOperatorSelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.NetworkOperatorSelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.NetworkOperatorSelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.NetworkOperatorSelectionIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRExtendedErrorResultActivationIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_extendedErrorResultActivationIndicationEvent,
                                 EventData->EventData.ExtendedErrorResultActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ExtendedErrorResultActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ExtendedErrorResultActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ExtendedErrorResultActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ExtendedErrorResultActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ExtendedErrorResultActivationIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                 (EventData->EventData.ExtendedErrorResultActivationIndicationEventData.Enabled)?JNI_TRUE:JNI_FALSE);
                           break;
                        case hetHFRSubscriberNumberInformationIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_subscriberNumberInformationIndicationEvent,
                                 EventData->EventData.SubscriberNumberInformationIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.SubscriberNumberInformationIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.SubscriberNumberInformationIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.SubscriberNumberInformationIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.SubscriberNumberInformationIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.SubscriberNumberInformationIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRResponseHoldStatusIndication:
                           Env->CallVoidMethod(HFRMObject, Method_HFRM_responseHoldStatusIndicationEvent,
                                 EventData->EventData.ResponseHoldStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ResponseHoldStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ResponseHoldStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ResponseHoldStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ResponseHoldStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ResponseHoldStatusIndicationEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case hetHFRArbitraryCommandIndication:
                           // FIXME Need to convert this UTF string to the "Modifed UTF-8" used by the JNI layer
                           if((String = Env->NewStringUTF(EventData->EventData.ArbitraryCommandIndicationEventData.CommandData)) != 0)
                           {
                              Env->CallVoidMethod(HFRMObject, Method_HFRM_arbitraryCommandIndicationEvent,
                                    EventData->EventData.ArbitraryCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.ArbitraryCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.ArbitraryCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.ArbitraryCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.ArbitraryCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.ArbitraryCommandIndicationEventData.RemoteDeviceAddress.BD_ADDR0,
                                    String);

                              Env->DeleteLocalRef(String);
                           }
                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during HFRM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, HFRMObject, LocalData);
                  }

                  Env->DeleteLocalRef(HFRMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the    */
                  /* HFRM java object. This can happen if the object has   */
                  /* been garbage collected or if the VM has run out of    */
                  /* memory. Now, check whether the object was GC'd. Since */
                  /* NewLocalRef doesn't throw exceptions, we can do this  */
                  /* with IsSameObject.                                    */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The HFRM Manager object has been GC'd. Since we are*/
                     /* still receiving events on this registration, the   */
                     /* Manager was not cleaned up properly.               */
                     PRINT_ERROR("HFRM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it  */
                     /* could indicate a leak in this thread context.      */
                     PRINT_ERROR("VM reports 'Out of Memory' in HFRM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The HFRM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: HFRM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: HFRM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_HFRM = NewWeakRef(Env, Clazz)) != 0)
   {
      if((Field_HFRM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_HFRM_incomingConnectionRequestEvent                              = Env->GetMethodID(Clazz, "incomingConnectionRequestEvent",                              "(IBBBBBB)V");
      Method_HFRM_connectedEvent                                              = Env->GetMethodID(Clazz, "connectedEvent",                                              "(IBBBBBB)V");
      Method_HFRM_disconnectedEvent                                           = Env->GetMethodID(Clazz, "disconnectedEvent",                                           "(IBBBBBBI)V");
      Method_HFRM_connectionStatusEvent                                       = Env->GetMethodID(Clazz, "connectionStatusEvent",                                       "(IBBBBBBI)V");
      Method_HFRM_serviceLevelConnectionEstablishedEvent                      = Env->GetMethodID(Clazz, "serviceLevelConnectionEstablishedEvent",                      "(IBBBBBBZII)V");
      Method_HFRM_audioConnectedEvent                                         = Env->GetMethodID(Clazz, "audioConnectedEvent",                                         "(IBBBBBB)V");
      Method_HFRM_audioDisconnectedEvent                                      = Env->GetMethodID(Clazz, "audioDisconnectedEvent",                                      "(IBBBBBB)V");
      Method_HFRM_audioConnectionStatusEvent                                  = Env->GetMethodID(Clazz, "audioConnectionStatusEvent",                                  "(IBBBBBBZ)V");
      Method_HFRM_audioDataEvent                                              = Env->GetMethodID(Clazz, "audioDataEvent",                                              "(IBBBBBB[B)V");
      Method_HFRM_voiceRecognitionIndicationEvent                             = Env->GetMethodID(Clazz, "voiceRecognitionIndicationEvent",                             "(IBBBBBBZ)V");
      Method_HFRM_speakerGainIndicationEvent                                  = Env->GetMethodID(Clazz, "speakerGainIndicationEvent",                                  "(IBBBBBBI)V");
      Method_HFRM_microphoneGainIndicationEvent                               = Env->GetMethodID(Clazz, "microphoneGainIndicationEvent",                               "(IBBBBBBI)V");
      Method_HFRM_incomingCallStateIndicationEvent                            = Env->GetMethodID(Clazz, "incomingCallStateIndicationEvent",                            "(IBBBBBBI)V");
      Method_HFRM_incomingCallStateConfirmationEvent                          = Env->GetMethodID(Clazz, "incomingCallStateConfirmationEvent",                          "(BBBBBBI)V");
      Method_HFRM_controlIndicatorStatusIndicationEvent_Boolean               = Env->GetMethodID(Clazz, "controlIndicatorStatusIndicationEvent",                       "(BBBBBBLjava/lang/String;Z)V");
      Method_HFRM_controlIndicatorStatusIndicationEvent_Range                 = Env->GetMethodID(Clazz, "controlIndicatorStatusIndicationEvent",                       "(BBBBBBLjava/lang/String;III)V");
      Method_HFRM_controlIndicatorStatusConfirmationEvent_Boolean             = Env->GetMethodID(Clazz, "controlIndicatorStatusConfirmationEvent",                     "(BBBBBBLjava/lang/String;Z)V");
      Method_HFRM_controlIndicatorStatusConfirmationEvent_Range               = Env->GetMethodID(Clazz, "controlIndicatorStatusConfirmationEvent",                     "(BBBBBBLjava/lang/String;III)V");
      Method_HFRM_callHoldMultipartySupportConfirmationEvent                  = Env->GetMethodID(Clazz, "callHoldMultipartySupportConfirmationEvent",                  "(BBBBBBZI)V");
      Method_HFRM_callWaitingNotificationIndicationEvent                      = Env->GetMethodID(Clazz, "callWaitingNotificationIndicationEvent",                      "(BBBBBBLjava/lang/String;)V");
      Method_HFRM_callLineIdentificationNotificationIndicationEvent           = Env->GetMethodID(Clazz, "callLineIdentificationNotificationIndicationEvent",           "(BBBBBBLjava/lang/String;)V");
      Method_HFRM_ringIndicationEvent                                         = Env->GetMethodID(Clazz, "ringIndicationEvent",                                         "(BBBBBB)V");
      Method_HFRM_inBandRingToneSettingIndicationEvent                        = Env->GetMethodID(Clazz, "inBandRingToneSettingIndicationEvent",                        "(BBBBBBZ)V");
      Method_HFRM_voiceTagRequestConfirmationEvent                            = Env->GetMethodID(Clazz, "voiceTagRequestConfirmationEvent",                            "(BBBBBBLjava/lang/String;)V");
      Method_HFRM_currentCallsListConfirmationEvent                           = Env->GetMethodID(Clazz, "currentCallsListConfirmationEvent",                           "(BBBBBBIIIIZLjava/lang/String;I)V");
      Method_HFRM_networkOperatorSelectionConfirmationEvent                   = Env->GetMethodID(Clazz, "networkOperatorSelectionConfirmationEvent",                   "(BBBBBBILjava/lang/String;)V");
      Method_HFRM_subscriberNumberInformationConfirmationEvent                = Env->GetMethodID(Clazz, "subscriberNumberInformationConfirmationEvent",                "(BBBBBBIILjava/lang/String;)V");
      Method_HFRM_responseHoldStatusConfirmationEvent                         = Env->GetMethodID(Clazz, "responseHoldStatusConfirmationEvent",                         "(BBBBBBI)V");
      Method_HFRM_commandResultEvent                                          = Env->GetMethodID(Clazz, "commandResultEvent",                                          "(BBBBBBII)V");
      Method_HFRM_arbitraryResponseIndicationEvent                            = Env->GetMethodID(Clazz, "arbitraryResponseIndicationEvent",                            "(BBBBBBLjava/lang/String;)V");
      Method_HFRM_callHoldMultipartySelectionIndicationEvent                  = Env->GetMethodID(Clazz, "callHoldMultipartySelectionIndicationEvent",                  "(BBBBBBII)V");
      Method_HFRM_callWaitingNotificationActivationIndicationEvent            = Env->GetMethodID(Clazz, "callWaitingNotificationActivationIndicationEvent",            "(BBBBBBZ)V");
      Method_HFRM_callLineIdentificationNotificationActivationIndicationEvent = Env->GetMethodID(Clazz, "callLineIdentificationNotificationActivationIndicationEvent", "(BBBBBBZ)V");
      Method_HFRM_disableSoundEnhancementIndicationEvent                      = Env->GetMethodID(Clazz, "disableSoundEnhancementIndicationEvent",                      "(BBBBBB)V");
      Method_HFRM_dialPhoneNumberIndicationEvent                              = Env->GetMethodID(Clazz, "dialPhoneNumberIndicationEvent",                              "(BBBBBBLjava/lang/String;)V");
      Method_HFRM_dialPhoneNumberFromMemoryIndicationEvent                    = Env->GetMethodID(Clazz, "dialPhoneNumberFromMemoryIndicationEvent",                    "(BBBBBBI)V");
      Method_HFRM_redialLastPhoneNumberIndicationEvent                        = Env->GetMethodID(Clazz, "redialLastPhoneNumberIndicationEvent",                        "(BBBBBB)V");
      Method_HFRM_generateDTMFCodeIndicationEvent                             = Env->GetMethodID(Clazz, "generateDTMFCodeIndicationEvent",                             "(BBBBBBC)V");
      Method_HFRM_answerCallIndicationEvent                                   = Env->GetMethodID(Clazz, "answerCallIndicationEvent",                                   "(BBBBBB)V");
      Method_HFRM_voiceTagRequestIndicationEvent                              = Env->GetMethodID(Clazz, "voiceTagRequestIndicationEvent",                              "(BBBBBB)V");
      Method_HFRM_hangUpIndicationEvent                                       = Env->GetMethodID(Clazz, "hangUpIndicationEvent",                                       "(BBBBBB)V");
      Method_HFRM_currentCallsListIndicationEvent                             = Env->GetMethodID(Clazz, "currentCallsListIndicationEvent",                             "(BBBBBB)V");
      Method_HFRM_networkOperatorSelectionFormatIndicationEvent               = Env->GetMethodID(Clazz, "networkOperatorSelectionFormatIndicationEvent",               "(BBBBBBI)V");
      Method_HFRM_networkOperatorSelectionIndicationEvent                     = Env->GetMethodID(Clazz, "networkOperatorSelectionIndicationEvent",                     "(BBBBBB)V");
      Method_HFRM_extendedErrorResultActivationIndicationEvent                = Env->GetMethodID(Clazz, "extendedErrorResultActivationIndicationEvent",                "(BBBBBBZ)V");
      Method_HFRM_subscriberNumberInformationIndicationEvent                  = Env->GetMethodID(Clazz, "subscriberNumberInformationIndicationEvent",                  "(BBBBBB)V");
      Method_HFRM_responseHoldStatusIndicationEvent                           = Env->GetMethodID(Clazz, "responseHoldStatusIndicationEvent",                           "(BBBBBB)V");
      Method_HFRM_arbitraryCommandIndicationEvent                             = Env->GetMethodID(Clazz, "arbitraryCommandIndicationEvent",                             "(BBBBBBLjava/lang/String;)V");
   }
   else
      PRINT_ERROR("HFRM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.HFRM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
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
               case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
                  RegistrationResult = HFRM_Register_Event_Callback(hctHandsFree, ((Controller == JNI_FALSE) ? FALSE : TRUE), HFRM_EventCallback, LocalData->WeakObjectReference);
                  break;
               case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
               default:
                  RegistrationResult = HFRM_Register_Event_Callback(hctAudioGateway, ((Controller == JNI_FALSE) ? FALSE : TRUE), HFRM_EventCallback, LocalData->WeakObjectReference);
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
                PRINT_ERROR("HFRM: Unable to register callback for events (%d)", RegistrationResult);

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
            PRINT_ERROR("HFRM: Out of Memory: Unable to obtain weak reference to object");
            
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
 * Class:     com_stonestreetone_bluetopiapm_HFRM
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
         HFRM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

      if(LocalData->CallbackID)
         HFRM_Un_Register_Event_Callback(LocalData->CallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    connectionRequestResponseNative
 * Signature: (IBBBBBBZ)I
 */
static jint ConnectionRequestResponseNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean AcceptConnection)
{
   jint                   Result;
   BD_ADDR_t              RemoteDeviceAddress;
   HFRM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return HFRM_Connection_Request_Response(ConnectionType, RemoteDeviceAddress, ((AcceptConnection == JNI_FALSE) ? FALSE : TRUE));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
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
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HFRM_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? HFRM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HFRM_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? HFRM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_ENCRYPTION : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Connect_Remote_Device(ConnectionType, RemoteDeviceAddress, RemoteServerPort, Flags, HFRM_EventCallback, LocalData->WeakObjectReference, 0);

         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   }
   else
   {
      Result = HFRM_Connect_Remote_Device(ConnectionType, RemoteDeviceAddress, RemoteServerPort, Flags, 0, 0, &Status);

      /* If the request was submitted correctly, use the connection  */
      /* status as the return value.                                 */
      if(Result == 0)
         Result = Status;
   }

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    disconnectDeviceNative
 * Signature: (IBBBBBB)I
 */
static jint DisconnectDeviceNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   BD_ADDR_t RemoteDeviceAddress;
   HFRM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   return HFRM_Disconnect_Device(ConnectionType, RemoteDeviceAddress);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
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
   HFRM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   /* Determine the number of connected devices.                        */
   if((Result = HFRM_Query_Connected_Devices(ConnectionType, 0, NULL, &TotalConnected)) >= 0)
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
               Result = HFRM_Query_Connected_Devices(ConnectionType, TotalConnected, AddressBuffer, NULL);

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
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    queryCurrentConfigurationNative
 * Signature: (I)Lcom/stonestreetone/bluetopiapm/LocalConfigurationNative;
 */
static jobject QueryCurrentConfigurationNative(JNIEnv *Env, jobject Object, jint ServerType)
{
   jint                         IncomingConnectionFlags;
   jint                         SupportedFeaturesMask;
   jint                         CallHoldSupportMask;
   jint                         NetworkType;
   jclass                       LocalConfigurationNativeClass;
   jclass                       ConfigurationIndicatorEntryClass;
   jclass                       ConfigurationIndicatorBooleanClass;
   jclass                       ConfigurationIndicatorRangeClass;
   jobject                      LocalConfig;
   jobject                      IndicatorEntry;
   jstring                      IndicatorDescription;
   jmethodID                    LocalConfigurationNativeConstructor;
   jmethodID                    ConfigurationIndicatorBooleanConstructor;
   jmethodID                    ConfigurationIndicatorRangeConstructor;
   jobjectArray                 IndicatorArray;
   unsigned int                 Index;
   unsigned int                 NumberIndicators;
   HFRM_Connection_Type_t       ConnectionType;
   HFRM_Current_Configuration_t CurrentConfiguration;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   LocalConfigurationNativeClass      = NULL;
   ConfigurationIndicatorEntryClass   = NULL;
   ConfigurationIndicatorBooleanClass = NULL;
   ConfigurationIndicatorRangeClass   = NULL;
   LocalConfig                        = NULL;
   IndicatorArray                     = NULL;

   memset(&CurrentConfiguration, 0, sizeof(CurrentConfiguration));

   /* Determine the number of Additional Indicators supported by the    */
   /* service.                                                          */
   if(HFRM_Query_Current_Configuration(ConnectionType, &CurrentConfiguration) == 0)
   {
      NumberIndicators = CurrentConfiguration.TotalNumberAdditionalIndicators;

      memset(&CurrentConfiguration, 0, sizeof(CurrentConfiguration));
      CurrentConfiguration.TotalNumberAdditionalIndicators = NumberIndicators;

      /* Allocate storage for the Indicators.                           */
      if((CurrentConfiguration.AdditionalIndicatorList = (HFRM_Configuration_Indicator_Entry_t *)malloc(NumberIndicators * sizeof(HFRM_Configuration_Indicator_Entry_t))) != NULL)
      {
         /* Obtain references to all the Java classes required to       */
         /* complete this operation.                                    */
         if((LocalConfigurationNativeClass = Env->FindClass("com/stonestreetone/bluetopiapm/HFRM$LocalConfigurationNative")) != NULL)
         {
            if((ConfigurationIndicatorEntryClass = Env->FindClass("com/stonestreetone/bluetopiapm/HFRM$ConfigurationIndicatorEntry")) != NULL)
            {
               if((ConfigurationIndicatorBooleanClass = Env->FindClass("com/stonestreetone/bluetopiapm/HFRM$ConfigurationIndicatorBoolean")) != NULL)
               {
                  if((ConfigurationIndicatorRangeClass = Env->FindClass("com/stonestreetone/bluetopiapm/HFRM$ConfigurationIndicatorRange")) != NULL)
                  {
                     if((LocalConfigurationNativeConstructor = Env->GetMethodID(LocalConfigurationNativeClass, "<init>", "(IIII[Lcom/stonestreetone/bluetopiapm/HFRM$ConfigurationIndicatorEntry;)V")) != NULL)
                     {
                        if((ConfigurationIndicatorBooleanConstructor = Env->GetMethodID(ConfigurationIndicatorBooleanClass, "<init>", "(Ljava/lang/String;Z)V")) != NULL)
                        {
                           if((ConfigurationIndicatorRangeConstructor = Env->GetMethodID(ConfigurationIndicatorRangeClass, "<init>", "(Ljava/lang/String;III)V")) != NULL)
                           {
                              if((IndicatorArray = Env->NewObjectArray(NumberIndicators, ConfigurationIndicatorEntryClass, NULL)) != NULL)
                              {
                                 if(HFRM_Query_Current_Configuration(ConnectionType, &CurrentConfiguration) == 0)
                                 {
                                    IncomingConnectionFlags  = 0;
                                    IncomingConnectionFlags |= ((CurrentConfiguration.IncomingConnectionFlags & HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? com_stonestreetone_bluetopiapm_HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
                                    IncomingConnectionFlags |= ((CurrentConfiguration.IncomingConnectionFlags & HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? com_stonestreetone_bluetopiapm_HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
                                    IncomingConnectionFlags |= ((CurrentConfiguration.IncomingConnectionFlags & HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? com_stonestreetone_bluetopiapm_HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

                                    SupportedFeaturesMask = 0;
                                    CallHoldSupportMask   = 0;

                                    if(ConnectionType == hctHandsFree)
                                    {
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_HF_SOUND_ENHANCEMENT_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_SOUND_ENHANCEMENT              : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_CALL_WAITING_THREE_WAY_CALLING_SUPPORTED_BIT) ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_CALL_WAITING_THREE_WAY_CALLING : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_CLI_SUPPORTED_BIT)                            ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_CLI                            : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_HF_VOICE_RECOGNITION_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_VOICE_RECOGNITION              : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_REMOTE_VOLUME_CONTROL_SUPPORTED_BIT)          ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_REMOTE_VOLUME_CONTROL          : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_HF_ENHANCED_CALL_STATUS_SUPPORTED_BIT)        ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_ENHANCED_CALL_STATUS           : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_HF_ENHANCED_CALL_CONTROL_SUPPORTED_BIT)       ? com_stonestreetone_bluetopiapm_HFRM_HandsFreeServerManager_SUPPORTED_FEATURE_ENHANCED_CALL_CONTROL          : 0);
                                    }
                                    else
                                    {
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_THREE_WAY_CALLING_SUPPORTED_BIT)              ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_THREE_WAY_CALLING              : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_AG_SOUND_ENHANCEMENT_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_SOUND_ENHANCEMENT           : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_AG_VOICE_RECOGNITION_SUPPORTED_BIT)           ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_VOICE_RECOGNITION           : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_INBAND_RINGING_SUPPORTED_BIT)                 ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_INBAND_RINGING                 : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_VOICE_TAGS_SUPPORTED_BIT)                     ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_VOICE_TAGS                     : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_REJECT_CALL_SUPPORT_BIT)                      ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_REJECT_CALL                    : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_AG_ENHANCED_CALL_STATUS_SUPPORTED_BIT)        ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_ENHANCED_CALL_STATUS        : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_AG_ENHANCED_CALL_CONTROL_SUPPORTED_BIT)       ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_ENHANCED_CALL_CONTROL       : 0);
                                       SupportedFeaturesMask |= ((CurrentConfiguration.SupportedFeaturesMask & HFRE_AG_EXTENDED_ERROR_RESULT_CODES_SUPPORTED_BIT) ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_SUPPORTED_FEATURE_AG_EXTENDED_ERROR_RESULT_CODES : 0);

                                       CallHoldSupportMask |= ((CurrentConfiguration.CallHoldingSupportMask & HFRE_RELEASE_ALL_HELD_CALLS)                          ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS                              : 0);
                                       CallHoldSupportMask |= ((CurrentConfiguration.CallHoldingSupportMask & HFRE_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL)    ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL    : 0);
                                       CallHoldSupportMask |= ((CurrentConfiguration.CallHoldingSupportMask & HFRE_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER) ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER : 0);
                                       CallHoldSupportMask |= ((CurrentConfiguration.CallHoldingSupportMask & HFRE_ADD_A_HELD_CALL_TO_CONVERSATION)                 ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION                     : 0);
                                       CallHoldSupportMask |= ((CurrentConfiguration.CallHoldingSupportMask & HFRE_CONNECT_TWO_CALLS_DISCONNECT_SUBSCRIBER)         ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER         : 0);
                                       CallHoldSupportMask |= ((CurrentConfiguration.CallHoldingSupportMask & HFRE_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY)              ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY                  : 0);
                                       CallHoldSupportMask |= ((CurrentConfiguration.CallHoldingSupportMask & HFRE_REQUEST_PRIVATE_CONSULTATION_MODE)               ? com_stonestreetone_bluetopiapm_HFRM_AudioGatewayServerManager_MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE                   : 0);
                                    }

                                    NetworkType = CurrentConfiguration.NetworkType;

                                    for(Index = 0; Index < CurrentConfiguration.NumberAdditionalIndicators; Index++) {
                                       /* Set the Entry object reference*/
                                       /* to NULL so we can detect when */
                                       /* creation of the object fails. */
                                       IndicatorEntry = NULL;

                                       /* Allocate the Java String for  */
                                       /* the indicator description.    */
                                       if((IndicatorDescription = Env->NewStringUTF(CurrentConfiguration.AdditionalIndicatorList[Index].IndicatorDescription)) != NULL)
                                       {
                                          if(CurrentConfiguration.AdditionalIndicatorList[Index].ControlIndicatorType == ciBoolean)
                                          {
                                             /* Construct a Boolean-type*/
                                             /* indicator entry.        */
                                             IndicatorEntry = Env->NewObject(ConfigurationIndicatorBooleanClass, ConfigurationIndicatorBooleanConstructor, IndicatorDescription, ((CurrentConfiguration.AdditionalIndicatorList[Index].Control_Indicator_Data.ControlIndicatorBooleanType.CurrentIndicatorValue == FALSE) ? JNI_FALSE : JNI_TRUE));
                                          }
                                          else
                                          {
                                             /* Construct a Range-type  */
                                             /* indicator entry.        */
                                             IndicatorEntry = Env->NewObject(ConfigurationIndicatorRangeClass, ConfigurationIndicatorRangeConstructor, IndicatorDescription, CurrentConfiguration.AdditionalIndicatorList[Index].Control_Indicator_Data.ControlIndicatorRangeType.CurrentIndicatorValue, CurrentConfiguration.AdditionalIndicatorList[Index].Control_Indicator_Data.ControlIndicatorRangeType.RangeStart, CurrentConfiguration.AdditionalIndicatorList[Index].Control_Indicator_Data.ControlIndicatorRangeType.RangeEnd);
                                          }

                                          /* If the Entry object was    */
                                          /* created successfully, add  */
                                          /* it to the list and release */
                                          /* the reference to it.       */
                                          if(IndicatorEntry)
                                          {
                                             Env->SetObjectArrayElement(IndicatorArray, Index, IndicatorEntry);
                                             Env->DeleteLocalRef(IndicatorEntry);
                                          }
                                          else
                                          {
                                             /* The Entry allocation    */
                                             /* failed, so break out of */
                                             /* the loop.               */
                                             break;
                                          }

                                          /* Release the reference to   */
                                          /* Description string.        */
                                          Env->DeleteLocalRef(IndicatorDescription);
                                       }
                                       else
                                       {
                                          /* The String allocation      */
                                          /* failed, so break out of the*/
                                          /* loop.                      */
                                          break;
                                       }
                                    }

                                    /* Allocate the final configuration */
                                    /* object, but only if the entire   */
                                    /* list was populated and nothing   */
                                    /* has failed up to this point.     */
                                    if((Index == CurrentConfiguration.NumberAdditionalIndicators) && (Env->ExceptionCheck() == JNI_FALSE))
                                       LocalConfig = Env->NewObject(LocalConfigurationNativeClass, LocalConfigurationNativeConstructor, IncomingConnectionFlags, SupportedFeaturesMask, CallHoldSupportMask, NetworkType, IndicatorArray);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         /* Free the indicator storage.                                 */
         free(CurrentConfiguration.AdditionalIndicatorList);
      }
   }

   return LocalConfig;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    changeIncomingConnectionFlagsNative
 * Signature: (II)I
 */
static jint ChangeIncomingConnectionFlagsNative(JNIEnv *Env, jobject Object, jint ServerType, jint ConnectionFlags)
{
   unsigned long          Flags;
   HFRM_Connection_Type_t ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? HFRM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);

   return HFRM_Change_Incoming_Connection_Flags(ConnectionType, Flags);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    disableRemoteEchoCancellationNoiseReductionNative
 * Signature: (IBBBBBB)I
 */
static jint DisableRemoteEchoCancellationNoiseReductionNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Disable_Remote_Echo_Cancellation_Noise_Reduction(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    setRemoteVoiceRecognitionActivationNative
 * Signature: (IBBBBBBZ)I
 */
static jint SetRemoteVoiceRecognitionActivationNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean VoiceRecognitionActive)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Set_Remote_Voice_Recognition_Activation(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress, VoiceRecognitionActive);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    setRemoteSpeakerGainNative
 * Signature: (IBBBBBBI)I
 */
static jint SetRemoteSpeakerGainNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint SpeakerGain)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Set_Remote_Speaker_Gain(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress, SpeakerGain);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    setRemoteMicrophoneGainNative
 * Signature: (IBBBBBBI)I
 */
static jint SetRemoteMicrophoneGainNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint MicrophoneGain)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Set_Remote_Microphone_Gain(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress, MicrophoneGain);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    registerDataEventCallbackNative
 * Signature: (I)I
 */
static jint RegisterDataEventCallbackNative(JNIEnv *Env, jobject Object, jint ServerType)
{
   jint                    Result;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, TRUE)) != NULL)
   {
      if((Result = HFRM_Register_Data_Event_Callback(ConnectionType, HFRM_EventCallback, LocalData->WeakObjectReference)) > 0)
         LocalData->DataCallbackID = (unsigned int)Result;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
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
         HFRM_Un_Register_Data_Event_Callback(LocalData->DataCallbackID);

         LocalData->DataCallbackID = 0;
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    setupAudioConnectionNative
 * Signature: (IBBBBBB)I
 */
static jint SetupAudioConnectionNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Setup_Audio_Connection(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    releaseAudioConnectionNative
 * Signature: (IBBBBBB)I
 */
static jint ReleaseAudioConnectionNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Release_Audio_Connection(LocalData->CallbackID, ConnectionType, RemoteDeviceAddress);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendAudioDataNative
 * Signature: (IBBBBBB[B)I
 */
static jint SendAudioDataNative(JNIEnv *Env, jobject Object, jint ServerType, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jbyteArray AudioData)
{
   jint                    Result;
   jbyte                  *AudioDataBytes;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRM_Connection_Type_t  ConnectionType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ServerType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_HANDSFREE:
         ConnectionType = hctHandsFree;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_SERVER_TYPE_AUDIOGATEWAY:
      default:
         ConnectionType = hctAudioGateway;
         break;
   }

   if((AudioDataBytes = Env->GetByteArrayElements(AudioData, NULL)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Send_Audio_Data(LocalData->DataCallbackID, ConnectionType, RemoteDeviceAddress, Env->GetArrayLength(AudioData), (unsigned char *)AudioDataBytes);

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
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    queryRemoteControlIndicatorStatusNative
 * Signature: (BBBBBB)I
 */
static jint QueryRemoteControlIndicatorStatusNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Query_Remote_Control_Indicator_Status(LocalData->CallbackID, RemoteDeviceAddress);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    enableRemoteIndicatorEventNotificationNative
 * Signature: (BBBBBBZ)I
 */
static jint EnableRemoteIndicatorEventNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean EnableEventNotification)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Enable_Remote_Indicator_Event_Notification(LocalData->CallbackID, RemoteDeviceAddress, ((EnableEventNotification == JNI_FALSE) ? FALSE : TRUE));

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    queryRemoteCallHoldingMultipartyServiceSupportNative
 * Signature: (BBBBBB)I
 */
static jint QueryRemoteCallHoldingMultipartyServiceSupportNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Query_Remote_Call_Holding_Multiparty_Service_Support(LocalData->CallbackID, RemoteDeviceAddress);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendCallHoldingMultipartySelectionNative
 * Signature: (BBBBBBII)I
 */
static jint SendCallHoldingMultipartySelectionNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, int CallHoldMultipartyHandling, jint Index)
{
   jint                                       Result;
   BD_ADDR_t                                  RemoteDeviceAddress;
   LocalData_t                               *LocalData;
   HFRE_Call_Hold_Multiparty_Handling_Type_t  HandlingType;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(CallHoldMultipartyHandling) {
      case com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_RELEASE_ALL_HELD_CALLS:
         HandlingType = chReleaseAllHeldCalls;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL:
         HandlingType = chReleaseAllActiveCallsAcceptWaitingCall;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER:
         HandlingType = chPlaceAllActiveCallsOnHoldAcceptTheOther;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_ADD_A_HELD_CALL_TO_CONVERSATION:
         HandlingType = chAddAHeldCallToConversation;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_CONNECT_TWO_CALLS_AND_DISCONNECT:
         HandlingType = chConnectTwoCallsAndDisconnect;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_RELEASE_SPECIFIED_CALL_INDEX:
         HandlingType = chReleaseSpecifiedCallIndex;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_MULTIPARTY_HANDLING_PRIVATE_CONSULTATION_MODE:
      default:
         HandlingType = chPrivateConsultationMode;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Send_Call_Holding_Multiparty_Selection(LocalData->CallbackID, RemoteDeviceAddress, HandlingType, Index);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    enableRemoteCallWaitingNotificationNative
 * Signature: (BBBBBBZ)I
 */
static jint EnableRemoteCallWaitingNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean EnableNotification)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Enable_Remote_Call_Waiting_Notification(LocalData->CallbackID, RemoteDeviceAddress, ((EnableNotification == JNI_FALSE) ? FALSE : TRUE));

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    enableRemoteCallLineIdentificationNotificationNative
 * Signature: (BBBBBBZ)I
 */
static jint EnableRemoteCallLineIdentificationNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean EnableNotification)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Enable_Remote_Call_Line_Identification_Notification(LocalData->CallbackID, RemoteDeviceAddress, ((EnableNotification == JNI_FALSE) ? FALSE : TRUE));

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    dialPhoneNumberNative
 * Signature: (BBBBBBLjava/lang/String;)I
 */
static jint DialPhoneNumberNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring PhoneNumber)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((String = Env->GetStringUTFChars(PhoneNumber, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Dial_Phone_Number(LocalData->CallbackID, RemoteDeviceAddress, const_cast<char *>(String));
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   
      Env->ReleaseStringUTFChars(PhoneNumber, String);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    dialPhoneNumberFromMemoryNative
 * Signature: (BBBBBBI)I
 */
static jint DialPhoneNumberFromMemoryNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint MemoryLocation)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Dial_Phone_Number_From_Memory(LocalData->CallbackID, RemoteDeviceAddress, MemoryLocation);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    redialLastPhoneNumberNative
 * Signature: (BBBBBB)I
 */
static jint RedialLastPhoneNumberNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Redial_Last_Phone_Number(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    answerIncomingCallNative
 * Signature: (BBBBBB)I
 */
static jint AnswerIncomingCallNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Answer_Incoming_Call(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    transmitDTMFCodeNative
 * Signature: (BBBBBBC)I
 */
static jint TransmitDTMFCodeNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jchar DTMFCode)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Transmit_DTMF_Code(LocalData->CallbackID, RemoteDeviceAddress, DTMFCode);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    voiceTagRequestNative
 * Signature: (BBBBBB)I
 */
static jint VoiceTagRequestNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Voice_Tag_Request(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    hangUpCallNative
 * Signature: (BBBBBB)I
 */
static jint HangUpCallNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Hang_Up_Call(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    queryRemoteCurrentCallsListNative
 * Signature: (BBBBBB)I
 */
static jint QueryRemoteCurrentCallsListNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Query_Remote_Current_Calls_List(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    setNetworkOperatorSelectionFormatNative
 * Signature: (BBBBBB)I
 */
static jint SetNetworkOperatorSelectionFormatNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Set_Network_Operator_Selection_Format(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    queryRemoteNetworkOperatorSelectionNative
 * Signature: (BBBBBB)I
 */
static jint QueryRemoteNetworkOperatorSelectionNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Query_Remote_Network_Operator_Selection(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    enableRemoteExtendedErrorResultNative
 * Signature: (BBBBBBZ)I
 */
static jint EnableRemoteExtendedErrorResultNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean EnableExtendedErrorResults)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Enable_Remote_Extended_Error_Result(LocalData->CallbackID, RemoteDeviceAddress, ((EnableExtendedErrorResults == JNI_FALSE) ? FALSE : TRUE));
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    querySubscriberNumberInformationNative
 * Signature: (BBBBBB)I
 */
static jint QuerySubscriberNumberInformationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Query_Subscriber_Number_Information(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    queryResponseHoldStatusNative
 * Signature: (BBBBBB)I
 */
static jint QueryResponseHoldStatusNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Query_Response_Hold_Status(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    setIncomingCallStateNative
 * Signature: (BBBBBBI)I
 */
static jint SetIncomingCallStateNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, int CallState)
{
   jint               Result;
   BD_ADDR_t          RemoteDeviceAddress;
   LocalData_t       *LocalData;
   HFRE_Call_State_t  State;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(CallState)
   {
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_HOLD:
         State = csHold;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_ACCEPT:
         State = csAccept;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_REJECT:
         State = csReject;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_NONE:
      default:
         State = csNone;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Set_Incoming_Call_State(LocalData->CallbackID, RemoteDeviceAddress, State);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendArbitraryCommandNative
 * Signature: (BBBBBBLjava/lang/String;)I
 */
static jint SendArbitraryCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring ArbitraryCommand)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((String = Env->GetStringUTFChars(ArbitraryCommand, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Send_Arbitrary_Command(LocalData->CallbackID, RemoteDeviceAddress, const_cast<char *>(String));
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
   
      Env->ReleaseStringUTFChars(ArbitraryCommand, String);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    updateCurrentControlIndicatorStatusNative
 * Signature: (BBBBBB[Ljava/lang/String;[I[I)I
 */
static jint UpdateCurrentControlIndicatorStatusNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jobjectArray IndicatorNames, jintArray IndicatorTypes, jintArray IndicatorValues)
{
   jint                      Result;
   jint                     *TypeArray;
   jint                     *ValueArray;
   jsize                     Index;
   jstring                   Name;
   BD_ADDR_t                 RemoteDeviceAddress;
   const char               *NameString;
   LocalData_t              *LocalData;
   unsigned int              Temp;
   HFRE_Indicator_Update_t  *IndicatorUpdates;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   /* Allocate the the update indicator structures.                     */
   if((IndicatorUpdates = (HFRE_Indicator_Update_t *)malloc(HFRE_INDICATOR_UPDATE_SIZE * Env->GetArrayLength(IndicatorTypes))) != NULL)
   {
      /* Get a handle to the array elements.                            */
      if(((TypeArray = Env->GetIntArrayElements(IndicatorTypes, NULL)) != NULL) && ((ValueArray = Env->GetIntArrayElements(IndicatorValues, NULL)) != NULL))
      {
         /* Access each member of the arrays to build the c structures. */
         for(Index=0,Result=0;Index<Env->GetArrayLength(IndicatorTypes) && !Result;Index++)
         {
            /* Attempt to get a handle to the string object.            */
            if((Name = (jstring)(Env->GetObjectArrayElement(IndicatorNames, Index))) != NULL)
            {
               /* Attempt to get the string chars.                      */
               if((NameString = Env->GetStringUTFChars(Name, 0)) != NULL)
               {
                  /* Attempt to allocate a string in the structure.     */
                  if((IndicatorUpdates[Index].IndicatorDescription = (char *)malloc(strlen(NameString) + 1)) != NULL)
                  {
                     strcpy(IndicatorUpdates[Index].IndicatorDescription, NameString);
                     
                     switch(TypeArray[Index])
                     {
                        case com_stonestreetone_bluetopiapm_HFRM_INDICATOR_TYPE_BOOLEAN:
                           IndicatorUpdates[Index].Indicator_Update_Data.CurrentBooleanValue.CurrentIndicatorValue = (ValueArray[Index])?TRUE:FALSE;
                           break;
                        case com_stonestreetone_bluetopiapm_HFRM_INDICATOR_TYPE_RANGE:
                           IndicatorUpdates[Index].Indicator_Update_Data.CurrentRangeValue.CurrentIndicatorValue = (int)ValueArray[Index];
                           break;
                     }
                  }
                  else
                     Result = -1;

                  Env->ReleaseStringUTFChars(Name, NameString);
               }
               else
                  Result = -1;

               Env->DeleteLocalRef(Name);
            }
            else
               Result = -1;
         }

         /* Make sure there was no error.                               */
         if(!Result)
         {   
            if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
            {
               Result = HFRM_Update_Current_Control_Indicator_Status(LocalData->CallbackID, RemoteDeviceAddress, (unsigned int)Env->GetArrayLength(IndicatorTypes), IndicatorUpdates);
   
               ReleaseLocalData(Env, Object, LocalData);
            }
            else
               Result = -1;
         }

         Env->ReleaseIntArrayElements(IndicatorTypes, TypeArray, JNI_ABORT);
         Env->ReleaseIntArrayElements(IndicatorValues, ValueArray, JNI_ABORT);
      }
      else
      {
         /* It's possible we got a handle to the first array and failed */
         /* the second. If so, we need to free it.                      */
         if(TypeArray)
            Env->ReleaseIntArrayElements(IndicatorTypes, TypeArray, JNI_ABORT);

         Result = -1;
      }

      /* Release the memory we allocated.                               */
      free(IndicatorUpdates);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    updateCurrentControlIndicatorStatusByNameNative
 * Signature: (BBBBBBLjava/lang/String;I)I
 */
static jint UpdateCurrentControlIndicatorStatusByNameNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring IndicatorName, jint IndicatorValue)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);


   if((String = Env->GetStringUTFChars(IndicatorName, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Update_Current_Control_Indicator_Status_By_Name(LocalData->CallbackID, RemoteDeviceAddress, const_cast<char *>(String), (unsigned int)IndicatorValue);
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;

      Env->ReleaseStringUTFChars(IndicatorName, String);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendCallWaitingNotificationNative
 * Signature: (BBBBBBLjava/lang/String;)I
 */
static jint SendCallWaitingNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring PhoneNumber)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);


   if((String = Env->GetStringUTFChars(PhoneNumber, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Send_Call_Waiting_Notification(LocalData->CallbackID, RemoteDeviceAddress, const_cast<char *>(String));
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;

      Env->ReleaseStringUTFChars(PhoneNumber, String);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendCallLineIdentificationNotificationNative
 * Signature: (BBBBBBLjava/lang/String;)I
 */
static jint SendCallLineIdentificationNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring PhoneNumber)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);


   if((String = Env->GetStringUTFChars(PhoneNumber, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Send_Call_Line_Identification_Notification(LocalData->CallbackID, RemoteDeviceAddress, const_cast<char *>(String));
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;

      Env->ReleaseStringUTFChars(PhoneNumber, String);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    ringIndicationNative
 * Signature: (BBBBBB)I
 */
static jint RingIndicationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Ring_Indication(LocalData->CallbackID, RemoteDeviceAddress);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    enableRemoteInBandRingToneSettingNative
 * Signature: (BBBBBBZ)I
 */
static jint EnableRemoteInBandRingToneSettingNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean EnableInBandRing)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Enable_Remote_In_Band_Ring_Tone_Setting(LocalData->CallbackID, RemoteDeviceAddress, (EnableInBandRing == JNI_TRUE)?TRUE:FALSE);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    voiceTagResponseNative
 * Signature: (BBBBBBLjava/lang/String;)I
 */
static jint VoiceTagResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring PhoneNumber)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((String = Env->GetStringUTFChars(PhoneNumber, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Voice_Tag_Response(LocalData->CallbackID, RemoteDeviceAddress, const_cast<char *>(String));
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;

      Env->ReleaseStringUTFChars(PhoneNumber, String);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendCurrentCallsListNative
 * Signature: (BBBBBB[I[I[I[I[Z[Ljava/lang/String;[I)I
 */
static jint SendCurrentCallsListNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jintArray Indices, jintArray CallDirections, jintArray CallStatuses, jintArray CallModes, jbooleanArray Multiparties, jobjectArray PhoneNumbers, jintArray NumberFormats)
{
   jint                            Result;
   jint                           *IndexList;
   jint                           *DirectionList;
   jint                           *StatusList;
   jint                           *ModeList;
   jint                           *FormatList;
   jsize                           Index;
   jsize                           Length;
   jboolean                       *MultiList;
   jstring                         Number;
   const char                     *NumberString;
   BD_ADDR_t                       RemoteDeviceAddress;
   LocalData_t                    *LocalData;
   HFRE_Current_Call_List_Entry_t *CurrentCallList;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   IndexList     = NULL;
   DirectionList = NULL;
   StatusList    = NULL;
   ModeList      = NULL;
   FormatList    = NULL;
   MultiList     = NULL;

   Length = Env->GetArrayLength(Indices);

   /* Allocate the Current Call List.                                   */
   if((Length) && ((CurrentCallList = (HFRE_Current_Call_List_Entry_t *)malloc(HFRE_CURRENT_CALL_LIST_ENTRY_SIZE * Length)) != NULL))
   {
      /* Attempt to grab references to the array entries.               */
      if((IndexList = Env->GetIntArrayElements(Indices, NULL)) && (DirectionList = Env->GetIntArrayElements(CallDirections, NULL)) && (StatusList = Env->GetIntArrayElements(CallStatuses, NULL)) && (ModeList = Env->GetIntArrayElements(CallModes, NULL)) && (MultiList = Env->GetBooleanArrayElements(Multiparties, NULL)) && (FormatList = Env->GetIntArrayElements(NumberFormats, NULL)))
      {
         /* Loop through the entries to build to data structure list.   */
         for(Index=0,Result=0;Index<Length && !Result;Index++)
         {
            /* Attempt to get the phone number jstring.                 */
            if((Number = (jstring)Env->GetObjectArrayElement(PhoneNumbers, Index)) != NULL)
            {
               /* Attempt to get the string chars.                      */
               if((NumberString = Env->GetStringUTFChars(Number, 0)) != NULL)
               {
                  /* Build the structure.                               */
                  CurrentCallList[Index].Index = (unsigned int)IndexList[Index];

                  switch(DirectionList[Index])
                  {
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_DIRECTION_MOBILE_ORIGINATED:
                        CurrentCallList[Index].CallDirection = cdMobileOriginated;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_DIRECTION_MOBILE_TERMINATED:
                        CurrentCallList[Index].CallDirection = cdMobileTerminated;
                        break;
                  }

                  switch(StatusList[Index])
                  {
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_ACTIVE:
                        CurrentCallList[Index].CallStatus = csActive;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_HELD:
                        CurrentCallList[Index].CallStatus = csHeld;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_DIALING:
                        CurrentCallList[Index].CallStatus = csDialing;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_ALERTING:
                        CurrentCallList[Index].CallStatus = csAlerting;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_INCOMING:
                        CurrentCallList[Index].CallStatus = csIncoming;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_STATUS_WAITING:
                        CurrentCallList[Index].CallStatus = csWaiting;
                        break;
                  }

                  switch(ModeList[Index])
                  {
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_MODE_VOICE:
                        CurrentCallList[Index].CallMode = cmVoice;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_MODE_DATA:
                        CurrentCallList[Index].CallMode = cmData;
                        break;
                     case com_stonestreetone_bluetopiapm_HFRM_CALL_MODE_FAX:
                        CurrentCallList[Index].CallMode = cmFAX;
                        break;
                  }

                  CurrentCallList[Index].Multiparty   = (MultiList[Index] == JNI_TRUE)?TRUE:FALSE;
                  CurrentCallList[Index].NumberFormat = (unsigned int)FormatList[Index];

                  /* Copy the local string into the structure so we can */
                  /* release it.                                        */
                  if((CurrentCallList[Index].PhoneNumber = (char *)malloc(strlen(NumberString) + 1)) != NULL)
                     strcpy(CurrentCallList[Index].PhoneNumber, NumberString);
                  else
                     Result = -1;
                        
                  Env->ReleaseStringUTFChars(Number, NumberString);
               }
               else
                  Result = -1;
               Env->DeleteLocalRef(Number);
            }
            else
               Result = -1;
         }

         /* We've built the structure, so send the command if there was */
         /* no error.                                                   */
         if(!Result)
         {   
            if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
            {
               Result = HFRM_Send_Current_Calls_List(LocalData->CallbackID, RemoteDeviceAddress, Length, CurrentCallList);
   
               ReleaseLocalData(Env, Object, LocalData);
            }
            else
               Result = -1;
         }

         /* Now free all of the arrays.                                 */
         Env->ReleaseIntArrayElements(Indices, IndexList, JNI_ABORT);
         Env->ReleaseIntArrayElements(CallDirections, DirectionList, JNI_ABORT);
         Env->ReleaseIntArrayElements(CallStatuses, StatusList, JNI_ABORT);
         Env->ReleaseIntArrayElements(CallModes, ModeList, JNI_ABORT);
         Env->ReleaseBooleanArrayElements(Multiparties, MultiList, JNI_ABORT);
         Env->ReleaseIntArrayElements(NumberFormats, FormatList, JNI_ABORT);
      }
      else
      {
         /* Make sure we free any references we were able to get a hold */
         /* of.                                                         */
         if(IndexList)
            Env->ReleaseIntArrayElements(Indices, IndexList, JNI_ABORT);

         if(DirectionList)
            Env->ReleaseIntArrayElements(CallDirections, DirectionList, JNI_ABORT);

         if(StatusList)
            Env->ReleaseIntArrayElements(CallStatuses, StatusList, JNI_ABORT);

         if(ModeList)
            Env->ReleaseIntArrayElements(CallModes, ModeList, JNI_ABORT);

         if(FormatList)
            Env->ReleaseIntArrayElements(NumberFormats, FormatList, JNI_ABORT);

         if(MultiList)
            Env->ReleaseBooleanArrayElements(Multiparties, MultiList, JNI_ABORT);

         Result = -1;
      }
      free(CurrentCallList);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendNetworkOperatorSelectionNative
 * Signature: (BBBBBBILjava/lang/String;)I
 */
static jint SendNetworkOperatorSelectionNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint NetworkMode, jstring NetworkOperator)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((String = Env->GetStringUTFChars(NetworkOperator, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Send_Network_Operator_Selection(LocalData->CallbackID, RemoteDeviceAddress, NetworkMode, const_cast<char *>(String));
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;
      
      Env->ReleaseStringUTFChars(NetworkOperator, String);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendExtendedErrorResultNative
 * Signature: (BBBBBBI)I
 */
static jint SendExtendedErrorResultNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ResultCode)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Send_Extended_Error_Result(LocalData->CallbackID, RemoteDeviceAddress, (Byte_t)ResultCode);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendSubscriberNumberInformationNative
 * Signature: (BBBBBB[I[I[Ljava/lang/String;)I
 */
static jint SendSubscriberNumberInformationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jintArray ServiceTypes, jintArray NumberFormats, jobjectArray PhoneNumbers)
{
   jint                                  Result;
   jint                                 *ServiceList;
   jint                                 *FormatList;
   jstring                               Number;
   BD_ADDR_t                             RemoteDeviceAddress;
   const char                           *NumberString;
   LocalData_t                          *LocalData;
   unsigned int                          Index;
   unsigned int                          Length;
   HFRM_Subscriber_Number_Information_t *SubscriberInfoList;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Length = Env->GetArrayLength(ServiceTypes);

   if((Length) && ((SubscriberInfoList = (HFRM_Subscriber_Number_Information_t *)malloc(sizeof(HFRM_Subscriber_Number_Information_t) * Length)) != NULL))
   {
      if((ServiceList = Env->GetIntArrayElements(ServiceTypes, NULL)) && (FormatList = Env->GetIntArrayElements(NumberFormats, NULL)))
      {
         PRINT_DEBUG("Length %u\n", Length);
         for(Index=0,Result=0;Index<Length && !Result;Index++)
         {
            PRINT_DEBUG("Index: %u\n", Index);
            if((Number = (jstring)Env->GetObjectArrayElement(PhoneNumbers, Index)) != NULL)
            {
               if((NumberString = Env->GetStringUTFChars(Number, 0)) != NULL)
               {     
                  SubscriberInfoList[Index].ServiceType  = (unsigned int)ServiceList[Index];
                  SubscriberInfoList[Index].NumberFormat = (unsigned int)FormatList[Index];

                  if((SubscriberInfoList[Index].PhoneNumber = (char *)malloc(strlen(NumberString) + 1)) != NULL)
                     strcpy(SubscriberInfoList[Index].PhoneNumber, NumberString);
                  else
                     Result = -1;
                  
                  Env->ReleaseStringUTFChars(Number, NumberString);
               }
               else
                  Result = -1;

               Env->DeleteLocalRef(Number);
            }
            else
               Result = -1;
         }

         if(!Result)
         {
            if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
            {
               Result = HFRM_Send_Subscriber_Number_Information(LocalData->CallbackID, RemoteDeviceAddress, Length, SubscriberInfoList);
   
               ReleaseLocalData(Env, Object, LocalData);
            }
            else
               Result = -1;
         }

         Env->ReleaseIntArrayElements(ServiceTypes, ServiceList, JNI_ABORT);
         Env->ReleaseIntArrayElements(NumberFormats, FormatList, JNI_ABORT);
      }
      else
      {
         /* Make sure we dont have a reference to the first list.       */
         if(ServiceList)
            Env->ReleaseIntArrayElements(ServiceTypes, ServiceList, JNI_ABORT);

         Result = -1;
      }
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendIncomingCallStateNative
 * Signature: (BBBBBBI)I
 */
static jint SendIncomingCallStateNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint CallState)
{
   jint               Result;
   BD_ADDR_t          RemoteDeviceAddress;
   LocalData_t       *LocalData;
   HFRE_Call_State_t  State;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(CallState)
   {
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_HOLD:
         State = csHold;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_ACCEPT:
         State = csAccept;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_REJECT:
         State = csReject;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_CALL_STATE_NONE:
         State = csNone;
         break;
      default:
         State = csNone;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Send_Incoming_Call_State(LocalData->CallbackID, RemoteDeviceAddress, State);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendTerminatingResponseNative
 * Signature: (BBBBBBII)I
 */
static jint SendTerminatingResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ResultType, jint ResultValue)
{
   jint                    Result;
   BD_ADDR_t               RemoteDeviceAddress;
   LocalData_t            *LocalData;
   HFRE_Extended_Result_t  Type;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   switch(ResultType)
   {
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_OK:
         Type = erOK;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_ERROR:
         Type = erError;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_NOCARRIER:
         Type = erNoCarrier;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_BUSY:
         Type = erBusy;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_NOANSWER:
         Type = erNoAnswer;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_DELAYED:
         Type = erDelayed;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_BLACKLISTED:
         Type = erBlacklisted;
         break;
      case com_stonestreetone_bluetopiapm_HFRM_EXTENDED_RESULT_RESULTCODE:
         Type = erResultCode;
         break;
      default:
         Type = erError;
         break;
   }

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Send_Terminating_Response(LocalData->CallbackID, RemoteDeviceAddress, Type, ResultValue);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    enableArbitraryCommandProcessingNative
 * Signature: ()I
 */
static jint EnableArbitraryCommandProcessingNative(JNIEnv *Env, jobject Object)
{
   jint         Result;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = HFRM_Enable_Arbitrary_Command_Processing(LocalData->CallbackID);
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_HFRM
 * Method:    sendArbitraryResponseNative
 * Signature: (BBBBBBLjava/lang/String;)I
 */
static jint SendArbitraryResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jstring ArbitraryResponse)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   const char  *String;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);


   if((String = Env->GetStringUTFChars(ArbitraryResponse, 0)) != NULL)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = HFRM_Send_Arbitrary_Response(LocalData->CallbackID, RemoteDeviceAddress, const_cast<char *>(String));
   
         ReleaseLocalData(Env, Object, LocalData);
      }
      else
         Result = -1;

      Env->ReleaseStringUTFChars(ArbitraryResponse, String);
   }
   else
      Result = -1;

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                                      "()V",                                                                          (void*) InitClassNative},
   {"initObjectNative",                                     "(IZ)V",                                                                        (void*) InitObjectNative},
   {"cleanupObjectNative",                                  "()V",                                                                          (void*) CleanupObjectNative},
   {"connectionRequestResponseNative",                      "(IBBBBBBZ)I",                                                                  (void*) ConnectionRequestResponseNative},
   {"connectRemoteDeviceNative",                            "(IBBBBBBIIZ)I",                                                                (void*) ConnectRemoteDeviceNative},
   {"disconnectDeviceNative",                               "(IBBBBBB)I",                                                                   (void*) DisconnectDeviceNative},
   {"queryConnectedDevicesNative",                          "(I)[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;",                        (void*) QueryConnectedDevicesNative},
   {"queryCurrentConfigurationNative",                      "(I)Lcom/stonestreetone/bluetopiapm/HFRM$LocalConfigurationNative;",            (void*) QueryCurrentConfigurationNative},
   {"changeIncomingConnectionFlagsNative",                  "(II)I",                                                                        (void*) ChangeIncomingConnectionFlagsNative},
   {"disableRemoteEchoCancellationNoiseReductionNative",    "(IBBBBBB)I",                                                                   (void*) DisableRemoteEchoCancellationNoiseReductionNative},
   {"setRemoteVoiceRecognitionActivationNative",            "(IBBBBBBZ)I",                                                                  (void*) SetRemoteVoiceRecognitionActivationNative},
   {"setRemoteSpeakerGainNative",                           "(IBBBBBBI)I",                                                                  (void*) SetRemoteSpeakerGainNative},
   {"setRemoteMicrophoneGainNative",                        "(IBBBBBBI)I",                                                                  (void*) SetRemoteMicrophoneGainNative},
   {"registerDataEventCallbackNative",                      "(I)I",                                                                         (void*) RegisterDataEventCallbackNative},
   {"unregisterDataEventCallbackNative",                    "()V",                                                                          (void*) UnregisterDataEventCallbackNative},
   {"setupAudioConnectionNative",                           "(IBBBBBB)I",                                                                   (void*) SetupAudioConnectionNative},
   {"releaseAudioConnectionNative",                         "(IBBBBBB)I",                                                                   (void*) ReleaseAudioConnectionNative},
   {"sendAudioDataNative",                                  "(IBBBBBB[B)I",                                                                 (void*) SendAudioDataNative},
   {"queryRemoteControlIndicatorStatusNative",              "(BBBBBB)I",                                                                    (void*) QueryRemoteControlIndicatorStatusNative},
   {"enableRemoteIndicatorEventNotificationNative",         "(BBBBBBZ)I",                                                                   (void*) EnableRemoteIndicatorEventNotificationNative},
   {"queryRemoteCallHoldingMultipartyServiceSupportNative", "(BBBBBB)I",                                                                    (void*) QueryRemoteCallHoldingMultipartyServiceSupportNative},
   {"sendCallHoldingMultipartySelectionNative",             "(BBBBBBII)I",                                                                  (void*) SendCallHoldingMultipartySelectionNative},
   {"enableRemoteCallWaitingNotificationNative",            "(BBBBBBZ)I",                                                                   (void*) EnableRemoteCallWaitingNotificationNative},
   {"enableRemoteCallLineIdentificationNotificationNative", "(BBBBBBZ)I",                                                                   (void*) EnableRemoteCallLineIdentificationNotificationNative},
   {"dialPhoneNumberNative",                                "(BBBBBBLjava/lang/String;)I",                                                  (void*) DialPhoneNumberNative},
   {"dialPhoneNumberFromMemoryNative",                      "(BBBBBBI)I",                                                                   (void*) DialPhoneNumberFromMemoryNative},
   {"redialLastPhoneNumberNative",                          "(BBBBBB)I",                                                                    (void*) RedialLastPhoneNumberNative},
   {"answerIncomingCallNative",                             "(BBBBBB)I",                                                                    (void*) AnswerIncomingCallNative},
   {"transmitDTMFCodeNative",                               "(BBBBBBC)I",                                                                   (void*) TransmitDTMFCodeNative},
   {"voiceTagRequestNative",                                "(BBBBBB)I",                                                                    (void*) VoiceTagRequestNative},
   {"hangUpCallNative",                                     "(BBBBBB)I",                                                                    (void*) HangUpCallNative},
   {"queryRemoteCurrentCallsListNative",                    "(BBBBBB)I",                                                                    (void*) QueryRemoteCurrentCallsListNative},
   {"setNetworkOperatorSelectionFormatNative",              "(BBBBBB)I",                                                                    (void*) SetNetworkOperatorSelectionFormatNative},
   {"queryRemoteNetworkOperatorSelectionNative",            "(BBBBBB)I",                                                                    (void*) QueryRemoteNetworkOperatorSelectionNative},
   {"enableRemoteExtendedErrorResultNative",                "(BBBBBBZ)I",                                                                   (void*) EnableRemoteExtendedErrorResultNative},
   {"querySubscriberNumberInformationNative",               "(BBBBBB)I",                                                                    (void*) QuerySubscriberNumberInformationNative},
   {"queryResponseHoldStatusNative",                        "(BBBBBB)I",                                                                    (void*) QueryResponseHoldStatusNative},
   {"setIncomingCallStateNative",                           "(BBBBBBI)I",                                                                   (void*) SetIncomingCallStateNative},
   {"sendArbitraryCommandNative",                           "(BBBBBBLjava/lang/String;)I",                                                  (void*) SendArbitraryCommandNative},
   {"updateCurrentControlIndicatorStatusNative",            "(BBBBBB[Ljava/lang/String;[I[I)I",                                             (void*) UpdateCurrentControlIndicatorStatusNative},
   {"updateCurrentControlIndicatorStatusByNameNative",      "(BBBBBBLjava/lang/String;I)I",                                                 (void*) UpdateCurrentControlIndicatorStatusByNameNative},
   {"sendCallWaitingNotificationNative",                    "(BBBBBBLjava/lang/String;)I",                                                  (void*) SendCallWaitingNotificationNative},
   {"sendCallLineIdentificationNotificationNative",         "(BBBBBBLjava/lang/String;)I",                                                  (void*) SendCallLineIdentificationNotificationNative},
   {"ringIndicationNative",                                 "(BBBBBB)I",                                                                    (void*) RingIndicationNative},
   {"enableRemoteInBandRingToneSettingNative",              "(BBBBBBZ)I",                                                                   (void*) EnableRemoteInBandRingToneSettingNative},
   {"voiceTagResponseNative",                               "(BBBBBBLjava/lang/String;)I",                                                  (void*) VoiceTagResponseNative},
   {"sendCurrentCallsListNative",                           "(BBBBBB[I[I[I[I[Z[Ljava/lang/String;[I)I",                                     (void*) SendCurrentCallsListNative},
   {"sendNetworkOperatorSelectionNative",                   "(BBBBBBILjava/lang/String;)I",                                                 (void*) SendNetworkOperatorSelectionNative},
   {"sendExtendedErrorResultNative",                        "(BBBBBBI)I",                                                                   (void*) SendExtendedErrorResultNative},
   {"sendSubscriberNumberInformationNative",                "(BBBBBB[I[I[Ljava/lang/String;)I",                                             (void*) SendSubscriberNumberInformationNative},
   {"sendIncomingCallStateNative",                          "(BBBBBBI)I",                                                                   (void*) SendIncomingCallStateNative},
   {"sendTerminatingResponseNative",                        "(BBBBBBII)I",                                                                  (void*) SendTerminatingResponseNative},
   {"enableArbitraryCommandProcessingNative",               "()I",                                                                          (void*) EnableArbitraryCommandProcessingNative},
   {"sendArbitraryResponseNative",                          "(BBBBBBLjava/lang/String;)I",                                                  (void*) SendArbitraryResponseNative},
};

int register_com_stonestreetone_bluetopiapm_HFRM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/HFRM";

   Result = -1;

   PRINT_DEBUG("Registering HFRM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}
