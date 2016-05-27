/*****< btpmsrvr.c >***********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  BTPMSRVR - Bluetopia Platform Manager Main Application Entry point for    */
/*             Linux.                                                         */
/*                                                                            */
/*  Author:  Damon Lange                                                      */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/07/10  D. Lange        Initial creation.                              */
/******************************************************************************/
#include <stdio.h>

#include "SS1CFG.h"

#include "SS1BTPM.h"             /* BTPM API Prototypes and Constants.        */

#if SS1_SUPPORT_A2DP
#include "SS1BTAUDM.h"           /* BTPM Audio API for AVRCP.                 */
#endif

#if SS1_SUPPORT_HID
#include "SS1BTHIDM.h"           /* BTPM HID Host API for HID Events.         */
#endif

#include "BTPMSRVR.h"            /* BTPM Main Application Proto./Constants.   */
#include "UINPUT.h"              /* AVRCP/uinput Interface Prototypes.        */

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include <stdio.h>
#include <string.h>


typedef struct _tagConnectionEntry_t
{
   BD_ADDR_t                     BD_ADDR;
   unsigned int                  Flags;
   struct _tagConnectionEntry_t *NextConnectionEntry;
} ConnectionEntry_t;

   /* The following constants are used with the Flags member of the     */
   /* ConnectionEntry_t structure to denote various state information   */
   /* about the connection.                                             */
#define CONNECTION_ENTRY_FLAGS_HID_KEYBOARD_ACTIVE    0x01
#define CONNECTION_ENTRY_FLAGS_HID_MOUSE_ACTIVE       0x02

#define KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_KANA     0x10 /* Represent the    */
#define KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_COMPOSE  0x08 /* Boot Protocol    */
#define KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_SCROLL   0x04 /* Output Report    */
#define KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_CAPS     0x02 /* bit mask         */
#define KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_NUM      0x01

   /* Internal Variables to this Module (Remember that all variables    */
   /* declared static are initialized to 0 automatically by the compiler*/
   /* as part of standard C/C++).                                       */

   /* Variable which holds the Local Device Manager Callback ID of the  */
   /* DEVM Event Callback that is registered (to watch for Power Off/On */
   /* events).                                                          */
static unsigned int DEVMEventCallbackID;

   /* Variable which holds the currently registered Remote Control      */
   /* Callback ID.  This ID is needed to respond to Remote Control      */
   /* events that are received.                                         */
static unsigned int RemoteControlCallbackID;

   /* Variable which holds the currently registered HID Data Callback   */
   /* ID.  This ID is needed to respond to HID events that are received.*/
static unsigned int HIDDataCallbackID;

   /* Variable which holds the first element of the Connection Entry    */
   /* List (used to keep track of virtual Keyboards and/or virtual Mice.*/
static ConnectionEntry_t *ConnectionEntryList;

   /* Variable used to hold the Lock State report that is used to       */
   /* indicate LED States to keyboards.                                 */
static Byte_t LockStateReport[2];

   /* Internal Function Prototypes.                                     */
static ConnectionEntry_t *AddConnectionEntry(ConnectionEntry_t **ConnectionEntryList, ConnectionEntry_t *ConnectionEntry);
static ConnectionEntry_t *SearchConnectionEntry(ConnectionEntry_t **ConnectionEntryList, BD_ADDR_t BD_ADDR);
static ConnectionEntry_t *DeleteConnectionEntry(ConnectionEntry_t **ConnectionEntryList, BD_ADDR_t BD_ADDR);
static void FreeConnectionEntryMemory(ConnectionEntry_t *ConnectionEntry);
static void FreeConnectionEntryList(ConnectionEntry_t **ConnectionEntryList);

static void BTPSAPI DEVMEventCallback(DEVM_Event_Data_t *EventData, void *CallbackParameter);
static void BTPSAPI BTPMDispatchCallback_Initialization(void *CallbackParameter);
static void BTPSAPI BTPMDispatchCallback_Power(void *CallbackParameter);

#if SS1_SUPPORT_A2DP

static void BTPSAPI AUDMEventCallback(AUDM_Event_Data_t *EventData, void *CallbackParameter);

#endif

#if ((SS1_PLATFORM_SDK_VERSION >= 14) && (SS1_SUPPORT_HID))

static void BTPSAPI HIDMEventCallback(HIDM_Event_Data_t *EventData, void *CallbackParameter);

#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)

   /* The following function adds the specified Entry to the specified  */
   /* List.  This function allocates and adds an entry to the list that */
   /* has the same attributes as the Entry passed into this function.   */
   /* This function will return NULL if NO Entry was added.  This can   */
   /* occur if the element passed in was deemed invalid or the actual   */
   /* List Head was invalid.                                            */
   /* ** NOTE ** This function does not insert duplicate entries into   */
   /*            the list.  An element is considered a duplicate if the */
   /*            BD_ADDR field is the same as an entry already in       */
   /*            the list.  When this occurs, this function returns     */
   /*            NULL.                                                  */
static ConnectionEntry_t *AddConnectionEntry(ConnectionEntry_t **ConnectionEntryList, ConnectionEntry_t *ConnectionEntry)
{
   ConnectionEntry_t *AddedEntry = NULL;
   ConnectionEntry_t *tmpEntry;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* First let's verify that values passed in are semi-valid.          */
   if((ConnectionEntryList) && (ConnectionEntry))
   {
      /* Make sure that the element that we are adding seems semi-valid.*/
      if(!COMPARE_NULL_BD_ADDR(ConnectionEntry->BD_ADDR))
      {
         /* OK, data seems semi-valid, let's allocate a new data        */
         /* structure to add to the list.                               */
         AddedEntry = (ConnectionEntry_t *)BTPS_AllocateMemory(sizeof(ConnectionEntry_t));

         if(AddedEntry)
         {
            /* Copy All Data over.                                      */
            *AddedEntry                     = *ConnectionEntry;

            /* Now Add it to the end of the list.                       */
            AddedEntry->NextConnectionEntry = NULL;

            /* First, let's check to see if there are any elements      */
            /* already present in the List that was passed in.          */
            if((tmpEntry = *ConnectionEntryList) != NULL)
            {
               /* Head Pointer was not NULL, so we will traverse the    */
               /* list until we reach the last element.                 */
               while(tmpEntry)
               {
                  if(COMPARE_BD_ADDR(tmpEntry->BD_ADDR, AddedEntry->BD_ADDR))
                  {
                     /* Entry was already added, so free the memory and */
                     /* flag an error to the caller.                    */
                     FreeConnectionEntryMemory(AddedEntry);
                     AddedEntry = NULL;

                     /* Abort the Search.                               */
                     tmpEntry   = NULL;
                  }
                  else
                  {
                     /* OK, we need to see if we are at the last element*/
                     /* of the List.  If we are, we simply break out of */
                     /* the list traversal because we know there are NO */
                     /* duplicates AND we are at the end of the list.   */
                     if(tmpEntry->NextConnectionEntry)
                        tmpEntry = tmpEntry->NextConnectionEntry;
                     else
                        break;
                  }
               }

               if(AddedEntry)
               {
                  /* Last element found, simply Add the entry.          */
                  tmpEntry->NextConnectionEntry = AddedEntry;
               }
            }
            else
               *ConnectionEntryList = AddedEntry;
         }
      }
   }

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: 0x%08X\n", AddedEntry));

   return(AddedEntry);
}

   /* The following function searches the specified List for the        */
   /* specified Connection Entry.  This function returns NULL if either */
   /* the List Head is invalid, the BD_ADDR is invalid, or the specified*/
   /* Connection Entry was NOT found.                                   */
static ConnectionEntry_t *SearchConnectionEntry(ConnectionEntry_t **ConnectionEntryList, BD_ADDR_t BD_ADDR)
{
   ConnectionEntry_t *FoundEntry = NULL;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* Let's make sure the List and BD_ADDR to search for appear to be   */
   /* semi-valid.                                                       */
   if((ConnectionEntryList) && (!COMPARE_NULL_BD_ADDR(BD_ADDR)))
   {
      /* Now, let's search the list until we find the correct entry.    */
      FoundEntry = *ConnectionEntryList;

      while((FoundEntry) && (!COMPARE_BD_ADDR(FoundEntry->BD_ADDR, BD_ADDR)))
         FoundEntry = FoundEntry->NextConnectionEntry;
   }

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: 0x%08X\n", FoundEntry));

   return(FoundEntry);
}

   /* The following function searches the specified Connection Entry    */
   /* List for the specified Connection Entry and removes it from the   */
   /* List.  This function returns NULL if either the Connection Entry  */
   /* List Head is invalid, the BD_ADDR is invalid, or the specified    */
   /* Connection Entry was NOT present in the list.  The entry returned */
   /* will have the Next Entry field set to NULL, and the caller is     */
   /* responsible for deleting the memory associated with this entry by */
   /* calling FreeConnectionEntryMemory().                              */
static ConnectionEntry_t *DeleteConnectionEntry(ConnectionEntry_t **ConnectionEntryList, BD_ADDR_t BD_ADDR)
{
   ConnectionEntry_t *FoundEntry = NULL;
   ConnectionEntry_t *LastEntry  = NULL;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* Let's make sure the List and BD_ADDR to search for appear to be   */
   /* semi-valid.                                                       */
   if((ConnectionEntryList) && (!COMPARE_NULL_BD_ADDR(BD_ADDR)))
   {
      /* Now, let's search the list until we find the correct entry.    */
      FoundEntry = *ConnectionEntryList;

      while((FoundEntry) && (!COMPARE_BD_ADDR(FoundEntry->BD_ADDR, BD_ADDR)))
      {
         LastEntry  = FoundEntry;
         FoundEntry = FoundEntry->NextConnectionEntry;
      }

      /* Check to see if we found the specified entry.                  */
      if(FoundEntry)
      {
         /* OK, now let's remove the entry from the list.  We have to   */
         /* check to see if the entry was the first entry in the list.  */
         if(LastEntry)
         {
            /* Entry was NOT the first entry in the list.               */
            LastEntry->NextConnectionEntry = FoundEntry->NextConnectionEntry;
         }
         else
            *ConnectionEntryList = FoundEntry->NextConnectionEntry;

         FoundEntry->NextConnectionEntry = NULL;
      }
   }

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: 0x%08X\n", FoundEntry));

   return(FoundEntry);
}

   /* This function frees the specified Connection Entry member.  No    */
   /* check is done on this entry other than making sure it NOT NULL.   */
static void FreeConnectionEntryMemory(ConnectionEntry_t *ConnectionEntry)
{
   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   if(ConnectionEntry)
      BTPS_FreeMemory(ConnectionEntry);

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit\n"));
}

   /* The following function deletes (and free's all memory) every      */
   /* element of the specified Connection Entry List.  Upon return of   */
   /* this function, the Head Pointer is set to NULL.                   */
static void FreeConnectionEntryList(ConnectionEntry_t **ConnectionEntryList)
{
   ConnectionEntry_t *EntryToFree;
   ConnectionEntry_t *tmpEntry;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   if(ConnectionEntryList)
   {
      /* Simply traverse the list and free every element present.       */
      EntryToFree = *ConnectionEntryList;

      while(EntryToFree)
      {
         tmpEntry    = EntryToFree;
         EntryToFree = EntryToFree->NextConnectionEntry;

         FreeConnectionEntryMemory(tmpEntry);
      }

      /* Make sure the List appears to be empty.                        */
      *ConnectionEntryList = NULL;
   }

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit\n"));
}

#endif

static Byte_t ProcessRemoteControlCommand(unsigned int PassThroughID, Boolean_t ButtonState);
static void RegisterRemoteControlHIDEvents(Boolean_t Register);

   /* The following function is the Device Manager Callback function    */
   /* that is Registered to watch for Power On/Off events.              */
static void BTPSAPI DEVMEventCallback(DEVM_Event_Data_t *EventData, void *CallbackParameter)
{
   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter (SRVR)\n"));

   if(EventData)
   {
      switch(EventData->EventType)
      {
         case detDevicePoweredOn:
            DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Device Power On Occurred.\n"));

            BTPM_QueueMailboxCallback(BTPMDispatchCallback_Power, (void *)TRUE);
            break;
         case detDevicePoweringOff:
         case detDevicePoweredOff:
            DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Device Power Off Occurred.\n"));

            BTPM_QueueMailboxCallback(BTPMDispatchCallback_Power, (void *)FALSE);
            break;
         default:
            /* We are not interested in any other type of event.        */
            break;
      }
   }

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit (SRVR)\n"));
}

   /* The following function is the BTPM Event Callback that is         */
   /* registered to be notified after initialization.  This callback    */
   /* simply registers a Device Manager Callback to trap Power On/Power */
   /* Events.                                                           */
static void BTPSAPI BTPMDispatchCallback_Initialization(void *CallbackParameter)
{
   int Result;

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* Register the Device Manager Callback.                             */
   if((Result = DEVM_RegisterEventCallback(DEVMEventCallback, NULL)) > 0)
   {
      /* Callback registered.                                           */
      DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Device Manager Callback Registered.\n"));

      /* Note the Callback ID.                                          */
      DEVMEventCallbackID = (unsigned int)Result;

      /* Finally, check to see if the device is already powered on.     */
      if(DEVM_QueryDevicePowerState())
      {
         DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Device is already powered on.\n"));

         /* Device is already powered on, go ahead and register for     */
         /* Remote Control Events.                                      */
         RegisterRemoteControlHIDEvents(TRUE);
      }
   }
   else
   {
      /* Unable to register the Device Manager Power Callback.          */
      DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Device Manager Callback NOT Registered: %d.\n", Result));
   }

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));
}

   /* The following function is the BTPM Event Callback that is         */
   /* registered to process device Power On/Off Messages.               */
static void BTPSAPI BTPMDispatchCallback_Power(void *CallbackParameter)
{
   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter (SRVR)\n"));

   /* Simply Register/Un-Register for Remote Control Events based on the*/
   /* current Power setting.                                            */
   RegisterRemoteControlHIDEvents((Boolean_t)(CallbackParameter?TRUE:FALSE));

   /* If the device is powering off, we need to acknowledge that we are */
   /* finished with our processing.                                     */
   if(!CallbackParameter)
   {
      /* All finished, acknowledge that we have completed our power down*/
      /* procedure.                                                     */
      DEVM_AcknowledgeDevicePoweringDown(DEVMEventCallbackID);
   }

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit (SRVR)\n"));
}

#if SS1_SUPPORT_A2DP

   /* The following function is responsible for processing All Remote   */
   /* Control Command Events that are received.                         */
static void BTPSAPI AUDMEventCallback(AUDM_Event_Data_t *EventData, void *CallbackParameter)
{
   AUD_Remote_Control_Response_Data_t ResponseData;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* Check to see if this a Remote Control Command Indication Event.   */
   if((EventData) && (EventData->EventType == aetRemoteControlCommandIndication) && (EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageType == amtPassThrough))
   {
      DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_VERBOSE), ("Remote Control Pass-Through command received: 0x%02X, State: %s\n", EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID, EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.StateFlag?"DOWN":"UP"));

      /* Format up Common Response values.                              */
      ResponseData.MessageType                                             = amtPassThrough;

      ResponseData.MessageData.PassThroughResponseData.SubunitType         = EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.SubunitType;
      ResponseData.MessageData.PassThroughResponseData.SubunitID           = EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.SubunitID;
      ResponseData.MessageData.PassThroughResponseData.OperationID         = EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.OperationID;
      ResponseData.MessageData.PassThroughResponseData.StateFlag           = EventData->EventData.RemoteControlCommandIndicationEventData.RemoteControlCommandData.MessageData.PassThroughCommandData.StateFlag;
      ResponseData.MessageData.PassThroughResponseData.OperationDataLength = 0;
      ResponseData.MessageData.PassThroughResponseData.OperationData       = NULL;

      /* Remote Control Command received, now let's process it.         */
      ResponseData.MessageData.PassThroughResponseData.ResponseCode = ProcessRemoteControlCommand(ResponseData.MessageData.PassThroughResponseData.OperationID, ResponseData.MessageData.PassThroughResponseData.StateFlag);

      DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_VERBOSE), ("Responding to Pass-Through command: 0x%02X\n", ResponseData.MessageData.PassThroughResponseData.ResponseCode));

      /* Respond to the Command.                                        */
      AUDM_Send_Remote_Control_Response(RemoteControlCallbackID, EventData->EventData.RemoteControlCommandIndicationEventData.RemoteDeviceAddress, EventData->EventData.RemoteControlCommandIndicationEventData.TransactionID, &ResponseData);
   }

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit\n"));
}

#endif

#if ((SS1_PLATFORM_SDK_VERSION >= 14) && (SS1_SUPPORT_HID))

   /* The following function is responsible for processing All HID      */
   /* Events that are received.                                         */
static void BTPSAPI HIDMEventCallback(HIDM_Event_Data_t *EventData, void *CallbackParameter)
{
   int                              Result;
   Byte_t                           DeviceSubclass;
   Boolean_t                        SendLockStateReport;
   unsigned int                     RecordIndex;
   unsigned int                     AttributeIndex;
   unsigned int                     ServiceDataLength;
   unsigned char                   *ServiceDataBuffer;
   ConnectionEntry_t                ConnectionEntry;
   ConnectionEntry_t               *ConnectionEntryPtr;
   DEVM_Parsed_SDP_Data_t           ParsedServices;
   DEVM_Remote_Device_Properties_t  RemoteDevProps;

   union
   {
      UUID_16_t  UUID_16;
      UUID_32_t  UUID_32;
      UUID_128_t UUID_128;
   } TempUUID;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* Check to see if this a Remote Control Command Indication Event.   */
   if(EventData)
   {
      if(EventData->EventType == hetHIDBootKeyboardKeyPress)
      {
         /* Determine if we need to add a HID Keyboard device.          */
         if((ConnectionEntryPtr = SearchConnectionEntry(&ConnectionEntryList, EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress)) == NULL)
         {
            /* Add the Connection Entry.                                */
            ConnectionEntry.BD_ADDR             = EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress;
            ConnectionEntry.NextConnectionEntry = NULL;

            if((ConnectionEntryPtr = AddConnectionEntry(&ConnectionEntryList, &ConnectionEntry)) != NULL)
            {
               if((Result = UINPUT_EnableKeyboard()) == 0)
                  ConnectionEntryPtr->Flags |= CONNECTION_ENTRY_FLAGS_HID_KEYBOARD_ACTIVE;
               else
               {
                  if((ConnectionEntryPtr = DeleteConnectionEntry(&ConnectionEntryList, ConnectionEntry.BD_ADDR)) != NULL)
                     FreeConnectionEntryMemory(ConnectionEntryPtr);
               }
            }
            else
               Result = -1;
         }
         else
         {
            if((ConnectionEntryPtr) && (!(ConnectionEntryPtr->Flags & CONNECTION_ENTRY_FLAGS_HID_KEYBOARD_ACTIVE)))
            {
               if((Result = UINPUT_EnableKeyboard()) == 0)
                  ConnectionEntryPtr->Flags |= CONNECTION_ENTRY_FLAGS_HID_KEYBOARD_ACTIVE;
            }
            else
               Result = 0;
         }

         //DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Keyboard Key received: 0x%02X, State: %s\n", EventData->EventData.BootKeyboardKeyPressEventData.Key, EventData->EventData.BootKeyboardKeyPressEventData.KeyDown?"DOWN":"UP"));
         DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Keyboard Key received: State: %s\n", EventData->EventData.BootKeyboardKeyPressEventData.KeyDown?"DOWN":"UP"));

         /* All that is left to do is to send the key to the input      */
         /* sub-system.                                                 */
         if(!Result)
         {
            UINPUT_SendKeyboardBootEvent(EventData->EventData.BootKeyboardKeyPressEventData.Key, EventData->EventData.BootKeyboardKeyPressEventData.KeyDown);

            /* Send an output report indicating the current state of    */
            /* scroll, caps and num lock keys.                          */
            /*                                                          */
            /*      6.2  Keyboard Boot Protocol Output Report           */
            /*                                                          */
            /*      +----------------------------------------------+    */
            /*      | BYTE | D7 | D6 | D5 | D4 | D3 | D2 | D1 | D0 |    */
            /*      |----------------------------------------------|    */
            /*      |  0   |     ---Report ID = 0x01---            |    */
            /*      +------+---------------------------------------+    */
            /*      |   1  |  0    0    0    A    B    C    D    E |    */
            /*      +----------------------------------------------+    */
            /*                                                          */
            /*    Where:                                                */
            /*                                                          */
            /*       A == Kana                                          */
            /*       B == Compose                                       */
            /*       C == Scroll Lock                                   */
            /*       D == CAPS Lock                                     */
            /*       E == NUM Lock                                      */
            /*                                                          */
            if(EventData->EventData.BootKeyboardKeyPressEventData.KeyDown)
            {
               SendLockStateReport = FALSE;
               LockStateReport[0]  = 0x01;

               if(EventData->EventData.BootKeyboardKeyPressEventData.Key == HID_HOST_KEYBOARD_CAPS_LOCK)
               {
                  LockStateReport[1]  ^= KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_CAPS;

                  SendLockStateReport  = TRUE;
               }

               if(EventData->EventData.BootKeyboardKeyPressEventData.Key == HID_HOST_KEYPAD_NUM_LOCK)
               {
                  LockStateReport[1]  ^= KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_NUM;

                  SendLockStateReport  = TRUE;
               }

               if(EventData->EventData.BootKeyboardKeyPressEventData.Key == HID_HOST_KEYBOARD_SCROLL_LOCK)
               {
                  LockStateReport[1] ^= KEYBOARD_BOOT_PROTOCOL_OUTPUT_REPORT_SCROLL;

                  SendLockStateReport = TRUE;
               }

               if(SendLockStateReport)
               {
                  if((Result = HIDM_Send_Report_Data(HIDDataCallbackID, EventData->EventData.BootKeyboardKeyPressEventData.RemoteDeviceAddress, 2, LockStateReport)) != 0)
                     DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HIDM_Send_Report_Data(): %d.\r\n", Result));
               }
            }
         }
      }
      else
      {
         if(EventData->EventType == hetHIDBootMouseEvent)
         {
            /* Determine if we need to add a HID Mouse device.          */
            if((ConnectionEntryPtr = SearchConnectionEntry(&ConnectionEntryList, EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress)) == NULL)
            {
               /* Add the Connection Entry.                             */
               ConnectionEntry.BD_ADDR             = EventData->EventData.BootMouseEventEventData.RemoteDeviceAddress;
               ConnectionEntry.NextConnectionEntry = NULL;

               if((ConnectionEntryPtr = AddConnectionEntry(&ConnectionEntryList, &ConnectionEntry)) != NULL)
               {
                  if((Result = UINPUT_EnableMouse()) == 0)
                     ConnectionEntryPtr->Flags |= CONNECTION_ENTRY_FLAGS_HID_MOUSE_ACTIVE;
                  else
                  {
                     if((ConnectionEntryPtr = DeleteConnectionEntry(&ConnectionEntryList, ConnectionEntry.BD_ADDR)) != NULL)
                        FreeConnectionEntryMemory(ConnectionEntryPtr);
                  }
               }
               else
                  Result = -1;
            }
            else
            {
               if((ConnectionEntryPtr) && (!(ConnectionEntryPtr->Flags & CONNECTION_ENTRY_FLAGS_HID_MOUSE_ACTIVE)))
               {
                  if((Result = UINPUT_EnableMouse()) == 0)
                     ConnectionEntryPtr->Flags |= CONNECTION_ENTRY_FLAGS_HID_MOUSE_ACTIVE;
               }
               else
                  Result = 0;
            }

            DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Mouse received: %d\n", Result));

            /* All that is left to do is to send the key to the input   */
            /* sub-system.                                              */
            if(!Result)
               UINPUT_SendMouseBootEvent(EventData->EventData.BootMouseEventEventData.ButtonState, EventData->EventData.BootMouseEventEventData.CX, EventData->EventData.BootMouseEventEventData.CY, &EventData->EventData.BootMouseEventEventData.CZ);
         }
         else
         {
            if(EventData->EventType == hetHIDDeviceConnected)
            {
               DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Device Connected Event received\n"));

               /* Add the Connection Entry.                                */
               BTPS_MemInitialize(&ConnectionEntry, 0, sizeof(ConnectionEntry));
               ConnectionEntry.BD_ADDR = EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress;

               if((ConnectionEntryPtr = AddConnectionEntry(&ConnectionEntryList, &ConnectionEntry)) != NULL)
               {
                  DeviceSubclass = 0;

                  if((!DEVM_QueryRemoteDeviceProperties(ConnectionEntryPtr->BD_ADDR, 0, &RemoteDevProps)) && (GET_MAJOR_DEVICE_CLASS(RemoteDevProps.ClassOfDevice) == HCI_LMP_CLASS_OF_DEVICE_MAJOR_DEVICE_CLASS_PERIPHERAL))
                  {
                     DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Device Subclass located via Class of Device.\n"));

                     DeviceSubclass = GET_MINOR_DEVICE_CLASS(RemoteDevProps.ClassOfDevice);
                  }
                  else
                  {
                     DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("Class of Device of the connected HID peripheral is unknown or is not a 'peripheral' type. Checking SDP cache.\n"));

                     /* Pull the cached SDP record data for this device.*/
                     if(DEVM_QueryRemoteDeviceServices(ConnectionEntryPtr->BD_ADDR, 0, 0, NULL, &ServiceDataLength) >= 0)
                     {
                        if((ServiceDataBuffer = BTPS_AllocateMemory(ServiceDataLength)) != NULL)
                        {
                           if(DEVM_QueryRemoteDeviceServices(ConnectionEntryPtr->BD_ADDR, 0, ServiceDataLength, ServiceDataBuffer, NULL) == (int)ServiceDataLength)
                           {
                              if(!DEVM_ConvertRawSDPStreamToParsedSDPData(ServiceDataLength, ServiceDataBuffer, &ParsedServices))
                              {
                                 /* Scan the SDP records until the HID  */
                                 /* record is located and the Device    */
                                 /* Subclass field is extracted.        */
                                 for(RecordIndex = 0; ((RecordIndex < ParsedServices.NumberServiceRecords) && (!DeviceSubclass)); RecordIndex++)
                                 {
                                    /* Locate Service Class ID          */
                                    /* attribute.                       */
                                    for(AttributeIndex = 0; AttributeIndex < ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values; AttributeIndex++)
                                    {
                                       if(ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].Attribute_ID == SDP_ATTRIBUTE_ID_SERVICE_CLASS_ID_LIST)
                                          break;
                                    }

                                    /* Confirm that the Service Class ID*/
                                    /* attribute was found.             */
                                    if(AttributeIndex < ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values)
                                    {
                                       /* Check whether this is a HID   */
                                       /* record.                       */
                                       if((ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element_Type == deSequence) && (ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element_Length == 1))
                                       {
                                          /* If the HID Service class   */
                                          /* is not found, indicate     */
                                          /* this by invalidating the   */
                                          /* AttributeIndex.            */
                                          switch(ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element_Type)
                                          {
                                             case deUUID_16:
                                                SDP_ASSIGN_HID_PROFILE_UUID_16(TempUUID.UUID_16);

                                                if(!COMPARE_UUID_16(TempUUID.UUID_16, ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_16))
                                                   AttributeIndex = ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values;
                                                break;
                                             case deUUID_32:
                                                SDP_ASSIGN_HID_PROFILE_UUID_32(TempUUID.UUID_32);

                                                if(!COMPARE_UUID_32(TempUUID.UUID_32, ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_32))
                                                   AttributeIndex = ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values;
                                                break;
                                             case deUUID_128:
                                                SDP_ASSIGN_HID_PROFILE_UUID_128(TempUUID.UUID_128);

                                                if(!COMPARE_UUID_128(TempUUID.UUID_128, ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_128))
                                                   AttributeIndex = ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values;
                                                break;
                                             default:
                                                AttributeIndex = ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values;
                                                break;
                                          }

                                          if(AttributeIndex < ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values)
                                          {
                                             /* AttributeIndex is still */
                                             /* valid, so this is a HID */
                                             /* record. Next, locate    */
                                             /* the HID Device Subclass */
                                             /* attribute.              */
                                             for(AttributeIndex = 0; AttributeIndex < ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values; AttributeIndex++)
                                             {
                                                if(ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].Attribute_ID == SDP_ATTRIBUTE_ID_HID_DEVICE_SUBCLASS)
                                                   break;
                                             }

                                             /* Confirm that the        */
                                             /* HID Device Subclass     */
                                             /* attribute was found.    */
                                             if(AttributeIndex < ParsedServices.SDPServiceAttributeResponseData[RecordIndex].Number_Attribute_Values)
                                             {
                                                if(ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element_Type == deUnsignedInteger1Byte)
                                                {
                                                   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Device Subclass located via SDP record.\n"));

                                                   DeviceSubclass = (ParsedServices.SDPServiceAttributeResponseData[RecordIndex].SDP_Service_Attribute_Value_Data[AttributeIndex].SDP_Data_Element->SDP_Data_Element.UnsignedInteger1Byte >> 2);
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }

                                 DEVM_FreeParsedSDPData(&ParsedServices);
                              }
                           }

                           BTPS_FreeMemory(ServiceDataBuffer);
                        }
                     }
                  }

                  /* If the HID Device Subclass is valid, use it to     */
                  /* determine whether the device supports keyboard     */
                  /* and/or mouse input in advance of receiving any     */
                  /* events.                                            */
                  if(DeviceSubclass)
                  {
                     DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Device Subclass: 0x%02X.\n", DeviceSubclass));

                     /* If the device has a keyboard, go ahead and      */
                     /* register a keyboard input device.               */
                     if(DeviceSubclass & HCI_LMP_CLASS_OF_DEVICE_MINOR_DEVICE_CLASS_PERIPHERAL_KEYBOARD_MASK)
                     {
                        if((Result = UINPUT_EnableKeyboard()) == 0)
                           ConnectionEntryPtr->Flags |= CONNECTION_ENTRY_FLAGS_HID_KEYBOARD_ACTIVE;
                     }

                     /* If the device supports pointer input (mouse or  */
                     /* touchpad), register a mouse input device.       */
                     if(DeviceSubclass & HCI_LMP_CLASS_OF_DEVICE_MINOR_DEVICE_CLASS_PERIPHERAL_POINTING_DEVICE_MASK)
                     {
                        if((Result = UINPUT_EnableMouse()) == 0)
                           ConnectionEntryPtr->Flags |= CONNECTION_ENTRY_FLAGS_HID_MOUSE_ACTIVE;
                     }
                  }
                  else
                     DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Device Subclass unknown. Input devices will be initialized as HID events are received.\n"));
               }
               else
                  DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("Unable to add HID device to connection list. This shouldn't happen, but will retry when the first HID event is received\n"));
            }
            else
            {
               if(EventData->EventType == hetHIDDeviceDisconnected)
               {
                  DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Device Disconnected Event received\n"));

                  /* Determine if we need to disable a Keyboard and/or a*/
                  /* Mouse.                                             */
                  if((ConnectionEntryPtr = DeleteConnectionEntry(&ConnectionEntryList, EventData->EventData.DeviceDisconnectedEventData.RemoteDeviceAddress)) != NULL)
                  {
                     if(ConnectionEntryPtr->Flags & CONNECTION_ENTRY_FLAGS_HID_KEYBOARD_ACTIVE)
                     {
                        Result = UINPUT_DisableKeyboard(FALSE);

                        DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Keyboard Disconnected Event received: %d\n", Result));
                     }

                     if(ConnectionEntryPtr->Flags & CONNECTION_ENTRY_FLAGS_HID_MOUSE_ACTIVE)
                     {
                        Result = UINPUT_DisableMouse(FALSE);

                        DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Mouse Disconnected Event received: %d\n", Result));
                     }

                     FreeConnectionEntryMemory(ConnectionEntryPtr);
                  }
               }
               else
               {
                  /* Unknown/un-handled event received.                 */
                  DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("HID Keyboard Event received: %d (0x%08X)\n", EventData->EventType, EventData->EventType));
               }
            }
         }
      }
   }

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit\n"));
}

#endif /* SS1_PLATFORM_SDK_VERSION >= 14 */

#if SS1_SUPPORT_A2DP

   /* The following function is actually responsible for performing the */
   /* Remote Control Action specified by the PassThroughID.  This       */
   /* function returns the AVRCP Pass-Through Response Code that is to  */
   /* be returned in the response to the original Pass-Through Command. */
static Byte_t ProcessRemoteControlCommand(unsigned int PassThroughID, Boolean_t ButtonState)
{
   Byte_t ret_val;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   if(UINPUT_SendRemoteControlPassthroughEvent(PassThroughID, ButtonState) == 0)
      ret_val = AVRCP_RESPONSE_ACCEPTED;
   else
      ret_val = AVRCP_RESPONSE_NOT_IMPLEMENTED;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

#endif

   /* The following function is responsible for                         */
   /* registering/un-registering for Remote Control Events in the       */
   /* system.                                                           */
static void RegisterRemoteControlHIDEvents(Boolean_t Register)
{
   int Result;

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter: %d\n", Register));

   /* Regardless if we are Registering or Un-Registering, make sure that*/
   /* we are currently Un-Registered and the uinput device is closed.   */

#if SS1_SUPPORT_A2DP

   if(RemoteControlCallbackID)
      AUDM_Un_Register_Remote_Control_Event_Callback(RemoteControlCallbackID);

#endif

#if ((SS1_PLATFORM_SDK_VERSION >= 14) && (SS1_SUPPORT_HID))

   if(HIDDataCallbackID)
      HIDM_Un_Register_Data_Event_Callback(HIDDataCallbackID);

#endif

#if ((SS1_SUPPORT_A2DP) || (SS1_SUPPORT_HID))

   if((RemoteControlCallbackID) || (HIDDataCallbackID))
      UINPUT_Cleanup();

#endif

   /* Flag that we have not registered for Remote Control Events.       */
   RemoteControlCallbackID = 0;
   HIDDataCallbackID       = 0;

   /* If we have been instructed to Register for Events, then we will do*/
   /* at this time.                                                     */
   if(Register)
   {

#if ((SS1_SUPPORT_A2DP) || (SS1_SUPPORT_HID))

      /* Initialize the uinput device.                                  */
      if((Result = UINPUT_Initialize()) == 0)
      {

#if SS1_SUPPORT_A2DP

         /* Simply Register for Remote Control Events.                  */
         if((Result = AUDM_Register_Remote_Control_Event_Callback(AUDM_REGISTER_REMOTE_CONTROL_DATA_SERVICE_TYPE_TARGET, AUDMEventCallback, NULL)) > 0)
         {
            DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Registered for Remote Control Events.\n"));

            /* Note the Remote Control Callback ID.                     */
            RemoteControlCallbackID = (unsigned int)Result;
         }
         else
         {
            /* Error registering for Remote Control Events.             */
            DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Error Registering for Remote Control Events: %d.\n", Result));
         }

#endif

#if ((SS1_PLATFORM_SDK_VERSION >= 14) && (SS1_SUPPORT_HID))

         /* Simply Register for HID Host Events.                        */
         if((Result = HIDM_Register_Data_Event_Callback(HIDMEventCallback, NULL)) > 0)
         {
            DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Registered for HID Events.\n"));

            /* Note the HID Data Callback ID.                           */
            HIDDataCallbackID = (unsigned int)Result;
         }
         else
         {
            /* Error registering for HID Events.                        */
            DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Error Registering for HID Events: %d.\n", Result));
         }

#endif

         /* Clean up uinput device.                                     */
         if((!RemoteControlCallbackID) && (!HIDDataCallbackID))
            UINPUT_Cleanup();

         /* Free any Remote Devices that might be present in the device */
         /* list.                                                       */
         FreeConnectionEntryList(&ConnectionEntryList);
      }
      else
      {
         /* Error creating the uinput device.                           */
         DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_VERBOSE), ("Error Creating UInput Virtual Input Device: %d.\n", Result));
      }

#endif  /* ((SS1_SUPPORT_A2DP) || (SS1_SUPPORT_HID)) */

   }

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit\n"));
}

   /* Main Program Entry Point.                                         */
int main(int argc, char *argv[])
{
   int                                ret_val;
   BTPM_Initialization_Info_t         InitializationInfo;
   DEVM_Initialization_Data_t         DeviceManagerInitializationInfo;
   BTPM_Debug_Initialization_Data_t   DebugInitializationInfo;
   DEVM_Default_Initialization_Data_t DefaultInitializationData;

/***************************/
   {
      unsigned int deviceMajor = 0;
      char deviceName[256];
      char line[256];
      ssize_t lineLength;
      FILE *devicesFile;

      /* Scan the /proc/devices file for the major number of the VNET   */
      /* driver.                                                        */
      if((devicesFile = fopen("/proc/devices", "r")) != NULL)
      {
         while(fgets(line, 256, devicesFile) != NULL)
         {
            lineLength = strlen(line);

            if(lineLength > 0)
            {
               if(sscanf(line, "%u%s", &deviceMajor, deviceName) == 2)
               {
                  if(strcmp(deviceName, "SS1VNET") == 0)
                     break;
                  else
                     deviceMajor = 0;
               }
            }
         }

         fclose(devicesFile);
      }
      else
         printf("Unable to open /proc/devices\n");

      if(deviceMajor > 0)
      {
         /* First, attempt to delete any existing device file.          */
         unlink("/dev/SS1VNET0");

         /* Now, create a new device file linked to the driver.         */
         if(mknod("/dev/SS1VNET0", (S_IFCHR | S_IRUSR | S_IWUSR), makedev(deviceMajor, 0)) == 0)
            printf("VNET device 0 created for major %u minor %u\n", deviceMajor, 0);
         else
            printf("Warning: failed to create VNET device 0\n");
      }
      else
         printf("SS1VNET driver not found\n");
   }
/***************************/

   /* Let's go ahead and specify a Platform Specific Android Debugging  */
   /* string.                                                           */
   DebugInitializationInfo.PlatformSpecificInitData                   = "SS1BTPMS";

   /* Set up the default configuration values for the Device Manager.   */
   BTPS_MemInitialize(&DefaultInitializationData, 0, sizeof(DefaultInitializationData));

   DefaultInitializationData.InitializationOverrideFlags              = (DEVM_INITIALIZATION_DATA_OVERRIDE_FLAGS_CLASS_OF_DEVICE | DEVM_INITIALIZATION_DATA_OVERRIDE_FLAGS_DEVICE_ID_INFO_VALID | DEVM_INITIALIZATION_DATA_OVERRIDE_FLAGS_DEVICE_ID_INFO);

   /* Set Class of Device.                                              */
   ASSIGN_CLASS_OF_DEVICE(DefaultInitializationData.DefaultClassOfDevice, 0x3E, 0x07, 0x14);

   DefaultInitializationData.DefaultDeviceIDInformationValid          = TRUE;

   /* Either the Vendor ID assigned by the USB Implementer's Forum, or  */
   /* the Company Identifier assigned by the Bluetooth SIG.             */
   DefaultInitializationData.DefaultDeviceIDInformation.VendorID      = 0x005E;

   /* Product identifier, chosen by the manufacturer, unique to this    */
   /* product, such as an internal model number. When new features are  */
   /* added to a device, this ID should be changed.                     */
   DefaultInitializationData.DefaultDeviceIDInformation.ProductID     = 0x1234;

   /* The reversion of this product, in BCD. The version number JJ.M.N  */
   /* would be encoded as 0xJJMN. This revision number should be updated*/
   /* for bug fixes or enhancements to existing features.               */
   DefaultInitializationData.DefaultDeviceIDInformation.DeviceVersion = 0x0100;

   /* If the VendorID used above was assigned by the USB Imlpementer's  */
   /* Forum, set this to TRUE.                                          */
   DefaultInitializationData.DefaultDeviceIDInformation.USBVendorID   = FALSE;

   /* Set up the Device Manager initialization structure.               */
   BTPS_MemInitialize(&DeviceManagerInitializationInfo, 0, sizeof(DeviceManagerInitializationInfo));

   DeviceManagerInitializationInfo.DefaultInitializationData = &DefaultInitializationData;

   /* Now that we have initialized the per-module information, let's go */
   /* ahead and initialize the correct global configuration structure.  */
   BTPS_MemInitialize(&InitializationInfo, 0, sizeof(InitializationInfo));

   InitializationInfo.DebugInitializationInfo         = &DebugInitializationInfo;
   InitializationInfo.DeviceManagerInitializationInfo = &DeviceManagerInitializationInfo;

   BTPM_DebugSetZoneMask((BTPM_SERVICE_DEBUG_ZONES | BTPM_SERVICE_DEBUG_LEVELS));
   BTPM_DebugSetZoneMask((BTPM_SERVICE_DEBUG_ZONES_PAGE_1 | BTPM_SERVICE_DEBUG_LEVELS));
   BTPM_DebugSetZoneMask((BTPM_SERVICE_DEBUG_ZONES_PAGE_2 | BTPM_SERVICE_DEBUG_LEVELS));

   /* Do nothing other than call the Library entry point.               */
   ret_val = BTPM_Main(&InitializationInfo, BTPMDispatchCallback_Initialization, NULL);

   return(ret_val);
}

