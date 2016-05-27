/*****< com_stonestreetone_bluetopiapm_GATM.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_GATM - JNI Module for Stonestreet One      */
/*                                        Bluetopia Platform Manager GATM     */
/*                                        Java API.                           */
/*                                                                            */
/*  Author:  Glenn Steenrod                                                   */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   11/21/12  G. Steenrod    Initial creation.                               */
/******************************************************************************/

#include <stdlib.h>

extern "C" {
#include "SS1BTPM.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_GATM.h"

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
static WEAK_REF Class_GATM;

static jfieldID Field_GATM_localData;

   /* Java event handlers.                                              */
static jmethodID Method_GATM_connected;
static jmethodID Method_GATM_disconnected;
static jmethodID Method_GATM_connectionMTUUpdate;
static jmethodID Method_GATM_handleValueData;

static jmethodID Method_GATM_readResponse;
static jmethodID Method_GATM_writeResponse;
static jmethodID Method_GATM_errorResponse;

static jmethodID Method_GATM_writeRequest;
static jmethodID Method_GATM_signedWrite;
static jmethodID Method_GATM_readRequest;
static jmethodID Method_GATM_prepareWriteRequest;
static jmethodID Method_GATM_commitPrepareWrite;
static jmethodID Method_GATM_handleValueConfirmation;

static jclass    Class_ServiceInformation;

static jmethodID Constructor_ServiceInformation;
static jmethodID Constructor_ServiceInformationUUID;

typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
} LocalData_t;

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_GATM_localData, Exclusive)) == NULL)
      PRINT_ERROR("GATM: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_GATM_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_GATM_localData, LocalData);
}

/*
 * Event callback handler.
 */
static void GATM_EventCallback(GATM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         IntegerValue01;
   jint         IntegerValue02;
   JNIEnv      *Env;
   jobject      GATMObject;
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
         /* The CallbackParameter is a weak ref to our GATM Manager     */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((GATMObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, GATMObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case getGATTConnected:

                           switch(EventData->EventData.ConnectedEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_connected,
                              IntegerValue01,
                              EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.ConnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.ConnectedEventData.MTU);

                           break;

                        case getGATTDisconnected:

                           switch(EventData->EventData.DisconnectedEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_disconnected,
                              IntegerValue01,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.DisconnectedEventData.RemoteDeviceAddress.BD_ADDR0);

                           break;

                        case getGATTConnectionMTUUpdate:

                           switch(EventData->EventData.ConnectionMTUUpdateEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_connectionMTUUpdate,
                              IntegerValue01,
                              EventData->EventData.ConnectionMTUUpdateEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.ConnectionMTUUpdateEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.ConnectionMTUUpdateEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.ConnectionMTUUpdateEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.ConnectionMTUUpdateEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.ConnectionMTUUpdateEventData.RemoteDeviceAddress.BD_ADDR0,
                              (jint)(EventData->EventData.ConnectionMTUUpdateEventData.MTU));

                           break;

                        case getGATTHandleValueData:

                           switch(EventData->EventData.HandleValueDataEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                            	  IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                            	  IntegerValue01 = 0;
                                 break;
                           }

                           if(EventData->EventData.HandleValueDataEventData.AttributeValueLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.HandleValueDataEventData.AttributeValueLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.HandleValueDataEventData.AttributeValueLength, (jbyte *)(EventData->EventData.HandleValueDataEventData.AttributeValue));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(GATMObject, Method_GATM_handleValueData,
                                    IntegerValue01,
                                    EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR0,
                                    ((EventData->EventData.HandleValueDataEventData.HandleValueIndication == FALSE) ? JNI_FALSE : JNI_TRUE),
                                    (jint)(EventData->EventData.HandleValueDataEventData.AttributeHandle),
                                    ByteArray);
                              }

                              if(ByteArray)
                                 Env->DeleteLocalRef(ByteArray);
                           }
                           else
                           {
                              Env->CallVoidMethod(GATMObject, Method_GATM_handleValueData,
                            	 IntegerValue01,
                                 EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.HandleValueDataEventData.RemoteDeviceAddress.BD_ADDR0,
                                 ((EventData->EventData.HandleValueDataEventData.HandleValueIndication == FALSE) ? JNI_FALSE : JNI_TRUE),
                                 (jint)(EventData->EventData.HandleValueDataEventData.AttributeHandle),
                                 NULL);
                           }

                           break;

                        case getGATTReadResponse:
                           PRINT_DEBUG("%s: GATT Read Response Event (%02X:%02X:%02X:%02X:%02X:%02X, %d, %d, %c, %d)", __FUNCTION__, EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                                     EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                                     EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                                     EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                                     EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                                     EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                                     EventData->EventData.ReadResponseEventData.TransactionID,
                                                                                                                                     EventData->EventData.ReadResponseEventData.Handle,
                                                                                                                                     (EventData->EventData.ReadResponseEventData.Final ? 'T' : 'F'),
                                                                                                                                     EventData->EventData.ReadResponseEventData.ValueLength);

                           switch(EventData->EventData.ReadResponseEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                            	 IntegerValue01 = 0;
                                 break;
                           }

                           if(EventData->EventData.ReadResponseEventData.ValueLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.ReadResponseEventData.ValueLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.ReadResponseEventData.ValueLength, (jbyte *)(EventData->EventData.ReadResponseEventData.Value));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(GATMObject, Method_GATM_readResponse,
                                    IntegerValue01,
                                    EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                    (jint)(EventData->EventData.ReadResponseEventData.TransactionID),
                                    (jint)(EventData->EventData.ReadResponseEventData.Handle),
                                    ((EventData->EventData.ReadResponseEventData.Final == TRUE) ? JNI_TRUE : JNI_FALSE),
                                    ByteArray);
                              }

                              if(ByteArray)
                                 Env->DeleteLocalRef(ByteArray);
                           }
                           else
                           {
                              Env->CallVoidMethod(GATMObject, Method_GATM_readResponse,
                                 IntegerValue01,
                                 EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.ReadResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                 (jint)(EventData->EventData.ReadResponseEventData.TransactionID),
                                 (jint)(EventData->EventData.ReadResponseEventData.Handle),
                                 ((EventData->EventData.ReadResponseEventData.Final == TRUE) ? JNI_TRUE : JNI_FALSE),
                                 NULL);
                           }

                           break;

                        case getGATTWriteResponse:

                           switch(EventData->EventData.WriteResponseEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_writeResponse,
                              IntegerValue01,
                              EventData->EventData.WriteResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.WriteResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.WriteResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.WriteResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.WriteResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.WriteResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.WriteResponseEventData.TransactionID,
                              EventData->EventData.WriteResponseEventData.Handle);

                           break;

                        case getGATTErrorResponse:
                           PRINT_DEBUG("%s: GATT Error Response Event (%02X:%02X:%02X:%02X:%02X:%02X, %d, %d, %d, %d)", __FUNCTION__, EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.TransactionID,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.Handle,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.ErrorType,
                                                                                                                                      EventData->EventData.ErrorResponseEventData.AttributeProtocolErrorCode);

                           switch(EventData->EventData.ErrorResponseEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           switch(EventData->EventData.ErrorResponseEventData.ErrorType)
                           {  
                              case retErrorResponse:
                                 IntegerValue02 = com_stonestreetone_bluetopiapm_GATM_REQUEST_ERROR_TYPE_ERROR_RESPONSE;
                                 break;
                              case retProtocolTimeout:
                                 IntegerValue02 = com_stonestreetone_bluetopiapm_GATM_REQUEST_ERROR_TYPE_PROTOCOL_TIMEOUT;
                                 break;
                              case retPrepareWriteDataMismatch:
                                 IntegerValue02 = com_stonestreetone_bluetopiapm_GATM_REQUEST_ERROR_TYPE_PREPARE_WRITE_DATA_MISMATCH;
                                 break;
                              default:
                                 IntegerValue02 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_errorResponse,
                              IntegerValue01,
                              EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.ErrorResponseEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.ErrorResponseEventData.TransactionID,
                              EventData->EventData.ErrorResponseEventData.Handle,
                              IntegerValue02,
                              EventData->EventData.ErrorResponseEventData.AttributeProtocolErrorCode);

                           break;

                        case getGATTWriteRequest:
                           PRINT_DEBUG("%s: GATT Write Request (%02X:%02X:%02X:%02X:%02X:%02X, %d, %d, %d, %d)", __FUNCTION__, EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                               EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                               EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                               EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                               EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                               EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                               EventData->EventData.WriteRequestData.ServiceID,
                                                                                                                               EventData->EventData.WriteRequestData.RequestID,
                                                                                                                               EventData->EventData.WriteRequestData.AttributeOffset,
                                                                                                                               EventData->EventData.WriteRequestData.DataLength);

                           switch(EventData->EventData.WriteRequestData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           if(EventData->EventData.WriteRequestData.DataLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.WriteRequestData.DataLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.WriteRequestData.DataLength, (jbyte *)(EventData->EventData.WriteRequestData.Data));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(GATMObject, Method_GATM_writeRequest,
                                    IntegerValue01,
                                    EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.WriteRequestData.ServiceID,
                                    EventData->EventData.WriteRequestData.RequestID,
                                    EventData->EventData.WriteRequestData.AttributeOffset,
                                    ByteArray);
                              }
                           }
                           else
                           {
                              Env->CallVoidMethod(GATMObject, Method_GATM_writeRequest,
                                 IntegerValue01,
                                 EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.WriteRequestData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.WriteRequestData.ServiceID,
                                 EventData->EventData.WriteRequestData.RequestID,
                                 EventData->EventData.WriteRequestData.AttributeOffset,
                                 NULL);                              
                           }

                           break;

                        case getGATTSignedWrite:
                           PRINT_DEBUG("%s: GATT Signed Write Request (%02X:%02X:%02X:%02X:%02X:%02X, %u, %c, %u, %u)", __FUNCTION__, EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                                      EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                                      EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                                      EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                                      EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                                      EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                                      EventData->EventData.SignedWriteData.ServiceID,
                                                                                                                                     (EventData->EventData.SignedWriteData.ValidSignature ? 'T' : 'F'),
                                                                                                                                      EventData->EventData.SignedWriteData.AttributeOffset,
                                                                                                                                      EventData->EventData.SignedWriteData.DataLength);

                           switch(EventData->EventData.SignedWriteData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           if(EventData->EventData.SignedWriteData.DataLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.SignedWriteData.DataLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.SignedWriteData.DataLength, (jbyte *)(EventData->EventData.SignedWriteData.Data));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(GATMObject, Method_GATM_signedWrite,
                                    IntegerValue01,
                                    EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.SignedWriteData.ServiceID,
                                  ((EventData->EventData.SignedWriteData.ValidSignature == TRUE) ? JNI_TRUE : JNI_FALSE),
                                    EventData->EventData.SignedWriteData.AttributeOffset,
                                    ByteArray);
                              }
                           }
                           else
                           {
                              Env->CallVoidMethod(GATMObject, Method_GATM_signedWrite,
                                 IntegerValue01,
                                 EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.SignedWriteData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.SignedWriteData.ServiceID,
                                 ((EventData->EventData.SignedWriteData.ValidSignature == TRUE) ? JNI_TRUE : JNI_FALSE),
                                 EventData->EventData.SignedWriteData.AttributeOffset,
                                 NULL);                              
                           }

                           break;

                        case getGATTReadRequest:
                           PRINT_DEBUG("%s: GATT Read Request (%02X:%02X:%02X:%02X:%02X:%02X, %u, %u, %u, %u)", __FUNCTION__, EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                              EventData->EventData.ReadRequestData.ServiceID,
                                                                                                                              EventData->EventData.ReadRequestData.RequestID,
                                                                                                                              EventData->EventData.ReadRequestData.AttributeOffset,
                                                                                                                              EventData->EventData.ReadRequestData.AttributeValueOffset);

                           switch(EventData->EventData.ReadRequestData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_readRequest,
                              IntegerValue01,
                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.ReadRequestData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.ReadRequestData.ServiceID,
                              EventData->EventData.ReadRequestData.RequestID,
                              EventData->EventData.ReadRequestData.AttributeOffset,
                              EventData->EventData.ReadRequestData.AttributeValueOffset);
                           
                           break;

                        case getGATTPrepareWriteRequest:
                           PRINT_DEBUG("%s: GATT Prepare Write Request (%02X:%02X:%02X:%02X:%02X:%02X, %u, %u, %u, %u, %u)", __FUNCTION__, EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.ServiceID,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.RequestID,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.AttributeOffset,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.AttributeValueOffset,
                                                                                                                                           EventData->EventData.PrepareWriteRequestEventData.DataLength);

                           switch(EventData->EventData.PrepareWriteRequestEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           if(EventData->EventData.PrepareWriteRequestEventData.DataLength)
                           {
                              if((ByteArray = Env->NewByteArray(EventData->EventData.PrepareWriteRequestEventData.DataLength)) != NULL)
                              {
                                 Env->SetByteArrayRegion(ByteArray, 0, EventData->EventData.PrepareWriteRequestEventData.DataLength, (jbyte *)(EventData->EventData.PrepareWriteRequestEventData.Data));
                              }

                              if(!(Env->ExceptionCheck()))
                              {
                                 Env->CallVoidMethod(GATMObject, Method_GATM_prepareWriteRequest,
                                    IntegerValue01,
                                    EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR0,
                                    EventData->EventData.PrepareWriteRequestEventData.ServiceID,
                                    EventData->EventData.PrepareWriteRequestEventData.RequestID,
                                    EventData->EventData.PrepareWriteRequestEventData.AttributeOffset,
                                    EventData->EventData.PrepareWriteRequestEventData.AttributeValueOffset,
                                    ByteArray);
                              }
                           }
                           else
                           {
                              Env->CallVoidMethod(GATMObject, Method_GATM_prepareWriteRequest,
                                 IntegerValue01,
                                 EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR5,
                                 EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR4,
                                 EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR3,
                                 EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR2,
                                 EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR1,
                                 EventData->EventData.PrepareWriteRequestEventData.RemoteDeviceAddress.BD_ADDR0,
                                 EventData->EventData.PrepareWriteRequestEventData.ServiceID,
                                 EventData->EventData.PrepareWriteRequestEventData.RequestID,
                                 EventData->EventData.PrepareWriteRequestEventData.AttributeOffset,
                                 EventData->EventData.PrepareWriteRequestEventData.AttributeValueOffset,
                                 NULL);                              
                           }

                           break;

                        case getGATTCommitPrepareWrite:
                           PRINT_DEBUG("%s: GATT Commit Prepare Write (%02X:%02X:%02X:%02X:%02X:%02X, %u, %c)", __FUNCTION__, EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                              EventData->EventData.CommitPrepareWriteEventData.ServiceID,
                                                                                                                              (EventData->EventData.CommitPrepareWriteEventData.CommitWrites ? 'T' : 'F'));

                           switch(EventData->EventData.CommitPrepareWriteEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_commitPrepareWrite,
                              IntegerValue01,
                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.CommitPrepareWriteEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.CommitPrepareWriteEventData.ServiceID,
                            ((EventData->EventData.CommitPrepareWriteEventData.CommitWrites == TRUE) ? JNI_TRUE : JNI_FALSE));

                           break;

                        case getGATTHandleValueConfirmation:
                           PRINT_DEBUG("%s: GATT Handle Value Confirmation  (%02X:%02X:%02X:%02X:%02X:%02X, %u, %u, %u, %u)", __FUNCTION__, EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.ServiceID,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.TransactionID,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.AttributeOffset,
                                                                                                                                            EventData->EventData.HandleValueConfirmationEventData.Status);

                           switch(EventData->EventData.HandleValueConfirmationEventData.ConnectionType)
                           {  
                              case gctLE:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                 break;
                              case gctBR_EDR:
                                 IntegerValue01 = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                 break;
                              default:
                                 IntegerValue01 = 0;
                                 break;
                           }

                           Env->CallVoidMethod(GATMObject, Method_GATM_handleValueConfirmation,
                              IntegerValue01,
                              EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                              EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                              EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                              EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                              EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                              EventData->EventData.HandleValueConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                              EventData->EventData.HandleValueConfirmationEventData.ServiceID,
                              EventData->EventData.HandleValueConfirmationEventData.TransactionID,
                              EventData->EventData.HandleValueConfirmationEventData.AttributeOffset,
                              EventData->EventData.HandleValueConfirmationEventData.Status);

                           break;
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during GATM event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, GATMObject, LocalData);
                  }

                  Env->DeleteLocalRef(GATMObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the GATM  */
                  /* java object. This can happen if the object has been      */
                  /* garbage collected or if the VM has run out of memory.    */
                  /* Now, check whether the object was GC'd. Since            */
                  /* NewLocalRef doesn't throw exceptions, we can do this with*/
                  /* IsSameObject.                                            */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The GATM Manager object has been GC'd. Since we are   */
                     /* still receiving events on this registration, the      */
                     /* Manager was not cleaned up properly.                  */
                     PRINT_ERROR("GATM Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it     */
                     /* could indicate a leak in this thread context.         */
                     PRINT_ERROR("VM reports 'Out of Memory' in GATM event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The GATM Manager object has been GC'd. Since we are still*/
               /* receiving events on this registration, the Manager was   */
               /* not cleaned up properly.                                 */
               PRINT_ERROR("Object reference is invalid: GATM Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: GATM Manager was already cleaned up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_GATM = (NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_GATM_localData = Env->GetFieldID(Clazz, "localData", "J")) == NULL)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_GATM_connected               = Env->GetMethodID(Clazz, "connected",                  "(IBBBBBBI)V");
      Method_GATM_disconnected            = Env->GetMethodID(Clazz, "disconnected",               "(IBBBBBB)V");
      Method_GATM_connectionMTUUpdate     = Env->GetMethodID(Clazz, "connectionMTUUpdate",        "(IBBBBBBI)V");  
      Method_GATM_handleValueData         = Env->GetMethodID(Clazz, "handleValue",                "(IBBBBBBZI[B)V");
      Method_GATM_readResponse            = Env->GetMethodID(Clazz, "readResponse",               "(IBBBBBBIIZ[B)V");
      Method_GATM_writeResponse           = Env->GetMethodID(Clazz, "writeResponse",              "(IBBBBBBII)V");
      Method_GATM_errorResponse           = Env->GetMethodID(Clazz, "errorResponse",              "(IBBBBBBIIIS)V");
      Method_GATM_writeRequest            = Env->GetMethodID(Clazz, "writeRequest",               "(IBBBBBBIII[B)V");
      Method_GATM_signedWrite             = Env->GetMethodID(Clazz, "signedWrite",                "(IBBBBBBIZI[B)V");
      Method_GATM_readRequest             = Env->GetMethodID(Clazz, "readRequest",                "(IBBBBBBIIII)V");
      Method_GATM_prepareWriteRequest     = Env->GetMethodID(Clazz, "prepareWriteRequest",        "(IBBBBBBIIII[B)V");
      Method_GATM_commitPrepareWrite      = Env->GetMethodID(Clazz, "commitPrepareWrite",         "(IBBBBBBIZ)V");
      Method_GATM_handleValueConfirmation = Env->GetMethodID(Clazz, "handleValueConfirmation",    "(IBBBBBBIIII)V");

      Class_ServiceInformation            = (jclass)Env->NewGlobalRef(Env->FindClass("com/stonestreetone/bluetopiapm/GATM$ServiceInformation"));
      Constructor_ServiceInformationUUID  = Env->GetMethodID(Class_ServiceInformation, "<init>", "(JJIII)V");
      Constructor_ServiceInformation      = Env->GetMethodID(Class_ServiceInformation, "<init>", "(III)V");
   }
   else
      PRINT_ERROR("GATM: Unable to load methods for class 'com.stonestreetone.bluetopiapm.GATM'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
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
            if((RegistrationResult = GATM_RegisterEventCallback(GATM_EventCallback, LocalData->WeakObjectReference)) > 0)
            {
               LocalData->CallbackID = RegistrationResult;
               SetLocalData(Env, Object, LocalData);
            }
            else
            {
               PRINT_ERROR("GATM: Unable to register callback for events (%d)", RegistrationResult);

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
            PRINT_ERROR("GATM: Out of Memory: Unable to obtain weak reference to object");

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
 * Class:     com_stonestreetone_bluetopiapm_GATM
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
         GATM_UnRegisterEventCallback(LocalData->CallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    queryConnectedDevicesNative
 * Signature: ([[I[[B)I
 */
static jint QueryConnectedDevicesNative(JNIEnv *Env, jobject Object, jobjectArray ConnectionTypeArray, jobjectArray RemoteDeviceAddressArray)
{
   int                            Result;
   int                            ReturnedConnectedDevices;
   unsigned int                   TotalNumberConnectedDevices;
   int                            BufferSize;
   jint                          *ConnectionTypePointer;
   jintArray                      ConnectionTypes;
   jbyte                         *RemoteDeviceAddressPointer;
   jbyteArray                     RemoteDeviceAddresses;
   GATM_Connection_Information_t *ConnectionListBuffer;
   int                            Index;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Result = 0;
   ReturnedConnectedDevices    = 0;
   TotalNumberConnectedDevices = 0;

   ReturnedConnectedDevices = GATM_QueryConnectedDevices(0, NULL, &TotalNumberConnectedDevices);

   if(!(ReturnedConnectedDevices < 0))
   {
      if((TotalNumberConnectedDevices > 0) && (ConnectionTypeArray) && (RemoteDeviceAddressArray))
      {
         BufferSize = sizeof(GATM_Connection_Information_t) * TotalNumberConnectedDevices;

         if((ConnectionListBuffer = (GATM_Connection_Information_t *)malloc(BufferSize)) != NULL)
         {
            ReturnedConnectedDevices = GATM_QueryConnectedDevices(TotalNumberConnectedDevices, &(ConnectionListBuffer[0]), NULL);

            if(ReturnedConnectedDevices > 0)
            {
               if((ConnectionTypes = Env->NewIntArray(ReturnedConnectedDevices)) != NULL)
               {
                  if((ConnectionTypePointer = Env->GetIntArrayElements(ConnectionTypes, NULL)) != NULL)
                  {
                     if((RemoteDeviceAddresses = Env->NewByteArray(ReturnedConnectedDevices * 6)) != NULL)
                     {
                        if((RemoteDeviceAddressPointer = Env->GetByteArrayElements(RemoteDeviceAddresses, NULL)) != NULL)
                        {
                           for(int Index = 0; Index < ReturnedConnectedDevices; Index++)
                           {
                              switch(ConnectionListBuffer[Index].ConnectionType)
                              {
                                 case gctLE:
                                    ConnectionTypePointer[Index] = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_LOW_ENERGY;
                                    break;
                                 case gctBR_EDR:
                                 default:
                                    ConnectionTypePointer[Index] = com_stonestreetone_bluetopiapm_GATM_CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE;
                                    break;
                              }
                             
                              RemoteDeviceAddressPointer[((Index * 6) + 0)] = ConnectionListBuffer[Index].RemoteDeviceAddress.BD_ADDR5;
                              RemoteDeviceAddressPointer[((Index * 6) + 1)] = ConnectionListBuffer[Index].RemoteDeviceAddress.BD_ADDR4;
                              RemoteDeviceAddressPointer[((Index * 6) + 2)] = ConnectionListBuffer[Index].RemoteDeviceAddress.BD_ADDR3;
                              RemoteDeviceAddressPointer[((Index * 6) + 3)] = ConnectionListBuffer[Index].RemoteDeviceAddress.BD_ADDR2;
                              RemoteDeviceAddressPointer[((Index * 6) + 4)] = ConnectionListBuffer[Index].RemoteDeviceAddress.BD_ADDR1;
                              RemoteDeviceAddressPointer[((Index * 6) + 5)] = ConnectionListBuffer[Index].RemoteDeviceAddress.BD_ADDR0;
                           }

                           Env->SetObjectArrayElement(ConnectionTypeArray, 0, ConnectionTypes);
                           if(Env->ExceptionCheck())
                           {
                              Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                           }
                           else
                           {
                              Env->SetObjectArrayElement(RemoteDeviceAddressArray, 0, RemoteDeviceAddresses);
                              if(Env->ExceptionCheck())
                              {
                                 Result = BTPM_ERROR_CODE_INSUFFICIENT_BUFFER_SIZE;
                              }
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
                  
                     Env->ReleaseIntArrayElements(ConnectionTypes, ConnectionTypePointer, 0);
                  }
                  else
                  {
                     Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
                  }

                  Env->DeleteLocalRef(ConnectionTypes);
               }
               else
               {
                  Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
               }
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

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    queryRemoteDeviceServicesNative
 * Signature: (BBBBBB[Ljava/lang/Object;)I
 */
static jint QueryRemoteDeviceServicesNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jobjectArray NativeParameters)
{
   int                                       Result;
   jint                                      IntValue;
   jlong                                     LongValue;
   Byte_t                                   *ServiceDataBuffer;
   BD_ADDR_t                                 RemoteDeviceAddress;
   UUID_16_t                                 TempUUID;
   UUID_128_t                                ServiceUUID;
   unsigned int                              Index;
   unsigned int                              DescriptorIndex;
   unsigned int                              ServiceResponseIndex;
   unsigned int                              TotalServiceDataLength;
   DEVM_Parsed_Services_Data_t               ParsedServiceData;
   GATT_Service_Discovery_Indication_Data_t *GATTServiceData;

   jobjectArray serviceUUIDsMajorOuterArray        = NULL;
   jobjectArray serviceUUIDsMinorOuterArray        = NULL;
   jobjectArray startHandlesOuterArray             = NULL;
   jobjectArray endHandlesOuterArray               = NULL;
   jobjectArray characteristicUUIDsMajorOuterArray = NULL;
   jobjectArray characteristicUUIDsMinorOuterArray = NULL;
   jobjectArray characteristicHandlesOuterArray    = NULL;
   jobjectArray characteristicPropertiesOuterArray = NULL;
   jobjectArray descriptorUUIDsMajorOuterArray     = NULL;
   jobjectArray descriptorUUIDsMinorOuterArray     = NULL;
   jobjectArray descriptorHandlesOuterArray        = NULL;

   jlongArray   serviceUUIDsMajorServiceArray           = NULL;
   jlongArray   serviceUUIDsMinorServiceArray           = NULL;
   jintArray    startHandlesServiceArray                = NULL;
   jintArray    endHandlesServiceArray                  = NULL;
   jlongArray   characteristicUUIDsMajorServiceArray    = NULL;
   jlongArray   characteristicUUIDsMinorServiceArray    = NULL;
   jintArray    characteristicHandlesServiceArray       = NULL;
   jintArray    characteristicPropertiesServiceArray    = NULL;
   jobjectArray descriptorUUIDsMajorServiceArray        = NULL;
   jobjectArray descriptorUUIDsMinorServiceArray        = NULL;
   jobjectArray descriptorHandlesServiceArray           = NULL;
   jlongArray   descriptorUUIDsMajorCharacteristicArray = NULL;
   jlongArray   descriptorUUIDsMinorCharacteristicArray = NULL;
   jintArray    descriptorHandlesCharacteristicArray    = NULL;

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   /* Verify arguments. */
   if((NativeParameters) && (Env->GetArrayLength(NativeParameters) == 11))
   {
      if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY, 0, NULL, &TotalServiceDataLength)) >= 0)
      {
         if((ServiceDataBuffer = (Byte_t *)malloc(TotalServiceDataLength)) != NULL)
         {
            if((Result = DEVM_QueryRemoteDeviceServices(RemoteDeviceAddress, DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY, TotalServiceDataLength, ServiceDataBuffer, NULL)) >= 0)
            {
               TotalServiceDataLength = Result;

               if(TotalServiceDataLength > 0)
               {
                  PRINT_DEBUG("%s: Cached service data located", __FUNCTION__);

                  if((Result = DEVM_ConvertRawServicesStreamToParsedServicesData(TotalServiceDataLength, ServiceDataBuffer, &ParsedServiceData)) == 0)
                  {
                     if(Env->PushLocalFrame(32) == 0)
                     {
                        if(((serviceUUIDsMajorOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[J"), NULL)) != NULL) &&
                           ((serviceUUIDsMinorOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[J"), NULL)) != NULL) &&
                           ((startHandlesOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[I"), NULL)) != NULL) &&
                           ((endHandlesOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[I"), NULL)) != NULL) &&
                           ((characteristicUUIDsMajorOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[J"), NULL)) != NULL) &&
                           ((characteristicUUIDsMinorOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[J"), NULL)) != NULL) &&
                           ((characteristicHandlesOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[I"), NULL)) != NULL) &&
                           ((characteristicPropertiesOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[I"), NULL)) != NULL) &&
                           ((descriptorUUIDsMajorOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[[J"), NULL)) != NULL) &&
                           ((descriptorUUIDsMinorOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[[J"), NULL)) != NULL) &&
                           ((descriptorHandlesOuterArray = Env->NewObjectArray(ParsedServiceData.NumberServices, Env->FindClass("[[I"), NULL)) != NULL))
                        {
                           PRINT_DEBUG("%s: Outer arrays allocated (space for %d services)", __FUNCTION__, ParsedServiceData.NumberServices);

                           /* Enumerate over the services. */
                           for(ServiceResponseIndex = 0; ServiceResponseIndex < ParsedServiceData.NumberServices; ServiceResponseIndex++)
                           {
                              PRINT_DEBUG("%s: Extracting service %d", __FUNCTION__, ServiceResponseIndex);

                              GATTServiceData = &(ParsedServiceData.GATTServiceDiscoveryIndicationData[ServiceResponseIndex]);

                              /* Allocate arrays to hold the service data. */
                              if(((serviceUUIDsMajorServiceArray = Env->NewLongArray(GATTServiceData->NumberOfIncludedService + 1)) != NULL) &&
                                 ((serviceUUIDsMinorServiceArray = Env->NewLongArray(GATTServiceData->NumberOfIncludedService + 1)) != NULL) &&
                                 ((startHandlesServiceArray = Env->NewIntArray(GATTServiceData->NumberOfIncludedService + 1)) != NULL) &&
                                 ((endHandlesServiceArray = Env->NewIntArray(GATTServiceData->NumberOfIncludedService + 1)) != NULL) &&
                                 ((characteristicUUIDsMajorServiceArray = Env->NewLongArray(GATTServiceData->NumberOfCharacteristics)) != NULL) &&
                                 ((characteristicUUIDsMinorServiceArray = Env->NewLongArray(GATTServiceData->NumberOfCharacteristics)) != NULL) &&
                                 ((characteristicHandlesServiceArray = Env->NewIntArray(GATTServiceData->NumberOfCharacteristics)) != NULL) &&
                                 ((characteristicPropertiesServiceArray = Env->NewIntArray(GATTServiceData->NumberOfCharacteristics)) != NULL) &&
                                 ((descriptorUUIDsMajorServiceArray = Env->NewObjectArray(GATTServiceData->NumberOfCharacteristics, Env->FindClass("[J"), NULL)) != NULL) &&
                                 ((descriptorUUIDsMinorServiceArray = Env->NewObjectArray(GATTServiceData->NumberOfCharacteristics, Env->FindClass("[J"), NULL)) != NULL) &&
                                 ((descriptorHandlesServiceArray = Env->NewObjectArray(GATTServiceData->NumberOfCharacteristics, Env->FindClass("[I"), NULL)) != NULL))
                              {
                                 PRINT_DEBUG("%s: Allocated inner arrays for service %d (for %d included services and %d characteristics)", __FUNCTION__, ServiceResponseIndex, GATTServiceData->NumberOfIncludedService, GATTServiceData->NumberOfCharacteristics);

                                 /* Load the primary service details into the first index of the */
                                 /* service information arrays. */
                                 switch(GATTServiceData->ServiceInformation.UUID.UUID_Type)
                                 {
                                    case guUUID_128:
                                       CONVERT_BLUETOOTH_UUID_128_TO_SDP_UUID_128(ServiceUUID, GATTServiceData->ServiceInformation.UUID.UUID.UUID_128);
                                       break;
                                    case guUUID_16:
                                       CONVERT_BLUETOOTH_UUID_16_TO_SDP_UUID_16(TempUUID, GATTServiceData->ServiceInformation.UUID.UUID.UUID_16);
                                       SDP_ASSIGN_BASE_UUID(ServiceUUID);
                                       ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(ServiceUUID, TempUUID);
                                       break;
                                    default:
                                       ASSIGN_SDP_UUID_128(ServiceUUID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
                                       break;
                                 }

                                 PRINT_DEBUG("%s:      UUID: %02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X", __FUNCTION__, ServiceUUID.UUID_Byte0, ServiceUUID.UUID_Byte1, ServiceUUID.UUID_Byte2, ServiceUUID.UUID_Byte3, ServiceUUID.UUID_Byte4, ServiceUUID.UUID_Byte5, ServiceUUID.UUID_Byte6, ServiceUUID.UUID_Byte7, ServiceUUID.UUID_Byte8, ServiceUUID.UUID_Byte9, ServiceUUID.UUID_Byte10, ServiceUUID.UUID_Byte11, ServiceUUID.UUID_Byte12, ServiceUUID.UUID_Byte13, ServiceUUID.UUID_Byte14, ServiceUUID.UUID_Byte15);

                                 LongValue = ((((jlong)(ServiceUUID.UUID_Byte0)  & 0x0FFL) << 56) |
                                              (((jlong)(ServiceUUID.UUID_Byte1)  & 0x0FFL) << 48) |
                                              (((jlong)(ServiceUUID.UUID_Byte2)  & 0x0FFL) << 40) |
                                              (((jlong)(ServiceUUID.UUID_Byte3)  & 0x0FFL) << 32) |
                                              (((jlong)(ServiceUUID.UUID_Byte4)  & 0x0FFL) << 24) |
                                              (((jlong)(ServiceUUID.UUID_Byte5)  & 0x0FFL) << 16) |
                                              (((jlong)(ServiceUUID.UUID_Byte6)  & 0x0FFL) << 8)  |
                                              (((jlong)(ServiceUUID.UUID_Byte7)  & 0x0FFL)));
                                 Env->SetLongArrayRegion(serviceUUIDsMajorServiceArray, 0, 1, &LongValue);

                                 LongValue = ((((jlong)(ServiceUUID.UUID_Byte8)  & 0x0FFL) << 56) |
                                              (((jlong)(ServiceUUID.UUID_Byte9)  & 0x0FFL) << 48) |
                                              (((jlong)(ServiceUUID.UUID_Byte10) & 0x0FFL) << 40) |
                                              (((jlong)(ServiceUUID.UUID_Byte11) & 0x0FFL) << 32) |
                                              (((jlong)(ServiceUUID.UUID_Byte12) & 0x0FFL) << 24) |
                                              (((jlong)(ServiceUUID.UUID_Byte13) & 0x0FFL) << 16) |
                                              (((jlong)(ServiceUUID.UUID_Byte14) & 0x0FFL) << 8)  |
                                              (((jlong)(ServiceUUID.UUID_Byte15) & 0x0FFL)));
                                 Env->SetLongArrayRegion(serviceUUIDsMinorServiceArray, 0, 1, &LongValue);

                                 PRINT_DEBUG("%s:      Start Handle: 0x%04X", __FUNCTION__, GATTServiceData->ServiceInformation.Service_Handle);

                                 IntValue = (jint)(GATTServiceData->ServiceInformation.Service_Handle);
                                 Env->SetIntArrayRegion(startHandlesServiceArray, 0, 1, &IntValue);

                                 PRINT_DEBUG("%s:      End Handle:   0x%04X", __FUNCTION__, GATTServiceData->ServiceInformation.End_Group_Handle);

                                 IntValue = (jint)(GATTServiceData->ServiceInformation.End_Group_Handle);
                                 Env->SetIntArrayRegion(endHandlesServiceArray, 0, 1, &IntValue);

                                 /* Now add the included services to the arrays at */
                                 /* indexes [1..n]. */
                                 for(Index = 0; Index < GATTServiceData->NumberOfIncludedService; Index++)
                                 {
                                    PRINT_DEBUG("%s:      Included Service %d:", __FUNCTION__, Index);

                                    switch(GATTServiceData->IncludedServiceList[Index].UUID.UUID_Type)
                                    {
                                       case guUUID_128:
                                          CONVERT_BLUETOOTH_UUID_128_TO_SDP_UUID_128(ServiceUUID, GATTServiceData->IncludedServiceList[Index].UUID.UUID.UUID_128);
                                          break;
                                       case guUUID_16:
                                          CONVERT_BLUETOOTH_UUID_16_TO_SDP_UUID_16(TempUUID, GATTServiceData->IncludedServiceList[Index].UUID.UUID.UUID_16);
                                          SDP_ASSIGN_BASE_UUID(ServiceUUID);
                                          ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(ServiceUUID, TempUUID);
                                          break;
                                       default:
                                          ASSIGN_SDP_UUID_128(ServiceUUID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
                                          break;
                                    }

                                    PRINT_DEBUG("%s:          UUID: %02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X", __FUNCTION__, ServiceUUID.UUID_Byte0, ServiceUUID.UUID_Byte1, ServiceUUID.UUID_Byte2, ServiceUUID.UUID_Byte3, ServiceUUID.UUID_Byte4, ServiceUUID.UUID_Byte5, ServiceUUID.UUID_Byte6, ServiceUUID.UUID_Byte7, ServiceUUID.UUID_Byte8, ServiceUUID.UUID_Byte9, ServiceUUID.UUID_Byte10, ServiceUUID.UUID_Byte11, ServiceUUID.UUID_Byte12, ServiceUUID.UUID_Byte13, ServiceUUID.UUID_Byte14, ServiceUUID.UUID_Byte15);

                                    LongValue = ((((jlong)(ServiceUUID.UUID_Byte0)  & 0x0FFL) << 56) |
                                                 (((jlong)(ServiceUUID.UUID_Byte1)  & 0x0FFL) << 48) |
                                                 (((jlong)(ServiceUUID.UUID_Byte2)  & 0x0FFL) << 40) |
                                                 (((jlong)(ServiceUUID.UUID_Byte3)  & 0x0FFL) << 32) |
                                                 (((jlong)(ServiceUUID.UUID_Byte4)  & 0x0FFL) << 24) |
                                                 (((jlong)(ServiceUUID.UUID_Byte5)  & 0x0FFL) << 16) |
                                                 (((jlong)(ServiceUUID.UUID_Byte6)  & 0x0FFL) << 8)  |
                                                 (((jlong)(ServiceUUID.UUID_Byte7)  & 0x0FFL)));
                                    Env->SetLongArrayRegion(serviceUUIDsMajorServiceArray, (Index + 1), 1, &LongValue);

                                    LongValue = ((((jlong)(ServiceUUID.UUID_Byte8)  & 0x0FFL) << 56) |
                                                 (((jlong)(ServiceUUID.UUID_Byte9)  & 0x0FFL) << 48) |
                                                 (((jlong)(ServiceUUID.UUID_Byte10) & 0x0FFL) << 40) |
                                                 (((jlong)(ServiceUUID.UUID_Byte11) & 0x0FFL) << 32) |
                                                 (((jlong)(ServiceUUID.UUID_Byte12) & 0x0FFL) << 24) |
                                                 (((jlong)(ServiceUUID.UUID_Byte13) & 0x0FFL) << 16) |
                                                 (((jlong)(ServiceUUID.UUID_Byte14) & 0x0FFL) << 8)  |
                                                 (((jlong)(ServiceUUID.UUID_Byte15) & 0x0FFL)));
                                    Env->SetLongArrayRegion(serviceUUIDsMinorServiceArray, (Index + 1), 1, &LongValue);

                                    PRINT_DEBUG("%s:          Start Handle: 0x%04X", __FUNCTION__, GATTServiceData->IncludedServiceList[Index].Service_Handle);

                                    IntValue = (jint)(GATTServiceData->IncludedServiceList[Index].Service_Handle);
                                    Env->SetIntArrayRegion(startHandlesServiceArray, (Index + 1), 1, &IntValue);

                                    PRINT_DEBUG("%s:          End Handle:   0x%04X", __FUNCTION__, GATTServiceData->IncludedServiceList[Index].End_Group_Handle);

                                    IntValue = (jint)(GATTServiceData->IncludedServiceList[Index].End_Group_Handle);
                                    Env->SetIntArrayRegion(endHandlesServiceArray, (Index + 1), 1, &IntValue);
                                 }

                                 for(Index = 0; Index < GATTServiceData->NumberOfCharacteristics; Index++)
                                 {
                                    PRINT_DEBUG("%s:      Characteristic: %d", __FUNCTION__, Index);

                                    switch(GATTServiceData->CharacteristicInformationList[Index].Characteristic_UUID.UUID_Type)
                                    {
                                       case guUUID_128:
                                          CONVERT_BLUETOOTH_UUID_128_TO_SDP_UUID_128(ServiceUUID, GATTServiceData->CharacteristicInformationList[Index].Characteristic_UUID.UUID.UUID_128);
                                          break;
                                       case guUUID_16:
                                          CONVERT_BLUETOOTH_UUID_16_TO_SDP_UUID_16(TempUUID, GATTServiceData->CharacteristicInformationList[Index].Characteristic_UUID.UUID.UUID_16);
                                          SDP_ASSIGN_BASE_UUID(ServiceUUID);
                                          ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(ServiceUUID, TempUUID);
                                          break;
                                       default:
                                          ASSIGN_SDP_UUID_128(ServiceUUID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
                                          break;
                                    }

                                    PRINT_DEBUG("%s:          UUID: %02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X", __FUNCTION__, ServiceUUID.UUID_Byte0, ServiceUUID.UUID_Byte1, ServiceUUID.UUID_Byte2, ServiceUUID.UUID_Byte3, ServiceUUID.UUID_Byte4, ServiceUUID.UUID_Byte5, ServiceUUID.UUID_Byte6, ServiceUUID.UUID_Byte7, ServiceUUID.UUID_Byte8, ServiceUUID.UUID_Byte9, ServiceUUID.UUID_Byte10, ServiceUUID.UUID_Byte11, ServiceUUID.UUID_Byte12, ServiceUUID.UUID_Byte13, ServiceUUID.UUID_Byte14, ServiceUUID.UUID_Byte15);

                                    LongValue = ((((jlong)(ServiceUUID.UUID_Byte0)  & 0x0FFL) << 56) |
                                                 (((jlong)(ServiceUUID.UUID_Byte1)  & 0x0FFL) << 48) |
                                                 (((jlong)(ServiceUUID.UUID_Byte2)  & 0x0FFL) << 40) |
                                                 (((jlong)(ServiceUUID.UUID_Byte3)  & 0x0FFL) << 32) |
                                                 (((jlong)(ServiceUUID.UUID_Byte4)  & 0x0FFL) << 24) |
                                                 (((jlong)(ServiceUUID.UUID_Byte5)  & 0x0FFL) << 16) |
                                                 (((jlong)(ServiceUUID.UUID_Byte6)  & 0x0FFL) << 8)  |
                                                 (((jlong)(ServiceUUID.UUID_Byte7)  & 0x0FFL)));
                                    Env->SetLongArrayRegion(characteristicUUIDsMajorServiceArray, Index, 1, &LongValue);

                                    LongValue = ((((jlong)(ServiceUUID.UUID_Byte8)  & 0x0FFL) << 56) |
                                                 (((jlong)(ServiceUUID.UUID_Byte9)  & 0x0FFL) << 48) |
                                                 (((jlong)(ServiceUUID.UUID_Byte10) & 0x0FFL) << 40) |
                                                 (((jlong)(ServiceUUID.UUID_Byte11) & 0x0FFL) << 32) |
                                                 (((jlong)(ServiceUUID.UUID_Byte12) & 0x0FFL) << 24) |
                                                 (((jlong)(ServiceUUID.UUID_Byte13) & 0x0FFL) << 16) |
                                                 (((jlong)(ServiceUUID.UUID_Byte14) & 0x0FFL) << 8)  |
                                                 (((jlong)(ServiceUUID.UUID_Byte15) & 0x0FFL)));
                                    Env->SetLongArrayRegion(characteristicUUIDsMinorServiceArray, Index, 1, &LongValue);

                                    PRINT_DEBUG("%s:          Start Handle: 0x%04X", __FUNCTION__, GATTServiceData->CharacteristicInformationList[Index].Characteristic_Handle);

                                    IntValue = (jint)(GATTServiceData->CharacteristicInformationList[Index].Characteristic_Handle);
                                    Env->SetIntArrayRegion(characteristicHandlesServiceArray, Index, 1, &IntValue);

                                    PRINT_DEBUG("%s:          End Handle:   0x%04X", __FUNCTION__, GATTServiceData->CharacteristicInformationList[Index].Characteristic_Properties);

                                    IntValue = (jint)(GATTServiceData->CharacteristicInformationList[Index].Characteristic_Properties);
                                    Env->SetIntArrayRegion(characteristicPropertiesServiceArray, Index, 1, &IntValue);

                                    if(((descriptorUUIDsMajorCharacteristicArray = Env->NewLongArray(GATTServiceData->CharacteristicInformationList[Index].NumberOfDescriptors)) != NULL) &&
                                       ((descriptorUUIDsMinorCharacteristicArray = Env->NewLongArray(GATTServiceData->CharacteristicInformationList[Index].NumberOfDescriptors)) != NULL) &&
                                       ((descriptorHandlesCharacteristicArray = Env->NewIntArray(GATTServiceData->CharacteristicInformationList[Index].NumberOfDescriptors)) != NULL))
                                    {
                                       PRINT_DEBUG("%s:            (Allocated arrays for %d descriptors)", __FUNCTION__, GATTServiceData->CharacteristicInformationList[Index].NumberOfDescriptors);

                                       for(DescriptorIndex = 0; DescriptorIndex < GATTServiceData->CharacteristicInformationList[Index].NumberOfDescriptors; DescriptorIndex++)
                                       {
                                          PRINT_DEBUG("%s:          Descriptor %d:", __FUNCTION__, DescriptorIndex);

                                          switch(GATTServiceData->CharacteristicInformationList[Index].DescriptorList[DescriptorIndex].Characteristic_Descriptor_UUID.UUID_Type)
                                          {
                                             case guUUID_128:
                                                CONVERT_BLUETOOTH_UUID_128_TO_SDP_UUID_128(ServiceUUID, GATTServiceData->CharacteristicInformationList[Index].DescriptorList[DescriptorIndex].Characteristic_Descriptor_UUID.UUID.UUID_128);
                                                break;
                                             case guUUID_16:
                                                CONVERT_BLUETOOTH_UUID_16_TO_SDP_UUID_16(TempUUID, GATTServiceData->CharacteristicInformationList[Index].DescriptorList[DescriptorIndex].Characteristic_Descriptor_UUID.UUID.UUID_16);
                                                SDP_ASSIGN_BASE_UUID(ServiceUUID);
                                                ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(ServiceUUID, TempUUID);
                                                break;
                                             default:
                                                ASSIGN_SDP_UUID_128(ServiceUUID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
                                                break;
                                          }

                                          PRINT_DEBUG("%s:              UUID: %02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X", __FUNCTION__, ServiceUUID.UUID_Byte0, ServiceUUID.UUID_Byte1, ServiceUUID.UUID_Byte2, ServiceUUID.UUID_Byte3, ServiceUUID.UUID_Byte4, ServiceUUID.UUID_Byte5, ServiceUUID.UUID_Byte6, ServiceUUID.UUID_Byte7, ServiceUUID.UUID_Byte8, ServiceUUID.UUID_Byte9, ServiceUUID.UUID_Byte10, ServiceUUID.UUID_Byte11, ServiceUUID.UUID_Byte12, ServiceUUID.UUID_Byte13, ServiceUUID.UUID_Byte14, ServiceUUID.UUID_Byte15);

                                          LongValue = ((((jlong)(ServiceUUID.UUID_Byte0)  & 0x0FFL) << 56) |
                                                       (((jlong)(ServiceUUID.UUID_Byte1)  & 0x0FFL) << 48) |
                                                       (((jlong)(ServiceUUID.UUID_Byte2)  & 0x0FFL) << 40) |
                                                       (((jlong)(ServiceUUID.UUID_Byte3)  & 0x0FFL) << 32) |
                                                       (((jlong)(ServiceUUID.UUID_Byte4)  & 0x0FFL) << 24) |
                                                       (((jlong)(ServiceUUID.UUID_Byte5)  & 0x0FFL) << 16) |
                                                       (((jlong)(ServiceUUID.UUID_Byte6)  & 0x0FFL) << 8)  |
                                                       (((jlong)(ServiceUUID.UUID_Byte7)  & 0x0FFL)));
                                          Env->SetLongArrayRegion(descriptorUUIDsMajorCharacteristicArray, DescriptorIndex, 1, &LongValue);

                                          LongValue = ((((jlong)(ServiceUUID.UUID_Byte8)  & 0x0FFL) << 56) |
                                                       (((jlong)(ServiceUUID.UUID_Byte9)  & 0x0FFL) << 48) |
                                                       (((jlong)(ServiceUUID.UUID_Byte10) & 0x0FFL) << 40) |
                                                       (((jlong)(ServiceUUID.UUID_Byte11) & 0x0FFL) << 32) |
                                                       (((jlong)(ServiceUUID.UUID_Byte12) & 0x0FFL) << 24) |
                                                       (((jlong)(ServiceUUID.UUID_Byte13) & 0x0FFL) << 16) |
                                                       (((jlong)(ServiceUUID.UUID_Byte14) & 0x0FFL) << 8)  |
                                                       (((jlong)(ServiceUUID.UUID_Byte15) & 0x0FFL)));
                                          Env->SetLongArrayRegion(descriptorUUIDsMinorCharacteristicArray, DescriptorIndex, 1, &LongValue);

                                          PRINT_DEBUG("%s:              Handle: 0x%04X", __FUNCTION__, GATTServiceData->CharacteristicInformationList[Index].DescriptorList[DescriptorIndex].Characteristic_Descriptor_Handle);

                                          IntValue = (jint)(GATTServiceData->CharacteristicInformationList[Index].DescriptorList[DescriptorIndex].Characteristic_Descriptor_Handle);
                                          Env->SetIntArrayRegion(descriptorHandlesCharacteristicArray, DescriptorIndex, 1, &IntValue);
                                       }

                                       PRINT_DEBUG("%s: Storing descriptor arrays for characteristic %d", __FUNCTION__, Index);

                                       Env->SetObjectArrayElement(descriptorUUIDsMajorServiceArray, Index, descriptorUUIDsMajorCharacteristicArray);
                                       Env->SetObjectArrayElement(descriptorUUIDsMinorServiceArray, Index, descriptorUUIDsMinorCharacteristicArray);
                                       Env->SetObjectArrayElement(descriptorHandlesServiceArray,    Index, descriptorHandlesCharacteristicArray);
                                    }
                                    else
                                    {
                                       PRINT_DEBUG("%s: Failed to allocate arrays for descriptors for characteristic %d", __FUNCTION__, Index);

                                       if(Env->ExceptionCheck() == JNI_FALSE)
                                          Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                                    }

                                    PRINT_DEBUG("%s: Deleting references to descriptor arrays for characteristic %d", __FUNCTION__, Index);

                                    if(descriptorUUIDsMajorCharacteristicArray)
                                       Env->DeleteLocalRef(descriptorUUIDsMajorCharacteristicArray);
                                    if(descriptorUUIDsMinorCharacteristicArray)
                                       Env->DeleteLocalRef(descriptorUUIDsMinorCharacteristicArray);
                                    if(descriptorHandlesCharacteristicArray)
                                       Env->DeleteLocalRef(descriptorHandlesCharacteristicArray);

                                    if((Env->ExceptionCheck() != JNI_FALSE) || (Result < 0))
                                    {
                                       PRINT_DEBUG("%s: Something failed (Result = %d)", __FUNCTION__, Result);

                                       Result = -1;
                                       break;
                                    }
                                 }

                                 /* Load the per-service arrays into the outer arrays. */
                                 if(Result >= 0)
                                 {
                                    PRINT_DEBUG("%s: Storing arrays for service %d", __FUNCTION__, ServiceResponseIndex);

                                    Env->SetObjectArrayElement(serviceUUIDsMajorOuterArray,        ServiceResponseIndex, serviceUUIDsMajorServiceArray);
                                    Env->SetObjectArrayElement(serviceUUIDsMinorOuterArray,        ServiceResponseIndex, serviceUUIDsMinorServiceArray);
                                    Env->SetObjectArrayElement(startHandlesOuterArray,             ServiceResponseIndex, startHandlesServiceArray);
                                    Env->SetObjectArrayElement(endHandlesOuterArray,               ServiceResponseIndex, endHandlesServiceArray);
                                    Env->SetObjectArrayElement(characteristicUUIDsMajorOuterArray, ServiceResponseIndex, characteristicUUIDsMajorServiceArray);
                                    Env->SetObjectArrayElement(characteristicUUIDsMinorOuterArray, ServiceResponseIndex, characteristicUUIDsMinorServiceArray);
                                    Env->SetObjectArrayElement(characteristicHandlesOuterArray,    ServiceResponseIndex, characteristicHandlesServiceArray);
                                    Env->SetObjectArrayElement(characteristicPropertiesOuterArray, ServiceResponseIndex, characteristicPropertiesServiceArray);
                                    Env->SetObjectArrayElement(descriptorUUIDsMajorOuterArray,     ServiceResponseIndex, descriptorUUIDsMajorServiceArray);
                                    Env->SetObjectArrayElement(descriptorUUIDsMinorOuterArray,     ServiceResponseIndex, descriptorUUIDsMinorServiceArray);
                                    Env->SetObjectArrayElement(descriptorHandlesOuterArray,        ServiceResponseIndex, descriptorHandlesServiceArray);
                                 }
                              } else {
                                 PRINT_DEBUG("%s: Error while allocated inner arrays for service %d", __FUNCTION__, ServiceResponseIndex);

                                 if(Env->ExceptionCheck() == JNI_FALSE)
                                    Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
                              }

                              PRINT_DEBUG("%s: Deleting references to arrays for service %d", __FUNCTION__, ServiceResponseIndex);

                              if(serviceUUIDsMajorServiceArray)
                                 Env->DeleteLocalRef(serviceUUIDsMajorServiceArray);
                              if(serviceUUIDsMinorServiceArray)
                                 Env->DeleteLocalRef(serviceUUIDsMinorServiceArray);
                              if(startHandlesServiceArray)
                                 Env->DeleteLocalRef(startHandlesServiceArray);
                              if(endHandlesServiceArray)
                                 Env->DeleteLocalRef(endHandlesServiceArray);
                              if(characteristicUUIDsMajorServiceArray)
                                 Env->DeleteLocalRef(characteristicUUIDsMajorServiceArray);
                              if(characteristicUUIDsMinorServiceArray)
                                 Env->DeleteLocalRef(characteristicUUIDsMinorServiceArray);
                              if(characteristicHandlesServiceArray)
                                 Env->DeleteLocalRef(characteristicHandlesServiceArray);
                              if(characteristicPropertiesServiceArray)
                                 Env->DeleteLocalRef(characteristicPropertiesServiceArray);
                              if(descriptorUUIDsMajorServiceArray)
                                 Env->DeleteLocalRef(descriptorUUIDsMajorServiceArray);
                              if(descriptorUUIDsMinorServiceArray)
                                 Env->DeleteLocalRef(descriptorUUIDsMinorServiceArray);
                              if(descriptorHandlesServiceArray)
                                 Env->DeleteLocalRef(descriptorHandlesServiceArray);

                              if((Env->ExceptionCheck() != JNI_FALSE) || (Result < 0))
                              {
                                 PRINT_DEBUG("%s: Something broke (Result = %d)", __FUNCTION__, Result);

                                 Result = -1;
                                 break;
                              }
                           }

                           /* Load the outer arrays into the parameter array for returning to */
                           /* the caller. */
                           if(Result >= 0)
                           {
                              PRINT_DEBUG("%s: Populating return parameter array", __FUNCTION__);

                              Env->SetObjectArrayElement(NativeParameters, 0,  serviceUUIDsMajorOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 1,  serviceUUIDsMinorOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 2,  startHandlesOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 3,  endHandlesOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 4,  characteristicUUIDsMajorOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 5,  characteristicUUIDsMinorOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 6,  characteristicHandlesOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 7,  characteristicPropertiesOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 8,  descriptorUUIDsMajorOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 9,  descriptorUUIDsMinorOuterArray);
                              Env->SetObjectArrayElement(NativeParameters, 10, descriptorHandlesOuterArray);
                           }
                        } else {
                           /* NewObjectArray has already thrown an exception, so just */
                           /* return an error. */
                           Result = -1;

                           PRINT_DEBUG("%s: At least one of the outer arrays could not be allocated", __FUNCTION__);
                        }

                        PRINT_DEBUG("%s: Deleting references to outer arrays", __FUNCTION__);

                        if(serviceUUIDsMajorOuterArray)
                           Env->DeleteLocalRef(serviceUUIDsMajorOuterArray);
                        if(serviceUUIDsMinorOuterArray)
                           Env->DeleteLocalRef(serviceUUIDsMinorOuterArray);
                        if(startHandlesOuterArray)
                           Env->DeleteLocalRef(startHandlesOuterArray);
                        if(endHandlesOuterArray)
                           Env->DeleteLocalRef(endHandlesOuterArray);
                        if(characteristicUUIDsMajorOuterArray)
                           Env->DeleteLocalRef(characteristicUUIDsMajorOuterArray);
                        if(characteristicUUIDsMinorOuterArray)
                           Env->DeleteLocalRef(characteristicUUIDsMinorOuterArray);
                        if(characteristicHandlesOuterArray)
                           Env->DeleteLocalRef(characteristicHandlesOuterArray);
                        if(characteristicPropertiesOuterArray)
                           Env->DeleteLocalRef(characteristicPropertiesOuterArray);
                        if(descriptorUUIDsMajorOuterArray)
                           Env->DeleteLocalRef(descriptorUUIDsMajorOuterArray);
                        if(descriptorUUIDsMinorOuterArray)
                           Env->DeleteLocalRef(descriptorUUIDsMinorOuterArray);
                        if(descriptorHandlesOuterArray)
                           Env->DeleteLocalRef(descriptorHandlesOuterArray);

                        PRINT_DEBUG("%s: Popping stack frame", __FUNCTION__);

                        Env->PopLocalFrame(NULL);
                     }
                     else
                     {
                        PRINT_DEBUG("%s: Unable to allocate new JNI stack frame", __FUNCTION__);
                        Result = -1;
                     }

                     PRINT_DEBUG("%s: Freeing parsed services data", __FUNCTION__);

                     DEVM_FreeParsedServicesData(&ParsedServiceData);
                  }
                  else
                     PRINT_DEBUG("%s: Error parsing service data: %d", __FUNCTION__, Result);

               }
               else
               {
                  PRINT_DEBUG("%s: Cached service data not available", __FUNCTION__);

                  /* Return empty lists to indicate that the  */
                  /* query was successful but returned no results.      */
                  if(((serviceUUIDsMajorOuterArray = Env->NewObjectArray(0, Env->FindClass("[J"), NULL)) != NULL) &&
                     ((serviceUUIDsMinorOuterArray = Env->NewObjectArray(0, Env->FindClass("[J"), NULL)) != NULL) &&
                     ((startHandlesOuterArray = Env->NewObjectArray(0, Env->FindClass("[I"), NULL)) != NULL) &&
                     ((endHandlesOuterArray = Env->NewObjectArray(0, Env->FindClass("[I"), NULL)) != NULL) &&
                     ((characteristicUUIDsMajorOuterArray = Env->NewObjectArray(0, Env->FindClass("[J"), NULL)) != NULL) &&
                     ((characteristicUUIDsMinorOuterArray = Env->NewObjectArray(0, Env->FindClass("[J"), NULL)) != NULL) &&
                     ((characteristicHandlesOuterArray = Env->NewObjectArray(0, Env->FindClass("[I"), NULL)) != NULL) &&
                     ((characteristicPropertiesOuterArray = Env->NewObjectArray(0, Env->FindClass("[I"), NULL)) != NULL) &&
                     ((descriptorUUIDsMajorOuterArray = Env->NewObjectArray(0, Env->FindClass("[[J"), NULL)) != NULL) &&
                     ((descriptorUUIDsMinorOuterArray = Env->NewObjectArray(0, Env->FindClass("[[J"), NULL)) != NULL) &&
                     ((descriptorHandlesOuterArray = Env->NewObjectArray(0, Env->FindClass("[[I"), NULL)) != NULL))
                  {
                     PRINT_DEBUG("%s: Populating return parameter array", __FUNCTION__);

                     Env->SetObjectArrayElement(NativeParameters, 0,  serviceUUIDsMajorOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 1,  serviceUUIDsMinorOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 2,  startHandlesOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 3,  endHandlesOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 4,  characteristicUUIDsMajorOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 5,  characteristicUUIDsMinorOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 6,  characteristicHandlesOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 7,  characteristicPropertiesOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 8,  descriptorUUIDsMajorOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 9,  descriptorUUIDsMinorOuterArray);
                     Env->SetObjectArrayElement(NativeParameters, 10, descriptorHandlesOuterArray);
                  }
               }
            }
            else
               PRINT_DEBUG("%s: Actual SDP Cache query failed: %d", __FUNCTION__, Result);

            PRINT_DEBUG("%s: Freeing raw service data buffer", __FUNCTION__);

            free(ServiceDataBuffer);
         }
         else
         {
            PRINT_DEBUG("%s: Unable to allocate memory for query result", __FUNCTION__);
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
         }
      }
      else
         PRINT_DEBUG("%s: Initial SDP Cache query failed: %d", __FUNCTION__, Result);
   }
   else
   {
      PRINT_DEBUG("%s: Output parameter array was not given or is the wrong length", __FUNCTION__);
      Result = -1;
   }

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    readValueNative
 * Signature: (BBBBBBIIZ)I
 */
static jint ReadValueNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint AttributeHandle, jint Offset, jboolean ReadAll)
{
   jint         Result;
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter (%02x:%02x:%02x:%02x:%02x:%02x, %d, %d, %d)", __FUNCTION__, Address1, Address2, Address3, Address4, Address5, Address6, AttributeHandle, Offset, ReadAll);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      Result = GATM_ReadValue(LocalData->CallbackID, RemoteDeviceAddress, AttributeHandle, Offset, ((ReadAll == JNI_FALSE) ? FALSE : TRUE));
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    writeValueNative
 * Signature: (BBBBBBI[B)I
 */
static jint WriteValueNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint AttributeHandle, jbyteArray DataArray)
{
   jint         Result;
   jbyte       *Data; 
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter (%02x:%02x:%02x:%02x:%02x:%02x, %d, %d)", __FUNCTION__, Address1, Address2, Address3, Address4, Address5, Address6, AttributeHandle, Env->GetArrayLength(DataArray));

   if(DataArray)
   {
      if((Data = Env->GetByteArrayElements(DataArray, NULL)) != NULL)
      {
         ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

         if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
         {
            Result = GATM_WriteValue(LocalData->CallbackID, RemoteDeviceAddress, AttributeHandle, Env->GetArrayLength(DataArray), (Byte_t *)Data);
   
            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = -1;

         Env->ReleaseByteArrayElements(DataArray, Data, 0);
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

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    writeValueWithoutResponseNative
 * Signature: (BBBBBBI[B)I
 */
static jint WriteValueWithoutResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint AttributeHandle, jbyteArray DataArray)
{
   jint         Result;
   jbyte       *Data; 
   BD_ADDR_t    RemoteDeviceAddress;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter (%02x:%02x:%02x:%02x:%02x:%02x, %d, %d)", __FUNCTION__, Address1, Address2, Address3, Address4, Address5, Address6, AttributeHandle, Env->GetArrayLength(DataArray));

   if(DataArray)
   {
      if((Data = Env->GetByteArrayElements(DataArray, NULL)) != NULL)
      {
         ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

         if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
         {
            Result = GATM_WriteValueWithoutResponse(LocalData->CallbackID, RemoteDeviceAddress, AttributeHandle, FALSE, Env->GetArrayLength(DataArray), (Byte_t *)Data);
   
            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = -1;

         Env->ReleaseByteArrayElements(DataArray, Data, 0);
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

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    registerPersistentUIDNative
 * Signature: (I[I)I
 */
static jint RegisterPersistentUIDNative(JNIEnv *Env, jobject Object, jint NumberOfAttributes, jintArray HandleData)
{
   int                           *HandleDataBuffer;
   jint                           Result;
   DWord_t                        PersistentUIDResult;
   GATT_Attribute_Handle_Group_t  ServiceHandleRange;

   PRINT_DEBUG("%s: Enter (%d)", __FUNCTION__, NumberOfAttributes);

   if((Result = GATM_RegisterPersistentUID(NumberOfAttributes, &PersistentUIDResult, &ServiceHandleRange)) == 0)
   {
   PRINT_DEBUG("%s: %lu (int)%d", __FUNCTION__, PersistentUIDResult, (int)PersistentUIDResult);
      if((HandleDataBuffer = (int *)malloc(sizeof(int) * 3)) != NULL)
      {
         HandleDataBuffer[0] = (int)PersistentUIDResult;
         HandleDataBuffer[1] = ServiceHandleRange.Starting_Handle;
         HandleDataBuffer[2] = ServiceHandleRange.Ending_Handle;
         Env->SetIntArrayRegion(HandleData, 0, 3, HandleDataBuffer);

         free(HandleDataBuffer);
      }
   }
   
   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    unRegisterPersistentUIDNative
 * Signature: (I)I
 */
static jint UnRegisterPersistentUIDNative(JNIEnv *Env, jobject Object, jint PersistentUID)
{
   int Result;

   PRINT_DEBUG("%s: Enter (%d)", __FUNCTION__, PersistentUID);

   Result = GATM_UnRegisterPersistentUID(PersistentUID);
   
   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    registerServiceNative
 * Signature: (ZIJJI)I
 */
static jint RegisterServiceNative(JNIEnv *Env, jobject Object, jboolean PrimaryService, jint NumberOfAttributes, jlong MostSignificantBits, jlong LeastSignificantBits, jint ReserveHandleRangeUID)
{
   jint         Result;
   DWord_t      PersistentUID;
   GATT_UUID_t  ServiceUUID;

   PRINT_DEBUG("%s: Enter (%s, %d, %lld, %lld, %d)", __FUNCTION__, PrimaryService == JNI_TRUE ? "TRUE" : "FALSE", NumberOfAttributes, MostSignificantBits, LeastSignificantBits, ReserveHandleRangeUID);

   ServiceUUID = LongsToUUID(MostSignificantBits, LeastSignificantBits);

   switch(ServiceUUID.UUID_Type)
   {
      case guUUID_16:
         PRINT_DEBUG("%s:  16-bit UUID: 0x%02X%02X", __FUNCTION__, ServiceUUID.UUID.UUID_16.UUID_Byte1, ServiceUUID.UUID.UUID_16.UUID_Byte0);
      case guUUID_128:
         PRINT_DEBUG("%s: 128-bit UUID: %02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X", __FUNCTION__, ServiceUUID.UUID.UUID_128.UUID_Byte15, ServiceUUID.UUID.UUID_128.UUID_Byte14, ServiceUUID.UUID.UUID_128.UUID_Byte13, ServiceUUID.UUID.UUID_128.UUID_Byte12, ServiceUUID.UUID.UUID_128.UUID_Byte11, ServiceUUID.UUID.UUID_128.UUID_Byte10, ServiceUUID.UUID.UUID_128.UUID_Byte9, ServiceUUID.UUID.UUID_128.UUID_Byte8, ServiceUUID.UUID.UUID_128.UUID_Byte7, ServiceUUID.UUID.UUID_128.UUID_Byte6, ServiceUUID.UUID.UUID_128.UUID_Byte5, ServiceUUID.UUID.UUID_128.UUID_Byte4, ServiceUUID.UUID.UUID_128.UUID_Byte3, ServiceUUID.UUID.UUID_128.UUID_Byte2, ServiceUUID.UUID.UUID_128.UUID_Byte1, ServiceUUID.UUID.UUID_128.UUID_Byte0);
   }

   if(ReserveHandleRangeUID > 0)
   {
      PersistentUID = (DWord_t)ReserveHandleRangeUID;
      Result = GATM_RegisterService(((PrimaryService == JNI_TRUE) ? TRUE : FALSE), NumberOfAttributes, &ServiceUUID, &PersistentUID);
   }
   else
      Result = GATM_RegisterService(((PrimaryService == JNI_TRUE) ? TRUE : FALSE), NumberOfAttributes, &ServiceUUID, NULL);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}
  
/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    addServiceIncludeNative
 * Signature: (JIII)I
 */
static jint AddServiceIncludeNative(JNIEnv *Env, jobject Object, jlong Flags, jint ServiceID, jint AttributeOffset, jint IncludedServiceID)
{
   jint         Result;

   PRINT_DEBUG("%s: Enter (%lld, %d, %d, %d)", __FUNCTION__, Flags, ServiceID, AttributeOffset, IncludedServiceID);

   Result = GATM_AddServiceInclude(Flags, ServiceID, AttributeOffset, IncludedServiceID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}
  
/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    addServiceCharacteristicNative
 * Signature: (IIJJJJ)I
 */
static jint AddServiceCharacteristicNative(JNIEnv *Env, jobject Object, jint ServiceID, jint AttributeOffset, jlong CharacteristicPropertiesMask, jlong SecurityPropertiesMask, jlong MostSignificantBits, jlong LeastSignificantBits)
{
   jint        Result;
   GATT_UUID_t CharacteristicUUID;

   PRINT_DEBUG("%s: Enter (%d, %d, %lld, %lld, %lld, %lld)", __FUNCTION__, ServiceID, AttributeOffset, CharacteristicPropertiesMask, SecurityPropertiesMask, MostSignificantBits, LeastSignificantBits);

   CharacteristicUUID = LongsToUUID(MostSignificantBits, LeastSignificantBits);

   Result             = GATM_AddServiceCharacteristic(ServiceID, AttributeOffset, CharacteristicPropertiesMask, SecurityPropertiesMask, &CharacteristicUUID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    addServiceDescriptorNative
 * Signature: (IIJJJJ)I
 */
static jint AddServiceDescriptorNative(JNIEnv *Env, jobject Object, jint ServiceID, jint AttributeOffset, jlong DescriptorPropertiesMask, jlong SecurityPropertiesMask, jlong MostSignificantBits, jlong LeastSignificantBits)
{
   jint        Result;
   GATT_UUID_t DescriptorUUID;

   PRINT_DEBUG("%s: Enter (%d, %d, %lld, %lld, %lld, %lld)", __FUNCTION__, ServiceID, AttributeOffset, DescriptorPropertiesMask, SecurityPropertiesMask, MostSignificantBits, LeastSignificantBits);

   DescriptorUUID = LongsToUUID(MostSignificantBits, LeastSignificantBits);

   Result         = GATM_AddServiceDescriptor(ServiceID, AttributeOffset, DescriptorPropertiesMask, SecurityPropertiesMask, &DescriptorUUID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    addServiceAttributeDataNative
 * Signature: (II[B)I
 */
static jint AddServiceAttributeDataNative(JNIEnv *Env, jobject Object, jint ServiceID, jint AttributeOffset, jbyteArray ValueArray)
{
   jint   Result;
   jbyte *Value; 

   PRINT_DEBUG("%s: Enter (%d, %d, %d)", __FUNCTION__, ServiceID, AttributeOffset, ValueArray ? Env->GetArrayLength(ValueArray) : 0);

   if(ValueArray)
   {
      if((Value = Env->GetByteArrayElements(ValueArray, NULL)) != NULL)
      {
         Result =  GATM_AddServiceAttributeData(ServiceID, AttributeOffset, Env->GetArrayLength(ValueArray), (Byte_t *)Value);
        
         Env->ReleaseByteArrayElements(ValueArray, Value, 0);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }
   else
      Result =  GATM_AddServiceAttributeData(ServiceID, AttributeOffset, 0, NULL);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    publishServiceNative
 * Signature: (IJ[I)I
 */
static int PublishServiceNative(JNIEnv *Env, jobject Object, jint ServiceID, jlong ServiceFlags, jintArray HandleRange)
{
   jint                           HandleRangeBuffer[2];
   jint                           Result;
   LocalData_t                   *LocalData;
   GATT_Attribute_Handle_Group_t  ServiceHandleRange;

   PRINT_DEBUG("%s: Enter (%d, %lld)", __FUNCTION__, ServiceID, ServiceFlags);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((Result = GATM_PublishService(ServiceID, LocalData->CallbackID, ServiceFlags, &ServiceHandleRange)) == 0)
      {
         if((HandleRange != NULL) && (Env->GetArrayLength(HandleRange) >= 2))
         {
            HandleRangeBuffer[0] = ServiceHandleRange.Starting_Handle;
            HandleRangeBuffer[1] = ServiceHandleRange.Ending_Handle;
            Env->SetIntArrayRegion(HandleRange, 0, 2, HandleRangeBuffer);
         }
      }
   
      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    deleteServiceNative
 * Signature: (I)I
 */
static int DeleteServiceNative(JNIEnv *Env, jobject Object, jint ServiceID)
{
   jint Result;

   PRINT_DEBUG("%s: Enter (%d)", __FUNCTION__, ServiceID);

   Result = GATM_DeleteService(ServiceID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    queryPublishedServicesNative
 * Signature: ()[Lcom/stonestreetone/bluetopiapm/GATM/ServiceInformation;
 */
static jobjectArray QueryPublishedServicesByUUIDNative(JNIEnv *Env, jobject Object, jlong MostSignificantBits, jlong LeastSignificantBits) 
{
   jint                        Result;
   jobject                     ServiceInformation;
   GATT_UUID_t                 ServiceUUID;
   unsigned int                TotalNumberPublishedServices;
   jobjectArray                PublishedServiceArray = NULL;
   GATM_Service_Information_t *PublishedServicesBuffer;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ServiceUUID = LongsToUUID(MostSignificantBits, LeastSignificantBits);

   if(((Result = GATM_QueryPublishedServices( 0, NULL, &ServiceUUID, &TotalNumberPublishedServices)) == 0) && (TotalNumberPublishedServices > 0))
   {
      if((PublishedServicesBuffer = (GATM_Service_Information_t *)malloc(sizeof(GATM_Service_Information_t) * TotalNumberPublishedServices)) != NULL)
      {
         if((Result = GATM_QueryPublishedServices(TotalNumberPublishedServices, &(PublishedServicesBuffer[0]), &ServiceUUID, NULL)) > 0)
         {
            if(((PublishedServiceArray = Env->NewObjectArray(Result, Class_ServiceInformation, NULL)) != NULL) && (Env->ExceptionCheck() == JNI_FALSE))
            {
               for(int Index = 0; Index < Result; Index++)
               {
                  if(((ServiceInformation = Env->NewObject(Class_ServiceInformation, Constructor_ServiceInformation, (jint)(PublishedServicesBuffer[Index].StartHandle), (jint)(PublishedServicesBuffer[Index].EndHandle), (jint)(PublishedServicesBuffer[Index].ServiceID))) != NULL) && (Env->ExceptionCheck() == JNI_FALSE))
                  {
                     Env->SetObjectArrayElement(PublishedServiceArray, Index, ServiceInformation);
                     if(Env->ExceptionCheck() == JNI_TRUE)
                     {
                        PRINT_DEBUG("%s: Failure at: Env->SetObjectArrayElement", __FUNCTION__);
                        Result = -1;
                        break;
                     }
                  }
                  else
                  {
                     PRINT_DEBUG("%s: Failure at: Env->NewObject", __FUNCTION__);
                     Result = -1;
                     break;
                  }

                  Env->DeleteLocalRef(ServiceInformation);
               }
            }
            else
            {
               PRINT_DEBUG("%s: Failure at: Env->NewObjectArray", __FUNCTION__);
               Result = -1;
            }
         }

         free(PublishedServicesBuffer);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if(Result <= 0)
     PublishedServiceArray = NULL;

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return PublishedServiceArray;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    queryPublishedServicesNative
 * Signature: (JJ)[Lcom/stonestreetone/bluetopiapm/GATM/ServiceInformation;
 */
static jobjectArray QueryPublishedServicesNative(JNIEnv *Env, jobject Object) 
{
   jint                        Result;
   jlong                       MostSignificantBits;
   jlong                       LeastSignificantBits;
   jobject                     ServiceInformation;
   unsigned int                TotalNumberPublishedServices;
   jobjectArray                PublishedServiceArray = NULL;
   GATM_Service_Information_t *PublishedServicesBuffer;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(((Result = GATM_QueryPublishedServices( 0, NULL, NULL, &TotalNumberPublishedServices)) == 0) && (TotalNumberPublishedServices > 0))
   {
      if((PublishedServicesBuffer = (GATM_Service_Information_t *)malloc(sizeof(GATM_Service_Information_t) * TotalNumberPublishedServices)) != NULL)
      {
         if((Result = GATM_QueryPublishedServices(TotalNumberPublishedServices, &(PublishedServicesBuffer[0]), NULL, NULL)) > 0)
         {
            if(((PublishedServiceArray = Env->NewObjectArray(Result, Class_ServiceInformation, NULL)) != NULL) && (Env->ExceptionCheck() == JNI_FALSE))
            {
               for(int Index = 0; Index < Result; Index++)
               {
                  LongsFromUUID(PublishedServicesBuffer[Index].ServiceUUID, &MostSignificantBits, &LeastSignificantBits);

                  if(((ServiceInformation = Env->NewObject(Class_ServiceInformation, Constructor_ServiceInformationUUID, MostSignificantBits, LeastSignificantBits, (jint)(PublishedServicesBuffer[Index].StartHandle), (jint)(PublishedServicesBuffer[Index].EndHandle), (jint)(PublishedServicesBuffer[Index].ServiceID))) != NULL) && (Env->ExceptionCheck() == JNI_FALSE))
                  {
                     Env->SetObjectArrayElement(PublishedServiceArray, Index, ServiceInformation);
                     if(Env->ExceptionCheck() == JNI_TRUE)
                     {
                        PRINT_DEBUG("%s: Failure at: Env->SetObjectArrayElement", __FUNCTION__);
                        Result = -1;
                        break;
                     }
                  }
                  else
                  {
                     PRINT_DEBUG("%s: Failure at: Env->NewObject", __FUNCTION__);
                     Result = -1;
                     break;
                  }

                  Env->DeleteLocalRef(ServiceInformation);
               }
            }
            else
            {
               PRINT_DEBUG("%s: Failure at: Env->NewObjectArray", __FUNCTION__);
               Result = -1;
            }
         }

         free(PublishedServicesBuffer);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }

   if(Result <= 0)
     PublishedServiceArray = NULL;

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return PublishedServiceArray;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    sendHandleValueIndicationNative
 * Signature: (BBBBBBII[B)I
 */
static int SendHandleValueIndicationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ServiceID, jint AttributeOffset, jbyteArray ValueDataArray)
{
   jint         Result;
   jbyte       *ValueData; 
   BD_ADDR_t    RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter (%02x:%02x:%02x:%02x:%02x:%02x, %d, %d, %d)", __FUNCTION__, Address1, Address2, Address3, Address4, Address5, Address6,ServiceID, AttributeOffset, ((ValueDataArray) ? Env->GetArrayLength(ValueDataArray) : 0));

   if(ValueDataArray)
   {
      if((ValueData = Env->GetByteArrayElements(ValueDataArray, NULL)) != NULL)
      {
         ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

         Result = GATM_SendHandleValueIndication(ServiceID, RemoteDeviceAddress, AttributeOffset, Env->GetArrayLength(ValueDataArray), (Byte_t *)ValueData);
   
         Env->ReleaseByteArrayElements(ValueDataArray, ValueData, 0);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }
   else
   {
      ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

      Result = GATM_SendHandleValueIndication(ServiceID, RemoteDeviceAddress, AttributeOffset, 0, NULL);
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;

}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    sendHandleValueNotificationNative
 * Signature: (BBBBBBII[B)I
 */
static int SendHandleValueNotificationNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jint ServiceID, jint AttributeOffset, jbyteArray ValueDataArray)
{
   jint         Result;
   jbyte       *ValueData; 
   BD_ADDR_t    RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter (%02x:%02x:%02x:%02x:%02x:%02x, %d, %d, %d)", __FUNCTION__, Address1, Address2, Address3, Address4, Address5, Address6,ServiceID, AttributeOffset, ((ValueDataArray) ? Env->GetArrayLength(ValueDataArray) : 0));

   if(ValueDataArray)
   {
      if((ValueData = Env->GetByteArrayElements(ValueDataArray, NULL)) != NULL)
      {
         ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

         Result = GATM_SendHandleValueNotification(ServiceID, RemoteDeviceAddress, AttributeOffset, Env->GetArrayLength(ValueDataArray), (Byte_t *)ValueData);
   
         Env->ReleaseByteArrayElements(ValueDataArray, ValueData, 0);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }
   else
   {
      ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

      Result = GATM_SendHandleValueNotification(ServiceID, RemoteDeviceAddress, AttributeOffset, 0, NULL);
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    writeResponseNative
 * Signature: (I)I
 */
static int WriteResponseNative(JNIEnv *Env, jobject Object, jint RequestID)
{
   jint   Result;

   PRINT_DEBUG("%s: Enter (%d)", __FUNCTION__, RequestID);

   Result = GATM_WriteResponse(RequestID);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    readResponseNative
 * Signature: (I[B)I
 */
static int ReadResponseNative(JNIEnv *Env, jobject Object, jint RequestID, jbyteArray DataArray)
{
   jint Result;
   jbyte *Data; 

   PRINT_DEBUG("%s: Enter (%d, %d)", __FUNCTION__, RequestID, ((DataArray) ? Env->GetArrayLength(DataArray) : 0));

   if(DataArray)
   {
      if((Data = Env->GetByteArrayElements(DataArray, NULL)) != NULL)
      {
         Result = GATM_ReadResponse(RequestID, Env->GetArrayLength(DataArray), (Byte_t *)Data);
   
         Env->ReleaseByteArrayElements(DataArray, Data, 0);
      }
      else
         Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
   }
   else
      Result = GATM_ReadResponse(RequestID, 0, NULL);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_GATM
 * Method:    errorResponseNative
 * Signature: (II)I
 */
static int ErrorResponseNative(JNIEnv *Env, jobject Object, jint RequestID, jint ErrorCode)
{
   jint   Result;

   PRINT_DEBUG("%s: Enter (%d, %d)", __FUNCTION__, RequestID, ErrorCode);

   Result = GATM_ErrorResponse(RequestID, ErrorCode);

   PRINT_DEBUG("%s: Exit", __FUNCTION__);

   return Result;
}

static JNINativeMethod Methods[] = {
   {"initClassNative",                   "()V",                                                            (void*) InitClassNative},
   {"initObjectNative",                  "()V",                                                            (void*) InitObjectNative},
   {"cleanupObjectNative",               "()V",                                                            (void*) CleanupObjectNative},
   {"queryConnectedDevicesNative",       "([[I[[B)I",                                                      (void*) QueryConnectedDevicesNative},
   {"queryRemoteDeviceServicesNative",   "(BBBBBB[Ljava/lang/Object;)I",                                   (void*) QueryRemoteDeviceServicesNative},
   {"readValueNative",                   "(BBBBBBIIZ)I",                                                   (void*) ReadValueNative},
   {"writeValueNative",                  "(BBBBBBI[B)I",                                                   (void*) WriteValueNative},
   {"writeValueWithoutResponseNative",   "(BBBBBBI[B)I",                                                   (void*) WriteValueWithoutResponseNative},
   {"registerPersistentUIDNative",       "(I[I)I",                                                         (void*) RegisterPersistentUIDNative},
   {"unRegisterPersistentUIDNative",     "(I)I",                                                           (void*) UnRegisterPersistentUIDNative},
   {"registerServiceNative",             "(ZIJJI)I",                                                       (void*) RegisterServiceNative},
   {"addServiceIncludeNative",           "(JIII)I",                                                        (void*) AddServiceIncludeNative},
   {"addServiceCharacteristicNative",    "(IIJJJJ)I",                                                      (void*) AddServiceCharacteristicNative},
   {"addServiceDescriptorNative",        "(IIJJJJ)I",                                                      (void*) AddServiceDescriptorNative},
   {"addServiceAttributeDataNative",     "(II[B)I",                                                        (void*) AddServiceAttributeDataNative},
   {"publishServiceNative",              "(IJ[I)I",                                                        (void*) PublishServiceNative},
   {"deleteServiceNative",               "(I)I",                                                           (void*) DeleteServiceNative},
   {"queryPublishedServicesNative",      "()[Lcom/stonestreetone/bluetopiapm/GATM$ServiceInformation;",    (void*) QueryPublishedServicesNative},
   {"queryPublishedServicesNative",      "(JJ)[Lcom/stonestreetone/bluetopiapm/GATM$ServiceInformation;",  (void*) QueryPublishedServicesByUUIDNative},
   {"sendHandleValueIndicationNative",   "(BBBBBBII[B)I",                                                  (void*) SendHandleValueIndicationNative},
   {"sendHandleValueNotificationNative", "(BBBBBBII[B)I",                                                  (void*) SendHandleValueNotificationNative},
   {"writeResponseNative",               "(I)I",                                                           (void*) WriteResponseNative},
   {"readResponseNative",                "(I[B)I",                                                         (void*) ReadResponseNative},
   {"errorResponseNative",               "(II)I",                                                          (void*) ErrorResponseNative}
};

int register_com_stonestreetone_bluetopiapm_GATM(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/GATM";

   Result = -1;

   PRINT_DEBUG("Registering GATM native functions");

   if((Class = Env->FindClass(ClassName)) != NULL)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}

void LongsFromUUID(GATT_UUID_t GATT_UUID, jlong *MostSignificantBits, jlong *LeastSignificantBits)
{
   *MostSignificantBits  = 0;
   *LeastSignificantBits = 0;

   switch(GATT_UUID.UUID_Type)
   {
      case guUUID_128:
         *MostSignificantBits  = (GATT_UUID.UUID.UUID_128.UUID_Byte15 & 0x00FF);
      
         *MostSignificantBits  = ((*MostSignificantBits << 8)  | (GATT_UUID.UUID.UUID_128.UUID_Byte14 & 0x00FF));      
         *MostSignificantBits  = ((*MostSignificantBits << 8)  | (GATT_UUID.UUID.UUID_128.UUID_Byte13 & 0x00FF));      
         *MostSignificantBits  = ((*MostSignificantBits << 8)  | (GATT_UUID.UUID.UUID_128.UUID_Byte12 & 0x00FF));      
         *MostSignificantBits  = ((*MostSignificantBits << 8)  | (GATT_UUID.UUID.UUID_128.UUID_Byte11 & 0x00FF));      
         *MostSignificantBits  = ((*MostSignificantBits << 8)  | (GATT_UUID.UUID.UUID_128.UUID_Byte10 & 0x00FF));      
         *MostSignificantBits  = ((*MostSignificantBits << 8)  | (GATT_UUID.UUID.UUID_128.UUID_Byte9  & 0x00FF));      
         *MostSignificantBits  = ((*MostSignificantBits << 8)  | (GATT_UUID.UUID.UUID_128.UUID_Byte8  & 0x00FF));      

         *LeastSignificantBits = (GATT_UUID.UUID.UUID_128.UUID_Byte7  & 0x00FF);      

         *LeastSignificantBits = ((*LeastSignificantBits << 8) | (GATT_UUID.UUID.UUID_128.UUID_Byte6  & 0x00FF));      
         *LeastSignificantBits = ((*LeastSignificantBits << 8) | (GATT_UUID.UUID.UUID_128.UUID_Byte5  & 0x00FF));      
         *LeastSignificantBits = ((*LeastSignificantBits << 8) | (GATT_UUID.UUID.UUID_128.UUID_Byte4  & 0x00FF));      
         *LeastSignificantBits = ((*LeastSignificantBits << 8) | (GATT_UUID.UUID.UUID_128.UUID_Byte3  & 0x00FF));      
         *LeastSignificantBits = ((*LeastSignificantBits << 8) | (GATT_UUID.UUID.UUID_128.UUID_Byte2  & 0x00FF));      
         *LeastSignificantBits = ((*LeastSignificantBits << 8) | (GATT_UUID.UUID.UUID_128.UUID_Byte1  & 0x00FF));      
         *LeastSignificantBits = ((*LeastSignificantBits << 8) | (GATT_UUID.UUID.UUID_128.UUID_Byte0  & 0x00FF));
         break;
      case guUUID_16:
         *LeastSignificantBits = (GATT_UUID.UUID.UUID_16.UUID_Byte1   & 0x00FF);      

         *LeastSignificantBits = ((*LeastSignificantBits << 8)  | (GATT_UUID.UUID.UUID_16.UUID_Byte0  & 0x00FF));
         break;      
   }
}

GATT_UUID_t LongsToUUID(jlong MostSignificantBits, jlong LeastSignificantBits)
{ 
   GATT_UUID_t GATT_UUID;

   if (MostSignificantBits || (LeastSignificantBits >> 16)) 
   {
      GATT_UUID.UUID_Type                 = guUUID_128;
      GATT_UUID.UUID.UUID_128.UUID_Byte0  = ((LeastSignificantBits >>  0) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte1  = ((LeastSignificantBits >>  8) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte2  = ((LeastSignificantBits >> 16) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte3  = ((LeastSignificantBits >> 24) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte4  = ((LeastSignificantBits >> 32) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte5  = ((LeastSignificantBits >> 40) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte6  = ((LeastSignificantBits >> 48) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte7  = ((LeastSignificantBits >> 56) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte8  = ((MostSignificantBits  >>  0) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte9  = ((MostSignificantBits  >>  8) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte10 = ((MostSignificantBits  >> 16) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte11 = ((MostSignificantBits  >> 24) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte12 = ((MostSignificantBits  >> 32) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte13 = ((MostSignificantBits  >> 40) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte14 = ((MostSignificantBits  >> 48) & 0x00FF);
      GATT_UUID.UUID.UUID_128.UUID_Byte15 = ((MostSignificantBits  >> 56) & 0x00FF);
   }
   else
   {
      GATT_UUID.UUID_Type                 = guUUID_16;
      GATT_UUID.UUID.UUID_16.UUID_Byte0   = ((LeastSignificantBits >>  0) & 0x00FF);
      GATT_UUID.UUID.UUID_16.UUID_Byte1   = ((LeastSignificantBits >>  8) & 0x00FF);
   }

   return GATT_UUID;
}
