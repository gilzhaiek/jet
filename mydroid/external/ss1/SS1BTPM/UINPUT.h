/*****< uinput.h >*************************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  UINPUT - Module for Stonestreet One Bluetopia Platform Manager to         */
/*           translate AVRCP/HID events into Linux UInput events.             */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   10/27/10  G. Hensley     Initial creation.                               */
/******************************************************************************/
#ifndef __UINPUTH__
#define __UINPUTH__

   /* The following constant defines the name of the virtual input      */
   /* device as it will be known to the Linux kernel.                   */
#define VIRTUAL_DEVICE_NAME                                    "Bluetooth Remote Control"

   /* The following constant defines the name of the virtual input      */
   /* device as it will be known to the Linux kernel.                   */
#define VIRTUAL_DEVICE_NAME_KEYBOARD                           "Bluetooth Keyboard"

   /* The following constant defines the name of the virtual input      */
   /* device as it will be known to the Linux kernel.                   */
#define VIRTUAL_DEVICE_NAME_MOUSE                              "Bluetooth Mouse"

   /* The following constants define error codes used within the UINPUT */
   /* module.                                                           */
#define UINPUT_ERROR_NOT_INITIALIZED                           (-20001)
#define UINPUT_ERROR_ALREADY_INITIALIZED                       (-20002)
#define UINPUT_ERROR_UNABLE_TO_OPEN_UINPUT_DEVICE_FILE         (-20003)
#define UINPUT_ERROR_UNABLE_TO_CREATE_VIRTUAL_DEVICE           (-20004)
#define UINPUT_ERROR_INVALID_UINPUT_DEVICE                     (-20005)
#define UINPUT_ERROR_UNSUPPORTED_AVRCP_PASS_THROUGH_OPERATION  (-20006)
#define UINPUT_ERROR_UNABLE_TO_WRITE_TO_UINPUT_DEVICE_FILE     (-20007)
#define UINPUT_ERROR_UNABLE_TO_PERFORM_KEY_BINDING             (-20008)

   /* The following function is used to submit a remote control         */
   /* pass-through event to the uinput subsystem for presentation on a  */
   /* virtual input device.  The first parameter contains the Remote    */
   /* Control Operation ID.  The second parameter defines the state of  */
   /* the remote device's button: FALSE for pressed, TRUE for released. */
   /* The function return zero on success, or a negative error code.    */
int UINPUT_SendRemoteControlPassthroughEvent(Byte_t AVRCPOperationId, Boolean_t ButtonState);

   /* The following function is used to submit a Keyboard (Boot Mode    */
   /* only) Key event to the uinput subsystem for presentation on a     */
   /* virtual input device.  The first parameter contains the Key Code  */
   /* (Boot Mode), followed by the key state.  The function return zero */
   /* on success, or a negative error code.                             */
int UINPUT_SendKeyboardBootEvent(Byte_t Key, Boolean_t KeyDown);

   /* The following function is used to submit a Mouse (Boot Mode only) */
   /* event to the uinput subsystem for presentation on a virtual input */
   /* device.  The first parameter contains the Mouse button state (Boot*/
   /* Mode), followed by the relative X/Y positions.  The final         */
   /* parameter, if specified, represents the Relative position of the  */
   /* scroll wheel.  The function return zero on success, or a negative */
   /* error code.                                                       */
int UINPUT_SendMouseBootEvent(Byte_t ButtonState, SByte_t RelativeX, SByte_t RelativeY, SByte_t *RelativeScrollWheel);

   /* The following function is used to register a virtual input device */
   /* with the uinput subsystem.  The device will be registered under   */
   /* the name defined as VIRTUAL_DEVICE_NAME as a keyboard-only device */
   /* with the keys listed in the SupportedKeys array.  On success, this*/
   /* function returns the file descriptor to the uinput device file (a */
   /* positive, non-zero value).  Otherwise, a negative error code is   */
   /* returned.                                                         */
int UINPUT_Initialize(void);

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
int UINPUT_EnableKeyboard(void);

   /* The following function is used to un-register a virtual input     */
   /* keyboard device (that was registered via a successful call to the */
   /* UINPUT_EnableKeyboard() function.  On success, this function      */
   /* returns zero, otherwise a negative error code is returned.        */
   /* * NOTE * This function, like the Enable function, reference counts*/
   /*          the keyboard device.  The only parameter to this function*/
   /*          is provided to override this and force the device to be  */
   /*          removed from the system (if specified as TRUE).          */
int UINPUT_DisableKeyboard(Boolean_t Force);

   /* The following function is used to register a virtual input device */
   /* with the uinput sub-system (for mouse support only).  The device  */
   /* will be registered under the name defined as                      */
   /* VIRTUAL_DEVICE_NAME_MOUSE as a mouse-only device On success, this */
   /* function returns zero, otherwise a negative error code is         */
   /* returned.                                                         */
   /* * NOTE * Only a single mouse can be added to the system at a time.*/
   /*          This is reference counted, such that if a mouse device   */
   /*          already exists, another one will not be created.         */
int UINPUT_EnableMouse(void);

   /* The following function is used to un-register a virtual input     */
   /* mouse device (that was registered via a successful call to the    */
   /* UINPUT_EnableMouse() function.  On success, this function returns */
   /* zero, otherwise a negative error code is returned.                */
   /* * NOTE * This function, like the Enable function, reference counts*/
   /*          the mouse device.  The only parameter to this function is*/
   /*          provided to override this and force the device to be     */
   /*          removed from the system (if specified as TRUE).          */
int UINPUT_DisableMouse(Boolean_t Force);

   /* The following function is used to remove the virtual input device */
   /* registration from the input system and close all connections to   */
   /* the uinput subsystem.                                             */
void UINPUT_Cleanup(void);

#endif

