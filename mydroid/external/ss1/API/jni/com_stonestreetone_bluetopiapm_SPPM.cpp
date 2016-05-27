/*****< com_stonestreetone_bluetopiapm_SPPM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_SPPM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager SPPM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   03/20/12  G. Hensley     Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>
#include <limits.h>

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_SPPM.h"

static WEAK_REF Class_SPPM;

static jfieldID Field_SPPM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_disconnectedEvent;
static jmethodID Method_lineStatusChangedEvent;
static jmethodID Method_portStatusChangedEvent;
static jmethodID Method_dataReceivedEvent;
static jmethodID Method_transmitBufferEmptyEvent;
static jmethodID Method_idpsStatusEvent;
static jmethodID Method_sessionOpenRequestEvent;
static jmethodID Method_sessionCloseEvent;
static jmethodID Method_sessionDataReceivedEvent;
static jmethodID Method_sessionDataConfirmationEvent;
static jmethodID Method_nonSessionDataReceivedEvent;
static jmethodID Method_nonSessionDataConfirmationEvent;
static jmethodID Method_connectionRequestEvent;
static jmethodID Method_connectedEvent;
static jmethodID Method_connectionStatusEvent;
static jmethodID Method_unhandledControlMessageReceivedEvent;

typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int PortHandle;
   Boolean_t    IsServer;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_SPPM_localData, Exclusive)) == NULL)
      PRINT_ERROR("SPPM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_SPPM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_SPPM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void SPPM_EventCallback(SPPM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         IntVal1;
   jint         IntVal2;
   jbyteArray   ByteArray;
   JNIEnv      *Env;
   jobject      SPPMObject;
   LocalData_t *LocalData;

   if((EventData) && (CallbackParameter))
   {
      if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
      {
         /* The CallbackParameter is a weak ref to our SPPM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((SPPMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, SPPMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case setServerPortOpenRequest:
                           Env->CallVoidMethod(SPPMObject, Method_connectionRequestEvent,
                                 (jint)(EventData->EventData.ServerPortOpenRequestEventData.PortHandle),
                                 EventData->EventData.ServerPortOpenRequestEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ServerPortOpenRequestEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ServerPortOpenRequestEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ServerPortOpenRequestEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ServerPortOpenRequestEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ServerPortOpenRequestEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case setServerPortOpen:
                           switch(EventData->EventData.ServerPortOpenEventData.ConnectionType)
                           {
                              case sctMFi:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_CONNECTION_TYPE_MFI;
                                 break;
                              case sctSPP:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_CONNECTION_TYPE_SPP;
                                 break;
                           }

                           Env->CallVoidMethod(SPPMObject, Method_connectedEvent,
                                 (jint)(EventData->EventData.ServerPortOpenEventData.PortHandle),
                                 EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR0,
                                 IntVal1);
                           break;
                        case setPortClose:
                           Env->CallVoidMethod(SPPMObject, Method_disconnectedEvent,
                                 (jint)(EventData->EventData.PortCloseEventData.PortHandle));
                           break;
                        case setRemotePortOpenStatus:
                           switch(EventData->EventData.RemotePortOpenStatusEventData.Status)
                           {
                              case SPPM_OPEN_REMOTE_PORT_STATUS_SUCCESS:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_STATUS_SUCCESS;
                                 break;
                              case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_TIMEOUT:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_STATUS_FAILURE_TIMEOUT;
                                 break;
                              case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_REFUSED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_STATUS_FAILURE_REFUSED;
                                 break;
                              case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_SECURITY:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_STATUS_FAILURE_SECURITY;
                                 break;
                              case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_DEVICE_POWER_OFF:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF;
                                 break;
                              case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_UNKNOWN:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_STATUS_FAILURE_UNKNOWN;
                                 break;
                           }

                           switch(EventData->EventData.RemotePortOpenStatusEventData.ConnectionType)
                           {
                              case sctMFi:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_CONNECTION_TYPE_MFI;
                                 break;
                              case sctSPP:
                              default:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_CONNECTION_TYPE_SPP;
                                 break;
                           }

                           Env->CallVoidMethod(SPPMObject, Method_connectionStatusEvent,
                                 (jint)(EventData->EventData.RemotePortOpenStatusEventData.PortHandle),
                                 IntVal1,
                                 IntVal2);
                           break;
                        case setLineStatusChanged:
                           IntVal1  = ((EventData->EventData.LineStatusChangedEventData.LineStatusMask & SPPM_LINE_STATUS_MASK_NO_ERROR_VALUE)     ? com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_NO_ERROR_VALUE     : 0);
                           IntVal1 |= ((EventData->EventData.LineStatusChangedEventData.LineStatusMask & SPPM_LINE_STATUS_MASK_OVERRUN_ERROR_MASK) ? com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_OVERRUN_ERROR_MASK : 0);
                           IntVal1 |= ((EventData->EventData.LineStatusChangedEventData.LineStatusMask & SPPM_LINE_STATUS_MASK_PARITY_ERROR_MASK)  ? com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_PARITY_ERROR_MASK  : 0);
                           IntVal1 |= ((EventData->EventData.LineStatusChangedEventData.LineStatusMask & SPPM_LINE_STATUS_MASK_FRAMING_ERROR_MASK) ? com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_FRAMING_ERROR_MASK : 0);

                           Env->CallVoidMethod(SPPMObject, Method_lineStatusChangedEvent,
                                 (jint)(EventData->EventData.LineStatusChangedEventData.PortHandle),
                                 IntVal1);
                           break;
                        case setPortStatusChanged:
                           IntVal1  = ((EventData->EventData.PortStatusChangedEventData.PortStatus.PortStatusMask & SPPM_PORT_STATUS_MASK_NO_ERROR_VALUE)      ? com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_NO_ERROR_VALUE      : 0);
                           IntVal1 |= ((EventData->EventData.PortStatusChangedEventData.PortStatus.PortStatusMask & SPPM_PORT_STATUS_MASK_RTS_CTS_MASK)        ? com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_RTS_CTS_MASK        : 0);
                           IntVal1 |= ((EventData->EventData.PortStatusChangedEventData.PortStatus.PortStatusMask & SPPM_PORT_STATUS_MASK_DTR_DSR_MASK)        ? com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_DTR_DSR_MASK        : 0);
                           IntVal1 |= ((EventData->EventData.PortStatusChangedEventData.PortStatus.PortStatusMask & SPPM_PORT_STATUS_MASK_RING_INDICATOR_MASK) ? com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_RING_INDICATOR_MASK : 0);
                           IntVal1 |= ((EventData->EventData.PortStatusChangedEventData.PortStatus.PortStatusMask & SPPM_PORT_STATUS_MASK_CARRIER_DETECT_MASK) ? com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_CARRIER_DETECT_MASK : 0);

                           Env->CallVoidMethod(SPPMObject, Method_portStatusChangedEvent,
                                 (jint)(EventData->EventData.PortStatusChangedEventData.PortHandle),
                                 IntVal1,
                                 ((EventData->EventData.PortStatusChangedEventData.PortStatus.BreakSignal == JNI_FALSE) ? FALSE : TRUE),
                                 (jint)(EventData->EventData.PortStatusChangedEventData.PortStatus.BreakTimeout));
                           break;
                        case setDataReceived:
                           Env->CallVoidMethod(SPPMObject, Method_dataReceivedEvent,
                                 (jint)(EventData->EventData.DataReceivedEventData.PortHandle),
                                 (jint)(EventData->EventData.DataReceivedEventData.DataLength));
                           break;
                        case setTransmitBufferEmpty:
                           Env->CallVoidMethod(SPPMObject, Method_transmitBufferEmptyEvent,
                                 (jint)(EventData->EventData.TransmitBufferEmptyEventData.PortHandle));
                           break;
                        case setIDPSStatus:
                           switch(EventData->EventData.IDPSStatusEventData.IDPSState)
                           {
                              case isStartIdentificationRequest:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATE_START_IDENTIFICATION_REQUEST;
                                 break;
                              case isStartIdentificationProcess:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATE_START_IDENTIFICATION_PROCESS;
                                 break;
                              case isIdentificationProcess:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATE_IDENTIFICATION_PROCESS;
                                 break;
                              case isIdentificationProcessComplete:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATE_IDENTIFICATION_PROCESS_COMPLETE;
                                 break;
                              case isStartAuthenticationProcess:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATE_START_AUTHENTICATION_PROCESS;
                                 break;
                              case isAuthenticationProcess:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATE_AUTHENTICATION_PROCESS;
                                 break;
                              case isAuthenticationProcessComplete:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATE_AUTHENTICATION_PROCESS_COMPLETE;
                                 break;
                              default:
                                 IntVal1 = -1;
                                 break;
                           }

                           switch(EventData->EventData.IDPSStatusEventData.Status)
                           {
                              case SPPM_IDPS_STATUS_SUCCESS:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATUS_SUCCESS;
                                 break;
                              case SPPM_IDPS_STATUS_STATUS_ERROR_RETRYING:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATUS_ERROR_RETRYING;
                                 break;
                              case SPPM_IDPS_STATUS_STATUS_TIMEOUT_HALTING:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATUS_TIMEOUT_HALTING;
                                 break;
                              case SPPM_IDPS_STATUS_STATUS_GENERAL_FAILURE:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATUS_GENERAL_FAILURE;
                                 break;
                              case SPPM_IDPS_STATUS_STATUS_PROCESS_FAILURE:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATUS_PROCESS_FAILURE;
                                 break;
                              case SPPM_IDPS_STATUS_STATUS_PROCESS_TIMEOUT_RETRYING:
                                 IntVal2 = com_stonestreetone_bluetopiapm_SPPM_IDPS_STATUS_PROCESS_TIMEOUT_RETRYING;
                                 break;
                              default:
                                 IntVal2 = -1;
                                 break;
                           }

                           Env->CallVoidMethod(SPPMObject, Method_idpsStatusEvent,
                                 (jint)(EventData->EventData.IDPSStatusEventData.PortHandle),
                                 IntVal1,
                                 IntVal2);
                           break;
                        case setSessionOpenRequest:
                           Env->CallVoidMethod(SPPMObject, Method_sessionOpenRequestEvent,
                                 (jint)(EventData->EventData.SessionOpenRequestEventData.PortHandle),
                                 (jint)(EventData->EventData.SessionOpenRequestEventData.MaximimTransmitPacket),
                                 (jint)(EventData->EventData.SessionOpenRequestEventData.MaximumReceivePacket),
                                 (jint)(EventData->EventData.SessionOpenRequestEventData.SessionID),
                                 (jint)(EventData->EventData.SessionOpenRequestEventData.ProtocolIndex));
                           break;
                        case setSessionClose:
                           Env->CallVoidMethod(SPPMObject, Method_sessionCloseEvent,
                                 (jint)(EventData->EventData.SessionOpenRequestEventData.PortHandle),
                                 (jint)(EventData->EventData.SessionOpenRequestEventData.SessionID));
                           break;
                        case setSessionDataReceived:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.SessionDataReceivedEventData.SessionDataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.SessionDataReceivedEventData.SessionDataLength), (jbyte*)(EventData->EventData.SessionDataReceivedEventData.SessionDataBuffer));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an      */
                           /* exception before continuing.                       */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(SPPMObject, Method_sessionDataReceivedEvent,
                                    (jint)(EventData->EventData.SessionDataReceivedEventData.PortHandle),
                                    (jint)(EventData->EventData.SessionDataReceivedEventData.SessionID),
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
                        case setSessionDataConfirmation:
                           switch(EventData->EventData.SessionDataConfirmationEventData.Status)
                           {
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_SENT:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_SENT;
                                 break;
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_ACKNOWLEDGED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_ACKNOWLEDGED;
                                 break;
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_FAILED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_FAILED;
                                 break;
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_CANCELED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_CANCELED;
                                 break;
                              default:
                                 IntVal1 = -1;
                                 break;
                           }

                           Env->CallVoidMethod(SPPMObject, Method_sessionDataConfirmationEvent,
                                 (jint)(EventData->EventData.SessionDataConfirmationEventData.PortHandle),
                                 (jint)(EventData->EventData.SessionDataConfirmationEventData.SessionID),
                                 (jint)(EventData->EventData.SessionDataConfirmationEventData.PacketID),
                                 IntVal1);
                           break;
                        case setNonSessionDataReceived:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.NonSessionDataReceivedEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.NonSessionDataReceivedEventData.DataLength), (jbyte*)(EventData->EventData.NonSessionDataReceivedEventData.DataBuffer));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an      */
                           /* exception before continuing.                       */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(SPPMObject, Method_nonSessionDataReceivedEvent,
                                    (jint)(EventData->EventData.NonSessionDataReceivedEventData.PortHandle),
                                    (jint)(EventData->EventData.NonSessionDataReceivedEventData.Lingo),
                                    (jint)(EventData->EventData.NonSessionDataReceivedEventData.CommandID),
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
                        case setNonSessionDataConfirmation:
                           switch(EventData->EventData.NonSessionDataConfirmationEventData.Status)
                           {
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_SENT:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_SENT;
                                 break;
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_ACKNOWLEDGED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_ACKNOWLEDGED;
                                 break;
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_FAILED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_FAILED;
                                 break;
                              case SPPM_DATA_CONFIRMATION_STATUS_PACKET_CANCELED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_SPPM_SPPM_DATA_CONFIRMATION_STATUS_PACKET_CANCELED;
                                 break;
                              default:
                                 IntVal1 = -1;
                                 break;
                           }

                           Env->CallVoidMethod(SPPMObject, Method_nonSessionDataConfirmationEvent,
                                 (jint)(EventData->EventData.NonSessionDataConfirmationEventData.PortHandle),
                                 (jint)(EventData->EventData.NonSessionDataConfirmationEventData.PacketID),
                                 (jint)(EventData->EventData.NonSessionDataConfirmationEventData.TransactionID),
                                 IntVal1);
                           break;
                        case setUnhandledControlMessageReceived:
                           if((ByteArray = Env->NewByteArray(EventData->EventData.UnhandledControlMessageReceivedEventData.DataLength)) != NULL)
                              Env->SetByteArrayRegion(ByteArray, 0, (EventData->EventData.UnhandledControlMessageReceivedEventData.DataLength), (jbyte*)(EventData->EventData.UnhandledControlMessageReceivedEventData.DataBuffer));
                           else
                              ByteArray = NULL;

                           /* Check whether SetByteArrayRegion triggered an      */
                           /* exception before continuing.                       */
                           if(!(Env->ExceptionCheck()))
                           {
                              Env->CallVoidMethod(SPPMObject, Method_unhandledControlMessageReceivedEvent,
                                    (jint)(EventData->EventData.UnhandledControlMessageReceivedEventData.PortHandle),
                                    (jint)(EventData->EventData.UnhandledControlMessageReceivedEventData.ControlMessageID),
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
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during SPPM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, SPPMObject, LocalData);
                  }

                  Env->DeleteLocalRef(SPPMObject);
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
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: SPPM Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_SPPM = (jclass)(NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_SPPM_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_disconnectedEvent                    = Env->GetMethodID(Clazz, "disconnectedEvent",               "(I)V");
      Method_lineStatusChangedEvent               = Env->GetMethodID(Clazz, "lineStatusChangedEvent",          "(II)V");
      Method_portStatusChangedEvent               = Env->GetMethodID(Clazz, "portStatusChangedEvent",          "(IIZI)V");
      Method_dataReceivedEvent                    = Env->GetMethodID(Clazz, "dataReceivedEvent",               "(II)V");
      Method_transmitBufferEmptyEvent             = Env->GetMethodID(Clazz, "transmitBufferEmptyEvent",        "(I)V");
      Method_idpsStatusEvent                      = Env->GetMethodID(Clazz, "idpsStatusEvent",                 "(III)V");
      Method_sessionOpenRequestEvent              = Env->GetMethodID(Clazz, "sessionOpenRequestEvent",         "(IIIII)V");
      Method_sessionCloseEvent                    = Env->GetMethodID(Clazz, "sessionCloseEvent",               "(II)V");
      Method_sessionDataReceivedEvent             = Env->GetMethodID(Clazz, "sessionDataReceivedEvent",        "(II[B)V");
      Method_sessionDataConfirmationEvent         = Env->GetMethodID(Clazz, "sessionDataConfirmationEvent",    "(IIII)V");
      Method_nonSessionDataReceivedEvent          = Env->GetMethodID(Clazz, "nonSessionDataReceivedEvent",     "(III[B)V");
      Method_nonSessionDataConfirmationEvent      = Env->GetMethodID(Clazz, "nonSessionDataConfirmationEvent", "(IIII)V");
      Method_connectionRequestEvent               = Env->GetMethodID(Clazz, "connectionRequestEvent",          "(IBBBBBB)V");
      Method_connectedEvent                       = Env->GetMethodID(Clazz, "connectedEvent",                  "(IBBBBBBI)V");
      Method_connectionStatusEvent                = Env->GetMethodID(Clazz, "connectionStatusEvent",           "(III)V");
      Method_unhandledControlMessageReceivedEvent = Env->GetMethodID(Clazz, "unhandledControlMessageReceivedEvent", "(IS[B)V");
   }
   else
      PRINT_ERROR("SPPM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.SPPM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
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
            LocalData->PortHandle = 0;
            LocalData->IsServer   = FALSE;
            SetLocalData(Env, Object, LocalData);
         }
         else
         {
            /* No need to throw an exception here. NewWeakRef will throw*/
            /* an OutOfMemoryError if we are out of resources.          */
            PRINT_ERROR("SPPM: Out of Memory: Unable to obtain weak reference to object");

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
 * Class:     com_stonestreetone_bluetopiapm_SPPM
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

      if(LocalData->PortHandle)
      {
         // TODO See about allowing a timeout here. Must be able to detect being called from a callback.
         SPPM_ClosePort(LocalData->PortHandle, 0);

         if(LocalData->IsServer)
            SPPM_UnRegisterServerPort(LocalData->PortHandle);
      }

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    disconnectNative
 * Signature: (II)I
 */
static jint DisconnectNative(JNIEnv *Env, jobject Object, jint PortHandle, jint Timeout)
{
   jint         ret_val;
   LocalData_t *LocalData;

   ret_val = SPPM_ClosePort(PortHandle, Timeout);

   if((ret_val == 0) && ((LocalData = AcquireLocalData(Env, Object, TRUE)) != NULL))
   {
      if(LocalData->IsServer == FALSE)
         LocalData->PortHandle = 0;

      ReleaseLocalData(Env, Object, LocalData);
   }

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    readDataNative
 * Signature: (II[BII)I
 */
static jint ReadDataNative(JNIEnv *Env, jobject Object, jint PortHandle, jint Timeout, jbyteArray DataBuffer, jint BufferOffset, jint ReadLength)
{
   jint   ret_val;
   jint   BufferLength;
   jbyte *RawBuffer;

   if(DataBuffer == NULL)
      ret_val = SPPM_ReadData(PortHandle, 0, 0, NULL);
   else
   {
      BufferLength = Env->GetArrayLength(DataBuffer);

      if((BufferOffset >= 0) && (ReadLength >= 0) && ((BufferOffset + ReadLength) <= BufferLength))
      {
         if((RawBuffer = Env->GetByteArrayElements(DataBuffer, NULL)) != NULL)
         {
            ret_val = SPPM_ReadData(PortHandle, Timeout, ReadLength, (((unsigned char *)RawBuffer) + BufferOffset));
            Env->ReleaseByteArrayElements(DataBuffer, RawBuffer, 0);
         }
         else
         {
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
      {
         ret_val = BTPM_ERROR_CODE_INVALID_PARAMETER;
         Env->ThrowNew(Env->FindClass("java/lang/ArrayIndexOutOfBoundsException"), NULL);
      }
   }

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    writeDataNative
 * Signature: (II[BII)I
 */
static jint WriteDataNative(JNIEnv *Env, jobject Object, jint PortHandle, jint Timeout, jbyteArray DataBuffer, jint BufferOffset, jint WriteLength)
{
   jint   ret_val;
   jsize  BufferLength;
   jbyte *RawBuffer;

   if(DataBuffer != NULL)
   {
      BufferLength = Env->GetArrayLength(DataBuffer);

      if((BufferOffset >= 0) && (WriteLength >= 0) && ((BufferOffset + WriteLength) <= BufferLength))
      {
         if((RawBuffer = Env->GetByteArrayElements(DataBuffer, NULL)) != NULL)
         {
            ret_val = SPPM_WriteData(PortHandle, Timeout, WriteLength, (((unsigned char *)RawBuffer) + BufferOffset));
            Env->ReleaseByteArrayElements(DataBuffer, RawBuffer, 0);
         }
         else
         {
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
      {
         ret_val = BTPM_ERROR_CODE_INVALID_PARAMETER;
         Env->ThrowNew(Env->FindClass("java/lang/ArrayIndexOutOfBoundsException"), NULL);
      }
   }
   else
      ret_val = 0;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    sendLineStatusNative
 * Signature: (II)I
 */
static jint SendLineStatusNative(JNIEnv *Env, jobject Object, jint PortHandle, jint LineStatusMask)
{
   unsigned long ActualLineStatusMask;

   ActualLineStatusMask  = ((LineStatusMask & com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_NO_ERROR_VALUE)     ? SPPM_LINE_STATUS_MASK_NO_ERROR_VALUE     : 0);
   ActualLineStatusMask |= ((LineStatusMask & com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_OVERRUN_ERROR_MASK) ? SPPM_LINE_STATUS_MASK_OVERRUN_ERROR_MASK : 0);
   ActualLineStatusMask |= ((LineStatusMask & com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_PARITY_ERROR_MASK)  ? SPPM_LINE_STATUS_MASK_PARITY_ERROR_MASK  : 0);
   ActualLineStatusMask |= ((LineStatusMask & com_stonestreetone_bluetopiapm_SPPM_LINE_STATUS_MASK_FRAMING_ERROR_MASK) ? SPPM_LINE_STATUS_MASK_FRAMING_ERROR_MASK : 0);

   return SPPM_SendLineStatus(PortHandle, ActualLineStatusMask);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    sendPortStatusNative
 * Signature: (IIZI)I
 */
static jint SendPortStatusNative(JNIEnv *Env, jobject Object, jint PortHandle, jint PortStatusMask, jboolean BreakSignal, jint BreakTimeout)
{
   SPPM_Port_Status_t PortStatus;

   PortStatus.PortStatusMask  = ((PortStatusMask & com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_NO_ERROR_VALUE)      ? SPPM_PORT_STATUS_MASK_NO_ERROR_VALUE      : 0);
   PortStatus.PortStatusMask |= ((PortStatusMask & com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_RTS_CTS_MASK)        ? SPPM_PORT_STATUS_MASK_RTS_CTS_MASK        : 0);
   PortStatus.PortStatusMask |= ((PortStatusMask & com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_DTR_DSR_MASK)        ? SPPM_PORT_STATUS_MASK_DTR_DSR_MASK        : 0);
   PortStatus.PortStatusMask |= ((PortStatusMask & com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_RING_INDICATOR_MASK) ? SPPM_PORT_STATUS_MASK_RING_INDICATOR_MASK : 0);
   PortStatus.PortStatusMask |= ((PortStatusMask & com_stonestreetone_bluetopiapm_SPPM_PORT_STATUS_MASK_CARRIER_DETECT_MASK) ? SPPM_PORT_STATUS_MASK_CARRIER_DETECT_MASK : 0);

   PortStatus.BreakSignal  = BreakSignal;
   PortStatus.BreakTimeout = BreakTimeout;

   return SPPM_SendPortStatus(PortHandle, &PortStatus);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    connectRemoteDeviceNative
 * Signature: (BBBBBBII[I)I
 */
static jint ConnectRemoteDeviceNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ServerPort, jint OpenFlags, jintArray ConnectionStatus)
{
   jint           ret_val;
   BD_ADDR_t      Address;
   LocalData_t   *LocalData;
   unsigned int   Status;
   unsigned long  NativeOpenFlags;

   ret_val = 0;

   ASSIGN_BD_ADDR(Address, Address1, Address2, Address3, Address4, Address5, Address6);

   NativeOpenFlags  = ((OpenFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_AUTHENTICATION : 0);
   NativeOpenFlags |= ((OpenFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_ENCRYPTION : 0);
   NativeOpenFlags |= ((OpenFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortClientManager_CONNECTION_FLAGS_MFI_REQUIRED)           ? SPPM_OPEN_REMOTE_PORT_FLAGS_MFI_REQUIRED : 0);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if(ConnectionStatus == NULL)
         ret_val = SPPM_OpenRemotePort(Address, ServerPort, NativeOpenFlags, SPPM_EventCallback, LocalData->WeakObjectReference, NULL);
      else
      {
         Status  = 0;
         ret_val = SPPM_OpenRemotePort(Address, ServerPort, NativeOpenFlags, SPPM_EventCallback, LocalData->WeakObjectReference, &Status);

         Env->SetIntArrayRegion(ConnectionStatus, 0, 1, (jint*)(&Status));
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      ret_val = -1;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    connectionRequestResponseNative
 * Signature: (IZ)I
 */
static jint ConnectionRequestResponseNative(JNIEnv *Env, jobject Object, jint PortHandle, jboolean Accept)
{
   return SPPM_OpenServerPortRequestResponse(PortHandle, ((Accept == JNI_FALSE) ? FALSE : TRUE));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    registerServerPortNative
 * Signature: (II)I
 */
static jint RegisterServerPortNative(JNIEnv *Env, jobject Object, int PortNumber, int ConnectionFlags)
{
   jint          ret_val;
   LocalData_t  *LocalData;
   unsigned long NativePortFlags;

   NativePortFlags  = ((ConnectionFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortServerManager_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_AUTHORIZATION  : 0);
   NativePortFlags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortServerManager_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_AUTHENTICATION : 0);
   NativePortFlags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortServerManager_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_ENCRYPTION     : 0);
   NativePortFlags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortServerManager_INCOMING_CONNECTION_FLAGS_MFI_ALLOWED)            ? SPPM_REGISTER_SERVER_PORT_FLAGS_MFI_ALLOWED            : 0);
   NativePortFlags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_SPPM_SerialPortServerManager_INCOMING_CONNECTION_FLAGS_MFI_REQUIRED)           ? SPPM_REGISTER_SERVER_PORT_FLAGS_MFI_REQUIRED           : 0);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      /* Check if the caller has specified a port. A value of 0 or less */
      /* indicates that the caller is requesting that we determine an   */
      /* available port to use.                                         */
      if(PortNumber > 0)
      {
         /* A port number was supplied, so just attempt to open register*/
         /* a server to it.                                             */
         ret_val = SPPM_RegisterServerPort(PortNumber, NativePortFlags, SPPM_EventCallback, LocalData->WeakObjectReference);
      }
      else
      {
         /* The caller has requested that we determine the port to use. */
         /* Locate a available port.                                    */
         PortNumber = SPPM_FindFreeServerPort();
         ret_val    = 0;

         while((PortNumber > 0) && (ret_val == 0))
         {
            /* A free port was identified, so attempt to claim it.      */
            ret_val = SPPM_RegisterServerPort(PortNumber, NativePortFlags, SPPM_EventCallback, LocalData->WeakObjectReference);

            if(ret_val == BTPM_ERROR_CODE_SERIAL_PORT_IS_NOT_AVAILABLE)
            {
               /* The port was claimed by another client in the time    */
               /* between the search and the registration request.      */
               /* Attempt to find another available port.               */
               PortNumber = SPPM_FindFreeServerPort();
               ret_val    = 0;
            }
         }
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      ret_val = -1;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    registerSerialPortSDPRecordNative
 * Signature: (I[J[J[J[JLjava/lang/String;)I
 */
static jint RegisterSerialPortSDPRecordNative(JNIEnv *Env, jobject Object, jint PortHandle, jlongArray ServiceClassIDsHigh, jlongArray ServiceClassIDsLow, jlongArray ProtocolsHigh, jlongArray ProtocolsLow, jstring ServiceName)
{
   jint                               ret_val;
   jlong                             *UUIDHighArray;
   jlong                             *UUIDLowArray;
   jsize                              StringLength;
   char                              *StringBuffer;
   char                               DefaultServiceName[16];
   const char                        *String;
   unsigned int                       Index;
   unsigned int                       Index2;
   SDP_Data_Element_t                 ProtocolSequence;
   SDP_Data_Element_t                *ProtocolSequenceElements;
   SPPM_Service_Record_Information_t  RecordInfo;

   ret_val                  = 0;
   StringBuffer             = NULL;
   ProtocolSequenceElements = NULL;

   BTPS_StringCopy(DefaultServiceName, "Serial Port");

   memset(&RecordInfo, 0, sizeof(RecordInfo));

   if((ServiceClassIDsHigh) && (ServiceClassIDsLow))
   {
      RecordInfo.NumberServiceClassUUID = Env->GetArrayLength(ServiceClassIDsHigh);

      if((UUIDHighArray = (jlong*)(Env->GetPrimitiveArrayCritical(ServiceClassIDsHigh, NULL))) != NULL)
      {
         if((UUIDLowArray = (jlong*)(Env->GetPrimitiveArrayCritical(ServiceClassIDsLow, NULL))) != NULL)
         {
            if((RecordInfo.SDPUUIDEntries = (SDP_UUID_Entry_t *)malloc(RecordInfo.NumberServiceClassUUID * sizeof(SDP_UUID_Entry_t))) != NULL)
            {
               for(Index = 0; Index < RecordInfo.NumberServiceClassUUID; Index++)
               {
                  RecordInfo.SDPUUIDEntries[Index].SDP_Data_Element_Type = deUUID_128;

                  ASSIGN_SDP_UUID_128(RecordInfo.SDPUUIDEntries[Index].UUID_Value.UUID_128, (Byte_t)((UUIDHighArray[Index] >> 56) & 0x0FF),
                                                                                            (Byte_t)((UUIDHighArray[Index] >> 48) & 0x0FF),
                                                                                            (Byte_t)((UUIDHighArray[Index] >> 40) & 0x0FF),
                                                                                            (Byte_t)((UUIDHighArray[Index] >> 32) & 0x0FF),
                                                                                            (Byte_t)((UUIDHighArray[Index] >> 24) & 0x0FF),
                                                                                            (Byte_t)((UUIDHighArray[Index] >> 16) & 0x0FF),
                                                                                            (Byte_t)((UUIDHighArray[Index] >> 8) & 0x0FF),
                                                                                            (Byte_t)((UUIDHighArray[Index]) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index] >> 56) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index] >> 48) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index] >> 40) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index] >> 32) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index] >> 24) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index] >> 16) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index] >> 8) & 0x0FF),
                                                                                            (Byte_t)((UUIDLowArray[Index]) & 0x0FF));
               }
            }
            else
               ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

            Env->ReleasePrimitiveArrayCritical(ServiceClassIDsLow, UUIDLowArray, JNI_ABORT);
         }
         else
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

         Env->ReleasePrimitiveArrayCritical(ServiceClassIDsHigh, UUIDHighArray, JNI_ABORT);
      }
      else
         ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }


   if((!ret_val) && (ProtocolsHigh) && (ProtocolsLow))
   {
      ProtocolSequence.SDP_Data_Element_Type   = deSequence;
      ProtocolSequence.SDP_Data_Element_Length = Env->GetArrayLength(ProtocolsHigh);

      if((UUIDHighArray = (jlong*)(Env->GetPrimitiveArrayCritical(ProtocolsHigh, NULL))) != NULL)
      {
         if((UUIDLowArray = (jlong*)(Env->GetPrimitiveArrayCritical(ProtocolsLow, NULL))) != NULL)
         {
            /* Allocate space for twice the number of elements,      */
            /* because each element is represented as a Sequence of  */
            /* length 1, with the UUID in the sequence's first       */
            /* element.                                              */
            if((ProtocolSequenceElements = (SDP_Data_Element_t*)malloc(2 * ProtocolSequence.SDP_Data_Element_Length * sizeof(SDP_Data_Element_t))) != NULL)
            {
               /* For N UUID values, store the Sequence elements in  */
               /* positions 0..(N-1), so this array can be used in   */
               /* the Protocol Descriptor sequence element, and store*/
               /* the actual UUIDs in elements N..(2N-1), where      */
               /* Protocol UUID N is represented by the Sequence at  */
               /* index (N-1) and the UUID_128 at index (2N-1).      */
               for(Index = 0; Index < ProtocolSequence.SDP_Data_Element_Length; Index++)
               {
                  /* Determine the index at which the UUID will be   */
                  /* stored.                                         */
                  Index2 = (ProtocolSequence.SDP_Data_Element_Length + (2 * Index));

                  ProtocolSequenceElements[Index].SDP_Data_Element_Type                      = deSequence;
                  ProtocolSequenceElements[Index].SDP_Data_Element_Length                    = 1;
                  ProtocolSequenceElements[Index].SDP_Data_Element.SDP_Data_Element_Sequence = &ProtocolSequenceElements[Index2];

                  ProtocolSequenceElements[Index2].SDP_Data_Element_Type   = deUUID_128;
                  ProtocolSequenceElements[Index2].SDP_Data_Element_Length = sizeof(UUID_128_t);

                  ASSIGN_SDP_UUID_128(ProtocolSequenceElements[Index2].SDP_Data_Element.UUID_128, (Byte_t)((UUIDHighArray[Index] >> 56) & 0x0FF),
                                                                                                  (Byte_t)((UUIDHighArray[Index] >> 48) & 0x0FF),
                                                                                                  (Byte_t)((UUIDHighArray[Index] >> 40) & 0x0FF),
                                                                                                  (Byte_t)((UUIDHighArray[Index] >> 32) & 0x0FF),
                                                                                                  (Byte_t)((UUIDHighArray[Index] >> 24) & 0x0FF),
                                                                                                  (Byte_t)((UUIDHighArray[Index] >> 16) & 0x0FF),
                                                                                                  (Byte_t)((UUIDHighArray[Index] >> 8) & 0x0FF),
                                                                                                  (Byte_t)((UUIDHighArray[Index]) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index] >> 56) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index] >> 48) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index] >> 40) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index] >> 32) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index] >> 24) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index] >> 16) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index] >> 8) & 0x0FF),
                                                                                                  (Byte_t)((UUIDLowArray[Index]) & 0x0FF));
               }

               ProtocolSequence.SDP_Data_Element.SDP_Data_Element_Sequence = ProtocolSequenceElements;
               RecordInfo.ProtocolList                                     = &ProtocolSequence;
            }
            else
               ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

            Env->ReleasePrimitiveArrayCritical(ProtocolsLow, UUIDLowArray, JNI_ABORT);
         }
         else
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

         Env->ReleasePrimitiveArrayCritical(ProtocolsHigh, UUIDHighArray, JNI_ABORT);
      }
      else
         ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if(!ret_val)
   {
      if(ServiceName)
      {
         if((String = Env->GetStringUTFChars(ServiceName, NULL)) != NULL)
         {
            StringLength = Env->GetStringUTFLength(ServiceName);

            if((StringBuffer = (char*)malloc(StringLength+1)) != NULL)
            {
               strncpy(StringBuffer, String, StringLength);
               StringBuffer[StringLength] = '\0';
            }
            else
               ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

            Env->ReleaseStringUTFChars(ServiceName, String);
         }
         else
            ret_val = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         /* A service name is required, so provide a default when the   */
         /* user has not given us one.                                  */
         StringBuffer = DefaultServiceName;
         StringLength = strlen(StringBuffer);
      }

      if(StringBuffer)
         RecordInfo.ServiceName = StringBuffer;
   }

   /* If no errors have occurred, register the service record.          */
   if(!ret_val)
      ret_val = SPPM_RegisterServerPortServiceRecord(PortHandle, &RecordInfo);

   /* Clean up allocated resources.                                     */
   if((ServiceName) && (StringBuffer))
      free(StringBuffer);

   if(RecordInfo.SDPUUIDEntries)
      free(RecordInfo.SDPUUIDEntries);

   if(ProtocolSequenceElements)
      free(ProtocolSequenceElements);

   if((ret_val == BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY) && (Env->ExceptionCheck() == JNI_FALSE))
      Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);

   return ret_val;
}


/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    unRegisterServerPortNative
 * Signature: (I)I
 */
static jint UnregisterServerPortNative(JNIEnv *Env, jobject Object, jint PortHandle)
{
   jint         ret_val;
   LocalData_t *LocalData;

   ret_val = SPPM_UnRegisterServerPort(PortHandle);

   if((ret_val == 0) && ((LocalData = AcquireLocalData(Env, Object, TRUE)) != NULL))
   {
      if(LocalData->IsServer == TRUE)
      {
         LocalData->PortHandle = 0;
         LocalData->IsServer   = FALSE;
      }

      ReleaseLocalData(Env, Object, LocalData);
   }

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    queryRemoteDeviceServicesNative
 * Signature: (BBBBBB[[I[[J[[I[[Ljava/lang/String;)I
 */
static jint QueryRemoteDeviceServicesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jobjectArray Handles, jobjectArray ServiceTypes, jobjectArray Ports, jobjectArray Names)
{
   int                                 Result;
   Byte_t                             *SDPDataBuffer;
   BD_ADDR_t                           RemoteDeviceAddress;
   UUID_128_t                          RfcommUUID;
   UUID_128_t                          ServiceUUID;
   jintArray                           HandleArray;
   jlongArray                          ServiceTypeMSBArray;
   jlongArray                          ServiceTypeLSBArray;
   jintArray                           PortArray;
   jobjectArray                        NameArray;
   jint                               *HandleArrayElements;
   jlong                              *ServiceTypeMSBArrayElements;
   jlong                              *ServiceTypeLSBArrayElements;
   jint                               *PortArrayElements;
   jstring                             ServiceName;
   char                               *ServiceNameNative;
   unsigned int                        Index;
   unsigned int                        RfcommRecordCount;
   unsigned int                        SDPResponseIndex;
   unsigned int                        TotalServiceDataLength;
   DEVM_Parsed_SDP_Data_t              ParsedSDPData;
   SDP_Data_Element_t                 *RfcommDescriptor;
   SDP_Data_Element_t                 *PrimaryServiceClassID;
   SDP_Service_Attribute_Value_Data_t *Attribute;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);
   SDP_ASSIGN_RFCOMM_UUID_128(RfcommUUID);

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
                  RfcommRecordCount = 0;

                  /* Search the record list to determine how many       */
                  /* RFCOMM-compatible profiles are present.            */
                  for(SDPResponseIndex = 0; SDPResponseIndex < ParsedSDPData.NumberServiceRecords; SDPResponseIndex++)
                  {
                     /* Find the Protocol Descriptor List attribute.    */
                     if((Attribute = FindSDPAttribute(&(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex]), SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST)) != NULL)
                     {
                        /* Find the RFCOMM protocol descriptor.         */
                        if((RfcommDescriptor = FindEntryInProtocolDescriptorList(Attribute, RfcommUUID)) != NULL)
                        {
                           if((RfcommDescriptor->SDP_Data_Element_Length >= 2) && (RfcommDescriptor->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element_Type == deUnsignedInteger1Byte))
                           {
                              if(SPP_VALID_PORT_NUMBER(RfcommDescriptor->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element.UnsignedInteger1Byte))
                                 RfcommRecordCount++;
                           }
                        }
                     }
                  }
                  
                  PRINT_DEBUG("%s: Found %u RFCOMM-based records", __FUNCTION__, RfcommRecordCount);

                  HandleArray = Env->NewIntArray(RfcommRecordCount);
                  ServiceTypeMSBArray = Env->NewLongArray(RfcommRecordCount);
                  ServiceTypeLSBArray = Env->NewLongArray(RfcommRecordCount);
                  PortArray = Env->NewIntArray(RfcommRecordCount);
                  NameArray = Env->NewObjectArray(RfcommRecordCount, Env->FindClass("java/lang/String"), NULL);

                  if((HandleArray) && (ServiceTypeMSBArray) && (ServiceTypeLSBArray) && (PortArray) && (NameArray))
                  {
                     //HandleArrayElements         = (jint*)(Env->GetPrimitiveArrayCritical(HandleArray, NULL));
                     //ServiceTypeMSBArrayElements = (jlong*)(Env->GetPrimitiveArrayCritical(ServiceTypeMSBArray, NULL));
                     //ServiceTypeLSBArrayElements = (jlong*)(Env->GetPrimitiveArrayCritical(ServiceTypeLSBArray, NULL));
                     //PortArrayElements           = (jint*)(Env->GetPrimitiveArrayCritical(PortArray, NULL));
                     HandleArrayElements         = Env->GetIntArrayElements(HandleArray, NULL);
                     ServiceTypeMSBArrayElements = Env->GetLongArrayElements(ServiceTypeMSBArray, NULL);
                     ServiceTypeLSBArrayElements = Env->GetLongArrayElements(ServiceTypeLSBArray, NULL);
                     PortArrayElements           = Env->GetIntArrayElements(PortArray, NULL);

                     memset(HandleArrayElements,         0, (RfcommRecordCount * sizeof(jint)));
                     memset(ServiceTypeMSBArrayElements, 0, (RfcommRecordCount * sizeof(jlong)));
                     memset(ServiceTypeLSBArrayElements, 0, (RfcommRecordCount * sizeof(jlong)));
                     memset(PortArrayElements,           0, (RfcommRecordCount * sizeof(jint)));

                     if((HandleArrayElements) && (ServiceTypeMSBArrayElements) && (ServiceTypeLSBArrayElements) && (PortArrayElements))
                     {
                        /* Now collect the relevant data from each      */
                        /* record.                                      */
                        for(SDPResponseIndex = 0, Index = 0; ((SDPResponseIndex < ParsedSDPData.NumberServiceRecords) && (Index < RfcommRecordCount)); SDPResponseIndex++)
                        {
                           /* Pull the appropriate entries from this    */
                           /* records. Start with the RFCOMM service    */
                           /* channel (port number).                    */

                           /* Find the Protocol Descriptor List         */
                           /* attribute.                                */
                           if((Attribute = FindSDPAttribute(&(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex]), SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST)) != NULL)
                           {
                              /* Find the RFCOMM protocol descriptor.   */
                              if((RfcommDescriptor = FindEntryInProtocolDescriptorList(Attribute, RfcommUUID)) != NULL)
                              {
                                 /* Get the RFCOMM channel from the     */
                                 /* descriptor. It should be present in */
                                 /* the second element of the descriptor*/
                                 /* (where the first element is a UUID  */
                                 /* identifying the record as an RFCOMM */
                                 /* descriptor.                         */
                                 if((RfcommDescriptor->SDP_Data_Element_Length >= 2) && (RfcommDescriptor->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element_Type == deUnsignedInteger1Byte))
                                 {
                                    if(SPP_VALID_PORT_NUMBER(RfcommDescriptor->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element.UnsignedInteger1Byte))
                                    {
                                       PortArrayElements[Index] = RfcommDescriptor->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element.UnsignedInteger1Byte;
                                    }
                                 }
                              }
                           }

                           /* Only continue parsing this record if an   */
                           /* RFCOMM channel was found.                 */
                           if(PortArrayElements[Index] > 0)
                           {
                              /* Use the parsed SDP index for the SDP   */
                              /* Handle.                                */
                              HandleArrayElements[Index] = (jint)(SDPResponseIndex);

                              /* Now determine the Service Class ID.       */
                              if((Attribute = FindSDPAttribute(&(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex]), SDP_ATTRIBUTE_ID_SERVICE_CLASS_ID_LIST)) != NULL)
                              {
                                 if((Attribute->SDP_Data_Element->SDP_Data_Element_Type == deSequence) && (Attribute->SDP_Data_Element->SDP_Data_Element_Length > 0))
                                 {
                                    PrimaryServiceClassID = &(Attribute->SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0]);

                                    if((PrimaryServiceClassID->SDP_Data_Element_Type == deUUID_128) || (PrimaryServiceClassID->SDP_Data_Element_Type == deUUID_32) || (PrimaryServiceClassID->SDP_Data_Element_Type == deUUID_16))
                                    {
                                       switch(PrimaryServiceClassID->SDP_Data_Element_Type)
                                       {
                                          case deUUID_128:
                                             ServiceUUID = PrimaryServiceClassID->SDP_Data_Element.UUID_128;
                                             break;
                                          case deUUID_32:
                                             SDP_ASSIGN_BASE_UUID(ServiceUUID);
                                             ASSIGN_SDP_UUID_32_TO_SDP_UUID_128(ServiceUUID, PrimaryServiceClassID->SDP_Data_Element.UUID_32);
                                             break;
                                          case deUUID_16:
                                             SDP_ASSIGN_BASE_UUID(ServiceUUID);
                                             ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(ServiceUUID, PrimaryServiceClassID->SDP_Data_Element.UUID_16);
                                             break;
                                          default:
                                             ASSIGN_SDP_UUID_128(ServiceUUID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
                                             break;
                                       }

                                       ServiceTypeMSBArrayElements[Index] = ((((jlong)(ServiceUUID.UUID_Byte0)  & 0x0FFL) << 56) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte1)  & 0x0FFL) << 48) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte2)  & 0x0FFL) << 40) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte3)  & 0x0FFL) << 32) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte4)  & 0x0FFL) << 24) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte5)  & 0x0FFL) << 16) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte6)  & 0x0FFL) << 8)  |
                                                                             (((jlong)(ServiceUUID.UUID_Byte7)  & 0x0FFL)));
                                       ServiceTypeLSBArrayElements[Index] = ((((jlong)(ServiceUUID.UUID_Byte8)  & 0x0FFL) << 56) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte9)  & 0x0FFL) << 48) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte10) & 0x0FFL) << 40) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte11) & 0x0FFL) << 32) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte12) & 0x0FFL) << 24) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte13) & 0x0FFL) << 16) |
                                                                             (((jlong)(ServiceUUID.UUID_Byte14) & 0x0FFL) << 8)  |
                                                                             (((jlong)(ServiceUUID.UUID_Byte15) & 0x0FFL)));
                                    }
                                 }
                              }

                              /* Now determine the Service Name.           */
                              if((Attribute = FindSDPAttribute(&(ParsedSDPData.SDPServiceAttributeResponseData[SDPResponseIndex]), (SDP_DEFAULT_LANGUAGE_BASE_ATTRIBUTE_ID + SDP_ATTRIBUTE_OFFSET_ID_SERVICE_NAME))) != NULL)
                              {
                                 if(Attribute->SDP_Data_Element->SDP_Data_Element_Type == deTextString)
                                 {
                                    if(Attribute->SDP_Data_Element->SDP_Data_Element_Length > 0)
                                    {
                                       if((ServiceNameNative = (char *)malloc(Attribute->SDP_Data_Element->SDP_Data_Element_Length + 1)) != NULL)
                                       {
                                          memcpy(ServiceNameNative, Attribute->SDP_Data_Element->SDP_Data_Element.TextString, Attribute->SDP_Data_Element->SDP_Data_Element_Length);
                                          ServiceNameNative[Attribute->SDP_Data_Element->SDP_Data_Element_Length] = '\0';

                                          if((ServiceName = Env->NewStringUTF(ServiceNameNative)) != NULL)
                                          {
                                             Env->SetObjectArrayElement(NameArray, Index, ServiceName);
                                             Env->DeleteLocalRef(ServiceName);
                                          }

                                          free(ServiceNameNative);
                                       }
                                    }
                                 }
                              }

                              /* An RFCOMM-based record has been        */
                              /* recorded.                              */
                              Index++;
                           }
                        }

                        /* Finished scanning the records. Now, assign   */
                        /* the data lists to the respective java arrays.*/
                        Env->SetObjectArrayElement(Handles, 0, HandleArray);
                        Env->SetObjectArrayElement(ServiceTypes, 0, ServiceTypeMSBArray);
                        Env->SetObjectArrayElement(ServiceTypes, 1, ServiceTypeLSBArray);
                        Env->SetObjectArrayElement(Ports, 0, PortArray);
                        Env->SetObjectArrayElement(Names, 0, NameArray);
                     }
                     else
                        Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

                     if(HandleArrayElements)
                        //Env->ReleasePrimitiveArrayCritical(HandleArray, HandleArrayElements, 0);
                        Env->ReleaseIntArrayElements(HandleArray, HandleArrayElements, 0);

                     if(ServiceTypeMSBArrayElements)
                        //Env->ReleasePrimitiveArrayCritical(ServiceTypeMSBArray, ServiceTypeMSBArrayElements, 0);
                        Env->ReleaseLongArrayElements(ServiceTypeMSBArray, ServiceTypeMSBArrayElements, 0);

                     if(ServiceTypeLSBArrayElements)
                        //Env->ReleasePrimitiveArrayCritical(ServiceTypeLSBArray, ServiceTypeLSBArrayElements, 0);
                        Env->ReleaseLongArrayElements(ServiceTypeLSBArray, ServiceTypeLSBArrayElements, 0);

                     if(PortArrayElements)
                        //Env->ReleasePrimitiveArrayCritical(PortArray, PortArrayElements, 0);
                        Env->ReleaseIntArrayElements(PortArray, PortArrayElements, 0);
                  }
                  else
                     Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

                  if(HandleArray)
                     Env->DeleteLocalRef(HandleArray);

                  if(ServiceTypeMSBArray)
                     Env->DeleteLocalRef(ServiceTypeMSBArray);

                  if(ServiceTypeLSBArray)
                     Env->DeleteLocalRef(ServiceTypeLSBArray);

                  if(PortArray)
                     Env->DeleteLocalRef(PortArray);

                  if(NameArray)
                     Env->DeleteLocalRef(NameArray);

                  DEVM_FreeParsedSDPData(&ParsedSDPData);
               }
            }
         }

         free(SDPDataBuffer);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if((Result == BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY) && (!Env->ExceptionCheck()))
      Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    configureMFiSettingsNative
 * Signature: (II[BJLjava/lang/String;IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/String;[ILjava/lang/String;[I[I[[BLjava/lang/String;[Ljava/lang/String;[S[S)I
 */
static jint ConfigureMFiSettingsNative(JNIEnv *Env, jobject Object, jint MaximumReceivePacketSize, jint DataPacketTimeout, jbyteArray SupportedLingos, jlong AccessoryCapabilitiesBitmask, jstring AccessoryName, jint AccessoryFirmwareVersion, jint AccessoryHardwareVersion, jstring AccessoryManufacturer, jstring AccessoryModelNumber, jstring AccessorySerialNumber, jint AccessoryRFCertification, jobjectArray SupportedProtocols, jintArray MatchActions, jstring BundleSeedID, jintArray FidTypes, jintArray FidSubtypes, jobjectArray FidDataBuffers, jstring CurrentLanguage, jobjectArray SupportedLanguages, jshortArray ControlMessageIDsSent, jshortArray ControlMessageIDsReceived)
{
   //XXX
   jint                              Result;
   jint                             *FidTypesArray;
   jint                             *FidSubtypesArray;
   jint                             *MatchActionArray;
   jshort                           *MessageIDsArray;
   Byte_t                            SupportedLingoList[SPPM_MFI_MAXIMUM_SUPPORTED_LINGOS];
   jstring                           StringArrayElement;
   jbyteArray                        FidTokenDataBuffer;
   const char                       *String;
   unsigned int                      Index;
   unsigned int                      NumberMatchActions;
   SPPM_MFi_Configuration_Settings_t ConfigSettings;

   Result = 0;
   memset(&ConfigSettings, 0, sizeof(ConfigSettings));

   ConfigSettings.MaximumReceivePacketSize = (unsigned int)MaximumReceivePacketSize;
   if(ConfigSettings.MaximumReceivePacketSize < SPPM_MFI_RECEIVE_PACKET_SIZE_MINIMUM)
      ConfigSettings.MaximumReceivePacketSize = SPPM_MFI_RECEIVE_PACKET_SIZE_MINIMUM;
   if(ConfigSettings.MaximumReceivePacketSize > SPPM_MFI_RECEIVE_PACKET_SIZE_MAXIMUM)
      ConfigSettings.MaximumReceivePacketSize = SPPM_MFI_RECEIVE_PACKET_SIZE_MAXIMUM;

   ConfigSettings.DataPacketTimeout = (unsigned int)DataPacketTimeout;
   if(ConfigSettings.DataPacketTimeout < SPPM_MFI_PACKET_TIMEOUT_MINIMUM_MS)
      ConfigSettings.DataPacketTimeout = SPPM_MFI_PACKET_TIMEOUT_MINIMUM_MS;
   if(ConfigSettings.DataPacketTimeout > SPPM_MFI_PACKET_TIMEOUT_MAXIMUM_MS)
      ConfigSettings.DataPacketTimeout = SPPM_MFI_PACKET_TIMEOUT_MAXIMUM_MS;

   if(SupportedLingos)
   {
      ConfigSettings.NumberSupportedLingos = ((Env->GetArrayLength(SupportedLingos) < SPPM_MFI_MAXIMUM_SUPPORTED_LINGOS) ? (Byte_t)(Env->GetArrayLength(SupportedLingos)) : SPPM_MFI_MAXIMUM_SUPPORTED_LINGOS);
      ConfigSettings.SupportedLingoList    = SupportedLingoList;

      Env->GetByteArrayRegion(SupportedLingos, 0, ConfigSettings.NumberSupportedLingos, (jbyte*)(&SupportedLingoList));
   }

   ConfigSettings.AccessoryInfo.AccessoryCapabilitiesBitmask = (QWord_t)AccessoryCapabilitiesBitmask;

   if((!Result) && (AccessoryName))
   {
      if((String = Env->GetStringUTFChars(AccessoryName, NULL)) != NULL)
      {
         strncpy((char*)(ConfigSettings.AccessoryInfo.AccessoryName), String, sizeof(ConfigSettings.AccessoryInfo.AccessoryName));
         ConfigSettings.AccessoryInfo.AccessoryName[sizeof(ConfigSettings.AccessoryInfo.AccessoryName)-1] = '\0';
         Env->ReleaseStringUTFChars(AccessoryName, String);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   ConfigSettings.AccessoryInfo.AccessoryFirmwareVersion[0] = (Byte_t)(AccessoryFirmwareVersion >> 16);
   ConfigSettings.AccessoryInfo.AccessoryFirmwareVersion[1] = (Byte_t)(AccessoryFirmwareVersion >> 8);
   ConfigSettings.AccessoryInfo.AccessoryFirmwareVersion[2] = (Byte_t)(AccessoryFirmwareVersion);
   ConfigSettings.AccessoryInfo.AccessoryInformationBitMask |= SPPM_MFI_ACCESSORY_INFORMATION_BITMASK_FIRMWARE_VERSION;

   ConfigSettings.AccessoryInfo.AccessoryHardwareVersion[0] = (Byte_t)(AccessoryHardwareVersion >> 16);
   ConfigSettings.AccessoryInfo.AccessoryHardwareVersion[1] = (Byte_t)(AccessoryHardwareVersion >> 8);
   ConfigSettings.AccessoryInfo.AccessoryHardwareVersion[2] = (Byte_t)(AccessoryHardwareVersion);
   ConfigSettings.AccessoryInfo.AccessoryInformationBitMask |= SPPM_MFI_ACCESSORY_INFORMATION_BITMASK_HARDWARE_VERSION;

   if((!Result) && (AccessoryManufacturer))
   {
      if((String = Env->GetStringUTFChars(AccessoryManufacturer, NULL)) != NULL)
      {
         strncpy((char*)(ConfigSettings.AccessoryInfo.AccessoryManufacturer), String, SPPM_MFI_MAXIMUM_SUPPORTED_MANUFACTURER_NAME_LENGTH);
         ConfigSettings.AccessoryInfo.AccessoryManufacturer[SPPM_MFI_MAXIMUM_SUPPORTED_MANUFACTURER_NAME_LENGTH-1] = '\0';
         Env->ReleaseStringUTFChars(AccessoryManufacturer, String);

         ConfigSettings.AccessoryInfo.AccessoryInformationBitMask |= SPPM_MFI_ACCESSORY_INFORMATION_BITMASK_MANUFACTURER_NAME;
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if((!Result) && (AccessoryModelNumber))
   {
      if((String = Env->GetStringUTFChars(AccessoryModelNumber, NULL)) != NULL)
      {
         strncpy((char*)(ConfigSettings.AccessoryInfo.AccessoryModelNumber), String, SPPM_MFI_MAXIMUM_SUPPORTED_MODEL_NUMBER_LENGTH);
         ConfigSettings.AccessoryInfo.AccessoryModelNumber[SPPM_MFI_MAXIMUM_SUPPORTED_MODEL_NUMBER_LENGTH-1] = '\0';
         Env->ReleaseStringUTFChars(AccessoryModelNumber, String);

         ConfigSettings.AccessoryInfo.AccessoryInformationBitMask |= SPPM_MFI_ACCESSORY_INFORMATION_BITMASK_MODEL_NUMBER;
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if((!Result) && (AccessorySerialNumber))
   {
      if((String = Env->GetStringUTFChars(AccessorySerialNumber, NULL)) != NULL)
      {
         strncpy((char*)(ConfigSettings.AccessoryInfo.AccessorySerialNumber), String, SPPM_MFI_MAXIMUM_SUPPORTED_SERIAL_NUMBER_LENGTH);
         ConfigSettings.AccessoryInfo.AccessorySerialNumber[SPPM_MFI_MAXIMUM_SUPPORTED_SERIAL_NUMBER_LENGTH-1] = '\0';
         Env->ReleaseStringUTFChars(AccessorySerialNumber, String);

         ConfigSettings.AccessoryInfo.AccessoryInformationBitMask |= SPPM_MFI_ACCESSORY_INFORMATION_BITMASK_SERIAL_NUMBER;
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   ConfigSettings.AccessoryInfo.AccessoryRFCertification = (DWord_t)AccessoryRFCertification;

   if((!Result) && (SupportedProtocols))
   {
      ConfigSettings.NumberSupportedProtocols = (Byte_t)(Env->GetArrayLength(SupportedProtocols));

      if(MatchActions)
      {
         NumberMatchActions = Env->GetArrayLength(MatchActions);
         MatchActionArray   = Env->GetIntArrayElements(MatchActions, NULL);

         if(!MatchActionArray)
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         NumberMatchActions = 0;
         MatchActionArray   = NULL;
      }

      if((!Result) && ((ConfigSettings.SupportedProtocolList = (SPPM_MFi_Protocol_String_t *)malloc(ConfigSettings.NumberSupportedProtocols * sizeof(SPPM_MFi_Protocol_String_t))) != NULL))
      {
         memset(ConfigSettings.SupportedProtocolList, 0, (ConfigSettings.NumberSupportedProtocols * sizeof(SPPM_MFi_Protocol_String_t)));

         for(Index = 0; Index < ConfigSettings.NumberSupportedProtocols; Index++)
         {
            StringArrayElement = (jstring)(Env->GetObjectArrayElement(SupportedProtocols, Index));

            if((String = Env->GetStringUTFChars(StringArrayElement, NULL)) != NULL)
            {
               strncpy((char*)(ConfigSettings.SupportedProtocolList[Index].ProtocolString), String, SPPM_MFI_MAXIMUM_SUPPORTED_PROTOCOL_STRING_LENGTH);
               ConfigSettings.SupportedProtocolList[Index].ProtocolString[SPPM_MFI_MAXIMUM_SUPPORTED_PROTOCOL_STRING_LENGTH-1] = '\0';
               Env->ReleaseStringUTFChars(StringArrayElement, String);

               if((MatchActionArray) && (Index < NumberMatchActions))
               {
                  switch(MatchActionArray[Index])
                  {
                     case com_stonestreetone_bluetopiapm_SPPM_MATCH_ACTION_AUTOMATIC_SEARCH:
                        ConfigSettings.SupportedProtocolList[Index].MatchAction = maAutomaticSearch;
                        break;
                     case com_stonestreetone_bluetopiapm_SPPM_MATCH_ACTION_SEARCH_BUTTON_ONLY:
                        ConfigSettings.SupportedProtocolList[Index].MatchAction = maSearchButtonOnly;
                        break;
                     case com_stonestreetone_bluetopiapm_SPPM_MATCH_ACTION_NONE:
                     default:
                        ConfigSettings.SupportedProtocolList[Index].MatchAction = maNone;
                        break;
                  }
               }
               else
                  ConfigSettings.SupportedProtocolList[Index].MatchAction = maNone;
            }
            else
            {
               Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               Index = (ConfigSettings.NumberSupportedProtocols - 1);
            }

            Env->DeleteLocalRef(StringArrayElement);
         }
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

      if(MatchActionArray)
         Env->ReleaseIntArrayElements(MatchActions, MatchActionArray, JNI_ABORT);
   }

   if((!Result) && (BundleSeedID))
   {
      if((String = Env->GetStringUTFChars(BundleSeedID, NULL)) != NULL)
      {
         strncpy((char*)(ConfigSettings.BundleSeedIDString), String, SPPM_MFI_MAXIMUM_SUPPORTED_BUNDLE_SEED_ID_LENGTH);
         ConfigSettings.BundleSeedIDString[SPPM_MFI_MAXIMUM_SUPPORTED_BUNDLE_SEED_ID_LENGTH-1] = '\0';
         Env->ReleaseStringUTFChars(BundleSeedID, String);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if((!Result) && (FidTypes) && (FidSubtypes) && (FidDataBuffers))
   {
      ConfigSettings.NumberFIDTokens = (Byte_t)(Env->GetArrayLength(FidTypes));

      if((ConfigSettings.NumberFIDTokens == (Byte_t)(Env->GetArrayLength(FidSubtypes))) && (ConfigSettings.NumberFIDTokens == (Byte_t)(Env->GetArrayLength(FidDataBuffers))))
      {
         if((ConfigSettings.FIDTokenList = (SPPM_MFi_FID_Token_Value_t *)malloc(ConfigSettings.NumberFIDTokens * SPPM_MFI_FID_TOKEN_VALUE_SIZE)) != NULL)
         {
            memset(ConfigSettings.FIDTokenList, 0, (ConfigSettings.NumberFIDTokens * SPPM_MFI_FID_TOKEN_VALUE_SIZE));

            if((FidTypesArray = Env->GetIntArrayElements(FidTypes, NULL)) != NULL)
            {
               if((FidSubtypesArray = Env->GetIntArrayElements(FidSubtypes, NULL)) != NULL)
               {
                  for(Index = 0; Index < ConfigSettings.NumberFIDTokens; Index++)
                  {
                     FidTokenDataBuffer = (jbyteArray)(Env->GetObjectArrayElement(FidDataBuffers, Index));

                     ConfigSettings.FIDTokenList[Index].FIDType    = FidTypesArray[Index];
                     ConfigSettings.FIDTokenList[Index].FIDSubType = FidSubtypesArray[Index];

                     ConfigSettings.FIDTokenList[Index].FIDDataLength = Env->GetArrayLength(FidTokenDataBuffer);

                     if((ConfigSettings.FIDTokenList[Index].FIDData = (Byte_t*)malloc(ConfigSettings.FIDTokenList[Index].FIDDataLength)) != NULL)
                        Env->GetByteArrayRegion(FidTokenDataBuffer, 0, ConfigSettings.FIDTokenList[Index].FIDDataLength, (jbyte*)(ConfigSettings.FIDTokenList[Index].FIDData));
                     else
                     {
                        Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                        Index = (ConfigSettings.NumberFIDTokens - 1);
                     }

                     Env->DeleteLocalRef(FidTokenDataBuffer);
                  }

                  Env->ReleaseIntArrayElements(FidSubtypes, FidSubtypesArray, JNI_ABORT);
               }
               else
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

               Env->ReleaseIntArrayElements(FidTypes, FidTypesArray, JNI_ABORT);
            }
            else
               Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
         Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }

   if((!Result) && (CurrentLanguage))
   {
      if((String = Env->GetStringUTFChars(CurrentLanguage, NULL)) != NULL)
      {
         strncpy((char*)(ConfigSettings.CurrentLanguage.LanguageID), String, SPPM_MFI_MAXIMUM_SUPPORTED_LANGUAGE_ID_STRING_LENGTH);
         ConfigSettings.CurrentLanguage.LanguageID[SPPM_MFI_MAXIMUM_SUPPORTED_LANGUAGE_ID_STRING_LENGTH-1] = '\0';
         Env->ReleaseStringUTFChars(CurrentLanguage, String);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if((!Result) && (SupportedLanguages))
   {
      ConfigSettings.NumberSupportedLanguages = (Byte_t)(Env->GetArrayLength(SupportedLanguages));

      if((ConfigSettings.SupportedLanguagesList = (SPPM_MFi_Language_ID_String_t *)malloc(ConfigSettings.NumberSupportedLanguages * sizeof(SPPM_MFi_Language_ID_String_t))) != NULL)
      {
         memset(ConfigSettings.SupportedLanguagesList, 0, (ConfigSettings.NumberSupportedLanguages * sizeof(SPPM_MFi_Language_ID_String_t)));

         for(Index = 0; Index < ConfigSettings.NumberSupportedLanguages; Index++)
         {
            StringArrayElement = (jstring)(Env->GetObjectArrayElement(SupportedLanguages, Index));

            if((String = Env->GetStringUTFChars(StringArrayElement, NULL)) != NULL)
            {
               strncpy((char*)(ConfigSettings.SupportedLanguagesList[Index].LanguageID), String, SPPM_MFI_MAXIMUM_SUPPORTED_LANGUAGE_ID_STRING_LENGTH);
               ConfigSettings.SupportedLanguagesList[Index].LanguageID[SPPM_MFI_MAXIMUM_SUPPORTED_LANGUAGE_ID_STRING_LENGTH-1] = '\0';
               Env->ReleaseStringUTFChars(StringArrayElement, String);
            }
            else
            {
               Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               Index = (ConfigSettings.NumberSupportedLanguages - 1);
            }

            Env->DeleteLocalRef(StringArrayElement);
         }
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if((!Result) && (ControlMessageIDsSent))
   {
      ConfigSettings.NumberControlMessagesSent = (Word_t)(Env->GetArrayLength(ControlMessageIDsSent));

      if((ConfigSettings.ControlMessagesSentIDList = (Word_t *)malloc(ConfigSettings.NumberControlMessagesSent * WORD_SIZE)) != NULL)
      {
         if((MessageIDsArray = Env->GetShortArrayElements(ControlMessageIDsSent, NULL)) != NULL)
         {
            if(sizeof(jshort) == sizeof(Word_t))
            {
               Env->GetShortArrayRegion(ControlMessageIDsSent, 0, ConfigSettings.NumberControlMessagesSent, (jshort *)ConfigSettings.ControlMessagesSentIDList);
            }
            else
            {
               memset(ConfigSettings.ControlMessagesSentIDList, 0, (ConfigSettings.NumberControlMessagesSent * WORD_SIZE));

               if((MessageIDsArray = Env->GetShortArrayElements(ControlMessageIDsSent, NULL)) != NULL)
               {
                  for(Index = 0; Index < ConfigSettings.NumberControlMessagesSent; Index++)
                     ConfigSettings.ControlMessagesSentIDList[Index] = (Word_t)MessageIDsArray[Index];

                  Env->ReleaseShortArrayElements(ControlMessageIDsSent, MessageIDsArray, JNI_ABORT);
               }
            }
         }
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if((!Result) && (ControlMessageIDsReceived))
   {
      ConfigSettings.NumberControlMessagesReceived = (Word_t)(Env->GetArrayLength(ControlMessageIDsReceived));

      if((ConfigSettings.ControlMessagesReceivedIDList = (Word_t *)malloc(ConfigSettings.NumberControlMessagesReceived * WORD_SIZE)) != NULL)
      {
         if((MessageIDsArray = Env->GetShortArrayElements(ControlMessageIDsReceived, NULL)) != NULL)
         {
            if(sizeof(jshort) == sizeof(Word_t))
            {
               Env->GetShortArrayRegion(ControlMessageIDsReceived, 0, ConfigSettings.NumberControlMessagesReceived, (jshort *)ConfigSettings.ControlMessagesReceivedIDList);
            }
            else
            {
               memset(ConfigSettings.ControlMessagesReceivedIDList, 0, (ConfigSettings.NumberControlMessagesReceived * WORD_SIZE));

               if((MessageIDsArray = Env->GetShortArrayElements(ControlMessageIDsReceived, NULL)) != NULL)
               {
                  for(Index = 0; Index < ConfigSettings.NumberControlMessagesReceived; Index++)
                     ConfigSettings.ControlMessagesReceivedIDList[Index] = (Word_t)MessageIDsArray[Index];

                  Env->ReleaseShortArrayElements(ControlMessageIDsReceived, MessageIDsArray, JNI_ABORT);
               }
            }
         }
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   /* If no error has occurred, perform the API call.                   */
   if(!Result)
      Result = SPPM_ConfigureMFiSettings(&ConfigSettings);

   /* Free allocated resources.                                         */
   if(ConfigSettings.SupportedProtocolList)
      free(ConfigSettings.SupportedProtocolList);

   if(ConfigSettings.FIDTokenList)
   {
      for(Index = 0; Index < ConfigSettings.NumberFIDTokens; Index++)
      {
         if(ConfigSettings.FIDTokenList[Index].FIDData)
            free(ConfigSettings.FIDTokenList[Index].FIDData);
      }

      free(ConfigSettings.FIDTokenList);
   }

   if(ConfigSettings.SupportedLanguagesList)
      free(ConfigSettings.SupportedLanguagesList);

   /* Translate error codes into exceptions.                            */
   if(Result == BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY)
      Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);

   if(Result == BTPM_ERROR_CODE_INVALID_PARAMETER)
      Env->ThrowNew(Env->FindClass("java/lang/IllegalArgumentException"), NULL);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    queryConnectionTypeNative
 * Signature: (I[I)I
 */
static jint QueryConnectionTypeNative(JNIEnv *Env, jobject Object, jint PortHandle, jintArray ConnectionType)
{
   jint                   Result;
   jint                   JavaType;
   SPPM_Connection_Type_t NativeType;

   if((Result = SPPM_QueryConnectionType(PortHandle, &NativeType)) == 0)
   {
      switch(NativeType)
      {
         case sctMFi:
            JavaType = com_stonestreetone_bluetopiapm_SPPM_CONNECTION_TYPE_MFI;
            break;
         case sctSPP:
         default:
            JavaType = com_stonestreetone_bluetopiapm_SPPM_CONNECTION_TYPE_SPP;
            break;
      }

      Env->SetIntArrayRegion(ConnectionType, 0, 1, &JavaType);
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    openSessionRequestResponseNative
 * Signature: (IIZ)I
 */
static jint OpenSessionRequestResponseNative(JNIEnv *Env, jobject Object, jint PortHandle, jint SessionID, jboolean Accept)
{
   return SPPM_OpenSessionRequestResponse(PortHandle, SessionID, ((Accept == JNI_FALSE) ? FALSE : TRUE));
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    sendSessionDataNative
 * Signature: (II[B)I
 */
static jint SendSessionDataNative(JNIEnv *Env, jobject Object, jint PortHandle, jint SessionID, jbyteArray SessionData)
{
   jint   Result;
   jsize  DataLength;
   jbyte *DataArray;

   DataLength = Env->GetArrayLength(SessionData);

   if((DataArray = (jbyte*)(Env->GetPrimitiveArrayCritical(SessionData, NULL))) != NULL)
   {
      Result = SPPM_SendSessionData(PortHandle, SessionID, DataLength, (Byte_t*)DataArray);
      Env->ReleasePrimitiveArrayCritical(SessionData, DataArray, JNI_ABORT);
   }
   else
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    sendNonSessionDataNative
 * Signature: (IIII[B)I
 */
static jint SendNonSessionDataNative(JNIEnv *Env, jobject Object, jint PortHandle, jint LingoID, jint CommandID, jint TransactionID, jbyteArray Data)
{
   jint   Result;
   jsize  DataLength;
   jbyte *DataArray;

   DataLength = Env->GetArrayLength(Data);

   if((DataArray = (jbyte*)(Env->GetPrimitiveArrayCritical(Data, NULL))) != NULL)
   {
      Result = SPPM_SendNonSessionData(PortHandle, LingoID, CommandID, TransactionID, DataLength, (Byte_t*)DataArray);
      Env->ReleasePrimitiveArrayCritical(Data, DataArray, JNI_ABORT);
   }
   else
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    cancelPacketNative
 * Signature: (II)I
 */
static jint CancelPacketNative(JNIEnv *Env, jobject Object, jint PortHandle, jint PacketID)
{
   return SPPM_CancelPacket(PortHandle, PacketID);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SPPM
 * Method:    sendControlMessageNative
 * Signature: (IS[B)I
 */
static jint SendControlMessageNative(JNIEnv *Env, jobject Object, jint PortHandle, jshort MessageID, jbyteArray Data)
{
   jint   Result;
   jsize  DataLength;
   jbyte *DataArray;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(Data != NULL)
   {
      DataLength = Env->GetArrayLength(Data);

      if((DataArray = (jbyte*)(Env->GetPrimitiveArrayCritical(Data, NULL))) != NULL)
         Result = 0;
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }
   else
   {
      DataLength = 0;
      DataArray  = NULL;

      Result = 0;
   }

   if(!Result)
   {
      Result = SPPM_SendControlMessage(PortHandle, MessageID, DataLength, (Byte_t*)DataArray);

      if(DataArray != NULL)
         Env->ReleasePrimitiveArrayCritical(Data, DataArray, JNI_ABORT);
   }
   else
      Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                   "()V",                                    (void*) InitClassNative},
   {"initObjectNative",                  "()V",                                    (void*) InitObjectNative},
   {"cleanupObjectNative",               "()V",                                    (void*) CleanupObjectNative},
   {"disconnectNative",                  "(II)I",                                  (void*) DisconnectNative},
   {"readDataNative",                    "(II[BII)I",                              (void*) ReadDataNative},
   {"writeDataNative",                   "(II[BII)I",                              (void*) WriteDataNative},
   {"sendLineStatusNative",              "(II)I",                                  (void*) SendLineStatusNative},
   {"sendPortStatusNative",              "(IIZI)I",                                (void*) SendPortStatusNative},
   {"connectRemoteDeviceNative",         "(BBBBBBII[I)I",                          (void*) ConnectRemoteDeviceNative},
   {"connectionRequestResponseNative",   "(IZ)I",                                  (void*) ConnectionRequestResponseNative},
   {"registerServerPortNative",          "(II)I",                                  (void*) RegisterServerPortNative},
   {"registerSerialPortSDPRecordNative", "(I[J[J[J[JLjava/lang/String;)I",         (void*) RegisterSerialPortSDPRecordNative},
   {"unRegisterServerPortNative",        "(I)I",                                   (void*) UnregisterServerPortNative},
   {"queryRemoteDeviceServicesNative",   "(BBBBBB[[I[[J[[I[[Ljava/lang/String;)I", (void*) QueryRemoteDeviceServicesNative},
   {"configureMFiSettingsNative",        "(II[BJLjava/lang/String;IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/String;[ILjava/lang/String;[I[I[[BLjava/lang/String;[Ljava/lang/String;[S[S)I", (void*) ConfigureMFiSettingsNative},
   {"queryConnectionTypeNative",         "(I[I)I",                                 (void*) QueryConnectionTypeNative},
   {"openSessionRequestResponseNative",  "(IIZ)I",                                 (void*) OpenSessionRequestResponseNative},
   {"sendSessionDataNative",             "(II[B)I",                                (void*) SendSessionDataNative},
   {"sendNonSessionDataNative",          "(IIII[B)I",                              (void*) SendNonSessionDataNative},
   {"cancelPacketNative",                "(II)I",                                  (void*) CancelPacketNative},
   {"sendControlMessageNative",          "(IS[B)I",                                (void*) SendControlMessageNative},
};

int register_com_stonestreetone_bluetopiapm_SPPM(JNIEnv *Env)
{
   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/SPPM";

   Result = -1;

   PRINT_DEBUG("Registering SPPM native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}
