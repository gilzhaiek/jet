/*****< com_stonestreetone_bluetopiapm_AVRCP.cpp >*****************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_AVRCP - JNI Module for Stonestreet One     */
/*                                         Bluetopia Platform Manager AVRCP   */
/*                                         Java API.                          */
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
#include "com_stonestreetone_bluetopiapm_AVRCP.h"

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
static WEAK_REF Class_AVRCP;

static jfieldID Field_AVRCP_localData;

   /* Java event handlers.                                              */
static jmethodID Method_AVRCP_remoteControlConnectionRequestEvent;
static jmethodID Method_AVRCP_remoteControlConnectedEvent;
static jmethodID Method_AVRCP_remoteControlConnectionStatusEvent;
static jmethodID Method_AVRCP_remoteControlDisconnectedEvent;
static jmethodID Method_AVRCP_remoteControlPassThroughCommandConfirmationEvent;
static jmethodID Method_AVRCP_vendorDependentCommandConfirmationEvent;
static jmethodID Method_AVRCP_groupNavigationCommandConfirmationEvent;
static jmethodID Method_AVRCP_getCompanyIDCapabilitiesCommandConfirmationEvent;
static jmethodID Method_AVRCP_getEventsSupportedCapabilitiesCommandConfirmationEvent;
static jmethodID Method_AVRCP_listPlayerApplicationSettingAttributesCommandConfirmationEvent;
static jmethodID Method_AVRCP_listPlayerApplicationSettingValuesCommandConfirmationEvent;
static jmethodID Method_AVRCP_getCurrentPlayerApplicationSettingValueCommandConfirmationEvent;
static jmethodID Method_AVRCP_setPlayerApplicationSettingValueCommandConfirmationEvent;
static jmethodID Method_AVRCP_getPlayerApplicationSettingAttributeTextCommandConfirmationEvent;
static jmethodID Method_AVRCP_getPlayerApplicationSettingValueTextCommandConfirmationEvent;
static jmethodID Method_AVRCP_informDisplayableCharacterSetCommandConfirmationEvent;
static jmethodID Method_AVRCP_informBatteryStatusCommandConfirmationEvent;
static jmethodID Method_AVRCP_getElementAttributesCommandConfirmationEvent;
static jmethodID Method_AVRCP_getPlayStatusCommandConfirmationEvent;
static jmethodID Method_AVRCP_playbackStatusChangedNotificationEvent;
static jmethodID Method_AVRCP_trackChangedNotificationEvent;
static jmethodID Method_AVRCP_trackReachedEndNotificationEvent;
static jmethodID Method_AVRCP_trackReachedStartNotificationEvent;
static jmethodID Method_AVRCP_playbackPositionChangedNotificationEvent;
static jmethodID Method_AVRCP_batteryStatusChangedNotificationEvent;
static jmethodID Method_AVRCP_systemStatusChangedNotificationEvent;
static jmethodID Method_AVRCP_playerApplicationSettingChangedNotificationEvent; 
static jmethodID Method_AVRCP_volumeChangeNotificationEvent;
static jmethodID Method_AVRCP_setAbsoluteVolumeCommandConfirmationEvent;
static jmethodID Method_AVRCP_commandRejectResponseEvent;
static jmethodID Method_AVRCP_commandFailureEvent;

typedef struct _tagLocalData_t
{
   WEAK_REF     WeakObjectReference;
   unsigned int CallbackID;
} LocalData_t;

/* Internal function prototypes.                                        */
static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive);
static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData);
static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData);

static void ProcessPassThroughResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Pass_Through_Response_Data_t *ResponseData);
static void ProcessVendorDependentResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Vendor_Dependent_Generic_Response_Data_t *ResponseData);
static void ProcessGroupNavigationResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Group_Navigation_Response_Data_t *ResponseData);
static void ProcessGetCapabilitiesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Capabilities_Response_Data_t *ResponseData);
static void ProcessListPlayerApplicationSettingAttributesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_List_Player_Application_Setting_Attributes_Response_Data_t *ResponseData);
static void ProcessListPlayerApplicationSettingValuesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_List_Player_Application_Setting_Values_Response_Data_t *ResponseData);
static void ProcessGetCurrentPlayerApplicationSettingValueResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Current_Player_Application_Setting_Value_Response_Data_t *ResponseData);
static void ProcessSetPlayerApplicationSettingValueResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Set_Player_Application_Setting_Value_Response_Data_t *ResponseData);
static void ProcessGetPlayerApplicationSettingAttributeTextResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Player_Application_Setting_Attribute_Text_Response_Data_t *ResponseData);
static void ProcessGetPlayerApplicationSettingValueTextResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Player_Application_Setting_Value_Text_Response_Data_t *ResponseData);
static void ProcessInformDisplayableCharacterSetResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Inform_Displayable_Character_Set_Response_Data_t *ResponseData);
static void ProcessInformBatteryStatusResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Inform_Battery_Status_Of_CT_Response_Data_t *ResponseData);
static void ProcessGetElementAttributesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Element_Attributes_Response_Data_t *ResponseData);
static void ProcessGetPlayStatusResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Play_Status_Response_Data_t *ResponseData);
static void ProcessRegisterNotificationResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Register_Notification_Response_Data_t *ResponseData);
static void ProcessSetAbsoluteVolumeResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Set_Absolute_Volume_Response_Data_t *ResponseData);
static void ProcessCommandRejectResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Command_Reject_Response_Data_t *ResponseData);

static void ProcessRemoteControlCommandConfirmation(JNIEnv *Env, jobject Object, AUDM_Remote_Control_Command_Confirmation_Event_Data_t *CommandConfirmationData);

static void AVRCP_AUDM_Event_Callback(AUDM_Event_Data_t *EventData, void *CallbackParameter);

static LocalData_t *AcquireLocalData(JNIEnv *Env, jobject Object, Boolean_t Exclusive)
{
   LocalData_t *LocalData;

   if((LocalData = (LocalData_t *)AcquireReferenceCountedField(Env, Object, Field_AVRCP_localData, Exclusive)) == NULL)
      PRINT_ERROR("AVRCP: Unable to obtain native data structure for Manager object");

   return(LocalData);
}

static void ReleaseLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   ReleaseReferenceCountedField(Env, Object, Field_AVRCP_localData, LocalData);
}

static void SetLocalData(JNIEnv *Env, jobject Object, LocalData_t *LocalData)
{
   SetReferenceCountedField(Env, Object, Field_AVRCP_localData, LocalData);
}

static void ProcessPassThroughResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Pass_Through_Response_Data_t *ResponseData)
{
   jint       OperationID;
   Boolean_t  Valid = TRUE;
   jbyteArray ByteArray;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      switch(ResponseData->OperationID)
      {
         case AVRCP_PASS_THROUGH_ID_SELECT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SELECT;
            break;
         case AVRCP_PASS_THROUGH_ID_UP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_UP;
            break;
         case AVRCP_PASS_THROUGH_ID_DOWN:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_DOWN;
            break;
         case AVRCP_PASS_THROUGH_ID_LEFT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_LEFT;
            break;
         case AVRCP_PASS_THROUGH_ID_RIGHT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RIGHT;
            break;
         case AVRCP_PASS_THROUGH_ID_RIGHT_UP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RIGHT_UP;
            break;
         case AVRCP_PASS_THROUGH_ID_RIGHT_DOWN:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
            break;
         case AVRCP_PASS_THROUGH_ID_LEFT_UP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_LEFT_UP;
            break;
         case AVRCP_PASS_THROUGH_ID_LEFT_DOWN:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
            break;
         case AVRCP_PASS_THROUGH_ID_ROOT_MENU:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_ROOT_MENU;
            break;
         case AVRCP_PASS_THROUGH_ID_SETUP_MENU:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SETUP_MENU;
            break;
         case AVRCP_PASS_THROUGH_ID_CONTENTS_MENU:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
            break;
         case AVRCP_PASS_THROUGH_ID_FAVORITE_MENU:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
            break;
         case AVRCP_PASS_THROUGH_ID_EXIT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_EXIT;
            break;
         case AVRCP_PASS_THROUGH_ID_0:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_0;
            break;
         case AVRCP_PASS_THROUGH_ID_1:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_1;
            break;
         case AVRCP_PASS_THROUGH_ID_2:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_2;
            break;
         case AVRCP_PASS_THROUGH_ID_3:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_3;
            break;
         case AVRCP_PASS_THROUGH_ID_4:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_4;
            break;
         case AVRCP_PASS_THROUGH_ID_5:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_5;
            break;
         case AVRCP_PASS_THROUGH_ID_6:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_6;
            break;
         case AVRCP_PASS_THROUGH_ID_7:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_7;
            break;
         case AVRCP_PASS_THROUGH_ID_8:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_8;
            break;
         case AVRCP_PASS_THROUGH_ID_9:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_9;
            break;
         case AVRCP_PASS_THROUGH_ID_DOT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_DOT;
            break;
         case AVRCP_PASS_THROUGH_ID_ENTER:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_ENTER;
            break;
         case AVRCP_PASS_THROUGH_ID_CLEAR:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CLEAR;
            break;
         case AVRCP_PASS_THROUGH_ID_CHANNEL_UP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
            break;
         case AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
            break;
         case AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
            break;
         case AVRCP_PASS_THROUGH_ID_SOUND_SELECT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
            break;
         case AVRCP_PASS_THROUGH_ID_INPUT_SELECT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
            break;
         case AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
            break;
         case AVRCP_PASS_THROUGH_ID_HELP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_HELP;
            break;
         case AVRCP_PASS_THROUGH_ID_PAGE_UP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PAGE_UP;
            break;
         case AVRCP_PASS_THROUGH_ID_PAGE_DOWN:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
            break;
         case AVRCP_PASS_THROUGH_ID_POWER:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_POWER;
            break;
         case AVRCP_PASS_THROUGH_ID_VOLUME_UP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_VOLUME_UP;
            break;
         case AVRCP_PASS_THROUGH_ID_VOLUME_DOWN:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
            break;
         case AVRCP_PASS_THROUGH_ID_MUTE:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_MUTE;
            break;
         case AVRCP_PASS_THROUGH_ID_PLAY:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PLAY;
            break;
         case AVRCP_PASS_THROUGH_ID_STOP:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_STOP;
            break;
         case AVRCP_PASS_THROUGH_ID_PAUSE:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PAUSE;
            break;
         case AVRCP_PASS_THROUGH_ID_RECORD:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RECORD;
            break;
         case AVRCP_PASS_THROUGH_ID_REWIND:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_REWIND;
            break;
         case AVRCP_PASS_THROUGH_ID_FAST_FORWARD:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
            break;
         case AVRCP_PASS_THROUGH_ID_EJECT:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_EJECT;
            break;
         case AVRCP_PASS_THROUGH_ID_FORWARD:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_FORWARD;
            break;
         case AVRCP_PASS_THROUGH_ID_BACKWARD:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_BACKWARD;
            break;
         case AVRCP_PASS_THROUGH_ID_ANGLE:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_ANGLE;
            break;
         case AVRCP_PASS_THROUGH_ID_SUBPICTURE:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SUBPICTURE;
            break;
         case AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE:
            OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
            break;
         default:
            /* Workaround the fact that the  */
            /* following Passthrough IDs were*/
            /* all defined to (0xFF, invalid)*/
            /* at the time of this writing,  */
            /* so we cannot include them in  */
            /* the switch block above.       */
            if(AVRCP_PASS_THROUGH_ID_F1 == ResponseData->OperationID)
               OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F1;
            else
            {
               if(AVRCP_PASS_THROUGH_ID_F2 == ResponseData->OperationID)
                  OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F2;
               else
               {
                  if(AVRCP_PASS_THROUGH_ID_F3 == ResponseData->OperationID)
                     OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F3;
                  else
                  {
                     if(AVRCP_PASS_THROUGH_ID_F4 == ResponseData->OperationID)
                        OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F4;
                     else
                     {
                        if(AVRCP_PASS_THROUGH_ID_F5 == ResponseData->OperationID)
                           OperationID = com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F5;
                        else
                        {
                           OperationID = 0;
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
         if((ByteArray = Env->NewByteArray(ResponseData->OperationDataLength)) != NULL)
         {
            Env->SetByteArrayRegion(ByteArray, 0, (jsize)(ResponseData->OperationDataLength), (jbyte *)(ResponseData->OperationData));

            Env->CallVoidMethod(Object, Method_AVRCP_remoteControlPassThroughCommandConfirmationEvent,
                  BD_ADDR.BD_ADDR5,
                  BD_ADDR.BD_ADDR4,
                  BD_ADDR.BD_ADDR3,
                  BD_ADDR.BD_ADDR2,
                  BD_ADDR.BD_ADDR1,
                  BD_ADDR.BD_ADDR0,
                  TransactionID,
                  ResponseData->ResponseCode,
                  ResponseData->SubunitType,
                  ResponseData->SubunitID,
                  OperationID,
                  ((ResponseData->StateFlag == FALSE) ? JNI_FALSE : JNI_TRUE),
                  ByteArray);

            Env->DeleteLocalRef(ByteArray);
         }
         else
            Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
      }
   }
}

static void ProcessVendorDependentResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Vendor_Dependent_Generic_Response_Data_t *ResponseData)
{
   jbyteArray ByteArray;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      if((ByteArray = Env->NewByteArray(ResponseData->DataLength)) != NULL)
      {
         Env->SetByteArrayRegion(ByteArray, 0, (jsize)(ResponseData->DataLength), (jbyte *)(ResponseData->DataBuffer));

         Env->CallVoidMethod(Object, Method_AVRCP_vendorDependentCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode,
               ResponseData->SubunitType,
               ResponseData->SubunitID,
               ResponseData->CompanyID.CompanyID0,
               ResponseData->CompanyID.CompanyID1,
               ResponseData->CompanyID.CompanyID2,
               ByteArray);

         Env->DeleteLocalRef(ByteArray);
      }
      else
         Env->ThrowNew(Env->FindClass("java/lang/OutOfMemoryError"), NULL);
   }


}

static void ProcessGroupNavigationResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Group_Navigation_Response_Data_t *ResponseData)
{
   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      Env->CallVoidMethod(Object, Method_AVRCP_groupNavigationCommandConfirmationEvent,
               BD_ADDR.BD_ADDR0,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR5,
               TransactionID,
               ResponseData->ResponseCode,
               (ResponseData->ButtonState == bsPressed)?JNI_TRUE:JNI_FALSE);
   }
}

static void ProcessGetCapabilitiesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Capabilities_Response_Data_t *ResponseData)
{
   jint          EventMask;          
   jbyte        *ArrayElements;
   unsigned int  Index;
   jbyteArray    ByteArray;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      switch(ResponseData->CapabilityID)
      {
         case AVRCP_GET_CAPABILITIES_CAPABILITY_ID_COMPANY_ID:
            if((ByteArray = Env->NewByteArray(ResponseData->NumberCapabilities*3)) != NULL)
            {
               if((ArrayElements = Env->GetByteArrayElements(ByteArray, JNI_FALSE)) != NULL)
               {
                  for(Index=0; Index<ResponseData->NumberCapabilities; Index++)
                  {
                     ArrayElements[Index*3] = ResponseData->CapabilityInfoList[Index].CapabilityInfo.CompanyID.CompanyID0;
                     ArrayElements[Index*3+1] = ResponseData->CapabilityInfoList[Index].CapabilityInfo.CompanyID.CompanyID1;
                     ArrayElements[Index*3+2] = ResponseData->CapabilityInfoList[Index].CapabilityInfo.CompanyID.CompanyID2;
                  }

                  Env->ReleaseByteArrayElements(ByteArray, ArrayElements, 0);

                  Env->CallVoidMethod(Object, Method_AVRCP_getCompanyIDCapabilitiesCommandConfirmationEvent,
                           BD_ADDR.BD_ADDR5,
                           BD_ADDR.BD_ADDR4,
                           BD_ADDR.BD_ADDR3,
                           BD_ADDR.BD_ADDR2,
                           BD_ADDR.BD_ADDR1,
                           BD_ADDR.BD_ADDR0,
                           TransactionID,
                           ResponseData->ResponseCode,
                           ByteArray);
                           
               }

               Env->DeleteLocalRef(ByteArray);
            }
            break;
         case AVRCP_GET_CAPABILITIES_CAPABILITY_ID_EVENTS_SUPPORTED:
            EventMask = 0;

            for(Index=0; Index<ResponseData->NumberCapabilities; Index++)
            {
               switch(ResponseData->CapabilityInfoList[Index].CapabilityInfo.EventID)
               {
                  case AVRCP_EVENT_PLAYBACK_STATUS_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_PLAYBACK_STATUS_CHANGED;
                     break;
                  case AVRCP_EVENT_TRACK_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_TRACK_CHANGED;
                     break;
                  case AVRCP_EVENT_TRACK_REACHED_END:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_TRACK_REACHED_END;
                     break;
                  case AVRCP_EVENT_TRACK_REACHED_START:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_TRACK_REACHED_START;
                     break;
                  case AVRCP_EVENT_PLAYBACK_POS_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_PLAYBACK_POSITION_CHANGED;
                     break;
                  case AVRCP_EVENT_BATT_STATUS_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_BATTERY_STATUS_CHANGED;
                     break;
                  case AVRCP_EVENT_SYSTEM_STATUS_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_SYSTEM_STATUS_CHANGED;
                     break;
                  case AVRCP_EVENT_PLAYER_APPLICATION_SETTING_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_PLAYER_APPLICATION_SETTING_CHANGED;
                     break;
                  case AVRCP_EVENT_NOW_PLAYING_CONTENT_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_NOW_PLAYING_CONTENT_CHANGED;
                     break;
                  case AVRCP_EVENT_AVAILABLE_PLAYERS_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_AVAILABLE_PLAYERS_CHANGED;
                     break;
                  case AVRCP_EVENT_ADDRESSED_PLAYER_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_ADDRESSED_PLAYER_CHANGED;
                     break;
                  case AVRCP_EVENT_UIDS_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_UIDS_CHANGED;
                     break;
                  case AVRCP_EVENT_VOLUME_CHANGED:
                     EventMask |= com_stonestreetone_bluetopiapm_AVRCP_EventID_BIT_VOLUME_CHANGED;
                     break;
               }
            }

            Env->CallVoidMethod(Object, Method_AVRCP_getEventsSupportedCapabilitiesCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode,
               EventMask);

            break;
         default:
            PRINT_DEBUG("Unrecognized Capability ID");
      }
   }
}

static void ProcessListPlayerApplicationSettingAttributesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_List_Player_Application_Setting_Attributes_Response_Data_t *ResponseData)
{
   jbyteArray AttributeArray;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      if((AttributeArray = Env->NewByteArray(ResponseData->NumberAttributes)) != NULL)
      {
         Env->SetByteArrayRegion(AttributeArray, 0, ResponseData->NumberAttributes, (const jbyte *)ResponseData->AttributeIDList);

         Env->CallVoidMethod(Object, Method_AVRCP_listPlayerApplicationSettingAttributesCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode,
               AttributeArray);

         Env->DeleteLocalRef(AttributeArray);
      }
   }
}

static void ProcessListPlayerApplicationSettingValuesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_List_Player_Application_Setting_Values_Response_Data_t *ResponseData)
{
   jbyteArray ValueArray;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      if((ValueArray = Env->NewByteArray(ResponseData->NumberValueIDs)) != NULL)
      {
         Env->SetByteArrayRegion(ValueArray, 0, ResponseData->NumberValueIDs, (const jbyte *)ResponseData->ValueIDList);

         Env->CallVoidMethod(Object, Method_AVRCP_listPlayerApplicationSettingValuesCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode,
               ValueArray);

         Env->DeleteLocalRef(ValueArray);
      }
   }
}

static void ProcessGetCurrentPlayerApplicationSettingValueResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Current_Player_Application_Setting_Value_Response_Data_t *ResponseData)
{
   jbyteArray   IDArray;
   jbyteArray   ValueArray;
   unsigned int Index;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      if((IDArray = Env->NewByteArray(ResponseData->NumberAttributeValueIDs)) != NULL)
      {
         if((ValueArray = Env->NewByteArray(ResponseData->NumberAttributeValueIDs)) != NULL)
         {
            for(Index=0; Index<ResponseData->NumberAttributeValueIDs; Index++)
            {
               Env->SetByteArrayRegion(IDArray, Index, 1, (const jbyte *)&(ResponseData->AttributeValueIDList[Index].AttributeID));
               Env->SetByteArrayRegion(ValueArray, Index, 1, (const jbyte *)&(ResponseData->AttributeValueIDList[Index].ValueID));
            }

            Env->CallVoidMethod(Object, Method_AVRCP_getCurrentPlayerApplicationSettingValueCommandConfirmationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode,
                     IDArray,
                     ValueArray);

            Env->DeleteLocalRef(ValueArray);
         }
         
         Env->DeleteLocalRef(IDArray);
      }
   }
}

static void ProcessSetPlayerApplicationSettingValueResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Set_Player_Application_Setting_Value_Response_Data_t *ResponseData)
{
   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      Env->CallVoidMethod(Object, Method_AVRCP_setPlayerApplicationSettingValueCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode);
   }
}

static void ProcessGetPlayerApplicationSettingAttributeTextResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Player_Application_Setting_Attribute_Text_Response_Data_t *ResponseData)
{
   jclass       ByteArrayClazz;
   Boolean_t    Error;
   jbyteArray   IDArray;
   jbyteArray   TextData;
   jshortArray  CharsetArray;
   jobjectArray TextDataArray;
   unsigned int Index;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      if((ByteArrayClazz = Env->FindClass("[B")) != NULL)
      {
         if((IDArray = Env->NewByteArray(ResponseData->NumberAttributeTextEntries)) != NULL)
         {
            if((CharsetArray = Env->NewShortArray(ResponseData->NumberAttributeTextEntries)) != NULL)
            {
               if((TextDataArray = Env->NewObjectArray(ResponseData->NumberAttributeTextEntries, ByteArrayClazz, NULL)) != NULL)
               {
                  Error = FALSE;

                  for(Index=0; Index<ResponseData->NumberAttributeTextEntries && !Error; Index++)
                  {
                     if((TextData = Env->NewByteArray(ResponseData->AttributeTextEntryList[Index].AttributeStringLength)) != NULL)
                     {
                        Env->SetByteArrayRegion(IDArray, Index, 1, (const jbyte *)&(ResponseData->AttributeTextEntryList[Index].AttributeID));
                        Env->SetShortArrayRegion(CharsetArray, Index, 1, (const jshort *)&(ResponseData->AttributeTextEntryList[Index].CharacterSet));

                        Env->SetByteArrayRegion(TextData, 0, ResponseData->AttributeTextEntryList[Index].AttributeStringLength, (const jbyte *)ResponseData->AttributeTextEntryList[Index].AttributeStringData);
                        Env->SetObjectArrayElement(TextDataArray, Index, (jobject)TextData);

                        Env->DeleteLocalRef(TextData);
                     }
                     else
                        Error = TRUE;
                  }

                  if(!Error)
                  {
                     Env->CallVoidMethod(Object, Method_AVRCP_getPlayerApplicationSettingAttributeTextCommandConfirmationEvent,
                              BD_ADDR.BD_ADDR5,
                              BD_ADDR.BD_ADDR4,
                              BD_ADDR.BD_ADDR3,
                              BD_ADDR.BD_ADDR2,
                              BD_ADDR.BD_ADDR1,
                              BD_ADDR.BD_ADDR0,
                              TransactionID,
                              ResponseData->ResponseCode,
                              IDArray,
                              CharsetArray,
                              TextDataArray);
                  }

                  Env->DeleteLocalRef(TextDataArray);
               }

               Env->DeleteLocalRef(CharsetArray);
            }

            Env->DeleteLocalRef(IDArray);
         }
      }
   }
}

static void ProcessGetPlayerApplicationSettingValueTextResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Player_Application_Setting_Value_Text_Response_Data_t *ResponseData)
{
   jclass       ByteArrayClazz;
   Boolean_t    Error;
   jbyteArray   IDArray;
   jbyteArray   TextData;
   jshortArray  CharsetArray;
   jobjectArray TextDataArray;
   unsigned int Index;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      if((ByteArrayClazz = Env->FindClass("[B")) != NULL)
      {
         if((IDArray = Env->NewByteArray(ResponseData->NumberValueTextEntries)) != NULL)
         {
            if((CharsetArray = Env->NewShortArray(ResponseData->NumberValueTextEntries)) != NULL)
            {
               if((TextDataArray = Env->NewObjectArray(ResponseData->NumberValueTextEntries, ByteArrayClazz, NULL)) != NULL)
               {
                  Error = FALSE;

                  for(Index=0; Index<ResponseData->NumberValueTextEntries && !Error; Index++)
                  {
                     if((TextData = Env->NewByteArray(ResponseData->ValueTextEntryList[Index].ValueStringLength)) != NULL)
                     {
                        Env->SetByteArrayRegion(IDArray, Index, 1, (const jbyte *)&(ResponseData->ValueTextEntryList[Index].ValueID));
                        Env->SetShortArrayRegion(CharsetArray, Index, 1, (const jshort *)&(ResponseData->ValueTextEntryList[Index].CharacterSet));

                        Env->SetByteArrayRegion(TextData, 0, ResponseData->ValueTextEntryList[Index].ValueStringLength, (const jbyte *)ResponseData->ValueTextEntryList[Index].ValueStringData);
                        Env->SetObjectArrayElement(TextDataArray, Index, (jobject)TextData);

                        Env->DeleteLocalRef(TextData);
                     }
                     else
                        Error = TRUE;
                  }

                  if(!Error)
                  {
                     Env->CallVoidMethod(Object, Method_AVRCP_getPlayerApplicationSettingValueTextCommandConfirmationEvent,
                              BD_ADDR.BD_ADDR5,
                              BD_ADDR.BD_ADDR4,
                              BD_ADDR.BD_ADDR3,
                              BD_ADDR.BD_ADDR2,
                              BD_ADDR.BD_ADDR1,
                              BD_ADDR.BD_ADDR0,
                              TransactionID,
                              ResponseData->ResponseCode,
                              IDArray,
                              CharsetArray,
                              TextDataArray);
                  }

                  Env->DeleteLocalRef(TextDataArray);
               }

               Env->DeleteLocalRef(CharsetArray);
            }

            Env->DeleteLocalRef(IDArray);
         }
      }
   }
}

static void ProcessInformDisplayableCharacterSetResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Inform_Displayable_Character_Set_Response_Data_t *ResponseData)
{
   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      Env->CallVoidMethod(Object, Method_AVRCP_informDisplayableCharacterSetCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode);
   }
}

static void ProcessInformBatteryStatusResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Inform_Battery_Status_Of_CT_Response_Data_t *ResponseData)
{
   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      Env->CallVoidMethod(Object, Method_AVRCP_informBatteryStatusCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode);
   }
}

static void ProcessGetElementAttributesResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Element_Attributes_Response_Data_t *ResponseData)
{
   jclass       ByteArrayClazz;
   Boolean_t    Error;
   jintArray    IDArray;
   jbyteArray   ElementData;
   jshortArray  CharsetArray;
   jobjectArray ElementDataArray;
   unsigned int Index;
   unsigned int Index2;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      if((ByteArrayClazz = Env->FindClass("[B")) != NULL)
      {
         if((IDArray = Env->NewIntArray(ResponseData->NumberAttributes)) != NULL)
         {
            if((CharsetArray = Env->NewShortArray(ResponseData->NumberAttributes)) != NULL)
            {
               if((ElementDataArray = Env->NewObjectArray(ResponseData->NumberAttributes, ByteArrayClazz, NULL)) != NULL)
               {
                  Error = FALSE;

                  for(Index=0; Index<ResponseData->NumberAttributes && !Error; Index++)
                  {
                     if((ElementData = Env->NewByteArray(ResponseData->AttributeList[Index].AttributeValueLength)) != NULL)
                     {
                        Env->SetIntArrayRegion(IDArray, Index, 1, (const jint *)&(ResponseData->AttributeList[Index].AttributeID));
                        Env->SetShortArrayRegion(CharsetArray, Index, 1, (const jshort *)&(ResponseData->AttributeList[Index].CharacterSet));

                        Env->SetByteArrayRegion(ElementData, 0, ResponseData->AttributeList[Index].AttributeValueLength, (const jbyte *)ResponseData->AttributeList[Index].AttributeValueData);
                        Env->SetObjectArrayElement(ElementDataArray, Index, ElementData);

                        Env->DeleteLocalRef(ElementData);
                     }
                     else
                        Error = TRUE;
                  }

                  if(!Error)
                  {
                     Env->CallVoidMethod(Object, Method_AVRCP_getElementAttributesCommandConfirmationEvent,
                              BD_ADDR.BD_ADDR5,
                              BD_ADDR.BD_ADDR4,
                              BD_ADDR.BD_ADDR3,
                              BD_ADDR.BD_ADDR2,
                              BD_ADDR.BD_ADDR1,
                              BD_ADDR.BD_ADDR0,
                              TransactionID,
                              ResponseData->ResponseCode,
                              IDArray,
                              CharsetArray,
                              ElementDataArray);
                  }

                  Env->DeleteLocalRef(ElementDataArray);
               }

               Env->DeleteLocalRef(CharsetArray);
            }

            Env->DeleteLocalRef(IDArray);
         }
      }
   }

}

static void ProcessGetPlayStatusResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Get_Play_Status_Response_Data_t *ResponseData)
{
   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      Env->CallVoidMethod(Object, Method_AVRCP_getPlayStatusCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode,
               ResponseData->SongLength,
               ResponseData->SongPosition,
               ResponseData->PlayStatus);
   }
}

static void ProcessRegisterNotificationResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Register_Notification_Response_Data_t *ResponseData)
{
   jbyteArray   IDArray;
   jbyteArray   ValueArray;
   unsigned int Index;

   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      switch(ResponseData->EventID)
      {
         case AVRCP_EVENT_PLAYBACK_STATUS_CHANGED:
            Env->CallVoidMethod(Object, Method_AVRCP_playbackStatusChangedNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode,
                     ResponseData->NotificationData.PlaybackStatusChangedData.PlayStatus);
            break;
         case AVRCP_EVENT_TRACK_CHANGED:
            Env->CallVoidMethod(Object, Method_AVRCP_trackChangedNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode,
                     ResponseData->NotificationData.TrackChangedData.Identifier);
            break;
         case AVRCP_EVENT_TRACK_REACHED_END:
            Env->CallVoidMethod(Object, Method_AVRCP_trackReachedEndNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode);
            break;
         case AVRCP_EVENT_TRACK_REACHED_START:
            Env->CallVoidMethod(Object, Method_AVRCP_trackReachedStartNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode);
            break;
         case AVRCP_EVENT_PLAYBACK_POS_CHANGED:
            Env->CallVoidMethod(Object, Method_AVRCP_playbackPositionChangedNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode,
                     ResponseData->NotificationData.PlaybackPosChangedData.PlaybackPosition);
            break;
         case AVRCP_EVENT_BATT_STATUS_CHANGED:
            Env->CallVoidMethod(Object, Method_AVRCP_batteryStatusChangedNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode,
                     ResponseData->NotificationData.BattStatusChangedData.BatteryStatus);
            break;
         case AVRCP_EVENT_SYSTEM_STATUS_CHANGED:
            Env->CallVoidMethod(Object, Method_AVRCP_systemStatusChangedNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode,
                     ResponseData->NotificationData.SystemStatusChangedData.SystemStatus);
            break;
         case AVRCP_EVENT_PLAYER_APPLICATION_SETTING_CHANGED:
            if((IDArray = Env->NewByteArray(ResponseData->NotificationData.PlayerApplicationSettingChangedData.NumberAttributeValueIDs)) != NULL)
            {
               if((ValueArray = Env->NewByteArray(ResponseData->NotificationData.PlayerApplicationSettingChangedData.NumberAttributeValueIDs)) != NULL)
               {
                  for(Index=0; Index<ResponseData->NotificationData.PlayerApplicationSettingChangedData.NumberAttributeValueIDs; Index++)
                  {
                     Env->SetByteArrayRegion(IDArray, Index, 1, (const jbyte *)&(ResponseData->NotificationData.PlayerApplicationSettingChangedData.AttributeValueIDList[Index].AttributeID));
                     Env->SetByteArrayRegion(ValueArray, Index, 1, (const jbyte *)&(ResponseData->NotificationData.PlayerApplicationSettingChangedData.AttributeValueIDList[Index].ValueID));
                  }

                  Env->CallVoidMethod(Object, Method_AVRCP_playerApplicationSettingChangedNotificationEvent,
                           BD_ADDR.BD_ADDR5,
                           BD_ADDR.BD_ADDR4,
                           BD_ADDR.BD_ADDR3,
                           BD_ADDR.BD_ADDR2,
                           BD_ADDR.BD_ADDR1,
                           BD_ADDR.BD_ADDR0,
                           TransactionID,
                           ResponseData->ResponseCode,
                           IDArray,
                           ValueArray);

                  Env->DeleteLocalRef(ValueArray);
               }
               
               Env->DeleteLocalRef(IDArray);
            }
            break;
         case AVRCP_EVENT_VOLUME_CHANGED:
            Env->CallVoidMethod(Object, Method_AVRCP_volumeChangeNotificationEvent,
                     BD_ADDR.BD_ADDR5,
                     BD_ADDR.BD_ADDR4,
                     BD_ADDR.BD_ADDR3,
                     BD_ADDR.BD_ADDR2,
                     BD_ADDR.BD_ADDR1,
                     BD_ADDR.BD_ADDR0,
                     TransactionID,
                     ResponseData->ResponseCode,
                     ResponseData->NotificationData.VolumeChangedData.AbsoluteVolume);
            break;
         case AVRCP_EVENT_NOW_PLAYING_CONTENT_CHANGED:
         case AVRCP_EVENT_AVAILABLE_PLAYERS_CHANGED:
         case AVRCP_EVENT_ADDRESSED_PLAYER_CHANGED:
         case AVRCP_EVENT_UIDS_CHANGED:
         default:
            PRINT_DEBUG("Unhandled Event ID: %d", ResponseData->EventID);
            break;
      }
   }
}

static void ProcessSetAbsoluteVolumeResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Set_Absolute_Volume_Response_Data_t *ResponseData)
{
   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      Env->CallVoidMethod(Object, Method_AVRCP_setAbsoluteVolumeCommandConfirmationEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode,
               ResponseData->AbsoluteVolume);
   }
}

static void ProcessCommandRejectResponse(JNIEnv *Env, jobject Object, BD_ADDR_t BD_ADDR, unsigned int TransactionID, AVRCP_Command_Reject_Response_Data_t *ResponseData)
{
   if((!COMPARE_NULL_BD_ADDR(BD_ADDR)) && (ResponseData))
   {
      Env->CallVoidMethod(Object, Method_AVRCP_commandRejectResponseEvent,
               BD_ADDR.BD_ADDR5,
               BD_ADDR.BD_ADDR4,
               BD_ADDR.BD_ADDR3,
               BD_ADDR.BD_ADDR2,
               BD_ADDR.BD_ADDR1,
               BD_ADDR.BD_ADDR0,
               TransactionID,
               ResponseData->ResponseCode,
               ResponseData->ErrorCode);
   }
}

static void ProcessRemoteControlCommandConfirmation(JNIEnv *Env, jobject Object, AUDM_Remote_Control_Command_Confirmation_Event_Data_t *CommandConfirmationData)
{
   if((Env) && (CommandConfirmationData))
   {
      switch(CommandConfirmationData->RemoteControlResponseData.MessageType)
      {
         case amtPassThrough:
            ProcessPassThroughResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.PassThroughResponseData);
            break;
         case amtVendorDependent_Generic:
            ProcessVendorDependentResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.VendorDependentGenericResponseData);
            break;
         case amtGroupNavigation:
            ProcessGroupNavigationResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.GroupNavigationResponseData);
            break;
         case amtGetCapabilities:
            ProcessGetCapabilitiesResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.GetCapabilitiesResponseData);
            break;
         case amtListPlayerApplicationSettingAttributes:
            ProcessListPlayerApplicationSettingAttributesResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.ListPlayerApplicationSettingAttributesResponseData);
            break;
         case amtListPlayerApplicationSettingValues:
            ProcessListPlayerApplicationSettingValuesResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.ListPlayerApplicationSettingValuesResponseData);
            break;
         case amtGetCurrentPlayerApplicationSettingValue:
            ProcessGetCurrentPlayerApplicationSettingValueResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.GetCurrentPlayerApplicationSettingValueResponseData);
            break;
         case amtSetPlayerApplicationSettingValue:
            ProcessSetPlayerApplicationSettingValueResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.SetPlayerApplicationSettingValueResponseData);
            break;
         case amtGetPlayerApplicationSettingAttributeText:
            ProcessGetPlayerApplicationSettingAttributeTextResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.GetPlayerApplicationSettingAttributeTextResponseData);
            break;
         case amtGetPlayerApplicationSettingValueText:
            ProcessGetPlayerApplicationSettingValueTextResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.GetPlayerApplicationSettingValueTextResponseData);
            break;
         case amtInformDisplayableCharacterSet:
            ProcessInformDisplayableCharacterSetResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.InformDisplayableCharacterSetResponseData);
            break;
         case amtInformBatteryStatusOfCT:
            ProcessInformBatteryStatusResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.InformBatteryStatusOfCTResponseData);
            break;
         case amtGetElementAttributes:
            ProcessGetElementAttributesResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.GetElementAttributesResponseData);
            break;
         case amtGetPlayStatus:
            ProcessGetPlayStatusResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.GetPlayStatusResponseData);
            break;
         case amtRegisterNotification:
            ProcessRegisterNotificationResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.RegisterNotificationResponseData);
            break;
         case amtSetAbsoluteVolume:
            ProcessSetAbsoluteVolumeResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.SetAbsoluteVolumeResponseData);
            break;
         case amtCommandRejectResponse:
            ProcessCommandRejectResponse(Env, Object, CommandConfirmationData->RemoteDeviceAddress, CommandConfirmationData->TransactionID, &CommandConfirmationData->RemoteControlResponseData.MessageData.CommandRejectResponseData);
            break;
         default:
            PRINT_DEBUG("Unhandled AVRCP Response type: %d", CommandConfirmationData->RemoteControlResponseData.MessageType);
            break;

      }
   }
}

static void AVRCP_AUDM_Event_Callback(AUDM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          NeedsDetach;
   jint         StreamType;
   jint         IntVal1;
   jint         IntVal2;
   jint         IntVal3;
   jint         IntVal4;
   jsize        Index;
   JNIEnv      *Env;
   jobject      AVRCPObject;
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
         /* The CallbackParameter is a weak ref to our AVRCP Manager    */
         /* Object. Check that the reference still appears valid, then  */
         /* obtain a strong local reference from the weak reference so  */
         /* we can access the object safely.                            */
         if(Env->GetObjectRefType((WEAK_REF)CallbackParameter) != JNIInvalidRefType)
         {
            if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_FALSE)
            {
               if((AVRCPObject = Env->NewLocalRef((WEAK_REF)CallbackParameter)) != NULL)
               {
                  if((LocalData = AcquireLocalData(Env, AVRCPObject, FALSE)) != NULL)
                  {
                     switch(EventData->EventType)
                     {
                        case aetIncomingConnectionRequest:
                           Env->CallVoidMethod(AVRCPObject, Method_AVRCP_remoteControlConnectionRequestEvent,
                                    EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.RemoteControlConnectedEventData.RemoteDeviceAddress.BD_ADDR0);
                           break;
                        case aetRemoteControlConnected:
                           Env->CallVoidMethod(AVRCPObject, Method_AVRCP_remoteControlConnectedEvent,
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
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_STATUS_SUCCESS;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_TIMEOUT:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_STATUS_FAILURE_TIMEOUT;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_REFUSED:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_STATUS_FAILURE_REFUSED;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_SECURITY:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_STATUS_FAILURE_SECURITY;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF;
                                 break;
                              case AUDM_REMOTE_CONTROL_CONNECTION_STATUS_FAILURE_UNKNOWN:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_STATUS_FAILURE_UNKNOWN;
                                 break;
                           }

                           Env->CallVoidMethod(AVRCPObject, Method_AVRCP_remoteControlConnectionStatusEvent,
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
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_REMOTE_CONTROL_DISCONNECT_REASON_DISCONNECT;
                                 break;
                              case adrRemoteDeviceLinkLoss:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_REMOTE_CONTROL_DISCONNECT_REASON_LINK_LOSS;
                                 break;
                              case adrRemoteDeviceTimeout:
                              default:
                                 IntVal1 = com_stonestreetone_bluetopiapm_AVRCP_REMOTE_CONTROL_DISCONNECT_REASON_TIMEOUT;
                                 break;
                           }
                           
                           Env->CallVoidMethod(AVRCPObject, Method_AVRCP_remoteControlDisconnectedEvent,
                                    EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR5,
                                    EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR4,
                                    EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR3,
                                    EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR2,
                                    EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR1,
                                    EventData->EventData.RemoteControlDisconnectedEventData.RemoteDeviceAddress.BD_ADDR0,
                                    IntVal1);
                           break;
                        case aetRemoteControlCommandConfirmation:
                           if(EventData->EventData.RemoteControlCommandConfirmationEventData.Status == 0)
                           {
                              ProcessRemoteControlCommandConfirmation(Env, AVRCPObject, &EventData->EventData.RemoteControlCommandConfirmationEventData);
                           }
                           else
                           {
                              /* The command failed.                    */
                              Env->CallVoidMethod(AVRCPObject, Method_AVRCP_commandFailureEvent,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR5,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR4,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR3,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR2,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR1,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.RemoteDeviceAddress.BD_ADDR0,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.TransactionID,
                                       EventData->EventData.RemoteControlCommandConfirmationEventData.Status);
                           }
                           break;
                        case aetRemoteControlCommandIndication:
                        default:
                           PRINT_DEBUG("AVRCP: Unhandled AUDM Event");
                     }

                     /* Check for Java exceptions thrown during the     */
                     /* callback.                                       */
                     if(Env->ExceptionCheck())
                     {
                        PRINT_ERROR("Exception thrown during AUDM-AVRCP event callback.");
                        Env->ExceptionDescribe();
                        Env->ExceptionClear();
                     }

                     ReleaseLocalData(Env, AVRCPObject, LocalData);
                  }

                  Env->DeleteLocalRef(AVRCPObject);
               }
               else
               {
                  /* We were unable to obtain a strong reference to the */
                  /* AVRCP java object. This can happen if the object   */
                  /* has been garbage collected or if the VM has run    */
                  /* out of memory. Now, check whether the object was   */
                  /* GC'd. Since NewLocalRef doesn't throw exceptions,  */
                  /* we can do this with IsSameObject.                  */
                  if(Env->IsSameObject((WEAK_REF)CallbackParameter, 0) == JNI_TRUE)
                  {
                     /* The AVRCP Manager object has been GC'd. Since we*/
                     /* are still receiving events on this registration,*/
                     /* the Manager was not cleaned up properly.        */
                     PRINT_ERROR("AVRCP Manager was not cleaned up properly.");
                  }
                  else
                  {
                     /* The VM ran out of memory. Report this error as it  */
                     /* could indicate a leak in this thread context.      */
                     PRINT_ERROR("VM reports 'Out of Memory' in AUDM-AVRCP event dispatch thread.");
                  }
               }
            }
            else
            {
               /* The AVRCP Manager object has been GC'd. Since we are  */
               /* still receiving events on this registration, the      */
               /* Manager was not cleaned up properly.                  */
               PRINT_ERROR("Object reference is invalid: AVRCP Manager was not cleaned up properly.");
            }
         }
         else
         {
            /* This callback was called after the Manager object was    */
            /* cleaned up. This could indicate a synchronization bug.   */
            PRINT_ERROR("Object reference is invalid: AVRCP Manager was already cleand up.");
         }

         if(NeedsDetach)
            DetachJavaEnv(Env);
      }
   }

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    initClassNative
 * Signature: ()V
 */
static void InitClassNative(JNIEnv *Env, jclass Clazz)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if((Class_AVRCP = (jclass)(NewWeakRef(Env, Clazz))) != 0)
   {
      if((Field_AVRCP_localData = Env->GetFieldID(Clazz, "localData", "J")) == 0)
      {
         PRINT_ERROR("Unable to locate $localData field");
      }

      Method_AVRCP_remoteControlConnectionRequestEvent                              = Env->GetMethodID(Clazz, "remoteControlConnectionRequestEvent",                              "(BBBBBB)V");
      Method_AVRCP_remoteControlConnectedEvent                                      = Env->GetMethodID(Clazz, "remoteControlConnectedEvent",                                      "(BBBBBB)V");
      Method_AVRCP_remoteControlConnectionStatusEvent                               = Env->GetMethodID(Clazz, "remoteControlConnectionStatusEvent",                               "(BBBBBBI)V");
      Method_AVRCP_remoteControlDisconnectedEvent                                   = Env->GetMethodID(Clazz, "remoteControlDisconnectedEvent",                                   "(BBBBBBI)V");
      Method_AVRCP_remoteControlPassThroughCommandConfirmationEvent                 = Env->GetMethodID(Clazz, "remoteControlPassThroughCommandConfirmationEvent",                 "(BBBBBBIBBBIZ[B)V");
      Method_AVRCP_vendorDependentCommandConfirmationEvent                          = Env->GetMethodID(Clazz, "vendorDependentCommandConfirmationEvent",                          "(BBBBBBIBBBBBB[B)V");
      Method_AVRCP_groupNavigationCommandConfirmationEvent                          = Env->GetMethodID(Clazz, "groupNavigationCommandConfirmationEvent",                          "(BBBBBBIBZ)V");
      Method_AVRCP_getCompanyIDCapabilitiesCommandConfirmationEvent                 = Env->GetMethodID(Clazz, "getCompanyIDCapabilitiesCommandConfirmationEvent",                 "(BBBBBBIB[B)V");
      Method_AVRCP_getEventsSupportedCapabilitiesCommandConfirmationEvent           = Env->GetMethodID(Clazz, "getEventsSupportedCapabilitiesCommandConfirmationEvent",           "(BBBBBBIBI)V");
      Method_AVRCP_listPlayerApplicationSettingAttributesCommandConfirmationEvent   = Env->GetMethodID(Clazz, "listPlayerApplicationSettingAttributesCommandConfirmationEvent",   "(BBBBBBIB[B)V");
      Method_AVRCP_listPlayerApplicationSettingValuesCommandConfirmationEvent       = Env->GetMethodID(Clazz, "listPlayerApplicationSettingValuesCommandConfirmationEvent",       "(BBBBBBIB[B)V");
      Method_AVRCP_getCurrentPlayerApplicationSettingValueCommandConfirmationEvent  = Env->GetMethodID(Clazz, "getCurrentPlayerApplicationSettingValueCommandConfirmationEvent",  "(BBBBBBIB[B[B)V");
      Method_AVRCP_setPlayerApplicationSettingValueCommandConfirmationEvent         = Env->GetMethodID(Clazz, "setPlayerApplicationSettingValueCommandConfirmationEvent",         "(BBBBBBIB)V");
      Method_AVRCP_getPlayerApplicationSettingAttributeTextCommandConfirmationEvent = Env->GetMethodID(Clazz, "getPlayerApplicationSettingAttributeTextCommandConfirmationEvent", "(BBBBBBIB[B[S[[B)V");
      Method_AVRCP_getPlayerApplicationSettingValueTextCommandConfirmationEvent     = Env->GetMethodID(Clazz, "getPlayerApplicationSettingValueTextCommandConfirmationEvent",     "(BBBBBBIB[B[S[[B)V");
      Method_AVRCP_informDisplayableCharacterSetCommandConfirmationEvent            = Env->GetMethodID(Clazz, "informDisplayableCharacterSetCommandConfirmationEvent",            "(BBBBBBIB)V");
      Method_AVRCP_informBatteryStatusCommandConfirmationEvent                      = Env->GetMethodID(Clazz, "informBatteryStatusCommandConfirmationEvent",                      "(BBBBBBIB)V");
      Method_AVRCP_getElementAttributesCommandConfirmationEvent                     = Env->GetMethodID(Clazz, "getElementAttributesCommandConfirmationEvent",                     "(BBBBBBIB[I[S[[B)V");
      Method_AVRCP_getPlayStatusCommandConfirmationEvent                            = Env->GetMethodID(Clazz, "getPlayStatusCommandConfirmationEvent",                            "(BBBBBBIBIII)V");
      Method_AVRCP_playbackStatusChangedNotificationEvent                           = Env->GetMethodID(Clazz, "playbackStatusChangedNotificationEvent",                           "(BBBBBBIBI)V");
      Method_AVRCP_trackChangedNotificationEvent                                    = Env->GetMethodID(Clazz, "trackChangedNotificationEvent",                                    "(BBBBBBIBJ)V");
      Method_AVRCP_trackReachedEndNotificationEvent                                 = Env->GetMethodID(Clazz, "trackReachedEndNotificationEvent",                                 "(BBBBBBIB)V");
      Method_AVRCP_trackReachedStartNotificationEvent                               = Env->GetMethodID(Clazz, "trackReachedStartNotificationEvent",                               "(BBBBBBIB)V");
      Method_AVRCP_playbackPositionChangedNotificationEvent                         = Env->GetMethodID(Clazz, "playbackPositionChangedNotificationEvent",                         "(BBBBBBIBI)V");
      Method_AVRCP_batteryStatusChangedNotificationEvent                            = Env->GetMethodID(Clazz, "batteryStatusChangedNotificationEvent",                            "(BBBBBBIBI)V");
      Method_AVRCP_systemStatusChangedNotificationEvent                             = Env->GetMethodID(Clazz, "systemStatusChangedNotificationEvent",                             "(BBBBBBIBI)V");
      Method_AVRCP_playerApplicationSettingChangedNotificationEvent                 = Env->GetMethodID(Clazz, "playerApplicationSettingChangedNotificationEvent",                 "(BBBBBBIB[B[B)V");
      Method_AVRCP_volumeChangeNotificationEvent                                    = Env->GetMethodID(Clazz, "volumeChangeNotificationEvent",                                    "(BBBBBBIBB)V");
      Method_AVRCP_setAbsoluteVolumeCommandConfirmationEvent                        = Env->GetMethodID(Clazz, "setAbsoluteVolumeCommandConfirmationEvent",                        "(BBBBBBIBB)V");
      Method_AVRCP_commandRejectResponseEvent                                       = Env->GetMethodID(Clazz, "commandRejectResponseEvent",                                       "(BBBBBBIBI)V");
      Method_AVRCP_commandFailureEvent                                              = Env->GetMethodID(Clazz, "commandFailureEvent",                                              "(BBBBBBII)V");
   }
   else
      PRINT_ERROR("AVRCP: Unable to load methods for class 'com.stonestreetone.bluetopiapm.AVRCP'");

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    initObjectNative
 * Signature: (I)V
 */
static void InitObjectNative(JNIEnv *Env, jobject Object, jint Type)
{
   int          RegistrationResult = 0;
   LocalData_t *LocalData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   if(InitBTPMClient(TRUE) == 0)
   {
      if((LocalData = (LocalData_t *)malloc(sizeof(LocalData_t))) != NULL)
      {
         LocalData->CallbackID           = 0;

         if((LocalData->WeakObjectReference = NewWeakRef(Env, Object)) != NULL)
         {
            if((RegistrationResult = AUDM_Register_Remote_Control_Event_Callback((Type == com_stonestreetone_bluetopiapm_AVRCP_TYPE_CONTROLLER)?AUDM_REGISTER_REMOTE_CONTROL_DATA_SERVICE_TYPE_CONTROLLER:AUDM_REGISTER_REMOTE_CONTROL_DATA_SERVICE_TYPE_TARGET, AVRCP_AUDM_Event_Callback, LocalData->WeakObjectReference)) > 0)
            {
               LocalData->CallbackID = (unsigned int)RegistrationResult;
               SetLocalData(Env, Object, LocalData);
            }
            else
            {
               CloseBTPMClient();
               DeleteWeakRef(Env, LocalData->WeakObjectReference);
               free(LocalData);
               LocalData = 0;

               if(RegistrationResult == BTPM_ERROR_CODE_REMOTE_CONTROL_EVENT_ALREADY_REGISTERED)
                  Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/AVRCP$RemoteControlAlreadyRegisteredException"), NULL);
               else
                  Env->ThrowNew(Env->FindClass("com/stonestreetone/bluetopiapm/ServerNotReachableException"), NULL);

            }
         }
         else
         {
            /* No need to throw an exception here. NewWeakRef will throw*/
            /* an OutOfMemoryError if we are out of resources.          */
            PRINT_ERROR("AVRCPM: Out of Memory: Unable to obtain weak reference to object");
            
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
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
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
         AUDM_Un_Register_Remote_Control_Event_Callback(LocalData->CallbackID);

      if(LocalData->WeakObjectReference)
         DeleteWeakRef(Env, LocalData->WeakObjectReference);

      memset(LocalData, 0, sizeof(LocalData_t));
      free(LocalData);
   }

   CloseBTPMClient();

   PRINT_DEBUG("%s: Exit", __FUNCTION__);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    connectionRequestResponseNative
 * Signature: (BBBBBBZ)I
 */
static jint ConnectionRequestResponseNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jboolean Accept)
{
   jint                          Result;
   BD_ADDR_t                     RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = AUDM_Connection_Request_Response(acrRemoteControl, RemoteDeviceAddress, ((Accept == JNI_FALSE) ? FALSE : TRUE));

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    connectRemoteControlNative
 * Signature: (BBBBBBIZ)I
 */
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
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AVRCP_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_ENCRYPTION     : 0);

   if(WaitForConnection == JNI_FALSE)
   {
      if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
      {
         Result = AUDM_Connect_Remote_Control(RemoteDeviceAddress, Flags, AVRCP_AUDM_Event_Callback, LocalData->WeakObjectReference, 0);

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

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    changeIncomingConnectionFlagsNative
 * Signature: (I)I
 */
static jint ChangeIncomingConnectionFlagsNative(JNIEnv *Env, jobject Object, jint ConnectionFlags)
{
   jint Result;
   long Flags;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   Flags = 0;
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AVRCP_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)  ? AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AVRCP_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) ? AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
   Flags |= ((ConnectionFlags & com_stonestreetone_bluetopiapm_AVRCP_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)     ? AUDM_INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

   Result = AUDM_Change_Incoming_Connection_Flags(Flags);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    disconnectRemoteConrolNative
 * Signature: (BBBBBB)I
 */
static jint DisconnectRemoteConrolNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6)
{
   jint      Result;
   BD_ADDR_t RemoteDeviceAddress;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   Result = AUDM_Disconnect_Remote_Control(RemoteDeviceAddress);

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    queryConnectedRemoteControlDevicesNative
 * Signature: ([[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;)I
 */
static jint QueryConnectedRemoteControlDevicesNative(JNIEnv *Env, jobject Object, jobjectArray RemoteDeviceAddressList)
{
   int           Index;
   jint          Result;
   jobject       Address;
   BD_ADDR_t    *BD_ADDRList;
   unsigned int  TotalConnected;
   jobjectArray  AddressList;

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
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    sendRemoteControlPassThroughCommandNative
 * Signature: (BBBBBBJIIIIZ[B)I
 */
static jint SendRemoteControlPassThroughCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyte CommandType, jbyte SubunitType, jbyte SubunitID, jint OperationID, jboolean StateFlag, jbyteArray OperationData)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageType = amtPassThrough;

   CommandData.MessageData.PassThroughCommandData.CommandType = CommandType;
   CommandData.MessageData.PassThroughCommandData.SubunitType = SubunitType;
   CommandData.MessageData.PassThroughCommandData.SubunitID   = SubunitID;

   switch(OperationID)
   {
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SELECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_UP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_LEFT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RIGHT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RIGHT_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT_UP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RIGHT_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_LEFT_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT_UP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_LEFT_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_ROOT_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_ROOT_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SETUP_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SETUP_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CONTENTS_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_FAVORITE_MENU:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_EXIT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_EXIT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_0:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_0;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_1:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_1;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_2:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_2;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_3:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_3;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_4:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_4;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_5:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_5;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_6:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_6;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_7:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_7;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_8:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_8;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_9:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_9;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_DOT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_DOT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_ENTER:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_ENTER;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CLEAR:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CLEAR;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CHANNEL_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SOUND_SELECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_INPUT_SELECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_HELP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_HELP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PAGE_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PAGE_UP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PAGE_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_POWER:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_POWER;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_VOLUME_UP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_VOLUME_UP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_VOLUME_DOWN:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_MUTE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_MUTE;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PLAY:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PLAY;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_STOP:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_STOP;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_PAUSE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_PAUSE;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_RECORD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_RECORD;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_REWIND:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_REWIND;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_FAST_FORWARD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_EJECT:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_EJECT;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_FORWARD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_FORWARD;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_BACKWARD:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_BACKWARD;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_ANGLE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_ANGLE;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_SUBPICTURE:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_SUBPICTURE;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F1:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F1;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F2:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F2;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F3:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F3;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F4:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F4;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_F5:
         CommandData.MessageData.PassThroughCommandData.OperationID = AVRCP_PASS_THROUGH_ID_F5;
         break;
      case com_stonestreetone_bluetopiapm_AVRCP_AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE:
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
            Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseByteArrayElements(OperationData, (jbyte *)(CommandData.MessageData.PassThroughCommandData.OperationData), JNI_ABORT);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         CommandData.MessageData.PassThroughCommandData.OperationDataLength = 0;
         CommandData.MessageData.PassThroughCommandData.OperationData       = NULL;

         Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    vendorDependentCommandNative
 * Signature: (BBBBBBJIIIBBB[B)I
 */
static jint VendorDependentCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyte CommandType, jbyte SubunitType, jbyte SubunitID, jbyte ID0, jbyte ID1, jbyte ID2, jbyteArray OperationData)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   CommandData.MessageData.VendorDependentGenericCommandData.CommandType          = CommandType;
   CommandData.MessageData.VendorDependentGenericCommandData.SubunitType          = SubunitType;
   CommandData.MessageData.VendorDependentGenericCommandData.SubunitID            = SubunitID;

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
            Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseByteArrayElements(OperationData, (jbyte *)(CommandData.MessageData.VendorDependentGenericCommandData.DataBuffer), JNI_ABORT);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
      {
         CommandData.MessageData.VendorDependentGenericCommandData.DataLength = 0;
         CommandData.MessageData.VendorDependentGenericCommandData.DataBuffer = NULL;

         Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);
      }

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    groupNavigationCommandNative
 * Signature: (BBBBBBJZI)I
 */
static jint GroupNavigationCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jboolean ButtonState, jint NavigationType)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType                                           = amtGroupNavigation;

      CommandData.MessageData.GroupNavigationCommandData.ButtonState    = ButtonState?bsPressed:bsReleased;
      CommandData.MessageData.GroupNavigationCommandData.NavigationType = NavigationType;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
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

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType                                         = amtGetCapabilities;

      CommandData.MessageData.GetCapabilitiesCommandData.CapabilityID = CapabilityID;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    listPlayerApplicationSettingAttribtutesCommandNative
 * Signature: (BBBBBBJ)I
 */
static jint ListPlayerApplicationSettingAttribtutesCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType = amtListPlayerApplicationSettingAttributes;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    listPlayerApplicationSettingValuesCommandNative
 * Signature: (BBBBBBJB)I
 */
static jint ListPlayerApplicationSettingValuesCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyte AttributeID)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType                                                           = amtListPlayerApplicationSettingValues;

      CommandData.MessageData.ListPlayerApplicationSettingValuesCommandData.AttributeID = AttributeID;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    getCurrentPlayerApplicationSettingValueCommandNative
 * Signature: (BBBBBBJ[B)I
 */
static jint GetCurrentPlayerApplicationSettingValueCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyteArray AttributeIDs)
{
   jint                               Result;
   jbyte                             *IDArray;
   jsize                              Index;
   jsize                              IDArrayLength;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((IDArrayLength = Env->GetArrayLength(AttributeIDs)) > 0)
      {
         if((IDArray = Env->GetByteArrayElements(AttributeIDs, JNI_FALSE)) != NULL)
         {
            CommandData.MessageType                                                                       = amtGetCurrentPlayerApplicationSettingValue;

            CommandData.MessageData.GetCurrentPlayerApplicationSettingValueCommandData.NumberAttributeIDs = IDArrayLength;
            CommandData.MessageData.GetCurrentPlayerApplicationSettingValueCommandData.AttributeIDList    = (Byte_t *)IDArray;

            Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseByteArrayElements(AttributeIDs, IDArray, JNI_ABORT);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
         Result = BTPM_ERROR_CODE_INVALID_PARAMETER;

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    setPlayerApplicationSettingValueCommandNative
 * Signature: (BBBBBBJ[B[B)I
 */
static jint SetPlayerApplicationSettingValueCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyteArray AttributeIDs, jbyteArray ValueIDs)
{
   jint                                   Result;
   jbyte                                 *AttributeArray      = NULL;
   jbyte                                 *ValueArray          = NULL;
   jsize                                  Length;
   jsize                                  Index;
   BD_ADDR_t                              RemoteDeviceAddress;
   LocalData_t                           *LocalData;
   AUD_Remote_Control_Command_Data_t      CommandData;
   AVRCP_Attribute_Value_ID_List_Entry_t *ValueList           = NULL;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((Length = Env->GetArrayLength(AttributeIDs)) > 0)
      {
         if(((AttributeArray = Env->GetByteArrayElements(AttributeIDs, JNI_FALSE)) != NULL) && ((ValueArray = Env->GetByteArrayElements(ValueIDs, JNI_FALSE)) != NULL) && ((ValueList = (AVRCP_Attribute_Value_ID_List_Entry_t *)BTPS_AllocateMemory(sizeof(AVRCP_Attribute_Value_ID_List_Entry_t) * Length)) != NULL))
         {
            for(Index=0;Index<Length;Index++)
            {
               ValueList[Index].AttributeID = AttributeArray[Index];
               ValueList[Index].ValueID     = ValueArray[Index];
            }

            CommandData.MessageType                                                                     = amtSetPlayerApplicationSettingValue;

            CommandData.MessageData.SetPlayerApplicationSettingValueCommandData.NumberAttributeValueIDs = Length;
            CommandData.MessageData.SetPlayerApplicationSettingValueCommandData.AttributeValueIDList    = ValueList;

            Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            if(AttributeArray)
               Env->ReleaseByteArrayElements(AttributeIDs, AttributeArray, JNI_ABORT);
            if(ValueArray)
               Env->ReleaseByteArrayElements(ValueIDs, ValueArray, JNI_ABORT);
            if(ValueList)
               BTPS_FreeMemory(ValueList);

            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
         Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    getPlayerApplicationSettingAttributeTextCommandNative
 * Signature: (BBBBBBJ[B)I
 */
static jint GetPlayerApplicationSettingAttributeTextCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyteArray AttributeIDs)
{
   jint                               Result;
   jbyte                             *AttributeArray;
   jsize                              Length;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((Length = Env->GetArrayLength(AttributeIDs)) > 0)
      {
         if((AttributeArray = Env->GetByteArrayElements(AttributeIDs, JNI_FALSE)) != NULL)
         {
            CommandData.MessageType                                                                        = amtGetPlayerApplicationSettingAttributeText;

            CommandData.MessageData.GetPlayerApplicationSettingAttributeTextCommandData.NumberAttributeIDs = Length;
            CommandData.MessageData.GetPlayerApplicationSettingAttributeTextCommandData.AttributeIDList    = (Byte_t *)AttributeArray;

            Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseByteArrayElements(AttributeIDs, AttributeArray, JNI_ABORT);

            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
         Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    getPlayerApplicationSettingValueTextCommandNative
 * Signature: (BBBBBBJB[B)I
 */
static jint GetPlayerApplicationSettingValueTextCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyte AttributeID, jbyteArray ValueIDs)
{
   jint                               Result;
   jbyte                             *ValueArray;
   jsize                              Length;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((Length = Env->GetArrayLength(ValueIDs)) > 0)
      {
         if((ValueArray = Env->GetByteArrayElements(ValueIDs, JNI_FALSE)) != NULL)
         {
            CommandData.MessageType                                                                = amtGetPlayerApplicationSettingValueText;

            CommandData.MessageData.GetPlayerApplicationSettingValueTextCommandData.AttributeID    = AttributeID;
            CommandData.MessageData.GetPlayerApplicationSettingValueTextCommandData.NumberValueIDs = Length;
            CommandData.MessageData.GetPlayerApplicationSettingValueTextCommandData.ValueIDList    = (Byte_t *)ValueArray;

            Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseByteArrayElements(ValueIDs, ValueArray, JNI_ABORT);

            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
         Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    informDisplayableCharacterSetCommandNative
 * Signature: (BBBBBBJ[S)I
 */
static jint InformDisplayableCharacterSetCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jshortArray CharacterSets)
{
   jint                               Result;
   jsize                              Length;
   jshort                            *CharsetArray;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if((Length = Env->GetArrayLength(CharacterSets)) > 0)
      {
         if((CharsetArray = Env->GetShortArrayElements(CharacterSets, JNI_FALSE)) != NULL)
         {
            CommandData.MessageType                                                              = amtInformDisplayableCharacterSet;

            CommandData.MessageData.InformDisplayableCharacterSetCommandData.NumberCharacterSets = Length;
            CommandData.MessageData.InformDisplayableCharacterSetCommandData.CharacterSetList    = (Word_t *)CharsetArray;

            Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

            Env->ReleaseShortArrayElements(CharacterSets, CharsetArray, JNI_ABORT);

            ReleaseLocalData(Env, Object, LocalData);
         }
         else
            Result = BTPM_ERROR_CODE_UNABLE_TO_ALLOCATE_MEMORY;
      }
      else
         Result = BTPM_ERROR_CODE_INVALID_PARAMETER;
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    informBatteryStatusOfControllerCommandNative
 * Signature: (BBBBBBJI)I
 */
static jint InformBatteryStatusOfControllerCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jint BatteryStatus)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType                                                  = amtInformBatteryStatusOfCT;

      CommandData.MessageData.InformBatteryStatusOfCTCommandData.BatteryStatus = BatteryStatus;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    getElementAttributesCommandNative
 * Signature: (BBBBBBJJI)I
 */
static jint GetElementAttributesCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jlong Identifier, jint AttributeMask)
{
   jint                               Result;
   DWord_t                            Attributes[7];
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   unsigned int                       NumberAttributes = 0;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      if(AttributeMask & com_stonestreetone_bluetopiapm_AVRCP_ElementAttributeID_BIT_TITLE)
         Attributes[NumberAttributes++] = AVRCP_MEDIA_ATTRIBUTE_ID_TITLE_OF_MEDIA;
      if(AttributeMask & com_stonestreetone_bluetopiapm_AVRCP_ElementAttributeID_BIT_ARTIST)
         Attributes[NumberAttributes++] = AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ARTIST;
      if(AttributeMask & com_stonestreetone_bluetopiapm_AVRCP_ElementAttributeID_BIT_ALBUM)
         Attributes[NumberAttributes++] = AVRCP_MEDIA_ATTRIBUTE_ID_NAME_OF_ALBUM;
      if(AttributeMask & com_stonestreetone_bluetopiapm_AVRCP_ElementAttributeID_BIT_NUMBER_OF_MEDIA)
         Attributes[NumberAttributes++] = AVRCP_MEDIA_ATTRIBUTE_ID_NUMBER_OF_MEDIA;
      if(AttributeMask & com_stonestreetone_bluetopiapm_AVRCP_ElementAttributeID_BIT_TOTAL_NUMBER_OF_MEDIA)
         Attributes[NumberAttributes++] = AVRCP_MEDIA_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA;
      if(AttributeMask & com_stonestreetone_bluetopiapm_AVRCP_ElementAttributeID_BIT_GENRE)
         Attributes[NumberAttributes++] = AVRCP_MEDIA_ATTRIBUTE_ID_GENRE;
      if(AttributeMask & com_stonestreetone_bluetopiapm_AVRCP_ElementAttributeID_BIT_PLAYING_TIME)
         Attributes[NumberAttributes++] = AVRCP_MEDIA_ATTRIBUTE_ID_PLAYING_TIME_MS;

      CommandData.MessageType                                                  = amtGetElementAttributes;

      CommandData.MessageData.GetElementAttributesCommandData.Identifier       = Identifier;
      CommandData.MessageData.GetElementAttributesCommandData.NumberAttributes = NumberAttributes;
      CommandData.MessageData.GetElementAttributesCommandData.AttributeIDList  = Attributes;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
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

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType = amtGetPlayStatus;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    registerNotificationCommandNative
 * Signature: (BBBBBBJII)I
 */
static jint RegisterNotificationCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jint EventID, jint PlaybackInterval)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType                                                  = amtRegisterNotification;
      CommandData.MessageData.RegisterNotificationCommandData.EventID          = EventID;
      CommandData.MessageData.RegisterNotificationCommandData.PlaybackInterval = PlaybackInterval;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_AVRCP
 * Method:    setAbsoluteVolumeCommandNative
 * Signature: (BBBBBBJB)I
 */
static jint SetAbsoluteVolumeCommandNative(JNIEnv *Env, jobject Object, jbyte Address1, jbyte Address2, jbyte Address3, jbyte Address4, jbyte Address5, jbyte Address6, jlong ResponseTimeout, jbyte Volume)
{
   jint                               Result;
   BD_ADDR_t                          RemoteDeviceAddress;
   LocalData_t                       *LocalData;
   AUD_Remote_Control_Command_Data_t  CommandData;

   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   ASSIGN_BD_ADDR(RemoteDeviceAddress, Address1, Address2, Address3, Address4, Address5, Address6);

   if((LocalData = AcquireLocalData(Env, Object, FALSE)) != NULL)
   {
      CommandData.MessageType                                             = amtSetAbsoluteVolume;

      CommandData.MessageData.SetAbsoluteVolumeCommandData.AbsoluteVolume = Volume;

      Result = AUDM_Send_Remote_Control_Command(LocalData->CallbackID, RemoteDeviceAddress, ResponseTimeout, &CommandData);

      ReleaseLocalData(Env, Object, LocalData);
   }
   else
      Result = -1;

   PRINT_DEBUG("%s: Exit (%d)", __FUNCTION__, Result);

   return Result;
}


static JNINativeMethod Methods[] = {
   {"initClassNative",                                       "()V",                                                    (void *)InitClassNative},
   {"initObjectNative",                                      "(I)V",                                                   (void *)InitObjectNative},
   {"cleanupObjectNative",                                   "()V",                                                    (void *)CleanupObjectNative},
   {"connectionRequestResponseNative",                       "(BBBBBBZ)I",                                             (void *)ConnectionRequestResponseNative},
   {"connectRemoteControlNative",                            "(BBBBBBIZ)I",                                            (void *)ConnectRemoteControlNative},
   {"changeIncomingConnectionFlagsNative",                   "(I)I",                                                   (void *)ChangeIncomingConnectionFlagsNative},
   {"disconnectRemoteConrolNative",                          "(BBBBBB)I",                                              (void *)DisconnectRemoteConrolNative},
   {"queryConnectedRemoteControlDevicesNative",              "([[Lcom/stonestreetone/bluetopiapm/BluetoothAddress;)I", (void *)QueryConnectedRemoteControlDevicesNative},
   {"sendRemoteControlPassThroughCommandNative",             "(BBBBBBJBBBIZ[B)I",                                      (void *)SendRemoteControlPassThroughCommandNative},
   {"vendorDependentCommandNative",                          "(BBBBBBJBBBBBB[B)I",                                     (void *)VendorDependentCommandNative},
   {"groupNavigationCommandNative",                          "(BBBBBBJZI)I",                                           (void *)GroupNavigationCommandNative},
   {"getCapabilitiesCommandNative",                          "(BBBBBBJI)I",                                            (void *)GetCapabilitiesCommandNative},
   {"listPlayerApplicationSettingAttribtutesCommandNative",  "(BBBBBBJ)I",                                             (void *)ListPlayerApplicationSettingAttribtutesCommandNative},
   {"listPlayerApplicationSettingValuesCommandNative",       "(BBBBBBJB)I",                                            (void *)ListPlayerApplicationSettingValuesCommandNative},
   {"getCurrentPlayerApplicationSettingValueCommandNative",  "(BBBBBBJ[B)I",                                           (void *)GetCurrentPlayerApplicationSettingValueCommandNative},
   {"setPlayerApplicationSettingValueCommandNative",         "(BBBBBBJ[B[B)I",                                         (void *)SetPlayerApplicationSettingValueCommandNative},
   {"getPlayerApplicationSettingAttributeTextCommandNative", "(BBBBBBJ[B)I",                                           (void *)GetPlayerApplicationSettingAttributeTextCommandNative},
   {"getPlayerApplicationSettingValueTextCommandNative",     "(BBBBBBJB[B)I",                                          (void *)GetPlayerApplicationSettingValueTextCommandNative},
   {"informDisplayableCharacterSetCommandNative",            "(BBBBBBJ[S)I",                                           (void *)InformDisplayableCharacterSetCommandNative},
   {"informBatteryStatusOfControllerCommandNative",          "(BBBBBBJI)I",                                            (void *)InformBatteryStatusOfControllerCommandNative},
   {"getElementAttributesCommandNative",                     "(BBBBBBJJI)I",                                           (void *)GetElementAttributesCommandNative},
   {"getPlayStatusCommandNative",                            "(BBBBBBJ)I",                                             (void *)GetPlayStatusCommandNative},
   {"registerNotificationCommandNative",                     "(BBBBBBJII)I",                                           (void *)RegisterNotificationCommandNative},
   {"setAbsoluteVolumeCommandNative",                        "(BBBBBBJB)I",                                            (void *)SetAbsoluteVolumeCommandNative},
};

int register_com_stonestreetone_bluetopiapm_AVRCP(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/AVRCP";

   Result = -1;

   PRINT_DEBUG("Registering AVRCP native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}

