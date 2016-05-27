/*****< uinput.c >*************************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  UINPUT - Module for Stonestreet One Bluetopia Platform Manager to         */
/*           translate AVRCP/HID events into Linux UINPUT events.             */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   10/27/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#include <string.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <unistd.h>

#include "SS1CFG.h"

#include "SS1BTPM.h"

#if SS1_SUPPORT_A2DP
#include "SS1BTAUDM.h"
#endif

#if SS1_SUPPORT_HID
#include "SS1BTHIDM.h"
#endif

#include "BTPSKRNL.h"

#include "UINPUT.h"

   /* The following enumerated type represents the defined input device */
   /* types that are supported by this module.  This value governs how  */
   /* the device is presented to the UINPUT sub-system.                 */
typedef enum
{
   dtRemoteControl,
   dtKeyboard,
   dtMouse
} InputDeviceType_t;

   /* The following structure represents a mapping between an AVRCP     */
   /* Passthrough Operation ID and a virtual keycode defined by the     */
   /* Linux input subsystem.                                            */
typedef struct _tagKeyBinding_t
{
   Byte_t AVRCPOperationId;
   Word_t UINPUTKeycode;
} KeyBinding_t;

   /* Key codes this module supports. These constants are defined in the*/
   /* <linux/input.h> header.                                           */
static BTPSCONST KeyBinding_t SupportedKeys[] =
{
#if SS1_SUPPORT_A2DP
   { AVRCP_PASS_THROUGH_ID_PLAY,         KEY_PLAYCD       },
   { AVRCP_PASS_THROUGH_ID_PAUSE,        KEY_PAUSECD      },
   { AVRCP_PASS_THROUGH_ID_STOP,         KEY_STOPCD       },
   { AVRCP_PASS_THROUGH_ID_BACKWARD,     KEY_PREVIOUSSONG },
   { AVRCP_PASS_THROUGH_ID_FORWARD,      KEY_NEXTSONG     },
   { AVRCP_PASS_THROUGH_ID_REWIND,       KEY_REWIND       },
   { AVRCP_PASS_THROUGH_ID_FAST_FORWARD, KEY_FASTFORWARD  },
#endif
   { 0,                                  0                }
} ;

   /* HID key lookup table.                                             */

   /* The first field of each entry is the mapped Key Code (0x00 if     */
   /* none).                                                            */
static BTPSCONST Byte_t HIDKeyLookupTable[] =
{
   KEY_A,
   KEY_B,
   KEY_C,
   KEY_D,
   KEY_E,
   KEY_F,
   KEY_G,
   KEY_H,
   KEY_I,
   KEY_J,
   KEY_K,
   KEY_L,
   KEY_M,
   KEY_N,
   KEY_O,
   KEY_P,
   KEY_Q,
   KEY_R,
   KEY_S,
   KEY_T,
   KEY_U,
   KEY_V,
   KEY_W,
   KEY_X,
   KEY_Y,
   KEY_Z,
   KEY_1,
   KEY_2,
   KEY_3,
   KEY_4,
   KEY_5,
   KEY_6,
   KEY_7,
   KEY_8,
   KEY_9,
   KEY_0,
   KEY_ENTER,
   KEY_ESC,
   KEY_BACKSPACE,
   KEY_TAB,
   KEY_SPACE,
   KEY_MINUS,
   KEY_EQUAL,
   KEY_LEFTBRACE,
   KEY_RIGHTBRACE,
   KEY_BACKSLASH,
   0,
   KEY_SEMICOLON,
   KEY_APOSTROPHE,
   KEY_GRAVE,
   KEY_COMMA,
   KEY_DOT,
   KEY_SLASH,
   KEY_CAPSLOCK,
   KEY_F1,
   KEY_F2,
   KEY_F3,
   KEY_F4,
   KEY_F5,
   KEY_F6,
   KEY_F7,
   KEY_F8,
   KEY_F9,
   KEY_F10,
   KEY_F11,
   KEY_F12,
   KEY_PRINT,
   KEY_SCROLLLOCK,
   KEY_PAUSE,
   KEY_INSERT,
   KEY_HOME,
   KEY_PAGEUP,
   KEY_DELETE,
   KEY_END,
   KEY_PAGEDOWN,
   KEY_RIGHT,
   KEY_LEFT,
   KEY_DOWN,
   KEY_UP,
   KEY_NUMLOCK,
   KEY_KPSLASH,
   KEY_KPASTERISK,
   KEY_KPMINUS,
   KEY_KPPLUS,
   KEY_KPENTER,
   KEY_KP1,
   KEY_KP2,
   KEY_KP3,
   KEY_KP4,
   KEY_KP5,
   KEY_KP6,
   KEY_KP7,
   KEY_KP8,
   KEY_KP9,
   KEY_KP0,
   KEY_KPDOT,
   0,
   0,

   KEY_LEFTCTRL,
   KEY_LEFTSHIFT,
   KEY_LEFTALT,
   KEY_LEFTMETA,
   KEY_RIGHTCTRL,
   KEY_RIGHTSHIFT,
   KEY_RIGHTALT,
   KEY_RIGHTMETA,
   0
} ;

   /* Handle to the uinput device file.  This doubles as an indicator of*/
   /* module initialization: valid values are non-zero positive integers*/
   /* while anything else indicates that the module has not been        */
   /* initialized.                                                      */
static int UINPUTDeviceHandle_AVRCP = (-1);

   /* Handle to the uinput device file (keyboard only).  valid values   */
   /* are non-zero positive integers while anything else indicates that */
   /* the input device has not been initialized.                        */
static int UINPUTDeviceHandle_Keyboard = (-1);

   /* Handle to the uinput device file (mouse only).  valid values are  */
   /* non-zero positive integers while anything else indicates that the */
   /* input device has not been initialized.                            */
static int UINPUTDeviceHandle_Mouse = (-1);

   /* Mutex which guards access to the Keyboard and Mouse Count (which  */
   /* contains the total number of virtual keyboards/Mice that are      */
   /* currently in the system).                                         */
static Mutex_t Mutex;

   /* Variable which holds the current number of virtual keyboards that */
   /* are currently present in the system.                              */
static unsigned int KeyboardCount = 0;

   /* Variable which holds the current number of virtual keyboards that */
   /* are currently present in the system.                              */
static unsigned int MouseCount = 0;

   /* Internal function prototypes.                                     */
static int OpenUINPUTDevice(void);
static int CreateInputDevice(int UINPUTDeviceFD, InputDeviceType_t InputDeviceType);
static int SendUINPUTKeyEvent(Boolean_t Keyboard, Word_t Keycode, Boolean_t Pressed);
static int SendUINPUTMouseEvent(Word_t Type, Word_t Code, SDWord_t Value, Boolean_t SyncInput);

   /* The following function is a utility function that exists to open  */
   /* the UINPUT device in the system.  This function attempts to find  */
   /* the right device name to open (as the device file varies in       */
   /* location depending upon the Linux distribution and/or kernel      */
   /* version).  This function returns a non-negative value which       */
   /* represents the File Descriptor if successful, or a negative value */
   /* if there was an error.                                            */
static int OpenUINPUTDevice(void)
{
   int ret_val;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* Open the device.  The location of the device file varies among    */
   /* distributions and Linux kernel versions, so try all common        */
   /* possibilities.                                                    */
   if((ret_val = open("/dev/uinput", O_WRONLY | O_NDELAY)) < 0)
   {
      if((ret_val = open("/dev/input/uinput", O_WRONLY | O_NDELAY)) < 0)
         ret_val = open("/dev/misc/uinput", O_WRONLY | O_NDELAY);
   }

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is a utility function that exists to create*/
   /* the actual Input Device that is to be bound the the specified     */
   /* UINPUT device (specified by the first parameter).  This function  */
   /* returns zero on success or a negative return value if there was an*/
   /* error.                                                            */
static int CreateInputDevice(int UINPUTDeviceFD, InputDeviceType_t InputDeviceType)
{
   int                     ret_val;
   BTPSCONST KeyBinding_t *Key;
   struct uinput_user_dev  UINPUTUserDevice;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter %d, %s\n", UINPUTDeviceFD, (InputDeviceType == dtKeyboard)?"Keyboard":((InputDeviceType == dtMouse)?"Mouse":"Remote Control")));

   /* First, make sure the input parameters appear to be semi-valid.    */
   if(UINPUTDeviceFD > 0)
   {
      memset(&UINPUTUserDevice, 0, sizeof(UINPUTUserDevice));

      UINPUTUserDevice.id.version = 4;
      UINPUTUserDevice.id.bustype = BUS_USB;

      if(InputDeviceType == dtMouse)
      {
         strncpy(UINPUTUserDevice.name, VIRTUAL_DEVICE_NAME_MOUSE, UINPUT_MAX_NAME_SIZE);

         /* Configure our input device for mouse events only.           */
         ioctl(UINPUTDeviceFD, UI_SET_EVBIT, EV_KEY);
         ioctl(UINPUTDeviceFD, UI_SET_EVBIT, EV_REL);

         /* First, take care of the buttons.                            */
         ioctl(UINPUTDeviceFD, UI_SET_KEYBIT, BTN_LEFT);
         ioctl(UINPUTDeviceFD, UI_SET_KEYBIT, BTN_RIGHT);
         ioctl(UINPUTDeviceFD, UI_SET_KEYBIT, BTN_MIDDLE);
         //XXX These buttons are not currently supported by Bluetopia
         //ioctl(UINPUTDeviceFD, UI_SET_KEYBIT, BTN_SIDE);
         //ioctl(UINPUTDeviceFD, UI_SET_KEYBIT, BTN_EXTRA);

         /* Next, the Relative X/Y axis.                                */
         ioctl(UINPUTDeviceFD, UI_SET_RELBIT, REL_X);
         ioctl(UINPUTDeviceFD, UI_SET_RELBIT, REL_Y);

         /* Finally, the relative Scroll Wheel.                         */
         ioctl(UINPUTDeviceFD, UI_SET_RELBIT, REL_WHEEL);
      }
      else
      {
         strncpy(UINPUTUserDevice.name, (InputDeviceType == dtKeyboard)?VIRTUAL_DEVICE_NAME_KEYBOARD:VIRTUAL_DEVICE_NAME, UINPUT_MAX_NAME_SIZE);

         /* Configure our input device for keyboard events only.        */
         ioctl(UINPUTDeviceFD, UI_SET_EVBIT, EV_KEY);

         /* Configure the keyboard buttons our input device will        */
         /* provide.                                                    */
         if(InputDeviceType == dtKeyboard)
         {
            /* Configure the Keyboard keys that we are supporting.      */
            for(ret_val = 0; ret_val < (int)(sizeof(HIDKeyLookupTable)); ret_val++)
            {
               if(HIDKeyLookupTable[ret_val])
                  ioctl(UINPUTDeviceFD, UI_SET_KEYBIT, HIDKeyLookupTable[ret_val]);
            }
         }
         else
         {
            Key = SupportedKeys;
            while((Key->AVRCPOperationId != 0) || (Key->UINPUTKeycode != 0))
            {
               ioctl(UINPUTDeviceFD, UI_SET_KEYBIT, Key->UINPUTKeycode);
               Key++;
            }
         }
      }

      /* Register our input device with the uinput driver.              */
      write(UINPUTDeviceFD, &UINPUTUserDevice, sizeof(UINPUTUserDevice));

      /* Actually create the device in the input system.                */
      if(ioctl(UINPUTDeviceFD, UI_DEV_CREATE) == 0)
         ret_val = 0;
      else
         ret_val = UINPUT_ERROR_UNABLE_TO_CREATE_VIRTUAL_DEVICE;
   }
   else
      ret_val = UINPUT_ERROR_UNABLE_TO_CREATE_VIRTUAL_DEVICE;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to submit an input event to the    */
   /* uinput driver for our virtual input device.  The first parameter  */
   /* is the virtual keycode as defined by the Linux input system.  The */
   /* second parameter determines whether the event is a key-pressed or */
   /* key-released event (TRUE for key-pressed, FALSE for key-released) */
   /* key-pressed).  A return value of 0 indicates success while a      */
   /* negative value is returned on error.                              */
static int SendUINPUTKeyEvent(Boolean_t Keyboard, Word_t Keycode, Boolean_t Pressed)
{
   int                ret_val;
   struct input_event event;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter (%d, %s)\n", Keycode, (Pressed == FALSE ? "FALSE" : "TRUE")));

   if(((Keyboard) && (UINPUTDeviceHandle_Keyboard >= 0)) || ((!Keyboard) && (UINPUTDeviceHandle_AVRCP >= 0)))
   {
      memset(&event, 0, sizeof(event));

      /* Send the actual button event.                                  */
      gettimeofday(&event.time, NULL);
      event.type  = EV_KEY;
      event.code  = Keycode;
      event.value = ((Pressed == FALSE) ? 0 : 1);

      if(write(Keyboard?UINPUTDeviceHandle_Keyboard:UINPUTDeviceHandle_AVRCP, &event, sizeof(event)) == sizeof(event))
      {
         /* Send a synchronize event to immediately apply the previous  */
         /* button event.                                               */
         gettimeofday(&event.time, NULL);
         event.type  = EV_SYN;
         event.code  = SYN_REPORT;
         event.value = 0;

         if(write(Keyboard?UINPUTDeviceHandle_Keyboard:UINPUTDeviceHandle_AVRCP, &event, sizeof(event)) == sizeof(event))
            ret_val = 0;
         else
            ret_val = UINPUT_ERROR_UNABLE_TO_WRITE_TO_UINPUT_DEVICE_FILE;
      }
      else
         ret_val = UINPUT_ERROR_UNABLE_TO_WRITE_TO_UINPUT_DEVICE_FILE;
   }
   else
   {
      DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("(%s) Error: uinput device not initialized\n", __FUNCTION__));

      ret_val = UINPUT_ERROR_NOT_INITIALIZED;
   }

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is provided to allow a mechanism for the   */
   /* caller to insert a Mouse Event into the Input sub-system.  The    */
   /* final parameter specifies whether the input should be synchronized*/
   /* or not.  A return value of 0 indicates success while a negative   */
   /* value is returned on error.                                       */
static int SendUINPUTMouseEvent(Word_t Type, Word_t Code, SDWord_t Value, Boolean_t SyncInput)
{
   int                ret_val;
   struct input_event event;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter (%d, %d, %d, %s)\n", Type, Code, Value, (SyncInput == FALSE ? "FALSE" : "TRUE")));

   if(UINPUTDeviceHandle_Mouse >= 0)
   {
      memset(&event, 0, sizeof(event));

      /* Send the actual button event.                                  */
      gettimeofday(&event.time, NULL);
      event.type  = Type;
      event.code  = Code;
      event.value = Value;

      if(write(UINPUTDeviceHandle_Mouse, &event, sizeof(event)) == sizeof(event))
      {
         if(SyncInput)
         {
            /* Send a synchronize event to immediately apply the        */
            /* previous button event.                                   */
            gettimeofday(&event.time, NULL);
            event.type  = EV_SYN;
            event.code  = SYN_REPORT;
            event.value = 0;

            if(write(UINPUTDeviceHandle_Mouse, &event, sizeof(event)) == sizeof(event))
               ret_val = 0;
            else
               ret_val = UINPUT_ERROR_UNABLE_TO_WRITE_TO_UINPUT_DEVICE_FILE;
            usleep(0);
         }
         else
            ret_val = 0;
      }
      else
         ret_val = UINPUT_ERROR_UNABLE_TO_WRITE_TO_UINPUT_DEVICE_FILE;
   }
   else
   {
      DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_VERBOSE), ("(%s) Error: uinput device not initialized\n", __FUNCTION__));

      ret_val = UINPUT_ERROR_NOT_INITIALIZED;
   }

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to submit a remote control         */
   /* pass-through event to the uinput subsystem for presentation on a  */
   /* virtual input device.  The first parameter contains the Remote    */
   /* Control Operation ID.  The second parameter defines the state of  */
   /* the remote device's button: FALSE for pressed, TRUE for released. */
   /* The function return zero on success, or a negative error code.    */
int UINPUT_SendRemoteControlPassthroughEvent(Byte_t AVRCPOperationId, Boolean_t ButtonState)
{
   int                     ret_val;
   BTPSCONST KeyBinding_t *Key;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter (%d, %s)\n", AVRCPOperationId, (ButtonState == FALSE ? "FALSE" : "TRUE")));

   /* First, make sure that there is a remote control device that has   */
   /* been initialized.                                                 */
   if(UINPUTDeviceHandle_AVRCP >= 0)
   {
      /* Set a default return error if we don't find the AVRCP Operation*/
      /* Id in our list of supported key events.                        */
      ret_val = UINPUT_ERROR_UNSUPPORTED_AVRCP_PASS_THROUGH_OPERATION;

      Key = &(SupportedKeys[0]);
      while((Key->AVRCPOperationId != 0) || (Key->UINPUTKeycode != 0))
      {
         if(Key->AVRCPOperationId == AVRCPOperationId)
         {
            ret_val = SendUINPUTKeyEvent(FALSE, Key->UINPUTKeycode, (ButtonState == FALSE ? TRUE : FALSE));
            break;
         }
         else
            Key++;
      }
   }
   else
      ret_val = UINPUT_ERROR_NOT_INITIALIZED;

   DebugPrint((BTPM_DEBUG_ZONE_AUDIO | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to submit a Keyboard (Boot Mode    */
   /* only) Key event to the uinput subsystem for presentation on a     */
   /* virtual input device.  The first parameter contains the Key Code  */
   /* (Boot Mode), followed by the key state.  The function return zero */
   /* on success, or a negative error code.                             */
int UINPUT_SendKeyboardBootEvent(Byte_t Key, Boolean_t KeyDown)
{
   int    ret_val;
   Word_t KeyCode = 0;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter (%d, %s)\n", Key, (KeyDown == FALSE ? "FALSE" : "TRUE")));

#if SS1_SUPPORT_HID

   /* First, make sure that there is a keyboard that has been           */
   /* initialized.                                                      */
   if(UINPUTDeviceHandle_Keyboard >= 0)
   {
      /* Map the incoming HID key code to the OS keys.                  */
      if((Key >= HID_HOST_KEYBOARD_A) && (Key <= HID_HOST_KEYBOARD_APPLICATION))
         KeyCode = HIDKeyLookupTable[Key-4];
      else
      {
         if((Key >= HID_HOST_KEYBOARD_LEFT_CONTROL) && (Key <= HID_HOST_KEYBOARD_RIGHT_GUI))
            KeyCode = HIDKeyLookupTable[Key-HID_HOST_KEYBOARD_LEFT_CONTROL+HID_HOST_KEYBOARD_APPLICATION-3];
      }

      /* Check to see if this is a key we know how to handle.           */
      if(KeyCode)
         ret_val = SendUINPUTKeyEvent(TRUE, KeyCode, KeyDown);
      else
         ret_val = UINPUT_ERROR_UNABLE_TO_PERFORM_KEY_BINDING;
   }
   else
      ret_val = UINPUT_ERROR_NOT_INITIALIZED;

#else

   ret_val = UINPUT_ERROR_NOT_INITIALIZED;

#endif

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to submit a Mouse (Boot Mode only) */
   /* event to the uinput subsystem for presentation on a virtual input */
   /* device.  The first parameter contains the Mouse button state (Boot*/
   /* Mode), followed by the relative X/Y positions.  The final         */
   /* parameter, if specified, represents the Relative position of the  */
   /* scroll wheel.  The function return zero on success, or a negative */
   /* error code.                                                       */
int UINPUT_SendMouseBootEvent(Byte_t ButtonState, SByte_t RelativeX, SByte_t RelativeY, SByte_t *RelativeScrollWheel)
{
   int ret_val;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter (0x%02X, %d, %d)\n", ButtonState, RelativeX, RelativeY));

#if SS1_SUPPORT_HID

   /* First, make sure that there is a mouse that has been initialized. */
   if(UINPUTDeviceHandle_Mouse >= 0)
   {
      ret_val = 0;

      /* Map the incoming Mouse codes to the OS events.                 */
      if((ButtonState & HID_HOST_LEFT_BUTTON_DOWN) || (ButtonState & HID_HOST_LEFT_BUTTON_UP))
         ret_val = SendUINPUTMouseEvent(EV_KEY, BTN_LEFT, (ButtonState & HID_HOST_LEFT_BUTTON_DOWN)?1:0, (((ButtonState & (HID_HOST_MIDDLE_BUTTON_DOWN | HID_HOST_MIDDLE_BUTTON_UP)) || (ButtonState & (HID_HOST_RIGHT_BUTTON_DOWN | HID_HOST_RIGHT_BUTTON_UP)) || (RelativeX) || (RelativeY) || ((RelativeScrollWheel) && (*RelativeScrollWheel))) ? FALSE : TRUE));

      if((ButtonState & HID_HOST_MIDDLE_BUTTON_DOWN) || (ButtonState & HID_HOST_MIDDLE_BUTTON_UP))
         ret_val = SendUINPUTMouseEvent(EV_KEY, BTN_MIDDLE, (ButtonState & HID_HOST_MIDDLE_BUTTON_DOWN)?1:0, (((ButtonState & (HID_HOST_RIGHT_BUTTON_DOWN | HID_HOST_RIGHT_BUTTON_UP)) || (RelativeX) || (RelativeY) || ((RelativeScrollWheel) && (*RelativeScrollWheel))) ? FALSE : TRUE));

      if((ButtonState & HID_HOST_RIGHT_BUTTON_DOWN) || (ButtonState & HID_HOST_RIGHT_BUTTON_UP))
         ret_val = SendUINPUTMouseEvent(EV_KEY, BTN_RIGHT, (ButtonState & HID_HOST_RIGHT_BUTTON_DOWN)?1:0, (((RelativeX) || (RelativeY) || ((RelativeScrollWheel) && (*RelativeScrollWheel))) ? FALSE : TRUE));

      //XXX These buttons are not currently supported by Bluetopia
      //SendUINPUTMouseEvent(EV_KEY, BTN_SIDE, 0, FALSE);
      //SendUINPUTMouseEvent(EV_KEY, BTN_EXTRA, 0, FALSE);

      if(RelativeX)
         ret_val = SendUINPUTMouseEvent(EV_REL, REL_X, (SByte_t)RelativeX, (((RelativeY) || ((RelativeScrollWheel) && (*RelativeScrollWheel))) ? FALSE : TRUE));

      if(RelativeY)
         ret_val = SendUINPUTMouseEvent(EV_REL, REL_Y, (SByte_t)RelativeY, (((RelativeScrollWheel) && (*RelativeScrollWheel)) ? FALSE : TRUE));

      if((RelativeScrollWheel) && (*RelativeScrollWheel))
         ret_val = SendUINPUTMouseEvent(EV_REL, REL_WHEEL, (SByte_t)(*RelativeScrollWheel), TRUE);
   }
   else
      ret_val = UINPUT_ERROR_NOT_INITIALIZED;

#else

   ret_val = UINPUT_ERROR_NOT_INITIALIZED;

#endif

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to register a virtual input device */
   /* with the uinput subsystem.  The device will be registered under   */
   /* the name defined as VIRTUAL_DEVICE_NAME as a keyboard-only device */
   /* with the keys listed in the SupportedKeys array.  On success, this*/
   /* function returns the file descriptor to the uinput device file (a */
   /* positive, non-zero value).  Otherwise, a negative error code is   */
   /* returned.                                                         */
int UINPUT_Initialize(void)
{
   int ret_val;
   int UINPUTDeviceFD;

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* First, make sure the module has not already been initialized.     */
   if((!Mutex) && (UINPUTDeviceHandle_AVRCP < 0))
   {
      /* Attempt to open UINPUT.                                        */
      if((UINPUTDeviceFD = OpenUINPUTDevice()) >= 0)
      {
         /* UINPUT opened, attempt to create the actual input device (in*/
         /* this case Remote Control only).                             */
         if((ret_val = CreateInputDevice(UINPUTDeviceFD, dtRemoteControl)) == 0)
         {
            /* Success.  Flag that we are succesful and note the Remote */
            /* Control File Descriptor.                                 */
            UINPUTDeviceHandle_AVRCP = UINPUTDeviceFD;

            /* Allocate a Mutex to guard access to the Keyboard/Mice    */
            /* counts.                                                  */
            Mutex                    = BTPS_CreateMutex(FALSE);

            /* Finally initialize the Keyboard/Mouse Counts.            */
            KeyboardCount            = 0;
            MouseCount               = 0;

            /* Finally flag success to the caller.                      */
            ret_val                  = 0;
         }
         else
         {
            /* Something went wrong with the device initialization.     */
            /* Close the device and clean up.                           */
            close(UINPUTDeviceFD);
         }
      }
      else
         ret_val = UINPUT_ERROR_UNABLE_TO_OPEN_UINPUT_DEVICE_FILE;
   }
   else
      ret_val = UINPUT_ERROR_ALREADY_INITIALIZED;

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to register a virtual input device */
   /* with the uinput sub-system (for keyboard support only).  The      */
   /* device will be registered under the name defined as               */
   /* VIRTUAL_DEVICE_NAME_KEYBOARD as a keyboard-only device with the   */
   /* keys listed in the HIDKeyLookupTable array.  On success, this     */
   /* function returns zero, otherwise a negative error code is         */
   /* returned.                                                         */
   /* * NOTE * Only a single keyboard can be added to the system at a   */
   /*          time.  This is reference counted, such that if a keyboard*/
   /*          device already exists, another one will not be created.  */
int UINPUT_EnableKeyboard(void)
{
   int ret_val;
   int UINPUTDeviceFD;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* First, make sure the module has been initialized and that the     */
   /* Keyboard Mutex can be acquired.                                   */
   if((Mutex) && (BTPS_WaitMutex(Mutex, BTPS_INFINITE_WAIT)))
   {
      /* Check to see if need to create a virtual Keyboard device.      */
      if(!KeyboardCount)
      {
         /* Attempt to open UINPUT.                                     */
         if((UINPUTDeviceFD = OpenUINPUTDevice()) >= 0)
         {
            /* UINPUT opened, attempt to create the actual input device */
            /* (in this case Remote Control only).                      */
            if((ret_val = CreateInputDevice(UINPUTDeviceFD, dtKeyboard)) == 0)
            {
               /* Success.  Flag that we are succesful and note the     */
               /* Keyboard File Descriptor.                             */
               UINPUTDeviceHandle_Keyboard = UINPUTDeviceFD;

               KeyboardCount               = 1;

               /* Flag success to the caller.                           */
               ret_val                     = 0;
            }
            else
               ret_val = UINPUT_ERROR_UNABLE_TO_CREATE_VIRTUAL_DEVICE;
         }
         else
            ret_val = UINPUT_ERROR_UNABLE_TO_OPEN_UINPUT_DEVICE_FILE;
      }
      else
      {
         KeyboardCount++;

         ret_val = 0;
      }

      /* Free the Mutex as we no longer need access to the Keyboard     */
      /* Device and/or count.                                           */
      BTPS_ReleaseMutex(Mutex);
   }
   else
      ret_val = UINPUT_ERROR_NOT_INITIALIZED;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to un-register a virtual input     */
   /* keyboard device (that was registered via a successful call to the */
   /* UINPUT_EnableKeyboard() function.  On success, this function      */
   /* returns zero, otherwise a negative error code is returned.        */
   /* * NOTE * This function, like the Enable function, reference counts*/
   /*          the keyboard device.  The only parameter to this function*/
   /*          is provided to override this and force the device to be  */
   /*          removed from the system (if specified as TRUE).          */
int UINPUT_DisableKeyboard(Boolean_t Force)
{
   int ret_val;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* First, make sure the module has been initialized and that the     */
   /* Keyboard Mutex can be acquired.                                   */
   if((Mutex) && (BTPS_WaitMutex(Mutex, BTPS_INFINITE_WAIT)))
   {
      /* Check to see if need to create a virtual Keyboard device.      */
      if(KeyboardCount)
         KeyboardCount--;

      if(((!KeyboardCount) || (Force)) && (UINPUTDeviceHandle_Keyboard >= 0))
      {
         /* Destroy the input device                                    */
         ioctl(UINPUTDeviceHandle_Keyboard, UI_DEV_DESTROY);

         /* Close the UINPUT device                                     */
         close(UINPUTDeviceHandle_Keyboard);

         /* Keyboard device closed, flag that there are no active       */
         /* keyboards.                                                  */
         UINPUTDeviceHandle_Keyboard = (-1);
         KeyboardCount               = 0;
      }

      /* Flag success to the caller.                                    */
      ret_val = 0;

      /* Free the Mutex as we no longer need access to the Keyboard     */
      /* Device and/or count.                                           */
      BTPS_ReleaseMutex(Mutex);
   }
   else
      ret_val = UINPUT_ERROR_NOT_INITIALIZED;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to register a virtual input device */
   /* with the uinput sub-system (for mouse support only).  The device  */
   /* will be registered under the name defined as                      */
   /* VIRTUAL_DEVICE_NAME_MOUSE as a mouse-only device On success, this */
   /* function returns zero, otherwise a negative error code is         */
   /* returned.                                                         */
   /* * NOTE * Only a single mouse can be added to the system at a time.*/
   /*          This is reference counted, such that if a mouse device   */
   /*          already exists, another one will not be created.         */
int UINPUT_EnableMouse(void)
{
   int ret_val;
   int UINPUTDeviceFD;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* First, make sure the module has been initialized and that the     */
   /* Keyboard/Mouse Mutex can be acquired.                             */
   if((Mutex) && (BTPS_WaitMutex(Mutex, BTPS_INFINITE_WAIT)))
   {
      /* Check to see if need to create a virtual Mouse device.         */
      if(!MouseCount)
      {
         /* Attempt to open UINPUT.                                     */
         if((UINPUTDeviceFD = OpenUINPUTDevice()) >= 0)
         {
            /* UINPUT opened, attempt to create the actual input device */
            /* (in this case Mouse only).                               */
            if((ret_val = CreateInputDevice(UINPUTDeviceFD, dtMouse)) == 0)
            {
               /* Success.  Flag that we are succesful and note the     */
               /* Mouse File Descriptor.                                */
               UINPUTDeviceHandle_Mouse = UINPUTDeviceFD;

               MouseCount               = 1;

               /* Flag success to the caller.                           */
               ret_val                  = 0;
            }
            else
               ret_val = UINPUT_ERROR_UNABLE_TO_CREATE_VIRTUAL_DEVICE;
         }
         else
            ret_val = UINPUT_ERROR_UNABLE_TO_OPEN_UINPUT_DEVICE_FILE;
      }
      else
      {
         MouseCount++;

         /* Flag success to the caller.                                 */
         ret_val = 0;
      }

      /* Free the Mutex as we no longer need access to the Mouse Device */
      /* and/or count.                                                  */
      BTPS_ReleaseMutex(Mutex);
   }
   else
      ret_val = UINPUT_ERROR_NOT_INITIALIZED;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to un-register a virtual input     */
   /* mouse device (that was registered via a successful call to the    */
   /* UINPUT_EnableMouse() function.  On success, this function returns */
   /* zero, otherwise a negative error code is returned.                */
   /* * NOTE * This function, like the Enable function, reference counts*/
   /*          the mouse device.  The only parameter to this function is*/
   /*          provided to override this and force the device to be     */
   /*          removed from the system (if specified as TRUE).          */
int UINPUT_DisableMouse(Boolean_t Force)
{
   int ret_val;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   /* First, make sure the module has been initialized and that the     */
   /* Keyboard/Mouse Mutex can be acquired.                             */
   if((Mutex) && (BTPS_WaitMutex(Mutex, BTPS_INFINITE_WAIT)))
   {
      /* Check to see if need to keep an existing virtual Mouse device  */
      /* for any additional connected HID mice.                         */
      if(MouseCount)
         MouseCount--;

      if(((!MouseCount) || (Force)) && (UINPUTDeviceHandle_Mouse >= 0))
      {
         /* Destroy the input device                                 */
         ioctl(UINPUTDeviceHandle_Mouse, UI_DEV_DESTROY);

         /* Close the UINPUT device                                  */
         close(UINPUTDeviceHandle_Mouse);

         /* Mouse device closed, flag that there are no active mice. */
         UINPUTDeviceHandle_Mouse = (-1);
         MouseCount               = 0;
      }

      /* Flag success to the caller.                                 */
      ret_val = 0;

      /* Free the Mutex as we no longer need access to the Mouse Device */
      /* and/or count.                                                  */
      BTPS_ReleaseMutex(Mutex);
   }
   else
      ret_val = UINPUT_ERROR_NOT_INITIALIZED;

   DebugPrint((BTPM_DEBUG_ZONE_HID | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit: %d\n", ret_val));

   return(ret_val);
}

   /* The following function is used to remove the virtual input device */
   /* registration from the input system and close all connections to   */
   /* the uinput subsystem.                                             */
void UINPUT_Cleanup(void)
{
   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Enter\n"));

   if(UINPUTDeviceHandle_AVRCP >= 0)
   {
      /* Destroy the input device                                       */
      ioctl(UINPUTDeviceHandle_AVRCP, UI_DEV_DESTROY);

      /* Close the UINPUT device                                        */
      close(UINPUTDeviceHandle_AVRCP);

      UINPUTDeviceHandle_AVRCP = (-1);
   }

   /* Close any existing keyboard device.                               */
   UINPUT_DisableKeyboard(TRUE);

   /* Close any existing mouse device.                                  */
   UINPUT_DisableMouse(TRUE);

   /* Free any mutex that might have been created.                   */
   if(Mutex)
   {
      BTPS_CloseMutex(Mutex);
      Mutex = NULL;
   }

   DebugPrint((BTPM_DEBUG_ZONE_INIT | BTPM_DEBUG_LEVEL_FUNCTION), ("Exit\n"));
}

