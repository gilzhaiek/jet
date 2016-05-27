/*****< ARBTJBSS.cpp >*********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBSS - android.bluetooth.ScoSocket module for Stonestreet One Android */
/*             Runtime Bluetooth JNI Bridge                                   */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   06/30/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "ARBTJBSS"

#include "android_runtime/AndroidRuntime.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <pthread.h>

#ifdef HAVE_BLUETOOTH

extern "C"
{
#include "SS1BTPM.h"
#include "client/SCOMAPI.h"
}

#include "ARBTJBCOM.h"
#include "ARBTJBBTSO.h"

#endif /* HAVE_BLUETOOTH */

#include "SS1UTIL.h"

namespace android
{
#ifdef HAVE_BLUETOOTH

static jfieldID Field_mNativeData;
static jmethodID Method_onAccepted;
static jmethodID Method_onConnected;
static jmethodID Method_onClosed;

typedef struct _tagSS_NativeData_t
{
   unsigned int    PortHandle;
   BTSO_PortType_t PortType;
   pthread_mutex_t Mutex;

   JavaVM         *VM;
   jobject         Object;
   jint            JNIEnvVersion;
} SS_NativeData_t;

static SS_NativeData_t *GetNativeData(JNIEnv *Env, jobject Object)
{
   SS_NativeData_t *NatData;

   if((NatData = (SS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) == NULL)
      SS1_LOGW("Using uninitialized ScoSocket object");

   return(NatData);
}

static void SS_EventCallback(SCOM_Event_Data_t *EventData, void *CallbackParameter)
{
   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   int              AttachResult;
   JNIEnv          *Env;
   SS_NativeData_t *NatData;

   *(void **)(&NatData) = CallbackParameter;

   if(EventData && NatData)
   {
      if(pthread_mutex_lock(&(NatData->Mutex)) == 0)
      {
         /* Attach the thread (if necessary) to the JVM so we can make  */
         /* calls into Java code.                                       */
         if((AttachResult = AttachThreadToJVM(NatData->VM, NatData->JNIEnvVersion, &Env)) >= 0)
         {
            switch(EventData->EventType)
            {
               case setServerConnectionOpen:
                  SS1_LOGI("Signal: ServerConnectionOpen");
                  if(NatData->PortHandle == EventData->EventData.ServerConnectionOpenEventData.ConnectionID)
                  {
                     /* If the ConnectionID represents a valid          */
                     /* connection handle, return the handle. Otherwise,*/
                     /* return a negative value to indicate an error.   */
                     if(EventData->EventData.ServerConnectionOpenEventData.ConnectionID > 0)
                        Env->CallVoidMethod(NatData->Object, Method_onAccepted, EventData->EventData.ServerConnectionOpenEventData.ConnectionID);
                     else
                        Env->CallVoidMethod(NatData->Object, Method_onAccepted, -1);
                  }
                  else
                     SS1_LOGE("Callback with wrong handle (Event is for %u, state data is for %u)", EventData->EventData.ServerConnectionOpenEventData.ConnectionID, NatData->PortHandle);

                  break;

               case setConnectionClose:
                  SS1_LOGI("Signal: ConnectionOpen");
                  if(NatData->PortHandle == EventData->EventData.ConnectionCloseEventData.ConnectionID)
                     Env->CallVoidMethod(NatData->Object, Method_onClosed);
                  else
                     SS1_LOGE("Callback with wrong handle (Event is for %u, state data is for %u)", EventData->EventData.ConnectionCloseEventData.ConnectionID, NatData->PortHandle);

                  break;

               case setRemoteConnectionOpenStatus:
                  SS1_LOGI("Signal: RemoteConnectionOpenStatus");
                  if(NatData->PortHandle == EventData->EventData.RemoteConnectionOpenStatusEventData.ConnectionID)
                  {
                     if(EventData->EventData.RemoteConnectionOpenStatusEventData.Status == 0)
                     {
                        SS1_LOGI("Reporting connection successful (%u)", EventData->EventData.RemoteConnectionOpenStatusEventData.ConnectionID);
                        Env->CallVoidMethod(NatData->Object, Method_onConnected, EventData->EventData.RemoteConnectionOpenStatusEventData.ConnectionID);
                     }
                     else
                     {
                        SS1_LOGE("Reporting connection failed (%d)", EventData->EventData.RemoteConnectionOpenStatusEventData.Status);
                        Env->CallVoidMethod(NatData->Object, Method_onConnected, EventData->EventData.RemoteConnectionOpenStatusEventData.Status);
                     }
                  }
                  else
                     SS1_LOGE("Callback with wrong handle (Event is for %u, state data is for %u)", EventData->EventData.ServerConnectionOpenEventData.ConnectionID, NatData->PortHandle);

                  break;

               default:
                  SS1_LOGE("Received unknown event code %d", EventData->EventType);
            }

            /* If we had to attach the thread earlier, detach it now so */
            /* the JVM doesn't leak any resources.                      */
            if(AttachResult > 0)
               NatData->VM->DetachCurrentThread();
         }
         else
            SS1_LOGE("Could not attach thread to Java VM");

         pthread_mutex_unlock(&(NatData->Mutex));
      }
      else
         SS1_LOGE("Could not lock SCO socket mutex");
   }
   else
      SS1_LOGE("Callback did not provide a valid parameter");

   SS1_LOGD("Exit");
}

static void ClosePort(SS_NativeData_t *NatData)
{
   /* Connect to the Bluetooth daemon, but only try once. Since we're   */
   /* only closing ports, if the service is down, then the connections  */
   /* are already closed. If we only lost IPC connectivity, the daemon  */
   /* should have closed our connections for us when it noticed we had  */
   /* dropped off.                                                      */
   InitBTPMClientNoRetries(NULL, NULL, NULL);

   if(NatData)
   {
      if(NatData->PortHandle > 0)
      {
         if(NatData->PortType == btsoTypeServer)
         {
            SS1_LOGI("Unregistering SCO server");
            SCOM_UnRegisterServerConnection(NatData->PortHandle);
         }
         else
         {
            SS1_LOGI("Closing client-only SCO connection");
            SCOM_CloseConnection(NatData->PortHandle);
         }

         NatData->PortHandle = 0;
         NatData->PortType   = btsoTypeUnknown;
      }
      else
         SS1_LOGW("Close called on unopened port");
   }
   else
      SS1_LOGE("invalid parameter");
}

#endif /* HAVE_BLUETOOTH */

static void SS_ClassInitNative(JNIEnv *Env, jclass Clazz)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   Field_mNativeData  = Env->GetFieldID(Clazz, "mNativeData", "I");

   Method_onAccepted  = Env->GetMethodID(Clazz, "onAccepted",  "(I)V");
   Method_onConnected = Env->GetMethodID(Clazz, "onConnected", "(I)V");
   Method_onClosed    = Env->GetMethodID(Clazz, "onClosed",    "()V");
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

   /* Returns false if a serious error occured */
static jboolean SS_InitNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jboolean         ret_val;
   SS_NativeData_t *NatData;

   if((NatData = (SS_NativeData_t *)calloc(1, sizeof(SS_NativeData_t))) != NULL)
   {
      Env->SetIntField(Object, Field_mNativeData, (jint)NatData);

      NatData->PortHandle    = 0;
      NatData->PortType      = btsoTypeUnknown;
      NatData->Object        = Env->NewGlobalRef(Object);
      NatData->JNIEnvVersion = Env->GetVersion();
      Env->GetJavaVM(&(NatData->VM));
      pthread_mutex_init(&(NatData->Mutex), NULL);

      ret_val = JNI_TRUE;
   }
   else
   {
      SS1_LOGE("Unable to allocate memory");
      ret_val = JNI_FALSE;
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_TRUE);
   return(JNI_TRUE);
#endif
}

static void SS_DestroyNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jobject          ObjRef;
   SS_NativeData_t *NatData;

   ObjRef = NULL;

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      Env->SetIntField(Object, Field_mNativeData, (jint)NULL);

      if(pthread_mutex_lock(&(NatData->Mutex)) == 0)
      {
         /* Close any open connections for this handle.                 */
         ClosePort(NatData);

         ObjRef          = NatData->Object;
         NatData->Object = NULL;

         pthread_mutex_unlock(&(NatData->Mutex));
      }
      else
         SS1_LOGE("Unable to lock mutex");

      /* Free resources.                                                */
      pthread_mutex_destroy(&(NatData->Mutex));
      Env->DeleteGlobalRef(ObjRef);
      free(NatData);
   }
   else
      SS1_LOGE("Object uninitialized");
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

static jboolean SS_AcceptNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int              Result;
   jboolean         ret_val;
   BD_ADDR_t        AnyAddress;
   SS_NativeData_t *NatData;

   ret_val = JNI_FALSE;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         if(pthread_mutex_lock(&(NatData->Mutex)) == 0)
         {
            if((NatData->PortHandle == 0) && (NatData->PortType == btsoTypeUnknown))
            {
               memset(&AnyAddress, 0, sizeof(AnyAddress));
               Result = SCOM_RegisterServerConnection(TRUE, AnyAddress, SS_EventCallback, NatData);

               if(Result > 0)
               {
                  SS1_LOGI("Listening for incoming SCO connections (%d)", Result);
                  NatData->PortHandle = (unsigned int)Result;
                  NatData->PortType   = btsoTypeServer;
                  ret_val             = JNI_TRUE;
               }
               else
                  SS1_LOGE("Unable to register a listening SCO socket (%d)", Result);
            }
            else
            {
               if(NatData->PortType == btsoTypeServer)
                  SS1_LOGE("SCO socket already listening for connections");
               else
                  SS1_LOGE("SCO socket cannot listen because it represents an outgoing connection");
            }

            pthread_mutex_unlock(&(NatData->Mutex));
         }
         else
            SS1_LOGE("Unable to obtain lock");
      }
      else
         SS1_LOGE("Object uninitialized");
   }
   else
      SS1_LOGE("Unable to access Bluetooth platform");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

#if SS1_PLATFORM_SDK_VERSION <= 7
static jboolean SS_ConnectNative(JNIEnv *Env, jobject Object, jstring Address)
#else
static jboolean SS_ConnectNative(JNIEnv *Env, jobject Object, jstring Address, jstring Name)
#endif
{
   SS1_LOGD("Enter (%p)", Address);
#ifdef HAVE_BLUETOOTH
   int              Result;
   jboolean         ret_val;
   BD_ADDR_t        RemoteAddress;
   const char      *AddressString;
   SS_NativeData_t *NatData;

   ret_val = JNI_FALSE;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         if(pthread_mutex_lock(&(NatData->Mutex)) == 0)
         {
            if((NatData->PortHandle == 0) && (NatData->PortType == btsoTypeUnknown))
            {
               if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
               {
                  if(StrToBD_ADDR(&RemoteAddress, AddressString))
                  {
                     Result = SCOM_OpenRemoteConnection(RemoteAddress, SS_EventCallback, NatData, NULL);

                     if(Result > 0)
                     {
                        SS1_LOGI("Connected to SCO sink (%d, %s)", Result, AddressString);
                        NatData->PortHandle = (unsigned int)Result;
                        NatData->PortType   = btsoTypeClient;
                        ret_val             = JNI_TRUE;
                     }
                     else
                        SS1_LOGE("Unable to make SCO connection to %s (%d)", AddressString, Result);
                  }
                  else
                     SS1_LOGE("Unable to parse remote address string: %s", AddressString);

                  Env->ReleaseStringUTFChars(Address, AddressString);
               }
               else
                  SS1_LOGE("Unable to obtain remote address string");
            }
            else
            {
               if(NatData->PortType == btsoTypeClient)
                  SS1_LOGE("SCO socket is already connected");
               else
                  SS1_LOGE("SCO socket cannot connect outbound because because it is listening for incoming connections");
            }

            pthread_mutex_unlock(&(NatData->Mutex));
         }
         else
            SS1_LOGE("Unable to obtain lock");
      }
      else
         SS1_LOGE("Object uninitialized");
   }
   else
      SS1_LOGE("Unable to access Bluetooth platform");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static void SS_CloseNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   SS_NativeData_t *NatData;

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      if(pthread_mutex_lock(&(NatData->Mutex)) == 0)
      {
         ClosePort(NatData);

         /* Notify upper layer of the port closure, since               */
         /* SS_EventCallback() should not be called after the port is   */
         /* closed.                                                     */
         Env->CallVoidMethod(Object, Method_onClosed);

         pthread_mutex_unlock(&(NatData->Mutex));
      }
      else
         SS1_LOGE("Unable to lock mutex");
   }
   else
      SS1_LOGE("Object uninitialized");
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

static JNINativeMethod NativeMethods[] =
{
   {"classInitNative", "()V",                                     (void *)SS_ClassInitNative},
   {"initNative",      "()V",                                     (void *)SS_InitNative},
   {"destroyNative",   "()V",                                     (void *)SS_DestroyNative},
#if SS1_PLATFORM_SDK_VERSION <= 7
   {"connectNative",   "(Ljava/lang/String;)Z",                   (void *)SS_ConnectNative},
#else
   {"connectNative",   "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)SS_ConnectNative},
#endif
   {"acceptNative",    "()Z",                                     (void *)SS_AcceptNative},
   {"closeNative",     "()V",                                     (void *)SS_CloseNative},
};

int SS1API register_android_bluetooth_ScoSocket(JNIEnv *Env)
{
   return(AndroidRuntime::registerNativeMethods(Env, "android/bluetooth/ScoSocket", NativeMethods, NELEM(NativeMethods)));
}

} /* namespace android */
