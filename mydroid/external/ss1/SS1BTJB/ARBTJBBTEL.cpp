/*****< ARBTJBBTEL.cpp >*******************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTEL - android.server.BluetoothEventLoop module for Stonestreet One */
/*               Android Runtime Bluetooth JNI Bridge                         */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   06/30/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "ARBTJBBTEL"

#include "android_runtime/AndroidRuntime.h"
#include "cutils/sockets.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>

#include "SS1UTIL.h"

#ifdef HAVE_BLUETOOTH

extern "C"
{
#include "SS1BTPM.h"

#if ((SS1_PLATFORM_SDK_VERSION >= 14) && (SS1_SUPPORT_HID))

#include "HIDMAPI.h"

#endif
}

#include "ARBTJBBTEL.h"
#include "ARBTJBBTAS.h"
#include "ARBTJBBTS.h"
#include "ARBTJBCOM.h"

#endif /* HAVE_BLUETOOTH */

namespace android
{

#ifdef HAVE_BLUETOOTH

   /* The following constant defines the time in milliseconds to wait   */
   /* for a reply to a PIN Code, Passkey, or Confirmation response.     */
   /* After this timeout expires, it is assumed the pairing attempt     */
   /* failed even if the remote device stays does not drop the          */
   /* connection.                                                       */
#define PAIR_REPLY_TIMEOUT_MS 30000

static jfieldID Field_mNativeData;
static jfieldID Field_mBluetoothService;

static jmethodID Method_onPropertyChanged;
static jmethodID Method_onDevicePropertyChanged;
static jmethodID Method_onDeviceFound;
static jmethodID Method_onDeviceDisappeared;
static jmethodID Method_onDeviceCreated;
static jmethodID Method_onDeviceRemoved;
static jmethodID Method_onDeviceDisconnectRequested;
static jmethodID Method_onNetworkDeviceConnected;
static jmethodID Method_onNetworkDeviceDisconnected;

static jmethodID Method_onCreatePairedDeviceResult;
static jmethodID Method_onCreateDeviceResult;
static jmethodID Method_onDiscoverServicesResult;

static jmethodID Method_onRequestPinCode;
static jmethodID Method_onRequestPasskey;
static jmethodID Method_onRequestPasskeyConfirmation;
static jmethodID Method_onRequestPairingConsent;
static jmethodID Method_onDisplayPasskey;
static jmethodID Method_onAgentAuthorize;
static jmethodID Method_onAgentCancel;

static jmethodID Method_onInputDevicePropertyChanged;
static jmethodID Method_onInputDeviceConnectionResult;
static jmethodID Method_onPanDevicePropertyChanged;
static jmethodID Method_onPanDeviceConnectionResult;
static jmethodID Method_onHealthDevicePropertyChanged;
static jmethodID Method_onHealthDeviceChannelChanged;
static jmethodID Method_onHealthDeviceConnectionResult;

static jclass Class_String;


static inline BTEL_NativeData_t *GetNativeData(JNIEnv *Env, jobject Object)
{
   return((BTEL_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData)));
}

#endif


static void BTEL_ClassInitNative(JNIEnv *Env, jclass Clazz)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   Method_onPropertyChanged              = Env->GetMethodID(Clazz, "onPropertyChanged",              "([Ljava/lang/String;)V");
   Method_onDevicePropertyChanged        = Env->GetMethodID(Clazz, "onDevicePropertyChanged",        "(Ljava/lang/String;[Ljava/lang/String;)V");
   Method_onDeviceFound                  = Env->GetMethodID(Clazz, "onDeviceFound",                  "(Ljava/lang/String;[Ljava/lang/String;)V");
   Method_onDeviceDisappeared            = Env->GetMethodID(Clazz, "onDeviceDisappeared",            "(Ljava/lang/String;)V");
   Method_onDeviceCreated                = Env->GetMethodID(Clazz, "onDeviceCreated",                "(Ljava/lang/String;)V");
   Method_onDeviceRemoved                = Env->GetMethodID(Clazz, "onDeviceRemoved",                "(Ljava/lang/String;)V");
   Method_onDeviceDisconnectRequested    = Env->GetMethodID(Clazz, "onDeviceDisconnectRequested",    "(Ljava/lang/String;)V");
#if (SS1_PLATFORM_SDK_VERSION >= 14)
   Method_onNetworkDeviceConnected       = Env->GetMethodID(Clazz, "onNetworkDeviceConnected",       "(Ljava/lang/String;Ljava/lang/String;I)V");
   Method_onNetworkDeviceDisconnected    = Env->GetMethodID(Clazz, "onNetworkDeviceDisconnected",    "(Ljava/lang/String;)V");
#endif

   Method_onCreatePairedDeviceResult     = Env->GetMethodID(Clazz, "onCreatePairedDeviceResult",     "(Ljava/lang/String;I)V");
   Method_onCreateDeviceResult           = Env->GetMethodID(Clazz, "onCreateDeviceResult",           "(Ljava/lang/String;I)V");
   Method_onDiscoverServicesResult       = Env->GetMethodID(Clazz, "onDiscoverServicesResult",       "(Ljava/lang/String;Z)V");

   Method_onRequestPinCode               = Env->GetMethodID(Clazz, "onRequestPinCode",               "(Ljava/lang/String;I)V");
   Method_onRequestPasskey               = Env->GetMethodID(Clazz, "onRequestPasskey",               "(Ljava/lang/String;I)V");
   Method_onRequestPasskeyConfirmation   = Env->GetMethodID(Clazz, "onRequestPasskeyConfirmation",   "(Ljava/lang/String;II)V");
   Method_onRequestPairingConsent        = Env->GetMethodID(Clazz, "onRequestPairingConsent",        "(Ljava/lang/String;I)V");
   Method_onDisplayPasskey               = Env->GetMethodID(Clazz, "onDisplayPasskey",               "(Ljava/lang/String;II)V");

#if ((SS1_PLATFORM_SDK_VERSION > 10) || ((SS1_PLATFORM_SDK_VERSION == 10) && (SS1_PLATFORM_VERSION_REV >= 7)))
   Method_onAgentAuthorize               = Env->GetMethodID(Clazz, "onAgentAuthorize",               "(Ljava/lang/String;Ljava/lang/String;I)V");
#else
   Method_onAgentAuthorize               = Env->GetMethodID(Clazz, "onAgentAuthorize",               "(Ljava/lang/String;Ljava/lang/String;)Z");
#endif

   Method_onAgentCancel                  = Env->GetMethodID(Clazz, "onAgentCancel",                  "()V");

#if (SS1_PLATFORM_SDK_VERSION >= 14)
   Method_onInputDevicePropertyChanged   = Env->GetMethodID(Clazz, "onInputDevicePropertyChanged",   "(Ljava/lang/String;[Ljava/lang/String;)V");
   Method_onInputDeviceConnectionResult  = Env->GetMethodID(Clazz, "onInputDeviceConnectionResult",  "(Ljava/lang/String;I)V");
   Method_onPanDevicePropertyChanged     = Env->GetMethodID(Clazz, "onPanDevicePropertyChanged",     "(Ljava/lang/String;[Ljava/lang/String;)V");
   Method_onPanDeviceConnectionResult    = Env->GetMethodID(Clazz, "onPanDeviceConnectionResult",    "(Ljava/lang/String;I)V");
   Method_onHealthDevicePropertyChanged  = Env->GetMethodID(Clazz, "onHealthDevicePropertyChanged",  "(Ljava/lang/String;[Ljava/lang/String;)V");
   Method_onHealthDeviceChannelChanged   = Env->GetMethodID(Clazz, "onHealthDeviceChannelChanged",   "(Ljava/lang/String;Ljava/lang/String;Z)V");
   Method_onHealthDeviceConnectionResult = Env->GetMethodID(Clazz, "onHealthDeviceConnectionResult", "(II)V");
#endif

   Field_mNativeData       = Env->GetFieldID(Clazz, "mNativeData",       "I");
   Field_mBluetoothService = Env->GetFieldID(Clazz, "mBluetoothService", "Landroid/server/BluetoothService;");

   Class_String = (jclass)(Env->NewGlobalRef(Env->FindClass("java/lang/String")));
#endif
   SS1_LOGD("Exit");
}

static void BTEL_InitializeNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BTEL_NativeData_t  *NatData;

   if((NatData = (BTEL_NativeData_t *)calloc(1, sizeof(BTEL_NativeData_t))) != NULL)
   {
      SS1_LOGI("BTEL NatData created @ %p", NatData);
      Env->SetIntField(Object, Field_mNativeData, (jint)NatData);

      Env->GetJavaVM(&(NatData->VM));
      NatData->Object                = Env->NewGlobalRef(Object);
      NatData->JNIEnvVersion         = Env->GetVersion();
      NatData->EventLoopThreadStatus = false;

      pthread_mutex_init(&(NatData->PendingCreateDeviceMutex), NULL);
      pthread_cond_init(&(NatData->PendingCreateDeviceRemovalCondition), NULL);
      memset(NatData->PendingCreateDeviceList, 0, sizeof(NatData->PendingCreateDeviceList));
      NatData->PendingCreateDeviceNumber = 0;

      pthread_mutex_init(&(NatData->DevicePoweredOffMutex), NULL);
      pthread_cond_init(&(NatData->DevicePoweredOffCondition), NULL);

      InitDeviceList(&(NatData->KnownDevices), 16);
      InitDeviceList(&(NatData->NamePendingForAuthDevices), 1);
#if BTEL_DELAY_FOUND_FOR_NAME
      InitDeviceList(&(NatData->NamePendingForFoundDevices), 16);
#endif /* BTEL_DELAY_FOUND_FOR_NAME */
      InitDeviceList(&(NatData->PairResponsePendingDevices), 1);
      InitDeviceList(&(NatData->ServiceDiscoveryPendingDevices), 8);
   }
#endif
   SS1_LOGD("Exit");
}

static void BTEL_CleanupNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BTEL_NativeData_t *NatData;

   NatData = (BTEL_NativeData_t *)Env->GetIntField(Object, Field_mNativeData);

   if(NatData)
   {
      Env->DeleteGlobalRef(NatData->Object);

      /* Clean up Pending Device Creation tracking resources.           */
      pthread_mutex_lock(&(NatData->PendingCreateDeviceMutex));
      pthread_mutex_unlock(&(NatData->PendingCreateDeviceMutex));
      pthread_mutex_destroy(&(NatData->PendingCreateDeviceMutex));

      while(pthread_cond_destroy(&(NatData->PendingCreateDeviceRemovalCondition)) == EBUSY)
         pthread_cond_signal(&(NatData->PendingCreateDeviceRemovalCondition));

      /* Clean up Device Powered Off Event tracking resources.          */
      pthread_mutex_lock(&(NatData->DevicePoweredOffMutex));
      pthread_mutex_unlock(&(NatData->DevicePoweredOffMutex));
      pthread_mutex_destroy(&(NatData->DevicePoweredOffMutex));

      while(pthread_cond_destroy(&(NatData->DevicePoweredOffCondition)) == EBUSY)
         pthread_cond_signal(&(NatData->DevicePoweredOffCondition));

      DestroyDeviceList(&(NatData->KnownDevices));
      DestroyDeviceList(&(NatData->NamePendingForAuthDevices));
#if BTEL_DELAY_FOUND_FOR_NAME
      DestroyDeviceList(&(NatData->NamePendingForFoundDevices));
#endif /* BTEL_DELAY_FOUND_FOR_NAME */
      DestroyDeviceList(&(NatData->PairResponsePendingDevices));
      DestroyDeviceList(&(NatData->ServiceDiscoveryPendingDevices));

      free(NatData);
   }

   Env->SetIntField(Object, Field_mNativeData, 0);
#endif
   SS1_LOGD("Exit");
}

static jboolean BTEL_StartEventLoopNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jboolean           ret_val;
   BTEL_NativeData_t *NatData;

   if((NatData = (BTEL_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
   {
      NatData->EventLoopThreadStatus = true;
      ret_val                        = JNI_TRUE;
   }
   else
      ret_val = JNI_FALSE;

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static void BTEL_StopEventLoopNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BTEL_NativeData_t *NatData;

   if((NatData = (BTEL_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
      NatData->EventLoopThreadStatus = false;
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

static jboolean BTEL_IsEventLoopRunningNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jboolean           ret_val;
   BTEL_NativeData_t *NatData;

   if((NatData = (BTEL_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
      ret_val = NatData->EventLoopThreadStatus;
   else
      ret_val = JNI_FALSE;

   SS1_LOGD("Exit (%d)", ret_val);

   return(NatData->EventLoopThreadStatus);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}



static JNINativeMethod NativeMethods[] =
{
   /* name, signature, funcPtr */
   {"classInitNative",            "()V", (void *)BTEL_ClassInitNative},
   {"initializeNativeDataNative", "()V", (void *)BTEL_InitializeNativeDataNative},
   {"cleanupNativeDataNative",    "()V", (void *)BTEL_CleanupNativeDataNative},
   {"startEventLoopNative",       "()V", (void *)BTEL_StartEventLoopNative},
   {"stopEventLoopNative",        "()V", (void *)BTEL_StopEventLoopNative},
   {"isEventLoopRunningNative",   "()Z", (void *)BTEL_IsEventLoopRunningNative}
};

int SS1API register_android_server_BluetoothEventLoop(JNIEnv *Env)
{
   return(AndroidRuntime::registerNativeMethods(Env, "android/server/BluetoothEventLoop", NativeMethods, NELEM(NativeMethods)));
}


#ifdef HAVE_BLUETOOTH

   /* The following function is provided to handle sending "Property    */
   /* Changed" notifications up to the Java side of the BTEL service.   */
static void HandlePropertyChanged(JNIEnv *Env, jobject Object, const char *Key, const char *Value)
{
   jobjectArray PropArray;

   if(Env && Object && Key && Value)
   {
      if((PropArray = Env->NewObjectArray(2, Class_String, NULL)) != NULL)
      {
         SetStringArrayElement(Env, PropArray, 0, Key);
         SetStringArrayElement(Env, PropArray, 1, Value);
         Env->CallVoidMethod(Object, Method_onPropertyChanged, PropArray);
         Env->DeleteLocalRef(PropArray);
      }
   }
}

   /* The following function is used to process "Device Created"        */
   /* notifications, which are not sent up to the Java side of the BTEL */
   /* service. The are not sent upstream in order to avoid a misleading */
   /* DEVICE_FOUND broadcast sent by the default implementation of      */
   /* BluetoothEventLoop which results in unintuitive behavior in the   */
   /* Settings app or any other app which listens for DEVICE_FOUND      */
   /* broadcasts all the time (as opposed to only when a Discovery      */
   /* process is running).                                              */
static void HandleDeviceCreated(JNIEnv *Env, BTEL_NativeData_t *NatData, DEVM_Remote_Device_Properties_t *RemoteDevProps)
{
   char    StringBuffer[64];
   jobject BTSObject;
   jobject PropArray;
   jstring AddressString;

   if(Env && NatData)
   {
      if((BTSObject = Env->GetObjectField(NatData->Object, Field_mBluetoothService)) != NULL)
      {
#if (SS1_PLATFORM_SDK_VERSION >= 14)
         /* XXX Hopefully, Android 4.0 has fixed the Settings app       */
         /* behavior where DEVICE_FOUND intents (triggered by the       */
         /* call to onDeviceCreated) confused the Settings app. If      */
         /* the issue is still present, rewrite this to replicate the   */
         /* new-as-of-4.0 behavior of onDeviceCreated.                  */
         BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), RemoteDevProps->BD_ADDR);
         if((AddressString = Env->NewStringUTF(StringBuffer)) != NULL)
         {
            Env->CallVoidMethod(NatData->Object, Method_onDeviceCreated, AddressString);

            Env->DeleteLocalRef(AddressString);
         }
#else
         /* Instead of calling BTEL::onDeviceCreated(), replicate its   */
         /* behavior here without issuing a DEVICE_FOUND broadcast. This*/
         /* is to work around a design flaw in the Settings app which   */
         /* confusingly displays the device if it's the first time the  */
         /* device is seen, but not on any future connection.           */
         BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), RemoteDevProps->BD_ADDR);
         if((AddressString = Env->NewStringUTF(StringBuffer)) != NULL)
         {
            if(BTS_CallIsRemoteDeviceInCache(Env, BTSObject, AddressString) == JNI_FALSE)
            {
               if((PropArray = BuildRemotePropertyStringArray(Env, REMOTE_PROPERTY_ALL, RemoteDevProps)) != NULL)
               {
                  BTS_CallAddRemoteDeviceProperties(Env, BTSObject, AddressString, PropArray);
                  Env->DeleteLocalRef(PropArray);
               }
            }

            Env->DeleteLocalRef(AddressString);
         }
#endif
      }
   }
}

   /* The following function is used to process Authentication Requests */
   /* coming from the Bluetooth stack.                                  */
static void HandleAuthenticationRequest(JNIEnv *Env, DEVM_Authentication_Information_t *AuthReqInfo, BTEL_NativeData_t *NatData)
{
   char                               StringBuffer[256];
   jstring                            Path;
   DEVM_Local_Device_Properties_t     LocalDevProps;
   DEVM_Authentication_Information_t *AuthReqInfoCopy;

   if(AuthReqInfo)
   {
      if(NatData)
      {
         switch(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_ACTION_MASK)
         {
            case DEVM_AUTHENTICATION_ACTION_PIN_CODE_REQUEST:
               SS1_LOGI("Received Auth Request (PIN Code)");
               BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), AuthReqInfo->BD_ADDR);
               if((Path = Env->NewStringUTF(StringBuffer)) != NULL)
               {
                  if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *)malloc(sizeof(DEVM_Authentication_Information_t))) != NULL)
                  {
                     memcpy(AuthReqInfoCopy, AuthReqInfo, sizeof(DEVM_Authentication_Information_t));

                     /* This event begins a classic bonding             */
                     /* process. Start an authentication timer if one   */
                     /* does not already exist.                         */
                     BTEL_AddPendingPairDevice(NatData, AuthReqInfo->BD_ADDR, (!!(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)));

                     /* If a discovery is in progress, stop the         */
                     /* discovery before servicing the pair request.    */
                     if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                        DEVM_StopDeviceDiscovery();

                     SS1_LOGI("Pairing request recorded and Discovery stopped. Notifying Android (PIN Code)");
                     Env->CallVoidMethod(NatData->Object, Method_onRequestPinCode, Path, (jint)AuthReqInfoCopy);
                  }

                  Env->DeleteLocalRef(Path);
               }
               break;

            case DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_REQUEST:
               SS1_LOGI("Received Auth Request (User Confirmation)");
               BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), AuthReqInfo->BD_ADDR);
               if((Path = Env->NewStringUTF(StringBuffer)) != NULL)
               {
                  /* If a discovery is in progress, stop the discovery  */
                  /* before servicing the pair request.                 */
                  if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                     DEVM_StopDeviceDiscovery();

                  SS1_LOGI("Pairing request recorded and Discovery stopped. Notifying Android (User Confirmation)");

                  if((AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) && (AuthReqInfo->AuthenticationDataLength == 0))
                  {
                     if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *)malloc(sizeof(DEVM_Authentication_Information_t))) != NULL)
                     {
                        memcpy(AuthReqInfoCopy, AuthReqInfo, sizeof(DEVM_Authentication_Information_t));

                        /* User Confirmation under LE does not use a    */
                        /* passkey to be compared. Just ask the user if */
                        /* they accept the pairing.                     */
                        SS1_LOGD("Processing LE User Confirmation using User Consent");
                        Env->CallVoidMethod(NatData->Object, Method_onRequestPairingConsent, Path, (jint)AuthReqInfoCopy);
                     }
                  }
                  else
                  {
                     if((!(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)) && (AuthReqInfo->AuthenticationDataLength == sizeof(AuthReqInfo->AuthenticationData.UserConfirmationRequestData)))
                     {
                        /* This request includes I/O Capabilities       */
                        /* information for the remote device, so we must*/
                        /* determine which authentication method to use.*/
                        /* These choices are based on the fact that we  */
                        /* report our I/O Capabilities to be "Display   */
                        /* Yes/No, MITM Required, No Out-Of-Band Data." */
                        switch(AuthReqInfo->AuthenticationData.UserConfirmationRequestData.IOCapabilities.IO_Capability)
                        {
                           case icDisplayOnly:
                              /* The remote device should display the   */
                              /* shared passkey and auto-accept and we  */
                              /* will display the passkey and ask the   */
                              /* user to confirm.                       */

                              /* Fall through because this case is      */
                              /* handled identically to icDisplayYesNo. */
                           case icDisplayYesNo:
                              if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *)malloc(sizeof(DEVM_Authentication_Information_t))) != NULL)
                              {
                                 memcpy(AuthReqInfoCopy, AuthReqInfo, sizeof(DEVM_Authentication_Information_t));

                                 /* Both devices should display the     */
                                 /* shared passkey and ask the user for */
                                 /* confirmation.                       */
                                 SS1_LOGD("Processing User Confirmation using Passkey Confirmation");
                                 Env->CallVoidMethod(NatData->Object, Method_onRequestPasskeyConfirmation, Path, AuthReqInfo->AuthenticationData.UserConfirmationRequestData.Passkey, (jint)AuthReqInfoCopy);
                              }
                              break;
                           case icKeyboardOnly:
                              if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *)malloc(sizeof(DEVM_Authentication_Information_t))) != NULL)
                              {
                                 memcpy(AuthReqInfoCopy, AuthReqInfo, sizeof(DEVM_Authentication_Information_t));

                                 /* We will display the shared passkey  */
                                 /* and the user should enter it on the */
                                 /* remote device.                      */
                                 SS1_LOGD("Processing User Confirmation using Passkey Display");
                                 Env->CallVoidMethod(NatData->Object, Method_onDisplayPasskey, Path, AuthReqInfo->AuthenticationData.UserConfirmationRequestData.Passkey, (jint)AuthReqInfoCopy);
                              }
                              break;
                           case icNoInputNoOutput:
                              if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *)malloc(sizeof(DEVM_Authentication_Information_t))) != NULL)
                              {
                                 memcpy(AuthReqInfoCopy, AuthReqInfo, sizeof(DEVM_Authentication_Information_t));

                                 /* The remote device has no way        */
                                 /* to engage the user, so it will      */
                                 /* auto-accept and we will ask the user*/
                                 /* for permission.                     */
                                 SS1_LOGD("Processing User Confirmation using User Consent");
                                 Env->CallVoidMethod(NatData->Object, Method_onRequestPairingConsent, Path, (jint)AuthReqInfoCopy);
                              }
                              break;
                           default:
                              break;
                        }
                     }
                     else
                     {
                        /* This authentication request is malformed.    */
                        /* Reject it immediately.                       */
                        SS1_LOGD("Rejecting User Confirmation request as malformed");
                        AuthReqInfo->AuthenticationAction            = (DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE | (AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
                        AuthReqInfo->AuthenticationDataLength        = sizeof(AuthReqInfo->AuthenticationData.Confirmation);
                        AuthReqInfo->AuthenticationData.Confirmation = FALSE;

                        DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthReqInfo);
                     }
                  }

                  Env->DeleteLocalRef(Path);
               }
               break;

            case DEVM_AUTHENTICATION_ACTION_PASSKEY_REQUEST:
               SS1_LOGI("Received Auth Request (Passkey)");
               BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), AuthReqInfo->BD_ADDR);
               if((Path = Env->NewStringUTF(StringBuffer)) != NULL)
               {
                  /* If a discovery is in progress, stop the discovery  */
                  /* before servicing the pair request.                 */
                  if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                     DEVM_StopDeviceDiscovery();

                  SS1_LOGI("Pairing request recorded and Discovery stopped. Notifying Android (Passkey)");

                  if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *)malloc(sizeof(DEVM_Authentication_Information_t))) != NULL)
                  {
                     memcpy(AuthReqInfoCopy, AuthReqInfo, sizeof(DEVM_Authentication_Information_t));

                     Env->CallVoidMethod(NatData->Object, Method_onRequestPasskey, Path, (jint)AuthReqInfoCopy);
                  }

                  Env->DeleteLocalRef(Path);
               }
               break;

            case DEVM_AUTHENTICATION_ACTION_PASSKEY_INDICATION:
               SS1_LOGI("Received Auth Request (Passkey Indication)");
               BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), AuthReqInfo->BD_ADDR);
               if((Path = Env->NewStringUTF(StringBuffer)) != NULL)
               {
                  /* If a discovery is in progress, stop the discovery  */
                  /* before servicing the pair request.                 */
                  if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                     DEVM_StopDeviceDiscovery();

                  SS1_LOGI("Pairing request recorded and Discovery stopped. Notifying Android (Passkey Indication)");

                  if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *)malloc(sizeof(DEVM_Authentication_Information_t))) != NULL)
                  {
                     memcpy(AuthReqInfoCopy, AuthReqInfo, sizeof(DEVM_Authentication_Information_t));

                     Env->CallVoidMethod(NatData->Object, Method_onDisplayPasskey, Path, AuthReqInfo->AuthenticationData.Passkey, (jint)AuthReqInfoCopy);
                  }

                  Env->DeleteLocalRef(Path);
               }
               break;

            case DEVM_AUTHENTICATION_ACTION_KEYPRESS_INDICATION:
               SS1_LOGI("Received Auth Request (Keypress)");
               /* There is nothing to do for this event, as far as      */
               /* Android is concerned.                                 */
               break;

            case DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_REQUEST:
               SS1_LOGI("Received Auth Request (OOB Data)");

               /* We don't support out-of-band, yet, so send a rejection*/
               /* response (indicated by length = 0).                   */
               AuthReqInfo->AuthenticationAction = (DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_RESPONSE | (AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
               AuthReqInfo->AuthenticationDataLength = 0;
               memset(&(AuthReqInfo->AuthenticationData), 0, sizeof(AuthReqInfo->AuthenticationData));
               DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthReqInfo);

               break;

            case DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_REQUEST:
               /* This event begins an SSP bonding process. Start an    */
               /* authentication timer if one does not already exist.   */
               BTEL_AddPendingPairDevice(NatData, AuthReqInfo->BD_ADDR, (!!(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)));

               /* For now, send a basic configuration of a Display w/   */
               /* Yes/No input, using no out-of-band data, requring     */
               /* man-in-the-middle protection, and using general       */
               /* bonding.                                              */
               if(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)
               {
                  SS1_LOGD("Received Auth Request (IO Caps LE)");

                  AuthReqInfo->AuthenticationAction                                       = (DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK | DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_RESPONSE);
                  AuthReqInfo->AuthenticationDataLength                                   = sizeof(AuthReqInfo->AuthenticationData.LEIOCapabilities);
                  memset(&(AuthReqInfo->AuthenticationData), 0, sizeof(AuthReqInfo->AuthenticationData));
                  AuthReqInfo->AuthenticationData.LEIOCapabilities.IO_Capability          = licKeyboardDisplay;
                  AuthReqInfo->AuthenticationData.LEIOCapabilities.OOB_Present            = FALSE;
                  AuthReqInfo->AuthenticationData.LEIOCapabilities.Bonding_Type           = lbtBonding;
                  AuthReqInfo->AuthenticationData.LEIOCapabilities.MITM                   = TRUE;
                  DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthReqInfo);
               }
               else
               {
                  SS1_LOGD("Received Auth Request (IO Caps BR/EDR)");

                  AuthReqInfo->AuthenticationAction                                       = DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_RESPONSE;
                  AuthReqInfo->AuthenticationDataLength                                   = sizeof(AuthReqInfo->AuthenticationData.IOCapabilities);
                  memset(&(AuthReqInfo->AuthenticationData), 0, sizeof(AuthReqInfo->AuthenticationData));
                  AuthReqInfo->AuthenticationData.IOCapabilities.IO_Capability            = icDisplayYesNo;
                  AuthReqInfo->AuthenticationData.IOCapabilities.OOB_Data_Present         = FALSE;
                  AuthReqInfo->AuthenticationData.IOCapabilities.MITM_Protection_Required = TRUE;
                  DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthReqInfo);
               }
               break;

            case DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_RESPONSE:
               if(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)
               {
                  SS1_LOGI("Received Auth Response (IO Caps LE): %s, OOB %spresent, MITM %srequired, %sBonding", ((AuthReqInfo->AuthenticationData.LEIOCapabilities.IO_Capability == licDisplayOnly) ? "Display Only" : ((AuthReqInfo->AuthenticationData.LEIOCapabilities.IO_Capability == licDisplayYesNo) ? "Display Yes/No" : ((AuthReqInfo->AuthenticationData.LEIOCapabilities.IO_Capability == licKeyboardOnly) ? "Keyboard Only" : ((AuthReqInfo->AuthenticationData.LEIOCapabilities.IO_Capability == licNoInputNoOutput) ? "No Input/Output" : ((AuthReqInfo->AuthenticationData.LEIOCapabilities.IO_Capability == licKeyboardDisplay) ? "Keyboard & Display" : "Invalid"))))),
                                                                                                                  ((AuthReqInfo->AuthenticationData.LEIOCapabilities.OOB_Present == JNI_FALSE) ? "not " : ""),
                                                                                                                  ((AuthReqInfo->AuthenticationData.LEIOCapabilities.MITM == JNI_FALSE) ? "not " : ""),
                                                                                                                  ((AuthReqInfo->AuthenticationData.LEIOCapabilities.Bonding_Type == lbtNoBonding) ? "No " : ""));
               }
               else
               {
                  SS1_LOGI("Received Auth Response (IO Caps BR/EDR): %s, OOB %spresent, MITM %srequired, %s Bonding", ((AuthReqInfo->AuthenticationData.IOCapabilities.IO_Capability == icDisplayOnly) ? "Display Only" : ((AuthReqInfo->AuthenticationData.IOCapabilities.IO_Capability == icDisplayYesNo) ? "Display Yes/No" : ((AuthReqInfo->AuthenticationData.IOCapabilities.IO_Capability == icKeyboardOnly) ? "Keyboard Only" : ((AuthReqInfo->AuthenticationData.IOCapabilities.IO_Capability == icNoInputNoOutput) ? "No Input/Output" : "Invalid")))),
                                                                                                                      ((AuthReqInfo->AuthenticationData.IOCapabilities.OOB_Data_Present == JNI_FALSE) ? "not " : ""),
                                                                                                                      ((AuthReqInfo->AuthenticationData.IOCapabilities.MITM_Protection_Required == JNI_FALSE) ? "not " : ""),
                                                                                                                      ((AuthReqInfo->AuthenticationData.IOCapabilities.Bonding_Type == ibNoBonding) ? "No" : ((AuthReqInfo->AuthenticationData.IOCapabilities.Bonding_Type == ibDedicatedBonding) ? "Dedicated" : ((AuthReqInfo->AuthenticationData.IOCapabilities.Bonding_Type == ibGeneralBonding) ? "General" : "Invalid"))));
               }
               break;

            default:
               /* We received an auth request we weren't expecting.     */
               /* Ignore the request.                                   */
               SS1_LOGW("Received unexpected auth action (%u)", AuthReqInfo->AuthenticationAction);

               break;
         }
      }
   }
}


static Boolean_t PendingPairTimeoutCallback(unsigned int TimerID, void *CallbackParameter)
{
   int                              AttachResult;
   int                              BondResult;
   int                              QueryPropertiesResult;
   char                             PathString[64];
   char                             AddrString[32];
   JNIEnv                          *Env;
   jstring                          Address;
   BD_ADDR_t                        BD_ADDR;
   Boolean_t                        LowEnergy;
   BTEL_NativeData_t               *NatData;
   DEVM_Remote_Device_Properties_t  RemoteDevProps;

   *(void **)&NatData = CallbackParameter;

   if(NatData)
   {
      if(NatData->VM)
      {
         SS1_LOGI("Timer ID %u fired", TimerID);
         if(RemoveDeviceFromListByExtraData(&(NatData->PairResponsePendingDevices), (void *)TimerID, &BD_ADDR, &LowEnergy))
         {
            /* Attach to the Java VM, if necessary, so we can notify    */
            /* the upper layers of the timeout.                         */
            if((AttachResult = AttachThreadToJVM(NatData->VM, NatData->JNIEnvVersion, &Env)) >= 0)
            {
               /* Report the pairing attempt result.                   */
               BD_ADDRToStr(AddrString, sizeof(AddrString), BD_ADDR);

               BondResult = BOND_RESULT_AUTH_TIMEOUT;

               SS1_LOGI("Found device matching Timer ID %u (%s)", TimerID, AddrString);

               QueryPropertiesResult = DEVM_QueryRemoteDeviceProperties(BD_ADDR, 0, &RemoteDevProps);

#if ANNOUNCE_BOND_AFTER_SDP

               /* If the device is already paired then the announcement */
               /* has been deferred and a timer, reset in anticipation  */
               /* of service discovery, has fired. The announcement     */
               /* needs to be made.                                     */ 
               if((!QueryPropertiesResult) && (RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED))
                  BondResult = BOND_RESULT_SUCCESS;

#endif /* ANNOUNCE_BOND_AFTER_SDP  */ 

               /* Announce status up to Java.                           */
               if((Address = Env->NewStringUTF(AddrString)) != NULL)
               {
                  SS1_LOGD("Reporting pairing %s for device %s", ((BondResult == BOND_RESULT_SUCCESS) ? "success" : ((BondResult == BOND_RESULT_AUTH_TIMEOUT) ? "timeout" : "=unknown status=")), AddrString);
                  Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, Address, BondResult);
                  Env->DeleteLocalRef(Address);
               }

               /* Android expects the device to be unpaired after a     */
               /* timeout. This mimics BlueZ's behavior of deleting a   */
               /* link key upon the beginning of any bonding attempt.   */
               if((!QueryPropertiesResult) && (BondResult == BOND_RESULT_AUTH_TIMEOUT))
                  DEVM_UnPairRemoteDevice(BD_ADDR, (LowEnergy ? DEVM_UNPAIR_REMOTE_DEVICE_FLAGS_LOW_ENERGY : 0));

               /* If we had to attach the thread earlier, detach it now */
               /* so the JVM doesn't leak any resources.                */
               if(AttachResult > 0)
               {
                  NatData->VM->DetachCurrentThread();
               }
            }
            else
               SS1_LOGE("Could not attach thread to Java VM");
         }
         else
            SS1_LOGW("Timer expired for ID %u, but no remote device matches", TimerID);
      }
      else
         SS1_LOGE("Callback parameter uninitialized");
   }
   else
      SS1_LOGE("Callback parameter invalid");

   /* Return false to invalidate the timer.                             */
   return(FALSE);
}

   /* Callback used by CleanupDeviceLists which automatically frees the */
   /* memory referenced by the ExtraData component of the device list   */
   /* entry.                                                            */
static void CleanupCallback_FreeExtraData(BD_ADDR_t BD_ADDR, Boolean_t LowEnergy, void *ExtraData, void *CallbackParameter)
{
   /* If defined, the ExtraData of this device points to the            */
   /* DEVM_Authentication_Information_t structure from an authentication*/
   /* request.                                                          */
   if(ExtraData)
      free(ExtraData);
}

   /* Utility function for immediately emptying all device tracking     */
   /* lists and performing any necessary cleanup on those device        */
   /* entries.                                                          */
static void CleanupDeviceLists(BTEL_NativeData_t *NatData)
{
   if(NatData)
   {
      /* Remove all entries from the Name Pending For Found Devices     */
      /* list.  It is possible that a DEVM_Authentication_Information_t */
      /* structure is stored with the device; the callback will free the*/
      /* memory associated with this structure.                         */
      CleanDeviceList(&(NatData->NamePendingForAuthDevices), CleanupCallback_FreeExtraData, NULL);

#if BTEL_DELAY_FOUND_FOR_NAME
      /* Remove all entries from the Name Pending For Found Devices     */
      /* list.  No extra data is stored with with these entries, so no  */
      /* cleanup handler is required.                                   */
      CleanDeviceList(&(NatData->NamePendingForFoundDevices), NULL, NULL);
#endif

      /* The PairResponsePendingDevices list does not need to be cleaned*/
      /* because each entry is associated with a timer. The timer       */
      /* callback will take care of cleaning each entry as it expires.  */

      /* Remove all entries from the Service Discovery Pending Devices  */
      /* list.  No extra data is stored with with these entries, so no  */
      /* cleanup handler is required.                                   */
      /*                                                                */
      /* TODO: May need to add a cleanup handler to                     */
      /*       notify Android that the query failed via                 */
      /*       Method_onDiscoverServicesResult.                         */
      CleanDeviceList(&(NatData->ServiceDiscoveryPendingDevices), NULL, NULL);
   }
}

void BTEL_DEVMEventCallback(DEVM_Event_Data_t *EventData, void *CallbackParameter)
{
   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   int                                AttachResult;
   int                                ResultCode;
   char                               StringBuffer[256];
   jsize                              PropPosition;
   JNIEnv                            *Env;
   jobject                            BTSObject;
   jstring                            AddressString;
   jstring                            PathString;
   jstring                            UuidDelimitedString;
   BD_ADDR_t                          BD_ADDR;
   Boolean_t                          IsLowEnergy;
   jthrowable                         Exception;
   jobjectArray                       PropArray;
   jobjectArray                       SinglePropArray;
   unsigned int                       i;
   unsigned int                       TimerID;
   unsigned long                      ChangedMask;
   BTEL_NativeData_t                 *NatData;
   DEVM_Local_Device_Properties_t     LocalDevProps;
   DEVM_Remote_Device_Properties_t   *RemoteDevProps;
   DEVM_Remote_Device_Properties_t    RemoteDeviceProperties;
   DEVM_Authentication_Information_t *AuthReqInfo;

#if ANNOUNCE_BOND_AFTER_SDP

   Boolean_t                          AnnouncePairing  = FALSE;

#endif /* ANNOUNCE_BOND_AFTER_SDP  */

   *(void **)(&NatData) = CallbackParameter;

   if(NatData)
   {
      /* Attach the thread (if necessary) to the JVM so we can make     */
      /* calls into Java code.                                          */
      if((AttachResult = AttachThreadToJVM(NatData->VM, NatData->JNIEnvVersion, &Env)) >= 0)
      {
         if(EventData)
         {
            switch(EventData->EventType)
            {
                  /* Android never expects to see these three Device    */
                  /* Power status messages because BlueZ doesn't send   */
                  /* them during normal operation. Instead, "Powered"   */
                  /* status changes are used as part of the recovery    */
                  /* process when server communications are lost. See   */
                  /* BTEL_IPCLostCallback() for more info.              */
               case detDevicePoweredOn:
                  SS1_LOGI("Signal: DevicePoweredOn");

                  /* Note that we will need to announce the power       */
                  /* transition. This will be done after the chip       */
                  /* becomes connectable.                               */
                  NatData->PoweredOnStateAnnounced = false;
                  break;
               case detDevicePoweringOff:
                  SS1_LOGI("Signal: DevicePoweringOff");

                  /* Announce the power transition.                     */
                  HandlePropertyChanged(Env, NatData->Object, "Powered", "false");
                  break;

               case detDevicePoweredOff:
                  SS1_LOGI("Signal: DevicePoweredOff");

                  CleanupDeviceLists(NatData);

                  /* During a legitimate (user-initiated) shutdown of   */
                  /* Bluetooth, we will see this message after all      */
                  /* server-side services have acknowledged the shutdown*/
                  /* and cleaned up.                                    */
                  if(pthread_mutex_lock(&(NatData->DevicePoweredOffMutex)) == 0)
                  {
                     pthread_cond_broadcast(&(NatData->DevicePoweredOffCondition));
                     pthread_mutex_unlock(&(NatData->DevicePoweredOffMutex));
                  }
                  break;

               case detLocalDevicePropertiesChanged:
                  SS1_LOGI("Signal: LocalDevicePropertiesChanged (0x%02lx)", EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask);

                  /* Only announce the connectable mode change (which   */
                  /* will be translated as a "Powered=True" property)   */
                  /* if we are now connectable and the "Powered" state  */
                  /* has not yet been announced. The "Powered=False"    */
                  /* property change will be announced when the stack is*/
                  /* actually shutting down.                            */
                  if((EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_CONNECTABLE_MODE) && (!(NatData->PoweredOnStateAnnounced) || (EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties.ConnectableMode)))
                  {
                     /* The power state has not yet been announced and  */
                     /* we just became connectable. Note that the power */
                     /* state will be announced.                        */
                     NatData->PoweredOnStateAnnounced = true;
                  }
                  else
                  {
                     /* Filter the connectability property change       */
                     /* because either it has already been announced or */
                     /* it has been disabled. Android does not care if  */
                     /* we become non-connectable, but it will consider */
                     /* a second transition into connectable mode to    */
                     /* mean that the stack was restarted and will      */
                     /* respond by shutting down and re-initing the     */
                     /* stack itself.                                   */
                     EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask &= ~DEVM_LOCAL_DEVICE_PROPERTIES_CHANGED_CONNECTABLE_MODE;
                  }

                  ChangedMask = BTPMChangedLocalPropsToBTJB(EventData->EventData.LocalDevicePropertiesChangedEventData.ChangedMemberMask);

                  /* The POWERED and DISCOVERING properties are handled */
                  /* elsewhere.  The POWERED property is a special case */
                  /* which is triggered on demand.  For the DISCOVERING */
                  /* property, see the detDeviceDiscoveryStarted and    */
                  /* detDeviceDiscoveryStopped event handlers.          */
                  ChangedMask &= ~LOCAL_PROPERTY_POWERED;
                  ChangedMask &= ~LOCAL_PROPERTY_DISCOVERING;

                  if(ChangedMask != 0)
                  {
                     PropArray = BuildLocalPropertyStringArray(Env, ChangedMask, &(EventData->EventData.LocalDevicePropertiesChangedEventData.LocalDeviceProperties));
                     PropPosition = 0;
                     SinglePropArray = SplitNextProp(Env, PropArray, &PropPosition);
                     while(SinglePropArray)
                     {
                        {
                           /* XXX DEBUG */
                           SS1_LOGI("Sending single property:");
                           DebugDumpStringArray(Env, SinglePropArray, __FUNCTION__);
                        }
                        Env->CallVoidMethod(NatData->Object, Method_onPropertyChanged, SinglePropArray);
                        Env->DeleteLocalRef(SinglePropArray);
                        SinglePropArray = SplitNextProp(Env, PropArray, &PropPosition);
                     }
                     Env->DeleteLocalRef(PropArray);
                  }
                  else
                  {
                     /* XXX DEBUG */
                     SS1_LOGI("Received LocalDevicePropertiesChanged signal, but didn't care about any of the properties");
                  }
                  break;

               case detDeviceDiscoveryStarted:
                  SS1_LOGI("Signal: DeviceDiscoveryStarted");
                  HandlePropertyChanged(Env, NatData->Object, "Discovering", "true");
                  break;

               case detDeviceDiscoveryStopped:
                  SS1_LOGI("Signal: DeviceDiscoveryStopped");

#if BTEL_DELAY_FOUND_FOR_NAME
                  /* Process any discovered devices which were still    */
                  /* waiting on a name discovery when the device        */
                  /* discovery process ended.                           */
                  while(RemoveDeviceFromListAny(&(NatData->NamePendingForFoundDevices), &BD_ADDR, &IsLowEnergy, NULL))
                  {
                     /* We found devices still waiting. This suggests   */
                     /* that these devices never responded to any name  */
                     /* requests, so go ahead and get their properties  */
                     /* and announce that they was found.               */
                     if(BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), BD_ADDR) && ((AddressString = Env->NewStringUTF(StringBuffer)) != NULL))
                     {
                        SS1_LOGI("DeviceDiscoveryStopped: Processing unnamed device (%s)", StringBuffer);
                        if((ResultCode = DEVM_QueryRemoteDeviceProperties(BD_ADDR, (IsLowEnergy ? DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_LOW_ENERGY : 0), &RemoteDeviceProperties)) == 0)
                        {
                           if((PropArray = BuildRemotePropertyStringArray(Env, REMOTE_PROPERTY_ALL, &RemoteDeviceProperties)) != NULL)
                           {
                              SS1_LOGI("DeviceDiscoveryStopped: Announcing unnamed device (%s)", StringBuffer);
                              Env->CallVoidMethod(NatData->Object, Method_onDeviceFound, AddressString, PropArray);
                              Env->DeleteLocalRef(PropArray);
                           }
                           else
                              SS1_LOGE("DeviceDiscoveryStopped: Couldn't build PropArray (%s)", StringBuffer);

                           Env->DeleteLocalRef(AddressString);
                        }
                        else
                           SS1_LOGE("DeviceDiscoveryStopped: Couldn't query device props (%s) (%d)", StringBuffer, ResultCode);
                     }
                     else
                        SS1_LOGE("DeviceDiscoveryStopped: Couldn't convert BD_ADDR to string");
                  }
#endif /* BTEL_DELAY_FOUND_FOR_NAME */

                  /* Now that all "found" notifications are processed,  */
                  /* we can announce that the discovery is finished.    */
                  HandlePropertyChanged(Env, NatData->Object, "Discovering", "false");
                  break;

               case detRemoteDeviceFound:
                  SS1_LOGI("Signal: RemoteDeviceFound");

                  RemoteDevProps = &(EventData->EventData.RemoteDeviceFoundEventData.RemoteDeviceProperties);
                  BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), RemoteDevProps->BD_ADDR);

                  /* DeviceFound should only be forwarded up to the     */
                  /* service if we are in a discovery. Otherwise, treat */
                  /* it like a device created and add the device's      */
                  /* properties to the BTS cache.                       */
                  if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                  {
                     /* We are in a discovery, so we can treat this like*/
                     /* a DeviceFound event.                            */
                     SS1_LOGI("Device found during discovery (%s)", StringBuffer);

#if BTEL_DELAY_FOUND_FOR_NAME
                     /* But first, check whether we have the device     */
                     /* name. If so, we can announce the device as      */
                     /* found. If not, queue it for later notification  */
                     /* once the name comes in.                         */
                     if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
                     {
                        /* Name is known, so go ahead and send the      */
                        /* notification.                                */
                        SS1_LOGI("Device found with name (%s): sending notification.", StringBuffer);
#endif /* BTEL_DELAY_FOUND_FOR_NAME */
                        if((AddressString = Env->NewStringUTF(StringBuffer)) != NULL)
                        {
                           if((PropArray = BuildRemotePropertyStringArray(Env, REMOTE_PROPERTY_ALL, RemoteDevProps)) != NULL)
                           {
                              Env->CallVoidMethod(NatData->Object, Method_onDeviceFound, AddressString, PropArray);
                              Env->DeleteLocalRef(PropArray);
                           }
                           Env->DeleteLocalRef(AddressString);
                        }
#if BTEL_DELAY_FOUND_FOR_NAME
                     }
                     else
                     {
                        /* Name is not known. Track the device so we    */
                        /* can send notification after the name is      */
                        /* retrieved.                                   */
                        if(AddDeviceToList(&(NatData->NamePendingForFoundDevices), RemoteDevProps->BD_ADDR, FALSE, NULL) == ADD_DEVICE_SUCCESS)
                           SS1_LOGI("Found device with no name (%s): added to pending list.", StringBuffer);
                        else
                           SS1_LOGW("Found device with no name (%s): could not add to pending list.", StringBuffer);
                     }
#endif /* BTEL_DELAY_FOUND_FOR_NAME */
                  }
                  else
                  {
                     SS1_LOGI("Device found outside discovery (%s)", StringBuffer);
                  }
                  break;

               case detRemoteDeviceDeleted:
                  SS1_LOGI("Signal: RemoteDeviceDeleted");
                  BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), EventData->EventData.RemoteDeviceDeletedEventData.RemoteDeviceAddress);
                  AddressString = Env->NewStringUTF(StringBuffer);
                  Env->CallVoidMethod(NatData->Object, Method_onDeviceDisappeared, AddressString);
                  Env->DeleteLocalRef(AddressString);
                  break;

               case detRemoteDevicePropertiesChanged:
                  SS1_LOGI("Signal: RemoteDevicePropertiesChanged (0x%02lx)", EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask);
                  RemoteDevProps = &(EventData->EventData.RemoteDevicePropertiesChangedEventData.RemoteDeviceProperties);

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), RemoteDevProps->BD_ADDR);
                  PathString = Env->NewStringUTF(StringBuffer);

                  BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), RemoteDevProps->BD_ADDR);
                  AddressString = Env->NewStringUTF(StringBuffer);

                  if(EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_DEVICE_FLAGS)
                  {
                     /* If a device is either Paired or Connected,      */
                     /* it is considered "known". If the device's       */
                     /* known-state changes, we notify the upper layers */
                     /* via a callback. First, check whether the device */
                     /* is known.                                       */
                     if((RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED) || (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED) || (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE) || (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE))
                     {
                        /* If this is due to the device becoming paired,*/
                        /* and we do not already have the device's SDP  */
                        /* records cached, request them now.            */
                        if((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_PAIRING_STATE) && (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED))
                        {
                           if(!(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN))
                           {
                              /* Normally, the PM Server will           */
                              /* automatically request SDP records after*/
                              /* pairing is completed. It will not do   */
                              /* this if the connection was initiated   */
                              /* remotely. Only request the records     */
                              /* if we are connected and we did not     */
                              /* initiate the connection.               */
                              if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED)
                              {
                                 if(!(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY))
                                 {
                                    ResultCode = DEVM_QueryRemoteDeviceServices(RemoteDevProps->BD_ADDR, DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_FORCE_UPDATE, 0, NULL, NULL);

                                    if(ResultCode == BTPM_ERROR_CODE_SERVICE_DISCOVERY_IN_PROGRESS)
                                       SS1_LOGE("SDP record request for newly paired device already in progress (%d)", ResultCode);
                                    else
                                    {
                                       if(ResultCode < 0)
                                          SS1_LOGE("Unable to request SDP records of newly paired device (%d)", ResultCode);
                                       else
                                          SS1_LOGE("Began SDP record request for newly paired device (%d)", ResultCode);
                                    }
                                 }
                              }
                              else
                                 SS1_LOGE("Remote device became paired but is not currently connected. This should not happen");
                           }

                           /* Check if we were waiting on pairing to    */
                           /* complete.                                 */
                           if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), RemoteDevProps->BD_ADDR, FALSE, (void **)&TimerID))
                           {
                              /* Pairing was in progress, so stop the   */
                              /* timer.                                 */
                              if(TMR_StopTimer(TimerID))
                                 SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                              else
                                 SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);

#if ANNOUNCE_BOND_AFTER_SDP

                              /* If the link was initiated remotely     */
                              /* then report bonding success.           */
                              /* Otherwise, defer the announcement.     */                               
                              if((!(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY)) && (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED))
                              {
                                 SS1_LOGI("The remote device is now paired and the link was initiated remotely. Pairing success will be reported.");
                                 AnnouncePairing = TRUE;
                              }
                              else
                                 BTEL_AddPendingPairDevice(NatData, RemoteDevProps->BD_ADDR, FALSE);

#else

                              /* Announce status up to Java.            */
                              SS1_LOGI("... ... Reporting pairing success");
                              Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_SUCCESS);

#endif /* ANNOUNCE_BOND_AFTER_SDP  */                             

                           }
                        }

                        if((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_LE_PAIRING_STATE) && (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE))
                        {
                           if(!(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_SERVICES_KNOWN))
                           {
                              ResultCode = DEVM_QueryRemoteDeviceServices(RemoteDevProps->BD_ADDR, (DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_FORCE_UPDATE | DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY), 0, NULL, NULL);

                              if(ResultCode == BTPM_ERROR_CODE_SERVICE_DISCOVERY_IN_PROGRESS)
                                 SS1_LOGE("LE Service request for newly paired device already in progress (%d)", ResultCode);
                              else
                              {
                                 if(ResultCode < 0)
                                    SS1_LOGE("Unable to request LE Services of newly paired device (%d)", ResultCode);
                                 else
                                    SS1_LOGE("Began LE Service request for newly paired device (%d)", ResultCode);
                              }
                           }

                           /* Check if we were waiting on pairing to    */
                           /* complete.                                 */
                           if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), RemoteDevProps->BD_ADDR, TRUE, (void **)&TimerID))
                           {
                              /* Pairing was in progress, so stop the   */
                              /* timer.                                 */
                              if(TMR_StopTimer(TimerID))
                                 SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                              else
                                 SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);

                              /* Announce status up to Java.            */
                              SS1_LOGI("... ... Reporting pairing success");
                              Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_SUCCESS);
                           }
                        }

                        /* The device is known, so try adding it to the */
                        /* list of known devices.                       */
                        ResultCode = 0;

                        if((RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED) || (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED))
                        {
                           if(AddDeviceToList(&(NatData->KnownDevices), RemoteDevProps->BD_ADDR, FALSE, NULL) == ADD_DEVICE_SUCCESS)
                              ResultCode |= 0x01;
                        }

                        if((RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE) || (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE))
                        {
                           if(AddDeviceToList(&(NatData->KnownDevices), RemoteDevProps->BD_ADDR, TRUE, NULL) == ADD_DEVICE_SUCCESS)
                              ResultCode |= 0x02;
                        }

                        /* If either a BD/EDR or LE device was added to */
                        /* the known device list, announce this change. */
                        if(ResultCode)
                        {
                           /* We were able to add the device, meaning   */
                           /* it was not known before now, so call up   */
                           /* to Java to announce this change.          */
                           SS1_LOGI("Remote Device Properties Changed: Added device to KnownDevices list (%s)", StringBuffer);

#if BTEL_DELAY_FOUND_FOR_NAME
                           /* Check whether we have the device name. If */
                           /* so, we can announce the device as found.  */
                           /* If not, queue it for later notification   */
                           /* once the name comes in.                   */
                           if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
                           {
                              /* Name is known, so go ahead and send the*/
                              /* notification.                          */
                              SS1_LOGI("Device found with name (%s): sending notification.", StringBuffer);
#endif /* BTEL_DELAY_FOUND_FOR_NAME */

                              HandleDeviceCreated(Env, NatData, RemoteDevProps);

#if BTEL_DELAY_FOUND_FOR_NAME
                           }
                           else
                           {
                              /* Name is not known. Track the device so */
                              /* we can send notification after the name*/
                              /* is retrieved.                          */
                              if(AddDeviceToList(&(NatData->NamePendingForFoundDevices), RemoteDevProps->BD_ADDR, FALSE, NULL) == ADD_DEVICE_SUCCESS)
                                 SS1_LOGI("Found device with no name (%s): added to pending list.", StringBuffer);
                              else
                                 SS1_LOGW("Found device with no name (%s): could not add to pending list.", StringBuffer);
                           }
#endif /* BTEL_DELAY_FOUND_FOR_NAME */
                        }
                        else
                        {
                           /* The device was already known, so there's  */
                           /* nothing to be done.                       */
                           SS1_LOGI("Remote Device Properties Changed: Device already in KnownDevices list (%s)", StringBuffer);
                        }
                     }
                     else
                     {
                        ResultCode = 0;

                        /* The device is unknown, so try removing it    */
                        /* from the list of known devices.              */
                        if(RemoveDeviceFromList(&(NatData->KnownDevices), RemoteDevProps->BD_ADDR, FALSE, NULL))
                           ResultCode = 1;

                        if(RemoveDeviceFromList(&(NatData->KnownDevices), RemoteDevProps->BD_ADDR, TRUE, NULL))
                           ResultCode = 1;

                        /* Continue if either a BR/EDR and/or LE device */
                        /* was removed from the list.                   */
                        if(ResultCode)
                        {
                           ResultCode = 0;

                           /* We were able to remove the device, meaning*/
                           /* it has disconnected without being paired. */
                           /* If we were waiting on a pairing process,  */
                           /* this suggests that the pairing failed,    */
                           /* so first try cleaning up auth-related     */
                           /* resources.                                */
                           if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), RemoteDevProps->BD_ADDR, FALSE, (void **)&TimerID))
                           {
                              if(TMR_StopTimer(TimerID))
                                 SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                              else
                                 SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);

                              ResultCode = 1;
                           }

                           if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), RemoteDevProps->BD_ADDR, TRUE, (void **)&TimerID))
                           {
                              if(TMR_StopTimer(TimerID))
                                 SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                              else
                                 SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);

                              ResultCode = 1;
                           }

                           /* If either a BR/EDR or LE device with this */
                           /* address was removed from the pending pair */
                           /* list, announce the auth failure.          */
                           if(ResultCode)
                           {
                              /* Report the authentication failure.     */
                              SS1_LOGI("... ... Failure reason: Remote Device Down");
                              Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_REMOTE_DEVICE_DOWN);
                           }

                           /* Now, call up to Java to announce that a   */
                           /* device with this address change.          */
                           SS1_LOGI("Remote Device Properties Changed: Device removed from KnownDevices list (%s)", StringBuffer);
                           Env->CallVoidMethod(NatData->Object, Method_onDeviceRemoved, PathString);
                        }
                        else
                        {
                           /* The device was already unknown, so there's*/
                           /* nothing to be done.                       */
                           SS1_LOGI("Remote Device Properties Changed: Device not found in KnownDevices list (%s)", StringBuffer);
                        }
                     }
 
#if ANNOUNCE_BOND_AFTER_SDP

                     /* If the connection state has changed and the     */
                     /* device is not connected but it is paired and is */
                     /* in the pending pair response list then report   */
                     /* pairing success.                                */
                     if((EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask & DEVM_REMOTE_DEVICE_PROPERTIES_CHANGED_CONNECTION_STATE) && !(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED) && (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED))
                     {
                        /* Check if we were waiting on pairing to       */
                        /* complete.                                    */
                        if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), RemoteDevProps->BD_ADDR, FALSE, (void **)&TimerID))
                        {
                           /* Pairing was in progress, so stop the      */
                           /* timer.                                    */
                           if(TMR_StopTimer(TimerID))
                              SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                           else
                              SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);

                           SS1_LOGI("Connection state has changed and the device is not connected but it is paired. Pairing success will be reported.");
                           AnnouncePairing = TRUE;
                        }
                     }

#endif /* ANNOUNCE_BOND_AFTER_SDP  */ 

                  }

#if BTEL_DELAY_FOUND_FOR_NAME
                  /* If we know the name, check whether a notification  */
                  /* is pending on discovering this device's name.      */
                  if((RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN) && (RemoveDeviceFromList(&(NatData->NamePendingForFoundDevices), RemoteDevProps->BD_ADDR, FALSE, NULL)))
                  {
                     /* The device is waiting on a name request, and we */
                     /* now have the name, so we can go ahead and send  */
                     /* the "found" notification.                       */
                     {
                        /* XXX DEBUG */
                        if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
                           SS1_LOGI("Remote Device Properties Changed: Device with pending dev-found (%s), name retrieved successfully", StringBuffer);
                        else
                           SS1_LOGI("Remote Device Properties Changed: Device with pending dev-found (%s), name was NOT retrieved", StringBuffer);
                     }

                     if((PropArray = BuildRemotePropertyStringArray(Env, REMOTE_PROPERTY_ALL, RemoteDevProps)) != NULL)
                     {
                        Env->CallVoidMethod(NatData->Object, Method_onDeviceFound, AddressString, PropArray);
                        Env->DeleteLocalRef(PropArray);
                     }
                  }
#endif /* BTEL_DELAY_FOUND_FOR_NAME */

                  UuidDelimitedString = NULL;
  
                  /* While setting the mask for all property changes,   */
                  /* test for a change in the service UUID. If that bit */
                  /* is set, then attempt to retrieve the UUID cached   */
                  /* by Android in order to compare that set of UUID    */
                  /* with the set in the platform manager's remote      */
                  /* device properties.                                 */
                  if((ChangedMask = BTPMChangedRemotePropsToBTJB(EventData->EventData.RemoteDevicePropertiesChangedEventData.ChangedMemberMask)) & REMOTE_PROPERTY_UUIDS)
                  {

#if ANNOUNCE_BOND_AFTER_SDP

                        /* If the link was initiated locally and the    */
                        /* device is paired send the deferred bond      */
                        /* success report.                              */
                        if((RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY) && (RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED))
                        {
                           /* Check if we were waiting on pairing to    */
                           /* complete.                                 */
                           if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), RemoteDevProps->BD_ADDR, FALSE, (void **)&TimerID))
                           {
                              /* Pairing was in progress, so stop the   */
                              /* timer.                                 */
                              if(TMR_StopTimer(TimerID))
                                 SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                              else
                                 SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);

                              SS1_LOGI("UUID will be updated and the device is paired. Pairing success will be reported.");
                              AnnouncePairing = TRUE;
                           }
                        }

#endif /* ANNOUNCE_BOND_AFTER_SDP  */ 

#if WITHOLD_IDENTICAL_UUID_SETS

                     if((BTSObject = Env->GetObjectField(NatData->Object, Field_mBluetoothService)) != NULL)
                     {
                        if(BTS_CallIsRemoteDeviceInCache(Env, BTSObject, AddressString) == JNI_TRUE)
                        {

#if (SS1_PLATFORM_SDK_VERSION < 14)

                           UuidDelimitedString = BTS_CallGetRemoteDeviceProperty(Env, BTSObject, AddressString, Env->NewStringUTF("UUIDs"));
                           if(UuidDelimitedString)
                              SS1_LOGI("Delimited string of UUID cached by Android: %s", Env->GetStringUTFChars(UuidDelimitedString, NULL));
                           else
                              SS1_LOGI("Delimited string of UUID cached by Android: NULL");

#endif /* (SS1_PLATFORM_SDK_VERSION < 14) */

                        }
                     }                 
#endif /* WITHOLD_IDENTICAL_UUID_SETS */

                  }

                  if(ChangedMask != 0)
                  {
                     SS1_LOGI("Remote Device Properties Changed: Processing device path %s", StringBuffer);
                     PropArray = BuildRemotePropertyStringArray(Env, ChangedMask, RemoteDevProps);
                     PropPosition = 0;
                     SinglePropArray = SplitNextProp(Env, PropArray, &PropPosition);

#if WITHOLD_IDENTICAL_UUID_SETS

                     /* If there is no difference bewteen the services  */
                     /* that are currently cached by android and those  */
                     /* in the remote device properties the array will  */
                     /* be skipped.                                     */
                     if((UuidDelimitedString) && (SinglePropArray))
                     {
                        if(IdenticalUuid(Env, SinglePropArray, UuidDelimitedString) == JNI_TRUE)
                        {
                           SinglePropArray = SplitNextProp(Env, PropArray, &PropPosition);
                           SS1_LOGI("No change in UUID. They will not be included in the properties change update.");
                        }
                        else
                           SS1_LOGI("The UUID will be included in the properties change update.");
                     }
   
#endif /* WITHOLD_IDENTICAL_UUID_SETS */

                     while(SinglePropArray)
                     {
                        {
                           /* XXX DEBUG */
                           SS1_LOGI("Remote Device Properties Changed: Calling onDevicePropertyChanged with...");
                           DebugDumpStringArray(Env, SinglePropArray, __FUNCTION__);
                        }
                        Env->CallVoidMethod(NatData->Object, Method_onDevicePropertyChanged, PathString, SinglePropArray);
                        Env->DeleteLocalRef(SinglePropArray);
                        SinglePropArray = SplitNextProp(Env, PropArray, &PropPosition);

#if WITHOLD_IDENTICAL_UUID_SETS

                        if((UuidDelimitedString) && (SinglePropArray))
                        {
                           if(IdenticalUuid(Env, SinglePropArray, UuidDelimitedString) == JNI_TRUE)
                           {
                              SinglePropArray = SplitNextProp(Env, PropArray, &PropPosition);
                              SS1_LOGI("No change in UUID. They will not be included in the properties change update.");
                           }
                           else
                              SS1_LOGI("The UUID will be included in the properties change update.");
                        }
   
#endif /* WITHOLD_IDENTICAL_UUID_SETS */

                     }
                     Env->DeleteLocalRef(PropArray);
                     Env->DeleteLocalRef(UuidDelimitedString);
                  }
                  else
                  {
                     /* XXX DEBUG */
                     SS1_LOGI("Remote Device Properties Changed: Received signal, but didn't care about any of the properties.");
                  }

#if ANNOUNCE_BOND_AFTER_SDP
                  
                  if(AnnouncePairing)
                  {               
                     /* Announce status up to Java.         */
                     SS1_LOGI("... ... Reporting pairing success");
                     Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_SUCCESS);
                  }

#endif /* ANNOUNCE_BOND_AFTER_SDP  */ 

                  Env->DeleteLocalRef(PathString);
                  Env->DeleteLocalRef(AddressString);

                  SS1_LOGI("Finished RemoteDevicePropertiesChanged");
                  break;

               case detRemoteDevicePropertiesStatus:
                  SS1_LOGI("Signal: RemoteDevicePropertiesStatus");

                  AuthReqInfo = NULL;

                  RemoteDevProps = &(EventData->EventData.RemoteDevicePropertiesStatusEventData.RemoteDeviceProperties);

                  /* Check the list of devices with pending             */
                  /* authentication requests waiting on device name     */
                  /* inquiries.                                         */
                  ResultCode = 0;

                  /* Check whether a Classic pairing process is in      */
                  /* progress for this device and was postponed for a   */
                  /* name request.                                      */
                  if(RemoveDeviceFromList(&(NatData->NamePendingForAuthDevices), RemoteDevProps->BD_ADDR, FALSE, (void **)&AuthReqInfo))
                  {
                     /* The device is waiting on a name request.        */
                     /* Whether or not we actually got the remote       */
                     /* device name, we should go ahead and handle the  */
                     /* authentication request since it's possible that */
                     /* the remote device has no name to give.          */
                     if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
                        SS1_LOGI("Property update for device with pending auth (%02X:%02X:%02X:%02X:%02X:%02X), name retreived successfully", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
                     else
                        SS1_LOGI("Property update for device with pending auth (%02X:%02X:%02X:%02X:%02X:%02X), name was NOT retreived", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);

                     HandleAuthenticationRequest(Env, AuthReqInfo, NatData);

                     if(AuthReqInfo)
                     {
                        free(AuthReqInfo);
                        AuthReqInfo = NULL;
                     }
                  }

                  /* Check whether a Low-Energy pairing process is in   */
                  /* progress for this device and was postponed for a   */
                  /* name request.                                      */
                  if(RemoveDeviceFromList(&(NatData->NamePendingForAuthDevices), RemoteDevProps->BD_ADDR, TRUE, (void **)&AuthReqInfo))
                  {
                     /* The device is waiting on a name request.        */
                     /* Whether or not we actually got the remote       */
                     /* device name, we should go ahead and handle the  */
                     /* authentication request since it's possible that */
                     /* the remote device has no name to give.          */
                     if(RemoteDevProps->RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
                        SS1_LOGI("Property update for device with pending auth (%02X:%02X:%02X:%02X:%02X:%02X), name retreived successfully", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
                     else
                        SS1_LOGI("Property update for device with pending auth (%02X:%02X:%02X:%02X:%02X:%02X), name was NOT retreived", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);

                     HandleAuthenticationRequest(Env, AuthReqInfo, NatData);

                     if(AuthReqInfo)
                     {
                        free(AuthReqInfo);
                        AuthReqInfo = NULL;
                     }
                  }

                  break;

               case detRemoteDeviceServicesStatus:
                  SS1_LOGI("Signal: RemoteDeviceServicesStatus");

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress);
                  if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                  {
                     if(pthread_mutex_lock(&(NatData->PendingCreateDeviceMutex)) == 0)
                     {
                        for(i = 0; i < NatData->PendingCreateDeviceNumber; i++)
                        {
                           if(COMPARE_BD_ADDR(EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress, NatData->PendingCreateDeviceList[i]))
                           {
                              /* We found a match in the list, so       */
                              /* remove it and set "i" to a sentinel    */
                              /* value (UINT_MAX) indicating that we    */
                              /* found the device.                      */
                              NatData->PendingCreateDeviceNumber -= 1;
                              if(i < NatData->PendingCreateDeviceNumber)
                                 NatData->PendingCreateDeviceList[i] = NatData->PendingCreateDeviceList[NatData->PendingCreateDeviceNumber];

                              i = UINT_MAX;

                              /* Notify anyone waiting for space to     */
                              /* become available in the list.          */
                              pthread_cond_broadcast(&(NatData->PendingCreateDeviceRemovalCondition));

                              break;
                           }
                        }

                        /* At this point, we are done with the shared   */
                        /* resource, so we can release our lock.        */
                        pthread_mutex_unlock(&(NatData->PendingCreateDeviceMutex));

                        if(i == UINT_MAX)
                        {
                           /* We found the device in the list of        */
                           /* pending Create Device requests, so this   */
                           /* message is the result of a successful     */
                           /* call to BTS_CreateDeviceNative(). Send    */
                           /* status up to Java via onDeviceCreated().  */
                           if(EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_SUCCESS)
                              Env->CallVoidMethod(NatData->Object, Method_onDeviceCreated, PathString);
                        }
                        else
                        {
                           /* The device was not found in the list of   */
                           /* pending Create Device requests, so this   */
                           /* message is the result of a straight call  */
                           /* to DEVM_QueryRemoteDeviceServices() on a  */
                           /* known device.                             */

                           /* Check whether this event was due to a call*/
                           /* to BTS_DiscoverServicesNative().          */
                           if(RemoveDeviceFromList(&(NatData->ServiceDiscoveryPendingDevices), EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress, (EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_LOW_ENERGY), NULL) == true)
                           {
                              /* This device was in the                 */
                              /* Discovery-Pending list, so this        */
                              /* event is a response to a call to       */
                              /* BTS_DiscoverServicesNative() and we    */
                              /* should handle it by calling up to Java */
                              /* via onDiscoverServicesResult().        */
                              if(!(EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_LOW_ENERGY))
                                 Env->CallVoidMethod(NatData->Object, Method_onDiscoverServicesResult, PathString, ((EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_SUCCESS) ? JNI_TRUE : JNI_FALSE));
                              //XXX TODO Add support for LE services
                           }
                           else
                           {
                              /* DEBUG */ SS1_LOGD("(RemoteDeviceServicesStatus) Ignoring completion since it's not for a new device nor for a DiscoverServicesNative() call.");
                           }
                        }
                     }

#if ANNOUNCE_BOND_AFTER_SDP

                     if(!(EventData->EventData.RemoteDeviceServicesStatusEventData.StatusFlags & DEVM_REMOTE_DEVICE_SERVICES_STATUS_FLAGS_SUCCESS))
                     {
                        /* If the link was initiated locally and the    */
                        /* device is paired and is in the pending pair  */
                        /* response list, then report pairing success.  */
                        if(DEVM_QueryRemoteDeviceProperties(EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress, FALSE, &RemoteDeviceProperties) == 0)
                        {
                           if((RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED) && (RemoteDeviceProperties.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY))
                           {
                              /* Check if we were waiting on pairing to */
                              /* complete.                              */
                              if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress, FALSE, (void **)&TimerID))
                              {
                                 /* Pairing was in progress, so stop    */
                                 /* the timer.                          */
                                 if(TMR_StopTimer(TimerID))
                                    SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                                 else
                                    SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);
 
                                 /* Announce status up to Java.         */
                                 BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), EventData->EventData.RemoteDeviceServicesStatusEventData.RemoteDeviceAddress);

                                 if((AddressString = Env->NewStringUTF(StringBuffer)) != NULL)
                                 {
                                    SS1_LOGI("... ... Reporting pairing success: detRemoteDeviceServicesStatus");
                                    Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_SUCCESS);
                     
                                    Env->DeleteLocalRef(AddressString);
                                 }
                              }
                           }
                        }
                     }
                         
#endif /* ANNOUNCE_BOND_AFTER_SDP  */

                     Env->DeleteLocalRef(PathString);
                  }

                  break;

               case detRemoteDevicePairingStatus:
                  SS1_LOGI("Signal: RemoteDevicePairingStatus");

                  BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress);
                  AddressString = Env->NewStringUTF(StringBuffer);

                  SS1_LOGI("... ... Auth Status = (0x%02x)", EventData->EventData.RemoteDevicePairingStatusEventData.AuthenticationStatus);

                  /* Send the status up to Java.                        */
                  if(EventData->EventData.RemoteDevicePairingStatusEventData.Success == FALSE)
                  {
                     /* Cancel any pending pairing time-out timer.      */
                     if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress, (EventData->EventData.RemoteDevicePairingStatusEventData.AuthenticationStatus & DEVM_REMOTE_DEVICE_PAIRING_STATUS_FLAGS_LOW_ENERGY), (void **)&TimerID))
                     {
                        if(TMR_StopTimer(TimerID))
                           SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                        else
                           SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);
                     }

                     SS1_LOGI("... ... Reporting pairing failure");
                     switch(EventData->EventData.RemoteDevicePairingStatusEventData.AuthenticationStatus)
                     {
                        case HCI_ERROR_CODE_AUTHENTICATION_FAILURE:
                           SS1_LOGI("... ... Failure reason: Auth Failed");
                           Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_AUTH_FAILED);
                           break;
                        case HCI_ERROR_CODE_PAIRING_NOT_ALLOWED:
                           SS1_LOGI("... ... Failure reason: Auth Rejected");
                           Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_AUTH_REJECTED);
                           break;
                        case HCI_ERROR_CODE_PAGE_TIMEOUT:
                           SS1_LOGI("... ... Failure reason: Remote Device Down - HCI_ERROR_CODE_PAGE_TIMEOUT");
                           Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_REMOTE_DEVICE_DOWN);
                           break;
                        case HCI_ERROR_CODE_REPEATED_ATTEMPTS:
                           SS1_LOGI("... ... Failure reason: Too Many Repeated Attempts");
                           Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_REPEATED_ATTEMPTS);
                           break;
                        case HCI_ERROR_CODE_HOST_TIMEOUT:
                           SS1_LOGI("... ... Failure reason: Remote Device Down - HCI_ERROR_CODE_HOST_TIMEOUT");
                           Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_REMOTE_DEVICE_DOWN);
                           break;
                        default:
                           SS1_LOGI("... ... Failure reason: Unknown (%u)", EventData->EventData.RemoteDevicePairingStatusEventData.AuthenticationStatus);
                           Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_ERROR);
                           break;
                     }
                     /* TODO Check AuthenticationStatus field for       */
                     /* additional failure reasons and report them      */
                     /* according to the constants defined in           */
                     /* ARBTJBBTEL.h.                                   */
                  }
                  else
                  {

                     /* Cancel any pending pairing time-out timer.      */
                     if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress, (EventData->EventData.RemoteDevicePairingStatusEventData.AuthenticationStatus & DEVM_REMOTE_DEVICE_PAIRING_STATUS_FLAGS_LOW_ENERGY), (void **)&TimerID))
                     {
                        if(TMR_StopTimer(TimerID))
                           SS1_LOGI("Remote Device Properties Changed: Auth Timer canceled for device %s (TimerID = %u)", StringBuffer, TimerID);
                        else
                           SS1_LOGI("Remote Device Properties Changed: Could not stop Auth Timer for device %s (TimerID = %u)", StringBuffer, TimerID);

#if ANNOUNCE_BOND_AFTER_SDP

                        BTEL_AddPendingPairDevice(NatData, EventData->EventData.RemoteDevicePairingStatusEventData.RemoteDeviceAddress, FALSE);
                     }

#else

                        SS1_LOGI("... ... Reporting pairing success: detRemoteDevicePairingStatus");
                        Env->CallVoidMethod(NatData->Object, Method_onCreatePairedDeviceResult, AddressString, BOND_RESULT_SUCCESS);
                     }

#endif /* ANNOUNCE_BOND_AFTER_SDP  */

                  }
                  break;

               case detRemoteDeviceAuthenticationStatus:
                  SS1_LOGI("Signal: RemoteDeviceAuthenticationStatus");
                  //XXX
                  break;

               case detRemoteDeviceEncryptionStatus:
                  SS1_LOGI("Signal: RemoteDeviceEncryptionStatus");
                  //XXX
                  break;

               case detRemoteDeviceConnectionStatus:
                  SS1_LOGI("Signal: RemoteDeviceConnectionStatus");
                  //XXX
                  break;

               case detDeviceScanStarted:
                  SS1_LOGI("Signal: DeviceScanStarted");
                  //XXX
                  break;

               case detDeviceScanStopped:
                  SS1_LOGI("Signal: DeviceScanStopped");
                  //XXX
                  break;

               case detRemoteDeviceAddressChanged:
                  SS1_LOGI("Signal: RemoteDeviceAddressChanged");
                  //XXX
                  break;

               default:
                  SS1_LOGE("Received unknown event code %d", EventData->EventType);
            }
            if(Env->ExceptionCheck())
            {
               SS1_LOGW("Triggered exception:");
               Env->ExceptionDescribe();
               Env->ExceptionClear();
               SS1_LOGW("Exception cleared.");
            }
         }
         else
            SS1_LOGE("Callback did not provide a valid event information structure");

         /* If we had to attach the thread earlier, detach it now so    */
         /* the JVM doesn't leak any resources.                         */
         if(AttachResult > 0)
            NatData->VM->DetachCurrentThread();
      }
      else
         SS1_LOGE("Could not attach thread to Java VM");
   }
   else
      SS1_LOGE("Callback did not provide a valid parameter");

   SS1_LOGD("Exit");
}


void BTEL_DEVMAuthenticationCallback(DEVM_Authentication_Information_t *AuthReqInfo, void *CallbackParameter)
{
   SS1_LOGD("Enter (%p, %p)", AuthReqInfo, CallbackParameter);

   int                                Result;
   int                                AttachResult;
   bool                               WaitingOnNameRequest;
   jsize                              PropPosition;
   JNIEnv                            *Env;
   jthrowable                         Exception;
   BTEL_NativeData_t                 *NatData;
   DEVM_Remote_Device_Properties_t    RemoteDevProps;
   DEVM_Authentication_Information_t *AuthReqInfoCopy;

   WaitingOnNameRequest = false;

   *(void **)(&NatData) = CallbackParameter;

   if(NatData && AuthReqInfo)
   {
      /* Attach the thread (if necessary) to the JVM so we can make     */
      /* calls into Java code.                                          */
      if((AttachResult = AttachThreadToJVM(NatData->VM, NatData->JNIEnvVersion, &Env)) >= 0)
      {
         /* Every authentication process begins with either an I/O Cap  */
         /* Request or Pincode Request. Only in these cases should      */
         /* we check for whether we need to delay the authentication    */
         /* process.                                                    */
         if(((AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_ACTION_MASK) == DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_REQUEST) || ((AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_ACTION_MASK) == DEVM_AUTHENTICATION_ACTION_PIN_CODE_REQUEST))
         {
            if(DEVM_QueryRemoteDeviceProperties(AuthReqInfo->BD_ADDR, FALSE, &RemoteDevProps) == 0)
            {
               /* Check whether we know the remote device name. We would*/
               /* prefer to have the name to show the user when we      */
               /* request some form of interaction.                     */
               if(!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN))
               {
                  /* Check whether the device is currently connected    */
                  /* (rather than authentication in a pre-connection    */
                  /* state).                                            */
                  if((AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) ? (RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE) : (RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED))
                  {
                     /* Copy the AuthenticationRequestInformation       */
                     /* structure so we can postpone handling of this   */
                     /* event. The parameter given to the callback is   */
                     /* owned by the caller so it may not live as long  */
                     /* as we need.                                     */
                     if((AuthReqInfoCopy = (DEVM_Authentication_Information_t *) malloc(DEVM_AUTHENTICATION_INFORMATION_SIZE)) != NULL)
                     {
                        *AuthReqInfoCopy = *AuthReqInfo;

                        /* We don't know the name, so add the device to */
                        /* the list of devices waiting on name requests.*/
                        if(AddDeviceToList(&(NatData->NamePendingForAuthDevices), AuthReqInfo->BD_ADDR, (!!(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)), AuthReqInfoCopy) == ADD_DEVICE_SUCCESS)
                        {
                           /* And issue the name request. This          */
                           /* will come back as an event in the         */
                           /* DEVM_EventCallback() function.            */
                           Result = DEVM_QueryRemoteDeviceProperties(AuthReqInfo->BD_ADDR, (DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_FORCE_UPDATE | ((AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) ? DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_LOW_ENERGY : 0)), &RemoteDevProps);

                           if((Result == 0) || (Result == BTPM_ERROR_CODE_PROPERTY_UPDATE_IN_PROGRESS))
                           {
                              /* The request was accepted, so note that */
                              /* we are going to wait before fulfilling */
                              /* the authentication request.            */
                              WaitingOnNameRequest = true;
                           }
                           else
                           {
                              /* The name request failed immediately    */
                              /* for some reason. Pull the device back  */
                              /* off the waiting list and go ahead and  */
                              /* service the authentication request.    */
                              SS1_LOGI("Failed to initiate remote %sdevice name request prior to authentication (%d)", ((AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) ? "LE " : ""), Result);

                              RemoveDeviceFromList(&(NatData->NamePendingForAuthDevices), AuthReqInfo->BD_ADDR, (!!(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)), NULL);
                              free(AuthReqInfoCopy);
                           }
                        }
                        else
                        {
                           /* Unable to add the device to the list.     */
                           /* Clean up and continue with the pairing    */
                           /* process.                                  */
                           SS1_LOGE("Error recording authentication request.");

                           free(AuthReqInfoCopy);
                        }
                     }
                     else
                     {
                        /* Unable to allocate space for the copy of the */
                        /* request data.  Note the event and continue   */
                        /* with the pairing process.                    */
                        SS1_LOGE("Out of memory for auth request");
                     }
                  }
               }

               /* If this request was intiated by the remote device, and*/
               /* we don't know the device's SDP records, query them    */
               /* now.                                                  */
               if(AuthReqInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)
               {
                  /* Confirm that the following is all true:            */
                  /*   1. The remote device is connected (rather than   */
                  /*      authenticating pre-connection).               */
                  /*   2. The remote device initiated the connection (PM*/
                  /*      will automatically query services if we       */
                  /*      initiated the link).                          */
                  /*   3. The remote device's services are not yet      */
                  /*      known.                                        */
                  if((RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED_OVER_LE) && (!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_LINK_INITIATED_LOCALLY)) && (!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LE_SERVICES_KNOWN)))
                  {
                     if((Result = DEVM_QueryRemoteDeviceServices(AuthReqInfo->BD_ADDR, (DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_FORCE_UPDATE | DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_LOW_ENERGY), 0, NULL, NULL)) == 0)
                        SS1_LOGI("Began LE Service query against device %02X:%02X:%02X:%02X:%02X:%02X", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
                     else
                     {
                        if(Result == BTPM_ERROR_CODE_SERVICE_DISCOVERY_IN_PROGRESS)
                           SS1_LOGI("LE Service query already in-progress against device %02X:%02X:%02X:%02X:%02X:%02X", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
                        else
                           SS1_LOGI("Failed to initiate LE Service query against device %02X:%02X:%02X:%02X:%02X:%02X (%d)", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0, Result);
                     }
                  }
               }
               else
               {
                  /* Confirm that the following is all true:            */
                  /*   1. The remote device is connected (rather than   */
                  /*      authenticating pre-connection).               */
                  /*   2. The remote device initiated the connection (PM*/
                  /*      will automatically query services if we       */
                  /*      initiated the link).                          */
                  /*   3. The remote device's services are not yet      */
                  /*      known.                                        */
                  if((RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_CONNECTED) && (!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_LINK_INITIATED_LOCALLY)) && (!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SERVICES_KNOWN)))
                  {
                     if((Result = DEVM_QueryRemoteDeviceServices(AuthReqInfo->BD_ADDR, DEVM_QUERY_REMOTE_DEVICE_SERVICES_FLAGS_FORCE_UPDATE, 0, NULL, NULL)) == 0)
                        SS1_LOGI("Began SDP query against device %02X:%02X:%02X:%02X:%02X:%02X", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
                     else
                     {
                        if(Result == BTPM_ERROR_CODE_SERVICE_DISCOVERY_IN_PROGRESS)
                           SS1_LOGI("SDP query already in-progress against device %02X:%02X:%02X:%02X:%02X:%02X", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
                        else
                           SS1_LOGI("Failed to initiate SDP query against device %02X:%02X:%02X:%02X:%02X:%02X (%d)", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0, Result);
                     }
                  }
               }
            }
         }

         if(!WaitingOnNameRequest)
         {
            /* We already have the remote device name or we weren't able*/
            /* to initiate a request for it, so go ahead and handle the */
            /* authentication request.                                  */
            if(DEVM_QueryRemoteDeviceProperties(AuthReqInfo->BD_ADDR, FALSE, &RemoteDevProps) == 0)
            {
               if(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_NAME_KNOWN)
                  SS1_LOGI("Handling incoming auth request from %02X:%02X:%02X:%02X:%02X:%02X, name already known", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
               else
                  SS1_LOGI("Handling incoming auth request from %02X:%02X:%02X:%02X:%02X:%02X, name unknown", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);
            }
            else
               SS1_LOGI("Handling incoming auth request from %02X:%02X:%02X:%02X:%02X:%02X, name state undetermined", AuthReqInfo->BD_ADDR.BD_ADDR5, AuthReqInfo->BD_ADDR.BD_ADDR4, AuthReqInfo->BD_ADDR.BD_ADDR3, AuthReqInfo->BD_ADDR.BD_ADDR2, AuthReqInfo->BD_ADDR.BD_ADDR1, AuthReqInfo->BD_ADDR.BD_ADDR0);

            HandleAuthenticationRequest(Env, AuthReqInfo, NatData);
         }

         /* If we had to attach the thread earlier, detach it now so the*/
         /* JVM doesn't leak any resources.                             */
         if(AttachResult > 0)
            NatData->VM->DetachCurrentThread();
      }
      else
         SS1_LOGE("Could not attach thread to Java VM");
   }
   else
      SS1_LOGE("Callback did not provide a valid parameter");

   SS1_LOGD("Exit");
}


#if (SS1_PLATFORM_SDK_VERSION >= 14)

#if SS1_SUPPORT_HID

void BTEL_HIDMEventCallback(HIDM_Event_Data_t *EventData, void *CallbackParameter)
{
   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   int                                AttachResult;
   char                               StringBuffer[256];
   jint                               ResultCode;
   JNIEnv                            *Env;
   jstring                            PathString;
   jstring                            UuidString;
   jthrowable                         Exception;
   jobjectArray                       PropArray;
   BTEL_NativeData_t                 *NatData;

   *(void **)(&NatData) = CallbackParameter;

   if(NatData)
   {
      /* Attach the thread (if necessary) to the JVM so we can make     */
      /* calls into Java code.                                          */
      if((AttachResult = AttachThreadToJVM(NatData->VM, NatData->JNIEnvVersion, &Env)) >= 0)
      {
         if(EventData)
         {
            switch(EventData->EventType)
            {
               case hetHIDDeviceConnectionRequest:
                  SS1_LOGI("Signal: HIDDeviceConnectionRequest");

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.DeviceConnectionRequestData.RemoteDeviceAddress);

                  if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                  {
                     /* Android expects the UUID for BNEP, rather than  */
                     /* NAP, in this case.                              */
                     if((UuidString = Env->NewStringUTF("00001124-0000-1000-8000-00805f9b34fb")) != NULL)
                     {
                        /* A remote PANU device established a connection*/
                        /* with the local NAP service.                  */
                        Env->CallVoidMethod(NatData->Object, Method_onAgentAuthorize, PathString, UuidString, AUTHORIZATION_PROFILE_HID);

                        Env->DeleteLocalRef(UuidString);
                     }

                     Env->DeleteLocalRef(PathString);
                  }
                  break;

               case hetHIDDeviceConnected:
                  SS1_LOGI("Signal: HIDDeviceConnected");

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress);
                  if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                  {
                     if((PropArray = Env->NewObjectArray(2, Env->FindClass("java/lang/String"), NULL)) != NULL)
                     {
                        SetStringArrayElement(Env, PropArray, 0, "Connected");

                        snprintf(StringBuffer, sizeof(StringBuffer), "true");
                        SetStringArrayElement(Env, PropArray, 1, StringBuffer);

                        Env->CallVoidMethod(NatData->Object, Method_onInputDevicePropertyChanged, PathString, PropArray);

                        Env->DeleteLocalRef(PropArray);
                     }

                     Env->DeleteLocalRef(PathString);
                  }
                  break;

               case hetHIDDeviceConnectionStatus:
                  SS1_LOGI("Signal: HIDDeviceConnectionStatus");

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.DeviceConnectionStatusEventData.RemoteDeviceAddress);
                  if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                  {
/* XXX The following errors are only known to ARBTJBBTS when it calls   */
/* XXX HIDM_Connect_Remote_Device(). This requires implementing a       */
/* XXX callback similar to what Authentication uses to simulate         */
/* XXX asynchronous result codes.                                       */
//XXX INPUT_DISCONNECT_FAILED_NOT_CONNECTED
//XXX INPUT_CONNECT_FAILED_ALREADY_CONNECTED
//XXX INPUT_CONNECT_FAILED_ATTEMPT_FAILED
//XXX INPUT_OPERATION_GENERIC_FAILURE

                     switch(EventData->EventData.DeviceConnectionStatusEventData.ConnectionStatus)
                     {
                        case HIDM_HID_DEVICE_CONNECTION_STATUS_SUCCESS:
                           ResultCode = INPUT_OPERATION_SUCCESS;
                           break;
                        case HIDM_HID_DEVICE_CONNECTION_STATUS_FAILURE_TIMEOUT:
                        case HIDM_HID_DEVICE_CONNECTION_STATUS_FAILURE_REFUSED:
                        case HIDM_HID_DEVICE_CONNECTION_STATUS_FAILURE_SECURITY:
                        case HIDM_HID_DEVICE_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                        case HIDM_HID_DEVICE_CONNECTION_STATUS_FAILURE_UNKNOWN:
                           ResultCode = INPUT_CONNECT_FAILED_ATTEMPT_FAILED;
                           break;
                        default:
                           ResultCode = INPUT_OPERATION_GENERIC_FAILURE;
                           break;
                     }

                     Env->CallVoidMethod(NatData->Object, Method_onInputDeviceConnectionResult, PathString, ResultCode);

                     Env->DeleteLocalRef(PathString);
                  }
                  break;

               case hetHIDDeviceDisconnected:
                  SS1_LOGI("Signal: HIDDeviceDisconnected");

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.DeviceConnectedEventData.RemoteDeviceAddress);
                  if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                  {
                     if((PropArray = Env->NewObjectArray(2, Env->FindClass("java/lang/String"), NULL)) != NULL)
                     {
                        SetStringArrayElement(Env, PropArray, 0, "Connected");

                        snprintf(StringBuffer, sizeof(StringBuffer), "false");
                        SetStringArrayElement(Env, PropArray, 1, StringBuffer);

                        Env->CallVoidMethod(NatData->Object, Method_onInputDevicePropertyChanged, PathString, PropArray);

                        Env->DeleteLocalRef(PropArray);
                     }

                     Env->DeleteLocalRef(PathString);
                  }
                  break;

                  /* The following HID Boot-mode events are not expected*/
                  /* to be delivered to this callback because we have   */
                  /* not registered for data events.                    */
               case hetHIDBootKeyboardKeyPress:
                  SS1_LOGI("Signal: HIDBootKeyboardKeyPress");
                  break;

               case hetHIDBootKeyboardKeyRepeat:
                  SS1_LOGI("Signal: HIDBootKeyboardKeyRepeat");
                  break;

               case hetHIDBootMouseEvent:
                  SS1_LOGI("Signal: HIDBootMouseEvent");
                  break;

               case hetHIDReportDataReceived:
                  SS1_LOGI("Signal: HIDReportDataReceived");
                  break;

               default:
                  SS1_LOGE("Received unknown event code %d", EventData->EventType);
            }

            if(Env->ExceptionCheck())
            {
               SS1_LOGW("Triggered exception:");
               Env->ExceptionDescribe();
               Env->ExceptionClear();
               SS1_LOGW("Exception cleared.");
            }
         }
         else
            SS1_LOGE("Callback did not provide a valid event information structure");

         /* If we had to attach the thread earlier, detach it now so    */
         /* the JVM doesn't leak any resources.                         */
         if(AttachResult > 0)
            NatData->VM->DetachCurrentThread();
      }
      else
         SS1_LOGE("Could not attach thread to Java VM");
   }
   else
      SS1_LOGE("Callback did not provide a valid parameter");

   SS1_LOGD("Exit");
}

#endif /* SS1_SUPPORT_HID */

#if SS1_SUPPORT_PAN

void BTEL_PANMEventCallback(PANM_Event_Data_t *EventData, void *CallbackParameter)
{
   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   int                                AttachResult;
   char                               StringBuffer[256];
   jint                               ResultCode;
   JNIEnv                            *Env;
   jstring                            InterfaceName;
   jstring                            Address;
   jstring                            PathString;
   jstring                            UuidString;
   jthrowable                         Exception;
   jobjectArray                       PropArray;
   BTEL_NativeData_t                 *NatData;

   *(void **)(&NatData) = CallbackParameter;

   if(NatData)
   {
      /* Attach the thread (if necessary) to the JVM so we can make     */
      /* calls into Java code.                                          */
      if((AttachResult = AttachThreadToJVM(NatData->VM, NatData->JNIEnvVersion, &Env)) >= 0)
      {
         if(EventData)
         {
            switch(EventData->EventType)
            {
               case petPANMIncomingConnectionRequest:
                  SS1_LOGI("Signal: PANMIncomingConnectionRequest");

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.IncomingConnectionReqeustEventData.RemoteDeviceAddress);

                  if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                  {
                     if((UuidString = Env->NewStringUTF("0000000f-0000-1000-8000-00805F9B34FB")) != NULL)
                     {
                        /* A remote PANU device established a connection*/
                        /* with the local NAP service.                  */
                        Env->CallVoidMethod(NatData->Object, Method_onAgentAuthorize, PathString, UuidString, AUTHORIZATION_PROFILE_PAN);

                        Env->DeleteLocalRef(UuidString);
                     }

                     Env->DeleteLocalRef(PathString);
                  }
                  break;

               case petPANMConnected:
                  SS1_LOGI("Signal: PANMConnected");

                  BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), EventData->EventData.ConnectedEventData.RemoteDeviceAddress);

                  if(EventData->EventData.ConnectedEventData.ServiceType == pstNetworkAccessPoint)
                  {
                     /* A remote PANU device established a connection   */
                     /* with the local NAP service.                     */
                     if((Address = Env->NewStringUTF(StringBuffer)) != NULL)
                     {
                        /* XXX We currently only support a single VNET  */
                        /* XXX interface.                               */
                        if((InterfaceName = Env->NewStringUTF("ss1vnet0")) != NULL)
                        {
                           /* Notify the upper layers. The final        */
                           /* parameter (DestUuid), provides the 16-bit */
                           /* UUID of the local service receiving the   */
                           /* connection, but is ignored by the upper   */
                           /* layer because Android only supports acting*/
                           /* as a NAP server.                          */
                           Env->CallVoidMethod(NatData->Object, Method_onNetworkDeviceConnected, Address, InterfaceName, 0);
                           Env->DeleteLocalRef(InterfaceName);
                        }

                        Env->DeleteLocalRef(Address);
                     }
                  }
                  else
                  {
                     /* We only operate a NAP service, so no other      */
                     /* connection type is expected to be received.     */
                     SS1_LOGD("Unexpected PAN connection: %s, Type %d", StringBuffer, EventData->EventData.ConnectedEventData.ServiceType);
                  }
                  break;

               case petPANMDisconnected:
                  SS1_LOGI("Signal: PANMDisconnected");

                  if(EventData->EventData.DisconnectedEventData.ServiceType == pstPersonalAreaNetworkUser)
                  {
                     /* A remote PANU device closed or lost its         */
                     /* connection to the local NAP service.            */
                     BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), EventData->EventData.DisconnectedEventData.RemoteDeviceAddress);

                     if((Address = Env->NewStringUTF(StringBuffer)) != NULL)
                     {
                        Env->CallVoidMethod(NatData->Object, Method_onNetworkDeviceDisconnected, Address);
                        Env->DeleteLocalRef(Address);
                     }
                  }
                  else
                  {
                     if(EventData->EventData.DisconnectedEventData.ServiceType == pstNetworkAccessPoint)
                     {
                        /* The local device disconnected from a remote  */
                        /* NAP.                                         */
                        BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.DisconnectedEventData.RemoteDeviceAddress);

                        if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                        {
                           if((PropArray = Env->NewObjectArray(2, Env->FindClass("java/lang/String"), NULL)) != NULL)
                           {
                              SetStringArrayElement(Env, PropArray, 0, "Connected");

                              snprintf(StringBuffer, sizeof(StringBuffer), "false");
                              SetStringArrayElement(Env, PropArray, 1, StringBuffer);

                              Env->CallVoidMethod(NatData->Object, Method_onPanDevicePropertyChanged, PathString, PropArray);

                              Env->DeleteLocalRef(PropArray);
                           }

                           Env->DeleteLocalRef(PathString);
                        }
                     }
                     else
                     {
                        /* We do not operate a GW service, so no other  */
                        /* disconnection type is expected.              */
                        BD_ADDRToStr(StringBuffer, sizeof(StringBuffer), EventData->EventData.DisconnectedEventData.RemoteDeviceAddress);
                        SS1_LOGD("Unexpected PAN disconnection: Address %s, Type %d", StringBuffer, EventData->EventData.DisconnectedEventData.ServiceType);
                     }
                  }
                  break;

               case petPANMConnectionStatus:
                  SS1_LOGI("Signal: PANMConnectionStatus");

                  BD_ADDRToPath(StringBuffer, sizeof(StringBuffer), EventData->EventData.ConnectionStatusEventData.RemoteDeviceAddress);
                  if((PathString = Env->NewStringUTF(StringBuffer)) != NULL)
                  {
                     SS1_LOGI("Connection status: %d", EventData->EventData.ConnectionStatusEventData.Status);

                     /* Map the result to the nearest state of:         */
                     /*    - PAN_DISCONNECT_FAILED_NOT_CONNECTED        */
                     /*    - PAN_CONNECT_FAILED_ALREADY_CONNECTED       */
                     /*    - PAN_CONNECT_FAILED_ATTEMPT_FAILED          */
                     /*    - PAN_OPERATION_GENERIC_FAILURE              */
                     /*    - PAN_OPERATION_SUCCESS                      */
                     if(EventData->EventData.ConnectionStatusEventData.Status == PANM_DEVICE_CONNECTION_STATUS_SUCCESS)
                        ResultCode = PAN_OPERATION_SUCCESS;
                     else
                        ResultCode = PAN_CONNECT_FAILED_ATTEMPT_FAILED;

                     Env->CallVoidMethod(NatData->Object, Method_onPanDeviceConnectionResult, PathString, ResultCode);

                     if(Env->ExceptionCheck() == JNI_FALSE)
                     {
                        if(EventData->EventData.ConnectionStatusEventData.Status == PANM_DEVICE_CONNECTION_STATUS_SUCCESS)
                        {
                           if((PropArray = Env->NewObjectArray(2, Env->FindClass("java/lang/String"), NULL)) != NULL)
                           {
                              SetStringArrayElement(Env, PropArray, 0, "Connected");

                              snprintf(StringBuffer, sizeof(StringBuffer), "true");
                              SetStringArrayElement(Env, PropArray, 1, StringBuffer);

                              Env->CallVoidMethod(NatData->Object, Method_onPanDevicePropertyChanged, PathString, PropArray);

                              if(Env->ExceptionCheck() == JNI_FALSE)
                              {
                                 SetStringArrayElement(Env, PropArray, 0, "Interface");
                                 SetStringArrayElement(Env, PropArray, 1, "ss1vnet0");

                                 Env->CallVoidMethod(NatData->Object, Method_onPanDevicePropertyChanged, PathString, PropArray);

                                 if(Env->ExceptionCheck() == JNI_TRUE)
                                    SS1_LOGE("Exception thrown by BluetoothEventLoop.onPanDevicePropertyChanged()");
                              }
                              else
                                 SS1_LOGE("Exception thrown by BluetoothEventLoop.onPanDevicePropertyChanged()");

                              Env->DeleteLocalRef(PropArray);
                           }
                        }
                     }
                     else
                        SS1_LOGE("Exception thrown by BluetoothEventLoop.onPanDeviceConnectionResult()");

                     Env->DeleteLocalRef(PathString);
                  }
                  break;

               default:
                  SS1_LOGE("Received unknown event code %d", EventData->EventType);
            }

            if(Env->ExceptionCheck())
            {
               SS1_LOGW("Triggered exception:");
               Env->ExceptionDescribe();
               Env->ExceptionClear();
               SS1_LOGW("Exception cleared.");
            }
         }
         else
            SS1_LOGE("Callback did not provide a valid event information structure");

         /* If we had to attach the thread earlier, detach it now so    */
         /* the JVM doesn't leak any resources.                         */
         if(AttachResult > 0)
            NatData->VM->DetachCurrentThread();
      }
      else
         SS1_LOGE("Could not attach thread to Java VM");
   }
   else
      SS1_LOGE("Callback did not provide a valid parameter");

   SS1_LOGD("Exit");
}

#endif /* SS1_SUPPORT_PAN */

#endif /* SS1_PLATFORM_SDK_VERSION >= 14 */

   /* The following callback function is called by the Bluetopia client */
   /* library when communications to the server are lost.               */
void BTEL_IPCLostCallback(void *CallbackParameter)
{
   SS1_LOGD("Enter (%p)", CallbackParameter);

   int                AttachResult;
   char               StringBuffer[256];
   jsize              PropPosition;
   JNIEnv            *Env;
   jstring            Path;
   jthrowable         Exception;
   BTEL_NativeData_t *NatData;

   *(void **)(&NatData) = CallbackParameter;

   /* The A2DP Service runs in the same process but doesn't receive this*/
   /* callback. Inform the service that Bluetooth is gone so it can     */
   /* prepare for a restart.                                            */
   BTAS_SignalBluetoothDisabled();

   if(NatData)
   {
      /* Attach the thread (if necessary) to the JVM so we can make     */
      /* calls into Java code.                                          */
      if((AttachResult = AttachThreadToJVM(NatData->VM, NatData->JNIEnvVersion, &Env)) >= 0)
      {
         /* Handle the actual "server connection lost" problem. When    */
         /* BlueZ starts up, and the device is already powered, it      */
         /* sends a PropertyChanged signal with the "Powered" key       */
         /* set to true. Since Android is normally not registered to    */
         /* receive DBus messages from BlueZ until after the BlueZ      */
         /* daemon is running, it only sees the "Powered" property      */
         /* change when BlueZ has crashed and the init process has      */
         /* restarted the daemon. Part of handling the "Powered"        */
         /* property change is fully shutting down and restarting       */
         /* the Bluetooth subsystem, which has the side-effect of       */
         /* explicitly restarting the daemon. Taking advantage of this  */
         /* behavior is the easiest way of dealing with a loss of IPC   */
         /* communications with the server because it puts everything   */
         /* all the components in a known state.                        */
         HandlePropertyChanged(Env, NatData->Object, "Powered", "true");

         CleanupDeviceLists(NatData);

         /* If we had to attach the thread earlier, detach it now so    */
         /* the JVM doesn't leak any resources.                         */
         if(AttachResult > 0)
            NatData->VM->DetachCurrentThread();
      }
      else
         SS1_LOGE("Could not attach thread to Java VM");
   }
   else
      SS1_LOGE("Callback did not provide a valid parameter");

   SS1_LOGD("Exit");
}


BTEL_NativeData_t *BTEL_GetNativeData(JNIEnv *Env, jobject Object)
{
   return((BTEL_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData)));
}

void BTEL_CallOnPropertyChanged(JNIEnv *Env, jobject BTELObject, jobjectArray PropValues)
{
   Env->CallVoidMethod(BTELObject, Method_onPropertyChanged, PropValues);
}

void BTEL_CallOnCreateDeviceResult(JNIEnv *Env, jobject BTELObject, jstring Address, jint Result)
{
   Env->CallVoidMethod(BTELObject, Method_onCreateDeviceResult, Address, Result);
}


void BTEL_CallOnCreatePairedDeviceResult(JNIEnv *Env, jobject BTELObject, jstring Address, jint Result)
{
   Env->CallVoidMethod(BTELObject, Method_onCreatePairedDeviceResult, Address, Result);
}


bool BTEL_AddPendingPairDevice(BTEL_NativeData_t *NatData, BD_ADDR_t BD_ADDR, Boolean_t LowEnergy)
{
   bool         ret_val;
   char         AddressString[32];
   unsigned int TimerID;
   unsigned int OldTimerID;

   ret_val = false;

   if(NatData)
   {
      memset(AddressString, 0, sizeof(AddressString));
      BD_ADDRToStr(AddressString, sizeof(AddressString), BD_ADDR);

      OldTimerID = 0;

      /* First, check if the device is already in the list. If so,      */
      /* remove it.                                                     */
      if(RemoveDeviceFromList(&(NatData->PairResponsePendingDevices), BD_ADDR, LowEnergy, (void **)&OldTimerID))
      {
         /* The device was already present in the list, so clean up     */
         /* the old timer. We'll add the device back with a new timer,  */
         /* effectively effectively resetting the clock.                */
         TMR_StopTimer(OldTimerID);
      }

      /* Create a new timer for this device.                            */
      if((TimerID = TMR_StartTimer(NatData, PendingPairTimeoutCallback, PAIR_REPLY_TIMEOUT_MS)) > 0)
      {
         /* The timer is running now, so add the device to the state    */
         /* list.                                                       */
         if(AddDeviceToList(&(NatData->PairResponsePendingDevices), BD_ADDR, LowEnergy, (void *)TimerID) == ADD_DEVICE_SUCCESS)
         {
            if(OldTimerID)
               SS1_LOGI("Device already in PairResponsePending list (%s). Timer reset (%d)", AddressString, TimerID);
            else
               SS1_LOGI("Device added to PairResponsePending list (%s) with timer (%d)", AddressString, TimerID);

            ret_val = true;
         }
         else
         {
            /* We couldn't add the device to the status list, so we need*/
            /* to tear everything back down.                            */
            SS1_LOGE("Could not add device to PairResponsePendingDevices list (%s)", AddressString);
            TMR_StopTimer(TimerID);
         }
      }
      else
         SS1_LOGE("Could not start auth timer for device (%s)", AddressString);
   }
   else
      SS1_LOGE("invalid parameter");

   return(ret_val);
}


void BTEL_CallOnInputDeviceConnectionResult(JNIEnv *Env, jobject BTELObject, jstring Path, jint Result)
{
#if (SS1_PLATFORM_SDK_VERSION >= 14)

   Env->CallVoidMethod(BTELObject, Method_onInputDeviceConnectionResult, Path, Result);

#endif
}

void BTEL_CallOnPanDeviceConnectionResult(JNIEnv *Env, jobject BTELObject, jstring Path, jint Result)
{
#if (SS1_PLATFORM_SDK_VERSION >= 14)

   Env->CallVoidMethod(BTELObject, Method_onPanDeviceConnectionResult, Path, Result);

#endif
}

#endif /* HAVE_BLUETOOTH */

} /* namespace android */
