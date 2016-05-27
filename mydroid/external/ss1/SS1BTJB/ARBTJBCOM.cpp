/*****< ARBTJBCOM.cpp >********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBCOM - Common routines for the Stonestreet One Android Runtime       */
/*              Bluetooth JNI Bridge                                          */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/18/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "ARBTJBCOM"

#include "android_runtime/AndroidRuntime.h"
#include "cutils/properties.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

#ifdef HAVE_BLUETOOTH
extern "C"
{
#include "SS1BTPM.h"
}
#endif /* HAVE_BLUETOOTH */

#include "ARBTJBCOM.h"

namespace android
{

#ifdef HAVE_BLUETOOTH

   /* The following enumerated type represents all categories of        */
   /* Bluetooth Profiles that this module knows about.                  */
typedef enum
{
   bpSerialPort,
   bpHeadsetAudioGateway,
   bpHeadset,
   bpDialUpNetworking,
   bpFAX,
   bpLANAccess,
   bpOBEXObjectPush,
   bpOBEXFileTransfer,
   bpOBEXSynchronization,
   bpHandsFreeAudioGateway,
   bpHandsFree,
   bpSIMAccess,
   bpHID,
   bpHardcopyCableReplacement,
   bpSyncML,
   bpAudioSink,
   bpAudioSource,
   bpAdvAudioDistribution,
   bpAVRCP,
   bpAVRCPTarget,
   bpAVRCPController,
   bpPANUser,
   bpPANNetworkAccessPoint,
   bpPANGroupAdhocNetwork,
   bpUnknown
} BluetoothProfileType_t;

typedef struct _tagKnownProfileUUIDs
{
   static const UUID_128_t AVRCPController;
   static const UUID_128_t AVRCPTarget;
   static const UUID_128_t AdvAudioDistribution;
   static const UUID_128_t AudioSink;
   static const UUID_128_t AudioSource;
   static const UUID_128_t DialUpNetworking;
   static const UUID_128_t FAX;
   static const UUID_128_t HID;
   static const UUID_128_t HandsFree;
   static const UUID_128_t HandsFreeAudioGateway;
   static const UUID_128_t HardcopyCableReplacement;
   static const UUID_128_t Headset;
   static const UUID_128_t HeadsetAudioGateway;
   static const UUID_128_t OBEXFileTransfer;
   static const UUID_128_t OBEXObjectPush;
   static const UUID_128_t OBEXSynchronization;
   static const UUID_128_t PANGroupAdhocNetwork;
   static const UUID_128_t PANNetworkAccessPoint;
   static const UUID_128_t PANUser;
   static const UUID_128_t SIMAccess;
   static const UUID_128_t SerialPort;
   static const UUID_128_t SyncML;
} KnownProfileUUIDs;

   /* Note that these values are assigned in SDP_UUID format (Big       */
   /* Endian).                                                          */
const UUID_128_t KnownProfileUUIDs::AVRCPController          = {0x00, 0x00, 0x11, 0x0E, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::AVRCPTarget              = {0x00, 0x00, 0x11, 0x0C, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::AdvAudioDistribution     = {0x00, 0x00, 0x11, 0x0D, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::AudioSink                = {0x00, 0x00, 0x11, 0x0B, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::AudioSource              = {0x00, 0x00, 0x11, 0x0A, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::DialUpNetworking         = {0x00, 0x00, 0x11, 0x03, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::FAX                      = {0x00, 0x00, 0x11, 0x11, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::HID                      = {0x00, 0x00, 0x11, 0x24, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::HandsFree                = {0x00, 0x00, 0x11, 0x1E, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::HandsFreeAudioGateway    = {0x00, 0x00, 0x11, 0x1F, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::HardcopyCableReplacement = {0x00, 0x00, 0x00, 0x16, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::Headset                  = {0x00, 0x00, 0x11, 0x08, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::HeadsetAudioGateway      = {0x00, 0x00, 0x11, 0x12, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::OBEXFileTransfer         = {0x00, 0x00, 0x11, 0x06, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::OBEXObjectPush           = {0x00, 0x00, 0x11, 0x05, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::OBEXSynchronization      = {0x00, 0x00, 0x11, 0x04, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::PANGroupAdhocNetwork     = {0x00, 0x00, 0x11, 0x17, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::PANNetworkAccessPoint    = {0x00, 0x00, 0x11, 0x16, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::PANUser                  = {0x00, 0x00, 0x11, 0x15, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::SIMAccess                = {0x00, 0x00, 0x11, 0x2D, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::SerialPort               = {0x00, 0x00, 0x11, 0x01, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
const UUID_128_t KnownProfileUUIDs::SyncML                   = {0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x02, 0xEE, 0x00, 0x00, 0x02};

static const UUID_128_t NullUUID_128 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

//typedef struct _tagIPCLostCallbackParameter_t
//{
//   BTPM_Server_UnRegistration_Callback_t Callback;
//   void                                 *Parameter;
//} IPCLostCallbackParameter_t;
//
//static bool                       BTPMServiceConnected      = false;
//static pthread_mutex_t            BTPMServiceConnectedMutex = PTHREAD_MUTEX_INITIALIZER;
//static IPCLostCallbackParameter_t IPCLostCallbackParameter  = {NULL, NULL};
//
//static void IPCLostCallback(void *CallbackParameter)
//{
//   IPCLostCallbackParameter_t *RealCallback;
//
//   if(pthread_mutex_lock(&BTPMServiceConnectedMutex) == 0)
//   {
//      BTPMServiceConnected = false;
//      pthread_mutex_unlock(&BTPMServiceConnectedMutex);
//   }
//   else
//   {
//      /* Having the mutex is nice, but set the flag anyway even when the*/
//      /* mutex throws an error. If the mutex is working, we'll never hit*/
//      /* this, so it's only an emergency fallback.                      */
//      BTPMServiceConnected = false;
//   }
//
//   /* If we have a real callback specified, and it appears valid, call  */
//   /* it now.                                                           */
//   if(CallbackParameter)
//   {
//      RealCallback = (IPCLostCallbackParameter_t *)CallbackParameter;
//
//      if(RealCallback->Callback)
//      {
//         (*(RealCallback->Callback))(RealCallback->Parameter);
//      }
//   }
//}

   /* The following function is responsible for determining the         */
   /* Bluetooth Profile that is represented by the specified Bluetooth  */
   /* 128 Bit UUID value. If the Bluetooth Profile is unable to be      */
   /* determined based upon the specified UUID then this function will  */
   /* return bpUnknown, else this function will return the correct      */
   /* Bluetooth Profile.                                                */
static BluetoothProfileType_t FetchBluetoothProfileType(UUID_128_t ProfileUUID)
{
   BluetoothProfileType_t ret_val = bpUnknown;

   /* Simply compare the passed in UUID to all the UUID's that we know  */
   /* about. * NOTE * We could put this into a Table, but then we       */
   /* couldn't use MACRO's to get the UUID values for the profiles that */
   /* we know about.                                                    */

   if(COMPARE_UUID_128(KnownProfileUUIDs::AVRCPController, ProfileUUID))
      ret_val = bpAVRCPController;
   else
   {
      if(COMPARE_UUID_128(KnownProfileUUIDs::AVRCPTarget, ProfileUUID))
         ret_val = bpAVRCPTarget;
      else
      {
         if(COMPARE_UUID_128(KnownProfileUUIDs::AdvAudioDistribution, ProfileUUID))
            ret_val = bpAdvAudioDistribution;
         else
         {
            if(COMPARE_UUID_128(KnownProfileUUIDs::AudioSink, ProfileUUID))
               ret_val = bpAudioSink;
            else
            {
               if(COMPARE_UUID_128(KnownProfileUUIDs::AudioSource, ProfileUUID))
                  ret_val = bpAudioSource;
               else
               {
                  if(COMPARE_UUID_128(KnownProfileUUIDs::HandsFreeAudioGateway, ProfileUUID))
                     ret_val = bpHandsFreeAudioGateway;
                  else
                  {
                     if(COMPARE_UUID_128(KnownProfileUUIDs::HandsFree, ProfileUUID))
                        ret_val = bpHandsFree;
                     else
                     {
                        if(COMPARE_UUID_128(KnownProfileUUIDs::Headset, ProfileUUID))
                           ret_val = bpHeadset;
                        else
                        {
                           if(COMPARE_UUID_128(KnownProfileUUIDs::HeadsetAudioGateway, ProfileUUID))
                              ret_val = bpHeadsetAudioGateway;
                           else
                           {
                              if(COMPARE_UUID_128(KnownProfileUUIDs::OBEXObjectPush, ProfileUUID))
                                 ret_val = bpOBEXObjectPush;
                              else
                              {
                                 if(COMPARE_UUID_128(KnownProfileUUIDs::OBEXFileTransfer, ProfileUUID))
                                    ret_val = bpOBEXFileTransfer;
                                 else
                                 {
                                    if(COMPARE_UUID_128(KnownProfileUUIDs::HID, ProfileUUID))
                                       ret_val = bpHID;
                                    else
                                    {
                                       if(COMPARE_UUID_128(KnownProfileUUIDs::PANUser, ProfileUUID))
                                          ret_val = bpPANUser;
                                       else
                                       {
                                          if(COMPARE_UUID_128(KnownProfileUUIDs::PANNetworkAccessPoint, ProfileUUID))
                                             ret_val = bpPANNetworkAccessPoint;
                                          else
                                          {
                                             if(COMPARE_UUID_128(KnownProfileUUIDs::PANGroupAdhocNetwork, ProfileUUID))
                                                ret_val = bpPANGroupAdhocNetwork;
                                             else
                                             {
                                                if(COMPARE_UUID_128(KnownProfileUUIDs::DialUpNetworking, ProfileUUID))
                                                   ret_val = bpDialUpNetworking;
                                                else
                                                {
                                                   if(COMPARE_UUID_128(KnownProfileUUIDs::SerialPort, ProfileUUID))
                                                      ret_val = bpSerialPort;
                                                   else
                                                   {
                                                      if(COMPARE_UUID_128(KnownProfileUUIDs::SIMAccess, ProfileUUID))
                                                         ret_val = bpSIMAccess;
                                                      else
                                                      {
                                                         if(COMPARE_UUID_128(KnownProfileUUIDs::FAX, ProfileUUID))
                                                            ret_val = bpFAX;
                                                         else
                                                         {
                                                            if(COMPARE_UUID_128(KnownProfileUUIDs::OBEXSynchronization, ProfileUUID))
                                                               ret_val = bpOBEXSynchronization;
                                                            else
                                                            {
                                                               if(COMPARE_UUID_128(KnownProfileUUIDs::HardcopyCableReplacement, ProfileUUID))
                                                                  ret_val = bpHardcopyCableReplacement;
                                                               else
                                                               {
                                                                  if(COMPARE_UUID_128(KnownProfileUUIDs::SyncML, ProfileUUID))
                                                                     ret_val = bpSyncML;
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   /* Finally return the result to the caller.                          */
   return(ret_val);
}

   /* This function is used to extract a UUID representing the service  */
   /* advertised in the given SDP record. On error, the function        */
   /* returns a UUID of all zeros.                                      */
static UUID_128_t GetServiceUUIDFromRecord(const SDP_Service_Attribute_Response_Data_t &SDPRecord)
{
   bool                   Error;
   bool                   Found;
   bool                   SeenServiceClass;
   Word_t                 Attr;
   UUID_128_t             UUID;
   UUID_128_t             PrimaryServiceClassUUID;
   unsigned int           Element;
   unsigned int           NumberElements;
   SDP_Data_Element_t    *AttrData;
   SDP_Data_Element_t    *Sequence;
   SDP_Data_Element_t    *SeqElement;
   BluetoothProfileType_t ProfileType;

   Error = false;
   Found = false;
   ProfileType = bpUnknown;

   memset(&PrimaryServiceClassUUID, 0, sizeof(PrimaryServiceClassUUID));

   for(Attr = 0; ((Attr < SDPRecord.Number_Attribute_Values) && (!Error) && (!Found)); Attr++)
   {
      if((AttrData = SDPRecord.SDP_Service_Attribute_Value_Data[Attr].SDP_Data_Element) != NULL)
      {
         switch(SDPRecord.SDP_Service_Attribute_Value_Data[Attr].Attribute_ID)
         {
            case SDP_ATTRIBUTE_ID_SERVICE_CLASS_ID_LIST:
               /* Next, let's make sure that a Data Element Sequence    */
               /* exists.                                               */
               if((AttrData->SDP_Data_Element_Type == deSequence) && (AttrData->SDP_Data_Element_Length))
               {
                  for(Element = 0; ((Element < AttrData->SDP_Data_Element_Length) && (!Error) && (!Found)); Element++)
                  {
                     switch(AttrData->SDP_Data_Element.SDP_Data_Element_Sequence[Element].SDP_Data_Element_Type)
                     {
                        case deUUID_16:
                           SDP_ASSIGN_BASE_UUID(UUID);
                           ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(UUID, AttrData->SDP_Data_Element.SDP_Data_Element_Sequence[Element].SDP_Data_Element.UUID_16);
                           break;
                        case deUUID_32:
                           SDP_ASSIGN_BASE_UUID(UUID);
                           ASSIGN_SDP_UUID_32_TO_SDP_UUID_128(UUID, AttrData->SDP_Data_Element.SDP_Data_Element_Sequence[Element].SDP_Data_Element.UUID_32);
                           break;
                        case deUUID_128:
                           UUID = AttrData->SDP_Data_Element.SDP_Data_Element_Sequence[Element].SDP_Data_Element.UUID_128;
                           break;
                        default:
                           SS1_LOGE("Invalid SDP record (non UUID value in Attribute %u, Index %u)", Attr, Element);
                           SDP_ASSIGN_BASE_UUID(UUID);
                           break;
                     }

                     /* Save the first Service Class for use as a       */
                     /* fallback in case we don't recognize the service.*/
                     if(Element == 0)
                        PrimaryServiceClassUUID = UUID;

                     /* Check to see if there is a match to one of the  */
                     /* Protocols that we know.                         */
                     if((ProfileType = FetchBluetoothProfileType(UUID)) != bpUnknown)
                     {
                        /*DEBUG*/
                        {
                           char uuid1[64];
                           UUID128ToStr(uuid1, sizeof(uuid1), UUID);
                           SS1_LOGI("Testing Class UUID %s == success (%u)", uuid1, ProfileType);
                        }

                        /* Note that we have determined the Profile     */
                        /* type. This will break us out of both loops   */
                        /* and return.                                  */
                        Found = true;
                     }
                     else
                     {
                        /*DEBUG*/
                        char uuid1[64];
                        UUID128ToStr(uuid1, sizeof(uuid1), UUID);
                        SS1_LOGI("Testing Class UUID %s == unknown", uuid1);
                     }
                  }
               }
               SeenServiceClass = true;
               break;

            case SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST:
               break;

            case SDP_ATTRIBUTE_ID_ADDITIONAL_PROTOCOL_DESCRIPTOR_LISTS:
               break;

            case SDP_ATTRIBUTE_ID_BLUETOOTH_PROFILE_DESCRIPTOR_LIST:
               /* Only process this attribute if we have not yet        */
               /* identified the service.                               */
               if(!Found)
               {
                  /* Any properly formed SDP Record should include the  */
                  /* ServiceClass attribute before this one. Make sure  */
                  /* we've seen the Service Class before processing     */
                  /* the Profile List because some profiles share a     */
                  /* common Profile UUID but distinguish themselves by  */
                  /* ServiceClass UUID.                                 */
                  if(SeenServiceClass)
                  {
                     /* Next, let's make sure that a Data Element       */
                     /* Sequence exists.                                */
                     if((AttrData->SDP_Data_Element_Type == deSequence) && (AttrData->SDP_Data_Element_Length > 0))
                     {
                        /* Data Element Sequence Exists, now let's loop */
                        /* through the Data Element Sequence.           */
                        for(Element = 0; ((Element < AttrData->SDP_Data_Element_Length) && (!Error) && (!Found)); Element++)
                        {
                           if(((SeqElement = &(AttrData->SDP_Data_Element.SDP_Data_Element_Sequence[Element])) != NULL) && (SeqElement->SDP_Data_Element_Type == deSequence) && (SeqElement->SDP_Data_Element_Length > 0) && ((Sequence = SeqElement->SDP_Data_Element.SDP_Data_Element_Sequence) != NULL))
                           {
                              /* Normalize the Profile UUID to 128      */
                              /* Bits.                                  */
                              switch(Sequence[0].SDP_Data_Element_Type)
                              {
                                 case deUUID_16:
                                    SDP_ASSIGN_BASE_UUID(UUID);
                                    ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(UUID, Sequence[0].SDP_Data_Element.UUID_16);
                                    break;
                                 case deUUID_32:
                                    SDP_ASSIGN_BASE_UUID(UUID);
                                    ASSIGN_SDP_UUID_32_TO_SDP_UUID_128(UUID, Sequence[0].SDP_Data_Element.UUID_32);
                                    break;
                                 case deUUID_128:
                                    UUID = Sequence[0].SDP_Data_Element.UUID_128;
                                    break;
                                 default:
                                    SS1_LOGE("Invalid SDP record (non UUID value in Attribute %u, Index %u, Index 0)", Attr, Element);
                                    SDP_ASSIGN_BASE_UUID(UUID);
                                    break;
                              }

                              /* Now let's check to see if there is a   */
                              /* match to one of the Protocols that     */
                              /* we know. * NOTE * If we have already   */
                              /* determined the Profile Type we will    */
                              /* not attempt to determine it again. We  */
                              /* do this because some Profiles tell     */
                              /* their type in the Service Class and    */
                              /* differentiate themselves that way (for */
                              /* example the Hands Free Profile, has    */
                              /* the same Profile for Audio Gateway and */
                              /* Hands Free, but the Service Classes    */
                              /* are different).                        */
                              if((ProfileType = FetchBluetoothProfileType(UUID)) != bpUnknown)
                              {
                                 {
                                    char uuid1[64];
                                    UUID128ToStr(uuid1, sizeof(uuid1), UUID);
                                    SS1_LOGI("Testing Profile UUID %s == success (%u)", uuid1, ProfileType);
                                 }
                                 Found = true;
                              }
                              else
                              {
                                 char uuid1[64];
                                 UUID128ToStr(uuid1, sizeof(uuid1), UUID);
                                 SS1_LOGI("Testing Profile UUID %s == unknown", uuid1);
                              }
                           }
                           else
                           {
                              SS1_LOGW("Invalid SDP record for Profile List, Index %u (not sequence, zero-length, or invalid contents", Element);
                              Error = true;
                           }
                        }
                     }
                     else
                     {
                        SS1_LOGW("Invalid SDP record for Profile List attribute (not sequence or zero-length)");
                        Error = true;
                     }
                  }
                  else
                  {
                     SS1_LOGE("Encountered Profile List attribute even though we haven't seen the ServiceClass attribute, yet.");
                     Error = true;
                  }
               }
               break;

            default:
               break;
         }
      }
   }

   if(Error)
      memset(&UUID, 0, sizeof(UUID));
   else
   {
      if((!Found) && (!COMPARE_UUID_128(PrimaryServiceClassUUID, NullUUID_128)))
         UUID = PrimaryServiceClassUUID;
   }

   return(UUID);
}

   /* This function is provided as a convenience for obtaining the raw  */
   /* Service record bytes for a device. Negative values are returned   */
   /* on error. A return value of zero indicates there were no errors,  */
   /* but no data available. Positive values indiicate the length       */
   /* of the data now pointed to by *ServiceData (second paramter).     */
   /* ServiceData will only be modified if data is available. The caller*/
   /* is responsible for releasing the buffer pointed to by ServiceData */
   /* (via free()).                                                     */
static int GetRawServiceData(BD_ADDR_t BD_ADDR, unsigned char **ServiceData, bool LowEnergy)
{
   int            ret_val;
   int            RawServiceDataLength;
   unsigned int   TotalServiceBytes;
   unsigned char *RawServiceData;
   unsigned char *TmpPtr;

   ret_val              = 0;
   RawServiceDataLength = 0;

   if(ServiceData)
   {
      if((*(void **)(&RawServiceData) = malloc(512)) != NULL)
      {
         if((RawServiceDataLength = DEVM_QueryRemoteDeviceServices(BD_ADDR, (LowEnergy ? DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY : 0), 512, RawServiceData, &TotalServiceBytes)) >= 0)
         {
            /* The query was successful. If we received some data back  */
            /* from the query but didn't receive all the available      */
            /* data, allocate a sufficiently large buffer and try       */
            /* again.                                                   */
            if((RawServiceDataLength > 0) && (((unsigned int)RawServiceDataLength) < TotalServiceBytes))
            {
               if((*(void **)(&TmpPtr) = realloc(RawServiceData, TotalServiceBytes)) != NULL)
               {
                  RawServiceData = TmpPtr;
                  if((RawServiceDataLength = DEVM_QueryRemoteDeviceServices(BD_ADDR, (LowEnergy ? DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY : 0), TotalServiceBytes, RawServiceData, NULL)) >= 0)
                  {
                     /* Unsigned conversion is safe because the         */
                     /* previous test asserted a positive value.        */
                     if(((unsigned int)RawServiceDataLength)!= TotalServiceBytes)
                     {
                        /* The second attempt failed (the first call    */
                        /* promised TotalServiceBytes-worth of data but     */
                        /* this time we didn't get that amount, so give */
                        /* up. Clean up happens later.                  */
                        ret_val = -1;
                     }
                  }
               }
               else
                  ret_val = -2;
            }
         }
         else
            ret_val = -3;
      }
      else
         ret_val = -4;

      if((ret_val == 0) && (RawServiceDataLength > 0))
      {
         ret_val = RawServiceDataLength;
         if(ret_val > 0)
            *ServiceData = RawServiceData;
      }
      else
      {
         if(RawServiceData)
            free(RawServiceData);
      }
   }
   else
      ret_val = -5;

   return(ret_val);
}

   /* This function is provided to obtain all available UUIDs           */
   /* advertised by a device. If successful, UUIDList (the seond        */
   /* parameter) will point to a buffer of 128-bit UUIDs and the return */
   /* value will give the number off UUIDs in the buffer. If no errors  */
   /* occurred but no UUIDs are advertised, the function returns zero   */
   /* and does not modify UUIDList. A negative value is returned on     */
   /* error and UUIDList is not modified. The caller is responsible for */
   /* releasing the buffer pointed to by UUIDList.                      */
static int GetRemoteDeviceServiceUUIDs(BD_ADDR_t BD_ADDR, UUID_128_t **UUIDList)
{
   int                    ret_val;
   int                    RawServiceDataLength;
   UUID_128_t            *UUIDBuffer;
   unsigned int           i;
   unsigned int           NumberServiceRecords;
   unsigned char         *RawServiceData;
   DEVM_Parsed_SDP_Data_t ParsedSDPData;

   ret_val              = 0;
   NumberServiceRecords = 0;

   /* Retreive the raw SDP record dump from the remote device.          */
   if((RawServiceDataLength = GetRawServiceData(BD_ADDR, &RawServiceData, false)) >= 0)
   {
      /* Parse the raw records into a usable format.                    */
      if(DEVM_ConvertRawSDPStreamToParsedSDPData(RawServiceDataLength, RawServiceData, &ParsedSDPData) == 0)
      {
         if((NumberServiceRecords = ParsedSDPData.NumberServiceRecords) > 0)
         {
            /* Allocate a buffer off the heap to hold the Service IDs   */
            /* (one ID per record).                                     */
            if((UUIDBuffer = (UUID_128_t *)malloc(NumberServiceRecords * sizeof(UUID_128_t))) != NULL)
            {
               for(i = 0; i < NumberServiceRecords; i++)
               {
                  UUIDBuffer[i] = GetServiceUUIDFromRecord(ParsedSDPData.SDPServiceAttributeResponseData[i]);
                  char uuid[64];
                  UUID128ToStr(uuid, sizeof(uuid), UUIDBuffer[i]);
               }

               *UUIDList = UUIDBuffer;
            }
         }

         DEVM_FreeParsedSDPData(&ParsedSDPData);
      }
      else
         ret_val = -20;

      free(RawServiceData);
   }
   else
   {
      /* GetRawServiceData() failed, so pick up its return value.           */
      ret_val = RawServiceDataLength;
   }

   /* If no errors have occurred, return the number of records we       */
   /* obtained. Otherwise, return the pending error code.               */
   return((ret_val == 0) ? NumberServiceRecords : ret_val);
}

   /* The following function is used to search an SDP Protocol          */
   /* Descriptor List for an RFCOMM channel number. The function        */
   /* returns either the channel number (1..31, inclusive), zero to     */
   /* indicate that no RFCOMM service is mentioned in the Protocol      */
   /* Descriptor List, or a negative value to indicate an error.        */
static int FindRfcommChannelInProtoList(SDP_Data_Element_t *ProtocolDescriptorList)
{
   int                 ret_val;
   DWord_t             i;
   SDP_Data_Element_t *PDEntry;
   UUID_128_t          RfcommUUID;
   UUID_128_t          TestUUID;

   SDP_ASSIGN_RFCOMM_UUID_128(RfcommUUID);

   ret_val = 0;

   if(ProtocolDescriptorList)
   {
      if(ProtocolDescriptorList->SDP_Data_Element_Type == deSequence)
      {
         for(i = 0; ((i < ProtocolDescriptorList->SDP_Data_Element_Length) && (ret_val == 0)); i++)
         {
            if((PDEntry = &(ProtocolDescriptorList->SDP_Data_Element.SDP_Data_Element_Sequence[i])) != NULL)
            {
               if((PDEntry->SDP_Data_Element_Type == deSequence) && (PDEntry->SDP_Data_Element_Length > 0))
               {
                  /* The first element of a PD is a UUID identifying    */
                  /* the protocol being described. Determine the format */
                  /* of the UUID and normalize on 128 bits.             */
                  SDP_ASSIGN_BASE_UUID(TestUUID);
                  switch(PDEntry->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element_Type)
                  {
                     case deUUID_128:
                        TestUUID = PDEntry->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_128;
                        break;
                     case deUUID_32:
                        ASSIGN_SDP_UUID_32_TO_SDP_UUID_128(TestUUID, PDEntry->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_32);
                        break;
                     case deUUID_16:
                        ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(TestUUID, PDEntry->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_32);
                        break;
                     default:
                        /* We found something other than a UUID. This   */
                        /* suggests that the PD is not well formed, so  */
                        /* abort the search.                            */
                        ret_val = -1;
                        break;
                  }

                  /* Check that what we found in the first field was    */
                  /* valid and whether it matches the standard RFCOMM   */
                  /* identifier.                                        */
                  if((ret_val == 0) && COMPARE_UUID_128(RfcommUUID, TestUUID))
                  {
                     /* We found an RFCOMM protocol descriptor, so      */
                     /* we're done searching. First, assert that the    */
                     /* descriptor conforms to the RFCOMM spec; that    */
                     /* is, the second field of the PD must be an 8-bit */
                     /* unsigned int and its value must represent a     */
                     /* valid RFCOMM channel number. If that's the      */
                     /* case, then we've found our channel. Otherwise,  */
                     /* the format of the PD is erroneous, so we return */
                     /* an error code.                                  */
                     if(PDEntry->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element_Type == deUnsignedInteger1Byte)
                     {
                        if(RFCOMM_VALID_SERVER_CHANNEL_ID(PDEntry->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element.UnsignedInteger1Byte))
                           ret_val = PDEntry->SDP_Data_Element.SDP_Data_Element_Sequence[1].SDP_Data_Element.UnsignedInteger1Byte;
                        else
                           ret_val = -2;
                     }
                     else
                        ret_val = -3;
                  }
               }
               else
               {
                  /* The protocol descriptor list may be invalid. Every */
                  /* member of the list should be another Sequence. We  */
                  /* have found a member that claims to be of another   */
                  /* type, so give up.                                  */
                  ret_val = -4;
               }
            }
            else
            {
               /* We encountered a list element which was not           */
               /* initialized correctly. Abort the search.              */
               ret_val = -5;
            }
         }
      }
      else
         ret_val = -6;
   }
   else
      ret_val = -7;

   return(ret_val);
}

   /* The following function is used to obtain a list of remote device  */
   /* addresses which are currently either paired or connected (or      */
   /* both) to the local device. The function returns the number of     */
   /* devices found and sets the parameter (AddrList) to a buffer       */
   /* containing the device addresses. If zero devices are found,       */
   /* AddrList will not be modified.                                    */
static unsigned int GetKnownDeviceList(BD_ADDR_t **AddrList)
{
   int               Result;
   BD_ADDR_t        *TmpPtr;
   BD_ADDR_t        *FullList;
   BD_ADDR_t        *PairedList;
   BD_ADDR_t        *ConnectedList;
   unsigned int      c;
   unsigned int      p;
   unsigned int      TotalPaired;
   unsigned int      TotalConnected;
   unsigned int      TotalDevices;
   Class_of_Device_t ClassOfDeviceFilter;

   ASSIGN_CLASS_OF_DEVICE(ClassOfDeviceFilter, 0, 0, 0);

   TotalDevices   = 0;
   TotalPaired    = 0;
   TotalConnected = 0;

   /* Figure out the total number of paired and connected devices and   */
   /* use these values to estimate the actual number of devices.        */
   SS1_LOGD("QueryRemoteDeviceList 1");
   if(DEVM_QueryRemoteDeviceList(DEVM_QUERY_REMOTE_DEVICE_LIST_CURRENTLY_PAIRED, ClassOfDeviceFilter, 0, NULL, &TotalPaired) >= 0)
      TotalDevices += TotalPaired;

   SS1_LOGD("QueryRemoteDeviceList 2");
   if(DEVM_QueryRemoteDeviceList(DEVM_QUERY_REMOTE_DEVICE_LIST_CURRENTLY_CONNECTED, ClassOfDeviceFilter, 0, NULL, &TotalConnected) >= 0)
      TotalDevices += TotalConnected;

   /* Allocate a buffer to hold the list of remote devices. This will   */
   /* be used for both paired and connected devices.                    */
   if((FullList = (BD_ADDR_t *)malloc(TotalDevices * sizeof(BD_ADDR_t))) != NULL)
   {
      /* Pull the list of paired devices and store it at the beginning  */
      /* of the buffer. Update TotalPaired to contain the number of     */
      /* devices actually stored.                                       */
      PairedList = FullList;
      if(TotalPaired)
      {
         SS1_LOGD("QueryRemoteDeviceList 3 (%d)", TotalPaired);
         Result = DEVM_QueryRemoteDeviceList(DEVM_QUERY_REMOTE_DEVICE_LIST_CURRENTLY_PAIRED, ClassOfDeviceFilter, TotalPaired, PairedList, NULL);
      }
      else
         Result = 0;
      TotalPaired = ((Result > 0) ? (unsigned)Result : 0);

      /* Start the Connected device list within the allocated        */
      /* buffer, immediately after the paired devices.               */
      ConnectedList = PairedList + TotalPaired;
      if(TotalConnected)
      {
         SS1_LOGD("QueryRemoteDeviceList 4 (%d)", TotalConnected);
         Result = DEVM_QueryRemoteDeviceList(DEVM_QUERY_REMOTE_DEVICE_LIST_CURRENTLY_CONNECTED, ClassOfDeviceFilter, TotalConnected, ConnectedList, NULL);
      }
      else
         Result = 0;
      TotalConnected = ((Result > 0) ? (unsigned)Result : 0);

      SS1_LOGD("Total retrieved devices: %d", TotalDevices);

      /* Eliminate duplicate devices. We can assume each list contains  */
      /* only unique entries within itself, so we only need to compare  */
      /* the lists to each other.                                       */
      for(c = 0; c < TotalConnected; c++)
      {
         for(p = 0; p < TotalPaired; p++)
         {
            if(COMPARE_BD_ADDR(ConnectedList[c], PairedList[p]))
            {
               TotalConnected -= 1;
               if(TotalConnected > c)
                  ConnectedList[c] = ConnectedList[TotalConnected];

               /* We moved the last element over the current one, so    */
               /* adjust the Connected counter so that on the next      */
               /* iteration we're checking the same location (so we     */
               /* don't skip the value we just copied).                 */
               c -= 1;

               /* Jump out of the inner loop to move to the next        */
               /* Connected device to search.                           */
               break;
            }
         }
      }

      /* Update TotalDevices to reflect any duplicate removals and      */
      /* shrink the allocated buffer accordingly.                       */
      if((TotalPaired + TotalConnected) < TotalDevices)
      {
         TotalDevices = TotalPaired + TotalConnected;
//         if((TmpPtr = (BD_ADDR_t *)realloc(FullList, TotalDevices * sizeof(BD_ADDR_t))) != NULL)
//            FullList = TmpPtr;
      }

      SS1_LOGD("Total unique devices: %d (%d, %d)", TotalDevices, TotalPaired, TotalConnected);
   }

   /* If we found any devices, update the out-bound paramter, AddrList, */
   /* to point to the buffer. Otherwise, free the buffer memory.        */
   if((TotalDevices > 0) && (FullList))
   {
      SS1_LOGD("Success.");
      *AddrList = FullList;
   }
   else
   {
      SS1_LOGD("Error (%d, %p)", TotalDevices, FullList);
      TotalDevices = 0;

      if(FullList)
         free(FullList);
   }

   return((unsigned)TotalDevices);
}

   /* This function is used to set a particular element of a Java       */
   /* String[] to the value of the buffer UTF8String (fourth            */
   /* parameter).                                                       */
void SetStringArrayElement(JNIEnv *Env, jobjectArray StringArray, unsigned int Index, char *UTF8String)
{
   int    i;
   int    CopyBufferLength;
   char  *CopyBuffer;
   jstring StringObject;

   if(Env && StringArray && UTF8String)
   {
      /* Protect against invalid UTF8. This will replace any invalid    */
      /* bytes with a printable ASCII character.                        */
      SanitizeUTF8(UTF8String, strlen(UTF8String));

      StringObject = Env->NewStringUTF(UTF8String);

      if(StringObject)
      {
         SS1_LOGI("Adding @ index %u, value is \"%s\".", Index, UTF8String);
         Env->SetObjectArrayElement(StringArray, Index, StringObject);
         Env->DeleteLocalRef(StringObject);
      }
      else
      {
         SS1_LOGE("Trying to set an invalid string value @ index %u", Index);
         Env->SetObjectArrayElement(StringArray, Index, NULL);
      }
   }
   else
      SS1_LOGE("called with invalid parameters");
}

void SetStringArrayElement(JNIEnv *Env, jobjectArray StringArray, unsigned int Index, const char *UTF8String)
{
   if(UTF8String)
   {
      unsigned int Length = strlen(UTF8String);
      char Buffer[Length + 1];
      strncpy(Buffer, UTF8String, sizeof(Buffer));

      SetStringArrayElement(Env, StringArray, Index, Buffer);
   }
}

jobjectArray BuildLocalPropertyStringArray(JNIEnv *Env, unsigned long IncludeFields, DEVM_Local_Device_Properties_t *LocalDevProps)
{
   char         PropBuffer[256];
   BD_ADDR_t   *DeviceList;
   unsigned int DeviceListLength;
   unsigned int i;
   unsigned int PropIndex;
   unsigned int ClassOfDevice;
   unsigned int PropStringArrayLength;
   jobjectArray PropStringArray;

   /* The list is a 1-dimensional array of Java strings formatted, for  */
   /* the most part, as sequential key:value pairs. That is, List[0]    */
   /* = Key, List[1] = Value, List[2] = Key, etc. The exception is      */
   /* the "Devices" key. Since its value is a list of elements, the     */
   /* "Devices" key is followed by the number of elements, then each    */
   /* element in its own index of the array.                            */

   /* Addresses are stored as hexadecimal strings with each octet       */
   /* separated by a colon and all characters in upper-case. Integers   */
   /* are stored in ASCII-numeral format with no leading zeros, and     */
   /* Boolean values are stored as either the string "true" or "false", */
   /* in lower-case.                                                    */

   if(LocalDevProps)
   {
      DeviceList = NULL;

      /* If we need to include a device list, obtain the list now so we    */
      /* have the number of devices available to us.                       */
      if((IncludeFields == LOCAL_PROPERTY_ALL) || (IncludeFields & LOCAL_PROPERTY_DEVICES))
         DeviceListLength = GetKnownDeviceList(&DeviceList);
      else
         DeviceListLength = 0;

      /* Calculate the number of strings required give the desired         */
      /* properties. There are up to nine basic local device properties    */
      /* to be returned (nine pairs == eighteen strings). If any devices   */
      /* are to be included, reserve another two slots ("Devices" key and  */
      /* length) plus enough slots for the devices themselves.             */
      PropStringArrayLength = 0;
      if(IncludeFields & LOCAL_PROPERTY_ADDRESS)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_NAME)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_CLASS)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_POWERED)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_DISCOVERABLE)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_DISCOVERABLETIMEOUT)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_PAIRABLE)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_PAIRABLETIMEOUT)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_DISCOVERING)
         PropStringArrayLength += 2;
      if(IncludeFields & LOCAL_PROPERTY_DEVICES)
         if(DeviceListLength > 0)
            PropStringArrayLength += 2 + DeviceListLength;

      /* Allocate the Java array.                                          */
      if((PropStringArray = Env->NewObjectArray(PropStringArrayLength, Env->FindClass("java/lang/String"), NULL)) != NULL)
      {
         PropIndex = 0;

         /* Address                                                        */
         if(IncludeFields & LOCAL_PROPERTY_ADDRESS)
         {
            SS1_LOGI("\tAddress");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Address");
            BD_ADDRToStr(PropBuffer, sizeof(PropBuffer), LocalDevProps->BD_ADDR);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* Name                                                           */
         if(IncludeFields & LOCAL_PROPERTY_NAME)
         {
            SS1_LOGI("\tName");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Name");
            strncpy(PropBuffer, LocalDevProps->DeviceName, sizeof(PropBuffer));
            PropBuffer[sizeof(PropBuffer) - 1] = '\0';
            if(LocalDevProps->DeviceNameLength < (sizeof(PropBuffer) - 1))
            {
               if(PropBuffer[LocalDevProps->DeviceNameLength] != '\0')
               {
                  SS1_LOGE("Found Name field with no null termination");
                  PropBuffer[LocalDevProps->DeviceNameLength] = '\0';
               }
            }
            else
               SS1_LOGW("Given Local Device Name Length of %d (longer than is valid)", LocalDevProps->DeviceNameLength);

            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* Class                                                          */
         if(IncludeFields & LOCAL_PROPERTY_CLASS)
         {
            SS1_LOGI("\tClass");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Class");
            ClassOfDevice = 0x00ffffff & ((LocalDevProps->ClassOfDevice.Class_of_Device2 << 16) | (LocalDevProps->ClassOfDevice.Class_of_Device1 << 8) | (LocalDevProps->ClassOfDevice.Class_of_Device0));
            snprintf(PropBuffer, sizeof(PropBuffer), "%u", ClassOfDevice);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* Powered                                                        */
         if(IncludeFields & LOCAL_PROPERTY_POWERED)
         {
            SS1_LOGI("\tPowered");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Powered");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, (((DEVM_QueryDevicePowerState() == 1) && (LocalDevProps->ConnectableMode)) ? "true" : "false"));
         }

         /* Discoverable                                                   */
         if(IncludeFields & LOCAL_PROPERTY_DISCOVERABLE)
         {
            SS1_LOGI("\tDiscoverable");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Discoverable");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, (LocalDevProps->DiscoverableMode ? "true" : "false"));
         }

         /* DiscoverableTimeout                                            */
         if(IncludeFields & LOCAL_PROPERTY_DISCOVERABLETIMEOUT)
         {
            SS1_LOGI("\tDiscoverableTimeout");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "DiscoverableTimeout");
            snprintf(PropBuffer, sizeof(PropBuffer), "%u", LocalDevProps->DiscoverableModeTimeout);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* Pairable                                                       */
         if(IncludeFields & LOCAL_PROPERTY_PAIRABLE)
         {
            SS1_LOGI("\tPariable");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Pairable");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, ((LocalDevProps->ConnectableMode && LocalDevProps->PairableMode) ? "true" : "false"));
         }

         /* PairableTimeout                                                */
         if(IncludeFields & LOCAL_PROPERTY_PAIRABLETIMEOUT)
         {
            SS1_LOGI("\tPariableTimeout");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "PairableTimeout");
            snprintf(PropBuffer, sizeof(PropBuffer), "%u", LocalDevProps->PairableModeTimeout);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* Discovering                                                    */
         if(IncludeFields & LOCAL_PROPERTY_DISCOVERING)
         {
            SS1_LOGI("\tDiscovering");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Discovering");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, ((LocalDevProps->LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS) ? "true" : "false"));
         }

         /* Devices                                                        */
         if((IncludeFields & LOCAL_PROPERTY_DEVICES) && (DeviceListLength > 0))
         {
            SS1_LOGI("\tDevices (%d)", DeviceListLength);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Devices");
            snprintf(PropBuffer, sizeof(PropBuffer), "%u", DeviceListLength);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
            for(i = 0; i < DeviceListLength; i++)
            {
               BD_ADDRToPath(PropBuffer, sizeof(PropBuffer), DeviceList[i]);
               SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
            }
         }
      }
      else
         SS1_LOGE("Unable to create Java object array");

      if((DeviceListLength > 0) && (DeviceList))
         free(DeviceList);
   }
   else
      PropStringArray = NULL;

   return(PropStringArray);
}

jobjectArray BuildRemotePropertyStringArray(JNIEnv *Env, unsigned long IncludeFields, DEVM_Remote_Device_Properties_t *RemoteDevProps)
{
   int          i;
   int          UUIDListLength;
   char         PropBuffer[256];
   size_t       Length;
   unsigned int PropIndex;
   unsigned int ClassOfDevice;
   unsigned int PropStringArrayLength;
   jobjectArray PropStringArray;
   UUID_128_t  *UUIDList;

   UUIDList = NULL;
   UUIDListLength = 0;

   /* The list is a 1-dimensional array of Java strings formatted, for  */
   /* the most part, as sequential key:value pairs. That is, List[0]    */
   /* = Key, List[1] = Value, List[2] = Key, etc. The exception is      */
   /* the "Devices" key. Since its value is a list of elements, the     */
   /* "Devices" key is followed by the number of elements, then each    */
   /* element in its own index of the array.                            */

   /* Addresses are stored as hexadecimal strings with each octet       */
   /* separated by a colon and all characters in upper-case. Integers   */
   /* are stored in ASCII-numeral format with no leading zeros, and     */
   /* Boolean values are stored as either the string "true" or "false", */
   /* in lower-case.                                                    */

   if(RemoteDevProps)
   {
      SS1_LOGI("Building Remote Device Prop List (0x%02lx)", IncludeFields);

      /* Calculate the number of strings required give the desired      */
      /* properties. There are up to twelve basic local device          */
      /* properties to be returned (12 pairs == 24 strings). If any     */
      /* UUIDS are to be included, reserve another two slots ("UUIDs"   */
      /* key and length) plus enough slots for the UUIDs themselves.    */
      PropStringArrayLength = 0;
      if(IncludeFields & REMOTE_PROPERTY_ADDRESS)
         PropStringArrayLength += 2;
      if((IncludeFields & REMOTE_PROPERTY_NAME) && (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN))
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_CLASS)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_PAIRED)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_CONNECTED)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_TRUSTED)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_ALIAS)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_NODES)
      {
         /* FIXME What's the format expected for "Nodes" and         */
         /* what's the content? I've never seen BlueZ provide this   */
         /* property.                                                */
      }
      if(IncludeFields & REMOTE_PROPERTY_ADAPTER)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_LEGACYPAIRING)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_RSSI)
         PropStringArrayLength += 2;
      if(IncludeFields & REMOTE_PROPERTY_TXPOWER)
         PropStringArrayLength += 2;
      if((IncludeFields & REMOTE_PROPERTY_UUIDS) && (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN))
      {
         if((UUIDListLength = GetRemoteDeviceServiceUUIDs(RemoteDevProps->BD_ADDR, &UUIDList)) > 0)
         {
            PropStringArrayLength += (2 + UUIDListLength);
         }
      }

      /* Allocate the Java array.                                       */
      if((PropStringArray = Env->NewObjectArray(PropStringArrayLength, Env->FindClass("java/lang/String"), NULL)) != NULL)
      {
         PropIndex = 0;

         /* Address                                                     */
         if(IncludeFields & REMOTE_PROPERTY_ADDRESS)
         {
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Address");
            BD_ADDRToStr(PropBuffer, sizeof(PropBuffer), RemoteDevProps->BD_ADDR);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* Name                                                        */
         if(IncludeFields & REMOTE_PROPERTY_NAME)
         {
            if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
            {
               SetStringArrayElement(Env, PropStringArray, PropIndex++, "Name");
               strncpy(PropBuffer, RemoteDevProps->DeviceName, sizeof(PropBuffer));

               /* Guarantee that we have a null byte at the end of the  */
               /* string, but also ensure that there's a null byte at   */
               /* the end of the claimed name length in case the name   */
               /* wasn't properly terminated (might be possible if the  */
               /* name length is zero?)                                 */
               PropBuffer[sizeof(PropBuffer) - 1] = '\0';
               if(RemoteDevProps->DeviceNameLength < sizeof(PropBuffer))
                  PropBuffer[RemoteDevProps->DeviceNameLength] = '\0';

               SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
            }
            else
               SS1_LOGD("Device name is unknown: NAME field excluded. (0x%02lx)", RemoteDevProps->RemoteDeviceFlags);
         }

         /* Class                                                       */
         if(IncludeFields & REMOTE_PROPERTY_CLASS)
         {
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Class");
            ClassOfDevice = 0x00ffffff & ((RemoteDevProps->ClassOfDevice.Class_of_Device2 << 16) | (RemoteDevProps->ClassOfDevice.Class_of_Device1 << 8) | (RemoteDevProps->ClassOfDevice.Class_of_Device0));
            snprintf(PropBuffer, sizeof(PropBuffer), "%u", ClassOfDevice);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* Paired                                                      */
         if(IncludeFields & REMOTE_PROPERTY_PAIRED)
         {
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Paired");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, ((RemoteDevProps->RemoteDeviceFlags & (DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED | DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE)) ? "true" : "false"));
         }

         /* Connected                                                   */
         if(IncludeFields & REMOTE_PROPERTY_CONNECTED)
         {
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Connected");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, ((RemoteDevProps->RemoteDeviceFlags & (DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED | DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE)) ? "true" : "false"));
         }

         /* Trusted                                                     */
         if(IncludeFields & REMOTE_PROPERTY_TRUSTED)
         {
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Trusted");
            if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)
            {
               if(RemoteDevProps->ApplicationData.ApplicationInfo & REMOTE_DEVICE_APPLICATION_INFO_FLAG_TRUSTED)
                  SetStringArrayElement(Env, PropStringArray, PropIndex++, "true");
               else
                  SetStringArrayElement(Env, PropStringArray, PropIndex++, "false");
            }
            else
               SetStringArrayElement(Env, PropStringArray, PropIndex++, "false");
         }

         /* Alias                                                       */
         if(IncludeFields & REMOTE_PROPERTY_ALIAS)
         {
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Alias");
            if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID)
            {
               memset(PropBuffer, 0, sizeof(PropBuffer));

               Length = (sizeof(PropBuffer) - 1);
               if(RemoteDevProps->ApplicationData.FriendlyNameLength < Length)
                  Length = RemoteDevProps->ApplicationData.FriendlyNameLength;

               memcpy(PropBuffer, RemoteDevProps->ApplicationData.FriendlyName, Length);
               PropBuffer[Length] = '\0';
               SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
            }
            else
            {
               /* No custom alias is available, so use the device name. */
               if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
               {
                  strncpy(PropBuffer, RemoteDevProps->DeviceName, sizeof(PropBuffer));
                  PropBuffer[sizeof(PropBuffer) - 1] = '\0';
                  SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
               }
               else
               {
                  /* Fall back to using the device address.             */
                  BD_ADDRToStr(PropBuffer, sizeof(PropBuffer), RemoteDevProps->BD_ADDR);
                  /* Replace colons with dashes. BD_ADDRToStr guarantees   */
                  /* five colons at every third element since each hex     */
                  /* octet is delimited with them, so there's no need to   */
                  /* search for them.                                      */
                  PropBuffer[2] = PropBuffer[5] = PropBuffer[8] = PropBuffer[11] = PropBuffer[14] = '-';
                  SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
               }
            }
         }

         /* Nodes                                                       */
         if(IncludeFields & REMOTE_PROPERTY_NODES)
         {
            /* Android makes no use of the "Nodes" property.            */
         }

         /* Adapter                                                     */
         if(IncludeFields & REMOTE_PROPERTY_ADAPTER)
         {
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "Adapter");
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "");
         }

         /* Legacy Pairing                                              */
         if(IncludeFields & REMOTE_PROPERTY_LEGACYPAIRING)
         {
            SS1_LOGI("Inserting LegacyPairing @ index %u", PropIndex);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "LegacyPairing");
            /* FIXME Add way to actually determine whether the device   */
            /* supports SSP. For now, assume no device supports         */
            /* it ("true" == "Requires Legacy/Non-SSP Pairing")         */
            /* because I don't think Android cares. Passkeys and        */
            /* User Confirmation are supported, and no SSP-specific     */
            /* connection routine is provided, so it's more likely that */
            /* it leaves the decision of legacy vs SSP up to the stack. */
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "true");
         }

         /* RSSI                                                        */
         if(IncludeFields & REMOTE_PROPERTY_RSSI)
         {
            SS1_LOGI("Inserting RSSI @ index %u", PropIndex);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "RSSI");
            if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)
               snprintf(PropBuffer, sizeof(PropBuffer), "%d", RemoteDevProps->RSSI);
            else
               snprintf(PropBuffer, sizeof(PropBuffer), "%d", RemoteDevProps->LE_RSSI);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* TX Power                                                    */
         if(IncludeFields & REMOTE_PROPERTY_TXPOWER)
         {
            SS1_LOGI("Inserting Tx Power @ index %u", PropIndex);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, "TX");
            snprintf(PropBuffer, sizeof(PropBuffer), "%d", RemoteDevProps->TransmitPower);
            SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
         }

         /* UUIDs                                                       */
         if(IncludeFields & REMOTE_PROPERTY_UUIDS)
         {
            if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN)
            {
               SS1_LOGI("DevFlags says I should include UUIDs (0x%02lx)", RemoteDevProps->RemoteDeviceFlags);
               SS1_LOGI("Inserting UUIDs @ index %u", PropIndex);
               if(UUIDListLength > 0)
               {
                  SetStringArrayElement(Env, PropStringArray, PropIndex++, "UUIDs");
                  snprintf(PropBuffer, sizeof(PropBuffer), "%d", UUIDListLength);
                  SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
                  for(i = 0; i < UUIDListLength; i++)
                  {
                     if(UUID128ToStr(PropBuffer, sizeof(PropBuffer), UUIDList[i]))
                        SetStringArrayElement(Env, PropStringArray, PropIndex++, PropBuffer);
                     else
                        SS1_LOGE("Unable to convert UUID to string (%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x)", UUIDList[i].UUID_Byte0, UUIDList[i].UUID_Byte1, UUIDList[i].UUID_Byte2, UUIDList[i].UUID_Byte3, UUIDList[i].UUID_Byte4, UUIDList[i].UUID_Byte5, UUIDList[i].UUID_Byte6, UUIDList[i].UUID_Byte7, UUIDList[i].UUID_Byte8, UUIDList[i].UUID_Byte9, UUIDList[i].UUID_Byte10, UUIDList[i].UUID_Byte11, UUIDList[i].UUID_Byte12, UUIDList[i].UUID_Byte13, UUIDList[i].UUID_Byte14, UUIDList[i].UUID_Byte15);
                  }
               }
            }
         }
      }
      else
         SS1_LOGE("Unable to allocate Java Array");
   }
   else
      PropStringArray = NULL;

   return(PropStringArray);
}


jobjectArray SplitNextProp(JNIEnv *Env, jobjectArray PropertyList, jsize *NextPropStart)
{
   jsize         i;
   jsize         PropListLength;
   jsize         NumExtraPropValues;
   jsize         SinglePropertyLength;
   jstring       PropStringKey;
   jstring       PropStringValue;
   const char   *PropStringChars;
   jthrowable    exception;
   jobjectArray  SingleProperty;
   unsigned int  PropStringCharsLength;
   unsigned long SinglePropertyType;

   SingleProperty = NULL;
   SinglePropertyLength = 2;

   if(Env && PropertyList && NextPropStart && (*NextPropStart >= 0))
   {
      PropListLength = Env->GetArrayLength(PropertyList);

      /* Check that we have at least two elements in the property list  */
      /* staring from NextPropStart (at least a key and value pair).    */
      if((*NextPropStart + 1) < PropListLength)
      {
         if((PropStringKey = (jstring)(Env->GetObjectArrayElement(PropertyList, *NextPropStart))) != NULL)
         {
            if((PropStringValue = (jstring)(Env->GetObjectArrayElement(PropertyList, (*NextPropStart + 1)))) != NULL)
            {
               if((PropStringChars = Env->GetStringUTFChars(PropStringKey, NULL)) != NULL)
               {
                  PropStringCharsLength = Env->GetStringUTFLength(PropStringKey) + 1;
                  if((strncmp(PropStringChars, "Devices", PropStringCharsLength) == 0) || (strncmp(PropStringChars, "UUIDs", PropStringCharsLength) == 0))
                  {
                     SinglePropertyType = LOCAL_PROPERTY_DEVICES;
                     Env->ReleaseStringUTFChars(PropStringKey, PropStringChars);

                     if((PropStringChars = Env->GetStringUTFChars(PropStringValue, NULL)) != NULL)
                     {
                        NumExtraPropValues = 0;
                        sscanf(PropStringChars, "%u", &NumExtraPropValues);
                        Env->ReleaseStringUTFChars(PropStringValue, PropStringChars);

                        /* Ensure we don't exceed the bounds of the     */
                        /* property list. If there aren't enough items  */
                        /* in the list, just use up the remaining       */
                        /* elements.                                    */
                        if((*NextPropStart + 1 + NumExtraPropValues) < PropListLength)
                           SinglePropertyLength += NumExtraPropValues;
                        else
                           SinglePropertyLength = (PropListLength - *NextPropStart);
                     }
                  }

                  if((SingleProperty = Env->NewObjectArray(SinglePropertyLength, Env->FindClass("java/lang/String"), NULL)) != NULL)
                  {
                     Env->SetObjectArrayElement(SingleProperty, 0, PropStringKey);
                     Env->SetObjectArrayElement(SingleProperty, 1, PropStringValue);

                     Env->DeleteLocalRef(PropStringKey);
                     Env->DeleteLocalRef(PropStringValue);

                     for(i = 2; i < SinglePropertyLength; i++)
                     {
                        PropStringValue = (jstring) Env->GetObjectArrayElement(PropertyList, (*NextPropStart + i));
                        Env->SetObjectArrayElement(SingleProperty, i, PropStringValue);
                        Env->DeleteLocalRef(PropStringValue);
                     }

                     *NextPropStart += SinglePropertyLength;
                     if(*NextPropStart >= PropListLength)
                        *NextPropStart = -1;
                  }
               }
               else
                  SS1_LOGE("Out of memory A");
            }
            else
               SS1_LOGE("Out of memory B");
         }
         else
            SS1_LOGE("Out of memory C");
      }
      else
         *NextPropStart = -1;
   }

   return(SingleProperty);
}

   /* The following function translates from BluetopiaPM                */
   /* DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_* flags to LOCAL_PROPERTY_*  */
   /* flags as used by the BuildLocalPropertyStringArray() function.    */
   /* Note that DEVM_LOCAL_PROPERTIES_CHANGED_CONNECTABLE_MODE is       */
   /* treated specially:                                                */
   /*   - In Android 2.3 and older, Android assumes that the BT stack is*/
   /*     always connectable, so we ignore the "Connectable Mode"       */
   /*     property.                                                     */
   /*   - Android 4.x does NOT assume that it is connectable. In its    */
   /*     version of BlueZ, the "Powered" property can be written to set*/
   /*     connectability, with the side-effect that, when becoming      */
   /*     connectable, discoverability is also set back to the          */
   /*     discoverability state that was in effect when connectability  */
   /*     was last disabled. This generates a "Discoverable" property   */
   /*     update which is what Android keys off of to continue the init */
   /*     process after becoming connectable. Therefore, a change in our*/
   /*     "Connectable Mode" property is remapped to report the current */
   /*     "Discoverable Mode" state.                                    */
unsigned long BTPMChangedLocalPropsToBTJB(unsigned long BTPMProps)
{
   unsigned long ret_val = 0;

   if(BTPMProps & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_CLASS_OF_DEVICE)
      ret_val |= LOCAL_PROPERTY_CLASS;

   if(BTPMProps & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_DEVICE_NAME)
      ret_val |= LOCAL_PROPERTY_NAME;

   if(BTPMProps & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_DISCOVERABLE_MODE)
      ret_val |= LOCAL_PROPERTY_DISCOVERABLE;

#if SS1_PLATFORM_SDK_VERSION >= 14
   if(BTPMProps & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_CONNECTABLE_MODE)
      ret_val |= (LOCAL_PROPERTY_DISCOVERABLE | LOCAL_PROPERTY_POWERED);
#endif

   if(BTPMProps & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_PAIRABLE_MODE)
      ret_val |= LOCAL_PROPERTY_PAIRABLE;

   if(BTPMProps & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_DEVICE_FLAGS)
      ret_val |= LOCAL_PROPERTY_DISCOVERING;

   return(ret_val);
}


   /* The following function translates from BluetopiaPM                */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_* flags to REMOTE_PROPERTY_**/
   /* flags as used by the BuildRemotePropertyStringArray() functions.  */
unsigned long BTPMChangedRemotePropsToBTJB(unsigned long BTPMProps)
{
   unsigned long ret_val = 0;

   if(BTPMProps & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_CLASS_OF_DEVICE)
      ret_val |= REMOTE_PROPERTY_CLASS;

   if(BTPMProps & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_DEVICE_NAME)
      ret_val |= REMOTE_PROPERTY_NAME;

   if(BTPMProps & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_APPLICATION_DATA)
      ret_val |= (REMOTE_PROPERTY_ALIAS | REMOTE_PROPERTY_TRUSTED);

   if(BTPMProps & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_RSSI)
      ret_val |= REMOTE_PROPERTY_RSSI;

   if(BTPMProps & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_PAIRING_STATE)
      ret_val |= REMOTE_PROPERTY_PAIRED;

   if(BTPMProps & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_CONNECTION_STATE)
      ret_val |= REMOTE_PROPERTY_CONNECTED;

   if(BTPMProps & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_SERVICES_STATE)
      ret_val |= REMOTE_PROPERTY_UUIDS;

   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_RSSI                     */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_PAIRING_STATE            */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_CONNECTION_STATE         */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_SERVICES_STATE           */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_ENCRYPTION_STATE            */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_SNIFF_STATE                 */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_ENCRYPTION_STATE         */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_PRIOR_RESOLVABLE_ADDRESS    */
   /* DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_DEVICE_APPEARANCE           */

   return(ret_val);
}


   /* The following function is provided to search the SDP records      */
   /* of a remote device in order to find the RFCOMM Channel on         */
   /* which a particular service (specified by UUID) is listening.      */
   /* The first parameter is the address of the remote device. The      */
   /* second parameter is the UUID of the service of interest (this     */
   /* is the UUID contained in the Service ID field of the service's    */
   /* SDP record, obtainable with GetRemoteServiceUUIDs() ). The        */
   /* third parameter is the record attribute to be searched -- this    */
   /* should be either SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST or     */
   /* SDP_ATTRIBUTE_ID_ADDITIONAL_PROTOCOL_DESCRIPTOR_LISTS, as no      */
   /* other attribute makes sense for this purpose. If successful,      */
   /* the function returns either the RFCOMM channel number. Zero is    */
   /* returned if no channel was found. On error, a negative value is   */
   /* returned.                                                         */
int GetRemoteDeviceRFCOMMChannel(BD_ADDR_t BD_ADDR, UUID_128_t UUID, Word_t AttributeID)
{
   int                                    ret_val;
   int                                    Channel;
   int                                    RawSDPDataLength;
   UUID_128_t                             RecordUUID;
   unsigned int                           i;
   unsigned char                         *RawSDPData;
   SDP_Data_Element_t                    *TargetAttributeData;
   DEVM_Parsed_SDP_Data_t                 ParsedSDPData;
   SDP_Service_Attribute_Response_Data_t *TargetRecord;

   /* XXX DEBUG */
   {
      char AddrStr[32];
      char UUIDStr[64];
      BD_ADDRToStr(AddrStr, sizeof(AddrStr), BD_ADDR);
      UUID128ToStr(UUIDStr, sizeof(UUIDStr), UUID);
      SS1_LOGI("Searching device %s for UUID %s", AddrStr, UUIDStr);
   }

   ret_val = -1;

   /* Validate parameters. Searching for RFCOMM channels only makes     */
   /* sense in the context of either a Protocol Descriptor List or      */
   /* Alternate Protocol Descriptor List attribute.                     */
   if((AttributeID = SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST) || (AttributeID == SDP_ATTRIBUTE_ID_ADDITIONAL_PROTOCOL_DESCRIPTOR_LISTS))
   {
      /* Retreive the raw SDP record dump from the remote device.       */
      if((RawSDPDataLength = GetRawServiceData(BD_ADDR, &RawSDPData, false)) >= 0)
      {
         /* Parse the raw records into a usable format.                 */
         if(DEVM_ConvertRawSDPStreamToParsedSDPData(RawSDPDataLength, RawSDPData, &ParsedSDPData) == 0)
         {
            TargetRecord = NULL;
            TargetAttributeData = NULL;

            /* Find the Service Record whose unique Service UUID        */
            /* matches our target UUID.                                 */
            for(i = 0; i < ParsedSDPData.NumberServiceRecords; i++)
            {
               RecordUUID = GetServiceUUIDFromRecord(ParsedSDPData.SDPServiceAttributeResponseData[i]);
               if(COMPARE_UUID_128(UUID, RecordUUID))
               {
                  SS1_LOGI("Found target UUID in record %u", i);
                  TargetRecord = &(ParsedSDPData.SDPServiceAttributeResponseData[i]);
                  break;
               }
            }

            /* If we found the matching service record, next find the   */
            /* attribute we want.                                       */
            if(TargetRecord)
            {
               for(i = 0; i < TargetRecord->Number_Attribute_Values; i++)
               {
                  if(AttributeID == TargetRecord->SDP_Service_Attribute_Value_Data[i].Attribute_ID)
                  {
                     TargetAttributeData = TargetRecord->SDP_Service_Attribute_Value_Data[i].SDP_Data_Element;
                     break;
                  }
               }
            }

            /* If we found the right attribute, search it's data for a  */
            /* channel number.                                          */
            if(TargetAttributeData)
            {
               Channel = 0;

               if(AttributeID == SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST)
               {
                  ret_val = FindRfcommChannelInProtoList(TargetAttributeData);
               }
               else
               {
                  if(AttributeID == SDP_ATTRIBUTE_ID_ADDITIONAL_PROTOCOL_DESCRIPTOR_LISTS)
                  {
                     /* An "Additional Protocol Descriptor List" is a   */
                     /* sequence of Protocol Descriptor Lists. Search   */
                     /* each in turn for an RFCOMM channel and return   */
                     /* the first channel we find, if any.              */
                     if(TargetAttributeData->SDP_Data_Element_Type == deSequence)
                     {
                        ret_val = 0;
                        for(i = 0; ((i < TargetAttributeData->SDP_Data_Element_Length) && (ret_val == 0)); i++)
                           ret_val = FindRfcommChannelInProtoList(&(TargetAttributeData->SDP_Data_Element.SDP_Data_Element_Sequence[i]));
                     }
                  }
               }
            }
            else
               ret_val = -20;

            DEVM_FreeParsedSDPData(&ParsedSDPData);
         }

         free(RawSDPData);
      }
      else
      {
         /* GetRawServiceData() failed, so pick up its return value.    */
         ret_val = RawSDPDataLength;
      }
   }

   if(ret_val > 0)
      SS1_LOGI("Found service running on RFCOMM channel # %d", ret_val);

   return(ret_val);
}


   /* The following function is used to attach the active thread        */
   /* to a Java VM with the intention of calling methods of the         */
   /* JNIEnv interface. This function returns >= 0 to indicate that     */
   /* the thread is attached and the *Env pointer is now valid,         */
   /* or a negative value if an error occurs. If the return value       */
   /* is 0, the thread was already attached before this function        */
   /* was called. If the return value is positive, non-zero, then       */
   /* JNIEnv::DetachCurrentThread(JavaVM *) MUST be called before the   */
   /* thread exits.                                                     */
int AttachThreadToJVM(JavaVM *VM, jint JNIEnvVersion, JNIEnv **Env)
{
   int              ret_val;
   jint             GetEnvResult;
   JavaVMAttachArgs AttachArgs;

   ret_val = -1;

   if(VM && Env)
   {
      GetEnvResult = VM->GetEnv((void **)Env, JNIEnvVersion);

      if(GetEnvResult == JNI_EDETACHED)
      {
         /* The thread isn't attached yet, so we need to try to do that */
         /* now.                                                        */
         AttachArgs.version = JNIEnvVersion;
         AttachArgs.name = NULL;
         AttachArgs.group = NULL;
         if(VM->AttachCurrentThread(Env, &AttachArgs) >= 0)
         {
            /* Since we know the thread wasn't already attached,        */
            /* success of AttachCurrentThread implies that the thread   */
            /* should be detached once the JNI interface is no longer   */
            /* needed. Return a value > 0 to indicate this condition.   */
            ret_val = 1;
         }
         else
            SS1_LOGE("Unable to attach thread");
      }
      else
      {
         if(GetEnvResult == JNI_OK)
         {
            /* We successfully obtain a JNIEnv reference from GetEnv(), */
            /* meaning the thread was already attached. Return 0 to     */
            /* indicate this condition.                                 */
            ret_val = 0;
         }
         else
         {
            if(GetEnvResult == JNI_EVERSION)
               SS1_LOGE("Invalid JNIEnv version specified");
            else
               SS1_LOGE("Unknown error obtaining JNIEnv interface");
         }
      }
   }
   else
      SS1_LOGE("Called with invalid parameters (%p, %p/%p)", VM, Env, (Env ? *Env : (JNIEnv *)0xffffffff));

   return(ret_val);
}


   /* The following function is used to sanitize character strings      */
   /* encoded in UTF-8. That is, Length bytes from the position         */
   /* given by UTF8String are tested for UTF-8 conformance. Any byte    */
   /* sequences determined to not be valid UTF-8 character encodings    */
   /* are replaced with sequences of a replacement character (third     */
   /* parameter, optional; by default, use '?'). Note that the buffer   */
   /* will be modified in place.                                        */
void SanitizeUTF8(char *UTF8String, unsigned int Length, char Replacement)
{
   bool           OK;
   unsigned int   i;
   unsigned int   j;
   unsigned int   SeqLength;
   unsigned int   EncodedValue;
   unsigned char *UCharString;

   if(UTF8String)
   {
      UCharString = (unsigned char *)UTF8String;

      for(i = 0; i < Length; i += SeqLength)
      {
         OK = true;

         if((UCharString[i] & 0x80) == 0x00)
         {
            /* 7-bit codepoint in 1 byte (ASCII). All possible values   */
            /* (0x00-0x7f) are valid here.                              */
            SeqLength = 1;
         }
         else
         {
            if((UCharString[i] & 0xE0) == 0xC0)
            {
               /* 11-bit codepoint spread over 2 bytes (5-6)            */
               SeqLength = 2;

               /* Make sure there enough bytes left in the buffer to    */
               /* contain this encoding.                                */
               if((i + 1) < Length)
               {
                  /* Check that this representation is the minimum      */
                  /* required for this code-point.                      */
                  if((UCharString[i] & 0xFE) != 0xC0)
                  {
                     /* Now check that the remaining bytes all follow   */
                     /* the (bit) format 10xxxxxx.                      */
                     for(j = 1; j < SeqLength; j++)
                     {
                        if((UCharString[i + j] & 0xC0) != 0x80)
                        {
                           OK = false;
                           break;
                        }
                     }
                  }
                  else
                     OK = false;
               }
               else
                  OK = false;
            }
            else
            {
               if((UCharString[i] & 0xF0) == 0xE0)
               {
                  /* 16-bit codepoint spread over 3 bytes (4-6-6)       */
                  SeqLength = 3;

                  /* Make sure there enough bytes left in the buffer to */
                  /* contain this encoding.                             */
                  if((i + 2) < Length)
                  {
                     /* Check that this representation is the minimum   */
                     /* required for this code-point.                   */
                     if((UCharString[i] != 0xE0) || ((UCharString[i + 1] & 0xE0) != 0x80))
                     {
                        /* Now check that the remaining bytes all       */
                        /* follow the (bit) format 10xxxxxx.            */
                        for(j = 1; j < SeqLength; j++)
                        {
                           if((UCharString[i + j] & 0xC0) != 0x80)
                           {
                              OK = false;
                              break;
                           }
                        }

                        /* The 16-bit codepoint has specific values     */
                        /* that are not allowed in UTF-8. Specifically, */
                        /* this is 0xD800 - 0xDFFF, 0x0FFFE, and        */
                        /* 0x0FFFF. If the encoding still appears       */
                        /* valid, check for these values now.           */
                        if(OK)
                        {
                           EncodedValue  = 0;
                           EncodedValue += (UCharString[i]     & 0x0F) << 12;
                           EncodedValue += (UCharString[i + 1] & 0x3F) << 6;
                           EncodedValue += (UCharString[i + 2] & 0x3F);

                           if(((0x0D800 <= EncodedValue) && (EncodedValue <= 0x0DFFF)) || (EncodedValue == 0x0FFFE) || (EncodedValue == 0x0FFFF))
                              OK = false;
                        }
                     }
                     else
                        OK = false;
                  }
                  else
                     OK = false;
               }
               else
               {
                  if((UCharString[i] & 0xF8) == 0xF0)
                  {
                     /* 21-bit codepoint spread over 4 bytes (3-6-6-6)  */
                     SeqLength = 4;

                     /* Make sure there enough bytes left in the buffer */
                     /* to contain this encoding.                       */
                     if((i + 3) < Length)
                     {
                        /* Check that this representation is the        */
                        /* minimum required for this code-point.        */
                        if((UCharString[i] != 0xF0) || ((UCharString[i + 1] & 0xF0) != 0x80))
                        {
                           /* Now check that the remaining bytes all    */
                           /* follow the (bit) format 10xxxxxx.         */
                           for(j = 1; j < SeqLength; j++)
                           {
                              if((UCharString[i + j] & 0xC0) != 0x80)
                              {
                                 OK = false;
                                 break;
                              }
                           }
                        }
                        else
                           OK = false;
                     }
                     else
                        OK = false;
                  }
                  else
                  {
                     if((UCharString[i] & 0xFC) == 0xF8)
                     {
                        /* 26-bit codepoint spread over 5 bytes         */
                        /* (2-6-6-6-6)                                  */
                        SeqLength = 5;

                        /* Make sure there enough bytes left in the     */
                        /* buffer to contain this encoding.             */
                        if((i + 4) < Length)
                        {
                           /* Check that this representation is the     */
                           /* minimum required for this code-point.     */
                           if((UCharString[i] != 0xF8) || ((UCharString[i + 1] & 0xF8) != 0x80))
                           {
                              /* Now check that the remaining bytes all */
                              /* follow the (bit) format 10xxxxxx.      */
                              for(j = 1; j < SeqLength; j++)
                              {
                                 if((UCharString[i + j] & 0xC0) != 0x80)
                                 {
                                    OK = false;
                                    break;
                                 }
                              }
                           }
                           else
                              OK = false;
                        }
                        else
                           OK = false;
                     }
                     else
                     {
                        if((UCharString[i] & 0xFE) == 0xFC)
                        {
                           /* 31-bit codepoint spread over 6 bytes      */
                           /* (1-6-6-6-6-6)                             */
                           SeqLength = 6;

                           /* Make sure there enough bytes left in the  */
                           /* buffer to contain this encoding.          */
                           if((i + 5) < Length)
                           {
                              /* Check that this representation is the  */
                              /* minimum required for this code-point.  */
                              if((UCharString[i] != 0xFC) || ((UCharString[i + 1] & 0xFC) != 0x80))
                              {
                                 /* Now check that the remaining        */
                                 /* bytes all follow the (bit) format   */
                                 /* 10xxxxxx.                           */
                                 for(j = 1; j < SeqLength; j++)
                                 {
                                    if((UCharString[i + j] & 0xC0) != 0x80)
                                    {
                                       OK = false;
                                       break;
                                    }
                                 }
                              }
                              else
                                 OK = false;
                           }
                           else
                              OK = false;
                        }
                        else
                        {
                           /* Invalid encoding.                         */
                           SeqLength = 1;
                           OK        = false;
                        }
                     }
                  }
               }
            }
         }

         if(!OK)
         {
            /* This encoding was invalid. Replace all bytes in the byte */
            /* sequence.                                                */
            for(j = 0; ((j < SeqLength) && ((i +j) < Length)); j++)
               UCharString[i + j] = Replacement;
         }
      }
   }
}


   /* The following function is used to initialize the contents of a    */
   /* DeviceList_t structure. The second parameter is optional -- if not*/
   /* specified, or if specified as zero, a default initial capacity is */
   /* used.                                                             */
bool InitDeviceList(DeviceList_t *DeviceList, unsigned int InitialCapacity)
{
   if(DeviceList)
   {
      if(InitialCapacity == 0)
         InitialCapacity = 16;

      pthread_mutex_init(&(DeviceList->Mutex), NULL);
      DeviceList->Devices  = (DeviceList_Device_t *)calloc(InitialCapacity, sizeof(DeviceList_Device_t));
      DeviceList->Length   = 0;
      DeviceList->Capacity = (DeviceList->Devices ? InitialCapacity : 0);
   }

   return((DeviceList) && (DeviceList->Devices));
}


   /* The following function is used to clean up the resources          */
   /* associated with a DeviceList_t structure.                         */
bool DestroyDeviceList(DeviceList_t *DeviceList)
{
   if(DeviceList)
   {
      /* Wait for active access to the list to finish. Not perfectly       */
      /* thread-safe, but pthreads doesn't support destroying a mutex while*/
      /* it is locked. The user shouldn't be cleaning up the list while    */
      /* another thread could still be using it, anyway.                   */
      pthread_mutex_lock(&(DeviceList->Mutex));
      pthread_mutex_unlock(&(DeviceList->Mutex));

      /* Free resource allocations.                                        */
      pthread_mutex_destroy(&(DeviceList->Mutex));

      if(DeviceList->Devices)
      {
         free(DeviceList->Devices);
         DeviceList->Devices = NULL;
      }
   }

   return(DeviceList?true:false);
}

   /* The following function is used to remove all devices from a       */
   /* DeviceList_t structure.  If the optional CleanCallback is         */
   /* specified, each device will be passed to this callback as it is   */
   /* removed from the list.                                            */
bool CleanDeviceList(DeviceList_t *DeviceList, void (*CleanCallback)(BD_ADDR_t BD_ADDR, Boolean_t LowEnergy, void *ExtraData, void *CallbackParameter), void *CallbackParameter)
{
   bool         ret_val;
   unsigned int Index;

   if(DeviceList)
   {
      pthread_mutex_lock(&(DeviceList->Mutex));

      /* If a callback was specified, call the callback with each device*/
      /* in the list).                                                  */
      if(CleanCallback)
      {
         for(Index = 0; Index < DeviceList->Length; Index++)
            (*CleanCallback)(DeviceList->Devices[Index].BD_ADDR, DeviceList->Devices[Index].LowEnergy, DeviceList->Devices[Index].ExtraData, CallbackParameter);
      }

      /* Erase the old devices.                                         */
      DeviceList->Length = 0;
      memset(DeviceList->Devices, 0, (DeviceList->Capacity * sizeof(DeviceList_Device_t)));

      ret_val = true;

      pthread_mutex_unlock(&(DeviceList->Mutex));
   }
   else
      ret_val = false;

   return(ret_val);
}

   /* The following function is used to add devices to a DeviceList_t   */
   /* list. The function returns true if the device is successfully     */
   /* added, or false if the device is already in the list or an error  */
   /* occurs. If the third parameter (ExtraData) is non-NULL, the       */
   /* value will be stored with the device and will be retrievable via  */
   /* RemoveDeviceFromList(). The fourth parameter (KeyOnExtraData) is  */
   /* optional (defaults to false) -- if given as true, the ExtraData   */
   /* field is used for uniqueness checks rather that the device        */
   /* address. In this case, RemoveDeviceFromListByExtraData() should be*/
   /* used for removing elements from the list.                         */
int AddDeviceToList(DeviceList_t *DeviceList, BD_ADDR_t BD_ADDR, Boolean_t LowEnergy, void *ExtraData, bool KeyOnExtraData)
{
   int                  ret_val;
   bool                 DeviceExists;
   unsigned int         i;
   DeviceList_Device_t *TmpPtr;

   ret_val = ADD_DEVICE_ERROR;

   if(DeviceList)
   {
      pthread_mutex_lock(&(DeviceList->Mutex));

      if(DeviceList->Devices)
      {
         DeviceExists = false;
         for(i = 0; i < DeviceList->Length; i++)
         {
            if((KeyOnExtraData && (ExtraData == DeviceList->Devices[i].ExtraData)) || (!KeyOnExtraData && COMPARE_BD_ADDR(BD_ADDR, DeviceList->Devices[i].BD_ADDR)))
            {
               DeviceExists = true;

               /* Exit the loop.                                        */
               i = DeviceList->Length;
            }
         }

         if(!DeviceExists)
         {
            if(DeviceList->Length == DeviceList->Capacity)
            {
               TmpPtr = (DeviceList_Device_t *)realloc(DeviceList->Devices, (((DeviceList->Capacity == 0) ? 16 : (DeviceList->Capacity * 2)) * sizeof(DeviceList_Device_t)));
               if(TmpPtr)
               {
                  DeviceList->Devices = TmpPtr;
                  DeviceList->Capacity = ((DeviceList->Capacity == 0) ? 16 : (DeviceList->Capacity * 2));
               }
            }

            if(DeviceList->Length < DeviceList->Capacity)
            {
               DeviceList->Devices[DeviceList->Length].BD_ADDR    = BD_ADDR;
               DeviceList->Devices[DeviceList->Length].LowEnergy  = (LowEnergy ? TRUE : FALSE);
               DeviceList->Devices[DeviceList->Length].ExtraData  = ExtraData;
               DeviceList->Length                                += 1;

               ret_val = ADD_DEVICE_SUCCESS;

/* XXX DEBUG */SS1_LOGI("Device added. List contents (%u/%u):", DeviceList->Length, DeviceList->Capacity);
/* XXX DEBUG */for(i = 0; i < DeviceList->Length; i++) { char bd[32]; BD_ADDRToStr(bd, sizeof(bd), DeviceList->Devices[i].BD_ADDR); SS1_LOGI("   -- %s/%c (%p)", bd, (DeviceList->Devices[i].LowEnergy ? 'L' : 'C'), DeviceList->Devices[i].ExtraData); }
            }
         }
         else
            ret_val = ADD_DEVICE_ALREADY_EXISTS;
      }

      pthread_mutex_unlock(&(DeviceList->Mutex));
   }

   return(ret_val);
}


   /* The following function is used to remove devices from a           */
   /* DeviceList_t list. The function returns true if a matching device */
   /* is found and removed, or false if no matching device is found. If */
   /* the caller provides the address of a pointer (third parameter), it*/
   /* will be set to the address of the device's ExtraData (or NULL if  */
   /* no ExtraData was provided when the device was added). Note that   */
   /* this function will NOT free any memory. If the ExtraData refers   */
   /* to heap-allocated storage, it is the caller's responsibility to   */
   /* retrieve the ExtraData and free it. DeviceInListHasExtraData() can*/
   /* be used to determine whether an ExtraData value is available for a*/
   /* given device.                                                     */
bool RemoveDeviceFromList(DeviceList_t *DeviceList, BD_ADDR_t BD_ADDR, Boolean_t LowEnergy, void **ExtraData)
{
   bool ret_val;
   unsigned int i;

   ret_val = false;

   if(DeviceList)
   {
      pthread_mutex_lock(&(DeviceList->Mutex));

      if(DeviceList->Devices)
      {
         for(i = 0; i < DeviceList->Length; i++)
         {
            if((COMPARE_BD_ADDR(BD_ADDR, DeviceList->Devices[i].BD_ADDR)) && ((LowEnergy ? TRUE : FALSE) == DeviceList->Devices[i].LowEnergy))
            {
               /* Return the device's ExtraData, if the caller provided */
               /* a location for the address.                           */
               if(ExtraData)
                  *ExtraData = DeviceList->Devices[i].ExtraData;

               DeviceList->Devices[i].ExtraData = NULL;

               /* If the removed device wasn't the last item in the     */
               /* list, copy over the removed device with the last      */
               /* element.                                              */
               DeviceList->Length -= 1;

               if(DeviceList->Length > i)
                  DeviceList->Devices[i] = DeviceList->Devices[DeviceList->Length];

               ret_val = true;

/* XXX DEBUG */SS1_LOGI("Device removed. List contents (%u/%u):", DeviceList->Length, DeviceList->Capacity);
/* XXX DEBUG */for(i = 0; i < DeviceList->Length; i++) { char bd[32]; BD_ADDRToStr(bd, sizeof(bd), DeviceList->Devices[i].BD_ADDR); SS1_LOGI("   -- %s/%c (%p)", bd, (DeviceList->Devices[i].LowEnergy ? 'L' : 'C'), DeviceList->Devices[i].ExtraData); }

               /* Exit the loop.                                        */
               i = DeviceList->Length;
            }
         }
      }

      pthread_mutex_unlock(&(DeviceList->Mutex));
   }

   return(ret_val);
}


   /* The following function is used to remove devices from a           */
   /* DeviceList_t list, using the ExtraData field as the lookup key.   */
   /* The function returns true if a matching device is found and       */
   /* removed, or false if no matching device is found. If the caller   */
   /* provides a pointer to a BD_ADDR_t structure (third parameter),    */
   /* it will copy the BD_ADDR of the device to that location. If more  */
   /* than one device has the same ExtraData value, only one device     */
   /* is removed from the list and it is undefined which device is      */
   /* removed. If the caller is searching for a specific device, the    */
   /* caller should call this function repeatedly, caching the returned */
   /* BD_ADDRs, until either the target is found or no matching devices */
   /* remain, then push all the unwanted BD_ADDRs back onto the list.   */
   /* Note that this use-case is NOT thread-safe.                       */
bool RemoveDeviceFromListByExtraData(DeviceList_t *DeviceList, void *ExtraData, BD_ADDR_t *BD_ADDR, Boolean_t *LowEnergy)
{
   bool ret_val;
   unsigned int i;

   ret_val = false;

   if(DeviceList)
   {
      pthread_mutex_lock(&(DeviceList->Mutex));

      if(DeviceList->Devices)
      {
         for(i = 0; i < DeviceList->Length; i++)
         {
            if(ExtraData == DeviceList->Devices[i].ExtraData)
            {
               /* Return the device's BD_ADDR, if the caller provided a */
               /* location for it.                                      */
               if(BD_ADDR)
                  *BD_ADDR = DeviceList->Devices[i].BD_ADDR;

               if(LowEnergy)
                  *LowEnergy = DeviceList->Devices[i].LowEnergy;

               DeviceList->Devices[i].ExtraData = NULL;

               /* If the removed device wasn't the last item in the     */
               /* list, copy over the removed device with the last      */
               /* element.                                              */
               DeviceList->Length -= 1;

               if(DeviceList->Length > i)
                  DeviceList->Devices[i] = DeviceList->Devices[DeviceList->Length];

               ret_val = true;

/* XXX DEBUG */SS1_LOGI("Device removed. List contents (%u/%u):", DeviceList->Length, DeviceList->Capacity);
/* XXX DEBUG */for(i = 0; i < DeviceList->Length; i++) { char bd[32]; BD_ADDRToStr(bd, sizeof(bd), DeviceList->Devices[i].BD_ADDR); SS1_LOGI("   -- %s/%c (%p)", bd, (DeviceList->Devices[i].LowEnergy ? 'L' : 'C'), DeviceList->Devices[i].ExtraData); }

               /* Exit the loop.                                        */
               i = DeviceList->Length;
            }
         }
      }

      pthread_mutex_unlock(&(DeviceList->Mutex));
   }

   return(ret_val);
}


   /* The following function is used to remove a device from a          */
   /* DeviceList_t list. The function returns true if a device exists   */
   /* in the list and is removed, or false if the list is empty. If the */
   /* second and/or third parameters are non-NULL, the appropriate data */
   /* (BD_ADDR and ExtraData, respectively) will be copied into the     */
   /* provided buffers. Note that order is not a property of the list,  */
   /* so there is no implicit relationship between insertion order and  */
   /* removal order of devices.                                         */
bool RemoveDeviceFromListAny(DeviceList_t *DeviceList, BD_ADDR_t *BD_ADDR, Boolean_t *LowEnergy, void **ExtraData)
{
   bool ret_val;

   ret_val = false;

   if(DeviceList)
   {
      pthread_mutex_lock(&(DeviceList->Mutex));

      if(DeviceList->Devices)
      {
         if(DeviceList->Length > 0)
         {
            DeviceList->Length -= 1;

            if(BD_ADDR)
               *BD_ADDR = DeviceList->Devices[DeviceList->Length].BD_ADDR;

            if(LowEnergy)
               *LowEnergy = DeviceList->Devices[DeviceList->Length].LowEnergy;

            if(ExtraData)
               *ExtraData = DeviceList->Devices[DeviceList->Length].ExtraData;

/* XXX DEBUG */SS1_LOGI("Device removed. List contents (%u/%u):", DeviceList->Length, DeviceList->Capacity);
/* XXX DEBUG */{int i; for(i = 0; i < DeviceList->Length; i++) { char bd[32]; BD_ADDRToStr(bd, sizeof(bd), DeviceList->Devices[i].BD_ADDR); SS1_LOGI("   -- %s/%c (%p)", bd, (DeviceList->Devices[i].LowEnergy ? 'L' : 'C'), DeviceList->Devices[i].ExtraData); }}

            ret_val = true;
         }
      }

      pthread_mutex_unlock(&(DeviceList->Mutex));
   }

   return(ret_val);
}


bool DeviceInListHasExtraData(DeviceList_t *DeviceList, BD_ADDR_t BD_ADDR, Boolean_t LowEnergy)
{
   bool ret_val;
   unsigned int i;

   ret_val = false;

   if(DeviceList)
   {
      pthread_mutex_lock(&(DeviceList->Mutex));

      if(DeviceList->Devices)
      {
         for(i = 0; i < DeviceList->Length; i++)
         {
            if((COMPARE_BD_ADDR(BD_ADDR, DeviceList->Devices[i].BD_ADDR)) && ((LowEnergy ? TRUE : FALSE) == DeviceList->Devices[i].LowEnergy))
            {
               if(DeviceList->Devices[i].ExtraData != NULL)
                  ret_val = true;

               /* Exit the loop.                                        */
               i = DeviceList->Length;
            }
         }
      }

      pthread_mutex_unlock(&(DeviceList->Mutex));
   }

   return(ret_val);
}


void DebugDumpStringArray(JNIEnv *Env, jobjectArray StringArray, const char *FnName)
{
   if(!FnName)
      FnName = "DEBUG";

   if(StringArray)
   {
      SS1_LOGI("%s: Array Has... %d items", FnName, Env->GetArrayLength(StringArray));
      for(int i = 0; i < Env->GetArrayLength(StringArray); i++)
      {
         jstring jstr = (jstring)Env->GetObjectArrayElement(StringArray, i);
         if(jstr)
         {
            const char *cstr = Env->GetStringUTFChars(jstr, NULL);
            if(cstr)
            {
               SS1_LOGI("%s:\t%s", FnName, cstr);
               Env->ReleaseStringUTFChars(jstr, cstr);
            }
            else
               SS1_LOGI("%s:   --NO MEM--", FnName);
            Env->DeleteLocalRef(jstr);
         }
         else
            SS1_LOGI("%s:   --NULL--", FnName);
      }
   }
   else
      SS1_LOGI("%s: Array Has... NULL", FnName);
}

   /* Compare the UUID cached by android and the UUID in the platform   */
   /* manager's remote device properties. If there are any differences  */
   /* return a jni false. Otherwise,  return jni true.                  */
jboolean IdenticalUuid(JNIEnv *Env, jobjectArray SinglePropertyArray, jstring JavaStringDelimitedUuid)
{
   jboolean      UuidMatch;
   jstring       JavaStringKey;
   jstring       JavaStringUuid;
   const char   *StringKey;
   const char   *StringUuid;
   const char   *StringDelimitedUuid;
   char         *StringToken;
   jsize         SinglePropertyLength = 0;
   jsize         i;
   unsigned int  TestedUuid;
   unsigned int  MatchedUuid;

   /* Presume that the UUID do not match.                               */
   UuidMatch = JNI_FALSE;

   /* Check if the parameters are valid.                                */
   if((SinglePropertyArray) && (JavaStringDelimitedUuid))
   {
      /* Get the key value in the single property array.                */
      if((JavaStringKey = (jstring)(Env->GetObjectArrayElement(SinglePropertyArray, 0))) != NULL)
      {
         /* Convert the jstring key value to a c character pointer.     */
         if((StringKey = Env->GetStringUTFChars(JavaStringKey, NULL)) != NULL)
         {
            /*If the key is UUIDs, then proceed.                        */
            if(strncmp(StringKey, "UUIDs", Env->GetStringUTFLength(JavaStringKey) + 1) == 0)
            {
               /* Convert the delimited jstring value to a c character  */
               /* pointer.                                              */
               if((StringDelimitedUuid = Env->GetStringUTFChars(JavaStringDelimitedUuid, NULL)) != NULL)
               {
                  StringToken  = (char *)StringDelimitedUuid;
                  TestedUuid   = 0;                  
                  MatchedUuid  = 0;
 
                  SS1_LOGD("Delimited UUID String:\t%s", StringDelimitedUuid);
                  SS1_LOGD("Starting Token:\t%s", StringToken);

                  while((StringToken = strtok(StringToken,",")) != NULL)
                  {
                     SS1_LOGD("Token:\t%s %d", StringToken, TestedUuid);
                     SinglePropertyLength = Env->GetArrayLength(SinglePropertyArray);
                     for(i = 2; i < SinglePropertyLength; i++) 
                     {
                        if((JavaStringUuid = (jstring)(Env->GetObjectArrayElement(SinglePropertyArray, i))) != NULL)
                        {
                           if((StringUuid = Env->GetStringUTFChars(JavaStringUuid, NULL)) != NULL)
                           {
                              if(strncmp(StringUuid, StringToken, Env->GetStringUTFLength(JavaStringUuid) + 1) == 0)
                              {
                                 MatchedUuid++;
                                 SS1_LOGD("UUID:\t%s Matches: %d", StringUuid, MatchedUuid);
                                 Env->ReleaseStringUTFChars(JavaStringUuid, StringUuid);
                                 break;
                              }
                              else
                              {
                                 SS1_LOGD("UUID:\t%s Matches: %d", StringUuid, MatchedUuid);
                                 Env->ReleaseStringUTFChars(JavaStringUuid, StringUuid);
                              }
                           }   
                        } 
                     }

                     TestedUuid++;                  
                     if(MatchedUuid != TestedUuid)
                        break;
                  
                     StringToken = NULL;
                  }

                  if((MatchedUuid == TestedUuid) && (MatchedUuid == (((unsigned int)SinglePropertyLength) - 2)))
                     UuidMatch = JNI_TRUE;   

                  Env->ReleaseStringUTFChars(JavaStringDelimitedUuid, StringDelimitedUuid);
               }
            }
         
            Env->ReleaseStringUTFChars(JavaStringKey, StringKey);
         } 
      }
   }  

   return UuidMatch;
}

#endif /* HAVE_BLUETOOTH */

} /* namespace android */
