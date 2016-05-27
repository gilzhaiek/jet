/*****< ARBTJBBTAS.cpp >*******************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTAS - android.server.BluetoothA2dpService module for Stonestreet   */
/*               One Android Runtime Bluetooth JNI Bridge                     */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   06/30/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "ARBTJBBTAS"

#include "android_runtime/AndroidRuntime.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <ctype.h>
#include <errno.h>
#include <pthread.h>
//#include <stdio.h>
//#include <string.h>
//#include <stdlib.h>
#include <time.h>
//#include <unistd.h>

#include "SS1UTIL.h"

#ifdef HAVE_BLUETOOTH

extern "C"
{
#include "SS1BTPM.h"

#if SS1_SUPPORT_A2DP
#include "SS1BTAUDM.h"
#endif
}

#include "ARBTJBBTAS.h"
#include "ARBTJBCOM.h"

#endif /* HAVE_BLUETOOTH */

namespace android
{

#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))

#define CONNECT_FLAGS (AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_AUTHENTICATION | AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_ENCRYPTION)

static jclass    Class_String;
static jmethodID Method_onSinkPropertyChanged;
#if SS1_PLATFORM_SDK_VERSION >= 8
static jmethodID Method_onConnectSinkResult;

#endif

typedef enum _tagBTAS_State_t
{
   btasStateDisconnected,
   btasStateConnecting,
   btasStateConnected,
   btasStatePlaying
} BTAS_State_t;

static JavaVM      *VM            = NULL;
static jobject      GlobalObject;
static jint         JNIEnvVersion;

static BD_ADDR_t    LastSinkAddress;
static unsigned int AUDMCallbackID;

static pthread_mutex_t BTASMutex        = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  RegistrationCond = PTHREAD_COND_INITIALIZER;

static jobjectArray BuildProperty(JNIEnv *Env, const char *Key, const char *Value);
static void BTAS_AUDMEventCallback(AUDM_Event_Data_t *EventData, void *CallbackParameter);
//static void *DelayedRegistrationThread(void *NativeData);
//static int WaitForRegistration();

#endif /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */


static jboolean BTAS_InitNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   int                Result;
   jboolean           ret_val;
   pthread_t          RegistrationThread;
   pthread_attr_t     ThreadAttr;

   ret_val = JNI_FALSE;

   Class_String = (jclass)(Env->NewGlobalRef(Env->FindClass("java/lang/String")));

   /* Don't need to initialize IPC since we run in the same process as  */
   /* BluetoothService, which takes care of this for us.                */
   if(VM == NULL)
   {
      Env->GetJavaVM(&VM);
      GlobalObject  = Env->NewGlobalRef(Object);
      JNIEnvVersion = Env->GetVersion();

      AUDMCallbackID = 0;

      ret_val = JNI_TRUE;

      if((Result = AUDM_Register_Event_Callback(BTAS_AUDMEventCallback, NULL)) > 0)
      {
         AUDMCallbackID = (unsigned int)Result;
      }
   }
   else
      SS1_LOGE("BluetoothA2dpService initialized twice");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%d)", JNI_TRUE);
   return(JNI_TRUE);
#endif
}

static void BTAS_CleanupNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   BTAS_SignalBluetoothDisabled();
#endif
   SS1_LOGD("Exit");
}

// Should return array of properties:
//    "State"
//    "disconnected" || "connecting" || "connected" || "playing"
//    "Connected"
//    "true" || "false"
//    "Playing"
//    "true" || "false"
//
//    State should progress as
//       disconnected -> connecting -> connected -> playing
//                 ^------/             |     ^------/ |
//                 \--------------------+--------------/
static jobjectArray BTAS_GetSinkPropertiesNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))

   int                ConnectionStatus;
   jstring            PropKeyString;
   jstring            PropValueString;
   BD_ADDR_t          QueryAddress;
   BD_ADDR_t          ConnectedAddress;
   const char        *StateName;
   const char        *PathString;
   jobjectArray       PropList;
   jobjectArray       SingleProp;
   AUD_Stream_State_t StreamState;

   PropList = NULL;

   if(Path)
   {
      if(pthread_mutex_lock(&BTASMutex) == 0)
      {
         if(AUDMCallbackID > 0)
         {
            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);

            if((PathString = Env->GetStringUTFChars(Path, NULL)) != NULL)
            {
               /* DEBUG */ SS1_LOGD("Getting A2DP SRC properties for %s", PathString);
               PathToBD_ADDR(&QueryAddress, PathString);
               Env->ReleaseStringUTFChars(Path, PathString);

               if((ConnectionStatus = AUDM_Query_Audio_Stream_Connected(astSRC, &ConnectedAddress)) >= 0)
               {
                  if(ConnectionStatus == AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTED)
                  {
                     if(AUDM_Query_Audio_Stream_State(astSRC, &StreamState) != 0)
                        StreamState = astStreamStopped;
                  }
                  else
                     StreamState = astStreamStopped;

                  if(StreamState == astStreamStarted)
                     StateName = "playing";
                  else
                  {
                     switch(ConnectionStatus)
                     {
                        case AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTING:
                           StateName = "connecting";
                           break;
                        case AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTED:
                           StateName = "connected";
                           break;
                        case AUDM_AUDIO_STREAM_CONNECTED_STATE_DISCONNECTED:
                        default:
                           StateName = "disconnected";
                           break;
                     }
                  }

                  if((PropList = Env->NewObjectArray(6, Class_String, NULL)) != NULL)
                  {
                     if((PropKeyString = Env->NewStringUTF("State")) != NULL)
                     {
                        if((PropValueString = Env->NewStringUTF(StateName)) != NULL)
                        {
                           Env->SetObjectArrayElement(PropList, 0, PropKeyString);
                           Env->SetObjectArrayElement(PropList, 1, PropValueString);

                           Env->DeleteLocalRef(PropKeyString);
                           Env->DeleteLocalRef(PropValueString);
                        }
                     }

                     if((PropKeyString = Env->NewStringUTF("Connected")) != NULL)
                     {
                        if((PropValueString = Env->NewStringUTF(((ConnectionStatus == AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTED) ? "true" : "false"))) != NULL)
                        {
                           Env->SetObjectArrayElement(PropList, 2, PropKeyString);
                           Env->SetObjectArrayElement(PropList, 3, PropValueString);

                           Env->DeleteLocalRef(PropKeyString);
                           Env->DeleteLocalRef(PropValueString);
                        }
                     }

                     if((PropKeyString = Env->NewStringUTF("Playing")) != NULL)
                     {
                        if((PropValueString = Env->NewStringUTF(((StreamState == astStreamStarted) ? "true" : "false"))) != NULL)
                        {
                           Env->SetObjectArrayElement(PropList, 4, PropKeyString);
                           Env->SetObjectArrayElement(PropList, 5, PropValueString);

                           Env->DeleteLocalRef(PropKeyString);
                           Env->DeleteLocalRef(PropValueString);
                        }
                     }

                     /* DEBUG */
                     SS1_LOGE("Sink Property Array:");
                     DebugDumpStringArray(Env, PropList, __FUNCTION__);
                  }
                  else
                     SS1_LOGE("Out of memory");
               }
               else
                  SS1_LOGE("Unable to query connection status");
            }
            else
               SS1_LOGE("Out of memory");
         }
         else
         {
            SS1_LOGE("Service not initialized");

            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);
         }
      }
      else
         SS1_LOGE("Unable to acquire lock");
   }
   else
      SS1_LOGE("Invalid parameters");

   CHECK_MUTEX(&BTASMutex);

   SS1_LOGD("Exit (%p)", PropList);

   return(PropList);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%p)", NULL);
   return(NULL);
#endif
}


// Starts asynchronous A2DP connection to remote sink at "Path" but without any callback registered.
// Returns JNI_TRUE if message is sent. Response comes back by watching for PropertyChanged message
// and calling onSinkPropertyChanged() with new state.
static jboolean BTAS_ConnectSinkNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   int          Result;
   bool         NeedUnlock;
   char         AddressBuffer[32];
   jboolean     ret_val;
   BD_ADDR_t    RemoteAddress;
   BD_ADDR_t    SinkAddress;
   const char  *PathString;
   jobjectArray PropArray;

   ret_val = JNI_FALSE;

   if(Path)
   {
      if(pthread_mutex_lock(&BTASMutex) == 0)
      {
         NeedUnlock = true;

         if(AUDMCallbackID > 0)
         {
            if((PathString = Env->GetStringUTFChars(Path, NULL)) != NULL)
            {
               PathToBD_ADDR(&SinkAddress, PathString);
               Env->ReleaseStringUTFChars(Path, PathString);

               BD_ADDRToStr(AddressBuffer, sizeof(AddressBuffer), SinkAddress);
               SS1_LOGI("Initiating A2DP connection to %s", AddressBuffer);

               Result = AUDM_Connect_Audio_Stream(SinkAddress, astSRC, CONNECT_FLAGS, BTAS_AUDMEventCallback, NULL, NULL);
               if(Result == BTPM_ERROR_CODE_AUDIO_STREAM_ALREADY_CONNECTED)
               {
                  SS1_LOGW("Stream already connected.");
               }
               else
               {
                  if(Result == BTPM_ERROR_CODE_AUDIO_STREAM_CONNECTION_IN_PROGRESS)
                     SS1_LOGW("Stream connection process already in progress.");
               }

               if(Result == 0)
               {
                  SS1_LOGD("Connect request sent successfully (%d)", Result);
                  LastSinkAddress = SinkAddress;
                  ret_val = JNI_TRUE;

                  if((PropArray = BuildProperty(Env, "State", "connecting")) != NULL)
                  {
                     pthread_mutex_unlock(&BTASMutex);
                     NeedUnlock = false;

                     /* Call upstream to announce that we're connecting.*/
                     Env->CallVoidMethod(Object, Method_onSinkPropertyChanged, Path, PropArray);
                     Env->DeleteLocalRef(PropArray);
                  }
               }
               else
               {
                  if(Result == BTPM_ERROR_CODE_AUDIO_STREAM_CONNECTION_IN_PROGRESS)
                  {
                     /* We're already in the process of connecting to a */
                     /* device. First, determine whether it's the same  */
                     /* device as is being requested.                   */
                     memset(&RemoteAddress, 0, sizeof(RemoteAddress));
                     if((Result = AUDM_Query_Audio_Stream_Connected(astSRC, &RemoteAddress)) >= 0)
                     {
                        switch(Result)
                        {
                           case AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTING:
                              if(COMPARE_BD_ADDR(SinkAddress, RemoteAddress))
                              {
                                 /* We are actively connecting to the   */
                                 /* requested device. Log this fact and */
                                 /* report success.                     */
                                 SS1_LOGD("Connection already in progress. Reporting as if we successfully sent the request (%d)", Result);
                                 LastSinkAddress = SinkAddress;
                                 ret_val = JNI_TRUE;

                                 if((PropArray = BuildProperty(Env, "State", "connecting")) != NULL)
                                 {
                                    pthread_mutex_unlock(&BTASMutex);
                                    NeedUnlock = false;

                                    /* Call upstream to announce that   */
                                    /* we're connecting.                */
                                    Env->CallVoidMethod(Object, Method_onSinkPropertyChanged, Path, PropArray);
                                    Env->DeleteLocalRef(PropArray);
                                 }
                              }
                              else
                              {
                                 /* We are actively connecting to a     */
                                 /* different device. only support one  */
                                 /* A2DP connection at a time, so report*/
                                 /* failure.                            */
                                 BD_ADDRToStr(AddressBuffer, sizeof(AddressBuffer), RemoteAddress);
                                 SS1_LOGD("Already existing A2DP connection to another device (%s)", AddressBuffer);
                                 ret_val = JNI_FALSE;
                              }
                              break;

                           case AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTED:
                              BD_ADDRToStr(AddressBuffer, sizeof(AddressBuffer), RemoteAddress);
                              SS1_LOGD("INTERNAL_ERROR: Connection request reports currently connecting, but connection status query reports already connected (%s)", AddressBuffer);
                              ret_val = JNI_FALSE;
                              break;

                           case AUDM_AUDIO_STREAM_CONNECTED_STATE_DISCONNECTED:
                              SS1_LOGD("INTERNAL ERROR: Connection request reports currently connecting, but connection status query reports disconnected");
                              ret_val = JNI_FALSE;
                              break;

                           default:
                              SS1_LOGD("INTERNAL ERROR: Unknown result from connection status query (%d)", Result);
                              ret_val = JNI_FALSE;
                        }
                     }
                     else
                        SS1_LOGE("A2DP connection appears to be in the process of connecting, but unable to determine remote device (%d)", Result);
                  }
                  else
                  {
                     if(Result == BTPM_ERROR_CODE_AUDIO_STREAM_ALREADY_CONNECTED)
                     {
                        /* We're already connected. Since we only       */
                        /* support one connection at a time, check      */
                        /* whether we're connected to the endpoint that */
                        /* was just requested or to some other device.  */
                        memset(&RemoteAddress, 0, sizeof(RemoteAddress));
                        if((Result = AUDM_Query_Audio_Stream_Connected(astSRC, &RemoteAddress)) >= 0)
                        {
                           switch(Result)
                           {
                              case AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTED:
                                 if(COMPARE_BD_ADDR(SinkAddress, RemoteAddress))
                                 {
                                    /* We are already connected to the  */
                                    /* requested device. Log this fact  */
                                    /* and report success.              */
                                    SS1_LOGD("A2DP connection already exists to this device");
                                    ret_val = JNI_TRUE;

#if SS1_PLATFORM_SDK_VERSION <= 7
                                    if((PropArray = BuildProperty(Env, "State", "connected")) != NULL)
                                    {
                                       pthread_mutex_unlock(&BTASMutex);
                                       NeedUnlock = false;

                                       /* We're already connected to the*/
                                       /* requested device, so make sure*/
                                       /* upstream knows this.          */
                                       Env->CallVoidMethod(Object, Method_onSinkPropertyChanged, Path, PropArray);
                                       Env->DeleteLocalRef(PropArray);
                                    }
#else /* SS1_PLATFORM_SDK_VERSION >= 8 */
                                    Env->CallVoidMethod(GlobalObject, Method_onConnectSinkResult, Path, JNI_TRUE);
#endif
                                 }
                                 else
                                 {
                                    /* We are currently connected to a  */
                                    /* different device. only support   */
                                    /* one A2DP connection at a time, so*/
                                    /* report failure.                  */
                                    BD_ADDRToStr(AddressBuffer, sizeof(AddressBuffer), RemoteAddress);
                                    SS1_LOGD("Already existing A2DP connection to another device (%s)", AddressBuffer);
                                    ret_val = JNI_FALSE;
                                 }
                                 break;

                              case AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTING:
                                 BD_ADDRToStr(AddressBuffer, sizeof(AddressBuffer), RemoteAddress);
                                 SS1_LOGD("INTERNAL_ERROR: Connection request reports already connected, but connection status query reports currently connecting (%s)", AddressBuffer);
                                 ret_val = JNI_FALSE;
                                 break;

                              case AUDM_AUDIO_STREAM_CONNECTED_STATE_DISCONNECTED:
                                 SS1_LOGD("INTERNAL ERROR: Connection request reports already connected, but connection status query reports disconnected");
                                 ret_val = JNI_FALSE;
                                 break;

                              default:
                                 SS1_LOGD("INTERNAL ERROR: Unknown result from connection status query (%d)", Result);
                                 ret_val = JNI_FALSE;
                           }
                        }
                        else
                           SS1_LOGE("A2DP connection appears to already exist, but unable to determine remote device (%d)", Result);
                     }
                     else
                        SS1_LOGE("Unable to send A2DP connect request (%d)", Result);
                  }
               }
            }
            else
               SS1_LOGE("Out of memory");
         }
         else
            SS1_LOGE("Service not initialized");

         if(NeedUnlock)
            pthread_mutex_unlock(&BTASMutex);
      }
      else
         SS1_LOGE("Unable to acquire lock");
   }
   else
      SS1_LOGE("Invalid parameters");

   CHECK_MUTEX(&BTASMutex);

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

// Makes async "disconnect" call for currently connected sink at "Path". Does not expect callback.
// Returns JNI_TRUE if message is sent. Response comes back by watching for PropertyChanged message
// and calling onSinkPropertyChanged() with new state.
static jboolean BTAS_DisconnectSinkNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   int          Result;
   jboolean     ret_val;
   BD_ADDR_t    SinkAddress;
   jobjectArray PropArray;

   ret_val = JNI_FALSE;

   if(Path)
   {
      if(pthread_mutex_lock(&BTASMutex) == 0)
      {
         if(AUDMCallbackID > 0)
         {
            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);

            if((Result = AUDM_Disconnect_Audio_Stream(astSRC)) == 0)
               ret_val = JNI_TRUE;
            else
            {
               if(Result == BTPM_ERROR_CODE_AUDIO_STREAM_NOT_CONNECTED)
               {
                  /* This sink is not currently connected. Treat this   */
                  /* like a successful disconnection, including the     */
                  /* property-changed notification.                     */
                  ret_val = JNI_TRUE;

                  if((PropArray = BuildProperty(Env, "State", "disconnected")) != NULL)
                  {
                     /* DEBUG */
                     {
                        const char *PathString;
                        PathString = Env->GetStringUTFChars(Path, NULL);
                        SS1_LOGD("Attempted to disconnect a not-connected sink. Faking a successful disconnection for %s.", (PathString != NULL ? PathString: "-NULL-"));
                        if(PathString)
                           Env->ReleaseStringUTFChars(Path, PathString);
                     }

                     Env->CallVoidMethod(GlobalObject, Method_onSinkPropertyChanged, Path, PropArray);
                     Env->DeleteLocalRef(PropArray);
                  }
               }
               else
                  SS1_LOGE("Unable to send A2DP Disconnect request (%d)", Result);
            }
         }
         else
         {
            SS1_LOGE("Service not initialized");

            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);
         }
      }
      else
         SS1_LOGE("Unable to acquire lock");
   }
   else
      SS1_LOGE("Invalid parameters");

   CHECK_MUTEX(&BTASMutex);

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

// Makes async "suspend" call for currently playing sink at "Path". Does not expect callback.
// Returns JNI_TRUE if message is sent. Response comes back by watching for PropertyChanged message
// and calling onSinkPropertyChanged() with new state.
static jboolean BTAS_SuspendSinkNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   int       Result;
   jboolean  ret_val;
   BD_ADDR_t SinkAddress;

   ret_val = JNI_FALSE;

   if(Path)
   {
      if(pthread_mutex_lock(&BTASMutex) == 0)
      {
         if(AUDMCallbackID > 0)
         {
            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);

            if((Result = AUDM_Change_Audio_Stream_State(astSRC, astStreamStopped)) == 0)
               ret_val = JNI_TRUE;
            else
               SS1_LOGE("Unable to send A2DP Suspend request (%d)", Result);
         }
         else
         {
            SS1_LOGE("Service not initialized");

            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);
         }
      }
      else
         SS1_LOGE("Unable to acquire lock");
   }
   else
      SS1_LOGE("Invalid parameters");

   CHECK_MUTEX(&BTASMutex);

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

// Makes async "resume" call for currently suspended sink at "Path". Does not expect callback.
// Returns JNI_TRUE if message is sent. Response comes back by watching for PropertyChanged message
// and calling onSinkPropertyChanged() with new state.
//
// Does this work for currently "connected" (not playing or suspended) as a "Play" command?
static jboolean BTAS_ResumeSinkNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   int       Result;
   jboolean  ret_val;
   BD_ADDR_t SinkAddress;

   ret_val = JNI_FALSE;

   if(Path)
   {
      if(pthread_mutex_lock(&BTASMutex) == 0)
      {
         if(AUDMCallbackID > 0)
         {
            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);

            if((Result = AUDM_Change_Audio_Stream_State(astSRC, astStreamStarted)) == 0)
               ret_val = JNI_TRUE;
            else
               SS1_LOGE("Unable to send A2DP Resume request (%d)", Result);
         }
         else
         {
            SS1_LOGE("Service not initialized");

            /* Release the BTAS module lock.                            */
            pthread_mutex_unlock(&BTASMutex);
         }
      }
      else
         SS1_LOGE("Unable to acquire lock");
   }
   else
      SS1_LOGE("Invalid parameters");

   CHECK_MUTEX(&BTASMutex);

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jboolean BTAS_AvrcpVolumeUpNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   jboolean ret_val;

   // TODO
   ret_val = JNI_TRUE;

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jboolean BTAS_AvrcpVolumeDownNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
   jboolean ret_val;

   // TODO
   ret_val = JNI_TRUE;

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

#ifdef HAVE_BLUETOOTH

#if SS1_SUPPORT_A2DP

static jobjectArray BuildProperty(JNIEnv *Env, const char *Key, const char *Value)
{
   jstring      PropKeyString;
   jstring      PropValueString;
   jobjectArray PropArray;

   PropArray = NULL;

   if(Env && Key && Value)
   {
      if((PropKeyString = Env->NewStringUTF(Key)) != NULL)
      {
         if((PropValueString = Env->NewStringUTF(Value)) != NULL)
         {
            if((PropArray = Env->NewObjectArray(2, Class_String, NULL)) != NULL)
            {
               Env->SetObjectArrayElement(PropArray, 0, PropKeyString);
               Env->SetObjectArrayElement(PropArray, 1, PropValueString);

               Env->DeleteLocalRef(PropKeyString);
               Env->DeleteLocalRef(PropValueString);
            }
         }
      }
   }
   else
      SS1_LOGE("Invalid parameters");

   if(PropArray == NULL)
      SS1_LOGE("Out of memory");

   return(PropArray);
}

static void BTAS_AUDMEventCallback(AUDM_Event_Data_t *EventData, void *CallbackParameter)
{
   int          AttachResult;
   bool         NeedUnlock;
   char         AddressBuffer[32];
   JNIEnv      *Env;
   jstring      AddressString;
   jobjectArray PropArray;

   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   if(EventData)
   {
      if(pthread_mutex_lock(&BTASMutex) == 0)
      {
         NeedUnlock = true;

         if(AUDMCallbackID > 0)
         {
            /* Attach the thread (if necessary) to the JVM so we can make  */
            /* calls into Java code.                                       */
            if((AttachResult = AttachThreadToJVM(VM, JNIEnvVersion, &Env)) >= 0)
            {
               switch(EventData->EventType)
               {
                  case aetAudioStreamConnected:
                     SS1_LOGD("Signal: AudioStreamConnected");
                     if(EventData->EventData.AudioStreamConnectedEventData.StreamType == astSRC)
                     {
                        LastSinkAddress = EventData->EventData.AudioStreamConnectedEventData.RemoteDeviceAddress;
                        if(!BD_ADDRToPath(AddressBuffer, sizeof(AddressBuffer), LastSinkAddress))
                           strncpy(AddressBuffer, "/dev_00:00:00:00:00:00", sizeof(AddressBuffer));

                        if((AddressString = Env->NewStringUTF(AddressBuffer)) != NULL)
                        {
                           if((PropArray = BuildProperty(Env, "State", "connected")) != NULL)
                           {
                              /* DEBUG */ SS1_LOGD("Sending state for %s:", AddressBuffer);
                              /* DEBUG */
                              DebugDumpStringArray(Env, PropArray, __FUNCTION__);

                              /* Release the BTAS module lock.          */
                              pthread_mutex_unlock(&BTASMutex);
                              NeedUnlock = false;

                              Env->CallVoidMethod(GlobalObject, Method_onSinkPropertyChanged, AddressString, PropArray);
                              Env->DeleteLocalRef(PropArray);
                           }
                           Env->DeleteLocalRef(AddressString);
                        }
                     }
                     break;

                  case aetAudioStreamConnectionStatus:
                     SS1_LOGD("Signal: AudioStreamConnectionStatus");
                     if(EventData->EventData.AudioStreamConnectionStatusEventData.StreamType == astSRC)
                     {
                        if(!BD_ADDRToPath(AddressBuffer, sizeof(AddressBuffer), LastSinkAddress))
                           strncpy(AddressBuffer, "/dev_00:00:00:00:00:00", sizeof(AddressBuffer));

                        if((AddressString = Env->NewStringUTF(AddressBuffer)) != NULL)
                        {
                           if(EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus == AUDM_STREAM_CONNECTION_STATUS_SUCCESS)
                           {
                              SS1_LOGD("A2DP connection to %s succeeded", AddressBuffer);
#if SS1_PLATFORM_SDK_VERSION <= 7
                              PropArray = BuildProperty(Env, "State", "connected");
#endif
                           }
                           else
                           {
                              SS1_LOGE("A2DP connection to %s failed (%u)", AddressBuffer, EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus);
#if SS1_PLATFORM_SDK_VERSION <= 7
                              PropArray = BuildProperty(Env, "State", "disconnected");
#endif
                           }

                           /* Release the BTAS module lock.          */
                           pthread_mutex_unlock(&BTASMutex);
                           NeedUnlock = false;

#if SS1_PLATFORM_SDK_VERSION <= 7
                           if(PropArray != NULL)
                           {
                              /* DEBUG */
                              {
                                 SS1_LOGD("Sending state for %s:", AddressBuffer);
                                 DebugDumpStringArray(Env, PropArray, __FUNCTION__);
                              }

                              Env->CallVoidMethod(GlobalObject, Method_onSinkPropertyChanged, AddressString, PropArray);
                              Env->DeleteLocalRef(PropArray);
                           }
#else /* SS1_PLATFORM_SDK_VERSION >= 8 */
                           /* DEBUG */ SS1_LOGD("Calling onConnectSinkResult via handle %p with result = %s", Method_onConnectSinkResult, ((EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus == AUDM_STREAM_CONNECTION_STATUS_SUCCESS) ? "TRUE" : "FALSE"));
                           Env->CallVoidMethod(GlobalObject, Method_onConnectSinkResult, AddressString, ((EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus == AUDM_STREAM_CONNECTION_STATUS_SUCCESS) ? JNI_TRUE : JNI_FALSE));
#endif
                           Env->DeleteLocalRef(AddressString);
                        }
                     }
                     break;

                  case aetAudioStreamDisconnected:
                     SS1_LOGD("Signal: AudioStreamDisconnected");
                     if(EventData->EventData.AudioStreamDisconnectedEventData.StreamType == astSRC)
                     {
                        if(!BD_ADDRToPath(AddressBuffer, sizeof(AddressBuffer), LastSinkAddress))
                           strncpy(AddressBuffer, "/dev_00:00:00:00:00:00", sizeof(AddressBuffer));
                        if((AddressString = Env->NewStringUTF(AddressBuffer)) != NULL)
                        {
                           if((PropArray = BuildProperty(Env, "State", "disconnected")) != NULL)
                           {
                              /* DEBUG */ SS1_LOGD("Sending state for %s:", AddressBuffer);
                              /* DEBUG */
                              DebugDumpStringArray(Env, PropArray, __FUNCTION__);

                              /* Release the BTAS module lock.          */
                              pthread_mutex_unlock(&BTASMutex);
                              NeedUnlock = false;

                              Env->CallVoidMethod(GlobalObject, Method_onSinkPropertyChanged, AddressString, PropArray);
                              Env->DeleteLocalRef(PropArray);
                           }
                           Env->DeleteLocalRef(AddressString);
                        }
                     }
                     break;

                  case aetAudioStreamStateChanged:
                     SS1_LOGD("Signal: AudioStreamStateChanged");
                     if(EventData->EventData.AudioStreamStateChangedEventData.StreamType == astSRC)
                     {
                        if(!BD_ADDRToPath(AddressBuffer, sizeof(AddressBuffer), LastSinkAddress))
                           strncpy(AddressBuffer, "/dev_00:00:00:00:00:00", sizeof(AddressBuffer));
                        if((AddressString = Env->NewStringUTF(AddressBuffer)) != NULL)
                        {
                           if(EventData->EventData.AudioStreamStateChangedEventData.StreamState == astStreamStopped)
                              PropArray = BuildProperty(Env, "State", "connected");
                           else
                              PropArray = BuildProperty(Env, "State", "playing");

                           if(PropArray)
                           {
                              /* DEBUG */ SS1_LOGD("Sending state for %s:", AddressBuffer);
                              /* DEBUG */
                              DebugDumpStringArray(Env, PropArray, __FUNCTION__);

                              /* Release the BTAS module lock.          */
                              pthread_mutex_unlock(&BTASMutex);
                              NeedUnlock = false;

                              Env->CallVoidMethod(GlobalObject, Method_onSinkPropertyChanged, AddressString, PropArray);
                              Env->DeleteLocalRef(PropArray);
                           }
                           Env->DeleteLocalRef(AddressString);
                        }
                     }
                     break;

                  case aetChangeAudioStreamStateStatus:
                     SS1_LOGD("Signal: ChangeAudioStreamStateStatus");
                     /* DEBUG */
                     SS1_LOGD("State Changed (Success: %d, State: %d)", EventData->EventData.ChangeAudioStreamStateStatusEventData.Successful, EventData->EventData.ChangeAudioStreamStateStatusEventData.StreamState);
                     break;

                  case aetAudioStreamFormatChanged:
                     SS1_LOGD("Signal: AudioStreamFormatChanged");
                     break;

                  case aetChangeAudioStreamFormatStatus:
                     SS1_LOGD("Signal: ChangeAudioStreamFormatStatus");
                     break;

                  default:
                     SS1_LOGW("Received unknown A2DP event %d", EventData->EventType);
               }

               if(Env->ExceptionCheck())
               {
                  SS1_LOGW("Triggered exception:");
                  Env->ExceptionDescribe();
                  Env->ExceptionClear();
                  SS1_LOGW("Exception cleared.");
               }

               /* If we had to attach the thread earlier, detach it now so */
               /* the JVM doesn't leak any resources.                      */
               if(AttachResult > 0)
                  VM->DetachCurrentThread();
            }
            else
               SS1_LOGE("Could not attach thread to Java VM");
         }
         else
            SS1_LOGE("Service not initialized");


         /* Release the BTAS module lock, if we haven't already.        */
         if(NeedUnlock)
            pthread_mutex_unlock(&BTASMutex);
      }
      else
         SS1_LOGE("Unable to acquire lock");
   }
   else
      SS1_LOGE("Callback did not provide a valid event information structure");

   CHECK_MUTEX(&BTASMutex);

   SS1_LOGD("Exit");
}

static void *BTAS_RegistrationThread(void *ThreadParameter)
{
   int             Result;
   unsigned int    Count;
   struct timespec Delay;

   Delay.tv_sec  = 1;
   Delay.tv_nsec = 0;

   Count = 0;

   if((Result = pthread_mutex_lock(&BTASMutex)) == 0)
   {
      /* Check Result here, first, since it indicates whether we really */
      /* are holding the lock that protects AUDMCallbackID.             */
      while((Result == 0) && (AUDMCallbackID == 0) && (Count < 30))
      {
         Count++;

         if((Result = AUDM_Register_Event_Callback(BTAS_AUDMEventCallback, NULL)) > 0)
         {
            AUDMCallbackID = (unsigned int)Result;
            Result         = 0;

            /* DEBUG */
            SS1_LOGD("A2DP registered successfully (%d)", AUDMCallbackID);
         }
         else
         {
            SS1_LOGE("Unable to register A2DP (%d)", Result);

            /* Release the BTAS module lock while we wait to try again. */
            pthread_mutex_unlock(&BTASMutex);
            nanosleep(&Delay, NULL);
            if((Result = pthread_mutex_lock(&BTASMutex)) != 0)
               SS1_LOGE("Unable to re-acquire lock");
         }
      }

      /* Release the BTAS module lock.                               */
      pthread_mutex_unlock(&BTASMutex);
   }
   else
      SS1_LOGE("Unable to acquire lock");

   return(NULL);
}

#endif /* SS1_SUPPORT_A2DP */

void BTAS_SignalBluetoothEnabled()
{
   pthread_t RegistrationThread;

   SS1_LOGD("Enter");

#if SS1_SUPPORT_A2DP

   if(pthread_mutex_lock(&BTASMutex) == 0)
   {
      if(AUDMCallbackID == 0)
      {
         memset(&LastSinkAddress, 0, sizeof(LastSinkAddress));

         if(pthread_create(&RegistrationThread, NULL, BTAS_RegistrationThread, NULL) != 0)
            SS1_LOGE("Unable to begin A2DP registration process");
      }
      else
         SS1_LOGW("Service already registered");

      pthread_mutex_unlock(&BTASMutex);
   }
   else
      SS1_LOGE("Unable to acquire lock");

#endif

   SS1_LOGD("Exit");
}

void BTAS_SignalBluetoothDisabled()
{
   SS1_LOGD("Enter");

#if SS1_SUPPORT_A2DP

   if(pthread_mutex_lock(&BTASMutex) == 0)
   {
      if(AUDMCallbackID)
      {
         AUDM_Un_Register_Event_Callback(AUDMCallbackID);
         AUDMCallbackID = 0;
      }
      else
         SS1_LOGW("Service already unregistered");

      memset(&LastSinkAddress, 0, sizeof(LastSinkAddress));

      pthread_mutex_unlock(&BTASMutex);
   }
   else
      SS1_LOGE("Unable to acquire lock");

#endif

   SS1_LOGD("Exit");
}

#endif /* HAVE_BLUETOOTH */

static JNINativeMethod NativeMethods[] =
{
   {"initNative",              "()Z",                                     (void *)BTAS_InitNative},
   {"cleanupNative",           "()V",                                     (void *)BTAS_CleanupNative},

   /* Bluez audio 4.40 API */
   {"connectSinkNative",       "(Ljava/lang/String;)Z",                   (void *)BTAS_ConnectSinkNative},
   {"disconnectSinkNative",    "(Ljava/lang/String;)Z",                   (void *)BTAS_DisconnectSinkNative},
   {"suspendSinkNative",       "(Ljava/lang/String;)Z",                   (void *)BTAS_SuspendSinkNative},
   {"resumeSinkNative",        "(Ljava/lang/String;)Z",                   (void *)BTAS_ResumeSinkNative},
   {"getSinkPropertiesNative", "(Ljava/lang/String;)[Ljava/lang/Object;", (void *)BTAS_GetSinkPropertiesNative},
#if SS1_PLATFORM_SDK_VERSION >= 8
   {"avrcpVolumeUpNative",     "(Ljava/lang/String;)Z",                   (void *)BTAS_AvrcpVolumeUpNative},
   {"avrcpVolumeDownNative",   "(Ljava/lang/String;)Z",                   (void *)BTAS_AvrcpVolumeDownNative},
#endif
};

int SS1API register_android_server_BluetoothA2dpService(JNIEnv *Env)
{
   int ret_val;
   jclass Clazz;
   if((Clazz = Env->FindClass("android/server/BluetoothA2dpService")) != NULL)
   {
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_A2DP))
      Method_onSinkPropertyChanged = Env->GetMethodID(Clazz, "onSinkPropertyChanged", "(Ljava/lang/String;[Ljava/lang/String;)V");
#if SS1_PLATFORM_SDK_VERSION >= 8
      Method_onConnectSinkResult   = Env->GetMethodID(Clazz, "onConnectSinkResult", "(Ljava/lang/String;Z)V");
      if(Method_onConnectSinkResult == NULL)
         SS1_LOGE("Cannot find method onConnectSinkResult");
#endif
#endif /* HAVE_BLUETOOTH && SS1_SUPPORT_A2DP */
      ret_val = AndroidRuntime::registerNativeMethods(Env, "android/server/BluetoothA2dpService", NativeMethods, NELEM(NativeMethods));
   }
   else
      ret_val = -1;

   return(ret_val);
}

} /* namespace android */
